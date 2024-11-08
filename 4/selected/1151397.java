package fulmine.context;

import static fulmine.util.Utils.COLON;
import static fulmine.util.Utils.logException;
import static fulmine.util.Utils.safeToString;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import fulmine.AbstractLifeCycle;
import fulmine.Domain;
import fulmine.Type;
import fulmine.distribution.channel.ChannelReadyEvent;
import fulmine.distribution.events.ConnectionDestroyedEvent;
import fulmine.event.EventFrameExecution;
import fulmine.event.IEvent;
import fulmine.event.listener.AbstractEventHandler;
import fulmine.event.listener.IEventListener;
import fulmine.event.listener.MultiEventListener;
import fulmine.event.listener.MultiSystemEventListener;
import fulmine.event.system.EventSourceNotObservedEvent;
import fulmine.event.system.EventSourceObservedEvent;
import fulmine.event.system.ISystemEventListener;
import fulmine.model.container.IContainer;
import fulmine.model.container.IContainer.DataState;
import fulmine.model.container.events.AbstractContainerFieldEvent;
import fulmine.model.container.events.ContainerFieldAddedEvent;
import fulmine.model.container.events.ContainerFieldRemovedEvent;
import fulmine.model.field.IField;
import fulmine.model.field.IntegerField;
import fulmine.model.field.StringField;
import fulmine.rpc.IRpcDefinition;
import fulmine.rpc.IRpcHandler;
import fulmine.rpc.IRpcManager;
import fulmine.rpc.IRpcMarker;
import fulmine.rpc.IRpcPublicationListener;
import fulmine.rpc.IRpcRegistry;
import fulmine.rpc.IRpcResult;
import fulmine.rpc.IRpcResultHandler;
import fulmine.rpc.RpcCodec;
import fulmine.rpc.RpcDefinition;
import fulmine.rpc.RpcMarker;
import fulmine.rpc.RpcRegistry;
import fulmine.rpc.RpcResult;
import fulmine.rpc.RpcUtils;
import fulmine.rpc.events.RpcInvokeEvent;
import fulmine.rpc.events.SendRpcEvent;
import fulmine.util.Utils;
import fulmine.util.collection.CollectionFactory;
import fulmine.util.collection.MapList;
import fulmine.util.collection.MapSet;
import fulmine.util.concurrent.ITaskExecutor;
import fulmine.util.concurrent.ITaskHandler;
import fulmine.util.concurrent.Task;
import fulmine.util.concurrent.TaskExecutor;
import fulmine.util.log.AsyncLog;
import fulmine.util.reference.DualValue;
import fulmine.util.reference.IReferenceCounter;
import fulmine.util.reference.QuadValue;
import fulmine.util.reference.ReferenceCounter;
import fulmine.util.reference.Value;

/**
 * The standard implementation of an {@link IRpcManager}.
 * <p>
 * RPC definitions can be invoked in a multi-threaded context. However,
 * simultaneous calls to the same RPC definition are handled sequentially.
 * <p>
 * Every RPC definition has an associated 'result record'. When a local context
 * handles an RPC invocation from a remote context, it also creates a result
 * record that will contain the result from the RPC invocation. The result
 * record has a specific naming convention, in ABNF form:
 * 
 * <pre>
 * result-record            = remote-context-identity &quot;:&quot; RPC-registry-key
 * 
 * remote-context-identity  = 1*(ALPHA / DIGIT)
 * RPC-registry-key         = &quot;RpcKey&quot; + 1*(DIGIT)
 * </pre>
 * 
 * When invoking an RPC, the local context first subscribes for the result
 * record then issues the RPC. On the receiving end, the invocation is packaged
 * up into an event. The remote context's RpcManager registers an
 * {@link IEventListener} that will respond to these {@link RpcInvokeEvent}s.
 * This listener will be responsible for locating the appropriate
 * {@link IRpcHandler} and invoking it with the arguments encapsulated in the
 * event. The result record attached to the remote context for the RPC
 * definition is then updated with the result and the invoking context will
 * receive this result and return it to the application caller.
 * <p>
 * The sequence diagram below helps to illustrate the operation.
 * 
 * <pre>
 * Application   IRpcManager    RpcResultHandler      RpcInvokeHandler   IRpcHandler   ResultRecord
 *  |                 |                |                     |                |             |
 *  |   invoke        |                |                     |                |             |
 *  |----------------&gt;|                |                     |                |             |
 *  |                 |            RpcInokeEvent             |                |             |
 *  |                 |-------------------------------------&gt;|                |             |
 *  |                 |            (remote call)             |                |             |
 *  |                 |                |                     |                |             |
 *  |                 |                |                     |    handle      |             |
 *  |                 |                |                     |---------------&gt;|             |
 *  |                 |                |                     |                |             |
 *  |                 |                |                     |    result      |             |
 *  |                 |                |                     |&lt;---------------|             |
 *  |                 |                |                     |                |             |
 *  |                 |                |                     |     update with result       |
 *  |                 |                |                     |-----------------------------&gt;|
 *  |                 |                |                     |                |             |
 *  |                 |                |   get result details (after remote transmission)   |
 *  |                 |                |---------------------------------------------------&gt;|
 *  |                 |                |                     |                |             |
 *  |              result              |                     |                |             |
 *  |&lt;---------------------------------|                     |                |             |
 *  |                 |                |                     |                |             |
 * </pre>
 * 
 * The timeout for RPC calls is defined by the system property
 * {@link IRpcManager#RPC_TIMEOUT}. If the timeout expires, an exception is
 * printed and a null value will be returned from
 * {@link #invoke(String, String, IField...)}.
 * <p>
 * Every context has a special component called the 'RPC registry'. This holds
 * every RPC that a local context exposes. Remote contexts must subscribe for
 * this via
 * {@link IFulmineContext#addRpcPublicationListener(String, IRpcPublicationListener)}
 * in order for the remote context to receive the RPC definitions of the
 * context. Calling {@link IFulmineContext#invoke(String, String, IField...)}
 * before the RPC registry is received is safe; when the RPC definition is
 * received from the target context, the RPC will be invoked.
 * 
 * @see RpcRegistry
 * @author Ramon Servadei
 */
