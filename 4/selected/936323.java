package freemail.protocols.mail;

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
 * The class responsible for publishing mail messages into Freenet.
 *
 * @author Adam Thomason
 **/
public class MessageSender implements CallbackRunnable {

    /**
     * Constructs a new MessageSender, which may be used asynchronously
     * using the CallbackThread mechanism.
     *
     * @param channel the requested channel.
     * @param encryptedMessage the message to publish.
     * @param fcpManager a connection to a Freenet node.
     **/
    public MessageSender(Channel channel, EncryptedMessage encryptedMessage, FCPManager fcpManager) {
        log = Logger.getLogger(this.getClass().getName());
        LogManager.getLogManager().addLogger(log);
        this.channel = channel;
        this.encryptedMessage = encryptedMessage;
        this.fcpManager = fcpManager;
    }

    /**
     * Publishes the message in Freenet.
     *
     * @exception ProtocolException if the message insertion fails.
     **/
    public void run() throws ProtocolException {
        log.info("Publishing message in Freenet.");
        FCPDocument doc = new FCPDocument("freenet:SSK@" + channel.getSelfPrivateSSKKey() + "/" + channel.getOutChannelName() + "/" + (channel.getOutLastMessageIndex() + 1));
        try {
            doc.setData(encryptedMessage.serialize());
        } catch (DocumentException e) {
            throw new ProtocolException(e);
        }
        fcpManager.insertAsync(doc);
    }

    /**
     * @return the request document as inserted into Freenet.
     **/
    public EncryptedMessage getEncryptedMessage() {
        return encryptedMessage;
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
     * The request to publish.
     **/
    protected EncryptedMessage encryptedMessage;

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
