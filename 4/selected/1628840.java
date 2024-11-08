package org.magiclight.common;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author Mikael Aronsson
 */
public final class MLUtil {

    /**
	 *
	 */
    public static final int VER_MAJOR = 1;

    /**
	 *
	 */
    public static final int VER_MINOR = 0;

    /**
	 *
	 */
    public static final int VER_REVISION = 0;

    /**
	 * Internal buffer size for file operations
	 */
    public static final int TEMP_FILE_BUFFER_SIZE = 2048;

    /**
	 * Colors
	 */
    public static final Color COLOR_USF_GREEN = new Color(125, 167, 116);

    /**
	 *
	 */
    public static final Color COLOR_USF_GREEN2 = new Color(58, 167, 123);

    /**
	 *
	 */
    public static final Color COLOR_USF_BLUE = new Color(63, 121, 186);

    /**
	 *
	 */
    public static final Color COLOR_USF_CYAN = new Color(49, 141, 176);

    /**
	 *
	 */
    public static final Color COLOR_USF_LBLUE = new Color(117, 187, 255);

    /**
	 *
	 */
    public static final Color COLOR_USF_ORANGE = new Color(241, 98, 69);

    /**
	 *
	 */
    public static final Color COLOR_USF_BROWN = new Color(130, 100, 84);

    /**
	 *
	 */
    public static final Color COLOR_USF_YELLOW = new Color(252, 211, 61);

    /**
	 *
	 */
    public static final Color COLOR_USF_PURPLE = new Color(70, 67, 123);

    /**
	 *
	 */
    public static final Color COLOR_USF_RED = new Color(193, 48, 32);

    /**
	 * Global debug flag
	 */
    public static final boolean DEBUG;

    /**
	 * Thread pool
	 */
    private static ExecutorService threadPool;

    /**
	 * Logger object for debugging
	 */
    public static Logger logger = null;

    /**
	 * Number format that is always english
	 */
    private static final NumberFormat numFormat = NumberFormat.getNumberInstance(new Locale("en", "GB"));

    /**
	 * Language to use
	 */
    private static String language;

    /**
	 * Locale to use
	 */
    private static Locale locale;

    /**
	 * Language change listeners
	 */
    private static final ArrayList<ILanguageListener> listeners = new ArrayList<ILanguageListener>();

    /**
	 * Resource bundle for localization of text
	 */
    private static ResourceBundle bundle;

    static {
        threadPool = Executors.newCachedThreadPool();
        int f = asInt(System.getProperty("ML_DEBUG"));
        DEBUG = (f == 0 ? false : true);
        Locale loc = Locale.getDefault();
        String s = loc.getLanguage() + "_" + loc.getCountry();
        language = s;
        locale = loc;
    }

    private MLUtil() {
    }

    /**
	 * Delete all files from <code>dir</code> directory.
	 * @param dir path to directory
	 * @return true if valid path, false if not
	 */
    public static boolean deleteAllFiles(String dir) {
        File[] list = new File(dir).listFiles();
        int i;
        if (list == null) return false;
        for (i = 0; i < list.length; i++) {
            list[i].delete();
        }
        return true;
    }

    /**
	 * Display debug string to console and log
	 * @param str
	 */
    public static void d(String str) {
        if (DEBUG) {
            if (logger == null) setupLogger();
            logger.info(str);
            System.out.println(str);
        }
    }

    /**
	 * Handle runtime errors
	 * @param e
	 */
    public static void runtimeError(Throwable e) {
        runtimeError(e, null);
    }

    /**
	 * Handle runtime errors
	 * @param msg
	 */
    public static void runtimeError(String msg) {
        runtimeError(null, msg);
    }

    /**
	 * Handle runtime errors
	 * @param e
	 * @param text extra text message, can be null
	 */
    public static void runtimeError(Throwable e, String text) {
        String msg;
        if (text != null) msg = text; else msg = "";
        if (e != null) {
            msg += "\n- Stack trace ---------------------------------------------------\nException:" + e.getLocalizedMessage() + "\n";
            StackTraceElement[] t = e.getStackTrace();
            int i;
            for (i = 0; i < t.length; i++) msg += t[i].toString() + "\n";
            msg += "----------------------------------------------------------------\n";
        } else {
            msg += "\n";
        }
        MLUtil.d(msg);
    }

