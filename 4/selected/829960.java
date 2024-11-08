package net.sf.mailsomething.util;

import java.util.Properties;
import java.io.*;

/**
 * This class handles common utility functions that many classes
 * may want to access.
 * 
 * Copied from limewire.org. I have kept some of the methods, since I think
 * they are good for ideas as how limewire handles theese things.
 * 
 */
public final class CommonUtils {

    /** 
	 * Constant for the java system properties.
	 */
    private static final Properties PROPS = System.getProperties();

    /** 
	 * Variable for whether or not we're on Windows.
	 */
    private static boolean _isWindows = false;

    /** 
	 * Variable for whether or not we're on Windows NT, 2000, or XP.
	 */
    private static boolean _isWindowsNTor2000orXP = false;

    /** 
	 * Variable for whether or not we're on Windows 95.
	 */
    private static boolean _isWindows95 = false;

    /** 
	 * Variable for whether or not we're on Windows 98.
	 */
    private static boolean _isWindows98 = false;

    /** 
	 * Variable for whether or not we're on Windows Me.
	 */
    private static boolean _isWindowsMe = false;

    /** 
	 * Variable for whether or not the operating system allows the 
	 * application to be reduced to the system tray.
	 */
    private static boolean _supportsTray = false;

    /**
	 * Variable for whether or not we're on Mac 9.1 or below.
	 */
    private static boolean _isMacClassic = false;

    /** 
	 * Variable for whether or not we're on MacOSX.
	 */
    private static boolean _isMacOSX = false;

    /** 
	 * Variable for whether or not we're on Linux.
	 */
    private static boolean _isLinux = false;

    /** 
	 * Variable for whether or not we're on Solaris.
	 */
    private static boolean _isSolaris = false;

    /**
	 * Variable for whether or not the JVM is 1.1.8.
	 */
    private static boolean _isJava118 = false;

    /**
	 * Constant for the current running directory.
	 */
    private static final File CURRENT_DIRECTORY = new File(PROPS.getProperty("user.dir"));

    /**
     * Variable for whether or this this is a PRO version of LimeWire. 
     */
    private static boolean _isPro = false;

    /**
	 * Make sure the constructor can never be called.
	 */
    private CommonUtils() {
    }

    /**
	 * Initialize the settings statically. 
	 */
    static {
        String os = System.getProperty("os.name").toLowerCase();
        _isWindows = os.indexOf("windows") != -1;
        if (os.indexOf("windows nt") != -1 || os.indexOf("windows 2000") != -1 || os.indexOf("windows xp") != -1) _isWindowsNTor2000orXP = true;
        if (os.indexOf("windows 95") != -1) _isWindows95 = true;
        if (os.indexOf("windows 98") != -1) _isWindows98 = true;
        if (os.indexOf("windows me") != -1) _isWindowsMe = true;
        if (_isWindows) _supportsTray = true;
        _isSolaris = os.indexOf("solaris") != -1;
        _isLinux = os.indexOf("linux") != -1;
        if (os.startsWith("mac os")) {
            if (os.endsWith("x")) {
                _isMacOSX = true;
            } else {
                _isMacClassic = true;
            }
        }
        if (CommonUtils.getJavaVersion().startsWith("1.1.8")) {
            _isJava118 = true;
        }
    }

    static int getMinorVersionNumberInternal(String version) {
        if (!version.equals("@version@")) {
            try {
                int firstDot = version.indexOf(".");
                String minusMajor = version.substring(firstDot + 1);
                int secondDot = minusMajor.indexOf(".");
                String minorStr = minusMajor.substring(0, secondDot);
                return new Integer(minorStr).intValue();
            } catch (NumberFormatException nfe) {
            }
        }
        return 7;
    }

    /**
	 * Returns the version of java we're using.
	 */
    public static String getJavaVersion() {
        return PROPS.getProperty("java.version");
    }

    /**
	 * Returns the operating system.
	 */
    public static String getOS() {
        return PROPS.getProperty("os.name");
    }

    /**
	 * Returns the operating system version.
	 */
    public static String getOSVersion() {
        return PROPS.getProperty("os.version");
    }

