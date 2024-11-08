package net.sf.autoshare.data.remote;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.sf.autoshare.data.DataConstants;

public class DataInterface {

    private static final int STREAM_BUF_SIZE = 8 * 1024;

    public static void sendNext(byte[] bytes, DataOutputStream out) throws IOException {
        out.writeLong(bytes.length);
        out.write(bytes);
    }

    public static byte[] receiveNext(DataInputStream in, long maxLength) throws IOException, MaxLengthExceededException {
        long length = in.readLong();
        if (length > maxLength) {
            throw new MaxLengthExceededException("max length (" + maxLength + ") " + "exceeded, requested length: " + length);
        }
        byte[] bytes = new byte[(int) length];
        in.readFully(bytes);
        return bytes;
    }

    public static void sendNextString(String str, DataOutputStream out) throws IOException {
        sendNext(str.getBytes(DataConstants.TEXT_ENCODING), out);
    }

    public static String receiveNextString(DataInputStream in, long maxLength) throws IOException, MaxLengthExceededException {
        byte[] bytes = receiveNext(in, maxLength);
        if (bytes == null) {
            return null;
        }
        return new String(bytes, DataConstants.TEXT_ENCODING);
    }

    public static void streamOut(DataOutputStream out, InputStream source, long length) throws IOException {
        out.writeLong(length);
        byte[] buf = new byte[STREAM_BUF_SIZE];
        int readLength;
        while ((readLength = source.read(buf)) > 0) {
            out.write(buf, 0, readLength);
        }
    }

    public static void streamIn(DataInputStream in, OutputStream dest, long maxLength) throws MaxLengthExceededException, IOException {
        long length = in.readLong();
        if (maxLength > 0 && length > maxLength) {
            throw new MaxLengthExceededException("max length (" + maxLength + ") " + "exceeded, requested length: " + length);
        }
        byte[] buf = new byte[STREAM_BUF_SIZE];
        long offset = 0;
        while (true) {
            long left = maxLength - offset;
            int readLength;
            if (left >= buf.length) {
                readLength = in.read(buf);
            } else {
                readLength = in.read(buf, 0, (int) left);
            }
            if (readLength <= 0) {
                break;
            }
            dest.write(buf, 0, readLength);
            offset += readLength;
            if (offset >= maxLength) {
                break;
            }
        }
    }

    public static void streamFile(DataOutputStream out, File file) throws IOException {
        long size = file.length();
        if (size == 0) {
            throw new IOException("can't determine file size");
        }
        InputStream in = new FileInputStream(file);
        try {
            streamOut(out, in, size);
        } finally {
            in.close();
        }
    }

    public static void readFile(DataInputStream in, File file, boolean shouldNotExist, int maxLength) throws IOException, FileAlreadyExistsException, MaxLengthExceededException {
        if (!file.createNewFile()) {
            throw new FileAlreadyExistsException("file already exists: " + file.getAbsolutePath());
        }
        OutputStream out = new FileOutputStream(file);
        try {
            streamIn(in, out, maxLength);
        } finally {
            out.close();
        }
    }
}
