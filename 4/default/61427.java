import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.StreamConnectionNotifier;
import bluetooth.BtUtilException;

public class PcktPC {

    public static String MODO = "Direta";

    private static LocalDevice localDevice;

    private static final UUID UUID = new javax.bluetooth.UUID("0000110100001000800000805F9B34FB", false);

    private static String url = "btspp://localhost:" + UUID.toString() + ";name=rfcommtest;authorize=false;authenticate=false";

    private static Logger logger;

    private static FileHandler fh;

    private static StreamConnectionNotifier strmNotf;

    private static Set<ConnectedThreadJava> threads;

    public static void main(String[] args) {
        threads = new HashSet<ConnectedThreadJava>();
        logger = Logger.getLogger("pckt.Test");
        try {
            fh = new FileHandler("Log.txt");
        } catch (Exception e) {
            logger.warning("Couldn't open file handler for logger: Main");
        }
        logger.addHandler(fh);
        if (!BTInicialization()) {
            System.exit(0);
        }
        ConnectingThreadJava connecting = new ConnectingThreadJava(strmNotf, threads, url);
        connecting.start();
    }

    public static void sendFile(ConnectedThreadJava thread, String name) {
        StorageHandler readHandler = new StorageHandler(name);
        readHandler.openRead();
        thread.write(readHandler.read());
    }

    public static Boolean BTInicialization() {
        try {
            localDevice = LocalDevice.getLocalDevice();
        } catch (BluetoothStateException e) {
            logger.warning("Couldn't set up LocalDevice");
            return false;
        }
        try {
            localDevice.setDiscoverable(DiscoveryAgent.GIAC);
        } catch (BluetoothStateException e) {
            logger.warning("Couldn't set device in discoverable mode");
            return false;
        }
        return true;
    }
}
