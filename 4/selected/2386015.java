package net.sourceforge.processdash.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Incrementally backup the files in a directory.
 * 
 * ZIP files will be placed into the backup directory over time. The last file
 * will always be a full backup. Earlier files will be incremental backups,
 * indicating the files that changed from one backup to the next.
 */
public class IncrementalDirectoryBackup extends DirectoryBackup {

    private static final String LOG_FILE_NAME = "log.txt";

    private static final String HIST_LOG_FILE_NAME = "histLog.txt";

    private static final String OLD_BACKUP_TEMP_FILENAME = "temp_old.zip";

    private static final String NEW_BACKUP_TEMP_FILENAME = "temp_new.zip";

    /** The maximum amount of data to retain in the historical log */
    private int maxHistLogSize = 500000;

    /** A set of filename groupings that should always be backed up together */
    private String[][] atomicFileGroupings;

    public int getMaxHistLogSize() {
        return maxHistLogSize;
    }

    public void setMaxHistLogSize(int maxHistLogSize) {
        this.maxHistLogSize = maxHistLogSize;
    }

    public String[][] getAtomicFileGroupings() {
        return atomicFileGroupings;
    }

    public void setAtomicFileGroupings(String[][] atomicFileGroupings) {
        this.atomicFileGroupings = atomicFileGroupings;
    }

    @Override
    protected void doBackup(File destFile) throws IOException {
        try {
            backupFiles(destFile, false);
        } catch (Exception e1) {
            try {
                backupFiles(destFile, true);
                printError("Unexpected error in FileBackupManager; " + "ignoring most recent backup", e1);
            } catch (IOException e2) {
                printError(e2);
                throw e2;
            }
        }
    }

    private boolean oldBackupIsEmpty;

    private List atomicFilesInOldBackup;

