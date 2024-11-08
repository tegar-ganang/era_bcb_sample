package org.dbe.dfs.explorer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.dbe.dfs.DFSException;
import org.dbe.dfs.DFSFile;
import org.dbe.dfs.DFSFileInfo;
import org.dbe.dfs.DFSInputStream;

/**
 * Uploads a file or directory to the DSS file system, in a new Thread.
 * This class provides also GUI dialogs (non modal).
 *
 * @author Intel Ireland Ltd.
 * @version 1.0.0a1
 */
class FileDownloadTask extends TransferTask {

    /**
     * Size of the blocks of data to be transferred (bytes).
     */
    private int blockSize;

    /**
     * True if user selected "Yes to All"
     */
    private boolean yesToAll = false;

    /**
     * True if user selected "No to All"
     */
    private boolean noToAll = false;

    /**
     * True if user selected "Cancel" in a "overwrite?" dialog
     */
    private boolean cancelled = false;

    /**
     * Creates an instance of this class.
     *
     * @param explorerReference the reference of the parent explorer object.
     * @param currentDir current directory in the explorer.
     * @param selectedFile selected file in the explorer.
     * @param transferBlockSize Size of the blocks of data to be
     *                          transferred (bytes).
     */
    protected FileDownloadTask(final DFSExplorer explorerReference, final DFSFile currentDir, final DFSFileInfo selectedFile, final int transferBlockSize) {
        super(explorerReference, currentDir, selectedFile);
        blockSize = transferBlockSize;
        worker = new SwingWorker() {

            public Object construct() {
                try {
                    download(new DFSFile("JFS", selected.getPath()));
                } catch (DFSException ex) {
                    logger.debug(ex);
                }
                return null;
            }
        };
    }

    /**
     * Downloads a file from the distributed storage system to the local
     * storage system. A file chooser dialog is used to select the
     * location the file will be saved to.
     *
     * @param downloadFile the file or directory being downloaded
     * @throws DFSException if a DFS exception occured
     */
    private void download(final DFSFile downloadFile) throws DFSException {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Download To");
        int result = fileChooser.showDialog(explorer, "Download");
        if (result == JFileChooser.CANCEL_OPTION) {
            return;
        }
        long operationSize = 0;
        if (downloadFile.isDirectory()) {
            try {
                operationSize = downloadFile.directoryInfo().getNumBytes();
            } catch (Exception e) {
                logger.debug(e);
                JOptionPane.showMessageDialog(explorer, "Unable to download file", "Download Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            operationSize = downloadFile.length();
        }
        progressMonitor.setMaximum((int) operationSize);
        progressCounter = 1;
        try {
            if (downloadFile.isFile()) {
                File localFile = new File(fileChooser.getSelectedFile().getPath() + DFSFile.separator + downloadFile.getName());
                downloadFile(localFile, downloadFile);
            } else {
                File parent = fileChooser.getSelectedFile();
                downloadDirectory(parent, downloadFile);
            }
        } catch (DFSException e) {
            if (progressMonitor.isCanceled()) {
                JOptionPane.showMessageDialog(explorer, "Download Cancelled", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(explorer, "Could not download succesfully", "Transfer Failure", JOptionPane.ERROR_MESSAGE);
            }
        }
        explorer.refresh();
    }

    /**
     * Downloads the relevant file to the local storage system.
     * The download is cancelled if the file concerned appears
     * to be corrupted.
     *
     * @param lFile the location the file is being written to
     * @param dFile the file being downloaded
     * @throws DFSException if a DFS exception occured
     */
    private void downloadFile(final File lFile, final DFSFile dFile) throws DFSException {
        boolean skip = false;
        if (lFile == null || lFile.getName().equals("")) {
            JOptionPane.showMessageDialog(explorer, "Invalid File Name", "Error Saving File", JOptionPane.ERROR_MESSAGE);
        } else {
            if (lFile.exists()) {
                if (noToAll) {
                    skip = true;
                } else if (!yesToAll) {
                    String[] options = new String[] { "Yes", "No", "Yes to all", "No to all" };
                    Object selected = JOptionPane.showInputDialog(explorer, lFile.getPath() + " already exists. Overwrite?", "File exists", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    if (selected == null) {
                        skip = true;
                        cancelled = true;
                        progressCounter = progressMonitor.getMaximum() + 1;
                        progressMonitor.setProgress(progressCounter);
                    } else if (selected == options[1]) {
                        skip = true;
                    } else if (selected == options[2]) {
                        yesToAll = true;
                    } else if (selected == options[3]) {
                        noToAll = true;
                        skip = true;
                    }
                }
            }
            if (!skip && !cancelled) {
                progressMonitor.setNote(dFile.getName());
                boolean suceeded = false;
                int failed = 0;
                while (!suceeded && failed < 4 && !progressMonitor.isCanceled()) {
                    suceeded = transferDown(lFile, dFile);
                    if (!suceeded) {
                        failed++;
                    }
                }
                if (!suceeded) {
                    progressMonitor.setProgress(progressMonitor.getMaximum() + 1);
                    throw new DFSException();
                }
            } else if (skip) {
                progressMonitor.setNote("Skipping " + dFile.getName());
                progressCounter += dFile.length();
                progressMonitor.setProgress(progressCounter);
            }
        }
    }

    /**
     * Recursive function downloads a directory and its entire contents
     * (including any subdirectories) from the Distributed Storage
     * System to the local file system.
     *
     * @param parent the parent directory of the directory being downloaded
     * @param child the directory being downloaded
     * @throws DFSException if a DFS exception occured
     */
    private void downloadDirectory(final File parent, final DFSFile child) throws DFSException {
        File newDir = new File(parent.getPath() + DFSFile.separator + child.getName());
        newDir.mkdir();
        DFSFile[] contents = child.listFiles();
        if (contents.length > 0) {
            for (int i = 0; i < contents.length && !cancelled; i++) {
                if (contents[i].isDirectory()) {
                    downloadDirectory(newDir, contents[i]);
                } else {
                    File newfile = new File(newDir.getPath() + DFSFile.separator + contents[i].getName());
                    try {
                        downloadFile(newfile, contents[i]);
                    } catch (DFSException e) {
                        throw new DFSException();
                    }
                }
            }
        }
    }

    /**
     * Deals with the physical transfer of the file from
     * the DFS to the local storage system.
     *
     * @param lFile the path of the file on the local storage system
     * @param dFile the path of the file on the distributed storage system
     * @return true if the transfer was succesful, false otherwise
     */
    private boolean transferDown(final File lFile, final DFSFile dFile) {
        DFSInputStream dssis = null;
        FileOutputStream los = null;
        long totalWritten = 0;
        try {
            byte[] readArray = new byte[blockSize];
            int rdSize = 0;
            dssis = new DFSInputStream(dFile);
            los = new FileOutputStream(lFile);
            while (rdSize != -1 && !progressMonitor.isCanceled()) {
                rdSize = dssis.read(readArray);
                if (rdSize > 0) {
                    los.write(readArray, 0, rdSize);
                    totalWritten += rdSize;
                    progressCounter += rdSize;
                    progressMonitor.setProgress(progressCounter);
                }
            }
            explorer.closeStreams(dssis, los);
            if (progressMonitor.isCanceled()) {
                lFile.delete();
                return false;
            }
            return true;
        } catch (IOException e) {
            logger.debug(e);
            lFile.delete();
            progressCounter -= totalWritten;
            explorer.closeStreams(dssis, los);
            return false;
        }
    }
}
