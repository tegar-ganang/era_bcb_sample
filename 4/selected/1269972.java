package org.apache.hadoop.io;

import java.io.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.util.Progressable;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.DefaultCodec;

/** A file-based map from keys to values.
 * 
 * <p>A map is a directory containing two files, the <code>data</code> file,
 * containing all keys and values in the map, and a smaller <code>index</code>
 * file, containing a fraction of the keys.  The fraction is determined by
 * {@link Writer#getIndexInterval()}.
 *
 * <p>The index file is read entirely into memory.  Thus key implementations
 * should try to keep themselves small.
 *
 * <p>Map files are created by adding entries in-order.  To maintain a large
 * database, perform updates by copying the previous version of a database and
 * merging in a sorted change list, to create a new version of the database in
 * a new file.  Sorting large change lists can be done with {@link
 * SequenceFile.Sorter}.
 */
public class MapFile {

    private static final Log LOG = LogFactory.getLog(MapFile.class);

    /** The name of the index file. */
    public static final String INDEX_FILE_NAME = "index";

    /** The name of the data file. */
    public static final String DATA_FILE_NAME = "data";

    protected MapFile() {
    }

    /** Writes a new map. */
    public static class Writer implements java.io.Closeable {

        private SequenceFile.Writer data;

        private SequenceFile.Writer index;

        private static final String INDEX_INTERVAL = "io.map.index.interval";

        private int indexInterval = 128;

        private long size;

        private LongWritable position = new LongWritable();

        private WritableComparator comparator;

        private DataInputBuffer inBuf = new DataInputBuffer();

        private DataOutputBuffer outBuf = new DataOutputBuffer();

        private WritableComparable lastKey;

        /** Create the named map for keys of the named class. */
        public Writer(Configuration conf, FileSystem fs, String dirName, Class<? extends WritableComparable> keyClass, Class valClass) throws IOException {
            this(conf, fs, dirName, WritableComparator.get(keyClass), valClass, SequenceFile.getCompressionType(conf));
        }

        /** Create the named map for keys of the named class. */
        public Writer(Configuration conf, FileSystem fs, String dirName, Class<? extends WritableComparable> keyClass, Class valClass, CompressionType compress, Progressable progress) throws IOException {
            this(conf, fs, dirName, WritableComparator.get(keyClass), valClass, compress, progress);
        }

        /** Create the named map for keys of the named class. */
        public Writer(Configuration conf, FileSystem fs, String dirName, Class<? extends WritableComparable> keyClass, Class valClass, CompressionType compress, CompressionCodec codec, Progressable progress) throws IOException {
            this(conf, fs, dirName, WritableComparator.get(keyClass), valClass, compress, codec, progress);
        }

        /** Create the named map for keys of the named class. */
        public Writer(Configuration conf, FileSystem fs, String dirName, Class<? extends WritableComparable> keyClass, Class valClass, CompressionType compress) throws IOException {
            this(conf, fs, dirName, WritableComparator.get(keyClass), valClass, compress);
        }

        /** Create the named map using the named key comparator. */
        public Writer(Configuration conf, FileSystem fs, String dirName, WritableComparator comparator, Class valClass) throws IOException {
            this(conf, fs, dirName, comparator, valClass, SequenceFile.getCompressionType(conf));
        }

        /** Create the named map using the named key comparator. */
        public Writer(Configuration conf, FileSystem fs, String dirName, WritableComparator comparator, Class valClass, SequenceFile.CompressionType compress) throws IOException {
            this(conf, fs, dirName, comparator, valClass, compress, null);
        }

        /** Create the named map using the named key comparator. */
        public Writer(Configuration conf, FileSystem fs, String dirName, WritableComparator comparator, Class valClass, SequenceFile.CompressionType compress, Progressable progress) throws IOException {
            this(conf, fs, dirName, comparator, valClass, compress, new DefaultCodec(), progress);
        }

        /** Create the named map using the named key comparator. */
        public Writer(Configuration conf, FileSystem fs, String dirName, WritableComparator comparator, Class valClass, SequenceFile.CompressionType compress, CompressionCodec codec, Progressable progress) throws IOException {
            this.indexInterval = conf.getInt(INDEX_INTERVAL, this.indexInterval);
            this.comparator = comparator;
            this.lastKey = comparator.newKey();
            Path dir = new Path(dirName);
            if (!fs.mkdirs(dir)) {
                throw new IOException("Mkdirs failed to create directory " + dir.toString());
            }
            Path dataFile = new Path(dir, DATA_FILE_NAME);
            Path indexFile = new Path(dir, INDEX_FILE_NAME);
            Class keyClass = comparator.getKeyClass();
            this.data = SequenceFile.createWriter(fs, conf, dataFile, keyClass, valClass, compress, codec, progress);
            this.index = SequenceFile.createWriter(fs, conf, indexFile, keyClass, LongWritable.class, CompressionType.BLOCK, progress);
        }

