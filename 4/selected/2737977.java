package org.purl.sword.server.fedora.utils;

import java.util.List;
import java.util.ArrayList;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Enumeration;
import java.util.Random;
import org.purl.sword.server.fedora.utils.FindMimeType;
import org.purl.sword.server.fedora.fedoraObjects.Datastream;
import org.purl.sword.server.fedora.fedoraObjects.LocalDatastream;
import org.purl.sword.base.SWORDException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class ZipFileAccess {

    private static final Logger LOG = Logger.getLogger(ZipFileAccess.class);

    protected String _tmpExtractDirName = "";

    /**
	 * Setup this object and tell it where it can extract the zip file to.
	 *
	 * @param String the location where the zip file can be extracted to.
	 */
    public ZipFileAccess(final String pTempDir) {
        this.setTmpDir(pTempDir);
    }

    /**
	 * Get tempLocation.
	 *
	 * @return tempLocation as String.
	 */
    public String getTmpExtractDirName() {
        return _tmpExtractDirName;
    }

    /**
	 * Set tempLocation.
	 *
	 * @param tempLocation the value to set.
	 */
    public void setTmpDir(final String pTempDir) {
        Random tRand = new Random();
        _tmpExtractDirName = pTempDir + "zip-extract-" + tRand.nextInt();
    }

    /** 
	 * This returns a list of all the files that were in the zip file as a list of datastreams.
	 *
	 * @param String the zip file
	 * @return List<Datastream> a list of datastreams
	 * @throws IOException if there was a problem extracting the Zip file or if accessing the files.
	 */
    public List<Datastream> getFiles(final String pFile) throws IOException {
        List<Datastream> tDatastreams = new ArrayList<Datastream>();
        new File(this.getTmpExtractDirName()).mkdir();
        ZipFile tZipFile = new ZipFile(pFile);
        Enumeration tEntries = tZipFile.entries();
        ZipEntry tEntry = null;
        File tFile = null;
        String tFileLocation = "";
        LocalDatastream tLocalDs = null;
        while (tEntries.hasMoreElements()) {
            tEntry = (ZipEntry) tEntries.nextElement();
            if (tEntry.isDirectory()) {
                continue;
            }
            tFileLocation = this.getTmpExtractDirName() + System.getProperty("file.separator") + tEntry.getName();
            tFile = new File(tFileLocation);
            LOG.debug("Saving " + tEntry.getName() + " to " + tFile.getPath());
            tFile.getParentFile().mkdirs();
            IOUtils.copy(tZipFile.getInputStream(tEntry), new FileOutputStream(tFile));
            tLocalDs = new LocalDatastream(tFile.getName().split("\\.")[0], FindMimeType.getMimeType(tFile.getName().split("\\.")[1]), tFileLocation);
            tLocalDs.setLabel(tEntry.getName());
            tDatastreams.add(tLocalDs);
        }
        return tDatastreams;
    }

    /**
	 * After ingest this removes all the directories that were created during the ingest. It will not 
	 * remove files that are still in the temp location so ensure you have removed any files that are under this directory
	 * otherwise a SWORDException will be thrown.
	 *
	 * @throws SWORDException if a file is present in the extract of the zip file after ingest has taken place
	 */
    public void removeLocalFiles() throws SWORDException {
        this.recursiveDelete(new File(this.getTmpExtractDirName()));
    }

    /**
	 * Recursive method to remove the directories created during the zip extract
	 *
	 * @throws SWORDException if a file is present in the extract of the zip file after ingest has taken place
	 */
    protected void recursiveDelete(final File pDir) throws SWORDException {
        File[] tFiles = pDir.listFiles();
        if (tFiles.length == 0) {
            pDir.delete();
            return;
        }
        for (int i = 0; i < tFiles.length; i++) {
            if (tFiles[i].isDirectory()) {
                if (tFiles[i].listFiles().length == 0) {
                    tFiles[i].delete();
                } else {
                    this.recursiveDelete(tFiles[i]);
                }
            } else {
                tFiles[i].delete();
            }
        }
        pDir.delete();
    }
}
