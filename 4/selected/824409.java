package com.continuent.tungsten.manager.gcs;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.log4j.Logger;
import org.continuent.hedera.adapters.MessageListener;
import org.continuent.hedera.adapters.PullPushAdapter;
import org.continuent.hedera.channel.AbstractReliableGroupChannel;
import org.continuent.hedera.channel.ChannelException;
import org.continuent.hedera.channel.JGroupsReliableGroupChannel;
import org.continuent.hedera.channel.NotConnectedException;
import org.continuent.hedera.common.Group;
import org.continuent.hedera.common.GroupIdentifier;
import org.continuent.hedera.common.IpAddress;
import org.continuent.hedera.common.Member;
import org.continuent.hedera.factory.AbstractGroupCommunicationFactory;
import org.continuent.hedera.gms.AbstractGroupMembershipService;
import org.continuent.hedera.gms.AlreadyMemberException;
import org.continuent.hedera.gms.GroupMembershipListener;
import com.continuent.tungsten.manager.handler.event.MonitoringEvent;
import com.continuent.tungsten.commons.cluster.resource.ResourceState;
import com.continuent.tungsten.commons.cluster.resource.notification.ClusterResourceNotification;
import com.continuent.tungsten.commons.cluster.resource.notification.ManagerHeartbeat;
import com.continuent.tungsten.commons.cluster.resource.notification.ManagerNotification;
import com.continuent.tungsten.commons.config.TungstenProperties;
import com.continuent.tungsten.commons.patterns.event.EventDispatcher;
import com.continuent.tungsten.commons.patterns.notification.NotificationGroupMember;
import com.continuent.tungsten.commons.patterns.notification.ResourceNotificationException;
import com.continuent.tungsten.commons.patterns.notification.ResourceNotificationListener;
import com.continuent.tungsten.commons.patterns.notification.ResourceNotifier;
import com.continuent.tungsten.commons.utils.CLUtils;
import com.continuent.tungsten.manager.common.ClusterManagerID;
import com.continuent.tungsten.manager.core.EventRouter;
import com.continuent.tungsten.manager.core.EventRouterImpl;
import com.continuent.tungsten.manager.core.HandlerManager;
import com.continuent.tungsten.manager.core.ManagerConf;
import com.continuent.tungsten.manager.core.QueueManager;
import com.continuent.tungsten.manager.core.ServiceManager;
import com.continuent.tungsten.manager.exception.ManagerException;
import com.continuent.tungsten.manager.gcs.gms.GroupCompositionMessage;
import com.continuent.tungsten.manager.gcs.gms.GroupPartitionMessage;
import com.continuent.tungsten.manager.gcs.gms.MemberStatus;
import com.continuent.tungsten.manager.gcs.gms.MemberStatusMessage;
import com.continuent.tungsten.manager.handler.event.ActionEvent;
import com.continuent.tungsten.manager.handler.event.ActionResponseEvent;
import com.continuent.tungsten.manager.handler.event.Event;
import com.continuent.tungsten.manager.handler.event.EventIdentifier;
import com.continuent.tungsten.manager.handler.event.ExceptionEvent;
import com.continuent.tungsten.manager.handler.event.ManagerViewUpdateEvent;
import com.continuent.tungsten.manager.handler.event.SystemEvent;

/**
 * @author edward.archibald@continuent.com
 */
public class ClusterCommunicationService implements GroupCommunicationListener, GroupCommunication, GroupMembershipListener, GroupMembershipManager, MessageListener, ResourceNotifier {

    static final Logger logger = Logger.getLogger(ClusterCommunicationService.class);

    private HandlerManager handlerManager = null;

    private QueueManager queueManager = null;

    private EventRouter eventRouter = null;

    private GroupIdentifier gid;

    private AbstractReliableGroupChannel channel;

    private AbstractGroupMembershipService gms;

    private PullPushAdapter adapter;

    AbstractGroupCommunicationFactory factory;

    TungstenProperties gcProperties;

    private int joinWaitSeconds = JOIN_WAIT_DEFAULT;

    public static int JOIN_WAIT_DEFAULT = 600;

    public static final int DUMP_MAX_EVENT = 1000;

    private ManagerException joinException;

    private ClusterManagerID clusterManagerID;

