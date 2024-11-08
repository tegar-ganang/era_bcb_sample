package ch.bbv.mda.persistence.nsuml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import ch.bbv.mda.persistence.PersistenceException;

/**
 * Returns the content of the *.argo file within a *.zargo archive readable to
 * ArgoUml.
 * @author Adrian Bachofen, Marcel Baumann
 * @version $Revision: 1.2 $
 */
public class ArgoWriter {

    /**
   * XML format at the begining of the file.
   */
    private static final String HEADER = "<?xml version = \"1.0\" encoding = \"ISO-8859-1\" ?>\r\n<!DOCTYPE argo SYSTEM \"argo.dtd\" >\r\n<argo>\r\n  <documentation>\r\n    <authorname>pmMDA</authorname>\r\n    <version>0.15.2</version>\r\n    <description>\r\n    This file has been generated using pmMDA - the MDA tool!\r\n    </description>\r\n  </documentation>\r\n\r\n  <member\r\n    type=\"xmi\"\r\n    name=\"{0}\"\r\n  />\r\n</argo>";

    /**
   * The ArgoUML extension with dot for ArgoUML generated model files.
   */
    private static final String ARGOUML_EXT = ".zargo";

    /**
   * The ArgoUML extension with dot for ArgoUML XMI generated model files.
   */
    private static final String XMI_EXT = ".xmi";

    /**
   * Returns the content of an argoUML file.
   * @param xmiFileName full file name of the xmi content with or without
   *          extension (.xmi) but with no path information.
   * @return String containing the *.argo content.
   */
    public static String getArgoContent(String xmiFileName) {
        if (!xmiFileName.endsWith(XMI_EXT)) xmiFileName += XMI_EXT;
        return MessageFormat.format(HEADER, new Object[] { xmiFileName });
    }

    /**
   * Writes a new argoUML file.
   * @param file non existing file with extension *.zargo.
   * @param input existing file with xmi content.
   * @throws PersistenceException if an error occured during the IO operations.
   */
    public static void writeEntry(File file, File input) throws PersistenceException {
        try {
            File temporaryFile = File.createTempFile("pmMDA_zargo", ARGOUML_EXT);
            temporaryFile.deleteOnExit();
            ZipOutputStream output = new ZipOutputStream(new FileOutputStream(temporaryFile));
            FileInputStream inputStream = new FileInputStream(input);
            ZipEntry entry = new ZipEntry(file.getName().substring(0, file.getName().indexOf(ARGOUML_EXT)) + XMI_EXT);
            output.putNextEntry(new ZipEntry(entry));
            IOUtils.copy(inputStream, output);
            output.closeEntry();
            inputStream.close();
            entry = new ZipEntry(file.getName().substring(0, file.getName().indexOf(ARGOUML_EXT)) + ".argo");
            output.putNextEntry(new ZipEntry(entry));
            output.write(ArgoWriter.getArgoContent(file.getName().substring(0, file.getName().indexOf(ARGOUML_EXT)) + XMI_EXT).getBytes());
            output.closeEntry();
            output.close();
            temporaryFile.renameTo(file);
        } catch (IOException ioe) {
            throw new PersistenceException(ioe);
        }
    }
}
