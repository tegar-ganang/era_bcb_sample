package org.bouncycastle.crypto.tls;

import org.bouncycastle.asn1.x509.RSAPublicKeyStructure;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSABlindedEngine;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.prng.ThreadedSeedGenerator;
import org.bouncycastle.util.BigIntegers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * An implementation of all high level protocols in TLS 1.0.
 */
public class TlsProtocolHandler {

    private static final short RL_CHANGE_CIPHER_SPEC = 20;

    private static final short RL_ALERT = 21;

    private static final short RL_HANDSHAKE = 22;

    private static final short RL_APPLICATION_DATA = 23;

    private static final short HP_HELLO_REQUEST = 0;

    private static final short HP_CLIENT_HELLO = 1;

    private static final short HP_SERVER_HELLO = 2;

    private static final short HP_CERTIFICATE = 11;

    private static final short HP_SERVER_KEY_EXCHANGE = 12;

    private static final short HP_CERTIFICATE_REQUEST = 13;

    private static final short HP_SERVER_HELLO_DONE = 14;

    private static final short HP_CERTIFICATE_VERIFY = 15;

    private static final short HP_CLIENT_KEY_EXCHANGE = 16;

    private static final short HP_FINISHED = 20;

    private static final short CS_CLIENT_HELLO_SEND = 1;

    private static final short CS_SERVER_HELLO_RECEIVED = 2;

    private static final short CS_SERVER_CERTIFICATE_RECEIVED = 3;

    private static final short CS_SERVER_KEY_EXCHANGE_RECEIVED = 4;

    private static final short CS_CERTIFICATE_REQUEST_RECEIVED = 5;

    private static final short CS_SERVER_HELLO_DONE_RECEIVED = 6;

    private static final short CS_CLIENT_KEY_EXCHANGE_SEND = 7;

    private static final short CS_CLIENT_CHANGE_CIPHER_SPEC_SEND = 8;

    private static final short CS_CLIENT_FINISHED_SEND = 9;

    private static final short CS_SERVER_CHANGE_CIPHER_SPEC_RECEIVED = 10;

    private static final short CS_DONE = 11;

    protected static final short AP_close_notify = 0;

    protected static final short AP_unexpected_message = 10;

    protected static final short AP_bad_record_mac = 20;

    protected static final short AP_decryption_failed = 21;

    protected static final short AP_record_overflow = 22;

    protected static final short AP_decompression_failure = 30;

    protected static final short AP_handshake_failure = 40;

    protected static final short AP_bad_certificate = 42;

    protected static final short AP_unsupported_certificate = 43;

    protected static final short AP_certificate_revoked = 44;

    protected static final short AP_certificate_expired = 45;

    protected static final short AP_certificate_unknown = 46;

    protected static final short AP_illegal_parameter = 47;

    protected static final short AP_unknown_ca = 48;

    protected static final short AP_access_denied = 49;

    protected static final short AP_decode_error = 50;

    protected static final short AP_decrypt_error = 51;

    protected static final short AP_export_restriction = 60;

    protected static final short AP_protocol_version = 70;

    protected static final short AP_insufficient_security = 71;

    protected static final short AP_internal_error = 80;

    protected static final short AP_user_canceled = 90;

    protected static final short AP_no_renegotiation = 100;

    protected static final short AL_warning = 1;

    protected static final short AL_fatal = 2;

    private static final byte[] emptybuf = new byte[0];

    private static final String TLS_ERROR_MESSAGE = "Internal TLS error, this could be an attack";

    private ByteQueue applicationDataQueue = new ByteQueue();

    private ByteQueue changeCipherSpecQueue = new ByteQueue();

    private ByteQueue alertQueue = new ByteQueue();

    private ByteQueue handshakeQueue = new ByteQueue();

    private RecordStream rs;

    private SecureRandom random;

    private RSAKeyParameters serverRsaKey = null;

    private TlsInputStream tlsInputStream = null;

