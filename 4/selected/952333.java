package net.sf.odinms.client.messages.commands;

import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.messages.Command;
import net.sf.odinms.client.messages.CommandDefinition;
import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.tools.StringUtil;

public class ArrayCommand implements Command {

    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splittedLine) throws Exception {
        if (splittedLine.length >= 2) {
            if (splittedLine[1].equalsIgnoreCase("*CLEAR")) {
                c.getChannelServer().setArrayString("");
                mc.dropMessage("Array Sucessfully Flushed");
            } else {
                c.getChannelServer().setArrayString(c.getChannelServer().getArrayString() + StringUtil.joinStringFrom(splittedLine, 1));
                mc.dropMessage("Added " + StringUtil.joinStringFrom(splittedLine, 1) + " to the array. Use !array to check.");
            }
        } else mc.dropMessage("Array: " + c.getChannelServer().getArrayString());
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] { new CommandDefinition("array", "[Message to append]", "NOTE: When appending '*CLEAR' array will flush.", 100) };
    }
}
