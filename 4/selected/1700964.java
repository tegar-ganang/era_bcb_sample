package com.volantis.mps.recipient;

/**
 *
 * @author  ianw
 */
public class DefaultRecipientResolver implements MessageRecipientInfo {

    private static final String mark = "(c) Volantis Systems Ltd 2002.";

    protected static final String SMTP_CHANNEL = "smtp";

    protected static final String MMSC_CHANNEL = "nokia-mmsc";

    protected static final String SMSC_CHANNEL = "smpp-smsc";

    /** Creates a new instance of DefaultRecipientResolver */
    public DefaultRecipientResolver() {
    }

    public String resolveChannelName(MessageRecipient recipient) {
        try {
            if (null != recipient.getChannelName()) {
                return recipient.getChannelName();
            }
        } catch (RecipientException e) {
        }
        String deviceName = resolveDeviceName(recipient);
        if (deviceName != null) {
            String messageProtocol = ResolverUtilities.getDeviceMessageProtocol(deviceName);
            if ("MHTML".equals(messageProtocol)) {
                return SMTP_CHANNEL;
            } else if ("MMS".equals(messageProtocol)) {
                return MMSC_CHANNEL;
            } else if ("SMS".equals(messageProtocol)) {
                return SMSC_CHANNEL;
            }
        }
        return null;
    }

    public String resolveDeviceName(MessageRecipient recipient) {
        try {
            if (null != recipient.getDeviceName()) {
                return recipient.getDeviceName();
            }
        } catch (RecipientException e) {
        }
        return "Outlook";
    }

    public String resolveCharacterEncoding(MessageRecipient recipient) {
        return null;
    }
}
