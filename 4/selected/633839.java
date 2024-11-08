package be.lassi.cues;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import java.util.List;
import org.testng.annotations.Test;
import be.lassi.base.DirtyIndicator;
import be.lassi.lanbox.cuesteps.Comment;
import be.lassi.lanbox.cuesteps.CueScene;
import be.lassi.lanbox.cuesteps.CueStep;
import be.lassi.lanbox.domain.ChannelChanges;
import be.lassi.lanbox.domain.FadeType;
import be.lassi.lanbox.domain.Time;

/**
 * Tests class <code>Cue</code>.
 */
public class CueTestCase {

    @Test
    public void equals() {
        Cue cue1 = newCue();
        Cue cue2 = newCue();
        assertEquals(cue1, cue2);
        cue1.setNumber("number");
        assertFalse(cue1.equals(cue2));
        cue2.setNumber("number");
        assertTrue(cue1.equals(cue2));
        cue1.setPage("page");
        assertFalse(cue1.equals(cue2));
        cue2.setPage("page");
        assertTrue(cue1.equals(cue2));
        cue1.setPrompt("prompt");
        assertFalse(cue1.equals(cue2));
        cue2.setPrompt("prompt");
        assertTrue(cue1.equals(cue2));
        cue1.setComment("comment");
        assertFalse(cue1.equals(cue2));
        cue2.setComment("comment");
        assertTrue(cue1.equals(cue2));
    }

    /**
     * Test setting and resetting the 'current' attribute.
     */
    @Test
    public void current() {
        Cue cue = newCue();
        TestingCueListener listener = new TestingCueListener();
        cue.addListener(listener);
        assertFalse(cue.isCurrent());
        cue.setCurrent(true);
        assertTrue(cue.isCurrent());
        assertTrue(listener.isCurrentChanged());
        assertTrue(listener.getCueDefinition() == cue);
        listener.setCueDefinition(null);
        listener.setCurrentChanged(false);
        cue.setCurrent(false);
        assertFalse(cue.isCurrent());
        assertTrue(listener.isCurrentChanged());
        assertTrue(listener.getCueDefinition() == cue);
    }

    /**
     * Test setting and resetting the 'number' attribute.
     */
    @Test
    public void number() {
        Cue cue = newCue();
        TestingCueListener listener = new TestingCueListener();
        cue.addListener(listener);
        cue.setNumber("test1");
        assertEquals(cue.getNumber(), "test1");
        assertTrue(listener.isNumberChanged());
        assertTrue(listener.getCueDefinition() == cue);
        listener.setCueDefinition(null);
        listener.setNumberChanged(false);
        cue.setNumber(listener, "test2");
        assertEquals(cue.getNumber(), "test2");
        assertFalse(listener.isNumberChanged());
        assertNull(listener.getCueDefinition());
    }

    /**
     * Test setting and resetting the 'selected' attribute.
     */
    @Test
    public void selected() {
        Cue cue = newCue();
        TestingCueListener listener = new TestingCueListener();
        cue.addListener(listener);
        assertFalse(cue.isSelected());
        cue.setSelected(true);
        assertTrue(cue.isSelected());
        assertTrue(listener.isSelectedChanged());
        assertTrue(listener.getCueDefinition() == cue);
        listener.setCueDefinition(null);
        listener.setSelectedChanged(false);
        cue.setSelected(false);
        assertFalse(cue.isSelected());
        assertTrue(listener.isSelectedChanged());
        assertTrue(listener.getCueDefinition() == cue);
    }

