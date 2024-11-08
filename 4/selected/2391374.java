package be.lassi.ui.patch;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import java.awt.AWTException;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.io.IOException;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JTableHeaderOperator;
import org.netbeans.jemmy.operators.JTableOperator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import be.lassi.domain.Channel;
import be.lassi.domain.Channels;
import be.lassi.domain.Dimmer;
import be.lassi.lanbox.domain.Time;
import be.lassi.support.FrameTester;
import be.lassi.util.Util;

/**
 * Tests class <code>PatchFrame</code>.
 */
public class PatchFrameTestCase {

    private static FrameTester tester;

    @BeforeMethod
    public static void open() {
        tester = new FrameTester(PatchFrame.class);
    }

    @AfterMethod
    public static void close() {
        tester.close();
        tester = null;
    }

    @Test
    public void defaultPatchAndClearPatch() {
        Dimmer dimmer = tester.getContext().getShow().getDimmers().get(0);
        defaultPatch();
        assertEquals(dimmer.getChannelId(), 0);
        clearPatch();
        assertEquals(dimmer.getChannelId(), -1);
        defaultPatch();
        assertEquals(dimmer.getChannelId(), 0);
    }

    @Test
    public void patch() {
        tester.getTable("channelTable").selectCell(1, 0);
        tester.getTable("detailTable").selectCell(0, 2);
        tester.pushButtonNamed("patch.action.patch");
        Dimmer dimmer = tester.getContext().getShow().getDimmers().get(0);
        assertEquals(dimmer.getChannelId(), 1);
    }

    @Test
    public void unPatch() {
        defaultPatch();
        JTableOperator detailTable = tester.getTable("detailTable");
        detailTable.clickOnCell(0, PatchDetail.CHANNEL_NAME);
        Dimmer dimmer = tester.getContext().getShow().getDimmers().get(0);
        assertNotNull(dimmer.getChannel());
        tester.pushButtonNamed("patch.action.unpatch");
        assertNull(dimmer.getChannel());
    }

    @Test
    public void saveToLanboxAndLoadFromLanbox() {
        Dimmer dimmer = tester.getContext().getShow().getDimmers().get(0);
        defaultPatch();
        assertEquals(dimmer.getChannelId(), 0);
        tester.pushMenu("File|Save to Lanbox");
        Util.sleep(500);
        assertEquals(dimmer.getChannelId(), 0);
        clearPatch();
        assertEquals(dimmer.getChannelId(), -1);
        tester.pushMenu("File|Load from Lanbox");
        Util.sleep(500);
        assertEquals(dimmer.getChannelId(), 0);
    }

    @Test
    public void setLayer() {
        tester.pushMenu("Options|Layer|A");
        Util.sleep(100);
        PatchPreferences preferences = tester.getContext().getPreferences().getPatch();
        String layerName = preferences.getLayer();
        assertEquals(layerName, "A");
        tester.pushMenu("Options|Layer|B");
        Util.sleep(100);
        layerName = preferences.getLayer();
        assertEquals(layerName, "B");
    }

    @Test
    public void setFadeTime() {
        tester.pushMenu("Options|Fade Time|2.0s");
        Util.sleep(100);
        PatchPreferences preferences = tester.getContext().getPreferences().getPatch();
        Time fadeTime = preferences.getFadeTime();
        assertEquals(fadeTime, Time.TIME_2S);
        tester.pushMenu("Options|Fade Time|1.0s");
        Util.sleep(100);
        fadeTime = preferences.getFadeTime();
        assertEquals(fadeTime, Time.TIME_1S);
    }

    @Test
    public void copyChannelNamesToDimmerNames() {
        defaultPatch();
        tester.pushMenu("Actions|Copy channel names to dimmer names");
        Util.sleep(100);
        Dimmer dimmer = tester.getContext().getShow().getDimmers().get(0);
        assertEquals(dimmer.getName(), "Channel 1");
    }

    @Test
    public void copyDimmerNamesToChannelNames() {
        Dimmer dimmer = tester.getContext().getShow().getDimmers().get(0);
        Channel channel = tester.getContext().getShow().getChannels().get(0);
        defaultPatch();
        dimmer.setName("Dimmer 1");
        tester.pushMenu("Actions|Copy dimmer names to channel names");
        Util.sleep(500);
        assertEquals(channel.getName(), "Dimmer 1");
    }

    @Test
    public void copyPatchToClipboard() throws UnsupportedFlavorException, IOException {
        tester.pushMenu("Actions|Copy patch to clipboard");
        Util.sleep(100);
        Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        String string = (String) t.getTransferData(DataFlavor.stringFlavor);
        assertTrue(string.startsWith("1\tDimmer 1\t"));
    }

