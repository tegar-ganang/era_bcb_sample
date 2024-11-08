package net.sf.odinms.client.messages.commands;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.messages.CommandDefinition;
import net.sf.odinms.client.messages.Command;
import net.sf.odinms.client.messages.IllegalCommandSyntaxException;
import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.tools.MaplePacketCreator;

public class ratesCommands implements Command {

    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception, IllegalCommandSyntaxException {
        if (splitted[0].equals("!mesorate")) {
            if (splitted.length > 1) {
                int meso = Integer.parseInt(splitted[1]);
                ChannelServer cserv = c.getChannelServer();
                cserv.setMesoRate(meso);
                MaplePacket packet = MaplePacketCreator.serverNotice(6, "Meso rate has been changed to " + meso + "x");
                ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
            } else {
                mc.dropMessage("Syntax: !mesorate <number>");
            }
        } else if (splitted[0].equals("!droprate")) {
            if (splitted.length > 1) {
                int drop = Integer.parseInt(splitted[1]);
                ChannelServer cserv = c.getChannelServer();
                cserv.setDropRate(drop);
                MaplePacket packet = MaplePacketCreator.serverNotice(6, "Drop rate has been changed to " + drop + "x");
                ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
            } else {
                mc.dropMessage("Syntax: !droprate <number>");
            }
        } else if (splitted[0].equals("!bossdroprate")) {
            if (splitted.length > 1) {
                int bossdrop = Integer.parseInt(splitted[1]);
                ChannelServer cserv = c.getChannelServer();
                cserv.setBossDropRate(bossdrop);
                MaplePacket packet = MaplePacketCreator.serverNotice(6, "Boss drop rate has been changed to " + bossdrop + "x");
                ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
            } else {
                mc.dropMessage("Syntax: !bossdroprate <number>");
            }
        } else if (splitted[0].equals("!mesorate")) {
            c.getPlayer().gainMeso(Integer.parseInt(splitted[1]), true);
        } else if (splitted[0].equals("!exprate")) {
            if (splitted.length > 1) {
                int exp = Integer.parseInt(splitted[1]);
                ChannelServer cserv = c.getChannelServer();
                cserv.setExpRate(exp);
                MaplePacket packet = MaplePacketCreator.serverNotice(6, "Exp rate has been changed to " + exp + "x");
                ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
            } else {
                mc.dropMessage("Syntax: !exprate <number>");
            }
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] { new CommandDefinition("mesorate", "?", "!mesorate <rate>", 10), new CommandDefinition("droprate", "?", "!drop <rate>", 10), new CommandDefinition("bossdroprate", "?", "!bossdrop <rate>", 10), new CommandDefinition("exprate", "?", "!exp <rate>", 10) };
    }
}