    private ClusterMemberIDMapping memberIDMapping;

    private String groupName;

    private String memberName;

    private int weight;

    private int fileServerPort;

    private static int FILE_PORT = 9998;

    private BlockingQueue<GroupCommunicationMessage> receivedMessageQueue = new LinkedBlockingQueue<GroupCommunicationMessage>();

    private TreeMap<String, ClusterManagerID> memberMap = new TreeMap<String, ClusterManagerID>();

    private TreeMap<String, ClusterManagerID> memberByGcsId = new TreeMap<String, ClusterManagerID>();

    private TreeMap<String, ClusterManagerID> prospectiveMemberMap = new TreeMap<String, ClusterManagerID>();

    private TreeMap<String, ClusterManagerID> prospectiveMemberByGcsId = new TreeMap<String, ClusterManagerID>();

    private TreeMap<Member, ClusterManagerID> failedManagers = new TreeMap<Member, ClusterManagerID>();

    private ServiceManager serviceMgr = null;

    private EventDispatcher eventDispatcher;

    private boolean heartbeatStarted = false;

    protected static ArrayList<ResourceNotificationListener> listeners = new ArrayList<ResourceNotificationListener>();

    /**
     * Hedera backend for Group Communication Since Hedera returns messages and
     * membership changes in different callbacks, to preserve the mutual order
     * of messages and membership changes all callbacks are made synchronized
     */
    public ClusterCommunicationService(ServiceManager serviceMgr, EventDispatcher eventDispatcher, TungstenProperties gcProperties, TungstenProperties managementProperties, HandlerManager handlerManager, QueueManager queueManager) throws GroupCommunicationException {
        this.serviceMgr = serviceMgr;
        this.eventDispatcher = eventDispatcher;
        this.gcProperties = gcProperties;
        this.handlerManager = handlerManager;
        this.queueManager = queueManager;
        String hederaFactory = gcProperties.getString("hedera.factory", "org.continuent.hedera.factory.JGroupsGroupCommunicationFactory", false);
        try {
            factory = (AbstractGroupCommunicationFactory) Class.forName(hederaFactory).newInstance();
        } catch (Exception e) {
            throw new GroupCommunicationException(GroupCommunicationException.Type.NotConnected, String.format("Error while loading class %s, reason=%s", hederaFactory, e.getLocalizedMessage()), e);
        }
        clusterManagerID = null;
        memberIDMapping = new ClusterMemberIDMapping();
        String fileServerPortString = managementProperties.getString(ManagerConf.FILE_SERVER_PORT);
        String joinWaitTimeString = managementProperties.getString(ManagerConf.JOIN_WAIT_TIME_SECS, "30", false);
        try {
            fileServerPort = Integer.parseInt(fileServerPortString);
        } catch (Exception e) {
            fileServerPort = FILE_PORT;
        }
        try {
            joinWaitSeconds = Integer.parseInt(joinWaitTimeString);
        } catch (Exception e) {
            joinWaitSeconds = JOIN_WAIT_DEFAULT;
        }
        joinException = null;
    }

    public void init(String groupName) throws GroupCommunicationException {
        GroupCommunicationMessageDispatcher dispatcher = new GroupCommunicationMessageDispatcher(eventDispatcher, receivedMessageQueue, this, this);
        dispatcher.addListener(serviceMgr.getQueueManager());
        dispatcher.start();
        gid = new GroupIdentifier(groupName);
        Object[] ret;
        try {
            ret = factory.createChannelAndGroupMembershipService(gcProperties.getProperties(), gid);
        } catch (NotConnectedException e) {
            throw new GroupCommunicationException(GroupCommunicationException.Type.NotConnected, e);
        } catch (ChannelException e) {
            throw new GroupCommunicationException(GroupCommunicationException.Type.Channel, e);
        } catch (UnknownHostException e) {
            throw new GroupCommunicationException(GroupCommunicationException.Type.UnknownHost, e);
        } finally {
            factory.dispose();
        }
        channel = (AbstractReliableGroupChannel) ret[0];
        gms = (AbstractGroupMembershipService) ret[1];
        adapter = new PullPushAdapter(channel, this);
        gms.registerGroupMembershipListener(this);
        adapter.start();
    }

