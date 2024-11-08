package org.crazydays.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import junit.framework.TestCase;

/**
 * 
 */
public class SimpleTestCase extends TestCase {

    /** sandbox */
    protected File sandbox;

    /**
     * Cleanup sandbox.
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown() {
        if (sandbox != null) {
            delete(sandbox);
        }
    }

    /**
     * Delete file or directory.
     * 
     * @param file File
     */
    protected void delete(File file) {
        assertNotNull("file == null", file);
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            for (File child : children) {
                if (child.getName().equals(".") || child.getName().equals("..")) {
                    continue;
                }
                delete(child);
            }
        }
        assertTrue("Unable to delete file: " + file, file.delete());
    }

    /**
     * Get sandbox.
     * 
     * @return Sanbox
     */
    protected File getSandbox() {
        if (sandbox == null) {
            sandbox = new File(new File(".", "tmp"), getClass().getName());
            assertTrue("sandbox.mkdirs", sandbox.mkdirs());
        }
        return sandbox;
    }

    public File copyLocal(File destination, String resourcePath) {
        assertNotNull("destination == null", destination);
        assertNotNull("resourcePath == null", resourcePath);
        return copy(destination, absolute(resourcePath));
    }

    public File copy(File destination, String resourcePath) {
        assertNotNull("destination == null", destination);
        assertNotNull("resourcePath == null", resourcePath);
        if (destination.isDirectory()) {
            destination = new File(destination, filename(resourcePath));
        }
        assertEquals("destination exists " + destination, false, destination.exists());
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = ClassLoader.getSystemResourceAsStream(resourcePath);
            assertNotNull("Unable to load resource " + resourcePath, inputStream);
            outputStream = new FileOutputStream(destination);
            copy(inputStream, outputStream);
            return destination;
        } catch (IOException e) {
            fail(e.toString());
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    fail("Unable to close resourcePath " + resourcePath);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    fail("Unable to close destination " + destination);
                }
            }
        }
    }

    protected int copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        assertNotNull("inputStream == null", inputStream);
        assertNotNull("outputStream == null", outputStream);
        int total = 0;
        int read = 0;
        byte[] bytes = new byte[8196];
        while ((read = inputStream.read(bytes)) > 0) {
            outputStream.write(bytes, 0, read);
            total += read;
        }
        return total;
    }

    /**
     * Determine the filename from the resource path.
     * 
     * @param resourcePath Resource path
     * @return Filename
     */
    protected String filename(String resourcePath) {
        assertNotNull("resourcePath == null", resourcePath);
        int last = resourcePath.lastIndexOf('/');
        if (last >= 0) {
            return resourcePath.substring(last);
        } else {
            return resourcePath;
        }
    }

    /**
     * Get absolute resource path from local resource path.
     * 
     * @param localResourcePath Local resource path
     * @return Absolute resource path
     */
    protected String absolute(String localResourcePath) {
        assertNotNull("localResourcePath == null", localResourcePath);
        StringBuffer absoluteResourcePath = new StringBuffer();
        absoluteResourcePath.append(getClass().getPackage().getName().replace('.', '/'));
        absoluteResourcePath.append('/');
        absoluteResourcePath.append(localResourcePath);
        return absoluteResourcePath.toString();
    }

    /**
     * Assert object is instance of clazz.
     * 
     * @param message Message
     * @param clazz Class
     * @param object Object
     */
    public void assertIsInstance(String message, Class<?> clazz, Object object) {
        assertTrue(message, clazz.isInstance(object));
    }

    /**
     * Assert call is unreachable.
     * 
     * @param message Message
     */
    public void assertUnreachable(String message) {
        fail(message);
    }
}
