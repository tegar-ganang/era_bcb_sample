package org.szegedi.nbpipe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Random;
import junit.framework.TestCase;

/**
 * The JUnit test suite for testing NonblockingPipe functionality.
 * @version $Id: TestNonblockingPipe.java,v 1.2 2002/02/11 08:18:30 szegedia Exp $
 * @author Attila Szegedi, szegedia at freemail dot hu
 */
public class TestNonblockingPipe extends TestCase {

    private static final int MEMORY_POOL_SIZE = 1024 * 1024;

    private static final int FILE_POOL_SIZE = 16 * 1024 * 1024;

    private static final int DATALEN = 1024 * 1024 - 1;

    private static ByteBufferPool pool;

    private NonblockingPipe pipe;

    private byte[] data1;

    private ChannelWriteListenerImpl listener;

    public TestNonblockingPipe(String name) {
        super(name);
    }

    public void setUp() {
        if (pool == null) {
            try {
                File file = File.createTempFile("testNonBlockingPipe", "tmp");
                file.deleteOnExit();
                pool = new GlobalByteBufferPool(MEMORY_POOL_SIZE, FILE_POOL_SIZE, file);
            } catch (IOException e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
                fail(sw.toString());
            }
        }
        pipe = new NonblockingPipe(pool);
        data1 = new byte[DATALEN];
        Random random = new Random();
        random.nextBytes(data1);
        listener = new ChannelWriteListenerImpl();
    }

    public void testSequentialStreamToStreamTransfer() throws IOException {
        OutputStream out = pipe.getOutputStream();
        out.write(data1);
        out.close();
        InputStream in = pipe.getInputStream();
        byte[] data2 = new byte[DATALEN];
        in.read(data2);
        assertTrue(Arrays.equals(data1, data2));
        in.close();
    }

    public void testConcurrentStreamToStreamTransfer() throws IOException {
        final Object signal = new Object();
        final byte[] data2 = new byte[DATALEN];
        final IOException[] ioe = new IOException[1];
        Thread t1 = new Thread(new Runnable() {

            public void run() {
                try {
                    writeToStreamInChunks();
                } catch (IOException e) {
                    ioe[0] = e;
                }
            }
        });
        Thread t2 = new Thread(new Runnable() {

            public void run() {
                try {
                    readFromStreamInChunks(data2);
                } catch (IOException e) {
                    ioe[0] = e;
                }
                synchronized (signal) {
                    signal.notify();
                }
            }
        });
        synchronized (signal) {
            t2.start();
            t1.start();
            try {
                signal.wait(10000);
            } catch (InterruptedException e) {
            }
        }
        if (ioe[0] != null) {
            throw ioe[0];
        }
        assertTrue(Arrays.equals(data1, data2));
    }

    private void writeToStreamInChunks() throws IOException {
        OutputStream out = pipe.getOutputStream();
        int ofs = 0;
        int blocksize = 65531;
        while (ofs < DATALEN) {
            out.write(data1, ofs, blocksize);
            ofs += blocksize;
            blocksize = Math.min(blocksize, DATALEN - ofs);
        }
        out.close();
    }

    private void readFromStreamInChunks(byte[] data2) throws IOException {
        InputStream in = pipe.getInputStream();
        int ofs = 0;
        int blocksize = 65533;
        while (ofs < DATALEN) {
            assertEquals(blocksize, in.read(data2, ofs, blocksize));
            ofs += blocksize;
            blocksize = Math.min(blocksize, DATALEN - ofs);
        }
        in.close();
    }

    public void testChunkedStreamToStreamTransfer() throws IOException {
        writeToStreamInChunks();
        byte[] data2 = new byte[DATALEN];
        readFromStreamInChunks(data2);
        assertTrue(Arrays.equals(data1, data2));
    }

    public void testSequentialChannelToChannelTransfer() throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(new ByteArrayInputStream(data1));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(DATALEN);
        WritableByteChannel wbc = Channels.newChannel(baos);
        pipe.transferFrom(rbc);
        pipe.setAutoWriteChannel(wbc, null);
        pipe.autoTransferTo();
        byte[] data2 = baos.toByteArray();
        assertTrue(Arrays.equals(data1, data2));
    }

    public void testConcurrentChannelToChannelTransfer() throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(new ByteArrayInputStream(data1));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(DATALEN);
        WritableByteChannel wbc = Channels.newChannel(baos);
        pipe.setAutoWriteChannel(wbc, null);
        pipe.transferFrom(rbc);
        pipe.closeForWriting();
        byte[] data2 = baos.toByteArray();
        assertTrue(Arrays.equals(data1, data2));
    }

    public void testChunkedChannelToChannelTransfer() throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(new ByteArrayInputStream(data1));
        ByteArrayOutputStream baos = new ByteArrayOutputStream(DATALEN);
        WritableByteChannel wbc = Channels.newChannel(baos);
        pipe.setAutoWriteChannel(wbc, null);
        int transferCount = 0;
        while (transferCount < DATALEN) {
            int chunkSize = Math.min(65531, DATALEN - transferCount);
            int transferSize = pipe.transferFrom(rbc, chunkSize);
            assertEquals(chunkSize, transferSize);
            transferCount += transferSize;
        }
        pipe.closeForWriting();
        byte[] data2 = baos.toByteArray();
        for (int i = 0; i < DATALEN; ++i) {
            if (data1[i] != data2[i]) {
                System.err.println(i);
                break;
            }
        }
        assertTrue(Arrays.equals(data1, data2));
    }

    public void testConcurrentStreamToChannelTransfer() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(DATALEN);
        WritableByteChannel wbc = Channels.newChannel(baos);
        ChannelWriteListenerImpl listener = new ChannelWriteListenerImpl();
        pipe.setAutoWriteChannel(wbc, listener);
        OutputStream out = pipe.getOutputStream();
        out.write(data1);
        out.close();
        byte[] data2 = baos.toByteArray();
        assertTrue(Arrays.equals(data1, data2));
    }

    private static final class ChannelWriteListenerImpl implements ChannelWriteListener {

        IOException ioException = null;

        public void ioExceptionRaised(IOException e) {
            ioException = e;
        }
    }
}
