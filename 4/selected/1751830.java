package org.pfyshnet.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.RIPEMD128Digest;
import org.bouncycastle.crypto.engines.SerpentEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.ElGamalParameters;
import org.bouncycastle.crypto.params.ElGamalPrivateKeyParameters;
import org.bouncycastle.crypto.params.ElGamalPublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.pfyshnet.bc_codec.PfyshElGamalEngine;
import org.pfyshnet.bc_codec.PfyshNodePublicKeys;
import org.pfyshnet.bc_codec.PfyshOAEPEncoding;
import org.pfyshnet.bc_codec.PfyshParametersWithPrime;
import org.pfyshnet.core.DownloadSpecification;
import org.pfyshnet.core.NodeHello;
import org.pfyshnet.core.SearchSpecification;

public class BCUtils {

    private static final BigInteger ZERO = BigInteger.valueOf(0);

    private static final BigInteger TWO = BigInteger.valueOf(2);

    private static int BUFFERLEN = 1024;

    private static int CURRENT_PARAMIV_VERSION = 0x01;

    private static int CURRENT_FILE_VERSION = 0x01;

    private static int CURRENT_ELGAMAL_VERSION = 0x01;

    private static int CURRENT_RSA_VERSION = 0x01;

    private static int CURRENT_DS_VERSION = 0x01;

    private static int CURRENT_SS_VERSION = 0x01;

    private static int CURRENT_HELLO_VERSION = 0x01;

    private static int ENCODEDHEADERLENGTH = 0;

    private static int MAXBYTES = 384;

    private static long MAXFILE = 100L * 1024L * 1024L;

    private static final int SYMKEYSIZE = 256 / Byte.SIZE;

    private static final int SYMBLOCKSIZE = 16;

    public static PfyshParametersWithPrime genK(ElGamalPublicKeyParameters pub, SecureRandom random) {
        BigInteger p = pub.getParameters().getP();
        BigInteger k = new BigInteger(p.bitLength(), random);
        while (k.equals(ZERO) || (k.compareTo(p.subtract(TWO)) > 0)) {
            k = new BigInteger(p.bitLength(), random);
        }
        PfyshParametersWithPrime pr = new PfyshParametersWithPrime(pub, k);
        return pr;
    }

    public static ParametersWithIV getSymmetricKey(Random random) {
        byte[] key = new byte[SYMKEYSIZE];
        byte[] iv = new byte[SYMBLOCKSIZE];
        random.nextBytes(key);
        random.nextBytes(iv);
        KeyParameter keyparm = new KeyParameter(key);
        ParametersWithIV ivparm = new ParametersWithIV(keyparm, iv);
        return ivparm;
    }

    public static byte[] writeParametersWithIV(Object parms) {
        ParametersWithIV parmsiv = (ParametersWithIV) parms;
        KeyParameter keyparm = (KeyParameter) parmsiv.getParameters();
        byte[] outbytes = new byte[1 + (Integer.SIZE / Byte.SIZE) + parmsiv.getIV().length + (Integer.SIZE / Byte.SIZE) + keyparm.getKey().length];
        ByteBuffer buf = ByteBuffer.wrap(outbytes);
        buf.put((byte) CURRENT_PARAMIV_VERSION);
        buf.putInt(parmsiv.getIV().length);
        buf.put(parmsiv.getIV());
        buf.putInt(keyparm.getKey().length);
        buf.put(keyparm.getKey());
        return outbytes;
    }

    public static void writeParametersWithIV(DataOutputStream dos, Object parms) throws IOException {
        dos.write(writeParametersWithIV(parms));
    }

