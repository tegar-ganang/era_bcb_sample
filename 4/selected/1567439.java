package ecks.services;

import ecks.Configuration;
import ecks.Hooks.Hooks;
import ecks.Utility.Client;
import ecks.Utility.UserModes;
import ecks.protocols.Generic;
import org.w3c.dom.NodeList;
import java.util.Map;

public class SrvSentinel extends bService {

    public String name = "SrvSentinel";

    public void introduce() {
        Generic.srvIntroduce(this);
        Hooks.regHook(this, Hooks.Events.E_SIGNON);
        Hooks.regHook(this, Hooks.Events.E_JOINCHAN);
        if (!(Configuration.Config.get("debugchan").equals("OFF"))) {
            Generic.curProtocol.srvJoin(this, Configuration.Config.get("debugchan"), "+stn");
            Generic.curProtocol.outSETMODE(this, Configuration.Config.get("debugchan"), "+o", name);
        }
    }

    public String getname() {
        return name;
    }

    public void setname(String nname) {
        name = nname;
    }

    public String getSRVDB() {
        return "";
    }

    public void loadSRVDB(NodeList XMLin) {
    }

    public int getcount() {
        return 0;
    }

    public void hookDispatch(Hooks.Events what, String source, String target, String args) {
        super.hookDispatch(this, what, source, target, args);
        switch(what) {
            case E_JOINCHAN:
                if (Configuration.chanservice != null) if (((SrvChannel) Configuration.getSvc().get(Configuration.chanservice)).getChannels().containsKey(source.toLowerCase())) {
                } else {
                    if (Generic.Channels.containsKey(source.toLowerCase())) {
                        try {
                            for (Map.Entry<Client, UserModes> e : Generic.Channels.get(source.toLowerCase()).clientmodes.entrySet()) {
                                Generic.curProtocol.outSETMODE(this, source, "-oaqh", e.getKey().uid);
                                if (e.getKey().modes.contains("r")) Generic.curProtocol.outSETMODE(this, source, "+v", e.getKey().uid);
                            }
                            Generic.curProtocol.outTOPIC(this, source, "This is an unregistered channel. Only registered users may chat. Please see the network help channel for more information.");
                            Generic.curProtocol.outSETMODE(this, source, "+Mstl", "10");
                            Generic.curProtocol.outPRVMSG(this, source, "This is an unregistered channel. Only registered users may chat. Please see the network help channel for more information.");
                        } catch (NullPointerException NPE) {
                            NPE.printStackTrace();
                        }
                    } else {
                    }
                }
                break;
            case E_SIGNON:
                if (source != null) Generic.curProtocol.outMODE(this, Generic.Users.get(source.toLowerCase()), "+R", "");
            default:
        }
    }
}
