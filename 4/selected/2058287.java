package lt.baltic_amadeus.jqbridge.server;

import lt.baltic_amadeus.jqbridge.providers.Port;
import lt.baltic_amadeus.jqbridge.providers.ChannelHandler;

/**
 * 
 * @author Baltic Amadeus, JSC
 * @author Antanas Kompanas
 *
 */
public class CommunicationException extends BridgeException {

    private static final long serialVersionUID = 5786811420397294363L;

    public CommunicationException() {
        super("General bridge communications exception");
    }

    public CommunicationException(String message) {
        super(message);
    }

    public CommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommunicationException(Port port, Throwable cause) {
        super("Port " + port.getName() + " has communcation problems", cause);
    }

    public CommunicationException(ChannelHandler handler, Throwable cause) {
        super("Channel " + handler.getEndpoint().getChannel().getName() + " has communication problems on " + Endpoint.getMnemonic(handler.getEndpoint().getSide()) + " side", cause);
    }
}
