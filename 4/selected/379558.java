package de.cinek.rssview;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import de.cinek.rssview.ui.DnDSupportTree;

/**
 * @author saintedlama
 * @version $Id: RssChannelList.java,v 1.27 2004/10/27 23:19:08 saintedlama Exp $
 */
public class RssChannelList extends JScrollPane implements ViewComponent {

    private RssView parent;

    private RssDropDownMenu dropdownmenu;

    private JTree channelTree;

    private ChannelModel model;

    private RssChannelTreeModel treeModel;

    private CategoryNode root;

    RssChannelCellRenderer cellRenderer;

    public static final int ADJACENT_TOLERANCE = 5;

    private TreeSelectionListener treeSelectionListener;

    public RssChannelList(RssView parent, ChannelModel model) {
        super();
        this.parent = parent;
        this.model = model;
        this.root = model.getRootNode();
        initComponents();
    }

    public void initComponents() {
        cellRenderer = new RssChannelCellRenderer();
        treeModel = new RssChannelTreeModel(root);
        model.addChannelModelListener(treeModel);
        channelTree = new DnDSupportTree(treeModel);
        channelTree.setCellRenderer(cellRenderer);
        channelTree.setRootVisible(true);
        channelTree.setRowHeight(20);
        channelTree.setShowsRootHandles(true);
        setViewportView(channelTree);
        JMenuItem customSection[] = new JMenuItem[5];
        customSection[0] = new javax.swing.JMenuItem(parent.getNewFolderAction());
        customSection[1] = new javax.swing.JMenuItem(parent.getEditFolderAction());
        customSection[2] = new javax.swing.JMenuItem(parent.getDeleteFolderAction());
        customSection[4] = new javax.swing.JMenuItem(parent.getMarkArticlesReadAction());
        dropdownmenu = new RssDropDownMenu(parent, customSection);
        channelTree.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent evt) {
                mousePressedOnTree(evt);
            }

            public void mouseReleased(MouseEvent evt) {
                mouseReleasedOnTree(evt);
            }

