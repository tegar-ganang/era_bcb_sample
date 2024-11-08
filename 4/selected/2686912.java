package be.lassi.library;

import be.lassi.domain.AttributeDefinition;
import be.lassi.domain.AttributeValue;
import be.lassi.domain.FixtureDefinition;
import be.lassi.domain.PresetDefinition;
import be.lassi.domain.PresetValueDefinition;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class FixtureDefinitionConverter implements Converter {

    private final FixtureDefinition fixtureDefinition;

    public FixtureDefinitionConverter(final FixtureDefinition fixtureDefinition) {
        this.fixtureDefinition = fixtureDefinition;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public boolean canConvert(final Class clazz) {
        return clazz.equals(FixtureDefinition.class);
    }

    public void marshal(final Object value, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        marshallAttributes(writer);
        marshallPresets(writer);
    }

    private void marshallAttributes(final HierarchicalStreamWriter writer) {
        if (fixtureDefinition.getAttributeCount() > 0) {
            writer.startNode("attributes");
            for (int i = 0; i < fixtureDefinition.getAttributeCount(); i++) {
                AttributeDefinition attribute = fixtureDefinition.getAttribute(i);
                writer.startNode("attribute");
                writer.addAttribute("name", attribute.getName());
                writer.addAttribute("channels", attribute.getChannels());
                marshalAttributeValues(attribute, writer);
                writer.endNode();
            }
            writer.endNode();
        }
    }

    private void marshalAttributeValues(final AttributeDefinition attribute, final HierarchicalStreamWriter writer) {
        if (attribute.getValueCount() > 0) {
            writer.startNode("values");
            for (int i = 0; i < attribute.getValueCount(); i++) {
                AttributeValue value = attribute.getValue(i);
                writer.startNode("value");
                writer.addAttribute("name", value.getName());
                writer.addAttribute("from", Integer.toString(value.getFrom()));
                writer.addAttribute("to", Integer.toString(value.getTo()));
                writer.endNode();
            }
            writer.endNode();
        }
    }

    private void marshallPresets(final HierarchicalStreamWriter writer) {
        if (fixtureDefinition.getPresetCount() > 0) {
            writer.startNode("presets");
            for (int i = 0; i < fixtureDefinition.getPresetCount(); i++) {
                PresetDefinition preset = fixtureDefinition.getPreset(i);
                writer.startNode("preset");
                writer.addAttribute("name", preset.getName());
                marshalPresetValues(preset, writer);
                writer.endNode();
            }
            writer.endNode();
        }
    }

    private void marshalPresetValues(final PresetDefinition preset, final HierarchicalStreamWriter writer) {
        if (preset.getValueCount() > 0) {
            writer.startNode("values");
            for (int i = 0; i < preset.getValueCount(); i++) {
                PresetValueDefinition value = preset.getValue(i);
                writer.startNode("value");
                writer.addAttribute("name", value.getName());
                writer.addAttribute("value", value.getValue());
                writer.endNode();
            }
            writer.endNode();
        }
    }

    public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            if ("attributes".equals(reader.getNodeName())) {
                unmarshalAttributes(reader);
            }
            if ("presets".equals(reader.getNodeName())) {
                unmarshalPresets(reader);
            }
            reader.moveUp();
        }
        return fixtureDefinition;
    }

    private void unmarshalPresets(final HierarchicalStreamReader reader) {
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            String name = reader.getAttribute("name");
            PresetDefinition preset = fixtureDefinition.addPreset(name);
            if (reader.hasMoreChildren()) {
                reader.moveDown();
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    String valueName = reader.getAttribute("name");
                    String value = reader.getAttribute("value");
                    AttributeDefinition attributeDefinition = fixtureDefinition.getAttribute(valueName);
                    if (attributeDefinition == null) {
                        throw new InvalidFixtureDefinition(fixtureDefinition, "Unknown attribute \"" + valueName + "\" in preset \"" + name + "\"");
                    }
                    preset.add(attributeDefinition, value);
                    reader.moveUp();
                }
                reader.moveUp();
            }
            reader.moveUp();
        }
    }

    private void unmarshalAttributes(final HierarchicalStreamReader reader) {
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            String name = reader.getAttribute("name");
            String channels = reader.getAttribute("channels");
            AttributeDefinition attribute = fixtureDefinition.addAttribute(name, channels);
            if (reader.hasMoreChildren()) {
                reader.moveDown();
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    String valueName = reader.getAttribute("name");
                    String fromString = reader.getAttribute("from");
                    String toString = reader.getAttribute("to");
                    int from = Integer.parseInt(fromString);
                    int to = Integer.parseInt(toString);
                    attribute.addValue(valueName, from, to);
                    reader.moveUp();
                }
                reader.moveUp();
            }
            reader.moveUp();
        }
    }
}