public final class RpcManager extends AbstractLifeCycle implements IRpcManager, IRpcManagerOperations {

    private static final AsyncLog LOG = new AsyncLog(RpcManager.class);

    /**
     * Defines the timeout in use - specified by the setting of
     * {@link IRpcManager#RPC_TIMEOUT}
     */
    private final int TIMEOUT = Integer.parseInt(System.getProperty(IRpcManager.RPC_TIMEOUT, IRpcManager.DEFAULT_RPC_TIMEOUT));

    /**
     * Internal class to handle results for the synchronous version of
     * {@link RpcManager#invoke(String, String, IField...)}. All RPC invocations
     * are asynchronous, this result handler notifies all threads waiting on
     * {@link #resultValue} when the result comes in for the correct RPC marker.
     * 
     * @author Ramon Servadei
     */
    private final class ResultHandler implements IRpcResultHandler {

        private final Value<IRpcMarker> markerValue;

        private final Value<IRpcResult> resultValue;

        private ResultHandler(Value<IRpcMarker> markerValue, Value<IRpcResult> resultValue) {
            this.markerValue = markerValue;
            this.resultValue = resultValue;
        }

        public void resultReceived(IRpcResult result, IRpcMarker marker) {
            if (marker.equals(this.markerValue.get())) {
                this.resultValue.set(result);
                synchronized (this.resultValue) {
                    this.resultValue.notifyAll();
                }
            }
        }
    }

    /** Shared state for the manager */
    private final IRpcManagerState state;

    /**
     * A multi-event listener that handles signalling RPC published/unpublished
     * events when remote RPC registry instances have RPC definitions
     * added/removed.
     */
    private final MultiEventListener rpcPublicationListener;

    /**
     * Construct the RPC manager.
     * 
     * @param context
     *            the local context for the RPC manager
     */
    RpcManager(IFrameworkContext context) {
        this(new RpcManagerState(context));
    }

    /**
     * Internally chained constructor
     * 
     * @param state
     *            the state for the RPC manager
     */
    @SuppressWarnings("unchecked")
    RpcManager(IRpcManagerState state) {
        super();
        this.state = state;
        this.rpcPublicationListener = new MultiEventListener(RpcManager.class.getSimpleName(), getState().getContext(), AbstractEventHandler.getEventHandlerMappings(new RpcPublishedHandler(getState(), this), new RpcUnpublishedHandler(getState(), this)));
    }

    public IRpcResult invoke(String remoteContextIdentity, String procedure, IField... args) {
        final Value<IRpcResult> resultValue = new Value<IRpcResult>();
        final Value<IRpcMarker> markerValue = new Value<IRpcMarker>();
        synchronized (resultValue) {
            final IRpcMarker marker = invoke(new ResultHandler(markerValue, resultValue), remoteContextIdentity, procedure, args);
            markerValue.set(marker);
            if (resultValue.get() == null) {
                try {
                    resultValue.wait(TIMEOUT);
                } catch (InterruptedException e) {
                    logException(getLog(), "RPC name=" + procedure + ", args=" + Arrays.deepToString(args) + ", context=" + remoteContextIdentity, e);
                }
            }
            if (resultValue.get() == null) {
                Utils.logException(getLog(), "TIMEOUT for RPC name=" + procedure + ", args=" + Arrays.deepToString(args) + ", context=" + remoteContextIdentity, new Exception());
            }
        }
        return resultValue.get();
    }

