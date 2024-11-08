package jifx.connection.connector.ifx;

import java.io.IOException;
import java.io.InputStream;
import jifx.message.ifx.IFXPackager;
import org.apache.log4j.Logger;

/**
 * This class reads data from client and recognizes an IFXMessage
 * through a stream of bytes.  
 */
public class ConnectorReader extends Thread {

    static Logger logger = Logger.getLogger(ConnectorIFX.class);

    private ConnectorIFX connector;

    private boolean activate;

    private InputStream in;

    private StringBuffer buffer;

    private byte[] byteBuffer;

    public ConnectorReader(InputStream inputStream, ConnectorIFX con) {
        connector = con;
        in = inputStream;
        byteBuffer = new byte[2048];
        buffer = new StringBuffer();
        activate = true;
    }

    /**
	 * Method in charge of reading the XML with the IFXMessage. It 'validates'
	 * the message when the label "</IFX>" is read. In this case we assume that
	 * all the message has been received correctly.   
	 * Then it converts this string into an IMessage. 
	 */
    public void run() {
        try {
            while (activate) {
                if (in.available() > 0) {
                    int numReads = in.read(byteBuffer);
                    buffer.append(new String(byteBuffer, 0, numReads));
                    int idx = buffer.indexOf("</IFX>");
                    if (idx >= 0) {
                        connector.onMessage(new IFXPackager().unpack(buffer.substring(0, idx + 6)));
                        idx = buffer.indexOf("<", idx + 7);
                        if (idx >= 0) buffer = new StringBuffer(buffer.substring(idx)); else buffer = new StringBuffer();
                    }
                }
            }
            logger.info(connector.getChannelName() + "| Reader IFX detenido.|");
        } catch (IOException e) {
            logger.error(connector.getChannelName() + "| Problema de lectura, " + e.getMessage() + "|");
        }
        notify();
    }

    /**
	 * Returns the state of this component. 
	 * @return
	 */
    public boolean isActivate() {
        return activate;
    }

    /**
	 * Sets the activation of this component. 
	 * @param activate
	 */
    public void setActivate(boolean activate) {
        this.activate = activate;
    }
}
