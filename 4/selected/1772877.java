package com.volantis.mps.recipient;

import com.volantis.synergetics.log.LogDispatcher;
import com.volantis.synergetics.localization.ExceptionLocalizer;
import com.volantis.mps.localization.LocalizationFactory;

/**
 * This comparator ensures that MessageRecipients are sorted by channel and 
 * device to enable sequential processing of channels and devices.
 */
public class MessageRecipientComparator implements java.util.Comparator {

    private static final String mark = "(c) Volantis Systems Ltd 2002.";

    /**
     * The logger to use
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(MessageRecipientComparator.class);

    /**
     * The exception localizer instance for this class.
     */
    private static final ExceptionLocalizer localizer = LocalizationFactory.createExceptionLocalizer(MessageRecipientComparator.class);

    /** Creates a new instance of MessageRecipientComparator */
    public MessageRecipientComparator() {
    }

    public int compare(Object o1, Object o2) {
        MessageRecipient mr1 = (MessageRecipient) o1;
        MessageRecipient mr2 = (MessageRecipient) o2;
        int result;
        if (mr1 != null) {
            try {
                StringBuffer key1 = new StringBuffer();
                key1.append(mr1.getChannelName());
                key1.append(mr1.getDeviceName());
                key1.append(mr1.getAddress());
                key1.append(mr1.getMSISDN());
                if (mr2 != null) {
                    StringBuffer key2 = new StringBuffer();
                    key2.append(mr2.getChannelName());
                    key2.append(mr2.getDeviceName());
                    key2.append(mr2.getAddress());
                    key2.append(mr2.getMSISDN());
                    result = key1.toString().compareTo(key2.toString());
                } else {
                    return 1;
                }
            } catch (RecipientException e) {
                logger.error(localizer.format("comparison-failure"), e);
                result = -1;
            }
        } else {
            return -1;
        }
        return result;
    }
}
