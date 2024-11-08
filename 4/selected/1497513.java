package isc.sensor;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import org.apache.log4j.Logger;

/**
 * 
 * The UDP Session processor manage the full lifecycle for each tcp session, defined by a client/port server/port pair.
 * 
 * The Session Processor runs in its own thread
 * 
 * @author John Casey
 * 
 * <br>
 * Project: DNA_sensor - Aug 12, 2005</li>
 *  
  * <p>
 * TODO: 
 * <ul>
 * <li>Externalize SVNUM
 * <li>Parent/child session definition
 * <li>Dynamic signature service definition
 * </ul>
* 
 */
public class UDPSessionProcessor extends SessionProcessor {

    /** Array of well-known service ports */
    protected static final int SVCNUMS[] = { 1, 5, 7, 9, 11, 13, 15, 17, 18, 19, 20, 21, 22, 23, 25, 37, 39, 42, 43, 49, 50, 53, 63, 67, 68, 69, 70, 71, 72, 73, 74, 79, 80, 88, 95, 98, 106, 101, 102, 105, 106, 107, 109, 110, 111, 113, 115, 117, 119, 123, 137, 138, 139, 143, 161, 163, 164, 174, 177, 178, 179, 191, 194, 199, 201, 202, 204, 206, 209, 213, 220, 245, 347, 363, 369, 370, 372, 389, 427, 434, 435, 443, 444, 445, 464, 465, 468, 487, 488, 496, 500, 512, 513, 514, 515, 519, 520, 521, 525, 526, 530, 531, 532, 535, 538, 540, 543, 544, 546, 547, 548, 554, 556, 563, 565, 587, 610, 611, 612, 616, 631, 636, 674, 694, 749, 750, 751, 754, 760, 765, 767, 808, 871, 873, 992, 993, 994, 995, 901, 953, 1080, 1109, 1127, 1178, 1236, 1300, 1313, 1433, 1434, 1494, 1512, 1524, 1525, 1529, 1645, 1646, 1649, 1701, 1718, 1719, 1720, 1758, 1789, 1812, 1813, 1863, 1911, 1985, 1986, 1997, 2003, 2049, 2053, 2102, 2103, 2104, 2105, 2150, 2401, 2430, 2431, 2432, 2433, 2600, 2601, 2602, 2603, 2604, 2605, 2606, 2809, 2988, 3128, 3130, 3306, 3346, 3455, 4321, 4444, 4557, 4559, 5002, 5232, 5308, 5354, 5355, 5432, 5680, 5999, 6010, 6667, 7100, 7666, 8008, 8080, 8081, 9100, 6000, 7000, 7001, 7002, 7003, 7004, 7005, 7006, 7007, 7008, 7009, 7777, 8245, 9876, 10080, 10081, 10082, 10083, 11371, 11720, 13720, 13721, 13722, 13724, 13782, 13783, 22273, 20011, 20012, 22305, 22289, 22321, 24554, 26000, 26208, 27374, 33434, 60177, 60179 };

