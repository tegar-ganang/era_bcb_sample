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
@ResponsibleForPacketTypes(0x0007)
public class PlayerListUpdate extends ReliableContent {

    final ByteBuffer updateBuffer;

    public PlayerListUpdate(short packetType, ByteBuffer data) throws ProtocolException {
        super(packetType, data);
        this.updateBuffer = data;
    }

    /**
	 * Applies changes in this update to the playList
	 * 
	 * @param playerList
	 */
    public void processUpdate(PlayerList playerList) throws ProtocolException {
        updateBuffer.rewind();
        byte[] raw = updateBuffer.array();
        short plannedPlayers = updateBuffer.getShort(0x00);
        int currentPlayer = 1;
        int chanOffset = 4;
        int seqPos = 0;
        try {
            while (currentPlayer <= plannedPlayers) {
                chanOffset = ((currentPlayer - 1) * 0x2C) + 4;
                int playerId = updateBuffer.getInt(chanOffset + 0x00);
                int channelId = updateBuffer.getInt(chanOffset + 0x04);
                byte channelPrivileges = updateBuffer.get(chanOffset + 0x08);
                byte serverPrivileges = updateBuffer.get(chanOffset + 0x0A);
                byte status = updateBuffer.get(chanOffset + 0x0C);
                StringBuffer nick = new StringBuffer();
                seqPos = chanOffset + 0x0F;
                while (raw[seqPos] != 0x00 && seqPos < raw.length) {
                    nick.append((char) raw[seqPos]);
                    seqPos++;
                }
                seqPos++;
                Player p;
                Player alreadyExists = playerList.get(playerId);
                if (alreadyExists != null) {
                    p = alreadyExists;
                    p.getChannelPrivileges().clear();
                    p.getServerPrivileges().clear();
                    p.getStatus().clear();
                } else {
                    p = new Player(playerId, nick.toString());
                }
                p.setNickname(nick.toString());
                Channel playerChannel = null;
                playerChannel = playerList.getChannelList().getChannelById(channelId);
                if (playerChannel == null) {
                    throw new ProtocolException("Channel " + channelId + " is unknown, so Player with ID " + playerId + " is lost in the void o.x!");
                }
                p.setCurrentChannel(playerChannel);
                p.getChannelPrivileges().addAll(PlayerChannelPrivilegeSet.fromByte(channelPrivileges));
                p.getServerPrivileges().addAll(PlayerServerPrivilegeSet.fromByte(serverPrivileges));
                p.getStatus().addAll(PlayerStatusSet.fromByte(status));
                if (alreadyExists == null) {
                    playerList.addPlayer(p);
                }
                currentPlayer++;
            }
        } catch (IndexOutOfBoundsException ioe) {
            throw new ProtocolException("Broken PlayerListUpdate package...", ioe);
        }
        System.out.println("Player update there!\n");
        for (Player p : playerList.values()) {
            System.out.println("\t" + p.toString() + "\n");
        }
    }
}
