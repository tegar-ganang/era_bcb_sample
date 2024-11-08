package org.rdv.ui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.rdv.rbnb.RBNBUtilities;
import com.rbnb.sapi.ChannelTree;
import com.rbnb.sapi.ChannelTree.NodeTypeEnum;

/**
 * A tree model based on a channel tree.
 * 
 * @author Jason P. Hanley
 */
public class ChannelTreeModel implements TreeModel {

    /** the channel tree object */
    private ChannelTree ctree;

    /** the root of the tree */
    private final Object root;

    /** the string used to filter the channel list */
    private String filterText;

    /** flag for showing hidden channels */
    boolean hiddenChannelsVisible;

    /** list of tree model listeners */
    private List<TreeModelListener> treeModelListeners;

    /** the bound property support class */
    private PropertyChangeSupport pcs;

    /**
   * Creates the tree model with an empty tree. By default no filter is used and
   * hidden channels are not shown.
   */
    public ChannelTreeModel() {
        this(ChannelTree.EMPTY_TREE);
    }

    /**
   * Creates the tree model with the given tree. By default no filter is used
   * and hidden channels are not shown.
   * @param ctree
   */
    public ChannelTreeModel(ChannelTree ctree) {
        if (ctree != null) {
            this.ctree = ctree;
        } else {
            this.ctree = ChannelTree.EMPTY_TREE;
        }
        root = new Object();
        filterText = "";
        hiddenChannelsVisible = false;
        treeModelListeners = new ArrayList<TreeModelListener>();
        pcs = new PropertyChangeSupport(this);
    }

    /**
   * Gets the channel tree for this model.
   * 
   * @return  the channel tree
   */
    public ChannelTree getChannelTree() {
        return ctree;
    }

    /**
   * Sets the channel tree for this model.
   *  
   * @param ctree  the new channel tree
   * @return       true if the tree new structure is different from the old 
   */
    public boolean setChannelTree(ChannelTree ctree) {
        if (ctree == null) {
            ctree = ChannelTree.EMPTY_TREE;
        }
        if (this.ctree == ctree) {
            return false;
        }
        boolean structureChanged = isStructureChanged(ctree);
        this.ctree = ctree;
        if (structureChanged) {
            fireTreeStructureChanged();
        } else {
            fireTreeNodesChanged();
        }
        return structureChanged;
    }

    /**
   * Compares the current channel tree with the new channel tree to see if there
   * are any structural differences.
   * 
   * @param newChannelTree  the new channel tree
   * @return                true if that are structural differences, false
   *                        otherwise
   */
    private boolean isStructureChanged(ChannelTree newChannelTree) {
        boolean channelListChanged = false;
        Iterator oldIterator = ctree.iterator();
        while (oldIterator.hasNext()) {
            ChannelTree.Node node = (ChannelTree.Node) oldIterator.next();
            NodeTypeEnum type = node.getType();
            if (type == ChannelTree.CHANNEL || type == ChannelTree.FOLDER || type == ChannelTree.SERVER || type == ChannelTree.SOURCE || type == ChannelTree.PLUGIN) {
                if (newChannelTree.findNode(node.getFullName()) == null) {
                    channelListChanged = true;
                    break;
                }
            }
        }
        if (!channelListChanged) {
            Iterator newIterator = newChannelTree.iterator();
            while (newIterator.hasNext()) {
                ChannelTree.Node node = (ChannelTree.Node) newIterator.next();
                NodeTypeEnum type = node.getType();
                if (type == ChannelTree.CHANNEL || type == ChannelTree.FOLDER || type == ChannelTree.SERVER || type == ChannelTree.SOURCE || type == ChannelTree.PLUGIN) {
                    if (ctree.findNode(node.getFullName()) == null) {
                        channelListChanged = true;
                        break;
                    }
                }
            }
        }
        return channelListChanged;
    }

    /**
   * Gets the root of the tree.
   * 
   * @return  the root of the tree
   */
    public Object getRoot() {
        return root;
    }

    /**
   * Gets the child of the object at the given index.
   * 
   * @param n      the parent object
   * @param index  the child index
   * @return       the child object
   */
    public Object getChild(Object n, int index) {
        return getSortedChildren(n).get(index);
    }

    /**
   * Gets the number of children for the object.
   * 
   * @param n  the object
   * @return   the number of children
   */
    public int getChildCount(Object n) {
        return getSortedChildren(n).size();
    }

