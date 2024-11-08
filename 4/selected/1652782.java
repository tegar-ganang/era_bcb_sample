package com.sshtools.j2ssh.connection;

import com.sshtools.j2ssh.io.ByteArrayReader;
import com.sshtools.j2ssh.io.ByteArrayWriter;
import com.sshtools.j2ssh.transport.InvalidMessageException;
import com.sshtools.j2ssh.transport.SshMessage;
import java.io.IOException;

/**
 *
 *
 * @author $author$
 * @version $Revision: 1.22 $
 */
public class SshMsgChannelData extends SshMessage {

    /**  */
    public static final int SSH_MSG_CHANNEL_DATA = 94;

    private byte[] channelData;

    private long recipientChannel;

    /**
     * Creates a new SshMsgChannelData object.
     *
     * @param recipientChannel
     * @param channelData
     */
    public SshMsgChannelData(long recipientChannel, byte[] channelData) {
        super(SSH_MSG_CHANNEL_DATA);
        this.recipientChannel = recipientChannel;
        this.channelData = channelData;
    }

    /**
     * Creates a new SshMsgChannelData object.
     */
    public SshMsgChannelData() {
        super(SSH_MSG_CHANNEL_DATA);
    }

    /**
     *
     *
     * @return
     */
    public byte[] getChannelData() {
        return channelData;
    }

    /**
     *
     *
     * @return
     */
    public long getChannelDataLength() {
        return channelData.length;
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_MSG_CHANNEL_DATA";
    }

    /**
     *
     *
     * @return
     */
    public long getRecipientChannel() {
        return recipientChannel;
    }

    /**
     *
     *
     * @param baw
     *
     * @throws InvalidMessageException
     */
    protected void constructByteArray(ByteArrayWriter baw) throws InvalidMessageException {
        try {
            baw.writeInt(recipientChannel);
            if (channelData != null) {
                baw.writeBinaryString(channelData);
            } else {
                baw.writeInt(0);
            }
        } catch (IOException ioe) {
            throw new InvalidMessageException("Invalid message data");
        }
    }

    /**
     *
     *
     * @param bar
     *
     * @throws InvalidMessageException
     */
    protected void constructMessage(ByteArrayReader bar) throws InvalidMessageException {
        try {
            recipientChannel = bar.readInt();
            if (bar.available() > 0) {
                channelData = bar.readBinaryString();
            }
        } catch (IOException ioe) {
            throw new InvalidMessageException("Invalid message data");
        }
    }
}