    /**
	 * Setup loggers
	 */
    private static void setupLogger() {
        try {
            Handler fh1 = new FileHandler("%h/magiclight_debug%g.log", 200000, 2);
            fh1.setFormatter(new SimpleFormatter());
            fh1.setLevel(Level.ALL);
            logger = Logger.getLogger("org.magiclight.debug");
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL);
            logger.addHandler(fh1);
        } catch (IOException e) {
            System.out.println("Logger init failure");
        }
    }

    /**
	 * Return stack trace from e as a string
	 *
	 * @param e
	 * @return
	 */
    public static String asString(Throwable e) {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bs);
        e.printStackTrace(pw);
        pw.flush();
        String str = bs.toString();
        return str;
    }

    /**
	 * Convert string integer value to integer or 0 if not possible
	 *
	 * @param s
	 * @return
	 */
    public static int asInt(String s) {
        if (s == null || s.length() == 0) return (0);
        try {
            return (Integer.parseInt(s));
        } catch (NumberFormatException e) {
            runtimeError(null, "String is not an integer value: '" + s + "'");
        }
        return (0);
    }

    /**
	 * Convert string to integer and clamp it
	 *
	 * @param s 
	 * @param lo
	 * @param hi
	 * @return
	 */
    public static int asInt(String s, int lo, int hi) {
        int n = asInt(s);
        MLUtil.clamp(n, lo, hi);
        return (n);
    }

    /**
	 * Convert string to integer, return default value if null or empty string
	 *
	 * @param s
	 * @param defval
	 * @return
	 */
    public static int asIntDefault(String s, int defval) {
        if (s == null) return (defval);
        if (s.length() == 0) return (defval);
        return (asInt(s));
    }

    /**
	 * Convert string integer value to integer if possible, b=base
	 *
	 * @param s
	 * @param b
	 * @return
	 */
    public static int asIntRadix(String s, int b) {
        if (s == null || s.length() == 0) return (0);
        try {
            return (Integer.parseInt(s, b));
        } catch (NumberFormatException e) {
            runtimeError(e, "String is not an integer value: '" + s + "'");
        }
        return (0);
    }

    /**
	 * Convert a string to a double or 0.0 if not possible, english (UK) parsing is used
	 * so '.' has to be used for the decimal point, this is used for portable parsing of
	 * floating point values (to files for example).
	 *
	 * @param s
	 * @return
	 */
    public static double asDouble(String s) {
        if (s == null || s.length() == 0) return (0.0);
        try {
            return (numFormat.parse(s).doubleValue());
        } catch (ParseException e) {
            runtimeError(null, "String is not a valid floating point value: '" + s + "'");
        }
        return (0.0);
    }

    /**
	 * Convert a string to a double or defval if s is null, english (UK) parsing is used
	 * so '.' has to be used for the decimal point, this is used for portable parsing of
	 * floating point values (to files for example).
	 *
	 * @param s
	 * @param defval
	 * @return
	 */
    public static double asDouble(String s, double defval) {
        if (MLUtil.isEmpty(s)) return (defval);
        return (asDouble(s));
    }

    /**
	 * Convert string integer value to long or 0L if not possible
	 *
	 * @param s
	 * @return
	 */
    public static long asLong(String s) {
        if (s == null || s.length() == 0) return (0);
        try {
            return (Long.parseLong(s));
        } catch (NumberFormatException e) {
            runtimeError(e, "String is not a long value: '" + s + "'");
        }
        return (0);
    }

    /**
	 * Format a double to a string in en_GB format
	 *
	 * @param v
	 * @return
	 */
    public static String format(double v) {
        return (numFormat.format(v));
    }

    /**
	 * Convert string to boolean, <code>0,1,false,true</code> are valid values
	 *
	 * @param s
	 * @param defval
	 * @return
	 */
    public static boolean asBoolean(String s, boolean defval) {
        if (s == null || s.length() == 0) return (defval);
        return (asBoolean(s));
    }

    /**
	 * Convert string to boolean, <code>0,1,false,true</code> are valid values (not case sensitive),
	 * null is returned as false.
	 *
	 * @param s
	 * @return
	 */
    public static boolean asBoolean(String s) {
        if (s == null) return (false);
        if (s.equalsIgnoreCase("true")) return (true);
        if (s.equalsIgnoreCase("false")) return (false);
        int n = asInt(s);
        return (n == 0 ? false : true);
    }

    /**
	 * Delay for n ms catching any exceptions
	 *
	 * @param n
	 */
    public static void sleep(int n) {
        try {
            Thread.sleep(n);
        } catch (Exception e) {
        }
    }

    /**
	 * Convert UTF8 bytes to unicode string
	 *
	 * @param buffer
	 * @param len
	 * @return
	 */
    public static String UTF8toUNICODE(byte[] buffer, int len) {
        try {
            return (new String(buffer, 0, len, "UTF8"));
        } catch (UnsupportedEncodingException e) {
            runtimeError(e);
        }
        return (null);
    }

    /**
	 * Convert UTF8 bytes to unicode string
	 *
	 * @param buffer
	 * @return
	 */
    public static String UTF8toUNICODE(byte[] buffer) {
        return (UTF8toUNICODE(buffer, buffer.length));
    }

    /**
	 * Convert unicode string to UTF8 bytes
	 *
	 * @param buffer
	 * @return
	 */
    public static byte[] UNICODEtoUTF8(String buffer) {
        try {
            return (buffer.getBytes("UTF8"));
        } catch (UnsupportedEncodingException e) {
            runtimeError(e);
        }
        return (null);
    }

    /**
	 * Load specified text file into a byte buffer and return it
	 *
	 * @param file
	 * @return
	 */
    public static byte[] loadFileToBuffer(String file) {
        try {
            File f = new File(file);
            if (!f.exists()) return (null);
            int len = (int) f.length();
            d("loadFileToBuffer: " + file + " = " + len);
            byte[] buffer = new byte[len];
            FileInputStream is = new FileInputStream(file);
            is.read(buffer);
            is.close();
            return (buffer);
        } catch (FileNotFoundException e) {
            runtimeError(e, "loadFileToBuffer: " + file);
        } catch (IOException e2) {
            runtimeError(e2, "loadFileToBuffer: " + file);
        }
        return (null);
    }

    /**
	 * Save byte buffer to a file, returns true on success
	 *
	 * @param file
	 * @param buffer
	 * @return
	 */
    public static boolean saveBufferToFile(String file, byte[] buffer) {
        try {
            d("saveBufferToFile: " + file + " = " + buffer.length);
            FileOutputStream os = new FileOutputStream(file);
            os.write(buffer);
            os.close();
            return (true);
        } catch (FileNotFoundException e) {
            runtimeError(e, "saveBufferToFile: " + file);
        } catch (IOException e2) {
            runtimeError(e2, "saveBufferToFile: " + file);
        }
        return (false);
    }

    /**
	 * Make sure directory for file exists
	 *
	 * @param file
	 */
    public static void makeSureDirectoryExists(String file) {
        new File(file).mkdirs();
    }

    /**
	 * Create directory
	 *
	 * @param s
	 * @return
	 */
    public static boolean createDirectory(String s) {
        File dir = new File(s);
        if (!dir.canRead()) {
            dir.mkdir();
            return (true);
        }
        return (false);
    }

    /**
	 * Create an object from specified class and return it, null on failure
	 *
	 * @param cls
	 * @return
	 */
    public static Object createObject(String cls) {
        Object obj = null;
        try {
            Class c = Class.forName(cls);
            obj = c.newInstance();
        } catch (ClassNotFoundException e1) {
            runtimeError("Cannot locate class: " + cls);
        } catch (Exception e2) {
            runtimeError(e2, "Cannot create object for class: " + cls);
        }
        return (obj);
    }

    /**
	 * Get file from stream to a local file on disk.
	 *
	 * @param src
	 * @param dst
	 * @return
	 */
    public static boolean getFile(InputStream src, File dst) {
        if (src == null) return false;
        FileOutputStream wr = null;
        boolean ret = true;
        try {
            wr = new FileOutputStream(dst);
            byte[] buf = new byte[TEMP_FILE_BUFFER_SIZE];
            int len = 1;
            while (len > 0) {
                len = src.read(buf, 0, TEMP_FILE_BUFFER_SIZE);
                if (len > 0) wr.write(buf, 0, len);
            }
        } catch (Exception e) {
            runtimeError(e, dst.getPath());
            dst.delete();
            ret = false;
        }
        try {
            src.close();
            if (wr != null) wr.close();
        } catch (IOException e) {
        }
        return (ret);
    }

    /**
	 * Get file from stream to a byte buffer
	 *
	 * @param src
	 * @return
	 */
    public static byte[] getFile(InputStream src) {
        byte[] result = null;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buf = new byte[TEMP_FILE_BUFFER_SIZE];
            int len = 1;
            while (len > 0) {
                len = src.read(buf, 0, TEMP_FILE_BUFFER_SIZE);
                if (len > 0) os.write(buf, 0, len);
            }
            result = os.toByteArray();
            src.close();
        } catch (IOException e) {
            runtimeError(e, "getFile error");
        } catch (NullPointerException e2) {
            runtimeError(e2, "getFile error");
        }
        return (result);
    }

    /**
	 * Return true if a string is null or zero length
	 *
	 * @param str
	 * @return
	 */
    public static boolean isEmpty(String str) {
        if (str == null || str.length() == 0) return (true); else return (false);
    }

    /**
	 * Get string, if string is null an empty string is returned
	 *
	 * @param s
	 * @return
	 */
    public static String asString(String s) {
        if (s == null) return ("");
        return (s);
    }

    /**
	 * Copy file src to dst
	 *
	 * @param src 
	 * @param dst
	 * @throws IOException
	 */
    public static void copyFile(File src, File dst) throws IOException {
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dst);
            byte[] buf = new byte[TEMP_FILE_BUFFER_SIZE];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
        } catch (FileNotFoundException e1) {
            MLUtil.runtimeError(e1, src.toString());
        } catch (IOException e2) {
            MLUtil.runtimeError(e2, src.toString());
        }
    }

    /**
	 * Serialize an object to a byte buffer, null is returned if error
	 *
	 * @param obj
	 * @return
	 */
    public static byte[] saveObjectToBuffer(Object obj) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(bo);
            os.writeObject(obj);
            return (bo.toByteArray());
        } catch (IOException e1) {
            MLUtil.runtimeError(e1, "");
        }
        return (null);
    }

    /**
	 * Load an object from a byte buffer, null is returned on error
	 *
	 * @param data
	 * @return
	 */
    public static Object loadObjectFromBuffer(byte[] data) {
        try {
            ByteArrayInputStream bi = new ByteArrayInputStream(data);
            ObjectInputStream is = new ObjectInputStream(bi);
            Object obj = is.readObject();
            return (obj);
        } catch (IOException e1) {
            MLUtil.runtimeError(e1, "");
        } catch (ClassNotFoundException e2) {
            MLUtil.runtimeError(e2, "");
        }
        return (null);
    }

    /**
	 * Serialize an object to a file
	 *
	 * @param file 
	 * @param object
	 * @return
	 */
    public static boolean saveObject(String file, Object object) {
        return (saveObject(file, object, 0L));
    }

    /**
	 * Serialize an object to a file, set timestamp on file to crc
	 *
	 * @param file
	 * @param object
	 * @param crc
	 * @return
	 */
    public static boolean saveObject(String file, Object object, long crc) {
        boolean f = false;
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(object);
            oos.close();
            if (crc != 0L) new File(file).setLastModified(crc);
            f = true;
        } catch (IOException e1) {
            MLUtil.runtimeError(e1, file);
        }
        return (f);
    }

    /**
	 * Load object from file using serialization.
	 *
	 * @param file
	 * @return
	 */
    public static Object loadObject(String file) {
        Object object = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            if (ois != null) {
                object = ois.readObject();
                ois.close();
            }
        } catch (IOException e1) {
            MLUtil.runtimeError(e1, file);
        } catch (ClassNotFoundException e2) {
            MLUtil.runtimeError(e2, file);
        }
        if (object == null) MLUtil.d("loadObject failure: " + file);
        return (object);
    }

    /**
	 * Load object from byte array
	 *
	 * @param data
	 * @return
	 */
    public static Object loadObject(byte[] data) {
        Object object = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            if (ois != null) {
                object = ois.readObject();
                ois.close();
            }
        } catch (IOException e1) {
            MLUtil.runtimeError(e1, "");
        } catch (ClassNotFoundException e2) {
            MLUtil.runtimeError(e2, "");
        }
        if (object == null) MLUtil.d("loadObject failure: " + "");
        return (object);
    }

    /**
	 * If the specified file does not have the specified extension, then add it or
	 * modify it.
	 *
	 * @param str
	 * @param ext
	 * @return
	 */
    public static String setExtension(String str, String ext) {
        if (str.endsWith(ext)) return (str);
        int j = str.lastIndexOf('.');
        if (j > 0) return (str.substring(0, j) + ext);
        return (str + ext);
    }

    /**
	 * Read a string from stream is until EOF, CR or LF is reached
	 *
	 * @param is
	 * @return
	 */
    public static String readLine(InputStreamReader is) {
        StringBuilder sb = new StringBuilder();
        try {
            int ch;
            while ((ch = is.read()) != -1) {
                if (ch == '\r' || ch == '\n') break;
                sb.append((char) ch);
            }
        } catch (IOException e1) {
            MLUtil.runtimeError(e1, "");
        }
        return (sb.toString());
    }

    /**
	 *
	 */
    public static int countedValue = 1;

    /**
	 * Get a unique counted value that can never be 0
	 *
	 * @return
	 */
    public static int getCountedValue() {
        if (countedValue < 1) countedValue = 1;
        return (countedValue++);
    }

    /**
	 * Assign a task to the thread pool to be executed at some time in the future.
	 *
	 * @param run
	 */
    public static void assign(Runnable run) {
        threadPool.execute(run);
    }

    /**
	 * Save bean to file in xml format
	 * @param obj
	 * @param file
	 */
    public static void saveBeanAsXml(Object obj, String file) {
        try {
            XMLEncoder x = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(file)));
            x.writeObject(obj);
            x.close();
        } catch (Exception e1) {
            MLUtil.runtimeError(e1, file);
        }
    }

    /**
	 * Save bean to byte array in xml format
	 * @param obj
	 * @return
	 */
    public static byte[] saveBeanAsXml(Object obj) {
        try {
            ByteArrayOutputStream os;
            XMLEncoder x = new XMLEncoder(new BufferedOutputStream(os = new ByteArrayOutputStream()));
            x.writeObject(obj);
            x.close();
            return os.toByteArray();
        } catch (Exception e1) {
            MLUtil.runtimeError(e1);
        }
        return null;
    }

    /**
	 * Load bean from xml file
	 * @param file
	 * @return
	 */
    public static Object loadBeanFromXml(String file) {
        try {
            if (new File(file).exists()) {
                XMLDecoder x = new XMLDecoder(new BufferedInputStream(new FileInputStream(file)));
                Object obj = x.readObject();
                x.close();
                return obj;
            } else {
                return null;
            }
        } catch (Exception e1) {
            MLUtil.runtimeError(e1, file);
        }
        return null;
    }

    /**
	 * Load bean from xml array
	 *
	 * @param data
	 * @return
	 */
    public static Object loadBeanFromXml(byte[] data) {
        try {
            XMLDecoder x = new XMLDecoder(new BufferedInputStream(new ByteArrayInputStream(data)));
            Object obj = x.readObject();
            x.close();
            return obj;
        } catch (Exception e1) {
            MLUtil.runtimeError(e1);
        }
        return null;
    }

    /**
	 * Split up string at delim character into a list of strings
	 * @param str   string to split
	 * @param delim character to use as delimiter
	 * @return
	 */
    public static String[] split(String str, String delim) {
        StringTokenizer tk = new StringTokenizer(str, delim);
        String[] list = new String[tk.countTokens()];
        int i = 0;
        while (tk.hasMoreTokens()) {
            list[i++] = tk.nextToken();
        }
        return list;
    }

    /**
	 * Grow array p to make sure it's lenth is len, the old or a new array is
	 * returned and all old contents are copied.
	 * @param p   array to grow
	 * @param len new length requested
	 * @return old array or a larger copy
	 */
    public static int[] growArray(int[] p, int len) {
        if (p.length >= len) return p;
        int[] p2 = new int[len];
        System.arraycopy(p, 0, p2, 0, p.length);
        return p2;
    }

    /**
	 * Grow array p to make sure it's lenth is len, the old or a new array is
	 * returned and all old contents are copied.
	 * @param p   array to grow
	 * @param len new length requested
	 * @return old array or a larger copy
	 */
    public static double[] growArray(double[] p, int len) {
        if (p.length >= len) return p;
        double[] p2 = new double[len];
        System.arraycopy(p, 0, p2, 0, p.length);
        return p2;
    }

    /**
	 * Returns a sub srray of p to minimize space used
	 * @param p   array to get data from
	 * @param len length of sub array
	 * @return old array or a smaller copy
	 */
    public static int[] subArray(int[] p, int len) {
        if (p.length <= len) return p;
        int[] p2 = new int[len];
        System.arraycopy(p, 0, p2, 0, len);
        return p2;
    }

    /**
	 * Returns a sub srray of p to minimize space used
	 * @param p   array to get data from
	 * @param len length of sub array
	 * @return old array or a smaller copy
	 */
    public static double[] subArray(double[] p, int len) {
        if (p.length <= len) return p;
        double[] p2 = new double[len];
        System.arraycopy(p, 0, p2, 0, len);
        return p2;
    }

    /**
	 * Returns a sub srray of p to minimize space used
	 * @param p   array to get data from
	 * @param len length of sub array
	 * @return old array or a smaller copy
	 */
    public static String[] subArray(String[] p, int len) {
        if (p.length <= len) return p;
        String[] p2 = new String[len];
        System.arraycopy(p, 0, p2, 0, len);
        return p2;
    }

    /**
	 * Get bit flag from an integer
	 * @param flags
	 * @param f
	 * @return modified integer
	 */
    public static boolean getFlag(int flags, int f) {
        return ((flags & f) != 0);
    }

    /**
	 * Set a bit flag in an integer
	 * @param flags
	 * @param f
	 * @return modified integer
	 */
    public static int setFlag(int flags, int f) {
        flags |= f;
        return flags;
    }

    /**
	 * Clear a bit flag in an integer
	 * @param flags
	 * @param f
	 * @return modified integer
	 */
    public static int clrFlag(int flags, int f) {
        flags &= (0x7fffffff - f);
        return flags;
    }

    /**
	 * Set a bit flags to state in an integer
	 * @param flags
	 * @param f
	 * @param state
	 * @return modified integer
	 */
    public static int setFlag(int flags, int f, boolean state) {
        if (state) flags |= f; else flags &= (0x7fffffff - f);
        return flags;
    }

    /**
	 * Scale a raw value to it's display value (clamps to <code>rmin</code>), <code>rmin-rmax</code> is the from range
	 * and <code>vmin-max</code> is the destination range.
	 *
	 * @param rmin
	 * @param rmax
	 * @param vmin
	 * @param value
	 * @param vmax
	 * @return
	 */
    public double scaleValue(double rmin, double rmax, double vmin, double vmax, double value) {
        if (value < rmin) value = rmin;
        double v = vmax - vmin;
        double r = rmax - rmin;
        value = ((v / r) * (value - rmin));
        return (value + vmin);
    }

    /**
	 * Place s in clipboard
	 * @param s
	 * @param owner
	 */
    public static void writeToClipboard(String s, ClipboardOwner owner) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable transferable = new StringSelection(s);
        clipboard.setContents(transferable, owner);
    }

    /**
	 * Clamp double value
	 *
	 * @param ddata
	 * @param lo
	 * @param hi
	 * @return
	 */
    public static double clamp(double ddata, double lo, double hi) {
        if (ddata < lo) ddata = lo;
        if (ddata > hi) ddata = hi;
        return (ddata);
    }

    /**
	 * Clamp float value
	 *
	 * @param ddata
	 * @param lo
	 * @param hi
	 * @return
	 */
    public static float clamp(float ddata, float lo, float hi) {
        if (ddata < lo) ddata = lo;
        if (ddata > hi) ddata = hi;
        return (ddata);
    }

    /**
	 * Clamp byte value
	 *
	 * @param ddata
	 * @param lo
	 * @param hi
	 * @return
	 */
    public static byte clamp(byte ddata, byte lo, byte hi) {
        if (ddata < lo) ddata = lo;
        if (ddata > hi) ddata = hi;
        return (ddata);
    }

    /**
	 * Clamp int value
	 *
	 * @param ddata
	 * @param lo
	 * @param hi
	 * @return
	 */
    public static int clamp(int ddata, int lo, int hi) {
        if (ddata < lo) ddata = lo;
        if (ddata > hi) ddata = hi;
        return (ddata);
    }

    /**
	 * Clamp long value
	 *
	 * @param ddata 
	 * @param lo
	 * @param hi
	 * @return
	 */
    public static long clamp(long ddata, long lo, long hi) {
        if (ddata < lo) ddata = lo;
        if (ddata > hi) ddata = hi;
        return (ddata);
    }

    /**
	 * Parse a string into an array of String object, default values are given as defval
	 * all values in string are | (pipe) separated.
	 * The size of the returned array is always the same size as defval, if to few
	 * items are given defval is used, if to many are given they are lost.
	 * if str is null a copy of defval is returned.
	 *
	 * @param str 
	 * @param defval
	 * @return
	 */
    public static String[] parsePipeString(String str, String... defval) {
        if (str == null) return defval;
        String[] r = new String[defval.length];
        System.arraycopy(defval, 0, r, 0, defval.length);
        StringTokenizer tk = new StringTokenizer(str, "|");
        int i = 0;
        while (tk.hasMoreTokens()) {
            r[i++] = tk.nextToken();
            if (i >= r.length) break;
        }
        return r;
    }

    /**
	 * Extract a string from text, start marks where the string begins
	 * and it will continue until end or there are no more text.
	 * Example:  extractString( "AB:", "|", "test AB:test|end");
	 * will return "test". null is returned if it is not found.
	 *
	 * @param start 
	 * @param end
	 * @param text
	 * @return
	 */
    public static String extractString(String start, String end, String text) {
        int i = text.indexOf(start);
        if (i == -1) return null;
        int j = -1;
        if (end != null) j = text.indexOf(end, i + start.length());
        if (j == -1) return text.substring(i + start.length()); else return text.substring(i + start.length(), j);
    }

    /**
	 * Convert a | separated string into a Vector
	 *
	 * @param s
	 * @return
	 */
    public static Vector<String> parseStringTovector(String s) {
        Vector<String> v = new Vector<String>();
        StringTokenizer tk = new StringTokenizer(s, "|");
        while (tk.hasMoreTokens()) v.addElement(tk.nextToken());
        return v;
    }

    /**
	 * Convert a configuration string to a map, each parameter in the configuration string
	 * is separated with a ; and each key/value is separated with a = character.
	 * Example "key1=value1;key2=value2...".
	 * ; and = are replaced with ! and |
	 *
	 * @param s
	 * @return
	 */
    public static HashMap<String, String> parseConfigurationString(String s) {
        HashMap<String, String> map = new HashMap<String, String>();
        if (s == null) return map;
        StringTokenizer tk = new StringTokenizer(s, ";");
        try {
            while (tk.hasMoreTokens()) {
                String str = tk.nextToken();
                int i = str.indexOf('=');
                if (i >= 0) {
                    String k = str.substring(0, i);
                    String v = str.substring(i + 1);
                    v = v.replace('|', '=');
                    v = v.replace('!', ';');
                    map.put(k, v);
                } else {
                    MLUtil.runtimeError(null, "Configuration key/value error: " + str);
                }
            }
        } catch (Exception e1) {
            MLUtil.runtimeError(e1, "Configuration string error: " + s);
        }
        return map;
    }

    /**
	 * Return an entry from a map, if it is missing defval is returned
	 * @param map
	 * @param key
	 * @param defval
	 * @return
	 */
    public static String getMapEntry(HashMap<String, String> map, String key, String defval) {
        String s = map.get(key);
        if (s == null) return defval;
        return s;
    }

    /**
	 * Build a configuration string from a map, format like "key=value;".
	 * ; and = are replaced with ! and |
	 * @param map
	 * @return
	 */
    public static String makeStringFromMap(HashMap<String, String> map) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = map.keySet().iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (!first) sb.append(';');
            String key = it.next();
            String v = map.get(key);
            v = v.replace('=', '|');
            v = v.replace(';', '!');
            sb.append(key);
            sb.append('=');
            sb.append(v);
            first = false;
        }
        return sb.toString();
    }

    /**
	 * Convert all special characters to %xx
	 *
	 * @param str
	 * @return
	 */
    public static String asNormalString(String str) {
        if (str.indexOf('%') == -1) return (str);
        StringBuilder sb = new StringBuilder(str.length());
        int len = str.length();
        int i = 0;
        while (len-- > 0) {
            char c = str.charAt(i++);
            if (c == '%') {
                int ch = MLUtil.asIntRadix(str.substring(i, i + 2), 16);
                sb.append((char) ch);
                i += 2;
            } else {
                sb.append(c);
            }
        }
        return (sb.toString());
    }

    /**
	 * Convert all unsuafe characters in a string to URI format
	 *
	 * @param str
	 * @return
	 */
    public static String asSafeString(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        int len = str.length();
        int i = 0;
        while (len-- > 0) {
            char c = str.charAt(i++);
            if ("=;|:".indexOf(c) >= 0) {
                String hex = Integer.toHexString(c);
                if (hex.length() == 1) sb.append("%0"); else sb.append('%');
                sb.append(hex);
            } else {
                sb.append(c);
            }
        }
        return (sb.toString());
    }

    /**
	 * Set language
	 * @param s
	 */
    public static void setLanguage(String s) {
        if (s.length() != 5) throw new IllegalArgumentException("Language must be in format ll_CC");
        if (s.charAt(2) != '_') throw new IllegalArgumentException("Language must be in format ll_CC");
        language = s;
        locale = new Locale(language.substring(0, 2), language.substring(3));
        MLUtil.s(null);
        synchronized (listeners) {
            int n = listeners.size();
            int i;
            for (i = 0; i < n; i++) listeners.get(i).languageHasChanged(language);
        }
    }

    /**
	 * Returns current language as ll_CC string.
	 * @return
	 */
    public static String getLanguage() {
        return language;
    }

    /**
	 * Returns current locale
	 * @return
	 */
    public static Locale getLocale() {
        return locale;
    }

    /**
	 * Add a language listener
	 * @param l
	 */
    public static void addLanguageListener(ILanguageListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    /**
	 * Remove a language listener
	 * @param l
	 */
    public static void removeLanguageListener(ILanguageListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }

    /**
	 * Return localized strings based on key. Example: <code>getString( 0, 1, "key")</code> will
	 * return resource for "key0" and "key1".
	 * @param from First index to get
	 * @param to   Last index to get
	 * @param key  Key to prepend index with
	 * @return List of strings
	 */
    public static String[] getStrings(int from, int to, String key) {
        String[] list = new String[(to - from) + 1];
        int i;
        for (i = from; i <= to; i++) {
            String s = s(key + i);
            list[i] = s;
        }
        return list;
    }

    /**
	 * Return string as a combination of one or more keys using "{key}" to replace with
	 * a localized text.
	 * @param format
	 * @return
	 */
    public static String S(String format) {
        int i = format.indexOf('{');
        int j;
        if (i == -1) return s(format);
        while (i >= 0) {
            j = format.indexOf('}', i);
            String a = format.substring(0, i);
            String b = format.substring(j + 1);
            String k = format.substring(i + 1, j);
            String s = s(k);
            format = a + s + b;
            i = format.indexOf('{', j + 1);
        }
        return format;
    }

    /**
	 * Return string from bundle, if name is null the cache is reset
	 * @param name
	 * @return
	 */
    public static String s(String name) {
        String s = null;
        synchronized (language) {
            if (name == null) {
                bundle = null;
                return null;
            }
            try {
                if (bundle == null) {
                    bundle = ResourceBundle.getBundle("org.magiclight.common.data.magiclight_text", getLocale());
                }
                return bundle.getString(name);
            } catch (Exception e1) {
                MLUtil.runtimeError(e1, "getString failure: " + s + " " + name);
                return ("{'" + s + "'/'" + name + "'}");
            }
        }
    }
}
