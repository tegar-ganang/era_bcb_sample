package org.josef.test.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.sql.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.josef.demo.jpa.Element;
import org.josef.demo.jpa.Moon;
import org.josef.demo.jpa.NobelPrize;
import org.josef.demo.jpa.NobelPrizeCategory;
import org.josef.demo.jpa.NobelPrizeLaureate;
import org.josef.demo.jpa.Planet;
import org.josef.demo.jpa.Scientist;
import org.josef.jpa.EntityDataAccess;
import org.josef.jpa.EntityManagerHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * JUnit test class for class {@link EntityDataAccess}.
 * @author Kees Schotanus
 * @version 1.2 $Revision: 752 $
 */
public class EntityDataAccessTest {

    /**
     * Cleans all the records from the Database that were created as a result of
     * executing the Unit Tests in this class.
     */
    @BeforeClass
    @AfterClass
    public static void cleanUpDatabase() {
        try {
            if (EntityManagerHelper.getTransaction().isActive()) {
                EntityManagerHelper.rollback();
            }
        } catch (final PersistenceException exception) {
            ;
        }
        EntityManagerHelper.beginTransaction();
        final Query removeElementsQuery = EntityManagerHelper.createQuery("DELETE from Element e where e.id >= 200");
        removeElementsQuery.executeUpdate();
        final Query removePlanetsQuery = EntityManagerHelper.createQuery("DELETE from Planet p where p.id > 9");
        removePlanetsQuery.executeUpdate();
        EntityManagerHelper.createQuery("DELETE from Scientist s").executeUpdate();
        EntityManagerHelper.createQuery("DELETE from NobelPrize n").executeUpdate();
        EntityManagerHelper.createQuery("DELETE from NobelPrizeLaureate n").executeUpdate();
        EntityManagerHelper.commit();
    }

    /**
     * Tests {@link EntityDataAccess#persist(org.josef.jpa.Persistable)} and
     * {@link EntityDataAccess#findById(Class, Long)} using a single Entity.
     */
    @Test
    public void persistSingle() {
        final Element elementToInsert = new Element(Long.valueOf(220), "DDZ", "Fantasy220");
        EntityManagerHelper.beginTransaction();
        EntityDataAccess.persist(elementToInsert);
        EntityManagerHelper.commit();
        EntityManagerHelper.beginTransaction();
        final Element elementFound = EntityDataAccess.findById(Element.class, Long.valueOf(220));
        assertEquals(elementToInsert.getPrimaryKey(), elementFound.getPrimaryKey());
        EntityManagerHelper.commit();
    }

    /**
     * Tests {@link EntityDataAccess#merge(org.josef.jpa.Persistable)} using a
     * single Entity.
     */
    @Test
    public void mergeSingle() {
        final Element element = new Element(Long.valueOf(221), "DDU", "Fantasy221");
        EntityManagerHelper.beginTransaction();
        EntityDataAccess.persist(element);
        EntityManagerHelper.commit();
        element.setName("Updated");
        EntityManagerHelper.beginTransaction();
        final Element mergedElement = EntityDataAccess.merge(element);
        assertEquals(element.getName(), mergedElement.getName());
        EntityManagerHelper.commit();
    }

    /**
     * Tests {@link EntityDataAccess#remove(org.josef.jpa.Persistable)} using a
     * single Entity.
     */
    @Test
    public void removeSingle() {
        final Element element = new Element(Long.valueOf(222), "DDD", "Fantasy222");
        EntityManagerHelper.beginTransaction();
        EntityDataAccess.persist(element);
        EntityManagerHelper.commit();
        EntityManagerHelper.beginTransaction();
        EntityDataAccess.remove(element);
        EntityManagerHelper.commit();
        EntityManagerHelper.beginTransaction();
        final Element elementFound = EntityDataAccess.findById(Element.class, Long.valueOf(222));
        assertTrue(elementFound == null);
        EntityManagerHelper.commit();
    }

    /**
     * Tests {@link EntityDataAccess#persist(org.josef.jpa.Persistable)} and
     * {@link EntityDataAccess#findById(Class, Long)} using a parent/child
     * relationship.
     */
    @Test
    public void persistParentChild() {
        final Long planetId = 10L;
        final Set<Moon> moons = new HashSet<Moon>(1);
        moons.add(new Moon("101", "FantasyMoon101"));
        final Planet planetToInsert = new Planet(planetId, "Fantasy10", moons);
        EntityManagerHelper.beginTransaction();
        EntityDataAccess.persist(planetToInsert);
        EntityManagerHelper.commit();
        EntityManagerHelper.beginTransaction();
        Planet planetFound = EntityDataAccess.findById(Planet.class, planetId);
        assertEquals(planetToInsert.getPrimaryKey(), planetFound.getPrimaryKey());
        assertEquals(1, planetFound.getMoons().size());
        EntityManagerHelper.commit();
    }

