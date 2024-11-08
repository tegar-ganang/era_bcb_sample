package ecks.services;

import ecks.Configuration;
import ecks.Hooks.Hooks;
import ecks.Logging;
import ecks.Utility.Client;
import ecks.protocols.Generic;
import ecks.util;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.util.HashMap;
import java.util.Map;

public class SrvHelp extends bService {

    public String name = "SrvHelp";

    public Map<String, SrvHelp_channel> Channels;

    public Map<Client, helpEntry> qMap;

    public void introduce() {
        Generic.srvIntroduce(this);
        Hooks.regHook(this, Hooks.Events.E_PRIVMSG);
        Hooks.regHook(this, Hooks.Events.E_JOINCHAN);
        Hooks.regHook(this, Hooks.Events.E_PARTCHAN);
        if (!(Configuration.Config.get("debugchan").equals("OFF"))) {
            Generic.curProtocol.srvJoin(this, Configuration.Config.get("debugchan"), "+stn");
            Generic.curProtocol.outSETMODE(this, Configuration.Config.get("debugchan"), "+o", name);
        }
        if (Configuration.Config.get("joinchannels").equals("YES")) for (String chan : Channels.keySet()) {
            Generic.curProtocol.srvJoin(this, chan, "+stn");
            Generic.curProtocol.outSETMODE(this, chan, "+o", name);
        }
    }

    public String getname() {
        return name;
    }

    public void setname(String nname) {
        name = nname;
    }

    public Map<String, SrvHelp_channel> getChannels() {
        return Channels;
    }

    public SrvHelp() {
        Channels = new HashMap<String, SrvHelp_channel>();
        qMap = new HashMap<Client, helpEntry>();
    }

    public String getSRVDB() {
        Logging.verbose("SRVHELP", "Started gathering SrvHelp Database...");
        StringBuilder tOut = new StringBuilder("<service class=\"" + this.getClass().getName() + "\" name=\"" + name + "\">\r\n");
        HashMap<String, SrvHelp_channel> X = new HashMap<String, SrvHelp_channel>(Channels);
        for (Map.Entry<String, SrvHelp_channel> usar : X.entrySet()) {
            tOut.append("\t" + "<channel>\r\n");
            tOut.append("\t\t" + "<name value=\"").append(util.encodeUTF(usar.getValue().channel)).append("\"/>\r\n");
            tOut.append("\t\t" + "<metadata>\r\n");
            for (Map.Entry<String, String> md : usar.getValue().getAllMeta().entrySet()) {
                tOut.append("\t\t\t" + "<").append(util.encodeUTF(md.getKey())).append(" value=\"").append(util.encodeUTF(md.getValue())).append("\"/>\r\n");
            }
            tOut.append("\t\t" + "</metadata>\r\n");
            tOut.append("\t" + "</channel>\r\n");
        }
        tOut.append("</service>\r\n");
        Logging.verbose("SRVHELP", "Done...");
        return tOut.toString();
    }

    public void loadSRVDB(NodeList XMLin) {
        for (int i = 0; i < XMLin.getLength(); i++) {
            String nTemp;
            Map<String, String> mTemp = new HashMap<String, String>();
            NodeList t;
            if (XMLin.item(i).getNodeType() != 1) continue;
            t = ((Element) XMLin.item(i)).getElementsByTagName("name");
            nTemp = util.decodeUTF(t.item(0).getAttributes().getNamedItem("value").getNodeValue());
            t = ((Element) XMLin.item(i)).getElementsByTagName("metadata").item(0).getChildNodes();
            for (int j = 0; j < t.getLength(); j++) {
                if (t.item(j).getNodeType() != 1) continue;
                mTemp.put(util.decodeUTF((t.item(j)).getNodeName()), util.decodeUTF((t.item(j)).getAttributes().getNamedItem("value").getNodeValue()));
            }
            Channels.put(nTemp.toLowerCase().trim(), new SrvHelp_channel(nTemp, mTemp));
        }
        Logging.info("SRVHELP", "Loaded " + Channels.size() + " help channels from database.");
    }

    public int getcount() {
        return Channels.size();
    }

    public void hookDispatch(Hooks.Events what, String source, String target, String args) {
        switch(what) {
            case E_PRIVMSG:
                String user = source.toLowerCase();
                String msg = args;
                if (qMap.containsKey(Generic.Users.get(user))) {
                    String chan = target;
                    helpEntry h = qMap.get(Generic.Users.get(user));
                    if (h.cbuffer.containsKey(Channels.get(chan))) {
                        if (h.redirect2 != null) {
                            Generic.curProtocol.outPRVMSG(this, h.redirect2.uid, user + "> " + msg);
                        } else h.cbuffer.get(Channels.get(chan)).append("\r\n").append(msg);
                    } else {
                        h.cbuffer.put(Channels.get(chan), new StringBuilder(msg));
                    }
                    break;
                }
            default:
                super.hookDispatch(this, what, source, target, args);
        }
    }
}

class helpEntry {

    public Client redirect2;

    public Map<SrvHelp_channel, StringBuilder> cbuffer;

    public helpEntry() {
        redirect2 = null;
        cbuffer = new HashMap<SrvHelp_channel, StringBuilder>();
    }
}
