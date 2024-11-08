package net.sourceforge.thinfeeder.command.action;

import net.sourceforge.thinfeeder.ThinFeeder;
import net.sourceforge.thinfeeder.model.dao.DAOChannel;
import de.nava.informa.core.ChannelIF;

/**
 * @author fabianofranz@users.sourceforge.net
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RefreshCurrentFeedAction extends Action {

    private Object thinList;

    private Object thinItems;

    /**
	 * @param main
	 */
    public RefreshCurrentFeedAction(ThinFeeder main, Object thinList, Object thinItems) {
        super(main);
        this.thinList = thinList;
        this.thinItems = thinItems;
    }

    public void doAction() {
        Object thinSelectedChannel = main.getSelectedItem(thinList);
        if (thinSelectedChannel == null) {
            main.status(main.getI18N("i18n.select_channel_refresh"), true);
            return;
        }
        long id = ((Long) main.getProperty(thinSelectedChannel, "id")).longValue();
        int itemsUpdated = 0;
        ChannelIF channel = null;
        try {
            channel = DAOChannel.getChannel((long) id);
            main.status(main.getI18N("i18n.refreshing_feed") + channel.getLocation().toExternalForm() + main.getI18N("i18n...."));
        } catch (Exception e) {
            main.status(main.getI18N("i18n.error_10"), true);
        }
        try {
            itemsUpdated = DAOChannel.updateChannelItems(channel);
        } catch (Throwable t) {
            t.printStackTrace();
            main.status(main.getI18N("i18n.error_08") + channel.getLocation().toExternalForm() + "!", true);
        }
        try {
            new ShowChannelAction(main, thinSelectedChannel, thinList, thinItems).doAction();
        } catch (Exception e) {
        }
        main.status(main.getI18N("i18n.feed_refreshed") + itemsUpdated + " " + main.getI18N("i18n.refresh_4"));
    }
}
