package com.volantis.mps.bms.impl.local;

import com.volantis.mps.bms.Address;
import com.volantis.mps.bms.Failures;
import com.volantis.mps.bms.MSISDN;
import com.volantis.mps.bms.MalformedAddressException;
import com.volantis.mps.bms.Message;
import com.volantis.mps.bms.MessageFactory;
import com.volantis.mps.bms.MessageService;
import com.volantis.mps.bms.MessageServiceException;
import com.volantis.mps.bms.Recipient;
import com.volantis.mps.bms.RecipientType;
import com.volantis.mps.bms.SMTPAddress;
import com.volantis.mps.bms.SendRequest;
import com.volantis.mps.bms.Sender;
import com.volantis.mps.localization.LocalizationFactory;
import com.volantis.mps.message.MessageException;
import com.volantis.mps.message.MultiChannelMessage;
import com.volantis.mps.message.MultiChannelMessageImpl;
import com.volantis.mps.recipient.MessageRecipient;
import com.volantis.mps.recipient.MessageRecipients;
import com.volantis.mps.recipient.RecipientException;
import com.volantis.mps.session.Session;
import com.volantis.synergetics.localization.ExceptionLocalizer;
import com.volantis.synergetics.log.LogDispatcher;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Internal implementation which talks to core MPS API to send the message.
 */
public class InternalMessageService implements MessageService {

    private static final LogDispatcher LOGGER = LocalizationFactory.createLogger(InternalMessageService.class);

    private static final ExceptionLocalizer EXCEPTION_LOCALIZER = LocalizationFactory.createExceptionLocalizer(InternalMessageService.class);

    /**
     * Public constructor used by reflective creation process.
     *
     * @param endpoint
     * @see com.volantis.mps.bms.impl.DefaultMessageServiceFactory
     */
    public InternalMessageService(String endpoint) {
    }

    public Failures process(SendRequest sendRequest) throws MessageServiceException {
        return process(sendRequest, new Session());
    }

