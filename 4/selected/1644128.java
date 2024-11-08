package com.funda.xmppserver;

import com.funda.xmppserver.presence.PresenceManager;
import com.funda.xmppserver.reader.XMPPPacketReader;
import com.funda.xmppserver.roster.Roster;
import com.funda.xmppserver.writer.XMPPPacketWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

/**
 *
 * @author kurinchi
 */
public class XMPPClient extends Thread {

    protected Socket clientsock;

    protected DataInputStream reader;

    protected DataOutputStream writer;

    XMPPPacketWriter packetwriter;

    Boolean authenticated = false;

    protected ArrayList resource;

    protected String username;

    protected JID jid;

    protected Roster roster;

    protected PresenceManager presencemanager;

    public XMPPPacketWriter getPacketwriter() {
        return packetwriter;
    }

    public void setPacketwriter(XMPPPacketWriter packetwriter) {
        this.packetwriter = packetwriter;
    }

    /**
     * Get the value of presencemanager
     *
     * @return the value of presencemanager
     */
    public PresenceManager getPresencemanager() {
        return presencemanager;
    }

    /**
     * Set the value of presencemanager
     *
     * @param presencemanager new value of presencemanager
     */
    public void setPresencemanager(PresenceManager presencemanager) {
        this.presencemanager = presencemanager;
    }

    /**
     * Get the value of roster
     *
     * @return the value of roster
     */
    public Roster getRoster() {
        return roster;
    }

    /**
     * Set the value of roster
     *
     * @param roster new value of roster
     */
    public void setRoster(Roster roster) {
        this.roster = roster;
    }

    /**
     * Get the value of jid
     *
     * @return the value of jid
     */
    public JID getJid() {
        return jid;
    }

    /**
     * Set the value of jid
     *
     * @param jid new value of jid
     */
    public void setJid(JID jid) {
        this.jid = jid;
    }

    /**
     * Get the value of username
     *
     * @return the value of username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set the value of username
     *
     * @param username new value of username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Get the value of resource
     *
     * @return the value of resource
     */
    public ArrayList getResource() {
        return resource;
    }

    /**
     * Set the value of resource
     *
     * @param resource new value of resource
     */
    public void setResource(ArrayList resource) {
        this.resource = resource;
    }

    public Boolean getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(Boolean authenticated) {
        this.authenticated = authenticated;
    }

    public Boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Get the value of writer
     *
     * @return the value of writer
     */
    public DataOutputStream getWriter() {
        return writer;
    }

    /**
     * Set the value of writer
     *
     * @param writer new value of writer
     */
    public void setWriter(DataOutputStream writer) {
        this.writer = writer;
    }

    /**
     * Get the value of reader
     *
     * @return the value of reader
     */
    public DataInputStream getReader() {
        return reader;
    }

    /**
     * Set the value of reader
     *
     * @param reader new value of reader
     */
    public void setReader(DataInputStream reader) {
        this.reader = reader;
    }

    /**
     * Get the value of clientsock
     *
     * @return the value of clientsock
     */
    public Socket getClientsock() {
        return clientsock;
    }

    /**
     * Set the value of clientsock
     *
     * @param clientsock new value of clientsock
     */
    public void setClientsock(Socket clientsock) {
        this.clientsock = clientsock;
    }

    public String getBareJID() {
        return getUsername() + "@" + XMPPInfo.getHOST();
    }

    public XMPPClient(Socket clientsock) {
        this.clientsock = clientsock;
        resource = new ArrayList();
        jid = new JID();
        roster = new Roster(this);
        presencemanager = new PresenceManager();
        initReaderWriter();
    }

    public void initReaderWriter() {
        try {
            reader = new DataInputStream(clientsock.getInputStream());
            writer = new DataOutputStream(clientsock.getOutputStream());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void run() {
        packetwriter = new XMPPPacketWriter(writer, this);
        Thread readerthread = new Thread(new XMPPPacketReader(reader, packetwriter));
        readerthread.start();
    }
}
