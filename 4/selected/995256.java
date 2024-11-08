package org.llama.jmex.terra.format;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

public class MemoryMappedFileMapStore extends FileMapStore {

    private boolean useTempFile;

    public MemoryMappedFileMapStore(String filename, boolean includeCoordinates, boolean useTempFile, boolean readOnly) {
        super(filename, includeCoordinates);
        init(useTempFile, readOnly);
    }

    public MemoryMappedFileMapStore(String filename, boolean includeCoordinates, String extention, boolean useTempFile, boolean readOnly) {
        super(filename, includeCoordinates, extention);
        init(useTempFile, readOnly);
    }

    private String filemode;

    private MapMode mapmode;

    private void init(boolean useTempFile, boolean readOnly) {
        this.useTempFile = useTempFile;
        if (readOnly) {
            filemode = "r";
            mapmode = MapMode.READ_ONLY;
        } else {
            filemode = "rw";
            mapmode = MapMode.READ_WRITE;
        }
    }

    public ByteBuffer loadMap(int x, int y) throws IOException {
        File file = getFile(x, y, false);
        return mapFile(file, -1);
    }

    protected ByteBuffer mapFile(File file, int length) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, filemode);
        length = prepFile(raf, length);
        FileChannel channel = raf.getChannel();
        ByteBuffer ret = channel.map(mapmode, 4, length);
        channel.close();
        raf.close();
        return ret;
    }

    protected int prepFile(RandomAccessFile raf, int size) throws IOException {
        if (raf.length() - 4 < size) {
            raf.setLength(4 + size);
            raf.seek(0);
            raf.writeInt(size);
        } else if (raf.length() - 4 > size) {
            raf.seek(0);
            return raf.readInt();
        }
        return size;
    }

    protected File getFile(int x, int y, boolean create) throws IOException {
        File file = super.getFile(x, y, create);
        if (useTempFile) {
            file.deleteOnExit();
        }
        return file;
    }

    public ByteBuffer createMap(int x, int y, int size) throws IOException {
        File file = getFile(x, y, true);
        return mapFile(file, size);
    }

    public void saveMap(int x, int y, ByteBuffer buf) throws IOException {
        ((MappedByteBuffer) buf).force();
    }
}
