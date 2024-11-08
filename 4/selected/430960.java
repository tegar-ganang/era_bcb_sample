package net.jetrix.commands;

import java.util.Locale;
import net.jetrix.messages.channel.CommandMessage;
import net.jetrix.messages.channel.PlineMessage;
import net.jetrix.config.*;
import net.jetrix.*;

/**
 * Switch between preconfigured settings.
 *
 * @author Gamereplay
 * @author Emmanuel Bourg
 * @version $Revision: 872 $, $Date: 2011-06-27 17:26:26 -0400 (Mon, 27 Jun 2011) $
 */
public class ModeCommand extends AbstractCommand {

    private static int[][] modes = { { 15, 14, 14, 14, 14, 14, 14, 1, 0 }, { 10, 15, 15, 15, 15, 15, 15, 1, 1 }, { 20, 14, 13, 13, 13, 13, 14, 1, 0 }, { 10, 15, 15, 15, 15, 15, 15, 2, 1 }, { 25, 12, 13, 13, 12, 12, 13, 1, 0 }, { 0, 17, 17, 17, 16, 16, 17, 1, 0 }, { 100, 0, 0, 0, 0, 0, 0, 1, 0 }, { 0, 0, 0, 0, 50, 50, 0, 1, 1 }, { 0, 0, 50, 50, 0, 0, 0, 1, 1 }, { 0, 0, 0, 0, 0, 0, 100, 1, 1 } };

    static {
        Language.getInstance().addResources("command.mode");
    }

    public String getAlias() {
        return "mode";
    }

    public String getUsage(Locale locale) {
        return "/" + getAlias() + " <0-" + (modes.length - 1) + ">";
    }

    public void updateSetting(Settings settings, int[] mode) {
        Occurancy<Block> occurancy = new Occurancy<Block>();
        for (Block block : Block.values()) {
            occurancy.setOccurancy(block, mode[block.ordinal()]);
        }
        settings.setBlockOccurancy(occurancy);
        settings.setLinesPerSpecial(mode[7]);
        settings.setSpecialAdded(mode[8]);
    }

    public void execute(CommandMessage message) {
        Client client = (Client) message.getSource();
        Channel channel = client.getChannel();
        if (message.getParameterCount() == 0) {
            Locale locale = client.getUser().getLocale();
            for (int i = 0; i < modes.length; i++) {
                Message tmode = new PlineMessage("<red>/" + getAlias() + " <aqua>" + i + "</aqua> : <darkBlue>" + Language.getText("command.mode.message" + i, locale));
                client.send(tmode);
            }
        } else {
            int param = -1;
            try {
                param = Integer.parseInt(message.getParameter(0));
            } catch (NumberFormatException e) {
            }
            if (param >= 0 && param < modes.length) {
                updateSetting(channel.getConfig().getSettings(), modes[param]);
                PlineMessage enabled = new PlineMessage();
                enabled.setKey("command.mode.enabled", "key:command.mode.message" + param);
                channel.send(enabled);
            } else {
                PlineMessage error = new PlineMessage();
                error.setText("<red>/" + getAlias() + "</red> <blue><0-" + (modes.length - 1) + "></blue>");
                client.send(error);
            }
        }
    }
}
