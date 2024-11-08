import java.awt.*;
import java.awt.event.*;
import java.io.PrintStream;
import java.util.*;

public class RetroXML extends Frame implements XMLTextListener {

    class SymWindow extends WindowAdapter {

        public void windowDeactivated(WindowEvent windowevent) {
        }

        public void windowClosing(WindowEvent windowevent) {
            Object obj = windowevent.getSource();
            if (obj == RetroXML.this) RetroXML_WindowClosing(windowevent);
        }

        SymWindow() {
        }
    }

    class TagReference {

        String tagName;

        Hashtable attributes;

        int tagStart;

        int tagEnd;

        TagReference() {
            tagName = "";
            tagStart = -1;
            tagEnd = -1;
        }
    }

    class SymCode implements ActionListener {

        public void actionPerformed(ActionEvent actionevent) {
            Object obj = actionevent.getSource();
            String s = ((MenuItem) obj).getLabel();
            menuCoding(s);
        }

        SymCode() {
        }
    }

    class SymStructure implements ActionListener {

        public void actionPerformed(ActionEvent actionevent) {
            Object obj = actionevent.getSource();
            String s = ((MenuItem) obj).getLabel();
            menuStructure(s);
        }

        SymStructure() {
        }
    }

    class SymAction implements ActionListener {

        public void actionPerformed(ActionEvent actionevent) {
            Object obj = actionevent.getSource();
            if (obj == miOpen) {
                miOpen_Action(actionevent);
                return;
            }
            if (obj == miAbout) {
                miAbout_Action(actionevent);
                return;
            }
            if (obj == miExit) {
                miExit_Action(actionevent);
                return;
            }
            if (obj == miCopy) {
                miCopy_ActionPerformed(actionevent);
                return;
            }
            if (obj == miCut) {
                miCut_ActionPerformed(actionevent);
                return;
            }
            if (obj == miPaste) {
                miPaste_ActionPerformed(actionevent);
                return;
            }
            if (obj == miSave) {
                miSave_ActionPerformed(actionevent);
                return;
            }
            if (obj == miSaveAs) {
                miSaveAs_ActionPerformed(actionevent);
                return;
            }
            if (obj == miNew) {
                miNew_ActionPerformed(actionevent);
                return;
            }
            if (obj == appendNewNotemenuItem) {
                appendNewNotemenuItem_ActionPerformed(actionevent);
                return;
            }
            if (obj == insertNewNotemenuItem) {
                insertNewNotemenuItem_ActionPerformed(actionevent);
                return;
            }
            if (obj == undoMenuItem) {
                undoMenuItem_ActionPerformed(actionevent);
                return;
            }
            if (obj == unwrapMenuItem) {
                unwrapMenuItem_ActionPerformed(actionevent);
                return;
            }
            if (obj == deTagMenuItem) {
                deTagMenuItem_ActionPerformed(actionevent);
                return;
            }
            if (obj == miSelectContext) {
                miSelectContext_ActionPerformed(actionevent);
                return;
            }
            if (obj == miSelectAll) {
                miSelectAll_ActionPerformed(actionevent);
                return;
            }
            if (obj == transfertomenuItem1) {
                transfertomenuItem1_ActionPerformed(actionevent);
                return;
            }
            if (obj == transferfrommenuItem2) transferfrommenuItem2_ActionPerformed(actionevent);
        }

        SymAction() {
        }
    }

    class SymMouse extends MouseAdapter {

        public void mouseEntered(MouseEvent mouseevent) {
            mouseevent.getSource();
        }

        public void mouseExited(MouseEvent mouseevent) {
            mouseevent.getSource();
        }

        public void mouseClicked(MouseEvent mouseevent) {
            mouseevent.getSource();
        }

        public void mouseReleased(MouseEvent mouseevent) {
            Object obj = mouseevent.getSource();
            if (obj == rightarrowButton) {
                rightarrowButton_MouseReleased(mouseevent);
                return;
            }
            if (obj == leftarrowButton) leftarrowButton_MouseReleased(mouseevent);
        }

        public void mousePressed(MouseEvent mouseevent) {
            Object obj = mouseevent.getSource();
            if (obj == RetroXML.this) doPopup_MousePressed(mouseevent);
        }

        SymMouse() {
        }
    }

    class SymKey extends KeyAdapter {

