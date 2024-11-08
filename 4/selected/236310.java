package de.renier.vdr.channel.editor;

import java.awt.BorderLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import de.renier.vdr.channel.Channel;
import de.renier.vdr.channel.ChannelCategory;
import de.renier.vdr.channel.ChannelElement;
import de.renier.vdr.channel.editor.actions.ActionManager;
import de.renier.vdr.channel.editor.actions.SortBouqetAction;
import de.renier.vdr.channel.editor.actions.SortFrequenzAction;
import de.renier.vdr.channel.editor.actions.SortNameAction;
import de.renier.vdr.channel.editor.actions.SortTypeAction;
import de.renier.vdr.channel.editor.container.ChannelTreeRenderer;
import de.renier.vdr.channel.editor.container.DNDTree;

/**
 * ChannelListingPanel
 * 
 * @author <a href="mailto:editor@renier.de">Renier Roth</a>
 */
public class ChannelListingPanel extends JPanel {

    private static final long serialVersionUID = -6325105053019841080L;

    private JTree jTree = null;

    private JPopupMenu jPopupMenu = null;

    private JScrollPane jScrollPane = null;

    private JMenu jMenu = null;

    /**
   * This is the default constructor
   */
    public ChannelListingPanel() {
        super();
        initialize();
    }

    /**
   * This method initializes this
   * 
   * @return void
   */
    private void initialize() {
        this.setLayout(new BorderLayout());
        this.setSize(300, 200);
        this.setVisible(true);
        this.setBackground(java.awt.Color.white);
        this.add(getJPopupMenu(), getJPopupMenu().getName());
        this.add(getJScrollPane(), java.awt.BorderLayout.CENTER);
    }

