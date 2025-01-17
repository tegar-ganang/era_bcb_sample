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
 * @version $Revision: 1.20 $
 */
public class SshMsgChannelOpen extends SshMessage {

    /**  */
    protected static final int SSH_MSG_CHANNEL_OPEN = 90;

    private String channelType;

    private byte[] channelData;

    private long initialWindowSize;

    private long maximumPacketSize;

    private long senderChannelId;

    /**
     * Creates a new SshMsgChannelOpen object.
     *
     * @param channelType
     * @param senderChannelId
     * @param initialWindowSize
     * @param maximumPacketSize
     * @param channelData
     */
    public SshMsgChannelOpen(String channelType, long senderChannelId, long initialWindowSize, long maximumPacketSize, byte[] channelData) {
        super(SSH_MSG_CHANNEL_OPEN);
        this.channelType = channelType;
        this.senderChannelId = senderChannelId;
        this.initialWindowSize = initialWindowSize;
        this.maximumPacketSize = maximumPacketSize;
        this.channelData = channelData;
    }

    /**
     * Creates a new SshMsgChannelOpen object.
     */
    public SshMsgChannelOpen() {
        super(SSH_MSG_CHANNEL_OPEN);
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
    public String getChannelType() {
        return channelType;
    }

    /**
     *
     *
     * @return
     */
    public long getInitialWindowSize() {
        return initialWindowSize;
    }

    /**
     *
     *
     * @return
     */
    public long getMaximumPacketSize() {
        return maximumPacketSize;
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_MSG_CHANNEL_OPEN";
    }

    /**
     *
     *
     * @return
     */
    public long getSenderChannelId() {
        return senderChannelId;
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
            baw.writeString(channelType);
            baw.writeInt(senderChannelId);
            baw.writeInt(initialWindowSize);
            baw.writeInt(maximumPacketSize);
            if (channelData != null) {
                baw.write(channelData);
            }
        } catch (IOException ioe) {
            throw new InvalidMessageException("Could not write message data");
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
            channelType = bar.readString();
            senderChannelId = bar.readInt();
            initialWindowSize = bar.readInt();
            maximumPacketSize = bar.readInt();
            if (bar.available() > 0) {
                channelData = new byte[bar.available()];
                bar.read(channelData);
            }
        } catch (IOException ioe) {
            throw new InvalidMessageException("Invalid message data");
        }
    }
}
