package ch.bbv.mda.persistence;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;

/**
 * The Zip file utilities class provides convenience methods to manipulate a ZIP
 * file containing a XMI model.
 * @author MarcelBaumann
 * @version $Revision: 1.8 $
 */
public class ZipFileUtilities {

    /**
   * Replace the entry with the given name in the zip file. The content of the
   * new entry is given as stream. This method implements the following
   * algorithm.
   * <ul>
   * <li>Create a temporary file in the same directory where the file is
   * located.</li>
   * <li>Copy each entry for the original file to the temporary file. If the
   * entry name is the one passed as parameters, replace the entry with the
   * given stream.</li>
   * <li>Close the temporary file. If the operation was sucessfull, delete the
   * original file and rename the temporary name to the original file.
   * </ul>
   * @param file zip file containing the entry.
   * @param entryName name of the entry to be replaced.
   * @param stream new content of the entry to update.
   * @throws PersistenceException an I/O error occurred during processing.
   */
    public static void replaceEntry(File file, String entryName, InputStream stream) throws PersistenceException {
        try {
            File temporaryFile = File.createTempFile("pmMDA_zargo", ".zargo");
            temporaryFile.deleteOnExit();
            FileInputStream inputStream = new FileInputStream(file);
            ZipInputStream input = new ZipInputStream(inputStream);
            ZipOutputStream output = new ZipOutputStream(new FileOutputStream(temporaryFile));
            ZipEntry entry = input.getNextEntry();
            while (entry != null) {
                ZipEntry zipEntry = new ZipEntry(entry);
                zipEntry.setCompressedSize(-1);
                output.putNextEntry(zipEntry);
                if (!entry.getName().equals(entryName)) {
                    IOUtils.copy(input, output);
                } else {
                    IOUtils.copy(stream, output);
                }
                input.closeEntry();
                output.closeEntry();
                entry = input.getNextEntry();
            }
            input.close();
            inputStream.close();
            output.close();
            System.gc();
            boolean isSuccess = file.delete();
            if (!isSuccess) {
                throw new PersistenceException();
            }
            isSuccess = temporaryFile.renameTo(file);
            if (!isSuccess) {
                throw new PersistenceException();
            }
        } catch (IOException e) {
            throw new PersistenceException(e);
        }
    }
}
