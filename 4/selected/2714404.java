package org.es.uma.XMLEditor.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import org.es.uma.DpdManager.DpdParser;
import org.es.uma.DpdManager.VersionsSetup;
import org.es.uma.XMLEditor.specific.Pool;
import org.es.uma.XMLEditor.specific.XmlFile;
import org.es.uma.XMLEditor.specific.XmlFilesManager;
import org.es.uma.XMLEditor.transfer.CutAndPaste;
import org.es.uma.XMLEditor.xerces.DocumentImpl;
import org.es.uma.XMLEditor.xerces.TreeElement;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import SDClient.SDMainFrame;

/**
 * This class shows a DOM Document in a JTree, and presents controls which allow
 * the user to create and view the progress of a NodeIterator in the DOM tree.
 */
public class TreeView extends JInternalFrame implements MessageDisplayer, TreeSelectionListener {

    private static final long serialVersionUID = 2419355924098723404L;

    public boolean isRemote;

    public String remoteName;

    public String remoteDomain;

    public String remoteVersion;

    public String artefactType;

    /**
	 * The document being viewed
	 */
    private DocumentImpl document;

    /**
	 * a pool of files used by this tree. The pool keeps for each file used the
	 * number of uses. This number can be zero in case every reference pointed
	 * to the file was closed and the file is not the root one. It is however
	 * kept into the pool until next close or save command
	 */
    private Pool fileList;

    /**
	 * A text area to display messages
	 */
    private JTextArea messageText;

    /**
	 * EditorPane to edit the content of the selected node
	 */
    private EditorPane editorPane;

    /**
	 * The Xml tree
	 */
    public XmlTree jTree;

    /**
	 * The Cut and Paste facilities
	 */
    private CutAndPaste cutAndPaste;

    /**
	 * a pointer on the XmlEditor that uses this TreeView
	 */
    private SDMainFrame editor;

    /**
	 * this boolean says whether the comment nodes should be displayed or not
	 */
    private boolean displayComments = false;

    /**
	 * this boolean says whether the node values are to be displayed like nodes
	 * or like attributes of their parent node
	 */
    private boolean displayValueAsAttribute = true;

    private JProgressBar jProgressBar1 = new JProgressBar();

    public TextView textView;

    /**
	 * creates a TreeView for the given fileName
	 * 
	 * @param fileName
	 *            the name of the Xml file to be viewed
	 * @param editor
	 *            a pointer on the underlying editor
	 */
    public TreeView(String fileName, SDMainFrame editor, boolean template, boolean remote) {
        super(fileName, true, true, true, true);
        this.editor = editor;
        isRemote = remote;
        buildsAPI();
        XmlFilesManager xfm = editor.getXmlFilesManager();
        XmlFile file = null;
        if (template) file = xfm.getXmlFileByTemplate(fileName, this); else file = xfm.getXmlFile(fileName, this);
        if (file == null) {
            xfm.closableFile(file);
            xfm.closeFile(file);
            return;
        }
        fileList = new Pool();
        fileList.addRef(file);
        if (template) {
            String newFileName = editor.getXmlFilesManager().getNewFileName();
            this.setTitle(newFileName);
            if (textView != null) {
                textView.setSubTitle(newFileName);
            }
        }
        document = file.getDocument();
        jTree.getXmlModel().setRootNode(document);
        String xml = "";
        try {
            xml = document.getXmlFile().getContent();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.setCommentDisplayed(editor.commentsCheckBoxMenuItem.isSelected());
    }

    /**
	 * creates a TreeView for the XmlFile given
	 * 
	 * @param docType
	 *            the name of the document type
	 * @param dtd
	 *            the file containing the dtd
	 * @param editor
	 *            a pointer on the underlying editor
	 */
    public TreeView(String docType, File dtd, SDMainFrame editor) {
        super(editor.getXmlFilesManager().getNewFileName(), true, true, true, true);
        this.editor = editor;
        buildsAPI();
        XmlFilesManager xfm = editor.getXmlFilesManager();
        XmlFile file = xfm.getNewFile(docType, dtd, this);
        fileList = new Pool();
        fileList.addRef(file);
        document = file.getDocument();
        jTree.getXmlModel().setRootNode(document);
        this.setCommentDisplayed(editor.commentsCheckBoxMenuItem.isSelected());
    }

    /**
	 * builds the TreeView API
	 */
    private void buildsAPI() {
        setSize(800, 600);
        if (isRemote) addRemoteFrameListener(); else addLocalFrameListener();
        addInternalFrameListener(new FrameAdapter());
        jTree = new XmlTree(this, this);
        JScrollPane treeScroll = new JScrollPane(jTree);
        treeScroll.setMinimumSize(new Dimension(0, 0));
        treeScroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Tree View"), BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        editorPane = new EditorPane(this);
        jTree.addTreeSelectionListener(this);
        JScrollPane editorScroll = new JScrollPane(editorPane);
        editorScroll.setMinimumSize(new Dimension(0, 0));
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, editorScroll);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(375);
        splitPane.setDividerSize(10);
        messageText = new JTextArea();
        messageText.setEditable(false);
        JPanel messagePanel = new JPanel(new BorderLayout());
        JScrollPane messageScroll = new JScrollPane(messageText);
        messagePanel.add(messageScroll);
        messagePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Messages"), BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPane, messagePanel);
        splitPane2.setContinuousLayout(true);
        splitPane2.setDividerLocation(0.75);
        splitPane2.setOneTouchExpandable(true);
        splitPane2.setDividerSize(10);
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(splitPane2, BorderLayout.CENTER);
        getContentPane().add(mainPanel);
        cutAndPaste = new CutAndPaste(jTree, this);
    }

