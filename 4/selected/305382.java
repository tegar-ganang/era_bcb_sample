package be.lassi.io;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import java.io.Reader;
import java.io.StringReader;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import be.lassi.base.DirtyStub;
import be.lassi.cues.Cue;
import be.lassi.cues.LightCueDetail;
import be.lassi.domain.FrameProperties;
import be.lassi.domain.Group;
import be.lassi.domain.Groups;
import be.lassi.domain.Level;
import be.lassi.domain.Show;
import be.lassi.domain.Submaster;
import be.lassi.lanbox.domain.Time;

/**
 * Tests class <code>ShowInterpreter</code>.
 */
public class ShowInterpreterTestCase {

    private final StringBuilder b = new StringBuilder();

    private Show show;

    @BeforeMethod
    public void setUp() {
        b.setLength(0);
        line("CHANNELS 2");
        line("DIMMERS 3");
        line("SUBMASTERS 4");
    }

    @Test
    public void interpreteBasicParameters() {
        interprete();
        assertEquals(show.getNumberOfChannels(), 2);
        assertEquals(show.getNumberOfDimmers(), 3);
        assertEquals(show.getNumberOfSubmasters(), 4);
    }

    @Test
    public void interpreteChannel() {
        line("CHANNEL 1 \"channel one\"");
        interprete();
        assertEquals(show.getChannels().get(0).getName(), "channel one");
        assertEquals(show.getChannels().get(1).getName(), "Channel 2");
    }

    @Test
    public void interpreteChannelNumberExceedsMaximum() {
        line("CHANNEL 3 Invalid");
        ShowFileException e = getException();
        assertEquals(e.displayString(), "Exception at line number 4\nMessage: Value exceeds maximum (maximum=2)\n    > channel number\n");
    }

    @Test
    public void interpreteChannelNumberTooSmall() {
        line("CHANNEL 0 Invalid");
        ShowFileException e = getException();
        assertEquals(e.displayString(), "Exception at line number 4\nMessage: Value too small (minimum=1)\n    > channel number\n");
    }

    @Test
    public void interpreteDimmer() {
        line("DIMMER 1 \"dimmer one\" 2");
        interprete();
        assertEquals(show.getDimmers().get(0).getName(), "dimmer one");
        assertEquals(show.getDimmers().get(1).getName(), "Dimmer 2");
        assertEquals(show.getDimmers().get(0).getChannelId(), 1);
        assertEquals(show.getDimmers().get(1).getChannelId(), -1);
    }

    @Test
    public void interpreteDimmerNumberExceedsMaximum() {
        line("DIMMER 4 Dimmer1 2");
        ShowFileException e = getException();
        assertEquals(e.displayString(), "Exception at line number 4\nMessage: Value exceeds maximum (maximum=3)\n    > dimmer number\n");
    }

    @Test
    public void interpreteDimmerNumberTooSmall() {
        line("DIMMER 0 Dimmer1 2");
        ShowFileException e = getException();
        assertEquals(e.displayString(), "Exception at line number 4\nMessage: Value too small (minimum=1)\n    > dimmer number\n");
    }

    @Test
    public void interpreteDimmerChannelNumberInvalid1() {
        line("DIMMER 1 Dimmer1 4");
        ShowFileException e = getException();
        assertEquals(e.displayString(), "Exception at line number 4\nMessage: Value exceeds maximum (maximum=2)\n    > dimmer channel patch\n");
    }

    @Test
    public void interpreteDimmerChannelNumberInvalid2() {
        line("DIMMER 1 Dimmer1 -1");
        ShowFileException e = getException();
        assertEquals(e.displayString(), "Exception at line number 4\nMessage: Value too small (minimum=0)\n    > dimmer channel patch\n");
    }

    @Test
    public void interpreteDimmerNotPatched() {
        line("DIMMER 1 Dimmer1 0");
        interprete();
        assertEquals(show.getDimmers().get(0).getChannelId(), -1);
    }

    @Test
    public void interpreteSubmaster() {
        line("SUBMASTER 1 \"submaster one\" 10 -");
        line("SUBMASTER 2 \"submaster two\" - -");
        interprete();
        assertSubmasterName("submaster one", 0);
        assertSubmasterName("submaster two", 1);
        assertSubmasterLevel(10, 0, 0);
        assertSubmasterActive(true, 0, 0);
        assertSubmasterActive(false, 0, 1);
        assertSubmasterActive(false, 1, 0);
        assertSubmasterActive(false, 1, 1);
    }

