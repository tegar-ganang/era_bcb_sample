package be.lassi.ui.patch;

import static be.lassi.util.Util.newArrayList;
import java.util.List;
import be.lassi.base.Listener;
import be.lassi.context.ShowContext;
import be.lassi.domain.Channel;
import be.lassi.domain.Channels;
import be.lassi.domain.Dimmer;
import be.lassi.domain.Dimmers;
import be.lassi.lanbox.commands.Command;
import be.lassi.lanbox.commands.CommonGetPatch;
import be.lassi.lanbox.domain.PatchBufferProcessor;

/**
 * Generates the lanbox commands that are needed to load the entire
 * patch information; and processes the command responses.
 */
public class GetPatch implements PatchBufferProcessor {

    /**
     * The show context.
     */
    private final ShowContext context;

    /**
     * Indicates whether the channels in the dimmers should be updated; when
     * false only the lanbox channel id in the dimmer is updated.
     */
    private final boolean updateChannels;

    /**
     * The listener that needs to be notified when the patch is completely loaded.
     */
    private final Listener listener;

    /**
     * The number of CommonGetPatch commands for which we still need to
     * receive a response from the lanbox.
     */
    private int expectedResponses;

    /**
     * Constructs a new instance.
     *
     * @param context the show context
     * @param listener the listeners that needs to be notified once all patch information is loaded
     * @param updateChannels true if the dimmer channels have to be updated
     */
    public GetPatch(final ShowContext context, final Listener listener, final boolean updateChannels) {
        this.context = context;
        this.listener = listener;
        this.updateChannels = updateChannels;
    }

    /**
     * Gets the lanbox commands that are needed to load all patch
     * information.
     *
     * @return the commands to get the patch
     */
    public List<CommonGetPatch> getCommands() {
        List<CommonGetPatch> commands = newArrayList();
        int totalDimmerCount = context.getShow().getDimmers().size();
        expectedResponses = (totalDimmerCount / 256) + 1;
        for (int i = 0; i < expectedResponses; i++) {
            int startDimmerId = i * 255;
            int dimmerCount = totalDimmerCount - startDimmerId;
            if (dimmerCount > 255) {
                dimmerCount = 255;
            }
            CommonGetPatch command = new CommonGetPatch(this, startDimmerId, dimmerCount);
            commands.add(command);
        }
        return commands;
    }

    /**
     * {@inheritDoc}
     */
    public void process(final int startDimmerId, final int[] channelIds) {
        Dimmers dimmers = context.getShow().getDimmers();
        Channels channels = context.getShow().getChannels();
        for (int i = 0; i < channelIds.length; i++) {
            Dimmer dimmer = dimmers.get(startDimmerId + i);
            int channelId = channelIds[i];
            dimmer.setLanboxChannelId(channelId);
            if (updateChannels) {
                if (channelId >= 0 && channelId < channels.size()) {
                    Channel channel = channels.get(channelIds[i]);
                    dimmer.setChannel(channel);
                } else {
                    dimmer.setChannel(null);
                }
            }
        }
        expectedResponses--;
        if (expectedResponses == 0) {
            listener.changed();
        }
    }

    /**
     * Executes the commands.
     */
    public void execute() {
        for (Command command : getCommands()) {
            context.getLanbox().getEngine().execute(command);
        }
    }
}
