package cz.kamosh.multiindex.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import org.junit.Assert;
import org.junit.Test;
import cz.kamosh.multiindex.criterion.ICriterion;
import cz.kamosh.multiindex.impl.Junction;
import cz.kamosh.multiindex.impl.Junction.Conjunction;
import cz.kamosh.multiindex.interf.IMultiIndexContainer;

public abstract class AbstractMultiIndexContainerTest<L, T extends IMultiIndexContainer<Person, Integer, L>> {

    private static final Logger logger = Logger.getLogger(AbstractMultiIndexContainerTest.class.getName());

    protected abstract T createMultiIndexContainer();

    protected abstract T createMultiIndexContainer(Collection<Person> people);

    protected abstract void addIndexForBirthYear(T mic);

    protected abstract void addIndexForSurname(T mic);

    protected abstract void addIndexForSex(T mic);

    protected abstract void addIndexForBMI(T mic);

    protected abstract Collection<Person> findEqBirthYear(T mic, int birthYear);

    protected abstract Collection<Person> findInBirthYear(T mic, Integer[] birthYears);

    protected abstract ICriterion<Person, Integer, L> createBetweenBirthYear(T mic, int minBirthYear, int maxBirthYear);

    protected abstract ICriterion<Person, Integer, L> createEqSex(T mic, boolean shouldBeMan);

    protected abstract ICriterion<Person, Integer, L> createEqBirthYear(T mic, int birthYear);

    protected abstract ICriterion<Person, Integer, L> createIsNullSurname(T mic);

    protected abstract ICriterion<Person, Integer, L> createLTBirthYear(T mic, int birthYear);

    protected abstract ICriterion<Person, Integer, L> createLTBMI(T mic, double bmi);

    public AbstractMultiIndexContainerTest() {
    }

    @Test
    public void testConstructor() {
        logger.info("testConstructor");
        T mic = createMultiIndexContainer();
        Assert.assertTrue("MultiIndexContainer constructor has not passed", true);
    }

    @Test
    public void testConstructorWithValues10() {
        logger.info("testConstructorWithValues");
        int count = 10;
        Collection<Person> people = Person.generatePeople(count);
        T mic = createMultiIndexContainer(people);
        Assert.assertTrue("There should be " + count + " people in MultiIndexContainerEnum, but only " + mic.size() + " to be present", count == mic.size());
    }

    /**
	 * Test for bulk data addition. Without any index
	 * <P>
	 * There is also measured time to add all people into multiindex main
	 * collection
	 */
    @Test
    public void testBulkData1M() {
        logger.info("testBulkData");
        int count = 1000000;
        Collection<Person> people = Person.generatePeople(count);
        TimeElapser te = new TimeElapser();
        T mic = createMultiIndexContainer(people);
        logger.info("Elapsed time to add " + count + " people is " + te.end() + " ms");
        Assert.assertTrue("There should be " + count + " people in MultiIndexContainerEnum, but only " + mic.size() + " to be present", count == mic.size());
    }

    /**
	 * Test for creating index for <code>birthYear</code> attribute
	 */
    @Test
    public void testIndexBirthYear100K() {
        logger.info("testIndexBirthYear");
        int count = 100000;
        Collection<Person> people = Person.generatePeople(count);
        TimeElapser te = new TimeElapser();
        T mic = createMultiIndexContainer(people);
        logger.info("Elapsed time to add " + count + " people is " + te.end() + " ms");
        te.start();
        addIndexForBirthYear(mic);
        logger.info("Elapsed time to create index birthYear for " + count + " people is " + te.end() + " ms");
        Assert.assertTrue("There should be " + count + " people in MultiIndexContainerEnum, but only " + mic.size() + " to be present", count == mic.size());
    }

    /**
	 * Test for creating index for <code>surname</code> attribute
	 */
    @Test
    public void testIndexSurname100K() {
        logger.info("testIndexSurname");
        int count = 100000;
        Collection<Person> people = Person.generatePeople(count);
        TimeElapser te = new TimeElapser();
        T mic = createMultiIndexContainer(people);
        logger.info("Elapsed time to add " + count + " people is " + te.end() + " ms");
        te.start();
        String attributeName = "surname";
        addIndexForSurname(mic);
        logger.info("Elapsed time to create index \"" + attributeName + "\" for " + count + " people is " + te.end() + " ms");
        Assert.assertTrue("There should be " + count + " people in MultiIndexContainerEnum, but only " + mic.size() + " to be present", count == mic.size());
    }

