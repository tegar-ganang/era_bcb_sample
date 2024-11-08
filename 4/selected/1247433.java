package gov.lanl.archive.impl;

import gov.lanl.archive.ArchiveConfig;
import gov.lanl.archive.ArchiveConstants;
import gov.lanl.archive.ArchiveProfile;
import gov.lanl.archive.ProfileProcessor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * When provided a list of source ARCfiles and XMLtapes, this processor 
 * copies the source files to the archive storage locations and updates 
 * the file list in preparation for indexing and registration.
 * 
 * @author Ryan Chute <rchute@lanl.gov>
 *
 */
public class StandardProcessor implements ProfileProcessor {

    ArrayList<File> arcFileList = new ArrayList<File>();

    ArrayList<File> xmlTapeList = new ArrayList<File>();

    ArrayList<File> sourceArcFileList;

    ArrayList<File> sourceXmlTapeList;

    ArchiveConfig archiveConfig;

    ArchiveProfile archiveProfile;

    /**
     * Gets list of ARCfile File Objects for indexing & registration
     * @return
     *        ArrayList of File objects for post-processing
     */
    public ArrayList<File> getArcFileList() {
        return arcFileList;
    }

    /**
     * Gets list of XMLtape File Objects for indexing & registration
     * @return
     *        ArrayList of File objects for post-processing
     */
    public ArrayList<File> getXMLTapeList() {
        return xmlTapeList;
    }

    /**
     * Sets ArchiveConfig Properties
     * @param config
     *        ArchiveConfig for adore archive
     */
    public void setArchiveConfig(ArchiveConfig config) {
        this.archiveConfig = config;
    }

    /**
     * Sets ArchiveProfile Pre-processing Properties
     * @param profile
     *        ArchiveProfile for collection pre-processing
     */
    public void setArchiveProfile(ArchiveProfile profile) {
        this.archiveProfile = profile;
    }

    /**
     * Sets list of files to be processed
     * @param type
     *        Content Type Alias
     * @param sourceFiles
     *        List of files to be preprocessed
     */
    public void setSourceArray(int type, ArrayList sourceFiles) {
        if (type == ArchiveConstants.TYPE_XML) {
            this.sourceXmlTapeList = sourceFiles;
        } else if (type == ArchiveConstants.TYPE_BINARY) {
            this.sourceArcFileList = sourceFiles;
        }
    }

    /**
     * Copy source files to destination directory, update File ArrayList
     */
    public void runIt() throws Exception {
        if (sourceXmlTapeList != null && sourceXmlTapeList.size() > 0) {
            String tapeDestDir = null;
            if ((tapeDestDir = archiveConfig.getCollectionTapeStoreDirectory(archiveProfile.getCollectionProfilePrefix())) == null) tapeDestDir = archiveConfig.getTapeStoredDirectory();
            for (Iterator i = sourceXmlTapeList.iterator(); i.hasNext(); ) {
                File sourceFile = (File) i.next();
                File targetFile = copyFile(tapeDestDir, sourceFile);
                xmlTapeList.add(targetFile);
            }
        }
        if (sourceArcFileList != null && sourceArcFileList.size() > 0) {
            String arcDestDir = null;
            if ((arcDestDir = archiveConfig.getCollectionArcStoreDirectory(archiveProfile.getCollectionProfilePrefix())) == null) arcDestDir = archiveConfig.getARCStoredDirectory();
            for (Iterator i = sourceArcFileList.iterator(); i.hasNext(); ) {
                File sourceFile = (File) i.next();
                File targetFile = copyFile(arcDestDir, sourceFile);
                arcFileList.add(targetFile);
            }
        }
    }

    /**
     * Copy source file to destination directory, returns File object 
     * for copied file
     * @param destDir
     *        Directory to copy file to.
     * @param sourceFile
     *        File to be copied.
     * @return
     *        File object containing the path to the copied file
     * @throws Exception
     */
    private File copyFile(String destDir, File sourceFile) throws Exception {
        long start = System.currentTimeMillis();
        String sourceFileName = sourceFile.getName();
        File targetFile = new File(destDir, sourceFileName);
        if (sourceFile.getAbsolutePath().equals(targetFile.getAbsolutePath())) return targetFile;
        copyFile(sourceFile, targetFile);
        long duration = System.currentTimeMillis() - start;
        System.out.println("File: " + sourceFileName);
        System.out.println("Duration: " + duration);
        return targetFile;
    }

    /**
     * Copy file from "in" to "out"
     * @param in
     *        File to be copied
     * @param out
     *        Destination to write file to.
     * @throws Exception
     */
    private void copyFile(File in, File out) throws Exception {
        FileChannel sourceChannel = new FileInputStream(in).getChannel();
        FileChannel destinationChannel = new FileOutputStream(out).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        sourceChannel.close();
        destinationChannel.close();
    }
}
