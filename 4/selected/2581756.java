package ecks.services.modules.SrvChannel.Chan;

import ecks.protocols.Generic;
import ecks.services.Service;
import ecks.services.SrvChannel;
import ecks.services.SrvChannel_channel;
import ecks.services.modules.CommandDesc;
import ecks.services.modules.bCommand;

public class SetModes extends bCommand {

    public final CommandDesc Desc = new CommandDesc("setmodes", 2, true, CommandDesc.access_levels.A_AUTHED, "Enforces the current channel modes.", "[channel]");

    public CommandDesc getDesc() {
        return Desc;
    }

    public void handle_command(Service who, String user, String replyto, String arguments) {
        String whatchan = "";
        String whom = "";
        String args[] = arguments.split(" ");
        String what = "";
        whatchan = replyto;
        whom = user;
        if (args.length > 0 && (!(args[0].equals("")))) if (args[0].startsWith("#")) {
            whatchan = args[0];
            what = arguments.substring(whatchan.length() + 1);
        } else {
            what = arguments;
        }
        whom = whom.toLowerCase();
        whatchan = whatchan.toLowerCase();
        if (whatchan.startsWith("#")) {
            if (((SrvChannel) who).getChannels().containsKey(whatchan)) {
                if (Generic.Users.containsKey(whom)) {
                    if (Generic.Users.get(whom).authhandle != null) {
                        String aname = Generic.Users.get(whom).authhandle;
                        if (((SrvChannel) who).getChannels().get(whatchan).getUsers().containsKey(aname)) {
                            if (((SrvChannel) who).getChannels().get(whatchan).getUsers().get(aname).ordinal() >= SrvChannel_channel.ChanAccess.C_MASTER.ordinal()) {
                                if (!what.trim().equals("*")) {
                                    ((SrvChannel) who).getChannels().get(whatchan).setMeta("enfmodes", Generic.Channels.get(whatchan).modes.getModes());
                                    Generic.curProtocol.outPRVMSG(who, replyto, "Enforced Modes Set.");
                                } else {
                                    ((SrvChannel) who).getChannels().get(whatchan).rmMeta("enfmodes");
                                    Generic.curProtocol.outPRVMSG(who, replyto, "Enforced Modes Cleared.");
                                }
                            } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: Must be master or greater to set modes!");
                        } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: User has no access to channel!");
                    } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: User is not authed!");
                } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: User does not exist!");
            } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: Not a registered channel!");
        } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: Not a channel!");
    }
}
