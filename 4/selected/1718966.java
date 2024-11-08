package ade;

import java.awt.Color;
import java.io.*;
import java.lang.reflect.*;
import java.rmi.registry.*;
import java.util.*;

/**
Global static constants, enums, methods, etcetera for the ADE system.

@author Jim Kramer
*/
public class ADEGlobals implements Serializable {

    /** The version of ADE. */
    public static String ADE_VERSION_STRING = "ADEv1.0-beta";

    /** The possible levels of access an {@link ade.ADEUser ADEUser} can
	 * be granted to an {@link ade.ADEServerImpl ADEServerImpl}. */
    public static enum UserPerms {

        NONE, VIEW, READ, WRITE, ADMIN, REGADMIN
    }

    /** The possible states of operation for an {@link ade.ADEHostInfo
	 * ADEHost}. */
    public static enum HostState {

        UP, AVAIL, DOWN
    }

    /** The possible states of operation for an {@link ade.ADEServerImpl
	 * ADEServerImpl}. Server states are determined from the perspective
	 * of the server itself; a server's information about other servers
	 * is determined by the {@link ade.ADEGlobals.PRefState PRefState}. */
    public static enum ServerState {

        /** Initializing, but not yet known to the system (i.e., not yet
		 * registered). */
        INIT, /** Known to an {@link ade.ADERegistry ADERegistry}; the server is
		 * ready to get and supply client connections. */
        REGISTER, /** Running. */
        RUN, /** Suspended. */
        SUSPEND, /** Shutting down. */
        SHUTDOWN, /** Deregistering. */
        DEREGISTER
    }

    /** The possible states of a connection between {@link ade.ADEServerImpl
	 * ADEServerImpl}s. */
    public static enum PRefState {

        /** Pseudo-reference has been instantiated, but the connection to the
		 * remote object is not yet usable. */
        INIT, /** Connected to the remote object. */
        CONNECTED, /** Connection to the remote object has failed; additional information
		 * is available via the {@link ade.ADEGlobals.RecoveryState
                 * RecoveryState}. */
        LOST, /** Disconnecting, after which the pseudoreference will be released. */
        CLOSE, /** Pseudo-reference has been initialized, but no connection exists
		 * with a remote reference (no heartbeat is sent). */
        SUSPEND
    }

    /** The possible states of a lost connection between {@link
	 * ade.ADEServerImpl ADEServerImpl}s. */
    public static enum RecoveryState {

        /** Server is believed to be functioning. */
        OK(false), /** Unknown; no information about the remote object's status has been
		 * made. */
        UNK(false), /** Registry does not have definitive state. */
        DOWN(false), /** In the recovery process. */
        INREC(true), /** Failed, but delaying recovery. */
        DELAY(true), /** Unrecoverable. */
        UNREC(true);

        private final boolean notify;

        RecoveryState(boolean n) {
            notify = n;
        }

        /** Whether to trigger a notification event in servers that have a
		 * <i>pseudo-reference</i>. */
        public boolean shouldNotify() {
            return notify;
        }
    }

    /** The recognized operating systems for ADE. OS names correspond to the
	 * string returned by Java's <tt>os.name</tt> system property <b>except</b>
	 * various flavors of Windows are not distinguished. File and path
	 * separators are also stored, as it is sometimes necessary to build
	 * appropriate classpaths or filepaths prior to accessing the hosts. OS
	 * default settings are defined in the {@link
	 * ade.ADEHostInfo.HostDefaultInfo HostDefaultInfo} enumeration in {@link
	 * ade.ADEHostInfo ADEHostInfo}, which are accessible by obtaining the
	 * enumeration constant with the {@link #hostDefaultInfo} method. Note
	 * that ADE has not necessarily been tested on the listed OSes.
	 * @see ade.ADEGlobals#getOSByName */
    public static enum HostOS {

        /** The enum constant for Linux. */
        LINUX("Linux", "/", ":", "; "), /** The enum constant for FreeBSD. */
        FREEBSD("FreeBSD", "/", ":", "; "), /** The enum constant for Sun's Solaris. */
        SOLARIS("Solaris", "/", ":", "; "), /** The enum constant for Apple's OS-X. */
        MACOSX("Mac OS X", "/", ":", "; "), /** The enum constant for Cygwin on Windows. */
        CYGWIN("Cygwin", "/", ":", "; "), /** The enum constant for Windows XP. */
        WINDOWSXP("Windows XP", "\\", ";", " && "), /** The enum constant for Windows Vista. */
        WINDOWSVISTA("Windows Vista", "\\", ";", " && "), /** The enum constant for an unknown OS. */
        UNKNOWN("Unknown", "", "", "; ");

