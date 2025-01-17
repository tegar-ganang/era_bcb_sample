package org.jgroups.blocks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.MembershipListener;
import org.jgroups.View;
import org.jgroups.annotations.Experimental;
import org.jgroups.annotations.ManagedAttribute;
import org.jgroups.annotations.ManagedOperation;
import org.jgroups.annotations.Unsupported;
import org.jgroups.logging.Log;
import org.jgroups.logging.LogFactory;
import org.jgroups.util.Util;

/** Hashmap which distributes its keys and values across the cluster. A PUT/GET/REMOVE computes the cluster node to which
 * or from which to get/set the key/value from a hash of the key and then forwards the request to the remote cluster node.
 * We also maintain a local cache (L1 cache) which is a bounded cache that caches retrieved keys/values. <br/>
 * Todos:<br/>
 * <ol>
 * <li>Use MarshalledValue to keep track of byte[] buffers, and be able to compute the exact size of the cache. This is
 *     good for maintaining a bounded cache (rather than using the number of entries)
 * <li>Provide a better consistent hashing algorithm than ConsistentHashFunction as default
 * <li>GUI (showing at least the topology and L1 and L2 caches)
 * <li>Notifications (puts, removes, gets etc)
 * <li>Invalidation of L1 caches (if used) on removal/put of item
 * <li>Benchmarks, comparison to memcached
 * <li>Documentation, comparison to memcached
 * </ol>
 * @author Bela Ban
 * @version $Id: PartitionedHashMap.java,v 1.20 2009/05/13 13:06:54 belaban Exp $
 */
@Experimental
@Unsupported
public class PartitionedHashMap<K, V> implements MembershipListener {

    /** The cache in which all partitioned entries are located */
    private Cache<K, V> l2_cache = new Cache<K, V>();

    /** The local bounded cache, to speed up access to frequently accessed entries. Can be disabled or enabled */
    private Cache<K, V> l1_cache = null;

    private static final Log log = LogFactory.getLog(PartitionedHashMap.class);

    private JChannel ch = null;

    private Address local_addr = null;

    private View view;

    private RpcDispatcher disp = null;

    @ManagedAttribute(writable = true)
    private String props = "udp.xml";

    @ManagedAttribute(writable = true)
    private String cluster_name = "PartitionedHashMap-Cluster";

    @ManagedAttribute(writable = true)
    private long call_timeout = 1000L;

    @ManagedAttribute(writable = true)
    private long caching_time = 30000L;

    private HashFunction<K> hash_function = null;

    private Set<MembershipListener> membership_listeners = new HashSet<MembershipListener>();

    /** On a view change, if a member P1 detects that for any given key K, P1 is not the owner of K, then
     * it will compute the new owner P2 and transfer ownership for all Ks for which P2 is the new owner. P1
     * will then also evict those keys from its L2 cache */
    @ManagedAttribute(writable = true)
    private boolean migrate_data = false;

    private static final short PUT = 1;

    private static final short GET = 2;

    private static final short REMOVE = 3;

    protected static Map<Short, Method> methods = new ConcurrentHashMap<Short, Method>(8);

