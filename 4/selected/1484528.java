package com.gc.iotools.stream.is;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import org.apache.commons.io.IOUtils;
import com.gc.iotools.stream.base.ExecutionModel;
import com.gc.iotools.stream.base.ExecutorServiceFactory;

/**
 * <p>
 * This class tries to read the <code>InputStream</code> passed as a parameter
 * in a memory buffer, thus improving the reading performances.
 * </p>
 * <p>
 * This can speed up reading when the time spent in reading from the stream is
 * same order as the time spent in elaborating the stream, because it
 * decouples the reading process and the elaboration in two different threads
 * putting them in parallel.
 * </p>
 * <p>
 * Sample Usage:
 *
 * <pre>
 * InputStream source = ... some slow InputStream.
 * InputStream fastIs = new ReadAheadInputStream(source);
 * //use here fastIs instead of the source InputStream
 * ...
 * fastIs.close();
 * </pre>
 * <p>
 *
 * @author dvd.smnt
 * @since 1.2.6
 * @version $Id: ReadAheadInputStream.java 463 2011-01-21 23:54:17Z dvd.smnt@gmail.com $
 */
public class ReadAheadInputStream extends InputStreamFromOutputStream<Void> {

    private final InputStream source;

    /**
	 * <p>Constructor for ReadAheadInputStream.</p>
	 *
	 * @param source a {@link java.io.InputStream} object.
	 */
    public ReadAheadInputStream(final InputStream source) {
        this(source, -1);
    }

    /**
	 * <p>Constructor for ReadAheadInputStream.</p>
	 *
	 * @param source a {@link java.io.InputStream} object.
	 * @param bufferSize a int.
	 */
    public ReadAheadInputStream(final InputStream source, final int bufferSize) {
        this(source, bufferSize, ExecutionModel.THREAD_PER_INSTANCE);
    }

    /**
	 * <p>Constructor for ReadAheadInputStream.</p>
	 *
	 * @param source a {@link java.io.InputStream} object.
	 * @param bufferSize a int.
	 * @param executionModel a {@link com.gc.iotools.stream.base.ExecutionModel} object.
	 */
    public ReadAheadInputStream(final InputStream source, final int bufferSize, final ExecutionModel executionModel) {
        this(source, bufferSize, ExecutorServiceFactory.getExecutor(executionModel));
    }

    /**
	 * <p>Constructor for ReadAheadInputStream.</p>
	 *
	 * @param source a {@link java.io.InputStream} object.
	 * @param bufferSize a int.
	 * @param executorService a {@link java.util.concurrent.ExecutorService} object.
	 */
    public ReadAheadInputStream(final InputStream source, final int bufferSize, final ExecutorService executorService) {
        super(false, executorService, (bufferSize > 0 ? bufferSize : 65536));
        this.source = source;
    }

    /** {@inheritDoc} */
    @Override
    protected Void produce(final OutputStream sink) throws Exception {
        IOUtils.copy(this.source, sink);
        this.source.close();
        return null;
    }
}
