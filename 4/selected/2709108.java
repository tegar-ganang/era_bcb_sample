package net.sourceforge.jaulp.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import net.sourceforge.jaulp.date.DateUtils;
import net.sourceforge.jaulp.file.compare.CompareFileUtils;
import net.sourceforge.jaulp.file.copy.CopyFileUtils;
import net.sourceforge.jaulp.file.create.CreateFileUtils;
import net.sourceforge.jaulp.file.delete.DeleteFileUtils;
import net.sourceforge.jaulp.file.exceptions.DirectoryAllreadyExistsException;
import net.sourceforge.jaulp.file.exceptions.FileDoesNotExistException;
import net.sourceforge.jaulp.file.exceptions.FileIsADirectoryException;
import net.sourceforge.jaulp.file.exceptions.FileIsNotADirectoryException;
import net.sourceforge.jaulp.file.exceptions.FileNotRenamedException;
import net.sourceforge.jaulp.file.read.ReadFileUtils;
import net.sourceforge.jaulp.file.rename.RenameFileUtils;
import net.sourceforge.jaulp.file.search.FileSearchUtils;
import net.sourceforge.jaulp.file.write.WriteFileUtils;
import net.sourceforge.jaulp.file.zip.ZipUtils;
import net.sourceforge.jaulp.io.SerializedObjectUtils;
import net.sourceforge.jaulp.io.StreamUtils;
import net.sourceforge.jaulp.string.StringUtils;

/**
 * Test class for the class FileUtils.
 *
 * @version 1.0
 * @author Asterios Raptis
 */
public class FileUtilsTest extends FileTestCase {

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
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#appendSystemtimeToFilename(java.io.File)} .
     */
    public void testAppendSystemtimeToFilename() {
        final String format = "HHmmssSSS";
        final String filePrefix = "testAppendSystemtimeToFilename";
        final String fileSuffix = ".txt";
        final File testFile1 = new File(this.testDir, filePrefix + fileSuffix);
        final String inputString = "Its a beautifull day!!!";
        final String ap = testFile1.getAbsolutePath();
        WriteFileUtils.string2File(inputString, ap);
        final Date before = new Date();
        final String compare = RenameFileUtils.appendSystemtimeToFilename(testFile1);
        final Date after = new Date();
        final int start = compare.indexOf("_");
        final int end = compare.indexOf(fileSuffix);
        final String sysDateFromFile = compare.substring(start + 1, end);
        final String sysTimeBefore = DateUtils.parseToString(before, format);
        final String sysTimeAfter = DateUtils.parseToString(after, format);
        final Date between = DateUtils.parseToDate(sysDateFromFile, format);
        final Date beforeDate = DateUtils.parseToDate(sysTimeBefore, format);
        final Date afterDate = DateUtils.parseToDate(sysTimeAfter, format);
        this.result = DateUtils.isBetween(beforeDate, afterDate, between);
        assertTrue("", this.result);
    }

    /**
     * Test method for.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws FileDoesNotExistException the file does not exist exception
     * {@link net.sourceforge.jaulp.file.FileUtils#changeAllFilenameSuffix(java.io.File, java.lang.String, java.lang.String)}
     * .
     */
    @SuppressWarnings("unchecked")
    public void testChangeAllFilenameSuffixFileStringString() throws IOException, FileDoesNotExistException {
        final String oldFileSuffix = ".txt";
        final String newFileSuffix = ".rtf";
        final List<File> filesWithNewSuffixes = new ArrayList<File>();
        final List<File> filesWithOldSuffixes = new ArrayList<File>();
        final String filePrefix1 = "testChangeAllFilenameSuffixFileStringString1";
        final File testFile1 = new File(this.deepDir, filePrefix1 + oldFileSuffix);
        filesWithOldSuffixes.add(testFile1);
        final File fileWithNewSuffix1 = new File(this.deepDir, filePrefix1 + newFileSuffix);
        filesWithNewSuffixes.add(fileWithNewSuffix1);
        final String filePrefix2 = "testChangeAllFilenameSuffixFileStringString2";
        final File testFile2 = new File(this.deepDir, filePrefix2 + oldFileSuffix);
        filesWithOldSuffixes.add(testFile2);
        final File fileWithNewSuffix2 = new File(this.deepDir, filePrefix2 + newFileSuffix);
        filesWithNewSuffixes.add(fileWithNewSuffix2);
        final String filePrefix3 = "testChangeAllFilenameSuffixFileStringString3";
        final File testFile3 = new File(this.deeperDir, filePrefix3 + oldFileSuffix);
        filesWithOldSuffixes.add(testFile3);
        final File fileWithNewSuffix3 = new File(this.deeperDir, filePrefix3 + newFileSuffix);
        filesWithNewSuffixes.add(fileWithNewSuffix3);
        final String filePrefix4 = "testChangeAllFilenameSuffixFileStringString4";
        final File testFile4 = new File(this.deeperDir, filePrefix4 + oldFileSuffix);
        filesWithOldSuffixes.add(testFile4);
        final File fileWithNewSuffix4 = new File(this.deeperDir, filePrefix4 + newFileSuffix);
        filesWithNewSuffixes.add(fileWithNewSuffix4);
        List notDeletedFiles = RenameFileUtils.changeAllFilenameSuffix(this.deepDir, oldFileSuffix, newFileSuffix);
        this.result = null == notDeletedFiles;
        assertTrue("", this.result);
        for (final Iterator iter = filesWithOldSuffixes.iterator(); iter.hasNext(); ) {
            final File currentFile = (File) iter.next();
            currentFile.createNewFile();
        }
        notDeletedFiles = RenameFileUtils.changeAllFilenameSuffix(this.deepDir, oldFileSuffix, newFileSuffix);
        this.result = null == notDeletedFiles;
        assertTrue("", this.result);
        for (final Iterator iter = filesWithNewSuffixes.iterator(); iter.hasNext(); ) {
            final File currentFile = (File) iter.next();
            this.result = FileSearchUtils.containsFileRecursive(this.deepDir, currentFile);
            assertTrue("", this.result);
        }
    }

