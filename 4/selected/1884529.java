package org.jugile.daims;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.log4j.Logger;
import org.jugile.util.Buffer;
import org.jugile.util.CsvTokenizer;
import org.jugile.util.DBConnection;
import org.jugile.util.DBPool;
import org.jugile.util.DBQueue;
import org.jugile.util.DOH;
import org.jugile.util.HiLo;
import org.jugile.util.Jugile;
import org.jugile.util.Props;
import org.jugile.util.Timer;

/**
 * <i>"this is verse"</i> <b>()</b>
 * 
 * <br/>==========<br/>
 *
 * here is doc
 *  
 * @author jukka.rahkonen@iki.fi
 *
 */
public abstract class DomainCore extends Jugile {

    static Logger log = Logger.getLogger(DomainCore.class);

    protected static DomainCore domain;

    private static volatile DomainData coreData;

    protected static DomainData cd() {
        if (domain == null) fail("tried to call cd() on empty domain");
        if (coreData == null) coreData = new DomainData(domain);
        return coreData;
    }

    public static void reset() {
        if (domain == null) return;
        coreData = null;
        uow.remove();
    }

    private static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private static final Lock readLock = readWriteLock.readLock();

    private static final Lock writeLock = readWriteLock.writeLock();

    private static ThreadLocal<UnitOfWork> uow = new ThreadLocal<UnitOfWork>() {

        @Override
        protected UnitOfWork initialValue() {
            log.debug("create UnitOfWork");
            UnitOfWork uow = new UnitOfWork(cd(), readLock, writeLock);
            return uow;
        }

        public void remove() {
            get().release();
            super.remove();
        }
    };

    protected static UnitOfWork getUnitOfWork() {
        return uow();
    }

    private static UnitOfWork uow() {
        UnitOfWork u = uow.get();
        u.assertAtLeastReadTx();
        return u;
    }

    private static void uowRemove() {
        uow.remove();
    }

    public Object get(String key) {
        return uow().get(key);
    }

    public void put(String key, Object value) {
        uow().put(key, value);
    }

    protected Bo get(Class cl, long id) {
        return uow().get(cl, id);
    }

    protected Bo createNew(Class cl) {
        return uow().createNewBo(cl);
    }

    protected BoMapDelta getAll(Class cl) {
        BoCollection bc = (BoCollection) uow().getAll(cl);
        bc.reset();
        return bc;
    }

    public void commit() {
        if (uow().getCommitSize() == 0) {
            log.debug("commit size == 0");
            uowRemove();
            return;
        }
        if (!Props.isAdminNode()) {
            log.debug("not adminNode - commit aborted");
            uowRemove();
            return;
        }
        String delta = uow().getDelta(classes());
        log.debug("delta:\n" + delta);
        if (hasdb()) modifyDbAndSendMsg(delta);
        uow.get().endReadTx();
        uow.get().startWriteTx();
        try {
            modifyDomain(delta);
        } catch (Exception e) {
            log.fatal("could not modify domain", e);
        } finally {
            uowRemove();
        }
    }

    /**
	 * Commit only to memory.
	 */
    public void commitMem() {
        if (uow().getCommitSize() == 0) {
            log.debug("commit size == 0");
            uowRemove();
            return;
        }
        String delta = uow().getDelta(classes());
        log.debug("delta:\n" + delta);
        uow.get().endReadTx();
        uow.get().startWriteTx();
        try {
            modifyDomain(delta);
        } catch (Exception e) {
            log.fatal("could not modify domain", e);
        } finally {
            uowRemove();
        }
    }

    public int getCommitSize() {
        return uow().getCommitSize();
    }

    public void rollback() {
        uowRemove();
    }

    public void startTx() {
    }

    public void close() {
    }

    public void setReadTx() {
        uow().setReadOnly(true);
    }

    public void setWriteTx() {
        uow().setReadOnly(false);
    }

    protected int modifyDomain(String delta) {
        CsvTokenizer t = CsvTokenizer.parseString(delta, Bo.CSVDELIMITER);
        try {
            return cd().modifyDomain(t);
        } catch (Exception e) {
            log.fatal("could not modify domain", e);
            fail(e);
            return 0;
        } finally {
            t.close();
        }
    }

    public void saveToCsvZip(String fname) {
        saveToCsv(zipWriter(fname));
    }

    public void saveToCsv(String fname) {
        saveToCsv(writer(fname));
    }

