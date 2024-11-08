package org.datanucleus.tests;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.datanucleus.tests.JPAPersistenceTestCase;
import org.jpox.samples.annotations.models.company.Employee;
import org.jpox.samples.annotations.models.company.Manager;
import org.jpox.samples.annotations.models.company.Person;
import org.jpox.samples.annotations.one_many.bidir.Animal;
import org.jpox.samples.annotations.one_many.bidir.Farm;
import org.jpox.samples.annotations.one_many.bidir_2.House;
import org.jpox.samples.annotations.one_many.bidir_2.Window;
import org.jpox.samples.annotations.one_many.collection.ListHolder;
import org.jpox.samples.annotations.one_many.collection.PCFKListElement;
import org.jpox.samples.annotations.one_many.unidir_2.GroupMember;
import org.jpox.samples.annotations.one_many.unidir_2.UserGroup;
import org.jpox.samples.annotations.one_one.bidir.Boiler;
import org.jpox.samples.annotations.one_one.bidir.Timer;
import org.jpox.samples.annotations.one_one.unidir.Login;
import org.jpox.samples.annotations.one_one.unidir.LoginAccount;
import org.jpox.samples.annotations.types.basic.DateHolder;

/**
 * Tests for JPQL.
 */
public class JPQLQueryTest extends JPAPersistenceTestCase {

    private static boolean initialised = false;

    public JPQLQueryTest(String name) {
        super(name);
        if (!initialised) {
            addClassesToSchema(new Class[] { Person.class, Employee.class, Manager.class, Animal.class, Farm.class, House.class, Window.class, Boiler.class, Timer.class, Login.class, LoginAccount.class, UserGroup.class, GroupMember.class });
        }
    }

