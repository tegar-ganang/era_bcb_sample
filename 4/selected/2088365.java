package com.qarks.util;

import java.io.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import com.qarks.util.stream.*;

public class CryptoHelper {

    static byte[] salt = { (byte) 0xA1, (byte) 0x3B, (byte) 0xF8, (byte) 0x14, (byte) 0x54, (byte) 0xFE, (byte) 0x12, (byte) 0xAA };

    public static String encrypt(String pass, byte[] plainText) throws Exception {
        int iterationCount = 19;
        KeySpec keySpec = new PBEKeySpec(pass.toCharArray(), salt, iterationCount);
        SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
        Cipher ecipher = Cipher.getInstance(key.getAlgorithm());
        AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);
        ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
        byte[] enc = ecipher.doFinal(plainText);
        ByteArrayOutputStream byteArrayOutpuStream = new ByteArrayOutputStream();
        Base64OutputStream encoder = new Base64OutputStream(byteArrayOutpuStream, true);
        encoder.write(enc);
        encoder.flush();
        encoder.close();
        return byteArrayOutpuStream.toString();
    }

    public static byte[] decrypt(String pass, String str) throws Exception {
        int iterationCount = 19;
        KeySpec keySpec = new PBEKeySpec(pass.toCharArray(), salt, iterationCount);
        SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
        Cipher dcipher = Cipher.getInstance(key.getAlgorithm());
        AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);
        dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(str.getBytes("UTF8"));
        Base64InputStream decoder = new Base64InputStream(byteArrayInputStream);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte array[] = new byte[2048];
        int nbread = 0;
        while ((nbread = decoder.read(array)) > -1) {
            baos.write(array, 0, nbread);
        }
        byte encoded[] = baos.toByteArray();
        byte decoded[] = dcipher.doFinal(encoded);
        return decoded;
    }
}
