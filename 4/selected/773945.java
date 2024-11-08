package bwmorg.bouncycastle.crypto.tls;

import java.io.*;
import java.util.*;
import bigjava.math.BigInteger;
import bigjava.security.SecureRandom;
import bwmorg.bouncycastle.asn1.DERBitString;
import bwmorg.bouncycastle.asn1.x509.*;
import bwmorg.bouncycastle.crypto.*;
import bwmorg.bouncycastle.crypto.agreement.DHBasicAgreement;
import bwmorg.bouncycastle.crypto.agreement.srp.SRP6Client;
import bwmorg.bouncycastle.crypto.digests.SHA1Digest;
import bwmorg.bouncycastle.crypto.encodings.PKCS1Encoding;
import bwmorg.bouncycastle.crypto.engines.RSABlindedEngine;
import bwmorg.bouncycastle.crypto.generators.DHBasicKeyPairGenerator;
import bwmorg.bouncycastle.crypto.io.SignerInputStream;
import bwmorg.bouncycastle.crypto.params.*;
import bwmorg.bouncycastle.crypto.prng.ThreadedSeedGenerator;
import bwmorg.bouncycastle.crypto.util.PublicKeyFactory;
import bwmorg.bouncycastle.util.BigIntegers;

/**
 * An implementation of all high level protocols in TLS 1.0.
 */
public class TlsProtocolHandler {

    /**
     * BlueWhaleSystems fix: Tatiana Rybak - 24 Jul 2007
     *
     * Fake random 20-byte array for testing.
     */
    public static final byte[] CACHED_RANDOM_SEED = { -120, -56, 79, 27, -83, 78, -34, 114, 4, -106, 40, -68, 80, -24, 120, 12, -96, 52, -56, 92 };

    public static final int CACHED_RANDOM_INT = 1185486809;

    private static final BigInteger ONE = BigInteger.valueOf(1);

    private static final BigInteger TWO = BigInteger.valueOf(2);

    /**
     * BlueWhaleSystems fix: Tatiana Rybak - 08 Aug 2007
     *
     * Exchange hack: make these constants visible for RecordStream
     */
    static final short RL_CHANGE_CIPHER_SPEC = 20;

    static final short RL_ALERT = 21;

    static final short RL_HANDSHAKE = 22;

    static final short RL_APPLICATION_DATA = 23;

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

    private static final short CS_CLIENT_VERIFICATION_SEND = 8;

    private static final short CS_CLIENT_CHANGE_CIPHER_SPEC_SEND = 9;

    private static final short CS_CLIENT_FINISHED_SEND = 10;

    private static final short CS_SERVER_CHANGE_CIPHER_SPEC_RECEIVED = 11;

    private static final short CS_DONE = 12;

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

    private ByteQueue applicationDataQueue = new ByteQueue();

    private ByteQueue changeCipherSpecQueue = new ByteQueue();

    private ByteQueue alertQueue = new ByteQueue();

    private ByteQueue handshakeQueue = new ByteQueue();

    private RecordStream rs;

    private SecureRandom random;

    private AsymmetricKeyParameter serverPublicKey = null;

    private TlsInputStream tlsInputStream = null;

    private TlsOuputStream tlsOutputStream = null;

    private boolean closed = false;

    private boolean failedWithError = false;

    private boolean appDataReady = false;

    private boolean extendedClientHello;

    private byte[] clientRandom;

    private byte[] serverRandom;

    private byte[] ms;

    private TlsCipherSuite chosenCipherSuite = null;

    private BigInteger SRP_A;

    private byte[] SRP_identity, SRP_password;

    private BigInteger Yc;

    private byte[] pms;

    private CertificateVerifyer verifyer = null;

    public TlsProtocolHandler(InputStream is, OutputStream os) {
        bwmorg.LOG.trace("TlsProtocolHandler: Instantiating...");
        ThreadedSeedGenerator tsg = new ThreadedSeedGenerator();
        this.random = new SecureRandom();
        this.random.setSeed(tsg.generateSeed(20, true));
        this.rs = new RecordStream(this, is, os);
        bwmorg.LOG.trace("TlsProtocolHandler: Created.");
    }

    public TlsProtocolHandler(InputStream is, OutputStream os, SecureRandom sr) {
        this.random = sr;
        this.rs = new RecordStream(this, is, os);
    }

    private short connection_state;

