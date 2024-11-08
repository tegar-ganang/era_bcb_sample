package be.lassi.cues;

import java.io.PrintStream;

/**
 * 
 *
 *
 */
public class CuePrinter {

    /**
     * 
     */
    private PrintStream out;

    /**
     * Constructs a new cue printer.
     *
     */
    public CuePrinter(final PrintStream out) {
        this.out = out;
    }

    public void print(final Cues cues) {
        for (Cue cue : cues) {
            print(cue);
        }
    }

    public void print(final LightCues cues) {
        for (Cue cue : cues) {
            print(cue);
        }
    }

    public void print(final Cue cue) {
        out.print("number=\"");
        out.print(cue.getNumber());
        out.print("\", description=\"");
        out.print(cue.getDescription());
        out.println("\"");
        if (cue.isLightCue()) {
            print((LightCueDetail) cue.getDetail());
        }
    }

    private void print(final LightCueDetail detail) {
        out.print("  submasters: ");
        for (int i = 0; i < detail.getNumberOfSubmasters(); i++) {
            out.print(i + 1);
            CueSubmasterLevel level = detail.getSubmasterLevel(i);
            print(level.getIntValue(), level.isDerived());
        }
        out.println("");
        out.print("  channels:   ");
        for (int i = 0; i < detail.getNumberOfChannels(); i++) {
            out.print(i + 1);
            CueChannelLevel level = detail.getChannelLevel(i);
            print(level.getChannelIntValue(), level.isDerived());
        }
        out.println("");
    }

    private void print(final int levelValue, final boolean derived) {
        String value = Integer.toString(levelValue);
        out.print(derived ? "{" : "[");
        out.print(value);
        out.print(derived ? "}" : "]");
        for (int j = 0; j < 4 - value.length(); j++) {
            out.print(" ");
        }
    }
}
