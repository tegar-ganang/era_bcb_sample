package net.oesterholt.jndbm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import net.oesterholt.jndbm.datastruct.Blob;
import net.oesterholt.jndbm.datastruct.Bucket;
import net.oesterholt.jndbm.datastruct.Types;
import net.oesterholt.jndbm.logging.NDbmLogProvider;
import net.oesterholt.jndbm.logging.NDbmLogger;
import net.oesterholt.jndbm.streams.NDbmByteArrayInputStream;
import net.oesterholt.jndbm.streams.NDbmByteArrayOutputStream;
import net.oesterholt.jndbm.streams.NDbmDataInputStream;
import net.oesterholt.jndbm.streams.NDbmDataOutputStream;
import net.oesterholt.jndbm.streams.NDbmRandomAccessFile;
import net.oesterholt.jndbm.util.MurmurHash;

public class NDbm extends NDbmEncDec {

    private static Hashtable<String, NDbm> _existingNDbms = new Hashtable<String, NDbm>();

    @SuppressWarnings("unused")
    private static boolean _exitHandlerInstalled = false;

    private static NDbmLogger logger = NDbm.getLogger(NDbm.class);

    private static NDbmLogProvider logProvider = null;

    private File _base;

    private File _meta;

    private File _index;

    private File _dbase;

    private NDbmRandomAccessFile _f_idx;

    private NDbmRandomAccessFile _f_meta;

    private NDbmRandomAccessFile _f_dbase;

    private FileChannel _c_meta;

    private FileLock _lock = null;

    private static int FORMAT_VERSION = 1210;

    private static int TABLE_SIZE = 100;

    @SuppressWarnings("unused")
    private static int S_VERSION = 0;

    private static int S_TABLESIZE = NDbmDataInputStream.sizeOfInt();

    private static int S_FIRSTGAP = 2 * NDbmDataInputStream.sizeOfInt();

    private static int S_NUMOFGAPS = 3 * NDbmDataInputStream.sizeOfInt();

    private static int S_COLLISIONS = 4 * NDbmDataInputStream.sizeOfInt();

    private static String RW_ATTR = "rws";

    private static String R_ATTR = "r";

    private String _attr = RW_ATTR;

    private boolean _closed = true;

    private boolean _closedAtExit = false;

    private boolean _readonly = false;

    private boolean _optimizing = false;

    private boolean versionSupported(int v) {
        return (v == 1100 || v == FORMAT_VERSION);
    }

    public static String infoVersion() {
        return "1.23";
    }

    public static String infoWebSite() {
        return "http://oesterholt.net/index.php?env=data&page=jndbm";
    }

    public static String infoLicense() {
        return "LGPL (c) 2009-2011 Hans Oesterholt-Dijkema";
    }

    public static NDbm openNDbm(File base, boolean readonly) {
        String bs = base.getAbsolutePath();
        if (_existingNDbms.containsKey(bs)) {
            return _existingNDbms.get(bs);
        } else {
            _existingNDbms.put(bs, new NDbm(base, readonly));
            return _existingNDbms.get(bs);
        }
    }

    public static synchronized void closeAllNDbms() {
        Set<String> keys = _existingNDbms.keySet();
        Iterator<String> it = keys.iterator();
        Vector<String> dbms = new Vector<String>();
        while (it.hasNext()) {
            dbms.add(it.next());
        }
        it = dbms.iterator();
        while (it.hasNext()) {
            String key = it.next();
            NDbm db = _existingNDbms.get(key);
            db.closeAtExit();
            _existingNDbms.remove(key);
        }
    }

    public NDbm(File base, boolean readonly) {
        _base = base;
        _readonly = readonly;
        init(MurmurHash.getPrimeLength(TABLE_SIZE));
    }

    public NDbm(File base, int table_len, boolean readonly) {
        _base = base;
        _readonly = readonly;
        init(MurmurHash.getPrimeLength(table_len));
    }

    public NDbm(File base, int table_len, boolean readonly, String attr) {
        _base = base;
        _attr = attr;
        _readonly = readonly;
        init(MurmurHash.getPrimeLength(table_len));
    }

    private void init() {
        init(TABLE_SIZE);
    }

    public static File[] dbFiles(File base) {
        File[] f = { new File(base.getAbsolutePath() + ".mta"), new File(base.getAbsolutePath() + ".idx"), new File(base.getAbsolutePath() + ".dbm") };
        return f;
    }

