package vavi.net.im.protocol.oscar.handler;

import java.io.IOException;
import java.util.logging.Logger;
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
public class BosServiceListHandler extends Handler {

    /** */
    private static Logger log = Logger.getLogger(BosServiceListHandler.class.getName());

    /** */
    public void exec(Command command) throws IOException {
        if (command.getChannel() == FlapConstants.FLAP_CHANNEL_SNAC) {
            if (command.getFamily() == SNACConstants.SNAC_FAMILY_GENERIC_SERVICE_CONTROLS) {
                if (command.getSubType() == SNACConstants.MSG_OF_THE_DAY) {
                    log.info("ignoring the MSG OF THE DAY");
                } else if (command.getSubType() == SNACConstants.SRV_SVC_VERSIONS) {
                    listeners.eventHappened(new IMEvent(this, OscarEventName.bosServiceList));
                }
            }
        }
    }
}
