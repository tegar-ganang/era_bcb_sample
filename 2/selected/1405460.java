package org.virbo.autoplot.bookmarks;

import java.awt.Component;
import java.awt.Container;
import java.awt.HeadlessException;
import org.virbo.autoplot.bookmarks.Bookmark.Folder;
import org.virbo.datasource.AutoplotSettings;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import org.virbo.autoplot.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.xml.parsers.ParserConfigurationException;
import org.das2.system.RequestProcessor;
import org.virbo.autoplot.scriptconsole.GuiExceptionHandler;
import org.virbo.datasource.DataSetSelector;
import org.virbo.datasource.URISplit;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Object for managing the user's bookmarks.  This object sits quietly beside the Autoplot
 * UI, becoming visible when the user asks to manage bookmarks.  This also populates the Bookmarks
 * submenu.
 * @author  jbf
 */
public class BookmarksManager extends javax.swing.JDialog {

    private static final Logger logger = Logger.getLogger("autoplot.bookmarks");

    /**
     * return the index of the node in the tree, by comparing at toString of each node.
     * @param tree
     * @param root
     * @param find
     * @return
     */
    private int indexOfChild(TreeModel tree, Object root, Object find) {
        for (int i = 0; i < tree.getChildCount(root); i++) {
            Object tt = tree.getChild(root, i);
            if (tt.toString().equals(find.toString())) return i;
        }
        return -1;
    }

    /**
     * returns the TreePath in the new tree, or null if it cannot be identified.
     * @param mod
     * @param foriegn
     * @return
     */
    private TreePath moveTreePath(TreeModel mod, TreePath foriegn) {
        Object parent = mod.getRoot();
        Object[] path = new Object[foriegn.getPathCount()];
        path[0] = parent;
        for (int i = 1; i < foriegn.getPathCount(); i++) {
            int j = indexOfChild(mod, parent, foriegn.getPathComponent(i));
            if (j > -1) {
                parent = mod.getChild(parent, j);
                path[i] = parent;
            } else {
                Object[] parentPath = new Object[i];
                System.arraycopy(path, 0, parentPath, 0, i);
                return new TreePath(parentPath);
            }
        }
        return new TreePath(path);
    }

