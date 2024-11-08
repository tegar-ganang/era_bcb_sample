package org.privale.coreclients.cryptoclient;

import java.io.IOException;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.privale.utils.BytesChannel;
import org.privale.utils.ChannelReader;
import org.privale.utils.ChannelWriter;
import org.privale.utils.Reader;
import org.privale.utils.WriteBytesChannel;
import org.privale.utils.Writer;
import org.privale.utils.crypto.CryptoClientTool;

public class SymmetricHeaderCodec implements Reader, Writer {

    protected Header Head;

    protected byte[] DecodedHeader;

    protected byte[] EncodedHeader;

    protected CoreCrypto C;

    protected int EncodedLength;

    protected HashMan H;

    protected SymmetricHeaderCodec(CoreCrypto c) {
        C = c;
        H = new HashMan(C);
    }

    protected synchronized void setHeader(Header head) {
        Head = head;
    }

    protected synchronized void UpdateHeader() {
        Head.NextEncodedLength = EncodedLength;
        Head.Seed = H.Seed;
    }

    protected synchronized void UpdateFromHeader() {
        EncodedLength = Head.NextEncodedLength;
        H.Seed = Head.Seed;
    }

    protected synchronized void Encode() throws EncodeFailedException, IOException {
        BlockCipher e = C.KM.getSymmetricEngine();
        CBCBlockCipher c = new CBCBlockCipher(e);
        c.init(true, Head.SymKey.getParameters());
        CryptoClientTool tool = new CryptoClientTool(c);
        BytesChannel in = new BytesChannel(DecodedHeader);
        WriteBytesChannel out = new WriteBytesChannel();
        ChannelReader reader = new ChannelReader(in);
        ChannelWriter writer = new ChannelWriter(out);
        tool.Process(reader, writer);
        writer.close();
        EncodedHeader = out.getBytes();
        EncodedLength = EncodedHeader.length;
    }

    protected synchronized void Decode() throws DecodeFailedException, IOException {
        BlockCipher e = C.KM.getSymmetricEngine();
        CBCBlockCipher c = new CBCBlockCipher(e);
        c.init(false, Head.SymKey.getParameters());
        CryptoClientTool tool = new CryptoClientTool(c);
        BytesChannel in = new BytesChannel(EncodedHeader);
        WriteBytesChannel out = new WriteBytesChannel();
        ChannelReader reader = new ChannelReader(in);
        ChannelWriter writer = new ChannelWriter(out);
        tool.Process(reader, writer);
        writer.close();
        DecodedHeader = out.getBytes();
    }

    public synchronized void Read(ChannelReader chan) throws IOException {
        EncodedHeader = new byte[EncodedLength];
        chan.getBytes(EncodedHeader, EncodedHeader.length);
    }

    public synchronized void Write(ChannelWriter chan) throws IOException {
        chan.putBytes(EncodedHeader);
    }
}
