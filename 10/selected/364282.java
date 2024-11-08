package org.hibernate.test.jpa.lock;

import java.math.BigDecimal;
import junit.framework.Test;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.jpa.AbstractJPATest;
import org.hibernate.test.jpa.Item;
import org.hibernate.test.jpa.Part;

/**
 * Test that the Hibernate Session complies with REPEATABLE_READ isolation
 * semantics.
 *
 * @author Steve Ebersole
 */
public class RepeatableReadTest extends AbstractJPATest {

    public RepeatableReadTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new FunctionalTestClassTestSuite(RepeatableReadTest.class);
    }

    public void testStaleVersionedInstanceFoundInQueryResult() {
        if (getDialect().doesReadCommittedCauseWritersToBlockReaders()) {
            reportSkip("lock blocking", "stale versioned instance");
            return;
        }
        String check = "EJB3 Specification";
        Session s1 = getSessions().openSession();
        Transaction t1 = s1.beginTransaction();
        Item item = new Item(check);
        s1.save(item);
        t1.commit();
        s1.close();
        Long itemId = item.getId();
        long initialVersion = item.getVersion();
        s1 = getSessions().openSession();
        t1 = s1.beginTransaction();
        item = (Item) s1.get(Item.class, itemId);
        Session s2 = getSessions().openSession();
        Transaction t2 = s2.beginTransaction();
        Item item2 = (Item) s2.get(Item.class, itemId);
        item2.setName("EJB3 Persistence Spec");
        t2.commit();
        s2.close();
        item2 = (Item) s1.createQuery("select i from Item i").list().get(0);
        assertTrue(item == item2);
        assertEquals("encountered non-repeatable read", check, item2.getName());
        assertEquals("encountered non-repeatable read", initialVersion, item2.getVersion());
        t1.commit();
        s1.close();
        s1 = getSessions().openSession();
        t1 = s1.beginTransaction();
        s1.createQuery("delete Item").executeUpdate();
        t1.commit();
        s1.close();
    }

    public void testStaleVersionedInstanceFoundOnLock() {
        if (!readCommittedIsolationMaintained("repeatable read tests")) {
            return;
        }
        if (getDialect().doesReadCommittedCauseWritersToBlockReaders()) {
            reportSkip("lock blocking", "stale versioned instance");
            return;
        }
        String check = "EJB3 Specification";
        Session s1 = getSessions().openSession();
        Transaction t1 = s1.beginTransaction();
        Item item = new Item(check);
        s1.save(item);
        t1.commit();
        s1.close();
        Long itemId = item.getId();
        long initialVersion = item.getVersion();
        s1 = getSessions().openSession();
        t1 = s1.beginTransaction();
        item = (Item) s1.get(Item.class, itemId);
        Session s2 = getSessions().openSession();
        Transaction t2 = s2.beginTransaction();
        Item item2 = (Item) s2.get(Item.class, itemId);
        item2.setName("EJB3 Persistence Spec");
        t2.commit();
        s2.close();
        s1.lock(item, LockMode.READ);
        item2 = (Item) s1.get(Item.class, itemId);
        assertTrue(item == item2);
        assertEquals("encountered non-repeatable read", check, item2.getName());
        assertEquals("encountered non-repeatable read", initialVersion, item2.getVersion());
        try {
            s1.lock(item, LockMode.UPGRADE);
            fail("expected UPGRADE lock failure");
        } catch (StaleObjectStateException expected) {
        } catch (SQLGrammarException t) {
            if (getDialect() instanceof SQLServerDialect) {
                t1.rollback();
                t1 = s1.beginTransaction();
            } else {
                throw t;
            }
        }
        t1.commit();
        s1.close();
        s1 = getSessions().openSession();
        t1 = s1.beginTransaction();
        s1.createQuery("delete Item").executeUpdate();
        t1.commit();
        s1.close();
    }

    public void testStaleNonVersionedInstanceFoundInQueryResult() {
        if (getDialect().doesReadCommittedCauseWritersToBlockReaders()) {
            reportSkip("lock blocking", "stale versioned instance");
            return;
        }
        String check = "Lock Modes";
        Session s1 = getSessions().openSession();
        Transaction t1 = s1.beginTransaction();
        Part part = new Part(new Item("EJB3 Specification"), check, "3.3.5.3", new BigDecimal(0.0));
        s1.save(part);
        t1.commit();
        s1.close();
        Long partId = part.getId();
        s1 = getSessions().openSession();
        t1 = s1.beginTransaction();
        part = (Part) s1.get(Part.class, partId);
        Session s2 = getSessions().openSession();
        Transaction t2 = s2.beginTransaction();
        Part part2 = (Part) s2.get(Part.class, partId);
        part2.setName("Lock Mode Types");
        t2.commit();
        s2.close();
        part2 = (Part) s1.createQuery("select p from Part p").list().get(0);
        assertTrue(part == part2);
        assertEquals("encountered non-repeatable read", check, part2.getName());
        t1.commit();
        s1.close();
        s1 = getSessions().openSession();
        t1 = s1.beginTransaction();
        s1.delete(part2);
        s1.delete(part2.getItem());
        t1.commit();
        s1.close();
    }

    public void testStaleNonVersionedInstanceFoundOnLock() {
        if (!readCommittedIsolationMaintained("repeatable read tests")) {
            return;
        }
        if (getDialect().doesReadCommittedCauseWritersToBlockReaders()) {
            reportSkip("lock blocking", "stale versioned instance");
            return;
        }
        String check = "Lock Modes";
        Session s1 = getSessions().openSession();
        Transaction t1 = s1.beginTransaction();
        Part part = new Part(new Item("EJB3 Specification"), check, "3.3.5.3", new BigDecimal(0.0));
        s1.save(part);
        t1.commit();
        s1.close();
        Long partId = part.getId();
        s1 = getSessions().openSession();
        t1 = s1.beginTransaction();
        part = (Part) s1.get(Part.class, partId);
        Session s2 = getSessions().openSession();
        Transaction t2 = s2.beginTransaction();
        Part part2 = (Part) s2.get(Part.class, partId);
        part2.setName("Lock Mode Types");
        t2.commit();
        s2.close();
        s1.lock(part, LockMode.READ);
        part2 = (Part) s1.get(Part.class, partId);
        assertTrue(part == part2);
        assertEquals("encountered non-repeatable read", check, part2.getName());
        try {
            s1.lock(part, LockMode.UPGRADE);
        } catch (Throwable t) {
            t1.rollback();
            t1 = s1.beginTransaction();
        }
        part2 = (Part) s1.get(Part.class, partId);
        assertTrue(part == part2);
        assertEquals("encountered non-repeatable read", check, part2.getName());
        t1.commit();
        s1.close();
        s1 = getSessions().openSession();
        t1 = s1.beginTransaction();
        s1.delete(part);
        s1.delete(part.getItem());
        t1.commit();
        s1.close();
    }
}
