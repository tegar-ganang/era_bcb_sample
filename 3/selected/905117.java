package org.signserver.module.mrtdsodsigner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x509.X509Name;
import org.ejbca.util.CertTools;
import org.signserver.module.mrtdsodsigner.jmrtd.SODFile;
import org.signserver.common.ArchiveData;
import org.signserver.common.CryptoTokenAuthenticationFailureException;
import org.signserver.common.CryptoTokenOfflineException;
import org.signserver.common.ISignRequest;
import org.signserver.common.IllegalRequestException;
import org.signserver.common.ProcessRequest;
import org.signserver.common.ProcessResponse;
import org.signserver.common.RequestContext;
import org.signserver.common.SODSignRequest;
import org.signserver.common.SODSignResponse;
import org.signserver.common.SignServerException;
import org.signserver.common.SignerStatus;
import org.signserver.server.cryptotokens.ICryptoToken;
import org.signserver.server.signers.BaseSigner;

/**
 * A Signer signing creating a signed Security Object Data (SOD) file to be stored in ePassports.
 *
 * Properties:
 * <ul>
 *  <li>DIGESTALGORITHM = Message digest algorithm that is applied or should be applied to the values. (Optional)</li>
 *  <li>SIGNATUREALGORITHM = Signature algorithm for signing the SO(d), should match
 *  the digest algorithm. (Optional)</li>
 *  <li>DODATAGROUPHASHING = True if this signer first should hash to values. Otherwise
 * the values are assumed to be hashes</li>
 * </ul>
 *
 * @author Tomas Gustavsson
 * @author Markus Kilås
 * @version $Id: MRTDSODSigner.java 1843 2011-08-14 14:42:39Z netmackan $
 */
public class MRTDSODSigner extends BaseSigner {

    private static final Logger log = Logger.getLogger(MRTDSODSigner.class);

    /** The digest algorithm, for example SHA1, SHA256. Defaults to SHA256. */
    private static final String PROPERTY_DIGESTALGORITHM = "DIGESTALGORITHM";

    /** Default value for the digestAlgorithm property */
    private static final String DEFAULT_DIGESTALGORITHM = "SHA256";

    /** The signature algorithm, for example SHA1withRSA, SHA256withRSA, SHA256withECDSA. Defaults to SHA256withRSA. */
    private static final String PROPERTY_SIGNATUREALGORITHM = "SIGNATUREALGORITHM";

    /** Default value for the signature algorithm property */
    private static final String DEFAULT_SIGNATUREALGORITHM = "SHA256withRSA";

    /** Determines if the the data group values should be hashed by the signer. If false we assume they are already hashed. */
    private static final String PROPERTY_DODATAGROUPHASHING = "DODATAGROUPHASHING";

    /** Default value if the data group values should be hashed by the signer. */
    private static final String DEFAULT_DODATAGROUPHASHING = "false";

    /** Determines which version of the LDS to use. */
    private static final String PROPERTY_LDSVERSION = "LDSVERSION";

    /** Default value if the LDS version is not specified. */
    private static final String DEFAULT_LDSVERSION = "0107";

    /** Determines which version of Unicode to set. */
    private static final String PROPERTY_UNICODEVERSION = "UNICODEVERSION";

    private static final Object syncObj = new Object();

