package be.lassi.lanbox.domain;

import static be.lassi.util.Util.newArrayList;
import java.util.List;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import be.lassi.util.Util;

/**
 * A collection of dimmer/channel pairs for use as parameters
 * with the <code>CommonSetPatch</code> Lanbox command.
 *
 */
public class PatchParameters {

    private final List<PatchPair> pairs = newArrayList();

    /**
     * Adds a new patch pair.
     *
     * @param dimmerId the dimmer identifier
     * @param channelId the channel identifier, a value of -1 means dimmer is not patched
     */
    public void add(final int dimmerId, final int channelId) {
        Util.validate("dimmerId", dimmerId, 0, 511);
        Util.validate("channelId", channelId, -1, 511);
        PatchPair pair = new PatchPair(dimmerId, channelId);
        pairs.add(pair);
    }

    /**
     * Gets the number of dimmer/channel pairs.
     *
     * @return the number of dimmer/channel pairs
     */
    public int size() {
        return pairs.size();
    }

    /**
     * Gets the identifier of the channel at given index.
     *
     * @param index the index in the patch pair collection
     * @return the channel id, a value of -1 means dimmer is not patched
     */
    public int getChannelId(final int index) {
        return pairs.get(index).getChannelId();
    }

    /**
     * Gets the identifier of the dimmer at given index.
     *
     * @param index the index in the patch pair collection
     * @return the dimmer id
     */
    public int getDimmerId(final int index) {
        return pairs.get(index).getDimmerId();
    }

    /**
     * Splits this parameters object in multiple parameter objects if
     * the maximum number of dimmer/channel pairs is exceeded.
     *
     * @return the splitted parameters
     */
    public PatchParameters[] split() {
        int count = 0;
        if (size() > 0) {
            count = ((size() - 1) / 255) + 1;
        }
        PatchParameters[] result = new PatchParameters[count];
        if (count == 1) {
            result[0] = this;
        } else {
            int index = 0;
            for (int i = 0; i < count; i++) {
                result[i] = new PatchParameters();
                for (int j = 0; j < 255 && index < size(); j++, index++) {
                    result[i].pairs.add(pairs.get(index));
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        boolean result = object == this;
        if (!result && object instanceof PatchParameters) {
            PatchParameters other = (PatchParameters) object;
            EqualsBuilder b = new EqualsBuilder();
            b.append(pairs, other.pairs);
            result = b.isEquals();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder b = new HashCodeBuilder(-363444041, -1700077223);
        b.append(pairs);
        return b.toHashCode();
    }
}
