package org.pfyshnet.pfysh_database;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class PfyshIndexer {

    public static long KEYMASK = 0x7FFFFFFFFFFFFFFFL;

    private static long RECORDSIZE = 2 * (Long.SIZE / Byte.SIZE);

    private String FileName;

    private String Root;

    private RandomAccessFile RAF;

    private long FirstKey;

    private long LastKey;

    public PfyshIndexer(String root, String file) {
        Root = root;
        FileName = file;
        RAF = null;
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                close();
            }
        });
    }

    public String getFileName() {
        return Root + File.separator + FileName;
    }

    private synchronized int Find(long key) throws IOException {
        if (key != (key & KEYMASK)) {
            throw new IOException("Invalid Key! " + key);
        }
        if (key <= FirstKey) {
            RAF.seek(0);
            return -1;
        } else if (key > LastKey) {
            RAF.seek(RAF.length());
            return 1;
        } else {
            long numrecords = RAF.length() / RECORDSIZE;
            long lastlow = 0;
            long lasthi = numrecords;
            long pos = numrecords / 2;
            while (lasthi > (lastlow + 1)) {
                RAF.seek(pos * RECORDSIZE);
                long tk = RAF.readLong();
                if (tk < key) {
                    lastlow = pos;
                    pos = (lasthi + pos) / 2;
                } else {
                    lasthi = pos;
                    pos = (lastlow + pos) / 2;
                }
            }
            RAF.seek(pos * RECORDSIZE);
            long tk = RAF.readLong();
            if (tk >= key) {
                RAF.seek(pos * RECORDSIZE);
            } else {
                RAF.readLong();
            }
            return 0;
        }
    }

    private synchronized File extractToEnd() throws IOException {
        File f = File.createTempFile("tmp1", ".dat", new File(Root));
        RandomAccessFile oraf = new RandomAccessFile(f, "rw");
        oraf.getChannel().transferFrom(RAF.getChannel(), 0, RAF.length() - RAF.getFilePointer());
        oraf.close();
        return f;
    }

    private synchronized void attachToEnd(File f) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        RAF.getChannel().transferFrom(raf.getChannel(), RAF.getFilePointer(), raf.length());
        raf.close();
    }

    private void DeleteFile(File f) throws IOException {
        if (!f.delete() && f.exists()) {
            RandomAccessFile raf = new RandomAccessFile(f, "rw");
            raf.getChannel().force(true);
            raf.close();
            if (!f.delete()) {
                throw new RuntimeException("Could not delete file: " + f.getPath());
            }
        }
    }

    public synchronized void display() throws IOException {
        System.out.println("=============================");
        RAF.seek(0);
        while (RAF.getFilePointer() < RAF.length()) {
            long k = RAF.readLong();
            long v = RAF.readLong();
            System.out.println("K: " + Long.toHexString(k) + "  V: " + Long.toHexString(v));
        }
    }

    public synchronized void Init() throws IOException {
        if (RAF == null) {
            File dir = new File(Root);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File f = new File(getFileName());
            RAF = new RandomAccessFile(f, "rw");
            if (RAF.length() >= RECORDSIZE) {
                if ((RAF.length() % RECORDSIZE) != 0) {
                    throw new IOException("Corrupt index file");
                }
                FirstKey = RAF.readLong();
                RAF.seek(RAF.length() - RECORDSIZE);
                LastKey = RAF.readLong();
            } else {
                FirstKey = Long.MIN_VALUE;
                LastKey = Long.MIN_VALUE;
            }
        }
    }

    public synchronized void close() {
        try {
            if (RAF != null) {
                RAF.close();
                RAF = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void reset() throws IOException {
        RAF.getChannel().truncate(0);
        FirstKey = Long.MIN_VALUE;
        LastKey = Long.MIN_VALUE;
    }

    public synchronized void Insert(long key, long value) throws IOException {
        if (key != (key & KEYMASK)) {
            throw new IOException("Invalid Key! " + key);
        }
        long idx = Find(key);
        if (idx < 1) {
            long pos = RAF.getFilePointer();
            File tf = extractToEnd();
            RAF.seek(pos);
            RAF.writeLong(key);
            RAF.writeLong(value);
            attachToEnd(tf);
            DeleteFile(tf);
        } else {
            RAF.writeLong(key);
            RAF.writeLong(value);
        }
        if (FirstKey == Long.MIN_VALUE) {
            FirstKey = key;
        }
        if (idx == -1) {
            FirstKey = key;
        }
        if (idx == 1) {
            LastKey = key;
        }
    }

    public synchronized void Delete(long value) throws IOException {
        File of = File.createTempFile("tmp2", ".dat", new File(Root));
        RandomAccessFile oraf = new RandomAccessFile(of, "rw");
        RAF.seek(0);
        long pos = 0;
        long numrecords = RAF.length() / RECORDSIZE;
        FirstKey = Long.MIN_VALUE;
        LastKey = Long.MIN_VALUE;
        while (pos < numrecords) {
            long key = RAF.readLong();
            long tvl = RAF.readLong();
            if (tvl != value) {
                oraf.writeLong(key);
                oraf.writeLong(tvl);
            } else {
                if (FirstKey == Long.MIN_VALUE) {
                    FirstKey = key;
                }
                LastKey = key;
            }
            pos++;
        }
        oraf.close();
        close();
        File f = new File(getFileName());
        DeleteFile(f);
        if (!of.renameTo(f)) {
            throw new IOException("Failed to move file from :" + of.getPath() + " to " + f.getPath());
        }
        Init();
    }

    public synchronized void DeleteKey(long key) throws IOException {
        if (key != (key & KEYMASK)) {
            throw new IOException("Invalid Key! " + key);
        }
        Find(key);
        long pos = RAF.getFilePointer();
        if (pos <= (RAF.length() - RECORDSIZE)) {
            long kv = RAF.readLong();
            RAF.readLong();
            if (kv == key) {
                File tf = extractToEnd();
                RAF.seek(pos);
                attachToEnd(tf);
                RAF.getChannel().truncate(RAF.length() - RECORDSIZE);
                DeleteFile(tf);
                if (RAF.length() == 0) {
                    FirstKey = Long.MIN_VALUE;
                    LastKey = Long.MIN_VALUE;
                } else {
                    if (pos == 0) {
                        RAF.seek(0);
                        FirstKey = RAF.readLong();
                    }
                    if (pos == RAF.length()) {
                        RAF.seek(pos - RECORDSIZE);
                        LastKey = RAF.readLong();
                    }
                }
                DeleteKey(key);
            }
        }
    }

    public synchronized void DeleteKey(long key, long val) throws IOException {
        if (key != (key & KEYMASK)) {
            throw new IOException("Invalid Key! " + key);
        }
        Find(key);
        long pos = RAF.getFilePointer();
        if (pos <= (RAF.length() - RECORDSIZE)) {
            long kv = RAF.readLong();
            long vv = RAF.readLong();
            if (kv == key) {
                boolean atend = false;
                File tf = File.createTempFile("tmp3", ".dat", new File(Root));
                RandomAccessFile traf = new RandomAccessFile(tf, "rw");
                while (kv == key && RAF.getFilePointer() < RAF.length()) {
                    if (vv != val) {
                        traf.writeLong(kv);
                        traf.writeLong(vv);
                    }
                    kv = RAF.readLong();
                    vv = RAF.readLong();
                }
                if (kv != key || vv != val) {
                    traf.writeLong(kv);
                    traf.writeLong(vv);
                }
                if (RAF.getFilePointer() < RAF.length()) {
                    traf.getChannel().transferFrom(RAF.getChannel(), traf.getFilePointer(), RAF.length() - RAF.getFilePointer());
                } else {
                    atend = true;
                }
                traf.close();
                RAF.seek(pos);
                attachToEnd(tf);
                RAF.getChannel().truncate(pos + tf.length());
                DeleteFile(tf);
                if (RAF.length() == 0) {
                    FirstKey = Long.MIN_VALUE;
                    LastKey = Long.MIN_VALUE;
                } else {
                    if (pos == 0) {
                        RAF.seek(0);
                        FirstKey = RAF.readLong();
                    }
                    if (atend) {
                        RAF.seek(RAF.length() - RECORDSIZE);
                        LastKey = RAF.readLong();
                    }
                }
            }
        }
    }

    public synchronized List<IndexKeyValue> getKeyAndAfter(long key) throws IOException {
        if (key != (key & KEYMASK)) {
            throw new IOException("Invalid Key! " + key);
        }
        LinkedList<IndexKeyValue> vals = new LinkedList<IndexKeyValue>();
        Find(key);
        while (RAF.getFilePointer() < RAF.length()) {
            long kv = RAF.readLong();
            long val = RAF.readLong();
            IndexKeyValue ikv = new IndexKeyValue(kv, val);
            vals.add(ikv);
        }
        return vals;
    }

    public synchronized List<IndexKeyValue> getRandKeyAndAfter(long key, int max, Random rand) throws IOException {
        if (key != (key & KEYMASK)) {
            throw new IOException("Invalid Key! " + key);
        }
        LinkedList<IndexKeyValue> vals = new LinkedList<IndexKeyValue>();
        Find(key);
        long cutpoint = RAF.getFilePointer();
        int numtopickfrom = (int) ((RAF.length() - cutpoint) / RECORDSIZE);
        max = Math.min(max, numtopickfrom);
        while (max > 0) {
            long randpos = cutpoint + ((long) ((double) numtopickfrom * rand.nextDouble()) * RECORDSIZE);
            RAF.seek(randpos);
            long kv = RAF.readLong();
            long val = RAF.readLong();
            IndexKeyValue ikv = new IndexKeyValue(kv, val);
            if (!vals.contains(ikv)) {
                vals.add(ikv);
                max--;
            }
        }
        return vals;
    }

    public synchronized IndexKeyValue getValue(long key) throws IOException {
        if (key != (key & KEYMASK)) {
            throw new IOException("Invalid Key! " + key);
        }
        IndexKeyValue val = null;
        Find(key);
        if (RAF.getFilePointer() < RAF.length()) {
            long kv = RAF.readLong();
            if (kv == key) {
                long vv = RAF.readLong();
                val = new IndexKeyValue(kv, vv);
            }
        }
        return val;
    }

    public synchronized List<IndexKeyValue> deleteBeforeKey(long key, int max) throws IOException {
        if (key != (key & KEYMASK)) {
            throw new IOException("Invalid Key! " + key);
        }
        LinkedList<IndexKeyValue> rl = new LinkedList<IndexKeyValue>();
        if (RAF.length() >= RECORDSIZE) {
            RAF.seek(0);
            long kv = Long.MIN_VALUE;
            long vv = Long.MIN_VALUE;
            while (RAF.getFilePointer() < RAF.length() && kv <= key && max > 0) {
                kv = RAF.readLong();
                vv = RAF.readLong();
                if (kv <= key) {
                    rl.add(new IndexKeyValue(kv, vv));
                }
                max--;
            }
            if (kv > key) {
                RAF.seek(RAF.getFilePointer() - RECORDSIZE);
            }
            if (rl.size() > 0) {
                long newsize = RAF.length() - RAF.getFilePointer();
                File tf = extractToEnd();
                RAF.seek(0);
                attachToEnd(tf);
                DeleteFile(tf);
                RAF.getChannel().truncate(newsize);
            }
        }
        return rl;
    }

    /**
	 * This is basically for finding nodes at a given level.  The start and end 
	 * values are masked ID values up to the level we want.
	 * Example.  We want to find all the nodes in level 0x100111XXXXXX
	 * We would set start = 0x100111000000
	 *                end = 0x101000000000 
	 *               mask = 0xFFFFFF000000
	 * @param start
	 * @param end
	 * @return
	 * @throws IOException
	 */
    public synchronized List<IndexKeyValue> findRandomKeys(long mask, long start, long end, int max, long time, Random rand) throws IOException {
        mask = mask & KEYMASK;
        start = start & KEYMASK;
        end = end & KEYMASK;
        LinkedList<IndexKeyValue> rl = new LinkedList<IndexKeyValue>();
        int idx = Find(start & mask);
        if (idx < 1 && RAF.getFilePointer() < RAF.length()) {
            long startpos = RAF.getFilePointer();
            long val = RAF.readLong();
            if ((val & mask) == (start & mask)) {
                Find(end & mask);
                long endpos = RAF.getFilePointer();
                int numrecs = (int) ((endpos - startpos) / RECORDSIZE);
                boolean inorder = false;
                if (numrecs <= max) {
                    inorder = true;
                }
                long trys = max * 5;
                long rec = 0;
                if (!inorder) {
                    rec = (long) ((double) numrecs * rand.nextDouble());
                }
                while (max > 0 && trys > 0 && rec < numrecs) {
                    long pos = startpos + ((long) rec * RECORDSIZE);
                    RAF.seek(pos);
                    long key = RAF.readLong();
                    long tim = RAF.readLong();
                    IndexKeyValue kv = new IndexKeyValue(key, tim);
                    if ((key & mask) != (start & mask)) {
                        throw new IOException("Index file seems to be corrupted.");
                    }
                    if (!rl.contains(kv) && tim >= time) {
                        rl.add(kv);
                        max--;
                    }
                    if (inorder) {
                        rec++;
                    } else {
                        rec = (long) ((double) numrecs * rand.nextDouble());
                    }
                    trys--;
                }
            }
        }
        return rl;
    }

    public synchronized void setValue(long key, long value) throws IOException {
        if (key != (key & KEYMASK)) {
            throw new IOException("Invalid Key! " + key);
        }
        long idx = Find(key);
        if (idx < 1) {
            long pos = RAF.getFilePointer();
            long tkey = RAF.readLong();
            if (tkey == key) {
                RAF.writeLong(value);
            } else {
                RAF.seek(pos);
                File tf = extractToEnd();
                RAF.seek(pos);
                RAF.writeLong(key);
                RAF.writeLong(value);
                attachToEnd(tf);
                DeleteFile(tf);
            }
        } else {
            RAF.writeLong(key);
            RAF.writeLong(value);
        }
        if (FirstKey == Long.MIN_VALUE) {
            FirstKey = key;
        }
        if (idx == -1) {
            FirstKey = key;
        }
        if (idx == 1) {
            LastKey = key;
        }
    }

    public synchronized long size() throws IOException {
        return RAF.length() / RECORDSIZE;
    }

    public synchronized long getFirstKey() {
        return FirstKey;
    }

    public synchronized long getLastKey() {
        return LastKey;
    }

    public class IndexKeyValue {

        public IndexKeyValue(long k, long v) {
            Key = k;
            Value = v;
        }

        public boolean equals(Object a) {
            if (a instanceof IndexKeyValue) {
                IndexKeyValue ta = (IndexKeyValue) a;
                return ta.Key == Key && ta.Value == Value;
            }
            return false;
        }

        public int hashCode() {
            return (int) (Key ^ Value);
        }

        public long Key;

        public long Value;
    }
}
