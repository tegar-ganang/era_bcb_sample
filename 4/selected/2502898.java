package gmgen.util;

import pcgen.core.Globals;
import pcgen.util.Logging;
import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.Cursor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 *  Misc Utilities, all static.  Will create and store a private static instance
 *  of itself that reads in properties and localization strings
 *
 *@author     Devon Jones
 *@since        GMGen 3.3
 */
public final class MiscUtilities {

    private static Properties localization;

    static {
        localization = new Properties();
    }

    private MiscUtilities() {
    }

    /**
	 *  Returns if the specified path name is an absolute path or URL.
	 *
	 *@param  path  a path
	 *@return       The absolute path
	 *@since        GMGen 3.3
	 */
    public static boolean isAbsolutePath(String path) {
        if (isURL(path)) {
            return true;
        } else if (path.startsWith("~/") || path.startsWith("~" + File.separator) || path.equals("~")) {
            return true;
        } else if (OperatingSystem.isDOSDerived()) {
            if ((path.length() == 2) && (path.charAt(1) == ':')) {
                return true;
            }
            if ((path.length() > 2) && (path.charAt(1) == ':') && (path.charAt(2) == '\\')) {
                return true;
            }
            if (path.startsWith("\\\\")) {
                return true;
            }
        } else if (OperatingSystem.isUnix()) {
            if ((path.length() > 0) && (path.charAt(0) == '/')) {
                return true;
            }
        }
        return false;
    }

    /**
	 *  Returns the file name component of the specified path.
	 *
	 *@param  path  The path
	 *@return       The file name
	 *@since        GMGen 3.3
	 *
	 * @deprecated Unused
	 */
    public static String getFileName(String path) {
        if (path.equals("/")) {
            return path;
        }
        if (path.endsWith("/") || path.endsWith(File.separator)) {
            path = path.substring(0, path.length() - 1);
        }
        int index = Math.max(path.lastIndexOf('/'), path.lastIndexOf(File.separatorChar));
        if (index == -1) {
            index = path.indexOf(':');
        }
        if ((index == -1) || (index == (path.length() - 1))) {
            return path;
        }
        return path.substring(index + 1);
    }

    /**
	 *  Returns the localization property with the specified name, formatting it
	 *  with the <code>java.text.MessageFormat.format()</code> method.
	 *
	 *@param  name  The localization property
	 *@param  args  The positional parameters
	 *@return       The localization value
	 *@since        GMGen 3.3
	 */
    public static final String getLocalization(String name, Object[] args) {
        if (name == null) {
            return null;
        }
        if (args == null) {
            return localization.getProperty(name);
        }
        String value = localization.getProperty(name);
        if (value == null) {
            return null;
        }
        return MessageFormat.format(value, args);
    }

    /**
	 *  Set the cursor for the specified component to the wait cursor
	 *
	 *@param  component  The component to set the cursor for
	 *@return        The currently set cursor
	 */
    public static Cursor setBusyCursor(java.awt.Component component) {
        Cursor old = component.getCursor();
        component.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        return old;
    }

    /**
	 *  Set the cursor the the specified component to the specified cursor
	 *
	 *@param  component  The component to set the cursor for
	 *@param  cursor  The cursor to set
	 */
    public static void setCursor(java.awt.Component component, Cursor cursor) {
        component.setCursor(cursor);
    }

    /**
	 *  Sets the any property values that need to be set on close
	 *@since        GMGen 3.3
	 *
	 * @deprecated Unused
	 */
    public static void setOptionsProperties() {
    }

    /**
	 *  Returns the parent of the specified path.
	 *
	 *@param  path  The path name
	 *@return       The parentOfPath value
	 *@since        GMGen 3.3
	 */
    public static String getParentOfPath(String path) {
        int count = Math.max(0, path.length() - 2);
        int index = path.lastIndexOf(File.separatorChar, count);
        if (index == -1) {
            index = path.lastIndexOf('/', count);
        }
        if (index == -1) {
            index = path.lastIndexOf(':');
        }
        return path.substring(0, index + 1);
    }

    /**
	 *  Checks if the specified string is a URL.
	 *
	 *@param  str  The string to check
	 *@return      True if the string is a URL, false otherwise
	 *@since       GMGen 3.3
	 */
    public static boolean isURL(String str) {
        int fsIndex = Math.max(str.indexOf(File.separatorChar), str.indexOf('/'));
        if (fsIndex == 0) {
            return false;
        } else if (fsIndex == 2) {
            return false;
        }
        int cIndex = str.indexOf(':');
        if (cIndex <= 1) {
            return false;
        } else if ((fsIndex != -1) && (cIndex > fsIndex)) {
            return false;
        }
        return true;
    }

