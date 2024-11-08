package be.lassi.fixtures;

import be.lassi.domain.Fixture;
import be.lassi.domain.FixtureChannel;
import be.lassi.domain.Preset;
import be.lassi.domain.PresetValue;
import be.lassi.lanbox.ChannelChangeProcessor;

/**
 * Sets the fixture attribute value to the values in a given preset.
 */
public class ApplyPreset extends FixtureCommand {

    private final String presetName;

    /**
     * Constructs a new instance.
     *
     * @param presetName the preset name
     */
    public ApplyPreset(final String presetName) {
        this.presetName = presetName;
    }

    /**
     * Gets the preset name.
     *
     * @return the preset name
     */
    public String getPresetName() {
        return presetName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(final Fixture fixture, final ChannelChangeProcessor channelChangeProcessor) {
        Preset preset = fixture.getPreset(presetName);
        if (preset != null) {
            for (int i = 0; i < preset.getValueCount(); i++) {
                PresetValue presetValue = preset.getValue(i);
                FixtureChannel channel = presetValue.getChannel();
                execute(channelChangeProcessor, channel.getNumber(), presetValue.getValue());
            }
        }
    }
}
