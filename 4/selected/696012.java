package entagged.audioformats.ape.util;

import entagged.audioformats.*;
import entagged.audioformats.exceptions.*;
import entagged.audioformats.generic.Utils;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class ApeTagWriter {

    private ApeTagCreator tc = new ApeTagCreator();

    public void delete(RandomAccessFile raf) throws IOException {
        if (!tagExists(raf)) return;
        raf.seek(raf.length() - 20);
        byte[] b = new byte[4];
        raf.read(b);
        long tagSize = Utils.getLongNumber(b, 0, 3);
        raf.setLength(raf.length() - tagSize);
        if (!tagExists(raf)) return;
        raf.setLength(raf.length() - 32);
    }

    private boolean tagExists(RandomAccessFile raf) throws IOException {
        raf.seek(raf.length() - 32);
        byte[] b = new byte[8];
        raf.read(b);
        return new String(b).equals("APETAGEX");
    }

    public void write(Tag tag, RandomAccessFile raf, RandomAccessFile rafTemp) throws CannotWriteException, IOException {
        FileChannel fc = raf.getChannel();
        ByteBuffer tagBuffer = tc.convert(tag, 0);
        if (!tagExists(raf)) {
            fc.position(fc.size());
            fc.write(tagBuffer);
        } else {
            raf.seek(raf.length() - 32 + 8);
            byte[] b = new byte[4];
            raf.read(b);
            int version = Utils.getNumber(b, 0, 3);
            if (version != 2000) {
                throw new CannotWriteException("APE Tag other than version 2.0 are not supported");
            }
            b = new byte[4];
            raf.read(b);
            long oldSize = Utils.getLongNumber(b, 0, 3) + 32;
            int tagSize = tagBuffer.capacity();
            if (oldSize <= tagSize) {
                System.err.println("Overwriting old tag in mpc file");
                fc.position(fc.size() - oldSize);
                fc.write(tagBuffer);
            } else {
                System.err.println("Shrinking mpc file");
                FileChannel tempFC = rafTemp.getChannel();
                tempFC.position(0);
                fc.position(0);
                tempFC.transferFrom(fc, 0, fc.size() - oldSize);
                tempFC.position(tempFC.size());
                tempFC.write(tagBuffer);
            }
        }
    }
}