    /**
	 * Add remote artefact frame listener
	 * 
	 */
    private void addRemoteFrameListener() {
        addInternalFrameListener(new InternalFrameAdapter() {

            public void internalFrameDeactivated(InternalFrameEvent evt) {
                rootInternalFrameDeactivated(evt);
            }

            public void internalFrameActivated(InternalFrameEvent evt) {
                rootInternalFrameActivated(evt);
            }
        });
    }

    /**
	 * Add local artefact frame listener
	 * 
	 */
    private void addLocalFrameListener() {
        addInternalFrameListener(new InternalFrameAdapter() {

            public void internalFrameDeactivated(InternalFrameEvent evt) {
                rootLocalInternalFrameDeactivated(evt);
            }

            public void internalFrameActivated(InternalFrameEvent evt) {
                rootLocalInternalFrameActivated(evt);
            }
        });
    }

    protected void rootLocalInternalFrameActivated(InternalFrameEvent evt) {
        getEditor().localTreeViewActivated();
    }

    protected void rootLocalInternalFrameDeactivated(InternalFrameEvent evt) {
        getEditor().localTreeViewDeactivated();
    }

    protected void rootInternalFrameActivated(InternalFrameEvent evt) {
        getEditor().remoteTreeViewActivated();
    }

    protected void rootInternalFrameDeactivated(InternalFrameEvent evt) {
        getEditor().remoteTreeViewDeactivated();
    }

    /**
	 * displays a message on the message window of this TreeView
	 * 
	 * @param message
	 *            the message to be displayed
	 */
    public void displayMessage(String message) {
        messageText.append(message + "\n");
    }

    /**
	 * pops up a message in this TreeView
	 * 
	 * @param message
	 *            the message to be displayed
	 */
    public void popupMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    /**
	 * returns the XmlTree of this TreeView
	 * 
	 * @return the XmlTree
	 */
    public XmlTree getXmlTree() {
        return jTree;
    }

    /**
	 * returns the CutAndPaste of this TreeView
	 * 
	 * @return the CutAndPaste
	 */
    public CutAndPaste getCutAndPaste() {
        return cutAndPaste;
    }

    /**
	 * changes this TreeView due to a change in the file displayed The document
	 * of the new file is supposed to be identical to the previous one
	 * 
	 * @param file
	 *            the new file to display
	 */
    public void topFileChanged(XmlFile file) {
        String absolutePath = file.getAbsolutePath();
        this.setTitle(absolutePath);
        if (textView != null) {
            textView.setSubTitle(absolutePath);
        }
        document = file.getDocument();
        ((XmlTreeNode) jTree.getXmlModel().getRoot()).setNode(document);
        redisplayTree(false);
    }

