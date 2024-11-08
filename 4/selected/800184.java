package system.container;

import java.io.File;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import system.log.LogMessage;
import static org.junit.Assert.*;

/**
 *  Rules of the testing game:
 *
 *      - If you make changes, add another entry @author below the previous one
 *      - Describe the purpose of the test case and which tests will be done
 *      - Split each group of tests onto its own method and use intuitive names
 *      - Test if something works as intended and also how it reacts to errors
 *      - Add System.out.println comments when:
 *          - A specific test is starting
 *          - A test has finished
 *      - Add an empty line of text between each test to keep results readable
 *      - Be verbose, explain to people what you are doing but keep it simple
 *      - Ensure you test with a clean environment, clean up your mess when done
 *
 *                                          - Thank you.
 *
 * @author Nuno Brito, 30th June 2011 in Darmstadt, Germany.
 */
public class ContainerFlatFileTest {

    String[] fields = new String[] { "uid", "time_created", "unique_key", "update", "author" };

    String id = "test";

    static String rootTestFolder = "testStorage";

    static ContainerFlatFile container;

    public ContainerFlatFileTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("Testing the Flat File Container");
        System.out.println(" Deleting the work folder");
        File file = new File(rootTestFolder);
        utils.files.deleteDir(file);
        if (file.exists()) fail("Failed to delete the work folder");
        System.out.println(" ..Done");
        utils.time.wait(1);
        System.out.println(" Creating the work folder");
        utils.files.mkdirs(file);
        if (file.exists() == false) fail("Failed to create the work folder");
        System.out.println(" ..Done");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        System.out.println("All tests completed!");
    }

    @Test
    public void initializationTest() {
        System.out.println(" Test instantiating the container");
        LogMessage result = new LogMessage();
        File rootFolder = new File(rootTestFolder);
        container = new ContainerFlatFile(id, fields, rootFolder, result);
        System.out.println("  " + result.getRecent());
        System.out.println("  ..Done!");
    }

    @Test
    public void initialWriteReadTest() {
        System.out.println(" Test writing");
        container.write(new String[] { "1", "A", "3", "4", "5" });
        container.write(new String[] { "2", "B", "3", "4", "5" });
        container.write(new String[] { "3", "C", "3", "4", "5" });
        container.write(new String[] { "4", "D", "3", "4", "5" });
        container.write(new String[] { "5", "E", "3", "4", "5" });
        System.out.println(" ..Done!");
        System.out.println(" Test counting");
        long count = container.count();
        if (count != 5) fail("We have " + count + " instead of the expected record " + "count");
        System.out.println(" ..Done!");
        System.out.println("  Test reading after write");
        String[] record = container.read("4");
        String out = record[1];
        assertEquals(out, "D");
        System.out.println("  ..Done!");
        System.out.println(" Test overwriting");
        container.write(new String[] { "1", "A", "AA", "4", "5" });
        container.write(new String[] { "2", "B", "BB", "4", "5" });
        container.write(new String[] { "3", "C", "CC", "4", "5" });
        container.write(new String[] { "4", "D", "XX", "4", "5" });
        container.write(new String[] { "5", "E", "ZZ", "4", "5" });
        System.out.println(" ..Done!");
        System.out.println("  Test reading after overwrite");
        record = container.read("2");
        out = record[2];
        assertEquals(out, "BB");
        System.out.println("  ..Done!");
    }

    @Test
    public void writeToLimitTest() {
        System.out.println(" Test the limits of storage for each file");
        long max = container.getMaxRecordsAllowed() * 3;
        System.out.println("  Creating " + max + " records..");
        long count = 6;
        long timeBegin = System.currentTimeMillis();
        long timePrevious = timeBegin;
        for (int i = 6; i < max + 1; i++) {
            if (count == container.getMaxRecordsAllowed()) {
                long timeResult = System.currentTimeMillis() - timePrevious;
                timePrevious = System.currentTimeMillis();
                String timeCount = utils.time.timeNumberToHumanReadable(timeResult);
                System.out.println("   " + i + " records and " + timeCount + " per file." + " (" + (i * 100) / max + "% processed in " + utils.time.timeNumberToHumanReadable(System.currentTimeMillis() - timeBegin) + ")");
                count = 0;
            }
            count++;
            container.write(new String[] { "" + i, "A", "AA", "4", "5" });
        }
        System.out.println("  ..Done!");
        long timeEnd = System.currentTimeMillis();
        long timeResult = timeEnd - timeBegin;
        String timeCount = utils.time.timeNumberToHumanReadable(timeResult);
        System.out.println("  Write operation took " + timeCount + " to write " + max + " records");
        long timeToGoal = (timeResult * 100000) / max;
        System.out.println("  Comparing to objective: \n" + "    " + utils.time.timeNumberToHumanReadable(timeToGoal) + " to write 100 000 records");
        System.out.println("  Testing number of created records");
        System.out.println("  Created " + container.count() + " records");
        assertEquals(container.count(), max);
        System.out.println("  ..Done!");
    }

    @Test
    public void deleteTest() {
        long initialCount = container.count();
        System.out.println(" Test deleting some records and see the result");
        container.delete("uid", "1");
        container.delete("uid", "3");
        container.delete("uid", "5");
        container.delete("uid", "7");
        container.delete("uid", "9");
        System.out.println(" ..Done!");
        System.out.println("  Testing number of deleted records");
        System.out.println("  Counting " + container.count() + " records");
        assertEquals(container.count(), initialCount - 5);
        System.out.println("  ..Done!");
        System.out.println("  Test deleting all records and files");
        container.deleteKnowledgeFiles();
        System.out.println("  ..Done!");
    }
}
