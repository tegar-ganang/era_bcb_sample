package net.sourceforge.processdash.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FullDirectoryBackup extends DirectoryBackup {

    @Override
    protected void doBackup(File outputZipFile) throws IOException {
        ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputZipFile)));
        List<String> filenames = getFilenamesToBackup();
        if (filenames.size() == 0) {
            zipOut.putNextEntry(new ZipEntry("No_Files_Found"));
        } else {
            for (String filename : filenames) {
                backupFile(zipOut, filename);
            }
        }
        if (extraContentSupplier != null) extraContentSupplier.addExtraContentToBackup(zipOut);
        zipOut.close();
    }

    private void backupFile(ZipOutputStream zipOut, String filename) throws IOException {
        ZipEntry e = new ZipEntry(filename);
        File file = new File(srcDirectory, filename);
        e.setTime(file.lastModified());
        zipOut.putNextEntry(e);
        FileUtils.copyFile(file, zipOut);
        zipOut.closeEntry();
    }
}
