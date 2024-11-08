package com.sshtools.j2ssh.forwarding;

import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.sshtools.j2ssh.connection.SshMsgChannelData;

public class ForwardingSocketX11Channel extends ForwardingSocketChannel {

    private static Log log = LogFactory.getLog(ForwardingSocketX11Channel.class);

    private String authType;

    private String fakeAuthData;

    private String realAuthData;

    public ForwardingSocketX11Channel(String forwardType, String name, String hostToConnectOrBind, int portToConnectOrBind, String originatingHost, int originatingPort, String authType, String fakeAuthData, String realAuthData) throws ForwardingConfigurationException {
        super(forwardType, name, hostToConnectOrBind, portToConnectOrBind, originatingHost, originatingPort);
        this.authType = authType;
        this.fakeAuthData = fakeAuthData;
        this.realAuthData = realAuthData;
    }

    private boolean firstPacket = true;

    private byte[] dataSoFar = null;

    private boolean allDataCheckSubst() throws IOException {
        int plen, dlen;
        if (dataSoFar.length < 12) return false;
        if (dataSoFar[0] == 0x42) {
            plen = 256 * dataSoFar[6] + dataSoFar[7];
            dlen = 256 * dataSoFar[8] + dataSoFar[9];
        } else if (dataSoFar[0] == 0x6C) {
            plen = 256 * dataSoFar[7] + dataSoFar[6];
            dlen = 256 * dataSoFar[9] + dataSoFar[8];
        } else {
            throw new IOException("Bad initial X11 packet: bad byte order byte: " + dataSoFar[0]);
        }
        if (dataSoFar.length < (12 + ((plen + 3) & ~3) + ((dlen + 3) & ~3))) return false;
        if (plen != authType.length()) {
            throw new IOException("X11 connection uses different authentication protocol.");
        } else {
            if (!authType.equals(new String(dataSoFar, 12, plen, "US-ASCII"))) {
                throw new IOException("X11 connection uses different authentication protocol.");
            }
        }
        if (fakeAuthData.length() != realAuthData.length()) throw new IOException("fake and real X11 authentication data differ in length.");
        int len = fakeAuthData.length() / 2;
        byte newdata[] = new byte[len];
        if (dlen != len) {
            throw new IOException("X11 connection used wrong authentication data.");
        } else {
            for (int i = 0; i < len; i++) {
                byte data = (byte) Integer.parseInt(fakeAuthData.substring(i * 2, i * 2 + 2), 16);
                if (data != dataSoFar[i + (12 + ((plen + 3) & ~3))]) throw new IOException("X11 connection used wrong authentication data.");
                newdata[i] = (byte) Integer.parseInt(realAuthData.substring(i * 2, i * 2 + 2), 16);
            }
        }
        System.arraycopy(newdata, 0, dataSoFar, 12 + ((plen + 3) & ~3), dlen);
        return true;
    }

    /**
   * Redefine onChannelData... for first packet check that there is the correct fake authentication data and then
   * substitute the real data.
   *
   */
    protected void onChannelData(SshMsgChannelData msg) throws IOException {
        if (firstPacket) {
            if (dataSoFar == null) {
                dataSoFar = msg.getChannelData();
            } else {
                byte newData[] = msg.getChannelData();
                byte data[] = new byte[dataSoFar.length + newData.length];
                System.arraycopy(dataSoFar, 0, data, 0, dataSoFar.length);
                System.arraycopy(newData, 0, data, dataSoFar.length, newData.length);
                dataSoFar = data;
            }
            if (allDataCheckSubst()) {
                firstPacket = false;
                socket.getOutputStream().write(dataSoFar);
            }
        } else {
            try {
                socket.getOutputStream().write(msg.getChannelData());
            } catch (IOException ex) {
            }
        }
    }
}
