package net.jxta.impl.endpoint;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.XMLDocument;
import net.jxta.document.XMLElement;
import net.jxta.endpoint.ChannelMessenger;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.MessageFilterListener;
import net.jxta.endpoint.MessagePropagater;
import net.jxta.endpoint.MessageReceiver;
import net.jxta.endpoint.MessageSender;
import net.jxta.endpoint.MessageTransport;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.MessengerEvent;
import net.jxta.endpoint.MessengerEventListener;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.ThreadedMessenger;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.impl.endpoint.endpointMeter.EndpointMeter;
import net.jxta.impl.endpoint.endpointMeter.EndpointMeterBuildSettings;
import net.jxta.impl.endpoint.endpointMeter.EndpointServiceMonitor;
import net.jxta.impl.endpoint.endpointMeter.InboundMeter;
import net.jxta.impl.endpoint.endpointMeter.OutboundMeter;
import net.jxta.impl.endpoint.endpointMeter.PropagationMeter;
import net.jxta.impl.endpoint.relay.RelayClient;
import net.jxta.impl.endpoint.router.EndpointRouter;
import net.jxta.impl.endpoint.tcp.TcpTransport;
import net.jxta.impl.meter.MonitorManager;
import net.jxta.impl.util.SequenceIterator;
import net.jxta.logging.Logging;
import net.jxta.meter.MonitorResources;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.Module;
import net.jxta.protocol.AccessPointAdvertisement;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.RouteAdvertisement;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the frontend for all the JXTA  endpoint protocols, as
 * well as the API for the implementation of the core protocols that use
 * directly the EndpointService. It theory it only needs to implement core methods.
 * legacy or convenience methods should stay out. However, that would require
 * a two-level interface for the service (internal and public). May be later.
 */
public class EndpointServiceImpl implements EndpointService, MessengerEventListener {

    /**
     * Logger
     */
    private static final Logger LOG = Logger.getLogger(EndpointServiceImpl.class.getName());

    /**
     * The Wire Message Format we will use by default.
     */
    public static final MimeMediaType DEFAULT_MESSAGE_TYPE = new MimeMediaType("application/x-jxta-msg").intern();

    /**
     * The name of this service.
     */
    public static final String ENDPOINTSERVICE_NAME = "EndpointService";

    /**
     * The Message empty namespace. This namespace is reserved for use by
     * applications. It will not be used by core protocols.
     */
    public static final String MESSAGE_EMPTY_NS = "";

    /**
     * The Message "jxta" namespace. This namespace is reserved for use by
     * core protocols. It will not be used by applications.
     */
    public static final String MESSAGE_JXTA_NS = "jxta";

    /**
     * Namespace in which the message source address will be placed.
     */
    public static final String MESSAGE_SOURCE_NS = MESSAGE_JXTA_NS;

    /**
     * Element name in which the message source address will be placed.
     */
    public static final String MESSAGE_SOURCE_NAME = "EndpointSourceAddress";

    /**
     * Namespace in which the message destination address will be placed.
     */
    public static final String MESSAGE_DESTINATION_NS = MESSAGE_JXTA_NS;

    /**
     * Element name in which the message destination address will be placed.
     * This element is used for loopback detection during propagate. Only
     * propagate messages currently contain this element.
     */
    public static final String MESSAGE_DESTINATION_NAME = "EndpointDestinationAddress";

    /**
     * Namespace in which the message source peer address will be placed.
     */
    public static final String MESSAGE_SRCPEERHDR_NS = MESSAGE_JXTA_NS;

    /**
     * Element name in which the message source peer address will be placed.
     * This element is used for loopback detection during propagate. Only
     * propagated messages currently contain this element.
     */
    public static final String MESSAGE_SRCPEERHDR_NAME = "EndpointHeaderSrcPeer";

    /**
     * Size of the message queue provided by virtual messengers.
     */
    private static final int DEFAULT_MESSAGE_QUEUE_SIZE = 20;

    /**
     * If {@code true} then the parent endpoint may be used for acquiring
     * messengers and for registering listeners.
     */
    private static final boolean DEFAULT_USE_PARENT_ENDPOINT = true;

    EndpointServiceMonitor endpointServiceMonitor;

    /**
     * the EndpointMeter
     */
    private EndpointMeter endpointMeter;

    private PropagationMeter propagationMeter;

    /**
     * If {@code true} then this service has been initialized.
     */
    private boolean initialized = false;

    /**
     * tunable: the virtual messenger queue size
     */
    private int vmQueueSize = DEFAULT_MESSAGE_QUEUE_SIZE;

    private PeerGroup group = null;

    private ID assignedID = null;

    private ModuleImplAdvertisement implAdvertisement = null;

    private String localPeerId = null;

    private boolean useParentEndpoint = DEFAULT_USE_PARENT_ENDPOINT;

    private EndpointService parentEndpoint = null;

    private String myServiceName = null;

    /**
     * The Message Transports which are registered for this endpoint. This is
     * only the message transport registered locally, it does not include
     * transports which are used from other groups.
     */
    private final Collection<MessageTransport> messageTransports = new HashSet<MessageTransport>();

    /**
     * Passive listeners for messengers. Three priorities, so far.
     */
    private final Collection[] passiveMessengerListeners = { Collections.synchronizedList(new ArrayList<MessengerEventListener>()), Collections.synchronizedList(new ArrayList<MessengerEventListener>()), Collections.synchronizedList(new ArrayList<MessengerEventListener>()) };

    /**
     * The set of listener managed by this instance of the endpoint svc.
     */
    private final Map<String, EndpointListener> incomingMessageListeners = new HashMap<String, EndpointListener>(16);

    /**
     * The set of shared transport messengers currently ready for use.
     */
    private final Map<EndpointAddress, Reference<Messenger>> messengerMap = new WeakHashMap<EndpointAddress, Reference<Messenger>>(32);

