package be.lassi.ui.patch;

import static be.lassi.util.Util.newArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import be.lassi.context.ShowContext;
import be.lassi.context.ShowContextListener;
import be.lassi.domain.Channel;
import be.lassi.domain.Channels;
import be.lassi.domain.Dimmer;
import be.lassi.domain.Dimmers;

/**
 *
 *
 *
 */
public class Patch implements ShowContextListener {

    private final ShowContext context;

    /** @aggregation PatchDetail */
    private final List<PatchDetail> patchDetails = newArrayList();

    private int sortColumn = PatchDetail.DIMMER_NUMBER;

    /**
     * Constructs a new instance.
     *
     * @param context the show context
     */
    public Patch(final ShowContext context) {
        this.context = context;
        postShowChange();
        context.addShowContextListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public void preShowChange() {
        patchDetails.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void postShowChange() {
        Dimmers dimmers = context.getShow().getDimmers();
        for (int i = 0; i < dimmers.size(); i++) {
            Dimmer dimmer = dimmers.get(i);
            patchDetails.add(new PatchDetail(dimmer));
        }
        sort();
    }

    /**
     * Gets the index of the column on which the patch details are sorted.
     *
     * @return the sort column index
     */
    public int getSortColumn() {
        return sortColumn;
    }

    /**
     * Sets the column on which the sort the patch details.
     *
     * @param sortColumn the column to set
     */
    public void setSortColumn(final int sortColumn) {
        this.sortColumn = sortColumn;
        sort();
    }

    /**
     * Sets the default patch (channels are patched to dimmers with
     * same number).
     */
    public void defaultPatch() {
        Dimmers dimmers = context.getShow().getDimmers();
        Channels channels = context.getShow().getChannels();
        for (int i = 0; i < dimmers.size(); i++) {
            Channel channel = null;
            if (i < channels.size()) {
                channel = channels.get(i);
            }
            dimmers.get(i).setChannel(channel);
        }
    }

    /**
     * Removes the patch information from all dimmers: sets all
     * dimmers to "not patched".
     */
    public void clearPatch() {
        Dimmers dimmers = context.getShow().getDimmers();
        for (int i = 0; i < dimmers.size(); i++) {
            dimmers.get(i).setChannel(null);
        }
    }

    /**
     * Sets the dimmer names to the names of the channels that are patched to
     * the dimmers, the names of the "not patched" dimmers are not touched.
     */
    public void copyChannelNamesToDimmers() {
        Dimmers dimmers = context.getShow().getDimmers();
        for (int i = 0; i < dimmers.size(); i++) {
            Dimmer dimmer = dimmers.get(i);
            Channel channel = dimmer.getChannel();
            if (channel != null) {
                dimmer.setName(channel.getName());
            }
        }
    }

    /**
     * Sets the names of the channels to the names of names of the
     * the dimmers that they are patched to.  If the channel is patched
     * to multiple dimmers, the channel gets the name of the dimmer
     * with the highest id.
     */
    public void copyDimmerNamesToChannels() {
        Dimmers dimmers = context.getShow().getDimmers();
        for (int i = 0; i < dimmers.size(); i++) {
            Dimmer dimmer = dimmers.get(i);
            Channel channel = dimmer.getChannel();
            if (channel != null) {
                channel.setName(dimmer.getName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getDimmerCount() {
        return patchDetails.size();
    }

    /**
     * Gets the patch detail at given index.
     *
     * @param index the patch detail index
     * @return the patch detail at given index
     */
    public PatchDetail getDetail(final int index) {
        return patchDetails.get(index);
    }

    /**
     * Sorts the patch details.
     */
    public void sort() {
        switch(sortColumn) {
            case PatchDetail.ON:
                sortOnDimmerOn();
                break;
            case PatchDetail.DIMMER_NUMBER:
                sortOnDimmerNumber();
                break;
            case PatchDetail.DIMMER_NAME:
                sortOnDimmerName();
                break;
            case PatchDetail.LANBOX_CHANNEL_NUMBER:
                sortOnLanboxChannelNumber();
                break;
            case PatchDetail.CHANNEL_NUMBER:
                sortOnChannelNumber();
                break;
            case PatchDetail.CHANNEL_NAME:
                sortOnChannelName();
                break;
            default:
                throw new IllegalArgumentException("Invalid sort column index: " + sortColumn);
        }
    }

    private void sortOnDimmerOn() {
        Collections.sort(patchDetails, new Comparator<PatchDetail>() {

            public int compare(final PatchDetail pd1, final PatchDetail pd2) {
                int result = 0;
                if (pd1.isOn()) {
                    if (!pd2.isOn()) {
                        result = -1;
                    }
                } else {
                    if (pd2.isOn()) {
                        result = 1;
                    }
                }
                if (result == 0) {
                    result = pd1.getDimmer().getId() - pd2.getDimmer().getId();
                }
                return result;
            }
        });
    }

    private void sortOnDimmerNumber() {
        Collections.sort(patchDetails, new Comparator<PatchDetail>() {

            public int compare(final PatchDetail pd1, final PatchDetail pd2) {
                return pd1.getDimmer().getId() - pd2.getDimmer().getId();
            }
        });
    }

    private void sortOnDimmerName() {
        Collections.sort(patchDetails, new Comparator<PatchDetail>() {

            public int compare(final PatchDetail pd1, final PatchDetail pd2) {
                int result = pd1.getDimmer().getName().compareTo(pd2.getDimmer().getName());
                if (result == 0) {
                    result = pd1.getDimmer().getId() - pd2.getDimmer().getId();
                }
                return result;
            }
        });
    }

    /**
     * Sorts on lanbox channel number, then on dimmer id; unpatched dimmers come last.
     */
    private void sortOnLanboxChannelNumber() {
        Collections.sort(patchDetails, new Comparator<PatchDetail>() {

            public int compare(final PatchDetail pd1, final PatchDetail pd2) {
                Dimmer dimmer1 = pd1.getDimmer();
                Dimmer dimmer2 = pd2.getDimmer();
                int result = 0;
                if (!dimmer1.isPatched()) {
                    if (dimmer2.isPatched()) {
                        result = 1;
                    }
                } else if (!dimmer2.isPatched()) {
                    result = -1;
                } else {
                    result = dimmer1.getLanboxChannelId() - dimmer2.getLanboxChannelId();
                }
                if (result == 0) {
                    result = dimmer1.getId() - dimmer2.getId();
                }
                return result;
            }
        });
    }

    /**
     * Sorts on channel number, then on dimmer id; unpatched dimmers come last.
     */
    private void sortOnChannelNumber() {
        Collections.sort(patchDetails, new Comparator<PatchDetail>() {

            public int compare(final PatchDetail pd1, final PatchDetail pd2) {
                Dimmer dimmer1 = pd1.getDimmer();
                Dimmer dimmer2 = pd2.getDimmer();
                int result = 0;
                if (!dimmer1.isPatched()) {
                    if (dimmer2.isPatched()) {
                        result = 1;
                    }
                } else if (!dimmer2.isPatched()) {
                    result = -1;
                } else {
                    result = dimmer1.getChannelId() - dimmer2.getChannelId();
                }
                if (result == 0) {
                    result = dimmer1.getId() - dimmer2.getId();
                }
                return result;
            }
        });
    }

    /**
     * Sorts on channel name, then on dimmer id; unpatched dimmers come last.
     */
    private void sortOnChannelName() {
        Collections.sort(patchDetails, new Comparator<PatchDetail>() {

            public int compare(final PatchDetail pd1, final PatchDetail pd2) {
                Dimmer dimmer1 = pd1.getDimmer();
                Dimmer dimmer2 = pd2.getDimmer();
                int result = 0;
                if (!dimmer1.isPatched()) {
                    if (dimmer2.isPatched()) {
                        result = 1;
                    }
                } else if (!dimmer2.isPatched()) {
                    result = -1;
                } else {
                    String name1 = dimmer1.getChannel().getName();
                    String name2 = dimmer2.getChannel().getName();
                    result = name1.compareTo(name2);
                }
                if (result == 0) {
                    result = dimmer1.getId() - dimmer2.getId();
                }
                return result;
            }
        });
    }

    /**
     * Indicates wheter one of the dimmers is currently on.
     *
     * @return true if one of the dimmers is currently on
     */
    public boolean isDimmerOn() {
        boolean result = false;
        for (int i = 0; !result && i < patchDetails.size(); i++) {
            result = patchDetails.get(i).isOn();
        }
        return result;
    }

    /**
     * Gets the patch details.
     *
     * @return the patch details
     */
    public List<PatchDetail> getDetails() {
        return patchDetails;
    }
}
