package net.jetrix.protocols;

import java.util.*;
import net.jetrix.*;
import net.jetrix.messages.*;
import net.jetrix.messages.channel.*;
import net.jetrix.messages.channel.specials.*;
import org.apache.commons.lang.*;

/**
 * Protocol to communicate with IRC clients.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 799 $, $Date: 2009-02-18 11:28:08 -0500 (Wed, 18 Feb 2009) $
 */
public class IRCProtocol extends AbstractProtocol {

    private static Map<String, String> styles = new HashMap<String, String>();

    static {
        styles.put("red", "04");
        styles.put("black", "01");
        styles.put("green", "03");
        styles.put("lightGreen", "09");
        styles.put("darkBlue", "02");
        styles.put("blue", "12");
        styles.put("cyan", "10");
        styles.put("aqua", "11");
        styles.put("yellow", "08");
        styles.put("kaki", "07");
        styles.put("brown", "05");
        styles.put("lightGray", "15");
        styles.put("gray", "14");
        styles.put("magenta", "13");
        styles.put("purple", "06");
        styles.put("b", "");
        styles.put("i", "");
        styles.put("u", "7");
        styles.put("white", "00");
    }

    /**
     * Return the name of this protocol
     */
    public String getName() {
        return "IRC";
    }

    /**
     * Parse the specified string and return the corresponding server
     * message for this protocol.
     */
    public Message getMessage(String line) {
        IRCMessage msg = IRCMessage.parse(line);
        if (msg.isCommand(IRCCommand.JOIN)) {
            AddPlayerMessage message = new AddPlayerMessage();
            message.setDestination(ChannelManager.getInstance().getChannel(msg.getParameter(0)));
            return message;
        } else if (msg.isCommand(IRCCommand.PART)) {
            LeaveMessage message = new LeaveMessage();
            message.setDestination(ChannelManager.getInstance().getChannel(msg.getParameter(0)));
            return message;
        } else if (msg.isCommand(IRCCommand.PRIVMSG)) {
            SmsgMessage message = new SmsgMessage();
            message.setDestination(ChannelManager.getInstance().getChannel(msg.getParameter(0)));
            message.setText(msg.getParameter(1));
            message.setPrivate(false);
            return message;
        } else {
            return null;
        }
    }

    /**
     * Translate the specified message into a string that will be sent
     * to a client using this protocol.
     */
    public String translate(Message m, Locale locale) {
        if (m instanceof PlineMessage) {
            return translate((PlineMessage) m, locale);
        } else if (m instanceof PlayerLostMessage) {
            return translate((PlayerLostMessage) m);
        } else if (m instanceof PlineActMessage) {
            return translate((PlineActMessage) m, locale);
        } else if (m instanceof TeamMessage) {
            return translate((TeamMessage) m, locale);
        } else if (m instanceof JoinMessage) {
            return translate((JoinMessage) m, locale);
        } else if (m instanceof LeaveMessage) {
            return translate((LeaveMessage) m, locale);
        } else if (m instanceof NewGameMessage) {
            return translate((NewGameMessage) m, locale);
        } else if (m instanceof EndGameMessage) {
            return translate((EndGameMessage) m, locale);
        } else if (m instanceof IngameMessage) {
            return translate((IngameMessage) m, locale);
        } else if (m instanceof PauseMessage) {
            return translate((PauseMessage) m);
        } else if (m instanceof ResumeMessage) {
            return translate((ResumeMessage) m);
        } else if (m instanceof GmsgMessage) {
            return translate((GmsgMessage) m, locale);
        } else if (m instanceof SpectatorListMessage) {
            return translate((SpectatorListMessage) m, locale);
        } else {
            return null;
        }
    }

    public String translate(PlineMessage m, Locale locale) {
        IRCMessage message = new IRCMessage(IRCCommand.PRIVMSG);
        Destination source = m.getSource();
        if (source != null && source instanceof Client) {
            message.setNick(((Client) source).getUser().getName());
        } else {
            message.setNick("jetrix");
        }
        String name = m.getChannelName() == null ? "#jetrix" : "#" + m.getChannelName();
        message.addParameter(name);
        message.addParameter(applyStyle(m.getText(locale)));
        return message.toString();
    }

    public String translate(PlineActMessage m, Locale locale) {
        IRCMessage message = new IRCMessage(IRCCommand.PRIVMSG);
        Destination source = m.getSource();
        if (source != null && source instanceof Client) {
            message.setNick(((Client) source).getUser().getName());
        } else {
            message.setNick("jetrix");
        }
        String name = m.getChannelName() == null ? "#jetrix" : "#" + m.getChannelName();
        message.addParameter(name);
        message.addParameter(applyStyle("ACTION " + m.getText(locale) + ""));
        return message.toString();
    }

