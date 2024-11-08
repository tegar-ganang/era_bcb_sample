package fulmine.context;

import static fulmine.util.Utils.COLON;
import static fulmine.util.Utils.COMMA_SPACE;
import static fulmine.util.Utils.EMPTY_STRING;
import static fulmine.util.Utils.SPACING_4_CHARS;
import static fulmine.util.Utils.logException;
import static fulmine.util.Utils.nullCheck;
import static fulmine.util.Utils.safeToString;
import static fulmine.util.Utils.string;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.SystemUtils;
import fulmine.AbstractLifeCycle;
import fulmine.IDomain;
import fulmine.IType;
import fulmine.distribution.IDistributionManager;
import fulmine.distribution.IDistributionState;
import fulmine.distribution.IRemoteUpdateInvoker;
import fulmine.distribution.RemoteUpdateInvoker;
import fulmine.distribution.channel.Channel;
import fulmine.distribution.channel.ChannelFactory;
import fulmine.distribution.channel.ChannelReadyEvent;
import fulmine.distribution.channel.ChannelTransmissionListener;
import fulmine.distribution.channel.IChannel;
import fulmine.distribution.channel.IChannelFactory;
import fulmine.distribution.connection.IConnection;
import fulmine.distribution.connection.IConnectionDiscoverer;
import fulmine.distribution.connection.IConnectionParameters;
import fulmine.distribution.events.ConnectionAvailableEvent;
import fulmine.distribution.events.ConnectionDestroyedEvent;
import fulmine.distribution.events.ContextDiscoveredEvent;
import fulmine.distribution.events.ContextNotAvailableEvent;
import fulmine.event.IEvent;
import fulmine.event.IEventManager;
import fulmine.event.listener.AbstractEventHandler;
import fulmine.event.listener.IEventListener;
import fulmine.event.listener.ILifeCycleEventListener;
import fulmine.event.listener.MultiSystemEventListener;
import fulmine.event.subscription.ISubscriptionListener;
import fulmine.event.subscription.ISubscriptionManager;
import fulmine.event.subscription.ISubscriptionParameters;
import fulmine.event.subscription.SubscriptionParameters;
import fulmine.event.system.AbstractSystemEvent;
import fulmine.event.system.EventSourceNotObservedEvent;
import fulmine.event.system.EventSourceObservedEvent;
import fulmine.event.system.ISystemEventListener;
import fulmine.event.system.SubscribeEvent;
import fulmine.event.system.UnsubscribeEvent;
import fulmine.model.container.IContainer;
import fulmine.model.container.subscription.ContainerSubscriptionManager;
import fulmine.model.field.IntegerField;
import fulmine.model.field.StringField;
import fulmine.protocol.specification.FrameReader;
import fulmine.protocol.specification.FrameWriter;
import fulmine.protocol.specification.IFrameReader;
import fulmine.protocol.specification.IFrameWriter;
import fulmine.rpc.IRpcResult;
import fulmine.util.collection.CollectionFactory;
import fulmine.util.collection.CollectionUtils;
import fulmine.util.log.AsyncLog;
import fulmine.util.reference.AutoCreatingStore;
import fulmine.util.reference.DualValue;
import fulmine.util.reference.IAutoCreatingStore;
import fulmine.util.reference.IObjectBuilder;
import fulmine.util.reference.IReferenceCounter;
import fulmine.util.reference.ReferenceCounter;
import fulmine.util.reference.Values;

/**
 * The manager of subscriptions for distribution of events within the local
 * context and from remote contexts. The manager also provides retransmission
 * and retransmission-request operations.
 * <p>
 * This registers for the following events:
 * <ul>
 * <li>{@link ContextDiscoveredEvent} - when a remote context is discovered, the
 * manager will initiate creation of an {@link IConnection} to the remote
 * context if there is a subscription waiting for a remote container in that
 * remote context, otherwise it will simply cache the connection parameters
 * against the remote context's identity for future use.
 * <li>{@link ContextNotAvailableEvent} - raised when a remote context is no
 * longer available. Any connection to the remote context (and by association
 * the channel) is destroyed after this event is received.
 * <li>{@link ConnectionAvailableEvent} - this is raised when an
 * {@link IConnection} has been created between the local and a remote context.
 * After this, an {@link IChannel} instance will be created using the
 * connection. When 2 contexts decide to connect to each other, duplicate
 * connections can occur between the contexts; only 1 connection is required.
 * The manager handles this scenario as follows:
 * <ul>
 * <li>If the name of this context is semantically greater than the other
 * <u>and</u> there is already a connection between the 2 contexts, the new
 * connection is destroyed (if the names are equal, the hashcodes of the
 * contexts are compared, see {@link IFrameworkContext#getContextHashCode()}).
 * This will cause the {@link IChannel} in the remote context to shutdown (for a
 * brief period the remote context may have had 2 channels, but only one will
 * have completed its initialisation sequence).
 * <li>Else a new channel is created using the connection. The channel will
 * begin its synchronisation with the remote peer channel - this may not
 * complete if the remote context destroys the connection as per the previous
 * point.
 * </ul>
 * <li>{@link ConnectionDestroyedEvent} - when an {@link IConnection} is
 * destroyed, the corresponding {@link IChannel} is also destroyed.
 * <li>{@link ChannelReadyEvent} - this is received when a {@link Channel} has
 * been created over an {@link IConnection} to a remote context and the channel
 * has synchronised its readiness with the other remote peer channel. At this
 * point the channel is available for use to issue subscriptions. Any
 * subscriptions that have been received by the manager for remote containers in
 * the remote context this channel connects to can now be issued.
 * </ul>
 * <p>
 * The synchronisation rule-of-thumb for this class is that any operations
 * involving I/O should not be executed whilst holding any object monitors.
 * 
 * @see IFulmineContext
 * @author Ramon Servadei
 */
