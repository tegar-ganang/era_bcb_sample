package org.happy.commons.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

public class FileUtils_1x0Test {

    @Before
    public void setup() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, InterruptedException {
        JUnitFileHelper.setUp();
    }

    @Test
    public void testWriteToFileDataHandlerFile() throws IOException, InterruptedException {
        File fileA = JUnitFileHelper.getFileA();
        DataHandler handler = new DataHandler(new FileDataSource(fileA));
        File fileB = JUnitFileHelper.getFileB();
        FileUtils_1x0.writeToFile(handler, fileB, false);
        assertEquals(fileA.length(), fileB.length());
        assertTrue(FileUtils_1x0.isFileContentsEquals(fileA, fileB));
    }

    /**
	 * inputStreams of both files have the same content
	 * @throws IOException
	 */
    @Test
    public void testInputStreamContentEquals() throws IOException {
        File file1 = JUnitFileHelper.getFileA();
        File file2 = JUnitFileHelper.getFileB();
        FileUtils.copyFile(file1, file2);
        FileInputStream is1 = new FileInputStream(file1);
        FileInputStream is2 = new FileInputStream(file2);
        assertTrue(FileUtils_1x0.isInputStreamContentEquals(is1, is2));
        is1.close();
        is2.close();
    }

    /**
	 * inputStreams of both files have the same content
	 * @throws IOException
	 */
    @Test
    public void testInputStreamContentDifferent() throws IOException {
        File file1 = JUnitFileHelper.getFileA();
        File file2 = JUnitFileHelper.getFileC();
        FileInputStream is1 = new FileInputStream(file1);
        FileInputStream is2 = new FileInputStream(file2);
        assertFalse(FileUtils_1x0.isInputStreamContentEquals(is1, is2));
        is1.close();
        is2.close();
    }

    /**
	 * both fiels are equals
	 * @throws IOException
	 */
    @Test
    public void testFileContentEquals() throws IOException {
        File file1 = JUnitFileHelper.getFileA();
        File file2 = JUnitFileHelper.getFileB();
        FileUtils.copyFile(file1, file2);
        assertTrue(FileUtils_1x0.isFileContentsEquals(file1, file2));
    }

    /**
	 * both fiels are different
	 * @throws IOException
	 */
    @Test
    public void testFileContentDifferent() throws IOException {
        File file1 = JUnitFileHelper.getFileA();
        File file2 = JUnitFileHelper.getFileC();
        assertFalse(FileUtils_1x0.isFileContentsEquals(file1, file2));
    }

    /**
	 * both fiels are equals
	 * @throws IOException
	 */
    @Test
    public void testFileContentEqualsStringString() throws IOException {
        File file1 = JUnitFileHelper.getFileA();
        File file2 = JUnitFileHelper.getFileB();
        FileUtils.copyFile(file1, file2);
        assertTrue(FileUtils_1x0.isFileContentsEquals(file1.getCanonicalPath(), file2.getCanonicalPath()));
    }

    /**
	 * both fiels are different
	 * @throws IOException
	 */
    @Test
    public void testFileContentDifferentStringString() throws IOException {
        File file1 = JUnitFileHelper.getFileA();
        File file2 = JUnitFileHelper.getFileC();
        assertFalse(FileUtils_1x0.isFileContentsEquals(file1.getCanonicalPath(), file2.getCanonicalPath()));
    }

    @Test
    public void testCreateChildFile() throws IOException {
        fail();
    }

    @Test
    public void testWriteFileToFile() throws IOException {
        fail();
    }

    @Test
    public void testWriteBytesToFile() throws IOException {
        fail();
    }
}
