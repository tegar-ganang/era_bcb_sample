package be.lassi.domain;

import static be.lassi.util.Util.newArrayList;
import java.util.List;
import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.base.Dirty;
import be.lassi.base.DirtyStub;
import be.lassi.base.SaveableObject;
import be.lassi.util.equals.EqualsTester;

public class AttributeDefinition extends SaveableObject {

    private String name;

    private String channels;

    private final List<AttributeValue> values = newArrayList();

    public AttributeDefinition() {
        this("", "");
    }

    public AttributeDefinition(final String name, final String channels) {
        this(new DirtyStub(), name, channels);
    }

    public AttributeDefinition(final Dirty dirty, final String name, final String channels) {
        super(dirty);
        this.name = name;
        this.channels = channels;
    }

    public void addValue(final String valueName, final int from, final int to) {
        AttributeValue value = new AttributeValue(valueName, from, to);
        value.setDirty(getDirty());
        values.add(value);
        markDirty();
    }

    public String getName() {
        return name;
    }

    public String getChannels() {
        return channels;
    }

    public void setName(final String name) {
        this.name = name;
        markDirty();
    }

    public void setChannels(final String channels) {
        this.channels = channels;
        markDirty();
    }

    public int getValueCount() {
        return values.size();
    }

    public AttributeValue getValue(final int index) {
        return values.get(index);
    }

    public String getValueOf(final int dmxValue) {
        String result = null;
        for (int i = 0; result == null && i < getValueCount(); i++) {
            AttributeValue value = getValue(i);
            if (value.getFrom() <= dmxValue && value.getTo() >= dmxValue) {
                result = value.getName();
            }
        }
        if (result == null) {
            result = "???";
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
                AttributeDefinition other = (AttributeDefinition) object;
                tester.test(name, other.name);
                tester.test(channels, other.channels);
                tester.test(values, other.values);
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
        HashCodeBuilder b = new HashCodeBuilder(589094935, 1582043635);
        b.append(name);
        b.append(channels);
        b.append(values);
        return b.toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(getClass().getName());
        b.append("(");
        b.append(name);
        b.append(")");
        String string = b.toString();
        return string;
    }

    public void removeValue(final int index) {
        values.remove(index);
        markDirty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDirty(final Dirty dirty) {
        super.setDirty(dirty);
        for (AttributeValue value : values) {
            value.setDirty(dirty);
        }
    }
}
