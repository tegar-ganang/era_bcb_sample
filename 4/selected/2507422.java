package net.sourceforge.cridmanager;

import org.apache.log4j.Logger;
import javax.swing.Icon;
import net.sourceforge.cridmanager.box.BoxManager;
import net.sourceforge.cridmanager.services.ServiceProvider;

/**
 * @author hil TODO To change the template for this generated type comment go to Window -
 *         Preferences - Java - Code Style - Code Templates
 */
public class ChannelGrouping implements IGrouping {

    /**
	 * Logger for this class
	 */
    private static final Logger logger = Logger.getLogger(ChannelGrouping.class);

    private static final Icon openIcon = Utils.getTreeIcon("images/Gruppieren_Sender.gif");

    private static final Icon closedIcon = Utils.getTreeIcon("images/Gruppieren_Sender.gif");

    private BoxManager boxManager;

    /**
	 */
    public ChannelGrouping() {
    }

    public boolean match(CridInfo info) {
        if (logger.isDebugEnabled()) {
            logger.debug("match(CridInfo) - start");
        }
        boolean returnboolean = info.getCridServiceID() != 0;
        if (logger.isDebugEnabled()) {
            logger.debug("match(CridInfo) - end");
        }
        return returnboolean;
    }

    public Object getGroupingKey(CridInfo info) {
        if (logger.isDebugEnabled()) {
            logger.debug("getGroupingKey(CridInfo) - start");
        }
        Object returnObject = info.getCridFile().getParent().getURI().toASCIIString() + ":" + info.getCridServiceID();
        if (logger.isDebugEnabled()) {
            logger.debug("getGroupingKey(CridInfo) - end");
        }
        return returnObject;
    }

    public Object getGroupTitel(CridInfo info) {
        if (logger.isDebugEnabled()) {
            logger.debug("getGroupTitel(CridInfo) - start");
        }
        String channelName;
        channelName = Messages.getString("CridInfo.Channel") + " " + info.getCridServiceID();
        try {
            channelName = getBoxManager().getBox(info.getCridFile()).getChannelManager().getServiceName(info.getCridServiceID());
        } catch (Exception e) {
            logger.error("getGroupTitel(CridInfo)", e);
            e.printStackTrace();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getGroupTitel(CridInfo) - end");
        }
        return channelName;
    }

    /**
	 * @param controller The controller to set.
	 */
    public void setController(CridController controller) {
    }

    public Icon getOpenIcon() {
        return openIcon;
    }

    public Icon getClosedIcon() {
        return closedIcon;
    }

    public String toString() {
        return "Channel";
    }

    public Comparable getCompareObject(CridInfo info) {
        if (logger.isDebugEnabled()) {
            logger.debug("getCompareObject(CridInfo) - start");
        }
        Comparable returnComparable = (Comparable) getGroupTitel(info);
        if (logger.isDebugEnabled()) {
            logger.debug("getCompareObject(CridInfo) - end");
        }
        return returnComparable;
    }

    public Object getGroupingValue(CridInfo info) {
        if (logger.isDebugEnabled()) {
            logger.debug("getGroupingValue(CridInfo) - start");
        }
        Object returnObject = new Long(info.getCridServiceID());
        if (logger.isDebugEnabled()) {
            logger.debug("getGroupingValue(CridInfo) - end");
        }
        return returnObject;
    }

    public void setGroupingValue(CridInfo from, CridInfo to) {
        if (logger.isDebugEnabled()) {
            logger.debug("setGroupingValue(CridInfo, CridInfo) - start");
        }
        to.setCridServiceID(from.getCridServiceID());
        if (logger.isDebugEnabled()) {
            logger.debug("setGroupingValue(CridInfo, CridInfo) - end");
        }
    }

    private BoxManager getBoxManager() {
        if (logger.isDebugEnabled()) {
            logger.debug("getBoxManager() - start");
        }
        if (boxManager == null) {
            boxManager = (BoxManager) ServiceProvider.instance().getService(this.getClass(), BoxManager.class);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getBoxManager() - end");
        }
        return boxManager;
    }
}
