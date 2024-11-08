package fi.hip.gb.disk.transport.jgroups;

import java.io.File;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.MembershipListener;
import org.jgroups.Message;
import org.jgroups.SuspectedException;
import org.jgroups.TimeoutException;
import org.jgroups.View;
import org.jgroups.blocks.GroupRequest;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.jmx.JmxConfigurator;
import org.jgroups.util.Util;
import fi.hip.gb.disk.conf.Config;
import fi.hip.gb.disk.conf.InitService;
import fi.hip.gb.disk.info.GroupSystem;
import fi.hip.gb.disk.transport.webdav.WebdavDiskStore;
import fi.hip.gb.disk.utils.FileUtils;

/**
 * Handler for JGroups group communication and file transfer.
 * Messages to the members can be send either synchronously with 
 * {@link JGroupsServer#sendSyncMessage(Message)}
 * or asynchronously with {@link JGroupsServer#sendAsyncMessage(Message)}.
 * <p>
 * Incoming files are transfered and handled with 
 * {@link fi.hip.gb.disk.transport.jgroups.FileMessage} implementation.
 * <p>
 * The same channel is used for dynamic discovery of storage elements.
 * Front-ends ask the state of all joining group members, and store
 * the list of {@link fi.hip.gb.disk.transport.jgroups.DiscoveryMessage}
 * messages received from the storage elements. This information is then used by 
 * scheduler throught {@link GroupSystem} interface.
 * 
 * @author Juho Karppinen
 */
public class JGroupsServer implements RequestHandler, MembershipListener, GroupSystem {

    /** channel for communication */
    private static org.jgroups.JChannel channel;

    /** message dispatcher */
    private MessageDispatcher md;

    /** name of the group */
    private final String GROUPNAME = "GBDiskGroup";

    /** mbean name */
    private final String MBEAN_NAME = "GridBlocks:channel=" + GROUPNAME;

    /** list of other servers and their status information, only used by front-ends */
    private Hashtable<String, DiscoveryMessage> members = new Hashtable<String, DiscoveryMessage>();

    private static Log log = LogFactory.getLog(JGroupsServer.class);

    /**
     * Initalises the server and registed listeners for incoming messages.
     * JGroups channel is static and initialised only once. Calling this
     * constructor multiple times has no effect unless 
     * the server is first stopped with {@link JGroupsServer#stop()} method.
     */
    public JGroupsServer() {
        if (channel == null) initialise(Config.getGroupInfoProperties());
    }

    /**
     * Initalise the server and registed listeners for incoming messages.
     * @param channelProperties file for the JGroups channel properties
     */
    @SuppressWarnings("unchecked")
    private void initialise(String channelProperties) {
        log.debug("Loading JGroups properties from " + channelProperties);
        try {
            if (channel == null) {
                log.debug("No JChannel MBean found, loading manually..");
                channel = new org.jgroups.JChannel(Config.getGroupInfoProperties());
                channel.setOpt(Channel.LOCAL, Boolean.TRUE);
                channel.setOpt(Channel.AUTO_RECONNECT, Boolean.TRUE);
            }
            md = new MessageDispatcher(channel, null, this, this);
            channel.connect(GROUPNAME);
            log.info("Connected to group " + GROUPNAME);
            MBeanServer server = InitService.findMBeanServer();
            if (server != null) {
                try {
                    log.info("Registering MBean " + MBEAN_NAME);
                    JmxConfigurator.registerChannel(channel, server, "GridBlocks", MBEAN_NAME, true);
                } catch (Exception e) {
                    log.error("could not start JGroups mbean");
                }
            }
            if (!Config.isFrontEnd()) {
                this.members.put(getLocalAddress(), getLocalStorage());
            }
        } catch (ChannelException ce) {
            log.error("Could not join channel " + ce.getMessage(), ce);
        }
    }

    @SuppressWarnings("unchecked")
    public void stop() {
        if (channel != null) {
            log.info("Stopping jgroups channel");
            MBeanServer server = InitService.findMBeanServer();
            if (server != null) {
                String name = "GridBlocks:type=protocol,cluster=" + channel.getClusterName();
                try {
                    log.info("Removing MBean " + name);
                    JmxConfigurator.unregisterChannel(server, new ObjectName(name));
                    JmxConfigurator.unregisterProtocols(server, channel, name);
                } catch (Exception e) {
                    log.error("could not stop " + name);
                }
            }
            this.md.stop();
            channel.disconnect();
            channel.close();
        }
    }

    /**
     * Gets the address of the local JGroups server.
     * @return hostname:ip
     */
    public String getLocalAddress() {
        return channel.getLocalAddress().toString();
    }

    public DiscoveryMessage getLocalStorage() {
        long usedBytes = FileUtils.getResourceSize(new File(Config.getSiloDir()));
        int quota = Config.getQuota();
        if (Config.isFrontEnd()) quota = -1;
        String contactString = "";
        if (Config.getTransport().equals("jgroups")) contactString = Config.getTransport() + "://" + this.getLocalAddress(); else contactString = WebdavDiskStore.getStorageURL();
        return new DiscoveryMessage(getLocalAddress(), contactString, usedBytes / 1000000, quota);
    }