    /** Array of well-know services */
    protected static final String SVCNAMES[] = { "tcpmux", "rje", "echo", "discard", "systat", "daytime", "netstat", "qotd", "msp", "chargen", "ftp-data", "ftp", "ssh", "telnet", "smtp", "time", "rlp", "nameserver", "nicname", "tacacs", "re-mail-ck", "domain", "whois++", "bootps", "bootpc", "tftp", "gopher", "netrjs-1", "netrjs-2", "netrjs-3", "netrjs-4", "finger", "http", "kerberos", "supdup", "linuxconf", "poppassd", "hostname", "iso-tsap", "csnet-ns", "#3com-tsmux", "rtelnet", "pop2", "pop3", "sunrpc", "auth", "sftp", "uucp-path", "nntp", "ntp", "netbios-ns", "netbios-dgm", "netbios-ssn", "imap", "snmp", "cmip-man", "cmip-agent", "mailq", "xdmcp", "nextstep", "bgp", "prospero", "irc", "smux", "at-rtmp", "at-nbp", "at-echo", "at-zis", "qmtp", "ipx", "imap3", "link", "fatserv", "rsvp_tunnel", "rpc2portmap", "codaauth2", "ulistproc", "ldap", "svrloc", "mobileip-agent", "mobilip-mn", "https", "snpp", "microsoft-ds", "kpasswd", "smtps", "photuris", "saft", "gss-http", "pim-rp-disc", "isakmp", "exec", "login", "shell", "printer", "utime", "efs", "ripng", "timed", "tempo", "courier", "conference", "netnews", "iiop", "gdomap", "uucp", "klogin", "kshell", "dhcpv6-client", "dhcpv6-server", "afpovertcp", "rtsp", "remotefs", "nntps", "whoami", "submission", "npmp-local", "npmp-gui", "hmmp-ind", "gii", "ipp", "ldaps", "acap", "ha-cluster", "kerberos-adm", "kerberos-iv", "kerberos_master", "krb5_prop", "krbupdate", "webster", "phonebook", "omirr", "supfilesrv", "rsync", "telnets", "imaps", "ircs", "pop3s", "swat", "rndc", "socks", "kpop", "supfiledbg", "skkserv", "bvcontrol", "h323hostcallsc", "xtel", "ms-sql-s", "ms-sql-m", "ica", "wins", "ingreslock", "prospero-np", "support", "datametrics", "sa-msg-port", "kermit", "l2tp", "h323gatedisc", "h323gatestat", "h323hostcall", "tftp-mcast", "hello", "radius", "radius-acct", "MSN-Messenger", "mtp", "hsrp", "licensedaemon", "gdp-port", "cfinger", "nfs", "knetd", "zephyr-srv", "zephyr-clt", "zephyr-hm", "eklogin", "ninstall", "cvspserver", "venus", "venus-se", "codasrv", "codasrv-se", "hpstgmgr", "discp-client", "discp-server", "servicemeter", "nsc-ccs", "nsc-posa", "netmon", "corbaloc", "afbackup", "squid", "icpv2", "mysql", "trnsprntproxy", "prsvp", "rwhois", "krb524", "fax", "hylafax", "rfe", "sgi-dgl", "cfengine", "noclog", "hostmon", "postgres", "canna", "cvsup", "x11-ssh-offset", "ircd", "xfs", "tircproxy", "http-alt", "webcache", "tproxy", "jetdirect", "x11", "afs3-fileserver", "afs3-callback", "afs3-prserver", "afs3-vlserver", "afs3-kaserver", "afs3-volser", "afs3-errors", "afs3-bos", "afs3-update", "afs3-rmtsys", "DynSite", "Dynsite", "sd", "amanda", "kamanda", "amandaidx", "amidxtape", "pgpkeyserver", "h323callsigalt", "bprd", "bpdbm", "bpjava-msvc", "vnetd", "bpcd", "vopied", "wnn6", "isdnlog", "vboxd", "wnn4_Kr", "wnn4_Cn", "wnn4_Tw", "binkp", "quake", "wnn6-ds", "asp", "traceroute", "tfido", "fido" };

    /** Property to enable/disable session output */
    protected static final String PROP_SESSION_OUTPUT = "OutputType.UDPSession";

    /** Property to specify the initial size of the packet work queue */
    protected static final String PROP_WORKQ_SIZE = "SessionProcessor.WorkQSize";

    /** Property to specify the initial size of the active session hash table */
    protected static final String PROP_SESHASH_SIZE = "SessionProcessor.SesHashSize";

    /** Property to specify the initial size of the inflight packet flow hashtable */
    protected static final String PROP_DBSIZE = "SessionProcessor.PacketDBSize";

    /** log4j */
    protected static Logger log = Logger.getLogger(UDPSessionProcessor.class);

    /**
	 * Create a new UDP Session processor, internally controlled Thread
	 * 
	 */
    public UDPSessionProcessor() {
        log.debug(">>Constructor()");
        writeSession = new Boolean(props.getProperty(PROP_SESSION_OUTPUT));
        init(true, writeSession.booleanValue());
    }

    /**
	 * Create a new UDP Session processor, stub for a subclass to manage the initialization of the thread
	 * 
	 * @param startThread unused 
	 */
    public UDPSessionProcessor(boolean startThread) {
        log.debug(">>Constructor(overRide)");
    }

