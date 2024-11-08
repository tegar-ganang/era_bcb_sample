package com.explosion.expf.supportmodules.editorsupport.edit;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.explosion.expf.Application;
import com.explosion.expf.ExpActionListener;
import com.explosion.expf.ExpConstants;
import com.explosion.expf.ExpFrame;
import com.explosion.utilities.exception.ExceptionManagerFactory;

/**
 * Thsi class forms a base class for any component that deals with loading saving and editing files
 * @author Stephen Cowx
 */
public abstract class FileEditorBase extends JPanel implements Editor, DocumentListener {

    private static final long serialVersionUID = 1;

    private boolean dirty = false;

    private Component parentLocalComponent;

    private FileBasedDocument document;

    private boolean modificationDecisionAlreadyMade = false;

    long dateLastEdited = 0;

    long originalSize = 0;

    private FileEditorLocalListener localListener = null;

    private FileEditorGlobalListener globalListener = null;

    /**
     * Initialises a new empty text editor
     */
    public FileEditorBase(int documentNumber, String extension, Component parentLocalComponent) throws Exception {
        document = new FileBasedDocument(documentNumber, extension);
        if (parentLocalComponent != null) this.parentLocalComponent = parentLocalComponent; else this.parentLocalComponent = this;
        this.setName("TEXT_EDITOR");
        this.parentLocalComponent = this;
        init();
    }

    public abstract void applyPreferences();

    public abstract void write(File file) throws IOException;

    public abstract void load(File file) throws IOException;

    public abstract void load() throws IOException;

    /**
     * This method initialises the gui
     */
    private void init() throws Exception {
        Application.addToGlobalCookie(ExpConstants.MENU_CLOSEALL, 1);
        Application.addToLocalCookie(ExpConstants.MENU_EDIT, 1, this);
        Application.addToLocalCookie(ExpConstants.MENU_CLOSE, 1, this);
        Application.addToLocalCookie(ExpConstants.MENU_SAVEAS, 1, this);
        Application.addToLocalCookie(ExpConstants.MENU_SELECTALL, 1, this);
        localListener = new FileEditorLocalListener(this, parentLocalComponent);
        if (parentLocalComponent == this) {
            globalListener = new FileEditorGlobalListener(this);
        }
        FileChangeListener.monitor(this);
    }

    /**
     * Returns the document associated with this editor
     * @return
     */
    public FileBasedDocument getDocument() {
        return document;
    }

    /**
    * Returns a name for this document e.g. untitled1.txt or MyDoc.txt
    * This is used to display titles etc for the document
    * @return
    */
    public String getDocumentName() {
        return document.getDocumentName();
    }

    /**
     * Sets the document to be associated with this editor
     * @param document
     */
    public void setDocument(FileBasedDocument document) {
        this.document = document;
    }

    /**
     * Returns the file associated with this editor
     * @return
     */
    public File getFile() {
        return document.getFile();
    }

    /**
     * Returns the string command which will make this editor close itself
     * @see com.explosion.expf.Closeable#getCloseCommand()
     * @return
     */
    public String getCloseCommand() {
        return ExpConstants.MENU_CLOSE;
    }

    /**
     * Code to support document Listener
     */
    public void changedUpdate(DocumentEvent e) {
        if (!dirty) dirty();
    }

    /**
     * Code to support document Listener
     */
    public void insertUpdate(DocumentEvent e) {
        if (!dirty) dirty();
    }

    /**
     * Code to support document Listener
     */
    public void removeUpdate(DocumentEvent e) {
        if (!dirty) dirty();
    }

    /**
     * Returns a boolean value indicating whether this document has changed since it was last saved
     */
    public boolean isDirty() throws Exception {
        return dirty;
    }

    private void dirty() {
        dirty = true;
        try {
            JInternalFrame frame = ((ExpFrame) Application.getApplicationFrame()).getFrameWithComponent(this, ExpFrame.DOC_FRAME_LAYER.intValue());
            frame.setTitle(document.getDocumentName() + "*");
        } catch (Exception e) {
        }
        Application.addToGlobalCookie(ExpConstants.MENU_SAVEALL, 1);
        Application.addToLocalCookie(ExpConstants.MENU_SAVE, 1, parentLocalComponent);
    }

