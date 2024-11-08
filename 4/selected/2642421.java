package org.abisso.PortableNotary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfPKCS7;
import com.lowagie.text.pdf.PdfReader;

public class Notary {

    private static Writer out = new OutputStreamWriter(System.out);

    private static PrintWriter pwOut;

    static {
        pwOut = new PrintWriter(out, true);
    }

    private static String replaceBoolean(String str) {
        str = str.replace("true", Messages.getString("Notary.True")).replace("false", Messages.getString("Notary.False"));
        return str;
    }

    static void println(boolean verbose, String str) {
        if (verbose) pwOut.println(replaceBoolean(str));
    }

    static void printf(boolean verbose, String str, Object... args) {
        if (verbose) pwOut.printf(str, args);
    }

    public static boolean check(String certdb, String signed_doc, char[] password, boolean verbose) {
        KeyStore kall;
        try {
            kall = KeyStore.getInstance(KeyStore.getDefaultType());
            File keystore = new File(certdb);
            kall.load(new FileInputStream(keystore), password);
        } catch (CertificateException e1) {
            e1.printStackTrace();
            return false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        PdfReader reader;
        try {
            reader = new PdfReader(signed_doc);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        AcroFields af = reader.getAcroFields();
        ArrayList names = af.getSignatureNames();
        for (int k = 0; k < names.size(); ++k) {
            String name = (String) names.get(k);
            println(verbose, Messages.getString("Notary.SignatureName") + name);
            println(verbose, Messages.getString("Notary.SignatuteCoversWholeDocument") + af.signatureCoversWholeDocument(name));
            printf(verbose, Messages.getString("Notary.DecumentRevisionNOfM"), af.getRevision(name), af.getTotalRevisions());
            PdfPKCS7 pk = af.verifySignature(name);
            Calendar cal = pk.getSignDate();
            Certificate pkc[] = pk.getCertificates();
            println(true, Messages.getString("Notary.Subject") + PdfPKCS7.getSubjectFields(pk.getSigningCertificate()));
            try {
                println(true, Messages.getString("Notary.DocumentModified") + !pk.verify());
            } catch (SignatureException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < pkc.length; i++) {
                try {
                    Enumeration<String> aliases = kall.aliases();
                    while (true) {
                        String alias = aliases.nextElement();
                        if (kall.getCertificate(alias).getPublicKey().equals(pkc[i].getPublicKey())) println(true, Messages.getString("Notary.TrustedSignature"));
                    }
                } catch (KeyStoreException e) {
                    e.printStackTrace();
                } catch (NoSuchElementException e) {
                    break;
                }
            }
            Object fails[] = PdfPKCS7.verifyCertificates(pkc, kall, null, cal);
            if (fails == null) {
                println(true, Messages.getString("Notary.CertificateVerified"));
                return true;
            } else {
                println(true, Messages.getString("Notary.CertificateFailed") + fails[1]);
                return false;
            }
        }
        return false;
    }

    static boolean extractRevision(String extraction_path, String signed_doc, boolean verbose) {
        PdfReader reader;
        try {
            reader = new PdfReader(signed_doc);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        AcroFields af = reader.getAcroFields();
        ArrayList names = af.getSignatureNames();
        printf(verbose, "%d revision(s) found.", names.size());
        for (int k = 0; k < names.size(); ++k) {
            String name = (String) names.get(k);
            FileOutputStream out;
            try {
                out = new FileOutputStream(extraction_path + "revision_" + af.getRevision(name) + ".pdf");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            byte bb[] = new byte[8192];
            InputStream ip;
            try {
                ip = af.extractRevision(name);
                int n = 0;
                while ((n = ip.read(bb)) > 0) out.write(bb, 0, n);
                out.close();
                ip.close();
                println(verbose, Messages.getString("Notary.RevisionExtracted") + "revision_" + af.getRevision(name) + ".pdf");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
	 * @param out the out to set
	 */
    public static void setOutStream(Writer out) {
        Notary.out = out;
        pwOut = new PrintWriter(out, true);
    }
}
