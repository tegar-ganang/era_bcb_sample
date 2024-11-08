package vavi.net.im.protocol.oscar.handler;

import java.io.IOException;
import vavi.net.im.event.IMEvent;
import vavi.net.im.protocol.oscar.OscarEventName;
import vavi.net.im.protocol.oscar.command.Command;
import vavi.net.im.protocol.oscar.flap.FlapConstants;
import vavi.net.im.protocol.oscar.snac.SNACConstants;

/**
 * ReconnectingHandler. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 051117 nsano initial version <br>
 */
public class BosConnectedHandler extends Handler {

    /** */
    public void exec(Command command) throws IOException {
        if (command.getChannel() == FlapConstants.FLAP_CHANNEL_SNAC) {
            if ((command.getFamily() == SNACConstants.SNAC_FAMILY_GENERIC_SERVICE_CONTROLS) && (command.getSubType() == SNACConstants.FAMILY_LIST)) {
                listeners.eventHappened(new IMEvent(this, OscarEventName.bosConnected));
            }
        }
    }
}