    /**
	 *  Converts an internal version number (build) into a `human-readable' form.
	 *
	 *@param  build  The build number
	 *@return        The Formatted Version Number
	 */
    public static String buildToVersion(String build) {
        StringTokenizer bt = new StringTokenizer(build, ".");
        int major = 0;
        if (bt.hasMoreTokens()) {
            major = Integer.parseInt(bt.nextToken());
        }
        int minor = 0;
        if (bt.hasMoreTokens()) {
            minor = Integer.parseInt(bt.nextToken());
        }
        int beta = 0;
        if (bt.hasMoreTokens()) {
            beta = Integer.parseInt(bt.nextToken());
        }
        int rc = 0;
        if (bt.hasMoreTokens()) {
            rc = Integer.parseInt(bt.nextToken());
        }
        int bugfix = 0;
        if (bt.hasMoreTokens()) {
            bugfix = Integer.parseInt(bt.nextToken());
        }
        return "" + major + "." + minor + ((beta != 99) ? ("pre" + beta) : ((rc != 99) ? ("rc" + rc) : ((bugfix != 0) ? ("." + bugfix) : "final")));
    }

    /**
	 *  Returns the canonical form of the specified path name. Currently only
	 *  expands a leading <code>~</code>. <b>For local path names only.</b>
	 *
	 *@param  path  The path name
	 *@return       the canonical form of the specified path name
	 *@since        GMGen 3.3
	 */
    public static String canonPath(String path) {
        if (path.startsWith("file://")) {
            path = path.substring("file://".length());
        } else if (path.startsWith("file:")) {
            path = path.substring("file:".length());
        } else if (isURL(path)) {
            return path;
        }
        if (File.separatorChar == '\\') {
            path = path.replace('/', '\\');
        }
        if (path.startsWith("~" + File.separator)) {
            path = path.substring(2);
            String home = System.getProperty("user.home");
            if (home.endsWith(File.separator)) {
                return home + path;
            }
            return home + File.separator + path;
        } else if (path.equals("~")) {
            return System.getProperty("user.home");
        } else {
            return path;
        }
    }

    /**
	 *  Converts a class name to a file name. All periods are replaced with slashes
	 *  and the '.class' extension is added.
	 *
	 *@param  name  The class name
	 *@return       the file name
	 *@since        GMGen 3.3
	 */
    public static String classToFile(String name) {
        return name.replace('.', '/').concat(".class");
    }

    /**
	 *  A more intelligent version of String.compareTo() that handles numbers
	 *  specially. For example, it places "My file 2" before "My file 10".
	 *
	 *@param  str1        The first string
	 *@param  str2        The second string
	 *@param  ignoreCase  If true, case will be ignored
	 *@return             negative If str1 &lt; str2, 0 if both are the same,
	 *                    positive if str1 &gt; str2
	 */
    public static int compareStrings(String str1, String str2, boolean ignoreCase) {
        char[] char1 = str1.toCharArray();
        char[] char2 = str2.toCharArray();
        int len = Math.min(char1.length, char2.length);
        for (int i = 0, j = 0; (i < len) && (j < len); i++, j++) {
            char ch1 = char1[i];
            char ch2 = char2[j];
            if (Character.isDigit(ch1) && Character.isDigit(ch2) && (ch1 != '0') && (ch2 != '0')) {
                int _i = i + 1;
                int _j = j + 1;
                for (; _i < char1.length; _i++) {
                    if (!Character.isDigit(char1[_i])) {
                        break;
                    }
                }
                for (; _j < char2.length; _j++) {
                    if (!Character.isDigit(char2[_j])) {
                        break;
                    }
                }
                int len1 = _i - i;
                int len2 = _j - j;
                if (len1 > len2) {
                    return 1;
                } else if (len1 < len2) {
                    return -1;
                } else {
                    for (int k = 0; k < len1; k++) {
                        ch1 = char1[i + k];
                        ch2 = char2[j + k];
                        if (ch1 != ch2) {
                            return ch1 - ch2;
                        }
                    }
                }
                i = _i - 1;
                j = _j - 1;
            } else {
                if (ignoreCase) {
                    ch1 = Character.toLowerCase(ch1);
                    ch2 = Character.toLowerCase(ch2);
                }
                if (ch1 != ch2) {
                    return ch1 - ch2;
                }
            }
        }
        return char1.length - char2.length;
    }

    /**
	 *  Constructs an absolute path name from a directory and another path name.
	 *  This method is VFS-aware.
	 *
	 *@param  parent  The directory
	 *@param  path    The path name
	 *@return         the absolute path name
	 *@since          GMGen 3.3
	 */
    public static String constructPath(String parent, String path) {
        if (isAbsolutePath(path)) {
            return canonPath(path);
        }
        if (OperatingSystem.isDOSDerived()) {
            if ((path.length() == 2) && (path.charAt(1) == ':')) {
                return path;
            } else if ((path.length() > 2) && (path.charAt(1) == ':') && (path.charAt(2) != '\\')) {
                path = path.substring(0, 2) + '\\' + path.substring(2);
                return canonPath(path);
            }
        }
        String dd = ".." + File.separator;
        String d = "." + File.separator;
        if (parent == null) {
            parent = Globals.getDefaultPath();
        }
        for (; ; ) {
            if (path.equals(".")) {
                return parent;
            } else if (path.equals("..")) {
                return getParentOfPath(parent);
            } else if (path.startsWith(dd) || path.startsWith("../")) {
                parent = getParentOfPath(parent);
                path = path.substring(3);
            } else if (path.startsWith(d)) {
                path = path.substring(2);
            } else {
                break;
            }
        }
        if (OperatingSystem.isDOSDerived() && path.startsWith("\\")) {
            parent = parent.substring(0, 2);
        }
        if (!path.endsWith("\\") && !path.endsWith("/")) {
            parent += File.separator;
        }
        return parent + path;
    }