    /**
	 * erases the editor pane
	 */
    public void eraseEditorPane() {
        this.editorPane.displayNode(null);
    }

    /**
	 * redisplays the editor Pane
	 */
    public void redisplayEditorPane() {
        this.editorPane.redisplay();
    }

    /**
	 * redisplays the tree
	 */
    public void redisplayTree(boolean b) {
        this.getXmlTree().getXmlModel().redisplayTree(b);
        if (textView != null) textView.actualize();
    }

    /**
	 * redisplays both the tree and the editor pane
	 */
    public void redisplayAll(boolean b) {
        this.redisplayEditorPane();
        this.redisplayTree(b);
    }

    /**
	 * gets the XmlEditor that displays this TreeView
	 * 
	 * @return Editor - The Editor
	 */
    public SDMainFrame getEditor() {
        return editor;
    }

    /**
	 * gets the EditorPane from this TreeView
	 * 
	 * @return EditorPane - The EditorPane
	 */
    public EditorPane getEditorPane() {
        return editorPane;
    }

    /**
	 * says whether values are displayed as attributes
	 * 
	 * @return true if values are displayed as attributes
	 */
    public boolean isValueDisplayedAsAttribute() {
        return displayValueAsAttribute;
    }

    /**
	 * sets the display of values. If true, they will be displayed as attributes
	 * 
	 * @param vAsA
	 *            true to display values as attributes
	 */
    public void setValueDisplayedAsAttribute(boolean vAsA) {
        displayValueAsAttribute = vAsA;
    }

    /**
	 * says whether comments are displayed
	 * 
	 * @return true if comments are displayed
	 */
    public boolean isCommentDisplayed() {
        return displayComments;
    }

    /**
	 * sets the display of comments.
	 * 
	 * @param display
	 *            true to display comments.
	 */
    public void setCommentDisplayed(boolean display) {
        displayComments = display;
    }

    /**
	 * closes the XmlTree displayed here after making sure that all files have
	 * been saved or have been canceled intentionnaly
	 * 
	 * @return if false, the exit operation has to be canceled
	 */
    protected boolean exit() {
        int result = this.checkSave(true);
        if (result == -2) {
            return false;
        }
        Enumeration enume = fileList.getPoolContent();
        while (enume.hasMoreElements()) {
            XmlFile file = (XmlFile) enume.nextElement();
            this.getEditor().getXmlFilesManager().closableFile(file);
            this.getEditor().getXmlFilesManager().closeFile(file);
        }
        fileList.clear();
        editor.internalFrameClosing(this);
        this.dispose();
        editor.updateEditMenu();
        editor.updateToolBar();
        return true;
    }

    /**
	 * this is a listener on TreeView. It allows to catch frame events like
	 * closing
	 */
    private class FrameAdapter extends InternalFrameAdapter {

        /**
		 * Invoked when an internal frame is in the process of being closed. The
		 * close operation can be overridden at this point.
		 * 
		 * @param e
		 *            the closing event
		 */
        public void internalFrameClosing(InternalFrameEvent e) {
            if (!TreeView.this.exit()) {
                TreeView.this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            }
        }
    }

    /**
	 * called when the selection changes
	 * 
	 * @param e
	 *            a TreeSelectionEvent
	 */
    public void valueChanged(TreeSelectionEvent e) {
        TreePath path = (TreePath) e.getPath();
        TreePath t[] = jTree.getSelectionPaths();
        if (t != null) {
            if (t.length == 1) {
                editorPane.displayNode(((XmlTreeNode) t[0].getLastPathComponent()).getNode());
            } else {
                for (int i = 0; i < t.length; i++) {
                    if (t[i].equals(path)) editorPane.displayNode(((XmlTreeNode) path.getLastPathComponent()).getNode());
                }
            }
        }
    }

    /**
	 * gets the document displayed in this treeView
	 * 
	 * @return the document displayed in this treeView
	 */
    public DocumentImpl getDocument() {
        return document;
    }

