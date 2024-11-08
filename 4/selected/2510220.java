package com.sleepycat.je.rep.impl.node;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.EnvironmentFailureException;
import com.sleepycat.je.StatsConfig;
import com.sleepycat.je.Durability.SyncPolicy;
import com.sleepycat.je.dbi.EnvironmentFailureReason;
import com.sleepycat.je.log.ChecksumException;
import com.sleepycat.je.log.LogEntryType;
import com.sleepycat.je.rep.InsufficientAcksException;
import com.sleepycat.je.rep.InsufficientReplicasException;
import com.sleepycat.je.rep.impl.RepImpl;
import com.sleepycat.je.rep.impl.RepParams;
import com.sleepycat.je.rep.stream.FeederReplicaHandshake;
import com.sleepycat.je.rep.stream.FeederReplicaSyncup;
import com.sleepycat.je.rep.stream.FeederSource;
import com.sleepycat.je.rep.stream.MasterFeederSource;
import com.sleepycat.je.rep.stream.MasterStatus;
import com.sleepycat.je.rep.stream.OutputWireRecord;
import com.sleepycat.je.rep.stream.Protocol;
import com.sleepycat.je.rep.stream.FeederReplicaSyncup.NetworkRestoreException;
import com.sleepycat.je.rep.stream.MasterStatus.MasterSyncException;
import com.sleepycat.je.rep.stream.Protocol.Ack;
import com.sleepycat.je.rep.stream.Protocol.HeartbeatResponse;
import com.sleepycat.je.rep.txn.MasterTxn;
import com.sleepycat.je.rep.utilint.BinaryProtocolStatDefinition;
import com.sleepycat.je.rep.utilint.NamedChannelWithTimeout;
import com.sleepycat.je.rep.utilint.RepUtils;
import com.sleepycat.je.rep.utilint.BinaryProtocol.Message;
import com.sleepycat.je.rep.vlsn.VLSNRange;
import com.sleepycat.je.utilint.LoggerUtils;
import com.sleepycat.je.utilint.StatGroup;
import com.sleepycat.je.utilint.StoppableThread;
import com.sleepycat.je.utilint.VLSN;

/**
 * There is an instance of a Feeder for each client that needs a replication
 * stream. Either a master, or replica (providing feeder services) may
 * establish a feeder.
 *
 * A feeder is created in response to a request from a Replica, and is shutdown
 * either upon loss of connectivity, or upon a change in mastership.
 *
 * The protocol used to validate and negotiate a connection is synchronous, but
 * once this phase has been completed, the communication between the feeder and
 * replica is asynchronous. To handle the async communications, the feeder has
 * two threads associated with it:
 *
 * 1) An output thread whose sole purpose is to pump log records (and if
 * necessary heart beat requests) down to the replica as fast as the network
 * will allow it
 *
 * 2) An input thread that listens for responses to transaction commits and
 * heart beat responses.
 */
public final class Feeder {

    private int heartbeatInterval;

    private final FeederManager feederManager;

    private final RepNode repNode;

    private final RepImpl repImpl;

    private final NamedChannelWithTimeout feederReplicaChannel;

    private final InputThread inputThread;

    private final OutputThread outputThread;

    private final FeederSource feederSource;

    private int protocolVersion;

    private VLSN feederVLSN;

    @SuppressWarnings("unused")
    private volatile long lastResponseTime = 0l;

    private final MasterStatus masterStatus;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    private final Logger logger;

    private final NameIdPair nameIdPair;

    private NameIdPair replicaNameIdPair = NameIdPair.NULL;

    private static int TRANSFER_LOGGING_THRESHOLD_MS = 1000;

    /**
     * Returns a configured SocketChannel
     *
     * @param channel the channel to be configured
     * @return the configured SocketChannel
     * @throws IOException
     */
    private NamedChannelWithTimeout configureChannel(SocketChannel channel) throws IOException {
        try {
            channel.configureBlocking(true);
            LoggerUtils.info(logger, repImpl, "Feeder accepted connection from " + channel);
            final int timeoutMs = repNode.getConfigManager().getDuration(RepParams.PRE_HEARTBEAT_TIMEOUT);
            channel.socket().setTcpNoDelay(true);
            return new NamedChannelWithTimeout(repNode, channel, timeoutMs);
        } catch (IOException e) {
            LoggerUtils.warning(logger, repImpl, "IO exception while configuring channel " + "Exception:" + e.getMessage());
            throw e;
        }
    }

