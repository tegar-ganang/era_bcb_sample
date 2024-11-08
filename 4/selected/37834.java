package isc.sensor;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.log4j.Logger;

/**
 * The TCP Session processor manage the full lifecycle for each tcp session, defined by a client/port server/port pair.
 * 
 * The Session processor runs in its own Thread
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
 * <li>Process packet retransmissions 
 * <li>Parent/child session definition
 * <li>Dynamic signature service definition
 * </ul>
 */
public class TCPSessionProcessor extends SessionProcessor {

    /** Array of well-known service ports */
    protected static final int svcnums[] = { 1, 5, 7, 9, 11, 13, 15, 17, 18, 19, 20, 21, 22, 23, 25, 37, 39, 42, 43, 49, 50, 53, 63, 67, 68, 69, 70, 71, 72, 73, 74, 79, 80, 88, 95, 98, 106, 101, 102, 105, 106, 107, 109, 110, 111, 113, 115, 117, 119, 123, 137, 138, 139, 143, 161, 163, 164, 174, 177, 178, 179, 191, 194, 199, 201, 202, 204, 206, 209, 213, 220, 245, 347, 363, 369, 370, 372, 389, 427, 434, 435, 443, 444, 445, 464, 465, 468, 487, 488, 496, 500, 512, 513, 514, 515, 519, 520, 521, 525, 526, 530, 531, 532, 535, 538, 540, 543, 544, 546, 547, 548, 554, 556, 563, 565, 587, 610, 611, 612, 616, 631, 636, 674, 694, 749, 750, 751, 754, 760, 765, 767, 808, 871, 873, 992, 993, 994, 995, 901, 953, 1080, 1109, 1127, 1178, 1236, 1300, 1313, 1433, 1434, 1494, 1512, 1524, 1525, 1529, 1645, 1646, 1649, 1701, 1718, 1719, 1720, 1758, 1789, 1812, 1813, 1863, 1911, 1985, 1986, 1997, 2003, 2049, 2053, 2102, 2103, 2104, 2105, 2150, 2401, 2430, 2431, 2432, 2433, 2600, 2601, 2602, 2603, 2604, 2605, 2606, 2809, 2988, 3128, 3130, 3306, 3346, 3455, 4321, 4444, 4557, 4559, 5002, 5232, 5308, 5354, 5355, 5432, 5680, 5999, 6010, 6667, 7100, 7666, 8008, 8080, 8081, 9100, 6000, 7000, 7001, 7002, 7003, 7004, 7005, 7006, 7007, 7008, 7009, 7777, 8245, 9876, 10080, 10081, 10082, 10083, 11371, 11720, 13720, 13721, 13722, 13724, 13782, 13783, 22273, 20011, 20012, 22305, 22289, 22321, 24554, 26000, 26208, 27374, 33434, 60177, 60179 };

