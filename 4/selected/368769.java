package ch.cern.lhcb.xmleditor.gui;

import java.awt.BorderLayout;
import java.io.File;
import java.util.Enumeration;
import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ch.cern.lhcb.xmleditor.Editor;
import ch.cern.lhcb.xmleditor.specific.Pool;
import ch.cern.lhcb.xmleditor.specific.XmlFile;
import ch.cern.lhcb.xmleditor.specific.XmlFilesManager;
import ch.cern.lhcb.xmleditor.transfer.CutAndPaste;
import ch.cern.lhcb.xmleditor.xerces.DocumentImpl;
import ch.cern.lhcb.xmleditor.xerces.TreeElement;

/**
 * This class shows a DOM Document in a JTree, and presents controls which allow
 * the user to create and view the progress of a NodeIterator in the DOM tree.
 */
public class TreeView extends JInternalFrame implements MessageDisplayer, TreeSelectionListener {

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
     * The parser used to parse the document
     */
    private DOMParser parser;

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
    private XmlTree jTree;

    /**
     * The Cut and Paste facilities
     */
    private CutAndPaste cutAndPaste;

    /**
     * a pointer on the XmlEditor that uses this TreeView
     */
    private Editor editor;

    /**
     * this boolean says whether the comment nodes should be displayed or not
     */
    private boolean displayComments = false;

    /**
     * this boolean says whether the node values are to be displayed like nodes
     * or like attributes of their parent node
     */
    private boolean displayValueAsAttribute = true;

    /**
     * creates a TreeView for the given fileName
     * 
     * @param fileName the name of the Xml file to be viewed
     * @param editor a pointer on the underlying editor
     */
    public TreeView(String fileName, Editor editor) {
        super(fileName, true, true, true, true);
        this.editor = editor;
        buildsAPI();
        XmlFilesManager xfm = editor.getXmlFilesManager();
        XmlFile file = xfm.getXmlFile(fileName, this);
        fileList = new Pool();
        if (file == null) {
            return;
        }
        fileList.addRef(file);
        document = file.getDocument();
        jTree.getXmlModel().setRootNode(document);
    }

    /**
     * creates a TreeView for the XmlFile given
     * 
     * @param docType the name of the document type
     * @param dtd the file containing the dtd
     * @param editor a pointer on the underlying editor
     */
    public TreeView(String docType, File dtd, Editor editor) {
        super(editor.getXmlFilesManager().getNewFileName(), true, true, true, true);
        this.editor = editor;
        buildsAPI();
        XmlFilesManager xfm = editor.getXmlFilesManager();
        XmlFile file = xfm.getNewFile(docType, dtd, this);
        fileList = new Pool();
        fileList.addRef(file);
        document = file.getDocument();
        jTree.getXmlModel().setRootNode(document);
    }

    /**
     * builds the TreeView API
     */
    private void buildsAPI() {
        setSize(450, 450);
        addInternalFrameListener(new FrameAdapter());
        jTree = new XmlTree(this, this);
        JScrollPane treeScroll = new JScrollPane(jTree);
        treeScroll.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Tree View"), BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        editorPane = new EditorPane(this);
        jTree.addTreeSelectionListener(this);
        JScrollPane editorScroll = new JScrollPane(editorPane);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, editorScroll);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(200);
        splitPane.setOneTouchExpandable(true);
        messageText = new JTextArea();
        messageText.setEditable(false);
        JPanel messagePanel = new JPanel(new BorderLayout());
        JScrollPane messageScroll = new JScrollPane(messageText);
        messagePanel.add(messageScroll);
        messagePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Messages"), BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPane, messagePanel);
        splitPane2.setContinuousLayout(true);
        splitPane2.setDividerLocation(300);
        splitPane2.setOneTouchExpandable(true);
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(splitPane2, BorderLayout.CENTER);
        getContentPane().add(mainPanel);
        TreeViewMenu menuBar = new TreeViewMenu(this);
        this.setJMenuBar(menuBar);
        cutAndPaste = new CutAndPaste(jTree, this);
    }

    /**
     * displays a message on the message window of this TreeView
     * 
     * @param message the message to be displayed
     */
    public void displayMessage(String message) {
        messageText.append(message);
    }

    /**
     * pops up a message in this TreeView
     * 
     * @param message the message to be displayed
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
     * @param file the new file to display
     */
    public void topFileChanged(XmlFile file) {
        this.setTitle(file.getAbsolutePath());
        document = file.getDocument();
        ((XmlTreeNode) jTree.getXmlModel().getRoot()).setNode(document);
        redisplayTree();
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
    public void redisplayTree() {
        this.getXmlTree().getXmlModel().redisplayTree();
    }

    /**
     * redisplays both the tree and the editor pane
     */
    public void redisplayAll() {
        this.redisplayEditorPane();
        this.redisplayTree();
    }

    /**
     * gets the XmlEditor that displays this TreeView
     */
    public Editor getEditor() {
        return editor;
    }

    /**
     * gets the EditorPane from this TreeView
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
     * @param vAsA true to display values as attributes
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
     * @param display true to display comments.
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
        Enumeration enm = fileList.getPoolContent();
        while (enm.hasMoreElements()) {
            XmlFile file = (XmlFile) enm.nextElement();
            this.getEditor().getXmlFilesManager().closableFile(file);
            this.getEditor().getXmlFilesManager().closeFile(file);
        }
        fileList.clear();
        editor.internalFrameClosing(this);
        this.dispose();
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
         * @param e the closing event
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
     * @param e a TreeSelectionEvent
     */
    public void valueChanged(TreeSelectionEvent e) {
        TreePath path = (TreePath) e.getPath();
        XmlTreeNode treeNode = (XmlTreeNode) path.getLastPathComponent();
        TreeElement node = (TreeElement) treeNode.getNode();
        editorPane.displayNode(node);
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
     * @param ask if true, ask the user whether to save each unsaved file
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
                    Object[] options = { "Yes", "No", "Yes to all", "No to all", "Cancel" };
                    String message;
                    if (!file.upToDate || !file.hasName) {
                        message = "Modifications will be lost in file " + file.getAbsolutePath() + ".\nDo you want to save it ?";
                    } else {
                        message = "File " + file.getAbsolutePath() + " was modified on disk but not in this editor.\nDo you want to save it ?";
                    }
                    int answer = JOptionPane.showOptionDialog(this.getEditor(), message, "Unsaved file", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                    switch(answer) {
                        case 0:
                            if (!saveFile(file)) {
                                return -2;
                            }
                            break;
                        case 1:
                            break;
                        case 2:
                            if (!saveFile(file)) {
                                return -2;
                            }
                            ask = false;
                            break;
                        case 3:
                            return -1;
                        case 4:
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
     * saves all the file that need to be saved in this tree
     */
    public void save() {
        checkSave(false);
    }

    /**
     * saves an XmlFile. If it has no name, name it before
     * 
     * @param file the file to ba saved
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
     * @param file the file concerned
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
            editor.getSetup().addLastOpenFile(newFile.getAbsolutePath());
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
     * @param node the reference node, if it is not one, this method just
     *        returns node. If it's reference does not exists, it returns null
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
            Enumeration enm = fileList.getPoolContent();
            while (enm.hasMoreElements()) {
                XmlFile openedFile = (XmlFile) enm.nextElement();
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
     * @param file the file concerned
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
     * @param file the file concerned
     */
    private void removeFile(XmlFile file) {
        fileList.remove(file);
        editor.getXmlFilesManager().closeFile(file);
    }
}
