package be.fedict.eid.dss.document.odf;

import be.fedict.eid.applet.service.signer.HttpSessionTemporaryDataStorage;
import be.fedict.eid.applet.service.signer.DigestAlgo;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.TemporaryDataStorage;
import be.fedict.eid.applet.service.signer.facets.IdentitySignatureFacet;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.facets.XAdESSignatureFacet;
import be.fedict.eid.applet.service.signer.facets.XAdESXLSignatureFacet;
import be.fedict.eid.applet.service.signer.odf.AbstractODFSignatureService;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;
import be.fedict.eid.applet.service.spi.AddressDTO;
import be.fedict.eid.applet.service.spi.DigestInfo;
import be.fedict.eid.applet.service.spi.IdentityDTO;
import be.fedict.eid.applet.service.spi.SignatureServiceEx;
import be.fedict.eid.dss.spi.utils.CloseActionOutputStream;
import org.apache.commons.io.IOUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;

public class ODFSignatureService extends AbstractODFSignatureService implements SignatureServiceEx {

    private final TemporaryDataStorage temporaryDataStorage;

    private final OutputStream documentOutputStream;

    private final File tmpFile;

    public ODFSignatureService(TimeStampServiceValidator timeStampServiceValidator, RevocationDataService revocationDataService, SignatureFacet signatureFacet, InputStream documentInputStream, OutputStream documentOutputStream, TimeStampService timeStampService, String role, IdentityDTO identity, byte[] photo, DigestAlgo digestAlgo) throws Exception {
        super(digestAlgo);
        this.temporaryDataStorage = new HttpSessionTemporaryDataStorage();
        this.documentOutputStream = documentOutputStream;
        this.tmpFile = File.createTempFile("eid-dss-", ".odf");
        FileOutputStream fileOutputStream;
        fileOutputStream = new FileOutputStream(this.tmpFile);
        IOUtils.copy(documentInputStream, fileOutputStream);
        addSignatureFacet(new XAdESXLSignatureFacet(timeStampService, revocationDataService, getSignatureDigestAlgorithm()));
        addSignatureFacet(signatureFacet);
        XAdESSignatureFacet xadesSignatureFacet = super.getXAdESSignatureFacet();
        xadesSignatureFacet.setRole(role);
        if (null != identity) {
            IdentitySignatureFacet identitySignatureFacet = new IdentitySignatureFacet(identity, photo, getSignatureDigestAlgorithm());
            addSignatureFacet(identitySignatureFacet);
        }
    }

    @Override
    protected URL getOpenDocumentURL() {
        try {
            return this.tmpFile.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("URL error: " + e.getMessage(), e);
        }
    }

    @Override
    protected OutputStream getSignedOpenDocumentOutputStream() {
        return new CloseActionOutputStream(this.documentOutputStream, new CloseAction());
    }

    private class CloseAction implements Runnable {

        public void run() {
            ODFSignatureService.this.tmpFile.delete();
        }
    }

    @Override
    protected TemporaryDataStorage getTemporaryDataStorage() {
        return this.temporaryDataStorage;
    }

    public DigestInfo preSign(List<DigestInfo> digestInfos, List<X509Certificate> signingCertificateChain, IdentityDTO identity, AddressDTO address, byte[] photo) throws NoSuchAlgorithmException {
        return super.preSign(digestInfos, signingCertificateChain);
    }
}
