package org.apache.axis2.jaxws.message.impl;

import org.apache.axiom.om.OMDataSourceExt;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.ds.ByteArrayDataSource;
import org.apache.axiom.om.impl.MTOMXMLStreamWriter;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axis2.jaxws.ExceptionFactory;
import org.apache.axis2.jaxws.i18n.Messages;
import org.apache.axis2.jaxws.message.Block;
import org.apache.axis2.jaxws.message.Message;
import org.apache.axis2.jaxws.message.factory.BlockFactory;
import org.apache.axis2.jaxws.message.util.Reader2Writer;
import org.apache.axis2.jaxws.spi.Constants;
import org.apache.axis2.jaxws.utility.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;

/**
 * BlockImpl Abstract Base class for various Block Implementations.
 * <p/>
 * The base class takes care of controlling the transformations between BusinessObject,
 * XMLStreamReader and SOAPElement A derived class must minimally define the following:
 * _getBOFromReader _getReaderFromBO _outputFromBO
 * <p/>
 * In addtion, the derived class may want to override the following: _getBOFromBO ...if the
 * BusinessObject is consumed when read (i.e. it is an InputSource)
 * <p/>
 * The derived classes don't have direct access to the instance data. This ensures that BlockImpl
 * controls the transformations.
 */
public abstract class BlockImpl implements Block {

    private static Log log = LogFactory.getLog(BlockImpl.class);

    protected Object busObject;

    protected Object busContext;

    protected OMElement omElement = null;

    protected QName qName;

    private boolean noQNameAvailable = false;

    protected BlockFactory factory;

    protected boolean consumed = false;

    protected Message parent;

    private HashMap map = null;

    /**
     * A Block has the following components
     *
     * @param busObject
     * @param busContext or null
     * @param qName      or null if unknown
     * @param factory    that creates the Block
     */
    protected BlockImpl(Object busObject, Object busContext, QName qName, BlockFactory factory) {
        this.busObject = busObject;
        this.busContext = busContext;
        this.qName = qName;
        this.factory = factory;
    }

    /**
     * A Block has the following components
     *
     * @param reader
     * @param busContext or null
     * @param qName      or null if unknown
     * @param factory    that creates the Block
     */
    protected BlockImpl(OMElement omElement, Object busContext, QName qName, BlockFactory factory) {
        this.omElement = omElement;
        this.busContext = busContext;
        this.qName = qName;
        this.factory = factory;
    }

    public BlockFactory getBlockFactory() {
        return factory;
    }

    public Object getBusinessContext() {
        return busContext;
    }

    public Message getParent() {
        return parent;
    }

    public void setParent(Message p) {
        parent = p;
    }

    public Object getBusinessObject(boolean consume) throws XMLStreamException, WebServiceException {
        if (consumed) {
            throw ExceptionFactory.makeWebServiceException(Messages.getMessage("BlockImplErr1", this.getClass().getName()));
        }
        if (busObject != null) {
            busObject = _getBOFromBO(busObject, busContext, consume);
        } else {
            busObject = _getBOFromOM(omElement, busContext);
            omElement = null;
        }
        Object newBusObject = busObject;
        setConsumed(consume);
        return newBusObject;
    }