    /**
     * Test method for.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws FileDoesNotExistException the file does not exist exception
     * {@link net.sourceforge.jaulp.file.FileUtils#changeAllFilenameSuffix(java.io.File, java.lang.String, java.lang.String, boolean)}
     * .
     */
    @SuppressWarnings("unchecked")
    public void testChangeAllFilenameSuffixFileStringStringBoolean() throws IOException, FileDoesNotExistException {
        final String oldFileSuffix = ".txt";
        final String newFileSuffix = ".rtf";
        final List<File> filesWithNewSuffixes = new ArrayList<File>();
        final List<File> filesWithOldSuffixes = new ArrayList<File>();
        final String filePrefix1 = "testChangeAllFilenameSuffixFileStringStringBoolean1";
        final File testFile1 = new File(this.deepDir, filePrefix1 + oldFileSuffix);
        filesWithOldSuffixes.add(testFile1);
        final File fileWithNewSuffix1 = new File(this.deepDir, filePrefix1 + newFileSuffix);
        filesWithNewSuffixes.add(fileWithNewSuffix1);
        final String filePrefix2 = "testChangeAllFilenameSuffixFileStringStringBoolean2";
        final File testFile2 = new File(this.deepDir, filePrefix2 + oldFileSuffix);
        filesWithOldSuffixes.add(testFile2);
        final File fileWithNewSuffix2 = new File(this.deepDir, filePrefix2 + newFileSuffix);
        filesWithNewSuffixes.add(fileWithNewSuffix2);
        final String filePrefix3 = "testChangeAllFilenameSuffixFileStringStringBoolean3";
        final File testFile3 = new File(this.deeperDir, filePrefix3 + oldFileSuffix);
        filesWithOldSuffixes.add(testFile3);
        final File fileWithNewSuffix3 = new File(this.deeperDir, filePrefix3 + newFileSuffix);
        filesWithNewSuffixes.add(fileWithNewSuffix3);
        final String filePrefix4 = "testChangeAllFilenameSuffixFileStringStringBoolean4";
        final File testFile4 = new File(this.deeperDir, filePrefix4 + oldFileSuffix);
        filesWithOldSuffixes.add(testFile4);
        final File fileWithNewSuffix4 = new File(this.deeperDir, filePrefix4 + newFileSuffix);
        filesWithNewSuffixes.add(fileWithNewSuffix4);
        List notDeletedFiles = RenameFileUtils.changeAllFilenameSuffix(this.deepDir, oldFileSuffix, newFileSuffix, true);
        this.result = null == notDeletedFiles;
        assertTrue("", this.result);
        for (final Iterator iter = filesWithOldSuffixes.iterator(); iter.hasNext(); ) {
            final File currentFile = (File) iter.next();
            currentFile.createNewFile();
        }
        notDeletedFiles = RenameFileUtils.changeAllFilenameSuffix(this.deepDir, oldFileSuffix, newFileSuffix, true);
        for (final Iterator iter = filesWithNewSuffixes.iterator(); iter.hasNext(); ) {
            final File currentFile = (File) iter.next();
            this.result = FileSearchUtils.containsFileRecursive(this.deepDir, currentFile);
            assertTrue("", this.result);
        }
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#changeFilenameSuffix(java.io.File, java.lang.String)}
     * .
     *
     * @throws FileNotRenamedException
     *             the file not renamed exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws FileDoesNotExistException
     *             the file does not exist exception
     */
    public void testChangeFilenameSuffixFileString() throws FileNotRenamedException, IOException, FileDoesNotExistException {
        final String filePrefix = "testChangeFilenameSuffixFileString";
        final String oldFileSuffix = ".txt";
        final String newFileSuffix = ".rtf";
        final File testFile1 = new File(this.deepDir, filePrefix + oldFileSuffix);
        final File fileWithNewSuffix = new File(this.deepDir, filePrefix + newFileSuffix);
        try {
            this.result = RenameFileUtils.changeFilenameSuffix(testFile1, newFileSuffix);
        } catch (final Exception e) {
            this.result = e instanceof FileDoesNotExistException;
            assertTrue("", this.result);
        }
        testFile1.createNewFile();
        this.result = RenameFileUtils.changeFilenameSuffix(testFile1, newFileSuffix);
        assertTrue("", this.result);
        this.result = FileSearchUtils.containsFile(this.deepDir, fileWithNewSuffix);
        assertTrue("", this.result);
    }

    /**
     * Test method for.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws FileNotRenamedException the file not renamed exception
     * @throws FileDoesNotExistException the file does not exist exception
     * {@link net.sourceforge.jaulp.file.FileUtils#changeFilenameSuffix(java.io.File, java.lang.String, boolean)} .
     */
    public void testChangeFilenameSuffixFileStringBoolean() throws IOException, FileNotRenamedException, FileDoesNotExistException {
        final String filePrefix = "testChangeFilenameSuffixFileStringBoolean";
        final String oldFileSuffix = ".txt";
        final String newFileSuffix = ".rtf";
        final File testFile1 = new File(this.deepDir, filePrefix + oldFileSuffix);
        final File fileWithNewSuffix = new File(this.deepDir, filePrefix + newFileSuffix);
        try {
            this.result = RenameFileUtils.changeFilenameSuffix(testFile1, newFileSuffix, true);
        } catch (final Exception e) {
            this.result = e instanceof FileDoesNotExistException;
            assertTrue("", this.result);
        }
        testFile1.createNewFile();
        this.result = RenameFileUtils.changeFilenameSuffix(testFile1, newFileSuffix, true);
        assertTrue("", this.result);
        this.result = FileSearchUtils.containsFile(this.deepDir, fileWithNewSuffix);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#checkFile(java.io.File)}.
     *
     * @throws DirectoryAllreadyExistsException
     *             the directory allready exists exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void testCheckFile() throws DirectoryAllreadyExistsException, IOException {
        if (this.testDir.exists()) {
            DeleteFileUtils.delete(this.testDir);
        }
        this.testDir = new File(this.testResources, "testDir");
        Exception ex = DeleteFileUtils.checkFile(this.testDir);
        this.result = ex != null;
        assertTrue("", this.result);
        this.result = ex instanceof FileDoesNotExistException;
        assertTrue("", this.result);
        if (!this.testDir.exists()) {
            final boolean created = CreateFileUtils.createDirectory(this.testDir);
            assertTrue("The directory should be created.", created);
        }
        ex = DeleteFileUtils.checkFile(this.testDir);
        this.result = ex == null;
        assertTrue("", this.result);
        final File testFile1 = new File(this.testDir, "testCheckFile.txt");
        WriteFileUtils.string2File(testFile1, "Its a beautifull day!!!");
        ex = DeleteFileUtils.checkFile(testFile1);
        this.result = ex != null;
        assertTrue("", this.result);
        this.result = ex instanceof FileIsNotADirectoryException;
        assertTrue("", this.result);
        final File testFile2 = new File("a");
        ex = DeleteFileUtils.checkFile(testFile2);
        this.result = ex != null;
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#compareFiles(File, File, boolean)} .
     */
    public void testCompareFiles() {
        final String filePrefix1 = "testCompareFiles1";
        final String filePrefix2 = "testCompareFiles2";
        final String oldFileSuffix = ".txt";
        final String newFileSuffix = ".rtf";
        final File source = new File(this.deepDir, filePrefix1 + oldFileSuffix);
        File compare = new File(this.deepDir, filePrefix1 + oldFileSuffix);
        this.result = CompareFileUtils.compareFiles(source, compare, false);
        assertTrue("File should be equal cause they dont exist.", this.result);
        compare = new File(this.deepDir, filePrefix2 + newFileSuffix);
        this.result = CompareFileUtils.compareFiles(source, compare, false);
        assertFalse("File should not be equal.", this.result);
        WriteFileUtils.string2File(source, "Its a beautifull day!!!");
        WriteFileUtils.string2File(compare, "Its a beautifull day!!!");
        this.result = CompareFileUtils.compareFiles(source, compare, false);
        assertTrue("File should be equal.", this.result);
        this.result = CompareFileUtils.compareFiles(source, compare, true);
        assertTrue("File should be equal.", this.result);
        WriteFileUtils.string2File(compare, "Its a beautifull evening!!!");
        this.result = CompareFileUtils.compareFiles(source, compare, true);
        assertFalse("File should not be equal.", this.result);
        WriteFileUtils.string2File(compare, "Its a beautifull boy!!!");
        this.result = CompareFileUtils.compareFiles(source, compare, true);
        assertFalse("File should not be equal.", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#containsFile(java.io.File, java.io.File)} .
     *
     * @throws DirectoryAllreadyExistsException
     *             the directory allready exists exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void testContainsFileFileFile() throws DirectoryAllreadyExistsException, IOException {
        final File testFile = new File(this.testDir, "beautifull.txt");
        WriteFileUtils.string2File(testFile, "Its a beautifull day!!!");
        boolean contains = FileSearchUtils.containsFile(new File("."), testFile);
        assertFalse("File should not exist in this directory.", contains);
        contains = FileSearchUtils.containsFile(this.testDir, testFile);
        assertTrue("File should not exist in this directory.", contains);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#containsFile(java.io.File, java.lang.String)} .
     */
    public void testContainsFileFileString() {
        final File testFile = new File(this.testDir, "beautifull.txt");
        WriteFileUtils.string2File(testFile, "Its a beautifull day!!!");
        boolean contains = FileSearchUtils.containsFile(new File("."), testFile);
        assertFalse("File should not exist in this directory.", contains);
        final String filename = testFile.getName();
        contains = FileSearchUtils.containsFile(this.testDir, filename);
        assertTrue("File should not exist in this directory.", contains);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#containsFileRecursive(File, File)} .
     */
    public void testContainsFileRecursive() {
        final File testFile = new File(this.testDir.getAbsoluteFile(), "testContainsFileRecursives.txt");
        WriteFileUtils.string2File(testFile, "Its a beautifull day!!!");
        final File testFile3 = new File(this.deepDir.getAbsoluteFile(), "testContainsFileRecursives.cvs");
        WriteFileUtils.string2File(testFile3, "Its a beautifull evening!!!");
        final File currentDir = new File(".").getAbsoluteFile();
        boolean contains = FileSearchUtils.containsFileRecursive(currentDir.getAbsoluteFile(), testFile);
        assertFalse("File should not exist in this directory.", contains);
        contains = FileSearchUtils.containsFileRecursive(this.testDir, testFile);
        assertTrue("File should not exist in this directory.", contains);
        this.result = FileSearchUtils.containsFileRecursive(this.testDir, testFile3);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#convert2ByteArray(java.lang.Object)} .
     */
    public void testConvert2ByteArray() {
        final byte[] expected = { -84, -19, 0, 5, 116, 0, 7, 70, 111, 111, 32, 98, 97, 114 };
        final String testString = "Foo bar";
        byte[] compare = null;
        compare = SerializedObjectUtils.convert2ByteArray(testString);
        for (int i = 0; i < compare.length; i++) {
            this.result = expected[i] == compare[i];
            assertTrue("", this.result);
        }
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#convert2Object(byte[])}.
     */
    public void testConvert2Object() {
        final byte[] testBytearray = { -84, -19, 0, 5, 116, 0, 7, 70, 111, 111, 32, 98, 97, 114 };
        final String expected = "Foo bar";
        final Object obj = SerializedObjectUtils.convert2Object(testBytearray);
        final String compare = (String) obj;
        this.result = expected.equals(compare);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#copyFile(java.io.File, java.io.File)} .
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws FileIsADirectoryException Is thrown if the destination file is a directory.
     */
    public void testCopyFile() throws IOException, FileIsADirectoryException {
        final File source = new File(this.testDir.getAbsoluteFile(), "testCopyFileInput.txt");
        final File destination = new File(this.testDir.getAbsoluteFile(), "testCopyFileOutput.tft");
        try {
            this.result = CopyFileUtils.copyFile(source, destination);
            assertFalse("", this.result);
        } catch (final Exception fnfe) {
            this.result = fnfe instanceof FileNotFoundException;
            assertTrue("", this.result);
        }
        final String inputString = "Its a beautifull day!!!";
        final String expected = inputString;
        WriteFileUtils.string2File(source, inputString);
        this.result = CopyFileUtils.copyFile(source, destination);
        assertTrue("", this.result);
        final String compare = ReadFileUtils.readFromFile(destination);
        this.result = expected.equals(compare);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#createDirectory(java.io.File)} .
     *
     * @throws DirectoryAllreadyExistsException
     *             the directory allready exists exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void testCreateDirectory() throws DirectoryAllreadyExistsException, IOException {
        final File testing = new File(this.testResources, "testCreateDirectory");
        if (testing.exists()) {
            DeleteFileUtils.delete(testing);
        }
        final boolean created = CreateFileUtils.createDirectory(testing);
        assertTrue("The directory should be created.", created);
        this.result = testing.isDirectory();
        assertTrue("Created File should be a directory.", this.result);
        if (testing.exists()) {
            DeleteFileUtils.delete(testing);
        }
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#createFile(File)}.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void testCreateFile() throws IOException {
        final File source = new File(this.testDir.getAbsoluteFile(), "testGetOutputStream.txt");
        CreateFileUtils.createFile(source);
        this.result = source.exists();
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#deleteFiles(java.io.File)}.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws DirectoryAllreadyExistsException
     *             the directory allready exists exception
     */
    public void testDeleleFiles() throws IOException, DirectoryAllreadyExistsException {
        final File testFile1 = new File(this.testDir, "testDeleleFiles1.txt");
        final File testFile2 = new File(this.testDir, "testDeleleFiles2.txt");
        final File testFile3 = new File(this.deepDir, "testDeleleFiles3.txt");
        final File testFile4 = new File(this.testDir, "testDeleleFiles4.tft");
        final File testFile5 = new File(this.deepDir, "testDeleleFiles5.cvs");
        WriteFileUtils.string2File(testFile1, "Its a beautifull day!!!");
        WriteFileUtils.string2File(testFile2, "Its a beautifull evening!!!");
        WriteFileUtils.string2File(testFile3, "Its a beautifull night!!!");
        WriteFileUtils.string2File(testFile4, "Its a beautifull morning!!!");
        WriteFileUtils.string2File(testFile5, "She's a beautifull woman!!!");
        this.result = this.deepDir.exists();
        assertTrue("", this.result);
        this.result = testFile1.exists();
        assertTrue("", this.result);
        this.result = testFile2.exists();
        assertTrue("", this.result);
        this.result = testFile3.exists();
        assertTrue("", this.result);
        this.result = testFile4.exists();
        assertTrue("", this.result);
        this.result = testFile5.exists();
        assertTrue("", this.result);
        this.result = this.testDir.exists();
        assertTrue("", this.result);
        DeleteFileUtils.deleteFiles(this.testDir);
        this.result = this.deepDir.exists();
        assertFalse("", this.result);
        this.result = testFile1.exists();
        assertFalse("", this.result);
        this.result = testFile2.exists();
        assertFalse("", this.result);
        this.result = testFile3.exists();
        assertFalse("", this.result);
        this.result = testFile4.exists();
        assertFalse("", this.result);
        this.result = testFile5.exists();
        assertFalse("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#delete(java.io.File)}.
     *
     * @throws DirectoryAllreadyExistsException
     *             the directory allready exists exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void testDelete() throws DirectoryAllreadyExistsException, IOException {
        final File testFile1 = new File(this.testDir, "testDelete1.txt");
        final File testFile2 = new File(this.testDir, "testDelete2.txt");
        final File testFile3 = new File(this.deepDir, "testDelete3.txt");
        final File testFile4 = new File(this.testDir, "testDelete4.tft");
        final File testFile5 = new File(this.deepDir, "testDelete5.cvs");
        WriteFileUtils.string2File(testFile1, "Its a beautifull day!!!");
        WriteFileUtils.string2File(testFile2, "Its a beautifull evening!!!");
        WriteFileUtils.string2File(testFile3, "Its a beautifull night!!!");
        WriteFileUtils.string2File(testFile4, "Its a beautifull morning!!!");
        WriteFileUtils.string2File(testFile5, "She's a beautifull woman!!!");
        this.result = testFile1.exists();
        assertTrue("", this.result);
        DeleteFileUtils.delete(testFile1);
        this.result = testFile1.exists();
        assertFalse("", this.result);
        this.result = testFile3.exists();
        assertTrue("", this.result);
        DeleteFileUtils.delete(testFile3);
        this.result = testFile3.exists();
        assertFalse("", this.result);
        this.result = testFile5.exists();
        assertTrue("", this.result);
        this.result = this.deepDir.exists();
        assertTrue("", this.result);
        DeleteFileUtils.delete(this.deepDir);
        this.result = this.deepDir.exists();
        assertFalse("", this.result);
        this.result = testFile5.exists();
        assertFalse("", this.result);
        this.result = testFile4.exists();
        assertTrue("", this.result);
        this.result = this.testDir.exists();
        assertTrue("", this.result);
        DeleteFileUtils.delete(this.testDir);
        this.result = testFile4.exists();
        assertFalse("", this.result);
        this.result = this.testDir.exists();
        assertFalse("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#deleteAllFiles(java.io.File)} .
     *
     * @throws DirectoryAllreadyExistsException
     *             the directory allready exists exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void testDeleteAllFiles() throws DirectoryAllreadyExistsException, IOException {
        final File testFile1 = new File(this.testDir, "testDeleteAllFiles1.txt");
        final File testFile2 = new File(this.testDir, "testDeleteAllFiles2.txt");
        final File testFile3 = new File(this.deepDir, "testDeleteAllFiles3.txt");
        final File testFile4 = new File(this.testDir, "testDeleteAllFiles4.tft");
        final File testFile5 = new File(this.deepDir, "testDeleteAllFiles5.cvs");
        WriteFileUtils.string2File(testFile1, "Its a beautifull day!!!");
        WriteFileUtils.string2File(testFile2, "Its a beautifull evening!!!");
        WriteFileUtils.string2File(testFile3, "Its a beautifull night!!!");
        WriteFileUtils.string2File(testFile4, "Its a beautifull morning!!!");
        WriteFileUtils.string2File(testFile5, "She's a beautifull woman!!!");
        this.result = testFile1.exists();
        assertTrue("", this.result);
        this.result = testFile3.exists();
        assertTrue("", this.result);
        this.result = testFile5.exists();
        assertTrue("", this.result);
        this.result = this.deepDir.exists();
        assertTrue("", this.result);
        this.result = testFile4.exists();
        assertTrue("", this.result);
        this.result = this.testDir.exists();
        assertTrue("", this.result);
        DeleteFileUtils.deleteAllFiles(this.testDir);
        this.result = this.deepDir.exists();
        assertFalse("", this.result);
        this.result = this.testDir.exists();
        assertFalse("", this.result);
        this.result = testFile1.exists();
        assertFalse("", this.result);
        this.result = testFile2.exists();
        assertFalse("", this.result);
        this.result = testFile3.exists();
        assertFalse("", this.result);
        this.result = testFile4.exists();
        assertFalse("", this.result);
        this.result = testFile5.exists();
        assertFalse("", this.result);
    }

    /**
     * Test method for.
     *
     * @throws DirectoryAllreadyExistsException the directory allready exists exception
     * @throws IOException Signals that an I/O exception has occurred.
     * {@link net.sourceforge.jaulp.file.FileUtils#deleteAllFilesWithSuffix(java.io.File, java.lang.String)} .
     */
    public void testDeleteAllFilesWithSuffix() throws DirectoryAllreadyExistsException, IOException {
        final File testFile1 = new File(this.testDir, "testDeleteAllFilesWithSuffix1.txt");
        final File testFile2 = new File(this.testDir, "testDeleteAllFilesWithSuffix2.txt");
        final File testFile3 = new File(this.deepDir, "testDeleteAllFilesWithSuffix3.txt");
        final File testFile4 = new File(this.testDir, "testDeleteAllFilesWithSuffix4.tft");
        final File testFile5 = new File(this.deepDir, "testDeleteAllFilesWithSuffix5.cvs");
        WriteFileUtils.string2File(testFile1, "Its a beautifull day!!!");
        WriteFileUtils.string2File(testFile2, "Its a beautifull evening!!!");
        WriteFileUtils.string2File(testFile3, "Its a beautifull night!!!");
        WriteFileUtils.string2File(testFile4, "Its a beautifull morning!!!");
        WriteFileUtils.string2File(testFile5, "She's a beautifull woman!!!");
        DeleteFileUtils.deleteAllFilesWithSuffix(this.testDir, ".txt");
        this.result = testFile1.exists();
        assertFalse("", this.result);
        this.result = testFile2.exists();
        assertFalse("", this.result);
        this.result = testFile3.exists();
        assertFalse("", this.result);
        this.result = testFile4.exists();
        assertTrue("", this.result);
        this.result = testFile5.exists();
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#deleteFile(java.io.File)}.
     *
     * @throws DirectoryAllreadyExistsException
     *             the directory allready exists exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void testDeleteFile() throws DirectoryAllreadyExistsException, IOException {
        final File testFile1 = new File(this.testDir, "testDeleteFile1.txt");
        final File testFile2 = new File(this.testDir, "testDeleteFile2.txt");
        final File testFile3 = new File(this.deepDir, "testDeleteFile3.txt");
        final File testFile4 = new File(this.testDir, "testDeleteFile4.tft");
        final File testFile5 = new File(this.deepDir, "testDeleteFile5.cvs");
        WriteFileUtils.string2File(testFile1, "Its a beautifull day!!!");
        WriteFileUtils.string2File(testFile2, "Its a beautifull evening!!!");
        WriteFileUtils.string2File(testFile3, "Its a beautifull night!!!");
        WriteFileUtils.string2File(testFile4, "Its a beautifull morning!!!");
        WriteFileUtils.string2File(testFile5, "She's a beautifull woman!!!");
        this.result = testFile1.exists();
        assertTrue("", this.result);
        DeleteFileUtils.deleteFile(testFile1);
        this.result = testFile1.exists();
        assertFalse("", this.result);
        this.result = testFile3.exists();
        assertTrue("", this.result);
        DeleteFileUtils.deleteFile(testFile3);
        this.result = testFile3.exists();
        assertFalse("", this.result);
        this.result = testFile5.exists();
        assertTrue("", this.result);
        this.result = this.deepDir.exists();
        assertTrue("", this.result);
        DeleteFileUtils.deleteFile(this.deepDir);
        this.result = this.deepDir.exists();
        assertFalse("", this.result);
        this.result = testFile5.exists();
        assertFalse("", this.result);
        this.result = testFile4.exists();
        assertTrue("", this.result);
        this.result = this.testDir.exists();
        assertTrue("", this.result);
        DeleteFileUtils.deleteFile(this.testDir);
        this.result = testFile4.exists();
        assertFalse("", this.result);
        this.result = this.testDir.exists();
        assertFalse("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#download(java.net.URI)}.
     */
    public void testDownload() {
        final byte[] expected = { -84, -19, 0, 5, 116, 0, 7, 70, 111, 111, 32, 98, 97, 114 };
        final File destination = new File(this.testDir.getAbsoluteFile(), "testDownload.txt");
        WriteFileUtils.storeByteArrayToFile(expected, destination);
        final byte[] compare = FileUtils.download(destination.toURI());
        for (int i = 0; i < compare.length; i++) {
            this.result = compare[i] == expected[i];
            assertTrue("", this.result);
        }
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#findFiles(java.io.File, java.lang.String)} .
     *
     * @throws DirectoryAllreadyExistsException
     *             the directory allready exists exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unchecked")
    public void testFindFilesFileString() throws DirectoryAllreadyExistsException, IOException {
        final String test = "testFindFilesFileString.t*";
        final File testFile1 = new File(this.testDir, "testFindFilesFileString.txt");
        final File testFile2 = new File(this.testDir, "testFindFilesFileString.tft");
        final File testFile3 = new File(this.deepDir, "testFindFilesFileString.cvs");
        WriteFileUtils.string2File(testFile1, "Its a beautifull day!!!");
        WriteFileUtils.string2File(testFile2, "Its a beautifull evening!!!");
        WriteFileUtils.string2File(testFile3, "Its a beautifull night!!!");
        final List foundedFiles = FileSearchUtils.findFiles(this.testDir, test);
        this.result = foundedFiles != null;
        assertTrue(this.result);
        this.result = foundedFiles.size() == 2;
        assertTrue(this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#findFilesRecursive(java.io.File, java.lang.String)} .
     *
     * @throws DirectoryAllreadyExistsException
     *             the directory allready exists exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unchecked")
    public void testFindFilesRecursive() throws DirectoryAllreadyExistsException, IOException {
        final String test = "testFindFilesRecursive.t*";
        final List expectedFiles = new ArrayList();
        final File testFile1 = new File(this.testDir.getAbsoluteFile(), "testFindFilesRecursive.txt");
        expectedFiles.add(testFile1);
        final File testFile2 = new File(this.testDir.getAbsoluteFile(), "testFindFilesRecursive.tft");
        expectedFiles.add(testFile2);
        final File testFile3 = new File(this.deepDir, "testFindFilesRecursive.cvs");
        WriteFileUtils.string2File(testFile1, "Its a beautifull day!!!");
        WriteFileUtils.string2File(testFile2, "Its a beautifull evening!!!");
        WriteFileUtils.string2File(testFile3, "Its a beautifull night!!!");
        List foundedFiles = FileSearchUtils.findFilesRecursive(this.testDir, test);
        this.result = foundedFiles != null;
        assertTrue(this.result);
        this.result = foundedFiles.size() == 2;
        assertTrue(this.result);
        for (int i = 0; i < expectedFiles.size(); i++) {
            this.result = foundedFiles.contains(expectedFiles.get(i));
            assertTrue(this.result);
        }
        String pattern = "*";
        final File testFile4 = new File(this.deepDir, "testFindFilesRecursive2.cvs");
        testFile4.createNewFile();
        foundedFiles = FileSearchUtils.findFilesRecursive(this.testDir, pattern);
        this.result = foundedFiles != null;
        assertTrue(this.result);
        this.result = foundedFiles.size() == 4;
        assertTrue(this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#findFiles(java.lang.String, java.lang.String[])} .
     */
    @SuppressWarnings("unchecked")
    public void testFindFilesStringStringArray() {
        final Vector expected = new Vector();
        final File testFile1 = new File(this.testDir.getAbsoluteFile(), "testFindFilesStringStringArray1.txt");
        final File testFile2 = new File(this.testDir.getAbsoluteFile(), "testFindFilesStringStringArray2.tft");
        final File testFile3 = new File(this.testDir.getAbsoluteFile(), "testFindFilesStringStringArray3.txt");
        final File testFile4 = new File(this.deepDir.getAbsoluteFile(), "testFindFilesStringStringArray4.tft");
        final File testFile5 = new File(this.deepDir.getAbsoluteFile(), "testFindFilesStringStringArray5.cvs");
        final File testFile6 = new File(this.deepDir2.getAbsoluteFile(), "testFindFilesStringStringArray6.txt");
        final File testFile7 = new File(this.deepDir2.getAbsoluteFile(), "testFindFilesStringStringArray7.cvs");
        final File testFile8 = new File(this.deeperDir.getAbsoluteFile(), "testFindFilesStringStringArray8.txt");
        final File testFile9 = new File(this.deeperDir.getAbsoluteFile(), "testFindFilesStringStringArray9.cvs");
        WriteFileUtils.string2File(testFile1, "Its a beautifull day!!!");
        WriteFileUtils.string2File(testFile2, "Its a beautifull evening!!!");
        WriteFileUtils.string2File(testFile3, "Its a beautifull night!!!");
        WriteFileUtils.string2File(testFile4, "Its a beautifull morning!!!");
        WriteFileUtils.string2File(testFile5, "She's a beautifull woman!!!");
        WriteFileUtils.string2File(testFile6, "Its a beautifull street!!!");
        WriteFileUtils.string2File(testFile7, "He's a beautifull man!!!");
        WriteFileUtils.string2File(testFile8, "Its a beautifull city!!!");
        WriteFileUtils.string2File(testFile9, "He's a beautifull boy!!!");
        expected.add(testFile1);
        expected.add(testFile3);
        expected.add(testFile6);
        expected.add(testFile8);
        final String[] txtExtension = { ".txt" };
        final Vector compare = FileSearchUtils.findFiles(this.testDir.getAbsolutePath(), txtExtension);
        this.result = expected.size() == compare.size();
        assertTrue("", this.result);
        for (final Iterator iter = compare.iterator(); iter.hasNext(); ) {
            final File currentFile = (File) iter.next();
            this.result = expected.contains(currentFile);
            assertTrue("", this.result);
        }
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#getFilenamePrefix(File)}.
     */
    public void testGetFilenamePrefix() {
        final String filePrefix = "testGetFilenamePrefix";
        final String fileSuffix = ".txt";
        final File testFile1 = new File(this.testDir, filePrefix + fileSuffix);
        final String ap = testFile1.getAbsolutePath();
        final int ext_index = ap.lastIndexOf(".");
        final String fileNamePrefix = ap.substring(0, ext_index);
        final String expected = fileNamePrefix;
        final String compare = FileUtils.getFilenamePrefix(testFile1);
        this.result = expected.equals(compare);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#getFilenameSuffix(File)}.
     */
    public void testGetFilenameSuffix() {
        final String filePrefix = "testAppendSystemtimeToFilename";
        final String fileSuffix = ".txt";
        final String expected = fileSuffix;
        final File testFile1 = new File(this.testDir, filePrefix + fileSuffix);
        final String compare = FileUtils.getFilenameSuffix(testFile1);
        this.result = expected.equals(compare);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#getInputStream(File, boolean)} .
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void testGetInputStream() throws IOException {
        final File source = new File(this.testDir.getAbsoluteFile(), "testGetInputStream.txt");
        final String inputString = "Its a beautifull day!!!  ����";
        final String expected = inputString;
        WriteFileUtils.writeStringToFile(source, inputString, null);
        final InputStream is = ReadFileUtils.getInputStream(source);
        final StringBuffer sb = new StringBuffer();
        int byt;
        while ((byt = is.read()) != -1) {
            sb.append((char) byt);
        }
        StreamUtils.closeInputStream(is);
        final String compare = sb.toString();
        this.result = expected.equals(compare);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#getInputStream(File, boolean)} .
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void testGetOutputStream() throws IOException {
        final File source = new File(this.testDir.getAbsoluteFile(), "testGetOutputStream.txt");
        final File destination = new File(this.testDir.getAbsoluteFile(), "testGetOutputStream.tft");
        final String inputString = "Its a beautifull day!!!  ����";
        final String expected = inputString;
        WriteFileUtils.writeStringToFile(source, inputString, null);
        final OutputStream os = WriteFileUtils.getOutputStream(destination, true);
        os.write(inputString.getBytes());
        StreamUtils.closeOutputStream(os);
        final String compare = ReadFileUtils.readFromFile(destination);
        this.result = expected.equals(compare);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#inputStream2String(java.io.InputStream)} .
     *
     * @throws DirectoryAllreadyExistsException
     *             the directory allready exists exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws FileDoesNotExistException
     *             the file does not exist exception
     */
    public void testInputStream2String() throws DirectoryAllreadyExistsException, IOException, FileDoesNotExistException {
        final File inputFile = new File(this.testDir, "testInputStream2String.inp");
        inputFile.createNewFile();
        final String inputString = "Its a beautifull day!!!\n" + "This is the second line.\n" + "This is the third line. ";
        WriteFileUtils.string2File(inputFile, inputString);
        final InputStream is = ReadFileUtils.getInputStream(inputFile);
        final String compare = ReadFileUtils.inputStream2String(is);
        this.result = inputString.equals(compare);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#isOpen(java.io.File)}.
     */
    public void testIsOpen() {
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#isZip(java.lang.String)}.
     */
    public void testIsZip() {
        final int length = FileConst.ZIP_EXTENSIONS.length;
        for (int i = 0; i < length; i++) {
            final File testIsZip = new File(this.testResources, "testIsZip" + FileConst.ZIP_EXTENSIONS[i]);
            this.result = ZipUtils.isZip(testIsZip.getName());
            assertTrue("The file " + testIsZip.getName() + " should be a zipfile.", this.result);
        }
        this.result = ZipUtils.isZip(this.testResources.getName());
        assertFalse("The file " + this.testResources.getName() + " should not be a zipfile.", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#match(java.lang.String, java.lang.String[])} .
     */
    public void testMatch() {
        final String filename = "testMatch.txt";
        final String txtExtension = ".txt";
        final String rtfExtension = ".rtf";
        final String cvsExtension = ".cvs";
        final String[] extensions = { txtExtension };
        this.result = FileSearchUtils.match(filename, extensions);
        assertTrue("", this.result);
        final String[] otherExtensions = { rtfExtension, cvsExtension };
        this.result = FileSearchUtils.match(filename, otherExtensions);
        assertFalse("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#moveFile(java.io.File, java.io.File)} .
     *
     * @throws DirectoryAllreadyExistsException the directory allready exists exception
     */
    public void testMoveFile() throws DirectoryAllreadyExistsException {
        final String filePrefix1 = "testMoveFile";
        final String oldFileSuffix = ".txt";
        final File srcFile = new File(this.deepDir, filePrefix1 + oldFileSuffix);
        final File destDir = new File(this.deeperDir, filePrefix1 + oldFileSuffix);
        this.result = RenameFileUtils.moveFile(srcFile, destDir);
        assertFalse("File should not exist in this directory.", this.result);
        WriteFileUtils.string2File(srcFile, "Its a beautifull day!!!");
        this.result = RenameFileUtils.moveFile(srcFile, destDir);
        assertTrue("File should be renamed.", this.result);
        this.result = FileSearchUtils.containsFile(this.deeperDir, destDir);
        assertTrue("The renamed file should exist in this directory.", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#moveFile(java.io.File, java.io.File)} .
     *
     * @throws DirectoryAllreadyExistsException the directory allready exists exception
     */
    public void testMoveDir() throws DirectoryAllreadyExistsException {
        final File srcDir = new File(this.deepDir, "dirToMove");
        File destDir = new File(this.deeperDir, "dirToMove");
        final String filePrefix = "testMoveFile";
        final String fileSuffix = ".txt";
        File srcFile = new File(srcDir, filePrefix + fileSuffix);
        if (!srcDir.exists()) {
            boolean created = CreateFileUtils.createDirectory(srcDir);
            assertTrue("The directory " + srcDir.getAbsolutePath() + " should be created.", created);
            WriteFileUtils.string2File(srcFile, "Its a beautifull day!!!");
        }
        System.err.println("-------------------------------------------------");
        System.err.println("srcFile.getAbsolutePath():" + srcFile.getAbsolutePath());
        System.err.println("-------------------------------------------------");
        this.result = RenameFileUtils.moveFile(srcDir, destDir);
        assertTrue("Directory should be renamed.", this.result);
        System.err.println("-------------------------------------------------");
        System.err.println("srcFile.getAbsolutePath():" + srcFile.getAbsolutePath());
        System.err.println("-------------------------------------------------");
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#openFileReader(java.lang.String)} .
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void testOpenFileReader() throws IOException {
        final File testFile1 = new File(this.testDir, "testOpenFileReader.txt");
        final String inputString = "Its a beautifull day!!!";
        final String expected = inputString;
        final String ap = testFile1.getAbsolutePath();
        WriteFileUtils.string2File(inputString, ap);
        final Reader reader = ReadFileUtils.openFileReader(ap);
        final String compare = ReadFileUtils.reader2String(reader);
        this.result = expected.equals(compare);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#reader2String(java.io.Reader)} .
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws DirectoryAllreadyExistsException
     *             the directory allready exists exception
     */
    public void testReader2String() throws IOException, DirectoryAllreadyExistsException {
        final File inputFile = new File(this.testDir, "testReader2String.inp");
        inputFile.createNewFile();
        final String inputString = "Its a beautifull day!!!\n" + "This is the second line.\n" + "This is the third line. ";
        WriteFileUtils.string2File(inputFile, inputString);
        final Reader reader = ReadFileUtils.getReader(inputFile);
        final String compare = ReadFileUtils.reader2String(reader);
        this.result = inputString.equals(compare);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#readFromFile(java.io.File)}.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @throws DirectoryAllreadyExistsException
     *             the directory allready exists exception
     */
    public void testReadFromFile() throws IOException, DirectoryAllreadyExistsException {
        final File testFile1 = new File(this.testDir, "testReadFromFile.txt");
        final String inputString = "Its a beautifull day!!!";
        WriteFileUtils.string2File(testFile1, inputString);
        final String content = ReadFileUtils.readFromFile(testFile1);
        this.result = inputString.equals(content);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#readHeadLine(java.lang.String)} .
     *
     * @throws DirectoryAllreadyExistsException
     *             the directory allready exists exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void testReadHeadLine() throws DirectoryAllreadyExistsException, IOException {
        final File inputFile = new File(this.testDir, "testReadHeadLine.inp");
        inputFile.createNewFile();
        final String inputString = "Its a beautifull day!!!\n This is the second line.\nThis is the third line. ";
        final String expected = "Its a beautifull day!!!";
        WriteFileUtils.string2File(inputFile, inputString);
        final String compare = ReadFileUtils.readHeadLine(inputFile.getAbsolutePath());
        this.result = expected.equals(compare);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#readLinesInList(java.io.File)} .
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unchecked")
    public void testReadLinesInList() throws IOException {
        final List expected = new ArrayList();
        expected.add("test1");
        expected.add("test2");
        expected.add("test3");
        expected.add("bla");
        expected.add("fasel");
        expected.add("and");
        expected.add("so");
        expected.add("on");
        expected.add("test4");
        expected.add("test5");
        expected.add("test6");
        expected.add("foo");
        expected.add("bar");
        expected.add("sim");
        expected.add("sala");
        expected.add("bim");
        final File testFile = new File(this.testResources, "testReadLinesInList.lst");
        final List testList = ReadFileUtils.readLinesInList(testFile);
        this.result = expected.equals(testList);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#readPropertiesFromFile(java.lang.String)} .
     *
     */
    public void testReadPropertiesFromFile() {
        final File tp = new File(this.testResources, "testReadPropertiesFromFile.properties");
        final String ap = tp.getAbsolutePath();
        Properties compare = new Properties();
        final Properties properties = new Properties();
        properties.setProperty("testkey1", "testvalue1");
        properties.setProperty("testkey2", "testvalue2");
        properties.setProperty("testkey3", "testvalue3");
        WriteFileUtils.writeProperties2File(ap, properties);
        compare = ReadFileUtils.readPropertiesFromFile(ap);
        this.result = properties.equals(compare);
        assertTrue(this.result);
    }

    /**
     * Test method for.
     *
     * {@link net.sourceforge.jaulp.file.FileUtils#readSourceFileAndWriteDestFile(java.lang.String, java.lang.String)} .
     */
    public void testReadSourceFileAndWriteDestFile() {
        final File source = new File(this.testDir.getAbsoluteFile(), "testReadSourceFileAndWriteDestFileInput.txt");
        final File destination = new File(this.testDir.getAbsoluteFile(), "testReadSourceFileAndWriteDestFileOutput.tft");
        try {
            WriteFileUtils.readSourceFileAndWriteDestFile(source.getAbsolutePath(), destination.getAbsolutePath());
        } catch (final Exception e) {
            this.result = e instanceof FileNotFoundException;
            assertTrue("Exception should be of type FileNotFoundException.", this.result);
        }
        final String inputString = "Its a beautifull day!!!";
        final String expected = inputString;
        WriteFileUtils.string2File(source, inputString);
        WriteFileUtils.readSourceFileAndWriteDestFile(source.getAbsolutePath(), destination.getAbsolutePath());
        final String compare = ReadFileUtils.readFromFile(destination);
        this.result = expected.equals(compare);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#renameFile(File, File, boolean)} .
     */
    public void testRenameFile() {
        final String filePrefix1 = "testRenameFileFileFile1";
        final String filePrefix2 = "testRenameFileFileFile2";
        final String oldFileSuffix = ".txt";
        final String newFileSuffix = ".rtf";
        final File testFile1 = new File(this.deepDir, filePrefix1 + oldFileSuffix);
        final File renamedFile1 = new File(this.deepDir, filePrefix2 + newFileSuffix);
        this.result = RenameFileUtils.renameFile(testFile1, renamedFile1, false);
        assertFalse("File should not exist in this directory.", this.result);
        WriteFileUtils.string2File(testFile1, "Its a beautifull day!!!");
        this.result = RenameFileUtils.renameFile(testFile1, renamedFile1, false);
        assertTrue("File should be renamed.", this.result);
        this.result = FileSearchUtils.containsFile(this.deepDir, renamedFile1);
        assertTrue("The renamed file should exist in this directory.", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#renameFile(java.io.File, java.io.File)} .
     */
    public void testRenameFileFileFile() {
        final String filePrefix1 = "testRenameFileFileFile1";
        final String filePrefix2 = "testRenameFileFileFile2";
        final String oldFileSuffix = ".txt";
        final String newFileSuffix = ".rtf";
        final File testFile1 = new File(this.deepDir, filePrefix1 + oldFileSuffix);
        final File renamedFile1 = new File(this.deepDir, filePrefix2 + newFileSuffix);
        this.result = RenameFileUtils.renameFile(testFile1, renamedFile1);
        assertFalse("File should not exist in this directory.", this.result);
        WriteFileUtils.string2File(testFile1, "Its a beautifull day!!!");
        this.result = RenameFileUtils.renameFile(testFile1, renamedFile1);
        assertTrue("File should be renamed.", this.result);
        this.result = FileSearchUtils.containsFile(this.deepDir, renamedFile1);
        assertTrue("The renamed file should exist in this directory.", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#renameFile(java.io.File, java.lang.String)} .
     */
    public void testRenameFileFileString() {
        final String filePrefix1 = "testRenameFileFileString1";
        final String filePrefix2 = "testRenameFileFileString2";
        final String oldFileSuffix = ".txt";
        final String newFileSuffix = ".rtf";
        final File testFile1 = new File(this.deepDir, filePrefix1 + oldFileSuffix);
        final File renamedFile1 = new File(this.deepDir, filePrefix2 + newFileSuffix);
        WriteFileUtils.string2File(testFile1, "Its a beautifull day!!!");
        this.result = RenameFileUtils.renameFile(testFile1, renamedFile1.getName());
        assertTrue("File should be renamed.", this.result);
        this.result = FileSearchUtils.containsFile(this.deepDir, renamedFile1);
        assertTrue("The renamed file should exist in this directory.", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#renameFileWithSystemtime(java.io.File)} .
     */
    public void testRenameFileWithSystemtime() {
        final String format = "HHmmssSSS";
        final String filePrefix = "testRenameFileWithSystemtime";
        final String fileSuffix = ".txt";
        final File testFile1 = new File(this.testDir, filePrefix + fileSuffix);
        final String inputString = "Its a beautifull day!!!";
        final String ap = testFile1.getAbsolutePath();
        WriteFileUtils.string2File(inputString, ap);
        final Date before = new Date();
        final File compareFile = RenameFileUtils.renameFileWithSystemtime(testFile1);
        final String newFilenameWithSystemtime = compareFile.getName();
        final Date after = new Date();
        final int start = newFilenameWithSystemtime.indexOf("_");
        final int end = newFilenameWithSystemtime.indexOf(fileSuffix);
        final String sysDateFromFile = newFilenameWithSystemtime.substring(start + 1, end);
        final String sysTimeBefore = DateUtils.parseToString(before, format);
        final String sysTimeAfter = DateUtils.parseToString(after, format);
        final Date between = DateUtils.parseToDate(sysDateFromFile, format);
        final Date beforeDate = DateUtils.parseToDate(sysTimeBefore, format);
        final Date afterDate = DateUtils.parseToDate(sysTimeAfter, format);
        this.result = DateUtils.isBetween(beforeDate, afterDate, between);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#storeByteArrayToFile(byte[], java.io.File)} .
     */
    public void testStoreByteArrayToFile() {
        final byte[] expected = { -84, -19, 0, 5, 116, 0, 7, 70, 111, 111, 32, 98, 97, 114 };
        final File destination = new File(this.testDir.getAbsoluteFile(), "testStoreByteArrayToFile.txt");
        WriteFileUtils.storeByteArrayToFile(expected, destination);
        final String compareString = ReadFileUtils.readFromFile(destination);
        final byte[] compare = StringUtils.convertToBytearray(compareString.toCharArray());
        for (int i = 0; i < compare.length; i++) {
            this.result = compare[i] == expected[i];
            assertTrue("", this.result);
        }
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#string2File(java.io.File, java.lang.String)} .
     *
     * @throws DirectoryAllreadyExistsException
     *             the directory allready exists exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void testString2FileFileString() throws DirectoryAllreadyExistsException, IOException {
        final File testFile1 = new File(this.testDir, "testString2FileFileString.txt");
        final String inputString = "Its a beautifull day!!!";
        WriteFileUtils.string2File(testFile1, inputString);
        final String content = ReadFileUtils.readFromFile(testFile1);
        this.result = inputString.equals(content);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#string2File(java.lang.String, java.lang.String)} .
     */
    public void testString2FileStringString() {
        final File testFile1 = new File(this.testDir, "testString2FileStringString.txt");
        final String inputString = "Its a beautifull day!!!";
        WriteFileUtils.string2File(inputString, testFile1.getAbsolutePath());
        final String content = ReadFileUtils.readFromFile(testFile1);
        this.result = inputString.equals(content);
        assertTrue("", this.result);
    }

    /**
     * Test method for.
     *
     * @throws DirectoryAllreadyExistsException the directory allready exists exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws FileDoesNotExistException the file does not exist exception
     * {@link net.sourceforge.jaulp.file.FileUtils#write2File(java.io.InputStream, java.io.OutputStream, boolean)} .
     */
    public void testWrite2FileInputStreamOutputStreamBoolean() throws DirectoryAllreadyExistsException, IOException, FileDoesNotExistException {
        final File inputFile = new File(this.testDir, "testWrite2FileInputStreamOutputStreamBoolean.inp");
        inputFile.createNewFile();
        final File outputFile = new File(this.testDir, "testWrite2FileInputStreamOutputStreamBoolean.outp");
        outputFile.createNewFile();
        final String inputString = "Its a beautifull day!!!";
        WriteFileUtils.string2File(inputFile, inputString);
        final InputStream is = ReadFileUtils.getInputStream(inputFile);
        final OutputStream os = WriteFileUtils.getOutputStream(outputFile);
        StreamUtils.writeInputStreamToOutputStream(is, os, true);
        final String content = ReadFileUtils.readFromFile(outputFile);
        this.result = inputString.equals(content);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#write2File(java.io.Reader, java.io.Writer, boolean)}
     * .
     *
     * @throws DirectoryAllreadyExistsException
     *             the directory allready exists exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void testWrite2FileReaderWriterBoolean() throws DirectoryAllreadyExistsException, IOException {
        final File inputFile = new File(this.testDir, "testWrite2FileReaderWriterBoolean.inp");
        inputFile.createNewFile();
        final File outputFile = new File(this.testDir, "testWrite2FileReaderWriterBoolean.outp");
        outputFile.createNewFile();
        final String inputString = "Its a beautifull day!!!";
        WriteFileUtils.string2File(inputFile, inputString);
        final Reader reader = ReadFileUtils.getReader(inputFile);
        final Writer writer = WriteFileUtils.getWriter(outputFile);
        WriteFileUtils.write2File(reader, writer, true);
        final String content = ReadFileUtils.readFromFile(outputFile);
        this.result = inputString.equals(content);
        assertTrue("", this.result);
    }

    /**
     * Test method for.
     *
     * @throws DirectoryAllreadyExistsException the directory allready exists exception
     * @throws IOException Signals that an I/O exception has occurred.
     * {@link net.sourceforge.jaulp.file.FileUtils#write2File(java.lang.String, java.io.PrintWriter, boolean)} .
     */
    public void testWrite2FileStringPrintWriterBoolean() throws DirectoryAllreadyExistsException, IOException {
        final File inputFile = new File(this.testDir, "testWrite2FileStringPrintWriterBoolean.inp");
        inputFile.createNewFile();
        final File outputFile = new File(this.testDir, "testWrite2FileStringPrintWriterBoolean.outp");
        outputFile.createNewFile();
        final String inputString = "Its a beautifull day!!!";
        WriteFileUtils.string2File(inputFile, inputString);
        final PrintWriter writer = (PrintWriter) WriteFileUtils.getWriter(outputFile);
        final String path = inputFile.getAbsolutePath();
        WriteFileUtils.write2File(path, writer, true);
        final String content = ReadFileUtils.readFromFile(outputFile);
        this.result = inputString.equals(content);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#write2File(java.lang.String, java.lang.String)} .
     *
     * @throws DirectoryAllreadyExistsException
     *             the directory allready exists exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void testWrite2FileStringString() throws DirectoryAllreadyExistsException, IOException {
        boolean created;
        final File inputFile = new File(this.testDir, "testWrite2FileStringString.inp");
        created = inputFile.createNewFile();
        if (!created) {
            fail("Fail to create inputFile.");
        }
        final File outputFile = new File(this.testDir, "testWrite2FileStringString.outp");
        outputFile.createNewFile();
        final String inputString = "Its a beautifull day!!!";
        WriteFileUtils.string2File(inputFile, inputString);
        WriteFileUtils.write2File(inputFile.getAbsolutePath(), outputFile.getAbsolutePath());
        final String content = ReadFileUtils.readFromFile(outputFile);
        this.result = inputString.equals(content);
        assertTrue("", this.result);
    }

    /**
     * Test method for.
     *
     * @throws DirectoryAllreadyExistsException the directory allready exists exception
     * @throws IOException Signals that an I/O exception has occurred.
     * {@link net.sourceforge.jaulp.file.FileUtils#write2FileWithBuffer(java.lang.String, java.lang.String)} .
     */
    public void testWrite2FileWithBuffer() throws DirectoryAllreadyExistsException, IOException {
        final File inputFile = new File(this.testDir, "testWrite2FileWithBuffer.inp");
        inputFile.createNewFile();
        final File outputFile = new File(this.testDir, "testWrite2FileWithBuffer.outp");
        outputFile.createNewFile();
        final String inputString = "Its a beautifull day!!!";
        WriteFileUtils.string2File(inputFile, inputString);
        WriteFileUtils.write2FileWithBuffer(inputFile.getAbsolutePath(), outputFile.getAbsolutePath());
        final String content = ReadFileUtils.readFromFile(outputFile);
        this.result = inputString.equals(content);
        assertTrue("", this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#writeByteArrayToFile(java.io.File, byte[])} .
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void testWriteByteArrayToFileFileByteArray() throws IOException {
        final byte[] expected = { -84, -19, 0, 5, 116, 0, 7, 70, 111, 111, 32, 98, 97, 114 };
        final File destination = new File(this.testDir.getAbsoluteFile(), "testStoreByteArrayToFile.txt");
        WriteFileUtils.writeByteArrayToFile(destination, expected);
        final String compareString = ReadFileUtils.readFromFile(destination);
        final byte[] compare = StringUtils.convertToBytearray(compareString.toCharArray());
        for (int i = 0; i < compare.length; i++) {
            this.result = compare[i] == expected[i];
            assertTrue("", this.result);
        }
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#writeByteArrayToFile(java.lang.String, byte[])} .
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void testWriteByteArrayToFileStringByteArray() throws IOException {
        final byte[] expected = { -84, -19, 0, 5, 116, 0, 7, 70, 111, 111, 32, 98, 97, 114 };
        final File destination = new File(this.testDir.getAbsoluteFile(), "testStoreByteArrayToFile.txt");
        WriteFileUtils.writeByteArrayToFile(destination.getAbsolutePath(), expected);
        final String compareString = ReadFileUtils.readFromFile(destination);
        final byte[] compare = StringUtils.convertToBytearray(compareString.toCharArray());
        for (int i = 0; i < compare.length; i++) {
            this.result = compare[i] == expected[i];
            assertTrue("", this.result);
        }
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#writeLinesToFile(java.util.Collection, java.io.File)}
     * .
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unchecked")
    public void testWriteLinesToFileCollectionFile() throws IOException {
        final List expected = new ArrayList();
        expected.add("test1");
        expected.add("test2");
        expected.add("test3");
        expected.add("bla");
        expected.add("fasel");
        expected.add("and");
        expected.add("so");
        expected.add("on");
        expected.add("test4");
        expected.add("test5");
        expected.add("test6");
        expected.add("foo");
        expected.add("bar");
        expected.add("sim");
        expected.add("sala");
        expected.add("bim");
        final File testFile = new File(this.testResources, "testWriteLinesToFile.lst");
        WriteFileUtils.writeLinesToFile(expected, testFile);
        final List testList = ReadFileUtils.readLinesInList(testFile);
        this.result = expected.equals(testList);
        assertTrue("", this.result);
    }

    /**
     * Test method for.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     * {@link net.sourceforge.jaulp.file.FileUtils#writeLinesToFile(java.io.File, java.util.List, java.lang.String)} .
     */
    @SuppressWarnings("unchecked")
    public void testWriteLinesToFileFileListString() throws IOException {
        final List expected = new ArrayList();
        expected.add("test1");
        expected.add("test2");
        expected.add("test3");
        expected.add("bla");
        expected.add("fasel");
        expected.add("and");
        expected.add("so");
        expected.add("on");
        expected.add("test4");
        expected.add("test5");
        expected.add("test6");
        expected.add("foo");
        expected.add("bar");
        expected.add("sim");
        expected.add("sala");
        expected.add("bim");
        final File testFile = new File(this.testResources, "testWriteLinesToFile.lst");
        WriteFileUtils.writeLinesToFile(testFile, expected, null);
        final List testList = ReadFileUtils.readLinesInList(testFile);
        final boolean result = expected.equals(testList);
        assertTrue("", result);
    }

    /**
     * Test method for.
     *
     * {@link net.sourceforge.jaulp.file.FileUtils#writeProperties2File(java.lang.String, java.util.Properties)} .
     */
    public void testWriteProperties2File() {
        final File tp = new File(this.testResources, "testWriteProperties2File.properties");
        final String ap = tp.getAbsolutePath();
        final Properties properties = new Properties();
        properties.setProperty("testkey1", "testvalue1");
        properties.setProperty("testkey2", "testvalue2");
        properties.setProperty("testkey3", "testvalue3");
        WriteFileUtils.writeProperties2File(ap, properties);
        final Properties compare = ReadFileUtils.readPropertiesFromFile(ap);
        this.result = properties.equals(compare);
        assertTrue(this.result);
    }

    /**
     * Test method for {@link net.sourceforge.jaulp.file.FileUtils#writeStringToFile(File, String, String)} .
     */
    public void testWriteStringToFile() {
        final File source = new File(this.testDir.getAbsoluteFile(), "testWriteStringToFile.txt");
        final String inputString = "Its a beautifull day!!!  ����";
        final String expected = inputString;
        WriteFileUtils.writeStringToFile(source, inputString, null);
        final String compare = ReadFileUtils.readFromFile(source);
        this.result = expected.equals(compare);
        assertTrue("", this.result);
    }
}
