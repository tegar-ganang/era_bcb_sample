package be.lassi.lanbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Tests class <code>ChannelChanges</code>.
 */
public class ChannelChangesTestCase {

    @Test
    public void splitEmpty() {
        ChannelChanges changes = new ChannelChanges();
        ChannelChanges[] cc = changes.split(5);
        assertEquals(0, cc.length);
    }

    @Test
    public void splitNoSplit1() {
        ChannelChanges changes = new ChannelChanges();
        changes.add(1, 0);
        changes.add(2, 0);
        ChannelChanges[] cc = changes.split(3);
        assertTrue(changes == cc[0]);
    }

    @Test
    public void splitNoSplit2() {
        ChannelChanges changes = new ChannelChanges();
        changes.add(1, 0);
        changes.add(2, 0);
        changes.add(3, 0);
        ChannelChanges[] cc = changes.split(3);
        assertTrue(changes == cc[0]);
    }

    @Test
    public void split() {
        ChannelChanges changes = new ChannelChanges();
        changes.add(1, 0);
        changes.add(2, 0);
        changes.add(3, 0);
        changes.add(4, 0);
        ChannelChanges[] cc = changes.split(3);
        assertEquals(3, cc[0].size());
        assertEquals(1, cc[1].size());
        ChannelChange[] cc1 = cc[0].toArray();
        ChannelChange[] cc2 = cc[1].toArray();
        assertEquals(1, cc1[0].getChannelId());
        assertEquals(2, cc1[1].getChannelId());
        assertEquals(3, cc1[2].getChannelId());
        assertEquals(4, cc2[0].getChannelId());
    }

    @Test
    public void eliminateDoubles() {
        ChannelChanges changes = new ChannelChanges();
        changes.add(1, 1);
        changes.add(2, 2);
        changes.add(2, 3);
        changes.add(4, 4);
        changes.eliminateDoubles();
        assertEquals(3, changes.size());
        ChannelChange[] cc = changes.toArray();
        assertEquals(1, cc[0].getDmxValue());
        assertEquals(3, cc[1].getDmxValue());
        assertEquals(4, cc[2].getDmxValue());
    }

    @Test
    public void eliminateDoublesNoDoubles() {
        ChannelChanges changes = new ChannelChanges();
        changes.add(1, 1);
        changes.add(2, 2);
        changes.add(3, 3);
        changes.eliminateDoubles();
        assertEquals(3, changes.size());
        ChannelChange[] cc = changes.toArray();
        assertEquals(1, cc[0].getDmxValue());
        assertEquals(2, cc[1].getDmxValue());
        assertEquals(3, cc[2].getDmxValue());
    }
}
