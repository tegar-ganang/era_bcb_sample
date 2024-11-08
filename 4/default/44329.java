import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * A unified interface to OS-specific implementation of a system manager. 
 *
 * <p>SystemManager provides methods for sampling system and process status 
 * information.</p>
 * <p>Use getSystemManager method to create an OS specific implementation.</p>
 * <p>Access to process information is done through process identifiers (PIDs)
 * and process handles. On some OSes (e.g. Windows) process handle makes sure 
 * that process information is available until the handle is closed, however 
 * one cannot rely on this as other implementations (e.g. Linux) may still fail
 * even if handle has not been closed.</p>
 *
 * @see #getSystemManager
 * @see ProcessStatus
 * @see SystemMemoryStatus
 * @see java.lang.Runtime#exec
 * @author Marius Mikucionis
 */
public abstract class SystemManager {

    protected SystemManager() {
    }

    /**
     * Fills the sms structure with system memory status information.
     * The method call may fail due to sms being null, or for technical 
     * implementation reasons. sms structure will not reflect the system 
     * status if the call fails.
     * @param sms the SystemMemoryStatus object, should not be null.
     * @return true if successfull, otherwise false.
     * @see SystemMemoryStatus
     */
    public abstract boolean fetchSystemMemoryStatus(SystemMemoryStatus sms);

    /**
     * A-not-so-memory-efficient version of fetchSystemMemoryStatus.
     * @return new instance of system memory status information if success 
     * or null otherwise.
     * @see #fetchSystemMemoryStatus
     * @see SystemMemoryStatus
     */
    public SystemMemoryStatus getSystemMemoryStatus() {
        SystemMemoryStatus sms = new SystemMemoryStatus();
        if (fetchSystemMemoryStatus(sms)) return sms; else return null;
    }

    /**
     * Fills the ps structure with process status information.
     * After the succesfull call, ps structure reflects the status information
     * about given process. The call may fail due to bad handle, ps being null
     * or other technical implementation reasons (e.g. security).
     * @param handle (OS specific) handle to a process, created from 
     * openProcessHandle.
     * @param ps ProcessStatus object, should not be null.
     * @return true if successfull, otherwise false.
     * @see #openProcessHandle
     * @see ProcessStatus
     */
    public abstract boolean fetchProcessStatus(int handle, ProcessStatus ps);

    /**
     * A-not-so-memory-efficient version of fetchProcessStatus.
     * @return new instance of process status information if success or null 
     * otherwise.
     * @see #fetchProcessStatus
     * @see #openProcessHandle
     * @see ProcessStatus
     */
    public ProcessStatus getProcessStatus(int handle) {
        ProcessStatus ps = new ProcessStatus();
        if (fetchProcessStatus(handle, ps)) return ps; else return null;
    }

    /**
     * Opens a handle to a process with given identifier.
     *
     * Some OSs (e.g. Windows) provide access to process information through 
     * handles, which is a nice feature since it keeps the status information 
     * available even if the process has terminated. The handle should be 
     * closed with closeProcessHandle when it's no longer needed.
     * @param processID (host OS specific) process identifier.
     * @return integer representing process handle upon success, 0 otherwise.
     * @see #closeProcessHandle
     */
    public abstract int openProcessHandle(int processID);

    /**
     * Closes a process handle (which was opened by openProcessHandle).
     * @param handle the open process handle.
     * @see #openProcessHandle
     */
    public abstract void closeProcessHandle(int handle);

    /**
     * Fills the integer array with PIDs (process identifiers) of currently 
     * running processes. The array should not be null but initialized with a 
     * length of estimated maximum number of processes. It is difficult to 
     * predict exact number of running processes (this is the official position
     * of Windows API) so the array should be large enough to incorporate them 
     * all. The excess PIDs will be trimmed, so if the return value is the 
     * same as the length of the array, then consider enlarging the array and 
     * calling again.
     * @param pids non-null array of integers to be filled with PIDs.
     * @return the number of PIDs put into arrray.
     */
    public abstract int fetchProcessIDs(int[] pids);

    /**
     * Retrieves the name of executable referenced by process handle.
     * The call may fail if invalid handle is given or there are technical 
     * problems (e.g. security obstacles) while retrieving the information.
     * @param hProcess is a handle to a process created by openProcessHandle.
     * @return the name of a process executable upon success, null otherwise.
     */
    public abstract String getProcessName(int hProcess);

    /**
     * Copies the resource into temporary directory and loads it as a library.
     * The temporary library file is scheduled to be deleted when JVM exits.
     */
    protected static void loadTmpLibrary(String libsource, String prefix, String suffix) throws IOException {
        File libfile = File.createTempFile(prefix, suffix);
        InputStream libinp = ClassLoader.getSystemClassLoader().getSystemResourceAsStream(libsource);
        OutputStream libout = new FileOutputStream(libfile);
        byte[] buf = new byte[1024];
        int len;
        while ((len = libinp.read(buf)) > 0) libout.write(buf, 0, len);
        libinp.close();
        libout.close();
        libfile.setExecutable(true);
        System.load(libfile.getAbsolutePath());
        libfile.deleteOnExit();
    }

    private static SystemManager singletone = null;

    private static final Object lock = new Object();

    /**
     * Returns an OS-specific implementation of a system manager.
     * The method maintains a singletone object of a system manager, 
     * i.e. there is only one instance if at all. Currently the following 
     * OSes are supported: Linux, Windows (32-bit).
     *
     * @return a reference to system manager singletone or null if no 
     * implementation is available or there are technical problems to 
     * create one.
     */
    public static SystemManager getSystemManager() {
        SystemManager local = null;
        synchronized (lock) {
            if (singletone == null) try {
                String os = System.getProperty("os.name");
                if (os.startsWith("Linux")) {
                    loadTmpLibrary("libSystemManagerLinux.so", "libSML", ".so");
                    singletone = new SystemManagerLinux();
                } else if (os.startsWith("Windows")) {
                    loadTmpLibrary("SystemManagerWin32.dll", "SMW", ".dll");
                    singletone = new SystemManagerWin32();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace(System.err);
            } catch (UnsatisfiedLinkError ule) {
                ule.printStackTrace(System.err);
            }
            local = singletone;
        }
        return local;
    }
}
