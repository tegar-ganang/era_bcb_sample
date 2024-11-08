package org.zoolib.blackberry;

import net.rim.device.api.system.Application;
import net.rim.device.api.system.USBPort;
import net.rim.device.api.system.USBPortListener;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.zoolib.stream.ZFIFOByte;
import org.zoolib.thread.ZCondition;
import org.zoolib.thread.ZMutex;

public class ZUSBChannel implements USBPortListener {

    public ZUSBChannel(String iName) throws IOException {
        fMaxReceiveSize = 1024;
        fMaxSendSize = 1024;
        fChannel = USBPort.registerChannel(iName, fMaxReceiveSize, fMaxSendSize);
        Application.getApplication().addIOPortListener(this);
    }

    public void close() {
        this.pClose();
        Application.getApplication().removeIOPortListener(this);
        if (fChannel != 0) {
            try {
                USBPort.deregisterChannel(fChannel);
            } catch (Exception ex) {
            }
        }
    }

    public InputStream getInputStream() {
        return fStreamI;
    }

    public OutputStream getOutputStream() {
        return fStreamO;
    }

    public void wrote(int iCount) {
    }

    public void connected() {
        fMutex.acquire();
        fOpen = true;
        fCondition_Read.broadcast();
        fCondition_Write.broadcast();
        fMutex.release();
    }

    public void disconnected() {
        this.pClose();
    }

    public void receiveError(int error) {
    }

    public void dataReceived(int length) {
        if (length == -1) length = fMaxReceiveSize;
        byte[] buffer = new byte[fMaxReceiveSize];
        try {
            int countRead = fUSBPort.read(buffer, 0, length);
            fMutex.acquire();
            fFIFO.pushBack(buffer, 0, countRead);
            fCondition_Read.broadcast();
            fMutex.release();
        } catch (Throwable ex) {
        }
    }

    public void dataSent() {
        fMutex.acquire();
        fWriteBusy = false;
        fCondition_Write.broadcast();
        fMutex.release();
    }

    public void patternReceived(byte[] pattern) {
    }

    public int getChannel() {
        return fChannel;
    }

    public void dataNotSent() {
        fMutex.acquire();
        fWriteBusy = false;
        fCondition_Write.broadcast();
        fMutex.release();
    }

    public void connectionRequested() {
        try {
            fUSBPort = new USBPort(fChannel);
        } catch (Throwable e) {
        }
    }

    private final void pClose() {
        fMutex.acquire();
        fOpen = false;
        fFIFO.clear();
        fCondition_Read.broadcast();
        fCondition_Write.broadcast();
        if (fUSBPort != null) fUSBPort.close();
        fUSBPort = null;
        fWriteBusy = false;
        fMutex.release();
    }

    private final int fChannel;

    private final int fMaxReceiveSize;

    private final int fMaxSendSize;

    private USBPort fUSBPort;

    private final ZMutex fMutex = new ZMutex();

    private final ZCondition fCondition_Read = new ZCondition();

    private final ZCondition fCondition_Write = new ZCondition();

    private boolean fWriteBusy = false;

    private boolean fOpen = false;

    private final ZFIFOByte fFIFO = new ZFIFOByte();

    private final StreamI fStreamI = new StreamI();

    private final StreamO fStreamO = new StreamO();

    private final class StreamI extends InputStream {

        StreamI() {
        }

        public final int read() throws IOException {
            byte[] theBuf = new byte[1];
            if (-1 == this.read(theBuf, 0, 1)) return -1;
            int result = ((int) theBuf[0]) & 0xFF;
            if (result == -1) return -1;
            return result;
        }

        public final int read(byte b[], int off, int len) throws IOException {
            fMutex.acquire();
            try {
                while (len > 0) {
                    if (!fOpen) break;
                    int countToCopy = Math.min(len, fFIFO.length());
                    if (countToCopy > 0) {
                        fFIFO.popFront(b, off, countToCopy);
                        return countToCopy;
                    } else {
                        fCondition_Read.wait(fMutex);
                    }
                }
            } finally {
                fMutex.release();
            }
            return -1;
        }

        public final int available() throws IOException {
            fMutex.acquire();
            try {
                if (!fOpen) return 0;
                return fFIFO.length();
            } finally {
                fMutex.release();
            }
        }

        public final void close() throws IOException {
        }
    }

    public final class StreamO extends OutputStream {

        StreamO() {
        }

        public final void write(int b) throws IOException {
            byte[] theBuf = new byte[1];
            theBuf[0] = (byte) b;
            this.write(theBuf, 0, 1);
        }

        public void write(byte b[], int off, int len) throws IOException {
            fMutex.acquire();
            try {
                while (len > 0) {
                    if (!fOpen) throw new IOException("ZUSBChannel.StreamO, wrote to closed stream");
                    while (fWriteBusy) {
                        fCondition_Write.wait(fMutex);
                        continue;
                    }
                    USBPort theUSBPort = fUSBPort;
                    if (theUSBPort != null) {
                        fWriteBusy = true;
                        fMutex.release();
                        try {
                            int countWritten = theUSBPort.write(b, off, Math.min(len, fMaxSendSize));
                            off += countWritten;
                            len -= countWritten;
                            wrote(countWritten);
                        } finally {
                            fMutex.acquire();
                        }
                    }
                }
            } finally {
                fMutex.release();
            }
        }

        public final void close() throws IOException {
        }
    }
}
