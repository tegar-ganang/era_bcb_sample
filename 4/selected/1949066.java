package de.huxhorn.sulky.groovy;

import de.huxhorn.sulky.junit.LoggingTestBase;
import groovy.lang.Script;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class GroovyInstanceTest extends LoggingTestBase {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File fooFile;

    public GroovyInstanceTest(Boolean logging) {
        super(logging);
    }

    @Before
    public void setUp() throws IOException {
        fooFile = folder.newFile("Foo.groovy");
        copyIntoFile("/Foo.groovy", fooFile);
    }

    private void copyIntoFile(String resource, File output) throws IOException {
        FileOutputStream out = null;
        InputStream in = null;
        try {
            out = FileUtils.openOutputStream(output);
            in = GroovyInstanceTest.class.getResourceAsStream(resource);
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
    }

    @SuppressWarnings({ "ThrowableResultOfMethodCallIgnored" })
    @Test
    public void normal() {
        GroovyInstance instance = new GroovyInstance();
        instance.setGroovyFileName(fooFile.getAbsolutePath());
        Class instanceClass = instance.getInstanceClass();
        assertNotNull(instanceClass);
        assertEquals("Foo", instanceClass.getName());
        Object object = instance.getInstance();
        assertNotNull(object);
        assertTrue(object instanceof Script);
        Script script = (Script) object;
        String result = (String) script.run();
        assertEquals("Foo", result);
        assertNull(instance.getErrorCause());
        assertNull(instance.getErrorMessage());
        Object newObject = instance.getNewInstance();
        assertNotNull(object);
        assertTrue(newObject instanceof Script);
        Script newScript = (Script) newObject;
        String newResult = (String) newScript.run();
        assertEquals("Foo", newResult);
        assertNotSame(newScript, script);
        assertNotSame(instance.getNewInstance(), newScript);
        assertSame(instance.getInstance(), script);
        assertSame(script, instance.getInstanceAs(Script.class));
        assertNotSame(script, instance.getNewInstanceAs(Script.class));
        newScript = instance.getNewInstanceAs(Script.class);
        newResult = (String) newScript.run();
        assertEquals("Foo", newResult);
        assertNull(instance.getInstanceAs(Comparable.class));
        assertNull(instance.getNewInstanceAs(Comparable.class));
    }

    @Test
    public void refresh() throws IOException, InterruptedException {
        GroovyInstance instance = new GroovyInstance();
        instance.setGroovyFileName(fooFile.getAbsolutePath());
        instance.setRefreshInterval(200);
        Class instanceClass = instance.getInstanceClass();
        assertNotNull(instanceClass);
        assertEquals("Foo", instanceClass.getName());
        Object object = instance.getInstance();
        assertNotNull(object);
        assertTrue(object instanceof Script);
        Script script = (Script) object;
        String result = (String) script.run();
        assertEquals("Foo", result);
        Thread.sleep(1000);
        copyIntoFile("/Bar.groovy", fooFile);
        object = instance.getInstance();
        assertTrue(object instanceof Script);
        script = (Script) object;
        result = (String) script.run();
        assertEquals("Bar", result);
    }

    @SuppressWarnings({ "ThrowableResultOfMethodCallIgnored" })
    @Test
    public void broken() throws IOException, InterruptedException {
        GroovyInstance instance = new GroovyInstance();
        instance.setGroovyFileName(fooFile.getAbsolutePath());
        instance.setRefreshInterval(200);
        Class instanceClass = instance.getInstanceClass();
        assertNotNull(instanceClass);
        assertEquals("Foo", instanceClass.getName());
        Object object = instance.getInstance();
        assertNotNull(object);
        assertTrue(object instanceof Script);
        Script script = (Script) object;
        String result = (String) script.run();
        assertEquals("Foo", result);
        Thread.sleep(1000);
        copyIntoFile("/Broken.b0rken", fooFile);
        Thread.sleep(1000);
        assertNull("" + instance.getInstanceClass(), instance.getInstanceClass());
        assertNull("" + instance.getInstance(), instance.getInstance());
        Thread.sleep(500);
        assertNull("" + instance.getInstanceClass(), instance.getInstanceClass());
        assertNull("" + instance.getInstance(), instance.getInstance());
        Thread.sleep(500);
        assertNull("" + instance.getInstanceClass(), instance.getInstanceClass());
        assertNull("" + instance.getInstance(), instance.getInstance());
        Thread.sleep(500);
        assertNotNull(instance.getErrorCause());
        assertNotNull(instance.getErrorMessage());
        copyIntoFile("/Bar.groovy", fooFile);
        object = instance.getInstance();
        assertTrue(object instanceof Script);
        script = (Script) object;
        result = (String) script.run();
        assertEquals("Bar", result);
        assertNull(instance.getErrorCause());
        assertNull(instance.getErrorMessage());
    }

    @SuppressWarnings({ "ThrowableResultOfMethodCallIgnored" })
    @Test
    public void nullFile() {
        GroovyInstance instance = new GroovyInstance();
        instance.setGroovyFileName(null);
        Class instanceClass = instance.getInstanceClass();
        assertNull(instanceClass);
        Object object = instance.getInstance();
        assertNull(object);
        assertNull(instance.getErrorCause());
        assertEquals("groovyFileName must not be null!", instance.getErrorMessage());
    }
}