    /**
     * Returns a Failures object containing {@link Recipient} objects which
     * have details about any failures. An empty Failures object indicates that
     * there were no failures.
     *
     * @param sendRequest   containing the Recipients and Message details -
     *                      must be non-null
     * @param session       within which this message service is operating -
     *                      must be non-null
     * @return Failures - will never be null
     * @throws MessageServiceException if there was a problem processing the
     * request
     * @throws IllegalArgumentException if the supplied SendRequest was null
     */
    public Failures process(SendRequest sendRequest, Session session) throws MessageServiceException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Entered send");
        }
        if (sendRequest == null) {
            throw new IllegalArgumentException(EXCEPTION_LOCALIZER.format("argument-is-null", "SendRequest"));
        }
        if (session == null) {
            throw new IllegalArgumentException(EXCEPTION_LOCALIZER.format("argument-is-null", "Session"));
        }
        final Map mpsRecipients = getRecipients(sendRequest.getRecipients());
        final MultiChannelMessage mcMessage = createMultiChannelMessage(sendRequest.getMessage());
        addRecipients(session, mpsRecipients);
        final MessageRecipient from = getSender(sendRequest.getSender());
        final MessageRecipients failures = sendMessage(session, mcMessage, from);
        final Failures result = aggregateResults(failures);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Exiting send with " + result.getRecipients().length + " failures.");
        }
        return result;
    }

    /**
     * Return a Map containing {@link MessageRecipients} keyed by
     * {@link RecipientType}.
     *
     * @param recipients an array of {@link Recipient} objects.
     * @return a Map - not null.
     * @throws IllegalArgumentException if the recipients parameter is null or
     *                                  zero-length.
     */
    private Map getRecipients(Recipient[] recipients) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Entered getRecipients");
        }
        if (null == recipients) {
            throw new IllegalArgumentException("recipients cannot be null");
        }
        if (0 == recipients.length) {
            throw new IllegalArgumentException("no recipients specified");
        }
        Map mpsMessageRecipients = new HashMap();
        mpsMessageRecipients.put(RecipientType.TO, new MessageRecipients());
        mpsMessageRecipients.put(RecipientType.CC, new MessageRecipients());
        mpsMessageRecipients.put(RecipientType.BCC, new MessageRecipients());
        partitionRecipients(mpsMessageRecipients, recipients);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Exited getRecipients");
        }
        return mpsMessageRecipients;
    }

    /**
     * Iterates the Recipient array, converting each Recipient to a
     * MessageRecipient and adding it to the appropriate MessageRecipients
     * object.
     *
     * @param mapping    a Map of MessageRecipients keyed by RecipientType.
     * @param recipients an array of Recipient objects.
     */
    private void partitionRecipients(Map mapping, Recipient[] recipients) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Entered partitionRecipients with " + recipients.length + " recipients.");
        }
        for (int i = 0; i < recipients.length; i++) {
            MessageRecipients target = (MessageRecipients) mapping.get(recipients[i].getRecipientType());
            try {
                MessageRecipient mpsRecipient = createMessageRecipient(recipients[i]);
                target.addRecipient(mpsRecipient);
            } catch (RecipientException e) {
                LOGGER.warn("add-recipient-failure", recipients[i]);
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Exited partitionRecipients");
        }
    }

    /**
     * Return a new MultiChannelMessage.
     *
     * @param message
     * @return a MultiChannelMessage
     */
    private MultiChannelMessage createMultiChannelMessage(Message message) throws MessageServiceException {
        try {
            MultiChannelMessage toBeSent = null;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Entered createMultiChannelMessage");
            }
            if (message.getContent() != null) {
                toBeSent = new MultiChannelMessageImpl();
                try {
                    toBeSent.setMessage(message.getContent());
                    toBeSent.setCharacterEncoding(message.getCharacterEncoding());
                    toBeSent.setSubject(message.getSubject());
                } catch (MessageException e) {
                    throw new MessageServiceException(e);
                }
            } else {
                toBeSent = new MultiChannelMessageImpl(message.getURL(), message.getSubject(), message.getCharacterEncoding());
            }
            return toBeSent;
        } finally {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exited createMultiChannelMessage");
            }
        }
    }

    /**
     * Return a {@link MessageRecipients} object containing any failed
     * MessageRecipients from trying to send the message.
     *
     * @param session   a Session.
     * @param mcMessage a MultiChannelMessage
     * @param from      a MessageRecipient representing the sender of the
     *                  message - may be null.
     * @return a MessageRecipients object.
     * @throws MessageServiceException if there was a problem.
     */
    private MessageRecipients sendMessage(Session session, MultiChannelMessage mcMessage, MessageRecipient from) throws MessageServiceException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Entered sendMessage");
        }
        try {
            return session.send(mcMessage, RecipientType.TO.name(), RecipientType.CC.name(), RecipientType.BCC.name(), from);
        } catch (MessageException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Problem sending message");
            }
            LOGGER.error(e);
            throw new MessageServiceException(e);
        } finally {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exited sendMessage");
            }
        }
    }

    /**
     * Return a Collection of {@link Recipient} objects based on the contents
     * of the specified failed MessageRecipients.
     *
     * @param failures a MessageRecipients object containing the recipients
     *                 that the message could not be sent to.
     * @return a Collection - not null.
     */
    private Failures aggregateResults(MessageRecipients failures) {
        final Failures result = MessageFactory.getDefaultInstance().createFailures();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Entered aggregateResults");
        }
        if (failures != null) {
            for (Iterator iterator = failures.getIterator(); iterator.hasNext(); ) {
                MessageRecipient failure = (MessageRecipient) iterator.next();
                try {
                    Recipient recipient = getFailedRecipient(failure);
                    recipient.setFailureReason(failure.getFailureReason());
                    recipient.setChannel(failure.getChannelName());
                    result.add(recipient);
                } catch (RecipientException e) {
                    LOGGER.warn("recipient-conversion-failure", failure, e);
                }
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Exited aggregateResults");
        }
        return result;
    }

    /**
     * Return a Recipient based on the specified {@link MessageRecipient}.
     *
     * @param failure a MessageRecipient
     * @return a Recipient - not null.
     * @throws RecipientException if there was a problem converting from one
     *                            form to the other.
     */
    private Recipient getFailedRecipient(MessageRecipient failure) throws RecipientException {
        Address address = getAddress(failure);
        return MessageFactory.getDefaultInstance().createRecipient(address, failure.getDeviceName());
    }

    /**
     * Return the address based on the populated fields of the
     * MessageRecipient.
     *
     * @param failure
     * @return
     * @throws RecipientException if there was a problem.
     */
    private Address getAddress(MessageRecipient failure) throws RecipientException {
        Address result;
        final MessageFactory factory = MessageFactory.getDefaultInstance();
        if (failure.getAddress() != null) {
            try {
                result = factory.createSMTPAddress(failure.getAddress().getAddress());
            } catch (MalformedAddressException e) {
                LOGGER.warn("unable-to-convert-mps-address", failure.getAddress().getAddress());
                throw new RecipientException(EXCEPTION_LOCALIZER.format("unable-to-convert-mps-address", failure.getAddress().getAddress()));
            }
        } else if (failure.getMSISDN() != null) {
            try {
                result = factory.createMSISDN(failure.getMSISDN());
            } catch (MalformedAddressException e) {
                LOGGER.warn("unable-to-convert-mps-address", failure.getMSISDN());
                throw new RecipientException(EXCEPTION_LOCALIZER.format("unable-to-convert-mps-address", failure.getMSISDN()));
            }
        } else {
            throw new RecipientException(EXCEPTION_LOCALIZER.format("unable-to-convert-mps-address", failure));
        }
        return result;
    }

    /**
     * Add the MessageRecipients in the Map to the Session.
     *
     * @param session    a new Session.
     * @param recipients a Map of {@link MessageRecipients}, keyed by
     *                   {@link RecipientType}.
     * @throws MessageServiceException if there was a problem adding the
     *                                 MessageRecipients to the Session.
     */
    private void addRecipients(Session session, Map recipients) throws MessageServiceException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Entered addRecipients");
        }
        RecipientType type = RecipientType.TO;
        try {
            for (Iterator iterator = recipients.keySet().iterator(); iterator.hasNext(); ) {
                type = (RecipientType) iterator.next();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Adding recipients: " + type);
                }
                session.addRecipients(type.name(), (MessageRecipients) recipients.get(type));
            }
        } catch (RecipientException ignore) {
            LOGGER.warn("message-recipient-addition-failed-for", type, ignore);
            throw new MessageServiceException();
        } finally {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exited addRecipients");
            }
        }
    }

    /**
     * Return a MessageRecipient representing the sender of the message, or
     * null if there isn't one specified.
     *
     * @param sender a Recipient
     * @return a MessageRecipient - may be null.
     */
    private MessageRecipient getSender(Sender sender) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Entered getSender");
        }
        try {
            if (null == sender) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Sender is null");
                }
                return null;
            }
            try {
                MessageRecipient result = new MessageRecipient();
                if (null != sender.getSMTPAddress()) {
                    result.setAddress(getInternetAddress(sender.getSMTPAddress().getValue()));
                }
                if (null != sender.getMSISDN()) {
                    result.setMSISDN(sender.getMSISDN().getValue());
                }
                return result;
            } catch (RecipientException e) {
                LOGGER.warn("add-recipient-failure", sender);
            }
            return null;
        } finally {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exited getSender");
            }
        }
    }

    /**
     * Return a MessageRecipient based on the provied Recipient. Essentially,
     * this is a translation method.
     *
     * @param recipient
     * @return a MessageRecipient
     * @throws RecipientException if there was a problem.
     */
    private MessageRecipient createMessageRecipient(Recipient recipient) throws RecipientException {
        MessageRecipient result = new MessageRecipient();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Entered createMessageRecipient");
        }
        setAddress(result, recipient.getAddress());
        result.setDeviceName(recipient.getDeviceName());
        result.setChannelName(recipient.getChannel());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Exited createMessageRecipient");
        }
        return result;
    }

    private void setAddress(MessageRecipient result, final Address address) throws RecipientException {
        if (address instanceof SMTPAddress) {
            result.setAddress(getInternetAddress(address.getValue()));
        } else if (address instanceof MSISDN) {
            result.setMSISDN(address.getValue());
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unknown Address type " + "- throwing IllegalArgumentException");
            }
            throw new IllegalArgumentException("Unknown address type: " + address);
        }
    }

    private javax.mail.internet.InternetAddress getInternetAddress(String value) throws RecipientException {
        try {
            return new InternetAddress(value);
        } catch (AddressException e) {
            throw new RecipientException(EXCEPTION_LOCALIZER.format("mmsaddress-creation-failed", value), e);
        }
    }
}