    @Test
    public void stageFollowsSelection() {
        tester.pushMenu("Options|Stage follows selection");
        assertFalse(isOn(0));
        assertFalse(isOn(1));
        tester.getTable("detailTable").selectCell(0, 1);
        assertTrue(isOn(0));
        assertFalse(isOn(1));
        tester.getTable("detailTable").selectCell(1, 1);
        assertFalse(isOn(0));
        assertTrue(isOn(1));
        tester.getTable("detailTable").clearSelection();
        assertFalse(isOn(0));
        assertFalse(isOn(1));
        tester.pushMenu("Options|Stage follows selection");
    }

    @Test
    public void updateLanboxImmediately() {
        Dimmer dimmer = tester.getContext().getShow().getDimmers().get(0);
        tester.pushMenu("Options|Update Lanbox patch immediately");
        tester.getTable("channelTable").selectCell(5, 0);
        tester.getTable("detailTable").selectCell(0, 2);
        tester.pushButtonNamed("patch.action.patch");
        tester.pushMenu("File|Load from Lanbox");
        Util.sleep(100);
        assertEquals(dimmer.getLanboxChannelId(), 5);
        tester.pushMenu("Options|Update Lanbox patch immediately");
    }

    @Test
    public void sortChannelTableOnChannelNr() {
        JTableOperator table = tester.getTable("channelTable");
        JTableHeaderOperator header = table.getHeaderOperator();
        header.selectColumn(0);
        PatchChannelTableModel model = (PatchChannelTableModel) table.getModel();
        assertEquals(model.getSortColumn(), 0);
    }

    @Test
    public void sortChannelTableOnChannelName() {
        JTableOperator table = tester.getTable("channelTable");
        JTableHeaderOperator header = table.getHeaderOperator();
        header.selectColumn(1);
        PatchChannelTableModel model = (PatchChannelTableModel) table.getModel();
        assertEquals(model.getSortColumn(), 1);
    }

    @Test
    public void sortChannelTableOnPatched() {
        JTableOperator table = tester.getTable("channelTable");
        JTableHeaderOperator header = table.getHeaderOperator();
        header.selectColumn(2);
        PatchChannelTableModel model = (PatchChannelTableModel) table.getModel();
        assertEquals(model.getSortColumn(), 2);
    }

    @Test
    public void sortDetailTableOnOn() {
        JTableOperator table = tester.getTable("detailTable");
        JTableHeaderOperator header = table.getHeaderOperator();
        header.selectColumn(PatchDetail.ON);
        PatchDetailTableModel model = (PatchDetailTableModel) table.getModel();
        assertEquals(model.getSortColumn(), PatchDetail.ON);
    }

    @Test
    public void sortDetailTableOnDimmerNumber() {
        JTableOperator table = tester.getTable("detailTable");
        JTableHeaderOperator header = table.getHeaderOperator();
        header.selectColumn(PatchDetail.DIMMER_NUMBER);
        PatchDetailTableModel model = (PatchDetailTableModel) table.getModel();
        assertEquals(model.getSortColumn(), PatchDetail.DIMMER_NUMBER);
    }

    @Test
    public void sortDetailTableOnDimmerName() {
        JTableOperator table = tester.getTable("detailTable");
        JTableHeaderOperator header = table.getHeaderOperator();
        header.selectColumn(PatchDetail.DIMMER_NAME);
        PatchDetailTableModel model = (PatchDetailTableModel) table.getModel();
        assertEquals(model.getSortColumn(), PatchDetail.DIMMER_NAME);
    }

    @Test
    public void sortDetailTableOnLanboxChannelNumber() {
        JTableOperator table = tester.getTable("detailTable");
        JTableHeaderOperator header = table.getHeaderOperator();
        header.selectColumn(PatchDetail.LANBOX_CHANNEL_NUMBER);
        PatchDetailTableModel model = (PatchDetailTableModel) table.getModel();
        assertEquals(model.getSortColumn(), PatchDetail.LANBOX_CHANNEL_NUMBER);
    }

    @Test
    public void sortDetailTableOnChannelNumber() {
        JTableOperator table = tester.getTable("detailTable");
        JTableHeaderOperator header = table.getHeaderOperator();
        header.selectColumn(PatchDetail.CHANNEL_NUMBER);
        PatchDetailTableModel model = (PatchDetailTableModel) table.getModel();
        assertEquals(model.getSortColumn(), PatchDetail.CHANNEL_NUMBER);
    }

