package fipa.adst.util.classloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

/**
 * This ClassLoader is intended to be used to load agent classes
 * packed within a jar file which is provided as a byte array. 
 * 
 * @author <a href="mailto:jcucurull@deic.uab.cat">Jordi Cucurull Juan</a>
 * 
 * @version 1.0
 */
public class ByteArrayJarClassLoader extends ClassLoader {

    public static final int BUFFER_SIZE = 1024;

    /**
	 * @param Path and name of the JAR file
	 * @throws IOException If there are problems opening the file
	 */
    public ByteArrayJarClassLoader(byte[] jarByteArray, ClassLoader parent) throws IOException {
        super(parent);
        _jar = jarByteArray;
    }

    /**
	 * Get a class from within the JAR file used in this classloader.
	 */
    protected Class findClass(String className) throws ClassNotFoundException {
        String resourceName = className.replace('.', '/') + ".class";
        InputStream is = getResourceAsStream(resourceName);
        if (is != null) {
            try {
                byte[] rawClass = readFully(is);
                is.close();
                return defineClass(className, rawClass, 0, rawClass.length);
            } catch (IOException ioe) {
                throw new ClassNotFoundException("Error getting class: " + className + " from JAR file: " + ioe);
            }
        } else {
            throw new ClassNotFoundException("Error getting class: " + className + " from JAR file.");
        }
    }

    /**
	 * Support class to create a byte array from an input stream.
	 * @param is Source input stream.
	 * @return Target byte array.
	 * @throws IOException
	 */
    private byte[] readFully(InputStream is) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int read = 0;
        while ((read = is.read(buffer)) >= 0) baos.write(buffer, 0, read);
        return baos.toByteArray();
    }

    /**
	 * Get a resource from within this classloader.
	 */
    public InputStream getResourceAsStream(String name) {
        if (_jar != null) {
            try {
                JarInputStream jis = new JarInputStream(new ByteArrayInputStream(_jar));
                JarEntry je = null;
                while ((je = jis.getNextJarEntry()) != null) if (je.getName().equals(name)) return jis;
            } catch (IOException ioe) {
                System.err.println(ioe);
            }
        }
        return null;
    }

    private byte[] _jar = null;
}
