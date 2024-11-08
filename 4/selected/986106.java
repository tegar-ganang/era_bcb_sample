package org.pfyshnet.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class StreamUtils {

    public static int MAXBYTES = 4096;

    public static long MAXFILE = 50L * 1024L * 1024L;

    /**
	 * In theory FileInputStream doesn't have to fill the buffer
	 * even if there is data in the file left.  We have this in case
	 * of really slow IO.
	 * @param fis
	 * @param buffer
	 * @return
	 * @throws IOException
	 */
    public static int fillBuffer(FileInputStream fis, byte[] buffer) throws IOException {
        int rlen = 0;
        int len = fis.read(buffer);
        while (len != -1 && rlen != buffer.length) {
            rlen += len;
            len = fis.read(buffer, rlen, buffer.length - rlen);
        }
        return rlen;
    }

    /**
	 * In theory FileInputStream doesn't have to fill the buffer
	 * even if there is data in the file left.  We have this in case
	 * of really slow IO.
	 * @param fis
	 * @param buffer
	 * @return
	 * @throws IOException
	 */
    public static int fillBuffer(DataInputStream fis, byte[] buffer) throws IOException {
        int rlen = 0;
        int len = fis.read(buffer);
        while (len != -1 && rlen != buffer.length) {
            rlen += len;
            len = fis.read(buffer, rlen, buffer.length - rlen);
        }
        return rlen;
    }

    public static byte[] readBytes(DataInputStream dis) throws IOException {
        int len = dis.readInt();
        if (len > MAXBYTES) {
            throw new IOException("Too large buffer for byte reads. " + len);
        }
        byte[] b = new byte[len];
        fillBuffer(dis, b);
        return b;
    }

    public static void writeBytes(DataOutputStream dos, byte[] b) throws IOException {
        dos.writeInt(b.length);
        dos.write(b);
    }

    public static String readString(DataInputStream dis) throws IOException {
        return new String(readBytes(dis), "ISO-8859-1");
    }

    public static void writeString(DataOutputStream dos, String str) throws IOException {
        writeBytes(dos, str.getBytes("ISO-8859-1"));
    }

    public static void readFile(DataInputStream dis, File outfile) throws IOException {
        long len = dis.readLong();
        if (len > MAXFILE) {
            throw new IOException("Too large file! " + MAXFILE);
        }
        FileOutputStream fos = new FileOutputStream(outfile);
        byte[] buffer = new byte[128];
        int readlen = 0;
        while (len > 0) {
            readlen = dis.read(buffer, 0, (int) Math.min(len, buffer.length));
            if (readlen > 0) {
                fos.write(buffer, 0, readlen);
                len -= readlen;
            }
        }
        fos.close();
    }

    public static void writeFile(DataOutputStream dos, File infile) throws IOException {
        if (infile.length() > MAXFILE) {
            throw new IOException("File too large to write! " + MAXFILE);
        }
        dos.writeLong(infile.length());
        writeRawFile(dos, infile);
    }

    public static void writeRawFile(DataOutputStream dos, File infile) throws IOException {
        if (infile.length() > MAXFILE) {
            throw new IOException("File too large to write!");
        }
        byte[] buffer = new byte[128];
        int readlen = 0;
        FileInputStream fis = new FileInputStream(infile);
        readlen = fis.read(buffer);
        while (readlen >= 0) {
            if (readlen > 0) {
                dos.write(buffer, 0, readlen);
            }
            readlen = fis.read(buffer);
        }
        fis.close();
    }

    public static void readRawFile(DataInputStream dis, File outfile) throws IOException {
        FileOutputStream fos = new FileOutputStream(outfile);
        byte[] buffer = new byte[128];
        int len = dis.read(buffer);
        while (len >= 0) {
            if (len > 0) {
                fos.write(buffer, 0, len);
            }
            len = dis.read(buffer);
        }
        fos.close();
    }
}
