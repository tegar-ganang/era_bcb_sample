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
 * @version $Revision: 1.21 $
 */
public class SshMsgChannelExtendedData extends SshMessage {

    /**  */
    public static final int SSH_MSG_CHANNEL_EXTENDED_DATA = 95;

    /**  */
    public static final int SSH_EXTENDED_DATA_STDERR = 1;

    private byte[] channelData;

    private int dataTypeCode;

    private long recipientChannel;

    /**
     * Creates a new SshMsgChannelExtendedData object.
     *
     * @param recipientChannel
     * @param dataTypeCode
     * @param channelData
     */
    public SshMsgChannelExtendedData(long recipientChannel, int dataTypeCode, byte[] channelData) {
        super(SSH_MSG_CHANNEL_EXTENDED_DATA);
        this.recipientChannel = recipientChannel;
        this.dataTypeCode = dataTypeCode;
        this.channelData = channelData;
    }

    /**
     * Creates a new SshMsgChannelExtendedData object.
     */
    public SshMsgChannelExtendedData() {
        super(SSH_MSG_CHANNEL_EXTENDED_DATA);
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
    public int getDataTypeCode() {
        return dataTypeCode;
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_MSG_CHANNEL_EXTENDED_DATA";
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
            baw.writeInt(dataTypeCode);
            if (channelData != null) {
                baw.writeBinaryString(channelData);
            } else {
                baw.writeString("");
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
            dataTypeCode = (int) bar.readInt();
            if (bar.available() > 0) {
                channelData = bar.readBinaryString();
            }
        } catch (IOException ioe) {
            throw new InvalidMessageException("Invalid message data");
        }
    }
}
