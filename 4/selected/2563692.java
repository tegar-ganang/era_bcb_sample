package sf2.io.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.LinkedList;
import java.util.List;
import sf2.io.MessageServer;
import sf2.io.StreamFuture;

public class StreamObjectInputStream extends ObjectInputStream {

    protected LinkedList<StreamFuture> futures = new LinkedList<StreamFuture>();

    protected ReadableByteChannel channel;

    protected MessageServer streamServer;

    protected ByteBuffer buffer;

    protected CharsetDecoder decoder;

    protected String prefix;

    protected StreamObjectInputStream() throws IOException, SecurityException {
        super();
        buffer = ByteBuffer.allocate(4096);
        Charset charset = Charset.forName("UTF-8");
        decoder = charset.newDecoder();
    }

    public StreamObjectInputStream(InputStream in, ReadableByteChannel channel, MessageServer streamServer, String prefix) throws IOException {
        super(in);
        this.channel = channel;
        this.streamServer = streamServer;
        this.prefix = prefix;
        buffer = ByteBuffer.allocate(4096);
        Charset charset = Charset.forName("UTF-8");
        decoder = charset.newDecoder();
    }

    public void addFuture(StreamFuture future) {
        futures.add(future);
    }

    public void readFutures() throws IOException {
        if (futures.size() > 0) {
            for (StreamFuture future : futures) future.transferTo(prefix);
            futures.clear();
        }
    }

    public ReadableByteChannel getChannel() {
        return channel;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public CharsetDecoder getDecoder() {
        return decoder;
    }
}
