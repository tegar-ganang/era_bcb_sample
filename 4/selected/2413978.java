package be.lassi.kernel;

import static be.lassi.util.Util.newArrayList;
import java.util.List;
import be.lassi.context.ShowContext;
import be.lassi.domain.Channel;
import be.lassi.domain.Dimmer;
import be.lassi.lanbox.udp.UdpSender;
import be.lassi.util.Dmx;
import be.lassi.util.Util;

/**
 * Implements the kernel of the Lassi software.
 *
 */
public class Kernel {

    /**
     *
     */
    private final ShowContext context;

    /**
     *
     */
    private final int[] dmxInputs = new int[Dmx.MAX_DIMMERS];

    /**
     *
     */
    private final int[] dmxOutputs = new int[Dmx.MAX_DIMMERS];

    /**
     *
     */
    private final int[] channelDmxOutputs = new int[Dmx.MAX_CHANNELS];

    /**
     *
     */
    private final int[] dimmerDmxOutputs = new int[Dmx.MAX_DIMMERS];

    /**
     *
     */
    private final KernelFigures statistics = new KernelFigures();

    /**
     *
     */
    private final KernelDaemon daemon;

    private final UdpSender udpSender;

    private final List<ClockListener> clockListeners = newArrayList();

    /**
     *
     *
     *
     */
    public Kernel(final ShowContext context) {
        this.context = context;
        daemon = new KernelDaemon(this);
        udpSender = new UdpSender(context);
    }

    public void start() {
        daemon.start();
    }

    public void close() {
        daemon.halt();
        daemon.interrupt();
        udpSender.close();
    }

    /**
     *
     */
    public ShowContext getContext() {
        return context;
    }

    void actionDriver(final long now, final boolean force) {
        updateChannelDmxOutputs(now);
    }

    /**
     * Update the "channelDmxOutputs" buffer by calculating new level
     * values from the values provided by the channelLevelProvider's.
     *
     */
    private void updateChannelDmxOutputs(final long now) {
        for (int i = 0; i < Dmx.MAX_CHANNELS; i++) {
            channelDmxOutputs[i] = getChannelDmxLevel(now, i);
        }
    }

    /**
     * Update the "dimmerDmxOutputs" buffer by calculating new level
     * values from the values provided by the dimmerLevelProvider's.
     *
     */
    private void updateDimmerDmxOutputs(final long now) {
        for (int i = 0; i < Dmx.MAX_DIMMERS; i++) {
            dimmerDmxOutputs[i] = getDimmerDmxLevel(now, i);
        }
    }

    /**
     * Answer the level for the channel with given index, using
     * the HTP (Highest Takes Precidence) principle.
     *
     * @param channelIndex
     * @return byte
     */
    private int getChannelDmxLevel(final long now, final int channelIndex) {
        float max = 0;
        for (int i = 0; i < context.getNumberOfChannelLevelProviders(); i++) {
            float level = context.getChannelLevelProvider(i).getValue(now, channelIndex);
            if (level > max) {
                max = level;
            }
        }
        return Dmx.getDmxValue(max);
    }

    /**
     * Answer the level for the channel with given index.
     *
     * @param channelIndex
     * @return byte
     */
    private int getDimmerDmxLevel(final long now, final int dimmerIndex) {
        float level = 0;
        Dimmer dimmer = context.getShow().getDimmers().get(dimmerIndex);
        Channel channel = dimmer.getChannel();
        if (channel != null) {
            level = channel.getValue();
        }
        return Dmx.getDmxValue(level);
    }

    /**
     * Update the "dmxOutputs" buffer from the "channelDmxOuputs" buffer
     * (taking into account the patching information) and the
     * "dimmerDmxOutputs" buffer.
     *
     * @return true if there were changes to the dmx output values
     */
    private boolean updateDmxOutputs() {
        boolean changes = false;
        for (int i = 0; i < context.getShow().getNumberOfDimmers(); i++) {
            int newLevel = 0;
            int channelIndex = context.getShow().getDimmers().get(i).getChannelId();
            if (channelIndex != -1) {
                newLevel = channelDmxOutputs[channelIndex];
            }
            if (dimmerDmxOutputs[i] > newLevel) {
                newLevel = dimmerDmxOutputs[i];
            }
            if (dmxOutputs[i] != newLevel) {
                dmxOutputs[i] = newLevel;
                changes = true;
            }
        }
        return changes;
    }

    void actionSwing(final int timeSinceLastRead) {
        for (int i = 0; i < Dmx.MAX_DIMMERS; i++) {
            context.getShow().setInputValue(i, Dmx.getValue(dmxInputs[i]));
            context.getShow().setDimmerValue(i, Dmx.getValue(dimmerDmxOutputs[i]));
            if (i < Dmx.MAX_CHANNELS) {
                context.getShow().setChannelValue(i, Dmx.getValue(channelDmxOutputs[i]));
            }
        }
        clockTick(Util.now());
    }

    /**
     *
     */
    public KernelFigures getStatistics() {
        return statistics;
    }

    public void addClockListener(final ClockListener listener) {
        clockListeners.add(listener);
    }

    public void removeClockListener(final ClockListener listener) {
        clockListeners.remove(listener);
    }

    public void clockTick(final long now) {
        for (ClockListener listener : clockListeners) {
            listener.clockTick(now);
        }
    }
}
