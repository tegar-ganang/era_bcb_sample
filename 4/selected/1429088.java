package it.imolinfo.jbi4ejb.webservice.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/**
 * Helper method to unjar files.
 * Some method are taken from: Taken from:http://www.koders.com/java/fidB15D2DD90588A86609A0E986A7987FA5DF5C454A.aspx
 * TODO :Verify license
 * @author <a href="mailto:mpiraccini@imolinfo.it">Marco Piraccini</a>
 */
public final class JarUtil {

    /** The BUFFER_LENGTH. */
    private static final int BUFFER_LENGTH = 1024;

    /**
     * Instantiates a new jar util.
     */
    private JarUtil() {
    }

    /**
     * Extracts the given jar-file to the specified directory. The target
     * directory will be cleaned before the jar-file will be extracted.
     * 
     * @param jarFile
     *            The jar file which should be unpacked
     * @param targetDir
     *            The directory to which the jar-content should be extracted.
     * 
     * @throws IOException
     *             when a file could not be written or the jar-file could not
     *             read.
     */
    public static void unjar(File jarFile, File targetDir) throws IOException {
        if (targetDir.exists()) {
            targetDir.delete();
        }
        targetDir.mkdirs();
        String targetPath = targetDir.getAbsolutePath() + File.separatorChar;
        byte[] buffer = new byte[BUFFER_LENGTH * BUFFER_LENGTH];
        JarFile input = new JarFile(jarFile, false, ZipFile.OPEN_READ);
        Enumeration<JarEntry> enumeration = input.entries();
        for (; enumeration.hasMoreElements(); ) {
            JarEntry entry = (JarEntry) enumeration.nextElement();
            if (!entry.isDirectory()) {
                if (entry.getName().indexOf("package cache") == -1) {
                    String path = targetPath + entry.getName();
                    File file = new File(path);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    FileOutputStream out = new FileOutputStream(file);
                    InputStream in = input.getInputStream(entry);
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    in.close();
                    out.close();
                }
            }
        }
    }

    /**
     * Extracts the given resource from a jar-file to the specified directory.
     * 
     * @param jarFile
     *            The jar file which should be unpacked
     * @param resource
     *            The name of a resource in the jar
     * @param targetDir
     *            The directory to which the jar-content should be extracted.
     * 
     * @throws IOException
     *             when a file could not be written or the jar-file could not
     *             read.
     */
    public static void unjar(File jarFile, String resource, File targetDir) throws IOException {
        if (targetDir.exists()) {
            targetDir.delete();
        }
        targetDir.mkdirs();
        String targetPath = targetDir.getAbsolutePath() + File.separatorChar;
        byte[] buffer = new byte[BUFFER_LENGTH * BUFFER_LENGTH];
        JarFile input = new JarFile(jarFile, false, ZipFile.OPEN_READ);
        Enumeration<JarEntry> enumeration = input.entries();
        for (; enumeration.hasMoreElements(); ) {
            JarEntry entry = (JarEntry) enumeration.nextElement();
            if (!entry.isDirectory()) {
                if (entry.getName().equals(resource)) {
                    String path = targetPath + entry.getName();
                    File file = new File(path);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    FileOutputStream out = new FileOutputStream(file);
                    InputStream in = input.getInputStream(entry);
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    in.close();
                    out.close();
                }
            }
        }
    }
}
