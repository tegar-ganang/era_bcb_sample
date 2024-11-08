package eu.popeye.network;

import eu.popeye.application.PropertiesLoader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import org.jgroups.*;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Random;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.jgroups.blocks.PullPushAdapter;
import org.jgroups.protocols.ClusterHeader;
import org.jgroups.protocols.GM;
import org.jgroups.protocols.GMHeader;
import org.jgroups.protocols.PDS;
import org.jgroups.protocols.PDSHeader;
import org.jgroups.protocols.GroupHeader;
import org.jgroups.protocols.UDP;
import org.jgroups.protocols.UpperHeader;
import org.jgroups.protocols.SPeerHeader;
import org.jgroups.protocols.strategy.PDSPeer;
import org.jgroups.protocols.strategy.PDSSuperPeerLocalCluster;
import org.jgroups.protocols.strategy.PDSSuperPeerSPCluster;
import org.jgroups.stack.IpAddress;
import org.jgroups.stack.Protocol;
import org.jgroups.structures.IP;
import org.jgroups.structures.Node;
import org.jgroups.structures.Service;
import org.jgroups.structures.ServiceGroup;
import org.jgroups.structures.ClusterStruct;
import org.jgroups.structures.Group;
import org.jgroups.structures.UpperStruct;

/**
 * This class implements Facade pattern and hides  
 * Peer-Discovery Service PDS and GM (Group Management) 
 * protocols to the upper layers. 
 * The upper layer must implement TransportPopeye interface.
 *
 * @author Miguel Angel Sanchis
 * 
 */
public class PullPushAdapterInterface {

    private static final byte SPEER = 1;

    private static final byte PEER = 2;

    private static final String groupCluster = "MANET";

    private static final String groupSpeer = "SPEER";

    private int stat = 0;

    private JChannel speerCLocal = null;

    private JChannel speerCSpeer = null;

    private JChannel peer = null;

    private ChannelList clistener = null;

    private MessageList mlistener = null;

    private TransportPopeye trans = null;

    private ClusterStruct struct = null;

    private UpperStruct upper = null;

    private boolean connect = false;

    private ConcurrentHashMap channels_WG = new ConcurrentHashMap();

    private ConcurrentHashMap upper_Groups = new ConcurrentHashMap();

    private ConcurrentHashMap ident_Serv = new ConcurrentHashMap();

    private ConcurrentHashMap ident_Head = new ConcurrentHashMap();

    private String nick = null;

    private boolean start = false;

    private String inter = ";bind_addr=";

    /**
     * Creates a new instance of PullPushAdapterInterface 
     * to run the superpeer or peer role in the lower layers
     * 
     * @param t Class that implements TransportPopeye interface
     * @param nick The name of the node
     * @param sp true--> the node is a superpeer, false--> the node is a peer
     * @param obj is the credential managed by Security layer of the node.
     */
    public PullPushAdapterInterface(TransportPopeye t, String nick, boolean sp, Serializable obj) throws ChannelException {
        System.setProperty("java.library.path", "./");
        getInterface();
        this.trans = t;
        clistener = new ChannelList();
        mlistener = new MessageList();
        this.nick = nick;
        if (sp) {
            String propsLocal = "";
            String propsSpeer = "";
            propsLocal = "UDP(mcast_addr=228.8.8.8;ip_ttl=32" + inter + "):" + "DRM:" + "FRAG2(frag_size=1350;overhead=150):" + "PDS(speer=true;mcast=228.8.8.8;nick=" + nick + ")";
            propsSpeer = "UDP(mcast_addr=224.0.0.8;ip_ttl=32" + inter + "):" + "DRM:" + "SMACKMANET2(speer=true;max_xmits=3;timeout=1000,2000,3000):" + "FRAG2(frag_size=1350;overhead=150):" + "PDS(speer=true;mcast=224.0.0.8;nick=" + nick + ")";
            speerCLocal = new JChannel(propsLocal);
            Protocol udplocal = speerCLocal.getProtocolStack().findProtocol("UDP");
            udplocal.setWarn(false);
            PDS protpds = (PDS) speerCLocal.getProtocolStack().findProtocol("PDS");
            protpds.setProfileIdentifier(obj);
            speerCLocal.addChannelListener(clistener);
            speerCSpeer = new JChannel(propsSpeer);
            Protocol udpspeer = speerCSpeer.getProtocolStack().findProtocol("UDP");
            udpspeer.setWarn(false);
            speerCSpeer.addChannelListener(clistener);
            stat = SPEER;
        } else {
            String propspeer = "";
            propspeer = "UDP(mcast_addr=228.8.8.8;ip_ttl=32" + inter + "):" + "DRM:" + "FRAG2(frag_size=1350;overhead=150):" + "PDS(speer=false;mcast=228.8.8.8;nick=" + nick + ")";
            peer = new JChannel(propspeer);
            Protocol udppeer = peer.getProtocolStack().findProtocol("UDP");
            udppeer.setWarn(false);
            PDS protpds = (PDS) peer.getProtocolStack().findProtocol("PDS");
            protpds.setProfileIdentifier(obj);
            peer.addChannelListener(clistener);
            stat = this.PEER;
        }
    }

