package com.l2jserver.gameserver.network.clientpackets;

import com.l2jserver.gameserver.model.L2Party;
import com.l2jserver.gameserver.model.L2Skill;
import com.l2jserver.gameserver.model.L2World;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ExAskJoinMPCC;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

/**
 * Format: (ch) S
 * @author chris_00
 *
 * D0 0D 00 5A 00 77 00 65 00 72 00 67 00 00 00
 *
 */
public final class RequestExAskJoinMPCC extends L2GameClientPacket {

    private static final String _C__D0_0D_REQUESTEXASKJOINMPCC = "[C] D0:0D RequestExAskJoinMPCC";

    private String _name;

    @Override
    protected void readImpl() {
        _name = readS();
    }

    @Override
    protected void runImpl() {
        L2PcInstance activeChar = getClient().getActiveChar();
        if (activeChar == null) return;
        L2PcInstance player = L2World.getInstance().getPlayer(_name);
        if (player == null) return;
        if (activeChar.isInParty() && player.isInParty() && activeChar.getParty().equals(player.getParty())) return;
        SystemMessage sm;
        if (activeChar.isInParty()) {
            L2Party activeParty = activeChar.getParty();
            if (activeParty.getLeader().equals(activeChar)) {
                if (activeParty.isInCommandChannel() && activeParty.getCommandChannel().getChannelLeader().equals(activeChar)) {
                    if (player.isInParty()) {
                        if (player.getParty().isInCommandChannel()) {
                            sm = new SystemMessage(SystemMessageId.C1_ALREADY_MEMBER_OF_COMMAND_CHANNEL);
                            sm.addString(player.getName());
                            activeChar.sendPacket(sm);
                        } else {
                            askJoinMPCC(activeChar, player);
                        }
                    } else {
                        activeChar.sendMessage("Your target has no Party.");
                    }
                } else if (activeParty.isInCommandChannel() && !activeParty.getCommandChannel().getChannelLeader().equals(activeChar)) {
                    sm = new SystemMessage(SystemMessageId.CANNOT_INVITE_TO_COMMAND_CHANNEL);
                    activeChar.sendPacket(sm);
                } else {
                    if (player.isInParty()) {
                        if (player.getParty().isInCommandChannel()) {
                            sm = new SystemMessage(SystemMessageId.C1_ALREADY_MEMBER_OF_COMMAND_CHANNEL);
                            sm.addString(player.getName());
                            activeChar.sendPacket(sm);
                        } else {
                            askJoinMPCC(activeChar, player);
                        }
                    } else {
                        activeChar.sendMessage("Your target has no Party.");
                    }
                }
            } else {
                sm = new SystemMessage(SystemMessageId.CANNOT_INVITE_TO_COMMAND_CHANNEL);
                activeChar.sendPacket(sm);
            }
        }
    }

    private void askJoinMPCC(L2PcInstance requestor, L2PcInstance target) {
        boolean hasRight = false;
        if (requestor.getClan() != null && requestor.getClan().getLeaderId() == requestor.getObjectId() && requestor.getClan().getLevel() >= 5) hasRight = true; else if (requestor.getInventory().getItemByItemId(8871) != null) hasRight = true; else if (requestor.getPledgeClass() >= 5) {
            for (L2Skill skill : requestor.getAllSkills()) {
                if (skill.getId() == 391) {
                    hasRight = true;
                    break;
                }
            }
        }
        if (!hasRight) {
            requestor.sendPacket(new SystemMessage(SystemMessageId.COMMAND_CHANNEL_ONLY_BY_LEVEL_5_CLAN_LEADER_PARTY_LEADER));
            return;
        }
        if (!target.isProcessingRequest()) {
            requestor.onTransactionRequest(target);
            SystemMessage sm = new SystemMessage(SystemMessageId.COMMAND_CHANNEL_CONFIRM_FROM_C1);
            sm.addString(requestor.getName());
            target.getParty().getLeader().sendPacket(sm);
            target.getParty().getLeader().sendPacket(new ExAskJoinMPCC(requestor.getName()));
            requestor.sendMessage("You invited " + target.getName() + " to your Command Channel.");
        } else {
            SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_BUSY_TRY_LATER);
            sm.addString(target.getName());
            requestor.sendPacket(sm);
        }
    }

    @Override
    public String getType() {
        return _C__D0_0D_REQUESTEXASKJOINMPCC;
    }
}
