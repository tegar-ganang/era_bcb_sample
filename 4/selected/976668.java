package org.xi8ix.jms;

import javax.jms.JMSException;
import javax.jms.StreamMessage;
import javax.jms.MessageNotReadableException;
import javax.jms.MessageNotWriteableException;
import java.nio.ByteBuffer;

/**
 * @author Iain Shigeoka
 */
public class JMSStreamMessage extends JMSMessage implements StreamMessage {

    private ByteBuffer buffer;

    private boolean input = true;

    public JMSStreamMessage() {
        buffer = ByteBuffer.allocate(512);
    }

    public JMSStreamMessage(JMSStreamMessage message) {
        super(message);
        buffer = ByteBuffer.allocate(message.buffer.capacity());
        buffer.position(message.buffer.position());
        buffer.limit(message.buffer.limit());
        this.input = message.input;
    }

    public boolean readBoolean() throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public byte readByte() throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public short readShort() throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public char readChar() throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public int readInt() throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public long readLong() throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public float readFloat() throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public double readDouble() throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public String readString() throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public int readBytes(byte[] bytes) throws JMSException {
        if (input) {
            throw new MessageNotReadableException("Message in write-only mode. Reset to read from message.");
        }
        int remaining = buffer.remaining();
        return remaining - buffer.get(bytes, 0, remaining < bytes.length ? remaining : bytes.length).remaining();
    }

    public Object readObject() throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public void writeBoolean(boolean b) throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public void writeByte(byte b) throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public void writeShort(short i) throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public void writeChar(char c) throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public void writeInt(int i) throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public void writeLong(long l) throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public void writeFloat(float v) throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public void writeDouble(double v) throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public void writeString(String string) throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public void writeBytes(byte[] bytes) throws JMSException {
        writeBytes(bytes, 0, bytes.length);
    }

    public void writeBytes(byte[] bytes, int offset, int length) throws JMSException {
        if (input) {
            buffer.put(bytes, offset, length);
        } else {
            throw new MessageNotWriteableException("Body is in read-only mode. Clear body to write.");
        }
    }

    public void writeObject(Object object) throws JMSException {
        throw new JMSException("Operation not supported");
    }

    public void clearBody() throws JMSException {
        buffer.reset();
        input = true;
    }

    public void reset() throws JMSException {
        if (input) {
            buffer.flip();
            input = false;
        }
    }

    public String toString() {
        return super.toString() + " " + buffer.toString();
    }
}
