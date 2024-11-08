package com.tmo;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.nio.channels.*;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

class SourceDestPair {

    File srcFile;

    File destFile;

    public SourceDestPair(File srcFile, File destFile) {
        this.srcFile = srcFile;
        this.destFile = destFile;
    }
}

public class CopyFileUtil implements Runnable {

    private ArrayList<SourceDestPair> fileList;

    private long totalSize;

    private long removedFilesSize;

    private long copiedSize;

    private JProgressBar progressBar;

    final long BLOCK_LENGTH = 1024 * 1024 * 16;

    private boolean threadStopped;

    public CopyFileUtil() {
        fileList = new ArrayList<SourceDestPair>();
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        totalSize = 0;
        removedFilesSize = 0;
        copiedSize = 0;
        threadStopped = true;
    }

    public void run() {
        threadStopped = false;
        Thread updateThread = new Thread(new Runnable() {

            public void run() {
                int noIterationPerSec = 2;
                int totalNumberOfIterations = 0;
                progressBar.setIndeterminate(false);
                while (copiedSize < totalSize) {
                    try {
                        long divfactor = totalSize / 100;
                        progressBar.setMaximum((int) (totalSize / divfactor));
                        progressBar.setValue((int) (copiedSize / divfactor));
                        totalNumberOfIterations++;
                        progressBar.setString("Copied " + getFileSizeString(copiedSize) + " of total " + getFileSizeString(totalSize) + "(Speed" + getFileSizeString(copiedSize * noIterationPerSec / totalNumberOfIterations) + "/sec)");
                        Logger.log("updateThread: copiedSize = " + copiedSize + " currentCopiedSize = " + copiedSize);
                        Thread.sleep(1000 / noIterationPerSec);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                int secs = totalNumberOfIterations / noIterationPerSec;
                int hrs = secs / 3600;
                secs = secs % 3600;
                Logger.log("hrs=" + hrs + " remaining seconds=" + secs);
                int mins = secs / 60;
                secs = secs % 60;
                Logger.log("mins=" + mins + " remaining seconds=" + secs);
                progressBar.setString("Copying completed - time taken " + hrs + "h:" + mins + "m:" + secs + "s");
                Logger.log("Copying completed - time taken " + (totalNumberOfIterations / noIterationPerSec) + "seconds taken ( " + totalNumberOfIterations + "/" + noIterationPerSec + ") " + +hrs + "h:" + mins + "m:" + secs + "s");
                progressBar.setMaximum(100);
                progressBar.setValue(100);
            }
        });
        updateThread.start();
        try {
            while (!fileList.isEmpty()) {
                SourceDestPair sourceDestPair;
                synchronized (this) {
                    sourceDestPair = fileList.remove(0);
                    removedFilesSize += getSize(sourceDestPair.srcFile);
                }
                Logger.log("copying " + sourceDestPair.srcFile + " to " + sourceDestPair.destFile);
                copyfile(sourceDestPair.srcFile, sourceDestPair.destFile);
                Logger.log("copying " + sourceDestPair.srcFile + " to " + sourceDestPair.destFile + "completed");
            }
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, "File not found." + e);
            e.printStackTrace();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "IO Error." + e);
            e.printStackTrace();
        } finally {
            synchronized (this) {
                threadStopped = true;
                removedFilesSize = 0;
                totalSize = 0;
                copiedSize = 0;
            }
        }
    }

    private void copyfile(File srcFile, File dstDir) throws FileNotFoundException, IOException {
        if (srcFile.isDirectory()) {
            File newDestDir = new File(dstDir, srcFile.getName());
            newDestDir.mkdir();
            String fileNameList[] = srcFile.list();
            for (int i = 0; i < fileNameList.length; i++) {
                File newSouceFile = new File(srcFile, fileNameList[i]);
                copyfile(newSouceFile, newDestDir);
            }
        } else {
            File newDestFile = new File(dstDir, srcFile.getName());
            FileInputStream in = new FileInputStream(srcFile);
            FileOutputStream out = new FileOutputStream(newDestFile);
            FileChannel inChannel = in.getChannel();
            FileChannel outChannel = out.getChannel();
            long i;
            Logger.log("copyFile before- copiedSize = " + copiedSize);
            for (i = 0; i < srcFile.length() - BLOCK_LENGTH; i += BLOCK_LENGTH) {
                synchronized (this) {
                    inChannel.transferTo(i, BLOCK_LENGTH, outChannel);
                    copiedSize += BLOCK_LENGTH;
                }
            }
            synchronized (this) {
                inChannel.transferTo(i, srcFile.length() - i, outChannel);
                copiedSize += srcFile.length() - i;
            }
            Logger.log("copyFile after copy- copiedSize = " + copiedSize + "srcFile.length = " + srcFile.length() + "diff = " + (copiedSize - srcFile.length()));
            in.close();
            out.close();
            outChannel = null;
            Logger.log("File copied.");
        }
    }

    public synchronized void addPair(File srcFile, File destDir) {
        SourceDestPair sourcDestPair = new SourceDestPair(srcFile, destDir);
        fileList.add(sourcDestPair);
    }

    public synchronized void updateTotalSize() {
        progressBar.setIndeterminate(true);
        Logger.log("Updating total size...");
        progressBar.setString("Updating total size...");
        Iterator<SourceDestPair> iterator = fileList.iterator();
        totalSize = removedFilesSize;
        while (iterator.hasNext()) {
            totalSize += getSize(iterator.next().srcFile);
        }
        Logger.log(" total size... " + totalSize + " bytes");
        String displayString = getFileSizeString(totalSize);
        progressBar.setString(" total size..." + displayString);
        progressBar.setIndeterminate(false);
    }

    private String getFileSizeString(long size) {
        float fileSize = size;
        String str = " Bytes";
        if (fileSize > 2 * 1024) {
            fileSize /= 1024;
            str = " KB";
        }
        if (fileSize > 2 * 1024) {
            fileSize /= 1024;
            str = " MB";
        }
        if (fileSize > 2 * 1024) {
            fileSize /= 1024;
            str = " GB";
        }
        return fileSize + str;
    }

    long getSize(File file) {
        long fileSize = 0;
        if (file.isDirectory()) {
            String fileNameList[] = file.list();
            for (int i = 0; i < fileNameList.length; i++) {
                fileSize += getSize(new File(file, fileNameList[i]));
            }
        } else {
            fileSize += file.length();
        }
        return fileSize;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public boolean isThreadStopped() {
        return threadStopped;
    }

    public int getNumberOfPairs() {
        return fileList.size();
    }
}
