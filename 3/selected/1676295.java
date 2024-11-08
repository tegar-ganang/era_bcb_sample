package org.hardtokenmgmt.core.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.PolicyInformation;
import org.bouncycastle.asn1.x509.ReasonFlags;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509DefaultEntryConverter;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.x509.X509NameEntryConverter;
import org.bouncycastle.asn1.x509.X509NameTokenizer;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.interfaces.ConfigurableProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.ejbca.core.model.ca.catoken.CATokenInfo;
import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.ejbca.core.protocol.PKCS10RequestMessage;
import org.ejbca.util.Base64;
import org.ejbca.util.FileTools;
import org.ejbca.util.StringTools;
import org.ejbca.util.dn.DnComponents;

/**
 * Certificate related utilities. Most of the method is copied from EJBCA 3.5 branch
 * since 3.9 and up is dependent of cvs libs that isn't necessary.
 * 
 * 
 * @author Philip Vendil 22 Jan 2010
 *
 * @version $Id$
 */
public class CertUtils {

    private static Logger log = Logger.getLogger(CertUtils.class);

    public static final String BEGIN_CERTIFICATE_REQUEST = "-----BEGIN CERTIFICATE REQUEST-----";

    public static final String END_CERTIFICATE_REQUEST = "-----END CERTIFICATE REQUEST-----";

    static CertificateFactory getCertificateFactory() {
        try {
            return CertificateFactory.getInstance("X.509", "BC");
        } catch (NoSuchProviderException nspe) {
            log.error("NoSuchProvider: ", nspe);
        } catch (CertificateException ce) {
            log.error("CertificateException: ", ce);
        }
        return null;
    }

