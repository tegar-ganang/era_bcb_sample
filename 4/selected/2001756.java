package webirc.client.commands;

import webirc.client.Channel;
import webirc.client.User;
import webirc.client.utils.Utils;
import java.util.HashMap;

/**
 * @author Ayzen
 * @version 1.0 03.09.2006 11:28:56
 */
public class ModeCommand extends IRCCommand {

    public static String getName() {
        return "MODE";
    }

    private boolean channelMode = false;

    private User user;

    private Channel channel;

    private String mode;

    private String args;

    boolean addingModes = false;

    private HashMap modes = new HashMap();

    public ModeCommand(String prefix, String command, String params) {
        super(prefix, command, params);
        name = getName();
        String target = nextParam();
        mode = nextParam();
        if (mode.charAt(0) == '+') addingModes = true;
        if (Utils.isChannel(target)) {
            channelMode = true;
            channel = new Channel(target);
            args = lastParam();
            parseChannelMode();
        } else {
            user = new User(target);
            parseUserMode();
        }
    }

    private void parseUserMode() {
        for (int i = 1; i < mode.length(); i++) {
            modes.put(new Character(mode.charAt(i)), null);
        }
    }

    private void parseChannelMode() {
        int argIndex = 0;
        String arguments;
        for (int i = 1; i < mode.length(); i++) {
            char modeChar = mode.charAt(i);
            if (args != null && (isUserTypeMode(modeChar) || isBanMode(modeChar))) {
                int divIndex = args.indexOf(" ", argIndex);
                if (divIndex != -1) {
                    arguments = args.substring(argIndex, divIndex);
                    argIndex = divIndex + 1;
                } else {
                    arguments = args.substring(argIndex);
                    argIndex = args.length() + 1;
                }
            } else arguments = null;
            modes.put(new Character(modeChar), arguments);
        }
    }

    public boolean isChannelMode() {
        return channelMode;
    }

    public boolean isAddingModes() {
        return addingModes;
    }

    public static boolean isUserTypeMode(char mode) {
        return mode == 'q' || mode == 'a' || mode == 'o' || mode == 'h' || mode == 'v';
    }

    public static boolean isBanMode(char mode) {
        return mode == 'b';
    }

    public HashMap getModes() {
        return modes;
    }

    public String getMode() {
        return mode;
    }

    public String getArgs() {
        return args;
    }

    public Channel getChannel() {
        return channel;
    }

    public User getUser() {
        return user;
    }
}
