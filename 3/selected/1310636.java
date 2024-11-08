package com.luxx.acms.gwt.server.utils;

import org.springframework.dao.DataAccessException;
import org.springframework.security.providers.encoding.PasswordEncoder;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Random;

public class Joomla15PasswordHash implements PasswordEncoder {

    public static boolean check(String passwd, String dbEntry) {
        if (passwd == null || dbEntry == null || dbEntry.length() == 0) throw new IllegalArgumentException();
        String[] arr = dbEntry.split(":", 2);
        if (arr.length == 2) {
            String cryptpass = arr[0];
            String salt = arr[1];
            return md5(passwd + salt).equals(cryptpass);
        } else {
            String cryptpass = dbEntry;
            return md5(passwd).equals(cryptpass);
        }
    }

    static Random _rnd;

    public static String create(String passwd) {
        StringBuffer saltBuf = new StringBuffer();
        synchronized (Joomla15PasswordHash.class) {
            if (_rnd == null) _rnd = new SecureRandom();
            int i;
            for (i = 0; i < 32; i++) {
                saltBuf.append(Integer.toString(_rnd.nextInt(36), 36));
            }
        }
        String salt = saltBuf.toString();
        return md5(passwd + salt) + ":" + salt;
    }

    /**
     * Takes the MD5 hash of a sequence of ASCII or LATIN1 characters,
     * and returns it as a 32-character lowercase hex string.
     * <p/>
     * Equivalent to MySQL's MD5() function
     * and to perl's Digest::MD5::md5_hex(),
     * and to PHP's md5().
     * <p/>
     * Does no error-checking of the input,  but only uses the low 8 bits
     * from each input character.
     */
    private static String md5(String data) {
        byte[] bdata = new byte[data.length()];
        int i;
        byte[] hash;
        for (i = 0; i < data.length(); i++) bdata[i] = (byte) (data.charAt(i) & 0xff);
        try {
            MessageDigest md5er = MessageDigest.getInstance("MD5");
            hash = md5er.digest(bdata);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        StringBuffer r = new StringBuffer(32);
        for (i = 0; i < hash.length; i++) {
            String x = Integer.toHexString(hash[i] & 0xff);
            if (x.length() < 2) r.append("0");
            r.append(x);
        }
        return r.toString();
    }

    public String encodePassword(String rawPass, Object salt) throws DataAccessException {
        return create(rawPass);
    }

    public boolean isPasswordValid(String encPass, String rawPass, Object salt) throws DataAccessException {
        return check(rawPass, encPass);
    }
}