    private synchronized void init(int _table_size) {
        _meta = new File(_base.getAbsolutePath() + ".mta");
        _index = new File(_base.getAbsolutePath() + ".idx");
        _dbase = new File(_base.getAbsolutePath() + ".dbm");
        String attr = (_readonly) ? R_ATTR : _attr;
        if (_meta.exists()) {
            try {
                _f_meta = new NDbmRandomAccessFile(_meta, attr);
                _f_idx = new NDbmRandomAccessFile(_index, attr);
                _f_dbase = new NDbmRandomAccessFile(_dbase, attr);
                _c_meta = _f_meta.getChannel();
                int v = _f_meta.readInt();
                if (!versionSupported(v)) {
                    logger.fatal(String.format("Unexpected: version of NDbm format = %d, expected %d", v, FORMAT_VERSION));
                }
                _closed = false;
            } catch (Exception E) {
                logger.fatal(E);
            }
        } else if (!_readonly) {
            try {
                _f_meta = new NDbmRandomAccessFile(_meta, RW_ATTR);
                _f_idx = new NDbmRandomAccessFile(_index, RW_ATTR);
                _f_dbase = new NDbmRandomAccessFile(_dbase, RW_ATTR);
                _f_meta.writeInt(FORMAT_VERSION);
                _f_meta.writeInt(_table_size);
                int first_empty_node = -1;
                _f_meta.writeInt(first_empty_node);
                int numOfGaps = 0;
                _f_meta.writeInt(numOfGaps);
                int collisions = 0;
                _f_meta.writeInt(collisions);
                int i;
                Bucket B = new Bucket();
                for (i = 0; i < _table_size; i++) {
                    B.writeNext(_f_idx);
                }
                _f_dbase.close();
                _f_idx.close();
                _f_meta.close();
                init();
            } catch (Exception E) {
                logger.fatal(E);
            }
        } else {
            logger.fatal("Trying to open a non existing database readonly");
        }
    }

    public synchronized void close() {
        if (!_closed) {
            try {
                logger.info("closing database");
                _f_idx.close();
                _f_meta.close();
                _f_dbase.close();
                _closed = true;
            } catch (Exception E) {
                logger.fatal(E);
            }
        }
    }

    private synchronized void closeAtExit() {
        if (!_closedAtExit) {
            _closedAtExit = true;
            close();
        }
    }

    protected void finalize() throws Throwable {
        logger.info("Finalizing");
        close();
        closeAtExit();
        super.finalize();
    }

    public static void removeDb(File _base) {
        File _meta, _index, _dbase;
        _meta = new File(_base.getAbsolutePath() + ".mta");
        _index = new File(_base.getAbsolutePath() + ".idx");
        _dbase = new File(_base.getAbsolutePath() + ".dbm");
        _meta.delete();
        _index.delete();
        _dbase.delete();
    }

    public void setUnsafe() {
        _attr = "rw";
        close();
        init();
    }

    public void setSafe() {
        _attr = "rws";
        close();
        init();
    }

    public static void setGlobalUnsafe() {
        RW_ATTR = "rw";
    }

    public static void setGlobalSafe() {
        RW_ATTR = "rws";
    }

    public static void setGlobalLogProvider(NDbmLogProvider lp) {
        logProvider = lp;
        logger = logProvider.getLogger(NDbm.class);
    }

    @SuppressWarnings("unchecked")
    public static NDbmLogger getLogger(Class c) {
        if (logProvider == null) {
            logProvider = new NDbmLogProvider() {

                public NDbmLogger getLogger(Class c) {
                    return NDbmLogger.getLogger(c);
                }
            };
        }
        return logProvider.getLogger(c);
    }

    NDbmByteArrayOutputStream pstr_bout = new NDbmByteArrayOutputStream();

    NDbmDataOutputStream pstr_dout = new NDbmDataOutputStream(pstr_bout);

    /**
	 * Puts a string in NDbm under the given key (end point function).
	 * 
	 * @param key
	 * @param data
	 *            - may be null.
	 */
    public synchronized void putStr(String key, String data) {
        if (data != null) {
            try {
                pstr_bout.reset();
                writeString(pstr_dout, data);
                writeBlob(new Blob(key, pstr_bout.bytes(), pstr_bout.size()));
            } catch (Exception E) {
                logger.error(_base + ":" + E.getMessage());
                logger.fatal(E);
            }
        }
    }

    NDbmByteArrayOutputStream pb_bout = new NDbmByteArrayOutputStream();

    NDbmDataOutputStream pb_dout = new NDbmDataOutputStream(pb_bout);

    /**
	 * Puts a string in NDbm under the given key (end point function).
	 * 
	 * @param key
	 * @param data
	 *            - may be null.
	 */
    public synchronized void putBoolean(String key, Boolean data) {
        if (data != null) {
            try {
                pb_bout.reset();
                writeBoolean(pb_dout, data);
                writeBlob(new Blob(key, pb_bout.bytes(), pb_bout.size()));
            } catch (Exception E) {
                logger.error(_base + ":" + E.getMessage());
                logger.fatal(E);
            }
        }
    }

    /**
	 * Puts an object in NDbm under the given key (end point function).
	 * 
	 * @param key
	 * @param i
	 */
    public synchronized void putObject(String key, int i) {
        putInt(key, i);
    }

    /**
	 * Puts an object in NDbm under the given key (end point function).
	 * 
	 * @param key
	 * @param i
	 */
    public synchronized void putObject(String key, long i) {
        putLong(key, i);
    }

    /**
	 * Puts an object in NDbm under the given key (end point function).
	 * 
	 * @param key
	 * @param b
	 *            - may be null.
	 */
    public synchronized void putObject(String key, boolean b) {
        putBoolean(key, b);
    }

    NDbmByteArrayOutputStream pobj_bout = new NDbmByteArrayOutputStream();

    NDbmDataOutputStream pobj_dout = new NDbmDataOutputStream(pobj_bout);

