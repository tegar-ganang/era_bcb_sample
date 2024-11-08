package simtools.util;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A memory buffer to be filled by one writer and read by several readers
 * The readers do not empty the buffer, they acces data in it using an index
 * The writer is filling the circular buffer it writes at a given index and increments
 * a minimum index value to prevent access from the readers on overwritten data due
 * to the cyclic buffer. Thus the writer is never constrained by the readers. The
 * readers have to be fast enough to read the buffer. The buffer is allocated
 * into native memory and thus can be quite large.
 * This implementation is based on the assumption that packets size has a maximum value
 * and that the minimum value is not too much different from the maximum value.
 * If not efficient in terms of memory usage, it is assumed that a subclass will manage it.
 *
 * @author cazenave_c
 *
 */
public class StreamMemoryBuffer {

    static Logger logger = simtools.util.LogConfigurator.getLogger(StreamMemoryBuffer.class.getName());

    /**
	 * The memory circular buffer
	 */
    protected ByteBuffer buffer;

    /**
	 * A circular array with packets length
	 */
    protected int[] lengths;

    /**
	 * The size of the circular buffer= number of packets in memory
	 */
    protected final long bufferSize;

    /**
	 * The maximum size for one packet
	 */
    protected final int streamMaxSize;

    /**
	 * The index of the next packet to be written
	 */
    protected long index;

    /**
	 * The last availble packet index (-1 at the beginning)
	 */
    protected long minIndex;

    /**
	 * The numbe of times the circular buffer is filled (starting at 1)
	 */
    protected int counter;

    /**
	 * A syncho to allow readers to synchrosize on the buffer updates
	 */
    protected Object synchro = new Object();

    /**
	 * Create a new memory buffer
	 * @param bufferSize the number of packets to store
	 * @param streamMaxSize the max size for one packet
	 */
    public StreamMemoryBuffer(int bufferSize, int streamMaxSize) {
        this.bufferSize = bufferSize;
        this.streamMaxSize = streamMaxSize;
        lengths = new int[bufferSize];
        logger.config("New StreamMemory buffer to size with bufferSize * streamMaxSize = (" + bufferSize + "*" + streamMaxSize + ")=" + (bufferSize * (streamMaxSize)));
        buffer = ByteBuffer.allocate(bufferSize * (streamMaxSize));
        index = 0;
        minIndex = -1;
        counter = 0;
    }

    /**
	 * Write a packet into the buffer
	 * @param b The array from which bytes are to be read
	 * @param offset The offset within the array of the first byte to be read
	 * @param length The number of bytes to be read from the given array
	 * @return the written packet index
	 */
    public long write(byte[] b, int offset, int length) {
        if (length > streamMaxSize) {
            throw new IllegalArgumentException("" + length + ">" + streamMaxSize);
        }
        if ((index - counter * bufferSize) == 0) {
            buffer.position(0);
            counter++;
        }
        if (counter > 1) {
            minIndex++;
        }
        lengths[(int) (index - (counter - 1) * bufferSize)] = length;
        buffer.put(b, offset, length);
        if (length < streamMaxSize) {
            buffer.position(buffer.position() + streamMaxSize - length);
        }
        synchronized (synchro) {
            index++;
            synchro.notifyAll();
        }
        return index - 1;
    }

