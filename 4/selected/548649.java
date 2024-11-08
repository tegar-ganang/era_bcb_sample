package jpar2.files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import jpar2.utility.Checksum;
import jpar2.utility.Crc32;
import jpar2.utility.FileInfo;
import jpar2.utility.Md5;

public class DiskFile {

    private FileInfo fileInfo;

    private boolean opened = false;

    private FileChannel fileChannel;

    public DiskFile(File file) {
        setFileInfo(file);
    }

    public void open() throws FileNotFoundException {
        open(FileAccessMode.ReadWrite);
    }

    public void open(FileAccessMode accessMode) throws FileNotFoundException {
        if (opened) return;
        fileChannel = new RandomAccessFile(fileInfo.getFile(), accessMode.getMode()).getChannel();
        opened = true;
    }

    public void close() throws IOException {
        if (!opened) return;
        opened = false;
        fileChannel.close();
    }

    public boolean isOpen() {
        return opened;
    }

    public ByteBuffer readData(long position, int length) throws IOException {
        return readData(position, length, false);
    }

    public ByteBuffer readData(long position, int length, boolean pad) throws IOException {
        if (!opened) throw new IllegalStateException("DiskFile, " + fileInfo + ", must be opened before reading data.");
        if (!exists()) throw new FileNotFoundException("DiskFile " + fileInfo + " does not exist.");
        ByteBuffer buffer = ByteBuffer.allocate(length);
        fileChannel.read(buffer, position);
        if (pad) {
            while (buffer.position() < buffer.capacity()) {
                buffer.put((byte) 0);
            }
        }
        buffer.rewind();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer;
    }

    public void writeData(ByteBuffer data, long position) throws IOException {
        if (!opened) throw new IllegalStateException("DiskFile, " + fileInfo + ", must be opened before writing data.");
        fileChannel.write(data, position);
    }

    public void setFileLength(double fileLength) throws IOException {
        if (!opened) throw new IllegalStateException("DiskFile, " + fileInfo + ", must be opened before setting file length.");
        if (fileLength == getFileLength()) {
            return;
        } else if (fileLength < getFileLength()) {
            fileChannel.truncate((long) fileLength);
        } else {
            ByteBuffer padding = ByteBuffer.allocate((int) (fileLength - getFileLength()));
            while (padding.position() < padding.capacity()) {
                padding.put((byte) 0);
            }
            writeData(padding, getFileLength());
        }
    }

    public Checksum calculateChecksumMd5(long offset, int length, boolean pad) throws IOException {
        Md5 checksum = new Md5();
        open();
        checksum.updateChecksum(readData(offset, length, pad));
        close();
        return checksum;
    }

    public Checksum calculateChecksumCrc32(long offset, int length, boolean pad) throws IOException {
        Crc32 checksum = new Crc32();
        open();
        checksum.updateChecksum(readData(offset, length, pad));
        close();
        return checksum;
    }

    private void setFileInfo(File file) {
        if (file == null) throw new NullPointerException("File cannot be null");
        setFileInfo(new FileInfo(file));
    }

    private void setFileInfo(FileInfo fileInfo) {
        if (fileInfo == null) throw new NullPointerException("FileInfo cannot be null");
        this.fileInfo = fileInfo;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public boolean exists() {
        return fileInfo.exists();
    }

    public long getFileLength() throws IOException {
        if (!opened) throw new IllegalStateException("DiskFile, " + fileInfo + ", must be opened before getting file length.");
        return fileChannel.size();
    }
}