            public void mouseClicked(MouseEvent evt) {
                mouseClickedOnTree(evt);
            }
        });
        DragSource dragSource = new DragSource();
        dragSource.createDefaultDragGestureRecognizer(channelTree, DnDConstants.ACTION_MOVE, new RssChannelList.RssDragGestureListener());
        channelTree.setDropTarget(new DropTarget(channelTree, new RssDropListener()));
        this.treeSelectionListener = new TreeSelectionListener() {

            public void valueChanged(TreeSelectionEvent evt) {
                treeSelectionChanged(evt);
            }
        };
    }

    public void repaintTree() {
        cellRenderer.initializeFonts();
        channelTree.repaint();
    }

    protected void mouseClickedOnTree(MouseEvent evt) {
        if (evt.getClickCount() > 1) {
            Object path[] = parent.getSelectionModel().getSelectionPath();
            if (path != null && path[path.length - 1] instanceof Channel) {
                ActionEvent actionEvent = new ActionEvent(parent, ActionEvent.ACTION_PERFORMED, "");
                parent.getEditChannelAction().actionPerformed(actionEvent);
            }
        }
    }

    protected void mousePressedOnTree(MouseEvent evt) {
        if (evt.isPopupTrigger()) {
            maybePopup(evt);
        }
    }

    protected void mouseReleasedOnTree(MouseEvent evt) {
        if (evt.isPopupTrigger()) {
            maybePopup(evt);
        }
    }

    protected void maybePopup(MouseEvent evt) {
        JTree tree = (JTree) evt.getSource();
        int row = tree.getRowForLocation(evt.getX(), evt.getY());
        if (row >= 0 && row < tree.getRowCount()) {
            tree.setSelectionRow(row);
        }
        this.dropdownmenu.show(tree, evt.getX(), evt.getY());
    }

    /**
	 * TODO (by Cinek):
	 * 
	 * I would like to see certain reactions when selecting GroupNodes. It would
	 * be nice to have a view of all Channels (recent articles) which are in the
	 * currently selected group.
	 * 
	 * E.a. the group node should show all recent articles. I think many people
	 * depend on this feature.
	 */
    protected synchronized void treeSelectionChanged(TreeSelectionEvent e) {
        if (e.getPath() != null) {
            parent.getSelectionModel().setSelectionPath(e.getPath().getPath());
            if (e.getPath().getLastPathComponent() instanceof Channel) {
                parent.getEditChannelAction().setEnabled(true);
                parent.getDeleteChannelAction().setEnabled(true);
                parent.getCopyChannelAction().setEnabled(true);
                parent.getMarkArticlesReadAction().setEnabled(true);
                parent.getEditFolderAction().setEnabled(false);
                parent.getDeleteFolderAction().setEnabled(false);
            } else if (e.getPath().getLastPathComponent() instanceof RssGroupNode) {
                parent.getEditChannelAction().setEnabled(false);
                parent.getDeleteChannelAction().setEnabled(false);
                parent.getCopyChannelAction().setEnabled(false);
                parent.getMarkArticlesReadAction().setEnabled(false);
                if (e.getPath().getLastPathComponent() == root) {
                    parent.getDeleteFolderAction().setEnabled(false);
                } else {
                    parent.getDeleteFolderAction().setEnabled(true);
                }
                parent.getEditFolderAction().setEnabled(true);
            }
        } else {
            parent.getSelectionModel().setSelectionPath(null);
            parent.getEditChannelAction().setEnabled(false);
            parent.getDeleteChannelAction().setEnabled(false);
            parent.getCopyChannelAction().setEnabled(false);
            parent.getEditFolderAction().setEnabled(false);
            parent.getDeleteFolderAction().setEnabled(false);
            parent.getMarkArticlesReadAction().setEnabled(false);
        }
    }

    public void newSubscription(String url) {
        RssChannelDialog dialog = RssChannelDialog.showDialog(parent, url);
        if (dialog.getDialogResult() == RssChannelDialog.YES_OPTION) {
            Object path[] = parent.getSelectionModel().getSelectionPath();
            model.add((RssGroupNode) path[path.length - 1], dialog.getChannel());
        }
    }

    /**
	 * Moves a node object to a given Point
	 */
    protected void move(TreePath path, int x, int y) {
        int upperRow = channelTree.getRowForLocation(x, y - ADJACENT_TOLERANCE);
        int lowerRow = channelTree.getRowForLocation(x, y + ADJACENT_TOLERANCE);
        if (lowerRow == -1 && upperRow == -1) {
            treeModel.move(path, null, null);
        } else if (upperRow != lowerRow) {
            treeModel.move(path, channelTree.getPathForRow(upperRow), channelTree.getPathForRow(lowerRow));
        } else {
            treeModel.moveOnNode(path, channelTree.getPathForLocation(x, y));
        }
    }

    protected void move(TreePath path, Point point) {
        move(path, point.x, point.y);
    }

    public void moveUp() {
        treeModel.move(channelTree.getSelectionPath(), -1);
    }

    public void moveDown() {
        treeModel.move(channelTree.getSelectionPath(), 1);
    }

    public void markArticlesRead() {
        Object path[] = parent.getSelectionModel().getSelectionPath();
        if (path != null) {
            Object selectedNode = path[path.length - 1];
            if (selectedNode != null && selectedNode instanceof Channel) {
                ((Channel) selectedNode).markArticlesRead();
                parent.getSelectionModel().setSelectionPath(path);
            }
        }
    }

    private class RssDragGestureListener implements DragGestureListener {

        public RssDragGestureListener() {
        }

        public void dragGestureRecognized(DragGestureEvent dge) {
            Point p = dge.getDragOrigin();
            TreePath path = channelTree.getPathForLocation(p.x, p.y);
            if (path != null) {
                dge.startDrag(DragSource.DefaultMoveDrop, new RssTransferablePath(path.getPath()));
            }
        }
    }

    private class RssDropListener implements DropTargetListener {

        public RssDropListener() {
        }

        public void dragEnter(DropTargetDragEvent e) {
        }

        public void dragOver(DropTargetDragEvent e) {
            if (e.isDataFlavorSupported(RssTransferablePath.PATH_FLAVOR) || e.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                e.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
                int x = e.getLocation().x;
                int y = e.getLocation().y;
                int upperRow = channelTree.getRowForLocation(x, y - ADJACENT_TOLERANCE);
                int lowerRow = channelTree.getRowForLocation(x, y + ADJACENT_TOLERANCE);
                if (lowerRow == -1 && upperRow == -1) {
                    ((DnDSupportTree) channelTree).setSeperatorRow(1000);
                } else if (lowerRow != upperRow) {
                    ((DnDSupportTree) channelTree).setSeperatorRow(upperRow);
                } else {
                    ((DnDSupportTree) channelTree).setSeperatorRow(-1);
                }
            } else {
                e.rejectDrag();
            }
        }

        public void dragExit(DropTargetEvent e) {
        }

        public void drop(DropTargetDropEvent e) {
            try {
                ((DnDSupportTree) channelTree).setSeperatorRow(-1);
                if (e.isDataFlavorSupported(RssTransferablePath.PATH_FLAVOR)) {
                    e.acceptDrop(DnDConstants.ACTION_MOVE);
                    Object[] path = (Object[]) e.getTransferable().getTransferData(RssTransferablePath.PATH_FLAVOR);
                    move(new TreePath(path), e.getLocation());
                    e.getDropTargetContext().dropComplete(true);
                } else if (e.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    e.acceptDrop(DnDConstants.ACTION_MOVE);
                    String url = (String) e.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    newSubscription(url);
                    e.getDropTargetContext().dropComplete(true);
                } else {
                    e.rejectDrop();
                }
            } catch (Exception ex) {
                e.rejectDrop();
            }
        }

        public void dropActionChanged(DropTargetDragEvent dtde) {
        }
    }

    /**
	 * @see de.cinek.rssview.ViewComponent#activate()
	 */
    public void activate() {
        TreePath path = new TreePath(parent.getSelectionModel().getSelectionPath());
        if (path != null && path.getPathCount() > 0) {
            this.channelTree.setSelectionPath(path);
        }
        channelTree.addTreeSelectionListener(this.treeSelectionListener);
    }

    /**
	 * @see de.cinek.rssview.ViewComponent#deactivate()
	 */
    public void deactivate() {
        channelTree.removeTreeSelectionListener(this.treeSelectionListener);
    }
}
