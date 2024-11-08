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
import freemail.util.*;

/**
 * The class responsible for retrieving mail messages from Freenet.
 *
 * @author Adam Thomason
 **/
public class MessageRetriever implements CallbackRunnable {

    /**
     * Constructs a new MessageRetriever, which may be used asynchronously
     * using the CallbackThread mechanism.
     *
     * @param mailstore the mailstore of the user.
     * @param fcpManager a connection to a Freenet node.
     **/
    public MessageRetriever(Mailstore mailstore, FCPManager fcpManager) {
        log = Logger.getLogger(this.getClass().getName());
        LogManager.getLogManager().addLogger(log);
        this.mailstore = mailstore;
        this.fcpManager = fcpManager;
        messages = new LinkedList();
    }

    /**
     * Retrieves messages from each channel's message subspace.
     *
     * @exception ProtocolException if the slot retrieval fails (see
     *     fcp.SlotChecker).
     **/
    public void run() throws ProtocolException {
        Iterator channelIter = mailstore.getChannels().iterator();
        while (channelIter.hasNext()) {
            Channel channel = (Channel) channelIter.next();
            if (channel.getOutChannelEnabled()) {
                log.info("Searching for messages from " + channel.getContactAlias() + ".");
                SlotRetriever slotRetriever = fcpManager.slotRetrieve("freenet:SSK@" + channel.getContactPublicSSKKey() + "/" + channel.getInChannelName() + "/", channel.getInLastMessageIndex() + 1, true);
                List documents = slotRetriever.getDocuments();
                channel.setInLastMessageIndex(slotRetriever.getLastFilledSlot());
                Iterator docIter = documents.iterator();
                while (docIter.hasNext()) {
                    FCPDocument doc = (FCPDocument) docIter.next();
                    try {
                        EncryptedMessage encryptedMessage = new EncryptedMessage(doc.getData(), channel.getInSecretKey(), channel.getInCryptoParams());
                        messages.add(encryptedMessage.getMessage());
                    } catch (DocumentException e) {
                        e.printStackTrace();
                        log.fine(e.toString());
                    }
                }
            }
        }
    }

    /**
     * @return a List of the retrieved Messages.
     **/
    public List getMessages() {
        return messages;
    }

    /**
     * An FCP connection.
     **/
    protected FCPManager fcpManager;

    /**
     * The user's mailstore.
     **/
    protected Mailstore mailstore;

    /**
     * The retrieved messages.
     **/
    protected List messages;

    /**
     * The log object for status updates.
     **/
    protected Logger log;

    public void setLogger(Logger log) {
        this.log = log;
    }
}
