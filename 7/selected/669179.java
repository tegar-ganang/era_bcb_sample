package overlaysim.protocol.overlay.pastry;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import overlaysim.node.Node;
import overlaysim.protocol.overlay.GuidTools;

public class LeafSet {

    protected BigInteger MODULUS;

    protected BigInteger my_guid;

    protected NeighborInfo my_neighbor_info;

    protected NeighborInfo[] leaf_preds;

    protected NeighborInfo[] leaf_succs;

    protected int leaf_set_size;

    protected int leaf_pred_count, leaf_succ_count;

    public boolean[] yijian = new boolean[Node.instances.size() + 1];

    public NeighborInfo random_member(Random rand) {
        if (leaf_pred_count + leaf_succ_count == 0) return null;
        int which = rand.nextInt(leaf_pred_count + leaf_succ_count);
        if (which < leaf_pred_count) return leaf_preds[which];
        which -= leaf_pred_count;
        return leaf_succs[which];
    }

    public LeafSet(NeighborInfo ni, int sz, BigInteger mod) {
        MODULUS = mod;
        my_neighbor_info = ni;
        my_guid = GuidTools.instance(ni.node_id);
        leaf_set_size = sz;
        leaf_preds = new NeighborInfo[leaf_set_size];
        leaf_succs = new NeighborInfo[leaf_set_size];
    }

    public LinkedList<NeighborInfo> as_list() {
        LinkedList<NeighborInfo> result = new LinkedList<NeighborInfo>();
        for (int j = leaf_pred_count - 1; j >= 0; --j) result.addLast(leaf_preds[j]);
        for (int j = 0; j < leaf_succ_count; ++j) result.addLast(leaf_succs[j]);
        return result;
    }

    public Set<NeighborInfo> as_set() {
        Set<NeighborInfo> result = new LinkedHashSet<NeighborInfo>();
        for (int j = 0; j < leaf_pred_count; ++j) result.add(leaf_preds[j]);
        for (int j = 0; j < leaf_succ_count; ++j) result.add(leaf_succs[j]);
        return result;
    }

    /**
	     * Returns a set of the nodes between us and the given node in our leaf
	     * set, or null if none exist.
	     */
    public Set<NeighborInfo> intermediates(BigInteger other) {
        Set<NeighborInfo> result = null;
        int i;
        for (i = 0; i < leaf_pred_count; ++i) {
            if (GuidTools.instance(leaf_preds[i].node_id).equals(other)) break;
        }
        if (i < leaf_pred_count) {
            for (i = 0; i < leaf_pred_count; ++i) {
                if (GuidTools.instance(leaf_preds[i].node_id).equals(other)) break;
                if (in_range_mod(other, my_guid, GuidTools.instance(leaf_preds[i].node_id))) {
                    if (result == null) result = new LinkedHashSet<NeighborInfo>();
                    result.add(leaf_preds[i]);
                }
            }
        }
        for (i = 0; i < leaf_succ_count; ++i) {
            if (GuidTools.instance(leaf_succs[i].node_id).equals(other)) break;
        }
        if (i < leaf_succ_count) {
            for (i = 0; i < leaf_succ_count; ++i) {
                if (GuidTools.instance(leaf_succs[i].node_id).equals(other)) break;
                if (in_range_mod(my_guid, other, GuidTools.instance(leaf_succs[i].node_id))) {
                    if (result == null) result = new LinkedHashSet<NeighborInfo>();
                    result.add(leaf_preds[i]);
                }
            }
        }
        return result;
    }

    public boolean contains(NeighborInfo ni) {
        if (ni.node_id == my_neighbor_info.node_id) return false;
        for (int i = 0; i < leaf_pred_count; ++i) if (leaf_preds[i].node_id == ni.node_id) return true;
        for (int i = 0; i < leaf_succ_count; ++i) if (leaf_succs[i].node_id == ni.node_id) return true;
        return false;
    }

