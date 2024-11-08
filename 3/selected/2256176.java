package org.xmlsh.commands.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.xmlsh.core.CoreException;

/**
 * @author David A. Lee
 */
public class Checksum {

    private String mMD5;

    private long mLength;

    private Checksum(String md5, long len) {
        mMD5 = md5;
        mLength = len;
    }

    public static String toHexString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            int b = (int) bytes[i] & 0xFF;
            String hex = Integer.toString(b, 16);
            if (hex.length() < 2) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }

    private static MessageDigest getMD5Digest() throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        return digest;
    }

    public static Checksum calcChecksum(InputStream in) throws CoreException, IOException {
        return calcChecksum(in, null);
    }

    /**
	 * @param file
	 * @return
	 * @throws CoreException 
	 * @throws IOException 
	 */
    public static Checksum calcChecksum(InputStream in, OutputStream out) throws CoreException, IOException {
        long totlen = 0;
        try {
            final int kBUF_SIZE = 1024 * 4;
            MessageDigest digest = getMD5Digest();
            if (digest == null) return null;
            byte buf[] = new byte[kBUF_SIZE];
            int len;
            while ((len = in.read(buf)) > 0) {
                digest.update(buf, 0, len);
                totlen += len;
                if (out != null) out.write(buf, 0, len);
            }
            return new Checksum(toHexString(digest.digest()), totlen);
        } catch (NoSuchAlgorithmException e) {
            throw new CoreException("Exception getting MD5 algorithm");
        }
    }

    /**
	 * @param file
	 * @return
	 */
    public static Checksum calcChecksum(File file) throws CoreException, IOException {
        InputStream in = new FileInputStream(file);
        Checksum checksum = calcChecksum(in);
        in.close();
        return checksum;
    }

    /**
	 * @param file
	 * @return
	 */
    public static Checksum calcChecksum(String data) throws CoreException, IOException {
        return calcChecksum(data.getBytes());
    }

    /**
     * @param file
     * @return
     */
    public static Checksum calcChecksum(byte[] data) throws CoreException, IOException {
        InputStream in = new ByteArrayInputStream(data);
        Checksum checksum = calcChecksum(in);
        in.close();
        return checksum;
    }

    /**
	 * @return the mD5
	 */
    public String getMD5() {
        return mMD5;
    }

    /**
	 * @return the length
	 */
    public long getLength() {
        return mLength;
    }
}
