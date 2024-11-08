package handlers.chathandlers;

import l2.universe.gameserver.handler.IChatHandler;
import l2.universe.gameserver.model.actor.instance.L2PcInstance;
import l2.universe.gameserver.network.serverpackets.CreatureSay;

/**
 * A chat handler
 *
 * @author  durgus
 */
public class ChatPartyRoomCommander implements IChatHandler {

    private static final int[] COMMAND_IDS = { 15 };

    /**
	 * Handle chat type 'party room commander'
	 */
    @Override
    public void handleChat(final int type, final L2PcInstance activeChar, final String target, final String text) {
        if (activeChar.isInParty()) {
            if (activeChar.getParty().isInCommandChannel() && activeChar.getParty().getCommandChannel().getChannelLeader().equals(activeChar)) {
                final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
                activeChar.getParty().getCommandChannel().broadcastCSToChannelMembers(cs, activeChar);
            }
        }
    }

    /**
	 * Returns the chat types registered to this handler
	 */
    @Override
    public int[] getChatTypeList() {
        return COMMAND_IDS;
    }
}
