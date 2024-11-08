package org.ctor.dev.llrps2.mediator;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ctor.dev.llrps2.session.Rps;
import org.ctor.dev.llrps2.session.RpsMessage;
import org.ctor.dev.llrps2.session.RpsSessionException;
import org.ctor.dev.llrps2.session.RpsState;

public class DecoySessionHandler extends SessionHandler {

    private static final Log LOG = LogFactory.getLog(DecoySessionHandler.class);

    private final StuffAgent strategy;

    private final List<Integer> myMoveStack = new ArrayList<Integer>();

    private final List<Integer> enemyMoveStack = new ArrayList<Integer>();

    static DecoySessionHandler create(int strategyTypeNum, String sessionId) {
        Validate.notNull(sessionId);
        return new DecoySessionHandler(strategyTypeNum, sessionId);
    }

    private DecoySessionHandler(int strategyTypeNum, String sessionId) {
        super(sessionId);
        strategy = new StuffAgent(strategyTypeNum);
    }

    @Override
    synchronized void connect() throws RpsSessionException {
        state.transition(RpsState.ESTABLISHED);
    }

    @Override
    synchronized void sendHello() throws RpsSessionException {
        LOG.info(String.format("[%s] sending HELLO", sessionId));
        state.transition(RpsState.C_HELLO);
    }

    @Override
    synchronized void receiveHello(RpsMessage message) throws RpsSessionException {
        throw new IllegalStateException("should not be called");
    }

    @Override
    synchronized void sendInitiate() throws RpsSessionException {
        LOG.info(String.format("[%s] sending INITIATE", sessionId));
        state.transition(RpsState.C_INITIATION);
        LOG.info(String.format("[%s] act as receiving INITIATE", sessionId));
        agentName = strategy.getAgentName();
        capacity = strategy.getCapacity();
        state.transition(RpsState.INITIATED);
    }

    @Override
    synchronized void receiveInitiate(RpsMessage message) throws RpsSessionException {
        throw new IllegalStateException("should not be called");
    }

    @Override
    synchronized void sendRoundReady(String newRoundId, String newIteration, String newRuleId) throws RpsSessionException {
        LOG.info(String.format("[%s - %s] sending READY", sessionId, getAgentName()));
        Validate.notNull(newRoundId);
        Validate.notNull(newIteration);
        Validate.notNull(newRuleId);
        this.roundId = newRoundId;
        this.iteration = newIteration;
        this.ruleId = newRuleId;
        state.transition(RpsState.C_ROUND_READY);
        myMoveStack.clear();
        enemyMoveStack.clear();
        LOG.info(String.format("[%s - %s] act as receiving READY", sessionId, getAgentName()));
        if (getRoundHandler() == null) {
            throw new IllegalStateException("roundHandler not set");
        }
        state.transition(RpsState.ROUND_READY);
        getRoundHandler().notifyGameReady(this);
    }

    @Override
    synchronized void receiveRoundReady(RpsMessage message) throws RpsSessionException {
        throw new IllegalStateException("should not be called");
    }

    @Override
    synchronized void sendCall() throws RpsSessionException {
        LOG.info(String.format("[%s - %s] sending CALL", sessionId, getAgentName()));
        state.transition(RpsState.CALL);
        LOG.info(String.format("[%s - %s] act as receiving MOVE", sessionId, getAgentName()));
        if (!inRecoveryMode && getRoundHandler() == null) {
            throw new IllegalStateException("roundHandler not set");
        }
        state.transition(RpsState.MOVE);
        final Rps move = Rps.parse(String.valueOf(strategy.getMove(myMoveStack, enemyMoveStack)));
        getRoundHandler().notifyMove(this, move);
    }

    @Override
    synchronized void receiveMove(RpsMessage message) throws RpsSessionException {
        throw new IllegalStateException("should not be called");
    }

    @Override
    synchronized void sendResultUpdate(Rps previousOppositeMove) throws RpsSessionException {
        LOG.info(String.format("[%s - %s] sending RESULT", sessionId, getAgentName()));
        enemyMoveStack.add(0, Integer.parseInt(previousOppositeMove.getRepresentation()));
        state.transition(RpsState.RESULT_UPDATED);
    }

    @Override
    synchronized void sendMatch() throws RpsSessionException {
        LOG.info(String.format("[%s - %s] sending MATCH", sessionId, getAgentName()));
        state.transition(RpsState.MATCH);
    }

    @Override
    synchronized void sendClose() throws RpsSessionException {
        LOG.info(String.format("[%s - %s] sending CLOSE", sessionId, getAgentName()));
        state.transition(RpsState.C_CLOSE);
    }

    @Override
    synchronized void close() {
        LOG.info(String.format("[%s - %s] closing session", sessionId, getAgentName()));
        if (getRoundHandler() != null) {
            getRoundHandler().notifySurrender(this);
        }
    }

    @Override
    boolean isConnected() {
        return true;
    }

    @Override
    SocketChannel getChannel() {
        return null;
    }
}
