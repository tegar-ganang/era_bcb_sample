package cunei.bits;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import cunei.util.Log;

public abstract class ManagedNIOBuffer implements ManagedBuffer {

    private static final long serialVersionUID = 1L;

    protected transient ByteBuffer buffer;

    protected ManagedNIOBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public ManagedNIOBuffer(int bytes) {
        allocate(bytes);
    }

    private void allocate(int bytes) {
        buffer = ByteBuffer.allocateDirect(bytes);
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public ManagedBuffer compress() {
        if (!Bits.CFG_BITS_COMPRESS.getValue()) return this;
        long maxValue = 0;
        for (int pos = 0; pos < length(); pos++) {
            final long value = get(pos);
            if (value < 0) return this;
            maxValue = Math.max(maxValue, value);
        }
        ManagedBitBuffer result = new ManagedBitBuffer(length(), maxValue);
        for (int pos = 0; pos < length(); pos++) result.set(pos, get(pos));
        return result;
    }

    protected final void load(File file) {
        try {
            final FileChannel channel = new FileInputStream(file).getChannel();
            final long bytes = channel.size();
            if (Bits.CFG_BITS_MAP.getValue()) buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, bytes).order(ByteOrder.LITTLE_ENDIAN); else {
                allocate((int) bytes);
                channel.read(buffer);
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

    public void remove(String path) {
        throw new RuntimeException("Attempted to remove in class that was not memory-mapped");
    }

    protected final void save(File file) {
        buffer = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        buffer.rewind();
        try {
            FileChannel channel = new FileOutputStream(file).getChannel();
            channel.write(buffer);
            channel.close();
        } catch (FileNotFoundException e) {
            Log.getInstance().severe("File " + file + " not found: " + e.getMessage());
        } catch (IOException e) {
            Log.getInstance().severe("IO Exception on " + file + ": " + e.getMessage());
        }
    }
}