        private final String name;

        private final String fsep;

        private final String psep;

        private final String csep;

        HostOS(String n, String f, String p, String c) {
            name = n;
            fsep = f;
            psep = p;
            csep = c;
        }

        /** The operating system name (as returned by the <tt>os.name</tt> system
		 * property. Note that all versions of Windows return <tt>Windows</tt>. */
        public String osname() {
            return name;
        }

        /** The file separator character ("/" on UNIX, "\" on Windows. */
        public String fsep() {
            return fsep;
        }

        /** The path separator character (":" on UNIX, ";" on Windows. */
        public String psep() {
            return psep;
        }

        /** The command separator ("; " on UNIX, " && " on Windows. */
        public String csep() {
            return csep;
        }

        /** Return the default operating system settings (as defined in {@link
		 * ade.ADEHostInfo.HostDefaultInfo HostDefaultInfo}). */
        public ADEHostInfo.HostDefaultInfo hostDefaultInfo() {
            return ADEHostInfo.getHostDefaultInfo(this);
        }

        /** Returns the same string as {@link #osname}. */
        public String toString() {
            return name;
        }
    }

    /** The exit codes used for a {@link java.lang.System#exit}, {@link
	 * java.lang.Runtime#exit}, or {@link java.lang.Runtime#halt} call. */
    public static enum ExitCode {

        /** Normal termination. */
        OK("Normal termination", 0), /** Invalid port value (not between 1025 and 65535). */
        PORT("Invalid port", 1), /** Configuration file not found. */
        CONFIG_FILE("Configuration file not found", 2), /** Hosts file not found. */
        HOST_FILE("Host file not found", 3), /** Argument parsing. */
        ARG_PARSE("Argument parsing", 4), /** File parsing. */
        FILE_PARSE("File parsing", 5), /** Java Registry failure. */
        JAVA_REG("Java Registry failure", 6), /** IP address determination. */
        IP("IP address", 7), /** Failure contacting an {@link ade.ADERegistry ADERegistry}. */
        REG_CONTACT("ADERegistry contact failure", 8), /** Requested name already taken. */
        NAME_CONFLICT("Server name already taken", 9), /** Failure in registering. */
        REG_FAIL("Registration failure", 10), /** Failure in creating an {@link ade.ADERemoteCallTimer
		 * ADERemoteCallTimer}. */
        RCT_CREATION("ADERemoteCallTimer creation", 11), /** Server killed. */
        KILL("Explicit kill", 12);

        private final String desc;

        private final int code;

        ExitCode(String d, int c) {
            desc = d;
            code = c;
        }

        /** Returns the integer code (generally matches the <tt>ordinal</tt>) */
        public int code() {
            return code;
        }

        /** Returns a description of the exit code. */
        public String toString() {
            return desc;
        }
    }

    /** Maximum unique identifiers for servers of the same type. */
    public static final int DEF_MAX_UNIQUE = 99999;

    /** Default registry IP address */
    public static final String DEF_REGIP = "127.0.0.1";

    /** Default port to use for ADERegistry RMI communications */
    public static final int DEF_REGPORT = Registry.REGISTRY_PORT;

    /** Default ADERegistry name. */
    public static final String DEF_REGNAME = "ADERegistry";

    /** Default ADERegistry type (name + "Impl"). */
    public static final String DEF_REGTYPE = DEF_REGNAME + "Impl";

    /** Default port to use for ADEServer RMI communications. */
    public static final int DEF_SERVERPORT = Registry.REGISTRY_PORT;

    /** Default period of the heartbeats (in ms). */
    public static final int DEF_HBPULSE = 1500;

    /** Default period of the reaper (in ms, twice the heartbeat). */
    public static final int DEF_RPPULSE = (2 * DEF_HBPULSE);

    /** Non-responsive timeout used for initiating failure recovery (in ms).
	 * Adjust this according to system load, network latency, etc. */
    public static final int DEF_RECOVERYTIMEOUT = 1000;

    /** Timeout value (in ms) for requesting a connection to an ADEServer. */
    public static final int DEF_TIMEOUT_REQUEST = 30000;

