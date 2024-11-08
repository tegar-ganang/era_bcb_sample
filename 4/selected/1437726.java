package ecks.services.modules.SrvChannel.Chan;

import ecks.Logging;
import ecks.protocols.Generic;
import ecks.services.Service;
import ecks.services.SrvChannel;
import ecks.services.modules.CommandDesc;
import ecks.services.modules.bCommand;
import ecks.util;

public class Access extends bCommand {

    public final CommandDesc Desc = new CommandDesc("access", 2, true, CommandDesc.access_levels.A_PENDING, "Shows access of user in a channel", "[channel] [user]");

    public CommandDesc getDesc() {
        return Desc;
    }

    public void handle_command(Service who, String user, String replyto, String arguments) {
        String whatchan = "";
        String whom = "";
        String args[] = arguments.split(" ");
        whatchan = replyto;
        whom = user;
        try {
            if (args.length > 0 && (!(args[0].equals("")))) {
                if (args[0].startsWith("#")) {
                    whatchan = args[0];
                    if (args.length > 1) whom = args[1];
                } else if ((args.length > 1) && args[1].startsWith("#")) {
                    whatchan = args[1];
                    whom = args[0];
                } else {
                    whom = args[0];
                }
            }
        } catch (NullPointerException NPE) {
            NPE.printStackTrace();
            Logging.warn("SRVCHAN_ACCESS", "Got NPE: " + arguments);
        }
        whom = whom.toLowerCase();
        whatchan = whatchan.toLowerCase();
        if (whatchan.startsWith("#")) {
            if (((SrvChannel) who).getChannels().containsKey(whatchan)) {
                if (Generic.Users.containsKey(whom)) {
                    if (Generic.Users.get(whom).authhandle != null) {
                        String aname = Generic.Users.get(whom).authhandle;
                        if (((SrvChannel) who).getChannels().get(whatchan).getUsers().containsKey(aname)) {
                            String alevel = ((SrvChannel) who).getChannels().get(whatchan).getUsers().get(aname).toString();
                            Generic.curProtocol.outPRVMSG(who, replyto, util.pad("" + aname + " ", 20) + alevel.substring(2));
                        } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: User has no access to channel!");
                    } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: User is not authed!");
                } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: User does not exist!");
            } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: Not a registered channel!");
        } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: Not a channel!");
    }
}
