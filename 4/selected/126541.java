package org.grailrtls.gui.network;

import java.awt.geom.Point2D;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.jdom.*;
import org.jdom.input.*;
import org.jdom.xpath.*;
import org.grailrtls.gui.event.*;
import org.grailrtls.server.*;
import org.grailrtls.server.util.UnitsPerSecond;

/**
 * @author romoore
 * 
 */
public class ServerInterface extends Thread {

    public static final int SERVER_TIMEOUT = 5000;

    /**
	 * User name to log in to the GRAIL server.
	 */
    private String userName = null;

    /**
	 * Password to log in to the GRAIL server.
	 */
    private String password = null;

    /**
	 * The server's hostname.
	 */
    private InetAddress serverAddress = null;

    /**
	 * The server's port number.
	 */
    private int port = -1;

    /**
	 * The TCP socket used to communicate with the GRAIL server.
	 */
    private Socket serverConnection;

    /**
	 * For receiving responses from the server.
	 */
    private DataInputStream serverInput = null;

    public final UnitsPerSecond receiveBPS = new UnitsPerSecond();

    /**
	 * For writing commands to the server.
	 */
    private PrintWriter serverOutput = null;

    private List<ServerListener> listeners = new ArrayList<ServerListener>();

    public transient boolean stayConnected = false;

    protected ServerInfo serverInfo;

    private volatile boolean recordingStream = false;

    private File recordFile = null;

    private DataOutputStream saveStream = null;

    private volatile long fileStartTime = 0l;

    /**
	 * Creates a new {@code ServerInterface} object with no connection
	 * information.
	 */
    public ServerInterface() {
        serverConnection = null;
        this.serverInfo = new ServerInfo();
    }

    /**
	 * Changes the server address and port. Does not modify any existing server
	 * connection. Call {@link #openConnection()} to establish a connection
	 * based on the new server information.
	 * 
	 * @param serverAddress
	 *            the IP address or hostname of the server.
	 * @param port
	 *            the port number of the server.
	 * @return {@code true} if the server information is successfully updated.
	 */
    public synchronized boolean setServerInfo(InetAddress serverAddress, int port) {
        if (serverAddress == null) {
            System.err.println("Invalid server address.");
            return false;
        }
        if (port < 1 || port > 65535) {
            System.err.println("Invalid port number.");
            return false;
        }
        this.serverAddress = serverAddress;
        this.port = port;
        return true;
    }

    /**
	 * Changes the login information for the server. Does not modify any
	 * existing server connection. Call {@link #loginToServer(String, String)}
	 * to log in to the server with the new user information.
	 * 
	 * @param userName
	 *            the user name used to log in to the server.
	 * @param password
	 *            the password used to log in to the server.
	 * @return {@code true} if the user information is updated successfully.
	 */
    public synchronized boolean setUserInfo(String userName, String password) {
        if (userName == null || userName.trim().length() == 0) {
            System.err.println("Username is invalid.");
            return false;
        }
        if (password == null || password.trim().length() == 0) {
            System.err.println("Password is invalid.");
            return false;
        }
        this.userName = userName;
        this.password = password;
        return true;
    }

    /**
	 * Establishes a new socket to the GRAIL server.
	 * 
	 * @return {@code true} if the socket was successfully created.
	 */
    private synchronized boolean openConnection() {
        this.closeConnection();
        if (this.serverAddress == null || this.port <= 0 || this.port > 65534) return false;
        try {
            this.serverConnection = new Socket(this.serverAddress, this.port);
        } catch (IOException ioe) {
            System.err.println("Failed to create new connection to server. (" + this.serverAddress + ":" + this.port + ")");
            return false;
        }
        try {
            this.serverInput = new DataInputStream(this.serverConnection.getInputStream());
            this.serverConnection.setSoTimeout(500);
        } catch (IOException ioe) {
            System.err.println("Failed to get input stream from server.");
            return false;
        }
        try {
            this.serverOutput = new PrintWriter(this.serverConnection.getOutputStream(), true);
        } catch (IOException ioe) {
            System.err.println("Failed to get output stream to server.");
            return false;
        }
        try {
            this.serverConnection.setSoTimeout(ServerInterface.SERVER_TIMEOUT);
        } catch (SocketException se) {
            System.err.println("Failed to set socket timeout for server connection.");
            return false;
        }
        this.fireServerEvent(new ConnectionEvent(this, true));
        return true;
    }

    /**
	 * Closes the connection to the server.
	 */
    public synchronized void closeConnection() {
        if (serverConnection == null) return;
        System.err.println("Closing server connection.");
        if (!serverConnection.isClosed()) {
            try {
                serverConnection.close();
            } catch (IOException ioe) {
                System.err.println("Failed to close existing connection to server. Ignoring.");
            }
        }
        serverConnection = null;
        this.fireServerEvent(new ConnectionEvent(this, false));
    }

