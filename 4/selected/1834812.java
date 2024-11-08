package com.tirsen.hanoi.engine;

/**
 *
 *
 * <!-- $Id: ConnectorRequest.java,v 1.1.1.1 2002/07/06 16:34:59 tirsen Exp $ -->
 * <!-- $Author: tirsen $ -->
 *
 * @author Jon Tirs&eacute;n (tirsen@users.sourceforge.net)
 * @version $Revision: 1.1.1.1 $
 */
public class ConnectorRequest extends Activity {

    protected Channel channel;

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    public int run() {
        if (!channel.isBusy() && !channel.hasResponse()) channel.request();
        if (channel.hasResponse()) {
            channel.fetchResponse();
            return CONTINUE;
        } else {
            return WAIT;
        }
    }
}