    /**
	 * Puts an object in NDbm under the given key (end point function).
	 * 
	 * @param key
	 * @param data
	 *            - may be null.
	 */
    public synchronized void putObject(String key, Object data) {
        if (data != null) {
            try {
                if (data instanceof Boolean) {
                    putBoolean(key, (Boolean) data);
                } else if (data instanceof Integer) {
                    putInt(key, (Integer) data);
                } else if (data instanceof String) {
                    putStr(key, (String) data);
                } else if (data instanceof Date) {
                    putDate(key, (Date) data);
                } else if (data instanceof Long) {
                    putLong(key, (Long) data);
                } else {
                    pobj_bout.reset();
                    writeType(pobj_dout, Types.TYPE_OBJECT);
                    ObjectOutputStream oout = new ObjectOutputStream(pobj_bout);
                    oout.writeObject(data);
                    oout.close();
                    writeBlob(new Blob(key, pobj_bout.bytes(), pobj_bout.size()));
                }
            } catch (Exception E) {
                logger.error(_base + ":" + E.getMessage());
                logger.fatal(E);
            }
        }
    }

    NDbmByteArrayOutputStream pv_bout = new NDbmByteArrayOutputStream();

    NDbmDataOutputStream pv_dout = new NDbmDataOutputStream(pv_bout);

    /**
	 * Puts a Vector of String in NDbm under the given key (end point function).
	 * 
	 * @param key
	 * @param data
	 *            - may be null.
	 */
    public synchronized void putVectorOfString(String key, Vector<String> data) {
        if (data != null) {
            try {
                pv_bout.reset();
                writeVectorOfString(pv_dout, data);
                writeBlob(new Blob(key, pv_bout.bytes(), pv_bout.size()));
            } catch (Exception E) {
                logger.error(_base + ":" + E.getMessage());
                logger.fatal(E);
            }
        }
    }

    /**
	 * Puts an object in NDbm under the given key (recursable function).
	 * 
	 * @param key
	 * @param wrt
	 */
    public synchronized void putObject(String key, NDbmObjectWriter wrt) {
        try {
            NDbmByteArrayOutputStream pobj1_bout = new NDbmByteArrayOutputStream();
            NDbmDataOutputStream pobj1_dout = new NDbmDataOutputStream(pobj1_bout);
            super.writeObject(pobj1_dout, wrt);
            writeBlob(new Blob(key, pobj1_bout.bytes(), pobj1_bout.size()));
        } catch (Exception E) {
            logger.error(_base + ":" + E.getMessage());
            logger.fatal(E);
        }
    }

    public synchronized void putBlob(String key, Blob b) {
        try {
            writeBlob(b);
        } catch (Exception E) {
            logger.error(_base + ":" + E.getMessage());
            logger.fatal(E);
        }
    }

    NDbmByteArrayOutputStream pi_bout = new NDbmByteArrayOutputStream();

    NDbmDataOutputStream pi_dout = new NDbmDataOutputStream(pi_bout);

    /**
	 * Puts an Integer in NDbm under the given key (end point function).
	 * 
	 * @param key
	 * @param data
	 *            - may be null.
	 */
    public synchronized void putInt(String key, int data) {
        try {
            pi_bout.reset();
            writeInt(pi_dout, data);
            writeBlob(new Blob(key, pi_bout.bytes(), pi_bout.size()));
        } catch (Exception E) {
            logger.error(_base + ":" + E.getMessage());
            logger.fatal(E);
        }
    }

    NDbmByteArrayOutputStream pl_bout = new NDbmByteArrayOutputStream();

    NDbmDataOutputStream pl_dout = new NDbmDataOutputStream(pl_bout);

    /**
	 * Puts a long in NDbm under the given key (end point function).
	 * 
	 * @param key
	 * @param data
	 *            - may be null.
	 */
    public synchronized void putLong(String key, long data) {
        try {
            pl_bout.reset();
            writeLong(pl_dout, data);
            writeBlob(new Blob(key, pl_bout.bytes(), pl_bout.size()));
        } catch (Exception E) {
            logger.error(_base + ":" + E.getMessage());
            logger.fatal(E);
        }
    }

    NDbmByteArrayOutputStream pd_bout = new NDbmByteArrayOutputStream();

    NDbmDataOutputStream pd_dout = new NDbmDataOutputStream(pd_bout);

    /**
	 * Puts a Date in NDbm under the given key (end point function).
	 * 
	 * @param key
	 * @param data
	 *            - may be null.
	 */
    public synchronized void putDate(String key, Date data) {
        try {
            pd_bout.reset();
            writeDate(pd_dout, data);
            writeBlob(new Blob(key, pd_bout.bytes(), pd_bout.size()));
        } catch (Exception E) {
            logger.error(_base + ":" + E.getMessage());
            logger.fatal(E);
        }
    }

    NDbmByteArrayInputStream gobj_bin = new NDbmByteArrayInputStream();

    NDbmDataInputStream gobj_din = new NDbmDataInputStream(gobj_bin);

