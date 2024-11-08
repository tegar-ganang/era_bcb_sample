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
 * <b>last modified:</b><br/> $Author: christian $<br/> $Date: 2008-02-07
 * 17:43:47 +0100 (Do, 07 Feb 2008) $<br/> $Revision: 2446 $
 * </p>
 * 
 * @author Christian Schollum
 */
public class UserUtterance implements Event {

    private final CommunicationChannel channel;

    private final String text;

    /**
     * @param userUtteranceText
     */
    public UserUtterance(CommunicationChannel channel, String text) {
        this.channel = channel;
        this.text = text;
    }

    /**
     * @return the channel
     */
    public CommunicationChannel getChannel() {
        return channel;
    }

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "UserUtterance[channel='" + channel + "',text='" + text + "']";
    }
}
