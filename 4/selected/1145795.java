package org.dreamspeak.lib.protocol.packets.inbound.reliablecontent;

import java.nio.ByteBuffer;
import org.dreamspeak.lib.data.Channel;
import org.dreamspeak.lib.data.Player;
import org.dreamspeak.lib.data.PlayerChannelPrivilegeSet;
import org.dreamspeak.lib.data.PlayerList;
import org.dreamspeak.lib.data.PlayerServerPrivilegeSet;
import org.dreamspeak.lib.data.PlayerStatusSet;
import org.dreamspeak.lib.protocol.ProtocolException;
import org.dreamspeak.lib.protocol.packets.ResponsibleForPacketTypes;

/**
 * TODO: Proper documentation
 * 
 * @author avithan
 */
@ResponsibleForPacketTypes(0x0064)
public class PlayerJoined extends PlayerListUpdate {

    public PlayerJoined(short packetType, ByteBuffer data) throws ProtocolException {
        super(packetType, data);
        byte raw[] = data.array();
        playerId = data.getInt(0x00);
        channelId = data.getInt(0x04);
        byte rawChannelPrivileges = updateBuffer.get(0x08);
        byte rawServerPrivileges = updateBuffer.get(0x0A);
        byte rawStatus = updateBuffer.get(0x0C);
        channelPrivileges = PlayerChannelPrivilegeSet.fromByte(rawChannelPrivileges);
        serverPrivileges = PlayerServerPrivilegeSet.fromByte(rawServerPrivileges);
        status = PlayerStatusSet.fromByte(rawStatus);
        int nicknameLength = data.get(0x0E);
        int nicknameOffset = 0x0F;
        StringBuilder rawNickname = new StringBuilder();
        for (int i = 0; i < nicknameLength; i++) {
            rawNickname.append((char) raw[i + nicknameOffset]);
        }
        nickname = rawNickname.toString();
    }

    final PlayerChannelPrivilegeSet channelPrivileges;

    final PlayerServerPrivilegeSet serverPrivileges;

    final PlayerStatusSet status;

    final String nickname;

    final int playerId;

    final int channelId;

    public int getPlayerId() {
        return playerId;
    }

    public int getChannelId() {
        return channelId;
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public void processUpdate(PlayerList playerList) throws ProtocolException {
        Player player = new Player(getPlayerId(), getNickname());
        Channel channel = playerList.getChannelList().getChannelById(getChannelId());
        if (channel == null) throw new ProtocolException("New Player with unknown channelId " + getChannelId());
        player.getChannelPrivileges().addAll(channelPrivileges);
        player.getServerPrivileges().addAll(serverPrivileges);
        player.getStatus().addAll(status);
        playerList.addPlayer(player);
    }
}
