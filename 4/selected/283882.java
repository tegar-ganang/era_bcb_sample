package org.beepcore.beep.core;

/**
 *
 * @author Eric Dixon
 * @author Huston Franklin
 * @author Jay Kint
 * @author Scott Pead
 * @version $Revision, $Date: 2002/04/30 04:06:22 $
 */
public class MessageStatus {

    /** Uninitialized BEEP status. */
    public static final int MESSAGE_STATUS_UNK = 0;

    /** BEEP message status. */
    public static final int MESSAGE_STATUS_NOT_SENT = 1;

    /** BEEP message status. */
    public static final int MESSAGE_STATUS_SENT = 2;

    /** BEEP message status. */
    public static final int MESSAGE_STATUS_RECEIVED_REPLY = 3;

    /** BEEP message status. */
    public static final int MESSAGE_STATUS_RECEIVED_ERROR = 4;

    /** Status of message. */
    private int messageStatus = MESSAGE_STATUS_UNK;

    private Channel channel;

    private int messageType;

    private int msgno;

    private int ansno;

    private OutputDataStream data;

    private ReplyListener replyListener;

    MessageStatus(Channel channel, int messageType, int msgno, OutputDataStream data) {
        this(channel, messageType, msgno, -1, data, null);
    }

    MessageStatus(Channel channel, int messageType, int msgno, OutputDataStream data, ReplyListener replyListener) {
        this(channel, messageType, msgno, -1, data, replyListener);
    }

    MessageStatus(Channel channel, int messageType, int msgno, int ansno, OutputDataStream data) {
        this(channel, messageType, msgno, ansno, data, null);
    }

    MessageStatus(Channel channel, int messageType, int msgno, int ansno, OutputDataStream data, ReplyListener replyListener) {
        this.messageStatus = MESSAGE_STATUS_UNK;
        this.channel = channel;
        this.messageType = messageType;
        this.msgno = msgno;
        this.ansno = ansno;
        this.data = data;
        this.replyListener = replyListener;
    }

    /**
     * Returns the answer number.
     *
     */
    public int getAnsno() {
        return this.ansno;
    }

    /**
     * Returns the channel.
     *
     */
    public Channel getChannel() {
        return this.channel;
    }

    /**
     * Returns the message data.
     *
     */
    public OutputDataStream getMessageData() {
        return this.data;
    }

    /**
     * Returns the message status.
     *
     */
    public int getMessageStatus() {
        return this.messageStatus;
    }

    /**
     * Returns the message number.
     *
     */
    public int getMsgno() {
        return this.msgno;
    }

    int getMessageType() {
        return this.messageType;
    }

    /**
     * Method setMessageStatus
     *
     *
     * @param status
     *
     */
    void setMessageStatus(int status) {
        this.messageStatus = status;
    }

    /**
     * Method getListener
     *
     *
     */
    ReplyListener getReplyListener() {
        return this.replyListener;
    }
}
