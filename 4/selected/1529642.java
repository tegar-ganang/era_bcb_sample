package org.paccman.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ferreira
 */
public class FileUtilsTest {

    public FileUtilsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of copyFile method, of class FileUtils.
     */
    @Test
    public void copyFile() throws Exception {
        System.out.println("copyFile");
        InputStream is = null;
        OutputStream os = null;
        FileUtils.copyFile(is, os);
    }

    /**
     * Test of zipDirectory method, of class FileUtils.
     */
    @Test
    public void zipDirectory() throws Exception {
        System.out.println("zipDirectory");
        File srcDir = new File("T:\\TZipDir");
        File destFile = new File("T:\\paccmantestzipfile.zip");
        if (destFile.exists()) {
            destFile.delete();
        }
        FileUtils.zipDirectory(srcDir, destFile);
    }

    /**
     * Test of unzipDirectory method, of class FileUtils.
     */
    @Test
    public void unzipDirectory() {
        try {
            System.out.println("unzipDirectory");
            File srcZip = new File("T:\\paccmantestzipfile.zip");
            File destDir = new File("T:\\TUnzipDir");
            FileUtils.unzipDirectory(srcZip, destDir);
        } catch (IOException ex) {
            fail("Exception caught: " + ex);
        }
    }
}
