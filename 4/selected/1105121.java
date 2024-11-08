package eu.planets_project.services.utils;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import eu.planets_project.services.datatypes.DigitalObject;

/**
 * @author melmsp
 *
 */
public class DigitalObjectUtilsTest {

    static File testFolder = new File("tests/test-files/documents/test_pdf");

    File testZip = new File("tests/test-files/archives/test_pdf.zip");

    File removeZip = new File("tests/test-files/archives/insertFragmentTest.zip");

    File work_folder = FileUtils.createWorkFolderInSysTemp("DigitalObjectUtilsTest_TMP".toUpperCase());

    @Test
    public void testCreateZipTypeDigObFolder() {
        printTestTitle("Test createZipTypeDigOb() from FOLDER");
        DigitalObject result = DigitalObjectUtils.createZipTypeDigitalObject(testFolder, testFolder.getName(), true, false, true);
        assertTrue("DigitalObject should NOT be NULL!", result != null);
        printDigOb(result);
        File resultFile = new File(work_folder, result.getTitle());
        FileUtils.writeInputStreamToFile(result.getContent().read(), resultFile);
        System.out.println("Result size: " + resultFile.length());
    }

    @Test
    public void testCreateZipTypeDigObZip() {
        printTestTitle("Test createZipTypeDigOb() from ZIP file");
        DigitalObject result = DigitalObjectUtils.createZipTypeDigitalObject(testZip, testFolder.getName(), true, false, true);
        assertTrue("DigitalObject should NOT be NULL!", result != null);
        printDigOb(result);
    }

    @Test
    public void testGetFragmentFromZipTypeDigitalObject() {
        printTestTitle("Test getFragmentFromZipTypeDigitalObject()");
        DigitalObject result = DigitalObjectUtils.createZipTypeDigitalObject(testFolder, "getFragmentTest.zip", false, false, true);
        List<String> fragments = result.getFragments();
        DigitalObject fragmentDigOb = null;
        Random random = new Random();
        int index = random.nextInt(fragments.size());
        System.err.println("Getting file: " + fragments.get(index));
        fragmentDigOb = DigitalObjectUtils.getFragment(result, fragments.get(index), false);
        printDigOb(fragmentDigOb);
    }

    @Test
    public void testInsertFragmentIntoZipTypeDigitalObject() {
        printTestTitle("Test insertFragmentIntoZipTypeDigitalObject()");
        DigitalObject result = DigitalObjectUtils.createZipTypeDigitalObject(testFolder, "insertFragmentTest.zip", false, false, true);
        List<String> fragments = result.getFragments();
        DigitalObject insertionResult = null;
        Random random = new Random();
        int index = random.nextInt(fragments.size());
        System.err.println("Getting file: " + fragments.get(index));
        File toInsert = new File("IF/common/src/test/resources/test_zip/images/test_gif/laptop.gif");
        insertionResult = DigitalObjectUtils.insertFragment(result, toInsert, new String("insertedFiles\\images\\" + toInsert.getName()), false);
        printDigOb(insertionResult);
        insertionResult = DigitalObjectUtils.insertFragment(insertionResult, toInsert, new String("insertedFiles\\images\\" + toInsert.getName()), false);
        printDigOb(insertionResult);
    }

    @Test
    public void testRemoveFragmentFromZipTypeDigitalObject() {
        printTestTitle("Test removeFragmentFromZipTypeDigitalObject()");
        DigitalObject result = DigitalObjectUtils.createZipTypeDigitalObject(removeZip, "removeFragmentTest.zip", false, false, true);
        printDigOb(result);
        DigitalObject removeResult = DigitalObjectUtils.removeFragment(result, new String("insertedFiles\\images\\laptop.gif"), false);
        FileUtils.writeInputStreamToFile(removeResult.getContent().read(), new File(work_folder, removeResult.getTitle()));
        printDigOb(removeResult);
    }

    private void printTestTitle(String title) {
        for (int i = 0; i < title.length() + 4; i++) {
            System.out.print("*");
        }
        System.out.println();
        System.out.println("* " + title + " *");
        for (int i = 0; i < title.length() + 4; i++) {
            System.out.print("*");
        }
        System.out.println();
    }

    private String tabulator(int level) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i <= (level * 4); i++) {
            buf.append(" ");
        }
        return buf.toString();
    }

    private void printFragments(DigitalObject digOb) {
        List<String> fragments = digOb.getFragments();
        int i = 1;
        for (String fragment : fragments) {
            System.out.println(tabulator(1) + i + ") " + fragment);
            i++;
        }
        System.out.println(tabulator(1) + "total count: " + fragments.size());
    }

    private void printDigOb(DigitalObject digOb) {
        List<DigitalObject> contained = null;
        System.out.println("--------------------------------------");
        System.out.println("Summary DigitalObject: " + digOb.getTitle());
        System.out.println("--------------------------------------");
        if (DigitalObjectUtils.isZipType(digOb)) {
            System.out.println("Contains Fragments: " + digOb.getFragments().size());
            printFragments(digOb);
        }
    }
}
