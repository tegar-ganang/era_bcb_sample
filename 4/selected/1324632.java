package com.goodcodeisbeautiful.syndic8.rss20;

import java.util.LinkedList;
import java.util.List;
import org.xml.sax.helpers.DefaultHandler;
import com.goodcodeisbeautiful.test.util.CommonTestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

public class Rss20Test extends CommonTestCase {

    public static Test suite() {
        return new TestSuite(Rss20Test.class);
    }

    private Rss20 m_rss20;

    protected List getSetupFilenames() {
        List l = new LinkedList();
        l.add("simple-rss20.xml");
        return l;
    }

    protected void setUp() throws Exception {
        super.setUp();
        m_rss20 = new Rss20();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        m_rss20 = null;
    }

    public void testRss20() {
        new Rss20();
    }

    public void testGetChannel() throws Exception {
        assertNull(m_rss20.getChannel());
        m_rss20.setChannel(new Rss20Channel());
        assertNotNull(m_rss20.getChannel());
    }

    public void testGetModules() throws Exception {
        assertNull(m_rss20.getModules());
        m_rss20.addModule(new Rss20Module() {

            public String getNamespace() {
                return null;
            }

            public DefaultHandler getHandler() {
                return null;
            }
        });
        assertNotNull(m_rss20.getModules());
        assertEquals(1, m_rss20.getModules().length);
        assertNotNull(m_rss20.getModules()[0]);
        m_rss20.addModule(new Rss20Module() {

            public String getNamespace() {
                return null;
            }

            public DefaultHandler getHandler() {
                return null;
            }
        });
        assertNotNull(m_rss20.getModules());
        assertEquals(2, m_rss20.getModules().length);
        assertNotNull(m_rss20.getModules()[0]);
        assertNotNull(m_rss20.getModules()[1]);
    }

    public void testAddModule() throws Exception {
        testGetModules();
    }

    public void testSetChannel() throws Exception {
        assertNull(m_rss20.getChannel());
        m_rss20.setChannel(new Rss20Channel());
        assertNotNull(m_rss20.getChannel());
        m_rss20.setChannel(null);
        assertNull(m_rss20.getChannel());
    }
}
