package spread;

import java.io.*;
import java.util.*;

/**
 * A SpreadMessage object represents either an incoming or outgoing spread message.
 * An outgoing message is one being sent with {@link SpreadConnection#multicast(SpreadMessage)}.
 * An incoming message is one received with {@link SpreadConnection#receive()}.
 * To send a message on a spread connection, first create a message object:
 * <p><blockquote><pre>
 * SpreadMessage message = new SpreadMessage();
 * </pre></blockquote><p>
 * Then set the data with either {@link SpreadMessage#setData(byte[])}, {@link SpreadMessage#setObject(Serializable)},
 * or {@link SpreadMessage#digest(Serializable)}:
 * <p><blockquote><pre>
 * message.setData(data);
 * </pre></blockquote><p>
 * Select which group(s) to send the message to with {@link SpreadMessage#addGroup(SpreadGroup)}:
 * <p><blockquote><pre>
 * message.setGroup(group);
 * </pre></blockquote><p>
 * When the message is read to be sent, send it with {@link SpreadConnection#multicast(SpreadMessage)}:
 * <p><blockquote><pre>
 * connection.multicast(message);
 * </pre></blockquote><p>
 */
public class SpreadMessage {

    protected static final int UNRELIABLE_MESS = 0x00000001;

    protected static final int RELIABLE_MESS = 0x00000002;

    protected static final int FIFO_MESS = 0x00000004;

    protected static final int CAUSAL_MESS = 0x00000008;

    protected static final int AGREED_MESS = 0x00000010;

    protected static final int SAFE_MESS = 0x00000020;

    protected static final int REGULAR_MESS = 0x0000003f;

    protected static final int SELF_DISCARD = 0x00000040;

    protected static final int REG_MEMB_MESS = 0x00001000;

    protected static final int TRANSITION_MESS = 0x00002000;

    protected static final int CAUSED_BY_JOIN = 0x00000100;

    protected static final int CAUSED_BY_LEAVE = 0x00000200;

    protected static final int CAUSED_BY_DISCONNECT = 0x00000400;

    protected static final int CAUSED_BY_NETWORK = 0x00000800;

    protected static final int MEMBERSHIP_MESS = 0x00003f00;

    protected static final int REJECT_MESS = 0x00400000;

    protected static final int JOIN_MESS = 0x00010000;

    protected static final int LEAVE_MESS = 0x00020000;

    protected static final int KILL_MESS = 0x00040000;

    protected static final int GROUPS_MESS = 0x00080000;

    private static final int CONTENT_DATA = 1;

    private static final int CONTENT_OBJECT = 2;

    private static final int CONTENT_DIGEST = 3;

    private boolean outgoing;

    protected int content;

    private int serviceType;

    protected Vector groups;

    private SpreadGroup sender;

    private byte[] data;

    private short type;

    private boolean endianMismatch;

    private MembershipInfo membershipInfo;

    protected ByteArrayOutputStream digestBytes;

    protected ObjectOutputStream digestOutput;

    protected SpreadMessage(int serviceType, Vector groups, SpreadGroup sender, byte[] data, short type, boolean endianMismatch, MembershipInfo membershipInfo) {
        outgoing = false;
        this.serviceType = serviceType;
        this.groups = groups;
        this.sender = sender;
        this.data = data;
        this.type = type;
        this.endianMismatch = endianMismatch;
        this.membershipInfo = membershipInfo;
    }

    /**
	 * Initializes a new outgoing SpreadMessage object.  By default the message is reliable.
	 */
    public SpreadMessage() {
        outgoing = true;
        content = CONTENT_DATA;
        serviceType = RELIABLE_MESS;
        groups = new Vector();
        data = new byte[0];
    }

    /**
	 * Check if this is an incoming message.  This is true if it has been received with 
	 * {@link SpreadConnection#receive()}.
	 * 
	 * @return  true if this in an incoming message
	 */
    public boolean isIncoming() {
        return !outgoing;
    }

