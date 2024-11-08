package be.lassi.fixtures;

import static be.lassi.domain.Attribute.CMY;
import static be.lassi.domain.Attribute.INTENSITY;
import static be.lassi.domain.Attribute.PAN;
import static be.lassi.domain.Attribute.RGB;
import static be.lassi.domain.Attribute.TILT;
import static org.testng.Assert.assertEquals;
import java.awt.Color;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import be.lassi.control.device.LevelControl;
import be.lassi.domain.Attribute;
import be.lassi.domain.AttributeDefinition;
import be.lassi.domain.Fixture;
import be.lassi.domain.FixtureDefinition;
import be.lassi.domain.Fixtures;
import be.lassi.lanbox.domain.ChannelChange;
import be.lassi.util.ContextTestCaseA;

/**
 * Tests class <code>FixtureControl</code>.
 */
public class FixtureControlTestCase extends ContextTestCaseA {

    private final MockChannelChangeProcessor channelChangeProcessor = new MockChannelChangeProcessor();

    private Fixture fixture1;

    private Fixture fixture2;

    private Fixture fixture3;

    @BeforeMethod
    public void resetChannelChangeProcessor() {
        channelChangeProcessor.reset();
    }

    @Test
    public void testSettingAttributesForSelectedFixtures() {
        buildFixtures();
        FixtureControl control = new FixtureControl(getContext(), channelChangeProcessor);
        fixture1.setSelected(true);
        fixture2.setSelected(true);
        control.process(new SetAttribute(Attribute.INTENSITY, 255));
        assertEquals(channelChangeProcessor.getChannelChangeCount(), 2);
        assertChannelChange(0, 10, 255);
        assertChannelChange(1, 20, 255);
        fixture2.setSelected(false);
        fixture3.setSelected(true);
        control.process(new SetAttribute(Attribute.INTENSITY, 127));
        assertEquals(channelChangeProcessor.getChannelChangeCount(), 4);
        assertChannelChange(2, 10, 127);
        assertChannelChange(3, 30, 127);
    }

    @Test
    public void testColorCommand() {
        buildFixtures();
        FixtureControl control = new FixtureControl(getContext(), channelChangeProcessor);
        control.process(new SetColor(new Color(23, 127, 255)));
        assertEquals(channelChangeProcessor.getChannelChangeCount(), 6);
        assertChannelChange(0, 13, 23);
        assertChannelChange(1, 14, 127);
        assertChannelChange(2, 15, 255);
        assertChannelChange(3, 23, 255 - 23);
        assertChannelChange(4, 24, 255 - 127);
        assertChannelChange(5, 25, 255 - 255);
    }

    private void assertChannelChange(final int index, final int channelId, final int dmxValue) {
        ChannelChange change = channelChangeProcessor.get(index);
        assertEquals(change.getChannelId(), channelId);
        assertEquals(change.getDmxValue(), dmxValue);
    }

    private void buildFixtures() {
        FixtureDefinition definition1 = new FixtureDefinition();
        definition1.addAttribute(INTENSITY, "1");
        definition1.addAttribute(PAN, "2");
        definition1.addAttribute(TILT, "3");
        definition1.addAttribute(RGB, "4,5,6");
        FixtureDefinition definition2 = new FixtureDefinition();
        definition2.addAttribute(INTENSITY, "1");
        definition2.addAttribute(PAN, "2");
        definition2.addAttribute(TILT, "3");
        definition2.addAttribute(CMY, "4,5,6");
        fixture1 = new Fixture(definition1, "1", 10);
        fixture2 = new Fixture(definition2, "2", 20);
        fixture3 = new Fixture(definition2, "3", 30);
        Fixtures fixtures = getContext().getShow().getFixtures();
        fixtures.add(fixture1);
        fixtures.add(fixture2);
        fixtures.add(fixture3);
        fixture1.setSelected(true);
        fixture2.setSelected(true);
    }

    @Test
    public void testConnectionBetweenControlAndFixtureControl() {
        LevelControl panFader = new LevelControl("F1", 1);
        LevelControl tiltFader = new LevelControl("F2", 2);
        Attribute pan = new Attribute(new AttributeDefinition(PAN, ""));
        Attribute tilt = new Attribute(new AttributeDefinition(TILT, ""));
        DeviceMap map = new DeviceMap();
        map.put(pan.getDefinition().getName(), panFader);
        map.put(tilt.getDefinition().getName(), tiltFader);
        MockFixtureCommandProcessor processor = new MockFixtureCommandProcessor();
        FixtureCommandProducer producer = new FixtureCommandProducer(processor);
        producer.setDeviceMap(map);
        panFader.setLevel(50);
        tiltFader.setLevel(100);
        assertFixtureCommand(processor, 0, PAN, 127);
        assertFixtureCommand(processor, 1, TILT, 255);
    }

    private void assertFixtureCommand(final MockFixtureCommandProcessor processor, final int index, final String name, final int dmxValue) {
        SetAttribute command = (SetAttribute) processor.get(index);
        assertEquals(command.getAttributeName(), name);
        assertEquals(command.getDmxValue(), dmxValue);
    }
}
