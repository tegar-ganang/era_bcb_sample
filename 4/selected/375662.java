package org.metastatic.rsync;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

/**
 * A version of {@link RebuilderStream} that maps large files to memory
 * using the NIO API. Small files are not mapped and automatically use
 * the superclass's implementation.
 */
public class MappedRebuilderStream extends RebuilderStream {

    /**
    * The default lower bound for files to map.
    */
    public static final int MAP_LIMIT = 32768;

    /**
    * The size of the map. If not specified, the entire file is mapped.
    */
    protected long mapSize;

    /**
    * The lower bound file length to map; files smaller than this will
    * not be mapped.
    */
    protected long mapLimit;

    /**
    * The mapped file, if any.
    */
    protected MappedByteBuffer mappedFile;

    /**
    * The current offset in the file where the region is mapped.
    */
    protected long mapOffset;

    /**
    * Create a new memory mapped rebuilder, with the default map limit
    * and a maximum map size of {@link java.lang.Integer#MAX_VALUE}.
    */
    public MappedRebuilderStream() {
        this(Integer.MAX_VALUE, MAP_LIMIT);
    }

    /**
    * Create a new memory mapped rebuilder with the given map limit and
    * a maximum map size of {@link java.lang.Integer#MAX_VALUE}.
    *
    * @param mapLimit The smallest file size to map.
    */
    public MappedRebuilderStream(long mapLimit) {
        this(Integer.MAX_VALUE, mapLimit);
    }

    /**
    * Create a new memory mapped rebuilder with the given map limit and
    * maximum map size.
    *
    * @param mapSize  The maximum size of map to create.
    * @param mapLimit The smallest file size to map.
    */
    public MappedRebuilderStream(long mapSize, long mapLimit) {
        super();
        this.mapSize = mapSize;
        this.mapLimit = mapLimit;
    }

    public void setBasisFile(File file) throws IOException {
        super.setBasisFile(file);
        mappedFile = null;
        if (file != null && file.length() >= mapLimit) {
            mappedFile = basisFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, Math.min(mapSize, file.length()));
            mapOffset = 0;
        }
    }

    public void setBasisFile(String filename) throws IOException {
        super.setBasisFile(filename);
        mappedFile = null;
        if (basisFile != null && basisFile.length() >= mapLimit) {
            mappedFile = basisFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, Math.min(mapSize, basisFile.length()));
            mapOffset = 0;
        }
    }

    public void update(Delta delta) throws IOException, ListenerException {
        if (mappedFile == null) {
            super.update(delta);
        } else {
            RebuilderEvent event = null;
            if (delta instanceof DataBlock) {
                event = new RebuilderEvent(((DataBlock) delta).getData(), delta.getWriteOffset());
            } else {
                long offset = ((Offsets) delta).getOldOffset();
                if (offset + delta.getBlockLength() > mapOffset + mappedFile.capacity()) remapFile(offset);
                byte[] buf = new byte[delta.getBlockLength()];
                mappedFile.position((int) (offset - mapOffset));
                mappedFile.get(buf);
                event = new RebuilderEvent(buf, delta.getWriteOffset());
            }
            for (Iterator i = listeners.iterator(); i.hasNext(); ) ((RebuilderListener) i.next()).update(event);
        }
    }

    /**
    * Remap the file, if the read offset is not currently mapped.
    *
    * @param newOffset The new offset that needs to be read. The mapped
    *    region will have this offset in the middle of the buffer.
    */
    private void remapFile(long newOffset) throws IOException {
        long newLen = Math.min(mapSize, 2 * (basisFile.length() - newOffset));
        mapOffset = newOffset - (newLen / 2);
        if (mapOffset < 0) mapOffset = 0;
        mappedFile = basisFile.getChannel().map(FileChannel.MapMode.READ_ONLY, mapOffset, newLen);
    }
}
