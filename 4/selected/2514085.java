package net.sourceforge.minigolf.provider;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import net.sourceforge.minigolf.exceptions.MinigolfException;

/**
 * This is a ClassLoader to get the provider-class out of a JARFile.
 */
public class JARClassLoader extends ClassLoader {

    /** This is the JARFile we are to load the class from. */
    private final JarFile jar;

    /** The buffer size for reading/writing */
    private static final int BUFFER_SIZE = 4096;

    /**
  * Create the ClassLoader.
  * @param f	The JARFile to load from.
  */
    public JARClassLoader(JarFile f) {
        super();
        jar = f;
    }

    /**
  * @see java.lang.ClassLoader#findClass(String)
  * @throws ClassNotFoundException if it is not found.
  */
    public Class findClass(String name) throws ClassNotFoundException {
        try {
            byte[] b = readEntry(name + ".class");
            return defineClass(name, b, 0, b.length);
        } catch (FileNotFoundException e) {
            throw new ClassNotFoundException(name);
        }
    }

    /**
  * Read the data of a given entry into a byte-array.
  * @param ent	The name of the entry to read.
  * @return This entry as byte-array.
  * @throws FileNotFoundException if entry is not found.
  */
    private byte[] readEntry(String name) throws FileNotFoundException {
        try {
            ZipEntry entry = jar.getEntry(name);
            if (entry == null) throw new FileNotFoundException(name);
            InputStream in = jar.getInputStream(entry);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[BUFFER_SIZE];
            while (true) {
                int read = in.read(buf);
                if (read == -1) break;
                out.write(buf, 0, read);
            }
            return out.toByteArray();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            MinigolfException.throwToDisplay(e);
        }
        return null;
    }
}
