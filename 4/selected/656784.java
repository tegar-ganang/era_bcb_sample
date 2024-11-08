package blueprint4j.utils;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * This class is a logging class that writes it's log out to a file.
 * The home directory of all the files is given when the class is created.
 * Per hour a new file is created the nameing convention is as follows.
 * <CODE>
 * Log_YYYY_MM_DD_HH.log
 * </CODE>
 * This way the log file will be easy to sort by date in any file system as well
 * as small. For hours when no loggin occures there will be no log file.
 */
public class LoggingFile extends Logging {

    SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd 'at' HH'h'mm.ss");

    static SimpleDateFormat filename_format = new SimpleDateFormat("'Log'yyyy-MM-dd_HH'.log'");

    String lastfilename = null;

    String location;

    String stop_date;

    OutputStreamWriter output = null;

    static String newname = null;

    boolean inexception = false;

    boolean dont_log = false;

    public LoggingFile(String p_location, boolean debug, boolean dev, boolean trace, boolean critical) throws IOException {
        super(debug, dev, trace, critical, false);
        location = p_location;
        if (!p_location.endsWith("\\") && !p_location.endsWith("/")) {
            p_location = p_location + '/';
        }
        Log.trace.out("LoggingFile", "Logging started in '" + p_location + "'");
    }

    public LoggingFile(String p_location, boolean debug, boolean trace, boolean critical) throws IOException {
        this(p_location, debug, false, trace, critical);
    }

    public static File getLastLogFile() {
        if (newname != null) {
            return new File(newname);
        }
        return null;
    }

    public void write(String level, long thread_id, String description, String details) {
        try {
            if (dont_log) return;
            buildOutputFile();
            output.write(level + " [" + thread_id + "] " + date_format.format(new Date()) + "\n" + "Description: " + description + "\n" + details + "\n" + "-------------------------------------\n");
            output.flush();
            output.close();
            setExceptionModeOff();
        } catch (IOException ioe) {
            setExceptionModeOn(ioe);
        }
    }

    /**
	 * sets the exception mode off
	 */
    private void setExceptionModeOff() {
        if (inexception) {
            inexception = false;
        }
    }

    /**
	 * sets the exception mode for logging.
	 */
    private void setExceptionModeOn(IOException ioe) {
        if (!inexception) {
            stop_date = date_format.format(new Date());
            inexception = true;
            dont_log = true;
            Log.critical.out(ioe);
            dont_log = false;
            lastfilename = null;
        }
    }

    /**
	 * Build the new file writer for outputing the logs to.
	 */
    private void buildOutputFile() throws IOException {
        File directory = new File(location);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IOException("Can't build [" + directory + "]");
            }
        }
        newname = location + filename_format.format(new Date());
        try {
            output = new FileWriter(newname, true);
        } catch (FileNotFoundException fnfe) {
            output = new FileWriter(newname);
        }
    }

    public void close() {
        try {
            if (output != null) {
                output.close();
            }
        } catch (IOException ioe) {
        }
    }

    private LogDataUnit loadLogDataUnitFromString(String buffer, Integer tid, Date from_date, Date to_date, Logging.Level levels[]) throws IOException {
        if (buffer.indexOf("-------------------------------------") != -1) {
            String data_unit = buffer.substring(0, buffer.indexOf("-------------------------------------") + "-------------------------------------".length());
            buffer = buffer.substring(buffer.indexOf("-------------------------------------") + "-------------------------------------".length());
            String level_str = data_unit.substring(0, data_unit.indexOf("[") - 1);
            String tid_str = data_unit.substring(data_unit.indexOf("[") + 1, data_unit.indexOf("]"));
            data_unit = data_unit.substring(data_unit.indexOf("]") + 2);
            String date_date_time = data_unit.substring(0, data_unit.indexOf("\n"));
            data_unit = data_unit.substring(data_unit.indexOf("\n") + 1);
            String description = data_unit.substring(data_unit.indexOf(":") + 1, data_unit.indexOf("\n"));
            data_unit = data_unit.substring(data_unit.indexOf("\n") + 1);
            String detail = data_unit.substring(0, data_unit.indexOf("-------------------------------------"));
            Logging.Level level = null;
            if (tid != null && Integer.parseInt(tid_str) != tid.intValue()) {
                return null;
            }
            if (level_str.equals(Logging.TRACE)) {
                level = Logging.TRACE;
            }
            if (level_str.equals(Logging.DEBUG)) {
                level = Logging.DEBUG;
            }
            if (level_str.equals(Logging.CRITICAL)) {
                level = Logging.CRITICAL;
            }
            boolean found_level = false;
            for (int i = 0; i < levels.length && found_level == false; i++) {
                if (levels[i].toString().equals(level.toString())) {
                    found_level = true;
                }
            }
            if (!found_level) {
                return null;
            }
            Date current_date = null;
            try {
                current_date = date_format.parse(date_date_time);
                if (!(current_date.getTime() >= from_date.getTime() && current_date.getTime() <= to_date.getTime())) {
                    return null;
                }
            } catch (java.text.ParseException pe) {
                throw new IOException(pe.getMessage());
            }
            return new LogDataUnit(level, description, detail, current_date);
        }
        return null;
    }

    /**
	 * Loads all the logs out of the stream
	 */
    private void loadLogDataUnitFromFile(InputStream stream_in, Integer tid, Date from_date, Date to_date, Logging.Level levels[], LogDataUnitVector logs) throws IOException {
        byte buffer[] = new byte[130];
        String str_buffer = "";
        while (stream_in.available() > 0) {
            int r = stream_in.read(buffer);
            str_buffer += new String(buffer, 0, r);
            LogDataUnit log_data_unit = loadLogDataUnitFromString(str_buffer, tid, from_date, to_date, levels);
            if (log_data_unit != null) {
                logs.add(log_data_unit);
            }
        }
        LogDataUnit log_data_unit = loadLogDataUnitFromString(str_buffer, tid, from_date, to_date, levels);
        if (log_data_unit != null) {
            logs.add(log_data_unit);
        }
    }

    /**
	 * retrieve all the logs matching the thread id and from and to_date (which may be null)
	 */
    public LogDataUnitVector findLogs(Integer tid, Date from_date, Date to_date, Logging.Level levels[]) throws IOException {
        if (from_date == null) {
            from_date = new java.util.Date();
        }
        if (to_date == null) {
            to_date = new java.util.Date();
        }
        Calendar calender = Calendar.getInstance();
        calender.setTime(from_date);
        LogDataUnitVector logs = new LogDataUnitVector();
        for (; calender.getTime().getTime() <= to_date.getTime(); calender.add(java.util.Calendar.HOUR, 1)) {
            if (new File(location + "/" + filename_format.format((calender.getTime()))).exists()) {
                FileInputStream file_in = new FileInputStream(location + "/" + filename_format.format((calender.getTime())));
                loadLogDataUnitFromFile(file_in, tid, from_date, to_date, levels, logs);
            }
        }
        return logs;
    }
}
