package nl.weeaboo.ogg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileOggInput implements RandomOggInput {

    private FileInputStream fin;

    private FileChannel channel;

    private InputStreamView current;

    public FileOggInput(File file) throws FileNotFoundException {
        this(new FileInputStream(file));
    }

    public FileOggInput(FileInputStream fin) throws FileNotFoundException {
        this.fin = fin;
        this.channel = fin.getChannel();
    }

    protected void closeCurrent() {
        if (current != null) {
            current.close();
            current = null;
        }
    }

    @Override
    public void close() throws IOException {
        closeCurrent();
        fin.close();
    }

    @Override
    public InputStream openStream() throws IOException {
        return openStream(0, length());
    }

    @Override
    public InputStream openStream(long off, long len) throws IOException {
        closeCurrent();
        channel.position(off);
        current = new InputStreamView(fin, len);
        return current;
    }

    @Override
    public int read(byte[] b, int off, long foff, int len) throws IOException {
        closeCurrent();
        ByteBuffer buf = ByteBuffer.wrap(b, off, len);
        channel.position(foff);
        return channel.read(buf);
    }

    @Override
    public boolean isReadSlow() {
        return false;
    }

    @Override
    public boolean isSeekSlow() {
        return false;
    }

    @Override
    public long length() throws IOException {
        return channel.size();
    }
}
