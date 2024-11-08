package be.lassi.ui.patch;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import be.lassi.context.ShowContext;
import be.lassi.domain.Channel;
import be.lassi.domain.Dimmer;
import be.lassi.domain.Dimmers;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;
import be.lassi.lanbox.Lanbox;
import be.lassi.lanbox.commands.Command;
import be.lassi.lanbox.commands.channel.ChannelReadData;
import be.lassi.lanbox.commands.channel.ChannelSetData;
import be.lassi.lanbox.domain.ChannelChanges;
import be.lassi.preferences.AllPreferences;
import be.lassi.util.Util;

public class PatcherTCL {

    private ShowContext context;

    @BeforeMethod
    public void setUp() {
        AllPreferences preferences = new AllPreferences();
        preferences.getConnectionPreferences().setEnabled(true);
        preferences.getPatch().setFadeTimeString("0s");
        preferences.getPatch().setLayer("A");
        context = new ShowContext(preferences);
        Show show = ShowBuilder.example();
        context.setShow(show);
    }

    @AfterMethod
    public void tearDown() {
        context.close();
    }

    @Test
    public void blackout() {
        Patch patch = new Patch(context);
        final Patcher patcher = new Patcher(context, patch);
        patcher.setUpdateLanbox(true);
        patcher.defaultPatchAndWait();
        patcher.setOn(0, true);
        Util.sleep(600);
        assertDmxOut(255, 1);
        patcher.blackout();
        Util.sleep(600);
        assertDmxOut(0, 1);
    }

    @Test
    public void setOn() {
        Patch patch = new Patch(context);
        final Patcher patcher = new Patcher(context, patch);
        patcher.setUpdateLanbox(true);
        patcher.defaultPatchAndWait();
        patcher.setOn(0, true);
        Util.sleep(600);
        assertDmxOut(255, 1);
        patcher.setOn(0, false);
        Util.sleep(600);
        assertDmxOut(0, 1);
    }

    @Test
    public void patchActions() {
        doTestPatchActions(32);
        doTestPatchActions(255);
        doTestPatchActions(256);
        doTestPatchActions(257);
        doTestPatchActions(512);
    }

    private void doTestPatchActions(final int dimmerCount) {
        Show show = ShowBuilder.build(dimmerCount, 0, dimmerCount, "");
        context.setShow(show);
        Patch patch = new Patch(context);
        final Patcher patcher = new Patcher(context, patch);
        patcher.setUpdateLanbox(true);
        Dimmer first = context.getShow().getDimmers().get(0);
        Dimmer last = context.getShow().getDimmers().get(dimmerCount - 1);
        patcher.defaultPatchAndWait();
        patcher.loadPatchAndWait(true);
        assertTrue(first.isPatched());
        assertEquals(first.getChannelId(), 0);
        assertEquals(first.getLanboxChannelId(), 0);
        assertTrue(last.isPatched());
        assertEquals(last.getChannelId(), dimmerCount - 1);
        assertEquals(last.getLanboxChannelId(), dimmerCount - 1);
        patcher.clearPatchAndWait();
        patcher.loadPatchAndWait(true);
        assertFalse(first.isPatched());
        assertFalse(last.isPatched());
        if (dimmerCount <= Patcher.PRE_PATCH_START) {
            assertEquals(first.getLanboxChannelId(), Patcher.PRE_PATCH_START);
            assertEquals(last.getLanboxChannelId(), Patcher.PRE_PATCH_START + dimmerCount - 1);
        } else {
            assertEquals(first.getLanboxChannelId(), -1);
            assertEquals(last.getLanboxChannelId(), -1);
        }
    }

