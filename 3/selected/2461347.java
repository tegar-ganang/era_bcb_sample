package navigators.smart.tom.server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import navigators.smart.statemanagment.ApplicationState;
import navigators.smart.tom.MessageContext;
import navigators.smart.tom.server.defaultservices.DefaultApplicationState;
import navigators.smart.tom.server.defaultservices.StateLog;
import navigators.smart.tom.util.Logger;

/**
 *
 * @author Joao Sousa
 */
public abstract class DefaultRecoverable implements Recoverable, BatchExecutable {

    public static final int CHECKPOINT_PERIOD = 50;

    private ReentrantLock logLock = new ReentrantLock();

    private ReentrantLock hashLock = new ReentrantLock();

    private ReentrantLock stateLock = new ReentrantLock();

    private MessageDigest md;

    private StateLog log;

    public DefaultRecoverable() {
        log = new StateLog(CHECKPOINT_PERIOD);
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            java.util.logging.Logger.getLogger(DefaultRecoverable.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public byte[][] executeBatch(byte[][] commands, MessageContext[] msgCtxs) {
        int eid = msgCtxs[0].getConsensusId();
        stateLock.lock();
        byte[][] replies = executeBatch2(commands, msgCtxs);
        stateLock.unlock();
        if ((eid > 0) && ((eid % CHECKPOINT_PERIOD) == 0)) {
            Logger.println("(DefaultRecoverable.executeBatch) Performing checkpoint for consensus " + eid);
            stateLock.lock();
            byte[] snapshot = getSnapshot();
            stateLock.unlock();
            saveState(snapshot, eid, 0, 0);
        } else {
            Logger.println("(DefaultRecoverable.executeBatch) Storing message batch in the state log for consensus " + eid);
            saveCommands(commands, eid, 0, 0);
        }
        return replies;
    }

    public final byte[] computeHash(byte[] data) {
        byte[] ret = null;
        hashLock.lock();
        ret = md.digest(data);
        hashLock.unlock();
        return ret;
    }

    private StateLog getLog() {
        return log;
    }

    public void saveState(byte[] snapshot, int lastEid, int decisionRound, int leader) {
        StateLog thisLog = getLog();
        logLock.lock();
        Logger.println("(TOMLayer.saveState) Saving state of EID " + lastEid + ", round " + decisionRound + " and leader " + leader);
        thisLog.newCheckpoint(snapshot, computeHash(snapshot));
        thisLog.setLastEid(-1);
        thisLog.setLastCheckpointEid(lastEid);
        thisLog.setLastCheckpointRound(decisionRound);
        thisLog.setLastCheckpointLeader(leader);
        logLock.unlock();
        Logger.println("(TOMLayer.saveState) Finished saving state of EID " + lastEid + ", round " + decisionRound + " and leader " + leader);
    }

    public void saveCommands(byte[][] commands, int lastEid, int decisionRound, int leader) {
        StateLog thisLog = getLog();
        logLock.lock();
        Logger.println("(TOMLayer.saveBatch) Saving batch of EID " + lastEid + ", round " + decisionRound + " and leader " + leader);
        thisLog.addMessageBatch(commands, decisionRound, leader);
        thisLog.setLastEid(lastEid);
        logLock.unlock();
        Logger.println("(TOMLayer.saveBatch) Finished saving batch of EID " + lastEid + ", round " + decisionRound + " and leader " + leader);
    }

    @Override
    public ApplicationState getState(int eid, boolean sendState) {
        logLock.lock();
        System.out.println(" EID: " + eid + "sendstate: " + sendState);
        ApplicationState ret = (eid > -1 ? getLog().getApplicationState(eid, sendState) : new DefaultApplicationState());
        logLock.unlock();
        return ret;
    }

    @Override
    public int setState(ApplicationState recvState) {
        int lastEid = -1;
        if (recvState instanceof DefaultApplicationState) {
            DefaultApplicationState state = (DefaultApplicationState) recvState;
            System.out.println("(DefaultRecoverable.setState) last eid in state: " + state.getLastEid());
            getLog().update(state);
            int lastCheckpointEid = state.getLastCheckpointEid();
            lastEid = state.getLastEid();
            navigators.smart.tom.util.Logger.println("(DefaultRecoverable.setState) I'm going to update myself from EID " + lastCheckpointEid + " to EID " + lastEid);
            stateLock.lock();
            installSnapshot(state.getState());
            for (int eid = lastCheckpointEid + 1; eid <= lastEid; eid++) {
                try {
                    navigators.smart.tom.util.Logger.println("(DefaultRecoverable.setState) interpreting and verifying batched requests for eid " + eid);
                    System.out.println("(DefaultRecoverable.setState) interpreting and verifying batched requests for eid " + eid);
                    if (state.getMessageBatch(eid) == null) System.out.println("(DefaultRecoverable.setState) " + eid + " NULO!!!");
                    byte[][] commands = state.getMessageBatch(eid).commands;
                    executeBatch2(commands, null);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    if (e instanceof ArrayIndexOutOfBoundsException) {
                        System.out.println("Eid do ultimo checkpoint: " + state.getLastCheckpointEid());
                        System.out.println("Eid do ultimo consenso: " + state.getLastEid());
                        System.out.println("numero de mensagens supostamente no batch: " + (state.getLastEid() - state.getLastCheckpointEid() + 1));
                        System.out.println("numero de mensagens realmente no batch: " + state.getMessageBatches().length);
                    }
                }
            }
            stateLock.unlock();
        }
        return lastEid;
    }

    public abstract void installSnapshot(byte[] state);

    public abstract byte[] getSnapshot();

    public abstract byte[][] executeBatch2(byte[][] commands, MessageContext[] msgCtxs);
}
