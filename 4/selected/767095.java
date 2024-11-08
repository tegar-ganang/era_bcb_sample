package com.spring.rssReader.web;

import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import com.spring.rssReader.jdbc.ChannelDAOJdbc;
import com.spring.rssReader.jdbc.IChannelDAO;
import com.spring.rssReader.jdbc.IItemDAO;
import com.spring.rssReader.Channel;
import com.spring.rssReader.Item;
import java.util.List;

/**
 * User: Ronald Date: 5-mei-2004 Time: 15:30:04
 */
public class TestItemsActions extends TestNewChannels {

    private int channelId;

    private int firstItemId;

    private int secondItemId;

    public void setUp() {
        template = new JdbcTemplate((DataSource) context.getBean("dataSource"));
        List totalChannels = template.queryForList("select * from channel");
        if (totalChannels.size() > 2) {
            fail("Woooaaa, there are more then 2 channels in the database. " + "Did you create a test database and pointed the webserver to this database in stead of the Real " + "Thing?" + "To setup the tests for the rssReader you must change the web.xml to point to the " + "application-hibernateTest.xml and in the application-hibernateTest.xml you must set the database " + "url to the name of your test database (default is rssreadertest).");
        }
        template.execute("delete from channel");
        template.execute("delete from item");
        setupChannelTests();
        channelId = template.queryForInt("select channelId from channel");
        firstItemId = template.queryForInt("select itemId from item where title='Title item 1'");
        secondItemId = template.queryForInt("select itemId from item where title='Title item 2'");
    }

    public void testMarkReadItem() {
        clickLink("getChannelsWithNews");
        StringBuffer link = new StringBuffer();
        link.append("readItems_").append(channelId);
        assertLinkPresent(link.toString());
        clickLink(link.toString());
        assertLinkPresent("markItemAsRead_" + firstItemId);
        assertLinkPresent("markItemAsRead_" + secondItemId);
        assertLinkNotPresent("markItemAsNotRead_" + firstItemId);
        assertLinkNotPresent("markItemAsNotRead_" + secondItemId);
        clickLink("markItemAsRead_" + firstItemId);
        assertLinkNotPresent("markItemAsRead_" + firstItemId);
        assertLinkPresent("markItemAsRead_" + secondItemId);
        assertLinkPresent("markItemAsNotRead_" + firstItemId);
        assertLinkNotPresent("markItemAsNotRead_" + secondItemId);
        clickLink("markItemAsNotRead_" + firstItemId);
        assertLinkPresent("markItemAsRead_" + firstItemId);
        assertLinkPresent("markItemAsRead_" + secondItemId);
        assertLinkNotPresent("markItemAsNotRead_" + firstItemId);
        assertLinkNotPresent("markItemAsNotRead_" + secondItemId);
    }

    public void testShowAllItemsNewItems() {
        clickLink("getChannelsWithNews");
        clickLink("readItems_" + channelId);
        assertLinkPresent("markItemAsRead_" + firstItemId);
        assertLinkPresent("markItemAsRead_" + secondItemId);
        assertLinkNotPresent("markItemAsNotRead_" + firstItemId);
        assertLinkNotPresent("markItemAsNotRead_" + secondItemId);
        clickLink("markItemAsRead_" + firstItemId);
        assertLinkNotPresent("markItemAsRead_" + firstItemId);
        assertLinkPresent("markItemAsRead_" + secondItemId);
        assertLinkPresent("toggleAllItems_" + channelId);
        assertTextPresent("Show new items");
        clickLink("toggleAllItems_" + channelId);
        assertLinkNotPresent("markItemAsRead_" + firstItemId);
        assertLinkNotPresent("markItemAsNotRead_" + firstItemId);
        assertTextNotPresent("Title item 1");
        assertTextPresent("Title item 2");
        assertLinkPresent("markItemAsRead_" + secondItemId);
        assertTextPresent("Show all items");
        assertLinkPresent("toggleAllItems_" + channelId);
        assertTextPresent("Show all items");
        clickLink("toggleAllItems_" + channelId);
        assertTextPresent("Title item 1");
        assertLinkPresent("markItemAsNotRead_" + firstItemId);
        assertTextPresent("Title item 2");
        assertLinkPresent("markItemAsRead_" + secondItemId);
    }

    public void testMarkChannelRead() {
        clickLink("getChannelsWithNoNews");
        assertLinkNotPresent("readItems_" + channelId);
        clickLink("getChannelsWithNews");
        clickLink("readItems_" + channelId);
        assertLinkPresent("markAsRead_" + channelId);
        clickLink("markAsRead_" + channelId);
        clickLink("getChannelsWithNews");
        assertLinkNotPresent("readItems_" + channelId);
        clickLink("getChannelsWithNoNews");
        assertLinkPresent("readItems_" + channelId);
        clickLink("readItems_" + channelId);
        assertLinkPresent("markItemAsNotRead_" + firstItemId);
        assertLinkPresent("markItemAsNotRead_" + secondItemId);
        clickLink("markItemAsNotRead_" + firstItemId);
        clickLink("getChannelsWithNoNews");
        assertLinkNotPresent("readItems_" + channelId);
        clickLink("getChannelsWithNews");
        clickLink("readItems_" + channelId);
        assertLinkPresent("markItemAsRead_" + firstItemId);
    }

    /**
	 * Test of the "delete all items". This will null out all (non-fetched) items.
	 */
    public void testDeleteItems() {
        clickLink("getChannelsWithNews");
        clickLink("readItems_" + channelId);
        clickLink("deleteAllItems_" + channelId);
        IChannelDAO channelDao = (IChannelDAO) context.getBean("channelDAO");
        Channel channel = channelDao.getChannel(new Long(channelId));
        assertTrue(channel.getNumberOfItems() == 0);
        assertTrue(channel.getNumberOfRead() == 0);
        IItemDAO itemDao = (IItemDAO) context.getBean("itemDAO");
        Item firstItem = itemDao.getItem(new Long(firstItemId));
        assertTrue(firstItem.getDescription() == null);
        assertTrue(firstItem.isRemove());
        Item secondItem = itemDao.getItem(new Long(secondItemId));
        assertTrue(secondItem.getDescription() == null);
        assertTrue(secondItem.isRemove());
    }

    /**
	 * Test when deleting all items, if only the visible items will be removed. So mark 1 message read, then only show
	 * the unread messages, delete all items, and show all unread messages. Now the first message that was read, should
	 * not have been deleted.
	 */
    public void testDeleteToggledItems() {
        clickLink("getChannelsWithNews");
        clickLink("readItems_" + channelId);
        clickLink("markItemAsRead_" + firstItemId);
        clickLink("toggleAllItems_" + channelId);
        clickLink("deleteAllItems_" + channelId);
        clickLink("getChannelsWithNoNews");
        clickLink("readItems_" + channelId);
        clickLink("toggleAllItems_" + channelId);
        assertLinkPresent("markItemAsNotRead_" + firstItemId);
    }
}