    /**
	 * Check if this is an outgoing message.  This is true if this is a message being sent with
	 * {@link SpreadConnection#multicast(SpreadMessage)}.
	 * 
	 * @return  true if this is an outgoing message
	 */
    public boolean isOutgoing() {
        return outgoing;
    }

    /**
	 * Get the message's service type.  The service type is a bitfield representing the type of message.
	 * 
	 * @return  the service-type
	 */
    public int getServiceType() {
        return serviceType;
    }

    /**
	 * Checks if this is a regular message.  If true, the get*() functions can be
	 * used to obtain more information about the message.
	 * 
	 * @return  true if this is a regular message
	 * @see SpreadMessage#getGroups()
	 * @see SpreadMessage#getSender()
	 * @see SpreadMessage#getData()
	 * @see SpreadMessage#getObject()
	 * @see SpreadMessage#getDigest()
	 * @see SpreadMessage#getType()
	 * @see SpreadMessage#getEndianMismatch()
	 */
    public boolean isRegular() {
        return (((serviceType & REGULAR_MESS) != 0) && ((serviceType & REJECT_MESS) == 0));
    }

    protected static boolean isRegular(int serviceType) {
        return (((serviceType & REGULAR_MESS) != 0) && ((serviceType & REJECT_MESS) == 0));
    }

    /**
	 * Checks if this is a rejected message.  If true, the get*() methods can
	 * be used to get more information on which message or join/leave was rejected.
	 * 
	 * @return  true if this is a rejected message
	 * @see SpreadMessage#getGroups()
	 * @see SpreadMessage#getSender()
	 * @see SpreadMessage#getData()
	 * @see SpreadMessage#getObject()
	 * @see SpreadMessage#getDigest()
	 * @see SpreadMessage#getType()
	 * @see SpreadMessage#getEndianMismatch()
	 */
    public boolean isReject() {
        return ((serviceType & REJECT_MESS) != 0);
    }

    protected static boolean isReject(int serviceType) {
        return ((serviceType & REJECT_MESS) != 0);
    }

    /**
	 * Checks if this is a membership message.  If true, {@link SpreadMessage#getMembershipInfo()} can
	 * be used to get more information on the membership change.
	 * 
	 * @return  true if this is a membership message
	 * @see  SpreadMessage#getMembershipInfo()
	 */
    public boolean isMembership() {
        return (((serviceType & MEMBERSHIP_MESS) != 0) && ((serviceType & REJECT_MESS) == 0));
    }

    protected static boolean isMembership(int serviceType) {
        return (((serviceType & MEMBERSHIP_MESS) != 0) && ((serviceType & REJECT_MESS) == 0));
    }

    /**
	 * Checks if this is an unreliable message.
	 * 
	 * @return  true if this is an unreliable message
	 */
    public boolean isUnreliable() {
        return ((serviceType & UNRELIABLE_MESS) != 0);
    }

    /**
	 * Checks if this is a reliable message.
	 * 
	 * @return  true if this is a reliable message
	 */
    public boolean isReliable() {
        return ((serviceType & RELIABLE_MESS) != 0);
    }

    /**
	 * Checks if this is a fifo message.
	 * 
	 * @return  true if this is a fifo message
	 */
    public boolean isFifo() {
        return ((serviceType & FIFO_MESS) != 0);
    }

    /**
	 * Checks if this is a causal message.
	 * 
	 * @return  true if this is a causal message
	 */
    public boolean isCausal() {
        return ((serviceType & CAUSAL_MESS) != 0);
    }

    /**
	 * Checks if this is an agreed message.
	 * 
	 * @return  true if this is an agreed message
	 */
    public boolean isAgreed() {
        return ((serviceType & AGREED_MESS) != 0);
    }

    /**
	 * Checks if this is a safe message.
	 * 
	 * @return  true if this is a safe message
	 */
    public boolean isSafe() {
        return ((serviceType & SAFE_MESS) != 0);
    }

