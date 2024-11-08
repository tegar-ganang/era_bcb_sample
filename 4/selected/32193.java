package org.fxplayer.util.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The Class TranscodeFileInputStream.
 */
public class TranscodeFileInputStream extends InputStream {

    /**
	 * The Class ErrorStreamReader.
	 */
    private class ErrorStreamReader extends Thread {

        /** The _is. */
        private InputStream _is;

        /**
		 * Instantiates a new error stream reader.
		 */
        private ErrorStreamReader() {
            _is = proc.getErrorStream();
        }

        @Override
        public void run() {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(_is));
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    stderr.append(line).append(System.getProperty("line.separator"));
                    if (LOG.isTraceEnabled()) LOG.trace(line);
                }
            } catch (final IOException ioe) {
            } finally {
                IOUtils.closeQuietly(_is);
                IOUtils.closeQuietly(reader);
            }
        }
    }

    /**
	 * The Class InputStreamReaderThread.
	 */
    private class InputStreamReaderThread extends Thread {

        /** The _is. */
        private InputStream _is;

        /**
		 * Instantiates a new input stream reader thread.
		 * @param in
		 *          the in
		 */
        private InputStreamReaderThread(final InputStream in) {
            _is = in;
        }

        @Override
        public void run() {
            try {
                IOUtils.copy(_is, processOutStr);
            } catch (final IOException ioe) {
                proc.destroy();
            } finally {
                IOUtils.closeQuietly(_is);
                IOUtils.closeQuietly(processOutStr);
            }
        }
    }

    /**
	 * The Class WaiterThread.
	 */
    private class WaiterThread extends Thread {

        /**
		 * Instantiates a new waiter thread.
		 */
        private WaiterThread() {
            super(command[0]);
        }

        @Override
        public void run() {
            try {
                returnCode = proc.waitFor();
            } catch (final InterruptedException e) {
                LOG.fatal("", e);
            } finally {
                if (returnCode != 0) {
                    LOG.warn("Process has exited with code :" + returnCode);
                    if (stderr.length() != 0) LOG.warn(stderr.toString());
                }
            }
        }
    }

    /** The Constant LOG. */
    private static final Log LOG = LogFactory.getLog(TranscodeFileInputStream.class);

    /**
	 * Gets the system property.
	 * @param propName
	 *          the prop name
	 * @return the system property
	 * @throws IOException
	 *           Signals that an I/O exception has occurred.
	 */
    protected static String getSystemProperty(final String propName) throws IOException {
        final String trPath = System.getProperty(propName);
        if (trPath == null) throw new IOException("System property " + propName + " not found");
        return trPath;
    }

    /** The command. */
    private String[] command;

    /** The max bit rate. */
    private int maxBitRate;

    /** The proc. */
    private Process proc;

    /** The process in str. */
    private InputStream processInStr;

    /** The process out str. */
    private OutputStream processOutStr;

    /** The return code. */
    private volatile int returnCode = -1;

    /** The stderr. */
    private final StringBuilder stderr = new StringBuilder();

    /** The track path. */
    private String trackPath;

    /**
	 * Instantiates a new transcode file input stream.
	 * @param runnablePath
	 *          the runnable path
	 * @param runnableParams
	 *          the runnable params
	 * @param trackPath
	 *          the track path
	 * @param maxBitRate
	 *          the max bit rate
	 * @throws IOException
	 *           Signals that an I/O exception has occurred.
	 */
    public TranscodeFileInputStream(final String runnablePath, final String runnableParams, final String trackPath, final int maxBitRate) throws IOException {
        try {
            this.trackPath = trackPath;
            this.maxBitRate = maxBitRate;
            command = getTranscodingCommand(runnablePath, runnableParams);
            if (LOG.isDebugEnabled()) {
                final StringBuilder cmdStr = new StringBuilder("Executing : ");
                for (final String tok : command) cmdStr.append("[").append(tok).append("] ");
                LOG.debug(cmdStr.toString());
            }
            proc = Runtime.getRuntime().exec(command);
            new ErrorStreamReader().start();
            new WaiterThread().start();
            processInStr = proc.getInputStream();
            processOutStr = proc.getOutputStream();
        } catch (final Exception e) {
            LOG.fatal("Transcoding error, please check that you've correctly set the System properties", e);
            final IOException exception = new IOException();
            exception.initCause(e);
            throw exception;
        }
    }

    /**
	 * Instantiates a new transcode file input stream.
	 * @param runnablePath
	 *          the runnable path
	 * @param runnableParams
	 *          the runnable params
	 * @param trackPath
	 *          the track path
	 * @param maxBitRate
	 *          the max bit rate
	 * @param previousStream
	 *          the previous stream
	 * @throws IOException
	 *           Signals that an I/O exception has occurred.
	 */
    public TranscodeFileInputStream(final String runnablePath, final String runnableParams, final String trackPath, final int maxBitRate, final InputStream previousStream) throws IOException {
        this(runnablePath, runnableParams, trackPath, maxBitRate);
        new InputStreamReaderThread(previousStream).start();
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(processInStr);
        IOUtils.closeQuietly(processOutStr);
        if (proc != null) proc.destroy();
    }

    /**
	 * Gets the transcoding command.
	 * @param runnablePath
	 *          the runnable path
	 * @param runnableParams
	 *          the runnable params
	 * @return the transcoding command
	 */
    private String[] getTranscodingCommand(final String runnablePath, final String runnableParams) {
        final ArrayList<String> cmd = new ArrayList<String>();
        cmd.add(runnablePath);
        cmd.addAll(Arrays.asList(runnableParams.split(" ")));
        final ArrayList<String> returnLst = new ArrayList<String>(cmd.size());
        for (final String string : cmd) returnLst.add(string.replace("%bitrate%", String.valueOf(maxBitRate)).replace("%track_path%", trackPath));
        return returnLst.toArray(new String[] {});
    }

    @Override
    public int read() throws IOException {
        return processInStr.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return processInStr.read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return processInStr.read(b, off, len);
    }
}
