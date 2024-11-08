package org.anton.z.music.backup.model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.swing.event.EventListenerList;
import org.anton.z.music.backup.BackupException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

/**
 * TODO make unique directories in backup dir
 * 
 * @author anton
 */
public class BackupRunner {

    public static final Logger log = Logger.getLogger(BackupRunner.class);

    private final EventListenerList listeners;

    public boolean status;

    private final Statistics statistics;

    public BackupRunner(Statistics statistics) {
        this.statistics = statistics;
        listeners = new EventListenerList();
    }

    /**
	 * TODO
	 */
    public void runBackup(List<String> backupDirsList, File targetDirectory) throws BackupException {
        for (String backupDirPath : backupDirsList) {
            File backupDir = new File(backupDirPath);
            backupDirectory(backupDir, targetDirectory);
        }
    }

    private void backupDirectory(File backupDir, File targetDirectory) throws BackupException {
        if (backupDir.exists() && backupDir.isDirectory()) {
            statistics.setTotalDirectories(statistics.getTotalDirectories() + 1);
            log.info("Processing directory: " + backupDir.getAbsolutePath());
            fireProcessingDirectory(backupDir);
            File newDirectory = new File(targetDirectory.getAbsolutePath() + File.separator + backupDir.getName());
            if (!newDirectory.exists()) {
                log.info("Found new directory to backup: \"" + backupDir.getAbsolutePath() + "\". Backuping this directory...");
                if (!newDirectory.mkdir()) {
                    throw new BackupException("Unable to create directory: \"" + newDirectory.getAbsolutePath() + "\"");
                } else {
                    statistics.setNewDirectories(statistics.getNewDirectories() + 1);
                }
            }
            File[] backupFiles = backupDir.listFiles();
            File[] backupedFilesArray = newDirectory.listFiles();
            Map<String, File> backupedFiles = new HashMap<String, File>();
            for (int i = 0; i < backupedFilesArray.length; i++) {
                backupedFiles.put(backupedFilesArray[i].getName(), backupedFilesArray[i]);
            }
            for (File backupFile : backupFiles) {
                backupFile(backupFile, newDirectory);
                backupedFiles.remove(backupFile.getName());
            }
            deleteFiles(backupedFiles);
        } else {
            log.info("Directory does not exist: " + backupDir.getAbsolutePath());
        }
    }

    private void fireProcessingDirectory(File backupDir) {
        for (IBackupListener listener : listeners.getListeners(IBackupListener.class)) {
            listener.processingDirectory(backupDir);
        }
    }

    private void backupFile(File backupFile, File targetDirectory) throws BackupException {
        if (backupFile.exists()) {
            if (backupFile.isDirectory()) {
                backupDirectory(backupFile, targetDirectory);
            } else {
                statistics.setTotalFiles(statistics.getTotalFiles() + 1);
                statistics.setTotalBackupFilesSize(statistics.getTotalBackupFilesSize() + backupFile.length());
                String targetFileName = targetDirectory.getAbsoluteFile() + File.separator + backupFile.getName();
                File targetFile = new File(targetFileName);
                if (!targetFile.exists()) {
                    statistics.setNewFiles(statistics.getNewFiles() + 1);
                    statistics.setTotalNewFilesSize(statistics.getTotalNewFilesSize() + backupFile.length());
                    log.info("Found new file to backup: \"" + backupFile.getAbsolutePath() + "\". Backuping this file...");
                    copy(backupFile, targetFile);
                } else {
                    if (updateFileTargetFile(backupFile, targetFile)) {
                        statistics.setUpdatedFiles(statistics.getUpdatedFiles() + 1);
                        statistics.setTotalUpdatedFilesSize(statistics.getTotalUpdatedFilesSize() + backupFile.length());
                        log.info("Found updated file: \"" + backupFile.getAbsolutePath() + "\". Updating this file...");
                        copy(backupFile, targetFile);
                    }
                }
            }
        } else {
            log.warn("File does not exists: " + backupFile.getAbsolutePath() + "\"");
        }
    }

    private void copy(File source, File dest) {
        try {
            dest.delete();
            FileUtils.copyFile(source, dest);
        } catch (IOException e) {
            log.error("Unable to copy file: \"" + source.getAbsolutePath() + "\" to \"" + dest.getAbsolutePath() + "\".");
            log.debug(e);
            throw new RuntimeException(e);
        }
    }

    private void checkThumbsFile(File dest) {
        if (dest.getName().equalsIgnoreCase("Thumbs.db") || dest.getName().equalsIgnoreCase("Desktop.ini")) {
            dest.delete();
        }
    }

