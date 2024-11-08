package webirc.client.commands;

import webirc.client.Channel;
import java.util.Collection;
import java.util.Vector;

/**
 * @author Ayzen
 * @version 1.0 03.08.2006 23:55:20
 */
public class PartCommand extends IRCCommand {

    public static String getName() {
        return "PART";
    }

    private Collection channels;

    private String partMessage;

    public PartCommand(Channel channel, String partMessage) {
        name = getName();
        params = ' ' + channel.toString() + partMessage != null ? " :" + partMessage : "";
    }

    public PartCommand(String prefix, String command, String params) {
        super(prefix, command, params);
        name = getName();
        String channelsNamesStr = nextParam();
        if (channelsNamesStr != null) {
            String[] channelsNames = channelsNamesStr.split(",");
            channels = new Vector();
            for (int i = 0; i < channelsNames.length; i++) channels.add(new Channel(channelsNames[i]));
        }
        partMessage = lastParam();
    }

    public Collection getChannels() {
        return channels;
    }

    public String getPartMessage() {
        return partMessage;
    }
}
