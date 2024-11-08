package dbs_project.myDB.indexlayer.myBpulstree;

public class InternalNodeArrayMap {

    /** n keys stored on this internal node */
    private Comparable[] keys;

    /**
	 * n + 1 pointers stored in this internal node - left is index-aligned with
	 * keys
	 */
    private BPlusTreeNode[] nodes;

    /**
	 * number of keys in this map. Note that we have one extra pointer to the
	 * left (position 0).
	 */
    private int currentSize = 0;

    /**
	 * @param a
	 * 		Array with keys
	 * @param key
	 * 		The key we are looking for
	 * @return
	 * 		The position in the array of the key we search for
	 */
    private int binarySearch(Comparable key) {
        int low = 0;
        int high = currentSize - 1;
        for (; low <= high; ) {
            int mid = (low + high) >> 1;
            int result = this.compareTo(keys[mid], key);
            if (result < 0) low = mid + 1; else if (result > 0) high = mid - 1; else return mid;
        }
        return -(low + 1);
    }

    /**
	 * @param n
	 * 		The maximum number of elements in the node
	 */
    public InternalNodeArrayMap(int n) {
        keys = new Comparable[n];
        nodes = new BPlusTreeNode[n + 1];
        nodes[0] = InternalNode.NULL;
    }

    /**
	 * @param zeroNode
	 * 		The leftmost first pointer to a child node
	 */
    public void setZeroNodePointer(BPlusTreeNode zeroNode) {
        nodes[0] = zeroNode;
    }

    /**
	 * @return
	 * 		The middle key of this mapping
	 */
    public Comparable getMidKey() {
        return keys[currentSize / 2];
    }

    /**
	 * Splits this map, keeps entries from 0 to (mid-1) and returns a new map
	 * with entries from (mid+1) to (currentSize-1). The key mid is no longer
	 * present in either map and thus should be promoted.
	 * 
	 * @return
	 * 		The new map which results from the split
	 */
    public InternalNodeArrayMap split() {
        InternalNodeArrayMap newMap = new InternalNodeArrayMap(keys.length);
        final int mid = currentSize / 2;
        int count = 0;
        newMap.nodes[0] = nodes[mid + 1];
        for (int i = mid + 1; i < currentSize; i++) {
            newMap.keys[count] = keys[i];
            newMap.nodes[++count] = nodes[i + 1];
        }
        for (int i = mid; i < currentSize; i++) {
            nodes[i + 1] = null;
        }
        newMap.currentSize = currentSize - mid - 1;
        currentSize = mid;
        return newMap;
    }

    /**
	 * Puts the given key to rightNode association in the node array map.
	 * 
	 * @param key
	 * 		The key we want to put node for
	 * @param rightNode
	 * 		The node we want to put
	 * @return
	 * 		NODE_IS_FULL if the current node is full 
	 */
    public int put(Comparable key, BPlusTreeNode rightNode) {
        if (currentSize == 0) {
            keys[0] = key;
            nodes[1] = rightNode;
            currentSize++;
            return 1;
        }
        int pos = binarySearch(key);
        if (pos >= 0) {
            keys[pos] = key;
            nodes[pos + 1] = rightNode;
        } else {
            if (currentSize == keys.length) return BPlusTreeNode.NODE_IS_FULL;
            pos = -(pos + 1);
            if (pos < currentSize) {
                System.arraycopy(keys, pos, keys, pos + 1, currentSize - pos);
                System.arraycopy(nodes, pos + 1, nodes, pos + 2, currentSize - pos);
                keys[pos] = key;
                nodes[pos + 1] = rightNode;
                currentSize++;
            } else {
                keys[currentSize] = key;
                nodes[currentSize + 1] = rightNode;
                currentSize++;
            }
        }
        return 1;
    }

    /**
	 * Returns the node corresponding to the interval in which the provided key
	 * falls.
	 * 
	 * @param key
	 * 		The key we search for
	 * @return
	 * 		The node which may contain the key
	 */
    public BPlusTreeNode get(Comparable key) {
        int pos = getIntervalPosition(key);
        if (pos == -1) return null; else return nodes[pos];
    }

    /**
	 * Obtains the position in the nodes array that represents the interval in
	 * which the provided key falls.
	 * 
	 * @param key
	 * 		The key we are searching for
	 * @return
	 * 		The position in the nodes array
	 */
    public int getIntervalPosition(Comparable key) {
        if (currentSize == 0) {
            return -1;
        } else {
            int pos = binarySearch(key);
            if (pos < 0) {
                pos = -(pos + 1);
            } else {
                pos++;
            }
            return pos;
        }
    }

    /**
	 * Tries to delete a key from the mapping. This method does not touch the
	 * left-most node in the array map, as the left-most node property of having
	 * keys smaller than the key of the left-most key will be kept if that key
	 * is deleted.
	 * 
	 * @param key
	 * 		The key we want to delete
	 * @return
	 * 		True if successful. False if the key was not found.
	 */
    public boolean delete(Comparable key) {
        if (currentSize == 0) {
            return false;
        }
        int pos = binarySearch(key);
        if (pos >= 0) {
            deleteAtPos(pos);
            return true;
        } else {
            return false;
        }
    }

    /**
	 * Deletes the key-node mapping at the given position.
	 * 
	 * @param pos
	 * 		The position in the nodes array
	 */
    private void deleteAtPos(int pos) {
        System.arraycopy(keys, pos + 1, keys, pos, currentSize - pos);
        System.arraycopy(nodes, pos + 2, nodes, pos + 1, currentSize - pos);
        nodes[currentSize] = null;
        currentSize--;
    }

    /**
	 * @param offset
	 * 		Number of spaces to leave in front of each row
	 * @return
	 * 		String with the formatted contents
	 */
    public String toString(int offset) {
        StringBuffer sb = new StringBuffer();
        String offsetString = "\n";
        for (int i = 0; i < offset; i++) offsetString += ".";
        offsetString += "|";
        if (nodes[0] == InternalNode.NULL) {
            sb.append(offsetString + "NULL");
        } else {
            String nodeValue = nodes[0] == null ? null : nodes[0].toString(offset + 1);
            sb.append(nodeValue);
        }
        for (int i = 0; i < currentSize; i++) {
            sb.append(offsetString + keys[i]);
            String nodeValue = nodes[i + 1] == null ? null : nodes[i + 1].toString(offset + 1);
            sb.append(nodeValue);
        }
        return sb.toString();
    }

    /**
	 * @return
	 * 		The current size of the mapping
	 */
    public int size() {
        return currentSize;
    }

    /**
	 * @param pos
	 * 		The position of the node
	 * @return
	 * 		Node at the input position
	 */
    public BPlusTreeNode getNode(int pos) {
        if (pos >= nodes.length) return null;
        return nodes[pos];
    }

    /**
	 * This method is used for bulk loading. It appends a whole mapping key->List<Values>
	 * 
	 * @param key
	 * 		The key
	 * @param rightNode
	 * 		The right node we are inserting
	 * @return
	 * 		NODE_IS_FULL or 1 if the operation is successful
	 */
    public int bulkAppend(Comparable key, BPlusTreeNode rightNode) {
        if (currentSize == keys.length) return BPlusTreeNode.NODE_IS_FULL;
        keys[currentSize] = key;
        nodes[currentSize + 1] = rightNode;
        currentSize++;
        return 1;
    }

    public int compareTo(Comparable key, Comparable o) {
        if (key != null && o != null) return key.compareTo(o);
        if (key == null && o != null) return -1;
        if (key != null && o == null) return 1; else return 0;
    }
}
