package org.apache.catalina.tribes.tipis;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.ChannelListener;
import org.apache.catalina.tribes.Heartbeat;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.MembershipListener;
import org.apache.catalina.tribes.group.Response;
import org.apache.catalina.tribes.group.RpcCallback;
import org.apache.catalina.tribes.group.RpcChannel;
import org.apache.catalina.tribes.io.XByteBuffer;
import org.apache.catalina.tribes.membership.MemberImpl;
import org.apache.catalina.tribes.util.Arrays;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Filip Hanik
 * @version 1.0
 */
public abstract class AbstractReplicatedMap extends ConcurrentHashMap implements RpcCallback, ChannelListener, MembershipListener, Heartbeat {

    protected static Log log = LogFactory.getLog(AbstractReplicatedMap.class);

    /**
     * The default initial capacity - MUST be a power of two.
     */
    public static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The load factor used when none specified in constructor.
     **/
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * Used to identify the map
     */
    final String chset = "ISO-8859-1";

    protected abstract int getStateMessageType();

    /**
     * Timeout for RPC messages, how long we will wait for a reply
     */
    protected transient long rpcTimeout = 5000;

    /**
     * Reference to the channel for sending messages
     */
    protected transient Channel channel;

    /**
     * The RpcChannel to send RPC messages through
     */
    protected transient RpcChannel rpcChannel;

    /**
     * The Map context name makes this map unique, this
     * allows us to have more than one map shared
     * through one channel
     */
    protected transient byte[] mapContextName;

    /**
     * Has the state been transferred
     */
    protected transient boolean stateTransferred = false;

    /**
     * Simple lock object for transfers
     */
    protected transient Object stateMutex = new Object();

    /**
     * A list of members in our map
     */
    protected transient HashMap mapMembers = new HashMap();

    /**
     * Our default send options
     */
    protected transient int channelSendOptions = Channel.SEND_OPTIONS_DEFAULT;

    /**
     * The owner of this map, ala a SessionManager for example
     */
    protected transient MapOwner mapOwner;

    /**
     * External class loaders if serialization and deserialization is to be performed successfully.
     */
    protected transient ClassLoader[] externalLoaders;

    /**
     * The node we are currently backing up data to, this index will rotate
     * on a round robin basis
     */
    protected transient int currentNode = 0;

    /**
     * Since the map keeps internal membership
     * this is the timeout for a ping message to be responded to
     * If a remote map doesn't respond within this timeframe, 
     * its considered dead.
     */
    protected transient long accessTimeout = 5000;

    /**
     * Readable string of the mapContextName value
     */
    protected transient String mapname = "";

    public static interface MapOwner {

        public void objectMadePrimay(Object key, Object value);
    }

    /**
     * Creates a new map
     * @param channel The channel to use for communication
     * @param timeout long - timeout for RPC messags
     * @param mapContextName String - unique name for this map, to allow multiple maps per channel
     * @param initialCapacity int - the size of this map, see HashMap
     * @param loadFactor float - load factor, see HashMap
     * @param cls - a list of classloaders to be used for deserialization of objects.
     */
    public AbstractReplicatedMap(MapOwner owner, Channel channel, long timeout, String mapContextName, int initialCapacity, float loadFactor, int channelSendOptions, ClassLoader[] cls) {
        super(initialCapacity, loadFactor, 15);
        init(owner, channel, mapContextName, timeout, channelSendOptions, cls);
    }

    /**
     * Helper methods, wraps a single member in an array
     * @param m Member
     * @return Member[]
     */
    protected Member[] wrap(Member m) {
        if (m == null) return new Member[0]; else return new Member[] { m };
    }

    /**
     * Initializes the map by creating the RPC channel, registering itself as a channel listener
     * This method is also responsible for initiating the state transfer
     * @param owner Object
     * @param channel Channel
     * @param mapContextName String
     * @param timeout long
     * @param channelSendOptions int
     * @param cls ClassLoader[]
     */
    protected void init(MapOwner owner, Channel channel, String mapContextName, long timeout, int channelSendOptions, ClassLoader[] cls) {
        log.info("Initializing AbstractReplicatedMap with context name:" + mapContextName);
        this.mapOwner = owner;
        this.externalLoaders = cls;
        this.channelSendOptions = channelSendOptions;
        this.channel = channel;
        this.rpcTimeout = timeout;
        try {
            this.mapname = mapContextName;
            this.mapContextName = mapContextName.getBytes(chset);
        } catch (UnsupportedEncodingException x) {
            log.warn("Unable to encode mapContextName[" + mapContextName + "] using getBytes(" + chset + ") using default getBytes()", x);
            this.mapContextName = mapContextName.getBytes();
        }
        if (log.isTraceEnabled()) log.trace("Created Lazy Map with name:" + mapContextName + ", bytes:" + Arrays.toString(this.mapContextName));
        this.rpcChannel = new RpcChannel(this.mapContextName, channel, this);
        this.channel.addChannelListener(this);
        this.channel.addMembershipListener(this);
        try {
            broadcast(MapMessage.MSG_INIT, true);
            transferState();
            broadcast(MapMessage.MSG_START, true);
        } catch (ChannelException x) {
            log.warn("Unable to send map start message.");
            throw new RuntimeException("Unable to start replicated map.", x);
        }
    }

