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

/**
 * The class responsible for publishing channel requests from the fixed-window
 * dropbox protocol into Freenet.
 *
 * @author Adam Thomason
 **/
public class FixedWindowPublisher implements CallbackRunnable {

    /**
     * Constructs a new FixedWindowPublisher, which may be used asynchronously
     * using the CallbackThread mechanism.
     *
     * @param requestee the identity of the intended contact.
     * @param encryptedRequest the request to publish.
     * @param channel the requested channel.
     * @param fcpManager a connection to a Freenet node.
     **/
    public FixedWindowPublisher(Identity requestee, EncryptedRequest encryptedRequest, Channel channel, FCPManager fcpManager) {
        log = Logger.getLogger(this.getClass().getName());
        LogManager.getLogManager().addLogger(log);
        this.requestee = requestee;
        this.encryptedRequest = encryptedRequest;
        this.channel = channel;
        this.fcpManager = fcpManager;
    }

    /**
     * Publishes the request in Freenet.
     *
     * @exception ProtocolException if the slot insertion fails (see
     *     fcp.SlotInserter).
     **/
    public void run() throws ProtocolException {
        try {
            log.info("Publishing request in Freenet.");
            FCPDocument doc = new FCPDocument();
            doc.setData(encryptedRequest.serialize());
            fcpManager.slotInsertAsync("freenet:KSK@freemail/requests/" + requestee.getPublicSSKKey() + "/", 1, doc);
        } catch (Exception e) {
            throw new ProtocolException(e);
        }
    }

    /**
     * @return the request document as inserted into Freenet.
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
     * The request to publish.
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
