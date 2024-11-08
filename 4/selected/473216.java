package vqwiki.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Test for functions in Utilities abstract class.
 *
 * @author mteodori
 * @version $Rev$ $Date$
 */
public class ZipUtilitiesTest extends TestCase {

    private static final Logger logger = Logger.getLogger(ZipUtilitiesTest.class.getName());

    private File zipFile;

    private File testOutputDirectory;

    private File zipOutputDirectory;

    protected void setUp() throws Exception {
        testOutputDirectory = new File(getClass().getResource("/").getPath());
        zipFile = new File(this.testOutputDirectory, "/plugin.zip");
        zipOutputDirectory = new File(this.testOutputDirectory, "zip");
        zipOutputDirectory.mkdir();
        logger.fine("zip dir created");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        zos.putNextEntry(new ZipEntry("css/"));
        zos.putNextEntry(new ZipEntry("css/system.properties"));
        System.getProperties().store(zos, null);
        zos.closeEntry();
        zos.putNextEntry(new ZipEntry("js/"));
        zos.putNextEntry(new ZipEntry("js/system.properties"));
        System.getProperties().store(zos, null);
        zos.closeEntry();
        zos.putNextEntry(new ZipEntry("WEB-INF/"));
        zos.putNextEntry(new ZipEntry("WEB-INF/classes/"));
        zos.putNextEntry(new ZipEntry("WEB-INF/classes/system.properties"));
        System.getProperties().store(zos, null);
        zos.closeEntry();
        zos.putNextEntry(new ZipEntry("WEB-INF/lib/"));
        zos.putNextEntry(new ZipEntry("WEB-INF/lib/mylib.jar"));
        File jarFile = new File(this.testOutputDirectory.getPath() + "/mylib.jar");
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile));
        jos.putNextEntry(new ZipEntry("vqwiki/"));
        jos.putNextEntry(new ZipEntry("vqwiki/plugins/"));
        jos.putNextEntry(new ZipEntry("vqwiki/plugins/system.properties"));
        System.getProperties().store(jos, null);
        jos.closeEntry();
        jos.close();
        IOUtils.copy(new FileInputStream(jarFile), zos);
        zos.closeEntry();
        zos.close();
        jarFile.delete();
    }

    protected void tearDown() throws Exception {
        zipFile.delete();
        FileUtils.deleteDirectory(zipOutputDirectory);
    }

    public void testUnzipWithExclusions() throws Exception {
        Utilities.unzip(zipFile, zipOutputDirectory, new String[] { "WEB-INF" });
        assertFalse(new File(zipOutputDirectory, "WEB-INF").exists());
        assertTrue(new File(zipOutputDirectory, "css").exists());
        assertTrue(new File(zipOutputDirectory, "js").exists());
    }

    public void testUnzipWithInclusions() throws Exception {
        Utilities.unzip(zipFile, zipOutputDirectory, null, new String[] { "WEB-INF" });
        assertTrue(new File(zipOutputDirectory, "WEB-INF").exists());
        assertFalse(new File(zipOutputDirectory, "css").exists());
        assertFalse(new File(zipOutputDirectory, "js").exists());
    }
}
