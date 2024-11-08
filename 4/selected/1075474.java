package org.charvolant.tmsnet.model;

import java.util.Date;
import org.charvolant.tmsnet.resources.PO;
import org.charvolant.tmsnet.resources.TMSNet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;

/**
 * Test cases for the {@link Event} class.
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
public class EventTest {

    private static final int CHANNEL = 1;

    private static final int DURATION = 30;

    private static final Date START = new Date(0);

    private static final Date FINISH = new Date(DURATION * 60 * 1000);

    private static final int EVENTID = 21;

    private static final String TITLE = "A title";

    private static final int TRANSPORTID = 432;

    private PVRState state;

    private Event event;

    @Before
    public void setUp() throws Exception {
        EventDescription ed = new EventDescription();
        ed.setDescription(TestPVRStateFactory.LOREM);
        ed.setDuration(this.DURATION);
        ed.setEventId(this.EVENTID);
        ed.setFinish(this.FINISH);
        ed.setRunning(false);
        ed.setServiceId(this.CHANNEL);
        ed.setStart(this.START);
        ed.setTitle(this.TITLE);
        ed.setTransportStreamId(TRANSPORTID);
        this.state = TestPVRStateFactory.getInstance().create();
        this.event = new Event(this.state.getEPG(ServiceType.TV, this.CHANNEL), ed, this.state.getStation(ServiceType.TV, this.CHANNEL).getResource());
    }

    private void setTitle(String title) {
        Resource res = this.event.getResource();
        res = res.getPropertyResourceValue(PO.broadcast_of);
        res = res.getPropertyResourceValue(TMSNet.version_of);
        res = res.getPropertyResourceValue(TMSNet.episode_of);
        res.removeAll(DCTerms.title);
        res.addLiteral(DCTerms.title, title);
    }

    /**
   * Test method for {@link Event#getChannelName()}
   */
    @Test
    public void testGetChannelName() {
        Assert.assertEquals("TV1", this.event.getChannelName());
    }

    /**
   * Test method for {@link Event#getDescription()}
   */
    @Test
    public void testGetDescription() {
        Assert.assertEquals(TestPVRStateFactory.LOREM, this.event.getDescription());
    }

    /**
   * Test method for {@link Event#getDuration()}
   */
    @Test
    public void testGetDuration() {
        Assert.assertEquals(this.DURATION, this.event.getDuration());
    }

    /**
   * Test method for {@link Event#getEventId()}
   */
    @Test
    public void testGetEventId() {
        Assert.assertEquals(this.EVENTID, this.event.getEventId());
    }

    /**
   * Test method for {@link Event#getFinish()}
   */
    @Test
    public void testGetFinish() {
        Assert.assertEquals(this.FINISH, this.event.getFinish());
    }

    /**
   * Test method for {@link Event#getLabel()}
   */
    @Test
    public void testGetLabel() {
        Assert.assertEquals(this.TITLE, this.event.getLabel());
    }

    /**
   * Test method for {@link Event#getStart()}
   */
    @Test
    public void testGetStart() {
        Assert.assertEquals(this.START, this.event.getStart());
    }

    /**
   * Test method for {@link Event#getTitle()}
   */
    @Test
    public void testGetTitle() {
        Assert.assertEquals(this.TITLE, this.event.getTitle());
    }

    /**
   * Test method for {@link org.charvolant.tmsnet.model.Event#getFilename()}.
   */
    @Test
    public void testGetFilename1() {
        this.setTitle("Simple");
        Assert.assertEquals("Simple 1970-01-01.mpg", this.event.getFilename());
    }

    /**
   * Test method for {@link org.charvolant.tmsnet.model.Event#getFilename()}.
   */
    @Test
    public void testGetFilename2() {
        this.setTitle("Simple Space\tThing");
        Assert.assertEquals("Simple Space Thing 1970-01-01.mpg", this.event.getFilename());
    }

    /**
   * Test method for {@link org.charvolant.tmsnet.model.Event#getFilename()}.
   */
    @Test
    public void testGetFilename3() {
        this.setTitle("Punct1234567890!@#$%^&*()_-+=[]{}\\|:;\"'<>,./?");
        Assert.assertEquals("Punct1234567890&_-+=:;,.? 1970-01-01.mpg", this.event.getFilename());
    }

    /**
   * Test method for {@link org.charvolant.tmsnet.model.Event#getFilename()}.
   */
    @Test
    public void testGetFilename4() {
        this.setTitle("A very very long name that should get cut with spaces");
        Assert.assertEquals("A very very long name that 1970-01-01.mpg", this.event.getFilename());
    }

    /**
   * Test method for {@link org.charvolant.tmsnet.model.Event#getFilename()}.
   */
    @Test
    public void testGetFilename5() {
        this.setTitle("A very very long name thatshouldntgetcutwithspaces");
        Assert.assertEquals("A very very long name thatshould 1970-01-01.mpg", this.event.getFilename());
    }

    /**
   * Test method for {@link org.charvolant.tmsnet.model.Event#toDescription()}.
   */
    @Test
    public void testToDescription() {
        EventDescription desc = this.event.toDescription();
        Assert.assertEquals(TestPVRStateFactory.LOREM, desc.getDescription());
        Assert.assertEquals(this.DURATION, desc.getDuration());
        Assert.assertEquals(this.EVENTID, desc.getEventId());
        Assert.assertEquals(this.TRANSPORTID, desc.getTransportStreamId());
        Assert.assertEquals(this.FINISH, desc.getFinish());
        Assert.assertEquals(this.CHANNEL, desc.getServiceId());
        Assert.assertEquals(this.START, desc.getStart());
        Assert.assertEquals(this.TITLE, desc.getTitle());
        Assert.assertEquals(false, desc.isRunning());
    }
}
