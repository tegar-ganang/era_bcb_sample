package com.mepping.snmpjaag.bootstrap;

import java.io.File;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Vector;
import org.opennms.protocols.snmp.SnmpHandler;
import org.opennms.protocols.snmp.SnmpOctetString;
import org.opennms.protocols.snmp.SnmpParameters;
import org.opennms.protocols.snmp.SnmpPduPacket;
import org.opennms.protocols.snmp.SnmpPduRequest;
import org.opennms.protocols.snmp.SnmpPeer;
import org.opennms.protocols.snmp.SnmpSMI;
import org.opennms.protocols.snmp.SnmpSession;
import org.opennms.protocols.snmp.SnmpSyntax;
import org.opennms.protocols.snmp.SnmpVarBind;

/**
 * Simple SNMP client
 * 
 * @author roberto
 */
public class SnmpClient {

    private String readCommunity;

    private String writeCommunity;

    private int timeout;

    private int retries;

    public SnmpClient(String readCommunity, String writeCommunity, int timeout, int retries) {
        super();
        this.readCommunity = readCommunity;
        this.writeCommunity = writeCommunity;
        this.timeout = timeout;
        this.retries = retries;
    }

    public String getReadCommunity() {
        return readCommunity;
    }

    public void setReadCommunity(String readCommunity) {
        this.readCommunity = readCommunity;
    }

    public String getWriteCommunity() {
        return writeCommunity;
    }

    public void setWriteCommunity(String writeCommunity) {
        this.writeCommunity = writeCommunity;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    /**
	 * SNMP get
	 * 
	 * @param hostname
	 * @param port
	 * @param oid
	 * @return fetched value as string or null if unable to complete operation
	 * @throws SocketException 
	 */
    public String get(InetAddress address, int port, String oid) throws SocketException {
        System.out.println("get(" + address + ", " + port + ", " + oid + ")");
        System.out.println("get: " + readCommunity + ", " + writeCommunity + ", " + timeout + ", " + retries);
        SnmpPeer peer = new SnmpPeer(address, port);
        peer.setTimeout(timeout);
        peer.setRetries(retries);
        SnmpParameters params = peer.getParameters();
        params.setVersion(SnmpSMI.SNMPV2);
        params.setReadCommunity(readCommunity);
        params.setWriteCommunity(writeCommunity);
        peer.setParameters(params);
        final Vector snmpVarBinds = new Vector();
        final SnmpSession session = new SnmpSession(peer);
        session.setDefaultHandler(new SnmpHandler() {

            public void snmpTimeoutError(SnmpSession session, SnmpSyntax pdu) {
                System.out.println(session.getPeer().getPeer());
                System.out.println(pdu.getClass().getName());
                System.out.println("SnmpTimeout");
                synchronized (session) {
                    session.notify();
                }
            }

            public void snmpInternalError(SnmpSession session, int err, SnmpSyntax pdu) {
                System.out.println("InternalError");
                synchronized (session) {
                    session.notify();
                }
            }

            public void snmpReceivedPdu(SnmpSession session, int command, SnmpPduPacket pdu) {
                for (int i = 0; i < pdu.getLength(); i++) {
                    SnmpVarBind varBind = pdu.getVarBindAt(i);
                    System.out.println("Received value: " + varBind.getName() + "=" + varBind.getValue());
                    snmpVarBinds.addElement(varBind);
                }
                synchronized (session) {
                    session.notify();
                }
            }
        });
        SnmpVarBind[] vblist = { new SnmpVarBind(oid) };
        SnmpPduRequest pdu = new SnmpPduRequest(SnmpPduPacket.GET, vblist);
        pdu.setRequestId(1);
        try {
            synchronized (session) {
                session.send(pdu);
                session.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String result = null;
        if (snmpVarBinds.size() > 0) {
            SnmpVarBind varBind = (SnmpVarBind) snmpVarBinds.get(0);
            SnmpSyntax value = varBind.getValue();
            if (value instanceof SnmpOctetString) {
                SnmpOctetString string = (SnmpOctetString) value;
                result = string.toString();
            }
        }
        try {
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        SnmpjaagConfig snmpjaagConfig = SnmpjaagConfig.getSnmpjaagConfig();
        snmpjaagConfig.load(new File(Bootstrap.SNMPJAAG_HOME, "snmpjaag.cfg").getAbsolutePath());
        SnmpClient snmp = new SnmpClient(snmpjaagConfig.getProperty("configReadCommunity"), snmpjaagConfig.getProperty("configWriteCommunity"), 3000, 5);
        String sysDescr = snmp.get(InetAddress.getByName(snmpjaagConfig.getLocalAddress()), snmpjaagConfig.getLocalPort(), "1.3.6.1.2.1.1.1.0");
        System.out.println(sysDescr);
    }
}
