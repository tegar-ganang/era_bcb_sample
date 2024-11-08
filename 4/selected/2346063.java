package com.beem.project.beem.service;

import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smack.packet.PacketExtension;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Date;

/**
 * This class represents a instant message.
 * 
 * @author darisk
 */
public class Message implements Parcelable {

    /** Normal message type. Theese messages are like an email, with subject. */
    public static final int MSG_TYPE_NORMAL = 100;

    /** Chat message type. */
    public static final int MSG_TYPE_CHAT = 200;

    /** Group chat message type. */
    public static final int MSG_TYPE_GROUP_CHAT = 300;

    /** Error message type. */
    public static final int MSG_TYPE_ERROR = 400;

    /** Informational message type. */
    public static final int MSG_TYPE_INFO = 500;

    /** Parcelable.Creator needs by Android. */
    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {

        @Override
        public Message createFromParcel(Parcel source) {
            return new Message(source);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    private int mType;

    private String mBody;

    private String mSubject;

    private String mTo;

    private String mFrom;

    private String mThread;

    private Date mTimestamp;

    private boolean mHL;

    /**
	 * Constructor.
	 * 
	 * @param to
	 *            the destinataire of the message
	 * @param type
	 *            the message type
	 */
    public Message(final String to, final int type) {
        mTo = to;
        mType = type;
        mBody = "";
        mSubject = "";
        mThread = "";
        mFrom = null;
        mTimestamp = new Date();
        mHL = false;
    }

    /**
	 * Constructor a message of type chat.
	 * 
	 * @param to
	 *            the destinataire of the message
	 */
    public Message(final String to) {
        this(to, MSG_TYPE_CHAT);
    }

    /**
	 * Construct a message from a smack message packet.
	 * 
	 * @param smackMsg
	 *            Smack message packet
	 */
    public Message(final org.jivesoftware.smack.packet.Message smackMsg) {
        this(smackMsg.getTo());
        switch(smackMsg.getType()) {
            case chat:
                mType = MSG_TYPE_CHAT;
                break;
            case groupchat:
                mType = MSG_TYPE_GROUP_CHAT;
                break;
            case normal:
                mType = MSG_TYPE_NORMAL;
                break;
            case error:
                mType = MSG_TYPE_ERROR;
                break;
            default:
                mType = MSG_TYPE_NORMAL;
                break;
        }
        this.mFrom = smackMsg.getFrom();
        mHL = false;
        if (mType == MSG_TYPE_ERROR) {
            XMPPError er = smackMsg.getError();
            String msg = er.getMessage();
            if (msg != null) mBody = msg; else mBody = er.getCondition();
        } else {
            mBody = smackMsg.getBody();
            mSubject = smackMsg.getSubject();
            mThread = smackMsg.getThread();
        }
        PacketExtension pTime = smackMsg.getExtension("delay", "urn:xmpp:delay");
        if (pTime instanceof DelayInformation) {
            mTimestamp = ((DelayInformation) pTime).getStamp();
        } else {
            mTimestamp = new Date();
        }
    }

    /**
	 * Construct a message from a parcel.
	 * 
	 * @param in
	 *            parcel to use for construction
	 */
    private Message(final Parcel in) {
        mType = in.readInt();
        mTo = in.readString();
        mBody = in.readString();
        mSubject = in.readString();
        mThread = in.readString();
        mFrom = in.readString();
        mTimestamp = new Date(in.readLong());
        mHL = false;
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mType);
        dest.writeString(mTo);
        dest.writeString(mBody);
        dest.writeString(mSubject);
        dest.writeString(mThread);
        dest.writeString(mFrom);
        dest.writeLong(mTimestamp.getTime());
    }

    /**
	 * Get the type of the message.
	 * 
	 * @return the type of the message.
	 */
    public int getType() {
        return mType;
    }

    /**
	 * Set the type of the message.
	 * 
	 * @param type
	 *            the type to set
	 */
    public void setType(int type) {
        mType = type;
    }

    /**
	 * Get the body of the message.
	 * 
	 * @return the Body of the message
	 */
    public String getBody() {
        return mBody;
    }

    /**
	 * Set the body of the message.
	 * 
	 * @param body
	 *            the body to set
	 */
    public void setBody(String body) {
        mBody = body;
    }

    /**
	 * Get the subject of the message.
	 * 
	 * @return the subject
	 */
    public String getSubject() {
        return mSubject;
    }

    /**
	 * Set the subject of the message.
	 * 
	 * @param subject
	 *            the subject to set
	 */
    public void setSubject(String subject) {
        mSubject = subject;
    }

    /**
	 * Get the destinataire of the message.
	 * 
	 * @return the destinataire of the message
	 */
    public String getTo() {
        return mTo;
    }

    /**
	 * Set the destinataire of the message.
	 * 
	 * @param to
	 *            the destinataire to set
	 */
    public void setTo(String to) {
        mTo = to;
    }

    /**
	 * Set the from field of the message.
	 * 
	 * @param from
	 *            the mFrom to set
	 */
    public void setFrom(String from) {
        this.mFrom = from;
    }

    /**
	 * Get the from field of the message.
	 * 
	 * @return the mFrom
	 */
    public String getFrom() {
        return mFrom;
    }

    /**
	 * Get the thread of the message.
	 * 
	 * @return the thread
	 */
    public String getThread() {
        return mThread;
    }

    /**
	 * Set the thread of the message.
	 * 
	 * @param thread
	 *            the thread to set
	 */
    public void setThread(String thread) {
        mThread = thread;
    }

    /**
	 * Set the Date of the message.
	 * 
	 * @param date
	 *            date of the message.
	 */
    public void setTimestamp(Date date) {
        mTimestamp = date;
    }

    /**
	 * Get the Date of the message.
	 * 
	 * @return if it is a delayed message get the date the message was sended.
	 */
    public Date getTimestamp() {
        return mTimestamp;
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public int describeContents() {
        return 0;
    }

    public void setHL(boolean mHL) {
        this.mHL = mHL;
    }

    public boolean isHL() {
        return mHL;
    }
}
