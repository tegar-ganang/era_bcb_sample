package freemail.protocols.mail;

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.logging.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;
import freemail.*;
import freemail.documents.*;
import freemail.util.*;

/**
 * <p>The class encapsulating an encrypted message.  This class is
 * a cryptographic wrapper around the FreemailDocument-based Message.</p>
 *
 * @author Adam Thomason
 **/
public class EncryptedMessage {

    /**
     * Creates a new channel encrypted message object.
     *
     * @param channelSecretKey the symmetric key for the appropriate channel.
     * @param channelCryptoParams the symmetric parameters for
     *     <code>secretKey</code>.
     * @param message the message to wrap.
     **/
    public EncryptedMessage(SecretKey channelSecretKey, AlgorithmParameters channelCryptoParams, Message message) {
        log = Logger.getLogger(this.getClass().getName());
        LogManager.getLogManager().addLogger(log);
        this.channelSecretKey = channelSecretKey;
        this.channelCryptoParams = channelCryptoParams;
        this.message = message;
    }

    /**
     * Parses an encrypted message.
     *
     * @param encryptedMessage the encoded message bytes, as retrieved from
     *     Freenet.
     * @param channelSecretKey the channel symmetric key.
     * @param channelCryptoParams the channel symmetric parameters.
     **/
    public EncryptedMessage(byte[] encryptedMessage, SecretKey channelSecretKey, AlgorithmParameters channelCryptoParams) throws DocumentException {
        String serializedMessage = null;
        try {
            Cipher cipher = Cipher.getInstance(Constants.MESSAGING_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, channelSecretKey, channelCryptoParams);
            serializedMessage = new String(cipher.doFinal(encryptedMessage));
        } catch (Exception e) {
            throw new DocumentException("Message decryption error", e);
        }
        message = new Message(serializedMessage);
    }

    /**
     * @return a byte array containing the encrypted message document.
     **/
    public byte[] serialize() throws DocumentException {
        try {
            Cipher cipher = Cipher.getInstance(Constants.MESSAGING_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, channelSecretKey, channelCryptoParams);
            byte[] ciphertext = cipher.doFinal(message.serialize().getBytes());
            return ciphertext;
        } catch (Exception e) {
            throw new DocumentException("Encryption failed", e);
        }
    }

    public SecretKey getChannelSecretKey() {
        return channelSecretKey;
    }

    public AlgorithmParameters getChannelCryptoParams() {
        return channelCryptoParams;
    }

    public Message getMessage() {
        return message;
    }

    protected SecretKey channelSecretKey;

    protected AlgorithmParameters channelCryptoParams;

    /**
     * The embedded message document.
     **/
    protected Message message;

    protected Logger log;

    public void setLogger(Logger log) {
        this.log = log;
    }
}
