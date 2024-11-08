package freemail.protocols.initiation;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.security.AlgorithmParameters;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;
import fcp.*;
import fcp.sessions.*;
import freemail.async.*;
import freemail.protocols.*;
import freemail.documents.*;
import freemail.util.*;

/**
 * The class responsible for publishing channel requests from the dynamic-window
 * dropbox protocol into Freenet.
 *
 * @author Adam Thomason
 **/
public class DynamicWindowPublisher implements CallbackRunnable {

    /**
     * Constructs a new DynamicWindowPublisher, which may be used asynchronously
     * using the CallbackThread mechanism.
     *
     * @param requestee the identity of the intended contact.
     * @param encryptedRequest the request to publish.
     * @param channel the requested channel.
     * @param fcpManager a connection to a Freenet node.
     * @param log the log object for status updates.
     **/
    public DynamicWindowPublisher(Identity requestee, EncryptedRequest encryptedRequest, Channel channel, FCPManager fcpManager) {
        this.requestee = requestee;
        this.encryptedRequest = encryptedRequest;
        this.channel = channel;
        this.fcpManager = fcpManager;
        log = Logger.getLogger(this.getClass().getName());
        LogManager.getLogManager().addLogger(log);
    }

    /**
     * Publishes the request in Freenet.
     *
     * @exception ProtocolException if the slot insertion fails (see
     *     fcp.SlotInserter).
     **/
    public void run() throws ProtocolException {
        try {
            log.info("Determining dynamic window location.");
            FCPDocument pointerDocument = new FCPDocument(requestee.getDynamicWindowPointerAddress());
            FCPRequester pointerRequest = fcpManager.request(pointerDocument);
            if (!pointerRequest.getSuccess() || !pointerRequest.getDataFound()) {
                throw new ProtocolException("Could not retrieve dynamic window pointer.", pointerRequest.getCause());
            }
            StringTokenizer t = new StringTokenizer(pointerDocument.getDataString(), "\n");
            if (!t.hasMoreTokens()) {
                throw new ProtocolException("Dynamic window pointer at " + requestee.getDynamicWindowPointerAddress() + " is empty.");
            }
            String windowPrefix = t.nextToken();
            if (!windowPrefix.startsWith("KSK@")) {
                throw new ProtocolException("Dynamic window pointer at " + requestee.getDynamicWindowPointerAddress() + " does not contain a KSK (" + windowPrefix + ").");
            }
            if (!t.hasMoreTokens()) {
                throw new ProtocolException("Dynamic window pointer at " + requestee.getDynamicWindowPointerAddress() + " is missing a start index.");
            }
            int startIndex = 1;
            String startIndexStr = t.nextToken();
            try {
                startIndex = Integer.parseInt(startIndexStr);
            } catch (NumberFormatException e) {
                throw new ProtocolException("Dynamic window pointer at " + requestee.getDynamicWindowPointerAddress() + " has an invalid start index (" + startIndex + ").");
            }
            log.info("Publishing request in Freenet.");
            FCPDocument doc = new FCPDocument();
            doc.setData(encryptedRequest.serialize());
            fcpManager.slotInsertAsync(windowPrefix + requestee.getPublicSSKKey() + "/", startIndex, doc);
        } catch (ProtocolException e) {
            throw e;
        } catch (Exception e) {
            throw new ProtocolException(e);
        }
    }

    /**
     * @return the request as inserted into Freenet.
     **/
    public EncryptedRequest getEncryptedRequest() {
        return encryptedRequest;
    }

    /**
     * @return a Channel object for the requested channel.
     **/
    public Channel getChannel() {
        return channel;
    }

    /**
     * An FCP connection.
     **/
    protected FCPManager fcpManager;

    /**
     * The requestee's identity.
     **/
    protected Identity requestee;

    /**
     * The generated request.
     **/
    protected EncryptedRequest encryptedRequest;

    /**
     * The requested channel.
     **/
    protected Channel channel;

    /**
     * The log object for status updates.
     **/
    protected Logger log;

    public void setLogger(Logger log) {
        this.log = log;
    }
}
