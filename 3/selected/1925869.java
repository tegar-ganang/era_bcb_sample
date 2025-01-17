package net.exclaimindustries.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * <code>MD5Tools</code> consists of a helper method for the common gruntwork
 * tasks commonly associated with MD5 hashing.  Most common of these would be
 * the hashing of a simple string.
 *
 * @author Nicholas Killewald
 */
public class MD5Tools {

    /**
     * Hashes a string through the MD5 algorithm.  If something goes wrong with
     * getting an MD5 instance, this returns an empty string.
     *
     * @param input String object to hash
     * @return the MD5 hash of the input
     */
    public static String MD5hash(String input) {
        MessageDigest diggy;
        try {
            diggy = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
        diggy.update(CharToByte.charsToBytes(input.toCharArray()));
        return CharToByte.bytesToString(diggy.digest());
    }
}
