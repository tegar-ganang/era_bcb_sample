package net.sourceforge.jaulp.io;

import java.io.File;
import java.util.Date;
import net.sourceforge.jaulp.date.DateUtils;
import net.sourceforge.jaulp.file.FileTestCase;

/**
 * Test class for the class SerializedObjectUtils.
 *
 * @version 1.0
 * @author Asterios Raptis
 */
public class SerializedObjectUtilsTest extends FileTestCase {

    /**
     * Sets the up.
     *
     * @throws Exception the exception
     * {@inheritDoc}
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Tear down.
     *
     * @throws Exception the exception
     * {@inheritDoc}
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for.
     *
     * {@link net.sourceforge.jaulp.io.SerializedObjectUtils#copySerializedObject(java.lang.Object)}.
     */
    public void testCopySerializedObject() {
        final String age = "Im too young!";
        final Object copy = SerializedObjectUtils.copySerializedObject(age);
        final String copiedAge = (String) copy;
        this.result = age.equals(copiedAge);
        assertTrue("", this.result);
        Object obj = new Object();
        Object otherCopy = SerializedObjectUtils.copySerializedObject(obj);
        this.result = obj.equals(otherCopy);
        assertTrue("", this.result);
    }

    /**
     * Test method for.
     *
     * {@link net.sourceforge.jaulp.io.SerializedObjectUtils#readSerializedObjectFromFile(java.io.File)}.
     */
    public void testReadSerializedObjectFromFile() {
        final Date birthdayFromNiko = DateUtils.createDate(2007, 11, 8);
        final File writeInMe = new File(this.deepDir, "testWriteSerializedObjectToFile.dat");
        this.result = SerializedObjectUtils.writeSerializedObjectToFile(birthdayFromNiko, writeInMe);
        assertTrue("", this.result);
        final Object readedObjectFromFile = SerializedObjectUtils.readSerializedObjectFromFile(writeInMe);
        final Date readedObj = (Date) readedObjectFromFile;
        this.result = birthdayFromNiko.equals(readedObj);
        assertTrue("", this.result);
    }

    /**
     * Test method for.
     *
     * {@link net.sourceforge.jaulp.io.SerializedObjectUtils#writeSerializedObjectToFile(java.lang.Object, java.io.File)}.
     */
    public void testWriteSerializedObjectToFile() {
        final Date birthdayFromNiko = DateUtils.createDate(2007, 11, 8);
        final File writeInMe = new File(this.deepDir, "testWriteSerializedObjectToFile.dat");
        this.result = SerializedObjectUtils.writeSerializedObjectToFile(birthdayFromNiko, writeInMe);
        assertTrue("", this.result);
        final Object readedObjectFromFile = SerializedObjectUtils.readSerializedObjectFromFile(writeInMe);
        final Date readedObj = (Date) readedObjectFromFile;
        this.result = birthdayFromNiko.equals(readedObj);
        assertTrue("", this.result);
    }
}