    Feeder(FeederManager feederManager, SocketChannel socketChannel) throws DatabaseException, IOException {
        this.feederManager = feederManager;
        this.repNode = feederManager.repNode();
        this.repImpl = repNode.getRepImpl();
        this.masterStatus = repNode.getMasterStatus();
        nameIdPair = repNode.getNameIdPair();
        this.feederSource = new MasterFeederSource(repNode.getRepImpl(), repNode.getVLSNIndex(), nameIdPair);
        logger = LoggerUtils.getLogger(getClass());
        this.feederReplicaChannel = configureChannel(socketChannel);
        inputThread = new InputThread(repNode.getRepImpl());
        outputThread = new OutputThread(repNode.getRepImpl());
        heartbeatInterval = feederManager.repNode().getHeartbeatInterval();
    }

    void startFeederThreads() {
        inputThread.start();
    }

    /**
     * @hidden
     * Place holder Feeder for testing only
     */
    public Feeder() {
        feederManager = null;
        repNode = null;
        repImpl = null;
        masterStatus = null;
        feederSource = null;
        feederReplicaChannel = null;
        nameIdPair = NameIdPair.NULL;
        logger = LoggerUtils.getLoggerFixedPrefix(getClass(), "TestFeeder");
        inputThread = null;
        outputThread = null;
        shutdown.set(true);
    }

    public StatGroup getProtocolStats(StatsConfig config) {
        final Protocol protocol = outputThread.protocol;
        return (protocol != null) ? protocol.getStats(config) : new StatGroup(BinaryProtocolStatDefinition.GROUP_NAME, BinaryProtocolStatDefinition.GROUP_DESC);
    }

    void resetStats() {
        final Protocol protocol = outputThread.protocol;
        if (protocol != null) {
            protocol.resetStats();
        }
    }

    /**
     * Returns the channel between this Feeder and its Replica.
     *
     * @return the channel
     */
    public SocketChannel getFeederReplicaChannel() {
        return feederReplicaChannel.getChannel();
    }

    public RepNode getRepNode() {
        return repNode;
    }

    public NameIdPair getReplicaNameIdPair() {
        return replicaNameIdPair;
    }

    /**
     * Shutdown the feeder, closing its channel and releasing its threads.  May
     * be called internally upon noticing a problem, or externally when the
     * RepNode is shutting down.
     */
    void shutdown(Exception shutdownException) {
        boolean changed = shutdown.compareAndSet(false, true);
        if (!changed) {
            return;
        }
        feederManager.removeFeeder(this);
        StatGroup pstats = (inputThread.protocol != null) ? inputThread.protocol.getStats(StatsConfig.DEFAULT) : new StatGroup(BinaryProtocolStatDefinition.GROUP_NAME, BinaryProtocolStatDefinition.GROUP_DESC);
        if (outputThread.protocol != null) {
            pstats.addAll(outputThread.protocol.getStats(StatsConfig.DEFAULT));
        }
        feederManager.incStats(pstats);
        LoggerUtils.info(logger, repImpl, "Shutting down feeder for replica " + replicaNameIdPair.getName() + ((shutdownException == null) ? "" : (" Reason: " + shutdownException.getMessage())) + RepUtils.writeTimesString(pstats));
        if (repNode.getReplicaCloseCatchupMs() >= 0) {
            try {
                inputThread.join();
            } catch (InterruptedException e) {
                LoggerUtils.warning(logger, repImpl, "Interrupted while waiting to join " + "thread:" + outputThread);
            }
        }
        outputThread.shutdownThread(logger);
        inputThread.shutdownThread(logger);
        LoggerUtils.finest(logger, repImpl, feederReplicaChannel + " isOpen=" + feederReplicaChannel.getChannel().isOpen());
    }

    public boolean isShutdown() {
        return shutdown.get();
    }

    /**
     * Implements the thread responsible for processing the responses from a
     * Replica.
     */
    private class InputThread extends StoppableThread {

        Protocol protocol = null;

        private LocalCBVLSNUpdater replicaCBVLSN;

        InputThread(RepImpl repImpl) {
            super(repImpl, new IOThreadsHandler());
        }

