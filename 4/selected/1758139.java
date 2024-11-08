package org.mule.providers.obex.facade;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.obex.ClientSession;
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
public class ObexClientImpl implements ObexClient {

    private static Log logger = LogFactory.getLog(ObexClientImpl.class);

    /**
	 * 
	 */
    public ObexClientImpl() {
        super();
    }

    @Override
    public void sendData(String serverUrl, String fileName, String type, byte[] data) throws IOException {
        ByteArrayInputStream is = new ByteArrayInputStream(data);
        sendData(serverUrl, fileName, type, is);
    }

    @Override
    public void sendData(String serverUrl, String fileName, String type, InputStream is) throws IOException {
        ClientSession clientSession = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Connecting to " + serverUrl);
            }
            clientSession = (ClientSession) Connector.open(serverUrl);
            HeaderSet hsConnectReply = clientSession.connect(clientSession.createHeaderSet());
            if (hsConnectReply.getResponseCode() != ResponseCodes.OBEX_HTTP_OK) {
                throw new IOException("Connect Error " + hsConnectReply.getResponseCode());
            }
            HeaderSet hsOperation = clientSession.createHeaderSet();
            hsOperation.setHeader(HeaderSet.NAME, fileName);
            if (type != null) {
                hsOperation.setHeader(HeaderSet.TYPE, type);
            }
            hsOperation.setHeader(HeaderSet.LENGTH, new Long(is.available()));
            Operation po = clientSession.put(hsOperation);
            OutputStream os = po.openOutputStream();
            IOUtils.copy(is, os);
            os.flush();
            os.close();
            if (logger.isDebugEnabled()) {
                logger.debug("put responseCode " + po.getResponseCode());
            }
            po.close();
            HeaderSet hsDisconnect = clientSession.disconnect(null);
            if (logger.isDebugEnabled()) {
                logger.debug("disconnect responseCode " + hsDisconnect.getResponseCode());
            }
            if (hsDisconnect.getResponseCode() != ResponseCodes.OBEX_HTTP_OK) {
                throw new IOException("Send Error " + hsConnectReply.getResponseCode());
            }
        } finally {
            if (clientSession != null) {
                try {
                    clientSession.close();
                } catch (IOException ignore) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("IOException during clientSession.close()", ignore);
                    }
                }
            }
            clientSession = null;
        }
    }
}
