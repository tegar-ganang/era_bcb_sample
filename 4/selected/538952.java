package com.volantis.mps.session;

import com.volantis.mcs.runtime.configuration.MpsChannelConfiguration;
import com.volantis.mcs.runtime.configuration.MpsPluginConfiguration;
import com.volantis.mcs.runtime.Volantis;
import com.volantis.mps.channels.MessageChannel;
import com.volantis.mps.localization.LocalizationFactory;
import com.volantis.mps.message.MessageException;
import com.volantis.mps.message.MultiChannelMessage;
import com.volantis.mps.recipient.MessageRecipient;
import com.volantis.mps.recipient.MessageRecipients;
import com.volantis.mps.recipient.RecipientException;
import com.volantis.mps.recipient.RecipientInternals;
import com.volantis.synergetics.log.LogDispatcher;
import com.volantis.synergetics.localization.ExceptionLocalizer;
import com.volantis.synergetics.localization.MessageLocalizer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <p>This class provides the sending capabilities of the Message Preparation
 * Server.</p>
 *
 * <p>The Session statically initialises the {@link MessageChannel} instances
 * which MPS then uses to service all requests, so all {@link MessageChannel}
 * implementations <strong>must</strong> be thread safe.</p>
 *
 * @volantis-api-include-in PublicAPI
 */
public class Session {

    /**
     * The logger to use
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(Session.class);

    /**
     * The exception localizer instance for this class.
     */
    private static final ExceptionLocalizer localizer = LocalizationFactory.createExceptionLocalizer(Session.class);

    /**
     * Used to obtain localized messages.
     */
    private static final MessageLocalizer messageLocalizer = LocalizationFactory.createMessageLocalizer(Session.class);

    /**
     * The map of recipient list names to <code>MessageRecipients</code>.
     * @deprecated this should be removed when the methods which use it are
     * removed from the public API. Recipient lists should no longer be stored
     * in the Session, but rather specified on send.
     */
    private HashMap recipientsMap = new HashMap();

    /**
     * The map of channel names to instances.
     */
    private static final HashMap CHANNEL_MAP = new HashMap();

    static {
        String channelName = null;
        try {
            Volantis volantisBean = Volantis.getInstance();
            if (volantisBean == null) {
                throw new IllegalStateException("Volantis bean has not been initialised");
            }
            MpsPluginConfiguration config = (MpsPluginConfiguration) volantisBean.getApplicationPluginConfiguration("MPS");
            Iterator channels = config.getChannelsIterator();
            while (channels.hasNext()) {
                MpsChannelConfiguration channelConfig = (MpsChannelConfiguration) channels.next();
                channelName = channelConfig.getName();
                if (logger.isDebugEnabled()) {
                    logger.debug("Initialising channel " + channelName);
                }
                String className = channelConfig.getClassName();
                Class channelClass = Class.forName(className);
                Class parameterTypes[] = { String.class, Map.class };
                Object initArgs[] = { channelName, channelConfig.getArguments() };
                MessageChannel channel = (MessageChannel) channelClass.getConstructor(parameterTypes).newInstance(initArgs);
                CHANNEL_MAP.put(channelName, channel);
            }
        } catch (Exception e) {
            throw new RuntimeException(localizer.format("channel-config-error-for", channelName), e);
        }
    }

    /**
     * Adds a named <code>{@link MessageRecipients}</code> list to the session.
     *
     * @param name the name of the list.
     * @param recipients the <code>MessageRecipients</code> containing the list.
     * @throws RecipientException if an error occurred when adding the list to
     *                            the session.
     * @deprecated MessageRecipients should no longer be stored in the session
     *  - one of the send methods which take a list should be used instead.
     */
    public void addRecipients(String name, MessageRecipients recipients) throws RecipientException {
        recipientsMap.put(name, recipients);
    }

    /**
     * Gets the named recipient list from the session.
     * <p>
     * @param name the name of the list to retrieve.
     * @throws RecipientException if an error occurs whilst retrieving the list
     * @return The <code>MessageRecipients</code> containing the list.
     * @deprecated MessageRecipients should no longer be stored in the session
     *  - one of the send methods which take a list should be used instead.
     */
    public MessageRecipients getRecipients(String name) throws RecipientException {
        return (MessageRecipients) recipientsMap.get(name);
    }

    /**
     * Removes a named <code>MessageRecipients</code> list from the session.
     * <p>
     * @param name the name of the list.
     * @throws RecipientException if an error occurs whilst removing the list.
     * @deprecated MessageRecipients should no longer be stored in the session
     *  - one of the send methods which take a list should be used instead.
     */
    public void removeRecipients(String name) throws RecipientException {
        recipientsMap.remove(name);
    }

