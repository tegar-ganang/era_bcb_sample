package org.datanucleus.tests.newfeatures;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import org.datanucleus.tests.JPAPersistenceTestCase;
import org.jpox.samples.annotations.models.company.Person;

/**
 * Tests for JPQL features that are not yet supported so are expected to fail.
 */
public class JPQLQueryTest extends JPAPersistenceTestCase {

    public JPQLQueryTest(String name) {
        super(name);
    }

    /**
     * Test of simple UPDATE statement.
     * See JIRA "NUCRDBMS-6"
     */
    public void testUpdateSimple() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Person p = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
                em.persist(p);
                em.flush();
                Query q = em.createQuery("UPDATE Person p SET p.emailAddress = :param WHERE p.firstName = 'Fred'");
                q.setParameter("param", "fred@flintstones.com");
                int val = q.executeUpdate();
                assertEquals("Number of records updated by query was incorrect", 1, val);
                tx.commit();
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                em.close();
            }
        } finally {
            clean(Person.class);
        }
    }

    /**
     * Test of simple DELETE statement.
     * See JIRA "NUCRDBMS-7"
     */
    public void testDeleteSimple() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Person p = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
                em.persist(p);
                em.flush();
                Query q = em.createQuery("DELETE FROM Person p WHERE p.firstName = 'Fred'");
                int val = q.executeUpdate();
                assertEquals("Number of records updated by query was incorrect", 1, val);
                tx.commit();
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                em.close();
            }
        } finally {
            clean(Person.class);
        }
    }
}
