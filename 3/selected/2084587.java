package org.icenigrid.gridsam.core.plugin.transfer.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <strong>Purpose:</strong><br>
 * This Class is used to generate the unique jobName of the different AftJob
 * 
 * @author long xinzheng , Haojie Zhou create 2008-9-4
 * 
 * 
 */
public final class HashFunction {

    /**
	 * the log object.
	 */
    private static final Log LOG = LogFactory.getLog(HashFunction.class);

    /**
	 * Singleton instance of this class.
	 */
    private static HashFunction hashFunction = null;

    /**
	 * The random object to get random number.
	 */
    private static Random rand = new Random(System.currentTimeMillis());

    /**
	 * Returns the singleton instance of this class.
	 * 
	 * @return Singleton instance of HashFunction.
	 */
    private static HashFunction getHashFunction() {
        if (hashFunction == null) {
            try {
                hashFunction = new HashFunction(MessageDigest.getInstance("SHA-1"));
            } catch (NoSuchAlgorithmException e) {
                LOG.error("Get MessageDigest failed. " + e.getMessage());
                throw new RuntimeException("No hash function available!", e);
            }
        }
        return hashFunction;
    }

    /**
	 * Message digest for calculating hash values.
	 */
    private static MessageDigest messageDigest;

    static {
        getHashFunction();
    }

    /**
	 * Constructor is hidden and invoked once by {@link #getHashFunction()}.
	 * 
	 * @param digest1
	 *            Message digest for calculating hash values.
	 */
    private HashFunction(MessageDigest digest1) {
        if (digest1 == null) {
            throw new NullPointerException("Parameter may not be null!");
        }
        messageDigest = digest1;
    }

    /**
	 * Returns the length of IDs generated by this hash function.
	 * 
	 * @return Number of bytes of generated IDs.
	 */
    public static int getLengthOfIDsInBytes() {
        return messageDigest.getDigestLength();
    }

    /**
	 * Calculates the hash value for a given data Key.
	 * 
	 * @param entry
	 *            the given data key.
	 * @return ID for the given Key.
	 */
    private static String getHashKey(String entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Parameter entry must not be null!");
        }
        if (entry.getBytes() == null || entry.getBytes().length == 0) {
            throw new IllegalArgumentException("Byte representation of Parameter " + " must not be null or have length 0!");
        }
        byte[] testBytes = entry.getBytes();
        synchronized (messageDigest) {
            messageDigest.reset();
            messageDigest.update(testBytes);
            byte[] id = messageDigest.digest();
            return toHexString(id, getLengthOfIDsInBytes());
        }
    }

    /**
	 * convert id to HexString.
	 * 
	 * @param id
	 * @param numberOfBytes
	 * @return a Hex String.
	 */
    private static String toHexString(byte[] id, int numberOfBytes) {
        final int max_byte = 0xff;
        int displayBytes = Math.max(1, Math.min(numberOfBytes, id.length));
        String result = "";
        for (int i = 0; i < displayBytes; i++) {
            String block = Integer.toHexString(id[i] & max_byte).toUpperCase();
            if (block.length() < 2) {
                block = "0" + block;
            }
            result += block;
        }
        return result.toString();
    }

    /**
	 * Create a global unique ID which is generated by system.
	 * 
	 * @return the generated ID.
	 */
    public static String createJobName() {
        String key = "";
        try {
            key = InetAddress.getLocalHost().getHostAddress();
            key += " / " + InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.error("Get local host's ip address or hostname failed. " + e.getMessage());
            key = "127.0.0.1 / localhost";
        }
        long time = System.currentTimeMillis();
        key += " / " + time;
        key += " / " + rand.nextInt();
        key += " / " + rand.nextInt();
        key += " / " + rand.nextInt();
        if (LOG.isDebugEnabled()) {
            LOG.debug("generated key is " + key);
        }
        return getHashKey(key);
    }

    /**
	 * Whether the two object is equal?
	 * 
	 * @param obj1
	 *            the first object.
	 * @param obj2
	 *            the second object.
	 * @return true if is equal.
	 */
    public static boolean isObjEqual(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) return true;
        if (obj1 != null) {
            return obj1.equals(obj2);
        } else {
            return obj2.equals(obj1);
        }
    }
}