    /**
   * This method initializes jTree
   * 
   * @return javax.swing.JTree
   */
    private JTree getJTree() {
        if (jTree == null) {
            jTree = new DNDTree();
            setDefaultTreeModel(new DefaultMutableTreeNode(new ChannelElement(Messages.getString("ChannelListingPanel.0"))));
            ChannelTreeRenderer channelRenderer = new ChannelTreeRenderer();
            jTree.setCellRenderer(channelRenderer);
            jTree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK), "parkAction");
            jTree.getActionMap().put("parkAction", ActionManager.getInstance().getParkAction());
            jTree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), "unparkAction");
            jTree.getInputMap().put(KeyStroke.getKeyStroke("INSERT"), "unparkAction");
            jTree.getActionMap().put("unparkAction", ActionManager.getInstance().getUnparkAction());
            jTree.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "deleteAction");
            jTree.getActionMap().put("deleteAction", ActionManager.getInstance().getDeleteChannelAction());
            jTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {

                public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
                    ActionManager.getInstance().getParkAction().setEnabled(false);
                    ActionManager.getInstance().getUnparkAction().setEnabled(false);
                    ActionManager.getInstance().getDeleteChannelAction().setEnabled(false);
                    ActionManager.getInstance().getCreateCategoryAction().setEnabled(false);
                    ActionManager.getInstance().getMultiRenameAction().setEnabled(false);
                    jMenu.setEnabled(false);
                    TreePath treePath = e.getNewLeadSelectionPath();
                    if (treePath != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                        if (node != null) {
                            ChannelElement channelElement = (ChannelElement) node.getUserObject();
                            ActionManager.getInstance().getParkAction().setEnabled(channelElement.isRadioOrTelevisionOrService() || channelElement.isCategory());
                            boolean unpargFlag = (channelElement.isRadioOrTelevisionOrService() || channelElement.isCategory()) && (ChannelEditor.application.getChannelParkingPanel().getListSize() > 0);
                            ActionManager.getInstance().getUnparkAction().setEnabled(unpargFlag);
                            ActionManager.getInstance().getDeleteChannelAction().setEnabled(channelElement.isRadioOrTelevisionOrService() || channelElement.isCategory());
                            ActionManager.getInstance().getCreateCategoryAction().setEnabled(node.isRoot());
                            ActionManager.getInstance().getMultiRenameAction().setEnabled(node.isRoot() || channelElement.isCategory());
                            jMenu.setEnabled(channelElement.isCategory() || node.isRoot());
                            ChannelEditor.application.getChannelPropertyPanel().updateFields(channelElement);
                        }
                    } else {
                        ChannelEditor.application.getChannelPropertyPanel().updateFields(ChannelEditor.nothingSelectedChannel);
                    }
                }
            });
            jTree.addMouseListener(new java.awt.event.MouseAdapter() {

                public void mouseReleased(java.awt.event.MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        jPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }

                public void mousePressed(java.awt.event.MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        jPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
        }
        return jTree;
    }

    /**
   * This method initializes jPopupMenu
   * 
   * @return javax.swing.JPopupMenu
   */
    private JPopupMenu getJPopupMenu() {
        if (jPopupMenu == null) {
            jPopupMenu = new JPopupMenu();
            jPopupMenu.add(ActionManager.getInstance().getParkAction()).setMnemonic(KeyEvent.VK_P);
            jPopupMenu.add(ActionManager.getInstance().getUnparkAction()).setMnemonic(KeyEvent.VK_U);
            jPopupMenu.addSeparator();
            jPopupMenu.add(ActionManager.getInstance().getDeleteChannelAction()).setMnemonic(KeyEvent.VK_DELETE);
            jPopupMenu.addSeparator();
            jPopupMenu.add(getJMenu());
            jPopupMenu.add(ActionManager.getInstance().getMultiRenameAction()).setMnemonic(KeyEvent.VK_R);
            jPopupMenu.add(ActionManager.getInstance().getCreateCategoryAction()).setMnemonic(KeyEvent.VK_C);
        }
        return jPopupMenu;
    }

    /**
   * setDefaultTreeModel
   * 
   * @param node
   */
    public void setDefaultTreeModel(DefaultMutableTreeNode node) {
        jTree.setModel(new DefaultTreeModel(node));
        jTree.getModel().addTreeModelListener(new TreeModelListener() {

            public void treeNodesChanged(TreeModelEvent e) {
                calculateChannelNumbers();
            }

            public void treeNodesInserted(TreeModelEvent e) {
                calculateChannelNumbers();
            }

            public void treeNodesRemoved(TreeModelEvent e) {
                calculateChannelNumbers();
            }

            public void treeStructureChanged(TreeModelEvent e) {
                calculateChannelNumbers();
            }
        });
        calculateChannelNumbers();
    }

    /**
   * This method initializes jScrollPane
   * 
   * @return javax.swing.JScrollPane
   */
    private JScrollPane getJScrollPane() {
        if (jScrollPane == null) {
            jScrollPane = new JScrollPane();
            jScrollPane.setViewportView(getJTree());
        }
        return jScrollPane;
    }

    public DefaultMutableTreeNode getRootNode() {
        return (DefaultMutableTreeNode) jTree.getModel().getRoot();
    }

    public void treeNodeChanged(DefaultMutableTreeNode node) {
        ((DefaultTreeModel) jTree.getModel()).nodeChanged(node);
    }

    public void treeNodeStructureChanged(DefaultMutableTreeNode node) {
        ((DefaultTreeModel) jTree.getModel()).nodeStructureChanged(node);
    }

    public void insertNodeInto(DefaultMutableTreeNode newChild, DefaultMutableTreeNode parent, int index) {
        DefaultTreeModel treeModel = (DefaultTreeModel) jTree.getModel();
        treeModel.insertNodeInto(newChild, parent, index);
    }

    public TreePath[] getSelectionPaths() {
        return jTree.getSelectionPaths();
    }

    public TreePath getLeadSelectionPath() {
        return jTree.getLeadSelectionPath();
    }

    public boolean selectAllNodesFiltered(SearchFilter filter) {
        boolean ret = false;
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) jTree.getLastSelectedPathComponent();
        if (root == null) {
            root = (DefaultMutableTreeNode) jTree.getModel().getRoot();
        }
        Enumeration enumer = root.preorderEnumeration();
        ArrayList foundPaths = new ArrayList();
        while (enumer.hasMoreElements()) {
            DefaultMutableTreeNode tempNode = (DefaultMutableTreeNode) enumer.nextElement();
            ChannelElement tempChannel = (ChannelElement) tempNode.getUserObject();
            if (matchChannelToFilter(tempChannel, filter)) {
                TreePath foundPath = new TreePath(tempNode.getPath());
                foundPaths.add(foundPath);
                jTree.scrollPathToVisible(foundPath);
            }
        }
        if (!foundPaths.isEmpty()) {
            ret = true;
            TreePath[] fTreePaths = new TreePath[foundPaths.size()];
            Iterator it = foundPaths.iterator();
            for (int i = 0; it.hasNext(); i++) {
                fTreePaths[i] = (TreePath) it.next();
            }
            jTree.setSelectionPaths(fTreePaths);
        }
        return ret;
    }

    private boolean matchChannelToFilter(ChannelElement channelElement, SearchFilter filter) {
        boolean ret = false;
        if (filter != null && channelElement != null && channelElement instanceof Channel) {
            Channel channel = (Channel) channelElement;
            String searchText = filter.getSearchText().toUpperCase();
            if (filter.isName() && channel.getNameOnly() != null && channel.getNameOnly().toUpperCase().indexOf(searchText) > -1) {
                ret = true;
            }
            if (filter.isBouqet() && channel.getBouqet() != null && channel.getBouqet().toUpperCase().indexOf(searchText) > -1) {
                ret = true;
            }
            if (filter.isFrequenz() && channel.getFrequenz() != null && channel.getFrequenz().toUpperCase().indexOf(searchText) > -1) {
                ret = true;
            }
            if (filter.isParameter() && channel.getParameter() != null && channel.getParameter().toUpperCase().indexOf(searchText) > -1) {
                ret = true;
            }
            if (filter.isSource() && channel.getSource() != null && channel.getSource().toUpperCase().indexOf(searchText) > -1) {
                ret = true;
            }
            if (filter.isSymbolrate() && channel.getSymbolrate() != null && channel.getSymbolrate().toUpperCase().indexOf(searchText) > -1) {
                ret = true;
            }
            if (filter.isPidfields()) {
                if (channel.getVPid() != null && channel.getVPid().toUpperCase().indexOf(searchText) > -1) {
                    ret = true;
                }
                if (channel.getAPid() != null && channel.getAPid().toUpperCase().indexOf(searchText) > -1) {
                    ret = true;
                }
                if (channel.getCaId() != null && channel.getCaId().toUpperCase().indexOf(searchText) > -1) {
                    ret = true;
                }
                if (channel.getNid() != null && channel.getNid().toUpperCase().indexOf(searchText) > -1) {
                    ret = true;
                }
                if (channel.getRid() != null && channel.getRid().toUpperCase().indexOf(searchText) > -1) {
                    ret = true;
                }
                if (channel.getSid() != null && channel.getSid().toUpperCase().indexOf(searchText) > -1) {
                    ret = true;
                }
                if (channel.getTid() != null && channel.getTid().toUpperCase().indexOf(searchText) > -1) {
                    ret = true;
                }
                if (channel.getTPid() != null && channel.getTPid().toUpperCase().indexOf(searchText) > -1) {
                    ret = true;
                }
            }
        }
        return ret;
    }

    private void calculateChannelNumbers() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) jTree.getModel().getRoot();
        Enumeration enumer = root.preorderEnumeration();
        int counter = 1;
        while (enumer.hasMoreElements()) {
            DefaultMutableTreeNode tempNode = (DefaultMutableTreeNode) enumer.nextElement();
            ChannelElement tempChannel = (ChannelElement) tempNode.getUserObject();
            if (tempChannel != null) {
                if (tempChannel instanceof ChannelCategory) {
                    int catNumber = ((ChannelCategory) tempChannel).getNumberAt();
                    if (catNumber > 0 && counter < catNumber) {
                        counter = catNumber;
                    }
                } else if (tempChannel instanceof Channel) {
                    ((Channel) tempChannel).setNumber(counter);
                    counter++;
                }
            }
        }
    }

    /**
   * This method initializes jMenu
   * 
   * @return javax.swing.JMenu
   */
    private JMenu getJMenu() {
        if (jMenu == null) {
            jMenu = new JMenu();
            jMenu.setText(Messages.getString("ChannelListingPanel.10"));
            jMenu.setIcon(new ImageIcon(getClass().getResource("/org/javalobby/icons/20x20/AlignLeftArrow.gif")));
            jMenu.setEnabled(false);
            jMenu.add(new SortNameAction());
            jMenu.add(new SortBouqetAction());
            jMenu.add(new SortFrequenzAction());
            jMenu.add(new SortTypeAction(SortTypeAction.SORT_TYPE_TV));
            jMenu.add(new SortTypeAction(SortTypeAction.SORT_TYPE_TVCRYPT));
            jMenu.add(new SortTypeAction(SortTypeAction.SORT_TYPE_RADIO));
            jMenu.add(new SortTypeAction(SortTypeAction.SORT_TYPE_RADIOCRYPT));
            jMenu.add(new SortTypeAction(SortTypeAction.SORT_TYPE_SERVICE));
            jMenu.add(new SortTypeAction(SortTypeAction.SORT_TYPE_SERVICECRYPT));
        }
        return jMenu;
    }

    public void readStatistic() {
    }
}
