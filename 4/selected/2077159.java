package org.oobench.ejb.common.deploy;

import junit.framework.*;
import java.io.*;

public class FileUtilsTest extends TestCase {

    private static final String TEMP_DIR_NAME = ".tmp";

    public FileUtilsTest(String arg) {
        super(arg);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(FileUtilsTest.class);
    }

    public void makeTempDir() throws Exception {
        cleanUpTempDir();
        File tempDir = new File(TEMP_DIR_NAME);
        try {
            tempDir.mkdir();
        } catch (Exception e) {
            throw new Exception("Cannot make directory: " + tempDir.getAbsolutePath() + ": " + e.toString());
        }
    }

    public void cleanUpTempDir() throws Exception {
        File tempDir = new File(TEMP_DIR_NAME);
        if (tempDir.exists()) {
            try {
                FileUtils.removeDir(tempDir);
            } catch (Exception e) {
                throw new Exception("Cannot delete directory: " + tempDir.getAbsolutePath() + ": " + e.toString());
            }
        }
    }

    public void createFile(File file) throws Exception {
        try {
            FileUtils.createFile(file);
        } catch (IOException ioe) {
            throw new Exception("Could not create file " + file.getAbsolutePath() + ": " + ioe.toString());
        }
    }

    public void testCopySimple() throws Exception {
        makeTempDir();
        File source = new File(TEMP_DIR_NAME + "/sourceFile");
        File dest = new File(TEMP_DIR_NAME + "/dest");
        createFile(source);
        try {
            FileUtils.copyFile(source, dest, false);
        } catch (Exception e) {
            assert (false);
        }
        assert (dest.exists());
        cleanUpTempDir();
    }

    public void testCopyExceptions() throws Exception {
        cleanUpTempDir();
        File source = new File(TEMP_DIR_NAME + "/sourceFile");
        File dest = new File(TEMP_DIR_NAME + "/dest");
        try {
            FileUtils.copyFile(source, dest, false);
            assert (false);
        } catch (DirNotFoundException dnfe) {
        } catch (Exception e) {
            e.printStackTrace();
            assert (false);
        }
        makeTempDir();
        try {
            FileUtils.copyFile(source, dest, false);
            assert (false);
        } catch (FileNotFoundException fnfe) {
        } catch (Exception e) {
            assert (false);
        }
        createFile(source);
        createFile(dest);
        try {
            FileUtils.copyFile(source, dest, false);
            assert (false);
        } catch (FileExistsAlreadyException feae) {
        } catch (Exception e) {
            assert (false);
        }
        assert (dest.exists());
        cleanUpTempDir();
    }

    public void testCopyOverwrite() throws Exception {
        makeTempDir();
        File source = new File(TEMP_DIR_NAME + "/sourceFile");
        File dest = new File(TEMP_DIR_NAME + "/dest");
        createFile(source);
        createFile(dest);
        try {
            FileUtils.copyFile(source, dest, true);
        } catch (Exception e) {
            assert (false);
        }
        cleanUpTempDir();
    }
}
