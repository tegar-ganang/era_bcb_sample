package ru.pit.tvlist.persistence.dao;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import ru.pit.tvlist.persistence.domain.Broadcast;
import ru.pit.tvlist.persistence.exception.PersistenceException;

/**
 * 
 */
public class BroadcastHibernateDAOTest extends ABaseDAOTestCase {

    public BroadcastHibernateDAOTest() {
        super();
    }

    public BroadcastHibernateDAOTest(String arg0) {
        super(arg0);
    }

    /**
     * Test method for {@link ru.pit.tvlist.persistence.dao.hibernate.BroadcastHibernateDAO#getById(java.lang.Long)}.
     */
    public final void testGetById() {
        Broadcast testData = null;
        Long id = new Long(1);
        try {
            testData = broadcastDao.getById(id);
        } catch (PersistenceException e) {
            fail(e.getMessage());
        }
        assertNotNull("null", testData);
        assertEquals("ID", testData.getId(), id);
        assertEquals("date", testData.getDate().getTime(), new Date(106, 9, 9, 15, 00, 10).getTime());
        assertEquals("descr", testData.getDescr(), "descr1");
        assertEquals("extId", testData.getExtId(), "ext1");
        assertEquals("name", testData.getName(), "name1");
        assertEquals("pic", testData.getPicName(), "pic1");
        assertEquals("type", testData.getType(), new Long(100));
        assertEquals("channel", testData.getChannel().getId(), new Long(1));
        testData = null;
        try {
            testData = broadcastDao.getById(new Long(100));
        } catch (PersistenceException e) {
            fail(e.getMessage());
        }
        assertNull("not null", testData);
    }

    /**
     * Test method for {@link ru.pit.tvlist.persistence.dao.hibernate.BroadcastHibernateDAO#save(ru.pit.tvlist.persistence.domain.Broadcast)}.
     */
    public final void testSave() {
        Broadcast savedData = new Broadcast();
        Long id = null;
        savedData.setName("newName");
        savedData.setExtId("newExt");
        savedData.setDescr("newDescr");
        savedData.setExtId("newExt");
        savedData.setName("newName");
        savedData.setPicName("newPic");
        savedData.setType(new Long(200));
        savedData.setChannel(getChannel(new Long(3), channelDao));
        try {
            id = broadcastDao.save(savedData);
        } catch (PersistenceException e) {
            fail(e.getMessage());
        }
        Broadcast testData = null;
        try {
            testData = broadcastDao.getById(id);
        } catch (PersistenceException e) {
            fail(e.getMessage());
        }
        assertNotNull("null", testData);
        assertEquals(savedData, testData);
    }

    /**
     * Test method for {@link ru.pit.tvlist.persistence.dao.hibernate.BroadcastHibernateDAO#update(ru.pit.tvlist.persistence.domain.Broadcast)}.
     */
    public final void testUpdate() {
        Broadcast updatedData = null;
        try {
            updatedData = broadcastDao.getById(new Long(3));
        } catch (PersistenceException e) {
            fail(e.getMessage());
        }
        updatedData.setName("newName");
        updatedData.setExtId("newExt");
        updatedData.setDescr("newDescr");
        updatedData.setExtId("newExt");
        updatedData.setName("newName");
        updatedData.setPicName("newPic");
        updatedData.setType(new Long(200));
        updatedData.setChannel(getChannel(new Long(3), channelDao));
        try {
            broadcastDao.update(updatedData);
        } catch (PersistenceException e) {
            fail(e.getMessage());
        }
        Broadcast testData = null;
        try {
            testData = broadcastDao.getById(updatedData.getId());
        } catch (PersistenceException e) {
            fail(e.getMessage());
        }
        assertNotNull("null", testData);
        assertEquals(updatedData, testData);
    }

    /**
     * Test method for {@link ru.pit.tvlist.persistence.dao.hibernate.BroadcastHibernateDAO#delete(ru.pit.tvlist.persistence.domain.Broadcast)}.
     */
    public final void testDelete() {
        Broadcast deletedData = null;
        try {
            deletedData = broadcastDao.getById(new Long(3));
        } catch (PersistenceException e) {
            fail(e.getMessage());
        }
        try {
            broadcastDao.delete(deletedData);
        } catch (PersistenceException e) {
            fail(e.getMessage());
        }
        Broadcast testData = null;
        try {
            testData = broadcastDao.getById(deletedData.getId());
        } catch (PersistenceException e) {
            fail(e.getMessage());
        }
        assertNull("not null", testData);
    }

    public void testGetByChannel() {
        Long channelId = new Long(4);
        GregorianCalendar fromTime = new GregorianCalendar(2006, 9, 10, 15, 00, 00);
        GregorianCalendar toTime = new GregorianCalendar(2006, 9, 10, 17, 00, 00);
        List list = null;
        try {
            list = broadcastDao.getByChannel(channelId, fromTime, toTime);
        } catch (PersistenceException e) {
            fail(e);
        }
        assertEquals(3, list.size());
        Iterator iter = list.iterator();
        Broadcast previousBr = null;
        while (iter.hasNext()) {
            Broadcast br = (Broadcast) iter.next();
            if (previousBr != null) {
                assertTrue("sort order", previousBr.getDate().before(br.getDate()));
            }
            assertTrue("wrong id", (br.getId().equals(new Long(9))) || (br.getId().equals(new Long(10))) || (br.getId().equals(new Long(14))));
            previousBr = br;
        }
    }
}
