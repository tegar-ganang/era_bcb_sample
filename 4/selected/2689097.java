package org.privale.coreclients.cryptoclient;

import java.nio.ByteBuffer;
import org.privale.node.PiggybackDataLayer;
import org.privale.utils.BytesChannel;
import org.privale.utils.ChannelReader;
import org.privale.utils.ChannelWriter;
import org.privale.utils.WriteBytesChannel;

public class PiggybackDataEncodeGenerator implements Generator {

    private PiggybackDataLayer Layer;

    private CoreCrypto C;

    public PiggybackDataEncodeGenerator(CoreCrypto c, PiggybackDataLayer layer) {
        C = c;
        Layer = layer;
    }

    public void Generate() {
        try {
            CipherParametersBytes symkeybytes = C.KM.GenSymKey();
            Layer.EncodeKey = ByteBuffer.wrap(symkeybytes.getBytes());
            Layer.PublicKey.clear();
            BytesChannel bc = new BytesChannel(Layer.PublicKey);
            ChannelReader cr = new ChannelReader(bc);
            CipherParametersChannel cc = C.KM.getPublicLinkedKeyChannel();
            cr.Read(cc);
            CipherParametersWithLink linkparms = (CipherParametersWithLink) cc.getParameters();
            CipherParametersChannel pubchan = C.KM.getPublicKeyChannel();
            pubchan.setParameters(linkparms.Parms);
            Header h = new Header(C);
            h.Instruction = 1;
            h.PublicKey = pubchan;
            h.SymKey = symkeybytes.getChannel();
            PiggybackDataHeader ph = new PiggybackDataHeader(C, linkparms.Link);
            C.setServerTimeIndex(ph);
            ph.setHeader(h);
            ph.GenCollision();
            ph.Encode();
            ph.UpdateHeader();
            h.Encode();
            WriteBytesChannel wbc = new WriteBytesChannel();
            ChannelWriter cw = new ChannelWriter(wbc);
            cw.Write(h);
            cw.Write(ph);
            cw.close();
            Layer.Header = wbc.getByteBuffer();
            Layer.EncodePacketDone = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
