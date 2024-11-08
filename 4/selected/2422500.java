package be.lassi.domain;

import static be.lassi.util.Util.newArrayList;
import static be.lassi.util.Util.newHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.util.equals.EqualsTester;

/**
 * Represents a single fixture with a given fixture address and
 * current state of the fixture attributes.
 */
public class Fixture implements Iterable<Attribute> {

    private String name;

    /**
     * DMX address of the first channel in the fixture, valid values are
     * between 1 and 512 (1 <= value <= 512), a value of 0 means the
     * fixture is not patched.
     */
    private int address;

    private final FixtureDefinition definition;

    private boolean selected = false;

    private final List<Attribute> attributes = newArrayList();

    private final Map<String, FixtureChannel> channelMap = newHashMap();

    private final List<Preset> presets = newArrayList();

    /**
     * Constructs a new instance.
     *
     * @param definition the fixture definition
     * @param name the fixture name
     * @param address the fixture address (0 if not patched)
     */
    public Fixture(final FixtureDefinition definition, final String name, final int address) {
        this.definition = definition;
        this.name = name;
        this.address = address;
        initAttributes();
        updateChannelNumbers();
        initPresets();
    }

    /**
     * Constructs a new instance.
     *
     * @param definition the fixture definition.
     */
    public Fixture(final FixtureDefinition definition) {
        this(definition, "", 0);
    }

    /**
     * Sets the 'selected' indicator.
     *
     * @param selected true if this fixture is selected
     */
    public void setSelected(final boolean selected) {
        this.selected = selected;
    }

    /**
     * Gets the dmx channel number of the attribute with given name.
     *
     * @param string the attribute name
     * @return the dmx channel number
     */
    public int getChannelNumber(final String string) {
        int number = 0;
        FixtureChannel fc = channelMap.get(string);
        if (fc != null) {
            number = fc.getNumber();
        }
        return number;
    }

    /**
     * Gets the fixture definition.
     *
     * @return the fixture definition
     */
    public FixtureDefinition getDefinition() {
        return definition;
    }

    /**
     * Indicates whether this fixture is selected.
     *
     * @return true if selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Gets the fixture name.
     *
     * @return the fixture name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the fixture name.
     *
     * @param name the fixture name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the fixture dmx address.
     *
     * @return the fixture dmx address, 0 if the fixture is not patched
     */
    public int getAddress() {
        return address;
    }

    /**
     * Sets the fixture dmx address.
     *
     * @param address the fixture dmx address, 0 if the fixture is not patched
     */
    public void setAddress(final int address) {
        this.address = address;
        updateChannelNumbers();
    }

    /**
     * Gets the fixture attribute at given index.
     *
     * @param index the attribute index
     * @return the attribute at given index
     */
    public Attribute getAttribute(final int index) {
        return attributes.get(index);
    }

    private void initAttributes() {
        for (int i = 0; i < definition.getAttributeCount(); i++) {
            AttributeDefinition attributeDefinition = definition.getAttribute(i);
            Attribute attribute = new Attribute(attributeDefinition);
            attributes.add(attribute);
            for (int j = 0; j < attribute.getChannelCount(); j++) {
                FixtureChannel channel = attribute.getChannel(j);
                channelMap.put(channel.getName(), channel);
            }
        }
    }

    /**
     * Initializes the fixture presets.
     */
    public void initPresets() {
        for (int i = 0; i < definition.getPresetCount(); i++) {
            PresetDefinition presetDefinition = definition.getPreset(i);
            Preset preset = new Preset(presetDefinition);
            initPreset(preset);
            presets.add(preset);
        }
    }

    private void initPreset(final Preset preset) {
        PresetDefinition presetDefinition = preset.getDefinition();
        for (int i = 0; i < presetDefinition.getValueCount(); i++) {
            PresetValueDefinition presetValueDefinition = presetDefinition.getValue(i);
            String attributeName = presetValueDefinition.getName();
            Attribute attribute = getAttribute(attributeName);
            if (attribute != null) {
                initPresetValue(preset, presetValueDefinition, attribute);
            }
        }
    }

    private void initPresetValue(final Preset preset, final PresetValueDefinition presetValueDefinition, final Attribute attribute) {
        int[] values = presetValueDefinition.getValues(attribute.getChannelCount());
        for (int i = 0; i < values.length; i++) {
            FixtureChannel channel = attribute.getChannel(i);
            PresetValue presetValue = new PresetValue(channel, values[i]);
            preset.addValue(presetValue);
        }
    }

    private void updateChannelNumbers() {
        for (Attribute attribute : attributes) {
            for (int i = 0; i < attribute.getChannelCount(); i++) {
                FixtureChannel channel = attribute.getChannel(i);
                int channelNumber = 0;
                if (address != 0) {
                    channelNumber = channel.getOffset() + address - 1;
                }
                channel.setNumber(channelNumber);
            }
        }
    }

    /**
     * Gets the attribute with given name.
     *
     * @param attributeName the attribute name
     * @return the attribute with given name
     */
    public Attribute getAttribute(final String attributeName) {
        Attribute result = null;
        for (int i = 0; i < attributes.size(); i++) {
            Attribute attribute = attributes.get(i);
            if (attributeName.equals(attribute.getDefinition().getName())) {
                result = attribute;
            }
        }
        return result;
    }

    /**
     * Gets the number of attributes.
     *
     * @return the attribute count
     */
    public int getAttributeCount() {
        return attributes.size();
    }

    /**
     * Gets the highest dmx channel number that is used by this fixture.
     *
     * @return the highest dmx channel number
     */
    public int getMaxChannelNumber() {
        int max = 0;
        for (int i = 0; i < attributes.size(); i++) {
            Attribute attribute = attributes.get(i);
            max = Math.max(max, attribute.getMaxChannelNumber());
        }
        return max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getName() + "(" + name + ")";
    }

    /**
     * Gets the preset with given name.
     *
     * @param presetName the preset name
     * @return the preset with given name
     */
    public Preset getPreset(final String presetName) {
        Preset result = null;
        for (int i = 0; i < presets.size(); i++) {
            Preset preset = presets.get(i);
            if (preset.getDefinition().getName().equals(presetName)) {
                result = preset;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "BC" })
    public boolean equals(final Object object) {
        boolean result = this == object;
        if (!result) {
            EqualsTester tester = EqualsTester.get(this, object);
            if (tester.isEquals()) {
                Fixture other = (Fixture) object;
                tester.test(address, other.address);
                tester.test(attributes, other.attributes);
                tester.test(channelMap, other.channelMap);
                tester.test(definition, other.definition);
                tester.test(name, other.name);
                tester.test(presets, other.presets);
                tester.test(selected, other.selected);
            }
            result = tester.isEquals();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder(-91100963, 1075437485);
        b.append(address);
        b.append(attributes);
        b.append(channelMap);
        b.append(definition);
        b.append(name);
        b.append(presets);
        b.append(selected);
        return b.toHashCode();
    }

    /**
     * Gets the index of given attribute.
     *
     * @param attribute the attribute for which to return the index
     * @return the attribute index
     */
    public int indexOfAttribute(final Attribute attribute) {
        return attributes.indexOf(attribute);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Attribute> iterator() {
        return attributes.iterator();
    }
}