class DistributionManager extends AbstractLifeCycle implements IDistributionManager {

    static final AsyncLog LOG = new AsyncLog(DistributionManager.class);

    /** The shared state */
    private IDistributionState state;

    /**
     * Standard constructor
     * 
     * @param context
     *            the context this is associated with
     */
    DistributionManager(IFrameworkContext context) {
        this(new DistributionState(context));
    }

    /**
     * Internally chained constructor
     * 
     * @param state
     *            the state for the manager
     */
    DistributionManager(IDistributionState state) {
        super();
        this.state = state;
    }

    @Override
    protected AsyncLog getLog() {
        return LOG;
    }

    @Override
    protected void doStart() {
        getState().init();
        getState().start();
        if (getLog().isInfoEnabled()) {
            getLog().info("Started");
        }
    }

    public boolean subscribe(String contextIdentity, String identityRegex, IType type, IDomain domain, IEventListener listener) {
        final SubscriptionParameters parameters = new SubscriptionParameters(identityRegex, type, domain);
        if (getLog().isInfoEnabled()) {
            getLog().info("[subscribe] context=" + contextIdentity + COMMA_SPACE + parameters + ", listener=" + safeToString(listener));
        }
        if (getState().getContext().getIdentity().equals(contextIdentity)) {
            boolean subscribed = false;
            final ISubscriptionManager subscriptionManager = getState().getSubscriptionManager();
            subscriptionManager.subscribe(parameters);
            subscribed = subscriptionManager.addListener(parameters, listener);
            return subscribed;
        }
        return doRemoteSubscribe(contextIdentity, parameters, listener);
    }

    /**
     * Performs the actions to service a remote subscription
     * 
     * @param remoteContextIdentity
     *            the remote context for the subscription
     * @param parameters
     *            the parameters for the subscription
     * @param listener
     *            the listener for the subscription
     * @return <code>true</code> if the subscription was created,
     *         <code>false</code> if it already existed
     */
    protected boolean doRemoteSubscribe(String remoteContextIdentity, ISubscriptionParameters parameters, IEventListener listener) {
        boolean subscribed = false;
        nullCheck(remoteContextIdentity, "No remote context identity");
        IConnectionParameters connectionParameters = null;
        DualValue<ISubscriptionParameters, IEventListener> values = new DualValue<ISubscriptionParameters, IEventListener>(parameters, listener);
        boolean connect = false;
        final IChannel channel;
        synchronized (getState()) {
            final Set<DualValue<ISubscriptionParameters, IEventListener>> remoteSubscriptions = getState().getRemoteSubscriptions().get(remoteContextIdentity);
            subscribed = !remoteSubscriptions.contains(values);
            remoteSubscriptions.add(values);
            if (getLog().isTraceEnabled()) {
                getLog().trace("Remote subscriptions for remote context " + remoteContextIdentity + " are" + CollectionUtils.toFormattedString(remoteSubscriptions));
            }
            connectionParameters = getState().getDiscoveredContexts().get(remoteContextIdentity);
            channel = getState().getChannels().get(remoteContextIdentity);
            if (channel == null) {
                connect = shouldConnect(remoteContextIdentity, connectionParameters);
            }
        }
        if (channel != null) {
            getState().getContext().queueEvent(new RemoteContainerSubscriptionEvent(getState().getContext(), remoteContextIdentity, values));
        }
        if (connect) {
            getState().getContext().getConnectionBroker().connect(connectionParameters);
        }
        return subscribed;
    }