    public synchronized boolean loginToServer() {
        if (this.userName == null || this.userName.trim().length() == 0 || this.password == null || this.password.trim().length() == 0) return false;
        if (!this.openConnection()) {
            System.err.println("Unable to open new connection to the server.");
            return false;
        }
        this.serverOutput.println("login " + this.userName + " " + this.password);
        String response = null;
        try {
            response = this.serverInput.readLine();
        } catch (IOException ioe) {
            System.err.println("Couldn't read response from server.");
            return false;
        }
        if (response == null || !response.trim().equalsIgnoreCase("OK")) return false;
        this.restart();
        this.sendConsoleCommand("event subscribe");
        this.startRecording();
        return true;
    }

    public synchronized boolean checkServerConnection() {
        if (this.serverConnection == null || this.serverConnection.isClosed() || !this.serverConnection.isConnected() || this.serverConnection.isInputShutdown() || this.serverConnection.isOutputShutdown()) {
            return this.loginToServer();
        }
        return true;
    }

    public synchronized void stopRunning() {
        this.stayConnected = false;
        this.stopRecording();
    }

    public synchronized void restart() {
        this.stayConnected = true;
        this.notifyAll();
    }

    public synchronized void sendConsoleCommand(String command) {
        if (!this.checkServerConnection()) {
            this.closeConnection();
            return;
        }
        if (command.equalsIgnoreCase("exit")) {
            this.stopRunning();
            this.closeConnection();
        } else this.serverOutput.println(command);
    }

    public synchronized void addServerListener(ServerListener listener) {
        synchronized (this.listeners) {
            this.listeners.add(listener);
        }
    }

    public synchronized void removeServerListener(ServerListener listener) {
        synchronized (this.listeners) {
            this.listeners.remove(listener);
        }
    }

    protected synchronized void fireServerEvent(final ServerEvent se) {
        synchronized (this.listeners) {
            for (final ServerListener listener : this.listeners) {
                listener.serverEventPerformed(se);
            }
        }
    }

    public synchronized String getServerString() {
        return this.serverAddress.toString() + ":" + this.port;
    }

    public ServerInfo getServerInfo() {
        return this.serverInfo;
    }

