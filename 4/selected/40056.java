package ru.pit.tvlist.persistence.service;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;
import ru.pit.tvlist.persistence.TestUtils;
import ru.pit.tvlist.persistence.dao.DAOFactory;
import ru.pit.tvlist.persistence.dao.IBroadcastDAO;
import ru.pit.tvlist.persistence.dao.IChannelDAO;
import ru.pit.tvlist.persistence.domain.Broadcast;
import ru.pit.tvlist.persistence.exception.PersistenceException;

/**
 * 
 */
public class TVListServiceTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
        TestUtils.loadData();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtils.clearData();
        TestUtils.getHibernateSingletonSessionFactory().closeSessions();
    }

    /**
     * Test method for {@link ru.pit.tvlist.persistence.service.TVListService#TVListService(ru.pit.tvlist.persistence.dao.IChannelDAO, ru.pit.tvlist.persistence.dao.IBroadcastDAO)}.
     */
    public final void testTVListService() {
        IChannelDAO channelDAO = DAOFactory.getChannelDAO(TestUtils.getHibernateSessionFactory());
        IBroadcastDAO broadcastDAO = DAOFactory.getBroadcastDAO(TestUtils.getHibernateSessionFactory());
        TVListService service = new TVListService(channelDAO, broadcastDAO);
        assertNotNull("service is null", service);
        assertEquals("channelDAO", service.getChannelDAO(), channelDAO);
        assertEquals("broadcastDAO", service.getBroadcastDAO(), broadcastDAO);
    }

    /**
     * Test method for {@link ru.pit.tvlist.persistence.service.TVListService#setBroadcastDAO(ru.pit.tvlist.persistence.dao.IBroadcastDAO)}, {@link ru.pit.tvlist.persistence.service.TVListService#setChannelDAO(ru.pit.tvlist.persistence.dao.IChannelDAO)}.
     */
    public final void testSetDAOs() {
        IBroadcastDAO broadcastDAO = DAOFactory.getBroadcastDAO(TestUtils.getHibernateSessionFactory());
        IChannelDAO channelDAO = DAOFactory.getChannelDAO(TestUtils.getHibernateSessionFactory());
        TVListService service = new TVListService(DAOFactory.getChannelDAO(TestUtils.getHibernateSessionFactory()), DAOFactory.getBroadcastDAO(TestUtils.getHibernateSessionFactory()));
        assertNotNull("service is null", service);
        service.setBroadcastDAO(broadcastDAO);
        service.setChannelDAO(channelDAO);
        assertEquals("broadcastDAO", service.getBroadcastDAO(), broadcastDAO);
        assertEquals("channelDAO", service.getChannelDAO(), channelDAO);
    }

    /**
     * Test method for {@link ru.pit.tvlist.persistence.service.TVListService#getChannels()}.
     * @throws Exception 
     * @throws PersistenceException 
     */
    public final void testGetChannels() throws Exception {
        List channels = TestUtils.getService().getChannels();
        assertEquals("channels.size", channels.size(), 4);
    }

    /**
     * Test method for {@link ru.pit.tvlist.persistence.service.TVListService#getBroadcastsByChannel(java.lang.Long, java.util.GregorianCalendar, java.util.GregorianCalendar, java.util.GregorianCalendar)}.
     * @throws Exception 
     */
    public final void testGetBroadcastsByChannel() throws Exception {
        Long channelId = new Long(4);
        List ids = new ArrayList();
        ids.add(new Long(9));
        ids.add(new Long(10));
        ids.add(new Long(14));
        GregorianCalendar day = new GregorianCalendar(2006, 9, 10);
        GregorianCalendar fromTime = new GregorianCalendar(2006, 9, 10, 15, 00);
        GregorianCalendar toTime = new GregorianCalendar(2006, 9, 10, 17, 00);
        List broadcasts = TestUtils.getService().getBroadcastsByChannel(channelId, day, fromTime, toTime);
        assertEquals("broadcasts.size", broadcasts.size(), 3);
        Iterator iter = broadcasts.iterator();
        while (iter.hasNext()) {
            Broadcast broadcast = (Broadcast) iter.next();
            assertTrue("wrong id", ids.contains(broadcast.getId()));
        }
    }

    /**
     * Test method for {@link ru.pit.tvlist.persistence.service.TVListService#getBroadcasts(java.util.GregorianCalendar, java.util.GregorianCalendar, java.util.GregorianCalendar)}.
     */
    public final void testGetBroadcasts() {
    }

    /**
     * Test method for {@link ru.pit.tvlist.persistence.service.TVListService#saveBroadcasts(java.util.List, java.lang.Long)}.
     */
    public final void testSaveBroadcasts() {
    }

    /**
     * Test method for {@link ru.pit.tvlist.persistence.service.TVListService#saveChannel(ru.pit.tvlist.persistence.domain.Channel)}.
     */
    public final void testSaveChannel() {
    }

    /**
     * Test method for {@link ru.pit.tvlist.persistence.service.TVListService#loadBroadcastsByChannel(java.util.List, java.util.GregorianCalendar, int)}.
     */
    public final void testLoadBroadcastsByChannel() {
    }
}
