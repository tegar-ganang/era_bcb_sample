package net.sourceforge.jnsynch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import net.sourceforge.jnsynch.event.SynchronizationEvent;
import net.sourceforge.jnsynch.event.SynchronizationEventType;
import net.sourceforge.jnsynch.event.SynchronizationListener;

class SynchronizationProcessor extends Thread {

    private SynchronizationContext context;

    private SynchronizationStatus status;

    private int filesToCopy = 0;

    private int filesToDelete = 0;

    public SynchronizationProcessor(SynchronizationContext context) {
        this.context = context;
        updateStatus(SynchronizationStatus.Initialized);
    }

    public SynchronizationStatus getStatus() {
        return status;
    }

    private void updateStatus(SynchronizationStatus status) {
        this.status = status;
        dispatchEvent(SynchronizationEventType.StatusUpdate);
    }

    private void updateStatus(String message) {
        dispatchEvent(message);
    }

    @Override
    public void run() {
        try {
            synchronize();
        } catch (Exception ex) {
            dispatchEvent(ex);
        }
    }

    private void synchronize() {
        updateStatus(SynchronizationStatus.CheckingTarget);
        File target = new File(context.getTarget());
        if (!(target.exists() == true && target.isDirectory() == true)) {
            dispatchEvent(new Exception("Target not available. Giving up now"));
            updateStatus(SynchronizationStatus.Failed);
            return;
        }
        updateStatus(SynchronizationStatus.ResolvingSource);
        ArrayList<FileCatalogItem> sourceFilesList = new ArrayList<FileCatalogItem>();
        sourceFilesList = resolveCatalog(sourceFilesList, context.getSource(), context.getSource());
        updateStatus(SynchronizationStatus.ResolvingTarget);
        ArrayList<FileCatalogItem> targetFilesList = new ArrayList<FileCatalogItem>();
        targetFilesList = resolveCatalog(targetFilesList, context.getTarget(), context.getTarget());
        updateStatus(SynchronizationStatus.ResolvingOperations);
        resolveFilesOperations(sourceFilesList, targetFilesList);
        updateStatus(SynchronizationStatus.Synchronizing);
        performFileOperations(sourceFilesList, targetFilesList);
        updateStatus(SynchronizationStatus.Finished);
    }

    private ArrayList<FileCatalogItem> resolveCatalog(ArrayList<FileCatalogItem> baseCatalog, String basePath, String rootPath) {
        String relativePath = basePath.substring(rootPath.length());
        File baseFolder = new File(basePath);
        File[] filesInFolder = baseFolder.listFiles();
        FileCatalogItem folderEntry = new FileCatalogItem(relativePath, baseFolder.lastModified(), FileOperationsEnum.None, FileEntryType.Folder);
        baseCatalog.add(folderEntry);
        for (int idx = 0; idx < filesInFolder.length; idx++) {
            beNice();
            File currentFile = filesInFolder[idx];
            if (context.isElegible(currentFile)) {
                if (currentFile.isFile()) {
                    relativePath = currentFile.getPath().substring(rootPath.length());
                    FileCatalogItem thisEntry = new FileCatalogItem(relativePath, currentFile.lastModified(), FileOperationsEnum.None, FileEntryType.File);
                    baseCatalog.add(thisEntry);
                } else {
                    resolveCatalog(baseCatalog, currentFile.getPath(), rootPath);
                }
            } else {
                dispatchEvent(SynchronizationEventType.FileIgnore, currentFile);
            }
        }
        return baseCatalog;
    }

