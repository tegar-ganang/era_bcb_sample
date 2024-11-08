package freemail.protocols.initiation;

import java.io.*;
import java.math.*;
import java.security.*;
import java.util.*;
import java.util.logging.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;
import fcp.*;
import fcp.sessions.*;
import freemail.*;
import freemail.async.*;
import freemail.documents.*;
import freemail.protocols.*;
import freemail.util.*;

/**
 * <p>This class responds to channel requests by constructing an active
 * Channel object locally and a Receipt for the channel requester.</p>
 *
 * @author Adam Thomason
 **/
public class ReceiptGenerator implements CallbackRunnable {

    /**
     * Constructs a new ReceiptGenerator, which may be used asynchronously
     * using the CallbackThread mechanism.
     *
     * @param request the request document.
     * @param requestee the requestee's mailstore, where the resultant
     *     channel will be held.
     * @param fcpManager a connection to a Freenet node.
     **/
    public ReceiptGenerator(Request request, Mailstore requestee, FCPManager fcpManager) {
        log = Logger.getLogger(this.getClass().getName());
        LogManager.getLogManager().addLogger(log);
        this.request = request;
        this.requestee = requestee;
        this.fcpManager = fcpManager;
    }

    /**
     * Processes a channel request and produces the corresponding channel.
     **/
    public void run() throws ProtocolException {
        try {
            log.info("Generating requestee-to-requester encryption key.");
            KeyGenerator kgen = KeyGenerator.getInstance(Constants.MESSAGING_CIPHER);
            kgen.init(Constants.MESSAGING_CIPHER_STRENGTH);
            SecretKey ee2erSecretKey = kgen.generateKey();
            Cipher ee2erCipher = Cipher.getInstance(Constants.MESSAGING_TRANSFORMATION);
            ee2erCipher.init(Cipher.ENCRYPT_MODE, ee2erSecretKey);
            AlgorithmParameters ee2erCryptoParams = ee2erCipher.getParameters();
            log.info("Generating requestee-to-requester channel name.");
            String ee2erChannelName = RandomString.generate(30);
            encryptedReceipt = new EncryptedReceipt(request.getNegotiationSecretKey(), request.getNegotiationCryptoParams(), new Receipt(ee2erSecretKey, ee2erCryptoParams, ee2erChannelName));
            log.info("Channel request receipt generated.");
            channel = new Channel(request.getRequesterAlias(), request.getRequesterPublicSSKKey(), requestee.getPrivateSSKKey(), requestee.getPublicSSKKey());
            channel.setExpectingReceipt(false);
            channel.setInChannelEnabled(true);
            channel.configureInbound(request.getEr2eeChannelName(), request.getEr2eeSecretKey(), request.getEr2eeCryptoParams(), 0);
            channel.setOutChannelEnabled(true);
            channel.configureOutbound(ee2erChannelName, ee2erSecretKey, ee2erCryptoParams, 0);
        } catch (Exception e) {
            throw new ProtocolException(e);
        }
    }

    /**
     * @return the constructed channel.
     **/
    public Channel getChannel() {
        return channel;
    }

    /**
     * @return the constructed receipt.
     **/
    public EncryptedReceipt getEncryptedReceipt() {
        return encryptedReceipt;
    }

    /**
     * @return the request that triggered this receipt.
     **/
    public Request getRequest() {
        return request;
    }

    /**
     * The channel request.
     **/
    protected Request request;

    /**
     * The requestee's mailstore.
     **/
    protected Mailstore requestee;

    /**
     * An FCP connection.
     **/
    protected FCPManager fcpManager;

    /**
     * The log object for status updates.
     **/
    protected Logger log;

    /**
     * The constructed channel.
     **/
    protected Channel channel;

    /**
     * The request receipt.
     **/
    protected EncryptedReceipt encryptedReceipt;

    public void setLogger(Logger log) {
        this.log = log;
    }
}
