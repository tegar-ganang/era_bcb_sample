package nl.flotsam.preon.buffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import junit.framework.TestCase;
import nl.flotsam.preon.buffer.BitBuffer;
import nl.flotsam.preon.buffer.BitBufferException;
import nl.flotsam.preon.buffer.BitBufferUnderflowException;
import nl.flotsam.preon.buffer.ByteOrder;
import nl.flotsam.preon.buffer.DefaultBitBuffer;
import org.apache.commons.io.IOUtils;

public class DefaultBitBufferTest extends TestCase {

    private DefaultBitBuffer bitBuffer;

    private ByteBuffer byteBuffer;

    private int fileSize;

    @Override
    protected void setUp() throws Exception {
        byteBuffer = getByteBuffer("testFile");
        bitBuffer = new DefaultBitBuffer(byteBuffer);
        fileSize = byteBuffer.capacity();
    }

    public void testReadBits() {
        assertEquals(67, bitBuffer.readBits(0, 8));
        assertEquals(52, bitBuffer.readBits(4, 8));
        assertEquals(52, bitBuffer.readBits(6, 6));
        assertEquals(2, bitBuffer.readBits(1, 2));
        byte[] bytesReadByApp = new byte[fileSize];
        for (int i = 0; i < fileSize; i++) bytesReadByApp[i] = (byte) bitBuffer.readBits(i * 8, 8);
        byte[] bytesReadDirectly = new byte[fileSize];
        byteBuffer.asReadOnlyBuffer().get(bytesReadDirectly);
        assertTrue(java.util.Arrays.equals(bytesReadDirectly, bytesReadByApp));
        assertEquals(byteBuffer.getLong(), bitBuffer.readBits(0, 64));
    }

    public void testLittleEndianBitOffset() {
        bitBuffer = new DefaultBitBuffer(ByteBuffer.wrap(new byte[] { 3, 0, 0, 0 }));
        assertEquals(1, bitBuffer.readAsInt(1, ByteOrder.LittleEndian));
        assertEquals(1, bitBuffer.readAsInt(1, ByteOrder.LittleEndian));
        assertEquals(0, bitBuffer.readAsInt(1, ByteOrder.LittleEndian));
        assertEquals(1, bitBuffer.readAsInt(0, 1, ByteOrder.LittleEndian));
        assertEquals(3, bitBuffer.readAsInt(0, 2, ByteOrder.LittleEndian));
        assertEquals(3, bitBuffer.readAsInt(0, 10, ByteOrder.LittleEndian));
    }

    public void testBigEndianBitOffset() {
        bitBuffer = new DefaultBitBuffer(ByteBuffer.wrap(new byte[] { (byte) 0xCA, (byte) 0x96, 0, 0 }));
        assertEquals(1, bitBuffer.readAsInt(1, ByteOrder.BigEndian));
        assertEquals(1, bitBuffer.readAsInt(1, ByteOrder.BigEndian));
        assertEquals(0, bitBuffer.readAsInt(1, ByteOrder.BigEndian));
        assertEquals(1, bitBuffer.readAsInt(0, 1, ByteOrder.BigEndian));
        assertEquals(3, bitBuffer.readAsInt(0, 2, ByteOrder.BigEndian));
        assertEquals(810, bitBuffer.readAsInt(0, 10, ByteOrder.BigEndian));
    }

    public void testReadBitsSequentially() {
        byte[] bytesReadByApp = new byte[fileSize];
        for (int i = 0; i < fileSize; i++) bytesReadByApp[i] = (byte) bitBuffer.readBits(8);
        byte[] bytesReadDirectly = new byte[fileSize];
        byteBuffer.asReadOnlyBuffer().get(bytesReadDirectly);
        assertTrue(java.util.Arrays.equals(bytesReadDirectly, bytesReadDirectly));
    }

    public void testBitOneAfterAnother() {
        assertEquals(0, bitBuffer.readBits(1));
        assertEquals(1, bitBuffer.readBits(1));
        assertEquals(0, bitBuffer.readBits(1));
        assertEquals(0, bitBuffer.readBits(1));
        assertEquals(0, bitBuffer.readBits(1));
        assertEquals(0, bitBuffer.readBits(1));
        assertEquals(true, bitBuffer.readAsBoolean());
        assertEquals(true, bitBuffer.readAsBoolean());
    }

