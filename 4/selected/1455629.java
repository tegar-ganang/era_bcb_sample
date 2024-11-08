package be.fedict.eid.applet.service.signer.odf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.crypto.URIDereferencer;
import javax.xml.parsers.ParserConfigurationException;
import be.fedict.eid.applet.service.signer.DigestAlgo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import be.fedict.eid.applet.service.signer.AbstractXmlSignatureService;
import be.fedict.eid.applet.service.signer.facets.KeyInfoSignatureFacet;
import be.fedict.eid.applet.service.signer.facets.XAdESSignatureFacet;

/**
 * Signature Service implementation for OpenDocument format signatures.
 * 
 * The signatures created with this class are accepted as valid signature within
 * OpenOffice.org 3.x. They probably don't get accepted by older OOo versions.
 * 
 * @see http://wiki.services.openoffice.org/wiki/Security/Digital_Signatures
 * 
 * @author fcorneli
 * 
 */
public abstract class AbstractODFSignatureService extends AbstractXmlSignatureService {

    private static final Log LOG = LogFactory.getLog(AbstractODFSignatureService.class);

    private final XAdESSignatureFacet xadesSignatureFacet;

    public AbstractODFSignatureService(DigestAlgo digestAlgo) {
        super(digestAlgo);
        addSignatureFacet(new ODFSignatureFacet(this, getSignatureDigestAlgorithm()));
        addSignatureFacet(new OpenOfficeSignatureFacet(getSignatureDigestAlgorithm()));
        this.xadesSignatureFacet = new XAdESSignatureFacet(getSignatureDigestAlgorithm());
        addSignatureFacet(this.xadesSignatureFacet);
        addSignatureFacet(new KeyInfoSignatureFacet(false, true, false));
    }

    /**
	 * Gives back the used XAdES signature facet. Allows for extra configuration
	 * of the XAdES elements.
	 * 
	 * @return
	 */
    protected XAdESSignatureFacet getXAdESSignatureFacet() {
        return this.xadesSignatureFacet;
    }

    /**
	 * Returns the URL of the ODF to be signed.
	 * 
	 * @return
	 */
    protected abstract URL getOpenDocumentURL();

    @Override
    protected final URIDereferencer getURIDereferencer() {
        URL odfUrl = getOpenDocumentURL();
        return new ODFURIDereferencer(odfUrl);
    }

    @Override
    protected String getSignatureDescription() {
        return "ODF Document";
    }

    @Override
    protected final OutputStream getSignedDocumentOutputStream() {
        LOG.debug("get signed document output stream");
        OutputStream signedDocumentOutputStream = new ODFSignedDocumentOutputStream();
        return signedDocumentOutputStream;
    }

    private class ODFSignedDocumentOutputStream extends ByteArrayOutputStream {

        @Override
        public void close() throws IOException {
            LOG.debug("close ODF signed document output stream");
            super.close();
            outputSignedOpenDocument(this.toByteArray());
        }
    }

    private void outputSignedOpenDocument(byte[] signatureData) throws IOException {
        LOG.debug("output signed open document");
        OutputStream signedOdfOutputStream = getSignedOpenDocumentOutputStream();
        if (null == signedOdfOutputStream) {
            throw new NullPointerException("signedOpenDocumentOutputStream is null");
        }
        ZipOutputStream zipOutputStream = new ZipOutputStream(signedOdfOutputStream);
        ZipInputStream zipInputStream = new ZipInputStream(this.getOpenDocumentURL().openStream());
        ZipEntry zipEntry;
        while (null != (zipEntry = zipInputStream.getNextEntry())) {
            if (!zipEntry.getName().equals(ODFUtil.SIGNATURE_FILE)) {
                zipOutputStream.putNextEntry(zipEntry);
                IOUtils.copy(zipInputStream, zipOutputStream);
            }
        }
        zipInputStream.close();
        zipEntry = new ZipEntry(ODFUtil.SIGNATURE_FILE);
        zipOutputStream.putNextEntry(zipEntry);
        IOUtils.write(signatureData, zipOutputStream);
        zipOutputStream.close();
    }

    /**
	 * The output stream to which to write the signed ODF file.
	 * 
	 * @return
	 */
    protected abstract OutputStream getSignedOpenDocumentOutputStream();

    public final String getFilesDigestAlgorithm() {
        return null;
    }

    @Override
    protected final Document getEnvelopingDocument() throws ParserConfigurationException, IOException, SAXException {
        Document document = getODFSignatureDocument();
        if (null != document) {
            return document;
        }
        document = ODFUtil.getNewDocument();
        Element rootElement = document.createElementNS(ODFUtil.SIGNATURE_NS, ODFUtil.SIGNATURE_ELEMENT);
        rootElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns", ODFUtil.SIGNATURE_NS);
        document.appendChild(rootElement);
        return document;
    }

    /**
	 * Get the XML signature file from the ODF package
	 * 
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
    private Document getODFSignatureDocument() throws IOException, ParserConfigurationException, SAXException {
        URL odfUrl = this.getOpenDocumentURL();
        InputStream inputStream = ODFUtil.findDataInputStream(odfUrl.openStream(), ODFUtil.SIGNATURE_FILE);
        if (null != inputStream) {
            return ODFUtil.loadDocument(inputStream);
        }
        return null;
    }
}
