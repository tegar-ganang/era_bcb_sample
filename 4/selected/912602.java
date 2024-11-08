package org.crypthing.things.signer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.crypthing.things.config.Bundle;

/**
 * A facade to a CMS SignedData structure
 * @see http://www.ietf.org/rfc/rfc2630.txt?number=2630
 * @author yorickflannagan
 * @version 1.0
 * 
 */
public class CMSSignedData implements java.io.Serializable {

    private static final long serialVersionUID = 2722959179798713530L;

    private DEROctetString eContent;

    private ArrayList<Certificate> certificates;

    private ArrayList<CRL> crls;

    private ArrayList<CMSSignerInfo> signerInfos;

    /**
   * Creates a new SignedData structure
   */
    public CMSSignedData() {
        eContent = new DEROctetString(new byte[0]);
        certificates = new ArrayList<Certificate>();
        crls = new ArrayList<CRL>();
        signerInfos = new ArrayList<CMSSignerInfo>();
    }

    /**
   * Sets the eContent property using specified stream.
   * @param content - the eContent value input stream.
   * @throws IOException - If an I/O error occurs.
   */
    public void setContent(InputStream content) throws IOException {
        setContent(readBlockFromStream(content));
    }

    /**
   * Sets the eContent property.
   * @param content - the encapsulated content itself.
   */
    public void setContent(byte[] content) {
        eContent = new DEROctetString(content);
    }

    /**
   * Gets the eContent property value;
   * @return - the encapsulated content itself.
   */
    public byte[] getContent() {
        return eContent.getOctets();
    }

    /**
   * Gets an InputStream to read the encapsulated content.
   * @return - an InputStream.
   */
    public InputStream getContentStream() {
        return new ByteArrayInputStream(eContent.getOctets());
    }

    /**
   * Gets the certificates from this SignedData
   * @return - the embedded certificates chain.
   */
    public Certificate[] getCertificatesChain() {
        Certificate[] chain = new Certificate[certificates.size()];
        return certificates.toArray(chain);
    }

    /**
   * Includes a chain in this SignedData
   * @param chain - the new chain.
   */
    public void setCertificatesChain(Certificate[] chain) {
        certificates.clear();
        addCertificatesChain(chain);
    }

    /**
   * Adds a chain to embedded certificates chain.
   * @param newChain - a new chain to include.
   */
    public void addCertificatesChain(Certificate[] newChain) {
        for (int i = 0; i < newChain.length; i++) certificates.add(newChain[i]);
    }

    /**
   * Gets the CRLs from this SignedData
   * @return - the embedded CRL chain.
   */
    public CRL[] getCrls() {
        CRL[] list = new CRL[crls.size()];
        return crls.toArray(list);
    }

    /**
   * Includes a new CRL to this SignedData 
   * @param list - the new CRLl
   */
    public void setCrls(CRL[] list) {
        crls.clear();
        addCrls(list);
    }

    /**
   * Adds this CRLs to current list.
   * @param newList = a new CRL to include.
   */
    public void addCrls(CRL[] newList) {
        for (int i = 0; i < newList.length; i++) crls.add(newList[i]);
    }

    /**
   * Sets the signers field for this SignedData.
   * @param signers - the signers themselves.
   */
    public void setSigners(Collection<CMSSignerInfo> signers) {
        signerInfos.clear();
        signerInfos.addAll(signers);
    }

    /**
   * Sets the signers field for this SignedData.
   * @param signers - the signers themselves.
   */
    public void setSigners(CMSSignerInfo[] signers) {
        signerInfos.clear();
        for (int i = 0; i < signers.length; i++) signerInfos.add(signers[i]);
    }

    /**
   * Adds a new signer for this SignedData.
   * @param signer - the signer itself.
   */
    public void addSigner(CMSSignerInfo signer) {
        signerInfos.add(signer);
    }

    /**
   * Gets the signers field from this SignedData.
   * @return - the signers themselves.
   */
    public CMSSignerInfo[] getSigners() {
        CMSSignerInfo[] signers = new CMSSignerInfo[signerInfos.size()];
        return signerInfos.toArray(signers);
    }

    /**
   * Converts this instance of CMSSignedData to an array of bytes (DER encoded).
   * @return - the array of bytes.
   * @throws InvalidCMSCallException - if one of the following fields are missing: sid, digestAlgorithm, signedAttributes, signatureAlgorithm and signature.
   * @throws CMSParseException - if a parse error occurrs.
   */
    public byte[] toByteArray() {
        if ((certificates.size() == 0) || (signerInfos.size() == 0)) throw new InvalidCMSCallException(Bundle.getInstance().getResourceString(this, "CMS_MISSING_FIELD_ERROR"));
        SignedData signedData = new SignedData(getDigestAlgorithms(signerInfos), getContentInfo(eContent), getCertificatesFromChain(certificates), ((crls.size() == 0) ? null : null), getSignerInfos(signerInfos));
        ContentInfo envelope = new ContentInfo(new DERObjectIdentifier("1.2.840.113549.1.7.2"), signedData);
        return envelope.getDEREncoded();
    }

