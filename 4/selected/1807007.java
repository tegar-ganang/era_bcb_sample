package org.vd.servlet.method.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.vd.extensions.xml.stream.VDXMLStreamWriter;
import org.vd.servlet.VDException;
import org.vd.servlet.VDStatus;
import org.vd.servlet.method.VDMethod;
import org.vd.store.NotLoggedInException;
import org.vd.store.VirtualFile;

/**
 * Created on Sep 9, 2007 8:59:47 AM by Ajay
 */
public class LockMethod extends AbstractVDMethod {

    private static final Log LOGGER = LogFactory.getLog(LockMethod.class);

    public static final String LOCK_PROPERTY = "LOCKED";

    private static final long DEFAULT_TIMEOUT = 60 * 60;

    private boolean m_exclusiveLock;

    private boolean m_writeLock;

    private String m_owner;

    private long m_timeout;

    @Override
    public void executeRequest() throws VDException {
        boolean lock = VDMethod.LOCK.name().equals(request.getMethod());
        VirtualFile file = null;
        try {
            file = storage.toFile(resourcePath);
        } catch (NotLoggedInException e) {
            throw new VDException(HttpServletResponse.SC_UNAUTHORIZED, "Not logged in.", e);
        } catch (FileNotFoundException e) {
            send(lock, file);
            return;
        } catch (IOException e) {
            throw new VDException(VDStatus.SC_METHOD_FAILURE, null, e);
        }
        send(lock, file);
    }

    private void send(boolean lock, VirtualFile file) throws VDException {
        if (lock) {
            response.setStatus(HttpServletResponse.SC_OK);
            try {
                sendResponse(lock);
            } catch (XMLStreamException e) {
                throw new VDException(VDStatus.SC_METHOD_FAILURE, null, e);
            } catch (IOException e) {
                throw new VDException(VDStatus.SC_METHOD_FAILURE, null, e);
            }
            file.getProperties().put(LOCK_PROPERTY, Long.valueOf(m_timeout));
        } else {
            file.getProperties().remove(LOCK_PROPERTY);
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    private void sendResponse(boolean lock) throws XMLStreamException, IOException {
        Writer x = response.getWriter();
        XMLStreamWriter writer = new VDXMLStreamWriter(x);
        writer.writeStartDocument();
        writer.writeStartElement("prop");
        writer.writeStartElement("lockdiscovery");
        writer.writeStartElement("activelock");
        writer.writeStartElement("locktype");
        writer.writeEmptyElement(m_writeLock ? "write" : "read");
        writer.writeEndElement();
        writer.writeStartElement("lockscope");
        writer.writeEmptyElement(m_exclusiveLock ? "exclusive" : "shared");
        writer.writeEndElement();
        writer.writeStartElement("depth");
        writer.writeCharacters("0");
        writer.writeEndElement();
        writer.writeStartElement("owner");
        writer.writeCData(m_owner);
        writer.writeEndElement();
        writer.writeStartElement("timeout");
        writer.writeCharacters("Second-" + Long.toString(m_timeout));
        writer.writeEndElement();
        writer.writeStartElement("locktoken");
        writer.writeStartElement("href");
        writer.writeCharacters("opaquelocktoken:" + UUID.randomUUID().toString());
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }

    @Override
    public void parseRequest() throws VDException {
        if (null == resourcePath || resourcePath.trim().length() == 0) {
            LOGGER.error("The resource path is not found or empty.");
            throw new VDException(HttpServletResponse.SC_BAD_REQUEST, null, null);
        }
        try {
            Document doc = new SAXBuilder().build(request.getInputStream());
            LOGGER.debug("Lock request = " + new XMLOutputter(Format.getPrettyFormat()).outputString(doc));
            Element info = doc.getRootElement();
            Namespace namespace = info.getNamespace();
            Element lockscope = info.getChild("lockscope", namespace);
            m_exclusiveLock = lockscope.getChild("exclusive", namespace) != null;
            Element lockType = info.getChild("locktype", namespace);
            m_writeLock = lockType.getChild("write", namespace) != null;
            m_owner = info.getChildTextTrim("owner", namespace);
            String timeout = request.getHeader("Timeout");
            m_timeout = DEFAULT_TIMEOUT;
            if (null == timeout || timeout.trim().length() == 0) {
                m_timeout = DEFAULT_TIMEOUT;
            }
        } catch (JDOMException e) {
            throw new VDException(HttpServletResponse.SC_BAD_REQUEST, null, e);
        } catch (IOException e) {
            throw new VDException(HttpServletResponse.SC_BAD_REQUEST, null, e);
        }
    }
}