    /** Default timeout value (in ms) for remote calls. */
    public static final int DEF_TIMEOUT_CALL = 3000;

    /** Timeout value (in ms) for short remote calls. */
    public static final int DEF_TIMEOUT_SHORT = 750;

    /** Number of times an ADEServer should be restarted. */
    public static final int DEF_RESTARTS = 0;

    /** Maximum connections allowed. Default is 1, making it exclusive. */
    public static final int DEF_MAXCONN = 10;

    /** Capability string (description of a server's use). */
    public static final String DEF_CAPS = "Capability string not defined.";

    /** Access rights to all servers. */
    public static final String ALL_ACCESS = "any";

    private static final String squashString(String s) {
        String r = "";
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != ' ') r += s.charAt(i);
        }
        return r;
    }

    /** Return the operating system enum constant for a given string.
	 * @param get the OS name, as defined in {@link ade.ADEGlobals.HostOS
	 * HostOS}
	 * @return the {@link ade.ADEGlobals.HostOS HostOS} enum constant */
    public static final HostOS getOSByName(String get) {
        String friendlyTest = squashString(get).toUpperCase();
        Set<HostOS> osset = EnumSet.allOf(HostOS.class);
        for (HostOS os : osset) {
            if (friendlyTest.indexOf(squashString(os.toString()).toUpperCase()) >= 0) {
                return os;
            }
        }
        return null;
    }

    /** Makes a String array out of Strings to split by whitespace. */
    public static final String[] makeArray(String s1, String s2) {
        return makeArray(s1, s2, "\\s");
    }

    /** Makes a String array out of Strings to split by some regex
	 * delimiter. */
    public static final String[] makeArray(String s1, String s2, String rex) {
        String[] tmp1 = s1.split(rex);
        String[] tmp2 = s2.split(rex);
        String[] tmpall = new String[tmp1.length + tmp2.length];
        System.arraycopy(tmp1, 0, tmpall, 0, tmp1.length);
        System.arraycopy(tmp2, 0, tmpall, (tmp1.length - 1), tmp2.length);
        return tmpall;
    }

    /** Round a double to a specific number of decimal places. Helpful in
	 * printing; note that this multiplies the 10^<tt>dec</tt>, so there's
	 * a definite limit on the size of the numbers it can handle.
	 * @param num The double to round
	 * @param dec the number of decimal places
	 * @return A double with <tt>dec</tt> decimal places */
    public static final double roundDec(double num, int dec) {
        int div = tenPowerInt(dec);
        long ret = Math.round(num * div);
        return ((double) ret / div);
    }

    /** Return 10 to the <i>n</i>th power via a <tt>switch</tt> rather than
	 * <tt>Math.pow</tt> for efficiency's sake (ends up being ~40% faster).
	 * Note that <tt>n</tt> must be 9 or less due to integer capacity
	 * limitations (need to write method <tt>tenPowerLong</tt> for a bigger
	 * number).
	 * @param n The power between 0 and 9, inclusive
	 * @throws UnsupportedOperationException If <tt>n</tt> is less than 0 or
	 * greater than 9 */
    public static final int tenPowerInt(int n) {
        switch(n) {
            case 1:
                return 10;
            case 2:
                return 100;
            case 3:
                return 1000;
            case 4:
                return 10000;
            case 5:
                return 100000;
            case 6:
                return 1000000;
            case 7:
                return 10000000;
            case 8:
                return 100000000;
            case 9:
                return 1000000000;
            case 0:
                return 1;
            default:
                throw new UnsupportedOperationException("Cannot do 10^" + n);
        }
    }

    /** Return whether value <tt>v1</tt> is within <tt>tol</tt> of <tt>v2</tt>
	 * (inclusive).
	 * @param v1 The value under examination
	 * @param v2 The target value
	 * @param tol The plus/minus tolerance */
    public static boolean within(int v1, int v2, int tol) {
        return (v1 >= (v2 - tol) && v1 <= (v2 + tol));
    }

    /** Return whether value <tt>v1</tt> is within <tt>tol</tt> of <tt>v2</tt>
	 * (inclusive).
	 * @param v1 The value under examination
	 * @param v2 The target value
	 * @param tol The plus/minus tolerance */
    public static boolean within(double v1, double v2, double tol) {
        return (v1 >= (v2 - tol) && v1 <= (v2 + tol));
    }

    /** Check to see if the current JVM is running on Windows. Due to Windows's
	 * peculiarities (e.g., backslashes, spaces in path names, etc.), it is
	 * helpful to have a standard mechanism that determines if the JVM is on
	 * a Windows machine. If an emulator is active (the only one currently
	 * checked for is Cygwin), the Windows manipulations are unnecessary and
	 * this method returns <tt>false</tt>.
	 * @return <tt>true</tt> if Windows manipulations have to be handled,
	 * <tt>false</tt> otherwise */
    public static final boolean isWindows() {
        String osname = System.getProperty("os.name");
        if (osname.indexOf("Windows") >= 0) {
            String sysdrive = System.getenv("SystemDrive");
            try {
                ADEGlobals.checkDir(sysdrive + "\\cygwin", true, false);
                if (System.getenv("CYGWIN") != null) {
                    return false;
                } else if (System.getenv("OSTYPE") != null) {
                    if (System.getenv("OSTYPE").equals("cygwin")) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                System.err.println("Unexpected exception in isWindows():\n" + e);
            }
        }
        return false;
    }

    /** Transform normal paths to Windows-compatible paths.
	 * @param path A UNIX path (may have a file)
	 * @return The path converted to Windows-compatible format */
    public static final String windowsPath(String path) {
        return path.replace(" ", "\\ ");
    }

    /** Return a brief String representation of a {@link java.lang.Throwable
	 * Throwable}. To provide the most concise, yet relevant information, two
	 * lines are returned: the first is obtained from the <tt>toString</tt> of
	 * the parameter, while the second is the first <tt>StackTraceElement</tt>
	 * that refers to a non-native method with a valid file name and known
	 * line number.
	 * @param e The <tt>Throwable</tt> object
	 * @return An abbreviated message */
    public static final String shortStackTrace(Throwable e) {
        StringBuilder sb;
        StackTraceElement[] stes;
        int steix = 0;
        sb = new StringBuilder(e.toString());
        sb.append("\n");
        stes = e.getStackTrace();
        if (stes.length < 1) {
            sb.append("\tNo stack trace information available");
        } else {
            for (StackTraceElement ste : stes) {
                if (ste.isNativeMethod() || ste.getFileName() == null || ste.getLineNumber() < 0) {
                    steix++;
                } else {
                    break;
                }
            }
            if (steix < 0) {
                sb.append("\tNo non-native methods with file name and line; ");
                sb.append("first element:\n\t");
                sb.append(stes[0].toString());
            } else {
                sb.append("\t");
                sb.append(stes[steix].toString());
            }
        }
        return sb.toString();
    }

    /** Print a brief String representation of a {@link java.lang.Throwable
	 * Throwable} (as specified by {@link #shortStackTrace}) to
	 * <tt>System.err</tt>.
	 * @param e The <tt>Throwable</tt> object */
    public static final void printShortStackTrace(Throwable e) {
        System.err.println(shortStackTrace(e));
    }

    /** Return a single String from an array of Strings, with each element
	 * separated by spaces.
	 * @param arr An array of Strings
	 * @return A single string */
    public static final String arrToStr(String[] arr) {
        return arrToStr(arr, " ");
    }

    /** Return a single String from an array of Strings, with each element
	 * separated <tt>delim</tt>.
	 * @param arr An array of Strings
	 * @param delim The delimiter (separator)
	 * @return A single string */
    public static final String arrToStr(String[] arr, String delim) {
        StringBuilder sb = new StringBuilder();
        for (String s : arr) {
            sb.append(s);
            sb.append(delim);
        }
        sb.setLength(sb.length() - delim.length());
        return sb.toString();
    }

    /** Return a single String from a List of Strings, with each element
	 * separated by spaces.
	 * @param list A List of Strings
	 * @return A single string */
    public static final String listToStr(List<String> list) {
        return listToStr(list, " ");
    }

    /** Return a single String from a List of Strings, with each element
	 * separated by <tt>delim</tt>.
	 * @param list A List of Strings
	 * @param delim The delimiter (separator) string
	 * @return A single string */
    public static final String listToStr(List<String> list, String delim) {
        if (list.size() < 1) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s);
            sb.append(delim);
        }
        sb.setLength(sb.length() - delim.length());
        return sb.toString();
    }

    /** Return a single String from a Set of Strings, with each element
	 * separated by spaces.
	 * @param set A Set of Strings
	 * @return A single string
	 * @throws NullPointerException If <tt>set</tt> is <tt>null</tt> */
    public static final String setToStr(Set<String> set) {
        return setToStr(set, " ");
    }

    /** Return a single String from a Set of Strings, with each element
	 * separated by <tt>delim</tt>.
	 * @param set A Set of Strings
	 * @param delim The delimiter (separator) string
	 * @return A single string 
	 * @throws NullPointerException If <tt>set</tt> is <tt>null</tt> */
    public static final String setToStr(Set<String> set, String delim) {
        if (set.size() < 1) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : set) {
            sb.append(s);
            sb.append(delim);
        }
        sb.setLength(sb.length() - delim.length());
        return sb.toString();
    }

    /** Obtains a system-wide locking mechanism for programs executing on a
	 * single host.
	 * @param lockname The name of the lock
	 * @return An {@link ade.ADELock ADELock} object, which can be used to
	 *   query/set the lock status
	 * @throws IOException If the lock object cannot be created/obtained */
    public static final ADELock getADELock(String lockname) throws IOException {
        return new ADELock(lockname);
    }

    /** Obtains a system-wide locking mechanism for programs executing on a
	 * single host.
	 * @param ahi Information about the host
	 * @param lockname The name of the lock
	 * @return An {@link ade.ADELock ADELock} object, which can be used to
	 *   query/set the lock status
	 * @throws IOException If the lock object cannot be created/obtained */
    public static final ADELock getADELock(ADEHostInfo ahi, String lockname) throws IOException {
        return new ADELock(ahi, lockname);
    }

    /** Confirm that a directory exists and is readable/writable.
	 * @param dir the directory
	 * @param read check readability
	 * @param write check writability
	 * @throws SecurityException
	 * @throws IOException if non-existant, unreadable, or unwritable */
    public static final void checkDir(String dir, boolean read, boolean write) throws SecurityException, IOException {
        File f = new File(dir);
        if (!f.exists()) {
            f.mkdir();
            if (read && !f.canRead()) {
                f.delete();
                throw new IOException("Cannot read from " + dir);
            }
            if (write && !f.canWrite()) {
                f.delete();
                throw new IOException("Cannot write to " + dir);
            }
        } else if (!f.isDirectory()) {
            throw new IOException(dir + " is not a directory");
        } else if (read && !f.canRead()) {
            throw new IOException("Cannot read from " + dir);
        } else if (write && !f.canWrite()) {
            throw new IOException("Cannot write to " + dir);
        }
        return;
    }

    /** Confirm that a file exists and is readable.
	 * @param file the file name
	 * @throws SecurityException
	 * @throws IOException if non-existant or unreadable */
    public static final void checkCommand(String file) throws SecurityException, IOException {
        File f = new File(file);
        if (!f.exists()) {
            throw new IOException(file + " does not exist");
        } else if (!f.isFile()) {
            throw new IOException(file + " is not a normal file");
        } else if (!f.canRead()) {
            throw new IOException("Cannot read from " + file);
        }
    }

    /** Given a path to an ADE configuration file, return a {@link
	 * java.io.File File} object if it exists or <tt>null</tt> otherwise. We
	 * assume that if a path is absolute, it is correct and do not attempt to
	 * find files located elsewhere. However, if the path is relative, we
	 * explicitly check the ADE home and configuration directories (as set in
	 * {@link ade.ADEPreferences#ADEHOME ADEHOME} and {@link
	 * ade.ADEPreferences#ADECONF ADECONF}).
	 * @param conffile The path to an ADE configuration file
	 * @return A {@link java.io.File File} object or <tt>null</tt> */
    public static final File getADEConfigFile(String conffile) {
        return getADEConfigFile(conffile, ADEPreferences.ADEHOME, ADEPreferences.ADECONF);
    }

    /** Utility to check alternate locations for an ADE configuration file.
	 * Given a path to an ADE configuration file, the ADE home directory, and
	 * the directory of ADE configuration files, return a {@link
	 * java.io.File File} object if the file exists or <tt>null</tt> otherwise.
	 * We assume that <b>if</b> a path is absolute, it is correct and do not
	 * attempt to find files located elsewhere. However, if the path is
	 * relative, we explicitly check the ADE home and configuration directories.
	 * @param cf The path to an ADE configuration file
	 * @param ah The ADE home directory
	 * @param ac The ADE configuration file directory
	 * @return A {@link java.io.File File} object or <tt>null</tt> */
    public static final File getADEConfigFile(String cf, String ah, String ac) {
        boolean foundfile = false;
        File f = new File(cf);
        if (!f.exists()) {
            if (!f.isAbsolute()) {
                String filesep = System.getProperty("file.separator");
                if ((f = relPath(ac, cf, filesep)) != null) {
                    foundfile = true;
                } else if ((f = relPath(ah, cf, filesep)) != null) {
                    foundfile = true;
                } else if ((f = relPath(ah + ac, cf, filesep)) != null) {
                    foundfile = true;
                }
            }
        } else {
            foundfile = true;
        }
        if (!foundfile) {
            return null;
        }
        return f;
    }

    /** Helper method for path checking. */
    private static File relPath(String addpath, String file, String sep) {
        File f;
        String newpath;
        if (file.startsWith("." + sep)) {
            newpath = addpath + file.substring(2);
        } else {
            newpath = addpath + file;
        }
        f = new File(newpath);
        if (f.exists()) return f;
        return null;
    }

    /** Utility to backup a file if the file exists, giving it the same name
	 * with a ".X" extension (where ".X" is a cumulative number appended to
	 * the name.
	 * @param path The directory in which the file is located
	 * @param name The file name
	 * @return The name of the backed up file (without the path; the same
	 * name is returned if backing up is unnecessary)
	 * @throws IOException */
    public static String backupFile(String path, String name) throws IOException {
        String filesep = System.getProperty("file.separator");
        File file;
        String newname;
        if (!path.endsWith(filesep)) path += filesep;
        checkDir(path, true, true);
        file = new File(path + name);
        if (file.exists()) {
            newname = nextFilename(path, name);
            file.renameTo(new File(path + newname));
        } else {
            newname = new String(name);
        }
        return newname;
    }

    /** Given a filename and directory, return an alternate filename that
	 * does not exist in the directory (by appending an extension of the form
	 * ".X", where "X" is a cumulative number). The intent of this method is
	 * to provide the means to backup a file by calling the {@link #backupFile}
	 * method.
	 * @param path The directory in which the file is located
	 * @param name The file name
	 * @return A unique file name
	 * @throws IOException */
    public static final String nextFilename(String path, String name) throws IOException {
        String filesep = System.getProperty("file.separator");
        StringBuilder filename = new StringBuilder(name);
        File dir, file;
        if (!path.endsWith(filesep)) path += filesep;
        ADEGlobals.checkDir(path, true, true);
        file = new File(path + name);
        if (file.exists()) {
            dir = new File(path);
            HashSet<String> sameroot = new HashSet<String>();
            String[] files = dir.list();
            int fileix, strix, maxfi, i = 1;
            try {
                Arrays.sort(files);
                fileix = Arrays.binarySearch(files, name);
            } catch (Exception e) {
                throw new IOException("Cannot compare " + name + " to file list");
            }
            while (files[fileix + i].startsWith(name)) {
                sameroot.add(files[fileix + i]);
                i++;
            }
            filename.append(".");
            strix = filename.length();
            maxfi = files.length - fileix;
            for (i = 1; i < maxfi; i++) {
                filename.append(i);
                if (!sameroot.contains(filename.toString())) {
                    break;
                } else {
                    if (i != (maxfi - 1)) filename.setLength(strix);
                }
            }
        }
        return filename.toString();
    }

    /** Produce a {@link java.lang.String String} representation of the
	 * given {@link java.lang.reflect.Method Method}, including parameter
	 * types. Format is: <tt>method(param1,param2,...)</tt>; note that
	 * because calls to a {@link java.lang.reflect.Method Method} use
	 * the <tt>varargs</tt> structure and parameter types must be exact
	 * matches, we convert primitives to their corresponding objects.
	 * @param m a {@link java.lang.reflect.Method Method} object
	 * @return a string representation of the method */
    public static final String getMethodString(Method m) {
        Class[] params;
        StringBuilder sb = new StringBuilder();
        sb.append(m.getName());
        sb.append("(");
        params = m.getParameterTypes();
        if (params.length > 0) {
            int i = 0;
            for (Class c : params) {
                sb.append(primitiveToObjectName(c));
                if (i != (params.length - 1)) sb.append(","); else sb.append(")");
                i++;
            }
        } else {
            sb.append(")");
        }
        return sb.toString();
    }

    /** Produce a {@link java.lang.String String} representation of the
	 * given method name that takes parameter types <tt>ps</tt>. Format
	 * is the same as the <tt>getMethodString</tt> method that takes a
	 * {@link java.lang.reflect.Method Method} as a parameter:
	 * <tt>method(param1,param2,...)</tt>.
	 * @param mn a method name
	 * @param args argument types for method <tt>mn</tt>
	 * @return a string representation of the method */
    public static final String getMethodString(String mn, Class[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append(mn);
        sb.append("(");
        if (args != null && args.length > 0) {
            int i = 0;
            for (Class c : args) {
                sb.append(c.getName());
                if (i != (args.length - 1)) sb.append(","); else sb.append(")");
                i++;
            }
        } else {
            sb.append(")");
        }
        return sb.toString();
    }

    /** Produce a {@link java.lang.String String} representation of the
	 * given method name that takes example parameters <tt>args</tt>. Format
	 * is the same as the <tt>getMethodString</tt> method that takes a
	 * {@link java.lang.reflect.Method Method} as a parameter:
	 * <tt>method(param1,param2,...)</tt>.
	 * @param mn a method name
	 * @param args names of parameter types for method <tt>mn</tt>
	 * @return a string representation of the method */
    public static final String getMethodString(String mn, String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append(mn);
        sb.append("(");
        if (args != null && args.length > 0) {
            int i = 0;
            for (String s : args) {
                sb.append(s);
                if (i != (args.length - 1)) sb.append(","); else sb.append(")");
                i++;
            }
        } else {
            sb.append(")");
        }
        return sb.toString();
    }

    /** Produce a {@link java.lang.String String} representation of the
	 * given method name that takes example parameters <tt>args</tt>. Format
	 * is the same as the <tt>getMethodString</tt> method that takes a
	 * {@link java.lang.reflect.Method Method} as a parameter:
	 * <tt>method(param1,param2,...)</tt>.
	 * @param mn a method name
	 * @param args example arguments for method <tt>mn</tt>; values are
	 * ignored, as only the type is considered
	 * @return a string representation of the method */
    public static final String getMethodString(String mn, Object... args) {
        StringBuilder sb = new StringBuilder(mn);
        sb.append("(");
        if (args != null && args.length > 0) {
            int i = 0;
            for (Object o : args) {
                sb.append(o.getClass().getName());
                if (i != (args.length - 1)) sb.append(","); else sb.append(")");
                i++;
            }
        } else {
            sb.append(")");
        }
        return sb.toString();
    }

    /** Return the name of a given Class, converting primitive types
	 * to their corresponding Objects.
	 * @param c the {@link java.lang.Class Class}
	 * @return the name of {@link java.lang.Class Class} <tt>c</tt> */
    public static final String primitiveToObjectName(Class c) {
        if (c.isPrimitive()) {
            String s = c.getName();
            if (s.equals("int")) {
                return "java.lang.Integer";
            } else if (s.equals("double")) {
                return "java.lang.Double";
            } else if (s.equals("boolean")) {
                return "java.lang.Boolean";
            } else if (s.equals("byte")) {
                return "java.lang.Byte";
            } else if (s.equals("char")) {
                return "java.lang.Character";
            } else if (s.equals("long")) {
                return "java.lang.Long";
            } else if (s.equals("short")) {
                return "java.lang.Short";
            } else if (s.equals("float")) {
                return "java.lang.Float";
            } else {
                return "";
            }
        }
        return c.getName();
    }

    /** Return the name of a Class, converting Object type names of
	 * primitives to their primitive names.
	 * @param c the {@link java.lang.Class Class}
	 * @return the name of the (possibly primitive) {@link java.lang.Class
	 * Class} */
    public static final String objectToPrimitiveName(Class c) {
        String s = c.getName();
        if (c.equals("java.lang.Integer")) {
            return "int";
        } else if (c.equals("java.lang.Double")) {
            return "double";
        } else if (c.equals("java.lang.Boolean")) {
            return "boolean";
        } else if (c.equals("java.lang.Byte")) {
            return "byte";
        } else if (c.equals("java.lang.Character")) {
            return "char";
        } else if (c.equals("java.lang.Long")) {
            return "long";
        } else if (c.equals("java.lang.Short")) {
            return "short";
        } else if (c.equals("java.lang.Float")) {
            return "float";
        } else if (c.equals("java.lang.Void")) {
            return "";
        }
        return s;
    }
}
