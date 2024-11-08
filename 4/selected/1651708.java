package be.lassi.fixtures;

import be.lassi.domain.Fixture;
import be.lassi.lanbox.ChannelChangeProcessor;
import be.lassi.lanbox.domain.ChannelChange;

/**
 * Command to set fixture attribute values.
 */
public abstract class FixtureCommand {

    public abstract void execute(Fixture fixture, ChannelChangeProcessor channelChangeProcessor);

    protected void execute(final Fixture fixture, final ChannelChangeProcessor processor, final String attributeName, final int dmxValue) {
        int number = fixture.getChannelNumber(attributeName);
        if (number > 0) {
            execute(processor, number, dmxValue);
        }
    }

    protected void execute(final ChannelChangeProcessor processor, final int channelNumber, final int dmxValue) {
        ChannelChange cc = new ChannelChange(channelNumber, dmxValue);
        processor.change(FixtureControl.LAYER_ID, cc);
    }
}