    /**
   * Populates the current instance of CMSSignedData with the stream contents .
   * @param stream - The input stream to parse.
   * @throws IOException - If a an I/O error occurred.
   */
    public void fromStream(InputStream stream) throws IOException {
        fromByteArray(readBlockFromStream(stream));
    }

    /**
   * Populates the current instance of CMSSignedData with the array of bytes contents.
   * @param stream - The input stream array of bytes.
   * @throws CMSParseException - if a parse error occurrs.
   */
    public void fromByteArray(byte[] stream) {
        SignedData data = parseStream(stream);
        eContent = (DEROctetString) data.getEncapContentInfo().getContent();
        setCertificatesChain(parseCertificates(data.getCertificates()));
        setSigners(parseSigners(data.getSignerInfos(), getCertificatesChain()));
    }

    private byte[] readBlockFromStream(InputStream in) throws IOException {
        byte[] buffer = new byte[4096];
        int readed = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((readed = in.read(buffer)) != -1) out.write(buffer, 0, readed);
        return out.toByteArray();
    }

    private DERSet getCertificatesFromChain(ArrayList<Certificate> chain) {
        ASN1EncodableVector set = new ASN1EncodableVector();
        for (int i = 0; i < chain.size(); i++) {
            try {
                ASN1StreamParser parser = new ASN1StreamParser(chain.get(i).getEncoded());
                DERSequence sequence = (DERSequence) parser.readObject().getDERObject();
                set.add(new X509CertificateStructure(sequence));
            } catch (IOException e) {
                throw new CMSParseException(Bundle.getInstance().getResourceString(this, "CMS_PARSE_ERROR"), e);
            } catch (CertificateEncodingException e) {
                throw new CMSParseException(Bundle.getInstance().getResourceString(this, "CMS_PARSE_ERROR"), e);
            }
        }
        return new DERSet(set);
    }

    private ContentInfo getContentInfo(DEROctetString document) {
        return new ContentInfo(new DERObjectIdentifier("1.2.840.113549.1.7.1"), (document.getOctets().length == 0) ? null : document);
    }

    private DERSet getSignerInfos(ArrayList<CMSSignerInfo> signers) {
        ASN1EncodableVector infos = new ASN1EncodableVector();
        for (int i = 0; i < signers.size(); i++) infos.add(signers.get(i).toCMSSignerInfo());
        return new DERSet(infos);
    }

    private DERSet getDigestAlgorithms(ArrayList<CMSSignerInfo> signers) {
        ASN1EncodableVector digestAlgorithms = new ASN1EncodableVector();
        for (int i = 0; i < signers.size(); i++) digestAlgorithms.add(signers.get(i).getDigestAlgorithm());
        return new DERSet(digestAlgorithms);
    }

    private SignedData parseStream(byte[] stream) {
        SignedData ret = null;
        try {
            ASN1StreamParser parser = new ASN1StreamParser(stream);
            ContentInfo content = new ContentInfo((ASN1Sequence) parser.readObject().getDERObject());
            if (content.getContentType().getId().contentEquals("1.2.840.113549.1.7.2")) {
                ret = SignedData.getInstance(content.getContent());
            } else {
                throw new CMSParseException("Criptographic envelope is not a SignedData");
            }
        } catch (IOException e) {
            throw new CMSParseException(Bundle.getInstance().getResourceString(this, "CMS_PARSE_ERROR"), e);
        }
        return ret;
    }

    private Certificate[] parseCertificates(ASN1Set from) {
        int i = 0;
        X509Certificate[] retVal = new X509Certificate[from.size()];
        while (i < from.size()) {
            DERSequence seq = (DERSequence) from.getObjectAt(i);
            X509CertificateStructure cert = new X509CertificateStructure(seq);
            ByteArrayInputStream stream = new ByteArrayInputStream(cert.getDEREncoded());
            CertificateFactory factory;
            try {
                factory = CertificateFactory.getInstance("X.509");
                retVal[i++] = (X509Certificate) factory.generateCertificate(stream);
            } catch (CertificateException e) {
                throw new CMSParseException(Bundle.getInstance().getResourceString(this, "CMS_PARSE_ERROR"), e);
            }
        }
        return retVal;
    }

    private CMSSignerInfo[] parseSigners(ASN1Set stream, Certificate[] chain) {
        CMSSignerInfo[] signers = new CMSSignerInfo[stream.size()];
        for (int i = 0; i < stream.size(); i++) {
            SignerInfo info = new SignerInfo((ASN1Sequence) stream.getObjectAt(i));
            CMSSignerInfo cmsInfo = new CMSSignerInfo();
            cmsInfo.fromCMSSignerInfo(info, chain);
            signers[i] = cmsInfo;
        }
        return signers;
    }
}
