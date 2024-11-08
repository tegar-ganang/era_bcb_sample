package be.lassi.ui.patch;

import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ListSelectionModel;
import be.lassi.base.Listener;
import be.lassi.context.ShowContext;
import be.lassi.domain.Channel;
import be.lassi.ui.util.table.RowSelectionModel;

/**
 * The model that supports the patch frame; includes the models for the
 * patch detail table and the channel table.
 */
public class PatchModel implements Listener {

    private Patch patch;

    private Patcher patcher;

    private PatchDetailTableModel detailTableModel;

    private PatchChannelTableModel channelTableModel;

    private RowSelectionModel detailSelectionModel;

    private RowSelectionModel channelSelectionModel;

    /**
     * Indicates whether the lights on the stage follow the selection
     * in the list of patch details.
     */
    private boolean stageFollowsSelection;

    /**
     * Constructs a new instance.
     * 
     * @param context the show context
     */
    public PatchModel(final ShowContext context) {
        patch = new Patch(context);
        patcher = new Patcher(context, patch);
        detailTableModel = new PatchDetailTableModel(context, patcher, patch);
        channelTableModel = new PatchChannelTableModel(context);
        detailSelectionModel = new RowSelectionModel();
        channelSelectionModel = new RowSelectionModel();
    }

    /**
     * Indicates whether the Lanbox patch is updated immediately.
     * 
     * @return true if the Lanbox patch is updated immediately
     */
    public boolean isUpdateLanbox() {
        return patcher.isUpdateLanbox();
    }

    /**
     * Sets the 'upateLanbox' indicator.
     * 
     * @param updateLanbox true if the Lanbox patch is updated immediately
     */
    public void setUpdateLanbox(final boolean updateLanbox) {
        patcher.setUpdateLanbox(updateLanbox);
        if (updateLanbox) {
            savePatch();
        }
    }

    /**
     * Gets the patch detail selection model.
     * 
     * @return the patch detail selection model
     */
    public ListSelectionModel getDetailSelectionModel() {
        return detailSelectionModel;
    }

    /**
     * Gets the channel selection model.
     * 
     * @return the channel selection model
     */
    public ListSelectionModel getChannelSelectionModel() {
        return channelSelectionModel;
    }

    /**
     * Copies the patch details to the clipboard.
     */
    public void copyPatchDetailsToClipboard() {
        List<PatchDetail> patchDetails = patch.getDetails();
        Transferable t = new PatchDetailTransferable(patchDetails);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
    }

    /**
     * Indicates whether a change in <code>stageFollowsSelection</code> state
     * would result in a blackout of all lights on stage.  A blackout
     * would be possible if the current state of stageFollowsSelection
     * is going from true to false, and there are lights on on stage.
     * 
     * @param on new state of stageFollowsSelection
     * @return true if blackout possible
     */
    public boolean isBlackout(final boolean on) {
        return stageFollowsSelection && !on && patch.isDimmerOn();
    }

    /**
     * Sets the 'stageFollowsSelection' indicator.
     * 
     * @param on the new value
     * @param blackout true if blackout allowed
     */
    public void setStageFollowSelection(final boolean on, final boolean blackout) {
        stageFollowsSelection = on;
        if (blackout) {
            patcher.blackout();
            for (int i = 0; i < patch.getDimmerCount(); i++) {
                if (detailSelectionModel.isSelectedIndex(i)) {
                    detailTableModel.fireTableRowsUpdated(0, i);
                }
            }
        } else {
            patchDetailSelectionChanged();
        }
    }

    /**
     * Gets the patch detail table model.
     * 
     * @return the patch detail table model
     */
    public PatchDetailTableModel getDetailTableModel() {
        return detailTableModel;
    }

    /**
     * Gets the channel table model.
     * 
     * @return the channel table model
     */
    public PatchChannelTableModel getChannelTableModel() {
        return channelTableModel;
    }

    /**
     * Updates the stage according to the patch detail selection changes
     * that have been made.
     */
    public void patchDetailSelectionChanged() {
        if (stageFollowsSelection) {
            boolean[] state = new boolean[patch.getDimmerCount()];
            for (int i = 0; i < patch.getDimmerCount(); i++) {
                if (detailSelectionModel.isSelectedIndex(i)) {
                    int id = patch.getDetail(i).getDimmer().getLanboxChannelId();
                    for (int j = 0; j < patch.getDimmerCount(); j++) {
                        PatchDetail d = patch.getDetail(j);
                        if (d.getDimmer().getLanboxChannelId() == id) {
                            state[j] = true;
                        }
                    }
                }
            }
            for (int i = 0; i < patch.getDimmerCount(); i++) {
                patcher.setOn(i, state[i]);
                detailTableModel.fireTableRowsUpdated(i, i);
            }
        }
    }

