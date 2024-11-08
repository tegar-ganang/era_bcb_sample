package com.sshtools.j2ssh.transport;

import com.sshtools.j2ssh.io.ByteArrayReader;
import com.sshtools.j2ssh.io.ByteArrayWriter;
import com.sshtools.j2ssh.transport.cipher.SshCipher;
import com.sshtools.j2ssh.transport.compression.SshCompression;
import com.sshtools.j2ssh.transport.hmac.SshHmac;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.math.BigInteger;
import java.net.SocketException;
import java.util.Iterator;

class TransportProtocolInputStream {

    private static Log log = LogFactory.getLog(TransportProtocolInputStream.class);

    private long bytesTransfered = 0;

    private BufferedInputStream in;

    private Object sequenceLock = new Object();

    private TransportProtocolCommon transport;

    private TransportProtocolAlgorithmSync algorithms;

    private long sequenceNo = 0;

    private long sequenceWrapLimit = BigInteger.valueOf(2).pow(32).longValue();

    private SshCipher cipher;

    private SshHmac hmac;

    private SshCompression compression;

    int msglen;

    int padlen;

    int read;

    int remaining;

    int cipherlen = 8;

    int maclen = 0;

    ByteArrayWriter message = new ByteArrayWriter();

    byte[] initial = new byte[cipherlen];

    byte[] data = new byte[65535];

    byte[] buffered = new byte[65535];

    int startpos = 0;

    int endpos = 0;

    /**
     * Creates a new TransportProtocolInputStream object.
     *
     * @param transport
     * @param in
     * @param algorithms
     *
     * @throws IOException
     */
    public TransportProtocolInputStream(TransportProtocolCommon transport, InputStream in, TransportProtocolAlgorithmSync algorithms) throws IOException {
        this.transport = transport;
        this.in = new BufferedInputStream(in);
        this.algorithms = algorithms;
    }

    /**
     *
     *
     * @return
     */
    public synchronized long getSequenceNo() {
        return sequenceNo;
    }

    /**
     *
     *
     * @return
     */
    protected long getNumBytesTransfered() {
        return bytesTransfered;
    }

    /**
     *
     *
     * @return
     */
    protected int available() {
        return endpos - startpos;
    }

    /**
     *
     *
     * @param buf
     * @param off
     * @param len
     *
     * @return
     *
     * @throws IOException
     */
    protected int readBufferedData(byte[] buf, int off, int len) throws IOException {
        int read;
        if ((endpos - startpos) < len) {
            if ((buffered.length - endpos) < len) {
                System.arraycopy(buffered, startpos, buffered, 0, endpos - startpos);
                endpos -= startpos;
                startpos = 0;
                if ((buffered.length - endpos) < len) {
                    byte[] tmp = new byte[buffered.length + len];
                    System.arraycopy(buffered, 0, tmp, 0, endpos);
                    buffered = tmp;
                }
            }
            while (((endpos - startpos) < len) && (transport.getState().getValue() != TransportProtocolState.DISCONNECTED)) {
                try {
                    read = in.read(buffered, endpos, (buffered.length - endpos));
                } catch (InterruptedIOException ex) {
                    read = ex.bytesTransferred;
                    Iterator it = transport.getEventHandlers().iterator();
                    TransportProtocolEventHandler eventHandler;
                    while (it.hasNext()) {
                        eventHandler = (TransportProtocolEventHandler) it.next();
                        eventHandler.onSocketTimeout(transport);
                    }
                }
                if (read < 0) {
                    throw new IOException("The socket is EOF");
                }
                endpos += read;
            }
        }
        try {
            System.arraycopy(buffered, startpos, buf, off, len);
        } catch (Throwable t) {
            System.out.println();
        }
        startpos += len;
        if (startpos >= endpos) {
            endpos = 0;
            startpos = 0;
        }
        return len;
    }

    /**
     *
     *
     * @return
     *
     * @throws SocketException
     * @throws IOException
     */
    public byte[] readMessage() throws SocketException, IOException {
        message.reset();
        read = readBufferedData(initial, 0, cipherlen);
        cipher = algorithms.getCipher();
        hmac = algorithms.getHmac();
        compression = algorithms.getCompression();
        if (cipher != null) {
            cipherlen = cipher.getBlockSize();
        } else {
            cipherlen = 8;
        }
        if (initial.length != cipherlen) {
            byte[] tmp = new byte[cipherlen];
            System.arraycopy(initial, 0, tmp, 0, initial.length);
            initial = tmp;
        }
        int count = read;
        if (count < initial.length) {
            count += readBufferedData(initial, count, initial.length - count);
        }
        if (hmac != null) {
            maclen = hmac.getMacLength();
        } else {
            maclen = 0;
        }
        if (cipher != null) {
            initial = cipher.transform(initial);
        }
        message.write(initial);
        msglen = (int) ByteArrayReader.readInt(initial, 0);
        padlen = initial[4];
        remaining = (msglen - (cipherlen - 4));
        while (remaining > 0) {
            read = readBufferedData(data, 0, (remaining < data.length) ? ((remaining / cipherlen) * cipherlen) : ((data.length / cipherlen) * cipherlen));
            remaining -= read;
            message.write((cipher == null) ? data : cipher.transform(data, 0, read), 0, read);
        }
        synchronized (sequenceLock) {
            if (hmac != null) {
                read = readBufferedData(data, 0, maclen);
                message.write(data, 0, read);
                if (!hmac.verify(sequenceNo, message.toByteArray())) {
                    throw new IOException("Corrupt Mac on input");
                }
            }
            if (sequenceNo < sequenceWrapLimit) {
                sequenceNo++;
            } else {
                sequenceNo = 0;
            }
        }
        bytesTransfered += message.size();
        byte[] msg = message.toByteArray();
        if (compression != null) {
            return compression.uncompress(msg, 5, (msglen + 4) - padlen - 5);
        }
        return msg;
    }
}