    /**
     * Sends a ping out to all the members in the cluster, not just map members
     * that this map is alive.
     * @param timeout long
     * @throws ChannelException
     */
    protected void ping(long timeout) throws ChannelException {
        MapMessage msg = new MapMessage(this.mapContextName, MapMessage.MSG_INIT, false, null, null, null, channel.getLocalMember(false), null);
        if (channel.getMembers().length > 0) {
            Response[] resp = rpcChannel.send(channel.getMembers(), msg, rpcChannel.ALL_REPLY, (channelSendOptions), (int) accessTimeout);
            for (int i = 0; i < resp.length; i++) {
                memberAlive(resp[i].getSource());
            }
        }
        synchronized (mapMembers) {
            Iterator it = mapMembers.entrySet().iterator();
            long now = System.currentTimeMillis();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                long access = ((Long) entry.getValue()).longValue();
                if ((now - access) > timeout) {
                    it.remove();
                    memberDisappeared((Member) entry.getKey());
                }
            }
        }
    }

    /**
     * We have received a member alive notification
     * @param member Member
     */
    protected void memberAlive(Member member) {
        synchronized (mapMembers) {
            if (!mapMembers.containsKey(member)) {
                mapMemberAdded(member);
            }
            mapMembers.put(member, new Long(System.currentTimeMillis()));
        }
    }

    /**
     * Helper method to broadcast a message to all members in a channel
     * @param msgtype int
     * @param rpc boolean
     * @throws ChannelException
     */
    protected void broadcast(int msgtype, boolean rpc) throws ChannelException {
        MapMessage msg = new MapMessage(this.mapContextName, msgtype, false, null, null, null, channel.getLocalMember(false), null);
        if (rpc) {
            Response[] resp = rpcChannel.send(channel.getMembers(), msg, rpcChannel.FIRST_REPLY, (channelSendOptions), rpcTimeout);
            for (int i = 0; i < resp.length; i++) {
                mapMemberAdded(resp[i].getSource());
                messageReceived(resp[i].getMessage(), resp[i].getSource());
            }
        } else {
            channel.send(channel.getMembers(), msg, channelSendOptions);
        }
    }

    public void breakdown() {
        finalize();
    }

    public void finalize() {
        try {
            broadcast(MapMessage.MSG_STOP, false);
        } catch (Exception ignore) {
        }
        if (this.rpcChannel != null) {
            this.rpcChannel.breakdown();
        }
        if (this.channel != null) {
            this.channel.removeChannelListener(this);
            this.channel.removeMembershipListener(this);
        }
        this.rpcChannel = null;
        this.channel = null;
        this.mapMembers.clear();
        super.clear();
        this.stateTransferred = false;
        this.externalLoaders = null;
    }

    public int hashCode() {
        return Arrays.hashCode(this.mapContextName);
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof AbstractReplicatedMap)) return false;
        if (!(o.getClass().equals(this.getClass()))) return false;
        AbstractReplicatedMap other = (AbstractReplicatedMap) o;
        return Arrays.equals(mapContextName, other.mapContextName);
    }

    public Member[] getMapMembers(HashMap members) {
        synchronized (members) {
            Member[] result = new Member[members.size()];
            members.keySet().toArray(result);
            return result;
        }
    }

    public Member[] getMapMembers() {
        return getMapMembers(this.mapMembers);
    }

    public Member[] getMapMembersExcl(Member[] exclude) {
        synchronized (mapMembers) {
            HashMap list = (HashMap) mapMembers.clone();
            for (int i = 0; i < exclude.length; i++) list.remove(exclude[i]);
            return getMapMembers(list);
        }
    }

    /**
     * Replicates any changes to the object since the last time
     * The object has to be primary, ie, if the object is a proxy or a backup, it will not be replicated<br>
     * @param complete - if set to true, the object is replicated to its backup
     * if set to false, only objects that implement ReplicatedMapEntry and the isDirty() returns true will
     * be replicated
     */
    public void replicate(Object key, boolean complete) {
        if (log.isTraceEnabled()) log.trace("Replicate invoked on key:" + key);
        MapEntry entry = (MapEntry) super.get(key);
        if (entry == null) return;
        if (!entry.isSerializable()) return;
        if (entry != null && entry.isPrimary() && entry.getBackupNodes() != null && entry.getBackupNodes().length > 0) {
            Object value = entry.getValue();
            boolean repl = complete || ((value instanceof ReplicatedMapEntry) && ((ReplicatedMapEntry) value).isDirty());
            if (!repl) {
                if (log.isTraceEnabled()) log.trace("Not replicating:" + key + ", no change made");
                return;
            }
            boolean diff = ((value instanceof ReplicatedMapEntry) && ((ReplicatedMapEntry) value).isDiffable());
            MapMessage msg = null;
            if (diff) {
                ReplicatedMapEntry rentry = (ReplicatedMapEntry) entry.getValue();
                try {
                    rentry.lock();
                    msg = new MapMessage(mapContextName, MapMessage.MSG_BACKUP, true, (Serializable) entry.getKey(), null, rentry.getDiff(), entry.getPrimary(), entry.getBackupNodes());
                } catch (IOException x) {
                    log.error("Unable to diff object. Will replicate the entire object instead.", x);
                } finally {
                    rentry.unlock();
                }
            }
            if (msg == null) {
                msg = new MapMessage(mapContextName, MapMessage.MSG_BACKUP, false, (Serializable) entry.getKey(), (Serializable) entry.getValue(), null, entry.getPrimary(), entry.getBackupNodes());
            }
            try {
                if (channel != null && entry.getBackupNodes() != null && entry.getBackupNodes().length > 0) {
                    channel.send(entry.getBackupNodes(), msg, channelSendOptions);
                }
            } catch (ChannelException x) {
                log.error("Unable to replicate data.", x);
            }
        }
    }

    /**
     * This can be invoked by a periodic thread to replicate out any changes.
     * For maps that don't store objects that implement ReplicatedMapEntry, this
     * method should be used infrequently to avoid large amounts of data transfer
     * @param complete boolean
     */
    public void replicate(boolean complete) {
        Iterator i = super.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            replicate(e.getKey(), complete);
        }
    }

    public void transferState() {
        try {
            Member[] members = getMapMembers();
            Member backup = members.length > 0 ? (Member) members[0] : null;
            if (backup != null) {
                MapMessage msg = new MapMessage(mapContextName, getStateMessageType(), false, null, null, null, null, null);
                Response[] resp = rpcChannel.send(new Member[] { backup }, msg, rpcChannel.FIRST_REPLY, channelSendOptions, rpcTimeout);
                if (resp.length > 0) {
                    synchronized (stateMutex) {
                        msg = (MapMessage) resp[0].getMessage();
                        msg.deserialize(getExternalLoaders());
                        ArrayList list = (ArrayList) msg.getValue();
                        for (int i = 0; i < list.size(); i++) {
                            messageReceived((Serializable) list.get(i), resp[0].getSource());
                        }
                    }
                } else {
                    log.warn("Transfer state, 0 replies, probably a timeout.");
                }
            }
        } catch (ChannelException x) {
            log.error("Unable to transfer LazyReplicatedMap state.", x);
        } catch (IOException x) {
            log.error("Unable to transfer LazyReplicatedMap state.", x);
        } catch (ClassNotFoundException x) {
            log.error("Unable to transfer LazyReplicatedMap state.", x);
        }
        stateTransferred = true;
    }

    /**
     * @todo implement state transfer
     * @param msg Serializable
     * @return Serializable - null if no reply should be sent
     */
    public Serializable replyRequest(Serializable msg, final Member sender) {
        if (!(msg instanceof MapMessage)) return null;
        MapMessage mapmsg = (MapMessage) msg;
        if (mapmsg.getMsgType() == mapmsg.MSG_INIT) {
            mapmsg.setPrimary(channel.getLocalMember(false));
            return mapmsg;
        }
        if (mapmsg.getMsgType() == mapmsg.MSG_START) {
            mapmsg.setPrimary(channel.getLocalMember(false));
            mapMemberAdded(sender);
            return mapmsg;
        }
        if (mapmsg.getMsgType() == mapmsg.MSG_RETRIEVE_BACKUP) {
            MapEntry entry = (MapEntry) super.get(mapmsg.getKey());
            if (entry == null || (!entry.isSerializable())) return null;
            mapmsg.setValue((Serializable) entry.getValue());
            return mapmsg;
        }
        if (mapmsg.getMsgType() == mapmsg.MSG_STATE || mapmsg.getMsgType() == mapmsg.MSG_STATE_COPY) {
            synchronized (stateMutex) {
                ArrayList list = new ArrayList();
                Iterator i = super.entrySet().iterator();
                while (i.hasNext()) {
                    Map.Entry e = (Map.Entry) i.next();
                    MapEntry entry = (MapEntry) super.get(e.getKey());
                    if (entry != null && entry.isSerializable()) {
                        boolean copy = (mapmsg.getMsgType() == mapmsg.MSG_STATE_COPY);
                        MapMessage me = new MapMessage(mapContextName, copy ? MapMessage.MSG_COPY : MapMessage.MSG_PROXY, false, (Serializable) entry.getKey(), copy ? (Serializable) entry.getValue() : null, null, entry.getPrimary(), entry.getBackupNodes());
                        list.add(me);
                    }
                }
                mapmsg.setValue(list);
                return mapmsg;
            }
        }
        return null;
    }

    /**
     * If the reply has already been sent to the requesting thread,
     * the rpc callback can handle any data that comes in after the fact.
     * @param msg Serializable
     * @param sender Member
     */
    public void leftOver(Serializable msg, Member sender) {
        if (!(msg instanceof MapMessage)) return;
        MapMessage mapmsg = (MapMessage) msg;
        try {
            mapmsg.deserialize(getExternalLoaders());
            if (mapmsg.getMsgType() == MapMessage.MSG_START) {
                mapMemberAdded(mapmsg.getPrimary());
            } else if (mapmsg.getMsgType() == MapMessage.MSG_INIT) {
                memberAlive(mapmsg.getPrimary());
            }
        } catch (IOException x) {
            log.error("Unable to deserialize MapMessage.", x);
        } catch (ClassNotFoundException x) {
            log.error("Unable to deserialize MapMessage.", x);
        }
    }

    public void messageReceived(Serializable msg, Member sender) {
        if (!(msg instanceof MapMessage)) return;
        MapMessage mapmsg = (MapMessage) msg;
        if (log.isTraceEnabled()) {
            log.trace("Map[" + mapname + "] received message:" + mapmsg);
        }
        try {
            mapmsg.deserialize(getExternalLoaders());
        } catch (IOException x) {
            log.error("Unable to deserialize MapMessage.", x);
            return;
        } catch (ClassNotFoundException x) {
            log.error("Unable to deserialize MapMessage.", x);
            return;
        }
        if (log.isTraceEnabled()) log.trace("Map message received from:" + sender.getName() + " msg:" + mapmsg);
        if (mapmsg.getMsgType() == MapMessage.MSG_START) {
            mapMemberAdded(mapmsg.getPrimary());
        }
        if (mapmsg.getMsgType() == MapMessage.MSG_STOP) {
            memberDisappeared(mapmsg.getPrimary());
        }
        if (mapmsg.getMsgType() == MapMessage.MSG_PROXY) {
            MapEntry entry = (MapEntry) super.get(mapmsg.getKey());
            if (entry == null) {
                entry = new MapEntry(mapmsg.getKey(), mapmsg.getValue());
                entry.setBackup(false);
                entry.setProxy(true);
                entry.setBackupNodes(mapmsg.getBackupNodes());
                entry.setPrimary(mapmsg.getPrimary());
                super.put(entry.getKey(), entry);
            } else {
                entry.setProxy(true);
                entry.setBackup(false);
                entry.setBackupNodes(mapmsg.getBackupNodes());
                entry.setPrimary(mapmsg.getPrimary());
            }
        }
        if (mapmsg.getMsgType() == MapMessage.MSG_REMOVE) {
            super.remove(mapmsg.getKey());
        }
        if (mapmsg.getMsgType() == MapMessage.MSG_BACKUP || mapmsg.getMsgType() == MapMessage.MSG_COPY) {
            MapEntry entry = (MapEntry) super.get(mapmsg.getKey());
            if (entry == null) {
                entry = new MapEntry(mapmsg.getKey(), mapmsg.getValue());
                entry.setBackup(mapmsg.getMsgType() == MapMessage.MSG_BACKUP);
                entry.setProxy(false);
                entry.setBackupNodes(mapmsg.getBackupNodes());
                entry.setPrimary(mapmsg.getPrimary());
                if (mapmsg.getValue() != null && mapmsg.getValue() instanceof ReplicatedMapEntry) {
                    ((ReplicatedMapEntry) mapmsg.getValue()).setOwner(getMapOwner());
                }
            } else {
                entry.setBackup(mapmsg.getMsgType() == MapMessage.MSG_BACKUP);
                entry.setProxy(false);
                entry.setBackupNodes(mapmsg.getBackupNodes());
                entry.setPrimary(mapmsg.getPrimary());
                if (entry.getValue() instanceof ReplicatedMapEntry) {
                    ReplicatedMapEntry diff = (ReplicatedMapEntry) entry.getValue();
                    if (mapmsg.isDiff()) {
                        try {
                            diff.lock();
                            diff.applyDiff(mapmsg.getDiffValue(), 0, mapmsg.getDiffValue().length);
                        } catch (Exception x) {
                            log.error("Unable to apply diff to key:" + entry.getKey(), x);
                        } finally {
                            diff.unlock();
                        }
                    } else {
                        if (mapmsg.getValue() != null) entry.setValue(mapmsg.getValue());
                        ((ReplicatedMapEntry) entry.getValue()).setOwner(getMapOwner());
                    }
                } else if (mapmsg.getValue() instanceof ReplicatedMapEntry) {
                    ReplicatedMapEntry re = (ReplicatedMapEntry) mapmsg.getValue();
                    re.setOwner(getMapOwner());
                    entry.setValue(re);
                } else {
                    if (mapmsg.getValue() != null) entry.setValue(mapmsg.getValue());
                }
            }
            super.put(entry.getKey(), entry);
        }
    }

    public boolean accept(Serializable msg, Member sender) {
        boolean result = false;
        if (msg instanceof MapMessage) {
            if (log.isTraceEnabled()) log.trace("Map[" + mapname + "] accepting...." + msg);
            result = Arrays.equals(mapContextName, ((MapMessage) msg).getMapId());
            if (log.isTraceEnabled()) log.trace("Msg[" + mapname + "] accepted[" + result + "]...." + msg);
        }
        return result;
    }

    public void mapMemberAdded(Member member) {
        if (member.equals(getChannel().getLocalMember(false))) return;
        boolean memberAdded = false;
        synchronized (mapMembers) {
            if (!mapMembers.containsKey(member)) {
                mapMembers.put(member, new Long(System.currentTimeMillis()));
                memberAdded = true;
            }
        }
        if (memberAdded) {
            synchronized (stateMutex) {
                Iterator i = super.entrySet().iterator();
                while (i.hasNext()) {
                    Map.Entry e = (Map.Entry) i.next();
                    MapEntry entry = (MapEntry) super.get(e.getKey());
                    if (entry == null) continue;
                    if (entry.isPrimary() && (entry.getBackupNodes() == null || entry.getBackupNodes().length == 0)) {
                        try {
                            Member[] backup = publishEntryInfo(entry.getKey(), entry.getValue());
                            entry.setBackupNodes(backup);
                            entry.setPrimary(channel.getLocalMember(false));
                        } catch (ChannelException x) {
                            log.error("Unable to select backup node.", x);
                        }
                    }
                }
            }
        }
    }

    public boolean inSet(Member m, Member[] set) {
        if (set == null) return false;
        boolean result = false;
        for (int i = 0; i < set.length && (!result); i++) if (m.equals(set[i])) result = true;
        return result;
    }

    public Member[] excludeFromSet(Member[] mbrs, Member[] set) {
        ArrayList result = new ArrayList();
        for (int i = 0; i < set.length; i++) {
            boolean include = true;
            for (int j = 0; j < mbrs.length; j++) if (mbrs[j].equals(set[i])) include = false;
            if (include) result.add(set[i]);
        }
        return (Member[]) result.toArray(new Member[result.size()]);
    }

    public void memberAdded(Member member) {
    }

    public void memberDisappeared(Member member) {
        boolean removed = false;
        synchronized (mapMembers) {
            removed = (mapMembers.remove(member) != null);
            if (!removed) {
                if (log.isDebugEnabled()) log.debug("Member[" + member + "] disappeared, but was not present in the map.");
                return;
            }
        }
        Iterator i = super.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            MapEntry entry = (MapEntry) super.get(e.getKey());
            if (entry == null) continue;
            if (entry.isPrimary() && inSet(member, entry.getBackupNodes())) {
                if (log.isDebugEnabled()) log.debug("[1] Primary choosing a new backup");
                try {
                    Member[] backup = publishEntryInfo(entry.getKey(), entry.getValue());
                    entry.setBackupNodes(backup);
                    entry.setPrimary(channel.getLocalMember(false));
                } catch (ChannelException x) {
                    log.error("Unable to relocate[" + entry.getKey() + "] to a new backup node", x);
                }
            } else if (member.equals(entry.getPrimary())) {
                if (log.isDebugEnabled()) log.debug("[2] Primary disappeared");
                entry.setPrimary(null);
            }
            if (entry.isProxy() && entry.getPrimary() == null && entry.getBackupNodes() != null && entry.getBackupNodes().length == 1 && entry.getBackupNodes()[0].equals(member)) {
                if (log.isDebugEnabled()) log.debug("[3] Removing orphaned proxy");
                i.remove();
            } else if (entry.getPrimary() == null && entry.isBackup() && entry.getBackupNodes() != null && entry.getBackupNodes().length == 1 && entry.getBackupNodes()[0].equals(channel.getLocalMember(false))) {
                try {
                    if (log.isDebugEnabled()) log.debug("[4] Backup becoming primary");
                    entry.setPrimary(channel.getLocalMember(false));
                    entry.setBackup(false);
                    entry.setProxy(false);
                    Member[] backup = publishEntryInfo(entry.getKey(), entry.getValue());
                    entry.setBackupNodes(backup);
                    if (mapOwner != null) mapOwner.objectMadePrimay(entry.getKey(), entry.getValue());
                } catch (ChannelException x) {
                    log.error("Unable to relocate[" + entry.getKey() + "] to a new backup node", x);
                }
            }
        }
    }

    public int getNextBackupIndex() {
        int size = mapMembers.size();
        if (mapMembers.size() == 0) return -1;
        int node = currentNode++;
        if (node >= size) {
            node = 0;
            currentNode = 0;
        }
        return node;
    }

    public Member getNextBackupNode() {
        Member[] members = getMapMembers();
        int node = getNextBackupIndex();
        if (members.length == 0 || node == -1) return null;
        if (node >= members.length) node = 0;
        return members[node];
    }

    protected abstract Member[] publishEntryInfo(Object key, Object value) throws ChannelException;

    public void heartbeat() {
        try {
            ping(accessTimeout);
        } catch (Exception x) {
            log.error("Unable to send AbstractReplicatedMap.ping message", x);
        }
    }

    /**
     * Removes an object from this map, it will also remove it from 
     * 
     * @param key Object
     * @return Object
     */
    public Object remove(Object key) {
        return remove(key, true);
    }

    public Object remove(Object key, boolean notify) {
        MapEntry entry = (MapEntry) super.remove(key);
        try {
            if (getMapMembers().length > 0 && notify) {
                MapMessage msg = new MapMessage(getMapContextName(), MapMessage.MSG_REMOVE, false, (Serializable) key, null, null, null, null);
                getChannel().send(getMapMembers(), msg, getChannelSendOptions());
            }
        } catch (ChannelException x) {
            log.error("Unable to replicate out data for a LazyReplicatedMap.remove operation", x);
        }
        return entry != null ? entry.getValue() : null;
    }

    public MapEntry getInternal(Object key) {
        return (MapEntry) super.get(key);
    }

    public Object get(Object key) {
        MapEntry entry = (MapEntry) super.get(key);
        if (log.isTraceEnabled()) log.trace("Requesting id:" + key + " entry:" + entry);
        if (entry == null) return null;
        if (!entry.isPrimary()) {
            try {
                Member[] backup = null;
                MapMessage msg = null;
                if (!entry.isBackup()) {
                    msg = new MapMessage(getMapContextName(), MapMessage.MSG_RETRIEVE_BACKUP, false, (Serializable) key, null, null, null, null);
                    Response[] resp = getRpcChannel().send(entry.getBackupNodes(), msg, this.getRpcChannel().FIRST_REPLY, Channel.SEND_OPTIONS_DEFAULT, getRpcTimeout());
                    if (resp == null || resp.length == 0) {
                        log.warn("Unable to retrieve remote object for key:" + key);
                        return null;
                    }
                    msg = (MapMessage) resp[0].getMessage();
                    msg.deserialize(getExternalLoaders());
                    backup = entry.getBackupNodes();
                    if (entry.getValue() instanceof ReplicatedMapEntry) {
                        ReplicatedMapEntry val = (ReplicatedMapEntry) entry.getValue();
                        val.setOwner(getMapOwner());
                    }
                    if (msg.getValue() != null) entry.setValue(msg.getValue());
                }
                if (entry.isBackup()) {
                    backup = publishEntryInfo(key, entry.getValue());
                } else if (entry.isProxy()) {
                    msg = new MapMessage(getMapContextName(), MapMessage.MSG_PROXY, false, (Serializable) key, null, null, channel.getLocalMember(false), backup);
                    Member[] dest = getMapMembersExcl(backup);
                    if (dest != null && dest.length > 0) {
                        getChannel().send(dest, msg, getChannelSendOptions());
                    }
                }
                entry.setPrimary(channel.getLocalMember(false));
                entry.setBackupNodes(backup);
                entry.setBackup(false);
                entry.setProxy(false);
            } catch (Exception x) {
                log.error("Unable to replicate out data for a LazyReplicatedMap.get operation", x);
                return null;
            }
        }
        if (log.isTraceEnabled()) log.trace("Requesting id:" + key + " result:" + entry.getValue());
        if (entry.getValue() != null && entry.getValue() instanceof ReplicatedMapEntry) {
            ReplicatedMapEntry val = (ReplicatedMapEntry) entry.getValue();
            val.setOwner(getMapOwner());
        }
        return entry.getValue();
    }

    protected void printMap(String header) {
        try {
            System.out.println("\nDEBUG MAP:" + header);
            System.out.println("Map[" + new String(mapContextName, chset) + ", Map Size:" + super.size());
            Member[] mbrs = getMapMembers();
            for (int i = 0; i < mbrs.length; i++) {
                System.out.println("Mbr[" + (i + 1) + "=" + mbrs[i].getName());
            }
            Iterator i = super.entrySet().iterator();
            int cnt = 0;
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                System.out.println((++cnt) + ". " + super.get(e.getKey()));
            }
            System.out.println("EndMap]\n\n");
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    /**
         * Returns true if the key has an entry in the map.
         * The entry can be a proxy or a backup entry, invoking <code>get(key)</code>
         * will make this entry primary for the group
         * @param key Object
         * @return boolean
         */
    public boolean containsKey(Object key) {
        return super.containsKey(key);
    }

    public Object put(Object key, Object value) {
        return put(key, value, true);
    }

    public Object put(Object key, Object value, boolean notify) {
        MapEntry entry = new MapEntry(key, value);
        entry.setBackup(false);
        entry.setProxy(false);
        entry.setPrimary(channel.getLocalMember(false));
        Object old = null;
        if (containsKey(key)) old = remove(key);
        try {
            if (notify) {
                Member[] backup = publishEntryInfo(key, value);
                entry.setBackupNodes(backup);
            }
        } catch (ChannelException x) {
            log.error("Unable to replicate out data for a LazyReplicatedMap.put operation", x);
        }
        super.put(key, entry);
        return old;
    }

    /**
         * Copies all values from one map to this instance
         * @param m Map
         */
    public void putAll(Map m) {
        Iterator i = m.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        clear(true);
    }

    public void clear(boolean notify) {
        if (notify) {
            Iterator keys = keySet().iterator();
            while (keys.hasNext()) remove(keys.next());
        } else {
            super.clear();
        }
    }

    public boolean containsValue(Object value) {
        if (value == null) {
            return super.containsValue(value);
        } else {
            Iterator i = super.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                MapEntry entry = (MapEntry) super.get(e.getKey());
                if (entry != null && entry.isPrimary() && value.equals(entry.getValue())) return true;
            }
            return false;
        }
    }

    public Object clone() {
        throw new UnsupportedOperationException("This operation is not valid on a replicated map");
    }

    /**
         * Returns the entire contents of the map
         * Map.Entry.getValue() will return a LazyReplicatedMap.MapEntry object containing all the information 
         * about the object.
         * @return Set
         */
    public Set entrySetFull() {
        return super.entrySet();
    }

    public Set keySetFull() {
        return super.keySet();
    }

    public int sizeFull() {
        return super.size();
    }

    public Set entrySet() {
        LinkedHashSet set = new LinkedHashSet(super.size());
        Iterator i = super.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            Object key = e.getKey();
            MapEntry entry = (MapEntry) super.get(key);
            if (entry != null && entry.isPrimary()) {
                set.add(new MapEntry(key, entry.getValue()));
            }
        }
        return Collections.unmodifiableSet(set);
    }

    public Set keySet() {
        LinkedHashSet set = new LinkedHashSet(super.size());
        Iterator i = super.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            Object key = e.getKey();
            MapEntry entry = (MapEntry) super.get(key);
            if (entry != null && entry.isPrimary()) set.add(key);
        }
        return Collections.unmodifiableSet(set);
    }

    public int size() {
        int counter = 0;
        Iterator it = super.entrySet().iterator();
        while (it != null && it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            if (e != null) {
                MapEntry entry = (MapEntry) super.get(e.getKey());
                if (entry != null && entry.isPrimary() && entry.getValue() != null) counter++;
            }
        }
        return counter;
    }

    protected boolean removeEldestEntry(Map.Entry eldest) {
        return false;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public Collection values() {
        ArrayList values = new ArrayList();
        Iterator i = super.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry e = (Map.Entry) i.next();
            MapEntry entry = (MapEntry) super.get(e.getKey());
            if (entry != null && entry.isPrimary() && entry.getValue() != null) values.add(entry.getValue());
        }
        return Collections.unmodifiableCollection(values);
    }

    public static class MapEntry implements Map.Entry {

        private boolean backup;

        private boolean proxy;

        private Member[] backupNodes;

        private Member primary;

        private Object key;

        private Object value;

        public MapEntry(Object key, Object value) {
            setKey(key);
            setValue(value);
        }

        public boolean isKeySerializable() {
            return (key == null) || (key instanceof Serializable);
        }

        public boolean isValueSerializable() {
            return (value == null) || (value instanceof Serializable);
        }

        public boolean isSerializable() {
            return isKeySerializable() && isValueSerializable();
        }

        public boolean isBackup() {
            return backup;
        }

        public void setBackup(boolean backup) {
            this.backup = backup;
        }

        public boolean isProxy() {
            return proxy;
        }

        public boolean isPrimary() {
            return ((!proxy) && (!backup));
        }

        public void setProxy(boolean proxy) {
            this.proxy = proxy;
        }

        public boolean isDiffable() {
            return (value instanceof ReplicatedMapEntry) && ((ReplicatedMapEntry) value).isDiffable();
        }

        public void setBackupNodes(Member[] nodes) {
            this.backupNodes = nodes;
        }

        public Member[] getBackupNodes() {
            return backupNodes;
        }

        public void setPrimary(Member m) {
            primary = m;
        }

        public Member getPrimary() {
            return primary;
        }

        public Object getValue() {
            return value;
        }

        public Object setValue(Object value) {
            Object old = this.value;
            this.value = value;
            return old;
        }

        public Object getKey() {
            return key;
        }

        public Object setKey(Object key) {
            Object old = this.key;
            this.key = key;
            return old;
        }

        public int hashCode() {
            return key.hashCode();
        }

        public boolean equals(Object o) {
            return key.equals(o);
        }

        /**
         * apply a diff, or an entire object
         * @param data byte[]
         * @param offset int
         * @param length int
         * @param diff boolean
         * @throws IOException
         * @throws ClassNotFoundException
         */
        public void apply(byte[] data, int offset, int length, boolean diff) throws IOException, ClassNotFoundException {
            if (isDiffable() && diff) {
                ReplicatedMapEntry rentry = (ReplicatedMapEntry) value;
                try {
                    rentry.lock();
                    rentry.applyDiff(data, offset, length);
                } finally {
                    rentry.unlock();
                }
            } else if (length == 0) {
                value = null;
                proxy = true;
            } else {
                value = XByteBuffer.deserialize(data, offset, length);
            }
        }

        public String toString() {
            StringBuffer buf = new StringBuffer("MapEntry[key:");
            buf.append(getKey()).append("; ");
            buf.append("value:").append(getValue()).append("; ");
            buf.append("primary:").append(isPrimary()).append("; ");
            buf.append("backup:").append(isBackup()).append("; ");
            buf.append("proxy:").append(isProxy()).append(";]");
            return buf.toString();
        }
    }

    public static class MapMessage implements Serializable {

        public static final int MSG_BACKUP = 1;

        public static final int MSG_RETRIEVE_BACKUP = 2;

        public static final int MSG_PROXY = 3;

        public static final int MSG_REMOVE = 4;

        public static final int MSG_STATE = 5;

        public static final int MSG_START = 6;

        public static final int MSG_STOP = 7;

        public static final int MSG_INIT = 8;

        public static final int MSG_COPY = 9;

        public static final int MSG_STATE_COPY = 10;

        private byte[] mapId;

        private int msgtype;

        private boolean diff;

        private transient Serializable key;

        private transient Serializable value;

        private byte[] valuedata;

        private byte[] keydata;

        private byte[] diffvalue;

        private Member[] nodes;

        private Member primary;

        public String toString() {
            StringBuffer buf = new StringBuffer("MapMessage[context=");
            buf.append(new String(mapId));
            buf.append("; type=");
            buf.append(getTypeDesc());
            buf.append("; key=");
            buf.append(key);
            buf.append("; value=");
            buf.append(value);
            return buf.toString();
        }

        public String getTypeDesc() {
            switch(msgtype) {
                case MSG_BACKUP:
                    return "MSG_BACKUP";
                case MSG_RETRIEVE_BACKUP:
                    return "MSG_RETRIEVE_BACKUP";
                case MSG_PROXY:
                    return "MSG_PROXY";
                case MSG_REMOVE:
                    return "MSG_REMOVE";
                case MSG_STATE:
                    return "MSG_STATE";
                case MSG_START:
                    return "MSG_START";
                case MSG_STOP:
                    return "MSG_STOP";
                case MSG_INIT:
                    return "MSG_INIT";
                case MSG_STATE_COPY:
                    return "MSG_STATE_COPY";
                case MSG_COPY:
                    return "MSG_COPY";
                default:
                    return "UNKNOWN";
            }
        }

        public MapMessage() {
        }

        public MapMessage(byte[] mapId, int msgtype, boolean diff, Serializable key, Serializable value, byte[] diffvalue, Member primary, Member[] nodes) {
            this.mapId = mapId;
            this.msgtype = msgtype;
            this.diff = diff;
            this.key = key;
            this.value = value;
            this.diffvalue = diffvalue;
            this.nodes = nodes;
            this.primary = primary;
            setValue(value);
            setKey(key);
        }

        public void deserialize(ClassLoader[] cls) throws IOException, ClassNotFoundException {
            key(cls);
            value(cls);
        }

        public int getMsgType() {
            return msgtype;
        }

        public boolean isDiff() {
            return diff;
        }

        public Serializable getKey() {
            try {
                return key(null);
            } catch (Exception x) {
                log.error("Deserialization error of the MapMessage.key", x);
                return null;
            }
        }

        public Serializable key(ClassLoader[] cls) throws IOException, ClassNotFoundException {
            if (key != null) return key;
            if (keydata == null || keydata.length == 0) return null;
            key = XByteBuffer.deserialize(keydata, 0, keydata.length, cls);
            keydata = null;
            return key;
        }

        public byte[] getKeyData() {
            return keydata;
        }

        public Serializable getValue() {
            try {
                return value(null);
            } catch (Exception x) {
                log.error("Deserialization error of the MapMessage.value", x);
                return null;
            }
        }

        public Serializable value(ClassLoader[] cls) throws IOException, ClassNotFoundException {
            if (value != null) return value;
            if (valuedata == null || valuedata.length == 0) return null;
            value = XByteBuffer.deserialize(valuedata, 0, valuedata.length, cls);
            valuedata = null;
            ;
            return value;
        }

        public byte[] getValueData() {
            return valuedata;
        }

        public byte[] getDiffValue() {
            return diffvalue;
        }

        public Member[] getBackupNodes() {
            return nodes;
        }

        private void setBackUpNodes(Member[] nodes) {
            this.nodes = nodes;
        }

        public Member getPrimary() {
            return primary;
        }

        private void setPrimary(Member m) {
            primary = m;
        }

        public byte[] getMapId() {
            return mapId;
        }

        public void setValue(Serializable value) {
            try {
                if (value != null) valuedata = XByteBuffer.serialize(value);
                this.value = value;
            } catch (IOException x) {
                throw new RuntimeException(x);
            }
        }

        public void setKey(Serializable key) {
            try {
                if (key != null) keydata = XByteBuffer.serialize(key);
                this.key = key;
            } catch (IOException x) {
                throw new RuntimeException(x);
            }
        }

        protected Member[] readMembers(ObjectInput in) throws IOException, ClassNotFoundException {
            int nodecount = in.readInt();
            Member[] members = new Member[nodecount];
            for (int i = 0; i < members.length; i++) {
                byte[] d = new byte[in.readInt()];
                in.read(d);
                if (d.length > 0) members[i] = MemberImpl.getMember(d);
            }
            return members;
        }

        protected void writeMembers(ObjectOutput out, Member[] members) throws IOException {
            if (members == null) members = new Member[0];
            out.writeInt(members.length);
            for (int i = 0; i < members.length; i++) {
                if (members[i] != null) {
                    byte[] d = members[i] != null ? ((MemberImpl) members[i]).getData(false) : new byte[0];
                    out.writeInt(d.length);
                    out.write(d);
                }
            }
        }

        /**
         * shallow clone
         * @return Object
         */
        public Object clone() {
            MapMessage msg = new MapMessage(this.mapId, this.msgtype, this.diff, this.key, this.value, this.diffvalue, this.primary, this.nodes);
            msg.keydata = this.keydata;
            msg.valuedata = this.valuedata;
            return msg;
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public byte[] getMapContextName() {
        return mapContextName;
    }

    public RpcChannel getRpcChannel() {
        return rpcChannel;
    }

    public long getRpcTimeout() {
        return rpcTimeout;
    }

    public Object getStateMutex() {
        return stateMutex;
    }

    public boolean isStateTransferred() {
        return stateTransferred;
    }

    public MapOwner getMapOwner() {
        return mapOwner;
    }

    public ClassLoader[] getExternalLoaders() {
        return externalLoaders;
    }

    public int getChannelSendOptions() {
        return channelSendOptions;
    }

    public long getAccessTimeout() {
        return accessTimeout;
    }

    public void setMapOwner(MapOwner mapOwner) {
        this.mapOwner = mapOwner;
    }

    public void setExternalLoaders(ClassLoader[] externalLoaders) {
        this.externalLoaders = externalLoaders;
    }

    public void setChannelSendOptions(int channelSendOptions) {
        this.channelSendOptions = channelSendOptions;
    }

    public void setAccessTimeout(long accessTimeout) {
        this.accessTimeout = accessTimeout;
    }
}
