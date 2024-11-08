package net.pandoragames.util.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A small collection of useful helper methods.
 *
 * @author Olivier Wehner
 * <!-- copyright note -->
 */
public class FileUtil {

    /**
	 * Copies source to sink. Both files must exist beforehand.
	 * @param source data to copy
	 * @param sink file to copy to
	 * @throws IOException if copy operation failed
	 */
    public static void copy(File source, File sink) throws IOException {
        if (source == null) throw new NullPointerException("Source file must not be null");
        if (sink == null) throw new NullPointerException("Target file must not be null");
        if (!source.exists()) throw new IOException("Source file " + source.getPath() + " does not exist");
        if (!source.isFile()) throw new IOException("Source file " + source.getPath() + " is not a regular file");
        if (!source.canRead()) throw new IOException("Source file " + source.getPath() + " can not be read (missing acces right)");
        if (!sink.exists()) throw new IOException("Target file " + sink.getPath() + " does not exist");
        if (!sink.isFile()) throw new IOException("Target file " + sink.getPath() + " is not a regular file");
        if (!sink.canWrite()) throw new IOException("Target file " + sink.getPath() + " is write protected");
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(source);
            output = new FileOutputStream(sink);
            byte[] buffer = new byte[1024];
            while (input.available() > 0) {
                int bread = input.read(buffer);
                if (bread > 0) output.write(buffer, 0, bread);
            }
        } finally {
            if (input != null) try {
                input.close();
            } catch (IOException x) {
            }
            if (output != null) try {
                output.close();
            } catch (IOException x) {
            }
        }
    }

    /**
	 * Returns true if the second directory argument is indeed a subdirectory
	 * of the first directory argument.
	 * Returns allways true if ancestor is null. 
	 * Returns allways false if subdirectory is null.
	 * @param ancestor base directory
	 * @param subdirectory to be tested
	 * @return true if "ancestor" is indeed an ancestor directory of "subdirectory"
	 */
    public static boolean isSubdirectory(File ancestor, File subdirectory) {
        if (ancestor == null) return true;
        if (subdirectory == null) return false;
        if (ancestor.equals(subdirectory)) return true;
        return isSubdirectory(ancestor, subdirectory.getParentFile());
    }
}