    public void run() {
        while (true) {
            if (this.stayConnected) {
                int message_length = -1;
                try {
                    message_length = this.serverInput.readInt();
                } catch (SocketTimeoutException ste) {
                    continue;
                } catch (IOException ioe) {
                    System.err.println(ioe.getLocalizedMessage());
                    this.stopRunning();
                    continue;
                }
                if (message_length < 1) {
                    this.stopRunning();
                    continue;
                }
                byte type = (byte) EventManager.MESSAGE_TYPE_UNKNOWN;
                try {
                    type = this.serverInput.readByte();
                } catch (IOException ioe) {
                    this.stopRunning();
                    continue;
                }
                final byte[] message = new byte[message_length - 1];
                int k = 0, read = 0;
                while (read < message.length) {
                    try {
                        k = this.serverInput.read(message, read, message.length - read);
                        if (k == -1) {
                            this.stopRunning();
                            continue;
                        }
                        if (k == 0) {
                            System.err.println("No data read...");
                            continue;
                        }
                        read += k;
                    } catch (SocketTimeoutException ste) {
                        continue;
                    } catch (IOException ioe) {
                        this.stopRunning();
                        break;
                    }
                }
                if (read < message.length) {
                    this.stopRunning();
                    continue;
                }
                this.receiveBPS.updateUnits(message.length + 5);
                if (this.recordingStream) this.recordMessage(message, type);
                switch(type) {
                    case EventManager.MESSAGE_TYPE_CONSOLE:
                        try {
                            this.fireServerEvent(new CommandResponseEvent(this, new String(message, "ASCII")));
                        } catch (UnsupportedEncodingException uee) {
                            this.stopRunning();
                            continue;
                        }
                        break;
                    case EventManager.MESSAGE_TYPE_LOCATION:
                        if (!this.handleLocation(message)) {
                            this.stopRunning();
                            continue;
                        }
                        break;
                    case EventManager.MESSAGE_TYPE_FINGERPRINT_MEAN_GZIP:
                        if (!this.handleFingerprint(message, EventManager.MESSAGE_TYPE_FINGERPRINT_MEAN_GZIP)) {
                            this.stopRunning();
                            continue;
                        }
                        break;
                    case EventManager.MESSAGE_TYPE_FINGERPRINT_STDEV_GZIP:
                        if (!this.handleFingerprint(message, EventManager.MESSAGE_TYPE_FINGERPRINT_STDEV_GZIP)) {
                            this.stopRunning();
                            continue;
                        }
                        break;
                    case EventManager.MESSAGE_TYPE_XML_GZIP:
                        ByteArrayOutputStream uncompressed = new ByteArrayOutputStream();
                        try {
                            GZIPInputStream unzipStream = new GZIPInputStream(new ByteArrayInputStream(message));
                            byte[] buffer = new byte[1024];
                            int readGZ = 0;
                            while ((readGZ = unzipStream.read(buffer, 0, buffer.length)) > 0) {
                                uncompressed.write(buffer, 0, readGZ);
                            }
                        } catch (IOException ioe) {
                            System.err.println("Couldn't decompress XML.");
                            break;
                        }
                        if (uncompressed.size() == 0) {
                            System.err.println("No data decompressed.");
                            break;
                        }
                        try {
                            this.handleXML(new String(uncompressed.toByteArray(), "ASCII"));
                        } catch (UnsupportedEncodingException uee) {
                            this.stopRunning();
                            continue;
                        }
                        break;
                    case EventManager.MESSAGE_TYPE_XML:
                        try {
                            this.handleXML(new String(message, "ASCII"));
                        } catch (UnsupportedEncodingException uee) {
                            this.stopRunning();
                            continue;
                        }
                        break;
                    case EventManager.MESSAGE_TYPE_STATISTICS_GZIP:
                        ByteArrayOutputStream uncompressedStat = new ByteArrayOutputStream();
                        try {
                            GZIPInputStream unzipStream = new GZIPInputStream(new ByteArrayInputStream(message));
                            byte[] buffer = new byte[1024];
                            int readGZ = 0;
                            while ((readGZ = unzipStream.read(buffer, 0, buffer.length)) > 0) {
                                uncompressedStat.write(buffer, 0, readGZ);
                            }
                        } catch (IOException ioe) {
                            System.err.println("Couldn't decompress XML.");
                            break;
                        }
                        if (uncompressedStat.size() == 0) {
                            System.err.println("No data decompressed.");
                            break;
                        }
                        if (!this.handleStatistics(uncompressedStat.toByteArray())) {
                            this.stopRunning();
                            continue;
                        }
                        break;
                    case EventManager.MESSAGE_TYPE_HUB_CONNECT:
                        if (!this.handleHubConnection(message)) {
                            this.stopRunning();
                            continue;
                        }
                        break;
                    default:
                        this.stopRunning();
                        continue;
                }
            } else {
                this.closeConnection();
                try {
                    synchronized (this) {
                        this.wait();
                    }
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    protected boolean handleLocation(byte[] message) {
        DataInputStream message_stream = new DataInputStream(new ByteArrayInputStream(message));
        Map<TransmitterInfo, Float[]> resultsMap = new HashMap<TransmitterInfo, Float[]>();
        try {
            while (message_stream.available() > 0) {
                int region_id = message_stream.readInt();
                MACAddress device_address = MACAddress.getMACAddress(message_stream.readLong());
                int phy = message_stream.readInt();
                long timestamp = message_stream.readLong();
                float x = message_stream.readFloat();
                float y = message_stream.readFloat();
                float x_prime = message_stream.readFloat();
                float y_prime = message_stream.readFloat();
                Float[] floats = new Float[] { x, y, x_prime, y_prime };
                TransmitterInfo transmitter = this.serverInfo.transmitterInfo.get(device_address);
                if (transmitter == null) {
                    transmitter = new TransmitterInfo(device_address, phy);
                    this.serverInfo.transmitterInfo.put(device_address, transmitter);
                }
                RegionInfo region = this.serverInfo.getRegion(region_id);
                if (region == null) {
                    System.err.println("ServerInterface could not find region with DB ID " + region_id);
                    continue;
                }
                transmitter.setPosition(region, new Point2D.Float(x, y));
                resultsMap.put(transmitter, floats);
            }
        } catch (IOException ioe) {
            System.err.println("Could not parse location information.");
            return false;
        }
        if (resultsMap.size() > 0) {
            this.fireServerEvent(new ResultEvent(this, resultsMap));
        }
        return true;
    }

    protected boolean handleFingerprint(byte[] message, byte fingerprint_type) {
        if (fingerprint_type != EventManager.MESSAGE_TYPE_FINGERPRINT_MEAN_GZIP && fingerprint_type != EventManager.MESSAGE_TYPE_FINGERPRINT_STDEV_GZIP) {
            System.err.println("Invalid fingerprint type received.  Ignoring data.");
            return false;
        }
        ByteArrayOutputStream uncompressed = new ByteArrayOutputStream();
        try {
            GZIPInputStream unzipStream = new GZIPInputStream(new ByteArrayInputStream(message));
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = unzipStream.read(buffer, 0, buffer.length)) > 0) {
                uncompressed.write(buffer, 0, read);
            }
        } catch (IOException ioe) {
            System.err.println("Couldn't decompress fingerprint.");
            return false;
        }
        if (uncompressed.size() == 0) {
            System.err.println("No data decompressed.");
            return false;
        }
        DataInputStream message_stream = new DataInputStream(new ByteArrayInputStream(uncompressed.toByteArray()));
        try {
            while (message_stream.available() > 0) {
                int region_id = message_stream.readInt();
                MACAddress device_mac = MACAddress.getMACAddress(message_stream.readLong());
                int phy = message_stream.readInt();
                long timestamp = message_stream.readLong();
                int total_landmarks = message_stream.readInt();
                TransmitterInfo transmitterInfo = this.serverInfo.transmitterInfo.get(device_mac);
                if (transmitterInfo == null) {
                    transmitterInfo = new TransmitterInfo(device_mac, phy);
                    this.serverInfo.transmitterInfo.put(device_mac, transmitterInfo);
                }
                byte decompressedType = fingerprint_type == EventManager.MESSAGE_TYPE_FINGERPRINT_MEAN_GZIP ? EventManager.MESSAGE_TYPE_FINGERPRINT_MEAN : EventManager.MESSAGE_TYPE_FINGERPRINT_STDEV;
                FingerprintInfo fingerprintInfo = new FingerprintInfo(transmitterInfo, decompressedType, -1l, -1l);
                int landmarks = 0;
                while (landmarks++ < total_landmarks) {
                    MACAddress hub_mac = MACAddress.getMACAddress(message_stream.readLong());
                    int hub_phy = message_stream.readInt();
                    int antenna = message_stream.readInt();
                    float rssi = message_stream.readFloat();
                    HubInfo temp_hub = this.serverInfo.hubInfo.get(hub_mac);
                    if (temp_hub == null) {
                        System.err.println("Couldn't find hub " + hub_mac.toString());
                        return true;
                    }
                    LandmarkInfo temp_landmark = temp_hub.getLandmarkInfo(hub_phy, antenna);
                    if (temp_landmark == null) {
                        System.err.println("Couldn't find landmark " + hub_mac.toString() + "/" + hub_phy + "/" + antenna);
                        return true;
                    }
                    fingerprintInfo.setRSSIValue(temp_landmark, rssi);
                }
                transmitterInfo.setFingerprintInfo(fingerprintInfo);
                if (fingerprintInfo.size() != 0) this.fireServerEvent(new FingerprintEvent(this, fingerprintInfo));
            }
        } catch (IOException ioe) {
            System.err.println(ioe.getLocalizedMessage());
            ioe.printStackTrace();
            System.err.println("Bad fingerprint data.");
            return false;
        }
        return true;
    }

    protected boolean handleStatistics(byte[] message) {
        DataInputStream message_stream = new DataInputStream(new ByteArrayInputStream(message));
        try {
            while (message_stream.available() > 0) {
                long timestamp = message_stream.readLong();
                this.serverInfo.lastUpdate = timestamp;
                float server_bps = message_stream.readFloat();
                this.serverInfo.serverReceiveBPS = server_bps;
                float server_sps = message_stream.readFloat();
                this.serverInfo.serverReceiveSPS = server_sps;
                int total_hubs = message_stream.readInt();
                int num_hubs = 0;
                while (num_hubs++ < total_hubs) {
                    MACAddress hub_mac = MACAddress.getMACAddress(message_stream.readLong());
                    HubInfo hubInfo = this.serverInfo.hubInfo.get(hub_mac);
                    if (hubInfo == null) {
                        hubInfo = new HubInfo(hub_mac, null, this.serverInfo);
                        this.serverInfo.hubInfo.put(hub_mac, hubInfo);
                    }
                    int total_landmarks = message_stream.readInt();
                    int num_landmarks = 0;
                    while (num_landmarks++ < total_landmarks) {
                        int phy = message_stream.readInt();
                        int antenna = message_stream.readInt();
                        long lastSampleTime = message_stream.readLong();
                        float bytesPerSecond = message_stream.readFloat();
                        float samplesPerSecond = message_stream.readFloat();
                        LandmarkInfo landmarkInfo = null;
                        synchronized (hubInfo.landmarks) {
                            for (LandmarkInfo lmInfo : hubInfo.landmarks) {
                                if (lmInfo.physicalLayer == phy && lmInfo.antenna == antenna) {
                                    landmarkInfo = lmInfo;
                                    break;
                                }
                            }
                        }
                        if (landmarkInfo == null) {
                            landmarkInfo = new LandmarkInfo(hubInfo, phy, antenna);
                            hubInfo.landmarks.add(landmarkInfo);
                        }
                        landmarkInfo.bytesPerSecond = bytesPerSecond;
                        landmarkInfo.samplesPerSecond = samplesPerSecond;
                        landmarkInfo.lastSampleTime = lastSampleTime;
                    }
                }
            }
        } catch (IOException ioe) {
            System.err.println(ioe.getLocalizedMessage());
            ioe.printStackTrace();
            System.err.println("Bad fingerprint data.");
            return false;
        }
        this.fireServerEvent(new StatisticsUpdatedEvent(this));
        return true;
    }

    protected boolean handleHubConnection(byte[] message) {
        DataInputStream messageInput = new DataInputStream(new ByteArrayInputStream(message));
        try {
            long connectedSince = messageInput.readLong();
            MACAddress hubID = MACAddress.getMACAddress(messageInput.readLong());
            boolean connected = messageInput.readByte() == (byte) 1 ? true : false;
            HubInfo hub = this.serverInfo.hubInfo.get(hubID);
            if (hub == null) {
                System.err.println("Could not find hub with id " + hubID);
                return false;
            }
            hub.connectedSince = connectedSince;
            hub.connected = connected;
            if (connected) {
                byte[] addressBytes = null;
                addressBytes = new byte[4];
                messageInput.read(addressBytes);
                InetAddress ipAddress = InetAddress.getByAddress(addressBytes);
                hub.setIPAddress(ipAddress);
            }
            this.fireServerEvent(new HubConnectionEvent(hub, connected));
        } catch (IOException ioe) {
            System.err.println(ioe.getLocalizedMessage());
            ioe.printStackTrace();
            System.err.println("Bad hub connection data.");
            return false;
        }
        return true;
    }

    protected static final String XPATH_EVENT = "/event";

    protected static final String XPATH_STAT = "/event/statistics";

    protected static final String XPATH_RESULT = "/event/result";

    protected static final String XPATH_FULL_INFO = "/event/fullinfo";

    protected static final String XPATH_FINGERPRINT = "/event/fingerprint";

    protected static final String XPATH_HUB = "/event/hub";

    protected String handleXML(String xmlString) {
        int xmlStart = xmlString.indexOf("<event");
        int xmlEnd = xmlString.indexOf("</event>");
        if (xmlStart == -1 || xmlEnd == -1) return xmlString;
        String returnString = xmlString.substring(0, xmlStart) + xmlString.substring(xmlEnd + 8);
        SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        Document xmlDocument;
        try {
            xmlDocument = saxBuilder.build(new StringReader(xmlString.substring(xmlStart, xmlEnd + 8)));
        } catch (IOException ioe) {
            System.err.println("Invalid XML?: " + xmlString);
            ioe.printStackTrace(System.err);
            return xmlString;
        } catch (JDOMException jdome) {
            System.err.println("Couldn't parse XML?: " + xmlString);
            jdome.printStackTrace(System.err);
            return xmlString;
        }
        XPath xPath = this.getXPath(XPATH_FULL_INFO);
        Element element = this.getElement(xPath, xmlDocument);
        if (element != null) {
            this.parseFullInfoXML(xmlDocument);
            return returnString;
        }
        return xmlString.trim();
    }

    protected static final String XPATH_FULL_INFO_REGION = "/event/fullinfo/region";

    protected static final String XPATH_FULL_INFO_HUB = "/event/fullinfo/hub";

    protected void parseFullInfoXML(Document xmlDocument) {
        XPath xPath = getXPath(XPATH_FULL_INFO);
        Element fullInfoElem = getElement(xPath, xmlDocument);
        if (fullInfoElem == null) {
            System.err.println("No full info element. Nothing to parse.");
            return;
        }
        xPath = getXPath(XPATH_EVENT);
        Element eventElem = getElement(xPath, xmlDocument);
        long timestamp = 0l;
        try {
            timestamp = Long.parseLong(eventElem.getAttributeValue("timestamp"));
        } catch (NumberFormatException nfe) {
            System.err.println("Could not parse timestamp for full info.");
        }
        xPath = getXPath(XPATH_FULL_INFO_REGION);
        List<Element> regionElems = getElements(xPath, xmlDocument);
        this.serverInfo.clearRegions();
        for (Element regionElem : regionElems) {
            String regionName = regionElem.getAttributeValue("name");
            if (regionName == null || regionName.length() == 0) {
                System.err.println("Missing region name!");
                return;
            }
            int databaseID = -1;
            try {
                databaseID = Integer.parseInt(regionElem.getAttributeValue("dbid"));
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid database ID for region " + regionName);
            }
            int units = Region.UNIT_UNDEFINED;
            try {
                units = Integer.parseInt(regionElem.getChildText("units"));
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid units format for region " + regionName);
            }
            if (units == Region.UNIT_UNDEFINED) continue;
            float xMin = -1;
            try {
                xMin = Float.parseFloat(regionElem.getChildText("xmin"));
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid x-min for " + regionName);
            }
            float xMax = -1;
            try {
                xMax = Float.parseFloat(regionElem.getChildText("xmax"));
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid x-max for " + regionName);
            }
            float yMin = -1;
            try {
                yMin = Float.parseFloat(regionElem.getChildText("ymin"));
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid y-min for " + regionName);
            }
            float yMax = -1;
            try {
                yMax = Float.parseFloat(regionElem.getChildText("ymax"));
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid y-max for " + regionName);
            }
            float zMin = -1;
            try {
                zMin = Float.parseFloat(regionElem.getChildText("zmin"));
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid z-min for " + regionName);
            }
            float zMax = -1;
            try {
                zMax = Float.parseFloat(regionElem.getChildText("zmax"));
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid z-max for " + regionName);
            }
            String imageURL = regionElem.getChildText("imageurl");
            RegionInfo regionInfo = new RegionInfo(regionName, databaseID, units, xMin, xMax, yMin, yMax, zMin, zMax, imageURL);
            this.serverInfo.addRegion(regionInfo);
        }
        xPath = getXPath(XPATH_FULL_INFO_HUB);
        List<Element> hubElems = getElements(xPath, xmlDocument);
        this.serverInfo.hubInfo.clear();
        for (Element hubElem : hubElems) {
            MACAddress hubID = null;
            try {
                hubID = MACAddress.getMACAddress(hubElem.getAttributeValue("id"));
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid id for hub.");
            }
            if (hubID == null) continue;
            String name = hubElem.getChildText("name");
            Element connectionElem = hubElem.getChild("connection");
            String connectionString = connectionElem.getAttributeValue("status");
            String connectedSinceString = connectionElem.getAttributeValue("since");
            long connectedSince = 0l;
            try {
                connectedSince = Long.parseLong(connectedSinceString);
            } catch (NumberFormatException nfe) {
                System.err.println("Invalid connected since value.");
            }
            boolean connected = connectionString.equalsIgnoreCase("true") ? true : false;
            String inetAddressString = connectionElem.getChildText("address");
            InetAddress hubAddress = null;
            if (inetAddressString != null) {
                try {
                    hubAddress = InetAddress.getByName(connectionElem.getChildText("address"));
                } catch (UnknownHostException uhe) {
                    System.err.println("Invalid hub address: " + connectionElem.getChildText("address"));
                }
            }
            HubInfo hubInfo = new HubInfo(hubID, name, this.serverInfo);
            hubInfo.connected = connected;
            hubInfo.setIPAddress(hubAddress);
            if (connected && connectedSince != 0l) hubInfo.connectedSince = connectedSince; else hubInfo.connectedSince = 0l;
            this.serverInfo.hubInfo.put(hubID, hubInfo);
            this.fireServerEvent(new HubConnectionEvent(hubInfo, hubInfo.connected));
            List<Element> landmarkElems = hubElem.getChildren("landmark");
            for (Element landmarkElem : landmarkElems) {
                int phy = Landmark.getPhyID(landmarkElem.getAttributeValue("phy"));
                if (phy == Landmark.PHY_UNDEFINED) {
                    System.err.println("Unrecognized physical layer type.");
                    continue;
                }
                int antenna = -1;
                try {
                    antenna = Integer.parseInt(landmarkElem.getAttributeValue("antenna"));
                } catch (NumberFormatException nfe) {
                    System.err.println("Unparseable antenna value.");
                }
                if (antenna == -1) {
                    System.err.println("Invalid antenna value.");
                    continue;
                }
                List<Element> positionElems = landmarkElem.getChildren("position");
                float x = 0f;
                float y = 0f;
                for (Element posElem : positionElems) {
                    String regionName = posElem.getAttributeValue("region");
                    if (regionName != null) {
                        RegionInfo region = this.serverInfo.getRegion(regionName);
                        if (region == null) {
                            System.err.println("Parsing full info XML: Could not find region " + regionName);
                            continue;
                        }
                        try {
                            x = Float.parseFloat(posElem.getAttributeValue("x"));
                        } catch (NumberFormatException nfe) {
                            System.err.println("Unparseable landmark x position.");
                        }
                        if (x == 0f) {
                            System.err.println("Invalid landmark x position.");
                            continue;
                        }
                        try {
                            y = Float.parseFloat(posElem.getAttributeValue("y"));
                        } catch (NumberFormatException nfe) {
                            System.err.println("Unparseable landmark y position.");
                        }
                        if (y == 0f) {
                            System.err.println("Invalid landmark y position.");
                            continue;
                        }
                        LandmarkInfo landmarkInfo = hubInfo.getLandmarkInfo(phy, antenna);
                        if (landmarkInfo == null) {
                            landmarkInfo = new LandmarkInfo(hubInfo, phy, antenna);
                            landmarkInfo.locations.put(region, new Point2D.Float(x, y));
                            hubInfo.landmarks.add(landmarkInfo);
                            this.fireServerEvent(new HubConnectionEvent(hubInfo, hubInfo.connected));
                        }
                    }
                }
            }
        }
        xPath = getXPath(XPATH_FULL_INFO_REGION);
        regionElems = getElements(xPath, xmlDocument);
        for (Element regionElem : regionElems) {
            String regionName = regionElem.getAttributeValue("name");
            if (regionName == null || regionName.length() == 0) {
                System.err.println("Missing region name!");
                return;
            }
            RegionInfo regionInfo = this.serverInfo.getRegion(regionName);
            List<Element> trainingElems = regionElem.getChildren("training");
            for (Element trainingElem : trainingElems) {
                String fileName = trainingElem.getAttributeValue("file");
                String name = trainingElem.getAttributeValue("name");
                TrainingInfo trainingInfo = new TrainingInfo(fileName, name, regionInfo);
                boolean devicePositionsRead = false;
                List<Element> landmarkElems = trainingElem.getChildren("landmark");
                for (Element landmarkElem : landmarkElems) {
                    MACAddress hubID = null;
                    try {
                        hubID = MACAddress.getMACAddress(landmarkElem.getAttributeValue("id"));
                    } catch (NumberFormatException nfe) {
                        System.err.println("Bad landmark id.");
                    }
                    if (hubID == null) continue;
                    int phy = Landmark.getPhyID(landmarkElem.getAttributeValue("phy"));
                    if (phy == Landmark.PHY_UNDEFINED) {
                        System.err.println("Bad landmark phy.");
                        continue;
                    }
                    int antenna = -1;
                    try {
                        antenna = Integer.parseInt(landmarkElem.getAttributeValue("ant"));
                    } catch (NumberFormatException nfe) {
                        System.err.println("Bad landmark antenna.");
                    }
                    if (antenna == -1) continue;
                    HubInfo hubInfo = this.serverInfo.hubInfo.get(hubID);
                    if (hubInfo == null) {
                        System.err.println("Training (" + name + ") couldn't find hub: " + hubID);
                        continue;
                    }
                    LandmarkInfo landmarkInfo = this.serverInfo.hubInfo.get(hubID).getLandmarkInfo(phy, antenna);
                    if (landmarkInfo == null) {
                        System.err.println("Couldn't find landmark.");
                        continue;
                    }
                    String xString = landmarkElem.getChildText("x");
                    float x = -1f;
                    try {
                        x = Float.parseFloat(xString);
                    } catch (NumberFormatException nfe) {
                        System.err.println("Bad training x coordinate.");
                    }
                    if (x == -1f) continue;
                    String yString = landmarkElem.getChildText("y");
                    float y = -1f;
                    try {
                        y = Float.parseFloat(yString);
                    } catch (NumberFormatException nfe) {
                        System.err.println("Bad training y coordinate.");
                    }
                    if (y == -1f) continue;
                    trainingInfo.landmarks.add(landmarkInfo);
                    trainingInfo.landmarkPositions.put(landmarkInfo, new float[] { x, y });
                    List<Element> rssiElems = landmarkElem.getChildren("rssi");
                    float[] rssis = new float[rssiElems.size()];
                    int index = 0;
                    for (Element rssiElem : rssiElems) {
                        float rssi = Float.NaN;
                        try {
                            rssi = Float.parseFloat(rssiElem.getText());
                        } catch (NumberFormatException nfe) {
                            System.err.println("Bad rssi value.");
                        }
                        if (!devicePositionsRead) {
                            float devX = -1f;
                            try {
                                devX = Float.parseFloat(rssiElem.getAttributeValue("x"));
                            } catch (NumberFormatException nfe) {
                                System.err.println("Bad device x-position.");
                            }
                            float devY = -1f;
                            try {
                                devY = Float.parseFloat(rssiElem.getAttributeValue("y"));
                            } catch (NumberFormatException nfe) {
                                System.err.println("Bad device y-position.");
                            }
                            trainingInfo.devicePositions.add(new float[] { devX, devY });
                        }
                        rssis[index++] = rssi;
                    }
                    trainingInfo.rssiValues.put(landmarkInfo, rssis);
                    devicePositionsRead = true;
                }
                regionInfo.trainingInfo.add(trainingInfo);
            }
        }
    }

    public static String toBPS(float bps) {
        String units = " B/s";
        if (bps > 1024) {
            bps /= 1024;
            units = " kB/s";
            if (bps > 1024) {
                bps /= 1024;
                units = " MB/s";
            }
        }
        return String.format("%1.1f" + units, bps);
    }

    public static String binaryUnits(float units) {
        String unitString = " B";
        if (units > 1024) {
            units /= 1024;
            unitString = " kB";
            if (units > 1024) {
                units /= 1024;
                unitString = " MB";
            }
        }
        return String.format("%1.1f" + unitString, units);
    }

    /**
	 * Converts an XPath string into an XPath object.
	 * 
	 * @param path
	 *            an String representing an XPath.
	 * @return the XPath object represented by {@code path}, or {@code null} if
	 *         the specified XPath is invalid.
	 */
    protected XPath getXPath(String path) {
        XPath xpath;
        try {
            xpath = XPath.newInstance(path);
        } catch (JDOMException jdome) {
            System.err.println("Could not create xpath for \"" + path + "\".");
            jdome.printStackTrace(System.err);
            return null;
        }
        return xpath;
    }

    /**
	 * Retrieves a list of XML elements from the specified document at the
	 * specified XPath.
	 * 
	 * @param xpath
	 *            the XPath to retrieve from the document.
	 * @param document
	 *            the XML document from which to retrieve the elements.
	 * @return a list of XML elements from the specified document/XPath, or
	 *         {@code null} if the XPath does not exist in the document.
	 */
    @SuppressWarnings("unchecked")
    protected List<Element> getElements(XPath xpath, Document document) {
        List<Element> elements;
        try {
            elements = xpath.selectNodes(document);
        } catch (JDOMException jdome) {
            System.err.println("Error while selecting elements. XPath: \"" + xpath.toString() + "\".");
            jdome.printStackTrace(System.err);
            return null;
        }
        return elements;
    }

    /**
	 * Retrieves an XML element from the specified document at the specified
	 * XPath.
	 * 
	 * @param xpath
	 *            the XPath to retrieve from the document.
	 * @param document
	 *            the XML document from which to retrieve the specified element.
	 * @return an XML element contained at the XPath within the document, or
	 *         {@code null} if it does not exist.
	 */
    protected Element getElement(XPath xpath, Document document) {
        Element element;
        try {
            element = (Element) xpath.selectSingleNode(document);
        } catch (JDOMException jdome) {
            System.err.println("Error while selecting an element. XPath: \"" + xpath.toString() + "\".");
            jdome.printStackTrace(System.err);
            return null;
        }
        return element;
    }

    private void startRecording() {
        if (this.recordingStream == true) return;
        if (this.fileStartTime == 0l) this.fileStartTime = System.currentTimeMillis();
        if (this.recordFile == null) {
            Calendar now = Calendar.getInstance();
            File savePath = new File("save");
            if (!savePath.exists()) savePath.mkdir();
            String fileName = "save" + File.separator + now.get(Calendar.YEAR) + String.format("%02d", now.get(Calendar.MONTH) + 1) + String.format("%02d", now.get(Calendar.DAY_OF_MONTH)) + "." + String.format("%02d", now.get(Calendar.HOUR_OF_DAY)) + String.format("%02d", now.get(Calendar.MINUTE)) + String.format("%02d", now.get(Calendar.SECOND)) + ".gcs";
            this.recordFile = new File(fileName);
            if (this.recordFile.exists()) {
                System.err.println("Save file already exists. Not creating.");
                this.stopRecording();
                return;
            }
            try {
                this.recordFile.createNewFile();
                if (!this.recordFile.canWrite()) {
                    System.err.println("Could not write to save file.");
                    this.stopRecording();
                    return;
                }
                this.saveStream = new DataOutputStream(new FileOutputStream(this.recordFile));
            } catch (IOException ioe) {
                System.err.println(ioe.getLocalizedMessage());
                ioe.printStackTrace(System.err);
                return;
            }
        }
        this.recordingStream = true;
    }

    private void stopRecording() {
        this.recordingStream = false;
        this.fileStartTime = 0l;
        if (this.saveStream != null) {
            try {
                this.saveStream.flush();
                this.saveStream.close();
            } catch (IOException ioe) {
                System.err.println("Could not close save stream.");
                System.err.println(ioe.getLocalizedMessage());
                ioe.printStackTrace(System.err);
            }
        }
        this.recordFile = null;
        this.saveStream = null;
    }

    private void recordMessage(byte[] message, byte messageType) {
        if (!this.recordingStream || this.saveStream == null) return;
        try {
            this.saveStream.writeLong(System.currentTimeMillis() - this.fileStartTime);
            this.saveStream.writeInt(message.length + 1);
            this.saveStream.writeByte(messageType);
            this.saveStream.write(message);
        } catch (IOException ioe) {
            System.err.println("Could not write message.");
            System.err.println(ioe.getLocalizedMessage());
            ioe.printStackTrace(System.err);
            this.stopRecording();
            return;
        }
    }
}
