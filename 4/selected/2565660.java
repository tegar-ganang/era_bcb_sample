package utils.transport;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

public class ShortMsgSerializer implements UDPSerializer {

    public static final int MSG_LENGTH = 4;

    public byte[] getBytes(ShortMessage message) {
        byte[] data = new byte[4];
        data[0] = (byte) message.getCommand();
        data[1] = (byte) message.getChannel();
        data[2] = (byte) message.getData1();
        data[3] = (byte) message.getData2();
        return data;
    }

    public int getByte(byte b) {
        if (b < 0) return b + 256;
        return b;
    }

    public ShortMessage getMessage(byte[] bytes) throws SerializeException {
        if (bytes.length != MSG_LENGTH) {
            throw new SerializeException();
        }
        ShortMessage mes = new ShortMessage();
        try {
            mes.setMessage(getByte(bytes[0]), getByte(bytes[1]), getByte(bytes[2]), getByte(bytes[3]));
        } catch (InvalidMidiDataException e) {
            throw new SerializeException();
        }
        return mes;
    }

    public int getPacketSize() {
        return MSG_LENGTH;
    }

    public byte[] serialize(Object obj) throws SerializeException {
        if (obj instanceof ShortMessage) return getBytes((ShortMessage) obj);
        throw new SerializeException();
    }

    public Object unserialize(byte[] bytes, int offset, int length) throws SerializeException {
        return getMessage(bytes);
    }
}
