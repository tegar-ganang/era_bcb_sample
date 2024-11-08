package jade.core.mobility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.InterruptedIOException;
import java.util.StringTokenizer;
import java.util.zip.*;
import jade.core.ServiceFinder;
import jade.core.HorizontalCommand;
import jade.core.VerticalCommand;
import jade.core.Command;
import jade.core.GenericCommand;
import jade.core.Service;
import jade.core.ServiceHelper;
import jade.core.BaseService;
import jade.core.ServiceException;
import jade.core.Sink;
import jade.core.Filter;
import jade.core.Node;
import jade.core.LifeCycle;
import jade.core.Profile;
import jade.core.Agent;
import jade.core.Agent.Interrupted;
import jade.core.AID;
import jade.core.CaseInsensitiveString;
import jade.core.ContainerID;
import jade.core.Location;
import jade.core.AgentContainer;
import jade.core.MainContainer;
import jade.core.AgentDescriptor;
import jade.core.ProfileException;
import jade.core.IMTPException;
import jade.core.NameClashException;
import jade.core.NotFoundException;
import jade.core.management.AgentManagementService;
import jade.core.management.AgentManagementSlice;
import jade.core.management.CodeLocator;
import jade.core.replication.MainReplicationHandle;
import jade.lang.acl.ACLMessage;
import jade.security.Credentials;
import jade.security.JADEPrincipal;
import jade.security.JADESecurityException;
import jade.security.CredentialsHelper;
import jade.util.leap.List;
import jade.util.leap.ArrayList;
import jade.util.leap.Map;
import jade.util.leap.HashMap;
import jade.util.Logger;

/**
 The JADE service to manage mobility-related agent life cycle: migration
 and clonation.
 
 @author Giovanni Rimassa - FRAMeTech s.r.l.
 @author Giovanni Caire - TILAB
 */
public class AgentMobilityService extends BaseService {

    public static final String NAME = AgentMobilitySlice.NAME;

    public static final int AP_TRANSIT = 7;

    public static final int AP_COPY = 8;

    public static final int AP_GONE = 9;

    private static final String[] OWNED_COMMANDS = new String[] { AgentMobilityHelper.REQUEST_MOVE, AgentMobilityHelper.REQUEST_CLONE, AgentMobilityHelper.INFORM_MOVED, AgentMobilityHelper.INFORM_CLONED };

    private static final int SIZE_JAR_BUFFER = 4096;

    static final boolean MIGRATION = false;

    static final boolean CLONING = true;

    static final boolean CREATE_AND_START = true;

    static final boolean CREATE_ONLY = false;

    static final boolean TRANSFER_ABORT = false;

    static final boolean TRANSFER_COMMIT = true;

    private final CommandSourceSink senderSink = new CommandSourceSink();

    private final CommandTargetSink receiverSink = new CommandTargetSink();

    private final Filter _outFilter = new CommandOutgoingFilter();

    private MainReplicationHandle replicationHandle;

    public void init(AgentContainer ac, Profile p) throws ProfileException {
        super.init(ac, p);
        myContainer = ac;
    }

    public void boot(Profile myProfile) throws ServiceException {
        replicationHandle = new MainReplicationHandle(this, myContainer.getServiceFinder());
    }

    public String getName() {
        return AgentMobilitySlice.NAME;
    }

    public Class getHorizontalInterface() {
        return AgentMobilitySlice.class;
    }

    public Service.Slice getLocalSlice() {
        return localSlice;
    }

    public ServiceHelper getHelper(Agent a) {
        return new AgentMobilityHelperImpl();
    }

    public Filter getCommandFilter(boolean direction) {
        if (direction == Filter.OUTGOING) {
            return _outFilter;
        } else return null;
    }

    public Sink getCommandSink(boolean side) {
        if (side == Sink.COMMAND_SOURCE) {
            return senderSink;
        } else {
            return receiverSink;
        }
    }

    public String[] getOwnedCommands() {
        return OWNED_COMMANDS;
    }

    /**
	 * Retrieve the name of the container where the classes of a given agent can be found
	 */
    public String getClassSite(Agent a) {
        return (String) sites.get(a);
    }

    private class CommandSourceSink implements Sink {

        public void consume(VerticalCommand cmd) {
            try {
                String name = cmd.getName();
                if (name.equals(AgentMobilityHelper.REQUEST_MOVE)) {
                    handleRequestMove(cmd);
                } else if (name.equals(AgentMobilityHelper.REQUEST_CLONE)) {
                    handleRequestClone(cmd);
                } else if (name.equals(AgentMobilityHelper.INFORM_MOVED)) {
                    handleInformMoved(cmd);
                } else if (name.equals(AgentMobilityHelper.INFORM_CLONED)) {
                    handleInformCloned(cmd);
                }
            } catch (IMTPException imtpe) {
                cmd.setReturnValue(imtpe);
            } catch (NotFoundException nfe) {
                cmd.setReturnValue(nfe);
            } catch (NameClashException nce) {
                cmd.setReturnValue(nce);
            } catch (JADESecurityException ae) {
                cmd.setReturnValue(ae);
            } catch (ServiceException se) {
                cmd.setReturnValue(new IMTPException("Service error", se));
            }
        }

        private void handleRequestMove(VerticalCommand cmd) throws IMTPException, ServiceException, NotFoundException {
            Object[] params = cmd.getParams();
            AID agentID = (AID) params[0];
            Location where = (Location) params[1];
            MainContainer impl = myContainer.getMain();
            if (impl != null) {
                ContainerID cid = impl.getContainerID(agentID);
                AgentMobilitySlice targetSlice = (AgentMobilitySlice) getSlice(cid.getName());
                try {
                    targetSlice.moveAgent(agentID, where);
                } catch (IMTPException imtpe) {
                    targetSlice = (AgentMobilitySlice) getFreshSlice(cid.getName());
                    targetSlice.moveAgent(agentID, where);
                }
            } else {
            }
        }

