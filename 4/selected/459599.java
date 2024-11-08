package com.sshtools.daemon.session;

import com.sshtools.daemon.configuration.*;
import com.sshtools.daemon.platform.*;
import com.sshtools.daemon.scp.*;
import com.sshtools.daemon.subsystem.*;
import com.sshtools.j2ssh.*;
import com.sshtools.j2ssh.agent.*;
import com.sshtools.j2ssh.configuration.*;
import com.sshtools.j2ssh.connection.*;
import com.sshtools.j2ssh.io.*;
import com.sshtools.j2ssh.util.*;
import org.apache.commons.logging.*;
import java.io.*;
import java.util.*;

/**
 *
 *
 * @author $author$
 * @version $Revision: 1.16 $
 */
public class SessionChannelServer extends IOChannel {

    private static Log log = LogFactory.getLog(SessionChannelServer.class);

    /**  */
    public static final String SESSION_CHANNEL_TYPE = "session";

    private static Map allowedSubsystems = new HashMap();

    private Map environment = new HashMap();

    private NativeProcessProvider processInstance;

    private SubsystemServer subsystemInstance;

    private Thread thread;

    private IOStreamConnector ios;

    private ChannelOutputStream stderrOut;

    private InputStream stderrIn;

    private ProcessMonitorThread processMonitor;

    private PseudoTerminalWrapper pty;

    private SshAgentForwardingListener agent;

    private ServerConfiguration config;

    /**
 * Creates a new SessionChannelServer object.
 *
 * @throws ConfigurationException
 */
    public SessionChannelServer() throws ConfigurationException {
        super();
        config = (ServerConfiguration) ConfigurationLoader.getConfiguration(ServerConfiguration.class);
        allowedSubsystems.putAll(config.getSubsystems());
    }

    private void bindStderrInputStream(InputStream stderrIn) {
        this.stderrIn = stderrIn;
        ios = new IOStreamConnector(stderrIn, stderrOut);
    }

    /**
 *
 *
 * @param cols
 * @param rows
 * @param width
 * @param height
 */
    protected void onChangeTerminalDimensions(int cols, int rows, int width, int height) {
    }

    /**
 *
 *
 * @throws IOException
 */
    protected void onChannelClose() throws IOException {
        if (agent != null) {
            agent.removeReference(this);
        }
        if (processInstance != null) {
            if (processInstance.stillActive()) {
                processInstance.kill();
            }
        }
        if (subsystemInstance != null) {
            subsystemInstance.stop();
        }
        if (processMonitor != null) {
            StartStopState state = processMonitor.getStartStopState();
            try {
                state.waitForState(StartStopState.STOPPED);
            } catch (InterruptedException ex) {
                throw new IOException("The process monitor was interrupted");
            }
        }
    }

    /**
 *
 *
 * @throws IOException
 */
    protected void onChannelEOF() throws IOException {
    }

    /**
 *
 *
 * @param data
 *
 * @throws IOException
 */
    protected void onChannelExtData(byte[] data) throws IOException {
    }

    /**
 *
 *
 * @throws InvalidChannelException
 */
    protected void onChannelOpen() throws InvalidChannelException {
        stderrOut = new ChannelOutputStream(this, new Integer(SshMsgChannelExtendedData.SSH_EXTENDED_DATA_STDERR));
    }

    /**
 *
 *
 * @param command
 *
 * @return
 *
 * @throws IOException
 */
    protected boolean onExecuteCommand(String command) throws IOException {
        log.debug("Executing command " + command);
        if (command.startsWith("scp ")) {
            if (processInstance == null) {
                processInstance = new ScpServer();
            }
        }
        if (processInstance == null) {
            processInstance = NativeProcessProvider.newInstance();
        }
        if (processInstance == null) {
            log.debug("Failed to create process");
            return false;
        }
        boolean result = processInstance.createProcess(command, environment);
        if (result) {
            if (pty != null) {
                pty.bindMasterOutputStream(getOutputStream());
                pty.bindMasterInputStream(getInputStream());
                pty.bindSlaveInputStream(processInstance.getInputStream());
                pty.bindSlaveOutputStream(processInstance.getOutputStream());
                pty.initialize();
                bindInputStream(pty.getMasterInputStream());
                bindStderrInputStream(processInstance.getStderrInputStream());
            } else {
                bindInputStream(processInstance.getInputStream());
                bindOutputStream(processInstance.getOutputStream());
                bindStderrInputStream(processInstance.getStderrInputStream());
            }
        }
        return result;
    }