    public IRpcMarker invoke(IRpcResultHandler resultHandler, String remoteContextIdentity, String procedure, IField... args) {
        IRpcMarker rpcMarker = new RpcMarker(getState().getMarkerCounter().getAndIncrement());
        DualValue<String, IRpcDefinition> keyAndDefinition = getRegistryKeyAndDefinition(remoteContextIdentity, procedure, args);
        if (keyAndDefinition == null) {
            final Set<String> connectedContexts = getState().getConnectedContexts();
            synchronized (connectedContexts) {
                boolean contextExists = false;
                if (connectedContexts.contains(remoteContextIdentity)) {
                    contextExists = true;
                }
                getState().getPendingRpcInvocations().get(remoteContextIdentity).add(new QuadValue<String, IField[], IRpcResultHandler, IRpcMarker>(procedure, args, resultHandler, rpcMarker));
                if (getLog().isDebugEnabled()) {
                    getLog().debug((contextExists ? "RPC for " + remoteContextIdentity : "Remote context " + remoteContextIdentity) + " is not yet available so placing RPC {name=" + safeToString(procedure) + ", args=" + Arrays.deepToString(args) + "} onto the pending RPC queue.");
                }
            }
            return rpcMarker;
        }
        invoke(remoteContextIdentity, keyAndDefinition.getFirst(), keyAndDefinition.getSecond(), args, resultHandler, rpcMarker);
        return rpcMarker;
    }

