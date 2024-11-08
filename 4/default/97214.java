import javax.microedition.lcdui.*;
import java.io.IOException;
import java.io.OutputStream;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import javax.microedition.lcdui.Form;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * A class that demonstrates Bluetooth communication between client mode PC and
 * server mode device through serial port profile. The example uses JSR-82 API.
 */
public class Bluetooth extends MIDlet {

    Displayxyz var = new Displayxyz(this);

    boolean waiting = true;

    OutputStream out;

    protected String UUID = new UUID("1101", true).toString();

    protected int discoveryMode = DiscoveryAgent.GIAC;

    Display display;

    protected Form infoArea = new Form("Bluetooth Server");

    protected void startApp() throws MIDletStateChangeException {
        display = Display.getDisplay(this);
        display.setCurrent(infoArea);
        infoArea.deleteAll();
        try {
            LocalDevice device = LocalDevice.getLocalDevice();
            device.setDiscoverable(DiscoveryAgent.GIAC);
            String url = "btspp://localhost:" + UUID + ";name=DeviceServerCOMM";
            log("Create server by uri: " + url);
            StreamConnectionNotifier notifier = (StreamConnectionNotifier) Connector.open(url);
            serverLoop(notifier);
        } catch (Throwable e) {
            log(e);
        }
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
        byte[] StopToken = { 100, 100, 100 };
        try {
            out.write(StopToken);
            out.flush();
            out.close();
        } catch (Exception e) {
        }
    }

    private void serverLoop(StreamConnectionNotifier notifier) {
        try {
            while (true) {
                log("Waiting for connection...");
                handleConnection(notifier.acceptAndOpen());
            }
        } catch (Exception e) {
            log(e);
        }
    }

    private synchronized void handleConnection(StreamConnection conn) throws IOException {
        out = conn.openOutputStream();
        log("connection open ready to write....");
        display.setCurrent(var);
        var.initSensor();
        try {
            while (waiting) wait();
        } catch (Exception e) {
            log(e);
        }
    }

    private synchronized void log(String msg) {
        infoArea.append(msg);
        infoArea.append("\n\n");
    }

    private void log(Throwable e) {
        log(e.getMessage());
    }
}
