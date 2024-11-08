package lt.baltic_amadeus.jqbridge.server;

import org.apache.log4j.Logger;
import lt.baltic_amadeus.jqbridge.msg.BridgeDestination;
import lt.baltic_amadeus.jqbridge.providers.ChannelHandler;
import lt.baltic_amadeus.jqbridge.providers.Port;

/** Virtual channel endpoint.
 * Contains information such as queue name so that a port can establish
 * a correct session on its side.
 * 
 * @author Baltic Amadeus, JSC
 * @author Antanas Kompanas
 *
 */
public class Endpoint {

    private static final Logger log = Logger.getLogger(Endpoint.class);

    public static final int SOURCE = 0;

    public static final int DESTINATION = 1;

    private VirtualChannel channel;

    private int side;

    private Port port;

    private BridgeDestination destination;

    private BridgeDestination replyTo;

    public Endpoint(VirtualChannel channel, int side) throws BridgeException {
        this.channel = channel;
        this.side = side;
        Server server = channel.getServer();
        Config conf = server.getConfig();
        String name = channel.getName();
        String pfx = "chan." + name + ".";
        String destKey;
        if (side == SOURCE) destKey = pfx + "from"; else destKey = pfx + "to";
        String destId = conf.getString(destKey);
        if (log.isDebugEnabled()) {
            log.debug("Channel " + name + " " + ((side == SOURCE) ? "from" : "to") + " " + destId);
        }
        if (destId == null || destId.equals("")) throw new ChannelConfigurationException(name, destKey + " is undefined or empty");
        int colon = destId.indexOf(':');
        if (colon < 0) throw new ChannelConfigurationException(name, destKey + " destination is not fully qualified");
        String portName = destId.substring(0, colon);
        String destName = destId.substring(colon + 1);
        port = server.getPort(portName);
        destination = new BridgeDestination(destName);
        do {
            if (side != DESTINATION) break;
            destKey = pfx + "reply";
            destId = conf.getString(destKey);
            if (destId == null || destId.equals("")) break;
            if (log.isDebugEnabled()) log.debug("Channel " + name + " will request replies to " + destId);
            colon = destId.indexOf(':');
            if (colon < 0) throw new ChannelConfigurationException(name, destKey + " destination is not fully qualified");
            String replyPortName = destId.substring(0, colon);
            String replyDestName = destId.substring(colon + 1);
            if (!replyPortName.equals(portName)) throw new ChannelConfigurationException(name, destKey + " destination port must match 'to'");
            replyTo = new BridgeDestination(replyDestName);
        } while (false);
    }

    public static String getMnemonic(int side) {
        switch(side) {
            case SOURCE:
                return "SOURCE";
            case DESTINATION:
                return "DESTINATION";
            default:
                return "UNKNOWN";
        }
    }

    /** Returns the channels this endpoint is part of.
	 * 
	 * @return virtual channel reference
	 */
    public VirtualChannel getChannel() {
        return channel;
    }

    /** Returns which side of a virtual channel this endpoint is on.
	 * 
	 * @return either <code>VirtualChannel.EP_SOURCE</code>
	 * or <code>VirtualChannel.EP_DESTINATION</code>.
	 * @see Endpoint#SOURCE
	 * @see Endpoint#DESTINATION
	 */
    public int getSide() {
        return side;
    }

    /** Returns the port of this channel endpoint.
	 * 
	 * @return port reference
	 */
    public Port getPort() {
        return port;
    }

    /** Returns a JMS destination description that this endpoint
	 * is associated with.
	 * @return BridgeDestination that describes a JMS queue or topic
	 */
    public BridgeDestination getDestination() {
        return destination;
    }

    /** Returns a destination that this endpoint will request replies to be sent to
	 * (DESTINATION endpoints only).
	 * @return BridgeDestination that is on the port of this Endpoint or null if replies will not be requested.
	 * @since 1.5 
	 */
    public BridgeDestination getReplyTo() {
        return replyTo;
    }

    /** Creates a channel handler for this endpoint.
	 * 
	 * @return a new ChannelHandler instance
	 * @throws BridgeException port throws this if it cannot create a channel handler
	 * @see Port#createChannelHandler(Endpoint)  
	 */
    public ChannelHandler createHandler() throws BridgeException {
        return port.createChannelHandler(this);
    }
}
