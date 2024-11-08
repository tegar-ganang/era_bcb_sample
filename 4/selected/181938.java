package freemail.protocols.initiation;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.security.AlgorithmParameters;
import java.security.AlgorithmParameterGenerator;
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
import freemail.*;
import freemail.async.*;
import freemail.documents.*;
import freemail.protocols.*;
import freemail.util.*;

/**
 * The class responsible for creating channel requests for the fixed-window
 * dropbox.
 *
 * @author Adam Thomason
 **/
public class RequestGenerator implements CallbackRunnable {

    /**
     * Constructs a new RequestGenerator, which may be used asynchrnously
     * using the CallbackThread mechanism.
     *
     * @param requester the mailstore of user requesting the channel.
     * @param requestee the identity of the intended contact.
     * @param fcpManager an FCP connection.
     **/
    public RequestGenerator(Mailstore requester, Identity requestee, FCPManager fcpManager) {
        log = Logger.getLogger(this.getClass().getName());
        LogManager.getLogManager().addLogger(log);
        this.requester = requester;
        this.requestee = requestee;
        this.fcpManager = fcpManager;
    }

    /**
     * Generates a request for a channel between the requester and requestee.
     *
     * @exception ProtocolException if the request cannot be generated.
     **/
    public void run() throws ProtocolException {
        try {
            log.info("Generating matching Diffie-Hellman keypair.");
            KeyPair requesterDHKeyPair = generateMatchingKeypair(requestee.getRequesteeDHParamSpec());
            log.info("Performing Diffie-Hellman key agreeement.");
            SecretKey negotiationSecretKey = keyAgree(requesterDHKeyPair.getPrivate(), requestee.getRequesteePublicDHKey());
            log.info("Generating negotiation encryption parameters.");
            Cipher negotiationCipher = Cipher.getInstance(Constants.NEGOTIATION_TRANSFORMATION);
            negotiationCipher.init(Cipher.ENCRYPT_MODE, negotiationSecretKey);
            AlgorithmParameters negotiationCryptoParams = negotiationCipher.getParameters();
            log.info("Generating requester-to-requestee encryption key.");
            KeyGenerator kgen = KeyGenerator.getInstance(Constants.MESSAGING_CIPHER);
            kgen.init(Constants.MESSAGING_CIPHER_STRENGTH);
            SecretKey er2eeSecretKey = kgen.generateKey();
            Cipher er2eeCipher = Cipher.getInstance(Constants.MESSAGING_TRANSFORMATION);
            er2eeCipher.init(Cipher.ENCRYPT_MODE, er2eeSecretKey);
            AlgorithmParameters er2eeCryptoParams = er2eeCipher.getParameters();
            log.info("Generating requester-to-requestee channel name.");
            String er2eeChannelName = RandomString.generate(30);
            log.info("Generating receipt address.");
            String receiptAddress = "freenet:SSK@" + requestee.getPublicSSKKey() + "/" + RandomString.generate(30);
            encryptedRequest = new EncryptedRequest(requesterDHKeyPair.getPublic(), negotiationSecretKey, negotiationCryptoParams, requester.getAlias(), requester.getPublicSSKKey(), er2eeChannelName, er2eeSecretKey, er2eeCryptoParams, receiptAddress);
            requestedChannel = new Channel(requestee.getAlias(), requestee.getPublicSSKKey(), requester.getPrivateSSKKey(), requester.getPublicSSKKey());
            requestedChannel.setExpectingReceipt(true);
            requestedChannel.setReceiptAddress(receiptAddress);
            requestedChannel.setInChannelEnabled(false);
            requestedChannel.setOutChannelEnabled(false);
            requestedChannel.configureOutbound(er2eeChannelName, er2eeSecretKey, er2eeCryptoParams, 0);
            requestedChannel.setNegotiationSecretKey(negotiationSecretKey);
            requestedChannel.setNegotiationCryptoParams(negotiationCryptoParams);
            log.info("Channel request generated.");
        } catch (Exception e) {
            throw new ProtocolException(e);
        }
    }

    /**
     * Generates a Diffie-Hellman keypair using predetermined DH parameters.
     *
     * @param dhParamSpec preset DH parameters.
     *
     * @return a keypair based on the same parameters.
     *
     * @exception DocumentException if an error occurs.
     **/
    public static KeyPair generateMatchingKeypair(DHParameterSpec dhParamSpec) throws DocumentException {
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(Constants.KEY_AGREEMENT_PROTOCOL);
            keyPairGen.initialize(dhParamSpec);
            return keyPairGen.generateKeyPair();
        } catch (Exception e) {
            throw new DocumentException("DH matching key generation failed", e);
        }
    }

    /**
     * Performs two-person Diffie-Hellman key agreement and returns the shared
     * secret key.
     *
     * @param privateKey the first party's private key.
     * @param publicKey the other party's public key.
     *
     * @return the shared secret key.
     *
     * @exception DocumentException if an error occurs.
     **/
    public static SecretKey keyAgree(PrivateKey privateKey, PublicKey publicKey) throws DocumentException {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance(Constants.KEY_AGREEMENT_PROTOCOL);
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(publicKey, true);
            return keyAgreement.generateSecret(Constants.NEGOTIATION_CIPHER);
        } catch (Exception e) {
            throw new DocumentException("DH key agreement failed", e);
        }
    }

    /**
     * @return the request document to be inserted into Freenet.
     **/
    public EncryptedRequest getEncryptedRequest() {
        return encryptedRequest;
    }

    /**
     * @return a Channel object for the requested channel.
     **/
    public Channel getChannel() {
        return requestedChannel;
    }

    /**
     * @return the requester mailstore.
     **/
    public Mailstore getRequester() {
        return requester;
    }

    /**
     * @return the requestee identity.
     **/
    public Identity getRequestee() {
        return requestee;
    }

    /**
     * The requester's mailstore.
     **/
    protected Mailstore requester;

    /**
     * The requestee's identity.
     **/
    protected Identity requestee;

    /**
     * An FCP connection.
     **/
    protected FCPManager fcpManager;

    /**
     * The generated request.
     **/
    protected EncryptedRequest encryptedRequest;

    /**
     * The requested channel.
     **/
    protected Channel requestedChannel;

    /**
     * The log object for status updates.
     **/
    protected Logger log;

    public void setLogger(Logger log) {
        this.log = log;
    }
}
