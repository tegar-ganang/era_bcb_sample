package jp.ac.nii.jgear.sink.applicationLayer;

import jp.ac.nii.jgear.sink.localisation.Coordinate;
import jp.ac.nii.jgear.sink.localisation.CreatingRegionProcess;
import jp.ac.nii.jgear.sink.localisation.LearningProcess;
import jp.ac.nii.jgear.sink.localisation.Region;
import jp.ac.nii.jgear.sink.localisation.SpotLocation;
import jp.ac.nii.jgear.sink.networklayer.NetworkLayer;
import jp.ac.nii.jgear.sink.protocoldataunit.Constants;
import jp.ac.nii.jgear.sink.protocoldataunit.InitialisationBS;
import jp.ac.nii.jgear.sink.protocoldataunit.Message;
import jp.ac.nii.jgear.sink.transportlayer.TransportLayer;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.radio.ILowPan;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.io.j2me.radiostream.*;
import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.peripheral.radio.LowPan;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import java.io.*;
import java.util.Calendar;
import java.util.Vector;
import javax.microedition.io.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import jp.ac.nii.jgear.sink.org.sunspotworld.SunSpotHostApplication;

/**
 * Sample Sun SPOT host application
 */
public class ApplicationLayer {

    private long bsAddress = Spot.getInstance().getRadioPolicyManager().getIEEEAddress();

    private static long bsRegion;

    private static Coordinate coordinateBS;

    private ILowPan lowPan = (LowPan) LowPan.getInstance();

    private static Vector coordinateSpots;

    private static CreatingRegionProcess creatingRegionProcess;

    private static String destination;

    private DatagramConnection connectionListen;

    private static NetworkLayer layer3;

    private static TransportLayer layer4;

    private static ApplicationLayer instance;

    private SunSpotHostApplication graphicApplication;

    private int nbRequestSent;

    private int nbReplyReceived;

    private int nbReplyLost;

    private long targetRegion;

    /** Creates a new instance of ApplicationLayer */
    private ApplicationLayer() {
    }

    /**
    * @return ApplicationLayer instance of this singleton
    */
    public static ApplicationLayer getInstance() {
        if (instance == null) {
            instance = new ApplicationLayer();
        }
        return instance;
    }

    /** Gets the graphicApplication */
    public SunSpotHostApplication getGraphicApplication() {
        return this.graphicApplication;
    }

    /** Gets the number of requests sent */
    public int getNbRequestSent() {
        return nbRequestSent;
    }

    /** Gets the number of replies received */
    public int getNbReplyReceived() {
        return nbReplyReceived;
    }

    /** Gets the number of of replies lost */
    public int getNbReplyLost() {
        return nbReplyLost;
    }

    /** Sets the number of requests sent */
    public void getNbRequestSent(int nbRequestSent) {
        this.nbRequestSent = nbRequestSent;
    }

    /** Sets the number of replies received */
    public void getNbReplyReceived(int nbReplyReceived) {
        this.nbReplyReceived = nbReplyReceived;
    }

    /**Sets the number of of replies lost */
    public void getNbReplyLost(int nbReplyLost) {
        this.nbReplyLost = nbReplyLost;
    }

