package net.wimpi.telnetd.util;

import net.wimpi.telnetd.impl.Activator;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.*;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Provides a SSH2 Key Generator (RSA)
 * that can generate keys for the SSH2 Listener.
 * <p/>
 * Code is partially derived from:
 * Copyright (c) 2002,2003,2004,2005 ymnk, JCraft,Inc. All rights reserved.
 * <p/>
 * Requires Java 1.4?
 *
 * @author Dieter Wimberger (wimpi)
 * @version @version@ (@date@)
 */
public class SSH2KeyGenerator {

    private byte[] d;

    private byte[] e;

    private byte[] n;

    private byte[] c;

    private byte[] ep;

    private byte[] eq;

    private byte[] p;

    private byte[] q;

    private byte[] passphrase;

    private MessageDigest md;

    public static String createSSH2Key(OutputStream out, String password, int size) throws Exception {
        SSH2KeyGenerator kg = new SSH2KeyGenerator();
        kg.setPassphrase(password);
        kg.generate(size);
        kg.writePrivateKey(out);
        return kg.getFingerPrint(kg.generateMD(), size);
    }

    private void setPassphrase(String pass) {
        if (pass == null || pass.length() == 0) {
            passphrase = (byte[]) null;
        } else {
            passphrase = pass.getBytes();
        }
    }