    /**
	 * Reads an object from NDbm under given key (end point function). Return
	 * value maybe null.
	 * 
	 * @param key
	 * @return
	 */
    public synchronized Object getObject(String key) {
        try {
            Blob data = readBlob(key);
            if (data == null) {
                return null;
            } else {
                gobj_bin.reset(data.getData());
                char type = peekType(gobj_din);
                if (isType(type, Types.TYPE_OBJECT)) {
                    checkType(gobj_din, Types.TYPE_OBJECT);
                    ObjectInputStream oin = new ObjectInputStream(gobj_bin);
                    Object o = oin.readObject();
                    oin.close();
                    return o;
                } else if (isType(type, Types.TYPE_BOOLEAN)) {
                    return readBoolean(gobj_din);
                } else if (isType(type, Types.TYPE_DATE)) {
                    return readDate(gobj_din);
                } else if (isType(type, Types.TYPE_INT)) {
                    return readInt(gobj_din);
                } else if (isType(type, Types.TYPE_STRING)) {
                    return readString(gobj_din);
                } else if (isType(type, Types.TYPE_VECTOROFSTRING)) {
                    return readVectorOfString(gobj_din);
                } else {
                    logger.fatal("Unknown type");
                    return null;
                }
            }
        } catch (Exception E) {
            logger.fatal(E);
            logger.error(_base + ":" + E.getMessage());
            return null;
        }
    }

    /**
	 * Reads an object from NDbm under given key (recursable function). Return
	 * value maybe null.
	 * 
	 * @param key
	 * @param rdr
	 */
    public synchronized void getObject(String key, NDbmObjectReader rdr) {
        try {
            Blob data = readBlob(key);
            if (data == null) {
                rdr.nildata();
            } else {
                NDbmByteArrayInputStream gobj1_bin = new NDbmByteArrayInputStream(data.getData());
                NDbmDataInputStream gobj1_din = new NDbmDataInputStream(gobj1_bin);
                readObject(gobj1_din, rdr);
            }
        } catch (Exception E) {
            logger.error(_base + ":" + E.getMessage());
            logger.fatal(E);
        }
    }

    public synchronized Blob getBlob(String key) {
        try {
            return readBlob(key);
        } catch (Exception E) {
            logger.error(_base + ":" + E.getMessage());
            logger.fatal(E);
            return null;
        }
    }

    NDbmByteArrayInputStream gi_bin = new NDbmByteArrayInputStream();

    NDbmDataInputStream gi_din = new NDbmDataInputStream(gi_bin);

    /**
	 * Reads an Integer from NDbm under given key (end point function). Return
	 * value maybe null.
	 * 
	 * @param key
	 * @return
	 */
    public synchronized Integer getInt(String key) {
        try {
            Blob data = readBlob(key);
            if (data == null) {
                return null;
            } else {
                gi_bin.reset(data.getData());
                return readInt(gi_din);
            }
        } catch (Exception E) {
            logger.error(_base + ":" + E.getMessage());
            logger.fatal(E);
            return null;
        }
    }

    NDbmByteArrayInputStream gl_bin = new NDbmByteArrayInputStream();

    NDbmDataInputStream gl_din = new NDbmDataInputStream(gl_bin);

    /**
	 * Reads a Long from NDbm under given key (end point function). Return value
	 * maybe null.
	 * 
	 * @param key
	 * @return
	 */
    public synchronized Long getLong(String key) {
        try {
            Blob data = readBlob(key);
            if (data == null) {
                return null;
            } else {
                gl_bin.reset(data.getData());
                return readLong(gl_din);
            }
        } catch (Exception E) {
            logger.error(_base + ":" + E.getMessage());
            logger.fatal(E);
            return null;
        }
    }

    NDbmByteArrayInputStream gb_bin = new NDbmByteArrayInputStream();

    NDbmDataInputStream gb_din = new NDbmDataInputStream(gb_bin);

    /**
	 * Reads a a Boolean from NDbm under given key (end point function). Return
	 * value maybe null.
	 * 
	 * @param key
	 * @return
	 */
    public synchronized Boolean getBoolean(String key) {
        try {
            Blob data = readBlob(key);
            if (data == null) {
                return null;
            } else {
                gb_bin.reset(data.getData());
                return readBoolean(gb_din);
            }
        } catch (Exception E) {
            logger.error(_base + ":" + E.getMessage());
            logger.fatal(E);
            return null;
        }
    }

    NDbmByteArrayInputStream gs_bin = new NDbmByteArrayInputStream();

    NDbmDataInputStream gs_din = new NDbmDataInputStream(gs_bin);

    /**
	 * Reads a String from NDbm under given key (end point function). Return
	 * value maybe null.
	 * 
	 * @param key
	 * @return
	 */
    public synchronized String getStr(String key) {
        try {
            Blob data = readBlob(key);
            if (data == null) {
                return null;
            } else {
                gs_bin.reset(data.getData());
                return readString(gs_din);
            }
        } catch (Exception E) {
            logger.error(_base + ":" + E.getMessage());
            logger.fatal(E);
            return null;
        }
    }

    NDbmByteArrayInputStream gd_bin = new NDbmByteArrayInputStream();

    NDbmDataInputStream gd_din = new NDbmDataInputStream(gd_bin);

    /**
	 * Reads a Date from NDbm under given key (end point function). Return value
	 * maybe null.
	 * 
	 * @param key
	 * @return
	 */
    public synchronized Date getDate(String key) {
        try {
            Blob data = readBlob(key);
            if (data == null) {
                return null;
            } else {
                gd_bin.reset(data.getData());
                return readDate(gd_din);
            }
        } catch (Exception E) {
            logger.error(_base + ":" + E.getMessage());
            logger.fatal(E);
            return null;
        }
    }

