package jaxlib.ee.jms;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Map;
import javax.annotation.Nullable;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;
import jaxlib.io.channel.IOChannels;

/**
 * @author  jw
 * @since   JaXLib 1.0
 * @version $Id: Jms.java 2773 2010-01-24 02:08:47Z joerg_wassmer $
 */
public class Jms extends Object {

    protected Jms() {
        super();
    }

    public static void copyProperties(final Message src, final Message dst) throws JMSException {
        if (src != dst) {
            for (final Enumeration<String> it = src.getPropertyNames(); it.hasMoreElements(); ) {
                final String name = it.nextElement();
                dst.setObjectProperty(name, src.getObjectProperty(name));
            }
        }
    }

    public static void copyProperties(final Message src, final Map<String, Object> dst) throws JMSException {
        for (final Enumeration<String> it = src.getPropertyNames(); it.hasMoreElements(); ) {
            final String name = it.nextElement();
            dst.put(name, src.getObjectProperty(name));
        }
    }

    public static void copyProperties(final Map<String, ?> src, final Message dst) throws JMSException {
        for (final Map.Entry<String, ?> e : src.entrySet()) dst.setObjectProperty(e.getKey(), e.getValue());
    }

    public static byte[] getBody(final BytesMessage msg) throws JMSException {
        final long len = msg.getBodyLength();
        if (len > Integer.MAX_VALUE) throw new MessageFormatException("body length exceeds Integer.MAX_VALUE");
        final byte[] buf = new byte[(int) len];
        if (msg.readBytes(buf) != len) throw new MessageEOFException("msg.readBytes() returned less bytes than reported by msg.getBodyLength()");
        return buf;
    }

    public static String getBody(final BytesMessage msg, final Charset charset) throws JMSException {
        final String s = new String(getBody(msg), charset);
        return s.isEmpty() ? "" : s;
    }

    public static String getBody(final BytesMessage msg, final String charsetName) throws JMSException, UnsupportedEncodingException {
        final String s = new String(getBody(msg), charsetName);
        return s.isEmpty() ? "" : s;
    }

    public static byte[] getAndClearBody(final BytesMessage msg) throws JMSException {
        final long len = msg.getBodyLength();
        if (len > Integer.MAX_VALUE) throw new MessageFormatException("body length exceeds Integer.MAX_VALUE");
        final byte[] buf = new byte[(int) len];
        if (msg.readBytes(buf) != len) throw new MessageEOFException("msg.readBytes() returned less bytes than reported by msg.getBodyLength()");
        msg.clearBody();
        return buf;
    }

    public static String getAndClearBody(final BytesMessage msg, final Charset charset) throws JMSException {
        final String s = new String(getAndClearBody(msg), charset);
        return s.isEmpty() ? "" : s;
    }

    public static String getAndClearBody(final BytesMessage msg, final String charsetName) throws JMSException, UnsupportedEncodingException {
        final String s = new String(getAndClearBody(msg), charsetName);
        return s.isEmpty() ? "" : s;
    }

    public static void transferBody(final BytesMessage msg, final OutputStream out) throws JMSException, IOException {
        final long len = msg.getBodyLength();
        if (len > 0) {
            final byte[] buf = new byte[(int) Math.min(8192, len)];
            for (int step; (step = msg.readBytes(buf)) > 0; ) out.write(buf, 0, step);
        }
    }

    public static void transferBody(final BytesMessage msg, final WritableByteChannel out) throws JMSException, IOException {
        final long len = msg.getBodyLength();
        if (len > 0) {
            final ByteBuffer buffer = ByteBuffer.allocate((int) Math.min(8192, len));
            for (int step; (step = msg.readBytes(buffer.array())) > 0; ) {
                buffer.limit(step).position(0);
                IOChannels.writeFully(out, buffer);
            }
        }
    }

    /**
   * Close the specified connection, ignore any {@link JMSException}.
   */
    public static void tryClose(@Nullable final Connection c) {
        try {
            if (c != null) c.close();
        } catch (final JMSException ex) {
        }
    }
}