    /**
     * Helper method to determine whether a connection should be made to the
     * remote context to issue the subscription. This method checks that there
     * is no connection currently existing or being made and that the context is
     * available.
     * 
     * @param remoteContextIdentity
     *            the remote context that the connection would be for
     * @param connectionParameters
     *            the connection parameters
     * @return <code>true</code> if a connection should be attempted
     */
    private boolean shouldConnect(String remoteContextIdentity, IConnectionParameters connectionParameters) {
        synchronized (getState()) {
            boolean connect = false;
            if (connectionParameters != null) {
                final String connectionIdentity = connectionParameters.getRemoteContextIdentity();
                if (getState().getConnectedContexts().getCount(connectionIdentity) == 0) {
                    if (getState().getCONNECTINGContexts().getCount(connectionIdentity) == 0) {
                        getState().getCONNECTINGContexts().adjustCount(connectionIdentity, 1);
                        if (getLog().isTraceEnabled()) {
                            getLog().trace("CONNECTING contexts are " + safeToString(getState().getCONNECTINGContexts()));
                        }
                        connect = true;
                    } else {
                        if (getLog().isTraceEnabled()) {
                            getLog().trace("Already trying to connect to " + safeToString(connectionParameters));
                        }
                    }
                } else {
                    if (getLog().isTraceEnabled()) {
                        getLog().trace("Already connected to " + safeToString(connectionParameters));
                    }
                }
            } else {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Remote context " + remoteContextIdentity + " has not been discovered yet");
                }
            }
            return connect;
        }
    }

    public boolean unsubscribe(String contextIdentity, String identityRegex, IType type, IDomain domain, IEventListener listener) {
        final SubscriptionParameters parameters = new SubscriptionParameters(identityRegex, type, domain);
        if (getLog().isInfoEnabled()) {
            getLog().info("[unsubscribe] context=" + contextIdentity + COMMA_SPACE + parameters + ", listener=" + safeToString(listener));
        }
        if (getState().getContext().getIdentity().equals(contextIdentity)) {
            boolean unsubscribed = false;
            if (canUnsubscribe(getState().getSubscriptionManager(), parameters, listener, false)) {
                unsubscribed = getState().getSubscriptionManager().unsubscribe(parameters);
            }
            return unsubscribed;
        }
        return doRemoteUnsubscribe(contextIdentity, parameters, listener);
    }

    /**
     * Helper method to remove the listener from the subscription managed by the
     * manager that is passed in. It returns whether the subscription can be
     * removed because there are no more application listeners. This works for
     * both local and remote unsubscribe operations.
     * 
     * @param subscriptionManager
     *            the subscription manager to operate on
     * @param parameters
     *            the parameters identifying the subscription
     * @param listener
     *            the listener to remove
     * @param unsubscribeRemote
     *            whether this is an unsubscribe for a remote container
     * @return <code>true</code> if there are no more application listeners
     *         associated with this subscription and thus the subscription can
     *         be removed. For a remote subscription, if there is only the
     *         {@link ChannelTransmissionListener} remaining as a listener, this
     *         effectively means there are no more application listeners.
     */
    private final boolean canUnsubscribe(ISubscriptionManager subscriptionManager, final ISubscriptionParameters parameters, IEventListener listener, boolean unsubscribeRemote) {
        if (getLog().isTraceEnabled()) {
            getLog().trace("removing listener " + safeToString(listener) + " for " + safeToString(parameters) + " from subscription listeners " + CollectionUtils.toFormattedString(subscriptionManager.getListeners(parameters)));
        }
        subscriptionManager.removeListener(parameters, listener);
        final List<IEventListener> listeners = subscriptionManager.getListeners(parameters);
        boolean canUnsubscribe = false;
        if (listeners.size() == 0) {
            canUnsubscribe = true;
        } else {
            if (unsubscribeRemote) {
                canUnsubscribe = applicationListenersExist(listeners);
            }
        }
        if (getLog().isTraceEnabled()) {
            getLog().trace("Can" + (canUnsubscribe ? EMPTY_STRING : "not") + " unsubscribe " + safeToString(parameters) + ", subscribed sources: " + CollectionUtils.toFormattedString(subscriptionManager.getSubscribedSources(parameters)) + ", listeners: " + CollectionUtils.toFormattedString(subscriptionManager.getListeners(parameters)));
        }
        return canUnsubscribe;
    }

    /**
     * Identify if there are any application listeners in the list of listeners.
     * Any application listeners existing mean that unsubscription cannot
     * continue.
     * 
     * @param listeners
     *            the list of listeners to examine for any application listeners
     * @return <code>false</code> if there are any application listeners and
     *         unsubscription should not continue
     */
    protected boolean applicationListenersExist(final List<IEventListener> listeners) {
        boolean canUnsubscribe = false;
        if (listeners.size() == 1 && listeners.get(0) instanceof ChannelTransmissionListener) {
            canUnsubscribe = true;
        }
        return canUnsubscribe;
    }

    /**
     * Performs the actions to service an unsubscribe operation for a remote
     * container.
     * 
     * @param remoteContextIdentity
     *            the remote context to unsubscribe from
     * @param parameters
     *            the subscription parameters
     * @param listener
     *            the listener to unsubscribe
     * @return <code>true</code> if the subscription was found and removed,
     *         <code>false</code> otherwise
     */
    protected boolean doRemoteUnsubscribe(String remoteContextIdentity, ISubscriptionParameters parameters, IEventListener listener) {
        IChannel channel = null;
        if (remoteContextIdentity != null) {
            final DualValue<ISubscriptionParameters, IEventListener> values = new DualValue<ISubscriptionParameters, IEventListener>(parameters, listener);
            synchronized (getState()) {
                final Set<DualValue<ISubscriptionParameters, IEventListener>> remoteSubscriptions = getState().getRemoteSubscriptions().get(remoteContextIdentity);
                remoteSubscriptions.remove(values);
                if (getLog().isTraceEnabled()) {
                    getLog().trace("Remote subscriptions for remote context " + remoteContextIdentity + " are now " + CollectionUtils.toFormattedString(remoteSubscriptions));
                }
            }
            channel = getState().getChannels().get(remoteContextIdentity);
        }
        if (channel != null) {
            if (canUnsubscribe(channel, parameters, listener, true)) {
                return channel.unsubscribe(parameters);
            }
        }
        return false;
    }

    @Override
    protected void doDestroy() {
        getState().destroy();
        for (IChannel channel : getState().getChannels().values()) {
            try {
                channel.destroy();
            } catch (Exception e) {
                logException(getLog(), channel, e);
            }
        }
        getState().getChannels().clear();
    }

    public IFrameReader getFrameReader() {
        return getState().getFrameReader();
    }

    public IFrameWriter getFrameWriter() {
        return getState().getFrameWriter();
    }

    public void requestRetransmit(String contextIdentity, String identityRegularExpression, IType type, IDomain domain) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("requestRetransmit for context=" + contextIdentity + ", identity=" + identityRegularExpression + ", type=" + type + ", domain=" + domain);
        }
        final IChannel channel = getChannel(contextIdentity);
        if (channel != null) {
            channel.requestRetransmit(identityRegularExpression, type, domain);
        }
    }

    public void requestRetransmitAll(String contextIdentity) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("requestRetransmitAll for context=" + contextIdentity);
        }
        final IChannel channel = getChannel(contextIdentity);
        if (channel != null) {
            channel.requestRetransmitAll();
        }
    }

    public void retransmit(String contextIdentity, String identityRegularExpression, IType type, IDomain domain) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("retransmit for context=" + contextIdentity + ", identity=" + identityRegularExpression + ", type=" + type + ", domain=" + domain);
        }
        final IChannel channel = getChannel(contextIdentity);
        if (channel != null) {
            channel.retransmit(identityRegularExpression, type, domain);
        }
    }

    public void retransmitAll(String contextIdentity) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("requestAll for context=" + contextIdentity);
        }
        final IChannel channel = getChannel(contextIdentity);
        if (channel != null) {
            channel.retransmitAll();
        }
    }

    public void retransmitAllToAll() {
        if (getLog().isDebugEnabled()) {
            getLog().debug("requestAllToAll");
        }
        for (IChannel channel : getState().getChannels().values()) {
            try {
                channel.retransmitAll();
            } catch (Exception e) {
                logException(getLog(), channel, e);
            }
        }
    }

    public void retransmitToAll(String identityRegularExpression, IType type, IDomain domain) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("retransmitToAll for identity=" + identityRegularExpression + ", type=" + type + ", domain=" + domain);
        }
        for (IChannel channel : getState().getChannels().values()) {
            try {
                channel.retransmit(identityRegularExpression, type, domain);
            } catch (Exception e) {
                logException(getLog(), channel, e);
            }
        }
    }

    private IChannel getChannel(String contextIdentity) {
        return getState().getChannels().get(contextIdentity);
    }

    public IChannel[] getConnectedChannels() {
        final Collection<IChannel> values = getState().getChannels().values();
        return values.toArray(new IChannel[values.size()]);
    }

    /**
     * Log the current connected channels
     */
    public void logConnectedChannels() {
        StringBuilder sb = new StringBuilder();
        final IChannel[] connectedChannels = getConnectedChannels();
        for (IChannel channel : connectedChannels) {
            sb.append(SystemUtils.LINE_SEPARATOR).append(SPACING_4_CHARS).append(safeToString(channel.toString()));
        }
        if (connectedChannels.length == 0) {
            sb.append(SystemUtils.LINE_SEPARATOR).append(SPACING_4_CHARS).append("<UNCONNECTED>");
        }
        if (getLog().isInfoEnabled()) {
            getLog().info(SystemUtils.LINE_SEPARATOR + this + " connected to:" + sb);
        }
    }

    public boolean addSubscriptionListener(ISubscriptionListener listener) {
        boolean addListener = getState().getContext().getSystemEventSource(EventSourceObservedEvent.class).addListener(listener);
        addListener |= getState().getContext().getSystemEventSource(EventSourceNotObservedEvent.class).addListener(listener);
        addListener |= getState().getContext().getSystemEventSource(SubscribeEvent.class).addListener(listener);
        addListener |= getState().getContext().getSystemEventSource(UnsubscribeEvent.class).addListener(listener);
        return addListener;
    }

    public boolean removeSubscriptionListener(ISubscriptionListener listener) {
        boolean removeListener = getState().getContext().getSystemEventSource(EventSourceNotObservedEvent.class).removeListener(listener);
        removeListener |= getState().getContext().getSystemEventSource(EventSourceObservedEvent.class).removeListener(listener);
        removeListener |= getState().getContext().getSystemEventSource(SubscribeEvent.class).removeListener(listener);
        removeListener |= getState().getContext().getSystemEventSource(UnsubscribeEvent.class).removeListener(listener);
        return removeListener;
    }

    public String updateRemoteContainer(String remoteContextIdentity, String identity, IType type, IDomain domain, String fieldName, String fieldValueAsString) {
        getState().getContext().addRpcPublicationListener(remoteContextIdentity, getState().getRemoteUpdateInvoker(remoteContextIdentity));
        final IRpcResult result = getState().getRemoteUpdateInvoker(remoteContextIdentity).invoke(remoteContextIdentity, new StringField("identity", identity), new IntegerField("type", type.value()), new IntegerField("domain", domain.value()), new StringField("fieldName", fieldName), new StringField("fieldValueString", fieldValueAsString), new IntegerField("permissionApp", getState().getContext().getPermissionProfile().getApplicationCode()), new IntegerField("permissionCode", getState().getContext().getPermissionProfile().getPermissionCode()), new StringField("remoteContextIdentity", getState().getContext().getIdentity()));
        if (result == null) {
            return "Null result, RPC definition not available";
        }
        if (result.isSuccessful()) {
            return result.getResult().getValue().toString();
        }
        return result.getExceptionMessage();
    }

    public void invokeRpc(String remoteContextIdentity, byte[] rpcData) {
        final IChannel channel = getState().getChannels().get(remoteContextIdentity);
        if (channel == null) {
            throw new RuntimeException("There is no channel available for " + remoteContextIdentity);
        }
        channel.invokeRpc(remoteContextIdentity, rpcData);
    }

    IDistributionState getState() {
        return this.state;
    }

    void setState(IDistributionState state) {
        this.state = state;
    }
}

