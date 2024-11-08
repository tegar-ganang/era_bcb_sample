package org.nexopenframework.signature;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nexopenframework.signature.transform.ObjectSource;
import org.nexopenframework.xml.XmlUtils;
import org.nexopenframework.xml.dom.DOMCapable;
import org.nexopenframework.xml.stream.StAXUtils;
import org.nexopenframework.xml.stream.XMLStreamReaderConverter;
import org.nexopenframework.xml.stream.support.StAXWriterSupport;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * <p>NexTReT Open Framework</p>
 * 
 * <p>Base class for providers which would like to implement {@link XMLSignatureEngine}</p>
 * 
 * @author <a href="mailto:fme@nextret.net">Francesc Xavier Magdaleno</a>
 * @version 1.0
 * @since 1.0
 */
public abstract class GenericXMLSignatureEngine extends StAXWriterSupport implements XMLSignatureEngine, InitializingBean {

    public static final String SIGNATURE_SPEC_NS = "http://www.w3.org/2000/09/xmldsig#";

    public static final String XPATH_SIGNATURE = new StringBuffer("declare namespace ds='").append(SIGNATURE_SPEC_NS).append("';").append("//ds:Signature").toString();

    /** {@link org.apache.commons.logging} logging facility */
    protected Log logger = LogFactory.getLog(this.getClass());

    /** Interface for performing operations related to DOM */
    protected DOMCapable aware;

    /***/
    protected XMLStreamReaderConverter converter;

    /**A key store*/
    private KeyStore keyStore;

    /***/
    private PrivateKey privateKey;

    /***/
    private X509Certificate certificate;

    private String privateKeyAlias;

    private String privateKeyPass;

    private String certificateAlias;

    /** QName Signature */
    private QName qsignature = new QName(SIGNATURE_SPEC_NS, "Signature", "ds");

    /**
	 * <p></p>
	 * 
	 * @throws IllegalArgumentException if KeyStore, certificate alias, 
	 * 									private key alias and password are null
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
    public void afterPropertiesSet() throws XMLSignatureException {
        Assert.notNull(keyStore, "KeyStore could not been null");
        Assert.notNull(privateKeyAlias, "private key alias could not been null");
        Assert.notNull(privateKeyPass, "private key password could not been null");
        Assert.notNull(certificateAlias, "certificate alias could not been null");
    }

    /**
	 * <p>
	 * DOM aware implementation. This will holds all the DOM operations related
	 * to creation/validation of signatures
	 * </p>
	 * 
	 * @param converter
	 */
    public void setDOMCapable(DOMCapable aware) {
        this.aware = aware;
    }

    /**
	 * 
	 * @param converter
	 */
    public void setXMLStreamReaderConverter(XMLStreamReaderConverter converter) {
        this.converter = converter;
    }

    /**
	 * 
	 */
    public void setQSignature(QName qSign) {
        this.qsignature = qSign;
    }

    /**
	 * @param certificateAlias
	 */
    public void setCertificateAlias(String certificateAlias) {
        this.certificateAlias = certificateAlias;
    }

    public void setPrivateKeyAlias(String privateKeyAlias) {
        this.privateKeyAlias = privateKeyAlias;
    }

    public void setPrivateKeyPass(String privateKeyPass) {
        this.privateKeyPass = privateKeyPass;
    }

    /**
	 * @param keyStore
	 */
    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    protected final X509Certificate getCertificate() throws KeyStoreException {
        if (this.certificate != null) {
            return this.certificate;
        }
        X509Certificate certificate = (X509Certificate) this.keyStore.getCertificate(certificateAlias);
        this.certificate = certificate;
        return certificate;
    }

    /**
	 * @return
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableKeyException
	 */
    protected final PrivateKey getPrivateKey() throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        if (this.privateKey != null) {
            return this.privateKey;
        }
        PrivateKey privateKey = (PrivateKey) this.keyStore.getKey(privateKeyAlias, privateKeyPass.toCharArray());
        this.privateKey = privateKey;
        return privateKey;
    }

    /**
	 * <p>
	 * </p>
	 * 
	 * @param source
	 * @return
	 * @throws XMLSignatureException
	 */
    protected final Document toDocument(Source source) throws XMLSignatureException {
        Document doc = null;
        if (source instanceof ObjectSource) {
            try {
                ObjectSource _source = (ObjectSource) source;
                Object _envelope = _source.getMessage();
                XMLStreamReader reader = converter.toXMLStreamReader(_envelope);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XMLStreamWriter writer = this.getXMLOutputFactory().createXMLStreamWriter(baos);
                StAXUtils.copy(reader, writer);
                writer.flush();
                writer.close();
                return toDocument(new StreamSource(new ByteArrayInputStream(baos.toByteArray())));
            } catch (XMLStreamException e) {
                throw new XMLSignatureException(e);
            } catch (RuntimeException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Unexpected runtime exception ocurred,processing source :: \n" + source, e);
                }
                throw e;
            }
        } else if (source instanceof StreamSource) {
            try {
                DocumentBuilder db = XmlUtils.newDocumentBuilder();
                doc = db.parse(((StreamSource) source).getInputStream());
            } catch (SAXException e) {
                throw new XMLSignatureException(e);
            } catch (IOException e) {
                throw new XMLSignatureException(e);
            }
        } else if (source instanceof DOMSource) {
            doc = (Document) ((DOMSource) source).getNode();
        } else {
            throw new XMLSignatureException("Not supported Source [" + source + "]");
        }
        return doc;
    }

    /**
	 * @param source
	 * @return
	 * @throws XMLSignatureException
	 */
    protected Element toSignElement(Source source) throws XMLSignatureException {
        Element signElement = null;
        if (source instanceof ObjectSource) {
            Object envelope = ((ObjectSource) source).getMessage();
            Map namespaces = new HashMap(1);
            namespaces.put(qsignature.getPrefix(), qsignature.getNamespaceURI());
            signElement = aware.executeXPath(XPATH_SIGNATURE, envelope, namespaces);
        } else if (source instanceof DOMSource) {
            DOMSource domSource = (DOMSource) source;
            Node root = domSource.getNode();
            Document doc = (Document) root;
            NodeList nodes = doc.getElementsByTagNameNS(qsignature.getNamespaceURI(), qsignature.getLocalPart());
            if (nodes != null && nodes.getLength() > 0) {
                signElement = (Element) nodes.item(0);
            }
        } else if (source instanceof StreamSource) {
            try {
                StreamSource streamSource = (StreamSource) source;
                DocumentBuilder db = XmlUtils.newDocumentBuilder();
                Document doc = db.parse(streamSource.getInputStream());
                return toSignElement(new DOMSource(doc));
            } catch (SAXException e) {
                throw new XMLSignatureException(e);
            } catch (IOException e) {
                throw new XMLSignatureException(e);
            }
        }
        return signElement;
    }

    /**
	 * <p>This method MUST be overwritten if you are dealing with enveloped
	 * structures just a <code>SOAP:Envelope</code></p>
	 * 
	 * @param doc
	 * @return
	 */
    protected Node retrieveEnvelopedElement(Document doc) throws XMLSignatureException {
        return doc.getDocumentElement();
    }
}