    public synchronized void join(String memberName, String groupName, int weight, long timeout) throws GroupCommunicationException {
        Member member = null;
        String exceptionMessage = String.format("Member %s is unable to join the group %s", memberName, groupName);
        try {
            logger.debug("Joining the channel");
            channel.join();
            member = channel.getLocalMembership();
            clusterManagerID = new ClusterManagerID(member, memberName, groupName, weight, member.getAddress().toString(), fileServerPort);
            addMember(clusterManagerID);
            clusterManagerID.setIsMe(true);
            if (!heartbeatStarted) startHeartbeatService();
            for (Member existingMember : channel.getGroup().getMembers()) {
                joinMember(existingMember, channel.getGroup().getGroupIdentifier());
            }
            eventRouter = new EventRouterImpl(getClusterManagerID(), handlerManager, queueManager);
        } catch (NotConnectedException e) {
            logger.debug("Not connected");
            throw new GroupCommunicationException(GroupCommunicationException.Type.NotConnected, exceptionMessage, e);
        } catch (ChannelException e) {
            logger.debug("channel exception");
            throw new GroupCommunicationException(GroupCommunicationException.Type.Channel, exceptionMessage, e);
        } catch (AlreadyMemberException e) {
            logger.debug("already member");
            throw new GroupCommunicationException(GroupCommunicationException.Type.AlreadyMember, exceptionMessage, e);
        } finally {
            logger.debug("finally block");
            if (member == null) cleanup();
        }
    }

    /********************************************************************************************/
    public void leave(String reason) throws GroupCommunicationException {
        cleanup();
    }

    public synchronized void send(Serializable msg) throws GroupCommunicationException {
        try {
            channel.send(msg);
        } catch (Exception e) {
            throw new GroupCommunicationException(GroupCommunicationException.Type.Channel, String.format("send() failed, msg class=%s, reason=%s", msg.getClass().getSimpleName(), e), e);
        }
    }

    public void receive(Serializable msg) {
        GroupCommunicationMessage gcMsg = null;
        if (msg instanceof Event) {
            gcMsg = new GroupCommunicationMessage(GroupCommunicationMessageTypes.APPLICATION, msg);
        } else if (msg instanceof MonitoringEvent) {
            gcMsg = new GroupCommunicationMessage(GroupCommunicationMessageTypes.MONITORING, msg);
        } else if (msg instanceof SystemEvent) {
            gcMsg = new GroupCommunicationMessage(GroupCommunicationMessageTypes.SYSTEM, msg);
        }
        receivedMessageQueue.add(gcMsg);
    }

