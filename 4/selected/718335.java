package net.kano.joscar.snaccmd.chat;

import net.kano.joscar.ByteBlock;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.snaccmd.AbstractIcbm;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;
import net.kano.joscar.tlv.TlvTools;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A base class for chat-message ICBM commands, both incoming and outgoing.
 */
public abstract class AbstractChatMsgIcbm extends AbstractIcbm {

    /** A TLV type sent if this message was sent to the entire chat room. */
    private static final int TYPE_IS_PUBLIC = 0x0001;

    /** A TLV type containing the chat message block. */
    private static final int TYPE_MSGBLOCK = 0x0005;

    /** The chat message block. */
    private final ChatMsg chatMsg;

    /** ICBM-type-specific TLV's. */
    private final TlvChain chatTlvs;

    /**
     * Creates a new chat ICBM with the given SNAC command subtype and with
     * properties read from the given incoming packet.
     *
     * @param command the SNAC command subtype of this command
     * @param packet a chat ICBM packet
     */
    protected AbstractChatMsgIcbm(int command, SnacPacket packet) {
        super(ChatCommand.FAMILY_CHAT, command, packet);
        TlvChain chain = TlvTools.readChain(getChannelData());
        Tlv msgTlv = chain.getLastTlv(TYPE_MSGBLOCK);
        if (msgTlv != null) {
            ByteBlock msgBlock = msgTlv.getData();
            chatMsg = ChatMsg.readChatMsg(msgBlock);
        } else {
            chatMsg = null;
        }
        chatTlvs = chain;
    }

    /**
     * Creates a new outgoing chat ICBM with the given properties.
     *
     * @param command this ICBM's SNAC command subtype
     * @param messageId a (normally unique) ICBM message ID
     * @param chatMsg the message to send to the channel
     */
    protected AbstractChatMsgIcbm(int command, long messageId, ChatMsg chatMsg) {
        super(ChatCommand.FAMILY_CHAT, command, messageId, CHANNEL_CHAT);
        this.chatMsg = chatMsg;
        chatTlvs = null;
    }

    /**
     * Returns this ICBM's embedded chat message.
     *
     * @return the chat message in this ICBM
     */
    public final ChatMsg getMessage() {
        return chatMsg;
    }

    /**
     * Returns the extra command-specific TLV's sent in this chat message. Will
     * be <code>null</code> if this message was not read from an incoming
     * stream.
     *
     * @return this ICBM's command-type-specific TLV's
     */
    protected final TlvChain getChatTlvs() {
        return chatTlvs;
    }

    protected void writeChannelData(OutputStream out) throws IOException {
        if (chatMsg != null) {
            ByteBlock msgBlock = ByteBlock.createByteBlock(chatMsg);
            new Tlv(TYPE_MSGBLOCK, msgBlock).write(out);
        }
        writeChatTlvs(out);
    }

    /**
     * Writes the extra command-type-specific TLV's to be sent with this ICBM
     * to the given stream.
     *
     * @param out the stream to which to write
     * @throws IOException if an I/O error occurs
     */
    protected abstract void writeChatTlvs(OutputStream out) throws IOException;

    public String toString() {
        return "AbstractChatMsgIcbm: <" + super.toString() + ">, " + "chatMsg=" + chatMsg;
    }
}
