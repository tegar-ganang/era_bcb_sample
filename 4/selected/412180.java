package com.hifi.core.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public class OSUtils {

    /** 
	* Variable for whether or not we're on Windows.
	*/
    private static boolean _isWindows = false;

    /** 
	 * Variable for whether or not we're on Windows NT.
	 */
    private static boolean _isWindowsNT = false;

    /** 
	 * Variable for whether or not we're on Windows XP.
	 */
    private static boolean _isWindowsXP = false;

    /** 
	 * Variable for whether or not we're on Windows NT, 2000, or XP.
	 */
    private static boolean _isWindowsNTor2000orXP = false;

    /** 
	 * Variable for whether or not we're on 2000 or XP.
	 */
    private static boolean _isWindows2000orXP = false;

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
     * Variable for whether or not we're on OS/2.
     */
    private static boolean _isOS2 = false;

    /**
     * Several arrays of illegal characters on various operating systems.
     * Used by convertFileName
     */
    private static final char[] ILLEGAL_CHARS_ANY_OS = { '/', '\n', '\r', '\t', '\0', '\f' };

    private static final char[] ILLEGAL_CHARS_UNIX = { '`' };

    private static final char[] ILLEGAL_CHARS_WINDOWS = { '?', '*', '\\', '<', '>', '|', '\"', ':' };

    private static final char[] ILLEGAL_CHARS_MACOS = { ':' };

    /**
	 * Constant for the current running directory.
	 */
    private static final File CURRENT_DIRECTORY = new File(System.getProperty("user.dir"));

    /**
     * Variable for the settings directory.
     */
    static File SETTINGS_DIRECTORY = null;

    /**
	 * Make sure the constructor can never be called.
	 */
    private OSUtils() {
    }

    /**
	 * Initialize the settings statically. 
	 */
    static {
        setOperatingSystems();
    }

    /**
	 * Sets the operating system variables.
	 */
    private static void setOperatingSystems() {
        _isWindows = false;
        _isWindowsNTor2000orXP = false;
        _isWindows2000orXP = false;
        _isWindowsNT = false;
        _isWindowsXP = false;
        _isWindows95 = false;
        _isWindows98 = false;
        _isWindowsMe = false;
        _isSolaris = false;
        _isLinux = false;
        _isOS2 = false;
        _isMacOSX = false;
        String os = System.getProperty("os.name").toLowerCase(Locale.US);
        _isWindows = os.indexOf("windows") != -1;
        if (os.indexOf("windows nt") != -1 || os.indexOf("windows 2000") != -1 || os.indexOf("windows xp") != -1) _isWindowsNTor2000orXP = true;
        if (os.indexOf("windows 2000") != -1 || os.indexOf("windows xp") != -1) _isWindows2000orXP = true;
        if (os.indexOf("windows nt") != -1) _isWindowsNT = true;
        if (os.indexOf("windows xp") != -1) _isWindowsXP = true;
        if (os.indexOf("windows 95") != -1) _isWindows95 = true;
        if (os.indexOf("windows 98") != -1) _isWindows98 = true;
        if (os.indexOf("windows me") != -1) _isWindowsMe = true;
        _isSolaris = os.indexOf("solaris") != -1;
        _isLinux = os.indexOf("linux") != -1;
        _isOS2 = os.indexOf("os/2") != -1;
        if (_isWindows || _isLinux) _supportsTray = true;
        if (os.startsWith("mac os")) {
            if (os.endsWith("x")) {
                _isMacOSX = true;
            }
        }
    }

    /**
	 * Returns the version of java we're using.
	 */
    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }

    /**
	 * Returns the operating system.
	 */
    public static String getOS() {
        return System.getProperty("os.name");
    }

    /**
	 * Returns the operating system version.
	 */
    public static String getOSVersion() {
        return System.getProperty("os.version");
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
        if (isLinux()) return false;
        return _supportsTray;
    }

    /**
	 * Returns whether or not this operating system is considered
	 * capable of meeting the requirements of a ultrapeer.
	 *
	 * @return <tt>true</tt> if this OS meets ultrapeer requirements,
	 *         <tt>false</tt> otherwise
	 */
    public static boolean isUltrapeerOS() {
        return !(_isWindows98 || _isWindows95 || _isWindowsMe || _isWindowsNT);
    }

    /**
	 * @return true if this is a well-supported version of windows.
	 * (not 95, 98, nt or me)
	 */
    public static boolean isGoodWindows() {
        return isWindows() && isUltrapeerOS();
    }

    /**
	 * Returns whether or not the OS is some version of Windows.
	 *
	 * @return <tt>true</tt> if the application is running on some Windows 
	 *         version, <tt>false</tt> otherwise
	 */
    public static boolean isWindows() {
        return _isWindows;
    }

    /**
	 * Returns whether or not the OS is Windows NT, 2000, or XP.
	 *
	 * @return <tt>true</tt> if the application is running on Windows NT,
	 *  2000, or XP <tt>false</tt> otherwise
	 */
    public static boolean isWindowsNTor2000orXP() {
        return _isWindowsNTor2000orXP;
    }

    /**
	 * Returns whether or not the OS is 2000 or XP.
	 *
	 * @return <tt>true</tt> if the application is running on 2000 or XP,
	 *  <tt>false</tt> otherwise
	 */
    public static boolean isWindows2000orXP() {
        return _isWindows2000orXP;
    }

    /**
	 * Returns whether or not the OS is WinXP.
	 *
	 * @return <tt>true</tt> if the application is running on WinXP,
	 *  <tt>false</tt> otherwise
	 */
    public static boolean isWindowsXP() {
        return _isWindowsXP;
    }

    /**
     * Returns whether or not the OS is OS/2.
     *
     * @return <tt>true</tt> if the application is running on OS/2,
     *         <tt>false</tt> otherwise
     */
    public static boolean isOS2() {
        return _isOS2;
    }

    /** 
	 * Returns whether or not the OS is Mac OSX.
	 *
	 * @return <tt>true</tt> if the application is running on Mac OSX, 
	 *         <tt>false</tt> otherwise
	 */
    public static boolean isMacOSX() {
        return _isMacOSX;
    }

    /**
     * Returns whether or not the Cocoa Foundation classes are available.
     */
    public static boolean isCocoaFoundationAvailable() {
        if (!isMacOSX()) return false;
        try {
            Class.forName("com.apple.cocoa.foundation.NSUserDefaults");
            Class.forName("com.apple.cocoa.foundation.NSMutableDictionary");
            Class.forName("com.apple.cocoa.foundation.NSMutableArray");
            Class.forName("com.apple.cocoa.foundation.NSObject");
            Class.forName("com.apple.cocoa.foundation.NSSystem");
            return true;
        } catch (ClassNotFoundException error) {
            return false;
        } catch (NoClassDefFoundError error) {
            return false;
        }
    }

    /** 
	 * Returns whether or not the OS is any Mac OS.
	 *
	 * @return <tt>true</tt> if the application is running on Mac OSX
	 *  or any previous mac version, <tt>false</tt> otherwise
	 */
    public static boolean isAnyMac() {
        return _isMacOSX;
    }

    /** 
	 * Returns whether or not the OS is Solaris.
	 *
	 * @return <tt>true</tt> if the application is running on Solaris, 
	 *         <tt>false</tt> otherwise
	 */
    public static boolean isSolaris() {
        return _isSolaris;
    }

    /** 
	 * Returns whether or not the OS is Linux.
	 *
	 * @return <tt>true</tt> if the application is running on Linux, 
	 *         <tt>false</tt> otherwise
	 */
    public static boolean isLinux() {
        return _isLinux;
    }

    /** 
	 * Returns whether or not the OS is some version of
	 * Unix, defined here as only Solaris or Linux.
	 */
    public static boolean isUnix() {
        return _isLinux || _isSolaris;
    }

    /**
	 * Returns whether the OS is POSIX-like. 
	 */
    public static boolean isPOSIX() {
        return _isLinux || _isSolaris || _isMacOSX;
    }

    /**
	 * Returns whether or not the current JVM is 1.4.x or later
	 *
	 * @return <tt>true</tt> if we are running on 1.4.x or later, 
     *  <tt>false</tt> otherwise
	 */
    public static boolean isJava14OrLater() {
        String version = getJavaVersion();
        return !version.startsWith("1.3") && !version.startsWith("1.2") && !version.startsWith("1.1") && !version.startsWith("1.0");
    }

    /**
	 * Returns whether or not the current JVM is 1.4.x or later
	 *
	 * @return <tt>true</tt> if we are running on 1.4.x or later, 
     *  <tt>false</tt> otherwise
	 */
    public static boolean isJava142OrLater() {
        String version = getJavaVersion();
        return !version.startsWith("1.4.1") && !version.startsWith("1.4.0") && isJava14OrLater();
    }

    /**
	 * Returns whether or not the current JVM is 1.5.x or later.
	 */
    public static boolean isJava15OrLater() {
        String version = getJavaVersion();
        return !version.startsWith("1.4") && !version.startsWith("1.3") && !version.startsWith("1.2") && !version.startsWith("1.1") && !version.startsWith("1.0");
    }

    /**
	 * Returns whether or not the current JVM is 1.6.x or later.
	 */
    public static boolean isJava16OrLater() {
        String version = getJavaVersion();
        return !version.startsWith("1.5") && !version.startsWith("1.4") && !version.startsWith("1.3") && !version.startsWith("1.2") && !version.startsWith("1.1") && !version.startsWith("1.0");
    }

    /**
     * Determines if your version of java is out of date.
     */
    public static boolean isJavaOutOfDate() {
        return isWindows() && !isSpecificJRE() && (getJavaVersion().startsWith("1.3") || getJavaVersion().startsWith("1.4.0"));
    }

    /**
     * Determines if this was loaded from a specific JRE.
     */
    public static boolean isSpecificJRE() {
        return new File(".", "jre").isDirectory();
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
        return new File(System.getProperty("user.home"));
    }

    /**
     * Return the user's name.
     *
     * @return the <tt>String</tt> denoting the user's name.
     */
    public static String getUserName() {
        return System.getProperty("user.name");
    }

    /** 
     * Replaces OS specific illegal characters from any filename with '_', 
	 * including ( / \n \r \t ) on all operating systems, ( ? * \  < > | " ) 
	 * on Windows, ( ` ) on unix.
     *
     * @param name the filename to check for illegal characters
     * @return String containing the cleaned filename
     */
    public static String convertFileName(String name) {
        if (name.length() > 180) {
            int extStart = name.lastIndexOf('.');
            if (extStart == -1) {
                name = name.substring(0, 180);
            } else {
                int extLength = name.length() - extStart;
                int extEnd = extLength > 11 ? extStart + 11 : name.length();
                name = name.substring(0, 180 - extLength) + name.substring(extStart, extEnd);
            }
        }
        for (int i = 0; i < ILLEGAL_CHARS_ANY_OS.length; i++) name = name.replace(ILLEGAL_CHARS_ANY_OS[i], '_');
        if (_isWindows || _isOS2) {
            for (int i = 0; i < ILLEGAL_CHARS_WINDOWS.length; i++) name = name.replace(ILLEGAL_CHARS_WINDOWS[i], '_');
        } else if (_isLinux || _isSolaris) {
            for (int i = 0; i < ILLEGAL_CHARS_UNIX.length; i++) name = name.replace(ILLEGAL_CHARS_UNIX[i], '_');
        } else if (_isMacOSX) {
            for (int i = 0; i < ILLEGAL_CHARS_MACOS.length; i++) name = name.replace(ILLEGAL_CHARS_MACOS[i], '_');
        }
        return name;
    }

    /**
	 * Converts a value in seconds to:
	 *     "d:hh:mm:ss" where d=days, hh=hours, mm=minutes, ss=seconds, or
	 *     "h:mm:ss" where h=hours<24, mm=minutes, ss=seconds, or
	 *     "m:ss" where m=minutes<60, ss=seconds
	 */
    public static String seconds2time(int seconds) {
        int minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        int hours = minutes / 60;
        minutes = minutes - hours * 60;
        int days = hours / 24;
        hours = hours - days * 24;
        StringBuilder time = new StringBuilder();
        if (days != 0) {
            time.append(Integer.toString(days));
            time.append(":");
            if (hours < 10) time.append("0");
        }
        if (days != 0 || hours != 0) {
            time.append(Integer.toString(hours));
            time.append(":");
            if (minutes < 10) time.append("0");
        }
        time.append(Integer.toString(minutes));
        time.append(":");
        if (seconds < 10) time.append("0");
        time.append(Integer.toString(seconds));
        return time.toString();
    }
}
