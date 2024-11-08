package org.rdv.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.apache.commons.logging.Log;
import org.rdv.DataPanelManager;
import org.rdv.DataViewer;
import org.rdv.Extension;
import org.rdv.RDV;
import org.rdv.action.ActionFactory;
import org.rdv.rbnb.MetadataListener;
import org.rdv.rbnb.Player;
import org.rdv.rbnb.RBNBController;
import org.rdv.rbnb.RBNBUtilities;
import org.rdv.rbnb.StateListener;
import org.rdv.util.ReadableStringComparator;
import com.jgoodies.uif_lite.panel.SimpleInternalFrame;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.ChannelTree;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.ChannelTree.Node;
import com.rbnb.sapi.ChannelTree.NodeTypeEnum;

/**
 * A panel that contains the channels in a tree.
 * 
 * @author Jason P. Hanley
 */
public class ChannelListPanel extends JPanel implements MetadataListener, StateListener {

    /** serialization version identifier */
    private static final long serialVersionUID = -5091214984427475802L;

    static Log log = org.rdv.LogFactory.getLog(ChannelListPanel.class.getName());

    private DataPanelManager dataPanelManager;

    private RBNBController rbnb;

    private List<ChannelSelectionListener> channelSelectionListeners;

    private JTextField filterTextField;

    private JButton clearFilterButton;

    private JTree tree;

    private ChannelTreeModel treeModel;

    private JButton metadataUpdateButton;

    public ChannelListPanel(DataPanelManager dataPanelManager, RBNBController rbnb) {
        super();
        this.dataPanelManager = dataPanelManager;
        this.rbnb = rbnb;
        channelSelectionListeners = new ArrayList<ChannelSelectionListener>();
        initPanel();
    }

