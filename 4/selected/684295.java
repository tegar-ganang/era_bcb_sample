package com.sleepycat.je.rep.impl.node;

import static com.sleepycat.je.rep.ReplicatedEnvironment.State.DETACHED;
import static com.sleepycat.je.rep.ReplicatedEnvironment.State.MASTER;
import static com.sleepycat.je.rep.ReplicatedEnvironment.State.REPLICA;
import static com.sleepycat.je.rep.ReplicatedEnvironment.State.UNKNOWN;
import static com.sleepycat.je.rep.impl.RepParams.DBTREE_CACHE_CLEAR_COUNT;
import static com.sleepycat.je.rep.impl.RepParams.ENV_CONSISTENCY_TIMEOUT;
import static com.sleepycat.je.rep.impl.RepParams.ENV_SETUP_TIMEOUT;
import static com.sleepycat.je.rep.impl.RepParams.HEARTBEAT_INTERVAL;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Durability;
import com.sleepycat.je.EnvironmentFailureException;
import com.sleepycat.je.ReplicaConsistencyPolicy;
import com.sleepycat.je.StatsConfig;
import com.sleepycat.je.Durability.ReplicaAckPolicy;
import com.sleepycat.je.dbi.DbConfigManager;
import com.sleepycat.je.log.LogManager;
import com.sleepycat.je.rep.GroupShutdownException;
import com.sleepycat.je.rep.MasterStateException;
import com.sleepycat.je.rep.MemberNotFoundException;
import com.sleepycat.je.rep.NoConsistencyRequiredPolicy;
import com.sleepycat.je.rep.QuorumPolicy;
import com.sleepycat.je.rep.RepInternal;
import com.sleepycat.je.rep.ReplicaConsistencyException;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.ReplicatedEnvironmentStats;
import com.sleepycat.je.rep.RestartRequiredException;
import com.sleepycat.je.rep.UnknownMasterException;
import com.sleepycat.je.rep.elections.Elections;
import com.sleepycat.je.rep.elections.TimebasedProposalGenerator;
import com.sleepycat.je.rep.elections.Proposer.Proposal;
import com.sleepycat.je.rep.impl.GroupService;
import com.sleepycat.je.rep.impl.NodeStateService;
import com.sleepycat.je.rep.impl.PointConsistencyPolicy;
import com.sleepycat.je.rep.impl.RepGroupDB;
import com.sleepycat.je.rep.impl.RepGroupImpl;
import com.sleepycat.je.rep.impl.RepImpl;
import com.sleepycat.je.rep.impl.RepNodeImpl;
import com.sleepycat.je.rep.impl.RepParams;
import com.sleepycat.je.rep.monitor.LeaveGroupEvent.LeaveReason;
import com.sleepycat.je.rep.stream.FeederTxns;
import com.sleepycat.je.rep.stream.MasterChangeListener;
import com.sleepycat.je.rep.stream.MasterStatus;
import com.sleepycat.je.rep.stream.MasterSuggestionGenerator;
import com.sleepycat.je.rep.util.ldiff.LDiffService;
import com.sleepycat.je.rep.utilint.RepUtils;
import com.sleepycat.je.rep.utilint.ServiceDispatcher;
import com.sleepycat.je.rep.utilint.RepUtils.ExceptionAwareCountDownLatch;
import com.sleepycat.je.rep.vlsn.VLSNIndex;
import com.sleepycat.je.utilint.LoggerUtils;
import com.sleepycat.je.utilint.StoppableThread;
import com.sleepycat.je.utilint.VLSN;

/**
 * Represents a replication node. This class is the locus of operations that
 * manage the state of the node, master, replica, etc. Once the state of a node
 * has been established the thread of control passes over to the Replica or
 * FeederManager instances.
 *
 * Note that both Feeders and the Replica instance may be active in future when
 * we support r2r replication, in addition to m2r replication. For now however,
 * either the FeederManager is active, or the Replica is and the same common
 * thread control can be shared between the two.
 */
public class RepNode extends StoppableThread {

    private final NameIdPair nameIdPair;

    private final InetSocketAddress mySocket;

    private final ServiceDispatcher serviceDispatcher;

    private Elections elections;

    private final Replica replica;

    private final FeederManager feederManager;

    private final MasterStatus masterStatus;

    private final MasterChangeListener changeListener;

    private final MasterSuggestionGenerator suggestionGenerator;

    private final NodeState nodeState;

    private volatile boolean activePrimary = false;

    private int electableGroupSizeOverride;

    private final RepImpl repImpl;

    final RepGroupDB repGroupDB;

    private volatile ExceptionAwareCountDownLatch readyLatch = null;

    private final CommitFreezeLatch vlsnFreezeLatch = new CommitFreezeLatch();

    private RepGroupImpl group;