    public static ParametersWithIV readParametersWithIV(DataInputStream dis) throws IOException {
        int version = dis.read();
        if (version == CURRENT_PARAMIV_VERSION) {
            int ivlen = dis.readInt();
            if (ivlen > MAXBYTES) {
                throw new IOException("IV length too long! " + ivlen);
            }
            byte[] iv = new byte[ivlen];
            dis.read(iv);
            int keylen = dis.readInt();
            if (keylen > MAXBYTES) {
                throw new IOException("Key length too long! " + keylen);
            }
            byte[] key = new byte[keylen];
            dis.read(key);
            KeyParameter keyparms = new KeyParameter(key);
            ParametersWithIV parms = new ParametersWithIV(keyparms, iv);
            return parms;
        } else {
            throw new IOException("Invalid version! " + version);
        }
    }

    public static ParametersWithIV readParametersWithIV(byte[] db) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(db);
        int version = buf.get();
        if (version == CURRENT_PARAMIV_VERSION) {
            int ivlen = buf.getInt();
            if (ivlen > MAXBYTES) {
                throw new IOException("IV length too long! " + ivlen);
            }
            byte[] iv = new byte[ivlen];
            buf.get(iv);
            int keylen = buf.getInt();
            if (keylen > MAXBYTES) {
                throw new IOException("Key length too long! " + keylen);
            }
            byte[] key = new byte[keylen];
            buf.get(key);
            KeyParameter keyparms = new KeyParameter(key);
            ParametersWithIV parms = new ParametersWithIV(keyparms, iv);
            return parms;
        } else {
            throw new IOException("Invalid version! " + version);
        }
    }

    public static void writeNodeHello(DataOutputStream dos, NodeHello h) throws IOException {
        dos.write(CURRENT_HELLO_VERSION);
        byte[] conbytes = ((String) h.getConnectionLocation()).getBytes("ISO-8859-1");
        byte[] sig = (byte[]) h.getSignature();
        dos.writeLong(h.getHelloNumber());
        dos.writeInt(conbytes.length);
        dos.write(conbytes);
        PfyshNodePublicKeys pub = (PfyshNodePublicKeys) h.getPublicKey();
        if (pub.getEncryptionKey() instanceof PfyshParametersWithPrime) {
            PfyshParametersWithPrime pp = (PfyshParametersWithPrime) pub.getEncryptionKey();
            writeElGamalKey(dos, pp.getParameters());
        } else {
            writeElGamalKey(dos, pub.getEncryptionKey());
        }
        writeRSAKey(dos, pub.getVerificationKey());
        dos.writeInt(sig.length);
        dos.write(sig);
    }

    public static NodeHello readNodeHello(DataInputStream dis) throws IOException {
        int version = dis.read();
        if (version == CURRENT_HELLO_VERSION) {
            NodeHello h = new NodeHello();
            h.setHelloNumber(dis.readLong());
            int len = dis.readInt();
            if (len > MAXBYTES) {
                throw new IOException("length too long! " + len);
            }
            byte[] b = new byte[len];
            dis.read(b);
            h.setConnectionLocation(new String(b, "ISO-8859-1"));
            PfyshNodePublicKeys pub = new PfyshNodePublicKeys();
            pub.setEncryptionKey(readElGamalKey(dis));
            pub.setVerificationKey(readRSAKey(dis));
            h.setPublicKey(pub);
            len = dis.readInt();
            if (len > MAXBYTES) {
                throw new IOException("length too long! " + len);
            }
            b = new byte[len];
            dis.read(b);
            h.setSignature(b);
            return h;
        } else {
            throw new RuntimeException("Invalid version! " + version);
        }
    }

    public static void writeFile(DataOutputStream dos, Object o) throws IOException {
        File f = (File) o;
        dos.write(CURRENT_FILE_VERSION);
        dos.writeLong(f.length());
        FileInputStream fis = new FileInputStream(f);
        byte[] buffer = new byte[BUFFERLEN];
        int len = fis.read(buffer);
        while (len != -1) {
            if (len > 0) {
                dos.write(buffer, 0, len);
            }
            len = fis.read(buffer);
        }
        fis.close();
    }

    public static void readFile(DataInputStream dis, File f, boolean append) throws IOException {
        int version = dis.read();
        if (version == CURRENT_FILE_VERSION) {
            long filelen = dis.readLong();
            if (filelen > MAXFILE) {
                throw new IOException("File size too large! " + filelen);
            }
            FileOutputStream fos = new FileOutputStream(f, append);
            long runningtotal = 0L;
            byte[] buffer = new byte[BUFFERLEN];
            int numread = 0;
            while (runningtotal < filelen && numread != -1) {
                long numtoread = Math.min((long) buffer.length, filelen - runningtotal);
                int inumtoread = (int) numtoread;
                numread = dis.read(buffer, 0, inumtoread);
                if (numread > 0) {
                    fos.write(buffer, 0, numread);
                    runningtotal += numread;
                }
            }
            if (runningtotal != filelen) {
                throw new IOException("Not enough data read!");
            }
            fos.close();
        } else {
            throw new IOException("Invalid version! " + version);
        }
    }

    public static void writeElGamalKey(DataOutputStream dos, Object obj) throws IOException {
        ElGamalPublicKeyParameters pub = (ElGamalPublicKeyParameters) obj;
        dos.write(CURRENT_ELGAMAL_VERSION);
        byte[] b = pub.getParameters().getP().toByteArray();
        dos.writeInt(b.length);
        dos.write(b);
        b = pub.getParameters().getG().toByteArray();
        dos.writeInt(b.length);
        dos.write(b);
        b = pub.getY().toByteArray();
        dos.writeInt(b.length);
        dos.write(b);
    }

    public static ElGamalPublicKeyParameters readElGamalKey(DataInputStream dis) throws IOException {
        int version = dis.read();
        if (version == CURRENT_ELGAMAL_VERSION) {
            int len = dis.readInt();
            if (len > MAXBYTES) {
                throw new IOException("Byte length too long! " + len);
            }
            byte[] b = new byte[len];
            dis.read(b);
            BigInteger P = new BigInteger(b);
            len = dis.readInt();
            if (len > MAXBYTES) {
                throw new IOException("Byte length too long! " + len);
            }
            b = new byte[len];
            dis.read(b);
            BigInteger G = new BigInteger(b);
            len = dis.readInt();
            if (len > MAXBYTES) {
                throw new IOException("Byte length too long! " + len);
            }
            b = new byte[len];
            dis.read(b);
            BigInteger Y = new BigInteger(b);
            ElGamalParameters parms = new ElGamalParameters(P, G);
            ElGamalPublicKeyParameters pub = new ElGamalPublicKeyParameters(Y, parms);
            return pub;
        } else {
            throw new IOException("Invalid version! " + version);
        }
    }

    public static void writeElGamalPrivateKey(DataOutputStream dos, Object obj) throws IOException {
        ElGamalPrivateKeyParameters priv = (ElGamalPrivateKeyParameters) obj;
        dos.write(CURRENT_ELGAMAL_VERSION);
        byte[] b = priv.getParameters().getP().toByteArray();
        dos.writeInt(b.length);
        dos.write(b);
        b = priv.getParameters().getG().toByteArray();
        dos.writeInt(b.length);
        dos.write(b);
        b = priv.getX().toByteArray();
        dos.writeInt(b.length);
        dos.write(b);
    }

    public static ElGamalPrivateKeyParameters readElGamalPrivateKey(DataInputStream dis) throws IOException {
        int version = dis.read();
        if (version == CURRENT_ELGAMAL_VERSION) {
            int len = dis.readInt();
            if (len > MAXBYTES) {
                throw new IOException("Byte length too long! " + len);
            }
            byte[] b = new byte[len];
            dis.read(b);
            BigInteger P = new BigInteger(b);
            len = dis.readInt();
            if (len > MAXBYTES) {
                throw new IOException("Byte length too long! " + len);
            }
            b = new byte[len];
            dis.read(b);
            BigInteger G = new BigInteger(b);
            len = dis.readInt();
            if (len > MAXBYTES) {
                throw new IOException("Byte length too long! " + len);
            }
            b = new byte[len];
            dis.read(b);
            BigInteger X = new BigInteger(b);
            ElGamalParameters parms = new ElGamalParameters(P, G);
            ElGamalPrivateKeyParameters priv = new ElGamalPrivateKeyParameters(X, parms);
            return priv;
        } else {
            throw new IOException("Invalid version! " + version);
        }
    }

    public static void writeRSAKey(DataOutputStream dos, Object obj) throws IOException {
        RSAKeyParameters pubkey = (RSAKeyParameters) obj;
        dos.write(CURRENT_RSA_VERSION);
        byte[] b = pubkey.getExponent().toByteArray();
        dos.writeInt(b.length);
        dos.write(b);
        b = pubkey.getModulus().toByteArray();
        dos.writeInt(b.length);
        dos.write(b);
    }

    public static RSAKeyParameters readRSAKey(DataInputStream dis) throws IOException {
        int version = dis.read();
        if (version == CURRENT_RSA_VERSION) {
            int len = dis.readInt();
            if (len > MAXBYTES) {
                throw new IOException("Byte length too long! " + len);
            }
            byte[] b = new byte[len];
            dis.read(b);
            BigInteger exp = new BigInteger(b);
            len = dis.readInt();
            if (len > MAXBYTES) {
                throw new IOException("Byte length too long! " + len);
            }
            b = new byte[len];
            dis.read(b);
            BigInteger mod = new BigInteger(b);
            RSAKeyParameters pub = new RSAKeyParameters(false, mod, exp);
            return pub;
        } else {
            throw new IOException("Invalid version! " + version);
        }
    }

    @SuppressWarnings("unchecked")
    public static void writeDownloadSpecification(DataOutputStream dos, Object obj) throws IOException {
        DownloadSpecification ds = (DownloadSpecification) obj;
        dos.write(CURRENT_DS_VERSION);
        dos.writeLong(ds.getTag());
        writeParametersWithIV(dos, ds.getKey());
        dos.writeInt(ds.getGroupKeys().size());
        Iterator i = ds.getGroupKeys().iterator();
        while (i.hasNext()) {
            writeElGamalKey(dos, i.next());
        }
    }

    @SuppressWarnings("unchecked")
    public static DownloadSpecification readDownloadSpecification(DataInputStream dis) throws IOException {
        int version = dis.read();
        if (version == CURRENT_DS_VERSION) {
            DownloadSpecification ds = new DownloadSpecification();
            ds.setTag(dis.readLong());
            ds.setKey(readParametersWithIV(dis));
            int num = dis.readInt();
            ds.setGroupKeys(new LinkedList());
            for (int cnt = 0; cnt < num; cnt++) {
                ds.getGroupKeys().add(readElGamalKey(dis));
            }
            return ds;
        } else {
            throw new IOException("Invalid version! " + version);
        }
    }

    @SuppressWarnings("unchecked")
    public static void writeSearchSpecification(DataOutputStream dos, SearchSpecification sp) throws IOException {
        dos.write(CURRENT_SS_VERSION);
        dos.writeInt(sp.getGroupKeys().size());
        Iterator i = sp.getGroupKeys().iterator();
        while (i.hasNext()) {
            writeElGamalKey(dos, i.next());
        }
    }

    @SuppressWarnings("unchecked")
    public static SearchSpecification readSearchSpecification(DataInputStream dis) throws IOException {
        int version = dis.read();
        if (version == CURRENT_SS_VERSION) {
            SearchSpecification ss = new SearchSpecification();
            int num = dis.readInt();
            ss.setGroupKeys(new LinkedList());
            for (int cnt = 0; cnt < num; cnt++) {
                ss.getGroupKeys().add(readElGamalKey(dis));
            }
            return ss;
        } else {
            throw new IOException("Invalid version! " + version);
        }
    }

    public static void EncryptFile(File in, File out, Object key, boolean append) throws IOException {
        ParametersWithIV params = (ParametersWithIV) key;
        CBCBlockCipher enc = new CBCBlockCipher(new SerpentEngine());
        enc.init(true, params);
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out, append);
        byte[] inbuffer = new byte[enc.getBlockSize()];
        byte[] outbuffer = new byte[enc.getBlockSize()];
        int len = StreamUtils.fillBuffer(fis, inbuffer);
        while (len == inbuffer.length) {
            int enclen = enc.processBlock(inbuffer, 0, outbuffer, 0);
            if (enclen != inbuffer.length) {
                throw new RuntimeException("processBlock did not process the expected number of bytes!");
            }
            fos.write(outbuffer);
            len = StreamUtils.fillBuffer(fis, inbuffer);
        }
        if (len > 0) {
            byte[] pad = new byte[inbuffer.length - len];
            Arrays.fill(pad, (byte) 0);
            System.arraycopy(pad, 0, inbuffer, len, pad.length);
            int enclen = enc.processBlock(inbuffer, 0, outbuffer, 0);
            if (enclen != inbuffer.length) {
                throw new RuntimeException("processBlock did not process the expected number of bytes!");
            }
            fos.write(outbuffer);
        }
        fis.close();
        fos.close();
    }

    public static void DecryptFile(File in, File out, Object key, boolean append) throws IOException {
        ParametersWithIV params = (ParametersWithIV) key;
        CBCBlockCipher enc = new CBCBlockCipher(new SerpentEngine());
        enc.init(false, params);
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out, append);
        byte[] inbuffer = new byte[enc.getBlockSize()];
        byte[] outbuffer = new byte[enc.getBlockSize()];
        int len = StreamUtils.fillBuffer(fis, inbuffer);
        while (len == inbuffer.length) {
            int enclen = enc.processBlock(inbuffer, 0, outbuffer, 0);
            if (enclen != inbuffer.length) {
                throw new RuntimeException("processBlock did not process the expected number of bytes!");
            }
            fos.write(outbuffer);
            len = StreamUtils.fillBuffer(fis, inbuffer);
        }
        if (len > 0) {
            byte[] pad = new byte[inbuffer.length - len];
            Arrays.fill(pad, (byte) 0);
            System.arraycopy(pad, 0, inbuffer, len, pad.length);
            int enclen = enc.processBlock(inbuffer, 0, outbuffer, 0);
            if (enclen != inbuffer.length) {
                throw new RuntimeException("processBlock did not process the expected number of bytes!");
            }
            fos.write(outbuffer);
        }
        fis.close();
        fos.close();
    }

    public static byte[] EncryptHeader(byte[] data, Object key) throws InvalidCipherTextException {
        PfyshParametersWithPrime pub = (PfyshParametersWithPrime) key;
        PfyshOAEPEncoding enc = new PfyshOAEPEncoding(new PfyshElGamalEngine(), new RIPEMD128Digest());
        enc.init(true, pub);
        byte[] encblock = enc.processBlock(data, 0, data.length);
        if (ENCODEDHEADERLENGTH == 0) {
            ENCODEDHEADERLENGTH = encblock.length;
        } else {
            if (ENCODEDHEADERLENGTH != encblock.length) {
                throw new RuntimeException("header length varied! " + ENCODEDHEADERLENGTH + " to " + encblock.length);
            }
        }
        return encblock;
    }

    public static byte[] DecryptHeader(byte[] data, Object key) throws InvalidCipherTextException {
        PfyshOAEPEncoding enc = new PfyshOAEPEncoding(new PfyshElGamalEngine(), new RIPEMD128Digest());
        enc.init(false, (CipherParameters) key);
        byte[] decblock = enc.processBlock(data, 0, data.length);
        return decblock;
    }

    public static int getHeaderLength() {
        return ENCODEDHEADERLENGTH;
    }
}