    private ArrayList<FileCatalogItem> resolveFilesOperations(ArrayList<FileCatalogItem> sourceFilesList, ArrayList<FileCatalogItem> targetFilesList) {
        for (int idx = 1; idx < sourceFilesList.size(); idx++) {
            updateStatus("... processing file # " + String.valueOf(idx));
            beNice();
            FileCatalogItem sourceEntry = sourceFilesList.get(idx);
            int correspondingTargetIdx = findEntry(sourceEntry.RelativeFilePath, targetFilesList);
            if (correspondingTargetIdx != -1 & sourceEntry.EntryType == FileEntryType.File) {
                FileCatalogItem targetEntry = targetFilesList.get(correspondingTargetIdx);
                long lm1 = sourceEntry.LastModified;
                long lm2 = targetEntry.LastModified;
                long dif = Math.abs(lm1 - lm2);
                if (dif > 10000) {
                    sourceEntry.FileOperation = FileOperationsEnum.Copy;
                    filesToCopy++;
                } else {
                    targetFilesList.get(correspondingTargetIdx).FileOperation = FileOperationsEnum.None;
                }
            } else if (sourceEntry.EntryType == FileEntryType.File) {
                sourceEntry.FileOperation = FileOperationsEnum.Copy;
                filesToCopy++;
            } else if (sourceEntry.EntryType == FileEntryType.Folder) {
                sourceEntry.FileOperation = FileOperationsEnum.Copy;
            }
        }
        if (!context.isOptionSet(SynchronizationOption.DoNoDeleteFilesInTarget)) {
            for (int idx = 1; idx < targetFilesList.size(); idx++) {
                FileCatalogItem targetEntry = targetFilesList.get(idx);
                int correspondingSourceIdx = findEntry(targetEntry.RelativeFilePath, sourceFilesList);
                if (correspondingSourceIdx == -1) {
                    targetEntry.FileOperation = FileOperationsEnum.Delete;
                    filesToDelete++;
                }
            }
        }
        updateStatus("... Files to Copy: " + String.valueOf(filesToCopy));
        updateStatus("... Entries to Delete: " + String.valueOf(filesToDelete));
        context.setFilesToCopy(filesToCopy);
        return null;
    }

    private int findEntry(String fileToFindPath, ArrayList<FileCatalogItem> fileList) {
        String patternToFind = fileToFindPath;
        for (int idx = 1; idx < fileList.size(); idx++) {
            FileCatalogItem thisEntry = fileList.get(idx);
            String patternToCompare = thisEntry.RelativeFilePath;
            if (patternToFind.equals(patternToCompare)) {
                return idx;
            }
        }
        return -1;
    }

    public void performFileOperations(ArrayList<FileCatalogItem> sourceFilesList, ArrayList<FileCatalogItem> targetFilesList) {
        String targetBasePath = context.getTarget();
        if (context.getFilesToCopy() > 0) {
            for (int idx = 1; idx < sourceFilesList.size(); idx++) {
                FileCatalogItem thisEntry = sourceFilesList.get(idx);
                if (thisEntry.FileOperation == FileOperationsEnum.Copy) {
                    String sourceFilePath = context.getSource() + thisEntry.RelativeFilePath;
                    String entryRelativePath = thisEntry.RelativeFilePath;
                    String targetFilePath = targetBasePath + entryRelativePath;
                    File sourceFile = new File(sourceFilePath);
                    File targetFile = new File(targetFilePath);
                    if (thisEntry.EntryType == FileEntryType.File) {
                        if (context.isOptionSet(SynchronizationOption.BackUpFileAtTarget)) {
                        }
                        String sourcePath = sourceFile.getPath();
                        if (sourcePath.equalsIgnoreCase("D:\\My Files\\AMTResearch\\AMTSDK\\Windows\\Common\\ThirdParty\\apache\\include\\apr_optional_hooks.h")) {
                            System.out.println();
                        }
                        copyFile(sourceFile, targetFile);
                    } else {
                        try {
                            if (!targetFile.exists()) {
                                targetFile.mkdir();
                                targetFile.setLastModified(sourceFile.lastModified());
                            }
                        } catch (Exception ex) {
                            dispatchEvent(ex);
                        }
                    }
                }
            }
        }
        if (!context.isOptionSet(SynchronizationOption.DoNoDeleteFilesInTarget)) {
            for (int idx = 1; idx < targetFilesList.size(); idx++) {
                FileCatalogItem thisEntry = targetFilesList.get(idx);
                if (thisEntry.EntryType == FileEntryType.File) {
                    if (thisEntry.FileOperation == FileOperationsEnum.Delete) {
                        if (context.isOptionSet(SynchronizationOption.BackUpFileAtTarget)) {
                        }
                        File fl = new File(context.getTarget() + thisEntry.RelativeFilePath);
                        dispatchEvent(SynchronizationEventType.FileDelete, fl);
                        fl.deleteOnExit();
                        fl.delete();
                        dispatchEvent(SynchronizationEventType.FileDeleteDone, fl);
                    }
                } else {
                    if (thisEntry.FileOperation == FileOperationsEnum.Delete) {
                        File folder = new File(context.getTarget() + thisEntry.RelativeFilePath);
                        try {
                            dispatchEvent(SynchronizationEventType.FolderDelete, folder);
                            folder.delete();
                            dispatchEvent(SynchronizationEventType.FolderDeleteDone, folder);
                        } catch (Exception ex) {
                            dispatchEvent("Folder cannot be delted for now:" + folder.getName());
                        }
                    }
                }
            }
        }
    }

