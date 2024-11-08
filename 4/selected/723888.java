package be.lassi.lanbox.tools;

import static be.lassi.domain.Attribute.BLUE;
import static be.lassi.domain.Attribute.CYAN;
import static be.lassi.domain.Attribute.GREEN;
import static be.lassi.domain.Attribute.INTENSITY;
import static be.lassi.domain.Attribute.MAGENTA;
import static be.lassi.domain.Attribute.RED;
import static be.lassi.domain.Attribute.YELLOW;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import be.lassi.cues.CueCommandFactory;
import be.lassi.domain.Colors;
import be.lassi.domain.Fixture;
import be.lassi.domain.FixtureGroups;
import be.lassi.domain.Fixtures;
import be.lassi.lanbox.Connection;
import be.lassi.lanbox.ConnectionCommandProcessor;
import be.lassi.lanbox.ConnectionPreferences;
import be.lassi.lanbox.commands.Command;
import be.lassi.lanbox.commands.CommandProcessor;
import be.lassi.lanbox.commands.Common16BitMode;
import be.lassi.lanbox.cuesteps.Comment;
import be.lassi.lanbox.cuesteps.CueScene;
import be.lassi.lanbox.cuesteps.CueStep;
import be.lassi.lanbox.cuesteps.GoLayerCueList;
import be.lassi.lanbox.cuesteps.LoopTo;
import be.lassi.lanbox.cuesteps.ResetLayer;
import be.lassi.lanbox.cuesteps.SetLayerMixMode;
import be.lassi.lanbox.cuesteps.StopLayer;
import be.lassi.lanbox.cuesteps.WaitLayer;
import be.lassi.lanbox.domain.ChannelChange;
import be.lassi.lanbox.domain.ChannelChanges;
import be.lassi.lanbox.domain.CueList;
import be.lassi.lanbox.domain.FadeType;
import be.lassi.lanbox.domain.MixMode;
import be.lassi.lanbox.domain.Time;
import be.lassi.preferences.AllPreferences;
import be.lassi.util.Dmx;
import be.lassi.util.TimeConverter;

/**
 * Builds cue lists and writes these cue lists to the Lanbox; classes
 * that build actual cue lists can inherited from this class.
 */
public class CueListBuilder {

    protected static final int LAYER_A = 1;

    protected static final int LAYER_B = 2;

    protected static final int LAYER_C = 3;

    protected static final int LAYER_D = 4;

    protected static final int LAYER_E = 5;

    protected static final int LAYER_F = 6;

    protected final List<CueList> cueLists = new ArrayList<CueList>();

    protected final CueList manualControlCueList;

    private final CueList timedControlCueList;

    private final List<ChannelChange> ccs = new ArrayList<ChannelChange>();

    private int previousMillis = 0;

    /**
     * Constructs a new cue list builder.
     */
    public CueListBuilder() {
        timedControlCueList = new CueList(5);
        timedControlCueList.add(new Comment("TIMED"));
        timedControlCueList.add(new Comment("CONTROL"));
        cueLists.add(timedControlCueList);
        manualControlCueList = new CueList(6);
        manualControlCueList.add(new Comment("MANUAL"));
        manualControlCueList.add(new Comment("CONTROL"));
        cueLists.add(manualControlCueList);
    }

    /**
     * Writes the cue lists to the Lanbox.
     */
    protected void writeCueLists() {
        AllPreferences preferences = new AllPreferences();
        try {
            preferences.load();
            writeCueLists(preferences.getConnectionPreferences());
        } catch (IOException e) {
            StringBuilder b = new StringBuilder();
            b.append("ERROR: Could not load file: \"");
            b.append(AllPreferences.FILE);
            b.append("\"\n");
            b.append(e.getMessage());
            String message = b.toString();
            System.out.println(message);
        }
    }

    private void writeCueLists(final ConnectionPreferences preferences) {
        Connection connection = new Connection(preferences);
        connection.openConnection();
        if (!connection.isOpen()) {
            System.out.println("ERROR: Connection not opened");
        } else {
            CommandProcessor processor = new ConnectionCommandProcessor(connection);
            processor.process(new Common16BitMode(true));
            writeCueLists(processor);
            connection.close();
            System.out.println("Ready");
        }
    }

