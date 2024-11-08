package net.sf.opensmus;

import org.jboss.netty.channel.*;
import java.io.*;
import java.util.*;
import java.net.*;
import net.sf.opensmus.io.SMUSPipeline;

public class MUSUser implements ServerUser {

    private MUSServer m_server;

    Channel channel;

    Channel udpchannel;

    public static ChannelFutureListener REPORT_CLOSE = new ChannelFutureListener() {

        public void operationComplete(ChannelFuture future) {
            MUSUser whatUser = ((SMUSPipeline) future.getChannel().getPipeline()).user;
            if (future.isSuccess()) {
            } else {
                MUSLog.Log("Close failure for " + whatUser + ": " + future.getCause(), MUSLog.kDeb);
                whatUser.m_scheduledToDie = false;
                future.getChannel().close();
            }
        }
    };

    private DatagramSocket m_udpsocket = null;

    MUSUDPListener m_udplistener;

    InetSocketAddress m_UDPSocketAddress;

    private int m_udpportnumber = 0;

    public int m_udpcookie = 0;

    public boolean m_udpenabled = false;

    private Thread m_timer;

    boolean m_scheduledToDie = false;

    public boolean logged = false;

    public String m_pathname;

    public int ip;

    public String m_name = "";

    public MUSMovie m_movie;

    public int m_userlevel = 0;

    private Vector<ServerGroup> m_grouplist = new Vector<ServerGroup>();

    private int m_creationtime = 0;

    public MUSUser(MUSServer svr, Channel s) throws IOException {
        m_server = svr;
        channel = s;
        m_creationtime = m_server.timeStamp();
        ip = this.ipAsInteger();
        m_name = "tmp_login_" + channel.getId();
    }

    public void setUDPEnabled(MUSLogonMessage logmsg) {
        if (m_movie.m_props.getIntProperty("EnableUDP") != 1) return;
        if (logmsg.m_localUDPPortInfo.getType() == LValue.vt_Void) return;
        InetAddress m_userUDPAddress;
        if (logmsg.m_localAddressInfo.getType() == LValue.vt_Void) {
            m_userUDPAddress = ((InetSocketAddress) channel.getRemoteAddress()).getAddress();
        } else {
            try {
                m_userUDPAddress = InetAddress.getByName(logmsg.m_localUDPAddress);
            } catch (UnknownHostException uh) {
                return;
            }
        }
        m_UDPSocketAddress = new InetSocketAddress(m_userUDPAddress, logmsg.m_localUDPPort);
        MUSLog.Log("Remote UDP socket : " + m_UDPSocketAddress, MUSLog.kDeb);
        if (!createUDPSocket()) return;
        m_udpenabled = true;
    }

    public boolean createUDPSocket() {
        try {
            InetAddress iad;
            String ipaddress = m_server.m_udpAddress;
            if (ipaddress.equalsIgnoreCase("default")) iad = InetAddress.getLocalHost(); else iad = InetAddress.getByName(ipaddress);
            m_udpportnumber = m_server.getUDPPortNumber();
            udpchannel = m_server.UDPBootstrap.bind(new InetSocketAddress(iad, m_udpportnumber));
            ((SMUSPipeline) udpchannel.getPipeline()).user = this;
            udpchannel.connect(m_UDPSocketAddress);
            m_server.addUDPPort(m_udpportnumber);
            MUSLog.Log("UDP socket created > " + m_udpportnumber, MUSLog.kDeb);
            return true;
        } catch (IOException e) {
            MUSLog.Log("UDP socket not created > " + m_udpportnumber, MUSLog.kDeb);
            m_udpsocket = null;
            return false;
        }
    }

    public void replyUDPInformation() {
        Random rd = new Random();
        m_udpcookie = rd.nextInt();
        String udpAdd = m_server.m_udpAddress;
        if (m_server.m_udpAddress.equalsIgnoreCase("default")) {
            try {
                InetAddress iad = InetAddress.getLocalHost();
                udpAdd = iad.getHostAddress();
            } catch (UnknownHostException uhe) {
                MUSLog.Log("Unknown host exception while getting localhost address, udp reply aborted", MUSLog.kDeb);
                return;
            }
        }
        String udpadd = udpAdd + ":" + m_udpportnumber;
        String udpcook = Integer.toString(m_udpcookie);
        MUSMessage reply = new MUSMessage();
        reply.m_errCode = 0;
        reply.m_timeStamp = 0;
        reply.m_subject = new MUSMsgHeaderString("udp");
        reply.m_senderID = new MUSMsgHeaderString("System");
        reply.m_recptID = new MUSMsgHeaderStringList();
        reply.m_recptID.addElement(new MUSMsgHeaderString(m_name));
        reply.m_msgContent = new LValue();
        LList ls = new LList();
        ls.addElement(new LString(udpadd));
        ls.addElement(new LString(udpcook));
        reply.m_msgContent = ls;
        sendMessage(reply);
    }

    public void replyLogon(MUSLogonMessage msg) {
        if (m_udpenabled) replyUDPInformation();
        MUSMessage reply = new MUSMessage();
        reply.m_errCode = 0;
        reply.m_timeStamp = 0;
        reply.m_subject = new MUSMsgHeaderString("Logon");
        reply.m_senderID = new MUSMsgHeaderString("System");
        reply.m_recptID = new MUSMsgHeaderStringList();
        if (msg.m_userID != null) reply.m_recptID.addElement(new MUSMsgHeaderString(msg.m_userID)); else reply.m_recptID.addElement(new MUSMsgHeaderString());
        if (msg.m_moviename != null) reply.m_msgContent = new LString(msg.m_moviename); else reply.m_msgContent = new LString();
        sendMessage(reply);
    }