    /**
     * Creates X509Certificate from byte[].
     *
     * @param cert byte array containing certificate in DER-format
     *
     * @return X509Certificate
     *
     * @throws CertificateException if the byte array does not contain a proper certificate.
     * @throws IOException if the byte array cannot be read.
     */
    public static X509Certificate getCertfromByteArray(byte[] cert) throws CertificateException {
        CertificateFactory cf = getCertificateFactory();
        X509Certificate x509cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert));
        return x509cert;
    }

    public static PKCS10RequestMessage genPKCS10RequestMessageFromPEM(byte[] b64Encoded) {
        byte[] buffer = null;
        try {
            String beginKey = BEGIN_CERTIFICATE_REQUEST;
            String endKey = END_CERTIFICATE_REQUEST;
            buffer = FileTools.getBytesFromPEM(b64Encoded, beginKey, endKey);
        } catch (IOException e) {
            try {
                String beginKey = "-----BEGIN NEW CERTIFICATE REQUEST-----";
                String endKey = "-----END NEW CERTIFICATE REQUEST-----";
                buffer = FileTools.getBytesFromPEM(b64Encoded, beginKey, endKey);
            } catch (IOException ioe) {
                buffer = Base64.decode(b64Encoded);
            }
        }
        if (buffer == null) {
            return null;
        }
        return new PKCS10RequestMessage(buffer);
    }

    static {
        DnComponents.getDnObjects();
    }

    public static final String EMAIL = "rfc822name";

    public static final String EMAIL1 = "email";

    public static final String EMAIL2 = "EmailAddress";

    public static final String EMAIL3 = "E";

    public static final String DNS = "dNSName";

    public static final String URI = "uniformResourceIdentifier";

    public static final String URI1 = "uri";

    public static final String URI2 = "uniformResourceId";

    public static final String IPADDR = "iPAddress";

    public static final String DIRECTORYNAME = "directoryName";

    /** Microsoft altName for windows smart card logon */
    public static final String UPN = "upn";

    /** ObjectID for upn altName for windows smart card logon */
    public static final String UPN_OBJECTID = "1.3.6.1.4.1.311.20.2.3";

    /** Microsoft altName for windows domain controller guid */
    public static final String GUID = "guid";

    /** ObjectID for upn altName for windows domain controller guid */
    public static final String GUID_OBJECTID = "1.3.6.1.4.1.311.25.1";

    /** Object id id-pkix */
    public static final String id_pkix = "1.3.6.1.5.5.7";

    /** Object id id-pda */
    public static final String id_pda = id_pkix + ".9";

    /** Object id id-pda-dateOfBirth 
     * DateOfBirth ::= GeneralizedTime
     */
    public static final String id_pda_dateOfBirth = id_pda + ".1";

    /** Object id id-pda-placeOfBirth 
     * PlaceOfBirth ::= DirectoryString 
     */
    public static final String id_pda_placeOfBirth = id_pda + ".2";

    /** Object id id-pda-gender
     *  Gender ::= PrintableString (SIZE(1))
     *          -- "M", "F", "m" or "f"
     */
    public static final String id_pda_gender = id_pda + ".3";

    /** Object id id-pda-countryOfCitizenship
     * CountryOfCitizenship ::= PrintableString (SIZE (2))
     *                      -- ISO 3166 Country Code 
     */
    public static final String id_pda_countryOfCitizenship = id_pda + ".4";

    /** Object id id-pda-countryOfResidence
     * CountryOfResidence ::= PrintableString (SIZE (2))
     *                    -- ISO 3166 Country Code 
     */
    public static final String id_pda_countryOfResidence = id_pda + ".5";

    /** Object id for qcStatements Extension */
    public static final String QCSTATEMENTS_OBJECTID = "1.3.6.1.5.5.7.1.3";

    /** OID used for creating MS Templates */
    public static final String OID_MSTEMPLATE = "1.3.6.1.4.1.311.20.2";

    private static final String[] EMAILIDS = { EMAIL, EMAIL1, EMAIL2, EMAIL3 };

    /** Parameters used when generating or verifying ECDSA keys/certs using the "implicitlyCA" key encoding.
     * The curve parameters is then defined outside of the key and configured in the BC provider.
     */
    private static String IMPLICITLYCA_Q = "@ecdsa.implicitlyca.q@";

    private static String IMPLICITLYCA_A = "@ecdsa.implicitlyca.a@";

    private static String IMPLICITLYCA_B = "@ecdsa.implicitlyca.b@";

    private static String IMPLICITLYCA_G = "@ecdsa.implicitlyca.g@";

    private static String IMPLICITLYCA_N = "@ecdsa.implicitlyca.n@";

    /** Flag indicating if the BC provider should be removed before installing it again. When developing and re-deploying alot
     * this is needed so you don't have to restart JBoss all the time. 
     * In production it may cause failures because the BC provider may get removed just when another thread wants to use it.
     * Therefore the default value is false. 
     */
    private static final boolean developmentProviderInstallation = BooleanUtils.toBoolean("@development.provider.installation@");

    /** See stringToBcX509Name(String, X509NameEntryConverter), this method uses the default BC converter (X509DefaultEntryConverter)
     * @see #stringToBcX509Name(String, X509NameEntryConverter)
     * @param dn String containing DN that will be transformed into X509Name, The
     *          DN string has the format "CN=zz,OU=yy,O=foo,C=SE". Unknown OIDs in
     *          the string will be added to the end positions of OID array.
     * 
     * @return X509Name or null if input is null
     */
    public static X509Name stringToBcX509Name(String dn) {
        X509NameEntryConverter converter = new X509DefaultEntryConverter();
        return stringToBcX509Name(dn, converter);
    }

    /**
     * Creates a (Bouncycastle) X509Name object from a string with a DN. Known OID
     * (with order) are:
     * <code> EmailAddress, UID, CN, SN (SerialNumber), GivenName, Initials, SurName, T, OU,
     * O, L, ST, DC, C </code>
     * To change order edit 'dnObjects' in this source file. Important NOT to mess
     * with the ordering within this class, since cert vierification on some
     * clients (IE :-() might depend on order.
     * 
     * @param dn
     *          String containing DN that will be transformed into X509Name, The
     *          DN string has the format "CN=zz,OU=yy,O=foo,C=SE". Unknown OIDs in
     *          the string will be added to the end positions of OID array.
     * @param converter BC converter for DirectoryStrings, that determines which encoding is chosen
     * @return X509Name or null if input is null
     */
    private static X509Name stringToBcX509Name(String dn, X509NameEntryConverter converter) {
        return stringToBcX509Name(dn, converter, getDefaultX509FieldOrder());
    }

    public static X509Name stringToBcX509Name(String dn, X509NameEntryConverter converter, Vector<DERObjectIdentifier> dnOrder) {
        if (dn == null) return null;
        Vector<DERObjectIdentifier> defaultOrdering = new Vector<DERObjectIdentifier>();
        Vector<Object> values = new Vector<Object>();
        X509NameTokenizer xt = new X509NameTokenizer(dn);
        while (xt.hasMoreTokens()) {
            String pair = xt.nextToken();
            int ix = pair.indexOf("=");
            if (ix != -1) {
                String key = pair.substring(0, ix).toLowerCase().trim();
                String val = pair.substring(ix + 1);
                if (val != null) {
                    val = StringUtils.stripStart(val, null);
                }
                DERObjectIdentifier oid = DnComponents.getOid(key);
                try {
                    if (oid == null) {
                        oid = new DERObjectIdentifier(key);
                    }
                    defaultOrdering.add(oid);
                    values.add(val);
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown DN component ignored and silently dropped: " + key);
                }
            } else {
                log.warn("Huh, what's this? DN: " + dn + " PAIR: " + pair);
            }
        }
        X509Name x509Name = new X509Name(defaultOrdering, values, converter);
        X509Name orderedX509Name = getOrderedX509Name(x509Name, dnOrder, converter);
        return orderedX509Name;
    }

    /**
     * Every DN-string should look the same. Creates a name string ordered and looking like we want
     * it...
     *
     * @param dn String containing DN
     *
     * @return String containing DN, or null if input is null
     */
    public static String stringToBCDNString(String dn) {
        if (isDNReversed(dn)) {
            dn = reverseDN(dn);
        }
        String ret = null;
        X509Name name = stringToBcX509Name(dn);
        if (name != null) {
            ret = name.toString();
        }
        if ((ret != null) && (ret.length() > 250)) {
            log.info("Warning! DN is more than 250 characters long. Some databases have only 250 characters in the database for SubjectDN. Clipping may occur! DN (" + ret.length() + " chars): " + ret);
        }
        return ret;
    }

    /**
     * Convenience method for getting an email addresses from a DN. Uses {@link
     * getPartsFromDN(String,String)} internally, and searches for {@link EMAIL}, {@link EMAIL1},
     * {@link EMAIL2}, {@link EMAIL3} and returns the first one found.
     *
     * @param dn the DN
     *
	 * @return ArrayList containing email or empty list if email is not present
     * @return the found email address, or <code>null</code> if none is found
     */
    public static ArrayList<String> getEmailFromDN(String dn) {
        log.debug(">getEmailFromDN(" + dn + ")");
        ArrayList<String> ret = new ArrayList<String>();
        for (int i = 0; i < EMAILIDS.length; i++) {
            ArrayList<String> emails = getPartsFromDN(dn, EMAILIDS[i]);
            if (emails.size() > 0) {
                ret.addAll(emails);
            }
        }
        log.debug("<getEmailFromDN(" + dn + "): " + ret.size());
        return ret;
    }

    /**
     * Search for e-mail address, first in SubjectAltName (as in PKIX
     * recomandation) then in subject DN.
     * Original author: Marco Ferrante, (c) 2005 CSITA - University of Genoa (Italy)
     * 
     * @param certificate
     * @return subject email or null if not present in certificate
     */
    public static String getEMailAddress(X509Certificate certificate) {
        log.debug("Searching for EMail Address in SubjectAltName");
        if (certificate == null) {
            return null;
        }
        try {
            if (certificate.getSubjectAlternativeNames() != null) {
                java.util.Collection<?> altNames = certificate.getSubjectAlternativeNames();
                Iterator<?> iter = altNames.iterator();
                while (iter.hasNext()) {
                    java.util.List<?> item = (java.util.List<?>) iter.next();
                    Integer type = (Integer) item.get(0);
                    if (type.intValue() == 1) {
                        return (String) item.get(1);
                    }
                }
            }
        } catch (CertificateParsingException e) {
            log.error("Error parsing certificate: ", e);
        }
        log.debug("Searching for EMail Address in Subject DN");
        ArrayList<String> emails = CertUtils.getEmailFromDN(certificate.getSubjectDN().getName());
        if (emails.size() > 0) {
            return emails.get(0);
        }
        return null;
    }

    /**
     * Takes a DN and reverses it completely so the first attribute ends up last. 
     * C=SE,O=Foo,CN=Bar becomes CN=Bar,O=Foo,C=SE.
     *
     * @param dn String containing DN to be reversed, The DN string has the format "C=SE, O=xx, OU=yy, CN=zz".
     *
     * @return String containing reversed DN
     */
    public static String reverseDN(String dn) {
        log.debug(">reverseDN: dn: " + dn);
        String ret = null;
        if (dn != null) {
            String o;
            BasicX509NameTokenizer xt = new BasicX509NameTokenizer(dn);
            StringBuffer buf = new StringBuffer();
            boolean first = true;
            while (xt.hasMoreTokens()) {
                o = xt.nextToken();
                if (!first) {
                    buf.insert(0, ",");
                } else {
                    first = false;
                }
                buf.insert(0, o);
            }
            if (buf.length() > 0) {
                ret = buf.toString();
            }
        }
        log.debug("<reverseDN: resulting dn: " + ret);
        return ret;
    }

    /**
     * Tries to determine if a DN is in reversed form. It does this by taking the last attribute 
     * and the first attribute. If the last attribute comes before the first in the dNObjects array
     * the DN is assumed to be in reversed order.
     * The check if a DN is revered is relative to the default ordering, so if the default ordering is:
     * "C=SE, O=PrimeKey, CN=Tomas" (dNObjectsReverse ordering in EJBCA) a dn or form "CN=Tomas, O=PrimeKey, C=SE" is reversed.
     * 
     * if the default ordering is:
     * "CN=Tomas, O=PrimeKey, C=SE" (dNObjectsForward ordering in EJBCA) a dn or form "C=SE, O=PrimeKey, CN=Tomas" is reversed.
     * 
     *
     * @param dn String containing DN to be checked, The DN string has the format "C=SE, O=xx, OU=yy, CN=zz".
     *
     * @return true if the DN is believed to be in reversed order, false otherwise
     */
    protected static boolean isDNReversed(String dn) {
        boolean ret = false;
        if (dn != null) {
            String first = null;
            String last = null;
            X509NameTokenizer xt = new X509NameTokenizer(dn);
            if (xt.hasMoreTokens()) {
                first = xt.nextToken();
            }
            while (xt.hasMoreTokens()) {
                last = xt.nextToken();
            }
            String[] dNObjects = DnComponents.getDnObjects();
            if ((first != null) && (last != null)) {
                first = first.substring(0, first.indexOf('='));
                last = last.substring(0, last.indexOf('='));
                int firsti = 0, lasti = 0;
                for (int i = 0; i < dNObjects.length; i++) {
                    if (first.toLowerCase().equals(dNObjects[i])) {
                        firsti = i;
                    }
                    if (last.toLowerCase().equals(dNObjects[i])) {
                        lasti = i;
                    }
                }
                if (lasti < firsti) {
                    ret = true;
                }
            }
        }
        return ret;
    }

    /**
     * Gets a specified part of a DN. Specifically the first occurrence it the DN contains several
     * instances of a part (i.e. cn=x, cn=y returns x).
     *
     * @param dn String containing DN, The DN string has the format "C=SE, O=xx, OU=yy, CN=zz".
     * @param dnpart String specifying which part of the DN to get, should be "CN" or "OU" etc.
     *
     * @return String containing dnpart or null if dnpart is not present
     */
    public static String getPartFromDN(String dn, String dnpart) {
        log.debug(">getPartFromDN: dn:'" + dn + "', dnpart=" + dnpart);
        String part = null;
        if ((dn != null) && (dnpart != null)) {
            String o;
            dnpart += "=";
            X509NameTokenizer xt = new X509NameTokenizer(dn);
            while (xt.hasMoreTokens()) {
                o = xt.nextToken();
                if ((o.length() > dnpart.length()) && o.substring(0, dnpart.length()).equalsIgnoreCase(dnpart)) {
                    part = o.substring(dnpart.length());
                    break;
                }
            }
        }
        log.debug("<getpartFromDN: resulting DN part=" + part);
        return part;
    }

    /**
	 * Gets a specified parts of a DN. Returns all occurences as an ArrayList, also works if DN contains several
	 * instances of a part (i.e. cn=x, cn=y returns {x, y, null}).
	 *
	 * @param dn String containing DN, The DN string has the format "C=SE, O=xx, OU=yy, CN=zz".
	 * @param dnpart String specifying which part of the DN to get, should be "CN" or "OU" etc.
	 *
	 * @return ArrayList containing dnparts or empty list if dnpart is not present
	 */
    public static ArrayList<String> getPartsFromDN(String dn, String dnpart) {
        log.debug(">getPartsFromDN: dn:'" + dn + "', dnpart=" + dnpart);
        ArrayList<String> parts = new ArrayList<String>();
        if ((dn != null) && (dnpart != null)) {
            String o;
            dnpart += "=";
            X509NameTokenizer xt = new X509NameTokenizer(dn);
            while (xt.hasMoreTokens()) {
                o = xt.nextToken();
                if ((o.length() > dnpart.length()) && o.substring(0, dnpart.length()).equalsIgnoreCase(dnpart)) {
                    parts.add(o.substring(dnpart.length()));
                }
            }
        }
        log.debug("<getpartsFromDN: resulting DN part=" + parts.toString());
        return parts;
    }

    /**
	 * Gets a list of all custom OIDs defined in the string. A custom OID is defined as an OID, simply as that. Otherwise, if it is not a custom oid, the DNpart is defined by a name such as CN och rfc822Name.
	 *
	 * @param dn String containing DN, The DN string has the format "C=SE, O=xx, OU=yy, CN=zz", or "rfc822Name=foo@bar.com", etc.
	 * @param dnpart String specifying which part of the DN to get, should be "CN" or "OU" etc.
	 *
	 * @return ArrayList containing oids or empty list if no custom OIDs are present
	 */
    public static ArrayList<String> getCustomOids(String dn) {
        log.debug(">getCustomOids: dn:'" + dn);
        ArrayList<String> parts = new ArrayList<String>();
        if (dn != null) {
            String o;
            X509NameTokenizer xt = new X509NameTokenizer(dn);
            while (xt.hasMoreTokens()) {
                o = xt.nextToken();
                try {
                    int i = o.indexOf('=');
                    if ((i > 2) && (o.charAt(1) == '.')) {
                        String oid = o.substring(0, i);
                        new DERObjectIdentifier(oid);
                        parts.add(oid);
                    }
                } catch (IllegalArgumentException e) {
                }
            }
        }
        log.debug("<getpartsFromDN: resulting DN part=" + parts.toString());
        return parts;
    }

    /**
     * Gets subject DN in the format we are sure about (BouncyCastle),supporting UTF8.
     *
     * @param cert X509Certificate
     *
     * @return String containing the subjects DN.
     */
    public static String getSubjectDN(X509Certificate cert) {
        return getDN(cert, 1);
    }

    /**
     * Gets issuer DN in the format we are sure about (BouncyCastle),supporting UTF8.
     *
     * @param cert X509Certificate
     *
     * @return String containing the issuers DN.
     */
    public static String getIssuerDN(X509Certificate cert) {
        return getDN(cert, 2);
    }

    /**
     * Gets subject or issuer DN in the format we are sure about (BouncyCastle),supporting UTF8.
     *
     * @param cert X509Certificate
     * @param which 1 = subjectDN, anything else = issuerDN
     *
     * @return String containing the DN.
     */
    private static String getDN(X509Certificate cert, int which) {
        String dn = null;
        if (cert == null) {
            return dn;
        }
        try {
            CertificateFactory cf = getCertificateFactory();
            X509Certificate x509cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert.getEncoded()));
            if (which == 1) {
                dn = x509cert.getSubjectDN().toString();
            } else {
                dn = x509cert.getIssuerDN().toString();
            }
        } catch (CertificateException ce) {
            log.error("CertificateException: ", ce);
            return null;
        }
        return stringToBCDNString(dn);
    }

    /**
     * Gets issuer DN for CRL in the format we are sure about (BouncyCastle),supporting UTF8.
     *
     * @param crl X509RL
     *
     * @return String containing the DN.
     */
    public static String getIssuerDN(X509CRL crl) {
        String dn = null;
        try {
            CertificateFactory cf = CertUtils.getCertificateFactory();
            X509CRL x509crl = (X509CRL) cf.generateCRL(new ByteArrayInputStream(crl.getEncoded()));
            dn = x509crl.getIssuerDN().toString();
        } catch (CRLException ce) {
            log.error("CRLException: ", ce);
            return null;
        }
        return stringToBCDNString(dn);
    }

    public static synchronized void removeBCProvider() {
        Security.removeProvider("BC");
    }

    @SuppressWarnings("unchecked")
    public static synchronized void installBCProvider() {
        boolean installImplicitlyCA = false;
        if (Security.addProvider(new BouncyCastleProvider()) < 0) {
            if (developmentProviderInstallation) {
                removeBCProvider();
                if (Security.addProvider(new BouncyCastleProvider()) < 0) {
                    log.error("Cannot even install BC provider again!");
                } else {
                    installImplicitlyCA = true;
                }
            }
        } else {
            installImplicitlyCA = true;
        }
        if (installImplicitlyCA) {
            checkImplicitParams();
            ECCurve curve = new ECCurve.Fp(new BigInteger(IMPLICITLYCA_Q), new BigInteger(IMPLICITLYCA_A, 16), new BigInteger(IMPLICITLYCA_B, 16));
            org.bouncycastle.jce.spec.ECParameterSpec implicitSpec = new org.bouncycastle.jce.spec.ECParameterSpec(curve, curve.decodePoint(Hex.decode(IMPLICITLYCA_G)), new BigInteger(IMPLICITLYCA_N));
            ConfigurableProvider config = (ConfigurableProvider) Security.getProvider("BC");
            if (config != null) {
                config.setParameter(ConfigurableProvider.EC_IMPLICITLY_CA, implicitSpec);
            } else {
                log.error("Can not get ConfigurableProvider, implicitlyCA EC parameters NOT set!");
            }
        }
        X509Name.DefaultSymbols.put(X509Name.SN, "SN");
    }

    /** Check if parameters have been set correctly during pre-process, otherwise log an error and
     * set default values. Mostly used to be able to do JUnit testing
     */
    private static void checkImplicitParams() {
        if (StringUtils.contains(IMPLICITLYCA_Q, "ecdsa.implicitlyca.q")) {
            log.error("IMPLICITLYCA_Q not set!");
            IMPLICITLYCA_Q = "883423532389192164791648750360308885314476597252960362792450860609699839";
        }
        if (StringUtils.contains(IMPLICITLYCA_A, "ecdsa.implicitlyca.a")) {
            log.error("IMPLICITLYCA_A not set!");
            IMPLICITLYCA_A = "7fffffffffffffffffffffff7fffffffffff8000000000007ffffffffffc";
        }
        if (StringUtils.contains(IMPLICITLYCA_B, "ecdsa.implicitlyca.b")) {
            log.error("IMPLICITLYCA_B not set!");
            IMPLICITLYCA_B = "6b016c3bdcf18941d0d654921475ca71a9db2fb27d1d37796185c2942c0a";
        }
        if (StringUtils.contains(IMPLICITLYCA_G, "ecdsa.implicitlyca.g")) {
            log.error("IMPLICITLYCA_G not set!");
            IMPLICITLYCA_G = "020ffa963cdca8816ccc33b8642bedf905c3d358573d3f27fbbd3b3cb9aaaf";
        }
        if (StringUtils.contains(IMPLICITLYCA_N, "ecdsa.implicitlyca.n")) {
            log.error("IMPLICITLYCA_N not set!");
            IMPLICITLYCA_N = "883423532389192164791648750360308884807550341691627752275345424702807307";
        }
    }

    /**
     * Reads a certificate in PEM-format from a file. The file may contain other things,
     * the first certificate in the file is read.
     *
     * @param certFile the file containing the certificate in PEM-format
     * @return Ordered Collection of X509Certificate, first certificate first, or empty Collection
     * @exception IOException if the filen cannot be read.
     * @exception CertificateException if the filen does not contain a correct certificate.
     */
    public static Collection<X509Certificate> getCertsFromPEM(String certFile) throws IOException, CertificateException {
        log.debug(">getCertfromPEM: certFile=" + certFile);
        InputStream inStrm = null;
        Collection<X509Certificate> certs;
        try {
            inStrm = new FileInputStream(certFile);
            certs = getCertsFromPEM(inStrm);
        } finally {
            if (inStrm != null) inStrm.close();
        }
        log.debug("<getCertfromPEM: certFile=" + certFile);
        return certs;
    }

    /**
     * Reads a certificate in PEM-format from an InputStream. The stream may contain other things,
     * the first certificate in the stream is read.
     *
     * @param certFile the input stream containing the certificate in PEM-format
     * @return Ordered Collection of X509Certificate, first certificate first, or empty Collection
     * @exception IOException if the stream cannot be read.
     * @exception CertificateException if the stream does not contain a correct certificate.
     */
    public static Collection<X509Certificate> getCertsFromPEM(InputStream certstream) throws IOException, CertificateException {
        log.debug(">getCertfromPEM:");
        ArrayList<X509Certificate> ret = new ArrayList<X509Certificate>();
        String beginKey = "-----BEGIN CERTIFICATE-----";
        String endKey = "-----END CERTIFICATE-----";
        BufferedReader bufRdr = null;
        ByteArrayOutputStream ostr = null;
        PrintStream opstr = null;
        try {
            bufRdr = new BufferedReader(new InputStreamReader(certstream));
            while (bufRdr.ready()) {
                ostr = new ByteArrayOutputStream();
                opstr = new PrintStream(ostr);
                String temp;
                while ((temp = bufRdr.readLine()) != null && !temp.equals(beginKey)) continue;
                if (temp == null) throw new IOException("Error in " + certstream.toString() + ", missing " + beginKey + " boundary");
                while ((temp = bufRdr.readLine()) != null && !temp.equals(endKey)) opstr.print(temp);
                if (temp == null) throw new IOException("Error in " + certstream.toString() + ", missing " + endKey + " boundary");
                opstr.close();
                byte[] certbuf = Base64.decode(ostr.toByteArray());
                ostr.close();
                CertificateFactory cf = CertUtils.getCertificateFactory();
                X509Certificate x509cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certbuf));
                ret.add(x509cert);
            }
        } finally {
            if (bufRdr != null) bufRdr.close();
            if (opstr != null) opstr.close();
            if (ostr != null) ostr.close();
        }
        log.debug("<getcertfromPEM:" + ret.size());
        return ret;
    }

    /** Converts a regular array of certificates into an ArrayList, using the provided provided.
    * 
    * @param certs Certificate[] of certificates to convert
    * @param provider provider for example "SUN" or "BC", use null for the default provider (BC)
    * @return An ArrayList of certificates in the same order as the passed in array
    * @throws NoSuchProviderException 
    * @throws CertificateException 
    */
    public static ArrayList<X509Certificate> getCertCollectionFromArray(Certificate[] certs, String provider) throws CertificateException, NoSuchProviderException {
        if (log.isDebugEnabled()) {
            log.debug(">getCertCollectionFromArray: " + provider);
        }
        ArrayList<X509Certificate> ret = new ArrayList<X509Certificate>();
        String prov = provider;
        if (prov == null) {
            prov = "BC";
        }
        for (int i = 0; i < certs.length; i++) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509", prov);
            Certificate cert = certs[i];
            X509Certificate x509cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(cert.getEncoded()));
            ret.add(x509cert);
        }
        if (log.isDebugEnabled()) {
            log.debug("<getCertCollectionFromArray: " + ret.size());
        }
        return ret;
    }

    /**
     * Returns a certificate in PEM-format.
     *
     * @param cert the certificate to convert to PEM
     * @return byte array containing PEM certificate
     * @exception IOException if the stream cannot be read.
     * @exception CertificateException if the stream does not contain a correct certificate.
     */
    public static byte[] getPEMFromCerts(Collection<X509Certificate> certs) throws CertificateException {
        String beginKey = "-----BEGIN CERTIFICATE-----";
        String endKey = "-----END CERTIFICATE-----";
        ByteArrayOutputStream ostr = new ByteArrayOutputStream();
        PrintStream opstr = new PrintStream(ostr);
        Iterator<X509Certificate> iter = certs.iterator();
        while (iter.hasNext()) {
            X509Certificate cert = (X509Certificate) iter.next();
            byte[] certbuf = Base64.encode(cert.getEncoded());
            opstr.println("Subject: " + cert.getSubjectDN());
            opstr.println("Issuer: " + cert.getIssuerDN());
            opstr.println(beginKey);
            opstr.println(new String(certbuf));
            opstr.println(endKey);
        }
        opstr.close();
        byte[] ret = ostr.toByteArray();
        return ret;
    }

    /**
     * Creates X509CRL from byte[].
     *
     * @param crl byte array containing CRL in DER-format
     *
     * @return X509CRL
     *
     * @throws IOException if the byte array can not be read.
     * @throws CertificateException if the byte arrayen does not contani a correct CRL.
     * @throws CRLException if the byte arrayen does not contani a correct CRL.
     */
    public static X509CRL getCRLfromByteArray(byte[] crl) throws IOException, CRLException {
        log.debug(">getCRLfromByteArray:");
        if (crl == null) {
            throw new IOException("Cannot read byte[] that is 'null'!");
        }
        CertificateFactory cf = CertUtils.getCertificateFactory();
        X509CRL x509crl = (X509CRL) cf.generateCRL(new ByteArrayInputStream(crl));
        log.debug("<getCRLfromByteArray:");
        return x509crl;
    }

    /**
     * Checks if a certificate is self signed by verifying if subject and issuer are the same.
     *
     * @param cert the certificate that skall be checked.
     *
     * @return boolean true if the certificate has the same issuer and subject, false otherwise.
     */
    public static boolean isSelfSigned(X509Certificate cert) {
        log.debug(">isSelfSigned: cert: " + CertUtils.getIssuerDN(cert) + "\n" + CertUtils.getSubjectDN(cert));
        boolean ret = CertUtils.getSubjectDN(cert).equals(CertUtils.getIssuerDN(cert));
        log.debug("<isSelfSigned:" + ret);
        return ret;
    }

    /**
     * Generate a selfsigned certiicate.
     *
     * @param dn subject and issuer DN
     * @param validity in days
     * @param policyId policy string ('2.5.29.32.0') or null
     * @param privKey private key
     * @param pubKey public key
     * @param sigAlg signature algorithm, you can use one of the contants CATokenInfo.SIGALG_XXX
     * @param isCA boolean true or false
     *
     * @return X509Certificate, self signed
     *
     * @throws NoSuchAlgorithmException DOCUMENT ME!
     * @throws SignatureException DOCUMENT ME!
     * @throws InvalidKeyException DOCUMENT ME!
     * @throws IllegalStateException 
     * @throws CertificateEncodingException 
     */
    public static X509Certificate genSelfCert(String dn, long validity, String policyId, PrivateKey privKey, PublicKey pubKey, String sigAlg, boolean isCA) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateEncodingException, IllegalStateException {
        int keyusage = X509KeyUsage.keyCertSign + X509KeyUsage.cRLSign;
        return genSelfCertForPurpose(dn, validity, policyId, privKey, pubKey, sigAlg, isCA, keyusage);
    }

    /**
     * Generate a selfsigned certiicate with possibility to specify key usage.
     *
     * @param dn subject and issuer DN
     * @param validity in days
     * @param policyId policy string ('2.5.29.32.0') or null
     * @param privKey private key
     * @param pubKey public key
     * @param sigAlg signature algorithm, you can use one of the contants CATokenInfo.SIGALG_XXX
     * @param isCA boolean true or false
     * @param keyusage as defined by constants in X509KeyUsage
     *
     * @return X509Certificate, self signed
     *
     * @throws NoSuchAlgorithmException DOCUMENT ME!
     * @throws SignatureException DOCUMENT ME!
     * @throws InvalidKeyException DOCUMENT ME!
     * @throws IllegalStateException 
     * @throws CertificateEncodingException 
     */
    public static X509Certificate genSelfCertForPurpose(String dn, long validity, String policyId, PrivateKey privKey, PublicKey pubKey, String sigAlg, boolean isCA, int keyusage) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateEncodingException, IllegalStateException {
        Date firstDate = new Date();
        firstDate.setTime(firstDate.getTime() - (10 * 60 * 1000));
        Date lastDate = new Date();
        lastDate.setTime(lastDate.getTime() + (validity * (24 * 60 * 60 * 1000)));
        X509V3CertificateGenerator certgen = new X509V3CertificateGenerator();
        byte[] serno = new byte[8];
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed((new Date().getTime()));
        random.nextBytes(serno);
        certgen.setSerialNumber((new java.math.BigInteger(serno)).abs());
        certgen.setNotBefore(firstDate);
        certgen.setNotAfter(lastDate);
        certgen.setSignatureAlgorithm(sigAlg);
        certgen.setSubjectDN(CertUtils.stringToBcX509Name(dn));
        certgen.setIssuerDN(CertUtils.stringToBcX509Name(dn));
        certgen.setPublicKey(pubKey);
        BasicConstraints bc = new BasicConstraints(isCA);
        certgen.addExtension(X509Extensions.BasicConstraints.getId(), true, bc);
        if (isCA == true) {
            X509KeyUsage ku = new X509KeyUsage(keyusage);
            certgen.addExtension(X509Extensions.KeyUsage.getId(), true, ku);
        }
        try {
            if (isCA == true) {
                SubjectPublicKeyInfo spki = new SubjectPublicKeyInfo((ASN1Sequence) new ASN1InputStream(new ByteArrayInputStream(pubKey.getEncoded())).readObject());
                SubjectKeyIdentifier ski = new SubjectKeyIdentifier(spki);
                SubjectPublicKeyInfo apki = new SubjectPublicKeyInfo((ASN1Sequence) new ASN1InputStream(new ByteArrayInputStream(pubKey.getEncoded())).readObject());
                AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier(apki);
                certgen.addExtension(X509Extensions.SubjectKeyIdentifier.getId(), false, ski);
                certgen.addExtension(X509Extensions.AuthorityKeyIdentifier.getId(), false, aki);
            }
        } catch (IOException e) {
        }
        if (policyId != null) {
            PolicyInformation pi = new PolicyInformation(new DERObjectIdentifier(policyId));
            DERSequence seq = new DERSequence(pi);
            certgen.addExtension(X509Extensions.CertificatePolicies.getId(), false, seq);
        }
        X509Certificate selfcert = certgen.generate(privKey);
        return selfcert;
    }

    /**
     * Get the authority key identifier from a certificate extensions
     *
     * @param cert certificate containing the extension
     * @return byte[] containing the authority key identifier, or null if it does not exist
     * @throws IOException if extension can not be parsed
     */
    public static byte[] getAuthorityKeyId(X509Certificate cert) throws IOException {
        if (cert == null) {
            return null;
        }
        byte[] extvalue = cert.getExtensionValue("2.5.29.35");
        if (extvalue == null) {
            return null;
        }
        DEROctetString oct = (DEROctetString) (new ASN1InputStream(new ByteArrayInputStream(extvalue)).readObject());
        AuthorityKeyIdentifier keyId = new AuthorityKeyIdentifier((ASN1Sequence) new ASN1InputStream(new ByteArrayInputStream(oct.getOctets())).readObject());
        return keyId.getKeyIdentifier();
    }

    /**
     * Get the subject key identifier from a certificate extensions
     *
     * @param cert certificate containing the extension
     * @return byte[] containing the subject key identifier, or null if it does not exist
     * @throws IOException if extension can not be parsed
     */
    public static byte[] getSubjectKeyId(X509Certificate cert) throws IOException {
        if (cert == null) {
            return null;
        }
        byte[] extvalue = cert.getExtensionValue("2.5.29.14");
        if (extvalue == null) {
            return null;
        }
        ASN1OctetString str = ASN1OctetString.getInstance(new ASN1InputStream(new ByteArrayInputStream(extvalue)).readObject());
        SubjectKeyIdentifier keyId = SubjectKeyIdentifier.getInstance(new ASN1InputStream(new ByteArrayInputStream(str.getOctets())).readObject());
        return keyId.getKeyIdentifier();
    }

    /**
     * Get a certificate policy ID from a certificate policies extension
     *
     * @param cert certificate containing the extension
     * @param pos position of the policy id, if several exist, the first is as pos 0
     * @return String with the certificate policy OID
     * @throws IOException if extension can not be parsed
     */
    public static String getCertificatePolicyId(X509Certificate cert, int pos) throws IOException {
        byte[] extvalue = cert.getExtensionValue(X509Extensions.CertificatePolicies.getId());
        if (extvalue == null) {
            return null;
        }
        DEROctetString oct = (DEROctetString) (new ASN1InputStream(new ByteArrayInputStream(extvalue)).readObject());
        ASN1Sequence seq = (ASN1Sequence) new ASN1InputStream(new ByteArrayInputStream(oct.getOctets())).readObject();
        if (seq.size() < pos + 1) {
            return null;
        }
        PolicyInformation pol = new PolicyInformation((ASN1Sequence) seq.getObjectAt(pos));
        String id = pol.getPolicyIdentifier().getId();
        return id;
    }

    /**
     * Gets the Microsoft specific UPN altName.
     *
     * @param cert certificate containing the extension
     * @return String with the UPN name or null if the altName does not exist
     */
    public static String getUPNAltName(X509Certificate cert) throws IOException, CertificateParsingException {
        Collection<?> altNames = cert.getSubjectAlternativeNames();
        if (altNames != null) {
            Iterator<?> i = altNames.iterator();
            while (i.hasNext()) {
                ASN1Sequence seq = getAltnameSequence((List<?>) i.next());
                String ret = getUPNStringFromSequence(seq);
                if (ret != null) {
                    return ret;
                }
            }
        }
        return null;
    }

    /** Helper method for the above method
     */
    private static String getUPNStringFromSequence(ASN1Sequence seq) {
        if (seq != null) {
            DERObjectIdentifier id = DERObjectIdentifier.getInstance(seq.getObjectAt(0));
            if (id.getId().equals(CertUtils.UPN_OBJECTID)) {
                ASN1TaggedObject obj = (ASN1TaggedObject) seq.getObjectAt(1);
                DERUTF8String str = DERUTF8String.getInstance(obj.getObject());
                return str.getString();
            }
        }
        return null;
    }

    /**
     * Gets the Microsoft specific GUID altName, that is encoded as an octect string.
     *
     * @param cert certificate containing the extension
     * @return String with the hex-encoded GUID byte array or null if the altName does not exist
     */
    public static String getGuidAltName(X509Certificate cert) throws IOException, CertificateParsingException {
        Collection<?> altNames = cert.getSubjectAlternativeNames();
        if (altNames != null) {
            Iterator<?> i = altNames.iterator();
            while (i.hasNext()) {
                ASN1Sequence seq = getAltnameSequence((List<?>) i.next());
                if (seq != null) {
                    DERObjectIdentifier id = DERObjectIdentifier.getInstance(seq.getObjectAt(0));
                    if (id.getId().equals(CertUtils.GUID_OBJECTID)) {
                        ASN1TaggedObject obj = (ASN1TaggedObject) seq.getObjectAt(1);
                        ASN1OctetString str = ASN1OctetString.getInstance(obj.getObject());
                        return new String(Hex.encode(str.getOctets()));
                    }
                }
            }
        }
        return null;
    }

    /** Helper for the above methods 
     */
    private static ASN1Sequence getAltnameSequence(List<?> listitem) throws IOException {
        Integer no = (Integer) listitem.get(0);
        if (no.intValue() == 0) {
            byte[] altName = (byte[]) listitem.get(1);
            return getAltnameSequence(altName);
        }
        return null;
    }

    private static ASN1Sequence getAltnameSequence(byte[] value) throws IOException {
        DERObject oct = (new ASN1InputStream(new ByteArrayInputStream(value)).readObject());
        ASN1Sequence seq = ASN1Sequence.getInstance(oct);
        return seq;
    }

    /** Gets an altName string from an X509Extension
     * 
     * @param ext X509Extension with AlternativeNames
     * @return String as defined in method getSubjectAlternativeName
     */
    public static String getAltNameStringFromExtension(X509Extension ext) {
        String altName = null;
        ASN1OctetString octs = ext.getValue();
        if (octs != null) {
            ASN1InputStream aIn = new ASN1InputStream(new ByteArrayInputStream(octs.getOctets()));
            DERObject obj;
            try {
                obj = aIn.readObject();
                GeneralNames gan = GeneralNames.getInstance(obj);
                GeneralName[] gns = gan.getNames();
                for (int i = 0; i < gns.length; i++) {
                    GeneralName gn = gns[i];
                    int tag = gn.getTagNo();
                    DEREncodable name = gn.getName();
                    String str = CertUtils.getGeneralNameString(tag, name);
                    if (altName == null) {
                        altName = str;
                    } else {
                        altName += ", " + str;
                    }
                }
            } catch (IOException e) {
                log.error("IOException parsing altNames: ", e);
                return null;
            }
        }
        return altName;
    }

    /**
	 * SubjectAltName ::= GeneralNames
	 *
	 * GeneralNames :: = SEQUENCE SIZE (1..MAX) OF GeneralName
	 *
	 * GeneralName ::= CHOICE {
	 * otherName                       [0]     OtherName,
	 * rfc822Name                      [1]     IA5String,
	 * dNSName                         [2]     IA5String,
	 * x400Address                     [3]     ORAddress,
	 * directoryName                   [4]     Name,
	 * ediPartyName                    [5]     EDIPartyName,
	 * uniformResourceIdentifier       [6]     IA5String,
	 * iPAddress                       [7]     OCTET STRING,
	 * registeredID                    [8]     OBJECT IDENTIFIER}
	 * 
	 * SubjectAltName is of form \"rfc822Name=<email>,
	 * dNSName=<host name>, uniformResourceIdentifier=<http://host.com/>,
	 * iPAddress=<address>, guid=<globally unique id>, directoryName=<CN=testDirName|dir|name>
     * 
     * Supported altNames are upn, rfc822Name, uniformResourceIdentifier, dNSName, iPAddress, directoryName
	 *
	 * @author Marco Ferrante, (c) 2005 CSITA - University of Genoa (Italy)
     * @author Tomas Gustavsson
	 * @param certificate containing alt names
	 * @return String containing altNames of form "rfc822Name=email, dNSName=hostname, uniformResourceIdentifier=uri, iPAddress=ip, upn=upn, directoryName=CN=testDirName|dir|name" or null if no altNames exist. Values in returned String is from CertUtils constants. AltNames not supported are simply not shown in the resulting string.  
	 * @throws CertificateParsingException 
	 * @throws IOException 
	 * @throws java.lang.Exception
	 */
    public static String getSubjectAlternativeName(X509Certificate certificate) throws CertificateParsingException, IOException {
        log.debug("Search for SubjectAltName");
        if (certificate.getSubjectAlternativeNames() == null) return null;
        java.util.Collection<?> altNames = certificate.getSubjectAlternativeNames();
        if (altNames == null) {
            return null;
        }
        Iterator<?> iter = altNames.iterator();
        String result = "";
        String append = "";
        while (iter.hasNext()) {
            List<?> item = (List<?>) iter.next();
            Integer type = (Integer) item.get(0);
            Object value = item.get(1);
            if (!StringUtils.isEmpty(result)) {
                append = ", ";
            }
            switch(type.intValue()) {
                case 0:
                    ASN1Sequence seq = getAltnameSequence(item);
                    String upn = getUPNStringFromSequence(seq);
                    if (upn != null) {
                        result += append + CertUtils.UPN + "=" + upn;
                    }
                    break;
                case 1:
                    result += append + CertUtils.EMAIL + "=" + (String) value;
                    break;
                case 2:
                    result += append + CertUtils.DNS + "=" + (String) value;
                    break;
                case 3:
                    break;
                case 4:
                    result += append + CertUtils.DIRECTORYNAME + "=" + (String) value;
                    break;
                case 5:
                    break;
                case 6:
                    result += append + CertUtils.URI + "=" + (String) value;
                    break;
                case 7:
                    result += append + CertUtils.IPADDR + "=" + (String) value;
                    break;
                default:
                    break;
            }
        }
        if (StringUtils.isEmpty(result)) {
            return null;
        }
        return result;
    }

    /**
     * From an altName string as defined in getSubjectAlternativeName 
     * @param altName
     * @return ASN.1 GeneralNames
     * @see #getSubjectAlternativeName
     */
    public static GeneralNames getGeneralNamesFromAltName(String altName) {
        log.debug(">getGeneralNamesFromAltName: " + altName);
        ASN1EncodableVector vec = new ASN1EncodableVector();
        ArrayList<String> emails = CertUtils.getEmailFromDN(altName);
        if (!emails.isEmpty()) {
            Iterator<String> iter = emails.iterator();
            while (iter.hasNext()) {
                GeneralName gn = new GeneralName(1, new DERIA5String((String) iter.next()));
                vec.add(gn);
            }
        }
        ArrayList<String> dns = CertUtils.getPartsFromDN(altName, CertUtils.DNS);
        if (!dns.isEmpty()) {
            Iterator<String> iter = dns.iterator();
            while (iter.hasNext()) {
                GeneralName gn = new GeneralName(2, new DERIA5String((String) iter.next()));
                vec.add(gn);
            }
        }
        ArrayList<String> uri = CertUtils.getPartsFromDN(altName, CertUtils.URI);
        if (!uri.isEmpty()) {
            Iterator<String> iter = uri.iterator();
            while (iter.hasNext()) {
                GeneralName gn = new GeneralName(6, new DERIA5String((String) iter.next()));
                vec.add(gn);
            }
        }
        uri = CertUtils.getPartsFromDN(altName, CertUtils.URI1);
        if (!uri.isEmpty()) {
            Iterator<String> iter = uri.iterator();
            while (iter.hasNext()) {
                GeneralName gn = new GeneralName(6, new DERIA5String((String) iter.next()));
                vec.add(gn);
            }
        }
        uri = CertUtils.getPartsFromDN(altName, CertUtils.URI2);
        if (!uri.isEmpty()) {
            Iterator<String> iter = uri.iterator();
            while (iter.hasNext()) {
                GeneralName gn = new GeneralName(6, new DERIA5String((String) iter.next()));
                vec.add(gn);
            }
        }
        ArrayList<String> ipstr = CertUtils.getPartsFromDN(altName, CertUtils.IPADDR);
        if (!ipstr.isEmpty()) {
            Iterator<String> iter = ipstr.iterator();
            while (iter.hasNext()) {
                byte[] ipoctets = StringTools.ipStringToOctets((String) iter.next());
                GeneralName gn = new GeneralName(7, new DEROctetString(ipoctets));
                vec.add(gn);
            }
        }
        ArrayList<String> upn = CertUtils.getPartsFromDN(altName, CertUtils.UPN);
        if (!upn.isEmpty()) {
            Iterator<String> iter = upn.iterator();
            while (iter.hasNext()) {
                ASN1EncodableVector v = new ASN1EncodableVector();
                v.add(new DERObjectIdentifier(CertUtils.UPN_OBJECTID));
                v.add(new DERTaggedObject(true, 0, new DERUTF8String((String) iter.next())));
                DERObject gn = new DERTaggedObject(false, 0, new DERSequence(v));
                vec.add(gn);
            }
        }
        ArrayList<String> guid = CertUtils.getPartsFromDN(altName, CertUtils.GUID);
        if (!guid.isEmpty()) {
            Iterator<String> iter = guid.iterator();
            while (iter.hasNext()) {
                ASN1EncodableVector v = new ASN1EncodableVector();
                byte[] guidbytes = Hex.decode((String) iter.next());
                if (guidbytes != null) {
                    v.add(new DERObjectIdentifier(CertUtils.GUID_OBJECTID));
                    v.add(new DERTaggedObject(true, 0, new DEROctetString(guidbytes)));
                    DERObject gn = new DERTaggedObject(false, 0, new DERSequence(v));
                    vec.add(gn);
                } else {
                    log.error("Cannot decode hexadecimal guid: " + guid);
                }
            }
        }
        ArrayList<String> customoids = CertUtils.getCustomOids(altName);
        if (!customoids.isEmpty()) {
            Iterator<String> iter = customoids.iterator();
            while (iter.hasNext()) {
                String oid = (String) iter.next();
                ArrayList<String> oidval = CertUtils.getPartsFromDN(altName, oid);
                if (!oidval.isEmpty()) {
                    Iterator<String> valiter = oidval.iterator();
                    while (valiter.hasNext()) {
                        ASN1EncodableVector v = new ASN1EncodableVector();
                        v.add(new DERObjectIdentifier(oid));
                        v.add(new DERTaggedObject(true, 0, new DERUTF8String((String) valiter.next())));
                        DERObject gn = new DERTaggedObject(false, 0, new DERSequence(v));
                        vec.add(gn);
                    }
                }
            }
        }
        GeneralNames ret = null;
        if (vec.size() > 0) {
            ret = new GeneralNames(new DERSequence(vec));
        }
        return ret;
    }

    /**
     * GeneralName ::= CHOICE {
     * otherName                       [0]     OtherName,
     * rfc822Name                      [1]     IA5String,
     * dNSName                         [2]     IA5String,
     * x400Address                     [3]     ORAddress,
     * directoryName                   [4]     Name,
     * ediPartyName                    [5]     EDIPartyName,
     * uniformResourceIdentifier       [6]     IA5String,
     * iPAddress                       [7]     OCTET STRING,
     * registeredID                    [8]     OBJECT IDENTIFIER}
     * 
     * @param tag the no tag 0-8
     * @param value the DEREncodable value as returned by GeneralName.getName()
     * @return String in form rfc822Name=<email> or uri=<uri> etc 
     * @throws IOException 
     * @see #getSubjectAlternativeName
     */
    public static String getGeneralNameString(int tag, DEREncodable value) throws IOException {
        String ret = null;
        switch(tag) {
            case 0:
                ASN1Sequence seq = getAltnameSequence(value.getDERObject().getEncoded());
                String upn = getUPNStringFromSequence(seq);
                if (upn != null) {
                    ret = CertUtils.UPN + "=" + upn;
                }
                break;
            case 1:
                ret = CertUtils.EMAIL + "=" + DERIA5String.getInstance(value).getString();
                break;
            case 2:
                ret = CertUtils.DNS + "=" + DERIA5String.getInstance(value).getString();
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
                break;
            case 6:
                ret = CertUtils.URI + "=" + DERIA5String.getInstance(value).getString();
                break;
            case 7:
                ASN1OctetString oct = ASN1OctetString.getInstance(value);
                ret = CertUtils.IPADDR + "=" + StringTools.ipOctetsToString(oct.getOctets());
                break;
            default:
                break;
        }
        return ret;
    }

    /**
	 * Check the certificate with CA certificate.
	 *
	 * @param certificate cert to verify
	 * @param caCertPath collection of X509Certificate
	 * @return true if verified OK, false if not
	 */
    public static boolean verify(X509Certificate certificate, Collection<X509Certificate> caCertPath) throws Exception {
        try {
            ArrayList<X509Certificate> certlist = new ArrayList<X509Certificate>();
            certlist.add(certificate);
            CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
            java.security.cert.CertPath cp = cf.generateCertPath(certlist);
            X509Certificate[] cac = (X509Certificate[]) caCertPath.toArray(new X509Certificate[] {});
            java.security.cert.TrustAnchor anchor = new java.security.cert.TrustAnchor(cac[0], null);
            java.security.cert.PKIXParameters params = new java.security.cert.PKIXParameters(java.util.Collections.singleton(anchor));
            params.setRevocationEnabled(false);
            java.security.cert.CertPathValidator cpv = java.security.cert.CertPathValidator.getInstance("PKIX", "BC");
            java.security.cert.PKIXCertPathValidatorResult result = (java.security.cert.PKIXCertPathValidatorResult) cpv.validate(cp, params);
            log.debug("Certificate verify result: " + result.toString());
        } catch (java.security.cert.CertPathValidatorException cpve) {
            throw new Exception("Invalid certificate or certificate not issued by specified CA: " + cpve.getMessage());
        } catch (Exception e) {
            throw new Exception("Error checking certificate chain: " + e.getMessage());
        }
        return true;
    }

    /**
     * Return the CRL distribution point URL form a certificate.
     */
    public static URL getCrlDistributionPoint(X509Certificate certificate) throws CertificateParsingException {
        try {
            DERObject obj = getExtensionValue(certificate, X509Extensions.CRLDistributionPoints.getId());
            if (obj == null) {
                return null;
            }
            ASN1Sequence distributionPoints = (ASN1Sequence) obj;
            for (int i = 0; i < distributionPoints.size(); i++) {
                ASN1Sequence distrPoint = (ASN1Sequence) distributionPoints.getObjectAt(i);
                for (int j = 0; j < distrPoint.size(); j++) {
                    ASN1TaggedObject tagged = (ASN1TaggedObject) distrPoint.getObjectAt(j);
                    if (tagged.getTagNo() == 0) {
                        String url = getStringFromGeneralNames(tagged.getObject());
                        if (url != null) {
                            return new URL(url);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing CrlDistributionPoint", e);
            throw new CertificateParsingException(e.toString());
        }
        return null;
    }

    /** Returns OCSP URL that is inside AuthorithInformationAccess extension, or null.
     * 
     * @param cert
     * @return
     * @throws CertificateParsingException
     */
    public static String getAuthorityInformationAccessOcspUrl(X509Certificate cert) throws CertificateParsingException {
        try {
            DERObject obj = getExtensionValue(cert, X509Extensions.AuthorityInfoAccess.getId());
            if (obj == null) {
                return null;
            }
            AuthorityInformationAccess aia = AuthorityInformationAccess.getInstance(obj);
            AccessDescription[] ad = aia.getAccessDescriptions();
            if ((ad == null) || (ad.length < 1)) {
                return null;
            }
            if (!ad[0].getAccessMethod().equals(X509ObjectIdentifiers.ocspAccessMethod)) {
                return null;
            }
            GeneralName gn = ad[0].getAccessLocation();
            if (gn.getTagNo() != 6) {
                return null;
            }
            DERIA5String str = DERIA5String.getInstance(gn.getDERObject());
            return str.getString();
        } catch (Exception e) {
            log.error("Error parsing AuthorityInformationAccess", e);
            throw new CertificateParsingException(e.toString());
        }
    }

    /**
     * Return an Extension DERObject from a certificate
     */
    protected static DERObject getExtensionValue(X509Certificate cert, String oid) throws IOException {
        if (cert == null) {
            return null;
        }
        byte[] bytes = cert.getExtensionValue(oid);
        if (bytes == null) {
            return null;
        }
        ASN1InputStream aIn = new ASN1InputStream(new ByteArrayInputStream(bytes));
        ASN1OctetString octs = (ASN1OctetString) aIn.readObject();
        aIn = new ASN1InputStream(new ByteArrayInputStream(octs.getOctets()));
        return aIn.readObject();
    }

    private static String getStringFromGeneralNames(DERObject names) {
        ASN1Sequence namesSequence = ASN1Sequence.getInstance((ASN1TaggedObject) names, false);
        if (namesSequence.size() == 0) {
            return null;
        }
        DERTaggedObject taggedObject = (DERTaggedObject) namesSequence.getObjectAt(0);
        return new String(ASN1OctetString.getInstance(taggedObject, false).getOctets());
    }

    /**
     * Generate SHA1 fingerprint in string representation.
     *
     * @param ba Byte array containing DER encoded X509Certificate.
     *
     * @return String containing hex format of SHA1 fingerprint.
     */
    public static String getCertFingerprintAsString(byte[] ba) {
        try {
            X509Certificate cert = getCertfromByteArray(ba);
            byte[] res = generateSHA1Fingerprint(cert.getEncoded());
            return new String(Hex.encode(res));
        } catch (CertificateEncodingException cee) {
            log.error("Error encoding X509 certificate.", cee);
        } catch (CertificateException cee) {
            log.error("Error decoding X509 certificate.", cee);
        }
        return null;
    }

    /**
     * Generate SHA1 fingerprint of certificate in string representation.
     *
     * @param cert X509Certificate.
     *
     * @return String containing hex format of SHA1 fingerprint, or null if input is null.
     */
    public static String getFingerprintAsString(X509Certificate cert) {
        if (cert == null) return null;
        try {
            byte[] res = generateSHA1Fingerprint(cert.getEncoded());
            return new String(Hex.encode(res));
        } catch (CertificateEncodingException cee) {
            log.error("Error encoding X509 certificate.", cee);
        }
        return null;
    }

    /**
     * Generate SHA1 fingerprint of CRL in string representation.
     *
     * @param crl X509CRL.
     *
     * @return String containing hex format of SHA1 fingerprint.
     */
    public static String getFingerprintAsString(X509CRL crl) {
        try {
            byte[] res = generateSHA1Fingerprint(crl.getEncoded());
            return new String(Hex.encode(res));
        } catch (CRLException ce) {
            log.error("Error encoding X509 CRL.", ce);
        }
        return null;
    }

    /**
     * Generate SHA1 fingerprint of byte array in string representation.
     *
     * @param byte array to fingerprint.
     *
     * @return String containing hex format of SHA1 fingerprint.
     */
    public static String getFingerprintAsString(byte[] in) {
        byte[] res = generateSHA1Fingerprint(in);
        return new String(Hex.encode(res));
    }

    /**
     * Generate a SHA1 fingerprint from a byte array containing a X.509 certificate
     *
     * @param ba Byte array containing DER encoded X509Certificate.
     *
     * @return Byte array containing SHA1 hash of DER encoded certificate.
     */
    public static byte[] generateSHA1Fingerprint(byte[] ba) {
        log.debug(">generateSHA1Fingerprint");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            return md.digest(ba);
        } catch (NoSuchAlgorithmException nsae) {
            log.error("SHA1 algorithm not supported", nsae);
        }
        log.debug("<generateSHA1Fingerprint");
        return null;
    }

    /**
     * Generate a MD5 fingerprint from a byte array containing a X.509 certificate
     *
     * @param ba Byte array containing DER encoded X509Certificate.
     *
     * @return Byte array containing MD5 hash of DER encoded certificate.
     */
    public static byte[] generateMD5Fingerprint(byte[] ba) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(ba);
        } catch (NoSuchAlgorithmException nsae) {
            log.error("MD5 algorithm not supported", nsae);
        }
        return null;
    }

    /** Converts Sun Key usage bits to Bouncy castle key usage kits
     * 
     * @param sku key usage bit fields according to java.security.cert.X509Certificate#getKeyUsage, must be a boolean aray of size 9.
     * @return key usage int according to org.bouncycastle.jce.X509KeyUsage#X509KeyUsage, or -1 if input is null.
     * @see java.security.cert.X509Certificate#getKeyUsage
     * @see org.bouncycastle.jce.X509KeyUsage#X509KeyUsage
     */
    public static int sunKeyUsageToBC(boolean[] sku) {
        if (sku == null) {
            return -1;
        }
        int bcku = 0;
        if (sku[0] == true) bcku = bcku | X509KeyUsage.digitalSignature;
        if (sku[1] == true) bcku = bcku | X509KeyUsage.nonRepudiation;
        if (sku[2] == true) bcku = bcku | X509KeyUsage.keyEncipherment;
        if (sku[3] == true) bcku = bcku | X509KeyUsage.dataEncipherment;
        if (sku[4] == true) bcku = bcku | X509KeyUsage.keyAgreement;
        if (sku[5] == true) bcku = bcku | X509KeyUsage.keyCertSign;
        if (sku[6] == true) bcku = bcku | X509KeyUsage.cRLSign;
        if (sku[7] == true) bcku = bcku | X509KeyUsage.encipherOnly;
        if (sku[8] == true) bcku = bcku | X509KeyUsage.decipherOnly;
        return bcku;
    }

    /** Converts DERBitString ResonFlags to a RevokedCertInfo constant 
     * 
     * @param reasonFlags DERBITString received from org.bouncycastle.asn1.x509.ReasonFlags.
     * @return int according to org.ejbca.core.model.ca.crl.RevokedCertInfo
     */
    public static int bitStringToRevokedCertInfo(DERBitString reasonFlags) {
        int ret = RevokedCertInfo.REVOKATION_REASON_UNSPECIFIED;
        if (reasonFlags == null) {
            return ret;
        }
        int val = reasonFlags.intValue();
        if ((val & ReasonFlags.aACompromise) != 0) {
            ret = RevokedCertInfo.REVOKATION_REASON_AACOMPROMISE;
        }
        if ((val & ReasonFlags.affiliationChanged) != 0) {
            ret = RevokedCertInfo.REVOKATION_REASON_AFFILIATIONCHANGED;
        }
        if ((val & ReasonFlags.cACompromise) != 0) {
            ret = RevokedCertInfo.REVOKATION_REASON_CACOMPROMISE;
        }
        if ((val & ReasonFlags.certificateHold) != 0) {
            ret = RevokedCertInfo.REVOKATION_REASON_CERTIFICATEHOLD;
        }
        if ((val & ReasonFlags.cessationOfOperation) != 0) {
            ret = RevokedCertInfo.REVOKATION_REASON_CESSATIONOFOPERATION;
        }
        if ((val & ReasonFlags.keyCompromise) != 0) {
            ret = RevokedCertInfo.REVOKATION_REASON_KEYCOMPROMISE;
        }
        if ((val & ReasonFlags.privilegeWithdrawn) != 0) {
            ret = RevokedCertInfo.REVOKATION_REASON_PRIVILEGESWITHDRAWN;
        }
        if ((val & ReasonFlags.superseded) != 0) {
            ret = RevokedCertInfo.REVOKATION_REASON_SUPERSEDED;
        }
        if ((val & ReasonFlags.unused) != 0) {
            ret = RevokedCertInfo.REVOKATION_REASON_UNSPECIFIED;
        }
        return ret;
    }

    /**
     * Method used to insert a CN postfix into DN by extracting the first found CN appending cnpostfix and then replacing the original CN 
     * with the new one in DN.
     * 
     * If no CN could be found in DN then should the given DN be returned untouched
     * 
     * @param dn the DN to manipulate, cannot be null
     * @param cnpostfix the postfix to insert, cannot be null
     * @return the new DN
     */
    public static String insertCNPostfix(String dn, String cnpostfix) {
        String newdn = null;
        if ((dn != null) && (cnpostfix != null)) {
            String o;
            X509NameTokenizer xt = new X509NameTokenizer(dn);
            boolean alreadyreplaced = false;
            while (xt.hasMoreTokens()) {
                o = xt.nextToken();
                if (!alreadyreplaced && (o.length() > 3) && o.substring(0, 3).equalsIgnoreCase("cn=")) {
                    o += cnpostfix;
                    alreadyreplaced = true;
                }
                if (newdn == null) {
                    newdn = o;
                } else {
                    newdn += "," + o;
                }
            }
        }
        return newdn;
    }

    /** Simple method that looks at the certificate and determines, from EJBCA's standpoint, which signature algorithm it is
     * 
     * @param cert the cert to examine
     * @return Signature algorithm from CATokenInfo.SIGALG_SHA1_WITH_RSA etc.
     */
    public static String getSignatureAlgorithm(X509Certificate cert) {
        String certSignatureAlgorithm = cert.getSigAlgName();
        String signatureAlgorithm = null;
        PublicKey publickey = cert.getPublicKey();
        if (publickey instanceof RSAPublicKey) {
            if (certSignatureAlgorithm.indexOf("256") == -1) {
                signatureAlgorithm = CATokenInfo.SIGALG_SHA1_WITH_RSA;
            } else {
                signatureAlgorithm = CATokenInfo.SIGALG_SHA256_WITH_RSA;
            }
        } else {
            if (certSignatureAlgorithm.indexOf("256") == -1) {
                signatureAlgorithm = CATokenInfo.SIGALG_SHA1_WITH_ECDSA;
            } else {
                signatureAlgorithm = CATokenInfo.SIGALG_SHA256_WITH_ECDSA;
            }
        }
        log.debug("getSignatureAlgorithm: " + signatureAlgorithm);
        return signatureAlgorithm;
    }

    /**
     * class for breaking up an X500 Name into it's component tokens, ala
     * java.util.StringTokenizer. Taken from BouncyCastle, but does NOT
     * use or consider escaped characters. Used for reversing DNs without unescaping.
     */
    private static class BasicX509NameTokenizer {

        private String oid;

        private int index;

        private StringBuffer buf = new StringBuffer();

        public BasicX509NameTokenizer(String oid) {
            this.oid = oid;
            this.index = -1;
        }

        public boolean hasMoreTokens() {
            return (index != oid.length());
        }

        public String nextToken() {
            if (index == oid.length()) {
                return null;
            }
            int end = index + 1;
            boolean quoted = false;
            boolean escaped = false;
            buf.setLength(0);
            while (end != oid.length()) {
                char c = oid.charAt(end);
                if (c == '"') {
                    if (!escaped) {
                        buf.append(c);
                        quoted = !quoted;
                    } else {
                        buf.append(c);
                    }
                    escaped = false;
                } else {
                    if (escaped || quoted) {
                        buf.append(c);
                        escaped = false;
                    } else if (c == '\\') {
                        buf.append(c);
                        escaped = true;
                    } else if ((c == ',') && (!escaped)) {
                        break;
                    } else {
                        buf.append(c);
                    }
                }
                end++;
            }
            index = end;
            return buf.toString().trim();
        }
    }

    /**
     * Obtains a Vector with the DERObjectIdentifiers for 
     * dNObjects names.
     * 
     * @return Vector with DERObjectIdentifiers defining the known order we require
     */
    private static Vector<DERObjectIdentifier> getDefaultX509FieldOrder() {
        Vector<DERObjectIdentifier> fieldOrder = new Vector<DERObjectIdentifier>();
        String[] dNObjects = DnComponents.getDnObjects();
        for (int i = 0; i < dNObjects.length; i++) {
            fieldOrder.add(DnComponents.getOid(dNObjects[i]));
        }
        return fieldOrder;
    }

    /**
     * Obtains a Vector with the DERObjectIdentifiers for 
     * dNObjects names, in the specified order
     * 
     * @param ldaporder if true the returned order are as defined in LDAP RFC (CN=foo,O=bar,C=SE), otherwise the order is a defined in X.500 (C=SE,O=bar,CN=foo).
     * @return Vector with DERObjectIdentifiers defining the known order we require
     */
    public static Vector<Object> getX509FieldOrder(boolean ldaporder) {
        Vector<Object> fieldOrder = new Vector<Object>();
        String[] dNObjects = DnComponents.getDnObjects(ldaporder);
        for (int i = 0; i < dNObjects.length; i++) {
            fieldOrder.add(DnComponents.getOid(dNObjects[i]));
        }
        return fieldOrder;
    }

    /**
     * Obtain a X509Name reordered, if some fields from original X509Name 
     * doesn't appear in "ordering" parameter, they will be added at end 
     * in the original order.
     *   
     * @param x509Name the X509Name that is unordered 
     * @param ordering Vector of DERObjectIdentifier defining the desired order of components
     * @return X509Name with ordered conmponents according to the orcering vector
     */
    private static X509Name getOrderedX509Name(X509Name x509Name, Vector<DERObjectIdentifier> ordering, X509NameEntryConverter converter) {
        if (ordering == null) {
            ordering = new Vector<DERObjectIdentifier>();
        }
        Vector<DERObjectIdentifier> newOrdering = new Vector<DERObjectIdentifier>();
        Vector<Object> newValues = new Vector<Object>();
        Hashtable<DERObjectIdentifier, Object> ht = new Hashtable<DERObjectIdentifier, Object>();
        Iterator<DERObjectIdentifier> it = ordering.iterator();
        while (it.hasNext()) {
            DERObjectIdentifier oid = (DERObjectIdentifier) it.next();
            if (!ht.containsKey(oid)) {
                Vector<?> valueList = getX509NameFields(x509Name, oid);
                if (valueList != null) {
                    Iterator<?> itVals = valueList.iterator();
                    while (itVals.hasNext()) {
                        Object value = itVals.next();
                        ht.put(oid, value);
                        newOrdering.add(oid);
                        newValues.add(value);
                    }
                }
            }
        }
        Vector<?> allOids = x509Name.getOIDs();
        for (int i = 0; i < allOids.size(); i++) {
            DERObjectIdentifier oid = (DERObjectIdentifier) allOids.get(i);
            if (!ht.containsKey(oid)) {
                Vector<?> valueList = getX509NameFields(x509Name, oid);
                if (valueList != null) {
                    Iterator<?> itVals = valueList.iterator();
                    while (itVals.hasNext()) {
                        Object value = itVals.next();
                        ht.put(oid, value);
                        newOrdering.add(oid);
                        newValues.add(value);
                        log.debug("added --> " + oid + " val: " + value);
                    }
                }
            }
        }
        X509Name orderedName = new X509Name(newOrdering, newValues, converter);
        return orderedName;
    }

    /**
     * Obtain the values for a DN field from X509Name, or null in case
     * of the field does not exist.
     * 
     * @param name
     * @param id
     * @return
     */
    private static Vector<Object> getX509NameFields(X509Name name, DERObjectIdentifier id) {
        Vector<?> oids = name.getOIDs();
        Vector<?> values = name.getValues();
        Vector<Object> vRet = null;
        for (int i = 0; i < oids.size(); i++) {
            if (id.equals(oids.elementAt(i))) {
                if (vRet == null) {
                    vRet = new Vector<Object>();
                }
                vRet.add(values.get(i));
            }
        }
        return vRet;
    }
}
