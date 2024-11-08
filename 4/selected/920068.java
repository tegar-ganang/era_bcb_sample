package be.fedict.eid.applet.service.signer.asic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.crypto.URIDereferencer;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import be.fedict.eid.applet.service.signer.AbstractXmlSignatureService;
import be.fedict.eid.applet.service.signer.CloseActionOutputStream;
import be.fedict.eid.applet.service.signer.DigestAlgo;
import be.fedict.eid.applet.service.signer.TemporaryDataStorage;
import be.fedict.eid.applet.service.signer.facets.IdentitySignatureFacet;
import be.fedict.eid.applet.service.signer.facets.KeyInfoSignatureFacet;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.facets.XAdESSignatureFacet;
import be.fedict.eid.applet.service.signer.facets.XAdESXLSignatureFacet;
import be.fedict.eid.applet.service.signer.odf.ODFUtil;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.spi.AddressDTO;
import be.fedict.eid.applet.service.spi.DigestInfo;
import be.fedict.eid.applet.service.spi.IdentityDTO;
import be.fedict.eid.applet.service.spi.SignatureServiceEx;

/**
 * Abstract ASiC signature service implementation. Implements Associated
 * Signature Containers according to ETSI TS 102 918 v1.1.1.
 * 
 * @author Frank Cornelis.
 * 
 */
public class AbstractASiCSignatureService extends AbstractXmlSignatureService implements SignatureServiceEx {

    private final TemporaryDataStorage temporaryDataStorage;

    private final File tmpFile;

    private final OutputStream documentOutputStream;

    public AbstractASiCSignatureService(InputStream documentInputStream, DigestAlgo digestAlgo, RevocationDataService revocationDataService, TimeStampService timeStampService, String claimedRole, IdentityDTO identity, byte[] photo, TemporaryDataStorage temporaryDataStorage, OutputStream documentOutputStream) throws IOException {
        super(digestAlgo);
        this.temporaryDataStorage = temporaryDataStorage;
        this.documentOutputStream = documentOutputStream;
        this.tmpFile = File.createTempFile("eid-dss-", ".asice");
        FileOutputStream fileOutputStream;
        fileOutputStream = new FileOutputStream(this.tmpFile);
        IOUtils.copy(documentInputStream, fileOutputStream);
        addSignatureFacet(new ASiCSignatureFacet(this.tmpFile, digestAlgo));
        XAdESSignatureFacet xadesSignatureFacet = new XAdESSignatureFacet(getSignatureDigestAlgorithm());
        xadesSignatureFacet.setRole(claimedRole);
        xadesSignatureFacet.setXadesNamespacePrefix("xades");
        addSignatureFacet(xadesSignatureFacet);
        addSignatureFacet(new XAdESXLSignatureFacet(timeStampService, revocationDataService, getSignatureDigestAlgorithm()));
        addSignatureFacet(new KeyInfoSignatureFacet(true, false, false));
        if (null != identity) {
            IdentitySignatureFacet identitySignatureFacet = new IdentitySignatureFacet(identity, photo, getSignatureDigestAlgorithm());
            addSignatureFacet(identitySignatureFacet);
        }
    }

    @Override
    protected String getSignatureDescription() {
        return "Associated Signature Container";
    }

    public String getFilesDigestAlgorithm() {
        return null;
    }

    @Override
    protected TemporaryDataStorage getTemporaryDataStorage() {
        return this.temporaryDataStorage;
    }

    @Override
    protected URIDereferencer getURIDereferencer() {
        return new ASiCURIDereferencer(this.tmpFile);
    }

    @Override
    protected OutputStream getSignedDocumentOutputStream() {
        return new ASiCSignatureOutputStream(this.tmpFile, new CloseActionOutputStream(this.documentOutputStream, new CloseAction()));
    }

    private class CloseAction implements Runnable {

        public void run() {
            AbstractASiCSignatureService.this.tmpFile.delete();
        }
    }

    public DigestInfo preSign(List<DigestInfo> digestInfos, List<X509Certificate> signingCertificateChain, IdentityDTO identity, AddressDTO address, byte[] photo) throws NoSuchAlgorithmException {
        return super.preSign(digestInfos, signingCertificateChain);
    }

    @Override
    protected Document getEnvelopingDocument() throws ParserConfigurationException, IOException, SAXException {
        FileInputStream fileInputStream = new FileInputStream(this.tmpFile);
        ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
        ZipEntry zipEntry;
        while (null != (zipEntry = zipInputStream.getNextEntry())) {
            if (ASiCUtil.isSignatureZipEntry(zipEntry)) {
                Document documentSignaturesDocument = ODFUtil.loadDocument(zipInputStream);
                return documentSignaturesDocument;
            }
        }
        Document document = ASiCUtil.createNewSignatureDocument();
        return document;
    }
}