    private volatile VLSN currentCommitVLSN = null;

    private QuorumPolicy electionQuorumPolicy = QuorumPolicy.SIMPLE_MAJORITY;

    private static final int MASTER_QUERY_INTERVAL = 10000;

    private static final int JOIN_RETRIES = 10;

    private final Clock clock;

    private com.sleepycat.je.rep.impl.networkRestore.FeederManager logFeederManager;

    private LDiffService ldiff;

    private NodeStateService nodeStateService;

    final LocalCBVLSNTracker cbvlsnTracker;

    final GlobalCBVLSN globalCBVLSN;

    private long replicaCloseCatchupMs = -1;

    private MonitorEventManager monitorEventManager;

    private final Timer timer;

    private final ChannelTimeoutTask channelTimeoutTask;

    final Logger logger;

    public RepNode(RepImpl repImpl, Replay replay, NodeState nodeState) throws IOException, DatabaseException {
        super(repImpl);
        this.repImpl = repImpl;
        readyLatch = new ExceptionAwareCountDownLatch(repImpl, 1);
        nameIdPair = repImpl.getNameIdPair();
        logger = LoggerUtils.getLogger(getClass());
        setName("RepNode " + nameIdPair);
        this.mySocket = repImpl.getSocket();
        this.serviceDispatcher = new ServiceDispatcher(mySocket, repImpl);
        serviceDispatcher.start();
        clock = new Clock(RepImpl.getClockSkewMs());
        this.repGroupDB = new RepGroupDB(repImpl);
        masterStatus = new MasterStatus(nameIdPair);
        replica = ReplicaFactory.create(this, replay);
        feederManager = new FeederManager(this);
        changeListener = new MasterChangeListener(this);
        suggestionGenerator = new MasterSuggestionGenerator(this);
        this.nodeState = nodeState;
        electableGroupSizeOverride = repImpl.getConfigManager().getInt(RepParams.ELECTABLE_GROUP_SIZE_OVERRIDE);
        if (electableGroupSizeOverride > 0) {
            LoggerUtils.warning(logger, repImpl, "Electable group size override set to:" + electableGroupSizeOverride);
        }
        utilityServicesStart();
        this.cbvlsnTracker = new LocalCBVLSNTracker(this);
        this.globalCBVLSN = new GlobalCBVLSN(this);
        this.monitorEventManager = new MonitorEventManager(this);
        timer = new Timer(true);
        channelTimeoutTask = new ChannelTimeoutTask(timer);
    }

    private void utilityServicesStart() {
        ldiff = new LDiffService(serviceDispatcher, repImpl);
        logFeederManager = new com.sleepycat.je.rep.impl.networkRestore.FeederManager(serviceDispatcher, repImpl, nameIdPair);
        nodeStateService = new NodeStateService(serviceDispatcher, this);
        serviceDispatcher.register(nodeStateService);
    }

    public RepNode(NameIdPair nameIdPair) {
        this(nameIdPair, null);
    }

    public RepNode() {
        this(NameIdPair.NULL);
    }

