package rescuecore.commands;

import rescuecore.InputBuffer;
import rescuecore.OutputBuffer;
import rescuecore.RescueConstants;

public class KAHear extends Command {

    private int toID;

    private int fromID;

    private int length;

    private byte[] msg;

    private byte channel;

    public KAHear(int to, int from, int length, byte[] data, byte channel) {
        super(RescueConstants.KA_HEAR);
        toID = to;
        fromID = from;
        this.length = length;
        msg = new byte[length];
        System.arraycopy(data, 0, msg, 0, length);
        this.channel = channel;
    }

    public KAHear(InputBuffer in) {
        super(RescueConstants.KA_HEAR);
        read(in);
    }

    public void read(InputBuffer in) {
        toID = in.readInt();
        fromID = in.readInt();
        channel = (byte) in.readInt();
        length = in.readInt();
        msg = new byte[length];
        in.readBytes(msg);
    }

    public void write(OutputBuffer out) {
        out.writeInt(toID);
        out.writeInt(fromID);
        out.writeInt(channel);
        out.writeInt(length);
        out.writeBytes(msg);
    }

    public int getToID() {
        return toID;
    }

    public int getFromID() {
        return fromID;
    }

    public int getLength() {
        return length;
    }

    public byte[] getData() {
        return msg;
    }

    public byte getChannel() {
        return channel;
    }

    public String toString() {
        return super.toString() + " from " + fromID + " to " + toID + ": " + length + " bytes on channel " + channel;
    }
}
