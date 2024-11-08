package com.memoire.bu;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyleContext;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import com.memoire.fu.FuLog;
import com.memoire.re.RE;
import com.memoire.re.REException;
import com.memoire.re.REMatch;
import com.memoire.re.RESyntax;

/**
 * This component extends a JTextPane. This component provides
 * its own methods to read and save files (even to zip them).
 * This component extends a JTextPane, providing methods to
 * undo/redo, find/replace and read/save.
 * @author Romain Guy, guy.romain@bigfoot.com
 */
public class BuTextPane extends JTextPane implements UndoableEditListener, DocumentListener, BuTextComponentInterface {

    /** This constant defines an open dialog box. */
    public static final int OPEN = 0;

    /** This constant defines a save dialog box. */
    public static final int SAVE = 1;

    private CompoundEdit compoundEdit;

    private String currentFile;

    private int anchor;

    private boolean dirty, newText, operation;

    private UndoManager undo = new UndoManager();

    /** This constant defines the size of the buffer used to read files */
    private static final int BUFFER_SIZE = 32768;

    public BuTextPane() {
        super();
        BuAutoStyledDocument doc = new BuAutoStyledDocument();
        doc.setTextPane(this);
        doc.addDocumentListener(this);
        doc.addUndoableEditListener(this);
        setDocument(doc);
    }

    public void paint(Graphics _g) {
        BuLib.setAntialiasing(this, _g);
        super.paint(_g);
    }

    public void setBorder(Border _b) {
        Border b = _b;
        if (b != null) b = new CompoundBorder(b, new EmptyBorder(0, 30, 0, 0)); else b = new EmptyBorder(0, 30, 0, 0);
        super.setBorder(b);
    }

    public void select() {
        select(0, getLength());
    }

    public void duplicate() {
        int s = getSelectionStart();
        int e = getSelectionEnd();
        if (s < e) {
            copy();
            setCaretPosition(e);
            paste();
            setSelectionEnd(e + (e - s));
            setSelectionStart(e);
        }
    }

    public void go(int _line) {
        Element map = getDocument().getDefaultRootElement();
        Element line = map.getElement(_line);
        select(line.getStartOffset(), line.getEndOffset());
    }

    public void paintComponent(Graphics _g) {
        try {
            super.paintComponent(_g);
        } catch (Exception ex) {
        }
        Insets insets = getInsets();
        Dimension vs = getPreferredScrollableViewportSize();
        Rectangle r = _g.getClipBounds();
        if (r == null) r = new Rectangle(0, 0, getWidth(), getHeight());
        int n, x, y, w, h;
        String s;
        x = vs.width * 2 + insets.left;
        _g.setColor(getBackground().darker());
        _g.drawLine(x, 0, x, getHeight());
        x = insets.left - 30;
        if ((x + 30 >= r.x) && (x <= r.x + r.width)) {
            n = getDocument().getDefaultRootElement().getElementCount();
            h = vs.height / 15;
            y = insets.top - 3;
            _g.setFont(BuLib.deriveFont(_g.getFont(), -2));
            FontMetrics fm = _g.getFontMetrics();
            for (int i = 1; i <= n; i++) {
                y += h;
                if ((y >= r.y) && (y - h <= r.y + r.height)) {
                    s = "" + i;
                    w = fm.stringWidth(s);
                    _g.drawString("" + i, x + 28 - w, y);
                }
            }
        }
    }

    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public Dimension getPreferredScrollableViewportSize() {
        Dimension r = null;
        Document doc = getDocument();
        if (doc instanceof BuAutoStyledDocument) {
            ((BuAutoStyledDocument) doc).getStyle(StyleContext.DEFAULT_STYLE);
            FontMetrics fm = getFontMetrics(getFont());
            r = new Dimension(fm.stringWidth("abcdefghijklmnopqrstuvwxyz01234567890,;:!"), fm.getHeight() * 15);
        } else r = super.getPreferredScrollableViewportSize();
        return r;
    }

