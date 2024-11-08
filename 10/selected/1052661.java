package org.adempiere.webui.component;

import org.adempiere.webui.window.FDialog;
import org.compiere.model.CTreeNode;
import org.compiere.model.MTree;
import org.compiere.util.CLogger;
import org.compiere.util.Trx;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.SimpleTreeNode;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

/**
 * 
 * @author Low Heng Sin
 *
 */
public class ADTreeOnDropListener implements EventListener {

    private SimpleTreeModel treeModel;

    private MTree mTree;

    private int windowNo;

    private Tree tree;

    private static final CLogger log = CLogger.getCLogger(ADTreeOnDropListener.class);

    /**
	 * 
	 * @param tree
	 * @param model
	 * @param mTree
	 * @param windowNo
	 */
    public ADTreeOnDropListener(Tree tree, SimpleTreeModel model, MTree mTree, int windowNo) {
        this.tree = tree;
        this.treeModel = model;
        this.mTree = mTree;
        this.windowNo = windowNo;
    }

    /**
	 * @param event
	 */
    public void onEvent(Event event) throws Exception {
        if (event instanceof DropEvent) {
            DropEvent de = (DropEvent) event;
            log.fine("Source=" + de.getDragged() + " Target=" + de.getTarget());
            if (de.getDragged() != de.getTarget()) {
                Treeitem src = (Treeitem) ((Treerow) de.getDragged()).getParent();
                Treeitem target = (Treeitem) ((Treerow) de.getTarget()).getParent();
                moveNode((SimpleTreeNode) src.getValue(), (SimpleTreeNode) target.getValue());
            }
        }
    }

    /**
	 *	Move TreeNode
	 *	@param	movingNode	The node to be moved
	 *	@param	toNode		The target node
	 */
    private void moveNode(SimpleTreeNode movingNode, SimpleTreeNode toNode) {
        log.info(movingNode.toString() + " to " + toNode.toString());
        if (movingNode == toNode) return;
        CTreeNode toMNode = (CTreeNode) toNode.getData();
        if (!toMNode.isSummary()) {
            moveNode(movingNode, toNode, false);
        } else {
            int path[] = treeModel.getPath(treeModel.getRoot(), toNode);
            Treeitem toItem = tree.renderItemByPath(path);
            tree.setSelectedItem(toItem);
            Events.sendEvent(tree, new Event(Events.ON_SELECT, tree));
            MenuListener listener = new MenuListener(movingNode, toNode);
            Menupopup popup = new Menupopup();
            Menuitem menuItem = new Menuitem("Insert After");
            menuItem.setValue("InsertAfter");
            menuItem.setParent(popup);
            menuItem.addEventListener(Events.ON_CLICK, listener);
            menuItem = new Menuitem("Move Into");
            menuItem.setValue("MoveInto");
            menuItem.setParent(popup);
            menuItem.addEventListener(Events.ON_CLICK, listener);
            popup.setPage(tree.getPage());
            popup.open(toItem.getTreerow());
        }
    }

    private void moveNode(SimpleTreeNode movingNode, SimpleTreeNode toNode, boolean moveInto) {
        SimpleTreeNode newParent;
        int index;
        SimpleTreeNode oldParent = treeModel.getParent(movingNode);
        treeModel.removeNode(movingNode);
        if (!moveInto) {
            newParent = treeModel.getParent(toNode);
            index = newParent.getChildren().indexOf(toNode) + 1;
        } else {
            newParent = toNode;
            index = 0;
        }
        treeModel.addNode(newParent, movingNode, index);
        int path[] = treeModel.getPath(treeModel.getRoot(), movingNode);
        Treeitem movingItem = tree.renderItemByPath(path);
        tree.setSelectedItem(movingItem);
        Events.sendEvent(tree, new Event(Events.ON_SELECT, tree));
        Trx trx = org.compierezk.util.Trx.get("ADTree");
        try {
            CTreeNode oldMParent = (CTreeNode) oldParent.getData();
            for (int i = 0; i < oldParent.getChildCount(); i++) {
                SimpleTreeNode nd = (SimpleTreeNode) oldParent.getChildAt(i);
                CTreeNode md = (CTreeNode) nd.getData();
                StringBuffer sql = new StringBuffer("UPDATE ");
                sql.append(mTree.getNodeTableName()).append(" SET Parent_ID=").append(oldMParent.getNode_ID()).append(", SeqNo=").append(i).append(", Updated=SysDate").append(" WHERE AD_Tree_ID=").append(mTree.getAD_Tree_ID()).append(" AND Node_ID=").append(md.getNode_ID());
                log.fine(sql.toString());
                org.compierezk.util.DB.executeUpdate(sql.toString(), trx);
            }
            if (oldParent != newParent) {
                CTreeNode newMParent = (CTreeNode) newParent.getData();
                for (int i = 0; i < newParent.getChildCount(); i++) {
                    SimpleTreeNode nd = (SimpleTreeNode) newParent.getChildAt(i);
                    CTreeNode md = (CTreeNode) nd.getData();
                    StringBuffer sql = new StringBuffer("UPDATE ");
                    sql.append(mTree.getNodeTableName()).append(" SET Parent_ID=").append(newMParent.getNode_ID()).append(", SeqNo=").append(i).append(", Updated=SysDate").append(" WHERE AD_Tree_ID=").append(mTree.getAD_Tree_ID()).append(" AND Node_ID=").append(md.getNode_ID());
                    log.fine(sql.toString());
                    org.compierezk.util.DB.executeUpdate(sql.toString(), trx);
                }
            }
            trx.commit();
        } catch (Exception e) {
            trx.rollback();
            FDialog.error(windowNo, tree, "TreeUpdateError", e.getLocalizedMessage());
        }
        trx.close();
        trx = null;
    }

    class MenuListener implements EventListener {

        private SimpleTreeNode movingNode;

        private SimpleTreeNode toNode;

        MenuListener(SimpleTreeNode movingNode, SimpleTreeNode toNode) {
            this.movingNode = movingNode;
            this.toNode = toNode;
        }

        public void onEvent(Event event) throws Exception {
            if (Events.ON_CLICK.equals(event.getName()) && event.getTarget() instanceof Menuitem) {
                Menuitem menuItem = (Menuitem) event.getTarget();
                if ("InsertAfter".equals(menuItem.getValue())) {
                    moveNode(movingNode, toNode, false);
                } else if ("MoveInto".equals(menuItem.getValue())) {
                    moveNode(movingNode, toNode, true);
                }
            }
        }
    }
}
