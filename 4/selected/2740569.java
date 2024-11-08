package rescuecore;

import rescuecore.commands.AKAcknowledge;
import rescuecore.commands.AKConnect;
import rescuecore.commands.Command;
import rescuecore.commands.KAConnectError;
import rescuecore.commands.KAConnectOK;
import rescuecore.commands.KAHear;
import rescuecore.commands.KASense;
import rescuecore.debug.DebugMemoryListener;
import rescuecore.debug.DebugWriter;

/**
   This is the base class for all agents. This class handles messages from the server, provides a memory of the simulation environment and convenience methods for path planning etc. This class also enforces the message limits imposed by the robocup rescue rules.
   <p>Agent implementations should provide at least one of the following three constructors:
   <ol><li>A no-arg constructor - e.g. MyAgent()
   <li>A String[] constructor - e.g. MyAgent(String[] args)
   <li>A constructor that takes one or more String arguments - e.g. MyAgent(String arg1, String arg2)
   </ol>
   The reason for this is that the AgentSystem allows arguments to be passed to the Agent via the command line. When creating an instance of the agent it first looks for any constructor that accepts the right number of String arguments, followed by the String[] constructor. Failing that, the no-arg constructor will be used.
   <p>
   For example, assuming we have the three constructors mentioned above, if the command line provides two arguments then the AgentSystem will use the MyAgent(String arg1, String arg2) constructor. If only one argument is provided then the MyAgent(String[] args) constructor is used.
 */
public abstract class Agent extends RescueComponent {

    private int agentType;

    protected int type;

    protected int id;

    protected int timeStep;

    protected Memory memory;

    private int tempID;

    private volatile boolean running;

    private static int NEXT_ID = 0;

    protected boolean debug = false;

    /**
       Create a new agent of a particular type.
       @param type The type of this agent - this value should be the logical OR of all types that this agent can be. For example, an Agent implementation that can be either a Police Force or an Ambulance Team should specify its type as AGENT_TYPE_POLICE_FORCE | AGENT_TYPE_AMBULANCE_TEAM.
       @see RescueConstants#AGENT_TYPE_CIVILIAN
       @see RescueConstants#AGENT_TYPE_FIRE_BRIGADE
       @see RescueConstants#AGENT_TYPE_FIRE_STATION
       @see RescueConstants#AGENT_TYPE_POLICE_FORCE
       @see RescueConstants#AGENT_TYPE_POLICE_OFFICE
       @see RescueConstants#AGENT_TYPE_AMBULANCE_TEAM
       @see RescueConstants#AGENT_TYPE_AMBULANCE_CENTER
       @see RescueConstants#AGENT_TYPE_ANY_MOBILE
       @see RescueConstants#AGENT_TYPE_ANY_BUILDING
       @see RescueConstants#AGENT_TYPE_ANY_AGENT
       @see RescueConstants#AGENT_TYPE_ANY
	*/
    protected Agent(int type) {
        this.agentType = type;
        id = -1;
        timeStep = -1;
        this.type = -1;
        tempID = ++NEXT_ID;
    }

    public final int getComponentType() {
        return RescueConstants.COMPONENT_TYPE_AGENT;
    }

    public final Command generateConnectCommand() {
        return new AKConnect(tempID, agentType, 0);
    }

    protected void appendCommand(Command c) {
        super.appendCommand(c);
        if (debug) logObject(c);
    }

    public final boolean handleConnectOK(Command c) {
        KAConnectOK ok = (KAConnectOK) c;
        int replyID = ok.getReplyID();
        if (replyID == tempID) {
            id = ok.getID();
            System.out.println("Connect succeeded for " + tempID + ". Kernel assigned id:" + id);
            try {
                RescueObject[] knowledge = ok.getKnowledge();
                RescueObject self = ok.getSelf();
                type = self.getType();
                initialise(knowledge, self);
                RescueMessage ack = new RescueMessage();
                ack.append(new AKAcknowledge(id));
                sendMessage(ack);
            } catch (Exception e) {
                System.out.println(e);
                e.printStackTrace();
            }
            timeStep = 0;
            running = true;
            return true;
        } else {
            System.out.println("Received a KA_CONNECT_OK for agent " + replyID + ", but I'm listening for a reply for " + tempID);
        }
        return false;
    }

    public final String handleConnectError(Command c) {
        KAConnectError error = (KAConnectError) c;
        int replyID = error.getReplyID();
        String reason = error.getReason();
        if (replyID == tempID) return reason; else System.out.println("Received a KA_CONNECT_ERROR (" + reason + ") for agent " + replyID + ", but I'm listening for a reply for " + tempID);
        return null;
    }

    public boolean isRunning() {
        return running;
    }

    public void shutdown() {
        running = false;
    }

    public final void handleMessage(Command c) {
        switch(c.getType()) {
            case RescueConstants.KA_SENSE:
                handleSense((KASense) c);
                break;
            case RescueConstants.KA_HEAR:
                handleHear(c);
                break;
            case RescueConstants.KA_CONNECT_OK:
                if (running) {
                    KAConnectOK ok = (KAConnectOK) c;
                    int replyID = ok.getReplyID();
                    System.out.println(this + " just received a KA_CONNECT_OK to " + replyID + " - my tempID is " + tempID);
                    if (replyID == tempID) {
                        int newID = ok.getID();
                        System.out.println("Old ID: " + id + ", new ID: " + newID);
                        id = newID;
                        RescueMessage ack = new RescueMessage();
                        ack.append(new AKAcknowledge(id));
                        sendMessage(ack);
                    }
                }
                break;
            default:
                handleOtherMessage(c);
                break;
        }
        logObject(c);
    }

