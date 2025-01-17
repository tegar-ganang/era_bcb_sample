package org.apache.axis.encoding.ser;

import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.encoding.DeserializationContext;
import org.apache.axis.attachments.OctetStream;
import org.apache.commons.logging.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import javax.activation.DataHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

/**
 * application/octet-stream DataHandler Deserializer
 * Modified by Davanum Srinivas <dims@yahoo.com>
 */
public class OctetStreamDataHandlerDeserializer extends JAFDataHandlerDeserializer {

    protected static Log log = LogFactory.getLog(OctetStreamDataHandlerDeserializer.class.getName());

    public void startElement(String namespace, String localName, String prefix, Attributes attributes, DeserializationContext context) throws SAXException {
        super.startElement(namespace, localName, prefix, attributes, context);
        if (getValue() instanceof DataHandler) {
            try {
                DataHandler dh = (DataHandler) getValue();
                InputStream in = dh.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int byte1 = -1;
                while ((byte1 = in.read()) != -1) baos.write(byte1);
                OctetStream os = new OctetStream(baos.toByteArray());
                setValue(os);
            } catch (IOException ioe) {
            }
        }
    }
}
