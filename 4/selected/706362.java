package org.privale.coreclients.cryptoclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DSA;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.privale.clients.BaseClient;
import org.privale.clients.CryptoClient;
import org.privale.clients.EncodeFailedException;
import org.privale.clients.EncodePacketGenFailedException;
import org.privale.coreclients.newsnetworkclient.CryptoInt;
import org.privale.coreclients.server2client.Token;
import org.privale.node.ClientBio;
import org.privale.node.DataDecodeFailedException;
import org.privale.node.PiggybackDataLayer;
import org.privale.node.PiggybackRouteNodes;
import org.privale.node.RemoteNode;
import org.privale.node.RouteNodes;
import org.privale.utils.Base64Codec;
import org.privale.utils.BigIntChannel;
import org.privale.utils.BytesChannel;
import org.privale.utils.ChannelReader;
import org.privale.utils.ChannelWriter;
import org.privale.utils.FileManager;
import org.privale.utils.WriteBytesChannel;
import org.privale.utils.crypto.CryptoClientTool;
import org.privale.utils.crypto.KeyReader;

public class CoreCrypto extends BaseClient implements CryptoClient, ValidateDecoder, CryptoInt, Server2CryptoInterface {

    protected static byte NORMAL = 1;

    protected static byte PIGGYBACK = 2;

    public static long CRYPTOCLIENTID = 0x00300001;

    public static String KEYPROP = "Key";

    public static String TOKENFILEKEY = "TokenFile";

    public static String MINTFILEKEY = "MintPublicKeyFile";

    public static String KEYFILE = "KEYFILE";

    protected KeyMaster KM;

    protected FileDecoder Decoder;

    protected GenerateManager GenMan;

    protected SecureRandom RandMan;

    protected FileManager FM;

    protected FileManager Temp;

    private Token Token;

    private CipherParameters MintKey;

    private File TokenFile;

    private Timer TokenTimer;

    public CoreCrypto() {
        super(CRYPTOCLIENTID);
        getBio().setType(ClientBio.CryptType);
        getBio().setPublic(true);
        RandMan = new SecureRandom();
        Decoder = new FileDecoder(this);
        GenMan = new GenerateManager();
    }

    public File Decode(File data) throws DataDecodeFailedException {
        File rf = null;
        try {
            rf = Decoder.Decode(data);
        } catch (IOException e) {
            e.printStackTrace();
            throw new DataDecodeFailedException("**** I/O exception ***");
        } catch (DecodeFailedException e) {
            e.printStackTrace();
            throw new DataDecodeFailedException("**** Decode Failed ****");
        }
        return rf;
    }

    public void GenerateRouteEncodePacket(RouteNodes Nodes) throws EncodePacketGenFailedException {
        RouteEncodePacketGenerator gen = new RouteEncodePacketGenerator(this, Nodes);
        GenMan.Add(gen);
    }

    public File EncodeRoute(RouteNodes Nodes, File Data) throws EncodeFailedException {
        try {
            Nodes.EncodePacket.clear();
            BytesChannel bc = new BytesChannel(Nodes.EncodePacket);
            ChannelReader cr = new ChannelReader(bc);
            ByteBuffer key = cr.getIntLenByteBuffer();
            File EncFile = RawEncode(Data, key);
            File f = getTemp().createNewFile("encode", "route");
            ChannelWriter cw = new ChannelWriter(f);
            cw.putByteBuffer(cr.getIntLenByteBuffer());
            cw.putFile(EncFile);
            cw.close();
            return f;
        } catch (IOException e) {
            e.printStackTrace();
            throw new EncodeFailedException("Died");
        }
    }

    public void GeneratePiggybackRouteEncodePacket(PiggybackRouteNodes Nodes) throws EncodePacketGenFailedException {
        PBRouteEncodePacketGenerator gen = new PBRouteEncodePacketGenerator(this, Nodes);
        GenMan.Add(gen);
    }

