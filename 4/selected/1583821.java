package be.lassi.ui.patch;

import static be.lassi.util.Util.newArrayList;
import java.util.List;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import be.lassi.base.Listener;
import be.lassi.context.ShowContext;
import be.lassi.context.ShowContextListener;
import be.lassi.domain.Channel;
import be.lassi.domain.Dimmer;
import be.lassi.domain.Dimmers;
import be.lassi.lanbox.commands.Command;
import be.lassi.lanbox.commands.CommandListener;
import be.lassi.lanbox.commands.CommonSetPatch;
import be.lassi.lanbox.commands.layer.LayerSetFadeTime;
import be.lassi.lanbox.commands.layer.LayerSetFadeType;
import be.lassi.lanbox.domain.ChannelChange;
import be.lassi.lanbox.domain.FadeType;
import be.lassi.lanbox.domain.LayerId;
import be.lassi.lanbox.domain.PatchParameters;
import be.lassi.lanbox.domain.Time;
import be.lassi.util.Dmx;
import be.lassi.util.Wait;

/**
 *
 */
public class Patcher implements ShowContextListener {

    /**
     * Destination for log messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(Patcher.class);

    /**
     *
     *
     *
     */
    public static final int PRE_PATCH_START = Dmx.NEW_MAX_CHANNELS / 2;

    private final ShowContext context;

    private Time fadeTime;

    private int layerNumber = 2;

    private final Patch patch;

    /**
     * Indicates whether any patch changes have to be communicated to
     * the Lanbox immediately.
     */
    private boolean updateLanbox;

    private boolean prePatchInitialized;

    private boolean fadeTimeInitialized;

    /**
     * Constructs a new instance.
     *
     * @param context the show context
     * @param patch the patch information
     */
    public Patcher(final ShowContext context, final Patch patch) {
        this.context = context;
        this.patch = patch;
        PatchPreferences preferences = context.getPreferences().getPatch();
        layerNumber = preferences.getLayerId();
        fadeTime = preferences.getFadeTime();
        context.addShowContextListener(this);
    }

    /**
     * Initializes the patcher.
     */
    public void initialize() {
        initializeFadeTime(layerNumber);
        initializeLevels();
        initializePrePatch();
    }

    private void initializeLevels() {
        int dimmerCount = context.getShow().getDimmers().size();
        List<ChannelChange> changes = newArrayList();
        for (int i = 0; i < dimmerCount; i++) {
            ChannelChange change = new ChannelChange(i + 1, 0);
            changes.add(change);
            if (dimmerCount <= PRE_PATCH_START) {
                change = new ChannelChange(PRE_PATCH_START + i + 1, 0);
                changes.add(change);
            }
        }
        context.getLanbox().getEngine().change(layerNumber, changes);
    }

    /**
     * Fades out all dimmers.
     */
    public void blackout() {
        for (int i = 0; i < patch.getDimmerCount(); i++) {
            setOn(i, false);
        }
    }

    /**
     * Unpatches all dimmers (asynchronous: does not wait until done).
     */
    public void clearPatch() {
        clearPatch(null);
    }

    /**
     * Unpatches all dimmers (asynchronous: does not wait until done).
     *
     * @param listener listener that gets notified when uppatch command completed,
     *                 null if nobody needs to be notified
     */
    public void clearPatch(final Listener listener) {
        patch.clearPatch();
        updatePatch(listener);
    }

    private void updatePatch(final Listener listener) {
        PatchParameters parameters = new PatchParameters();
        Dimmers dimmers = context.getShow().getDimmers();
        for (int i = 0; i < dimmers.size(); i++) {
            parameters.add(i, channelIndex(dimmers.get(i)));
        }
        patch(parameters, listener);
    }

    /**
     * Unpatches all dimmers, and waits until done.
     */
    public void clearPatchAndWait() {
        final Wait wait = new Wait();
        clearPatch(new Listener() {

            public void changed() {
                wait.stop();
            }
        });
        wait.here();
    }