        private void handleRequestClone(VerticalCommand cmd) throws IMTPException, ServiceException, NotFoundException {
            Object[] params = cmd.getParams();
            AID agentID = (AID) params[0];
            Location where = (Location) params[1];
            String newName = (String) params[2];
            MainContainer impl = myContainer.getMain();
            if (impl != null) {
                ContainerID cid = impl.getContainerID(agentID);
                AgentMobilitySlice targetSlice = (AgentMobilitySlice) getSlice(cid.getName());
                try {
                    targetSlice.copyAgent(agentID, where, newName);
                } catch (IMTPException imtpe) {
                    targetSlice = (AgentMobilitySlice) getFreshSlice(cid.getName());
                    targetSlice.copyAgent(agentID, where, newName);
                }
            } else {
            }
        }

        private void handleInformMoved(VerticalCommand cmd) throws IMTPException, ServiceException, JADESecurityException, NotFoundException {
            Object[] params = cmd.getParams();
            AID agentID = (AID) params[0];
            Location where = (Location) params[1];
            if (myLogger.isLoggable(Logger.CONFIG)) myLogger.log(Logger.CONFIG, "Moving agent " + agentID.getName() + " on container " + where.getName());
            Agent a = myContainer.acquireLocalAgent(agentID);
            if (a == null) {
                myLogger.log(Logger.SEVERE, "Internal error: handleMove() called with a wrong name (" + agentID.getName() + ") !!!");
                return;
            }
            int transferState = 0;
            List messages = new ArrayList();
            AgentMobilitySlice dest = null;
            try {
                if (CaseInsensitiveString.equalsIgnoreCase(where.getName(), myContainer.here().getName())) {
                    return;
                }
                dest = (AgentMobilitySlice) getSlice(where.getName());
                if (dest == null) {
                    myLogger.log(Logger.SEVERE, "Destination " + where.getName() + " does not exist or does not support mobility");
                    return;
                }
                if (myLogger.isLoggable(Logger.FINE)) {
                    myLogger.log(Logger.FINE, "Destination container for agent " + agentID + " found");
                }
                transferState = 1;
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ObjectOutputStream encoder = new ObjectOutputStream(out);
                encoder.writeObject(a);
                byte[] bytes = out.toByteArray();
                if (myLogger.isLoggable(Logger.FINE)) {
                    myLogger.log(Logger.FINE, "Agent " + agentID.getName() + " correctly serialized");
                }
                String classSiteName = (String) sites.get(a);
                if (classSiteName == null) {
                    classSiteName = getLocalNode().getName();
                }
                try {
                    dest.createAgent(agentID, bytes, classSiteName, MIGRATION, CREATE_ONLY);
                } catch (IMTPException imtpe) {
                    dest = (AgentMobilitySlice) getFreshSlice(where.getName());
                    dest.createAgent(agentID, bytes, classSiteName, MIGRATION, CREATE_ONLY);
                }
                transferState = 2;
                if (myLogger.isLoggable(Logger.FINE)) {
                    myLogger.log(Logger.FINE, "Agent " + agentID.getName() + " correctly created on destination container");
                }
                AgentMobilitySlice mainSlice = (AgentMobilitySlice) getSlice(MAIN_SLICE);
                boolean transferResult = false;
                try {
                    transferResult = mainSlice.transferIdentity(agentID, (ContainerID) myContainer.here(), (ContainerID) where);
                } catch (IMTPException imtpe) {
                    mainSlice = (AgentMobilitySlice) getFreshSlice(MAIN_SLICE);
                    transferResult = mainSlice.transferIdentity(agentID, (ContainerID) myContainer.here(), (ContainerID) where);
                }
                transferState = 3;
                if (transferResult == TRANSFER_COMMIT) {
                    if (myLogger.isLoggable(Logger.FINE)) {
                        myLogger.log(Logger.FINE, "Identity of agent " + agentID.getName() + " correctly transferred");
                    }
                    myContainer.fillListFromMessageQueue(messages, a);
                    dest.handleTransferResult(agentID, transferResult, messages);
                    try {
                        a.changeStateTo(new LifeCycle(AP_GONE) {

                            public boolean alive() {
                                return false;
                            }
                        });
                        myContainer.removeLocalAgent(a.getAID());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    sites.remove(a);
                    if (myLogger.isLoggable(Logger.FINE)) {
                        myLogger.log(Logger.FINE, "Agent " + agentID.getName() + " correctly gone");
                    }
                } else {
                    myLogger.log(Logger.WARNING, "Error transferring identity of agent " + agentID.getName());
                    a.restoreBufferedState();
                    dest.handleTransferResult(agentID, transferResult, messages);
                    myLogger.log(Logger.WARNING, "Migration of agent " + agentID.getName() + "aborted");
                }
            } catch (IOException ioe) {
                myLogger.log(Logger.SEVERE, "Error in agent serialization. Abort transfer. " + ioe);
            } catch (JADESecurityException ae) {
                myLogger.log(Logger.SEVERE, "Permission to move not owned. Abort transfer. " + ae.getMessage());
            } catch (NotFoundException nfe) {
                if (transferState == 0) {
                    myLogger.log(Logger.SEVERE, "Destination container does not exist. Abort transfer. " + nfe.getMessage());
                } else if (transferState == 2) {
                    myLogger.log(Logger.SEVERE, "Transferring agent does not seem to be part of the platform. Abort transfer. " + nfe.getMessage());
                } else if (transferState == 3) {
                    myLogger.log(Logger.SEVERE, "Transferred agent not found on destination container. Can't roll back. " + nfe.getMessage());
                }
            } catch (NameClashException nce) {
                nce.printStackTrace();
            } catch (IMTPException imtpe) {
                if (transferState == 0) {
                    myLogger.log(Logger.SEVERE, "Can't retrieve destination container. Abort transfer. " + imtpe.getMessage());
                } else if (transferState == 1) {
                    myLogger.log(Logger.SEVERE, "Error creating agent on destination container. Abort transfer. " + imtpe.getMessage());
                } else if (transferState == 2) {
                    myLogger.log(Logger.SEVERE, "Error transferring agent identity. Abort transfer. " + imtpe.getMessage());
                    try {
                        dest.handleTransferResult(agentID, TRANSFER_ABORT, messages);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (transferState == 3) {
                    myLogger.log(Logger.SEVERE, "Error activating transferred agent. Can't roll back!!!. " + imtpe.getMessage());
                }
            } finally {
                if (transferState <= 2) {
                    a.restoreBufferedState();
                }
                myContainer.releaseLocalAgent(agentID);
            }
        }

        private void handleInformCloned(VerticalCommand cmd) throws IMTPException, NotFoundException, NameClashException, JADESecurityException {
            Object[] params = cmd.getParams();
            AID agentID = (AID) params[0];
            Location where = (Location) params[1];
            String newName = (String) params[2];
            try {
                String containerName = myContainer.getID().getName();
                Agent agent = myContainer.acquireLocalAgent(agentID);
                String codeContainerName = getClassSite(agent);
                myContainer.releaseLocalAgent(agentID);
                AgentManagementService amSrv = (AgentManagementService) myFinder.findService(AgentManagementService.NAME);
                CodeLocator codeLocator = amSrv.getCodeLocator();
                if (codeContainerName == null) codeContainerName = containerName;
                if (containerName.equals(codeContainerName)) {
                    if (codeLocator.isRegistered(agentID)) {
                        if (myLogger.isLoggable(Logger.FINE)) {
                            myLogger.log(Logger.FINE, " adding clone " + newName + " to code locator.");
                        }
                        codeLocator.cloneAgent(agentID, new AID(newName, AID.ISLOCALNAME));
                    }
                } else {
                    AgentMobilitySlice codeSlice = (AgentMobilitySlice) getSlice(codeContainerName);
                    try {
                        codeSlice.cloneCodeLocatorEntry(agentID, new AID(newName, AID.ISLOCALNAME));
                    } catch (IMTPException imtpe) {
                        codeSlice = (AgentMobilitySlice) getSlice(codeContainerName);
                        codeSlice.cloneCodeLocatorEntry(agentID, new AID(newName, AID.ISLOCALNAME));
                    }
                }
                if (myLogger.isLoggable(Logger.CONFIG)) myLogger.log(Logger.CONFIG, "Cloning agent " + agentID + " on container " + where.getName());
                Agent a = myContainer.acquireLocalAgent(agentID);
                if (a == null) {
                    if (myLogger.isLoggable(Logger.SEVERE)) myLogger.log(Logger.SEVERE, "Internal error: handleClone() called with a wrong name (" + agentID + ") !!!");
                    return;
                }
                AgentMobilitySlice dest = (AgentMobilitySlice) getSlice(where.getName());
                if (dest == null) {
                    myLogger.log(Logger.SEVERE, "Destination " + where.getName() + " does not exist or does not support mobility");
                    return;
                }
                if (myLogger.isLoggable(Logger.FINE)) myLogger.log(Logger.FINE, "Destination container for agent " + agentID + " found");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ObjectOutputStream encoder = new ObjectOutputStream(out);
                encoder.writeObject(a);
                byte[] bytes = out.toByteArray();
                if (myLogger.isLoggable(Logger.FINE)) myLogger.log(Logger.FINE, "Agent " + agentID + " correctly serialized");
                String classSiteName = (String) sites.get(a);
                if (classSiteName == null) {
                    classSiteName = getLocalNode().getName();
                }
                AID newID = new AID(newName, AID.ISLOCALNAME);
                try {
                    dest.createAgent(newID, bytes, classSiteName, CLONING, CREATE_AND_START);
                } catch (IMTPException imtpe) {
                    dest = (AgentMobilitySlice) getFreshSlice(where.getName());
                    dest.createAgent(newID, bytes, classSiteName, CLONING, CREATE_AND_START);
                }
                if (myLogger.isLoggable(Logger.FINE)) myLogger.log(Logger.FINE, "Cloned Agent " + newID + " correctly created on destination container");
            } catch (IOException ioe) {
                throw new IMTPException("I/O serialization error in handleInformCloned()", ioe);
            } catch (ServiceException se) {
                throw new IMTPException("Destination container not found in handleInformCloned()", se);
            } finally {
                myContainer.releaseLocalAgent(agentID);
            }
        }
    }

    private class CommandTargetSink implements Sink {

        public void consume(VerticalCommand cmd) {
            try {
                String name = cmd.getName();
                if (name.equals(AgentMobilityHelper.REQUEST_MOVE)) {
                    handleRequestMove(cmd);
                } else if (name.equals(AgentMobilityHelper.REQUEST_CLONE)) {
                    handleRequestClone(cmd);
                } else if (name.equals(AgentMobilityHelper.INFORM_MOVED)) {
                    handleInformMoved(cmd);
                } else if (name.equals(AgentMobilityHelper.INFORM_CLONED)) {
                    handleInformCloned(cmd);
                }
            } catch (Throwable t) {
                cmd.setReturnValue(t);
            }
        }

        private void handleRequestMove(VerticalCommand cmd) throws IMTPException, NotFoundException {
            Object[] params = cmd.getParams();
            AID agentID = (AID) params[0];
            Location where = (Location) params[1];
            moveAgent(agentID, where);
        }

        private void handleRequestClone(VerticalCommand cmd) throws IMTPException, NotFoundException {
            Object[] params = cmd.getParams();
            AID agentID = (AID) params[0];
            Location where = (Location) params[1];
            String newName = (String) params[2];
            copyAgent(agentID, where, newName);
        }

        private void handleInformMoved(VerticalCommand cmd) {
        }

        private void handleInformCloned(VerticalCommand cmd) throws JADESecurityException, NotFoundException, NameClashException {
            Object[] params = cmd.getParams();
            AID agentID = (AID) params[0];
            ContainerID cid = (ContainerID) params[1];
            Credentials creds = (Credentials) params[2];
            clonedAgent(agentID, cid, creds);
        }

        private void moveAgent(AID agentID, Location where) throws IMTPException, NotFoundException {
            Agent a = myContainer.acquireLocalAgent(agentID);
            if (a == null) {
                throw new NotFoundException("Move-Agent failed to find " + agentID);
            }
            a.doMove(where);
            myContainer.releaseLocalAgent(agentID);
        }

        private void copyAgent(AID agentID, Location where, String newName) throws IMTPException, NotFoundException {
            Agent a = myContainer.acquireLocalAgent(agentID);
            if (a == null) throw new NotFoundException("Clone-Agent failed to find " + agentID);
            a.doClone(where, newName);
            myContainer.releaseLocalAgent(agentID);
        }

        private void clonedAgent(AID agentID, ContainerID cid, Credentials credentials) throws JADESecurityException, NotFoundException, NameClashException {
            MainContainer impl = myContainer.getMain();
            if (impl != null) {
                String ownership = "NONE";
                if (credentials != null) {
                    JADEPrincipal ownerPr = credentials.getOwner();
                    if (ownerPr != null) {
                        ownership = ownerPr.getName();
                    }
                }
                bornAgent(agentID, cid, null, ownership, false);
                replicationHandle.invokeReplicatedMethod("bornAgent", new Object[] { agentID, cid, null, ownership, new Boolean(true) });
            }
        }
    }

    private class CommandOutgoingFilter extends Filter {

        protected boolean accept(VerticalCommand cmd) {
            String name = cmd.getName();
            if (name.equals(AgentManagementSlice.INFORM_KILLED)) {
                try {
                    handleInformKilled(cmd);
                } catch (NotFoundException nfe) {
                    if (myLogger.isLoggable(Logger.WARNING)) myLogger.log(Logger.WARNING, "CommandOutgoingFilter: Error deleting remote CodeLocator entry: " + nfe);
                } catch (ServiceException se) {
                    if (myLogger.isLoggable(Logger.WARNING)) myLogger.log(Logger.WARNING, "CommandOutgoingFilter: Error deleting remote CodeLocator entry: " + se);
                } catch (IMTPException imtpe) {
                    if (myLogger.isLoggable(Logger.WARNING)) myLogger.log(Logger.WARNING, "CommandOutgoingFilter: Error deleting remote CodeLocator entry: " + imtpe);
                }
            }
            return true;
        }

        private void handleInformKilled(VerticalCommand cmd) throws IMTPException, NotFoundException, ServiceException {
            Object[] params = cmd.getParams();
            AID target = (AID) params[0];
            if (myLogger.isLoggable(Logger.CONFIG)) myLogger.log(Logger.CONFIG, "Outgoing Filer accepting command INFORM_KILLED. Name is " + target.getName());
            String containerName = myContainer.getID().getName();
            Agent agent = myContainer.acquireLocalAgent(target);
            String codeContainerName = getClassSite(agent);
            myContainer.releaseLocalAgent(target);
            if (codeContainerName != null) {
                if (!containerName.equals(codeContainerName)) {
                    AgentMobilitySlice codeSlice = (AgentMobilitySlice) getSlice(codeContainerName);
                    if (codeSlice != null) {
                        try {
                            try {
                                codeSlice.removeCodeLocatorEntry(target);
                            } catch (IMTPException imtpe) {
                                codeSlice = (AgentMobilitySlice) getSlice(codeContainerName);
                                codeSlice.removeCodeLocatorEntry(target);
                            }
                        } catch (Exception e) {
                            myLogger.log(Logger.WARNING, "Error notifying home container " + codeContainerName + " of terminating agent " + target.getName(), e);
                        }
                    }
                }
            }
        }
    }

    /**
	 Inner mix-in class for this service: this class receives
	 commands through its <code>Filter</code> interface and serves
	 them, coordinating with remote parts of this service through
	 the <code>Slice</code> interface (that extends the
	 <code>Service.Slice</code> interface).
	 */
    private class ServiceComponent implements Service.Slice {

        public Service getService() {
            return AgentMobilityService.this;
        }

        public Node getNode() throws ServiceException {
            try {
                return AgentMobilityService.this.getLocalNode();
            } catch (IMTPException imtpe) {
                throw new ServiceException("Problem in contacting the IMTP Manager", imtpe);
            }
        }

        public VerticalCommand serve(HorizontalCommand cmd) {
            VerticalCommand result = null;
            try {
                String cmdName = cmd.getName();
                Object[] params = cmd.getParams();
                if (cmdName.equals(AgentMobilitySlice.H_CREATEAGENT)) {
                    AID agentID = (AID) params[0];
                    byte[] serializedInstance = (byte[]) params[1];
                    String classSiteName = (String) params[2];
                    boolean isCloned = ((Boolean) params[3]).booleanValue();
                    boolean startIt = ((Boolean) params[4]).booleanValue();
                    createAgent(agentID, serializedInstance, classSiteName, isCloned, startIt);
                } else if (cmdName.equals(AgentMobilitySlice.H_FETCHCLASSFILE)) {
                    String className = (String) params[0];
                    String agentName = (String) params[1];
                    cmd.setReturnValue(fetchClassFile(className, agentName));
                } else if (cmdName.equals(AgentMobilitySlice.H_MOVEAGENT)) {
                    GenericCommand gCmd = new GenericCommand(AgentMobilityHelper.REQUEST_MOVE, AgentMobilitySlice.NAME, null);
                    AID agentID = (AID) params[0];
                    Location where = (Location) params[1];
                    gCmd.addParam(agentID);
                    gCmd.addParam(where);
                    result = gCmd;
                } else if (cmdName.equals(AgentMobilitySlice.H_COPYAGENT)) {
                    GenericCommand gCmd = new GenericCommand(AgentMobilityHelper.REQUEST_CLONE, AgentMobilitySlice.NAME, null);
                    AID agentID = (AID) params[0];
                    Location where = (Location) params[1];
                    String newName = (String) params[2];
                    gCmd.addParam(agentID);
                    gCmd.addParam(where);
                    gCmd.addParam(newName);
                    result = gCmd;
                } else if (cmdName.equals(AgentMobilitySlice.H_PREPARE)) {
                    cmd.setReturnValue(new Boolean(prepare()));
                } else if (cmdName.equals(AgentMobilitySlice.H_TRANSFERIDENTITY)) {
                    AID agentID = (AID) params[0];
                    Location src = (Location) params[1];
                    Location dest = (Location) params[2];
                    cmd.setReturnValue(new Boolean(transferIdentity(agentID, src, dest)));
                } else if (cmdName.equals(AgentMobilitySlice.H_HANDLETRANSFERRESULT)) {
                    AID agentID = (AID) params[0];
                    boolean transferResult = ((Boolean) params[1]).booleanValue();
                    List messages = (List) params[2];
                    handleTransferResult(agentID, transferResult, messages);
                } else if (cmdName.equals(AgentMobilitySlice.H_CLONEDAGENT)) {
                    GenericCommand gCmd = new GenericCommand(AgentMobilityHelper.INFORM_CLONED, AgentMobilitySlice.NAME, null);
                    AID agentID = (AID) params[0];
                    ContainerID cid = (ContainerID) params[1];
                    Credentials creds = (Credentials) params[2];
                    gCmd.addParam(agentID);
                    gCmd.addParam(cid);
                    gCmd.addParam(creds);
                    result = gCmd;
                } else if (cmdName.equals(AgentMobilitySlice.H_CLONECODELOCATORENTRY)) {
                    AID oldAgentID = (AID) params[0];
                    AID newAgentID = (AID) params[1];
                    handleCloneCodeLocatorEntry(oldAgentID, newAgentID);
                } else if (cmdName.equals(AgentMobilitySlice.H_REMOVECODELOCATORENTRY)) {
                    AID agentID = (AID) params[0];
                    handleRemoveCodeLocatorEntry(agentID);
                }
            } catch (Throwable t) {
                cmd.setReturnValue(t);
                if (result != null) {
                    result.setReturnValue(t);
                }
            }
            return result;
        }

        private void createAgent(AID agentID, byte[] serializedInstance, String classSiteName, boolean isCloned, boolean startIt) throws IMTPException, ServiceException, NotFoundException, NameClashException, JADESecurityException {
            try {
                if (myLogger.isLoggable(Logger.CONFIG)) myLogger.log(Logger.CONFIG, "Incoming agent " + agentID.getName());
                ObjectInputStream in = new Deserializer(new ByteArrayInputStream(serializedInstance), agentID.getName(), classSiteName, myContainer.getServiceFinder());
                Agent instance = (Agent) in.readObject();
                if (myLogger.isLoggable(Logger.FINE)) myLogger.log(Logger.FINE, "Agent " + agentID + " reconstructed");
                Credentials agentCerts = null;
                if (isCloned) {
                    AgentMobilitySlice mainSlice = (AgentMobilitySlice) getSlice(MAIN_SLICE);
                    try {
                        mainSlice.clonedAgent(agentID, myContainer.getID(), agentCerts);
                    } catch (IMTPException imtpe) {
                        mainSlice = (AgentMobilitySlice) getFreshSlice(MAIN_SLICE);
                        mainSlice.clonedAgent(agentID, myContainer.getID(), agentCerts);
                    }
                }
                sites.put(instance, classSiteName);
                Agent old = myContainer.addLocalAgent(agentID, instance);
                if (myLogger.isLoggable(Logger.FINE)) myLogger.log(Logger.FINE, "Agent " + agentID.getName() + " inserted into LADT");
                if (startIt) {
                    myContainer.powerUpLocalAgent(agentID);
                }
            } catch (IOException ioe) {
                throw new IMTPException("An I/O error occurred during de-serialization", ioe);
            } catch (ClassNotFoundException cnfe) {
                throw new IMTPException("A class was not found during de-serialization", cnfe);
            } catch (Throwable t) {
                t.printStackTrace();
                throw new IMTPException("Unexpected error.", t);
            }
        }

        private byte[] fetchClassFile(String className, String agentName) throws IMTPException, ClassNotFoundException {
            if (myLogger.isLoggable(Logger.FINE)) myLogger.log(Logger.FINE, "Fetching class " + className);
            String fileName = className.replace('.', '/') + ".class";
            InputStream classStream = getClass().getClassLoader().getResourceAsStream(fileName);
            if (classStream == null) {
                classStream = ClassLoader.getSystemResourceAsStream(fileName);
            }
            if (classStream == null) {
                if (myLogger.isLoggable(Logger.FINER)) myLogger.log(Logger.FINER, "Class not found as a system resource. Try manually");
                classStream = manualGetResourceAsStream(fileName);
            }
            if (classStream == null && agentName != null) {
                try {
                    AgentManagementService amSrv = (AgentManagementService) myFinder.findService(AgentManagementService.NAME);
                    ClassLoader cLoader = amSrv.getCodeLocator().getAgentClassLoader(new AID(agentName, AID.ISGUID));
                    classStream = cLoader.getResourceAsStream(fileName);
                } catch (NullPointerException npe) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (classStream == null) {
                if (myLogger.isLoggable(Logger.WARNING)) {
                    myLogger.log(Logger.WARNING, "Class " + className + " not found");
                }
                throw new ClassNotFoundException(className);
            }
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] bytes = new byte[SIZE_JAR_BUFFER];
                int read = 0;
                DataInputStream dis = new DataInputStream(classStream);
                while ((read = dis.read(bytes)) >= 0) {
                    baos.write(bytes, 0, read);
                }
                dis.close();
                if (myLogger.isLoggable(Logger.FINER)) {
                    myLogger.log(Logger.FINER, "Class " + className + " fetched");
                }
                return (baos.toByteArray());
            } catch (IOException ioe) {
                throw new ClassNotFoundException("IOException reading class bytes. " + ioe.getMessage());
            }
        }

        private InputStream manualGetResourceAsStream(String fileName) {
            InputStream classStream = null;
            String currentCp = System.getProperty("java.class.path");
            StringTokenizer st = new StringTokenizer(currentCp, File.pathSeparator);
            while (st.hasMoreTokens()) {
                try {
                    String path = st.nextToken();
                    if (myLogger.isLoggable(Logger.FINER)) {
                        myLogger.log(Logger.FINER, "Searching in path " + path);
                    }
                    if (path.endsWith(".jar")) {
                        if (myLogger.isLoggable(Logger.FINER)) {
                            myLogger.log(Logger.FINER, "It's a jar file");
                        }
                        ClassInfo info = getClassStreamFromJar(fileName, path);
                        if (info != null) {
                            classStream = info.getClassStream();
                            break;
                        }
                    } else {
                        if (myLogger.isLoggable(Logger.FINER)) {
                            myLogger.log(Logger.FINER, "Trying file " + path + "/" + fileName);
                        }
                        File f = new File(path + "/" + fileName);
                        if (f.exists()) {
                            if (myLogger.isLoggable(Logger.FINER)) {
                                myLogger.log(Logger.FINER, "File exists");
                            }
                            classStream = new FileInputStream(f);
                            break;
                        }
                    }
                } catch (Exception e) {
                    if (myLogger.isLoggable(Logger.WARNING)) {
                        myLogger.log(Logger.WARNING, e.toString());
                    }
                }
            }
            return classStream;
        }

        private ClassInfo getClassStreamFromJar(String classFileName, String jarName) throws IOException {
            File f = new File(jarName);
            if (f.exists()) {
                if (myLogger.isLoggable(Logger.FINER)) {
                    myLogger.log(Logger.FINER, "Jar file exists");
                }
            }
            ZipFile zf = new ZipFile(f);
            ZipEntry e = zf.getEntry(classFileName);
            if (e != null) {
                if (myLogger.isLoggable(Logger.FINER)) {
                    myLogger.log(Logger.FINER, "Entry " + classFileName + " found");
                }
                return new ClassInfo(zf.getInputStream(e), (int) e.getSize());
            }
            return null;
        }

        /**
		 * Inner class ClassInfo
		 * This utility bean class is used only to keep together some pieces of information related to a class
		 */
        private class ClassInfo {

            private InputStream classStream;

            private int length = -1;

            public ClassInfo(InputStream is, int l) {
                classStream = is;
                length = l;
            }

            public InputStream getClassStream() {
                return classStream;
            }

            public int getLength() {
                return length;
            }
        }

        private void handleTransferResult(AID agentID, boolean result, List messages) throws IMTPException, NotFoundException {
            if (myLogger.isLoggable(Logger.FINER)) myLogger.log(Logger.FINER, "Activating incoming agent " + agentID);
            try {
                Agent agent = myContainer.acquireLocalAgent(agentID);
                if ((agent == null) || (agent.getState() != AP_TRANSIT)) {
                    throw new NotFoundException("handleTransferResult() unable to find a suitable agent.");
                }
                if (result == TRANSFER_ABORT) {
                    myContainer.removeLocalAgent(agentID);
                } else {
                    for (int i = messages.size(); i > 0; i--) {
                        agent.putBack((ACLMessage) messages.get(i - 1));
                    }
                    myContainer.powerUpLocalAgent(agentID);
                    if (myLogger.isLoggable(Logger.CONFIG)) myLogger.log(Logger.CONFIG, "Incoming agent " + agentID.getName() + " activated");
                }
            } finally {
                myContainer.releaseLocalAgent(agentID);
            }
        }

        private boolean prepare() {
            return true;
        }

        private boolean transferIdentity(AID agentID, Location src, Location dest) throws IMTPException, NotFoundException {
            if (myLogger.isLoggable(Logger.FINE)) myLogger.log(Logger.FINE, "Transferring identity of agent " + agentID + " from " + src.getName() + " to " + dest.getName());
            MainContainer impl = myContainer.getMain();
            if (impl != null) {
                AgentDescriptor ad = impl.acquireAgentDescriptor(agentID);
                if (ad != null) {
                    try {
                        AgentMobilitySlice srcSlice = (AgentMobilitySlice) getSlice(src.getName());
                        AgentMobilitySlice destSlice = (AgentMobilitySlice) getSlice(dest.getName());
                        boolean srcReady = false;
                        boolean destReady = false;
                        try {
                            srcReady = srcSlice.prepare();
                        } catch (IMTPException imtpe) {
                            srcSlice = (AgentMobilitySlice) getFreshSlice(src.getName());
                            srcReady = srcSlice.prepare();
                        }
                        if (myLogger.isLoggable(Logger.FINE)) myLogger.log(Logger.FINE, "Source " + src.getName() + " " + srcReady);
                        try {
                            destReady = destSlice.prepare();
                        } catch (IMTPException imtpe) {
                            destSlice = (AgentMobilitySlice) getFreshSlice(dest.getName());
                            destReady = destSlice.prepare();
                        }
                        if (myLogger.isLoggable(Logger.FINE)) myLogger.log(Logger.FINE, "Destination " + dest.getName() + " " + destReady);
                        if (srcReady && destReady) {
                            movedAgent(agentID, (ContainerID) src, (ContainerID) dest);
                            replicationHandle.invokeReplicatedMethod("movedAgent", new Object[] { agentID, (ContainerID) src, (ContainerID) dest });
                            return true;
                        } else {
                            return false;
                        }
                    } catch (Exception e) {
                        if (myLogger.isLoggable(Logger.WARNING)) myLogger.log(Logger.WARNING, "Link failure!");
                        return false;
                    } finally {
                        impl.releaseAgentDescriptor(agentID);
                    }
                } else {
                    throw new NotFoundException("Agent agentID not found");
                }
            } else {
                if (myLogger.isLoggable(Logger.WARNING)) myLogger.log(Logger.WARNING, "Not a main!");
                return false;
            }
        }

        private void handleCloneCodeLocatorEntry(AID oldAgentID, AID newAgentID) throws ServiceException, IMTPException, NotFoundException {
            AgentManagementService amSrv = (AgentManagementService) myFinder.findService(AgentManagementService.NAME);
            CodeLocator codeLocator = amSrv.getCodeLocator();
            if (codeLocator.isRegistered(oldAgentID)) {
                if (myLogger.isLoggable(Logger.FINE)) {
                    myLogger.log(Logger.FINE, " adding clone " + newAgentID.getName() + " to code locator.");
                }
                codeLocator.cloneAgent(oldAgentID, newAgentID);
            }
        }

        private void handleRemoveCodeLocatorEntry(AID agentID) throws IMTPException, ServiceException {
            if (myLogger.isLoggable(Logger.FINE)) {
                myLogger.log(Logger.FINE, "Target sink consuming command REMOVE_CODE_LOCATOR_ENTRY");
            }
            AgentManagementService amSrv = (AgentManagementService) myFinder.findService(AgentManagementService.NAME);
            CodeLocator codeLocator = amSrv.getCodeLocator();
            codeLocator.removeAgent(agentID);
        }
    }

    public void movedAgent(AID agentID, ContainerID src, ContainerID dest) throws NotFoundException {
        myContainer.getMain().movedAgent(agentID, src, dest);
    }

    public void bornAgent(AID agentID, ContainerID cid, JADEPrincipal principal, String ownership, boolean forceReplacement) throws NameClashException, NotFoundException {
        MainContainer impl = myContainer.getMain();
        try {
            impl.bornAgent(agentID, cid, principal, ownership, forceReplacement);
        } catch (NameClashException nce) {
            try {
                ContainerID oldCid = impl.getContainerID(agentID);
                Node n = impl.getContainerNode(oldCid).getNode();
                n.ping(false);
                throw nce;
            } catch (NameClashException nce2) {
                throw nce2;
            } catch (Exception e) {
                impl.bornAgent(agentID, cid, null, ownership, true);
            }
        }
    }

    /**
	 * Inner class Deserializer
	 */
    private class Deserializer extends ObjectInputStream {

        private String agentName;

        private String classSiteName;

        private ServiceFinder finder;

        /**
		 */
        public Deserializer(InputStream inner, String an, String sliceName, ServiceFinder sf) throws IOException {
            super(inner);
            agentName = an;
            classSiteName = sliceName;
            finder = sf;
        }

        /**
		 */
        protected Class resolveClass(ObjectStreamClass v) throws IOException, ClassNotFoundException {
            String key = createClassLoaderKey(agentName, classSiteName);
            MobileAgentClassLoader cl = (MobileAgentClassLoader) loaders.get(key);
            if (cl == null) {
                try {
                    cl = new MobileAgentClassLoader(agentName, classSiteName, finder, AgentMobilityService.this.getClass().getClassLoader());
                    loaders.put(key, cl);
                } catch (IMTPException imtpe) {
                    imtpe.printStackTrace();
                    throw new ClassNotFoundException("Error creating MobileAgent ClassLoader. " + imtpe.getMessage());
                } catch (ServiceException se) {
                    se.printStackTrace();
                    throw new ClassNotFoundException("Error creating MobileAgent ClassLoader. " + se.getMessage());
                }
            }
            Class c;
            try {
                c = Class.forName(v.getName(), true, cl);
            } catch (ClassNotFoundException ex) {
                c = (Class) primitiveJavaClasses.get(v.getName());
                if (c == null) {
                    throw ex;
                }
            }
            return c;
        }

        private String createClassLoaderKey(String agentName, String classSiteName) {
            return agentName + '#' + classSiteName;
        }
    }

    private static final java.util.HashMap primitiveJavaClasses = new java.util.HashMap(8, 1.0F);

    static {
        primitiveJavaClasses.put("boolean", boolean.class);
        primitiveJavaClasses.put("byte", byte.class);
        primitiveJavaClasses.put("char", char.class);
        primitiveJavaClasses.put("short", short.class);
        primitiveJavaClasses.put("int", int.class);
        primitiveJavaClasses.put("long", long.class);
        primitiveJavaClasses.put("float", float.class);
        primitiveJavaClasses.put("double", double.class);
        primitiveJavaClasses.put("void", void.class);
    }

    private final Map loaders = new HashMap();

    private final Map sites = new HashMap();

    private AgentContainer myContainer;

    private final ServiceComponent localSlice = new ServiceComponent();

    /**
	 Inner class AgentMobilityHelperImpl.
	 The actual implementation of the AgentMobilityHelper interface.
	 */
    private class AgentMobilityHelperImpl implements AgentMobilityHelper {

        private Agent myAgent;

        private Movable myMovable;

        public void init(Agent a) {
            myAgent = a;
        }

        public void registerMovable(Movable m) {
            myMovable = m;
        }

        public void move(Location destination) {
            myAgent.changeStateTo(new TransitLifeCycle(destination, myMovable, AgentMobilityService.this));
        }

        public void clone(Location destination, String newName) {
            myAgent.changeStateTo(new CopyLifeCycle(destination, newName, myMovable, AgentMobilityService.this));
        }

        public ClassLoader getContainerClassLoader(String codeSourceContainer, ClassLoader parent) throws ServiceException {
            try {
                return new MobileAgentClassLoader(null, codeSourceContainer, AgentMobilityService.this.myFinder, parent);
            } catch (IMTPException imtpe) {
                throw new ServiceException("Communication error retrieving code source container slice.", imtpe);
            }
        }
    }

    /**
	 Inner class TransitLifeCycle
	 */
    private static class TransitLifeCycle extends LifeCycle {

        private Location myDestination;

        private Movable myMovable;

        private transient AgentMobilityService myService;

        private Logger myLogger;

        private boolean firstTime = true;

        private boolean messageAware = false;

        private TransitLifeCycle(Location l, Movable m, AgentMobilityService s) {
            super(AP_TRANSIT);
            myDestination = l;
            myMovable = m;
            myService = s;
            myLogger = Logger.getMyLogger(myService.getName());
        }

        public void init() {
            myAgent.restoreBufferedState();
            if (myMovable != null) {
                myMovable.afterMove();
            }
        }

        public void execute() throws JADESecurityException, InterruptedException, InterruptedIOException {
            try {
                if (firstTime) {
                    firstTime = false;
                    if (myMovable != null) {
                        messageAware = true;
                        myMovable.beforeMove();
                        messageAware = false;
                    }
                    informMoved(myAgent.getAID(), myDestination);
                }
            } catch (Exception e) {
                if (myAgent.getState() == myState) {
                    myAgent.restoreBufferedState();
                    myDestination = null;
                    if (e instanceof JADESecurityException) {
                        throw (JADESecurityException) e;
                    } else {
                        e.printStackTrace();
                    }
                } else {
                    throw new Interrupted();
                }
            }
        }

        public void end() {
            if (myLogger.isLoggable(Logger.SEVERE)) myLogger.log(Logger.SEVERE, "***  Agent " + myAgent.getName() + " moved in a forbidden situation ***");
            myAgent.clean(true);
        }

        public boolean transitionTo(LifeCycle newLF) {
            int s = newLF.getState();
            return (s == AP_GONE || s == Agent.AP_ACTIVE || s == Agent.AP_DELETED);
        }

        public boolean isMessageAware() {
            return messageAware;
        }

        public void informMoved(AID agentID, Location where) throws ServiceException, JADESecurityException, NotFoundException, IMTPException {
            GenericCommand cmd = new GenericCommand(AgentMobilityHelper.INFORM_MOVED, AgentMobilitySlice.NAME, null);
            cmd.addParam(agentID);
            cmd.addParam(where);
            myService.initCredentials(cmd, agentID);
            Object lastException = myService.submit(cmd);
            if (lastException != null) {
                if (lastException instanceof JADESecurityException) {
                    throw (JADESecurityException) lastException;
                }
                if (lastException instanceof NotFoundException) {
                    throw (NotFoundException) lastException;
                }
                if (lastException instanceof IMTPException) {
                    throw (IMTPException) lastException;
                }
            }
        }
    }

    /**
	 Inner class CopyLifeCycle
	 */
    private static class CopyLifeCycle extends LifeCycle {

        private Location myDestination;

        private String myNewName;

        private Movable myMovable;

        private transient AgentMobilityService myService;

        private Logger myLogger;

        private boolean firstTime = true;

        private boolean messageAware = false;

        private CopyLifeCycle(Location l, String newName, Movable m, AgentMobilityService s) {
            super(AP_COPY);
            myDestination = l;
            myNewName = newName;
            myMovable = m;
            myService = s;
            myLogger = Logger.getMyLogger(myService.getName());
        }

        public void init() {
            myAgent.restoreBufferedState();
            if (myMovable != null) {
                myMovable.afterClone();
            }
        }

        public void execute() throws JADESecurityException, InterruptedException, InterruptedIOException {
            try {
                if (firstTime) {
                    firstTime = false;
                    if (myMovable != null) {
                        messageAware = true;
                        myMovable.beforeClone();
                        messageAware = false;
                    }
                    informCloned(myAgent.getAID(), myDestination, myNewName);
                }
            } catch (Exception e) {
                if (myAgent.getState() == myState) {
                    myDestination = null;
                    myNewName = null;
                    myAgent.restoreBufferedState();
                    if (e instanceof JADESecurityException) {
                        throw (JADESecurityException) e;
                    } else {
                        e.printStackTrace();
                        return;
                    }
                } else {
                    throw new Interrupted();
                }
            }
            myAgent.restoreBufferedState();
        }

        public boolean transitionTo(LifeCycle newLF) {
            int s = newLF.getState();
            return (s == Agent.AP_ACTIVE || s == Agent.AP_DELETED);
        }

        public boolean isMessageAware() {
            return messageAware;
        }

        public void end() {
            if (myLogger.isLoggable(Logger.SEVERE)) myLogger.log(Logger.SEVERE, "***  Agent " + myAgent.getName() + " cloned in a forbidden situation ***");
            myAgent.clean(true);
        }

        public void informCloned(AID agentID, Location where, String newName) throws ServiceException, JADESecurityException, IMTPException, NotFoundException, NameClashException {
            GenericCommand cmd = new GenericCommand(AgentMobilityHelper.INFORM_CLONED, AgentMobilitySlice.NAME, null);
            cmd.addParam(agentID);
            cmd.addParam(where);
            cmd.addParam(newName);
            myService.initCredentials(cmd, agentID);
            Object lastException = myService.submit(cmd);
            if (lastException != null) {
                if (lastException instanceof JADESecurityException) {
                    throw (JADESecurityException) lastException;
                }
                if (lastException instanceof NotFoundException) {
                    throw (NotFoundException) lastException;
                }
                if (lastException instanceof IMTPException) {
                    throw (IMTPException) lastException;
                }
                if (lastException instanceof NameClashException) {
                    throw (NameClashException) lastException;
                }
            }
        }
    }

    protected Service.Slice getFreshSlice(String name) throws ServiceException {
        return super.getFreshSlice(name);
    }

    private void initCredentials(Command cmd, AID id) {
        Agent agent = myContainer.acquireLocalAgent(id);
        if (agent != null) {
            try {
                CredentialsHelper ch = (CredentialsHelper) agent.getHelper("jade.core.security.Security");
                cmd.setPrincipal(ch.getPrincipal());
                cmd.setCredentials(ch.getCredentials());
            } catch (ServiceException se) {
            }
        }
        myContainer.releaseLocalAgent(id);
    }
}
