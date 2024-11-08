import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import jegg.EggBase;
import jegg.Port;
import jegg.PortException;
import transport.AddPacketListenerCommand;
import transport.ConfigureLocalAddressCommand;
import transport.LocalAddress;
import transport.packet.Packet;
import transport.packet.PacketTypeEnum;

/**
 * 
 *
 * @author Bruce Lowery
 */
public class PrimeNumberServer extends EggBase {

    private static final Log LOG = LogFactory.getLog(PrimeNumberServer.class);

    private static final int TCP_PORT = 10100;

    private Port networkPort;

    private long nextPrime = 1;

    public void handle(Port p) {
        String name = (String) p.getId();
        if (name.endsWith("packet-transport")) {
            networkPort = p;
        }
    }

    public void init() {
        if (null == networkPort) return;
        int port = Integer.parseInt(getContext().getProperty("tcp-port"));
        LocalAddress la = new LocalAddress(null, port);
        try {
            networkPort.send(getContext().createMessage(new ConfigureLocalAddressCommand(la)));
            networkPort.send(getContext().createMessage(new AddPacketListenerCommand(null, getContext().getPort())));
        } catch (PortException e) {
            LOG.error("Failed to configure network address: ", e);
        }
    }

    public void handle(Object message) {
        LOG.warn("Unexpected message: " + message);
    }

    public void handle(Packet p) {
        Packet response = new Packet(PacketTypeEnum.APPLICATION, new Long(getNextPrime()));
        response.setChannelID(p.getChannelID());
        try {
            networkPort.send(getContext().createMessage(response));
        } catch (PortException e) {
            LOG.error("Unable to send response packet: ", e);
        }
    }

    private long getNextPrime() {
        for (; ; ) {
            nextPrime += 2;
            boolean notPrime = false;
            for (long el = 2; el < nextPrime / 2; ++el) {
                if (0 == nextPrime % el) {
                    notPrime = true;
                    break;
                }
            }
            if (!notPrime) return nextPrime;
        }
    }
}