    @Test
    public void interpreteFrame() {
        line("FRAME TestFrame1 10 20 30 40 hidden");
        line("FRAME TestFrame2 50 60 70 80 visible");
        interprete();
        FrameProperties p = show.getFrameProperties("TestFrame1");
        assertEquals(p.getId(), "TestFrame1");
        assertEquals(p.getBounds().x, 10);
        assertEquals(p.getBounds().y, 20);
        assertEquals(p.getBounds().width, 30);
        assertEquals(p.getBounds().height, 40);
        assertFalse(p.isVisible());
        p = show.getFrameProperties("TestFrame2");
        assertEquals(p.getId(), "TestFrame2");
        assertEquals(p.getBounds().x, 50);
        assertEquals(p.getBounds().y, 60);
        assertEquals(p.getBounds().width, 70);
        assertEquals(p.getBounds().height, 80);
        assertTrue(p.isVisible());
    }

    @Test
    public void interpreteGroup() {
        line("GROUP \"Group 1\" 2 2 1");
        line("GROUP \"Group 2\" 1 2");
        interprete();
        Groups groups = show.getGroups();
        assertEquals(groups.size(), 2);
        Group group1 = groups.get(0);
        Group group2 = groups.get(1);
        assertEquals(group1.getName(), "Group 1");
        assertEquals(group2.getName(), "Group 2");
        assertEquals(group1.size(), 2);
        assertEquals(group2.size(), 1);
        assertEquals(group1.get(0).getId(), 1);
        assertEquals(group1.get(1).getId(), 0);
        assertEquals(group2.get(0).getId(), 1);
    }

    @Test
    public void interpreteCue() {
        line("SUBMASTER 1 Submaster1 10 20");
        line("SUBMASTER 2 Submaster2 10 20");
        line("SUBMASTER 3 Submaster3 10 20");
        line("SUBMASTER 4 Submaster4 10 20");
        line("CUE \"number\" \"page\" \"prompt\" \"L 5\" 50 x - 40 x 50");
        interprete();
        Cue cue = show.getCues().get(0);
        assertEquals(cue.getNumber(), "number");
        assertEquals(cue.getPage(), "page");
        assertEquals(cue.getPrompt(), "prompt");
        LightCueDetail detail = (LightCueDetail) cue.getDetail();
        assertEquals(detail.getTiming().getFadeInTime(), Time.TIME_5S);
        assertFalse(detail.getSubmasterLevel(0).isDerived());
        assertTrue(detail.getSubmasterLevel(1).isDerived());
        assertFalse(detail.getSubmasterLevel(2).isDerived());
        assertFalse(detail.getSubmasterLevel(3).isDerived());
        assertTrue(detail.getChannelLevel(0).isDerived());
        assertFalse(detail.getChannelLevel(1).isDerived());
        assertEquals(detail.getSubmasterLevel(0).getIntValue(), 50);
        assertEquals(detail.getSubmasterLevel(1).getIntValue(), 0);
        assertEquals(detail.getSubmasterLevel(2).getIntValue(), 0);
        assertEquals(detail.getSubmasterLevel(3).getIntValue(), 40);
        assertEquals(detail.getChannelLevel(0).getChannelIntValue(), 0);
        assertEquals(detail.getChannelLevel(1).getChannelIntValue(), 50);
        assertEquals(detail.getChannelLevel(0).getSubmasterIntValue(), 5);
        assertEquals(detail.getChannelLevel(1).getSubmasterIntValue(), 10);
        assertTrue(detail.getChannelLevel(0).getChannelLevelValue().isActive());
        assertTrue(detail.getChannelLevel(1).getChannelLevelValue().isActive());
    }

    private void interprete() {
        Reader reader = new StringReader(b.toString());
        try {
            show = new ShowInterpreter(new DirtyStub(), reader).getShow();
        } catch (ShowFileException e) {
            System.out.println(e.displayString());
            fail("Unexpected ShowFileException " + e);
        }
    }

    private ShowFileException getException() {
        ShowFileException result = null;
        Reader reader = new StringReader(b.toString());
        try {
            new ShowInterpreter(new DirtyStub(), reader).getShow();
            fail("Expected exception not thrown");
        } catch (ShowFileException e) {
            result = e;
        }
        return result;
    }

    private void line(final String string) {
        b.append(string);
        b.append('\n');
    }

    private void assertSubmasterActive(final boolean expected, final int submasterIndex, final int channelIndex) {
        Submaster submaster = show.getSubmasters().get(submasterIndex);
        boolean result = submaster.getLevel(channelIndex).isActive();
        assertEquals(result, expected);
    }

    private void assertSubmasterName(final String expected, final int submasterIndex) {
        Submaster submaster = show.getSubmasters().get(submasterIndex);
        assertEquals(submaster.getName(), expected);
    }

    private void assertSubmasterLevel(final int expected, final int submasterIndex, final int channelIndex) {
        Submaster submaster = show.getSubmasters().get(submasterIndex);
        Level level = submaster.getLevel(channelIndex);
        assertEquals(level.getIntValue(), expected);
    }
}
