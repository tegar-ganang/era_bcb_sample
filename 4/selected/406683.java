package informaclient;

import java.util.logging.Logger;
import de.nava.informa.core.*;

public class ChannelObserver implements ChannelObserverIF {

    GlobalController theMan;

    Logger log = Logger.getLogger(this.getClass().toString());

    public ChannelObserver(GlobalController aMan) {
        theMan = aMan;
        log.info("Constructed");
    }

    public void itemAdded(ItemIF parm1) {
        String itemTitle = parm1.getTitle();
        ChannelIF chan = parm1.getChannel();
        theMan.newItemInChannel(chan, itemTitle);
        log.info("ItemAdded");
    }

    public void channelRetrieved(ChannelIF theChan) {
        log.info("channelRetrieved: " + theChan.toString());
        theMan.channelUpdated(theChan);
    }
}
