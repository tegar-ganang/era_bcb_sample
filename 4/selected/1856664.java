package org.bing.adapter.com.caucho.hessian.mux;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Output stream to a specific channel.
 */
public class MuxOutputStream extends OutputStream {

    private MuxServer server;

    private int channel;

    private OutputStream os;

    /**
   * Null argument constructor.
   */
    public MuxOutputStream() {
    }

    /**
   * Initialize the multiplexor with input and output streams.
   */
    protected void init(MuxServer server, int channel) throws IOException {
        this.server = server;
        this.channel = channel;
        this.os = null;
    }

    /**
   * Gets the raw output stream.  Clients will normally not call
   * this.
   */
    protected OutputStream getOutputStream() throws IOException {
        if (os == null && server != null) os = server.writeChannel(channel);
        return os;
    }

    /**
   * Gets the channel of the connection.
   */
    public int getChannel() {
        return channel;
    }

    /**
   * Writes a URL to the stream.
   */
    public void writeURL(String url) throws IOException {
        writeUTF('U', url);
    }

    /**
   * Writes a data byte to the output stream.
   */
    public void write(int ch) throws IOException {
        OutputStream os = getOutputStream();
        os.write('D');
        os.write(0);
        os.write(1);
        os.write(ch);
    }

    /**
   * Writes data to the output stream.
   */
    public void write(byte[] buffer, int offset, int length) throws IOException {
        OutputStream os = getOutputStream();
        for (; length > 0x8000; length -= 0x8000) {
            os.write('D');
            os.write(0x80);
            os.write(0x00);
            os.write(buffer, offset, 0x8000);
            offset += 0x8000;
        }
        os.write('D');
        os.write(length >> 8);
        os.write(length);
        os.write(buffer, offset, length);
    }

    /**
   * Flush data to the output stream.
   */
    public void yield() throws IOException {
        OutputStream os = this.os;
        this.os = null;
        if (os != null) server.yield(channel);
    }

    /**
   * Flush data to the output stream.
   */
    public void flush() throws IOException {
        OutputStream os = this.os;
        this.os = null;
        if (os != null) server.flush(channel);
    }

    /**
   * Complete writing to the stream, closing the channel.
   */
    public void close() throws IOException {
        if (server != null) {
            OutputStream os = getOutputStream();
            this.os = null;
            MuxServer server = this.server;
            this.server = null;
            server.close(channel);
        }
    }

    /**
   * Writes a UTF-8 string.
   *
   * @param code the HMUX code identifying the string
   * @param string the string to write
   */
    protected void writeUTF(int code, String string) throws IOException {
        OutputStream os = getOutputStream();
        os.write(code);
        int charLength = string.length();
        int length = 0;
        for (int i = 0; i < charLength; i++) {
            char ch = string.charAt(i);
            if (ch < 0x80) length++; else if (ch < 0x800) length += 2; else length += 3;
        }
        os.write(length >> 8);
        os.write(length);
        for (int i = 0; i < length; i++) {
            char ch = string.charAt(i);
            if (ch < 0x80) os.write(ch); else if (ch < 0x800) {
                os.write(0xc0 + (ch >> 6) & 0x1f);
                os.write(0x80 + (ch & 0x3f));
            } else {
                os.write(0xe0 + (ch >> 12) & 0xf);
                os.write(0x80 + ((ch >> 6) & 0x3f));
                os.write(0x80 + (ch & 0x3f));
            }
        }
    }
}
