package ultramc;

import java.nio.charset.Charset;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import ultramc.buffer.*;
import ultramc.connect.ServerConnection;

public class Operation<T extends Operation> {

    public static final String NOT_CALLED = "NOT_CALLED";

    public static final String ERROR = "ERROR";

    protected static final byte[] END_OF_LINE = new byte[2];

    protected static final byte[] END_OF_GET = new byte[5];

    protected static final Charset UTF8 = Charset.forName("UTF-8");

    protected MemCachedClient m_client;

    protected ValueTranscoder m_valueEncoder;

    protected KeyEncoder m_keyEncoder;

    protected long m_timeout;

    static {
        END_OF_LINE[0] = '\r';
        END_OF_LINE[1] = '\n';
        END_OF_GET[0] = 'E';
        END_OF_GET[1] = 'N';
        END_OF_GET[2] = 'D';
        END_OF_GET[3] = '\r';
        END_OF_GET[4] = '\n';
    }

    public Operation(MemCachedClient client) {
        m_client = client;
        m_keyEncoder = m_client.getDefaultKeyEncoder();
        m_timeout = client.getDefaultTimeout();
        m_valueEncoder = client.getDefaultValueTranscoder();
    }

    @SuppressWarnings("unchecked")
    public T setValueTranscoder(ValueTranscoder encoder) {
        m_valueEncoder = encoder;
        return ((T) this);
    }

    @SuppressWarnings("unchecked")
    public T setTimeout(long timeout) {
        m_timeout = timeout;
        return ((T) this);
    }

    static void writeToChannel(GatheringByteChannel channel, ByteBuffer[] buf, int bytesToWrite) throws IOException {
        int bytesWritten = 0;
        int offset = 0;
        while (bytesWritten < bytesToWrite) {
            while (buf[offset].position() == buf[offset].capacity()) offset++;
            bytesWritten += channel.write(buf, offset, (buf.length - offset));
        }
    }

    static void readResponse(ServerConnection sc, BufferSet bufferSet, long timeout, byte[] stop) throws IOException {
        boolean done = false;
        ByteBuffer buf = bufferSet.allocateBuffer();
        do {
            int keyCount = sc.getSelector().select(timeout);
            if (keyCount == 0) {
                sc.closeConnection();
                break;
            }
            Iterator<SelectionKey> it = sc.getSelector().selectedKeys().iterator();
            it.hasNext();
            it.next();
            it.remove();
            sc.getChannel().read(buf);
            if (buf.position() >= stop.length) {
                int checkStart = buf.position() - stop.length;
                int I;
                for (I = 0; I < stop.length; I++) {
                    byte b = buf.get(I + checkStart);
                    if (b != stop[I]) {
                        break;
                    }
                }
                if (I == stop.length) done = true;
            }
            if ((!done) && (buf.position() == buf.limit())) {
                buf.flip();
                buf = bufferSet.allocateBuffer();
            }
        } while (!done);
        buf.flip();
    }

    static String readLine(InputStream is) throws IOException {
        boolean foundR = false;
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = is.read()) != -1) {
            if (b == '\r') {
                foundR = true;
            } else if ((foundR) && (b == '\n')) {
                break;
            } else {
                sb.append((char) b);
            }
        }
        if (sb.length() == 0) return (null); else return (sb.toString());
    }
}
