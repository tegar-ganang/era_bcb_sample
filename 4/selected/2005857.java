package jade.core.messaging;

import jade.util.Logger;
import jade.lang.acl.ACLMessage;
import jade.domain.FIPAAgentManagement.InternalError;
import jade.core.AID;
import jade.core.ResourceManager;
import jade.core.Profile;
import jade.core.ProfileException;
import jade.core.NotFoundException;
import jade.core.UnreachableException;

/**
 * This class manages the delivery of ACLMessages to remote destinations
 * in an asynchronous way.
 * If network problems prevent the delivery of a message, this class also 
 * embeds a mechanism to buffer the message and periodically retry to 
 * deliver it.
 * @author  Giovanni Caire - TILAB
 * @author  Elisabetta Cortese - TILAB
 * @author  Fabio Bellifemine - TILAB
 * @author  Jerome Picault - Motorola Labs
 * @version $Date: 2008-08-19 12:27:27 +0200 (mar, 19 ago 2008) $ $Revision: 6043 $
 */
class MessageManager {

    public interface Channel {

        void deliverNow(GenericMessage msg, AID receiverID) throws UnreachableException, NotFoundException;

        void notifyFailureToSender(GenericMessage msg, AID receiver, InternalError ie);
    }

    private static MessageManager theInstance;

    private static final int POOL_SIZE_DEFAULT = 5;

    private static final int MAX_POOL_SIZE = 100;

    private static final int MAX_QUEUE_SIZE_DEFAULT = 10000000;

    private OutBox outBox;

    private Thread[] delivererThreads;

    private Deliverer[] deliverers;

    private Logger myLogger = Logger.getMyLogger(getClass().getName());

    private MessageManager() {
    }

    public static synchronized MessageManager instance(Profile p) {
        if (theInstance == null) {
            theInstance = new MessageManager();
            theInstance.initialize(p);
        }
        return theInstance;
    }

    public void initialize(Profile p) {
        int poolSize = POOL_SIZE_DEFAULT;
        try {
            String tmp = p.getParameter("jade_core_messaging_MessageManager_poolsize", null);
            poolSize = Integer.parseInt(tmp);
        } catch (Exception e) {
        }
        int maxQueueSize = MAX_QUEUE_SIZE_DEFAULT;
        try {
            String tmp = p.getParameter("jade_core_messaging_MessageManager_maxqueuesize", null);
            maxQueueSize = Integer.parseInt(tmp);
        } catch (Exception e) {
        }
        outBox = new OutBox(maxQueueSize);
        try {
            ResourceManager rm = p.getResourceManager();
            delivererThreads = new Thread[poolSize];
            deliverers = new Deliverer[poolSize];
            for (int i = 0; i < poolSize; ++i) {
                String name = "Deliverer-" + i;
                deliverers[i] = new Deliverer();
                delivererThreads[i] = rm.getThread(ResourceManager.TIME_CRITICAL, name, deliverers[i]);
                if (myLogger.isLoggable(Logger.FINE)) {
                    myLogger.log(Logger.FINE, "Starting deliverer " + name + ". Thread=" + delivererThreads[i]);
                }
                delivererThreads[i].start();
            }
        } catch (ProfileException pe) {
            throw new RuntimeException("Can't get ResourceManager. " + pe.getMessage());
        }
    }

    /**
	   Activate the asynchronous delivery of a GenericMessage
	 */
    public void deliver(GenericMessage msg, AID receiverID, Channel ch) {
        outBox.addLast(receiverID, msg, ch);
    }

    /**
 	   Inner class Deliverer
	 */
    class Deliverer implements Runnable {

        private long servedCnt = 0;

        public void run() {
            while (true) {
                PendingMsg pm = outBox.get();
                GenericMessage msg = pm.getMessage();
                AID receiverID = pm.getReceiver();
                Channel ch = pm.getChannel();
                try {
                    ch.deliverNow(msg, receiverID);
                } catch (Throwable t) {
                    myLogger.log(Logger.WARNING, "MessageManager cannot deliver message " + stringify(msg) + " to agent " + receiverID.getName(), t);
                    ch.notifyFailureToSender(msg, receiverID, new InternalError(ACLMessage.AMS_FAILURE_UNEXPECTED_ERROR + ": " + t));
                }
                servedCnt++;
                outBox.handleServed(receiverID);
            }
        }

        long getServedCnt() {
            return servedCnt;
        }
    }

    /**
	   Inner class PendingMsg
	 */
    public static class PendingMsg {

        private final GenericMessage msg;

        private final AID receiverID;

        private final Channel channel;

        private long deadline;

        public PendingMsg(GenericMessage msg, AID receiverID, Channel channel, long deadline) {
            this.msg = msg;
            this.receiverID = receiverID;
            this.channel = channel;
            this.deadline = deadline;
        }

        public GenericMessage getMessage() {
            return msg;
        }

        public AID getReceiver() {
            return receiverID;
        }

        public Channel getChannel() {
            return channel;
        }

        public long getDeadline() {
            return deadline;
        }

        public void setDeadline(long deadline) {
            this.deadline = deadline;
        }
    }

    /**
	 */
    public static final String stringify(GenericMessage m) {
        ACLMessage msg = m.getACLMessage();
        if (msg != null) {
            StringBuffer sb = new StringBuffer("(");
            sb.append(ACLMessage.getPerformative(msg.getPerformative()));
            sb.append(" sender: ");
            sb.append(msg.getSender().getName());
            if (msg.getOntology() != null) {
                sb.append(" ontology: ");
                sb.append(msg.getOntology());
            }
            if (msg.getConversationId() != null) {
                sb.append(" conversation-id: ");
                sb.append(msg.getConversationId());
            }
            sb.append(')');
            return sb.toString();
        } else {
            return ("unavailable");
        }
    }

    String[] getQueueStatus() {
        return outBox.getStatus();
    }

    String getGlobalInfo() {
        return "Submitted-messages = " + outBox.getSubmittedCnt() + ", Served-messages = " + outBox.getServedCnt() + ", Queue-size (byte) = " + outBox.getSize();
    }

    String[] getThreadPoolStatus() {
        String[] status = new String[delivererThreads.length];
        for (int i = 0; i < delivererThreads.length; ++i) {
            status[i] = "(Deliverer-" + i + " :alive " + delivererThreads[i].isAlive() + " :Served-messages " + deliverers[i].getServedCnt() + ")";
        }
        return status;
    }

    Thread[] getThreadPool() {
        return delivererThreads;
    }
}