/**
 * Internal state for the {@link DistributionManager}.
 * 
 * @author Ramon Servadei
 * 
 */
class DistributionState extends AbstractLifeCycle implements IDistributionState {

    /**
     * A builder for {@link RemoteUpdateInvoker} objects, keyed by their remote
     * context identity
     * 
     * @author Ramon Servadei
     */
    final class RemoteUpdateInvokerBuilder implements IObjectBuilder<String, IRemoteUpdateInvoker> {

        public IRemoteUpdateInvoker create(String key) {
            return new RemoteUpdateInvoker(key, getContext());
        }
    }

    /**
     * A builder of a {@link List} of {@link Values}
     * 
     * @author Ramon Servadei
     * 
     */
    static final class ValuesBuilder implements IObjectBuilder<String, Set<DualValue<ISubscriptionParameters, IEventListener>>> {

        public Set<DualValue<ISubscriptionParameters, IEventListener>> create(String key) {
            return CollectionFactory.newSet(1);
        }
    }

    /**
     * The channels the manager has. Uses the <a
     * href="http://www.ibm.com/developerworks/java/library/j-jtp06197.html"
     * >'cheap read-write lock'</a>
     */
    private volatile Map<String, IChannel> channels;

    private final Map<String, IConnectionParameters> discoveredConnections;

