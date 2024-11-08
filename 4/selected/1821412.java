package net.sf.odinms.net.channel.handler;

import java.util.concurrent.ScheduledFuture;
import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.MapleBuffStat;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleCharacter.CancelCooldownAction;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleJob;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.anticheat.CheatingOffense;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.server.MapleStatEffect;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import net.sf.odinms.server.maps.FakeCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnergyChargeHandler extends AbstractDealDamageHandler {

    private static Logger log = LoggerFactory.getLogger(EnergyChargeHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        AttackInfo attack = parseDamage(slea, false);
        MapleCharacter player = c.getPlayer();
        MaplePacket packet = MaplePacketCreator.closeRangeAttack(player.getId(), attack.skill, attack.stance, attack.numAttackedAndDamage, attack.allDamage, attack.speed);
        player.getMap().broadcastMessage(player, packet, false, true);
        int numFinisherOrbs = 0;
        Integer comboBuff = player.getBuffedValue(MapleBuffStat.COMBO);
        ISkill energycharge = SkillFactory.getSkill(5110001);
        int energyChargeSkillLevel = player.getSkillLevel(energycharge);
        if (attack.numAttacked > 0) {
            if ((player.getJob().equals(MapleJob.CRUSADER) || player.getJob().equals(MapleJob.HERO)) || ChannelServer.getInstance(c.getChannel()).getOrbGain()) {
                if (attack.skill != 1111008 && comboBuff != null) {
                    player.handleOrbgain();
                }
            }
        }
        int maxdamage = c.getPlayer().getCurrentMaxBaseDamage();
        int attackCount = 1;
        if (attack.skill != 0) {
            MapleStatEffect effect = attack.getAttackEffect(c.getPlayer());
            attackCount = effect.getAttackCount();
            maxdamage *= effect.getDamage() / 100.0;
            maxdamage *= attackCount;
        }
        maxdamage = Math.min(maxdamage, 99999);
        if (attack.skill == 4211006) {
            maxdamage = 700000;
        } else if (numFinisherOrbs > 0) {
            maxdamage *= numFinisherOrbs;
        } else if (comboBuff != null) {
            ISkill combo = SkillFactory.getSkill(1111002);
            int comboLevel = player.getSkillLevel(combo);
            MapleStatEffect comboEffect = combo.getEffect(comboLevel);
            double comboMod = 1.0 + (comboEffect.getDamage() / 100.0 - 1.0) * (comboBuff.intValue() - 1);
            maxdamage *= comboMod;
        }
        applyAttack(attack, player, maxdamage, attackCount);
        if (c.getPlayer().hasFakeChar()) {
            for (FakeCharacter ch : c.getPlayer().getFakeChars()) {
                MaplePacket packett = MaplePacketCreator.closeRangeAttack(ch.getFakeChar().getId(), attack.skill, attack.stance, attack.numAttackedAndDamage, attack.allDamage, attack.speed);
                player.getMap().broadcastMessage(ch.getFakeChar(), packett, false, true);
                applyAttack(attack, ch.getFakeChar(), maxdamage, attackCount);
            }
        }
    }
}
