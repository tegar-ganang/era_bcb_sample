package org.mule.providers.obex.facade;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Jeroen Benckhuijsen (jeroen.benckhuijsen@gmail.com)
 * 
 */
public class MuleObexRequestHandler extends RequestHandler {

    private static Log logger = LogFactory.getLog(MuleObexRequestHandler.class);

    private byte[] data;

    private HeaderSet headers;

    private boolean interrupted;

    private Long maxFileSize;

    private static Integer connections = 0;

    /**
	 * @param obexMessageDispatcher
	 */
    public MuleObexRequestHandler(Long maxFileSize) {
        super();
        this.maxFileSize = maxFileSize;
    }

    @Override
    public int onPut(Operation operation) {
        synchronized (MuleObexRequestHandler.connections) {
            MuleObexRequestHandler.connections++;
            if (logger.isDebugEnabled()) {
                logger.debug("Connection accepted, total number of connections: " + MuleObexRequestHandler.connections);
            }
        }
        int result = ResponseCodes.OBEX_HTTP_OK;
        try {
            headers = operation.getReceivedHeaders();
            if (!this.maxFileSize.equals(ObexServer.UNLIMMITED_FILE_SIZE)) {
                Long fileSize = (Long) headers.getHeader(HeaderSet.LENGTH);
                if (fileSize == null) {
                    result = ResponseCodes.OBEX_HTTP_LENGTH_REQUIRED;
                }
                if (fileSize > this.maxFileSize) {
                    result = ResponseCodes.OBEX_HTTP_REQ_TOO_LARGE;
                }
            }
            if (result != ResponseCodes.OBEX_HTTP_OK) {
                InputStream in = operation.openInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                IOUtils.copy(in, out);
                in.close();
                out.close();
                data = out.toByteArray();
                if (interrupted) {
                    data = null;
                    result = ResponseCodes.OBEX_HTTP_GONE;
                }
            }
            return result;
        } catch (IOException e) {
            return ResponseCodes.OBEX_HTTP_UNAVAILABLE;
        } finally {
            synchronized (this) {
                this.notify();
            }
            synchronized (MuleObexRequestHandler.connections) {
                MuleObexRequestHandler.connections--;
                if (logger.isDebugEnabled()) {
                    logger.debug("Connection closed, total number of connections: " + MuleObexRequestHandler.connections);
                }
            }
        }
    }

    /**
	 * @throws InterruptedException
	 */
    public void waitForMessage() throws InterruptedException {
        synchronized (this) {
            this.wait();
        }
    }

    /**
	 * @param handler
	 * @throws InterruptedException
	 */
    public static void waitForHandler(MuleObexRequestHandler handler) throws InterruptedException {
        handler.waitForMessage();
    }

    /**
	 * 
	 */
    public void interrupt() {
        interrupted = true;
    }

    /**
	 * @return the data
	 */
    public byte[] getData() {
        return data;
    }

    /**
	 * @return the headers
	 */
    public HeaderSet getHeaders() {
        return headers;
    }
}
