package simulation;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.*;
import javax.jms.TopicConnectionFactory;
import javax.naming.Context;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.event.EventContext;
import networkSimulator.VirtualModeEditorPanel;
import org.jgroups.stack.IpAddress;
import eu.popeye.middleware.groupmanagement.management.Workgroup;
import eu.popeye.middleware.groupmanagement.management.WorkgroupActionListener;
import eu.popeye.middleware.groupmanagement.membership.Member;
import eu.popeye.middleware.groupmanagement.membership.MemberActionListener;
import eu.popeye.middleware.groupmanagement.membership.MemberImpl;
import eu.popeye.middleware.naming.search.CtxSearch;
import eu.popeye.networkabstraction.communication.ApplicationMessageListener;
import eu.popeye.networkabstraction.communication.CommunicationChannel;
import eu.popeye.networkabstraction.communication.TimeoutException;
import eu.popeye.networkabstraction.communication.basic.util.WorkgroupAlreadyCreatedException;
import eu.popeye.networkabstraction.communication.basic.util.WorkgroupNotExistException;
import eu.popeye.networkabstraction.communication.message.PopeyeMessage;

public class SimulationServer {

    private final String ADDRESS_PREFFIX = "192.168.0.";

    private int ADDRESS_SUFFIX = 1;

    private int nextPort = 0;

    private final String USERNAME_PREFIX = "USER_";

    private VirtualModeEditorPanel virtualPanel;

    private LinkedList<Member> memberList;

    private HashMap<String, HashMap<Member, Workgroup>> joinedWorkgroups;

    private HashMap<String, FlatContext> flatContext;

    private HashMap<String, HashMap<Member, CommunicationChannelImpl>> commChannels;

    private HashMap<String, HashMap<Member, LinkedList<MemberActionListener>>> memberActionListeners;

    private HashMap<String, HashMap<Member, LinkedList<ApplicationMessageListener>>> applicationMessageListeners;

    private HashMap<Member, LinkedList<WorkgroupActionListener>> workgroupActionListeners;

    private static SimulationServer instance = null;

    public static void createInstance(VirtualModeEditorPanel virtualPanel) {
        instance = new SimulationServer(virtualPanel);
    }

    /**
	 * Returns the unique instance of the SimulationServer
	 * @return
	 * @throws Exception 
	 */
    public static SimulationServer getInstance() {
        return instance;
    }

    private SimulationServer(VirtualModeEditorPanel virtualPanel) {
        this.virtualPanel = virtualPanel;
        memberList = new LinkedList<Member>();
        joinedWorkgroups = new HashMap<String, HashMap<Member, Workgroup>>();
        flatContext = new HashMap<String, FlatContext>();
        commChannels = new HashMap<String, HashMap<Member, CommunicationChannelImpl>>();
        memberActionListeners = new HashMap<String, HashMap<Member, LinkedList<MemberActionListener>>>();
        applicationMessageListeners = new HashMap<String, HashMap<Member, LinkedList<ApplicationMessageListener>>>();
        workgroupActionListeners = new HashMap<Member, LinkedList<WorkgroupActionListener>>();
        String args[] = { "/tmp/NAMING" };
        Setup.main(args);
    }

    /**
	 * Creates a new Member
	 * @return
	 */
    public Member createMember(String username) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
            keyGen.initialize(1024);
            KeyPair keyPair = keyGen.genKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            Member member = new MemberImpl(InetAddress.getByName(ADDRESS_PREFFIX + ADDRESS_SUFFIX), nextPort, USERNAME_PREFIX + nextPort, username);
            member.setUsername(username);
            ADDRESS_SUFFIX++;
            nextPort++;
            this.memberList.add(member);
            return member;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Creates and joins a new workgroup in the MANET.
	 * 
	 * @param name The name of the new workgroup. It must be different than 
	 * existing workgroup names.
	 * @throws WorkgroupAlreadyCreatedException if the group already exists
	 */
    public Workgroup createWorkgroup(String name, Member member, MemberActionListener listener) throws WorkgroupAlreadyCreatedException {
        if (!this.joinedWorkgroups.containsKey(name)) {
            Workgroup workgroup = new WorkgroupImpl(name, member);
            HashMap<Member, Workgroup> workgroups = new HashMap<Member, Workgroup>();
            workgroups.put(member, workgroup);
            this.joinedWorkgroups.put(name, workgroups);
            LinkedList<MemberActionListener> listeners = new LinkedList<MemberActionListener>();
            listeners.add(listener);
            HashMap<Member, LinkedList<MemberActionListener>> groupListeners = new HashMap<Member, LinkedList<MemberActionListener>>();
            groupListeners.put(member, listeners);
            this.memberActionListeners.put(name, groupListeners);
            String ldapServerName = "localhost";
            String rootdn = "cn=Manager, o=jndiTest";
            String rootpass = "secret";
            Date date = new Date();
            System.out.println(date.getTime());
            String rootContext = "o=" + name + "_" + date.getTime();
            try {
                Properties env = new Properties();
                env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                env.put(Context.PROVIDER_URL, "ldap://" + ldapServerName + "/");
                env.put(Context.SECURITY_PRINCIPAL, rootdn);
                env.put(Context.SECURITY_CREDENTIALS, rootpass);
                DirContext ctx = new InitialDirContext(env);
                ctx.createSubcontext(rootContext);
                env.put(Context.PROVIDER_URL, "ldap://" + ldapServerName + "/" + rootContext);
                FlatContext ctx2 = new FlatContext(env);
                this.flatContext.put(name, ctx2);
            } catch (NameAlreadyBoundException nabe) {
                System.err.println(rootContext + " has already been bound!");
            } catch (Exception e) {
                System.err.println(e);
            }
            return workgroup;
        } else {
            throw new WorkgroupAlreadyCreatedException(new Throwable("Cannot create a workgroup with a name already in use."));
        }
    }

