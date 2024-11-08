package org.zoolib.blackberry;

import net.rim.device.api.system.Application;
import net.rim.device.api.system.USBPort;
import net.rim.device.api.system.USBPortListener;
import java.io.IOException;
import org.zoolib.ZDebug;

public class IOPort_USB extends IOPort implements USBPortListener {

    public IOPort_USB(String iName) throws IOException {
        this(iName, 0x4000, 0x4000);
    }

    public IOPort_USB(String iName, int iMaxReceiveSize, int iMaxSendSize) throws IOException {
        super(Math.min(USBPort.getMaximumRxSize(), iMaxReceiveSize), Math.min(USBPort.getMaximumTxSize(), iMaxSendSize));
        ZDebug.sAssert(iName.length() <= 16);
        fChannel = USBPort.registerChannel(iName, fMaxReceiveSize, fMaxSendSize);
        Application.getApplication().addIOPortListener(this);
    }

    public void close() {
        super.close();
        Application.getApplication().removeIOPortListener(this);
        if (fChannel != 0) {
            try {
                USBPort.deregisterChannel(fChannel);
            } catch (Exception ex) {
            }
        }
    }

    protected void impDisconnect() {
        if (fUSBPort != null) {
            try {
                fUSBPort.close();
            } catch (Exception ex) {
            }
            fUSBPort = null;
        }
    }

    protected int impWrite(byte b[], int off, int len) throws IOException {
        int countWritten = fUSBPort.write(b, off, len);
        this.wrote(countWritten);
        return countWritten;
    }

    protected int impRead(byte b[], int off, int len) throws IOException {
        return fUSBPort.read(b, off, len);
    }

    public void connected() {
        super.pConnected();
    }

    public void disconnected() {
        super.pDisconnected();
    }

    public void receiveError(int error) {
        super.pReceiveError(error);
    }

    public void dataReceived(int length) {
        this.pDataReceived(length);
    }

    public void dataSent() {
        this.pDataSent();
    }

    public void patternReceived(byte[] pattern) {
    }

    public int getChannel() {
        return fChannel;
    }

    public void dataNotSent() {
        this.pDataNotSent();
    }

    public void connectionRequested() {
        try {
            fUSBPort = new USBPort(fChannel);
        } catch (Throwable e) {
        }
    }

    private final int fChannel;

    private USBPort fUSBPort;
}
