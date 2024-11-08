package cunei.bits;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import cunei.util.Log;

public class BitBuffer implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final int BUFFER_SIZE = Integer.MAX_VALUE - 8;

    private final long length;

    private transient ByteBuffer[] buffers;

    protected BitBuffer(ByteBuffer[] buffer, long length) {
        this.buffers = buffer;
        this.length = length;
    }

    public BitBuffer(long length) {
        this.length = length;
        allocate(8 + (length - 1) / 8);
    }

    private void allocate(long bytes) {
        int size = (int) (1 + (bytes - 1) / BUFFER_SIZE);
        buffers = new ByteBuffer[size--];
        buffers[size] = ByteBuffer.allocateDirect((int) (bytes % BUFFER_SIZE) + 7);
        while (size-- > 0) buffers[size] = ByteBuffer.allocateDirect(BUFFER_SIZE + 7);
        for (ByteBuffer buffer : buffers) buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public long getBits(long startBit, long mask) {
        assert startBit >= 0;
        if (mask == 0L) return 0L;
        assert mask == 0xFFFFFFFFFFFFFFFFL && startBit % 64 == 0 || mask <= 0x01FFFFFFFFFFFFFFL;
        return getLong(startBit >>> 3) >>> (startBit & 7) & mask;
    }

    private final long getLong(long startByte) {
        assert buffers.length != 0;
        if (buffers.length == 1) {
            assert startByte < BUFFER_SIZE;
            return buffers[0].getLong((int) startByte);
        } else {
            final ByteBuffer buffer = buffers[(int) (startByte / BUFFER_SIZE)];
            return buffer.getLong((int) (startByte % BUFFER_SIZE));
        }
    }

    public final long length() {
        return length;
    }

    protected final void load(File file) {
        try {
            final FileChannel channel = new FileInputStream(file).getChannel();
            final long bytes = channel.size();
            int size = (int) (1 + (bytes - 1) / (BUFFER_SIZE + 7));
            if (Bits.CFG_BITS_MAP.getValue()) {
                buffers = new ByteBuffer[size--];
                buffers[size] = channel.map(FileChannel.MapMode.READ_ONLY, size * (BUFFER_SIZE + 7L), bytes % (BUFFER_SIZE + 7L));
                while (size-- > 0) buffers[size] = channel.map(FileChannel.MapMode.READ_ONLY, size * (BUFFER_SIZE + 7L), BUFFER_SIZE + 7L);
                for (ByteBuffer buffer : buffers) buffer.order(ByteOrder.LITTLE_ENDIAN);
            } else {
                allocate(bytes - 7l * size);
                channel.read(buffers);
            }
            channel.close();
        } catch (FileNotFoundException e) {
            Log.getInstance().severe("File " + file + " not found: " + e.getMessage());
        } catch (IOException e) {
            Log.getInstance().severe("IO Exception on " + file + ": " + e.getMessage());
        }
    }

    public void load(String path) {
        throw new RuntimeException("Attempted to load from class that was not memory-mapped");
    }

    private final void putLong(long startByte, long value) {
        assert buffers.length != 0;
        if (buffers.length == 1) {
            assert startByte < BUFFER_SIZE;
            buffers[0].putLong((int) startByte, value);
        } else {
            final ByteBuffer buffer = buffers[(int) (startByte / BUFFER_SIZE)];
            buffer.putLong((int) (startByte % BUFFER_SIZE), value);
        }
    }

    public void remove(String path) {
        throw new RuntimeException("Attempted to remove in class that was not memory-mapped");
    }

    public final MemoryMappedBitBuffer save(String path, String file) {
        setReadOnly();
        try {
            FileChannel channel = new FileOutputStream(new File(path, file)).getChannel();
            for (ByteBuffer buffer : buffers) {
                buffer.rewind();
                channel.write(buffer);
            }
            channel.close();
        } catch (FileNotFoundException e) {
            Log.getInstance().severe("File " + file + " not found: " + e.getMessage());
        } catch (IOException e) {
            Log.getInstance().severe("IO Exception on " + file + ": " + e.getMessage());
        }
        return new MemoryMappedBitBuffer(buffers, length, file);
    }

    public final void setBits(long startBit, BitBuffer other) {
        long loc = other.length();
        int rem = (int) (loc % 57);
        if (rem > 0) {
            loc -= rem;
            long mask = Bits.getMask(rem);
            setBits(startBit + loc, mask, other.getBits(loc, mask));
        }
        while (loc > 0) {
            loc -= 57;
            setBits(startBit + loc, 0x01FFFFFFFFFFFFFFL, other.getBits(loc, 0x01FFFFFFFFFFFFFFL));
        }
    }

    public void setBits(long startBit, long mask, long bitValue) {
        assert bitValue == (bitValue & mask) && startBit >= 0 && mask != 0L && (mask == -1L && startBit % 64 == 0 || mask <= 0x01FFFFFFFFFFFFFFL);
        long startByte = startBit / 8;
        int offset = (int) (startBit % 8);
        putLong(startByte, getLong(startByte) & ~(mask << offset) | (bitValue & mask) << offset);
    }

    private final void setReadOnly() {
        for (int i = 0; i < buffers.length; i++) {
            final ByteBuffer buffer = buffers[i];
            if (!buffer.isReadOnly()) buffers[i] = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    public BitBuffer sub(long startBit, long bitLength) {
        assert startBit >= 0 && bitLength > 0 && bitLength <= length;
        setReadOnly();
        return new SubBitBuffer(buffers, startBit, bitLength);
    }

    public String toString() {
        return "{BITBUF " + length + "}";
    }
}
