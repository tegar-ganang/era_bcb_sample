package be.lassi.xml;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import org.testng.annotations.Test;
import be.lassi.base.DirtyStub;
import be.lassi.cues.Cue;
import be.lassi.cues.CueDetailFactory;
import be.lassi.cues.LightCues;
import be.lassi.domain.Channel;
import be.lassi.domain.Dimmer;
import be.lassi.domain.FrameProperties;
import be.lassi.domain.Group;
import be.lassi.domain.Groups;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;
import be.lassi.domain.Submaster;
import be.lassi.util.equals.EqualsTester;

/**
 * Tests classes <code>XmlShowWriter</code> and <code>XmlShowReader</code>.
 */
public class XmlShowWriterTestCase {

    @Test
    public void writeAndReadShow() throws IOException {
        Show show1 = ShowBuilder.example();
        Show show2 = doTest(show1);
        assertEquals(show1, show2);
        show1 = ShowBuilder.build(2, 4, 6, "");
        show2 = doTest(show1);
        assertEquals(show1, show2);
    }

    @Test
    public void writeAndReadGroups() throws IOException {
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
    public void writeAndReadDimmers() throws IOException {
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
    public void writeAndReadSubmasters() throws IOException {
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
    public void writeAndReadCues() throws IOException {
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
    public void writeAndReadFrameProperties() throws IOException {
        Show show1 = ShowBuilder.example();
        Rectangle bounds1 = new Rectangle(10, 20, 30, 40);
        FrameProperties properties1 = new FrameProperties("frame1", bounds1, true);
        show1.setFrameProperties(properties1.getId(), properties1);
        Rectangle bounds2 = new Rectangle(11, 21, 31, 41);
        FrameProperties properties2 = new FrameProperties("frame2", bounds2, false);
        show1.setFrameProperties(properties2.getId(), properties2);
        Show show2 = doTest(show1);
        EqualsTester.assertEquals(show1, show2);
    }

    @Test
    public void testEntities() throws IOException {
        Show show1 = ShowBuilder.build(2, 2, 4, "");
        Channel channel = show1.getChannels().get(0);
        channel.setName("Test & < > ; ' \"");
        Show show2 = doTest(show1);
        assertEquals(show1, show2);
    }

    @Test
    public void testBigShow() throws IOException {
        Show show1 = buildBigShow();
        writeNewXml(show1);
        long t1 = System.currentTimeMillis();
        Reader fr = new FileReader("/temp/test1.xml");
        Reader r = new BufferedReader(fr);
        XmlShowReader reader = new XmlShowReader(new DirtyStub(), r);
        String message = reader.parse();
        Show show2 = reader.getShow();
        System.out.println(message);
        long t2 = System.currentTimeMillis();
        System.out.println("Read XML in " + (t2 - t1) + "ms");
    }

    private void writeNewXml(final Show show1) throws IOException {
        long t1 = System.currentTimeMillis();
        Writer fw = new FileWriter("/temp/test1.xml");
        Writer bf = new BufferedWriter(fw);
        new XmlShowWriter(bf, show1).write();
        bf.close();
        long t2 = System.currentTimeMillis();
        System.out.println("New XML was written in " + (t2 - t1) + "ms");
    }

    private Show buildBigShow() {
        long t1 = System.currentTimeMillis();
        Show show1 = ShowBuilder.build(512, 512, 512, "");
        for (int i = 0; i < 350; i++) {
            Group group = new Group(show1.getDirty(), "Group " + (i + 1));
            show1.getGroups().add(group);
            for (Channel channel : show1.getChannels()) {
                group.add(channel);
            }
        }
        long t2 = System.currentTimeMillis();
        System.out.println("Big show was created in " + (t2 - t1) + "ms");
        return show1;
    }

    private Show doTest(final Show show) throws IOException {
        StringWriter sw = new StringWriter();
        new XmlShowWriter(sw, show).write();
        String string = sw.toString();
        System.out.println(string);
        StringReader sr = new StringReader(string);
        XmlShowReader reader = new XmlShowReader(show.getDirty(), sr);
        String message = reader.parse();
        if (message.length() > 0) {
            fail(message);
        }
        return reader.getShow();
    }
}