    /**
	 *  Constructs an absolute path name from three path components. This method is
	 *  VFS-aware.
	 *
	 *@param  parent  The parent directory
	 *@param  path1   The first path
	 *@param  path2   The second path
	 *@return         the absolute path name
	 *@since          GMGen 3.3
	 *
	 * @deprecated Unused
	 */
    public static String constructPath(String parent, String path1, String path2) {
        return constructPath(constructPath(parent, path1), path2);
    }

    /**
	 * Copy a file
	 * @param from_file
	 * @param to_file
	 * @throws IOException
	 */
    public static void copy(File from_file, File to_file) throws IOException {
        if (!from_file.exists()) {
            throw new IOException("FileCopy: no such source file: " + from_file.getPath());
        }
        if (!from_file.isFile()) {
            throw new IOException("FileCopy: can't copy directory: " + from_file.getPath());
        }
        if (!from_file.canRead()) {
            throw new IOException("FileCopy: source file is unreadable: " + from_file.getPath());
        }
        if (to_file.isDirectory()) {
            to_file = new File(to_file, from_file.getName());
        }
        if (to_file.exists()) {
            if (!to_file.canWrite()) {
                throw new IOException("FileCopy: destination file is unwriteable: " + to_file.getPath());
            }
            int choice = JOptionPane.showConfirmDialog(null, "Overwrite existing file " + to_file.getPath(), "File Exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                throw new IOException("FileCopy: existing file was not overwritten.");
            }
        } else {
            String parent = to_file.getParent();
            if (parent == null) {
                parent = Globals.getDefaultPath();
            }
            File dir = new File(parent);
            if (!dir.exists()) {
                throw new IOException("FileCopy: destination directory doesn't exist: " + parent);
            }
            if (dir.isFile()) {
                throw new IOException("FileCopy: destination is not a directory: " + parent);
            }
            if (!dir.canWrite()) {
                throw new IOException("FileCopy: destination directory is unwriteable: " + parent);
            }
        }
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(from_file);
            to = new FileOutputStream(to_file);
            byte[] buffer = new byte[4096];
            int bytes_read;
            while ((bytes_read = from.read(buffer)) != -1) {
                to.write(buffer, 0, bytes_read);
            }
        } finally {
            if (from != null) {
                try {
                    from.close();
                } catch (IOException e) {
                }
            }
            if (to != null) {
                try {
                    to.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
	 * @param path
	 * @return exxpanded relative path
	 * @deprecated Unused - remove in 5.9.5
	 */
    public static String expandRelativePath(String path) {
        if (path.startsWith("@")) {
            path = Globals.getDefaultPath() + File.separator + path.substring(1);
        }
        return path;
    }

    /**
	 *  Converts a file name to a class name. All slash characters are replaced
	 *  with periods and the trailing '.class' is removed.
	 *
	 *@param  name  The file name
	 *@return       the class name
	 *@since        GMGen 3.3
	 */
    public static String fileToClass(String name) {
        char[] clsName = name.toCharArray();
        for (int i = clsName.length - 6; i >= 0; i--) {
            if (clsName[i] == '/') {
                clsName[i] = '.';
            }
        }
        return new String(clsName, 0, clsName.length - 6);
    }

    /**
	 *  Reads the localization properties file.
	 *@since        GMGen 3.3
	 *
	 * @deprecated Unused
	 */
    public static void readLocalizationProperties() {
        try {
            FileInputStream in = new FileInputStream("/pcgen/gui/prop/LanguageBundle.properties");
            localization.load(in);
        } catch (IOException e) {
            Logging.errorPrint("No localization file found.");
        }
    }

    /**
	 * @param path
	 * @return relative path 
	 * @deprecated Unused - remove in 5.9.5
	 * because user.dir changes to the pcgen dir, this doesn't work as 
	 * well as it did in pcgen - we need to get it so it functions correctly.
	 */
    public static String retractRelativePath(String path) {
        File systemDir = new File(Globals.getDefaultPath());
        if (path.startsWith(systemDir.getAbsolutePath())) {
            path = "@" + path.substring(systemDir.getAbsolutePath().length());
        }
        return path;
    }

    /** Converts a Color to an 7 byte hex string starting with '#'.
	 * @param c
	 * @return String*/
    public static String colorToHex(Color c) {
        final char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int i = c.getRGB();
        char[] buf7 = new char[7];
        buf7[0] = '#';
        for (int pos = 6; pos >= 1; pos--) {
            buf7[pos] = hexDigits[i & 0xf];
            i >>>= 4;
        }
        return new String(buf7);
    }
}
