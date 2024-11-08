package javax.darjeeling.radio;

/**
 *
 * API for working with the radio. We assume that the device has either one radio or none at all. The radio is
 * controlled through static methods, and has to be initialised before use. If the device does not contain a 
 * radio, a RadioNotInitialisedException will be thrown upon init.
 * 
 * The behavior or certain functions, such as setting the desired channel, is undefined until after the radio has
 * been initialised. They may either throw a RadioNotInitialisedException, or display unknown behavior depending 
 * on the native implementation.
 * 
 * @author Niels Brouwers
 *
 */
public class Radio {

    /** Indicates no flags. */
    public static short FLAGS_NONE = 0;

    /** Indicates that low power listening should be enabled, if supported. */
    public static short FLAGS_LOWPOWERLISTENING = 1;

    /** Minimum transmission power */
    public static short POWER_MIN = 0;

    /** Maximum transmission power */
    public static short POWER_MAX = 10000;

    private static Object sendLock, receiveLock;

    private Radio() {
    }

    private static native void _waitForMessage();

    private static native byte[] _readBytes();

    private static native void _init(int flags);

    private static native byte _getNumMessages();

    /**
	 * @return true iff the device has a radio on it.
	 */
    public static native boolean hasRadio();

    /**
	 * Sets the radio channel.
	 * @param channel radio channel
	 */
    public static native void setChannel(short channel);

    /**
	 * @return the number of the first channel this radio is able to work with. Does not necessarily have to be zero.
	 */
    public static native short getFirstChannel();

    /**
	 * @return the number of the last channel this radio is able to work with. 
	 */
    public static native short getLastChannel();

    /**
	 * Sets the transmission output power. 
	 * @param power desired output power. Ranges from 0 (minimum) to 1000 (maximum).
	 */
    public static native void setOutputPower(short power);

    /**
	 * @return the number of channels this radio is able to work with. Defined as getLastChannel() - getFirstChannel().
	 */
    public static short getChannelCount() {
        return (short) (getLastChannel() - getFirstChannel());
    }

    /**
	 * Returns the maximum packet size in bytes. Trying to transmit a packet larger that this will result in a PacketLengthExceededException.
	 * @return maximum packet size.
	 */
    public static native short getMaxMessageLength();

    /**
	 * Initialises the radio. The radio will not work properly until this method is called. Flags may be passed to it to enable certain
	 * properties such as low-power listening.
	 * @param flags bit-string containing initialisation flags. 
	 */
    public static void init(int flags) {
        _init(flags);
        sendLock = new Object();
        receiveLock = new Object();
    }

    /**
	 * Initialises the radio. The radio will not work properly until this method is called. This is a convenience method that calls init with FLAGS_NONE.
	 */
    public static void init() {
        init(FLAGS_NONE);
    }

    /**
	 * Returns the send lock object. This allows threads to synchronise on the sending lock, to avoid entering some critical section while another thread is
	 * busy sending a packet over the radio.
	 * @return returns the send lock object.
	 */
    public static Object getSendLock() {
        return sendLock;
    }

    /**
	 * @return number of packets currently in the receive buffer. 
	 */
    public static byte getNumMessages() {
        return _getNumMessages();
    }

    /**
	 * Receives a packet over the radio. The method blocks the current thread if there is no packet to dequeue from the receive buffer. 
	 * @return a byte array containing the packet payload.
	 */
    public static byte[] receive() {
        if (receiveLock == null) throw new RadioNotInitialisedException();
        synchronized (receiveLock) {
            _waitForMessage();
            byte[] data = _readBytes();
            return data;
        }
    }

    private static native void _broadcast(byte[] data);

    /**
	 * Broadcasts a message.
	 * @param data message payload.
	 */
    public static void broadcast(byte[] data) {
        if (sendLock == null) throw new RadioNotInitialisedException();
        if (data == null) throw new NullPointerException();
        synchronized (sendLock) {
            _broadcast(data);
        }
    }

    private static native boolean _send(short receiverId, byte[] data);

    /**
	 * Unicast transmit. 
	 * @param receiverId id of the receiver node.
	 * @param data message payload.
	 * @return true if sending is successful
	 */
    public static boolean send(short receiverId, byte[] data) {
        if (sendLock == null) throw new RadioNotInitialisedException();
        if (data == null) throw new NullPointerException();
        synchronized (sendLock) {
            return _send(receiverId, data);
        }
    }
}
