package be.lassi.ui.patch;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import be.lassi.context.ShowContext;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;

public class PatchTestCase {

    private ShowContext context;

    private Patch patch;

    @BeforeMethod
    public void newPatch() {
        context = new ShowContext();
        Show show = ShowBuilder.build(24, 12, 32, "");
        context.setShow(show);
        patch = new Patch(context);
        patch.defaultPatch();
    }

    @Test
    public void showContextListener() {
        patch.clearPatch();
        assertEquals(patch.getDimmerCount(), 32);
        assertEquals(patch.getDetails().size(), 32);
        PatchDetail detail = patch.getDetail(0);
        assertEquals(detail.getDimmer().getId(), 0);
        assertEquals(detail.getDimmer().getChannel(), null);
        Show show = ShowBuilder.build(512, 0, 512, "TestShow");
        context.setShow(show);
        assertEquals(patch.getDimmerCount(), 512);
        detail = patch.getDetail(511);
        assertEquals(detail.getDimmer().getId(), 511);
        assertEquals(detail.getDimmer().getChannel(), null);
    }

    @Test
    public void defaultPatch() {
        assertEquals(patch.getDetail(0).getDimmer().getChannel().getId(), 0);
        assertEquals(patch.getDetail(23).getDimmer().getChannel().getId(), 23);
        assertNull(patch.getDetail(24).getDimmer().getChannel());
        assertNull(patch.getDetail(31).getDimmer().getChannel());
        assertTrue(context.getShow().getChannels().get(0).isPatched());
        assertTrue(context.getShow().getChannels().get(23).isPatched());
    }

    @Test
    public void clearPatch() {
        patch.clearPatch();
        assertNull(patch.getDetail(0).getDimmer().getChannel());
        assertNull(patch.getDetail(23).getDimmer().getChannel());
        assertNull(patch.getDetail(24).getDimmer().getChannel());
        assertNull(patch.getDetail(31).getDimmer().getChannel());
    }

    @Test
    public void copyChannelNamesToDimmers() {
        patch.copyChannelNamesToDimmers();
        assertEquals(context.getShow().getDimmers().get(0).getName(), "Channel 1");
        assertEquals(context.getShow().getDimmers().get(24).getName(), "Dimmer 25");
    }

    @Test
    public void copyDimmerNamesToChannels() {
        patch.copyDimmerNamesToChannels();
        assertEquals(context.getShow().getChannels().get(0).getName(), "Dimmer 1");
    }

    @Test
    public void sortOn() {
        patch.getDetail(1).setOn(true);
        patch.getDetail(3).setOn(true);
        patch.getDetail(4).setOn(true);
        patch.setSortColumn(PatchDetail.ON);
        patch.setSortColumn(PatchDetail.ON);
        assertEquals(patch.getSortColumn(), PatchDetail.ON);
        assertEquals(patch.getDetail(0).getDimmer().getId(), 1);
        assertEquals(patch.getDetail(1).getDimmer().getId(), 3);
        assertEquals(patch.getDetail(2).getDimmer().getId(), 4);
        assertEquals(patch.getDetail(3).getDimmer().getId(), 0);
        assertEquals(patch.getDetail(4).getDimmer().getId(), 2);
    }

    @Test
    public void sortDimmerNumber() {
        patch.setSortColumn(PatchDetail.DIMMER_NUMBER);
        assertEquals(patch.getDetail(0).getDimmer().getId(), 0);
        assertEquals(patch.getDetail(1).getDimmer().getId(), 1);
        assertEquals(patch.getDetail(2).getDimmer().getId(), 2);
        assertEquals(patch.getDetail(3).getDimmer().getId(), 3);
    }

    @Test
    public void sortDimmerName() {
        patch.getDetail(10).getDimmer().setName("B");
        patch.getDetail(11).getDimmer().setName("B");
        patch.getDetail(12).getDimmer().setName("A");
        patch.setSortColumn(PatchDetail.DIMMER_NAME);
        assertEquals(patch.getDetail(0).getDimmer().getId(), 12);
        assertEquals(patch.getDetail(1).getDimmer().getId(), 10);
        assertEquals(patch.getDetail(2).getDimmer().getId(), 11);
        assertEquals(patch.getDetail(3).getDimmer().getId(), 0);
    }

    @Test
    public void sortLanboxChannelNumber() {
        patch.getDetail(0).getDimmer().setChannel(null);
        patch.getDetail(10).getDimmer().setChannel(null);
        patch.setSortColumn(PatchDetail.LANBOX_CHANNEL_NUMBER);
        assertEquals(patch.getDetail(0).getDimmer().getId(), 1);
        assertEquals(patch.getDetail(22).getDimmer().getId(), 0);
        assertEquals(patch.getDetail(23).getDimmer().getId(), 10);
        assertEquals(patch.getDetail(24).getDimmer().getId(), 24);
    }

    @Test
    public void sortChannelNumber() {
        patch.getDetail(0).getDimmer().setChannel(null);
        patch.getDetail(10).getDimmer().setChannel(null);
        patch.setSortColumn(PatchDetail.CHANNEL_NUMBER);
        assertEquals(patch.getDetail(0).getDimmer().getId(), 1);
        assertEquals(patch.getDetail(22).getDimmer().getId(), 0);
        assertEquals(patch.getDetail(23).getDimmer().getId(), 10);
        assertEquals(patch.getDetail(24).getDimmer().getId(), 24);
    }

    @Test
    public void sortChannelName() {
        patch.getDetail(0).getDimmer().setChannel(null);
        patch.getDetail(10).getDimmer().getChannel().setName("A");
        patch.getDetail(11).getDimmer().getChannel().setName("A");
        patch.setSortColumn(PatchDetail.CHANNEL_NAME);
        assertEquals(patch.getDetail(0).getDimmer().getId(), 10);
        assertEquals(patch.getDetail(1).getDimmer().getId(), 11);
        assertEquals(patch.getDetail(23).getDimmer().getId(), 0);
        assertEquals(patch.getDetail(24).getDimmer().getId(), 24);
    }

    @Test
    public void isDimmerOn() {
        assertFalse(patch.isDimmerOn());
        patch.getDetail(0).setOn(true);
        assertTrue(patch.isDimmerOn());
    }
}
