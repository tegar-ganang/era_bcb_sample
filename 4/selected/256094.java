package gnu.gcj.runtime;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.security.MessageDigest;
import java.math.BigInteger;

public class PersistentByteMap {

    private MappedByteBuffer buf;

    private static final int MAGIC = 0;

    private static final int VERSION = 4;

    private static final int CAPACITY = 8;

    private static final int TABLE_BASE = 12;

    private static final int STRING_BASE = 16;

    private static final int STRING_SIZE = 20;

    private static final int FILE_SIZE = 24;

    private static final int ELEMENTS = 28;

    private static final int INT_SIZE = 4;

    private static final int TABLE_ENTRY_SIZE = 2 * INT_SIZE;

    private int capacity;

    private int table_base;

    private int string_base;

    private int string_size;

    private int file_size;

    private int elements;

    private long length;

    private final File name;

    private static final int UNUSED_ENTRY = -1;

    public static final int KEYS = 0;

    public static final int VALUES = 1;

    public static final int ENTRIES = 2;

    private HashMap values;

    FileChannel fc;

    public static final class AccessMode {

        private final FileChannel.MapMode mapMode;

        static {
            READ_ONLY = new AccessMode(FileChannel.MapMode.READ_ONLY);
            READ_WRITE = new AccessMode(FileChannel.MapMode.READ_WRITE);
            PRIVATE = new AccessMode(FileChannel.MapMode.PRIVATE);
        }

        public static final AccessMode READ_ONLY;

        public static final AccessMode READ_WRITE;

        public static final AccessMode PRIVATE;

        private AccessMode(FileChannel.MapMode mode) {
            this.mapMode = mode;
        }
    }

    private PersistentByteMap(File name) {
        this.name = name;
    }

    public PersistentByteMap(String filename, AccessMode mode) throws IOException {
        this(new File(filename), mode);
    }

    public PersistentByteMap(File f, AccessMode mode) throws IOException {
        name = f;
        if (mode == AccessMode.READ_ONLY) {
            FileInputStream fis = new FileInputStream(f);
            fc = fis.getChannel();
        } else {
            RandomAccessFile fos = new RandomAccessFile(f, "rw");
            fc = fos.getChannel();
        }
        length = fc.size();
        buf = fc.map(mode.mapMode, 0, length);
        int magic = getWord(MAGIC);
        if (magic != 0x67636a64) throw new IllegalArgumentException(f.getName());
        table_base = getWord(TABLE_BASE);
        capacity = getWord(CAPACITY);
        string_base = getWord(STRING_BASE);
        string_size = getWord(STRING_SIZE);
        file_size = getWord(FILE_SIZE);
        elements = getWord(ELEMENTS);
    }

    private void init(PersistentByteMap m, File f, int capacity, int strtabSize) throws IOException {
        f.createNewFile();
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        {
            BigInteger size = new BigInteger(Integer.toString(((capacity * 3) + 1) / 2));
            BigInteger two = BigInteger.ONE.add(BigInteger.ONE);
            if (size.getLowestSetBit() != 0) size = size.add(BigInteger.ONE);
            while (!size.isProbablePrime(10)) size = size.add(two);
            this.capacity = capacity = size.intValue();
        }
        table_base = 64;
        string_base = table_base + capacity * TABLE_ENTRY_SIZE;
        string_size = 0;
        file_size = string_base;
        elements = 0;
        int totalFileSize = string_base + strtabSize;
        byte[] _4k = new byte[4096];
        for (long i = 0; i < totalFileSize; i += 4096) raf.write(_4k);
        fc = raf.getChannel();
        buf = fc.map(FileChannel.MapMode.READ_WRITE, 0, raf.length());
        for (int i = 0; i < capacity; i++) putKeyPos(UNUSED_ENTRY, i);
        putWord(0x67636a64, MAGIC);
        putWord(0x01, VERSION);
        putWord(capacity, CAPACITY);
        putWord(table_base, TABLE_BASE);
        putWord(string_base, STRING_BASE);
        putWord(file_size, FILE_SIZE);
        putWord(elements, ELEMENTS);
        buf.force();
        length = fc.size();
        string_size = 0;
    }

