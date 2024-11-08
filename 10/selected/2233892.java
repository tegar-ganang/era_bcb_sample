package org.compiere.grid.tree;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.*;
import org.adempiere.plaf.AdempierePLAF;
import org.compiere.apps.*;
import org.compiere.model.*;
import org.compiere.swing.*;
import org.compiere.util.*;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;

/**
 *  Tree Panel displays trees.
 *  <br>
 *	When a node is selected, a propertyChange (NODE_SELECTION) event is fired
 *  <pre>
 *		PropertyChangeListener -
 *			treePanel.addPropertyChangeListener(VTreePanel.NODE_SELECTION, this);
 *			calls: public void propertyChange(PropertyChangeEvent e)
 *  </pre>
 *  To select a specific node call
 *      setSelectedNode(NodeID);
 *
 *  @author 	Jorg Janke
 *  @version 	$Id: VTreePanel.java,v 1.3 2006/07/30 00:51:28 jjanke Exp $
 */
public final class VTreePanel extends CPanel implements ActionListener, DragGestureListener, DragSourceListener, DropTargetListener {

    protected boolean m_lookAndFeelChanged = false;

    /**
	 *  Tree Panel for browsing and editing of a tree.
	 *  Need to call initTree
	 *  @param  WindowNo	WindowNo
	 *  @param  editable    if true you can edit it
	 *  @param  hasBar      has OutlookBar
	 */
    public VTreePanel(int WindowNo, boolean hasBar, boolean editable) {
        super();
        toolbar = new ArrayList<JToolBar>();
        log.config("Bar=" + hasBar + ", Editable=" + editable);
        m_WindowNo = WindowNo;
        m_hasBar = hasBar;
        m_editable = editable;
        jbInit();
        if (!hasBar) {
            barScrollPane.setPreferredSize(new Dimension(0, 0));
            barScrollPane.setMaximumSize(new Dimension(0, 0));
            barScrollPane.setMinimumSize(new Dimension(0, 0));
            centerSplitPane.setDividerLocation(0);
            centerSplitPane.setDividerSize(0);
            popMenuTree.remove(mBarAdd);
        } else {
            centerSplitPane.setDividerLocation(80);
            UIManager.addPropertyChangeListener(new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent evt) {
                    if ("lookAndFeel".equals(evt.getPropertyName())) m_lookAndFeelChanged = true;
                }
            });
        }
        if (editable) tree.setDropTarget(dropTarget); else {
            popMenuTree.remove(mFrom);
            popMenuTree.remove(mTo);
        }
    }

    /**
	 *  Tree initialization.
	 * 	May be called several times
	 *	@param	AD_Tree_ID	tree to load
	 *  @return true if loaded ok
	 */
    public boolean initTree(int AD_Tree_ID) {
        log.config("AD_Tree_ID=" + AD_Tree_ID);
        m_AD_Tree_ID = AD_Tree_ID;
        MTree vTree = new MTree(Env.getCtx(), AD_Tree_ID, m_editable, true, null);
        m_root = vTree.getRoot();
        log.config("root=" + m_root);
        m_nodeTableName = vTree.getNodeTableName();
        treeModel = new DefaultTreeModel(m_root, true);
        tree.setModel(treeModel);
        if (m_hasBar) {
            for (JToolBar jt : toolbar) jt.removeAll();
            toolbarMap = new HashMap<Integer, JToolBar>();
            Enumeration enTop = m_root.children();
            JToolBar jt = null;
            Map<JToolBar, String> titleMap = new HashMap<JToolBar, String>();
            while (enTop.hasMoreElements()) {
                MTreeNode ndTop = (MTreeNode) enTop.nextElement();
                Enumeration en = ndTop.preorderEnumeration();
                boolean labelDrawn = false;
                while (en.hasMoreElements()) {
                    MTreeNode nd = (MTreeNode) en.nextElement();
                    if (nd.isOnBar()) {
                        if (!labelDrawn) {
                            jt = new JToolBar(JToolBar.VERTICAL);
                            titleMap.put(jt, ndTop.toString().trim());
                            labelDrawn = true;
                            toolbarMap.put(ndTop.getNode_ID(), jt);
                        }
                        addToBar(nd, jt, false);
                    }
                }
                if (jt != null) toolbar.add(jt);
                jt = null;
            }
            for (JToolBar jt2 : toolbar) {
                jt2.setOpaque(false);
                jt2.setFloatable(false);
                jt2.setRollover(true);
                jt2.setBorder(BorderFactory.createEmptyBorder());
                JXTaskPane barPart = new JXTaskPane();
                barPart.setAnimated(true);
                barPart.setLayout(new BorderLayout());
                barPart.add(jt2, BorderLayout.NORTH);
                barPart.setTitle(titleMap.get(jt2));
                bar.add(barPart);
            }
        }
        return true;
    }

    /**	Logger			*/
    private static CLogger log = CLogger.getCLogger(VTreePanel.class);

    private BorderLayout mainLayout = new BorderLayout();

    private JTree tree = new JTree();

    private DefaultTreeModel treeModel;

    private DefaultTreeSelectionModel treeSelect = new DefaultTreeSelectionModel();

    private CPanel southPanel = new CPanel();

    private CCheckBox treeExpand = new CCheckBox();

    private CTextField treeSearch = new CTextField(10);

    private CLabel treeSearchLabel = new CLabel();

    private JPopupMenu popMenuTree = new JPopupMenu();

    private JPopupMenu popMenuBar = new JPopupMenu();

    private CMenuItem mFrom = new CMenuItem();

    private CMenuItem mTo = new CMenuItem();

    private JXTaskPaneContainer bar = new JXTaskPaneContainer();

    private java.util.List<JToolBar> toolbar;

    private HashMap<Integer, JToolBar> toolbarMap;

    private int toolBarCols = 3;

    private CMenuItem mBarAdd = new CMenuItem();

    private CMenuItem mBarRemove = new CMenuItem();

    private BorderLayout southLayout = new BorderLayout();

    private JSplitPane centerSplitPane = new JSplitPane();

    private JScrollPane treePane = new JScrollPane();

    private MouseListener mouseListener = new VTreePanel_mouseAdapter(this);

    private KeyListener keyListener = new VTreePanel_keyAdapter(this);

    private int m_WindowNo;

    /** Tree ID                     */
    private int m_AD_Tree_ID = 0;

    /** Table Name for TreeNode     */
    private String m_nodeTableName = null;

    /** Tree is editable (can move nodes) - also not active shown   */
    private boolean m_editable;

    /** Tree has a shortcut Bar     */
    private boolean m_hasBar;

    /** The root node               */
    private MTreeNode m_root = null;

    private MTreeNode m_moveNode;

    private String m_search = "";

    private Enumeration m_nodeEn;

    private MTreeNode m_selectedNode;

    private CButton m_buttonSelected;

    private JScrollPane barScrollPane;

    /**	Property Listener NodeSelected			*/
    public static final String NODE_SELECTION = "NodeSelected";

    /**
	 *  Static Component initialization.
	 *  <pre>
	 *  - centerSplitPane
	 *      - treePane
	 *          - tree
	 *      - bar
	 *  - southPanel
	 *  </pre>
	 */
    private void jbInit() {
        this.setLayout(mainLayout);
        mainLayout.setVgap(5);
        treeSelect.setSelectionMode(DefaultTreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setSelectionModel(treeSelect);
        tree.setEditable(false);
        tree.addMouseListener(mouseListener);
        tree.addKeyListener(keyListener);
        tree.setCellRenderer(new VTreeCellRenderer());
        treePane.getViewport().add(tree, null);
        treePane.setBorder(new ShadowBorder());
        tree.setBorder(BorderFactory.createEmptyBorder());
        CPanel treePart = new CPanel();
        treePart.setLayout(new BorderLayout());
        treePart.add(treePane, BorderLayout.CENTER);
        treePart.setBorder(BorderFactory.createEmptyBorder());
        treeExpand.setText(Msg.getMsg(Env.getCtx(), "ExpandTree"));
        treeExpand.setActionCommand("Expand");
        treeExpand.addMouseListener(mouseListener);
        treeExpand.addActionListener(this);
        treeSearchLabel.setText(Msg.getMsg(Env.getCtx(), "TreeSearch") + " ");
        treeSearchLabel.setLabelFor(treeSearch);
        treeSearchLabel.setToolTipText(Msg.getMsg(Env.getCtx(), "TreeSearchText"));
        treeSearch.setBackground(AdempierePLAF.getInfoBackground());
        treeSearch.addKeyListener(keyListener);
        southPanel.setLayout(southLayout);
        southPanel.add(treeExpand, BorderLayout.WEST);
        southPanel.add(treeSearchLabel, BorderLayout.CENTER);
        southPanel.add(treeSearch, BorderLayout.EAST);
        treePart.add(southPanel, BorderLayout.SOUTH);
        centerSplitPane.setOpaque(false);
        barScrollPane = new JScrollPane();
        barScrollPane.getViewport().add(bar);
        centerSplitPane.add(barScrollPane, JSplitPane.LEFT);
        centerSplitPane.add(treePart, JSplitPane.RIGHT);
        centerSplitPane.setBorder(BorderFactory.createEmptyBorder());
        removeSplitPaneBorder();
        this.add(centerSplitPane, BorderLayout.CENTER);
        mFrom.setText(Msg.getMsg(Env.getCtx(), "ItemMove"));
        mFrom.setActionCommand("From");
        mFrom.addActionListener(this);
        mTo.setEnabled(false);
        mTo.setText(Msg.getMsg(Env.getCtx(), "ItemInsert"));
        mTo.setActionCommand("To");
        mTo.addActionListener(this);
        mBarAdd.setText(Msg.getMsg(Env.getCtx(), "BarAdd"));
        mBarAdd.setActionCommand("BarAdd");
        mBarAdd.addActionListener(this);
        mBarRemove.setText(Msg.getMsg(Env.getCtx(), "BarRemove"));
        mBarRemove.setActionCommand("BarRemove");
        mBarRemove.addActionListener(this);
        popMenuTree.setLightWeightPopupEnabled(false);
        popMenuTree.add(mBarAdd);
        popMenuTree.addSeparator();
        popMenuTree.add(mFrom);
        popMenuTree.add(mTo);
        popMenuBar.setLightWeightPopupEnabled(false);
        popMenuBar.add(mBarRemove);
    }

    private void removeSplitPaneBorder() {
        if (centerSplitPane != null) {
            SplitPaneUI splitPaneUI = centerSplitPane.getUI();
            if (splitPaneUI instanceof BasicSplitPaneUI) {
                BasicSplitPaneUI basicUI = (BasicSplitPaneUI) splitPaneUI;
                basicUI.getDivider().setBorder(BorderFactory.createEmptyBorder());
            }
        }
    }

    /**
	 * 	Set Divider Location
	 *	@param location location (80 default)
	 */
    public void setDividerLocation(int location) {
        centerSplitPane.setDividerLocation(location);
    }

    /**
	 * 	Get Divider Location
	 *	@return divider location
	 */
    public int getDividerLocation() {
        return centerSplitPane.getDividerLocation();
    }

    /*************************************************************************
	 *	Drag & Drop
	 */
    protected DragSource dragSource = DragSource.getDefaultDragSource();

    protected DropTarget dropTarget = new DropTarget(tree, DnDConstants.ACTION_MOVE, this, true, null);

    protected DragGestureRecognizer recognizer = dragSource.createDefaultDragGestureRecognizer(tree, DnDConstants.ACTION_MOVE, this);

    /**
	 *	Drag Gesture Interface	** Start **
	 *  @param e event
	 */
    public void dragGestureRecognized(DragGestureEvent e) {
        if (!m_editable) return;
        try {
            m_moveNode = (MTreeNode) tree.getSelectionPath().getLastPathComponent();
        } catch (Exception ex) {
            return;
        }
        StringSelection content = new StringSelection(m_moveNode.toString());
        e.startDrag(DragSource.DefaultMoveDrop, content, this);
        log.fine("Drag: " + m_moveNode.toString());
    }

    /**
	 *	DragSourceListener interface - noop
	 *  @param e event
	 */
    public void dragDropEnd(DragSourceDropEvent e) {
    }

    /**
	 *	DragSourceListener interface - noop
	 *  @param e event
	 */
    public void dragEnter(DragSourceDragEvent e) {
    }

    /**
	 *	DragSourceListener interface - noop
	 *  @param e event
	 */
    public void dragExit(DragSourceEvent e) {
    }

    /**
	 *	DragSourceListener interface - noop
	 *  @param e event
	 */
    public void dragOver(DragSourceDragEvent e) {
    }

    /**
	 *	DragSourceListener interface - noop
	 *  @param e event
	 */
    public void dropActionChanged(DragSourceDragEvent e) {
    }

    /**
	 *	DropTargetListener interface
	 *  @param e event
	 */
    public void dragEnter(DropTargetDragEvent e) {
        e.acceptDrag(DnDConstants.ACTION_MOVE);
    }

    /**
	 *	DragSourceListener interface - noop
	 *  @param e event
	 */
    public void dropActionChanged(DropTargetDragEvent e) {
    }

    /**
	 *	DragSourceListener interface - noop
	 *  @param e event
	 */
    public void dragExit(DropTargetEvent e) {
    }

    /**
	 *	Drag over 				** Between **
	 *  @param e event
	 */
    public void dragOver(DropTargetDragEvent e) {
        Point mouseLoc = e.getLocation();
        TreePath path = tree.getClosestPathForLocation(mouseLoc.x, mouseLoc.y);
        tree.setSelectionPath(path);
        MTreeNode toNode = (MTreeNode) path.getLastPathComponent();
        if (m_moveNode == null || toNode == null) e.rejectDrag(); else e.acceptDrag(DnDConstants.ACTION_MOVE);
    }

    /**
	 *	Drop					** End **
	 *  @param e event
	 */
    public void drop(DropTargetDropEvent e) {
        Point mouseLoc = e.getLocation();
        TreePath path = tree.getClosestPathForLocation(mouseLoc.x, mouseLoc.y);
        tree.setSelectionPath(path);
        MTreeNode toNode = (MTreeNode) path.getLastPathComponent();
        log.fine("Drop: " + toNode);
        if (m_moveNode == null || toNode == null) {
            e.rejectDrop();
            return;
        }
        e.acceptDrop(DnDConstants.ACTION_MOVE);
        moveNode(m_moveNode, toNode);
        e.dropComplete(true);
        m_moveNode = null;
    }

    /**
	 *	Move TreeNode
	 *	@param	movingNode	The node to be moved
	 *	@param	toNode		The target node
	 */
    private void moveNode(MTreeNode movingNode, MTreeNode toNode) {
        log.info(movingNode.toString() + " to " + toNode.toString());
        if (movingNode == toNode) return;
        MTreeNode oldParent = (MTreeNode) movingNode.getParent();
        movingNode.removeFromParent();
        treeModel.nodeStructureChanged(oldParent);
        MTreeNode newParent;
        int index;
        if (!toNode.isSummary()) {
            newParent = (MTreeNode) toNode.getParent();
            index = newParent.getIndex(toNode) + 1;
        } else {
            newParent = toNode;
            index = 0;
        }
        newParent.insert(movingNode, index);
        treeModel.nodeStructureChanged(newParent);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Trx trx = Trx.get(Trx.createTrxName("VTreePanel"), true);
        try {
            int no = 0;
            for (int i = 0; i < oldParent.getChildCount(); i++) {
                MTreeNode nd = (MTreeNode) oldParent.getChildAt(i);
                StringBuffer sql = new StringBuffer("UPDATE ");
                sql.append(m_nodeTableName).append(" SET Parent_ID=").append(oldParent.getNode_ID()).append(", SeqNo=").append(i).append(", Updated=SysDate").append(" WHERE AD_Tree_ID=").append(m_AD_Tree_ID).append(" AND Node_ID=").append(nd.getNode_ID());
                log.fine(sql.toString());
                no = DB.executeUpdate(sql.toString(), trx.getTrxName());
            }
            if (oldParent != newParent) for (int i = 0; i < newParent.getChildCount(); i++) {
                MTreeNode nd = (MTreeNode) newParent.getChildAt(i);
                StringBuffer sql = new StringBuffer("UPDATE ");
                sql.append(m_nodeTableName).append(" SET Parent_ID=").append(newParent.getNode_ID()).append(", SeqNo=").append(i).append(", Updated=SysDate").append(" WHERE AD_Tree_ID=").append(m_AD_Tree_ID).append(" AND Node_ID=").append(nd.getNode_ID());
                log.fine(sql.toString());
                no = DB.executeUpdate(sql.toString(), trx.getTrxName());
            }
            trx.commit(true);
        } catch (Exception e) {
            trx.rollback();
            log.log(Level.SEVERE, "move", e);
            ADialog.error(m_WindowNo, this, "TreeUpdateError", e.getLocalizedMessage());
        }
        trx.close();
        trx = null;
        setCursor(Cursor.getDefaultCursor());
        log.config("complete");
    }

    /**
	 *  Enter Key
	 *  @param e event
	 */
    protected void keyPressed(KeyEvent e) {
        if (e.getSource() instanceof JTree || (e.getSource() == treeSearch && e.getModifiers() != 0)) {
            TreePath tp = tree.getSelectionPath();
            if (tp == null) ADialog.beep(); else {
                MTreeNode tn = (MTreeNode) tp.getLastPathComponent();
                setSelectedNode(tn);
            }
        } else if (e.getSource() == treeSearch) {
            String search = treeSearch.getText();
            boolean found = false;
            if (m_nodeEn != null && !m_nodeEn.hasMoreElements()) m_search = "";
            if (!search.equals(m_search)) {
                m_nodeEn = m_root.preorderEnumeration();
                m_search = search;
            }
            while (!found && m_nodeEn != null && m_nodeEn.hasMoreElements()) {
                MTreeNode nd = (MTreeNode) m_nodeEn.nextElement();
                if (nd.toString().toUpperCase().indexOf(search.toUpperCase()) != -1) {
                    found = true;
                    TreePath treePath = new TreePath(nd.getPath());
                    tree.setSelectionPath(treePath);
                    tree.makeVisible(treePath);
                    tree.scrollPathToVisible(treePath);
                }
            }
            if (!found) ADialog.beep();
        }
    }

    /**
	 *  Mouse clicked
	 *  @param e event
	 */
    protected void mouseClicked(MouseEvent e) {
        if (e.getSource() instanceof JTree) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 0) {
                int selRow = tree.getRowForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    MTreeNode tn = (MTreeNode) tree.getPathForLocation(e.getX(), e.getY()).getLastPathComponent();
                    setSelectedNode(tn);
                }
            } else if ((m_editable || m_hasBar) && SwingUtilities.isRightMouseButton(e) && tree.getSelectionPath() != null) {
                MTreeNode nd = (MTreeNode) tree.getSelectionPath().getLastPathComponent();
                {
                    Rectangle r = tree.getPathBounds(tree.getSelectionPath());
                    popMenuTree.show(tree, (int) r.getMaxX(), (int) r.getY());
                }
            }
        } else if (e.getSource() instanceof JButton) {
            if (SwingUtilities.isRightMouseButton(e)) {
                m_buttonSelected = (CButton) e.getSource();
                popMenuBar.show(m_buttonSelected, e.getX(), e.getY());
            }
        }
    }

    /**
	 *  Get currently selected node
	 *  @return MTreeNode
	 */
    public MTreeNode getSelectedNode() {
        return m_selectedNode;
    }

    /**
	 *  Search Field
	 *  @return Search Field
	 */
    public JComponent getSearchField() {
        return treeSearch;
    }

    /**
	 *  Set Selection to Node in Event
	 *  @param nodeID Node ID
	 * 	@return true if selected
	 */
    public boolean setSelectedNode(int nodeID) {
        log.config("ID=" + nodeID);
        if (nodeID != -1) return selectID(nodeID, true);
        return false;
    }

    /**
	 *  Select ID in Tree
	 *  @param nodeID	Node ID
	 *  @param show	scroll to node
	 * 	@return true if selected
	 */
    private boolean selectID(int nodeID, boolean show) {
        if (m_root == null) return false;
        log.config("NodeID=" + nodeID + ", Show=" + show + ", root=" + m_root);
        MTreeNode node = m_root.findNode(nodeID);
        if (node != null) {
            TreePath treePath = new TreePath(node.getPath());
            log.config("Node=" + node + ", Path=" + treePath.toString());
            tree.setSelectionPath(treePath);
            if (show) {
                tree.makeVisible(treePath);
                tree.scrollPathToVisible(treePath);
            }
            return true;
        }
        log.info("Node not found; ID=" + nodeID);
        return false;
    }

    /**
	 *  Set the selected node & initiate all listeners
	 *  @param nd node
	 */
    private void setSelectedNode(MTreeNode nd) {
        log.config("Node = " + nd);
        m_selectedNode = nd;
        firePropertyChange(NODE_SELECTION, null, nd);
    }

    /**************************************************************************
	 *  Node Changed - synchromize Node
	 *
	 *  @param  save    true the node was saved (changed/added), false if the row was deleted
	 *  @param  keyID   the ID of the row changed
	 *  @param  name	name
	 *  @param  description	description
	 *  @param  isSummary	summary node
	 *  @param  imageIndicator image indicator
	 */
    public void nodeChanged(boolean save, int keyID, String name, String description, boolean isSummary, String imageIndicator) {
        log.config("Save=" + save + ", KeyID=" + keyID + ", Name=" + name + ", Description=" + description + ", IsSummary=" + isSummary + ", ImageInd=" + imageIndicator + ", root=" + m_root);
        if (keyID == 0) return;
        MTreeNode node = m_root.findNode(keyID);
        if (node == null && save) {
            node = new MTreeNode(keyID, 0, name, description, m_root.getNode_ID(), isSummary, imageIndicator, false, null);
            m_root.add(node);
        } else if (node != null && save) {
            node.setName(name);
            node.setAllowsChildren(isSummary);
        } else if (node != null && !save) {
            MTreeNode parent = (MTreeNode) node.getParent();
            node.removeFromParent();
            node = parent;
        } else {
            log.log(Level.SEVERE, "Save=" + save + ", KeyID=" + keyID + ", Node=" + node);
            node = null;
        }
        if (node == null) return;
        tree.updateUI();
        TreePath treePath = new TreePath(node.getPath());
        tree.setSelectionPath(treePath);
        tree.makeVisible(treePath);
        tree.scrollPathToVisible(treePath);
    }

    /**************************************************************************
	 *  ActionListener
	 *  @param e event
	 */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
            selectID(Integer.parseInt(e.getActionCommand()), false);
            MTreeNode tn = (MTreeNode) tree.getSelectionPath().getLastPathComponent();
            setSelectedNode(tn);
        } else if (e.getSource() instanceof JMenuItem) {
            if (e.getActionCommand().equals("From")) moveFrom(); else if (e.getActionCommand().equals("To")) moveTo(); else if (e.getActionCommand().equals("BarAdd")) barAdd(); else if (e.getActionCommand().equals("BarRemove")) barRemove();
        } else if (e.getSource() instanceof JCheckBox) {
            if (e.getActionCommand().equals("Expand")) expandTree();
        }
    }

    /**
	 *  Copy Node into buffer
	 */
    private void moveFrom() {
        m_moveNode = (MTreeNode) tree.getSelectionPath().getLastPathComponent();
        if (m_moveNode != null) mTo.setEnabled(true);
    }

    /**
	 *  Move Node
	 */
    private void moveTo() {
        mFrom.setEnabled(true);
        mTo.setEnabled(false);
        if (m_moveNode == null) return;
        MTreeNode toNode = (MTreeNode) tree.getSelectionPath().getLastPathComponent();
        moveNode(m_moveNode, toNode);
        m_moveNode = null;
    }

    /**
	 *  Add selected TreeNode to Bar
	 */
    private void barAdd() {
        MTreeNode nd = (MTreeNode) tree.getSelectionPath().getLastPathComponent();
        if (barDBupdate(true, nd.getNode_ID())) addToBar(nd, getParentToolBar(nd), false); else if (CLogger.retrieveException().getMessage().indexOf("ORA-00001") != -1) ADialog.error(0, this, "BookmarkExist", null);
    }

    /**
	 * Returns the top level parent JToolBar for the given MTreenode. If the parent is not on 
	 * the CPanel yet a new one is created and added.
	 * @param nd
	 * @return top level parent JToolBar for the given MTreenode
	 */
    private JToolBar getParentToolBar(MTreeNode nd) {
        int topParentId = getTopParentId(nd);
        JToolBar parent = toolbarMap.get(topParentId);
        if (parent == null) {
            Enumeration enTop = m_root.children();
            while (enTop.hasMoreElements()) {
                MTreeNode ndTop = (MTreeNode) enTop.nextElement();
                if (ndTop.getNode_ID() == topParentId) {
                    log.fine("add new category: " + ndTop);
                    parent = new JToolBar(JToolBar.VERTICAL);
                    toolbarMap.put(ndTop.getNode_ID(), parent);
                    toolbar.add(parent);
                    parent.setOpaque(false);
                    parent.setFloatable(false);
                    parent.setRollover(true);
                    parent.setBorder(BorderFactory.createEmptyBorder());
                    JXTaskPane barPart = new JXTaskPane();
                    barPart.setTitle(ndTop.toString().trim());
                    barPart.setAnimated(true);
                    barPart.setLayout(new BorderLayout());
                    barPart.add(parent, BorderLayout.NORTH);
                    bar.add(barPart);
                    return parent;
                }
            }
        } else {
            log.fine("parent found: " + parent);
        }
        return parent;
    }

    /**
	 * Returns the id of the top level parent of the given MTreenode
	 * @param nd
	 * @return
	 */
    private int getTopParentId(MTreeNode nd) {
        MTreeNode parent = (MTreeNode) nd.getParent();
        if (parent != null && parent.getNode_ID() != 0) {
            return getTopParentId(parent);
        }
        return nd.getNode_ID();
    }

    /**
	 *  Add TreeNode to Bar
	 *  @param nd node
	 */
    private void addToBar(MTreeNode nd, JToolBar currentToolBar, boolean isLabel) {
        String label = nd.toString().trim();
        if (!isLabel) {
            CButton button = new CButton(label);
            button.setOpaque(false);
            button.setHorizontalAlignment(JButton.LEFT);
            button.setMargin(new Insets(0, 0, 0, 0));
            button.setIcon(nd.getIcon());
            button.setRequestFocusEnabled(false);
            button.setToolTipText(nd.getDescription());
            button.setActionCommand(String.valueOf(nd.getNode_ID()));
            button.addActionListener(this);
            button.addMouseListener(mouseListener);
            currentToolBar.add(button);
        } else {
            currentToolBar.add(new JLabel("<html><u><b>" + label + "</b></u></html>"));
        }
        bar.validate();
        bar.repaint();
    }

    /**
	 *  Remove from Bar
	 */
    private void barRemove() {
        JToolBar parentBar = (JToolBar) m_buttonSelected.getParent();
        Container parentPanel = null;
        if (parentBar != null) {
            parentPanel = parentBar.getParent();
        }
        for (JToolBar jt : toolbar) {
            jt.remove(m_buttonSelected);
        }
        if (parentPanel != null && parentBar.getComponentCount() == 1) {
            bar.remove(parentPanel);
            toolbarMap.values().remove(parentBar);
        }
        bar.validate();
        bar.repaint();
        barDBupdate(false, Integer.parseInt(m_buttonSelected.getActionCommand()));
    }

    /**
	 *	Make Bar add/remove persistent
	 *  @param add true if add - otherwise remove
	 *  @param Node_ID Node ID
	 *  @return true if updated
	 */
    private boolean barDBupdate(boolean add, int Node_ID) {
        int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
        int AD_Org_ID = Env.getContextAsInt(Env.getCtx(), "#AD_Org_ID");
        int AD_User_ID = Env.getContextAsInt(Env.getCtx(), "#AD_User_ID");
        StringBuffer sql = new StringBuffer();
        if (add) sql.append("INSERT INTO AD_TreeBar " + "(AD_Tree_ID,AD_User_ID,Node_ID, " + "AD_Client_ID,AD_Org_ID, " + "IsActive,Created,CreatedBy,Updated,UpdatedBy)VALUES (").append(m_AD_Tree_ID).append(",").append(AD_User_ID).append(",").append(Node_ID).append(",").append(AD_Client_ID).append(",").append(AD_Org_ID).append(",").append("'Y',SysDate,").append(AD_User_ID).append(",SysDate,").append(AD_User_ID).append(")"); else sql.append("DELETE AD_TreeBar WHERE AD_Tree_ID=").append(m_AD_Tree_ID).append(" AND AD_User_ID=").append(AD_User_ID).append(" AND Node_ID=").append(Node_ID);
        int no = DB.executeUpdate(sql.toString(), false, null);
        return no == 1;
    }

    /**
	 *  Clicked on Expand All
	 */
    private void expandTree() {
        if (treeExpand.isSelected()) {
            for (int row = 0; row < tree.getRowCount(); row++) tree.expandRow(row);
        } else {
            for (int row = tree.getRowCount(); row > 0; row--) tree.collapseRow(row);
        }
    }

    @Override
    public void paint(Graphics g) {
        if (m_lookAndFeelChanged) {
            m_lookAndFeelChanged = false;
            if (m_hasBar) removeSplitPaneBorder();
        }
        super.paint(g);
    }
}

/******************************************************************************
 *  Mouse Clicked
 */
class VTreePanel_mouseAdapter extends java.awt.event.MouseAdapter {

    VTreePanel m_adaptee;

    /**
	 * 	VTreePanel_mouseAdapter
	 *	@param adaptee
	 */
    VTreePanel_mouseAdapter(VTreePanel adaptee) {
        m_adaptee = adaptee;
    }

    /**
	 *	Mouse Clicked
	 *	@param e
	 */
    public void mouseClicked(MouseEvent e) {
        m_adaptee.mouseClicked(e);
    }
}

/**
 *  Key Pressed
 */
class VTreePanel_keyAdapter extends java.awt.event.KeyAdapter {

    VTreePanel m_adaptee;

    /**
	 * 	VTreePanel_keyAdapter
	 *	@param adaptee
	 */
    VTreePanel_keyAdapter(VTreePanel adaptee) {
        m_adaptee = adaptee;
    }

    /**
	 * 	Key Pressed
	 *	@param e
	 */
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) m_adaptee.keyPressed(e);
    }
}
