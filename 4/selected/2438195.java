package org.colombbus.tangara.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A thread reading the content of a stream. It ends when the stream is empty.
 * It returns the content of the stream as a String
 * 
 * @version $Id: StringReaderThread.java,v 1.1 2009/03/22 08:58:18 gwenael.le_roux Exp $
 */
public class StringReaderThread extends Thread {

    private Log LOG = LogFactory.getLog(StringReaderThread.class);

    /** Input stream to read */
    private InputStream inputStream;

    /** input stream content */
    private StringWriter streamBuffer = new StringWriter();

    private IOException readException;

    /**
	 * Create a new thread reading an input stream and converting it to a string
	 * 
	 * @param inStream
	 *            input stream to read
	 */
    public StringReaderThread(InputStream inStream) {
        if (inStream == null) {
            throw new IllegalArgumentException("inStream cannot be null");
        }
        this.inputStream = inStream;
    }

    @Override
    public void run() {
        try {
            readStreamContent();
        } catch (IOException ioEx) {
            readException = ioEx;
            LOG.warn("Fail reading stream", ioEx);
        }
    }

    private void readStreamContent() throws IOException {
        int readByte;
        while ((readByte = inputStream.read()) != -1) streamBuffer.write(readByte);
    }

    /**
	 * Get the read string of the stream as this moment
	 * 
	 * @return a non <code>null</code> text
	 */
    public String getStreamString() {
        return streamBuffer.toString();
    }

    /**
	 * Get the failure status of the stream reading process
	 * 
	 * @return <code>true</code> if the thread is not started or if no error has
	 *         occur at this moment, <code>false</code> of the stream reading
	 *         failed.
	 */
    public boolean failed() {
        return readException != null;
    }

    /**
	 * Get the exception thrown during the stream read
	 * 
	 * @return a {@link IOException} or <code>null</code> if the thread is not
	 *         started or if no error occurs at this moment, or an exception
	 *         throw by the read stream.
	 */
    public IOException getReadException() {
        return readException;
    }
}
