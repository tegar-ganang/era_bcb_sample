package be.lassi.domain;

import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.util.equals.EqualsTester;

public class PresetValue {

    private final FixtureChannel channel;

    private final int value;

    public PresetValue(final FixtureChannel channel, final int value) {
        this.channel = channel;
        this.value = value;
    }

    public FixtureChannel getChannel() {
        return channel;
    }

    public int getValue() {
        return value;
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
                PresetValue other = (PresetValue) object;
                tester.test(channel, other.channel);
                tester.test(value, other.value);
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
        HashCodeBuilder b = new HashCodeBuilder(2008516407, 1723876407);
        b.append(channel);
        b.append(value);
        return b.toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(getClass().getName());
        b.append("(name=");
        b.append(channel.getName());
        b.append(", value=");
        b.append(value);
        b.append(")");
        String string = b.toString();
        return string;
    }
}
