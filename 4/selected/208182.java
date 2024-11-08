package org.privale.coreclients.cryptoclient;

import java.io.IOException;
import org.bouncycastle.crypto.CipherParameters;
import org.privale.utils.BytesChannel;
import org.privale.utils.ChannelReader;
import org.privale.utils.ChannelWriter;
import org.privale.utils.WriteBytesChannel;

public class CipherParametersBytes {

    private CipherParametersChannel Chan;

    public CipherParametersBytes(CipherParametersChannel chan) {
        Chan = chan;
    }

    public synchronized CipherParametersChannel getChannel() {
        return Chan;
    }

    public synchronized CipherParameters getParameters() {
        return getChannel().getParameters();
    }

    public synchronized void setBytes(byte[] bytes) {
        BytesChannel bc = new BytesChannel(bytes);
        ChannelReader cr = new ChannelReader(bc);
        try {
            getChannel().Read(cr);
        } catch (IOException e) {
            getChannel().setParameters(null);
            e.printStackTrace();
        }
    }

    public synchronized byte[] getBytes() {
        WriteBytesChannel bc = new WriteBytesChannel();
        ChannelWriter cw = new ChannelWriter(bc);
        try {
            getChannel().Write(cw);
            cw.close();
            return bc.getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
