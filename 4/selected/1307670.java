package de.nava.risotto;

import java.awt.BorderLayout;
import java.io.File;
import java.net.MalformedURLException;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import de.nava.informa.core.ChannelBuilderIF;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ChannelObserverIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.core.ParseException;
import de.nava.informa.core.UnsupportedFormatException;
import de.nava.informa.impl.basic.ChannelBuilder;
import de.nava.informa.utils.ChannelRegistry;
import de.nava.informa.utils.FileUtils;

/**
 * Demonstration on how to put together a small RSS news client on top of
 * the informa API.
 */
public class Nuevo extends JFrame {

    private ChannelRegistry registry;

    private ChannelTree channelTree;

    private ItemTable itemTable;

    private ItemTextPane itemPane;

    public Nuevo() throws Exception {
        super("Nuevo Frame");
        this.setSize(500, 300);
        this.addWindowListener(new BasicWindowMonitor());
        this.init();
        channelTree = new ChannelTree(registry.getChannels());
        JScrollPane scrollChannels = new JScrollPane(channelTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        channelTree.addTreeSelectionListener(new ChannelSelectionListener());
        itemTable = new ItemTable((ChannelIF) registry.getChannels().iterator().next());
        JScrollPane scrollItems = new JScrollPane(itemTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel lsm = itemTable.getSelectionModel();
        lsm.addListSelectionListener(new ItemSelectionListener(lsm));
        itemPane = new ItemTextPane();
        JScrollPane scrollItem = new JScrollPane(itemPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JSplitPane spB = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollItems, scrollItem);
        JSplitPane spA = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollChannels, spB);
        getContentPane().add(spA, BorderLayout.CENTER);
        getContentPane().add(spB, BorderLayout.EAST);
        this.pack();
    }

    public void init() throws MalformedURLException, ParseException, UnsupportedFormatException {
        File basePath = new File("/opt/cvs-co/informa/test/data");
        FileUtils.copyFile(new File(basePath, "xmlhack-0.91.xml"), new File("/tmp/dummy.xml"));
        String[] chl_locs = { "/tmp/dummy.xml" };
        ChannelBuilderIF builder = new ChannelBuilder();
        ChannelObserverIF observer = new MyChannelObserver();
        registry = new ChannelRegistry(builder);
        for (int i = 0; i < chl_locs.length; i++) {
            ChannelIF c = registry.addChannel(new File(chl_locs[i]).toURL(), 20, true);
            c.addObserver(observer);
        }
        Thread t = new MySimulator();
        t.start();
    }

    public static void main(String args[]) throws Exception {
        Nuevo n = new Nuevo();
        n.setVisible(true);
    }

    class MySimulator extends Thread {

        public MySimulator() {
            super();
        }

        public void run() {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
            File basePath = new File("/opt/cvs-co/informa/test/data");
            FileUtils.copyFile(new File(basePath, "xmlhack-0.91-added-item.xml"), new File("/tmp/dummy.xml"));
        }
    }

    class MyChannelObserver implements ChannelObserverIF {

        public void itemAdded(ItemIF newItem) {
            DefaultTreeModel treeModel = channelTree.getTreeModel();
            ChannelTreeNode node = channelTree.getChannelTreeNode(newItem.getChannel());
            node.update();
            treeModel.nodeChanged(node);
            if (itemTable.getItemModel().getChannel().equals(newItem.getChannel())) {
                itemTable.getItemModel().addItem(newItem);
            }
        }

        public void channelRetrieved(ChannelIF channel) {
        }
    }

    /**
   * Class for observing the user's row selection in the news item table.
   */
    class ItemSelectionListener implements ListSelectionListener {

        ListSelectionModel model;

        public ItemSelectionListener(ListSelectionModel lsm) {
            model = lsm;
        }

        public void valueChanged(ListSelectionEvent lse) {
            if (!lse.getValueIsAdjusting()) {
                int[] selection = getSelectedIndices(model.getMinSelectionIndex(), model.getMaxSelectionIndex());
                if (selection.length > 1) {
                    throw new IllegalArgumentException("Only one item can be selected at a time.");
                } else if (selection.length == 1) {
                    System.out.println("selected item " + selection[0]);
                    itemPane.setItem(itemTable.getItemModel().getItem(selection[0]));
                } else {
                    System.out.println("deselected item");
                }
            }
        }

        protected int[] getSelectedIndices(int start, int stop) {
            if ((start == -1) || (stop == -1)) {
                return new int[0];
            }
            int guesses[] = new int[stop - start + 1];
            int index = 0;
            for (int i = start; i <= stop; i++) {
                if (model.isSelectedIndex(i)) {
                    guesses[index++] = i;
                }
            }
            int realthing[] = new int[index];
            System.arraycopy(guesses, 0, realthing, 0, index);
            return realthing;
        }
    }

    class ChannelSelectionListener implements TreeSelectionListener {

        public void valueChanged(TreeSelectionEvent e) {
            TreePath treePath = e.getNewLeadSelectionPath();
            System.out.println("selected " + treePath);
            Object o = treePath.getLastPathComponent();
            if (o instanceof ChannelTreeNode) {
                ChannelIF channel = ((ChannelTreeNode) o).getChannel();
                itemTable.getItemModel().setChannel(channel);
                itemPane.setItem((ItemIF) channel.getItems().iterator().next());
            }
        }
    }
}
