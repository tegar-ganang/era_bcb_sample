package net.jsrb.util.lang;

import java.io.*;
import java.util.*;
import net.jsrb.util.log4j.Logger;
import net.jsrb.rtl.*;
import net.jsrb.util.*;
import static net.jsrb.rtl.RtlConstants.*;

/**
 *Process wrapper class
 */
public class UnixProcess {

    static Logger TRC_LOGGER = Logger.getLogger(UnixProcess.class);

    @java.lang.SuppressWarnings("unused")
    private static native int sigchildInit();

    @java.lang.SuppressWarnings("unused")
    private static native void sigchildStop();

    private static final int MAX_PENDING_SIGCHILD = 10;

    private static List<SIGCHLD> sigchldPending = new ArrayList<SIGCHLD>();

    private static Thread notifyThread = null;

    private static FileDescriptor fdNotify = null;

    private static UnixProcessListener listener = null;

    /**
     * Data Holder for SIGCHLD signal passed from native code
     */
    public static class SIGCHLD {

        public int pid;

        public int exitStatus;

        public boolean WIFEXITED = false;

        public boolean WIFSIGNALED = false;

        public int WTERMSIG = 0;

        public boolean WIFSTOPPED = false;

        public int WSTOPSIG = 0;

        public boolean WIFCONTINUED = false;

        /**
         * Load value from the sigchild string ,coming from native code, like below:
         * <BR>pid=22601\tWIFEXITED=1\tWEXITSTATUS=99\tWIFSIGNALED=0\tWTERMSIG=0  WIFSTOPPED=0\tWSTOPSIG=99\tWIFCONTINUED=0  
         * @param sigchildStr
         */
        SIGCHLD load(String sigchildStr) {
            sigchildStr = sigchildStr.replace('\t', '\n');
            Properties props = new Properties();
            try {
                props.load(new ByteArrayInputStream(sigchildStr.getBytes()));
                pid = Integer.parseInt(props.getProperty("pid"));
                exitStatus = Integer.parseInt(props.getProperty("WEXITSTATUS"));
                WIFEXITED = Integer.parseInt(props.getProperty("WIFEXITED")) != 0;
                WIFSIGNALED = Integer.parseInt(props.getProperty("WIFSIGNALED")) != 0;
                WTERMSIG = Integer.parseInt(props.getProperty("WTERMSIG"));
                WIFSTOPPED = Integer.parseInt(props.getProperty("WIFSTOPPED")) != 0;
                WSTOPSIG = Integer.parseInt(props.getProperty("WSTOPSIG"));
                WIFCONTINUED = Integer.parseInt(props.getProperty("WIFCONTINUED")) != 0;
            } catch (IOException e) {
            }
            return this;
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("sigchild[").append("pid=" + pid).append(",exitStatus=" + exitStatus).append(",WIFEXITED=" + WIFEXITED).append(",WIFSIGNALED=" + WIFSIGNALED).append(",WTERMSIG=" + WTERMSIG).append(",WIFSTOPPED=" + WIFSTOPPED).append(",WSTOPSIG=" + WSTOPSIG).append(",WIFCONTINUED=" + WIFCONTINUED).append("]");
            return buffer.toString();
        }
    }

    /**
     * Dispatch SIGCHLD event to listener or notify waiting threads
     */
    static class NotifyThread extends Thread {

        NotifyThread(String name) {
            super(name);
        }

        public void run() {
            InputStream is = null;
            try {
                is = new FileInputStream(fdNotify);
                while (true) {
                    byte buflen[] = new byte[3];
                    int buflenSize = is.read(buflen);
                    if (buflenSize != 3) {
                        TRC_LOGGER.error("UnixProcess: get invalid notify head buffer, size=" + buflenSize);
                        continue;
                    }
                    int bl = Integer.parseInt(new String(buflen));
                    byte buf[] = new byte[bl];
                    int bufRead = is.read(buf);
                    String sigchildStr = new String(buf, 0, bufRead);
                    if (TRC_LOGGER.isDebugMinEnabled()) {
                        TRC_LOGGER.debugMin("UnixProcess: SIGCHILD triggered, " + sigchildStr);
                    }
                    SIGCHLD sig = new SIGCHLD().load(sigchildStr);
                    boolean processed = false;
                    if (listener != null) {
                        processed = listener.onSIGCHILD(sig);
                    }
                    if (processed) {
                        continue;
                    }
                    synchronized (sigchldPending) {
                        sigchldPending.add(sig);
                        if (sigchldPending.size() > MAX_PENDING_SIGCHILD) {
                            sigchldPending.remove(0);
                        }
                        sigchldPending.notifyAll();
                    }
                }
            } catch (Exception e) {
                TRC_LOGGER.error(e);
            }
        }
    }