    NDbmByteArrayInputStream gv_bin = new NDbmByteArrayInputStream();

    NDbmDataInputStream gv_din = new NDbmDataInputStream(gv_bin);

    /**
	 * Reads a Vector of String from NDbm under given key (end point function).
	 * Return value maybe null.
	 * 
	 * @param key
	 * @return
	 */
    public synchronized Vector<String> getVectorOfString(String key) {
        Blob b = readBlob(key);
        if (b == null) {
            return null;
        } else {
            try {
                gv_bin.reset(b.getData());
                return super.readVectorOfString(gv_din);
            } catch (Exception E) {
                logger.error(_base + ":" + E.getMessage());
                logger.fatal(E);
                return null;
            }
        }
    }

    /**
	 * Removes the given key from NDbm.
	 * 
	 * @param key
	 */
    public synchronized void remove(String key) {
        removeBlob(key);
    }

    class Iter implements Iterator<String> {

        private int i;

        private int currentSeek;

        private Bucket b = new Bucket();

        public boolean hasNext() {
            try {
                checkOpen();
                if (currentSeek < 0) {
                    lock();
                    int ts = tableSize();
                    if (i < ts) {
                        b.set(_f_idx, i);
                        while (i < ts && b.hash() < 0) {
                            i += 1;
                            if (i < ts) {
                                checkOpen();
                                b.set(_f_idx, i);
                            }
                        }
                        if (i < ts) {
                            currentSeek = b.seek();
                        }
                        unlock();
                        checkClose();
                        return i < ts;
                    } else {
                        unlock();
                        return false;
                    }
                } else {
                    checkClose();
                    return true;
                }
            } catch (Exception E) {
                logger.fatal(E);
                return false;
            }
        }

        public String next() {
            try {
                checkOpen();
                if (currentSeek < 0) {
                    if (!hasNext()) {
                        logger.fatal("There is no next element");
                    }
                }
                Node node = new Node(_f_dbase, currentSeek);
                currentSeek = node.seekOfNext();
                if (currentSeek < 0) {
                    i += 1;
                }
                checkClose();
                return node.key();
            } catch (Exception E) {
                try {
                    Node n = new Node(_f_dbase, currentSeek, true);
                    logger.error("current node=" + n);
                } catch (Exception E1) {
                    logger.error(E1);
                }
                logger.fatal(E);
                return null;
            }
        }

        public int getNextSeek() {
            try {
                checkOpen();
                if (currentSeek < 0) {
                    if (!hasNext()) {
                        logger.fatal("There is no next element");
                    }
                }
                int sk = currentSeek;
                Node node = new Node(_f_dbase, currentSeek, true);
                currentSeek = node.seekOfNext();
                if (currentSeek < 0) {
                    i += 1;
                }
                checkClose();
                return sk;
            } catch (Exception E) {
                logger.error(E);
                return -1;
            }
        }

        public void remove() {
            logger.error("Remove has not been implemented");
        }

        public Iter() {
            currentSeek = -1;
            i = 0;
        }
    }

    /**
	 * Beware, this iterator is not safe over writes or removes. Also not from
	 * others!
	 * 
	 * @return
	 */
    public Iterator<String> iterator() {
        return new Iter();
    }

    public Vector<String> keys() {
        Iterator<String> it = this.iterator();
        Vector<String> keys = new Vector<String>();
        while (it.hasNext()) {
            String key = it.next();
            keys.add(key);
        }
        return keys;
    }

    private void checkOpen() {
        if (_closed) {
            init();
        }
    }

    private void checkClose() {
        if (_closedAtExit) {
            close();
        }
    }

    private void incCollisions() throws Exception {
        _f_meta.seek(S_COLLISIONS);
        int collisions = _f_meta.readInt();
        collisions += 1;
        _f_meta.seek(S_COLLISIONS);
        _f_meta.writeInt(collisions);
    }

    private void decCollisions() throws Exception {
        _f_meta.seek(S_COLLISIONS);
        int collisions = _f_meta.readInt();
        collisions -= 1;
        if (collisions < 0) {
            logger.error("Unexpected: Number of collisions drops below 0");
        }
        _f_meta.seek(S_COLLISIONS);
        _f_meta.writeInt(collisions);
    }

    private void lock() {
        if (!_readonly) {
            try {
                while (_lock == null) {
                    try {
                        FileLock l = _c_meta.tryLock();
                        _lock = l;
                    } catch (OverlappingFileLockException e) {
                        logger.info("locking: waiting for lock release:" + Thread.currentThread().getId());
                        Thread.sleep(1000);
                    }
                }
            } catch (Exception e) {
                logger.fatal(e);
            }
        }
    }

    private void unlock() {
        if (!_readonly) {
            if (_lock != null) {
                try {
                    _lock.release();
                    _lock = null;
                } catch (Exception e) {
                    logger.fatal(e);
                }
            }
        }
    }

    private int tableSize() {
        try {
            _f_meta.seek(S_TABLESIZE);
            return _f_meta.readInt();
        } catch (Exception E) {
            checkOpen();
            int s;
            try {
                _f_meta.seek(S_TABLESIZE);
                s = _f_meta.readInt();
            } catch (Exception E1) {
                logger.fatal(E1);
                s = -1;
            }
            checkClose();
            return s;
        }
    }

