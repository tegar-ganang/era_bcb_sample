package com.tgjorgoski.window;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.demo.HTMLDocument;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.store.LockObtainFailedException;
import com.tgjorgoski.browser.BrowserPanel;
import com.tgjorgoski.htmlview.HTMLDocChanged;
import com.tgjorgoski.htmlview.HTMLEditor;
import com.tgjorgoski.rtfview.support.*;
import com.tgjorgoski.spellcheck.SpellcheckMenuFiller;
import com.tgjorgoski.utils.*;
import skt.swing.scroll.ScrollGestureRecognizer;
import skt.swing.search.TextComponentFindAction;
import skt.swing.text.CurrentLineHighlighter;

public class HTMLView extends JPanel implements MouseListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = -8745479843842219910L;

    HTMLEditor editor;

    SideNotesApp mainWindow;

    BrowserPanel bPanel;

    SearchPanel searchPanel;

    JToolBar nameToolbar;

    JLabel noteNameLabel;

    static char dirSep = System.getProperty("file.separator").charAt(0);

    public HTMLView(SideNotesApp mainWindow) {
        this.mainWindow = mainWindow;
        setBorder(new CompoundBorder(new BevelBorder(BevelBorder.RAISED), new EmptyBorder(0, 4, 4, 2)));
        setSize(500, 700);
        setLocation(300, 20);
        setLayout(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        add(splitPane, BorderLayout.CENTER);
        JPanel editorPanel = new JPanel();
        editorPanel.setLayout(new BorderLayout());
        editor = new HTMLEditor(this);
        final Color c1 = new Color(170, 170, 255);
        final Color c2 = new Color(255, 255, 255);
        nameToolbar = new JToolBar() {

            @Override
            public void paint(Graphics g) {
                int w = getWidth();
                int h = getHeight();
                Graphics2D g2d = (Graphics2D) g;
                Color bckgColor = g2d.getColor();
                g2d.setColor(c1);
                g2d.fillRect(0, 0, w, h);
                g2d.setColor(bckgColor);
                super.paint(g);
            }
        };
        nameToolbar.add(Box.createHorizontalStrut(5));
        nameToolbar.setOpaque(false);
        nameToolbar.setBorder(new EmptyBorder(-2, 0, 4, 0));
        nameToolbar.setFloatable(false);
        nameToolbar.setRollover(true);
        JPanel doubleToolbar = new JPanel();
        doubleToolbar.setLayout(new BorderLayout(0, -7));
        JToolBar toolbar = new JToolBar();
        initToolbarActions(toolbar);
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        JToolBar toolbar2 = new JToolBar();
        initToolbarActions2(toolbar2);
        toolbar2.setFloatable(false);
        toolbar2.setRollover(true);
        doubleToolbar.add(toolbar2, BorderLayout.CENTER);
        doubleToolbar.add(toolbar, BorderLayout.SOUTH);
        noteNameLabel = new JLabel("Untitled");
        noteNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        noteNameLabel.setAlignmentY(CENTER_ALIGNMENT);
        noteNameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameToolbar.add(new JLabel(IconResource.getIconResource("vseparator.gif")));
        nameToolbar.add(Box.createHorizontalStrut(10));
        nameToolbar.add(new JLabel(IconResource.getIconResource("dots3.gif")));
        nameToolbar.add(noteNameLabel);
        doubleToolbar.add(nameToolbar, BorderLayout.NORTH);
        editorPanel.add(doubleToolbar, BorderLayout.NORTH);
        JScrollPane scroller = new JScrollPane();
        scroller.getViewport().add(editor);
        editorPanel.add(scroller, BorderLayout.CENTER);
        ScrollGestureRecognizer.getInstance();
        Action findAction = new TextComponentFindAction(true);
        String name = (String) findAction.getValue(Action.NAME);
        editor.getActionMap().put(name, findAction);
        InputMap inputMap = editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK);
        inputMap.put(key, name);
        key = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
        inputMap.put(key, name);
        popupMenuFillers.add(new ClipboardMenuFiller());
        popupMenuFillers.add(new SpellcheckMenuFiller());
        editor.addMouseListener(this);
        File homeFolder = new File(Session.getHomeFolder());
        if (!homeFolder.exists()) {
            homeFolder.mkdir();
        }
        File notesFolder = new File(Session.getNotesFolder());
        if (!notesFolder.exists()) {
            notesFolder.mkdir();
        }
        searchPanel = new SearchPanel(this);
        add(searchPanel, BorderLayout.NORTH);
        bPanel = new BrowserPanel(notesFolder, this);
        bPanel.refresh(null);
        splitPane.setTopComponent(bPanel);
        splitPane.setBottomComponent(editorPanel);
        splitPane.setDividerLocation(180);
        editor.addCaretListener(new CaretListener() {

            public void caretUpdate(CaretEvent e) {
                showAttributes();
            }
        });
        editor.getStyledDocument().addUndoableEditListener(new UndoableEditListener() {

            public void undoableEditHappened(UndoableEditEvent e) {
                showAttributes();
            }
        });
        editor.addKeyListener(new KeyListener() {

            public void keyTyped(KeyEvent e) {
                showAttributes();
            }

            public void keyReleased(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
            }
        });
        showAttributes();
    }

    boolean m_skipUpdate = false;

    JComboBox m_cbSizes;

    JComboBox m_cbFonts;

    JToggleButton m_bBold, m_bItalic, m_bUnderline;

    JToggleButton m_bSuperscript, m_bSubscript, m_bStrikethrough;

    protected void showAttributes() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                m_skipUpdate = true;
                AttributeSet a = editor.getInputAttributes();
                String name = StyleConstants.getFontFamily(a);
                if (name.equals("Monospaced")) name = "Default";
                m_cbFonts.setSelectedItem(name);
                int size = StyleConstants.getFontSize(a);
                m_cbSizes.setSelectedItem(Integer.toString(size));
                boolean bold = StyleConstants.isBold(a);
                m_bBold.setSelected(bold);
                boolean italic = StyleConstants.isItalic(a);
                m_bItalic.setSelected(italic);
                boolean underline = StyleConstants.isUnderline(a);
                m_bUnderline.setSelected(underline);
                m_skipUpdate = false;
            }
        });
    }

    protected int m_xStart = -1;

    protected int m_xFinish = -1;

    protected void setAttributeSet(AttributeSet attr) {
        if (m_skipUpdate) return;
        editor.setCharacterAttributes(attr, false);
        StyledEditorKit k = (StyledEditorKit) editor.getEditorKit();
        MutableAttributeSet inputAttributes = k.getInputAttributes();
        inputAttributes.addAttributes(attr);
    }

    private void initToolbarActions(JToolBar panel) {
        m_cbSizes = new JComboBox(new String[] { "10", "12", "14", "18", "24", "36" });
        m_cbSizes.setRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String) {
                    String st = (String) value;
                    int size = Integer.parseInt(st);
                    Font f = getFont().deriveFont(size * 1.0f);
                    setFont(f);
                }
                return comp;
            }
        });
        m_cbSizes.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object value = m_cbSizes.getSelectedItem();
                    if (value instanceof String) {
                        String st = (String) value;
                        int size = Integer.parseInt(st);
                        MutableAttributeSet attr = new SimpleAttributeSet();
                        StyleConstants.setFontSize(attr, size);
                        setAttributeSet(attr);
                        editor.grabFocus();
                    }
                }
            }
        });
        Dimension dimPS = m_cbSizes.getPreferredSize();
        dimPS.width += 10;
        m_cbSizes.setEditable(true);
        m_cbSizes.setPreferredSize(dimPS);
        m_cbSizes.setMaximumSize(dimPS);
        m_cbFonts = new JComboBox(new String[] { "Default", "Arial", "Courier New", "Georgia", "Tahoma", "Times New Roman", "Verdana" });
        m_cbFonts.setEditable(true);
        m_cbFonts.setRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String) {
                    String st = (String) value;
                    if (st.equals("Default")) st = "Monospaced";
                    Font f = getFont();
                    Font font = new Font(st, f.getStyle(), f.getSize());
                    setFont(font);
                }
                return comp;
            }
        });
        m_cbFonts.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object value = m_cbFonts.getSelectedItem();
                    if (value instanceof String) {
                        String st = (String) value;
                        MutableAttributeSet attr = new SimpleAttributeSet();
                        StyleConstants.setFontFamily(attr, st);
                        setAttributeSet(attr);
                        editor.grabFocus();
                    }
                }
            }
        });
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        m_bBold = new JToggleButton("");
        m_bBold.setAction(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                editor.getActionMap().get("font-bold").actionPerformed(e);
                editor.requestFocus();
            }
        });
        m_bItalic = new JToggleButton("");
        m_bItalic.setAction(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                editor.getActionMap().get("font-italic").actionPerformed(e);
                editor.requestFocus();
            }
        });
        m_bUnderline = new JToggleButton("");
        m_bUnderline.setAction(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                editor.getActionMap().get("font-underline").actionPerformed(e);
                editor.requestFocus();
            }
        });
        JButton clearFormatting = new JButton("");
        clearFormatting.setAction(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                editor.clearFormatting();
                editor.requestFocus();
            }
        });
        clearFormatting.setText("");
        clearFormatting.setIcon(IconResource.getIconResource("clear-formatting"));
        clearFormatting.setToolTipText("Clear Formatting");
        clearFormatting.setPreferredSize(buttonSize);
        Dimension buttonSize = new Dimension(28, 30);
        m_bBold.setText("");
        m_bBold.setIcon(IconResource.getIconResource("text-bold"));
        m_bBold.setToolTipText("Bold");
        m_bBold.setPreferredSize(buttonSize);
        m_bItalic.setText("");
        m_bItalic.setIcon(IconResource.getIconResource("text-italic"));
        m_bItalic.setToolTipText("Italic");
        m_bItalic.setPreferredSize(buttonSize);
        m_bUnderline.setText("");
        m_bUnderline.setIcon(IconResource.getIconResource("text-underline"));
        m_bUnderline.setToolTipText("Underline");
        m_bUnderline.setPreferredSize(buttonSize);
        DropDownColorButton ddb = new DropDownColorButton(editor);
        ddb.setPreferredSize(buttonSize);
        ddb.setToolTipText("Font Color");
        ddb.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (e.getSource() instanceof DropDownColorButton) {
                    DropDownColorButton ddcb = (DropDownColorButton) e.getSource();
                    MutableAttributeSet attr = new SimpleAttributeSet();
                    StyleConstants.setForeground(attr, ddcb.getColor());
                    editor.setCharacterAttributes(attr, false);
                    editor.requestFocus();
                }
            }
        });
        panel.add(m_cbFonts);
        panel.add(m_cbSizes);
        panel.add(new JLabel(IconResource.getIconResource("vseparator.gif")));
        panel.add(m_bBold);
        panel.add(m_bItalic);
        panel.add(m_bUnderline);
        panel.add(new JLabel(IconResource.getIconResource("vseparator.gif")));
        ddb.addToToolBar(panel);
        panel.add(new JLabel(IconResource.getIconResource("vseparator.gif")));
        panel.add(clearFormatting);
    }

    private void initToolbarActions2(JToolBar panel) {
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        InputMap inputMap = editor.getInputMap(JComponent.WHEN_FOCUSED);
        JButton editNew = new JButton("");
        Action newDocAction = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                requestNewDocument(false);
            }
        };
        editNew.setAction(newDocAction);
        editor.getActionMap().put("new-doc-action", newDocAction);
        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK);
        inputMap.put(key, "new-doc-action");
        JButton saveButton = new JButton("");
        Action saveAction = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                if (saveCurrent()) return;
                editor.requestFocus();
            }
        };
        saveButton.setAction(saveAction);
        editor.getActionMap().put("save-action", saveAction);
        KeyStroke key2 = KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK);
        inputMap.put(key2, "save-action");
        JButton insertSeparator = new JButton("");
        insertSeparator.setAction(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                try {
                    int sPosition = editor.getSelectionStart();
                    HTMLDocChanged htmlDoc = (HTMLDocChanged) editor.getDocument();
                    Element paragraph = htmlDoc.getParagraphElement(sPosition);
                    if (paragraph != null) {
                        htmlDoc.insertBeforeStart(paragraph, "<hr />");
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
                editor.requestFocus();
            }
        });
        insertSeparator.setIcon(IconResource.getIconResource("insertseparator"));
        insertSeparator.setToolTipText("Insert Separator");
        insertSeparator.setPreferredSize(buttonSize);
        editNew.setText("");
        editNew.setIcon(IconResource.getIconResource("new18"));
        editNew.setToolTipText("New");
        editNew.setPreferredSize(buttonSize);
        saveButton.setText("");
        saveButton.setIcon(IconResource.getIconResource("save18"));
        saveButton.setToolTipText("Save");
        saveButton.setPreferredSize(buttonSize);
        JButton alignLeft = new JButton("");
        alignLeft.setAction(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                editor.getActionMap().get("left-justify").actionPerformed(e);
                editor.requestFocus();
            }
        });
        JButton alignCenter = new JButton("");
        alignCenter.setAction(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                editor.getActionMap().get("center-justify").actionPerformed(e);
                editor.requestFocus();
            }
        });
        JButton alignRight = new JButton("");
        alignRight.setAction(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                editor.getActionMap().get("right-justify").actionPerformed(e);
                editor.requestFocus();
            }
        });
        alignLeft.setIcon(IconResource.getIconResource("justify-left"));
        alignLeft.setText("");
        alignLeft.setToolTipText("Align Left");
        alignLeft.setPreferredSize(buttonSize);
        alignCenter.setIcon(IconResource.getIconResource("justify-center"));
        alignCenter.setText("");
        alignCenter.setToolTipText("Center");
        alignCenter.setPreferredSize(buttonSize);
        alignRight.setIcon(IconResource.getIconResource("justify-right"));
        alignRight.setText("");
        alignRight.setToolTipText("Align Right");
        alignRight.setPreferredSize(buttonSize);
        JButton editUndo = getButton(UndoRedoSupport.getInstance().getUndoAction(), "undo", "Undo", true);
        JButton editRedo = getButton(UndoRedoSupport.getInstance().getRedoAction(), "redo", "Redo", true);
        JButton editCut = getButton(editor.getActionMap().get(DefaultEditorKit.cutAction), "editcut", "Cut", false);
        JButton editCopy = getButton(editor.getActionMap().get(DefaultEditorKit.copyAction), "editcopy", "Copy", false);
        JButton editPaste = getButton(editor.getActionMap().get(DefaultEditorKit.pasteAction), "editpaste", "Paste", false);
        editNew.setOpaque(false);
        saveButton.setOpaque(false);
        nameToolbar.add(editNew);
        nameToolbar.add(saveButton);
        panel.add(editCut);
        panel.add(editCopy);
        panel.add(editPaste);
        panel.add(new JLabel(IconResource.getIconResource("vseparator.gif")));
        editUndo.setEnabled(false);
        editRedo.setEnabled(false);
        panel.add(editUndo);
        panel.add(editRedo);
        panel.add(new JLabel(IconResource.getIconResource("vseparator.gif")));
        panel.add(alignLeft);
        panel.add(alignCenter);
        panel.add(alignRight);
        panel.add(new JLabel(IconResource.getIconResource("vseparator.gif")));
        panel.add(insertSeparator);
    }

    Dimension buttonSize = new Dimension(28, 30);

    private JButton getButton(final Action act, String iconName, String tooltip, final boolean updateTooltip) {
        final JButton newButton = new JButton("");
        newButton.setAction(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                act.actionPerformed(e);
                editor.requestFocus();
            }
        });
        act.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("enabled")) {
                    Boolean bool = (Boolean) evt.getNewValue();
                    newButton.setEnabled(bool);
                } else if (evt.getPropertyName().equals("Name") && updateTooltip) {
                    String name = (String) evt.getNewValue();
                    newButton.setToolTipText(name);
                }
            }
        });
        newButton.setText("");
        newButton.setIcon(IconResource.getIconResource(iconName));
        newButton.setToolTipText(tooltip);
        newButton.setPreferredSize(buttonSize);
        return newButton;
    }

    JPopupMenu rightClickMenu = new JPopupMenu();

    private java.util.List popupMenuFillers = new Vector();

    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e) && (e.getSource() instanceof JTextComponent)) {
            JTextComponent comp = (JTextComponent) e.getSource();
            Point pt = new Point(e.getX(), e.getY());
            int pos = comp.viewToModel(pt);
            if (comp.getSelectionStart() == comp.getSelectionEnd()) {
                comp.requestFocus();
                comp.setCaretPosition(pos);
            }
            rightClickMenu.removeAll();
            int number = 0;
            for (Iterator iter = popupMenuFillers.iterator(); iter.hasNext(); ) {
                PopupMenuFiller filler = (PopupMenuFiller) iter.next();
                filler.populateMenu(rightClickMenu, e);
                int newNumber = rightClickMenu.getComponentCount();
                if ((newNumber > number)) {
                    rightClickMenu.addSeparator();
                    number = newNumber;
                }
            }
            ViewUtils.cleanUpPopupMenu(rightClickMenu);
            if (rightClickMenu.getComponentCount() != 0) {
                rightClickMenu.show(e.getComponent(), e.getX(), e.getY());
                SpellCheckSupport.getInstance().updateHighlights();
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void requestNewDocument(boolean forceNotSave) {
        if (!forceNotSave) if (saveCurrentIfNeeded("New Note")) return;
        editor.newDocument();
        updateTitle();
    }

    public void requestFileOpen(File file) {
        if (saveCurrentIfNeeded("Open Note")) return;
        editor.load(file);
        editor.requestFocus();
        updateTitle();
    }

    /**
	 * @return true if calling operation should be canceled
	 */
    public boolean saveCurrentIfNeeded(String operation) {
        if (!editor.isModified()) return false;
        int option = JOptionPane.showConfirmDialog(HTMLView.this, "Save changes to current note? If you don't all changes will be lost.", operation, JOptionPane.YES_NO_CANCEL_OPTION);
        if (option == JOptionPane.CANCEL_OPTION) return true;
        if (option == JOptionPane.YES_OPTION) return saveCurrent();
        return false;
    }

    /**
	 * @return true if calling operation should be canceled
	 */
    public boolean saveCurrent() {
        File openedFile = editor.getOpenedFile();
        Writer wr;
        boolean oldFile = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            if (openedFile != null) {
                wr = new FileWriter(openedFile);
            } else {
                int option;
                do {
                    option = JOptionPane.OK_OPTION;
                    String name = JOptionPane.showInputDialog(HTMLView.this, "Enter the note name...", "Save Note", JOptionPane.OK_CANCEL_OPTION);
                    if (name == null || name.equals("")) return true;
                    File saveInFolder = bPanel.getFolder();
                    openedFile = new File(saveInFolder, name + ".html");
                    if (openedFile.exists()) {
                        option = JOptionPane.showConfirmDialog(HTMLView.this, "A note with the name - '" + name + "' already exists. Overwrite?", "Save Note", JOptionPane.YES_NO_CANCEL_OPTION);
                        if (option == JOptionPane.CANCEL_OPTION) return true;
                    } else {
                        oldFile = false;
                    }
                } while (option != JOptionPane.YES_OPTION);
                openedFile.createNewFile();
                wr = new FileWriter(openedFile);
            }
            editor.getEditorKit().write(wr, editor.getDocument(), 0, editor.getDocument().getLength());
            wr.close();
            IndexModifier modifier = getIndexModifier();
            if (oldFile) {
                deleteFromIndex(openedFile, modifier);
            }
            addToIndex(openedFile, modifier);
            modifier.flush();
            modifier.close();
            editor.setOpenedFile(openedFile);
            editor.resetModified();
        } catch (Exception exc) {
            exc.printStackTrace();
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
        refresh(openedFile);
        return false;
    }

    public void refresh(File ensureVisibleFile) {
        String text = searchPanel.tComp.getText();
        if (ensureVisibleFile != null) {
            if (!text.equals("")) doSearch(text);
            boolean isFocused = bPanel.focusOnFile(ensureVisibleFile);
            if (!isFocused) {
                searchPanel.clearTheSearch();
                bPanel.refreshToAllFiles();
                bPanel.focusOnFile(ensureVisibleFile);
            }
        } else {
            if (!text.equals("")) doSearch(text); else bPanel.refreshToAllFiles();
        }
    }

    public int doSearch(String query) {
        int numFound = bPanel.doSearch(query);
        if (numFound == 0) {
            bPanel.refreshToAllFiles();
        }
        return numFound;
    }

    public void setFocusOnEditor() {
        editor.requestFocusInWindow();
        editor.requestFocusInWindow();
    }

    public void setFocusOnBrowser() {
        bPanel.setFocusOnBrowser();
    }

    public void delete(File deleteFile) {
        String name = deleteFile.getName();
        if (name.endsWith(".html")) name = name.substring(0, name.length() - 5);
        int option = JOptionPane.showConfirmDialog(HTMLView.this, "Are you sure you want to delete note - '" + name + " ?\n" + "After deleting you won't be able to undo this operation.", "Delete Note", JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.NO_OPTION) return;
        boolean deleted = deleteFile.delete();
        if (!deleted) {
            JOptionPane.showMessageDialog(HTMLView.this, "Note '" + name + "' couldn't be deleted", "Delete Note", JOptionPane.OK_OPTION);
            return;
        }
        try {
            IndexModifier modifier = getIndexModifier();
            deleteFromIndex(deleteFile, modifier);
            modifier.flush();
            modifier.close();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        File openedFile = editor.getOpenedFile();
        if (openedFile != null && openedFile.equals(deleteFile)) {
            requestNewDocument(true);
        }
        refresh(null);
    }

    public void rename(File renameFile) {
        String name = renameFile.getName();
        File renamedFile;
        String newName;
        if (name.endsWith(".html")) name = name.substring(0, name.length() - 5);
        boolean success = false;
        ;
        do {
            newName = JOptionPane.showInputDialog(HTMLView.this, "Enter the new name for the note '" + name + "'", "Rename Note", JOptionPane.OK_CANCEL_OPTION);
            if (newName == null || newName.equals("")) return;
            File saveInFolder = bPanel.getFolder();
            renamedFile = new File(saveInFolder, newName + ".html");
            if (renamedFile.exists()) {
                JOptionPane.showMessageDialog(HTMLView.this, "A note with the name - '" + newName + "' already exists. Please choose another name.", "Rename Note", JOptionPane.OK_OPTION);
            } else {
                success = true;
            }
        } while (!success);
        boolean succ = renameFile.renameTo(renamedFile);
        if (!succ) {
            JOptionPane.showMessageDialog(HTMLView.this, "Note '" + name + "' couldn't be renamed", "Rename Note", JOptionPane.OK_OPTION);
            return;
        }
        try {
            IndexModifier modifier = getIndexModifier();
            deleteFromIndex(renameFile, modifier);
            addToIndex(renamedFile, modifier);
            modifier.flush();
            modifier.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        File openedFile = editor.getOpenedFile();
        if (openedFile != null && openedFile.equals(renameFile)) {
            editor.setOpenedFile(renamedFile);
            updateTitle();
        }
        refresh(renamedFile);
    }

    public IndexModifier getIndexModifier() {
        IndexModifier modifier = null;
        try {
            try {
                modifier = new IndexModifier(Session.getIndexFolder(), new StandardAnalyzer(), false);
            } catch (FileNotFoundException fnfe) {
                modifier = new IndexModifier(Session.getIndexFolder(), new StandardAnalyzer(), true);
            }
        } catch (CorruptIndexException e) {
            e.printStackTrace();
            return null;
        } catch (LockObtainFailedException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return modifier;
    }

    public int deleteFromIndex(File fileName, IndexModifier modifier) {
        int deltd = 0;
        String cPath = HTMLDocument.getNormPath(fileName, Session.getHomeFolder());
        Term fileNameTerm = new Term("path", cPath);
        try {
            deltd = modifier.deleteDocuments(fileNameTerm);
            System.out.println("Deleted " + deltd);
        } catch (IOException exc) {
            exc.printStackTrace();
        }
        return deltd;
    }

    private void addToIndex(File file, IndexModifier modifier) {
        try {
            Document indDocument = HTMLDocument.Document(file, Session.getHomeFolder());
            modifier.addDocument(indDocument);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    public HTMLEditor getEditor() {
        return editor;
    }

    public void updateTitle() {
        File openedFile = editor.getOpenedFile();
        String name = "Untitled";
        if (openedFile != null) {
            name = openedFile.getName();
            if (name.endsWith(".html")) {
                name = name.substring(0, name.length() - 5);
            }
        }
        if (editor.isModified()) {
            name = name + "*";
        }
        final String theName = name;
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                noteNameLabel.setText(theName);
            }
        });
    }

    public void exit() {
        boolean shouldCancel = saveCurrentIfNeeded("Quit");
        if (shouldCancel) return;
        System.exit(0);
    }
}
