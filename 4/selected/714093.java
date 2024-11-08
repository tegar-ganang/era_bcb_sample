package rescuecore.commands;

import rescuecore.InputBuffer;
import rescuecore.OutputBuffer;
import rescuecore.RescueConstants;

public class AKTell extends AgentCommand {

    private byte[] msg;

    private byte channel;

    public AKTell(int senderID, byte[] msg, int length, byte channel) {
        super(RescueConstants.AK_TELL, senderID);
        this.msg = new byte[length];
        System.arraycopy(msg, 0, this.msg, 0, length);
        this.channel = channel;
    }

    public AKTell(InputBuffer in) {
        super(RescueConstants.AK_TELL, 0);
        read(in);
    }

    public void write(OutputBuffer out) {
        super.write(out);
        out.writeInt(channel);
        out.writeInt(msg.length);
        out.writeBytes(msg);
    }

    public void read(InputBuffer in) {
        super.read(in);
        channel = (byte) in.readInt();
        msg = new byte[in.readInt()];
        in.readBytes(msg);
    }

    public byte[] getMessage() {
        return msg;
    }

    public byte getChannel() {
        return channel;
    }
}