    /**
	 * Test for creating index first and then add data
	 */
    @Test
    public void testIndexBeforeData100K() {
        logger.info("testAheadIndex");
        int count = 100000;
        T mic = createMultiIndexContainer();
        logger.info("Created emtpy MultiIndexContainerEnum");
        addIndexForBirthYear(mic);
        logger.info("Established index for birthYear");
        addIndexForSurname(mic);
        logger.info("Established index for surname");
        Collection<Person> people = Person.generatePeople(count);
        TimeElapser te = new TimeElapser();
        mic.addAll(people);
        logger.info("Elapsed time to add " + count + " people is " + te.end() + " ms");
        Assert.assertTrue("There should be " + count + " people in MultiIndexContainerEnum, but only " + mic.size() + " to be present", count == mic.size());
    }

    /**
	 * Method to test find by index on birthYear column
	 */
    @Test
    public void testFindByIndex100K() {
        logger.info("testFindByIndex");
        int count = 100000;
        Collection<Person> people = Person.generatePeople(count);
        T mic = createMultiIndexContainer(people);
        logger.info("Created filled MultiIndexContainer");
        addIndexForBirthYear(mic);
        logger.info("Established index for birthYear");
        int birthYear = 1977;
        Collection<Person> peopleBorn1977 = findEqBirthYear(mic, birthYear);
        logger.info("There are " + peopleBorn1977.size() + " people born in " + birthYear);
        Assert.assertTrue("There should be at least one person born in " + birthYear, peopleBorn1977.size() > 0);
    }

    /**
	 * Method to test find by index on birthYear in multiple values
	 */
    @Test
    public void testFindByIndexWithMultipleValues100K() {
        logger.info("testFindByIndex");
        int count = 100000;
        Collection<Person> people = Person.generatePeople(count);
        T mic = createMultiIndexContainer(people);
        logger.info("Created emtpy MultiIndexContainerEnum");
        addIndexForBirthYear(mic);
        int birthYear1977 = 1977;
        int birthYear1978 = 1978;
        int birthYear1979 = 1979;
        Integer[] birthYears = { birthYear1977, birthYear1978, birthYear1979 };
        Collection<Person> peopleBorn1977_9 = findInBirthYear(mic, birthYears);
        logger.info("There are " + peopleBorn1977_9.size() + " people born in " + birthYear1977 + "," + birthYear1978 + "," + birthYear1979);
        Assert.assertTrue("There should be at least one person born in " + birthYear1977 + "," + birthYear1978 + "," + birthYear1979, peopleBorn1977_9.size() > 0);
    }

    /**
	 * Method to test find without index and with index possibility of
	 * MultiIndexContainerPureIndexes
	 */
    @Test
    public void testFindByWithAndWithoutIndex100K() {
        logger.info("testFindByWithAndWithoutIndex");
        int count = 1000000;
        Collection<Person> people = Person.generatePeople(count);
        T mic = createMultiIndexContainer(people);
        logger.info("Created empty MultiIndexContainerEnum");
        int birthYear = 1977;
        TimeElapser te = new TimeElapser();
        int countFound = 0;
        for (Person p : mic.getAll()) {
            if (p.getBirthYear() == birthYear) {
                countFound++;
            }
        }
        logger.info("There are " + countFound + " people born in " + birthYear);
        logger.info("Elapsed time to find people is " + te.end() + " ms");
        addIndexForBirthYear(mic);
        logger.info("Established index for birthYear");
        te.start();
        Collection<Person> peopleBorn1977 = findEqBirthYear(mic, birthYear);
        logger.info("There are " + peopleBorn1977.size() + " people born in " + birthYear);
        logger.info("Elapsed time to find people is " + te.end() + " ms");
        Assert.assertTrue("There should be at least one person born in " + birthYear, peopleBorn1977.size() > 0);
    }

