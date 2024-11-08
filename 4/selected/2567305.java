package fulmine.context;

import static fulmine.util.Utils.EMPTY_STRING;
import static org.junit.Assert.assertEquals;
import java.util.Map;
import java.util.Set;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import fulmine.Domain;
import fulmine.IDomain;
import fulmine.IType;
import fulmine.Type;
import fulmine.context.DistributionState.ValuesBuilder;
import fulmine.distribution.IDistributionState;
import fulmine.distribution.channel.ChannelReadyEvent;
import fulmine.distribution.channel.IChannel;
import fulmine.distribution.channel.IChannelFactory;
import fulmine.distribution.connection.IConnection;
import fulmine.distribution.connection.IConnectionBroker;
import fulmine.distribution.connection.IConnectionDiscoverer;
import fulmine.distribution.connection.IConnectionParameters;
import fulmine.distribution.events.ConnectionAvailableEvent;
import fulmine.distribution.events.ConnectionDestroyedEvent;
import fulmine.distribution.events.ContextDiscoveredEvent;
import fulmine.event.listener.IEventListener;
import fulmine.event.listener.ILifeCycleEventListener;
import fulmine.event.subscription.ISubscriptionManager;
import fulmine.event.subscription.ISubscriptionParameters;
import fulmine.event.subscription.SubscriptionParameters;
import fulmine.event.system.ISystemEventSource;
import fulmine.protocol.specification.IFrameReader;
import fulmine.protocol.specification.IFrameWriter;
import fulmine.util.collection.CollectionFactory;
import fulmine.util.reference.AutoCreatingStore;
import fulmine.util.reference.DualValue;
import fulmine.util.reference.IAutoCreatingStore;
import fulmine.util.reference.IReferenceCounter;
import fulmine.util.reference.ReferenceCounter;

/**
 * Test cases for the {@link ChannelReadyEventHandler},
 * {@link ConnectionAvailableEventHandler},
 * {@link ConnectionDestroyedEventHandler} and
 * {@link ContextDiscoveredEventHandler}
 * 
 * @author Ramon Servadei
 */
@SuppressWarnings("all")
public class DistributionEventHandlersJUnitTest {

    private static final String ID = "junit_remote_id";

    private static final IDomain DOMAIN = Domain.get(34);

    private static final IType TYPE = Type.get(33);

    Mockery mocks = new JUnit4Mockery();

    IDistributionState state;

    IFrameworkContext context;

    ISubscriptionManager subscriptionManager;

    ISystemEventSource sysEventSource;

    IFrameReader frameReader;

    IFrameWriter frameWriter;

    IChannel channel;

    ILifeCycleEventListener listener;

    IConnection connection;

    IConnectionParameters connectionParams;

    IConnectionBroker connectionBroker;

    IChannelFactory factory;

    Map<String, IChannel> channels = CollectionFactory.newMap();

    Map<String, IConnectionParameters> discoveredConnections = CollectionFactory.newMap();

    IAutoCreatingStore<String, Set<DualValue<ISubscriptionParameters, IEventListener>>> remoteSubscriptions = new AutoCreatingStore<String, Set<DualValue<ISubscriptionParameters, IEventListener>>>(new ValuesBuilder());

    IReferenceCounter connectedContexts = new ReferenceCounter();

    IReferenceCounter CONNECTINGContexts = new ReferenceCounter();

