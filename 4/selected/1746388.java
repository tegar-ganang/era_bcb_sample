package be.lassi.domain;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;
import be.lassi.support.DirtyTest;
import be.lassi.support.ObjectBuilder;
import be.lassi.support.ObjectTest;

/**
 * Tests class <code>Dimmer</code>.
 */
public class DimmerTestCase {

    @Test
    public void constructor() {
        Dimmer dimmer = new Dimmer(12, "Test");
        assertEquals(dimmer.getName(), "Test");
        assertEquals(dimmer.getId(), 12);
        assertEquals(dimmer.getIntValue(), 0);
    }

    @Test
    public void dirty() {
        DirtyTest test = new DirtyTest();
        Dimmer dimmer = new Dimmer(test.getDirty(), 0, "Name");
        test.notDirty();
        dimmer.setName("New name");
        test.dirty();
        Channel channel = new Channel(test.getDirty(), 0, "");
        dimmer.setChannel(channel);
        test.dirty();
        dimmer.setChannel(null);
        test.dirty();
        dimmer.setLanboxChannelId(10);
        test.notDirty();
    }

    @Test
    public void equals() {
        Dimmer dimmer1;
        Dimmer dimmer2;
        Dimmer dimmer3;
        dimmer1 = new Dimmer(1, "Test");
        dimmer2 = new Dimmer(1, "Test");
        dimmer3 = new Dimmer(3, "Test");
        assertTrue(dimmer1.equals(dimmer2));
        assertFalse(dimmer1.equals(dimmer3));
        dimmer1.setChannel(new Channel(0, "Channel 1"));
        assertFalse(dimmer1.equals(dimmer2));
        dimmer2.setChannel(new Channel(0, "Channel 1"));
        assertTrue(dimmer1.equals(dimmer2));
    }

    @Test
    public void object() {
        ObjectBuilder b = new ObjectBuilder() {

            public Object getObject1() {
                return new Dimmer(0, "One");
            }

            public Object getObject2() {
                return new Dimmer(1, "Two");
            }
        };
        ObjectTest.test(b);
    }

    @Test
    public void isPatched() {
        Dimmer dimmer = new Dimmer(0, "Dimmer 1");
        Channel channel1 = new Channel(0, "Channel 1");
        Channel channel2 = new Channel(1, "Channel 2");
        assertFalse(dimmer.isPatched());
        dimmer.setChannel(channel1);
        assertTrue(dimmer.isPatched());
        dimmer.setChannel(channel2);
        assertTrue(dimmer.isPatched());
        dimmer.setChannel(null);
        assertFalse(dimmer.isPatched());
    }

    @Test
    public void getChannelId() {
        Dimmer dimmer = new Dimmer(0, "Dimmer 1");
        Channel channel = new Channel(4, "Channel 5");
        assertEquals(dimmer.getChannelId(), -1);
        dimmer.setChannel(channel);
        assertEquals(dimmer.getChannelId(), 4);
        dimmer.setChannel(null);
        assertEquals(dimmer.getChannelId(), -1);
    }
}