    /**
	 * Write a byte buffer into the buffer
	 * @param b the byte buffer
	 * @return the written packet index
	 */
    public long write(ByteBuffer b) {
        int length = b.remaining();
        if (length > streamMaxSize) {
            throw new IllegalArgumentException("" + length + ">" + streamMaxSize);
        }
        if ((index - counter * bufferSize) == 0) {
            buffer.position(0);
            counter++;
        }
        if (counter > 1) {
            minIndex++;
        }
        lengths[(int) (index - (counter - 1) * bufferSize)] = length;
        buffer.put(b);
        if (length < streamMaxSize) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Set position to : (position + streamMaxSize - length)=" + "(" + buffer.position() + "+" + streamMaxSize + "-" + length + ")=" + (buffer.position() + streamMaxSize - length));
            }
            buffer.position(buffer.position() + streamMaxSize - length);
        }
        synchronized (synchro) {
            index++;
            synchro.notifyAll();
        }
        return index - 1;
    }

    /**
	 * Create a reader on this buffer
	 * This method is overwritten to implement specific readers
	 * @return the new reader
	 */
    public StreamReader createReader() {
        return new StreamReader();
    }

    /**
	 * A buffer reader
	 */
    public class StreamReader {

        /**
		 * The read buffer : an independent view on the write buffer
		 */
        protected final ByteBuffer readBuffer;

        /**
		 * The mark used to perform the writing of the buffer into a channel or
		 * into packet buffers
		 */
        protected long mark;

        ArrayList[] _validPacketList;

        /**
		 * Create a reader
		 */
        protected StreamReader() {
            readBuffer = buffer.asReadOnlyBuffer();
            mark = minIndex;
            _validPacketList = new ArrayList[(int) bufferSize];
        }

        /**
		 * Wait for the next writing on the buffer and return the index of
		 * this packet. The index is equal to previous index if timeout ellapses.
		 * The retuned index is negative if nothing to read or in case of
		 * thread interruption.
		 * @param timeout the wait timeout
		 * @return the index of the last packet to read
		 */
        public long getNextReadIndex(long timeout, long lastReadIndex) {
            synchronized (synchro) {
                if (lastReadIndex < (index - 1)) {
                    return index - 1;
                }
                try {
                    synchro.wait(timeout);
                } catch (InterruptedException e) {
                    return -1;
                }
                return index - 1;
            }
        }

        /**
		 * Set the mark location equal to the last received packet
		 */
        public void resetMark() {
            mark = getReadIndex();
        }

        /**
		 * Set the mark location wrt the oldest packet to read
		 * The mark is initialized with last available index at reader
		 * creation time.
		 * To start saving while a unknown number of packets is already
		 * written the mark can be set with a margin relative to
		 * the oldest packet to read index. For instance margin can be
		 * equal to bufferSize/2
		 * @param margin the mark location relative to the oldest available packet
		 */
        public void resetMark(int margin) {
            mark = minIndex + margin;
        }

        /**
		 * Bufferize the buffer content from the mark to packet buffer
		 * @param a packet factory to address the right buffers according
		 * to the key read from the packet
		 * @param timeout the timeout to wait
		 * @return the number of written bytes
		 * @throws IOException
		 * @throws PacketBufferValidationError
		 */
        public long bufferize(PacketBufferFactory factory, long timeout) throws IOException, PacketBufferValidationError {
            long result = 0;
            long nextReadIndex = getNextReadIndex(timeout, mark);
            if (mark > nextReadIndex) {
                return result;
            }
            if (mark < minIndex) {
                throw new BufferUnderflowException();
            }
            int firstPacketToBeReadIndex;
            if (mark < 0) {
                mark = -1;
            }
            firstPacketToBeReadIndex = (int) ((mark + 1) % bufferSize);
            int numberOfPacketToRead = (int) (nextReadIndex - mark);
            for (int i = 0; i < numberOfPacketToRead; i++) {
                int currentCircularIndex = (firstPacketToBeReadIndex + i) % (int) bufferSize;
                readBuffer.limit(currentCircularIndex * streamMaxSize + lengths[currentCircularIndex]);
                readBuffer.position(currentCircularIndex * streamMaxSize);
                ByteBuffer bb = readBuffer.slice();
                try {
                    ArrayList validPackets = null;
                    if ((mark + 1 + i) >= bufferSize) {
                        validPackets = (ArrayList) _validPacketList[currentCircularIndex];
                        for (Iterator iterator = validPackets.iterator(); iterator.hasNext(); ) {
                            ValidPacket vp = (ValidPacket) iterator.next();
                            PacketBuffer pb = vp.getPacketBuffer();
                            if (pb != null) {
                                if (pb.obsoleteIndex < vp.getIndex()) {
                                    pb.obsoleteIndex = vp.getIndex();
                                }
                            }
                        }
                        validPackets.clear();
                    } else {
                        validPackets = new ArrayList();
                    }
                    factory.add(bb, true, null, validPackets);
                    _validPacketList[currentCircularIndex] = validPackets;
                } catch (PacketBufferValidationError e) {
                    validationErrorControl(e, nextReadIndex);
                }
                result += lengths[currentCircularIndex];
            }
            if (mark < minIndex) {
                throw new BufferUnderflowException();
            }
            mark = nextReadIndex;
            return result;
        }

        /**
		 * Default management of a packet validation error is to send the
		 * exception at upper level
		 *
		 * @param e the validation error
		 * @throws PacketBufferValidationError
		 */
        protected void validationErrorControl(PacketBufferValidationError e, long nextReadIndex) throws PacketBufferValidationError {
            mark = nextReadIndex;
            throw e;
        }

        /**
		 * Save the buffer content from the mark to the last written packet
		 * @param channel the channel to write into
		 * @param timeout the timeout to wait
		 * @return the number of written bytes
		 * @throws IOException
		 */
        public long save(ByteChannel channel, long timeout) throws IOException {
            long res = 0;
            long r = getNextReadIndex(timeout, mark);
            if (mark > r) {
                return res;
            }
            if (mark < minIndex) {
                throw new BufferUnderflowException();
            }
            int k;
            if (mark < 0) {
                k = 0;
            } else {
                k = (int) (mark % bufferSize);
            }
            int c = (int) (r - mark);
            for (int i = 0; i < c; i++) {
                int x = k + i;
                if (x == bufferSize) x = 0;
                readBuffer.limit(x * streamMaxSize + lengths[x]);
                readBuffer.position(x * streamMaxSize);
                channel.write(readBuffer);
                res += lengths[x];
            }
            if (mark < minIndex) {
                throw new BufferUnderflowException();
            }
            mark = r;
            return res;
        }

        /**
		 * Read a packet
		 * @param readIndex the index of the packet to read
		 * @param readOffset the offset inside this packet
		 * @param readLength the number of bytes to read or
		 * -1 if it is up to the packet end
		 * @param b the array to which bytes are to be written
		 * @param offset the offset within the array of the first byte to be written
		 * @return the number of bytes read
		 */
        public int read(long readIndex, int readOffset, int readLength, byte[] b, int offset) {
            if (readIndex < minIndex) {
                throw new BufferUnderflowException();
            }
            if (readIndex >= index) {
                throw new BufferOverflowException();
            }
            int k = (int) (readIndex % bufferSize);
            readBuffer.position(k * streamMaxSize + readOffset);
            int l = lengths[k];
            if (readLength < 0) {
                l -= readOffset;
                readBuffer.get(b, offset, l);
            } else {
                if (l - readOffset < readLength) {
                    throw new BufferOverflowException();
                }
                l = readLength;
                readBuffer.get(b, offset, l);
            }
            if (readIndex < minIndex) {
                throw new BufferUnderflowException();
            }
            return l;
        }

        /**
		 * Get the packet length
		 * @param readIndex the index of the packet
		 * @return the number of bytes in this packet
		 */
        public int getLength(int readIndex) {
            if (readIndex < minIndex) {
                throw new BufferUnderflowException();
            }
            if (readIndex >= index) {
                throw new BufferOverflowException();
            }
            int l = lengths[(int) (readIndex % bufferSize)];
            if (readIndex < minIndex) {
                throw new BufferUnderflowException();
            }
            return l;
        }

        /**
		 * Get the last packet index
		 * @return the index
		 */
        public long getReadIndex() {
            return index - 1;
        }

        /**
		 * Get the oldest packet index
		 * @return the index
		 */
        public long getMinIndex() {
            return minIndex;
        }
    }

    /**
     * Clean the StreamMemoryBuffer
     */
    public void clean() {
        lengths = null;
        buffer = null;
        synchro = null;
    }
}