    /**
 *
 *
 * @param term
 * @param cols
 * @param rows
 * @param width
 * @param height
 * @param modes
 *
 * @return
 */
    protected boolean onRequestPseudoTerminal(String term, int cols, int rows, int width, int height, String modes) {
        try {
            processInstance = NativeProcessProvider.newInstance();
            if (processInstance.supportsPseudoTerminal(term)) {
                return processInstance.allocatePseudoTerminal(term, cols, rows, width, height, modes);
            } else {
                pty = new PseudoTerminalWrapper(term, cols, rows, width, height, modes);
                return true;
            }
        } catch (IOException ioe) {
            log.warn("Failed to allocate pseudo terminal " + term, ioe);
            return false;
        }
    }

    /**
 *
 *
 * @param name
 * @param value
 */
    protected void onSetEnvironmentVariable(String name, String value) {
        environment.put(name, value);
    }

    /**
 *
 *
 * @return
 *
 * @throws IOException
 */
    protected boolean onStartShell() throws IOException {
        String shell = config.getTerminalProvider();
        if (processInstance == null) {
            processInstance = NativeProcessProvider.newInstance();
        }
        if ((shell != null) && !shell.trim().equals("")) {
            int idx = shell.indexOf("%DEFAULT_TERMINAL%");
            if (idx > -1) {
                shell = ((idx > 0) ? shell.substring(0, idx) : "") + processInstance.getDefaultTerminalProvider() + (((idx + 18) < shell.length()) ? shell.substring(idx + 18) : "");
            }
        } else {
            shell = processInstance.getDefaultTerminalProvider();
        }
        return onExecuteCommand(shell);
    }

    /**
 *
 *
 * @param subsystem
 *
 * @return
 */
    protected boolean onStartSubsystem(String subsystem) {
        boolean result = false;
        try {
            if (!allowedSubsystems.containsKey(subsystem)) {
                log.error(subsystem + " Subsystem is not available");
                return false;
            }
            AllowedSubsystem obj = (AllowedSubsystem) allowedSubsystems.get(subsystem);
            if (obj.getType().equals("class")) {
                Class cls = Class.forName(obj.getProvider());
                subsystemInstance = (SubsystemServer) cls.newInstance();
                subsystemInstance.setSession(this);
                bindInputStream(subsystemInstance.getInputStream());
                bindOutputStream(subsystemInstance.getOutputStream());
                return true;
            } else {
                String provider = obj.getProvider();
                File f = new File(provider);
                if (!f.exists()) {
                    provider = ConfigurationLoader.getHomeDirectory() + "bin" + File.separator + provider;
                    f = new File(provider);
                    if (!f.exists()) {
                        log.error("Failed to locate subsystem provider " + obj.getProvider());
                        return false;
                    }
                }
                return onExecuteCommand(provider);
            }
        } catch (Exception e) {
            log.error("Failed to start subsystem " + subsystem, e);
        }
        return false;
    }

    /**
 *
 *
 * @return
 */
    public byte[] getChannelOpenData() {
        return null;
    }

    /**
 *
 *
 * @return
 */
    public byte[] getChannelConfirmationData() {
        return null;
    }

    /**
 *
 *
 * @return
 */
    protected int getMinimumWindowSpace() {
        return 1024;
    }

    /**
 *
 *
 * @return
 */
    protected int getMaximumWindowSpace() {
        return 32648;
    }

    /**
 *
 *
 * @return
 */
    protected int getMaximumPacketSize() {
        return 32648;
    }

    /**
 *
 *
 * @return
 */
    public String getChannelType() {
        return SESSION_CHANNEL_TYPE;
    }