    @SuppressWarnings("boxing")
    public void invoke(String remoteContextIdentity, String rpcKey, IRpcDefinition definition, IField[] args, IRpcResultHandler resultHandler, IRpcMarker marker) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Invoking " + safeToString(definition) + " with args=" + Arrays.deepToString(args) + ", " + safeToString(resultHandler) + ", " + safeToString(marker));
        }
        final String resultRecordName = getState().getResultRecordName(getState().getContext().getIdentity(), rpcKey);
        final int count;
        final IReferenceCounter<DualValue<String, String>> counter = getState().getResultRecordSubscriptionCounter();
        synchronized (counter) {
            count = counter.adjustCount(new DualValue<String, String>(remoteContextIdentity, resultRecordName), 1);
        }
        if (count == 1) {
            getState().getContext().subscribe(remoteContextIdentity, resultRecordName, Type.SYSTEM, Domain.FRAMEWORK, getState().getEventHandler());
        }
        byte[] data = new RpcCodec(getState().getRegistry()).encode(marker, rpcKey, getState().getContext().getIdentity(), args);
        final Map<Integer, DualValue<IRpcResultHandler, IRpcDefinition>> resultHandlers = getState().getResultHandlers();
        synchronized (resultHandlers) {
            resultHandlers.put(marker.getId(), new DualValue<IRpcResultHandler, IRpcDefinition>(resultHandler, definition));
        }
        getState().getContext().queueEvent(new SendRpcEvent(getState().getContext(), remoteContextIdentity, data));
    }

    public boolean publishProdedure(IRpcHandler handler, IRpcDefinition rpcDefinition) {
        return getState().getRegistry().publishProdedure(handler, rpcDefinition);
    }

    public boolean unpublishProdedure(IRpcDefinition rpcDefinition) {
        return getState().getRegistry().unpublishProdedure(rpcDefinition);
    }

    public boolean addRpcPublicationListener(String remoteContextIdentity, IRpcPublicationListener listener) {
        boolean subscribe = false;
        final boolean added;
        final MapSet<String, IRpcPublicationListener> rpcPublicationListeners = getState().getRpcPublicationListeners();
        synchronized (rpcPublicationListeners) {
            final Set<IRpcPublicationListener> listenersForContext = rpcPublicationListeners.get(remoteContextIdentity);
            added = listenersForContext.add(listener);
            if (added) {
                rpcPublicationListeners.put(remoteContextIdentity, CollectionFactory.newSet(listenersForContext));
                subscribe = listenersForContext.size() == 1;
            }
        }
        if (subscribe) {
            getState().getContext().subscribe(remoteContextIdentity, IRpcRegistry.RPC_REGISTRY, Type.SYSTEM, Domain.FRAMEWORK, getRpcPublicationListener());
        }
        final IContainer remoteContainer = getState().getContext().getRemoteContainer(remoteContextIdentity, IRpcRegistry.RPC_REGISTRY, Type.SYSTEM, Domain.FRAMEWORK);
        final String[] componentIdentities = remoteContainer.getComponentIdentities();
        for (String identity : componentIdentities) {
            if (identity.indexOf(IRpcRegistry.RPC_KEY) > -1) {
                final IField field = remoteContainer.get(identity);
                if (field instanceof StringField) {
                    final String definitionAsString = ((StringField) field).get();
                    IRpcDefinition rpcDefinition = new RpcDefinition(definitionAsString);
                    listener.procedureAvailable(remoteContextIdentity, rpcDefinition);
                }
            }
        }
        return added;
    }

    public boolean removeRpcPublicationListener(String remoteContextIdentity, IRpcPublicationListener listener) {
        boolean unsubscribe;
        boolean removed;
        final MapSet<String, IRpcPublicationListener> rpcPublicationListeners = getState().getRpcPublicationListeners();
        synchronized (rpcPublicationListeners) {
            unsubscribe = false;
            final Set<IRpcPublicationListener> listenersForContext = rpcPublicationListeners.get(remoteContextIdentity);
            removed = listenersForContext.remove(listener);
            if (removed) {
                rpcPublicationListeners.put(remoteContextIdentity, CollectionFactory.newSet(listenersForContext));
            }
            unsubscribe = listenersForContext.size() == 0;
        }
        if (unsubscribe) {
            getState().getContext().unsubscribe(remoteContextIdentity, IRpcRegistry.RPC_REGISTRY, Type.SYSTEM, Domain.FRAMEWORK, getRpcPublicationListener());
        }
        return removed;
    }

    @Override
    protected void doDestroy() {
        getState().destroy();
    }

    @Override
    protected void doStart() {
        getState().init(this);
        getState().start();
    }

    @Override
    protected AsyncLog getLog() {
        return LOG;
    }

    /**
     * Get the RPC registry key and {@link IRpcDefinition} for the procedure.
     * This checks the RPC registry record of the remote context to find the key
     * for an RPC with matching name and arguments.
     * 
     * @param remoteContextIdentity
     *            the remote context
     * @param procedure
     *            the RPC name
     * @param args
     *            the RPC arguments
     * @return the RPC registry key and {@link IRpcDefinition} for the RPC in
     *         the remote context or <code>null</code> if not found
     */
    public DualValue<String, IRpcDefinition> getRegistryKeyAndDefinition(String remoteContextIdentity, String procedure, IField[] args) {
        if (!getState().getContext().containsRemoteContainer(remoteContextIdentity, IRpcRegistry.RPC_REGISTRY, Type.SYSTEM, Domain.FRAMEWORK)) {
            return null;
        }
        String rpcSignature = RpcUtils.getSignature(procedure, args);
        final IContainer registry = getState().getContext().getRemoteContainer(remoteContextIdentity, IRpcRegistry.RPC_REGISTRY, Type.SYSTEM, Domain.FRAMEWORK);
        final String[] componentIdentities = registry.getComponentIdentities();
        for (String fieldId : componentIdentities) {
            if (fieldId.startsWith(IRpcRegistry.RPC_KEY)) {
                final IField field = registry.get(fieldId);
                if (field instanceof StringField) {
                    final String definitionAsString = ((StringField) field).get();
                    if (definitionAsString.contains(rpcSignature)) {
                        return new DualValue<String, IRpcDefinition>(fieldId, new RpcDefinition(definitionAsString));
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the shared state for the manager
     * 
     * @return the shared state
     */
    private IRpcManagerState getState() {
        return this.state;
    }

    private MultiEventListener getRpcPublicationListener() {
        return this.rpcPublicationListener;
    }

    public boolean unpublishRpcs(Class<?> definition, Object handler) {
        return getState().getRegistry().unpublishRpcs(definition, handler);
    }

    public boolean publishRpcs(Class<?> definition, Object handler) {
        return getState().getRegistry().publishRpcs(definition, handler);
    }
}

/**
 * State for the {@link RpcManager}
 * 
 * @author Ramon Servadei
 */
final class RpcManagerState extends AbstractLifeCycle implements IRpcManagerState {

    private final IReferenceCounter<DualValue<String, String>> resultRecordSubscriptionCounter;

    /** The task executor */
    private final ITaskExecutor executor;

    /** The local context */
    private final IFrameworkContext context;

    /**
     * Sub-component that manages {@link RpcDefinition} instances registered by
     * local application code. These are the RPCs for the local context.
     */
    private final IRpcRegistry registry;

    /**
     * Holds the {@link IRpcResultHandler} to invoke with the {@link IRpcResult}
     * for the matching RPC marker ID. The {@link IRpcDefinition} is also held
     * with the result handler.
     */
    private final Map<Integer, DualValue<IRpcResultHandler, IRpcDefinition>> resultHandlers;

    /** Used to generate the marker ID for each successive RPC invocation */
    private final AtomicInteger markerCounter;

    /** Holds RPC invocations for contexts not yet available */
    private final MapList<String, QuadValue<String, IField[], IRpcResultHandler, IRpcMarker>> pendingRpcInvocations;

    /** The collection of result records keyed by remote context identity */
    private final MapSet<String, IContainer> resultRecords;

    /** The currently connected contexts */
    private final Set<String> connectedContexts;

    /** Holds the event handlers used for the RpcManager */
    private MultiSystemEventListener eventHandler;

    /** The RPC publication listeners per remote context identity */
    private final MapSet<String, IRpcPublicationListener> rpcPublicationListeners;

    /** The known collection of observed result records */
    private final Set<String> observedResultRecords;

    /**
     * Construct the state
     * 
     * @param context
     *            the context
     */
    public RpcManagerState(IFrameworkContext context) {
        super();
        this.context = context;
        this.markerCounter = new AtomicInteger(0);
        this.pendingRpcInvocations = new MapList<String, QuadValue<String, IField[], IRpcResultHandler, IRpcMarker>>();
        this.resultHandlers = CollectionFactory.newMap();
        this.registry = new RpcRegistry(context);
        this.resultRecords = new MapSet<String, IContainer>(1);
        this.connectedContexts = CollectionFactory.newSet();
        this.rpcPublicationListeners = new MapSet<String, IRpcPublicationListener>();
        this.resultRecordSubscriptionCounter = new ReferenceCounter<DualValue<String, String>>(1);
        this.observedResultRecords = CollectionFactory.newSet();
        final String identity = context.getIdentity() + COLON + "RpcProcessor";
        this.executor = new TaskExecutor(identity, context);
    }

    @SuppressWarnings("unchecked")
    public void init(IRpcManagerOperations operations) {
        this.eventHandler = new MultiSystemEventListener(RpcManager.class.getSimpleName() + "EventHandler", getContext(), AbstractEventHandler.getEventHandlerMappings(new RpcConnectionDestroyedEventHandler(this), new RpcChannelReadyEventHandler(this, operations), new RpcResultHandler(this), new ResultRecordObservedHandler(this), new SendRpcEventHandler(this), new ResultRecordNotObservedHandler(this), new RpcInvokeHandler(this)));
    }

    public IFrameworkContext getContext() {
        return this.context;
    }

    public IRpcRegistry getRegistry() {
        return this.registry;
    }

    public Map<Integer, DualValue<IRpcResultHandler, IRpcDefinition>> getResultHandlers() {
        return this.resultHandlers;
    }

    public AtomicInteger getMarkerCounter() {
        return this.markerCounter;
    }

    public MapList<String, QuadValue<String, IField[], IRpcResultHandler, IRpcMarker>> getPendingRpcInvocations() {
        return this.pendingRpcInvocations;
    }

    public MapSet<String, IContainer> getResultRecords() {
        return this.resultRecords;
    }

    public Set<String> getConnectedContexts() {
        return this.connectedContexts;
    }

    public String getResultRecordName(String remoteContextIdentity, String rpcKey) {
        return remoteContextIdentity + COLON + rpcKey;
    }

    public MultiSystemEventListener getEventHandler() {
        return this.eventHandler;
    }

    public MapSet<String, IRpcPublicationListener> getRpcPublicationListeners() {
        return this.rpcPublicationListeners;
    }

    @Override
    protected void doDestroy() {
        getEventHandler().destroy();
        getRegistry().destroy();
        getTaskExecutor().destroy();
    }

    @Override
    protected void doStart() {
        getTaskExecutor().start();
        getEventHandler().start();
        getRegistry().start();
    }

    public IReferenceCounter<DualValue<String, String>> getResultRecordSubscriptionCounter() {
        return this.resultRecordSubscriptionCounter;
    }

    public ITaskExecutor getTaskExecutor() {
        return this.executor;
    }

    public Set<String> getObservedResultRecords() {
        return this.observedResultRecords;
    }
}

/**
 * Base class for RPC event handlers used by the {@link RpcManager}
 * 
 * @author Ramon Servadei
 * @param <EVENT>
 *            the event type the handler works with
 */
abstract class RpcEventHandler<EVENT extends IEvent> extends AbstractEventHandler<EVENT> implements ISystemEventListener {

    /** The state */
    private final IRpcManagerState state;

    /**
     * Standard constructor
     * 
     * @param state
     *            the {@link RpcManager} state
     */
    RpcEventHandler(IRpcManagerState state) {
        super();
        this.state = state;
    }

    IRpcManagerState getState() {
        return state;
    }
}

/**
 * Handler for {@link RpcInvokeEvent}s. This locates the appropriate
 * {@link IRpcHandler} and calls it with the arguments encapsulated in the
 * invoke event. The result from the handler is used to update the result
 * record. The update triggers the record to be transmitted to the remote
 * context that issued the RPC and the remote context will thus get the result
 * from the RPC invocation.
 * 
 * @author Ramon Servadei
 */
final class RpcInvokeHandler extends RpcEventHandler<RpcInvokeEvent> implements ITaskHandler<RpcInvokeEvent> {

    private static final AsyncLog LOG = new AsyncLog(RpcInvokeHandler.class);

    RpcInvokeHandler(IRpcManagerState state) {
        super(state);
    }

    /**
     * Locates the {@link IRpcHandler} for the {@link RpcDefinition} identified
     * by the RPC registry key in the event. The handler is executed and the
     * result record attached to the remote context for the RPC definition is
     * updated with the {@link IRpcResult} from the handler.
     */
    @Override
    public void handle(RpcInvokeEvent event) {
        getState().getTaskExecutor().execute(new Task<RpcInvokeEvent>(this, event));
    }

    public void handleTask(RpcInvokeEvent event) {
        event.decode(getState().getRegistry());
        final String resultRecordName = getState().getResultRecordName(event.getRemoteContextIdentity(), event.getRpcKey());
        final IContainer resultRecord = getState().getContext().getLocalContainer(resultRecordName, Type.SYSTEM, Domain.FRAMEWORK);
        synchronized (getState().getObservedResultRecords()) {
            if (!getState().getObservedResultRecords().contains(resultRecordName)) {
                if (getLog().isInfoEnabled()) {
                    getLog().info("Waiting for result record " + resultRecordName + " to be observed");
                }
                try {
                    getState().getObservedResultRecords().wait();
                } catch (InterruptedException e) {
                    logException(getLog(), "Waiting for " + resultRecordName, e);
                }
            }
        }
        IRpcResult result = null;
        try {
            String rpcKey = event.getRpcKey();
            final IRpcHandler handler = getState().getRegistry().getHandler(rpcKey);
            if (handler == null) {
                final String message = "No hander for " + safeToString(event);
                if (getLog().isDebugEnabled()) {
                    getLog().debug(message);
                }
                result = new RpcResult(false, null, message);
            } else {
                IRpcDefinition definition = getState().getRegistry().getDefinition(rpcKey);
                if (definition == null) {
                    final String message = "No definition for " + safeToString(event);
                    if (getLog().isDebugEnabled()) {
                        getLog().debug(message);
                    }
                    result = new RpcResult(false, null, message);
                } else {
                    try {
                        if (getLog().isDebugEnabled()) {
                            getLog().debug("Invoking " + safeToString(definition) + " with arguments " + Arrays.deepToString(event.getArguments()));
                        }
                        result = handler.handle(definition, event.getArguments());
                    } catch (Exception e) {
                        logException(getLog(), "Handling RPC invoke for " + safeToString(definition) + " with " + Arrays.deepToString(event.getArguments()) + " from " + event.getRemoteContextIdentity(), e);
                        result = new RpcResult(false, null, e.toString());
                    }
                }
            }
        } catch (Exception e) {
            logException(getLog(), "Could not handle " + safeToString(event), e);
            result = new RpcResult(false, null, e.toString());
        }
        getState().getResultRecords().get(event.getRemoteContextIdentity()).add(resultRecord);
        resultRecord.beginFrame(new EventFrameExecution());
        try {
            result.updateResultRecord(event.getMarker(), resultRecord);
        } finally {
            resultRecord.endFrame();
        }
    }

    @Override
    protected AsyncLog getLog() {
        return LOG;
    }
}

/**
 * Handler for result records (represented as {@link IContainer} instances).
 * When an update event occurs for a result record, the marker ID for the RPC
 * invocation is extracted from the result record and this is used to locate the
 * {@link IRpcResultHandler}. An {@link IRpcResult} is then constructed from the
 * result record and is passed to the result handler.
 * 
 * @author Ramon Servadei
 */
final class RpcResultHandler extends RpcEventHandler<IContainer> implements ITaskHandler<IContainer> {

    private static final AsyncLog LOG = new AsyncLog(RpcResultHandler.class);

    RpcResultHandler(IRpcManagerState state) {
        super(state);
    }

    /**
     * Finds the {@link IRpcResultHandler} from {@link #getResultHandlers()} and
     * notifies it with the result contained within the result record (the
     * event).
     */
    @Override
    public void handle(IContainer event) {
        getState().getTaskExecutor().execute(new Task<IContainer>(this, event));
    }

    @Override
    protected AsyncLog getLog() {
        return LOG;
    }

    @SuppressWarnings("boxing")
    public void handleTask(IContainer event) {
        if (event.getDataState() != DataState.LIVE) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Waiting for result record to become active " + event);
            }
            return;
        }
        final IntegerField markerIdField = event.getIntegerField(IRpcResult.MARKER);
        if (markerIdField == null) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Waiting for result record to be populated " + event);
            }
            return;
        }
        final int markerId = markerIdField.get();
        final DualValue<IRpcResultHandler, IRpcDefinition> result;
        final Map<Integer, DualValue<IRpcResultHandler, IRpcDefinition>> resultHandlers = getState().getResultHandlers();
        synchronized (resultHandlers) {
            result = resultHandlers.remove(markerId);
        }
        if (result == null) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("No result handler found for " + event);
            }
            return;
        }
        IRpcMarker marker = new RpcMarker(markerId);
        if (result.getSecond() == null) {
            throw new IllegalStateException("No definition found for " + safeToString(marker) + ", " + safeToString(result));
        }
        result.getFirst().resultReceived(new RpcResult(result.getSecond(), event), marker);
        final int count;
        final IReferenceCounter<DualValue<String, String>> counter = getState().getResultRecordSubscriptionCounter();
        synchronized (counter) {
            count = counter.adjustCount(new DualValue<String, String>(event.getNativeContextIdentity(), event.getIdentity()), -1);
        }
        if (count == 0) {
            getState().getContext().unsubscribe(event.getNativeContextIdentity(), event.getIdentity(), Type.SYSTEM, Domain.FRAMEWORK, getState().getEventHandler());
        }
    }
}

