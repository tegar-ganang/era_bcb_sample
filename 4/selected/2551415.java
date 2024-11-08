package phex.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.ByteChannel;
import phex.common.address.DestAddress;
import phex.io.channels.StreamingByteChannel;
import phex.net.repres.SocketFacade;

public class DummySocketFacade implements SocketFacade {

    private ByteArrayInputStream inStream;

    private ByteArrayOutputStream outStream;

    private StreamingByteChannel channel;

    public DummySocketFacade(byte[] inStreamContent) {
        inStream = new ByteArrayInputStream(inStreamContent);
        outStream = new ByteArrayOutputStream();
    }

    public byte[] getOutData() {
        return outStream.toByteArray();
    }

    public void close() throws IOException {
        IOUtil.closeQuietly(inStream);
        IOUtil.closeQuietly(outStream);
    }

    public DestAddress getRemoteAddress() {
        return null;
    }

    public void setSoTimeout(int socketRWTimeout) throws SocketException {
    }

    public ByteChannel getChannel() throws IOException {
        if (channel == null) {
            channel = new StreamingByteChannel(inStream, outStream);
        }
        return channel;
    }
}
