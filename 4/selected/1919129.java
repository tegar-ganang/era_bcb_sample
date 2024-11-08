package org.openamf.examples;

import java.io.IOException;
import org.apache.commons.digester.rss.Channel;
import org.apache.commons.digester.rss.RSSDigester;
import org.xml.sax.SAXException;

/**
 * @author Jason Calabrese <jasonc@missionvi.com>
 * @version $Revision: 1.1 $, $Date: 2003/08/20 19:32:21 $
 */
public class RSS {

    public Channel getChannel(String rssURL) throws IOException, SAXException {
        Channel channel = null;
        RSSDigester digester = new RSSDigester();
        channel = (Channel) digester.parse(rssURL);
        String url = "http://www.openamf.org/javadocs/index.html";
        String desc = channel.getDescription() + "<br/><br/>Click <A href='" + url + "' target='_blank'><U>here</U></A> to view Java Doc's";
        channel.setDescription(desc);
        return channel;
    }
}
