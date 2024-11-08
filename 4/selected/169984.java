package jaxlib.ee.jms.serial;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import javax.jms.JMSException;
import javax.jms.MessageEOFException;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotReadableException;
import javax.jms.MessageNotWriteableException;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import jaxlib.io.stream.ByteBufferInputStream;
import jaxlib.io.stream.ByteBufferOutputStream;
import jaxlib.lang.Bytes;
import jaxlib.lang.Chars;
import jaxlib.util.CheckBounds;
import jaxlib.util.Strings;

/**
 * @author  jw
 * @since   JaXLib 1.0
 * @version $Id: SerialStreamMessage.java 3055 2012-02-27 05:56:56Z joerg_wassmer $
 */
@XmlType(name = "StreamMessage", namespace = SerialMessage.XMLNS)
@XmlRootElement(name = "streamMessage", namespace = SerialMessage.XMLNS)
@XmlAccessorType(XmlAccessType.NONE)
public class SerialStreamMessage extends SerialMessage implements StreamMessage {

    /**
   * @since JaXLib 1.0
   */
    private static final long serialVersionUID = 1L;

    private static final int EOF = -1;

    private static final int BOOLEAN = 1;

    private static final int BYTE = 2;

    private static final int CHAR = 3;

    private static final int DOUBLE = 4;

    private static final int FLOAT = 5;

    private static final int INT = 6;

    private static final int LONG = 7;

    private static final int SHORT = 8;

    private static final int STRING = 9;

    private static final int ARRAY = 10;

    private static final int NULL = 11;

    private static final String[] typeNames = { "boolean", "byte", "char", "double", "float", "int", "long", "short", "string", "byte[]", "null" };

    private static JMSException exceptionOnRead(final Exception ex) {
        if (ex instanceof JMSException) return (JMSException) ex;
        if (ex instanceof EOFException) return (MessageEOFException) new MessageEOFException("end of stream").initCause(ex);
        if (ex instanceof NumberFormatException) return (MessageFormatException) new MessageFormatException("invalid conversion").initCause(ex);
        return (JMSException) new JMSException("i/o error").initCause(ex);
    }

    private static JMSException exceptionOnWrite(final IOException ex) {
        return (JMSException) new JMSException("i/o error").initCause(ex);
    }

    private static JMSException invalidConversion(final int fromType, final int toType) {
        if (toType == EOF) return new MessageEOFException("end of stream");
        return new MessageFormatException(Strings.format("invalid conversion from %s to %s", SerialStreamMessage.typeNames[fromType], SerialStreamMessage.typeNames[toType]));
    }

    private transient byte[] data;

    private transient ByteBufferInputStream in;

    private transient ByteBufferOutputStream out;

    private transient int inArrayRemaining;

    private transient int inType;

    public SerialStreamMessage() {
        super();
        this.data = Bytes.EMPTY_ARRAY;
    }

    public SerialStreamMessage(final StreamMessage src) throws JMSException {
        super(src);
        src.reset();
        if (src instanceof SerialStreamMessage) this.data = ((SerialStreamMessage) src).data;
        if (this.data == null) {
            while (true) {
                final Object v;
                try {
                    v = src.readObject();
                } catch (final MessageEOFException ex) {
                    break;
                }
                writeObject(v);
            }
            if (this.out == null) this.data = Bytes.EMPTY_ARRAY; else reset();
        }
        src.reset();
    }

    /**
   * @serialData
   * @since JaXLib 1.0
   */
    private void readObject(final ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        this.data = new byte[in.readInt()];
        in.readFully(this.data);
    }

