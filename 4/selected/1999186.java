package org.psepr.PsEPRServer.Commands;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import org.psepr.PsEPRServer.ConnectionReader;
import org.psepr.PsEPRServer.DebugLogger;
import org.psepr.PsEPRServer.Global;
import org.psepr.PsEPRServer.PsEPRServerException;
import org.psepr.PsEPRServer.ParamCollection;
import org.psepr.PsEPRServer.ParamFile;
import org.psepr.PsEPRServer.ParamServer;
import org.psepr.PsEPRServer.Processor;
import org.psepr.PsEPRServer.ServerEvent;
import org.psepr.PsEPRServer.ServerEventChannel;
import org.psepr.PsEPRServer.Utilities;
import org.psepr.services.service.ChannelUseDescription;
import org.psepr.services.service.ChannelUseDescriptionCollection;
import org.psepr.services.service.EventDescription;
import org.psepr.services.service.EventDescriptionCollection;

public class ServerCommands extends Processor {

    DebugLogger log;

    public static final String UPDATE_NAMESPACE = "http://psepr.org/xs/psepr/server/parameter";

    public static final String PARAMETER_NAMESPACE = "http://dsmt.org/schema/psepr/payload/PsEPRServer/parameters-1.0";

    public ServerCommands() {
        super();
        this.init();
    }

    private void init() {
        log = new DebugLogger("ServerCommands");
    }

    public void start() {
        Global.routeTable.addRoute("ServerCommands", Global.params().getParamString(ParamServer.PARAM_CHANNEL_UPDATE), UPDATE_NAMESPACE, ConnectionReader.TYPE_CLIENT, this);
        if (Global.serviceStatus != null) {
            ChannelUseDescriptionCollection cudc = Global.serviceStatus.getServiceDescription().getChannelUses();
            cudc.add(new ChannelUseDescription(Global.params().getParamString(ParamServer.PARAM_CHANNEL_UPDATE), "dynamic update of server parameters", 0L, 0L, null, new EventDescriptionCollection(new EventDescription(UPDATE_NAMESPACE, "specification of parameters to change"))));
            cudc.add(new ChannelUseDescription(Global.params().getParamString(ParamServer.PARAM_CHANNEL_REPORT), "reporting of the current setting of server parameters", 0L, 0L, new EventDescriptionCollection(new EventDescription(PARAMETER_NAMESPACE, "the current parameter setting")), null));
        }
        return;
    }

    public void stop() {
        Global.routeTable.removeRoute(this);
        return;
    }

    /**
	 * If asked, the route is for a client connection. This makes sure that the route added to the
	 * routeTable in start() is sent out to the other peers.
	 */
    public int getConnectionType() {
        return ConnectionReader.TYPE_CLIENT;
    }

    public void send(ServerEvent se) {
        if (!(se instanceof ServerEventChannel)) {
            log.log(log.SERVICES, "Received server event of unknown type: " + se.getClass().getName());
            return;
        }
        ServerEventChannel sec = (ServerEventChannel) se;
        if (sec.getToChannel() == null || sec.getPayload() == null) {
            log.log(log.SERVICES, "Received server command message for no channel or no payload");
            return;
        }
        if (!sec.getFromService().equalsIgnoreCase(Global.params().getParamString(ParamServer.PARAM_UPDATE_SERVICE))) {
            log.log(log.SERVICES, "Received command from odd service:" + sec.getFromService());
            return;
        }
        try {
            ParamCollection commFlags = new ParamCollection();
            ParamFile commReader = new ParamFile();
            Reader rdr = new StringReader(sec.getPayload());
            commReader.parseFile(rdr, commFlags);
            if (commFlags.hasParam("/payload/destinationHost")) {
                String destinationHost = commFlags.getParam("/payload/destinationHost", "");
                if (!destinationHost.equalsIgnoreCase(Global.serviceHostname)) {
                    log.log(log.SERVICES, "Parameter change command not addressed to me. To " + destinationHost);
                    return;
                }
            }
            if (commFlags.hasParam("/payload/parameter/name") && commFlags.hasParam("/payload/parameter/value")) {
                String pName = commFlags.getParam("/payload/parameter/name", "xx");
                String pValue = commFlags.getParam("/payload/parameter/value", "xx");
                log.log(log.SERVICES, "setting param '" + pName + " to " + pValue);
                Global.params().addParam(pName, pValue);
            } else if (commFlags.hasParam("/payload/resetLogfile")) {
                log.log(log.SERVICES, "Refreshing debug files");
                DebugLogger.refreshDebugFile();
            } else if (commFlags.hasParam("/payload/reportStatus")) {
                log.log(log.SERVICES, "Forcing a push of status");
            } else if (commFlags.hasParam("/payload/reportParameters")) {
                log.log(log.SERVICES, "Causing my current parameter configuration to be output");
                reportParameters();
            }
        } catch (Exception e) {
            log.log(log.SERVICES, "Exception parsing command payload: " + e.toString());
            throw new PsEPRServerException("Could not parse command payload:" + e.toString());
        }
    }

    private void reportParameters() {
        try {
            ServerEventChannel se = new ServerEventChannel();
            se.setIqID("Discovery-" + Utilities.randomString(8));
            se.setFromService(Global.serviceName);
            se.setFromInstance(Global.serviceInstance);
            se.setToChannel(Global.params().getParamString(ParamServer.PARAM_CHANNEL_REPORT));
            se.setPayloadNamespace(PARAMETER_NAMESPACE);
            StringWriter wtr = new StringWriter();
            wtr.write("<payload xmlns=\"" + PARAMETER_NAMESPACE + "\">");
            Global.params().paramsToXML(wtr);
            wtr.write("</payload>");
            se.setPayload(wtr.toString());
            Global.internalSender.send(se);
        } catch (Exception e) {
        }
        return;
    }
}
