package eu.ict.persist.ThirdPartyServices.DMTService.impl;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;
import org.personalsmartspace.log.impl.PSSLog;
import org.personalsmartspace.sre.api.pss3p.IServiceIdentifier;
import org.personalsmartspace.sre.slm.api.pss3p.ISlm3P;
import eu.ict.persist.ThirdPartyServices.DMTService.api.DMTService;
import eu.ict.persist.ThirdPartyServices.DMTService.interfaces.IDMT2PSS;
import eu.ict.persist.ThirdPartyServices.DMTService.interfaces.IDMTPDSSListener;
import eu.ict.persist.ThirdPartyServices.DMTService.interfaces.IPSS2DMT;

/**
 * use case 1: view
 * - on restore view notify PSS (users loads view)
 * - while e.g. same timestamp PSS sets view
 * use case 2: send POIs
 * - users manually sends POIs to other DMTs
 * - PSS triggers automatically sendPOIs
 * additionally:
 * - PSS gets position (every second)
 * - PSS gets direction (every second)
 * NICE TO HAVE
 * use case 3: synchronise POIs based on category
 * - on startup the PSS is informed about the categories in scope
 * - PSS organizes the synchronisation of these categories
 */
public class SocketThread extends Thread implements IPSS2DMT, IDMTPDSSListener {

    private PSSLog logger = new PSSLog(this);

    private int port;

    private ServerSocket serverSocket;

    private Socket clientSocket;

    private HashMap<IServiceIdentifier, DMTService> listeners;

    private WriteThread writer;

    private IDMT2PSS persistDMT;

    private ISlm3P slm;

    public SocketThread(ISlm3P slm, int port, IDMT2PSS dmtService) {
        setName("DMTService waiting for ServerSocket on port " + port);
        this.port = port;
        this.slm = slm;
        listeners = new HashMap<IServiceIdentifier, DMTService>();
        this.persistDMT = dmtService;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            logger.debug("waiting for accept");
            while (true) {
                clientSocket = serverSocket.accept();
                logger.debug("connection established via serverport " + port + " on " + clientSocket.getPort());
                new ListenThread(clientSocket, this).start();
                writer = new WriteThread(clientSocket, this);
                writer.start();
            }
        } catch (Exception e) {
            exceptionCaught(e);
        }
    }

    public void shutdown() {
        try {
            clientSocket.close();
            serverSocket.close();
        } catch (Exception e) {
        }
    }

    public void exceptionCaught(Exception e) {
        if ("Connection reset".equals(e.getMessage())) {
            return;
        }
        e.printStackTrace();
        shutdown();
    }

    /** Process a message coming from a DMT (PersistAdapter)
	 * @param protocolType
	 * @param message
	 */
    public void processMessage(String protocolType, String message) {
        if (protocolType.equals(DMTPPSProtocol.SEND_POI)) {
            logger.debug("SYNCHRONIZE POIs: #listeners=" + listeners.size());
            for (IServiceIdentifier partnerID : listeners.keySet()) {
                DMTService proxy = listeners.get(partnerID);
                if (proxy == null) {
                    proxy = (DMTService) slm.getProxy(partnerID);
                    listeners.put(partnerID, proxy);
                }
                logger.debug(proxy);
                proxy.addPOI(message);
            }
        } else if (protocolType.equals(DMTPPSProtocol.SEND_VIEW)) {
            logger.debug("SEND VIEW: #listeners=" + listeners.size());
            for (IServiceIdentifier partnerID : listeners.keySet()) {
                DMTService partner = listeners.get(partnerID);
                if (partner == null) {
                    partner = (DMTService) slm.getProxy(partnerID);
                    listeners.put(partnerID, partner);
                }
                logger.debug(partner);
                partner.processView(message);
            }
        } else if (protocolType.equals(DMTPPSProtocol.SENDPOIS_MANUALLY_TRIGGERED)) {
            persistDMT.poisSent();
        } else if (protocolType.equals(DMTPPSProtocol.VIEW_LOADED)) {
            persistDMT.viewLoaded(message);
        } else if (protocolType.equals(DMTPPSProtocol.GPS_STATUS)) {
            persistDMT.gpsConnected(new Boolean(message));
        } else if (protocolType.equals(DMTPPSProtocol.POSITION_UPDATE)) {
            String[] splittedString = message.split(DMTPPSProtocol.DELIMITER);
            persistDMT.setPosition(new Double(splittedString[0]), new Double(splittedString[1]), new Double(splittedString[2]), new Integer(splittedString[3]));
        } else if (protocolType.equals(DMTPPSProtocol.COMPASS_STATUS)) {
            persistDMT.compassConnected(new Boolean(message));
        } else if (protocolType.equals(DMTPPSProtocol.DIRECTION_UPDATE)) {
            String[] splittedString = message.split(DMTPPSProtocol.DELIMITER);
            persistDMT.setDirection(new Double(splittedString[0]), new Double(splittedString[1]), new Double(splittedString[2]));
        } else {
        }
    }

    @Override
    public void addCommunicationPartner(IServiceIdentifier serviceID) {
        listeners.put(serviceID, null);
    }

    @Override
    public void removeCommunicationPartner(IServiceIdentifier partner) {
        listeners.remove(partner);
    }

    @Override
    public void sendPOIs() {
        writer.write(DMTPPSProtocol.TRIGGER_SENDPOIS, "");
    }

    @Override
    public void addPOI(String POIXML) {
        if (writer != null) writer.write(DMTPPSProtocol.ADD_POI, POIXML);
    }

    @Override
    public void processView(String viewXML) {
        setView(viewXML);
    }

    /**
	 * View is set by PSS -> inform DMT
	 */
    @Override
    public void setView(String viewXML) {
        if (writer != null) writer.write(DMTPPSProtocol.SET_VIEW, viewXML);
    }

    public void setAllViews(String viewXML) {
        setView(viewXML);
        logger.debug("SEND VIEW: #listeners=" + listeners.size());
        for (IServiceIdentifier partnerID : listeners.keySet()) {
            DMTService partner = listeners.get(partnerID);
            if (partner == null) {
                partner = (DMTService) slm.getProxy(partnerID);
                listeners.put(partnerID, partner);
            }
            logger.debug(partner);
            partner.processView(viewXML);
        }
    }
}
