package org.privale.testclients;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.LinkedList;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ElGamalKeyPairGenerator;
import org.bouncycastle.crypto.generators.ElGamalParametersGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.ElGamalKeyGenerationParameters;
import org.bouncycastle.crypto.params.ElGamalParameters;
import org.bouncycastle.crypto.params.ElGamalPrivateKeyParameters;
import org.bouncycastle.crypto.params.ElGamalPublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.engines.ElGamalEngine;
import org.bouncycastle.crypto.engines.SerpentEngine;
import org.privale.utils.ChannelReader;
import org.privale.utils.ChannelWriter;
import org.privale.utils.FileManager;
import org.privale.utils.crypto.CryptoClientTool;
import org.privale.clients.BaseClient;
import org.privale.clients.CryptoClient;
import org.privale.clients.EncodeFailedException;
import org.privale.clients.EncodePacketGenFailedException;
import org.privale.node.DataDecodeFailedException;
import org.privale.node.PiggybackDataLayer;
import org.privale.node.PiggybackRouteNodes;
import org.privale.node.RemoteNode;
import org.privale.node.RouteNodes;
import org.privale.node.ClientBio;

public class BaseCrypto extends BaseClient implements CryptoClient {

    public BaseCrypto() {
        super(0x00100001L);
        getBio().setType(ClientBio.CryptType);
        getBio().setPublic(true);
        try {
            TmpDir = FileManager.getDir("basecrypto");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public File Decode(File data) throws DataDecodeFailedException {
        File outfile = null;
        try {
            ChannelReader cr = new ChannelReader(data);
            ByteBuffer encheader = cr.getByteBuffer(HeaderLength);
            ByteBuffer decheader = DecodeHeader(encheader, PrivKey);
            decheader.clear();
            byte[] keyb = new byte[SymKeySize / Byte.SIZE];
            decheader.get();
            decheader.get(keyb);
            ByteBuffer key = ByteBuffer.wrap(keyb);
            int instr = decheader.getInt();
            if (instr == 0x01) {
                File rf = cr.getToEndFile(TmpDir);
                cr.close();
                outfile = RawDecode(rf, key);
            } else if (instr == 0x02) {
                long len = decheader.getLong();
                File rf = cr.getFile(len, TmpDir);
                File encbot = cr.getToEndFile(TmpDir);
                cr.close();
                File topfile = RawDecode(rf, key);
                cr = new ChannelReader(topfile);
                int numkeys = cr.getInt();
                LinkedList<ByteBuffer> pigkeys = new LinkedList<ByteBuffer>();
                for (int cnt = 0; cnt < numkeys; cnt++) {
                    pigkeys.add(cr.getIntLenByteBuffer());
                }
                topfile = cr.getLongFile(TmpDir);
                cr.close();
                for (int cnt = 0; cnt < numkeys; cnt++) {
                    LinkedList<BigInteger> l = ByteBufferToBigInts(pigkeys.get(cnt));
                    BigInteger P = l.get(0);
                    BigInteger G = l.get(1);
                    BigInteger X = l.get(2);
                    ElGamalParameters ep = new ElGamalParameters(P, G);
                    ElGamalPrivateKeyParameters pk = new ElGamalPrivateKeyParameters(X, ep);
                    cr = new ChannelReader(encbot);
                    ByteBuffer pigenc = cr.getByteBuffer(HeaderLength);
                    ByteBuffer pigdec = DecodeHeader(pigenc, pk);
                    pigdec.get();
                    pigdec.get(keyb);
                    int val = pigdec.getInt();
                    if (val != 0x12345) {
                        throw new DataDecodeFailedException("Piggyback data decode failed!");
                    }
                    File decbot = cr.getToEndFile(TmpDir);
                    cr.close();
                    encbot = RawDecode(decbot, key);
                }
                ChannelWriter cw = new ChannelWriter(topfile, true);
                cw.putFile(encbot);
                cw.close();
                outfile = topfile;
            } else {
                decheader.clear();
                String outs = new String(decheader.array());
                System.out.println("INVALID DECODE! " + outs);
                System.out.println("INSTR = " + instr);
                decheader.clear();
                System.out.println("HEADER LEN = " + decheader.limit());
                throw new DataDecodeFailedException("Invalid decode instruction!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataDecodeFailedException(e.getMessage());
        }
        return outfile;
    }

    public void GenerateRouteEncodePacket(RouteNodes Nodes) throws EncodePacketGenFailedException {
        Nodes.EncodePacket = GenerateRawEncodeKey();
        ByteBuffer rawheader = ByteBuffer.allocate(1 + Nodes.EncodePacket.limit() + Integer.SIZE / Byte.SIZE + Long.SIZE / Byte.SIZE);
        Nodes.EncodePacket.clear();
        rawheader.put((byte) 5);
        rawheader.put(Nodes.EncodePacket);
        rawheader.putInt(0x01);
        rawheader.clear();
        for (int cnt = 0; cnt < Nodes.getNodes().size(); cnt++) {
            RemoteNode n = Nodes.getNodes().get(cnt);
            ClientBio bio = n.getClientID(getBio().getID());
            if (bio == null) {
                throw new EncodePacketGenFailedException("Node does not have bio!!");
            }
            BigInteger P = new BigInteger(bio.getProp("P"));
            BigInteger G = new BigInteger(bio.getProp("G"));
            BigInteger Y = new BigInteger(bio.getProp("Y"));
            ElGamalParameters parms = new ElGamalParameters(P, G);
            ElGamalPublicKeyParameters pub = new ElGamalPublicKeyParameters(Y, parms);
            Nodes.BaseEncodeHeaders.add(EncodeHeader(rawheader, pub));
        }
        Nodes.EncodePacketDone = true;
        System.out.println("ROUTE ENCODE GENERATED!");
    }

    public File EncodeRoute(RouteNodes Nodes, File Data) throws EncodeFailedException {
        File outfile = RawEncode(Data, Nodes.EncodePacket);
        return outfile;
    }

    public void GeneratePiggybackRouteEncodePacket(PiggybackRouteNodes Nodes) throws EncodePacketGenFailedException {
        ElGamalKeyGenerationParameters p = new ElGamalKeyGenerationParameters(getRandom(), getElGamalParams());
        ElGamalKeyPairGenerator g = new ElGamalKeyPairGenerator();
        g.init(p);
        AsymmetricCipherKeyPair pair = g.generateKeyPair();
        ElGamalPublicKeyParameters Pub = (ElGamalPublicKeyParameters) pair.getPublic();
        ElGamalPrivateKeyParameters Priv = (ElGamalPrivateKeyParameters) pair.getPrivate();
        BigInteger P = Pub.getParameters().getP();
        BigInteger G = Pub.getParameters().getG();
        BigInteger Y = Pub.getY();
        BigInteger X = Priv.getX();
        Nodes.PublicEncodeKeys.add(BigIntsToByteBuffer(P, G, Y));
        Nodes.PrivateEncodeKeys.add(BigIntsToByteBuffer(P, G, X));
        Nodes.EncodePacketDone = true;
        System.out.println("PIGGYBACK ROUTE ENCODE GENERATED!");
    }

    public File EncodePiggybackRoute(PiggybackRouteNodes Nodes, File Data) throws EncodeFailedException {
        File dataout = null;
        try {
            File outfile = TmpDir.createNewFile("tmp", "tmp");
            ChannelWriter cw = new ChannelWriter(outfile);
            int len = Nodes.PrivateEncodeKeys.size();
            cw.putInt(len);
            for (int cnt = 0; cnt < len; cnt++) {
                Nodes.PrivateEncodeKeys.get(cnt).clear();
                cw.putIntLenByteBuffer(Nodes.PrivateEncodeKeys.get(cnt));
            }
            cw.putLongFile(Data);
            cw.close();
            Nodes.EncodePacket = GenerateRawEncodeKey();
            Nodes.EncodePacket.clear();
            dataout = RawEncode(outfile, Nodes.EncodePacket);
            Nodes.EncodePacket.clear();
            ByteBuffer rawheader = ByteBuffer.allocate(1 + Nodes.EncodePacket.limit() + Integer.SIZE / Byte.SIZE + Long.SIZE / Byte.SIZE);
            rawheader.put((byte) 5);
            rawheader.put(Nodes.EncodePacket);
            rawheader.putInt(0x02);
            rawheader.putLong(dataout.length());
            for (int cnt = 0; cnt < Nodes.getNodes().size(); cnt++) {
                RemoteNode n = Nodes.getNodes().get(cnt);
                ClientBio bio = n.getClientID(getBio().getID());
                if (bio == null) {
                    throw new EncodePacketGenFailedException("Node does not have bio!!");
                }
                BigInteger P = new BigInteger(bio.getProp("P"));
                BigInteger G = new BigInteger(bio.getProp("G"));
                BigInteger Y = new BigInteger(bio.getProp("Y"));
                ElGamalParameters parms = new ElGamalParameters(P, G);
                ElGamalPublicKeyParameters pub = new ElGamalPublicKeyParameters(Y, parms);
                Nodes.BaseEncodeHeaders.add(EncodeHeader(rawheader, pub));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataout;
    }

    public void GeneratePiggybackDataEncode(PiggybackDataLayer layer) throws EncodePacketGenFailedException {
        layer.EncodeKey = GenerateRawEncodeKey();
        LinkedList<BigInteger> l = ByteBufferToBigInts(layer.PublicKey);
        BigInteger P = l.get(0);
        BigInteger G = l.get(1);
        BigInteger Y = l.get(2);
        ElGamalParameters p = new ElGamalParameters(P, G);
        ElGamalPublicKeyParameters pub = new ElGamalPublicKeyParameters(Y, p);
        layer.EncodeKey.clear();
        ByteBuffer rawheader = ByteBuffer.allocate(1 + layer.EncodeKey.limit() + Integer.SIZE / Byte.SIZE + Long.SIZE / Byte.SIZE);
        rawheader.put((byte) 5);
        rawheader.put(layer.EncodeKey);
        layer.EncodeKey.clear();
        rawheader.putInt(0x12345);
        rawheader.clear();
        layer.Header = EncodeHeader(rawheader, pub);
        layer.EncodePacketDone = true;
        System.out.println("PIGGYBACK DATA ENCODE GENERATED!");
    }

    public File EncodePiggybackData(PiggybackDataLayer layer, File data) throws EncodeFailedException {
        File outfile = RawEncode(data, layer.EncodeKey);
        File newout = null;
        try {
            newout = TmpDir.createNewFile("tmp", "tmp");
            ChannelWriter cw = new ChannelWriter(newout);
            layer.Header.clear();
            cw.putByteBuffer(layer.Header);
            cw.putFile(outfile);
            cw.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new EncodeFailedException(e.getMessage());
        }
        return newout;
    }

    public ByteBuffer GenerateRawEncodeKey() throws EncodePacketGenFailedException {
        byte[] keyb = new byte[SymKeySize / Byte.SIZE];
        getRandom().nextBytes(keyb);
        ByteBuffer ob = ByteBuffer.wrap(keyb);
        return ob;
    }

    public File RawEncode(File Data, ByteBuffer RawKey) throws EncodeFailedException {
        SerpentEngine e = new SerpentEngine();
        CBCBlockCipher c = new CBCBlockCipher(e);
        KeyParameter p = new KeyParameter(ChannelReader.BufToBytes(RawKey));
        c.init(true, p);
        CryptoClientTool tool = new CryptoClientTool(c);
        File outfile = null;
        try {
            ChannelReader reader = new ChannelReader(Data);
            outfile = TmpDir.createNewFile("tmp", "tmp");
            ChannelWriter writer = new ChannelWriter(outfile);
            tool.Process(reader, writer);
            reader.close();
            writer.close();
        } catch (IOException e1) {
            e1.printStackTrace();
            throw new EncodeFailedException(e1.getMessage());
        }
        return outfile;
    }

    public File RawDecode(File Data, ByteBuffer RawKey) throws EncodeFailedException {
        SerpentEngine e = new SerpentEngine();
        CBCBlockCipher c = new CBCBlockCipher(e);
        KeyParameter p = new KeyParameter(ChannelReader.BufToBytes(RawKey));
        c.init(false, p);
        CryptoClientTool tool = new CryptoClientTool(c);
        File outfile = null;
        try {
            ChannelReader reader = new ChannelReader(Data);
            outfile = TmpDir.createNewFile("tmp", "tmp");
            ChannelWriter writer = new ChannelWriter(outfile);
            tool.Process(reader, writer);
            reader.close();
            writer.close();
        } catch (IOException e1) {
            e1.printStackTrace();
            throw new EncodeFailedException(e1.getMessage());
        }
        return outfile;
    }

    public void start() {
        String ps = getProperties().getProperty("P");
        String gs = getProperties().getProperty("G");
        String ys = getProperties().getProperty("Y");
        String xs = getProperties().getProperty("X");
        BigInteger p = null;
        BigInteger g = null;
        BigInteger y = null;
        BigInteger x = null;
        if (ps == null || gs == null || ys == null || xs == null) {
            ElGamalKeyGenerationParameters parm = new ElGamalKeyGenerationParameters(getRandom(), getElGamalParams());
            ElGamalKeyPairGenerator gen = new ElGamalKeyPairGenerator();
            System.out.println("generating keys!!");
            gen.init(parm);
            KeyPair = gen.generateKeyPair();
            System.out.println("Keys generated!!");
            ElGamalPublicKeyParameters Pub = (ElGamalPublicKeyParameters) KeyPair.getPublic();
            p = Pub.getParameters().getP();
            g = Pub.getParameters().getG();
            y = Pub.getY();
            ElGamalPrivateKeyParameters Priv = (ElGamalPrivateKeyParameters) KeyPair.getPrivate();
            x = Priv.getX();
            getProperties().setProperty("P", p.toString());
            getProperties().setProperty("G", g.toString());
            getProperties().setProperty("Y", y.toString());
            getProperties().setProperty("X", x.toString());
        } else {
            p = new BigInteger(ps);
            g = new BigInteger(gs);
            y = new BigInteger(ys);
            x = new BigInteger(xs);
            ElGamalParameters parms = new ElGamalParameters(p, g);
            ElGamalPrivateKeyParameters priv = new ElGamalPrivateKeyParameters(x, parms);
            ElGamalPublicKeyParameters pub = new ElGamalPublicKeyParameters(y, parms);
            KeyPair = new AsymmetricCipherKeyPair(pub, priv);
        }
        PrivKey = (ElGamalPrivateKeyParameters) KeyPair.getPrivate();
        getBio().setProp("P", p.toString());
        getBio().setProp("G", g.toString());
        getBio().setProp("Y", y.toString());
        getNode().RegisterClientBio(this, getBio());
    }

    public void stop() {
    }

    private ByteBuffer EncodeHeader(ByteBuffer in, ElGamalPublicKeyParameters pub) {
        ElGamalEngine e = new ElGamalEngine();
        e.init(true, pub);
        byte[] buf = ChannelReader.BufToBytes(in);
        if (buf.length != 1 + SymKeySize / Byte.SIZE + Integer.SIZE / Byte.SIZE + Long.SIZE / Byte.SIZE) {
            System.out.println("HEADER NOT EXPECTED SIZE!!");
            Thread.dumpStack();
            System.exit(1);
        }
        byte[] cb = e.processBlock(buf, 0, buf.length);
        if (cb.length != HeaderLength && HeaderLength != -1) {
            System.out.println("HEADER SIZE MISMATCH!! Found: " + cb.length + " Expecting: " + HeaderLength);
            System.exit(1);
        }
        HeaderLength = cb.length;
        ByteBuffer outb = ByteBuffer.wrap(cb);
        return outb;
    }

    private ByteBuffer BigIntsToByteBuffer(BigInteger a, BigInteger b, BigInteger c) {
        byte[] ab = a.toString().getBytes();
        byte[] bb = b.toString().getBytes();
        byte[] cb = c.toString().getBytes();
        byte[] tb = new byte[ab.length + bb.length + cb.length + ((Integer.SIZE / Byte.SIZE) * 3)];
        ByteBuffer buf = ByteBuffer.wrap(tb);
        buf.putInt(ab.length);
        buf.put(ab);
        buf.putInt(bb.length);
        buf.put(bb);
        buf.putInt(cb.length);
        buf.put(cb);
        buf.clear();
        return buf;
    }

    private LinkedList<BigInteger> ByteBufferToBigInts(ByteBuffer buf) {
        LinkedList<BigInteger> l = new LinkedList<BigInteger>();
        buf.clear();
        int len = buf.getInt();
        byte[] ab = new byte[len];
        buf.get(ab, 0, len);
        String s = new String(ab);
        BigInteger a = new BigInteger(s);
        l.add(a);
        len = buf.getInt();
        ab = new byte[len];
        buf.get(ab, 0, len);
        s = new String(ab);
        a = new BigInteger(s);
        l.add(a);
        len = buf.getInt();
        ab = new byte[len];
        buf.get(ab, 0, len);
        s = new String(ab);
        a = new BigInteger(s);
        l.add(a);
        return l;
    }

    private ByteBuffer DecodeHeader(ByteBuffer in, ElGamalPrivateKeyParameters priv) {
        ElGamalEngine e = new ElGamalEngine();
        e.init(false, priv);
        byte[] buf = ChannelReader.BufToBytes(in);
        if (buf.length != HeaderLength) {
            System.out.println("CANNOT DECODE HEADER!! WRONG LENGTH!! " + buf.length);
            System.exit(1);
        }
        byte[] cb = e.processBlock(buf, 0, buf.length);
        if (cb.length != 1 + SymKeySize / Byte.SIZE + Integer.SIZE / Byte.SIZE + Long.SIZE / Byte.SIZE) {
            System.out.println("DECODED HEADER NOT EXPECTED SIZE!!");
            Thread.dumpStack();
            System.exit(1);
        }
        if (cb[0] != (byte) 5) {
            System.out.println("DECODED HEADER DOES NOT START WITH 5!");
            Thread.dumpStack();
            System.exit(1);
        }
        ByteBuffer outb = ByteBuffer.wrap(cb);
        return outb;
    }

    private ElGamalParameters getElGamalParams() {
        if (EParams == null) {
            ElGamalParametersGenerator g = new ElGamalParametersGenerator();
            g.init(576, 20, getRandom());
            EParams = g.generateParameters();
        }
        return EParams;
    }

    private SecureRandom getRandom() {
        if (RandMan == null) {
            RandMan = new SecureRandom();
        }
        return RandMan;
    }

    private int SymKeySize = 256;

    private FileManager TmpDir;

    public ElGamalPrivateKeyParameters PrivKey;

    private AsymmetricCipherKeyPair KeyPair;

    private static ElGamalParameters EParams;

    private static SecureRandom RandMan;

    private static int HeaderLength = -1;
}
