package org.log5j.writer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileLock;
import java.util.Properties;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.log5j.Format;
import org.log5j.Writer;

/**
 * <code>RollingFileWriter</code> writes log messages to a file, and rolls the
 * file either periodically or on the basis of file size.
 * <p>
 * <code>RollingFileWriter</code> recognises the following configuration
 * properties;
 * <ul>
 * <li><code>append</code>: If true, append messages to the end of the file if
 * it exists already; Over-write any existing file if false. The rules for
 * setting the value of <code>append</code> are those of the
 * {@link Boolean#valueOf(String)} method.</li>
 * <li><code>filename</code>: The name of the file to write to. If filename is
 * not an absolute path, It will be relative to the runtime directory of the
 * JVM.</li>
 * <li><code>maxsize</code>: The maximum size of the file (in bytes) before the
 * file should be rolled. This should be an integer value.</li>
 * <li><code>period</code>: The time period between file rolls. The format the
 * period is written in is specified by {@link ISOPeriodFormat#standard()}</li>
 * <li><code>rolltime</code>: The next time the file should be rolled. The
 * format the time is written in is specified by
 * {@link ISODateTimeFormat#basicDateTime()}. Use this in conjunction with
 * <code>period</code> to specify that a file be rolled every hour on the hour,
 * or at 2:00am every morning.</li>
 * </ul>
 * 
 * @author Bruce Ashton
 * @date 2007-07-16
 */
public final class RollingFileWriter extends Writer {

    private static final DateTimeFormatter DATE_FORMAT = ISODateTimeFormat.basicDateTime();

    private static final PeriodFormatter PERIOD_FORMAT = ISOPeriodFormat.standard();

    private File file;

    private FileLock fileLock;

    private final String fileName;

    private final Object lock = new Object();

    private final long maxSize;

    private PrintStream out;

    private final Period period;

    private long rollTime = Long.MAX_VALUE;

    private long size = 0;

    /**
     * Create a new <code>RollingFileWriter</code>.
     * 
     * @param format the <code>Format</code> object for this
     *        <code>RollingFileWriter</code>
     * @param properties configuration properties for this
     *        <code>RollingFileWriter</code>.
     * @throws IOException if the file cannot be opened for writing
     */
    public RollingFileWriter(final Format format, final Properties properties) throws IOException {
        super(format);
        long maxSize;
        try {
            maxSize = Long.valueOf(properties.getProperty("maxsize"));
        } catch (Exception e) {
            maxSize = Long.MAX_VALUE;
        }
        this.maxSize = maxSize;
        Period period;
        try {
            period = PERIOD_FORMAT.parsePeriod(properties.getProperty("period"));
        } catch (Exception e) {
            period = null;
        }
        this.period = period;
        if (period != null) {
            DateTime rollDateTime;
            try {
                rollDateTime = DATE_FORMAT.parseDateTime(properties.getProperty("rolltime"));
            } catch (Exception e) {
                rollDateTime = new DateTime();
            }
            final DateTime now = new DateTime();
            while (now.compareTo(rollDateTime) >= 0) {
                rollDateTime = rollDateTime.withPeriodAdded(period, 1);
            }
            rollTime = rollDateTime.getMillis();
        }
        final boolean append = Boolean.valueOf(properties.getProperty("append"));
        fileName = properties.getProperty("filename");
        if (fileName == null) {
            throw new NullPointerException("The filename property has not been set");
        }
        file = new File(fileName);
        if (append) {
            size = file.length();
        }
        FileOutputStream outputStream = new FileOutputStream(file, append);
        fileLock = outputStream.getChannel().tryLock();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                synchronized (lock) {
                    FileLock fileLock = getFileLock();
                    try {
                        if (fileLock != null) {
                            fileLock.release();
                        }
                    } catch (IOException e) {
                    }
                }
            }
        });
        out = new PrintStream(outputStream);
    }

    private FileLock getFileLock() {
        return fileLock;
    }

    private void roll() {
        try {
            final StringBuilder rollFileName = new StringBuilder(fileName);
            rollFileName.append('.');
            rollFileName.append(System.currentTimeMillis());
            if (fileLock != null) {
                fileLock.release();
            }
            out.close();
            file.renameTo(new File(rollFileName.toString()));
            size = 0;
            file = new File(fileName);
            final FileOutputStream outputStream = new FileOutputStream(file);
            fileLock = outputStream.getChannel().tryLock();
            out = new PrintStream(outputStream);
            if (rollTime != Long.MAX_VALUE && period != null) {
                final DateTime now = new DateTime();
                DateTime rollDateTime = new DateTime(rollTime);
                try {
                    while (now.compareTo(rollDateTime) >= 0) {
                        rollDateTime = rollDateTime.withPeriodAdded(period, 1);
                    }
                    rollTime = rollDateTime.getMillis();
                } catch (Exception e) {
                    e.printStackTrace();
                    rollTime = Long.MAX_VALUE;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void write(final String message) {
        synchronized (lock) {
            if (size > maxSize || System.currentTimeMillis() > rollTime) {
                roll();
            }
            out.println(message);
            out.flush();
            if (message != null) {
                size += message.length();
            } else {
                size += 4;
            }
        }
    }
}
