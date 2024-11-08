package com.wozgonon.eventstore.mmap;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.IntBuffer;
import com.wozgonon.docustrate.DesignPattern;
import com.wozgonon.docustrate.WorkInProgress;
import com.wozgonon.math.IntBufferMatrix;

/**
 * The purpose of this class is to provide a highly efficient persistent event store.
 * using a memory mapped buffer.  
 */
@WorkInProgress("TODO implement http://en.wikipedia.org/wiki/Builder_pattern")
@DesignPattern(url = "http://en.wikipedia.org/wiki/Builder_pattern", usage = "The event store contains a heirarchy of matrices, but the implementation of those matrices can vary." + "So the construction steps are abstracted into one Builder class with separate ConcreteBuilder classes for each representation.")
public class MmapEventStore {

    private final MappedByteBuffer mappedBuffer;

    private final IntBuffer buffer;

    public MmapEventStore(String fileName) throws IOException {
        final File file = new File(fileName);
        final RandomAccessFile raFile = new RandomAccessFile(file, "rw");
        final FileChannel channel = raFile.getChannel();
        this.mappedBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, (int) channel.size());
        this.buffer = this.mappedBuffer.asIntBuffer();
        IntBufferMatrix matrix = new IntBufferMatrix(this.buffer, (short) 10, (short) 10);
    }
}
