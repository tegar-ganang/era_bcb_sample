package be.lassi.lanbox.commands.channel;

import static org.testng.Assert.assertEquals;
import be.lassi.lanbox.commands.Command;
import be.lassi.lanbox.commands.CommandTCA;
import be.lassi.lanbox.domain.ChannelChanges;
import be.lassi.lanbox.domain.ChannelStatus;

/**
 * Common code for ChannelSet*TC testcase classes.
 */
public class ChannelSetTCA extends CommandTCA {

    protected static final int BUFFER_ID = 3;

    protected static final int CHANNEL_ID_1 = 5;

    protected static final int CHANNEL_ID_2 = CHANNEL_ID_1 + 1;

    protected static final int CHANNEL_COUNT = 2;

    protected void assertChannelIds(final ChannelStatus[] statusses) {
        assertEquals(CHANNEL_ID_1, statusses[0].getChannelId());
        assertEquals(CHANNEL_ID_2, statusses[1].getChannelId());
    }

    protected void channelSetData() {
        ChannelChanges changes = new ChannelChanges();
        changes.add(CHANNEL_ID_1, 0);
        changes.add(CHANNEL_ID_2, 0);
        Command command = new ChannelSetData(BUFFER_ID, changes);
        execute(command);
    }

    protected ChannelStatus[] readStatusses() {
        ChannelReadStatus command = new ChannelReadStatus(BUFFER_ID, CHANNEL_ID_1, CHANNEL_COUNT);
        execute(command);
        return command.getStatusses();
    }
}