        /** The number of entries that are added before an index entry is added.*/
        public int getIndexInterval() {
            return indexInterval;
        }

        /** Sets the index interval.
     * @see #getIndexInterval()
     */
        public void setIndexInterval(int interval) {
            indexInterval = interval;
        }

        /** Sets the index interval and stores it in conf
     * @see #getIndexInterval()
     */
        public static void setIndexInterval(Configuration conf, int interval) {
            conf.setInt(INDEX_INTERVAL, interval);
        }

        /** Close the map. */
        public synchronized void close() throws IOException {
            data.close();
            index.close();
        }

        /** Append a key/value pair to the map.  The key must be greater or equal
     * to the previous key added to the map. */
        public synchronized void append(WritableComparable key, Writable val) throws IOException {
            checkKey(key);
            if (size % indexInterval == 0) {
                position.set(data.getLength());
                index.append(key, position);
            }
            data.append(key, val);
            size++;
        }

        private void checkKey(WritableComparable key) throws IOException {
            if (size != 0 && comparator.compare(lastKey, key) > 0) throw new IOException("key out of order: " + key + " after " + lastKey);
            outBuf.reset();
            key.write(outBuf);
            inBuf.reset(outBuf.getData(), outBuf.getLength());
            lastKey.readFields(inBuf);
        }
    }

    /** Provide access to an existing map. */
    public static class Reader implements java.io.Closeable {

        /** Number of index entries to skip between each entry.  Zero by default.
     * Setting this to values larger than zero can facilitate opening large map
     * files using less memory. */
        private int INDEX_SKIP = 0;

        private WritableComparator comparator;

        private WritableComparable nextKey;

        private long seekPosition = -1;

        private int seekIndex = -1;

        private long firstPosition;

        private SequenceFile.Reader data;

        private SequenceFile.Reader index;

        private boolean indexClosed = false;

        private int count = -1;

        private WritableComparable[] keys;

        private long[] positions;

        /** Returns the class of keys in this file. */
        public Class<?> getKeyClass() {
            return data.getKeyClass();
        }

        /** Returns the class of values in this file. */
        public Class<?> getValueClass() {
            return data.getValueClass();
        }

        /** Construct a map reader for the named map.*/
        public Reader(FileSystem fs, String dirName, Configuration conf) throws IOException {
            this(fs, dirName, null, conf);
            INDEX_SKIP = conf.getInt("io.map.index.skip", 0);
        }

        /** Construct a map reader for the named map using the named comparator.*/
        public Reader(FileSystem fs, String dirName, WritableComparator comparator, Configuration conf) throws IOException {
            this(fs, dirName, comparator, conf, true);
        }

        /**
     * Hook to allow subclasses to defer opening streams until further
     * initialization is complete.
     * @see #createDataFileReader(FileSystem, Path, Configuration)
     */
        protected Reader(FileSystem fs, String dirName, WritableComparator comparator, Configuration conf, boolean open) throws IOException {
            if (open) {
                open(fs, dirName, comparator, conf);
            }
        }

        protected synchronized void open(FileSystem fs, String dirName, WritableComparator comparator, Configuration conf) throws IOException {
            Path dir = new Path(dirName);
            Path dataFile = new Path(dir, DATA_FILE_NAME);
            Path indexFile = new Path(dir, INDEX_FILE_NAME);
            this.data = createDataFileReader(fs, dataFile, conf);
            this.firstPosition = data.getPosition();
            if (comparator == null) this.comparator = WritableComparator.get(data.getKeyClass().asSubclass(WritableComparable.class)); else this.comparator = comparator;
            this.index = new SequenceFile.Reader(fs, indexFile, conf);
        }

        /**
     * Override this method to specialize the type of
     * {@link SequenceFile.Reader} returned.
     */
        protected SequenceFile.Reader createDataFileReader(FileSystem fs, Path dataFile, Configuration conf) throws IOException {
            return new SequenceFile.Reader(fs, dataFile, conf);
        }

        private void readIndex() throws IOException {
            if (this.keys != null) return;
            this.count = 0;
            this.keys = new WritableComparable[1024];
            this.positions = new long[1024];
            try {
                int skip = INDEX_SKIP;
                LongWritable position = new LongWritable();
                WritableComparable lastKey = null;
                while (true) {
                    WritableComparable k = comparator.newKey();
                    if (!index.next(k, position)) break;
                    if (lastKey != null && comparator.compare(lastKey, k) > 0) throw new IOException("key out of order: " + k + " after " + lastKey);
                    lastKey = k;
                    if (skip > 0) {
                        skip--;
                        continue;
                    } else {
                        skip = INDEX_SKIP;
                    }
                    if (count == keys.length) {
                        int newLength = (keys.length * 3) / 2;
                        WritableComparable[] newKeys = new WritableComparable[newLength];
                        long[] newPositions = new long[newLength];
                        System.arraycopy(keys, 0, newKeys, 0, count);
                        System.arraycopy(positions, 0, newPositions, 0, count);
                        keys = newKeys;
                        positions = newPositions;
                    }
                    keys[count] = k;
                    positions[count] = position.get();
                    count++;
                }
            } catch (EOFException e) {
                LOG.warn("Unexpected EOF reading " + index + " at entry #" + count + ".  Ignoring.");
            } finally {
                indexClosed = true;
                index.close();
            }
        }