    protected void processData(short protocol, byte[] buf, int offset, int len) throws IOException {
        switch(protocol) {
            case RL_CHANGE_CIPHER_SPEC:
                bwmorg.LOG.debug("TlsProtocolHandler: processData() - CHANGE_CIPHER_SPEC data");
                changeCipherSpecQueue.addData(buf, offset, len);
                processChangeCipherSpec();
                break;
            case RL_ALERT:
                bwmorg.LOG.debug("TlsProtocolHandler: processData() - ALERT data");
                alertQueue.addData(buf, offset, len);
                processAlert();
                break;
            case RL_HANDSHAKE:
                bwmorg.LOG.debug("TlsProtocolHandler: processData() - HANDSHAKE data");
                handshakeQueue.addData(buf, offset, len);
                processHandshake();
                break;
            case RL_APPLICATION_DATA:
                bwmorg.LOG.debug("TlsProtocolHandler: processData() - APPLICATION_DATA data");
                if (!appDataReady) {
                    bwmorg.LOG.info("TlsProtocolHandler: processData() - ERROR: application data is not ready");
                    this.failWithError(AL_fatal, AP_unexpected_message);
                }
                applicationDataQueue.addData(buf, offset, len);
                processApplicationData();
                break;
            default:
                bwmorg.LOG.info("TlsProtocolHandler: processData() - ERROR: Unknown data");
        }
    }

