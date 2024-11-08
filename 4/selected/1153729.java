package com.planet_ink.coffee_mud.WebMacros;

import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;
import java.util.*;

@SuppressWarnings("unchecked")
public class ChannelNext extends StdWebMacro {

    public String name() {
        return this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.') + 1);
    }

    public String runMacro(ExternalHTTPRequests httpReq, String parm) {
        Hashtable parms = parseParms(parm);
        String last = httpReq.getRequestParameter("CHANNEL");
        if (parms.containsKey("RESET")) {
            if (last != null) httpReq.removeRequestParameter("CHANNEL");
            return "";
        }
        MOB mob = Authenticate.getAuthenticatedMob(httpReq);
        if (mob != null) {
            String lastID = "";
            for (int i = 0; i < CMLib.channels().getNumChannels(); i++) {
                String name = CMLib.channels().getChannelName(i);
                if ((last == null) || ((last.length() > 0) && (last.equals(lastID)) && (!name.equals(lastID)))) {
                    if (CMLib.channels().mayReadThisChannel(mob, i, true)) {
                        httpReq.addRequestParameters("CHANNEL", name);
                        return "";
                    }
                    last = name;
                }
                lastID = name;
            }
            httpReq.addRequestParameters("CHANNEL", "");
            if (parms.containsKey("EMPTYOK")) return "<!--EMPTY-->";
            return " @break@";
        }
        return " @break@";
    }
}
