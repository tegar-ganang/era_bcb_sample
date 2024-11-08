package jaxlib.io.channel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import jaxlib.junit.XTestCase;

/**
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: PipeTaskTest.java 2267 2007-03-16 08:33:33Z joerg_wassmer $
 */
public final class PipeTaskTest extends XTestCase {

    public PipeTaskTest(String name) {
        super(name);
    }

    private static byte[] generateBytes() {
        ByteBuffer a = ByteBuffer.allocate(4 * 32768);
        for (int i = 0; i < 32768; i++) a.putInt(i);
        return a.array();
    }

    public void testCancelBeforeRun() throws Exception {
        ReadableByteChannel in = Channels.newChannel(new ByteArrayInputStream(new byte[1]));
        WritableByteChannel out = Channels.newChannel(new ByteArrayOutputStream(1));
        PipeTask task = new PipeTask(in, out);
        task.cancel(false);
        assertFalse(in.isOpen());
        assertFalse(out.isOpen());
    }

    public void testTransfer() throws Exception {
        byte[] a = generateBytes();
        ReadableByteChannel in = Channels.newChannel(new ByteArrayInputStream(a));
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(a.length);
        WritableByteChannel out = Channels.newChannel(outStream);
        PipeTask task = new PipeTask(in, out);
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        task.awaitTermination(5, TimeUnit.SECONDS);
        byte[] b = outStream.toByteArray();
        assertEquals(a.length, b.length);
        assertTrue(Arrays.equals(a, b));
        assertFalse(in.isOpen());
        assertFalse(out.isOpen());
    }

    public void testTransferFromFileChannel() throws Exception {
        byte[] a = generateBytes();
        File f = File.createTempFile("test", null);
        RandomAccessFile rf = new RandomAccessFile(f, "rw");
        rf.write(a);
        rf.seek(0);
        ReadableByteChannel in = rf.getChannel();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(a.length);
        WritableByteChannel out = Channels.newChannel(outStream);
        PipeTask task = new PipeTask(in, out);
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        task.awaitTermination(5, TimeUnit.SECONDS);
        byte[] b = outStream.toByteArray();
        assertEquals(a.length, b.length);
        assertTrue(Arrays.equals(a, b));
        assertFalse(in.isOpen());
        f.delete();
        assertFalse(out.isOpen());
    }

    public void testTransferToFileChannel() throws Exception {
        byte[] a = generateBytes();
        File f = File.createTempFile("test", null);
        RandomAccessFile rf = new RandomAccessFile(f, "rw");
        rf.write(a);
        rf.seek(0);
        ReadableByteChannel in = Channels.newChannel(new ByteArrayInputStream(a));
        WritableByteChannel out = rf.getChannel();
        PipeTask task = new PipeTask(in, true, out, false, 8192, true);
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        task.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(out.isOpen());
        byte[] b = new byte[a.length];
        rf.seek(0);
        rf.readFully(b);
        rf.close();
        f.delete();
        assertEquals(a.length, b.length);
        assertTrue(Arrays.equals(a, b));
        assertFalse(in.isOpen());
    }

    public void testTransferFromToPipe() throws Exception {
        byte[] a = generateBytes();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(a.length);
        WritableByteChannel out = Channels.newChannel(outStream);
        Pipe p = Pipe.open();
        ReadableByteChannel in = p.source();
        PipeTask task = new PipeTask(in, out);
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        WritableByteChannel sink = p.sink();
        ByteBuffer abuf = ByteBuffer.wrap(a);
        while (abuf.hasRemaining()) {
            int step = Math.min(1024, abuf.remaining());
            ByteBuffer buf = abuf.duplicate();
            buf.limit(buf.position() + step);
            sink.write(buf);
            abuf.position(buf.position());
        }
        sink.close();
        task.awaitTermination(5, TimeUnit.SECONDS);
        byte[] b = outStream.toByteArray();
        assertEquals(a.length, b.length);
        assertTrue(Arrays.equals(a, b));
        assertFalse(in.isOpen());
        assertFalse(out.isOpen());
        assertFalse(sink.isOpen());
    }
}