    /**
     * Validates that the <code>MessageRecipients</code> can resolve
     * device and channel as well as having either an address or MSISDN.
     * This method also set the internal <code>RecipientType</code> of the
     * <code>MessageRecipients</code> to the type passed in.
     * @param recipients The list of <code>MessageRecipients<code> to process.
     * @param recipientType The <code>RecipientType</code> for these recipients.
     * @return A <code>MessageRecipientsStatusContainer</code> containing the
     * processed (validated) recipients and the failures.
     * @throws MessageException if there was a problem validating the recipients
     */
    private MessageRecipientsStatusContainer validateRecipients(MessageRecipients recipients, int recipientType) throws MessageException {
        MessageRecipientsStatusContainer container = new MessageRecipientsStatusContainer();
        MessageRecipients failures = new MessageRecipients();
        MessageRecipients successes = new MessageRecipients();
        container.setFailures(failures);
        container.setSuccesses(successes);
        MessageRecipient recipient;
        MessageRecipient clone = null;
        Iterator recipientsIterator = recipients.getIterator();
        while (recipientsIterator.hasNext()) {
            recipient = (MessageRecipient) recipientsIterator.next();
            try {
                clone = (MessageRecipient) recipient.clone();
                RecipientInternals.setRecipientType(clone, recipientType);
                int channelStatus = clone.resolveChannelName(false);
                int deviceStatus = clone.resolveDeviceName(false);
                if (clone.getChannelName() != null && clone.getDeviceName() != null) {
                    if (clone.getAddress() != null || clone.getMSISDN() != null) {
                        successes.addRecipient(clone);
                    } else {
                        clone.setFailureReason(messageLocalizer.format("address-resolution-failed-for", clone));
                        failures.addRecipient(clone);
                    }
                } else {
                    if (deviceStatus != MessageRecipient.OK) {
                        clone.setFailureReason(messageLocalizer.format("device-resolution-failed-for", clone));
                    } else if (channelStatus != MessageRecipient.OK) {
                        clone.setFailureReason(messageLocalizer.format("channel-resolution-failed-for", clone));
                    }
                    failures.addRecipient(clone);
                }
            } catch (CloneNotSupportedException e) {
                logger.error("unexpected-clonenotsupportedexception", e);
            } catch (RecipientException rce) {
                try {
                    clone.setFailureReason(rce.getMessage());
                    failures.addRecipient(clone);
                } catch (RecipientException rce2) {
                    final String messageKey = "recipient-exception-caught";
                    logger.error(messageKey);
                    throw new MessageException(localizer.format(messageKey), rce2);
                }
            }
        }
        return container;
    }

    /**
     * Sends a <code>MultiChannelMessage</code> to a single recipient.
     * <p>
     * @param message The <code>MultiChannelMessage</code> to send.
     * @param recipient The <code>MessageRecipient</code> to send this message to.
     * @throws MessageException if an error occurs when sending the message.
     * @return A <code>MessageRecipients</code> list of failed
     *         <code>MessageRecipients</code>.
     */
    public MessageRecipients send(MultiChannelMessage message, MessageRecipient recipient) throws MessageException {
        try {
            if (recipient == null) {
                throw new MessageException(localizer.format("recipient-null-invalid"));
            }
            MessageRecipientsStatusContainer container;
            MessageRecipients recipients = new MessageRecipients();
            recipients.addRecipient(recipient);
            container = validateRecipients(recipients, RecipientInternals.TO_RECIPIENT);
            MessageRecipients sendFailures = sendToChannelAdapters(message, container.getSuccesses(), null);
            MessageRecipients failures = container.getFailures();
            if (sendFailures != null) {
                RecipientInternals.addRecipients(failures, sendFailures);
            }
            return failures;
        } catch (RecipientException e) {
            throw new MessageException(localizer.format("recipient-errors"), e);
        }
    }

    /**
     * Sends a <code>MultiChannelMessage</code> to a named recipient list.
     * <p>
     * @param message The <code>MultiChannelMessage</code> to send.
     * @param toListName The recipient list previously added to the
     *                   <code>Session</code>.
     * @param replyTo The <code>MessageRecipient</code> who will be the sender
     *                of the message.
     * @throws MessageException if an error occurs when sending the message.
     * @return A <code>MessageRecipients</code> list of failed
     *         <code>MessageRecipients</code>.
     * @deprecated use {@link #send(com.volantis.mps.message.MultiChannelMessage, com.volantis.mps.recipient.MessageRecipients, com.volantis.mps.recipient.MessageRecipient)}
     * instead, as this implementation is not guaranteed to be threadsafe
     */
    public MessageRecipients send(MultiChannelMessage message, String toListName, MessageRecipient replyTo) throws MessageException {
        MessageRecipients recipients = (MessageRecipients) recipientsMap.get(toListName);
        return send(message, recipients, replyTo);
    }