    private void checkIter(String key) {
    }

    private Bucket wb_bk = new Bucket();

    private synchronized void writeBlob(Blob b) {
        checkOpen();
        removeBlob(b.key());
        try {
            lock();
            int seek = createNode(b);
            int hash = MurmurHash.hash(b.key(), tableSize());
            wb_bk.set(_f_idx, hash);
            if (wb_bk.seek() < 0) {
                wb_bk.set(hash, seek);
                wb_bk.write(_f_idx);
            } else {
                incCollisions();
                Gap currentNode = new Gap(_f_dbase, wb_bk.seek());
                Gap newNode = new Gap(_f_dbase, seek);
                newNode.seekOfNext(currentNode.seek());
                newNode.seekOfPrev(-1);
                currentNode.seekOfPrev(newNode.seek());
                newNode.write(_f_dbase);
                currentNode.write(_f_dbase);
                wb_bk.set(hash, seek);
                wb_bk.write(_f_idx);
            }
            unlock();
        } catch (Exception E) {
            logger.fatal(E);
        }
        checkClose();
    }

    private Bucket rb_bk = new Bucket();

    private synchronized Blob readBlob(String key) {
        checkOpen();
        try {
            lock();
            int hash = MurmurHash.hash(key, tableSize());
            rb_bk.set(_f_idx, hash);
            if (rb_bk.seek() >= 0) {
                Node n = readNode(rb_bk.seek());
                while (!n.key().equals(key)) {
                    int seek = n.seekOfNext();
                    if (seek >= 0) {
                        n = readNode(n.seekOfNext());
                    } else {
                        return null;
                    }
                }
                unlock();
                checkClose();
                return n.getData(_f_dbase);
            } else {
                unlock();
                checkClose();
                return null;
            }
        } catch (Exception E) {
            logger.fatal(E);
            return null;
        }
    }

    private Bucket db_bk = new Bucket();

    private synchronized void removeBlob(String key) {
        checkOpen();
        lock();
        try {
            int hash = MurmurHash.hash(key, tableSize());
            db_bk.set(_f_idx, hash);
            if (db_bk.seek() >= 0) {
                Node node = null;
                Node n = readNode(db_bk.seek());
                int seek = db_bk.seek();
                while (seek >= 0 && !n.key().equals(key)) {
                    seek = n.seekOfNext();
                    if (seek >= 0) {
                        n = readNode(n.seekOfNext());
                    }
                }
                if (seek >= 0) {
                    node = n;
                    if (node.seekOfPrev() < 0 && node.seekOfNext() < 0) {
                        db_bk.reset(_f_idx);
                    } else {
                        decCollisions();
                        if (node.seekOfPrev() < 0) {
                            if (node.seekOfNext() >= 0) {
                                Gap next = new Gap(_f_dbase, node.seekOfNext());
                                next.seekOfPrev(-1);
                                next.write(_f_dbase);
                            }
                            db_bk.seek(node.seekOfNext());
                            db_bk.write(_f_idx);
                        } else {
                            Gap prev = new Gap(_f_dbase, node.seekOfPrev());
                            prev.seekOfNext(node.seekOfNext());
                            prev.write(_f_dbase);
                            if (node.seekOfNext() >= 0) {
                                Gap next = new Gap(_f_dbase, node.seekOfNext());
                                next.seekOfPrev(node.seekOfPrev());
                                next.write(_f_dbase);
                            }
                        }
                    }
                    moveNodeToGapList(node);
                }
            } else {
            }
        } catch (Exception E) {
            logger.fatal(E);
        }
        unlock();
        checkClose();
    }

    private class Gap {

        int _seek;

        int _seek_of_next_empty_node;

        int _seek_of_prev_empty_node;

        int _length;

        public String toString() {
            return String.format("[%d,%d,%d,%d]", _seek, _seek_of_prev_empty_node, _seek_of_next_empty_node, _length);
        }

        public int seek() {
            return _seek;
        }

        public int seekOfNext() {
            return _seek_of_next_empty_node;
        }

        public int seekOfPrev() {
            return _seek_of_prev_empty_node;
        }

        public int length() {
            return _length;
        }

        public void seekOfNext(int s) {
            _seek_of_next_empty_node = s;
        }

        public void seekOfPrev(int s) {
            _seek_of_prev_empty_node = s;
        }

        public void length(int l) {
            _length = l;
        }

        public void seek(int s) {
            _seek = s;
        }

        public void write(NDbmRandomAccessFile f) throws Exception {
            f.seek(_seek);
            f.writeInt(_seek);
            f.writeInt(_seek_of_prev_empty_node);
            f.writeInt(_seek_of_next_empty_node);
            f.writeInt(_length);
        }

        public Gap(NDbmRandomAccessFile f, int seek) throws Exception {
            f.seek(seek);
            _seek = f.readInt();
            _seek_of_prev_empty_node = f.readInt();
            _seek_of_next_empty_node = f.readInt();
            _length = f.readInt();
        }