    /**
     * Copies the channel names to the dimmers that are patched to these channels.
     */
    public void copyChannelNamesToDimmers() {
        patch.copyChannelNamesToDimmers();
    }

    /**
     * Copies the dimmer names to the channels that are patched to these dimmers.
     */
    public void copyDimmerNamesToChannels() {
        patch.copyDimmerNamesToChannels();
    }

    /**
     * Patches dimmers to channels 1-to-1 (asynchronous: does not wait until done).
     */
    public void defaultPatch() {
        defaultPatch(null);
    }

    /**
     * Patches dimmers to channels 1-to-1 (asynchronous: does not wait until done).
     *
     * @param listener listener that gets notified when uppatch command completed,
     *                 null if nobody needs to be notified
     */
    public void defaultPatch(final Listener listener) {
        patch.defaultPatch();
        updatePatch(listener);
    }

    /**
     * Patches dimmers to channels 1-to-1, and waits until done.
     */
    public void defaultPatchAndWait() {
        if (context.getShow().getNumberOfChannels() > 0) {
            final Wait wait = new Wait();
            defaultPatch(new Listener() {

                public void changed() {
                    wait.stop();
                }
            });
            wait.here();
        }
    }

    /**
     * Reads the patch from the lanbox (asynchronous: do not wait for result here).
     */
    public void loadPatch() {
        loadPatch(null, true);
    }

    /**
     * Reads the patch from the lanbox (asynchronous: do not wait for result here).
     *
     * @param listener the listener to be notified when the patch is loaded
     * @param updateChannels true if the dimmer channels have to be updated
     */
    public void loadPatch(final Listener listener, final boolean updateChannels) {
        new GetPatch(context, listener, updateChannels).execute();
    }

    /**
     * Reads the patch from the lanbox, and waits for the response.
     *
     * @param updateChannels true if the dimmer channels have to be updated
     */
    public void loadPatchAndWait(final boolean updateChannels) {
        if (context.getConnectionStatus().isOpen()) {
            final Wait wait = new Wait();
            loadPatch(new Listener() {

                public void changed() {
                    wait.stop();
                }
            }, updateChannels);
            wait.here();
        }
    }

    /**
     * Patches a number of channels to dimmers.
     *
     * @param patchDetailIndexes the dimmers to patch
     * @param channels the channels to patch
     * @param listener the listener to be notified when the patch is done,
     *                 null if nobody needs to be notified
     */
    public void patch(final int[] patchDetailIndexes, final List<Channel> channels, final Listener listener) {
        PatchParameters parameters = new PatchParameters();
        if (channels.size() == 1) {
            Channel channel = channels.get(0);
            for (int index : patchDetailIndexes) {
                Dimmer dimmer = patch.getDetail(index).getDimmer();
                dimmer.setChannel(channel);
                parameters.add(dimmer.getId(), channelIndex(dimmer));
            }
        } else {
            for (int i = 0; i < channels.size(); i++) {
                int dimmerIndex = patchDetailIndexes[0] + i;
                if (dimmerIndex < patch.getDimmerCount()) {
                    Dimmer dimmer = patch.getDetail(dimmerIndex).getDimmer();
                    Channel channel = channels.get(i);
                    dimmer.setChannel(channel);
                    parameters.add(dimmer.getId(), channel.getId());
                }
            }
        }
        patch(parameters, listener);
    }

    /**
     * Patches channels to dimmers.
     *
     * @param patchDetailIndexes the indexes of the dimmers to patch to
     * @param channels the channels to be patched
     */
    public void patchAndWait(final int[] patchDetailIndexes, final List<Channel> channels) {
        final Wait wait = new Wait();
        patch(patchDetailIndexes, channels, new Listener() {

            public void changed() {
                wait.stop();
            }
        });
        wait.here();
    }

