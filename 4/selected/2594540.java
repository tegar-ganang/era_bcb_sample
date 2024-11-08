package de.nava.informa.impl.hibernate;

import java.sql.*;
import java.sql.DriverManager;
import java.util.*;
import java.util.Iterator;
import net.sf.hibernate.HibernateException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.nava.informa.utils.PersistChanGrpMgr;

/**
 * TestPersistentChannelGroup - description...
 * 
 */
public class TestPersistentChannelGroup {

    private Connection conn;

    private SessionHandler handler;

    private static Log logger = LogFactory.getLog(TestPersistentChannelGroup.class);

    public static void main(String[] args) {
        TestPersistentChannelGroup root = new TestPersistentChannelGroup();
        root.checkChannelConsistency();
        root.testFinalize();
        System.out.println("All Done!");
        System.exit(0);
    }

    /**
   * checkChannelConsistency - 
   * In this test I assume that there is a Channel Group called "Group with Channels"
   * I want to see if the actual identity of the Group, Channel and Items are consistent. 
   *  - 
   */
    private void checkChannelConsistency() {
        Channel achannel;
        List before = new ArrayList();
        PersistChanGrpMgr bgroup = testEmptyGroup("Group With Channels");
        logVerboseChannelGroup(bgroup, before);
        achannel = bgroup.addChannel("http://radio.weblogs.com/0125664/rss.xml");
        bgroup.notifyChannelsAndItems(achannel);
        bgroup.activate();
        List after = new ArrayList();
        logVerboseChannelGroup(bgroup, after);
        for (int i = 0; i < before.size(); i++) if (before.get(i) != after.get(i)) logger.info("Before and After are different at item " + i);
    }

    /**
   * logVerboseChannelGroup - 
   * 
   * @param bgroup - 
   */
    private void logVerboseChannelGroup(PersistChanGrpMgr bgroup, List l) {
        ChannelGroup g = bgroup.getChannelGroup();
        l.clear();
        String s = "Verbose dump of Group\n  group: " + g.toString() + "(" + g.getId() + ")\n";
        s = s + g.getChannels().toString() + "\n";
        Iterator chans = g.getChannels().iterator();
        while (chans.hasNext()) {
            Channel c = (Channel) chans.next();
            l.add(c);
            s = s + "Channel ids: " + c.getId() + " ";
        }
        s = s + "\n";
        logger.info(s);
    }

    /**
   * testActivateGroupWithoutChannels - 
   * 
   *  - 
   */
    private void testActivateGroupWithoutChannels() {
        PersistChanGrpMgr agroup = testEmptyGroup("Group Foo");
        agroup.activate();
        PersistChanGrpMgr bgroup = testEmptyGroup("Never Here");
        bgroup.activate();
    }

    /**
   * testGroupWithChannels - 
   * 
   *  - 
   */
    private void testGroupWithChannels() {
        PersistChanGrpMgr agroup;
        agroup = testEmptyGroup("Group With Channels");
        logChanGroup(agroup);
        testAddingChannel(agroup, "Joho", "http://www.hyperorg.com/blogger/index.rdf");
        testAddingChannel(agroup, "Raliable", "http://www.raibledesigns.com/rss/rd");
        testAddingChannel(agroup, "Active Window", "http://www.activewin.com/awin/headlines.rss");
        testAddingChannel(agroup, "Pitos Blog", "http://radio.weblogs.com/0125664/rss.xml");
        agroup.activate();
    }

    /**
   * logChanGroup - 
   * 
   * @param agroup - 
   */
    private void logChanGroup(PersistChanGrpMgr agroup) {
        Iterator chans = agroup.channelIterator();
        logger.info("Listing channel: " + agroup.toString());
        while (chans.hasNext()) {
            logger.info(chans.next().toString());
        }
    }

    /**
   * testAddingChannel - 
   * 
   * @param agroup
   * @param string
   * @param string2 - 
   */
    private void testAddingChannel(PersistChanGrpMgr agroup, String label, String url) {
        Channel achannel;
        logger.info("Adding channel: " + label + " to " + agroup + " " + url);
        achannel = agroup.addChannel(url);
        agroup.notifyChannelsAndItems(achannel);
    }

    /**
   * testFinalize - 
   * 
   *  - 
   */
    private void testFinalize() {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
   * testEmptyGroups - 
   * 
   *  - 
   */
    private void testEmptyGroups() {
        PersistChanGrpMgr agroup;
        agroup = testEmptyGroup("Group Foo");
        agroup = testEmptyGroup("Group Bar");
        agroup = testEmptyGroup("Group Too");
    }

    /**
   * testEmptyGroup - 
   * 
   * @param string - 
   */
    private PersistChanGrpMgr testEmptyGroup(String name) {
        PersistChanGrpMgr res;
        logger.info("Creating group: " + name);
        res = new PersistChanGrpMgr(handler, false);
        res.createGroup(name);
        logger.info("Result: " + res);
        return res;
    }

    /**
   * setup - 
   * 
   *  - 
   */
    private TestPersistentChannelGroup() {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            conn = DriverManager.getConnection("jdbc:hsqldb:informa", "sa", "");
            handler = SessionHandler.getInstance();
            handler.setConnection(conn);
        } catch (HibernateException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