    /**
 *
 *
 * @param requestType
 * @param wantReply
 * @param requestData
 *
 * @throws IOException
 */
    protected void onChannelRequest(String requestType, boolean wantReply, byte[] requestData) throws IOException {
        log.debug("Channel Request received: " + requestType);
        boolean success = false;
        if (requestType.equals("shell")) {
            success = onStartShell();
            if (success) {
                if (wantReply) {
                    connection.sendChannelRequestSuccess(this);
                }
                processInstance.start();
                processMonitor = new ProcessMonitorThread(processInstance);
            } else if (wantReply) {
                connection.sendChannelRequestFailure(this);
            }
        }
        if (requestType.equals("env")) {
            ByteArrayReader bar = new ByteArrayReader(requestData);
            String name = bar.readString();
            String value = bar.readString();
            onSetEnvironmentVariable(name, value);
            if (wantReply) {
                connection.sendChannelRequestSuccess(this);
            }
        }
        if (requestType.equals("exec")) {
            ByteArrayReader bar = new ByteArrayReader(requestData);
            String command = bar.readString();
            success = onExecuteCommand(command);
            if (success) {
                if (wantReply) {
                    connection.sendChannelRequestSuccess(this);
                }
                processInstance.start();
                processMonitor = new ProcessMonitorThread(processInstance);
            } else if (wantReply) {
                connection.sendChannelRequestFailure(this);
            }
        }
        if (requestType.equals("subsystem")) {
            ByteArrayReader bar = new ByteArrayReader(requestData);
            String subsystem = bar.readString();
            success = onStartSubsystem(subsystem);
            if (success) {
                if (wantReply) {
                    connection.sendChannelRequestSuccess(this);
                }
                if (processInstance != null) {
                    processInstance.start();
                    processMonitor = new ProcessMonitorThread(processInstance);
                } else if (subsystemInstance != null) {
                    subsystemInstance.start();
                    processMonitor = new ProcessMonitorThread(subsystemInstance);
                }
            } else if (wantReply) {
                connection.sendChannelRequestFailure(this);
            }
        }
        if (requestType.equals("pty-req")) {
            ByteArrayReader bar = new ByteArrayReader(requestData);
            String term = bar.readString();
            int cols = (int) bar.readInt();
            int rows = (int) bar.readInt();
            int width = (int) bar.readInt();
            int height = (int) bar.readInt();
            String modes = bar.readString();
            success = onRequestPseudoTerminal(term, cols, rows, width, height, modes);
            if (wantReply && success) {
                connection.sendChannelRequestSuccess(this);
            } else if (wantReply) {
                connection.sendChannelRequestFailure(this);
            }
        }
        if (requestType.equals("window-change")) {
            ByteArrayReader bar = new ByteArrayReader(requestData);
            int cols = (int) bar.readInt();
            int rows = (int) bar.readInt();
            int width = (int) bar.readInt();
            int height = (int) bar.readInt();
            onChangeTerminalDimensions(cols, rows, width, height);
            if (wantReply && success) {
                connection.sendChannelRequestSuccess(this);
            } else if (wantReply) {
                connection.sendChannelRequestFailure(this);
            }
        }
        if (requestType.equals("auth-agent-req")) {
            try {
                SshThread thread = SshThread.getCurrentThread();
                agent = SshAgentForwardingListener.getInstance(thread.getSessionIdString(), connection);
                agent.addReference(this);
                environment.put("SSH_AGENT_AUTH", agent.getConfiguration());
                thread.setProperty("sshtools.agent", agent.getConfiguration());
                if (wantReply) {
                    connection.sendChannelRequestSuccess(this);
                }
            } catch (Exception ex) {
                if (wantReply) {
                    connection.sendChannelRequestFailure(this);
                }
            }
        }
    }

    class ProcessMonitorThread extends Thread {

        private NativeProcessProvider process;

        private SubsystemServer subsystem;

        private StartStopState state;

        public ProcessMonitorThread(NativeProcessProvider process) {
            this.process = process;
            state = new StartStopState(StartStopState.STARTED);
            start();
        }

        public ProcessMonitorThread(SubsystemServer subsystem) {
            state = subsystem.getState();
        }

        public StartStopState getStartStopState() {
            return state;
        }

        public void run() {
            try {
                log.info("Monitor waiting for process exit code");
                int exitcode = process.waitForExitCode();
                if (exitcode == 9999999) {
                    log.error("Process monitor failed to retrieve exit code");
                } else {
                    log.debug("Process exit code is " + String.valueOf(exitcode));
                    process.getInputStream().close();
                    process.getOutputStream().close();
                    process.getStderrInputStream().close();
                    ByteArrayWriter baw = new ByteArrayWriter();
                    baw.writeInt(exitcode);
                    if (connection.isConnected() && SessionChannelServer.this.isOpen()) {
                        connection.sendChannelRequest(SessionChannelServer.this, "exit-status", false, baw.toByteArray());
                    }
                    state.setValue(StartStopState.STOPPED);
                    SessionChannelServer.this.close();
                }
            } catch (IOException ioe) {
                log.error("Failed to kill process", ioe);
            }
        }
    }
}
