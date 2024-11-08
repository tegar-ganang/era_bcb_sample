package net.sourceforge.antme.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileLock;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.tools.ant.Project;
import net.sourceforge.antme.Constants;
import net.sourceforge.antme.Language;
import net.sourceforge.antme.jad.JadFormatException;

public class PackageTaskTest extends BaseTestTask {

    public PackageTaskTest() {
    }

    public void setUp() {
        configureProject("testCases.xml", "dir.test.temp");
        Language.setTestMode(true);
    }

    public void tearDown() {
        cleanTestDirectory();
        Language.setTestMode(false);
    }

    public void testExcludeMatching() {
        PackageTask.ExcludeListEntry test = new PackageTask.ExcludeListEntry("Foo");
        assertTrue(test.matches("Foo"));
        assertFalse(test.matches("Foobar"));
        assertFalse(test.matches("Bar"));
        test = new PackageTask.ExcludeListEntry("Foo*");
        assertTrue(test.matches("Foo"));
        assertTrue(test.matches("Foobar"));
        assertFalse(test.matches("Bar"));
        assertFalse(test.matches("BarFoo"));
    }

    public void testNoManifest1() throws Exception {
        executeBuildExceptionTarget("PackageTaskTest.testNoManifest1");
        assertExceptionMessageIs(PackageTask.LANG_DONT_USE_MANIFEST);
    }

    public void testNoManifest2() throws Exception {
        executeBuildExceptionTarget("PackageTaskTest.testNoManifest2");
        assertExceptionMessageIs(PackageTask.LANG_DONT_USE_MANIFEST);
    }

    public void testNoManifest3() throws Exception {
        executeBuildExceptionTarget("PackageTaskTest.testNoManifest3");
        assertExceptionMessageIs(PackageTask.LANG_DONT_USE_MANIFEST);
    }

    public void testMissingName() throws Exception {
        executeBuildExceptionTarget("PackageTaskTest.testMissingName");
        assertExceptionMessageIs(PackageTask.LANG_CANNOT_CONTINUE);
        assertLogContains(Project.MSG_ERR, PackageTask.LANG_MISSING_REQUIRED_ATTRIBUTE + ":MIDlet-Name");
    }

    public void testMissingMidlet() throws Exception {
        executeBuildExceptionTarget("PackageTaskTest.testMissingMidlet");
        assertExceptionMessageIs(PackageTask.LANG_CANNOT_CONTINUE);
        assertLogContains(Project.MSG_ERR, PackageTask.LANG_NO_MIDLETS_DEFINED);
    }

    public void testSuccess() throws Exception {
        executeTarget("PackageTaskTest.testSuccess");
        JarFile jar = new JarFile(new File(getTestDirectory(), "Output.jar"));
        assertNotNull(jar.getEntry("java/lang/String.class"));
        Manifest manifest = jar.getManifest();
        assertNotNull(manifest);
        Attributes mainAttributes = manifest.getMainAttributes();
        assertEquals("MyMidlet Midlet Suite", mainAttributes.getValue("MIDlet-Name"));
    }

    public void testFilter1() throws Exception {
        executeTarget("PackageTaskTest.testFilter1");
        File outputJarFile = new File(getTestDirectory(), "Output.jar");
        JarFile jar = new JarFile(outputJarFile);
        Manifest manifest = jar.getManifest();
        Attributes mainAttributes = manifest.getMainAttributes();
        assertEquals(null, mainAttributes.getValue("Filter-A"));
        assertEquals(null, mainAttributes.getValue("Filter-B"));
        assertEquals("C", mainAttributes.getValue("Filter-C"));
        assertEquals("A-A", mainAttributes.getValue("Filter-A-A"));
        assertEquals("A-B", mainAttributes.getValue("Filter-A-B"));
        assertEquals("A-C", mainAttributes.getValue("Filter-A-C"));
        File outputJadFile = new File(getTestDirectory(), "Output.jad");
        FileInputStream fis = new FileInputStream(outputJadFile);
        Properties outputJad = new Properties();
        outputJad.load(fis);
        fis.close();
        assertEquals("A", outputJad.getProperty("Filter-A"));
        assertEquals("B", outputJad.getProperty("Filter-B"));
        assertEquals("C", outputJad.getProperty("Filter-C"));
        assertEquals("A-A", outputJad.getProperty("Filter-A-A"));
        assertEquals("A-B", outputJad.getProperty("Filter-A-B"));
        assertEquals("A-C", outputJad.getProperty("Filter-A-C"));
        long length = outputJarFile.length();
        assertEquals(length, Long.parseLong(outputJad.getProperty("MIDlet-Jar-Size")));
    }

    public void testFilter2() throws Exception {
        executeTarget("PackageTaskTest.testFilter2");
        JarFile jar = new JarFile(new File(getTestDirectory(), "Output.jar"));
        Manifest manifest = jar.getManifest();
        Attributes mainAttributes = manifest.getMainAttributes();
        assertEquals(null, mainAttributes.getValue("Filter-A"));
        assertEquals("B", mainAttributes.getValue("Filter-B"));
        assertEquals("C", mainAttributes.getValue("Filter-C"));
        assertEquals(null, mainAttributes.getValue("Filter-A-A"));
        assertEquals(null, mainAttributes.getValue("Filter-A-B"));
        assertEquals(null, mainAttributes.getValue("Filter-A-C"));
    }

    public void testNoInput() throws Exception {
        executeBuildExceptionTarget("PackageTaskTest.testNoInput");
        assertExceptionMessageIs(Constants.LANG_ATTRIBUTE_REQUIRED + ":inputJad:mePackage");
    }

    public void testMissingInput() throws Exception {
        executeBuildExceptionTarget("PackageTaskTest.testMissingInput");
        assertExceptionMessageContains(Constants.LANG_NO_SUCH_FILE);
    }

    public void testNoOutput() throws Exception {
        executeBuildExceptionTarget("PackageTaskTest.testNoOutput");
        assertExceptionMessageIs(Constants.LANG_ATTRIBUTE_REQUIRED + ":jarfile:mePackage");
    }

    public void testBadOutputJad() throws Exception {
        executeBuildExceptionTarget("PackageTaskTest.testBadOutputJad");
        assertExceptionMessageContains(Constants.LANG_NO_SUCH_FILE);
    }

    public void testBadOutputJar() throws Exception {
        executeBuildExceptionTarget("PackageTaskTest.testBadOutputJar");
        assertExceptionMessageContains(Constants.LANG_NO_SUCH_FILE);
    }

    public void testIoErrorOnWrite() throws Exception {
        String[] outputs = new String[] { "Output.jad", "Output.jar" };
        for (int i = 0; i < outputs.length; i++) {
            File outputFile = new File(getTestDirectory(), outputs[i]);
            FileOutputStream fos = null;
            FileLock lock = null;
            try {
                fos = new FileOutputStream(outputFile);
                lock = fos.getChannel().lock();
                fos.write(0);
                executeBuildExceptionTarget("PackageTaskTest.testIoErrorOnWrite");
                assertTrue(getBuildException().getCause() instanceof IOException);
            } finally {
                lock.release();
                fos.close();
            }
        }
    }

    public void testInvalidJad() throws Exception {
        executeBuildExceptionTarget("PackageTaskTest.testInvalidJad");
        assertTrue(getBuildException().getCause() instanceof JadFormatException);
    }

    public void testInvalidEncoding() throws Exception {
        executeBuildExceptionTarget("PackageTaskTest.testInvalidEncoding");
        assertTrue(getBuildException().getCause() instanceof UnsupportedEncodingException);
    }
}