    /**
	 * checks that every file in this tree was saved. For those which are not,
	 * it may ask the user whether to save them (ask flag)
	 * 
	 * @param ask
	 *            if true, ask the user whether to save each unsaved file
	 * @return 0 if nothing special, 1 if all files should be saved from now, -1
	 *         if all modifications should be canceled from now, -2 if the
	 *         operation should be canceled
	 */
    public int checkSave(boolean ask) {
        Enumeration files = fileList.getPoolContent();
        while (files.hasMoreElements()) {
            XmlFile file = (XmlFile) files.nextElement();
            boolean modifiedOnDisk = file.lastModified < file.lastModified();
            if (!file.upToDate || !file.hasName || modifiedOnDisk) {
                if (ask) {
                    Object[] options = { "Yes", "Cancel" };
                    String message;
                    message = "Modifications will be lost.\nDo you still want to close it ?";
                    int answer = JOptionPane.showOptionDialog(this.getEditor(), message, "Closing artefact", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                    switch(answer) {
                        case 0:
                            break;
                        case 1:
                            return -2;
                    }
                } else {
                    if (!saveFile(file)) {
                        return -2;
                    }
                }
            }
        }
        if (ask) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
	 * Same as save, but with save as option
	 * 
	 * @param ask
	 * @return
	 */
    public int checkSaveAs(boolean ask) {
        Enumeration files = fileList.getPoolContent();
        if (files.hasMoreElements()) {
            XmlFile file = (XmlFile) files.nextElement();
            if (ask) {
                Object[] options = { "Yes", "Cancel" };
                String message;
                message = "Modifications will be lost.\nDo you still want to close it ?";
                int answer = JOptionPane.showOptionDialog(this.getEditor(), message, "Closing artefact", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                switch(answer) {
                    case 0:
                        break;
                    case 1:
                        return -2;
                }
            } else {
                if (!nameAndSaveFileAs(file)) {
                    return -2;
                }
            }
        }
        if (ask) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
	 * saves all the file that need to be saved in this tree
	 */
    public void save() {
        if (!isRemote) {
            checkSave(false);
        } else {
            if (editor.online) {
                int b = JOptionPane.showConfirmDialog(new JFrame(), "System will replace this artefact on the repository.\n" + "Do you want to continue with this operation?", "Saving on repository", JOptionPane.WARNING_MESSAGE);
            } else saveSecureFile();
        }
    }

    /**
	 * Save as a new file
	 * 
	 */
    public void saveAs() {
        checkSaveAs(false);
    }

    /**
	 * Export actual treeview as a template
	 * 
	 */
    public void saveTemplate() {
        Enumeration files = fileList.getPoolContent();
        if (files.hasMoreElements()) {
            XmlFile file = (XmlFile) files.nextElement();
            String fileName = editor.getXmlFilesManager().nameNewFileAsTemplate();
            File newFile = new File(fileName);
            if (newFile != null) {
                if (newFile.exists()) {
                    Object[] options = { "Yes", "No" };
                    int answer = JOptionPane.showOptionDialog(this.getEditor(), "This file already exists.\nDo you want to overwrite it ?", "Existing file", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                    if (answer != 1) {
                        try {
                            SDMainFrame.stringToFile(file.getContent(), fileName);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    try {
                        SDMainFrame.stringToFile(file.getContent(), fileName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
	 * Save a copy of security of a remote artefact
	 * 
	 */
    public void saveSecureFile() {
        Enumeration files = fileList.getPoolContent();
        if (files.hasMoreElements()) {
            XmlFile file = (XmlFile) files.nextElement();
            String fileName = editor.getXmlFilesManager().nameNewFileAsSecureFile();
            File newFile = new File(fileName);
            if (newFile != null) {
                if (newFile.exists()) {
                    Object[] options = { "Yes", "No" };
                    int answer = JOptionPane.showOptionDialog(this.getEditor(), "This file already exists.\nDo you want to overwrite it ?", "Existing file", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                    if (answer != 1) {
                        try {
                            SDMainFrame.stringToFile(file.getContent(), fileName);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    try {
                        SDMainFrame.stringToFile(file.getContent(), fileName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
	 * Crea una nueva versi�n de la DPD seleccionada
	 */
    public void newVersion() {
        String soadPath = this.getTitle();
        DpdParser soad = new DpdParser(soadPath, this.getEditor());
        String actualVersion = soad.version();
        String selectedVersion = getVersionFromUser(actualVersion);
        if (selectedVersion != null) {
            String versionDescription = getDescriptionFromUser();
            if (versionDescription != null) {
                saveOldVersion(actualVersion, selectedVersion, versionDescription);
            }
        }
    }

    /**
	 * Obtiene el nuevo n�mero de versi�n del usuario para la DPD seleccionada
	 * 
	 * @param actualVersion
	 *            versi�n actual de la DPD
	 * @return String - Numero de la nueva versi�n
	 */
    public String getVersionFromUser(String actualVersion) {
        int versionChar0 = ((int) actualVersion.charAt(0));
        int versionChar0plus1 = (versionChar0 % 48) + 1;
        char versionChar0Modified = (char) (versionChar0plus1 + 48);
        int versionChar2 = ((int) actualVersion.charAt(2));
        int versionChar2plus1 = (versionChar2 % 48) + 1;
        char versionChar2Modified = (char) (versionChar2plus1 + 48);
        Object[] possibilities = { versionChar0Modified + "." + "0", actualVersion.charAt(0) + "." + versionChar2Modified };
        JComboBox versionsCombo = new JComboBox(possibilities);
        versionsCombo.setEditable(true);
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Select next version:"));
        panel.add(versionsCombo);
        int value = JOptionPane.showOptionDialog(this, panel, "New Version", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null, null);
        if (value == JOptionPane.OK_OPTION) {
            String selectedVersion = new String((String) versionsCombo.getSelectedItem());
            if (selectedVersion.equals("")) return null;
            return (selectedVersion);
        } else {
            return null;
        }
    }

    /**
	 * Obtiene la descripci�n de la DPD del usuario
	 * 
	 * @return String - Cadena con la descripci�n de la versi�n de la DPD
	 */
    public String getDescriptionFromUser() {
        String description = (String) JOptionPane.showInputDialog(this, "OLD Dpd description:\n", "New Version", JOptionPane.PLAIN_MESSAGE, null, null, "Old version description");
        return description;
    }

    /**
	 * Obtiene las verfsiones existentes de la DPD seleccionada
	 */
    public void getVersions() {
        Vector versionList = new Vector();
        String actualFileName = this.getTitle();
        DpdParser actualSoad = new DpdParser(actualFileName, this.getEditor());
        VersionsSetup versionsSetup = new VersionsSetup(this.getEditor());
        versionList = versionsSetup.getVersions(actualSoad);
        if (versionList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No available versions for " + "this DPD", "Get Versions", JOptionPane.WARNING_MESSAGE);
            displayMessage("No available versions for this DPD");
        }
    }

    /**
	 * Almacena la DPD actual como antigua
	 * 
	 * @param actualVersion
	 *            versi�n de la DPD actual
	 * @param newVersion
	 *            nueva Versi�n de la DPD
	 * @param versionDescription
	 *            descripci�n de la DPD que se va a almacenar como antigua
	 */
    public void saveOldVersion(String actualVersion, String newVersion, String versionDescription) {
        String actualFileName = this.getTitle();
        String newFileName = changeVersionFileName(actualFileName, actualVersion);
        DpdParser actualSoad = new DpdParser(actualFileName, this.getEditor());
        File oldVersionFile = new File(actualFileName);
        File newVersionFile = new File(newFileName);
        boolean bool = oldVersionFile.renameTo(newVersionFile);
        VersionsSetup versionsSetup = new VersionsSetup(this.getEditor());
        versionsSetup.appendSoad(newFileName, versionDescription, actualSoad);
        this.exit();
        actualSoad.changeVersion(newVersion);
        actualSoad.saveContent(actualFileName);
        this.getEditor().createNewTreeView(actualFileName, true, isRemote);
    }

    /**
	 * Cambia el nombre del fichero DPD, para registrar la nueva versi�n. El
	 * nombre ser� idDPD+Version+.old
	 * 
	 * @param actualFileName
	 *            nombre actual del fichero DPD
	 * @param actualVersion
	 *            versi�n actual de la DPD
	 * @return String - cadena con el nuervo nombre del fichero
	 */
    public String changeVersionFileName(String actualFileName, String actualVersion) {
        String newFileName = null;
        for (int i = 0; i < actualFileName.length(); i++) {
            if (actualFileName.charAt(i) == '.') {
                newFileName = actualFileName.substring(0, i) + actualVersion + ".old";
            }
        }
        return newFileName;
    }

    /**
	 * saves an XmlFile. If it has no name, name it before
	 * 
	 * @param file
	 *            the file to ba saved
	 * @return true if succesfull
	 */
    private boolean saveFile(XmlFile file) {
        if (file.hasName) {
            if ((file.lastModified < file.lastModified()) && !file.upToDate) {
                Object[] options = { "Yes", "No" };
                int answer = JOptionPane.showOptionDialog(this.getEditor(), "This file was modified on disk.\nDo you want to overwrite it ?", "Modified file", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                if (answer == 1) {
                    return false;
                }
            }
            boolean result = file.save();
            if (result && fileList.getRefNumber(file) == 0) {
                this.removeFile(file);
            }
            return result;
        } else {
            return this.nameAndSaveFile(file);
        }
    }

    /**
	 * tries to save an unnamed file after naming it
	 * 
	 * @param file
	 *            the file concerned
	 * @return true if succesfull, ie the file was saved
	 */
    public boolean nameAndSaveFile(XmlFile file) {
        XmlFile newFile = editor.getXmlFilesManager().nameNewFile(file);
        if (newFile != null) {
            if (newFile.exists()) {
                Object[] options = { "Yes", "No" };
                int answer = JOptionPane.showOptionDialog(this.getEditor(), "This file already exists.\nDo you want to overwrite it ?", "Existing file", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                if (answer == 1) {
                    return false;
                }
            }
            this.topFileChanged(newFile);
            if (!isRemote) editor.getSetup().addLastOpenFile(newFile.getAbsolutePath());
            int value = fileList.getRefNumber(file);
            this.removeFile(file);
            fileList.addRef(newFile, value);
            newFile.save();
            return true;
        }
        return false;
    }

    /**
	 * tries to "save as" an unnamed file after naming it
	 * 
	 * @param file
	 *            the file concerned
	 * @return true if succesfull, ie the file was saved
	 */
    public boolean nameAndSaveFileAs(XmlFile file) {
        XmlFile newFile = editor.getXmlFilesManager().nameNewFileAs(file);
        if (newFile != null) {
            if (newFile.exists()) {
                Object[] options = { "Yes", "No" };
                int answer = JOptionPane.showOptionDialog(this.getEditor(), "This file already exists.\nDo you want to overwrite it ?", "Existing file", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                if (answer == 1) {
                    return false;
                }
            }
            this.topFileChanged(newFile);
            if (!isRemote) editor.getSetup().addLastOpenFile(newFile.getAbsolutePath());
            int value = fileList.getRefNumber(file);
            this.removeFile(file);
            fileList.addRef(newFile, value);
            newFile.save();
            return true;
        }
        return false;
    }

    /**
	 * resolves a link in the Xml file by parsing the current node, getting the
	 * filename and bookmark pointed by the node, retrieving the file, parsing
	 * it if necessary and finding in the resulting document the pointed node
	 * 
	 * @param node
	 *            the reference node, if it is not one, this method just returns
	 *            node. If it's reference does not exists, it returns null
	 * @return the unreferenced node
	 */
    public TreeElement resolveLink(TreeElement node) {
        if (node == null) {
            return null;
        }
        NamedNodeMap attrs = node.getAttributes();
        String href = null;
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            if (attr.getNodeName().equals("href")) {
                href = attr.getNodeValue();
                break;
            }
        }
        if (href == null) {
            return node;
        }
        String fileName = null;
        String bookmark = null;
        int hash = href.indexOf("#");
        if (hash != -1) {
            fileName = href.substring(0, hash);
            bookmark = href.substring(hash + 1);
        } else {
            fileName = href;
            bookmark = null;
        }
        XmlFile file = ((DocumentImpl) node.getOwnerDocument()).getXmlFile();
        if (fileName.length() != 0) {
            file = new XmlFile(file, fileName, this, editor);
            boolean alreadyOpened = false;
            Enumeration enume = fileList.getPoolContent();
            while (enume.hasMoreElements()) {
                XmlFile openedFile = (XmlFile) enume.nextElement();
                if (file.equals(openedFile)) {
                    file = openedFile;
                    alreadyOpened = true;
                    break;
                }
            }
            if (!alreadyOpened) {
                XmlFilesManager xfm = editor.getXmlFilesManager();
                file = xfm.getXmlFile(XmlFilesManager.getFileFromNode(node), fileName, this);
                if (file == null) {
                    xfm.closableFile(file);
                    xfm.closeFile(file);
                    return null;
                }
            }
        }
        DocumentImpl document = (DocumentImpl) file.getDocument();
        if (document == null) {
            return null;
        }
        fileList.addRef(file);
        String kind = node.getNodeName().substring(0, node.getNodeName().length() - 3);
        if (document.getNodeName().equals(kind)) {
            NamedNodeMap nattrs = document.getAttributes();
            for (int j = 0; j < nattrs.getLength(); j++) {
                if (nattrs.item(j).getNodeName().equals("name") && nattrs.item(j).getNodeValue().equals(bookmark)) {
                    return document;
                }
            }
        }
        NodeList nlist = document.getElementsByTagName(kind);
        for (int i = 0; i < nlist.getLength(); i++) {
            Node n = nlist.item(i);
            NamedNodeMap nattrs = n.getAttributes();
            for (int j = 0; j < nattrs.getLength(); j++) {
                if (nattrs.item(j).getNodeName().equals("name") && nattrs.item(j).getNodeValue().equals(bookmark)) {
                    return (TreeElement) n;
                }
            }
        }
        return null;
    }

    /**
	 * this removes a reference of File file
	 * 
	 * @param file
	 *            the file concerned
	 */
    protected void removeRefOnFile(XmlFile file) {
        fileList.removeRef(file);
        if (fileList.getRefNumber(file) == 0) {
            editor.getXmlFilesManager().closableFile(file);
            if (file.upToDate) {
                this.removeFile(file);
            }
        }
    }

    /**
	 * this removes the File file from this TreeView
	 * 
	 * @param file
	 *            the file concerned
	 */
    private void removeFile(XmlFile file) {
        fileList.remove(file);
        editor.getXmlFilesManager().closeFile(file);
    }

    /**
	 * Comprueba si la DPD est� firmada
	 * 
	 * @param DpdFile
	 *            path del fichero DPD que se desea comprobar
	 * @return boolean - True si la DPD est� firmada
	 */
    public boolean soadIsSigned(String soadFile) {
        String signFileName = obtainSignName(soadFile);
        File tempFile = new File(signFileName);
        return (tempFile.exists());
    }

    /**
	 * Obtiene el nombre del fichero de DPD firma a partir del nombre de la DPD
	 * 
	 * @param fileToSign
	 *            nombre de la DPD original
	 * @return String - cadena con el nombre de la firma digital
	 */
    public String obtainSignName(String fileToSign) {
        String signName = null;
        int fileNameSize = fileToSign.length();
        String fileSimpleName = fileToSign.substring(0, fileNameSize - 4);
        String fileExtension = fileToSign.substring(fileNameSize - 4, fileNameSize);
        signName = fileSimpleName + "Sign" + fileExtension;
        return signName;
    }

    /**
	 * Cambia el tipo de documento. Antes de enviarlo al servidor le indicamos
	 * el XML-Schema asociado.
	 * 
	 * @param DpdPath
	 *            path del fichero DPD
	 * @return String contenido de la DPD con el nuevo tipo
	 */
    public String changeDocType(String soadPath) {
        DpdParser soad = new DpdParser(soadPath, this.getEditor());
        return (soad.toXMLString());
    }

    public TreeView() {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        this.getContentPane().add(jProgressBar1, BorderLayout.SOUTH);
    }

    /**
	 * Remove actual frame listener
	 * 
	 */
    private void removeActualFrameListener() {
        for (int i = 0; i < getInternalFrameListeners().length; i++) {
            if (getInternalFrameListeners()[i] instanceof InternalFrameAdapter && !(getInternalFrameListeners()[i] instanceof FrameAdapter)) {
                removeInternalFrameListener(getInternalFrameListeners()[i]);
                i--;
            }
        }
    }

    public static void main(String[] args) {
        SDMainFrame.stringToFile("a\nb", "z.txt");
    }
}
