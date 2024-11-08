package blueprint4j.db;

import java.io.*;
import java.text.*;
import java.util.*;
import blueprint4j.utils.*;

public class LoggingDB extends Logging {

    private static LoggingDB loggingdb = null;

    static {
        try {
            loggingdb = new LoggingDB(DBTools.getDLC(), false, false, false);
        } catch (Throwable exception) {
            exception.printStackTrace();
            Log.critical.out(exception);
        }
    }

    private SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd 'at' HH'h'mm.ss");

    private DBConnection connection = null;

    public LoggingDB(DBConnection p_connection, boolean debug, boolean trace, boolean critical) {
        super(debug, trace, critical);
        connection = p_connection;
    }

    public LoggingDB(DBConnection p_connection, boolean debug, boolean trace, boolean critical, boolean comm) {
        super(debug, trace, critical, comm);
        connection = p_connection;
    }

    public static LoggingDB getLoggingDB() {
        return loggingdb;
    }

    public void write(String level, long thread_id, String description, String details) {
        try {
            DBLog dblog = new DBLog(connection);
            if (description == null) {
                description = "";
            }
            if (description.length() > 100) {
                description = description.substring(0, 100);
            }
            dblog.log_level.set(level);
            dblog.description.set(description);
            dblog.detail.set(details);
            dblog.logtime.set(new java.util.Date());
            dblog.save();
            connection.commit();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public LogDataUnitVector findLogs(Integer tid, java.util.Date from_date, java.util.Date to_date, Logging.Level levels[]) throws IOException {
        try {
            VectorLogDataUnit data_unit_vector = new VectorLogDataUnit();
            for (DBLog dblog = DBLog.findLogs(connection, tid, from_date, to_date, levels); dblog != null; dblog = (DBLog) dblog.getNextBindable()) {
                data_unit_vector.add(dblog.transfromToLogDataUnit());
            }
            return new LogDataUnitVector();
        } catch (Exception ex) {
            IOException ioe = new IOException(ex.getMessage());
            ioe.setStackTrace(ex.getStackTrace());
            throw ioe;
        }
    }
}
