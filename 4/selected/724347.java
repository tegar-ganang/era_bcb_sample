package org.progeeks.util;

import java.io.*;
import java.io.IOException;
import junit.framework.TestCase;

/**
 * Tests of the StringUtils class.
 *
 * @version		$Revision: 1.1 $
 * @author		Paul Wisneskey
 */
public class StringUtilsTests extends TestCase {

    protected static final String TEST_RESOURCE = "stringutils-test-data.txt";

    public void testConstructor() {
        StringUtils utils = new StringUtils();
    }

    public void testReadStringResource() {
        String string = null;
        try {
            string = StringUtils.readStringResource(this.getClass(), TEST_RESOURCE);
        } catch (IOException e) {
            fail("Unexpected exception reading resource.");
        }
        assertEquals("This is test data for StringUtilsTests.  Do not edit!", string);
    }

    public void testReadStringResourceUnknownResource() {
        boolean caughtException = false;
        try {
            String string = StringUtils.readStringResource(this.getClass(), "BogusResource.txt");
        } catch (Exception e) {
            caughtException = true;
        }
        assertTrue("Failed to catch expected exception.", caughtException);
    }

    public void testReadString() {
        StringReader reader = new StringReader("This is a test string.");
        String string = null;
        try {
            string = StringUtils.readString(reader);
        } catch (IOException e) {
            fail("Unexpected exception reading string.");
        }
        assertEquals("This is a test string.", string);
    }

    public void testReadWriteFile() {
        File file = null;
        try {
            String string = "This is a test string to write to a file.";
            file = File.createTempFile("stringutils-test", "tst");
            StringUtils.writeFile(string, file);
            String result = StringUtils.readFile(file);
            assertEquals(string, result);
        } catch (IOException e) {
            fail("Unexpected exception testing read/write file.");
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    public void testWriteString() {
        StringWriter writer = new StringWriter();
        String result = null;
        try {
            StringUtils.writeString("This is another test string.", writer);
        } catch (IOException e) {
            fail("Unexpected exception testing read/write file.");
        }
        assertEquals("This is another test string.", writer.getBuffer().toString());
    }
}
