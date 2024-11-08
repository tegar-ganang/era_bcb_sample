package ecks.services.modules.SrvChannel.Chan;

import ecks.Configuration;
import ecks.protocols.Generic;
import ecks.services.Service;
import ecks.services.SrvAuth;
import ecks.services.SrvChannel;
import ecks.services.SrvChannel_channel;
import ecks.services.modules.CommandDesc;
import ecks.services.modules.bCommand;

public class ChUser extends bCommand {

    public final CommandDesc Desc = new CommandDesc("chuser", 3, true, CommandDesc.access_levels.A_AUTHED, "Changes a user's access on a channel", "<user> [channel] <access>");

    public CommandDesc getDesc() {
        return Desc;
    }

    public void handle_command(Service who, String user, String replyto, String arguments) {
        String whatchan = "";
        String whom = "";
        String args[] = arguments.split(" ");
        SrvChannel_channel.ChanAccess newacc;
        boolean silent = false;
        if (replyto.startsWith("#")) whatchan = replyto;
        whom = "";
        newacc = SrvChannel_channel.ChanAccess.C_NONE;
        if (args.length > 0 && (!(args[0].equals("")))) {
            if (args.length == 1) {
                whom = args[0];
            } else if (args.length == 2) {
                if (args[0].startsWith("#")) {
                    whom = args[1];
                    whatchan = args[0];
                } else if (args[1].startsWith("#")) {
                    whom = args[0];
                    whatchan = args[1];
                } else {
                    whom = args[0];
                    try {
                        newacc = SrvChannel_channel.ChanAccess.valueOf("C_" + args[1].toUpperCase());
                    } catch (IllegalArgumentException iae) {
                        Generic.curProtocol.outPRVMSG(who, replyto, "Error: Invalid level.");
                        return;
                    }
                }
            } else if (args.length == 3) {
                if (args[0].startsWith("#")) {
                    whom = args[1];
                    whatchan = args[0];
                    try {
                        newacc = SrvChannel_channel.ChanAccess.valueOf("C_" + args[2].toUpperCase());
                    } catch (IllegalArgumentException iae) {
                        Generic.curProtocol.outPRVMSG(who, replyto, "Error: Invalid level.");
                        return;
                    }
                } else if (args[1].startsWith("#")) {
                    whom = args[0];
                    whatchan = args[1];
                    try {
                        newacc = SrvChannel_channel.ChanAccess.valueOf("C_" + args[2].toUpperCase());
                    } catch (IllegalArgumentException iae) {
                        Generic.curProtocol.outPRVMSG(who, replyto, "Error: Invalid level.");
                        return;
                    }
                } else {
                    Generic.curProtocol.outPRVMSG(who, replyto, "Error: Invalid arguments");
                    return;
                }
            }
        }
        whom = whom.toLowerCase();
        whatchan = whatchan.toLowerCase();
        if (whatchan.startsWith("#")) {
            if (((SrvChannel) who).getChannels().containsKey(whatchan)) {
                if (Generic.Users.containsKey(whom)) {
                    if (Generic.Users.get(whom).authhandle != null) {
                        if (Generic.Users.get(user).authhandle != null) {
                            if (!Generic.Users.get(user).authhandle.equals(Generic.Users.get(whom).authhandle)) {
                                String aname = Generic.Users.get(user).authhandle;
                                String bname = Generic.Users.get(whom).authhandle;
                                if (((SrvChannel) who).getChannels().get(whatchan).getUsers().containsKey(aname)) {
                                    if (((SrvChannel) who).getChannels().get(whatchan).getUsers().containsKey(bname)) {
                                        SrvChannel_channel.ChanAccess alevel = ((SrvChannel) who).getChannels().get(whatchan).getUsers().get(Generic.Users.get(user).authhandle);
                                        SrvChannel_channel.ChanAccess blevel = ((SrvChannel) who).getChannels().get(whatchan).getUsers().get(Generic.Users.get(whom).authhandle);
                                        if (alevel.ordinal() > blevel.ordinal()) {
                                            if (newacc.ordinal() < alevel.ordinal()) {
                                                ((SrvChannel) who).getChannels().get(whatchan).getUsers().remove(Generic.Users.get(whom).authhandle);
                                                ((SrvChannel) who).getChannels().get(whatchan).getUsers().put(Generic.Users.get(whom).authhandle, newacc);
                                                ((SrvAuth) Configuration.getSvc().get(Configuration.authservice)).getUsers().get(Generic.Users.get(whom).authhandle).WhereAccess.put(whatchan, newacc.toString());
                                                Generic.curProtocol.outPRVMSG(who, replyto, "User Changed!");
                                            } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: You cannot change a user to higher access than yourself!");
                                        } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: You cannot change a with higher access than yourself!");
                                    } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: User does not have access to channel!");
                                } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: You have no access to channel!");
                            } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: You cannot change yourself!");
                        } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: You are not authed!");
                    } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: User is not authed!");
                } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: No such user!");
            } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: Not a registered channel!");
        } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: Not a channel!");
    }
}