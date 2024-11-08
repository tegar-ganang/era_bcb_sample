package informaclient;

import java.util.Iterator;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.utils.ChannelRegistry;

public class DumpChannelsAndWindows extends CommandHandler {

    public DumpChannelsAndWindows(HomeFrame homeFrame) {
        super(homeFrame);
    }

    public void actionPerformed() {
        ChannelRegistry theReg = theFrame.manager.getRegistry();
        Iterator chanIter = theReg.getChannels().iterator();
        while (chanIter.hasNext()) {
            ChannelIF nextChan;
            nextChan = (ChannelIF) chanIter.next();
            theFrame.log.info("Channel: " + nextChan.toString());
            Iterator itemIter = nextChan.getItems().iterator();
            while (itemIter.hasNext()) {
                ItemIF nextItem;
                nextItem = (ItemIF) itemIter.next();
                theFrame.log.info("    Item: " + nextItem.toString());
            }
        }
    }
}
