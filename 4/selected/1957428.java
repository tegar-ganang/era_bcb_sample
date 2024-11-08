package com.volantis.mps.recipient;

import com.volantis.mps.localization.LocalizationFactory;
import com.volantis.synergetics.localization.ExceptionLocalizer;
import com.volantis.synergetics.log.LogDispatcher;
import javax.mail.internet.InternetAddress;

/**
 * This class represents the recipient of a {@link
 * com.volantis.mps.message.MultiChannelMessage MultiChannelMessage}. The
 * recipient should be supplied with an address or phone number as a minimum.
 * If a device or channel name are not supplied these will be resolved as
 * needed by the Message Preparation Server.
 *
 * @volantis-api-include-in PublicAPI
 */
public class MessageRecipient implements Cloneable {

    private static final String mark = "(c) Volantis Systems Ltd 2002.";

    /**
     * The logger to use
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(MessageRecipient.class);

    /**
     * The exception localizer instance for this class.
     */
    private static final ExceptionLocalizer localizer = LocalizationFactory.createExceptionLocalizer(MessageRecipient.class);

    /**
     * The device/channel was not resolved.
     */
    public static final int NOT_RESOLVED = 1;

    /**
     * The device/channel was succesfully resolved.
     */
    public static final int OK = 0;

    /**
     * This is a TO recipient.
     */
    static final int TO_RECIPIENT = 0;

    /**
     * This is a CC recipient.
     */
    static final int CC_RECIPIENT = 1;

    /**
     * This is a BCC recipient.
     */
    static final int BCC_RECIPIENT = 2;

    /**
     * The internet address of this recipient.
     */
    private InternetAddress address;

    /**
     * The phone number for this recipient.
     */
    private String msISDN;

    /**
     * The device for this recipient.
     */
    private String deviceName;

    /**
     * The communication channel to use for this recipient.
     */
    private String channelName;

    /**
     * The class to use to resolve device/channel.
     */
    private MessageRecipientInfo messageRecipientInfo;

    /**
     * The type of recipient e.g TO, CC or BCC.
     */
    private int recipientType;

    /**
     * Holds a pre-localized reason for a failure to send a message to this
     * recipient. Will be null if there was no failure to send.
     */
    private String failureReason = null;

    /**
     * Helper class for this class
     */
    private MessageRecipientHelper helper = MessageRecipientHelper.getInstance();

    /**
     * Creates a new instance of MessegeRecipient.
     * <p>
     *
     * @throws RecipientException if there were problems creating the instance.
     */
    public MessageRecipient() throws RecipientException {
        messageRecipientInfo = helper.getMessageRecipientInfo();
    }

    /**
     * Creates a new instance of MessegeRecipient.
     * <p>
     *
     * @param address    The recipient's internet email address.
     * @param deviceName The name of the device for the recipient.
     * @throws RecipientException if there were problems creating the instance.
     */
    public MessageRecipient(InternetAddress address, String deviceName) throws RecipientException {
        this();
        this.address = address;
        this.deviceName = deviceName;
    }

    /**
     * Sets the internet email address for the recipient.
     * <p>
     *
     * @param address The recipient's internet email address.
     * @throws RecipientException if there were problems setting the address.
     */
    public void setAddress(InternetAddress address) throws RecipientException {
        this.address = address;
    }

    /**
     * Gets the internet email address of the recipient.
     * <p>
     *
     * @return The recipient's internet email address.
     * @throws RecipientException if there were problems retrieving the address.
     */
    public InternetAddress getAddress() throws RecipientException {
        return address;
    }

    /**
     * Sets the phone number for the recipient.
     * <p>
     *
     * @param msISDN The recipient's phone number.
     * @throws RecipientException if there were problems setting the number.
     */
    public void setMSISDN(String msISDN) throws RecipientException {
        this.msISDN = msISDN;
    }

    /**
     * Gets the phone number of the recipient.
     * <p>
     *
     * @return The recipient's phone number.
     * @throws RecipientException if there were problems retrieving the number.
     */
    public String getMSISDN() throws RecipientException {
        return msISDN;
    }

    /**
     * Sets the device for the recipient.
     * <p>
     *
     * @param deviceName The recipient's device.
     * @throws RecipientException if there were problems setting the device.
     */
    public void setDeviceName(String deviceName) throws RecipientException {
        this.deviceName = deviceName;
    }

    /**
     * Gets the device of the recipient.
     * <p>
     *
     * @return The recipient's device.
     * @throws RecipientException if there were problems retrieving the device.
     */
    public String getDeviceName() throws RecipientException {
        return deviceName;
    }

