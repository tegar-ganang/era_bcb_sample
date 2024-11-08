package ti.comm.win32;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;
import javax.comm.SerialPortEventListener;
import javax.comm.UnsupportedCommOperationException;

public class Win32SerialPort extends javax.comm.SerialPort {

    int handle;

    private Win32SerialInputStream in;

    private Win32SerialOutputStream out;

    Win32SerialPort(String portName) {
        in = new Win32SerialInputStream(this);
        out = new Win32SerialOutputStream(this);
        handle = NativeApi.openPort("\\\\.\\" + portName, in.readComplete, out.writeComplete);
    }

    void checkValidHandle() throws IOException {
        if (handle < 0) throw new EOFException("invalid handle");
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        checkValidHandle();
        return out;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        checkValidHandle();
        return in;
    }

    @Override
    public void close() {
        super.close();
        System.err.println("CLOSE PORT: " + handle);
        NativeApi.closePort(handle);
        handle = -1;
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setSerialPortParams(int baudRate, int dataBits, int stopBits, int parity) throws UnsupportedCommOperationException {
        NativeApi.setSerialPortParams(handle, baudRate, dataBits, stopBits, parity);
    }

    @Override
    public void setFlowControlMode(int flowcontrol) throws UnsupportedCommOperationException {
        NativeApi.setFlowControlMode(handle, flowcontrol);
    }

    @Override
    public void addEventListener(SerialPortEventListener arg0) throws TooManyListenersException {
    }

    @Override
    public int getBaudRate() {
        return 0;
    }

    @Override
    public int getDataBits() {
        return 0;
    }

    @Override
    public int getFlowControlMode() {
        return 0;
    }

    @Override
    public int getParity() {
        return 0;
    }

    @Override
    public int getStopBits() {
        return 0;
    }

    @Override
    public boolean isCD() {
        return false;
    }

    @Override
    public boolean isCTS() {
        return false;
    }

    @Override
    public boolean isDSR() {
        return false;
    }

    @Override
    public boolean isDTR() {
        return false;
    }

    @Override
    public boolean isRI() {
        return false;
    }

    @Override
    public boolean isRTS() {
        return false;
    }

    @Override
    public void notifyOnBreakInterrupt(boolean arg0) {
    }

    @Override
    public void notifyOnCTS(boolean arg0) {
    }

    @Override
    public void notifyOnCarrierDetect(boolean arg0) {
    }

    @Override
    public void notifyOnDSR(boolean arg0) {
    }

    @Override
    public void notifyOnDataAvailable(boolean arg0) {
    }

    @Override
    public void notifyOnFramingError(boolean arg0) {
    }

    @Override
    public void notifyOnOutputEmpty(boolean arg0) {
    }

    @Override
    public void notifyOnOverrunError(boolean arg0) {
    }

    @Override
    public void notifyOnParityError(boolean arg0) {
    }

    @Override
    public void notifyOnRingIndicator(boolean arg0) {
    }

    @Override
    public void removeEventListener() {
    }

    @Override
    public void sendBreak(int arg0) {
    }

    @Override
    public void setDTR(boolean arg0) {
    }

    @Override
    public void setRTS(boolean arg0) {
    }

    @Override
    public void disableReceiveFraming() {
    }

    @Override
    public void disableReceiveThreshold() {
    }

    @Override
    public void disableReceiveTimeout() {
    }

    @Override
    public void enableReceiveFraming(int arg0) throws UnsupportedCommOperationException {
    }

    @Override
    public void enableReceiveThreshold(int arg0) throws UnsupportedCommOperationException {
    }

    @Override
    public void enableReceiveTimeout(int arg0) throws UnsupportedCommOperationException {
    }

    @Override
    public int getInputBufferSize() {
        return 0;
    }

    @Override
    public int getOutputBufferSize() {
        return 0;
    }

    @Override
    public int getReceiveFramingByte() {
        return 0;
    }

    @Override
    public int getReceiveThreshold() {
        return 0;
    }

    @Override
    public int getReceiveTimeout() {
        return 0;
    }

    @Override
    public boolean isReceiveFramingEnabled() {
        return false;
    }

    @Override
    public boolean isReceiveThresholdEnabled() {
        return false;
    }

    @Override
    public boolean isReceiveTimeoutEnabled() {
        return false;
    }

    @Override
    public void setInputBufferSize(int arg0) {
    }

    @Override
    public void setOutputBufferSize(int arg0) {
    }
}