/**
 * Handles {@link ConnectionDestroyedEvent}s. This will destroy any result
 * records associated with the remote context the connection was for. The reason
 * for this handler is to prevent memory leaks occurring from stale result
 * records not being purged when remote contexts are destroyed.
 * 
 * @author Ramon Servadei
 */
final class RpcConnectionDestroyedEventHandler extends RpcEventHandler<ConnectionDestroyedEvent> {

    private static final AsyncLog LOG = new AsyncLog(RpcConnectionDestroyedEventHandler.class);

    RpcConnectionDestroyedEventHandler(IRpcManagerState state) {
        super(state);
    }

    /**
     * Find all result records associated with the remote context the connection
     * was for and destroy them.
     */
    @Override
    public void handle(ConnectionDestroyedEvent event) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Destroying result records attached to remote context " + event.getRemoteContextIdentity());
        }
        final Set<IContainer> resultRecords;
        final Set<String> connectedContexts = getState().getConnectedContexts();
        synchronized (connectedContexts) {
            resultRecords = getState().getResultRecords().remove(event.getRemoteContextIdentity());
            connectedContexts.remove(event.getRemoteContextIdentity());
        }
        if (resultRecords != null) {
            for (IContainer container : resultRecords) {
                container.destroy();
            }
        }
    }

    @Override
    protected AsyncLog getLog() {
        return LOG;
    }
}

