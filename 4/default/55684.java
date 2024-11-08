import java.net.InetAddress;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import transport.AddPacketListenerCommand;
import transport.ConfigureLocalAddressCommand;
import transport.LocalAddress;
import transport.packet.Packet;
import jegg.EggBase;
import jegg.Port;
import jegg.PortException;
import jegg.timer.Timeout;

/**
 * 
 *
 * @author Bruce Lowery
 */
public class PrimeNumberClient extends EggBase {

    private static final Log LOG = LogFactory.getLog(PrimeNumberClient.class);

    private int SERVICE_TCP_PORT;

    private int MY_TCP_PORT;

    private Port networkPort;

    private int channelID;

    public void init() {
        MY_TCP_PORT = Integer.parseInt(getContext().getProperty("my-tcp-port"));
        SERVICE_TCP_PORT = Integer.parseInt(getContext().getProperty("server-tcp-port"));
        if (LOG.isInfoEnabled()) {
            LOG.info("MY_TCP_PORT: " + MY_TCP_PORT);
            LOG.info("SERVER_TCP_PORT: " + SERVICE_TCP_PORT);
        }
        LocalAddress la = new LocalAddress(null, MY_TCP_PORT);
        try {
            networkPort.send(getContext().createMessage(new ConfigureLocalAddressCommand(la)));
            networkPort.send(getContext().createMessage(new AddPacketListenerCommand(null, getContext().getPort())));
        } catch (PortException pe) {
            LOG.error("Failed to configure network address: ", pe);
        }
        getContext().createSingleShotTimer(5000);
    }

    public void handle(Port p) {
        String name = (String) p.getId();
        if (LOG.isDebugEnabled()) LOG.debug("handle(" + name + ")");
        if (name.endsWith("packet-transport")) {
            networkPort = p;
        }
    }

    public void handle(Object message) {
        LOG.warn("Unexpected message: " + message);
    }

    public void handle(Packet p) {
        Long el = (Long) p.getPayload();
        LOG.info("Next prime number: " + el);
        channelID = p.getChannelID();
        getContext().createSingleShotTimer(1000);
    }

    public void handle(Timeout t) {
        LOG.debug("TIMEOUT");
        LOG.debug("Sending request packet");
        Packet p = new Packet(null, null);
        if (0 == channelID) {
            try {
                p.setIP(InetAddress.getLocalHost());
                p.setPort(SERVICE_TCP_PORT);
            } catch (Throwable th) {
                LOG.error("Unable to send request", th);
                return;
            }
        } else {
            p.setChannelID(channelID);
        }
        try {
            networkPort.send(getContext().createMessage(p));
        } catch (PortException e) {
            LOG.error("Failed to send request packet: ", e);
        }
    }
}
