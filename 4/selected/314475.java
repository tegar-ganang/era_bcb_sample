package org.psepr.PsEPRServer.StatusReporter;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import org.psepr.PsEPRServer.ConnectionManager;
import org.psepr.PsEPRServer.ConnectionReader;
import org.psepr.PsEPRServer.Counter;
import org.psepr.PsEPRServer.DebugLogger;
import org.psepr.PsEPRServer.Global;
import org.psepr.PsEPRServer.ParamServer;
import org.psepr.PsEPRServer.Processor;
import org.psepr.PsEPRServer.ServerEvent;
import org.psepr.PsEPRServer.ServerEventChannel;
import org.psepr.PsEPRServer.Utilities;
import org.psepr.services.service.ChannelUseDescription;
import org.psepr.services.service.ChannelUseDescriptionCollection;
import org.psepr.services.service.EventDescription;
import org.psepr.services.service.EventDescriptionCollection;

/**
 * This collects data about the operation of the system and periodically
 * creates a heartbeat message with piles of interesting facts.
 * It is started as a connection processor -- it accepts messages so it
 * can account them and it generates status messages.
 * 
 * @author Robert.Adams@intel.com
 */
public class StatusReporter extends Processor {

    private DebugLogger log;

    private HashMap<String, Counter> messagesByUser;

    public static final String HEARTBEAT_NAMESPACE = "http://dsmt.org/schema/psepr/payload/PsEPRServer/heartbeat-1.0";

    Counter totalMessages;

    private ArrayList<String> retiredStatus;

    /**
	 * 
	 */
    public StatusReporter() {
        super();
        this.init();
    }

    private void init() {
        log = new DebugLogger("StatusReporter");
        Global.statusReporter = this;
        retiredStatus = new ArrayList<String>();
        totalMessages = new Counter();
        messagesByUser = new HashMap<String, Counter>();
        if (Global.serviceStatus != null) {
            ChannelUseDescriptionCollection cudc = Global.serviceStatus.getServiceDescription().getChannelUses();
            cudc.add(new ChannelUseDescription(Global.params().getParamString(ParamServer.STATUS_REPORTER_CHANNEL), "detailed status of server operation", 12L, 1200L, new EventDescriptionCollection(new EventDescription(HEARTBEAT_NAMESPACE, "detailed status of server operation")), null));
        }
    }

    public void start() {
        Global.threads.execute(new Runnable() {

            public void run() {
                Thread.currentThread().setName("StatusReporter");
                ReportStatus();
            }
        });
        Global.routeTable.addRoute("StatusReporter", "/", null, this);
    }

    public void stop() {
        return;
    }

    /**
	 * If asked, I'm just a server service.
	 */
    public int getConnectionType() {
        return ConnectionReader.TYPE_SERVER;
    }

    /**
	 * when connections are disconnected and the controlling object is removed from the
	 * Connections list, the status is extracted and remembered here.  This is added
	 * to the next status report and then retired.
	 * @param stat
	 */
    public void addRetiredStatus(ConnectionReader conrdr) {
        boolean sendPeer = Global.params().getParamBoolean(ParamServer.STATUS_REPORTER_CONNECTION_PEER);
        boolean sendClient = Global.params().getParamBoolean(ParamServer.STATUS_REPORTER_CONNECTION_CLIENT);
        if ((sendPeer && conrdr.ifConnectionType(ConnectionReader.TYPE_PEER)) || (sendClient && conrdr.ifConnectionType(ConnectionReader.TYPE_CLIENT))) {
            synchronized (retiredStatus) {
                retiredStatus.add(appendOneConnection(conrdr));
            }
        }
        return;
    }

    private void ReportStatus() {
        while (Global.keepWorking) {
            try {
                Thread.sleep(Global.params().getParamInt(ParamServer.STATUS_REPORTER_SECONDS) * 1000);
                ServerEventChannel se = new ServerEventChannel();
                se.setIqID("StatusReporter-" + Utilities.randomString(8));
                se.setFromService(Global.serviceName);
                se.setFromInstance(Global.serviceInstance);
                se.setToChannel(Global.params().getParamString(ParamServer.STATUS_REPORTER_CHANNEL));
                se.setPayloadNamespace(HEARTBEAT_NAMESPACE);
                se.setPayloadCompressedAndCompress(statusToString());
                if (log.ifLog(log.STATUSREPORT)) {
                    log.log(log.STATUSREPORT, "Reporting: total=" + totalMessages);
                    StringBuffer buff = new StringBuffer();
                    buff.append("   Connections=");
                    Collection<ConnectionReader> conns = Global.connections.getConnectionsClone();
                    for (Iterator<ConnectionReader> cr = conns.iterator(); cr.hasNext(); ) {
                        buff.append(cr.next().getConnectionID() + " ");
                    }
                    log.log(log.STATUSREPORT, se.getPayload());
                }
                Global.internalSender.send(se);
            } catch (Exception e) {
                log.log(log.BADERROR, "Exception: " + e.toString());
            }
        }
    }

    /**
	 * Return the status payload as a string
	 * @return
	 */
    private String statusToString() {
        StringWriter wtr = new StringWriter();
        statusToStream(wtr);
        return wtr.toString();
    }

