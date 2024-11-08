package bwmorg.bouncycastle.crypto.tls;

import java.io.*;
import bwmorg.LOG;

/**
 * An implementation of the TLS 1.0 record layer.
 */
public class RecordStream {

    private TlsProtocolHandler handler;

    private InputStream is;

    private OutputStream os;

    protected CombinedHash hash1;

    protected CombinedHash hash2;

    protected TlsCipherSuite readSuite = null;

    protected TlsCipherSuite writeSuite = null;

    protected RecordStream(TlsProtocolHandler handler, InputStream is, OutputStream os) {
        this.handler = handler;
        this.is = is;
        this.os = os;
        hash1 = new CombinedHash();
        hash2 = new CombinedHash();
        this.readSuite = new TlsNullCipherSuite();
        this.writeSuite = this.readSuite;
    }

    /**
     * BlueWhaleSystems fix: Tatiana Rybak - 08 Aug 2007
     */
    private boolean isValidRecordType(int type) {
        return (type == TlsProtocolHandler.RL_CHANGE_CIPHER_SPEC || type == TlsProtocolHandler.RL_ALERT || type == TlsProtocolHandler.RL_APPLICATION_DATA || type == TlsProtocolHandler.RL_HANDSHAKE);
    }

    /**
     * BlueWhaleSystems fix: Tatiana Rybak - 08 Aug 2007
     *
     * This is a HACK to have TLS working with Microsoft Exchange using DES-CBC3-SHA cipher.
     * The cipher is broken on the exchange side and sends garbage data at the end of valid data.
     * Try to skip this junk bytes and find valid data that does appear sometimes. 
     * 
     * This implementation has major 2 issues that need to be fixed:
     * 
     * 1. It heavily relies on available(), therefore any delay in the network might cause this code to break.
     * Ideally, we would use some buffering or wait for data before bailing.
     * 
     * 2. While scanning for valid data, only the first 3 bytes are checked for validity. Once it finds what
     * looks like a valid record and valid version (3 bytes), it reads the size and provided it is positive the code
     * will then try to read the data in. I.e. no error checking is done once the first 5 bytes look OK.
     * It is possible that the garbage data will have the first 3-5 bytes that <look> like a valid record, but in 
     * reality is just garbage. This code does not currently recover from this failure.
     * 
     * The code recoveres the scanning if the failure occures within the first 3 bytes or 5 if byte 4 or 5 are negative.
     * 
     * Ideally, once the data size has been read in we would compare that the two byte in the data size do not look like
     *  a start of a valid record. If it doesn't then we try to read the data and if it fails we restart from byte 6. 
     * (Read data needs to be buffered, since we want to rescan it. Here we could possibly run into a problem of reading
     * too much data, if this is actaully not a valid record)
     *  If either one of the two bytes for the size look like it could be a start of the valid record. Here we can try 
     *  seeing if the next two bytes after a potential valid record type are valid version. If there are not - read the 
     *  size of the record that we found originally. If the next 2 bytes do look like a version.. UGH! take the smallest 
     *  size of the two datas and read and try to decode that first. If that fails, try the second one. Of course, both can fail. 
     *  Also, we have to make sure that the data is actually available when we try to read it. 
     */
    private boolean scanForValidData() throws IOException, UnknownDataException {
        boolean skipReadingRecordtype = false;
        short type;
        int i = 0;
        while (is.available() > 0) {
            if (!skipReadingRecordtype) {
                type = TlsUtils.readUint8(is);
            } else {
                type = (short) i;
            }
            if (!isValidRecordType(type)) {
                continue;
            }
            if (is.available() > 0) {
                i = is.read();
                if (i != 3) {
                    skipReadingRecordtype = isValidRecordType(i);
                    continue;
                }
            } else {
                return false;
            }
            if (is.available() > 0) {
                i = is.read();
                if (i != 1) {
                    skipReadingRecordtype = isValidRecordType(i);
                    continue;
                }
            } else {
                return false;
            }
            try {
                int size = 0;
                if (is.available() > 0) {
                    i = is.read();
                    if (i < 0) {
                        continue;
                    }
                    size = i << 8;
                }
                if (is.available() > 0) {
                    i = is.read();
                    if (i < 0) {
                        continue;
                    }
                    size = size | i;
                }
                byte[] buf = decodeAndVerify(type, is, size);
                handler.processData(type, buf, 0, buf.length);
                return true;
            } catch (IOException e) {
                continue;
            }
        }
        return false;
    }

    public void readData() throws IOException, UnknownDataException {
        short type = TlsUtils.readUint8(is);
        if (!isValidRecordType(type)) {
            try {
                if (scanForValidData()) {
                    return;
                }
            } catch (Exception e) {
            }
            throw new UnknownDataException();
        }
        TlsUtils.checkVersion(is, handler);
        int size = TlsUtils.readUint16(is);
        LOG.trace("Tls RecordStream: size: " + size + ", type: " + type);
        byte[] buf = decodeAndVerify(type, is, size);
        handler.processData(type, buf, 0, buf.length);
    }

    protected byte[] decodeAndVerify(short type, InputStream is, int len) throws IOException {
        byte[] buf = new byte[len];
        TlsUtils.readFully(buf, is);
        byte[] result = readSuite.decodeCiphertext(type, buf, 0, buf.length, handler);
        return result;
    }

    protected void writeMessage(short type, byte[] message, int offset, int len) throws IOException {
        if (type == 22) {
            hash1.update(message, offset, len);
            hash2.update(message, offset, len);
        }
        byte[] ciphertext = writeSuite.encodePlaintext(type, message, offset, len);
        byte[] writeMessage = new byte[ciphertext.length + 5];
        TlsUtils.writeUint8(type, writeMessage, 0);
        TlsUtils.writeUint8((short) 3, writeMessage, 1);
        TlsUtils.writeUint8((short) 1, writeMessage, 2);
        TlsUtils.writeUint16(ciphertext.length, writeMessage, 3);
        System.arraycopy(ciphertext, 0, writeMessage, 5, ciphertext.length);
        os.write(writeMessage);
        os.flush();
    }

    protected void close() throws IOException {
        IOException e = null;
        try {
            is.close();
        } catch (IOException ex) {
            e = ex;
        } finally {
            is = null;
        }
        try {
            os.close();
        } catch (IOException ex) {
            e = ex;
        } finally {
            os = null;
        }
        if (e != null) {
            throw e;
        }
    }

    protected void flush() throws IOException {
        os.flush();
    }

    /**
     * BlueWhaleSystems fix: Tatiana Rybak - 02 Mar 2007
     * 
     * Added a method to return available bytes in the data stream.
     */
    protected int available() throws IOException {
        return is.available();
    }
}

/**
 * BlueWhaleSystems fix: Tatiana Rybak - 08 Aug 2007
 */
class UnknownDataException extends Exception {
}
