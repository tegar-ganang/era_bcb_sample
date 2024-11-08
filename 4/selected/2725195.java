package javax.jcr.tools.backup.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import javax.jcr.tools.backup.Context;
import javax.jcr.tools.backup.PersistenceException;
import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.zip.Deflater;

/**
 * Zip file persistence manager.
 *
 * @author Ivan Latysh <ivan@yourmail.com>
 * @version 0.1
 * @since 19-Feb-2008 10:35:23 AM
 */
public class ZipPersistenceManagerImpl extends FSPersistenceManagerImpl {

    /** Logger */
    protected Logger logger = Logger.getLogger(this.getClass().getName());

    /** Zip file */
    protected File backup;

    /** Mode */
    protected String mode;

    /**
   * Explode backup
   * 
   * @throws java.io.IOException when unable to explode backup
   */
    protected void unZip() throws PersistenceException {
        boolean newZip = false;
        try {
            if (null == backup) {
                mode = (String) context.get(Context.MODE);
                if (null == mode) mode = Context.MODE_NAME_RESTORE;
                backupDirectory = (File) context.get(Context.BACKUP_DIRECTORY);
                logger.debug("Got backup directory {" + backupDirectory.getAbsolutePath() + "}");
                if (!backupDirectory.exists() && mode.equals(Context.MODE_NAME_BACKUP)) {
                    newZip = true;
                    backupDirectory.mkdirs();
                } else if (!backupDirectory.exists()) {
                    throw new PersistenceException("Backup directory {" + backupDirectory.getAbsolutePath() + "} does not exist.");
                }
                backup = new File(backupDirectory + "/" + getBackupName() + ".zip");
                logger.debug("Got zip file {" + backup.getAbsolutePath() + "}");
            }
            File _explodedDirectory = File.createTempFile("exploded-" + backup.getName() + "-", ".zip");
            _explodedDirectory.mkdirs();
            _explodedDirectory.delete();
            backupDirectory = new File(_explodedDirectory.getParentFile(), _explodedDirectory.getName());
            backupDirectory.mkdirs();
            logger.debug("Created exploded directory {" + backupDirectory.getAbsolutePath() + "}");
            if (!backup.exists() && mode.equals(Context.MODE_NAME_BACKUP)) {
                newZip = true;
                backup.createNewFile();
            } else if (!backup.exists()) {
                throw new PersistenceException("Backup file {" + backup.getAbsolutePath() + "} does not exist.");
            }
            if (newZip) return;
            ZipFile zip = new ZipFile(backup);
            Enumeration zipFileEntries = zip.entries();
            while (zipFileEntries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
                String currentEntry = entry.getName();
                logger.debug("Inflating: " + entry);
                File destFile = new File(backupDirectory, currentEntry);
                File destinationParent = destFile.getParentFile();
                destinationParent.mkdirs();
                if (!entry.isDirectory()) {
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        in = zip.getInputStream(entry);
                        out = new FileOutputStream(destFile);
                        IOUtils.copy(in, out);
                    } finally {
                        if (null != out) out.close();
                        if (null != in) in.close();
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Unable to unzip {" + backup + "}", e);
            throw new PersistenceException(e);
        }
    }

    /**
   * Zip content
   *
   * @throws PersistenceException
   */
    public void zipUp() throws PersistenceException {
        ZipOutputStream out = null;
        try {
            if (!backup.exists()) backup.createNewFile();
            out = new ZipOutputStream(new FileOutputStream(backup));
            out.setLevel(Deflater.DEFAULT_COMPRESSION);
            for (String file : backupDirectory.list()) {
                logger.debug("Deflating: " + file);
                FileInputStream in = null;
                try {
                    in = new FileInputStream(new File(backupDirectory, file));
                    out.putNextEntry(new ZipEntry(file));
                    IOUtils.copy(in, out);
                } finally {
                    out.closeEntry();
                    if (null != in) in.close();
                }
            }
            FileUtils.deleteDirectory(backupDirectory);
        } catch (Exception ex) {
            logger.error("Unable to ZIP the backup {" + backupDirectory.getAbsolutePath() + "}.", ex);
            throw new PersistenceException(ex);
        } finally {
            try {
                if (null != out) out.close();
            } catch (IOException e) {
                logger.error("Unable to close ZIP output stream.", e);
            }
        }
    }

    /** {@inheritDoc} */
    public InputStream getInResource(String name, boolean autoCreate) throws PersistenceException, IOException {
        if (null == backupDirectory) unZip();
        return super.getInResource(name, autoCreate);
    }

    /** {@inheritDoc} */
    public boolean isResourceExist(String name) throws PersistenceException {
        if (null == backupDirectory) unZip();
        return super.isResourceExist(name);
    }

    /** {@inheritDoc} */
    public OutputStream getOutResource(String name, boolean autoCreate) throws PersistenceException {
        if (null == backupDirectory) unZip();
        return super.getOutResource(name, autoCreate);
    }

    /** {@inheritDoc} */
    public void destroy() {
        if (null != mode && mode.equals(Context.MODE_NAME_BACKUP)) {
            try {
                zipUp();
            } catch (PersistenceException e) {
                logger.error("Unable to zip up backup content, content has been left as is in {" + backupDirectory.getAbsolutePath() + "}");
            }
        } else {
            if (null != backupDirectory) {
                try {
                    logger.debug("Deleting {" + backupDirectory.getAbsolutePath() + "}");
                    FileUtils.deleteDirectory(backupDirectory);
                } catch (Exception e) {
                    logger.error("Unable to delete exploded directory {" + backupDirectory.getAbsolutePath() + "}", e);
                }
            }
        }
    }
}
