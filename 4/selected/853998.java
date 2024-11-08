package org.multipi;

import java.util.HashMap;
import java.util.Iterator;
import org.jdom.Element;

public class InteractionManager extends Thread {

    String id;

    int castSize;

    CastManager castMgr;

    Coordinator coordinator;

    int debugLevel = Literal.DEBUG_LEVEL_NOWRITER;

    int requestCount = 0;

    int committedCount = 0;

    int withdrawnCount = 0;

    int state = State.I_MEETING;

    boolean repeat;

    boolean schedulerRunning = true;

    int interactionCount = 0;

    DebugOutputter dout;

    /**
   * 
   * @param coord the Coordinator object which created this object
   * @param iDef jave representation of XML definition of Interaction 
   * @param rpt allow more the one interaction
   * @param debugLevel
   */
    public InteractionManager(Coordinator coord, Element iDef, boolean rpt, int debugLevel) {
        id = iDef.getAttributeValue("procId");
        if (debugLevel > Literal.DEBUG_LEVEL_NOWRITER) {
            dout = new DebugOutputter("IMgr," + id + ":", debugLevel);
        }
        castMgr = new CastManager(iDef, dout, debugLevel);
        castSize = iDef.getChildren("role").size();
        coordinator = coord;
        repeat = rpt;
    }

    public InteractionManager(Coordinator coord, Element prDef, boolean rpt) {
        this(coord, prDef, rpt, Literal.DEBUG_LEVEL_QUIET);
    }

    /**
   * 
   * @param msgRcvrs table from which {@see Interactor} instance
   * {@see MsgReceiver} references can be looked up.
   */
    void setContractorMsgRcvrs(HashMap msgRcvrs) {
        castMgr.setContractorMsgRcvrs(msgRcvrs);
    }

    /** 
 *  <p>The Finite State Automaton (FSA) of the InteractionManager process.  
 *  Essentilly
 *  this process continues indefinitely until it has acheved a state of 
 *  PRODN_SUCCESS, that is after both a BID_REQUEST and then a BID_COMMIT
 *  message has been received from an Interactor Bidder process for each 
 *  RoleHandler (or role) in the (movie) InteractionManager.</p>
 *
 *  <p>Regarding state I_ALL_MET and I_CLEANUP: 
 *
 *  <ol><li>if message sending and
 *  receiving could be instantaneous, then would be only be a need to handle
 *  BID_WITHDRAW messages in these states, however, because it is possible for
 *  an {@see Interactor} {@see Bidder} process to send a BID_ABORT mesage after 
 *  an I_ALL_MET message has been sent by a InteractionManager process, but 
 *  before teh ALL_MET message has been received at its end.  That is why it 
 *  is possible for a BID_ABORT message to be received in state I_ALL_MET and 
 *  even in I_CLEANUP.</li>
 *
 * </ol></p> 
 */
    protected int coordinate() {
        int msgFromBid = MsgType.BID_NO_MSG;
        while (state != State.I_SUCCESS) {
            while ((msgFromBid = castMgr.getMessage()) == MsgType.NO_MESSAGE) ;
            if (msgFromBid == MsgType.TERMINATED) {
                break;
            }
            switch(state) {
                case State.I_MEETING:
                    if (msgFromBid == MsgType.BID_REQUEST) {
                        requestCount++;
                        if (requestCount >= castSize) {
                            requestCount = 0;
                            castMgr.sendMsgsToAll(MsgType.I_ALL_MET);
                            state = State.I_ALL_MET;
                        }
                    } else if (msgFromBid == MsgType.BID_ABORT) {
                        requestCount--;
                    } else {
                        System.out.println("INVALID BID MESSAGE (1)");
                        System.exit(1);
                    }
                    break;
                case State.I_ALL_MET:
                    if (msgFromBid == MsgType.BID_COMMIT) {
                        committedCount++;
                        if (withdrawnCount <= 0) {
                            if (committedCount >= castSize) {
                                committedCount = 0;
                                castMgr.sendMsgsToAll(MsgType.I_SUCCEED);
                                state = State.I_SUCCESS;
                                break;
                            }
                        } else {
                            castMgr.rejectCurrentInteractor();
                        }
                    } else if (msgFromBid == MsgType.BID_WITHDRAW || msgFromBid == MsgType.BID_ABORT) {
                        if (withdrawnCount == 0 && committedCount > 0) {
                            castMgr.rejectCommittedInteractors(committedCount);
                        }
                        withdrawnCount++;
                    } else {
                        System.out.println("INVALID BID MESSAGE (2)");
                        System.exit(1);
                    }
                    if ((withdrawnCount + committedCount) >= castSize) {
                        withdrawnCount = committedCount = 0;
                        castMgr.resetRoles(State.R_INIT);
                        state = State.I_MEETING;
                    }
                    break;
                default:
                    System.out.println("INVALID InteractionManager STATE (2)");
                    System.exit(1);
            }
        }
        return MsgType.NOT_TERMINATED;
    }

    /** 
 *
 * <p>Write record of interaction.  Simulate time taken by sleeping fro some
 * arbitrary length of time.</p>
 * 
 * TODO: add some form of shared memory interaction as outlined in 
 * {@see Interactor#interact(Bidder)}. 
 *
 */
    protected void enableInteraction() {
        dout.write("Interaction enabled");
        InteractionRecord pe = new InteractionRecord(System.currentTimeMillis(), id, castMgr.getRolesPlayed());
        coordinator.recordInteraction(pe);
        try {
            sleep(500);
        } catch (InterruptedException ie) {
        }
        interactionCount++;
        pe.setFinishTime(System.currentTimeMillis());
    }

    /** 
 * <p>Runs the main loop either until either the interaction has occurred
 * once, ore else indefinitely</p> 
 *
 * <p>TODO: implement means by which criterea for shutting down 
 * InteractionMananger threads (and hence the whole application), after 
 * more than one Intereaction has occurred, can be defined.</p>
 *
 */
    public void run() {
        dout.write("Starting thread(1) (thread,state)=" + State.str[state] + "," + Thread.currentThread().getName());
        boolean runAgain = true;
        while (runAgain) {
            if (coordinate() == MsgType.TERMINATED) {
                break;
            }
            if (state == State.I_SUCCESS) {
                enableInteraction();
            }
            state = State.I_MEETING;
            if (!repeat) {
                castMgr.resetRoles(State.R_UNAVAILABLE);
                runAgain = false;
            } else {
                castMgr.resetRoles(State.R_INIT);
            }
        }
        coordinator.notifyInteractionFinished();
        dout.write("notified finished");
        while (castMgr.getMessage() != MsgType.TERMINATED) ;
    }

    /**
	 * @return id the String identifier of this InteractionManager instance.
	 */
    public String getId() {
        return id;
    }

    /**
  * @return count of interactions which the encompassing {@see 
  * InteractionManager} has facilitated.
  */
    public int getInteractionCount() {
        return interactionCount;
    }

    /**
  * @return
  */
    public MsgReceiver getRcvr() {
        return castMgr.getRcvr();
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer(id + "(" + interactionCount + ",(" + castSize + "," + requestCount + "," + withdrawnCount + "," + committedCount + "))," + State.str[state] + "\n");
        for (Iterator iter = castMgr.roleMgrs.values().iterator(); iter.hasNext(); ) {
            RoleManager rMgr = (RoleManager) iter.next();
            sbuf.append(rMgr.toString());
        }
        return sbuf.toString();
    }
}