    public File EncodePiggybackRoute(PiggybackRouteNodes Nodes, File Data) throws EncodeFailedException {
        File outfile = null;
        try {
            CipherParametersBytes symkey = KM.GenSymKey();
            Nodes.EncodePacket = ByteBuffer.wrap(symkey.getBytes());
            File basefile = getTemp().createNewFile("encode", "pigroute");
            ChannelWriter cw = new ChannelWriter(basefile);
            for (int cnt = 0; cnt < Nodes.PrivateEncodeKeys.size(); cnt++) {
                Nodes.PrivateEncodeKeys.get(cnt).clear();
                cw.putByteBuffer(Nodes.PrivateEncodeKeys.get(cnt));
                Nodes.PrivateEncodeKeys.get(cnt).clear();
            }
            cw.putFile(Data);
            cw.close();
            File encodedfile = RawEncode(basefile, Nodes.EncodePacket);
            Header head = new Header(this);
            head.SymKey = symkey.getChannel();
            PiggybackRouteHeader nh = new PiggybackRouteHeader(this);
            nh.setHeader(head);
            nh.DecodeLength = encodedfile.length();
            nh.NumberPiggybackDecodes = Nodes.PrivateEncodeKeys.size();
            nh.RouteFileLength = Data.length();
            nh.Encode();
            outfile = getTemp().createNewFile("encode", "pigroute");
            cw = new ChannelWriter(outfile);
            cw.Write(nh);
            cw.putFile(encodedfile);
            cw.close();
            boolean ok = true;
            for (int cnt = 0; cnt < Nodes.getNodes().size() && ok; cnt++) {
                ok = false;
                RemoteNode n = Nodes.getNodes().get(cnt);
                ClientBio b = n.getClientID(getBio().getID());
                if (b != null) {
                    String keystr = b.getProp(KEYPROP);
                    if (keystr != null) {
                        byte[] keyb = Base64Codec.Decode(keystr);
                        if (keyb != null) {
                            BytesChannel cb = new BytesChannel(keyb);
                            ChannelReader r = new ChannelReader(cb);
                            CipherParametersChannel keychan = KM.getPublicKeyChannel();
                            r.Read(keychan);
                            Header h = new Header(this);
                            h.PublicKey = keychan;
                            h.Instruction = PIGGYBACK;
                            h.SymKey = symkey.getChannel();
                            nh.setHeader(h);
                            nh.UpdateHeader();
                            h.Encode();
                            WriteBytesChannel wcb = new WriteBytesChannel();
                            cw = new ChannelWriter(wcb);
                            cw.Write(h);
                            cw.close();
                            Nodes.BaseEncodeHeaders.add(wcb.getByteBuffer());
                            ok = true;
                        }
                    }
                }
            }
            if (!ok) {
                throw new EncodeFailedException("!ok");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new EncodeFailedException("failed");
        }
        return outfile;
    }

    public void GeneratePiggybackDataEncode(PiggybackDataLayer layer) throws EncodePacketGenFailedException {
        PiggybackDataEncodeGenerator gen = new PiggybackDataEncodeGenerator(this, layer);
        GenMan.Add(gen);
    }

    public File EncodePiggybackData(PiggybackDataLayer layer, File data) throws EncodeFailedException {
        File encdata = RawEncode(data, layer.EncodeKey);
        try {
            File outfile = getTemp().createNewFile("encode", "pigdata");
            ChannelWriter cw = new ChannelWriter(outfile);
            layer.Header.clear();
            cw.putByteBuffer(layer.Header);
            layer.Header.clear();
            cw.putFile(encdata);
            cw.close();
            return outfile;
        } catch (IOException e) {
            e.printStackTrace();
            throw new EncodeFailedException("");
        }
    }

    public ByteBuffer GenerateRawEncodeKey() throws EncodePacketGenFailedException {
        CipherParametersBytes bytes = KM.GenSymKey();
        return ByteBuffer.wrap(bytes.getBytes());
    }

    public File RawEncode(File Data, ByteBuffer RawKey) throws EncodeFailedException {
        RawKey.clear();
        BytesChannel cb = new BytesChannel(RawKey);
        ChannelReader cr = new ChannelReader(cb);
        CipherParametersChannel cparms = KM.getSymmetricChannel();
        try {
            cr.Read(cparms);
            BlockCipher cipher = KM.getSymmetricEngine();
            CBCBlockCipher c = new CBCBlockCipher(cipher);
            c.init(true, cparms.getParameters());
            CryptoClientTool tool = new CryptoClientTool(c);
            cr = new ChannelReader(Data);
            File outfile = getTemp().createNewFile("encode", "raw");
            ChannelWriter cw = new ChannelWriter(outfile);
            tool.Process(cr, cw);
            cr.close();
            cw.close();
            return outfile;
        } catch (IOException e) {
            e.printStackTrace();
            throw new EncodeFailedException("failed");
        }
    }

    public File RawDecode(File Data, ByteBuffer RawKey) throws EncodeFailedException {
        RawKey.clear();
        BytesChannel cb = new BytesChannel(RawKey);
        ChannelReader cr = new ChannelReader(cb);
        CipherParametersChannel cparms = KM.getSymmetricChannel();
        try {
            cr.Read(cparms);
            BlockCipher cipher = KM.getSymmetricEngine();
            CBCBlockCipher c = new CBCBlockCipher(cipher);
            c.init(false, cparms.getParameters());
            CryptoClientTool tool = new CryptoClientTool(c);
            cr = new ChannelReader(Data);
            File outfile = getTemp().createNewFile("decode", "raw");
            ChannelWriter cw = new ChannelWriter(outfile);
            tool.Process(cr, cw);
            cr.close();
            cw.close();
            return outfile;
        } catch (IOException e) {
            e.printStackTrace();
            throw new EncodeFailedException("failed");
        }
    }

    public void start() {
        BringUpThread b = new BringUpThread(this);
        Thread t = new Thread(b);
        t.start();
    }

    public void stop() {
        if (TokenTimer != null) {
            TokenTimer.cancel();
        }
        KM.Stop();
        String keyfile = getProperties().getProperty(KEYFILE);
        File f = new File(keyfile);
        try {
            FileOutputStream fos = new FileOutputStream(f);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(KM);
            oos.close();
            CipherParametersChannel pubkey = KM.getNodePublicKey();
            File pubfile = FM.getFile("pubkey.dat");
            ChannelWriter cw = new ChannelWriter(pubfile);
            cw.Write(pubkey);
            cw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long DecodeValidation(byte[] b) {
        AsymmetricBlockCipher e = KM.getAsymmetricEngine();
        e.init(false, KM.getNodePrivateKey().getParameters());
        try {
            byte[] outbytes = e.processBlock(b, 0, b.length);
            byte[] longbytes = new byte[Long.SIZE / Byte.SIZE];
            System.arraycopy(outbytes, 0, longbytes, longbytes.length - outbytes.length, outbytes.length);
            ByteBuffer buf = ByteBuffer.wrap(longbytes);
            return buf.getLong();
        } catch (InvalidCipherTextException e1) {
            e1.printStackTrace();
        }
        return 0;
    }

    public long DecodeChallenge(ByteBuffer challenge, CipherParametersBytes sessionkey) throws ChallengeFailedException {
        long cv = 0;
        try {
            AsymmetricBlockCipher cipher = KM.getAsymmetricEngine();
            cipher.init(false, KM.getNodePrivateKey().getParameters());
            byte[] encodedbytes = ChannelReader.BufToBytes(challenge);
            byte[] decbytes = cipher.processBlock(encodedbytes, 0, encodedbytes.length);
            byte[] origsessionkey = sessionkey.getBytes();
            byte[] returnsessionkey = new byte[origsessionkey.length];
            System.arraycopy(decbytes, 0, returnsessionkey, 0, returnsessionkey.length);
            if (Arrays.equals(returnsessionkey, origsessionkey)) {
                System.out.println("RETURN CHALLENGE SUCCEEDED!");
                ByteBuffer buf = ByteBuffer.wrap(decbytes, returnsessionkey.length, Long.SIZE / Byte.SIZE);
                cv = buf.getLong();
            }
        } catch (Exception e) {
            throw new ChallengeFailedException(e.getMessage());
        }
        return cv;
    }

    public File EncodeToken(CipherParametersBytes sessionkey, byte[] serverpubkey) {
        try {
            File encfile = RawEncode(TokenFile, ByteBuffer.wrap(sessionkey.getBytes()));
            byte[] symkeybytes = sessionkey.getBytes();
            CipherParametersChannel cp = KM.getPublicKeyChannel();
            CipherParametersBytes cb = new CipherParametersBytes(cp);
            cb.setBytes(serverpubkey);
            AsymmetricBlockCipher cipher = KM.getAsymmetricEngine();
            cipher.init(true, cb.getParameters());
            byte[] encodedsymkey = cipher.processBlock(symkeybytes, 0, symkeybytes.length);
            File outfile = FM.createNewFile("enctoken", "dat");
            ChannelWriter cw = new ChannelWriter(outfile);
            cw.putIntLenBytes(encodedsymkey);
            cw.putFile(encfile);
            cw.close();
            return outfile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public CipherParametersBytes GenSessionKey() {
        return KM.GenSymKey();
    }

    public File getTokenFile() throws IOException {
        return FM.copyFile(TokenFile);
    }

    public org.privale.coreclients.server2client.Token getToken() {
        return Token;
    }

    /**
	 * We first decode the symmetric key with our token's matching
	 * private key.  Then we decode other nodes token with the 
	 * symmetric key.  We re-encode the sessionkey along with a 
	 * counter challenge value with the presented token's public key.
	 * The DepotChallenge object contains the session key, the
	 * encode challenge responce and the correct counter challenge
	 * answer we should get back.
	 * @param challengefile
	 * @return
	 */
    public DepotChallenge DecodeDepotChallenge(File challengefile) throws EncodeFailedException {
        try {
            ChannelReader cr = new ChannelReader(challengefile);
            byte[] encodedsessionkey = cr.getIntLenBytes();
            File encodedtoken = cr.getToEndFile(FM);
            cr.close();
            AsymmetricBlockCipher cipher = KM.getAsymmetricEngine();
            CipherParametersChannel privkey = KM.getNodePrivateKey();
            cipher.init(false, privkey.getParameters());
            byte[] symkey = cipher.processBlock(encodedsessionkey, 0, encodedsessionkey.length);
            DepotChallenge chan = new DepotChallenge();
            CipherParametersChannel sessionkeychan = KM.getSymmetricChannel();
            chan.SessionKey = new CipherParametersBytes(sessionkeychan);
            chan.SessionKey.setBytes(symkey);
            chan.SessionKeyBuf = ByteBuffer.wrap(symkey);
            File tokenfile = RawDecode(encodedtoken, chan.SessionKeyBuf);
            cr = new ChannelReader(tokenfile);
            Token t = new Token();
            t.setKeyMaster(KM);
            cr.Read(t);
            cr.close();
            WriteBytesChannel wbc = new WriteBytesChannel();
            ChannelWriter cw = new ChannelWriter(wbc);
            cw.putBytes(symkey);
            chan.CounterChallengeAnswer = getRandom().nextLong();
            cw.putLong(chan.CounterChallengeAnswer);
            cw.close();
            byte[] rawreturn = wbc.getBytes();
            cipher.init(true, t.NodePublicKey.getParameters());
            chan.CounterChallenge = cipher.processBlock(rawreturn, 0, rawreturn.length);
            return chan;
        } catch (IOException e) {
            e.printStackTrace();
            throw new EncodeFailedException(e.getMessage());
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
            throw new EncodeFailedException(e.getMessage());
        }
    }

    /**
	 * we decode the data with our token's private key.  We match
	 * the resulting session key with the one passed and return
	 * the counter challenge long value.
	 * @param challengefile
	 * @param sessionkey
	 * @return
	 */
    public long DecodeDepotCounterChallenge(byte[] challenge, CipherParametersBytes sessionkey) throws ChallengeFailedException {
        try {
            AsymmetricBlockCipher cipher = KM.getAsymmetricEngine();
            CipherParametersChannel privkey = KM.getNodePrivateKey();
            cipher.init(false, privkey.getParameters());
            byte[] decbytes = cipher.processBlock(challenge, 0, challenge.length);
            byte[] sessionbytes = sessionkey.getBytes();
            byte[] chkey = new byte[sessionbytes.length];
            System.arraycopy(decbytes, 0, chkey, 0, chkey.length);
            if (!Arrays.equals(chkey, sessionbytes)) {
                throw new ChallengeFailedException("Returned session key is incorrect!");
            }
            ByteBuffer buf = ByteBuffer.wrap(decbytes, chkey.length, Long.SIZE / Byte.SIZE);
            return buf.getLong();
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
            throw new ChallengeFailedException(e.getMessage());
        }
    }

    /**
	 * remotenodetoken is an unencoded token file. We create
	 * a symmetric session key and encode this node's
	 * token with it.  The symmetric key is encoded with the
	 * other nodes public key and our encoded token is attached.
	 * @param remotenodetoken
	 * @param sessionkey
	 * @return
	 */
    public File DepotSendChallenge(File remotenodetoken, CipherParametersBytes sessionkey) throws EncodeFailedException {
        try {
            ChannelReader cr = new ChannelReader(remotenodetoken);
            Token t = new Token();
            t.setKeyMaster(KM);
            cr.Read(t);
            cr.close();
            File sendtoken = RawEncode(TokenFile, ByteBuffer.wrap(sessionkey.getBytes()));
            AsymmetricBlockCipher cipher = KM.getAsymmetricEngine();
            cipher.init(true, t.NodePublicKey.getParameters());
            byte[] sessionkeybytes = sessionkey.getBytes();
            byte[] encodedsymkey = cipher.processBlock(sessionkeybytes, 0, sessionkeybytes.length);
            File returnfile = FM.createNewFile("depotsendchallenge", "dat");
            ChannelWriter cw = new ChannelWriter(returnfile);
            cw.putIntLenBytes(encodedsymkey);
            cw.putFile(sendtoken);
            cw.close();
            return returnfile;
        } catch (IOException e) {
            e.printStackTrace();
            throw new EncodeFailedException(e.getMessage());
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
            throw new EncodeFailedException(e.getMessage());
        }
    }

    public byte[] VerifyServerKey(String key) throws IOException {
        byte[] all = Base64Codec.Decode(key);
        BytesChannel bc = new BytesChannel(all);
        ChannelReader cr = new ChannelReader(bc);
        byte[] pubkey = cr.getIntLenBytes();
        BigIntChannel cbi = new BigIntChannel();
        cr.Read(cbi);
        BigInteger R = cbi.BI;
        cr.Read(cbi);
        BigInteger S = cbi.BI;
        DSA dsa = KM.getDSASigner();
        dsa.init(false, MintKey);
        if (!dsa.verifySignature(pubkey, R, S)) {
            pubkey = null;
        }
        return pubkey;
    }

    protected boolean isServerTimeIndexValid(ServerTimeIndex idx) {
        ServerTimeIndexUpdater updater = (ServerTimeIndexUpdater) getNode().getClientReference(org.privale.coreclients.server2client.CoreServer2.SERVERCLIENTID);
        if (updater != null) {
            return updater.isServerTimeIndexValid(idx);
        }
        return false;
    }

    protected void setServerTimeIndex(ServerTimeIndex idx) throws TimeIndexException {
        ServerTimeIndexUpdater updater = (ServerTimeIndexUpdater) getNode().getClientReference(org.privale.coreclients.server2client.CoreServer2.SERVERCLIENTID);
        if (updater != null) {
            updater.setServerTimeIndex(idx);
        }
    }

    protected boolean isKeyValid(CipherParametersChannel key) {
        return KM.isKeyOk(key);
    }

    protected FileManager getTemp() {
        return Temp;
    }

    protected SecureRandom getRandom() {
        return RandMan;
    }

    class BringUpThread implements Runnable {

        private CoreCrypto C;

        public BringUpThread(CoreCrypto c) {
            C = c;
        }

        public void run() {
            try {
                FM = FileManager.getDir("corecrypto");
                FM.HardDir = true;
                Temp = FileManager.getDir(FM.getPath() + File.separator + "data");
                Temp.HardDir = false;
                String keyfile = getProperties().getProperty(KEYFILE);
                if (keyfile == null) {
                    File tmpfile = FM.createNewFile("key", "data");
                    getProperties().setProperty(KEYFILE, tmpfile.getPath());
                    KM = new EGKeyMaster();
                } else {
                    File tmpfile = new File(keyfile);
                    if (tmpfile.exists()) {
                        FileInputStream fis = new FileInputStream(tmpfile);
                        ObjectInputStream ois = new ObjectInputStream(fis);
                        KM = (KeyMaster) ois.readObject();
                        ois.close();
                    } else {
                        KM = new EGKeyMaster();
                    }
                }
                System.out.println("KEY MASTER INITT!");
                KM.Init(C.getRandom());
                WriteBytesChannel wbc = new WriteBytesChannel();
                ChannelWriter cw = new ChannelWriter(wbc);
                cw.Write(KM.getNodePublicKey());
                cw.close();
                String keystr = Base64Codec.Encode(wbc.getBytes());
                getBio().setProp(KEYPROP, keystr);
                boolean ok = true;
                String errormsg = "";
                String mintkeyfile = getProperties().getProperty(MINTFILEKEY);
                String tokenkeyfile = getProperties().getProperty(TOKENFILEKEY);
                if (mintkeyfile == null || tokenkeyfile == null) {
                    if (mintkeyfile == null) {
                        mintkeyfile = "ERROR";
                    }
                    if (tokenkeyfile == null) {
                        tokenkeyfile = "ERROR";
                    }
                    ok = false;
                }
                if (ok) {
                    if (mintkeyfile.equals("ERROR") || tokenkeyfile.equals("ERROR")) {
                        ok = false;
                    }
                }
                if (ok) {
                    KeyReader reader = new KeyReader();
                    reader.ReadDSAPublicKey(mintkeyfile);
                    MintKey = reader.DSAPubKey;
                    TokenFile = new File(tokenkeyfile);
                    ChannelReader cr = new ChannelReader(TokenFile);
                    Token = new Token();
                    Token.setKeyMaster(KM);
                    cr.Read(Token);
                    cr.close();
                    if (!Token.Verify(KM.getDSASigner(), MintKey)) {
                        ok = false;
                        errormsg = "ERROR: Your token is invalid!";
                    } else {
                        Date d = new Date(Token.ExpirationTime);
                        System.out.println("=====================================================");
                        System.out.println();
                        System.out.println(" Your node ID: " + Token.MintNodeID);
                        System.out.println(" Token issue rank: " + Token.SigningRank);
                        System.out.println(" Expiration: " + d.toString());
                        System.out.println();
                        System.out.println("=====================================================");
                    }
                } else {
                    errormsg = "ERROR: No token file or mint key file specified!";
                }
                if (ok) {
                    System.out.println("REGISTERING CORE CRYPTO!!");
                    getNode().RegisterClientBio(C, getBio());
                    System.out.println("DONE REGISTERING CORE CRYPTO!!");
                    Date d = new Date(getToken().ExpirationTime);
                    TokenTimer = new Timer();
                    TokenTimer.schedule(new TokenExpired(C), d);
                } else {
                    getProperties().setProperty(TOKENFILEKEY, tokenkeyfile);
                    getProperties().setProperty(MINTFILEKEY, mintkeyfile);
                    getNode().ClientNodeShutdown(C, errormsg);
                }
            } catch (Exception e) {
                getNode().ClientNodeShutdown(C, "Exception caught while bringing up the crypto client!");
                e.printStackTrace();
            }
        }
    }

    private class TokenExpired extends TimerTask {

        public CoreCrypto C;

        public TokenExpired(CoreCrypto c) {
            C = c;
        }

        public void run() {
            C.getNode().ClientNodeShutdown(C, "\nERROR: YOUR TOKEN HAS EXPIRED!  Make sure your system time is correct.\n" + "You can trick your node into thinking your token has not expired yet\n" + "by setting your system time back, but you cannot trick the servers\n" + "or other nodes.  They will not let you connect.  Please get a new\n" + "token.\n");
        }
    }
}
