package be.lassi.io;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;
import be.lassi.base.DirtyStub;
import be.lassi.cues.Cue;
import be.lassi.cues.CueDetailFactory;
import be.lassi.cues.LightCues;
import be.lassi.domain.Channel;
import be.lassi.domain.Dimmer;
import be.lassi.domain.Group;
import be.lassi.domain.Groups;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;
import be.lassi.domain.Submaster;
import be.lassi.util.equals.EqualsTester;

/**
 * Tests class <code>ShowWriter</code>.
 */
public class ShowWriterTestCase {

    @Test
    public void writeAndReadShow() throws ShowFileException {
        Show show1 = ShowBuilder.example();
        Show show2 = doTest(show1);
        assertEquals(show1, show2);
        show1 = ShowBuilder.build(2, 4, 6, "");
        show2 = doTest(show1);
        assertEquals(show1, show2);
    }

    @Test
    public void writeAndReadGroups() throws ShowFileException {
        Show show1 = ShowBuilder.example();
        Group group1 = new Group("Group one");
        Group group2 = new Group("Group two");
        Group group3 = new Group("Group three");
        show1.getGroups().add(group1);
        show1.getGroups().add(group2);
        show1.getGroups().add(group3);
        group1.add(show1.getChannels().get(0));
        group1.add(show1.getChannels().get(1));
        group1.add(show1.getChannels().get(2));
        group2.add(show1.getChannels().get(10));
        group2.add(show1.getChannels().get(5));
        group2.add(show1.getChannels().get(1));
        Show show2 = doTest(show1);
        Groups groups = show2.getGroups();
        assertEquals(groups.size(), 3);
        assertEquals(groups.get(0).getName(), "Group one");
        assertEquals(groups.get(1).getName(), "Group two");
        assertEquals(groups.get(2).getName(), "Group three");
        assertEquals(groups.get(0).size(), 3);
        assertEquals(groups.get(1).size(), 3);
        assertEquals(groups.get(2).size(), 0);
        Channel[] channels = groups.get(0).getChannels();
        assertEquals(channels[0].getId(), 0);
        assertEquals(channels[1].getId(), 1);
        assertEquals(channels[2].getId(), 2);
        channels = groups.get(1).getChannels();
        assertEquals(channels[0].getId(), 10);
        assertEquals(channels[1].getId(), 5);
        assertEquals(channels[2].getId(), 1);
        channels = groups.get(2).getChannels();
        assertEquals(channels.length, 0);
    }

    @Test
    public void writeAndReadDimmers() throws ShowFileException {
        Show show1 = ShowBuilder.build(2, 2, 4, "");
        Dimmer dimmer1 = show1.getDimmers().get(0);
        Dimmer dimmer2 = show1.getDimmers().get(1);
        Dimmer dimmer3 = show1.getDimmers().get(2);
        Dimmer dimmer4 = show1.getDimmers().get(3);
        Channel channel1 = show1.getChannels().get(0);
        Channel channel2 = show1.getChannels().get(1);
        dimmer1.setName("D1");
        dimmer1.setName("D2");
        dimmer1.setName(" D 3 ");
        dimmer1.setName("");
        dimmer1.setChannel(channel2);
        dimmer2.setChannel(null);
        dimmer3.setChannel(channel1);
        dimmer4.setChannel(channel1);
        Show show2 = doTest(show1);
        assertEquals(show1, show2);
    }

    @Test
    public void writeAndReadChannels() {
    }

    @Test
    public void writeAndReadSubmasters() throws ShowFileException {
        Show show1 = ShowBuilder.example();
        Submaster submaster1 = show1.getSubmasters().get(0);
        Submaster submaster2 = show1.getSubmasters().get(1);
        submaster1.setName("Name 1");
        submaster2.setName("Name 2");
        submaster1.getLevel(0).setActive(true);
        submaster1.getLevel(0).setIntValue(10);
        submaster1.getLevel(1).setActive(false);
        submaster1.getLevel(1).setIntValue(0);
        submaster1.getLevel(2).setActive(true);
        submaster1.getLevel(2).setIntValue(0);
        Show show2 = doTest(show1);
        submaster1 = show2.getSubmasters().get(0);
        submaster2 = show2.getSubmasters().get(1);
        assertEquals(submaster1.getName(), "Name 1");
        assertEquals(submaster2.getName(), "Name 2");
        assertTrue(submaster1.getLevel(0).isActive());
        assertFalse(submaster1.getLevel(1).isActive());
        assertTrue(submaster1.getLevel(2).isActive());
        assertEquals(submaster1.getLevel(0).getIntValue(), 10);
        assertEquals(submaster1.getLevel(1).getIntValue(), 0);
        assertEquals(submaster1.getLevel(2).getIntValue(), 0);
        assertEquals(show1, show2);
    }

    @Test
    public void writeAndReadCues() throws ShowFileException {
        Show show1 = ShowBuilder.example();
        Cue cue = new Cue("Cue one", "page1", "prompt1", "L 2");
        new CueDetailFactory(show1.getNumberOfChannels(), show1.getNumberOfSubmasters()).update(cue);
        show1.getCues().add(cue);
        LightCues cues = show1.getCues().getLightCues();
        cues.setCueSubmaster(0, 0, 0.1f);
        cues.deactivateCueSubmaster(0, 1);
        cues.setChannel(0, 0, 0.2f);
        cues.deactivateChannel(0, 1);
        Show show2 = doTest(show1);
        EqualsTester.assertEquals(show1, show2);
    }

    @Test
    public void writeAndReadFrameProperties() {
    }

    private Show doTest(final Show show) throws ShowFileException {
        new ShowWriter("/tmp/test.show").write(show);
        Show show2 = new ShowReader(new DirtyStub(), "/tmp/test.show").getShow();
        return show2;
    }
}