/**
 * Handler for {@link ChannelReadyEvent}s. This keeps track of available
 * contexts and triggers any pending RPC invocations for available contexts.
 * 
 * @author Ramon Servadei
 */
final class RpcChannelReadyEventHandler extends RpcEventHandler<ChannelReadyEvent> {

    private static final AsyncLog LOG = new AsyncLog(RpcChannelReadyEventHandler.class);

    final IRpcManagerOperations operations;

    RpcChannelReadyEventHandler(IRpcManagerState state, IRpcManagerOperations operations) {
        super(state);
        this.operations = operations;
    }

    /**
     * This keeps track of available contexts and triggers any pending RPC
     * invocations for available contexts.
     */
    @Override
    public void handle(ChannelReadyEvent event) {
        final String remoteContextIdentity = event.getChannel().getRemoteContextIdentity();
        final List<QuadValue<String, IField[], IRpcResultHandler, IRpcMarker>> pendingRpcs;
        final Set<String> connectedContexts = getState().getConnectedContexts();
        synchronized (connectedContexts) {
            connectedContexts.add(remoteContextIdentity);
            pendingRpcs = getState().getPendingRpcInvocations().remove(remoteContextIdentity);
        }
        if (pendingRpcs == null) {
            return;
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Invoking " + pendingRpcs.size() + " RPCs for " + remoteContextIdentity);
        }
        for (QuadValue<String, IField[], IRpcResultHandler, IRpcMarker> rpc : pendingRpcs) {
            final DualValue<String, IRpcDefinition> keyAndDefinition = getOperations().getRegistryKeyAndDefinition(remoteContextIdentity, rpc.getFirst(), rpc.getSecond());
            getOperations().invoke(remoteContextIdentity, keyAndDefinition.getFirst(), keyAndDefinition.getSecond(), rpc.getSecond(), rpc.getThird(), rpc.getFourth());
        }
    }

