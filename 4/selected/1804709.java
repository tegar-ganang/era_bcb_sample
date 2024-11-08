package net.sf.odinms.net.channel.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.client.MapleCharacter;

/**
 *
 * @author Inu
 */
public class RequestBoatHandler extends AbstractMovementPacketHandler {

    private static Logger log = LoggerFactory.getLogger(RequestBoatHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int mapid = slea.readInt();
        MapleCharacter player = c.getPlayer();
        if (player.getMap().getId() != mapid) {
            log.warn("Player: " + player.getName() + " is trying to find out if boat is there without being in the map he says.");
        }
        MapleMap map = c.getChannelServer().getMapFactory().getMap(mapid);
        if (map.hasBoat() == 2 && !player.GetBoatHere()) {
            player.toggleBoatHere();
            c.getSession().write((MaplePacketCreator.boatPacket(true)));
            return;
        } else if (map.hasBoat() == 1 && (mapid != 200090000 || mapid != 200090010) && player.GetBoatHere()) {
            player.toggleBoatHere();
            c.getSession().write(MaplePacketCreator.boatPacket(false));
            return;
        }
    }
}
