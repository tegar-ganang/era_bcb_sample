package be.lassi.lanbox.domain;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;
import be.lassi.support.ObjectBuilder;
import be.lassi.support.ObjectTest;

/**
 * Tests class <code>ChannelChange</code>.
 */
public class ChannelChangeTestCase {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void validate1() {
        new ChannelChange(0, 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void validate2() {
        new ChannelChange(513, 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void validate3() {
        new ChannelChange(0, -1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void validate4() {
        new ChannelChange(0, 256);
    }

    @Test
    public void constructor() {
        ChannelChange cc = new ChannelChange(10, 100);
        assertEquals(cc.getChannelId(), 10);
        assertEquals(cc.getDmxValue(), 100);
    }

    @Test
    public void testToString() {
        ChannelChange cc = new ChannelChange(10, 100);
        assertTrue(cc.toString().length() > 0);
    }

    @Test
    public void object() {
        ObjectBuilder b = new ObjectBuilder() {

            public Object getObject1() {
                return new ChannelChange(10, 50);
            }

            public Object getObject2() {
                return new ChannelChange(10, 60);
            }
        };
        ObjectTest.test(b);
    }
}
