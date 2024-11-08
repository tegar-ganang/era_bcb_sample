package org.apache.axis2.jaxws.message.databinding.impl;

import org.apache.axiom.om.OMDataSource;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMSourcedElement;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axis2.datasource.SourceDataSource;
import org.apache.axis2.java.security.AccessController;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.message.databinding.SourceBlock;
import org.apache.axis2.jaxws.message.factory.BlockFactory;
import org.apache.axis2.jaxws.message.impl.BlockImpl;
import org.apache.axis2.jaxws.message.util.Reader2Writer;
import org.apache.axis2.jaxws.utility.ConvertUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.WebServiceException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * SourceBlock
 * <p/>
 * Block containing a business object that is a javax.xml.transform.Source.
 * <p/>
 * The javax.xml.transform.Source is an interface.  The actual concrete class may be one of the
 * following: - StreamSource - DOMSource - JAXBSource - SAXSource - StAXSource
 * <p/>
 * During processing of the block, the block is free to change the representation from one source
 * to another.  (i.e. if you initially seed this with a SAXSource, but a later access may give you
 * a StAXSource).
 * <p/>
 * A Source is consumed when read.  The block will make a copy of the source if a non-consumable
 * request is made.
 */
public class SourceBlockImpl extends BlockImpl implements SourceBlock {

    private static final Log log = LogFactory.getLog(SourceBlockImpl.class);

    private static Class staxSource = null;

    static {
        try {
            staxSource = forName("javax.xml.transform.stax.StAXSource");
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("StAXSource is not present in the JDK.  " + "This is acceptable.  Processing continues");
            }
        }
        try {
            if (XMLInputFactory.newInstance().getClass().getName().indexOf("wstx") != -1) {
                staxSource = null;
            }
        } catch (Exception e) {
        }
    }

    /**
     * Constructor called from factory
     *
     * @param busObject
     * @param qName
     * @param factory
     */
    SourceBlockImpl(Source busObject, QName qName, BlockFactory factory) throws WebServiceException {
        super(busObject, null, qName, factory);
        if (busObject instanceof DOMSource || busObject instanceof SAXSource || busObject instanceof StreamSource || (busObject.getClass().equals(staxSource)) || busObject instanceof JAXBSource) {
            if (log.isDebugEnabled()) {
                log.debug("data object is a " + busObject.getClass().getName());
            }
        } else {
            throw ExceptionFactory.makeWebServiceException(Messages.getMessage("SourceNotSupported", busObject.getClass().getName()));
        }
    }

    /**
     * Constructor called from factory
     *
     * @param reader
     * @param qName
     * @param factory
     */
    public SourceBlockImpl(OMElement omElement, QName qName, BlockFactory factory) {
        super(omElement, null, qName, factory);
    }

    @Override
    protected Object _getBOFromReader(XMLStreamReader reader, Object busContext) throws XMLStreamException {
        Reader2Writer r2w = new Reader2Writer(reader);
        String text = r2w.getAsString();
        StringReader sr = new StringReader(text);
        return new StreamSource(sr);
    }

    @Override
    protected Object _getBOFromOM(OMElement omElement, Object busContext) throws XMLStreamException, WebServiceException {
        Object busObject;
        if (omElement instanceof OMSourcedElement) {
            OMDataSource ds = ((OMSourcedElement) omElement).getDataSource();
            if (ds instanceof SourceDataSource) {
                return ((SourceDataSource) ds).getObject();
            }
        }
        boolean hasFault = false;
        if ((parent != null && parent.isFault()) || omElement.getQName().getLocalPart().equals(SOAP11Constants.SOAPFAULT_LOCAL_NAME)) {
            hasFault = true;
        }
        if (!hasFault) {
            busObject = super._getBOFromOM(omElement, busContext);
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            omElement.serialize(baos);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            busObject = new StreamSource(bais);
        }
        return busObject;
    }

    @Override
    protected XMLStreamReader _getReaderFromBO(Object busObj, Object busContext) throws XMLStreamException, WebServiceException {
        try {
            if (busObj instanceof StreamSource) {
                XMLInputFactory f = StAXUtils.getXMLInputFactory();
                XMLStreamReader reader = f.createXMLStreamReader((Source) busObj);
                StAXUtils.releaseXMLInputFactory(f);
                return reader;
            }
            return _slow_getReaderFromSource((Source) busObj);
        } catch (Exception e) {
            String className = (busObj == null) ? "none" : busObj.getClass().getName();
            throw ExceptionFactory.makeWebServiceException(Messages.getMessage("SourceReadErr", className), e);
        }
    }

    /** Creates an XMLStreamReader from a Source using a slow but proven algorithm. */
    private XMLStreamReader _slow_getReaderFromSource(Source src) throws XMLStreamException {
        if (log.isDebugEnabled()) {
            log.debug("Start _slow_getReaderFromSource");
        }
        byte[] bytes = (byte[]) ConvertUtils.convert(src, byte[].class);
        if (log.isDebugEnabled()) {
            log.debug("Successfully converted to ByteArray");
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        XMLStreamReader reader = StAXUtils.createXMLStreamReader(bais);
        if (log.isDebugEnabled()) {
            log.debug("End _slow_getReaderFromSource =" + reader);
        }
        return reader;
    }

    @Override
    protected void _outputFromBO(Object busObject, Object busContext, XMLStreamWriter writer) throws XMLStreamException, WebServiceException {
        if (log.isDebugEnabled()) {
            log.debug("Start _outputFromBO");
        }
        XMLStreamReader reader = _getReaderFromBO(busObject, busContext);
        if (log.isDebugEnabled()) {
            log.debug("Obtained reader=" + reader);
        }
        _outputFromReader(reader, writer);
        if (log.isDebugEnabled()) {
            log.debug("End _outputReaderFromBO");
        }
    }

    @Override
    protected Object _getBOFromBO(Object busObject, Object busContext, boolean consume) {
        if (consume) {
            return busObject;
        } else {
            throw ExceptionFactory.makeWebServiceException(Messages.getMessage("SourceMissingSupport", busObject.getClass().getName()));
        }
    }

    public boolean isElementData() {
        return false;
    }

    /**
     * Return the class for this name
     * @return Class
     */
    private static Class forName(final String className) throws ClassNotFoundException {
        Class cl = null;
        try {
            cl = (Class) AccessController.doPrivileged(new PrivilegedExceptionAction() {

                public Object run() throws ClassNotFoundException {
                    return Class.forName(className);
                }
            });
        } catch (PrivilegedActionException e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception thrown from AccessController: " + e);
            }
            throw (ClassNotFoundException) e.getException();
        }
        return cl;
    }

    public void close() {
        return;
    }

    public InputStream getXMLInputStream(String encoding) throws UnsupportedEncodingException {
        try {
            byte[] bytes = (byte[]) ConvertUtils.convert(getBusinessObject(false), byte[].class);
            return new ByteArrayInputStream(bytes);
        } catch (XMLStreamException e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }

    public Object getObject() {
        try {
            return getBusinessObject(false);
        } catch (XMLStreamException e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }

    public boolean isDestructiveRead() {
        return true;
    }

    public boolean isDestructiveWrite() {
        return true;
    }

    public byte[] getXMLBytes(String encoding) throws UnsupportedEncodingException {
        if (log.isDebugEnabled()) {
            log.debug("Start getXMLBytes");
        }
        byte[] bytes = null;
        try {
            bytes = (byte[]) ConvertUtils.convert(getBusinessObject(false), byte[].class);
        } catch (XMLStreamException e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
        if (log.isDebugEnabled()) {
            log.debug("End getXMLBytes");
        }
        return bytes;
    }
}