    /**
     * This method connects the facade to the jchannels depending on
     * the role being executed
     */
    public void connect() throws ChannelException {
        connect = true;
        switch(stat) {
            case SPEER:
                speerCSpeer.connect(this.groupSpeer);
                speerCLocal.connect(this.groupCluster);
                break;
            case PEER:
                peer.connect(this.groupCluster);
                break;
        }
        boolean cont = false;
        synchronized (this) {
            cont = start;
        }
        while (!cont) {
            try {
                Thread.currentThread().sleep(10);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            synchronized (this) {
                cont = start;
            }
        }
        System.out.println("PPAI start " + start);
    }

    /**
     * Method that disconnects every JChannel
     */
    public void disconnect() {
        connect = false;
        switch(stat) {
            case SPEER:
                speerCLocal.disconnect();
                speerCSpeer.disconnect();
                disconnectChannels();
                break;
            case PEER:
                peer.disconnect();
                disconnectChannels();
                break;
        }
    }

    /**
     * This function returns a copy of Cluster Struct in a specific moment
     * 
     * @return a structure ClusterStruct that exist in this moment
     */
    private synchronized ClusterStruct getClusterStruct() {
        if (struct != null) return struct.clone();
        return new ClusterStruct();
    }

    /**
     * This function returns a copy of Upper Struct in a specific moment
     *
     * @return a structure UpperStruct that exist in this moment
     *
     **/
    private synchronized UpperStruct getUpperStruct() {
        if (upper != null) return upper.clone();
        return new UpperStruct();
    }

    /**
     * This function returns 1 when the node is Super-peer
     * and returns 2 when it is peer
     *
     * @return int
     */
    public int getState() {
        return stat;
    }

    /**
     * This function returns the super-peer address
     *
     * @return Address
     */
    public IpAddress getSuperPeer() {
        if (stat == this.SPEER) {
            UDP udp = (UDP) speerCLocal.getProtocolStack().findProtocol("UDP");
            return (IpAddress) udp.getLocalAddress();
        } else {
            PDS pds = (PDS) peer.getProtocolStack().findProtocol("PDS");
            PDSPeer pdsp = (PDSPeer) pds.getStrategy();
            return pdsp.getIPSuperPeer();
        }
    }

    /**
     * Method that captures every message coming fron the SPCLocal JChannel
     * and process them generating the right messages to a proper work of the facade
     */
    private void sPeerCLocalLoop() {
        new Thread("FacadePopeyeSpeerCLocal") {

            public void run() {
                Object tmp;
                Message msg;
                while (connect) {
                    try {
                        tmp = speerCLocal.receive(0);
                        if (tmp == null) continue;
                        if (!(tmp instanceof Message)) continue;
                        msg = (Message) tmp;
                        ClusterHeader head = (ClusterHeader) msg.getHeader("Cluster");
                        if (head != null) {
                            if (speerCSpeer.isConnected()) speerCSpeer.send(msg);
                            PDS pd = (PDS) speerCLocal.getProtocolStack().findProtocol("PDS");
                            ClusterStruct aux = pd.getGlobalClusterStruct();
                            if (aux != null) struct = aux.clone();
                        }
                        UpperHeader uph = (UpperHeader) msg.getHeader("Upper");
                        if (uph != null) {
                            if (speerCSpeer.isConnected()) speerCSpeer.send(msg);
                            upper = uph.upper.clone();
                            boolean new_group = false;
                            Enumeration en = upper.getGroups().elements();
                            while (en.hasMoreElements()) {
                                Group g = (Group) en.nextElement();
                                if (upper.getGroup(g.getName()).getNumNodes() == 1 && !upper.getGroup(g.getName()).isNode(nick)) {
                                    new_group = true;
                                    if (upper_Groups.get(g.getName()) != null) {
                                        upper_Groups.replace(g.getName(), msg);
                                    } else upper_Groups.put(g.getName(), msg);
                                }
                            }
                            if (!new_group) {
                                trans.receiveStruct(msg);
                            }
                            synchronized (this) {
                                start = true;
                            }
                        }
                        GroupHeader headwg = (GroupHeader) msg.getHeader("Group");
                        if (headwg != null) {
                            switch(headwg.tipo) {
                                case GroupHeader.SP_New_WorkGroup:
                                    try {
                                        JChannel jc = (JChannel) channels_WG.get(headwg.wg.getName());
                                        if (jc == null) {
                                            JChannel j = groupJchannel(true, headwg.wg);
                                            channels_WG.put(headwg.wg.getName(), j);
                                            GM protocolGM = (GM) j.getProtocolStack().findProtocol("GM");
                                            if (protocolGM != null) {
                                                protocolGM.getStrategy().addGroup(headwg.wg);
                                                protocolGM.setCoordinator(true);
                                            }
                                            Message m = new Message(null);
                                            GMHeader headmwg = new GMHeader(GMHeader.SP_Port_Serv, headwg.wg.getName(), ((IpAddress) j.getLocalAddress()).getPort(), true);
                                            m.putHeader("GM", headmwg);
                                            speerCLocal.send(m);
                                            if (((Message) upper_Groups.get(headwg.wg.getName())) != null) {
                                                trans.receiveStruct(((Message) upper_Groups.get(headwg.wg.getName())).copy());
                                            }
                                        }
                                    } catch (ChannelException ex) {
                                        ex.printStackTrace();
                                    }
                                    break;
                                case GroupHeader.SP_WorkGroup:
                                    if (headwg.wg.getNumServices() > 0) {
                                        JChannel jc = (JChannel) channels_WG.get(headwg.wg.getName());
                                        if (jc != null) {
                                            GM protocolGM = (GM) jc.getProtocolStack().findProtocol("GM");
                                            if (protocolGM != null) protocolGM.getStrategy().addGroup(headwg.wg);
                                        } else {
                                            JChannel j = null;
                                            try {
                                                j = groupJchannel(true, headwg.wg);
                                                channels_WG.put(headwg.wg.getName(), j);
                                                Message m = new Message(null);
                                                GMHeader headmwg = new GMHeader(GMHeader.SP_Port_Serv, headwg.wg.getName(), ((IpAddress) j.getLocalAddress()).getPort(), false);
                                                m.putHeader("GM", headmwg);
                                                speerCLocal.send(m);
                                            } catch (ChannelException ex) {
                                                ex.printStackTrace();
                                            }
                                            if (j != null) {
                                                GM protocolGM = (GM) j.getProtocolStack().findProtocol("GM");
                                                if (protocolGM != null) {
                                                    protocolGM.getStrategy().addGroup(headwg.wg);
                                                }
                                                if (((Message) upper_Groups.get(headwg.wg.getName())) != null) {
                                                    trans.receiveStruct(((Message) upper_Groups.get(headwg.wg.getName())).copy());
                                                }
                                            }
                                        }
                                    } else {
                                        try {
                                            JChannel jc = (JChannel) channels_WG.get(headwg.wg.getName());
                                            if (jc != null) {
                                                GM protocolGM = (GM) jc.getProtocolStack().findProtocol("GM");
                                                if (protocolGM != null) protocolGM.getStrategy().addGroup(headwg.wg);
                                            } else {
                                                JChannel j = groupJchannel(true, headwg.wg);
                                                channels_WG.put(headwg.wg.getName(), j);
                                                GM protocolGM = (GM) j.getProtocolStack().findProtocol("GM");
                                                if (protocolGM != null) {
                                                    protocolGM.getStrategy().addGroup(headwg.wg);
                                                }
                                                Message m = new Message(null);
                                                GMHeader headmwg = new GMHeader(GMHeader.SP_Port_Serv, headwg.wg.getName(), ((IpAddress) j.getLocalAddress()).getPort(), false);
                                                m.putHeader("GM", headmwg);
                                                speerCLocal.send(m);
                                                if (((Message) upper_Groups.get(headwg.wg.getName())) != null) {
                                                    trans.receiveStruct(((Message) upper_Groups.get(headwg.wg.getName())).copy());
                                                }
                                            }
                                        } catch (ChannelException ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                    break;
                                case GroupHeader.SP_KO_WorkGroup:
                                    closeChannel(headwg.wg.getName());
                                    break;
                            }
                        } else {
                            if (msg != null && msg.getObject() != null) {
                                trans.receiveData(msg, "Unicast");
                            }
                        }
                    } catch (ChannelClosedException ex) {
                        ex.printStackTrace();
                    } catch (ChannelNotConnectedException ex) {
                        ex.printStackTrace();
                    } catch (TimeoutException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }.start();
    }

    /**
     * Method that captures every message coming fron the SPClusterSPeer JChannel
     * and process them generating the right messages to a proper work of the facade
     */
    private void sPeerCSPeerLoop() {
        new Thread("FacadePopeyeSpeerCSPeer") {

            public void run() {
                Object tmp;
                Message msg;
                while (connect) {
                    try {
                        tmp = speerCSpeer.receive(0);
                        if (tmp == null) continue;
                        if (!(tmp instanceof Message)) continue;
                        msg = (Message) tmp;
                        ClusterHeader head = (ClusterHeader) msg.getHeader("Cluster");
                        if (head != null) {
                            if (speerCLocal.isConnected()) {
                                PDS pd = (PDS) speerCSpeer.getProtocolStack().findProtocol("PDS");
                                speerCLocal.send(msg);
                                try {
                                    Thread.currentThread().sleep(20);
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            }
                            Message mesg = new Message(null);
                            PDS pd = (PDS) speerCLocal.getProtocolStack().findProtocol("PDS");
                            ClusterStruct aux = pd.getGlobalClusterStruct();
                            if (aux != null) {
                                ClusterHeader hea = new ClusterHeader(aux.clone());
                                mesg.putHeader("Cluster", hea);
                            }
                        }
                        UpperHeader uph = (UpperHeader) msg.getHeader("Upper");
                        if (uph != null) {
                            speerCLocal.send(msg);
                            upper = uph.upper.clone();
                            Enumeration en = upper.getGroups().elements();
                            while (en.hasMoreElements()) {
                                Group g = (Group) en.nextElement();
                                JChannel jc = (JChannel) channels_WG.get(g.getName());
                                if (jc != null) {
                                    try {
                                        jc.send(msg);
                                    } catch (ChannelNotConnectedException ex) {
                                        ex.printStackTrace();
                                    } catch (ChannelClosedException ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            }
                            synchronized (this) {
                                start = true;
                            }
                            trans.receiveStruct(msg);
                        }
                        PDSHeader pdsh = (PDSHeader) msg.getHeader("PDS");
                        if (pdsh != null) {
                            if (pdsh.tipo == PDSHeader.SP_Data) {
                                Group wg = pdsh.workGroup.clone();
                                wg.removeAllNodes();
                                JChannel jc = (JChannel) channels_WG.get(wg.getName());
                                if (jc != null) {
                                    PDS pd = (PDS) speerCSpeer.getProtocolStack().findProtocol("PDS");
                                    PDSSuperPeerSPCluster pdsp = (PDSSuperPeerSPCluster) pd.getStrategy();
                                    String ipSpeer = ((InetAddress) ((IpAddress) pdsp.getLocalAddress()).getIpAddress()).getHostAddress();
                                    GroupHeader wgh = new GroupHeader(GroupHeader.SP_Data_WorkGroup, wg, ipSpeer);
                                    msg.removeHeader("PDS");
                                    msg.putHeader("Group", wgh);
                                    jc.send(msg);
                                    if (upper.getGroup(wg.getName()).isNode(nick)) {
                                        trans.receiveData(msg.copy(), wg.getName());
                                    }
                                }
                            }
                        }
                    } catch (ChannelClosedException ex) {
                        ex.printStackTrace();
                    } catch (ChannelNotConnectedException ex) {
                        ex.printStackTrace();
                    } catch (TimeoutException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }.start();
    }

    /**
     * Method that captures every message coming fron the Peer JChannel
     * and process them generating the right messages to a proper work of the facade
     */
    private void peerLoop() {
        new Thread("FacadePopeyePeer") {

            public void run() {
                Object tmp;
                Message msg;
                while (connect) {
                    try {
                        tmp = peer.receive(0);
                        if (tmp == null) continue;
                        if (!(tmp instanceof Message)) continue;
                        msg = (Message) tmp;
                        ClusterHeader head = (ClusterHeader) msg.getHeader("Cluster");
                        if (head != null) {
                            struct = head.struct_cluster.clone();
                        }
                        UpperHeader uh = (UpperHeader) msg.getHeader("Upper");
                        if (uh != null) {
                            boolean new_group = false;
                            upper = uh.upper.clone();
                            Enumeration en = upper.getGroups().elements();
                            while (en.hasMoreElements()) {
                                Group g = (Group) en.nextElement();
                                if (upper.getGroup(g.getName()).isNode(nick) && upper_Groups.get(g.getName()) == null) {
                                    new_group = true;
                                    upper_Groups.put(g.getName(), msg);
                                }
                            }
                            if (!new_group) {
                                trans.receiveStruct(msg);
                            }
                            synchronized (this) {
                                start = true;
                            }
                        }
                        GroupHeader headwg = (GroupHeader) msg.getHeader("Group");
                        if (headwg != null) {
                            switch(headwg.tipo) {
                                case GroupHeader.SP_WorkGroup:
                                    try {
                                        channels_WG.put(headwg.wg.getName(), groupJchannel(false, headwg.wg));
                                    } catch (ChannelException ex) {
                                        ex.printStackTrace();
                                    }
                                    if (((Message) upper_Groups.get(headwg.wg.getName())) != null) {
                                        trans.receiveStruct(((Message) upper_Groups.get(headwg.wg.getName())).copy());
                                    }
                                    break;
                                case GroupHeader.SP_KO_WorkGroup:
                                    closeChannel(headwg.wg.getName());
                                    break;
                                case GroupHeader.SP_Ip_SuperPeer_WorkGroup:
                                    JChannel c = (JChannel) channels_WG.get(headwg.wg.getName());
                                    if (c != null) {
                                        GM protocolmwg = (GM) c.getProtocolStack().findProtocol("GM");
                                        IpAddress ip_aux = protocolmwg.getIpSuperPeer();
                                        protocolmwg.setIpSuperPeer(new IpAddress(headwg.wg.getIpMulticast(), ip_aux.getPort()));
                                    }
                                    break;
                            }
                        }
                        GMHeader headmwg = (GMHeader) msg.getHeader("GM");
                        if (headmwg != null && headmwg.tipo == GMHeader.SP_Port_Serv) {
                            JChannel c = (JChannel) channels_WG.get(headmwg.nom_WG);
                            if (c != null) {
                                GM protocolmwg = (GM) c.getProtocolStack().findProtocol("GM");
                                IpAddress ip_aux = protocolmwg.getIpSuperPeer();
                                protocolmwg.setIpSuperPeer(new IpAddress(ip_aux.getIpAddress(), headmwg.port));
                            } else System.out.println("JChannel null");
                        } else {
                            if (msg != null && msg.getObject() != null) {
                                trans.receiveData(msg, "Unicast");
                            }
                        }
                    } catch (ChannelClosedException ex) {
                        ex.printStackTrace();
                    } catch (ChannelNotConnectedException ex) {
                        ex.printStackTrace();
                    } catch (TimeoutException ex) {
                        ex.printStackTrace();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }.start();
    }

    /**
     * Function that create a JChannel using a
     * Group, introducing thd DRM and GM protocols
     * in the JChannel protocol stack
     * 
     * These JChannels are stored in a ConcurrentHashMap structure
     * and they are in charge of the GM protocol
     * 
     * @param speer
     * @param wg
     * @return JChannel
     */
    private JChannel groupJchannel(boolean speer, Group wg) throws ChannelException {
        return groupJchannel(speer, wg.getName(), wg.getIPMulticast());
    }

    /**
     * Function that create a JChannel using a
     * Group, introducing thd DRM and GM protocols
     * in the JChannel protocol stack
     * 
     * These JChannels are stored in a ConcurrentHashMap structure
     * and they are in charge of the GM protocol
     * 
     * @param  speer
     * @param addr_mcast
     * @param name
     * @return JChannel
     */
    private JChannel groupJchannel(boolean speer, String name, IP addr_mcast) throws ChannelException {
        JChannel canal = null;
        String ip_speer = "";
        String nick = "";
        if (speer) {
            PDS protocolpds = (PDS) speerCLocal.getProtocolStack().findProtocol("PDS");
            PDSSuperPeerLocalCluster pdsspeer = (PDSSuperPeerLocalCluster) protocolpds.getStrategy();
            nick = ";nick=" + pdsspeer.getNick();
            ip_speer = ";ip_speer=" + ((IpAddress) speerCLocal.getLocalAddress()).getIpAddress().getHostAddress();
        } else {
            PDS protocolpds = (PDS) peer.getProtocolStack().findProtocol("PDS");
            PDSPeer pdspeer = (PDSPeer) protocolpds.getStrategy();
            ip_speer = ";ip_speer=" + pdspeer.getIPSuperPeer().getIpAddress().getHostAddress();
            nick = ";nick=" + pdspeer.getNick();
        }
        String propsWG = "UDP(mcast_addr=" + addr_mcast + ";ip_ttl=32" + inter + "):" + "DRM:" + "SMACKMANET2(speer=" + speer + ";max_xmits=3;timeout=1000,2000,3000):" + "FRAG2(frag_size=1250;overhead=150):" + "GM(speer=" + speer + ";mcast=" + addr_mcast + ";name_WG=" + name + ip_speer + "" + nick + ")";
        canal = new JChannel(propsWG);
        Protocol udplocal = canal.getProtocolStack().findProtocol("UDP");
        Properties p = udplocal.getProperties();
        udplocal.setWarn(false);
        canal.connect(name);
        new PullPushAdapter(canal, mlistener);
        return canal;
    }

    /**
     * This method send a data message to the network
     * @param ident The ident parameter is the service bein executed in the upper layer
     * @param msg The message that is going to be sent
     **/
    public void sendData(int ident, Message msg) {
        GroupHeader wgh = (GroupHeader) ident_Serv.get("" + ident);
        if (wgh != null) {
            JChannel j = (JChannel) channels_WG.get(wgh.wg.getName());
            GM protocolgm = (GM) j.getProtocolStack().findProtocol("GM");
            if (protocolgm != null) {
                msg.setDest(protocolgm.getGroupMcast());
                msg.setSrc(protocolgm.getStrategy().getLocalAddress());
                msg.putHeader("Group", wgh);
                try {
                    j.send(msg);
                    if (speerCSpeer != null && speerCSpeer.isConnected()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        Message msg2 = (Message) msg.copy();
                        speerCSpeer.send(msg2);
                        trans.receiveData(msg, wgh.wg.getName());
                    }
                } catch (ChannelClosedException ex) {
                    ex.printStackTrace();
                } catch (ChannelNotConnectedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * This method sends a message to all of the members of a group
     *
     * @param msg message being sent
     * @param groupName group to send the message
     */
    private void sendToGroup(Message msg, String groupName) {
        JChannel jc = (JChannel) channels_WG.get(groupName);
        if (jc != null) {
            GM protocolgm = (GM) jc.getProtocolStack().findProtocol("GM");
            if (protocolgm != null) {
                Group gr = this.struct.getGroup(groupName).clone();
                gr.removeAllNodes();
                String ipSpeer = null;
                if (stat == SPEER) {
                    PDS pd = (PDS) speerCSpeer.getProtocolStack().findProtocol("PDS");
                    PDSSuperPeerSPCluster pdsp = (PDSSuperPeerSPCluster) pd.getStrategy();
                    ipSpeer = ((InetAddress) ((IpAddress) pdsp.getLocalAddress()).getIpAddress()).getHostAddress();
                } else if (stat == PEER) {
                    PDS pd = (PDS) peer.getProtocolStack().findProtocol("PDS");
                    PDSPeer pdsp = (PDSPeer) pd.getStrategy();
                    ipSpeer = ((InetAddress) pdsp.getIPSuperPeer().getIpAddress()).getHostAddress();
                }
                GroupHeader wgh = new GroupHeader(GroupHeader.SP_Data_WorkGroup, gr, ipSpeer);
                msg.putHeader("Group", wgh);
                msg.setDest(protocolgm.getGroupMcast());
                msg.setSrc(protocolgm.getStrategy().getLocalAddress());
                if (stat == SPEER && upper.getGroup(groupName).isNode(nick)) {
                    trans.receiveData(msg, groupName);
                } else if (stat == PEER) {
                    trans.receiveData(msg, groupName);
                }
                try {
                    jc.send(msg);
                    if (speerCSpeer != null && speerCSpeer.isConnected() && upper.getClusters().size() > 1) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        Message msg2 = (Message) msg.copy();
                        speerCSpeer.send(msg2);
                    }
                } catch (ChannelClosedException ex) {
                    ex.printStackTrace();
                } catch (ChannelNotConnectedException ex) {
                    ex.printStackTrace();
                }
            }
        } else System.out.println("ERROR PPAI.sendToGroup: JChannel for Group " + groupName + " not exist");
    }

    /**
     *
     * This method sends a message to a peer identified by his nick
     *
     * @param msg The message that is going to be sent
     * @param nick name of the peer
     *
     */
    private void sendToPeer(Message msg, String nick) {
        if (getUpperStruct() != null && getUpperStruct().isNode(nick)) {
            Enumeration en = getUpperStruct().getListNodes().elements();
            while (en.hasMoreElements()) {
                Node n = (Node) en.nextElement();
                if (n.getNick().compareToIgnoreCase(nick) == 0) {
                    if (this.stat == this.PEER) {
                        msg.setDest(n.getIpAddress());
                        try {
                            peer.send(msg);
                        } catch (ChannelClosedException ex) {
                            ex.printStackTrace();
                        } catch (ChannelNotConnectedException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        msg.setDest(n.getIpAddress());
                        try {
                            speerCLocal.send(msg);
                        } catch (ChannelClosedException ex) {
                            ex.printStackTrace();
                        } catch (ChannelNotConnectedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * This method sends a message to a peer given by its name
     * @param peerName The name of the peer
     * @param msg The msg that must be sent
     **/
    public void sendPopeyeMessage(String peerName, Message msg) {
        sendToPeer(msg, peerName);
    }

    /**
     * This method sends a message to a group of peers
     *
     * @param groupName The name of the group of peers
     * @param msg The message that must be sent
     **/
    public void sendGroupPopeyeMessage(String groupName, Message msg) {
        sendToGroup(msg, groupName);
    }

    /**
     * This method initializes the Adapter connecting the necessary channels
     **/
    public void startAdapter() throws ChannelException {
        connect();
    }

    /**
     * This method stop the Adapter
     **/
    public void stopAdapter() {
        disconnect();
    }

    /**
     * This method returns the view of a group passed as parameter
     * @param groupName The name of the group whose view you are interested in
     **/
    private View getView(String groupName) {
        return getClusterStruct().getGroupView(groupName);
    }

    /**
     * Method that lets a peer to join a group
     * @param groupName The name of the group you want to join
     */
    public void joinGroup(String groupName) {
        if (stat == PEER && struct != null && struct.isGroup(groupName)) {
            Message msg = new Message(null);
            Group wg = new Group(groupName, struct.getGroup(groupName).getIPMulticast());
            GroupHeader head = new GroupHeader(GroupHeader.SP_WorkGroup, wg);
            msg.putHeader("Group", head);
            Event evt = new Event(Event.MSG, msg);
            peer.down(evt);
        } else if (stat == SPEER) {
            PDS pd = (PDS) speerCLocal.getProtocolStack().findProtocol("PDS");
            ClusterStruct aux = pd.getGlobalClusterStruct();
            if (aux.isGroup(groupName)) {
                Message msg = new Message(null);
                Group wg = new Group(groupName, aux.getGroup(groupName).getIPMulticast());
                GroupHeader head = new GroupHeader(GroupHeader.SP_WorkGroup, wg);
                msg.putHeader("Group", head);
                SPeerHeader sph = new SPeerHeader();
                msg.putHeader("SPeer", sph);
                Event evt = new Event(Event.MSG, msg);
                speerCLocal.down(evt);
            }
        } else System.out.println("ERROR: joinGroup " + groupName + " struct " + this.struct);
    }

    /**
     * Method that allows a peer to create a group
     * @param groupName Name of the group
     * @param groupAddr Multicast address of the new group
     */
    public void newGroup(String groupName, String groupAddr) {
        if (stat == PEER && struct != null && !struct.isGroup(groupName)) {
            Message msg = new Message(null);
            Group wg = new Group(groupName, groupAddr);
            GroupHeader head = new GroupHeader(GroupHeader.SP_New_WorkGroup, wg);
            msg.putHeader("Group", head);
            Event evt = new Event(Event.MSG, msg);
            peer.down(evt);
        } else if (stat == SPEER && struct != null && !struct.isGroup(groupName)) {
            Message msg = new Message(null);
            Group wg = new Group(groupName, groupAddr);
            GroupHeader head = new GroupHeader(GroupHeader.SP_New_WorkGroup, wg);
            msg.putHeader("Group", head);
            SPeerHeader sph = new SPeerHeader();
            msg.putHeader("SPeer", sph);
            Event evt = new Event(Event.MSG, msg);
            speerCLocal.down(evt);
        }
    }

    /**
     * Method that lets a peer to leave a group
     * @param groupName The name of the group
     */
    public void leaveGroup(String groupName) {
        if (stat == PEER && channels_WG.get(groupName) != null) {
            Message msg = new Message(null);
            Group wg = new Group(groupName, struct.getGroup(groupName).getIPMulticast());
            GroupHeader head = new GroupHeader(GroupHeader.SP_KO_WorkGroup, wg);
            msg.putHeader("Group", head);
            Event evt = new Event(Event.MSG, msg);
            peer.down(evt);
        } else if (stat == SPEER && channels_WG.get(groupName) != null) {
            Message msg = new Message(null);
            Group wg = new Group(groupName, struct.getGroup(groupName).getIPMulticast());
            GroupHeader head = new GroupHeader(GroupHeader.SP_KO_WorkGroup, wg);
            msg.putHeader("Group", head);
            Event evt = new Event(Event.MSG, msg);
            SPeerHeader sph = new SPeerHeader();
            msg.putHeader("SPeer", sph);
            speerCLocal.down(evt);
        }
    }

    /**
     *
     * Method that allows a peer to create a group of service
     * @param groupName the name of the group where is located the service group
     * @param servName the name of the service.
     * @param servGroupName Name of the Service Group
     *   
     */
    public int newServiceGroup(String groupName, String servName, String servGroupName) {
        int ident = -1;
        if (struct != null) {
            Group wg = struct.getGroup(groupName);
            if (wg != null && !wg.isServiceGroupInService(servGroupName, servName)) {
                JChannel channelwg = (JChannel) channels_WG.get(groupName);
                if (channelwg != null) {
                    PDS protocolpds;
                    if (stat == PEER) {
                        protocolpds = (PDS) peer.getProtocolStack().findProtocol("PDS");
                    } else {
                        protocolpds = (PDS) speerCLocal.getProtocolStack().findProtocol("PDS");
                    }
                    Serializable obj = protocolpds.getProfileIdentifier();
                    GM protocolGM = (GM) channelwg.getProtocolStack().findProtocol("GM");
                    String ip_m = protocolGM.getGroupMcast().getIpAddress().getHostAddress();
                    String ip = ((IpAddress) protocolGM.getStrategy().getLocalAddress()).getIpAddress().getHostAddress();
                    String nick = protocolGM.getNick();
                    int port = ((IpAddress) protocolGM.getStrategy().getLocalAddress()).getPort();
                    Message msg = new Message(null);
                    GMHeader head = new GMHeader(GMHeader.SP_New_Serv, servName, servGroupName, nick);
                    msg.putHeader("GM", head);
                    Event evt = new Event(Event.MSG, msg);
                    msg.setSrc((IpAddress) protocolGM.getStrategy().getLocalAddress());
                    channelwg.down(evt);
                    ServiceGroup gs = new ServiceGroup(servGroupName);
                    gs.addNode(ip, nick, port, obj);
                    Service s = new Service(servName);
                    s.addServiceGroup(gs);
                    Group workg = new Group(groupName, ip_m);
                    workg.addService(s);
                    String ipSpeer = null;
                    if (stat == SPEER) {
                        PDS pd = (PDS) speerCSpeer.getProtocolStack().findProtocol("PDS");
                        PDSSuperPeerSPCluster pdsp = (PDSSuperPeerSPCluster) pd.getStrategy();
                        ipSpeer = ((InetAddress) ((IpAddress) pdsp.getLocalAddress()).getIpAddress()).getHostAddress();
                    } else if (stat == PEER) {
                        PDS pd = (PDS) peer.getProtocolStack().findProtocol("PDS");
                        PDSPeer pdsp = (PDSPeer) pd.getStrategy();
                        ipSpeer = ((InetAddress) pdsp.getIPSuperPeer().getIpAddress()).getHostAddress();
                    }
                    GroupHeader headwg = new GroupHeader(GroupHeader.SP_Data_WorkGroup, workg, ipSpeer);
                    boolean ex = true;
                    Random enteros = new Random();
                    while (ex) {
                        ident = enteros.nextInt();
                        Object ob = ident_Serv.get("" + ident);
                        if (ob == null) {
                            ident_Serv.put("" + ident, headwg);
                            ident_Head.put(headwg, "" + ident);
                            ex = false;
                        }
                    }
                } else System.out.println("JChannel channelwg null");
            }
        }
        return ident;
    }

    /**
     * Method that allows a peer to join a service group
     * @param groupName the name of the group where is located the service group
     * @param servName the name of the service.
     * @param servGroupName Name of the GroupService
     * @return It will return a service group identifier
     */
    public int joinServiceGroup(String groupName, String servName, String servGroupName) {
        int ident = -1;
        Group wg = null;
        if (struct != null && stat == PEER) {
            wg = struct.getGroup(groupName);
        } else {
            PDS pd = (PDS) speerCLocal.getProtocolStack().findProtocol("PDS");
            if (pd != null) wg = pd.getGlobalClusterStruct().getGroup(groupName);
        }
        if (wg != null && wg.isServiceGroupInService(servGroupName, servName)) {
            JChannel channelwg = (JChannel) channels_WG.get(groupName);
            if (channelwg != null) {
                PDS protocolpds;
                if (stat == PEER) {
                    protocolpds = (PDS) peer.getProtocolStack().findProtocol("PDS");
                } else {
                    protocolpds = (PDS) speerCLocal.getProtocolStack().findProtocol("PDS");
                }
                Serializable obj = protocolpds.getProfileIdentifier();
                GM protocolGM = (GM) channelwg.getProtocolStack().findProtocol("GM");
                String ip_m = protocolGM.getGroupMcast().getIpAddress().getHostAddress();
                String ip = ((IpAddress) protocolGM.getStrategy().getLocalAddress()).getIpAddress().getHostAddress();
                int port = ((IpAddress) protocolGM.getStrategy().getLocalAddress()).getPort();
                Message msg = new Message(null);
                GMHeader head = new GMHeader(GMHeader.SP_Serv, servName, servGroupName, nick);
                msg.putHeader("GM", head);
                msg.setSrc((IpAddress) protocolGM.getStrategy().getLocalAddress());
                Event evt = new Event(Event.MSG, msg);
                channelwg.down(evt);
                ServiceGroup gs = new ServiceGroup(servGroupName);
                gs.addNode(ip, nick, port, obj);
                Service s = new Service(servName);
                s.addServiceGroup(gs);
                Group workg = new Group(groupName, ip_m);
                workg.addService(s);
                String ipSpeer = null;
                if (stat == SPEER) {
                    PDS pd = (PDS) speerCSpeer.getProtocolStack().findProtocol("PDS");
                    PDSSuperPeerSPCluster pdsp = (PDSSuperPeerSPCluster) pd.getStrategy();
                    ipSpeer = ((InetAddress) ((IpAddress) pdsp.getLocalAddress()).getIpAddress()).getHostAddress();
                } else if (stat == PEER) {
                    PDS pd = (PDS) peer.getProtocolStack().findProtocol("PDS");
                    PDSPeer pdsp = (PDSPeer) pd.getStrategy();
                    ipSpeer = ((InetAddress) pdsp.getIPSuperPeer().getIpAddress()).getHostAddress();
                }
                GroupHeader headwg = new GroupHeader(GroupHeader.SP_Data_WorkGroup, workg, ipSpeer);
                boolean ex = true;
                Random enteros = new Random();
                while (ex) {
                    ident = enteros.nextInt();
                    Object ob = ident_Serv.get("" + ident);
                    if (ob == null) {
                        ident_Serv.put("" + ident, headwg);
                        ident_Head.put(headwg, "" + ident);
                        ex = false;
                    }
                }
            } else System.out.println("JChannel channelwg null");
        }
        return ident;
    }

    /**
     * Method that allows a peer to leave a service group
     * @param groupName the name of the group where is located the service group
     * @param servName the name of the service.
     * @param servGroupName Name of the Service Group
     */
    public void leaveServiceGroup(String groupName, String servName, String servGroupName) {
        if (struct != null) {
            Group wg = struct.getGroup(groupName);
            if (wg != null && wg.isServiceGroupInService(servGroupName, servName)) {
                JChannel channelwg = (JChannel) channels_WG.get(groupName);
                if (channelwg != null) {
                    Message msg = new Message(null);
                    GMHeader head = new GMHeader(GMHeader.SP_KO_Serv_Nick, servName, servGroupName, nick);
                    msg.putHeader("GM", head);
                    Event evt = new Event(Event.MSG, msg);
                    channelwg.down(evt);
                } else System.out.println("JChannel channelwg null");
            }
        }
    }

    /**
     * Method to disconnect all the channels
     */
    private void disconnectChannels() {
        if (!channels_WG.keySet().isEmpty()) {
            Iterator it = channels_WG.values().iterator();
            while (it.hasNext()) {
                JChannel can = (JChannel) it.next();
                can.disconnect();
                can.close();
            }
            channels_WG = new ConcurrentHashMap();
        }
    }

    /**
     * Function that returns true when a Jchannel (group) exists and is able to close it,
     * otherwise return false
     * @param name
     */
    private boolean closeChannel(String name) {
        if (!channels_WG.keySet().isEmpty()) {
            JChannel can = (JChannel) channels_WG.get(name);
            if (can != null) {
                can.disconnect();
                can.close();
                channels_WG.remove(name);
                return true;
            }
        }
        return false;
    }

    private void getInterface() {
        try {
            String aux = PropertiesLoader.getNetworkInterface();
            Enumeration en = java.net.NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                java.net.NetworkInterface ni = (java.net.NetworkInterface) en.nextElement();
                if (ni.getDisplayName().equalsIgnoreCase(aux)) {
                    Enumeration enu = ni.getInetAddresses();
                    while (enu.hasMoreElements()) {
                        String ia = ((InetAddress) enu.nextElement()).getHostAddress();
                        if (ia.indexOf(':') == -1) {
                            inter = inter + ia;
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Class that implements the ChannelListener     
     */
    private class ChannelList implements ChannelListener {

        public void ChannelList() {
        }

        public void channelConnected(Channel channel) {
            String gc = channel.getChannelName();
            switch(stat) {
                case SPEER:
                    if (gc.equals("MANET")) {
                        sPeerCLocalLoop();
                    } else if (gc.equals("SPEER")) {
                        sPeerCSPeerLoop();
                    }
                    break;
                case PEER:
                    if (gc.equals("MANET")) {
                        peerLoop();
                    }
                    break;
            }
        }

        public void channelDisconnected(Channel channel) {
        }

        public void channelClosed(Channel channel) {
        }

        public void channelShunned() {
        }

        public void channelReconnected(Address addr) {
        }
    }

    /**
     * Class that implements MessageListener
     * Recive los handleUP del canal del grupo
     */
    private class MessageList implements MessageListener {

        public void MessaListener() {
        }

        public void receive(Message msg) {
            GroupHeader head = (GroupHeader) msg.getHeader("Group");
            if (head != null) {
                switch(head.tipo) {
                    case GroupHeader.SP_WorkGroup:
                        if (stat == PullPushAdapterInterface.SPEER) {
                            try {
                                speerCLocal.send(msg);
                            } catch (ChannelNotConnectedException ex) {
                                ex.printStackTrace();
                            } catch (ChannelClosedException ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            try {
                                peer.send(msg);
                            } catch (ChannelNotConnectedException ex) {
                                ex.printStackTrace();
                            } catch (ChannelClosedException ex) {
                                ex.printStackTrace();
                            }
                        }
                        break;
                    case GroupHeader.SP_Data_WorkGroup:
                        if (stat == PullPushAdapterInterface.SPEER) {
                            try {
                                SPeerHeader sph = (SPeerHeader) msg.getHeader("SPeer");
                                if (sph == null) {
                                    IpAddress ip = (IpAddress) msg.getSrc();
                                    String sip = ip.getIpAddress().getHostAddress().toString();
                                    UDP udp = (UDP) speerCLocal.getProtocolStack().findProtocol("UDP");
                                    String iplocal = ((IpAddress) udp.getLocalAddress()).getIpAddress().getHostAddress().toString();
                                    boolean filter = false;
                                    UpperStruct up = getUpperStruct().clone();
                                    ClusterStruct us = up.getCluster(iplocal);
                                    if (us != null && us.isNodeinClusterStruct(new IP(sip))) {
                                        filter = true;
                                    }
                                    if (upper.getGroup(head.wg.getName()).isNode(nick) && filter) {
                                        trans.receiveData(msg, head.wg.getName());
                                    }
                                    if (filter) speerCSpeer.send(msg);
                                }
                            } catch (ChannelNotConnectedException ex) {
                                ex.printStackTrace();
                            } catch (ChannelClosedException ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            SPeerHeader sph = (SPeerHeader) msg.getHeader("SPeer");
                            PDS protpds = (PDS) peer.getProtocolStack().findProtocol("PDS");
                            PDSPeer pdsp = (PDSPeer) protpds.getStrategy();
                            String ip_sp_cluster = pdsp.getIPSuperPeer().getIpAddress().getHostAddress().toString();
                            if (sph != null) {
                                IpAddress ip = (IpAddress) msg.getSrc();
                                String sip = ip.getIpAddress().getHostAddress().toString();
                                if (sip.equalsIgnoreCase(ip_sp_cluster)) trans.receiveData(msg, head.wg.getName());
                            } else {
                                if (head.ip.equalsIgnoreCase(ip_sp_cluster)) trans.receiveData(msg, head.wg.getName());
                            }
                        }
                        break;
                }
            }
        }

        public byte[] getState() {
            return null;
        }

        public void setState(byte[] state) {
        }
    }
}