    public void testBasicIncompleteQuery() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Person p = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
                em.persist(p);
                em.createQuery("SELECT T FROM " + Person.class.getName()).getResultList();
                fail("should have thrown an exception");
            } catch (PersistenceException e) {
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

    public void testBasicQuery() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Person p = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
                em.persist(p);
                List result = em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " T").getResultList();
                assertEquals(1, result.size());
                tx.rollback();
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

    public void testMaxResultsQuery() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
                em.persist(p1);
                Person p2 = new Person(102, "Barney", "Rubble", "barney.rubble@jpox.com");
                em.persist(p2);
                em.flush();
                tx.commit();
                tx.begin();
                List result = em.createQuery("SELECT T FROM " + Person.class.getName() + " T").setMaxResults(1).getResultList();
                assertEquals(1, result.size());
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

    public void testQueryUsingEntityName() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
                em.persist(p1);
                em.flush();
                List result = em.createQuery("SELECT Object(T) FROM Person_Ann T").getResultList();
                assertEquals(1, result.size());
                tx.rollback();
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

    public void testQueryUsingEntityNameNotYetLoaded() {
        getEMF("JPATest");
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            List result = em.createQuery("SELECT Object(T) FROM Person_Ann T").getResultList();
            assertEquals(0, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
            getEMF(null);
        }
    }

    public void testLikeQuery() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p);
            em.flush();
            List result = em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " T WHERE firstName like '%Fred%'").getResultList();
            assertEquals(1, result.size());
            result = em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " T WHERE T.firstName like '%Fred%'").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testOrderBy() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            Person p2 = new Person(102, "Barney", "Rubble", "barney.rubble@jpox.com");
            em.persist(p2);
            List result = em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " T ORDER BY T.firstName DESC").getResultList();
            assertEquals(2, result.size());
            assertEquals("Fred", ((Person) result.get(0)).getFirstName());
            assertEquals("Barney", ((Person) result.get(1)).getFirstName());
            result = em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " T ORDER BY T.firstName ASC").getResultList();
            assertEquals(2, result.size());
            assertEquals("Barney", ((Person) result.get(0)).getFirstName());
            assertEquals("Fred", ((Person) result.get(1)).getFirstName());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testIsNull() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            List result = em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " T where T.firstName IS NULL").getResultList();
            assertEquals(0, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testIsNotNull() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            List result = em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " T where T.firstName IS NOT NULL").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testIsSomething() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            List result = em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " T where T.firstName IS something").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
            fail("Expected exception");
        } catch (RuntimeException ex) {
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testIsNotSomething() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            List result = em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " T where T.firstName IS NOT something").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
            fail("Expected exception");
        } catch (RuntimeException ex) {
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testNotEquals() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            List result = em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " T where T.firstName <> 'Fred1' ").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testNoResultExceptionThrown() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " T where T.firstName <> 'Fred' ").getSingleResult();
            fail("expected NoResultException");
        } catch (NoResultException ex) {
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testNonUniqueResultExceptionThrown() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            Person p2 = new Person(102, "Barney", "Rubble", "barney.rubble@jpox.com");
            em.persist(p2);
            em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " T where T.firstName <> 'Wilma' ").getSingleResult();
            fail("expected NonUniqueResultException");
        } catch (NonUniqueResultException ex) {
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test the specification of positional parameters.
     */
    public void testPositionalParameter() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            Query q = em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " T where T.firstName <> ?1 AND T.firstName = ?2");
            q.setParameter(1, "Fred1");
            q.setParameter(2, "Fred");
            List result = q.getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test the specification of named parameters with a relation.
     */
    public void testNamedParameterRelation() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            Query q = em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " T where :name > T.globalNum");
            try {
                q.setParameter("badName", "Fred1");
            } catch (IllegalArgumentException iae) {
                return;
            } catch (Exception e) {
                fail("Unexpected exception thrown when setting parameter " + e.getMessage());
            }
            fail("Allowed to specify parameter with invalid name");
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test of trying to set a named parameter where the named parameter doesnt exist in the query.
     * Expects an IllegalArgumentException to be thrown
     */
    public void testUnknownParameter() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            Query q = em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " T where T.firstName = :theName");
            try {
                q.setParameter("otherName", "John");
            } catch (IllegalArgumentException iae) {
                return;
            }
            fail("Should have thrown IllegalArgumentException on setting wrong parameter name but didnt");
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test of trying to set a named parameter where the type used is incorrect.
     * Expects an IllegalArgumentException to be thrown
     */
    public void testParameterWithIncorrectType() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            Query q = em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " T where T.firstName = :theName");
            try {
                q.setParameter("theName", new Integer(1));
            } catch (IllegalArgumentException iae) {
                return;
            }
            fail("Should have thrown IllegalArgumentException on setting parameter with wrong type but didnt");
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testAsQuery() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            List result = em.createQuery("SELECT Object(T) FROM " + Person.class.getName() + " AS T").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test for Inner Join syntax.
     */
    public void testInnerJoinSyntax() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            List result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P JOIN P.bestFriend AS B").getResultList();
            assertEquals(0, result.size());
            result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P INNER JOIN P.bestFriend AS B").getResultList();
            assertEquals(0, result.size());
            result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P INNER JOIN P.bestFriend").getResultList();
            assertEquals(0, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testFetchJoinSyntax() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            List result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P JOIN FETCH P.bestFriend AS B").getResultList();
            assertEquals(0, result.size());
            result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P INNER JOIN FETCH P.bestFriend AS B").getResultList();
            assertEquals(0, result.size());
            result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P INNER JOIN FETCH P.bestFriend").getResultList();
            assertEquals(0, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test for Inner Join 1-1 relation from the owner side.
     */
    public void testInnerJoinOneToOneOwner() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Boiler boiler = new Boiler("Baxi", "Calentador");
            Timer timer = new Timer("Casio", true, boiler);
            em.persist(timer);
            em.flush();
            List result = em.createQuery("SELECT Object(T) FROM " + Timer.class.getName() + " T " + "INNER JOIN T.boiler B").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test for Inner Join 1-1 relation from the non-owner side.
     */
    public void testInnerJoinOneToOneNonOwner() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Boiler boiler = new Boiler("Baxi", "Calentador");
            Timer timer = new Timer("Casio", true, boiler);
            em.persist(timer);
            em.flush();
            List result = em.createQuery("SELECT Object(B) FROM " + Boiler.class.getName() + " B " + "INNER JOIN B.timer T").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test for Inner Join 1-N bidirectional FK relation from the owner side.
     */
    public void testInnerJoinOneToManyBiFKOwner() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Farm farm1 = new Farm("High Farm");
            Farm farm2 = new Farm("Low Farm");
            Animal a1 = new Animal("Dog");
            Animal a2 = new Animal("Sheep");
            Animal a3 = new Animal("Cow");
            farm1.getAnimals().add(a1);
            farm1.getAnimals().add(a2);
            farm2.getAnimals().add(a3);
            em.persist(farm1);
            em.persist(farm2);
            em.flush();
            List result = em.createQuery("SELECT Object(F) FROM " + Farm.class.getName() + " F " + "INNER JOIN F.animals A").getResultList();
            assertEquals(3, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test for Inner Join 1-N unidirectional FK relation from the owner side.
     */
    @SuppressWarnings("unchecked")
    public void testInnerJoinOneToManyUniFKOwner() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            UserGroup grp = new UserGroup(101, "JPOX Users");
            GroupMember member1 = new GroupMember(201, "Joe User");
            grp.getMembers().add(member1);
            em.persist(grp);
            em.flush();
            List result = em.createQuery("SELECT Object(G) FROM " + UserGroup.class.getName() + " G " + "INNER JOIN G.members M").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test for Inner Join 1-N bidirectional JoinTable relation from the owner side.
     */
    public void testInnerJoinOneToManyBiJoinTableOwner() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            House house = new House(101, "Coronation Street");
            Window window = new Window(200, 400, house);
            house.getWindows().add(window);
            em.persist(house);
            em.flush();
            List result = em.createQuery("SELECT Object(H) FROM " + House.class.getName() + " H " + "INNER JOIN H.windows W").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test for Inner Join N-1 bidirectional JoinTable relation from the element side.
     */
    public void testInnerJoinManyToOneBiJoinTableElement() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            House house = new House(101, "Coronation Street");
            Window window = new Window(200, 400, house);
            house.getWindows().add(window);
            em.persist(house);
            em.flush();
            List result = em.createQuery("SELECT Object(W) FROM " + Window.class.getName() + " W " + "INNER JOIN W.house H").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test for Left Outer Join.
     */
    public void testLeftOuterJoinQuery() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            LoginAccount acct = new LoginAccount(1, "Fred", "Flintstone");
            Login login = new Login("fred", "yabbadabbadoo");
            acct.setLogin(login);
            em.persist(acct);
            em.flush();
            List result = em.createQuery("SELECT Object(A) FROM " + LoginAccount.class.getName() + " A " + "LEFT OUTER JOIN A.login L " + "WHERE L.userName = 'fred'").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test for multiple levels of field access via identifiers.
     */
    public void testThreeLevelsOfFieldAccess() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Boiler boiler = new Boiler("Baxi", "Calentador");
            Timer timer = new Timer("Casio", true, boiler);
            boiler.setTimer(timer);
            em.persist(timer);
            em.flush();
            List result = em.createQuery("Select b.model FROM " + Boiler.class.getName() + " b " + "WHERE b.timer.make = 'Seiko'").getResultList();
            assertEquals(0, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test projection of N-1 field.
     */
    public void testProjectionOfManyToOneField() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Farm farm1 = new Farm("High Farm");
            Farm farm2 = new Farm("Low Farm");
            Animal a1 = new Animal("Dog");
            Animal a2 = new Animal("Sheep");
            Animal a3 = new Animal("Cow");
            farm1.getAnimals().add(a1);
            farm1.getAnimals().add(a2);
            farm2.getAnimals().add(a3);
            em.persist(farm1);
            em.persist(farm2);
            em.flush();
            List results = em.createQuery("SELECT a.farm FROM " + Animal.class.getName() + " a ", Farm.class).getResultList();
            assertEquals(3, results.size());
            Object result = results.get(0);
            assertNotNull(result);
            assertTrue("Result is of incorrect type", result instanceof Farm);
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testNotBetweenQuery() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            List result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P WHERE 2 NOT BETWEEN 1 AND 3").getResultList();
            assertEquals(0, result.size());
            result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P WHERE 2 NOT BETWEEN 3 AND 4").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testBetweenQuery() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            List result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P WHERE 2 BETWEEN 1 AND 3").getResultList();
            assertEquals(1, result.size());
            result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P WHERE 2 BETWEEN 3 AND 4").getResultList();
            assertEquals(0, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testABS() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            List result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P WHERE ABS(2) = 2").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testSUBSTRING() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            List result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P WHERE SUBSTRING('erik',2,2) = 'ri'").getResultList();
            assertEquals(1, result.size());
            result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P WHERE SUBSTRING('erik',2) = 'rik'").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testLOCATE() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            List result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P WHERE LOCATE('r','erik') = 2").getResultList();
            assertEquals(1, result.size());
            result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P WHERE LOCATE('i','eriki',5) = 5").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testHaving() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            List result = em.createQuery("SELECT P.firstName FROM " + Person.class.getName() + " P Group By P.firstName HAVING P.firstName = 'Fred'").getResultList();
            assertEquals(1, result.size());
            assertEquals("Fred", result.get(0).toString());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testConcat() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            List result = em.createQuery("SELECT P.firstName FROM " + Person.class.getName() + " P WHERE P.firstName = concat(:a,:b)").setParameter("a", "Fr").setParameter("b", "ed").getResultList();
            assertEquals(1, result.size());
            assertEquals("Fred", result.get(0).toString());
            result = em.createQuery("SELECT P.firstName FROM " + Person.class.getName() + " P WHERE P.firstName = concat(:c,concat(:a,:b))").setParameter("a", "r").setParameter("b", "ed").setParameter("c", "F").getResultList();
            assertEquals(1, result.size());
            assertEquals("Fred", result.get(0).toString());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testTrueFalse() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            List result = em.createQuery("SELECT P.firstName FROM " + Person.class.getName() + " P WHERE false = True").getResultList();
            assertEquals(0, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testNot() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            List result = em.createQuery("SELECT P.firstName FROM " + Person.class.getName() + " P WHERE NOT (1 = 0)").getResultList();
            assertEquals(1, result.size());
            result = em.createQuery("SELECT P.firstName FROM " + Person.class.getName() + " P WHERE NOT (1 = 0 AND 1 = 2)").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testNonTransactionalQuery() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            List result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P").getResultList();
            assertEquals(0, result.size());
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test of JPQL having case insensitive identifiers.
     */
    public void testCaseInsensitiveIdentifier() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
                em.persist(p1);
                Person p2 = new Person(102, "Barney", "Rubble", "barney.rubble@jpox.com");
                em.persist(p2);
                em.flush();
                tx.commit();
                tx.begin();
                List result = em.createQuery("SELECT DISTINCT Object(P) FROM " + Person.class.getName() + " p").getResultList();
                assertEquals(2, result.size());
                tx.commit();
            } catch (Exception e) {
                LOG.error("Exception in test", e);
                fail("Exception thrown generating query testing case-insensitive identifiers " + e.getMessage());
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
     * Test of JPQL "IN (literal)" syntax.
     */
    public void testInLiterals() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
                em.persist(p1);
                Person p2 = new Person(102, "Barney", "Rubble", "barney.rubble@jpox.com");
                em.persist(p2);
                Person p3 = new Person(103, "Pebbles", "Flintstone", "pebbles.flintstone@jpox.com");
                em.persist(p3);
                em.flush();
                tx.commit();
                tx.begin();
                List result = em.createQuery("SELECT DISTINCT Object(p) FROM " + Person.class.getName() + " p " + "WHERE p.firstName IN ('Fred', 'Pebbles')").getResultList();
                assertEquals(2, result.size());
                tx.commit();
                tx.begin();
                result = em.createQuery("SELECT DISTINCT Object(p) FROM " + Person.class.getName() + " p " + "WHERE p.firstName NOT IN ('Fred', 'Pebbles')").getResultList();
                assertEquals(1, result.size());
                tx.commit();
            } catch (Exception e) {
                LOG.error("Exception in test", e);
                fail("Exception thrown generating query with IN syntax for literals " + e.getMessage());
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
     * Test of JPQL "IN (parameter)" syntax.
     */
    public void testInParameters() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
                em.persist(p1);
                Person p2 = new Person(102, "Barney", "Rubble", "barney.rubble@jpox.com");
                em.persist(p2);
                Person p3 = new Person(103, "Pebbles", "Flintstone", "pebbles.flintstone@jpox.com");
                em.persist(p3);
                em.flush();
                tx.commit();
                tx.begin();
                Query q1 = em.createQuery("SELECT DISTINCT Object(p) FROM " + Person.class.getName() + " p " + "WHERE p.firstName IN (:param1, :param2)");
                q1.setParameter("param1", "Fred");
                q1.setParameter("param2", "Pebbles");
                List result = q1.getResultList();
                assertEquals(2, result.size());
                tx.commit();
                tx.begin();
                Query q2 = em.createQuery("SELECT DISTINCT Object(p) FROM " + Person.class.getName() + " p " + "WHERE p.firstName NOT IN (:param1, :param2)");
                q2.setParameter("param1", "Fred");
                q2.setParameter("param2", "Pebbles");
                result = q2.getResultList();
                assertEquals(1, result.size());
                tx.commit();
                tx.begin();
                Query q3 = em.createQuery("SELECT DISTINCT Object(p) FROM " + Person.class.getName() + " p " + "WHERE p.firstName IN (:param1)");
                Collection<String> options = new HashSet<String>();
                options.add("Fred");
                options.add("Pebbles");
                q3.setParameter("param1", options);
                result = q3.getResultList();
                assertEquals(2, result.size());
                tx.commit();
            } catch (Exception e) {
                LOG.error("Exception in test", e);
                fail("Exception thrown generating query with IN syntax for literals " + e.getMessage());
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
     * Test of JPQL "MEMBER [OF] (container-expr)" syntax.
     */
    public void testMemberOf() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Employee e1 = new Employee(101, "Fred", "Flintstone", "fred.flintstone@jpox.com", 30000f, "1234A");
                Employee e2 = new Employee(102, "Barney", "Rubble", "barney.rubble@jpox.com", 27000f, "1234B");
                Employee e3 = new Employee(103, "George", "Cement", "george.cement@jpox.com", 20000f, "1235C");
                Manager mgr1 = new Manager(100, "Chief", "Rock", "chief.rock@warnerbros.com", 40000.0f, "12345A");
                mgr1.setBestFriend(e1);
                Manager mgr2 = new Manager(106, "Boss", "Blaster", "boss.blaster@warnerbros.com", 40005.0f, "12345B");
                mgr2.setBestFriend(e2);
                mgr1.addSubordinate(e1);
                mgr1.addSubordinate(e2);
                e1.setManager(mgr1);
                e2.setManager(mgr1);
                mgr2.addSubordinate(e3);
                e3.setManager(mgr2);
                em.persist(mgr1);
                em.persist(mgr2);
                em.flush();
                tx.commit();
                tx.begin();
                Employee emp = (Employee) em.createQuery("SELECT DISTINCT Object(e) FROM " + Employee.class.getName() + " e " + "WHERE e.firstName = 'Fred'").getSingleResult();
                List result = em.createQuery("SELECT DISTINCT Object(m) FROM " + Manager.class.getName() + " m " + "WHERE :param MEMBER OF m.subordinates").setParameter("param", emp).getResultList();
                assertEquals(1, result.size());
                Manager mgr = (Manager) result.get(0);
                assertEquals("Manager returned from MEMBER OF query has incorrect firstName", "Chief", mgr.getFirstName());
                assertEquals("Manager returned from MEMBER OF query has incorrect lastName", "Rock", mgr.getLastName());
                result = em.createQuery("SELECT DISTINCT Object(m) FROM " + Manager.class.getName() + " m " + "WHERE :param NOT MEMBER OF m.subordinates").setParameter("param", emp).getResultList();
                assertEquals(1, result.size());
                mgr = (Manager) result.get(0);
                assertEquals("Manager returned from NOT MEMBER OF query has incorrect firstName", "Boss", mgr.getFirstName());
                assertEquals("Manager returned from NOT MEMBER OF query has incorrect lastName", "Blaster", mgr.getLastName());
                result = em.createQuery("SELECT DISTINCT Object(m) FROM " + Manager.class.getName() + " m " + "WHERE m.bestFriend MEMBER OF m.subordinates").getResultList();
                assertEquals(1, result.size());
                mgr = (Manager) result.get(0);
                assertEquals("Manager returned from MEMBER OF query has incorrect firstName", "Chief", mgr.getFirstName());
                assertEquals("Manager returned from MEMBER OF query has incorrect lastName", "Rock", mgr.getLastName());
                tx.commit();
            } catch (Exception e) {
                LOG.error("Exception in test", e);
                fail("Exception thrown generating query with MEMBER syntax " + e.getMessage());
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                em.close();
            }
        } finally {
            clean(Manager.class);
            clean(Employee.class);
        }
    }

    public void testQueryNestedCollections() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Query query = em.createQuery("SELECT Object(M) FROM " + Manager.class.getName() + " AS M " + ", IN (M.departments) D" + ", IN (D.projects) P" + "WHERE P.name = 'DN'");
                List result = query.getResultList();
                assertEquals(0, result.size());
                tx.rollback();
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                em.close();
            }
        } finally {
            clean(Manager.class);
        }
    }

    /**
     * Test of JPQL "MEMBER [OF] (container-expr)" syntax, using JoinTable.
     */
    public void testMemberOfViaUnboundVariableJoinTable() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Employee e1 = new Employee(101, "Fred", "Flintstone", "fred.flintstone@jpox.com", 30000f, "1234A");
                Employee e2 = new Employee(102, "Barney", "Rubble", "barney.rubble@jpox.com", 27000f, "1234B");
                Employee e3 = new Employee(103, "George", "Cement", "george.cement@jpox.com", 20000f, "1235C");
                Manager mgr1 = new Manager(100, "Chief", "Rock", "chief.rock@warnerbros.com", 40000.0f, "12345A");
                mgr1.setBestFriend(e1);
                Manager mgr2 = new Manager(106, "Boss", "Blaster", "boss.blaster@warnerbros.com", 40005.0f, "12345B");
                mgr2.setBestFriend(e2);
                mgr1.addSubordinate(e1);
                mgr1.addSubordinate(e2);
                e1.setManager(mgr1);
                e2.setManager(mgr1);
                mgr2.addSubordinate(e3);
                e3.setManager(mgr2);
                em.persist(mgr1);
                em.persist(mgr2);
                em.flush();
                tx.commit();
                tx.begin();
                List result = em.createQuery("SELECT DISTINCT Object(m) FROM " + Manager.class.getName() + " m," + Employee.class.getName() + " e " + "WHERE e MEMBER OF m.subordinates AND e.firstName = 'Barney'").getResultList();
                assertEquals(1, result.size());
                Manager mgr = (Manager) result.get(0);
                assertEquals("Manager returned from MEMBER OF query has incorrect firstName", "Chief", mgr.getFirstName());
                assertEquals("Manager returned from MEMBER OF query has incorrect lastName", "Rock", mgr.getLastName());
                tx.commit();
            } catch (Exception e) {
                LOG.error("Exception in test", e);
                fail("Exception thrown generating query with MEMBER syntax " + e.getMessage());
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                em.close();
            }
        } finally {
            clean(Manager.class);
            clean(Employee.class);
        }
    }

    /**
     * Test of JPQL "MEMBER [OF] (container-expr)" syntax, using FK.
     */
    public void testMemberOfViaUnboundVariableForeignKey() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Farm farm1 = new Farm("High Farm");
                Farm farm2 = new Farm("Low Farm");
                Animal a1 = new Animal("Dog");
                Animal a2 = new Animal("Sheep");
                Animal a3 = new Animal("Cow");
                farm1.getAnimals().add(a1);
                farm1.getAnimals().add(a2);
                farm2.getAnimals().add(a3);
                em.persist(farm1);
                em.persist(farm2);
                em.flush();
                tx.commit();
                tx.begin();
                List result = em.createQuery("SELECT DISTINCT Object(f) FROM " + Farm.class.getName() + " f," + Animal.class.getName() + " a " + "WHERE a MEMBER OF f.animals AND a.name = 'Dog'").getResultList();
                assertEquals(1, result.size());
                Farm farm = (Farm) result.get(0);
                assertEquals("Farm returned from MEMBER OF query has incorrect name", "High Farm", farm.getName());
                tx.commit();
            } catch (Exception e) {
                LOG.error("Exception in test", e);
                fail("Exception thrown generating query with MEMBER syntax " + e.getMessage());
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                em.close();
            }
        } finally {
            clean(Farm.class);
            clean(Animal.class);
        }
    }

    /**
     * Test of simple UPDATE statement then calling getSingleResult().
     * This should throw an IllegalStateException
     */
    public void testUpdateSingleResult() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Person p = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
                em.persist(p);
                em.flush();
                Query q = em.createQuery("UPDATE Person p SET p.emailAddress = :param");
                q.setParameter("param", "fred@flintstones.com");
                try {
                    q.getSingleResult();
                } catch (IllegalStateException ise) {
                    return;
                }
                fail("Called getSingleResult() on an UPDATE query!");
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
     * Test for use of CURRENT_DATE.
     */
    public void testCurrentDate() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            DateHolder d1 = new DateHolder();
            Calendar cal = Calendar.getInstance();
            cal.set(2006, 11, 01);
            d1.setDateField(cal.getTime());
            em.persist(d1);
            DateHolder d2 = new DateHolder();
            Calendar cal2 = Calendar.getInstance();
            cal2.set(2012, 11, 01);
            d2.setDateField(cal2.getTime());
            em.persist(d2);
            em.flush();
            List result = em.createQuery("SELECT Object(D) FROM " + DateHolder.class.getName() + " D WHERE D.dateField < CURRENT_DATE").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test for use of LENGTH.
     */
    public void testStringLength() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p);
            em.flush();
            List result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P WHERE LENGTH(P.firstName) > 3").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test for use of TRIM.
     */
    public void testStringTrim() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p = new Person(101, "Fred   ", "   Flintstone", "   fred.flintstone@jpox.com   ");
            em.persist(p);
            em.flush();
            List result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P " + "WHERE TRIM(LEADING FROM lastName) = 'Flintstone'").getResultList();
            assertEquals(1, result.size());
            result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P " + "WHERE TRIM(TRAILING FROM firstName) = 'Fred'").getResultList();
            assertEquals(1, result.size());
            result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P " + "WHERE TRIM(emailAddress) = 'fred.flintstone@jpox.com'").getResultList();
            assertEquals(1, result.size());
            result = em.createQuery("SELECT Object(P) FROM " + Person.class.getName() + " P " + "WHERE TRIM(BOTH FROM emailAddress) = 'fred.flintstone@jpox.com'").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testSingleResultWithParams() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Person p1 = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
            em.persist(p1);
            em.flush();
            Query q = em.createQuery("SELECT T FROM " + Person.class.getName() + " T where T.firstName = :param");
            q.setParameter("param", "Fred");
            Person p = (Person) q.getSingleResult();
            assertNotNull("Returned object was null!", p);
            assertEquals("First name was wrong", "Fred", p.getFirstName());
            assertEquals("Last name was wrong", "Flintstone", p.getLastName());
        } catch (NoResultException ex) {
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    /**
     * Test for SIZE function of container field.
     */
    public void testContainerSIZE() {
        EntityManager em = getEM();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Farm farm1 = new Farm("High Farm");
            Farm farm2 = new Farm("Low Farm");
            Animal a1 = new Animal("Dog");
            Animal a2 = new Animal("Sheep");
            Animal a3 = new Animal("Cow");
            farm1.getAnimals().add(a1);
            farm1.getAnimals().add(a2);
            farm2.getAnimals().add(a3);
            em.persist(farm1);
            em.persist(farm2);
            em.flush();
            List result = em.createQuery("SELECT Object(F) FROM " + Farm.class.getName() + " F WHERE SIZE(animals) > 1").getResultList();
            assertEquals(1, result.size());
            tx.rollback();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            em.close();
        }
    }

    public void testTYPE() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Person p = new Person(101, "Fred", "Flintstone", "fred.flintstone@jpox.com");
                em.persist(p);
                Employee e = new Employee(102, "Barney", "Rubble", "barney.rubble@jpox.com", 10000.0f, "12345");
                em.persist(e);
                em.flush();
                List result = em.createQuery("SELECT Object(p) FROM " + Person.class.getName() + " p WHERE TYPE(p) <> Employee_Ann").getResultList();
                assertEquals(1, result.size());
                tx.rollback();
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                em.close();
            }
        } finally {
            clean(Employee.class);
            clean(Person.class);
        }
    }

    public void testCASE() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Person p = new Person(105, "Pebbles", "Flintstone", "pebbles.flintstone@datanucleus.org");
                p.setAge(5);
                em.persist(p);
                Employee e = new Employee(102, "Barney", "Rubble", "barney.rubble@jpox.com", 10000.0f, "12345");
                e.setAge(35);
                em.persist(e);
                em.flush();
                List result = em.createQuery("SELECT p.personNum, CASE WHEN p.age < 20 THEN 'Youth' WHEN p.age >= 20 AND p.age < 50 THEN 'Adult' ELSE 'Old'" + " FROM " + Person.class.getName() + " p").getResultList();
                Iterator resultsIter = result.iterator();
                boolean pebbles = false;
                boolean barney = false;
                while (resultsIter.hasNext()) {
                    Object[] values = (Object[]) resultsIter.next();
                    if (((Number) values[0]).intValue() == 105 && values[1].equals("Youth")) {
                        pebbles = true;
                    }
                    if (((Number) values[0]).intValue() == 102 && values[1].equals("Adult")) {
                        barney = true;
                    }
                }
                assertTrue("Pebbles wasn't correct in the Case results", pebbles);
                assertTrue("Barney wasn't correct in the Case results", barney);
                tx.rollback();
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                em.close();
            }
        } finally {
            clean(Employee.class);
            clean(Person.class);
        }
    }

    public void testINDEX() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                ListHolder holder = new ListHolder(1);
                holder.getJoinListPC().add(new PCFKListElement(1, "First"));
                holder.getJoinListPC().add(new PCFKListElement(2, "Second"));
                holder.getJoinListPC().add(new PCFKListElement(3, "Third"));
                em.persist(holder);
                em.flush();
                List result = em.createQuery("SELECT e.name FROM ListHolder l JOIN l.joinListPC e " + "WHERE INDEX(e) = 1").getResultList();
                assertEquals("Number of records is incorrect", 1, result.size());
                assertEquals("Name of element is incorrect", "Second", result.iterator().next());
                tx.rollback();
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                em.close();
            }
        } finally {
        }
    }

    public void testBulkDelete() {
        try {
            EntityManager em = getEM();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                Timer t = new Timer("Seiko", true, null);
                em.persist(t);
                em.flush();
                Query q = em.createQuery("DELETE FROM " + Timer.class.getName() + " t");
                int number = q.executeUpdate();
                assertEquals(1, number);
                tx.rollback();
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
                em.close();
            }
        } finally {
            clean(Timer.class);
        }
    }
}
