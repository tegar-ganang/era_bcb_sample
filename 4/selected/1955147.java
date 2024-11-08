package fulmine.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.List;
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
import fulmine.distribution.channel.IChannel;
import fulmine.distribution.connection.IConnection;
import fulmine.distribution.connection.IConnectionBroker;
import fulmine.distribution.connection.IConnectionParameters;
import fulmine.event.IEvent;
import fulmine.event.listener.IEventListener;
import fulmine.event.listener.ILifeCycleEventListener;
import fulmine.event.subscription.ISubscriptionManager;
import fulmine.event.subscription.ISubscriptionParameters;
import fulmine.event.subscription.SubscriptionParameters;
import fulmine.event.system.ISystemEventSource;
import fulmine.protocol.specification.IFrameReader;
import fulmine.protocol.specification.IFrameWriter;
import fulmine.util.collection.CollectionFactory;
import fulmine.util.log.AsyncLog;
import fulmine.util.reference.AutoCreatingStore;
import fulmine.util.reference.DualValue;
import fulmine.util.reference.IAutoCreatingStore;
import fulmine.util.reference.IReferenceCounter;
import fulmine.util.reference.ReferenceCounter;

/**
 * Test cases for the {@link DistributionManager}
 * 
 * @author Ramon Servadei
 */
@SuppressWarnings("all")
public class DistributionManagerJUnitTest {

    private static final AsyncLog LOG = new AsyncLog(DistributionManagerJUnitTest.class);

    final SubscriptionParameters params = new SubscriptionParameters(ID_REGEX, TYPE, DOMAIN);

    private static final String ID = "junit_id";

    private static final String ID2 = "junit_id2";

    private static final String ID_REGEX = "junit_regex";

    private static final IDomain DOMAIN = Domain.get(23);

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

    DistributionManager candidate;

    IEvent event;

    Map<String, IChannel> channels = CollectionFactory.newMap();

    Map<String, IConnectionParameters> discoveredConnections = CollectionFactory.newMap();

    IAutoCreatingStore<String, Set<DualValue<ISubscriptionParameters, IEventListener>>> remoteSubscriptions = new AutoCreatingStore<String, Set<DualValue<ISubscriptionParameters, IEventListener>>>(new ValuesBuilder());

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        state = mocks.mock(IDistributionState.class);
        event = mocks.mock(IEvent.class);
        sysEventSource = mocks.mock(ISystemEventSource.class);
        channel = mocks.mock(IChannel.class);
        listener = mocks.mock(ILifeCycleEventListener.class);
        context = mocks.mock(IFrameworkContext.class);
        frameReader = mocks.mock(IFrameReader.class);
        frameWriter = mocks.mock(IFrameWriter.class);
        connection = mocks.mock(IConnection.class);
        connectionParams = mocks.mock(IConnectionParameters.class);
        connectionBroker = mocks.mock(IConnectionBroker.class);
        subscriptionManager = mocks.mock(ISubscriptionManager.class);
        candidate = new DistributionManager(state);
        candidate.setState(state);
        prepareForStart();
        candidate.start();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    private void prepareForStart() {
        mocks.checking(new Expectations() {

            {
                one(state).init();
                one(state).start();
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
                allowing(state).getChannels();
                will(returnValue(channels));
            }
        });
    }

    /**
     * Test method for
     * {@link fulmine.context.DistributionManager#subscribe(java.lang.String, java.lang.String, IType, IDomain, fulmine.event.listener.IEventListener)}
     * .
     */
    @Test
    public void testSubscribeLocal() {
        mocks.checking(new Expectations() {

            {
                one(context).getIdentity();
                will(returnValue(ID));
                one(subscriptionManager).subscribe(params);
                will(returnValue(true));
                one(subscriptionManager).addListener(params, listener);
                will(returnValue(true));
            }
        });
        assertTrue(candidate.subscribe(ID, ID_REGEX, TYPE, DOMAIN, listener));
    }

    @Test
    public void testSubscribeLocalIsIdempotent() {
        testSubscribeLocal();
        testSubscribeLocal();
    }

