package be.lassi.lanbox.domain;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;
import be.lassi.support.ObjectBuilder;
import be.lassi.support.ObjectTest;

/**
 * Tests class <code>ChannelChanges</code>.
 */
public class ChannelChangesTestCase {

    @Test
    public void splitEmpty() {
        ChannelChanges changes = new ChannelChanges();
        ChannelChanges[] cc = changes.split(5);
        assertEquals(cc.length, 0);
    }

    @Test
    public void splitNoSplit1() {
        ChannelChanges changes = new ChannelChanges();
        changes.add(1, 0);
        changes.add(2, 0);
        ChannelChanges[] cc = changes.split(3);
        assertTrue(changes == cc[0]);
    }

    @Test
    public void splitNoSplit2() {
        ChannelChanges changes = new ChannelChanges();
        changes.add(1, 0);
        changes.add(2, 0);
        changes.add(3, 0);
        ChannelChanges[] cc = changes.split(3);
        assertTrue(changes == cc[0]);
    }

    @Test
    public void split() {
        ChannelChanges changes = new ChannelChanges();
        changes.add(1, 0);
        changes.add(2, 0);
        changes.add(3, 0);
        changes.add(4, 0);
        ChannelChanges[] cc = changes.split(3);
        assertEquals(cc[0].size(), 3);
        assertEquals(cc[1].size(), 1);
        ChannelChange[] cc1 = cc[0].toArray();
        ChannelChange[] cc2 = cc[1].toArray();
        assertEquals(cc1[0].getChannelId(), 1);
        assertEquals(cc1[1].getChannelId(), 2);
        assertEquals(cc1[2].getChannelId(), 3);
        assertEquals(cc2[0].getChannelId(), 4);
    }

    @Test
    public void eliminateDoubles() {
        ChannelChanges changes = new ChannelChanges();
        changes.add(1, 1);
        changes.add(2, 2);
        changes.add(2, 3);
        changes.add(4, 4);
        changes.eliminateDoubles();
        assertEquals(changes.size(), 3);
        ChannelChange[] cc = changes.toArray();
        assertEquals(cc[0].getDmxValue(), 1);
        assertEquals(cc[1].getDmxValue(), 3);
        assertEquals(cc[2].getDmxValue(), 4);
    }

    @Test
    public void eliminateDoublesNoDoubles() {
        ChannelChanges changes = new ChannelChanges();
        changes.add(1, 1);
        changes.add(2, 2);
        changes.add(3, 3);
        changes.eliminateDoubles();
        assertEquals(changes.size(), 3);
        ChannelChange[] cc = changes.toArray();
        assertEquals(cc[0].getDmxValue(), 1);
        assertEquals(cc[1].getDmxValue(), 2);
        assertEquals(cc[2].getDmxValue(), 3);
    }

    @Test
    public void testSet() {
        ChannelChanges changes1 = new ChannelChanges();
        ChannelChanges changes2 = new ChannelChanges();
        changes1.add(1, 10);
        changes1.add(2, 20);
        changes2.set(changes1);
        assertEquals(changes1, changes2);
    }

    @Test
    public void testGetString() {
        ChannelChanges changes = new ChannelChanges();
        changes.add(1, 10);
        changes.add(2, 20);
        assertEquals(changes.getString(), "1[10] 2[20] ");
    }

    @Test
    public void object() {
        ObjectBuilder b = new ObjectBuilder() {

            public Object getObject1() {
                ChannelChanges changes = new ChannelChanges();
                changes.add(1, 10);
                return changes;
            }

            public Object getObject2() {
                ChannelChanges changes = new ChannelChanges();
                changes.add(1, 20);
                return changes;
            }
        };
        ObjectTest.test(b);
    }

    @Test
    public void testAppend1() {
        String expected = "\n" + "      1[10]\n";
        assertAppend(expected, 1);
    }

    @Test
    public void testAppend2() {
        String expected = "\n" + "      1[10] 2[20]\n";
        assertAppend(expected, 2);
    }

    @Test
    public void testAppend3() {
        String expected = "\n" + "      1[10] 2[20] 3[30] 4[40] 5[50] 6[60] 7[70] 8[80] 9[90] 10[100]\n";
        assertAppend(expected, 10);
    }

    @Test
    public void testAppend4() {
        String expected = "\n" + "      1[10] 2[20] 3[30] 4[40] 5[50] 6[60] 7[70] 8[80] 9[90] 10[100]\n" + "      11[110]\n";
        assertAppend(expected, 11);
    }

    @Test
    public void testAppend5() {
        String expected = "\n" + "      1[10] 2[20] 3[30] 4[40] 5[50] 6[60] 7[70] 8[80] 9[90] 10[100]\n" + "      11[110] 12[120] 13[130] 14[140] 15[150] 16[160] 17[170] 18[180] 19[190] 20[200]\n";
        assertAppend(expected, 20);
    }

    @Test
    public void testAppend6() {
        String expected = "\n" + "      1[10] 2[20] 3[30] 4[40] 5[50] 6[60] 7[70] 8[80] 9[90] 10[100]\n" + "      11[110] 12[120] 13[130] 14[140] 15[150] 16[160] 17[170] 18[180] 19[190] 20[200]\n" + "      21[210]\n";
        assertAppend(expected, 21);
    }

    private void assertAppend(final String expected, final int channelCount) {
        ChannelChanges changes = new ChannelChanges();
        for (int i = 0; i < channelCount; i++) {
            changes.add(i + 1, (i + 1) * 10);
        }
        StringBuilder b = new StringBuilder();
        changes.append(b);
        String string = b.toString();
        assertEquals(string, expected);
    }
}
