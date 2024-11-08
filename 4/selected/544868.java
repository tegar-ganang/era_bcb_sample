package be.lassi.domain;

import static be.lassi.util.Util.newArrayList;
import java.util.Iterator;
import java.util.List;
import be.lassi.base.Dirty;
import be.lassi.base.DirtyStub;
import be.lassi.base.NamedObject;
import be.lassi.base.NamedObjects;
import be.lassi.util.equals.EqualsTester;

/**
 * Collection with <code>Dimmer</code> objects.
 */
public class Dimmers extends NamedObjects implements Iterable<Dimmer> {

    /**
     * The actual dimmer collection that is wrapped by this class.
     * @aggregation Dimmer
     */
    private final List<Dimmer> dimmers = newArrayList();

    /**
     * Constructs a new instance.
     */
    public Dimmers() {
        this(new DirtyStub());
    }

    /**
     * Constructs a new instance.
     *
     * @param dirty the dirty indicator
     */
    public Dimmers(final Dirty dirty) {
        super(dirty);
    }

    /**
     * Adds a dimmer.
     *
     * @param dimmer the dimmer to be added
     */
    public void add(final Dimmer dimmer) {
        dimmers.add(dimmer);
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
                Dimmers other = (Dimmers) object;
                tester.test(dimmers, other.dimmers);
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
        return dimmers.hashCode();
    }

    /**
     * Gets the dimmer with given index.
     *
     * @param index the dimmer index
     * @return the dimmer at given index
     */
    public Dimmer get(final int index) {
        return dimmers.get(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NamedObject getNamedObject(final int index) {
        return get(index);
    }

    /**
     * Gets the level value of dimmer with given index.
     *
     * @param index the dimmer index
     * @return the level value of dimmer at given index
     */
    public float getValue(final int index) {
        return get(index).getValue();
    }

    /**
     * Set level value of all dimmers that are patched to channel with given index.
     *
     * @param channelIndex the channel index
     * @param value the level value to set
     */
    public void setValue(final int channelIndex, final float value) {
        for (int i = 0; i < size(); i++) {
            Dimmer dimmer = get(i);
            if (dimmer.getChannelId() == channelIndex) {
                dimmer.setValue(value);
            }
        }
        doNotMarkDirty();
    }

    /**
     * The number of dimmers.
     *
     * @return the number of dimmers
     */
    @Override
    public int size() {
        return dimmers.size();
    }

    public Iterator<Dimmer> iterator() {
        return dimmers.iterator();
    }
}