    @Test
    public void testSubscribeRemoteIsIdempotent() {
        final IReferenceCounter CONNECTING = new ReferenceCounter();
        final IReferenceCounter connected = new ReferenceCounter();
        prepareMocksForRemoteSubscribeNoChannel(CONNECTING, connected);
        final DualValue<ISubscriptionParameters, IEventListener> values = new DualValue<ISubscriptionParameters, IEventListener>(params, listener);
        candidate.getState().getDiscoveredContexts().put(ID, connectionParams);
        assertTrue(candidate.subscribe(ID, ID_REGEX, TYPE, DOMAIN, listener));
        assertTrue("values not found", candidate.getState().getRemoteSubscriptions().get(ID).contains(values));
        mocks.checking(new Expectations() {

            {
                one(context).getIdentity();
                will(returnValue("local"));
                allowing(state).getConnectedContexts();
                will(returnValue(connected));
                allowing(state).getCONNECTINGContexts();
                will(returnValue(CONNECTING));
                allowing(connectionParams).getRemoteContextIdentity();
                will(returnValue(ID));
            }
        });
        assertFalse(candidate.subscribe(ID, ID_REGEX, TYPE, DOMAIN, listener));
        assertEquals("more values found", 1, candidate.getState().getRemoteSubscriptions().get(ID).size());
    }

    private void prepareMocksForRemoteSubscribeNoChannel(final IReferenceCounter CONNECTING, final IReferenceCounter connected) {
        mocks.checking(new Expectations() {

            {
                one(context).getIdentity();
                will(returnValue("local"));
                one(context).getConnectionBroker();
                will(returnValue(connectionBroker));
                one(connectionBroker).connect(connectionParams);
                allowing(state).getConnectedContexts();
                will(returnValue(connected));
                allowing(state).getCONNECTINGContexts();
                will(returnValue(CONNECTING));
                allowing(connectionParams).getRemoteContextIdentity();
                will(returnValue(ID));
            }
        });
    }

    /**
     * Test method for
     * {@link fulmine.context.DistributionManager#subscribe(java.lang.String, java.lang.String, IType, IDomain, fulmine.event.listener.IEventListener)}
     * .
     */
    @Test
    public void testSubscribeRemote_ChannelExists() {
        mocks.checking(new Expectations() {

            {
                one(context).getIdentity();
                will(returnValue("local"));
                one(context).getSystemEventSource(RemoteContainerSubscriptionEvent.class);
                will(returnValue(sysEventSource));
                one(context).queueEvent(with(a(RemoteContainerSubscriptionEvent.class)));
            }
        });
        final DualValue<ISubscriptionParameters, IEventListener> values = new DualValue<ISubscriptionParameters, IEventListener>(params, listener);
        candidate.getState().getChannels().put(ID, channel);
        assertTrue(candidate.subscribe(ID, ID_REGEX, TYPE, DOMAIN, listener));
        assertTrue("values not found", candidate.getState().getRemoteSubscriptions().get(ID).contains(values));
    }

    /**
     * Test method for
     * {@link fulmine.context.DistributionManager#subscribe(java.lang.String, java.lang.String, IType, IDomain, fulmine.event.listener.IEventListener)}
     * .
     */
    @Test
    public void testSubscribeRemote_NoChannel() {
        final IReferenceCounter connected = new ReferenceCounter();
        final IReferenceCounter CONNECTING = new ReferenceCounter();
        prepareMocksForRemoteSubscribeNoChannel(CONNECTING, connected);
        final DualValue<ISubscriptionParameters, IEventListener> values = new DualValue<ISubscriptionParameters, IEventListener>(params, listener);
        candidate.getState().getDiscoveredContexts().put(ID, connectionParams);
        assertTrue(candidate.subscribe(ID, ID_REGEX, TYPE, DOMAIN, listener));
        assertTrue("values not found", candidate.getState().getRemoteSubscriptions().get(ID).contains(values));
    }

    /**
     * Test method for
     * {@link fulmine.context.DistributionManager#unsubscribe(java.lang.String, java.lang.String, IType, IDomain, fulmine.event.listener.IEventListener)}
     * .
     */
    @Test
    public void testUnsubscribeLocal() {
        mocks.checking(new Expectations() {

            {
                one(context).getIdentity();
                will(returnValue(ID));
                final SubscriptionParameters subscriptionParameters = params;
                one(subscriptionManager).removeListener(subscriptionParameters, listener);
                will(returnValue(true));
                final List<IEventListener> listeners = CollectionFactory.newList();
                atLeast(1).of(subscriptionManager).getListeners(subscriptionParameters);
                will(returnValue(listeners));
                one(subscriptionManager).unsubscribe(subscriptionParameters);
                will(returnValue(true));
                atMost(1).of(subscriptionManager).getSubscribedSources(subscriptionParameters);
                will(returnValue(CollectionFactory.newList()));
            }
        });
        assertTrue(candidate.unsubscribe(ID, ID_REGEX, TYPE, DOMAIN, listener));
    }