    /**
   * Returns whether the object is a leaf.
   * 
   * @param n  the object
   * @return   true if the object is a leaf, false otherwise
   */
    public boolean isLeaf(Object n) {
        boolean isLeaf = false;
        if (n != root) {
            ChannelTree.Node node = (ChannelTree.Node) n;
            if (node.getType() == ChannelTree.CHANNEL) {
                isLeaf = true;
            }
        }
        return isLeaf;
    }

    public void valueForPathChanged(TreePath path, Object n) {
    }

    /**
   * Gets the child index for the object.
   * 
   * @param n      the parent object
   * @param child  the child object
   * @return       the index of the child object
   */
    public int getIndexOfChild(Object n, Object child) {
        return getSortedChildren(n).indexOf(n);
    }

    /**
   * Gets a sorted list of children for the object.
   * 
   * @param n  the object
   * @return   a list of children, sorted
   */
    private List getSortedChildren(Object n) {
        List children;
        if (n == root) {
            children = RBNBUtilities.getSortedChildren(ctree, hiddenChannelsVisible);
        } else {
            ChannelTree.Node node = (ChannelTree.Node) n;
            children = RBNBUtilities.getSortedChildren(node, hiddenChannelsVisible);
        }
        if (filterText.length() > 0) {
            try {
                String regex = ".*" + filterText.replaceAll("\\*", ".*") + ".*";
                Pattern p = Pattern.compile(regex);
                for (int i = children.size() - 1; i >= 0; i--) {
                    ChannelTree.Node child = (ChannelTree.Node) children.get(i);
                    String fullName = child.getFullName().toLowerCase();
                    if (!p.matcher(fullName).matches() && getSortedChildren(child).size() == 0) {
                        children.remove(i);
                    }
                }
            } catch (PatternSyntaxException e) {
            }
        }
        return children;
    }

    /**
   * Gets the filter for this model.
   * 
   * @return  the filter text
   */
    public String getFilter() {
        return filterText;
    }

    /**
   * Sets the filter for this model.
   * 
   * @param filterText  the filter text
   */
    public void setFilter(String filterText) {
        if (filterText == null) {
            filterText = "";
        } else {
            filterText = filterText.toLowerCase();
        }
        if (this.filterText.equals(filterText)) {
            return;
        }
        String oldFilterText = this.filterText;
        this.filterText = filterText;
        fireTreeStructureChanged();
        pcs.firePropertyChange("filter", oldFilterText, filterText);
    }

    /**
   * Returns whether hidden channels are visible.
   * 
   * @return  true if hidden channels are visible, false otherwise
   */
    public boolean isHiddenChannelsVisible() {
        return hiddenChannelsVisible;
    }

    /**
   * Sets whether hidden channels are visible.
   * 
   * @param hiddenChannelsVisible  flag for hidden channel visibility
   */
    public void setHiddenChannelsVisible(boolean hiddenChannelsVisible) {
        if (this.hiddenChannelsVisible == hiddenChannelsVisible) {
            return;
        }
        boolean oldHiddenChannelsVisible = hiddenChannelsVisible;
        this.hiddenChannelsVisible = hiddenChannelsVisible;
        fireTreeStructureChanged();
        pcs.firePropertyChange("hiddenChannelsVisible", oldHiddenChannelsVisible, hiddenChannelsVisible);
    }

    public void addTreeModelListener(TreeModelListener l) {
        treeModelListeners.add(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        treeModelListeners.remove(l);
    }

    /**
   * Fires the tree structure changed event to all listeners. 
   */
    private void fireTreeStructureChanged() {
        TreeModelEvent e = new TreeModelEvent(this, new Object[] { root });
        for (TreeModelListener listener : treeModelListeners) {
            listener.treeStructureChanged(e);
        }
    }

    /**
   * Fires the tree nodes changed event to all listeners.
   */
    private void fireTreeNodesChanged() {
        TreeModelEvent e = new TreeModelEvent(this, new Object[] { root });
        for (TreeModelListener listener : treeModelListeners) {
            listener.treeNodesChanged(e);
        }
    }

    /**
   * Add a property change listener.
   * 
   * @param listener  the property change listener to add
   */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    /**
   * Remove a property change listener.
   * 
   * @param listener  the property change listener to remove
   */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}