    /**
     * Sends a new message to the channel and gets a return value.
     * @param msg message containing target endpoint and payload data 
     * @throws SuspectedException 
     * @throws TimeoutException 
     * @throws Exception if return type was an exception
     */
    public Object sendSyncMessage(Message msg) throws TimeoutException, SuspectedException, Exception {
        Object o = md.sendMessage(msg, GroupRequest.GET_ALL, 60000);
        if (o instanceof Exception) {
            log.error("Exception of type " + o.getClass().getName() + ": " + o.toString(), (Exception) o);
            throw (Exception) o;
        }
        return o;
    }

    /**
     * Sends a new message to the channel.
     * @param msg message containing target endpoint and payload data 
     * @throws TimeoutException
     * @throws SuspectedException
     */
    public void sendAsyncMessage(Message msg) throws TimeoutException, SuspectedException {
        md.sendMessage(msg, GroupRequest.GET_NONE, 0);
    }

    /**
     * Handle incoming messages. In case of incoming file transfer, the payload is
     * always {@link fi.hip.gb.disk.transport.jgroups.FileMessage} object.
     * @param msg message
     * @return object to return to the client
     */
    public Object handle(final Message msg) {
        Object obj = msg.getObject();
        if (obj instanceof FileMessage) {
            FileMessage fileMsg = (FileMessage) obj;
            return fileMsg.handle();
        } else if (obj instanceof DiscoveryMessage) {
            DiscoveryMessage storage = getLocalStorage();
            log.info("Discovery request from " + msg.getSrc() + " offering " + storage.getQuota() + " megabytes of space (" + storage.getUsedSpace() + " used) on " + storage.getContactString());
            return storage;
        } else if (obj instanceof Message) {
            log.debug("*** msg: message is " + Util.printMessage((Message) obj));
            Util.dumpStack(false);
        } else if (obj != null) {
            log.debug("*** obj is " + obj.getClass() + ", hdrs are" + msg.printObjectHeaders());
        }
        return null;
    }

    /**
     * New member has arrived. Front-end servers ask the state of the
     * new member.
     * @see org.jgroups.MembershipListener#viewAccepted(org.jgroups.View)
     */
    @SuppressWarnings("unchecked")
    public void viewAccepted(final View new_view) {
        if (!Config.isFrontEnd()) {
            return;
        }
        HashSet<String> currentItems = new HashSet<String>();
        for (Iterator<Address> iter = new_view.getMembers().iterator(); iter.hasNext(); ) {
            Address element = iter.next();
            if (JGroupsServer.this.members.containsKey(element.toString()) == false) {
                log.info("member joins " + element);
                new Thread(new StateRequester(element)).start();
            }
            currentItems.add(element.toString());
        }
        if (this.members.size() > 0) {
            for (Iterator<String> iter = this.members.keySet().iterator(); iter.hasNext(); ) {
                String element = iter.next();
                if (currentItems.contains(element) == false) {
                    log.info("member has disconnected " + element);
                    iter.remove();
                }
            }
        }
    }

    public void suspect(Address suspected_mbr) {
    }

    public void block() {
    }

    /**
     * Prints information about available JGroups servers.
     * 
     * @return list of jgroups server urls separated with new line character
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (String host : getServers()) {
            sb.append(host).append("\n");
        }
        return sb.toString();
    }

    /**
     * Gets all members of the JGroups channel.
     * @return an array of jgroup server addesses in format hostname:port
     */
    public String[] getServers() {
        Vector<String> members = new Vector<String>();
        for (Object m : md.getChannel().getView().getMembers()) {
            Address address = (Address) m;
            members.add(address.toString());
        }
        return members.toArray(new String[0]);
    }

    /**
     * Generally this is only useful if we are acting as
     * front-end and collect these informations from other
     * members of the JGroups channel.
     * 
     * 
     * @see fi.hip.gb.disk.info.GroupSystem#getDiscoveredHosts()
     */
    public DiscoveryMessage[] getDiscoveredHosts() {
        return this.members.values().toArray(new DiscoveryMessage[this.members.size()]);
    }

    public void refreshState() {
        for (Object m : md.getChannel().getView().getMembers()) {
            new Thread(new StateRequester((Address) m)).start();
        }
    }

    /**
     * Asks discovery state from the group member.
     */
    class StateRequester implements Runnable {

        private Address member;

        /**
         * @param member address for the JGroups service
         */
        public StateRequester(Address member) {
            this.member = member;
        }

        public void run() {
            Message req = new Message(member, null, new DiscoveryMessage());
            try {
                log.debug("asking state from member " + member);
                DiscoveryMessage res = (DiscoveryMessage) sendSyncMessage(req);
                if (res != null) {
                    log.info("state for " + (res.getQuota() > 0 ? "storage-element " : " front-end ") + res + "# " + members.size());
                    JGroupsServer.this.members.put(member.toString(), res);
                } else {
                    log.warn("null state from member " + member);
                }
            } catch (Exception e) {
                log.error("failed to receive state from group member " + member, e);
            }
        }
    }
}