        public Gap(NDbmRandomAccessFile f, int seek, int next, int prev, int length) throws Exception {
            f.seek(seek);
            _seek = seek;
            _seek_of_next_empty_node = next;
            _seek_of_prev_empty_node = prev;
            _length = length;
            f.writeInt(_seek);
            f.writeInt(_seek_of_prev_empty_node);
            f.writeInt(_seek_of_next_empty_node);
            f.writeInt(_length);
            f.seek(seek + length - 1);
            f.writeByte((byte) 'A');
        }
    }

    private class Node extends Gap {

        int _length_of_key;

        int _length_of_blob;

        int _blobseek = -1;

        byte[] _data = null;

        String _key = null;

        public String toString() {
            return String.format("[%s]-%d-%d(%d)-%s", super.toString(), +_length_of_key, _length_of_blob, _blobseek, _key);
        }

        public void writeGap(NDbmRandomAccessFile f) throws Exception {
            super.write(f);
        }

        public Blob getData(NDbmRandomAccessFile f) throws Exception {
            if (_data == null) {
                if (f.getFilePointer() != _blobseek) {
                    f.seek(_blobseek);
                }
                _length_of_blob = f.readInt();
                _data = new byte[_length_of_blob];
                f.read(_data);
            }
            return new Blob(_key, _data, _length_of_blob);
        }

        public String key() {
            return _key;
        }

        private void initRd(NDbmRandomAccessFile f, int seek, boolean noThrow) throws Exception {
            f.seek(seek + 4 * NDbmDataInputStream.sizeOfInt());
            _length_of_key = f.readInt();
            if (_length_of_key > 100000) {
                if (noThrow) {
                    _blobseek = -1;
                    _key = "";
                } else {
                    throw new Exception("KEY LENGTH ERROR");
                }
            } else {
                byte[] k = new byte[_length_of_key];
                f.read(k);
                _key = new String(k, "UTF-8");
                _blobseek = (int) f.getFilePointer();
            }
        }

        public Node(NDbmRandomAccessFile f, int seek, boolean noThrow) throws Exception {
            super(f, seek);
            initRd(f, seek, noThrow);
        }

        public Node(NDbmRandomAccessFile f, int seek) throws Exception {
            super(f, seek);
            initRd(f, seek, false);
        }

        public Node(NDbmRandomAccessFile f, int seek, int length, Blob blob) throws Exception {
            super(f, seek, -1, -1, length);
            _data = blob.getData();
            _length_of_blob = blob.getDataSize();
            _key = blob.key();
            byte[] k;
            try {
                k = _key.getBytes("UTF-8");
            } catch (Exception E) {
                logger.fatal(E);
                k = new byte[] { 0 };
            }
            f.seek(seek + 4 * NDbmDataInputStream.sizeOfInt());
            f.writeInt(k.length);
            f.write(k);
            f.writeInt(_length_of_blob);
            f.write(_data, 0, _length_of_blob);
        }
    }

    private int baseSize() {
        return 6 * NDbmDataInputStream.sizeOfInt();
    }

    private int calcLength(Blob b) {
        try {
            int l_key = b.key().getBytes("UTF-8").length;
            int l_blob = b.getDataSize();
            return baseSize() + l_key + l_blob;
        } catch (Exception E) {
            logger.fatal(E);
            return -1;
        }
    }

    private int createNode(Blob blob) {
        try {
            _f_meta.seek(S_NUMOFGAPS);
            int numOfGaps = _f_meta.readInt();
            int collisions = _f_meta.readInt();
            checkForOptimize(numOfGaps, collisions);
            _f_meta.seek(S_FIRSTGAP);
            int first_gap = _f_meta.readInt();
            int node_size = calcLength(blob);
            Node newNode = null;
            if (first_gap >= 0) {
                Gap g = new Gap(_f_dbase, first_gap);
                boolean found = false;
                while (g != null && !found) {
                    if (g.length() >= node_size && g.length() <= node_size + (2 * baseSize())) {
                        found = true;
                        if (g.seekOfPrev() >= 0) {
                            Gap prev = new Gap(_f_dbase, g.seekOfPrev());
                            prev.seekOfNext(g.seekOfNext());
                            if (g.seekOfNext() >= 0) {
                                Gap next = new Gap(_f_dbase, g.seekOfNext());
                                next.seekOfPrev(g.seekOfPrev());
                                next.write(_f_dbase);
                            }
                            prev.write(_f_dbase);
                            _f_meta.seek(S_NUMOFGAPS);
                            _f_meta.writeInt(numOfGaps - 1);
                        } else {
                            if (g.seekOfNext() >= 0) {
                                Gap next = new Gap(_f_dbase, g.seekOfNext());
                                next.seekOfPrev(-1);
                                next.write(_f_dbase);
                                _f_meta.seek(S_FIRSTGAP);
                                _f_meta.writeInt(next.seek());
                                _f_meta.writeInt(numOfGaps - 1);
                            } else {
                                _f_meta.seek(S_FIRSTGAP);
                                _f_meta.writeInt(-1);
                                _f_meta.writeInt(0);
                            }
                        }
                        newNode = new Node(_f_dbase, g.seek(), node_size, blob);
                    } else if (g.length() > node_size) {
                        found = true;
                        newNode = new Node(_f_dbase, g.seek(), node_size, blob);
                        g.seek(g.seek() + node_size);
                        g.length(g.length() - node_size);
                        g.write(_f_dbase);
                        if (g.seekOfPrev() >= 0) {
                            Gap prev = new Gap(_f_dbase, g.seekOfPrev());
                            prev.seekOfNext(g.seek());
                            prev.write(_f_dbase);
                        } else {
                            _f_meta.seek(S_FIRSTGAP);
                            _f_meta.writeInt(g.seek());
                        }
                        if (g.seekOfNext() >= 0) {
                            Gap next = new Gap(_f_dbase, g.seekOfNext());
                            next.seekOfPrev(g.seek());
                            next.write(_f_dbase);
                        }
                    } else {
                        if (g.seekOfNext() >= 0) {
                            g = new Gap(_f_dbase, g.seekOfNext());
                        } else {
                            g = null;
                        }
                    }
                }
            }
            if (newNode == null) {
                newNode = new Node(_f_dbase, (int) _f_dbase.length(), node_size, blob);
            }
            return newNode.seek();
        } catch (Exception E) {
            logger.fatal(E);
            return -1;
        }
    }