    /**
	 * Checks if this is a self-discard message.
	 * 
	 * @return  true if this is a self-discard message
	 */
    public boolean isSelfDiscard() {
        return ((serviceType & SELF_DISCARD) != 0);
    }

    /**
	 * Sets the service type.  The service type is a bitfield representing the type of message.
	 * 
	 * @param  serviceType  the new service type
	 */
    public void setServiceType(int serviceType) {
        this.serviceType = serviceType;
    }

    /**
	 * Sets the message to be unreliable.
	 */
    public void setUnreliable() {
        serviceType &= ~REGULAR_MESS;
        serviceType |= UNRELIABLE_MESS;
    }

    /**
	 * Sets the message to be reliable.  This is the default type for a new outgoing message.
	 */
    public void setReliable() {
        serviceType &= ~REGULAR_MESS;
        serviceType |= RELIABLE_MESS;
    }

    /**
	 * Sets the message to be fifo.
	 */
    public void setFifo() {
        serviceType &= ~REGULAR_MESS;
        serviceType |= FIFO_MESS;
    }

    /**
	 * Sets the message to be causal.
	 */
    public void setCausal() {
        serviceType &= ~REGULAR_MESS;
        serviceType |= CAUSAL_MESS;
    }

    /**
	 * Sets the message to be agreed.
	 */
    public void setAgreed() {
        serviceType &= ~REGULAR_MESS;
        serviceType |= AGREED_MESS;
    }

    /**
	 * Sets the message to be safe.
	 */
    public void setSafe() {
        serviceType &= ~REGULAR_MESS;
        serviceType |= SAFE_MESS;
    }

    /**
	 * If <code>selfDiscard</code> is true, sets the self discard flag for the message, otherwise
	 * clears the flag.  If the self discard flag is set, the message will not be received at the 
	 * connection it is multicast on.
	 * 
	 * @param  selfDiscard  if true, set the self discard flag, if false, clear the self discard flag
	 */
    public void setSelfDiscard(boolean selfDiscard) {
        if (selfDiscard) {
            serviceType |= SELF_DISCARD;
        } else {
            serviceType &= ~SELF_DISCARD;
        }
    }

    /**
	 * Gets an array containing the SpreadGroup's to which this message was sent.
	 * 
	 * @return  the groups to which this message was sent
	 */
    public SpreadGroup[] getGroups() {
        SpreadGroup[] groupArray = new SpreadGroup[groups.size()];
        for (int i = 0; i < groupArray.length; i++) {
            groupArray[i] = (SpreadGroup) groups.elementAt(i);
        }
        return groupArray;
    }

    /**
	 * Gets the message sender's private group.  This can be used to uniquely identify
	 * the sender on the connection this message was received from or to send
	 * a reply to the sender.
	 */
    public SpreadGroup getSender() {
        return sender;
    }

    /**
	 * Gets the message data as an array of bytes.  This can be used no matter how
	 * the message was sent, but is usually used when the message was sent using
	 * {@link SpreadMessage#setData(byte[])} or from an application using the C library.
	 * 
	 * @return  the message data
	 * @see  SpreadMessage#setData(byte[])
	 */
    public byte[] getData() {
        if (content == CONTENT_DIGEST) {
            data = digestBytes.toByteArray();
        }
        return data;
    }

    /**
	 * Gets the message data as a java object.  The message data should have been set
	 * using {@link SpreadMessage#setObject(Serializable)}.  Regardless of the type of
	 * object passed to {@link SpreadMessage#setObject(Serializable)}, this method returns
	 * an object of type Object, so it must be cast to the correct type.
	 * 
	 * @return  the message data as an object
	 * @throws  SpreadException  if there is an error reading the object
	 * @see SpreadMessage#setObject(Serializable)
	 */
    public Object getObject() throws SpreadException {
        ByteArrayInputStream objectBytes = new ByteArrayInputStream(data);
        ObjectInputStream objectInput;
        try {
            objectInput = new ObjectInputStream(objectBytes);
        } catch (IOException e) {
            throw new SpreadException("ObjectInputStream(): " + e);
        }
        Object object;
        try {
            object = objectInput.readObject();
        } catch (ClassNotFoundException e) {
            throw new SpreadException("readObject(): " + e);
        } catch (IOException e) {
            throw new SpreadException("readObject(): " + e);
        }
        try {
            objectInput.close();
            objectBytes.close();
        } catch (IOException e) {
            throw new SpreadException("close/close(): " + e);
        }
        return object;
    }