        /**
         * Does the initial negotiation to validate replication group wide
         * consistency and establish the starting VLSN. It then starts up the
         * Output thread and enters the response loop.
         */
        @Override
        public void run() {
            Thread.currentThread().setName("Feeder Input ");
            Error feederInputError = null;
            Exception shutdownException = null;
            try {
                FeederReplicaHandshake handshake = new FeederReplicaHandshake(repNode, Feeder.this, feederReplicaChannel);
                try {
                    protocol = handshake.execute();
                } catch (InsufficientReplicasException e) {
                    return;
                } catch (InsufficientAcksException e) {
                    return;
                }
                protocolVersion = protocol.getVersion();
                replicaNameIdPair = handshake.getReplicaNameIdPair();
                Thread.currentThread().setName("Feeder Input for " + replicaNameIdPair.getName());
                FeederReplicaSyncup syncup = new FeederReplicaSyncup(Feeder.this, feederReplicaChannel, protocol);
                this.replicaCBVLSN = new LocalCBVLSNUpdater(replicaNameIdPair, repNode);
                feederVLSN = syncup.execute(replicaCBVLSN);
                feederSource.init(feederVLSN);
                outputThread.start();
                lastResponseTime = System.currentTimeMillis();
                masterStatus.assertSync();
                feederManager.activateFeeder(Feeder.this);
                runResponseLoop();
            } catch (NetworkRestoreException e) {
                LoggerUtils.info(logger, repImpl, e.getMessage());
            } catch (IOException e) {
                shutdownException = e;
            } catch (MasterSyncException e) {
                shutdownException = e;
            } catch (InterruptedException e) {
                shutdownException = e;
            } catch (ExitException e) {
                LoggerUtils.fine(logger, repImpl, "Exiting feeder loop: " + e.getMessage());
            } catch (Error e) {
                feederInputError = e;
                repNode.getRepImpl().invalidate(e);
            } catch (ChecksumException e) {
                shutdownException = e;
                throw new EnvironmentFailureException(repNode.getRepImpl(), EnvironmentFailureReason.LOG_CHECKSUM, e);
            } catch (RuntimeException e) {
                shutdownException = e;
                LoggerUtils.severe(logger, repImpl, "Unexpected exception: " + e.getMessage() + LoggerUtils.getStackTrace(e));
                throw e;
            } finally {
                if (feederInputError != null) {
                    throw feederInputError;
                }
                shutdown(shutdownException);
                cleanup();
            }
        }

        private void runResponseLoop() throws IOException, MasterSyncException {
            while (!checkShutdown()) {
                Message response = protocol.read(feederReplicaChannel);
                if (checkShutdown()) {
                    break;
                }
                masterStatus.assertSync();
                lastResponseTime = System.currentTimeMillis();
                if (response.getOp() == Protocol.HEARTBEAT_RESPONSE) {
                    HeartbeatResponse hbResponse = (Protocol.HeartbeatResponse) response;
                    replicaCBVLSN.updateForReplica(hbResponse);
                    continue;
                } else if (response.getOp() == Protocol.ACK) {
                    long txnId = ((Ack) response).getTxnId();
                    if (logger.isLoggable(Level.FINE)) {
                        LoggerUtils.fine(logger, repImpl, "Ack for: " + txnId);
                    }
                    repNode.getFeederTxns().noteReplicaAck(txnId);
                    continue;
                } else if (response.getOp() == Protocol.SHUTDOWN_RESPONSE) {
                    LoggerUtils.info(logger, repImpl, "Shutdown confirmed by replica " + replicaNameIdPair.getName());
                    break;
                } else {
                    throw EnvironmentFailureException.unexpectedState("Unexpected message: " + response);
                }
            }
        }

        private boolean checkShutdown() {
            return shutdown.get() && (repNode.getReplicaCloseCatchupMs() < 0);
        }

        @Override
        protected int initiateSoftShutdown() {
            RepUtils.shutdownChannel(feederReplicaChannel);
            return repNode.getThreadWaitInterval();
        }

        @Override
        protected Logger getLogger() {
            return logger;
        }
    }

    private static long sprayAfterNMessagesCount = 0;

    public static void setSprayAfterNMessagesCount(long sANMC) {
        sprayAfterNMessagesCount = sANMC;
    }

    /**
     * Simply pumps out log entries as rapidly as it can.
     */
    private class OutputThread extends StoppableThread {

        private long lastHeartbeat = 0l;

        Protocol protocol = null;

        private long totalTransferDelay = 0;

        private long shutdownRequestStart = 0;

        private final RepImpl threadRepImpl;

        OutputThread(RepImpl repImpl) {
            super(repImpl, new IOThreadsHandler());
            this.threadRepImpl = repImpl;
        }