    /**
     * Tests {@link EntityDataAccess#merge(org.josef.jpa.Persistable)} using a
     * parent/child relationship.
     */
    @Test
    public void mergeParentChild() {
        final Long planetId = 11L;
        final Planet planet = new Planet(planetId, "Fantasy11");
        EntityManagerHelper.beginTransaction();
        EntityDataAccess.persist(planet);
        EntityManagerHelper.commit();
        planet.addMoon(new Moon("111", "The Moon"));
        planet.setGravity(10.0);
        EntityManagerHelper.beginTransaction();
        EntityDataAccess.merge(planet);
        EntityManagerHelper.commit();
        EntityManagerHelper.beginTransaction();
        Planet planetFound = EntityDataAccess.findById(Planet.class, planetId);
        assertEquals(planetId, planetFound.getPrimaryKey());
        assertEquals(10.0, planetFound.getGravity(), 0);
        assertEquals(1, planetFound.getMoons().size());
        EntityManagerHelper.commit();
    }

    /**
     * Tests {@link EntityDataAccess#remove(org.josef.jpa.Persistable)} using a
     * parent/child relationship.
     * <br>In this particular case the parent (Planet) is removed.
     */
    @Test
    public void removeParent() {
        final Long planetId = 12L;
        final Planet planetToInsert = new Planet(planetId, "Planet12");
        planetToInsert.addMoon(new Moon("one", "Planet12Moon1"));
        planetToInsert.addMoon(new Moon("two", "Planet12Moon2"));
        EntityManagerHelper.beginTransaction();
        EntityDataAccess.persist(planetToInsert);
        EntityManagerHelper.commit();
        EntityManagerHelper.beginTransaction();
        EntityDataAccess.remove(planetToInsert);
        EntityManagerHelper.commit();
        EntityManagerHelper.beginTransaction();
        final Planet planetFound = EntityDataAccess.findById(Planet.class, planetId);
        assertTrue(planetFound == null);
        EntityManagerHelper.commit();
    }

    /**
     * Tests {@link EntityDataAccess#remove(org.josef.jpa.Persistable)} using a
     * parent/child relationship.
     * <br>In this particular case a child (Moon) is removed from the parent
     * (Planet).
     */
    @Test
    public void removeChild() {
        final Long planetId = 13L;
        final Planet planetToInsert = new Planet(planetId, "Planet13");
        final Moon moonOne = new Moon("abc", "Planet13Moon1");
        final Moon moonTwo = new Moon("def", "Planet13Moon2");
        planetToInsert.addMoon(moonOne);
        planetToInsert.addMoon(moonTwo);
        EntityManagerHelper.beginTransaction();
        EntityDataAccess.persist(planetToInsert);
        EntityManagerHelper.commit();
        EntityManagerHelper.beginTransaction();
        Planet planetFound = EntityDataAccess.findById(Planet.class, planetId);
        final org.josef.demo.jpa.Moon moonFound = planetFound.getMoons().iterator().next();
        final boolean removed = planetFound.getMoons().remove(moonFound);
        System.out.println("removed=" + removed);
        EntityDataAccess.remove(moonFound);
        EntityManagerHelper.commit();
        planetFound = EntityDataAccess.findById(Planet.class, planetId);
        if (planetFound == null) {
            fail("Planet should exist but has disappeared!");
        } else {
            assertEquals(1, planetFound.getMoons().size());
        }
    }

    /**
     * Tests {@link EntityDataAccess#persist(org.josef.jpa.Persistable)} using a
     * parent/child relationship.
     * <br>In this particular case a Scientist and NobelPrize are linked to form
     * a NobelPrizeLaureate.
     */
    @Test
    public void persistKoppelEntiteit() {
        final NobelPrizeCategory physics = EntityDataAccess.findById(NobelPrizeCategory.class, 1L);
        final GregorianCalendar einsteinsBirthDate = new GregorianCalendar(1879, 2, 14);
        final Scientist einstein = new Scientist("Einstein", "Einstein, Albert", new Date(einsteinsBirthDate.getTimeInMillis()));
        final NobelPrize nobelPrize = new NobelPrize(physics, Integer.valueOf(1921));
        final NobelPrizeLaureate nobelPrizeLaurate = new NobelPrizeLaureate(einstein, nobelPrize);
        EntityManagerHelper.beginTransaction();
        EntityDataAccess.persist(einstein);
        EntityDataAccess.persist(nobelPrize);
        EntityDataAccess.persist(nobelPrizeLaurate);
        EntityManagerHelper.commit();
    }

    /**
     * Simple main method to run/debug tests using text mode version of JUnit.
     * @param args Not used.
     * @throws Exception Depending on executed code.
     */
    public static void main(final String[] args) throws Exception {
        java.util.logging.Handler fh = new java.util.logging.FileHandler("/temp/josef.log");
        java.util.logging.Logger.getLogger("org.josef").addHandler(fh);
        java.util.logging.Logger.getLogger("org.josef").setLevel(java.util.logging.Level.FINEST);
        org.junit.runner.JUnitCore.runClasses(EntityDataAccessTest.class);
    }
}
