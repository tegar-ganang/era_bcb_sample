package org.settings4j.contentresolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class FSContentResolverTest extends TestCase {

    /** General Logger for this Class. */
    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(FSContentResolverTest.class);

    private static final String BASE_DIR = FSContentResolverTest.class.getPackage().getName().replace('.', '/');

    private File testDir;

    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();
        this.testDir = (new File("test/ContentResolverTest/".toLowerCase())).getAbsoluteFile();
        FileUtils.forceMkdir(this.testDir);
    }

    /** {@inheritDoc} */
    protected void tearDown() throws Exception {
        FileUtils.deleteDirectory(new File("test"));
        super.tearDown();
    }

    public void testReadHelloWorldTxt() throws Exception {
        final InputStream helloWorldIS = this.getClass().getClassLoader().getResourceAsStream(BASE_DIR + "/HelloWorld.txt");
        FileUtils.forceMkdir(new File(this.testDir.getAbsolutePath() + "/org/settings4j/contentresolver"));
        final String helloWorldPath = this.testDir.getAbsolutePath() + "/org/settings4j/contentresolver/HelloWorld.txt";
        final FileOutputStream fileOutputStream = new FileOutputStream(new File(helloWorldPath));
        IOUtils.copy(helloWorldIS, fileOutputStream);
        IOUtils.closeQuietly(helloWorldIS);
        IOUtils.closeQuietly(fileOutputStream);
        LOG.info("helloWorldPath: " + helloWorldPath);
        final FSContentResolver contentResolver = new FSContentResolver();
        contentResolver.setRootFolderPath(this.testDir.getAbsolutePath());
        byte[] content = contentResolver.getContent("org/settings4j/contentresolver/HelloWorld.txt");
        assertNotNull(content);
        assertEquals("Hello World", new String(content, "UTF-8"));
        content = contentResolver.getContent("file:org/settings4j/contentresolver/HelloWorld.txt");
        assertNotNull(content);
        assertEquals("Hello World", new String(content, "UTF-8"));
        content = contentResolver.getContent("file:/org/settings4j/contentresolver/HelloWorld.txt");
        assertNotNull(content);
        assertEquals("Hello World", new String(content, "UTF-8"));
        content = contentResolver.getContent("file:laksjdhalksdhfa");
        assertNull(content);
        content = contentResolver.getContent("/org/settings4j/contentresolver/HelloWorld.txt");
        assertNotNull(content);
        assertEquals("Hello World", new String(content, "UTF-8"));
    }

    public void testWriteHelloWorldTxt() throws Exception {
        FileUtils.deleteDirectory(new File(this.testDir.getCanonicalPath() + "/org"));
        final InputStream helloWorldIS = this.getClass().getClassLoader().getResourceAsStream(BASE_DIR + "/HelloWorld.txt");
        final byte[] value = IOUtils.toByteArray(helloWorldIS);
        final FSContentResolver contentResolver = new FSContentResolver();
        contentResolver.setRootFolderPath(this.testDir.getAbsolutePath());
        byte[] content = contentResolver.getContent("org/settings4j/contentresolver/HelloWorld.txt");
        assertNull(content);
        content = contentResolver.getContent("file:org/settings4j/contentresolver/HelloWorld.txt");
        assertNull(content);
        FileUtils.writeByteArrayToFile(new File(this.testDir.getAbsolutePath() + "/org/settings4j/contentresolver/HelloWorld.txt"), value);
        content = contentResolver.getContent("org/settings4j/contentresolver/HelloWorld.txt");
        assertNotNull(content);
        assertEquals("Hello World", new String(content, "UTF-8"));
        content = contentResolver.getContent("file:org/settings4j/contentresolver/HelloWorld.txt");
        assertNotNull(content);
        assertEquals("Hello World", new String(content, "UTF-8"));
        content = contentResolver.getContent("file:laksjdhalksdhfa");
        assertNull(content);
        content = contentResolver.getContent("/org/settings4j/contentresolver/HelloWorld.txt");
        assertNotNull(content);
        assertEquals("Hello World", new String(content, "UTF-8"));
    }
}
