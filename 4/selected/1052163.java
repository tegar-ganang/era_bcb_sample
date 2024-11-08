package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

/**
 * A chat handler
 *
 * @author durgus
 */
public class ChatPartyRoomCommander implements IChatHandler {

    private static final int[] COMMAND_IDS = { 15 };

    /**
	 * Handle chat type 'party room commander'
	 *
	 * @see net.sf.l2j.gameserver.handler.IChatHandler#handleChat(int, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance, java.lang.String)
	 */
    public void handleChat(int type, L2PcInstance activeChar, String target, String text) {
        if (activeChar.isInParty()) if (activeChar.getParty().isInCommandChannel() && activeChar.getParty().getCommandChannel().getChannelLeader().equals(activeChar)) {
            CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
            activeChar.getParty().getCommandChannel().broadcastToChannelMembers(cs);
        }
    }

    /**
	 * Returns the chat types registered to this handler
	 *
	 * @see net.sf.l2j.gameserver.handler.IChatHandler#getChatTypeList()
	 */
    public int[] getChatTypeList() {
        return COMMAND_IDS;
    }
}