    /**
	 * Joins an existing workgroup in the MANET. It is necessary to have some 
	 * group membership manager to know which groups exist in the MANET 
	 * @param name The name of the existing workgroup.
	 * @throws WorkgroupNotExistException if the group does not exist
	 */
    public Workgroup joinWorkgroup(String name, Member member, MemberActionListener listener) throws WorkgroupNotExistException {
        if (this.joinedWorkgroups.containsKey(name)) {
            if (this.joinedWorkgroups.get(name).containsKey(member)) {
                this.memberActionListeners.get(name).get(member).add(listener);
                return this.joinedWorkgroups.get(name).get(member);
            } else {
                Workgroup workgroup = new WorkgroupImpl(name, member);
                this.joinedWorkgroups.get(name).put(member, workgroup);
                LinkedList<MemberActionListener> listeners = new LinkedList<MemberActionListener>();
                listeners.add(listener);
                this.memberActionListeners.get(name).put(member, listeners);
                return workgroup;
            }
        } else {
            throw new WorkgroupNotExistException(new Throwable("Cannot join a non-existing group."));
        }
    }

    /**
	 * Checks if the user has joined the selected group
	 * @param name
	 * @return true if this group is currently joined
	 */
    public boolean hasJoined(String name, Member member) {
        return this.joinedWorkgroups.get(name).containsKey(member);
    }

    /**
	 * 
	 * @param name
	 */
    public void leaveWorkgroup(String groupName, Member member) {
        this.joinedWorkgroups.get(groupName).remove(member);
        Iterator it = this.commChannels.keySet().iterator();
        while (it.hasNext()) {
            String channelID = (String) it.next();
            if (channelID.startsWith(groupName)) {
                this.commChannels.get(channelID).remove(member);
            }
        }
        this.memberActionListeners.get(groupName).remove(member);
        it = this.applicationMessageListeners.keySet().iterator();
        while (it.hasNext()) {
            String channelID = (String) it.next();
            if (channelID.startsWith(groupName)) {
                this.applicationMessageListeners.get(channelID).remove(member);
            }
        }
    }

    /**
	 * Removes the group from the group list 
	 * @param name
	 */
    public void terminateWorkgroup(String name) {
    }

    /**
	 * Returns the name of the created workgroups
	 */
    public LinkedList getExistingWorkgroupNames() {
        LinkedList list = new LinkedList();
        Iterator it = this.joinedWorkgroups.keySet().iterator();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list;
    }

    public Enumeration getJoinedWorkgroupNames(Member member) {
        Hashtable table = new Hashtable();
        Iterator it = this.joinedWorkgroups.keySet().iterator();
        while (it.hasNext()) {
            String groupName = (String) it.next();
            if (this.joinedWorkgroups.get(groupName).containsKey(member)) {
                table.put(member, member);
            }
        }
        return table.keys();
    }

    public Workgroup getWorkgroup(String name, Member member) {
        return this.joinedWorkgroups.get(name).get(member);
    }

    /**
	 * Adds a listener that will be notified about group list changes
	 */
    public void addWorkgroupActionListener(Member member, WorkgroupActionListener listener) {
        if (this.workgroupActionListeners.containsKey(member)) {
            this.workgroupActionListeners.get(member).add(listener);
        } else {
            LinkedList<WorkgroupActionListener> list = new LinkedList<WorkgroupActionListener>();
            list.add(listener);
            this.workgroupActionListeners.put(member, list);
        }
    }