    @Override
    protected AsyncLog getLog() {
        return LOG;
    }

    private IRpcManagerOperations getOperations() {
        return this.operations;
    }
}

/**
 * Base class for handling {@link ContainerFieldAddedEvent} and
 * {@link ContainerFieldRemovedEvent}s. This holds the standard logic required
 * for each sub-class.
 * 
 * @author Ramon Servadei
 */
abstract class AbstractRpcPublicationHandler<T extends AbstractContainerFieldEvent> extends RpcEventHandler<T> {

    private final IRpcManagerOperations operations;

    AbstractRpcPublicationHandler(IRpcManagerState state, IRpcManagerOperations operations) {
        super(state);
        this.operations = operations;
    }

    @Override
    public void handle(T event) {
        final IField field = event.getField();
        final String identity = field.getIdentity();
        if (identity.indexOf(IRpcRegistry.RPC_KEY) > -1) {
            if (!(field instanceof StringField)) {
                return;
            }
            IContainer container = (IContainer) event.getSource();
            final Set<IRpcPublicationListener> listeners;
            final MapSet<String, IRpcPublicationListener> rpcPublicationListeners = getState().getRpcPublicationListeners();
            synchronized (rpcPublicationListeners) {
                listeners = rpcPublicationListeners.get(container.getNativeContextIdentity());
            }
            final String definitionAsString = ((StringField) field).get();
            IRpcDefinition rpcDefinition = new RpcDefinition(definitionAsString);
            for (IRpcPublicationListener listener : listeners) {
                doAction(container, rpcDefinition, listener);
            }
        }
    }

    IRpcManagerOperations getOperations() {
        return this.operations;
    }

    /**
     * Perform the action for a change in an {@link IRpcDefinition}
     * 
     * @param container
     *            the remote RPC registry
     * @param rpcDefinition
     *            the RPC definition
     * @param listener
     *            the {@link IRpcPublicationListener} to notify
     */
    abstract void doAction(IContainer container, IRpcDefinition rpcDefinition, IRpcPublicationListener listener);
}

/**
 * Handler for {@link ContainerFieldAddedEvent}s. Checks if the field is an RPC
 * definition and if it is this will notify all {@link IRpcPublicationListener}
 * instances with the published RPC.
 * 
 * @author Ramon Servadei
 */
final class RpcPublishedHandler extends AbstractRpcPublicationHandler<ContainerFieldAddedEvent> {

    private static final AsyncLog LOG = new AsyncLog(RpcPublishedHandler.class);

    RpcPublishedHandler(IRpcManagerState state, IRpcManagerOperations operations) {
        super(state, operations);
    }

    @Override
    protected AsyncLog getLog() {
        return LOG;
    }

