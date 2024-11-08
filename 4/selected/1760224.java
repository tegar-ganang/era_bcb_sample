package groupcomm.common.consensus;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import uka.transport.DeepClone;
import uka.transport.MarshalStream;
import uka.transport.Transportable;
import uka.transport.UnmarshalStream;
import framework.Constants;
import framework.GroupCommEventArgs;
import framework.GroupCommException;
import framework.GroupCommMessage;
import framework.PID;
import framework.libraries.Trigger;
import framework.libraries.serialization.TArrayList;
import framework.libraries.serialization.TBoolean;
import framework.libraries.serialization.TInteger;
import framework.libraries.serialization.TList;

/** 
 * Cette classe g�re une ex�cution de ConsensusMR. <br>
 */
public class ConsensusPaxosExecution implements Transportable {

    /**
     * (Exclusive) variants/optimizations.
     */
    private int optVariant;

    /**
     * Variant flag: basic paxos.
     */
    protected static final int P0 = 0;

    /**
     * Variant flag: skip 1st read.
     */
    protected static final int P1 = 1;

    private static final Logger logger = Logger.getLogger(ConsensusPaxosExecution.class.getName());

    private Trigger trigger;

    /**
     * Identifiers of ConsensusPAXOS messages
     */
    public static final int CONS_READ = 15965;

    public static final int CONS_ACKREAD = 15966;

    public static final int CONS_NACKREAD = 15967;

    public static final int CONS_ACKWRITE = 15968;

    public static final int CONS_NACKWRITE = 15969;

    public static final int CONS_WRITE = 15970;

    public static final int CONS_DECISION = 15971;

    private PID myself;

    /** le num�ro du consensus.
     */
    private Transportable k;

    /** The serial number of the current round.
     * -1 means that the algorithm has not started yet.
     * +infinity means that the algorithm has finished
     * (with a decision).
     */
    private int round = -1;

    /**le leader courant
     */
    private PID leader;

    /**l'estimation courante du processus
     */
    private Transportable estimate;

    /**
     * Number of processes that execute this consensus algorithm.
     */
    private int n;

    /**
     * Latest read and write phase.
     */
    private int write = -1;

    private int read = -1;

    /**
     * Number of [Read|Write]Ack] received so far, in the current round.
     */
    private int nbAckRead;

    private int nbAckWrite;

    private int nbNack;

    private int highestWrite;

    /**
     * The round at which the process initiated a propose.
     */
    private int proposedRound = -1;

    private int limit;

    protected int nackLimit;

    private TList others;

    private TList group;

    private boolean ignoreSuspicions = false;

    public ConsensusPaxosExecution(PID myself, Transportable k, Trigger trigger, int optVariant) {
        this.myself = myself;
        this.k = k;
        this.leader = null;
        this.trigger = trigger;
        this.optVariant = optVariant;
    }

    /**
     * Lance l'�x�cution de consensus.
     */
    public void processStart(Transportable proposal, TList group) throws GroupCommException {
        estimate = proposal;
        n = group.size();
        this.group = new TArrayList();
        this.others = new TArrayList();
        for (int i = 0; i < n; i++) {
            PID p = (PID) group.get(i);
            this.group.add(p);
            if (!p.equals(myself)) {
                this.others.add(p);
            }
        }
        limit = n / 2;
        nackLimit = 1;
        if (this.leader == null) this.leader = (PID) this.group.get(0);
        if (round != -1) throw new GroupCommException("ConsensusPaxosExecution: Calling start while round != -1!!");
        round = group.indexOf(myself);
        processPropose();
        ignoreSuspicions = false;
    }

    /**
     * Commence la proc�dure de proposition. Si l'optimisation est choisie
     * on envois directement un CONS_WRITE avce notre estimate au lieu du
     * CONS_READ pr�liminaire.
     */
    public void processPropose() throws GroupCommException {
        if ((!hasDecided()) && (isLeader()) && (round != proposedRound)) {
            if (optVariant == P0 || (optVariant == P1 && round != 0)) {
                sendRead();
                read = round;
            } else if (optVariant == P1 && round == 0) {
                sendWrite();
                read = round;
                write = round;
            } else {
                throw new GroupCommException("ConsensusPaxosExecution: Invalid option!!!");
            }
            nbAckRead = 0;
            nbAckWrite = 0;
            nbNack = 0;
            highestWrite = write;
            proposedRound = round;
        }
    }

    /**
     * Re�oit un read. Le consid�re seulement s'il est d'un round non 
     * connu (-> sup�rieur au round connu actuellement
     */
    public void processRead(int r, PID source) {
        if ((read > r) || (write > r)) sendNackRead(source, r); else {
            read = r;
            sendAckRead(source, r);
        }
    }

    /**
     * Re�oit un write. Le consid�re seulement s'il est d'un round non 
     * connu (-> sup�rieur au round connu actuellement
     */
    public void processWrite(int r, PID source, Transportable estimateFromW) {
        if ((read > r) || (write > r)) sendNackWrite(source, r); else {
            write = r;
            estimate = estimateFromW;
            sendAckWrite(source, r);
        }
    }

    /**
     * Re�oit un ackRead. Envoi un write si on a obtenu le nombre limite d'ack
     */
    public void processAckRead(int r, int lastWrite, Transportable estimateFromAck) {
        if ((r == round) && (nbNack == 0)) {
            nbAckRead++;
            if (lastWrite > highestWrite) {
                estimate = estimateFromAck;
                highestWrite = lastWrite;
            }
            if (nbAckRead == limit) {
                if (write < round) write = round;
                highestWrite = Integer.MAX_VALUE;
                sendWrite();
            }
        }
    }

