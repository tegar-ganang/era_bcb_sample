package com.l2jserver.gameserver.network.clientpackets;

import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * @author -Wooden-
 *
 * D0 0F 00 5A 00 77 00 65 00 72 00 67 00 00 00
 *
 */
public final class RequestExOustFromMPCC extends L2GameClientPacket {

    private static final String _C__D0_0F_REQUESTEXOUSTFROMMPCC = "[C] D0:0F RequestExOustFromMPCC";

    private String _name;

    @Override
    protected void readImpl() {
        _name = readS();
    }

    @Override
    protected void runImpl() {
        L2PcInstance target = L2World.getInstance().getPlayer(_name);
        L2PcInstance activeChar = getClient().getActiveChar();
        if (target != null && target.isInParty() && activeChar.isInParty() && activeChar.getParty().isInCommandChannel() && target.getParty().isInCommandChannel() && activeChar.getParty().getCommandChannel().getChannelLeader().equals(activeChar)) {
            if (activeChar.equals(target)) return;
            target.getParty().getCommandChannel().removeParty(target.getParty());
            SystemMessage sm = new SystemMessage(SystemMessageId.DISMISSED_FROM_COMMAND_CHANNEL);
            target.getParty().broadcastToPartyMembers(sm);
            if (activeChar.getParty().isInCommandChannel()) {
                sm = new SystemMessage(SystemMessageId.C1_PARTY_DISMISSED_FROM_COMMAND_CHANNEL);
                sm.addString(target.getParty().getLeader().getName());
                activeChar.getParty().getCommandChannel().broadcastToChannelMembers(sm);
            }
        } else {
            activeChar.sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
        }
    }

    @Override
    public String getType() {
        return _C__D0_0F_REQUESTEXOUSTFROMMPCC;
    }
}
