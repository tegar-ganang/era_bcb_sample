package soct2;

import org.jgroups.Channel;
import org.jgroups.ExtendedReceiverAdapter;
import org.jgroups.Message;
import broadcaster.BroadcastI;
import data.TSPlainDocument;

/**
 * @author molli
 */
public class TSWorkspace implements Cloneable {

    private int sid;

    private String workspaceName;

    private StateVector stateVector;

    private TSPlainDocument pd;

    private TSLog log;

    private TSReceptQueue receptQueue;

    private BroadcastI broadcastManager;

    private Channel channel;

    private ExtendedReceiverAdapter extRecAdapt;

    private boolean isSynchro = false;

    public TSWorkspace(BroadcastI bm) {
        this.stateVector = new StateVector();
        this.log = new TSLog(this);
        this.receptQueue = new TSReceptQueue(this);
        this.pd = null;
        this.broadcastManager = bm;
        bm.register(this);
        this.workspaceName = "" + channel.getLocalAddress();
        this.sid = this.workspaceName.hashCode();
        extRecAdapt = new ExtendedReceiverAdapter() {

            public void receive(Message arg0) {
                if (arg0.getSrc() != channel.getLocalAddress()) {
                    System.out.println(arg0.getSrc() + " : " + arg0.getObject());
                    TSOperation op = (TSOperation) arg0.getObject();
                    receiveOp(op);
                    if (isSynchro) {
                        try {
                            integrateAll();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        channel.setReceiver(extRecAdapt);
    }

    public TSWorkspace(int sid, String wsname, BroadcastI bm) {
        this.sid = sid;
        this.workspaceName = wsname;
        this.stateVector = new StateVector();
        this.log = new TSLog(this);
        this.receptQueue = new TSReceptQueue(this);
        this.pd = null;
        this.broadcastManager = bm;
        bm.register(this);
    }

    public TSWorkspace(TSWorkspace workspace, String name, int sid) {
        this.stateVector = workspace.getVS().clone();
        this.broadcastManager = workspace.getBroadcastManager();
        workspace.getBroadcastManager().register(this);
        this.log = new TSLog(workspace.log, this);
        this.receptQueue = new TSReceptQueue(workspace.receptQueue, this);
        this.workspaceName = name;
        this.sid = sid;
        this.pd = new TSPlainDocument(workspace.getPd(), this);
    }

    public void receive(TSOperation op) {
        receptQueue.add(op);
    }

    public void integrateAll() throws Exception {
        System.out.println("liste : " + receptQueue.getSize());
        TSOperation op;
        int i = 0;
        double time = System.currentTimeMillis();
        double time_merge = 0, time_execute = 0;
        double t1, t2, t3;
        int n = 0;
        if (pd == null) throw new Exception("PlainDocument null");
        while (i < receptQueue.getSize()) {
            op = receptQueue.getOp(i);
            if (stateVector.dominate(op.getVS())) {
                t1 = System.currentTimeMillis();
                TSOperation opt = log.merge(op);
                t2 = System.currentTimeMillis();
                time_merge += t2 - t1;
                TSOperation opt2 = (TSOperation) opt.clone();
                pd.executeRemote(opt);
                t3 = System.currentTimeMillis();
                time_execute += t3 - t2;
                log.addOp(opt2);
                stateVector.incStateNumber(op.getSid());
                receptQueue.removeOp(i);
                n++;
                i = 0;
            } else {
                i++;
            }
        }
        double end = System.currentTimeMillis();
        System.out.println("Integration time: " + (end - time));
        System.out.println("Nombre d'intégration " + n);
        System.out.println("Temps d'intégration par opération " + (end - time) / n);
        System.out.println("Temps merge: " + time_merge + " " + time_merge / n);
        System.out.println("Temps executeRemote: " + time_execute + " " + time_execute / n);
    }

    /**
	 * @return Returns the stringState.
	 * @uml.property name="stringState"
	 */
    public String toString() {
        return this.workspaceName;
    }

    /**
	 * @return Returns the workspaceName.
	 * @uml.property name="workspaceName"
	 */
    public String getWorkspaceName() {
        return this.workspaceName;
    }

    public StateVector getVS() {
        return this.stateVector;
    }

    public TSOperation getLastOp() {
        return log.getOp(log.getSize() - 1);
    }

    public int getSid() {
        return sid;
    }

    public BroadcastI getBroadcastManager() {
        return broadcastManager;
    }

    public TSLog getLog() {
        return log;
    }

    public TSReceptQueue getReceptQueue() {
        return receptQueue;
    }

    public void send(TSOperation op) {
        log.addOp(op);
        stateVector.incStateNumber(this.sid);
        broadcastManager.broadcast(this, op);
    }

    public TSPlainDocument getPd() {
        return pd;
    }

    public void setPd(TSPlainDocument pd) {
        this.pd = pd;
    }

    public void setChannel(Channel c) {
        this.channel = c;
        channel.setReceiver(extRecAdapt);
    }

    public Channel getChannel() {
        return this.channel;
    }

    public void receiveOp(TSOperation op) {
        this.receive(op);
    }

    public void setSynchro(boolean isSynchro) {
        this.isSynchro = isSynchro;
    }
}
