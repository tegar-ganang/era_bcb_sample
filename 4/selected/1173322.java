package de.psisystems.dmachinery.jobs;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import de.psisystems.dmachinery.core.exeptions.PrintException;
import de.psisystems.dmachinery.io.ContentReader;
import de.psisystems.dmachinery.xml.AbstractMarshallable;
import de.psisystems.dmachinery.xml.XMLHelper;

public class SimpleBlock extends AbstractMarshallable implements Block {

    private URL url;

    private String id;

    private String version;

    public String getId() {
        return this.id;
    }

    public String getVersion() {
        return this.version;
    }

    public URL getURL() {
        return url;
    }

    public SimpleBlock(String id, String version, URL url) {
        super();
        this.url = url;
        this.id = id;
        this.version = version;
    }

    @Override
    public void toXML(XMLStreamWriter streamWriter, boolean standalone) throws PrintException {
        try {
            XMLHelper.writeStartElement(streamWriter, "block");
            XMLHelper.writeAttribute(streamWriter, "id", getId());
            XMLHelper.writeAttribute(streamWriter, "version", getVersion());
            XMLHelper.writeRawData(streamWriter, new String(ContentReader.read(getURL()), "UTF-8"));
            XMLHelper.writeEndElement(streamWriter, "block");
        } catch (XMLStreamException e) {
            throw new PrintException(e.getMessage() + " " + getURL(), e);
        } catch (UnsupportedEncodingException e) {
            throw new PrintException(e.getMessage() + " " + getURL(), e);
        } catch (IOException e) {
            throw new PrintException(e.getMessage() + " " + getURL(), e);
        }
    }
}
