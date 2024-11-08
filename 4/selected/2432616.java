package org.jugile.daims;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import org.apache.log4j.Logger;
import org.jugile.util.Buffer;
import org.jugile.util.DBConnection;
import org.jugile.util.Jugile;

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
public class UnitOfWork extends Jugile {

    static Logger log = Logger.getLogger(UnitOfWork.class);

    private boolean readOnly = false;

    private final DomainData origin;

    protected UnitOfWork(DomainData dd, Lock rl, Lock wl) {
        origin = dd;
        readLock = rl;
        writeLock = wl;
        log.debug("UOW CREATED");
    }

    private Map<String, Object> store = new HashMap<String, Object>();

    public Object get(String key) {
        return store.get(key);
    }

    public void put(String key, Object v) {
        store.put(key, v);
    }

    private boolean hasReadTx = false;

    private boolean hasWriteTx = false;

    private final Lock readLock;

    private final Lock writeLock;

    protected void release() {
        if (hasReadTx) {
            log.debug("READ RELEASED");
            readLock.unlock();
            hasReadTx = false;
            log.debug("READ RELEASED - OK");
        }
        if (hasWriteTx) {
            log.debug("WRITE RELEASED");
            writeLock.unlock();
            hasWriteTx = false;
            log.debug("WRITE RELEASED - OK");
        }
        log.debug("UOW RELEASED");
    }

    protected void startReadTx() {
        log.debug("READ START");
        readLock.lock();
        hasReadTx = true;
        log.debug("READ START - OK");
    }

    protected void endReadTx() {
        log.debug("READ END");
        readLock.unlock();
        hasReadTx = false;
        log.debug("READ END - OK");
    }

    protected void startWriteTx() {
        log.debug("WRITE START");
        writeLock.lock();
        hasWriteTx = true;
        log.debug("WRITE START - OK");
    }

    protected void endWriteTx() {
        log.debug("WRITE END");
        writeLock.unlock();
        hasWriteTx = false;
        log.debug("WRITE END - OK");
    }

    protected void assertReadTx() {
        if (hasReadTx) return;
        if (hasWriteTx) fail("assertReadTx: had writeTx");
        startReadTx();
    }

    protected void assertAtLeastReadTx() {
        if (hasReadTx) return;
        if (hasWriteTx) return;
        startReadTx();
    }

    protected String getTxInfo() {
        return "  ===== UOW TX ====== readTx: " + hasReadTx + " writeTx: " + hasWriteTx;
    }

    private Boolean isReadOnly = null;

    protected boolean isReadOnly() {
        if (isReadOnly == null) {
            isReadOnly = DomainCore.isReadOnly();
        }
        return isReadOnly;
    }

    protected void setReadOnly(boolean v) {
        isReadOnly = v;
    }

    private Map<Class<Bo>, BoMapDelta<Bo>> maps = new HashMap<Class<Bo>, BoMapDelta<Bo>>();

    private BoMapDelta<Bo> map(Class<Bo> cl) {
        BoMapDelta<Bo> md = maps.get(cl);
        if (md == null) {
            md = origin.getBoCollection(cl);
            maps.put(cl, md);
        }
        return md;
    }

    protected Bo get(Class cl, long id) {
        return map(cl).get(id);
    }

    protected Bo createNewBo(Class cl) {
        return map(cl).createNewBo();
    }

    protected BoMapDelta getAll(Class cl) {
        return map(cl);
    }

    public void archive(Bo o) {
        delete(o);
    }

    public void delete(Bo o) {
        if (o != null) {
            o = this.get(o.getClass(), o.id());
            map((Class<Bo>) o.getClass()).remove(o);
        }
    }

    protected int getCommitSize() {
        int cs = 0;
        for (Class<Bo> cl : maps.keySet()) {
            cs += map(cl).getCommitSize();
        }
        return cs;
    }

    protected String getDelta(Class<Bo>[] cls) {
        StringWriter out = new StringWriter();
        try {
            delta(out, cls);
        } catch (Exception e) {
            fail(e);
        }
        return out.toString();
    }

    protected void delta(Writer out, Class<Bo>[] cls) throws Exception {
        for (Class<Bo> cl : cls) {
            log.debug("delta: " + cl);
            map(cl).delta(out);
            map(cl).nndelta(out);
        }
    }

    protected int writeToDB(DBConnection c, Class<Bo>[] cls) throws Exception {
        int count = 0;
        for (Class<Bo> cl : cls) {
            count += map(cl).writeToDB(c);
        }
        return count;
    }

    protected void dump(Buffer buf) throws Exception {
        buf.ln();
        buf.ln("UnitOfWork:");
        buf.ln("===========");
        buf.ln("deleted:");
        for (BoMapDelta m : maps.values()) m.dumpDeleted(buf);
        buf.ln("items:");
        for (BoMapDelta m : maps.values()) m.dumpItems(buf);
    }
}
