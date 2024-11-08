package net.jetrix.protocols;

import net.jetrix.*;
import net.jetrix.messages.*;
import net.jetrix.messages.channel.*;
import java.util.*;

/**
 * An abstract protocol to communicate with a client.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 798 $, $Date: 2009-02-18 10:24:28 -0500 (Wed, 18 Feb 2009) $
 */
public class TspecProtocol extends TetrinetProtocol {

    /**
     * Return the name of this protocol
     */
    public String getName() {
        return "tspec";
    }

    /**
     * Parse the specified string and return the corresponding server
     * message for this protocol.
     */
    public Message getMessage(String line) {
        Message message = null;
        if (line.startsWith("pline") && !line.startsWith("plineact")) {
            SmsgMessage smsg = new SmsgMessage();
            smsg.setSlot(Integer.parseInt(line.substring(6, 7)));
            if (line.indexOf("//") == 8) {
                smsg.setPrivate(false);
                if (line.length() > 11) {
                    smsg.setText(line.substring(11));
                }
                message = smsg;
            } else if (line.indexOf("/") != 8) {
                smsg.setPrivate(true);
                if (line.length() > 8) {
                    smsg.setText(line.substring(8));
                }
                message = smsg;
            }
        } else if (line.startsWith("/pline")) {
            SmsgMessage smsg = new SmsgMessage();
            smsg.setText(line.substring("/pline".length()));
            smsg.setPrivate(false);
            message = smsg;
        } else if (line.startsWith("/")) {
            line = "pline 1 " + line;
        } else if (line.startsWith("<") && line.contains(">")) {
            SmsgMessage smsg = new SmsgMessage();
            smsg.setText(line.substring(line.indexOf(">") + 1).trim());
            smsg.setPrivate(true);
            message = smsg;
        } else if (line.startsWith("speclist")) {
            SpectatorListMessage slist = new SpectatorListMessage();
            String[] tokens = line.split(" ");
            slist.setChannel(tokens[1].substring(1));
            slist.setSpectators(Arrays.asList(tokens).subList(2, tokens.length));
            message = slist;
        }
        return message != null ? message : super.getMessage(line);
    }

    /**
     * Translate the specified message into a string that will be sent
     * to a client using this protocol.
     */
    public String translate(Message m, Locale locale) {
        if (m instanceof SpectatorListMessage) {
            return translate((SpectatorListMessage) m, locale);
        }
        if (m instanceof SmsgMessage) {
            return translate((SmsgMessage) m, locale);
        } else {
            return super.translate(m, locale);
        }
    }

    public String translate(SpectatorListMessage m, Locale locale) {
        StringBuilder message = new StringBuilder();
        message.append("speclist #");
        message.append(m.getChannel());
        for (String spectator : m.getSpectators()) {
            message.append(" ");
            message.append(spectator);
        }
        return message.toString();
    }

    public String translate(SmsgMessage m, Locale locale) {
        StringBuilder message = new StringBuilder();
        String name = ((Client) m.getSource()).getUser().getName();
        if (m.isPrivate()) {
            message.append("smsg ");
            message.append(name);
            message.append(" ");
            message.append(m.getText());
        } else {
            message.append(super.translate(m, locale));
        }
        return message.toString();
    }

    public String translate(JoinMessage m, Locale locale) {
        if (m.getSlot() == 0) {
            StringBuilder message = new StringBuilder();
            message.append("specjoin ");
            message.append(m.getName());
            return message.toString();
        } else {
            return super.translate(m, locale);
        }
    }

    public String translate(LeaveMessage m, Locale locale) {
        if (m.getSlot() == 0) {
            StringBuilder message = new StringBuilder();
            message.append("specleave ");
            message.append(m.getName());
            return message.toString();
        } else {
            return super.translate(m, locale);
        }
    }
}