    @Test
    public void patchSingleChannelToMultipleDimmers() {
        Patch patch = new Patch(context);
        final Patcher patcher = new Patcher(context, patch);
        patcher.setUpdateLanbox(true);
        patcher.defaultPatchAndWait();
        Channel channel = context.getShow().getChannels().get(5);
        List<Channel> channels = new ArrayList<Channel>();
        channels.add(channel);
        int[] patchDetailIndexes = { 1, 3 };
        patcher.patchAndWait(patchDetailIndexes, channels);
        Dimmers dimmers = context.getShow().getDimmers();
        assertEquals(dimmers.get(0).getChannelId(), 0);
        assertEquals(dimmers.get(1).getChannelId(), 5);
        assertEquals(dimmers.get(2).getChannelId(), 2);
        assertEquals(dimmers.get(3).getChannelId(), 5);
        assertEquals(dimmers.get(4).getChannelId(), 4);
        assertEquals(dimmers.get(0).getLanboxChannelId(), 0);
        assertEquals(dimmers.get(1).getLanboxChannelId(), 5);
        assertEquals(dimmers.get(2).getLanboxChannelId(), 2);
        assertEquals(dimmers.get(3).getLanboxChannelId(), 5);
        assertEquals(dimmers.get(4).getLanboxChannelId(), 4);
        patcher.loadPatchAndWait(true);
        assertEquals(dimmers.get(0).getChannelId(), 0);
        assertEquals(dimmers.get(1).getChannelId(), 5);
        assertEquals(dimmers.get(2).getChannelId(), 2);
        assertEquals(dimmers.get(3).getChannelId(), 5);
        assertEquals(dimmers.get(4).getChannelId(), 4);
        assertEquals(dimmers.get(0).getLanboxChannelId(), 0);
        assertEquals(dimmers.get(1).getLanboxChannelId(), 5);
        assertEquals(dimmers.get(2).getLanboxChannelId(), 2);
        assertEquals(dimmers.get(3).getLanboxChannelId(), 5);
        assertEquals(dimmers.get(4).getLanboxChannelId(), 4);
    }

    @Test
    public void patchMultipleChannels() {
        Patch patch = new Patch(context);
        final Patcher patcher = new Patcher(context, patch);
        patcher.setUpdateLanbox(true);
        patcher.defaultPatchAndWait();
        List<Channel> channels = new ArrayList<Channel>();
        channels.add(context.getShow().getChannels().get(10));
        channels.add(context.getShow().getChannels().get(20));
        int[] patchDetailIndexes = { 1, 3, 5 };
        patcher.patchAndWait(patchDetailIndexes, channels);
        Dimmers dimmers = context.getShow().getDimmers();
        assertEquals(dimmers.get(0).getChannelId(), 0);
        assertEquals(dimmers.get(1).getChannelId(), 10);
        assertEquals(dimmers.get(2).getChannelId(), 20);
        assertEquals(dimmers.get(3).getChannelId(), 3);
        assertEquals(dimmers.get(4).getChannelId(), 4);
        assertEquals(dimmers.get(5).getChannelId(), 5);
        assertEquals(dimmers.get(0).getLanboxChannelId(), 0);
        assertEquals(dimmers.get(1).getLanboxChannelId(), 10);
        assertEquals(dimmers.get(2).getLanboxChannelId(), 20);
        assertEquals(dimmers.get(3).getLanboxChannelId(), 3);
        assertEquals(dimmers.get(4).getLanboxChannelId(), 4);
        assertEquals(dimmers.get(5).getLanboxChannelId(), 5);
        patcher.loadPatchAndWait(true);
        assertEquals(dimmers.get(0).getChannelId(), 0);
        assertEquals(dimmers.get(1).getChannelId(), 10);
        assertEquals(dimmers.get(2).getChannelId(), 20);
        assertEquals(dimmers.get(3).getChannelId(), 3);
        assertEquals(dimmers.get(4).getChannelId(), 4);
        assertEquals(dimmers.get(5).getChannelId(), 5);
        assertEquals(dimmers.get(0).getLanboxChannelId(), 0);
        assertEquals(dimmers.get(1).getLanboxChannelId(), 10);
        assertEquals(dimmers.get(2).getLanboxChannelId(), 20);
        assertEquals(dimmers.get(3).getLanboxChannelId(), 3);
        assertEquals(dimmers.get(4).getLanboxChannelId(), 4);
        assertEquals(dimmers.get(5).getLanboxChannelId(), 5);
    }

    @Test
    public void savePatch() {
        Patch patch = new Patch(context);
        final Patcher patcher = new Patcher(context, patch);
        patcher.setUpdateLanbox(true);
        patcher.defaultPatchAndWait();
        patcher.setUpdateLanbox(false);
        List<Channel> channels = new ArrayList<Channel>();
        channels.add(context.getShow().getChannels().get(10));
        int[] patchDetailIndexes = { 5 };
        patcher.patchAndWait(patchDetailIndexes, channels);
        Dimmer dimmer = context.getShow().getDimmers().get(5);
        assertEquals(dimmer.getChannelId(), 10);
        assertEquals(dimmer.getLanboxChannelId(), 5);
        patcher.loadPatchAndWait(true);
        assertEquals(dimmer.getChannelId(), 5);
        assertEquals(dimmer.getLanboxChannelId(), 5);
        patcher.patchAndWait(patchDetailIndexes, channels);
        patcher.savePatchAndWait();
        assertEquals(dimmer.getChannelId(), 10);
        assertEquals(dimmer.getLanboxChannelId(), 10);
        patcher.loadPatchAndWait(true);
        assertEquals(dimmer.getChannelId(), 10);
        assertEquals(dimmer.getLanboxChannelId(), 10);
    }