    private void backupFiles(File destFile, boolean ignoreLastBackup) throws IOException {
        List dataFiles = getFilenamesToBackup();
        if (dataFiles == null || dataFiles.size() == 0) return;
        Collections.sort(dataFiles);
        ProfTimer pt = new ProfTimer(IncrementalDirectoryBackup.class, "IncrementalDirectoryBackup.backupFiles");
        File dataDir = srcDirectory;
        File backupDir = destDirectory;
        File[] backupFiles = getBackupFiles(backupDir);
        File mostRecentBackupFile = (ignoreLastBackup ? null : findMostRecentBackupFile(backupFiles));
        File oldBackupTempFile = new File(backupDir, OLD_BACKUP_TEMP_FILENAME);
        File newBackupTempFile = new File(backupDir, NEW_BACKUP_TEMP_FILENAME);
        ZipOutputStream newBackupOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(newBackupTempFile)));
        newBackupOut.setLevel(9);
        boolean wroteHistLog = false;
        if (mostRecentBackupFile != null) {
            ZipInputStream oldBackupIn = new ZipInputStream(new TimedInputStream(new BufferedInputStream(new FileInputStream(mostRecentBackupFile)), 60000));
            ZipOutputStream oldBackupOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(oldBackupTempFile)));
            oldBackupOut.setLevel(9);
            oldBackupIsEmpty = true;
            atomicFilesInOldBackup = new ArrayList();
            ZipEntry oldEntry;
            while ((oldEntry = oldBackupIn.getNextEntry()) != null) {
                String filename = oldEntry.getName();
                ThreadThrottler.tick();
                if (HIST_LOG_FILE_NAME.equals(filename)) {
                    long histLogModTime = oldEntry.getTime();
                    if (histLogModTime < 1) histLogModTime = mostRecentBackupFile.lastModified();
                    File logFile = new File(dataDir, LOG_FILE_NAME);
                    long currentLogModTime = logFile.lastModified();
                    if (currentLogModTime <= histLogModTime) copyZipEntry(oldBackupIn, newBackupOut, oldEntry, null); else writeHistLogFile(oldBackupIn, newBackupOut, dataDir);
                    wroteHistLog = true;
                    continue;
                }
                if (!fileFilter.accept(srcDirectory, filename)) continue;
                File file = new File(dataDir, filename);
                if (dataFiles.remove(filename)) {
                    backupFile(oldEntry, oldBackupIn, oldBackupOut, newBackupOut, file, filename);
                } else {
                    copyZipEntry(oldBackupIn, oldBackupOut, oldEntry, null);
                    wroteEntryToOldBackup(filename);
                }
            }
            addAtomicFilesToBackup(oldBackupOut);
            oldBackupIn.close();
            mostRecentBackupFile.delete();
            if (oldBackupIsEmpty) {
                oldBackupOut.putNextEntry(new ZipEntry("foo"));
                oldBackupOut.close();
                oldBackupTempFile.delete();
            } else {
                oldBackupOut.close();
                FileUtils.renameFile(oldBackupTempFile, mostRecentBackupFile);
            }
        }
        for (Iterator iter = dataFiles.iterator(); iter.hasNext(); ) {
            ThreadThrottler.tick();
            String filename = (String) iter.next();
            File file = new File(dataDir, filename);
            backupFile(null, null, null, newBackupOut, file, filename);
        }
        if (wroteHistLog == false) writeHistLogFile(null, newBackupOut, dataDir);
        pt.click("Backed up data files");
        if (extraContentSupplier != null) extraContentSupplier.addExtraContentToBackup(newBackupOut);
        pt.click("Backed up extra content");
        newBackupOut.close();
        FileUtils.renameFile(newBackupTempFile, destFile);
    }

    private File[] getBackupFiles(File backupDir) {
        File[] backupFiles = backupDir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                try {
                    backupFilenameFormat.parse(name);
                    return true;
                } catch (ParseException e) {
                    return false;
                }
            }
        });
        Arrays.sort(backupFiles);
        return backupFiles;
    }

    private File findMostRecentBackupFile(File[] backupFiles) {
        if (backupFiles != null && backupFiles.length > 0) return backupFiles[backupFiles.length - 1]; else return null;
    }

    private void copyZipEntry(InputStream oldBackupIn, ZipOutputStream oldBackupOut, ZipEntry e, byte[] prepend) throws IOException {
        ZipEntry eOut = new ZipEntry(e.getName());
        eOut.setTime(e.getTime());
        oldBackupOut.putNextEntry(eOut);
        if (prepend != null) oldBackupOut.write(prepend);
        int bytesRead;
        while ((bytesRead = oldBackupIn.read(copyBuf)) != -1) {
            oldBackupOut.write(copyBuf, 0, bytesRead);
        }
        oldBackupOut.closeEntry();
    }

    private byte[] copyBuf = new byte[1024];

    private void backupFile(ZipEntry oldEntry, ZipInputStream oldBackupIn, ZipOutputStream oldBackupOut, ZipOutputStream newBackupOut, File file, String filename) throws IOException {
        ByteArrayOutputStream bytesSeen = null;
        InputStream oldIn = null;
        if (oldEntry != null && oldBackupIn != null && oldBackupOut != null) {
            bytesSeen = new ByteArrayOutputStream();
            oldIn = oldBackupIn;
        }
        ZipEntry e = new ZipEntry(filename);
        e.setTime(file.lastModified());
        e.setSize(file.length());
        newBackupOut.putNextEntry(e);
        InputStream fileIn = new BufferedInputStream(new FileInputStream(file));
        OutputStream fileOut = newBackupOut;
        int c, d;
        try {
            while ((c = fileIn.read()) != -1) {
                fileOut.write(c);
                if (oldIn != null) {
                    d = oldIn.read();
                    if (d != -1) bytesSeen.write(d);
                    if (c != d) {
                        copyZipEntry(oldIn, oldBackupOut, oldEntry, bytesSeen.toByteArray());
                        oldIn = null;
                        bytesSeen = null;
                        oldBackupIn = null;
                        oldBackupOut = null;
                        wroteEntryToOldBackup(filename);
                    }
                }
                ThreadThrottler.tick();
            }
        } finally {
            fileIn.close();
        }
        if (oldIn != null) {
            d = oldIn.read();
            if (d != -1) {
                bytesSeen.write(d);
                copyZipEntry(oldIn, oldBackupOut, oldEntry, bytesSeen.toByteArray());
                wroteEntryToOldBackup(filename);
            }
        }
        fileOut.flush();
        newBackupOut.closeEntry();
    }

    private void wroteEntryToOldBackup(String filename) {
        oldBackupIsEmpty = false;
        if (atomicFileGroupings != null) {
            for (int g = 0; g < atomicFileGroupings.length; g++) {
                String[] group = atomicFileGroupings[g];
                for (int i = 0; i < group.length; i++) {
                    if (filename.equalsIgnoreCase(group[i])) {
                        atomicFilesInOldBackup.add(filename);
                        break;
                    }
                }
            }
        }
    }

    private void writeHistLogFile(ZipInputStream oldBackupIn, ZipOutputStream newBackupOut, File dataDir) throws IOException {
        File currentLog = new File(dataDir, LOG_FILE_NAME);
        if (oldBackupIn == null && !currentLog.exists()) return;
        ZipEntry e = new ZipEntry(HIST_LOG_FILE_NAME);
        e.setTime(System.currentTimeMillis());
        newBackupOut.putNextEntry(e);
        byte[] histLog = null;
        if (oldBackupIn != null) {
            histLog = FileUtils.slurpContents(oldBackupIn, false);
            long totalSize = histLog.length + currentLog.length();
            int skip = (int) Math.max(0, totalSize - maxHistLogSize);
            if (skip < histLog.length) newBackupOut.write(histLog, skip, histLog.length - skip); else histLog = null;
        }
        if (currentLog.exists() && currentLog.length() > 0) {
            InputStream currentLogIn = new BufferedInputStream(new FileInputStream(currentLog));
            try {
                byte[] currLogStart = new byte[100];
                int matchLen = currentLogIn.read(currLogStart);
                int lastLogEntryPos = findLastLogEntryStart(histLog);
                if (matches(histLog, lastLogEntryPos, currLogStart, 0, matchLen)) {
                    int duplicateLen = histLog.length - lastLogEntryPos;
                    int skip = duplicateLen - matchLen;
                    if (skip > 0) currentLogIn.skip(skip);
                } else {
                    newBackupOut.write(HIST_SEPARATOR.getBytes());
                    newBackupOut.write(currLogStart, 0, matchLen);
                }
                FileUtils.copyFile(currentLogIn, newBackupOut);
            } finally {
                currentLogIn.close();
            }
        }
        newBackupOut.closeEntry();
    }

    private int findLastLogEntryStart(byte[] histLog) {
        if (histLog == null || histLog.length == 0) return -1;
        int dashcount = 0;
        for (int pos = histLog.length; pos-- > 0; ) {
            if (histLog[pos] != '-') {
                dashcount = 0;
            } else if (++dashcount > 60) {
                int result = pos + dashcount;
                while (result < histLog.length) {
                    if (Character.isWhitespace(histLog[result])) result++; else return result;
                }
                return -1;
            }
        }
        return -1;
    }

    private boolean matches(byte[] a, int aPos, byte[] b, int bPos, int len) {
        if (a == null || b == null) return false;
        if (aPos < 0 || aPos + len > a.length) return false;
        if (bPos < 0 || bPos + len > b.length) return false;
        while (len-- > 0) {
            if (a[aPos++] != b[bPos++]) return false;
        }
        return true;
    }

    private static final String HIST_SEPARATOR = "--------------------" + "--------------------------------------------------" + System.getProperty("line.separator");

    private void addAtomicFilesToBackup(ZipOutputStream zipOut) throws IOException {
        if (atomicFileGroupings != null) {
            for (int g = 0; g < atomicFileGroupings.length; g++) {
                addAtomicFilesToBackup(zipOut, atomicFileGroupings[g]);
            }
        }
    }

    private void addAtomicFilesToBackup(ZipOutputStream zipOut, String[] filenames) throws IOException {
        List matchedFiles = new ArrayList();
        List unmatchedFiles = new ArrayList();
        for (int i = 0; i < filenames.length; i++) {
            String filename = filenames[i];
            if (containsIgnoreCase(atomicFilesInOldBackup, filename)) matchedFiles.add(filename); else unmatchedFiles.add(filename);
        }
        if (!matchedFiles.isEmpty() && !unmatchedFiles.isEmpty()) {
            for (Iterator i = unmatchedFiles.iterator(); i.hasNext(); ) {
                String filename = (String) i.next();
                File file = new File(srcDirectory, filename);
                backupFile(null, null, null, zipOut, file, filename);
            }
        }
    }

    private boolean containsIgnoreCase(List list, String str) {
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            String oneItem = (String) i.next();
            if (oneItem.equalsIgnoreCase(str)) return true;
        }
        return false;
    }

    private static void printError(Throwable t) {
        printError("Unexpected error in FileBackupManager", t);
    }

    private static void printError(String msg, Throwable t) {
        System.err.println(msg);
        t.printStackTrace();
    }
}