    /**
     * The set of shared transport messengers currently ready for use.
     */
    private final Map<EndpointAddress, Reference<Messenger>> directMessengerMap = new WeakHashMap<EndpointAddress, Reference<Messenger>>(32);

    /**
     * The filter listeners.
     * <p/>
     * We rarely add/remove, never remove without iterating
     * and insert objects that are always unique. So using a set
     * does not make sense. An array list is the best.
     */
    private final Collection<FilterListenerAndMask> incomingFilterListeners = new ArrayList<FilterListenerAndMask>();

    private final Collection<FilterListenerAndMask> outgoingFilterListeners = new ArrayList<FilterListenerAndMask>();

    /**
     * Holder for a filter listener and its conditions
     */
    private static class FilterListenerAndMask {

        final String namespace;

        final String name;

        final MessageFilterListener listener;

        public FilterListenerAndMask(MessageFilterListener listener, String namespace, String name) {
            this.namespace = namespace;
            this.name = name;
            this.listener = listener;
        }

        @Override
        public boolean equals(Object target) {
            if (this == target) {
                return true;
            }
            if (target instanceof FilterListenerAndMask) {
                FilterListenerAndMask likeMe = (FilterListenerAndMask) target;
                boolean result = (null != namespace) ? (namespace.equals(likeMe.namespace)) : (null == likeMe.namespace);
                result &= (null != name) ? (name.equals(likeMe.name)) : (null == likeMe.name);
                result &= (listener == likeMe.listener);
                return result;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * Added to make PMD shut up....
         */
        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }

    /**
     * A non blocking messenger that obtains a backing (possibly blocking)
     * messenger on-demand.
     */
    private class CanonicalMessenger extends ThreadedMessenger {

        /**
         * If the hint was not used because there already was a transport
         * messenger available, then it is saved here for the next time we are
         * forced to create a new transport messenger by the breakage of the one
         * that's here.
         * <p/>
         * The management of hints is a bit inconsistent for now: the hint
         * used may be different dependent upon which invocation created the
         * current canonical messenger and, although we try to use the hint only
         * once (to avoid carrying an invalid hint forever) it may happen that a
         * hint is used long after it was suggested.
         */
        Object hint;

        /**
         * The transport messenger that this canonical messenger currently uses.
         */
        Messenger cachedMessenger = null;

        /**
         * Create a new CanonicalMessenger.
         *
         * @param vmQueueSize        queue size
         * @param destination        destination who messages should be addressed to
         * @param logicalDestination logical destination
         * @param hint               route hint
         * @param messengerMeter     the metering object if any
         */
        public CanonicalMessenger(int vmQueueSize, EndpointAddress destination, EndpointAddress logicalDestination, Object hint, OutboundMeter messengerMeter) {
            super(group.getPeerGroupID(), destination, logicalDestination, vmQueueSize);
            this.hint = hint;
        }

        /**
         * close this canonical messenger.
         */
        @Override
        public void close() {
        }

        /**
         * Drop the current messenger.
         */
        @Override
        protected void closeImpl() {
            if (cachedMessenger != null) {
                cachedMessenger.close();
                cachedMessenger = null;
            } else {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.severe("Internal messenger error: close requested while not connected.");
                }
            }
        }

        /**
         * Get a transport messenger to the destination.
         * <p/>
         * FIXME 20040413 jice : Do better hint management.
         */
        @Override
        protected boolean connectImpl() {
            if (cachedMessenger != null) {
                if ((cachedMessenger.getState() & Messenger.TERMINAL) != 0) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.SEVERE)) {
                        LOG.fine("Closing TERMINAL internal messenger : attempting requested connect.");
                    }
                    cachedMessenger.close();
                    cachedMessenger = null;
                } else {
                    return true;
                }
            }
            Object theHint = hint;
            hint = null;
            cachedMessenger = getLocalTransportMessenger(getDestinationAddress(), theHint);
            if (cachedMessenger == null) {
                return false;
            }
            try {
                ((BlockingMessenger) cachedMessenger).setOwner(this);
            } catch (ClassCastException cce) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.severe("Transport messengers must all extend BlockingMessenger for now. " + cachedMessenger + " may remain open beyond its use.");
                }
            }
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected EndpointAddress getLogicalDestinationImpl() {
            if (cachedMessenger == null) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.severe("Internal messenger error: logical destination requested while not connected.");
                }
                return null;
            }
            return cachedMessenger.getLogicalDestinationAddress();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void sendMessageBImpl(Message msg, String service, String param) throws IOException {
            if (cachedMessenger == null) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.severe("Internal messenger error: send requested while not connected.");
                }
                throw new IOException("Internal messenger error.");
            }
            try {
                cachedMessenger.sendMessageB(msg, service, param);
            } catch (IOException any) {
                cachedMessenger = null;
                throw any;
            } catch (RuntimeException any) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, "Failure sending " + msg, any);
                }
                throw any;
            }
        }
    }

    /**
     * Create a new EndpointService.
     */
    public EndpointServiceImpl() {
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void init(PeerGroup group, ID assignedID, Advertisement impl) throws PeerGroupException {
        if (initialized) {
            throw new PeerGroupException("Cannot initialize service more than once");
        }
        this.group = group;
        this.assignedID = assignedID;
        this.implAdvertisement = (ModuleImplAdvertisement) impl;
        this.localPeerId = group.getPeerID().toString();
        this.myServiceName = ChannelMessenger.InsertedServicePrefix + group.getPeerGroupID().getUniqueValue().toString();
        ConfigParams confAdv = group.getConfigAdvertisement();
        XMLElement paramBlock = null;
        if (confAdv != null) {
            paramBlock = (XMLElement) confAdv.getServiceParam(assignedID);
        }
        if (paramBlock != null) {
            Enumeration param;
            param = paramBlock.getChildren("MessengerQueueSize");
            if (param.hasMoreElements()) {
                String textQSz = ((XMLElement) param.nextElement()).getTextValue();
                try {
                    Integer requestedSize = Integer.parseInt(textQSz.trim());
                    if (requestedSize > 0) {
                        vmQueueSize = requestedSize;
                    } else {
                        LOG.warning("Illegal MessengerQueueSize : " + textQSz);
                    }
                } catch (NumberFormatException e) {
                    if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING, "could not parse MessengerQueueSize string", e);
                    }
                }
            }
            param = paramBlock.getChildren("UseParentEndpoint");
            if (param.hasMoreElements()) {
                String textUPE = ((XMLElement) param.nextElement()).getTextValue();
                useParentEndpoint = textUPE.trim().equalsIgnoreCase("true");
            }
        }
        PeerGroup parentGroup = group.getParentGroup();
        if (useParentEndpoint && parentGroup != null) {
            parentEndpoint = parentGroup.getEndpointService();
            parentEndpoint.addMessengerEventListener(this, EndpointService.LowPrecedence);
        }
        initialized = true;
        if (Logging.SHOW_CONFIG && LOG.isLoggable(Level.CONFIG)) {
            StringBuilder configInfo = new StringBuilder("Configuring Endpoint Service : " + assignedID);
            if (implAdvertisement != null) {
                configInfo.append("\n\tImplementation :");
                configInfo.append("\n\t\tModule Spec ID: ");
                configInfo.append(implAdvertisement.getModuleSpecID());
                configInfo.append("\n\t\tImpl Description : ").append(implAdvertisement.getDescription());
                configInfo.append("\n\t\tImpl URI : ").append(implAdvertisement.getUri());
                configInfo.append("\n\t\tImpl Code : ").append(implAdvertisement.getCode());
            }
            configInfo.append("\n\tGroup Params :");
            configInfo.append("\n\t\tGroup : ").append(group);
            configInfo.append("\n\t\tPeer ID : ").append(group.getPeerID());
            configInfo.append("\n\tConfiguration :");
            if (null == parentGroup) {
                configInfo.append("\n\t\tHome Group : (none)");
            } else {
                configInfo.append("\n\t\tHome Group : ").append(parentGroup.getPeerGroupName()).append(" / ").append(parentGroup.getPeerGroupID());
            }
            configInfo.append("\n\t\tUsing home group endpoint : ").append(parentEndpoint);
            configInfo.append("\n\t\tVirtual Messenger Queue Size : ").append(vmQueueSize);
            LOG.config(configInfo.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {
        if (!initialized) {
            return -1;
        }
        if (EndpointMeterBuildSettings.ENDPOINT_METERING) {
            endpointServiceMonitor = (EndpointServiceMonitor) MonitorManager.getServiceMonitor(group, MonitorResources.endpointServiceMonitorClassID);
            if (endpointServiceMonitor != null) {
                endpointMeter = endpointServiceMonitor.getEndpointMeter();
            }
        }
        if (parentEndpoint != null) {
            Iterator<MessageTransport> parentMTs = parentEndpoint.getAllMessageTransports();
            synchronized (this) {
                while (parentMTs.hasNext()) {
                    addProtoToAdv(parentMTs.next());
                }
            }
        }
        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info("Endpoint Service started.");
        }
        return Module.START_OK;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * The transports and services are going to be stopped as well. When
     * they are, they will dereference us and we'll go into oblivion.
     */
    public void stopApp() {
        if (parentEndpoint != null) {
            parentEndpoint.removeMessengerEventListener(this, EndpointService.LowPrecedence);
        }
        int prec = EndpointService.HighPrecedence;
        while (prec >= EndpointService.LowPrecedence) {
            passiveMessengerListeners[prec--].clear();
        }
        messengerMap.clear();
        directMessengerMap.clear();
        incomingMessageListeners.clear();
        incomingFilterListeners.clear();
        outgoingFilterListeners.clear();
        messageTransports.clear();
        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info("Endpoint Service stopped.");
        }
    }

    /**
     * {@inheritDoc}
     */
    public PeerGroup getGroup() {
        return group;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * We create a new instance each time because our interface actually
     * has state (channel messengers and listener callback adaptor).
     */
    public EndpointService getInterface() {
        return new EndpointServiceInterface(this);
    }

    /**
     * {@inheritDoc}
     */
    public ModuleImplAdvertisement getImplAdvertisement() {
        return implAdvertisement;
    }

    private static class Metrics {

        int numFilteredOut = 0;

        int numPropagatedTo = 0;

        int numErrorsPropagated = 0;
    }

    private void propagateThroughAll(Iterator<MessageTransport> eachProto, Message myMsg, String serviceName, String serviceParam, int initialTTL, Metrics metrics) {
        Message filtered = null;
        while (eachProto.hasNext()) {
            MessageTransport aTransport = eachProto.next();
            try {
                if (!(aTransport instanceof MessagePropagater)) {
                    continue;
                }
                MessagePropagater propagater = (MessagePropagater) aTransport;
                if (null == filtered) {
                    filtered = processFilters(myMsg, propagater.getPublicAddress(), new EndpointAddress(group.getPeerGroupID(), serviceName, serviceParam), false);
                }
                if (null == filtered) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("   message " + myMsg + " discarded upon filter decision");
                    }
                    if (EndpointMeterBuildSettings.ENDPOINT_METERING) {
                        metrics.numFilteredOut++;
                    }
                    break;
                }
                propagater.propagate(filtered.clone(), serviceName, serviceParam, initialTTL);
                if (EndpointMeterBuildSettings.ENDPOINT_METERING) {
                    metrics.numPropagatedTo++;
                }
            } catch (Exception e) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, "Failed propagating message " + filtered + " on message transport " + aTransport, e);
                }
                if (EndpointMeterBuildSettings.ENDPOINT_METERING) {
                    metrics.numErrorsPropagated++;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void propagate(Message msg, String serviceName, String serviceParam) {
        propagate(msg, serviceName, serviceParam, Integer.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    public void propagate(Message msg, String serviceName, String serviceParam, int initialTTL) {
        long startPropagationTime = 0;
        if (null == serviceName) {
            throw new IllegalArgumentException("serviceName may not be null");
        }
        Metrics metrics = null;
        if (EndpointMeterBuildSettings.ENDPOINT_METERING) {
            metrics = new Metrics();
        }
        msg = msg.clone();
        if (EndpointMeterBuildSettings.ENDPOINT_METERING) {
            startPropagationTime = System.currentTimeMillis();
        }
        MessageElement srcHdrElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_SRCPEERHDR_NAME, localPeerId, null);
        msg.replaceMessageElement(EndpointServiceImpl.MESSAGE_SRCPEERHDR_NS, srcHdrElement);
        Iterator<MessageTransport> eachProto = getAllLocalTransports();
        propagateThroughAll(eachProto, msg.clone(), serviceName, serviceParam, initialTTL, metrics);
        if (parentEndpoint != null) {
            eachProto = parentEndpoint.getAllMessageTransports();
            StringBuilder mangled = new StringBuilder(serviceName);
            if (null != serviceParam) {
                mangled.append('/');
                mangled.append(serviceParam);
            }
            propagateThroughAll(eachProto, msg.clone(), myServiceName, mangled.toString(), initialTTL, metrics);
        }
        if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointServiceMonitor != null)) {
            PropagationMeter propagationMeter = endpointServiceMonitor.getPropagationMeter(serviceName, serviceParam);
            propagationMeter.registerPropagateMessageStats(metrics.numPropagatedTo, metrics.numFilteredOut, metrics.numErrorsPropagated, System.currentTimeMillis() - startPropagationTime);
        }
    }

    /**
     * Process the filters for this message.
     */
    private Message processFilters(Message message, EndpointAddress srcAddress, EndpointAddress dstAddress, boolean incoming) {
        Iterator<FilterListenerAndMask> eachFilter = incoming ? incomingFilterListeners.iterator() : outgoingFilterListeners.iterator();
        while (eachFilter.hasNext()) {
            FilterListenerAndMask aFilter = eachFilter.next();
            Message.ElementIterator eachElement = message.getMessageElements();
            while (eachElement.hasNext()) {
                MessageElement anElement = eachElement.next();
                if ((null != aFilter.namespace) && (!aFilter.namespace.equals(eachElement.getNamespace()))) {
                    continue;
                }
                if ((null != aFilter.name) && (!aFilter.name.equals(anElement.getElementName()))) {
                    continue;
                }
                message = aFilter.listener.filterMessage(message, srcAddress, dstAddress);
                if (null == message) {
                    return null;
                }
            }
        }
        return message;
    }

    private static EndpointAddress demangleAddress(EndpointAddress mangled) {
        String serviceName = mangled.getServiceName();
        if (null == serviceName) {
            return mangled;
        }
        if (!serviceName.startsWith(ChannelMessenger.InsertedServicePrefix)) {
            return mangled;
        }
        String serviceParam = mangled.getServiceParameter();
        if (null == serviceParam) {
            return new EndpointAddress(mangled, null, null);
        }
        int slashAt = serviceParam.indexOf('/');
        if (-1 == slashAt) {
            return new EndpointAddress(mangled, serviceParam, null);
        }
        return new EndpointAddress(mangled, serviceParam.substring(0, slashAt), serviceParam.substring(slashAt + 1));
    }

    /**
     * {@inheritDoc}
     */
    public void processIncomingMessage(Message msg, EndpointAddress srcAddress, EndpointAddress dstAddress) {
        MessageElement srcPeerElement = msg.getMessageElement(EndpointServiceImpl.MESSAGE_SRCPEERHDR_NS, EndpointServiceImpl.MESSAGE_SRCPEERHDR_NAME);
        if (null != srcPeerElement) {
            msg.removeMessageElement(srcPeerElement);
            String srcPeer = srcPeerElement.toString();
            if (localPeerId.equals(srcPeer)) {
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine(msg + " is a propagate loopback. Discarded");
                }
                if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                    endpointMeter.discardedLoopbackDemuxMessage();
                }
                return;
            }
        }
        if (null == srcAddress) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning("null src address, discarding message " + msg);
            }
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.invalidIncomingMessage();
            }
            return;
        }
        if (null == dstAddress) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning("null destination address, discarding message " + msg);
            }
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.invalidIncomingMessage();
            }
            return;
        }
        EndpointAddress demangledAddress = demangleAddress(dstAddress);
        String decodedServiceName = demangledAddress.getServiceName();
        String decodedServiceParam = demangledAddress.getServiceParameter();
        if ((null == decodedServiceName) || (0 == decodedServiceName.length())) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning("dest serviceName must not be null, discarding message " + msg);
            }
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.invalidIncomingMessage();
            }
            return;
        }
        msg = processFilters(msg, srcAddress, demangledAddress, true);
        if (msg == null) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Message discarded during filter processing");
            }
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.incomingMessageFilteredOut();
            }
            return;
        }
        if (demangledAddress != dstAddress) {
            decodedServiceName = dstAddress.getServiceName() + "/" + decodedServiceName;
        }
        EndpointListener listener = getIncomingMessageListener(decodedServiceName, decodedServiceParam);
        if (listener == null) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning("No listener for \'" + dstAddress + "\' in group " + group + "\n\tdecodedServiceName :" + decodedServiceName + "\tdecodedServiceParam :" + decodedServiceParam);
            }
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.noListenerForIncomingMessage();
            }
            return;
        }
        try {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                if (null != decodedServiceParam) {
                    LOG.fine("Calling listener for \'" + decodedServiceName + "/" + decodedServiceParam + "\' with " + msg);
                } else {
                    LOG.fine("Calling listener for \'" + decodedServiceName + "\' with " + msg);
                }
            }
            listener.processIncomingMessage(msg, srcAddress, demangledAddress);
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.incomingMessageSentToEndpointListener();
            }
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.demuxMessageProcessed();
            }
        } catch (Throwable all) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Uncaught throwable from listener for " + dstAddress, all);
            }
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.errorProcessingIncomingMessage();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void demux(Message msg) {
        MessageElement dstAddressElement = msg.getMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NS, EndpointServiceImpl.MESSAGE_DESTINATION_NAME);
        if (null == dstAddressElement) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning(msg + " has no destination address. Discarded");
            }
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.noDestinationAddressForDemuxMessage();
            }
            return;
        }
        msg.removeMessageElement(dstAddressElement);
        EndpointAddress dstAddress = new EndpointAddress(dstAddressElement.toString());
        MessageElement srcAddressElement = msg.getMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NS, EndpointServiceImpl.MESSAGE_SOURCE_NAME);
        if (null == srcAddressElement) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning(msg + " has no source address. Discarded");
            }
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointMeter != null)) {
                endpointMeter.noSourceAddressForDemuxMessage();
            }
            return;
        }
        msg.removeMessageElement(srcAddressElement);
        EndpointAddress msgScrAddress = new EndpointAddress(srcAddressElement.toString());
        processIncomingMessage(msg, msgScrAddress, dstAddress);
    }

    /**
     * {@inheritDoc}
     */
    public MessengerEventListener addMessageTransport(MessageTransport transpt) {
        synchronized (messageTransports) {
            if (!messageTransports.contains(transpt)) {
                clearProtoFromAdv(transpt);
                messageTransports.add(transpt);
                addProtoToAdv(transpt);
                return this;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeMessageTransport(MessageTransport transpt) {
        boolean removed;
        synchronized (messageTransports) {
            removed = messageTransports.remove(transpt);
        }
        if (removed) {
            clearProtoFromAdv(transpt);
        }
        return removed;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<MessageTransport> getAllMessageTransports() {
        if (null != parentEndpoint) {
            return new SequenceIterator(getAllLocalTransports(), parentEndpoint.getAllMessageTransports());
        } else {
            return getAllLocalTransports();
        }
    }

    /**
     * {@inheritDoc}
     */
    public MessageTransport getMessageTransport(String name) {
        Iterator<MessageTransport> allTransports = getAllMessageTransports();
        while (allTransports.hasNext()) {
            MessageTransport transpt = allTransports.next();
            if (transpt.getProtocolName().equals(name)) {
                return transpt;
            }
        }
        return null;
    }

    private void addProtoToAdv(MessageTransport proto) {
        LOG.info("message transport proto:" + proto + "  " + proto.getProtocolName());
        boolean relay = false;
        try {
            if (!(proto instanceof MessageReceiver)) {
                return;
            }
            if (proto instanceof EndpointRouter) {
                addActiveRelayListener(group);
                return;
            }
            if (proto instanceof RelayClient) {
                relay = true;
                ((RelayClient) proto).addActiveRelayListener(group);
            }
            Iterator<EndpointAddress> allAddresses = ((MessageReceiver) proto).getPublicAddresses();
            Vector<String> ea = new Vector<String>();
            while (allAddresses.hasNext()) {
                EndpointAddress anEndpointAddress = allAddresses.next();
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Adding endpoint address to route advertisement : " + anEndpointAddress);
                }
                ea.add(anEndpointAddress.toString());
            }
            PeerAdvertisement padv = group.getPeerAdvertisement();
            StructuredDocument myParam = padv.getServiceParam(assignedID);
            RouteAdvertisement route = null;
            if (myParam != null) {
                Enumeration paramChilds = myParam.getChildren(RouteAdvertisement.getAdvertisementType());
                if (paramChilds.hasMoreElements()) {
                    XMLElement param = (XMLElement) paramChilds.nextElement();
                    route = (RouteAdvertisement) AdvertisementFactory.newAdvertisement(param);
                    route.addDestEndpointAddresses(ea);
                    if (relay) {
                        Vector<AccessPointAdvertisement> hops = ((RelayClient) proto).getActiveRelays(group);
                        if (!hops.isEmpty()) {
                            route.setHops(hops);
                        }
                    }
                }
            }
            if (null == route) {
                AccessPointAdvertisement destAP = (AccessPointAdvertisement) AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());
                destAP.setPeerID(group.getPeerID());
                destAP.setEndpointAddresses(ea);
                route = (RouteAdvertisement) AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());
                route.setDest(destAP);
                if (relay) {
                    Vector<AccessPointAdvertisement> hops = ((RelayClient) proto).getActiveRelays(group);
                    if (!hops.isEmpty()) {
                        route.setHops(hops);
                    }
                }
            }
            XMLDocument newParam = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");
            XMLDocument xptDoc = (XMLDocument) route.getDocument(MimeMediaType.XMLUTF8);
            StructuredDocumentUtils.copyElements(newParam, newParam, xptDoc);
            padv.putServiceParam(assignedID, newParam);
            DiscoveryService discovery = group.getDiscoveryService();
            LOG.info("publish peer advertisement!");
            if (discovery != null) {
                discovery.publish(padv, DiscoveryService.INFINITE_LIFETIME, DiscoveryService.DEFAULT_EXPIRATION);
            }
        } catch (Exception ex) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Exception adding message transport ", ex);
            }
        }
    }

    private void clearProtoFromAdv(MessageTransport transpt) {
        try {
            if (!(transpt instanceof MessageReceiver)) {
                return;
            }
            if (transpt instanceof EndpointRouter) {
                removeActiveRelayListener(group);
                return;
            }
            if (transpt instanceof RelayClient) {
                ((RelayClient) transpt).removeActiveRelayListener(group);
            }
            Iterator<EndpointAddress> allAddresses = ((MessageReceiver) transpt).getPublicAddresses();
            Vector<String> ea = new Vector<String>();
            while (allAddresses.hasNext()) {
                EndpointAddress anEndpointAddress = allAddresses.next();
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Removing endpoint address from route advertisement : " + anEndpointAddress);
                }
                ea.add(anEndpointAddress.toString());
            }
            PeerAdvertisement padv = group.getPeerAdvertisement();
            XMLDocument myParam = (XMLDocument) padv.getServiceParam(assignedID);
            if (myParam == null) {
                return;
            }
            Enumeration paramChilds = myParam.getChildren(RouteAdvertisement.getAdvertisementType());
            if (!paramChilds.hasMoreElements()) {
                return;
            }
            XMLElement param = (XMLElement) paramChilds.nextElement();
            RouteAdvertisement route = (RouteAdvertisement) AdvertisementFactory.newAdvertisement(param);
            route.removeDestEndpointAddresses(ea);
            XMLDocument newParam = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, "Parm");
            XMLDocument xptDoc = (XMLDocument) route.getDocument(MimeMediaType.XMLUTF8);
            StructuredDocumentUtils.copyElements(newParam, newParam, xptDoc);
            padv.putServiceParam(assignedID, newParam);
            DiscoveryService discovery = group.getDiscoveryService();
            if (discovery != null) {
                discovery.publish(padv, DiscoveryService.INFINITE_LIFETIME, DiscoveryService.DEFAULT_EXPIRATION);
            }
        } catch (Exception ex) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Exception removing messsage transport ", ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean addMessengerEventListener(MessengerEventListener listener, int prio) {
        int priority = prio;
        if (priority > EndpointService.HighPrecedence) {
            priority = EndpointService.HighPrecedence;
        }
        if (priority < EndpointService.LowPrecedence) {
            priority = EndpointService.LowPrecedence;
        }
        return passiveMessengerListeners[priority].add(listener);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeMessengerEventListener(MessengerEventListener listener, int prio) {
        int priority = prio;
        if (priority > EndpointService.HighPrecedence) {
            priority = EndpointService.HighPrecedence;
        }
        if (priority < EndpointService.LowPrecedence) {
            priority = EndpointService.LowPrecedence;
        }
        return passiveMessengerListeners[priority].remove(listener);
    }

    /**
     * {@inheritDoc}
     */
    public boolean addIncomingMessageListener(EndpointListener listener, String serviceName, String serviceParam) {
        if (null == listener) {
            throw new IllegalArgumentException("EndpointListener must be non-null");
        }
        if (null == serviceName) {
            throw new IllegalArgumentException("serviceName must not be null");
        }
        if (-1 != serviceName.indexOf('/')) {
            throw new IllegalArgumentException("serviceName may not contain '/' characters");
        }
        String address = serviceName;
        if (null != serviceParam) {
            address += "/" + serviceParam;
        }
        synchronized (incomingMessageListeners) {
            if (incomingMessageListeners.containsKey(address)) {
                return false;
            }
            InboundMeter incomingMessageListenerMeter = null;
            if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointServiceMonitor != null)) {
                incomingMessageListenerMeter = endpointServiceMonitor.getInboundMeter(serviceName, serviceParam);
            }
            incomingMessageListeners.put(address, listener);
        }
        if (parentEndpoint != null) {
            if (serviceName.startsWith(ChannelMessenger.InsertedServicePrefix)) {
                parentEndpoint.addIncomingMessageListener(listener, serviceName, serviceParam);
            } else {
                parentEndpoint.addIncomingMessageListener(listener, myServiceName, address);
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public EndpointListener getIncomingMessageListener(String serviceName, String serviceParam) {
        if (null == serviceName) {
            throw new IllegalArgumentException("serviceName must not be null");
        }
        EndpointListener listener = null;
        if (null != serviceParam) {
            listener = incomingMessageListeners.get(serviceName + "/" + serviceParam);
        }
        if (listener == null) {
            listener = incomingMessageListeners.get(serviceName);
        }
        if (listener == null) {
            listener = incomingMessageListeners.get(serviceName + serviceParam);
            if ((null != listener) && Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.warning("Found handler only via compatibility listener : " + serviceName + serviceParam);
            }
        }
        return listener;
    }

    /**
     * {@inheritDoc}
     */
    public EndpointListener removeIncomingMessageListener(String serviceName, String serviceParam) {
        if (null == serviceName) {
            throw new IllegalArgumentException("serviceName must not be null");
        }
        if (-1 != serviceName.indexOf('/')) {
            throw new IllegalArgumentException("serviceName may not contain '/' characters");
        }
        String address = serviceName;
        if (null != serviceParam) {
            address += "/" + serviceParam;
        }
        EndpointListener removedListener;
        synchronized (incomingMessageListeners) {
            removedListener = incomingMessageListeners.remove(address);
        }
        if (parentEndpoint != null) {
            if (serviceName.startsWith(ChannelMessenger.InsertedServicePrefix)) {
                parentEndpoint.removeIncomingMessageListener(serviceName, serviceParam);
            } else {
                parentEndpoint.removeIncomingMessageListener(myServiceName, address);
            }
        }
        return removedListener;
    }

    /**
     * Returns a local transport that can send to the given address. For now
     * this is based only on the protocol name.
     *
     * @param addr the endpoint address
     * @return the transport if the address protocol is supported by this transport
     */
    private MessageSender getLocalSenderForAddress(EndpointAddress addr) {
        Iterator<MessageTransport> localTransports = getAllLocalTransports();
        while (localTransports.hasNext()) {
            MessageTransport transpt = localTransports.next();
            if (!transpt.getProtocolName().equals(addr.getProtocolName())) {
                continue;
            }
            if (!(transpt instanceof MessageSender)) {
                continue;
            }
            return (MessageSender) transpt;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Note: canonical messenger itself does not do any address rewriting.
     * Any address rewriting must be specified when getting a channel. However,
     * canonical knows the default group redirection for its owning endpoint and
     * will automatically skip redirection if it is the same.
     */
    public Messenger getCanonicalMessenger(EndpointAddress addr, Object hint) {
        if (addr == null) {
            throw new IllegalArgumentException("null endpoint address not allowed.");
        }
        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            Throwable trace = new Throwable("Stack Trace");
            StackTraceElement elements[] = trace.getStackTrace();
            int position = 1;
            while (elements[position].getClassName().startsWith("net.jxta.impl.endpoint.EndpointService")) {
                position++;
            }
            if ((elements.length - 1) == position) {
                position--;
            }
            LOG.fine("Get Messenger for " + addr + " by " + elements[position]);
        }
        synchronized (messengerMap) {
            Reference<Messenger> ref = messengerMap.get(addr);
            if (ref != null) {
                Messenger found = ref.get();
                if ((found != null) && ((found.getState() & Messenger.USABLE) != 0)) {
                    return found;
                }
                messengerMap.remove(addr);
            }
            if (getLocalSenderForAddress(addr) != null) {
                OutboundMeter messengerMeter = null;
                if (EndpointMeterBuildSettings.ENDPOINT_METERING && (endpointServiceMonitor != null)) {
                    messengerMeter = endpointServiceMonitor.getOutboundMeter(addr);
                }
                Messenger m = new CanonicalMessenger(vmQueueSize, addr, null, hint, messengerMeter);
                messengerMap.put(m.getDestinationAddress(), new SoftReference<Messenger>(m));
                return m;
            }
        }
        if (parentEndpoint == null) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Could not create messenger for : " + addr);
            }
            return null;
        }
        return parentEndpoint.getCanonicalMessenger(addr, hint);
    }

    /**
     * Return only the message transport registered locally.
     */
    protected Iterator<MessageTransport> getAllLocalTransports() {
        List<MessageTransport> transportList;
        synchronized (messageTransports) {
            transportList = new ArrayList<MessageTransport>(messageTransports);
        }
        return transportList.iterator();
    }

    /**
     * Returns a messenger for the specified address from one of the Message
     * Transports registered with this very endpoint service. Message
     * Transports inherited from parent groups will not be used.
     *
     * @param addr The destination address of the desired Messenger.
     * @param hint A hint provided to the Message Transport which may assist it
     *             in creating the messenger.
     * @return A Messenger for the specified destination address or {@code null}
     *         if no Messenger could be created.
     */
    private Messenger getLocalTransportMessenger(EndpointAddress addr, Object hint) {
        MessageSender sender = getLocalSenderForAddress(addr);
        Messenger messenger = null;
        if (sender != null) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Trying address \'" + addr + "\' with : " + sender);
            }
            messenger = sender.getMessenger(addr, hint);
        }
        if (messenger == null) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Couldn\'t create messenger for : " + addr);
            }
        }
        return messenger;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void addIncomingMessageFilterListener(MessageFilterListener listener, String namespace, String name) {
        if (null == listener) {
            throw new IllegalArgumentException("listener must be non-null");
        }
        FilterListenerAndMask aFilter = new FilterListenerAndMask(listener, namespace, name);
        incomingFilterListeners.add(aFilter);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void addOutgoingMessageFilterListener(MessageFilterListener listener, String namespace, String name) {
        if (null == listener) {
            throw new IllegalArgumentException("listener must be non-null");
        }
        FilterListenerAndMask aFilter = new FilterListenerAndMask(listener, namespace, name);
        outgoingFilterListeners.add(aFilter);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized MessageFilterListener removeIncomingMessageFilterListener(MessageFilterListener listener, String namespace, String name) {
        Iterator<FilterListenerAndMask> eachListener = incomingFilterListeners.iterator();
        while (eachListener.hasNext()) {
            FilterListenerAndMask aFilter = eachListener.next();
            if (listener == aFilter.listener) {
                eachListener.remove();
                return listener;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized MessageFilterListener removeOutgoingMessageFilterListener(MessageFilterListener listener, String namespace, String name) {
        Iterator<FilterListenerAndMask> eachListener = outgoingFilterListeners.iterator();
        while (eachListener.hasNext()) {
            FilterListenerAndMask aFilter = eachListener.next();
            if ((listener == aFilter.listener) && ((null != namespace) ? namespace.equals(aFilter.namespace) : (null == aFilter.namespace)) && ((null != name) ? name.equals(aFilter.name) : (null == aFilter.name))) {
                eachListener.remove();
                return listener;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * <p/>Redistribute the event to those interested.
     */
    public boolean messengerReady(MessengerEvent event) {
        Messenger messenger = event.getMessenger();
        Messenger messengerForHere;
        EndpointAddress connAddr = event.getConnectionAddress();
        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine("New " + messenger + " for : " + messenger.getDestinationAddress() + " (" + messenger.getLogicalDestinationAddress() + ")");
        }
        int highestPrec = EndpointService.HighPrecedence;
        int lowestPrec = EndpointService.LowPrecedence;
        if (connAddr != null) {
            String cgServiceName = connAddr.getServiceName();
            if (cgServiceName == null || !cgServiceName.startsWith(ChannelMessenger.InsertedServicePrefix)) {
            } else if (!myServiceName.equals(cgServiceName)) {
                highestPrec = EndpointService.LowPrecedence;
            } else {
                lowestPrec = EndpointService.LowPrecedence + 1;
                String serviceParam = connAddr.getServiceParameter();
                String realService = null;
                String realParam = null;
                if (null != serviceParam) {
                    int slashAt = serviceParam.indexOf('/');
                    if (-1 == slashAt) {
                        realService = serviceParam;
                    } else {
                        realService = serviceParam.substring(0, slashAt);
                        realParam = serviceParam.substring(slashAt + 1);
                    }
                }
                connAddr = new EndpointAddress(connAddr, realService, realParam);
            }
        }
        messengerForHere = event.getMessenger().getChannelMessenger(group.getPeerGroupID(), null, null);
        for (int prec = highestPrec + 1; prec-- > lowestPrec; ) {
            MessengerEvent newMessenger = new MessengerEvent(event.getSource(), prec == EndpointService.LowPrecedence ? messenger : messengerForHere, connAddr);
            Collection<MessengerEventListener> allML = new ArrayList<MessengerEventListener>(passiveMessengerListeners[prec]);
            for (MessengerEventListener listener : allML) {
                try {
                    if (listener.messengerReady(newMessenger)) {
                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.fine(newMessenger + " claimed by " + listener);
                        }
                        return true;
                    }
                } catch (Throwable all) {
                    if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                        LOG.log(Level.WARNING, "Uncaught Throwable in listener " + listener, all);
                    }
                }
            }
        }
        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine("Nobody cared about " + event);
        }
        return false;
    }

    private void addActiveRelayListener(PeerGroup listeningGroup) {
        PeerGroup parentGroup = group.getParentGroup();
        while (parentGroup != null) {
            EndpointService parentEndpoint = parentGroup.getEndpointService();
            for (Iterator<MessageTransport> it = parentEndpoint.getAllMessageTransports(); it.hasNext(); ) {
                MessageTransport mt = it.next();
                if ((mt instanceof RelayClient)) {
                    ((RelayClient) mt).addActiveRelayListener(listeningGroup);
                    break;
                }
            }
            parentGroup = parentGroup.getParentGroup();
        }
    }

    private void removeActiveRelayListener(PeerGroup listeningGroup) {
        PeerGroup parentGroup = group.getParentGroup();
        while (parentGroup != null) {
            EndpointService parentEndpoint = parentGroup.getEndpointService();
            for (Iterator<MessageTransport> it = parentEndpoint.getAllMessageTransports(); it.hasNext(); ) {
                MessageTransport mt = it.next();
                if ((mt instanceof RelayClient)) {
                    ((RelayClient) mt).removeActiveRelayListener(listeningGroup);
                    break;
                }
            }
            parentGroup = parentGroup.getParentGroup();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated legacy method.
     */
    @Deprecated
    public boolean ping(EndpointAddress addr) {
        throw new UnsupportedOperationException("Legacy method not implemented. Use an interface object.");
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated legacy method.
     */
    @Deprecated
    public boolean getMessenger(MessengerEventListener listener, EndpointAddress addr, Object hint) {
        throw new UnsupportedOperationException("Legacy method not implemented. Use an interface object.");
    }

    /**
     * {@inheritDoc}
     * <p/>
     * convenience method not supported here.
     */
    public Messenger getMessenger(EndpointAddress addr) {
        throw new UnsupportedOperationException("Convenience method not implemented. Use an interface object.");
    }

    /**
     * {@inheritDoc}
     * <p/>
     * convenience method not supported here.
     */
    public Messenger getMessengerImmediate(EndpointAddress addr, Object hint) {
        throw new UnsupportedOperationException("Convenience method not implemented. Use an interface object.");
    }

    /**
     * {@inheritDoc}
     * <p/>
     * convenience method not supported here.
     */
    public Messenger getMessenger(EndpointAddress addr, Object hint) {
        throw new UnsupportedOperationException("Convenience method not implemented. Use an interface object.");
    }

    /**
     * Returns a Direct Messenger that may be used to send messages via  this endpoint
     * to the specified destination.
     *
     * @param address the destination address.
     * @param hint the messenger hint, if any, otherwise null.
     * @param exclusive if true avoids caching the messenger
     * @return The messenger or {@code null} is returned if the destination address is not reachable.
     * @throws IllegalArgumentException if hint is not of RouteAdvertisement, or PeerAdvertisement type.
     */
    public Messenger getDirectMessenger(EndpointAddress address, Object hint, boolean exclusive) {
        if (!exclusive) {
            Reference<Messenger> reference = directMessengerMap.get(address);
            if (reference != null) {
                Messenger messenger = reference.get();
                if (messenger != null && !messenger.isClosed()) {
                    return messenger;
                }
            }
        }
        TcpTransport tcpTransport = (TcpTransport) getMessageTransport("tcp");
        if ((tcpTransport != null) && (hint != null)) {
            RouteAdvertisement route;
            EndpointAddress direct;
            Messenger messenger;
            if (hint instanceof RouteAdvertisement) {
                route = (RouteAdvertisement) hint;
            } else if (hint instanceof PeerAdvertisement) {
                route = EndpointUtils.extractRouteAdv((PeerAdvertisement) hint);
            } else {
                throw new IllegalArgumentException("Unknown route hint object type" + hint);
            }
            for (EndpointAddress transportAddr : route.getDestEndpointAddresses()) {
                if (transportAddr.getProtocolName().equals("tcp")) {
                    direct = createDirectAddress(transportAddr, address);
                    messenger = tcpTransport.getMessenger(direct, route, false);
                    if (messenger != null) {
                        if (!exclusive) {
                            directMessengerMap.put(address, new WeakReference<Messenger>(messenger));
                        }
                        return messenger;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Given a transport address and service address, create a mangled endpoint address
     *
     * @param transportAddr   the transport messenger address
     * @param serviceEndpoint the service endpoint
     * @return an composite endpoint address
     */
    private EndpointAddress createDirectAddress(EndpointAddress transportAddr, EndpointAddress serviceEndpoint) {
        StringBuilder destStr = new StringBuilder(transportAddr.toString()).append("/");
        destStr.append(ENDPOINTSERVICE_NAME);
        destStr.append(":").append(group.getPeerGroupID().getUniqueValue().toString()).append("/");
        destStr.append(serviceEndpoint.getServiceName()).append("/").append(serviceEndpoint.getServiceParameter());
        return new EndpointAddress(destStr.toString());
    }
}
