package ecks.services.modules.SrvHelp;

import ecks.Logging;
import ecks.protocols.Generic;
import ecks.services.Service;
import ecks.services.SrvHelp;
import ecks.services.SrvHelp_channel;
import ecks.services.modules.CommandDesc;
import ecks.services.modules.bCommand;

public class Register extends bCommand {

    public final CommandDesc Desc = new CommandDesc("register", 2, true, CommandDesc.access_levels.A_OPER, "Registers a channel.", "<channel>");

    public CommandDesc getDesc() {
        return Desc;
    }

    public void handle_command(Service who, String user, String replyto, String arguments) {
        SrvHelp temp = ((SrvHelp) who);
        String args[] = arguments.split(" ");
        if (args.length == 1) {
            String ch = args[0].toLowerCase();
            if (!temp.getChannels().containsKey(ch)) {
                temp.getChannels().put(ch, new SrvHelp_channel(ch));
                Generic.curProtocol.outPRVMSG(who, replyto, "" + Generic.Users.get(user).uid + ": Registration Succeeded!");
                Logging.info("SRVHELP", "Channel " + ch + " registered by " + user + ".");
                Generic.curProtocol.srvJoin(who, ch, "+stn");
                Generic.curProtocol.outSETMODE(who, ch, "+o", who.getname());
            } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: Channel is already registered.");
        } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: Invalid Arguments. Usage: register [channel]");
    }
}