        public void keyTyped(KeyEvent keyevent) {
            if (keyevent.getKeyChar() >= ' ' && keyevent.getKeyChar() <= '}') dirty = true;
        }

        public void keyReleased(KeyEvent keyevent) {
            if (keyevent.getKeyChar() >= ' ' && keyevent.getKeyChar() <= '}') setDirty(true);
        }

        public void keyPressed(KeyEvent keyevent) {
            if (keyevent.getKeyChar() >= ' ' && keyevent.getKeyChar() <= '}') setDirty(true);
        }

        SymKey() {
        }
    }

    class SymComponent extends ComponentAdapter {

        public void componentResized(ComponentEvent componentevent) {
            Object obj = componentevent.getSource();
            if (obj == RetroXML.this) RetroXML_ComponentResized(componentevent);
        }

        SymComponent() {
        }
    }

    public RetroXML() {
        theDtd = "contentcodes.dtd";
        topElement = "Fieldnotes";
        fComponentsAdjusted = false;
        fp = new FragmentParser();
        currentNote = -1;
        baseSize = new Dimension(594, 430);
        lastSize = baseSize;
        cap = new CutAndPaste();
        dirty = false;
        fileDirty = false;
        undo = new Undo();
        setLayout(null);
        setVisible(false);
        setSize(594, 430);
        legalElementsArea = new TextArea("", 0, 0, 3);
        legalElementsArea.setBounds(4, 384, 433, 41);
        legalElementsArea.setFont(new Font("SansSerif", 0, 10));
        add(legalElementsArea);
        openFileDialog1 = new FileDialog(this);
        openFileDialog1.setMode(0);
        openFileDialog1.setTitle("Open");
        arrowPanel = new Panel();
        arrowPanel.setLayout(null);
        arrowPanel.setBounds(462, 408, 110, 22);
        add(arrowPanel);
        rightarrowButton = new ArrowButton();
        rightarrowButton.setBounds(60, 0, 50, 22);
        arrowPanel.add(rightarrowButton);
        leftarrowButton = new ArrowButton();
        leftarrowButton.setBounds(0, 1, 52, 21);
        arrowPanel.add(leftarrowButton);
        attributePanel = new AttributePanel();
        attributePanel.setLayout(null);
        attributePanel.setBounds(441, 29, 150, 387);
        add(attributePanel);
        noteText = new XMLTextArea();
        noteText.setText("Note");
        noteText.setBounds(3, 6, 435, 377);
        add(noteText);
        setTitle("Retro CSAC Fieldnote Editor");
        mainMenuBar = new MenuBar();
        filemenu1 = new Menu("File");
        miNew = new MenuItem("New Notes");
        miNew.setShortcut(new MenuShortcut(78, false));
        filemenu1.add(miNew);
        miOpen = new MenuItem("Open Notes...");
        miOpen.setShortcut(new MenuShortcut(79, false));
        filemenu1.add(miOpen);
        miSave = new MenuItem("Save Notes");
        miSave.setShortcut(new MenuShortcut(83, false));
        filemenu1.add(miSave);
        miSaveAs = new MenuItem("Save Notes As...");
        filemenu1.add(miSaveAs);
        filemenu1.addSeparator();
        appendNewNotemenuItem = new MenuItem("Append New Note");
        appendNewNotemenuItem.setShortcut(new MenuShortcut(69, false));
        filemenu1.add(appendNewNotemenuItem);
        insertNewNotemenuItem = new MenuItem("Insert New Note");
        insertNewNotemenuItem.setShortcut(new MenuShortcut(73, false));
        filemenu1.add(insertNewNotemenuItem);
        filemenu1.addSeparator();
        miExit = new MenuItem("Quit");
        miExit.setShortcut(new MenuShortcut(81, false));
        filemenu1.add(miExit);
        mainMenuBar.add(filemenu1);
        editmenu2 = new Menu("Edit");
        undoMenuItem = new MenuItem("Undo");
        undoMenuItem.setShortcut(new MenuShortcut(90, false));
        editmenu2.add(undoMenuItem);
        editmenu2.addSeparator();
        miCut = new MenuItem("Cut");
        miCut.setShortcut(new MenuShortcut(88, false));
        editmenu2.add(miCut);
        miCopy = new MenuItem("Copy");
        miCopy.setShortcut(new MenuShortcut(67, false));
        editmenu2.add(miCopy);
        miPaste = new MenuItem("Paste");
        miPaste.setShortcut(new MenuShortcut(86, false));
        editmenu2.add(miPaste);
        miClear = new MenuItem("Clear");
        editmenu2.add(miClear);
        editmenu2.addSeparator();
        unwrapMenuItem = new MenuItem("Unwrap");
        unwrapMenuItem.setShortcut(new MenuShortcut(85, false));
        editmenu2.add(unwrapMenuItem);
        deTagMenuItem = new MenuItem("De-Tag Selection");
        deTagMenuItem.setShortcut(new MenuShortcut(89, false));
        editmenu2.add(deTagMenuItem);
        editmenu2.addSeparator();
        miSelectContext = new MenuItem("Select Context");
        miSelectContext.setShortcut(new MenuShortcut(75, false));
        editmenu2.add(miSelectContext);
        miSelectAll = new MenuItem("Select All");
        miSelectAll.setShortcut(new MenuShortcut(65, false));
        editmenu2.add(miSelectAll);
        mainMenuBar.add(editmenu2);
        menu1 = new Menu("Transfer");
        transfertomenuItem1 = new MenuItem("Transfer to Clipboard");
        transfertomenuItem1.setShortcut(new MenuShortcut(84, false));
        menu1.add(transfertomenuItem1);
        transferfrommenuItem2 = new MenuItem("Transfer from Clipboard");
        transferfrommenuItem2.setShortcut(new MenuShortcut(70, false));
        menu1.add(transferfrommenuItem2);
        mainMenuBar.add(menu1);
        helpmenu3 = new Menu("Help");
        mainMenuBar.setHelpMenu(helpmenu3);
        miAbout = new MenuItem("About..");
        helpmenu3.add(miAbout);
        mainMenuBar.add(helpmenu3);
        setMenuBar(mainMenuBar);
        leftarrowButton.setArrowDirection("left");
        SymWindow symwindow = new SymWindow();
        addWindowListener(symwindow);
        SymAction symaction = new SymAction();
        miOpen.addActionListener(symaction);
        miAbout.addActionListener(symaction);
        miExit.addActionListener(symaction);
        SymMouse symmouse = new SymMouse();
        addMouseListener(symmouse);
        SymKey symkey = new SymKey();
        noteText.addKeyListener(symkey);
        rightarrowButton.addMouseListener(symmouse);
        leftarrowButton.addMouseListener(symmouse);
        SymComponent symcomponent = new SymComponent();
        addComponentListener(symcomponent);
        miCopy.addActionListener(symaction);
        miCut.addActionListener(symaction);
        miPaste.addActionListener(symaction);
        miSave.addActionListener(symaction);
        miSaveAs.addActionListener(symaction);
        miNew.addActionListener(symaction);
        appendNewNotemenuItem.addActionListener(symaction);
        insertNewNotemenuItem.addActionListener(symaction);
        attributePanel.addMouseListener(symmouse);
        undoMenuItem.addActionListener(symaction);
        unwrapMenuItem.addActionListener(symaction);
        deTagMenuItem.addActionListener(symaction);
        miSelectContext.addActionListener(symaction);
        miSelectAll.addActionListener(symaction);
        transfertomenuItem1.addActionListener(symaction);
        transferfrommenuItem2.addActionListener(symaction);
        noteText.addXMLTextListener(this);
        noteText.addXMLAttributeListener(attributePanel);
        noteText.setLabelTarget("Note");
        prefs = (new PrefsHandler()).readPreferences();
        ContentCodesHandler contentcodeshandler = new ContentCodesHandler();
        if (prefs.getPref("optionsdtd") != null) {
            try {
                contentcodeshandler.readDTD(prefs.getPref("optionsdtd"));
                optionMenus(contentcodeshandler);
            } catch (Exception _ex) {
            }
            theDtd = prefs.getPref("optionsdtd");
        } else {
            try {
                contentcodeshandler.readDTD(prefs.getPref("contentcodes.dtd"));
                optionMenus(contentcodeshandler);
            } catch (Exception _ex) {
            }
            theDtd = "contentcodes.dtd";
        }
        miNew_ActionPerformed(null);
        StringVector stringvector;
        if ((stringvector = (StringVector) contentcodeshandler.menucontents.get("top")) != null) topElement = (String) stringvector.elementAt(0);
    }