    /**
     * Sends a <code>MultiChannelMessage</code> to a named recipient list.
     * <p>
     * @param message       the <code>MultiChannelMessage</code> to send.
     * @param recipients    to which this message should be sent
     * @param replyTo       the <code>MessageRecipient</code> who will be the
     *                      sender of the message.
     * @throws MessageException if an error occurs when sending the message.
     * @return A <code>MessageRecipients</code> list of failed
     *         <code>MessageRecipients</code>.
     */
    public MessageRecipients send(MultiChannelMessage message, MessageRecipients recipients, MessageRecipient replyTo) throws MessageException {
        if (recipients == null) {
            throw new MessageException(localizer.format("recipient-list-null-invalid"));
        }
        MessageRecipientsStatusContainer container;
        container = validateRecipients(recipients, RecipientInternals.TO_RECIPIENT);
        MessageRecipients sendFailures = sendToChannelAdapters(message, container.getSuccesses(), replyTo);
        MessageRecipients failures = container.getFailures();
        try {
            if (sendFailures != null) {
                RecipientInternals.addRecipients(failures, sendFailures);
            }
        } catch (RecipientException e) {
            throw new MessageException(e);
        }
        return failures;
    }

    /**
     * Sends a <code>MultiChannelMessage</code> to a named recipient list.
     * <p>
     * @param message The <code>MultiChannelMessage</code> to send.
     * @param toListName The to recipient list previously added to the
     *                   <code>Session</code>.
     * @param ccListName The cc recipient list previously added to the
     *                   <code>Session</code>.
     * @param bccListName The bcc recipient list previously added to the
     *                    <code>Session</code>.
     * @param replyTo The <code>MessageRecipient</code> who will be the sender
     *                of the message.
     * @throws MessageException if an error occurs when sending the message.
     * @return A <code>MessageRecipients</code> list of failed
     *         <code>MessageRecipients</code>.
     * @deprecated use {@link #send(com.volantis.mps.message.MultiChannelMessage, com.volantis.mps.recipient.MessageRecipients, com.volantis.mps.recipient.MessageRecipients, com.volantis.mps.recipient.MessageRecipients, com.volantis.mps.recipient.MessageRecipient)}
     * instead, as this implementation is not guaranteed to be threadsafe
     */
    public MessageRecipients send(MultiChannelMessage message, String toListName, String ccListName, String bccListName, MessageRecipient replyTo) throws MessageException {
        MessageRecipients toList = (MessageRecipients) recipientsMap.get(toListName);
        MessageRecipients ccList = (MessageRecipients) recipientsMap.get(ccListName);
        MessageRecipients bccList = (MessageRecipients) recipientsMap.get(bccListName);
        return send(message, toList, ccList, bccList, replyTo);
    }

    /**
     * Sends a <code>MultiChannelMessage</code> to the specified recipients.
     * <p>
     * @param message   the <code>MultiChannelMessage</code> to send.
     * @param toList    list of recipients to which this message should be sent.
     * @param ccList    list of recipients to which this message should be cc'ed.
     * @param bccList   list of recipients to which this message should be bcc'ed.
     * @param replyTo   the <code>MessageRecipient</code> who will be the
     *                  sender of the message.
     * @throws MessageException if an error occurs when sending the message.
     * @return A <code>MessageRecipients</code> list of failed
     *         <code>MessageRecipients</code>.
     */
    public MessageRecipients send(MultiChannelMessage message, MessageRecipients toList, MessageRecipients ccList, MessageRecipients bccList, MessageRecipient replyTo) throws MessageException {
        if (toList == null && ccList == null && bccList == null) {
            throw new MessageException(localizer.format("recipient-lists-null-invalid"));
        }
        MessageRecipientsStatusContainer container;
        MessageRecipients failures = new MessageRecipients();
        MessageRecipients recipients = new MessageRecipients();
        try {
            if (toList != null) {
                container = validateRecipients(toList, RecipientInternals.TO_RECIPIENT);
                RecipientInternals.addRecipients(recipients, container.getSuccesses());
                RecipientInternals.addRecipients(failures, container.getFailures());
            }
            if (ccList != null) {
                container = validateRecipients(ccList, RecipientInternals.CC_RECIPIENT);
                RecipientInternals.addRecipients(recipients, container.getSuccesses());
                RecipientInternals.addRecipients(failures, container.getFailures());
            }
            if (bccList != null) {
                container = validateRecipients(bccList, RecipientInternals.BCC_RECIPIENT);
                RecipientInternals.addRecipients(recipients, container.getSuccesses());
                RecipientInternals.addRecipients(failures, container.getFailures());
            }
        } catch (RecipientException e) {
            throw new MessageException(e);
        }
        MessageRecipients sendFailures = sendToChannelAdapters(message, recipients, replyTo);
        try {
            if (sendFailures != null) {
                RecipientInternals.addRecipients(failures, sendFailures);
            }
        } catch (RecipientException e) {
            throw new MessageException(e);
        }
        return failures;
    }

