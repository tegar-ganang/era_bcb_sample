package org.progeeks.util;

import java.io.*;
import junit.framework.TestCase;

/**
 * Tests of the FileUtils class.
 *
 * @version		$Revision: 1.2 $
 * @author		Paul Wisneskey
 */
public class FileUtilsTests extends TestCase {

    protected static final String TEST_STRING = "This is a test string for the FileUtils test suite.";

    protected static final long TEST_CRC32 = 0x1120653c;

    protected static final String TEST_TEMP_FILE_PREFIX = "FileUtilsTests";

    protected static final String TEST_TEMP_FILE_SUFFIX = "txt";

    public void testConstructor() {
        FileUtils utils = new FileUtils();
    }

    public void testCopyFile() throws IOException {
        File testFile = null;
        File destFile = null;
        try {
            testFile = createTestFile();
            verifyTestFile(testFile);
            destFile = File.createTempFile(TEST_TEMP_FILE_PREFIX, TEST_TEMP_FILE_SUFFIX);
            FileUtils.copyFile(testFile, destFile);
            verifyTestFile(destFile);
        } finally {
            if (testFile != null) testFile.delete();
            if (destFile != null) destFile.delete();
        }
    }

    public void testCopyFileProgressReporter() throws IOException {
        File testFile = null;
        File destFile = null;
        try {
            testFile = createTestFile();
            verifyTestFile(testFile);
            destFile = File.createTempFile(TEST_TEMP_FILE_PREFIX, TEST_TEMP_FILE_SUFFIX);
            DefaultProgressReporter reporter = new DefaultProgressReporter();
            FileUtils.copyFile(testFile, destFile, reporter);
            verifyTestFile(destFile);
            assertFalse(reporter.isCanceled());
        } finally {
            if (testFile != null) testFile.delete();
            if (destFile != null) destFile.delete();
        }
    }

    public void testSaveStream() throws IOException {
        File testFile = null;
        File destFile = null;
        try {
            testFile = createTestFile();
            verifyTestFile(testFile);
            FileInputStream inputStream = new FileInputStream(testFile);
            destFile = File.createTempFile(TEST_TEMP_FILE_PREFIX, TEST_TEMP_FILE_SUFFIX);
            DefaultProgressReporter reporter = new DefaultProgressReporter();
            FileUtils.saveStream(destFile, inputStream);
            verifyTestFile(destFile);
            assertFalse(reporter.isCanceled());
        } finally {
            if (testFile != null) testFile.delete();
            if (destFile != null) destFile.delete();
        }
    }

    public void testGetCrcStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(TEST_STRING.getBytes());
        assertEquals(TEST_CRC32, FileUtils.getCrc(stream));
    }

    public void testGetCrcFile() throws IOException {
        File testFile = null;
        try {
            testFile = createTestFile();
            assertEquals(TEST_CRC32, FileUtils.getCrc(testFile));
        } finally {
            if (testFile != null) testFile.delete();
        }
    }

    protected File createTestFile() {
        File file = null;
        try {
            file = File.createTempFile(TEST_TEMP_FILE_PREFIX, TEST_TEMP_FILE_SUFFIX);
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter writer = new BufferedWriter(fileWriter);
            writer.write(TEST_STRING);
            writer.close();
            fileWriter.close();
        } catch (IOException e) {
            fail("Failed to create test file.");
        }
        return file;
    }

    protected void verifyTestFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuffer read = new StringBuffer();
            String line = reader.readLine();
            assertEquals("Incorrect value found in test file.", TEST_STRING, line);
            reader.close();
        } catch (IOException e) {
            fail("Failed to read test file.");
        }
    }
}
