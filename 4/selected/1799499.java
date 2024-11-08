package ecks.services.modules.SrvAuth;

import ecks.Configuration;
import ecks.Logging;
import ecks.protocols.Generic;
import ecks.services.Service;
import ecks.services.SrvAuth;
import ecks.services.SrvChannel;
import ecks.services.SrvChannel_channel;
import ecks.services.modules.CommandDesc;
import ecks.services.modules.bCommand;
import ecks.util;
import java.util.Map;

public class Unregister extends bCommand {

    public final CommandDesc Desc = new CommandDesc("unregister", 1, true, CommandDesc.access_levels.A_HELPER, "Unregisters an account");

    public CommandDesc getDesc() {
        return Desc;
    }

    public void handle_command(Service who, String user, String replyto, String arguments) {
        SrvAuth temp = ((SrvAuth) who);
        String args[] = arguments.split(" ");
        String tU = args[0].toLowerCase();
        if (args.length == 1) {
            if (util.sanitize(tU)) {
                if (temp.getUsers().containsKey(tU)) {
                    if ((temp.getUsers().get(Generic.Users.get(user).authhandle)).getAccess().ordinal() > (temp.getUsers().get(tU).getAccess().ordinal())) {
                        for (String e : temp.getUsers().get(tU).WhereAccess.keySet()) {
                            if (((SrvChannel) Configuration.getSvc().get(Configuration.chanservice)).getChannels().get(e).getUsers().get(tU) == SrvChannel_channel.ChanAccess.C_OWNER) {
                                boolean promoted = false;
                                int threshold = SrvChannel_channel.ChanAccess.C_OWNER.ordinal();
                                while (!promoted) {
                                    threshold--;
                                    if (threshold < SrvChannel_channel.ChanAccess.C_PEON.ordinal()) {
                                        ((SrvChannel) Configuration.getSvc().get(Configuration.chanservice)).getChannels().remove(e);
                                        Generic.srvPart(who, e, "Channel Unregistered (owner unregistered, no other users).");
                                        Logging.info("SRVCHAN", "Channel " + e + " unregistered by virtue of having no users left.");
                                        promoted = true;
                                        break;
                                    }
                                    for (Map.Entry<String, SrvChannel_channel.ChanAccess> z : (((SrvChannel) Configuration.getSvc().get(Configuration.chanservice)).getChannels().get(e).getUsers().entrySet())) {
                                        if (z.getValue().ordinal() >= threshold) {
                                            ((SrvChannel) Configuration.getSvc().get(Configuration.chanservice)).getChannels().get(e).getUsers().remove(Generic.Users.get(user).authhandle);
                                            ((SrvChannel) Configuration.getSvc().get(Configuration.chanservice)).getChannels().get(e).getUsers().put(Generic.Users.get(user).authhandle, SrvChannel_channel.ChanAccess.C_OWNER);
                                            promoted = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            ((SrvChannel) Configuration.getSvc().get(Configuration.chanservice)).getChannels().get(e).getUsers().remove(tU);
                        }
                        temp.getUsers().remove(tU);
                        Generic.curProtocol.outMODE(who, Generic.Users.get(tU), "-r", "");
                        Generic.curProtocol.outPRVMSG(who, replyto, "User account removed.");
                    } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: User has equal/higher access than you!");
                } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: No such username is registered");
            } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: Invalid username.");
        } else Generic.curProtocol.outPRVMSG(who, replyto, "Error: Invalid Arguments. Usage: unregister [username]");
    }
}