    private TlsOuputStream tlsOutputStream = null;

    private boolean closed = false;

    private boolean failedWithError = false;

    private boolean appDataReady = false;

    private byte[] clientRandom;

    private byte[] serverRandom;

    private byte[] ms;

    private TlsCipherSuite chosenCipherSuite = null;

    private BigInteger Yc;

    private byte[] pms;

    private CertificateVerifyer verifyer = null;

    public TlsProtocolHandler(InputStream is, OutputStream os) {
        ThreadedSeedGenerator tsg = new ThreadedSeedGenerator();
        this.random = new SecureRandom();
        this.random.setSeed(tsg.generateSeed(20, true));
        this.rs = new RecordStream(this, is, os);
    }

    public TlsProtocolHandler(InputStream is, OutputStream os, SecureRandom sr) {
        this.random = sr;
        this.rs = new RecordStream(this, is, os);
    }

    private short connection_state;

    protected void processData(short protocol, byte[] buf, int offset, int len) throws IOException {
        switch(protocol) {
            case RL_CHANGE_CIPHER_SPEC:
                changeCipherSpecQueue.addData(buf, offset, len);
                processChangeCipherSpec();
                break;
            case RL_ALERT:
                alertQueue.addData(buf, offset, len);
                processAlert();
                break;
            case RL_HANDSHAKE:
                handshakeQueue.addData(buf, offset, len);
                processHandshake();
                break;
            case RL_APPLICATION_DATA:
                if (!appDataReady) {
                    this.failWithError(AL_fatal, AP_unexpected_message);
                }
                applicationDataQueue.addData(buf, offset, len);
                processApplicationData();
                break;
            default:
        }
    }

