package jaxlib.ee.jms.serial;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotReadableException;
import javax.jms.MessageNotWriteableException;
import javax.jms.Session;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import jaxlib.io.stream.ByteBufferOutputStream;
import jaxlib.io.stream.XInputStream;
import jaxlib.lang.Bytes;
import jaxlib.lang.Ints;
import jaxlib.util.CheckArg;
import jaxlib.util.CheckBounds;

/**
 * @author  jw
 * @since   JaXLib 1.0
 * @version $Id: SerialBytesMessage.java 3055 2012-02-27 05:56:56Z joerg_wassmer $
 */
@XmlType(name = "BytesMessage", namespace = SerialMessage.XMLNS)
@XmlRootElement(name = "bytesMessage", namespace = SerialMessage.XMLNS)
@XmlAccessorType(XmlAccessType.NONE)
public class SerialBytesMessage extends SerialMessage implements BytesMessage {

    /**
   * @since JaXLib 1.0
   */
    private static final long serialVersionUID = 1L;

    private static JMSException exceptionOnRead(final IOException ex) {
        if (ex instanceof EOFException) return (MessageEOFException) new MessageEOFException("end of stream").initCause(ex);
        return (JMSException) new JMSException("i/o error").initCause(ex);
    }

    private static JMSException exceptionOnWrite(final IOException ex) {
        return (JMSException) new JMSException("i/o error").initCause(ex);
    }

    private final BytesBody body;

    private transient XInputStream in;

    private transient ByteBufferOutputStream out;

    public SerialBytesMessage() {
        super();
        this.body = new BytesBody();
    }

    public SerialBytesMessage(final BytesMessage src) throws JMSException {
        super(src);
        this.body = new BytesBody();
        src.reset();
        if (src instanceof SerialBytesMessage) this.body.setData(((SerialBytesMessage) src).body); else {
            byte[] data = new byte[Ints.cast(src.getBodyLength())];
            final int len = src.readBytes(data);
            if (len < data.length) data = Arrays.copyOf(data, len);
            this.body.setData(data);
        }
        src.reset();
    }

    private XInputStream beforeRead() throws JMSException {
        if (this.out != null) throw new MessageNotReadableException("can not read while writing, call reset() first");
        if (this.in == null) this.in = this.body.openInputStream();
        return this.in;
    }

    private ByteBufferOutputStream beforeWrite() throws JMSException {
        if (this.in != null) throw new MessageNotWriteableException("can not write while reading, call reset() first");
        if (this.out == null) {
            this.out = new ByteBufferOutputStream(ByteBuffer.wrap(this.body.getData()), 0.5f);
            this.out.getBuffer().position(this.out.getBuffer().capacity());
            this.body.setData(Bytes.EMPTY_ARRAY);
        }
        return this.out;
    }

    @XmlElement(name = "body")
    private BytesBody getBodyXml() {
        return this.body;
    }

    private void setBodyXml(final BytesBody v) {
        if (v != null) this.body.setData(v);
    }

    @Override
    public void clearBody() throws JMSException {
        this.body.setData(Bytes.EMPTY_ARRAY);
        this.in = null;
        this.out = null;
    }

    @Override
    public long getBodyLength() throws JMSException {
        return (this.out == null) ? this.body.getDataLength() : this.out.getBuffer().position();
    }

    public byte[] getBytes() throws JMSException {
        if (this.out != null) throw new MessageNotReadableException("can not read while writing, call reset() first");
        return this.body.getData();
    }

    public SerialMessageCompression getCompression() throws JMSException {
        return this.body.getCompression();
    }

