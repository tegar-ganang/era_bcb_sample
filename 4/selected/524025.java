package be.abeel.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 
 * @author jrobinso
 *  * @author Thomas Abeel
 */
public class SeekableFileStream extends SeekableStream {

    private File file;

    private FileInputStream fis;

    public SeekableFileStream(File file) throws FileNotFoundException {
        this.file = file;
        fis = new FileInputStream(file);
    }

    public boolean eof() throws IOException {
        return file.length() == fis.getChannel().position();
    }

    public void seek(long position) throws IOException {
        fis.getChannel().position(position);
    }

    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (length < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
        while (n < length) {
            int count = fis.read(buffer, offset + n, length - n);
            if (count < 0) {
                return 0;
            }
            n += count;
        }
        return n;
    }

    public void close() throws IOException {
        fis.close();
    }

    public byte[] readBytes(long position, int nBytes) throws IOException {
        seek(position);
        byte[] buffer = new byte[nBytes];
        read(buffer, 0, nBytes);
        return buffer;
    }

    @Override
    public SeekableStream stream() {
        try {
            return new SeekableFileStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int available() throws IOException {
        return fis.available();
    }

    @Override
    public String toString() {
        return file.toString();
    }

    @Override
    protected long position() throws IOException {
        return fis.getChannel().position();
    }

    public void finalize() {
        try {
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