    /**
     * Sets the color attribute of given fixtures to given color.
     *
     * @param fixtures the fixtures for which to set the color
     * @param color the color to be set
     */
    protected void setColor(final Fixtures fixtures, final Color color) {
        for (Fixture fixture : fixtures) {
            setColor(fixture, color);
        }
    }

    /**
     * Sets the color attributes of given fixtures to the colors in given
     * color collection (both the fixtures and the colors collection have
     * to have the same size).
     *
     * @param fixtures the fixtures for which to set the colors
     * @param colors the colors to set
     */
    protected void setColors(final Fixtures fixtures, final Colors colors) {
        for (int i = 0; i < fixtures.size(); i++) {
            setColor(fixtures.get(i), colors.get(i));
        }
    }

    /**
     * Sets the color attributes of the fixtures in given fixture groups to
     * the colors in given  color collection (both the fixture groups and
     * the colors collection have to have the same size).
     *
     * @param groups the fixture groups for which to set the colors
     * @param colors the colors to set
     */
    protected void setColors(final FixtureGroups groups, final Colors colors) {
        for (int i = 0; i < groups.size(); i++) {
            setColor(groups.get(i), colors.get(i));
        }
    }

    /**
     * Sets the attribute with given name to given dmx value in given fixtures.
     *
     * @param fixtures the fixtures for which to change the attribute
     * @param attributeName the name of the attribute to be changed
     * @param dmxValue the new dmx value for given attribute
     */
    protected void setAttribute(final Fixtures fixtures, final String attributeName, final int dmxValue) {
        for (Fixture fixture : fixtures) {
            setAttribute(fixture, attributeName, dmxValue);
        }
    }

    /**
     * Sets the attribute with given name to given dmx value in given fixture.
     *
     * @param fixture the fixture for which to change the attribute
     * @param attributeName the name of the attribute to be changed
     * @param dmxValue the new dmx value for given attribute
     */
    protected void setAttribute(final Fixture fixture, final String attributeName, final int dmxValue) {
        int number = fixture.getChannelNumber(attributeName);
        if (number > 0) {
            ChannelChange cc = new ChannelChange(number, dmxValue);
            ccs.add(cc);
        }
    }

    /**
     * Sets the intensity attribute value (0 to 100) of given fixtures.
     *
     * @param fixtures the fixtures for which to set the intensity attribute value
     * @param percentage the intensity value (0 to 100) to be set
     */
    protected void setIntensity(final Fixtures fixtures, final int percentage) {
        for (Fixture fixture : fixtures) {
            setIntensity(fixture, percentage);
        }
    }

    /**
     * Sets the intensity attribute value (0 to 100) of given fixture.
     *
     * @param fixtures the fixture for which to set the intensity attribute value
     * @param percentage the intensity value (0 to 100) to be set
     */
    protected void setIntensity(final Fixture fixture, final int percentage) {
        int dmxValue = Dmx.getDmxValue(percentage / 100f);
        setAttribute(fixture, INTENSITY, dmxValue);
    }

    /**
     * Sets the color attributes of given fixture to given color.
     *
     * @param fixture the fixture for which to set the color
     * @param color the color to be set
     */
    protected void setColor(final Fixture fixture, final Color color) {
        setAttribute(fixture, RED, color.getRed());
        setAttribute(fixture, GREEN, color.getGreen());
        setAttribute(fixture, BLUE, color.getBlue());
        setAttribute(fixture, CYAN, 255 - color.getRed());
        setAttribute(fixture, MAGENTA, 255 - color.getGreen());
        setAttribute(fixture, YELLOW, 255 - color.getBlue());
    }

    /**
     * Gets the color with given hue (with full saturation and brightness);
     *
     * @param hue the color hue value
     * @return the color with given hue
     */
    protected Color colorWithHue(final float hue) {
        return new Color(Color.HSBtoRGB(hue, 1.0f, 1.0f));
    }

