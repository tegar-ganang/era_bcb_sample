package de.lamasep.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Utility Klasse für Operationen auf dem FileSystem.
 * @author Andreas Brandl <mail@andreas-brandl.de>
 */
public final class FileSystemUtil {

    /**
     * Privater Konstruktor.
     */
    private FileSystemUtil() {
    }

    /**
     * Löscht eine Datei oder ein Verzeichnis.
     * Handelt es sich bei <tt>file</tt> um ein Verzeichnis, wird dieses
     * vollständig entfernt - inklusive etwaiger Unterverzeichnisse und deren
     * Dateien sowie Dateien, die in <tt>file</tt> liegen.
     * @param file  zu löschendes Verzeichnis   <tt>file != null</tt>
     * @throws IllegalArgumentException falls <tt> file == null</tt>
     */
    public static void delete(File file) {
        if (file == null) {
            throw new IllegalArgumentException();
        }
        if (file.isFile()) {
            file.delete();
        } else if (file.isDirectory()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    delete(f);
                }
            }
            file.delete();
        }
    }

    /**
     * Verschiebt eine Datei oder ein komplettes Verzeichnis (inkl. Dateien
     * und Unterverzeichnissen) nach <tt>dest</tt>.
     *
     * Existiert das Ziel, wird es überschrieben.
     *
     * @param src   Quelle <tt>src != null</tt>
     * @param dest  Ziel <tt>dest != null</tt>
     * @return  <tt>true</tt> falls die Operation erfolgreich war,
     *      <tt>false</tt> sonst.
     * @throws java.io.IOException falls das Kopieren fehlschlägt.
     * @throws IllegalArgumentException falls
     *          <tt>src == null || dest == null</tt>
     */
    public static boolean move(File src, File dest) throws IOException {
        if (src == null) {
            throw new IllegalArgumentException("src == null");
        }
        if (dest == null) {
            throw new IllegalArgumentException("dest == null");
        }
        boolean result = true;
        if (src.isDirectory()) {
            dest.mkdirs();
            File[] content = src.listFiles();
            if (content != null) {
                for (File f : content) {
                    result &= move(f, new File(dest, f.getName()));
                }
            }
        } else {
            result &= copyFile(src, dest);
        }
        if (result) {
            delete(src);
        }
        return result;
    }

    /**
     * Kopiert eine Datei.
     *
     * Ist <tt>src</tt> ein Verzeichnis, wird nichts kopiert.
     *
     * @param src   Quelle <code>src != null</code>
     * @param dest  Ziel <code>tt != null</code>
     * @return  <tt>true</tt> falls das Kopieren erfolgreich war,
     *      <tt>false</tt> sonst.
     * @throws java.io.IOException falls ein Fehler beim Kopieren auftritt
     * @throws IllegalArgumentException
     *          falls <code>src == null || dest == null</code>
     */
    public static boolean copyFile(File src, File dest) throws IOException {
        if (src == null) {
            throw new IllegalArgumentException("src == null");
        }
        if (dest == null) {
            throw new IllegalArgumentException("dest == null");
        }
        if (!src.isFile()) {
            return false;
        }
        FileChannel in = new FileInputStream(src).getChannel();
        FileChannel out = new FileOutputStream(dest).getChannel();
        try {
            in.transferTo(0, in.size(), out);
            return true;
        } catch (IOException e) {
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }
}
