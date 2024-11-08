package vavi.net.im.protocol.oscar.handler;

import java.io.IOException;
import vavi.net.im.event.IMEvent;
import vavi.net.im.protocol.oscar.OscarEventName;
import vavi.net.im.protocol.oscar.command.Command;
import vavi.net.im.protocol.oscar.flap.FlapConstants;

/**
 * ReconnectingHandler. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 051117 nsano initial version <br>
 */
public class BosConnectingHandler extends Handler {

    /** */
    public void exec(Command command) throws IOException {
        if (command.getChannel() == FlapConstants.FLAP_CHANNEL_CONNECT) {
            listeners.eventHappened(new IMEvent(this, OscarEventName.bosConnecting));
        }
    }
}