    /**
	 * Returns the user's current working directory as a <tt>File</tt>
	 * instance, or <tt>null</tt> if the property is not set.
	 *
	 * @return the user's current working directory as a <tt>File</tt>
	 *  instance, or <tt>null</tt> if the property is not set
	 */
    public static File getCurrentDirectory() {
        return CURRENT_DIRECTORY;
    }

    /**
     * Returns true if this is Windows NT or Windows 2000 and
	 * hence can support a system tray feature.
     */
    public static boolean supportsTray() {
        return _supportsTray;
    }

    /**
	 * Returns whether or not this operating system is considered
	 * capable of meeting the requirements of a supernode.
	 *
	 * @return <tt>true</tt> if this os meets supernode requirements,
	 *         <tt>false</tt> otherwise
	 */
    public static boolean isSupernodeOS() {
        if (_isWindows98 || _isWindows95 || _isWindowsMe || _isMacClassic) {
            return false;
        }
        return true;
    }

    /**
	 * Returns whether or not the os is some version of Windows.
	 *
	 * @return <tt>true</tt> if the application is running on some Windows 
	 *         version, <tt>false</tt> otherwise
	 */
    public static boolean isWindows() {
        return _isWindows;
    }

    /**
	 * Returns whether or not the os is some version of Windows.
	 *
	 * @return <tt>true</tt> if the application is running on Windows NT 
	 *         or 2000, <tt>false</tt> otherwise
	 */
    public static boolean isWindowsNTor2000orXP() {
        return _isWindowsNTor2000orXP;
    }

    /** 
	 * Returns whether or not the os is Mac 9.1 or earlier.
	 *
	 * @return <tt>true</tt> if the application is running on a Mac version
	 *         prior to OSX, <tt>false</tt> otherwise
	 */
    public static boolean isMacClassic() {
        return _isMacClassic;
    }

    /** 
	 * Returns whether or not the os is Mac OSX.
	 *
	 * @return <tt>true</tt> if the application is running on Mac OSX, 
	 *         <tt>false</tt> otherwise
	 */
    public static boolean isMacOSX() {
        return _isMacOSX;
    }

    /** 
	 * Returns whether or not the os is Mac OSX 10.2 or above.
	 *
	 * @return <tt>true</tt> if the application is running on Mac OSX, 
	 *  10.2 or above, <tt>false</tt> otherwise
	 */
    public static boolean isJaguarOrAbove() {
        if (!isMacOSX()) return false;
        return getOSVersion().startsWith("10.2");
    }

    /** 
	 * Returns whether or not the os is any Mac os.
	 *
	 * @return <tt>true</tt> if the application is running on Mac OSX
	 *  or any previous mac version, <tt>false</tt> otherwise
	 */
    public static boolean isAnyMac() {
        return _isMacClassic || _isMacOSX;
    }

    /** 
	 * Returns whether or not the os is Solaris.
	 *
	 * @return <tt>true</tt> if the application is running on Solaris, 
	 *         <tt>false</tt> otherwise
	 */
    public static boolean isSolaris() {
        return _isSolaris;
    }

    /** 
	 * Returns whether or not the os is Linux.
	 *
	 * @return <tt>true</tt> if the application is running on Linux, 
	 *         <tt>false</tt> otherwise
	 */
    public static boolean isLinux() {
        return _isLinux;
    }

    /** 
	 * Returns whether or not the os is some version of
	 * Unix, defined here as only Solaris or Linux.
	 */
    public static boolean isUnix() {
        return _isLinux || _isSolaris;
    }

    /**
	 * Returns whether or not the current JVM is a 1.1.8 implementation.
	 *
	 * @return <tt>true</tt> if we are running on 1.1.8, <tt>false</tt>
	 *  otherwise
	 */
    public static boolean isJava118() {
        return _isJava118;
    }

    /**
	 * Returns whether or not the current JVM is 1.3.x or later
	 *
	 * @return <tt>true</tt> if we are running on 1.3.x or later, 
     *  <tt>false</tt> otherwise
	 */
    public static boolean isJava13OrLater() {
        String version = CommonUtils.getJavaVersion();
        return !version.startsWith("1.2") && !version.startsWith("1.1") && !version.startsWith("1.0");
    }

