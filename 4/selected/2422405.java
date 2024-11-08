package tests.api.java.io;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.StringWriter;
import tests.support.Support_ASimpleWriter;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

@TestTargetClass(CharArrayWriter.class)
public class CharArrayWriterTest extends junit.framework.TestCase {

    char[] hw = { 'H', 'e', 'l', 'l', 'o', 'W', 'o', 'r', 'l', 'd' };

    CharArrayWriter cw;

    CharArrayReader cr;

    /**
     * @tests java.io.CharArrayWriter#CharArrayWriter(int)
     */
    @TestTargetNew(level = TestLevel.COMPLETE, method = "CharArrayWriter", args = { int.class })
    public void test_ConstructorI() {
        cw = new CharArrayWriter(90);
        assertEquals("Test 1: Incorrect writer created.", 0, cw.size());
        try {
            cw = new CharArrayWriter(-1);
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * @tests java.io.CharArrayWriter#CharArrayWriter()
     */
    @TestTargetNew(level = TestLevel.COMPLETE, notes = "Verifies CharArrayWriter() method.", method = "CharArrayWriter", args = {  })
    public void test_Constructor() {
        cw = new CharArrayWriter();
        assertEquals("Created incorrect writer", 0, cw.size());
    }

    /**
     * @tests java.io.CharArrayWriter#close()
     */
    @TestTargetNew(level = TestLevel.COMPLETE, notes = "Verifies close() method.", method = "close", args = {  })
    public void test_close() {
        cw.close();
    }

    /**
     * @tests java.io.CharArrayWriter#flush()
     */
    @TestTargetNew(level = TestLevel.COMPLETE, notes = "Verifies flush() method.", method = "flush", args = {  })
    public void test_flush() {
        cw.flush();
    }

    /**
     * @tests java.io.CharArrayWriter#reset()
     */
    @TestTargetNew(level = TestLevel.COMPLETE, notes = "Verifies reset() method.", method = "reset", args = {  })
    public void test_reset() {
        cw.write("HelloWorld", 5, 5);
        cw.reset();
        cw.write("HelloWorld", 0, 5);
        cr = new CharArrayReader(cw.toCharArray());
        try {
            char[] c = new char[100];
            cr.read(c, 0, 5);
            assertEquals("Reset failed to reset buffer", "Hello", new String(c, 0, 5));
        } catch (IOException e) {
            fail("Exception during reset test : " + e.getMessage());
        }
    }

    /**
     * @tests java.io.CharArrayWriter#size()
     */
    @TestTargetNew(level = TestLevel.COMPLETE, notes = "Verifies size() method.", method = "size", args = {  })
    public void test_size() {
        assertEquals("Returned incorrect size", 0, cw.size());
        cw.write(hw, 5, 5);
        assertEquals("Returned incorrect size", 5, cw.size());
    }

    /**
     * @tests java.io.CharArrayWriter#toCharArray()
     */
    @TestTargetNew(level = TestLevel.COMPLETE, notes = "Verifies toCharArray() method.", method = "toCharArray", args = {  })
    public void test_toCharArray() {
        cw.write("HelloWorld", 0, 10);
        cr = new CharArrayReader(cw.toCharArray());
        try {
            char[] c = new char[100];
            cr.read(c, 0, 10);
            assertEquals("toCharArray failed to return correct array", "HelloWorld", new String(c, 0, 10));
        } catch (IOException e) {
            fail("Exception during toCharArray test : " + e.getMessage());
        }
    }

    /**
     * @tests java.io.CharArrayWriter#toString()
     */
    @TestTargetNew(level = TestLevel.COMPLETE, notes = "Verifies toString() method.", method = "toString", args = {  })
    public void test_toString() {
        cw.write("HelloWorld", 5, 5);
        cr = new CharArrayReader(cw.toCharArray());
        assertEquals("Returned incorrect string", "World", cw.toString());
    }

    /**
     * @tests java.io.CharArrayWriter#write(char[], int, int)
     */
    @TestTargetNew(level = TestLevel.PARTIAL_COMPLETE, method = "write", args = { char[].class, int.class, int.class })
    public void test_write$CII() {
        cw.write(hw, 5, 5);
        cr = new CharArrayReader(cw.toCharArray());
        try {
            char[] c = new char[100];
            cr.read(c, 0, 5);
            assertEquals("Writer failed to write correct chars", "World", new String(c, 0, 5));
        } catch (IOException e) {
            fail("Exception during write test : " + e.getMessage());
        }
    }

    /**
     * @tests java.io.CharArrayWriter#write(char[], int, int)
     * Regression for HARMONY-387
     */
    @TestTargetNew(level = TestLevel.PARTIAL_COMPLETE, notes = "Illegal argument checks.", method = "write", args = { char[].class, int.class, int.class })
    public void test_write$CII_Exception() {
        char[] target = new char[10];
        cw = new CharArrayWriter();
        try {
            cw.write(target, -1, 1);
            fail("Test 1: IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            cw.write(target, 0, -1);
            fail("Test 2: IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            cw.write(target, 1, target.length);
            fail("Test 3: IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            cw.write((char[]) null, 1, 1);
            fail("Test 4: NullPointerException expected.");
        } catch (NullPointerException e) {
        }
    }

    /**
     * @tests java.io.CharArrayWriter#write(int)
     */
    @TestTargetNew(level = TestLevel.COMPLETE, notes = "Verifies write(int) method.", method = "write", args = { int.class })
    public void test_writeI() {
        cw.write('T');
        cr = new CharArrayReader(cw.toCharArray());
        try {
            assertEquals("Writer failed to write char", 'T', cr.read());
        } catch (IOException e) {
            fail("Exception during write test : " + e.getMessage());
        }
    }

    /**
     * @tests java.io.CharArrayWriter#write(java.lang.String, int, int)
     */
    @TestTargetNew(level = TestLevel.COMPLETE, notes = "Verifies write(java.lang.String, int, int) method. [Need to check different strings?]", method = "write", args = { java.lang.String.class, int.class, int.class })
    public void test_writeLjava_lang_StringII() {
        cw.write("HelloWorld", 5, 5);
        cr = new CharArrayReader(cw.toCharArray());
        try {
            char[] c = new char[100];
            cr.read(c, 0, 5);
            assertEquals("Writer failed to write correct chars", "World", new String(c, 0, 5));
        } catch (IOException e) {
            fail("Exception during write test : " + e.getMessage());
        }
    }

    /**
     * @tests java.io.CharArrayWriter#write(java.lang.String, int, int)
     * Regression for HARMONY-387
     */
    @TestTargetNew(level = TestLevel.PARTIAL, notes = "Regression for write(java.lang.String, int, int) method.", method = "write", args = { java.lang.String.class, int.class, int.class })
    public void test_writeLjava_lang_StringII_2() throws StringIndexOutOfBoundsException {
        CharArrayWriter obj = new CharArrayWriter();
        try {
            obj.write((String) null, -1, 0);
            fail("NullPointerException expected");
        } catch (NullPointerException t) {
        }
    }

    /**
     * @tests java.io.CharArrayWriter#writeTo(java.io.Writer)
     */
    @TestTargetNew(level = TestLevel.COMPLETE, method = "writeTo", args = { java.io.Writer.class })
    public void test_writeToLjava_io_Writer() {
        Support_ASimpleWriter ssw = new Support_ASimpleWriter(true);
        cw.write("HelloWorld", 0, 10);
        StringWriter sw = new StringWriter();
        try {
            cw.writeTo(sw);
            assertEquals("Test 1: Writer failed to write correct chars;", "HelloWorld", sw.toString());
        } catch (IOException e) {
            fail("Exception during writeTo test : " + e.getMessage());
        }
        try {
            cw.writeTo(ssw);
            fail("Test 2: IOException expected.");
        } catch (IOException e) {
        }
    }

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    protected void setUp() {
        cw = new CharArrayWriter();
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    protected void tearDown() {
        if (cr != null) cr.close();
        cw.close();
    }

    /**
     * @tests java.io.CharArrayWriter#append(char)
     */
    @TestTargetNew(level = TestLevel.COMPLETE, notes = "Verifies append(char c) method.", method = "append", args = { char.class })
    public void test_appendChar() throws IOException {
        char testChar = ' ';
        CharArrayWriter writer = new CharArrayWriter(10);
        writer.append(testChar);
        writer.flush();
        assertEquals(String.valueOf(testChar), writer.toString());
        writer.close();
    }

    /**
     * @tests java.io.CharArrayWriter#append(CharSequence)
     */
    @TestTargetNew(level = TestLevel.COMPLETE, notes = "Verifies append(CharSequence csq) method.", method = "append", args = { java.lang.CharSequence.class })
    public void test_appendCharSequence() {
        String testString = "My Test String";
        CharArrayWriter writer = new CharArrayWriter(10);
        writer.append(testString);
        writer.flush();
        assertEquals(testString, writer.toString());
        writer.close();
    }

    /**
     * @tests java.io.CharArrayWriter#append(CharSequence, int, int)
     */
    @TestTargetNew(level = TestLevel.COMPLETE, method = "append", args = { java.lang.CharSequence.class, int.class, int.class })
    public void test_appendLjava_langCharSequenceII() {
        String testString = "My Test String";
        CharArrayWriter writer = new CharArrayWriter(10);
        try {
            writer.append(testString, -1, 0);
            fail("Test 1: IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            writer.append(testString, 0, -1);
            fail("Test 2: IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            writer.append(testString, 1, 0);
            fail("Test 3: IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            writer.append(testString, 1, testString.length() + 1);
            fail("Test 4: IndexOutOfBoundsException expected.");
        } catch (IndexOutOfBoundsException e) {
        }
        writer.append(testString, 1, 3);
        writer.flush();
        assertEquals("Test 5: Appending failed;", testString.substring(1, 3), writer.toString());
        writer.close();
    }
}
