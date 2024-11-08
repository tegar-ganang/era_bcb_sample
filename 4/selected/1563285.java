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
public class SshMsgChannelSuccess extends SshMessage {

    /**  */
    protected static final int SSH_MSG_CHANNEL_SUCCESS = 99;

    private long channelId;

    /**
     * Creates a new SshMsgChannelSuccess object.
     *
     * @param recipientChannelId
     */
    public SshMsgChannelSuccess(long recipientChannelId) {
        super(SSH_MSG_CHANNEL_SUCCESS);
        channelId = recipientChannelId;
    }

    /**
     * Creates a new SshMsgChannelSuccess object.
     */
    public SshMsgChannelSuccess() {
        super(SSH_MSG_CHANNEL_SUCCESS);
    }

    /**
     *
     *
     * @return
     */
    public long getChannelId() {
        return channelId;
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_MSG_CHANNEL_SUCCESS";
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
            baw.writeInt(channelId);
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
            channelId = bar.readInt();
        } catch (IOException ioe) {
            throw new InvalidMessageException("Invalid message data");
        }
    }
}