    /**
	 * Returns whether or not the current JVM is 1.4.x or later
	 *
	 * @return <tt>true</tt> if we are running on 1.4.x or later, 
     *  <tt>false</tt> otherwise
	 */
    public static boolean isJava14OrLater() {
        String version = CommonUtils.getJavaVersion();
        return !version.startsWith("1.3") && !version.startsWith("1.2") && !version.startsWith("1.1") && !version.startsWith("1.0");
    }

    /** 
	 * Attempts to copy the first 'amount' bytes of file 'src' to 'dst',
	 * returning the number of bytes actually copied.  If 'dst' already exists,
	 * the copy may or may not succeed.
     * 
     * @param src the source file to copy
     * @param amount the amount of src to copy, in bytes
     * @param dst the place to copy the file
     * @return the number of bytes actually copied.  Returns 'amount' if the
     *  entire requested range was copied.
     */
    public static int copy(File src, int amount, File dst) {
        final int BUFFER_SIZE = 1024;
        int amountToRead = amount;
        boolean ok = true;
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new BufferedInputStream(new FileInputStream(src));
            out = new BufferedOutputStream(new FileOutputStream(dst));
            byte[] buf = new byte[BUFFER_SIZE];
            while (amountToRead > 0) {
                int read = in.read(buf, 0, Math.min(BUFFER_SIZE, amountToRead));
                if (read == -1) break;
                amountToRead -= read;
                out.write(buf, 0, read);
            }
        } catch (IOException e) {
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
            }
            if (out != null) {
                try {
                    out.flush();
                } catch (IOException e) {
                }
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
        return amount - amountToRead;
    }

    /** 
	 * Copies the file 'src' to 'dst', returning true iff the copy succeeded.
     * If 'dst' already exists, the copy may or may not succeed.  May also
     * fail for VERY large source files.
	 */
    public static boolean copy(File src, File dst) {
        long length = src.length();
        return copy(src, (int) length, dst) == length;
    }

    /**
     * Returns the user home directory.
     *
     * @return the <tt>File</tt> instance denoting the abstract pathname of
     *  the user's home directory, or <tt>null</tt> if the home directory
	 *  does not exist
     */
    public static File getUserHomeDir() {
        return new File(PROPS.getProperty("user.home"));
    }

    /**
	 * Copies the specified resource file into the current directory from
	 * the jar file. If the file already exists, no copy is performed.
	 *
	 * @param fileName the name of the file to copy, relative to the jar 
	 *  file -- such as "com/limegroup/gnutella/gui/images/image.gif"
	 */
    public static void copyResourceFile(final String fileName) {
        copyResourceFile(fileName, null, false);
    }

    /**
	 * Copies the specified resource file into the current directory from
	 * the jar file. If the file already exists, no copy is performed.
	 *
	 * @param fileName the name of the file to copy, relative to the jar 
	 *  file -- such as "com/limegroup/gnutella/gui/images/image.gif"
	 */
    public static void copyResourceFile(final String fileName, File newFile, final boolean forceOverwrite) {
        if (newFile == null) newFile = new File(fileName);
        if (!forceOverwrite && newFile.exists()) return;
        String parentString = newFile.getParent();
        if (parentString == null) return;
        File parentFile = new File(parentString);
        if (!parentFile.isDirectory()) {
            parentFile.mkdirs();
        }
        ClassLoader cl = CommonUtils.class.getClassLoader();
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            InputStream is = cl != null ? cl.getResource(fileName).openStream() : ClassLoader.getSystemResource(fileName).openStream();
            final int bufferSize = 2048;
            bis = new BufferedInputStream(is, bufferSize);
            bos = new BufferedOutputStream(new FileOutputStream(newFile), bufferSize);
            byte[] buffer = new byte[bufferSize];
            int c = 0;
            do {
                c = bis.read(buffer, 0, bufferSize);
                bos.write(buffer, 0, c);
            } while (c == bufferSize);
        } catch (Exception e) {
            newFile.delete();
        } finally {
            try {
                if (bis != null) bis.close();
                if (bos != null) bos.close();
            } catch (IOException ioe) {
            }
        }
    }
}