    /**
     * Fades out any dimmers that are currently faded in.
     */
    public void blackout() {
        patcher.blackout();
        detailTableModel.fireTableDataChanged();
    }

    /**
     * Unpatches all dimmers.
     */
    public void clearPatch() {
        patcher.clearPatch(new Listener() {

            public void changed() {
                detailTableModel.fireTableDataChanged();
                channelTableModel.fireTableDataChanged();
            }
        });
    }

    /**
     * Loads the patch information from the lanbox.
     */
    public void loadPatch() {
        patcher.loadPatch(this, true);
    }

    /**
     * Save the show patch information to the lanbox.
     */
    public void savePatch() {
        patcher.savePatch();
        detailTableModel.fireTableDataChanged();
        channelTableModel.fireTableDataChanged();
    }

    /**
     * Patches dimmers to channels 1-to-1.
     */
    public void defaultPatch() {
        patcher.defaultPatch(new Listener() {

            public void changed() {
                detailTableModel.fireTableDataChanged();
                channelTableModel.fireTableDataChanged();
            }
        });
    }

    /**
     * Patches the selected channels to the selected dimmers.
     */
    public void patch() {
        int[] detailRows = detailSelectionModel.getSelectedRows();
        if (detailRows.length > 0) {
            List<Channel> channels = getSelectedChannels();
            patcher.patch(detailRows, channels, null);
            int index1 = detailRows[0];
            int index2 = index1 + channels.size() - 1;
            detailTableModel.fireTableDataChanged();
            detailSelectionModel.setSelectionInterval(index1, index2);
            channelTableModel.fireTableDataChanged();
        }
    }

    /**
     * Unpatches the dimmers that are currently selected.
     */
    public void unpatch() {
        int[] detailRows = detailSelectionModel.getSelectedRows();
        patcher.unpatch(detailRows, null);
        detailTableModel.fireTableDataChanged();
        channelTableModel.fireTableDataChanged();
    }

    /**
     * Gets the channels that are currently selected.
     * 
     * @return the selected channels
     */
    public List<Channel> getSelectedChannels() {
        List<Channel> channels = new ArrayList<Channel>();
        int[] rows = channelSelectionModel.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            Channel channel = channelTableModel.getChannel(rows[i]);
            channels.add(channel);
        }
        return channels;
    }

    /**
     * Gets the patch details that are currently selected.
     * 
     * @return the selected patch details
     */
    public List<PatchDetail> getSelectedPatchDetails() {
        List<PatchDetail> patchDetails = new ArrayList<PatchDetail>();
        int[] rows = detailSelectionModel.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            PatchDetail detail = patch.getDetail(rows[i]);
            patchDetails.add(detail);
        }
        return patchDetails;
    }

    /**
     * Indicates whether a patch operation will do something; a patch
     * operation will only do something if both dimmers and channels
     * have been selected. 
     * 
     * @return true if patch operation is meaningful
     */
    public boolean canPatch() {
        boolean detailsSelected = !detailSelectionModel.isSelectionEmpty();
        boolean channelsSelected = !channelSelectionModel.isSelectionEmpty();
        return detailsSelected && channelsSelected;
    }

    /**
     * Indicates whether an unpatch operation will do something; an unpatch
     * will only do something if at least one of the currently selected
     * dimmers is patched.
     * 
     * @return true if unpatch is meaningful
     */
    public boolean canUnpatch() {
        boolean patched = false;
        int[] rows = detailSelectionModel.getSelectedRows();
        for (int i = 0; !patched && i < rows.length; i++) {
            patched = patch.getDetail(rows[i]).getDimmer().isPatched();
        }
        return patched;
    }

    /**
     * Gets the patcher.
     * 
     * @return the patcher
     */
    public Patcher getPatcher() {
        return patcher;
    }

    /**
     * {@inheritDoc}
     */
    public void changed() {
        detailTableModel.fireTableDataChanged();
        channelTableModel.fireTableDataChanged();
    }

    /**
     * Copies the channel names to the names of the dimmers that the
     * channels are patched to.
     */
    public void copyChannelNamesToDimmers() {
        patcher.copyChannelNamesToDimmers();
        detailTableModel.fireTableDataChanged();
    }

    /**
     * Copies the dimmer names to the names of the channels that are patched 
     * to the dimmers.
     */
    public void copyDimmerNamesToChannels() {
        patcher.copyDimmerNamesToChannels();
        channelTableModel.fireTableDataChanged();
    }

    /**
     * Initializes the patcher.
     */
    public void initialize() {
        patcher.initialize();
        detailTableModel.fireTableDataChanged();
    }
}