    private void processHandshake() throws IOException {
        bwmorg.LOG.debug("TlsProtocolHandler: in processHandshake()");
        boolean read;
        do {
            read = false;
            if (handshakeQueue.size() >= 4) {
                byte[] beginning = new byte[4];
                handshakeQueue.read(beginning, 0, 4, 0);
                ByteArrayInputStream bis = new ByteArrayInputStream(beginning);
                short type = TlsUtils.readUint8(bis);
                int len = TlsUtils.readUint24(bis);
                bwmorg.LOG.trace("TlsProtocolHandler: processHandshake() - type: " + type + ", len: " + len);
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
                    bwmorg.LOG.debug("TlsProtocolHandler: processHandshake() - processing handshake message. Type: " + type);
                    switch(type) {
                        case HP_CERTIFICATE:
                            {
                                bwmorg.LOG.debug("TlsProtocolHandler: processHandshake() - processing HP_CERTIFICATE");
                                switch(connection_state) {
                                    case CS_SERVER_HELLO_RECEIVED:
                                        {
                                            Certificate cert = Certificate.parse(is);
                                            assertEmpty(is);
                                            X509CertificateStructure x509Cert = cert.certs[0];
                                            SubjectPublicKeyInfo keyInfo = x509Cert.getSubjectPublicKeyInfo();
                                            try {
                                                this.serverPublicKey = PublicKeyFactory.createKey(keyInfo);
                                            } catch (RuntimeException e) {
                                                bwmorg.LOG.info("TlsProtocolHandler: Error: processHandshake() - unsupported certificate.");
                                                this.failWithError(AL_fatal, AP_unsupported_certificate);
                                            }
                                            if (this.serverPublicKey.isPrivate()) {
                                                bwmorg.LOG.info("TlsProtocolHandler: Error: processHandshake() - certificate private.");
                                                this.failWithError(AL_fatal, AP_internal_error);
                                            }
                                            switch(this.chosenCipherSuite.getKeyExchangeAlgorithm()) {
                                                case TlsCipherSuite.KE_RSA:
                                                    if (!(this.serverPublicKey instanceof RSAKeyParameters)) {
                                                        bwmorg.LOG.info("TlsProtocolHandler: Error: processHandshake() - certificate unknown.");
                                                        this.failWithError(AL_fatal, AP_certificate_unknown);
                                                    }
                                                    validateKeyUsage(x509Cert, KeyUsage.keyEncipherment);
                                                    break;
                                                case TlsCipherSuite.KE_DHE_RSA:
                                                case TlsCipherSuite.KE_SRP_RSA:
                                                    if (!(this.serverPublicKey instanceof RSAKeyParameters)) {
                                                        bwmorg.LOG.info("TlsProtocolHandler: Error: processHandshake() - certificate unknown.");
                                                        this.failWithError(AL_fatal, AP_certificate_unknown);
                                                    }
                                                    validateKeyUsage(x509Cert, KeyUsage.digitalSignature);
                                                    break;
                                                case TlsCipherSuite.KE_DHE_DSS:
                                                case TlsCipherSuite.KE_SRP_DSS:
                                                    if (!(this.serverPublicKey instanceof DSAPublicKeyParameters)) {
                                                        bwmorg.LOG.info("TlsProtocolHandler: Error: processHandshake() - certificate unknown.");
                                                        this.failWithError(AL_fatal, AP_certificate_unknown);
                                                    }
                                                    break;
                                                default:
                                                    bwmorg.LOG.info("TlsProtocolHandler: Error: processHandshake() - certificate unsupported.");
                                                    this.failWithError(AL_fatal, AP_unsupported_certificate);
                                            }
                                            if (!this.verifyer.isValid(cert.getCerts())) {
                                                bwmorg.LOG.info("TlsProtocolHandler: Error: processHandshake() - invalid certificates.");
                                                this.failWithError(AL_fatal, AP_user_canceled);
                                            }
                                            break;
                                        }
                                    default:
                                        bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: HP_CERTIFICATE received during wrong connection state.");
                                        this.failWithError(AL_fatal, AP_unexpected_message);
                                }
                                connection_state = CS_SERVER_CERTIFICATE_RECEIVED;
                                read = true;
                                bwmorg.LOG.debug("TlsProtocolHandler: processHandshake() - done processing HP_CERTIFICATE");
                                break;
                            }
                        case HP_FINISHED:
                            bwmorg.LOG.debug("TlsProtocolHandler: processHandshake() - processing HP_FINISHED");
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
                                            bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: wrong checksum.");
                                            this.failWithError(AL_fatal, AP_handshake_failure);
                                        }
                                    }
                                    connection_state = CS_DONE;
                                    this.appDataReady = true;
                                    read = true;
                                    break;
                                default:
                                    bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: HP_FINISHED received during wrong connection state.");
                                    this.failWithError(AL_fatal, AP_unexpected_message);
                            }
                            bwmorg.LOG.debug("TlsProtocolHandler: processHandshake() - done processing HP_FINISHED");
                            break;
                        case HP_SERVER_HELLO:
                            bwmorg.LOG.debug("TlsProtocolHandler: processHandshake() - processing HP_SERVER_HELLO");
                            switch(connection_state) {
                                case CS_CLIENT_HELLO_SEND:
                                    TlsUtils.checkVersion(is, this);
                                    this.serverRandom = new byte[32];
                                    TlsUtils.readFully(this.serverRandom, is);
                                    byte[] sessionId = TlsUtils.readOpaque8(is);
                                    this.chosenCipherSuite = TlsCipherSuiteManager.getCipherSuite(TlsUtils.readUint16(is), this);
                                    short compressionMethod = TlsUtils.readUint8(is);
                                    if (compressionMethod != 0) {
                                        bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: Compression not supported.");
                                        this.failWithError(TlsProtocolHandler.AL_fatal, TlsProtocolHandler.AP_illegal_parameter);
                                    }
                                    if (extendedClientHello && is.available() > 0) {
                                        byte[] extBytes = TlsUtils.readOpaque16(is);
                                        Hashtable serverExtensions = new Hashtable();
                                        ByteArrayInputStream ext = new ByteArrayInputStream(extBytes);
                                        while (ext.available() > 0) {
                                            int extType = TlsUtils.readUint16(ext);
                                            byte[] extValue = TlsUtils.readOpaque16(ext);
                                            serverExtensions.put(new Integer(extType), extValue);
                                        }
                                    }
                                    assertEmpty(is);
                                    connection_state = CS_SERVER_HELLO_RECEIVED;
                                    read = true;
                                    break;
                                default:
                                    bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: unexpected message.");
                                    this.failWithError(AL_fatal, AP_unexpected_message);
                            }
                            bwmorg.LOG.debug("TlsProtocolHandler: processHandshake() - done processing HP_SERVER_HELLO");
                            break;
                        case HP_SERVER_HELLO_DONE:
                            bwmorg.LOG.debug("TlsProtocolHandler: processHandshake() - processing HP_SERVER_HELLO_DONE");
                            switch(connection_state) {
                                case CS_SERVER_CERTIFICATE_RECEIVED:
                                    if (this.chosenCipherSuite.getKeyExchangeAlgorithm() != TlsCipherSuite.KE_RSA) {
                                        bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: Chosen key exhange algorithm is not RSA.");
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
                                    switch(this.chosenCipherSuite.getKeyExchangeAlgorithm()) {
                                        case TlsCipherSuite.KE_RSA:
                                            {
                                                pms = new byte[48];
                                                random.nextBytes(pms);
                                                pms[0] = 3;
                                                pms[1] = 1;
                                                RSABlindedEngine rsa = new RSABlindedEngine();
                                                PKCS1Encoding encoding = new PKCS1Encoding(rsa);
                                                encoding.init(true, new ParametersWithRandom(this.serverPublicKey, this.random));
                                                byte[] encrypted = null;
                                                try {
                                                    encrypted = encoding.processBlock(pms, 0, pms.length);
                                                } catch (InvalidCipherTextException e) {
                                                    bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: InvalidCipherTextException thrown.");
                                                    this.failWithError(AL_fatal, AP_internal_error);
                                                }
                                                sendClientKeyExchange(encrypted);
                                                break;
                                            }
                                        case TlsCipherSuite.KE_DHE_DSS:
                                        case TlsCipherSuite.KE_DHE_RSA:
                                            {
                                                byte[] YcByte = BigIntegers.asUnsignedByteArray(this.Yc);
                                                sendClientKeyExchange(YcByte);
                                                break;
                                            }
                                        case TlsCipherSuite.KE_SRP:
                                        case TlsCipherSuite.KE_SRP_RSA:
                                        case TlsCipherSuite.KE_SRP_DSS:
                                            {
                                                byte[] bytes = BigIntegers.asUnsignedByteArray(this.SRP_A);
                                                sendClientKeyExchange(bytes);
                                                break;
                                            }
                                        default:
                                            bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: Unknown key exhange method.");
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
                                    bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: unexpected message.");
                                    this.failWithError(AL_fatal, AP_handshake_failure);
                            }
                            bwmorg.LOG.debug("TlsProtocolHandler: processHandshake() - done processing HP_SERVER_HELLO_DONE");
                            break;
                        case HP_SERVER_KEY_EXCHANGE:
                            {
                                bwmorg.LOG.debug("TlsProtocolHandler: processHandshake() - processing HP_SERVER_KEY_EXCHANGE");
                                switch(connection_state) {
                                    case CS_SERVER_HELLO_RECEIVED:
                                        if (this.chosenCipherSuite.getKeyExchangeAlgorithm() != TlsCipherSuite.KE_SRP) {
                                            bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: Chosen key exchange is not KE_SRP.");
                                            this.failWithError(AL_fatal, AP_unexpected_message);
                                        }
                                    case CS_SERVER_CERTIFICATE_RECEIVED:
                                        {
                                            switch(this.chosenCipherSuite.getKeyExchangeAlgorithm()) {
                                                case TlsCipherSuite.KE_DHE_RSA:
                                                    {
                                                        processDHEKeyExchange(is, new TlsRSASigner());
                                                        break;
                                                    }
                                                case TlsCipherSuite.KE_DHE_DSS:
                                                    {
                                                        processDHEKeyExchange(is, new TlsDSSSigner());
                                                        break;
                                                    }
                                                case TlsCipherSuite.KE_SRP:
                                                    {
                                                        processSRPKeyExchange(is, null);
                                                        break;
                                                    }
                                                case TlsCipherSuite.KE_SRP_RSA:
                                                    {
                                                        processSRPKeyExchange(is, new TlsRSASigner());
                                                        break;
                                                    }
                                                case TlsCipherSuite.KE_SRP_DSS:
                                                    {
                                                        processSRPKeyExchange(is, new TlsDSSSigner());
                                                        break;
                                                    }
                                                default:
                                                    bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: Unknown key exchange.");
                                                    this.failWithError(AL_fatal, AP_unexpected_message);
                                            }
                                            break;
                                        }
                                    default:
                                        bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: unexpected message.");
                                        this.failWithError(AL_fatal, AP_unexpected_message);
                                }
                                this.connection_state = CS_SERVER_KEY_EXCHANGE_RECEIVED;
                                read = true;
                                bwmorg.LOG.debug("TlsProtocolHandler: processHandshake() - done processing HP_SERVER_KEY_EXCHANGE");
                                break;
                            }
                        case HP_CERTIFICATE_REQUEST:
                            {
                                bwmorg.LOG.debug("TlsProtocolHandler: processHandshake() - processing HP_CERTIFICATE_REQUEST");
                                switch(connection_state) {
                                    case CS_SERVER_CERTIFICATE_RECEIVED:
                                        if (this.chosenCipherSuite.getKeyExchangeAlgorithm() != TlsCipherSuite.KE_RSA) {
                                            bwmorg.LOG.debug("TlsProtocolHandler: processHandshake() - key exchange is not KE_RSA");
                                            this.failWithError(AL_fatal, AP_unexpected_message);
                                        }
                                    case CS_SERVER_KEY_EXCHANGE_RECEIVED:
                                        {
                                            byte[] types = TlsUtils.readOpaque8(is);
                                            byte[] auths = TlsUtils.readOpaque16(is);
                                            assertEmpty(is);
                                            break;
                                        }
                                    default:
                                        bwmorg.LOG.debug("TlsProtocolHandler: processHandshake() - AP_unexpected_message");
                                        this.failWithError(AL_fatal, AP_unexpected_message);
                                }
                                this.connection_state = CS_CERTIFICATE_REQUEST_RECEIVED;
                                read = true;
                                bwmorg.LOG.debug("TlsProtocolHandler: processHandshake() - done processing HP_CERTIFICATE_REQUEST");
                                break;
                            }
                        case HP_HELLO_REQUEST:
                            bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: HP_HELLO_REQUEST not supported.");
                            this.failWithError(AL_fatal, AP_unexpected_message);
                            break;
                        case HP_CLIENT_KEY_EXCHANGE:
                            bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: HP_CLIENT_KEY_EXCHANGE not supported.");
                            this.failWithError(AL_fatal, AP_unexpected_message);
                            break;
                        case HP_CERTIFICATE_VERIFY:
                            bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: HP_CERTIFICATE_VERIFY not supported.");
                            this.failWithError(AL_fatal, AP_unexpected_message);
                            break;
                        case HP_CLIENT_HELLO:
                            bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: HP_CLIENT_HELLO not supported.");
                            this.failWithError(AL_fatal, AP_unexpected_message);
                            break;
                        default:
                            bwmorg.LOG.info("TlsProtocolHandler: processHandshake() - Error: unknown unsupported type.");
                            this.failWithError(AL_fatal, AP_unexpected_message);
                            break;
                    }
                }
            }
        } while (read);
        bwmorg.LOG.debug("TlsProtocolHandler: done processHandshake() ");
    }

    private void processApplicationData() {
    }

    private void processAlert() throws IOException {
        bwmorg.LOG.debug("TlsProtocolHandler: in processAlert() ");
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
                    bwmorg.LOG.info("TlsProtocolHandler: Error: exception thrown in rs.close()");
                } finally {
                    rs = null;
                }
                throw new IOException("TLS processAlert");
            } else {
                if (description == AP_close_notify) {
                    this.failWithError(AL_warning, AP_close_notify);
                }
            }
        }
        bwmorg.LOG.debug("TlsProtocolHandler: done processAlert() ");
    }

    /**
     * This method is called, when a change cipher spec message is received.
     *
     * @throws IOException If the message has an invalid content or the
     *                     handshake is not in the correct state.
     */
    private void processChangeCipherSpec() throws IOException {
        bwmorg.LOG.debug("TlsProtocolHandler: in processChangeCipherSpec() ");
        while (changeCipherSpecQueue.size() > 0) {
            byte[] b = new byte[1];
            changeCipherSpecQueue.read(b, 0, 1, 0);
            changeCipherSpecQueue.removeData(1);
            if (b[0] != 1) {
                bwmorg.LOG.info("TlsProtocolHandler: processChangeCipherSpec() - Error: unexpected message.");
                this.failWithError(AL_fatal, AP_unexpected_message);
            } else {
                if (this.connection_state == CS_CLIENT_FINISHED_SEND) {
                    rs.readSuite = rs.writeSuite;
                    this.connection_state = CS_SERVER_CHANGE_CIPHER_SPEC_RECEIVED;
                } else {
                    bwmorg.LOG.info("TlsProtocolHandler: processChangeCipherSpec() - Error: Not in the correct connection state.");
                    this.failWithError(AL_fatal, AP_handshake_failure);
                }
            }
        }
        bwmorg.LOG.debug("TlsProtocolHandler: done processChangeCipherSpec() ");
    }

    private void processDHEKeyExchange(ByteArrayInputStream is, Signer signer) throws IOException {
        InputStream sigIn = is;
        if (signer != null) {
            signer.init(false, this.serverPublicKey);
            signer.update(this.clientRandom, 0, this.clientRandom.length);
            signer.update(this.serverRandom, 0, this.serverRandom.length);
            sigIn = new SignerInputStream(is, signer);
        }
        byte[] pByte = TlsUtils.readOpaque16(sigIn);
        byte[] gByte = TlsUtils.readOpaque16(sigIn);
        byte[] YsByte = TlsUtils.readOpaque16(sigIn);
        if (signer != null) {
            byte[] sigByte = TlsUtils.readOpaque16(is);
            if (!signer.verifySignature(sigByte)) {
                bwmorg.LOG.debug("TlsProtocolHandler: processDHEKeyExchange() - Error: Bad certificate.");
                this.failWithError(AL_fatal, AP_bad_certificate);
            }
        }
        this.assertEmpty(is);
        BigInteger p = new BigInteger(1, pByte);
        BigInteger g = new BigInteger(1, gByte);
        BigInteger Ys = new BigInteger(1, YsByte);
        if (!p.isProbablePrime(10)) {
            bwmorg.LOG.debug("TlsProtocolHandler: processDHEKeyExchange() - Error: Illegal parameter.");
            this.failWithError(AL_fatal, AP_illegal_parameter);
        }
        if (g.compareTo(TWO) < 0 || g.compareTo(p.subtract(TWO)) > 0) {
            bwmorg.LOG.debug("TlsProtocolHandler: processDHEKeyExchange() - Error: Illegal parameter.");
            this.failWithError(AL_fatal, AP_illegal_parameter);
        }
        if (Ys.compareTo(TWO) < 0 || Ys.compareTo(p.subtract(ONE)) > 0) {
            bwmorg.LOG.debug("TlsProtocolHandler: processDHEKeyExchange() - Error: Illegal parameter.");
            this.failWithError(AL_fatal, AP_illegal_parameter);
        }
        DHParameters dhParams = new DHParameters(p, g);
        DHBasicKeyPairGenerator dhGen = new DHBasicKeyPairGenerator();
        dhGen.init(new DHKeyGenerationParameters(random, dhParams));
        AsymmetricCipherKeyPair dhPair = dhGen.generateKeyPair();
        this.Yc = ((DHPublicKeyParameters) dhPair.getPublic()).getY();
        DHBasicAgreement dhAgree = new DHBasicAgreement();
        dhAgree.init(dhPair.getPrivate());
        BigInteger agreement = dhAgree.calculateAgreement(new DHPublicKeyParameters(Ys, dhParams));
        this.pms = BigIntegers.asUnsignedByteArray(agreement);
    }

    private void processSRPKeyExchange(ByteArrayInputStream is, Signer signer) throws IOException {
        InputStream sigIn = is;
        if (signer != null) {
            signer.init(false, this.serverPublicKey);
            signer.update(this.clientRandom, 0, this.clientRandom.length);
            signer.update(this.serverRandom, 0, this.serverRandom.length);
            sigIn = new SignerInputStream(is, signer);
        }
        byte[] NByte = TlsUtils.readOpaque16(sigIn);
        byte[] gByte = TlsUtils.readOpaque16(sigIn);
        byte[] sByte = TlsUtils.readOpaque8(sigIn);
        byte[] BByte = TlsUtils.readOpaque16(sigIn);
        if (signer != null) {
            byte[] sigByte = TlsUtils.readOpaque16(is);
            if (!signer.verifySignature(sigByte)) {
                bwmorg.LOG.debug("TlsProtocolHandler: processSRPKeyExchange() - Error: Bad certificate.");
                this.failWithError(AL_fatal, AP_bad_certificate);
            }
        }
        this.assertEmpty(is);
        BigInteger N = new BigInteger(1, NByte);
        BigInteger g = new BigInteger(1, gByte);
        byte[] s = sByte;
        BigInteger B = new BigInteger(1, BByte);
        SRP6Client srpClient = new SRP6Client();
        srpClient.init(N, g, new SHA1Digest(), random);
        this.SRP_A = srpClient.generateClientCredentials(s, this.SRP_identity, this.SRP_password);
        try {
            BigInteger S = srpClient.calculateSecret(B);
            this.pms = BigIntegers.asUnsignedByteArray(S);
        } catch (CryptoException e) {
            bwmorg.LOG.debug("TlsProtocolHandler: processSRPKeyExchange() - Error: illegal parameter.");
            this.failWithError(AL_fatal, AP_illegal_parameter);
        }
    }

    private void validateKeyUsage(X509CertificateStructure c, int keyUsageBits) throws IOException {
        X509Extensions exts = c.getTBSCertificate().getExtensions();
        if (exts != null) {
            X509Extension ext = exts.getExtension(X509Extensions.KeyUsage);
            if (ext != null) {
                DERBitString ku = KeyUsage.getInstance(ext);
                int bits = ku.getBytes()[0] & 0xff;
                if ((bits & keyUsageBits) != keyUsageBits) {
                    bwmorg.LOG.debug("TlsProtocolHandler: validateKeyUsage() - Error: certificate unknown.");
                    this.failWithError(AL_fatal, AP_certificate_unknown);
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

    private void sendClientKeyExchange(byte[] keData) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TlsUtils.writeUint8(HP_CLIENT_KEY_EXCHANGE, bos);
        TlsUtils.writeUint24(keData.length + 2, bos);
        TlsUtils.writeOpaque16(keData, bos);
        byte[] message = bos.toByteArray();
        rs.writeMessage((short) RL_HANDSHAKE, message, 0, message.length);
    }

    /**
      * BlueWhaleSystems fix: Tatiana Rybak - 15 July 2007
      *
      * Added ability to set which ciphers to report during tls negotiation.
      */
    public void connect(CertificateVerifyer verifyer) throws IOException {
        connect(verifyer, 0xFFFFFF, (int) (System.currentTimeMillis() / 1000));
    }

    /**
      * BlueWhaleSystems fix: Tatiana Rybak - 24 Jul 2007
      *
      * Pass int t as a parameter. This is used for cached data testing.
      */
    public void connect(CertificateVerifyer verifyer, int cipherMask, int t) throws IOException {
        bwmorg.LOG.debug("TlsProtocolHandler: --> in connect()");
        this.verifyer = verifyer;
        this.clientRandom = new byte[32];
        random.nextBytes(this.clientRandom);
        this.clientRandom[0] = (byte) (t >> 24);
        this.clientRandom[1] = (byte) (t >> 16);
        this.clientRandom[2] = (byte) (t >> 8);
        this.clientRandom[3] = (byte) t;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        TlsUtils.writeVersion(os);
        os.write(this.clientRandom);
        TlsUtils.writeUint8((short) 0, os);
        TlsCipherSuiteManager.writeCipherSuites(os, cipherMask);
        byte[] compressionMethods = new byte[] { 0x00 };
        TlsUtils.writeOpaque8(compressionMethods, os);
        Hashtable clientExtensions = new Hashtable();
        this.extendedClientHello = !clientExtensions.isEmpty();
        if (extendedClientHello) {
            ByteArrayOutputStream ext = new ByteArrayOutputStream();
            Enumeration keys = clientExtensions.keys();
            while (keys.hasMoreElements()) {
                Integer extType = (Integer) keys.nextElement();
                byte[] extValue = (byte[]) clientExtensions.get(extType);
                TlsUtils.writeUint16(extType.intValue(), ext);
                TlsUtils.writeOpaque16(extValue, ext);
            }
            TlsUtils.writeOpaque16(ext.toByteArray(), os);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TlsUtils.writeUint8(HP_CLIENT_HELLO, bos);
        TlsUtils.writeUint24(os.size(), bos);
        bos.write(os.toByteArray());
        byte[] message = bos.toByteArray();
        rs.writeMessage(RL_HANDSHAKE, message, 0, message.length);
        connection_state = CS_CLIENT_HELLO_SEND;
        bwmorg.LOG.trace("TlsProtocolHandler: connect() - Client HELLO sent.");
        while (connection_state != CS_DONE) {
            try {
                rs.readData();
            } catch (UnknownDataException e) {
                bwmorg.LOG.info("TlsProtocolHandler: connect() - UnknownDataException is thrown during handshake!.");
                this.failWithError(AL_fatal, AP_internal_error);
            }
        }
        this.tlsInputStream = new TlsInputStream(this);
        this.tlsOutputStream = new TlsOuputStream(this);
        bwmorg.LOG.debug("TlsProtocolHandler: <-- done connect()");
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
                bwmorg.LOG.info("TlsProtocolHandler: readApplicationData() - Unable to read data due to previous error.");
                throw new IOException("TLS readApplicationData");
            }
            if (this.closed) {
                return -1;
            }
            try {
                rs.readData();
            } catch (IOException e) {
                if (!this.closed) {
                    bwmorg.LOG.info("TlsProtocolHandler: readApplicationData() - Error: IOException thrown during writeMessage.");
                    this.failWithError(AL_fatal, AP_internal_error);
                }
                throw e;
            } catch (RuntimeException e) {
                if (!this.closed) {
                    bwmorg.LOG.info("TlsProtocolHandler: readApplicationData() - Error: Runtime Exception thrown during writeMessage.");
                    this.failWithError(AL_fatal, AP_internal_error);
                }
                throw e;
            } catch (UnknownDataException e) {
                return -1;
            }
        }
        len = Math.min(len, applicationDataQueue.size());
        applicationDataQueue.read(buf, offset, len, 0);
        applicationDataQueue.removeData(len);
        return len;
    }

    /**
     * BlueWhaleSystems fix: Tatiana Rybak - 02 Mar 2007
     * 
     * Added a method to return available bytes in the data stream.
     */
    protected int availableData() throws IOException {
        int appDataSize = applicationDataQueue.size();
        if (appDataSize > 0) {
            return appDataSize;
        }
        if (this.failedWithError) {
            bwmorg.LOG.info("TlsProtocolHandler: availableData() - Exception occured, no data available");
            throw new IOException("TLS availableData: Exception occured, no data available");
        }
        if (this.closed) {
            bwmorg.LOG.debug("TlsProtocolHandler: availableData() - Connection closed, no data available");
            return -1;
        }
        int available = rs.available();
        return available;
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
        bwmorg.LOG.debug("TlsProtocolHandler: --> in writeData(), writing: " + (len - offset) + " bytes.");
        if (this.failedWithError) {
            bwmorg.LOG.info("TlsProtocolHandler: writeData() - Error: Cannot write data due to an earlier failure.");
            throw new IOException("TLS writeData");
        }
        if (this.closed) {
            bwmorg.LOG.info("TlsProtocolHandler: writeData() - Error: Cannot write data the connection was closed.");
            throw new IOException("Sorry, connection has been closed, you cannot write more data");
        }
        rs.writeMessage(RL_APPLICATION_DATA, emptybuf, 0, 0);
        do {
            int toWrite = Math.min(len, 1 << 14);
            try {
                rs.writeMessage(RL_APPLICATION_DATA, buf, offset, toWrite);
            } catch (IOException e) {
                if (!closed) {
                    bwmorg.LOG.info("TlsProtocolHandler: writeData() - Error: IO Exception thrown during writeMessage.");
                    this.failWithError(AL_fatal, AP_internal_error);
                }
                throw e;
            } catch (RuntimeException e) {
                if (!closed) {
                    bwmorg.LOG.info("TlsProtocolHandler: writeData() - Error: Runtime Exception thrown during writeMessage.");
                    this.failWithError(AL_fatal, AP_internal_error);
                }
                throw e;
            }
            offset += toWrite;
            len -= toWrite;
        } while (len > 0);
        bwmorg.LOG.debug("TlsProtocolHandler: <-- done writeData()");
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
     * Terminate this connection with an alert.
     * <p/>
     * Can be used for normal closure too.
     *
     * @param alertLevel       The level of the alert, an be AL_fatal or AL_warning.
     * @param alertDescription The exact alert message.
     * @throws IOException If alert was fatal.
     */
    protected void failWithError(short alertLevel, short alertDescription) throws IOException {
        bwmorg.LOG.info("TlsProtocolHandler: --> in failWithError() -  alertLevel: " + alertLevel + ", AlertDescription: " + alertDescription);
        if (!closed) {
            try {
                byte[] error = new byte[2];
                error[0] = (byte) alertLevel;
                error[1] = (byte) alertDescription;
                this.closed = true;
                if (alertLevel == AL_fatal) {
                    this.failedWithError = true;
                }
                rs.writeMessage(RL_ALERT, error, 0, 2);
                rs.close();
            } finally {
                rs = null;
            }
            if (alertLevel == AL_fatal) {
                bwmorg.LOG.info("TlsProtocolHandler: failWithError() - fatal error, throwing exception");
                throw new IOException("TLS failWithError");
            }
        } else {
            bwmorg.LOG.info("TlsProtocolHandler: failWithError() - stream closed; fatal error, throwing exception");
            throw new IOException("TLS failWithError");
        }
    }

    /**
     * Closes this connection.
     *
     * @throws IOException If something goes wrong during closing.
     */
    public void close() throws IOException {
        if (!closed) {
            bwmorg.LOG.info("TlsProtocolHandler.close() - calling failWithError(1,0) for normal termination");
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
            bwmorg.LOG.info("TlsProtocolHandler: assertEmpty() - Error: input stream is not empty.");
            this.failWithError(AL_fatal, AP_decode_error);
        }
    }

    protected void flush() throws IOException {
        rs.flush();
    }
}