    /**
	 * Checks if target file has to be updated.
	 * 
	 * @param backupFile - file which should be backed up
	 * @param targetFile - destination file
	 * 
	 * @return true if file should be overwritten
	 */
    private boolean updateFileTargetFile(File backupFile, File targetFile) {
        if (backupFile.length() != targetFile.length()) {
            return true;
        }
        return false;
    }

    /**
	 * TODO
	 */
    public void showStatistics() {
        log.info("______________________________________________");
        log.info("B A C K U P   S T A T I S T I C S:");
        log.info("Total directories - " + statistics.getTotalDirectories());
        log.info("Total files - " + statistics.getTotalFiles());
        log.info("New directories - " + statistics.getNewDirectories());
        log.info("New files - " + statistics.getNewFiles());
        log.info("Updated files - " + statistics.getUpdatedFiles());
        log.info("Deleted directories - " + statistics.getDeletedDirectories());
        log.info("Deleted files - " + statistics.getDeletedFiles());
        log.info("");
        log.info("Total backup size - " + statistics.getTotalBackupFilesSize() + " bytes");
        log.info("Total backup size - " + NumberFormatter.formatSizeInBytes(statistics.getTotalBackupFilesSize()) + " bytes");
        log.info("New files size - " + statistics.getTotalNewFilesSize() + " bytes");
        log.info("New files size - " + NumberFormatter.formatSizeInBytes(statistics.getTotalNewFilesSize()) + " bytes");
        log.info("Updated files size - " + statistics.getTotalUpdatedFilesSize() + " bytes");
        log.info("Updated files size - " + NumberFormatter.formatSizeInBytes(statistics.getTotalUpdatedFilesSize()) + " bytes");
        log.info("Deleted files size - " + statistics.getTotalDeletedFilesSize() + " bytes");
        log.info("Deleted files size - " + NumberFormatter.formatSizeInBytes(statistics.getTotalDeletedFilesSize()) + " bytes");
        log.info("");
        showTime();
        log.info("______________________________________________");
    }

    private void deleteFiles(Map<String, File> backupedFiles) throws BackupException {
        Set<Map.Entry<String, File>> set = backupedFiles.entrySet();
        for (Entry<String, File> mapEntry : set) {
            deleteFile(mapEntry.getValue());
        }
    }

    private void showTime() {
        long elapsedTime = statistics.getEndTime() - statistics.getStartTime();
        long hours = elapsedTime / (1000 * 60 * 60);
        long minutes = (elapsedTime - (hours * 1000 * 60 * 60)) / (1000 * 60);
        long seconds = (elapsedTime - (hours * 1000 * 60 * 60) - (minutes * 1000 * 60)) / 1000;
        long ms = elapsedTime - (hours * 1000 * 60 * 60) - (minutes * 1000 * 60) - (seconds * 1000);
        StringBuilder logText = new StringBuilder();
        logText.append("Time elapsed: ");
        if (hours > 0) {
            logText.append(hours + " hours ");
        }
        if (minutes > 0) {
            logText.append(minutes + " minutes ");
        }
        if (seconds > 0) {
            logText.append(seconds + " seconds ");
        }
        log.info(logText.toString() + ms + " ms.");
    }

    private void deleteFile(File fileToDelete) throws BackupException {
        if (!fileToDelete.exists()) {
            throw new BackupException("File does not exists: " + fileToDelete.getAbsolutePath() + "\"");
        }
        if (fileToDelete.isDirectory()) {
            log.info("Found directory to delete: \"" + fileToDelete.getAbsolutePath() + "\". Deleting this directory...");
            File[] filesToDelete = fileToDelete.listFiles();
            for (int i = 0; i < filesToDelete.length; i++) {
                deleteFile(filesToDelete[i]);
            }
            if (!fileToDelete.delete()) {
                throw new BackupException("Cannot delete directory: \"" + fileToDelete.getAbsolutePath() + "\"");
            }
            statistics.setDeletedDirectories(statistics.getDeletedDirectories() + 1);
        } else {
            log.info("Found file to delete: \"" + fileToDelete.getAbsolutePath() + "\". Deleting this file...");
            long deletedFileSize = fileToDelete.length();
            if (!fileToDelete.delete()) {
                throw new BackupException("Cannot delete file: \"" + fileToDelete.getAbsolutePath() + "\"");
            }
            statistics.setTotalDeletedFilesSize(statistics.getTotalDeletedFilesSize() + deletedFileSize);
            statistics.setDeletedFiles(statistics.getDeletedFiles() + 1);
        }
    }

    public void addBackupListener(IBackupListener listener) {
        listeners.add(IBackupListener.class, listener);
    }

    public long calculateBackupInfo(List<String> backupDirsList) {
        long totalSize = 0;
        for (String backupDirPath : backupDirsList) {
            totalSize += FileUtils.sizeOfDirectory(new File(backupDirPath));
        }
        return totalSize;
    }
}