    @Override
    public boolean readBoolean() throws JMSException {
        try {
            return beforeRead().readBoolean();
        } catch (final IOException ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public byte readByte() throws JMSException {
        try {
            return beforeRead().readByte();
        } catch (final IOException ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public int readBytes(final byte[] dest) throws JMSException {
        return readBytes(dest, dest.length);
    }

    @Override
    public int readBytes(final byte[] dest, final int length) throws JMSException {
        try {
            return beforeRead().read(dest, 0, length);
        } catch (final IOException ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public char readChar() throws JMSException {
        try {
            return beforeRead().readChar();
        } catch (final IOException ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public double readDouble() throws JMSException {
        try {
            return beforeRead().readDouble();
        } catch (final IOException ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public float readFloat() throws JMSException {
        try {
            return beforeRead().readFloat();
        } catch (final IOException ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public int readInt() throws JMSException {
        try {
            return beforeRead().readInt();
        } catch (final IOException ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public long readLong() throws JMSException {
        try {
            return beforeRead().readLong();
        } catch (final IOException ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public short readShort() throws JMSException {
        try {
            return beforeRead().readShort();
        } catch (final IOException ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public int readUnsignedByte() throws JMSException {
        try {
            return beforeRead().readUnsignedByte();
        } catch (final IOException ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public int readUnsignedShort() throws JMSException {
        try {
            return beforeRead().readUnsignedShort();
        } catch (final IOException ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public String readUTF() throws JMSException {
        try {
            return beforeRead().readUTF();
        } catch (final IOException ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public BytesMessage recreate(final Session session) throws JMSException {
        if (this.out != null) throw new MessageNotReadableException("can not recreate while writing");
        final BytesMessage msg = session.createBytesMessage();
        recreateProperties(session, msg);
        msg.writeBytes(this.body.getData());
        msg.reset();
        return msg;
    }

    @Override
    public void reset() throws JMSException {
        if (this.in != null) this.in = null;
        if (this.out != null) {
            try {
                this.body.setData(this.out.toByteArray());
            } catch (final IOException ex) {
                throw (JMSException) new JMSException("i/o error").initCause(ex);
            }
            this.out = null;
        }
    }

    public void setBytes(final byte[] v) throws JMSException {
        reset();
        this.body.setData(v);
    }

    public void setCompression(final SerialMessageCompression v) throws JMSException {
        if (this.out != null) throw new JMSException("can not set this property while writing");
        this.body.setCompression(v);
    }

    @Override
    public void writeBoolean(final boolean v) throws JMSException {
        try {
            beforeWrite().writeBoolean(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeByte(final byte v) throws JMSException {
        try {
            beforeWrite().writeByte(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeBytes(final byte[] src) throws JMSException {
        writeBytes(src, 0, src.length);
    }

    @Override
    public void writeBytes(final byte[] src, final int offs, final int len) throws JMSException {
        CheckBounds.offset(src, offs, len);
        try {
            beforeWrite().write(src, offs, len);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeChar(final char v) throws JMSException {
        try {
            beforeWrite().writeChar(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeDouble(final double v) throws JMSException {
        try {
            beforeWrite().writeDouble(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeFloat(final float v) throws JMSException {
        try {
            beforeWrite().writeFloat(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeInt(final int v) throws JMSException {
        try {
            beforeWrite().writeInt(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeLong(final long v) throws JMSException {
        try {
            beforeWrite().writeLong(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeObject(final Object v) throws JMSException {
        if (v instanceof String) writeUTF((String) v); else if (v instanceof byte[]) writeBytes((byte[]) v); else if (v instanceof Boolean) writeBoolean((Boolean) v); else if (v instanceof Byte) writeByte((Byte) v); else if (v instanceof Double) writeDouble((Double) v); else if (v instanceof Float) writeFloat((Float) v); else if (v instanceof Integer) writeInt((Integer) v); else if (v instanceof Long) writeLong((Long) v); else if (v instanceof Short) writeShort((Short) v); else if (v == null) throw new MessageFormatException("can not write null"); else throw new MessageFormatException("unsupported type: ".concat(v.getClass().toString()));
    }

    @Override
    public void writeShort(final short v) throws JMSException {
        try {
            beforeWrite().writeShort(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeUTF(final String v) throws JMSException {
        CheckArg.notNull(v, "value");
        try {
            beforeWrite().writeUTF(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }
}
