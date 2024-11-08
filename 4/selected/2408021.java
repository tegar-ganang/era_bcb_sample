package de.helwich.mpo;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

/**
 * @author Hendrik Helwich
 */
class Util {

    private Util() {
    }

    static void copy(InputStream fis, OutputStream fos) throws IOException {
        copy(fis, fos, 0xffff);
    }

    static void copy(InputStream fis, OutputStream fos, int bufferSize) throws IOException {
        byte buffer[] = new byte[bufferSize];
        int nbytes;
        while ((nbytes = fis.read(buffer)) != -1) fos.write(buffer, 0, nbytes);
    }

    private static Random random = new Random();

    static void assertEquals_(InputStream prefix, InputStream in2) throws IOException {
        final int LENGTH = 200;
        byte buffer[] = new byte[LENGTH * 2 - 2];
        byte buffer2[] = new byte[LENGTH * 2 - 2];
        while (true) {
            int off = random.nextInt(LENGTH);
            int len = random.nextInt(LENGTH);
            int len2 = prefix.read(buffer, off, len);
            if (len2 == -1) {
                return;
            }
            int len3;
            for (int remaining = len2; remaining > 0; remaining -= len3) {
                len3 = in2.read(buffer2, off, remaining);
                assertTrue(len3 >= 0);
                for (int i = off; i < off + len3; i++) assertEquals(buffer[i] & 0xFF, buffer2[i] & 0xFF);
                off += len3;
            }
        }
    }

    static void assertStreamFinished(InputStream in) throws IOException {
        assertTrue(in.read() == -1);
    }
}