        /** Re-positions the reader before its first key. */
        public synchronized void reset() throws IOException {
            data.seek(firstPosition);
        }

        /** Get the key at approximately the middle of the file.
     * 
     * @throws IOException
     */
        public synchronized WritableComparable midKey() throws IOException {
            readIndex();
            int pos = ((count - 1) / 2);
            if (pos < 0) {
                throw new IOException("MapFile empty");
            }
            return keys[pos];
        }

        /** Reads the final key from the file.
     *
     * @param key key to read into
     */
        public synchronized void finalKey(WritableComparable key) throws IOException {
            long originalPosition = data.getPosition();
            try {
                readIndex();
                if (count > 0) {
                    data.seek(positions[count - 1]);
                } else {
                    reset();
                }
                while (data.next(key)) {
                }
            } finally {
                data.seek(originalPosition);
            }
        }

        /** Positions the reader at the named key, or if none such exists, at the
     * first entry after the named key.  Returns true iff the named key exists
     * in this map.
     */
        public synchronized boolean seek(WritableComparable key) throws IOException {
            return seekInternal(key) == 0;
        }

        /** 
     * Positions the reader at the named key, or if none such exists, at the
     * first entry after the named key.
     *
     * @return  0   - exact match found
     *          < 0 - positioned at next record
     *          1   - no more records in file
     */
        private synchronized int seekInternal(WritableComparable key) throws IOException {
            return seekInternal(key, false);
        }

        /** 
     * Positions the reader at the named key, or if none such exists, at the
     * key that falls just before or just after dependent on how the
     * <code>before</code> parameter is set.
     * 
     * @param before - IF true, and <code>key</code> does not exist, position
     * file at entry that falls just before <code>key</code>.  Otherwise,
     * position file at record that sorts just after.
     * @return  0   - exact match found
     *          < 0 - positioned at next record
     *          1   - no more records in file
     */
        private synchronized int seekInternal(WritableComparable key, final boolean before) throws IOException {
            readIndex();
            if (seekIndex != -1 && seekIndex + 1 < count && comparator.compare(key, keys[seekIndex + 1]) < 0 && comparator.compare(key, nextKey) >= 0) {
            } else {
                seekIndex = binarySearch(key);
                if (seekIndex < 0) seekIndex = -seekIndex - 2;
                if (seekIndex == -1) seekPosition = firstPosition; else seekPosition = positions[seekIndex];
            }
            data.seek(seekPosition);
            if (nextKey == null) nextKey = comparator.newKey();
            long prevPosition = -1;
            long curPosition = seekPosition;
            while (data.next(nextKey)) {
                int c = comparator.compare(key, nextKey);
                if (c <= 0) {
                    if (before && c != 0) {
                        if (prevPosition == -1) {
                            data.seek(curPosition);
                        } else {
                            data.seek(prevPosition);
                            data.next(nextKey);
                            return 1;
                        }
                    }
                    return c;
                }
                if (before) {
                    prevPosition = curPosition;
                    curPosition = data.getPosition();
                }
            }
            return 1;
        }

        private int binarySearch(WritableComparable key) {
            int low = 0;
            int high = count - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                WritableComparable midVal = keys[mid];
                int cmp = comparator.compare(midVal, key);
                if (cmp < 0) low = mid + 1; else if (cmp > 0) high = mid - 1; else return mid;
            }
            return -(low + 1);
        }

        /** Read the next key/value pair in the map into <code>key</code> and
     * <code>val</code>.  Returns true if such a pair exists and false when at
     * the end of the map */
        public synchronized boolean next(WritableComparable key, Writable val) throws IOException {
            return data.next(key, val);
        }

        /** Return the value for the named key, or null if none exists. */
        public synchronized Writable get(WritableComparable key, Writable val) throws IOException {
            if (seek(key)) {
                data.getCurrentValue(val);
                return val;
            } else return null;
        }

        /** 
     * Finds the record that is the closest match to the specified key.
     * Returns <code>key</code> or if it does not exist, at the first entry
     * after the named key.
     * 
-     * @param key       - key that we're trying to find
-     * @param val       - data value if key is found
-     * @return          - the key that was the closest match or null if eof.
     */
        public synchronized WritableComparable getClosest(WritableComparable key, Writable val) throws IOException {
            return getClosest(key, val, false);
        }

