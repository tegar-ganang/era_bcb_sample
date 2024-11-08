package com.mepping.snmpjaag;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.Vector;
import org.opennms.protocols.snmp.SnmpAgentHandler;
import org.opennms.protocols.snmp.SnmpAgentSession;
import org.opennms.protocols.snmp.SnmpEndOfMibView;
import org.opennms.protocols.snmp.SnmpInt32;
import org.opennms.protocols.snmp.SnmpObjectId;
import org.opennms.protocols.snmp.SnmpOctetString;
import org.opennms.protocols.snmp.SnmpParameters;
import org.opennms.protocols.snmp.SnmpPduBulk;
import org.opennms.protocols.snmp.SnmpPduEncodingException;
import org.opennms.protocols.snmp.SnmpPduPacket;
import org.opennms.protocols.snmp.SnmpPduRequest;
import org.opennms.protocols.snmp.SnmpPduTrap;
import org.opennms.protocols.snmp.SnmpPeer;
import org.opennms.protocols.snmp.SnmpSMI;
import org.opennms.protocols.snmp.SnmpSyntax;
import org.opennms.protocols.snmp.SnmpTrapHandler;
import org.opennms.protocols.snmp.SnmpTrapSession;
import org.opennms.protocols.snmp.SnmpVarBind;
import org.opennms.protocols.snmp.asn1.AsnEncodingException;
import com.mepping.snmpjaag.admin.AdminFileDelete;
import com.mepping.snmpjaag.admin.AdminFileDownload;
import com.mepping.snmpjaag.admin.AdminFileUpload;
import com.mepping.snmpjaag.admin.AdminInit;
import com.mepping.snmpjaag.admin.AdminObject;
import com.mepping.snmpjaag.bootstrap.Bootstrap;
import com.mepping.snmpjaag.bootstrap.SnmpjaagConfig;
import com.mepping.snmpjaag.nrpe.NrpeConfig;
import com.mepping.snmpjaag.nrpe.PluginResult;

public class SnmpJavaAgent implements SnmpAgentHandler, SnmpTrapHandler {

    private static final boolean DEBUG = true;

    private Vector cmdThreads;

    private SnmpAgentSession agentSession;

    private SnmpTrapSession trapSession;

    private SortedMap mib;

    private SortedMap staticMib;

    private ChangeAwareHashMap results;

    private MgmtInfoBase mgmtInfoBase;

    private boolean stopped;

    public SnmpJavaAgent() throws UnknownHostException, SocketException {
        super();
        cmdThreads = new Vector();
        results = new ChangeAwareHashMap();
        mgmtInfoBase = new MgmtInfoBase();
        stopped = false;
        mib = new SnmpTreeMap();
        staticMib = new SnmpTreeMap();
        agentSession = null;
        trapSession = null;
    }

    public void SnmpAgentSessionError(SnmpAgentSession session, int error, Object ref) {
        new Exception().printStackTrace();
        System.exit(-1);
    }

