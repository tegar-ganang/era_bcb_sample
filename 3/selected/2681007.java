package net.sourceforge.simpleworklog.server.dao;

import org.apache.log4j.Logger;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Ignat Alexeyenko
 *         Date: 28.02.2010
 */
public final class CryptoUtils {

    private static final Logger logger = Logger.getLogger(HibernateAccountDao.class);

    private static final String MESSAGE_DIGEST_ALGORITHM_MD5 = "MD5";

    private CryptoUtils() {
    }

    public static String hashPassword(String password) {
        String hashword = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_MD5);
            md5.update(password.getBytes());
            BigInteger hash = new BigInteger(1, md5.digest());
            hashword = hash.toString(16);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Cannot find algorithm = '" + MESSAGE_DIGEST_ALGORITHM_MD5 + "'", e);
            throw new IllegalStateException(e);
        }
        return pad(hashword, 32, '0');
    }

    private static String pad(String s, int length, char pad) {
        StringBuffer buffer = new StringBuffer(s);
        while (buffer.length() < length) {
            buffer.insert(0, pad);
        }
        return buffer.toString();
    }
}