    public RetroXML(String s) {
        this();
        setTitle(s);
    }

    public void xmlTextValueChanged(XMLTextEvent xmltextevent) {
    }

    public void xmlTag(String s, int i, int j) {
    }

    public void clearXmlAttributes() {
    }

    public void resetXmlAttributes(XMLTextArea xmltextarea, AttributeModel attributemodel) {
    }

    public void addXmlAttribute() {
    }

    public void optionMenus(ContentCodesHandler contentcodeshandler) {
        Menu menu = new Menu("Coding", true);
        coding = new PopupMenu("Coding");
        add(coding);
        Menu menu3 = null;
        SymStructure symstructure = null;
        char c = 'A';
        char c1 = '1';
        char c2 = '5';
        SymCode symcode = new SymCode();
        contentcodeshandler.menus.reset();
        Menu menu2;
        for (; contentcodeshandler.menus.isNext(); menu.add(menu2)) {
            String s = contentcodeshandler.menus.getNextString();
            menu2 = new Menu(s);
            if (s.equals("Structures")) {
                menu3 = new Menu(s);
                symstructure = new SymStructure();
            }
            StringVector stringvector = (StringVector) contentcodeshandler.menucontents.get(s);
            stringvector.reset();
            MenuItem menuitem1;
            for (; stringvector.isNext(); coding.add(menuitem1)) {
                String s1 = stringvector.getNextString();
                MenuItem menuitem = new MenuItem(s1);
                if (s.equals("ContentCodes")) menuitem.setShortcut(new MenuShortcut(c++, true));
                if (s.equals("Security")) menuitem.setShortcut(new MenuShortcut(c1++, false));
                menu2.add(menuitem);
                menuitem1 = new MenuItem(s1);
                if (s.equals("Structures")) {
                    MenuItem menuitem2 = new MenuItem(s1);
                    menuitem.setShortcut(new MenuShortcut(c2++, false));
                    if (c2 > '9') c2 = '0';
                    menu3.add(menuitem2);
                    menuitem2.addActionListener(symstructure);
                }
                menuitem.addActionListener(symcode);
                menuitem1.addActionListener(symcode);
            }
        }
        mainMenuBar.add(menu);
        if (menu3 != null) mainMenuBar.add(menu3);
    }

