package org.statcato;

import org.statcato.file.FileChooserUtils;
import org.statcato.file.ExtensionFileFilter;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.text.html.*;
import javax.swing.text.*;
import java.io.*;
import org.statcato.utils.*;
import org.statcato.Statcato.*;

/**
 * A log text pane containing HTML-formatted text.  
 * 
 * @author Margaret Yau
 * @version %I%, %G%
 * @since 1.0
 */
public class LogWindow extends JTextPane implements Serializable {

    File savedFile = null;

    boolean changed = false;

    transient Statcato app = null;

    /**
     * Constructor, given the parent application object.
     * 
     * @param mTab parent application object
     */
    public LogWindow(Statcato mTab) {
        super();
        app = mTab;
        initialize();
        addKeyListener(new KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    HTMLDocument doc = (HTMLDocument) getDocument();
                    HTMLEditorKit editorKit = (HTMLEditorKit) getEditorKit();
                    try {
                        editorKit.insertHTML(doc, getCaretPosition(), "<br>", 0, 0, null);
                        int pos = getCaretPosition();
                        setChangedStatus();
                        String txt = getText();
                        setText("");
                        setText(txt);
                        setCaretPosition(pos - 2);
                    } catch (BadLocationException ex) {
                        System.out.println(ex);
                    } catch (IOException ex) {
                        System.out.println(ex);
                    }
                }
                if (!e.isControlDown()) setChangedStatus();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_V) {
                    setChangedStatus();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });
    }

    /**
     * Initializes this object.  Sets the content type to HTML,
     * adds the default message, and sets the status to unchanged.
     */
    private void initialize() {
        setContentType("text/html");
        setText("");
        String WelcomeText = "Welcome to Statcato!<br> ";
        WelcomeText += HelperFunctions.getDateTime() + "<hr size='1'>";
        setText(WelcomeText);
        int pos = getCaretPosition();
        addUnformattedText("");
        setCaretPosition(pos);
        setUnchangedStatus();
        setEditable(true);
        StyleSheet sheet = new HTMLEditorKit().getStyleSheet();
        sheet.addRule("body { font-family: san-serif;}");
    }

    /**
     * Adds the given heading and text message to this object.
     * The heading followed by the message are appended to the end of the
     * HTML document in this object.
     * 
     * @param heading heading string
     * @param message message string
     */
    public void addParagraph(String heading, String message) {
        message = "<br><b>" + heading + "</b><br>" + message.replace("\n", "") + "<br>";
        HTMLDocument doc = (HTMLDocument) getDocument();
        HTMLEditorKit editorKit = (HTMLEditorKit) getEditorKit();
        try {
            editorKit.insertHTML(doc, doc.getLength(), message, 0, 0, null);
            setChangedStatus();
        } catch (BadLocationException e) {
        } catch (IOException e) {
        }
    }

    /**
     * Adds the given message to this object.  The message is appended
     * to the end of the HTML document in this object.
     * 
     * @param message a string to be added to this object
     */
    public void addText(String message) {
        message = message.replace("\n", "") + "<br>";
        HTMLDocument doc = (HTMLDocument) getDocument();
        HTMLEditorKit editorKit = (HTMLEditorKit) getEditorKit();
        try {
            editorKit.insertHTML(doc, doc.getLength(), message, 0, 0, null);
            setChangedStatus();
        } catch (BadLocationException e) {
        } catch (IOException e) {
        }
    }

    /**
     * Adds the given message (as is) to this object.  The message is appended
     * to the end of the HTML document in this object.
     * 
     * @param message a string to be added to this object
     */
    public void addUnformattedText(String message) {
        HTMLDocument doc = (HTMLDocument) getDocument();
        HTMLEditorKit editorKit = (HTMLEditorKit) getEditorKit();
        try {
            editorKit.insertHTML(doc, doc.getLength(), message.replace("\n", ""), 0, 0, null);
            setChangedStatus();
        } catch (BadLocationException e) {
        } catch (IOException e) {
        }
    }

    /**
     * Adds the given message as a heading to this object.  The message
     * is appended to the end of the HTML document in this object in bold font.
     * 
     * @param message a string to be added to this object
     */
    public void addHeading(String message) {
        message = "<br><b>" + message + "</b><br>";
        HTMLDocument doc = (HTMLDocument) getDocument();
        HTMLEditorKit editorKit = (HTMLEditorKit) getEditorKit();
        try {
            editorKit.insertHTML(doc, doc.getLength(), message, 0, 0, null);
            setChangedStatus();
        } catch (BadLocationException e) {
        } catch (IOException e) {
        }
    }

    /**
     * Sets the status of this object to changed.  Adds an asterisk to the
     * log window title to indicate a change to the log window has
     * occured.
     */
    public void setChangedStatus() {
        if (!changed) {
            changed = true;
            app.setLogTitle(app.getLogTitle() + "*");
        }
    }

    /**
     * Sets the status of this object to unchanged.  Removes the asterisk in the
     * log window title to indicate the log window has no 
     * unsaved contents.
     */
    public void setUnchangedStatus() {
        changed = false;
        String title = app.getLogTitle();
        if (title.endsWith("*")) app.setLogTitle(app.getLogTitle().substring(0, title.length() - 1));
    }

    /**
     * Returns the changed status.
     * 
     * @return changed status
     */
    public boolean getChangedStatus() {
        return changed;
    }

    /**
     * Writes the HTML source of this log window object to a file.
     * Prompts the user to select a file location if this object has
     * not previously been saved  or if forced to save in a new file.
     * 
     * @param frame the parent frame
     * @param saveAs a boolean value indicating whether to save as a new file
     * @return the file containing the saved contents, or null if 
     * the save action fails
     */
    public File writeToFile(JFrame frame, boolean saveAs) {
        String path = "";
        String extension = "";
        if (savedFile != null && !saveAs) {
            path = savedFile.getPath();
            extension = FileChooserUtils.getExtension(savedFile);
            writeFileHelper(frame, path);
            return savedFile;
        } else {
            JFileChooser fc = new JFileChooser();
            ExtensionFileFilter HTMLFilter = new ExtensionFileFilter("HTML (*.html)", "html");
            fc.addChoosableFileFilter(HTMLFilter);
            fc.setAcceptAllFileFilterUsed(false);
            int returnValue = fc.showSaveDialog(frame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                path = file.getPath();
                extension = "";
                javax.swing.filechooser.FileFilter filter = fc.getFileFilter();
                if (filter.equals(HTMLFilter)) {
                    extension = "html";
                }
                if (!path.toLowerCase().endsWith("." + extension)) {
                    path += "." + extension;
                    file = new File(path);
                }
                if (file.exists()) {
                    System.out.println("file exists already");
                    Object[] options = { "Overwrite file", "Cancel" };
                    int choice = JOptionPane.showOptionDialog(frame, "The specified file already exists.  Overwrite existing file?", "Overwrite file?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                    if (choice != 0) return null;
                }
                writeFileHelper(frame, path);
                savedFile = file;
                return file;
            }
            return null;
        }
    }

    /**
     * Gets the HTML source in this log pane and actually writes 
     * to the file at the given path.
     * 
     * @param frame parent frame
     * @param path a string indicating the file path 
     */
    private void writeFileHelper(JFrame frame, String path) {
        try {
            String htmlSource = getText();
            BufferedWriter Writer = new BufferedWriter(new FileWriter(path));
            String[] lines = htmlSource.split("\n");
            for (int i = 0; i < lines.length; ++i) {
                Writer.write(lines[i]);
                Writer.newLine();
            }
            Writer.close();
            setUnchangedStatus();
        } catch (IOException e) {
            HelperFunctions.showErrorDialog(frame, "Write file failed!");
        }
    }

    /**
     * Clears the text in this log window and reintializes.
     */
    public void clear() {
        app.compoundEdit = new DialogEdit("clear log");
        initialize();
        setChangedStatus();
        app.compoundEdit.end();
        app.addCompoundEdit(app.compoundEdit);
    }

    /**
     * Replaces the contents of this log window with the given string.
     */
    public void overwrite(String txt) {
        app.compoundEdit = new DialogEdit("load log");
        setText("");
        addText(txt);
        setChangedStatus();
        app.compoundEdit.end();
        app.addCompoundEdit(app.compoundEdit);
    }

    public void appendLog(String log) {
        app.compoundEdit = new DialogEdit("Append log");
        addUnformattedText(log);
        setUnchangedStatus();
        app.compoundEdit.end();
        app.addCompoundEdit(app.compoundEdit);
    }
}
