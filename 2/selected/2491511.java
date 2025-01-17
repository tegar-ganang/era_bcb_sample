package eu.medsea.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
  * Utility class providing methods to work with Zip and Jar Files
  * @author Steven McArdle
  *
  */
public class ZipJarUtil {

    /**
	 * Get an InputStream from the zip file capable of reading from
	 * @param url
	 * @return InputStream for reading from a jar or zip file
	 * @throws IOException
	 */
    public static InputStream getInputStreamForURL(URL url) throws IOException {
        JarURLConnection conn = (JarURLConnection) url.openConnection();
        return conn.getInputStream();
    }

    /**
	 * Get all entries from a Zip or Jar file. This defers to the getEntries(ZipFile zipFile) method
	 *
	 * Example of usage:
	 * Collection entries = ZipJarUtil.getEntries("src/test/resources/a.zip");
	 *
	 * @param fileName path identifying a zip or jar file
	 * @return collection of Strings representing the entries in the zip or jar file
	 * @throws ZipException
	 * @throws IOException
	 */
    public static Collection getEntries(String fileName) throws ZipException, IOException {
        return getEntries(new ZipFile(fileName));
    }

    /**
	 * Get all entries from a Zip or Jar file. This defers to the getEntries(ZipFile zipFile) method
	 *
	 * Example of usage:
	 * Collection entries = ZipJarUtil.getEntries(new File("src/test/resources/a.zip"));
	 *
	 * @param file identifies a zip or jar file
	 * @return collection of Strings representing the entries in the zip or jar file
	 * @throws ZipException
	 * @throws IOException
	 */
    public static Collection getEntries(File file) throws ZipException, IOException {
        return getEntries(new ZipFile(file));
    }

    /**
	 * Get all entries from a Zip or Jar file identified by a URL.
	 *
	 * Example of usage:
	 * Collection entries = ZipJarUtil.getEntries(new URL("jar:file:src/test/resources/a.zip!/"));
	 *
	 * This defers to the getEntries(ZipFile zipFile) method
	 *
	 * @param url identifying a jar or zip file. Can also refer to an entry, which is ignored.
	 * @return collection of Strings representing the entries in the zip or jar file
	 * @throws ZipException
	 * @throws IOException
	 */
    public static Collection getEntries(URL url) throws ZipException, IOException {
        JarURLConnection conn = (JarURLConnection) url.openConnection();
        return getEntries(conn.getJarFile());
    }

    /**
	 * Get all entries from a zip or jar file. Ignores directories
	 *
	 * @param zipFile
	 * @return collection of Strings representing the entries in the zip or jar file
	 * @throws ZipException
	 * @throws IOException
	 */
    public static Collection getEntries(ZipFile zipFile) throws ZipException, IOException {
        Collection entries = new ArrayList();
        Enumeration e = zipFile.entries();
        while (e.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) e.nextElement();
            if (!ze.isDirectory()) {
                entries.add(ze.getName());
            }
        }
        return entries;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(ZipJarUtil.getEntries("src/test/resources/a.zip"));
        System.out.println(ZipJarUtil.getEntries(new File("src/test/resources/a.zip")));
        System.out.println(ZipJarUtil.getEntries(new URL("jar:file:src/test/resources/a.zip!/")));
        System.out.println(ZipJarUtil.getEntries(new URL("jar:file:src/test/resources/a.zip!/resources/eu/medsea/mimeutil/magic.mime")));
    }
}
