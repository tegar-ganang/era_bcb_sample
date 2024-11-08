package org.apache.xml.security.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import org.apache.poi.util.HexDump;
import org.apache.xml.security.algorithms.SignatureAlgorithm;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.signature.XMLSignatureException;
import org.bouncycastle.util.encoders.HexEncoder;
import org.openconcerto.modules.finance.payment.ebics.EbicsUtil;

/**
 * @author raul
 * 
 */
public class SignerOutputStream extends ByteArrayOutputStream {

    private static org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(SignerOutputStream.class);

    final SignatureAlgorithm sa;

    /**
     * @param sa
     */
    public SignerOutputStream(SignatureAlgorithm sa) {
        this.sa = sa;
    }

    /** @inheritDoc */
    public void write(byte[] arg0) {
        try {
            sa.update(arg0);
        } catch (XMLSignatureException e) {
            throw new RuntimeException("" + e);
        }
    }

    /** @inheritDoc */
    public void write(int arg0) {
        try {
            sa.update((byte) arg0);
        } catch (XMLSignatureException e) {
            throw new RuntimeException("" + e);
        }
    }

    /** @inheritDoc */
    public void write(byte[] arg0, int arg1, int arg2) {
        if (log.isDebugEnabled()) {
            log.debug("Canonicalized SignedInfo:");
            StringBuilder sb = new StringBuilder(arg2);
            for (int i = arg1; i < (arg1 + arg2); i++) {
                sb.append((char) arg0[i]);
            }
            log.debug(sb.toString());
        }
        try {
            System.err.println("SignerOutputStream.write():" + new String(arg0, arg1, arg2));
            try {
                MessageDigest hash = MessageDigest.getInstance("SHA-256", "BC");
                hash.reset();
                byte[] toHash = new byte[arg2];
                System.arraycopy(arg0, arg1, toHash, 0, arg2);
                EbicsUtil.saveToFile(toHash, new File("toHash.bin"));
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                for (int i = 0; i < toHash.length; i++) {
                    byte b = toHash[i];
                    o.write(b);
                }
                o.close();
                EbicsUtil.saveToFile(toHash, new File("toHash.bin"));
                byte[] r = hash.digest(toHash);
                System.err.println("== toHash");
                System.err.println(new String(toHash));
                System.err.println(HexDump.toHex(toHash));
                System.err.println("== r");
                System.err.println(HexDump.toHex(r));
            } catch (Exception e) {
                e.printStackTrace();
            }
            sa.update(arg0, arg1, arg2);
        } catch (XMLSignatureException e) {
            throw new RuntimeException("" + e);
        }
    }
}
