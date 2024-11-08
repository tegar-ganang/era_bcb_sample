package handlers.usercommandhandlers;

import l2.universe.gameserver.handler.IUserCommandHandler;
import l2.universe.gameserver.model.L2CommandChannel;
import l2.universe.gameserver.model.actor.instance.L2PcInstance;
import l2.universe.gameserver.network.SystemMessageId;
import l2.universe.gameserver.network.serverpackets.SystemMessage;

/**
 *
 * @author  Chris
 */
public class ChannelDelete implements IUserCommandHandler {

    private static final int[] COMMAND_IDS = { 93 };

    @Override
    public boolean useUserCommand(final int id, final L2PcInstance activeChar) {
        if (id != COMMAND_IDS[0]) return false;
        if (activeChar.getParty() == null || activeChar.getParty().getCommandChannel() == null) return false;
        if (activeChar.getParty().isLeader(activeChar) && activeChar.getParty().getCommandChannel().getChannelLeader().equals(activeChar)) {
            final L2CommandChannel channel = activeChar.getParty().getCommandChannel();
            SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.COMMAND_CHANNEL_DISBANDED);
            channel.broadcastToChannelMembers(sm);
            sm = null;
            channel.disbandChannel();
            return true;
        }
        return false;
    }

    @Override
    public int[] getUserCommandList() {
        return COMMAND_IDS;
    }
}
