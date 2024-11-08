package org.multipi;

import java.util.HashMap;
import org.jdom.Element;

public class Interactor extends Thread {

    String id;

    String prodn;

    DebugOutputter dout;

    int bidCount;

    BidManager bidManager;

    boolean running = true;

    public Interactor(Element interactorDef, HashMap totalOrderTable, boolean repeat, int debugLevel) {
        id = interactorDef.getAttributeValue("procId");
        bidCount = interactorDef.getChildren().size();
        if (debugLevel > Literal.DEBUG_LEVEL_NOWRITER) {
            dout = new DebugOutputter("I," + id + ":", debugLevel);
        }
        bidManager = new BidManager(interactorDef, totalOrderTable, repeat, dout, debugLevel);
    }

    public Interactor(Element contractorDef, HashMap totalOrderTable, boolean repeat) {
        this(contractorDef, totalOrderTable, repeat, Literal.DEBUG_LEVEL_NOWRITER);
    }

    /**
 * 
 * <p>Runs until the coniditions for termination have been met. Within
 * the main loop the Interaction thread will interact if coordination 
 * has been achieved.</p> 
 *
 */
    public void run() {
        dout.write("starting (thread,activeBidCount)=" + Thread.currentThread().getName() + "," + bidManager.getBidCount());
        Bidder successfulBidder = null;
        while ((bidManager.getActiveBidCount() > 0) && bidManager.running) {
            dout.write("activeBidCount = " + bidManager.getActiveBidCount());
            if ((successfulBidder = bidManager.bidForRole()) != null) {
                dout.write("starting (3)");
                interact(successfulBidder);
            }
        }
        dout.write("finished");
    }

    public String getId() {
        return id;
    }

    public void init(HashMap prodnRcvrTable) {
        bidManager.init(prodnRcvrTable);
    }

    public MsgReceiver getRcvr() {
        return bidManager.getRcvr();
    }

    /**
  * <p>Dummy method to simulate interaction.</p>
  * <p>TODO: implement 'shared memory' which allows all other parties to 
  * the interaction access to data fields in the state they were prior
  * to coordination.  Similarly, this party to the interaction will have
  * access to the data fields of any other party to the interaction in 
  * the state they were in prior to the interaction.  Do this by:
  * <ol>
  * <li>instantiate a data field object</li>
  * <li>at point of coordination, 'clone' that data object and make a 
  *     reference to that object available to other parties to the intereaction</li>
  * <li>as desired, update local data object with values from 'cloned' data
  *     objects from other parties to the interaction.</li>
  * </ol></p>
  * <p>@see InteractionManager#enableInteraction()</p>   
  */
    public void interact(Bidder successfulBidder) {
        System.out.println(id + " interacting with (" + successfulBidder.interactionId + "," + successfulBidder.roleId + ")");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ie) {
        }
    }

    public void terminate() {
        bidManager.terminate();
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer(id + "," + bidCount + "\n");
        for (int i = 0; i < bidManager.bidders.length; i++) {
            Bidder b = bidManager.bidders[i];
            if (b != null) sbuf.append(b.toString()); else sbuf.append("   null\n");
        }
        return sbuf.toString();
    }
}
