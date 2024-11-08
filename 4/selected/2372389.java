package btaddon;

import btaddon.MediaForm.Media;
import javax.bluetooth.UUID;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DataElement;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.ServiceRegistrationException;
import javax.bluetooth.BluetoothStateException;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import javax.microedition.io.Connector;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import java.util.Vector;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BtServer extends Thread {

    public static final String myServiceUUID = "eadd9d5be3ef44edb78f25e0308ae3b5";

    protected static String servURL = "btspp://localhost:" + myServiceUUID + ";name=" + Media.nick_me + ";authorize=false";

    private LocalDevice localDevice;

    private ServiceRecord record;

    public StreamConnectionNotifier scn;

    private StreamConnection sc;

    private boolean stop;

    protected static RemoteDevice _rd;

    private static DataInputStream dataIn;

    private static DataOutputStream dataOut;

    private static Menu menu;

    private static Vector s_Clients;

    public static final int maxClients = Media.maxNodes;

    private static int clients_count;

    public static SingleClient active_client = null;

    public static String auxserver_btaddr;

    private static final DataElement fullAvail = new DataElement(DataElement.U_INT_1, 0x02), stillAvail = new DataElement(DataElement.U_INT_1, 0x01), noneAvail = new DataElement(DataElement.U_INT_1, 0x00);

    public BtServer(Menu m) {
        super();
        menu = m;
        localDevice = null;
        clients_count = 1;
        s_Clients = new Vector(maxClients);
        stop = false;
        auxserver_btaddr = "";
    }

    public void run() {
        try {
            localDevice = LocalDevice.getLocalDevice();
            active_client = new SingleClient(null, null, menu);
            active_client.name = Media.nick_me;
            active_client.formats = Media.format_all;
            s_Clients.addElement(active_client);
            active_client.btaddr = localDevice.getBluetoothAddress();
            active_client = null;
            localDevice.setDiscoverable(DiscoveryAgent.GIAC);
        } catch (BluetoothStateException bse) {
            Alert al = new Alert("Error", "BluetoothStateException: " + bse.getMessage(), null, AlertType.ERROR);
            menu.FormExchange(1);
            menu.FormShow(al);
        }
        try {
            scn = (StreamConnectionNotifier) Connector.open(servURL);
        } catch (IOException ioe) {
            Alert al = new Alert("Error", "IOException: " + ioe.getMessage(), null, AlertType.ERROR);
            menu.FormExchange(-1);
            menu.FormShow(al);
        }
        acceptClientConnections();
    }

    public SingleClient findClient(String name) {
        SingleClient tmp;
        for (int i = 0; i < s_Clients.size(); i++) {
            tmp = (SingleClient) s_Clients.elementAt(i);
            if (tmp.name.equals(name)) return tmp;
        }
        return null;
    }

    public String[] getNodeNames() {
        int size = 0;
        SingleClient tmp;
        String[] temp = new String[s_Clients.size() - 1];
        for (int i = 1; i < s_Clients.size(); i++) {
            tmp = (SingleClient) s_Clients.elementAt(i);
            temp[size++] = new String(tmp.name);
        }
        return temp;
    }

    private void setNewClient(DataInputStream dI, DataOutputStream dO, String cl_name, String btaddr) {
        SingleClient tmp = new SingleClient(dI, dO, menu);
        tmp.name = cl_name;
        tmp.btaddr = btaddr;
        if (auxserver_btaddr.equals("")) auxserver_btaddr = btaddr;
        s_Clients.addElement(tmp);
        tmp.last_sam = System.currentTimeMillis();
        tmp.start();
    }

    public void killClient(String name) {
        SingleClient sc = findClient(name);
        String btad = sc.btaddr;
        sc.end();
        s_Clients.removeElement(sc);
        if (btad.equals(auxserver_btaddr)) {
            btad = getNodeNames()[0];
            sc = findClient(btad);
            auxserver_btaddr = sc.btaddr;
        }
        --clients_count;
    }

    public void endClientConnections() {
        int temp;
        SingleClient tmp;
        for (int i = 0; i < s_Clients.size(); i++) {
            tmp = (SingleClient) s_Clients.elementAt(i);
            s_Clients.removeElementAt(i);
            tmp.end();
            tmp = null;
        }
    }

    private void UpdateServiceAvail(int addsubclient) {
        DataElement currElem;
        clients_count += addsubclient;
        if (clients_count > maxClients) clients_count = maxClients; else if (clients_count < 0) clients_count = 0;
        switch(clients_count) {
            case 7:
                currElem = noneAvail;
                break;
            case 6:
            case 5:
            case 4:
            case 3:
            case 2:
            case 1:
                currElem = stillAvail;
                break;
            case 0:
                currElem = fullAvail;
                break;
            default:
                currElem = noneAvail;
                break;
        }
        record.setAttributeValue(0x0008, currElem);
        try {
            localDevice.updateRecord(record);
        } catch (ServiceRegistrationException sre) {
            Alert al = new Alert("Error", "ServiceRegistrationException: " + sre.getMessage(), null, AlertType.ERROR);
            core.mobber.display.setCurrent(al, menu.form);
        }
    }

    public void updateList(String name, String[] inc_formats) {
        SingleClient tmp;
        int len = 0;
        if (inc_formats == null) killClient(name); else len = inc_formats.length;
        for (int i = 1; i < s_Clients.size(); i++) {
            tmp = (SingleClient) s_Clients.elementAt(i);
            tmp.btSend("update");
            tmp.btSend(len);
            for (int j = 0; j < len; j++) tmp.btSend(inc_formats[j]);
            tmp.btSend(name);
            tmp.btSend(auxserver_btaddr);
        }
    }

    private void acceptClientConnections() {
        String name;
        record = localDevice.getRecord(scn);
        record.setDeviceServiceClasses(0x40000);
        UpdateServiceAvail(0);
        while (!stop) {
            try {
                sc = scn.acceptAndOpen();
                if (clients_count > maxClients) continue;
                _rd = RemoteDevice.getRemoteDevice(sc);
                dataIn = sc.openDataInputStream();
                dataOut = sc.openDataOutputStream();
                name = dataIn.readUTF();
                if (findClient(name) != null) {
                    dataOut.writeUTF("name_already_chosen");
                    dataOut.flush();
                    dataOut.close();
                    continue;
                } else {
                    dataOut.writeUTF("send_server_name");
                    dataOut.writeUTF(Media.nick_me);
                    dataOut.flush();
                }
            } catch (IOException ioe) {
                Alert al = new Alert("Error", "IOException: " + ioe.getMessage(), null, AlertType.ERROR);
                menu.FormExchange(-1);
                menu.FormShow(al);
                return;
            }
            UpdateServiceAvail(1);
            setNewClient(dataIn, dataOut, name, _rd.getBluetoothAddress());
            menu.insertNodeNames(getNodeNames());
            Media.ticker.setString("Client connected");
        }
    }

    public void clientActive() {
        active_client = findClient(Media.nick_ot);
        menu.insertFormats(active_client.formats);
    }

    public void end() {
        try {
            stop = true;
            endClientConnections();
            if (localDevice != null) localDevice.setDiscoverable(DiscoveryAgent.NOT_DISCOVERABLE);
            if (scn != null) scn.close();
            if (sc != null) sc.close();
        } catch (BluetoothStateException bse) {
            Alert al = new Alert("Error", "BluetoothStateError: " + bse.getMessage(), null, AlertType.ERROR);
            menu.FormShow(al);
        } catch (IOException ioe) {
            Alert al = new Alert("Error", "IOException: " + ioe.getMessage(), null, AlertType.ERROR);
            menu.FormShow(al);
        }
    }

    public void btSend(String name, final String snd) {
        if (name == null) active_client.btSend(snd); else {
            SingleClient sclient = findClient(name);
            sclient.btSend(snd);
        }
    }

    public void btSend(String name, final int snd) {
        if (name == null) active_client.btSend(snd); else {
            SingleClient sclient = findClient(name);
            sclient.btSend(snd);
        }
    }

    public void btSend(String name, final byte[] byteArray, String mime) {
        if (name == null) active_client.btSend(byteArray, mime); else {
            SingleClient sclient = findClient(name);
            sclient.btSend(byteArray, mime);
        }
    }
}