    static {
        try {
            methods.put(PUT, PartitionedHashMap.class.getMethod("_put", Object.class, Object.class, long.class));
            methods.put(GET, PartitionedHashMap.class.getMethod("_get", Object.class));
            methods.put(REMOVE, PartitionedHashMap.class.getMethod("_remove", Object.class));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public interface HashFunction<K> {

        /**
         * Defines a hash function to pick the right node from the list of cluster nodes. Ideally, this function uses
         * consistent hashing, so that the same key maps to the same node despite cluster view changes. If a view change
         * causes all keys to hash to different nodes, then PartitionedHashMap will redirect requests to different nodes
         * and this causes unnecessary overhead.
         * @param key The object to be hashed
         * @param membership The membership. This value can be ignored for example if the hash function keeps
         * track of the membership itself, e.g. by registering as a membership
         * listener ({@link PartitionedHashMap#addMembershipListener(org.jgroups.MembershipListener)} ) 
         * @return
         */
        Address hash(K key, List<Address> membership);
    }

    public PartitionedHashMap(String props, String cluster_name) {
        this.props = props;
        this.cluster_name = cluster_name;
    }

    public String getProps() {
        return props;
    }

    public void setProps(String props) {
        this.props = props;
    }

    public Address getLocalAddress() {
        return local_addr;
    }

    @ManagedAttribute
    public String getLocalAddressAsString() {
        return local_addr != null ? local_addr.toString() : "null";
    }

    @ManagedAttribute
    public String getView() {
        return view != null ? view.toString() : "null";
    }

    @ManagedAttribute
    public boolean isL1CacheEnabled() {
        return l1_cache != null;
    }

    public String getClusterName() {
        return cluster_name;
    }

    public void setClusterName(String cluster_name) {
        this.cluster_name = cluster_name;
    }

    public long getCallTimeout() {
        return call_timeout;
    }

    public void setCallTimeout(long call_timeout) {
        this.call_timeout = call_timeout;
    }

    public long getCachingTime() {
        return caching_time;
    }

    public void setCachingTime(long caching_time) {
        this.caching_time = caching_time;
    }

    public boolean isMigrateData() {
        return migrate_data;
    }

    public void setMigrateData(boolean migrate_data) {
        this.migrate_data = migrate_data;
    }

    public HashFunction getHashFunction() {
        return hash_function;
    }

    public void setHashFunction(HashFunction<K> hash_function) {
        this.hash_function = hash_function;
    }

    public void addMembershipListener(MembershipListener l) {
        membership_listeners.add(l);
    }

    public void removeMembershipListener(MembershipListener l) {
        membership_listeners.remove(l);
    }

    public Cache<K, V> getL1Cache() {
        return l1_cache;
    }

    public void setL1Cache(Cache<K, V> cache) {
        if (l1_cache != null) l1_cache.stop();
        l1_cache = cache;
    }

    public Cache<K, V> getL2Cache() {
        return l2_cache;
    }

    public void setL2Cache(Cache<K, V> cache) {
        if (l2_cache != null) l2_cache.stop();
        l2_cache = cache;
    }

    @ManagedOperation
    public void start() throws Exception {
        hash_function = new ConsistentHashFunction<K>();
        addMembershipListener((MembershipListener) hash_function);
        ch = new JChannel(props);
        disp = new RpcDispatcher(ch, null, this, this);
        RpcDispatcher.Marshaller marshaller = new CustomMarshaller();
        disp.setRequestMarshaller(marshaller);
        disp.setResponseMarshaller(marshaller);
        disp.setMethodLookup(new MethodLookup() {

            public Method findMethod(short id) {
                return methods.get(id);
            }
        });
        ch.connect(cluster_name);
        local_addr = ch.getAddress();
        view = ch.getView();
    }

    @ManagedOperation
    public void stop() {
        if (l1_cache != null) l1_cache.stop();
        if (migrate_data) {
            List<Address> members_without_me = new ArrayList<Address>(view.getMembers());
            members_without_me.remove(local_addr);
            for (Map.Entry<K, Cache.Value<V>> entry : l2_cache.entrySet()) {
                K key = entry.getKey();
                Address node = hash_function.hash(key, members_without_me);
                if (!node.equals(local_addr)) {
                    Cache.Value<V> val = entry.getValue();
                    sendPut(node, key, val.getValue(), val.getTimeout(), true);
                    if (log.isTraceEnabled()) log.trace("migrated " + key + " from " + local_addr + " to " + node);
                }
            }
        }
        l2_cache.stop();
        disp.stop();
        ch.close();
    }

    @ManagedOperation
    public void put(K key, V val) {
        put(key, val, caching_time);
    }

    /**
     * Adds a key/value to the cache, replacing a previous item if there was one
     * @param key The key
     * @param val The value
     * @param caching_time Time to live. -1 means never cache, 0 means cache forever. All other (positive) values
     * are the number of milliseconds to cache the item
     */
    @ManagedOperation
    public void put(K key, V val, long caching_time) {
        Address dest_node = getNode(key);
        if (dest_node.equals(local_addr)) {
            l2_cache.put(key, val, caching_time);
        } else {
            sendPut(dest_node, key, val, caching_time, false);
        }
        if (l1_cache != null && caching_time >= 0) l1_cache.put(key, val, caching_time);
    }

    @ManagedOperation
    public V get(K key) {
        if (l1_cache != null) {
            V val = l1_cache.get(key);
            if (val != null) {
                if (log.isTraceEnabled()) log.trace("returned value " + val + " for " + key + " from L1 cache");
                return val;
            }
        }
        Cache.Value<V> val;
        try {
            Address dest_node = getNode(key);
            if (dest_node.equals(local_addr)) {
                val = l2_cache.getEntry(key);
            } else {
                val = (Cache.Value<V>) disp.callRemoteMethod(dest_node, new MethodCall(GET, new Object[] { key }), GroupRequest.GET_FIRST, call_timeout);
            }
            if (val != null) {
                V retval = val.getValue();
                if (l1_cache != null && val.getTimeout() >= 0) l1_cache.put(key, retval, val.getTimeout());
                return retval;
            }
            return null;
        } catch (Throwable t) {
            if (log.isWarnEnabled()) log.warn("_get() failed", t);
            return null;
        }
    }

    @ManagedOperation
    public void remove(K key) {
        Address dest_node = getNode(key);
        try {
            if (dest_node.equals(local_addr)) {
                l2_cache.remove(key);
            } else {
                disp.callRemoteMethod(dest_node, new MethodCall(REMOVE, new Object[] { key }), GroupRequest.GET_NONE, call_timeout);
            }
            if (l1_cache != null) l1_cache.remove(key);
        } catch (Throwable t) {
            if (log.isWarnEnabled()) log.warn("_remove() failed", t);
        }
    }

    public V _put(K key, V val, long caching_time) {
        if (log.isTraceEnabled()) log.trace("_put(" + key + ", " + val + ", " + caching_time + ")");
        return l2_cache.put(key, val, caching_time);
    }

    public Cache.Value<V> _get(K key) {
        if (log.isTraceEnabled()) log.trace("_get(" + key + ")");
        return l2_cache.getEntry(key);
    }

    public V _remove(K key) {
        if (log.isTraceEnabled()) log.trace("_remove(" + key + ")");
        return l2_cache.remove(key);
    }

    public void viewAccepted(View new_view) {
        System.out.println("view = " + new_view);
        this.view = new_view;
        for (MembershipListener l : membership_listeners) {
            l.viewAccepted(new_view);
        }
        if (migrate_data) {
            migrateData();
        }
    }

    public void suspect(Address suspected_mbr) {
    }

    public void block() {
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (l1_cache != null) sb.append("L1 cache: " + l1_cache.getSize() + " entries");
        sb.append("\nL2 cache: " + l2_cache.getSize() + "entries()");
        return sb.toString();
    }

    @ManagedOperation
    public String dump() {
        StringBuilder sb = new StringBuilder();
        if (l1_cache != null) {
            sb.append("L1 cache:\n").append(l1_cache.dump());
        }
        sb.append("\nL2 cache:\n").append(l2_cache.dump());
        return sb.toString();
    }

    private void migrateData() {
        for (Map.Entry<K, Cache.Value<V>> entry : l2_cache.entrySet()) {
            K key = entry.getKey();
            Address node = getNode(key);
            if (!node.equals(local_addr)) {
                Cache.Value<V> val = entry.getValue();
                put(key, val.getValue(), val.getTimeout());
                l2_cache.remove(key);
                if (log.isTraceEnabled()) log.trace("migrated " + key + " from " + local_addr + " to " + node);
            }
        }
    }

    private void sendPut(Address dest, K key, V val, long caching_time, boolean synchronous) {
        try {
            int mode = synchronous ? GroupRequest.GET_ALL : GroupRequest.GET_NONE;
            disp.callRemoteMethod(dest, new MethodCall(PUT, new Object[] { key, val, caching_time }), mode, call_timeout);
        } catch (Throwable t) {
            if (log.isWarnEnabled()) log.warn("_put() failed", t);
        }
    }

    private Address getNode(K key) {
        return hash_function.hash(key, null);
    }

    public static class ConsistentHashFunction<K> extends MembershipListenerAdapter implements HashFunction<K> {

        private SortedMap<Short, Address> nodes = new TreeMap<Short, Address>();

        private static final int HASH_SPACE = 2000;

        public Address hash(K key, List<Address> members) {
            int hash = Math.abs(key.hashCode());
            int index = hash % HASH_SPACE;
            if (members != null && !members.isEmpty()) {
                SortedMap<Short, Address> tmp = new TreeMap<Short, Address>(nodes);
                for (Iterator<Map.Entry<Short, Address>> it = tmp.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<Short, Address> entry = it.next();
                    if (!members.contains(entry.getValue())) {
                        it.remove();
                    }
                }
                return findFirst(tmp, index);
            }
            return findFirst(nodes, index);
        }

        public void viewAccepted(View new_view) {
            nodes.clear();
            for (Address node : new_view.getMembers()) {
                int hash = Math.abs(node.hashCode()) % HASH_SPACE;
                for (int i = hash; i < hash + HASH_SPACE; i++) {
                    short new_index = (short) (i % HASH_SPACE);
                    if (!nodes.containsKey(new_index)) {
                        nodes.put(new_index, node);
                        break;
                    }
                }
            }
            if (log.isTraceEnabled()) {
                StringBuilder sb = new StringBuilder("node mappings:\n");
                for (Map.Entry<Short, Address> entry : nodes.entrySet()) {
                    sb.append(entry.getKey() + ": " + entry.getValue()).append("\n");
                }
                log.trace(sb);
            }
        }

        private static Address findFirst(Map<Short, Address> map, int index) {
            Address retval;
            for (int i = index; i < index + HASH_SPACE; i++) {
                short new_index = (short) (i % HASH_SPACE);
                retval = map.get(new_index);
                if (retval != null) return retval;
            }
            return null;
        }
    }

    /**
     * Uses arrays to store hash values of addresses, plus addresses.
     */
    public static class ArrayBasedConsistentHashFunction<K> extends MembershipListenerAdapter implements HashFunction<K> {

        Object[] nodes = null;

        private static final int HASH_SPACE = 2000;

        public Address hash(K key, List<Address> members) {
            int hash = Math.abs(key.hashCode());
            int index = hash % HASH_SPACE;
            if (members != null && !members.isEmpty()) {
                Object[] tmp = new Object[nodes.length];
                System.arraycopy(nodes, 0, tmp, 0, nodes.length);
                for (int i = 0; i < tmp.length; i += 2) {
                    if (!members.contains(tmp[i + 1])) {
                        tmp[i] = tmp[i + 1] = null;
                    }
                }
                return findFirst(tmp, index);
            }
            return findFirst(nodes, index);
        }

        public void viewAccepted(View new_view) {
            nodes = new Object[new_view.size() * 2];
            int index = 0;
            for (Address node : new_view.getMembers()) {
                int hash = Math.abs(node.hashCode()) % HASH_SPACE;
                nodes[index++] = hash;
                nodes[index++] = node;
            }
            if (log.isTraceEnabled()) {
                StringBuilder sb = new StringBuilder("node mappings:\n");
                for (int i = 0; i < nodes.length; i += 2) {
                    sb.append(nodes[i] + ": " + nodes[i + 1]).append("\n");
                }
                log.trace(sb);
            }
        }

        public void suspect(Address suspected_mbr) {
        }

        public void block() {
        }

        private static Address findFirst(Object[] array, int index) {
            Address retval = null;
            if (array == null) return null;
            for (int i = 0; i < array.length; i += 2) {
                if (array[i] == null) continue;
                if (array[i + 1] != null) retval = (Address) array[i + 1];
                if (((Integer) array[i]) >= index) return (Address) array[i + 1];
            }
            return retval;
        }
    }

    private static class CustomMarshaller implements RpcDispatcher.Marshaller {

        static final byte NULL = 0;

        static final byte OBJ = 1;

        static final byte METHOD_CALL = 2;

        static final byte VALUE = 3;

        public byte[] objectToByteBuffer(Object obj) throws Exception {
            ByteArrayOutputStream out_stream = new ByteArrayOutputStream(35);
            DataOutputStream out = new DataOutputStream(out_stream);
            try {
                if (obj == null) {
                    out_stream.write(NULL);
                    out_stream.flush();
                    return out_stream.toByteArray();
                }
                if (obj instanceof MethodCall) {
                    out.writeByte(METHOD_CALL);
                    MethodCall call = (MethodCall) obj;
                    out.writeShort(call.getId());
                    Object[] args = call.getArgs();
                    if (args == null || args.length == 0) {
                        out.writeShort(0);
                    } else {
                        out.writeShort(args.length);
                        for (int i = 0; i < args.length; i++) {
                            Util.objectToStream(args[i], out);
                        }
                    }
                } else if (obj instanceof Cache.Value) {
                    Cache.Value value = (Cache.Value) obj;
                    out.writeByte(VALUE);
                    out.writeLong(value.getTimeout());
                    Util.objectToStream(value.getValue(), out);
                } else {
                    out.writeByte(OBJ);
                    Util.objectToStream(obj, out);
                }
                out.flush();
                return out_stream.toByteArray();
            } finally {
                Util.close(out);
            }
        }

        public Object objectFromByteBuffer(byte[] buf) throws Exception {
            if (buf == null) return null;
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(buf));
            byte type = in.readByte();
            if (type == NULL) return null;
            if (type == METHOD_CALL) {
                short id = in.readShort();
                short length = in.readShort();
                Object[] args = length > 0 ? new Object[length] : null;
                if (args != null) {
                    for (int i = 0; i < args.length; i++) args[i] = Util.objectFromStream(in);
                }
                return new MethodCall(id, args);
            } else if (type == VALUE) {
                long expiration_time = in.readLong();
                Object obj = Util.objectFromStream(in);
                return new Cache.Value(obj, expiration_time);
            } else return Util.objectFromStream(in);
        }
    }
}