    public void snmpReceivedPdu(SnmpAgentSession session, InetAddress manager, int port, SnmpOctetString community, SnmpPduPacket pdu) {
        if (DEBUG) {
            System.out.println("snmpReceivedPdu(" + manager + ", " + port + ", " + community + ", " + pdu.getClass().getName() + ")");
        }
        if (pdu instanceof SnmpPduBulk) {
            SnmpPduRequest response = new SnmpPduRequest(SnmpPduRequest.RESPONSE);
            response.setRequestId(pdu.getRequestId());
            doBulk(pdu.toVarBindArray(), pdu, response);
            try {
                session.send(new SnmpPeer(manager, port), response);
            } catch (AsnEncodingException ex) {
                try {
                    response = new SnmpPduRequest(SnmpPduRequest.RESPONSE);
                    response.setRequestId(pdu.getRequestId());
                    response.setErrorStatus(SnmpPduRequest.ErrTooBig);
                    response.setErrorIndex(0);
                    session.send(new SnmpPeer(manager, port), response);
                } catch (Exception exp) {
                    exp.printStackTrace();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public SnmpPduRequest snmpReceivedGet(SnmpPduPacket pdu, boolean getNext) {
        if (DEBUG) {
            System.out.println("snmpReceivedGet(" + pdu.getClass().getName() + ", " + getNext + ")");
        }
        SnmpVarBind[] binds = pdu.toVarBindArray();
        SnmpPduRequest response = new SnmpPduRequest(SnmpPduRequest.RESPONSE);
        response.setRequestId(pdu.getRequestId());
        if (binds.length > 20) {
            if (DEBUG) {
                System.out.println("snmpReceivedGet: responding with too big -> " + pdu.getRequestId() + ", " + getOidList(binds));
            }
            response.setErrorStatus(SnmpPduRequest.ErrTooBig);
            response.setErrorIndex(0);
            return response;
        }
        if (pdu instanceof SnmpPduRequest) {
            int errorIndex = 0;
            if (getNext) {
                if (DEBUG) {
                    System.out.println("snmpReceivedGet: getNext --> " + pdu.getRequestId() + ", " + getOidList(binds));
                }
                for (int i = 0; i < binds.length; i++) {
                    String oid = binds[i].getName().toString();
                    SortedMap map = mib.tailMap(oid + ".0");
                    if (!map.isEmpty()) {
                        oid = (String) map.firstKey();
                        response.addVarBind(new SnmpVarBind(oid, (SnmpSyntax) map.get(oid)));
                    } else {
                        response.addVarBind(new SnmpVarBind(oid));
                        errorIndex = i + 1;
                        break;
                    }
                }
            } else {
                if (DEBUG) {
                    System.out.println("snmpReceivedGet: get --> " + pdu.getRequestId() + ", " + getOidList(binds));
                }
                for (int i = 0; i < binds.length; i++) {
                    String oid = binds[i].getName().toString();
                    SnmpSyntax value = (SnmpSyntax) mib.get(oid);
                    if (value != null) {
                        response.addVarBind(new SnmpVarBind(oid, value));
                    } else {
                        response.addVarBind(new SnmpVarBind(oid));
                        errorIndex = i + 1;
                        break;
                    }
                }
            }
            if (errorIndex > 0) {
                if (DEBUG) {
                    System.out.println("snmpReceivedGet: get/getNext --> " + pdu.getRequestId() + " index " + errorIndex + " not found");
                }
                response.setErrorStatus(SnmpPduRequest.ErrNoSuchName);
                response.setErrorIndex(errorIndex);
            }
        } else if (pdu instanceof SnmpPduBulk) {
            doBulk(binds, pdu, response);
        } else {
            if (DEBUG) {
                System.out.println("snmpReceivedGet: unknown command");
            }
            response.setErrorStatus(SnmpPduRequest.ErrGenError);
            response.setErrorIndex(1);
        }
        return response;
    }

    public SnmpPduRequest snmpReceivedSet(SnmpPduPacket pdu) {
        if (DEBUG) {
            System.out.println("snmpReceivedSet(" + pdu.getClass().getName() + ")");
        }
        SnmpjaagConfig snmpjaagConfig = SnmpjaagConfig.getSnmpjaagConfig();
        boolean forbidden = true;
        InetAddress writer = pdu.getPeer().getPeer();
        if (DEBUG) {
            System.out.println("snmpReceivedSet: remote host -> " + writer);
        }
        InetAddress[] allowedWriters = snmpjaagConfig.getAllowedWriters();
        for (int i = 0; i < allowedWriters.length; i++) {
            if (writer.getHostAddress().equals(allowedWriters[i].getHostAddress())) {
                forbidden = false;
                break;
            }
        }
        SnmpVarBind[] binds = pdu.toVarBindArray();
        SnmpPduRequest response = new SnmpPduRequest(SnmpPduRequest.RESPONSE);
        int errorIndex = 0;
        response.setRequestId(pdu.getRequestId());
        if (forbidden) {
            if (DEBUG) {
                System.out.println("snmpReceivedSet: remote host not allowed -> " + pdu.getRequestId() + ", " + getOidList(binds));
            }
            response.setErrorStatus(SnmpPduRequest.ErrGenError);
            response.setErrorIndex(0);
            return response;
        }
        if (binds.length > 20) {
            if (DEBUG) {
                System.out.println("snmpReceivedSet: responding with too big -> " + pdu.getRequestId() + ", " + getOidList(binds));
            }
            response.setErrorStatus(SnmpPduRequest.ErrTooBig);
            response.setErrorIndex(0);
            return response;
        }
        for (int i = 0; i < binds.length; i++) {
            String oid = binds[i].getName().toString();
            SnmpSyntax newValue = binds[i].getValue();
            SnmpSyntax value = (SnmpSyntax) mib.get(oid);
            if (DEBUG) {
                System.out.println("snmpReceivedSet: oid value is -> " + oid + ", " + binds[i].getName());
                System.out.println("snmpReceivedSet: old value is -> " + value);
                System.out.println("snmpReceivedSet: new value is -> " + newValue);
            }
            if (value != null) {
                if (value instanceof AdminObject) {
                    if (value instanceof AdminInit) {
                        AdminInit adminInit = (AdminInit) value;
                        if (newValue instanceof SnmpInt32) {
                            SnmpInt32 operation = (SnmpInt32) newValue;
                            adminInit.setSnmpJavaAgent(this);
                            try {
                                adminInit.setValue(operation.getValue());
                                response.addVarBind(new SnmpVarBind(oid, newValue));
                                continue;
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (value instanceof AdminFileDownload) {
                        AdminFileDownload adminFileDownload = (AdminFileDownload) value;
                        if (newValue instanceof SnmpOctetString) {
                            SnmpOctetString octets = (SnmpOctetString) newValue;
                            adminFileDownload.setSnmpJavaAgent(this);
                            try {
                                adminFileDownload.setString(octets.toString());
                                response.addVarBind(new SnmpVarBind(oid, newValue));
                                continue;
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                        response.setErrorStatus(SnmpPduRequest.ErrBadValue);
                        response.setErrorIndex(i);
                        return response;
                    } else if (value instanceof AdminFileUpload) {
                        AdminFileUpload adminFileUpload = (AdminFileUpload) value;
                        if (newValue instanceof SnmpOctetString) {
                            SnmpOctetString octets = (SnmpOctetString) newValue;
                            adminFileUpload.setSnmpJavaAgent(this);
                            try {
                                adminFileUpload.setString(octets.toString());
                                response.addVarBind(new SnmpVarBind(oid, newValue));
                                continue;
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                        response.setErrorStatus(SnmpPduRequest.ErrBadValue);
                        response.setErrorIndex(i);
                        return response;
                    } else if (value instanceof AdminFileDelete) {
                        AdminFileDelete adminFileDelete = (AdminFileDelete) value;
                        if (newValue instanceof SnmpOctetString) {
                            SnmpOctetString octets = (SnmpOctetString) newValue;
                            adminFileDelete.setSnmpJavaAgent(this);
                            try {
                                adminFileDelete.setString(octets.toString());
                                response.addVarBind(new SnmpVarBind(oid, newValue));
                                continue;
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                        response.setErrorStatus(SnmpPduRequest.ErrBadValue);
                        response.setErrorIndex(i);
                        return response;
                    }
                } else {
                    mib.put(oid, newValue);
                    response.addVarBind(new SnmpVarBind(oid, newValue));
                    continue;
                }
            }
            response.addVarBind(new SnmpVarBind(oid));
            errorIndex = i + 1;
            break;
        }
        if (errorIndex > 0) {
            if (DEBUG) {
                System.out.println("snmpReceivedSet: " + pdu.getRequestId() + " index " + errorIndex + " not found");
            }
            response.setErrorStatus(SnmpPduRequest.ErrNoSuchName);
            response.setErrorIndex(errorIndex);
        }
        return response;
    }

    public void snmpReceivedTrap(SnmpTrapSession session, InetAddress agent, int port, SnmpOctetString community, SnmpPduPacket pdu) {
    }

    public void snmpReceivedTrap(SnmpTrapSession session, InetAddress agent, int port, SnmpOctetString community, SnmpPduTrap pdu) {
    }

    public void snmpTrapSessionError(SnmpTrapSession session, int error, Object ref) {
    }

    /**
     * 
     */
    private static List getOidList(SnmpVarBind[] binds) {
        List list = new ArrayList(binds.length);
        for (int i = 0; i < binds.length; i++) {
            list.add(binds[i].getName());
        }
        return list;
    }

    /**
     * 
     */
    private void doBulk(SnmpVarBind binds[], SnmpPduPacket pdu, SnmpPduRequest response) {
        if (DEBUG) {
            System.out.println("doBulk(" + getOidList(binds) + ", " + pdu.getRequestId() + ")");
        }
        SnmpPduBulk bulk = (SnmpPduBulk) pdu;
        int nonRepeaters = bulk.getNonRepeaters();
        int maxRep = bulk.getMaxRepititions();
        for (int i = 0; i < Math.min(nonRepeaters, binds.length); i++) {
            String oid = binds[i].getName().toString();
            SortedMap map = mib.tailMap(oid + ".0");
            if (!map.isEmpty()) {
                oid = (String) map.firstKey();
                response.addVarBind(new SnmpVarBind(oid, (SnmpSyntax) map.get(oid)));
            } else {
                response.addVarBind(new SnmpVarBind(oid, new SnmpEndOfMibView()));
            }
        }
        for (int i = nonRepeaters; i < binds.length; i++) {
            String oid = binds[i].getName().toString();
            SortedMap map = mib.tailMap(oid + ".0");
            if (!map.isEmpty()) {
                Iterator it = map.keySet().iterator();
                for (int j = 0; j < maxRep; j++) {
                    if (!it.hasNext()) {
                        response.addVarBind(new SnmpVarBind(".1.9", new SnmpEndOfMibView()));
                        break;
                    }
                    oid = (String) it.next();
                    response.addVarBind(new SnmpVarBind(oid, (SnmpSyntax) map.get(oid)));
                }
            } else {
                response.addVarBind(new SnmpVarBind(oid, new SnmpEndOfMibView()));
            }
        }
    }

    public void sendTrap(String oid) {
        SnmpjaagConfig snmpjaagConfig = SnmpjaagConfig.getSnmpjaagConfig();
        Object[] trapReceivers = snmpjaagConfig.getNagiosTrapReceivers();
        for (int i = 0; i < trapReceivers.length; i++) {
            String[] receiver = (String[]) trapReceivers[i];
            try {
                sendTrap(InetAddress.getByName(receiver[0]), Integer.parseInt(receiver[1]), oid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendTrap(InetAddress address, int port, String oid) throws SnmpPduEncodingException, AsnEncodingException, IOException {
        SnmpjaagConfig snmpjaagConfig = SnmpjaagConfig.getSnmpjaagConfig();
        SnmpPeer peer = new SnmpPeer(address, port);
        SnmpParameters params = peer.getParameters();
        params.setVersion(SnmpSMI.SNMPV2);
        params.setReadCommunity(snmpjaagConfig.getProperty("configReadCommunity"));
        params.setWriteCommunity(snmpjaagConfig.getProperty("configWriteCommunity"));
        peer.setParameters(params);
        SnmpPduRequest trapPdu = new SnmpPduRequest(SnmpPduPacket.V2TRAP);
        trapPdu.addVarBind(new SnmpVarBind(new SnmpObjectId(".1.3.6.1.2.1.1.3.0"), MgmtInfoBase.sysUpTime));
        trapPdu.addVarBind(new SnmpVarBind(new SnmpObjectId(".1.3.6.1.6.3.1.1.4.1.0"), new SnmpObjectId(oid)));
        synchronized (trapSession) {
            trapSession.send(peer, trapPdu);
        }
    }

    public void run() throws IOException {
        reload();
        rebuildStaticMib();
        mib = new SnmpTreeMap();
        mib.putAll(staticMib);
        startupSnmp();
        System.out.println(Bootstrap.UP_AND_RUNNING);
        startCommandThreads();
        sendTrap(MgmtInfoBase.coldStart);
        while (!stopped) {
            if (results.isUpdated()) {
                if (DEBUG) {
                    System.out.println("Rebuilding MIB...");
                }
                results.setUpdated(false);
                SortedMap dynamicMib = new SnmpTreeMap();
                mgmtInfoBase.putSnmpjaagNagios(dynamicMib, results);
                SortedMap mib = new SnmpTreeMap();
                mib.putAll(staticMib);
                mib.putAll(dynamicMib);
                this.mib = mib;
                if (DEBUG) {
                    System.out.println("" + mib);
                }
            }
            try {
                synchronized (results) {
                    results.wait(500L);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        stopCommandThreads();
        shutdownSnmp();
    }

    public void rebuildStaticMib() throws UnknownHostException {
        if (DEBUG) {
            System.out.println("rebuildStaticMib");
        }
        SortedMap staticMib = new SnmpTreeMap();
        mgmtInfoBase.putSystemMib(staticMib);
        mgmtInfoBase.putSnmpjaagInfoMib(staticMib);
        mgmtInfoBase.putSnmpjaagConfigMib(staticMib);
        mgmtInfoBase.putSnmpjaagAdminMib(staticMib);
        this.staticMib = staticMib;
    }

    public void startCommandThreads() {
        if (DEBUG) {
            System.out.println("startCommandThreads");
        }
        SnmpjaagConfig snmpjaagCfg = SnmpjaagConfig.getSnmpjaagConfig();
        NrpeConfig nrpeCfg = NrpeConfig.getNrpeConfig();
        long frequency = Integer.parseInt(snmpjaagCfg.getProperty("configNagiosCheckFrequency")) * 1000L;
        Vector commands = nrpeCfg.getCommands();
        Enumeration checks = commands.elements();
        for (int index = 1; checks.hasMoreElements(); index++) {
            NameValuePair check = (NameValuePair) checks.nextElement();
            results.put(check.getName(), new PluginResult());
            String cmdline = (nrpeCfg.getCommandPrefix() + " " + check.getValue()).trim();
            CommandThread thread = new CommandThread(check.getName(), cmdline, frequency, results);
            thread.start();
            cmdThreads.addElement(thread);
        }
    }

    public void stopCommandThreads() {
        if (DEBUG) {
            System.out.println("stopCommandThreads");
        }
        Enumeration threads = cmdThreads.elements();
        while (threads.hasMoreElements()) {
            CommandThread thread = (CommandThread) threads.nextElement();
            thread.kill();
        }
        cmdThreads.clear();
    }

    public void reload() throws IOException {
        if (DEBUG) {
            System.out.println("reload");
        }
        SnmpjaagConfig snmpjaagConfig = SnmpjaagConfig.getSnmpjaagConfig();
        snmpjaagConfig.load(new File(Bootstrap.SNMPJAAG_HOME, "snmpjaag.cfg").getAbsolutePath());
        NrpeConfig nrpeConfig = NrpeConfig.getNrpeConfig();
        nrpeConfig.load(snmpjaagConfig.getProperty("configNrpeCfgFile"));
    }

    public void startupSnmp() throws SocketException, UnknownHostException {
        if (DEBUG) {
            System.out.println("startupSnmp");
        }
        SnmpjaagConfig snmpjaagConfig = SnmpjaagConfig.getSnmpjaagConfig();
        startupSnmp(InetAddress.getByName(snmpjaagConfig.getLocalAddress()), snmpjaagConfig.getLocalPort(), snmpjaagConfig.getLocalTrapPort(), snmpjaagConfig.getProperty("configReadCommunity"), snmpjaagConfig.getProperty("configWriteCommunity"));
    }

    public void startupSnmp(InetAddress address, int port, int trapPort, String readCommunity, String writeCommunity) throws SocketException {
        if (DEBUG) {
            System.out.println("startupSnmp(" + address + "," + port + "," + readCommunity + "," + writeCommunity + ")");
        }
        SnmpPeer peer = new SnmpPeer(address, port);
        SnmpParameters params = peer.getParameters();
        params.setVersion(SnmpSMI.SNMPV2);
        params.setReadCommunity(readCommunity);
        params.setWriteCommunity(writeCommunity);
        peer.setParameters(params);
        agentSession = new SnmpAgentSession(this, peer);
        trapSession = new SnmpTrapSession(this, trapPort);
    }

    public void shutdownSnmp() {
        if (DEBUG) {
            System.out.println("shutdownSnmp()");
        }
        agentSession.close();
        trapSession.close();
    }

    public static void main(String args[]) {
        try {
            new SnmpJavaAgent().run();
            System.exit(0);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
