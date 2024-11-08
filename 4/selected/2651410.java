package be.lassi.domain;

import static org.junit.Assert.assertArrayEquals;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import org.testng.annotations.Test;
import be.lassi.base.Listener;
import be.lassi.support.DirtyTest;
import be.lassi.support.ObjectBuilder;
import be.lassi.support.ObjectTest;

/**
 * Tests class <code>Groups</code>.
 */
public class GroupsTestCase implements Listener {

    private int changes;

    @Test
    public void listeners() {
        Groups groups = new Groups();
        groups.getListeners().add(this);
        Group group1 = new Group();
        Group group2 = new Group();
        groups.add(group1);
        assertEquals(1, changes);
        groups.add(group2);
        assertEquals(2, changes);
        int[] indexes1 = { 1 };
        groups.moveUp(indexes1);
        assertEquals(3, changes);
        int[] indexes2 = { 0 };
        groups.moveDown(indexes2);
        assertEquals(4, changes);
        groups.remove(0);
        assertEquals(5, changes);
        groups.remove(0);
        assertEquals(6, changes);
    }

    @Test
    public void getEnabledCount() {
        Group group1 = new Group("");
        Group group2 = new Group("");
        Groups groups = new Groups();
        assertEquals(0, groups.getEnabledGroupCount());
        groups.add(group1);
        groups.add(group2);
        assertEquals(0, groups.getEnabledGroupCount());
        group1.setEnabled(true);
        assertEquals(1, groups.getEnabledGroupCount());
        group2.setEnabled(true);
        assertEquals(2, groups.getEnabledGroupCount());
        groups.setAllEnabled(false);
        assertEquals(0, groups.getEnabledGroupCount());
        groups.setAllEnabled(true);
        assertEquals(2, groups.getEnabledGroupCount());
    }

    @Test
    public void getChannelCount() {
        Group group1 = new Group();
        Group group2 = new Group();
        Groups groups = new Groups();
        groups.add(group1);
        groups.add(group2);
        assertEquals(0, groups.getChannelCount(5));
        group1.add(new Channel(2, ""));
        assertEquals(1, groups.getChannelCount(5));
        group2.add(new Channel(3, ""));
        assertEquals(2, groups.getChannelCount(5));
    }

    @Test
    public void getChannelsNotInGroup() {
        Group group1 = new Group();
        Group group2 = new Group();
        Groups groups = new Groups();
        groups.add(group1);
        groups.add(group2);
        int indexes1[] = { 0, 1, 2, 3, 4 };
        assertArrayEquals(indexes1, groups.getChannelsNotInGroup(true, 0, 5));
        group2.add(new Channel(2, ""));
        int indexes2[] = { 0, 1, 3, 4 };
        assertArrayEquals(indexes1, groups.getChannelsNotInGroup(false, 0, 5));
        assertArrayEquals(indexes2, groups.getChannelsNotInGroup(true, 0, 5));
    }

    @Test
    public void getChannel() {
        Channel channel1 = new Channel(0, "1");
        Channel channel2 = new Channel(1, "2");
        Channel channel3 = new Channel(2, "3");
        Channel channel4 = new Channel(3, "4");
        Group group1 = new Group("");
        Group group2 = new Group("");
        group1.add(channel1);
        group1.add(channel2);
        group2.add(channel3);
        group2.add(channel4);
        Groups groups = new Groups();
        groups.add(group1);
        groups.add(group2);
        group1.setEnabled(true);
        group2.setEnabled(true);
        assertEquals(channel1, groups.getChannel(0));
        assertEquals(channel2, groups.getChannel(1));
        assertEquals(channel3, groups.getChannel(2));
        assertEquals(channel4, groups.getChannel(3));
        group1.setEnabled(false);
        group2.setEnabled(true);
        assertEquals(channel3, groups.getChannel(0));
        assertEquals(channel4, groups.getChannel(1));
    }