    public ProcessResponse processData(ProcessRequest signRequest, RequestContext requestContext) throws IllegalRequestException, CryptoTokenOfflineException, SignServerException {
        if (log.isTraceEnabled()) {
            log.trace(">processData");
        }
        ProcessResponse ret = null;
        final ISignRequest sReq = (ISignRequest) signRequest;
        if (!(signRequest instanceof SODSignRequest)) {
            throw new IllegalRequestException("Recieved request wasn't an expected SODSignRequest.");
        }
        final SODSignRequest sodRequest = (SODSignRequest) signRequest;
        final ICryptoToken token = getCryptoToken();
        synchronized (syncObj) {
            int status = token.getCryptoTokenStatus();
            if (log.isDebugEnabled()) {
                log.debug("Crypto token status: " + status);
            }
            if (status != SignerStatus.STATUS_ACTIVE) {
                log.info("Crypto token status is not active, will see if we can autoactivate.");
                String pin = config.getProperty("PIN");
                if (pin == null) {
                    pin = config.getProperty("pin");
                }
                if (pin != null) {
                    log.info("Deactivating and re-activating crypto token.");
                    token.deactivate();
                    try {
                        token.activate(pin);
                    } catch (CryptoTokenAuthenticationFailureException e) {
                        throw new CryptoTokenOfflineException(e);
                    }
                } else {
                    log.info("Autoactivation not enabled, can not re-activate crypto token.");
                }
            }
        }
        final X509Certificate cert = (X509Certificate) getSigningCertificate();
        final PrivateKey privKey = token.getPrivateKey(ICryptoToken.PURPOSE_SIGN);
        final String provider = token.getProvider(ICryptoToken.PURPOSE_SIGN);
        if (cert == null) {
            throw new CryptoTokenOfflineException("No signing certificate");
        }
        if (log.isDebugEnabled()) {
            log.debug("Using signer certificate with subjectDN '" + CertTools.getSubjectDN(cert) + "', issuerDN '" + CertTools.getIssuerDN(cert) + ", serNo " + CertTools.getSerialNumberAsString(cert));
        }
        final SODFile sod;
        try {
            final String digestAlgorithm = config.getProperty(PROPERTY_DIGESTALGORITHM, DEFAULT_DIGESTALGORITHM);
            final String digestEncryptionAlgorithm = config.getProperty(PROPERTY_SIGNATUREALGORITHM, DEFAULT_SIGNATUREALGORITHM);
            if (log.isDebugEnabled()) {
                log.debug("Using algorithms " + digestAlgorithm + ", " + digestEncryptionAlgorithm);
            }
            final String doHashing = config.getProperty(PROPERTY_DODATAGROUPHASHING, DEFAULT_DODATAGROUPHASHING);
            final Map<Integer, byte[]> dgvalues = sodRequest.getDataGroupHashes();
            Map<Integer, byte[]> dghashes = dgvalues;
            if (StringUtils.equalsIgnoreCase(doHashing, "true")) {
                if (log.isDebugEnabled()) {
                    log.debug("Converting data group values to hashes using algorithm " + digestAlgorithm);
                }
                dghashes = new HashMap<Integer, byte[]>(16);
                for (Integer dgId : dgvalues.keySet()) {
                    byte[] value = dgvalues.get(dgId);
                    if (log.isDebugEnabled()) {
                        log.debug("Hashing data group " + dgId + ", value is of length: " + value.length);
                    }
                    if ((value != null) && (value.length > 0)) {
                        MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
                        byte[] result = digest.digest(value);
                        if (log.isDebugEnabled()) {
                            log.debug("Resulting hash is of length: " + result.length);
                        }
                        dghashes.put(dgId, result);
                    }
                }
            }
            String ldsVersion = config.getProperty(PROPERTY_LDSVERSION, DEFAULT_LDSVERSION);
            String unicodeVersion = config.getProperty(PROPERTY_UNICODEVERSION);
            final String ldsVersionRequest = sodRequest.getLdsVersion();
            if (ldsVersionRequest != null) {
                ldsVersion = ldsVersionRequest;
            }
            final String unicodeVersionRequest = sodRequest.getUnicodeVersion();
            if (unicodeVersionRequest != null) {
                unicodeVersion = unicodeVersionRequest;
            }
            if ("0107".equals(ldsVersion)) {
                ldsVersion = null;
                unicodeVersion = null;
            } else if ("0108".equals(ldsVersion)) {
                if (unicodeVersion == null) {
                    throw new IllegalRequestException("Unicode version must be specified in LDS version 1.8");
                }
            } else {
                throw new IllegalRequestException("Unsupported LDS version: " + ldsVersion);
            }
            if (log.isDebugEnabled()) {
                log.debug("LDS version: " + ldsVersion + ", unicodeVerison: " + unicodeVersion);
            }
            final SODFile constructedSod = new SODFile(digestAlgorithm, digestEncryptionAlgorithm, dghashes, privKey, cert, provider, ldsVersion, unicodeVersion);
            sod = new SODFile(new ByteArrayInputStream(constructedSod.getEncoded()));
        } catch (NoSuchAlgorithmException ex) {
            throw new SignServerException("Problem constructing SOD", ex);
        } catch (CertificateException ex) {
            throw new SignServerException("Problem constructing SOD", ex);
        } catch (IOException ex) {
            throw new SignServerException("Problem reconstructing SOD", ex);
        }
        try {
            verifySignatureAndChain(sod, getSigningCertificateChain());
            if (log.isDebugEnabled()) {
                log.debug("SOD verified correctly, returning SOD.");
            }
            final byte[] signedbytes = sod.getEncoded();
            String fp = CertTools.getFingerprintAsString(signedbytes);
            ret = new SODSignResponse(sReq.getRequestID(), signedbytes, cert, fp, new ArchiveData(signedbytes));
        } catch (GeneralSecurityException e) {
            log.error("Error verifying the SOD we signed ourselves. ", e);
            throw new SignServerException("SOD verification failure", e);
        }
        if (log.isTraceEnabled()) {
            log.trace("<processData");
        }
        return ret;
    }