    IConnectionDiscoverer connectionDiscoverer;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        connectionDiscoverer = mocks.mock(IConnectionDiscoverer.class);
        factory = mocks.mock(IChannelFactory.class);
        sysEventSource = mocks.mock(ISystemEventSource.class);
        channel = mocks.mock(IChannel.class);
        listener = mocks.mock(ILifeCycleEventListener.class);
        context = mocks.mock(IFrameworkContext.class);
        frameReader = mocks.mock(IFrameReader.class);
        frameWriter = mocks.mock(IFrameWriter.class);
        connection = mocks.mock(IConnection.class);
        connectionParams = mocks.mock(IConnectionParameters.class);
        connectionBroker = mocks.mock(IConnectionBroker.class);
        state = mocks.mock(IDistributionState.class);
        prepareForStart();
    }

    @SuppressWarnings("unchecked")
    private void prepareForStart() {
        mocks.checking(new Expectations() {

            {
                allowing(sysEventSource).toIdentityString();
                will(returnValue("sysEventSource"));
                allowing(state).getContext();
                will(returnValue(context));
                allowing(state).getEventHandler();
                will(returnValue(listener));
                allowing(state).getFrameReader();
                will(returnValue(frameReader));
                allowing(state).getFrameWriter();
                will(returnValue(frameWriter));
                allowing(state).getSubscriptionManager();
                will(returnValue(subscriptionManager));
                allowing(state).getRemoteSubscriptions();
                will(returnValue(remoteSubscriptions));
                allowing(state).getDiscoveredContexts();
                will(returnValue(discoveredConnections));
                allowing(state).getConnectedContexts();
                will(returnValue(connectedContexts));
                allowing(state).getChannels();
                will(returnValue(channels));
                allowing(state).getChannelFactory();
                will(returnValue(factory));
                allowing(state).setChannels(with(a(Map.class)));
                allowing(state).getCONNECTINGContexts();
                will(returnValue(CONNECTINGContexts));
            }
        });
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testChannelAvailableEventHandler_NoSubscriptions() {
        ChannelReadyEventHandler candidate = new ChannelReadyEventHandler(state);
        mocks.checking(new Expectations() {

            {
                one(context).getSystemEventSource(ChannelReadyEvent.class);
                will(returnValue(sysEventSource));
            }
        });
        ChannelReadyEvent event = new ChannelReadyEvent(context, channel);
        mocks.checking(new Expectations() {

            {
                one(channel).getConnection();
                will(returnValue(connection));
                one(connection).isOutbound();
                will(returnValue(false));
                exactly(3).of(channel).getRemoteContextIdentity();
                will(returnValue(ID));
                one(context).getConnectedChannels();
                will(returnValue(new IChannel[0]));
            }
        });
        candidate.update(event);
    }

    @Test
    public void testChannelAvailableEventHandler_Subscriptions() {
        ChannelReadyEventHandler candidate = new ChannelReadyEventHandler(state);
        mocks.checking(new Expectations() {

            {
                one(context).getSystemEventSource(ChannelReadyEvent.class);
                will(returnValue(sysEventSource));
            }
        });
        ChannelReadyEvent event = new ChannelReadyEvent(context, channel);
        final DualValue<ISubscriptionParameters, IEventListener> values = new DualValue<ISubscriptionParameters, IEventListener>(new SubscriptionParameters(ID, TYPE, DOMAIN), listener);
        candidate.getState().getRemoteSubscriptions().get(ID).add(values);
        mocks.checking(new Expectations() {

            {
                one(channel).getConnection();
                will(returnValue(connection));
                one(connection).isOutbound();
                will(returnValue(false));
                exactly(4).of(channel).getRemoteContextIdentity();
                will(returnValue(ID));
                one(context).getSystemEventSource(RemoteContainerSubscriptionEvent.class);
                will(returnValue(sysEventSource));
                one(context).queueEvent(with(a(RemoteContainerSubscriptionEvent.class)));
                one(context).getConnectedChannels();
                will(returnValue(new IChannel[0]));
            }
        });
        candidate.update(event);
    }

    @Test
    public void testConnectionAvailableEventHandler_SingleConnection() {
        mocks.checking(new Expectations() {

            {
                one(context).getSystemEventSource(ConnectionAvailableEvent.class);
                will(returnValue(sysEventSource));
            }
        });
        ConnectionAvailableEvent event = new ConnectionAvailableEvent(context, connection);
        ConnectionAvailableEventHandler candidate = new ConnectionAvailableEventHandler(state);
        mocks.checking(new Expectations() {

            {
                one(connection).isOutbound();
                will(returnValue(false));
                allowing(connection).getRemoteContextIdentity();
                will(returnValue(ID));
                one(factory).createChannel(connection, context);
                will(returnValue(channel));
                one(channel).start();
            }
        });
        candidate.update(event);
        assertEquals("connection count", 1, candidate.getState().getConnectedContexts().getCount(ID));
    }

    @Test
    public void testConnectionAvailableEventHandler_DuplicateConnectionOnInferiorContext() {
        mocks.checking(new Expectations() {

            {
                one(context).getSystemEventSource(ConnectionAvailableEvent.class);
                will(returnValue(sysEventSource));
            }
        });
        ConnectionAvailableEvent event = new ConnectionAvailableEvent(context, connection);
        ConnectionAvailableEventHandler candidate = new ConnectionAvailableEventHandler(state);
        mocks.checking(new Expectations() {

            {
                one(connection).isOutbound();
                will(returnValue(false));
                allowing(connection).getRemoteContextIdentity();
                will(returnValue(ID));
                one(factory).createChannel(connection, context);
                will(returnValue(channel));
                one(channel).start();
                one(context).getIdentity();
                will(returnValue(EMPTY_STRING));
            }
        });
        candidate.getState().getConnectedContexts().adjustCount(ID, 1);
        candidate.update(event);
        assertEquals("connection count", 2, candidate.getState().getConnectedContexts().getCount(ID));
    }

    @Test
    public void testConnectionAvailableEventHandler_DuplicateConnectionEqualIdentity() {
        mocks.checking(new Expectations() {

            {
                one(context).getSystemEventSource(ConnectionAvailableEvent.class);
                will(returnValue(sysEventSource));
            }
        });
        ConnectionAvailableEvent event = new ConnectionAvailableEvent(context, connection);
        ConnectionAvailableEventHandler candidate = new ConnectionAvailableEventHandler(state);
        mocks.checking(new Expectations() {

            {
                one(connection).isOutbound();
                will(returnValue(false));
                allowing(connection).getRemoteContextIdentity();
                will(returnValue(ID));
                one(context).getIdentity();
                will(returnValue(ID));
                one(connection).getRemoteContextHashCode();
                will(returnValue(-1));
                one(context).getContextHashCode();
                will(returnValue(1));
                one(connection).destroy();
            }
        });
        candidate.getState().getConnectedContexts().adjustCount(ID, 1);
        candidate.update(event);
        assertEquals("connection count", 1, candidate.getState().getConnectedContexts().getCount(ID));
    }

    @Test
    public void testConnectionAvailableEventHandler_DuplicateConnectionOnSuperiorContext() {
        mocks.checking(new Expectations() {

            {
                allowing(connection).getRemoteContextIdentity();
                will(returnValue(ID));
                allowing(connectionParams).getRemoteContextIdentity();
                will(returnValue(ID));
                one(context).getSystemEventSource(ConnectionAvailableEvent.class);
                will(returnValue(sysEventSource));
            }
        });
        ConnectionAvailableEvent event = new ConnectionAvailableEvent(context, connection);
        ConnectionAvailableEventHandler candidate = new ConnectionAvailableEventHandler(state);
        mocks.checking(new Expectations() {

            {
                one(connection).isOutbound();
                will(returnValue(false));
                allowing(context).getIdentity();
                will(returnValue("z" + ID + "1"));
                one(connection).destroy();
            }
        });
        candidate.getState().getConnectedContexts().adjustCount(ID, 1);
        candidate.update(event);
        assertEquals("connection count", 1, candidate.getState().getConnectedContexts().getCount(ID));
    }

    @Test
    public void testConnectionDestroyedEventHandler() {
        mocks.checking(new Expectations() {

            {
                one(context).getSystemEventSource(ConnectionDestroyedEvent.class);
                will(returnValue(sysEventSource));
                one(context).getConnectionDiscoverer();
                will(returnValue(connectionDiscoverer));
                one(connectionDiscoverer).connectionDestroyed(ID);
            }
        });
        ConnectionDestroyedEvent event = new ConnectionDestroyedEvent(context, ID);
        ConnectionDestroyedEventHandler candidate = new ConnectionDestroyedEventHandler(state);
        candidate.getState().getChannels().put(ID, channel);
        mocks.checking(new Expectations() {

            {
                one(channel).destroy();
            }
        });
        candidate.getState().getConnectedContexts().adjustCount(ID, 1);
        candidate.update(event);
        assertEquals("connection count", 0, candidate.getState().getConnectedContexts().getCount(ID));
    }

    @Test
    public void testConnectionDestroyedEventHandler_NoChannelExists() {
        mocks.checking(new Expectations() {

            {
                one(connectionDiscoverer).connectionDestroyed(ID);
                one(context).getSystemEventSource(ConnectionDestroyedEvent.class);
                will(returnValue(sysEventSource));
                one(context).getConnectionDiscoverer();
                will(returnValue(connectionDiscoverer));
            }
        });
        ConnectionDestroyedEvent event = new ConnectionDestroyedEvent(context, ID);
        ConnectionDestroyedEventHandler candidate = new ConnectionDestroyedEventHandler(state);
        candidate.getState().getConnectedContexts().adjustCount(ID, 1);
        candidate.update(event);
        assertEquals("connection count", 0, candidate.getState().getConnectedContexts().getCount(ID));
    }

    @Test
    public void testConnectionDestroyedEventHandler_ReconnectRequired() {
        mocks.checking(new Expectations() {

            {
                allowing(connectionParams).getRemoteContextIdentity();
                will(returnValue(ID));
                one(context).getSystemEventSource(ConnectionDestroyedEvent.class);
                will(returnValue(sysEventSource));
                one(context).getConnectionDiscoverer();
                will(returnValue(connectionDiscoverer));
                one(connectionDiscoverer).connectionDestroyed(ID);
            }
        });
        ConnectionDestroyedEvent event = new ConnectionDestroyedEvent(context, ID);
        ConnectionDestroyedEventHandler candidate = new ConnectionDestroyedEventHandler(state);
        candidate.getState().getChannels().put(ID, channel);
        candidate.getState().getDiscoveredContexts().put(ID, connectionParams);
        candidate.getState().getRemoteSubscriptions().get(ID).add(new DualValue<ISubscriptionParameters, IEventListener>(null, null));
        mocks.checking(new Expectations() {

            {
                one(channel).destroy();
            }
        });
        candidate.getState().getConnectedContexts().adjustCount(ID, 1);
        candidate.update(event);
        assertEquals("connection count", 0, candidate.getState().getConnectedContexts().getCount(ID));
    }

    @Test
    public void testConnectionDiscoveredEventHandler_FirstDiscoveryNoSubscriptions() {
        mocks.checking(new Expectations() {

            {
                one(context).isActive();
                will(returnValue(true));
                one(context).getSystemEventSource(ContextDiscoveredEvent.class);
                will(returnValue(sysEventSource));
            }
        });
        ContextDiscoveredEvent event = new ContextDiscoveredEvent(context, connectionParams);
        ContextDiscoveredEventHandler candidate = new ContextDiscoveredEventHandler(state);
        mocks.checking(new Expectations() {

            {
                allowing(connectionParams).getRemoteContextIdentity();
                will(returnValue(ID));
            }
        });
        candidate.update(event);
    }

    @Test
    public void testConnectionDiscoveredEventHandler_FirstDiscoveryWithSubscriptions() {
        mocks.checking(new Expectations() {

            {
                one(context).isActive();
                will(returnValue(true));
                one(context).getSystemEventSource(ContextDiscoveredEvent.class);
                will(returnValue(sysEventSource));
            }
        });
        ContextDiscoveredEvent event = new ContextDiscoveredEvent(context, connectionParams);
        ContextDiscoveredEventHandler candidate = new ContextDiscoveredEventHandler(state);
        mocks.checking(new Expectations() {

            {
                allowing(connectionParams).getRemoteContextIdentity();
                will(returnValue(ID));
                one(context).getConnectionBroker();
                will(returnValue(connectionBroker));
                one(connectionBroker).connect(connectionParams);
            }
        });
        candidate.getState().getRemoteSubscriptions().get(ID).add(new DualValue<ISubscriptionParameters, IEventListener>(null, null));
        candidate.update(event);
    }

    @Test
    public void testConnectionDiscoveredEventHandler_SecondDiscovery() {
        mocks.checking(new Expectations() {

            {
                one(context).isActive();
                will(returnValue(true));
                one(context).getSystemEventSource(ContextDiscoveredEvent.class);
                will(returnValue(sysEventSource));
            }
        });
        ContextDiscoveredEvent event = new ContextDiscoveredEvent(context, connectionParams);
        ContextDiscoveredEventHandler candidate = new ContextDiscoveredEventHandler(state);
        mocks.checking(new Expectations() {

            {
                allowing(connectionParams).getRemoteContextIdentity();
                will(returnValue(ID));
            }
        });
        candidate.getState().getDiscoveredContexts().put(ID, connectionParams);
        candidate.update(event);
    }

    @Test
    public void testConnectionDiscoveredEventHandler_SecondDiscoveryAndConnectionParametersChange() {
        mocks.checking(new Expectations() {

            {
                one(context).isActive();
                will(returnValue(true));
                one(context).getSystemEventSource(ContextDiscoveredEvent.class);
                will(returnValue(sysEventSource));
            }
        });
        final IConnectionParameters params1 = mocks.mock(IConnectionParameters.class);
        ContextDiscoveredEvent event = new ContextDiscoveredEvent(context, connectionParams);
        ContextDiscoveredEventHandler candidate = new ContextDiscoveredEventHandler(state);
        mocks.checking(new Expectations() {

            {
                one(channel).getConnection();
                will(returnValue(connection));
                one(connection).isOutbound();
                will(returnValue(true));
                one(channel).getConnection();
                will(returnValue(connection));
                one(connectionParams).isEqual(connection);
                will(returnValue(false));
                allowing(connectionParams).getRemoteContextIdentity();
                will(returnValue(ID));
                one(channel).getConnection();
                will(returnValue(connection));
                one(connection).destroy();
            }
        });
        candidate.getState().getDiscoveredContexts().put(ID, params1);
        candidate.getState().getChannels().put(ID, channel);
        candidate.update(event);
    }

    @Test
    public void testChannelSubscriptionEventHandler() {
        mocks.checking(new Expectations() {

            {
                one(context).getSystemEventSource(RemoteContainerSubscriptionEvent.class);
                will(returnValue(sysEventSource));
            }
        });
        final String idRegex = "id-regex";
        DualValue<ISubscriptionParameters, IEventListener> values = new DualValue<ISubscriptionParameters, IEventListener>(new SubscriptionParameters(idRegex, TYPE, DOMAIN), listener);
        RemoteContainerSubscriptionEvent event = new RemoteContainerSubscriptionEvent(context, ID, values);
        RemoteContainerSubscriptionEventHandler candidate = new RemoteContainerSubscriptionEventHandler(state);
        candidate.getState().getChannels().put(ID, channel);
        mocks.checking(new Expectations() {

            {
                one(channel).subscribe(new SubscriptionParameters(idRegex, TYPE, DOMAIN));
                one(channel).addListener(new SubscriptionParameters(idRegex, TYPE, DOMAIN), listener);
            }
        });
        candidate.update(event);
    }
}
