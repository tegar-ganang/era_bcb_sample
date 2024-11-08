package projectawesome;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**<b>Project AWESOME</b> <br/>
 * The class containing all the logging functions. When a class is initialized
 * it is given a unique integer id which is stored as the index in a list of
 * {@link DebugInfo} objects.
 * @author Karl Linderhed and Anton Sundblad
 */
public class Debug {

    public static final String strDelim = "§";

    /**
     * A logging level flag for errors that make the continued run of the program
     * impossible.
     */
    public static byte FLAG_FATAL = 1;

    /**
     * A logging level flag for errors that don't require program termination.
     */
    public static byte FLAG_ERROR = 2;

    /**
     * A logging level flag for general events in the program.
     */
    public static byte FLAG_NOTICE = 4;

    /**
     * A generic logging level flag for anything that needs to be logged while
     * debugging.
     */
    public static byte FLAG_DEBUG = 8;

    public static LinkedList<DebugInfo> lIdInUse = new LinkedList<DebugInfo>();

    public static int nId = 0;

    private static String strLogFile = "debug.log";

    private static File fLog = new File(strLogFile);

    private static BufferedWriter out;

    private static int nDebugId = Debug.getId(Debug.class.getName(), Thread.currentThread().toString());

    /**
     * Initializes the logfile.
     */
    public static void init() {
        if (!fLog.exists()) {
            try {
                fLog.createNewFile();
                out = new BufferedWriter(new FileWriter(fLog, true));
            } catch (IOException ex) {
                System.out.println("Could not create new logfile, location:" + fLog.getAbsolutePath());
            }
        } else {
            try {
                out = new BufferedWriter(new FileWriter(fLog, true));
            } catch (IOException e) {
                System.out.println("Could not create a writer handle to the logfile, location:" + fLog.getAbsolutePath() + ", error: " + e.toString());
            }
        }
    }

    /**
     * Static method as a way for other classes to register their use of the debug
     * system. Simply adds a new {@link DebugInfo} to the list at a new specified index.
     * @param strClass the name of the class calling getID.
     * @param strThread the name of the current thread calling getID.
     * @return an unique id number for the specified class and thread.
     */
    public static int getId(String strClass, String strThread) {
        lIdInUse.add(nId, new DebugInfo(strClass, strThread));
        return nId++;
    }

    /**
     * A way for classes to unregister their use of the debug system. Deletes
     * the specified id from the list.
     * @param nId The id number to delete.
     */
    public static void deleteId(int nId) {
        lIdInUse.remove(nId);
    }

    /**
     * Logs a message in the logfile.
     * @param strMessage the message to log.
     * @param nId the unique id for the class calling log().
     * @param nFlags a byte with bitwise flags specifying the log level.
     * @see projectawesome.Debug#FLAG_FATAL
     * @see projectawesome.Debug#FLAG_ERROR
     * @see projectawesome.Debug#FLAG_NOTICE
     * @see projectawesome.Debug#FLAG_DEBUG
     */
    public static void log(String strMessage, int nId, byte nFlags) {
        strMessage = strMessage.replace("\n", "*n");
        try {
            out.write(getDate() + ":" + lIdInUse.get(nId).strClass + ":" + lIdInUse.get(nId).strThread + strDelim + strMessage + strDelim + nFlags + "\r\n");
            out.flush();
        } catch (IOException e) {
            System.err.println("Couldn't log message: " + strMessage + " Location: " + fLog.getAbsolutePath());
        }
    }

    /**
     * Logs an array of messages in the logfile.
     * @param strMessageArr the String array containing the messages to log.
     * @param nId the unique id for the class calling log().
     * @param nFlags a byte with bitwise flags specifying the log level.
     * @see debuging.Debug#FLAG_FATAL
     * @see debuging.Debug#FLAG_ERROR
     * @see debuging.Debug#FLAG_NOTICE
     * @see debuging.Debug#FLAG_DEBUG
     */
    public static void log(String[] strMessageArr, int nId, byte nFlags) {
        for (String strArray : strMessageArr) {
            strArray = strArray.replace("\n", "*n");
        }
        try {
            out.write(getDate() + ":" + lIdInUse.get(nId).strClass + ":" + lIdInUse.get(nId).strThread + strDelim + "Dumping array" + strDelim + nFlags + "\r\n");
            for (int i = 0; i < strMessageArr.length; i++) {
                out.write(getDate() + ":" + lIdInUse.get(nId).strClass + ":" + lIdInUse.get(nId).strThread + strDelim + strMessageArr[i] + strDelim + nFlags + "\r\n");
            }
            out.write(getDate() + ":" + lIdInUse.get(nId).strClass + ":" + lIdInUse.get(nId).strThread + strDelim + "Finished dumping array" + strDelim + nFlags + "\r\n");
            out.flush();
        } catch (IOException e) {
            log("in log(String[]), couldn't log array to file, location: " + fLog.getAbsolutePath(), nDebugId, FLAG_ERROR);
        }
    }