    /**
   * Create the main UI panel.
   */
    private void initPanel() {
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(130, 27));
        JComponent filterComponent = createFilterPanel();
        JComponent treePanel = createTreePanel();
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(filterComponent, BorderLayout.NORTH);
        mainPanel.add(treePanel, BorderLayout.CENTER);
        JToolBar channelToolBar = createToolBar();
        SimpleInternalFrame treeFrame = new SimpleInternalFrame(DataViewer.getIcon("icons/channels.gif"), "Channels", channelToolBar, mainPanel);
        add(treeFrame, BorderLayout.CENTER);
    }

    /**
   * Create the UI panel that contains the controls to filter the channel list.
   * 
   * @return  the UI component dealing with filtering
   */
    private JComponent createFilterPanel() {
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BorderLayout(5, 5));
        filterPanel.setBackground(Color.white);
        filterPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JLabel filterIconLabel = new JLabel(DataViewer.getIcon("icons/filter.gif"));
        filterPanel.add(filterIconLabel, BorderLayout.WEST);
        filterTextField = new JTextField();
        filterTextField.setToolTipText("Enter text here to filter the channel list");
        filterTextField.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent e) {
                treeModel.setFilter(filterTextField.getText());
            }

            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }
        });
        filterPanel.add(filterTextField, BorderLayout.CENTER);
        Action focusFilterAction = new AbstractAction() {

            private static final long serialVersionUID = -2443410059209958411L;

            public void actionPerformed(ActionEvent e) {
                filterTextField.requestFocusInWindow();
                filterTextField.selectAll();
            }
        };
        int modifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        filterTextField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, modifier), "focusFilter");
        filterTextField.getActionMap().put("focusFilter", focusFilterAction);
        Action cancelFilterAction = new AbstractAction(null, DataViewer.getIcon("icons/cancel.gif")) {

            private static final long serialVersionUID = 8913797349366699615L;

            public void actionPerformed(ActionEvent e) {
                treeModel.setFilter(null);
            }
        };
        cancelFilterAction.putValue(Action.SHORT_DESCRIPTION, "Cancel filter");
        filterTextField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelFilter");
        filterTextField.getActionMap().put("cancelFilter", cancelFilterAction);
        clearFilterButton = new JButton(cancelFilterAction);
        clearFilterButton.setBorderPainted(false);
        clearFilterButton.setVisible(false);
        filterPanel.add(clearFilterButton, BorderLayout.EAST);
        return filterPanel;
    }

    /**
   * Create the channel tree panel.
   * 
   * @return  the component containing the channel tree
   */
    private JComponent createTreePanel() {
        treeModel = new ChannelTreeModel();
        treeModel.addPropertyChangeListener(new FilterPropertyChangeListener());
        tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setExpandsSelectedPaths(true);
        tree.setCellRenderer(new ChannelTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.addTreeSelectionListener(new ChannelTreeSelectionListener());
        tree.addMouseListener(new ChannelTreeMouseListener());
        tree.setBorder(new EmptyBorder(0, 5, 5, 5));
        JScrollPane treeView = new JScrollPane(tree);
        treeView.setBorder(null);
        treeView.setViewportBorder(null);
        tree.setDragEnabled(true);
        tree.setTransferHandler(new ChannelTransferHandler());
        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(tree, DnDConstants.ACTION_LINK, new ChannelDragGestureListener());
        return treeView;
    }

    /**
   * Create the tool bar.
   * 
   * @return  the tool bar for the channel list panel
   */
    private JToolBar createToolBar() {
        JToolBar channelToolBar = new JToolBar();
        channelToolBar.setFloatable(false);
        JButton button = new ToolBarButton("icons/expandall.gif", "Expand channel list");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                expandTree();
            }
        });
        channelToolBar.add(button);
        button = new ToolBarButton("icons/collapseall.gif", "Collapse channel list");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                collapseTree();
            }
        });
        channelToolBar.add(button);
        metadataUpdateButton = new ToolBarButton("icons/refresh.gif", "Update channel list");
        metadataUpdateButton.setEnabled(false);
        metadataUpdateButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                rbnb.updateMetadata();
            }
        });
        channelToolBar.add(metadataUpdateButton);
        return channelToolBar;
    }

    public void channelTreeUpdated(final ChannelTree newChannelTree) {
        ChannelTree ctree = newChannelTree;
        TreePath[] paths = tree.getSelectionPaths();
        Iterator it = ctree.iterator();
        System.out.println("#####");
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof ChannelTree.Node) {
                ChannelTree.Node node = (ChannelTree.Node) o;
                System.out.println(node.getName());
            }
        }
        boolean structureChanged = treeModel.setChannelTree(ctree);
        if (structureChanged) {
            tree.clearSelection();
            if (paths != null) {
                for (int i = 0; i < paths.length; i++) {
                    Object o = paths[i].getLastPathComponent();
                    if (o instanceof ChannelTree.Node) {
                        ChannelTree.Node node = (ChannelTree.Node) o;
                        selectNode(node.getFullName());
                    } else {
                        selectRootNode();
                    }
                }
            }
        }
    }

    private class FilterPropertyChangeListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent pce) {
            if (!pce.getPropertyName().equals("filter")) {
                return;
            }
            String filterText = (String) pce.getNewValue();
            if (filterText.compareToIgnoreCase(filterTextField.getText()) != 0) {
                filterTextField.setText(filterText);
            }
            clearFilterButton.setVisible(filterText.length() > 0);
            if (filterText.length() > 0) {
                expandTree();
            }
        }
    }

    public List<String> getSelectedChannels() {
        ArrayList<String> channels = new ArrayList<String>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null) {
            for (int i = 0; i < paths.length; i++) {
                if (paths[i].getLastPathComponent() != treeModel.getRoot()) {
                    Node node = (ChannelTree.Node) paths[i].getLastPathComponent();
                    NodeTypeEnum type = node.getType();
                    if (type == ChannelTree.SERVER || type == ChannelTree.SOURCE || type == ChannelTree.PLUGIN || type == ChannelTree.PLUGIN) {
                        for (String channel : RBNBUtilities.getChildChannels(node, treeModel.isHiddenChannelsVisible())) {
                            if (!channels.contains(channel)) {
                                channels.add(channel);
                            }
                        }
                    } else if (type == ChannelTree.CHANNEL) {
                        if (!channels.contains(node.getFullName())) {
                            channels.add(node.getFullName());
                        }
                    }
                }
            }
        }
        Collections.sort(channels, new ReadableStringComparator());
        return channels;
    }

    public void addChannelSelectionListener(ChannelSelectionListener e) {
        channelSelectionListeners.add(e);
    }

    public void removeChannelSelectionListener(ChannelSelectionListener e) {
        channelSelectionListeners.remove(e);
    }

    private void fireChannelSelected(Object o) {
        boolean isRoot = (o == treeModel.getRoot());
        int children = treeModel.getChildCount(o);
        String channelName = null;
        if (!isRoot) {
            ChannelTree.Node node = (ChannelTree.Node) o;
            channelName = node.getFullName();
        }
        ChannelSelectionEvent cse = new ChannelSelectionEvent(channelName, children, isRoot);
        Iterator<ChannelSelectionListener> i = channelSelectionListeners.iterator();
        while (i.hasNext()) {
            ChannelSelectionListener csl = (ChannelSelectionListener) i.next();
            csl.channelSelected(cse);
        }
    }

    private void fireNoChannelsSelected() {
        Iterator<ChannelSelectionListener> i = channelSelectionListeners.iterator();
        while (i.hasNext()) {
            ChannelSelectionListener csl = (ChannelSelectionListener) i.next();
            csl.channelSelectionCleared();
        }
    }

    public boolean isShowingHiddenChannles() {
        return treeModel.isHiddenChannelsVisible();
    }

    public void showHiddenChannels(boolean showHiddenChannels) {
        treeModel.setHiddenChannelsVisible(showHiddenChannels);
    }

    private void expandTree() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void collapseTree() {
        for (int i = tree.getRowCount() - 1; i >= 0; i--) {
            tree.collapseRow(i);
        }
    }

    private void selectRootNode() {
        tree.addSelectionPath(new TreePath(treeModel.getRoot()));
    }

    private void selectNode(String nodeName) {
        ChannelTree.Node node = treeModel.getChannelTree().findNode(nodeName);
        if (node != null) {
            int depth = node.getDepth();
            Object[] path = new Object[depth + 2];
            path[0] = treeModel.getRoot();
            for (int i = path.length - 1; i > 0; i--) {
                path[i] = node;
                node = node.getParent();
            }
            tree.addSelectionPath(new TreePath(path));
        }
    }

    public class ChannelTreeSelectionListener implements TreeSelectionListener {

        public void valueChanged(TreeSelectionEvent e) {
            TreePath selectedPath = e.getNewLeadSelectionPath();
            if (selectedPath == null) {
                fireNoChannelsSelected();
            } else {
                Object o = selectedPath.getLastPathComponent();
                fireChannelSelected(o);
            }
        }
    }

    public Transferable createChannelsTransferable() {
        List<String> channels = getSelectedChannels();
        if (channels.size() == 0) {
            return null;
        }
        return new ChannelListTransferable(channels);
    }

    private class ChannelTransferHandler extends TransferHandler {

        /** serialization version identifier */
        private static final long serialVersionUID = 5965378439143524577L;

        public int getSourceActions(JComponent c) {
            return DnDConstants.ACTION_LINK;
        }

        protected Transferable createTransferable(JComponent c) {
            return createChannelsTransferable();
        }
    }

    private class ChannelDragGestureListener implements DragGestureListener {

        public void dragGestureRecognized(DragGestureEvent dge) {
            Transferable transferable = createChannelsTransferable();
            if (transferable == null) {
                return;
            }
            try {
                dge.startDrag(DragSource.DefaultLinkDrop, transferable);
            } catch (InvalidDnDOperationException e) {
            }
        }
    }

    private class ChannelTreeMouseListener extends MouseAdapter {

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                handleDoubleClick(e);
            }
        }

        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                handlePopup(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                handlePopup(e);
            }
        }
    }

    private void handleDoubleClick(MouseEvent e) {
        TreePath treePath = tree.getSelectionPath();
        if (treePath == null) {
            return;
        }
        Object o = treePath.getLastPathComponent();
        if (o != treeModel.getRoot()) {
            ChannelTree.Node node = (ChannelTree.Node) o;
            if (node.getType() == ChannelTree.CHANNEL) {
                String channelName = node.getFullName();
                viewChannel(channelName);
            }
        }
    }

    private void handlePopup(MouseEvent e) {
        TreePath treePath = tree.getPathForLocation(e.getX(), e.getY());
        if (treePath == null) {
            return;
        }
        if (!tree.isPathSelected(treePath)) {
            tree.setSelectionPath(treePath);
        }
        JPopupMenu popup = null;
        Object o = treePath.getLastPathComponent();
        if (o == treeModel.getRoot()) {
            popup = getRootPopup();
        } else {
            popup = getChannelPopup();
        }
        if (popup != null && popup.getComponentCount() > 0) {
            popup.show(tree, e.getX(), e.getY());
        }
    }

    private JPopupMenu getRootPopup() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("Import data...", DataViewer.getIcon("icons/import.gif"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                ActionFactory.getInstance().getDataImportAction().importData();
            }
        });
        popup.add(menuItem);
        menuItem = new JMenuItem("Export data...", DataViewer.getIcon("icons/export.gif"));
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                ActionFactory.getInstance().getDataExportAction().exportData(RBNBUtilities.getAllChannels(treeModel.getChannelTree(), treeModel.isHiddenChannelsVisible()));
            }
        });
        popup.add(menuItem);
        return popup;
    }

    private JPopupMenu getChannelPopup() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;
        final List<String> channels = getSelectedChannels();
        if (channels.isEmpty()) {
            return null;
        }
        String plural = (channels.size() == 1) ? "" : "s";
        List<Extension> extensions = dataPanelManager.getExtensions(channels);
        if (extensions.size() > 0) {
            for (final Extension extension : extensions) {
                menuItem = new JMenuItem("View channel" + plural + " with " + extension.getName());
                menuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        viewChannels(channels, extension);
                    }
                });
                popup.add(menuItem);
            }
            popup.addSeparator();
        }
        if (dataPanelManager.isAnyChannelSubscribed(channels)) {
            menuItem = new JMenuItem("Unsubscribe from channel" + plural);
            menuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    dataPanelManager.unsubscribeChannels(channels);
                }
            });
            popup.add(menuItem);
            popup.addSeparator();
        }
        String mime = getMime(channels);
        if (mime != null) {
            if (mime.equals("application/octet-stream")) {
                menuItem = new JMenuItem("Export data channel" + plural + "...", DataViewer.getIcon("icons/export.gif"));
                menuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        ActionFactory.getInstance().getDataExportAction().exportData(channels);
                    }
                });
                popup.add(menuItem);
            } else if (mime.equals("image/jpeg")) {
                menuItem = new JMenuItem("Export video channel" + plural + "...", DataViewer.getIcon("icons/export.gif"));
                menuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        JFrame frame = RDV.getInstance(RDV.class).getMainFrame();
                        new ExportVideoDialog(frame, rbnb, channels);
                    }
                });
                popup.add(menuItem);
            } else {
                popup.remove(popup.getComponentCount() - 1);
            }
        }
        return popup;
    }

    private void viewChannel(final String channelName) {
        new Thread() {

            public void run() {
                dataPanelManager.viewChannel(channelName);
            }
        }.start();
    }

    private void viewChannels(final List<String> channels, final Extension extension) {
        new Thread() {

            public void run() {
                dataPanelManager.viewChannels(channels, extension);
            }
        }.start();
    }

    /**
   * Gets the common mime type amoung all the channels. If the channels are all
   * the same mime type this will return their mime type. If they are of
   * different type this will return null.
   * 
   * @param channels  the list of channels
   * @return          the common mime type of the channels, or null if there is
   *                  none
   */
    private String getMime(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return null;
        }
        String mime = rbnb.getChannel(channels.get(0)).getMetadata("mime");
        for (int i = 1; i < channels.size(); i++) {
            String channel = channels.get(i);
            if (!mime.equals(rbnb.getChannel(channel).getMetadata("mime"))) {
                return null;
            }
        }
        return mime;
    }

    public void postState(int newState, int oldState) {
        if (newState == Player.STATE_DISCONNECTED || newState == Player.STATE_EXITING) {
            metadataUpdateButton.setEnabled(false);
            filterTextField.setEnabled(false);
            clearFilterButton.setEnabled(false);
            tree.setEnabled(false);
        } else {
            metadataUpdateButton.setEnabled(true);
            filterTextField.setEnabled(true);
            clearFilterButton.setEnabled(true);
            tree.setEnabled(true);
        }
    }
}
