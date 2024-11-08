package be.lassi.domain;

import be.lassi.cues.CueChannelLevel;
import be.lassi.cues.CueLevel;
import be.lassi.cues.CueSubmasterLevel;
import be.lassi.cues.LightCueDetail;
import be.lassi.util.Util;

/**
 * Helper methods for setting initial values in domain objects.
 */
public class Setup {

    /**
     * Sets channel level values in a submaster.
     * <pre>
     * Example: Setup.submaster(submaster, "0@0 3@60 6@100 1@-");
     *   set channel 0 of given submaster to 0% (active)
     *   set channel 3 of given submaster to 60%  (active)
     *   set channel 6 of given submaster to 100% (active)
     *   set channel 1 of given submaster to not active
     * </pre>
     * @param submaster the submaster in which to set values
     * @param levels a string with a specification of the level values
     */
    public static void submaster(final Submaster submaster, final String levels) {
        String[] strings = levels.split(" ");
        for (String string : strings) {
            String[] subStrings = string.split("@");
            int channelIndex = Util.toInt(subStrings[0]);
            Level level = submaster.getLevel(channelIndex);
            if ("-".equals(subStrings[1])) {
                level.setActive(false);
            } else {
                int intValue = Util.toInt(subStrings[1]);
                level.setActive(true);
                level.setIntValue(intValue);
            }
        }
    }

    /**
     * Sets submaster and channel level values in a cue.
     * <pre>
     * Example: Setup.cue(cue, "s0@10 s3@60 0@100 1@-");
     *   set level of submaster 0 in given cue to 10% (active)
     *   set level of submaster 3 in given cue to 60%  (active)
     *   set level of channel 0 in given cue to 100% (active)
     *   set channel 1 in given cue to not active
     * </pre>
     * @param cue the cue in which to set values
     * @param levels a string with a specification of the level values
     */
    public static void cue(final LightCueDetail cue, final String levels) {
        String[] strings = levels.split(" ");
        for (String string : strings) {
            String[] subStrings = string.split("@");
            if (subStrings[0].startsWith("s")) {
                int submasterIndex = Util.toInt(subStrings[0].substring(1));
                CueSubmasterLevel level = cue.getSubmasterLevel(submasterIndex);
                LevelValue levelValue = level.getLevelValue();
                updateLevel(level, levelValue, subStrings[1]);
            } else {
                int channelIndex = Util.toInt(subStrings[0]);
                CueChannelLevel level = cue.getChannelLevel(channelIndex);
                LevelValue levelValue = level.getChannelLevelValue();
                updateLevel(level, levelValue, subStrings[1]);
            }
        }
    }

    private static void updateLevel(final CueLevel level, final LevelValue levelValue, final String levelString) {
        String string = levelString;
        if (string.endsWith("*")) {
            level.setDerived(true);
            string = string.substring(0, string.length() - 1);
        } else {
            level.setDerived(false);
        }
        if ("-".equals(string)) {
            levelValue.setActive(false);
        } else {
            int intValue = Util.toInt(string);
            levelValue.setActive(true);
            levelValue.setIntValue(intValue);
        }
    }
}