    public static PersistentByteMap emptyPersistentByteMap(File name, int capacity, int strtabSize) throws IOException {
        PersistentByteMap m = new PersistentByteMap(name);
        m.init(m, name, capacity, strtabSize);
        return m;
    }

    private int getWord(int index) {
        buf.position(index);
        byte[] wordBuf = new byte[4];
        buf.get(wordBuf);
        int result = (int) wordBuf[0] & 0xff;
        result += ((int) wordBuf[1] & 0xff) << 8;
        result += ((int) wordBuf[2] & 0xff) << 16;
        result += ((int) wordBuf[3] & 0xff) << 24;
        return result;
    }

    private void putWord(int word, int index) {
        buf.position(index);
        byte[] wordBuf = new byte[4];
        wordBuf[0] = (byte) (word);
        wordBuf[1] = (byte) (word >>> 8);
        wordBuf[2] = (byte) (word >>> 16);
        wordBuf[3] = (byte) (word >>> 24);
        buf.put(wordBuf);
    }

    public Set entrySet() {
        return null;
    }

    private int getBucket(int n) {
        return table_base + (2 * n * INT_SIZE);
    }

    private int getKeyPos(int n) {
        return getWord(getBucket(n));
    }

    private int getValuePos(int n) {
        return getWord(getBucket(n) + INT_SIZE);
    }

    private void putKeyPos(int index, int n) {
        putWord(index, getBucket(n));
    }

    private void putValuePos(int index, int n) {
        putWord(index, getBucket(n) + INT_SIZE);
    }

    private byte[] getBytes(int n) {
        int len = getWord(string_base + n);
        int base = string_base + n + INT_SIZE;
        byte[] key = new byte[len];
        buf.position(base);
        buf.get(key, 0, len);
        return key;
    }

    private int hash(byte[] b) {
        long hashIndex = ((b[0] & 0xffL) + ((b[1] & 0xffL) << 8) + ((b[2] & 0xffL) << 16) + ((b[3] & 0xffL) << 24));
        long result = hashIndex % (long) capacity;
        return (int) result;
    }

    public byte[] get(byte[] digest) {
        int hashIndex = hash(digest);
        do {
            int k = getKeyPos(hashIndex);
            if (k == UNUSED_ENTRY) return null;
            if (Arrays.equals((byte[]) digest, getBytes(k))) return getBytes(getValuePos(hashIndex));
            hashIndex++;
            hashIndex %= capacity;
        } while (true);
    }

    public void put(byte[] digest, byte[] value) throws IllegalAccessException {
        int hashIndex = hash(digest);
        if (elements >= capacity()) throw new IllegalAccessException("Table Full: " + elements);
        do {
            int k = getKeyPos(hashIndex);
            if (k == UNUSED_ENTRY) {
                int newKey = addBytes(digest);
                putKeyPos(newKey, hashIndex);
                int newValue = addBytes(value);
                putValuePos(newValue, hashIndex);
                elements++;
                putWord(elements, ELEMENTS);
                return;
            } else if (Arrays.equals(digest, getBytes(k))) {
                int newValue = addBytes((byte[]) value);
                putValuePos(newValue, hashIndex);
                return;
            }
            hashIndex++;
            hashIndex %= capacity;
        } while (true);
    }

    private int addBytes(byte[] data) throws IllegalAccessException {
        if (data.length > 16) {
            if (values == null) {
                values = new HashMap();
                for (int i = 0; i < capacity; i++) if (getKeyPos(i) != UNUSED_ENTRY) {
                    int pos = getValuePos(i);
                    ByteWrapper bytes = new ByteWrapper(getBytes(pos));
                    values.put(bytes, new Integer(pos));
                }
            }
            {
                Object result = values.get(new ByteWrapper(data));
                if (result != null) {
                    return ((Integer) result).intValue();
                }
            }
        }
        if (data.length + INT_SIZE >= this.length) throw new IllegalAccessException("String table Full");
        int extent = string_base + string_size;
        int top = extent;
        putWord(data.length, extent);
        extent += INT_SIZE;
        buf.position(extent);
        buf.put(data, 0, data.length);
        extent += data.length;
        extent += INT_SIZE - 1;
        extent &= ~(INT_SIZE - 1);
        string_size = extent - string_base;
        file_size = extent;
        putWord(string_size, STRING_SIZE);
        putWord(file_size, FILE_SIZE);
        if (data.length > 16) values.put(new ByteWrapper(data), new Integer(top - string_base));
        return top - string_base;
    }

