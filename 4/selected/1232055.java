package jaxlib.io.stream.benchmark;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.ref.SoftReference;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import org.jaxlib.junit.Benchmark;
import org.jaxlib.junit.PerformanceMeter;
import jaxlib.io.stream.BufferedXReader;
import jaxlib.io.stream.ByteBufferInputStream;
import jaxlib.io.stream.ByteBufferOutputStream;
import jaxlib.io.stream.ByteChannelReader;
import jaxlib.io.stream.InputStreamXReader;

/**
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: InputStreamXReaderBenchmark.java 2267 2007-03-16 08:33:33Z joerg_wassmer $
 */
public class InputStreamXReaderBenchmark extends Benchmark {

    public static void main(String[] argv) {
        runSuite(InputStreamXReaderBenchmark.class);
    }

    private static final int BYTE_COUNT = 1000000;

    private static SoftReference<byte[]> sharedIn;

    private static File sharedFile;

    public InputStreamXReaderBenchmark(String name) {
        super(name);
    }

    private static File createFile() throws IOException {
        File f = sharedFile;
        if (f == null) {
            f = File.createTempFile("jaxlib-test", null);
            f.deleteOnExit();
            FileOutputStream out = new FileOutputStream(f);
            Object inReference = createInput();
            out.write(sharedIn.get());
            out.close();
            sharedFile = f;
        }
        return f;
    }

    private static ByteArrayInputStream createInput() throws IOException {
        byte[] buf = (sharedIn == null) ? null : sharedIn.get();
        if (buf == null) {
            ByteBufferOutputStream bout = new ByteBufferOutputStream(BYTE_COUNT);
            OutputStreamWriter out = new OutputStreamWriter(bout, Charset.forName("ISO-8859-1"));
            for (int i = BYTE_COUNT; --i >= 0; ) {
                out.write(i & 0xff);
            }
            out.close();
            buf = bout.getBuffer().array();
            sharedIn = new SoftReference<byte[]>(buf);
        }
        return new ByteArrayInputStream(buf);
    }

    public void testByteChannelReader_fileChannelSource() throws IOException {
        File file = createFile();
        FileChannel inChannel = new FileInputStream(file).getChannel();
        BufferedXReader in = new BufferedXReader(new ByteChannelReader(inChannel, Charset.forName("ISO-8859-1").newDecoder()));
        long ops = 0;
        PerformanceMeter meter = initBenchmark();
        gc();
        meter.start();
        do {
            while (in.read() >= 0) ;
            ops += BYTE_COUNT;
            inChannel.position(0);
        } while (meter.getRunTime() < 5000);
        meter.stop(ops);
        in.close();
    }

    public void testInputStreamReader_fileInputStreamSource() throws IOException {
        File file = createFile();
        FileInputStream fileIn = new FileInputStream(file);
        BufferedXReader in = new BufferedXReader(new InputStreamReader(fileIn, Charset.forName("ISO-8859-1").newDecoder()));
        long ops = 0;
        PerformanceMeter meter = initBenchmark();
        gc();
        meter.start();
        do {
            while (in.read() >= 0) ;
            ops += BYTE_COUNT;
            fileIn.getChannel().position(0);
        } while (meter.getRunTime() < 5000);
        meter.stop(ops);
        in.close();
    }

    public void testInputStreamXReader_fileInputStreamSource() throws IOException {
        File file = createFile();
        FileInputStream fileIn = new FileInputStream(file);
        fileIn.mark(Integer.MAX_VALUE);
        BufferedXReader in = new BufferedXReader(new InputStreamReader(fileIn, Charset.forName("ISO-8859-1").newDecoder()));
        long ops = 0;
        PerformanceMeter meter = initBenchmark();
        gc();
        meter.start();
        do {
            while (in.read() >= 0) ;
            ops += BYTE_COUNT;
            fileIn.getChannel().position(0);
        } while (meter.getRunTime() < 5000);
        meter.stop(ops);
        in.close();
    }

    public void testInputStreamReader_memorySource() throws IOException {
        ByteArrayInputStream bin = createInput();
        BufferedXReader in = new BufferedXReader(new InputStreamReader(bin, Charset.forName("ISO-8859-1").newDecoder()));
        long ops = 0;
        PerformanceMeter meter = initBenchmark();
        gc();
        meter.start();
        do {
            while (in.read() >= 0) ;
            ops += BYTE_COUNT;
            bin.reset();
        } while (meter.getRunTime() < 5000);
        meter.stop(ops);
        in.close();
    }

    public void testInputStreamXReader_memorySource() throws IOException {
        ByteArrayInputStream bin = createInput();
        BufferedXReader in = new BufferedXReader(new InputStreamXReader(bin, Charset.forName("ISO-8859-1").newDecoder()));
        long ops = 0;
        PerformanceMeter meter = initBenchmark();
        gc();
        meter.start();
        do {
            while (in.read() >= 0) ;
            ops += BYTE_COUNT;
            bin.reset();
        } while (meter.getRunTime() < 5000);
        meter.stop(ops);
        in.close();
    }
}
