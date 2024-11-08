package pvoim.im_protocols.xmpp;

import java.util.Collection;
import java.io.*;
import java.util.Map;
import java.util.Date;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.proxy.ProxyException;
import pvoim.im_protocols_common.Im_Contact;
import pvoim.im_protocols_common.Im_Message;
import pvoim.im_protocols_common.Im_State;
import pvoim.lib.Base64Coder;
import pvoim.main.pvoim_main;
import pvoim.data_structs.Pvoim_Settings;
import pvoim.im_protocols.xmpp.*;

public class XMPP_Protocol {

    pvoim_main pm;

    ConnectionConfiguration Config;

    public XMPPConnection Conn;

    Roster roster;

    Pvoim_Settings settings;

    public String Error_message;

    public XMPP_Protocol(pvoim_main p) {
        this.pm = p;
    }

    public class Login_Thread extends Thread {

        private XMPP_Protocol xmpp;

        private String login;

        private String server;

        private String resource;

        private String pass;

        public Login_Thread(XMPP_Protocol xmpp_, String login_, String server_, String resource_, String pass_) {
            this.xmpp = xmpp_;
            this.login = login_;
            this.server = server_;
            this.pass = pass_;
            this.resource = resource_;
        }

        public void run() {
            this.xmpp.Conn = new XMPPConnection(this.xmpp.Config);
            try {
                this.xmpp.Conn.connect();
                this.xmpp.Conn.getSASLAuthentication().supportSASLMechanism("PLAIN", 0);
                this.xmpp.Conn.login(login, pass, resource);
            } catch (XMPPException e) {
                System.out.println("XMPPException: " + e.getMessage());
                this.xmpp.Error_message = e.getMessage();
                this.xmpp.After_Login(false);
                return;
            }
            this.xmpp.After_Login(true);
        }
    }

    public void After_Login(boolean Success) {
        if (Success) {
            roster = this.Conn.getRoster();
            roster.addRosterListener(new RosterL(this));
            roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
            PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
            this.Conn.addPacketListener(new MessageL(this), filter);
        } else {
            System.out.println("Connection was unsuccess");
        }
        this.pm.On_Logined(Success);
    }

    public void Connect(String login, String server, String resource, String pass) {
        this.settings = new Pvoim_Settings();
        this.settings.Load();
        if (this.settings.Proxy_Use_Proxy) {
            String plogin;
            String ppass;
            ProxyInfo pi;
            if (this.settings.Proxy_Use_Authorization) {
                plogin = this.settings.Proxy_Login;
                ppass = this.settings.Proxy_Pass;
            } else {
                plogin = "";
                ppass = "";
            }
            if (this.settings.Proxy_Type == this.settings.Proxy_Type_HTTP) {
                System.out.println("Using HTTP Proxy...");
                pi = new ProxyInfo(ProxyInfo.ProxyType.HTTP, this.settings.Proxy_Address, this.settings.Proxy_Port, plogin, ppass);
            } else if (this.settings.Proxy_Type == this.settings.Proxy_Type_SOCKS4) {
                pi = new ProxyInfo(ProxyInfo.ProxyType.SOCKS4, this.settings.Proxy_Address, this.settings.Proxy_Port, plogin, ppass);
            } else if (this.settings.Proxy_Type == this.settings.Proxy_Type_SOCKS5) {
                pi = new ProxyInfo(ProxyInfo.ProxyType.SOCKS5, this.settings.Proxy_Address, this.settings.Proxy_Port, plogin, ppass);
            } else {
                System.out.println("XMPP_Protocol: Connect: Proxy type unknown, trying without proxy");
                pi = new ProxyInfo(ProxyInfo.ProxyType.NONE, this.settings.Proxy_Address, this.settings.Proxy_Port, plogin, ppass);
            }
            this.Config = new ConnectionConfiguration(server, this.settings.Port, pi);
        } else {
            this.Config = new ConnectionConfiguration(server, this.settings.Port);
        }
        Login_Thread lt = new Login_Thread(this, login, server, resource, pass);
        lt.start();
    }

    public boolean Disconnect() {
        this.Conn.disconnect();
        return !this.Conn.isConnected();
    }

    public void Get_Roaster_Entries() {
        Collection<RosterEntry> entries = roster.getEntries();
        for (RosterEntry entry : entries) {
            Im_Contact c = this.Get_Im_Contact(entry.getUser());
            this.pm.On_New_Roaster_Item(c);
        }
    }

    private Im_Contact Get_Im_Contact(String jid) {
        int slashpos = jid.indexOf("/");
        String id;
        if (slashpos > -1) {
            id = jid.substring(0, slashpos);
        } else {
            id = jid;
        }
        Im_Contact c = new Im_Contact(id);
        return c;
    }

    private class RosterL implements RosterListener {

        private XMPP_Protocol xmpp;

        public RosterL(XMPP_Protocol xmpp_p) {
            this.xmpp = xmpp_p;
        }

        public void entriesAdded(Collection<String> addresses) {
            System.out.println("XMPP Roaster entries Added");
            for (int i = 0; i < addresses.size(); i++) {
                String addr = (String) addresses.toArray()[i];
                Im_Contact c = this.xmpp.Get_Im_Contact(addr);
                this.xmpp.Send_Auth_Request(c);
                this.xmpp.pm.On_New_Roaster_Item(c);
            }
        }

