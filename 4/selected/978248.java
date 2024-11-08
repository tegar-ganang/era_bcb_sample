package com.planet_ink.coffee_mud.Abilities.Properties;

import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;
import java.util.*;

@SuppressWarnings("unchecked")
public class Prop_NoChannel extends Property {

    public String ID() {
        return "Prop_NoChannel";
    }

    public String name() {
        return "Channel Neutralizing";
    }

    protected int canAffectCode() {
        return Ability.CAN_ROOMS | Ability.CAN_AREAS;
    }

    protected Vector channels = null;

    protected boolean receive = true;

    protected boolean sendOK = false;

    public String accountForYourself() {
        return "No Channeling Field";
    }

    public void setMiscText(String newText) {
        super.setMiscText(newText);
        channels = CMParms.parseSemicolons(newText.toUpperCase(), true);
        int x = channels.indexOf("SENDOK");
        sendOK = (x >= 0);
        if (sendOK) channels.removeElementAt(x);
        x = channels.indexOf("QUIET");
        receive = (x < 0);
        if (!receive) channels.removeElementAt(x);
    }

    public boolean okMessage(Environmental myHost, CMMsg msg) {
        if (!super.okMessage(myHost, msg)) return false;
        if ((msg.othersMajor() & CMMsg.MASK_CHANNEL) > 0) {
            int channelInt = msg.othersMinor() - CMMsg.TYP_CHANNEL;
            if ((msg.source() == affected) || (!(affected instanceof MOB)) && ((channels == null) || (channels.size() == 0) || (channels.contains(CMLib.channels().getChannelName(channelInt))))) {
                if (!sendOK) {
                    if (msg.source() == affected) msg.source().tell("Your message drifts into oblivion."); else if ((!(affected instanceof MOB)) && (CMLib.map().roomLocation(affected) == msg.source().location())) msg.source().tell("This is a no-channel area.");
                    return false;
                }
                if (!receive) {
                    if ((msg.source() != affected) || ((!(affected instanceof MOB)) && (CMLib.map().roomLocation(affected) != msg.source().location()))) return false;
                }
            }
        }
        return true;
    }
}