    /**
	 * Removes a listener that was notified about group list changes
	 */
    public void removeWorkgroupActionListener(Member member, WorkgroupActionListener listener) {
        this.workgroupActionListeners.get(member).remove(listener);
    }

    public LinkedList getWorkgroupActionListeners(Member member) {
        return this.workgroupActionListeners.get(member);
    }

    /**
	 * Registers a member action listener to the current group
	 * @param listener
	 */
    public void addMemberActionListener(String groupName, Member member, MemberActionListener listener) {
        this.memberActionListeners.get(groupName).get(member).add(listener);
    }

    /**
	 * Deregisters a member action listener to the current group
	 * @param listener
	 */
    public void removeMemberActionListener(String groupName, Member member, MemberActionListener listener) {
        this.memberActionListeners.get(groupName).get(member).remove(listener);
    }

    /**
	 * Creates a named communication channel
	 * @param channelName
	 */
    public void createNamedCommunicationChannel(String groupName, String channelName, Member member) {
        String channelID = this.getChannelID(groupName, channelName);
        if (!this.commChannels.containsKey(channelID)) {
            Workgroup workgroup = this.joinedWorkgroups.get(groupName).get(member);
            CommunicationChannelImpl channel = new CommunicationChannelImpl(channelID, (WorkgroupImpl) workgroup);
            HashMap<Member, CommunicationChannelImpl> map = new HashMap<Member, CommunicationChannelImpl>();
            map.put(member, channel);
            this.commChannels.put(channelID, map);
            this.applicationMessageListeners.put(channelID, new HashMap<Member, LinkedList<ApplicationMessageListener>>());
        } else {
            Workgroup workgroup = this.joinedWorkgroups.get(groupName).get(member);
            CommunicationChannelImpl channel = new CommunicationChannelImpl(channelID, (WorkgroupImpl) workgroup);
            this.commChannels.get(channelID).put(member, channel);
        }
    }

    /**
	 * Destroys a named communication channel
	 * @param channelName
	 */
    public void destroyNamedCommunicationChannel(String channelID, Member member) {
        this.commChannels.get(channelID).remove(member);
        this.applicationMessageListeners.get(channelID).remove(member);
    }

    /**
	 * Returns an already created named communication channel from a group
	 */
    public CommunicationChannel getNamedCommunicationChannel(String groupName, String channelName, Member member) {
        return this.commChannels.get(this.getChannelID(groupName, channelName)).get(member);
    }

    /**
	 * Returns the default communication channel from a group
	 */
    public CommunicationChannel getDefaultCommunicationChannel() {
        return null;
    }

    /**
	 * Returns a topicConnectionFactory (JMS) in order to create Topic, Publishers and Subscribers
	 * @return the TopicConnectionFactory attached to this group
	 */
    public TopicConnectionFactory getTopicConnectionFactory() {
        return null;
    }

    /**
	 * Returns a list of Members that are connected to this group
	 * @return the list of members connected to this group
	 */
    public List<Member> getMembers(String groupName) {
        List<Member> memberList = new LinkedList<Member>();
        Iterator it = this.joinedWorkgroups.get(groupName).keySet().iterator();
        while (it.hasNext()) {
            memberList.add((Member) it.next());
        }
        return memberList;
    }

    /**
	 * Returns the member representation that matches
	 * the given InetAddress
	 * @return the member with this InetAddress
	 */
    public Member getMember(InetAddress address) {
        Iterator it = this.memberList.iterator();
        while (it.hasNext()) {
            Member member = (Member) it.next();
            if (member.getInetAddress().equals(address)) {
                return member;
            }
        }
        return null;
    }

    /**
	 * Returns the member representation that matches
	 * the given username
	 * @return the member with this suername
	 */
    private Member getMember(String username) {
        Iterator it = this.memberList.iterator();
        while (it.hasNext()) {
            Member member = (Member) it.next();
            if (member.getUsername().equals(username)) {
                return member;
            }
        }
        return null;
    }

    /**
	 * Returns the context (naming service) of this group
	 * @return the EventContext attached to this group
	 */
    public EventContext getContext(String groupName) {
        return this.flatContext.get(groupName);
    }

    /**
	 * Returns the context search (naming service search object) of this group
	 * @return the CtxSearch attached to this group
	 */
    public CtxSearch getContextSearch(String groupName) {
        return this.flatContext.get(groupName);
    }

