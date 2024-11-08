package net.sourceforge.jaulp.file.copy;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import net.sourceforge.jaulp.file.FileTestCase;
import net.sourceforge.jaulp.file.create.CreateFileUtils;
import net.sourceforge.jaulp.file.exceptions.DirectoryAllreadyExistsException;
import net.sourceforge.jaulp.file.exceptions.FileIsADirectoryException;
import net.sourceforge.jaulp.file.exceptions.FileIsNotADirectoryException;
import net.sourceforge.jaulp.file.exceptions.FileIsSecurityRestrictedException;
import net.sourceforge.jaulp.file.filter.MultiplyExtensionsFileFilter;
import net.sourceforge.jaulp.file.filter.TxtFileFilter;
import net.sourceforge.jaulp.file.namefilter.MultiplyExtensionsFilenameFilter;
import net.sourceforge.jaulp.file.namefilter.SimpleFilenameFilter;
import net.sourceforge.jaulp.file.read.ReadFileUtils;
import net.sourceforge.jaulp.file.write.WriteFileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The Class CopyFileUtilsTest.
 *
 * @version 1.0
 *
 * @author Asterios Raptis
 *
 */
public class CopyFileUtilsTest extends FileTestCase {

    /**
	 * Sets the up.
	 *
	 * @throws Exception the exception
	 */
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
	 * Tear down.
	 *
	 * @throws Exception the exception
	 */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
	 * Test method for {@link net.sourceforge.jaulp.file.copy.CopyFileUtils#copyFile(java.io.File, java.io.File)}.
	 *
	 * @throws IOException Is thrown if an error occurs by reading or writing.
	 * @throws FileIsADirectoryException Is thrown if the destination file is a directory.
	 */
    @Test
    public void testCopyFileFileFile() throws IOException, FileIsADirectoryException {
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
        assertTrue("Source file " + source.getName() + " was not copied in the destination file " + destination.getName() + ".", this.result);
        final String compare = ReadFileUtils.readFromFile(destination);
        this.result = expected.equals(compare);
        assertTrue("The content from the source file " + source.getName() + " is not the same as the destination file " + destination.getName() + ".", this.result);
    }

    /**
	 * Test method for {@link net.sourceforge.jaulp.file.copy.CopyFileUtils#copyFile(java.io.File, java.io.File, boolean)}.
	 * @throws IOException Is thrown if an error occurs by reading or writing.
     * @throws FileIsADirectoryException Is thrown if the destination file is a directory.
	 */
    @Test
    public void testCopyFileFileFileBoolean() throws IOException, FileIsADirectoryException {
        final File source = new File(this.testDir.getAbsoluteFile(), "testCopyFileInput.txt");
        final File destination = new File(this.testDir.getAbsoluteFile(), "testCopyFileOutput.tft");
        try {
            this.result = CopyFileUtils.copyFile(source, destination, false);
            assertFalse("", this.result);
        } catch (final Exception fnfe) {
            this.result = fnfe instanceof FileNotFoundException;
            assertTrue("", this.result);
        }
        final String inputString = "Its a beautifull day!!!";
        final String expected = inputString;
        WriteFileUtils.string2File(source, inputString);
        this.result = CopyFileUtils.copyFile(source, destination, false);
        assertTrue("Source file " + source.getName() + " was not copied in the destination file " + destination.getName() + ".", this.result);
        final String compare = ReadFileUtils.readFromFile(destination);
        this.result = expected.equals(compare);
        assertTrue("The content from the source file " + source.getName() + " is not the same as the destination file " + destination.getName() + ".", this.result);
    }

    /**
	 * Test method for {@link net.sourceforge.jaulp.file.copy.CopyFileUtils#copyDirectory(java.io.File, java.io.File)}.
	 *
	 * @throws DirectoryAllreadyExistsException Is thrown if the directory all ready exists.
	 * @throws FileIsSecurityRestrictedException Is thrown if the source file is security restricted.
	 * @throws IOException Is thrown if an error occurs by reading or writing.
	 * @throws FileIsADirectoryException Is thrown if the destination file is a directory.
	 * @throws FileIsNotADirectoryException Is thrown if the source file is not a directory.
	 */
    @Test
    public void testCopyDirectoryFileFile() throws DirectoryAllreadyExistsException, FileIsSecurityRestrictedException, IOException, FileIsADirectoryException, FileIsNotADirectoryException {
        String dirToCopyName = "dirToCopy";
        final File srcDeepDir = new File(this.deepDir, dirToCopyName);
        File destDir = new File(this.deeperDir, dirToCopyName);
        final String filePrefix = "testCopyFile";
        final String txtSuffix = ".txt";
        File srcDeepFile = new File(srcDeepDir, filePrefix + txtSuffix);
        if (!srcDeepDir.exists()) {
            boolean created = CreateFileUtils.createDirectory(srcDeepDir);
            assertTrue("The directory " + srcDeepDir.getAbsolutePath() + " should be created.", created);
            WriteFileUtils.string2File(srcDeepFile, "Its a beautifull day!!!");
        }
        String deepestDirName = "deepest";
        File srcDeepestDir = new File(srcDeepDir, deepestDirName);
        String deepestFilename = "test" + txtSuffix;
        File srcDeepestFile = new File(srcDeepestDir, deepestFilename);
        if (!srcDeepestDir.exists()) {
            boolean created = CreateFileUtils.createDirectory(srcDeepestDir);
            assertTrue("The directory " + srcDeepestDir.getAbsolutePath() + " should be created.", created);
            WriteFileUtils.string2File(srcDeepestFile, "Its a beautifull night!!!");
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        this.result = CopyFileUtils.copyDirectory(srcDeepDir, destDir);
        assertTrue("Directory " + destDir.getAbsolutePath() + " should be copied.", this.result);
        File expectedDeeperDir = new File(this.deeperDir, dirToCopyName);
        assertTrue("Directory " + expectedDeeperDir.getAbsolutePath() + " should be copied.", expectedDeeperDir.exists());
        File expectedDeeperFile = new File(expectedDeeperDir, filePrefix + txtSuffix);
        assertTrue("File " + expectedDeeperFile.getAbsolutePath() + " should be copied.", expectedDeeperFile.exists());
        assertTrue("long lastModified was not set.", srcDeepFile.lastModified() == expectedDeeperFile.lastModified());
        File expectedDeepestDir = new File(expectedDeeperDir, deepestDirName);
        assertTrue("Directory " + expectedDeepestDir.getAbsolutePath() + " should be copied.", expectedDeepestDir.exists());
        File expectedDeepestFile = new File(expectedDeepestDir, deepestFilename);
        assertTrue("File " + expectedDeepestFile.getAbsolutePath() + " should be copied.", expectedDeepestFile.exists());
        assertTrue("long lastModified was not set.", srcDeepestFile.lastModified() == expectedDeepestFile.lastModified());
    }

    /**
	 * Test method for {@link net.sourceforge.jaulp.file.copy.CopyFileUtils#copyDirectory(java.io.File, java.io.File, boolean)}.
	 * @throws FileIsSecurityRestrictedException Is thrown if the source file is security restricted.
     * @throws IOException Is thrown if an error occurs by reading or writing.
     * @throws FileIsADirectoryException Is thrown if the destination file is a directory.
     * @throws FileIsNotADirectoryException Is thrown if the source file is not a directory.
     * @throws DirectoryAllreadyExistsException Is thrown if the directory all ready exists.
	 */
    @Test
    public void testCopyDirectoryFileFileBoolean() throws FileIsSecurityRestrictedException, IOException, FileIsADirectoryException, FileIsNotADirectoryException, DirectoryAllreadyExistsException {
        String dirToCopyName = "dirToCopy";
        final File srcDir = new File(this.deepDir, dirToCopyName);
        File destDir = new File(this.deeperDir, dirToCopyName);
        final String filePrefix = "testCopyFile";
        final String txtSuffix = ".txt";
        File srcFile = new File(srcDir, filePrefix + txtSuffix);
        if (!srcDir.exists()) {
            boolean created = CreateFileUtils.createDirectory(srcDir);
            assertTrue("The directory " + srcDir.getAbsolutePath() + " should be created.", created);
            WriteFileUtils.string2File(srcFile, "Its a beautifull day!!!");
        }
        String deepestDirName = "deepest";
        File srcDeepestDir = new File(srcDir, deepestDirName);
        String deepestFilename = "test" + txtSuffix;
        File srcDeepestFile = new File(srcDeepestDir, deepestFilename);
        if (!srcDeepestDir.exists()) {
            boolean created = CreateFileUtils.createDirectory(srcDeepestDir);
            assertTrue("The directory " + srcDeepestDir.getAbsolutePath() + " should be created.", created);
            WriteFileUtils.string2File(srcDeepestFile, "Its a beautifull night!!!");
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        this.result = CopyFileUtils.copyDirectory(srcDir, destDir, false);
        assertTrue("Directory " + destDir.getAbsolutePath() + " should be copied.", this.result);
        File expectedDeeperDir = new File(this.deeperDir, dirToCopyName);
        assertTrue("Directory " + expectedDeeperDir.getAbsolutePath() + " should be copied.", expectedDeeperDir.exists());
        assertFalse("long lastModified was not set.", srcDir.lastModified() == expectedDeeperDir.lastModified());
        File expectedDeeperFile = new File(expectedDeeperDir, filePrefix + txtSuffix);
        assertTrue("File " + expectedDeeperFile.getAbsolutePath() + " should be copied.", expectedDeeperFile.exists());
        assertFalse("long lastModified was not set.", srcFile.lastModified() == expectedDeeperFile.lastModified());
        File expectedDeepestDir = new File(expectedDeeperDir, deepestDirName);
        assertTrue("Directory " + expectedDeepestDir.getAbsolutePath() + " should be copied.", expectedDeepestDir.exists());
        assertFalse("long lastModified was not set.", srcDeepestDir.lastModified() == expectedDeepestDir.lastModified());
        File expectedDeepestFile = new File(expectedDeepestDir, deepestFilename);
        assertTrue("File " + expectedDeepestFile.getAbsolutePath() + " should be copied.", expectedDeepestFile.exists());
        assertFalse("long lastModified was not set.", srcDeepestFile.lastModified() == expectedDeepestFile.lastModified());
    }

    /**
	 * Test method for {@link net.sourceforge.jaulp.file.copy.CopyFileUtils#copyDirectoryWithFileFilter(java.io.File, java.io.File, java.io.FileFilter, boolean)}.
	 * @throws FileIsSecurityRestrictedException Is thrown if the source file is security restricted.
     * @throws IOException Is thrown if an error occurs by reading or writing.
     * @throws FileIsADirectoryException Is thrown if the destination file is a directory.
     * @throws FileIsNotADirectoryException Is thrown if the source file is not a directory.
     * @throws DirectoryAllreadyExistsException Is thrown if the directory all ready exists.
	 */
    @Test
    public void testCopyDirectoryWithFileFilter() throws FileIsSecurityRestrictedException, IOException, FileIsADirectoryException, FileIsNotADirectoryException, DirectoryAllreadyExistsException {
        String dirToCopyName = "dirToCopy";
        final File srcDir = new File(this.deepDir, dirToCopyName);
        File destDir = new File(this.deeperDir, dirToCopyName);
        final String filePrefix = "testCopyFile";
        final String txtSuffix = ".txt";
        final String rtfSuffix = ".rtf";
        File srcFile1 = new File(srcDir, filePrefix + txtSuffix);
        File srcFile2 = new File(srcDir, filePrefix + rtfSuffix);
        if (!srcDir.exists()) {
            boolean created = CreateFileUtils.createDirectory(srcDir);
            assertTrue("The directory " + srcDir.getAbsolutePath() + " should be created.", created);
            WriteFileUtils.string2File(srcFile1, "Its a beautifull day!!!");
            WriteFileUtils.string2File(srcFile2, "Its a beautifull night!!!");
        }
        String deepestDirName = "deepest";
        File srcDeepestDir = new File(srcDir, deepestDirName);
        String srcDeepestFileName1 = "test1" + txtSuffix;
        String srcDeepestFileName2 = "test2" + rtfSuffix;
        File srcDeepestFile1 = new File(srcDeepestDir, srcDeepestFileName1);
        File srcDeepestFile2 = new File(srcDeepestDir, srcDeepestFileName2);
        if (!srcDeepestDir.exists()) {
            boolean created = CreateFileUtils.createDirectory(srcDeepestDir);
            assertTrue("The directory " + srcDeepestDir.getAbsolutePath() + " should be created.", created);
            WriteFileUtils.string2File(srcDeepestFile1, "Its a beautifull day!!!");
            WriteFileUtils.string2File(srcDeepestFile2, "Its a beautifull night!!!");
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        FileFilter fileFilter = new TxtFileFilter();
        this.result = CopyFileUtils.copyDirectoryWithFileFilter(srcDir, destDir, fileFilter, false);
        assertTrue("Directory " + destDir.getAbsolutePath() + " should be copied.", this.result);
        File expectedDeeperDir = new File(this.deeperDir, dirToCopyName);
        assertTrue("Directory " + expectedDeeperDir.getAbsolutePath() + " should be copied.", expectedDeeperDir.exists());
        assertFalse("long lastModified was not set.", srcDir.lastModified() == expectedDeeperDir.lastModified());
        File expectedDeeperFile = new File(expectedDeeperDir, filePrefix + txtSuffix);
        assertTrue("File " + expectedDeeperFile.getAbsolutePath() + " should be copied.", expectedDeeperFile.exists());
        assertFalse("long lastModified was not set.", srcFile1.lastModified() == expectedDeeperFile.lastModified());
        File notCopied1 = new File(expectedDeeperDir, filePrefix + rtfSuffix);
        assertFalse("File " + notCopied1.getAbsolutePath() + " should not be copied.", notCopied1.exists());
        File expectedDeepestDir = new File(expectedDeeperDir, deepestDirName);
        assertTrue("Directory " + expectedDeepestDir.getAbsolutePath() + " should be copied.", expectedDeepestDir.exists());
        assertFalse("long lastModified was not set.", srcDeepestDir.lastModified() == expectedDeepestDir.lastModified());
        File expectedDeepestFile1 = new File(expectedDeepestDir, srcDeepestFileName1);
        assertTrue("File " + expectedDeepestFile1.getAbsolutePath() + " should be copied.", expectedDeepestFile1.exists());
        assertFalse("long lastModified was not set.", srcDeepestFile1.lastModified() == expectedDeepestFile1.lastModified());
        File notExpectedDeepestFile2 = new File(expectedDeepestDir, srcDeepestFileName2);
        assertFalse("File " + notExpectedDeepestFile2.getAbsolutePath() + " should not be copied.", notExpectedDeepestFile2.exists());
    }

    /**
	 * Test method for {@link net.sourceforge.jaulp.file.copy.CopyFileUtils#copyDirectoryWithFileFilter(java.io.File, java.io.File, java.io.FileFilter, java.io.FileFilter, boolean)}.
	 * @throws FileIsSecurityRestrictedException Is thrown if the source file is security restricted.
     * @throws IOException Is thrown if an error occurs by reading or writing.
     * @throws FileIsADirectoryException Is thrown if the destination file is a directory.
     * @throws FileIsNotADirectoryException Is thrown if the source file is not a directory.
     * @throws DirectoryAllreadyExistsException Is thrown if the directory all ready exists.
	 */
    @Test
    public void testCopyDirectoryWithFileFilters() throws FileIsSecurityRestrictedException, IOException, FileIsADirectoryException, FileIsNotADirectoryException, DirectoryAllreadyExistsException {
        String dirToCopyName = "dirToCopy";
        final File srcDir = new File(this.deepDir, dirToCopyName);
        File destDir = new File(this.deeperDir, dirToCopyName);
        final String filePrefix = "testCopyFile";
        final String txtSuffix = ".txt";
        final String rtfSuffix = ".rtf";
        final String exeSuffix = ".exe";
        File srcFile1 = new File(srcDir, filePrefix + txtSuffix);
        File srcFile2 = new File(srcDir, filePrefix + rtfSuffix);
        File srcFile3 = new File(srcDir, filePrefix + exeSuffix);
        if (!srcDir.exists()) {
            boolean created = CreateFileUtils.createDirectory(srcDir);
            assertTrue("The directory " + srcDir.getAbsolutePath() + " should be created.", created);
            WriteFileUtils.string2File(srcFile1, "Its a beautifull day!!!");
            WriteFileUtils.string2File(srcFile2, "Its a beautifull night!!!");
            WriteFileUtils.string2File(srcFile3, "Its a beautifull exe morning!!!");
        }
        String deepestDirName = "deepest";
        File srcDeepestDir = new File(srcDir, deepestDirName);
        String srcDeepestFileName1 = "test1" + txtSuffix;
        String srcDeepestFileName2 = "test2" + rtfSuffix;
        String srcDeepestFileName3 = "test3" + exeSuffix;
        File srcDeepestFile1 = new File(srcDeepestDir, srcDeepestFileName1);
        File srcDeepestFile2 = new File(srcDeepestDir, srcDeepestFileName2);
        File srcDeepestFile3 = new File(srcDeepestDir, srcDeepestFileName3);
        if (!srcDeepestDir.exists()) {
            boolean created = CreateFileUtils.createDirectory(srcDeepestDir);
            assertTrue("The directory " + srcDeepestDir.getAbsolutePath() + " should be created.", created);
            WriteFileUtils.string2File(srcDeepestFile1, "Its a beautifull day!!!");
            WriteFileUtils.string2File(srcDeepestFile2, "Its a beautifull night!!!");
            WriteFileUtils.string2File(srcDeepestFile3, "Its a beautifull exe morning!!!");
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        FileFilter includeFileFilter = new MultiplyExtensionsFileFilter(Arrays.asList(new String[] { ".txt", ".rtf" }), true);
        FileFilter excludeFileFilter = new MultiplyExtensionsFileFilter(Arrays.asList(new String[] { ".exe" }));
        this.result = CopyFileUtils.copyDirectoryWithFileFilter(srcDir, destDir, includeFileFilter, excludeFileFilter, false);
        assertTrue("Directory " + destDir.getAbsolutePath() + " should be copied.", this.result);
        File expectedDeeperDir = new File(this.deeperDir, dirToCopyName);
        assertTrue("Directory " + expectedDeeperDir.getAbsolutePath() + " should be copied.", expectedDeeperDir.exists());
        assertFalse("long lastModified was not set.", srcDir.lastModified() == expectedDeeperDir.lastModified());
        File expectedDeeperFile = new File(expectedDeeperDir, filePrefix + txtSuffix);
        assertTrue("File " + expectedDeeperFile.getAbsolutePath() + " should be copied.", expectedDeeperFile.exists());
        assertFalse("long lastModified was not set.", srcFile1.lastModified() == expectedDeeperFile.lastModified());
        File expectedDeeperFile2 = new File(expectedDeeperDir, filePrefix + rtfSuffix);
        assertTrue("File " + expectedDeeperFile2.getAbsolutePath() + " should be copied.", expectedDeeperFile2.exists());
        assertFalse("long lastModified was not set.", srcFile1.lastModified() == expectedDeeperFile2.lastModified());
        File notExpectedDeeperFile1 = new File(expectedDeeperDir, filePrefix + exeSuffix);
        assertFalse("File " + notExpectedDeeperFile1.getAbsolutePath() + " should not be copied.", notExpectedDeeperFile1.exists());
        File expectedDeepestDir = new File(expectedDeeperDir, deepestDirName);
        assertTrue("Directory " + expectedDeepestDir.getAbsolutePath() + " should be copied.", expectedDeepestDir.exists());
        assertFalse("long lastModified was not set.", srcDeepestDir.lastModified() == expectedDeepestDir.lastModified());
        File expectedDeepestFile1 = new File(expectedDeepestDir, srcDeepestFileName1);
        assertTrue("File " + expectedDeepestFile1.getAbsolutePath() + " should be copied.", expectedDeepestFile1.exists());
        assertFalse("long lastModified was not set.", srcDeepestFile1.lastModified() == expectedDeepestFile1.lastModified());
        File expectedDeepestFile2 = new File(expectedDeepestDir, srcDeepestFileName2);
        assertTrue("File " + expectedDeepestFile2.getAbsolutePath() + " should be copied.", expectedDeepestFile2.exists());
        assertFalse("long lastModified was not set.", srcDeepestFile2.lastModified() == expectedDeepestFile2.lastModified());
        File notExpectedDeepestFile3 = new File(expectedDeepestDir, srcDeepestFileName3);
        assertFalse("File " + notExpectedDeepestFile3.getAbsolutePath() + " should not be copied.", notExpectedDeepestFile3.exists());
    }

    /**
	 * Test method for {@link net.sourceforge.jaulp.file.copy.CopyFileUtils#copyDirectoryWithFilenameFilter(java.io.File, java.io.File, java.io.FilenameFilter, java.io.FilenameFilter, boolean)}.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws FileIsNotADirectoryException the file is not a directory exception
	 * @throws FileIsADirectoryException the file is a directory exception
	 * @throws FileIsSecurityRestrictedException the file is security restricted exception
	 * @throws DirectoryAllreadyExistsException the directory allready exists exception
	 */
    @Test
    public void testCopyDirectoryWithFilenameFilters() throws IOException, FileIsNotADirectoryException, FileIsADirectoryException, FileIsSecurityRestrictedException, DirectoryAllreadyExistsException {
        String dirToCopyName = "dirToCopy";
        final File srcDir = new File(this.deepDir, dirToCopyName);
        File destDir = new File(this.deeperDir, dirToCopyName);
        final String filePrefix = "testCopyFile";
        final String txtSuffix = ".txt";
        final String rtfSuffix = ".rtf";
        final String exeSuffix = ".exe";
        File srcFile1 = new File(srcDir, filePrefix + txtSuffix);
        File srcFile2 = new File(srcDir, filePrefix + rtfSuffix);
        File srcFile3 = new File(srcDir, filePrefix + exeSuffix);
        if (!srcDir.exists()) {
            boolean created = CreateFileUtils.createDirectory(srcDir);
            assertTrue("The directory " + srcDir.getAbsolutePath() + " should be created.", created);
            WriteFileUtils.string2File(srcFile1, "Its a beautifull day!!!");
            WriteFileUtils.string2File(srcFile2, "Its a beautifull night!!!");
            WriteFileUtils.string2File(srcFile3, "Its a beautifull exe morning!!!");
        }
        String deepestDirName = "deepest";
        File srcDeepestDir = new File(srcDir, deepestDirName);
        String srcDeepestFileName1 = "test1" + txtSuffix;
        String srcDeepestFileName2 = "test2" + rtfSuffix;
        String srcDeepestFileName3 = "test3" + exeSuffix;
        File srcDeepestFile1 = new File(srcDeepestDir, srcDeepestFileName1);
        File srcDeepestFile2 = new File(srcDeepestDir, srcDeepestFileName2);
        File srcDeepestFile3 = new File(srcDeepestDir, srcDeepestFileName3);
        if (!srcDeepestDir.exists()) {
            boolean created = CreateFileUtils.createDirectory(srcDeepestDir);
            assertTrue("The directory " + srcDeepestDir.getAbsolutePath() + " should be created.", created);
            WriteFileUtils.string2File(srcDeepestFile1, "Its a beautifull day!!!");
            WriteFileUtils.string2File(srcDeepestFile2, "Its a beautifull night!!!");
            WriteFileUtils.string2File(srcDeepestFile3, "Its a beautifull exe morning!!!");
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        FilenameFilter includeFilenameFilter = new MultiplyExtensionsFilenameFilter(Arrays.asList(new String[] { ".txt", ".rtf" }), true);
        FilenameFilter excludeFilenameFilter = new MultiplyExtensionsFilenameFilter(Arrays.asList(new String[] { ".exe" }));
        this.result = CopyFileUtils.copyDirectoryWithFilenameFilter(srcDir, destDir, includeFilenameFilter, excludeFilenameFilter, false);
        assertTrue("Directory " + destDir.getAbsolutePath() + " should be copied.", this.result);
        File expectedDeeperDir = new File(this.deeperDir, dirToCopyName);
        assertTrue("Directory " + expectedDeeperDir.getAbsolutePath() + " should be copied.", expectedDeeperDir.exists());
        assertFalse("long lastModified was not set.", srcDir.lastModified() == expectedDeeperDir.lastModified());
        File expectedDeeperFile = new File(expectedDeeperDir, filePrefix + txtSuffix);
        assertTrue("File " + expectedDeeperFile.getAbsolutePath() + " should be copied.", expectedDeeperFile.exists());
        assertFalse("long lastModified was not set.", srcFile1.lastModified() == expectedDeeperFile.lastModified());
        File expectedDeeperFile2 = new File(expectedDeeperDir, filePrefix + rtfSuffix);
        assertTrue("File " + expectedDeeperFile2.getAbsolutePath() + " should be copied.", expectedDeeperFile2.exists());
        assertFalse("long lastModified was not set.", srcFile1.lastModified() == expectedDeeperFile2.lastModified());
        File notExpectedDeeperFile1 = new File(expectedDeeperDir, filePrefix + exeSuffix);
        assertFalse("File " + notExpectedDeeperFile1.getAbsolutePath() + " should not be copied.", notExpectedDeeperFile1.exists());
        File expectedDeepestDir = new File(expectedDeeperDir, deepestDirName);
        assertTrue("Directory " + expectedDeepestDir.getAbsolutePath() + " should be copied.", expectedDeepestDir.exists());
        assertFalse("long lastModified was not set.", srcDeepestDir.lastModified() == expectedDeepestDir.lastModified());
        File expectedDeepestFile1 = new File(expectedDeepestDir, srcDeepestFileName1);
        assertTrue("File " + expectedDeepestFile1.getAbsolutePath() + " should be copied.", expectedDeepestFile1.exists());
        assertFalse("long lastModified was not set.", srcDeepestFile1.lastModified() == expectedDeepestFile1.lastModified());
        File expectedDeepestFile2 = new File(expectedDeepestDir, srcDeepestFileName2);
        assertTrue("File " + expectedDeepestFile2.getAbsolutePath() + " should be copied.", expectedDeepestFile2.exists());
        assertFalse("long lastModified was not set.", srcDeepestFile2.lastModified() == expectedDeepestFile2.lastModified());
        File notExpectedDeepestFile3 = new File(expectedDeepestDir, srcDeepestFileName3);
        assertFalse("File " + notExpectedDeepestFile3.getAbsolutePath() + " should not be copied.", notExpectedDeepestFile3.exists());
    }

    /**
	 * Test method for {@link net.sourceforge.jaulp.file.copy.CopyFileUtils#copyDirectoryWithFilenameFilter(java.io.File, java.io.File, java.io.FilenameFilter, boolean)}.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws FileIsNotADirectoryException the file is not a directory exception
	 * @throws FileIsADirectoryException the file is a directory exception
	 * @throws FileIsSecurityRestrictedException the file is security restricted exception
	 * @throws DirectoryAllreadyExistsException the directory allready exists exception
	 */
    @Test
    public void testCopyDirectoryWithFilenameFilter() throws IOException, FileIsNotADirectoryException, FileIsADirectoryException, FileIsSecurityRestrictedException, DirectoryAllreadyExistsException {
        String dirToCopyName = "dirToCopy";
        final File srcDir = new File(this.deepDir, dirToCopyName);
        File destDir = new File(this.deeperDir, dirToCopyName);
        final String filePrefix = "testCopyFile";
        final String txtSuffix = ".txt";
        final String rtfSuffix = ".rtf";
        File srcFile1 = new File(srcDir, filePrefix + txtSuffix);
        File srcFile2 = new File(srcDir, filePrefix + rtfSuffix);
        if (!srcDir.exists()) {
            boolean created = CreateFileUtils.createDirectory(srcDir);
            assertTrue("The directory " + srcDir.getAbsolutePath() + " should be created.", created);
            WriteFileUtils.string2File(srcFile1, "Its a beautifull day!!!");
            WriteFileUtils.string2File(srcFile2, "Its a beautifull night!!!");
        }
        String deepestDirName = "deepest";
        File srcDeepestDir = new File(srcDir, deepestDirName);
        String srcDeepestFileName1 = "test1" + txtSuffix;
        String srcDeepestFileName2 = "test2" + rtfSuffix;
        File srcDeepestFile1 = new File(srcDeepestDir, srcDeepestFileName1);
        File srcDeepestFile2 = new File(srcDeepestDir, srcDeepestFileName2);
        if (!srcDeepestDir.exists()) {
            boolean created = CreateFileUtils.createDirectory(srcDeepestDir);
            assertTrue("The directory " + srcDeepestDir.getAbsolutePath() + " should be created.", created);
            WriteFileUtils.string2File(srcDeepestFile1, "Its a beautifull day!!!");
            WriteFileUtils.string2File(srcDeepestFile2, "Its a beautifull night!!!");
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        FilenameFilter fileFilter = new SimpleFilenameFilter(".txt", true);
        this.result = CopyFileUtils.copyDirectoryWithFilenameFilter(srcDir, destDir, fileFilter, false);
        assertTrue("Directory " + destDir.getAbsolutePath() + " should be copied.", this.result);
        File expectedDeeperDir = new File(this.deeperDir, dirToCopyName);
        assertTrue("Directory " + expectedDeeperDir.getAbsolutePath() + " should be copied.", expectedDeeperDir.exists());
        assertFalse("long lastModified was not set.", srcDir.lastModified() == expectedDeeperDir.lastModified());
        File expectedDeeperFile = new File(expectedDeeperDir, filePrefix + txtSuffix);
        assertTrue("File " + expectedDeeperFile.getAbsolutePath() + " should be copied.", expectedDeeperFile.exists());
        assertFalse("long lastModified was not set.", srcFile1.lastModified() == expectedDeeperFile.lastModified());
        File notCopied1 = new File(expectedDeeperDir, filePrefix + rtfSuffix);
        assertFalse("File " + notCopied1.getAbsolutePath() + " should not be copied.", notCopied1.exists());
        File expectedDeepestDir = new File(expectedDeeperDir, deepestDirName);
        assertTrue("Directory " + expectedDeepestDir.getAbsolutePath() + " should be copied.", expectedDeepestDir.exists());
        assertFalse("long lastModified was not set.", srcDeepestDir.lastModified() == expectedDeepestDir.lastModified());
        File expectedDeepestFile1 = new File(expectedDeepestDir, srcDeepestFileName1);
        assertTrue("File " + expectedDeepestFile1.getAbsolutePath() + " should be copied.", expectedDeepestFile1.exists());
        assertFalse("long lastModified was not set.", srcDeepestFile1.lastModified() == expectedDeepestFile1.lastModified());
        File notExpectedDeepestFile2 = new File(expectedDeepestDir, srcDeepestFileName2);
        assertFalse("File " + notExpectedDeepestFile2.getAbsolutePath() + " should not be copied.", notExpectedDeepestFile2.exists());
    }

    /**
	 * Test method for {@link net.sourceforge.jaulp.file.copy.CopyFileUtils#copyFileToDirectory(java.io.File, java.io.File)}.
	 *
	 * @throws DirectoryAllreadyExistsException the directory allready exists exception
	 * @throws FileIsNotADirectoryException the file is not a directory exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws FileIsADirectoryException the file is a directory exception
	 */
    @Test
    public void testCopyFileToDirectoryFileFile() throws DirectoryAllreadyExistsException, FileIsNotADirectoryException, IOException, FileIsADirectoryException {
        String dirToCopyName = "dirToCopy";
        final File srcDir = new File(this.deepDir, dirToCopyName);
        final String filePrefix = "testCopyFile";
        final String txtSuffix = ".txt";
        File srcFile = new File(this.testDir, filePrefix + txtSuffix);
        WriteFileUtils.string2File(srcFile, "Its a beautifull day!!!");
        if (!srcDir.exists()) {
            boolean created = CreateFileUtils.createDirectory(srcDir);
            assertTrue("The directory " + srcDir.getAbsolutePath() + " should be created.", created);
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        this.result = CopyFileUtils.copyFileToDirectory(srcFile, srcDir);
        final File expectedCopiedFile = new File(srcDir, filePrefix + txtSuffix);
        assertTrue("File " + expectedCopiedFile.getAbsolutePath() + " should be copied.", expectedCopiedFile.exists());
        assertTrue("long lastModified is not the same.", srcFile.lastModified() == expectedCopiedFile.lastModified());
    }

    /**
	 * Test method for {@link net.sourceforge.jaulp.file.copy.CopyFileUtils#copyFileToDirectory(java.io.File, java.io.File, boolean)}.
	 *
	 * @throws FileIsNotADirectoryException the file is not a directory exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws FileIsADirectoryException the file is a directory exception
	 * @throws DirectoryAllreadyExistsException the directory allready exists exception
	 */
    @Test
    public void testCopyFileToDirectoryFileFileBoolean() throws FileIsNotADirectoryException, IOException, FileIsADirectoryException, DirectoryAllreadyExistsException {
        String dirToCopyName = "dirToCopy";
        final File srcDir = new File(this.deepDir, dirToCopyName);
        final String filePrefix = "testCopyFile";
        final String txtSuffix = ".txt";
        File srcFile = new File(this.testDir, filePrefix + txtSuffix);
        WriteFileUtils.string2File(srcFile, "Its a beautifull day!!!");
        if (!srcDir.exists()) {
            boolean created = CreateFileUtils.createDirectory(srcDir);
            assertTrue("The directory " + srcDir.getAbsolutePath() + " should be created.", created);
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        this.result = CopyFileUtils.copyFileToDirectory(srcFile, srcDir, false);
        final File expectedCopiedFile = new File(srcDir, filePrefix + txtSuffix);
        assertTrue("File " + expectedCopiedFile.getAbsolutePath() + " should be copied.", expectedCopiedFile.exists());
        assertTrue("long lastModified was set.", srcFile.lastModified() != expectedCopiedFile.lastModified());
    }
}
