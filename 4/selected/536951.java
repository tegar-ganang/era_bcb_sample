package com.safi.asterisk.handler.mbean;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import com.safi.asterisk.handler.SafletEngine;
import com.safi.asterisk.handler.importing.SafiArchiveImporter;
import com.safi.asterisk.handler.importing.OverwriteMode;
import com.safi.asterisk.handler.util.FileUtils;
import com.safi.db.manager.DBManager;
import com.safi.db.manager.DBManagerException;
import com.safi.db.server.config.Prompt;

public class FileTransferImpl implements FileTransfer {

    private static final Logger log = Logger.getLogger(FileTransferImpl.class);

    @Override
    public void transferJar(String filename, byte[] data) throws Exception {
        File file = new File(SafletEngine.getInstance().getActionpakDirectory() + File.separatorChar + filename);
        FileUtils.writeFile(file.getAbsolutePath(), data);
        SafletEngine.getInstance().setActionpakPkgsInitated(false);
        SafletEngine.getInstance().loadActionpaks();
    }

    @Override
    public void transferWorkspaceArchive(String filename, byte[] data, OverwriteMode mode) throws Exception {
        SafiArchiveImporter importer = SafletEngine.getInstance().getPollManager();
        if (importer == null) {
            throw new IllegalStateException("No SafiArchiveImporter has been configured for this server.");
        }
        File file = new File(SafletEngine.getInstance().getImportDirectory() + File.separatorChar + "received" + File.separatorChar + filename);
        FileUtils.writeFile(file.getAbsolutePath(), data);
        importer.doImport(file, mode);
    }

    @Override
    public boolean needsUpdateActionPak(String filename) {
        File file = new File(SafletEngine.getInstance().getActionpakDirectory() + File.separatorChar + filename);
        return !file.exists() && FileUtils.jarIsNewer(filename, SafletEngine.getInstance().getActionpakJars().toArray(new File[SafletEngine.getInstance().getActionpakJars().size()]));
    }

    @Override
    public void transferAudioFile(String project, String filename, byte[] data) throws Exception {
        String fn = "safi" + File.separatorChar + (StringUtils.isBlank(project) ? "shared" + File.separatorChar + filename : ("project" + File.separatorChar + (project.trim() + File.separatorChar + filename)));
        File file = new File(SafletEngine.getInstance().getAudioDirectoryRoot() + File.separatorChar + fn);
        if (file.exists() && !file.delete()) {
            throw new IOException("File " + filename + " exists and could not be deleted");
        }
        file.getParentFile().mkdirs();
        FileUtils.writeFile(file.getAbsolutePath(), data);
    }

    @Override
    public void renamePromptFile(int promptId, String projectName, String name, String extension) throws Exception {
        Prompt p = DBManager.getInstance().getPromptByID(promptId);
        if (p == null) throw new DBManagerException("Couldn't find prompt with id " + promptId);
        String oldName = "safi" + File.separatorChar + (p.getProject() == null ? "shared" + File.separatorChar + p.getName() + "." + p.getExtension() : ("project" + File.separatorChar + (p.getProject().getName() + File.separatorChar + p.getName() + "." + p.getExtension())));
        String newName = "safi" + File.separatorChar + (projectName == null ? "shared" + File.separatorChar + name + "." + extension : ("project" + File.separatorChar + (projectName + File.separatorChar + name + "." + extension)));
        String oldNameFull = SafletEngine.getInstance().getAudioDirectoryRoot() + File.separatorChar + oldName;
        String newNameFull = SafletEngine.getInstance().getAudioDirectoryRoot() + File.separatorChar + newName;
        if (StringUtils.equals(oldNameFull, newNameFull)) {
            log.info("Prompt path was unchanged: " + oldNameFull);
            return;
        }
        File newFile = new File(newNameFull);
        File parent = newFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) throw new IOException("Couldn't create directory path: " + parent);
        File oldFile = new File(oldNameFull);
        if (newFile.equals(oldFile)) return;
        if (parent.equals(newFile.getParent())) {
            if (!oldFile.renameTo(newFile)) {
                throw new IOException("Couldn't rename " + oldFile + " to " + newFile);
            }
        } else {
            FileUtils.copyFile(oldFile, newFile);
            FileUtils.deleteFileAndEmptyParents(oldFile);
        }
    }

    @Override
    public boolean needsUpdateAudioFile(String project, String filename, Date timestamp) {
        String fn = "safi" + File.separatorChar + (StringUtils.isBlank(project) ? "shared" + File.separatorChar + filename : ("project" + File.separatorChar + (project.trim() + File.separatorChar + filename)));
        File file = new File(SafletEngine.getInstance().getAudioDirectoryRoot() + File.separatorChar + fn);
        if (file.exists() && file.isFile() && file.lastModified() >= timestamp.getTime()) {
            return false;
        }
        return true;
    }

    @Override
    public void transferServerJar(String filename, byte[] data) throws Exception {
        File file = new File("lib/" + filename);
        FileUtils.writeFile(file.getAbsolutePath(), data);
        SafletEngine.getInstance().getServiceConfigUpdater().updateServiceConfig();
    }

    @Override
    public boolean needsUpdateServerJar(String filename) {
        return !new File("lib/" + filename).exists() && SafletEngine.getInstance().getServiceConfigUpdater().jarIsNewer(filename);
    }
}
