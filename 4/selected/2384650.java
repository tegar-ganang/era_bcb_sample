package tuwien.auto.calimero.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Basic API for communication with a logical serial port connection.
 * <p>
 * The implementation of this API contains platform dependent code. It is used as a
 * fallback to enable serial communication on a RS-232 port in case default access
 * mechanism are not available or there is no protocol support.
 * 
 * @author B. Malinowsky
 */
class SerialCom extends LibraryAdapter {

    static final int BAUDRATE = 1;

    static final int PARITY = 2;

    static final int DATABITS = 3;

    static final int STOPBITS = 4;

    static final int FLOWCTRL = 5;

    static final int FLOWCTRL_NONE = 0;

    static final int FLOWCTRL_CTSRTS = 1;

    static final int PARITY_NONE = 0;

    static final int PARITY_ODD = 1;

    static final int PARITY_EVEN = 2;

    static final int PARITY_MARK = 3;

    static final int ONE_STOPBIT = 1;

    static final int TWO_STOPBITS = 2;

    static final int AVAILABLE_INPUT_STATUS = 2;

    private static final int ERROR_STATUS = 1;

    private static final int LINE_STATUS = 3;

    private static final int EVENT_RXCHAR = 0x0001;

    private static final int EVENT_RXFLAG = 0x0002;

    private static final int EVENT_TXEMPTY = 0x0004;

    private static final int EVENT_CTS = 0x0008;

    private static final int EVENT_DSR = 0x0010;

    private static final int EVENT_RLSD = 0x0020;

    private static final int EVENT_BREAK = 0x0040;

    private static final int EVENT_ERR = 0x0080;

    private static final int EVENT_RING = 0x0100;

    private static final boolean loaded;

    private static final int INVALID_HANDLE = -1;

    private long fd = INVALID_HANDLE;

    static final class Timeouts {

        final int readInterval;

        final int readTotalMultiplier;

        final int readTotalConstant;

        final int writeTotalMultiplier;

        final int writeTotalConstant;

        Timeouts(int readInterval, int readTotalMultiplier, int readTotalConstant, int writeTotalMultiplier, int writeTotalConstant) {
            this.readInterval = readInterval;
            this.readTotalMultiplier = readTotalMultiplier;
            this.readTotalConstant = readTotalConstant;
            this.writeTotalMultiplier = writeTotalMultiplier;
            this.writeTotalConstant = writeTotalConstant;
        }

        public String toString() {
            return "read " + readInterval + " read total " + readTotalMultiplier + " constant " + readTotalConstant + " write total " + writeTotalMultiplier + " write constant " + writeTotalConstant;
        }
    }

    static {
        boolean b = false;
        try {
            System.loadLibrary("serialcom");
            b = true;
        } catch (final SecurityException e) {
        } catch (final UnsatisfiedLinkError e) {
        }
        loaded = b;
    }

    SerialCom(String portID) throws IOException {
        if (portID == null) throw new NullPointerException("port ID");
        if (!loaded) throw new IOException("no serial I/O communication support");
        open(portID);
    }

    static boolean isLoaded() {
        return loaded;
    }

    static native boolean portExists(String portID);

    native int writeBytes(byte[] b, int off, int len) throws IOException;

    native int write(int b) throws IOException;

    native int readBytes(byte[] b, int off, int len) throws IOException;

    native int read() throws IOException;

    native void setTimeouts(Timeouts times) throws IOException;

    native Timeouts getTimeouts() throws IOException;

    public final void setBaudRate(int baudrate) {
        try {
            setControl(BAUDRATE, baudrate);
        } catch (final IOException e) {
        }
    }

    public final int getBaudRate() {
        try {
            return getControl(BAUDRATE);
        } catch (final IOException e) {
        }
        return 0;
    }

    final int setParity(int parity) {
        try {
            return setControl(PARITY, parity);
        } catch (final IOException e) {
        }
        return 0;
    }

    final int getParity() {
        try {
            return getControl(PARITY);
        } catch (final IOException e) {
        }
        return 0;
    }

    native int getStatus(int type);

    native int setControl(int control, int newValue) throws IOException;

    native int getControl(int control) throws IOException;

    public InputStream getInputStream() {
        if (fd == INVALID_HANDLE) return null;
        return new PortInputStream(this);
    }

    public OutputStream getOutputStream() {
        if (fd == INVALID_HANDLE) return null;
        return new PortOutputStream(this);
    }

    DataInputStream openDataInputStream() {
        final InputStream is = getInputStream();
        if (is instanceof DataInputStream) {
            return (DataInputStream) is;
        }
        return new DataInputStream(is);
    }

    DataOutputStream openDataOutputStream() {
        final OutputStream os = getOutputStream();
        if (os instanceof DataOutputStream) {
            return (DataOutputStream) os;
        }
        return new DataOutputStream(os);
    }

    public final void close() throws IOException {
        if (fd != INVALID_HANDLE) close0();
        fd = INVALID_HANDLE;
    }

    private native void setEvents(int eventMask, boolean enable) throws IOException;

    private native int waitEvent() throws IOException;

    private native void open(String portID) throws IOException;

    private native void close0() throws IOException;
}
