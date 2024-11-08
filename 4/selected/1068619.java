package net.sf.odinms.client.messages.commands;

import java.rmi.RemoteException;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.channel.remote.ChannelWorldInterface;
import net.sf.odinms.net.world.WorldRegistryImpl;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleCharacterUtil;
import net.sf.odinms.client.messages.Command;
import net.sf.odinms.client.messages.CommandDefinition;
import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.client.messages.ServernoticeMapleClientMessageCallback;

public class OnlineCommand implements Command {

    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception, RemoteException {
        StringBuilder sb = new StringBuilder("Characters online: ");
        mc.dropMessage(sb.toString());
        if (splitted[0].toLowerCase().equals("@online")) {
            for (ChannelServer cs : c.getChannelServer().getAllInstances()) {
                sb = new StringBuilder("[Channel " + cs.getChannel() + "]");
                mc.dropMessage(sb.toString());
                sb = new StringBuilder();
                for (MapleCharacter chr : cs.getPlayerStorage().getAllCharacters()) {
                    if (sb.length() > 150) {
                        sb.setLength(sb.length() - 2);
                        mc.dropMessage(sb.toString());
                        sb = new StringBuilder();
                    }
                    sb.append(MapleCharacterUtil.makeMapleReadable(chr.getName()));
                    sb.append(", ");
                }
                if (sb.length() >= 2) sb.setLength(sb.length() - 2);
                mc.dropMessage(sb.toString());
            }
        } else if (splitted[0].toLowerCase().equals("@channel")) {
            sb = new StringBuilder("[Channel " + c.getChannel() + "]");
            mc.dropMessage(sb.toString());
            sb = new StringBuilder();
            for (MapleCharacter chr : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                if (sb.length() > 150) {
                    sb.setLength(sb.length() - 2);
                    mc.dropMessage(sb.toString());
                    sb = new StringBuilder();
                }
                sb.append(MapleCharacterUtil.makeMapleReadable(chr.getName()));
                sb.append(", ");
            }
            if (sb.length() >= 2) sb.setLength(sb.length() - 2);
            new ServernoticeMapleClientMessageCallback(c).dropMessage(sb.toString());
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] { new CommandDefinition("online", "", "List all of the users on the server, organized by channel.", 0), new CommandDefinition("channel", "", "List all characters online on the current channel.", 0) };
    }
}
