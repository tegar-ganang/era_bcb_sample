package alto.sec.x509;

import alto.io.Code;
import alto.io.Check;
import alto.io.u.Bbuf;
import alto.io.u.Hex;
import alto.hash.Function;
import alto.sec.util.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Principal;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CRLException;
import java.util.*;

/**
 * <p>
 * An implmentation for X509 CRL (Certificate Revocation List).
 * <p>
 * The X.509 v2 CRL format is described below in ASN.1:
 * <pre>
 * CertificateList  ::=  SEQUENCE  {
 *     tbsCertList          TBSCertList,
 *     signatureAlgorithm   AlgorithmIdentifier,
 *     signature            BIT STRING  }
 * </pre>
 * More information can be found in
 * <a href="http://www.ietf.org/rfc/rfc3280.txt">RFC 3280: Internet X.509
 * Public Key Infrastructure Certificate and CRL Profile</a>.
 * <p>
 * The ASN.1 definition of <code>tbsCertList</code> is:
 * <pre>
 * TBSCertList  ::=  SEQUENCE  {
 *     version                 Version OPTIONAL,
 *                             -- if present, must be v2
 *     signature               AlgorithmIdentifier,
 *     issuer                  Name,
 *     thisUpdate              ChoiceOfTime,
 *     nextUpdate              ChoiceOfTime OPTIONAL,
 *     revokedCertificates     SEQUENCE OF SEQUENCE  {
 *         userCertificate         CertificateSerialNumber,
 *         revocationDate          ChoiceOfTime,
 *         crlEntryExtensions      Extensions OPTIONAL
 *                                 -- if present, must be v2
 *         }  OPTIONAL,
 *     crlExtensions           [0]  EXPLICIT Extensions OPTIONAL
 *                                  -- if present, must be v2
 *     }
 * </pre>
 *
 * @author Hemma Prafullchandra
 * @see X509CRL
 */
public class X509CRL extends java.security.cert.CRL implements java.security.cert.X509Extension {

    /**
     * Immutable X.509 Certificate Issuer DN and serial number pair
     */
    private static final class X509IssuerSerial {

        final X500Name issuer;

        final BigInteger serial;

        volatile int hashcode = 0;

        /**
         * Create an X509IssuerSerial.
         *
         * @param issuer the issuer DN
         * @param serial the serial number
         */
        X509IssuerSerial(X500Name issuer, BigInteger serial) {
            this.issuer = issuer;
            this.serial = serial;
        }

        /**
         * Construct an X509IssuerSerial from an X509Certificate.
         */
        X509IssuerSerial(X509Certificate cert) {
            this(cert.getIssuerX500Principal(), cert.getSerialNumber());
        }

        /**
         * Returns the issuer.
         *
         * @return the issuer
         */
        X500Name getIssuer() {
            return issuer;
        }

        /**
         * Returns the serial number.
         *
         * @return the serial number
         */
        BigInteger getSerial() {
            return serial;
        }

