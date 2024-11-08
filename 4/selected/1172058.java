package net.kano.joscar.snaccmd.icbm;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.snaccmd.ExtraInfoBlock;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;
import net.kano.joscar.tlv.TlvTools;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

/**
 * A SNAC command containing an IM.
 *
 * @snac.src server
 * @snac.cmd 0x04 0x07
 *
 * @see SendImIcbm
 */
public class RecvImIcbm extends AbstractImIcbm implements RecvIcbm {

    /** A TLV type present if the sender supports typing notification. */
    private static final int TYPE_CAN_TYPE = 0x000b;

    /** Whether the sender supports typing notification. */
    private final boolean canType;

    /** Information about the sender of this IM. */
    private final FullUserInfo userInfo;

    /**
     * Generates a new incoming IM ICBM command from the given incoming SNAC
     * packet.
     *
     * @param packet an incoming IM ICBM packet
     */
    protected RecvImIcbm(SnacPacket packet) {
        super(IcbmCommand.CMD_ICBM, packet);
        DefensiveTools.checkNull(packet, "packet");
        ByteBlock snacData = getChannelData();
        userInfo = FullUserInfo.readUserInfo(snacData);
        ByteBlock tlvBlock = snacData.subBlock(userInfo.getTotalSize());
        TlvChain chain = TlvTools.readChain(tlvBlock);
        processImTlvs(chain);
        canType = chain.hasTlv(TYPE_CAN_TYPE);
    }

    /**
     * Creates a new outgoing client-bound IM ICBM command with the given
     * properties.
     *
     * @param messageId the ICBM message ID to associate with this message
     * @param userInfo a user information block for the sender of this IM
     * @param message the instant message
     * @param autoResponse whether this message is an auto-response
     * @param wantsIcon whether the sender wants the receiver's buddy icon
     * @param iconInfo a set of icon information provided by the sender, or
     *        <code>null</code> if none was provided
     * @param expInfoBlocks a list of AIM Expression information blocks
     * @param featuresBlock an IM "features" block, like {@link
     *        #FEATURES_DEFAULT}
     * @param canType whether or not the sender supports typing notification
     */
    public RecvImIcbm(long messageId, FullUserInfo userInfo, InstantMessage message, boolean autoResponse, boolean wantsIcon, OldIconHashInfo iconInfo, Collection<ExtraInfoBlock> expInfoBlocks, ByteBlock featuresBlock, boolean canType) {
        super(IcbmCommand.CMD_ICBM, messageId, message, autoResponse, wantsIcon, iconInfo, expInfoBlocks, featuresBlock);
        DefensiveTools.checkNull(userInfo, "userInfo");
        this.canType = canType;
        this.userInfo = userInfo;
    }

    /**
     * Returns a user information block containing information about the sender
     * of this IM.
     *
     * @return a user information block for the sender of this IM
     */
    public final FullUserInfo getSenderInfo() {
        return userInfo;
    }

    /**
     * Returns whether or not the sender supports {@linkplain
     * SendTypingNotification typing notification}.
     *
     * @return whether the sender supports typing notification
     */
    public final boolean canType() {
        return canType;
    }

    protected final void writeChannelData(OutputStream out) throws IOException {
        userInfo.write(out);
        if (canType) new Tlv(TYPE_CAN_TYPE).write(out);
        writeImTlvs(out);
    }

    public String toString() {
        return "RecvImIcbm: message from " + userInfo + ": " + getMessage();
    }
}