    /** Array of well-know services */
    protected static final String svcnams[] = { "tcpmux", "rje", "echo", "discard", "systat", "daytime", "netstat", "qotd", "msp", "chargen", "ftp-data", "ftp", "ssh", "telnet", "smtp", "time", "rlp", "nameserver", "nicname", "tacacs", "re-mail-ck", "domain", "whois++", "bootps", "bootpc", "tftp", "gopher", "netrjs-1", "netrjs-2", "netrjs-3", "netrjs-4", "finger", "http", "kerberos", "supdup", "linuxconf", "poppassd", "hostname", "iso-tsap", "csnet-ns", "#3com-tsmux", "rtelnet", "pop2", "pop3", "sunrpc", "auth", "sftp", "uucp-path", "nntp", "ntp", "netbios-ns", "netbios-dgm", "netbios-ssn", "imap", "snmp", "cmip-man", "cmip-agent", "mailq", "xdmcp", "nextstep", "bgp", "prospero", "irc", "smux", "at-rtmp", "at-nbp", "at-echo", "at-zis", "qmtp", "ipx", "imap3", "link", "fatserv", "rsvp_tunnel", "rpc2portmap", "codaauth2", "ulistproc", "ldap", "svrloc", "mobileip-agent", "mobilip-mn", "https", "snpp", "microsoft-ds", "kpasswd", "smtps", "photuris", "saft", "gss-http", "pim-rp-disc", "isakmp", "exec", "login", "shell", "printer", "utime", "efs", "ripng", "timed", "tempo", "courier", "conference", "netnews", "iiop", "gdomap", "uucp", "klogin", "kshell", "dhcpv6-client", "dhcpv6-server", "afpovertcp", "rtsp", "remotefs", "nntps", "whoami", "submission", "npmp-local", "npmp-gui", "hmmp-ind", "gii", "ipp", "ldaps", "acap", "ha-cluster", "kerberos-adm", "kerberos-iv", "kerberos_master", "krb5_prop", "krbupdate", "webster", "phonebook", "omirr", "supfilesrv", "rsync", "telnets", "imaps", "ircs", "pop3s", "swat", "rndc", "socks", "kpop", "supfiledbg", "skkserv", "bvcontrol", "h323hostcallsc", "xtel", "ms-sql-s", "ms-sql-m", "ica", "wins", "ingreslock", "prospero-np", "support", "datametrics", "sa-msg-port", "kermit", "l2tp", "h323gatedisc", "h323gatestat", "h323hostcall", "tftp-mcast", "hello", "radius", "radius-acct", "MSN-Messenger", "mtp", "hsrp", "licensedaemon", "gdp-port", "cfinger", "nfs", "knetd", "zephyr-srv", "zephyr-clt", "zephyr-hm", "eklogin", "ninstall", "cvspserver", "venus", "venus-se", "codasrv", "codasrv-se", "hpstgmgr", "discp-client", "discp-server", "servicemeter", "nsc-ccs", "nsc-posa", "netmon", "corbaloc", "afbackup", "squid", "icpv2", "mysql", "trnsprntproxy", "prsvp", "rwhois", "krb524", "fax", "hylafax", "rfe", "sgi-dgl", "cfengine", "noclog", "hostmon", "postgres", "canna", "cvsup", "x11-ssh-offset", "ircd", "xfs", "tircproxy", "http-alt", "webcache", "tproxy", "jetdirect", "x11", "afs3-fileserver", "afs3-callback", "afs3-prserver", "afs3-vlserver", "afs3-kaserver", "afs3-volser", "afs3-errors", "afs3-bos", "afs3-update", "afs3-rmtsys", "DynSite", "Dynsite", "sd", "amanda", "kamanda", "amandaidx", "amidxtape", "pgpkeyserver", "h323callsigalt", "bprd", "bpdbm", "bpjava-msvc", "vnetd", "bpcd", "vopied", "wnn6", "isdnlog", "vboxd", "wnn4_Kr", "wnn4_Cn", "wnn4_Tw", "binkp", "quake", "wnn6-ds", "asp", "traceroute", "tfido", "fido" };

    /** Property to enable/disable session output */
    protected static String PROP_SESSION_OUTPUT = "OutputType.TCPSession";

    /** Property to specify the initial size of the packet work queue */
    protected static String PROP_WORKQ_SIZE = "SessionProcessor.WorkQSize";

    /** Property to specify the initial size of the active session hash table */
    protected static String PROP_SESHASH_SIZE = "SessionProcessor.SesHashSize";

    /** Property to specify the initial size of the inflight packet flow hashtable */
    protected static String PROP_DBSIZE = "SessionProcessor.PacketDBSize";

    /** log4j */
    protected static Logger log = Logger.getLogger(TCPSessionProcessor.class);

    /**
	 * Create a new TCP Session processor, internally controlled Thread
	 * 
	 */
    public TCPSessionProcessor() {
        log.debug(">>Constructor()");
        writeSession = new Boolean(props.getProperty(PROP_SESSION_OUTPUT));
        init(true, writeSession.booleanValue());
    }

