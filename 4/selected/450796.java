package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * @author -Wooden- D0 0F 00 5A 00 77 00 65 00 72 00 67 00 00 00
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
            target.getParty().getCommandChannel().removeParty(target.getParty());
            SystemMessage sm = SystemMessage.sendString("Your party was dismissed from the CommandChannel.");
            target.getParty().broadcastToPartyMembers(sm);
            sm = SystemMessage.sendString(target.getParty().getPartyMembers().get(0).getName() + "'s party was dismissed from the CommandChannel.");
        } else activeChar.sendMessage("Incorrect Target");
    }

    @Override
    public String getType() {
        return _C__D0_0F_REQUESTEXOUSTFROMMPCC;
    }
}
