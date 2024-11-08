package base;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Write a description of class LogBot here.
 * 
 * TODO: implement logfile entries cap
 * 
 * @author Peter Andrews 
 * @version 1.5
 */
public class LogBot implements CopyUpdateListener, fullGUI.DirSearchListener {

    private String sendState, filePath;

    private RandomAccessFile writer;

    private final String SEPARATOR = "<<------>>";

    private boolean delOnClose = false;

    /**
     * Creates a log object, given the path to the resident file.
     * 
     * @param where  A string containing the path to the logfile. It really should be absolute/canonical,
     * if only because Java resolves relative paths from the user's home directory.
     */
    public LogBot(String where) {
        if (where == null) {
            where = System.getProperty("user.home") + File.separator + "PSE.log.temp";
            delOnClose = true;
        }
        try {
            writer = new RandomAccessFile(where, "rw");
        } catch (IOException e) {
            System.err.println("There was an error while opening the log file (" + where + ")");
            e.printStackTrace();
        }
        if (compareVersionTo("v1.5.0 alpha 6") < 0) {
            System.err.println("Log file is old; deleting to prevent mix-ups.");
            try {
                writer.close();
                File temp = new File(where);
                temp.delete();
                writer = new RandomAccessFile(where, "rw");
            } catch (IOException e) {
                System.err.println("There was an error while closing, deleting, and re-opening the file (" + where + ")");
                e.printStackTrace();
            }
        } else {
        }
        filePath = where;
    }

    /**
     * Compares the given version string to the last one in the file.
     * 
     * @param in The version to compare to.
     * @return Returns an int greater than 0 if the version in the file is newer
     * than the given version, 0 if they're equal, and something less than 0 if the given
     * is newer.   
     */
    public int compareVersionTo(String in) {
        String ownVersion = getLastRecordedVersion();
        String[] temp, temp1;
        char yours, mine;
        int ctr;
        for (ctr = 1; ctr <= 5; ctr += 2) {
            yours = in.charAt(ctr);
            mine = ownVersion.charAt(ctr);
            if (!(yours == mine)) {
                return 1;
            }
        }
        temp = in.split(" ");
        temp1 = ownVersion.split(" ");
        if (!(temp.length == temp1.length)) {
            System.out.println("diff lengths: " + (temp.length - temp1.length));
            return 1;
        } else if (temp.length == 0) {
            System.out.println("versions match");
            return 1;
        }
        if (!temp[1].equals(temp1[1])) {
            System.out.println("different alpha/beta designations: " + "\n(" + temp[1] + " vs. " + temp1[1] + ")" + (temp1[1].charAt(0) - temp[1].charAt(0)));
            return 1;
        }
        System.out.println("last bit compared lexigraphically: " + temp1[2].compareToIgnoreCase(temp[2]));
        return 1;
    }

    /**
     * Gets the last version string that was recorded in the log file. It searches from
     * the beginning backwards, as per the new standard introduced in 1.5 alpha 5b.
     * @return The last ver. string that was recorded
     */
    public String getLastRecordedVersion() {
        String line = null, ret, defaultVal = "PSE " + AppFrame.getCurrentVersionString() + " started at " + getTimeAndDatestamp();
        int start, end;
        do {
            try {
                line = writer.readLine();
                if (line == null) line = defaultVal;
            } catch (IOException e) {
                System.err.println("There was an error while reading the log file.");
                e.printStackTrace();
            }
        } while (!line.matches("^PSE\\s.+started.+"));
        start = line.indexOf("v");
        end = line.indexOf("s") - 1;
        ret = line.substring(start, end);
        return ret;
    }

    /**
     * Handles <code>CopyUpdateEvent</code>'s for logging purposes.
     * 
     * @param event  The <code>CopyUpdateEvent</code> to be logged.
     * @see base.SongCopyer
     */
    public void copyUpdate(CopyUpdateEvent event) {
        switch(event.getState()) {
            case CopyUpdateEvent.START_EVENT:
                print("Song sending process started at " + getTimestamp() + "\n");
                break;
            case CopyUpdateEvent.START_SENDING_EVENT:
                print("\tTransferring new songs...\n");
                sendState = "sending";
                break;
            case CopyUpdateEvent.START_DELETING_EVENT:
                print("\tRemoving unused old songs...\n");
                sendState = "deleting";
                break;
            case CopyUpdateEvent.TOTAL_EVENT:
                print("\tTotal number of songs to be " + (sendState == "sending" ? "sent" : "checked") + ": " + event.getMessage() + "\n");
                break;
            case CopyUpdateEvent.MESSAGE_EVENT:
                print("\t" + event.getMessage() + "\n");
                break;
            case CopyUpdateEvent.STOP_EVENT:
                print("Song sending process ended at " + getTimestamp() + "\n");
                break;
            default:
                break;
        }
    }

    /**
     * Logs a message.
     * @param in    The string to be logged.
     */
    public void print(String in) {
        try {
            writer.writeChars(in);
        } catch (IOException t) {
            System.err.println("Error while writing to log file. Was trying to write \"" + in + "\".");
            t.printStackTrace();
        }
    }

    /**
     * Does a normal message, repeating verbatum what the input is.
     * @param in    The string to be logged.
     */
    public void doMessage(String in) {
        print(in);
    }

    /**
     * Does an error message, prepending "Error: " to the beginning of the message.
     * @param in    The string to be logged.
     */
    public void doError(String in) {
        print("Error: " + in);
    }

    /**
     * Returns a string containing a time and date stamp.
     * @return A string containing the hour, minute, month, day, and year, in the format "hh:mm on mm/dd/yy".
     */
    public String getTimeAndDatestamp() {
        Calendar cal = new GregorianCalendar();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        return (hour > 9 ? "" : "0") + hour + ":" + (minute > 9 ? "" : "0") + minute + " on " + (month > 9 ? "" : "0") + month + "/" + (day > 9 ? "" : "0") + day + "/" + (year > 9 ? "" : "0") + year;
    }

    /**
     * Generates a string containing a simple timestamp.
     * @return A string containing the hour and minute, in the format "hh:mm".
     */
    public String getTimestamp() {
        Calendar cal = new GregorianCalendar();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        return (hour > 9 ? "" : "0") + hour + ":" + (minute > 9 ? "" : "0") + minute;
    }

    /**
     * Prints the exit message.
     */
    public void doExit() {
        print("PSE exited at " + getTimestamp() + "\n" + SEPARATOR + "\n");
        close();
    }

    /**
     * Logs directory searching events.
     * @param msg   The message.
     */
    public void dirSearchEvent(String msg) {
        print(msg + "\n");
    }

    /**
     * Closes the logfile by calling the internal <code>BufferedWriter</code>'s <code>close()</code> method.
     */
    public void close() {
        try {
            writer.close();
            if (delOnClose) new File(filePath).delete();
        } catch (IOException e) {
            System.err.println("Error while closing log file.");
            e.printStackTrace();
        }
    }
}