    @Test
    public void unPatch() {
        Patch patch = new Patch(context);
        final Patcher patcher = new Patcher(context, patch);
        patcher.setUpdateLanbox(true);
        patcher.defaultPatchAndWait();
        int[] patchDetailIndexes = { 1, 3 };
        patcher.unpatchAndWait(patchDetailIndexes);
        Dimmers dimmers = context.getShow().getDimmers();
        assertEquals(dimmers.get(0).getChannelId(), 0);
        assertEquals(dimmers.get(1).getChannelId(), -1);
        assertEquals(dimmers.get(2).getChannelId(), 2);
        assertEquals(dimmers.get(3).getChannelId(), -1);
        assertEquals(dimmers.get(4).getChannelId(), 4);
        assertEquals(dimmers.get(0).getLanboxChannelId(), 0);
        assertEquals(dimmers.get(1).getLanboxChannelId(), 257);
        assertEquals(dimmers.get(2).getLanboxChannelId(), 2);
        assertEquals(dimmers.get(3).getLanboxChannelId(), 259);
        assertEquals(dimmers.get(4).getLanboxChannelId(), 4);
        patcher.loadPatchAndWait(true);
        assertEquals(dimmers.get(0).getChannelId(), 0);
        assertEquals(dimmers.get(1).getChannelId(), -1);
        assertEquals(dimmers.get(2).getChannelId(), 2);
        assertEquals(dimmers.get(3).getChannelId(), -1);
        assertEquals(dimmers.get(4).getChannelId(), 4);
        assertEquals(dimmers.get(0).getLanboxChannelId(), 0);
        assertEquals(dimmers.get(1).getLanboxChannelId(), 257);
        assertEquals(dimmers.get(2).getLanboxChannelId(), 2);
        assertEquals(dimmers.get(3).getLanboxChannelId(), 259);
        assertEquals(dimmers.get(4).getLanboxChannelId(), 4);
    }

    @Test
    public void setLayerNumber() {
        Patch patch = new Patch(context);
        final Patcher patcher = new Patcher(context, patch);
        patcher.setUpdateLanbox(true);
        patcher.defaultPatchAndWait();
        ChannelChanges changes = new ChannelChanges();
        changes.add(1, 0);
        changes.add(2, 0);
        changes.add(3, 0);
        context.execute(new ChannelSetData(1, changes));
        context.execute(new ChannelSetData(2, changes));
        patcher.setOn(0, true);
        patcher.setOn(1, false);
        Util.sleep(1000);
        assertLevel(255, 1, 1);
        assertLevel(0, 1, 2);
        assertLevel(0, 2, 1);
        assertLevel(0, 2, 2);
        patcher.setLayerNumber(2);
        Util.sleep(600);
        assertLevel(0, 1, 1);
        assertLevel(0, 1, 2);
        assertLevel(255, 2, 1);
        assertLevel(0, 2, 2);
        patcher.setOn(0, false);
        assertLevel(0, 1, 1);
        assertLevel(0, 1, 2);
        assertLevel(0, 2, 1);
        assertLevel(0, 2, 2);
    }

    @Test
    public void testShowWithoutChannels() {
        Show show = ShowBuilder.build();
        context.setShow(show);
        Patch patch = new Patch(context);
        final Patcher patcher = new Patcher(context, patch);
        patcher.setUpdateLanbox(true);
        patcher.defaultPatchAndWait();
    }

    private void assertDmxOut(final int expectedLevel, final int dimmerIndex) {
        assertLevel(expectedLevel, Lanbox.DMX_OUT, dimmerIndex);
    }

    private void assertLevel(final int expectedLevel, final int layerNumber, final int channelIndex) {
        final int[] dmxValues = new int[1];
        Command command = new ChannelReadData(dmxValues, layerNumber, channelIndex, dmxValues.length);
        context.getLanbox().getEngine().executeAndWait(command);
        assertEquals(dmxValues[0], expectedLevel);
    }
}