    public RepNode(NameIdPair nameIdPair, ServiceDispatcher serviceDispatcher) {
        super();
        repImpl = null;
        clock = new Clock(0);
        this.nameIdPair = nameIdPair;
        mySocket = null;
        this.serviceDispatcher = serviceDispatcher;
        this.repGroupDB = null;
        masterStatus = new MasterStatus(NameIdPair.NULL);
        replica = null;
        feederManager = null;
        changeListener = null;
        suggestionGenerator = null;
        nodeState = null;
        cbvlsnTracker = null;
        globalCBVLSN = null;
        logger = null;
        timer = null;
        channelTimeoutTask = null;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    public ServiceDispatcher getServiceDispatcher() {
        return serviceDispatcher;
    }

    /**
     * Returns the accumulated statistics for this node. The method
     * encapsulates the statistics associated with its two principal components
     * the FeederManager and the Replica.
     */
    public ReplicatedEnvironmentStats getStats(StatsConfig config) {
        ReplicatedEnvironmentStats ret = RepInternal.makeReplicatedEnvironmentStats(feederManager, replica, config);
        return ret;
    }

    public void resetStats() {
        feederManager.resetStats();
        replica.resetStats();
    }

    public ExceptionAwareCountDownLatch getReadyLatch() {
        return readyLatch;
    }

    public CommitFreezeLatch getVLSNFreezeLatch() {
        return vlsnFreezeLatch;
    }

    public void resetReadyLatch(Exception exception) {
        if (readyLatch.getCount() != 0) {
            readyLatch.releaseAwait(exception);
        }
        readyLatch = new ExceptionAwareCountDownLatch(repImpl, 1);
    }

    public FeederManager feederManager() {
        return feederManager;
    }

    public Replica replica() {
        return replica;
    }

    public Clock getClock() {
        return clock;
    }

    Replica getReplica() {
        return replica;
    }

    public RepGroupDB getRepGroupDB() {
        return repGroupDB;
    }

    public RepGroupImpl getGroup() {
        return group;
    }

    /**
     * Returns the UUID associated with the replicated environment.
     */
    public UUID getUUID() {
        if (group == null) {
            throw EnvironmentFailureException.unexpectedState("Group info is not available");
        }
        return group.getUUID();
    }

    /**
     * Returns the nodeName associated with this replication node.
     *
     * @return the nodeName
     */
    public String getNodeName() {
        return nameIdPair.getName();
    }

    /**
     * Returns the nodeId associated with this replication node.
     *
     * @return the nodeId
     */
    public int getNodeId() {
        return nameIdPair.getId();
    }

    public NameIdPair getNameIdPair() {
        return nameIdPair;
    }

    public InetSocketAddress getSocket() {
        return mySocket;
    }

    public MasterStatus getMasterStatus() {
        return masterStatus;
    }

    public int getHeartbeatInterval() {
        return getConfigManager().getInt(HEARTBEAT_INTERVAL);
    }

    public int getElectionPriority() {
        final int priority = getConfigManager().getInt(RepParams.NODE_PRIORITY);
        final int defaultPriority = Integer.parseInt(RepParams.NODE_PRIORITY.getDefault());
        return (getConfigManager().getBoolean(RepParams.DESIGNATED_PRIMARY) && (priority == defaultPriority)) ? defaultPriority + 1 : priority;
    }

    public int getThreadWaitInterval() {
        return getHeartbeatInterval() * 4;
    }

    int getDbTreeCacheClearingOpCount() {
        return getConfigManager().getInt(DBTREE_CACHE_CLEAR_COUNT);
    }

    public RepImpl getRepImpl() {
        return repImpl;
    }

    public LogManager getLogManager() {
        return repImpl.getLogManager();
    }

    DbConfigManager getConfigManager() {
        return repImpl.getConfigManager();
    }

    public VLSNIndex getVLSNIndex() {
        return repImpl.getVLSNIndex();
    }

    public FeederTxns getFeederTxns() {
        return repImpl.getFeederTxns();
    }

    public Elections getElections() {
        return elections;
    }

    /**
     * Returns a list of nodes suitable for feeding log files for a network
     * restore.
     *
     * @return a list of hostPort pairs
     */
    public RepNodeImpl[] getLogProviders() {
        Set<RepNodeImpl> nodes = getGroup().getAllElectableMembers();
        RepNodeImpl[] logProviders = new RepNodeImpl[nodes.size()];
        int i = 0;
        for (RepNodeImpl node : nodes) {
            logProviders[i++] = node;
        }
        return logProviders;
    }

    public ChannelTimeoutTask getChannelTimeoutTask() {
        return channelTimeoutTask;
    }

    public boolean isMaster() {
        return masterStatus.isNodeMaster();
    }

    /**
     * Notes the VLSN associated with the latest commit. The updates are
     * done in ascending order.
     *
     * @param commitVLSN the commit VLSNt
     */
    public void currentCommitVLSN(VLSN commitVLSN) {
        currentCommitVLSN = commitVLSN;
    }

    public MonitorEventManager getMonitorEventManager() {
        return monitorEventManager;
    }

    public String getMasterName() {
        if (masterStatus.getGroupMasterNameId().getId() == NameIdPair.NULL_NODE_ID) {
            return null;
        }
        return masterStatus.getGroupMasterNameId().getName();
    }

    /**
     * Returns the latest VLSN associated with a replicated commit.
     */
    public VLSN getCurrentCommitVLSN() {
        return currentCommitVLSN;
    }

    public void forceMaster(boolean force) throws InterruptedException, DatabaseException {
        suggestionGenerator.forceMaster(force);
        refreshCachedGroup();
        elections.initiateElection(group, electionQuorumPolicy);
    }

    /**
     * Starts up the thread in which the node does its processing as a master
     * or replica. It then waits for the newly started thread to transition it
     * out of the DETACHED state, and returns upon completion of this
     * transition.
     *
     * @throws IOException
     * @throws DatabaseException
     */
    private void startup(QuorumPolicy initialElectionPolicy) throws IOException, DatabaseException {
        if (isAlive()) {
            return;
        }
        assert (nodeState.getRepEnvState().isDetached());
        elections = new Elections(this, changeListener, suggestionGenerator);
        group = repGroupDB.emptyGroup;
        refreshCachedGroup();
        findMaster();
        this.electionQuorumPolicy = initialElectionPolicy;
        elections.participate();
        start();
    }

    /**
     * This method must be invoked when a RepNode is first initialized and
     * subsequently every time there is a change to the replication group.
     * <p>
     * The Master should invoke this method each time a member is added or
     * removed, and a replica should invoke it each time it detects the commit
     * of a transaction that modifies the membership database.
     * <p>
     * In addition, it must be invoked after a syncup operation, since it may
     * revert changes made to the membership table.
     *
     * @throws DatabaseException
     */
    public RepGroupImpl refreshCachedGroup() throws DatabaseException {
        group = repGroupDB.getGroup(new NoConsistencyRequiredPolicy());
        elections.updateRepGroup(group);
        if (nameIdPair.hasNullId()) {
            RepNodeImpl n = group.getMember(nameIdPair.getName());
            if (n != null) {
                nameIdPair.update(n.getNameIdPair());
            }
        }
        return group;
    }

    /**
     * Removes a node so that it's no longer a member of the group.
     *
     * Note that names referring to deleted nodes cannot be reused.
     *
     * @param nodeName identifies the node to be deleted.
     *
     * @throws MemberNotFoundException if the node denoted by
     * <code>memberName</code> is not a member of the replication group.
     *
     * @throws MasterStateException if the member being removed is currently
     * the Master
     *
     * @see <a href="https://sleepycat.oracle.com/trac/wiki/DynamicGroupMembership#DeletingMembers">Member Deletion</a>
     */
    public void removeMember(String nodeName) throws MemberNotFoundException {
        if (!nodeState.getRepEnvState().isMaster()) {
            throw EnvironmentFailureException.unexpectedState("Not currently a master. " + "removeMember() must be invoked on the node that's " + "currently the master.");
        }
        RepNodeImpl node = group.getNode(nodeName);
        if (node == null) {
            throw new MemberNotFoundException("Node:" + nodeName + "is not a member of the group:" + group.getName());
        }
        if (node.isRemoved() && node.isQuorumAck()) {
            throw new MemberNotFoundException("Node:" + nodeName + "is not currently a member of " + "the group:" + group.getName() + " It had been removed.");
        }
        if (nodeName.equals(getNodeName())) {
            throw new MasterStateException(getRepImpl().getStateChangeEvent());
        }
        node = group.removeMember(nodeName);
        feederManager.shutdownFeeder(node);
        repGroupDB.removeMember(node);
    }

    /**
     * Updates the cached group info for the node, avoiding a database read.
     *
     * @param updateNameIdPair the node whose localCBVLSN must be updated.
     * @param barrierState the new node syncup state
     */
    public void updateGroupInfo(NameIdPair updateNameIdPair, RepGroupImpl.BarrierState barrierState) {
        RepNodeImpl node = group.getMember(updateNameIdPair.getName());
        if (node == null) {
            return;
        }
        LoggerUtils.fine(logger, repImpl, "LocalCBVLSN for " + updateNameIdPair + " updated to " + barrierState + " from " + node.getBarrierState().getLastCBVLSN());
        node.setBarrierState(barrierState);
        globalCBVLSN.recalculate(group);
    }

    /**
     * Recalculate the Global CBVLSN, provoked by Replay, to ensure that the
     * replica's global CBVLSN is up to date.
     */
    void recalculateGlobalCBVLSN() {
        globalCBVLSN.recalculate(group);
    }

    LocalCBVLSNTracker getCBVLSNTracker() {
        return cbvlsnTracker;
    }

    /**
     * Finds a master node.
     *
     * @throws IOException
     * @throws DatabaseException
     * @throws InterruptedException
     */
    private void findMaster() throws IOException, DatabaseException {
        elections.startLearner();
        LoggerUtils.info(logger, repImpl, "Current group size: " + group.getElectableGroupSize());
        RepNodeImpl thisNode = group.getNode(nameIdPair.getName());
        if (thisNode == null) {
            LoggerUtils.info(logger, repImpl, "New node " + nameIdPair + " unknown to rep group");
            Set<InetSocketAddress> helperSockets = repImpl.getHelperSockets();
            if ((group.getElectableGroupSize() == 0) && (helperSockets.size() == 1) && serviceDispatcher.getSocketAddress().equals(helperSockets.iterator().next())) {
                selfElect();
                elections.updateRepGroup(group);
                return;
            }
            queryGroupForMaster();
        } else {
            if (thisNode.isRemoved()) {
                throw EnvironmentFailureException.unexpectedState("Node: " + nameIdPair.getName() + " was previously deleted.");
            }
            LoggerUtils.info(logger, repImpl, "Existing node " + nameIdPair.getName() + " querying for a current master.");
            elections.getLearner().queryForMaster(group.getLearnerSockets());
        }
    }

    /**
     * This method enforces the requirement that all addresses within a
     * replication group, must be loopback addresses or they must all be
     * non-local ip addresses. Mixing them means that the node with a loopback
     * address cannot be contacted by a different node.
     *
     * @param helperSockets the helper nodes used by this node when contacting
     * the master.
     */
    private void checkLoopbackAddresses(Set<InetSocketAddress> helperSockets) {
        final InetAddress myAddress = mySocket.getAddress();
        final boolean isLoopback = myAddress.isLoopbackAddress();
        for (InetSocketAddress socketAddress : helperSockets) {
            final InetAddress nodeAddress = socketAddress.getAddress();
            if (nodeAddress.isLoopbackAddress() == isLoopback) {
                continue;
            }
            String message = mySocket + " the address associated with this node, " + (isLoopback ? "is " : "is not ") + "a loopback address." + " It conflicts with an existing use, by a different node " + " of the address:" + socketAddress + (!isLoopback ? " which is a loopback address." : " which is not a loopback address.") + " Such mixing of addresses within a group is not allowed, " + "since the nodes will not be able to communicate with " + "each other.";
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Used by a new node (that is not a self-elected master) to identify a
     * master. A new node, one that is not as yet in group database, queries
     * the designated helpers and all known learners for the current master.
     * The helpers are the ones that were identified via the node's
     * configuration, while the learners are the ones currently in the member
     * database. It uses both to cast the widest possible net.
     */
    private void queryGroupForMaster() {
        Set<InetSocketAddress> helperSockets = repImpl.getHelperSockets();
        checkLoopbackAddresses(helperSockets);
        Set<InetSocketAddress> learners = new HashSet<InetSocketAddress>(helperSockets);
        learners.addAll(group.getLearnerSockets());
        if (learners.size() == 0) {
            throw EnvironmentFailureException.unexpectedState("Need a helper to add a new node into the group");
        }
        while (true) {
            elections.getLearner().queryForMaster(learners);
            if (masterStatus.getGroupMasterNameId().getId() != NameIdPair.NULL_NODE_ID) {
                break;
            }
            try {
                Thread.sleep(MASTER_QUERY_INTERVAL);
            } catch (InterruptedException e) {
                throw EnvironmentFailureException.unexpectedException(e);
            }
        }
        LoggerUtils.info(logger, repImpl, "New node " + nameIdPair.getName() + " located master: " + masterStatus.getGroupMasterNameId());
    }

    /**
     * Elects this node as the master. The operation is only valid when the
     * group consists of just this node.
     * @param helperLearner
     * @throws DatabaseException
     */
    private void selfElect() throws DatabaseException {
        nameIdPair.setId(RepGroupImpl.getFirstNodeId());
        Proposal proposal = new TimebasedProposalGenerator().nextProposal();
        elections.getLearner().processResult(proposal, suggestionGenerator.get(proposal));
        LoggerUtils.info(logger, repImpl, "Nascent group. " + nameIdPair.getName() + " is master by virtue of being the first node.");
        nodeState.changeAndNotify(UNKNOWN, NameIdPair.NULL);
        masterStatus.sync();
        nodeState.changeAndNotify(MASTER, masterStatus.getNodeMasterNameId());
        repImpl.getVLSNIndex().initAsMaster();
        repGroupDB.addFirstNode();
        refreshCachedGroup();
        masterStatus.unSync();
    }

    /**
     * The top level Master/Feeder or Replica loop in support of replication.
     * It's responsible for driving the node level state changes resulting
     * from elections initiated either by this node, or by other members of the
     * group.
     * <p>
     * The thread is terminated via an orderly shutdown initiated as a result
     * of an interrupt issued by the shutdown() method. Any exception that is
     * not handled by the run method itself is caught by the thread's uncaught
     * exception handler, and results in the RepImpl being made invalid.  In
     * that case, the application is responsible for closing the Replicated
     * Environment, which will provoke the shutdown.
     * <p>
     * Note: This method currently runs either the feeder loop or the replica
     * loop. With R to R support, it would be possible for a Replica to run
     * both. This will be a future feature.
     */
    @Override
    public void run() {
        if (nodeState.getRepEnvState().isDetached()) {
            nodeState.changeAndNotify(UNKNOWN, NameIdPair.NULL);
        }
        Error repNodeError = null;
        try {
            LoggerUtils.info(logger, repImpl, "Node " + nameIdPair.getName() + " started");
            while (!isShutdown()) {
                if (nodeState.getRepEnvState() != UNKNOWN) {
                    nodeState.changeAndNotify(UNKNOWN, NameIdPair.NULL);
                }
                if (masterStatus.getGroupMasterNameId().hasNullId() || masterStatus.inSync()) {
                    elections.initiateElection(group, electionQuorumPolicy);
                    electionQuorumPolicy = QuorumPolicy.SIMPLE_MAJORITY;
                    if (isShutdown()) {
                        return;
                    }
                }
                masterStatus.sync();
                MasterStatus status = (MasterStatus) masterStatus.clone();
                if (status.isNodeMaster()) {
                    repImpl.getVLSNIndex().initAsMaster();
                    replica.masterTransitionCleanup();
                    try {
                        serviceDispatcher.register(new GroupService(serviceDispatcher, this));
                        nodeState.changeAndNotify(MASTER, status.getNodeMasterNameId());
                        feederManager.runFeeders();
                    } finally {
                        serviceDispatcher.cancel(GroupService.SERVICE_NAME);
                    }
                } else {
                    nodeState.changeAndNotify(REPLICA, status.getNodeMasterNameId());
                    replica.runReplicaLoop();
                }
            }
        } catch (InterruptedException e) {
            LoggerUtils.fine(logger, repImpl, "RepNode main thread interrupted - " + " forced shutdown.");
        } catch (GroupShutdownException e) {
            saveShutdownException(e);
        } catch (RuntimeException e) {
            saveShutdownException(e);
            throw e;
        } catch (Error e) {
            repNodeError = e;
            repImpl.invalidate(e);
        } finally {
            try {
                LoggerUtils.info(logger, repImpl, "RepNode main thread shutting down.");
                if (repNodeError != null) {
                    LoggerUtils.info(logger, repImpl, "Node state at shutdown:\n" + repImpl.dumpState());
                    throw repNodeError;
                }
                Throwable exception = getSavedShutdownException();
                if (exception == null) {
                    LoggerUtils.fine(logger, repImpl, "Node state at shutdown:\n" + repImpl.dumpState());
                } else {
                    LoggerUtils.info(logger, repImpl, "RepNode shutdown exception:\n" + exception.getMessage() + repImpl.dumpState());
                }
                try {
                    shutdown();
                } catch (DatabaseException e) {
                    RepUtils.chainExceptionCause(e, exception);
                    LoggerUtils.severe(logger, repImpl, "Unexpected exception during shutdown" + e);
                    throw e;
                }
            } catch (InterruptedException e1) {
            }
            nodeState.changeAndNotify(DETACHED, NameIdPair.NULL);
            cleanup();
        }
    }

    /**
     * Used to shutdown all activity associated with this replication stream.
     * If method is invoked from different thread of control, it will wait
     * until the rep node thread exits. If it's from the same thread, it's the
     * caller's responsibility to exit the thread upon return from this method.
     *
     * @throws InterruptedException
     * @throws DatabaseException
     */
    public void shutdown() throws InterruptedException, DatabaseException {
        if (shutdownDone()) {
            return;
        }
        LoggerUtils.info(logger, repImpl, "Shutting down node " + nameIdPair);
        if (repImpl.isValid()) {
            monitorEventManager.notifyLeaveGroup(getLeaveReason());
        }
        serviceDispatcher.preShutdown();
        if (elections != null) {
            elections.shutdown();
        }
        feederManager.shutdownQueue();
        if ((getReplicaCloseCatchupMs() >= 0) && (nodeState.getRepEnvState().isMaster())) {
            this.join();
        }
        replica.shutdown();
        shutdownThread(logger);
        LoggerUtils.info(logger, repImpl, "RepNode main thread: " + this.getName() + " exited.");
        utilityServicesShutdown();
        serviceDispatcher.shutdown();
        LoggerUtils.info(logger, repImpl, nameIdPair + " shutdown completed.");
        masterStatus.setGroupMaster(null, NameIdPair.NULL);
        readyLatch.releaseAwait(getSavedShutdownException());
        timer.cancel();
    }

    /**
     * Soft shutdown for the RepNode thread. Note that since the thread is
     * shared by the FeederManager and the Replica, the FeederManager or
     * Replica specific soft shutdown actions should already have been done
     * earlier.
     */
    @Override
    protected int initiateSoftShutdown() {
        return getThreadWaitInterval();
    }

    private LeaveReason getLeaveReason() {
        LeaveReason reason = null;
        Exception exception = getSavedShutdownException();
        if (exception == null) {
            reason = LeaveReason.NORMAL_SHUTDOWN;
        } else if (exception instanceof GroupShutdownException) {
            reason = LeaveReason.MASTER_SHUTDOWN_GROUP;
        } else {
            reason = LeaveReason.ABNORMAL_TERMINATION;
        }
        return reason;
    }

    private void utilityServicesShutdown() {
        if (ldiff != null) {
            ldiff.shutdown();
        }
        if (logFeederManager != null) {
            logFeederManager.shutdown();
        }
        if (nodeStateService != null) {
            serviceDispatcher.cancel(NodeStateService.SERVICE_NAME);
        }
    }

    /**
     * Must be invoked on the Master via the last open handle.
     *
     * Note that the method itself does not shutdown the group. It merely
     * sets replicaCloseCatchupMs, indicating that the ensuing handle close
     * should shutdown the Replicas. The actual coordination with the closing
     * of the handle is implemented by ReplicatedEnvironment.shutdownGroup().
     *
     * @see ReplicatedEnvironment#shutdownGroup(long, TimeUnit)
     */
    public void shutdownGroupOnClose(long timeoutMs) throws IllegalStateException {
        if (!nodeState.getRepEnvState().isMaster()) {
            throw new IllegalStateException("Node state must be " + MASTER + ", not " + nodeState.getRepEnvState());
        }
        replicaCloseCatchupMs = (timeoutMs < 0) ? 0 : timeoutMs;
    }

    /**
     * JoinGroup returns whether this node is a MASTER or REPLICA. If that's
     * already known, it returns immediately. Otherwise it waits until a master
     * is elected this node is a functional, either as a Master, or as a
     * Replica. If it joins as a replica, it will wait until it has become
     * sufficiently consistent as defined by its argument.
     *
     * @throws IOException
     */
    public ReplicatedEnvironment.State joinGroup(ReplicaConsistencyPolicy consistency, QuorumPolicy initialElectionPolicy) throws ReplicaConsistencyException, DatabaseException, IOException {
        final int setupTimeout = getConfigManager().getDuration(ENV_SETUP_TIMEOUT);
        final long limitTime = System.currentTimeMillis() + setupTimeout;
        startup(initialElectionPolicy);
        LoggerUtils.finest(logger, repImpl, "joinGroup " + nodeState.getRepEnvState());
        DatabaseException exitException = null;
        int retries = 0;
        for (retries = 0; retries < JOIN_RETRIES; retries++) {
            try {
                boolean done = getReadyLatch().awaitOrException((limitTime - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
                ReplicatedEnvironment.State finalState = nodeState.getRepEnvState();
                if (!done) {
                    if (finalState.isReplica()) {
                        throw new ReplicaConsistencyException(String.format("Setup time exceeded %,d ms", setupTimeout), null);
                    }
                    break;
                }
                String consistencyInfo = null;
                if (finalState.isReplica()) {
                    if (consistency == null) {
                        final int consistencyTimeout = getConfigManager().getDuration(ENV_CONSISTENCY_TIMEOUT);
                        consistency = new PointConsistencyPolicy(new VLSN(replica.getMasterCommitVLSN()), consistencyTimeout, TimeUnit.MILLISECONDS);
                    }
                    consistency.ensureConsistency(repImpl);
                    consistencyInfo = "consistencyPolicy=" + consistency + " " + repImpl.getVLSNIndex().getRange();
                }
                LoggerUtils.info(logger, repImpl, "Finished joinGroup " + finalState + consistencyInfo);
                return finalState;
            } catch (InterruptedException e) {
                throw EnvironmentFailureException.unexpectedException(e);
            } catch (MasterStateException e) {
                LoggerUtils.warning(logger, repImpl, "Join retry due to master transition: " + e.getMessage());
                continue;
            } catch (RestartRequiredException e) {
                LoggerUtils.warning(logger, repImpl, "Environment needs to be restarted: " + e.getMessage());
                throw e;
            } catch (DatabaseException e) {
                Throwable cause = e.getCause();
                if ((cause != null) && (cause.getClass() == Replica.ConnectRetryException.class)) {
                    exitException = e;
                    if ((limitTime - System.currentTimeMillis()) > 0) {
                        LoggerUtils.warning(logger, repImpl, "Join retry due to exception: " + cause.getMessage());
                        continue;
                    }
                }
                throw e;
            }
        }
        if (exitException != null) {
            LoggerUtils.warning(logger, repImpl, "Exiting joinGroup after " + retries + " retries." + exitException);
            throw exitException;
        }
        throw new UnknownMasterException(null, repImpl.getStateChangeEvent());
    }

    /**
     * Should be called whenever a new VLSN is associated with a log entry
     * suitable for Replica/Feeder syncup.
     */
    public void trackSyncableVLSN(VLSN syncableVLSN, long lsn) {
        cbvlsnTracker.track(syncableVLSN, lsn);
    }

    /** May return NULL_VLSN */
    public VLSN getGroupCBVLSN() {
        return globalCBVLSN.getCBVLSN();
    }

    /**
     * Returns the number of nodes needed to form a quorum for elections
     *
     * @param quorumPolicy
     * @return the number of nodes required for a quorum
     */
    public int getElectionQuorumSize(QuorumPolicy quorumPolicy) {
        if (electableGroupSizeOverride > 0) {
            return quorumPolicy.quorumSize(electableGroupSizeOverride);
        }
        if (activePrimary && QuorumPolicy.SIMPLE_MAJORITY.equals(quorumPolicy)) {
            return 1;
        }
        return quorumPolicy.quorumSize(group.getElectableGroupSize());
    }

    /**
     * Returns the minimum number of replication nodes required to
     * implement the ReplicaAckPolicy for a given group size.
     *
     * @return the number of nodes that are needed
     */
    public int minAckNodes(ReplicaAckPolicy ackPolicy) {
        if (electableGroupSizeOverride > 0) {
            return ackPolicy.minAckNodes(electableGroupSizeOverride);
        }
        if (activePrimary && ReplicaAckPolicy.SIMPLE_MAJORITY.equals(ackPolicy)) {
            return 1;
        }
        return ackPolicy.minAckNodes(group.getElectableGroupSize());
    }

    public int minAckNodes(Durability durability) {
        return minAckNodes(durability.getReplicaAck());
    }

    /**
     * Returns the group wide CBVLSN. The group CBVLSN is computed as the
     * minimum of CBVLSNs after discarding CBVLSNs that are obsolete. A CBVLSN
     * is considered obsolete, if it has not been updated within a configurable
     * time interval relative to the time that the most recent CBVLSN was
     * updated.
     *
     * @throws DatabaseException
     */
    public void syncupStarted() {
        globalCBVLSN.syncupStarted();
    }

    public void syncupEnded() {
        globalCBVLSN.syncupEnded();
    }

    /**
     * Returns the file number that forms a barrier for the cleaner's file
     * deletion activities. Files with numbers >= this file number cannot be
     * by the cleaner without disrupting the replication stream.
     *
     * @return the file number that's the barrier for cleaner file deletion
     *
     * @throws DatabaseException
     */
    public long getCleanerBarrierFile() throws DatabaseException {
        return globalCBVLSN.getCleanerBarrierFile();
    }

    long getReplicaCloseCatchupMs() {
        return replicaCloseCatchupMs;
    }

    /**
     * Returns true if the node is a designated Primary that has been
     * activated.
     */
    public boolean isActivePrimary() {
        return activePrimary;
    }

    /**
     * Tries to activate this node as a Primary, if it has been configured as
     * such and if the group size is two. This method is invoked when an
     * operation falls short of quorum requirements and is ready to trade
     * durability for availability. More specifically it's invoked when an
     * election fails, or there is an insufficient number of replicas during
     * a begin transaction or a transaction commit.
     *
     * The Primary is passivated again when the Secondary contacts it.
     *
     * @return true if the primary was activated -- the quorum value is 1
     */
    public boolean tryActivatePrimary() {
        boolean activatedPrimary = (repImpl != null) && repImpl.isDesignatedPrimary() && getGroup().getElectableGroupSize() == 2;
        if (activatedPrimary) {
            LoggerUtils.info(logger, repImpl, "Primary activated; quorum is one.");
            activePrimary = true;
        }
        return activatedPrimary;
    }

    public final void passivatePrimary() {
        if (activePrimary) {
            LoggerUtils.info(logger, repImpl, "Primary passivated.");
        }
        activePrimary = false;
    }

    /**
     * Shuts down the Network backup service *before* a rollback is initiated
     * as part of syncup, thus ensuring that NetworkRestore does not see an
     * inconsistent set of log files. Any network backup operations that are in
     * progress at this node are aborted. The client of the service will
     * experience network connection failures and will retry with this node
     * (when the service is re-established at this node), or with some other
     * node.
     * <p>
     * restarNetworkBackup() is then used to restart the service after it was
     * shut down.
     */
    public final void shutdownNetworkBackup() {
        logFeederManager.shutdown();
        logFeederManager = null;
    }

    /**
     * Restarts the network backup service *after* a rollback has been
     * completed and the log files are once again in a consistent state.
     */
    public final void restartNetworkBackup() {
        if (logFeederManager != null) {
            throw EnvironmentFailureException.unexpectedState(repImpl);
        }
        logFeederManager = new com.sleepycat.je.rep.impl.networkRestore.FeederManager(serviceDispatcher, repImpl, nameIdPair);
    }

    public static class Clock {

        private final int skewMs;

        private Clock(int skewMs) {
            this.skewMs = skewMs;
        }

        public long currentTimeMillis() {
            return System.currentTimeMillis() + skewMs;
        }
    }

    public String dumpState() {
        return "\n" + feederManager.dumpState() + "\nGlobalCBVLSN=" + getGroupCBVLSN() + "\n" + getGroup();
    }

    public void setElectableGroupSizeOverride(int override) {
        if (electableGroupSizeOverride != override) {
            LoggerUtils.warning(logger, repImpl, "Electable group size override changed to:" + override);
        }
        this.electableGroupSizeOverride = override;
    }
}