    /**
     * Unpatches the dimmers at given indexes.
     *
     * @param patchDetailIndexes the indexes of the dimmers to be unpatched
     * @param listener the listener to be notified when the unpatch is complete
     */
    public void unpatch(final int[] patchDetailIndexes, final Listener listener) {
        PatchParameters parameters = new PatchParameters();
        for (int index : patchDetailIndexes) {
            Dimmer dimmer = patch.getDetail(index).getDimmer();
            dimmer.setChannel(null);
            int lanboxChannelId = channelIndex(dimmer);
            dimmer.setLanboxChannelId(lanboxChannelId);
            parameters.add(dimmer.getId(), lanboxChannelId);
        }
        patch(parameters, listener);
    }

    /**
     * Unpatches the dimmers at given indexes.
     *
     * @param patchDetailIndexes the indexes of the dimmers to be unpatched
     */
    public void unpatchAndWait(final int[] patchDetailIndexes) {
        final Wait wait = new Wait();
        unpatch(patchDetailIndexes, new Listener() {

            public void changed() {
                wait.stop();
            }
        });
        wait.here();
    }

    /**
     * {@inheritDoc}
     */
    public void postShowChange() {
        initializePrePatch();
        initializeFadeTime(layerNumber);
    }

    /**
     * {@inheritDoc}
     */
    public void preShowChange() {
    }

    /**
     * Saves the show patch information to the lanbox.
     */
    public void savePatch() {
        savePatch(null);
    }

    /**
     * Saves the show patch information to the lanbox.
     *
     * @param listener the listener that needs to be notified when the save is complete
     */
    public void savePatch(final Listener listener) {
        PatchParameters parameters = new PatchParameters();
        Dimmers dimmers = context.getShow().getDimmers();
        for (int i = 0; i < dimmers.size(); i++) {
            Dimmer dimmer = dimmers.get(i);
            parameters.add(i, dimmer.getChannelId());
        }
        patch(parameters, true, listener);
    }

    /**
     * Saves the show patch information to the lanbox.
     */
    public void savePatchAndWait() {
        final Wait wait = new Wait();
        savePatch(new Listener() {

            public void changed() {
                wait.stop();
            }
        });
        wait.here();
    }

    /**
     * Sets the fade time that is used for fading dimmers in and out.
     *
     * @param fadeTime the fade time to be set
     */
    public void setFadeTime(final Time fadeTime) {
        PatchPreferences preferences = context.getPreferences().getPatch();
        preferences.setFadeTimeString(fadeTime.getString());
        this.fadeTime = fadeTime;
        initializeFadeTime(layerNumber);
    }

    /**
     * Sets the Lanbox layer that is used for fading dimmers in and out.
     *
     * @param newLayerNumber the new layer number
     */
    public void setLayerNumber(final int newLayerNumber) {
        PatchPreferences preferences = context.getPreferences().getPatch();
        preferences.setLayer(LayerId.get(newLayerNumber));
        if (patch.isDimmerOn()) {
            if (!fadeTimeInitialized) {
                initializeFadeTime(layerNumber);
            }
            initializeFadeTime(newLayerNumber);
            List<PatchDetail> details = patch.getDetails();
            for (int i = 0; i < details.size(); i++) {
                if (details.get(i).isOn()) {
                    int channelId = details.get(i).getDimmer().getLanboxChannelId();
                    ChannelChange off = new ChannelChange(channelId + 1, 0);
                    ChannelChange on = new ChannelChange(channelId + 1, 255);
                    context.getLanbox().getEngine().change(layerNumber, off);
                    context.getLanbox().getEngine().change(newLayerNumber, on);
                }
            }
        }
        layerNumber = newLayerNumber;
    }

    /**
     * Switches dimmer at given index on or off.
     *
     * @param row index of dimmer to be switched on or off
     * @param on true if on
     */
    public void setOn(final int row, final boolean on) {
        if (!prePatchInitialized) {
            initializePrePatch();
        }
        if (!fadeTimeInitialized) {
            initializeFadeTime(layerNumber);
        }
        PatchDetail detail = patch.getDetail(row);
        int channelIndex = detail.getDimmer().getLanboxChannelId();
        int dmxValue = on ? Dmx.NEW_MAX : 0;
        if (channelIndex >= 0) {
            ChannelChange change = new ChannelChange(channelIndex + 1, dmxValue);
            context.getLanbox().getEngine().change(layerNumber, change);
        }
        detail.setOn(on);
    }

