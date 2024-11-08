package com.webstersmalley.picweb.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Matthew Smalley
 */
public class IOUtilsTest extends TestCase {

    /** Logger for the class. */
    private static Logger log = Logger.getLogger(IOUtilsTest.class);

    private static final String FILECONTENTS = "Hello\nThis is a test file\n";

    private static String srcFolderName = "picweb-test-" + Long.toString(new Date().getTime());

    private static String destFolderName = srcFolderName + " copy";

    private static File srcFolder = new File(srcFolderName);

    private static File srcFile1 = new File(srcFolderName + File.separator + "dir1" + File.separator + "file1");

    private static File srcFile2 = new File(srcFolderName + File.separator + "dir2" + File.separator + "file2");

    private static File srcFile3 = new File(srcFolderName + File.separator + "dir2" + File.separator + "file3");

    private static File destFolder = new File(destFolderName);

    private static File destFile1 = new File(destFolderName + File.separator + "dir1" + File.separator + "file1");

    private static File destFile2 = new File(destFolderName + File.separator + "dir2" + File.separator + "file2");

    private static File destFile3 = new File(destFolderName + File.separator + "dir2" + File.separator + "file3");

    public static void setupTestFiles() throws IOException {
        IOUtils.touch(srcFile1);
        IOUtils.touch(srcFile2);
        IOUtils.touch(srcFile3);
    }

    public static void remoteTestFiles() throws IOException {
        IOUtils.purgeFolder(srcFolder);
        if (destFolder.exists()) {
            IOUtils.purgeFolder(destFolder);
        }
        srcFolder.delete();
        destFolder.delete();
    }

    public static File getSourceFolder() {
        return srcFolder;
    }

    public void setUp() throws Exception {
        log.info("setting up");
        setupTestFiles();
    }

    public void testPurgeFolder() throws IOException {
        log.info("Running: testPurgeFolder()");
        Assert.assertTrue(srcFile1.exists());
        Assert.assertTrue(srcFile2.exists());
        Assert.assertTrue(srcFile3.exists());
        Assert.assertTrue(srcFolder.exists());
        IOUtils.purgeFolder(srcFolder);
        Assert.assertTrue(!srcFile1.exists());
        Assert.assertTrue(!srcFile2.exists());
        Assert.assertTrue(!srcFile3.exists());
        Assert.assertTrue(srcFolder.exists());
    }

    public void testCopyFolderContents() throws IOException {
        log.info("Running: testCopyFolderContents()");
        IOUtils.copyFolderContents(srcFolderName, destFolderName);
        Assert.assertTrue(destFile1.exists() && destFile1.isFile());
        Assert.assertTrue(destFile2.exists() && destFile2.isFile());
        Assert.assertTrue(destFile3.exists() && destFile3.isFile());
    }

    public void testTouchFileCreation() throws IOException {
        log.info("Running: testTouchFileCreation()");
        if (srcFile1.exists()) {
            srcFile1.delete();
        }
        IOUtils.touch(srcFile1);
        Assert.assertTrue(srcFile1.exists() && srcFile1.isFile());
    }

    public void testTouchFileModifiedUpdate() throws IOException {
        log.info("Running: testTouchFileModifiedUpdate()");
        if (srcFile1.exists()) {
            srcFile1.delete();
        }
        if (!srcFile1.createNewFile()) {
            fail("Couldn't create new file: " + srcFile1.getAbsolutePath());
        }
        long initialDate = new Date().getTime() - 1;
        srcFile1.setLastModified(initialDate);
        IOUtils.touch(srcFile1);
        Assert.assertTrue(srcFile1.exists() && srcFile1.isFile() && (srcFile1.lastModified() > initialDate));
    }

    public void testTouchExistingFolder() throws IOException {
        log.info("Running: testTouchExistingFolder()");
        if (srcFolder.exists()) {
            IOUtils.purgeFolder(srcFolder);
        }
        srcFolder.mkdirs();
        try {
            IOUtils.touch(srcFolder);
        } catch (IOException e) {
            return;
        }
        fail("Expected an Exception to be thrown as passed in parameter was a folder");
    }

    public void testWriteFile() throws IOException {
        log.info("Running: testWriteFile()");
        IOUtils.writeFile(srcFile1.getAbsolutePath(), FILECONTENTS);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(srcFile1)));
        String line = reader.readLine();
        StringBuffer contents = new StringBuffer();
        while (line != null) {
            contents.append(line);
            contents.append("\n");
            line = reader.readLine();
        }
        String output = contents.toString().trim();
        reader.close();
        Assert.assertTrue(FILECONTENTS.trim().equals(output));
    }

    public void testWriteXMLFile() throws Exception {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element testElt = doc.createElement("testWriteXMLFile");
        doc.appendChild(testElt);
        IOUtils.writeXmlFile(srcFile1.getAbsolutePath(), doc);
        doc = builder.parse(srcFile1);
        NodeList nodes = doc.getElementsByTagName("testWriteXMLFile");
        Assert.assertTrue(nodes.getLength() > 0);
    }

    public void testWriteXMLFileBogus() throws Exception {
        log.info("Running: testWriteXMLFileBogus()");
        try {
            IOUtils.writeXmlFile(srcFolder.getAbsolutePath(), null);
            fail("Should have thrown exception trying to write to a folder");
        } catch (Exception e) {
        }
    }

    public void tearDown() throws IOException {
        log.info("tearing down");
        remoteTestFiles();
    }
}
