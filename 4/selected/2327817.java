package com.elibera.m.utils;

import javax.microedition.lcdui.Displayable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import com.elibera.m.app.MLE;
import com.elibera.m.events.HelperThread;
import com.elibera.m.events.ProgressBarThread;
import com.elibera.m.events.system.EventLink;
import com.elibera.m.rms.HelperRMSStoreMLibera;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import com.elibera.m.fileio.FileBrowserData;
import com.elibera.m.fileio.FileIOFilenameChooser;
import com.elibera.m.fileio.HelperFileIO;
import com.elibera.m.fileio.ProcFileBrowser;
import com.elibera.m.io.HelperServer;
import com.elibera.m.io.msg.HelperMsgServer;

/**
 * this is a helper class for bluetooth operations
 */
public class HelperBT {

    public static boolean bluetoothIsRunningAndIsDiscoverable = false;

    /**
	 * turns off the bluetooth stuff, only if it is running
	 *
	 */
    public static void turnOffBluetooth() {
        if (serverUrl == null) init();
        synchronized (serverUrl) {
            if (bluetoothIsRunningAndIsDiscoverable) {
                try {
                    LocalDevice ld = LocalDevice.getLocalDevice();
                    ld.setDiscoverable(DiscoveryAgent.NOT_DISCOVERABLE);
                    thread.stop();
                    if (mdl != null) mdl.stopInquirey();
                    mdl = null;
                    thread = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            bluetoothIsRunningAndIsDiscoverable = false;
        }
    }

    /**
	 * turns the discoverable mode on or off and activates the bluetooth internet server framework
	 */
    public static void turnOnBluetooth() {
        LocalDevice ld = null;
        ServiceRecord sr = null;
        try {
            if (serverUrl == null) init();
            System.out.println(serverUrl);
            synchronized (serverUrl) {
                if (bluetoothIsRunningAndIsDiscoverable) return;
                ld = LocalDevice.getLocalDevice();
                System.out.println(ld);
                System.out.println(ld.getFriendlyName() + "," + ld.getBluetoothAddress());
                ld.setDiscoverable(DiscoveryAgent.GIAC);
                StreamConnectionNotifier notifier = (StreamConnectionNotifier) Connector.open(serverUrl);
                sr = ld.getRecord(notifier);
                System.out.println(sr);
                System.out.println(notifier);
                MLE midlet = MLE.midlet;
                DataElement base = new DataElement(DataElement.DATSEQ);
                for (int i = 0; i < midlet.httpMLPServerNames.length; i++) {
                    DataElement plat = new DataElement(DataElement.DATSEQ);
                    plat.addElement(new DataElement(DataElement.STRING, "p" + midlet.httpMLPServerNames[i]));
                    plat.addElement(new DataElement(DataElement.STRING, "u" + HelperRMSStoreMLibera.getUsername(midlet.httpMLPServerNames[i])));
                    base.addElement(plat);
                }
                sr.setAttributeValue(SERVER_RECORD_MLE, base);
                ld.updateRecord(sr);
                int channel = getChannel(sr, 1);
                System.out.println("channel:" + channel);
                serverAddress = "btspp://" + ld.getBluetoothAddress() + ":" + channel;
                thread = new MyServerThread(notifier);
                System.out.println(thread);
                thread.start();
                bluetoothIsRunningAndIsDiscoverable = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int getChannel(ServiceRecord sr, int def) {
        try {
            Enumeration protocolDescriptorListElems = (Enumeration) (sr.getAttributeValue(0x0004)).getValue();
            protocolDescriptorListElems.nextElement();
            Enumeration deRFCOMMElems = (Enumeration) ((DataElement) protocolDescriptorListElems.nextElement()).getValue();
            deRFCOMMElems.nextElement();
            return HelperStd.parseInt("" + ((DataElement) deRFCOMMElems.nextElement()).getLong(), 1);
        } catch (Exception e) {
            e.printStackTrace();
            return def;
        }
    }

    /**
	 * 
	 * searches for devices<br/>
	 * method only avaliable on bluetooth devices !!!
	 * @param previousScreen
	 * @param mode types for the search: &lt;=0 we search for MLE devices; 1==MLE Internet Server; 2==every bluetooth device
	 * @param al will be called with a String-array containing: {connection url,name,channels,bluetooth-address}
	 * @param attrs only necessary for mode 2, see the method DiscoveryAgent.searchServices() of the bluetooth API for details
	 * @param uuid only necessary for mode 2, see the method DiscoveryAgent.searchServices() of the bluetooth API for details
	 */
    public static void searchForDevices(Displayable previousScreen, int mode, ActionListener al, int[] attrs, javax.bluetooth.UUID[] uuid) {
        try {
            turnOnBluetooth();
            deviceList = new List("Bluetooth", List.IMPLICIT);
            LocalDevice ld = LocalDevice.getLocalDevice();
            if (mdl == null) {
                DiscoveryAgent agent = ld.getDiscoveryAgent();
                mdl = new MyDiscoveryListener(agent);
            }
            if (uuid == null) {
                if (mode <= 0) {
                    attrs = attrsMLE;
                    uuid = uuidMLE;
                } else if (mode == 1) {
                    attrs = attrsInternet;
                    uuid = uuidInternet;
                } else {
                    if (al != null) al.errorOccured(0, "No UUIDs!", 0);
                    return;
                }
            }
            mdl.init(previousScreen, mode, al, attrs, uuid);
        } catch (Exception e) {
            e.printStackTrace();
            HelperApp.setErrorAlert("Couldn't start the Bluetooth-search!", e, mdl, null);
        }
    }

    /**
	 * sends a message to the given connection URL, blocks till the message has been send
	 * @param connectionUrl
	 * @param username
	 * @param platform
	 * @param msg
	 * @param msgTitel
	 * @throws Exception
	 */
    public static void sendMsg(String connectionUrl, String username, String platform, String msg, String msgTitel) throws Exception {
        turnOnBluetooth();
        StreamConnection conn = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            String myUsername = HelperRMSStoreMLibera.getUsername(null);
            conn = (StreamConnection) Connector.open(connectionUrl);
            in = conn.openInputStream();
            out = conn.openOutputStream();
            write(encoding, out);
            writeEncoded(MLE.midlet.httpMLPServerNames[0], out);
            writeEncoded(myUsername, out);
            writeEncoded(serverAddress, out);
            out.flush();
            String encoding = readLine(in);
            write("msg", out);
            if (msgTitel == null) msgTitel = myUsername;
            writeEncoded(msgTitel, out);
            writeEncoded(msg, out);
            out.flush();
        } catch (Exception e) {
            throw e;
        } finally {
            in = HelperStd.closeStream(in);
            out = HelperStd.closeStream(out);
            HelperStd.closeStream(conn);
            conn = null;
        }
    }

    /**
	 * stops all bluetooth tasks, including GPS mouse
	 *
	 */
    public static void stopBTTasks() {
        if (thread != null) thread.stop();
        if (mdl != null) mdl.stopInquirey();
        if (obexThread != null) obexThread.stopThread();
        obexThread = null;
        HelperLocation.stopGPSMouse();
    }

    /**
	 * searches for a GPS Mouse<br/>
	 * the actionListener will be called with an array containing the BT connection url and the display name
	 */
    public static void searchBTForGPSMaus(Displayable previousScreen, ActionListener al) {
        searchForDevices(previousScreen, 2, al, attrsGPSMaus, uuidGPSMaus);
    }

    private static void init() {
        if (serverUrl != null) return;
        serverUrl = "btspp://localhost:" + RFCOMM_UUID_MLE.toString() + ";name=MLE;authorize=false";
    }

    public static List deviceList;

    private static final int SERVER_RECORD_MLE = 0x4321;

    private static final int SERVER_RECORD_INERNET = 0x4321;

    private static UUID RFCOMM_UUID_MLE = new UUID("BEB2679F9AD640F298FADC8670404DFC", false);

    private static UUID RFCOMM_UUID_INTERNET = new UUID("2F966477866848A188F0C92CBCBB0BFC", false);

    private static int[] attrsMLE = { SERVER_RECORD_MLE };

    private static UUID[] uuidMLE = { RFCOMM_UUID_MLE };

    private static int[] attrsInternet = { SERVER_RECORD_INERNET };

    private static UUID[] uuidInternet = { RFCOMM_UUID_INTERNET };

    private static int[] attrsGPSMaus = { 0x4321 };

    private static UUID[] uuidGPSMaus = { new UUID(0x1101) };

    private static String serverUrl = null, serverAddress = null;

    private static MyServerThread thread;

    private static MyDiscoveryListener mdl;

    public static String[] httpMLPUserInfoURLs = {};

    /**
	 * the Server Thread that accepts the connections and manages them
	 * @author matthias
	 *
	 */
    private static class MyServerThread extends Thread {

        StreamConnectionNotifier notifier;

        boolean running = true;

        public MyServerThread(StreamConnectionNotifier _notifier) {
            notifier = _notifier;
        }

        public void run() {
            try {
                System.out.println("\n\nBluetooth Server Running...");
                while (running) {
                    if (notifier == null) return;
                    StreamConnection conn = null;
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        conn = notifier.acceptAndOpen();
                        in = conn.openInputStream();
                        out = conn.openOutputStream();
                        if (!running) break;
                        String encoding = readLine(in);
                        String platform = readEncoded(in, encoding);
                        String username = readEncoded(in, encoding);
                        String connectionUrl = readEncoded(in, encoding);
                        write(encoding, out);
                        out.flush();
                        String type = readLine(in);
                        if (type != null && type.compareTo("msg") == 0) {
                            String msgTitel = readEncoded(in, encoding);
                            String msg = readEncoded(in, encoding);
                            int pos = HelperStd.whereIsItInArray(MLE.midlet.httpMLPServerNames, platform);
                            if (pos < 0 || pos >= httpMLPUserInfoURLs.length) pos = 0;
                            msg = "Msg from:" + username + "<br><hr>Time:" + System.currentTimeMillis() + "<br>" + msg + "<br><hr>Reply: <textbox id=\"msg\"><br>" + "<button t=\"s\" action=\"settings|msgbtsend\" data=\"" + HelperStd.escapeTextForXML(connectionUrl + "|" + username + "|" + platform) + "\" w=\"Send\" di=\"13\" dt=\"l\">" + "<br><menu url=\"" + httpMLPUserInfoURLs[pos] + username + "\" t=\"m|" + platform + "\" w=\"User Info\">";
                            HelperMsgServer.processMsg("system", msgTitel, msg.getBytes(), -1, null, false);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        in = HelperStd.closeStream(in);
                        out = HelperStd.closeStream(out);
                        HelperStd.closeStream(conn);
                        conn = null;
                    }
                }
            } catch (Exception ex) {
                System.err.println("Bluetooth Server Running Error: " + ex);
            } finally {
                stop();
            }
        }

        public void stop() {
            running = false;
            try {
                notifier.close();
            } catch (Exception fe) {
            }
            notifier = null;
        }
    }

    /**
	 * searches for the devices and adds them to the list
	 * @author matthias
	 *
	 */
    private static class MyDiscoveryListener implements DiscoveryListener, CommandListener {

        Displayable previousScreen;

        DiscoveryAgent agent;

        String[] usernames, platformIDs, connectionUrl, serverIDs;

        MLE midlet = MLE.midlet;

        Command sendMsg = new Command("Send Msg", Command.OK, 1);

        Command userDetail = new Command("User Profile", Command.OK, 1);

        Command cmdSelect = new Command("Select", Command.OK, 1);

        Command refresh = new Command("Refresh", Command.OK, 1);

        boolean connectionOpen = false;

        boolean allDevices = false;

        boolean searchMle = false;

        ActionListener al;

        int[] attrs;

        UUID[] uuid;

        TextBox tb;

        StringBuffer debug = new StringBuffer();

        RemoteDevice[] foundDevices;

        ServiceRecord[][] foundServices;

        int[] servceSearchTransID, foundChannels;

        int curDevice = -1;

        int msgMode = 0;

        public MyDiscoveryListener(DiscoveryAgent _agent) {
            agent = _agent;
        }

        public void init(Displayable _previousScreen, int mode, ActionListener _al, int[] _attrs, UUID[] _uuid) throws Exception {
            stopInquirey();
            debug = new StringBuffer();
            deviceList.addCommand(midlet.abbrechen);
            deviceList.addCommand(refresh);
            connectionOpen = mode == 1;
            allDevices = mode == 2;
            searchMle = mode == 0;
            al = _al;
            attrs = _attrs;
            uuid = _uuid;
            deviceList.setCommandListener(this);
            midlet.setCurrent(deviceList);
            previousScreen = _previousScreen;
            deviceList.removeCommand(sendMsg);
            deviceList.removeCommand(userDetail);
            deviceList.removeCommand(cmdSelect);
            foundDevices = new RemoteDevice[0];
            foundChannels = null;
            deviceList.deleteAll();
            deviceList.append("Please wait ...", null);
            agent.startInquiry(DiscoveryAgent.GIAC, this);
        }

        public void stopInquirey() {
            try {
                agent.cancelInquiry(this);
                if (servceSearchTransID == null) return;
                synchronized (servceSearchTransID) {
                    if (servceSearchTransID == null) return;
                    for (int i = 0; i < servceSearchTransID.length; i++) {
                        if (servceSearchTransID[i] >= 0) agent.cancelServiceSearch(servceSearchTransID[i]);
                    }
                }
            } catch (Exception e) {
            }
        }

        public void clean() {
            stopInquirey();
            usernames = null;
            serverIDs = null;
            connectionUrl = null;
            foundDevices = null;
            foundServices = null;
            foundChannels = null;
        }

        public void commandAction(Command c, Displayable d) {
            if (c.equals(midlet.abbrechen)) {
                stopInquirey();
                clean();
                midlet.setCurrent(previousScreen);
            } else if (c.equals(refresh)) {
                stopInquirey();
                usernames = null;
                platformIDs = null;
                connectionUrl = null;
                serverIDs = null;
                searchForDevices(previousScreen, connectionOpen ? 1 : allDevices ? 2 : 0, al, attrs, uuid);
            } else if (c.equals(userDetail)) {
                int sel = deviceList.getSelectedIndex();
                int pos = HelperStd.whereIsItInArray(midlet.httpMLPServerNames, platformIDs[sel]);
                if (pos < 0 || pos >= httpMLPUserInfoURLs.length) return;
                String url = httpMLPUserInfoURLs[pos] + usernames[sel];
                System.out.println(url);
                EventLink ev = new EventLink('m', url, false, "m|" + platformIDs[sel]);
                ev.startThread(deviceList);
                clean();
            } else if (c.equals(sendMsg)) {
                int sel = deviceList.getSelectedIndex();
                System.out.println("Sending Message to:" + sel);
                System.out.println("Sending Message to:" + usernames[sel] + "," + platformIDs[sel] + "," + connectionUrl[sel]);
                if (msgMode != 2) {
                    tb = new TextBox("Send message to: " + usernames[sel], "", 5000, TextField.ANY);
                    tb.addCommand(sendMsg);
                    tb.setCommandListener(this);
                    midlet.display.setCurrent(tb);
                    msgMode = 2;
                } else {
                    midlet.display.setCurrent(deviceList);
                    msgMode = 0;
                    try {
                        sendMsg(connectionUrl[sel], usernames[sel], platformIDs[sel], tb.getString(), null);
                        HelperApp.setInfoAlert("Msg has been sent!", "Info", deviceList);
                    } catch (Exception e) {
                        e.printStackTrace();
                        HelperApp.setErrorAlert("Msg couldn't not be send!2 ", e, this, deviceList);
                    }
                    tb = null;
                }
            } else if (c.equals(cmdSelect)) {
                int sel = deviceList.getSelectedIndex();
                String name = deviceList.getString(sel);
                String url = connectionUrl[sel];
                if (connectionOpen) {
                    System.out.println("Open Connection to:" + sel + "," + url);
                    HelperServer.setProxyConnectionUrl(url);
                    try {
                        HelperRMSStoreMLibera.setDataValue("BTIT", url);
                        HelperRMSStoreMLibera.setDataValue("BTITN", name);
                    } catch (Exception e) {
                    }
                    stopInquirey();
                    midlet.setCurrent(previousScreen);
                    HelperApp.setInfoAlert("Proxy has been set to: " + url, "Info", previousScreen);
                } else {
                    if (al != null) al.doAction(0, new String[] { url, name, foundChannels[sel] + "", foundDevices[sel].getBluetoothAddress() });
                    stopInquirey();
                    midlet.setCurrent(previousScreen);
                    System.out.println("device has been selected:" + connectionUrl[sel]);
                }
                clean();
            }
        }

        public void deviceDiscovered(RemoteDevice d, DeviceClass dc) {
            try {
                for (int i = 0; i < foundDevices.length; i++) {
                    if (foundDevices[i] != null && foundDevices[i].equals(d)) return;
                }
                RemoteDevice[] n = new RemoteDevice[foundDevices.length + 1];
                if (foundDevices.length > 0) System.arraycopy(foundDevices, 0, n, 0, foundDevices.length);
                n[foundDevices.length] = d;
                foundDevices = n;
                debug.append(d.getFriendlyName(false) + ";");
                if (deviceList.size() <= 1) {
                    deviceList.append("Device[s] found", null);
                    deviceList.append("verifying ...", null);
                }
            } catch (Exception e) {
                e.printStackTrace();
                debug.append("EXCEPTION:" + e.getMessage() + ":" + e + ";");
            }
        }

        public void inquiryCompleted(int respCode) {
            if (respCode == INQUIRY_TERMINATED) return;
            servceSearchTransID = new int[foundDevices.length];
            foundServices = new ServiceRecord[foundDevices.length][];
            debug.append("foundDevices:" + foundDevices.length + "," + (respCode != INQUIRY_TERMINATED));
            if (foundDevices.length <= 0) processFoundServices();
            try {
                curDevice = 0;
                new Thread() {

                    public void run() {
                        try {
                            if (servceSearchTransID == null) return;
                            synchronized (servceSearchTransID) {
                                servceSearchTransID[0] = agent.searchServices(attrs, uuid, foundDevices[0], mdl);
                                debug.append(";ds:" + servceSearchTransID[0]);
                            }
                        } catch (Exception e) {
                            processFoundServices();
                        }
                    }
                }.start();
            } catch (Exception e) {
                e.printStackTrace();
                debug.append("EXCEPTION:" + e.getMessage() + ":" + e + ";");
            }
        }

        public void servicesDiscovered(int transID, ServiceRecord[] sr) {
            debug.append(";sr:" + transID + ":" + sr.length);
            foundServices[curDevice] = sr;
        }

        public void serviceSearchCompleted(int transID, int respCode) {
            debug.append(";sc:" + (respCode != SERVICE_SEARCH_TERMINATED));
            if (respCode == SERVICE_SEARCH_TERMINATED) return;
            curDevice++;
            new Thread() {

                public void run() {
                    try {
                        if (servceSearchTransID == null) return;
                        synchronized (servceSearchTransID) {
                            if (curDevice >= foundDevices.length) processFoundServices(); else {
                                servceSearchTransID[curDevice] = agent.searchServices(attrs, uuid, foundDevices[curDevice], mdl);
                                debug.append(";ds:" + servceSearchTransID[curDevice]);
                            }
                        }
                    } catch (Exception e) {
                        processFoundServices();
                    }
                }
            }.start();
        }

        /**
		 * wir durchsuchen jetzt alle service records, die wir gefunden haben
		 *
		 */
        private void processFoundServices() {
            String[] list = new String[0];
            for (int curDevice = 0; curDevice < foundDevices.length; curDevice++) {
                if (foundDevices[curDevice] == null || foundServices[curDevice] == null) continue;
                String serviceUrl = null, serverID = null;
                int channel = -1;
                try {
                    serverID = foundDevices[curDevice].getFriendlyName(false);
                    if (serverID == null) serverID = foundDevices[curDevice].getBluetoothAddress();
                } catch (Exception ee) {
                }
                if (serverID == null) serverID = "unkown";
                for (int i = 0; i < foundServices[curDevice].length; i++) {
                    ServiceRecord sr = foundServices[curDevice][i];
                    DataElement plat = attrs != null ? sr.getAttributeValue(attrs[0]) : null;
                    if (plat != null) debug.append(";pf"); else debug.append(";p0");
                    if (plat != null || serviceUrl == null) serviceUrl = sr.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                    if (plat != null || channel < 0) channel = getChannel(sr, -1);
                    if (searchMle && plat != null) {
                        Enumeration eplat = (Enumeration) plat.getValue();
                        while (eplat.hasMoreElements()) {
                            DataElement p1 = (DataElement) eplat.nextElement();
                            Enumeration eplat2 = (Enumeration) p1.getValue();
                            while (eplat2.hasMoreElements()) {
                                DataElement p2 = (DataElement) eplat2.nextElement();
                                String v = (String) p2.getValue();
                                char c = v.charAt(0);
                                v = v.substring(1);
                                if (c == 'p') platformIDs = HelperStd.incArray(platformIDs, v);
                                if (c == 'u') {
                                    usernames = HelperStd.incArray(usernames, v);
                                    connectionUrl = HelperStd.incArray(connectionUrl, serviceUrl);
                                    list = HelperStd.incArray(list, v + (serverID != null ? " (" + serverID + ")" : ""));
                                }
                            }
                        }
                    }
                }
                if (serviceUrl != null) foundChannels = HelperStd.incArray(foundChannels, channel);
                if ((connectionOpen || allDevices) && serviceUrl != null) {
                    serverIDs = HelperStd.incArray(serverIDs, serverID);
                    connectionUrl = HelperStd.incArray(connectionUrl, serviceUrl);
                    list = HelperStd.incArray(list, serverID);
                }
            }
            deviceList.deleteAll();
            if (list.length <= 0) {
                deviceList.append("No device found", null);
                HelperApp.setInfoAlert(debug.toString(), "search finished", deviceList);
            } else {
                if (searchMle) {
                    deviceList.addCommand(sendMsg);
                    deviceList.addCommand(userDetail);
                } else deviceList.addCommand(cmdSelect);
                for (int i = 0; i < list.length; i++) {
                    if (list[i] != null) deviceList.append(list[i], HelperApp.getSystemImage(HelperApp.IMAGE_APP_MAIL));
                }
            }
            stopInquirey();
        }
    }

    private static OBEXSender obexThread;

    /**
	 * sends a File via OBEX to a bluetooth device<br>
	 * only avaliable if the FileIO api is present
	 * @param al (optional) will be called if there are errors or , when we are finished
	 * @param previousScreen screen to go back to after the task
	 * @param fileUrl if null, we let the user select a file
	 * @param btURL if null we make a BT device search to find a device
	 */
    public static void sendFileToBTDevice(ActionListener al, Displayable previousScreen, String fileUrl, String btURL) {
        obexThread = new OBEXSender();
        obexThread.start(previousScreen, fileUrl, btURL, al);
    }

    /**
	 * this class sends a file to the client
	 * @author matthias
	 *
	 */
    private static class OBEXSender extends ProgressBarThread implements ActionListener, FileIOFilenameChooser {

        private String fileUrl, btUrl, btUrl2;

        private ActionListener al;

        FileBrowserData fb;

        Displayable prev;

        int mode = 0;

        public void start(Displayable _prev, String fileURL, String _btUrl, ActionListener _al) {
            fileUrl = fileURL;
            btUrl = _btUrl;
            al = _al;
            prev = _prev;
            action();
        }

        private void action() {
            if (fileUrl == null) {
                fb = new FileBrowserData(this, prev, null, true, false);
                ProcFileBrowser proc = (ProcFileBrowser) fb.proc;
                mode = 1;
                proc.openFileBrowser(fb, prev);
                return;
            }
            if (btUrl != null) {
                HelperThread.setProgressCanvas(this, "send", null, -1);
                this.startThread(prev);
                return;
            }
            mode = 2;
            HelperBT.searchForDevices(prev, 2, this, new int[] { 0x0004 }, new UUID[] { new UUID(0x1106) });
        }

        public Displayable setFilename(String name) {
            return null;
        }

        public void doAction(int taskCode, Object result) {
            if (mode == 1) {
                fileUrl = fb.fileName;
                action();
                return;
            }
            String[] parts = (String[]) result;
            btUrl = "btspp" + parts[0].substring(parts[0].indexOf(':'));
            btUrl2 = "btspp://" + parts[3] + ":" + parts[2] + ";master=false;encrypt=false;authenticate=false";
            action();
        }

        protected void doTask() throws Exception {
            StreamConnection cs = null;
            OutputStream os = null, obexOut = null;
            InputStream is = null;
            Integer ret = null;
            int mtu = -1;
            try {
                pb.setValueRePaint(1);
                try {
                    cs = (StreamConnection) Connector.open(btUrl);
                } catch (Exception ee) {
                }
                if (cs == null) cs = (StreamConnection) Connector.open(btUrl2);
                pb.setValueRePaint(2);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                os = cs.openOutputStream();
                is = cs.openInputStream();
                bos.write(0x10);
                bos.write(0);
                bos.write(new byte[] { 0x20, 0x00 });
                bos.write(new byte[0]);
                conID = -1;
                obexSendCommand(0x80, bos.toByteArray(), os);
                pb.setValueRePaint(3);
                byte[] bresp = obexReceiveCommand(is);
                pb.setValueRePaint(4);
                mtu = 0xffff & ((0xff & bresp[5]) << 8 | (0xff & bresp[6]));
                Object[] hs = parseHeaders(bresp, 7);
                int[] headerIds = (int[]) hs[0];
                for (int i = 0; i < headerIds.length; i++) {
                    if (headerIds[i] == 0xCB) conID = ((Long) ((Object[]) hs[1])[i]).longValue();
                }
                if (bresp[0] != (byte) 0xa0) throw new IOException("Connection not accepted ");
                bos.reset();
                bos.write(0xC3);
                writeLen(bos, HelperFileIO.getFileSize(fileUrl));
                bos.write(0x01);
                byte[] b = fileUrl.substring(fileUrl.lastIndexOf('/') + 1).getBytes("UTF-8");
                byte[] b2 = new byte[b.length * 2 + 2];
                for (int j = 0; j < b2.length; j++) b2[j] = 0;
                for (int j = 0; j < b.length; j++) b2[j * 2 + 1] = b[j];
                writeShortLen(bos, 3 + b2.length);
                bos.write(b2);
                byte[] headers = bos.toByteArray();
                obexSendCommand(0x02, headers, os);
                bresp = obexReceiveCommand(is);
                if (bresp[0] != (byte) 0x90 && bresp[0] != (byte) 0xa0) throw new IOException("Command not accepted ");
                OBEXPutOutputStream oos = new OBEXPutOutputStream();
                obexOut = oos;
                oos.is = is;
                oos.os = os;
                oos.mtu = mtu;
                HelperFileIO.writeFileData(fileUrl, oos, this.pb);
                obexSendCommand(0x82, new byte[] { 0x49, 0x00, 0x03 }, os);
                bresp = obexReceiveCommand(is);
                int respCode = (int) b[0] & 0xff;
                oos.flush();
                obexSendCommand(0x81, new byte[0], os);
                bresp = obexReceiveCommand(is);
                oos.flush();
                ret = new Integer(respCode);
                if (al == null) HelperApp.setInfoAlert("file has been sent:" + respCode, "success", prev);
            } catch (Exception e) {
                errorOccured(0, e + ":" + e.getMessage() + ":" + btUrl + "," + btUrl2, 0);
            } finally {
                HelperStd.closeStream(os);
                HelperStd.closeStream(is);
                HelperStd.closeStream(obexOut);
                HelperStd.closeStream(cs);
            }
            if (ret != null && al != null) al.doAction(1, ret);
        }

        public void errorOccured(int errorCode, String errorMsg, int taskCode) {
            if (al != null) al.errorOccured(errorCode, errorMsg, taskCode); else HelperApp.setErrorAlert(errorMsg, null, this, prev);
            obexThread = null;
        }

        protected void abortTask() {
        }

        private long conID = -1;

        private void obexSendCommand(int commId, byte[] data, OutputStream os) throws IOException {
            int len = 3 + data.length;
            if (conID != -1) len += 5;
            byte d2[] = new byte[len];
            d2[0] = (byte) commId;
            d2[1] = (byte) ((len >> 8) & 0xff);
            d2[2] = (byte) (len & 0xff);
            if (conID != -1) {
                d2[3] = (byte) 0xcb;
                d2[4] = (byte) (0xff & (conID >> 24));
                d2[5] = (byte) (0xff & (conID >> 16));
                d2[6] = (byte) (0xff & (conID >> 8));
                d2[7] = (byte) (0xff & (conID >> 0));
            }
            System.arraycopy(data, 0, d2, len - data.length, data.length);
            os.write(d2);
            os.flush();
        }

        private byte[] obexReceiveCommand(InputStream is) throws IOException {
            byte start[] = new byte[3];
            int read = 0;
            while (read < 3) {
                read += Math.max(0, is.read(start, read, 3 - read));
            }
            int toRead = 0xffff & (((start[1] & 0xff) << 8) | (start[2] & 0xff));
            byte[] data = new byte[toRead];
            System.arraycopy(start, 0, data, 0, 3);
            while (read < toRead) {
                read += Math.max(0, is.read(data, read, toRead - read));
            }
            return data;
        }

        private Object[] parseHeaders(byte[] data, int offset) {
            int[] a = new int[0];
            Object[] b = new Object[0];
            while (offset < data.length) {
                int id = (int) data[offset] & 0xff;
                if (id == 0xCB) {
                    a = HelperStd.incArray(a, id);
                    b = HelperStd.incArray(b, new Long(parseLong(data, offset + 1)));
                }
            }
            return new Object[] { a, b };
        }

        class OBEXPutOutputStream extends OutputStream {

            int mtu = 0, response;

            OutputStream os;

            InputStream is;

            public void write(int b) throws IOException {
                write(new byte[] { (byte) (b & 0xff) });
            }

            public void write(byte b[]) throws IOException {
                write(b, 0, b.length);
            }

            public synchronized void write(byte b[], int off, int len) throws IOException {
                byte[] hsba = new byte[0];
                while (len + 6 + hsba.length > mtu) {
                    write(b, off, mtu - 6 - hsba.length);
                    off += mtu - 6 - hsba.length;
                    len -= mtu - 6 - hsba.length;
                }
                byte[] d;
                d = new byte[len];
                System.arraycopy(b, off, d, 0, len);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bos.write(0x48);
                writeShortLen(bos, 3 + d.length);
                bos.write(d);
                byte[] b2 = bos.toByteArray();
                obexSendCommand(0x02, b2, os);
                d = obexReceiveCommand(is);
                if (d[0] != (byte) 0x90) throw new IOException("Error while sending PUT command " + Integer.toHexString(d[0] & 0xff));
                response = (int) d[0] & 0xff;
            }
        }

        private static long parseLong(byte data[], int offset) {
            long v = 0;
            for (int i = 0; i < 4; i++) {
                v = v << 8;
                v |= (int) (data[offset++] & 0xff);
            }
            return v;
        }

        private static void writeShortLen(ByteArrayOutputStream bos, int v) {
            byte[] b = new byte[2];
            b[0] = (byte) ((v >> 8) & 0xff);
            b[1] = (byte) (v & 0xff);
            try {
                bos.write(b);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private static void writeLen(OutputStream os, long v) {
            byte[] b = new byte[4];
            b[0] = (byte) ((v >> 24) & 0xff);
            b[1] = (byte) ((v >> 16) & 0xff);
            b[2] = (byte) ((v >> 8) & 0xff);
            b[3] = (byte) (v & 0xff);
            try {
                os.write(b);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String readEncoded(InputStream in, String encoding) throws IOException {
        int len = HelperStd.parseInt(readLine(in), 0);
        byte[] d = new byte[len];
        in.read(d);
        return new String(d, encoding);
    }

    private static int AVALIABLEWORKS = 0;

    public static String encoding = MLE.midlet.deviceEncoding;

    private static String readLine(InputStream is) throws IOException {
        System.out.println("readLine:" + Thread.currentThread());
        StringBuffer ret = new StringBuffer();
        int ch = 0, co = 0, doAvaliable = AVALIABLEWORKS;
        boolean istrue = true;
        while (istrue) {
            try {
                if (doAvaliable <= 1 && is.available() <= 0) {
                    co += 50;
                    if (co > 5000) doAvaliable = 2;
                    Thread.sleep(50);
                    continue;
                } else co = 0;
            } catch (Exception e) {
                istrue = false;
            }
            ch = is.read();
            if (ch <= -1) break;
            if ((char) ch == '\n' || (char) ch == '\r') break;
            ret.append((char) ch);
        }
        if (AVALIABLEWORKS == 0) {
            if (ret.length() > 0) {
                if (doAvaliable == 2) AVALIABLEWORKS = 2; else AVALIABLEWORKS = 1;
            }
        }
        System.out.println("readLine finished:" + AVALIABLEWORKS);
        return ret.toString();
    }

    private static void write(String out, OutputStream os) throws IOException {
        if (out == null) out = "";
        out = out.replace('\n', ' ');
        out = out.replace('\r', ' ');
        os.write(out.getBytes("ISO-8859-1"));
        os.write("\n".getBytes());
        HelperServer.write(out, os);
    }

    private static void writeEncoded(String out, OutputStream os) throws IOException {
        if (out == null) out = "";
        byte[] d = out.getBytes(encoding);
        write(d.length + "", os);
        os.write(d);
        HelperServer.write(out, os);
    }
}
