package org.rakiura.rak;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Represents a temporary and library files manager.
 * 
 *<br><br>
 * FileManager.java<br>
 * Created: Fri Jun  1 23:42:18 2001<br>
 *
 * @author <a href="mailto:mariusz@rakiura.org">Mariusz Nowostawski</a>
 * @version $Revision: 1.11 $ $Date: 2002/02/25 23:26:30 $
 */
public class FileManager {

    /** Temp directory. */
    File tmpPath;

    /** Module storage directory. */
    File libraryPath;

    /** Removed module storage directory. */
    File removedPath;

    /** Logger. */
    static final Logger logger = Logger.getLogger("org.rakiura.rak");

    public FileManager(File library, File removed, File tmp) {
        this.libraryPath = library;
        this.removedPath = removed;
        this.tmpPath = tmp;
    }

    public File createTmpModuleFile() {
        try {
            return File.createTempFile("module", ".jar", tmpPath);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public File getModuleFile(String name) {
        return new File(libraryPath, name);
    }

    public void removeModule(ModuleInfo info) {
        final File file = new File(libraryPath, info.getFileName());
        try {
            copy(file, new File(removedPath, info.getFileName()));
        } catch (IOException ex) {
            logger.warning("Copying file into removed directory faild.");
            logger.finer("cp " + file + " to removed failed: " + ex.getMessage());
        }
        file.delete();
    }

    public void moveFromTmp(File src, String name) {
        final File dest = getModuleFile(name.trim());
        src.renameTo(dest);
    }

    /**
   * Copies a source file to a destination, deleting the latter if it already
   * exists, and creating containing directory(ies) if not there.
   * @param src the file to copy from.
   * @param dest the file to copy to.
   * @exception IOException if an I/O error occurs during the execution.
   */
    public static void copy(File src, File dest) throws IOException {
        if (dest.exists() && dest.isFile()) {
            logger.fine("cp " + src + " " + dest + " -- Destination file " + dest + " already exists. Deleting...");
            dest.delete();
        }
        final File parent = dest.getParentFile();
        if (!parent.exists()) {
            logger.info("Directory to contain destination does not exist. Creating...");
            parent.mkdirs();
        }
        final FileInputStream fis = new FileInputStream(src);
        final FileOutputStream fos = new FileOutputStream(dest);
        final byte[] b = new byte[2048];
        int n;
        while ((n = fis.read(b)) != -1) fos.write(b, 0, n);
        fis.close();
        fos.close();
    }

    /**
   * Copies a source jar to a destination jar.
   * @param src the jar file to copy from.
   * @param dest the jar file to copy to.
   * @exception IOException if an I/O error occurs during the execution.
   */
    public static void copyJar(JarFile src, JarOutputStream dest) throws IOException {
        Enumeration entries = src.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zse = (ZipEntry) entries.nextElement();
            if (zse.getName().equals("META-INF/MODULEINFO")) continue;
            if (zse.getName().equals("META-INF/")) continue;
            dest.putNextEntry(zse);
            InputStream fis = src.getInputStream(zse);
            byte[] b = new byte[2048];
            int n;
            while ((n = fis.read(b)) != -1) {
                dest.write(b, 0, n);
            }
        }
    }

    /**
   * Creates a MODULEINFO file and inserts it inside a given Zip file. If moduleinfo 
   * entry is already present in the destination file, the exception will be thrown to say so.
   */
    public static void insertModuleinfo(File dest, ModuleInfo info) throws IOException {
        ZipFile zFile = new ZipFile(dest);
        ZipEntry zse = zFile.getEntry(ModuleInfo.MODULEINFO_RAK);
        if (zse == null) {
            zFile.close();
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dest));
            insertModuleinfo(zos, info);
            zos.flush();
            zos.close();
            return;
        }
        throw new IOException("Destination file: " + dest + " already has moduleinfo annotations. ");
    }

    /**
   * Creates a MODULEINFO file and inserts it inside a given Zip file.
   */
    public static void insertModuleinfo(ZipOutputStream dest, ModuleInfo info) throws IOException {
        ZipEntry zse = new ZipEntry(ModuleInfo.MODULEINFO_RAK);
        dest.putNextEntry(zse);
        info.write(dest);
        dest.flush();
    }
}
