package org.psepr.PsEPRServer.Discovery;

import java.io.StringWriter;
import java.util.Iterator;
import org.psepr.PsEPRServer.ConnectionReader;
import org.psepr.PsEPRServer.ConnectionManager;
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
 * Transmits information about the ConnectionManagers so clients can discover who
 * to connect to.
 * <p>
 * This routine periodicaly asks all ConnectionManagers their connection port information
 * and then transmits this in a block on a specific channel. This allows clients to
 * listen to that channel and find the specifics of the connection managers
 * they can connect to.
 * </p>
 * 
 * @author radams1
 *
 */
public class Discovery extends Processor {

    public static final String DISCOVERY_NAMESPACE = "http://dsmt.org/schema/psepr/payload/PsEPRServer/discovery-1.0";

    public Discovery() {
        super();
        this.init();
    }

    private void init() {
        if (Global.serviceStatus != null) {
            ChannelUseDescriptionCollection cudc = Global.serviceStatus.getServiceDescription().getChannelUses();
            cudc.add(new ChannelUseDescription(Global.params().getParamString(ParamServer.DISCOVERY_CHANNEL), "connection information so clients can discover the servers and their capabilities", 12L, 120L, new EventDescriptionCollection(new EventDescription(DISCOVERY_NAMESPACE, "information on services available from this server")), null));
        }
        return;
    }

    public void start() {
        Global.threads.execute(new Runnable() {

            public void run() {
                GenerateDiscovery();
            }
        });
    }

    public void stop() {
    }

    /**
	 * If asked, this is just a server service.
	 */
    public int getConnectionType() {
        return ConnectionReader.TYPE_SERVER;
    }

    private void GenerateDiscovery() {
        Thread.currentThread().setName(this.getClass().getName());
        while (Global.keepWorking) {
            try {
                Thread.sleep(Global.params().getParamInt(ParamServer.DISCOVERY_SECONDS) * 1000);
                ServerEventChannel se = new ServerEventChannel();
                se.setIqID("Discovery-" + Utilities.randomString(8));
                se.setFromService(Global.serviceName);
                se.setFromInstance(Global.serviceInstance);
                se.setToChannel(Global.params().getParamString(ParamServer.DISCOVERY_CHANNEL));
                se.setPayloadNamespace(DISCOVERY_NAMESPACE);
                StringWriter wtr = new StringWriter();
                wtr.write("<payload xmlns=\"" + DISCOVERY_NAMESPACE + "\">");
                wtr.write("<connections>");
                wtr.write("<hostname>");
                wtr.write(Global.serviceHostname);
                wtr.write("</hostname>");
                wtr.write("<instance>");
                wtr.write(Global.serviceInstance);
                wtr.write("</instance>");
                wtr.write("<serverVersion>");
                wtr.write(Global.version);
                wtr.write("</serverVersion>");
                for (Iterator<ConnectionManager> ii = Global.connections.getManagers().iterator(); ii.hasNext(); ) {
                    ii.next().xmlForDiscovery(wtr);
                }
                wtr.write("</connections>");
                wtr.write("</payload>");
                se.setPayload(wtr.toString());
                Global.internalSender.send(se);
            } catch (Exception e) {
            }
        }
    }

    public void send(ServerEvent pMessage) {
        return;
    }
}