        /**
         * Determines whether we should exit the output loop. If we are trying
         * to shutdown the Replica cleanly, that is, this is a group shutdown,
         * the method delays the shutdown until the Replica has had a chance
         * to catch up to the current commit VLSN on this node, after which
         * it sends the Replica a Shutdown message.
         *
         * @return true if the output thread should be shutdown.
         *
         * @throws IOException
         */
        private boolean checkShutdown() throws IOException {
            if (!shutdown.get()) {
                return false;
            }
            if (repNode.getReplicaCloseCatchupMs() >= 0) {
                if (shutdownRequestStart == 0) {
                    shutdownRequestStart = System.currentTimeMillis();
                }
                boolean timedOut = (System.currentTimeMillis() - shutdownRequestStart) > repNode.getReplicaCloseCatchupMs();
                if (!timedOut && (feederVLSN.compareTo(repNode.getCurrentCommitVLSN()) < 0)) {
                    return false;
                }
                protocol.write(protocol.new ShutdownRequest(shutdownRequestStart), feederReplicaChannel);
                String shutdownMessage = String.format("Shutdown message sent to: %s " + " Shutdown elapsed time: %,dms", replicaNameIdPair, (System.currentTimeMillis() - shutdownRequestStart));
                LoggerUtils.info(logger, threadRepImpl, shutdownMessage);
                return true;
            }
            return true;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("Feeder Output ");
            protocol = Protocol.get(repNode, protocolVersion);
            Thread.currentThread().setName("Feeder Output for " + Feeder.this.getReplicaNameIdPair().getName());
            final int testDelayMs = feederManager.getTestDelayMs();
            if (testDelayMs > 0) {
                LoggerUtils.info(logger, threadRepImpl, "Test delay of:" + testDelayMs + "ms." + " after each message sent");
            }
            {
                VLSNRange range = repNode.getVLSNIndex().getRange();
                LoggerUtils.info(logger, threadRepImpl, String.format("Feeder output thread for replica %s started at " + "VLSN %,d master at %,d VLSN delta=%,d socket=%s", replicaNameIdPair.getName(), feederVLSN.getSequence(), range.getLast().getSequence(), range.getLast().getSequence() - feederVLSN.getSequence(), feederReplicaChannel));
            }
            Error feederOutputError = null;
            Exception shutdownException = null;
            try {
                sendHeartbeat();
                final int timeoutMs = repNode.getConfigManager().getDuration(RepParams.FEEDER_TIMEOUT);
                feederReplicaChannel.setTimeoutMs(timeoutMs);
                while (!checkShutdown()) {
                    if (feederVLSN.compareTo(repNode.getCurrentCommitVLSN()) >= 0) {
                        repNode.passivatePrimary();
                    }
                    OutputWireRecord record = feederSource.getWireRecord(feederVLSN, heartbeatInterval);
                    masterStatus.assertSync();
                    if (record == null) {
                        sendHeartbeat();
                    } else {
                        Message entry = createMessage(record);
                        validate(record);
                        maybeSpray(entry, record);
                        protocol.write(entry, feederReplicaChannel);
                        sendHeartbeat();
                        feederVLSN = feederVLSN.getNext();
                    }
                    if (testDelayMs > 0) {
                        Thread.sleep(testDelayMs);
                    }
                }
            } catch (IOException e) {
                shutdownException = e;
            } catch (MasterSyncException e) {
                shutdownException = e;
            } catch (InterruptedException e) {
                shutdownException = e;
            } catch (RuntimeException e) {
                shutdownException = e;
                LoggerUtils.severe(logger, threadRepImpl, "Unexpected exception: " + e.getMessage() + LoggerUtils.getStackTrace(e));
                throw e;
            } catch (Error e) {
                feederOutputError = e;
                repNode.getRepImpl().invalidate(e);
            } finally {
                if (feederOutputError != null) {
                    throw feederOutputError;
                }
                LoggerUtils.info(logger, threadRepImpl, "Feeder output for replica " + replicaNameIdPair.getName() + " shutdown. feeder VLSN: " + feederVLSN + " currentCommitVLSN: " + repNode.getCurrentCommitVLSN());
                shutdown(shutdownException);
                cleanup();
            }
        }

