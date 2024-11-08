package be.lassi.domain;

import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import be.lassi.base.BooleanHolder;

/**
 * LevelController to add the 'pre-heating' component to the channel level
 * output. When pre-heating is enabled for a given channel, the minimum level of
 * that channel is set to a fix value (e.g. 5%). This is to reduce the peak
 * current the dimmer has to deliver when switching the channel from zero to
 * full.
 * <p>
 * The fix pre-heating level can be set (the same value is used for all
 * channels).
 * <p>
 * Pre-heating can be enabled or disabled per channel.
 * <p>
 * Pre-heating can be switched on or off for all channels in one go. However,
 * when switching on, a small delay is included between each channel that is
 * switched on to avoid the peak current.
 * 
 */
public class PreHeating implements LevelController, LevelProvider {

    private Level level = new Level();

    private BooleanHolder[] channelEnabled;

    public PreHeating(final int numberOfChannels) {
        level.setIntValue(5);
        channelEnabled = new BooleanHolder[numberOfChannels];
        for (int i = 0; i < numberOfChannels; i++) {
            channelEnabled[i] = new BooleanHolder();
        }
    }

    private void disableAllChannels() {
        for (int i = 0; i < getNumberOfChannels(); i++) {
            setChannelEnabled(i, false);
        }
    }

    /** delay of 250 ms ==> 24 channels are switched on in 6 seconds */
    private void enableAllChannels() {
        for (int i = 0; i < getNumberOfChannels(); i++) {
            if (!isChannelEnabled(i)) {
                final int channelIndex = i;
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        public void run() {
                            sleep(250);
                            setChannelEnabled(channelIndex, true);
                        }
                    });
                } catch (InterruptedException e) {
                } catch (InvocationTargetException e) {
                }
            }
        }
    }

    public BooleanHolder getChannelEnabled(final int channelIndex) {
        return channelEnabled[channelIndex];
    }

    public Level getLevel() {
        return level;
    }

    public float getLevelValue() {
        return level.getValue();
    }

    public float getLevelValue(final long now, final int channelIndex) {
        float result = 0f;
        if (isChannelEnabled(channelIndex)) {
            result = level.getValue();
        }
        return result;
    }

    public int getNumberOfChannels() {
        return channelEnabled.length;
    }

    public boolean isChannelEnabled(final int channelIndex) {
        return channelEnabled[channelIndex].getValue();
    }

    public void setAllChannelsEnabled(final boolean enabled) {
        if (enabled) {
            enableAllChannels();
        } else {
            disableAllChannels();
        }
    }

    public void setChannelEnabled(final int channelIndex, final boolean value) {
        channelEnabled[channelIndex].setValue(value);
    }

    public void setLevelValue(final float value) {
        level.setValue(value);
    }

    private void sleep(final int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
        }
    }

    public float getValue(final long now, final int channelIndex) {
        return getLevelValue(now, channelIndex);
    }
}
