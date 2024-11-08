package be.lassi.domain;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;
import be.lassi.support.ObjectBuilder;
import be.lassi.support.ObjectTest;

public class FixtureTestCase {

    private Fixture fixture;

    @Test
    public void testFixtureChannels() {
        FixtureDefinition definition = new FixtureDefinition();
        definition.addAttribute("Intensity", "1");
        definition.addAttribute("Pan", "2");
        definition.addAttribute("Tilt", "3");
        fixture = new Fixture(definition);
        assertChannels("Intensity", 0);
        assertChannels("Pan", 0);
        assertChannels("Tilt", 0);
        fixture.setAddress(11);
        assertChannels("Intensity", 11);
        assertChannels("Pan", 12);
        assertChannels("Tilt", 13);
    }

    @Test
    public void testFixtureWithDualChannels() {
        FixtureDefinition definition = new FixtureDefinition();
        definition.addAttribute("Intensity", "1");
        definition.addAttribute("Pan", "2,3");
        definition.addAttribute("Tilt", "4,5");
        fixture = new Fixture(definition);
        assertChannels("Intensity", 0);
        assertChannels("Pan", 0, 0);
        assertChannels("Tilt", 0, 0);
        fixture.setAddress(11);
        assertChannels("Intensity", 11);
        assertChannels("Pan", 12, 13);
        assertChannels("Tilt", 14, 15);
    }

    @Test
    public void testFixtureWithRgbChannels() {
        FixtureDefinition definition = new FixtureDefinition();
        definition.addAttribute("Intensity", "1");
        definition.addAttribute("RGB", "2,3,4");
        fixture = new Fixture(definition);
        assertChannels("Intensity", 0);
        assertChannels("RGB", 0, 0, 0);
        fixture.setAddress(11);
        assertChannels("Intensity", 11);
        assertChannels("RGB", 12, 13, 14);
    }

    @Test
    public void testGetMaxChannelNumber() {
        FixtureDefinition definition = new FixtureDefinition();
        definition.addAttribute("Intensity", "1");
        definition.addAttribute("RGB", "2,3,4");
        fixture = new Fixture(definition, "", 11);
        assertEquals(fixture.getMaxChannelNumber(), 14);
    }

    @Test
    public void testGetChannelNumber() {
        FixtureDefinition definition = new FixtureDefinition();
        definition.addAttribute("Intensity", "1");
        fixture = new Fixture(definition, "", 11);
        assertEquals(fixture.getChannelNumber("Intensity"), 11);
        assertEquals(fixture.getChannelNumber("Gobo"), 0);
    }

    @Test
    public void testGetPreset() {
        FixtureDefinition definition = new FixtureDefinition();
        definition.addPreset("home");
        fixture = new Fixture(definition, "", 0);
        assertEquals(fixture.getPreset("home").getDefinition().getName(), "home");
        assertEquals(fixture.getPreset("locate"), null);
    }

    @Test
    public void testInitPresets1() {
        fixture = newFixture("Intensity", "1", "home", "10");
        assertPreset("home", 0, "Intensity", 10);
    }

    @Test
    public void testInitPresets2() {
        fixture = newFixture("Pan", "1,2", "home", "10");
        assertPreset("home", 0, "Pan", 0);
        assertPreset("home", 1, "Pan-fine", 10);
    }

    @Test
    public void testInitPresets3() {
        fixture = newFixture("Tilt", "1,2", "home", "" + (256 * 10 + 20));
        assertPreset("home", 0, "Tilt", 10);
        assertPreset("home", 1, "Tilt-fine", 20);
    }

    @Test
    public void testInitPresets4() {
        fixture = newFixture("RGB", "1,2,1", "color1", "10,20,30");
        assertPreset("color1", 0, "Red", 10);
        assertPreset("color1", 1, "Green", 20);
        assertPreset("color1", 2, "Blue", 30);
    }

    private void assertPreset(final String presetName, final int valueIndex, final String expectedChannelName, final int expectedValue) {
        Preset preset = fixture.getPreset(presetName);
        PresetValue presetValue = preset.getValue(valueIndex);
        assertEquals(presetValue.getChannel().getName(), expectedChannelName);
        assertEquals(presetValue.getValue(), expectedValue);
    }

    private Fixture newFixture(final String attributeName, final String channels, final String presetName, final String presetValues) {
        FixtureDefinition definition = new FixtureDefinition();
        AttributeDefinition attributeDefinition = definition.addAttribute(attributeName, channels);
        PresetDefinition presetDefinition = definition.addPreset(presetName);
        presetDefinition.add(attributeDefinition, presetValues);
        return new Fixture(definition, "", 0);
    }

    @Test
    public void equals() {
        FixtureDefinition definition1 = new FixtureDefinition();
        FixtureDefinition definition2 = new FixtureDefinition();
        FixtureDefinition definition3 = new FixtureDefinition();
        FixtureDefinition definition4 = new FixtureDefinition();
        definition3.addAttribute("Intensity", "1");
        definition4.addPreset("home");
        Fixture fixture1 = new Fixture(definition1);
        Fixture fixture2 = new Fixture(definition2);
        Fixture fixture3 = new Fixture(definition3);
        Fixture fixture4 = new Fixture(definition4);
        assertTrue(fixture1.equals(fixture2));
        assertFalse(fixture1.equals(fixture3));
        assertFalse(fixture1.equals(fixture4));
        fixture1.setAddress(10);
        assertFalse(fixture1.equals(fixture2));
        fixture2.setAddress(10);
        assertTrue(fixture1.equals(fixture2));
        fixture1.setName("name1");
        assertFalse(fixture1.equals(fixture2));
        fixture2.setName("name1");
        assertTrue(fixture1.equals(fixture2));
        fixture1.setSelected(true);
        assertFalse(fixture1.equals(fixture2));
        fixture2.setSelected(true);
        assertTrue(fixture1.equals(fixture2));
    }

    @Test
    public void object() {
        ObjectBuilder b = new ObjectBuilder() {

            public Object getObject1() {
                FixtureDefinition definition = new FixtureDefinition();
                return new Fixture(definition, "name1", 1);
            }

            public Object getObject2() {
                FixtureDefinition definition = new FixtureDefinition();
                return new Fixture(definition, "name2", 1);
            }
        };
        ObjectTest.test(b);
    }

    private void assertChannels(final String attributeName, final int... channelNumbers) {
        Attribute attribute = fixture.getAttribute(attributeName);
        assertEquals(attribute.getChannelCount(), channelNumbers.length);
        for (int i = 0; i < channelNumbers.length; i++) {
            assertEquals(attribute.getChannel(i).getNumber(), channelNumbers[i]);
        }
    }
}
