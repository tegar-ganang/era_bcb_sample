package org.beepcore.beep.core.serialize;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 *
 * @author Huston Franklin
 * @version $Revision: 1.1 $, $Date: 2006/02/25 18:02:49 $
 */
public class StartElement extends ChannelIndication {

    private static final Collection emptyCollection = Collections.unmodifiableCollection(new LinkedList());

    private int channelNumber;

    private String serverName;

    private Collection profiles;

    public StartElement(int channelNumber, String serverName, Collection profiles) {
        super(ChannelIndication.START);
        this.channelNumber = channelNumber;
        this.serverName = serverName;
        this.profiles = Collections.unmodifiableCollection(profiles);
    }

    public StartElement(int channelNumber, Collection profiles) {
        this(channelNumber, null, profiles);
    }

    public StartElement() {
        super(ChannelIndication.START);
        channelNumber = 0;
        serverName = null;
        profiles = emptyCollection;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    public String getServerName() {
        return serverName;
    }

    public Collection getProfiles() {
        return profiles;
    }

    public void setChannelNumber(int channelNumber) {
        this.channelNumber = channelNumber;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setProfiles(Collection profiles) {
        this.profiles = Collections.unmodifiableCollection(profiles);
    }
}
