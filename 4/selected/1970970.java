package com.l2jserver.gameserver.network.serverpackets;

import com.l2jserver.gameserver.model.L2CommandChannel;
import com.l2jserver.gameserver.model.L2Party;

/**
 *
 * @author  chris_00
 * ch sdd d[sdd]
 */
public class ExMultiPartyCommandChannelInfo extends L2GameServerPacket {

    private static final String _S__FE_31_EXMULTIPARTYCOMMANDCHANNELINFO = "[S] FE:31 ExMultiPartyCommandChannelInfo";

    private L2CommandChannel _channel;

    public ExMultiPartyCommandChannelInfo(L2CommandChannel channel) {
        this._channel = channel;
    }

    /**
     * @see com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket#getType()
     */
    @Override
    public String getType() {
        return _S__FE_31_EXMULTIPARTYCOMMANDCHANNELINFO;
    }

    /**
     * @see com.l2jserver.gameserver.network.serverpackets.L2GameServerPacket#writeImpl()
     */
    @Override
    protected void writeImpl() {
        if (_channel == null) return;
        writeC(0xfe);
        writeH(0x31);
        writeS(_channel.getChannelLeader().getName());
        writeD(0);
        writeD(_channel.getMemberCount());
        writeD(_channel.getPartys().size());
        for (L2Party p : _channel.getPartys()) {
            writeS(p.getLeader().getName());
            writeD(p.getPartyLeaderOID());
            writeD(p.getMemberCount());
        }
    }
}
