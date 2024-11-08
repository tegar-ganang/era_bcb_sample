package fr.sonictools.jgrisbicatcleaner;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import fr.sonictools.jgrisbicatcleaner.business.auto_generated.grisbi06.Grisbi;
import fr.sonictools.jgrisbicatcleaner.model.GrisbiFileManager;
import fr.sonictools.jgrisbicatcleaner.tool.FileUtils;

public class MainTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
	 * Ensure the Main class works fine
	 * 
	 * @throws IOException
	 */
    @Test
    public void testMain() throws IOException {
        String copyInputFilePath = "./resources/test_copyInput.gsb";
        File copyInputFile = new File(copyInputFilePath);
        FileUtils.copyFile(new File(JgrisbicatcleanerSuite.inputGrisbiFilePath), copyInputFile);
        String args[] = { copyInputFilePath };
        Main.main(args);
        Grisbi expectedGrisbiObj = new GrisbiFileManager(JgrisbicatcleanerSuite.outputExpectedOnlyNotAssociatedOperationsGrisbiFilePath).getGrisbiFileObj();
        GrisbiFileManager updatedGrisbiManager = new GrisbiFileManager(copyInputFilePath);
        Grisbi updatedGrisbiObj = updatedGrisbiManager.getGrisbiFileObj();
        JgrisbicatcleanerSuite.compareGrisbiObjects(expectedGrisbiObj, updatedGrisbiObj);
        assertEquals(true, FileUtils.compareFiles(new File(JgrisbicatcleanerSuite.inputGrisbiFilePath), new File(updatedGrisbiManager.getBackupGrisbiFilePath())));
    }

    /**
	 * Ensure the checkArguments detect the no argument has been set
	 */
    @Test
    public void testCheckNoArgument() {
        boolean expectedResult = false;
        boolean result;
        String args[] = {};
        result = Main.checkArguments(args);
        assertEquals(expectedResult, result);
    }

    /**
	 * Ensure the checkArguments works when the argument is correct
	 */
    @Test
    public void testCheckOneArgumentOK() {
        boolean expectedResult = true;
        boolean result;
        String args[] = { JgrisbicatcleanerSuite.inputGrisbiFilePath };
        result = Main.checkArguments(args);
        assertEquals(expectedResult, result);
    }

    /**
	 * Ensure the checkArguments works even if more argument than the requested
	 * one is set. (obviously the expected one is correct.)
	 */
    @Test
    public void testCheckMoreArgument() {
        boolean expectedResult = true;
        boolean result;
        String args[] = { JgrisbicatcleanerSuite.inputGrisbiFilePath, "other input value" };
        result = Main.checkArguments(args);
        assertEquals(expectedResult, result);
    }

    /**
	 * Ensure the checkArguments fails if the input argument is wrong
	 */
    @Test
    public void testCheckOneArgumentKO() {
        boolean expectedResult = false;
        boolean result;
        String args[] = { "wrong_input_file.gsb" };
        result = Main.checkArguments(args);
        assertEquals(expectedResult, result);
    }
}