    /**
     * Adds a cuestep to the current cue list to add a scene with the
     * currently collected channel values.
     *
     * @param fadeType the type of fading to be used to set the channel values
     * @param fadeTime the time used for the fading operation
     * @param holdTime the time to wait before stepping to the next cue list step
     */
    protected void cueScene(final FadeType fadeType, final Time fadeTime, final Time holdTime) {
        CueStep cueStep = new CueScene(fadeType, fadeTime, holdTime, new ChannelChanges(ccs));
        currentCueList().add(cueStep);
        ccs.clear();
    }

    /**
     * Gets the cue list that is currently being constructed.
     *
     * @return the current cue list
     */
    private CueList currentCueList() {
        return cueLists.get(cueLists.size() - 1);
    }

    /**
     * Adds a cuestep to the current cue list to add a scene with the
     * currently collected channel values; the crossfade is done instantly.
     *
     * @param holdTime the time to wait before stepping to the next cue list step
     */
    protected void hold(final Time holdTime) {
        crossFade(Time.TIME_0S, holdTime, true);
    }

    /**
     * Adds a cuestep to the current cue list to add a scene with the
     * currently collected channel values.
     *
     * @param fadeTime the time used for the fading operation
     * @param holdTime the time to wait before stepping to the next cue list step
     */
    protected void crossFade(final Time fadeTime, final Time holdTime) {
        crossFade(fadeTime, holdTime, true);
    }

    /**
     * Adds a cuestep to the current cue list to add a scene with the
     * currently collected channel values.
     *
     * @param fadeTime the time used for the fading operation
     * @param holdTime the time to wait before stepping to the next cue list step
     * @param clearChannelChanges true if the collection in which channel changes
     *             are collected needs to be emptied afterwards
     */
    protected void crossFade(final Time fadeTime, final Time holdTime, final boolean clearChannelChanges) {
        ChannelChanges changes = new ChannelChanges();
        for (ChannelChange cc : ccs) {
            changes.add(cc);
        }
        changes.sort();
        CueStep cueStep = new CueScene(FadeType.CROSS_FADE, fadeTime, holdTime, changes);
        currentCueList().add(cueStep);
        if (clearChannelChanges) {
            clearChannelChanges();
        }
    }

    /**
     * Empties the collection in which channel changes are collected.
     */
    protected void clearChannelChanges() {
        ccs.clear();
    }

    /**
     * Adds a cuestep to the current cue list to jump to given
     * cue list step, and repeat the jump the specified number of times.
     *
     * @param cueStepId the number of the cue step within the current cue list to jump to
     * @param loopCount number of time to repeat the jump
     */
    protected void loopTo(final int cueStepId, final int loopCount) {
        CueStep cueStep = new LoopTo(cueStepId, loopCount);
        currentCueList().add(cueStep);
    }

    /**
     * Adds one or more comment cuesteps to the current cue list.
     *
     * @param comment the comment to be added, multiple comments can be added
     *                by separating the comments with a newline character
     */
    protected void comment(final String comment) {
        String[] strings = comment.split("\n");
        for (String string : strings) {
            currentCueList().add(new Comment(string));
        }
    }

    /**
     * Gets the number that the next cue step that will be added to
     * the current cue list will have.  This can be usefull in the
     * program logic to record a cue list position to jump back to
     * in a loop.
     *
     * @return the number of the next cue step
     */
    protected int nextCueStepNumber() {
        return currentCueList().size() + 1;
    }

    /**
     * Adds a cue step in the timed control cue list that will execute a go
     * for the current cue list at given timestamp in given layer.
     *
     * @param timestamp the timestamp at which to execute the go
     * @param layerNumber the layer in which to
     */
    protected void go(final String timestamp, final int layerNumber) {
        int millis = new TimeConverter().convert(timestamp);
        int offset = new TimeConverter().convert("00:00:50");
        millis -= offset;
        String newTimestamp = new TimeConverter().convert(millis);
        System.out.println("new=" + newTimestamp + ", old=" + timestamp);
        int delay = millis - previousMillis;
        previousMillis = millis;
        List<Time> times = new TimeConverter().split(delay);
        for (Time time : times) {
            timedControlCueList.add(new WaitLayer(time));
        }
        int cueListId = createCueList();
        timedControlCueList.add(new GoLayerCueList(layerNumber, cueListId, 1));
    }

