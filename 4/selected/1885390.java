package net.sourceforge.thinfeeder.command.action;

import java.util.Iterator;
import java.util.List;
import net.sourceforge.thinfeeder.ThinFeeder;
import net.sourceforge.thinfeeder.model.dao.DAOChannel;
import de.nava.informa.core.ChannelIF;

/**
 * @author fabianofranz@users.sourceforge.net
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ShowChannelsAction extends Action {

    /**
	 * @param main
	 */
    public ShowChannelsAction(ThinFeeder main) {
        super(main);
    }

    public void doAction() throws Exception {
        Object channels = main.find("channels");
        main.removeAll(channels);
        List c = DAOChannel.getChannelsOrderByTitle();
        Iterator i = c.iterator();
        while (i.hasNext()) {
            ChannelIF channel = (ChannelIF) i.next();
            new PopulateChannelAction(main, channel).doAction();
        }
    }
}