    /**
	 * Initialize this session processor from this class or a subclass.  External management of the writer flag and thread creation
	 * 
	 * 
	 * @param startThread	True to start the thread internally, False to start/manage the thread from a subclass
	 * @param writeSession  True to write out the session record, False to disable
	 */
    protected void init(boolean startThread, boolean writeSession) {
        log.debug(">>init(" + Boolean.valueOf(startThread) + ")");
        sesWorkQueue = new Vector(Integer.parseInt(props.getProperty(PROP_WORKQ_SIZE)));
        if (writeSession || !startThread) {
            sessions = new Hashtable(Integer.parseInt(props.getProperty(PROP_SESHASH_SIZE)));
            packetDB = new Hashtable(Integer.parseInt(props.getProperty(PROP_DBSIZE)));
            open();
            sesTimer = new UDPSessionTimer(this);
            sesTimer.start();
            log.debug("Start Thread = " + Boolean.valueOf(startThread));
            if (startThread) this.start();
        }
    }

    /**
	 * Add a packet to the current session's packet summary DB
	 * 
	 * 
	 * @param bean The UDPBean to add
	 */
    public void insertBeanDB(UDPBean bean) throws Exception {
        UDPSumBean s;
        if (!writeSession.booleanValue()) return;
        synchronized (packetDB) {
            if (packetDB.containsKey(bean.skey.toString())) {
                Vector rows = (Vector) packetDB.get(bean.skey.toString());
                if (bean.ipbean.dstAddrBin.compareTo(bean.ipbean.srcAddrBin) > 0) {
                    s = (UDPSumBean) rows.elementAt(0);
                    s.addPacket(bean);
                } else {
                    s = (UDPSumBean) rows.elementAt(1);
                    s.addPacket(bean);
                }
            } else {
                Vector rows = new Vector();
                if (bean.ipbean.dstAddrBin.compareTo(bean.ipbean.srcAddrBin) > 0) {
                    s = new UDPSumBean(bean);
                    rows.add(0, s);
                    rows.add(1, new UDPSumBean());
                } else {
                    s = new UDPSumBean(bean);
                    rows.add(0, new UDPSumBean());
                    rows.add(1, s);
                }
                packetDB.put(bean.skey.toString(), rows);
            }
        }
    }

