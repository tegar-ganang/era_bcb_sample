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
public class ShowChannelAction extends Action {

    private Object thinList;

    private Object thinItems;

    private Object thinSelectedChannel;

    /**
	 * @param main
	 */
    public ShowChannelAction(ThinFeeder main, Object thinSelectedChannel, Object thinList, Object thinItems) {
        super(main);
        this.thinList = thinList;
        this.thinItems = thinItems;
        this.thinSelectedChannel = thinSelectedChannel;
    }

    public void doAction() throws Exception {
        if (equalChannels(thinSelectedChannel, main.getSelectedItem(main.find("channels")))) main.removeAll(thinItems);
        long id = ((Long) main.getProperty(thinSelectedChannel, "id")).longValue();
        ChannelIF channel = DAOChannel.getChannel((long) id);
        new ShowChannelPropertiesAction(main, channel).doAction();
        new ShowChannelItemsAction(main, thinSelectedChannel, channel, thinItems).doAction();
        new ToggleChannelHasUnreadItemsAction(main, channel, thinSelectedChannel).doAction();
    }

    private boolean equalChannels(Object c1, Object c2) {
        Long c1Id = (Long) main.getProperty(c1, "id");
        Long c2Id = (Long) main.getProperty(c2, "id");
        return c1Id == null ? (c1Id == c2Id) : (c1Id.longValue() == c2Id.longValue());
    }
}
