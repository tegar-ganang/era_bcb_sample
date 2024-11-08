package org.xmdl.ida.lib.util;

import java.security.MessageDigest;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * String Utility Class This is used to encode passwords programmatically
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 */
public final class StringUtil {

    private static final Log log = LogFactory.getLog(StringUtil.class);

    /**
     * Checkstyle rule: utility classes should not have public constructor
     */
    private StringUtil() {
    }

    /**
     * Encode a string using algorithm specified in web.xml and return the
     * resulting encrypted password. If exception, the plain credentials
     * string is returned
     *
     * @param password Password or other credentials to use in authenticating
     *        this username
     * @param algorithm Algorithm used to do the digest
     *
     * @return encypted password based on the algorithm.
     */
    public static String encodePassword(String password, String algorithm) {
        byte[] unencodedPassword = password.getBytes();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            log.error("Exception: " + e);
            return password;
        }
        md.reset();
        md.update(unencodedPassword);
        byte[] encodedPassword = md.digest();
        StringBuffer buf = new StringBuffer();
        for (byte anEncodedPassword : encodedPassword) {
            if ((anEncodedPassword & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toString(anEncodedPassword & 0xff, 16));
        }
        return buf.toString();
    }

    /**
     * Encode a string using Base64 encoding. Used when storing passwords
     * as cookies.
     *
     * This is weak encoding in that anyone can use the decodeString
     * routine to reverse the encoding.
     *
     * @param str the string to encode
     * @return the encoded string
     */
    public static String encodeString(String str) {
        Base64 encoder = new Base64();
        return String.valueOf(encoder.encode(str.getBytes())).trim();
    }

    /**
     * Decode a string using Base64 encoding.
     *
     * @param str the string to decode
     * @return the decoded string
     */
    public static String decodeString(String str) {
        Base64 dec = new Base64();
        try {
            return String.valueOf(dec.decode(str));
        } catch (DecoderException de) {
            throw new RuntimeException(de.getMessage(), de.getCause());
        }
    }

    /**
     * @param str string to check
     * @return true if the given string is null or empty
     */
    public static boolean isEmpty(String str) {
        return str == null || "".equals(str);
    }
}