    public Dimension getPreferredSize() {
        Dimension r = super.getPreferredSize();
        Document doc = getDocument();
        if (getParent() != null) r = getParent().getSize();
        if (doc instanceof BuAutoStyledDocument) {
            ((BuAutoStyledDocument) doc).getStyle(StyleContext.DEFAULT_STYLE);
            FontMetrics fm = getFontMetrics(getFont());
            int n = doc.getDefaultRootElement().getElementCount();
            int c = 0;
            for (int i = 0; i < n; i++) {
                int start = getLineStartOffset(i);
                int end = getLineEndOffset(i);
                c = Math.max(c, end - start + 1);
            }
            int w = fm.stringWidth("m") * c;
            int h = fm.getHeight() * n;
            Insets insets = getInsets();
            r = new Dimension(Math.max(r.width, w + insets.left + insets.right), Math.max(r.height, h + insets.top + insets.bottom));
        }
        return r;
    }

    public void setCaretPosition(int _pos) {
        super.setCaretPosition(_pos);
        try {
            Rectangle r = modelToView(getCaretPosition());
            r.x = Math.max(0, r.x - 20);
            r.y = Math.max(0, r.y - 20);
            r.width = 40;
            r.height = 40;
            scrollRectToVisible(r);
        } catch (Exception ex) {
        }
    }

    public String getSyntax() {
        String r = null;
        if (getDocument() instanceof BuAutoStyledDocument) r = ((BuAutoStyledDocument) getDocument()).getSyntax();
        return r;
    }

    public void setSyntax(String _syntax) {
        if (getDocument() instanceof BuAutoStyledDocument) ((BuAutoStyledDocument) getDocument()).setSyntax(_syntax);
    }

    public void colorize(BuCommonInterface _app) {
        if (getDocument() instanceof BuAutoStyledDocument) ((BuAutoStyledDocument) getDocument()).colorize(_app);
    }

    /**
   * Display a file chooser dialog box.
   * @param owner <code>Component</code> which 'owns' the dialog
   * @param mode Can be either <code>LOAD</code> or <code>SAVE</code>
   * @return The path to selected file, null otherwise
   */
    public static String chooseFile(Component owner, int mode) {
        BuFileChooser chooser = new BuFileChooser();
        if (mode == OPEN) chooser.setDialogType(JFileChooser.OPEN_DIALOG); else if (mode == SAVE) chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileHidingEnabled(true);
        if (chooser.showDialog(owner, null) == JFileChooser.APPROVE_OPTION) return chooser.getSelectedFile().getAbsolutePath();
        return null;
    }

