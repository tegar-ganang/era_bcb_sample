package net.kano.joscar.snaccmd.icbm;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.LiveWritable;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.snaccmd.CapabilityBlock;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.tlv.TlvChain;
import net.kano.joscar.tlv.TlvTools;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A SNAC command containing rendezvous information.
 *
 * @snac.src server
 * @snac.cmd 0x04 0x07
 *
 * @see SendRvIcbm
 */
public class RecvRvIcbm extends AbstractRvIcbm implements RecvIcbm {

    /** A block describing the sender of this rendezvous ICBM. */
    private final FullUserInfo sender;

    /**
     * Generates an incoming rendezvous ICBM command from the given incoming
     * SNAC packet.
     *
     * @param packet an incoming rendezvous ICBM packet
     */
    protected RecvRvIcbm(SnacPacket packet) {
        super(IcbmCommand.CMD_ICBM, packet);
        DefensiveTools.checkNull(packet, "packet");
        ByteBlock channelData = getChannelData();
        sender = FullUserInfo.readUserInfo(channelData);
        ByteBlock tlvBlock = channelData.subBlock(sender.getTotalSize());
        TlvChain chain = TlvTools.readChain(tlvBlock);
        processRvTlvs(chain);
    }

    /**
     * Creates a new outgoing client-bound ICBM with the given properties.
     * 
     * @param icbmMessageId an ICBM message ID to associate with this rendezvous
     *        command
     * @param status a status code, like {@link #RVSTATUS_REQUEST}
     * @param rvSessionId a rendezvous session ID on which this rendezvous
     *        exists
     * @param cap this rendezvous's associated capability block
     * @param rvDataWriter an object used to write the rendezvous-specific data
     * @param sender an object describing the user who sent this rendezvous
     */
    public RecvRvIcbm(long icbmMessageId, int status, long rvSessionId, CapabilityBlock cap, LiveWritable rvDataWriter, FullUserInfo sender) {
        super(IcbmCommand.CMD_ICBM, icbmMessageId, status, rvSessionId, cap, rvDataWriter);
        DefensiveTools.checkNull(sender, "sender");
        this.sender = sender;
    }

    public final FullUserInfo getSenderInfo() {
        return sender;
    }

    protected final void writeChannelData(OutputStream out) throws IOException {
        sender.write(out);
        writeRvTlvs(out);
    }

    public String toString() {
        return "RecvRvIcbm: sender=<" + sender + ">, on top of " + super.toString();
    }
}