    public void addApplicationMessageListener(String channelID, Member member, ApplicationMessageListener listener) {
        if (this.applicationMessageListeners.get(channelID).containsKey(member)) {
            this.applicationMessageListeners.get(channelID).get(member).add(listener);
        } else {
            LinkedList<ApplicationMessageListener> list = new LinkedList<ApplicationMessageListener>();
            list.add(listener);
            this.applicationMessageListeners.get(channelID).put(member, list);
        }
    }

    public void removeApplicationMessageListener(String channelID, Member member, ApplicationMessageListener listener) {
        this.applicationMessageListeners.get(channelID).get(member).remove(listener);
    }

    public Collection getApplicationMessageListener(String channelID, Member member) {
        return this.applicationMessageListeners.get(channelID).get(member);
    }

    public void send(String channelID, Member member, PopeyeMessage msg) {
        SendThread thread = new SendThread(channelID, member, msg);
        thread.start();
    }

    private class SendThread extends Thread {

        private String channelID;

        private Member member;

        private PopeyeMessage msg;

        public SendThread(String channelID, Member member, PopeyeMessage msg) {
            this.channelID = channelID;
            this.member = member;
            this.msg = msg;
        }

        public void run() {
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bout);
                out.writeObject(msg);
                out.flush();
                ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray()));
                PopeyeMessage msg2 = (PopeyeMessage) in.readObject();
                in.close();
                commChannels.get(channelID).get(member).receive(msg2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendGroup(String channelID, PopeyeMessage msg) {
        SendGroupThread thread = new SendGroupThread(channelID, msg);
        thread.start();
    }

    private class SendGroupThread extends Thread {

        private String channelID;

        private PopeyeMessage msg;

        public SendGroupThread(String channelID, PopeyeMessage msg) {
            this.channelID = channelID;
            this.msg = msg;
        }

        public void run() {
            if (commChannels.containsKey(channelID)) {
                Iterator channelIterator = commChannels.get(channelID).values().iterator();
                while (channelIterator.hasNext()) {
                    try {
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        ObjectOutputStream out = new ObjectOutputStream(bout);
                        out.writeObject(msg);
                        out.flush();
                        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray()));
                        PopeyeMessage msg2 = (PopeyeMessage) in.readObject();
                        in.close();
                        ((CommunicationChannelImpl) channelIterator.next()).receive(msg2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void sendGroupByUnicast(String channelID, PopeyeMessage msg) {
        sendGroup(channelID, msg);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("##############################################\n");
        sb.append("NETWORK MEMBERS:\n");
        Iterator it = this.memberList.iterator();
        while (it.hasNext()) sb.append(it.next().toString() + "\n");
        sb.append("GROUP MEMBERS:\n");
        it = this.joinedWorkgroups.keySet().iterator();
        while (it.hasNext()) {
            String groupName = (String) it.next();
            sb.append(groupName + ": ");
            Iterator it2 = this.joinedWorkgroups.get(groupName).keySet().iterator();
            while (it2.hasNext()) {
                sb.append(it2.next() + " ");
            }
            sb.append("\n");
        }
        sb.append("COMMUNICATION CHANNEL MEMBERS:\n");
        it = this.commChannels.keySet().iterator();
        while (it.hasNext()) {
            String channelID = (String) it.next();
            sb.append(channelID + ": ");
            Iterator it2 = this.commChannels.get(channelID).keySet().iterator();
            while (it2.hasNext()) {
                sb.append(it2.next() + " ");
            }
            sb.append("\n");
        }
        sb.append("##############################################\n");
        return sb.toString();
    }

    private String getChannelID(String groupName, String channelName) {
        return groupName + "#" + channelName;
    }

    public List<Member> getClusterMembers(String username) {
        List<String> usernameList = this.virtualPanel.getClusterMembers(username);
        List<Member> memberList = new LinkedList<Member>();
        Iterator it = usernameList.iterator();
        while (it.hasNext()) {
            memberList.add(this.getMember((String) it.next()));
        }
        return memberList;
    }

    public Member getLocalMember(String username) {
        return this.getMember(username);
    }

    public List<Member> getManetMembers() {
        List<String> usernameList = this.virtualPanel.getManetMembers();
        List<Member> memberList = new LinkedList<Member>();
        Iterator it = usernameList.iterator();
        while (it.hasNext()) {
            memberList.add(this.getMember((String) it.next()));
        }
        return memberList;
    }

    public IpAddress getSuperPeerIpAddress(String username) {
        return null;
    }
}