    /**
   * @serialData
   * @since JaXLib 1.0
   */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        byte[] data = this.data;
        if (data != null) {
            out.writeInt(data.length);
            out.write(data);
        } else {
            out.writeInt(this.out.getBuffer().position());
            out.write(this.out.getBuffer().array(), 0, this.out.getBuffer().position());
        }
    }

    private int beforeRead() throws JMSException {
        try {
            if (this.out != null) throw new MessageNotReadableException("can not read while writing, call reset() first");
            if (this.in == null) this.in = new ByteBufferInputStream(this.data);
            int t = this.inType;
            if ((t == ARRAY) && (this.inArrayRemaining <= 0)) this.inType = t = 0;
            if (t == 0) {
                t = this.in.read();
                this.inType = t;
                if (((t < BOOLEAN) || (t > NULL)) && (t != EOF)) throw new IOException("corrupted stream");
                if (t == ARRAY) {
                    this.inArrayRemaining = this.in.readInt();
                    if (this.inArrayRemaining < 0) throw new IOException("corrupted stream");
                }
            }
            return t;
        } catch (final IOException ex) {
            throw exceptionOnRead(ex);
        }
    }

    private ByteBufferOutputStream beforeWrite(final int type) throws JMSException {
        if (this.in != null) throw new MessageNotWriteableException("can not write while reading, call reset() first");
        if (this.out == null) {
            this.out = new ByteBufferOutputStream(ByteBuffer.wrap(this.data), 0.5f);
            this.out.getBuffer().position(this.out.getBuffer().capacity());
            this.data = null;
        }
        try {
            this.out.writeByte(type);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
        return this.out;
    }

    @XmlElement(name = "body", required = true)
    private byte[] getXmlBody() {
        if (this.data == null) throw new IllegalStateException("can not read while writing, call reset() first");
        return this.data;
    }

    private void setXmlBody(final byte[] v) {
        this.data = (v == null) ? Bytes.EMPTY_ARRAY : v;
    }

    @Override
    public void clearBody() throws JMSException {
        this.data = Bytes.EMPTY_ARRAY;
        this.in = null;
        this.out = null;
        this.inArrayRemaining = 0;
        this.inType = 0;
    }

    @Override
    public boolean readBoolean() throws JMSException {
        try {
            final int type = beforeRead();
            switch(type) {
                case BOOLEAN:
                    return this.in.readBoolean();
                case STRING:
                    return Boolean.valueOf(readString());
                default:
                    throw invalidConversion(type, BOOLEAN);
            }
        } catch (final Exception ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public byte readByte() throws JMSException {
        try {
            final int type = beforeRead();
            switch(type) {
                case BOOLEAN:
                case BYTE:
                    this.inType = 0;
                    return this.in.readByte();
                case STRING:
                    return Byte.parseByte(readString());
                default:
                    throw invalidConversion(type, BYTE);
            }
        } catch (final Exception ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public int readBytes(final byte[] dest) throws JMSException {
        try {
            final int type = beforeRead();
            switch(type) {
                case EOF:
                    return -1;
                case ARRAY:
                    {
                        if (this.inArrayRemaining <= 0) return -1;
                        final int count = this.in.read(dest, 0, Math.min(this.inArrayRemaining, dest.length));
                        if (count <= 0) throw new MessageEOFException("unexpected end of stream");
                        this.inArrayRemaining -= count;
                        if (this.inArrayRemaining <= 0) this.inType = 0;
                        return count;
                    }
                default:
                    throw invalidConversion(type, BYTE);
            }
        } catch (final Exception ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public char readChar() throws JMSException {
        try {
            final int type = beforeRead();
            switch(type) {
                case CHAR:
                    this.inType = 0;
                    return this.in.readChar();
                default:
                    throw invalidConversion(type, CHAR);
            }
        } catch (final Exception ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public double readDouble() throws JMSException {
        try {
            final int type = beforeRead();
            switch(type) {
                case DOUBLE:
                    this.inType = 0;
                    return this.in.readDouble();
                case FLOAT:
                    this.inType = 0;
                    return this.in.readFloat();
                case STRING:
                    return Double.parseDouble(readString());
                default:
                    throw invalidConversion(type, DOUBLE);
            }
        } catch (final Exception ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public float readFloat() throws JMSException {
        try {
            final int type = beforeRead();
            switch(type) {
                case FLOAT:
                    this.inType = 0;
                    return this.in.readFloat();
                case STRING:
                    return Float.parseFloat(readString());
                default:
                    throw invalidConversion(type, FLOAT);
            }
        } catch (final Exception ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public int readInt() throws JMSException {
        try {
            final int type = beforeRead();
            switch(type) {
                case BOOLEAN:
                case BYTE:
                    this.inType = 0;
                    return this.in.readByte();
                case INT:
                    this.inType = 0;
                    return this.in.readInt();
                case SHORT:
                    this.inType = 0;
                    return this.in.readShort();
                case STRING:
                    return Integer.parseInt(readString());
                default:
                    throw invalidConversion(type, INT);
            }
        } catch (final Exception ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public long readLong() throws JMSException {
        try {
            final int type = beforeRead();
            switch(type) {
                case BOOLEAN:
                case BYTE:
                    this.inType = 0;
                    return this.in.readByte();
                case INT:
                    this.inType = 0;
                    return this.in.readInt();
                case LONG:
                    this.inType = 0;
                    return this.in.readLong();
                case SHORT:
                    this.inType = 0;
                    return this.in.readShort();
                case STRING:
                    return Long.parseLong(readString());
                default:
                    throw invalidConversion(type, LONG);
            }
        } catch (final Exception ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public Object readObject() throws JMSException {
        try {
            final int type = beforeRead();
            switch(type) {
                case BOOLEAN:
                    return readBoolean();
                case BYTE:
                    return readByte();
                case CHAR:
                    return readChar();
                case DOUBLE:
                    return readDouble();
                case FLOAT:
                    return readFloat();
                case INT:
                    return readInt();
                case LONG:
                    return readLong();
                case SHORT:
                    return readShort();
                case STRING:
                    return readString();
                case NULL:
                    return null;
                case ARRAY:
                    {
                        final byte[] a = new byte[this.inArrayRemaining];
                        this.in.readFully(a);
                        this.inArrayRemaining = 0;
                        this.inType = 0;
                        return a;
                    }
                default:
                    throw new AssertionError(type);
            }
        } catch (final Exception ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public short readShort() throws JMSException {
        try {
            final int type = beforeRead();
            switch(type) {
                case BOOLEAN:
                case BYTE:
                    this.inType = 0;
                    return this.in.readByte();
                case SHORT:
                    this.inType = 0;
                    return this.in.readShort();
                case STRING:
                    return Short.parseShort(readString());
                default:
                    throw invalidConversion(type, LONG);
            }
        } catch (final Exception ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public String readString() throws JMSException {
        try {
            final int type = beforeRead();
            switch(type) {
                case STRING:
                    {
                        this.inType = 0;
                        final int len = this.in.readInt();
                        if (len < 0) throw new IOException("corrupted stream");
                        if (len == 0) return "";
                        if (len == 1) return Chars.toString(this.in.readChar());
                        final int pos = this.in.position();
                        this.in.skipFully(len);
                        return new String(this.data, pos, len, "UTF-16BE");
                    }
                case NULL:
                    return null;
                default:
                    {
                        final Object v = readObject();
                        return (v == null) ? null : v.toString();
                    }
            }
        } catch (final Exception ex) {
            throw exceptionOnRead(ex);
        }
    }

    @Override
    public StreamMessage recreate(final Session session) throws JMSException {
        if ((this.in != null) || (this.out != null)) throw new javax.jms.IllegalStateException("can not recreate while reading or writing, call reset() first");
        final StreamMessage msg = session.createStreamMessage();
        recreateProperties(session, msg);
        try {
            READ: while (true) {
                final int type = beforeRead();
                switch(type) {
                    case EOF:
                        break READ;
                    case BOOLEAN:
                        msg.writeBoolean(this.in.readBoolean());
                        break;
                    case BYTE:
                        msg.writeByte(this.in.readByte());
                        break;
                    case CHAR:
                        msg.writeChar(this.in.readChar());
                        break;
                    case DOUBLE:
                        msg.writeDouble(this.in.readDouble());
                        break;
                    case FLOAT:
                        msg.writeFloat(this.in.readFloat());
                        break;
                    case INT:
                        msg.writeInt(this.in.readInt());
                        break;
                    case LONG:
                        msg.writeLong(this.in.readLong());
                        break;
                    case SHORT:
                        msg.writeShort(this.in.readShort());
                        break;
                    case STRING:
                        msg.writeString(readString());
                        break;
                    case NULL:
                        msg.writeString(null);
                        break;
                    case ARRAY:
                        {
                            final byte[] a = new byte[this.inArrayRemaining];
                            this.in.readFully(a);
                            this.inArrayRemaining = 0;
                            msg.writeBytes(a);
                        }
                        break;
                    default:
                        throw new AssertionError(type);
                }
                this.inType = 0;
            }
            msg.reset();
            return msg;
        } catch (final IOException ex) {
            throw exceptionOnRead(ex);
        } finally {
            reset();
        }
    }

    @Override
    public void reset() throws JMSException {
        this.inArrayRemaining = 0;
        this.inType = 0;
        if (this.in != null) this.in = null;
        if (this.out != null) {
            try {
                this.data = this.out.toByteArray();
            } catch (final IOException ex) {
                throw (JMSException) new JMSException("i/o error").initCause(ex);
            }
            this.out = null;
        }
    }

    @Override
    public void writeBoolean(final boolean v) throws JMSException {
        try {
            beforeWrite(BOOLEAN).writeBoolean(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeByte(final byte v) throws JMSException {
        try {
            beforeWrite(BYTE).writeByte(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeBytes(final byte[] v) throws JMSException {
        writeBytes(v, 0, v.length);
    }

    @Override
    public void writeBytes(final byte[] src, final int offs, final int len) throws JMSException {
        CheckBounds.offset(src, offs, len);
        try {
            beforeWrite(ARRAY).writeInt(len);
            this.out.write(src, offs, len);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeChar(final char v) throws JMSException {
        try {
            beforeWrite(CHAR).writeChar(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeDouble(final double v) throws JMSException {
        try {
            beforeWrite(DOUBLE).writeDouble(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeFloat(final float v) throws JMSException {
        try {
            beforeWrite(FLOAT).writeFloat(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeInt(final int v) throws JMSException {
        try {
            beforeWrite(INT).writeInt(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeLong(final long v) throws JMSException {
        try {
            beforeWrite(LONG).writeLong(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeObject(final Object v) throws JMSException {
        final Class<?> c = (v == null) ? String.class : v.getClass();
        if (c == String.class) writeString((String) v); else if (c == Boolean.class) writeBoolean((Boolean) v); else if (c == Byte.class) writeByte((Byte) v); else if (c == Character.class) writeChar((Character) v); else if (c == Double.class) writeDouble((Double) v); else if (c == Float.class) writeFloat((Float) v); else if (c == Integer.class) writeInt((Integer) v); else if (c == Long.class) writeLong((Long) v); else if (c == Short.class) writeShort((Short) v); else if (c == byte[].class) writeBytes((byte[]) v); else throw new MessageFormatException("unsupported type: " + c);
    }

    @Override
    public void writeShort(final short v) throws JMSException {
        try {
            beforeWrite(SHORT).writeShort(v);
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }

    @Override
    public void writeString(final String v) throws JMSException {
        try {
            if (v == null) beforeWrite(NULL); else {
                final ByteBufferOutputStream out = beforeWrite(STRING);
                out.writeInt(v.length());
                out.writeChars(v);
            }
        } catch (final IOException ex) {
            throw exceptionOnWrite(ex);
        }
    }
}
