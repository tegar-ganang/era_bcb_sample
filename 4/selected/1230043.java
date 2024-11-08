package be.lassi.cues;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import be.lassi.domain.Level;
import be.lassi.domain.LevelValue;
import be.lassi.domain.Submaster;
import be.lassi.domain.Submasters;
import be.lassi.util.Printer;

/**
 * Prints the cue submaster and channel level values for testing purposes.
 */
public class LevelPrinter extends Printer {

    /**
     * Constructs a new level printer.
     */
    public LevelPrinter() {
        this(System.out);
    }

    /**
     * Constructs a new level printer.
     */
    public LevelPrinter(final PrintStream stream) {
        super(stream);
    }

    public void print(final Submasters submasters, final NewLightCues cues) {
        printSubmasters(submasters);
        printCues(submasters, cues);
    }

    private void printSubmasters(final Submasters submasters) {
        println("Submasters  (columns are submaster channel levels)");
        for (Submaster submaster : submasters) {
            printf("  Submaster %d  ", submaster.getId());
            printSubmaster(submaster);
            println("");
        }
    }

    public static String toString(final Submaster submaster) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        new LevelPrinter(ps).printSubmaster(submaster);
        ps.close();
        return baos.toString();
    }

    public void printSubmaster(final Submaster submaster) {
        List<Level> activeLevels = submaster.getActiveLevels();
        for (int i = 0; i < activeLevels.size(); i++) {
            LevelValue levelValue = activeLevels.get(i).getLevelValue();
            printf("%d@%d", i, levelValue.getIntValue());
            if (i < activeLevels.size() - 1) {
                printf(" ");
            }
        }
    }

    private void printCues(final Submasters submasters, final NewLightCues cues) {
        println("Cues (cue submaster levels | cue channel levels)");
        for (int i = 0; i < cues.size(); i++) {
            LightCueDetail detail = cues.get(i);
            printf("  Cue %d ", i);
            printCue(detail);
            println("");
        }
    }

    public static String toString(final LightCueDetail detail) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        new LevelPrinter(ps).printCue(detail);
        ps.close();
        return baos.toString();
    }

    public static String channelLevels(final LightCueDetail detail) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        new LevelPrinter(ps).printChannelLevels(detail);
        ps.close();
        return baos.toString();
    }

    public void printCue(final LightCueDetail detail) {
        printCueSubmasterLevels(detail);
        printCueChannelLevels(detail);
    }

    public void printChannelLevels(final LightCueDetail detail) {
        for (int channelIndex = 0; channelIndex < detail.getNumberOfChannels(); channelIndex++) {
            CueChannelLevel level = detail.getChannelLevel(channelIndex);
            printf(" %d@%s", channelIndex, level.isActive() ? Integer.toString(level.getIntValue()) : "-");
        }
    }

    private void printCueChannelLevels(final LightCueDetail detail) {
        for (int channelIndex = 0; channelIndex < detail.getNumberOfChannels(); channelIndex++) {
            CueChannelLevel level = detail.getChannelLevel(channelIndex);
            printCueChannelLevel(level, channelIndex);
        }
    }

    private void printCueSubmasterLevels(final LightCueDetail detail) {
        for (int submasterIndex = 0; submasterIndex < detail.getNumberOfSubmasters(); submasterIndex++) {
            CueSubmasterLevel level = detail.getSubmasterLevel(submasterIndex);
            LevelValue levelValue = level.getLevelValue();
            printf(" s%d@%s%s", submasterIndex, formatted(levelValue), derived(level));
        }
    }

    private void printCueChannelLevel(final CueChannelLevel cueChannelLevel, final int channelIndex) {
        LevelValue channelLevelValue = cueChannelLevel.getChannelLevelValue();
        LevelValue submasterLevelValue = cueChannelLevel.getSubmasterLevelValue();
        printf(" %d@%s..%s%s", channelIndex, formatted(channelLevelValue), formatted(submasterLevelValue), derived(cueChannelLevel));
    }

    private String formatted(final LevelValue levelValue) {
        return levelValue.isActive() ? Integer.toString(levelValue.getIntValue()) : "-";
    }

    private String derived(final CueLevel level) {
        return level.isDerived() ? "*" : "";
    }
}
