package com.epicsagaonline.bukkit.EpicZones.integration;

import com.epicsagaonline.bukkit.EpicZones.Config;
import com.epicsagaonline.bukkit.EpicZones.EpicZones;
import com.epicsagaonline.bukkit.EpicZones.General;
import com.epicsagaonline.bukkit.EpicZones.objects.EpicZone;
import com.epicsagaonline.bukkit.EpicZones.objects.EpicZonePlayer;

public class HeroChatIntegration {

    public static void joinChat(String zoneTag, EpicZonePlayer ezp) {
        if (Config.enableHeroChat) {
            if (EpicZones.heroChat != null && EpicZones.heroChat.isEnabled()) {
                EpicZone theZone = General.myZones.get(zoneTag);
                if (theZone != null) {
                    if (EpicZones.heroChat.getChannelManager() != null) {
                        while (EpicZones.heroChat.getChannelManager().getChannel(theZone.getTag()) == null && theZone.hasParent()) {
                            theZone = General.myZones.get(theZone.getParent().getTag());
                        }
                        if (!ezp.getPreviousZoneTag().equals(theZone.getTag())) {
                            if (EpicZones.heroChat.getChannelManager().getChannel(theZone.getTag()) != null) {
                                EpicZones.heroChat.getChannelManager().getChannel(theZone.getTag()).addPlayer(ezp.getName());
                                if (ezp.getHasMoved()) {
                                    EpicZones.heroChat.getChannelManager().setActiveChannel(ezp.getName(), zoneTag);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void leaveChat(String zoneTag, EpicZonePlayer ezp) {
        if (Config.enableHeroChat) {
            if (EpicZones.heroChat != null && EpicZones.heroChat.isEnabled()) {
                if (EpicZones.heroChat.getChannelManager().getChannel(zoneTag) != null) {
                    EpicZones.heroChat.getChannelManager().getChannel(zoneTag).removePlayer(ezp.getName());
                    EpicZones.heroChat.getChannelManager().setActiveChannel(ezp.getName(), EpicZones.heroChat.getChannelManager().getDefaultChannel().getName());
                }
            }
        }
    }
}