    public String translate(GmsgMessage m, Locale locale) {
        IRCMessage message = new IRCMessage(IRCCommand.PRIVMSG);
        Destination source = m.getSource();
        if (source != null && source instanceof Client) {
            message.setNick(((Client) source).getUser().getName());
        } else {
            message.setNick("jetrix");
        }
        String name = m.getChannelName() == null ? "#jetrix" : "#" + m.getChannelName();
        message.addParameter(name);
        message.addParameter(applyStyle("<gray>" + m.getText(locale)));
        return message.toString();
    }

    public String translate(SpectatorListMessage m, Locale locale) {
        IRCMessage message1 = new IRCMessage(IRCReply.RPL_NAMREPLY);
        message1.setNick("jetrix");
        message1.addParameter(((Client) m.getDestination()).getUser().getName());
        message1.addParameter("=");
        message1.addParameter("#" + m.getChannel());
        Collection<String> spectators = m.getSpectators();
        message1.addParameter(StringUtils.join(spectators.iterator(), " "));
        IRCMessage message2 = new IRCMessage(IRCReply.RPL_ENDOFNAMES);
        message2.setNick("jetrix");
        message2.addParameter(((Client) m.getDestination()).getUser().getName());
        message2.addParameter("#" + m.getChannel());
        message2.addParameter("End of /NAMES list");
        return message1.toString() + getEOL() + message2;
    }

    public String translate(TeamMessage m, Locale locale) {
        Client client = (Client) m.getSource();
        IRCMessage message = new IRCMessage(IRCCommand.PRIVMSG);
        message.setNick("jetrix");
        message.addParameter("#" + m.getChannelName());
        String messageKey = m.getName() == null ? "channel.team.none" : "channel.team.new";
        Object[] params = new Object[] { client.getUser().getName(), m.getName() };
        message.addParameter(applyStyle(Language.getText(messageKey, locale, params)));
        return message.toString();
    }

    public String translate(JoinMessage m, Locale locale) {
        IRCMessage message = new IRCMessage(IRCCommand.JOIN);
        message.setNick(m.getName());
        message.addParameter("#" + m.getChannelName());
        return message.toString();
    }

    public String translate(LeaveMessage m, Locale locale) {
        IRCMessage message = new IRCMessage(IRCCommand.PART);
        message.setNick(m.getName());
        message.addParameter("#" + m.getChannelName());
        return message.toString();
    }

    public String translate(NewGameMessage m, Locale locale) {
        IRCMessage message = new IRCMessage(IRCCommand.PRIVMSG);
        message.setNick("jetrix");
        message.addParameter("#" + m.getChannelName());
        message.addParameter(applyStyle(Language.getText("channel.game.start", locale)));
        return message.toString();
    }

    public String translate(EndGameMessage m, Locale locale) {
        IRCMessage message = new IRCMessage(IRCCommand.PRIVMSG);
        message.setNick("jetrix");
        message.addParameter("#" + m.getChannelName());
        message.addParameter(applyStyle(Language.getText("channel.game.stop", locale)));
        return message.toString();
    }

    public String translate(IngameMessage m, Locale locale) {
        IRCMessage message = new IRCMessage(IRCCommand.PRIVMSG);
        message.setNick("jetrix");
        message.addParameter("#" + m.getChannelName());
        message.addParameter(applyStyle(Language.getText("channel.game.running", locale)));
        return message.toString();
    }

    public String translate(PauseMessage m) {
        return null;
    }

    public String translate(ResumeMessage m) {
        return null;
    }

    public String translate(LevelMessage m) {
        return null;
    }

    public String translate(FieldMessage m) {
        return null;
    }

    public String translate(PlayerLostMessage m) {
        return null;
    }

    public String translate(DisconnectedMessage m) {
        return null;
    }

    public String translate(CommandMessage m) {
        return null;
    }

    public String translate(LinesAddedMessage m) {
        return null;
    }

    public String translate(AddLineMessage m) {
        return null;
    }

    public String translate(ClearLineMessage m) {
        return null;
    }

    public String translate(NukeFieldMessage m) {
        return null;
    }

    public String translate(RandomClearMessage m) {
        return null;
    }

    public String translate(SwitchFieldsMessage m) {
        return null;
    }

    public String translate(ClearSpecialsMessage m) {
        return null;
    }

    public String translate(GravityMessage m) {
        return null;
    }

    public String translate(BlockQuakeMessage m) {
        return null;
    }

    public String translate(BlockBombMessage m) {
        return null;
    }

    public Map<String, String> getStyles() {
        return styles;
    }

    public String applyStyle(String text) {
        Map<String, String> styles = getStyles();
        if (styles == null) return text;
        for (String key : styles.keySet()) {
            String value = styles.get(key);
            if (value == null) {
                value = "";
            }
            text = text.replaceAll("<" + key + ">", value);
            text = text.replaceAll("</" + key + ">", value);
        }
        return text;
    }

    public char getEOL() {
        return '\n';
    }
}
