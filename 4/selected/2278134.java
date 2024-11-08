package be.lassi.lanbox;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import be.lassi.cues.Cues;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;
import be.lassi.lanbox.domain.ChannelChange;
import be.lassi.util.ContextTestCaseA;
import be.lassi.util.CuesFactory;

/**
 * Tests class <code>CurrentChannelChanges</code>.
 */
public class CurrentChannelChangesTestCase extends ContextTestCaseA {

    private int lastLayerId;

    private ChannelChange lastChange;

    @BeforeMethod
    public void init() {
        Show show = ShowBuilder.example();
        getContext().setShow(show);
        ChannelChangeProcessor ccp = new MyChannelChangeProcessor();
        new CurrentCueChannelChanges(getContext(), ccp);
        CuesFactory.populateCues(getContext());
        lastChange = null;
    }

    @Test
    public void channelChangeCurrentCue() {
        getCues().setCurrent(0);
        getCues().getLightCues().setChannel(0, 0, 0.5f);
        assertEquals(lastLayerId, Lanbox.ENGINE_SHEET);
        assertEquals(lastChange.getChannelId(), 1);
        assertEquals(lastChange.getDmxValue(), 127);
    }

    @Test
    public void channelChangeCueNotCurrent() {
        getCues().setCurrent(0);
        getCues().getLightCues().setChannel(1, 0, 0.5f);
        assertNull(lastChange);
    }

    @Test
    public void showChange() {
        Show show = ShowBuilder.example();
        getContext().setShow(show);
        Cues cues = CuesFactory.populateCues(getContext());
        cues.setCurrent(0);
        cues.getLightCues().setChannel(0, 0, 0.5f);
        assertEquals(lastLayerId, Lanbox.ENGINE_SHEET);
        assertEquals(lastChange.getChannelId(), 1);
        assertEquals(lastChange.getDmxValue(), 127);
    }

    private Cues getCues() {
        return getContext().getShow().getCues();
    }

    private class MyChannelChangeProcessor implements ChannelChangeProcessor {

        public void change(final int layerId, final ChannelChange change) {
            lastLayerId = layerId;
            lastChange = change;
        }
    }
}
