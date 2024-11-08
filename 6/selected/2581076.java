package de.tudresden.inf.rn.mobilis.kmlloader;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.ProviderManager;
import se.su.it.smack.utils.XMPPUtils;
import de.tudresden.inf.rn.mobilis.xmpp.packet.BuddylistIQ;
import de.tudresden.inf.rn.mobilis.xmpp.packet.LocationIQ;
import de.tudresden.inf.rn.mobilis.xmpp.provider.LocationIQProvider;

public class LocationJabberer extends Thread {

    private String host;

    private String port;

    private String service;

    private String user;

    private String password;

    private String ressource;

    private String androidbuddy;

    private String interval;

    private Collection<Coordinate> coordinates;

    public LocationJabberer(String host, String port, String service, String user, String password, String ressource, String androidbuddy, String interval) {
        this.host = host;
        this.port = port;
        this.service = service;
        this.user = user;
        this.password = password;
        this.ressource = ressource;
        this.androidbuddy = androidbuddy;
        this.interval = interval;
    }

    public void setCoordinates(Collection<Coordinate> coordinates) {
        this.coordinates = coordinates;
    }

    @Override
    public void run() {
        this.xmppTalk();
    }

    private void xmppTalk() {
        System.err.println("INFO: KML-Simulating has started.");
        System.err.println("INFO: Stage 1: Initializing XMPP connection.");
        XMPPConnection connection = this.xmppInitialize();
        if (connection == null) return;
        System.err.println("INFO: Stage 2: Reading and posting roster.");
        this.xmppRosterPost(connection);
        System.err.println("INFO: Stage 3: Walking through coordinates.");
        int interval = 1000;
        try {
            interval = Integer.parseInt(this.interval);
        } catch (NumberFormatException e) {
        }
        for (Coordinate c : this.coordinates) {
            this.xmppLocationUpdate(connection, c.lat, c.lon);
            System.err.println("INFO: Waiting for " + interval + "ms.");
            try {
                LocationJabberer.sleep(interval);
            } catch (InterruptedException e) {
            }
        }
        System.err.println("INFO: Stage 4: Done walking. Shutting down connection.");
        connection.disconnect();
        System.err.println("INFO: Connection closed.");
    }

    private XMPPConnection xmppInitialize() {
        int port = 5222;
        try {
            port = Integer.parseInt(this.port);
        } catch (NumberFormatException e) {
            System.err.println("WARNING: Invalid port number. Assuming port " + String.valueOf(port) + " instead.");
        }
        System.err.println("INFO: Connecting to " + this.host + ", port: " + String.valueOf(port) + ", service: " + this.service + ".");
        ConnectionConfiguration configuration = new ConnectionConfiguration(this.host, port, this.service);
        XMPPConnection connection = new XMPPConnection(configuration);
        try {
            connection.connect();
        } catch (XMPPException e) {
            System.err.println("ERROR: Failed to connect to server.");
            return null;
        }
        try {
            System.err.println("INFO: Trying to login as " + this.user + ".");
            connection.login(this.user, this.password);
            System.err.println("INFO: Logged in sucessfully.");
            return connection;
        } catch (XMPPException e) {
            System.err.println("ERROR: Failed to login to server.");
            return null;
        }
    }

    private void xmppRosterPost(XMPPConnection connection) {
        List<String> jidList = new LinkedList<String>();
        System.err.println("INFO: Reading roster.");
        for (RosterEntry entry : connection.getRoster().getEntries()) {
            jidList.add(entry.getUser());
            System.err.println("INFO: Got " + entry.getUser() + " from roster.");
        }
        System.err.println("INFO: Sending " + jidList.size() + " roster entries to server.");
        BuddylistIQ blIQ = new BuddylistIQ();
        blIQ.setFrom(connection.getUser());
        blIQ.setTo(this.androidbuddy);
        blIQ.setBuddies(jidList);
        blIQ.setIdentity(XMPPUtil.jidWithoutRessource(connection.getUser()));
        blIQ.setNetwork("roster");
        blIQ.setType(IQ.Type.SET);
        connection.sendPacket(blIQ);
    }

    private void xmppLocationUpdate(XMPPConnection connection, double lat, double lon) {
        System.err.println("INFO: Sending location update: lat " + String.valueOf(lat) + ", lon " + String.valueOf(lon) + ".");
        LocationIQ luIQ = new LocationIQ();
        luIQ.setFrom(connection.getUser());
        luIQ.setTo(this.androidbuddy);
        luIQ.setLatitude(lat);
        luIQ.setLongitude(lon);
        luIQ.setAltitude(0.0f);
        luIQ.setType(IQ.Type.SET);
        luIQ.setTimestamp(new Date());
        luIQ.setIdentity(connection.getUser());
        connection.sendPacket(luIQ);
    }
}
