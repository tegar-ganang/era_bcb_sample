package be.lassi.domain;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;
import be.lassi.support.ObjectBuilder;
import be.lassi.support.ObjectTest;

/**
 * Tests class <code>PresetValue</code>.
 */
public class PresetValueTestCase {

    @Test
    public void equals() {
        PresetValue value1 = newPresetValue("name1", 1);
        PresetValue value2 = newPresetValue("name1", 1);
        PresetValue value3 = newPresetValue("name2", 1);
        assertTrue(value1.equals(value2));
        assertFalse(value1.equals(value3));
        value1.getChannel().setNumber(2);
        assertFalse(value1.equals(value2));
        value2.getChannel().setNumber(2);
        assertTrue(value1.equals(value2));
    }

    @Test
    public void object() {
        ObjectBuilder b = new ObjectBuilder() {

            public Object getObject1() {
                return newPresetValue("name1", 1);
            }

            public Object getObject2() {
                return newPresetValue("name2", 1);
            }
        };
        ObjectTest.test(b);
    }

    private PresetValue newPresetValue(final String name, final int value) {
        FixtureChannel channel = new FixtureChannel(name, 1);
        return new PresetValue(channel, value);
    }
}