    public void receiveMessage(Event event) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Received event:%s", event));
        }
        try {
            eventRouter.onEvent(event, getMemberList());
        } catch (ManagerException me) {
            ExceptionEvent exceptionEvent = null;
            if (event instanceof ActionEvent) {
                ActionEvent ae = (ActionEvent) event;
                exceptionEvent = new ExceptionEvent(ae.getEventID(), ae.getEventID(), ae.getOriginalAddress(), me);
            } else if (event instanceof ActionResponseEvent) {
                ActionResponseEvent response = (ActionResponseEvent) event;
                exceptionEvent = new ExceptionEvent(response.getEventID(), response.getEventID(), response.getSourceAddress(), me);
            }
            try {
                sendEvent(exceptionEvent);
            } catch (ManagerException sendException) {
                logger.error("unable to send message", sendException);
            }
        }
    }

    public EventIdentifier sendEvent(Event event) throws ManagerException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Sending event: " + event.toString());
            }
            event.getEventID().setViewID(getViewID());
            send(event);
        } catch (Exception e) {
            throw new ManagerException(ManagerException.Type.SEND_EVENT_ERROR, String.format("Failed to send event of type %s, connectionID=%d reason=%s", event.getClass(), event.getEventID().getConnectionID(), e.getLocalizedMessage()), e);
        }
        return event.getEventID();
    }

    public void receiveMembershipMessage(GroupCommunicationEvent e) {
        logger.error("receiveMembershipMessage(NOT IMPLEMENTED!)");
    }

    /**
     * Implementations for methods of GroupMembershipListener
     */
    public void joinMember(Member m, GroupIdentifier gid) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("JOINED(%s)", m.getAddress()));
        }
        MemberStatusMessage memberStatus = new MemberStatusMessage(MemberStatus.JOINED, m, gid);
        GroupCommunicationMessage gcMsg = new GroupCommunicationMessage(GroupCommunicationMessageTypes.MEMBERSTATUS, memberStatus);
        receivedMessageQueue.add(gcMsg);
    }

    public void failedMember(Member failed, GroupIdentifier gid, Member sender) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("FAILED(%s) by %s", failed.getAddress(), sender.getAddress()));
        }
        MemberStatusMessage memberStatus = new MemberStatusMessage(MemberStatus.FAILED, failed, gid, sender);
        GroupCommunicationMessage gcMsg = new GroupCommunicationMessage(GroupCommunicationMessageTypes.MEMBERSTATUS, memberStatus);
        receivedMessageQueue.add(gcMsg);
    }

    public void quitMember(Member m, GroupIdentifier gid) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("GCS:QUIT(%s)", m.getAddress()));
        }
        MemberStatusMessage memberStatus = new MemberStatusMessage(MemberStatus.LEFT, m, gid);
        GroupCommunicationMessage gcMsg = new GroupCommunicationMessage(GroupCommunicationMessageTypes.MEMBERSTATUS, memberStatus);
        receivedMessageQueue.add(gcMsg);
    }

    public void groupComposition(Group g, IpAddress sender, int gmsStatus) {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("GCS:GROUP(%s) from %s", printGroup(g), sender));
        }
        GroupCompositionMessage groupComposition = new GroupCompositionMessage(g, sender, gmsStatus);
        GroupCommunicationMessage gcMsg = new GroupCommunicationMessage(GroupCommunicationMessageTypes.GROUPCOMPOSITION, groupComposition);
        receivedMessageQueue.add(gcMsg);
    }

    private String printGroup(Group g) {
        StringBuilder builder = new StringBuilder();
        for (Member member : g.getMembers()) {
            builder.append(String.format("%s ", member.getAddress()));
        }
        return builder.toString().trim();
    }

    @SuppressWarnings("unchecked")
    public void networkPartition(GroupIdentifier gid, List mergedGroupCompositions) {
        GroupPartitionMessage partitionMessage = new GroupPartitionMessage(gid, mergedGroupCompositions);
        GroupCommunicationMessage gcMsg = new GroupCommunicationMessage(GroupCommunicationMessageTypes.GROUPPARTITION, partitionMessage);
        receivedMessageQueue.add(gcMsg);
    }

    /**
     * Cleanup function. Must always be called from protected context.
     * 
     * @throws GroupCommunicationException
     */
    private synchronized void cleanup() throws GroupCommunicationException {
        adapter.stop();
        gms.unregisterGroupMembershipListener(this);
        try {
            channel.quit();
        } catch (NotConnectedException e) {
            throw new GroupCommunicationException(GroupCommunicationException.Type.NotConnected, e);
        } catch (ChannelException e) {
            throw new GroupCommunicationException(GroupCommunicationException.Type.Channel, e);
        }
    }

    protected void finalize() throws Throwable {
        cleanup();
        super.finalize();
    }

    public void promoteMember(ClusterManagerID memberToPromote) {
        if (memberWasPromoted(memberToPromote)) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("PROMOTE(%s)", memberToPromote.getMemberName()));
            }
        }
    }

    public boolean memberWasPromoted(ClusterManagerID memberToPromote) {
        boolean memberWasPromoted = false;
        synchronized (memberMap) {
            ClusterManagerID promotedMember = prospectiveMemberMap.get(memberToPromote.getMemberName());
            if (promotedMember != null) {
                logger.info(String.format("PROMOTED(%s)", promotedMember.getMemberName()));
                addMember(promotedMember);
                prospectiveMemberMap.remove(promotedMember.getMemberName());
                memberWasPromoted = true;
            }
        }
        return memberWasPromoted;
    }

    public void promoteProspectiveMembers() {
        if (logger.isDebugEnabled()) {
            logger.debug("Promoting members");
        }
        for (ClusterManagerID member : getProspectiveMemberMap().values()) {
            if (member.isOnline() || member.equals(getClusterManagerID())) promoteMember(member);
        }
    }

    /**
     * Implementation details
     */
    public void removeMember(ClusterManagerID member) {
        synchronized (memberMap) {
            memberByGcsId.remove(member.getMember().getAddress().toString());
            prospectiveMemberByGcsId.remove(member.getMember().getAddress().toString());
            memberMap.remove(member.getMemberName());
            prospectiveMemberMap.remove(member.getMemberName());
            if (!failedManagers.containsKey(member.getMember())) {
                failedManagers.put(member.getMember(), member);
            }
            ManagerNotification notification = new ManagerNotification(member.getGroupName(), member.getMemberName(), member.getMemberName(), ResourceState.STOPPED, getClass().getSimpleName());
            logger.info(String.format("MEMBER '%s' IS NOW STOPPED", member.getMemberName()));
            try {
                notifyListeners(notification);
            } catch (ResourceNotificationException r) {
                logger.warn("Unable to notify listeners of membership change.", r);
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info(String.format("REMOVE(%s) yields %s", member.getMemberName(), formatView()));
        }
    }

    public void removeMember(Member gcsMember) {
        ClusterManagerID member = getMember(gcsMember);
        if (member != null) {
            removeMember(member);
            if (!failedManagers.containsKey(member.getMember())) {
                logger.info(String.format("Adding manager '%s' to failed manager list", member.getMemberName()));
                failedManagers.put(member.getMember(), member);
            }
        }
    }

    public ClusterManagerID getMember(ClusterManagerID member) {
        synchronized (memberMap) {
            return memberMap.get(member.getMemberName());
        }
    }

    public ClusterManagerID getMember(Member gcsMember) {
        synchronized (memberMap) {
            return memberByGcsId.get(gcsMember.getAddress().toString());
        }
    }

    public void addProspectiveMember(ClusterManagerID prospectiveMember) {
        synchronized (prospectiveMemberMap) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("PROSPECT(%s)", prospectiveMember.getMemberName()));
            }
            prospectiveMemberMap.put(prospectiveMember.getMemberName(), prospectiveMember);
            prospectiveMemberByGcsId.put(prospectiveMember.getMember().getAddress().toString(), prospectiveMember);
        }
    }

    public ClusterManagerID getProspectiveMember(String memberName) {
        synchronized (memberMap) {
            return prospectiveMemberMap.get(memberName);
        }
    }

    public TreeMap<String, ClusterManagerID> getProspectiveMemberMap() {
        synchronized (memberMap) {
            return new TreeMap<String, ClusterManagerID>(prospectiveMemberMap);
        }
    }

    public boolean isCoordinator() {
        return getCoordinator().equals(clusterManagerID.getMemberName()) || isIsolated();
    }

    public boolean isCoordinator(String memberName) {
        return getCoordinator().equals(memberName);
    }

    public boolean isIsolated() {
        synchronized (memberMap) {
            if (memberMap.size() == 1 && failedManagers.size() > 0) {
                if (logger.isDebugEnabled()) logger.debug(String.format("MANAGER %s is ISOLATED", getMemberName()));
                return true;
            }
        }
        return false;
    }

    public boolean isInPartition() {
        logger.warn(String.format("DETERMINING IF MEMBER %s IS IN A PARTITION BY ITSELF", clusterManagerID.getMemberName()));
        List<Object> membersInPartition = new ArrayList<Object>();
        int notReachableCount = 0;
        for (ClusterManagerID managerID : failedManagers.values()) {
            if (!pingHost(managerID.getMemberName())) {
                notReachableCount++;
            } else {
                if (!membersInPartition.contains(managerID)) membersInPartition.add(managerID);
            }
        }
        if ((notReachableCount > 0) && (notReachableCount == failedManagers.size())) {
            logger.warn(String.format("MEMBER %s IS IN A PARTITION BY ITSELF", clusterManagerID.getMemberName()));
            return true;
        }
        failedManagers.clear();
        logger.info(String.format("MEMBER %s IS IN A PARTITION WITH REACHABLE NODES:\n%s\n", clusterManagerID.getMemberName(), CLUtils.listToString(membersInPartition)));
        return false;
    }

    public String getCoordinator() {
        Member coordinator = channel.getCoordinator();
        if (coordinator == null) return getClusterManagerID().getMemberName();
        synchronized (memberByGcsId) {
            ClusterManagerID currentCoordinator = memberByGcsId.get(coordinator.getAddress().toString());
            if (currentCoordinator != null) return currentCoordinator.getMemberName();
            return getClusterManagerID().getMemberName();
        }
    }

    public BlockingQueue<GroupCommunicationMessage> getReceivedMessageQueue() {
        return receivedMessageQueue;
    }

    /*****************************************************************************************
     * MembershipManagerImpl
     */
    public ClusterManagerID getClusterManagerID() {
        return clusterManagerID;
    }

    public String getGroupName() {
        return groupName;
    }

    public synchronized TreeMap<String, ClusterManagerID> getUuidToServerIDMap() {
        return memberIDMapping.getUUIDToMemberMap();
    }

    public synchronized HashMap<String, Set<String>> getNameToUuidMap() {
        return memberIDMapping.memberNameToUuidMap;
    }

    public int getJoinWaitSeconds() {
        return joinWaitSeconds;
    }

    public ManagerException getJoinException() {
        return joinException;
    }

    public String getMemberName() {
        return memberName;
    }

    public int getWeight() {
        return weight;
    }

    public int getFileServerPort() {
        return fileServerPort;
    }

    public AbstractReliableGroupChannel getChannel() {
        return channel;
    }

    public static int getFILE_PORT() {
        return FILE_PORT;
    }

    public GroupCommunicationListener getListener() {
        return this;
    }

    public GroupIdentifier getGid() {
        return gid;
    }

    public AbstractGroupMembershipService getGms() {
        return gms;
    }

    public PullPushAdapter getAdapter() {
        return adapter;
    }

    public AbstractGroupCommunicationFactory getFactory() {
        return factory;
    }

    public TungstenProperties getGroupCommProperties() {
        return gcProperties;
    }

    public static Logger getLogger() {
        return logger;
    }

    public List<ClusterManagerID> getMemberList() {
        synchronized (memberMap) {
            List<ClusterManagerID> copy = new ArrayList<ClusterManagerID>(memberMap.values());
            return copy;
        }
    }

    public ClusterManagerID getMember(String memberName) {
        synchronized (memberMap) {
            return memberMap.get(memberName);
        }
    }

    public void installView(Map<String, ClusterManagerID> newView) {
        boolean memberAdded = false;
        synchronized (memberMap) {
            for (ClusterManagerID member : newView.values()) {
                memberAdded = tryAddMember(member);
            }
        }
        synchronized (prospectiveMemberMap) {
            prospectiveMemberMap.clear();
            prospectiveMemberByGcsId.clear();
        }
        if (memberAdded) {
            logger.info(String.format("INSTALL(%s)", formatView()));
        }
    }

    public String printMemberList() {
        return printMemberNames(getMemberMap());
    }

    private String printMemberNames(TreeMap<String, ClusterManagerID> members) {
        StringBuilder builder = new StringBuilder();
        for (ClusterManagerID member : members.values()) {
            builder.append(String.format("%s(%s) ", member.getMemberName(), (member.isOnline() ? "ONLINE" : "JOINING")));
        }
        return builder.toString().trim();
    }

    public void broadcastView() throws GroupCommunicationException {
        promoteProspectiveMembers();
        Map<String, ClusterManagerID> onlineMembers = new TreeMap<String, ClusterManagerID>();
        for (ClusterManagerID member : getMemberMap().values()) {
            if (member.isOnline()) {
                onlineMembers.put(member.getMemberName(), member);
            }
        }
        ManagerViewUpdateEvent viewUpdate = new ManagerViewUpdateEvent(getMyMemberID(), onlineMembers);
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("BROADCAST(%s)", formatView()));
        }
        send(viewUpdate);
    }

    public boolean tryAddMember(ClusterManagerID member) {
        synchronized (memberMap) {
            if (failedManagers.containsKey(member.getMember())) {
                logger.info(String.format("Removing manager '%s' from failed manager list", member.getMemberName()));
                failedManagers.remove(member.getMember());
            }
            if (memberMap.get(member.getMemberName()) == null) {
                memberMap.put(member.getMemberName(), member);
                memberByGcsId.put(member.getMember().getAddress().toString(), member);
                logger.info(String.format("ADD(%s) yields %s", member.getMemberName(), formatView()));
                ManagerNotification notification = new ManagerNotification(member.getGroupName(), member.getMemberName(), member.getMemberName(), ResourceState.JOINING, getClass().getSimpleName());
                logger.info(String.format("MEMBER '%s' IS NOW JOINING", member.getMemberName()));
                try {
                    notifyListeners(notification);
                } catch (ResourceNotificationException r) {
                    logger.warn("Unable to notify listeners of membership change.", r);
                }
                return true;
            }
        }
        return false;
    }

    public void addMember(ClusterManagerID member) {
        tryAddMember(member);
    }

    public TreeMap<String, ClusterManagerID> getMemberMap() {
        synchronized (memberMap) {
            TreeMap<String, ClusterManagerID> retMap = new TreeMap<String, ClusterManagerID>(memberMap);
            return retMap;
        }
    }

    public List<Member> getGcsMemberMap() {
        return this.channel.getGroup().getMembers();
    }

    public ClusterManagerID getMyMemberID() {
        return clusterManagerID;
    }

    public String formatView() {
        return String.format("VIEW(%s)[%d]", printMemberList(), getGcsMemberMap().size());
    }

    class ClusterMemberIDMapping {

        private TreeMap<String, ClusterManagerID> uuidToServerIDMap;

        private HashMap<String, String> groupCommunicationtoUuidMap;

        private HashMap<String, Set<String>> memberNameToUuidMap;

        ClusterMemberIDMapping() {
            uuidToServerIDMap = new TreeMap<String, ClusterManagerID>();
            groupCommunicationtoUuidMap = new HashMap<String, String>();
            memberNameToUuidMap = new HashMap<String, Set<String>>();
        }

        public void put(ClusterManagerID clusterManagerID) {
            String uuid = clusterManagerID.getUUID();
            uuidToServerIDMap.put(uuid, clusterManagerID);
            groupCommunicationtoUuidMap.put(clusterManagerID.getGroupCommunicationIdentity(), uuid);
            String memberName = clusterManagerID.getMemberName();
            if (memberNameToUuidMap.containsKey(memberName)) {
                Set<String> uuids = memberNameToUuidMap.get(memberName);
                uuids.add(uuid);
            } else {
                Set<String> uuids = new HashSet<String>();
                uuids.add(uuid);
                memberNameToUuidMap.put(memberName, uuids);
            }
        }

        public boolean isRepresentive() {
            String representitiveKey = uuidToServerIDMap.lastKey();
            if (representitiveKey.equals(clusterManagerID.getUUID())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("this member is a representive");
                }
                return true;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("this member is not a representive");
                }
                return false;
            }
        }

        public TreeMap<String, ClusterManagerID> getUUIDToMemberMap() {
            return uuidToServerIDMap;
        }

        public HashMap<String, Set<String>> getNameToUUIDMap() {
            return memberNameToUuidMap;
        }
    }

    public Map<String, NotificationGroupMember> getNotificationGroupMembers() {
        return null;
    }

    public void addListener(ResourceNotificationListener listener) {
        if (listener == null) {
            logger.warn("Attempting to add a null listener");
            return;
        }
        synchronized (listeners) {
            listeners.add(listener);
            if (logger.isDebugEnabled()) logger.info(String.format("Added listener %s to %s", listener.getClass().getSimpleName(), getClass().getSimpleName()));
        }
    }

    public void removeListener(ResourceNotificationListener listener) {
        if (listeners != null) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }
    }

    public void notifyListeners(ClusterResourceNotification notification) throws ResourceNotificationException {
        synchronized (listeners) {
            for (ResourceNotificationListener listener : listeners) {
                if (logger.isDebugEnabled()) logger.debug(String.format("Notifying %s with %s\n%s", listener.getClass().getSimpleName(), notification.getClass().getSimpleName(), notification.toString()));
                listener.notify(notification);
            }
        }
    }

    public static String printMemberList(Collection<ClusterManagerID> members) {
        StringBuilder builder = new StringBuilder();
        for (ClusterManagerID member : members) {
            builder.append("  ").append(member.toString()).append("\n");
        }
        return builder.toString();
    }

    public void run() {
    }

    public void setMemberOnline(ClusterManagerID member) {
        tryAddMember(member);
        ClusterManagerID currentMember = memberMap.get(member.getMemberName());
        if (currentMember == null) {
            logger.warn(String.format("Unable to update member '%s' to ONLINE - was not added", member));
            return;
        }
        currentMember.setOnline(true);
        logger.info(String.format("MEMBER '%s' IS NOW ONLINE", currentMember.getMemberName()));
        ManagerNotification notification = new ManagerNotification(currentMember.getGroupName(), currentMember.getMemberName(), currentMember.getMemberName(), ResourceState.ONLINE, getClass().getSimpleName());
        try {
            notifyListeners(notification);
        } catch (ResourceNotificationException r) {
            logger.warn("Unable to notify listeners of membership change.", r);
        }
    }

    private void startHeartbeatService() {
        final String memberName = serviceMgr.getMemberName();
        final String clusterName = serviceMgr.getGroupName();
        final String source = getClass().getSimpleName();
        Runnable heartBeatProcess = new Runnable() {

            public void run() {
                heartbeatStarted = true;
                logger.info("Heartbeat Service is now running");
                while (true) {
                    ResourceState currentState;
                    if (clusterManagerID.isOnline()) currentState = ResourceState.ONLINE; else currentState = ResourceState.JOINING;
                    ManagerHeartbeat notification = new ManagerHeartbeat(clusterName, memberName, memberName, currentState, source);
                    try {
                        notifyListeners(notification);
                    } catch (Exception r) {
                        logger.error("Unable to post member notification to policy manager", r);
                        logger.info("Heartbeat Service exiting...");
                        return;
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException i) {
                        logger.info("Heartbeat Service exiting because it was interrupted");
                        return;
                    }
                }
            }
        };
        new Thread(heartBeatProcess, "HeartbeatService").start();
    }

    public void mergedMembers(GroupIdentifier gid, Member sender, List<Member> joinedMembers, List<Member> failedMembers, List<Member> quitMembers) {
        if (joinedMembers.size() > 0) logger.debug(String.format("MERGE: JOINING:\n%s", formatMemberList(joinedMembers)));
        if (failedMembers.size() > 0) logger.debug(String.format("MERGE: FAILED:\n%s", formatMemberList(failedMembers)));
        if (quitMembers.size() > 0) logger.debug(String.format("MERGE: QUIT:\n%s", formatMemberList(quitMembers)));
    }

    public static String formatMemberList(List<Member> memberList) {
        StringBuilder builder = new StringBuilder();
        for (Member member : memberList) {
            builder.append(member.toString()).append("\n");
        }
        return builder.toString();
    }

    public static String formatMamagerList(List<ClusterManagerID> managerList) {
        StringBuilder builder = new StringBuilder();
        for (ClusterManagerID manager : managerList) {
            builder.append(manager.toString()).append("\n");
        }
        return builder.toString();
    }

    public void suspectMember(Member arg0, GroupIdentifier arg1, Member arg2) {
    }

    public String dumpAllState() {
        StringBuilder builder = new StringBuilder();
        builder.append(queueManager.dumpAllState());
        return builder.toString();
    }

    public long getViewID() {
        return ((JGroupsReliableGroupChannel) channel).getViewID();
    }

    public void prepare() throws Exception {
    }

    public void receiveMonitoringNotification(ClusterResourceNotification notification) {
        try {
            notifyListeners(notification);
        } catch (ResourceNotificationException e) {
            logger.error(String.format("Encountered exception while processing notification.\n" + "Content=%s", notification), e);
        }
    }

    public boolean pingHost(String hostName) {
        try {
            InetAddress address = InetAddress.getByName(hostName);
            boolean isReachable = address.isReachable(1000);
            if (logger.isInfoEnabled()) logger.info(String.format("HOST %s@%s IS %s", address.getHostName(), address.getHostAddress(), isReachable ? "ALIVE" : "NOT REACHABLE"));
            return isReachable;
        } catch (UnknownHostException e) {
            CLUtils.println(String.format("host %s is unknown", hostName));
            return false;
        } catch (IOException e) {
            CLUtils.println(String.format("host %s is unreachable", hostName));
            return false;
        }
    }
}