    public void setVisible(boolean flag) {
        if (flag) setLocation(50, 50);
        super.setVisible(flag);
    }

    public static void main(String args[]) {
        (new RetroXML()).setVisible(true);
    }

    public void addNotify() {
        Dimension dimension = getSize();
        super.addNotify();
        if (fComponentsAdjusted) return;
        setSize(insets().left + insets().right + dimension.width, insets().top + insets().bottom + dimension.height);
        Component acomponent[] = getComponents();
        for (int i = 0; i < acomponent.length; i++) {
            Point point = acomponent[i].getLocation();
            point.translate(insets().left, insets().top);
            acomponent[i].setLocation(point);
        }
        fComponentsAdjusted = true;
    }

    void populateFieldnote(FieldnoteTemplate fieldnotetemplate) {
        noteText.setText(fieldnotetemplate.getNote());
    }

    void populateFieldnoteTemplate(FieldnoteTemplate fieldnotetemplate) {
        fieldnotetemplate.setNote(noteText.getText());
    }

    public void elementOptions(String s) {
        legalElementsArea.setText(s.toString());
    }

    void RetroXML_WindowClosing(WindowEvent windowevent) {
        if (dirty) saveNote();
        if (fileDirty) {
            (new QuitRetroDialog(this, true)).setVisible(true);
            return;
        } else {
            setVisible(false);
            dispose();
            System.exit(0);
            return;
        }
    }

    public void menuCoding(String s) {
        Component component = getFocusOwner();
        if (component instanceof TextArea) {
            setUndo((TextComponent) component);
            TextArea textarea = (TextArea) component;
            if (textarea == null) return;
            ElementDef elementdef = ElementManager.findExistingElement(s);
            if (elementdef == null) return;
            int i = textarea.getSelectionStart();
            int j = textarea.getSelectionEnd();
            if (i == j) {
                setUndo(textarea);
                textarea.insert("<" + s + "></" + s + ">", i);
                textarea.setCaretPosition(i + 2 + s.length());
                if (textarea instanceof XMLTextArea) ((XMLTextArea) textarea).setAttributes();
                setDirty(true);
                return;
            }
            String s1 = "<" + s + ">" + textarea.getSelectedText() + "</" + s + ">";
            if (fp.parseFragment(s1)) {
                setUndo(textarea);
                textarea.replaceText(s1, i, j);
                textarea.select(i, i + s1.length());
                setDirty(true);
                if (textarea instanceof XMLTextArea) ((XMLTextArea) textarea).setAttributes();
            }
        }
    }

