package net.sf.cantina.util;

import junit.framework.TestCase;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * @author Stephane JAIS
 */
public class FileUtilsTest extends TestCase {

    public void testDeleteDir() throws Exception {
        File dir = new File("/tmp/" + this.getClass().getName());
        assertTrue(dir.mkdir());
        File file1 = new File(dir.getAbsolutePath() + "/file1");
        assertTrue(file1.createNewFile());
        File dir1 = new File(dir.getAbsolutePath() + "/dir1");
        assertTrue(dir1.mkdir());
        File file2 = new File(dir1.getAbsolutePath() + "/file2");
        assertTrue(file2.createNewFile());
        assertTrue(FileUtils.deleteDir(dir));
        assertFalse(dir.exists());
    }

    public void testCopyFile() throws Exception {
        String origFileName = "/tmp/utest-cantina-FileUtils";
        String copyFileName = "/tmp/utest-cantina-FileUtils-copy";
        File orig = new File(origFileName);
        File dest = new File(copyFileName);
        Writer opw = new PrintWriter(new FileOutputStream(orig));
        opw.write("coucou!\n");
        opw.close();
        Writer dpw = new PrintWriter(new FileOutputStream(dest));
        dpw.write("yoyoyoyoyoyoyoyoyoyoyoyoyo!\n");
        dpw.close();
        assertFalse(FileUtils.copyFile(origFileName, copyFileName, false));
        assertFalse(orig.length() == dest.length());
        assertTrue(FileUtils.copyFile(origFileName, copyFileName, true));
        File copy = new File(copyFileName);
        assertTrue("File exists", copy.exists());
        assertEquals(copy.length(), orig.length());
    }
}
