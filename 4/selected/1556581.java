package net.sf.odinms.net.channel.handler;

import net.sf.odinms.client.IItem;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Inu
 */
public class UseSolomonHandler extends AbstractMaplePacketHandler {

    private static final Logger log = LoggerFactory.getLogger(UseSolomonHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().isAlive() || c.getPlayer().isPvPMap()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        slea.readInt();
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt();
        int solomonid;
        IItem toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        int expgained = 0;
        if (itemId < 2370000) {
        } else {
            solomonid = itemId - 2370000;
            switch(solomonid) {
                case 0:
                    expgained = 100000;
                    break;
                case 1:
                    expgained = 50000;
                    break;
                case 2:
                    expgained = 30000;
                    break;
                case 3:
                    expgained = 20000;
                    break;
                case 4:
                    expgained = 10000;
                    break;
                case 5:
                    expgained = 5000;
                    break;
                case 6:
                    expgained = 3000;
                    break;
                case 7:
                    expgained = 2000;
                    break;
                case 8:
                    expgained = 1000;
                    break;
                case 9:
                    expgained = 500;
                    break;
                case 10:
                    expgained = 300;
                    break;
                case 11:
                    expgained = 200;
                    break;
                case 12:
                    expgained = 100;
                    break;
                default:
                    expgained = 0;
                    break;
            }
            MapleCharacter player = c.getPlayer();
            expgained = expgained * player.getClient().getChannelServer().getExpRate() * player.getClient().getPlayer().getEXPRate() * player.getClient().getPlayer().hasEXPCard();
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
            player.gainExp(expgained, true, true);
        }
    }
}