    /**
     * Cleans this up a little
     *
     */
    protected void clean() {
        dirty = false;
        try {
            JInternalFrame frame = ((ExpFrame) Application.getApplicationFrame()).getFrameWithComponent(this, ExpFrame.DOC_FRAME_LAYER.intValue());
            frame.setTitle(document.getDocumentName());
        } catch (Exception e) {
        }
        Application.removeFromGlobalCookie(ExpConstants.MENU_SAVEALL, 1);
        Application.removeFromLocalCookie(ExpConstants.MENU_SAVE, 1, parentLocalComponent);
        modificationDecisionAlreadyMade = false;
        resetModifications();
    }

    /**
     * This method sets the date modified and filesize
     * of the file as it is on disk.  This happends on save and load,
     * whenever a clean() is called.
     */
    private void resetModifications() {
        File file = document.getFile();
        if (file != null) {
            if (file.exists()) {
                this.dateLastEdited = file.lastModified();
                this.originalSize = file.length();
            }
        }
    }

    /**
     * This method closes the editor
     */
    public boolean close() throws Exception {
        boolean close = true;
        int decision = -1;
        if (dirty) {
            decision = JOptionPane.showConfirmDialog(this, "'" + document.getDocumentName() + "' has changed, would you like to save it ?", "Save", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (decision == JOptionPane.YES_OPTION) {
                close = save();
            } else if (decision == JOptionPane.CANCEL_OPTION) {
                close = false;
            } else {
                close = true;
            }
        }
        if (close) {
            Application.removeFromGlobalCookie(ExpConstants.MENU_CLOSEALL, 1);
            Application.removeFromGlobalCookie(ExpConstants.MENU_SAVEALL, 1);
        }
        return close;
    }

    /**
     * save the current document
     */
    public boolean save() throws Exception {
        try {
            if (document.getDocumentNumber() == -1) {
                write(document.getFile());
                clean();
                return true;
            } else {
                return saveAs();
            }
        } catch (Exception ex) {
            ExceptionManagerFactory.getExceptionManager().manageException(ex, "Exception caught while saving document.");
        }
        return true;
    }

    /**
     * Save the document as something that it is not at the moment
     */
    public boolean saveAs() throws Exception {
        try {
            boolean done = false;
            while (!done) {
                JFileChooser fc = new JFileChooser();
                File currentFile = getFile();
                if (currentFile == null) currentFile = new File(System.getProperty("user.dir"));
                fc.setCurrentDirectory(currentFile);
                fc.setName("Save as");
                int result = fc.showSaveDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedfile = fc.getSelectedFile();
                    int overWriteDecision = -1;
                    if (selectedfile.exists()) overWriteDecision = JOptionPane.showConfirmDialog(this, "'" + selectedfile.getName() + "' already exists.  Overwrite ?", "File exists !", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (overWriteDecision == JOptionPane.YES_OPTION || overWriteDecision == -1) {
                        write(selectedfile);
                        document.setDocumentName(selectedfile.getName());
                        document.setDocumentNumber(-1);
                        document.setFile(selectedfile);
                        clean();
                        return true;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception ex) {
            ExceptionManagerFactory.getExceptionManager().manageException(ex, "Exception caught while saving document.");
        }
        return true;
    }

    /**
     * This method checks to see if the file size is the same as it was the last time
     * the filesize was checked.  If it isn't the user is asked if they would like
     * to have the file reloaded. 
     * Specifyng the force flag as true will skip the user confirmation and just reload the file.
     *  
     * @param force
     */
    public void checkModifications(boolean force) {
        try {
            File newFile = document.getFile();
            if (newFile != null) {
                if (newFile.lastModified() != dateLastEdited || newFile.length() != originalSize) {
                    if (force) {
                        load();
                    } else {
                        if (!modificationDecisionAlreadyMade) {
                            int decision = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), "'" + document.getDocumentName() + "' has changed, would you like to reload it ?", "Reload?", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                            if (decision == JOptionPane.YES_OPTION) {
                                load();
                            }
                            modificationDecisionAlreadyMade = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            ExceptionManagerFactory.getExceptionManager().manageException(e, "Exception caught while reloading file");
        }
    }

    /**
     * Returns this global action Listener
     * @return
     */
    public ActionListener getGlobalListener() {
        return this.getGlobalListener();
    }

    /**
     * @return Returns the parentLocalComponent.
     */
    public Component getParentLocalComponent() {
        return parentLocalComponent;
    }
}

class FileEditorLocalListener implements ExpActionListener {

    private Map map;

    private int newDocumentNumbers = 1;

    private FileEditorBase editor;

    /**
   * Constructor for TextEditorLocalListener.
   */
    public FileEditorLocalListener(FileEditorBase editor, Component parentReference) {
        this.editor = editor;
        map = new HashMap();
        map.put(ExpConstants.MENU_SAVE, ExpConstants.MENU_SAVE);
        map.put(ExpConstants.MENU_SAVEAS, ExpConstants.MENU_SAVEAS);
        map.put(ExpConstants.MENU_CLOSE, ExpConstants.MENU_CLOSE);
        map.put(ExpConstants.MENU_REFRESH, ExpConstants.MENU_REFRESH);
        ((ExpFrame) Application.getApplicationFrame()).getListener().addLocalActionListener(this, parentReference);
    }

    /**
   * @see package com.explosion.expf.Interfaces.ExpActionListener#getListensFor()
   */
    public Map getListensFor() {
        return map;
    }

    /**
   * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
   */
    public void actionPerformed(ActionEvent e) {
        try {
            if (e.getActionCommand().equals(ExpConstants.MENU_SAVE)) {
                editor.save();
            } else if (e.getActionCommand().equals(ExpConstants.MENU_SAVEAS)) {
                editor.saveAs();
            } else if (e.getActionCommand().equals(ExpConstants.MENU_CLOSE)) {
                if (editor.close()) ((ExpFrame) Application.getApplicationFrame()).closeFrameWithComponent(editor, ExpFrame.DOC_FRAME_LAYER);
            } else if (e.getActionCommand().equals(ExpConstants.MENU_REFRESH)) {
                editor.load();
            }
        } catch (Exception ex) {
            com.explosion.utilities.exception.ExceptionManagerFactory.getExceptionManager().manageException(ex, "Exception caught while responding to SimpleProcess Event.");
        }
    }
}

class FileEditorGlobalListener implements ExpActionListener {

    private HashMap map;

    private int newDocumentNumbers = 1;

    private FileEditorBase editor;

    /**
   * Constructor for FileEditorGlobalListener.
   */
    public FileEditorGlobalListener(FileEditorBase editor) {
        this.editor = editor;
        map = new HashMap();
        map.put(ExpConstants.MENU_CLOSEALL, ExpConstants.MENU_CLOSEALL);
        map.put(ExpConstants.MENU_SAVEALL, ExpConstants.MENU_SAVEALL);
        ((ExpFrame) Application.getApplicationFrame()).getListener().addGlobalActionListener(this, editor);
    }

    /**
   * @see package com.explosion.expf.Interfaces.ExpActionListener#getListensFor()
   */
    public Map getListensFor() {
        return map;
    }

    /**
   * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
   */
    public void actionPerformed(ActionEvent e) {
        try {
            if (e.getActionCommand().equals(ExpConstants.MENU_SAVEALL)) {
                editor.save();
            } else if (e.getActionCommand().equals(ExpConstants.MENU_CLOSEALL)) {
                if (editor.close()) ((ExpFrame) Application.getApplicationFrame()).closeFrameWithComponent(editor, ExpFrame.DOC_FRAME_LAYER);
            }
        } catch (Exception ex) {
            com.explosion.utilities.exception.ExceptionManagerFactory.getExceptionManager().manageException(ex, "Exception caught while responding to SimpleProcess Event.");
        }
    }
}

/**
 * This listener checks to see if the file has changed every time the component gains focus
 * @author Stephen
 * Created on Apr 23, 2004
 */
class FileChangeListener implements FocusListener {

    File file;

    FileEditorBase editor;

    /**
     * Constructor
     * @param textEditor
     * @param file
     */
    private FileChangeListener(FileEditorBase editor) {
        this.file = editor.getDocument().getFile();
        this.editor = editor;
        editor.addFocusListener(this);
    }

    /**
     * Register this editor as one whose file which requires listening to
     * @param editor
     */
    public static void monitor(FileEditorBase editor) {
        if (editor.getDocument().getFile() != null) new FileChangeListener(editor);
    }

    /**
     * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
     * @param arg0
     */
    public void focusGained(FocusEvent arg0) {
        editor.checkModifications(false);
    }

    /**
     * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
     * @param arg0
     */
    public void focusLost(FocusEvent arg0) {
    }
}