    /**
     * Sends the <code>MultiChannelMessage</code> to each of the <code>ChannelAdapters</code>
     * in turn. Each <code>ChannelAdapter</code> will only consume recipients
     * that are destined for it.
     *
     * @param message The <code>MultiChannelMessage</code> to send.
     * @param recipients The <code>MessageRecipients</code> list to send to.
     * @param replyTo The <code>MessageRecipient</code> who will be the sender of the message.
     *
     * @throws MessageException Thrown if an error occurs in sending the message.
     *
     * @return A <code>MessageRecipients</code> list of failed <code>MessageRecipients</code>.
     *
     * @todo Check status from channel.send and combine to return to caller.
     */
    private MessageRecipients sendToChannelAdapters(MultiChannelMessage message, MessageRecipients recipients, MessageRecipient replyTo) throws MessageException {
        MessageRecipients failures = new MessageRecipients();
        Iterator recipientsIterator = recipients.getIterator();
        String currentChannel = null;
        while (recipientsIterator.hasNext()) {
            MessageRecipient recipient = (MessageRecipient) recipientsIterator.next();
            try {
                if (recipient.getChannelName() != null) {
                    if (!recipient.getChannelName().equals(currentChannel)) {
                        currentChannel = recipient.getChannelName();
                        MessageChannel channel = (MessageChannel) CHANNEL_MAP.get(currentChannel);
                        if (channel != null) {
                            try {
                                MessageRecipients sendFailures = channel.send(message, recipients, replyTo);
                                if (sendFailures != null) {
                                    RecipientInternals.addRecipients(failures, sendFailures);
                                }
                            } catch (RecipientException e) {
                                throw new MessageException(e);
                            }
                        } else {
                            logger.error("unconfigured-message-channel", currentChannel);
                            Iterator cfIterator = recipients.getIterator();
                            MessageRecipients channelFailures = new MessageRecipients();
                            String failMsg = messageLocalizer.format("unconfigured-message-channel", currentChannel);
                            while (cfIterator.hasNext()) {
                                MessageRecipient mr = (MessageRecipient) cfIterator.next();
                                if (mr.getChannelName().equals(currentChannel)) {
                                    mr.setFailureReason(failMsg);
                                    channelFailures.addRecipient(mr);
                                }
                            }
                            RecipientInternals.addRecipients(failures, channelFailures);
                        }
                    }
                }
            } catch (RecipientException rce) {
                throw new MessageException(rce);
            }
        }
        return failures;
    }

    /**
     * Invalidate the session, closing any channels that are being managed by
     * this session.
     */
    public void invalidate() {
        Iterator i = CHANNEL_MAP.values().iterator();
        while (i.hasNext()) {
            MessageChannel channel = (MessageChannel) i.next();
            channel.close();
        }
    }

    /**
      * Container for validation of MessageRecipients.
      */
    private class MessageRecipientsStatusContainer {

        /**
         * Recipients who have failed validation.
         */
        private MessageRecipients failures;

        /**
         * Recipients who have passed validation.
         */
        private MessageRecipients successes;

        public MessageRecipientsStatusContainer() {
        }

        /**
         * Set the <code>MessageRecipients</code> that have failed vallidation.
         * @param failures The <code>MessageRecipients</code> who have failed.
         */
        public void setFailures(MessageRecipients failures) {
            this.failures = failures;
        }

        /**
         * Set the <code>MessageRecipients</code> that have passed validation.
         * @param successes The <code>MessageRecipients</code> who have
         *                  succeeded.
         */
        public void setSuccesses(MessageRecipients successes) {
            this.successes = successes;
        }

        /**
         * Get the <code>MessageRecipients</code> that have failed validation.
         * @return The <code>MessageRecipients</code> who have failed.
         */
        public MessageRecipients getFailures() {
            return failures;
        }

        /**
         * Get the <code>MessageRecipients</code> that have passed validation.
         * @return The <code>MessageRecipients</code> who have passed.
         */
        public MessageRecipients getSuccesses() {
            return successes;
        }
    }
}
