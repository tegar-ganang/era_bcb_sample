package blueprint4j.utils;

import java.io.*;
import java.util.*;
import java.math.*;

public class LogOld {

    static Vector logs = new Vector();

    private LogOld() {
    }

    public static void addLogging(Logging log) {
        logs.add(log);
    }

    public static synchronized void writeLog(Logging.Level lvl, Throwable details) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        details.printStackTrace(new PrintWriter(buffer, true));
        writeLog(lvl, details.getMessage(), new String(buffer.toByteArray()));
    }

    public static synchronized void writeLog(Logging.Level lvl, String description, String details) {
        for (int t = 0; t < logs.size(); t++) {
            ((Logging) logs.get(t)).writeLog(lvl, ThreadId.getCurrentId(), description, details);
        }
        if (lvl.toString().equals(Logging.CRITICAL.toString())) {
            System.exit(-1);
        }
    }

    public static synchronized void writeLog(Logging.Level lvl, String description, Throwable throwable) {
        for (int t = 0; t < logs.size(); t++) {
            ((Logging) logs.get(t)).writeLog(lvl, ThreadId.getCurrentId(), description, throwable);
        }
    }

    public static synchronized LogDataUnitVector findLogs(Integer tid, java.util.Date from_date, java.util.Date to_date, Logging.Level levels[]) throws IOException {
        LogDataUnitVector log_data_units = new LogDataUnitVector();
        int size = 0;
        for (int t = 0; t < logs.size(); t++) {
            LogDataUnitVector v_log_data_units = ((Logging) logs.get(t)).findLogs(tid, from_date, to_date, levels);
            log_data_units.add(v_log_data_units);
        }
        return log_data_units;
    }

    public static synchronized void writeLog(Logging.Level lvl, String description, Properties props) {
        String propstr = "";
        try {
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            props.store(ba, "");
            propstr = ba.toString();
        } catch (IOException e) {
        }
        LogOld.writeLog(lvl, description, propstr);
    }

    public static synchronized void writeLog(Logging.Level lvl, String description, byte[] data) {
        StringBuffer strdata = new StringBuffer();
        StringBuffer pos = new StringBuffer();
        StringBuffer hex = new StringBuffer();
        StringBuffer str = new StringBuffer();
        for (int b = 0; b < data.length; b += 16) {
            String one;
            pos.delete(0, pos.length());
            hex.delete(0, hex.length());
            str.delete(0, str.length());
            while (pos.length() < 4) pos.insert(0, "0");
            for (int u = 0; u < 16; u++) {
                if (b + u < data.length) {
                    int value = data[b + u];
                    if (value < 0) value += 128;
                    one = new BigInteger("" + value).toString(16);
                    while (one.length() < 2) one = "0" + one;
                    hex.append(one);
                    if (u == 7) {
                        hex.append("-");
                    } else {
                        hex.append(" ");
                    }
                    if (value >= 32 && value <= 127) {
                        str.append(new String(new byte[] { (byte) value }));
                    } else {
                        str.append(".");
                    }
                } else {
                    hex.append("   ");
                    str.append(" ");
                }
            }
            strdata.append(pos + ":  " + hex + "  " + str + "\r\n");
        }
        for (int t = 0; t < logs.size(); t++) {
            ((Logging) logs.get(t)).writeLog(lvl, ThreadId.getCurrentId(), description, strdata.toString());
        }
    }

    private static Date next_cleanup = new Date();

    static boolean isTimeForNextCleanup() {
        return next_cleanup.compareTo(new Date()) <= 0;
    }

    static void doCleanUp() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, 15);
        next_cleanup = cal.getTime();
    }

    public static void close() {
        for (int t = 0; t < logs.size(); t++) {
            ((Logging) logs.get(t)).close();
        }
        logs = new Vector();
    }
}
