package be.lassi.fixtures;

import static be.lassi.util.Util.newArrayList;
import static org.testng.Assert.assertEquals;
import java.util.List;
import be.lassi.lanbox.ChannelChangeProcessor;
import be.lassi.lanbox.domain.ChannelChange;

public class MockChannelChangeProcessor implements ChannelChangeProcessor {

    private final List<ChannelChange> channelChanges = newArrayList();

    public int getChannelChangeCount() {
        return channelChanges.size();
    }

    public ChannelChange get(final int index) {
        return channelChanges.get(index);
    }

    public void change(final int layerId, final ChannelChange change) {
        channelChanges.add(change);
    }

    public void reset() {
        channelChanges.clear();
    }

    public void assertChannelChange(final int index, final int channelId, final int dmxValue) {
        ChannelChange change = get(index);
        assertEquals(change.getChannelId(), channelId);
        assertEquals(change.getDmxValue(), dmxValue);
    }

    public void assertChannelChangeCount(final int expectedCount) {
        assertEquals(getChannelChangeCount(), expectedCount);
    }
}