    public void saveToCsv(Writer out) {
        try {
            uow().delta(out, classes());
        } catch (Exception e) {
            log.fatal("could not write domain to csv file", e);
            fail(e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
            uowRemove();
        }
    }

    public int loadFromCsvZip(String fname) {
        return loadFromCsv(zipInputStream(fname));
    }

    public int loadFromCsv(String fname) {
        return loadFromCsv(inputStream(fname));
    }

    public int loadFromCsv(InputStream is) {
        CsvTokenizer t = new CsvTokenizer(is);
        t.setDelimiter(Bo.CSVDELIMITER);
        uow.get().startWriteTx();
        try {
            int count = cd().modifyDomain(t);
            HiLo.setNextid(count + 100L);
            log.info("loaded " + count + " items. updated HiLo, nextid now: " + HiLo.nextid());
            log.info("stats: " + stats());
            return count;
        } catch (Exception e) {
            log.fatal("could not modify domain", e);
            fail(e);
            return 0;
        } finally {
            uowRemove();
        }
    }

    protected int saveAllToDB() {
        uow();
        try {
            DBPool db = DBPool.getPool();
            DBConnection c = db.getConnection();
            try {
                int res = cd().saveToDB(c);
                c.commit();
                return res;
            } catch (Exception e) {
                try {
                    c.rollback();
                    fail(e);
                } catch (Exception e2) {
                    fail(e2);
                }
            } finally {
                try {
                    c.free();
                } catch (Exception e2) {
                    fail(e2);
                }
            }
        } catch (Exception e) {
            log.fatal("could not write domain to db", e);
            fail(e);
        } finally {
            uowRemove();
        }
        return 0;
    }

    protected int loadFromDB() {
        uow.get().startWriteTx();
        try {
            log.debug("loadFromDB");
            int res = doLoadFromDB();
            readDeltasFromQueue(1000);
            log.debug("loadFromDB done");
            log.info("stats: " + stats());
            return res;
        } catch (Exception e) {
            log.fatal("could not load domain from db", e);
            fail(e);
            return 0;
        } finally {
            uowRemove();
        }
    }

    protected abstract Class<Bo>[] classes();

    private int doLoadFromDB() throws Exception {
        DBPool db = DBPool.getPool();
        DBConnection c = db.getConnection();
        int count = 0;
        try {
            count = cd().loadFromDB(c, classes());
        } catch (Exception e) {
            log.error("dbread failed", e);
            try {
                c.rollback();
            } catch (Exception e2) {
                fail(e2);
            }
        } finally {
            try {
                c.free();
            } catch (Exception e) {
            }
        }
        return count;
    }

    public int update() {
        uow.get().startWriteTx();
        try {
            return cd().readDeltasFromQueue(node(), 1000);
        } catch (Exception e) {
            log.fatal("could not load deltas from queue", e);
            fail(e);
            return 0;
        } finally {
            uowRemove();
        }
    }

    protected int readDeltasFromQueue(int max) throws Exception {
        return cd().readDeltasFromQueue(node(), max);
    }

    private int modifyDbAndSendMsg(String delta) {
        DBPool db = DBPool.getPool();
        DBConnection c = db.getConnection();
        try {
            int res = uow().writeToDB(c, classes());
            DBQueue.writeMessage(delta, node(), nodes(), c);
            c.commit();
            return res;
        } catch (Exception e) {
            try {
                c.rollback();
            } catch (Exception e2) {
                fail(e2);
            }
            log.error("could not write db changes", e);
            fail(e);
        } finally {
            try {
                c.free();
            } catch (Exception e2) {
                fail(e2);
            }
        }
        return 0;
    }

    private static String node = null;

    protected final String node() {
        if (node == null) {
            node = Props.get("jugile.node");
            if (empty(node)) {
                node = getHostname();
            }
        }
        return node;
    }

    public static void setNode(String v) {
        node = v;
    }

    private String nodes[] = null;

    protected final String[] nodes() {
        if (nodes == null) {
            String nodesstr = Props.get("jugile.nodes");
            if (empty(nodesstr)) return null;
            nodes = nodesstr.split(",");
        }
        return nodes;
    }

    public void dump(String header) {
        Buffer buf = new Buffer("    ");
        buf.ln("");
        buf.ln("");
        buf.ln(mult("=", 77));
        buf.ln("   DAIMS OO memory dump: " + now().getUiTs());
        buf.ln("     - " + header);
        buf.ln(mult("=", 77));
        buf.ln("");
        try {
            cd().dump(buf);
            uow().dump(buf);
        } catch (Exception e) {
            fail(e);
        }
        print(buf.toString());
    }

    public String stats() {
        Stats st = new Stats(classes());
        Timer t = new Timer();
        cd().stats(st);
        return st.toString() + "\n stats took ms: " + t.stop();
    }

    private static String hostname = null;

    protected static String getHostname() {
        if (hostname != null) return hostname;
        hostname = resolveHostname();
        return hostname;
    }

    private static synchronized String resolveHostname() {
        return Jugile.getHostName();
    }

    private static String hostnum;

    public static String getHostNum() {
        if (hostnum != null) return hostnum;
        hostnum = "S" + nn(getHostname()).replaceAll("[^0-9]+", "");
        return hostnum;
    }

    protected static final boolean hasdb() {
        return HiLo.hasdb();
    }

    public boolean _hasDb() {
        return this.hasdb();
    }

    public Bo get(DOH doh) {
        if (doh == null) return null;
        String clname = doh.getDomainClassName();
        String sid = doh.getId();
        long id = parseLongSafe(sid);
        Class<Bo> cl = getClassByName(clname);
        return uow().getAll(cl).get(id);
    }

    private static volatile Boolean isReadOnly = null;

    public static boolean isReadOnly() {
        if (isReadOnly != null) return isReadOnly;
        isReadOnly = !parseBoolSafe(Props.get("jugile.isAdminNode"));
        return isReadOnly;
    }
}