    public void testCompleteBytes() {
        DefaultBitBuffer bitBuffer;
        bitBuffer = new DefaultBitBuffer(ByteBuffer.wrap(new byte[] { 0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF }));
        assertEquals(0x0123456789ABCDEFL, bitBuffer.readBits(64));
        bitBuffer = new DefaultBitBuffer(ByteBuffer.wrap(new byte[] { (byte) 0xEF, (byte) 0xCD, (byte) 0xAB, (byte) 0x89, 0x67, 0x45, 0x23, 0x01 }));
        assertEquals(0x0123456789ABCDEFL, bitBuffer.readBits(64, ByteOrder.LittleEndian));
    }

    public void testReadBeyondEnd() {
        DefaultBitBuffer buffer = new DefaultBitBuffer(ByteBuffer.wrap(new byte[] { 1, 2, 3 }));
        buffer.readAsByte(8);
        buffer.readAsByte(8);
        buffer.readAsByte(8);
        try {
            buffer.readAsByte(8);
            fail("Expecting exception while reading beyond end of buffer.");
        } catch (BitBufferUnderflowException oore) {
        }
    }

    public void testReadAsByteBuffer() throws Exception {
        DefaultBitBuffer buffer = new DefaultBitBuffer(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }));
        assertEquals(1, buffer.readAsByte(8));
        assertEquals(2 * 256 + 3, buffer.readAsShort(16));
        ByteBuffer byteBuffer = buffer.readAsByteBuffer(5);
        byte[] bufferArray = new byte[5];
        byteBuffer.get(bufferArray);
        assertEquals(4, bufferArray[0]);
        assertEquals(8, bufferArray[4]);
        assertEquals(9, buffer.readAsByte(8));
        assertEquals(10, buffer.readAsByte(8));
    }

    public void testReadAsByteBufferWithBitAlignments() throws Exception {
        DefaultBitBuffer buffer = new DefaultBitBuffer(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 }));
        buffer.readAsByte(7);
        try {
            ByteBuffer byteBuffer = buffer.readAsByteBuffer(2);
            fail();
        } catch (BitBufferException bbe) {
        }
        buffer = new DefaultBitBuffer(ByteBuffer.wrap(new byte[] { 1, 2, 3, 4 }));
        buffer.readAsByte(7);
        buffer.readAsInt(9);
        buffer.readAsByteBuffer(2);
    }

    private ByteBuffer getByteBuffer(String resource) throws IOException {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream in = classLoader.getResourceAsStream(resource);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        return ByteBuffer.wrap(out.toByteArray());
    }

    public void testReadingIntegers() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 });
        BitBuffer bitBuffer = new DefaultBitBuffer(buffer);
        bitBuffer.setBitPos(0);
        assertEquals(1, bitBuffer.readAsInt(8));
        bitBuffer.setBitPos(0);
        assertEquals(0, bitBuffer.readAsInt(7));
        bitBuffer.setBitPos(1);
        assertEquals(2, bitBuffer.readAsInt(8));
        bitBuffer.setBitPos(0);
        assertEquals(0x0102, bitBuffer.readAsInt(16));
        bitBuffer.setBitPos(0);
        assertEquals(0x0102, bitBuffer.readAsInt(16, ByteOrder.BigEndian));
        bitBuffer.setBitPos(0);
        assertEquals(0x0201, bitBuffer.readAsInt(16, ByteOrder.LittleEndian));
        bitBuffer.setBitPos(1);
        assertEquals(0x0102 << 1, bitBuffer.readAsInt(16));
        bitBuffer.setBitPos(1);
        assertEquals(bitBuffer.readAsInt(1, 5, ByteOrder.BigEndian), bitBuffer.readAsInt(1, 5, ByteOrder.LittleEndian));
        assertEquals(bitBuffer.readAsShort(1, 9, ByteOrder.BigEndian), bitBuffer.readAsInt(1, 9, ByteOrder.BigEndian));
        assertEquals(bitBuffer.readAsByte(3, 7, ByteOrder.BigEndian), bitBuffer.readAsInt(3, 7, ByteOrder.BigEndian));
    }

    public void testReading1() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 0x00, 0x00, 0x00, 0x01 });
        BitBuffer bitBuffer = new DefaultBitBuffer(buffer);
        assertEquals(1, bitBuffer.readAsInt(32));
    }
}
