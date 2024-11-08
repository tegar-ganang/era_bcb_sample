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
public class SshMsgChannelRequest extends SshMessage {

    /**  */
    protected static final int SSH_MSG_CHANNEL_REQUEST = 98;

    private String requestType;

    private byte[] channelData;

    private boolean wantReply;

    private long recipientChannel;

    /**
     * Creates a new SshMsgChannelRequest object.
     *
     * @param recipientChannel
     * @param requestType
     * @param wantReply
     * @param channelData
     */
    public SshMsgChannelRequest(long recipientChannel, String requestType, boolean wantReply, byte[] channelData) {
        super(SSH_MSG_CHANNEL_REQUEST);
        this.recipientChannel = recipientChannel;
        this.requestType = requestType;
        this.wantReply = wantReply;
        this.channelData = channelData;
    }

    /**
     * Creates a new SshMsgChannelRequest object.
     */
    public SshMsgChannelRequest() {
        super(SSH_MSG_CHANNEL_REQUEST);
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
    public String getMessageName() {
        return "SSH_MSG_CHANNEL_REQUEST";
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
     * @return
     */
    public String getRequestType() {
        return requestType;
    }

    /**
     *
     *
     * @return
     */
    public boolean getWantReply() {
        return wantReply;
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
            baw.writeString(requestType);
            baw.write((wantReply ? 1 : 0));
            if (channelData != null) {
                baw.write(channelData);
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
            requestType = bar.readString();
            wantReply = ((bar.read() == 0) ? false : true);
            if (bar.available() > 0) {
                channelData = new byte[bar.available()];
                bar.read(channelData);
            }
        } catch (IOException ioe) {
            throw new InvalidMessageException("Invalid message data");
        }
    }
}
