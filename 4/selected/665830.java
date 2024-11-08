package com.epicsagaonline.bukkit.EpicZones;

import org.bukkit.entity.Player;

public class HeroChatIntegration {

    public static void joinChat(String zoneTag, EpicZonePlayer ezp, Player player) {
        if (General.config.enableHeroChat) {
            if (EpicZones.heroChat != null && EpicZones.heroChat.isEnabled()) {
                Zone theZone = General.myZones.get(zoneTag);
                if (theZone != null) {
                    if (EpicZones.heroChat.getChannelManager() != null) {
                        while (EpicZones.heroChat.getChannelManager().getChannel(theZone.getTag()) == null && theZone.hasParent()) {
                            theZone = General.myZones.get(theZone.getParent().getTag());
                        }
                        if (!ezp.getPreviousZoneTag().equals(theZone.getTag())) {
                            if (EpicZones.heroChat.getChannelManager().getChannel(theZone.getTag()) != null) {
                                EpicZones.heroChat.getChannelManager().getChannel(theZone.getTag()).addPlayer(player.getName());
                                if (ezp.getHasMoved()) {
                                    EpicZones.heroChat.getChannelManager().setActiveChannel(player.getName(), zoneTag);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void leaveChat(String zoneTag, Player player) {
        if (General.config.enableHeroChat) {
            if (EpicZones.heroChat != null && EpicZones.heroChat.isEnabled()) {
                if (EpicZones.heroChat.getChannelManager().getChannel(zoneTag) != null) {
                    EpicZones.heroChat.getChannelManager().getChannel(zoneTag).removePlayer(player.getName());
                }
            }
        }
    }
}
