package jifx.connection.connector.ifx;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.log4j.Logger;
import jifx.commons.messages.IMessage;
import jifx.message.ifx.IFXPackager;

/**
 * This class writes data and sends an IFXMessage to the connected client. 
 *
 */
public class ConnectorWriter extends Thread {

    static Logger logger = Logger.getLogger(ConnectorIFX.class);

    private OutputStream out;

    private boolean activate;

    private IMessage buffer;

    private ConnectorIFX connector;

    public ConnectorWriter(OutputStream outputStream, ConnectorIFX connector) {
        out = outputStream;
        activate = true;
        buffer = null;
        this.connector = connector;
    }

    /**
	 * Method which generates an IXFMessage and writes it to the output stream in order to
	 * communicate it to the client. 
	 */
    public void run() {
        try {
            while (activate) {
                synchronized (this) {
                    if (buffer != null) {
                        String msg = (String) new IFXPackager().pack(buffer);
                        out.write(msg.getBytes());
                        buffer = null;
                    }
                }
            }
        } catch (IOException e) {
            logger.error(connector.getChannelName() + "| Problema de escritura, " + e.getMessage() + "|");
        }
        notify();
    }

    /**
	 * Return the state of the connector (active or not active)
	 */
    public boolean isActivate() {
        return activate;
    }

    /**
	 * Sets the state of the connector. 
	 */
    public void setActivate(boolean activate) {
        this.activate = activate;
    }

    /**
	 * Method which is invoked from the TryConnectThread in order to send a message (IFXMessage)
	 * to a specific client.  
	 */
    public void sendMessage(IMessage message) {
        synchronized (this) {
            buffer = message;
        }
    }
}