    private void generate(int key_size) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(key_size, new SecureRandom());
        KeyPair pair = keyGen.generateKeyPair();
        PublicKey pubKey = pair.getPublic();
        PrivateKey prvKey = pair.getPrivate();
        d = ((RSAPrivateKey) prvKey).getPrivateExponent().toByteArray();
        e = ((RSAPublicKey) pubKey).getPublicExponent().toByteArray();
        n = ((RSAKey) prvKey).getModulus().toByteArray();
        c = ((RSAPrivateCrtKey) prvKey).getCrtCoefficient().toByteArray();
        ep = ((RSAPrivateCrtKey) prvKey).getPrimeExponentP().toByteArray();
        eq = ((RSAPrivateCrtKey) prvKey).getPrimeExponentQ().toByteArray();
        p = ((RSAPrivateCrtKey) prvKey).getPrimeP().toByteArray();
        q = ((RSAPrivateCrtKey) prvKey).getPrimeQ().toByteArray();
    }

    public void writePrivateKey(java.io.OutputStream out) {
        byte[] plain = getPrivateKey();
        byte[][] _iv = new byte[1][];
        byte[] encoded = encrypt(plain, _iv);
        byte[] iv = _iv[0];
        byte[] prv = toBase64(encoded, 0, encoded.length);
        try {
            out.write(begin);
            out.write(cr);
            if (passphrase != null) {
                out.write(header[0]);
                out.write(cr);
                out.write(header[1]);
                for (int i = 0; i < iv.length; i++) {
                    out.write(b2a((byte) ((iv[i] >>> 4) & 0x0f)));
                    out.write(b2a((byte) (iv[i] & 0x0f)));
                }
                out.write(cr);
                out.write(cr);
            }
            int i = 0;
            while (i < prv.length) {
                if (i + 64 < prv.length) {
                    out.write(prv, i, 64);
                    out.write(cr);
                    i += 64;
                    continue;
                }
                out.write(prv, i, prv.length - i);
                out.write(cr);
                break;
            }
            out.write(end);
            out.write(cr);
        } catch (Exception e) {
        }
    }

    private String getFingerPrint(MessageDigest md, int size) throws Exception {
        byte[] kblob = getPublicKeyBlob();
        if (kblob == null) return null;
        md.reset();
        md.update(kblob, 0, kblob.length);
        byte[] foo = md.digest();
        StringBuffer sb = new StringBuffer();
        int bar;
        for (int i = 0; i < foo.length; i++) {
            bar = foo[i] & 0xff;
            sb.append(chars[(bar >>> 4) & 0xf]);
            sb.append(chars[(bar) & 0xf]);
            if (i + 1 < foo.length) sb.append(":");
        }
        return "ssh-rsa " + size + " " + sb.toString();
    }

    public byte[] getPublicKeyBlob() {
        if (e == null) return null;
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream(sshrsa.length + 4 + e.length + 4 + n.length + 4);
            buf.write(toBytes(sshrsa.length));
            buf.write(sshrsa);
            buf.write(toBytes(e.length));
            buf.write(e);
            buf.write(toBytes(n.length));
            buf.write(n);
            return buf.toByteArray();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private byte[] toBytes(int val) {
        byte[] tmp = new byte[4];
        tmp[0] = (byte) (val >>> 24);
        tmp[1] = (byte) (val >>> 16);
        tmp[2] = (byte) (val >>> 8);
        tmp[3] = (byte) (val);
        return tmp;
    }

    private byte[] getPrivateKey() {
        int content = 1 + countLength(1) + 1 + 1 + countLength(n.length) + n.length + 1 + countLength(e.length) + e.length + 1 + countLength(d.length) + d.length + 1 + countLength(p.length) + p.length + 1 + countLength(q.length) + q.length + 1 + countLength(ep.length) + ep.length + 1 + countLength(eq.length) + eq.length + 1 + countLength(c.length) + c.length;
        int total = 1 + countLength(content) + content;
        byte[] plain = new byte[total];
        int index = 0;
        index = writeSEQUENCE(plain, index, content);
        index = writeINTEGER(plain, index, new byte[1]);
        index = writeINTEGER(plain, index, n);
        index = writeINTEGER(plain, index, e);
        index = writeINTEGER(plain, index, d);
        index = writeINTEGER(plain, index, p);
        index = writeINTEGER(plain, index, q);
        index = writeINTEGER(plain, index, ep);
        index = writeINTEGER(plain, index, eq);
        index = writeINTEGER(plain, index, c);
        return plain;
    }

    private static int writeSEQUENCE(byte[] buf, int index, int len) {
        buf[index++] = 0x30;
        index = writeLength(buf, index, len);
        return index;
    }

    private static int writeINTEGER(byte[] buf, int index, byte[] data) {
        buf[index++] = 0x02;
        index = writeLength(buf, index, data.length);
        System.arraycopy(data, 0, buf, index, data.length);
        index += data.length;
        return index;
    }

    private static int countLength(int len) {
        int i = 1;
        if (len <= 0x7f) return i;
        while (len > 0) {
            len >>>= 8;
            i++;
        }
        return i;
    }

    private static int writeLength(byte[] data, int index, int len) {
        int i = countLength(len) - 1;
        if (i == 0) {
            data[index++] = (byte) len;
            return index;
        }
        data[index++] = (byte) (0x80 | i);
        int j = index + i;
        while (i > 0) {
            data[index + i - 1] = (byte) (len & 0xff);
            len >>>= 8;
            i--;
        }
        return j;
    }

    private Cipher generateCipher() {
        try {
            return Cipher.getInstance("DESede/CBC/NoPadding");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private MessageDigest generateMD() {
        if (md == null) {
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            md.reset();
        }
        return md;
    }

    private byte[] generateKey(MessageDigest hash, byte[] passphrase, byte[] iv) {
        byte[] key = new byte[24];
        int hsize = hash.getDigestLength();
        byte[] hn = new byte[key.length / hsize * hsize + (key.length % hsize == 0 ? 0 : hsize)];
        try {
            byte[] tmp = null;
            for (int index = 0; index + hsize <= hn.length; ) {
                if (tmp != null) {
                    hash.update(tmp, 0, tmp.length);
                }
                hash.update(passphrase, 0, passphrase.length);
                hash.update(iv, 0, iv.length);
                tmp = hash.digest();
                System.arraycopy(tmp, 0, hn, index, tmp.length);
                index += tmp.length;
            }
            System.arraycopy(hn, 0, key, 0, key.length);
        } catch (Exception e) {
            Activator.getServices().error("generateKey()", e);
        }
        return key;
    }

    private byte[] encrypt(byte[] plain, byte[][] _iv) {
        if (passphrase == null) return plain;
        Cipher cipher = generateCipher();
        MessageDigest md = generateMD();
        SecureRandom rnd = new SecureRandom();
        _iv[0] = new byte[8];
        byte[] iv = _iv[0];
        rnd.nextBytes(iv);
        byte[] key = generateKey(md, passphrase, iv);
        byte[] encoded = plain;
        int bsize = 24;
        if (encoded.length % bsize != 0) {
            byte[] foo = new byte[(encoded.length / bsize + 1) * bsize];
            System.arraycopy(encoded, 0, foo, 0, encoded.length);
            encoded = foo;
        }
        try {
            DESedeKeySpec keyspec = new DESedeKeySpec(key);
            SecretKeyFactory keyfactory = SecretKeyFactory.getInstance("DESede");
            SecretKey _key = keyfactory.generateSecret(keyspec);
            cipher.init(Cipher.ENCRYPT_MODE, _key, new IvParameterSpec(iv));
            cipher.update(encoded, 0, encoded.length, encoded, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encoded;
    }

    private static byte[] toBase64(byte[] buf, int start, int length) {
        byte[] tmp = new byte[length * 2];
        int i, j, k;
        int foo = (length / 3) * 3 + start;
        i = 0;
        for (j = start; j < foo; j += 3) {
            k = (buf[j] >>> 2) & 0x3f;
            tmp[i++] = b64[k];
            k = (buf[j] & 0x03) << 4 | (buf[j + 1] >>> 4) & 0x0f;
            tmp[i++] = b64[k];
            k = (buf[j + 1] & 0x0f) << 2 | (buf[j + 2] >>> 6) & 0x03;
            tmp[i++] = b64[k];
            k = buf[j + 2] & 0x3f;
            tmp[i++] = b64[k];
        }
        foo = (start + length) - foo;
        if (foo == 1) {
            k = (buf[j] >>> 2) & 0x3f;
            tmp[i++] = b64[k];
            k = ((buf[j] & 0x03) << 4) & 0x3f;
            tmp[i++] = b64[k];
            tmp[i++] = (byte) '=';
            tmp[i++] = (byte) '=';
        } else if (foo == 2) {
            k = (buf[j] >>> 2) & 0x3f;
            tmp[i++] = b64[k];
            k = (buf[j] & 0x03) << 4 | (buf[j + 1] >>> 4) & 0x0f;
            tmp[i++] = b64[k];
            k = ((buf[j + 1] & 0x0f) << 2) & 0x3f;
            tmp[i++] = b64[k];
            tmp[i++] = (byte) '=';
        }
        byte[] bar = new byte[i];
        System.arraycopy(tmp, 0, bar, 0, i);
        return bar;
    }

    private static byte b2a(byte c) {
        if (0 <= c && c <= 9) return (byte) (c + '0');
        return (byte) (c - 10 + 'A');
    }

    private static final byte[] begin = "-----BEGIN RSA PRIVATE KEY-----".getBytes();

    private static final byte[] end = "-----END RSA PRIVATE KEY-----".getBytes();

    private static final byte[] b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".getBytes();

    private static final byte[] cr = "\n".getBytes();

    static byte[][] header = { "Proc-Type: 4,ENCRYPTED".getBytes(), "DEK-Info: DES-EDE3-CBC,".getBytes() };

    private static final byte[] sshrsa = "ssh-rsa".getBytes();

    private static String[] chars = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    /**
   * Test method, will generate a key and print it to standard out.
   *
   * @param args the input arguments.
   */
    public static void main(String[] args) {
        try {
            String fp = createSSH2Key(System.out, args[0], Integer.parseInt(args[1]));
            System.out.println();
            System.out.println("Fingerprint:" + fp);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
