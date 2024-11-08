package com.spring.rssReader.web;

import com.spring.rssReader.Channel;
import com.spring.rssReader.jdbc.IChannelDAO;
import com.spring.workflow.util.StringUtils;
import junit.framework.AssertionFailedError;
import net.sourceforge.jwebunit.WebTestCase;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * User: Ronald Date: 4-mei-2004 Time: 21:46:17
 */
public class TestNewChannels extends WebTestCase {

    static {
        System.setProperty("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser");
    }

    protected static ApplicationContext context = new FileSystemXmlApplicationContext(new String[] { "\\java/springworkflow/webroot/WEB-INF/applicationContext-jdbc.xml" });

    protected JdbcTemplate template;

    public void setUp() {
    }

    public void testRemoveBodyTags() {
        template = new JdbcTemplate((DataSource) context.getBean("dataSource"));
        List channels = template.queryForList("select channelId from channel");
        for (int j = 0; j < channels.size(); j++) {
            Map channelRecord = (Map) channels.get(j);
            Long channelId = (Long) channelRecord.get("channelId");
            List itemIds = template.queryForList("select itemId, description from item where channelId=" + channelId.longValue());
            for (int i = 0; i < itemIds.size(); i++) {
                Map record = (Map) itemIds.get(i);
                Long id = (Long) record.get("itemId");
                String description = (String) record.get("description");
                if (description == null) {
                    continue;
                }
                int position = description.toLowerCase().indexOf("</body>");
                boolean update = false;
                if (position != -1) {
                    description = StringUtils.replaceCaseInSensitive(description, "</body>", "&lt;/body&gt;");
                    update = true;
                }
                position = description.toLowerCase().indexOf("</html>");
                if (position != -1) {
                    description = StringUtils.replaceCaseInSensitive(description, "</html>", "&lt;/html&gt;");
                    update = true;
                }
                if (update) {
                    template.update("update item set description=? where itemId=?", new Object[] { description, id }, new int[] { Types.VARCHAR, Types.BIGINT });
                }
            }
        }
    }

    public void testInsertArticle() {
        try {
            getTestContext().setBaseUrl("http://localhost/springworkflow");
            beginAt("/index.go");
            assertLinkPresent("adminMethods");
            clickLink("adminMethods");
            assertFormElementPresent("username");
            assertFormElementPresent("password");
            assertButtonPresent("login");
            setFormElement("username", "system");
            setFormElement("password", "manager");
            clickButton("login");
            clickLink("getArticles");
            clickButton("newChannel");
            assertFormElementPresent("title");
            assertFormElementPresent("url");
            setFormElement("title", "test new article");
            setFormElement("url", "http://localhost/springworkflow/index.html");
            selectOption("html", "Yes");
            clickButton("save");
            assertTextPresent("test new article");
            IChannelDAO channelDao = (IChannelDAO) context.getBean("channelDAO");
            List results = channelDao.findChannelsByUrl("http://localhost/springworkflow/index.html");
            assertTrue(results.size() == 1);
            Channel insertedChannel = (Channel) results.get(0);
            assertTrue(insertedChannel.getNumberOfItems() == 0);
            assertTrue(insertedChannel.getNumberOfRead() == 0);
            assertTrue(insertedChannel.isHtml());
        } catch (Exception e) {
            dumpResponse(System.out);
            throw new AssertionFailedError(e.getMessage());
        }
    }

    public void testInsertChannelWithExistingUrl() {
        try {
            this.testInsertArticle();
            clickButton("newChannel");
            assertFormElementPresent("title");
            assertFormElementPresent("url");
            setFormElement("title", "test new article");
            setFormElement("url", "http://localhost/springworkflow/index.html");
            selectOption("html", "Yes");
            clickButton("save");
            assertTextPresent("url is not unique");
        } catch (Exception e) {
            dumpResponse(System.out);
            throw new AssertionFailedError(e.getMessage());
        }
    }

    public void testSaveAndPollHtml() {
        try {
            getTestContext().setBaseUrl("http://localhost/springworkflow");
            beginAt("/index.go");
            assertLinkPresent("adminMethods");
            clickLink("adminMethods");
            assertFormElementPresent("username");
            assertFormElementPresent("password");
            assertButtonPresent("login");
            setFormElement("username", "system");
            setFormElement("password", "manager");
            clickButton("login");
            clickLink("getArticles");
            clickButton("newChannel");
            assertFormElementPresent("title");
            assertFormElementPresent("url");
            setFormElement("title", "test new article");
            setFormElement("url", "http://localhost/springworkflow/index.html");
            selectOption("html", "Yes");
            clickButton("saveAndPoll");
            assertTextPresent("test new article");
            int channelID = template.queryForInt("select channelID from channel where " + "url='http://localhost/springworkflow/index.html'");
            List items = template.queryForList("select * from item where channelID=" + channelID);
            assertTrue(items.size() == 1);
            IChannelDAO channelDao = (IChannelDAO) context.getBean("channelDAO");
            List results = channelDao.findChannelsByUrl("http://localhost/springworkflow/index.html");
            assertTrue(results.size() == 1);
            Channel insertedChannel = (Channel) results.get(0);
            assertTrue(insertedChannel.getNumberOfItems() == 1);
            assertTrue(insertedChannel.getNumberOfRead() == 0);
            assertTrue(insertedChannel.isHtml());
        } catch (AssertionFailedError e) {
            dumpResponse(System.out);
            throw new AssertionFailedError(e.getMessage());
        }
    }

    public void setupChannelTests() {
        try {
            getTestContext().setBaseUrl("http://localhost/springworkflow");
            beginAt("/index.go");
            assertLinkPresent("adminMethods");
            clickLink("adminMethods");
            assertFormElementPresent("username");
            assertFormElementPresent("password");
            assertButtonPresent("login");
            setFormElement("username", "system");
            setFormElement("password", "manager");
            clickButton("login");
            clickLink("getChannelsWithNews");
            clickButton("newChannel");
            assertFormElementPresent("title");
            assertFormElementPresent("url");
            setFormElement("title", "test new channel");
            setFormElement("url", "http://localhost/springworkflow/channel.xml");
            selectOption("html", "No");
            clickButton("saveAndPoll");
            assertTextNotPresent("test new channel");
            assertTextPresent("Updated title");
            int channelID = template.queryForInt("select channelID from channel where " + "url='http://localhost/springworkflow/channel.xml'");
            List items = template.queryForList("select * from item where channelID=" + channelID);
            assertTrue(items.size() == 2);
            IChannelDAO channelDao = (IChannelDAO) context.getBean("channelDAO");
            List results = channelDao.findChannelsByUrl("http://localhost/springworkflow/channel.xml");
            assertTrue(results.size() == 1);
            Channel insertedChannel = (Channel) results.get(0);
            assertTrue(insertedChannel.getNumberOfItems() == 2);
            assertTrue(insertedChannel.getNumberOfRead() == 0);
            assertTrue(!insertedChannel.isHtml());
        } catch (AssertionFailedError e) {
            dumpResponse(System.out);
            throw new AssertionFailedError(e.getMessage());
        }
    }
}
