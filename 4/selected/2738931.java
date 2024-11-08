package net.sourceforge.parser.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author aza_sf@yahoo.com
 *
 * @version $Revision: 28 $
 */
public class ByteStreamFileChannel extends ByteStream {

    private FileInputStream fis;

    private FileChannel channel;

    private long counter;

    public ByteStreamFileChannel(FileInputStream fis) {
        this.fis = fis;
        channel = this.fis.getChannel();
    }

    @Override
    public void close() throws IOException {
        channel.close();
        fis.close();
    }

    @Override
    public int read() throws IOException {
        counter++;
        ByteBuffer buf = ByteBuffer.allocate(1);
        if (channel.read(buf) == -1) return -1;
        return buf.get(0) & 0xff;
    }

    @Override
    public int read(byte[] b) throws IOException {
        counter += b.length;
        ByteBuffer buf = ByteBuffer.wrap(b);
        return channel.read(buf);
    }

    @Override
    public long skip(long n) throws IOException {
        long c = channel.position();
        channel.position(c + n);
        return channel.position() - c;
    }

    @Override
    public long getCounter() {
        return counter;
    }

    @Override
    public void resetCounter() {
        counter = 0;
    }

    @Override
    public void setCounter(int n) {
        counter = n;
    }

    @Override
    public void position(long n) throws IOException {
        channel.position(n);
    }

    @Override
    public long position() throws IOException {
        return channel.position();
    }

    public InputStream getInputStream() {
        return fis;
    }
}