    private Node readNode(int seek) {
        return readNode(seek, false);
    }

    private Node readNode(int seek, boolean noThrow) {
        try {
            return new Node(_f_dbase, seek, noThrow);
        } catch (Exception E) {
            logger.fatal(E);
            return null;
        }
    }

    private void moveNodeToGapList(Node n) {
        try {
            _f_meta.seek(S_FIRSTGAP);
            int seek = _f_meta.readInt();
            int numOfGaps = _f_meta.readInt();
            @SuppressWarnings("unused") int collisions = _f_meta.readInt();
            if (seek >= 0) {
                Gap first = new Gap(_f_dbase, seek);
                n.seekOfNext(seek);
                first.seekOfPrev(n.seek());
                n.seekOfPrev(-1);
                first.write(_f_dbase);
                n.writeGap(_f_dbase);
                _f_meta.seek(S_FIRSTGAP);
                _f_meta.writeInt(n.seek());
                numOfGaps += 1;
                _f_meta.writeInt(numOfGaps);
            } else {
                n.seekOfPrev(-1);
                n.seekOfNext(-1);
                n.writeGap(_f_dbase);
                _f_meta.seek(S_FIRSTGAP);
                _f_meta.writeInt(n.seek());
                _f_meta.writeInt(1);
            }
        } catch (Exception E) {
            logger.fatal(E);
        }
    }

    private void checkForOptimize(int numOfGaps, int collisions) {
        if (!_optimizing) {
            int ts = tableSize();
            int maxGaps;
            if (ts < 2000) {
                maxGaps = ((int) (ts * 0.5)) + 1;
            } else if (ts < 10000) {
                maxGaps = ((int) (ts * 0.25)) + 1;
            } else {
                maxGaps = ((int) (ts * 0.1)) + 1;
            }
            int maxCollisions = ts / 3 + 1;
            if (numOfGaps >= maxGaps || collisions >= maxCollisions) {
                logger.info("Optimizing database:" + _dbase.getName());
                optimizeDatabase((collisions > maxCollisions) ? ts * 2 : ts + collisions);
                logger.info("Done optimizing database:" + _dbase.getName());
            }
        }
    }

    private void optimizeDatabase(int ts) {
        File newBase = new File(_base.getAbsolutePath() + "_opt");
        NDbm newDb = new NDbm(newBase, ts, _readonly, "rw");
        newDb._optimizing = true;
        Iterator<String> it = this.iterator();
        while (it.hasNext()) {
            String key = it.next();
            Blob b = readBlob(key);
            newDb.writeBlob(b);
        }
        newDb.close();
        copyFrom(newBase);
    }

    private void copyFrom(File frombase) {
        File fromMeta = new File(frombase.getAbsolutePath() + ".mta");
        File fromIndex = new File(frombase.getAbsolutePath() + ".idx");
        File fromDbase = new File(frombase.getAbsolutePath() + ".dbm");
        try {
            _f_idx.close();
            _f_dbase.close();
            copyFile(fromIndex, _index);
            copyFile(fromDbase, _dbase);
            _f_idx = new NDbmRandomAccessFile(_index, RW_ATTR);
            _f_dbase = new NDbmRandomAccessFile(_dbase, RW_ATTR);
        } catch (Exception E) {
            logger.fatal(E);
        }
        try {
            NDbmRandomAccessFile f = new NDbmRandomAccessFile(fromMeta, R_ATTR);
            _f_meta.seek(0);
            _f_meta.writeInt(f.readInt());
            int ts = f.readInt();
            _f_meta.writeInt(ts);
            _f_meta.writeInt(f.readInt());
            _f_meta.writeInt(f.readInt());
            _f_meta.writeInt(f.readInt());
            f.close();
        } catch (Exception E) {
            logger.fatal(E);
        }
        fromIndex.delete();
        fromDbase.delete();
        fromMeta.delete();
    }

    private void copyFile(File from, File to) {
        try {
            InputStream in = new FileInputStream(from);
            OutputStream out = new FileOutputStream(to);
            byte[] buf = new byte[10240];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            from.delete();
        } catch (Exception E) {
            logger.error(_base + ":" + E.getMessage());
            logger.fatal(E);
        }
    }
}