    /**
     * Creates a new cue list from the current cue steps, and adds
     * a step to the manual control cue list to start this new cue list
     * in a dynamic layer.
     */
    protected void go() {
        go(0);
    }

    /**
     * Creates a new cue list from the current cue steps, and adds
     * a step to the manual control cue list to start this new cue list
     * in given layer.
     *
     * @param layerNumber the layer in which to execute the cue list
     */
    protected void go(final int layerNumber) {
        int cueListId = createCueList();
        manualControlCueList.add(new GoLayerCueList(layerNumber, cueListId, 1));
        manualControlCueList.add(new WaitLayer(Time.FOREVER));
    }

    private int createCueList() {
        int cueListId = 50 + cueLists.size() - 1;
        createCueList(cueListId);
        return cueListId;
    }

    /**
     * Creates a new cue list; the new cue list becomes the "current" cue list
     * to which new cue steps will be added.
     *
     * @param cueListId the cue list number
     */
    protected void createCueList(final int cueListId) {
        CueList cueList = new CueList(cueListId);
        cueLists.add(cueList);
    }

    /**
     * Creates a new cue list; the new cue list becomes the "current" cue list
     * to which new cue steps will be added.
     *
     * @param cueListId the cue list number
     * @param comment a comment that will added in the first cue step(s) of the new cue list
     */
    protected void createCueList(final int cueListId, final String comment) {
        createCueList(cueListId);
        comment(comment);
    }

    /**
     * Creates a new cue list; the new cue list becomes the "current" cue list
     * to which new cue steps will be added.
     *
     * @param comment a comment that will added in the first cue step(s) of the new cue list
     */
    protected void createCueList(final String comment) {
        System.out.println(comment.replaceAll("\n", " "));
        createCueList();
        comment(comment);
    }

    /**
     * Gets the Lanbox encoded time that is closest to given number of seconds.
     *
     * @param seconds the time duration in number of seconds
     * @return a Lanbox encoded time
     */
    protected Time seconds(final int seconds) {
        return Time.fromMillis(seconds * 1000);
    }

    /**
     * Gets the Lanbox encoded time that is closest to given number of milliseconds.
     *
     * @param millis the time duration in number of milliseconds
     * @return a Lanbox encoded time
     */
    protected Time millis(final int millis) {
        return Time.fromMillis(millis);
    }

    /**
     * Adds a cuestep to the current cue list to reset given layer.
     *
     * @param layerNumber the layer to be reset
     */
    protected void resetLayer(final int layerNumber) {
        CueStep cueStep = new ResetLayer(layerNumber);
        currentCueList().add(cueStep);
    }

    /**
     * Adds a cuestep to the current cue list to stop the sequencer
     * in given layer.
     *
     * @param layerNumber the layer in which to stop the sequencer
     */
    protected void stop(final int layerNumber) {
        CueStep cueStep = new StopLayer(layerNumber);
        currentCueList().add(cueStep);
    }

    /**
     * Adds a cuestep to the current cue list to set the mix mode in
     * given layer.
     *
     * @param layerNumber the layer for which to set the mix mode
     * @param mixMode the mix mode to be set
     */
    protected void setLayerMixMode(final int layerNumber, final MixMode mixMode) {
        CueStep cueStep = new SetLayerMixMode(layerNumber, mixMode);
        currentCueList().add(cueStep);
    }

    private void writeCueLists(final CommandProcessor processor) {
        for (CueList cueList : cueLists) {
            writeCueList(cueList, processor);
        }
        System.out.println("");
    }

    private void writeCueList(final CueList cueList, final CommandProcessor processor) {
        List<Command> commands = new CueCommandFactory().getCommands(cueList);
        for (Command command : commands) {
            processor.process(command);
            dot();
        }
    }

    private int dots = 0;

    private void dot() {
        if (++dots < 50) {
            System.out.print(".");
        } else {
            System.out.println(".");
            dots = 0;
        }
    }
}