    public void menuStructure(String s) {
        Component component = getFocusOwner();
        if (component instanceof TextArea) {
            setUndo((TextComponent) component);
            TextArea textarea = (TextArea) component;
            ElementDef elementdef = ElementManager.findExistingElement(s);
            if (elementdef == null) {
                System.out.println("No element defintion for " + s + " in RetroXML.menuCoding");
                return;
            }
            textarea.getSelectionStart();
            textarea.getSelectionEnd();
            ElementModel elementmodel = elementdef.getContentModel();
            String s1 = elementmodel.getStructure(s);
            textarea.insert(s1, textarea.getCaretPosition());
        }
    }

    public TagReference findTagText(TextArea textarea) {
        String s = textarea.getSelectedText();
        TagReference tagreference = new TagReference();
        String s1 = "";
        int i = s.indexOf("<");
        if (i != -1) {
            int j = s.indexOf(">", i);
            if (j != -1) {
                int k = s.indexOf(" ", i + 1);
                if (k == -1 || k > j) k = j;
                String s2 = s.substring(i + 1, k);
                tagreference.tagName = s2;
                tagreference.tagStart = i;
                tagreference.tagEnd = j;
            }
        }
        return tagreference;
    }

    public TagReference findTagEnclosure(TextArea textarea, int i) {
        String s = textarea.getText().substring(0, i);
        String s1 = textarea.getText();
        TagReference tagreference = new TagReference();
        String s2 = "";
        int j = s.lastIndexOf("<");
        int k = s.lastIndexOf(">");
        if (j > k) {
            int l = s1.indexOf(">", j);
            if (l != -1) {
                int i1 = s1.indexOf(" ", j + 1);
                if (i1 == -1 || i1 > l) i1 = l;
                String s3 = s1.substring(j + 1, i1);
                tagreference.tagName = s3;
                tagreference.tagStart = j;
                tagreference.tagEnd = l;
            }
        }
        return tagreference;
    }

    void miAbout_Action(ActionEvent actionevent) {
        (new AboutDialog(this, true)).setVisible(true);
    }

    void miExit_Action(ActionEvent actionevent) {
        if (dirty) saveNote();
        (new QuitRetroDialog(this, true)).setVisible(true);
    }