    public Iterator iterator(int type) {
        return new HashIterator(type);
    }

    public int size() {
        return elements;
    }

    public int stringTableSize() {
        return string_size;
    }

    public int capacity() {
        return capacity * 2 / 3;
    }

    public void force() {
        buf.force();
    }

    public File getFile() {
        return name;
    }

    public void close() throws IOException {
        force();
        fc.close();
    }

    public void putAll(PersistentByteMap t) throws IllegalAccessException {
        if (this.elements == 0 && t.capacity == this.capacity && t.length == this.length) {
            this.buf.position(0);
            t.buf.position(0);
            this.buf.put(t.buf);
            this.table_base = t.table_base;
            this.string_base = t.string_base;
            this.string_size = t.string_size;
            this.file_size = t.file_size;
            this.elements = t.elements;
            if (t.values != null) this.values = (HashMap) t.values.clone();
            return;
        }
        Iterator iterator = t.iterator(PersistentByteMap.ENTRIES);
        while (iterator.hasNext()) {
            PersistentByteMap.MapEntry entry = (PersistentByteMap.MapEntry) iterator.next();
            this.put((byte[]) entry.getKey(), (byte[]) entry.getValue());
        }
    }

    private final class HashIterator implements Iterator {

        /** Current index in the physical hash table. */
        private int idx;

        private int count;

        private final int type;

        /**
     * Construct a new HashIterator with the supplied type.
     * @param type {@link #KEYS}, {@link #VALUES}, or {@link #ENTRIES}
     */
        HashIterator(int type) {
            this.type = type;
            count = elements;
            idx = 0;
        }

        /**
     * Returns true if the Iterator has more elements.
     * @return true if there are more elements
     * @throws ConcurrentModificationException if the HashMap was modified
     */
        public boolean hasNext() {
            return count > 0;
        }

        /**
     * Returns the next element in the Iterator's sequential view.
     * @return the next element
     * @throws ConcurrentModificationException if the HashMap was modified
     * @throws NoSuchElementException if there is none
     */
        public Object next() {
            count--;
            for (int i = idx; i < capacity; i++) if (getKeyPos(i) != UNUSED_ENTRY) {
                idx = i + 1;
                if (type == VALUES) return getBytes(getValuePos(i));
                if (type == KEYS) return getBytes(getKeyPos(i));
                return new MapEntry(i, getBytes(getKeyPos(i)), getBytes(getValuePos(i)));
            }
            return null;
        }

        /**
     * Remove from the underlying collection the last element returned
     * by next (optional operation). This method can be called only
     * once after each call to <code>next()</code>. It does not affect
     * what will be returned by subsequent calls to next.
     *
     * @throws IllegalStateException if next has not yet been called
     *         or remove has already been called since the last call
     *         to next.
     * @throws UnsupportedOperationException if this Iterator does not
     *         support the remove operation.
     */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static final class MapEntry {

        private final Object key;

        private final Object value;

        private final int bucket;

        public MapEntry(int bucket, Object newKey, Object newValue) {
            this.key = newKey;
            this.value = newValue;
            this.bucket = bucket;
        }

        public final Object getKey() {
            return key;
        }

        public final Object getValue() {
            return value;
        }

        public final int getBucket() {
            return bucket;
        }
    }

    private final class ByteWrapper {

        final byte[] bytes;

        final int hash;

        public ByteWrapper(byte[] bytes) {
            int sum = 0;
            this.bytes = bytes;
            for (int i = 0; i < bytes.length; i++) sum += bytes[i];
            hash = sum;
        }

        public int hashCode() {
            return hash;
        }

        public boolean equals(Object obj) {
            return Arrays.equals(bytes, ((ByteWrapper) obj).bytes);
        }
    }
}
