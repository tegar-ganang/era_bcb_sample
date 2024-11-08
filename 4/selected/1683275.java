package net.sourceforge.ftpowl.util.shop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * SimpleFileShop defines methods to copy a file into another
 * 
 * @author <a href="mailto:admiral_kay@users.sourceforge.net" title="Kay
 *         Patzwald">Kay Patzwald </a>
 */
public class SimpleFileShop {

    /** FileShop constant: Operation was aborted */
    public static final int OP_ABORTED = -1;

    /** FileShop constant: Operation failed */
    public static final int OP_FAILED = 1;

    /** FileShop constant: Operation was successfull */
    public static final int OP_SUCCESSFULL = 0;

    /** This utility class constructor is protected */
    protected SimpleFileShop() {
    }

    /**
	 * Copy one file to another
	 * 
	 * @param fis The source stream
	 * @param fos The destination stream
	 */
    public static void copy(InputStream fis, OutputStream fos) {
        try {
            byte buffer[] = new byte[0xffff];
            int nbytes;
            while ((nbytes = fis.read(buffer)) != -1) fos.write(buffer, 0, nbytes);
        } catch (IOException e) {
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
	 * Copy one file to another
	 * 
	 * @param src The path of the sourcefile
	 * @param dest The path of the destinationfile
	 */
    public static void copy(String src, String dest) {
        try {
            copy(new FileInputStream(src), new FileOutputStream(dest));
        } catch (IOException e) {
        }
    }

    /**
	 * Copy files and directories
	 * @param fileToCopy
	 * @param newFile
	 */
    public static void copy(File fileToCopy, File newFile) {
        copy(fileToCopy.getAbsolutePath(), newFile.getAbsolutePath());
    }

    /**
	 * Move files and directories
	 * @param fileToCopy
	 * @param newFile
	 */
    public static void move(File fileToMove, File newFile) {
        if (!fileToMove.renameTo(newFile)) {
            System.err.println("Fehler beim Verschieben des Verzeichnisses / der Datei: " + fileToMove);
        }
    }
}
