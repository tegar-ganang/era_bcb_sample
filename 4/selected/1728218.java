package gnu.java.nio;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * A Writer implementation that works by wrapping an NIO channel.
 */
public class ChannelWriter extends Writer {

    private static final int DEFAULT_BUFFER_CAP = 8192;

    /**
   * The output channel.
   */
    private WritableByteChannel byteChannel;

    /**
   * The encoder to use.
   */
    private CharsetEncoder enc;

    /**
   * The byte buffer.  Translated characters are stored here on their way out.
   */
    private ByteBuffer byteBuffer;

    /**
   * The character buffer.  Characters are stored here on their way into
   * the encoder.
   */
    private CharBuffer charBuffer;

    private void writeBuffer() throws IOException {
        byteBuffer.flip();
        byteChannel.write(byteBuffer);
    }

    /**
   * Create a new instance, given the output byte channel, the encoder
   * to use, and the minimum buffer capacity.
   */
    public ChannelWriter(WritableByteChannel ch, CharsetEncoder enc, int minBufferCap) {
        this.byteChannel = ch;
        this.enc = enc;
        if (minBufferCap == -1) minBufferCap = DEFAULT_BUFFER_CAP;
        this.byteBuffer = ByteBuffer.allocate((int) (minBufferCap * enc.maxBytesPerChar()));
        this.charBuffer = CharBuffer.allocate(minBufferCap);
        this.charBuffer.clear();
    }

    public void flush() throws IOException {
    }

    public void close() throws IOException {
        synchronized (lock) {
            if (enc == null) throw new IOException("writer already closed");
            byteBuffer.clear();
            charBuffer.flip();
            CoderResult res = enc.encode(charBuffer, byteBuffer, true);
            if (res.isError() || res.isMalformed() || res.isUnmappable()) res.throwException();
            writeBuffer();
            byteBuffer.clear();
            res = enc.flush(byteBuffer);
            if (res.isError() || res.isMalformed() || res.isUnmappable()) res.throwException();
            writeBuffer();
            enc = null;
        }
    }

    public void write(char[] buf, int offset, int len) throws IOException {
        synchronized (lock) {
            if (enc == null) throw new IOException("writer already closed");
            int lastLen = -1;
            while (len > 0) {
                int allowed = Math.min(charBuffer.remaining(), len);
                charBuffer.put(buf, offset, allowed);
                offset += allowed;
                len -= allowed;
                charBuffer.flip();
                if (len == lastLen) {
                    if (len <= charBuffer.remaining()) {
                        charBuffer.put(buf, offset, len);
                        charBuffer.flip();
                    } else {
                        CharBuffer ncb = CharBuffer.allocate(charBuffer.length() + len);
                        ncb.put(charBuffer);
                        ncb.put(buf, offset, len);
                        charBuffer = ncb;
                    }
                    break;
                }
                lastLen = len;
                byteBuffer.clear();
                CoderResult res = enc.encode(charBuffer, byteBuffer, false);
                charBuffer.compact();
                if (res.isError() || res.isMalformed() || res.isUnmappable()) res.throwException();
                writeBuffer();
            }
        }
    }
}