    /**
	 * Gets the message data as a digest.  The message data should have been set using
	 * {@link SpreadMessage#digest(Serializable)}.  This method returns a Vector containing
	 * all of the objects passed to {@link SpreadMessage#digest(Serializable)}, in the order
	 * they were passed.
	 * 
	 * @return  the message data as a list of objects
	 * @throws  SpreadException  if there is an error reading the objects
	 * @see  SpreadMessage#digest(Serializable)
	 */
    public Vector getDigest() throws SpreadException {
        Vector objects = new Vector();
        ByteArrayInputStream objectBytes = new ByteArrayInputStream(data);
        ObjectInputStream objectInput;
        try {
            objectInput = new ObjectInputStream(objectBytes);
        } catch (IOException e) {
            throw new SpreadException("ObjectInputStream(): " + e);
        }
        try {
            while (true) {
                objects.addElement(objectInput.readObject());
            }
        } catch (EOFException e) {
        } catch (ClassNotFoundException e) {
            throw new SpreadException("readObject(): " + e);
        } catch (IOException e) {
            throw new SpreadException("readObject(): " + e);
        }
        try {
            objectInput.close();
            objectBytes.close();
        } catch (IOException e) {
            throw new SpreadException("close/close(): " + e);
        }
        return objects;
    }

    /**
	 * Gets the message type.  The message type is set with {@link SpreadMessage#setType(short)}.
	 * 
	 * @return  the message type
	 * @see  SpreadMessage#setType(short)
	 */
    public short getType() {
        return type;
    }

    /**
	 * Checks for an endian mismatch.  If there is an endian mismatch between the machine that sent
	 * the message and the local machine, this is true.  This is a signal to the application so that
	 * it can handle endian flips in its message data.  Aside from the message data, spread handles
	 * all other endian mismatches itself (for example, the message type).
	 * 
	 * @return  true if there is an endian mismatch
	 */
    public boolean getEndianMismatch() {
        return endianMismatch;
    }

    /**
	 * Adds this group to the list this message will be sent to.  When the message is multicast, 
	 * all members of this group will receive it.
	 * 
	 * @param  group  a group to send this message to
	 */
    public void addGroup(SpreadGroup group) {
        groups.addElement(group);
    }

    /**
	 * Adds these groups to the list this message will be sent to.  When the message is multicast, 
	 * all members of these groups will receive it.
	 * 
	 * @param  groups  a list of groups to send this message to
	 */
    public void addGroups(SpreadGroup groups[]) {
        int len = groups.length;
        for (int i = 0; i < len; i++) this.groups.addElement(groups);
    }

    /**
	 * Adds this group to the list this message will be sent to.  When the message is multicast, 
	 * all members of this group will receive it.
	 * 
	 * @param  group  a group to send this message to
	 */
    public void addGroup(String group) {
        SpreadGroup spreadGroup = new SpreadGroup(null, group);
        addGroup(spreadGroup);
    }

    /**
	 * Adds these groups to the list this message will be sent to.  When the message is multicast, 
	 * all members of these groups will receive it.
	 * 
	 * @param  groups  a list of groups to send this message to
	 */
    public void addGroups(String groups[]) {
        int len = groups.length;
        for (int i = 0; i < len; i++) {
            SpreadGroup spreadGroup = new SpreadGroup(null, groups[i]);
            addGroup(spreadGroup);
        }
    }

