package com.sun.star.addon.sugarcrm.action;

import org.apache.log4j.Logger;
import com.sun.star.addon.sugarcrm.main.MainWindow;
import com.sun.star.addon.sugarcrm.soap.NoteInfo;
import com.sun.star.addon.sugarcrm.soap.SugarInfo;
import com.sun.star.addon.sugarcrm.soap.SugarItem;
import com.sun.star.addon.sugarcrm.util.SwingWorker;
import java.util.ArrayList;
import javax.swing.JOptionPane;

/**
 * Swing worker thread for archiving sugar Notes
 * @author othman
 */
public class ArchiveAction extends SwingWorker {

    private static final int NOTOALL_OPTION = 3;

    private static final int NO_OPTION = 2;

    private static final int PROMPT_INIT_VALUE = -2;

    private static final int YESTOALL_OPTION = 1;

    private static final int YES_OPTION = 0;

    private static Logger _logger = Logger.getLogger(ArchiveAction.class);

    private int archivedCount = 0;

    private int fileCt = 0;

    private int confirmOverwriteValue = PROMPT_INIT_VALUE;

    private boolean confirmOverwrite = true;

    private volatile Boolean cancelRequest = false;

    private int confirmStatus;

    private int itemsNum;

    /**
     * constructor
     * starts archive swing worker thread
     */
    public ArchiveAction() {
        MainWindow.getInstance().enableComponents(false);
        MainWindow.getInstance().getProgressBar().setProgressBarVisible(true);
        start();
    }

    @Override
    public Object construct() {
        MainWindow.getInstance().getProgressBar().setProgressBar("archiving Notes...");
        boolean archiveSuccessfull = archive();
        MainWindow.getInstance().getProgressBar().setProgressBarVisible(false);
        MainWindow.getInstance().enableComponents(true);
        if (archiveSuccessfull) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), archivedCount + " Out Of " + fileCt + " Note(s) archived successfully", "Archive Message", JOptionPane.INFORMATION_MESSAGE);
        }
        return Boolean.valueOf(archiveSuccessfull);
    }

    /**
     * prompt user to confir file overwrite
     * @param existingFile
     */
    private void confirmOverwrite(final NoteInfo existingNote, SugarItem item) {
        Object[] possibleValues = { "Yes", "Yes To All", "No", "No To All" };
        String message = "<html><b>Warning</b><hr>Sugar Note <b><i>'" + existingNote.getTitle() + "'</i></b><br> Already attached to following Sugar Item:<br>" + "<b><i>'" + item.getName() + "'</i></b><hr>" + " <br>Are you sure you want to overwrite ?<br></html>";
        int val = JOptionPane.showOptionDialog(MainWindow.getInstance(), message, "Confirm Overwrite", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, possibleValues, possibleValues[0]);
        confirmOverwriteValue = val;
    }

    /**
     * handles file overwrite options
     * @param existingFile
     * @return true if user confirm overwrite .false otherwize
     * @throws java.lang.Exception
     */
    private boolean handleOverwrite(final NoteInfo existingNote, SugarItem item) throws Exception {
        boolean status = false;
        if (confirmOverwrite) {
            confirmOverwrite(existingNote, item);
            while (confirmOverwriteValue == PROMPT_INIT_VALUE) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ignore) {
                }
            }
            confirmStatus = confirmOverwriteValue;
            confirmOverwriteValue = PROMPT_INIT_VALUE;
            if (confirmStatus == YES_OPTION) {
                status = true;
            } else if (confirmStatus == YESTOALL_OPTION) {
                confirmOverwrite = false;
                status = true;
            } else if ((confirmStatus == NO_OPTION) || (confirmStatus == JOptionPane.CLOSED_OPTION)) {
                status = false;
            } else {
                confirmOverwrite = false;
                status = false;
            }
        }
        return status;
    }

    /**
     * validates archiving of sugar notes list into sugar items list
     * @return true if notes archiving is successful
     */
    private boolean archive() {
        boolean hasErrors = false;
        String errorMessage = null;
        try {
            ArrayList<SugarItem> items = MainWindow.getInstance().getSugarTree().getSelectedItems();
            itemsNum = items.size();
            _logger.debug("target sugar items list=" + items);
            NoteInfo[] notes = MainWindow.getInstance().getSugarTable().getModel().getSelectedNotes();
            if ((items == null) || (notes == null) || (notes.length == 0)) {
                hasErrors = true;
                errorMessage = "No selected Documents!\nPlease select documents from table and try again";
            }
            if (!hasErrors) {
                archive(notes, items);
            }
        } catch (Exception e) {
            errorMessage = e.getMessage();
            _logger.warn("Exception in Archive:", e);
            hasErrors = true;
        }
        if (hasErrors) {
            MainWindow.getInstance().getProgressBar().setProgressBarVisible(false);
            JOptionPane.showMessageDialog(MainWindow.getInstance(), errorMessage, "Archive Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return !hasErrors;
    }

    /**
     * archives an array of sugar notes into a list of sugar items
     * @param xDocs
     * @param items
     * @return
     * @throws java.lang.Exception
     */
    private int archive(NoteInfo[] notes, ArrayList<SugarItem> items) throws Exception {
        fileCt = notes.length;
        int archiveCt = 0;
        for (int i = 0; i < notes.length; i++) {
            archiveCt += archive(notes[i], items);
        }
        return archiveCt;
    }

    /**
     * archives one sugar noteDesc into all items of a list of sugar items
     * @param note
     * @param items
     * @return
     * @throws java.lang.Exception
     */
    private int archive(NoteInfo note, ArrayList<SugarItem> items) throws Exception {
        int archiveCt = 0;
        for (SugarItem item : items) {
            archiveCt += archive(note, item);
        }
        if (archiveCt > 0) {
            ++archivedCount;
        }
        return archiveCt;
    }

    /**
     * archives sugar note into sugar item
     * @param note
     * @param item
     * @return
     * @throws java.lang.Exception
     */
    private int archive(NoteInfo note, SugarItem item) throws Exception {
        int archiveCt = 0;
        _logger.debug("Archiving Note :  " + note + " to sugar item " + item + " ..");
        String docid = SugarInfo.getInstance().findNoteByFileName(item, note.getAttachement().getFile().getName());
        if (docid != null) {
            _logger.debug("Note: " + note + " already archived to sugar item '" + item.getName() + "'");
            if (handleOverwrite(note, item)) {
                SugarInfo.getInstance().archiveNote(note, item, docid, 1);
                return ++archiveCt;
            } else if (confirmStatus == YESTOALL_OPTION) {
                SugarInfo.getInstance().archiveNote(note, item, docid, 1);
                return ++archiveCt;
            } else {
                return archiveCt;
            }
        } else {
            SugarInfo.getInstance().archiveNote(note, item, docid, 0);
            archiveCt++;
        }
        return archiveCt;
    }
}
