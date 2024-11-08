package informaclient;

import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Logger;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import de.nava.informa.core.ChannelBuilderIF;
import de.nava.informa.core.ChannelFormat;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.impl.basic.ChannelBuilder;
import de.nava.informa.utils.ChannelRegistry;

/**
 * This object contains the global state of this application, and controlls all the
 * interactions between the pieces.
 */
public class GlobalController {

    private ChannelRegistry channelReg;

    private ChannelObserver chanObs = new ChannelObserver(this);

    private ChannelBuilderIF builder;

    private JTree channelTree;

    private ChannelTreeModel chanTreeModel;

    private Logger log = Logger.getLogger("global ctrl");

    private javax.swing.JTextPane logPane;

    private ChannelItemListModel channelItemListModel;

    private ChannelStampView channelStampView;

    private ItemDetailView itemDetailView;

    /**
	 * Constructor which initializes this object before the UI is built
	 */
    public GlobalController() {
        setChannelItemListModel(new ChannelItemListModel());
        builder = new ChannelBuilder();
        channelReg = new ChannelRegistry(builder);
    }

    /**
	 * Initialization that has to happen after the UI has been built.
	 */
    public void PostUIInit() {
        setRegistry(channelReg);
        initChannelTree();
    }

    /**
	 * newItemInChannel
	 * @param chan parameter for newItemInChannel
	 * @param title parameter for newItemInChannel
	 */
    public void newItemInChannel(ChannelIF chan, String title) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(chan);
    }

    /**
	 * setRegistry
	 * @param areg parameter for setRegistry
	 */
    public void setRegistry(ChannelRegistry areg) {
        channelReg = areg;
    }

    /**
	 * getRegistry
	 * @return the returned ChannelRegistry
	 */
    public ChannelRegistry getRegistry() {
        return channelReg;
    }

    /**
	 * getChannelTree
	 * @return the returned JTree
	 */
    public JTree getChannelTree() {
        return channelTree;
    }

    /**
	 * setChannelTree
	 * @param channelTree parameter for setChannelTree
	 */
    public void setChannelTree(JTree channelTree) {
        this.channelTree = channelTree;
        if (chanTreeModel != null) {
            channelTree.setModel(chanTreeModel);
        }
    }

    /**
	 * setChanTreeModel
	 * @param achantreemodel parameter for setChanTreeModel
	 */
    public void setChanTreeModel(ChannelTreeModel achantreemodel) {
        if (channelTree != null) {
            channelTree.setModel(achantreemodel);
        }
    }

    /**
	 * getChanTreeModel
	 * @return the returned ChannelTreeModel
	 */
    public ChannelTreeModel getChanTreeModel() {
        return chanTreeModel;
    }

    /**
	/*
	  * (Re) Initialize the ChannelTreeModel that in the GlobalController. Recreate the root, and stitch everything together
	  */
    public void initChannelTree() {
        if (channelReg == null || channelTree == null) {
            throw new IllegalArgumentException();
        }
        log.info("initChannelTree");
        ChannelTreeNode root = new ChannelTreeNode();
        chanTreeModel = new ChannelTreeModel(root);
        channelTree.setModel(chanTreeModel);
        channelTree.expandRow(0);
        channelTree.addTreeSelectionListener(new ChannelTreeSelectionListener(this));
    }

    /**
	 * Update the user interface after a channel gets selected
	 * @param selectedChannel The selected channel
	 */
    public void channelSelected(ChannelIF selectedChannel) {
        updateChannelStamp(selectedChannel);
        channelItemListModel.setTheChannel(selectedChannel);
        channelItemListModel.displayChannel();
        log.info("Selected Channel: " + selectedChannel);
    }

    /**
	 * Called when the contents of a channel has changed to update all the parts of
	 * the UI which depends on it.
	 * @param updatedChannel Channel that was updated (duh)
	 */
    public void channelUpdated(ChannelIF updatedChannel) {
        updateChannelStamp(updatedChannel);
        updateChannelTreeNode(updatedChannel);
    }

    /**
	     * Called to cause the ChannelTree to be redrawn if necessary as the result of
	     * an update to a specific Informa Channel. Also handle the case when the Channel
	 * is not found in the Tree, in which case a node has to be added.
	 */
    public void updateChannelTreeNode(ChannelIF chan) {
        log.info("updateChannelNode" + chan.toString());
        ChannelTreeNode theNode = locateChannelTreeNode(chan);
        if (theNode != null) {
            log.info("... existing node changed: " + theNode.toString());
            chanTreeModel.nodeChanged(theNode.getParent());
            chanTreeModel.reload(theNode);
        } else {
            log.info("... this is new channel in the tree");
            channelAdded(chan);
        }
        chanTreeModel.dumpToTraceWindow();
    }

    public void channelAdded(ChannelIF chan) {
        log.info("Channel Added: " + chan.toString());
        ChannelTreeNode newNode = new ChannelTreeNode(chan);
        ChannelTreeNode rootNode = (ChannelTreeNode) chanTreeModel.getRoot();
        int insertionPoint = rootNode.getChildCount();
        rootNode.insert(newNode, insertionPoint);
        chanTreeModel.reload(rootNode);
        channelTree.expandRow(0);
    }

    /**
	 * Given an Informa ChannelIF, locate the ChannelTreeNode which represents it.
	 * Return null if none is located.
	 */
    public ChannelTreeNode locateChannelTreeNode(ChannelIF chan) {
        ChannelTreeNode userObject = null;
        ChannelTreeNode curr = (ChannelTreeNode) chanTreeModel.getRoot();
        while (curr != null && curr.getUserObject() != chan) {
            curr = (ChannelTreeNode) curr.getNextNode();
        }
        userObject = curr;
        return userObject;
    }

    /**
	 * Called when an item is selected anywhere in the UI to update all relevant
	 * other windows etc.
	 * @param sItem Selected item
	 */
    public void itemSelected(ItemIF sItem) {
        if (sItem != null) {
            log.info("sel item: Ch:" + sItem.getChannel().getTitle() + " Title:" + sItem.getTitle() + " subj:" + sItem.getSubject() + " date: " + sItem.getDate() + "\n     creatr: " + sItem.getCreator() + " found:" + sItem.getFound() + " ID:" + sItem.getId() + " \ndesc:" + sItem.getDescription() + "\n");
            itemDetailView.update(sItem.getTitle(), sItem.getCreator(), sItem.getDate(), sItem.getSubject(), sItem.getDescription(), sItem.getFound(), sItem.getLink());
        }
    }

    /**
	 * updateChannelStamp
	 * @param theChan parameter for updateChannelStamp
	 */
    public void updateChannelStamp(ChannelIF theChan) {
        log.info("update the stamp now: " + theChan.toString());
        if (theChan.getFormat() == ChannelFormat.UNKNOWN_CHANNEL_FORMAT) {
            channelStampView.displayChannel("Unknown Channel", "", "", "", "", "");
        } else {
            String pubDateAsString = "";
            Date pubdate = theChan.getPubDate();
            if (pubdate != null) {
                DateFormat pubDatefmt = DateFormat.getDateInstance(DateFormat.SHORT);
                pubDateAsString = pubDatefmt.format(pubdate);
            }
            ChannelFormat fmt = theChan.getFormat();
            String fmtAsString = "";
            if (fmt != null) fmtAsString = fmt.toString();
            channelStampView.displayChannel(theChan.getTitle(), theChan.getDescription(), theChan.getPublisher(), fmtAsString, pubDateAsString, "0");
        }
    }

    /**
	 * setLogPane
	 * @param logPane parameter for setLogPane
	 */
    public void setLogPane(javax.swing.JTextPane logPane) {
        this.logPane = logPane;
    }

    /**
	 * getLogPane
	 * @return the returned javax.swing.JTextPane
	 */
    public javax.swing.JTextPane getLogPane() {
        return logPane;
    }

    /**
	 * setChannelStampView
	 * @param channelStampView parameter for setChannelStampView
	 */
    public void setChannelStampView(ChannelStampView theCSV) {
        this.channelStampView = theCSV;
    }

    /**
	 * getChannelStampView
	 * @return the returned ChannelStampView
	 */
    public ChannelStampView getChannelStampView() {
        return channelStampView;
    }

    /**
	 * setChannelItemListModel
	 * @param aItemListModel parameter for setChannelItemListModel
	 */
    public void setChannelItemListModel(ChannelItemListModel aItemListModel) {
        channelItemListModel = aItemListModel;
        channelItemListModel.setGlobalModel(this);
    }

    /**
	 * getChannelItemListModel
	 * @return the returned ChannelItemListModel
	 */
    public ChannelItemListModel getChannelItemListModel() {
        return channelItemListModel;
    }

    /**
	 * setItemDetailView
	 * @param itemDetailView parameter for setItemDetailView
	 */
    public void setItemDetailView(ItemDetailView itemDetailView) {
        this.itemDetailView = itemDetailView;
    }

    /**
	 * getItemDetailView
	 * @return the returned ItemDetailView
	 */
    public ItemDetailView getItemDetailView() {
        return itemDetailView;
    }

    public ChannelObserver getChanObs() {
        return chanObs;
    }

    public void setChanObs(ChannelObserver chanObs) {
        this.chanObs = chanObs;
    }
}
