package com.spring.rssReader;

import junit.framework.TestCase;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import com.spring.rssReader.jdbc.ChannelController;
import java.util.List;

/**
 * User: Ronald Date: 4-mei-2004 Time: 20:59:05
 */
public class TestChannelController extends TestCase {

    static {
        System.setProperty("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser");
    }

    private static ApplicationContext context = new FileSystemXmlApplicationContext(new String[] { "f:/springworkflow/webroot/WEB-INF/applicationContext-jdbcTest.xml", "f:/springworkflow/webroot/WEB-INF/workflow-servlet.xml" });

    public void testInsertFindAndDelete() {
        ChannelController channelController = (ChannelController) context.getBean("channelController");
        Channel channel = new Channel();
        channel.setUrl("http://localhost/springworkflow/index.html");
        channel.setHtml(true);
        channelController.update(channel);
        Channel insertedChannel = channelController.getChannel(channel.getId());
        assertTrue(insertedChannel.getUrl().equals(channel.getUrl()));
        List results = channelController.getChannelsByUrl("http://localhost/springworkflow/index.html");
        assertTrue(results.size() == 1);
        Channel foundChannel = (Channel) results.get(0);
        assertTrue(foundChannel.getUrl().equals(insertedChannel.getUrl()));
        assertTrue(channelController.getAllItems(foundChannel.getId()).size() == 0);
        channelController.remove(foundChannel);
        results = channelController.getChannelsByUrl("http://localhost/springworkflow/index.html");
        assertTrue(results.size() == 0);
    }
}