    @Test
    public void getChannelIndexOutOfBoundsException() {
        Channel channel1 = new Channel(0, "1");
        Channel channel2 = new Channel(1, "2");
        Group group1 = new Group("");
        Group group2 = new Group("");
        group1.add(channel1);
        group2.add(channel2);
        Groups groups = new Groups();
        groups.add(group1);
        groups.add(group2);
        group1.setEnabled(true);
        group2.setEnabled(true);
        try {
            groups.getChannel(2);
            fail();
        } catch (IndexOutOfBoundsException e) {
            assertEquals("Index: 2, Size: 2", e.getMessage());
        }
        group1.setEnabled(false);
        group2.setEnabled(true);
        try {
            groups.getChannel(1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            assertEquals("Index: 1, Size: 1", e.getMessage());
        }
        group1.setEnabled(false);
        group2.setEnabled(false);
        try {
            groups.getChannel(0);
            fail();
        } catch (IndexOutOfBoundsException e) {
            assertEquals("Index: 0, Size: 0", e.getMessage());
        }
    }

    @Test
    public void dirty() {
        DirtyTest test = new DirtyTest();
        Groups groups = new Groups(test.getDirty());
        groups.add(new Group("1"));
        test.dirty();
        groups.add(new Group("3"));
        groups.add(new Group("2"));
        test.dirty();
        int[] indexes = { 1 };
        groups.moveUp(indexes);
        test.dirty();
        groups.moveDown(indexes);
        test.dirty();
        groups.remove(0);
        test.dirty();
    }

    @Test
    public void testGetIndexOfChannelWithId() {
        Group group1 = new Group("1");
        Group group2 = new Group("2");
        Groups groups = new Groups();
        groups.add(group1);
        groups.add(group2);
        assertEquals(-1, groups.getIndexOfChannelWithId(0));
        group1.add(new Channel(0, "Channel 1"));
        group1.add(new Channel(1, "Channel 2"));
        group2.add(new Channel(2, "Channel 3"));
        group2.add(new Channel(3, "Channel 4"));
        assertEquals(0, groups.getIndexOfChannelWithId(0));
        assertEquals(1, groups.getIndexOfChannelWithId(1));
        assertEquals(2, groups.getIndexOfChannelWithId(2));
        assertEquals(3, groups.getIndexOfChannelWithId(3));
        group1.setEnabled(true);
        assertEquals(0, groups.getIndexOfChannelWithId(0));
        assertEquals(1, groups.getIndexOfChannelWithId(1));
        assertEquals(-1, groups.getIndexOfChannelWithId(2));
        assertEquals(-1, groups.getIndexOfChannelWithId(3));
        group2.setEnabled(true);
        assertEquals(0, groups.getIndexOfChannelWithId(0));
        assertEquals(1, groups.getIndexOfChannelWithId(1));
        assertEquals(2, groups.getIndexOfChannelWithId(2));
        assertEquals(3, groups.getIndexOfChannelWithId(3));
        group1.setEnabled(false);
        assertEquals(-1, groups.getIndexOfChannelWithId(0));
        assertEquals(-1, groups.getIndexOfChannelWithId(1));
        assertEquals(0, groups.getIndexOfChannelWithId(2));
        assertEquals(1, groups.getIndexOfChannelWithId(3));
    }

    @Test
    public void equals() {
        Groups groups1 = new Groups();
        Groups groups2 = new Groups();
        assertTrue(groups1.equals(groups2));
        groups1.add(new Group("Group 1"));
        assertFalse(groups1.equals(groups2));
        groups2.add(new Group("Group 1"));
        assertTrue(groups1.equals(groups2));
    }

    @Test
    public void object() {
        ObjectBuilder b = new ObjectBuilder() {

            public Object getObject1() {
                Groups groups = new Groups();
                groups.add(new Group("Group 1"));
                return groups;
            }

            public Object getObject2() {
                Groups groups = new Groups();
                groups.add(new Group("Group 2"));
                return groups;
            }
        };
        ObjectTest.test(b);
    }

    public void changed() {
        changes++;
    }
}