    /**
	 * Test for creating index for <code>birthYear</code> attribute and greater
	 * values for specified year
	 */
    @Test
    public void testIndexBirthYearBetween100K() {
        logger.info("testIndexBirthYearBetween");
        int count = 1000000;
        Collection<Person> people = Person.generatePeople(count);
        TimeElapser te = new TimeElapser();
        T mic = createMultiIndexContainer(people);
        System.out.println("Elapsed time to add " + count + " people is " + te.end() + " ms");
        te.start();
        addIndexForBirthYear(mic);
        logger.info("Elapsed time to create index for birthYear for " + count + " people is " + te.end() + " ms");
        te.start();
        int minBirthYear = 1960;
        int maxBirthYear = 1980;
        ICriterion<Person, Integer, L> yearBetween = createBetweenBirthYear(mic, minBirthYear, maxBirthYear);
        Conjunction<Person, Integer, L> conjunction = mic.conjunction();
        conjunction.add(yearBetween);
        Collection<Person> personTests = mic.find(conjunction);
        logger.info("Found " + personTests.size() + " people for birthYear [" + minBirthYear + ", " + maxBirthYear + "]");
        logger.info("Elapsed time to find people " + te.end() + " ms");
        Assert.assertTrue("There should be found at least one person, but " + personTests.size() + " returned", personTests.size() > 0);
    }

    /**
	 * Test for creating index for <code>birthYear</code> attribute and greater
	 * values for specified year
	 */
    @Test
    public void testIndexBirthYearBetweenWithoutIndex1M() {
        logger.info("testIndexBirthYearBetweenWithoutIndex");
        int count = 1000000;
        Collection<Person> people = Person.generatePeople(count);
        TimeElapser te = new TimeElapser();
        T mic = createMultiIndexContainer(people);
        logger.info("Elapsed time to add " + count + " people is " + te.end() + " ms");
        te.start();
        int minBirthYear = 1960;
        int maxBirthYear = 1980;
        Collection<Person> personTests = new ArrayList<Person>();
        for (Person p : mic.getAll()) {
            if (p.getBirthYear() >= minBirthYear && p.getBirthYear() <= maxBirthYear) {
                personTests.add(p);
            }
        }
        logger.info("Found " + personTests.size() + " people for birthYear [" + minBirthYear + ", " + maxBirthYear + "]");
        logger.info("Elapsed time to find people " + te.end() + " ms");
        Assert.assertTrue("There should be found at least one person, but " + personTests.size() + " returned", personTests.size() > 0);
    }

    /**
	 * Test for creating index for <code>birthYear</code> attribute and greater
	 * values for specified year
	 */
    @Test
    public void testIndexBirthYearBetween1_1M() {
        logger.info("testIndexBirthYearBetween1");
        int count = 1000000;
        Collection<Person> people = Person.generatePeople(count);
        TimeElapser te = new TimeElapser();
        T mic = createMultiIndexContainer(people);
        logger.info("Elapsed time to add " + count + " people is " + te.end() + " ms");
        te.start();
        addIndexForBirthYear(mic);
        logger.info("Elapsed time to create index for birthDate for " + count + " people is " + te.end() + " ms");
        te.start();
        int minBirthYear = 1960;
        int maxBirthYear = 1980;
        Collection<Person> personTests = new ArrayList<Person>();
        for (int year = minBirthYear; year < maxBirthYear; year++) {
            personTests.addAll(findEqBirthYear(mic, year));
        }
        logger.info("Found " + personTests.size() + " people for birthYear [" + minBirthYear + "-" + maxBirthYear + "]");
        logger.info("Elapsed time to find people " + te.end() + " ms");
        Assert.assertTrue("There should be found at least one person, but " + personTests.size() + " returned", personTests.size() > 0);
    }

    /**
	 * Test to find people between specified birthYears and with specified sex
	 */
    @Test
    public void testIndexBirthYearAndSex1M() {
        logger.info("testIndexBirthYearAndSex");
        int count = 1000000;
        Collection<Person> people = Person.generatePeople(count);
        TimeElapser te = new TimeElapser();
        T mic = createMultiIndexContainer(people);
        System.out.println("Elapsed time to add " + count + " people is " + te.end() + " ms");
        te.start();
        addIndexForBirthYear(mic);
        logger.info("Elapsed time to create index for birthYear for " + count + " people is " + te.end() + " ms");
        te.start();
        addIndexForSex(mic);
        logger.info("Elapsed time to create index for sex  for " + count + " people is " + te.end() + " ms");
        te.start();
        int minBirthYear = 1960;
        int maxBirthYear = 1980;
        boolean shouldBeMan = true;
        Junction<Person, Integer, L> lookupRules = mic.conjunction().add(createBetweenBirthYear(mic, minBirthYear, maxBirthYear)).add(createEqSex(mic, shouldBeMan));
        Collection<Person> personTests = mic.find(lookupRules);
        logger.info("Found " + personTests.size() + " people for birthYear [" + minBirthYear + ", " + maxBirthYear + "] and" + " isMan=" + shouldBeMan);
        logger.info("Elapsed time to find people " + te.end() + " ms");
        Assert.assertTrue("There should be found at least one person, but " + personTests.size() + " returned", personTests.size() > 0);
    }