    private final ISubscriptionManager subscriptionManager;

    private final IAutoCreatingStore<String, Set<DualValue<ISubscriptionParameters, IEventListener>>> remoteSubscriptions;

    private final IReferenceCounter<String> connectedContexts;

    private final IReferenceCounter<String> CONNECTINGContexts;

    private final IFrameworkContext context;

    private final IFrameReader frameReader;

    private final IFrameWriter frameWriter;

    private ILifeCycleEventListener eventHandler;

    private IChannelFactory channelFactory;

    private IAutoCreatingStore<String, IRemoteUpdateInvoker> remoteUpdateInvokers;

    DistributionState(IFrameworkContext context) {
        super();
        this.context = context;
        this.discoveredConnections = CollectionFactory.newMap(2);
        this.channels = CollectionFactory.newMap(2);
        this.subscriptionManager = new ContainerSubscriptionManager(this.context, null);
        this.remoteSubscriptions = new AutoCreatingStore<String, Set<DualValue<ISubscriptionParameters, IEventListener>>>(new ValuesBuilder());
        this.connectedContexts = new ReferenceCounter<String>();
        this.CONNECTINGContexts = new ReferenceCounter<String>();
        this.frameReader = new FrameReader();
        this.frameWriter = new FrameWriter();
        this.remoteUpdateInvokers = new AutoCreatingStore<String, IRemoteUpdateInvoker>(new RemoteUpdateInvokerBuilder());
    }

    /**
     * Initialise the state
     */
    public void init() {
        this.channelFactory = createChannelFactory();
        this.eventHandler = new MultiSystemEventListener(string(this, context.toString()), context, AbstractEventHandler.getEventHandlerMappings(createEventHandlers()));
    }

    public IChannelFactory createChannelFactory() {
        return new ChannelFactory();
    }

    @SuppressWarnings("unchecked")
    AbstractEventHandler<? extends IEvent>[] createEventHandlers() {
        return new AbstractEventHandler[] { new ChannelReadyEventHandler(this), new ConnectionAvailableEventHandler(this), new ConnectionDestroyedEventHandler(this), new RemoteContainerSubscriptionEventHandler(this), new RemoteEventSourceNotObservedEventHandler(this), new ContextDiscoveredEventHandler(this), new ContextNotAvailableEventHandler(this) };
    }

    protected void doDestroy() {
        this.eventHandler.destroy();
        this.subscriptionManager.destroy();
    }

    protected void doStart() {
        this.eventHandler.start();
        this.subscriptionManager.start();
    }

    public final IChannelFactory getChannelFactory() {
        return this.channelFactory;
    }

    public final ILifeCycleEventListener getEventHandler() {
        return this.eventHandler;
    }

    public final Map<String, IChannel> getChannels() {
        return this.channels;
    }

    public final Map<String, IConnectionParameters> getDiscoveredContexts() {
        return this.discoveredConnections;
    }

    public final ISubscriptionManager getSubscriptionManager() {
        return this.subscriptionManager;
    }

    public final IAutoCreatingStore<String, Set<DualValue<ISubscriptionParameters, IEventListener>>> getRemoteSubscriptions() {
        return this.remoteSubscriptions;
    }

    public final IReferenceCounter<String> getConnectedContexts() {
        return this.connectedContexts;
    }

    public final IFrameworkContext getContext() {
        return this.context;
    }

    public final IFrameReader getFrameReader() {
        return this.frameReader;
    }

    public final IFrameWriter getFrameWriter() {
        return this.frameWriter;
    }

    public final void setChannels(Map<String, IChannel> channels) {
        this.channels = channels;
    }

    public final IReferenceCounter<String> getCONNECTINGContexts() {
        return this.CONNECTINGContexts;
    }

    public IRemoteUpdateInvoker getRemoteUpdateInvoker(String remoteContextIdentity) {
        return this.remoteUpdateInvokers.get(remoteContextIdentity);
    }
}

/**
 * Base class for distribution manager event handlers
 * 
 * @author Ramon Servadei
 * 
 * @param <T>
 *            the type of event
 */
abstract class DistributionEventHandler<T extends IEvent> extends AbstractEventHandler<T> implements ISystemEventListener {

    /** The distribution state reference */
    private final IDistributionState state;

    DistributionEventHandler(IDistributionState state) {
        super();
        nullCheck(state, "No state provided");
        this.state = state;
    }

    /** Get the state */
    IDistributionState getState() {
        return state;
    }

    /**
     * Compares the {@link IConnection} of the {@link IChannel} against the
     * {@link IConnectionParameters}. If the channel's connection is different
     * to the connection parameters, this method destroys the channel's
     * connection.
     * 
     * @param channel
     *            the channel
     * @param connectionParameters
     *            the connection parameters to compare with the channel's
     *            connection
     * @return <code>true</code> if the channel's connection parameters are
     *         equal to the connection parameters argument
     */
    final boolean validateChannelConnection(final IChannel channel, final IConnectionParameters connectionParameters) {
        if (channel.getConnection().isOutbound() && (connectionParameters != null && !connectionParameters.isEqual(channel.getConnection()))) {
            if (getLog().isInfoEnabled()) {
                getLog().info("Destroying " + safeToString(channel) + " because remote context" + " connection parameters have changed to " + safeToString(connectionParameters));
            }
            channel.getConnection().destroy();
            return false;
        }
        return true;
    }
}

