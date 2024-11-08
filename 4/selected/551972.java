package net.jetrix.commands;

import java.util.*;
import net.jetrix.*;
import net.jetrix.messages.*;
import net.jetrix.messages.channel.CommandMessage;

/**
 * Move a player to a new slot or switch two players.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 798 $, $Date: 2009-02-18 10:24:28 -0500 (Wed, 18 Feb 2009) $
 */
public class MoveCommand extends AbstractCommand implements ParameterCommand {

    public String getAlias() {
        return "move";
    }

    public String getUsage(Locale locale) {
        return "/move <" + Language.getText("command.params.player_num", locale) + "> <" + Language.getText("command.params.slot_num", locale) + ">";
    }

    public int getParameterCount() {
        return 2;
    }

    public void execute(CommandMessage m) {
        Client client = (Client) m.getSource();
        int slot1 = m.getIntParameter(0, 0);
        int slot2 = m.getIntParameter(1, 0);
        if (slot1 >= 1 && slot1 <= 6 && slot2 >= 1 && slot2 <= 6 && slot1 != slot2) {
            PlayerSwitchMessage pswitch = new PlayerSwitchMessage();
            pswitch.setSlot1(slot1);
            pswitch.setSlot2(slot2);
            client.getChannel().send(pswitch);
        }
    }
}