    /**
	 * Test to find people between specified birthYears and with specified sex
	 */
    @Test
    public void testIndexBirthYearAndSexWithoutIndex_1M() {
        logger.info("testIndexBirthYearAndSexWithoutIndex");
        int count = 1000000;
        Collection<Person> people = Person.generatePeople(count);
        TimeElapser te = new TimeElapser();
        T mic = createMultiIndexContainer(people);
        logger.info("Elapsed time to add " + count + " people is " + te.end() + " ms");
        te.start();
        int minBirthYear = 1960;
        int maxBirthYear = 1980;
        boolean shouldBeMan = true;
        Collection<Person> personTests = new ArrayList<Person>();
        for (Person p : mic.getAll()) {
            if (p.getBirthYear() >= minBirthYear && p.getBirthYear() < maxBirthYear && p.isMan() == shouldBeMan) {
                personTests.add(p);
            }
        }
        logger.info("Found " + personTests.size() + " people for birthYear [" + minBirthYear + ", " + maxBirthYear + "] and" + " isMan=" + shouldBeMan);
        logger.info("Elapsed time to find people " + te.end() + " ms");
        Assert.assertTrue("There should be found at least one person, but " + personTests.size() + " returned", personTests.size() > 0);
    }

    @Test
    public void testConcurrency() {
        logger.info("testConcurrency");
        int count = 1000;
        Collection<Person> people = Person.generatePeople(count);
        TimeElapser te = new TimeElapser();
        final T mic = createMultiIndexContainer(people);
        logger.info("Elapsed time to add " + count + " people is " + te.end() + " ms");
        te.start();
        addIndexForBirthYear(mic);
        logger.info("Elapsed time to create index for birthYear  for " + count + " people is " + te.end() + " ms");
        te.start();
        addIndexForSex(mic);
        logger.info("Elapsed time to create index for sex for " + count + " people is " + te.end() + " ms");
        te.start();
        int minBirthYear = 1960;
        int maxBirthYear = 1980;
        boolean shouldBeMan = true;
        final Junction<Person, Integer, L> lookupRules = mic.conjunction().add(createBetweenBirthYear(mic, minBirthYear, maxBirthYear)).add(createEqSex(mic, shouldBeMan));
        int howManyThreads = 100;
        final int howManyPeopleToAdd = 1000;
        Thread[] threads = new Thread[howManyThreads];
        for (int i = 0; i < howManyThreads; i++) {
            final int y = i;
            if (y % 2 == 0) {
                threads[y] = new Thread(new Runnable() {

                    public void run() {
                        logger.info("Thread " + y + " (writer) started");
                        mic.addAll(Person.generatePeople(howManyPeopleToAdd));
                        logger.info("Thread " + y + " (writer) " + howManyPeopleToAdd + " people added");
                        logger.info("Thread " + y + " (writer) ended");
                    }
                }, "Writer-" + y);
            } else {
                threads[y] = new Thread(new Runnable() {

                    public void run() {
                        logger.info("Thread " + y + " (reader) started");
                        logger.info("Thread " + y + " (reader) ... " + mic.find(lookupRules).size() + " people found");
                        logger.info("Thread " + y + " (reader) ended");
                    }
                }, "Reader-" + y);
            }
        }
        for (int i = 0; i < howManyThreads; i++) {
            threads[i].start();
        }
        logger.info("Threads finished");
        try {
            Thread.sleep(15 * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(true);
    }

    /**
	 * Test for creating index for <code>birthYear</code> attribute
	 */
    @Test
    public void testIndexBirthYearPureIndexes100K() {
        logger.info("testIndexBirthYearPureIndexes");
        int count = 100000;
        Collection<Person> people = Person.generatePeople(count);
        TimeElapser te = new TimeElapser();
        T mic = createMultiIndexContainer(people);
        logger.info("Elapsed time to add " + count + " people is " + te.end() + " ms");
        te.start();
        addIndexForBirthYear(mic);
        logger.info("Elapsed time to create index for birthYear  for " + count + " people is " + te.end() + " ms");
        int birthYear = 1977;
        Collection<Person> peopleBorn1977 = mic.find(createEqBirthYear(mic, birthYear));
        logger.info("There are " + peopleBorn1977.size() + " people born in " + birthYear);
        Assert.assertTrue("There should be " + count + " people in MultiIndexContainerEnum, but only " + mic.size() + " to be present", count == mic.size());
    }

    /**
	 * Test for finding records with null indexed value
	 */
    @Test
    public void testIndexWithNullValue100() {
        logger.info("testIndexWithNullValue");
        int count = 100;
        Collection<Person> people = Person.generatePeople(count);
        TimeElapser te = new TimeElapser();
        T mic = createMultiIndexContainer(people);
        logger.info("Elapsed time to add " + count + " people is " + te.end() + " ms");
        te.start();
        addIndexForBirthYear(mic);
        addIndexForSurname(mic);
        te.end();
        logger.info("Elapsed time to create indexes for birthYear and surname for " + count + " people is " + te.end() + " ms");
        int birthYear = 1950;
        Junction<Person, Integer, L> lookupRules = mic.conjunction().add(createEqBirthYear(mic, birthYear)).add(createIsNullSurname(mic));
        Collection<Person> born1950 = mic.find(lookupRules);
        logger.info("There are " + born1950.size() + " people born in " + birthYear + " and named null");
        Assert.assertTrue("There should be found at least 1 people in MultiIndexContainerEnum, but only " + born1950.size() + " found", born1950.size() > 0);
    }

    /**
	 * Test for finding records with null indexed value
	 */
    @Test
    public void testIndexWithNullValueForBirthDate_100() {
        logger.info("testIndexWithNullValueForBirthDate");
        int count = 100;
        Collection<Person> people = Person.generatePeople(count);
        for (Person p : people) {
            if (p.getBirthYear() != null && p.getBirthYear() < 1975) {
                p.setBirthYear(null);
            }
        }
        TimeElapser te = new TimeElapser();
        T mic = createMultiIndexContainer(people);
        logger.info("Elapsed time to add " + count + " people is " + te.end() + " ms");
        te.start();
        addIndexForBirthYear(mic);
        addIndexForSurname(mic);
        logger.info("Elapsed time to create index [" + PersonTest_Indexes.BIRTH_YEAR + ", " + PersonTest_Indexes.SURNAME + "]  for " + count + " people is " + te.end() + " ms");
        int birthYear = 1980;
        Junction<Person, Integer, L> lookupRules = mic.conjunction().add(createLTBirthYear(mic, birthYear)).add(createIsNullSurname(mic));
        Collection<Person> bornBefore1980 = mic.find(lookupRules);
        logger.info("There are " + bornBefore1980.size() + " people born before " + birthYear + " with surname null");
        Assert.assertTrue("There should be found at least 1 people in MultiIndexContainerEnum, but only " + bornBefore1980.size() + " found", bornBefore1980.size() > 0);
    }

    /**
	 * Test for finding records with null indexed value
	 */
    @Test
    public void testIndexNotNull() {
        logger.info("testIndexNotNull");
        int count = 100;
        Collection<Person> people = Person.generatePeople(count);
        for (Person p : people) {
            if (p.getBirthYear() != null && p.getBirthYear() < 1975) {
                p.setBirthYear(null);
            }
        }
        TimeElapser te = new TimeElapser();
        T mic = createMultiIndexContainer(people);
        logger.info("Elapsed time to add " + count + " people is " + te.end() + " ms");
        te.start();
        addIndexForBirthYear(mic);
        addIndexForSurname(mic);
        logger.info("Elapsed time to create indexes birthYear and surname  for " + count + " people is " + te.end() + " ms");
        int birthYear = 1980;
        Junction<Person, Integer, L> lookupRules = mic.conjunction().add(createLTBirthYear(mic, birthYear)).add(createIsNullSurname(mic));
        Collection<Person> knownBirthYear = mic.find(lookupRules);
        logger.info("There are " + knownBirthYear.size() + " people with knowh birth year ");
        Assert.assertTrue("There should be found at least 1 people in MultiIndexContainerEnum, but only " + knownBirthYear.size() + " found", knownBirthYear.size() > 0);
    }

    /**
	 * Testing for find underweight persons. They have BMI < 16
	 */
    @Test
    public void testFindByBMIIndex() {
        Collection<Person> people = Person.generatePeople(10000);
        T mic = createMultiIndexContainer(people);
        addIndexForBMI(mic);
        Collection<Person> underweightPersons = mic.find(createLTBMI(mic, 16d));
        logger.info("There are " + underweightPersons.size() + " underweight people ");
        Assert.assertTrue("There should be found at least 1 underweigt person found, but only " + underweightPersons.size() + " found", underweightPersons.size() > 0);
    }
}
