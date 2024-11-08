package collabed.util;

import java.io.*;
import java.util.*;

/**
 * A collection of miscellaneous procedures that are used throughout 
 * the collabed package, and/or do not share any unique connection to
 * an individual file in the package.
 *
 *@author Kenroy Granville / Tim Hickey
 *@version "%I%, %G%"
 */
public class Util {

    public static String arrayToString(Object[] arr) {
        String g = "[";
        if (arr != null) {
            for (int i = 0; i < arr.length - 1; i++) g += arr[i] + ",\n ";
            if (arr.length - 1 >= 0) g += arr[arr.length - 1];
        }
        return g + "]";
    }

    private static boolean debugging = true;

    public static void debug(Object msg) {
        if (debugging) {
            System.out.print(msg.toString());
            System.out.flush();
        }
    }

    public static void debugln(Object msg) {
        if (debugging) {
            System.out.println(msg.toString());
            System.out.flush();
        }
    }

    /**
    * @param b debugging toggle
    * Turn debugging on or off.
    **/
    public static void setDebugging(boolean b) {
        debugging = b;
    }

    private static Vector<ErrorListener> errListeners = new Vector<ErrorListener>();

    public static void addErrorListener(ErrorListener errLis) {
        errListeners.add(errLis);
    }

    public static void removeErrorListener(ErrorListener errLis) {
        errListeners.remove(errLis);
    }

    public static void error(Object source, String msg) {
        error(source, msg, null);
    }

    public static void error(Object source, String msg, Exception e) {
        if (e == null) {
            logEvent(source, msg);
            System.out.println("ERROR:\n  Source: " + source + "\n  Message: " + msg);
        } else {
            logEvent(source, msg, e);
            System.out.println("ERROR:\n  Source: " + source + "\n  Message: " + msg + "\n  Exception: ");
        }
        e.printStackTrace();
        for (int i = 0; i < errListeners.size(); i++) {
            try {
                if (e == null) errListeners.get(i).error(source, msg); else errListeners.get(i).error(source, msg, e);
            } catch (Exception ex) {
                System.out.println("ERROR LISTENER FAILED: " + errListeners.get(i));
            }
        }
    }

    private static boolean logging = false;

    private static File logfile = null;

    private static PrintWriter writer = null;

    public static void logEvent(Object source, String info) {
        if (debugging) debugln("LOG: " + source + " / " + info);
        if (logging) {
            if (logfile == null) try {
                String path = System.getenv("user.home");
                logfile = new File(path, "collabed-" + new Date().getTime() + ".log");
                writer = new PrintWriter(new FileWriter(logfile));
            } catch (Exception e) {
                e.printStackTrace();
            }
            writer.println("SYSID: Thread[" + Thread.currentThread().getName() + "]" + "Class[" + source.getClass() + "]");
            writer.println(source + "[" + info + "]");
            writer.flush();
        }
    }

    public static void logEvent(Object source, String info, Exception e) {
        if (logging) {
            logEvent(source, info + " (Exception Below)");
            logEvent(e, arrayToString(e.getStackTrace()));
        }
    }

    /**
    * @param b logging toggle
    * Turn logging on or off.
    **/
    public static void setLogging(boolean b) {
        logging = b;
    }
}
