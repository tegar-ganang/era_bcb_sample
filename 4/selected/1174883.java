package be.lassi.domain;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

/**
 * Tests class <code>Attribute</code>.
 */
public class AttributeTestCase {

    @Test
    public void initChannels1() {
        AttributeDefinition definition = new AttributeDefinition("", "5");
        Attribute attribute = new Attribute(definition);
        assertEquals(attribute.getChannelCount(), 1);
        assertEquals(attribute.getChannel(0).getOffset(), 5);
    }

    @Test
    public void testGetAttributeChannels2() {
        AttributeDefinition definition = new AttributeDefinition("", "4,5");
        Attribute attribute = new Attribute(definition);
        assertEquals(attribute.getChannelCount(), 2);
        assertEquals(attribute.getChannel(0).getOffset(), 4);
        assertEquals(attribute.getChannel(1).getOffset(), 5);
    }

    @Test
    public void testGetMaxChannelNumber() {
        AttributeDefinition definition = new AttributeDefinition("", "4,5");
        Attribute attribute = new Attribute(definition);
        assertEquals(attribute.getMaxChannelNumber(), 0);
        attribute.getChannel(0).setNumber(1);
        attribute.getChannel(1).setNumber(2);
        assertEquals(attribute.getMaxChannelNumber(), 2);
    }
}