    /**
     * Start Notify Thread
     */
    private static void startNotifyThread() {
        if (notifyThread != null) {
            return;
        }
        int p_notify = sigchildInit();
        if (p_notify != 0) {
            fdNotify = NativeUtil.i2fd(p_notify, new FileDescriptor());
            notifyThread = new NotifyThread("UnixProcess Notify Thread");
            notifyThread.setDaemon(true);
            notifyThread.start();
        }
    }

    public static void setListener(UnixProcessListener l) {
        listener = l;
    }

    static {
        startNotifyThread();
    }

    private int pid;

    private UnixProcess(int pid) {
        this.pid = pid;
    }

    public int getPid() {
        return pid;
    }

    public boolean isAlive() {
        return isAlive(pid);
    }

    public void kill() {
        kill(pid);
    }

    private static native int createProcess(byte[] prog, byte[] argBlock, int argCount, byte[] envBlock, int envCount, byte[] dir, ProcessIo pio) throws IOException;

    static UnixProcess createUnixProcess(String[] commands, Map<String, String> envs, String workDirectory, ProcessIo pio) throws IOException {
        String prog = commands[0];
        byte[][] args = new byte[commands.length - 1][];
        int size = 0;
        for (int i = 0; i < args.length; i++) {
            args[i] = commands[i + 1].getBytes();
            size += (args[i].length + 1);
        }
        int argCount = args.length;
        byte[] argBlock = new byte[size];
        int tmpOfs = 0;
        for (int k = 0; k < args.length; k++) {
            byte[] arg = args[k];
            System.arraycopy(arg, 0, argBlock, tmpOfs, arg.length);
            tmpOfs += (arg.length + 1);
        }
        byte[][] envBytes = new byte[envs.size()][];
        int envBlockSize = 0;
        int envOfs = 0;
        for (Map.Entry<String, String> entry : envs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String envEntry = key + "=" + value;
            byte[] envEntryBytes = envEntry.getBytes();
            envBytes[envOfs++] = envEntryBytes;
            envBlockSize += (envEntryBytes.length + 1);
        }
        byte[] envBlock = new byte[envBlockSize];
        envOfs = 0;
        for (int i = 0; i < envBytes.length; i++) {
            System.arraycopy(envBytes[i], 0, envBlock, envOfs, envBytes[i].length);
            envOfs += (envBytes[i].length + 1);
        }
        int pid = createProcess(NativeUtil.toCString(prog), argBlock, argCount, envBlock, envBytes.length, NativeUtil.toCString(workDirectory), pio);
        UnixProcess process = new UnixProcess(pid);
        return process;
    }

    /**
     * Check if a process with given pid is alive
     * 
     * @param pid
     * @return
     */
    public static boolean isAlive(int pid) {
        ProcessState state = procinfo.getProcessState(pid);
        if (state != null && state != ProcessState.ZOMBIE) {
            return true;
        }
        return false;
    }

    /**
     * Wait for the process ends.
     * 
     * @param timeout wait timeout , in ms. -1 means wait for ever.
     * 
     * @return exit status 
     */
    public int waitFor(int timeout) {
        long enterTs = System.currentTimeMillis();
        int exitStatus = -1;
        synchronized (sigchldPending) {
            while (true) {
                for (SIGCHLD sig : sigchldPending) {
                    if (sig.pid == pid) {
                        sigchldPending.remove(sig);
                        return sig.exitStatus;
                    }
                }
                try {
                    if (timeout < 0) {
                        sigchldPending.wait();
                    } else {
                        int t = timeout - (int) (System.currentTimeMillis() - enterTs);
                        if (t <= 0) {
                            break;
                        }
                        sigchldPending.wait(t);
                    }
                } catch (InterruptedException e) {
                }
            }
        }
        return exitStatus;
    }

    /**
     * Kill a process
     * 
     * @param pid
     */
    public static void kill(int pid) {
        if (!isAlive(pid)) {
            return;
        }
        try {
            unistd.kill(pid, SIGKILL);
        } catch (RtlException e) {
        }
    }
}