    @Override
    void doAction(IContainer container, IRpcDefinition rpcDefinition, IRpcPublicationListener listener) {
        final String remoteContextIdentity = container.getNativeContextIdentity();
        listener.procedureAvailable(remoteContextIdentity, rpcDefinition);
        final List<QuadValue<String, IField[], IRpcResultHandler, IRpcMarker>> pendingRpcs;
        final Set<String> connectedContexts = getState().getConnectedContexts();
        final List<QuadValue<String, IField[], IRpcResultHandler, IRpcMarker>> toInvoke = CollectionFactory.newList();
        synchronized (connectedContexts) {
            pendingRpcs = getState().getPendingRpcInvocations().get(remoteContextIdentity);
            if (pendingRpcs == null) {
                return;
            }
            for (Iterator<QuadValue<String, IField[], IRpcResultHandler, IRpcMarker>> iterator = pendingRpcs.iterator(); iterator.hasNext(); ) {
                QuadValue<String, IField[], IRpcResultHandler, IRpcMarker> rpc = iterator.next();
                if (rpcDefinition.getName().equals(rpc.getFirst()) && Arrays.equals(rpcDefinition.getArgumentTypes(), RpcUtils.getArgumentTypes(rpc.getSecond()))) {
                    iterator.remove();
                    toInvoke.add(rpc);
                }
            }
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Invoking " + toInvoke.size() + " awaiting calls for newly available RPC " + rpcDefinition + " from " + remoteContextIdentity);
        }
        for (QuadValue<String, IField[], IRpcResultHandler, IRpcMarker> rpc : toInvoke) {
            final DualValue<String, IRpcDefinition> keyAndDefinition = getOperations().getRegistryKeyAndDefinition(remoteContextIdentity, rpc.getFirst(), rpc.getSecond());
            getOperations().invoke(remoteContextIdentity, keyAndDefinition.getFirst(), keyAndDefinition.getSecond(), rpc.getSecond(), rpc.getThird(), rpc.getFourth());
        }
    }
}

/**
 * Handler for {@link ContainerFieldRemovedEvent}s. Checks if the field is an
 * RPC definition and if it is this will notify all
 * {@link IRpcPublicationListener} instances with the unpublished RPC.
 * 
 * @author Ramon Servadei
 */
final class RpcUnpublishedHandler extends AbstractRpcPublicationHandler<ContainerFieldRemovedEvent> {

    private static final AsyncLog LOG = new AsyncLog(RpcPublishedHandler.class);

    RpcUnpublishedHandler(IRpcManagerState state, IRpcManagerOperations operations) {
        super(state, operations);
    }

    @Override
    protected AsyncLog getLog() {
        return LOG;
    }

    void doAction(IContainer container, IRpcDefinition rpcDefinition, IRpcPublicationListener listener) {
        listener.procedureUnavailable(container.getNativeContextIdentity(), rpcDefinition);
    }
}

/**
 * Handles the {@link SendRpcEvent}s
 * 
 * @author Ramon Servadei
 */
final class SendRpcEventHandler extends RpcEventHandler<SendRpcEvent> {

    private static final AsyncLog LOG = new AsyncLog(SendRpcEventHandler.class);

    SendRpcEventHandler(IRpcManagerState state) {
        super(state);
    }

    @Override
    protected AsyncLog getLog() {
        return LOG;
    }

    /**
     * Invoke the RPC by calling
     * {@link IFulmineContext#invokeRpc(String, byte[])}
     */
    @Override
    public void handle(SendRpcEvent event) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("invoking " + event);
        }
        getState().getContext().invokeRpc(event.getRemoteContextIdentity(), event.getRpcData());
    }
}

/**
 * If a result record is observed, adds the record name to a list of known
 * active result records.
 * 
 * @see RpcInvokeHandler#handleTask(RpcInvokeEvent)
 * @author Ramon Servadei
 */
final class ResultRecordObservedHandler extends RpcEventHandler<EventSourceObservedEvent> {

    private static final AsyncLog LOG = new AsyncLog(ResultRecordObservedHandler.class);

    ResultRecordObservedHandler(IRpcManagerState state) {
        super(state);
    }

    @Override
    protected AsyncLog getLog() {
        return LOG;
    }

    @Override
    public void handle(final EventSourceObservedEvent event) {
        if (event.getIdentity().contains(IRpcRegistry.RPC_KEY)) {
            synchronized (getState().getObservedResultRecords()) {
                getState().getObservedResultRecords().add(event.getIdentity());
                getState().getObservedResultRecords().notify();
            }
        }
    }
}

/**
 * Complimentary to {@link ResultRecordObservedHandler}. This removes the result
 * record from the collection of observed result records.
 * 
 * @author Ramon Servadei
 */
final class ResultRecordNotObservedHandler extends RpcEventHandler<EventSourceNotObservedEvent> {

    private static final AsyncLog LOG = new AsyncLog(ResultRecordNotObservedHandler.class);

    ResultRecordNotObservedHandler(IRpcManagerState state) {
        super(state);
    }

    @Override
    protected AsyncLog getLog() {
        return LOG;
    }

    @Override
    public void handle(EventSourceNotObservedEvent event) {
        if (event.getSource().getIdentity().contains(IRpcRegistry.RPC_KEY)) {
            synchronized (getState().getObservedResultRecords()) {
                getState().getObservedResultRecords().remove(event.getSource().getIdentity());
            }
        }
    }
}