    private void processHandshake() throws IOException {
        boolean read;
        do {
            read = false;
            if (handshakeQueue.size() >= 4) {
                byte[] beginning = new byte[4];
                handshakeQueue.read(beginning, 0, 4, 0);
                ByteArrayInputStream bis = new ByteArrayInputStream(beginning);
                short type = TlsUtils.readUint8(bis);
                int len = TlsUtils.readUint24(bis);
                if (handshakeQueue.size() >= (len + 4)) {
                    byte[] buf = new byte[len];
                    handshakeQueue.read(buf, 0, len, 4);
                    handshakeQueue.removeData(len + 4);
                    if (type != HP_FINISHED) {
                        rs.hash1.update(beginning, 0, 4);
                        rs.hash2.update(beginning, 0, 4);
                        rs.hash1.update(buf, 0, len);
                        rs.hash2.update(buf, 0, len);
                    }
                    ByteArrayInputStream is = new ByteArrayInputStream(buf);
                    switch(type) {
                        case HP_CERTIFICATE:
                            switch(connection_state) {
                                case CS_SERVER_HELLO_RECEIVED:
                                    Certificate cert = Certificate.parse(is);
                                    assertEmpty(is);
                                    if (!this.verifyer.isValid(cert.getCerts())) {
                                        this.failWithError(AL_fatal, AP_user_canceled);
                                    }
                                    RSAPublicKeyStructure rsaKey = null;
                                    try {
                                        rsaKey = RSAPublicKeyStructure.getInstance(cert.certs[0].getTBSCertificate().getSubjectPublicKeyInfo().getPublicKey());
                                    } catch (Exception e) {
                                        this.failWithError(AL_fatal, AP_unsupported_certificate);
                                    }
                                    this.serverRsaKey = new RSAKeyParameters(false, rsaKey.getModulus(), rsaKey.getPublicExponent());
                                    connection_state = CS_SERVER_CERTIFICATE_RECEIVED;
                                    read = true;
                                    break;
                                default:
                                    this.failWithError(AL_fatal, AP_unexpected_message);
                            }
                            break;
                        case HP_FINISHED:
                            switch(connection_state) {
                                case CS_SERVER_CHANGE_CIPHER_SPEC_RECEIVED:
                                    byte[] receivedChecksum = new byte[12];
                                    TlsUtils.readFully(receivedChecksum, is);
                                    assertEmpty(is);
                                    byte[] checksum = new byte[12];
                                    byte[] md5andsha1 = new byte[16 + 20];
                                    rs.hash2.doFinal(md5andsha1, 0);
                                    TlsUtils.PRF(this.ms, TlsUtils.toByteArray("server finished"), md5andsha1, checksum);
                                    for (int i = 0; i < receivedChecksum.length; i++) {
                                        if (receivedChecksum[i] != checksum[i]) {
                                            this.failWithError(AL_fatal, AP_handshake_failure);
                                        }
                                    }
                                    connection_state = CS_DONE;
                                    this.appDataReady = true;
                                    read = true;
                                    break;
                                default:
                                    this.failWithError(AL_fatal, AP_unexpected_message);
                            }
                            break;
                        case HP_SERVER_HELLO:
                            switch(connection_state) {
                                case CS_CLIENT_HELLO_SEND:
                                    TlsUtils.checkVersion(is, this);
                                    this.serverRandom = new byte[32];
                                    TlsUtils.readFully(this.serverRandom, is);
                                    short sessionIdLength = TlsUtils.readUint8(is);
                                    byte[] sessionId = new byte[sessionIdLength];
                                    TlsUtils.readFully(sessionId, is);
                                    this.chosenCipherSuite = TlsCipherSuiteManager.getCipherSuite(TlsUtils.readUint16(is), this);
                                    short compressionMethod = TlsUtils.readUint8(is);
                                    if (compressionMethod != 0) {
                                        this.failWithError(TlsProtocolHandler.AL_fatal, TlsProtocolHandler.AP_illegal_parameter);
                                    }
                                    assertEmpty(is);
                                    connection_state = CS_SERVER_HELLO_RECEIVED;
                                    read = true;
                                    break;
                                default:
                                    this.failWithError(AL_fatal, AP_unexpected_message);
                            }
                            break;
                        case HP_SERVER_HELLO_DONE:
                            switch(connection_state) {
                                case CS_SERVER_CERTIFICATE_RECEIVED:
                                    if (this.chosenCipherSuite.getKeyExchangeAlgorithm() != TlsCipherSuite.KE_RSA) {
                                        this.failWithError(AL_fatal, AP_unexpected_message);
                                    }
                                case CS_SERVER_KEY_EXCHANGE_RECEIVED:
                                case CS_CERTIFICATE_REQUEST_RECEIVED:
                                    assertEmpty(is);
                                    boolean isCertReq = (connection_state == CS_CERTIFICATE_REQUEST_RECEIVED);
                                    connection_state = CS_SERVER_HELLO_DONE_RECEIVED;
                                    if (isCertReq) {
                                        sendClientCertificate();
                                    }
                                    short ke = this.chosenCipherSuite.getKeyExchangeAlgorithm();
                                    switch(ke) {
                                        case TlsCipherSuite.KE_RSA:
                                            pms = new byte[48];
                                            pms[0] = 3;
                                            pms[1] = 1;
                                            for (int i = 2; i < 48; i++) {
                                                pms[i] = (byte) random.nextInt();
                                            }
                                            RSABlindedEngine rsa = new RSABlindedEngine();
                                            PKCS1Encoding encoding = new PKCS1Encoding(rsa);
                                            encoding.init(true, new ParametersWithRandom(this.serverRsaKey, this.random));
                                            byte[] encrypted = null;
                                            try {
                                                encrypted = encoding.processBlock(pms, 0, pms.length);
                                            } catch (InvalidCipherTextException e) {
                                                this.failWithError(AL_fatal, AP_internal_error);
                                            }
                                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                            TlsUtils.writeUint8(HP_CLIENT_KEY_EXCHANGE, bos);
                                            TlsUtils.writeUint24(encrypted.length + 2, bos);
                                            TlsUtils.writeUint16(encrypted.length, bos);
                                            bos.write(encrypted);
                                            byte[] message = bos.toByteArray();
                                            rs.writeMessage((short) RL_HANDSHAKE, message, 0, message.length);
                                            break;
                                        case TlsCipherSuite.KE_DHE_RSA:
                                            byte[] YcByte = this.Yc.toByteArray();
                                            ByteArrayOutputStream DHbos = new ByteArrayOutputStream();
                                            TlsUtils.writeUint8(HP_CLIENT_KEY_EXCHANGE, DHbos);
                                            TlsUtils.writeUint24(YcByte.length + 2, DHbos);
                                            TlsUtils.writeUint16(YcByte.length, DHbos);
                                            DHbos.write(YcByte);
                                            byte[] DHmessage = DHbos.toByteArray();
                                            rs.writeMessage((short) RL_HANDSHAKE, DHmessage, 0, DHmessage.length);
                                            break;
                                        default:
                                            this.failWithError(AL_fatal, AP_unexpected_message);
                                    }
                                    connection_state = CS_CLIENT_KEY_EXCHANGE_SEND;
                                    byte[] cmessage = new byte[1];
                                    cmessage[0] = 1;
                                    rs.writeMessage((short) RL_CHANGE_CIPHER_SPEC, cmessage, 0, cmessage.length);
                                    connection_state = CS_CLIENT_CHANGE_CIPHER_SPEC_SEND;
                                    this.ms = new byte[48];
                                    byte[] random = new byte[clientRandom.length + serverRandom.length];
                                    System.arraycopy(clientRandom, 0, random, 0, clientRandom.length);
                                    System.arraycopy(serverRandom, 0, random, clientRandom.length, serverRandom.length);
                                    TlsUtils.PRF(pms, TlsUtils.toByteArray("master secret"), random, this.ms);
                                    rs.writeSuite = this.chosenCipherSuite;
                                    rs.writeSuite.init(this.ms, clientRandom, serverRandom);
                                    byte[] checksum = new byte[12];
                                    byte[] md5andsha1 = new byte[16 + 20];
                                    rs.hash1.doFinal(md5andsha1, 0);
                                    TlsUtils.PRF(this.ms, TlsUtils.toByteArray("client finished"), md5andsha1, checksum);
                                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                    TlsUtils.writeUint8(HP_FINISHED, bos);
                                    TlsUtils.writeUint24(12, bos);
                                    bos.write(checksum);
                                    byte[] message = bos.toByteArray();
                                    rs.writeMessage((short) RL_HANDSHAKE, message, 0, message.length);
                                    this.connection_state = CS_CLIENT_FINISHED_SEND;
                                    read = true;
                                    break;
                                default:
                                    this.failWithError(AL_fatal, AP_handshake_failure);
                            }
                            break;
                        case HP_SERVER_KEY_EXCHANGE:
                            switch(connection_state) {
                                case CS_SERVER_CERTIFICATE_RECEIVED:
                                    if (this.chosenCipherSuite.getKeyExchangeAlgorithm() != TlsCipherSuite.KE_DHE_RSA) {
                                        this.failWithError(AL_fatal, AP_unexpected_message);
                                    }
                                    int pLength = TlsUtils.readUint16(is);
                                    byte[] pByte = new byte[pLength];
                                    TlsUtils.readFully(pByte, is);
                                    int gLength = TlsUtils.readUint16(is);
                                    byte[] gByte = new byte[gLength];
                                    TlsUtils.readFully(gByte, is);
                                    int YsLength = TlsUtils.readUint16(is);
                                    byte[] YsByte = new byte[YsLength];
                                    TlsUtils.readFully(YsByte, is);
                                    int sigLength = TlsUtils.readUint16(is);
                                    byte[] sigByte = new byte[sigLength];
                                    TlsUtils.readFully(sigByte, is);
                                    this.assertEmpty(is);
                                    CombinedHash sigDigest = new CombinedHash();
                                    ByteArrayOutputStream signedData = new ByteArrayOutputStream();
                                    TlsUtils.writeUint16(pLength, signedData);
                                    signedData.write(pByte);
                                    TlsUtils.writeUint16(gLength, signedData);
                                    signedData.write(gByte);
                                    TlsUtils.writeUint16(YsLength, signedData);
                                    signedData.write(YsByte);
                                    byte[] signed = signedData.toByteArray();
                                    sigDigest.update(this.clientRandom, 0, this.clientRandom.length);
                                    sigDigest.update(this.serverRandom, 0, this.serverRandom.length);
                                    sigDigest.update(signed, 0, signed.length);
                                    byte[] hash = new byte[sigDigest.getDigestSize()];
                                    sigDigest.doFinal(hash, 0);
                                    RSABlindedEngine rsa = new RSABlindedEngine();
                                    PKCS1Encoding encoding = new PKCS1Encoding(rsa);
                                    encoding.init(false, this.serverRsaKey);
                                    byte[] sigHash = null;
                                    try {
                                        sigHash = encoding.processBlock(sigByte, 0, sigByte.length);
                                    } catch (InvalidCipherTextException e) {
                                        this.failWithError(AL_fatal, AP_bad_certificate);
                                    }
                                    if (sigHash.length != hash.length) {
                                        this.failWithError(AL_fatal, AP_bad_certificate);
                                    }
                                    for (int i = 0; i < sigHash.length; i++) {
                                        if (sigHash[i] != hash[i]) {
                                            this.failWithError(AL_fatal, AP_bad_certificate);
                                        }
                                    }
                                    BigInteger p = new BigInteger(1, pByte);
                                    BigInteger g = new BigInteger(1, gByte);
                                    BigInteger Ys = new BigInteger(1, YsByte);
                                    BigInteger x = new BigInteger(p.bitLength() - 1, this.random);
                                    Yc = g.modPow(x, p);
                                    this.pms = BigIntegers.asUnsignedByteArray(Ys.modPow(x, p));
                                    this.connection_state = CS_SERVER_KEY_EXCHANGE_RECEIVED;
                                    read = true;
                                    break;
                                default:
                                    this.failWithError(AL_fatal, AP_unexpected_message);
                            }
                            break;
                        case HP_CERTIFICATE_REQUEST:
                            switch(connection_state) {
                                case CS_SERVER_CERTIFICATE_RECEIVED:
                                    if (this.chosenCipherSuite.getKeyExchangeAlgorithm() != TlsCipherSuite.KE_RSA) {
                                        this.failWithError(AL_fatal, AP_unexpected_message);
                                    }
                                case CS_SERVER_KEY_EXCHANGE_RECEIVED:
                                    short typesLength = TlsUtils.readUint8(is);
                                    byte[] types = new byte[typesLength];
                                    TlsUtils.readFully(types, is);
                                    int authsLength = TlsUtils.readUint16(is);
                                    byte[] auths = new byte[authsLength];
                                    TlsUtils.readFully(auths, is);
                                    assertEmpty(is);
                                    this.connection_state = CS_CERTIFICATE_REQUEST_RECEIVED;
                                    read = true;
                                    break;
                                default:
                                    this.failWithError(AL_fatal, AP_unexpected_message);
                            }
                            break;
                        case HP_HELLO_REQUEST:
                        case HP_CLIENT_KEY_EXCHANGE:
                        case HP_CERTIFICATE_VERIFY:
                        case HP_CLIENT_HELLO:
                        default:
                            this.failWithError(AL_fatal, AP_unexpected_message);
                            break;
                    }
                }
            }
        } while (read);
    }