    public QName getQName() throws WebServiceException {
        try {
            if (qName == null) {
                if (noQNameAvailable) {
                    return null;
                }
                if (omElement == null) {
                    try {
                        XMLStreamReader newReader = _getReaderFromBO(busObject, busContext);
                        busObject = null;
                        StAXOMBuilder builder = new StAXOMBuilder(newReader);
                        omElement = builder.getDocumentElement();
                    } catch (Exception e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Exception occurred while obtaining QName:" + e);
                        }
                        if (!isElementData()) {
                            if (log.isDebugEnabled()) {
                                log.debug("The block does not contain an xml element. Processing continues.");
                            }
                            noQNameAvailable = true;
                            return null;
                        } else {
                            throw ExceptionFactory.makeWebServiceException(e);
                        }
                    }
                }
                qName = omElement.getQName();
            }
            return qName;
        } catch (Exception xse) {
            setConsumed(true);
            throw ExceptionFactory.makeWebServiceException(xse);
        }
    }

    /**
     * This method is intended for derived objects to set the qName
     *
     * @param qName
     */
    protected void setQName(QName qName) {
        this.qName = qName;
    }

    public XMLStreamReader getXMLStreamReader(boolean consume) throws XMLStreamException, WebServiceException {
        XMLStreamReader newReader = null;
        if (consumed) {
            if (this.getParent() != null && getParent().isPostPivot()) {
                return _postPivot_getXMLStreamReader();
            }
            throw ExceptionFactory.makeWebServiceException(Messages.getMessage("BlockImplErr1", this.getClass().getName()));
        }
        if (omElement != null) {
            if (consume) {
                if (omElement.getBuilder() != null && !omElement.getBuilder().isCompleted()) {
                    newReader = omElement.getXMLStreamReaderWithoutCaching();
                } else {
                    newReader = omElement.getXMLStreamReader();
                }
                omElement = null;
            } else {
                newReader = omElement.getXMLStreamReader();
            }
        } else if (busObject != null) {
            busObject = _getBOFromBO(busObject, busContext, consume);
            newReader = _getReaderFromBO(busObject, busContext);
        }
        setConsumed(consume);
        return newReader;
    }

    public XMLStreamReader getReader() throws XMLStreamException {
        return getXMLStreamReader(true);
    }

    public void serialize(OutputStream output, OMOutputFormat format) throws XMLStreamException {
        MTOMXMLStreamWriter writer = new MTOMXMLStreamWriter(output, format);
        serialize(writer);
        writer.flush();
        try {
            writer.close();
        } catch (XMLStreamException e) {
            if (log.isDebugEnabled()) {
                log.debug("Catching and swallowing exception " + e);
            }
        }
    }

    public void serialize(Writer writerTarget, OMOutputFormat format) throws XMLStreamException {
        MTOMXMLStreamWriter writer = new MTOMXMLStreamWriter(StAXUtils.createXMLStreamWriter(writerTarget));
        writer.setOutputFormat(format);
        serialize(writer);
        writer.flush();
        writer.close();
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        outputTo(writer, isDestructiveWrite());
    }

    public OMElement getOMElement() throws XMLStreamException, WebServiceException {
        OMElement newOMElement = null;
        boolean consume = true;
        if (consumed) {
            throw ExceptionFactory.makeWebServiceException(Messages.getMessage("BlockImplErr1", this.getClass().getName()));
        }
        if (omElement != null) {
            newOMElement = omElement;
        } else if (busObject != null) {
            busObject = _getBOFromBO(busObject, busContext, consume);
            newOMElement = _getOMFromBO(busObject, busContext);
        }
        setConsumed(consume);
        return newOMElement;
    }

    public boolean isConsumed() {
        return consumed;
    }

    /**
     * Once consumed, all instance data objects are nullified to prevent subsequent access
     *
     * @param consume
     * @return
     */
    public void setConsumed(boolean consume) {
        if (consume) {
            this.consumed = true;
            busObject = null;
            busContext = null;
            omElement = null;
            if (log.isDebugEnabled()) {
                log.debug("Message Block Monitor: Action=Consumed");
                log.trace(JavaUtils.stackToString());
            }
        } else {
            consumed = false;
        }
    }

    public boolean isQNameAvailable() {
        return (qName != null);
    }

    public void outputTo(XMLStreamWriter writer, boolean consume) throws XMLStreamException, WebServiceException {
        if (log.isDebugEnabled()) {
            log.debug("Start outputTo");
        }
        if (consumed) {
            if (this.getParent() != null && getParent().isPostPivot()) {
                _postPivot_outputTo(writer);
            }
            throw ExceptionFactory.makeWebServiceException(Messages.getMessage("BlockImplErr1", this.getClass().getName()));
        }
        if (omElement != null) {
            _outputFromOM(omElement, writer, consume);
        } else if (busObject != null) {
            if (log.isDebugEnabled()) {
                log.debug("Write business object");
            }
            busObject = _getBOFromBO(busObject, busContext, consume);
            _outputFromBO(busObject, busContext, writer);
        }
        setConsumed(consume);
        if (log.isDebugEnabled()) {
            log.debug("End outputTo");
        }
        return;
    }

    /**
     * Called if we have passed the pivot point but someone wants to output the block. The actual
     * block implementation may choose to override this setting
     */
    protected void _postPivot_outputTo(XMLStreamWriter writer) throws XMLStreamException, WebServiceException {
        if (log.isDebugEnabled()) {
            QName theQName = isQNameAvailable() ? getQName() : new QName("unknown");
            log.debug("The Block for " + theQName + " is already consumed and therefore it is not written.");
            log.debug("If you need this block preserved, please set the " + Constants.SAVE_REQUEST_MSG + " property on the MessageContext.");
        }
        return;
    }

    /**
     * Called if we have passed the pivot point but someone wants to output the block. The actual
     * block implementation may choose to override this setting.
     */
    protected XMLStreamReader _postPivot_getXMLStreamReader() throws XMLStreamException, WebServiceException {
        if (log.isDebugEnabled()) {
            QName theQName = isQNameAvailable() ? getQName() : new QName("unknown");
            log.debug("The Block for " + theQName + " is already consumed and therefore it is only partially read.");
            log.debug("If you need this block preserved, please set the " + Constants.SAVE_REQUEST_MSG + " property on the MessageContext.");
        }
        QName qName = getQName();
        String text = "";
        if (qName.getNamespaceURI().length() > 0) {
            text = "<prefix:" + qName.getLocalPart() + " xmlns:prefix='" + qName.getNamespaceURI() + "'/>";
        } else {
            text = "<" + qName.getLocalPart() + "/>";
        }
        StringReader sr = new StringReader(text);
        return StAXUtils.createXMLStreamReader(sr);
    }

    /**
     * @return true if the representation of the block is currently a business object. Derived classes
     *         may use this information to get information in a performant way.
     */
    protected boolean isBusinessObject() {
        return busObject != null;
    }

    public String traceString(String indent) {
        return null;
    }

    /**
     * The default implementation is to return the business object. A derived block may want to
     * override this class if the business object is consumed when read (thus the dervived block may
     * want to make a buffered copy) (An example use case for overriding this method is the
     * businessObject is an InputSource)
     *
     * @param busObject
     * @param busContext
     * @param consume
     * @return
     */
    protected Object _getBOFromBO(Object busObject, Object busContext, boolean consume) {
        return busObject;
    }

    /**
     * The derived class must provide an implementation that builds the business object from the
     * reader
     *
     * @param reader     XMLStreamReader, which is consumed
     * @param busContext
     * @return
     */
    protected abstract Object _getBOFromReader(XMLStreamReader reader, Object busContext) throws XMLStreamException, WebServiceException;

    /**
     * Default method for getting business object from OM.
     * Derived classes may override this method to get the business object from a
     * data source.
     * 
     * @param om
     * @param busContext
     * @return Business Object
     * @throws XMLStreamException
     * @throws WebServiceException
     */
    protected Object _getBOFromOM(OMElement omElement, Object busContext) throws XMLStreamException, WebServiceException {
        XMLStreamReader reader = _getReaderFromOM(omElement);
        return _getBOFromReader(reader, busContext);
    }

    /**
     * Get an XMLStreamReader for the BusinessObject The derived Block must implement this method
     *
     * @param busObj
     * @param busContext
     * @return
     */
    protected abstract XMLStreamReader _getReaderFromBO(Object busObj, Object busContext) throws XMLStreamException, WebServiceException;

    /**
     * @param omElement
     * @return XMLStreamReader
     */
    protected XMLStreamReader _getReaderFromOM(OMElement omElement) {
        XMLStreamReader reader;
        if (omElement.getBuilder() != null && !omElement.getBuilder().isCompleted()) {
            reader = omElement.getXMLStreamReaderWithoutCaching();
        } else {
            reader = omElement.getXMLStreamReader();
        }
        return reader;
    }

    /**
     * @param busObject
     * @param busContext
     * @return OMElement
     * @throws XMLStreamException
     * @throws WebServiceException
     */
    protected OMElement _getOMFromBO(Object busObject, Object busContext) throws XMLStreamException, WebServiceException {
        XMLStreamReader newReader = _getReaderFromBO(busObject, busContext);
        StAXOMBuilder builder = new StAXOMBuilder(newReader);
        return builder.getDocumentElement();
    }

    /**
     * Output Reader contents to a Writer. The default implementation is probably sufficient for most
     * derived classes.
     *
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    protected void _outputFromReader(XMLStreamReader reader, XMLStreamWriter writer) throws XMLStreamException {
        Reader2Writer r2w = new Reader2Writer(reader);
        r2w.outputTo(writer);
    }

    /**
     * Output OMElement contents to a Writer. The default implementation is probably sufficient for most
     * derived classes.
     *
     * @param om
     * @param writer
     * @throws XMLStreamException
     */
    protected void _outputFromOM(OMElement omElement, XMLStreamWriter writer, boolean consume) throws XMLStreamException {
        if (consume) {
            if (log.isDebugEnabled()) {
                log.debug("Write using OMElement.serializeAndConsume");
            }
            omElement.serializeAndConsume(writer);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Write Using OMElement.serialize");
            }
            omElement.serialize(writer);
        }
    }

    public OMDataSourceExt copy() throws OMException {
        try {
            String encoding = "utf-8";
            byte[] bytes = this.getXMLBytes(encoding);
            return new ByteArrayDataSource(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new OMException(e);
        }
    }

    /**
     * Output BusinessObject contents to a Writer.
     * Derived classes must provide this implementation
     * @param busObject
     * @param busContext
     * @param writer
     * @throws XMLStreamException
     * @throws WebServiceException
     */
    protected abstract void _outputFromBO(Object busObject, Object busContext, XMLStreamWriter writer) throws XMLStreamException, WebServiceException;

    public Object getProperty(String key) {
        if (map == null) {
            return null;
        }
        return map.get(key);
    }

    public Object setProperty(String key, Object value) {
        if (map == null) {
            map = new HashMap();
        }
        return map.put(key, value);
    }

    public boolean hasProperty(String key) {
        if (map == null) {
            return false;
        }
        return map.containsKey(key);
    }
}