        public void entriesDeleted(Collection<String> addresses) {
            System.out.println("XMPP Roaster entries Deleted");
            for (int i = 0; i < addresses.size(); i++) {
                String addr = (String) addresses.toArray()[i];
                Im_Contact c = this.xmpp.Get_Im_Contact(addr);
                this.xmpp.Send_Remove_Auth_Request(c);
                this.xmpp.pm.On_Delete_Contact(c);
            }
        }

        public void entriesUpdated(Collection<String> addresses) {
            System.out.println("XMPP Roaster entries Updated");
        }

        public void presenceChanged(Presence presence) {
            Im_Contact c = this.xmpp.Get_Im_Contact(presence.getFrom());
            this.xmpp.CheckPresence(c);
            this.xmpp.OnPresence(c);
        }
    }

    private void CheckPresence(Im_Contact c) {
        Presence presence = roster.getPresence(c.id);
        if (presence.getType() == Presence.Type.unavailable) {
            c.State.State = Im_State.OFFLINE;
        } else if (presence.getType() == Presence.Type.error) {
            c.State.State = Im_State.ERROR;
        } else {
            c.State.State = Im_State.ONLINE;
            c.State.State_Message = presence.getStatus();
        }
    }

    private class MessageL implements PacketListener {

        private XMPP_Protocol xmpp;

        public MessageL(XMPP_Protocol xmpp_p) {
            this.xmpp = xmpp_p;
        }

        public void processPacket(Packet packet) {
            Im_Contact c = this.xmpp.Get_Im_Contact(packet.getFrom());
            Message m = (Message) packet;
            Im_Message msg = new Im_Message(c);
            PacketExtension pe = packet.getExtension("http://pvoim.org/protocol/pvoim");
            if (pe != null) {
                if (pe.getElementName().equals("pvoimsound")) {
                    msg.Sound = Base64Coder.decode(((XMPP_pvoimsound) pe).Get_Sound());
                }
                if (pe.getElementName().equals("pvoimifcompat")) {
                    this.xmpp.On_Recv_pvoimifcompat(packet.getFrom());
                    return;
                }
            } else {
                msg.Text = m.getBody();
            }
            this.xmpp.On_Recv_Message(msg);
        }
    }

    public void On_Recv_Message(Im_Message msg) {
        this.pm.On_Recv_Message(msg);
    }

    public void On_Recv_pvoimifcompat(String from) {
        this.Send_pvoimiamcompat(from);
    }

    public void Send_pvoimiamcompat(String to) {
        Message m = new Message();
        m.setType(Message.Type.chat);
        m.setTo(to);
        m.addExtension(new XMPP_pvoimiamcompat());
        this.Conn.sendPacket(m);
    }

    public boolean Add_Contact(Im_Contact c) {
        try {
            this.roster.createEntry(c.id, "", null);
        } catch (XMPPException e) {
            System.out.println("XMPPException: " + e.getMessage());
            this.Error_message = e.getMessage();
            return false;
        }
        return true;
    }

    public boolean Delete_Contact(Im_Contact c) {
        try {
            this.roster.removeEntry(this.roster.getEntry(c.id));
        } catch (XMPPException e) {
            System.out.println("XMPPException: " + e.getMessage());
            this.Error_message = e.getMessage();
            return false;
        }
        return true;
    }

    private void OnPresence(Im_Contact c) {
        this.pm.On_Presence(c);
    }

    public void Send_Message(Im_Message msg) {
        if (msg.Sound != null) {
            Message p = new Message();
            p.setType(Message.Type.chat);
            p.setTo(msg.To.id);
            char[] encoded_chars = Base64Coder.encode(msg.Sound);
            String encoded = new String(encoded_chars);
            p.addExtension(new XMPP_pvoimsound(encoded));
            this.Conn.sendPacket(p);
        } else {
            Message m = new Message();
            m.setType(Message.Type.chat);
            m.setBody(msg.Text);
            m.setTo(msg.To.id);
            this.Conn.sendPacket(m);
        }
    }

    public void Send_Auth_Request(Im_Contact c) {
        Presence p = new Presence(Presence.Type.subscribe);
        p.setTo(c.id);
        this.Conn.sendPacket(p);
    }

    public void Send_Remove_Auth_Request(Im_Contact c) {
        Presence p = new Presence(Presence.Type.unsubscribe);
        p.setTo(c.id);
        this.Conn.sendPacket(p);
    }

    public Collection<String> Get_Registration_Account_Attributes(String server) {
        XMPPConnection conn = new XMPPConnection(server);
        AccountManager accman = new AccountManager(conn);
        try {
            conn.connect();
            Collection<String> attrs = accman.getAccountAttributes();
            conn.disconnect();
            return attrs;
        } catch (XMPPException e) {
            System.out.println("XMPPException: " + e.getMessage());
            this.Error_message = e.getMessage();
            conn.disconnect();
            return null;
        }
    }

    public boolean Register_New_Account(Pvoim_Settings settings, Map<String, String> attrs) {
        XMPPConnection conn = new XMPPConnection(settings.Server);
        AccountManager accman = new AccountManager(conn);
        try {
            conn.connect();
            accman.createAccount(settings.Login, settings.Password, attrs);
            conn.disconnect();
        } catch (XMPPException e) {
            System.out.println("XMPPException: " + e.getMessage());
            this.Error_message = e.getMessage();
            conn.disconnect();
            return false;
        }
        return true;
    }
}
