package se.unlogic.standardutils.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import se.unlogic.standardutils.operation.ProgressMeter;
import se.unlogic.standardutils.streams.StreamUtils;

public class EncryptionUtils {

    public static String hash(String string, String algorithm) throws UnsupportedEncodingException {
        return hash(string, algorithm, Charset.defaultCharset().toString());
    }

    public static String hash(String string, String algorithm, String encoding) throws UnsupportedEncodingException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            digest.update(string.getBytes(encoding));
            byte[] encodedPassword = digest.digest();
            return new BigInteger(1, encodedPassword).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String hash(File file, String algorithm) throws IOException {
        return hash(file, algorithm, null);
    }

    public static String hash(File file, String algorithm, ProgressMeter progressMeter) throws IOException {
        FileInputStream fileInputStream = null;
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            fileInputStream = new FileInputStream(file);
            DigestInputStream digestInputStream = new DigestInputStream(fileInputStream, digest);
            if (progressMeter != null) {
                progressMeter.setStartTime();
                progressMeter.setFinish(file.length());
            }
            byte[] buffer = new byte[8192];
            int bytesRead = 1;
            if (progressMeter != null) {
                while ((bytesRead = digestInputStream.read(buffer)) != -1) {
                    progressMeter.incrementCurrentPosition(bytesRead);
                }
            } else {
                while ((bytesRead = digestInputStream.read(buffer)) != -1) {
                }
            }
            byte[] hash = digest.digest();
            if (progressMeter != null) {
                progressMeter.setEndTime();
            }
            return new BigInteger(1, hash).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } finally {
            StreamUtils.closeStream(fileInputStream);
        }
    }

    public static String mysqlPasswordHash(String string) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HashAlgorithms.SHA1);
            try {
                digest.update(string.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            byte[] encodedPassword = digest.digest();
            digest.update(encodedPassword);
            encodedPassword = digest.digest();
            String hash = new BigInteger(1, encodedPassword).toString(16).toUpperCase();
            while (hash.length() < 40) {
                hash = "0" + hash;
            }
            return "*" + hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
