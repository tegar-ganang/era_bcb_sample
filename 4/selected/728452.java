package net.sf.odinms.client.messages.commands;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.messages.Command;
import net.sf.odinms.client.messages.CommandDefinition;
import net.sf.odinms.client.messages.IllegalCommandSyntaxException;
import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.net.channel.ChannelServer;

public class CustomCommands implements Command {

    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception, IllegalCommandSyntaxException {
        ChannelServer cserv = c.getChannelServer();
        if (splitted[0].equals("!spy")) {
            double var;
            double var2;
            int str;
            int dex;
            int intel;
            int luk;
            int meso;
            int maxhp;
            int maxmp;
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            var = victim.getJumpMod();
            var2 = victim.getSpeedMod();
            str = victim.getStr();
            dex = victim.getDex();
            intel = victim.getInt();
            luk = victim.getLuk();
            meso = victim.getMeso();
            maxhp = victim.getCurrentMaxHp();
            maxmp = victim.getCurrentMaxMp();
            mc.dropMessage("JumpMod is" + var + " and Speedmod is" + var2 + "!");
            mc.dropMessage("Players stats are:");
            mc.dropMessage(" Str: " + str + ", Dex: " + dex + ", Int: " + intel + ", Luk: " + luk + " .");
            mc.dropMessage("Player has " + meso + "mesos.");
            mc.dropMessage("Max hp is" + maxhp + " Max mp is" + maxmp + ".");
        } else if (splitted[0].equals("!maxall")) {
            int max = Integer.parseInt(splitted[1]);
            MapleCharacter player = c.getPlayer();
            player.setStr(max);
            player.setDex(max);
            player.setInt(max);
            player.setLuk(max);
            player.setMaxMp(max);
            player.setMaxHp(max);
            player.updateSingleStat(MapleStat.STR, player.getStr());
            player.updateSingleStat(MapleStat.DEX, player.getStr());
            player.updateSingleStat(MapleStat.INT, player.getStr());
            player.updateSingleStat(MapleStat.LUK, player.getStr());
            player.updateSingleStat(MapleStat.MAXHP, player.getStr());
            player.updateSingleStat(MapleStat.MAXMP, player.getStr());
        } else if (splitted[0].equals("!giftnx")) {
            MapleCharacter victim1 = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            int points = Integer.parseInt(splitted[2]);
            victim1.modifyCSPoints(1, points);
            mc.dropMessage("NX Cash has been gifted.");
        } else if (splitted[0].equals("!fame")) {
            MapleCharacter player = c.getPlayer();
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            int fame = Integer.parseInt(splitted[2]);
            victim.addFame(fame);
            player.updateSingleStat(MapleStat.FAME, fame);
        } else if (splitted[0].equals("!heal")) {
            MapleCharacter player = c.getPlayer();
            player.setHp(player.getMaxHp());
            player.updateSingleStat(MapleStat.HP, player.getMaxHp());
            player.setMp(player.getMaxMp());
            player.updateSingleStat(MapleStat.MP, player.getMaxMp());
        } else if (splitted[0].equals("!kill")) {
            for (String name : splitted) {
                if (!name.equals(splitted[0])) {
                    MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(name);
                    if (victim != null) {
                        victim.setHp(0);
                        victim.setMp(0);
                        victim.updateSingleStat(MapleStat.HP, 0);
                        victim.updateSingleStat(MapleStat.MP, 0);
                    }
                }
            }
        } else if (splitted[0].equals("!dcall")) {
            for (ChannelServer cservers : ChannelServer.getAllInstances()) {
                for (MapleCharacter mch : cservers.getPlayerStorage().getAllCharacters()) {
                    mch.getClient().getSession().close();
                    mch.getClient().disconnect();
                }
            }
        } else if (splitted[0].equals("!healmap")) {
            for (MapleCharacter mch : c.getPlayer().getMap().getCharacters()) {
                if (mch != null) {
                    mch.setHp(mch.getMaxHp());
                    mch.setMp(mch.getMaxMp());
                    mch.updateSingleStat(MapleStat.HP, mch.getMaxHp());
                    mch.updateSingleStat(MapleStat.MP, mch.getMaxMp());
                }
            }
        } else if (splitted[0].equals("!unstick")) {
            MapleCharacter victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
            victim.getMap().removePlayer(victim);
            victim.getClient().getChannelServer().removePlayer(victim);
            victim.getClient().disconnect();
            victim.unstick();
            mc.dropMessage(victim + " has been unstuck.");
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[] { new CommandDefinition("spy", "", "", 1), new CommandDefinition("maxall", "", "", 10), new CommandDefinition("giftnx", "", "", 5), new CommandDefinition("fame", "", "", 10), new CommandDefinition("heal", "", "", 5), new CommandDefinition("kill", "", "", 1), new CommandDefinition("dcall", "", "", 10), new CommandDefinition("spy", "", "", 1), new CommandDefinition("healmap", "", "", 5), new CommandDefinition("unstick", "", "", 1) };
    }
}
