package org.apache.axis2.jaxws.message.databinding.impl;

import org.apache.axiom.om.OMDataSource;
import org.apache.axiom.om.OMDataSourceExt;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMSourcedElement;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axis2.datasource.XMLStringDataSource;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.message.databinding.XMLStringBlock;
import org.apache.axis2.jaxws.message.factory.BlockFactory;
import org.apache.axis2.jaxws.message.impl.BlockImpl;
import org.apache.axis2.jaxws.message.util.Reader2Writer;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

/**
 * XMLStringBlock
 * <p/>
 * Block containing a business object that is a String of xml text
 */
public class XMLStringBlockImpl extends BlockImpl implements XMLStringBlock {

    /**
     * Constructor called from factory
     *
     * @param busObject
     * @param qName
     * @param factory
     */
    XMLStringBlockImpl(String busObject, QName qName, BlockFactory factory) {
        super(busObject, null, qName, factory);
    }

    /**
     * Constructor called from factory
     *
     * @param reader
     * @param qName
     * @param factory
     */
    public XMLStringBlockImpl(OMElement omElement, QName qName, BlockFactory factory) {
        super(omElement, null, qName, factory);
    }

    protected Object _getBOFromReader(XMLStreamReader reader, Object busContext) throws XMLStreamException {
        Reader2Writer r2w = new Reader2Writer(reader);
        return r2w.getAsString();
    }

    @Override
    protected Object _getBOFromOM(OMElement omElement, Object busContext) throws XMLStreamException, WebServiceException {
        if (omElement instanceof OMSourcedElement) {
            OMDataSource ds = ((OMSourcedElement) omElement).getDataSource();
            if (ds instanceof XMLStringDataSource) {
                return ((XMLStringDataSource) ds).getObject();
            }
        }
        return super._getBOFromOM(omElement, busContext);
    }

    protected XMLStreamReader _getReaderFromBO(Object busObj, Object busContext) throws XMLStreamException {
        String str = (String) busObj;
        StringReader sr = new StringReader(str);
        return StAXUtils.createXMLStreamReader(sr);
    }

    protected void _outputFromBO(Object busObject, Object busContext, XMLStreamWriter writer) throws XMLStreamException {
        XMLStreamReader reader = _getReaderFromBO(busObject, busContext);
        _outputFromReader(reader, writer);
    }

    public boolean isElementData() {
        return false;
    }

    public void close() {
        return;
    }

    public InputStream getXMLInputStream(String encoding) throws UnsupportedEncodingException {
        try {
            byte[] bytes = ((String) getBusinessObject(false)).getBytes(encoding);
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
        return false;
    }

    public boolean isDestructiveWrite() {
        return false;
    }

    public OMDataSourceExt copy() throws OMException {
        return new XMLStringDataSource((String) getObject());
    }

    public byte[] getXMLBytes(String encoding) throws UnsupportedEncodingException {
        try {
            return ((String) getBusinessObject(false)).getBytes(encoding);
        } catch (XMLStreamException e) {
            throw ExceptionFactory.makeWebServiceException(e);
        }
    }
}
