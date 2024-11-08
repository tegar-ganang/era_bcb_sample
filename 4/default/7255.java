import javax.microedition.midlet.*;
import javax.bluetooth.*;
import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import java.util.*;
import java.io.*;

/**
 * @author ueffel
 */
public class BTTest extends MIDlet implements CommandListener, DiscoveryListener, Runnable {

    private Display d;

    private Alert a;

    private Command c1, c2;

    private LocalDevice l;

    private DiscoveryAgent agent;

    private Vector devices;

    private Vector services;

    private final Object lock = new Object();

    private boolean Server;

    private final char EndOfStream = 27;

    private InputStream in = null;

    private OutputStream out = null;

    public BTTest() {
        d = Display.getDisplay(this);
        devices = new Vector();
        services = new Vector();
    }

    public void startApp() {
        a = new Alert("");
        a.setString("Grüß dich!");
        c1 = new Command("Erstellen", Command.ITEM, 1);
        c2 = new Command("Suchen", Command.ITEM, 2);
        a.addCommand(c1);
        a.addCommand(c2);
        a.setTimeout(Alert.FOREVER);
        a.setCommandListener(this);
        d.setCurrent(a);
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    public void commandAction(Command cmd, Displayable d) {
        if (cmd.getLabel().equals("Erstellen")) {
            Server = true;
            new Thread(this).start();
        } else if (cmd.getLabel().equals("Suchen")) {
            Server = false;
            new Thread(this).start();
        } else {
        }
    }

    public void beServer(StreamConnection connection) {
        try {
            RemoteDevice rd = RemoteDevice.getRemoteDevice(connection);
            println("Client: " + "(" + rd.getBluetoothAddress() + ")");
            System.out.println("Client: " + "(" + rd.getBluetoothAddress() + ")");
            println("Warte auf Client");
            System.out.println("Warte auf Client");
            String clientmsg = receive(connection);
            System.out.println(clientmsg);
            println(clientmsg);
            String answer;
            if (clientmsg.equals("GetQ")) {
                System.out.println("Sende frage!");
                println("Sende frage!");
                char sep = 29;
                send(connection, "1" + sep + "Wie findest du BlauWahl?" + sep + "(Zeit)" + sep + "Toll!" + sep + "naja geht." + sep + "zum kotzen!" + sep);
            } else if (clientmsg.equals("GiveA")) {
                System.out.println("Empfange Antwort");
                println("Empfange Antwort");
                answer = receive(connection);
                System.out.println("Antwort: " + answer);
                println("Antwort: " + answer);
            }
            connection.close();
            println("Connection closed");
            System.out.println("Connection closed");
            connection = null;
            in = null;
            out = null;
        } catch (BluetoothStateException e) {
            println("BluetoothStateException geschmissen");
        } catch (IOException ee) {
            println("AHH IOException jeschmissen!!");
        }
    }

    public void beClient() {
        try {
            devices.removeAllElements();
            services.removeAllElements();
            l = LocalDevice.getLocalDevice();
            println("Ich bin " + l.getFriendlyName() + "(" + l.getBluetoothAddress() + ")");
            l.setDiscoverable(DiscoveryAgent.GIAC);
            println("Suche nach Geräten");
            agent = l.getDiscoveryAgent();
            agent.startInquiry(DiscoveryAgent.GIAC, this);
            synchronized (lock) {
                lock.wait();
            }
            println("Suche nach Services");
            int devCount = devices.size();
            UUID[] uuidSet = { new UUID("F0E0D0C0B0A000908070605040302010", false) };
            for (int i = 0; i < devCount; i++) {
                agent.searchServices(null, uuidSet, (RemoteDevice) devices.elementAt(i), this);
                synchronized (lock) {
                    lock.wait();
                }
            }
            ServiceRecord srvrec;
            String conurl;
            if (!services.isEmpty()) {
                srvrec = (ServiceRecord) services.elementAt(0);
                conurl = srvrec.getConnectionURL(0, false);
            } else {
                System.out.println("Keine services");
                println("Keine services");
                return;
            }
            StreamConnection streamcon = (StreamConnection) Connector.open(conurl, Connector.READ_WRITE, true);
            System.out.println("will antwort geben");
            println("will antwort geben");
            send(streamcon, "GiveA");
            System.out.println("Gebe Antwort!");
            println("Gebe Antwort!");
            send(streamcon, "Antwort!!!");
            streamcon.close();
            println("Connection closed");
            System.out.println("Connection closed");
            streamcon = null;
            in = null;
            out = null;
        } catch (BluetoothStateException e) {
            System.out.println("BluetoothStateException geschmissen");
            println("BluetoothStateException geschmissen");
        } catch (IOException ee) {
            System.out.println("AHH IOException jeschmissen!!");
            println("AHH IOException jeschmissen!!");
        } catch (Exception ee) {
            System.out.println(ee + "");
            println(ee + "");
        }
    }

    public String receive(StreamConnection con) throws IOException {
        if (in == null) {
            in = con.openInputStream();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read;
        while ((read = in.read()) != -1 && read != this.EndOfStream) {
            out.write(read);
            System.out.println(new String(out.toByteArray(), "UTF-8"));
        }
        System.out.println("Debug1");
        String result = new String(out.toByteArray(), "UTF-8");
        out.close();
        return result;
    }

    public void send(StreamConnection con, String msg) throws IOException {
        if (out == null) {
            out = con.openOutputStream();
            out.flush();
        }
        byte[] b = (msg + EndOfStream).getBytes("UTF-8");
        out.write(b);
        System.out.println(msg + " gesendet");
    }

    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
        for (int i = 0; i < servRecord.length; i++) {
            if (!services.contains(servRecord[i])) {
                services.addElement(servRecord[i]);
            }
            System.out.println("Service gefunden.");
            println("Service gefunden.");
            synchronized (lock) {
                lock.notify();
            }
        }
    }

    public void serviceSearchCompleted(int transID, int respCode) {
        switch(respCode) {
            case SERVICE_SEARCH_COMPLETED:
                {
                    System.out.println("Service search completed");
                    break;
                }
            case SERVICE_SEARCH_TERMINATED:
                {
                    System.out.println("Service search terminated");
                    break;
                }
            case SERVICE_SEARCH_ERROR:
                {
                    System.out.println("Service search error");
                    break;
                }
            case SERVICE_SEARCH_NO_RECORDS:
                {
                    System.out.println("Service search no records");
                    break;
                }
            case SERVICE_SEARCH_DEVICE_NOT_REACHABLE:
                {
                    System.out.println("Service search not reachable");
                    break;
                }
            default:
                {
                    System.out.println("Service search, komische sachen oO");
                    break;
                }
        }
        synchronized (lock) {
            lock.notify();
        }
    }

    public void inquiryCompleted(int discType) {
        switch(discType) {
            case INQUIRY_COMPLETED:
                {
                    System.out.println("Inquiry completed");
                    break;
                }
            case INQUIRY_ERROR:
                {
                    System.out.println("Inquiry error");
                    break;
                }
            case INQUIRY_TERMINATED:
                {
                    System.out.println("Inquiry terminated");
                    break;
                }
            default:
                {
                    System.out.println("Inquiry, Komsiche Sache passiert");
                    break;
                }
        }
        synchronized (lock) {
            lock.notify();
        }
    }

    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        if (!devices.contains(btDevice)) {
            devices.addElement(btDevice);
        }
        System.out.println("Gerät gefunden: " + btDevice.getBluetoothAddress());
        println("Gerät gefunden: " + btDevice.getBluetoothAddress());
    }

    public void println(String msg) {
        a.setString(a.getString() + "\n" + msg);
    }

    public void run() {
        if (Server) {
            try {
                l = LocalDevice.getLocalDevice();
                println("Ich bin " + l.getFriendlyName() + "(" + l.getBluetoothAddress() + ")");
                l.setDiscoverable(DiscoveryAgent.GIAC);
                UUID uuid = new UUID("F0E0D0C0B0A000908070605040302010", false);
                String connectionString = "btspp://localhost:" + "F0E0D0C0B0A000908070605040302010" + ";name=kranker Bluetoothtest";
                println("Starte Service @" + uuid);
                System.out.println("Starte Service @\n(1000) -> " + uuid + " hash: " + uuid.hashCode());
                StreamConnectionNotifier streamConnNotifier = (StreamConnectionNotifier) Connector.open(connectionString, Connector.READ_WRITE, true);
                while (true) {
                    println("Service gestarted, warte auf Clients");
                    System.out.println("Service gestarted, warte auf Clients");
                    StreamConnection connection = streamConnNotifier.acceptAndOpen();
                    beServer(connection);
                }
            } catch (Exception e) {
                System.out.println("Exception -> " + e);
                println("Exception -> " + e);
            }
        } else {
            beClient();
        }
    }
}