/**
 * Handles the {@link ContextNotAvailableEvent}s raised when a remote context is
 * not available anymore. Removes the context {@link IConnectionParameters} from
 * the discovered contexts collection and destroys the active {@link IChannel}
 * connection if it exists. The channel is not destroyed, only the connection -
 * the {@link ConnectionDestroyedEventHandler} will destroy the channel.
 * 
 * @author Ramon Servadei
 */
class ContextNotAvailableEventHandler extends DistributionEventHandler<ContextNotAvailableEvent> {

    private static final AsyncLog LOG = new AsyncLog(ContextNotAvailableEventHandler.class);

    ContextNotAvailableEventHandler(IDistributionState state) {
        super(state);
    }

    /**
     * Removes the context {@link IConnectionParameters} from the discovered
     * contexts collection and destroys the active {@link IChannel} connection
     * if it exists. The channel is not destroyed, only the connection - the
     * {@link ConnectionDestroyedEventHandler} will destroy the channel.
     */
    @Override
    public void handle(ContextNotAvailableEvent event) {
        IChannel channel;
        synchronized (getState()) {
            getState().getDiscoveredContexts().remove(event.getRemoteContextIdentity());
            channel = getState().getChannels().get(event.getRemoteContextIdentity());
        }
        if (channel != null) {
            final IConnection connection = channel.getConnection();
            if (connection != null) {
                connection.destroy();
            }
        }
    }

    @Override
    public AsyncLog getLog() {
        return LOG;
    }
}

/**
 * Handler for {@link ContextDiscoveredEvent} events. Saves the
 * {@link ContextDiscoveredEvent} and connects to the remote context if there is
 * a remote container subscription for the remote context. This will also
 * destroy any connection to the remote context if the connection parameters
 * have changed.
 * 
 * @author Ramon Servadei
 */
class ContextDiscoveredEventHandler extends DistributionEventHandler<ContextDiscoveredEvent> {

    private static final AsyncLog LOG = new AsyncLog(ContextDiscoveredEventHandler.class);

    ContextDiscoveredEventHandler(IDistributionState state) {
        super(state);
    }

    /**
     * Saves the {@link ContextDiscoveredEvent} and connects to the remote
     * context if there is a remote container subscription for the remote
     * context. This will also destroy any connection to the remote context if
     * the connection parameters have changed.
     */
    @Override
    public void handle(ContextDiscoveredEvent event) {
        if (!getState().getContext().isActive()) {
            if (getLog().isInfoEnabled()) {
                getLog().info("Context is not active, ignoring " + safeToString(event));
            }
            return;
        }
        final IConnectionParameters connectionParameters = event.getConnectionParameters();
        final String remoteContextIdentity = connectionParameters.getRemoteContextIdentity();
        nullCheck(remoteContextIdentity, "No remote context identity");
        boolean connect = false;
        synchronized (getState()) {
            connect = getState().getRemoteSubscriptions().get(remoteContextIdentity).size() > 0;
            if (connect) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Connect pending because there are remote subscriptions to " + remoteContextIdentity + CollectionUtils.toFormattedString(getState().getRemoteSubscriptions().get(remoteContextIdentity)));
                }
                connect = getState().getConnectedContexts().getCount(remoteContextIdentity) == 0;
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Connected contexts are " + safeToString(getState().getConnectedContexts()));
                }
                if (connect) {
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Connect still pending because there is no connection, connecting contexts are " + safeToString(getState().getCONNECTINGContexts()));
                    }
                    connect = getState().getCONNECTINGContexts().getCount(remoteContextIdentity) == 0;
                    if (connect) {
                        getState().getCONNECTINGContexts().adjustCount(remoteContextIdentity, 1);
                    } else {
                        if (getLog().isDebugEnabled()) {
                            getLog().debug("Already trying to connect to " + safeToString(event.getConnectionParameters()));
                        }
                    }
                } else {
                    if (getLog().isDebugEnabled()) {
                        getLog().debug("Already connected to " + safeToString(event.getConnectionParameters()) + ", current connections are: " + safeToString(getState().getConnectedContexts()));
                    }
                }
            }
            getState().getDiscoveredContexts().put(remoteContextIdentity, event.getConnectionParameters());
            if (getLog().isInfoEnabled()) {
                getLog().info("Discovered contexts are: " + CollectionUtils.toFormattedString(getState().getDiscoveredContexts()));
            }
        }
        final IChannel channel = getState().getChannels().get(remoteContextIdentity);
        if (channel != null) {
            validateChannelConnection(channel, event.getConnectionParameters());
        }
        if (connect) {
            try {
                getState().getContext().getConnectionBroker().connect(event.getConnectionParameters());
            } catch (RuntimeException e) {
                synchronized (getState()) {
                    getState().getCONNECTINGContexts().adjustCount(remoteContextIdentity, -1);
                }
                throw e;
            }
        } else {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Not connecting to (already connected or not required) " + safeToString(event.getConnectionParameters()));
            }
        }
    }

    @Override
    public AsyncLog getLog() {
        return LOG;
    }
}

/**
 * Handler for {@link ConnectionAvailableEvent} events. Creates an
 * {@link IChannel} using the {@link IConnection} encapsulated in the event.
 * Also deals with the situation where two contexts have both connected to each
 * other; the context with the semantically greater identity will severe/close
 * the latest connection thus leaving only one connection between the two
 * contexts.
 * 
 * @author Ramon Servadei
 * 
 */
