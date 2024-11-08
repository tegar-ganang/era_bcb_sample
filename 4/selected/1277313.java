package org.dbe.dfs.explorer;

import java.io.IOException;
import javax.swing.JOptionPane;
import org.dbe.dfs.DFSException;
import org.dbe.dfs.DFSFile;
import org.dbe.dfs.DFSInputStream;
import org.dbe.dfs.DFSOutputStream;

/**
 * Uploads a file or directory to the DSS file system, in a new Thread.
 * This class provides also GUI dialogs (non modal).
 *
 * @author Intel Ireland Ltd.
 * @version 1.0.0a1
 */
class FilePasteTask extends TransferTask {

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
     * @param clipboard selected file in the explorer.
     * @param transferBlockSize Size of the blocks of data to be
     *                          transferred (bytes).
     */
    protected FilePasteTask(final DFSExplorer explorerReference, final DFSFile currentDir, final DFSFile clipboard, final int transferBlockSize) {
        super(explorerReference, currentDir);
        blockSize = transferBlockSize;
        worker = new SwingWorker() {

            public Object construct() {
                try {
                    paste(clipboard, currentDir);
                } catch (DFSException ex) {
                    logger.debug(ex);
                }
                return null;
            }
        };
    }

    /**
     * Pastes a file or directory.
     *
     * @param from file or directory to be copied
     * @param to destination directory
     *
     * @throws DSSException if a DSS exception occured
     */
    private void paste(final DFSFile from, final DFSFile to) throws DFSException {
        long operationSize = 0;
        if (from.isDirectory()) {
            try {
                operationSize = from.directoryInfo().getNumBytes();
            } catch (Exception e) {
                logger.debug(e);
                JOptionPane.showMessageDialog(explorer, "Unable to download file", "Download Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            operationSize = from.length();
        }
        progressMonitor.setMaximum((int) operationSize);
        progressCounter = 1;
        try {
            if (from.isFile()) {
                pasteFile(from, new DFSFile("JFS", to + DFSFile.separator + from.getName()));
            } else {
                pasteDirectory(to, from);
            }
        } catch (DFSException e) {
            if (progressMonitor.isCanceled()) {
                JOptionPane.showMessageDialog(explorer, "Paste Cancelled", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(explorer, "Could not paste succesfully", "Transfer Failure", JOptionPane.ERROR_MESSAGE);
            }
        }
        explorer.refresh();
    }

    /**
     * Pastes a regular file.
     *
     * @param from the origin file
     * @param to the destination file
     * @throws DSSException if a DSS exception occured
     */
    private void pasteFile(final DFSFile from, final DFSFile to) throws DFSException {
        boolean skip = false;
        if (to == null || to.getName().equals("")) {
            JOptionPane.showMessageDialog(explorer, "Invalid File Name", "Error Saving File", JOptionPane.ERROR_MESSAGE);
        } else {
            if (to.exists()) {
                if (noToAll) {
                    skip = true;
                } else if (!yesToAll) {
                    String[] options = new String[] { "Yes", "No", "Yes to all", "No to all" };
                    Object selected = JOptionPane.showInputDialog(explorer, to.getPath() + " already exists. Overwrite?", "File exists", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
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
                progressMonitor.setNote(from.getName());
                boolean suceeded = false;
                int failed = 0;
                while (!suceeded && failed < 4 && !progressMonitor.isCanceled()) {
                    suceeded = copy(from, to);
                    if (!suceeded) {
                        failed++;
                    }
                }
                if (!suceeded) {
                    progressMonitor.setProgress(progressMonitor.getMaximum() + 1);
                    throw new DFSException();
                }
            } else if (skip) {
                progressMonitor.setNote("Skipping " + from.getName());
                progressCounter += from.length();
                progressMonitor.setProgress(progressCounter);
            }
        }
    }

    /**
     * Recursive function pastes a directory and its entire contents
     * (including any subdirectories).
     *
     * @param parent the parent directory of the directory being downloaded
     * @param child the directory being downloaded
     * @throws DSSException if a DSS exception occured
     */
    private void pasteDirectory(final DFSFile parent, final DFSFile child) throws DFSException {
        DFSFile[] contents = child.listFiles();
        DFSFile newDir = new DFSFile("JFS", parent.getPath() + DFSFile.separator + child.getName());
        newDir.mkdir();
        if (contents == null) {
            throw new DFSException();
        }
        if (contents.length > 0) {
            for (int i = 0; i < contents.length && !cancelled; i++) {
                if (contents[i].isDirectory()) {
                    pasteDirectory(newDir, contents[i]);
                } else {
                    DFSFile newfile = new DFSFile("JFS", newDir.getPath() + DFSFile.separator + contents[i].getName());
                    try {
                        pasteFile(contents[i], newfile);
                    } catch (DFSException e) {
                        throw new DFSException();
                    }
                }
            }
        }
    }

    /**
     * Deals with the physical transfer of the file from
     * the DSS to the local storage system.
     *
     * @param from the path of the origin file
     * @param to the path of the destination file
     * @return true if the transfer was succesful, false otherwise
     */
    private boolean copy(final DFSFile from, final DFSFile to) {
        DFSInputStream dssis = null;
        DFSOutputStream dssos = null;
        long totalWritten = 0;
        try {
            byte[] readArray = new byte[blockSize];
            int rdSize = 0;
            dssis = new DFSInputStream(from);
            dssos = new DFSOutputStream(to);
            while (rdSize != -1 && !progressMonitor.isCanceled()) {
                rdSize = dssis.read(readArray);
                if (rdSize > 0) {
                    dssos.write(readArray, 0, rdSize);
                    totalWritten += rdSize;
                    progressCounter += rdSize;
                    progressMonitor.setProgress(progressCounter);
                }
            }
            explorer.closeStreams(dssis, dssos);
            if (progressMonitor.isCanceled()) {
                to.delete();
                return false;
            }
            return true;
        } catch (IOException e) {
            logger.debug(e);
            try {
                to.delete();
            } catch (DFSException ex) {
                logger.error(e);
            }
            progressCounter -= totalWritten;
            explorer.closeStreams(dssis, dssos);
            return false;
        }
    }
}
