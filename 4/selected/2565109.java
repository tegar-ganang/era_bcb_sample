package nl.flotsam.preon.buffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * An implementation of {@link BitBuffer} wrapping a {@link ByteBuffer}.
 *
 * @author Bartosz Wieczorek
 * @since Feb 18, 2007
 */
public class DefaultBitBuffer implements BitBuffer {

    static Log log = LogFactory.getLog(DefaultBitBuffer.class);

    private ByteBuffer byteBuffer;

    private long bitPos;

    private long bitBufBitSize;

    /**
     * Constructs a new instance.
     *
     * @param inputByteBuffer input buffered byte stream
     */
    public DefaultBitBuffer(ByteBuffer inputByteBuffer) {
        this(inputByteBuffer, ((long) (inputByteBuffer.capacity())) << 3, 0L);
    }

    /**
     * Constructs a new instance.
     *
     * @param inputByteBuffer
     * @param bitBufBitSize
     * @param bitPos
     */
    private DefaultBitBuffer(ByteBuffer inputByteBuffer, long bitBufBitSize, long bitPos) {
        this.byteBuffer = inputByteBuffer;
        this.bitBufBitSize = bitBufBitSize;
        this.bitPos = bitPos;
    }

    /** Read byte buffer containing binary stream and set the bit pointer position to 0. */
    public DefaultBitBuffer(String fileName) {
        File file = new File(fileName);
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            int fileSize = (int) fc.size();
            ByteBuffer inputByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
            fc.close();
            this.byteBuffer = inputByteBuffer;
            bitBufBitSize = ((long) (inputByteBuffer.capacity())) << 3;
            bitPos = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setBitPos(long bitPos) {
        this.bitPos = bitPos;
    }

    public long getBitPos() {
        return this.bitPos;
    }

    public long getBitBufBitSize() {
        return bitBufBitSize;
    }

    public long readBits(int nrBits) {
        return readAsLong(nrBits);
    }

    public long readBits(long bitPos, int nrBits) {
        return readAsLong(bitPos, nrBits);
    }

    public long readBits(int nrBits, ByteOrder endian) {
        return readAsLong(nrBits, endian);
    }

    public long readBits(long bitPos, int nrBits, ByteOrder endian) {
        if (nrBits <= 8) return readAsByte(bitPos, nrBits, endian); else if (nrBits <= 16) return readAsShort(bitPos, nrBits, endian); else if (nrBits <= 32) return readAsInt(bitPos, nrBits, endian); else if (nrBits <= 64) return readAsLong(bitPos, nrBits, endian); else throw new BitBufferException("Wrong number of bits to read (" + nrBits + ").");
    }

    public boolean readAsBoolean() {
        return readAsBoolean(bitPos, ByteOrder.BigEndian);
    }

    public boolean readAsBoolean(long bitPos) {
        return readAsBoolean(bitPos, ByteOrder.BigEndian);
    }

    public boolean readAsBoolean(ByteOrder endian) {
        return readAsBoolean(bitPos, endian);
    }

    public boolean readAsBoolean(long bitPos, ByteOrder endian) {
        return getResultAsInt(bitPos, 1, endian, 1) == 1;
    }

    public byte readAsByte(int nrBits) {
        return readAsByte(bitPos, nrBits, ByteOrder.BigEndian);
    }

    public byte readAsByte(int nrBits, ByteOrder endian) {
        return readAsByte(bitPos, nrBits, endian);
    }

    public byte readAsByte(int nrBits, long bitPos) {
        return readAsByte(bitPos, nrBits, ByteOrder.BigEndian);
    }

    public byte readAsByte(long bitPos, int nrBits, ByteOrder endian) {
        return (byte) getResultAsInt(bitPos, nrBits, endian, 8);
    }

    public short readAsShort(int nrBits) {
        return readAsShort(bitPos, nrBits, ByteOrder.BigEndian);
    }

    public short readAsShort(long bitPos, int nrBits) {
        return readAsShort(bitPos, nrBits, ByteOrder.BigEndian);
    }

    public short readAsShort(int nrBits, ByteOrder endian) {
        return readAsShort(bitPos, nrBits, endian);
    }

    public short readAsShort(long bitPos, int nrBits, ByteOrder endian) {
        return (short) getResultAsInt(bitPos, nrBits, endian, 16);
    }

    public int readAsInt(int nrBits) {
        return readAsInt(bitPos, nrBits, ByteOrder.BigEndian);
    }

    public int readAsInt(long bitPos, int nrBits) {
        return readAsInt(bitPos, nrBits, ByteOrder.BigEndian);
    }

    public int readAsInt(int nrBits, ByteOrder endian) {
        return readAsInt(bitPos, nrBits, endian);
    }

    public int readAsInt(long bitPos, int nrBits, ByteOrder endian) {
        if (getNrNecessaryBytes(bitPos, nrBits) > 4) return (int) getResultAsLong(bitPos, nrBits, endian, 32); else return getResultAsInt(bitPos, nrBits, endian, 32);
    }

    public long readAsLong(int nrBits) {
        return readAsLong(bitPos, nrBits, ByteOrder.BigEndian);
    }

    public long readAsLong(long bitPos, int nrBits) {
        return readAsLong(bitPos, nrBits, ByteOrder.BigEndian);
    }

    public long readAsLong(int nrBits, ByteOrder endian) {
        return readAsLong(bitPos, nrBits, endian);
    }

    public long readAsLong(long bitPos, int nrBits, ByteOrder endian) {
        return getResultAsLong(bitPos, nrBits, endian, 64);
    }

    /**
     * Return the minimum number of bytes that are necessary to be read in order to read specified bits
     *
     * @param bitPos position of the first bit to read in the bit buffer
     * @param nrBits number of bits to read
     * @return number of bytes to read
     */
    private static int getNrNecessaryBytes(long bitPos, int nrBits) {
        return (int) (((bitPos % 8) + nrBits + 7) / 8);
    }

    /**
     * This method shifts to the right the integer buffer containing the specified bits, so that the last specified bit is
     * the last bit in the buffer.
     *
     * @param numberBuf integer buffer containing specified bits
     * @param bitPos    position of the first specified bit
     * @param nrBits    number of bits to read
     * @return shifted integer buffer
     */
    private static int getRightShiftedNumberBufAsInt(int numberBuf, long bitPos, int nrBits, ByteOrder endian) throws BitBufferException {
        long shiftBits;
        if (endian == ByteOrder.BigEndian) shiftBits = 7 - ((nrBits + bitPos + 7) % 8); else shiftBits = bitPos % 8;
        return numberBuf >> shiftBits;
    }

    /**
     * This method shifts to the right the long buffer containing the specified bits, so that the last specified bit is the
     * last bit in the buffer.
     *
     * @param numberBuf long buffer containing specified bits
     * @param bitPos    position of the first specified bit
     * @param nrBits    number of bits to read
     * @return shifted integer buffer
     */
    private static long getRightShiftedNumberBufAsLong(long numberBuf, long bitPos, int nrBits, ByteOrder endian) throws BitBufferException {
        long shiftBits;
        if (endian == ByteOrder.BigEndian) shiftBits = 7 - ((nrBits + bitPos + 7) % 8); else shiftBits = bitPos % 8;
        return numberBuf >> shiftBits;
    }

    /**
     * Return a value of integer type representing the buffer storing specified bits
     *
     * @param endian       Endian.Little of Endian.Big order of storing bytes
     * @param nrReadBytes  number of bytes that are necessary to be read in order to read specific bits
     * @param firstBytePos position of the first byte that is necessary to be read
     * @return value of all read bytes, containing specified bits
     */
    private int getNumberBufAsInt(ByteOrder endian, int nrReadBytes, int firstBytePos) {
        int result = 0;
        int bytePortion = 0;
        for (int i = 0; i < nrReadBytes; i++) {
            bytePortion = 0xFF & (byteBuffer.get(firstBytePos++));
            if (endian == ByteOrder.LittleEndian) result = result | bytePortion << (i << 3); else result = bytePortion << ((nrReadBytes - i - 1) << 3) | result;
        }
        return result;
    }

    /**
     * Return a value of long type representing the buffer storing specified bits.
     *
     * @param endian       Endian.Little of Endian.Big order of storing bytes
     * @param nrReadBytes  number of bytes that are necessary to be read in order to read specific bits
     * @param firstBytePos position of the first byte that is necessary to be read
     * @return value of all read bytes, containing specified bits
     */
    private long getNumberBufAsLong(ByteOrder endian, int nrReadBytes, int firstBytePos) {
        long result = 0L;
        long bytePortion = 0L;
        for (int i = 0; i < nrReadBytes; i++) {
            bytePortion = 0xFF & (byteBuffer.get(firstBytePos++));
            if (endian == ByteOrder.LittleEndian) result = result | bytePortion << (i << 3); else result = bytePortion << ((nrReadBytes - i - 1) << 3) | result;
        }
        return result;
    }

    /**
     * Check if all input parameters are correct, otherwise throw BitBufferException
     *
     * @param bitPos        position of the first bit to read in the bit buffer
     * @param nrBits        number of bits to read
     * @param maxNrBitsRead maximum number of bits allowed to read, based on the method return type
     */
    private void validateInputParams(long bitPos, int nrBits, int maxNrBitsRead) {
        if (nrBits < 1) {
            throw new BitBufferException("Number of bits to read (" + nrBits + ") should greater than zero.");
        }
        if (bitPos < 0) throw new BitBufferException("Bit position (" + bitPos + ") should be positive.");
        if (maxNrBitsRead != 1 && maxNrBitsRead != 8 && maxNrBitsRead != 16 && maxNrBitsRead != 32 && maxNrBitsRead != 64) throw new BitBufferException("Max number of bits to read (" + maxNrBitsRead + ") should be either 1, 8, 16, 32 or 64.");
        if (nrBits > maxNrBitsRead) throw new BitBufferException("Cannot read " + nrBits + " bits using " + maxNrBitsRead + " bit long numberBuf (bitPos=" + bitPos + ").");
        if (bitPos + nrBits > bitBufBitSize) throw new BitBufferUnderflowException(bitPos, nrBits);
    }

    /**
     * Return an integral mask with a given number of 1's at least significant bit positions.
     *
     * @param nrBits number of bits to read
     * @return integer value of the mask
     */
    private static int getMaskAsInt(int nrBits) {
        return 0xFFFFFFFF >>> (32 - nrBits);
    }

    /**
     * Return an integral mask with a given number of 1's at least significant bit positions.
     *
     * @param nrBits number of bits to read
     * @return long value of the mask
     */
    private static long getMaskAsLong(int nrBits) {
        return 0xFFFFFFFFFFFFFFFFL >>> (64 - nrBits);
    }

    /**
     * Calculates the value represented by the given bits.
     *
     * @param bitPos        position of the first bit to read in the bit buffer
     * @param nrBits        number of bits to read
     * @param endian        order of reading bytes (either Endian.Big or Endian.Little)
     * @param maxNrBitsRead maximum number of bits allowed to read, based on the method return type
     * @return the integer value represented by the given bits
     */
    private int getResultAsInt(long bitPos, int nrBits, ByteOrder endian, int maxNrBitsRead) {
        validateInputParams(bitPos, nrBits, maxNrBitsRead);
        int nrReadBytes = getNrNecessaryBytes(bitPos, nrBits);
        int numberBuf = getNumberBufAsInt(endian, nrReadBytes, (int) (bitPos >> 3));
        int mask = getMaskAsInt(nrBits);
        int result = mask & getRightShiftedNumberBufAsInt(numberBuf, bitPos, nrBits, endian);
        this.bitPos = bitPos + nrBits;
        return result;
    }

    /**
     * Calculates the value represented by the given bits.
     *
     * @param bitPos        position of the first bit to read in the bit buffer
     * @param nrBits        number of bits to read
     * @param endian        order of reading bytes (either Endian.Big or Endian.Little)
     * @param maxNrBitsRead maximum number of bits allowed to read, based on the method return type
     * @return the long value represented by the given bits
     */
    private long getResultAsLong(long bitPos, int nrBits, ByteOrder endian, int maxNrBitsRead) {
        validateInputParams(bitPos, nrBits, maxNrBitsRead);
        int nrReadBytes = getNrNecessaryBytes(bitPos, nrBits);
        long numberBuf = getNumberBufAsLong(endian, nrReadBytes, (int) (bitPos >> 3));
        long mask = getMaskAsLong(nrBits);
        long result = mask & getRightShiftedNumberBufAsLong(numberBuf, bitPos, nrBits, endian);
        this.bitPos = bitPos + nrBits;
        return result;
    }

    /**
     * Getter for inputByteBuf.
     *
     * @return Returns the inputByteBuf.
     */
    protected ByteBuffer getInputByteBuf() {
        return byteBuffer;
    }

    public BitBuffer slice(long length) {
        BitBuffer result = new SlicedBitBuffer(duplicate(), length);
        setBitPos(getBitPos() + length);
        return result;
    }

    public BitBuffer duplicate() {
        return new DefaultBitBuffer(byteBuffer.duplicate(), bitBufBitSize, bitPos);
    }

    public ByteBuffer readAsByteBuffer(int length) throws BitBufferUnderflowException {
        if ((this.bitPos % 8) != 0) {
            throw new BitBufferException("8-bit alignemnt exception. Bit possition (" + bitPos + ") should be 8-bit aligned");
        }
        int bitsToRead = length << 3;
        if (getBitPos() + bitsToRead > getBitBufBitSize()) {
            throw new BitBufferUnderflowException(getBitPos(), bitsToRead);
        }
        int sliceStartPosition = (int) (this.bitPos >>> 3);
        ByteBuffer slicedByteBuffer = this.slice(byteBuffer, sliceStartPosition, length);
        this.bitPos = bitPos + bitsToRead;
        return slicedByteBuffer;
    }

    /**
     * Work around that allows creation of a sub-view of a larger byte buffer.
     * <p/>
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5071718
     *
     * @param byteBuffer    - Original {@link ByteBuffer} to be sliced
     * @param slicePosition - Start position of the slice (e.g. sub-view) in the byte buffer
     * @param length        - Length of the slice (e.g. sub-view) in bytes, measured from the positions
     * @return Returns the sliced {@link ByteBuffer}. Original buffer is left in its original state;
     */
    private ByteBuffer slice(ByteBuffer byteBuffer, int slicePosition, int length) {
        int currentBufferPosition = this.byteBuffer.position();
        int currentBufferLimit = this.byteBuffer.limit();
        this.byteBuffer.position(slicePosition).limit(slicePosition + length);
        ByteBuffer slicedByteBuffer = this.byteBuffer.slice();
        this.byteBuffer.position(currentBufferPosition).limit(currentBufferLimit);
        return slicedByteBuffer;
    }

    public long getActualBitPos() {
        return bitPos;
    }
}