final class ConnectionAvailableEventHandler extends DistributionEventHandler<ConnectionAvailableEvent> {

    private static final AsyncLog LOG = new AsyncLog(ConnectionAvailableEventHandler.class);

    ConnectionAvailableEventHandler(IDistributionState state) {
        super(state);
    }

    /**
     * Creates an {@link IChannel} using the {@link IConnection} encapsulated in
     * the event. Also deals with the situation where two contexts have both
     * connected to each other; the context with the semantically greater
     * identity will severe/close the latest connection thus leaving only one
     * connection between the two contexts.
     */
    @Override
    public void handle(ConnectionAvailableEvent event) {
        boolean destroyConnection = false;
        final IConnection connection = event.getConnection();
        synchronized (getState()) {
            final String remoteContextIdentity = connection.getRemoteContextIdentity();
            if (getState().getConnectedContexts().getCount(remoteContextIdentity) > 0) {
                final int compareTo = getState().getContext().getIdentity().compareTo(connection.getRemoteContextIdentity());
                if (compareTo > 0) {
                    destroyConnection = true;
                } else {
                    if (compareTo == 0) {
                        if (getState().getContext().getContextHashCode() > connection.getRemoteContextHashCode()) {
                            destroyConnection = true;
                        }
                    }
                }
            }
            final IConnectionParameters connectionParameters = getState().getDiscoveredContexts().get(connection.getRemoteContextIdentity());
            if (connection.isOutbound() && connectionParameters != null && !connectionParameters.isEqual(connection)) {
                destroyConnection = true;
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Destroying connection " + safeToString(connection) + ", parameters have changed to " + safeToString(connectionParameters));
                }
            }
            if (!destroyConnection) {
                getState().getConnectedContexts().adjustCount(remoteContextIdentity, 1);
                getState().getChannelFactory().createChannel(connection, getState().getContext()).start();
            }
            getState().getCONNECTINGContexts().adjustCount(connection.getRemoteContextIdentity(), -1);
        }
        if (destroyConnection) {
            if (getLog().isTraceEnabled()) {
                getLog().trace("Destroying duplicate new connection " + safeToString(connection) + ", current connections are " + CollectionUtils.toFormattedString(getState().getChannels()));
            }
            connection.destroy();
        }
    }

    @Override
    public AsyncLog getLog() {
        return LOG;
    }
}

/**
 * Handler for {@link ConnectionDestroyedEvent} events. Destroys the
 * {@link IChannel} associated with the {@link IConnection}. If there are
 * pending subscriptions for the remote context then when the context is
 * re-discovered (when a {@link ContextDiscoveredEvent} is raised) the
 * subscriptions will be re-serviced.
 * 
 * @author Ramon Servadei
 * 
 */
class ConnectionDestroyedEventHandler extends DistributionEventHandler<ConnectionDestroyedEvent> {

    private static final AsyncLog LOG = new AsyncLog(ConnectionDestroyedEventHandler.class);

    ConnectionDestroyedEventHandler(IDistributionState state) {
        super(state);
    }

    /**
     * Destroys the {@link IChannel} associated with the {@link IConnection}. If
     * there are pending subscriptions for the remote context then when the
     * context is re-discovered (when a {@link ContextDiscoveredEvent} is
     * raised) the subscriptions will be re-serviced.
     */
    @Override
    public void handle(ConnectionDestroyedEvent event) {
        final IConnectionDiscoverer connectionDiscoverer = getState().getContext().getConnectionDiscoverer();
        if (connectionDiscoverer != null) {
            connectionDiscoverer.connectionDestroyed(event.getRemoteContextIdentity());
        }
        IChannel channel = null;
        final String remoteContextIdentity = event.getRemoteContextIdentity();
        synchronized (getState()) {
            final Map<String, IChannel> copy = CollectionFactory.newMap(getState().getChannels());
            channel = copy.remove(remoteContextIdentity);
            getState().setChannels(copy);
            getState().getConnectedContexts().adjustCount(remoteContextIdentity, -1);
        }
        if (channel != null) {
            if (getLog().isInfoEnabled()) {
                getLog().info("Destroying " + safeToString(channel));
            }
            channel.destroy();
        }
    }

    @Override
    public AsyncLog getLog() {
        return LOG;
    }
}

/**
 * Handler for {@link ChannelReadyEvent} events. Saves the ready
 * {@link IChannel} and issues any pending subscriptions for remote containers
 * for the remote context the channel connects to.
 * 
 * @author Ramon Servadei
 * 
 */
class ChannelReadyEventHandler extends DistributionEventHandler<ChannelReadyEvent> {

    private static final AsyncLog LOG = new AsyncLog(ChannelReadyEventHandler.class);

    ChannelReadyEventHandler(IDistributionState state) {
        super(state);
    }

