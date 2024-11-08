package net.jxta.endpoint;

import net.jxta.peergroup.PeerGroupID;

/**
 * A Channel Messenger provides an exclusive interface to the shared messenger.
 * <p/>
 * What is typically exclusive is the message queue, addressing parameters
 * that are not usefully shared (serviceName, serviceParam), and if needed
 * cross-group address rewriting parameters.
 * <p/>
 * This class is provided as a base for implementing such channel messengers,
 * which are typically what Messenger.getChannelMessenger() needs to return.
 *
 * @see net.jxta.endpoint.EndpointService
 * @see net.jxta.endpoint.EndpointAddress
 * @see net.jxta.endpoint.Message
 */
public abstract class ChannelMessenger extends AbstractMessenger implements Messenger {

    /**
     * insertedServicePrefix This is how all valid inserted services start. This
     * lets us recognize if a message already has an inserted service. Then we
     * must not add another one. Only the top-most one counts.  Since insertion
     * is only done here, the constant is defined here. Even if historically it
     * was done within the endpoint implementation, it has become a protocol now.
     */
    public static final String InsertedServicePrefix = "EndpointService:";

    private String insertedService;

    /**
     * The worker that implements sendMessage-with-listener for this channel. If
     * there's none, sendMessage-with-listener will throw an exception. Channels
     * returned by getMessenger methods all have one already. It is up to the
     * invoker of getChannelMessenger to supply one or not.
     */
    private ListenerAdaptor messageWatcher;

    protected String origService;

    protected String origServiceParam;

    /**
     * Figure out what the service string will be after mangling (if required)
     * and applying relevant defaults.
     *
     * @param service The service name in the unmangled address.
     * @return String The service name in the mangled address.
     */
    protected String effectiveService(String service) {
        if (insertedService == null) {
            return (service == null) ? origService : service;
        }
        return ((service != null) && service.startsWith(InsertedServicePrefix)) ? service : insertedService;
    }

    /**
     * Figure out what the param string will be after mangling (if required) and
     * applying relevant defaults.
     *
     * @param service      The service name in the unmangled address.
     * @param serviceParam The service parameter in the unmangled address.
     * @return String The service parameter in the mangled address.
     */
    protected String effectiveParam(String service, String serviceParam) {
        if ((insertedService == null) || ((service != null) && service.startsWith(InsertedServicePrefix))) {
            return (serviceParam == null) ? origServiceParam : serviceParam;
        }
        if (service == null) {
            service = origService;
        }
        if (serviceParam == null) {
            serviceParam = origServiceParam;
        }
        return ((null != service) && (null != serviceParam)) ? (service + "/" + serviceParam) : service;
    }

    /**
     * Give this channel the watcher that it must use whenever sendMessage(...,listener) is used. If not set,
     * sendMessage(..., listener) will throw.
     *
     * @param messageWatcher the listener
     */
    public void setMessageWatcher(ListenerAdaptor messageWatcher) {
        this.messageWatcher = messageWatcher;
    }

    /**
     * Create a new ChannelMessenger
     *
     * @param baseAddress      The network address messages go to; regardless of service, param, or group.
     * @param groupRedirection Group to which the messages must be redirected. This is used to implement the automatic group
     *                         segregation which has become a de-facto standard. If not null, the unique portion of the specified groupID is
     *                         prepended with {@link #InsertedServicePrefix} and inserted in every message's destination address in place of the
     *                         the original service name, which gets shifted into the beginning of the service parameter. The opposite is done
     *                         on arrival to restore the original destination address before the message is delivered to the listener in the
     *                         the specified group. Messages that already bear a group redirection are not affected.
     * @param origService      The default destination service for messages sent without specifying a different service.
     * @param origServiceParam The default destination service parameter for messages sent without specifying a different service
     *                         parameter.
     */
    public ChannelMessenger(EndpointAddress baseAddress, PeerGroupID groupRedirection, String origService, String origServiceParam) {
        super(baseAddress);
        if (groupRedirection == null) {
            insertedService = null;
        } else {
            insertedService = InsertedServicePrefix + groupRedirection.getUniqueValue().toString();
        }
        this.origService = origService;
        this.origServiceParam = origServiceParam;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * By default a channel refuses to make a channel.
     */
    public Messenger getChannelMessenger(PeerGroupID redirection, String service, String serviceParam) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void sendMessage(Message msg, String service, String serviceParam, OutgoingMessageEventListener listener) {
        if (messageWatcher == null) {
            throw new UnsupportedOperationException("This channel was not configured to emulate this legacy method.");
        }
        msg.setMessageProperty(Messenger.class, null);
        messageWatcher.watchMessage(listener, msg);
        sendMessageN(msg, service, serviceParam);
    }
}
