package blueprint4j.utils;

import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.*;

public class LoggingSystemOut extends Logging {

    public static JFrame parent = null;

    SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd 'at' HH'h'mm.ss");

    public LoggingSystemOut(boolean debug, boolean trace, boolean critical) {
        super(debug, trace, critical);
    }

    public LoggingSystemOut(boolean debug, boolean dev, boolean trace, boolean critical) {
        super(debug, dev, trace, critical, false);
    }

    public LoggingSystemOut(boolean debug, boolean dev, boolean trace, boolean critical, boolean message) {
        super(debug, dev, trace, critical, true, message);
    }

    public void write(String level, long thread_id, String description, String details) {
        if ("Message".equals(level)) {
            Utils.createDialog(parent, description, details, false).setVisible(true);
        }
        System.out.println(level + " [" + thread_id + "] " + date_format.format(new Date()) + "\n" + "TCE " + date_format.format(new Date()) + "\n" + "Description: " + description + "\n" + details + "\n" + "-------------------------------------");
        if (Logging.CRITICAL.toString().equals(level)) {
            new Exception("CURRENT STACK TRACE").printStackTrace();
        }
    }

    public LogDataUnitVector findLogs(Integer tid, java.util.Date from_date, java.util.Date to_date, Logging.Level levels[]) throws IOException {
        return new LogDataUnitVector();
    }
}
