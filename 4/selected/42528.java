package ecks.services.modules.SrvChannel.Chan;

import ecks.protocols.Generic;
import ecks.services.Service;
import ecks.services.SrvChannel;
import ecks.services.SrvChannel_channel;
import ecks.services.modules.CommandDesc;
import ecks.services.modules.bCommand;
import ecks.util;
import java.util.Map;

public class ShowUsers extends bCommand {

    public final CommandDesc Desc = new CommandDesc("showusers", 1, true, CommandDesc.access_levels.A_PENDING, "Shows users in channel", "[channel]");

    public CommandDesc getDesc() {
        return Desc;
    }

    public void handle_command(Service who, String user, String replyto, String arguments) {
        String whatchan = "";
        if (arguments.length() > 1) whatchan = arguments; else whatchan = replyto;
        whatchan = whatchan.toLowerCase();
        if (whatchan.startsWith("#")) {
            if (((SrvChannel) who).getChannels().containsKey(whatchan)) {
                Generic.curProtocol.outPRVMSG(who, user, "" + util.pad("USER", 12) + " " + "ACCESS");
                Generic.curProtocol.outPRVMSG(who, user, "------------------------------");
                for (Map.Entry<String, SrvChannel_channel.ChanAccess> t : ((SrvChannel) who).getChannels().get(whatchan).getUsers().entrySet()) {
                    Generic.curProtocol.outPRVMSG(who, user, "" + util.pad(t.getKey(), 12) + " " + t.getValue().toString().substring(2));
                }
            } else Generic.curProtocol.outPRVMSG(who, user, "Error: Not a registered channel!");
        } else Generic.curProtocol.outPRVMSG(who, user, "Error: Not a channel!");
    }
}