    public boolean promising(NeighborInfo ni) {
        if (ni.node_id == my_neighbor_info.node_id) return false;
        if (leaf_pred_count == 0) {
            return true;
        }
        BigInteger ni_guid = GuidTools.instance(ni.node_id);
        boolean duplicate = false;
        for (int i = 0; i < leaf_pred_count; ++i) if (leaf_preds[i].node_id == ni.node_id) duplicate = true;
        if (!duplicate) {
            int i = 0;
            for (i = 0; i < leaf_pred_count; ++i) {
                if (in_range_mod(GuidTools.instance(leaf_preds[i].node_id), my_guid, ni_guid)) return true;
            }
            if ((i == leaf_pred_count) && (leaf_pred_count < leaf_set_size)) {
                return true;
            }
        }
        duplicate = false;
        for (int i = 0; i < leaf_succ_count; ++i) if (leaf_succs[i].node_id == ni.node_id) duplicate = true;
        if (!duplicate) {
            int i;
            for (i = 0; i < leaf_succ_count; ++i) {
                if (in_range_mod(my_guid, GuidTools.instance(leaf_succs[i].node_id), ni_guid)) return true;
            }
            if ((i == leaf_succ_count) && (leaf_succ_count < leaf_set_size)) {
                return true;
            }
        }
        return false;
    }

    public NeighborInfo add_node(NeighborInfo ni) {
        if (yijian[ni.node_id]) {
            return null;
        } else {
            yijian[ni.node_id] = true;
        }
        Set old_leaf_set = null;
        if (ni.node_id == my_neighbor_info.node_id) return null;
        if (leaf_pred_count == 0) {
            leaf_preds[0] = leaf_succs[0] = ni;
            leaf_pred_count = leaf_succ_count = 1;
            return my_neighbor_info;
        }
        BigInteger ni_guid = GuidTools.instance(ni.node_id);
        boolean duplicate = false;
        for (int i = 0; i < leaf_pred_count; ++i) if (leaf_preds[i].node_id == ni.node_id) duplicate = true;
        if (!duplicate) {
            int i = 0;
            for (i = 0; i < leaf_pred_count; ++i) {
                if (in_range_mod(GuidTools.instance(leaf_preds[i].node_id), my_guid, ni_guid)) {
                    if (old_leaf_set == null) old_leaf_set = as_set();
                    for (int j = leaf_set_size - 1; j > i; --j) leaf_preds[j] = leaf_preds[j - 1];
                    leaf_preds[i] = ni;
                    if (leaf_pred_count < leaf_set_size) ++leaf_pred_count;
                    break;
                }
            }
            if ((i == leaf_pred_count) && (leaf_pred_count < leaf_set_size)) {
                if (old_leaf_set == null) old_leaf_set = as_set();
                leaf_preds[leaf_pred_count] = ni;
                ++leaf_pred_count;
            }
        }
        duplicate = false;
        for (int i = 0; i < leaf_succ_count; ++i) if (leaf_succs[i].node_id == ni.node_id) duplicate = true;
        if (!duplicate) {
            int i;
            for (i = 0; i < leaf_succ_count; ++i) {
                if (in_range_mod(my_guid, GuidTools.instance(leaf_succs[i].node_id), ni_guid)) {
                    if (old_leaf_set == null) old_leaf_set = as_set();
                    for (int j = leaf_set_size - 1; j > i; --j) leaf_succs[j] = leaf_succs[j - 1];
                    leaf_succs[i] = ni;
                    if (leaf_succ_count < leaf_set_size) ++leaf_succ_count;
                    break;
                }
            }
            if ((i == leaf_succ_count) && (leaf_succ_count < leaf_set_size)) {
                if (old_leaf_set == null) old_leaf_set = as_set();
                leaf_succs[leaf_succ_count] = ni;
                ++leaf_succ_count;
            }
        }
        if (old_leaf_set != null) {
            old_leaf_set.removeAll(as_set());
            if (old_leaf_set.isEmpty()) return my_neighbor_info;
            if (old_leaf_set.size() != 1) {
                System.err.println("Error adding " + ni);
                System.err.println("old_leaf_set=");
                Iterator i = old_leaf_set.iterator();
                while (i.hasNext()) {
                    NeighborInfo other = (NeighborInfo) i.next();
                    System.err.println("  " + other);
                }
                System.err.println("leaf_set=");
                i = as_set().iterator();
                while (i.hasNext()) {
                    NeighborInfo other = (NeighborInfo) i.next();
                    System.err.println("  " + other);
                }
                assert false;
            }
            return (NeighborInfo) old_leaf_set.iterator().next();
        }
        return null;
    }

