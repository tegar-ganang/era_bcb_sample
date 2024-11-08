package com.volantis.shared.net.impl.url.file;

import com.volantis.shared.net.impl.RunnableTimerTask;
import com.volantis.shared.net.impl.ThreadInterruptingTimingOutTask;
import com.volantis.shared.net.impl.TimingOutTask;
import com.volantis.shared.time.Period;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.Timer;

/**
 * This class is responsible for reading the contents of a file in
 * an interruptable manner.
 */
public class FileReader {

    /**
     * The timer with which all the tasks will be registered.
     *
     * <p>This will create a single daemon thread to manage a queue of
     * tasks.</p>
     */
    private static final Timer TIMER = new Timer(true);

    /**
     * Constant for the file protocol (lowercase).
     */
    private static final String FILE_PROTOCOL = "file";

    /**
     * Retrieve the contents of the file at the given URI.
     *
     * @param fileToRead the file based url to be read from
     * @param timeout period of time after which reading will stop
     * (if not already completed).
     *
     * @return the contents read from this file.
     */
    public FileContent read(URI fileToRead, Period timeout) throws IOException {
        if (!isFileBased(fileToRead)) {
            throw new IllegalArgumentException("URI must be file based, " + fileToRead.toString());
        }
        File toRead = new File(fileToRead);
        return readImpl(toRead, timeout);
    }

    /**
     * Read the contents of the given file. If a timeout value is specified
     * then this read operation will be interrupted if the read takes longer
     * than the specified timeout.
     *
     * @param toRead the file to be read from.
     * @param timeout the timeout.
     *
     * @return an instance of FileContent encapsulating the
     * contents read from the given file.
     */
    private FileContent readImpl(File toRead, Period timeout) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(toRead);
        FileChannel fileChannel = fileInputStream.getChannel();
        TimingOutTask task = null;
        FileContent fileContents = null;
        TimeLimitedInputStream timeLimitedInputStream = null;
        InputStream channelBackedInputStream = Channels.newInputStream(fileChannel);
        timeLimitedInputStream = new TimeLimitedInputStream(channelBackedInputStream);
        task = createAndScheduleTimingOutTask(timeout);
        timeLimitedInputStream.setTimerTask(task);
        fileContents = new FileContent(timeLimitedInputStream, toRead);
        return fileContents;
    }

    /**
     * Creates a configures a {@link TimingOutTask} that will interrupt
     * the read performed in {@link #readImpl(java.io.File, com.volantis.shared.time.Period)}
     * should the read time exceed the timeout value.
     *
     * @param timeout the timeout to be applied.
     *
     * @return the scheduled timeout task.
     */
    private TimingOutTask createAndScheduleTimingOutTask(Period timeout) {
        TimingOutTask task = new ThreadInterruptingTimingOutTask();
        if (timeout != null && timeout != Period.INDEFINITELY) {
            TIMER.schedule(new RunnableTimerTask(task), timeout.inMillis());
        }
        return task;
    }

    /**
     * Returns true if the given url uses the {@link #FILE_PROTOCOL}.
     *
     * @param uri the uri to be tested for being file based.
     */
    private boolean isFileBased(URI uri) {
        boolean isFileBased = false;
        String protocol = uri.getScheme().toLowerCase();
        if (FILE_PROTOCOL.equals(protocol)) {
            isFileBased = true;
        }
        return isFileBased;
    }
}
