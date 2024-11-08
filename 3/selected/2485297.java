package com.conflux.mifos.live.common;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * This class encapsulate all the logic related to password hashing
 */
public class EncryptionUtils {

    MessageDigest messageDigest = null;

    /**
     * This function will return the hashed password out of the passed string
     * password
     *
     * @param password    password passed by the user
     * @param randomBytes random bytes
     */
    public static byte[] getHashedPassword(String password, byte[] randomBytes) {
        byte[] hashedPassword = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(randomBytes);
            messageDigest.update(password.getBytes("UTF-8"));
            hashedPassword = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return hashedPassword;
    }

    /**
     * This function verifies a given password
     */
    public static boolean verifyPassword(String password, byte[] encPassword) {
        byte[] randomBytes = new byte[12];
        byte[] decPassword = null;
        System.arraycopy(encPassword, 0, randomBytes, 0, randomBytes.length);
        byte[] decTempPassword = getHashedPassword(password, randomBytes);
        decPassword = new byte[randomBytes.length + decTempPassword.length];
        System.arraycopy(randomBytes, 0, decPassword, 0, randomBytes.length);
        System.arraycopy(decTempPassword, 0, decPassword, randomBytes.length, decTempPassword.length);
        return compare(encPassword, decPassword);
    }

    /**
     * This function create the hashed password
     */
    public byte[] createEncryptedPassword(String password) {
        byte[] randomBytes = generateRandomBytes();
        byte[] encPassword = null;
        byte[] tempEncPassword = getHashedPassword(password, randomBytes);
        encPassword = new byte[randomBytes.length + tempEncPassword.length];
        System.arraycopy(randomBytes, 0, encPassword, 0, randomBytes.length);
        System.arraycopy(tempEncPassword, 0, encPassword, randomBytes.length, tempEncPassword.length);
        return encPassword;
    }

    /**
     * Hepler function which compare two hashed password
     *
     * @param encPassword
     * @param decPassword
     * @return
     */
    public static boolean compare(byte[] encPassword, byte[] decPassword) {
        if (Arrays.equals(encPassword, decPassword)) return true; else return false;
    }

    /**
     * This function generate and returns the random no of bytes
     *
     * @return
     */
    public byte[] generateRandomBytes() {
        byte[] randomBytes = new byte[12];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(randomBytes);
        return randomBytes;
    }
}
