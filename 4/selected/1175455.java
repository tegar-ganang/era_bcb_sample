package net.kano.joustsim.trust;

import net.kano.joscar.ByteBlock;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public final class TrustTools {

    private TrustTools() {
    }

    public static boolean isSigned(X509Certificate signer, X509Certificate cert) {
        try {
            cert.verify(signer.getPublicKey());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isCertificateAuthority(X509Certificate certificate) {
        return certificate.getBasicConstraints() != -1;
    }

    public static X509Certificate loadX509Certificate(File file) throws CertificateException, NoSuchProviderException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
        FileInputStream fin = new FileInputStream(file);
        Certificate cert;
        try {
            fin.getChannel().lock(0L, Long.MAX_VALUE, true);
            cert = cf.generateCertificate(fin);
        } finally {
            fin.close();
        }
        if (cert == null) {
            throw new NullPointerException("Unknown error: Certificate was " + "null");
        }
        if (!(cert instanceof X509Certificate)) {
            throw new IllegalArgumentException("this file is not an X.509 " + "certificate, it's a " + cert.getClass().getName());
        }
        return (X509Certificate) cert;
    }

    public static X509Certificate decodeCertificate(ByteBlock certData) throws NoSuchProviderException, CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509", "BC");
        InputStream is = ByteBlock.createInputStream(certData);
        return (X509Certificate) factory.generateCertificate(is);
    }
}
