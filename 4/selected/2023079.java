package org.utupiu.nibbana.log;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Collection;
import org.utupiu.nibbana.log.enums.LogLevel;

public class WriterThread extends Thread {

    private LogWriter mWriter;

    private Collection<LogRow> mLogs;

    protected WriterThread() {
    }

    protected WriterThread(LogWriter writer, Collection<LogRow> logs) {
        this.mWriter = writer;
        this.mLogs = logs;
    }

    public void run() {
        Calendar time = Calendar.getInstance();
        for (LogRow row : mLogs) {
            time.setTimeInMillis(row.Time);
            writeLog(mWriter, row.Level, time, row.Message, row.Throwable);
        }
        mLogs = null;
        mWriter = null;
    }

    protected void writeLog(LogWriter writer, LogLevel logLevel, Calendar time, String message, Throwable throwable) {
        LogLevel logLevelOfRow = logLevel;
        int logLevelOfRowOrdinal = logLevelOfRow.ordinal();
        LogLevel writerLevel = writer.Level;
        if (logLevelOfRowOrdinal < writerLevel.ordinal()) {
            return;
        }
        int year = time.get(Calendar.YEAR);
        int month = time.get(Calendar.MONTH) + 1;
        int date = time.get(Calendar.DAY_OF_MONTH);
        int hour = time.get(Calendar.HOUR_OF_DAY);
        int minute = time.get(Calendar.MINUTE);
        int second = time.get(Calendar.SECOND);
        int millis = time.get(Calendar.MILLISECOND);
        PrintStream stream = writer.PrintStream;
        StringBuffer sb = new StringBuffer();
        if (!(stream instanceof FilePrintStream)) {
            sb.append(writer.Component.shortName);
            sb.append("\t");
        }
        sb.append(logLevelOfRow.toString());
        sb.append("\t");
        sb.append(year);
        sb.append("/");
        sb.append(month);
        sb.append("/");
        sb.append(date);
        sb.append(" ");
        sb.append(hour);
        sb.append(":");
        sb.append(minute);
        sb.append(":");
        sb.append(second);
        sb.append(".");
        sb.append(millis);
        sb.append("\t");
        if (message != null) {
            sb.append(message);
        }
        if (throwable != null) {
            StringWriter result = new StringWriter();
            PrintWriter printWriter = new PrintWriter(result);
            throwable.printStackTrace(printWriter);
            sb.append(result.toString());
        }
        stream.println(sb.toString());
    }
}