    /**
	 * Create a new TCP Session processor, stub for a subclass to manage the initialization of the thread
	 * 
	 * @param startThread unused 
	 */
    public TCPSessionProcessor(boolean startThread) {
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
            sesTimer = new TCPSessionTimer(this);
            sesTimer.start();
            log.debug("Start Thread = " + Boolean.valueOf(startThread));
            if (startThread) this.start();
        }
    }

    /**
	 * Add a packet to the current session's packet summary DB
	 * 
	 * 
	 * @param bean The TCPBean to add
	 */
    public void insertBeanDB(TCPBean bean) {
        TCPSumBean s;
        if (!writeSession.booleanValue()) return;
        synchronized (packetDB) {
            if (packetDB.containsKey(bean.skey.toString())) {
                Vector rows = (Vector) packetDB.get(bean.skey.toString());
                if (bean.ipbean.dstAddrBin.compareTo(bean.ipbean.srcAddrBin) > 0) {
                    s = (TCPSumBean) rows.elementAt(0);
                    s.addPacket(bean);
                } else {
                    s = (TCPSumBean) rows.elementAt(1);
                    s.addPacket(bean);
                }
            } else {
                Vector rows = new Vector();
                if (bean.ipbean.dstAddrBin.compareTo(bean.ipbean.srcAddrBin) > 0) {
                    s = new TCPSumBean(bean);
                    rows.add(0, s);
                    rows.add(1, new TCPSumBean());
                } else {
                    s = new TCPSumBean(bean);
                    rows.add(0, new TCPSumBean());
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
        SessionBean tcpsbean = new SessionBean();
        tcpsbean.flow1 = (PacketSumBean) workItem.get(0);
        tcpsbean.flow2 = (PacketSumBean) workItem.get(1);
        boolean emptyFlows = false;
        if ((!tcpsbean.flow1.empty) && (!tcpsbean.flow2.empty)) {
            if (tcpsbean.flow1.ipsbean.firstPacketTime.after(tcpsbean.flow2.ipsbean.firstPacketTime)) {
                tcpsbean.flow1 = (PacketSumBean) workItem.get(1);
                tcpsbean.flow2 = (PacketSumBean) workItem.get(0);
            }
        } else if (!tcpsbean.flow1.empty) {
            tcpsbean.flow1 = (PacketSumBean) workItem.get(0);
            tcpsbean.flow2 = (PacketSumBean) workItem.get(1);
        } else if (!tcpsbean.flow2.empty) {
            tcpsbean.flow2 = (PacketSumBean) workItem.get(0);
            tcpsbean.flow1 = (PacketSumBean) workItem.get(1);
        } else {
            emptyFlows = true;
            log.fatal("both flows are empty");
            System.exit(-1);
        }
        Short tmp = (Short) ((TCPSumBean) tcpsbean.flow1).tcpFlags.get(0);
        if ((tmp.shortValue() & 0x0002) == 0x0002) {
            tcpsbean.clientAddr = tcpsbean.flow1.ipsbean.srcAddr;
            tcpsbean.clientPort = tcpsbean.flow1.ipsbean.srcPort;
            tcpsbean.serverAddr = tcpsbean.flow1.ipsbean.dstAddr;
            tcpsbean.serverPort = tcpsbean.flow1.ipsbean.dstPort;
            tcpsbean.startTime = tcpsbean.flow1.ipsbean.firstPacketTime;
            tcpsbean.skey = tcpsbean.flow1.sKey;
            int i = Arrays.binarySearch(svcnums, tcpsbean.serverPort.intValue());
            if (i >= 0) tcpsbean.svcName = svcnams[i]; else tcpsbean.svcName = "unknown";
        } else if ((tmp.shortValue() & (short) 0x0012) == (short) 0x0012) {
            log.debug("missed first syn, caught ack : " + tmp.toString() + " : " + tcpsbean.flow1.sKey);
            tcpsbean.clientAddr = tcpsbean.flow1.ipsbean.dstAddr;
            tcpsbean.clientPort = tcpsbean.flow1.ipsbean.dstPort;
            tcpsbean.serverAddr = tcpsbean.flow1.ipsbean.srcAddr;
            tcpsbean.serverPort = tcpsbean.flow1.ipsbean.srcPort;
            tcpsbean.startTime = tcpsbean.flow1.ipsbean.firstPacketTime;
            tcpsbean.skey = tcpsbean.skey;
            int i = Arrays.binarySearch(svcnums, tcpsbean.serverPort.intValue());
            if (i >= 0) tcpsbean.svcName = svcnams[i]; else tcpsbean.svcName = "unknown";
        } else if (Arrays.binarySearch(svcnums, tcpsbean.flow1.ipsbean.dstPort.intValue()) >= 0) {
            log.debug("unknow client/server, found svc: " + tmp.toString() + " : " + tcpsbean.flow1.sKey);
            tcpsbean.clientAddr = tcpsbean.flow1.ipsbean.srcAddr;
            tcpsbean.clientPort = tcpsbean.flow1.ipsbean.srcPort;
            tcpsbean.serverAddr = tcpsbean.flow1.ipsbean.dstAddr;
            tcpsbean.serverPort = tcpsbean.flow1.ipsbean.dstPort;
            tcpsbean.startTime = tcpsbean.flow1.ipsbean.firstPacketTime;
            tcpsbean.skey = tcpsbean.flow1.sKey;
            int i = Arrays.binarySearch(svcnums, tcpsbean.serverPort.intValue());
            if (i >= 0) tcpsbean.svcName = svcnams[i]; else tcpsbean.svcName = "unknown";
        } else {
            log.debug("unknow client/server, making assumption: " + tmp.toString() + " : " + tcpsbean.flow1.sKey);
            tcpsbean.clientAddr = tcpsbean.flow1.ipsbean.dstAddr;
            tcpsbean.clientPort = tcpsbean.flow1.ipsbean.dstPort;
            tcpsbean.serverAddr = tcpsbean.flow1.ipsbean.srcAddr;
            tcpsbean.serverPort = tcpsbean.flow1.ipsbean.srcPort;
            tcpsbean.startTime = tcpsbean.flow1.ipsbean.firstPacketTime;
            tcpsbean.skey = tcpsbean.flow1.sKey;
            int i = Arrays.binarySearch(svcnums, tcpsbean.serverPort.intValue());
            if (i >= 0) tcpsbean.svcName = svcnams[i]; else tcpsbean.svcName = "unknown";
        }
        tcpsbean.dataSent = tcpsbean.flow1.ipsbean.totalPktLen;
        tcpsbean.packetsSent = tcpsbean.flow1.ipsbean.numPackets;
        tcpsbean.dataRecv = tcpsbean.flow2.ipsbean.totalPktLen;
        tcpsbean.packetsRecv = tcpsbean.flow2.ipsbean.numPackets;
        if (((TCPSumBean) tcpsbean.flow1).hasTCPFIN || ((TCPSumBean) tcpsbean.flow2).hasTCPFIN) tcpsbean.status = new String("closed"); else tcpsbean.status = new String("open");
        if (tcpsbean.flow1.ipsbean.lastPacketTime.after(tcpsbean.flow2.ipsbean.lastPacketTime)) {
            tcpsbean.duration = Long.valueOf(tcpsbean.flow1.ipsbean.lastPacketTime.getTime() - tcpsbean.startTime.getTime());
        } else {
            tcpsbean.duration = Long.valueOf(tcpsbean.flow2.ipsbean.lastPacketTime.getTime() - tcpsbean.startTime.getTime());
        }
        log.debug("flushing: " + tcpsbean.skey + " " + tcpsbean);
        flush(tcpsbean);
    }

    /**
	 * Add a packet to be process into a session
	 * 
	 * 
	 * @param tcpbean The TCPBean to be processed 
	 */
    public void addPacket(TCPBean tcpbean) {
        if (!writeSession.booleanValue()) return;
        updatePacketClock(tcpbean.ipbean.timeStamp.getTime());
        insertBeanDB((TCPBean) tcpbean);
        synchronized (sessions) {
            if (sessions.containsKey(tcpbean.skey.toString())) {
                SessionHash shb = (SessionHash) sessions.get(tcpbean.skey.toString());
                shb.tickOfLastPacket = 0;
                shb.packetTimeOfLastPacket = currentPacketTime;
                if (shb.closeFINS > 0) {
                    shb.packetTimeAtFin = currentPacketTime;
                }
                if (tcpbean.tcpflgFIN.booleanValue()) {
                    shb.closeFINS++;
                    shb.packetTimeAtFin = currentPacketTime;
                } else if (shb.closeFINS > 0 && tcpbean.tcpflgACK.booleanValue()) {
                    shb.closeFINACK++;
                    shb.packetTimeAtFin = currentPacketTime;
                }
                if (tcpbean.tcpflgRST.booleanValue()) {
                    shb.resetFLG++;
                }
            } else {
                log.debug("adding bean to sessions hash - obj: " + tcpbean + " key: " + tcpbean.skey);
                SessionHash shb = new SessionHash(tcpbean.skey.toString());
                shb.tickOfLastPacket = 0;
                shb.packetTimeOfLastPacket = currentPacketTime;
                if (shb.closeFINS > 0) {
                    shb.packetTimeAtFin = currentPacketTime;
                }
                if (tcpbean.tcpflgFIN.booleanValue()) {
                    shb.closeFINS++;
                    shb.packetTimeAtFin = currentPacketTime;
                } else if (shb.closeFINS > 0 && tcpbean.tcpflgACK.booleanValue()) {
                    shb.closeFINACK++;
                }
                if (tcpbean.tcpflgRST.booleanValue()) {
                    shb.resetFLG++;
                }
                sessions.put(shb.sessionKey, shb);
            }
        }
    }

    /**
	 * Flush/write out a completed TCP Session
	 * 
	 * 
	 * @param tcpsbean The TCPSession bean to flush
	 */
    protected void flush(SessionBean tcpsbean) {
        if (!writeSession.booleanValue()) return;
        try {
            ((PacketOutputAdaptorIF) dbh).writeTCPSession(tcpsbean);
        } catch (Exception e) {
            log.error(e, e);
        }
    }
}
