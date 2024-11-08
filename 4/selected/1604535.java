package be.lassi.lanbox.commands;

import static org.testng.Assert.fail;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import be.lassi.lanbox.Connection;
import be.lassi.lanbox.ConnectionCommandProcessor;
import be.lassi.lanbox.ConnectionPreferences;
import be.lassi.lanbox.LayerStatusPrinter;
import be.lassi.lanbox.commands.channel.ChannelReadData;
import be.lassi.lanbox.commands.channel.ChannelSetData;
import be.lassi.lanbox.commands.layer.LayerClear;
import be.lassi.lanbox.commands.layer.LayerGetStatus;
import be.lassi.lanbox.commands.layer.LayerSetChaseMode;
import be.lassi.lanbox.commands.layer.LayerSetFadeType;
import be.lassi.lanbox.commands.layer.LayerSetMixMode;
import be.lassi.lanbox.commands.layer.LayerSetOutput;
import be.lassi.lanbox.domain.ChannelChange;
import be.lassi.lanbox.domain.ChannelChanges;
import be.lassi.lanbox.domain.ChaseMode;
import be.lassi.lanbox.domain.FadeType;
import be.lassi.lanbox.domain.LayerStatus;
import be.lassi.lanbox.domain.MixMode;

public abstract class CommandTCA {

    private Connection connection;

    private CommandProcessor processor;

    @BeforeMethod
    public void open() {
        ConnectionPreferences preferences = new ConnectionPreferences();
        preferences.setEnabled(true);
        connection = new Connection(preferences);
        connection.openAndWait();
        if (!connection.isOpen()) {
            fail("Could not open connection to lanbox");
        }
        processor = new ConnectionCommandProcessor(connection);
        execute(new Common16BitMode(true));
        execute(new LayerClear(1));
        execute(new LayerClear(2));
        execute(new LayerClear(3));
        execute(new LayerClear(4));
        execute(new LayerClear(5));
        execute(new LayerSetFadeType(1, FadeType.OFF));
        execute(new LayerSetFadeType(2, FadeType.OFF));
        execute(new LayerSetFadeType(3, FadeType.OFF));
        execute(new LayerSetChaseMode(1, ChaseMode.NO_CHASE));
        execute(new LayerSetChaseMode(2, ChaseMode.NO_CHASE));
        execute(new LayerSetChaseMode(3, ChaseMode.NO_CHASE));
        execute(new LayerSetMixMode(1, MixMode.COPY));
        execute(new LayerSetMixMode(2, MixMode.COPY));
        execute(new LayerSetMixMode(3, MixMode.COPY));
        execute(new LayerSetOutput(1, true));
        execute(new LayerSetOutput(2, true));
        execute(new LayerSetOutput(3, true));
        execute(new LayerSetOutput(4, true));
        execute(new LayerSetOutput(5, true));
    }

    @AfterMethod
    public void closeConnection() {
        connection.close();
    }

    protected void execute(final Command command) {
        processor.process(command);
        StringBuilder b = new StringBuilder();
        command.appendCommand(b);
        command.appendResponse(b);
        System.out.println(b.toString());
    }

    protected LayerStatus layerGetStatus(final int engineId) {
        LayerStatus status = new LayerStatus();
        execute(new LayerGetStatus(engineId, status));
        return status;
    }

    protected void channelSetData(final int layerNumber, final int[] values) {
        ChannelChanges changes = new ChannelChanges();
        for (int i = 0; i < values.length; i++) {
            changes.add(i + 1, values[i]);
        }
        execute(new ChannelSetData(layerNumber, changes));
    }

    protected void channelReadData(final int layerNumber, final int[] values) {
        ChannelChange[] changes = new ChannelChange[values.length];
        for (int i = 0; i < values.length; i++) {
            changes[i] = new ChannelChange(i + 1, values[i]);
        }
        execute(new ChannelReadData(values, layerNumber, 1, values.length));
    }

    protected void assertBuffer(final int bufferId, final int[] expectedValues) {
        int[] values = new int[expectedValues.length];
        execute(new ChannelReadData(values, bufferId, 1, values.length));
        boolean ok = true;
        for (int i = 0; ok && i < values.length; i++) {
            ok = values[i] == expectedValues[i];
        }
        if (!ok) {
            StringBuilder b = new StringBuilder();
            b.append("Values do not match, expected: { ");
            for (int i = 0; i < expectedValues.length; i++) {
                b.append(expectedValues[i]);
                b.append(", ");
            }
            b.append("}, but found: { ");
            for (int i = 0; i < expectedValues.length; i++) {
                b.append(values[i]);
                b.append(", ");
            }
            b.append("}");
            fail(b.toString());
        }
    }

    protected void printChannel1() {
        System.out.println("1[" + getChannel1() + "]");
    }

    protected int getChannel1() {
        int[] values = new int[1];
        execute(new ChannelReadData(values, 1, 1, values.length));
        return values[0];
    }

    protected void setChannel1(final int value) {
        int[] values = new int[1];
        values[0] = value;
        channelSetData(1, values);
    }

    protected void print(final LayerStatus status) {
        StringBuilder b = new StringBuilder();
        new LayerStatusPrinter().print(status, b);
        System.out.println(b.toString());
    }

    protected void sleep(final int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    protected Connection getConnection() {
        return connection;
    }
}
