package jgnash.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import org.junit.Test;

/**
 * File utilities test
 *
 * @author Craig Cavanaugh
 * @version $Id: FileUtilsTest.java 3051 2012-01-02 11:27:23Z ccavanaugh $
 */
public class FileUtilsTest {

    @Test
    public void fileExtensionStripTest() {
        assertTrue(FileUtils.fileHasExtension("text.txt"));
        assertTrue(FileUtils.fileHasExtension("text.txt.txt"));
        assertFalse(FileUtils.fileHasExtension("test"));
    }

    @Test
    public void fileExtensionText() {
        assertEquals(FileUtils.getFileExtension("test.txt"), "txt");
    }

    @Test
    public void fileCopyToSelf() throws IOException {
        File tempfile = File.createTempFile("jgnash-test", "jdb");
        String absolutepath = tempfile.getAbsolutePath();
        String testdata = "42";
        writeTestData(testdata, tempfile);
        checkTestData(testdata, absolutepath);
        assertFalse(FileUtils.copyFile(new File(absolutepath), new File(absolutepath)));
    }

    @Test
    public void fileCopy() throws IOException {
        File tempfile = File.createTempFile("jgnash-test", "jdb");
        String absolutepath = tempfile.getAbsolutePath();
        String testdata = "42";
        writeTestData(testdata, tempfile);
        checkTestData(testdata, absolutepath);
        File secondTempFile = File.createTempFile("jgnash-test", "jdb");
        if (FileUtils.copyFile(tempfile, secondTempFile)) {
            checkTestData(testdata, secondTempFile.getAbsolutePath());
        }
    }

    private void checkTestData(String testdata, String absolutepath) throws IOException {
        char[] buffer = new char[testdata.length()];
        new InputStreamReader(new FileInputStream(absolutepath)).read(buffer);
        assertEquals(testdata, new String(buffer));
    }

    private void writeTestData(String testdata, File tempfile) throws IOException {
        OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(tempfile));
        os.write(testdata);
        os.close();
    }
}