    /**
	 * write the status payload to a stream
	 * @param wtr
	 */
    private void statusToStream(Writer wtr) {
        try {
            wtr.write("<payload xmlns='" + HEARTBEAT_NAMESPACE + "'>");
            wtr.write("<hostname>" + Global.serviceHostname + "</hostname>");
            wtr.write("<account>" + Global.serviceAccountname + "</account>");
            wtr.write("<instance>" + Global.serviceInstance + "</instance>");
            wtr.write("<hostID>" + Global.getMyID() + "</hostID>");
            wtr.write("<serverVersion>" + Global.version + "</serverVersion>");
            wtr.write("<totalMessages>" + totalMessages.toXML() + "</totalMessages>");
            wtr.write("<totalConnections>" + Global.connections.getConnections().size() + "</totalConnections>");
            wtr.write(Utilities.xmlDate("localTime"));
            wtr.write(Utilities.xmlDate("startTime", Global.startTime));
            appendConnectionInfo(wtr);
            wtr.write(Global.routeTable.statusXML());
            if (Global.params().getParamBoolean(ParamServer.STATUS_REPORTER_SERVICE_TRAFFIC)) {
                appendServiceTrafficInfo(wtr);
            }
            wtr.write("</payload>");
        } catch (Exception e) {
        }
    }

    private void appendConnectionInfo(Writer wtr) {
        try {
            wtr.write("<connectionManagers>");
            for (Iterator<ConnectionManager> mm = Global.connections.getManagers().iterator(); mm.hasNext(); ) {
                wtr.write(mm.next().getStatusXML());
            }
            wtr.write("</connectionManagers>");
            boolean sendPeer = Global.params().getParamBoolean(ParamServer.STATUS_REPORTER_CONNECTION_PEER);
            boolean sendClient = Global.params().getParamBoolean(ParamServer.STATUS_REPORTER_CONNECTION_CLIENT);
            wtr.write("<connections>");
            Collection<ConnectionReader> conns = Global.connections.getConnectionsClone();
            wtr.write("<current>");
            wtr.write(Integer.toString(conns.size()));
            wtr.write("</current>");
            try {
                for (Iterator<ConnectionReader> ii = conns.iterator(); ii.hasNext(); ) {
                    ConnectionReader cr = ii.next();
                    if ((sendPeer && cr.ifConnectionType(ConnectionReader.TYPE_PEER)) || (sendClient && cr.ifConnectionType(ConnectionReader.TYPE_CLIENT))) {
                        appendOneConnection(cr, wtr);
                    }
                }
                synchronized (retiredStatus) {
                    wtr.write("<retired>" + Integer.toString(retiredStatus.size()) + "</retired>");
                    for (Iterator<String> ii = retiredStatus.iterator(); ii.hasNext(); ) {
                        wtr.write(ii.next());
                    }
                    retiredStatus.clear();
                }
            } catch (Exception e) {
            }
            wtr.write("</connections>");
        } catch (Exception e) {
        }
    }

    /**
	 * Output the XML for one connection
	 * @param cr
	 * @param wtr
	 */
    private void appendOneConnection(ConnectionReader cr, Writer wtr) {
        try {
            wtr.write("<connection name=\"" + cr.getConnectionID() + "\">");
            wtr.write("<type>" + cr.getClass().getName() + "</type>");
            wtr.write(cr.getStatsXML());
            wtr.write("</connection>");
        } catch (Exception e) {
        }
    }

    /**
	 * Return the XML for one connection
	 * @param cr
	 * @return
	 */
    private String appendOneConnection(ConnectionReader cr) {
        StringWriter sw = new StringWriter();
        appendOneConnection(cr, sw);
        return sw.toString();
    }

    private void appendServiceTrafficInfo(Writer wtr) {
        try {
            wtr.write("<serviceTraffic>");
            for (Iterator<String> ii = messagesByUser.keySet().iterator(); ii.hasNext(); ) {
                String serviceName = ii.next();
                if (messagesByUser.get(serviceName).isZero()) {
                    ii.remove();
                    continue;
                }
                wtr.write("<service>");
                wtr.write("<name>");
                wtr.write(serviceName);
                wtr.write("</name>");
                messagesByUser.get(serviceName).toXML(wtr);
                wtr.write("</service>");
            }
            wtr.write("</serviceTraffic>");
        } catch (Exception e) {
        }
        return;
    }

    /**
	 * All of the events that more through the system pass this point.
	 * Do some accounting.
	 */
    public void send(ServerEvent se) {
        totalMessages.count();
        if (se instanceof ServerEventChannel) {
            ServerEventChannel sec = (ServerEventChannel) se;
            if (sec.getFromService() != null) {
                String fs = sec.getFromService();
                Counter ii = messagesByUser.get(fs);
                if (ii == null) {
                    ii = new Counter();
                    ii.setExpirationTime(Global.params().getParamLong(ParamServer.STATUS_REPORTER_SERVICE_EXPIRE_MIN) * 60 * 1000);
                    messagesByUser.put(fs, ii);
                }
                ii.count();
            }
        }
        return;
    }
}