    private void copyFile(File sourceFile, File targetFile) {
        beNice();
        dispatchEvent(SynchronizationEventType.FileCopy, sourceFile, targetFile);
        File temporaryFile = new File(targetFile.getPath().concat(".jnstemp"));
        while (temporaryFile.exists()) {
            try {
                beNice();
                temporaryFile.delete();
                beNice();
            } catch (Exception ex) {
            }
        }
        try {
            if (targetFile.exists()) {
                targetFile.delete();
            }
            FileInputStream fis = new FileInputStream(sourceFile);
            FileOutputStream fos = new FileOutputStream(temporaryFile);
            byte[] buffer = new byte[204800];
            int readBytes = 0;
            int counter = 0;
            while ((readBytes = fis.read(buffer)) != -1) {
                counter++;
                updateStatus("... processing fragment " + String.valueOf(counter));
                fos.write(buffer, 0, readBytes);
            }
            fis.close();
            fos.close();
            temporaryFile.renameTo(targetFile);
            temporaryFile.setLastModified(sourceFile.lastModified());
            targetFile.setLastModified(sourceFile.lastModified());
        } catch (IOException e) {
            Exception dispatchedException = new Exception("ERROR: Copy File( " + sourceFile.getPath() + ", " + targetFile.getPath() + " )");
            dispatchEvent(dispatchedException, sourceFile, targetFile);
        }
        dispatchEvent(SynchronizationEventType.FileCopyDone, sourceFile, targetFile);
    }

    private void beNice() {
        try {
            Thread.yield();
            Thread.sleep(context.getNiceMilliSeconds());
        } catch (InterruptedException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void dispatchEvent(SynchronizationEventType type) {
        dispatchEvent(type, null);
    }

    private void dispatchEvent(String message) {
        synchronized (this) {
            for (SynchronizationListener listener : context.getListeners()) {
                listener.synchronizationEvent(message);
            }
        }
    }

    private void dispatchEvent(SynchronizationEventType type, File file) {
        dispatchEvent(type, file, null);
    }

    private void dispatchEvent(SynchronizationEventType type, File file, File targetFile) {
        SynchronizationHandler handler = new SynchronizationHandler(this);
        SynchronizationEvent event = new SynchronizationEvent(handler, type, file, targetFile);
        dispatchEvent(event);
    }

    private void dispatchEvent(Exception error) {
        dispatchEvent(error, null);
    }

    private void dispatchEvent(Exception error, File file) {
        dispatchEvent(error, file, null);
    }

    private void dispatchEvent(Exception error, File file, File targetFile) {
        SynchronizationHandler handler = new SynchronizationHandler(this);
        SynchronizationEvent event = new SynchronizationEvent(handler, error, file, targetFile);
        dispatchEvent(event);
    }

    private void dispatchEvent(SynchronizationEvent event) {
        synchronized (this) {
            for (SynchronizationListener listener : context.getListeners()) {
                listener.synchronizationEvent(event);
            }
        }
    }
}
