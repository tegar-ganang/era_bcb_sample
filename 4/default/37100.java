import java.util.logging.Logger;
import com.rubixinfotech.SKJava.*;
import com.rubixinfotech.SKJava.Messages.EXS.*;

public class MyChanGroupEventListener implements SKJChanGroupEventListener {

    private static final Logger logger = Logger.getLogger("MyChanGroupEventListener");

    MyChanGroupEventListener() {
    }

    public boolean onEvent(SKJMessage msg) {
        logger.finest("MyChanGroupEventListener got an " + msg.toString() + "event");
        return false;
    }

    public boolean onEvent(String chanGroup, SKJMessage msg) {
        logger.finest("MyChanGroupEventListener got an " + msg.toString() + "event on group '" + chanGroup + "'");
        if (msg instanceof XL_RFSWithData) {
            XL_RFSWithData rfsd = (XL_RFSWithData) msg;
            logger.finest("RFS With Data on Port:" + rfsd.getSpan() + ":" + rfsd.getChannel() + " - " + SKJMessage.printableFormat(rfsd.getData()));
        } else if (msg instanceof XL_PPLEventIndication) {
            XL_PPLEventIndication pei = (XL_PPLEventIndication) msg;
            logger.finest("PPL Event Indication on Component:" + pei.getComponentID() + ", Event:" + pei.getPPLEvent() + ", AIB: " + SKJMessage.printableFormat(pei.getAddrInfo()) + ", Data:" + SKJMessage.printableFormat(pei.getData()));
        }
        return false;
    }
}