        /**
         * Compares this X509Serial with another and returns true if they
         * are equivalent.
         *
         * @param o the other object to compare with
         * @return true if equal, false otherwise
         */
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof X509IssuerSerial)) {
                return false;
            }
            X509IssuerSerial other = (X509IssuerSerial) o;
            if (serial.equals(other.getSerial()) && issuer.equals(other.getIssuer())) {
                return true;
            }
            return false;
        }

        /**
         * Returns a hash code value for this X509IssuerSerial.
         *
         * @return the hash code value
         */
        public int hashCode() {
            if (hashcode == 0) {
                int result = 17;
                result = 37 * result + issuer.hashCode();
                result = 37 * result + serial.hashCode();
                hashcode = result;
            }
            return hashcode;
        }
    }

    /**
     * Returns the X500 certificate issuer DN of a CRL entry.
     *
     * @param entry the entry to check
     * @param prevCertIssuer the previous entry's certificate issuer
     * @return the X500Name in a CertificateIssuerExtension, or
     *   prevCertIssuer if it does not exist
     */
    private static final X500Name GetCertIssuer(X509CRLEntry entry, X500Name prevCertIssuer) throws IOException {
        CertificateIssuerExtension ciExt = entry.getCertificateIssuerExtension();
        if (ciExt != null) {
            GeneralNames names = (GeneralNames) ciExt.get(CertificateIssuerExtension.ISSUER);
            return (X500Name) names.get(0).getName();
        } else {
            return prevCertIssuer;
        }
    }

    private static final X500Name Enter(Map<X509IssuerSerial, X509CRLEntry> revokedCerts, X509CRLEntry entry, X500Name crlIssuer, X500Name badCertIssuer) throws IOException {
        badCertIssuer = GetCertIssuer(entry, badCertIssuer);
        entry.setCertificateIssuer(crlIssuer, badCertIssuer);
        X509IssuerSerial issuerSerial = new X509IssuerSerial(badCertIssuer, entry.getSerialNumber());
        revokedCerts.put(issuerSerial, entry);
        return badCertIssuer;
    }

    private byte[] signedCRL = null;

    private byte[] signature = null;

    private byte[] tbsCertList = null;

    private AlgorithmId sigAlgId = null;

    private int version;

    private AlgorithmId infoSigAlgId;

    private X500Name issuer = null;

    private X500Name issuerPrincipal = null;

    private Date thisUpdate = null;

    private Date nextUpdate = null;

    private Map<X509IssuerSerial, X509CRLEntry> revokedCerts = new LinkedHashMap<X509IssuerSerial, X509CRLEntry>();

    private CRLExtensions extensions = null;

    private static final boolean isExplicit = true;

    private static final long YR_2050 = 2524636800000L;

    private boolean readOnly = false;

    /**
     * PublicKey that has previously been used to successfully verify
     * the signature of this CRL. Null if the CRL has not
     * yet been verified (successfully).
     */
    private PublicKey verifiedPublicKey;

    /**
     * If verifiedPublicKey is not null, name of the provider used to
     * successfully verify the signature of this CRL, or the
     * empty String if no provider was explicitly specified.
     */
    private String verifiedProvider;

    /**
     * Not to be used. As it would lead to cases of uninitialized
     * CRL objects.
     */
    private X509CRL() {
        super("X.509");
    }

    /**
     * Unmarshals an X.509 CRL from its encoded form, parsing the encoded
     * bytes.  This form of constructor is used by agents which
     * need to examine and use CRL contents. Note that the buffer
     * must include only one CRL, and no "garbage" may be left at
     * the end.
     *
     * @param crlData the encoded bytes, with no trailing padding.
     * @exception CRLException on parsing errors.
     */
    public X509CRL(byte[] crlData) throws CRLException {
        this();
        try {
            this.parse(new DerValue(crlData));
        } catch (IOException e) {
            this.signedCRL = null;
            throw new CRLException("Parsing", e);
        }
    }

    /**
     * Unmarshals an X.509 CRL from an DER value.
     *
     * @param val a DER value holding at least one CRL
     * @exception CRLException on parsing errors.
     */
    public X509CRL(DerValue val) throws CRLException {
        this();
        try {
            this.parse(val);
        } catch (IOException e) {
            this.signedCRL = null;
            throw new CRLException("Parsing", e);
        }
    }

    /**
     * Unmarshals an X.509 CRL from an input stream. Only one CRL
     * is expected at the end of the input stream.
     *
     * @param inStrm an input stream holding at least one CRL
     * @exception CRLException on parsing errors.
     */
    public X509CRL(InputStream inStrm) throws CRLException {
        this();
        try {
            this.parse(new DerValue(inStrm));
        } catch (IOException e) {
            this.signedCRL = null;
            throw new CRLException("Parsing", e);
        }
    }

    /**
     * Initial CRL constructor, no revoked certs, and no extensions.
     *
     * @param issuer the name of the CA issuing this CRL.
     * @param thisUpdate the Date of this issue.
     * @param nextUpdate the Date of the next CRL.
     */
    public X509CRL(X500Name issuer, Date thisDate, Date nextDate) {
        this();
        this.issuer = issuer;
        this.thisUpdate = thisDate;
        this.nextUpdate = nextDate;
    }

    /**
     * CRL constructor, revoked certs, no extensions.
     *
     * @param issuer the name of the CA issuing this CRL.
     * @param thisUpdate the Date of this issue.
     * @param nextUpdate the Date of the next CRL.
     * @param badCerts the array of CRL entries.
     *
     * @exception CRLException on parsing/construction errors.
     */
    public X509CRL(X500Name issuer, Date thisDate, Date nextDate, X509CRLEntry[] badCerts) throws CRLException {
        this();
        this.issuer = issuer;
        this.thisUpdate = thisDate;
        this.nextUpdate = nextDate;
        if (badCerts != null) {
            X500Name crlIssuer = issuer;
            X500Name badCertIssuer = issuer;
            Map<X509IssuerSerial, X509CRLEntry> revokedCerts = this.revokedCerts;
            for (int i = 0; i < badCerts.length; i++) {
                X509CRLEntry badCert = (X509CRLEntry) badCerts[i];
                try {
                    badCertIssuer = Enter(revokedCerts, badCert, crlIssuer, badCertIssuer);
                } catch (IOException ioe) {
                    throw new CRLException(ioe);
                }
                if (badCert.hasExtensions()) {
                    this.version = 1;
                }
            }
        }
    }

    /**
     * CRL constructor, revoked certs and extensions.
     *
     * @param issuer the name of the CA issuing this CRL.
     * @param thisUpdate the Date of this issue.
     * @param nextUpdate the Date of the next CRL.
     * @param badCerts the array of CRL entries.
     * @param crlExts the CRL extensions.
     *
     * @exception CRLException on parsing/construction errors.
     */
    public X509CRL(X500Name issuer, Date thisDate, Date nextDate, X509CRLEntry[] badCerts, CRLExtensions crlExts) throws CRLException {
        this(issuer, thisDate, nextDate, badCerts);
        if (crlExts != null) {
            this.extensions = crlExts;
            this.version = 1;
        }
    }

    /**
     * Returned the encoding as an uncloned byte array. Callers must
     * guarantee that they neither modify it nor expose it to untrusted
     * code.
     */
    public byte[] getEncodedInternal() throws CRLException {
        byte[] signedCRL = this.signedCRL;
        if (null == signedCRL) throw new CRLException("Null CRL to encode"); else return signedCRL;
    }

    /**
     * Returns the ASN.1 DER encoded form of this CRL.
     *
     * @exception CRLException if an encoding error occurs.
     */
    public byte[] getEncoded() throws CRLException {
        return this.getEncodedInternal().clone();
    }

    /**
     * Encodes the "to-be-signed" CRL to the OutputStream.
     *
     * @param out the OutputStream to write to.
     * @exception CRLException on encoding errors.
     */
    public void encodeInfo(OutputStream out) throws CRLException {
        try {
            DerOutputStream tmp = new DerOutputStream();
            DerOutputStream rCerts = new DerOutputStream();
            DerOutputStream seq = new DerOutputStream();
            if (version != 0) tmp.putInteger(version);
            infoSigAlgId.encode(tmp);
            if ((version == 0) && (issuer.toString() == null)) throw new CRLException("Null Issuer DN not allowed in v1 CRL"); else {
                this.issuer.encode(tmp);
                if (thisUpdate.getTime() < YR_2050) tmp.putUTCTime(this.thisUpdate); else tmp.putGeneralizedTime(this.thisUpdate);
                if (null != this.nextUpdate) {
                    if (this.nextUpdate.getTime() < YR_2050) tmp.putUTCTime(this.nextUpdate); else tmp.putGeneralizedTime(this.nextUpdate);
                }
                if (!this.revokedCerts.isEmpty()) {
                    for (X509CRLEntry entry : this.revokedCerts.values()) {
                        ((X509CRLEntry) entry).encode(rCerts);
                    }
                    tmp.write(DerValue.tag_Sequence, rCerts);
                }
                if (null != this.extensions) this.extensions.encode(tmp, isExplicit);
                seq.write(DerValue.tag_Sequence, tmp);
                tbsCertList = seq.toByteArray();
                out.write(tbsCertList);
            }
        } catch (IOException e) {
            throw new CRLException("Encoding", e);
        }
    }

    /**
     * Verifies that this CRL was signed using the
     * private key that corresponds to the given public key.
     *
     * @param key the PublicKey used to carry out the verification.
     *
     * @exception NoSuchAlgorithmException on unsupported signature
     * algorithms.
     * @exception InvalidKeyException on incorrect key.
     * @exception NoSuchProviderException if there's no default provider.
     * @exception SignatureException on signature errors.
     * @exception CRLException on encoding errors.
     */
    public void verify(PublicKey key) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        this.verify(key, "");
    }

    /**
     * Verifies that this CRL was signed using the
     * private key that corresponds to the given public key,
     * and that the signature verification was computed by
     * the given provider.
     *
     * @param key the PublicKey used to carry out the verification.
     * @param sigProvider the name of the signature provider.
     *
     * @exception NoSuchAlgorithmException on unsupported signature
     * algorithms.
     * @exception InvalidKeyException on incorrect key.
     * @exception NoSuchProviderException on incorrect provider.
     * @exception SignatureException on signature errors.
     * @exception CRLException on encoding errors.
     */
    public synchronized void verify(PublicKey key, String sigProvider) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        if (sigProvider == null) {
            sigProvider = "";
        }
        if ((verifiedPublicKey != null) && verifiedPublicKey.equals(key)) {
            if (sigProvider.equals(verifiedProvider)) {
                return;
            }
        }
        if (null == this.signedCRL) throw new CRLException("Uninitialized CRL"); else {
            Signature sigVerf = null;
            if (1 > sigProvider.length()) sigVerf = Signature.getInstance(sigAlgId.getName()); else sigVerf = Signature.getInstance(sigAlgId.getName(), sigProvider);
            sigVerf.initVerify(key);
            if (tbsCertList == null) {
                throw new CRLException("Uninitialized CRL");
            } else {
                sigVerf.update(tbsCertList, 0, tbsCertList.length);
                if (!sigVerf.verify(signature)) throw new SignatureException("Signature does not match."); else {
                    this.verifiedPublicKey = key;
                    this.verifiedProvider = sigProvider;
                }
            }
        }
    }

    /**
     * Encodes an X.509 CRL, and signs it using the given key.
     *
     * @param key the private key used for signing.
     * @param algorithm the name of the signature algorithm used.
     *
     * @exception NoSuchAlgorithmException on unsupported signature
     * algorithms.
     * @exception InvalidKeyException on incorrect key.
     * @exception NoSuchProviderException on incorrect provider.
     * @exception SignatureException on signature errors.
     * @exception CRLException if any mandatory data was omitted.
     */
    public void sign(PrivateKey key, String algorithm) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        this.sign(key, algorithm, null);
    }

    /**
     * Encodes an X.509 CRL, and signs it using the given key.
     *
     * @param key the private key used for signing.
     * @param algorithm the name of the signature algorithm used.
     * @param provider the name of the provider.
     *
     * @exception NoSuchAlgorithmException on unsupported signature
     * algorithms.
     * @exception InvalidKeyException on incorrect key.
     * @exception NoSuchProviderException on incorrect provider.
     * @exception SignatureException on signature errors.
     * @exception CRLException if any mandatory data was omitted.
     */
    public void sign(PrivateKey key, String algorithm, String provider) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        try {
            if (this.readOnly) throw new CRLException("cannot over-write existing CRL"); else {
                Signature sigEngine = null;
                if ((provider == null) || (provider.length() == 0)) sigEngine = Signature.getInstance(algorithm); else sigEngine = Signature.getInstance(algorithm, provider);
                sigEngine.initSign(key);
                sigAlgId = AlgorithmId.get(sigEngine.getAlgorithm());
                infoSigAlgId = sigAlgId;
                DerOutputStream out = new DerOutputStream();
                DerOutputStream tmp = new DerOutputStream();
                encodeInfo(tmp);
                sigAlgId.encode(tmp);
                sigEngine.update(tbsCertList, 0, tbsCertList.length);
                signature = sigEngine.sign();
                tmp.putBitString(signature);
                out.write(DerValue.tag_Sequence, tmp);
                this.signedCRL = out.toByteArray();
                this.readOnly = true;
            }
        } catch (IOException e) {
            throw new CRLException("Encoding", e);
        }
    }

    /**
     * Returns a printable string of this CRL.
     *
     * @return value of this CRL in a printable form.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("X.509 CRL v" + (version + 1) + "\n");
        if (sigAlgId != null) sb.append("Signature Algorithm: " + sigAlgId.toString() + ", OID=" + (sigAlgId.getOID()).toString() + "\n");
        if (issuer != null) sb.append("Issuer: " + issuer.toString() + "\n");
        if (thisUpdate != null) sb.append("\nThis Update: " + thisUpdate.toString() + "\n");
        if (nextUpdate != null) sb.append("Next Update: " + nextUpdate.toString() + "\n");
        if (revokedCerts.isEmpty()) sb.append("\nNO certificates have been revoked\n"); else {
            sb.append("\nRevoked Certificates: " + revokedCerts.size());
            int i = 1;
            for (Iterator<X509CRLEntry> iter = revokedCerts.values().iterator(); iter.hasNext(); i++) sb.append("\n[" + i + "] " + iter.next().toString());
        }
        if (extensions != null) {
            Collection<Extension> allExts = extensions.getAllExtensions();
            Object[] objs = allExts.toArray();
            sb.append("\nCRL Extensions: " + objs.length);
            for (int i = 0; i < objs.length; i++) {
                sb.append("\n[" + (i + 1) + "]: ");
                Extension ext = (Extension) objs[i];
                try {
                    if (OIDMap.getClass(ext.getExtensionId()) == null) {
                        sb.append(ext.toString());
                        byte[] extValue = ext.getExtensionValue();
                        if (extValue != null) {
                            DerOutputStream out = new DerOutputStream();
                            out.putOctetString(extValue);
                            extValue = out.toByteArray();
                            sb.append("Extension unknown: " + "DER encoded OCTET string =\n" + Hex.encode(extValue) + "\n");
                        }
                    } else sb.append(ext.toString());
                } catch (Exception e) {
                    sb.append(", Error parsing this extension");
                }
            }
        }
        if (signature != null) sb.append("\nSignature:\n" + Hex.encode(signature) + "\n"); else sb.append("NOT signed yet\n");
        return sb.toString();
    }

    /**
     * Checks whether the given certificate is on this CRL.
     *
     * @param cert the certificate to check for.
     * @return true if the given certificate is on this CRL,
     * false otherwise.
     */
    public boolean isRevoked(Certificate cert) {
        if (revokedCerts.isEmpty() || (!(cert instanceof X509Certificate))) {
            return false;
        } else {
            X509Certificate xcert = (X509Certificate) cert;
            X509IssuerSerial issuerSerial = new X509IssuerSerial(xcert);
            return revokedCerts.containsKey(issuerSerial);
        }
    }

    /**
     * Gets the version number from this CRL.
     * The ASN.1 definition for this is:
     * <pre>
     * Version  ::=  INTEGER  {  v1(0), v2(1), v3(2)  }
     *             -- v3 does not apply to CRLs but appears for consistency
     *             -- with definition of Version for certs
     * </pre>
     * @return the version number, i.e. 1 or 2.
     */
    public int getVersion() {
        return (this.version + 1);
    }

    /**
     * Gets the issuer distinguished name from this CRL.
     * The issuer name identifies the entity who has signed (and
     * issued the CRL). The issuer name field contains an
     * X.500 distinguished name (DN).
     * The ASN.1 definition for this is:
     * <pre>
     * issuer    Name
     *
     * Name ::= CHOICE { RDNSequence }
     * RDNSequence ::= SEQUENCE OF RelativeDistinguishedName
     * RelativeDistinguishedName ::=
     *     SET OF AttributeValueAssertion
     *
     * AttributeValueAssertion ::= SEQUENCE {
     *                               AttributeType,
     *                               AttributeValue }
     * AttributeType ::= OBJECT IDENTIFIER
     * AttributeValue ::= ANY
     * </pre>
     * The Name describes a hierarchical name composed of attributes,
     * such as country name, and corresponding values, such as US.
     * The type of the component AttributeValue is determined by the
     * AttributeType; in general it will be a directoryString.
     * A directoryString is usually one of PrintableString,
     * TeletexString or UniversalString.
     * @return the issuer name.
     */
    public Principal getIssuerDN() {
        return this.issuer;
    }

    /**
     * Return the issuer as X500Name. Overrides method in X509CRL
     * to provide a slightly more efficient version.
     */
    public X500Name getIssuerX500Principal() {
        return this.issuer;
    }

    /**
     * Gets the thisUpdate date from the CRL.
     * The ASN.1 definition for this is:
     *
     * @return the thisUpdate date from the CRL.
     */
    public Date getThisUpdate() {
        return (new Date(thisUpdate.getTime()));
    }

    /**
     * Gets the nextUpdate date from the CRL.
     *
     * @return the nextUpdate date from the CRL, or null if
     * not present.
     */
    public Date getNextUpdate() {
        Date nextUpdate = this.nextUpdate;
        if (null == nextUpdate) return null; else return (new Date(nextUpdate.getTime()));
    }

    /**
     * Gets the CRL entry with the given serial number from this CRL.
     *
     * @return the entry with the given serial number, or <code>null</code> if
     * no such entry exists in the CRL.
     * @see X509CRLEntry
     */
    public X509CRLEntry getRevokedCertificate(BigInteger serialNumber) {
        Map<X509IssuerSerial, X509CRLEntry> revokedCerts = this.revokedCerts;
        if (revokedCerts.isEmpty()) return null; else {
            X509IssuerSerial issuerSerial = new X509IssuerSerial(this.getIssuerX500Principal(), serialNumber);
            return revokedCerts.get(issuerSerial);
        }
    }

    /**
     * Gets the CRL entry for the given certificate.
     */
    public X509CRLEntry getRevokedCertificate(X509Certificate cert) {
        Map<X509IssuerSerial, X509CRLEntry> revokedCerts = this.revokedCerts;
        if (revokedCerts.isEmpty()) return null; else {
            X509IssuerSerial issuerSerial = new X509IssuerSerial(cert);
            return revokedCerts.get(issuerSerial);
        }
    }

    /**
     * Gets all the revoked certificates from the CRL.
     * A Set of X509CRLEntry.
     *
     * @return all the revoked certificates or <code>null</code> if there are
     * none.
     * @see X509CRLEntry
     */
    public Set<X509CRLEntry> getRevokedCertificates() {
        Map<X509IssuerSerial, X509CRLEntry> revokedCerts = this.revokedCerts;
        if (revokedCerts.isEmpty()) return null; else return new HashSet<X509CRLEntry>(revokedCerts.values());
    }

    /**
     * Gets the DER encoded CRL information, the
     * <code>tbsCertList</code> from this CRL.
     * This can be used to verify the signature independently.
     *
     * @return the DER encoded CRL information.
     * @exception CRLException on encoding errors.
     */
    public byte[] getTBSCertList() throws CRLException {
        byte[] tbsCertList = this.tbsCertList;
        if (null == this.tbsCertList) throw new CRLException("Uninitialized CRL"); else {
            int len = tbsCertList.length;
            byte[] dup = new byte[len];
            System.arraycopy(tbsCertList, 0, dup, 0, len);
            return dup;
        }
    }

    /**
     * Gets the raw Signature bits from the CRL.
     *
     * @return the signature.
     */
    public byte[] getSignature() {
        byte[] signature = this.signature;
        if (null == signature) return null; else {
            int len = signature.length;
            byte[] dup = new byte[len];
            System.arraycopy(signature, 0, dup, 0, len);
            return dup;
        }
    }

    /**
     * Gets the signature algorithm name for the CRL
     * signature algorithm. For example, the string "SHA1withDSA".
     * The ASN.1 definition for this is:
     * <pre>
     * AlgorithmIdentifier  ::=  SEQUENCE  {
     *     algorithm               OBJECT IDENTIFIER,
     *     parameters              ANY DEFINED BY algorithm OPTIONAL  }
     *                             -- contains a value of the type
     *                             -- registered for use with the
     *                             -- algorithm object identifier value
     * </pre>
     *
     * @return the signature algorithm name.
     */
    public String getSigAlgName() {
        AlgorithmId sigAlgId = this.sigAlgId;
        if (null == this.sigAlgId) return null; else return sigAlgId.getName();
    }

    /**
     * Gets the signature algorithm OID string from the CRL.
     * An OID is represented by a set of positive whole number separated
     * by ".", that means,<br>
     * &lt;positive whole number&gt;.&lt;positive whole number&gt;.&lt;...&gt;
     * For example, the string "1.2.840.10040.4.3" identifies the SHA-1
     * with DSA signature algorithm defined in
     * <a href="http://www.ietf.org/rfc/rfc3279.txt">RFC 3279: Algorithms and
     * Identifiers for the Internet X.509 Public Key Infrastructure Certificate
     * and CRL Profile</a>.
     *
     * @return the signature algorithm oid string.
     */
    public String getSigAlgOID() {
        AlgorithmId sigAlgId = this.sigAlgId;
        if (null == this.sigAlgId) return null; else {
            ObjectIdentifier oid = sigAlgId.getOID();
            return oid.toString();
        }
    }

    /**
     * Gets the DER encoded signature algorithm parameters from this
     * CRL's signature algorithm. In most cases, the signature
     * algorithm parameters are null, the parameters are usually
     * supplied with the Public Key.
     *
     * @return the DER encoded signature algorithm parameters, or
     *         null if no parameters are present.
     */
    public byte[] getSigAlgParams() {
        AlgorithmId sigAlgId = this.sigAlgId;
        if (null == sigAlgId) return null; else {
            try {
                return sigAlgId.getEncodedParams();
            } catch (IOException e) {
                return null;
            }
        }
    }

    public KeyIdentifier getAuthKeyId() throws IOException {
        AuthorityKeyIdentifierExtension aki = this.getAuthKeyIdExtension();
        if (null != aki) return (KeyIdentifier) aki.get(aki.KEY_ID); else return null;
    }

    public AuthorityKeyIdentifierExtension getAuthKeyIdExtension() throws IOException {
        return (AuthorityKeyIdentifierExtension) this.getExtension(PKIXExtensions.AuthorityKey_Id);
    }

    public CRLNumberExtension getCRLNumberExtension() throws IOException {
        return (CRLNumberExtension) this.getExtension(PKIXExtensions.CRLNumber_Id);
    }

    public BigInteger getCRLNumber() throws IOException {
        CRLNumberExtension numExt = this.getCRLNumberExtension();
        if (null != numExt) return (BigInteger) numExt.get(numExt.NUMBER); else return null;
    }

    public DeltaCRLIndicatorExtension getDeltaCRLIndicatorExtension() throws IOException {
        return (DeltaCRLIndicatorExtension) this.getExtension(PKIXExtensions.DeltaCRLIndicator_Id);
    }

    public BigInteger getBaseCRLNumber() throws IOException {
        DeltaCRLIndicatorExtension dciExt = this.getDeltaCRLIndicatorExtension();
        if (null != dciExt) return (BigInteger) dciExt.get(dciExt.NUMBER); else return null;
    }

    public IssuerAlternativeNameExtension getIssuerAltNameExtension() throws IOException {
        return (IssuerAlternativeNameExtension) this.getExtension(PKIXExtensions.IssuerAlternativeName_Id);
    }

    public IssuingDistributionPointExtension getIssuingDistributionPointExtension() throws IOException {
        return (IssuingDistributionPointExtension) this.getExtension(PKIXExtensions.IssuingDistributionPoint_Id);
    }

    /**
     * Return true if a critical extension is found that is
     * not supported, otherwise return false.
     */
    public boolean hasUnsupportedCriticalExtension() {
        CRLExtensions extensions = this.extensions;
        if (null == extensions) return false; else return extensions.hasUnsupportedCriticalExtension();
    }

    /**
     * Gets a Set of the extension(s) marked CRITICAL in the
     * CRL. In the returned set, each extension is represented by
     * its OID string.
     *
     * @return a set of the extension oid strings in the
     * CRL that are marked critical.
     */
    @Code(Check.Builds)
    public Set<String> getCriticalExtensionOIDs() {
        AlgorithmId sigAlgId = this.sigAlgId;
        if (null == extensions) return null; else {
            Set<String> extSet = new HashSet<String>();
            for (Extension ex : extensions.getAllExtensions()) {
                if (ex.isCritical()) {
                    extSet.add(ex.getExtensionId().toString());
                }
            }
            return extSet;
        }
    }

    /**
     * Gets a Set of the extension(s) marked NON-CRITICAL in the
     * CRL. In the returned set, each extension is represented by
     * its OID string.
     *
     * @return a set of the extension oid strings in the
     * CRL that are NOT marked critical.
     */
    @Code(Check.Builds)
    public Set<String> getNonCriticalExtensionOIDs() {
        if (extensions == null) {
            return null;
        }
        Set<String> extSet = new HashSet<String>();
        for (Extension ex : extensions.getAllExtensions()) {
            if (!ex.isCritical()) {
                extSet.add(ex.getExtensionId().toString());
            }
        }
        return extSet;
    }

    /**
     * Gets the DER encoded OCTET string for the extension value
     * (<code>extnValue</code>) identified by the passed in oid String.
     * The <code>oid</code> string is
     * represented by a set of positive whole number separated
     * by ".", that means,<br>
     * &lt;positive whole number&gt;.&lt;positive whole number&gt;.&lt;...&gt;
     *
     * @param oid the Object Identifier value for the extension.
     * @return the der encoded octet string of the extension value.
     */
    public byte[] getExtensionValue(String oid) {
        CRLExtensions extensions = this.extensions;
        if (extensions == null) return null; else {
            try {
                ObjectIdentifier findOID = new ObjectIdentifier(oid);
                String extAlias = OIDMap.getName(findOID);
                Extension crlExt = null;
                if (extAlias == null) {
                    Extension ex = null;
                    ObjectIdentifier inCertOID;
                    Enumeration<Extension> e = extensions.getElements();
                    while (e.hasMoreElements()) {
                        ex = e.nextElement();
                        inCertOID = ex.getExtensionId();
                        if (inCertOID.equals(findOID)) {
                            crlExt = ex;
                            break;
                        }
                    }
                } else crlExt = extensions.get(extAlias);
                if (crlExt == null) return null; else {
                    byte[] extData = crlExt.getExtensionValue();
                    if (extData == null) return null; else return DerValue.Encode.OctetString(extData);
                }
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * get an extension
     *
     * @param oid ObjectIdentifier of extension desired
     * @returns Object of type <extension> or null, if not found
     * @throws IOException on error
     */
    @Code(Check.SecNoCopy)
    public Object getExtension(ObjectIdentifier oid) {
        CRLExtensions extensions = this.extensions;
        if (null == extensions) return null; else return extensions.get(OIDMap.getName(oid));
    }

    private void parse(DerValue val) throws CRLException, IOException {
        if (readOnly) throw new CRLException("cannot over-write existing CRL"); else if (val.getData() == null || DerValue.tag_Sequence != val.tag) throw new CRLException("Invalid DER-encoded CRL data"); else {
            this.signedCRL = val.toByteArray();
            DerInputStream data = val.data;
            DerValue seq0Certlist = data.getDerValue();
            DerValue seq1Algid = data.getDerValue();
            DerValue seq2Signa = data.getDerValue();
            if (0 != data.available()) throw new CRLException("signed overrun, bytes = " + data.available()); else if (DerValue.tag_Sequence != seq0Certlist.tag) throw new CRLException("signed CRL fields invalid"); else {
                this.sigAlgId = AlgorithmId.parse(seq1Algid);
                this.signature = seq2Signa.getBitString();
                if (0 != seq1Algid.data.available()) throw new CRLException("AlgorithmId field overrun"); else if (0 != seq2Signa.data.available()) throw new CRLException("Signature field overrun"); else {
                    this.tbsCertList = seq0Certlist.toByteArray();
                    data = seq0Certlist.data;
                    DerValue tmp;
                    byte nextByte;
                    this.version = 0;
                    nextByte = (byte) data.peekByte();
                    if (DerValue.tag_Integer == nextByte) {
                        this.version = data.getInteger();
                        if (1 != this.version) throw new CRLException("Invalid version");
                    }
                    tmp = data.getDerValue();
                    AlgorithmId tmpId = AlgorithmId.parse(tmp);
                    if (!tmpId.equals(this.sigAlgId)) throw new CRLException("Signature algorithm mismatch"); else {
                        this.infoSigAlgId = tmpId;
                        this.issuer = new X500Name(data);
                        if (this.issuer.isEmpty()) {
                            throw new CRLException("Empty issuer DN not allowed in X509CRLs");
                        } else {
                            nextByte = (byte) data.peekByte();
                            switch(nextByte) {
                                case DerValue.tag_UtcTime:
                                    thisUpdate = data.getUTCTime();
                                    break;
                                case DerValue.tag_GeneralizedTime:
                                    thisUpdate = data.getGeneralizedTime();
                                    break;
                                default:
                                    throw new CRLException("Invalid encoding for thisUpdate (tag=" + nextByte + ")");
                            }
                            if (data.available() == 0) return; else {
                                nextByte = (byte) data.peekByte();
                                switch(nextByte) {
                                    case DerValue.tag_UtcTime:
                                        nextUpdate = data.getUTCTime();
                                        break;
                                    case DerValue.tag_GeneralizedTime:
                                        nextUpdate = data.getGeneralizedTime();
                                        break;
                                    default:
                                        break;
                                }
                                if (data.available() == 0) return; else {
                                    nextByte = (byte) data.peekByte();
                                    if ((DerValue.tag_SequenceOf == nextByte) && ((nextByte & 0x0c0) != 0x080)) {
                                        DerValue[] badCerts = data.getSequence(4);
                                        X500Name crlIssuer = this.issuer;
                                        X500Name badCertIssuer = crlIssuer;
                                        Map<X509IssuerSerial, X509CRLEntry> revokedCerts = this.revokedCerts;
                                        for (int i = 0; i < badCerts.length; i++) {
                                            X509CRLEntry entry = new X509CRLEntry(badCerts[i]);
                                            badCertIssuer = Enter(revokedCerts, entry, crlIssuer, badCertIssuer);
                                        }
                                    }
                                    if (data.available() == 0) return; else {
                                        tmp = data.getDerValue();
                                        if (tmp.isConstructed() && tmp.isContextSpecific((byte) 0)) {
                                            extensions = new CRLExtensions(tmp.data);
                                        }
                                        this.readOnly = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public int hashCode() {
        try {
            return (Function.Xor.Hash32(this.getEncodedInternal()));
        } catch (CRLException exc) {
            return 0;
        }
    }

    public boolean equals(Object ano) {
        if (this == ano) return true; else if (ano instanceof X509CRL) {
            X509CRL that = (X509CRL) ano;
            return Bbuf.equals(this.signedCRL, that.signedCRL);
        } else return false;
    }
}
