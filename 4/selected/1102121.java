package Watermill.util.codes;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidParameterException;
import Watermill.interfaces.Fingerprint;

/**
 * @author Julien Lafaye
 *
 */
public class FileInputStreamFingerprint implements Fingerprint {

    private FileInputStream fis;

    private byte[] buffer;

    private int length = Integer.MAX_VALUE;

    private static final int SIZEOFBYTE = 8;

    public FileInputStreamFingerprint(String name) {
        try {
            this.fis = new FileInputStream(name);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        loadFile();
    }

    private void loadFile() {
        try {
            long size = fis.getChannel().size();
            if (size > Integer.MAX_VALUE) {
                throw new InvalidParameterException("Cannot handle file: too big !");
            }
            this.length = (int) size;
            this.buffer = new byte[(int) size];
            int cread = 0;
            cread += fis.read(buffer);
            while (cread < size) {
                cread += fis.read(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean getBit(int pos) {
        int bpos = pos / SIZEOFBYTE;
        int cpos = pos % SIZEOFBYTE;
        int mask = 1 << cpos;
        return (buffer[bpos] & mask) > 0;
    }

    public int getLength() {
        return this.length;
    }

    public void setBit(int pos, boolean b) {
        throw new UnsupportedOperationException("setBit unavailable yet");
    }

    public static void main(String args) {
    }
}
