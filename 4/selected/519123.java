package jhomenet.ui.tree;

import jhomenet.commons.GeneralApplicationContext;
import jhomenet.commons.hw.Channel;

/**
 * TODO: Class description.
 *
 * @author Dave Irwin (jhomenet at gmail dot com)
 */
public class ChannelNode extends CustomMutableTreeNode<Channel> {

    /**
	 * Serial version hardwareID information - used for the serialization process.
	 */
    private static final long serialVersionUID = 00001;

    /**
	 * Icon filename.
	 */
    private static final String iconFilename = "port.png";

    /**
	 * 
	 * @param channel
	 * @param treeView
	 * @param config
	 */
    public ChannelNode(Channel channel, TreeView treeView, GeneralApplicationContext serverContext) {
        super(channel, false, treeView, serverContext);
    }

    /**
	 * @see jhomenet.ui.tree.CustomMutableTreeNode#getIconFilename()
	 */
    @Override
    protected String getIconFilename() {
        return iconFilename;
    }

    /**
	 * @see jhomenet.ui.tree.CustomMutableTreeNode#getCustomToolTipText()
	 */
    public String getCustomToolTipText() {
        Channel channel = (Channel) this.getUserObject();
        StringBuffer buf = new StringBuffer();
        buf.append("<html>");
        buf.append("Channel #: " + channel.getChannelNum());
        buf.append("<br>Channel description: " + channel.getDescription());
        buf.append("</html>");
        return buf.toString();
    }

    /**
	 * @see java.lang.Object#hashCode()
	 */
    @Override
    public int hashCode() {
        Channel channel = (Channel) this.getUserObject();
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());
        return result;
    }

    /**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final ChannelNode otherNode = (ChannelNode) obj;
        final Channel otherChannel = (Channel) otherNode.getUserObject();
        final Channel channel = (Channel) this.getUserObject();
        if (channel == null) {
            if (otherChannel != null) return false;
        } else if (!channel.equals(otherChannel)) return false;
        return true;
    }
}
