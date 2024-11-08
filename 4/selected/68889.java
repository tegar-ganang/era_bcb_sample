package net.sourceforge.sandirc;

import jerklib.Channel;
import jerklib.Session;
import net.sourceforge.sandirc.gui.IRCWindow;
import net.sourceforge.sandirc.gui.IRCWindow.Type;
import net.sourceforge.sandirc.gui.IRCWindowContainer;
import net.sourceforge.sandirc.gui.SandIRCFrame;
import net.sourceforge.sandirc.gui.text.IRCDocument;

public abstract class AbstractCommandHandler implements InputListener {

    private final AcceptedView view;

    private final String[] params;

    protected AbstractCommandHandler(AcceptedView view, String format) {
        System.out.print("Parsing: " + format);
        if ((view == null) || (format == null)) {
            throw new NullPointerException();
        }
        this.view = view;
        this.params = format.split("\\s+");
        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            char value = param.charAt(0);
            boolean isOptional = (param.length() == 2) && (param.charAt(1) == '?');
            System.out.print(" " + isOptional);
            switch(value) {
                case '#':
                case '@':
                    if (isOptional && (view == AcceptedView.NONE)) {
                        throw new IllegalArgumentException("A user parameter may not be optional outside of a session.");
                    }
                    if (!isOptional && ((view != AcceptedView.CHANNEL) && (view != AcceptedView.PRIVATE))) {
                        throw new IllegalArgumentException("A user parameter may not exist outside of a channel or private view. (Use $ for any parameter.)");
                    }
                    break;
                case '$':
                    break;
                case '*':
                    if (i != (params.length - 1)) {
                        throw new IllegalArgumentException("The phrase param must be last.");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized token: " + value);
            }
        }
        System.out.println(" :");
    }

    public void receiveInput(IRCWindow window, String input) {
        System.out.println(input);
        Channel selectedChannel = null;
        Session selectedSession = null;
        Type type = null;
        if (window != null) {
            selectedChannel = window.getChannel();
            selectedSession = window.getSession();
            type = window.getType();
        }
        switch(view) {
            case CHANNEL:
                if (type != IRCWindow.Type.CHANNEL) {
                    throw new IllegalArgumentException("Channel view required");
                }
                break;
            case PRIVATE:
                if (type != IRCWindow.Type.PRIVATE) {
                    throw new IllegalArgumentException("Private view required");
                }
                break;
            case SESSION:
                if (window == null || selectedSession == null || !selectedSession.isConnected()) {
                    throw new IllegalArgumentException("Connection required");
                }
                break;
            case NONE:
        }
        String[] tokens = new String[params.length];
        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            char value = param.charAt(0);
            boolean isOptional = (param.length() == 2) && (param.charAt(1) == '?');
            boolean paramMatches = false;
            String[] splitInput = input.split("\\s+", 2);
            switch(value) {
                case '$':
                case '*':
                    paramMatches = input.length() > 0;
                    break;
                case '#':
                    paramMatches = (input.length() > 1) && input.startsWith("#");
                    break;
                case '@':
                    paramMatches = (selectedChannel != null) && selectedChannel.getNicks().contains(splitInput[0]);
            }
            if ((value == '#') && !paramMatches && isOptional && (selectedChannel != null)) {
                tokens[i] = selectedChannel.getName();
            } else if ((value == '@') && !paramMatches && isOptional) {
                tokens[i] = selectedSession.getNick();
            } else if ((value == '*') && paramMatches) {
                tokens[i] = input;
                break;
            }
            if (paramMatches) {
                tokens[i] = splitInput[0];
                input = (splitInput.length > 1) ? splitInput[1] : "";
            } else if (!isOptional) {
                throw new IllegalArgumentException("Missing parameter.");
            }
        }
        System.out.println(java.util.Arrays.toString(tokens));
        handleInput(window, tokens);
    }

    protected abstract void handleInput(IRCWindow window, String[] tokens);

    enum AcceptedView {

        CHANNEL, PRIVATE, SESSION, NONE
    }
}
