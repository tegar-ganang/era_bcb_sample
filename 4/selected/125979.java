package org.soda.dpws.exchange;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.jdom.Element;
import org.soda.dpws.attachments.Attachments;
import org.soda.dpws.soap.Soap11;
import org.soda.dpws.soap.SoapVersion;
import org.soda.dpws.soap.SoapVersionFactory;
import org.soda.dpws.transport.Channel;

/**
 * 
 * 
 */
public abstract class AbstractMessage {

    /**
   * 
   */
    public static final String ANONYMOUS_URI = "urn:codehaus:xfire:anonymous";

    private Element header;

    private String uri;

    private String encoding = "UTF-8";

    private Object body;

    private Attachments attachments;

    private SoapVersion soapVersion = Soap11.getInstance();

    private Channel channel;

    private Map<Object, Object> properties = new HashMap<Object, Object>();

    private Map<Object, Object> headerProperties = new HashMap<Object, Object>();

    /**
   * @return the Body
   */
    public Object getBody() {
        return body;
    }

    /**
   * @param body
   */
    public void setBody(Object body) {
        this.body = body;
    }

    /**
   * @return the {@link Channel}
   */
    public Channel getChannel() {
        return channel;
    }

    /**
   * @param channel
   */
    public void setChannel(final Channel channel) {
        this.channel = channel;
    }

    /**
   * @return the {@link SoapVersion}
   */
    public SoapVersion getSoapVersion() {
        return soapVersion;
    }

    /**
   * @param soapVersion
   */
    public void setSoapVersion(String soapVersion) {
        this.soapVersion = SoapVersionFactory.getInstance().getSoapVersion(soapVersion);
    }

    /**
   * @param soapVersion
   */
    public void setSoapVersion(SoapVersion soapVersion) {
        this.soapVersion = soapVersion;
    }

    /**
   * @return the encoding
   */
    public String getEncoding() {
        return encoding;
    }

    /**
   * @param encoding
   */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
   * @return .
   */
    public boolean hasHeader() {
        return header != null;
    }

    /**
   * @return the {@link Element}
   */
    public Element getHeader() {
        return header;
    }

    /**
   * @return the {@link Element}
   */
    public Element getOrCreateHeader() {
        if (headerProperties.size() > 0) {
            Set<Entry<Object, Object>> headerSet = headerProperties.entrySet();
            Iterator<Entry<Object, Object>> it = headerSet.iterator();
            if (header == null) {
                header = new Element("Header", SoapVersion.prefix, SoapVersion.namespace);
            }
            while (it.hasNext()) {
                Map.Entry<Object, Object> entry = it.next();
                Element newAttributeElement = new Element((String) entry.getKey(), SoapVersion.prefix, SoapVersion.namespace);
                newAttributeElement.addContent((String) entry.getValue());
                header.addContent(newAttributeElement);
            }
        } else {
            if (header == null) {
                header = new Element("Header", SoapVersion.prefix, SoapVersion.namespace);
            }
        }
        return header;
    }

    /**
   * @param header
   */
    public void setHeader(Element header) {
        this.header = header;
    }

    /**
   * @return the current uri
   */
    public String getUri() {
        return uri;
    }

    /**
   * @param uri
   */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
   * @return the {@link Attachments}
   */
    public Attachments getAttachments() {
        return attachments;
    }

    /**
   * @param attachments
   */
    public void setAttachments(Attachments attachments) {
        this.attachments = attachments;
    }

    /**
   * @param key
   * @return the property
   */
    public Object getProperty(Object key) {
        return properties.get(key);
    }

    /**
   * @param key
   * @param value
   */
    public void setProperty(Object key, Object value) {
        properties.put(key, value);
    }

    /**
   * @param key
   * @param value
   */
    public void setHeaderProperty(Object key, Object value) {
        headerProperties.put(key, value);
    }

    public String toString() {
        return super.toString() + "[uri=\"" + getUri() + "\"]";
    }
}