    protected void handleOtherMessage(Command c) {
        System.out.println("Timestep " + timeStep + ": " + this + " received a weird command: " + Handy.getCommandTypeName(c.getType()));
    }

    /**
       Handle a KA_SENSE message
       @param c The KA_SENSE Command object
	*/
    private void handleSense(Command c) {
        KASense sense = (KASense) c;
        try {
            int newTimeStep = sense.getTime();
            if (newTimeStep < timeStep) System.err.println(this + " just moved back in time! It was timestep " + timeStep + " and now it's timestep " + newTimeStep);
            if (newTimeStep > timeStep + 1) System.err.println(this + " just skipped ahead in time! It was timestep " + timeStep + " and now it's timestep " + newTimeStep);
            timeStep = newTimeStep;
            memory.update(sense);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sense();
        flushCommands();
        DebugWriter.flush(this);
    }

    /**
       Handle a KA_HEAR (or KA_HEAR_SAY, or KA_HEAR_TELL) message
	   @param hear The KA_HEAR Command object.
	   @see RescueConstants#KA_HEAR
	   @see RescueConstants#KA_HEAR_SAY
	   @see RescueConstants#KA_HEAR_TELL
	*/
    private void handleHear(Command c) {
        KAHear hear = (KAHear) c;
        int toID = hear.getToID();
        int fromID = hear.getFromID();
        int length = hear.getLength();
        byte[] msg = hear.getData();
        byte channel = hear.getChannel();
        hear(fromID, msg, channel);
    }

    private RescueObject me() {
        return memory.lookup(id);
    }

    public String toString() {
        if (!running) return "Unconnected agent. Temporary ID: " + tempID;
        return Handy.getTypeName(type) + " (" + id + ")";
    }

    /**
       Get this agents Memory
       @return The agents Memory
    */
    public final Memory getMemory() {
        return memory;
    }

    /**
       Get the type of RescueObject that this agent represents
       @see RescueConstants#TYPE_CIVILIAN
       @see RescueConstants#TYPE_CAR
       @see RescueConstants#TYPE_FIRE_BRIGADE
       @see RescueConstants#TYPE_FIRE_STATION
       @see RescueConstants#TYPE_POLICE_FORCE
       @see RescueConstants#TYPE_POLICE_OFFICE
       @see RescueConstants#TYPE_AMBULANCE_TEAM
       @see RescueConstants#TYPE_AMBULANCE_CENTER
	*/
    public final int getType() {
        return type;
    }

    /**
       Get this agent's unique id, assigned by the kernel
	*/
    public final int getID() {
        return id;
    }

    /**
	   Enable debugging
	   @param file The File to log information to. 
	*/
    protected void enableDebug(String target) {
        debug = true;
        DebugWriter.register(this, this.toString(), target);
    }

    /**
	   Disable debugging
	*/
    protected void disableDebug() {
        debug = false;
    }

    /**
	 * Writes a transient Object to the log.
	 **/
    public final void logObject(Object obj) {
        if (debug) DebugWriter.logUserObject(this, obj, timeStep);
    }

    /**
       Initialise this agent. Subclasses that override this method should invoke super.initialise(knowledge,self) at some point.
       @param knowledge This agent's knowledge of the world
       @param self The RescueObject describing this agent
	*/
    protected void initialise(RescueObject[] knowledge, RescueObject self) {
        memory = generateMemory();
        memory.add(self, 0, RescueConstants.SOURCE_INITIAL);
        for (int i = 0; i < knowledge.length; ++i) {
            memory.add(knowledge[i], 0, RescueConstants.SOURCE_INITIAL);
        }
        if (debug) {
            DebugWriter.logInitialObjects(this, memory.getAllObjects());
            memory.addMemoryListener(new DebugMemoryListener(this));
        }
    }

    /**
       Construct a new Memory object for use by this Agent. This method allows Agents to customise their choice of Memory object. The default implementation returns a {@link HashMemory}.
       @return A new Memory object
	*/
    protected Memory generateMemory() {
        return new HashMemory();
    }

    /**
       Called after a KA_SENSE is received
	*/
    protected abstract void sense();

    /**
       Called after a KA_HEAR is received
       @param from The agent that sent the message
       @param msg The message body
	   @param channel The channel that this message was received on
    */
    protected void hear(int from, byte[] msg, byte channel) {
    }

    /**
       Send an AK_SAY message to the kernel. If this agent has already send too many messages this timestep then this will be silently ignored
       @param message The message
	*/
    protected final void say(byte[] message) {
        appendCommand(Command.SAY(id, message, message.length));
    }

    /**
       Send an AK_TELL message to the kernel. If this agent has already send too many messages this timestep then this will be silently ignored
       @param message The message
	*/
    protected final void tell(byte[] message, byte channel) {
        appendCommand(Command.TELL(id, message, message.length, channel));
    }
}
