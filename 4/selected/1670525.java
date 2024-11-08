package org.privale.coreclients.server2client;

import java.io.IOException;
import java.math.BigInteger;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DSA;
import org.privale.coreclients.cryptoclient.CipherParametersBytes;
import org.privale.coreclients.cryptoclient.CipherParametersChannel;
import org.privale.coreclients.cryptoclient.KeyMaster;
import org.privale.utils.BigIntChannel;
import org.privale.utils.ChannelReader;
import org.privale.utils.ChannelWriter;
import org.privale.utils.Reader;
import org.privale.utils.WriteBytesChannel;
import org.privale.utils.Writer;

public class Token implements Reader, Writer {

    public static byte HEADERBYTE = 0x25;

    public long MintID;

    public long RequestNumber;

    public int SigningRank;

    public long MintNodeID;

    public CipherParametersBytes NodePublicKey;

    public String IssuingServerConnectString;

    public long IssuingServerID;

    public long ExpirationTime;

    public BigInteger R;

    public BigInteger S;

    public transient KeyMaster KM;

    public Token() {
    }

    public void setKeyMaster(KeyMaster km) {
        KM = km;
    }

    public void Read(ChannelReader chan) throws IOException {
        byte header = chan.getByte();
        if (header != HEADERBYTE) {
            throw new IOException("Invalid header! Data is corrupt!");
        }
        MintID = chan.getLong();
        RequestNumber = chan.getLong();
        SigningRank = chan.getInt();
        MintNodeID = chan.getLong();
        byte nkb = chan.getByte();
        if (nkb == 1) {
            CipherParametersChannel parms = KM.getPublicKeyChannel();
            chan.Read(parms);
            NodePublicKey = new CipherParametersBytes(parms);
        } else {
            NodePublicKey = null;
        }
        IssuingServerConnectString = chan.getString();
        IssuingServerID = chan.getLong();
        ExpirationTime = chan.getLong();
        R = null;
        S = null;
        byte signed = chan.getByte();
        if (signed == 1) {
            BigIntChannel bi = new BigIntChannel();
            chan.Read(bi);
            R = bi.BI;
            chan.Read(bi);
            S = bi.BI;
        }
    }

    public void Write(ChannelWriter chan) throws IOException {
        chan.putByte(HEADERBYTE);
        chan.putLong(MintID);
        chan.putLong(RequestNumber);
        chan.putInt(SigningRank);
        chan.putLong(MintNodeID);
        if (NodePublicKey != null) {
            chan.putByte((byte) 1);
            chan.Write(NodePublicKey.getChannel());
        } else {
            chan.putByte((byte) 0);
        }
        chan.putString(IssuingServerConnectString);
        chan.putLong(IssuingServerID);
        chan.putLong(ExpirationTime);
        if (R == null || S == null) {
            chan.putByte((byte) 0);
        } else {
            chan.putByte((byte) 1);
            BigIntChannel bi = new BigIntChannel();
            bi.BI = R;
            chan.Write(bi);
            bi.BI = S;
            chan.Write(bi);
        }
    }

    public void Sign(DSA dsa, CipherParameters privkey) {
        WriteBytesChannel bc = new WriteBytesChannel();
        ChannelWriter chan = new ChannelWriter(bc);
        try {
            chan.putLong(MintID);
            chan.putLong(RequestNumber);
            chan.putInt(SigningRank);
            chan.putLong(MintNodeID);
            if (NodePublicKey != null) {
                chan.putByte((byte) 1);
                chan.Write(NodePublicKey.getChannel());
            } else {
                chan.putByte((byte) 0);
            }
            chan.putString(IssuingServerConnectString);
            chan.putLong(IssuingServerID);
            chan.putLong(ExpirationTime);
            chan.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dsa.init(true, privkey);
        BigInteger[] b = dsa.generateSignature(bc.getBytes());
        R = b[0];
        S = b[1];
    }

    public boolean Verify(DSA dsa, CipherParameters pubkey) {
        WriteBytesChannel bc = new WriteBytesChannel();
        ChannelWriter chan = new ChannelWriter(bc);
        try {
            chan.putLong(MintID);
            chan.putLong(RequestNumber);
            chan.putInt(SigningRank);
            chan.putLong(MintNodeID);
            if (NodePublicKey != null) {
                chan.putByte((byte) 1);
                chan.Write(NodePublicKey.getChannel());
            } else {
                chan.putByte((byte) 0);
            }
            chan.putString(IssuingServerConnectString);
            chan.putLong(IssuingServerID);
            chan.putLong(ExpirationTime);
            chan.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dsa.init(false, pubkey);
        return dsa.verifySignature(bc.getBytes(), R, S);
    }
}
