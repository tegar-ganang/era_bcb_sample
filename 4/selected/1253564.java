package com.spinn3r.log5j;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Date;

public class LogUtils {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    public static String toString(LogEvent logEvent) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, UTF8));
        writer.append(logEvent.level().name());
        writer.append(" [");
        writer.append(new Date(logEvent.time()).toString());
        writer.append("]: ");
        writer.append(logEvent.threadName());
        writer.append(" > ");
        writer.append(logEvent.logName());
        writer.append(" - ");
        writer.append(logEvent.message());
        if (logEvent.throwable() != null) {
            writer.append('\n');
            logEvent.throwable().printStackTrace(writer);
        }
        writer.close();
        return baos.toString();
    }

    private LogUtils() {
    }
}
