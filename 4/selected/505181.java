package de.carne.fs.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * FileScannerInput backed up by a file.
 */
class RootFileScannerInput extends FileScannerInput {

    private RandomAccessFile file;

    private final long fileSize;

    private final String fileName;

    /**
	 * Construct FileScannerInputFile.
	 * 
	 * @param scanner The FileScanner this instance belongs to.
	 * @param fileName The name of the file to open.
	 * @throws FileNotFoundException If the file cannot be accessed.
	 * @throws IOException If an I/O error occurs.
	 */
    public RootFileScannerInput(FileScanner scanner, String fileName) throws IOException {
        super(scanner, (new File(fileName)).getName());
        pushClosable(this.file = new RandomAccessFile(fileName, "r"));
        this.fileSize = this.file.getChannel().size();
        this.fileName = fileName;
    }

    /**
	 * Get the file name of the underlying file.
	 * 
	 * @return The file name of the underlying file.
	 */
    public String fileName() {
        return this.fileName;
    }

    @Override
    public long size() {
        return this.fileSize;
    }

    @Override
    public int readAt(long position, ByteBuffer buffer) throws IOException {
        final int startBufferPosition = buffer.position();
        try {
            this.file.getChannel().read(buffer, position);
        } catch (final IOException e) {
            recordIOError(e);
            throw e;
        }
        return buffer.position() - startBufferPosition;
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        return this.file.getChannel().transferTo(position, count, target);
    }
}