    @Test
    public void sortDetailTableOnChannelName() {
        JTableOperator table = tester.getTable("detailTable");
        JTableHeaderOperator header = table.getHeaderOperator();
        header.selectColumn(PatchDetail.CHANNEL_NAME);
        PatchDetailTableModel model = (PatchDetailTableModel) table.getModel();
        assertEquals(model.getSortColumn(), PatchDetail.CHANNEL_NAME);
    }

    @Test
    public void changeChannelName() {
        sortChannelTableOnChannelNr();
        JTableOperator table = tester.getTable("channelTable");
        table.clickForEdit(0, 1);
        table.changeCellObject(0, 1, "New name");
        Util.sleep(100);
        String name = tester.getContext().getShow().getChannels().get(0).getName();
        assertEquals(name, "New name");
    }

    @Test
    public void changeDimmerName() {
        sortDetailTableOnDimmerNumber();
        JTableOperator table = tester.getTable("detailTable");
        table.clickForEdit(0, 2);
        table.changeCellObject(0, 2, "New name");
        Util.sleep(100);
        String name = tester.getContext().getShow().getDimmers().get(0).getName();
        assertEquals(name, "New name");
    }

    @Test
    public void dragUnPatchToFrame() throws AWTException {
        defaultPatch();
        assertTrue(tester.getContext().getShow().getDimmers().get(0).isPatched());
        JTableOperator table = tester.getTable("detailTable");
        Point click = table.getPointToClick(0, PatchDetail.CHANNEL_NAME);
        Point dragStart = table.getLocationOnScreen();
        dragStart.translate(click.x, click.y);
        Point dragEnd = new Point(10, dragStart.y);
        tester.drag(dragStart, dragEnd);
        assertFalse(tester.getContext().getShow().getDimmers().get(0).isPatched());
    }

    @Test
    public void dragUnPatchToChannelTable() throws AWTException {
        defaultPatch();
        Dimmer dimmer = tester.getContext().getShow().getDimmers().get(0);
        assertTrue(dimmer.isPatched());
        JTableOperator detailTable = tester.getTable("detailTable");
        Point click = detailTable.getPointToClick(0, PatchDetail.CHANNEL_NAME);
        Point start = detailTable.getLocationOnScreen();
        start.translate(click.x, click.y);
        JTableOperator channelTable = tester.getTable("channelTable");
        click = channelTable.getPointToClick(0, 1);
        Point end = channelTable.getLocationOnScreen();
        end.translate(click.x, click.y);
        tester.drag(start, end);
        assertFalse(dimmer.isPatched());
    }

    @Test
    public void dragChannelName() throws AWTException {
        JTableOperator table = tester.getTable("channelTable");
        JTableHeaderOperator header = table.getHeaderOperator();
        header.selectColumn(0);
        Channels channels = tester.getContext().getShow().getChannels();
        channels.get(0).setName("Channel 1");
        channels.get(1).setName("Channel 2");
        channels.get(2).setName("Channel 3");
        Point start = tester.getTableCellLocation("channelTable", 2, 1);
        Point end = tester.getTableCellLocation("channelTable", 0, 1);
        Robot robot = new Robot();
        robot.setAutoWaitForIdle(true);
        robot.mouseMove(start.x, start.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseMove(10, start.y);
        Util.sleep(100);
        robot.mouseMove(end.x, end.y);
        Util.sleep(100);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        Util.sleep(100);
        assertEquals(channels.get(0).getName(), "Channel 3");
        assertEquals(channels.get(1).getName(), "Channel 1");
        assertEquals(channels.get(2).getName(), "Channel 2");
    }

    private boolean isOn(final int row) {
        return (Boolean) tester.getTable("detailTable").getValueAt(row, 0);
    }

    @Test
    public void closeFrame() {
        JFrameOperator frame = tester.getFrame();
        frame.close();
        Util.sleep(500);
        assertFalse(frame.isVisible());
    }

    private void defaultPatch() {
        tester.pushMenuNoBlock("Actions|Default Patch");
        JDialogOperator dialog = new JDialogOperator("Default Patch");
        JButtonOperator button = new JButtonOperator(dialog, "Yes");
        button.push();
        Util.sleep(100);
    }

    private void clearPatch() {
        tester.pushMenuNoBlock("Actions|Clear Patch");
        JDialogOperator dialog = new JDialogOperator("Clear Patch");
        JButtonOperator button = new JButtonOperator(dialog, "Yes");
        button.push();
        Util.sleep(100);
    }

    public static void main(final String[] args) throws Exception {
        PatchFrameTestCase.open();
    }
}