    /**
   * Display a sample message in a dialog box.
   * @param message The message to display
   */
    public static void showMessage(String message) {
        JOptionPane.showMessageDialog(null, message, "Message", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
   * Display an error message in a dialog box.
   * @param message The message to display
   */
    public static void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
   * Return the full path of the opened file.
   */
    public String getCurrentFile() {
        return currentFile;
    }

    /**
   * When an operation has began, setChanged() cannot be called.
   * This is very important when we need to insert or remove some
   * parts of the texte without turning on the 'to_be_saved' flag.
   */
    public void beginOperation() {
        operation = true;
    }

    /**
   * Calling this will allow the DocumentListener to use setChanged().
   */
    public void endOperation() {
        operation = false;
    }

    /**
   * Return true if we can use the setChanged() method,
   * false otherwise.
   */
    public boolean getOperation() {
        return operation;
    }

    /**
   * Set a new file. We first ask the user if he'd like to save its
   * changes (if some have been made).
   */
    public void newFile() {
        beginOperation();
        if (isDirty() && !isEmpty()) {
            int response = JOptionPane.showConfirmDialog(null, "Do you want to save your changes ?", "Save", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            switch(response) {
                case 0:
                    saveContent();
                    break;
                case 1:
                    break;
                case 2:
                    endOperation();
                    return;
                default:
                    endOperation();
                    return;
            }
        }
        getDocument().removeUndoableEditListener(this);
        getDocument().removeDocumentListener(this);
        clean();
        newText = true;
        currentFile = null;
        setEditable(true);
        setText("");
        setAnchor(0);
        discard();
        getDocument().addUndoableEditListener(this);
        getDocument().addDocumentListener(this);
        endOperation();
    }

    /**
   * This is just to reduce code size of other classes.
   * @param off The line index
   * @return The offset in the text where the line begins
   */
    public int getLineStartOffset(int _n) {
        return getDocument().getDefaultRootElement().getElement(_n).getStartOffset();
    }

    public int getLineEndOffset(int _n) {
        return getDocument().getDefaultRootElement().getElement(_n).getEndOffset();
    }

    /**
   * Called to save current content in specified zip file.
   * Call zip(String file) but asks user for overwriting if
   * file already exists.
   */
    public void zipContent() {
        if (getText().equals("")) return;
        String zipFile = chooseFile(this, SAVE);
        if (zipFile != null) {
            if (!zipFile.endsWith(".zip")) zipFile += ".zip";
            if (!(new File(zipFile)).exists()) zip(zipFile); else {
                int response = JOptionPane.showConfirmDialog(null, "File already exists, overwrite it ?", "Save", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                switch(response) {
                    case 0:
                        zip(zipFile);
                        break;
                    case 1:
                        break;
                    default:
                        return;
                }
            }
        }
    }

    /**
   * Zip text pane content into specified file.
   * @param zipFile The file name where to zip the text
   */
    public void zip(String zipFile) {
        try {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
            out.putNextEntry(new ZipEntry((new File(currentFile)).getName()));
            String newline = System.getProperty("line.separator");
            Element map = getDocument().getDefaultRootElement();
            for (int i = 0; i < map.getElementCount(); i++) {
                Element line = map.getElement(i);
                int start = line.getStartOffset();
                byte[] buf = (getText(start, line.getEndOffset() - start - 1) + newline).getBytes();
                out.write(buf, 0, buf.length);
            }
            out.closeEntry();
            out.close();
        } catch (IOException ioe) {
            showError("Error has occured while ziping");
        } catch (BadLocationException ble) {
            showError("Error has occured while ziping");
        }
    }

    /**
   * Called to save this component's content.
   * Call save(String file) but let the user choosing a file name.
   * In the case the user choosed an existing file, we ask him if
   * he really wants to overwrite it.
   */
    public void saveContent() {
        String fileToSave = chooseFile(this, SAVE);
        if (fileToSave != null) {
            if (!(new File(fileToSave)).exists()) save(fileToSave); else {
                int response = JOptionPane.showConfirmDialog(null, "File already exists, overwrite it ?", "Save", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                switch(response) {
                    case 0:
                        save(fileToSave);
                        break;
                    case 1:
                        break;
                    default:
                        return;
                }
            }
        }
    }

    /**
   * Store the text in a specified file.
   * @param file The file in wich we'll write the text
   */
    public void save(String file) {
        try {
            OutputStream outs = new FileOutputStream(new File(file));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outs), BUFFER_SIZE);
            String newline = System.getProperty("line.separator");
            Element map = getDocument().getDefaultRootElement();
            for (int i = 0; i < map.getElementCount(); i++) {
                Element line = map.getElement(i);
                int start = line.getStartOffset();
                out.write(getText(start, line.getEndOffset() - start - 1));
                out.write(newline);
            }
            out.close();
            outs.close();
            if (!file.equals(currentFile)) currentFile = file;
        } catch (IOException ioe) {
            showError("Error has occured while saving");
        } catch (BadLocationException ble) {
            showError("Error has occured while saving");
        }
    }

    /**
   * Called to load a new file in the text pane.
   * Determines which line separator (\n, \r\n...) are used in the
   * file to open. Convert'em into Swing line separator (\n).
   * @param path The path of the file to be loaded
   */
    public void open(String path) {
        beginOperation();
        getDocument().removeUndoableEditListener(this);
        getDocument().removeDocumentListener(this);
        clean();
        discard();
        InputStream is;
        StringBuffer buffer = new StringBuffer();
        try {
            File toLoad = new File(path);
            if (!toLoad.canWrite()) setEditable(false); else if (!isEditable()) setEditable(true);
            is = new FileInputStream(toLoad);
            InputStreamReader in = new InputStreamReader(is);
            char[] buf = new char[BUFFER_SIZE];
            int len;
            boolean lastWasCR = false;
            while ((len = in.read(buf, 0, buf.length)) != -1) {
                int lastLine = 0;
                for (int i = 0; i < len; i++) {
                    switch(buf[i]) {
                        case '\r':
                            if (lastWasCR) {
                            } else lastWasCR = true;
                            buffer.append(buf, lastLine, i - lastLine);
                            buffer.append('\n');
                            lastLine = i + 1;
                            break;
                        case '\n':
                            if (lastWasCR) {
                                lastWasCR = false;
                                lastLine = i + 1;
                            } else {
                                buffer.append(buf, lastLine, i - lastLine);
                                buffer.append('\n');
                                lastLine = i + 1;
                            }
                            break;
                        default:
                            if (lastWasCR) {
                                lastWasCR = false;
                            }
                            break;
                    }
                }
                buffer.append(buf, lastLine, len - lastLine);
            }
            in.close();
            if (buffer.length() != 0 && buffer.charAt(buffer.length() - 1) == '\n') buffer.setLength(buffer.length() - 1);
            getDocument().remove(0, getLength());
            getDocument().insertString(0, buffer.toString(), null);
            currentFile = path;
            setCaretPosition(0);
            newText = false;
            getDocument().addUndoableEditListener(this);
            getDocument().addDocumentListener(this);
        } catch (BadLocationException bl) {
            FuLog.error(bl);
        } catch (FileNotFoundException fnf) {
            showError(path + " not found !");
        } catch (IOException io) {
            showError(io.toString());
        }
        endOperation();
    }

    /**
   * Return true if current text is new, false otherwise.
   */
    public boolean isNew() {
        return newText;
    }

    /**
   * Return true if pane is empty, false otherwise.
   */
    public boolean isEmpty() {
        return (getLength() == 0);
    }

    /**
   * Return true if pane content has changed, false otherwise.
   */
    public boolean isDirty() {
        return dirty;
    }

    /**
   * Called when the content of the pane has changed.
   */
    public void setDirty() {
        dirty = true;
    }

    /**
   * Called after having saved or created a new document to ensure
   * the content isn't 'dirty'.
   */
    public void clean() {
        dirty = false;
    }

    /**
   * Discard all edits contained in the UndoManager.
   */
    public void discard() {
        undo.discardAllEdits();
    }

    /**
   * Useful for the GUI.
   */
    public UndoManager getUndo() {
        return undo;
    }

    /**
   * undo the last operation
   */
    public void undo() {
        if (undo.canUndo()) undo.undo();
    }

    /**
   * redo the last operation
   */
    public void redo() {
        if (undo.canRedo()) undo.redo();
    }

    /**
   * Return the anchor position.
   */
    public int getAnchor() {
        return anchor;
    }

    /**
   * Set the anchor postion.
   * @param offset The new anchor's position
   */
    public void setAnchor(int offset) {
        anchor = offset;
    }

    /**
   * Return the lentgh of the text in the pane.
   */
    public int getLength() {
        return getDocument().getLength();
    }

    /**
   * Used for ReplaceAll.
   * This merges all text changes made between the beginCompoundEdit()
   * and the endCompoundEdit() calls into only one undo event.
   */
    public void beginCompoundEdit() {
        if (compoundEdit == null) compoundEdit = new CompoundEdit();
    }

    /**
   * See beginCompoundEdit().
   */
    public void endCompoundEdit() {
        if (compoundEdit != null) {
            compoundEdit.end();
            if (compoundEdit.canUndo()) undo.addEdit(compoundEdit);
            compoundEdit = null;
        }
    }

    /**
   * Return the result of a string search.
   * @param searchStr The string to be found
   * @param start The search's start offset
   * @param ignoreCase Set to true, we'll ignore the text case
   * @return True if <code>searchStr</code> has been found, false otherwise
   */
    public boolean find(String _searchStr, int start, boolean ignoreCase) {
        try {
            String searchStr = _searchStr;
            if (searchStr == null || searchStr.equals("")) return false;
            RE regexp = null;
            try {
                regexp = new RE(searchStr, (ignoreCase == true ? RE.REG_ICASE : 0) | RE.REG_MULTILINE, RESyntax.RE_SYNTAX_PERL5);
            } catch (Exception ex) {
            }
            if (regexp == null) {
                getToolkit().beep();
                return false;
            }
            String text = getText(start, getLength() - start);
            REMatch match = regexp.getMatch(text);
            if (match != null) {
                this.select(start + match.getStartIndex(), start + match.getEndIndex());
                return true;
            }
        } catch (Exception ex) {
            FuLog.error(ex);
        }
        return false;
    }

    public boolean replace(String searchStr, String replaceStr, int start, int end, boolean ignoreCase) {
        boolean r = false;
        try {
            r = replaceAll(searchStr, replaceStr, start, end, ignoreCase);
        } catch (Exception ex) {
        }
        return r;
    }

    /**
   * Return the result of a string replace.
   * @param searchStr The string to be found
   * @param replaceStr The string which will replace <code>searchStr</code>
   * @param start The search's start offset
   * @param end The search's end offset
   * @param ignoreCase Set to true, we'll ignore the text case
   * @return True if the replace has been successfully done, false otherwise
   */
    public boolean replaceAll(String _searchStr, String _replaceStr, int start, int _end, boolean ignoreCase) throws REException {
        boolean found = false;
        try {
            String searchStr = _searchStr;
            if (searchStr == null || searchStr.equals("")) return false;
            RE regexp = null;
            try {
                regexp = new RE(searchStr, (ignoreCase == true ? RE.REG_ICASE : 0) | RE.REG_MULTILINE, RESyntax.RE_SYNTAX_PERL5);
            } catch (Exception ex) {
            }
            if (regexp == null) {
                getToolkit().beep();
                return false;
            }
            String replaceStr = _replaceStr;
            if (replaceStr == null) replaceStr = "";
            beginCompoundEdit();
            Element map = getDocument().getDefaultRootElement();
            int startLine = map.getElementIndex(start);
            int end = _end;
            int endLine = map.getElementIndex(end);
            for (int i = startLine; i <= endLine; i++) {
                Element lineElement = map.getElement(i);
                int lineStart;
                int lineEnd;
                if (i == startLine) lineStart = start; else lineStart = lineElement.getStartOffset();
                if (i == endLine) lineEnd = end; else lineEnd = lineElement.getEndOffset() - 1;
                lineEnd -= lineStart;
                String line = getText(lineStart, lineEnd);
                String newLine = regexp.substituteAll(line, replaceStr);
                if (line.equals(newLine)) continue;
                getDocument().remove(lineStart, lineEnd);
                getDocument().insertString(lineStart, newLine, null);
                end += (newLine.length() - lineEnd);
                found = true;
            }
        } catch (Exception ex) {
            FuLog.error(ex);
        }
        endCompoundEdit();
        return found;
    }

    /**
   * When an undoable event is fired, we add it to the undo/redo list.
   */
    public void undoableEditHappened(UndoableEditEvent e) {
        if (!getOperation()) {
            if (compoundEdit == null) undo.addEdit(e.getEdit()); else compoundEdit.addEdit(e.getEdit());
        }
    }

    /**
   * When a modification is made in the text, we turn
   * the 'to_be_saved' flag to true.
   */
    public void changedUpdate(DocumentEvent e) {
        if (!getOperation() && !isDirty()) setDirty();
    }

    /**
   * When a modification is made in the text, we turn
   * the 'to_be_saved' flag to true.
   */
    public void insertUpdate(DocumentEvent e) {
        if (!getOperation() && !isDirty()) setDirty();
    }

    /**
   * When a modification is made in the text, we turn
   * the 'to_be_saved' flag to true.
   */
    public void removeUpdate(DocumentEvent e) {
        if (!getOperation() && !isDirty()) setDirty();
    }

    /**
   * Return a String representation of this object.
   */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("BuTextPane: ");
        buf.append("[filename: " + getCurrentFile() + ";");
        buf.append(" filesize: " + getLength() + "] -");
        buf.append("[anchor: " + getAnchor() + "] -");
        return buf.toString();
    }
}