    /**
     * Logs a list of corresponding keys and String values in the logfile.
     * @param strKeyArr the String array containing the keys.
     * @param strValueArr the String array containing the values to log.
     * @param nId the unique id for the class calling log().
     * @param nFlags a byte with bitwise flags specifying the log level.
     * @see debuging.Debug#FLAG_FATAL
     * @see debuging.Debug#FLAG_ERROR
     * @see debuging.Debug#FLAG_NOTICE
     * @see debuging.Debug#FLAG_DEBUG
     */
    public static void log(String[] strKeyArr, String[] strValueArr, int nId, byte nFlags) {
        if (strKeyArr.length != strValueArr.length) {
            log("strKeyArr and strValueArr are not of equal size, can't log key->value", nDebugId, FLAG_ERROR);
            return;
        }
        try {
            out.write(getDate() + ":" + lIdInUse.get(nId).strClass + ":" + lIdInUse.get(nId).strThread + strDelim + "Dumping key->value" + strDelim + nFlags + "\r\n");
            for (int i = 0; i < strKeyArr.length; i++) {
                out.write(getDate() + ":" + lIdInUse.get(nId).strClass + ":" + lIdInUse.get(nId).strThread + strDelim + "Key: " + strKeyArr[i] + " -> " + strValueArr[i] + strDelim + nFlags + "\r\n");
            }
            out.write(getDate() + ":" + lIdInUse.get(nId).strClass + ":" + lIdInUse.get(nId).strThread + strDelim + "Finished dumping key->value" + strDelim + nFlags + "\r\n");
            out.flush();
        } catch (IOException e) {
            log("in log(String[], String[]), couldn't log key->values pairs to file, location: " + fLog.getAbsolutePath(), nDebugId, FLAG_ERROR);
        }
    }

    /**
     * Logs a list of corresponding keys and integer values in the logfile.
     * @param strKeyArr the String array containing the keys.
     * @param nValueArr the String array containing the values to log.
     * @param nId the unique id for the class calling log().
     * @param nFlags a byte with bitwise flags specifying the log level.
     * @see debuging.Debug#FLAG_FATAL
     * @see debuging.Debug#FLAG_ERROR
     * @see debuging.Debug#FLAG_NOTICE
     * @see debuging.Debug#FLAG_DEBUG
     */
    public static void log(String[] strKeyArr, int[] nValueArr, int nId, byte nFlags) {
        if (strKeyArr.length != nValueArr.length) {
            log("strKeyArr and nValueArr are not of equal size, can't log key->value", Debug.nDebugId, FLAG_ERROR);
            return;
        }
        try {
            out.write(getDate() + ":" + lIdInUse.get(nId).strClass + ":" + lIdInUse.get(nId).strThread + strDelim + "Dumping key->value" + strDelim + nFlags + "\r\n");
            for (int i = 0; i < strKeyArr.length; i++) {
                out.write(getDate() + ":" + lIdInUse.get(nId).strClass + ":" + lIdInUse.get(nId).strThread + strDelim + "Key: " + strKeyArr[i] + " -> " + nValueArr[i] + strDelim + nFlags + "\r\n");
            }
            out.write(getDate() + ":" + lIdInUse.get(nId).strClass + ":" + lIdInUse.get(nId).strThread + strDelim + "Finished dumping key->value" + strDelim + nFlags + "\r\n");
            out.flush();
        } catch (IOException e) {
            log("in log(String[], int[]), couldn't log key->values pairs to file, location: " + fLog.getAbsolutePath(), Debug.nDebugId, FLAG_ERROR);
        }
    }

    /**
     *
     * @return a string with the current time and date in the format
     * YYYY-MM-DD-HH-MM-SS
     */
    private static String getDate() {
        String strDate = "";
        Calendar c = Calendar.getInstance();
        strDate += c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DATE) + "-" + c.get(Calendar.HOUR_OF_DAY) + "-" + c.get(Calendar.MINUTE) + "-" + c.get(Calendar.SECOND);
        return strDate;
    }
}
