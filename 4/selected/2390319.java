package rescuecore.commands;

import rescuecore.InputBuffer;
import rescuecore.OutputBuffer;
import rescuecore.RescueConstants;

public class AKChannel extends AgentCommand {

    private byte[] channels;

    public AKChannel(int senderID, byte channel) {
        this(senderID, new byte[] { channel });
    }

    public AKChannel(int senderID, byte[] channels) {
        super(RescueConstants.AK_CHANNEL, senderID);
        this.channels = new byte[channels.length];
        System.arraycopy(channels, 0, this.channels, 0, channels.length);
    }

    public AKChannel(InputBuffer in) {
        super(RescueConstants.AK_CHANNEL, 0);
        read(in);
    }

    public void write(OutputBuffer out) {
        super.write(out);
        out.writeInt(channels.length);
        for (int i = 0; i < channels.length; ++i) out.writeByte(channels[i]);
    }

    public void read(InputBuffer in) {
        super.read(in);
        channels = new byte[in.readInt()];
        for (int i = 0; i < channels.length; ++i) channels[i] = in.readByte();
    }

    public byte[] getChannels() {
        return channels;
    }
}