    /**
     * Saves the ready {@link IChannel} and issues any pending subscriptions for
     * remote containers for the remote context the channel connects to.
     */
    @Override
    public void handle(ChannelReadyEvent event) {
        final IChannel channel = event.getChannel();
        final List<DualValue<ISubscriptionParameters, IEventListener>> list;
        synchronized (getState()) {
            final IConnectionParameters connectionParameters = getState().getDiscoveredContexts().get(channel.getRemoteContextIdentity());
            if (!validateChannelConnection(channel, connectionParameters)) {
                return;
            }
            final Map<String, IChannel> copy = CollectionFactory.newMap(getState().getChannels());
            copy.put(channel.getRemoteContextIdentity(), channel);
            getState().setChannels(copy);
            list = CollectionFactory.newList(getState().getRemoteSubscriptions().get(channel.getRemoteContextIdentity()));
        }
        if (list.size() > 0) {
            if (getLog().isInfoEnabled()) {
                getLog().info("Sending subscriptions to new channel " + safeToString(channel) + COLON + CollectionUtils.toFormattedString(list));
            }
            for (DualValue<ISubscriptionParameters, IEventListener> values : list) {
                try {
                    getState().getContext().queueEvent(new RemoteContainerSubscriptionEvent(getState().getContext(), channel.getRemoteContextIdentity(), values));
                } catch (Exception e) {
                    logException(getLog(), values, e);
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        final IChannel[] connectedChannels = getState().getContext().getConnectedChannels();
        for (IChannel channel2 : connectedChannels) {
            sb.append(SystemUtils.LINE_SEPARATOR).append(SPACING_4_CHARS).append(safeToString(channel2.toString()));
        }
        if (connectedChannels.length == 0) {
            sb.append(SystemUtils.LINE_SEPARATOR).append(SPACING_4_CHARS).append("<UNCONNECTED>");
        }
        if (getLog().isInfoEnabled()) {
            getLog().info(SystemUtils.LINE_SEPARATOR + getState().getContext() + " connected to:" + sb);
        }
    }

    @Override
    public AsyncLog getLog() {
        return LOG;
    }
}

/**
 * Encapsulates all necessary information for a remote container subscription
 * into an event to queue onto the context event framework. The event will be
 * intercepted by the {@link DistributionManager}. This ensures subscriptions
 * are handled on the same thread.
 * 
 * @author Ramon Servadei
 */
final class RemoteContainerSubscriptionEvent extends AbstractSystemEvent {

    /** The identity of the remote context for this subscription */
    final String remoteContextIdentity;

    /** The subscription parameters */
    final ISubscriptionParameters parameters;

    /** The listener the subscription should use */
    final IEventListener listener;

    /**
     * Standard constructor to encapsulate the parameters for a remote
     * subscription.
     * 
     * @param context
     *            the context for event operations
     * @param remoteContextIdentity
     *            the identity of the remote context for the subscription
     * @param values
     *            an {@link ISubscriptionParameters} and {@link IEventListener}
     *            instance, in that order
     */
    public RemoteContainerSubscriptionEvent(IEventManager context, String remoteContextIdentity, DualValue<ISubscriptionParameters, IEventListener> values) {
        super(context);
        this.remoteContextIdentity = remoteContextIdentity;
        this.parameters = values.getFirst();
        this.listener = values.getSecond();
    }

    /**
     * The identity of the remote context for this subscription
     * 
     * @return the remote context identity
     */
    public String getRemoteContextIdentity() {
        return remoteContextIdentity;
    }

    /**
     * Get the subscription parameters
     * 
     * @return the subscription parameters
     */
    public ISubscriptionParameters getParameters() {
        return parameters;
    }

    /**
     * Get the listener the subscription should use
     * 
     * @return the listener the subscription should use
     */
    public IEventListener getListener() {
        return listener;
    }

    protected String getAdditionalToString() {
        return "subcriptionParameters=" + getParameters();
    }
}

/**
 * Handler for {@link RemoteContainerSubscriptionEvent} events. Locates the
 * appropriate {@link IChannel} and issues the remote container subscription
 * encapsulated in the event.
 * 
 * @author Ramon Servadei
 */
final class RemoteContainerSubscriptionEventHandler extends DistributionEventHandler<RemoteContainerSubscriptionEvent> {

    private static final AsyncLog LOG = new AsyncLog(RemoteContainerSubscriptionEventHandler.class);

    RemoteContainerSubscriptionEventHandler(IDistributionState state) {
        super(state);
    }

    /**
     * Locates the appropriate {@link IChannel} and issues the remote container
     * subscription encapsulated in the event.
     */
    @Override
    public void handle(RemoteContainerSubscriptionEvent event) {
        final IChannel channel = getState().getChannels().get(event.getRemoteContextIdentity());
        if (channel != null) {
            channel.subscribe(event.getParameters());
            channel.addListener(event.getParameters(), event.getListener());
        }
    }

    @Override
    public AsyncLog getLog() {
        return LOG;
    }
}

/**
 * Handler for {@link EventSourceNotObservedEvent} events. Removes remote
 * {@link IContainer} instances from the context. This cleans up the process
 * space when a remote {@link IContainer} has no more listeners (it will no
 * longer be subscribed for). The {@link EventSourceNotObservedEvent} is
 * actually raised by the {@link IContainer} itself when it has no more
 * {@link IEventListener} instances attached to it.
 * 
 * @author Ramon Servadei
 */
final class RemoteEventSourceNotObservedEventHandler extends DistributionEventHandler<EventSourceNotObservedEvent> {

    private static final AsyncLog LOG = new AsyncLog(RemoteEventSourceNotObservedEventHandler.class);

    RemoteEventSourceNotObservedEventHandler(IDistributionState state) {
        super(state);
    }

    /**
     * Removes remote {@link IContainer} instances from the context.
     */
    @Override
    public void handle(EventSourceNotObservedEvent event) {
        final IFrameworkContext context = getState().getContext();
        if (context.containsRemoteContainer(event.getNativeContextIdentity(), event.getIdentity(), event.getType(), event.getDomain())) {
            final IContainer remoteContainer = context.getRemoteContainer(event.getNativeContextIdentity(), event.getIdentity(), event.getType(), event.getDomain());
            if (!remoteContainer.isLocal()) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Removing " + remoteContainer.toIdentityString());
                }
                context.removeContainer(remoteContainer);
            }
        }
    }

    @Override
    protected AsyncLog getLog() {
        return LOG;
    }
}
