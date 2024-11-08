package org.ctor.dev.llrps2.mediator;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SocketChannel;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ctor.dev.llrps2.session.Rps;
import org.ctor.dev.llrps2.session.RpsCommand;
import org.ctor.dev.llrps2.session.RpsCommandParameter;
import org.ctor.dev.llrps2.session.RpsMessage;
import org.ctor.dev.llrps2.session.RpsRole;
import org.ctor.dev.llrps2.session.RpsSessionException;
import org.ctor.dev.llrps2.session.RpsState;
import org.ctor.dev.llrps2.session.SessionStub;

public class SocketSessionHandler extends SessionHandler {

    private static final Log LOG = LogFactory.getLog(SocketSessionHandler.class);

    private final SessionStub stub;

    static SocketSessionHandler create(SocketChannel channel, String sessionId) {
        Validate.notNull(channel);
        Validate.notNull(sessionId);
        return new SocketSessionHandler(channel, sessionId);
    }

    private SocketSessionHandler(SocketChannel channel, String sessionId) {
        super(sessionId);
        this.stub = new SessionStub(channel, RpsRole.AGENT);
    }

    void handle() throws RpsSessionException, ClosedByInterruptException {
        LOG.info("handling the session");
        try {
            final long retrieveSize = stub.read();
            if (retrieveSize > 0) {
                RpsMessage message = null;
                while ((message = stub.receiveMessage()) != null) {
                    handleMessage(message);
                }
                stub.flushWrite();
            } else {
                throw new RpsSessionException("0 byte read of readable channel");
            }
        } catch (ClosedByInterruptException cbie) {
            throw cbie;
        } catch (IOException ioe) {
            LOG.info(ioe.getMessage(), ioe);
            throw new RpsSessionException(ioe);
        }
    }

    private void handleMessage(RpsMessage message) throws RpsSessionException {
        switch(message.getCommand()) {
            case A_HELLO:
                receiveHello(message);
                sendInitiate();
                break;
            case A_INITIATE:
                receiveInitiate(message);
                break;
            case A_READY:
                receiveRoundReady(message);
                break;
            case A_MOVE:
                receiveMove(message);
                break;
            default:
                throw new IllegalArgumentException("unknown command: " + message.getCommand());
        }
    }

    @Override
    synchronized void connect() throws RpsSessionException {
        state.transition(RpsState.ESTABLISHED);
    }

    @Override
    synchronized void sendHello() throws RpsSessionException {
        LOG.info(String.format("[%s] sending HELLO", sessionId));
        stub.sendMessage(RpsCommand.C_HELLO);
        flushWrite();
        state.transition(RpsState.C_HELLO);
    }

    @Override
    synchronized void receiveHello(RpsMessage message) throws RpsSessionException {
        LOG.info(String.format("[%s] receiving HELLO", sessionId));
        state.transition(RpsState.A_HELLO);
    }

    @Override
    synchronized void sendInitiate() throws RpsSessionException {
        LOG.info(String.format("[%s] sending INITIATE", sessionId));
        stub.sendMessage(RpsCommand.C_INITIATE, sessionId);
        flushWrite();
        state.transition(RpsState.C_INITIATION);
    }

    @Override
    synchronized void receiveInitiate(RpsMessage message) throws RpsSessionException {
        LOG.info(String.format("[%s] receiving INITIATE", sessionId));
        checkSessionId(message);
        agentName = message.getParameter(RpsCommandParameter.AgentName);
        capacity = message.getParameter(RpsCommandParameter.Capacity);
        state.transition(RpsState.INITIATED);
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
        stub.sendMessage(RpsCommand.C_READY, sessionId, newRoundId, newIteration, newRuleId);
        flushWrite();
        state.transition(RpsState.C_ROUND_READY);
    }