    /** Creates new form BookmarksManager */
    public BookmarksManager(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        this.model = new BookmarksManagerModel();
        this.jTree1.setModel(model.getTreeModel());
        this.jTree1.addMouseListener(createContextMenuMouseListener());
        model.addPropertyChangeListener(BookmarksManagerModel.PROP_LIST, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                Runnable run = new Runnable() {

                    public void run() {
                        TreeModel mod = model.getTreeModel();
                        TreePath tp = jTree1.getSelectionPath();
                        jTree1.setModel(mod);
                        if (tp != null) {
                            tp = moveTreePath(mod, tp);
                            if (tp != null) {
                                jTree1.setSelectionPath(tp);
                                if (jTree1.getModel().isLeaf(tp.getLastPathComponent())) tp = tp.getParentPath();
                                jTree1.expandPath(tp);
                                jTree1.scrollPathToVisible(tp);
                            }
                        }
                    }
                };
                if (SwingUtilities.isEventDispatchThread()) {
                    run.run();
                } else {
                    SwingUtilities.invokeLater(run);
                }
            }
        });
        BookmarksManagerTransferrable trans = new BookmarksManagerTransferrable(model, jTree1);
        DragSource dragSource = DragSource.getDefaultDragSource();
        DropTarget dropTarget = new DropTarget();
        try {
            dropTarget.addDropTargetListener(trans.createDropTargetListener());
        } catch (TooManyListenersException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        jTree1.setDropTarget(dropTarget);
        dragSource.createDefaultDragGestureRecognizer(jTree1, DnDConstants.ACTION_COPY_OR_MOVE, trans.createDragGestureListener());
    }

    BookmarksManagerModel model;

    Bookmark dirtyBookmark;

    JPopupMenu contextMenu = createContextMenu();

    /**
     * true indicates the menu managed by the bookmarks manager is dirty and needs to be updated.
     */
    private boolean menuIsDirty = false;

    private JMenu dirtyMenu = null;

    private DataSetSelector dirtySelector = null;

    public BookmarksManagerModel getModel() {
        return model;
    }

    /**
     * DataSetSelector to send URIs to.
     */
    DataSetSelector sel;

    /**
     * return the remoteUrl containing this bookmark, or an empty string.
     * @param b the target bookmark.  This can be null.
     * @param treeModel the model containing the bookmark
     * @param ppath the path to the bookmark.
     * @return the first remote URL containing the bookmark, or an empty string if it is local.
     */
    protected static String maybeGetRemoteBookmarkUrl(Bookmark b, BookmarksManagerModel model, TreeModel treeModel, TreePath ppath) {
        String remoteUrl = "";
        if (b != null && b instanceof Bookmark.Item) ppath = ppath.getParentPath();
        while (ppath.getPathCount() > 1) {
            Bookmark.Folder f = (Folder) model.getSelectedBookmark(treeModel, ppath);
            if (f.remoteUrl != null && !f.remoteUrl.equals("")) {
                remoteUrl = f.remoteUrl;
                break;
            }
            ppath = ppath.getParentPath();
        }
        return remoteUrl;
    }

    /**
     * remove the bookmarks from the list that come from a remote bookmarks file.  For example, these cannot be individually deleted.
     * The root node of a bookmarks file is left in there, so that it may be used to delete the folder.
     * @param bs
     * @param tmodel
     * @param selectionPaths
     * @return the remaining remote bookmarks.
     */
    private List<Bookmark> removeRemoteBookmarks(List<Bookmark> bs, TreeModel tmodel, TreePath[] selectionPaths) {
        assert selectionPaths.length == bs.size();
        List<Bookmark> result = new ArrayList();
        for (int i = 0; i < bs.size(); i++) {
            Bookmark bs1 = bs.get(i);
            if ((bs1 instanceof Bookmark.Folder && ((Bookmark.Folder) bs1).getRemoteUrl() != null) || "".equals(maybeGetRemoteBookmarkUrl(bs.get(i), model, tmodel, selectionPaths[i]))) {
                result.add(bs.get(i));
            }
        }
        return result;
    }

    /**
     * present a GUI offering to delete the set of bookmarks.
     * @param bs
     * @throws HeadlessException
     */
    private void maybeDeleteBookmarks(List<Bookmark> bs) throws HeadlessException {
        boolean confirm = false;
        if (bs.size() > 1 && JOptionPane.showConfirmDialog(this, "Delete " + bs.size() + " bookmarks?", "Delete Bookmarks", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            confirm = true;
        }
        if (confirm) {
            model.removeBookmarks(bs);
        } else {
            for (Bookmark b : bs) {
                if (b instanceof Bookmark.Folder) {
                    if (confirm || JOptionPane.showConfirmDialog(this, "Delete all bookmarks and folder?", "Delete Bookmarks Folder", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                        model.removeBookmark(b);
                    }
                } else {
                    model.removeBookmark(b);
                }
            }
        }
    }

    /**
     * show a message to the user.  Copied from ApplicationModel.
     * @param message
     * @param title
     * @param messageType JOptionPane.WARNING_MESSAGE, JOptionPane.INFORMATION_MESSAGE, JOptionPane.PLAIN_MESSAGE,
     */
    private void showMessage(String message, String title, int messageType) {
        if (!"true".equals(AutoplotUtil.getProperty("java.awt.headless", "false"))) {
            Component p = SwingUtilities.getRoot(this);
            if (p == null) {
                if (messageType == JOptionPane.WARNING_MESSAGE) {
                    System.err.println("WARNING: " + title + ": " + message);
                } else if (messageType == JOptionPane.INFORMATION_MESSAGE) {
                    System.err.println("INFO: " + title + ": " + message);
                } else {
                    System.err.println(title + ": " + message);
                }
            } else {
                JOptionPane.showMessageDialog(p, message, title, messageType);
            }
        } else {
            if (messageType == JOptionPane.WARNING_MESSAGE) {
                System.err.println("WARNING: " + title + ": " + message);
            } else if (messageType == JOptionPane.INFORMATION_MESSAGE) {
                System.err.println("INFO: " + title + ": " + message);
            } else {
                System.err.println(title + ": " + message);
            }
        }
    }

    public void setAddBookmark(Bookmark b) {
        TreePath tp = model.getPathFor(b, jTree1.getModel(), new TreePath(jTree1.getModel().getRoot()));
        jTree1.setSelectionPath(tp);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                titleTextField.requestFocusInWindow();
                titleTextField.selectAll();
            }
        });
    }

    /**
     * set if the play button was used to excuse the GUI.  I was thinking it would be useful to allow the manager to be used to select
     * data sources, since it shows the descriptions, but I need to focus on getting this cleaned up first.  I'm leaving this as
     * a reminder.  
     */
    private boolean doPlay = false;

    public boolean isPlay() {
        return doPlay;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jScrollPane1 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();
        dismissButton = new javax.swing.JButton();
        URILabel = new javax.swing.JLabel();
        URLTextField = new javax.swing.JTextField();
        titleLabel = new javax.swing.JLabel();
        titleTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        descriptionTextField = new javax.swing.JTextField();
        editDescriptionButton = new javax.swing.JButton();
        plotButton = new javax.swing.JButton();
        plotBelowButton = new javax.swing.JButton();
        overplotButton = new javax.swing.JButton();
        viewDetailsButton = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        importMenuItem = new javax.swing.JMenuItem();
        importUrlMenuItem = new javax.swing.JMenuItem();
        reloadMenuItem = new javax.swing.JMenuItem();
        resetToDefaultMenuItem = new javax.swing.JMenuItem();
        mergeInDefaultMenuItem = new javax.swing.JMenuItem();
        exportMenuItem = new javax.swing.JMenuItem();
        closeMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        newFolderMenuItem = new javax.swing.JMenuItem();
        addItemMenuItem = new javax.swing.JMenuItem();
        deleteMenuItem = new javax.swing.JMenuItem();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        jTree1.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {

            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                jTree1ValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jTree1);
        dismissButton.setText("OK");
        dismissButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dismissButtonActionPerformed(evt);
            }
        });
        URILabel.setText("URI:");
        URILabel.setToolTipText("Location of the data (often the URL), or remote folder location");
        URLTextField.setToolTipText("");
        URLTextField.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                URLTextFieldFocusLost(evt);
            }
        });
        URLTextField.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                URLTextFieldKeyTyped(evt);
            }
        });
        titleLabel.setText("Title:");
        titleLabel.setToolTipText("Title for the URI");
        titleTextField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                titleTextFieldActionPerformed(evt);
            }
        });
        titleTextField.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                titleTextFieldFocusLost(evt);
            }
        });
        titleTextField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                titleTextFieldPropertyChange(evt);
            }
        });
        titleTextField.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                titleTextFieldKeyTyped(evt);
            }
        });
        jLabel1.setText("Bookmarks Manager");
        jLabel4.setText("Description:");
        jLabel4.setToolTipText("Up to a short paragraph describing the data");
        descriptionTextField.setToolTipText("Up to a short paragraph describing the data");
        descriptionTextField.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                descriptionTextFieldFocusLost(evt);
            }
        });
        descriptionTextField.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(java.awt.event.KeyEvent evt) {
                descriptionTextFieldKeyTyped(evt);
            }
        });
        editDescriptionButton.setText("Edit");
        editDescriptionButton.setToolTipText("Edit/View description");
        editDescriptionButton.setEnabled(false);
        editDescriptionButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editDescriptionButtonActionPerformed(evt);
            }
        });
        plotButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/virbo/autoplot/go.png")));
        plotButton.setText("Plot");
        plotButton.setToolTipText("Plot the URI in the current focus position");
        plotButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                plotButtonActionPerformed(evt);
            }
        });
        plotBelowButton.setText("Plot Below");
        plotBelowButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                plotBelowButtonActionPerformed(evt);
            }
        });
        overplotButton.setText("Overplot");
        overplotButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                overplotButtonActionPerformed(evt);
            }
        });
        viewDetailsButton.setText("Detailed Description");
        viewDetailsButton.setToolTipText("View description URL in browser");
        viewDetailsButton.setEnabled(false);
        viewDetailsButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewDetailsButtonActionPerformed(evt);
            }
        });
        jMenu1.setText("File");
        importMenuItem.setText("Import...");
        importMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(importMenuItem);
        importUrlMenuItem.setText("Import From Web...");
        importUrlMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importUrlMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(importUrlMenuItem);
        reloadMenuItem.setText("Reload Bookmarks");
        reloadMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(reloadMenuItem);
        resetToDefaultMenuItem.setText("Reset to Default");
        resetToDefaultMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetToDefaultMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(resetToDefaultMenuItem);
        mergeInDefaultMenuItem.setText("Merge in Default");
        mergeInDefaultMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mergeInDefaultMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(mergeInDefaultMenuItem);
        exportMenuItem.setText("Export...");
        exportMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(exportMenuItem);
        closeMenuItem.setText("Close");
        closeMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(closeMenuItem);
        jMenuBar1.add(jMenu1);
        editMenu.setText("Edit");
        newFolderMenuItem.setAction(newFolderAction());
        newFolderMenuItem.setText("New Folder...");
        editMenu.add(newFolderMenuItem);
        addItemMenuItem.setAction(addItemAction());
        addItemMenuItem.setText("New Bookmark...");
        editMenu.add(addItemMenuItem);
        deleteMenuItem.setText("Delete Bookmark");
        deleteMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(deleteMenuItem);
        jMenuBar1.add(editMenu);
        setJMenuBar(jMenuBar1);
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup().addContainerGap().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING).add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 681, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.LEADING, jLabel1).add(layout.createSequentialGroup().add(overplotButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 83, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(plotBelowButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 88, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(plotButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 75, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(dismissButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 56, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup().add(URILabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(URLTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 644, Short.MAX_VALUE)).add(layout.createSequentialGroup().add(jLabel4).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(descriptionTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 516, Short.MAX_VALUE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(editDescriptionButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 71, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(layout.createSequentialGroup().add(titleLabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(titleTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 475, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(viewDetailsButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE))).addContainerGap()));
        layout.linkSize(new java.awt.Component[] { overplotButton, plotBelowButton, plotButton }, org.jdesktop.layout.GroupLayout.HORIZONTAL);
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(jLabel1).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(titleLabel).add(titleTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(viewDetailsButton)).add(7, 7, 7).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel4).add(descriptionTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(editDescriptionButton)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(URILabel).add(URLTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(dismissButton).add(plotButton).add(plotBelowButton).add(overplotButton)).addContainerGap()));
        layout.linkSize(new java.awt.Component[] { URLTextField, titleTextField }, org.jdesktop.layout.GroupLayout.VERTICAL);
        pack();
    }

    private void dismissButtonActionPerformed(java.awt.event.ActionEvent evt) {
        doPlay = false;
        this.dispose();
        if (menuIsDirty) {
            updateBookmarks(dirtyMenu, dirtySelector);
        }
    }

    private void URLTextFieldFocusLost(java.awt.event.FocusEvent evt) {
        Bookmark b = model.getSelectedBookmark(jTree1.getModel(), jTree1.getSelectionPath());
        if (b != null && b instanceof Bookmark.Item) {
            ((Bookmark.Item) b).setUri(URLTextField.getText());
            jTree1.repaint();
            model.fireBookmarkChange(b);
        }
    }

    private void titleTextFieldFocusLost(java.awt.event.FocusEvent evt) {
        Bookmark b = model.getSelectedBookmark(jTree1.getModel(), jTree1.getSelectionPath());
        if (b != null) {
            b.setTitle(titleTextField.getText());
            jTree1.repaint();
            model.fireBookmarkChange(b);
        }
    }

    private void jTree1ValueChanged(javax.swing.event.TreeSelectionEvent evt) {
        if (dirtyBookmark != null) {
            dirtyBookmark.setTitle(titleTextField.getText());
            if (dirtyBookmark instanceof Bookmark.Item) {
                ((Bookmark.Item) dirtyBookmark).setUri(URLTextField.getText());
            }
            dirtyBookmark.setDescription(descriptionTextField.getText());
            model.fireBookmarkChange(dirtyBookmark);
            dirtyBookmark = null;
        }
        Bookmark b = model.getSelectedBookmark(jTree1.getModel(), jTree1.getSelectionPath());
        if (b != null) {
            titleTextField.setText(b.getTitle());
            descriptionTextField.setText(b.getDescription());
            viewDetailsButton.setEnabled(b.getDescriptionUrl() != null);
            if (b.getDescriptionUrl() == null) {
                viewDetailsButton.setToolTipText("");
            } else {
                viewDetailsButton.setToolTipText("View " + b.getDescriptionUrl());
            }
            URLTextField.setEditable(b instanceof Bookmark.Item);
            int status = 0;
            String err = "";
            if (b instanceof Bookmark.Item) {
                URLTextField.setText(((Bookmark.Item) b).getUri());
                URILabel.setText("URI:");
            } else {
                if (b instanceof Bookmark.Folder && ((Bookmark.Folder) b).getRemoteUrl() != null) {
                    String url = ((Bookmark.Folder) b).getRemoteUrl();
                    status = ((Bookmark.Folder) b).getRemoteStatus();
                    if (status == Bookmark.Folder.REMOTE_STATUS_UNSUCCESSFUL) err = "<br>** Unable to connect to remote URL **";
                    URLTextField.setText(url);
                    URILabel.setText("URL:");
                } else {
                    URLTextField.setText("");
                    URILabel.setText("URI:");
                }
            }
            String remoteUrl = "";
            TreePath ppath = jTree1.getSelectionPath();
            if (ppath != null) {
                remoteUrl = maybeGetRemoteBookmarkUrl(b, model, jTree1.getModel(), ppath);
                URLTextField.setEditable(remoteUrl.length() == 0);
                if (remoteUrl.length() == 0) {
                    URLTextField.setToolTipText("Location of the remote folder" + err);
                } else {
                    URLTextField.setToolTipText("Location of the data (often the URL)");
                }
            } else {
                URLTextField.setEditable(false);
            }
            if (remoteUrl.length() == 0) {
                titleLabel.setText("Title:");
                titleLabel.setToolTipText("Title for the URI");
            } else {
                titleLabel.setText("[Title]:");
                titleLabel.setToolTipText("<html>Title for the URI.<br>This bookmark is part of a set of remote bookmarks from<br>" + remoteUrl + "<br> and cannot be edited." + err);
            }
            descriptionTextField.setEditable(remoteUrl.length() == 0);
            editDescriptionButton.setEnabled(true);
            editDescriptionButton.setText(remoteUrl.length() == 0 ? "Edit" : "View");
            titleTextField.setEditable(remoteUrl.length() == 0);
        } else {
            titleTextField.setText("");
            descriptionTextField.setText("");
            URLTextField.setText("");
            titleLabel.setText("Title:");
            editDescriptionButton.setEnabled(false);
            viewDetailsButton.setEnabled(false);
            titleTextField.setEditable(false);
        }
    }

    private void importMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        model.doImport(this);
    }

    private void importUrlMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        model.doImportUrl(this);
    }

    private void resetToDefaultMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        String surl = AutoplotUtil.getProperty("autoplot.default.bookmarks", "http://www.autoplot.org/data/demos.xml");
        int r = JOptionPane.showConfirmDialog(this, "Reset your bookmarks to " + surl + "?", "Reset Bookmarks", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            try {
                URL url = new URL(surl);
                Document doc = AutoplotUtil.readDoc(url.openStream());
                List<Bookmark> book = Bookmark.parseBookmarks(doc.getDocumentElement());
                model.setList(book);
                formatToFile(bookmarksFile);
            } catch (SAXException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                new GuiExceptionHandler().handle(ex);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (ParserConfigurationException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    private void exportMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        model.doExport(this);
    }

    private void closeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
        if (menuIsDirty) {
            updateBookmarks(dirtyMenu, dirtySelector);
        }
    }

    private void deleteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        List<Bookmark> bs = model.getSelectedBookmarks(jTree1.getModel(), jTree1.getSelectionPaths());
        if (bs.size() > 0) {
            bs = removeRemoteBookmarks(bs, jTree1.getModel(), jTree1.getSelectionPaths());
            if (bs.size() == 0) {
                JOptionPane.showMessageDialog(rootPane, "Part of remote bookmarks tree cannot be deleted", "Remote Bookmark Delete", JOptionPane.OK_OPTION);
            } else {
                maybeDeleteBookmarks(bs);
            }
        }
    }

    private void titleTextFieldActionPerformed(java.awt.event.ActionEvent evt) {
        Bookmark b = model.getSelectedBookmark(jTree1.getModel(), jTree1.getSelectionPath());
        if (b == null) {
            JOptionPane.showMessageDialog(rootPane, "No bookmark is selected", "No Bookmark Selected", JOptionPane.OK_OPTION);
            return;
        }
        b.setTitle(titleTextField.getText());
        jTree1.repaint();
        model.fireBookmarkChange(b);
    }

    private void titleTextFieldPropertyChange(java.beans.PropertyChangeEvent evt) {
    }

    private void titleTextFieldKeyTyped(java.awt.event.KeyEvent evt) {
        dirtyBookmark = model.getSelectedBookmark(jTree1.getModel(), jTree1.getSelectionPath());
    }

    private void URLTextFieldKeyTyped(java.awt.event.KeyEvent evt) {
        dirtyBookmark = model.getSelectedBookmark(jTree1.getModel(), jTree1.getSelectionPath());
    }

    private void mergeInDefaultMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            String surl = AutoplotUtil.getProperty("autoplot.default.bookmarks", "http://www.autoplot.org/data/demos.xml");
            URL url = new URL(surl);
            Document doc = AutoplotUtil.readDoc(url.openStream());
            List<Bookmark> importBook = Bookmark.parseBookmarks(doc.getDocumentElement());
            List<Bookmark> newList = new ArrayList(model.list.size());
            for (int i = 0; i < model.list.size(); i++) {
                newList.add(i, model.list.get(i).copy());
            }
            model.mergeList(importBook, newList);
            model.setList(newList);
            formatToFile(bookmarksFile);
        } catch (SAXException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    private void descriptionTextFieldFocusLost(java.awt.event.FocusEvent evt) {
        Bookmark b = model.getSelectedBookmark(jTree1.getModel(), jTree1.getSelectionPath());
        if (b != null) {
            b.setDescription(descriptionTextField.getText());
            jTree1.repaint();
            model.fireBookmarkChange(b);
        }
    }

    private void descriptionTextFieldKeyTyped(java.awt.event.KeyEvent evt) {
        dirtyBookmark = model.getSelectedBookmark(jTree1.getModel(), jTree1.getSelectionPath());
    }

    private void editDescriptionButtonActionPerformed(java.awt.event.ActionEvent evt) {
        String txt = descriptionTextField.getText();
        if (txt.contains("<br>")) {
            txt = txt.replaceAll("<br>", "\n");
        }
        JTextArea edit = new JTextArea(txt);
        edit.setRows(5);
        edit.setColumns(80);
        JScrollPane jScrollPane2 = new JScrollPane(edit);
        edit.setEditable(descriptionTextField.isEditable());
        int ok = JOptionPane.showConfirmDialog(this, jScrollPane2, titleTextField.getText(), edit.isEditable() ? JOptionPane.OK_CANCEL_OPTION : JOptionPane.OK_CANCEL_OPTION);
        if (edit.isEditable() && ok == JOptionPane.OK_OPTION) {
            String ntxt = edit.getText();
            if (!ntxt.equals(txt)) {
                ntxt = ntxt.replaceAll("\n", "<br>");
                descriptionTextField.setText(ntxt);
                Bookmark b = model.getSelectedBookmark(jTree1.getModel(), jTree1.getSelectionPath());
                if (b != null) {
                    b.setDescription(descriptionTextField.getText());
                    jTree1.repaint();
                    model.fireBookmarkChange(b);
                }
            }
        }
    }

    private boolean maybePlot(int modifiers) {
        Bookmark book = (Bookmark) model.getSelectedBookmark(jTree1.getModel(), jTree1.getSelectionPath());
        if (book instanceof Bookmark.Item) {
            if (getParent() instanceof AutoplotUI) {
                sel.setValue(((Bookmark.Item) book).getUri());
                sel.maybePlot(modifiers);
            }
            dispose();
            if (menuIsDirty) {
                updateBookmarks(dirtyMenu, dirtySelector);
            }
            return true;
        }
        return false;
    }

    private void plotButtonActionPerformed(java.awt.event.ActionEvent evt) {
        maybePlot(evt.getModifiers());
    }

    private void plotBelowButtonActionPerformed(java.awt.event.ActionEvent evt) {
        maybePlot(KeyEvent.CTRL_MASK);
    }

    private void overplotButtonActionPerformed(java.awt.event.ActionEvent evt) {
        maybePlot(KeyEvent.SHIFT_MASK);
    }

    private void viewDetailsButtonActionPerformed(java.awt.event.ActionEvent evt) {
        Bookmark book = model.getSelectedBookmark(jTree1.getModel(), jTree1.getSelectionPath());
        if (book.getDescriptionUrl() != null) {
            AutoplotUtil.openBrowser(book.getDescriptionUrl().toString());
        }
    }

    private void reloadMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        reload();
    }

    private javax.swing.JLabel URILabel;

    private javax.swing.JTextField URLTextField;

    private javax.swing.JMenuItem addItemMenuItem;

    private javax.swing.JMenuItem closeMenuItem;

    private javax.swing.JMenuItem deleteMenuItem;

    private javax.swing.JTextField descriptionTextField;

    private javax.swing.JButton dismissButton;

    private javax.swing.JButton editDescriptionButton;

    private javax.swing.JMenu editMenu;

    private javax.swing.JMenuItem exportMenuItem;

    private javax.swing.JMenuItem importMenuItem;

    private javax.swing.JMenuItem importUrlMenuItem;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JMenu jMenu1;

    private javax.swing.JMenuBar jMenuBar1;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JTree jTree1;

    private javax.swing.JMenuItem mergeInDefaultMenuItem;

    private javax.swing.JMenuItem newFolderMenuItem;

    private javax.swing.JButton overplotButton;

    private javax.swing.JButton plotBelowButton;

    private javax.swing.JButton plotButton;

    private javax.swing.JMenuItem reloadMenuItem;

    private javax.swing.JMenuItem resetToDefaultMenuItem;

    private javax.swing.JLabel titleLabel;

    private javax.swing.JTextField titleTextField;

    private javax.swing.JButton viewDetailsButton;

    private MouseListener createContextMenuMouseListener() {
        return new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    maybePlot(e.getModifiers());
                }
                if (e.isPopupTrigger()) {
                    contextMenu.show(jTree1, e.getX(), e.getY());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                TreePath path = jTree1.getPathForLocation(e.getX(), e.getY());
                if (!jTree1.getSelectionModel().isPathSelected(path)) {
                    jTree1.getSelectionModel().setSelectionPath(path);
                }
                if (e.isPopupTrigger()) {
                    contextMenu.show(jTree1, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    contextMenu.show(jTree1, e.getX(), e.getY());
                }
            }
        };
    }

    private JPopupMenu createContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(newFolderAction());
        menu.add(addItemAction());
        menu.add(createExportFolderAction());
        menu.add(createDeleteAction());
        return menu;
    }

    private Action addItemAction() throws HeadlessException {
        return new AbstractAction("New Bookmark...") {

            public void actionPerformed(ActionEvent e) {
                String s = JOptionPane.showInputDialog(BookmarksManager.this, "Bookmark URL:");
                if (s != null && !s.equals("")) {
                    String x = maybeGetRemoteBookmarkUrl(null, model, jTree1.getModel(), jTree1.getSelectionPath());
                    if (x.length() == 0) {
                        model.addBookmark(new Bookmark.Item(s), model.getSelectedBookmark(jTree1.getModel(), jTree1.getSelectionPath()));
                    } else {
                        JOptionPane.showMessageDialog(rootPane, "Cannot add item to remote bookmarks\n" + x, "Remote Bookmark Add Item", JOptionPane.OK_OPTION);
                    }
                }
            }
        };
    }

    private Action newFolderAction() throws HeadlessException {
        return new AbstractAction("New Folder...") {

            public void actionPerformed(ActionEvent e) {
                Bookmark context = model.getSelectedBookmark(jTree1.getModel(), jTree1.getSelectionPath());
                String x = maybeGetRemoteBookmarkUrl(context, model, jTree1.getModel(), jTree1.getSelectionPath());
                if (x.length() > 0) {
                    JOptionPane.showMessageDialog(rootPane, "Cannot add folder to remote bookmarks\n" + x, "Remote Bookmark Add Folder", JOptionPane.OK_OPTION);
                    return;
                }
                String s = JOptionPane.showInputDialog(BookmarksManager.this, "New Folder Name:");
                if (s != null && !s.equals("")) {
                    if (s.startsWith("http:") || s.startsWith("https:") || s.startsWith("ftp:")) {
                        try {
                            model.addRemoteBookmarks(s, model.getSelectedBookmark(jTree1.getModel(), jTree1.getSelectionPath()));
                            reload();
                        } catch (IllegalArgumentException ex) {
                            if (true) {
                                showMessage("Error in format of " + s + "\n" + ex.toString(), "Error in import bookmarks", JOptionPane.WARNING_MESSAGE);
                            }
                        }
                    } else {
                        model.addBookmark(new Bookmark.Folder(s), context);
                    }
                }
            }
        };
    }

    Action createDeleteAction() {
        return new AbstractAction("Delete") {

            public void actionPerformed(ActionEvent e) {
                List<Bookmark> bs = model.getSelectedBookmarks(jTree1.getModel(), jTree1.getSelectionPaths());
                if (bs.size() > 0) {
                    bs = removeRemoteBookmarks(bs, jTree1.getModel(), jTree1.getSelectionPaths());
                    if (bs.size() == 0) {
                        JOptionPane.showMessageDialog(rootPane, "Part of remote bookmarks tree cannot be deleted", "Remote Bookmark Delete", JOptionPane.OK_OPTION);
                    } else {
                        maybeDeleteBookmarks(bs);
                    }
                }
            }
        };
    }

    Action createExportFolderAction() {
        return new AbstractAction("Export Items...") {

            public void actionPerformed(ActionEvent e) {
                List<Bookmark> bs = model.getSelectedBookmarks(jTree1.getModel(), jTree1.getSelectionPaths());
                model.doExport(BookmarksManager.this, bs);
            }
        };
    }

    /**
     * returns true if the preference node exists.
     * @param nodeName
     * @return
     */
    public boolean hasPrefNode(String nodeName) {
        final File f = new File(AutoplotSettings.settings().resolveProperty(AutoplotSettings.PROP_AUTOPLOTDATA), "bookmarks/" + nodeName + ".xml");
        return f.exists();
    }

    String prefNode = null;

    /**
     * keep track of the bookmarks file.
     */
    File bookmarksFile = null;

    private boolean checkUnresolved(List<Bookmark> book) {
        boolean unresolved = false;
        for (Bookmark b : book) {
            if (b instanceof Bookmark.Folder) {
                Bookmark.Folder bf = (Bookmark.Folder) b;
                if (bf.remoteStatus == Bookmark.Folder.REMOTE_STATUS_NOT_LOADED) {
                    unresolved = true;
                }
            }
        }
        return unresolved;
    }

    private Runnable loadBooksRunnable(final String start, final int depthf) {
        Runnable run = new Runnable() {

            public void run() {
                try {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                    List<Bookmark> book = Bookmark.parseBookmarks(start, depthf);
                    model.setList(book);
                    int depthLimit = 5;
                    if (checkUnresolved(book) && depthf < depthLimit) {
                        Runnable run = loadBooksRunnable(start, depthf + 1);
                        RequestProcessor.invokeLater(run);
                    } else {
                        if (depthf >= depthLimit) {
                            System.err.println("remote bookmarks depth limit met");
                        }
                    }
                } catch (SAXException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    showMessage("XML error while parsing " + bookmarksFile + "\n" + ex.getMessage(), "Error while parsing bookmarks", JOptionPane.WARNING_MESSAGE);
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    showMessage("XML error while parsing " + bookmarksFile + "\n" + ex.getMessage(), "Error while parsing bookmarks", JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        return run;
    }

    /**
     * reload the bookmarks from disk.  Remote bookmarks will be reloaded slightly later.
     */
    public void reload() {
        setPrefNode(prefNode);
    }

    /**
     * setting this makes the manager the authority on bookmarks.
     * @param nodeName
     */
    public void setPrefNode(String nodeName) {
        prefNode = nodeName;
        BufferedReader read = null;
        try {
            File f2 = new File(AutoplotSettings.settings().resolveProperty(AutoplotSettings.PROP_AUTOPLOTDATA), "bookmarks/");
            if (!f2.exists()) {
                boolean ok = f2.mkdirs();
                if (!ok) {
                    throw new RuntimeException("unable to create folder " + f2);
                }
            }
            final File f = new File(f2, nodeName + ".xml");
            bookmarksFile = f;
            if (!f.exists()) {
                model.setList(new ArrayList<Bookmark>());
            } else {
                read = new BufferedReader(new FileReader(f));
                StringBuilder buff = new StringBuilder();
                String s = null;
                do {
                    if (s != null) buff.append(s).append("\n");
                    s = read.readLine();
                } while (s != null);
                int depth = 0;
                List<Bookmark> book = Bookmark.parseBookmarks(buff.toString(), depth);
                model.setList(book);
                boolean unresolved;
                unresolved = checkUnresolved(book);
                if (unresolved) {
                    final String start = buff.toString();
                    Runnable run = loadBooksRunnable(start, depth + 1);
                    RequestProcessor.invokeLater(run);
                }
            }
            model.addPropertyChangeListener(new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent evt) {
                    formatToFile(bookmarksFile);
                }
            });
        } catch (SAXException ex) {
            logger.log(Level.SEVERE, null, ex);
            showMessage("XML error while parsing " + bookmarksFile + "\n" + ex.getMessage(), "Error while parsing bookmarks", JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            showMessage("IO Error while parsing. " + bookmarksFile + "\n" + ex.getMessage(), "Error while parsing bookmarks", JOptionPane.WARNING_MESSAGE);
        } finally {
            try {
                if (read != null) read.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * rename the pref node, to aid with version changes.  E.g. convert autoplot.xml to bookmarks.xml
     * @param nodeName
     */
    public void resetPrefNode(String nodeName) {
        File f2 = new File(AutoplotSettings.settings().resolveProperty(AutoplotSettings.PROP_AUTOPLOTDATA), "bookmarks/");
        if (!f2.exists()) {
            boolean ok = f2.mkdirs();
            if (!ok) {
                throw new RuntimeException("unable to create folder " + f2);
            }
        }
        final File f = new File(f2, nodeName + ".xml");
        if (f.exists()) {
            throw new IllegalArgumentException("bookmarks pref node already exists: " + f);
        } else {
            formatToFile(f);
        }
        bookmarksFile = f;
        prefNode = nodeName;
    }

    private void formatToFile(File f) {
        logger.log(Level.FINE, "formatting {0}", f);
        OutputStream out = null;
        try {
            out = new FileOutputStream(f);
            Bookmark.formatBooks(out, model.getList());
            out.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void updateBookmarks(JMenu bookmarksMenu, final DataSetSelector dataSetSelector) {
        if (this.isVisible()) {
            menuIsDirty = true;
            dirtyMenu = bookmarksMenu;
            dirtySelector = dataSetSelector;
            return;
        }
        List<Bookmark> bookmarks = model.getList();
        bookmarksMenu.removeAll();
        JMenuItem mi;
        mi = new JMenuItem(new AbstractAction("Manage and Browse...") {

            public void actionPerformed(ActionEvent e) {
                Container parent = BookmarksManager.this.getParent();
                BookmarksManager.this.setLocationRelativeTo(parent);
                setVisible(true);
            }
        });
        mi.setToolTipText("Manage bookmarks, or select bookmark to plot");
        bookmarksMenu.add(mi);
        mi = new JMenuItem(new AbstractAction("Add Bookmark...") {

            public void actionPerformed(ActionEvent e) {
                Bookmark bookmark = addBookmark(dataSetSelector.getEditor().getText());
                setAddBookmark(bookmark);
                setVisible(true);
            }
        });
        mi.setToolTipText("Add the current URI to the bookmarks");
        bookmarksMenu.add(mi);
        bookmarksMenu.add(new JSeparator());
        if (bookmarks == null) {
            bookmarks = Collections.emptyList();
        }
        addBookmarks(bookmarksMenu, bookmarks, 0, dataSetSelector);
        menuIsDirty = false;
    }

    private void addBookmarks(JMenu bookmarksMenu, List<Bookmark> bookmarks, int treeDepth, final DataSetSelector select) {
        this.sel = select;
        DelayMenu.calculateMenu(bookmarksMenu, bookmarks, treeDepth, select);
        if (bookmarksMenu.isPopupMenuVisible()) {
            select.setMessage("Bookmarks updated");
        }
    }

    public Bookmark addBookmark(final String surl) {
        Bookmark.Item item = new Bookmark.Item(surl);
        URISplit split = URISplit.parse(surl);
        String autoTitle = split.file == null ? surl : split.file.substring(split.path.length());
        if (autoTitle.length() == 0) autoTitle = surl;
        item.setTitle(autoTitle);
        List<Bookmark> bookmarks = model.getList();
        if (bookmarks == null) bookmarks = new ArrayList<Bookmark>();
        List<Bookmark> newValue = new ArrayList<Bookmark>(bookmarks);
        if (newValue.contains(item)) {
            Bookmark.Item old = (Bookmark.Item) newValue.get(newValue.indexOf(item));
            item = old;
            newValue.remove(old);
        }
        newValue.add(item);
        if (prefNode == null) {
            Preferences prefs = Preferences.userNodeForPackage(ApplicationModel.class);
            prefs.put("bookmarks", Bookmark.formatBooks(newValue));
            try {
                prefs.flush();
            } catch (BackingStoreException ex) {
                ex.printStackTrace();
            }
        } else {
        }
        model.setList(newValue);
        formatToFile(bookmarksFile);
        return item;
    }

    public static void main(String[] args) {
        new BookmarksManager(null, false).setPrefNode("bookmarks");
    }

    /**
     * return true if we are already using the remote bookmark, marked as a remote bookmark,
     * at the root level.
     * @param bookmarksFile
     * @return true if we already have the bookmark.
     */
    public boolean haveRemoteBookmark(String bookmarksFile) {
        List<Bookmark> list = model.getList();
        for (Bookmark book : list) {
            if (book instanceof Bookmark.Folder) {
                String rurl = ((Bookmark.Folder) book).getRemoteUrl();
                if (rurl != null && rurl.equals(bookmarksFile)) {
                    return true;
                }
            }
        }
        return false;
    }
}
