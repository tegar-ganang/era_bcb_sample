package net.sf.odinms.client.messages.commands;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.messages.Command;
import net.sf.odinms.client.messages.CommandDefinition;
import net.sf.odinms.client.messages.IllegalCommandSyntaxException;
import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.tools.StringUtil;
import java.util.Collection;

public class ServerMessageCommand implements Command {

    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splittedLine) throws Exception, IllegalCommandSyntaxException {
        Collection<ChannelServer> cservs = ChannelServer.getAllInstances();
        String outputMessage = StringUtil.joinStringFrom(splittedLine, 1);
        if (outputMessage.equalsIgnoreCase("!array")) outputMessage = c.getChannelServer().getArrayString();
        for (ChannelServer cserv : cservs) {
            cserv.setServerMessage(outputMessage);
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] { new CommandDefinition("servermessage", "<new message>", "Changes the servermessage to the new message", 10) };
    }
}