    /**
     * Sets the channel for the recipient.
     * <p>
     *
     * @param channelName The recipient's channel.
     * @throws RecipientException if there were problems setting the channel.
     */
    public void setChannelName(String channelName) throws RecipientException {
        this.channelName = channelName;
    }

    /**
     * Gets the channel of the recipient.
     * <p>
     *
     * @return The recipient's channel.
     * @throws RecipientException if there were problems retrieving the channel.
     */
    public String getChannelName() throws RecipientException {
        return channelName;
    }

    /**
     * Set the type of recipient.
     * <p>
     *
     * @param recipientType The type of recipient. Must be one of:
     *                      <ul>
     *                      <li><code>TO_RECIPIENT</code></li>
     *                      <li><code>CC_RECIPIENT</code></li>
     *                      <li><code>BCC_RECIPIENT</code></li>
     *                      </ul>
     * @throws RecipientException if there were problems setting the type.
     */
    void setRecipientType(int recipientType) throws RecipientException {
        this.recipientType = recipientType;
    }

    /**
     * Gets the type of recipient.
     * <p>
     *
     * @return The recipient's type.
     * @throws RecipientException if there were problems retrieving the type.
     */
    int getRecipientType() throws RecipientException {
        return recipientType;
    }

    /**
     * Resolves the device for this recipient using the user-supplied
     * <code>RecipientInfo</code>.
     * <p>
     *
     * @param force If true the resolved value will overwrite any existing
     *              device name.
     * @return <code>OK</code> if the device name was resolved successfully, or
     *         <code>NOT_RESOLVED</code> if unsuccessful.
     */
    public int resolveDeviceName(boolean force) throws RecipientException {
        if ((deviceName == null) || force) {
            if (messageRecipientInfo != null) {
                deviceName = messageRecipientInfo.resolveDeviceName(this);
            }
            if (deviceName == null) {
                return NOT_RESOLVED;
            }
        }
        return OK;
    }

    /**
     * Resolves the channel for this recipient using the user-supplied
     * <code>RecipientInfo</code>.
     * <p>
     *
     * @param force If true the resolved value will overwrite any existing
     *              channel name.
     * @return <code>OK</code> if the channel name was resolved successfully, or
     *         <code>NOT_RESOLVED</code> if unsuccessful.
     */
    public int resolveChannelName(boolean force) throws RecipientException {
        if ((channelName == null) || force) {
            if (messageRecipientInfo != null) {
                channelName = messageRecipientInfo.resolveChannelName(this);
            }
            if (channelName == null) {
                return NOT_RESOLVED;
            }
        }
        return OK;
    }

    /**
     * Returns the failure reason associated with the recipient.
     *
     * @return the failure reason or null if there is none
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * Records the given failure reason against this recipient. This should be
     * used by the {@link com.volantis.mps.channels.MessageChannel
     * MessageChannel} implementations when a send to this recipient fails.
     *
     * @param failureReason the failure reason message to be recorded against
     *                      the recipient. If localization is required this
     *                      should be a pre-localized message
     */
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    /**
     * Returns a copy of this MessageRecipient, in line with the
     * weakly-specified contract described in {@link Object#clone()}. The
     * visibility of the method has been increased to <code>public</code>,
     * rather than the <code>protected</code> declaration in the superclass.
     *
     * @return a copy of this MessageRecipient.
     * @throws CloneNotSupportedException if the clone operation cannot be
     *                                    performed.
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public boolean equal(Object o) {
        MessageRecipient mr1 = this;
        MessageRecipient mr2 = (MessageRecipient) o;
        boolean result;
        try {
            String key1 = generateKeyRepresentation(mr1);
            if (mr2 != null) {
                String key2 = generateKeyRepresentation(mr2);
                System.err.println(key1 + "-" + key2);
                result = (key1.compareTo(key2) == 0);
            } else {
                return false;
            }
        } catch (RecipientException e) {
            logger.error(localizer.format("comparison-failure"), e);
            result = false;
        }
        return result;
    }

    /**
     * Return a String representation of the specified MessageRecipient.
     *
     * @param mr a MessageRecipient - not null.
     * @return a String.
     * @throws RecipientException if there was a problem reading the
     *                            MessageRecipient fields.
     */
    private static String generateKeyRepresentation(MessageRecipient mr) throws RecipientException {
        StringBuffer buf = new StringBuffer();
        buf.append(mr.getChannelName());
        buf.append(mr.getDeviceName());
        buf.append(mr.getAddress());
        buf.append(mr.getMSISDN());
        return buf.toString();
    }
}
