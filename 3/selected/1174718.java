package org.apache.ws.security.components.crypto;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSSecurityException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: dims
 * Date: Sep 15, 2005
 * Time: 9:50:40 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class CryptoBase implements Crypto {

    private static Log log = LogFactory.getLog(CryptoBase.class);

    protected static CertificateFactory certFact;

    protected KeyStore keystore = null;

    static String SKI_OID = "2.5.29.14";

    protected KeyStore cacerts = null;

    /**
     * Constructor
     *
     * @param properties
     */
    protected CryptoBase() {
    }

    /**
     * @return      a crypto provider name.  This operation should
     *              return null if the default crypto provider should
     *              be used.
     */
    protected abstract String getCryptoProvider();

    /**
     * Singleton certificate factory for this Crypto instance.
     * <p/>
     *
     * @return Returns a <code>CertificateFactory</code> to construct
     *         X509 certficates
     * @throws org.apache.ws.security.WSSecurityException
     *
     */
    public synchronized CertificateFactory getCertificateFactory() throws WSSecurityException {
        if (certFact == null) {
            try {
                String provider = getCryptoProvider();
                if (provider == null || provider.length() == 0) {
                    certFact = CertificateFactory.getInstance("X.509");
                } else {
                    certFact = CertificateFactory.getInstance("X.509", provider);
                }
            } catch (CertificateException e) {
                throw new WSSecurityException(WSSecurityException.SECURITY_TOKEN_UNAVAILABLE, "unsupportedCertType");
            } catch (NoSuchProviderException e) {
                throw new WSSecurityException(WSSecurityException.SECURITY_TOKEN_UNAVAILABLE, "noSecProvider");
            }
        }
        return certFact;
    }

    /**
     * load a X509Certificate from the input stream.
     * <p/>
     *
     * @param in The <code>InputStream</code> array containing the X509 data
     * @return Returns a X509 certificate
     * @throws org.apache.ws.security.WSSecurityException
     *
     */
    public X509Certificate loadCertificate(InputStream in) throws WSSecurityException {
        X509Certificate cert = null;
        try {
            cert = (X509Certificate) getCertificateFactory().generateCertificate(in);
        } catch (CertificateException e) {
            throw new WSSecurityException(WSSecurityException.SECURITY_TOKEN_UNAVAILABLE, "parseError");
        }
        return cert;
    }

    /**
     * Gets the private key identified by <code>alias</> and <code>password</code>.
     * <p/>
     *
     * @param alias    The alias (<code>KeyStore</code>) of the key owner
     * @param password The password needed to access the private key
     * @return The private key
     * @throws Exception
     */
    public PrivateKey getPrivateKey(String alias, String password) throws Exception {
        if (alias == null) {
            throw new Exception("alias is null");
        }
        boolean b = keystore.isKeyEntry(alias);
        if (!b) {
            log.error("Cannot find key for alias: " + alias);
            throw new Exception("Cannot find key for alias: " + alias);
        }
        Key keyTmp = keystore.getKey(alias, password.toCharArray());
        if (!(keyTmp instanceof PrivateKey)) {
            throw new Exception("Key is not a private key, alias: " + alias);
        }
        return (PrivateKey) keyTmp;
    }

    protected Vector splitAndTrim(String inString) {
        X509NameTokenizer nmTokens = new X509NameTokenizer(inString);
        Vector vr = new Vector();
        while (nmTokens.hasMoreTokens()) {
            vr.add(nmTokens.nextToken());
        }
        java.util.Collections.sort(vr);
        return vr;
    }

    /**
     * Lookup a X509 Certificate in the keystore according to a given
     * the issuer of a Certficate.
     * <p/>
     * The search gets all alias names of the keystore and gets the certificate chain
     * for each alias. Then the Issuer fo each certificate of the chain
     * is compared with the parameters.
     *
     * @param issuer The issuer's name for the certificate
     * @return alias name of the certificate that matches the issuer name
     *         or null if no such certificate was found.
     */
    public String getAliasForX509Cert(String issuer) throws WSSecurityException {
        return getAliasForX509Cert(issuer, null, false);
    }

    /**
     * Lookup a X509 Certificate in the keystore according to a given serial number and
     * the issuer of a Certificate.
     * <p/>
     * The search gets all alias names of the keystore and gets the certificate chain
     * for each alias. Then the SerialNumber and Issuer fo each certificate of the chain
     * is compared with the parameters.
     *
     * @param issuer       The issuer's name for the certificate
     * @param serialNumber The serial number of the certificate from the named issuer
     * @return alias name of the certificate that matches serialNumber and issuer name
     *         or null if no such certificate was found.
     */
    public String getAliasForX509Cert(String issuer, BigInteger serialNumber) throws WSSecurityException {
        return getAliasForX509Cert(issuer, serialNumber, true);
    }

    private String getAliasForX509Cert(String issuer, BigInteger serialNumber, boolean useSerialNumber) throws WSSecurityException {
        Vector issuerRDN = splitAndTrim(issuer);
        X509Certificate x509cert = null;
        Vector certRDN = null;
        Certificate cert = null;
        try {
            for (Enumeration e = keystore.aliases(); e.hasMoreElements(); ) {
                String alias = (String) e.nextElement();
                Certificate[] certs = keystore.getCertificateChain(alias);
                if (certs == null || certs.length == 0) {
                    cert = keystore.getCertificate(alias);
                    if (cert == null) {
                        return null;
                    }
                } else {
                    cert = certs[0];
                }
                if (!(cert instanceof X509Certificate)) {
                    continue;
                }
                x509cert = (X509Certificate) cert;
                if (!useSerialNumber || useSerialNumber && x509cert.getSerialNumber().compareTo(serialNumber) == 0) {
                    certRDN = splitAndTrim(x509cert.getIssuerDN().getName());
                    if (certRDN.equals(issuerRDN)) {
                        return alias;
                    }
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "keystore");
        }
        return null;
    }

    /**
     * Lookup a X509 Certificate in the keystore according to a given
     * SubjectKeyIdentifier.
     * <p/>
     * The search gets all alias names of the keystore and gets the certificate chain
     * or certificate for each alias. Then the SKI for each user certificate
     * is compared with the SKI parameter.
     *
     * @param skiBytes The SKI info bytes
     * @return alias name of the certificate that matches serialNumber and issuer name
     *         or null if no such certificate was found.
     * @throws org.apache.ws.security.WSSecurityException
     *          if problems during keystore handling or wrong certificate (no SKI data)
     */
    public String getAliasForX509Cert(byte[] skiBytes) throws WSSecurityException {
        Certificate cert = null;
        boolean found = false;
        try {
            for (Enumeration e = keystore.aliases(); e.hasMoreElements(); ) {
                String alias = (String) e.nextElement();
                Certificate[] certs = keystore.getCertificateChain(alias);
                if (certs == null || certs.length == 0) {
                    cert = keystore.getCertificate(alias);
                    if (cert == null) {
                        return null;
                    }
                } else {
                    cert = certs[0];
                }
                if (!(cert instanceof X509Certificate)) {
                    continue;
                }
                byte[] data = getSKIBytesFromCert((X509Certificate) cert);
                if (data.length != skiBytes.length) {
                    continue;
                }
                if (Arrays.equals(data, skiBytes)) {
                    return alias;
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "keystore");
        }
        return null;
    }

    public String getAliasForX509Cert(Certificate cert) throws WSSecurityException {
        try {
            String alias = keystore.getCertificateAlias(cert);
            if (alias != null) return alias;
            Enumeration e = keystore.aliases();
            while (e.hasMoreElements()) {
                alias = (String) e.nextElement();
                X509Certificate cert2 = (X509Certificate) keystore.getCertificate(alias);
                if (cert2.equals(cert)) {
                    return alias;
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "keystore");
        }
        return null;
    }

    /**
     * Gets the list of certificates for a given alias.
     * <p/>
     *
     * @param alias Lookup certificate chain for this alias
     * @return Array of X509 certificates for this alias name, or
     *         null if this alias does not exist in the keystore
     */
    public X509Certificate[] getCertificates(String alias) throws WSSecurityException {
        Certificate[] certs = null;
        Certificate cert = null;
        try {
            if (this.keystore != null) {
                certs = keystore.getCertificateChain(alias);
                if (certs == null || certs.length == 0) {
                    cert = keystore.getCertificate(alias);
                }
            }
            if (certs == null && cert == null && cacerts != null) {
                certs = cacerts.getCertificateChain(alias);
                if (certs == null) {
                    cert = cacerts.getCertificate(alias);
                }
            }
            if (cert != null) {
                certs = new Certificate[] { cert };
            } else if (certs == null) {
                return null;
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "keystore");
        }
        X509Certificate[] x509certs = new X509Certificate[certs.length];
        for (int i = 0; i < certs.length; i++) {
            x509certs[i] = (X509Certificate) certs[i];
        }
        return x509certs;
    }

    /**
     * Lookup a X509 Certificate in the keystore according to a given
     * Thumbprint.
     * <p/>
     * The search gets all alias names of the keystore, then reads the certificate chain
     * or certificate for each alias. Then the thumbprint for each user certificate
     * is compared with the thumbprint parameter.
     *
     * @param thumb The SHA1 thumbprint info bytes
     * @return alias name of the certificate that matches the thumbprint
     *         or null if no such certificate was found.
     * @throws org.apache.ws.security.WSSecurityException
     *          if problems during keystore handling or wrong certificate
     */
    public String getAliasForX509CertThumb(byte[] thumb) throws WSSecurityException {
        Certificate cert = null;
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e1) {
            throw new WSSecurityException(0, "noSHA1availabe");
        }
        try {
            for (Enumeration e = keystore.aliases(); e.hasMoreElements(); ) {
                String alias = (String) e.nextElement();
                Certificate[] certs = keystore.getCertificateChain(alias);
                if (certs == null || certs.length == 0) {
                    cert = keystore.getCertificate(alias);
                    if (cert == null) {
                        return null;
                    }
                } else {
                    cert = certs[0];
                }
                if (!(cert instanceof X509Certificate)) {
                    continue;
                }
                sha.reset();
                try {
                    sha.update(cert.getEncoded());
                } catch (CertificateEncodingException e1) {
                    throw new WSSecurityException(WSSecurityException.SECURITY_TOKEN_UNAVAILABLE, "encodeError");
                }
                byte[] data = sha.digest();
                if (Arrays.equals(data, thumb)) {
                    return alias;
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "keystore");
        }
        return null;
    }

    /**
     * A Hook for subclasses to set the keystore without having to
     * load it from an <code>InputStream</code>.
     *
     * @param ks existing keystore
     */
    public void setKeyStore(KeyStore ks) {
        keystore = ks;
    }

    /**
     * Reads the SubjectKeyIdentifier information from the certificate.
     * <p/>
     * If the the certificate does not contain a SKI extension then
     * try to compute the SKI according to RFC3280 using the
     * SHA-1 hash value of the public key. The second method described
     * in RFC3280 is not support. Also only RSA public keys are supported.
     * If we cannot compute the SKI throw a WSSecurityException.
     *
     * @param cert The certificate to read SKI
     * @return The byte array containing the binary SKI data
     */
    public byte[] getSKIBytesFromCert(X509Certificate cert) throws WSSecurityException {
        byte[] derEncodedValue = cert.getExtensionValue(SKI_OID);
        if (cert.getVersion() < 3 || derEncodedValue == null) {
            PublicKey key = cert.getPublicKey();
            if (!(key instanceof RSAPublicKey)) {
                throw new WSSecurityException(1, "noSKIHandling", new Object[] { "Support for RSA key only" });
            }
            byte[] encoded = key.getEncoded();
            byte[] value = new byte[encoded.length - 22];
            System.arraycopy(encoded, 22, value, 0, value.length);
            MessageDigest sha;
            try {
                sha = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException ex) {
                throw new WSSecurityException(1, "noSKIHandling", new Object[] { "Wrong certificate version (<3) and no SHA1 message digest availabe" });
            }
            sha.reset();
            sha.update(value);
            return sha.digest();
        }
        byte abyte0[] = new byte[derEncodedValue.length - 4];
        System.arraycopy(derEncodedValue, 4, abyte0, 0, abyte0.length);
        return abyte0;
    }

    public KeyStore getKeyStore() {
        return this.keystore;
    }

    /**
     * Lookup X509 Certificates in the keystore according to a given DN of the subject of the certificate
     * <p/>
     * The search gets all alias names of the keystore and gets the certificate (chain)
     * for each alias. Then the DN of the certificate is compared with the parameters.
     *
     * @param subjectDN The DN of subject to look for in the keystore
     * @return Vector with all alias of certificates with the same DN as given in the parameters
     * @throws org.apache.ws.security.WSSecurityException
     *
     */
    public String[] getAliasesForDN(String subjectDN) throws WSSecurityException {
        Vector subjectRDN = splitAndTrim(subjectDN);
        Vector aliases = getAlias(subjectRDN, keystore);
        if (aliases.size() == 0) {
            aliases = getAlias(subjectRDN, cacerts);
        }
        String[] result = new String[aliases.size()];
        for (int i = 0; i < aliases.size(); i++) result[i] = (String) aliases.elementAt(i);
        return result;
    }

    /**
     * get a byte array given an array of X509 certificates.
     * <p/>
     *
     * @param reverse If set the first certificate in the array data will
     *                the last in the byte array
     * @param certs   The certificates to convert
     * @return The byte array for the certificates ordered according
     *         to the reverse flag
     * @throws WSSecurityException
     */
    public byte[] getCertificateData(boolean reverse, X509Certificate[] certs) throws WSSecurityException {
        Vector list = new Vector();
        for (int i = 0; i < certs.length; i++) {
            if (reverse) {
                list.insertElementAt(certs[i], 0);
            } else {
                list.add(certs[i]);
            }
        }
        try {
            CertPath path = getCertificateFactory().generateCertPath(list);
            return path.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new WSSecurityException(WSSecurityException.SECURITY_TOKEN_UNAVAILABLE, "encodeError");
        } catch (CertificateException e) {
            throw new WSSecurityException(WSSecurityException.SECURITY_TOKEN_UNAVAILABLE, "parseError");
        }
    }

    /**
     * Construct an array of X509Certificate's from the byte array.
     * <p/>
     *
     * @param data    The <code>byte</code> array containing the X509 data
     * @param reverse If set the first certificate in input data will
     *                the last in the array
     * @return An array of X509 certificates, ordered according to
     *         the reverse flag
     * @throws WSSecurityException
     */
    public X509Certificate[] getX509Certificates(byte[] data, boolean reverse) throws WSSecurityException {
        InputStream in = new ByteArrayInputStream(data);
        CertPath path = null;
        try {
            path = getCertificateFactory().generateCertPath(in);
        } catch (CertificateException e) {
            throw new WSSecurityException(WSSecurityException.SECURITY_TOKEN_UNAVAILABLE, "parseError");
        }
        List l = path.getCertificates();
        X509Certificate[] certs = new X509Certificate[l.size()];
        Iterator iterator = l.iterator();
        for (int i = 0; i < l.size(); i++) {
            certs[(reverse) ? (l.size() - 1 - i) : i] = (X509Certificate) iterator.next();
        }
        return certs;
    }

    /**
     * Overridden because there's a bug in the base class where they don't use
     * the provider variant for the certificate validator.
     *
     * @param certs
     *            Certificate chain to validate
     * @return true if the certificate chain is valid, false otherwise
     * @throws WSSecurityException
     */
    public boolean validateCertPath(java.security.cert.X509Certificate[] certs) throws org.apache.ws.security.WSSecurityException {
        try {
            java.util.List cert_list = java.util.Arrays.asList(certs);
            java.security.cert.CertPath path = getCertificateFactory().generateCertPath(cert_list);
            java.security.cert.PKIXParameters param = new java.security.cert.PKIXParameters(this.keystore);
            param.setRevocationEnabled(false);
            String provider = getCryptoProvider();
            java.security.cert.CertPathValidator validator = null;
            if (provider == null || provider.length() == 0) {
                validator = java.security.cert.CertPathValidator.getInstance("PKIX");
            } else {
                validator = java.security.cert.CertPathValidator.getInstance("PKIX", provider);
            }
            validator.validate(path, param);
        } catch (java.security.NoSuchProviderException e) {
            throw new org.apache.ws.security.WSSecurityException(org.apache.ws.security.WSSecurityException.FAILURE, "certpath", new Object[] { e.getMessage() }, e);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new org.apache.ws.security.WSSecurityException(org.apache.ws.security.WSSecurityException.FAILURE, "certpath", new Object[] { e.getMessage() }, e);
        } catch (java.security.cert.CertificateException e) {
            throw new org.apache.ws.security.WSSecurityException(org.apache.ws.security.WSSecurityException.FAILURE, "certpath", new Object[] { e.getMessage() }, e);
        } catch (java.security.InvalidAlgorithmParameterException e) {
            throw new org.apache.ws.security.WSSecurityException(org.apache.ws.security.WSSecurityException.FAILURE, "certpath", new Object[] { e.getMessage() }, e);
        } catch (java.security.cert.CertPathValidatorException e) {
            throw new org.apache.ws.security.WSSecurityException(org.apache.ws.security.WSSecurityException.FAILURE, "certpath", new Object[] { e.getMessage() }, e);
        } catch (java.security.KeyStoreException e) {
            throw new org.apache.ws.security.WSSecurityException(org.apache.ws.security.WSSecurityException.FAILURE, "certpath", new Object[] { e.getMessage() }, e);
        }
        return true;
    }

    private Vector getAlias(Vector subjectRDN, KeyStore store) throws WSSecurityException {
        Vector aliases = new Vector();
        Certificate cert = null;
        try {
            for (Enumeration e = store.aliases(); e.hasMoreElements(); ) {
                String alias = (String) e.nextElement();
                Certificate[] certs = store.getCertificateChain(alias);
                if (certs == null || certs.length == 0) {
                    cert = store.getCertificate(alias);
                    if (cert == null) {
                        return null;
                    }
                    certs = new Certificate[] { cert };
                } else {
                    cert = certs[0];
                }
                if (cert instanceof X509Certificate) {
                    Vector foundRDN = splitAndTrim(((X509Certificate) cert).getSubjectDN().getName());
                    if (subjectRDN.equals(foundRDN)) {
                        aliases.add(alias);
                    }
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "keystore");
        }
        return aliases;
    }
}
