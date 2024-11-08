package be.lassi.lanbox;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;
import be.lassi.lanbox.domain.ChannelChange;

/**
 * Tests class <code>ChannelChangeQueue</code>.
 *
 */
public class ChannelChangeQueueTestCase {

    @Test
    public void add() {
        ChannelChangeQueue queue = new ChannelChangeQueue();
        assertEquals(queue.size(), 0);
        queue.add(new ChannelChange(1, 0));
        assertEquals(queue.size(), 1);
        queue.add(new ChannelChange(2, 0));
        assertEquals(queue.size(), 2);
    }

    @Test
    public void get() {
        ChannelChangeQueue queue = new ChannelChangeQueue();
        assertEquals(queue.get().size(), 0);
        queue.add(new ChannelChange(1, 0));
        queue.add(new ChannelChange(2, 0));
        ChannelChange[] changes = queue.get().toArray();
        assertEquals(changes.length, 2);
        assertEquals(changes[0].getChannelId(), 1);
        assertEquals(changes[1].getChannelId(), 2);
        assertEquals(queue.size(), 0);
        assertEquals(queue.get().size(), 0);
    }
}
