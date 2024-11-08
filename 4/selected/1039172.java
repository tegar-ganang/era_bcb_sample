package ecks.services.modules.SrvHelp;

import ecks.protocols.Generic;
import ecks.services.Service;
import ecks.services.SrvHelp;
import ecks.services.modules.CommandDesc;
import ecks.services.modules.bCommand;

public class DeSync extends bCommand {

    public final CommandDesc Desc = new CommandDesc("desync", 1, true, CommandDesc.access_levels.A_NONE, "Abandons a help request", "[channel]");

    public CommandDesc getDesc() {
        return Desc;
    }

    public void handle_command(Service who, String user, String replyto, String arguments) {
        String whatchan = "";
        String whom = "";
        String args[] = arguments.split(" ");
        boolean silent = false;
        whatchan = replyto;
        whom = user;
        if (args.length > 0 && (!(args[0].equals("")))) {
            if (args[0].startsWith("#")) {
                whatchan = args[0];
            }
            if (args[0].equals("silent")) silent = true;
        }
        whom = whom.toLowerCase();
        whatchan = whatchan.toLowerCase();
        if (whatchan.startsWith("#")) {
            if (((SrvHelp) who).getChannels().containsKey(whatchan)) {
                if (((SrvHelp) who).getChannels().get(whatchan).queue.contains(Generic.Users.get(whom))) {
                    Generic.curProtocol.outPRVMSG(who, whom, "Your help request has been abandoned.");
                    ((SrvHelp) who).getChannels().get(whatchan).queue.remove(Generic.Users.get(whom));
                }
            } else if (!silent) Generic.curProtocol.outPRVMSG(who, replyto, "Error: Not a registered channel!");
        } else if (!silent) Generic.curProtocol.outPRVMSG(who, replyto, "Error: Not a channel!");
    }
}
