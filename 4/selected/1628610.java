package org.dreamspeak.lib.protocol.packets.inbound.reliablecontent;

import java.nio.ByteBuffer;
import org.dreamspeak.lib.data.Channel;
import org.dreamspeak.lib.data.ChannelList;
import org.dreamspeak.lib.data.Player;
import org.dreamspeak.lib.data.PlayerList;
import org.dreamspeak.lib.protocol.ProtocolException;
import org.dreamspeak.lib.protocol.packets.ResponsibleForPacketTypes;

/**
 * TODO: Proper documentation
 * 
 * @author avithan
 */
@ResponsibleForPacketTypes(0x0067)
public class PlayerChannelChange extends PlayerListUpdate {

    public PlayerChannelChange(short packetType, ByteBuffer data) throws ProtocolException {
        super(packetType, data);
        playerId = data.getInt(0x00);
        fromChannelId = data.getInt(0x04);
        toChannelId = data.getInt(0x08);
    }

    final int fromChannelId;

    final int toChannelId;

    public int getFromChannelId() {
        return fromChannelId;
    }

    public int getToChannelId() {
        return toChannelId;
    }

    final int playerId;

    public int getPlayerId() {
        return playerId;
    }

    @Override
    public void processUpdate(PlayerList playerList) throws ProtocolException {
        Player p = playerList.get(getPlayerId());
        if (p == null) {
            throw new ProtocolException("Recieved PlayerChannelChange for unknown player with id " + playerId);
        }
        ChannelList cl = playerList.getChannelList();
        Channel channelFrom = cl.getChannelById(getFromChannelId());
        Channel channelTo = cl.getChannelById(getToChannelId());
        if (channelFrom == null || channelTo == null) {
            throw new ProtocolException("Recieved PlayerChannelChange for at least one unknown Channel with ids [from:" + getFromChannelId() + " , to:" + getToChannelId() + "]");
        }
        if (p.getCurrentChannel() != channelFrom) {
            throw new ProtocolException("Recieved PlayerChannelChange for player " + getPlayerId() + " with wrong data for current channel [known:" + p.getCurrentChannel().getId() + " , recieved:" + getFromChannelId() + "]");
        }
        p.setCurrentChannel(channelTo);
    }
}
