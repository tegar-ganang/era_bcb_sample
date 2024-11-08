package com.volantis.mps.channels;

import com.volantis.mps.attachment.AttachmentUtilities;
import com.volantis.mps.localization.LocalizationFactory;
import com.volantis.mps.message.MessageException;
import com.volantis.mps.message.MessageInternals;
import com.volantis.mps.message.MultiChannelMessage;
import com.volantis.mps.recipient.MessageRecipient;
import com.volantis.mps.recipient.MessageRecipients;
import com.volantis.mps.recipient.RecipientException;
import com.volantis.synergetics.localization.MessageLocalizer;
import com.volantis.synergetics.log.LogDispatcher;
import java.util.HashMap;
import java.util.Iterator;

/**
 * <p>This abstract class describes the interface for defining protocol-specific
 * adapters for sending messages via specific protocols.</p>
 *
 * <p><strong>Implementations must be designed for use in a multi-threaded
 * environment.</strong></p>
 * @volantis-api-include-in PublicAPI
 */
public abstract class MessageChannel {

    /**
     * The logger to use
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(MessageChannel.class);

    private static final MessageLocalizer messageLocalizer = LocalizationFactory.createMessageLocalizer(MessageChannel.class);

    /**
     * The channel name representing the current channel.
     */
    protected String channelName;

    /**
     * A cache or already channelised MultiChannelMessages.
     */
    HashMap cache;

    /**
     * Sends the message via the channel.
     * <p>
     * @param multiChannelMessage The message.
     * @param messageRecipients   The list of recipients.
     * @param messageSender       The sender of the message.
     *
     * @return A list of failed recipients.
     *
     * @throws RecipientException if a problem occurred during recipient
     *                            processing.
     * @throws MessageException   if a problem occurred during message
     *                            processing.
     */
    public MessageRecipients send(MultiChannelMessage multiChannelMessage, MessageRecipients messageRecipients, MessageRecipient messageSender) throws RecipientException, MessageException {
        MultiChannelMessage mcmClone = getCached(multiChannelMessage);
        if (mcmClone == null) {
            mcmClone = (MultiChannelMessage) multiChannelMessage.clone();
        }
        if (mcmClone != null) {
            try {
                mcmClone.addAttachments(AttachmentUtilities.getAttachmentsForChannel(getChannelName(), MessageInternals.getAttachments(multiChannelMessage)));
                cache.put(multiChannelMessage, mcmClone);
            } catch (Exception e) {
                logger.error("multi-channel-message-channelise-attachments-failure", e);
            }
        } else {
            logger.error("multi-channel-message-clone-failure-send-failed-for", getChannelName());
            return messageRecipients;
        }
        MessageRecipients failures = new MessageRecipients();
        MessageRecipients deviceSpecificRecips = new MessageRecipients();
        String currentDeviceName = null;
        Iterator recipientsIterator = messageRecipients.getIterator();
        while (recipientsIterator.hasNext()) {
            MessageRecipient recipient = (MessageRecipient) recipientsIterator.next();
            if (recipient.resolveDeviceName(false) != MessageRecipient.OK) {
                logger.warn("device-resolution-failed-for", recipient);
                recipient.setFailureReason(messageLocalizer.format("device-resolution-failed-for", recipient));
                failures.addRecipient(recipient);
                continue;
            }
            if (recipient.getChannelName().equals(channelName)) {
                if (currentDeviceName != null) {
                    if (!currentDeviceName.equals(recipient.getDeviceName())) {
                        boolean failed = false;
                        Exception cause = null;
                        try {
                            MessageRecipients localFails = sendImpl(multiChannelMessage, deviceSpecificRecips, messageSender);
                            Iterator lfIterator = localFails.getIterator();
                            while (lfIterator.hasNext()) {
                                failures.addRecipient((MessageRecipient) lfIterator.next());
                            }
                        } catch (MessageException me) {
                            failed = true;
                            cause = me;
                        } catch (RecipientException re) {
                            failed = true;
                            cause = re;
                        } finally {
                            if (failed) {
                                populateFailures(deviceSpecificRecips, failures, cause);
                                continue;
                            }
                        }
                        deviceSpecificRecips = new MessageRecipients();
                        currentDeviceName = recipient.getDeviceName();
                        deviceSpecificRecips.addRecipient(recipient);
                    } else {
                        deviceSpecificRecips.addRecipient(recipient);
                    }
                } else {
                    currentDeviceName = recipient.getDeviceName();
                    deviceSpecificRecips = new MessageRecipients();
                    deviceSpecificRecips.addRecipient(recipient);
                }
            }
        }
        boolean failed = false;
        Exception cause = null;
        try {
            MessageRecipients localFails = sendImpl(multiChannelMessage, deviceSpecificRecips, messageSender);
            Iterator lfIterator = localFails.getIterator();
            while (lfIterator.hasNext()) {
                failures.addRecipient((MessageRecipient) lfIterator.next());
            }
        } catch (MessageException me) {
            failed = true;
            cause = me;
        } catch (RecipientException re) {
            failed = true;
            cause = re;
        } finally {
            if (failed) {
                populateFailures(deviceSpecificRecips, failures, cause);
            }
        }
        return failures;
    }

