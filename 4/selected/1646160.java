package free.jin.console.ics;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import free.jin.Preferences;
import free.jin.console.ConsoleManager;
import free.jin.console.CustomConsoleDesignation;

/**
 * A base class for <code>ConsoleManager</code> implementations ICS-based
 * servers.
 * 
 * @author Maryanovsky Alexander
 */
public abstract class IcsConsoleManager extends ConsoleManager {

    /**
   * {@inheritDoc}
   */
    protected CustomConsoleDesignation loadCustomConsoleDesignation(String prefsPrefix, String title, String encoding, List channels, Pattern messageRegex) {
        Preferences prefs = getPrefs();
        boolean includeShouts = prefs.getBool(prefsPrefix + "includeShouts", false);
        boolean includeCShouts = prefs.getBool(prefsPrefix + "includeCShouts", false);
        return loadCustomConsoleDesignation(prefsPrefix, title, encoding, channels, messageRegex, includeShouts, includeCShouts);
    }

    /**
   * Loads a custom console designation from the preferences using the specified
   * prefix and the already retrieved data.
   */
    protected abstract IcsCustomConsoleDesignation loadCustomConsoleDesignation(String prefsPrefix, String title, String encoding, List channels, Pattern messageRegex, boolean includeShouts, boolean includeCShouts);

    /**
   * {@inheritDoc} 
   */
    public Object encodeConsoleChannelsPref(List channels) {
        int[] channelNumbers = new int[channels.size()];
        for (int i = 0; i < channels.size(); i++) {
            IcsChannel channel = (IcsChannel) channels.get(i);
            channelNumbers[i] = channel.getNumber();
        }
        return channelNumbers;
    }

    /**
   * {@inheritDoc}
   */
    public List parseConsoleChannelsPref(Object channelsPrefsValue) {
        if (channelsPrefsValue == null) return Collections.EMPTY_LIST;
        Map allChannels = getChannels();
        List channels = new LinkedList();
        int[] channelNumbers = (int[]) channelsPrefsValue;
        for (int i = 0; i < channelNumbers.length; i++) channels.add(allChannels.get(new Integer(channelNumbers[i])));
        return channels;
    }
}
