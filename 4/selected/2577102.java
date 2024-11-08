package net.sourceforge.jbackupfw.core.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.sourceforge.jbackupfw.core.data.BackUpInfoFileGroup;
import net.sourceforge.jbackupfw.core.data.BackupException;
import org.apache.commons.io.IOUtils;

/**
 * This class provides the basic system operation reading from the file in order
 * to see the information about the files that were backed up 
 *
 * @author Boris Horvat and Dusan Guduric
 */
public class ImportData {

    /** holds the path to the file that has the backed up information */
    private static final String IMPORT_PATH = "temp//backUpExternalInfo.out";

    /**
     * This method is used to extract all of the information about the data that
     * was backed up to some location. It first unpacks the file and then
     * it reads the information from it, after which the file is deleted.
     *
     * If some error happens and tha file is corrupted the file is removed,
     * so that the integrity is kept.
     *
     * @param archive - holds the file which has the informations backed up data
     *
     * @return the object of the type BackUpInfoFileGroup that holds all of the
     *         information about the files that were backed up
     *
     * @throws BackupException if IO error occures
     */
    public BackUpInfoFileGroup execute(File archive) {
        try {
            this.restoraDataFile(archive);
            ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(IMPORT_PATH)));
            BackUpInfoFileGroup backUpData = (BackUpInfoFileGroup) (in.readObject());
            in.close();
            new File(IMPORT_PATH).delete();
            return backUpData;
        } catch (IOException e) {
            archive.delete();
            throw new BackupException(e.getMessage());
        } catch (ClassNotFoundException e) {
            archive.delete();
            throw new BackupException(e.getMessage());
        }
    }

    /**
     * This method is used to restore only the file that has the back up info
     * all other files are skiped
     *
     * @param archive - holds the file which has the informations about the data
     *                  that was backed up
     *
     * @throws BackupException if IO error occures
     */
    private void restoraDataFile(File archive) throws IOException {
        try {
            ZipFile zipfile = new ZipFile(archive);
            for (Enumeration<?> e = zipfile.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                unzipData(zipfile, entry);
            }
            zipfile.close();
        } catch (IOException e) {
            throw new BackupException(e.getMessage());
        }
    }

    /**
     * This is a private method used to restore only the file that has the back up info
     * it first cheks to see if the given file is the one that has all of the information
     * if it is it is resored otherwise it is skiped
     *
     * @param zipfile - holds the file archive
     * @param entry - holds one entry from the archive
     *
     * @throws BackupException if IO error occures
     */
    private void unzipData(ZipFile zipfile, ZipEntry entry) {
        if (entry.getName().equals("backUpExternalInfo.out")) {
            File outputFile = new File("temp", entry.getName());
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            try {
                BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
                IOUtils.copy(inputStream, outputStream);
                outputStream.close();
                inputStream.close();
            } catch (IOException e) {
                throw new BackupException(e.getMessage());
            }
        }
    }
}