    public static final int REMOVED_NONE = 0x0;

    public static final int REMOVED_PREDECESSOR = 0x1;

    public static final int REMOVED_SUCCESSOR = 0x2;

    public static final int REMOVED_BOTH = 0x3;

    public int remove_node(NeighborInfo ni) {
        int result = 0;
        int i;
        for (i = 0; i < leaf_pred_count; ++i) {
            if (leaf_preds[i].node_id == ni.node_id) break;
        }
        if (i != leaf_pred_count) {
            result |= REMOVED_PREDECESSOR;
            for (; i < leaf_pred_count - 1; ++i) leaf_preds[i] = leaf_preds[i + 1];
            --leaf_pred_count;
        }
        for (i = 0; i < leaf_succ_count; ++i) {
            if (leaf_succs[i].node_id == ni.node_id) break;
        }
        if (i != leaf_succ_count) {
            result |= REMOVED_SUCCESSOR;
            for (; i < leaf_succ_count - 1; ++i) leaf_succs[i] = leaf_succs[i + 1];
            --leaf_succ_count;
        }
        return result;
    }

    public NeighborInfo closest_leaf(BigInteger guid, Set ignore) {
        NeighborInfo closest = my_neighbor_info;
        BigInteger dist = calc_dist(my_guid, guid);
        for (int j = 0; j < leaf_pred_count; j++) {
            NeighborInfo ni = leaf_preds[j];
            BigInteger ni_guid = GuidTools.instance(ni.node_id);
            BigInteger this_dist = calc_dist(ni_guid, guid);
            if (this_dist.compareTo(dist) < 0) {
                closest = ni;
                dist = this_dist;
            } else if ((this_dist.compareTo(dist) == 0) && (!ni.equals(closest)) && (in_range_mod(GuidTools.instance(closest.node_id), ni_guid, guid) && (!in_range_mod(ni_guid, GuidTools.instance(closest.node_id), guid)))) {
                closest = ni;
                dist = this_dist;
            }
        }
        for (int j = 0; j < leaf_succ_count; j++) {
            NeighborInfo ni = leaf_succs[j];
            BigInteger ni_guid = GuidTools.instance(ni.node_id);
            BigInteger this_dist = calc_dist(ni_guid, guid);
            if (this_dist.compareTo(dist) < 0) {
                closest = ni;
                dist = this_dist;
            } else if ((this_dist.compareTo(dist) == 0) && (!ni.equals(closest)) && (in_range_mod(GuidTools.instance(closest.node_id), ni_guid, guid) && (!in_range_mod(ni_guid, GuidTools.instance(closest.node_id), guid)))) {
                closest = ni;
                dist = this_dist;
            }
        }
        return closest;
    }

    public String toString() {
        StringBuffer result = new StringBuffer(500);
        for (int i = leaf_pred_count - 1; i >= 0; --i) {
            result.append("  ");
            result.append(0 - i - 1);
            result.append('\t');
            result.append(leaf_preds[i]);
            result.append('\n');
        }
        result.append("  ");
        result.append(0);
        result.append('\t');
        result.append(my_neighbor_info);
        result.append('\n');
        for (int i = 0; i < leaf_succ_count; ++i) {
            result.append("  ");
            result.append(i + 1);
            result.append('\t');
            result.append(leaf_succs[i]);
            result.append('\n');
        }
        return result.toString();
    }

    public final boolean within_leaf_set(BigInteger i) {
        return in_range_mod(leaf_set_low(), my_guid, i) || in_range_mod(my_guid, leaf_set_high(), i);
    }

    public final BigInteger leaf_set_low() {
        if (leaf_pred_count == 0) return my_guid; else return GuidTools.instance(leaf_preds[leaf_pred_count - 1].node_id);
    }

    public final BigInteger leaf_set_high() {
        if (leaf_succ_count == 0) return my_guid; else return GuidTools.instance(leaf_succs[leaf_succ_count - 1].node_id);
    }

    public BigInteger calc_dist(BigInteger a, BigInteger b) {
        return GuidTools.calc_dist(a, b, MODULUS);
    }

    protected boolean in_range_mod(BigInteger low, BigInteger high, BigInteger query) {
        return GuidTools.in_range_mod(low, high, query, MODULUS);
    }
}
