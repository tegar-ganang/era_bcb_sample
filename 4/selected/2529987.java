package org.wat.wcy.isi.mmazur.bp.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;

public class MainClass {

    public static void main(String[] args) throws Exception {
        createFile();
        File aFile = new File("C:/primes.bin");
        FileInputStream inFile = new FileInputStream(aFile);
        FileChannel inChannel = inFile.getChannel();
        final int PRIMESREQUIRED = 10;
        ByteBuffer buf = ByteBuffer.allocate(8 * PRIMESREQUIRED);
        long[] primes = new long[PRIMESREQUIRED];
        int index = 0;
        final int PRIMECOUNT = (int) inChannel.size() / 8;
        for (int i = 0; i < PRIMESREQUIRED; i++) {
            index = 8 * (int) (PRIMECOUNT * Math.random());
            inChannel.read(buf, index);
            buf.flip();
            primes[i] = buf.getLong();
            buf.clear();
        }
        for (long prime : primes) {
            System.out.printf("%12d", prime);
        }
        inFile.close();
    }

    private static void createFile() throws Exception {
        long[] primes = new long[] { 1, 2, 3, 5, 7 };
        File aFile = new File("C:/primes.bin");
        FileOutputStream outputFile = new FileOutputStream(aFile);
        FileChannel file = outputFile.getChannel();
        final int BUFFERSIZE = 100;
        ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);
        LongBuffer longBuf = buf.asLongBuffer();
        int primesWritten = 0;
        while (primesWritten < primes.length) {
            longBuf.put(primes, primesWritten, min(longBuf.capacity(), primes.length - primesWritten));
            buf.limit(8 * longBuf.position());
            file.write(buf);
            primesWritten += longBuf.position();
            longBuf.clear();
            buf.clear();
        }
        System.out.println("File written is " + file.size() + "bytes.");
        outputFile.close();
    }

    private static int min(int a, int b) {
        if (a > b) {
            return b;
        } else {
            return a;
        }
    }
}
