package mindbright.ssh;

import java.io.*;
import java.math.BigInteger;
import mindbright.security.*;
import mindbright.util.*;

public final class SSHPduOutputStream extends SSHDataOutputStream implements SSHPdu {

    static final class PduByteArrayOutputStream extends ByteArrayOutputStream {

        PduByteArrayOutputStream() {
            super();
        }

        PduByteArrayOutputStream(int size) {
            super(size);
        }

        PduByteArrayOutputStream(byte[] buf) {
            this.buf = buf;
        }

        public byte[] getBuf() {
            return buf;
        }

        public int getCount() {
            return count;
        }

        public void setBuf(byte[] buf) {
            this.buf = buf;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    public static final int SSH_DEFAULT_PKT_LEN = 8192;

    public static int mtu = SSH_DEFAULT_PKT_LEN;

    public static synchronized void setMTU(int newMtu) {
        mtu = newMtu;
    }

    byte[] readFromRawData;

    int readFromOff;

    int readFromSize;

    public int type;

    public Cipher cipher;

    SSHPduOutputStream(Cipher cipher) {
        super(null);
        this.cipher = cipher;
    }

    SSHPduOutputStream(int type, Cipher cipher) throws IOException {
        super(new PduByteArrayOutputStream(mtu));
        this.type = type;
        this.cipher = cipher;
        if (cipher != null) {
            PduByteArrayOutputStream bytes = (PduByteArrayOutputStream) out;
            SecureRandom rand = SSH.secureRandom();
            rand.nextPadBytes(bytes.getBuf(), 8);
            bytes.setCount(8);
        } else {
            for (int i = 0; i < 8; i++) write(0);
        }
        write(type);
    }

    public SSHPdu createPdu() throws IOException {
        SSHPdu pdu;
        pdu = new SSHPduOutputStream(this.type, this.cipher);
        return pdu;
    }

    public void readFrom(InputStream in) throws IOException {
        if (type != SSH.MSG_CHANNEL_DATA && type != SSH.CMSG_STDIN_DATA) throw new IOException("Trying to read raw data into non-data PDU");
        PduByteArrayOutputStream bytes = (PduByteArrayOutputStream) out;
        readFromRawData = bytes.getBuf();
        readFromOff = bytes.size() + 4;
        readFromSize = in.read(readFromRawData, readFromOff, mtu - readFromOff);
        if (readFromSize == -1) throw new IOException("EOF");
        writeInt(readFromSize);
        bytes.setCount(readFromOff + readFromSize);
    }

    public void writeTo(OutputStream sshOut) throws IOException {
        PduByteArrayOutputStream bytes = (PduByteArrayOutputStream) out;
        byte[] padData;
        int iSz;
        int pad;
        int crc32;
        int padSz;
        int off = 0;
        iSz = bytes.size();
        pad = (iSz + 4) % 8;
        crc32 = (int) CRC32.getValue(bytes.getBuf(), pad, iSz - pad);
        padSz = iSz + 4 - pad;
        writeInt(crc32);
        padData = bytes.getBuf();
        if (cipher != null) {
            cipher.encrypt(padData, pad, padData, pad, padSz);
        }
        int sz = padSz - (8 - pad);
        sshOut.write((sz >>> 24) & 0xff);
        sshOut.write((sz >>> 16) & 0xff);
        sshOut.write((sz >>> 8) & 0xff);
        sshOut.write((sz >>> 0) & 0xff);
        sshOut.write(padData, pad, padSz);
        sshOut.flush();
    }

    public byte[] rawData() {
        return readFromRawData;
    }

    public void rawSetData(byte[] raw) {
    }

    public int rawOffset() {
        return readFromOff;
    }

    public int rawSize() {
        byte[] bytes = readFromRawData;
        int off = readFromOff - 4;
        int ch1 = ((bytes[off++] + 256) & 0xff);
        int ch2 = ((bytes[off++] + 256) & 0xff);
        int ch3 = ((bytes[off++] + 256) & 0xff);
        int ch4 = ((bytes[off] + 256) & 0xff);
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    public void rawAdjustSize(int size) {
        PduByteArrayOutputStream bytes = (PduByteArrayOutputStream) out;
        bytes.setCount(size);
    }
}