    /**
	 * Sets the message's data to this array of bytes.  This cancels any previous calls to
	 * {@link SpreadMessage#setData(byte[])}, {@link SpreadMessage#setObject(Serializable)},
	 * {@link SpreadMessage#digest(Serializable)}.
	 * 
	 * @param  data  the new message data
	 * @see  SpreadMessage#getData()
	 */
    public void setData(byte[] data) {
        digestBytes = null;
        digestOutput = null;
        content = CONTENT_DATA;
        this.data = (byte[]) data.clone();
    }

    /**
	 * Sets the message's data to this object, in serialized form.  The object must support
	 * the Serializable interface to use this method.  This cancels any previous calls to
	 * {@link SpreadMessage#setData(byte[])}, {@link SpreadMessage#setObject(Serializable)},
	 * {@link SpreadMessage#digest(Serializable)}.  This should not be used if an application using
	 * the C library needs to read this message.
	 * 
	 * @param  object  the object to set the data to
	 * @see  SpreadMessage#getObject()
	 */
    public void setObject(Serializable object) throws SpreadException {
        digestBytes = null;
        digestOutput = null;
        content = CONTENT_OBJECT;
        ByteArrayOutputStream objectBytes = new ByteArrayOutputStream();
        ObjectOutputStream objectOutput;
        try {
            objectOutput = new ObjectOutputStream(objectBytes);
        } catch (IOException e) {
            throw new SpreadException("ObjectOutputStream(): " + e);
        }
        try {
            objectOutput.writeObject(object);
            objectOutput.flush();
        } catch (IOException e) {
            throw new SpreadException("writeObject/flush(): " + e);
        }
        data = objectBytes.toByteArray();
        try {
            objectOutput.close();
            objectBytes.close();
        } catch (IOException e) {
            throw new SpreadException("close/close(): " + e);
        }
    }

    /**
	 * Adds this message to the digest.  The object must support
	 * the Serializable interface to use this method.  This cancels any previous calls to
	 * {@link SpreadMessage#setData(byte[])} or {@link SpreadMessage#setObject(Serializable)}.
	 * This should not be used if an application using the C library needs to read this message.
	 * When the message is sent, all of the objects that have been passed to this method get
	 * sent as the message data.
	 * 
	 * @param  object  the object to add to the digets
	 * @see  SpreadMessage#getDigest()
	 */
    public void digest(Serializable object) throws SpreadException {
        if (content != CONTENT_DIGEST) {
            content = CONTENT_DIGEST;
            digestBytes = new ByteArrayOutputStream();
            try {
                digestOutput = new ObjectOutputStream(digestBytes);
            } catch (IOException e) {
                throw new SpreadException("ObjectOutputStream(): " + e);
            }
        }
        try {
            digestOutput.writeObject(object);
            digestOutput.flush();
        } catch (IOException e) {
            throw new SpreadException("writeObject/flush(): " + e);
        }
    }

    /**
	 * Set's the message type.  This is a 16-bit integer that can be used by the application
	 * to identify the message.
	 * 
	 * @param  type  the message type
	 * @see  SpreadMessage#getType()
	 */
    public void setType(short type) {
        this.type = type;
    }

    /**
	 * Get the membership info for this message.  This should only be called if this is a
	 * membership message ({@link SpreadMessage#isMembership()} is true).
	 * 
	 * @return  the membership info for this message
	 */
    public MembershipInfo getMembershipInfo() {
        return membershipInfo;
    }

    /**
	 * Creates a copy of this message.
	 * 
	 * @return  a copy of this message
	 */
    public Object clone() {
        SpreadMessage message = new SpreadMessage();
        message.setServiceType(serviceType);
        message.groups = (Vector) groups.clone();
        message.setType(type);
        message.setData(data);
        message.content = content;
        try {
            if (content == CONTENT_DIGEST) {
                message.digestBytes = new ByteArrayOutputStream();
                message.digestBytes.write(digestBytes.toByteArray());
                message.digestOutput = new ObjectOutputStream(message.digestBytes);
            }
        } catch (IOException e) {
        }
        return message;
    }
}