    public void replyLogonError(MUSLogonMessage msg, int error) {
        MUSMessage reply = new MUSMessage();
        reply.m_errCode = error;
        reply.m_timeStamp = 0;
        reply.m_subject = new MUSMsgHeaderString("Logon");
        reply.m_senderID = new MUSMsgHeaderString("System");
        reply.m_recptID = new MUSMsgHeaderStringList();
        if (msg.m_userID != null) reply.m_recptID.addElement(new MUSMsgHeaderString(msg.m_userID)); else reply.m_recptID.addElement(new MUSMsgHeaderString());
        if (msg.m_moviename != null) reply.m_msgContent = new LString(msg.m_moviename); else reply.m_msgContent = new LString();
        sendMessage(reply);
    }

    public void processUDPPacket(byte[] content) {
        try {
            ByteArrayInputStream bs = new ByteArrayInputStream(content);
            DataInputStream ds = new DataInputStream(bs);
            byte[] headers = new byte[8];
            int bytesRead = ds.read(headers, 0, 8);
            if (bytesRead != 8) return;
            byte[] decodedheaders = new byte[8];
            System.arraycopy(headers, 0, decodedheaders, 0, 8);
            if (decodedheaders[0] != 114) return;
            if (decodedheaders[1] != 0) return;
            int msgsize = ConversionUtils.byteArrayToInt(decodedheaders, 2);
            byte[] finalmsg = new byte[msgsize + 6];
            ds.readFully(finalmsg, 8, (msgsize - 2));
            System.arraycopy(headers, 0, finalmsg, 0, 8);
            MUSMessage msg;
            if (logged) msg = new MUSMessage(); else return;
            msg.m_udp = true;
            if (m_udpcookie == msg.m_timeStamp) {
                postMessage(msg);
            }
        } catch (Exception e) {
            MUSLog.Log("Error reading UDP packet data from user " + name(), MUSLog.kUsr);
        }
    }

    public void deleteUser() {
        if (!m_scheduledToDie) {
            m_timer = new MUSKillUserTimer(this, 600);
            m_scheduledToDie = true;
        }
    }

    public void killMUSUser() {
        disconnectFromMovie();
        if (m_udpenabled) m_server.releaseUDPPort(m_udpportnumber);
        if (m_udplistener != null) m_udplistener.kill();
        if (channel.isOpen()) {
            MUSLog.Log("Channel open in killMUSUser(), closing...: " + name(), MUSLog.kUsr);
            channel.close().addListener(REPORT_CLOSE);
        }
    }

    public void disconnectFromMovie() {
        if (m_movie != null) {
            m_movie.removeUser(this);
            m_movie = null;
        }
        m_grouplist.clear();
        m_server.removeMUSUser(this);
    }

    public void addToMovie(MUSMovie newmov) {
        m_movie = newmov;
        m_movie.addUser(this);
        MUSLog.Log("User " + name() + " logged to movie " + m_movie.name(), MUSLog.kUsr);
        MUSGroup mg;
        try {
            mg = m_movie.getGroup("@AllUsers");
        } catch (GroupNotFoundException gnf) {
            mg = new MUSGroup(m_movie, "@AllUsers");
        } catch (MUSErrorCode err) {
            mg = new MUSGroup(m_movie, "@AllUsers");
        }
        try {
            mg.addUser(this);
        } catch (MUSErrorCode err) {
        }
    }

    public void logDroppedMsg() {
        m_server.logDroppedMsg();
    }

    public void sendMessage(MUSMessage msg) {
        if (msg.m_udp && m_udpenabled) {
            MUSLog.Log("Writing outgoing UDP message : " + msg, MUSLog.kDeb);
            udpchannel.write(msg, m_UDPSocketAddress);
        } else {
            channel.write(msg);
        }
    }

    public void postMessage(MUSMessage msg) {
        if (m_movie != null) {
            m_movie.handleMsg(this, msg);
        }
    }

    public String name() {
        return m_name;
    }

    public String pathname() {
        return m_pathname;
    }

    public int userLevel() {
        return m_userlevel;
    }

    public void setuserLevel(int level) {
        m_userlevel = level;
        if (level >= m_server.m_props.getIntProperty("AntiFloodUserLevelIgnore")) {
            try {
                channel.getPipeline().remove("floodfilter");
            } catch (NoSuchElementException e) {
            }
        }
    }

    public ServerMovie serverMovie() {
        return m_movie;
    }

    public long creationTime() {
        return m_creationtime;
    }

    public String ipAddress() {
        return ((InetSocketAddress) channel.getRemoteAddress()).getAddress().getHostAddress();
    }

    public int ipAsInteger() {
        byte[] adr = ((InetSocketAddress) channel.getRemoteAddress()).getAddress().getAddress();
        return ConversionUtils.byteArrayToInt(adr, 0);
    }

    public Vector<String> getGroupNames() {
        Vector<String> groups = new Vector<String>();
        for (ServerGroup group : m_grouplist) {
            groups.addElement(((MUSGroup) group).m_name);
        }
        return groups;
    }

    public Vector<ServerGroup> getGroups() {
        return new Vector<ServerGroup>(m_grouplist);
    }

    public int getGroupsCount() {
        return m_grouplist.size();
    }

    public void groupJoined(ServerGroup grp) {
        m_grouplist.addElement(grp);
    }

    public void groupLeft(ServerGroup grp) {
        m_grouplist.removeElement(grp);
    }

    @Override
    public String toString() {
        return m_name + " (" + ipAddress() + ")";
    }
}