    /** Receives the coordinates of all the spots during the initialisation phase */
    public synchronized SpotLocation receiveCoordinateMessage() {
        SpotLocation spotLocation = null;
        try {
            if (connectionListen == null) connectionListen = (DatagramConnection) Connector.open("radiogram://:37");
            Datagram dg = connectionListen.newDatagram(connectionListen.getMaximumLength());
            connectionListen.receive(dg);
            byte[] message = dg.getData();
            String source = dg.getAddress();
            System.out.println("Node " + source);
            Coordinate coordinate = InitialisationBS.readDatagram(message);
            spotLocation = new SpotLocation(coordinate, source, 0);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return spotLocation;
    }

    /** Receives a message */
    public synchronized ReceivedMessage receiveMessage() {
        ReceivedMessage receivedMessage = null;
        try {
            if (connectionListen == null) connectionListen = (DatagramConnection) Connector.open("radiogram://:37");
            Datagram dg = null;
            dg = connectionListen.newDatagram(connectionListen.getMaximumLength());
            connectionListen.receive(dg);
            byte[] message = dg.getData();
            String messageSender = dg.getAddress();
            receivedMessage = new ReceivedMessage(message, messageSender);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return receivedMessage;
    }

    /** Sends a broadcast message */
    public synchronized void sendBroadcastMessage(byte[] data) {
        try {
            DatagramConnection connection = (DatagramConnection) Connector.open("radiogram://broadcast:38");
            Datagram dg = connection.newDatagram(connection.getMaximumLength());
            dg.reset();
            dg.write(data);
            Utils.sleep(10);
            connection.send(dg);
            Utils.sleep(10);
            connection.close();
            connection = null;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** Sends a unicast message */
    public synchronized void sendMessage(String address, byte[] data) {
        try {
            DatagramConnection connection = (DatagramConnection) Connector.open("radiogram://" + address + ":38");
            Datagram dg = connection.newDatagram(connection.getMaximumLength());
            dg.reset();
            dg.write(data);
            Utils.sleep(10);
            connection.send(dg);
            Utils.sleep(10);
            connection.close();
            connection = null;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** Starts the coordinates reception thread */
    public void startReceiverCoordinate() {
        String message;
        int nb = 0;
        SpotLocation spotLocation = null;
        while (nb < Constants.NB_SPOTS) {
            spotLocation = receiveCoordinateMessage();
            if (!isPresent(spotLocation)) {
                coordinateSpots.add(spotLocation);
                nb++;
            }
        }
        for (int i = 0; i < coordinateSpots.size(); i++) ((SpotLocation) coordinateSpots.elementAt(i)).print();
        creatingRegionProcess.appointRegions(coordinateSpots);
        creatingRegionProcess.appointNodes();
        Region regionBS = new Region(bsRegion, coordinateBS, 0, 0, 0);
        creatingRegionProcess.addBSRegion(regionBS);
        for (int i = 0; i < coordinateSpots.size(); i++) {
            spotLocation = (SpotLocation) coordinateSpots.elementAt(i);
            spotLocation.print();
            String spotAddress = spotLocation.getSpotAddress();
            long spotRegion = spotLocation.getNameRegion();
            byte[] messageSpot = InitialisationBS.writeBelongingRegion(spotRegion);
            sendMessage(spotAddress, messageSpot);
        }
        int nbRegions = creatingRegionProcess.getNbRegions();
        System.out.println("NbRegions=" + nbRegions);
        for (int i = 0; i < nbRegions; i++) {
            byte[] featuresRegionBroadcast = InitialisationBS.writeRegion(creatingRegionProcess.getRegion(i));
            sendBroadcastMessage(featuresRegionBroadcast);
        }
        byte[] endProcess = InitialisationBS.writeEnd();
        sendBroadcastMessage(endProcess);
    }

    /** Starts the initialisation sending thread */
    public void startSenderInitialisation() {
        System.out.println("startSenderInitialisationThread = " + InitialisationBS.writeStart());
        sendBroadcastMessage(InitialisationBS.writeStart());
        Utils.sleep(10);
    }

    /** Sends a request message */
    public void sendRequest(long region, byte temperatureCondition, double temperatureLevel, byte lightCondition, long lightLevel) {
        writeMessageTime("Request to region " + region);
        System.out.println("REQUEST to region " + region);
        writeMessage("\n");
        this.targetRegion = region;
        this.layer4.sendRequest(region, temperatureCondition, temperatureLevel, lightCondition, lightLevel);
        this.nbReplyLost += getNbSpotsRegion(this.targetRegion);
        this.graphicApplication.getReplyLostField().setText(Integer.toString(this.nbReplyLost));
    }

    /**
     * Start up the host application.
     *
     * @param args any command line arguments
     */
    public void initialize(SunSpotHostApplication graphicApplication, Coordinate coordinateBS) {
        this.bsRegion = 0;
        this.coordinateBS = coordinateBS;
        this.coordinateSpots = new Vector();
        this.creatingRegionProcess = new CreatingRegionProcess();
        this.graphicApplication = graphicApplication;
        this.nbRequestSent = 0;
        this.nbReplyReceived = 0;
        this.nbReplyLost = 0;
        this.targetRegion = -1;
    }

    /** Starts the application */
    public void startApp() throws MIDletStateChangeException {
        startSenderInitialisation();
        startReceiverCoordinate();
    }

    /** Starts GEAR protocol */
    public void startGEAR() {
        this.layer3 = NetworkLayer.getInstance();
        this.layer3.initialize(bsAddress, lowPan, coordinateBS, bsRegion, creatingRegionProcess.getregions());
        this.layer4 = TransportLayer.getInstance();
        this.layer4.initialize(bsAddress, bsRegion);
        Utils.sleep(5000);
        System.out.println("End of initialisation !!!!! ");
    }

    /** Stops the SPOT */
    public void rebootSPOTs() {
        this.layer4.getInstance().rebootSPOTs();
    }

    /** Stops the base station */
    public void stopBS() {
        System.exit(0);
    }

    /** Gets the number of spot in the target region region */
    public int getNbSpotsRegion(long region) {
        return this.creatingRegionProcess.getNbSpots(region);
    }

    /** Writes the message received and the time of reception in the text area  */
    public void writeMessageTime(String message) {
        Calendar rightNow = Calendar.getInstance();
        graphicApplication.getTextArea().append("At " + rightNow.getTime().toString());
        graphicApplication.getTextArea().append("\n" + message);
    }

    /** Writes the message received in the text area */
    public void writeMessage(String message) {
        graphicApplication.getTextArea().append("\n" + message);
    }

    /** Raises the number of replies received */
    public void raiseNbRepliesReceived() {
        this.nbReplyReceived++;
        this.nbReplyLost--;
        this.graphicApplication.getReplyReceivedField().setText(Integer.toString(this.nbReplyReceived));
        System.out.println("nbReplyReceived=" + nbReplyReceived);
        System.out.println("nbReplyLost=" + nbReplyLost);
        this.graphicApplication.getReplyLostField().setText(Integer.toString(this.nbReplyLost));
    }

    /** Raises the number of requests sent */
    public void raiseNbRequestsSent() {
        this.nbRequestSent++;
        this.graphicApplication.getRequestSendField().setText(Integer.toString(this.nbRequestSent));
    }

    /** Determine if the SPOT's coordinate is present in the coordinateSpots vector */
    private boolean isPresent(SpotLocation spotLocation) {
        int i = 0;
        boolean found = false;
        SpotLocation spotLocation_i = null;
        while ((i < coordinateSpots.size()) && !found) {
            spotLocation_i = (SpotLocation) coordinateSpots.elementAt(i);
            found = spotLocation_i.equals(spotLocation);
            i++;
        }
        return found;
    }

    private class ReceivedMessage {

        public byte[] message;

        public String messageSender;

        public ReceivedMessage(byte[] message, String messageSender) {
            this.message = message;
            this.messageSender = messageSender;
        }
    }
}
