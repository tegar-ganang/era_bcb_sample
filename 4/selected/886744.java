package jaxlib.io.channel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import jaxlib.io.file.Files;
import jaxlib.junit.XTestCase;

/**
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: FilePipeTest.java 3016 2011-11-28 06:17:26Z joerg_wassmer $
 */
public final class FilePipeTest extends XTestCase {

    public FilePipeTest(String name) {
        super(name);
    }

    private static byte[] generateBytes() {
        final int c = 32768;
        ByteBuffer a = ByteBuffer.allocate(4 * c);
        for (int i = 0; i < c; i++) a.putInt(i);
        return a.array();
    }

    public void testReadWriteMultiThread() throws Exception {
        final byte[] a = generateBytes();
        File f = Files.createTempFile();
        f.deleteOnExit();
        final FilePipe p = new FilePipe(f, false, 0, false);
        assertFalse(p.isDeletingFile());
        assertEquals(0, p.getInitialSize());
        assertTrue(p.sink().isOpen());
        assertTrue(p.source().isOpen());
        assertTrue(p.sink().isBlocking());
        assertTrue(p.source().isBlocking());
        class ReaderThread extends Thread {

            volatile Throwable error = null;

            volatile byte[] result;

            ReaderThread() {
                super();
                setDaemon(false);
            }

            public void run() {
                ByteBuffer buf = ByteBuffer.allocate(a.length);
                buf.limit(0);
                synchronized (this) {
                    notifyAll();
                }
                try {
                    while (buf.position() < buf.capacity()) {
                        buf.limit(Math.min(buf.position() + 128, buf.capacity()));
                        p.source().read(buf);
                    }
                    p.source().close();
                    buf.clear();
                    synchronized (this) {
                        byte[] a = new byte[buf.capacity()];
                        buf.get(a);
                        this.result = a;
                    }
                } catch (final Throwable ex) {
                    this.error = ex;
                    ex.printStackTrace();
                }
            }
        }
        class WriterThread extends Thread {

            volatile Throwable error = null;

            WriterThread() {
                super();
                setDaemon(false);
            }

            public void run() {
                ByteBuffer buf = ByteBuffer.wrap(a);
                buf.limit(0);
                try {
                    for (int i = 0, hi = buf.capacity(); i <= hi; i += 4) {
                        buf.limit(i);
                        p.sink().write(buf);
                    }
                    p.sink().close();
                } catch (final Throwable ex) {
                    this.error = ex;
                    ex.printStackTrace();
                }
            }
        }
        ReaderThread rt = new ReaderThread();
        synchronized (rt) {
            rt.start();
            rt.wait();
        }
        Thread.yield();
        WriterThread wt = new WriterThread();
        wt.start();
        wt.join(20000);
        assertFalse(wt.isAlive());
        rt.join(10000);
        assertFalse(rt.isAlive());
        assertNull(wt.error);
        assertNull(rt.error);
        assertEquals(a.length, f.length());
        synchronized (rt) {
            byte[] b = rt.result;
            assertEquals(a.length, b.length);
            for (int i = 0; i < a.length; i++) {
                if (a[i] != b[i]) fail("Array differs at index " + i);
            }
        }
        f.delete();
        assertFalse(p.source().isOpen());
        assertFalse(p.sink().isOpen());
        assertFalse(p.isFileOpen());
        assertEquals(a.length, p.sink().writeCount());
        assertEquals(a.length, p.source().readCount());
        assertFalse(f.exists());
    }

    public void testReadWriteSingleThread() throws Exception {
        byte[] a = generateBytes();
        File f = Files.createTempFile();
        f.deleteOnExit();
        FilePipe p = new FilePipe(f, false, 0, false);
        assertFalse(p.isDeletingFile());
        assertEquals(0, p.getInitialSize());
        assertTrue(p.sink().isOpen());
        assertTrue(p.source().isOpen());
        ByteBuffer b = ByteBuffer.wrap(a);
        b.limit(0);
        for (int i = 0, hi = b.capacity(); i <= hi; i += 4) {
            b.limit(i);
            p.sink().write(b);
        }
        p.sink().close();
        b = ByteBuffer.allocate(a.length);
        p.source().read(b);
        assertTrue(Arrays.equals(a, b.array()));
        p.source().close();
        f.delete();
        assertFalse(p.source().isOpen());
        assertFalse(p.sink().isOpen());
        assertFalse(p.isFileOpen());
        assertEquals(a.length, p.sink().writeCount());
        assertEquals(a.length, p.source().readCount());
        assertFalse(f.exists());
    }

    public void testTransferFromToByteChannel() throws Exception {
        byte[] a = generateBytes();
        File f = Files.createTempFile();
        f.deleteOnExit();
        FilePipe p = new FilePipe(f, false, 0, false);
        assertFalse(p.isDeletingFile());
        assertEquals(0, p.getInitialSize());
        assertTrue(p.sink().isOpen());
        assertTrue(p.source().isOpen());
        ReadableByteChannel in = Channels.newChannel(new ByteArrayInputStream(a));
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(a.length);
        WritableByteChannel out = Channels.newChannel(outStream);
        assertEquals(a.length, p.sink().transferFromByteChannel(in, a.length));
        p.sink().close();
        assertEquals(a.length, p.source().transferToByteChannel(out, -1));
        p.source().close();
        f.delete();
        assertFalse(p.source().isOpen());
        assertFalse(p.sink().isOpen());
        assertFalse(p.isFileOpen());
        byte[] b = outStream.toByteArray();
        assertEquals(a.length, b.length);
        assertTrue(Arrays.equals(a, b));
        assertFalse(f.exists());
    }
}