    void miOpen_Action(ActionEvent actionevent) {
        if (dirty) saveNote();
        if (fileDirty) {
            (new WantToSaveDialog(this, true)).setVisible(true);
            if (fileDirty) return;
        }
        try {
            XFile xfile = new XFile();
            if (!xfile.Choose(0)) return;
            xfile.Open(0);
            StringBuffer stringbuffer = new StringBuffer();
            String s;
            while ((s = xfile.ReadLine()) != null) stringbuffer.append(s + XMLIndent.Eol);
            noteText.setText(stringbuffer.toString());
            xfile.Close();
            return;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    void noteText_MousePressed(MouseEvent mouseevent) {
        if (noteText.getText().equals("Note")) noteText.setText("");
        oldCaretPos = noteText.getCaretPosition();
    }

    void noteText_MouseClicked(MouseEvent mouseevent) {
        int i = noteText.getSelectionStart();
        TagReference tagreference = findTagEnclosure(noteText, i);
        if (tagreference.tagEnd != -1) {
            if (tagreference.tagName.startsWith("/")) {
                noteText.setCaretPosition(tagreference.tagStart);
                return;
            }
            noteText.setCaretPosition(tagreference.tagEnd + 1);
        }
    }

    void noteText_MouseReleased(MouseEvent mouseevent) {
        int i;
        int j;
        if ((i = noteText.getSelectionStart()) != (j = noteText.getSelectionEnd())) {
            if (!fp.parseFragment(noteText.getSelectedText())) {
                noteText.select(oldCaretPos, oldCaretPos);
                return;
            }
            TagReference tagreference = findTagEnclosure(noteText, i);
            if (i > tagreference.tagStart && j < tagreference.tagEnd) {
                noteText.select(oldCaretPos, oldCaretPos);
                return;
            }
            findTagText(noteText);
        }
    }

    void noteText_KeyPressed(KeyEvent keyevent) {
        if (keyevent.getKeyCode() == 8) {
            int i = noteText.getSelectionStart();
            if (i > 0 && noteText.getText().charAt(i - 1) == '>') noteText.insert(" ", i);
        }
    }

    void rightarrowButton_MouseReleased(MouseEvent mouseevent) {
    }

    void leftarrowButton_MouseReleased(MouseEvent mouseevent) {
    }

    public void saveNote() {
        if (dirty) {
            fileDirty = true;
            setDirty(false);
        }
    }

    public void appendNewNote() {
    }

    public void insertNewNote(int i) {
    }

    void RetroXML_ComponentResized(ComponentEvent componentevent) {
        Dimension dimension = getSize();
        Rectangle rectangle = attributePanel.getBounds();
        Rectangle rectangle1 = arrowPanel.getBounds();
        Rectangle rectangle2 = legalElementsArea.getBounds();
        Rectangle rectangle3 = noteText.getBounds();
        int i = dimension.width - lastSize.width;
        int j = dimension.height - lastSize.height;
        rectangle.translate(i, 0);
        rectangle1.translate(i, j);
        rectangle3.width += i;
        rectangle3.height += j;
        rectangle2.translate(0, j);
        attributePanel.setBounds(rectangle);
        arrowPanel.setBounds(rectangle1);
        legalElementsArea.setBounds(rectangle2);
        noteText.setBounds(rectangle3);
        lastSize = dimension;
    }

    /**
     * @deprecated Method minimumSize is deprecated
     */
    public Dimension minimumSize() {
        return getMinimumSize();
    }

    public Dimension getMinimumSize() {
        return baseSize;
    }

    void miCopy_ActionPerformed(ActionEvent actionevent) {
        Component component = getFocusOwner();
        if (component instanceof TextComponent) cap.copyText((TextComponent) component);
    }

    void miCut_ActionPerformed(ActionEvent actionevent) {
        Component component = getFocusOwner();
        if (component instanceof TextComponent) cap.cutText((TextComponent) component);
    }

    void miPaste_ActionPerformed(ActionEvent actionevent) {
        Component component = getFocusOwner();
        if (component instanceof TextComponent) cap.pasteText((TextComponent) component);
    }

    void copyToClipboard() {
        Component component = getFocusOwner();
        if (component instanceof TextComponent) cap.transferToClipboard((TextComponent) component);
    }

    void copyFromClipboard() {
        Component component = getFocusOwner();
        if (component instanceof TextComponent) cap.transferFromClipboard((TextComponent) component);
    }

    boolean miSave_ActionPerformed(ActionEvent actionevent) {
        if (dirty) saveNote();
        if (!fileDirty) return true;
        if (saveFile == null) return miSaveAs_ActionPerformed(null);
        try {
            saveFile.Open(1);
            String s = noteText.getText();
            if (!s.startsWith("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>")) saveFile.WriteString("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" + XMLIndent.Eol + "<!DOCTYPE " + topElement + " SYSTEM \"" + theDtd + "\">" + XMLIndent.Eol + XMLIndent.Eol);
            saveFile.WriteString(s);
            saveFile.Close();
            setDirty(false);
            fileDirty = false;
        } catch (Exception exception) {
            exception.printStackTrace();
            System.out.println("Couldn't save! " + exception.getMessage());
            return false;
        }
        return true;
    }

    boolean miSaveAs_ActionPerformed(ActionEvent actionevent) {
        if (dirty) saveNote();
        fileDirty = true;
        XFile xfile = new XFile();
        if (xfile.Choose(1)) {
            saveFile = xfile;
        } else {
            dirty = false;
            fileDirty = false;
            return false;
        }
        return miSave_ActionPerformed(null);
    }

    void miNew_ActionPerformed(ActionEvent actionevent) {
        if (dirty) saveNote();
        if (fileDirty) {
            (new WantToSaveDialog(this, true)).setVisible(true);
            if (fileDirty) return;
        }
        saveFile = null;
        noteText.setText("");
        setDirty(false);
        fileDirty = false;
    }

    void setDirty(boolean flag) {
        dirty = flag;
        miSave.setEnabled(flag);
    }

    void appendNewNotemenuItem_ActionPerformed(ActionEvent actionevent) {
        appendNewNote();
    }

    void insertNewNotemenuItem_ActionPerformed(ActionEvent actionevent) {
        insertNewNote(currentNote);
    }

    void noteText_MouseEntered(MouseEvent mouseevent) {
    }

    void noteText_MouseExited(MouseEvent mouseevent) {
    }

    void doPopup_MousePressed(MouseEvent mouseevent) {
        doPopup(this, mouseevent);
    }

    void attributePanel_MousePressed(MouseEvent mouseevent) {
        doPopup(attributePanel, mouseevent);
    }

    public void doPopup(Component component, MouseEvent mouseevent) {
        coding.show(component, mouseevent.getX(), mouseevent.getY());
    }

    public void setUndo(TextComponent textcomponent) {
        undo.setContext(textcomponent);
        undoMenuItem.setEnabled(true);
        undoMenuItem.setLabel("Undo");
    }

    public void doUndo() {
        Undo undo1 = new Undo(undo.comp);
        undo.undo();
        undoMenuItem.setEnabled(true);
        undoMenuItem.setLabel("Redo");
        undo = undo1;
    }

    void undoMenuItem_ActionPerformed(ActionEvent actionevent) {
        doUndo();
    }

    void unwrapMenuItem_ActionPerformed(ActionEvent actionevent) {
        Component component = getFocusOwner();
        if (component instanceof XMLTextArea) {
            setUndo((TextComponent) component);
            ((XMLTextArea) component).unWrap();
        }
    }

    void deTagMenuItem_ActionPerformed(ActionEvent actionevent) {
        Component component = getFocusOwner();
        if (component instanceof XMLTextArea) setUndo((TextComponent) component);
    }

    void miSelectContext_ActionPerformed(ActionEvent actionevent) {
        Component component = getFocusOwner();
        if (component instanceof XMLTextArea) {
            ((XMLTextArea) component).selectContext();
            return;
        }
        if (component instanceof TextComponent) {
            setUndo((TextComponent) component);
            ((TextComponent) component).select(0, ((TextComponent) component).getText().length());
        }
    }

    void miSelectAll_ActionPerformed(ActionEvent actionevent) {
        Component component = getFocusOwner();
        if (component instanceof TextComponent) {
            setUndo((TextComponent) component);
            ((TextComponent) component).select(0, ((TextComponent) component).getText().length());
        }
    }

    void transfertomenuItem1_ActionPerformed(ActionEvent actionevent) {
        copyToClipboard();
    }

    void transferfrommenuItem2_ActionPerformed(ActionEvent actionevent) {
        copyFromClipboard();
    }

    String theDtd;

    PopupMenu coding;

    String topElement;

    boolean fComponentsAdjusted;

    TextArea legalElementsArea;

    FileDialog openFileDialog1;

    Panel arrowPanel;

    ArrowButton rightarrowButton;

    ArrowButton leftarrowButton;

    AttributePanel attributePanel;

    XMLTextArea noteText;

    MenuBar mainMenuBar;

    Menu filemenu1;

    MenuItem miNew;

    MenuItem miOpen;

    MenuItem miSave;

    MenuItem miSaveAs;

    MenuItem appendNewNotemenuItem;

    MenuItem insertNewNotemenuItem;

    MenuItem miExit;

    Menu editmenu2;

    MenuItem undoMenuItem;

    MenuItem miCut;

    MenuItem miCopy;

    MenuItem miPaste;

    MenuItem miClear;

    MenuItem unwrapMenuItem;

    MenuItem deTagMenuItem;

    MenuItem miSelectContext;

    MenuItem miSelectAll;

    Menu menu1;

    MenuItem transfertomenuItem1;

    MenuItem transferfrommenuItem2;

    Menu helpmenu3;

    MenuItem miAbout;

    FragmentParser fp;

    int oldCaretPos;

    Preferences prefs;

    int currentNote;

    Dimension baseSize;

    Dimension lastSize;

    CutAndPaste cap;

    XFile saveFile;

    boolean dirty;

    boolean fileDirty;

    protected Undo undo;
}
