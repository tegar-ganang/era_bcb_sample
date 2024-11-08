package ru.pit.tvlist.persistence.dao;

import java.util.Date;
import junit.framework.TestCase;
import ru.pit.tvlist.persistence.TestUtils;
import ru.pit.tvlist.persistence.domain.Broadcast;
import ru.pit.tvlist.persistence.domain.Channel;
import ru.pit.tvlist.persistence.exception.PersistenceException;

public abstract class ABaseDAOTestCase extends TestCase {

    protected IBroadcastDAO broadcastDao = null;

    protected IChannelDAO channelDao = null;

    public ABaseDAOTestCase() {
        super();
    }

    public ABaseDAOTestCase(String arg0) {
        super(arg0);
        TestUtils.getCfg();
    }

    protected void setUp() throws Exception {
        super.setUp();
        TestUtils.loadData();
        broadcastDao = DAOFactory.getBroadcastDAO(TestUtils.getHibernateSingletonSessionFactory());
        channelDao = DAOFactory.getChannelDAO(TestUtils.getHibernateSingletonSessionFactory());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        TestUtils.clearData();
        TestUtils.getHibernateSingletonSessionFactory().closeSessions();
    }

    public Broadcast getBroadcast(Long id, IBroadcastDAO dao) {
        Broadcast result = null;
        try {
            result = dao.getById(id);
        } catch (PersistenceException e) {
            fail(e.getMessage());
        }
        return result;
    }

    public Channel getChannel(Long id, IChannelDAO dao) {
        Channel result = null;
        try {
            result = dao.getById(id);
        } catch (PersistenceException e) {
            fail(e.getMessage());
        }
        return result;
    }

    protected void assertEquals(Broadcast data1, Broadcast data2) {
        assertEquals("ID", data1.getId(), data2.getId());
        assertEquals("date", data1.getDate(), data2.getDate());
        assertEquals("descr", data1.getDescr(), data2.getDescr());
        assertEquals("extId", data1.getExtId(), data2.getExtId());
        assertEquals("name", data1.getName(), data2.getName());
        assertEquals("pic", data1.getPicName(), data2.getPicName());
        assertEquals("type", data1.getType(), data2.getType());
        assertSame("channel", data1.getChannel(), data2.getChannel());
    }

    protected void assertEquals(Channel data1, Channel data2) {
        assertEquals("ID", data1.getId(), data2.getId());
        assertEquals("name", data1.getName(), data2.getName());
        assertEquals("logo", data1.getLogoName(), data2.getLogoName());
        assertEquals("number", data1.getNumber(), data2.getNumber());
        assertEquals("extId", data1.getExtId(), data2.getExtId());
        assertEquals("extId", data1.getEpgSourceType(), data2.getEpgSourceType());
    }

    protected void assertEquals(Date data1, Date data2) {
        assertEquals("year", data1.getYear(), data2.getYear());
        assertEquals("month", data1.getMonth(), data2.getMonth());
        assertEquals("day", data1.getDate(), data2.getDate());
        assertEquals("hours", data1.getHours(), data2.getHours());
        assertEquals("minutes", data1.getMinutes(), data2.getMinutes());
        assertEquals("seconds", data1.getSeconds(), data2.getSeconds());
    }

    protected void fail(Throwable e) {
        fail(e.getMessage());
    }

    public void pr(String s) {
        System.out.println(s);
    }
}
