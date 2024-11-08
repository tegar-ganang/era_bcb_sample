package org.virbo.autoplot.bookmarks;

import org.virbo.autoplot.*;
import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author jbf
 */
public class BookmarksManagerModel {

    private static final Logger logger = Logger.getLogger("autoplot.bookmarks");

    protected void doImport(Component c) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {

            public boolean accept(File f) {
                if (f.toString() == null) return false;
                return f.isDirectory() || f.getName().endsWith(".xml");
            }

            public String getDescription() {
                return "bookmarks files (*.xml)";
            }
        });
        int r = chooser.showOpenDialog(c);
        if (r == JFileChooser.APPROVE_OPTION) {
            try {
                List<Bookmark> importBook = Bookmark.parseBookmarks(AutoplotUtil.readDoc(new FileInputStream(chooser.getSelectedFile())).getDocumentElement());
                List<Bookmark> newList = new ArrayList(this.list.size());
                for (int i = 0; i < this.list.size(); i++) {
                    newList.add(i, this.list.get(i).copy());
                }
                mergeList(importBook, newList);
                setList(newList);
            } catch (SAXException ex) {
                Logger.getLogger(BookmarksManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(BookmarksManager.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParserConfigurationException ex) {
                Logger.getLogger(BookmarksManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    protected void doExport(Component c) {
        doExport(c, getList());
    }

    protected void doExport(Component c, List<Bookmark> list) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {

            public boolean accept(File f) {
                if (f.toString() == null) return false;
                return f.isDirectory() || f.getName().endsWith(".xml");
            }

            public String getDescription() {
                return "bookmarks files (*.xml)";
            }
        });
        int r = chooser.showSaveDialog(c);
        if (r == JFileChooser.APPROVE_OPTION) {
            FileOutputStream out = null;
            try {
                File f = chooser.getSelectedFile();
                if (!f.toString().endsWith(".xml")) f = new File(f.toString() + ".xml");
                out = new FileOutputStream(f);
                Bookmark.formatBooks(out, list);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) try {
                    out.close();
                } catch (IOException ex) {
                    Logger.getLogger(BookmarksManagerModel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    protected List<Bookmark> list = null;

    public static final String PROP_LIST = "list";

    /**
     * the contents of a bookmark changed, like the title or URL.
     */
    public static final String PROP_BOOKMARK = "bookmark";

    public List<Bookmark> getList() {
        return list;
    }

    public void setList(List<Bookmark> list) {
        logger.log(Level.FINE, "setting list to {0}", list);
        this.list = list;
        propertyChangeSupport.firePropertyChange(PROP_LIST, null, list);
    }

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public TreeModel getTreeModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Bookmarks");
        DefaultTreeModel model = new DefaultTreeModel(root);
        if (this.list != null) addChildNodes(root, this.list);
        return model;
    }

    /**
     * return the equal bookmark from the list, traverse tree
     * @param newList
     * @param context
     * @return
     */
    Bookmark.Folder getFolder(List<Bookmark> newList, Bookmark context) {
        for (int i = 0; i < newList.size(); i++) {
            Bookmark item = newList.get(i);
            if (item.equals(context)) {
                return (Bookmark.Folder) item;
            } else if (item instanceof Bookmark.Folder) {
                Bookmark.Folder sub = getFolder(((Bookmark.Folder) item).getBookmarks(), context);
                if (sub != null) return sub;
            } else {
            }
        }
        return null;
    }

    void addBookmarks(List<Bookmark> bookmarks, Bookmark context, boolean insert) {
        ArrayList<Bookmark> newList = new ArrayList<Bookmark>(this.list.size());
        for (Bookmark b : this.list) {
            newList.add((Bookmark) b.copy());
        }
        boolean containsFolder = false;
        for (Bookmark b : bookmarks) {
            containsFolder = containsFolder || b instanceof Bookmark.Folder;
        }
        if (context == null) {
            if (newList.contains(null)) {
                newList.addAll(newList.indexOf(null) + (insert ? 0 : 1), bookmarks);
            } else {
                newList.addAll(bookmarks);
            }
        } else if (context instanceof Bookmark.Folder) {
            Bookmark.Folder newFolder = getFolder(newList, context);
            newFolder.getBookmarks().addAll(bookmarks);
        } else {
            if (newList.contains(context)) {
                newList.addAll(newList.indexOf(context) + (insert ? 0 : 1), bookmarks);
            } else {
                boolean isAdded = false;
                for (Bookmark b : newList) {
                    if (b instanceof Bookmark.Folder) {
                        List<Bookmark> bs = ((Bookmark.Folder) b).getBookmarks();
                        if (!isAdded && bs.contains(context)) {
                            bs.addAll(bs.indexOf(context) + (insert ? 0 : 1), bookmarks);
                            isAdded = true;
                        }
                    }
                }
                if (isAdded == false) newList.addAll(bookmarks);
            }
        }
        setList(newList);
    }

    void addBookmark(Bookmark bookmark, Bookmark context) {
        addBookmarks(Collections.singletonList(bookmark), context, false);
    }

    void insertBookmark(Bookmark bookmark, Bookmark context) {
        addBookmarks(Collections.singletonList(bookmark), context, true);
    }

    /**
     * return the first item with this name and type, or null.  This does
     * not recurse through the folders.
     * @param oldList
     * @param title
     * @return
     */
    private Bookmark findItem(List<Bookmark> oldList, String title, boolean findFolder) {
        for (int i = 0; i < oldList.size(); i++) {
            final Bookmark item = oldList.get(i);
            boolean isFolder = item instanceof Bookmark.Folder;
            if ((findFolder == isFolder) && oldList.get(i).getTitle().equals(title)) {
                return item;
            }
        }
        return null;
    }

    /**
     * merge in the bookmarks.  Items with the same title are repeated, and
     * folders with the same name are merged.  When merging in a remote folder,
     * the entire folder is replaced.
     * @param src the items to merge in
     * @param dest the list to update.
     */
    public void mergeList(List<Bookmark> src, List<Bookmark> dest) {
        if (src.size() == 0) return;
        for (int i = 0; i < src.size(); i++) {
            Bookmark item = src.get(i);
            if (item instanceof Bookmark.Folder) {
                String folderName = item.getTitle();
                Bookmark.Folder old = (Bookmark.Folder) findItem(dest, folderName, true);
                Bookmark.Folder itemBook = (Bookmark.Folder) item;
                if (old != null) {
                    int indx;
                    boolean replace;
                    indx = dest.indexOf(old);
                    replace = old.remoteUrl != null;
                    if (itemBook.remoteUrl == null) {
                        mergeList(((Bookmark.Folder) item).getBookmarks(), old.getBookmarks());
                    } else {
                        Bookmark.Folder parent = old.getParent();
                        if (parent == null) {
                            dest.add(indx + 1, itemBook);
                            if (replace) dest.remove(indx);
                        } else {
                            List<Bookmark> list = parent.getBookmarks();
                            indx = list.indexOf(old);
                            list.add(indx + 1, itemBook);
                            if (replace) list.remove(indx);
                        }
                    }
                } else {
                    dest.add(item);
                }
            } else {
                String id = item.getTitle();
                Bookmark.Item old = (Bookmark.Item) findItem(dest, id, false);
                if (old != null) {
                    if (old.equals(item)) continue;
                } else {
                    dest.add(item);
                }
            }
        }
    }

    /**
     * kludge to trigger list change when a title is changed.
     */
    void fireBookmarkChange(Bookmark book) {
        propertyChangeSupport.firePropertyChange(PROP_BOOKMARK, null, book);
    }

    TreePath getPathFor(Bookmark b, TreeModel model, TreePath root) {
        if (root == null) return null;
        final Object parent = root.getLastPathComponent();
        final int childCount = model.getChildCount(parent);
        for (int ii = 0; ii < childCount; ii++) {
            final Object child = model.getChild(parent, ii);
            if (b.equals(((DefaultMutableTreeNode) child).getUserObject())) {
                b.equals(((DefaultMutableTreeNode) child).getUserObject());
                return root.pathByAddingChild(child);
            }
            if (model.getChildCount(child) > 0) {
                TreePath childResult = getPathFor(b, model, root.pathByAddingChild(child));
                if (childResult != null) return childResult;
            }
        }
        return null;
    }

    Bookmark.Folder removeBookmarks(Bookmark.Folder folder, Bookmark book) {
        ArrayList<Bookmark> newList = new ArrayList<Bookmark>(folder.getBookmarks().size());
        for (Bookmark b : folder.getBookmarks()) {
            newList.add((Bookmark) b.copy());
        }
        for (int i = 0; i < newList.size(); i++) {
            Bookmark bookmark = newList.get(i);
            if (bookmark instanceof Bookmark.Folder) {
                if (bookmark.equals(book)) {
                    newList.set(i, null);
                } else {
                    bookmark = removeBookmarks((Bookmark.Folder) bookmark, book);
                    newList.set(i, bookmark);
                }
            } else {
                if (bookmark.equals(book)) {
                    newList.set(i, null);
                }
            }
        }
        newList.removeAll(Collections.singleton(null));
        Bookmark.Folder result = new Bookmark.Folder(folder.getTitle());
        result.bookmarks = newList;
        return result;
    }

    /**
     * remove the listed bookmarks.
     * TODO: there is still a problem, where we do not check that the bookmark is part of a remoteBookmarks.
     * @param bookmarks
     */
    void removeBookmarks(List<Bookmark> bookmarks) {
        ArrayList<Bookmark> newList = new ArrayList<Bookmark>(this.list.size());
        for (Bookmark b : this.list) {
            newList.add((Bookmark) b.copy());
        }
        for (Bookmark bookmark : bookmarks) {
            if (newList.contains(bookmark)) {
                newList.remove(bookmark);
            } else {
                int i = 0;
                for (Bookmark b2 : newList) {
                    if (b2 instanceof Bookmark.Folder) {
                        b2 = removeBookmarks((Bookmark.Folder) b2, bookmark);
                        newList.set(i, b2);
                    } else {
                        newList.set(i, null);
                    }
                    i++;
                }
            }
        }
        newList.removeAll(Collections.singleton(null));
        setList(newList);
    }

    void removeBookmark(Bookmark bookmark) {
        removeBookmarks(Collections.singletonList(bookmark));
    }

    private void addChildNodes(MutableTreeNode parent, List<Bookmark> bookmarks) {
        for (Bookmark b : bookmarks) {
            String node = b.toString();
            if (b instanceof Bookmark.Folder) {
                if (((Bookmark.Folder) b).remoteUrl != null && ((Bookmark.Folder) b).remoteUrl.length() > 0) {
                    node = node + String.format(" [remoteUrl=%s]", ((Bookmark.Folder) b).remoteUrl);
                }
            }
            final String fnode = node;
            MutableTreeNode child = new DefaultMutableTreeNode(b) {

                @Override
                public String toString() {
                    return fnode;
                }
            };
            parent.insert(child, parent.getChildCount());
            if (b instanceof Bookmark.Folder) {
                List<Bookmark> kids = ((Bookmark.Folder) b).getBookmarks();
                if (kids.size() == 0) {
                    child.insert(new DefaultMutableTreeNode("(empty)"), 0);
                } else {
                    addChildNodes(child, kids);
                }
            }
        }
    }

    protected Bookmark getSelectedBookmark(TreeModel model, TreePath path) {
        if (path == null || path.getPathCount() == 1) return null;
        Object sel = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        if (sel.equals("(empty)")) {
            return getSelectedBookmark(model, path.getParentPath());
        }
        return (Bookmark) sel;
    }

    protected List<Bookmark> getSelectedBookmarks(TreeModel model, TreePath[] paths) {
        List<Bookmark> result = new ArrayList<Bookmark>();
        if (paths == null) return result;
        for (TreePath path : paths) {
            if (path == null) return null;
            if (path.getPathCount() == 1) return list;
            Object sel = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            Bookmark b;
            if (sel.equals("(empty)")) {
                b = getSelectedBookmark(model, path.getParentPath());
            } else {
                b = (Bookmark) sel;
            }
            if (b != null) result.add(b);
        }
        return result;
    }

    void doImportUrl(Component c) {
        String ansr = null;
        URL url = null;
        boolean okay = false;
        while (okay == false) {
            String s;
            if (ansr == null) {
                s = JOptionPane.showInputDialog(c, "Enter the URL of a bookmarks file:", "");
            } else {
                s = JOptionPane.showInputDialog(c, "Whoops, Enter the URL of a bookmarks file:", ansr);
            }
            if (s == null) {
                return;
            } else {
                try {
                    ansr = s;
                    url = new URL(s);
                    okay = true;
                } catch (MalformedURLException ex) {
                }
            }
        }
        try {
            Document doc = AutoplotUtil.readDoc(url.openStream());
            List<Bookmark> importBook = Bookmark.parseBookmarks(doc.getDocumentElement());
            importList(importBook);
        } catch (SAXException ex) {
            Logger.getLogger(BookmarksManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BookmarksManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(BookmarksManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void importList(List<Bookmark> books) {
        List<Bookmark> newList = new ArrayList(this.list.size());
        for (int i = 0; i < this.list.size(); i++) {
            newList.add(i, this.list.get(i).copy());
        }
        mergeList(books, newList);
        setList(newList);
    }

    public void addRemoteBookmarks(String surl) throws MalformedURLException, SAXException, ParserConfigurationException, IOException {
        addRemoteBookmarks(surl, null);
    }

    /**
     * add the remote bookmarks.  An ordinary bookmarks file downloaded from
     * a website can be tacked on to a user's existing bookmarks, and updates
     * to the file will be visible on the client's bookmark list.
     * 
     * @param surl
     * @param selectedBookmark location to add the bookmark, can be null.
     * @throws MalformedURLException
     */
    public void addRemoteBookmarks(String surl, Bookmark selectedBookmark) {
        List<Bookmark> importBook = new ArrayList(100);
        RemoteStatus remote = Bookmark.getRemoteBookmarks(surl, 1, true, importBook);
        if (importBook.size() != 1) {
            throw new IllegalArgumentException("Remote bookmarks file contains more than one root folder: " + surl);
        }
        if (remote.remoteRemote == true) {
            System.err.println("remote bookmarks found...");
        }
        List<Bookmark> newList = new ArrayList(this.list.size());
        for (int i = 0; i < this.list.size(); i++) {
            newList.add(i, this.list.get(i).copy());
        }
        List<Bookmark> copy = new ArrayList();
        for (int i = 0; i < importBook.size(); i++) {
            Bookmark m = importBook.get(i);
            if (m instanceof Bookmark.Folder) {
                Bookmark.Folder bf = (Bookmark.Folder) m;
                if (bf.getRemoteUrl() == null) {
                    bf.setRemoteUrl(surl);
                }
                copy.add(m);
            } else if (m.getTitle().equals(Bookmark.TITLE_ERROR_OCCURRED)) {
                copy.add(m);
            }
        }
        mergeList(copy, newList);
        setList(newList);
    }
}