    @Override
    synchronized void receiveRoundReady(RpsMessage message) throws RpsSessionException {
        LOG.info(String.format("[%s - %s] receiving READY", sessionId, getAgentName()));
        if (getRoundHandler() == null) {
            throw new IllegalStateException("roundHandler not set");
        }
        checkSessionId(message);
        checkRoundId(message);
        state.transition(RpsState.ROUND_READY);
        getRoundHandler().notifyGameReady(this);
    }

    @Override
    synchronized void sendCall() throws RpsSessionException {
        LOG.info(String.format("[%s - %s] sending CALL", sessionId, getAgentName()));
        stub.sendMessage(RpsCommand.C_CALL, sessionId, roundId);
        flushWrite();
        state.transition(RpsState.CALL);
    }

    @Override
    synchronized void receiveMove(RpsMessage message) throws RpsSessionException {
        LOG.info(String.format("[%s - %s] receiving MOVE", sessionId, getAgentName()));
        if (!inRecoveryMode && getRoundHandler() == null) {
            throw new IllegalStateException("roundHandler not set");
        }
        state.transition(RpsState.MOVE);
        checkSessionId(message);
        checkRoundId(message);
        final Rps move = Rps.parse(message.getParameter(RpsCommandParameter.Move));
        if (inRecoveryMode) {
            sendResultUpdate(Rps.NotAMove);
            sendMatch();
            inRecoveryMode = false;
        } else {
            getRoundHandler().notifyMove(this, move);
        }
    }

    @Override
    synchronized void sendResultUpdate(Rps previousOppositeMove) throws RpsSessionException {
        LOG.info(String.format("[%s - %s] sending RESULT", sessionId, getAgentName()));
        stub.sendMessage(RpsCommand.C_RESULT, sessionId, roundId, previousOppositeMove.getRepresentation());
        flushWrite();
        state.transition(RpsState.RESULT_UPDATED);
    }

    @Override
    synchronized void sendMatch() throws RpsSessionException {
        LOG.info(String.format("[%s - %s] sending MATCH", sessionId, getAgentName()));
        stub.sendMessage(RpsCommand.C_MATCH, sessionId, roundId);
        flushWrite();
        state.transition(RpsState.MATCH);
    }

    @Override
    synchronized void sendClose() throws RpsSessionException {
        LOG.info(String.format("[%s - %s] sending CLOSE", sessionId, getAgentName()));
        stub.sendMessage(RpsCommand.C_CLOSE, sessionId);
        flushWrite();
        state.transition(RpsState.C_CLOSE);
    }

    @Override
    synchronized void close() {
        LOG.info(String.format("[%s - %s] closing session", sessionId, getAgentName()));
        gracefulClose();
        try {
            stub.close();
        } catch (IOException ioe) {
            LOG.warn(ioe.getMessage(), ioe);
        }
        if (getRoundHandler() != null) {
            getRoundHandler().notifySurrender(this);
        }
    }

    private void gracefulClose() {
        LOG.info(String.format("[%s - %s] graceful close from %s", sessionId, getAgentName(), state.getState()));
        switch(state.getState()) {
            case INITIATED:
            case MATCH:
                try {
                    sendClose();
                } catch (RpsSessionException rse) {
                    LOG.warn(rse.getMessage(), rse);
                }
                break;
            case START:
            case ESTABLISHED:
            case A_HELLO:
            case C_HELLO:
            case C_INITIATION:
            case C_ROUND_READY:
            case ROUND_READY:
            case CALL:
            case MOVE:
            case RESULT_UPDATED:
            case C_CLOSE:
            case END:
                break;
            default:
                throw new IllegalArgumentException("unknown state " + state.getState());
        }
    }

    private void flushWrite() throws RpsSessionException {
        try {
            stub.flushWrite();
        } catch (IOException ioe) {
            LOG.info(ioe.getMessage(), ioe);
            throw new RpsSessionException(ioe);
        }
    }

    @Override
    boolean isConnected() {
        return getChannel().isConnected();
    }

    @Override
    SocketChannel getChannel() {
        return stub.getChannel();
    }
}
