package de.renier.vdr.channel.editor.actions;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import de.renier.vdr.channel.editor.ChannelEditor;

/**
 * AbstractSortAction
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public abstract class AbstractSortAction extends AbstractAction {

    public AbstractSortAction(String title) {
        super(title);
    }

    public void actionPerformed(ActionEvent e) {
        TreePath treePath = ChannelEditor.application.getChannelListingPanel().getLeadSelectionPath();
        if (treePath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            if (node.isRoot()) {
                Enumeration enumer = node.children();
                while (enumer.hasMoreElements()) {
                    doSorting((DefaultMutableTreeNode) enumer.nextElement());
                }
                doSorting(node);
            } else {
                doSorting(node);
            }
            ChannelEditor.application.getChannelListingPanel().treeNodeStructureChanged(node);
        }
    }

    /**
   * @param node
   */
    private void doSorting(DefaultMutableTreeNode node) {
        Enumeration enumer = node.children();
        List sortList = new LinkedList();
        while (enumer.hasMoreElements()) {
            sortList.add(enumer.nextElement());
        }
        Collections.sort(sortList, createSortComparator());
        Iterator it = sortList.iterator();
        while (it.hasNext()) {
            node.add((DefaultMutableTreeNode) it.next());
        }
    }

    protected abstract Comparator createSortComparator();
}