    private X509Certificate findIssuerCert(Collection<Certificate> chain, X509Certificate sodCert) {
        X509Certificate result = null;
        final X509Name issuer = new X509Name(sodCert.getIssuerX500Principal().getName());
        if (log.isDebugEnabled()) {
            final StringBuilder buff = new StringBuilder();
            buff.append("Looking for ");
            buff.append(issuer);
            log.debug(buff.toString());
        }
        for (Certificate cert : chain) {
            if (cert instanceof X509Certificate) {
                final X509Certificate x509 = (X509Certificate) cert;
                final X509Name subject = new X509Name(x509.getSubjectX500Principal().getName());
                if (issuer.equals(subject)) {
                    result = (X509Certificate) cert;
                    if (log.isDebugEnabled()) {
                        log.debug("Found issuer");
                    }
                    break;
                } else {
                    if (log.isDebugEnabled()) {
                        final StringBuilder buff = new StringBuilder();
                        buff.append(issuer);
                        buff.append("!=");
                        buff.append(subject);
                        log.debug(buff.toString());
                    }
                }
            }
        }
        return result;
    }

    private void verifySignatureAndChain(final SODFile sod, final Collection<Certificate> chain) throws GeneralSecurityException {
        try {
            if (log.isDebugEnabled()) {
                final StringBuilder buff = new StringBuilder();
                buff.append("Verifying SOD signed by DS with issuer: ");
                buff.append(sod.toString());
                log.debug(buff.toString());
            }
            final X509Certificate sodCert = sod.getDocSigningCertificate();
            final CertificateFactory factory = CertificateFactory.getInstance("X.509", "BC");
            final X509Certificate signerCert = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(sodCert.getEncoded()));
            final boolean consistent = sod.checkDocSignature(signerCert);
            if (!consistent) {
                log.error("Failed to verify the SOD we signed ourselves.");
                log.error("Cert: " + signerCert);
                log.error("SOD: " + sod);
                throw new GeneralSecurityException("Signature not consistent");
            }
            final X509Certificate issuerCert = (chain == null ? null : findIssuerCert(chain, signerCert));
            if (issuerCert == null) {
                log.error("Failed to verify certificate chain");
                log.error("Cert: " + signerCert);
                log.error("SOD Cert: " + signerCert);
                log.error("Chain: " + chain);
                throw new GeneralSecurityException("Issuer of cert not in chain");
            }
            signerCert.verify(issuerCert.getPublicKey());
        } catch (IOException e) {
            log.error("Getting signer certificate from SOD failed", e);
            throw new GeneralSecurityException("Getting signer certificate from SOD failed", e);
        }
    }
}
