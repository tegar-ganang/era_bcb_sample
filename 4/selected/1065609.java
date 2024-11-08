package org.charvolant.tmsnet.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.charvolant.tmsnet.resources.ResourceLocator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * TODO Class documentation
 *
 * @author Doug Palmer &lt;doug@charvolant.org&gt;
 *
 */
public class ChannelTest {

    private static final int CHANNEL = 5;

    /** The state */
    private PVRState state;

    /** The epg */
    private Channel epg;

    /** The start date */
    private Date now;

    /** The order for sorting */
    private EventDescription.DateComparator dateOrder = new EventDescription.DateComparator();

    /** The event id */
    private int eventId;

    /**
   * @throws java.lang.Exception
   */
    @Before
    public void setUp() throws Exception {
        TestPVRStateFactory factory = TestPVRStateFactory.getInstance();
        Station station;
        this.now = new Date();
        this.state = new PVRState();
        this.state.setLocator(new ResourceLocator());
        factory.buildChannels(this.state, this.now);
        station = this.state.getStation(ServiceType.TV, this.CHANNEL);
        this.epg = new Channel(this.state, station.getChannel(), this.CHANNEL);
        this.eventId = 5000;
    }

    /**
   * @throws java.lang.Exception
   */
    @After
    public void tearDown() throws Exception {
    }

    /**
   * Generate a list of events
   * 
   * @param channel The channel index
   * @param n The number of events
   * @param offsetStart where to start from (now) in hours
   * @param offsetEnd where to end from (now) in hours
   * 
   * @return The list of events
   */
    protected List<EventDescription> generateEvents(int channel, int n, int offsetStart, int offsetEnd) {
        ArrayList<EventDescription> evts = new ArrayList<EventDescription>(n);
        Calendar cal = Calendar.getInstance();
        int mins = (offsetEnd - offsetStart) * 60 / n;
        EventDescription evt;
        cal.setTime(this.now);
        cal.add(Calendar.HOUR, offsetStart);
        for (int i = 0; i < n; i++) {
            evt = new EventDescription();
            evt.setDescription("Event " + i + " on " + channel);
            evt.setDuration(mins);
            evt.setStart(cal.getTime());
            cal.add(Calendar.MINUTE, mins);
            evt.setFinish(cal.getTime());
            evt.setEventId(this.eventId++);
            evt.setRunning(!this.now.before(evt.getStart()) && evt.getFinish().after(this.now));
            evt.setServiceId(channel);
            evt.setTitle("Event " + channel + "/" + i);
            evt.setTransportStreamId(channel);
            evts.add(evt);
        }
        return evts;
    }

    /**
   * Test method for {@link org.charvolant.tmsnet.model.PVRState#updateEvents(int, java.util.List)}.
   */
    @Test
    public void testUpdateEvents1() {
        List<EventDescription> list = this.generateEvents(5, 2, -1, 1);
        this.epg.updateEvents(list);
        Assert.assertNotNull(this.epg.getEvents());
        Assert.assertEquals(2, this.epg.getEvents().size());
    }

    /**
   * Test method for {@link org.charvolant.tmsnet.model.PVRState#updateEvents(int, java.util.List)}.
   */
    @Test
    public void testUpdateEvents2() {
        List<EventDescription> list = this.generateEvents(5, 2, -1, 1);
        this.epg.updateEvents(list);
        list = this.generateEvents(5, 2, 1, 2);
        this.epg.updateEvents(list);
        Assert.assertNotNull(this.epg.getEvents());
        Assert.assertEquals(4, this.epg.getEvents().size());
    }

    /**
   * Test method for {@link org.charvolant.tmsnet.model.PVRState#updateEvents(int, java.util.List)}.
   */
    @Test
    public void testUpdateEvents3() {
        List<EventDescription> list = this.generateEvents(5, 2, -1, 1);
        this.epg.updateEvents(list);
        list = this.generateEvents(5, 2, -2, -1);
        this.epg.updateEvents(list);
        Assert.assertNotNull(this.epg.getEvents());
        Assert.assertEquals(4, this.epg.getEvents().size());
    }

    /**
   * Test method for {@link org.charvolant.tmsnet.model.PVRState#updateEvents(int, java.util.List)}.
   */
    @Test
    public void testUpdateEvents4() {
        List<EventDescription> list = this.generateEvents(5, 2, -1, 1);
        List<EventDescription> expected = new ArrayList<EventDescription>(3);
        List<Event> actual;
        expected.add(list.get(1));
        this.epg.updateEvents(list);
        list = this.generateEvents(5, 2, -1, 0);
        this.epg.updateEvents(list);
        expected.addAll(list);
        Collections.sort(expected, this.dateOrder);
        actual = this.epg.getEvents();
        Assert.assertNotNull(actual);
        Assert.assertEquals(3, actual.size());
        for (int i = 0; i < actual.size(); i++) Assert.assertEquals(expected.get(i).getEventId(), actual.get(i).getEventId());
    }

    /**
   * Test method for {@link org.charvolant.tmsnet.model.PVRState#updateEvents(int, java.util.List)}.
   */
    @Test
    public void testUpdateEvents5() {
        List<EventDescription> list = this.generateEvents(5, 2, -1, 1);
        List<EventDescription> expected = new ArrayList<EventDescription>(3);
        List<Event> actual;
        expected.add(list.get(0));
        this.epg.updateEvents(list);
        list = this.generateEvents(5, 2, 0, 1);
        this.epg.updateEvents(list);
        expected.addAll(list);
        Collections.sort(expected, this.dateOrder);
        actual = this.epg.getEvents();
        Assert.assertNotNull(actual);
        Assert.assertEquals(3, actual.size());
        for (int i = 0; i < actual.size(); i++) Assert.assertEquals(expected.get(i).getEventId(), actual.get(i).getEventId());
    }

    /**
   * Test method for {@link org.charvolant.tmsnet.model.PVRState#updateEvents(int, java.util.List)}.
   */
    @Test
    public void testUpdateEvents6() {
        List<EventDescription> list = this.generateEvents(5, 4, -2, 2);
        List<EventDescription> expected = new ArrayList<EventDescription>(3);
        List<Event> actual;
        expected.add(list.get(0));
        expected.add(list.get(1));
        expected.add(list.get(3));
        this.epg.updateEvents(list);
        list = this.generateEvents(5, 2, 0, 1);
        this.epg.updateEvents(list);
        expected.addAll(list);
        Collections.sort(expected, this.dateOrder);
        actual = this.epg.getEvents();
        Assert.assertNotNull(actual);
        Assert.assertEquals(5, actual.size());
        for (int i = 0; i < actual.size(); i++) Assert.assertEquals(expected.get(i).getEventId(), actual.get(i).getEventId());
    }
}
