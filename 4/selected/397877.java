package blueprint4j.utils;

import java.io.*;
import java.util.*;

public class LoggingMixer extends Logging {

    Vector logs = new Vector();

    public LoggingMixer(boolean debug, boolean trace, boolean critical) {
        super(debug, trace, critical);
    }

    public void addLogging(Logging log) {
        logs.add(log);
    }

    public void write(String level, long thread_id, String description, String details) {
        for (int t = 0; t < logs.size(); t++) {
            ((Logging) logs.get(t)).write(level, thread_id, description, details);
        }
    }

    /**
	 * Build logs from all the Loggers and renove the duplicates
	 */
    public LogDataUnitVector findLogs(Integer tid, java.util.Date from_date, java.util.Date to_date, Logging.Level levels[]) throws IOException {
        LogDataUnitVector log_data_units = new LogDataUnitVector();
        int size = 0;
        for (int t = 0; t < logs.size(); t++) {
            LogDataUnitVector v_log_data_units = ((Logging) logs.get(t)).findLogs(tid, from_date, to_date, levels);
            log_data_units.add(v_log_data_units);
        }
        return log_data_units;
    }

    public void close() {
        for (int t = 0; t < logs.size(); t++) {
            ((Logging) logs.get(t)).close();
        }
    }
}
