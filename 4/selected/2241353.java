package org.code4flex.codegenerators.resourcexporter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 
 * @author Facundo Merighi
 * @version $Revision: 1.1 $
 */
public class UnzipResourceExporter extends ResourceExporterCodeGenerator {

    @Override
    public void exportResource() throws IOException {
        createPathIfDontExist();
        String fileResource = this.codeGenerator.getTemplatePath() + File.separator + this.getSourceDirectory() + this.resourceToExport;
        String folderOutput = this.getFinalPath() + File.separator;
        unzip(fileResource, folderOutput);
    }

    @Override
    public String getFinalPath() {
        return this.codeGenerator.getProyectDestPath();
    }

    private void unzip(String fileResource, String folderOutput) throws IOException {
        Enumeration<? extends ZipEntry> entries;
        ZipFile zipFile;
        try {
            zipFile = new ZipFile(fileResource);
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory()) {
                    (new File(folderOutput + entry.getName())).mkdir();
                    continue;
                }
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(folderOutput + entry.getName())));
            }
            zipFile.close();
        } catch (IOException ioe) {
            System.err.println("Unhandled exception:");
            ioe.printStackTrace();
            throw ioe;
        }
    }

    private static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }
}