    /**
     * Re�oit un nack.
     */
    public void processNack(int r) throws GroupCommException {
        if (hasDecided()) return;
        if (r == round) {
            nbNack++;
            if (((nbAckWrite + nbNack) == limit) && (nbNack > nackLimit)) processNackOnLimit();
        }
    }

    /**
     * Re�oit un ackWrite. Decide si le nombre d'ack+nack>limit et que l'on
     * a pas re�u un nombre de nack>=nackLimit
     */
    public void processAckWrite(int r) throws GroupCommException {
        if (r == round) {
            nbAckWrite++;
            if ((nbAckWrite + nbNack) == limit) {
                if (nbNack < nackLimit) broadcastDecision(); else processNackOnLimit();
            }
        }
    }

    /**
     * Envoit d'un NACK au coordinateur si et seulement si celui-ci est suspect�.
     */
    public void processNewLeader(PID newLeader) throws GroupCommException {
        this.leader = newLeader;
        if (!ignoreSuspicions) processAbort();
    }

    private void processAbort() throws GroupCommException {
        if (!isLeader() || hasDecided()) return;
        processPropose();
    }

    /**
     * Indique si ce processus est leader. <br>
     *
     * @return Ce processsus est leader.
     */
    private boolean isLeader() {
        return (leader.equals(myself));
    }

    /**
     * Indique si le consensus a d�j� d�cid� pour ce processus. <br>
     *
     * @return Ce consensus a d�j� d�cid�.
     */
    private boolean hasDecided() {
        return (round >= Integer.MAX_VALUE);
    }

    /**
     * Indique si le consensus a d�j� commencer pour ce processus. <br>
     *
     * @return Ce consensus a d�j� commencer.
     */
    public boolean hasStarted() {
        return round > -1;
    }

    /**
     * Appel�e si le nombre Ack+Nack>limit et Nack > nackLimit
     */
    private void processNackOnLimit() throws GroupCommException {
        round = round + n;
        processPropose();
    }

    private void sendRead() {
        GroupCommMessage proposeMessage = new GroupCommMessage();
        proposeMessage.tpack(new TInteger(round));
        proposeMessage.tpack(new TInteger(CONS_READ));
        proposeMessage.tpack(k);
        triggerSend(proposeMessage, others);
    }

    private void sendWrite() {
        GroupCommMessage proposeMessage = new GroupCommMessage();
        proposeMessage.tpack(estimate);
        proposeMessage.tpack(new TInteger(round));
        proposeMessage.tpack(new TInteger(CONS_WRITE));
        proposeMessage.tpack(k);
        triggerSend(proposeMessage, others);
    }

    private void sendAckRead(PID receiver, int r) {
        GroupCommMessage proposeMessage = new GroupCommMessage();
        proposeMessage.tpack(estimate);
        proposeMessage.tpack(new TInteger(write));
        proposeMessage.tpack(new TInteger(r));
        proposeMessage.tpack(new TInteger(CONS_ACKREAD));
        proposeMessage.tpack(k);
        triggerSend(proposeMessage, receiver);
    }

    private void sendNackRead(PID receiver, int r) {
        GroupCommMessage proposeMessage = new GroupCommMessage();
        proposeMessage.tpack(new TInteger(r));
        proposeMessage.tpack(new TInteger(CONS_NACKREAD));
        proposeMessage.tpack(k);
        triggerSend(proposeMessage, receiver);
    }

    private void sendAckWrite(PID receiver, int r) {
        GroupCommMessage proposeMessage = new GroupCommMessage();
        proposeMessage.tpack(new TInteger(r));
        proposeMessage.tpack(new TInteger(CONS_ACKWRITE));
        proposeMessage.tpack(k);
        triggerSend(proposeMessage, receiver);
    }

    private void sendNackWrite(PID receiver, int r) {
        GroupCommMessage proposeMessage = new GroupCommMessage();
        proposeMessage.tpack(new TInteger(r));
        proposeMessage.tpack(new TInteger(CONS_NACKWRITE));
        proposeMessage.tpack(k);
        triggerSend(proposeMessage, receiver);
    }

    private void broadcastDecision() {
        GroupCommMessage decisionMessage = new GroupCommMessage();
        decisionMessage.tpack(group);
        decisionMessage.tpack(estimate);
        decisionMessage.tpack(new TInteger(CONS_DECISION));
        decisionMessage.tpack(k);
        triggerSend(decisionMessage, group);
    }

    private void triggerSend(GroupCommMessage m, TList g) {
        for (int i = 0; i < g.size(); i++) {
            triggerSend(m.cloneGroupCommMessage(), (PID) g.get(i));
        }
    }

    private void triggerSend(GroupCommMessage m, PID p) {
        GroupCommEventArgs pt2ptSend = new GroupCommEventArgs();
        pt2ptSend.addLast(m);
        pt2ptSend.addLast(p);
        pt2ptSend.addLast(new TBoolean(false));
        logger.log(Level.FINE, "Sending Pt2Pt message {0} to {1}", new Object[] { m, p });
        trigger.trigger(Constants.PT2PTSEND, pt2ptSend);
    }

    public String toString() {
        return new String("(** k: " + k + " r: " + round + " leader: " + leader + " write: " + write + " read: " + read + " nbAckRead: " + nbAckRead + " nbAckWrite: " + nbAckWrite + " nbNack: " + nbNack + " highestWrite: " + highestWrite + " estimate: " + estimate + "**)");
    }

    public void marshal(MarshalStream arg0) throws IOException {
        throw new IOException("not implemented");
    }

    public void unmarshalReferences(UnmarshalStream arg0) throws IOException, ClassNotFoundException {
        throw new IOException("not implemented");
    }

    public Object deepClone(DeepClone arg0) throws CloneNotSupportedException {
        throw new CloneNotSupportedException("not implemented");
    }
}
