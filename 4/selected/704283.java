package com.planet_ink.coffee_mud.Abilities.Thief;

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
import com.planet_ink.coffee_mud.Libraries.interfaces.ChannelsLibrary;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;
import java.util.*;

@SuppressWarnings("unchecked")
public class Thief_Espionage extends ThiefSkill {

    public String ID() {
        return "Thief_Espionage";
    }

    public String name() {
        return "Espionage";
    }

    public String displayText() {
        return "";
    }

    protected int canAffectCode() {
        return CAN_MOBS;
    }

    protected int canTargetCode() {
        return CAN_MOBS;
    }

    public int abstractQuality() {
        return Ability.QUALITY_OK_OTHERS;
    }

    private static final String[] triggerStrings = { "ESPIONAGE" };

    public int classificationCode() {
        return Ability.ACODE_THIEF_SKILL | Ability.DOMAIN_STEALTHY;
    }

    public String[] triggerStrings() {
        return triggerStrings;
    }

    public int usageType() {
        return USAGE_MOVEMENT | USAGE_MANA;
    }

    public int code = 0;

    public int abilityCode() {
        return code;
    }

    public void setAbilityCode(int newCode) {
        code = newCode;
    }

    public void executeMsg(Environmental myHost, CMMsg msg) {
        super.executeMsg(myHost, msg);
        if ((CMath.bset(msg.othersMajor(), CMMsg.MASK_CHANNEL))) {
            int channelInt = msg.othersMinor() - CMMsg.TYP_CHANNEL;
            boolean areareq = CMLib.channels().getChannelFlags(channelInt).contains(ChannelsLibrary.ChannelFlag.SAMEAREA);
            if ((CMLib.channels().getChannelFlags(channelInt).contains(ChannelsLibrary.ChannelFlag.CLANONLY) || CMLib.channels().getChannelFlags(channelInt).contains(ChannelsLibrary.ChannelFlag.CLANALLYONLY)) && (invoker() != null) && (invoker().getClanID().length() > 0) && (!((MOB) affected).getClanID().equals(invoker().getClanID())) && (!CMLib.channels().mayReadThisChannel(msg.source(), areareq, invoker(), channelInt))) invoker.executeMsg(myHost, msg);
        }
    }

    public void unInvoke() {
        if (canBeUninvoked()) {
            if ((invoker != null) && (affected != null)) invoker.tell("You are no longer committing espionage with " + affected.name() + ".");
        }
        super.unInvoke();
    }

    public boolean invoke(MOB mob, Vector commands, Environmental givenTarget, boolean auto, int asLevel) {
        if (commands.size() < 1) {
            mob.tell("Commit espionage through whom?");
            return false;
        }
        MOB target = this.getTarget(mob, commands, givenTarget);
        if (target == null) return false;
        if (target == mob) {
            mob.tell("You cannot do that with yourself?!");
            return false;
        }
        Ability A = target.fetchEffect(ID());
        if (A != null) {
            if (A.invoker() == mob) A.unInvoke(); else {
                mob.tell(mob, target, null, "It is too crowded to commit espionage with <T-NAME>.");
                return false;
            }
        }
        if (mob.isInCombat()) {
            mob.tell("Not while you are fighting!");
            return false;
        }
        if (CMLib.flags().canBeSeenBy(mob, target)) {
            mob.tell(target.name() + " is watching you too closely.");
            return false;
        }
        if (!super.invoke(mob, commands, givenTarget, auto, asLevel)) return false;
        int levelDiff = target.envStats().level() - (mob.envStats().level() + abilityCode() + (getXLEVELLevel(mob) * 2));
        boolean success = proficiencyCheck(mob, -(levelDiff * 10), auto);
        if (!success) {
            CMMsg msg = CMClass.getMsg(mob, target, null, CMMsg.MSG_OK_VISUAL, auto ? "" : "Your attempt to commit espionage using <T-NAMESELF> fails; <T-NAME> spots you!", CMMsg.MSG_OK_VISUAL, auto ? "" : "You spot <S-NAME> trying to commit espionage through you.", CMMsg.NO_EFFECT, null);
            if (mob.location().okMessage(mob, msg)) mob.location().send(mob, msg);
        } else {
            CMMsg msg = CMClass.getMsg(mob, target, this, auto ? CMMsg.MSG_OK_VISUAL : CMMsg.MSG_THIEF_ACT, "You are now committing espionage with <T-NAME>.  Enter 'espionage <targetname>' again to disengage.", CMMsg.NO_EFFECT, null, CMMsg.NO_EFFECT, null);
            if (mob.location().okMessage(mob, msg)) {
                mob.location().send(mob, msg);
                beneficialAffect(mob, target, asLevel, 0);
            }
        }
        return success;
    }
}
