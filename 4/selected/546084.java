package connection;

import lombok.Getter;
import lombok.Setter;
import org.pircbotx.PircBotX;
import scripting.Script;
import scripting.ScriptGUI;
import scripting.ScriptManager;
import scripting.ScriptVars;
import shared.Message;
import shared.RoomManager;

/**
 * The Class KEllyBot.
 */
public class KEllyBot extends PircBotX {

    /** The Constant VERSION. */
    public static final String VERSION = "kEllyIRC 0.5.167 alpha";

    /** The Constant systemName. */
    public static final String systemName = "SYSTEM";

    /**
	 * Gets the connection.
	 *
	 * @return the connection
	 */
    @Getter
    @Setter
    private Connection connection;

    /**
	 * Instantiates a new kellybot.
	 *
	 * @param c the c
	 */
    public KEllyBot(Connection c) {
        this.connection = c;
    }

    @Override
    public void sendMessage(String target, String message) {
        if (message.startsWith("/")) {
            doCommand(message.substring(1));
        } else {
            if (target == null || target.equals(Connection.CONSOLE_ROOM)) {
                return;
            }
            message = Message.quicklinkToLink(message);
            RoomManager.enQueue(new Message(this.getConnection(), message, this.getUserBot(), getChannel(target), Message.MSG));
            super.sendMessage(target, message);
        }
    }

    @Override
    public void sendAction(String target, String message) {
        if (message.startsWith("/")) {
            doCommand(message.substring(1));
        } else {
            if (target == null || target.equals(Connection.CONSOLE_ROOM)) {
                return;
            }
            RoomManager.enQueue(new Message(this, message, getNick(), target, Message.ACTION));
            super.sendAction(target, message);
        }
    }

    @Override
    public void sendNotice(String target, String notice) {
        if (target == null || target.equals(Connection.CONSOLE_ROOM)) {
            return;
        }
        RoomManager.enQueue(new Message(this, "NOTICE: " + notice, getNick(), target, Message.NOTICE));
        super.sendNotice(target, notice);
    }

    public void changeNick(String nick) {
        super.changeNick(nick);
        RoomManager.enQueue(new Message(connection, "You are now known as " + nick, systemName, ScriptVars.curChannel, Message.CONSOLE));
    }

    /**
	 * Do command.
	 *
	 * @param line the line
	 */
    public void doCommand(String line) {
        if (line.startsWith("/")) line = line.substring(1);
        String command = line.split(" ")[0].trim();
        boolean found = false;
        for (Script s : ScriptManager.scripts) {
            if (s.getFunctions().contains(command)) {
                if (line.split(" ").length > 1) {
                    s.invoke(command, this, line.substring(line.indexOf(line.split(" ")[1])));
                } else {
                    s.invoke(command, this, "");
                }
                found = true;
            }
        }
        if (!found) {
            ScriptGUI.window(command + " is not a valid alias.");
        }
    }
}