    private void processApplicationData() {
    }

    private void processAlert() throws IOException {
        while (alertQueue.size() >= 2) {
            byte[] tmp = new byte[2];
            alertQueue.read(tmp, 0, 2, 0);
            alertQueue.removeData(2);
            short level = tmp[0];
            short description = tmp[1];
            if (level == AL_fatal) {
                this.failedWithError = true;
                this.closed = true;
                try {
                    rs.close();
                } catch (Exception e) {
                }
                throw new IOException(TLS_ERROR_MESSAGE);
            } else {
                if (description == AP_close_notify) {
                    this.failWithError(AL_warning, AP_close_notify);
                }
            }
        }
    }

    /**
     * This method is called, when a change cipher spec message is received.
     *
     * @throws IOException If the message has an invalid content or the
     *                     handshake is not in the correct state.
     */
    private void processChangeCipherSpec() throws IOException {
        while (changeCipherSpecQueue.size() > 0) {
            byte[] b = new byte[1];
            changeCipherSpecQueue.read(b, 0, 1, 0);
            changeCipherSpecQueue.removeData(1);
            if (b[0] != 1) {
                this.failWithError(AL_fatal, AP_unexpected_message);
            } else {
                if (this.connection_state == CS_CLIENT_FINISHED_SEND) {
                    rs.readSuite = rs.writeSuite;
                    this.connection_state = CS_SERVER_CHANGE_CIPHER_SPEC_RECEIVED;
                } else {
                    this.failWithError(AL_fatal, AP_handshake_failure);
                }
            }
        }
    }