    /**
	 * Process the sesWorkQueue containing completed sessions @see SessionProcessor#addCompletedSession(String)
	 */
    protected void processWorkItem() {
        if (!writeSession.booleanValue()) return;
        synchronized (sesWorkQueue) {
            workItem = (Vector) sesWorkQueue.firstElement();
            sesWorkQueue.remove(0);
        }
        Enumeration array = workItem.elements();
        SessionBean udpsbean = new SessionBean();
        udpsbean.flow1 = (PacketSumBean) workItem.get(0);
        udpsbean.flow2 = (PacketSumBean) workItem.get(1);
        boolean emptyFlows = false;
        int i;
        if ((!udpsbean.flow1.empty) && (!udpsbean.flow2.empty)) {
            if (udpsbean.flow1.ipsbean.firstPacketTime.after(udpsbean.flow2.ipsbean.firstPacketTime)) {
                udpsbean.flow1 = (PacketSumBean) workItem.get(1);
                udpsbean.flow2 = (PacketSumBean) workItem.get(0);
            }
        } else if (!udpsbean.flow1.empty) {
            udpsbean.flow1 = (PacketSumBean) workItem.get(0);
            udpsbean.flow2 = (PacketSumBean) workItem.get(1);
        } else if (!udpsbean.flow2.empty) {
            udpsbean.flow2 = (PacketSumBean) workItem.get(0);
            udpsbean.flow1 = (PacketSumBean) workItem.get(1);
        } else {
            emptyFlows = true;
            log.fatal("both flows are empty");
            System.exit(-1);
        }
        i = Arrays.binarySearch(SVCNUMS, udpsbean.flow1.ipsbean.dstPort.intValue());
        if (i >= 0) {
            udpsbean.svcName = SVCNAMES[i];
            udpsbean.clientAddr = udpsbean.flow1.ipsbean.srcAddr;
            udpsbean.clientPort = udpsbean.flow1.ipsbean.srcPort;
            udpsbean.serverAddr = udpsbean.flow1.ipsbean.dstAddr;
            udpsbean.serverPort = udpsbean.flow1.ipsbean.dstPort;
            udpsbean.startTime = udpsbean.flow1.ipsbean.firstPacketTime;
            udpsbean.skey = udpsbean.flow1.sKey;
        } else {
            i = Arrays.binarySearch(SVCNUMS, udpsbean.flow1.ipsbean.srcPort.intValue());
            if (i >= 0) {
                udpsbean.svcName = SVCNAMES[i];
                udpsbean.clientAddr = udpsbean.flow1.ipsbean.dstAddr;
                udpsbean.clientPort = udpsbean.flow1.ipsbean.dstPort;
                udpsbean.serverAddr = udpsbean.flow1.ipsbean.srcAddr;
                udpsbean.serverPort = udpsbean.flow1.ipsbean.srcPort;
                udpsbean.startTime = udpsbean.flow1.ipsbean.firstPacketTime;
                udpsbean.skey = udpsbean.flow1.sKey;
            } else {
                udpsbean.svcName = "unknown";
                udpsbean.clientAddr = udpsbean.flow1.ipsbean.srcAddr;
                udpsbean.clientPort = udpsbean.flow1.ipsbean.srcPort;
                udpsbean.serverAddr = udpsbean.flow1.ipsbean.dstAddr;
                udpsbean.serverPort = udpsbean.flow1.ipsbean.dstPort;
                udpsbean.startTime = udpsbean.flow1.ipsbean.firstPacketTime;
                udpsbean.skey = udpsbean.flow1.sKey;
            }
        }
        udpsbean.dataSent = udpsbean.flow1.ipsbean.totalPktLen;
        udpsbean.packetsSent = udpsbean.flow1.ipsbean.numPackets;
        udpsbean.dataRecv = udpsbean.flow2.ipsbean.totalPktLen;
        udpsbean.packetsRecv = udpsbean.flow2.ipsbean.numPackets;
        udpsbean.status = new String("open");
        if (udpsbean.flow1.ipsbean.lastPacketTime.after(udpsbean.flow2.ipsbean.lastPacketTime)) {
            udpsbean.duration = Long.valueOf(udpsbean.flow1.ipsbean.lastPacketTime.getTime() - udpsbean.startTime.getTime());
        } else {
            udpsbean.duration = Long.valueOf(udpsbean.flow2.ipsbean.lastPacketTime.getTime() - udpsbean.startTime.getTime());
        }
        try {
            ((PacketOutputAdaptorIF) dbh).writeUDPSession(udpsbean);
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    /**
	 * Add a packet to be process into a session
	 * 
	 * 
	 * @param udpbean The UDPBean to be processed 
	 */
    public void addPacket(UDPBean udpbean) {
        if (!writeSession.booleanValue()) return;
        updatePacketClock(udpbean.ipbean.timeStamp.getTime());
        try {
            insertBeanDB(udpbean);
        } catch (Exception e) {
            log.error(e, e);
        }
        synchronized (sessions) {
            if (sessions.containsKey(udpbean.skey)) {
                SessionHash shb = (SessionHash) sessions.get(udpbean.skey);
                shb.tickOfLastPacket = 0;
                shb.packetTimeOfLastPacket = currentPacketTime;
            } else {
                SessionHash shb = new SessionHash(udpbean.skey.toString());
                shb.packetTimeOfLastPacket = currentPacketTime;
                sessions.put(shb.sessionKey, shb);
            }
        }
    }

    /**
	 * Flush/write out a completed UDP Session
	 * 
	 * 
	 * @param udpsbean The UDPSession to flush
	 */
    protected void flush(SessionBean udpsbean) {
        if (!writeSession.booleanValue()) return;
        try {
            ((PacketOutputAdaptorIF) dbh).writeUDPSession(udpsbean);
        } catch (Exception e) {
            log.error(e, e);
        }
    }
}
