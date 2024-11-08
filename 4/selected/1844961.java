package jifx.connection.connector.iso;

import java.io.IOException;
import java.io.InputStream;
import org.apache.log4j.Logger;
import jifx.message.iso8583.ISOPackager;

/**
* This class reads data from client and recognizes an ISOMessage
* through a stream of bytes.  
*/
public class ConnectorReader extends Thread {

    static Logger logger = Logger.getLogger(ConnectorISO.class);

    private ConnectorISO connector;

    private boolean activate;

    private InputStream in;

    private StringBuffer buffer;

    private byte[] byteBuffer;

    public ConnectorReader(InputStream inputStream, ConnectorISO con) {
        connector = con;
        in = inputStream;
        byteBuffer = new byte[2048];
        buffer = new StringBuffer();
        activate = true;
    }

    /**
	 * Method in charge of reading the XML with the ISOMessage. It 'validates'
	 * the message when the label "</isomsg>" is read. In this case we assume that
	 * all the message has been received correctly.   
	 * Then it converts this string into an IMessage. 
	 */
    public void run() {
        try {
            while (activate) {
                if (in.available() > 0) {
                    int numReads = in.read(byteBuffer);
                    buffer.append(new String(byteBuffer, 0, numReads));
                    int idx = buffer.indexOf("</isomsg>");
                    if (idx >= 0) {
                        connector.onMessage(new ISOPackager().unpack(buffer.substring(0, idx + 9)));
                        idx = buffer.indexOf("<", idx + 10);
                        if (idx >= 0) buffer = new StringBuffer(buffer.substring(idx)); else buffer = new StringBuffer();
                    }
                }
            }
            logger.info(connector.getChannelName() + "| Reader ISO detenido.|");
        } catch (IOException e) {
            logger.error(connector.getChannelName() + "| Problema de lectura, " + e.getMessage() + "|");
        }
    }

    public boolean isActivate() {
        return activate;
    }

    public void setActivate(boolean activate) {
        this.activate = activate;
    }
}
