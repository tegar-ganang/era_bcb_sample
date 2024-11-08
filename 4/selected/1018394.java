package com.phloc.commons.io.streams;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.IOException;
import org.junit.Test;
import com.phloc.commons.CGlobal;
import com.phloc.commons.random.VerySecureRandom;

/**
 * Test class for class {@link BitOutputStream}.
 * 
 * @author philip
 */
public final class BitOutputStreamTest {

    @Test
    public void testSemantics() throws IOException {
        try {
            new BitOutputStream(null, true);
            fail();
        } catch (final NullPointerException ex) {
        }
        final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream();
        final BitOutputStream aBOS = new BitOutputStream(aBAOS, true);
        aBOS.writeBit(CGlobal.BIT_SET);
        try {
            aBOS.writeBit(-1);
            fail();
        } catch (final IllegalArgumentException ex) {
        }
        try {
            aBOS.writeBits(1, 0);
            fail();
        } catch (final IllegalArgumentException ex) {
        }
        try {
            aBOS.writeBits(1, 33);
            fail();
        } catch (final IllegalArgumentException ex) {
        }
        aBOS.close();
        try {
            aBOS.writeBit(CGlobal.BIT_NOT_SET);
            fail();
        } catch (final IllegalStateException ex) {
        }
        aBOS.close();
        assertNotNull(aBOS.toString());
    }

    @Test
    public void testWriteBitHighOrder() throws IOException {
        final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream();
        final BitOutputStream aBOS = new BitOutputStream(aBAOS, true);
        aBOS.writeBit(1);
        for (int i = 0; i < 7; ++i) aBOS.writeBit(0);
        assertTrue(aBAOS.size() > 0);
        final int aByte = aBAOS.toByteArray()[0] & 0xff;
        assertEquals(128, aByte);
    }

    @Test
    public void testWriteBitLowOrder() throws IOException {
        final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream();
        final BitOutputStream aBOS = new BitOutputStream(aBAOS, false);
        for (int i = 0; i < 7; ++i) aBOS.writeBit(0);
        aBOS.writeBit(1);
        assertTrue(aBAOS.size() > 0);
        final int aByte = aBAOS.toByteArray()[0] & 0xff;
        assertEquals(128, aByte);
    }

    @Test
    public void testWriteManyHighOrder() throws IOException {
        for (int i = 0; i < 200; i += 3) {
            final byte[] buf = new byte[i * 100];
            VerySecureRandom.getInstance().nextBytes(buf);
            final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream();
            final BitOutputStream aBOS = new BitOutputStream(aBAOS, true);
            for (final byte b : buf) aBOS.writeBits(b & 0xff, 8);
            final byte[] written = aBAOS.toByteArray();
            assertArrayEquals(buf, written);
            final BitInputStream aBIS = new BitInputStream(new NonBlockingByteArrayInputStream(written), true);
            aBAOS.reset();
            for (int x = 0; x < written.length; ++x) aBAOS.write(aBIS.readBits(8));
            final byte[] read = aBAOS.toByteArray();
            assertArrayEquals(written, read);
            assertArrayEquals(buf, read);
        }
    }

    @Test
    public void testWriteManyLowOrder() throws IOException {
        for (int i = 0; i < 200; i += 3) {
            final byte[] buf = new byte[i * 100];
            VerySecureRandom.getInstance().nextBytes(buf);
            final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream();
            final BitOutputStream aBOS = new BitOutputStream(aBAOS, false);
            for (final byte b : buf) aBOS.writeBits(b & 0xff, 8);
            final byte[] written = aBAOS.toByteArray();
            final BitInputStream aBIS = new BitInputStream(new NonBlockingByteArrayInputStream(written), false);
            aBAOS.reset();
            for (int x = 0; x < written.length; ++x) aBAOS.write(aBIS.readBits(8));
            final byte[] read = aBAOS.toByteArray();
            assertArrayEquals(buf, read);
        }
    }

    @Test
    public void testReadWriteRandom() throws IOException {
        for (int i = 0; i < 200; i += 3) {
            final byte[] buf = new byte[i * 100];
            VerySecureRandom.getInstance().nextBytes(buf);
            final BitInputStream aBIS = new BitInputStream(new NonBlockingByteArrayInputStream(buf), true);
            final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream();
            final BitOutputStream aBOS = new BitOutputStream(aBAOS, true);
            int nBitCount = buf.length * 8;
            while (nBitCount > 0) {
                final int nBits = Math.min(nBitCount, Math.max(1, VerySecureRandom.getInstance().nextInt(13)));
                aBOS.writeBits(aBIS.readBits(nBits), nBits);
                nBitCount -= nBits;
            }
            final byte[] read = aBAOS.toByteArray();
            assertArrayEquals(buf, read);
        }
    }

    @Test
    public void testWriteManual() throws IOException {
        final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream();
        final BitOutputStream aBOS = new BitOutputStream(aBAOS, true);
        aBOS.writeBits(255, 8);
        aBOS.writeBits(0, 8);
        aBOS.writeBits(255, 8);
        aBOS.close();
        assertArrayEquals(new byte[] { (byte) 0xff, 0, (byte) 0xff }, aBAOS.toByteArray());
    }
}
