package org.apache.axiom.om.ds.custombuilder;

import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.ds.ByteArrayDataSource;
import org.apache.axiom.om.impl.builder.CustomBuilder;
import org.apache.axiom.om.impl.serialize.StreamingOMSerializer;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;

/**
 * CustomBuilder that creates an OMSourcedElement backed by a ByteArrayDataSource.
 * If you have a payload or header that will consume a lot of space, it 
 * may be beneficial to plug in this CustomBuilder.
 * 
 * Use this CustomBuilder as a pattern for other CustomBuilders.
 */
public class ByteArrayCustomBuilder implements CustomBuilder {

    private String encoding = null;

    /**
     * Constructor
     * @param encoding 
     */
    public ByteArrayCustomBuilder(String encoding) {
        this.encoding = (encoding == null) ? "utf-8" : encoding;
    }

    public OMElement create(String namespace, String localPart, OMContainer parent, XMLStreamReader reader, OMFactory factory) throws OMException {
        try {
            String prefix = reader.getPrefix();
            StreamingOMSerializer ser = new StreamingOMSerializer();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            XMLStreamWriter writer = StAXUtils.createXMLStreamWriter(baos, encoding);
            ser.serialize(reader, writer, false);
            writer.flush();
            byte[] bytes = baos.toByteArray();
            String text = new String(bytes, "utf-8");
            ByteArrayDataSource ds = new ByteArrayDataSource(bytes, encoding);
            OMNamespace ns = factory.createOMNamespace(namespace, prefix);
            OMElement om = null;
            if (parent instanceof SOAPHeader && factory instanceof SOAPFactory) {
                om = ((SOAPFactory) factory).createSOAPHeaderBlock(localPart, ns, ds);
            } else {
                om = factory.createOMElement(ds, localPart, ns);
            }
            parent.addChild(om);
            return om;
        } catch (XMLStreamException e) {
            throw new OMException(e);
        } catch (OMException e) {
            throw e;
        } catch (Throwable t) {
            throw new OMException(t);
        }
    }
}
