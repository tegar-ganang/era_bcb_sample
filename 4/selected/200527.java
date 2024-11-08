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
 * The class responsible for retrieving channel requests receipts from Freenet.
 *
 * @author Adam Thomason
 **/
public class ReceiptRetriever implements CallbackRunnable {

    /**
     * Constructs a new ReceiptRetriever, which may be used asynchronously
     * using the CallbackThread mechanism.
     *
     * @param mailstore the mailstore of the user.
     * @param fcpManager a connection to a Freenet node.
     **/
    public ReceiptRetriever(Mailstore mailstore, FCPManager fcpManager) {
        log = Logger.getLogger(this.getClass().getName());
        LogManager.getLogManager().addLogger(log);
        this.mailstore = mailstore;
        this.fcpManager = fcpManager;
        receipts = new LinkedList();
    }

    /**
     * Retrieves request receipts listed with the channels in a mailstore.
     * If a receipt is found, its channel is updated accordingly.
     *
     * @exception ProtocolException if receipt retrieval fails.
     **/
    public void run() throws ProtocolException {
        Iterator channelIter = mailstore.getChannels().iterator();
        while (channelIter.hasNext()) {
            Channel channel = (Channel) channelIter.next();
            if (channel.getExpectingReceipt()) {
                log.info("Searching for receipt " + channel.getReceiptAddress());
                FCPDocument doc = new FCPDocument(channel.getReceiptAddress());
                FCPRequester request = fcpManager.request(doc);
                if (request.getSuccess()) {
                    if (request.getDataFound()) {
                        log.fine("Receipt found.");
                        try {
                            EncryptedReceipt encryptedReceipt = new EncryptedReceipt(doc.getDataString(), channel.getNegotiationSecretKey(), channel.getNegotiationCryptoParams());
                            Receipt receipt = encryptedReceipt.getReceipt();
                            channel.setExpectingReceipt(false);
                            channel.setInChannelEnabled(true);
                            channel.configureInbound(receipt.getEe2erChannelName(), receipt.getEe2erSecretKey(), receipt.getEe2erCryptoParams(), 0);
                            channel.setOutChannelEnabled(true);
                            receipts.add(receipt);
                        } catch (DocumentException e) {
                            e.printStackTrace();
                            log.fine(e.toString());
                        }
                    } else {
                        log.fine("Receipt not found.");
                    }
                }
            }
        }
    }

    /**
     * @return a List of the retrieved channel Receipts.
     **/
    public List getReceipts() {
        return receipts;
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
     * The retrieved channel request receipts.
     **/
    protected List receipts;

    /**
     * The log object for status updates.
     **/
    protected Logger log;

    public void setLogger(Logger log) {
        this.log = log;
    }
}