    /**
     * Test method for
     * {@link fulmine.context.DistributionManager#unsubscribe(java.lang.String, java.lang.String, IType, IDomain, fulmine.event.listener.IEventListener)}
     * .
     */
    @Test
    public void testUnsubscribeRemote() {
        mocks.checking(new Expectations() {

            {
                one(context).getIdentity();
                will(returnValue("local"));
                one(channel).removeListener(params, listener);
                will(returnValue(true));
                final List<IEventListener> listeners = CollectionFactory.newList();
                atLeast(1).of(channel).getListeners(params);
                will(returnValue(listeners));
                one(channel).unsubscribe(params);
                will(returnValue(true));
                final SubscriptionParameters subscriptionParameters = params;
                atMost(1).of(channel).getSubscribedSources(subscriptionParameters);
                will(returnValue(CollectionFactory.newList()));
            }
        });
        final DualValue<ISubscriptionParameters, IEventListener> values = new DualValue<ISubscriptionParameters, IEventListener>(params, listener);
        candidate.getState().getRemoteSubscriptions().get(ID).add(values);
        candidate.getState().getChannels().put(ID, channel);
        assertTrue(candidate.unsubscribe(ID, ID_REGEX, TYPE, DOMAIN, listener));
        assertFalse("values found", candidate.getState().getRemoteSubscriptions().get(ID).contains(values));
    }

    /**
     * Test method for
     * {@link fulmine.context.DistributionManager#getFrameReader()}.
     */
    @Test
    public void testGetFrameReader() {
        assertEquals("frameReader", frameReader, candidate.getFrameReader());
    }

    /**
     * Test method for
     * {@link fulmine.context.DistributionManager#getFrameWriter()}.
     */
    @Test
    public void testGetFrameWriter() {
        assertEquals("frameWriter", frameWriter, candidate.getFrameWriter());
    }

    /**
     * Test method for
     * {@link fulmine.context.DistributionManager#requestRetransmit(java.lang.String, java.lang.String, IType, IDomain)}
     * .
     */
    @Test
    public void testRequestRetransmit() {
        state.getChannels().put(ID, channel);
        state.getChannels().put(ID2, channel);
        mocks.checking(new Expectations() {

            {
                one(channel).requestRetransmit(ID_REGEX, TYPE, DOMAIN);
            }
        });
        candidate.requestRetransmit(ID, ID_REGEX, TYPE, DOMAIN);
    }

    /**
     * Test method for
     * {@link fulmine.context.DistributionManager#requestRetransmitAll(java.lang.String)}
     * .
     */
    @Test
    public void testRequestRetransmitAll() {
        state.getChannels().put(ID, channel);
        state.getChannels().put(ID2, channel);
        mocks.checking(new Expectations() {

            {
                one(channel).requestRetransmitAll();
            }
        });
        candidate.requestRetransmitAll(ID);
    }

    /**
     * Test method for
     * {@link fulmine.context.DistributionManager#retransmit(java.lang.String, java.lang.String, IType, IDomain)}
     * .
     */
    @Test
    public void testRetransmit() {
        state.getChannels().put(ID, channel);
        state.getChannels().put(ID2, channel);
        mocks.checking(new Expectations() {

            {
                one(channel).retransmit(ID_REGEX, TYPE, DOMAIN);
            }
        });
        candidate.retransmit(ID, ID_REGEX, TYPE, DOMAIN);
    }

    /**
     * Test method for
     * {@link fulmine.context.DistributionManager#retransmitAll(java.lang.String)}
     * .
     */
    @Test
    public void testRetransmitAll() {
        state.getChannels().put(ID, channel);
        state.getChannels().put(ID2, channel);
        mocks.checking(new Expectations() {

            {
                one(channel).retransmitAll();
            }
        });
        candidate.retransmitAll(ID);
    }

    /**
     * Test method for
     * {@link fulmine.context.DistributionManager#retransmitAllToAll()}.
     */
    @Test
    public void testRetransmitAllToAll() {
        state.getChannels().put(ID, channel);
        state.getChannels().put(ID2, channel);
        mocks.checking(new Expectations() {

            {
                exactly(2).of(channel).retransmit(ID_REGEX, TYPE, DOMAIN);
            }
        });
        candidate.retransmitToAll(ID_REGEX, TYPE, DOMAIN);
    }

    /**
     * Test method for
     * {@link fulmine.context.DistributionManager#retransmitToAll(java.lang.String, IType, IDomain)}
     * .
     */
    @Test
    public void testRetransmitToAll() {
        state.getChannels().put(ID, channel);
        state.getChannels().put(ID2, channel);
        mocks.checking(new Expectations() {

            {
                exactly(2).of(channel).retransmitAll();
            }
        });
        candidate.retransmitAllToAll();
    }
}
