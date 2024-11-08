package org.dbe.dfs.explorer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.dbe.dfs.DFSException;
import org.dbe.dfs.DFSFile;
import org.dbe.dfs.DFSFileInfo;
import org.dbe.dfs.DFSOutputStream;

/**
 * Uploads a file or directory to the DSS file system, in a new Thread.
 * This class provides also GUI dialogs (non modal)
 *
 * @author Intel Ireland Ltd.
 * @version 1.0.0a1
 */
class FileUploadTask extends TransferTask {

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
    protected FileUploadTask(final DFSExplorer explorerReference, final DFSFile currentDir, final DFSFileInfo selectedFile, final int transferBlockSize) {
        super(explorerReference, currentDir, selectedFile);
        blockSize = transferBlockSize;
        worker = new SwingWorker() {

            public Object construct() {
                try {
                    upload();
                } catch (DFSException ex) {
                    logger.debug(ex);
                }
                return null;
            }
        };
    }

    /**
     * Uploads a file from the local file system to the distributed storage
     * system. A file chooser dialog is used to select the file or directory
     * to be uploaded.
     *
     * @throws DFSException if a DFS exception occured
     */
    private void upload() throws DFSException {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setDialogTitle("Upload From");
        int result = fileChooser.showDialog(explorer, "Upload");
        if (result == JFileChooser.CANCEL_OPTION) {
            return;
        }
        File lFile = fileChooser.getSelectedFile();
        progressMonitor.setMaximum((int) explorer.getOperationSize(lFile));
        try {
            if (lFile.isFile()) {
                DFSFile uploadFile = new DFSFile("JFS", currentDirectory.getPath() + DFSFile.separator + lFile.getName());
                if (uploadFile.getName().equals("")) {
                    JOptionPane.showMessageDialog(explorer, "Invalid Name", "Error Uploading File", JOptionPane.ERROR_MESSAGE);
                } else {
                    uploadFile(lFile, uploadFile);
                }
            } else {
                uploadDirectory(currentDirectory, lFile);
            }
        } catch (Exception e) {
            if (progressMonitor.isCanceled()) {
                JOptionPane.showMessageDialog(explorer, "Upload Cancelled", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(explorer, "Could not upload succesfully: " + lFile, "Transfer Failure", JOptionPane.ERROR_MESSAGE);
            }
        }
        explorer.refresh();
    }

    /**
     * Uploads the relevant file from the local storage system to
     * the distributed storage system. The upload is cancelled if
     * the file apppears to be corrupted.
     *
     * @param localFile the file being uploaded
     * @param upFile the location the file will be stored in the DFS
     * @throws DFSException if a DFS exception occured
     */
    private void uploadFile(final File localFile, final DFSFile upFile) throws DFSException {
        boolean skip = false;
        if (upFile.exists()) {
            if (noToAll) {
                skip = true;
            } else if (!yesToAll) {
                String[] options = new String[] { "Yes", "No", "Yes to all", "No to all" };
                Object selected = JOptionPane.showInputDialog(explorer, upFile.getPath() + " already exists. Overwrite?", "File exists", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
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
            progressMonitor.setNote(localFile.getName());
            boolean suceeded = false;
            int failed = 0;
            while (!suceeded && failed < 4 && !progressMonitor.isCanceled()) {
                suceeded = transferUp(localFile, upFile);
                if (!suceeded) {
                    failed++;
                }
            }
            if (!suceeded) {
                progressMonitor.setProgress(progressMonitor.getMaximum() + 1);
                throw new DFSException();
            }
        } else if (skip) {
            progressMonitor.setNote("Skipping " + localFile.getName());
            progressCounter += localFile.length();
            progressMonitor.setProgress(progressCounter);
        }
    }

    /**
     * Recursive function uploads a directory and its entire contents
     * (including any subdirectories) from the local file system to the
     * Distributed Storage System.
     *
     * @param parent the parent directory of the directory being uploaded
     * @param child the directory being uploaded
     * @throws DFSException if a DFS exception occured
     */
    private void uploadDirectory(final DFSFile parent, final File child) throws DFSException {
        DFSFile newDir = new DFSFile("JFS", parent.getPath() + DFSFile.separator + child.getName());
        newDir.mkdir();
        File[] contents = child.listFiles();
        if (contents.length > 0) {
            for (int i = 0; i < contents.length && !cancelled; i++) {
                if (contents[i].isDirectory()) {
                    uploadDirectory(newDir, contents[i]);
                } else {
                    DFSFile newFile = new DFSFile("JFS", newDir.getPath() + DFSFile.separator + contents[i].getName());
                    uploadFile(contents[i], newFile);
                }
            }
        }
    }

    /**
     * Deals with the physical transfer of the file from the local
     * storage system to the distributed storage system.
     *
     * @param lFile the path of the file on the local storage system
     * @param uFile the path of the file on the distributed storage system
     * @return true if the transfer was succesful, otherwise false
     */
    private boolean transferUp(final File lFile, final DFSFile uFile) {
        FileInputStream lis = null;
        DFSOutputStream dssos = null;
        long totalWritten = 0;
        try {
            byte[] readArray = new byte[blockSize];
            int rdSize = 0;
            lis = new FileInputStream(lFile);
            dssos = new DFSOutputStream(uFile);
            while (rdSize != -1 && !progressMonitor.isCanceled()) {
                rdSize = lis.read(readArray);
                if (rdSize > 0) {
                    dssos.write(readArray, 0, rdSize);
                    totalWritten += rdSize;
                    progressCounter += rdSize;
                    progressMonitor.setProgress(progressCounter);
                }
            }
            explorer.closeStreams(lis, dssos);
            if (progressMonitor.isCanceled()) {
                uFile.delete();
                return false;
            }
            return true;
        } catch (IOException e) {
            logger.debug(e);
            try {
                uFile.delete();
                progressCounter -= totalWritten;
            } catch (DFSException ex) {
                logger.debug(ex);
            }
            explorer.closeStreams(lis, dssos);
            return false;
        }
    }
}
