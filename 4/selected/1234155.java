package org.rascalli.framework.eca;

import org.rascalli.framework.core.CommunicationChannel;
import org.rascalli.framework.event.Event;

/**
 * <p>
 *
 * </p>
 *
 * <p>
 * <b>Company:&nbsp;</b> SAT, Research Studios Austria
 * </p>
 *
 * <p>
 * <b>Copyright:&nbsp;</b> (c) 2007
 * </p>
 *
 * <p>
 * <b>last modified:</b><br/>
 * $Author: christian $<br/>
 * $Date: 2009-07-22 14:01:34 +0200 (Mi, 22 Jul 2009) $<br/>
 * $Revision: 2446 $
 * </p>
 *
 * @author Christian Schollum
 */
public class UserScolding implements Event {

    CommunicationChannel channel;

    public UserScolding() {
        channel = CommunicationChannel.ECA;
    }

    public UserScolding(CommunicationChannel channel) {
        this.channel = channel;
    }

    public CommunicationChannel getChannel() {
        return channel;
    }
}
