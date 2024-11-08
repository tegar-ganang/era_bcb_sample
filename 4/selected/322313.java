package org.ctor.dev.llrps2.mediator;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ctor.dev.llrps2.message.CloseMessage;
import org.ctor.dev.llrps2.message.RoundMessage;
import org.ctor.dev.llrps2.model.DateTimeMapper;
import org.ctor.dev.llrps2.session.RpsSessionException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Mediator {

    private static final Log LOG = LogFactory.getLog(Mediator.class);

    private AgentEnrollmentManager agentEnrollmentManager = null;

    private RoundMediationManager roundMediationManager = null;

    private SessionFactory sessionFactory = null;

    private int connectionScanInterleaveMsec = 2000;

    private int maxConnectionsForAgent = 3;

    private Selector selector = null;

    private int sessionCounter = 0;

    private boolean scanNext = false;

    private boolean closeNext = false;

    private Map<SocketChannel, SocketSessionHandler> handlerMap = new HashMap<SocketChannel, SocketSessionHandler>();

    public static void main(String[] args) throws IOException {
        final ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:org/ctor/dev/llrps2/mediator/applicationContext.xml");
        final Mediator mgr = (Mediator) ctx.getBean("mediator");
        mgr.start();
    }

    void start() throws IOException {
        LOG.info("mediator started");
        selector = Selector.open();
        while (true) {
            try {
                selectAndProcess();
            } catch (ClosedByInterruptException cbie) {
                LOG.info(cbie.getMessage());
                LOG.debug(cbie.getMessage(), cbie);
                break;
            } catch (IOException ie) {
                LOG.info(ie.getMessage(), ie);
            } catch (RpsSessionException rse) {
                LOG.info(rse.getMessage(), rse);
            }
        }
        LOG.info("mediator stopped");
    }

    void notifyAgentEnrollmentRequest() {
    }

    void notifyRoundMediationRequest() {
    }

    void notifyCloseRequest(CloseMessage message) {
        closeNext = true;
    }

    void notifyScan() {
        scanNext = true;
    }

    private void selectAndProcess() throws IOException, RpsSessionException {
        final int keys = selector.select(connectionScanInterleaveMsec);
        if (keys == 0) {
            scan();
            if (closeNext) {
                close();
                closeNext = false;
            }
            return;
        }
        LOG.info(String.format("selected %d keys", keys));
        final Iterator<SelectionKey> ite = selector.selectedKeys().iterator();
        while (ite.hasNext()) {
            final SelectionKey key = ite.next();
            if (!key.isReadable()) {
                throw new IllegalStateException();
            }
            ite.remove();
            final SocketChannel channel = (SocketChannel) key.channel();
            final SocketSessionHandler handler = getHandler(channel);
            try {
                handler.handle();
            } catch (RpsSessionException rse) {
                handler.close();
                removeHandler(channel);
                throw rse;
            }
        }
        if (scanNext) {
            scan();
            scanNext = false;
        }
        if (closeNext) {
            close();
            closeNext = false;
        }
    }

    private void scan() {
        scanConnection();
        scanRound();
    }

    private void close() {
        LOG.info("close all sessions");
        for (SessionHandler handler : handlerMap.values()) {
            handler.close();
        }
        handlerMap.clear();
    }

    private void scanConnection() {
        if (agentEnrollmentManager.getAgents().size() == 0) {
            return;
        }
        boolean added = false;
        for (EnrolledAgent agent : agentEnrollmentManager.getAgents()) {
            final int connections = agent.connections();
            if (connections >= maxConnectionsForAgent) {
                continue;
            }
            LOG.debug("trying to create new connection for " + agent.getAgent());
            try {
                final SessionHandler handler = sessionFactory.create(agent, newSessionId());
                if (handler == null) {
                    LOG.debug("cannot create");
                } else {
                    final SocketChannel channel = handler.getChannel();
                    if (channel != null) {
                        channel.configureBlocking(false);
                        channel.register(selector, SelectionKey.OP_READ);
                        assignHandler(channel, (SocketSessionHandler) handler);
                    }
                    agent.pushbackSession(handler);
                    added = true;
                }
            } catch (RpsSessionException rse) {
                LOG.warn(rse.getMessage(), rse);
            } catch (IOException ioe) {
                LOG.warn(ioe.getMessage(), ioe);
            }
        }
        if (added) {
            agentEnrollmentManager.notifyConnectedAgents();
        }
    }

    private void scanRound() {
        if (roundMediationManager.getRounds().size() == 0) {
            return;
        }
        LOG.info("scanning rounds...");
        for (RoundMessage round : roundMediationManager.getRounds()) {
            if (!round.isAssigned()) {
                assignRound(round);
            }
        }
    }

    private void assignRound(RoundMessage round) {
        LOG.info("trying to assign a round: " + round);
        final EnrolledAgent leftAgent = agentEnrollmentManager.findAgent(round.getLeft());
        if (leftAgent == null) {
            throw new IllegalStateException("leftagent not enrolled: " + round.getLeft());
        }
        final EnrolledAgent rightAgent = agentEnrollmentManager.findAgent(round.getRight());
        if (rightAgent == null) {
            throw new IllegalStateException("right agent not enrolled: " + round.getRight());
        }
        final SessionHandler leftSession = pollSession(leftAgent);
        final SessionHandler rightSession = pollSession(rightAgent);
        if (leftSession == null) {
            LOG.info("no valid connection found for left agent");
            if (rightSession != null) {
                rightAgent.pushbackSession(rightSession);
            }
            return;
        } else if (rightSession == null) {
            LOG.info("no valid connection found for right agent");
            if (leftSession != null) {
                leftAgent.pushbackSession(leftSession);
            }
            return;
        }
        if (!leftSession.isReadyForRoundStart() || !rightSession.isReadyForRoundStart()) {
            if (leftSession.isReadyForRoundStart()) {
                leftAgent.pushfrontSession(leftSession);
            } else {
                leftAgent.pushbackSession(leftSession);
                LOG.info("leftSession not ready (state " + leftSession.getState() + ")");
            }
            if (rightSession.isReadyForRoundStart()) {
                rightAgent.pushfrontSession(rightSession);
            } else {
                rightAgent.pushbackSession(rightSession);
                LOG.info("rightSession not ready (state " + rightSession.getState() + ")");
            }
            return;
        }
        if (leftAgent.getAgent().isDecoy() && rightAgent.getAgent().isDecoy()) {
            final String now = DateTimeMapper.modelToMessage(new GregorianCalendar());
            round.setStartDateTime(now);
            round.setFinishDateTime(now);
            getRoundMediationManager().notifyRoundResult(round);
            leftAgent.pushbackSession(leftSession);
            rightAgent.pushbackSession(rightSession);
            return;
        }
        try {
            MediationRoundHandler.create(roundMediationManager, round, leftSession, rightSession);
            round.setAssigned(true);
            LOG.info("assigned a new round");
            leftAgent.pushbackSession(leftSession);
            rightAgent.pushbackSession(rightSession);
        } catch (RpsSessionException rse) {
            LOG.warn("RoundHandler create failed");
            leftSession.close();
            rightSession.close();
        }
    }

    private SessionHandler pollSession(EnrolledAgent agent) {
        SessionHandler session = null;
        while ((session = agent.pollSession()) != null) {
            if (session.isConnected()) {
                return session;
            }
            LOG.info("fail: polled session was closed");
        }
        return null;
    }

    private String newSessionId() {
        sessionCounter += 1;
        return String.format("S_%d", sessionCounter);
    }

    private void assignHandler(SocketChannel channel, SocketSessionHandler handler) {
        handlerMap.put(channel, handler);
    }

    private void removeHandler(SocketChannel channel) {
        handlerMap.remove(channel);
    }

    private SocketSessionHandler getHandler(SocketChannel channel) {
        return handlerMap.get(channel);
    }

    public void setAgentEnrollmentManager(AgentEnrollmentManager agentEnrollmentManager) {
        this.agentEnrollmentManager = agentEnrollmentManager;
    }

    public AgentEnrollmentManager getAgentEnrollmentManager() {
        return agentEnrollmentManager;
    }

    public void setRoundMediationManager(RoundMediationManager roundMediationManager) {
        this.roundMediationManager = roundMediationManager;
    }

    public RoundMediationManager getRoundMediationManager() {
        return roundMediationManager;
    }

    public void setSessionFactory(SessionFactory channelManager) {
        this.sessionFactory = channelManager;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setConnectionScanInterleaveMsec(int connectionScanInterleaveMsec) {
        this.connectionScanInterleaveMsec = connectionScanInterleaveMsec;
    }

    public int getConnectionScanInterleaveMsec() {
        return connectionScanInterleaveMsec;
    }

    public void setMaxConnectionsForAgent(int maxConnectionsForPassiveAgent) {
        this.maxConnectionsForAgent = maxConnectionsForPassiveAgent;
    }

    public int getMaxConnectionsForAgent() {
        return maxConnectionsForAgent;
    }
}
