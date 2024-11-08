package net.sourceforge.jbackupfw.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.sourceforge.jbackupfw.core.data.BackUpInfoFile;
import net.sourceforge.jbackupfw.core.data.BackUpInfoFileGroup;
import net.sourceforge.jbackupfw.core.data.BackupException;
import org.apache.commons.io.IOUtils;

/**
 * This class provides the basic system operations for restoring data it can restore
 * to the original location from which the date was originlly backed up or to the
 * new location where the user wants it. The method can also restore all the files
 * or only the chosen ones.
 * 
 * @author Boris Horvat
 *
 */
public class Restore {

    /** this attribute shows the number of the file which is being backed up*/
    private int counter = 0;

    /**
     * This is a method that is used to restore the data, it can restore to the
     * original location from which the date was originlly backed up or to the
     * new location where the user wants it. The method can also restore all the files
     * or only the chosen ones.
     *  
     * @param archive - holds the path to a archive from which the files need to be restored
     * @param outputDir - the location where to files will be restored
     * @param fileGroup - holds the informations about all of the files that were backed up
     * @param restoreList - list of files that need to be restored
     *
     * @throws BackupException if IO error occures
     */
    public void execute(File archive, File outputDir, BackUpInfoFileGroup fileGroup, LinkedList<String> restoreList) {
        this.counter = 0;
        try {
            ZipFile zipfile = new ZipFile(archive);
            for (Enumeration<?> e = zipfile.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                unzipEntry(zipfile, entry, outputDir, fileGroup, restoreList);
            }
            zipfile.close();
        } catch (IOException e) {
            throw new BackupException(e.getMessage());
        }
    }

    /**
     * Returns the number of file that is currently being backed up
     *
     * @return the intager number of file that is currently being backed up
     *         0 if the process is begining
     */
    public int getCounter() {
        return counter;
    }

    /**
     * This method is used to restore all the files that needs to be restored,
     * it first cheks to see if the file sholud be restored if not the file is skiped
     * else the file is restored to the provied location
     *
     * @param zipfile - represents the archive from whice the data must be restored
     * @param entry - represents one entry in the archive
     * @param outputDir - the location where to files will be restored
     * @param fileGroup - holds the informations about all of the files that were backed up
     * @param restoreList - list of files that need to be restored
     *
     * @throws BackupException if IO error occures
     */
    private void unzipEntry(ZipFile zipfile, ZipEntry entry, File outputDir, BackUpInfoFileGroup fileGroup, LinkedList<String> restoreList) {
        LinkedList<BackUpInfoFile> fileList = fileGroup.getFileList();
        if (entry.isDirectory()) {
            createDir(new File(outputDir, entry.getName()));
            return;
        }
        for (int i = 0; i < fileList.size(); i++) {
            if (fileList.get(i).getId().equals(entry.getName())) {
                for (int j = 0; j < restoreList.size(); j++) {
                    if ((fileList.get(i).getName() + "." + fileList.get(i).getType()).equals(restoreList.get(j))) {
                        counter += 1;
                        File outputFile = new File(outputDir, fileList.get(i).getName() + "." + fileList.get(i).getType());
                        if (!outputFile.getParentFile().exists()) {
                            createDir(outputFile.getParentFile());
                        }
                        BufferedInputStream inputStream;
                        BufferedOutputStream outputStream;
                        try {
                            inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
                            outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
                            IOUtils.copy(inputStream, outputStream);
                            outputStream.close();
                            inputStream.close();
                        } catch (IOException ex) {
                            throw new BackupException(ex.getMessage());
                        }
                    }
                }
            }
        }
    }

    /**
     * This is a private method that is used to create directory which sholud
     * hold the file that is being restored, also it will creat any parrent
     * directory if it is missing
     *
     * @param directory - holds the location where the directory must be created
     * 
     * @throws BackupException if the directory can not be created
     */
    private void createDir(File directory) {
        if (!directory.mkdirs()) {
            throw new BackupException("Can not create the fallowing directorium: " + directory);
        }
    }
}
