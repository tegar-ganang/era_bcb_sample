package org.infoeng.ofbiz.ltans.services;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import javolution.util.FastMap;
import java.io.IOException;
import java.util.Map;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.entity.GenericDelegator;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.*;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.EnvelopedData;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.pkcs.*;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.util.encoders.*;
import java.io.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;
import org.infoeng.ofbiz.ltans.scep.*;
import org.infoeng.ofbiz.ltans.util.*;

public class SCEPServices {

    public static Map submitPKCSRequest(DispatchContext dctx, Map context) throws Exception {
        KeyPair scepKeyPair = LtansUtils.getDefaultKeyPair();
        X509Certificate scepCert = LtansUtils.getDefaultCertificate();
        GenericDelegator delegator = dctx.getDelegator();
        String pkiMessageStr = (String) context.get("pkiMessageRequest");
        byte[] scepRequestContentInfoBytes = Base64.decode(pkiMessageStr.getBytes());
        ASN1InputStream aIS = new ASN1InputStream(scepRequestContentInfoBytes);
        ASN1Sequence seqTwo = (ASN1Sequence) aIS.readObject();
        ContentInfo inCtntInfo = ContentInfo.getInstance(seqTwo);
        CertificationRequest certReq = (CertificationRequest) LtansEnvelopeUtils.openSignedEnvelopedData(inCtntInfo, scepCert, scepKeyPair);
        CertificationRequestInfo certReqInfo = certReq.getCertificationRequestInfo();
        X509Name subject = certReqInfo.getSubject();
        SubjectPublicKeyInfo subjectKey = certReqInfo.getSubjectPublicKeyInfo();
        RSAPublicKeyStructure rsastruct = RSAPublicKeyStructure.getInstance(subjectKey);
        RSAPublicKeySpec rsaKey = new RSAPublicKeySpec(rsastruct.getModulus(), rsastruct.getPublicExponent());
        KeyFactory rsaFactory = KeyFactory.getInstance("RSA");
        RSAPublicKey rsaPubKey = (RSAPublicKey) rsaFactory.generatePublic(rsaKey);
        DERBitString derStr = certReq.getSignature();
        AlgorithmIdentifier sigalgId = certReq.getSignatureAlgorithm();
        Signature verifySig = Signature.getInstance(LtansUtils.getJavaAlgorithmIdentifier(sigalgId));
        verifySig.initVerify(rsaPubKey);
        verifySig.update(LtansUtils.getDERObjectBytes(certReq.getCertificationRequestInfo()));
        boolean signedTest = verifySig.verify(derStr.getBytes());
        if (!signedTest) Debug.logError(new IllegalArgumentException("PKCSRequest not correctly signed."), "PKCSRequest not correctly signed.");
        Map searchFields = FastMap.newInstance();
        searchFields.put("ltansPubKeyFactor", rsaKey.getPublicExponent());
        searchFields.put("ltansPubKeyName", certReqInfo.getSubject().toString());
        GenericValue pubKeyValue = delegator.findByPrimaryKey("LtansPublicKey", searchFields);
        if (pubKeyValue != null) Debug.logError(new IllegalArgumentException(), "Key already present for public exponent " + rsaKey.getPublicExponent() + ".");
        String pubkeyid = delegator.getNextSeqId("LtansPublicKey");
        Map pubKeyMap = FastMap.newInstance();
        pubKeyMap.put("ltansPubKeyId", pubkeyid);
        pubKeyMap.put("ltansPubKeyEncoded", rsaPubKey.getEncoded());
        pubKeyMap.put("ltansPubKeyName", certReqInfo.getSubject().toString());
        byte[] digestBytes = MessageDigest.getInstance(LtansUtils.DIGEST_ALGORITHM).digest(rsaPubKey.getEncoded());
        pubKeyMap.put("ltansEncodedPubKeyDigest", new String(Hex.encode(digestBytes)));
        pubKeyMap.put("ltansPubKeyType", "RSA");
        pubKeyMap.put("ltansPubKeyFactor", rsaKey.getPublicExponent().intValue());
        pubKeyValue = delegator.makeValue("LtansPublicKey", pubKeyMap);
        delegator.create(pubKeyValue);
        Map retMap = FastMap.newInstance();
        return retMap;
    }
}