        /** 
     * Finds the record that is the closest match to the specified key.
     * 
     * @param key       - key that we're trying to find
     * @param val       - data value if key is found
     * @param before    - IF true, and <code>key</code> does not exist, return
     * the first entry that falls just before the <code>key</code>.  Otherwise,
     * return the record that sorts just after.
     * @return          - the key that was the closest match or null if eof.
     */
        public synchronized WritableComparable getClosest(WritableComparable key, Writable val, final boolean before) throws IOException {
            int c = seekInternal(key, before);
            if ((!before && c > 0) || (before && c < 0)) {
                return null;
            }
            data.getCurrentValue(val);
            return nextKey;
        }

        /** Close the map. */
        public synchronized void close() throws IOException {
            if (!indexClosed) {
                index.close();
            }
            data.close();
        }
    }

    /** Renames an existing map directory. */
    public static void rename(FileSystem fs, String oldName, String newName) throws IOException {
        Path oldDir = new Path(oldName);
        Path newDir = new Path(newName);
        if (!fs.rename(oldDir, newDir)) {
            throw new IOException("Could not rename " + oldDir + " to " + newDir);
        }
    }

    /** Deletes the named map file. */
    public static void delete(FileSystem fs, String name) throws IOException {
        Path dir = new Path(name);
        Path data = new Path(dir, DATA_FILE_NAME);
        Path index = new Path(dir, INDEX_FILE_NAME);
        fs.delete(data, true);
        fs.delete(index, true);
        fs.delete(dir, true);
    }

    /**
   * This method attempts to fix a corrupt MapFile by re-creating its index.
   * @param fs filesystem
   * @param dir directory containing the MapFile data and index
   * @param keyClass key class (has to be a subclass of Writable)
   * @param valueClass value class (has to be a subclass of Writable)
   * @param dryrun do not perform any changes, just report what needs to be done
   * @return number of valid entries in this MapFile, or -1 if no fixing was needed
   * @throws Exception
   */
    public static long fix(FileSystem fs, Path dir, Class<? extends Writable> keyClass, Class<? extends Writable> valueClass, boolean dryrun, Configuration conf) throws Exception {
        String dr = (dryrun ? "[DRY RUN ] " : "");
        Path data = new Path(dir, DATA_FILE_NAME);
        Path index = new Path(dir, INDEX_FILE_NAME);
        int indexInterval = 128;
        if (!fs.exists(data)) {
            throw new Exception(dr + "Missing data file in " + dir + ", impossible to fix this.");
        }
        if (fs.exists(index)) {
            return -1;
        }
        SequenceFile.Reader dataReader = new SequenceFile.Reader(fs, data, conf);
        if (!dataReader.getKeyClass().equals(keyClass)) {
            throw new Exception(dr + "Wrong key class in " + dir + ", expected" + keyClass.getName() + ", got " + dataReader.getKeyClass().getName());
        }
        if (!dataReader.getValueClass().equals(valueClass)) {
            throw new Exception(dr + "Wrong value class in " + dir + ", expected" + valueClass.getName() + ", got " + dataReader.getValueClass().getName());
        }
        long cnt = 0L;
        Writable key = ReflectionUtils.newInstance(keyClass, conf);
        Writable value = ReflectionUtils.newInstance(valueClass, conf);
        SequenceFile.Writer indexWriter = null;
        if (!dryrun) indexWriter = SequenceFile.createWriter(fs, conf, index, keyClass, LongWritable.class);
        try {
            long pos = 0L;
            LongWritable position = new LongWritable();
            while (dataReader.next(key, value)) {
                cnt++;
                if (cnt % indexInterval == 0) {
                    position.set(pos);
                    if (!dryrun) indexWriter.append(key, position);
                }
                pos = dataReader.getPosition();
            }
        } catch (Throwable t) {
        }
        dataReader.close();
        if (!dryrun) indexWriter.close();
        return cnt;
    }

    public static void main(String[] args) throws Exception {
        String usage = "Usage: MapFile inFile outFile";
        if (args.length != 2) {
            System.err.println(usage);
            System.exit(-1);
        }
        String in = args[0];
        String out = args[1];
        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.getLocal(conf);
        MapFile.Reader reader = new MapFile.Reader(fs, in, conf);
        MapFile.Writer writer = new MapFile.Writer(conf, fs, out, reader.getKeyClass().asSubclass(WritableComparable.class), reader.getValueClass());
        WritableComparable key = ReflectionUtils.newInstance(reader.getKeyClass().asSubclass(WritableComparable.class), conf);
        Writable value = ReflectionUtils.newInstance(reader.getValueClass().asSubclass(Writable.class), conf);
        while (reader.next(key, value)) writer.append(key, value);
        writer.close();
    }
}