    /**
     * Gets the channelIndex that can be used to fade in the dimmer
     * in given patch details.
     *
     * @param detail the dimmer patch details
     * @return the channelIndex to fade in given dimmer, -1 if no channel available
     */
    private int channelIndex(final Dimmer dimmer) {
        int channelIndex = -1;
        if (dimmer.getChannel() != null) {
            channelIndex = dimmer.getChannel().getId();
        } else if (isPrePatch()) {
            channelIndex = dimmer.getId() + PRE_PATCH_START;
        }
        return channelIndex;
    }

    private void initializeFadeTime(final int layer) {
        Command command = new LayerSetFadeTime(layer, fadeTime);
        context.getLanbox().getEngine().execute(command);
        command = new LayerSetFadeType(layer, FadeType.CROSS_FADE);
        context.getLanbox().getEngine().execute(command);
        fadeTimeInitialized = true;
    }

    private void initializePrePatch() {
        if (isPrePatch() && patch.getDimmerCount() > 0) {
            LOGGER.debug("Pre patch load");
            loadPatchAndWait(false);
            LOGGER.debug("Post patch load");
            PatchParameters parameters = new PatchParameters();
            Dimmers dimmers = context.getShow().getDimmers();
            for (int i = 0; i < dimmers.size(); i++) {
                Dimmer dimmer = dimmers.get(i);
                if (dimmer.getLanboxChannelId() == -1) {
                    parameters.add(i, PRE_PATCH_START + i);
                }
            }
            patch(parameters, true, null);
        }
        prePatchInitialized = true;
    }

    /**
     * Indicates whether the upper half of the channels can be used to fade in
     * the dimmers, even if they are not patched yet.
     *
     * @return true if upper half of channels is available
     */
    private boolean isPrePatch() {
        int dimmerCount = context.getShow().getDimmers().size();
        return dimmerCount <= PRE_PATCH_START;
    }

    private void patch(final PatchParameters parameters, final Listener listener) {
        patch(parameters, updateLanbox, listener);
    }

    private void patch(final PatchParameters parameters, final boolean update, final Listener listener) {
        if (update) {
            for (int i = 0; i < parameters.size(); i++) {
                int dimmerId = parameters.getDimmerId(i);
                int channelId = parameters.getChannelId(i);
                if (channelId == -1) {
                    if (isPrePatch()) {
                        channelId = dimmerId + PRE_PATCH_START;
                    }
                }
                Dimmer dimmer = context.getShow().getDimmers().get(dimmerId);
                dimmer.setLanboxChannelId(channelId);
            }
            PatchParameters[] pp = parameters.split();
            for (int i = 0; i < pp.length; i++) {
                Command command = new CommonSetPatch(pp[i]);
                if (i == pp.length - 1 && listener != null) {
                    command.add(new CommandListener() {

                        public void commandPerformed(final Command c) {
                            if (listener != null) {
                                listener.changed();
                            }
                        }
                    });
                }
                context.getLanbox().execute(command);
            }
        } else {
            if (listener != null) {
                listener.changed();
            }
        }
    }

    /**
     * Indicates whether any patch changes have to be communicated to
     * the Lanbox immediately.
     *
     * @return true true if the Lanbox kept needs to be updated immediately
     */
    public boolean isUpdateLanbox() {
        return updateLanbox;
    }

    /**
     * Sets the 'updatelanbox' indicator.
     *
     * @param updateLanbox true if the Lanbox kept needs to be updated immediately
     */
    public void setUpdateLanbox(final boolean updateLanbox) {
        this.updateLanbox = updateLanbox;
    }
}
