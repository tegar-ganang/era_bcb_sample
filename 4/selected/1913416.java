package org.apache.http.impl.nio.codecs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.nio.reactor.SessionOutputBufferImpl;
import org.apache.http.nio.reactor.SessionOutputBuffer;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EncodingUtils;

/**
 * Simple tests for {@link LengthDelimitedEncoder}.
 *
 * 
 * @version $Id: TestLengthDelimitedEncoder.java 744515 2009-02-14 16:36:56Z sebb $
 */
public class TestLengthDelimitedEncoder extends TestCase {

    public TestLengthDelimitedEncoder(String testName) {
        super(testName);
    }

    public static void main(String args[]) {
        String[] testCaseName = { TestLengthDelimitedEncoder.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite() {
        return new TestSuite(TestLengthDelimitedEncoder.class);
    }

    private static ByteBuffer wrap(final String s) {
        return ByteBuffer.wrap(EncodingUtils.getAsciiBytes(s));
    }

    private static WritableByteChannel newChannel(final ByteArrayOutputStream baos) {
        return Channels.newChannel(baos);
    }

    public void testBasicCoding() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableByteChannel channel = newChannel(baos);
        HttpParams params = new BasicHttpParams();
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(1024, 128, params);
        HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();
        LengthDelimitedEncoder encoder = new LengthDelimitedEncoder(channel, outbuf, metrics, 16);
        encoder.write(wrap("stuff;"));
        encoder.write(wrap("more stuff"));
        String s = baos.toString("US-ASCII");
        assertTrue(encoder.isCompleted());
        assertEquals("stuff;more stuff", s);
    }

    public void testCodingBeyondContentLimit() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableByteChannel channel = newChannel(baos);
        HttpParams params = new BasicHttpParams();
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(1024, 128, params);
        HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();
        LengthDelimitedEncoder encoder = new LengthDelimitedEncoder(channel, outbuf, metrics, 16);
        encoder.write(wrap("stuff;"));
        encoder.write(wrap("more stuff; and a lot more stuff"));
        String s = baos.toString("US-ASCII");
        assertTrue(encoder.isCompleted());
        assertEquals("stuff;more stuff", s);
    }

    public void testCodingEmptyBuffer() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableByteChannel channel = newChannel(baos);
        HttpParams params = new BasicHttpParams();
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(1024, 128, params);
        HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();
        LengthDelimitedEncoder encoder = new LengthDelimitedEncoder(channel, outbuf, metrics, 16);
        encoder.write(wrap("stuff;"));
        ByteBuffer empty = ByteBuffer.allocate(100);
        empty.flip();
        encoder.write(empty);
        encoder.write(null);
        encoder.write(wrap("more stuff"));
        String s = baos.toString("US-ASCII");
        assertTrue(encoder.isCompleted());
        assertEquals("stuff;more stuff", s);
    }

    public void testCodingCompleted() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableByteChannel channel = newChannel(baos);
        HttpParams params = new BasicHttpParams();
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(1024, 128, params);
        HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();
        LengthDelimitedEncoder encoder = new LengthDelimitedEncoder(channel, outbuf, metrics, 5);
        encoder.write(wrap("stuff"));
        try {
            encoder.write(wrap("more stuff"));
            fail("IllegalStateException should have been thrown");
        } catch (IllegalStateException ex) {
        }
    }

    public void testCodingBeyondContentLimitFromFile() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableByteChannel channel = newChannel(baos);
        HttpParams params = new BasicHttpParams();
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(1024, 128, params);
        HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();
        LengthDelimitedEncoder encoder = new LengthDelimitedEncoder(channel, outbuf, metrics, 16);
        File tmpFile = File.createTempFile("testFile", "txt");
        FileOutputStream fout = new FileOutputStream(tmpFile);
        OutputStreamWriter wrtout = new OutputStreamWriter(fout);
        wrtout.write("stuff;");
        wrtout.write("more stuff; and a lot more stuff");
        wrtout.flush();
        wrtout.close();
        FileChannel fchannel = new FileInputStream(tmpFile).getChannel();
        encoder.transfer(fchannel, 0, 20);
        String s = baos.toString("US-ASCII");
        assertTrue(encoder.isCompleted());
        assertEquals("stuff;more stuff", s);
        tmpFile.delete();
    }

    public void testCodingEmptyFile() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableByteChannel channel = newChannel(baos);
        HttpParams params = new BasicHttpParams();
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(1024, 128, params);
        HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();
        LengthDelimitedEncoder encoder = new LengthDelimitedEncoder(channel, outbuf, metrics, 16);
        encoder.write(wrap("stuff;"));
        File tmpFile = File.createTempFile("testFile", "txt");
        FileOutputStream fout = new FileOutputStream(tmpFile);
        OutputStreamWriter wrtout = new OutputStreamWriter(fout);
        wrtout.flush();
        wrtout.close();
        FileChannel fchannel = new FileInputStream(tmpFile).getChannel();
        encoder.transfer(fchannel, 0, 20);
        encoder.write(wrap("more stuff"));
        String s = baos.toString("US-ASCII");
        assertTrue(encoder.isCompleted());
        assertEquals("stuff;more stuff", s);
        tmpFile.delete();
    }

    public void testCodingCompletedFromFile() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableByteChannel channel = newChannel(baos);
        HttpParams params = new BasicHttpParams();
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(1024, 128, params);
        HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();
        LengthDelimitedEncoder encoder = new LengthDelimitedEncoder(channel, outbuf, metrics, 5);
        encoder.write(wrap("stuff"));
        File tmpFile = File.createTempFile("testFile", "txt");
        FileOutputStream fout = new FileOutputStream(tmpFile);
        OutputStreamWriter wrtout = new OutputStreamWriter(fout);
        wrtout.write("more stuff");
        wrtout.flush();
        wrtout.close();
        try {
            FileChannel fchannel = new FileInputStream(tmpFile).getChannel();
            encoder.transfer(fchannel, 0, 10);
            fail("IllegalStateException should have been thrown");
        } catch (IllegalStateException ex) {
        } finally {
            tmpFile.delete();
        }
    }

    public void testCodingFromFileSmaller() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableByteChannel channel = newChannel(baos);
        HttpParams params = new BasicHttpParams();
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(1024, 128, params);
        HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();
        LengthDelimitedEncoder encoder = new LengthDelimitedEncoder(channel, outbuf, metrics, 16);
        File tmpFile = File.createTempFile("testFile", "txt");
        FileOutputStream fout = new FileOutputStream(tmpFile);
        OutputStreamWriter wrtout = new OutputStreamWriter(fout);
        wrtout.write("stuff;");
        wrtout.write("more stuff;");
        wrtout.flush();
        wrtout.close();
        FileChannel fchannel = new FileInputStream(tmpFile).getChannel();
        encoder.transfer(fchannel, 0, 20);
        String s = baos.toString("US-ASCII");
        assertTrue(encoder.isCompleted());
        assertEquals("stuff;more stuff", s);
        tmpFile.delete();
    }

    public void testInvalidConstructor() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableByteChannel channel = newChannel(baos);
        HttpParams params = new BasicHttpParams();
        SessionOutputBuffer outbuf = new SessionOutputBufferImpl(1024, 128, params);
        HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();
        try {
            new LengthDelimitedEncoder(null, null, null, 10);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException ex) {
        }
        try {
            new LengthDelimitedEncoder(channel, null, null, 10);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException ex) {
        }
        try {
            new LengthDelimitedEncoder(channel, outbuf, null, 10);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException ex) {
        }
        try {
            new LengthDelimitedEncoder(channel, outbuf, metrics, -10);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException ex) {
        }
    }
}
