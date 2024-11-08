package wtanaka.praya.bnet;

import wtanaka.praya.Protocol;
import wtanaka.praya.obj.Message;

/**
 * Sent when you join a channel, informing you which channel you are in.
 *
 * <p>
 * Return to <A href="http://sourceforge.net/projects/praya/">
 * <IMG src="http://sourceforge.net/sflogo.php?group_id=2302&type=1"
 *   alt="Sourceforge" width="88" height="31" border="0"></A>
 * or the <a href="http://praya.sourceforge.net/">Praya Homepage</a>
 *
 * @author $Author: wtanaka $
 * @version $Name:  $ $Date: 2003/12/17 01:27:21 $
 **/
public class BnetChannelMessage extends Message {

    private String channelName;

    /**
    * Constructor.
    * @param p the protocol instance that generated this message
    * @param channelName the name of the channel you are now a part
    * of.
    **/
    public BnetChannelMessage(Protocol p, String channelName) {
        super(p);
        this.channelName = channelName;
    }

    public String getSubject() {
        return "You have joined \"" + channelName + '"';
    }

    public String getChannelName() {
        return channelName;
    }
}
