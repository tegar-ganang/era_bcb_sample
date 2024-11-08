package net.kano.joscar.snaccmd;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.MiscTools;
import net.kano.joscar.flapcmd.SnacCommand;
import net.kano.joscar.flapcmd.SnacPacket;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Provides a base for all six "Inter-Client Basic Message" SNAC commands. These
 * six commands are {@linkplain net.kano.joscar.snaccmd.icbm.SendImIcbm sending
 * an IM}, {@linkplain net.kano.joscar.snaccmd.icbm.RecvImIcbm receiving an IM},
 * {@linkplain net.kano.joscar.snaccmd.icbm.SendRvIcbm sending a rendezvous
 * packet}, {@linkplain net.kano.joscar.snaccmd.icbm.RecvRvIcbm receiving a
 * rendezvous packet}, {@linkplain net.kano.joscar.snaccmd.chat.SendChatMsgIcbm
 * sending a chat room message}, and {@linkplain
 * net.kano.joscar.snaccmd.chat.RecvChatMsgIcbm receiving a chat room message}.
 * All ICBM's contain an ICBM message ID, an ICBM channel, and a
 * channel-specific data block.
 */
public abstract class AbstractIcbm extends SnacCommand {

    /**
     * The ICBM channel on which instant messages are sent.
     */
    public static final int CHANNEL_IM = 0x0001;

    /**
     * The ICBM channel on which rendezvous messages are sent.
     */
    public static final int CHANNEL_RV = 0x0002;

    /**
     * The ICBM channel on which chat messages are sent.
     */
    public static final int CHANNEL_CHAT = 0x0003;

    /**
     * Returns the ICBM channel on which the given <code>SnacPacket</code>
     * resides. This assumes the validity of the given packet as an ICBM
     * packet.
     *
     * @param packet the ICBM packet whose channel number will be returned
     * @return the ICBM channel on which the given packet was received
     */
    public static int getIcbmChannel(SnacPacket packet) {
        DefensiveTools.checkNull(packet, "packet");
        ByteBlock block = packet.getData();
        return BinaryTools.getUShort(block, 8);
    }

    /**
     * The eight-byte ICBM message ID of this ICBM.
     */
    private final long messageId;

    /**
     * The ICBM channel on which this ICBM resides.
     */
    private final int channel;

    /**
     * This ICBM's "channel-specific data" block.
     */
    private final ByteBlock channelData;

    /**
     * Creates an ICBM command with the given SNAC family and command type,
     * deriving ICBM fields from the contents of the given packet. Use
     * {@link #getChannelData()} after using this constructor to retrieve
     * channel-specific data for this ICBM.
     *
     * @param family the SNAC family of this ICBM command
     * @param command the SNAC command subtype of this ICBM command
     * @param packet the packet to represent
     */
    protected AbstractIcbm(int family, int command, SnacPacket packet) {
        super(family, command);
        DefensiveTools.checkNull(packet, "packet");
        ByteBlock snacData = packet.getData();
        messageId = BinaryTools.getLong(snacData, 0);
        channel = BinaryTools.getUShort(snacData, 8);
        channelData = snacData.subBlock(10);
    }

    /**
     * Creates an ICBM command with the given SNAC family and command subtype,
     * the given ICBM message ID, and on the given ICBM channel.
     *
     * @param family the SNAC family of this ICBM command
     * @param command the SNAC command subtype of this ICBM command
     * @param messageId the 8-byte ICBM message ID of this command
     * @param channel the ICBM channel of this command (should be one of {@link
     *        #CHANNEL_IM}, {@link #CHANNEL_RV}, {@link #CHANNEL_CHAT})
     */
    protected AbstractIcbm(int family, int command, long messageId, int channel) {
        super(family, command);
        DefensiveTools.checkRange(channel, "channel", 0);
        this.messageId = messageId;
        this.channel = channel;
        channelData = null;
    }

    /**
     * Returns this ICBM command's message ID. This value is used to uniquely
     * identify ICBM responses.
     *
     * @return this command's ICBM message ID
     */
    public final long getIcbmMessageId() {
        return messageId;
    }

    /**
     * Returns the ICBM channel on which this command resides.
     *
     * @return this command's ICBM channel
     */
    public final int getChannel() {
        return channel;
    }

    /**
     * Returns the "channel-specific data" block of this ICBM. Only
     * non-<code>null</code> for incoming ICBM's created using {@link
     * #AbstractIcbm(int, int, SnacPacket)}.
     *
     * @return this ICBM's "channel-specific data"
     */
    protected final ByteBlock getChannelData() {
        return channelData;
    }

    public void writeData(OutputStream out) throws IOException {
        BinaryTools.writeLong(out, messageId);
        BinaryTools.writeUShort(out, channel);
        writeChannelData(out);
    }

    /**
     * Writes the channel-specific data for this ICBM command to the
     * given output stream.
     *
     * @param out the stream to write to
     * @throws IOException if an I/O error occurs
     */
    protected abstract void writeChannelData(OutputStream out) throws IOException;

    public String toString() {
        return MiscTools.getClassName(this) + ": channel=" + this.channel + ", messageId=" + this.getIcbmMessageId();
    }
}
