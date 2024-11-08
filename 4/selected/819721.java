package ecks.services;

import ecks.Configuration;
import ecks.Hooks.Hooks;
import ecks.Logging;
import ecks.Threads.SrvChannel_ExpiryThread;
import ecks.protocols.Generic;
import ecks.util;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.util.HashMap;
import java.util.Map;

public class SrvChannel extends bService {

    public String name = "SrvChan";

    public Map<String, SrvChannel_channel> Channels;

    public void introduce() {
        Configuration.chanservice = name.toLowerCase();
        Generic.srvIntroduce(this);
        Hooks.regHook(this, Hooks.Events.E_PRIVMSG);
        Hooks.regHook(this, Hooks.Events.E_JOINCHAN);
        Hooks.regHook(this, Hooks.Events.E_KICK);
        Hooks.regHook(this, Hooks.Events.E_MODE);
        Hooks.regHook(this, Hooks.Events.E_TOPIC);
        util.startThread(new Thread(new SrvChannel_ExpiryThread())).start();
        Logging.info("SRVCHAN", "Expiry thread started...");
        if (!(Configuration.Config.get("debugchan").equals("OFF"))) {
            Generic.curProtocol.srvJoin(this, Configuration.Config.get("debugchan"), "+stn");
            Generic.curProtocol.outSETMODE(this, Configuration.Config.get("debugchan"), "+o", name);
        }
        if (Configuration.Config.get("joinchannels").equals("YES")) for (String chan : Channels.keySet()) {
            Generic.curProtocol.srvJoin(this, chan, "+srtn");
            Generic.curProtocol.outSETMODE(this, chan, "+ntro", name);
            if (Channels.get(chan).getAllMeta().containsKey("enftopic")) {
                Generic.curProtocol.outTOPIC(this, chan, Channels.get(chan).getMeta("enftopic"));
            }
        }
    }

    public String getname() {
        return name;
    }

    public void setname(String nname) {
        name = nname;
    }

    public Map<String, SrvChannel_channel> getChannels() {
        return Channels;
    }

    public SrvChannel() {
        Channels = new HashMap<String, SrvChannel_channel>();
    }

    public String getSRVDB() {
        Logging.verbose("SRVCHAN", "Started gathering SrvChan Database...");
        StringBuilder tOut = new StringBuilder("<service class=\"" + this.getClass().getName() + "\" name=\"" + name + "\">\r\n");
        HashMap<String, SrvChannel_channel> X = new HashMap<String, SrvChannel_channel>(Channels);
        for (Map.Entry<String, SrvChannel_channel> usar : X.entrySet()) {
            tOut.append("\t" + "<channel>\r\n");
            tOut.append("\t\t" + "<name value=\"").append(util.encodeUTF(usar.getValue().channel)).append("\"/>\r\n");
            tOut.append("\t\t" + "<users>\r\n");
            for (Map.Entry<String, SrvChannel_channel.ChanAccess> md : usar.getValue().getUsers().entrySet()) {
                tOut.append("\t\t\t" + "<").append(util.encodeUTF(md.getKey())).append(" value=\"").append(md.getValue()).append("\"/>\r\n");
            }
            tOut.append("\t\t" + "</users>\r\n");
            tOut.append("\t\t" + "<metadata>\r\n");
            for (Map.Entry<String, String> md : usar.getValue().getAllMeta().entrySet()) {
                tOut.append("\t\t\t" + "<").append(util.encodeUTF(md.getKey())).append(" value=\"").append(util.encodeUTF(md.getValue())).append("\"/>\r\n");
            }
            tOut.append("\t\t" + "</metadata>\r\n");
            tOut.append("\t" + "</channel>\r\n");
        }
        tOut.append("</service>\r\n");
        Logging.verbose("SRVCHAN", "Done...");
        return tOut.toString();
    }

    public void loadSRVDB(NodeList XMLin) {
        for (int i = 0; i < XMLin.getLength(); i++) {
            String nTemp;
            Map<String, String> sTemp = new HashMap<String, String>();
            Map<String, String> mTemp = new HashMap<String, String>();
            Map<String, SrvChannel_channel.ChanAccess> uTemp = new HashMap<String, SrvChannel_channel.ChanAccess>();
            NodeList t;
            if (XMLin.item(i).getNodeType() != 1) continue;
            t = ((Element) XMLin.item(i)).getElementsByTagName("name");
            nTemp = util.decodeUTF(t.item(0).getAttributes().getNamedItem("value").getNodeValue());
            t = ((Element) XMLin.item(i)).getElementsByTagName("users").item(0).getChildNodes();
            for (int j = 0; j < t.getLength(); j++) {
                if (t.item(j).getNodeType() != 1) continue;
                String handle = (util.decodeUTF((t.item(j)).getNodeName()));
                String access = (t.item(j)).getAttributes().getNamedItem("value").getNodeValue();
                if (((SrvAuth) Configuration.getSvc().get(Configuration.authservice)).getUsers().containsKey(handle.toLowerCase())) ((SrvAuth) Configuration.getSvc().get(Configuration.authservice)).getUsers().get(handle.toLowerCase()).WhereAccess.put(nTemp, access);
                uTemp.put(handle, SrvChannel_channel.ChanAccess.valueOf(access));
            }
            t = ((Element) XMLin.item(i)).getElementsByTagName("metadata").item(0).getChildNodes();
            for (int j = 0; j < t.getLength(); j++) {
                if (t.item(j).getNodeType() != 1) continue;
                mTemp.put(util.decodeUTF((t.item(j)).getNodeName()), util.decodeUTF((t.item(j)).getAttributes().getNamedItem("value").getNodeValue()));
            }
            Channels.put(nTemp.toLowerCase().trim(), new SrvChannel_channel(nTemp, uTemp, sTemp, mTemp));
        }
        Logging.info("SRVCHAN", "Loaded " + Channels.size() + " registered channels from database.");
    }

    public int getcount() {
        return Channels.size();
    }

    public void hookDispatch(Hooks.Events what, String source, String target, String args) {
        if (what.equals(Hooks.Events.E_PRIVMSG)) {
            if (args.length() > 0) if (args.substring(0, 1).equals("!")) super.hookDispatch(this, what, source, target, this.getname() + ": " + args.substring(1)); else super.hookDispatch(this, what, source, target, args);
        } else super.hookDispatch(this, what, source, target, args);
        switch(what) {
            case E_JOINCHAN:
                this.handle(target, source, "sync silent");
                break;
            case E_MODE:
                if (Channels.containsKey(target)) {
                    if (Channels.get(target).getAllMeta().containsKey("enfmodes")) {
                        Generic.curProtocol.outSETMODE(this, target, Channels.get(target).getMeta("enfmodes"), this.getname());
                    }
                }
                break;
            case E_TOPIC:
                if (Channels.containsKey(source)) if (Channels.get(source).getAllMeta().containsKey("enftopic")) {
                    if (!Generic.Channels.get(source).topic.equals(Channels.get(source).getMeta("enftopic"))) Generic.curProtocol.outTOPIC(this, source, Channels.get(source).getMeta("enftopic"));
                }
                break;
            default:
        }
    }
}