    private void sendClientCertificate() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TlsUtils.writeUint8(HP_CERTIFICATE, bos);
        TlsUtils.writeUint24(3, bos);
        TlsUtils.writeUint24(0, bos);
        byte[] message = bos.toByteArray();
        rs.writeMessage((short) RL_HANDSHAKE, message, 0, message.length);
    }

    /**
     * Connects to the remote system.
     *
     * @param verifyer Will be used when a certificate is received to verify
     *                 that this certificate is accepted by the client.
     * @throws IOException If handshake was not successful.
     */
    public void connect(CertificateVerifyer verifyer) throws IOException {
        this.verifyer = verifyer;
        this.clientRandom = new byte[32];
        int t = (int) (System.currentTimeMillis() / 1000);
        this.clientRandom[0] = (byte) (t >> 24);
        this.clientRandom[1] = (byte) (t >> 16);
        this.clientRandom[2] = (byte) (t >> 8);
        this.clientRandom[3] = (byte) t;
        for (int i = 4; i < clientRandom.length; i++) {
            this.clientRandom[i] = (byte) random.nextInt();
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        TlsUtils.writeVersion(os);
        os.write(this.clientRandom);
        TlsUtils.writeUint8((short) 0, os);
        TlsCipherSuiteManager.writeCipherSuites(os);
        byte[] compressionMethods = new byte[] { 0x00 };
        TlsUtils.writeUint8((short) compressionMethods.length, os);
        os.write(compressionMethods);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TlsUtils.writeUint8(HP_CLIENT_HELLO, bos);
        TlsUtils.writeUint24(os.size(), bos);
        bos.write(os.toByteArray());
        byte[] message = bos.toByteArray();
        rs.writeMessage(RL_HANDSHAKE, message, 0, message.length);
        connection_state = CS_CLIENT_HELLO_SEND;
        while (connection_state != CS_DONE) {
            rs.readData();
        }
        this.tlsInputStream = new TlsInputStream(this);
        this.tlsOutputStream = new TlsOuputStream(this);
    }

    /**
     * Read data from the network. The method will return immediately, if there is
     * still some data left in the buffer, or block until some application
     * data has been read from the network.
     *
     * @param buf    The buffer where the data will be copied to.
     * @param offset The position where the data will be placed in the buffer.
     * @param len    The maximum number of bytes to read.
     * @return The number of bytes read.
     * @throws IOException If something goes wrong during reading data.
     */
    protected int readApplicationData(byte[] buf, int offset, int len) throws IOException {
        while (applicationDataQueue.size() == 0) {
            if (this.failedWithError) {
                throw new IOException(TLS_ERROR_MESSAGE);
            }
            if (this.closed) {
                return -1;
            }
            try {
                rs.readData();
            } catch (IOException e) {
                if (!this.closed) {
                    this.failWithError(AL_fatal, AP_internal_error);
                }
                throw e;
            } catch (RuntimeException e) {
                if (!this.closed) {
                    this.failWithError(AL_fatal, AP_internal_error);
                }
                throw e;
            }
        }
        len = Math.min(len, applicationDataQueue.size());
        applicationDataQueue.read(buf, offset, len, 0);
        applicationDataQueue.removeData(len);
        return len;
    }

    /**
     * Send some application data to the remote system.
     * <p/>
     * The method will handle fragmentation internally.
     *
     * @param buf    The buffer with the data.
     * @param offset The position in the buffer where the data is placed.
     * @param len    The length of the data.
     * @throws IOException If something goes wrong during sending.
     */
    protected void writeData(byte[] buf, int offset, int len) throws IOException {
        if (this.failedWithError) {
            throw new IOException(TLS_ERROR_MESSAGE);
        }
        if (this.closed) {
            throw new IOException("Sorry, connection has been closed, you cannot write more data");
        }
        rs.writeMessage(RL_APPLICATION_DATA, emptybuf, 0, 0);
        do {
            int toWrite = Math.min(len, 1 << 14);
            try {
                rs.writeMessage(RL_APPLICATION_DATA, buf, offset, toWrite);
            } catch (IOException e) {
                if (!closed) {
                    this.failWithError(AL_fatal, AP_internal_error);
                }
                throw e;
            } catch (RuntimeException e) {
                if (!closed) {
                    this.failWithError(AL_fatal, AP_internal_error);
                }
                throw e;
            }
            offset += toWrite;
            len -= toWrite;
        } while (len > 0);
    }

    /** @deprecated use 'getOutputStream' instead */
    public TlsOuputStream getTlsOuputStream() {
        return this.tlsOutputStream;
    }

    /**
     * @return An OutputStream which can be used to send data.
     */
    public OutputStream getOutputStream() {
        return this.tlsOutputStream;
    }

    /** @deprecated use 'getInputStream' instead */
    public TlsInputStream getTlsInputStream() {
        return this.tlsInputStream;
    }

    /**
     * @return An InputStream which can be used to read data.
     */
    public InputStream getInputStream() {
        return this.tlsInputStream;
    }

    /**
     * Terminate this connection whith an alert.
     * <p/>
     * Can be used for normal closure too.
     *
     * @param alertLevel       The level of the alert, an be AL_fatal or AL_warning.
     * @param alertDescription The exact alert message.
     * @throws IOException If alert was fatal.
     */
    protected void failWithError(short alertLevel, short alertDescription) throws IOException {
        if (!closed) {
            byte[] error = new byte[2];
            error[0] = (byte) alertLevel;
            error[1] = (byte) alertDescription;
            this.closed = true;
            if (alertLevel == AL_fatal) {
                this.failedWithError = true;
            }
            rs.writeMessage(RL_ALERT, error, 0, 2);
            rs.close();
            if (alertLevel == AL_fatal) {
                throw new IOException(TLS_ERROR_MESSAGE);
            }
        } else {
            throw new IOException(TLS_ERROR_MESSAGE);
        }
    }

    /**
     * Closes this connection.
     *
     * @throws IOException If something goes wrong during closing.
     */
    public void close() throws IOException {
        if (!closed) {
            this.failWithError((short) 1, (short) 0);
        }
    }

    /**
     * Make sure the InputStream is now empty. Fail otherwise.
     *
     * @param is The InputStream to check.
     * @throws IOException If is is not empty.
     */
    protected void assertEmpty(ByteArrayInputStream is) throws IOException {
        if (is.available() > 0) {
            this.failWithError(AL_fatal, AP_decode_error);
        }
    }

    protected void flush() throws IOException {
        rs.flush();
    }
}