    @Test
    public void copy() {
        DirtyIndicator dirty = new DirtyIndicator();
        Cue cue1 = new Cue(dirty, "number1", "page1", "prompt1", "L 1");
        new CueDetailFactory(24, 12).update(cue1);
        TestingCueListener listener = new TestingCueListener();
        cue1.addListener(listener);
        cue1.setPage("page1");
        cue1.setCurrent(true);
        cue1.setSelected(true);
        LightCueDetail detail1 = (LightCueDetail) cue1.getDetail();
        detail1.getChannelLevel(0).setChannelValue(0.4f);
        detail1.getChannelLevel(0).setSubmasterValue(0.5f);
        detail1.getSubmasterLevel(0).getLevelValue().setValue(0.6f);
        Cue cue2 = cue1.copy();
        assertEquals(cue1, cue2);
        assertEquals(cue1.getPage(), cue2.getPage());
        assertEquals(cue1.getNumber(), cue2.getNumber());
        assertEquals(cue1.getPrompt(), cue2.getPrompt());
        assertEquals(cue1.getDescription(), cue2.getDescription());
        LightCueDetail detail2 = (LightCueDetail) cue2.getDetail();
        assertEquals(detail1.getTiming(), detail2.getTiming());
        assertEquals(detail2.getChannelLevel(0).getChannelLevelValue().getValue(), 0.4f, 0.05f);
        assertEquals(detail2.getChannelLevel(0).getSubmasterValue(), 0.5f, 0.05f);
        assertEquals(detail2.getSubmasterLevel(0).getValue(), 0.6f, 0.05f);
        assertFalse(cue2.isCurrent());
        assertFalse(cue2.isSelected());
        cue1.setPage("page2");
        assertEquals(cue2.getPage(), "page1");
        cue1.setNumber("number2");
        assertEquals(cue2.getNumber(), "number1");
        cue1.setPrompt("prompt2");
        assertEquals(cue2.getPrompt(), "prompt1");
        detail1.getTiming().setFadeInDelay(Time.TIME_2S);
        assertEquals(detail2.getTiming().getFadeInDelay(), Time.TIME_0S);
        detail1.getChannelLevel(0).setChannelValue(0.2f);
        assertEquals(detail2.getChannelLevel(0).getChannelLevelValue().getValue(), 0.4f, 0.05f);
        detail1.getChannelLevel(0).setSubmasterValue(0.3f);
        assertEquals(detail2.getChannelLevel(0).getSubmasterValue(), 0.5f, 0.05f);
        detail1.getSubmasterLevel(0).getLevelValue().setValue(0.4f);
        assertEquals(detail2.getSubmasterLevel(0).getValue(), 0.6f, 0.05f);
        listener.setCurrentChanged(false);
        cue2.setCurrent(true);
        assertFalse(listener.isCurrentChanged());
        dirty.clear();
        assertFalse(cue1.isDirty());
        assertFalse(cue2.isDirty());
        dirty.mark();
        assertTrue(cue1.isDirty());
        assertTrue(cue2.isDirty());
    }

    @Test
    public void dirty() {
        DirtyIndicator dirty = new DirtyIndicator();
        Cue cue = new Cue(dirty, "", "", "", "");
        assertFalse(cue.isDirty());
        cue.setNumber("number");
        assertTrue(cue.isDirty());
        dirty.clear();
        cue.setPage("page");
        assertTrue(cue.isDirty());
        dirty.clear();
        cue.setPrompt("prompt");
        assertTrue(cue.isDirty());
        dirty.clear();
        cue.setDescription("description");
        assertTrue(cue.isDirty());
    }

    @Test
    public void testGetCueSteps() {
        CueStep expectedStep1 = new Comment("1-2-3");
        ChannelChanges changes = new ChannelChanges();
        changes.add(1, 0);
        CueScene expectedStep2 = new CueScene(FadeType.CROSS_FADE, Time.TIME_2S, Time.FOREVER, changes);
        Cue cue = new Cue("1.2.3", "", "", "L 2");
        new CueDetailFactory(1, 1).update(cue);
        List<CueStep> cueSteps = cue.getCueSteps();
        assertEquals(cueSteps.size(), 2);
        assertEquals(cueSteps.get(0), expectedStep1);
        assertEquals(cueSteps.get(1), expectedStep2);
    }

    private Cue newCue() {
        return new Cue("", "", "", "");
    }
}
