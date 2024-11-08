package org.qtitools.validatr.action.file;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.qtitools.validatr.MainFrame;
import org.qtitools.validatr.action.AbstractValidatorAction;
import org.qtitools.validatr.model.AssessmentDocument;
import org.qtitools.validatr.model.ValidatorModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parent for save/close/exit actions.
 * <p>
 * Contains shared logic.
 */
public abstract class FileAction extends AbstractValidatorAction {

    /** Logger. */
    protected Logger logger = LoggerFactory.getLogger(FileAction.class);

    /** Model of application. */
    private ValidatorModel model;

    /** Main frame of application. */
    private MainFrame mainFrame;

    /** File chooser of application. */
    private JFileChooser fileChooser;

    /**
	 * Constructs action.
	 *
	 * @param model model of application
	 * @param mainFrame main frame of application
	 * @param fileChooser file chooser of application
	 */
    public FileAction(ValidatorModel model, MainFrame mainFrame, JFileChooser fileChooser) {
        this.model = model;
        this.mainFrame = mainFrame;
        this.fileChooser = fileChooser;
    }

    /**
	 * Gets model of application.
	 *
	 * @return model of application
	 */
    public ValidatorModel getModel() {
        return model;
    }

    /**
	 * Gets main frame of application.
	 *
	 * @return main frame of application
	 */
    public MainFrame getMainFrame() {
        return mainFrame;
    }

    /**
	 * Gets file chooser of application.
	 *
	 * @return file chooser of application
	 */
    public JFileChooser getFileChooser() {
        return fileChooser;
    }

    /**
	 * Returns true if it is possible to close all documents; false otherwise.
	 * <p>
	 * Tests if any of document is modified and asks user for saving changes.
	 *
	 * @param source source of this action
	 * @return true if it is possible to close all documents; false otherwise
	 */
    protected boolean canCloseAll(Object source) {
        for (int i = 0; i < model.size(); i++) {
            AssessmentDocument document = model.get(i);
            if (!canClose(source, document)) return false;
        }
        return true;
    }

    /**
	 * Returns true if it is possible to close given document; false otherwise.
	 * <p>
	 * Tests if given documents is modified and asks user for saving changes.
	 *
	 * @param source source of this action
	 * @param document document to be closed
	 * @return true if it is possible to close given document; false otherwise
	 */
    protected boolean canClose(Object source, AssessmentDocument document) {
        if (document.isModified()) {
            String root = document.getRoot().getSimpleName();
            int returnValue = JOptionPane.showConfirmDialog(mainFrame, "Current " + root + " is modified. Save?", "Save", JOptionPane.YES_NO_CANCEL_OPTION);
            if (returnValue == JOptionPane.CANCEL_OPTION) return false;
            if (returnValue == JOptionPane.YES_OPTION) return save(source, document);
        }
        return true;
    }

    /**
	 * Saves given document into file.
	 *
	 * @param source source of this action
	 * @param document document to be saved
	 * @return true if given document was successfully saved; false otherwise
	 */
    protected boolean save(Object source, AssessmentDocument document) {
        File file = document.getRoot().getSourceFile();
        if (file == null) return saveAs(source, document); else return save(source, document, file);
    }

    /**
	 * Saves given document into file.
	 * <p>
	 * Enables to choose new file name also.
	 *
	 * @param source source of this action
	 * @param document document to be saved
	 * @return true if given document was successfully saved; false otherwise
	 */
    protected boolean saveAs(Object source, AssessmentDocument document) {
        File file = document.getRoot().getSourceFile();
        if (file != null) fileChooser.setSelectedFile(file); else fileChooser.setSelectedFile(new File(""));
        if (fileChooser.showSaveDialog(mainFrame) != JFileChooser.APPROVE_OPTION) return false;
        file = fileChooser.getSelectedFile();
        if (file == null) return false;
        if (file.exists() && !file.equals(document.getRoot().getSourceFile())) {
            Object[] options = new Object[] { "Yes", "No" };
            Object initialOption = "No";
            int returnValue = JOptionPane.showOptionDialog(mainFrame, "File already exists. Overwrite?", "Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, initialOption);
            if (returnValue != JOptionPane.YES_OPTION) return false;
        }
        return save(source, document, file);
    }

    /**
	 * Saves given document into given file.
	 *
	 * @param source source of this action.
	 * @param document document to be saved
	 * @param file where to save given document
	 * @return true if given document was successfully saved; false otherwise
	 */
    private boolean save(Object source, AssessmentDocument document, File file) {
        try {
            document.save(source, file);
            return true;
        } catch (Throwable ex) {
            String message = ex.getMessage();
            if (message == null || message.length() == 0) message = "Error while saving: " + file.getPath();
            logger.error(message, ex);
            JOptionPane.showMessageDialog(mainFrame, message, "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