    /**
     * Takes the device specific recipients listed and adds them to the
     * specified failures list, setting each recipient's failure exception
     * to the given causative exception.
     *
     * @param deviceSpecificRecips the device-specific recipients for
     *                             which sends have failed
     * @param failures             the resulting set of failed recipients
     * @param cause                the exception that caused the failure
     * @throws RecipientException if there is a problem populating the
     *                            failures list
     */
    private void populateFailures(MessageRecipients deviceSpecificRecips, MessageRecipients failures, Exception cause) throws RecipientException {
        Iterator fIterator = deviceSpecificRecips.getIterator();
        while (fIterator.hasNext()) {
            MessageRecipient failedRecipient = (MessageRecipient) fIterator.next();
            String address = getAddress(failedRecipient);
            if (cause instanceof MessageException) {
                logger.warn("message-send-failed-to", address);
            } else {
                logger.warn("recipient-exception-for", address);
            }
            if ((failedRecipient.getFailureReason() == null) && (cause != null)) {
                failedRecipient.setFailureReason(cause.getMessage());
            }
            failures.addRecipient(failedRecipient);
        }
    }

    /**
     * Utility method to obtain an address for a recipient.
     *
     * @param recipient The recipient to resolve
     *
     * @return A <code>String</code> representing the specified recipient's
     *         <code>InternetAddress</code> or <code>MSISDN</code>
     *
     * @throws RecipientException if there was a problem getting the address
     */
    private String getAddress(MessageRecipient recipient) throws RecipientException {
        if (recipient.getAddress() != null) {
            return recipient.getAddress().getAddress();
        } else {
            return recipient.getMSISDN();
        }
    }

    /**
     * Sends the message via the channel.
     * <p>
     * @param multiChannelMessage The message.
     * @param messageRecipients   The list of recipients.
     * @param messageSender       The sender of the message.
     *
     * @return A list of failed recipients.
     *
     * @throws RecipientException if a problem occurred during recipient
     *                            processing.
     * @throws MessageException   if a problem occurred during message
     *                            processing.
     */
    protected abstract MessageRecipients sendImpl(MultiChannelMessage multiChannelMessage, MessageRecipients messageRecipients, MessageRecipient messageSender) throws RecipientException, MessageException;

    /**
     * Retrieves the channel name being used. This can be used to confirm that
     * messages are being sent to the correct channel.
     * <p>
     * @return the name of the channel
     */
    public String getChannelName() {
        return channelName;
    }

    private MultiChannelMessage getCached(MultiChannelMessage nonChannelised) {
        if (cache == null) {
            cache = new HashMap();
            return null;
        }
        return (MultiChannelMessage) cache.get(nonChannelised);
    }

    /**
     * Close any resources managed by this message channel.
     */
    public void close() {
    }
}