        final void maybeSpray(Message entry, OutputWireRecord record) throws IOException {
            if (--sprayAfterNMessagesCount == 0) {
                if (record.getEntryType() != LogEntryType.LOG_LN_TRANSACTIONAL.getTypeNum()) {
                    sprayAfterNMessagesCount++;
                    return;
                }
                System.out.println("Initiating message spray: " + entry);
                while (true) {
                    protocol.write(entry, feederReplicaChannel);
                }
            }
        }

        /**
         * Sends a heartbeat message, if we have exceeded the heartbeat
         * interval.
         *
         * @param protocol protocol to use for the heartbeat.
         *
         * @throws IOException
         */
        private void sendHeartbeat() throws IOException {
            long now = System.currentTimeMillis();
            long interval = now - lastHeartbeat;
            if (interval <= heartbeatInterval) {
                return;
            }
            VLSN vlsn = repNode.getCurrentCommitVLSN();
            protocol.write(protocol.new Heartbeat(now, vlsn.getSequence()), feederReplicaChannel);
            lastHeartbeat = now;
        }

        @Override
        protected int initiateSoftShutdown() {
            RepUtils.shutdownChannel(feederReplicaChannel);
            return repNode.getThreadWaitInterval();
        }

        /**
         * Converts a log entry into a specific Message to be sent out by the
         * Feeder.
         *
         * @param logBytes the bytes representing the log entry
         *
         * @return the Message representing the entry
         *
         * @throws DatabaseException
         */
        private Message createMessage(OutputWireRecord wireRecord) throws DatabaseException {
            long txnId = wireRecord.getCommitTxnId();
            if (txnId != 0) {
                MasterTxn ackTxn = repNode.getFeederTxns().getAckTxn(txnId);
                if (ackTxn != null) {
                    ackTxn.stampRepWriteTime();
                    long messageTransferMs = ackTxn.messageTransferMs();
                    totalTransferDelay += messageTransferMs;
                    if (messageTransferMs > TRANSFER_LOGGING_THRESHOLD_MS) {
                        LoggerUtils.info(logger, threadRepImpl, (String.format("Feeder for: %s, Txn: %,d " + " log to rep stream time %,dms." + " Total transfer time: %,dms.", replicaNameIdPair.getName(), txnId, messageTransferMs, totalTransferDelay)));
                    }
                }
                SyncPolicy replicaSync = (ackTxn != null) ? ackTxn.getCommitDurability().getReplicaSync() : SyncPolicy.NO_SYNC;
                return protocol.new Commit((ackTxn != null), replicaSync, wireRecord);
            }
            return protocol.new Entry(wireRecord);
        }

        /**
         * Sanity check the outgoing record.
         */
        private void validate(OutputWireRecord record) {
            if (!record.getVLSN().equals(feederVLSN)) {
                throw EnvironmentFailureException.unexpectedState("Expected VLSN:" + feederVLSN + " log entry VLSN:" + record.getVLSN());
            }
            if (!threadRepImpl.isConverted()) {
                assert record.verifyNegativeSequences("node=" + nameIdPair);
            }
        }

        @Override
        protected Logger getLogger() {
            return logger;
        }
    }

    /**
     * Defines the handler for the RepNode thread. The handler invalidates the
     * environment by ensuring that an EnvironmentFailureException is in place.
     *
     * The handler communicates the cause of the exception back to the
     * FeederManager's thread by setting the repNodeShutdownException and then
     * interrupting the FM thread. The FM thread upon handling the interrupt
     * notices the exception and propagates it out in turn to other threads
     * that might be coordinating activities with it.
     */
    private class IOThreadsHandler implements UncaughtExceptionHandler {

        public void uncaughtException(Thread t, Throwable e) {
            LoggerUtils.severe(logger, repImpl, "Uncaught exception in feeder thread " + t + e.getMessage() + LoggerUtils.getStackTrace(e));
            feederManager.setRepNodeShutdownException(EnvironmentFailureException.promote(repNode.getRepImpl(), EnvironmentFailureReason.UNCAUGHT_EXCEPTION, "Uncaught exception in feeder thread:" + t, e));
            repNode.interrupt();
        }
    }

    /**
     * A marker exception that wraps the real exception. It indicates that the
     * wrapped exception was sufficient cause to exit the Feeder, but not the
     * RepNode.
     */
    @SuppressWarnings("serial")
    public static class ExitException extends Exception {

        public ExitException(String message) {
            super(message);
        }

        public ExitException(Throwable cause) {
            super(cause);
        }
    }

    /**  For debugging and exception messages. */
    public String dumpState() {
        return "feederVLSN=" + feederVLSN;
    }
}
