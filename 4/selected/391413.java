package net.sf.mybatchfwk.history;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * The KeysFileIndexer classe is used in order to index keys contained into a file (one line = one key).<br>
 * This indexer (based on the HashMap class) store into the memory only the hash key and the line number of each key
 * (so it can be used to work with a large number of keys).
 * 
 * @author J�r�me Bert�che (cyberteche@users.sourceforge.net)
 */
public class KeysFileIndexer {

    /**
     * The keys reader
     */
    private RandomAccessFile reader;

    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     **/
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    transient Entry[] table;

    /**
     * The number of key-value mappings contained in this identity hash map.
     */
    transient int size;

    /**
     * The next size value at which to resize (capacity * load factor).
     * @serial
     */
    int threshold;

    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    final float loadFactor;

    /**
     * The number of times this KeysFileIndexer has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the KeysFileIndexer or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the KeysFileIndexer fail-fast.  (See ConcurrentModificationException).
     */
    transient volatile int modCount;

    /**
     * Constructs an empty <tt>KeysFileIndexer</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     * @throws IOException 
     */
    public KeysFileIndexer(File keysFile, String mode) throws IOException {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = (int) (DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
        table = new Entry[DEFAULT_INITIAL_CAPACITY];
        reader = new RandomAccessFile(keysFile, mode);
        buildIndex();
    }

    /**
     * Index the keys of the file
     * @throws IOException
     */
    protected void buildIndex() throws IOException {
        String key = null;
        long pointer = 0;
        while ((key = reader.readLine()) != null) {
            if (key.length() == 0) {
                pointer = reader.getFilePointer();
                continue;
            }
            putKey(pointer, key);
            pointer = reader.getFilePointer();
        }
    }

    /**
     * Close the keys reader
     * @throws IOException
     */
    public void closeReader() throws IOException {
        reader.close();
    }

    /**
     * Returns a hash value for the specified object.  In addition to 
     * the object's own hashCode, this method applies a "supplemental
     * hash function," which defends against poor quality hash functions.
     * This is critical because HashMap uses power-of two length 
     * hash tables.<p>
     *
     * The shift distances in this function were chosen as the result
     * of an automated search over the entire four-dimensional search space.
     */
    static int hash(Object x) {
        int h = x.hashCode();
        h += ~(h << 9);
        h ^= (h >>> 14);
        h += (h << 4);
        h ^= (h >>> 10);
        return h;
    }

    /**
     * Returns index for hash code h. 
     */
    static int indexFor(int h, int length) {
        return h & (length - 1);
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map.
     */
    public int size() {
        return size;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Add a new key to the end of the file
     * @param key
     * @throws IOException
     */
    public void addKey(String key) throws IOException {
        reader.seek(reader.length());
        long pointer = reader.getFilePointer();
        reader.write(key.getBytes());
        reader.write("\n".getBytes());
        putKey(pointer, key);
    }

    /**
     * Add a new entry from the given key
     * @param pointer
     * @param key
     */
    protected void putKey(long pointer, String key) {
        int hash = hash(key);
        int i = indexFor(hash, table.length);
        for (Entry e = table[i]; e != null; e = e.next) {
            if (e.hash == hash && (pointer == e.pointer)) {
                return;
            }
        }
        modCount++;
        addEntry(hash, pointer, i);
        return;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the
     * specified key.
     *
     * @param   key   The key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     * @throws IOException 
     */
    public boolean containsKey(String key) throws IOException {
        int hash = hash(key);
        int i = indexFor(hash, table.length);
        Entry e = table[i];
        while (e != null) {
            if (e.hash == hash) {
                reader.seek(e.pointer);
                if (key.equals(reader.readLine())) {
                    return true;
                }
            }
            e = e.next;
        }
        return false;
    }

    /**
     * Rehashes the contents of this map into a new array with a
     * larger capacity.  This method is called automatically when the
     * number of keys in this map reaches its threshold.
     *
     * If current capacity is MAXIMUM_CAPACITY, this method does not
     * resize the map, but sets threshold to Integer.MAX_VALUE.
     * This has the effect of preventing future calls.
     *
     * @param newCapacity the new capacity, MUST be a power of two;
     *        must be greater than current capacity unless current
     *        capacity is MAXIMUM_CAPACITY (in which case value
     *        is irrelevant).
     */
    void resize(int newCapacity) {
        Entry[] oldTable = table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }
        Entry[] newTable = new Entry[newCapacity];
        transfer(newTable);
        table = newTable;
        threshold = (int) (newCapacity * loadFactor);
    }

    /** 
     * Transfer all entries from current table to newTable.
     */
    void transfer(Entry[] newTable) {
        Entry[] src = table;
        int newCapacity = newTable.length;
        for (int j = 0; j < src.length; j++) {
            Entry e = src[j];
            if (e != null) {
                src[j] = null;
                do {
                    Entry next = e.next;
                    int i = indexFor(e.hash, newCapacity);
                    e.next = newTable[i];
                    newTable[i] = e;
                    e = next;
                } while (e != null);
            }
        }
    }

    /**
     * An entry contains a hash key and a pointer to the line
     */
    static class Entry {

        final int hash;

        final long pointer;

        Entry next;

        /**
         * Create new entry.
         */
        Entry(int hash, long pointer, Entry next) {
            this.next = next;
            this.pointer = pointer;
            this.hash = hash;
        }

        public long getPointer() {
            return pointer;
        }
    }

    /**
     * Add a new entry with the specified key, value and hash code to
     * the specified bucket.  It is the responsibility of this 
     * method to resize the table if appropriate.
     *
     * Subclass overrides this to alter the behavior of put method.
     */
    void addEntry(int hash, long pointer, int bucketIndex) {
        Entry e = table[bucketIndex];
        table[bucketIndex] = new Entry(hash, pointer, e);
        if (size++ >= threshold) resize(2 * table.length);
    }
}
