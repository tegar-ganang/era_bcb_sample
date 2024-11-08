package ecks.services.modules.SrvChannel;

import ecks.protocols.Generic;
import ecks.services.Service;
import ecks.services.SrvChannel;
import ecks.services.SrvChannel_channel;
import ecks.services.modules.CommandDesc;
import ecks.services.modules.bCommand;
import java.util.Map;

public class DumpChans extends bCommand {

    public final CommandDesc Desc = new CommandDesc("dumpchans", 0, true, CommandDesc.access_levels.A_SRA, "Dumps registered channels.");

    public CommandDesc getDesc() {
        return Desc;
    }

    public void handle_command(Service who, String user, String replyto, String arguments) {
        for (Map.Entry<String, SrvChannel_channel> t : ((SrvChannel) who).getChannels().entrySet()) {
            Generic.curProtocol.outPRVMSG(who, replyto, "Entry " + t.toString());
        }
    }
}
