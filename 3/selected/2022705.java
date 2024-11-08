package com.mindbright.ssh;

import java.io.*;
import com.mindbright.jca.security.SecureRandom;
import com.mindbright.jca.security.MessageDigest;
import com.mindbright.security.publickey.RSAPublicKey;
import com.mindbright.jca.security.interfaces.RSAPrivateCrtKey;
import com.mindbright.util.RandomSeed;
import com.mindbright.util.SecureRandomAndPad;

public abstract class SSH {

    public static boolean DEBUG = false;

    public static boolean DEBUGMORE = false;

    public static final int SSH_VER_MAJOR = 1;

    public static final int SSH_VER_MINOR = 5;

    public static final String VER_MINDTERM = "MindTerm_" + Version.version;

    public static final String VER_MINDTUNL = "MindTunnel_" + Version.version;

    public static final String CVS_NAME = "$Name:  $";

    public static final String CVS_DATE = "$Date: 2005/10/06 13:42:47 $";

    public static final int DEFAULTPORT = 22;

    public static final int SESSION_KEY_LENGTH = 256;

    public static final int SERVER_KEY_LENGTH = 768;

    public static final int HOST_KEY_LENGTH = 1024;

    public static final int PROTOFLAG_SCREEN_NUMBER = 1;

    public static final int PROTOFLAG_HOST_IN_FWD_OPEN = 2;

    public static final int MSG_ANY = -1;

    public static final int MSG_NONE = 0;

    public static final int MSG_DISCONNECT = 1;

    public static final int SMSG_PUBLIC_KEY = 2;

    public static final int CMSG_SESSION_KEY = 3;

    public static final int CMSG_USER = 4;

    public static final int CMSG_AUTH_RHOSTS = 5;

    public static final int CMSG_AUTH_RSA = 6;

    public static final int SMSG_AUTH_RSA_CHALLENGE = 7;

    public static final int CMSG_AUTH_RSA_RESPONSE = 8;

    public static final int CMSG_AUTH_PASSWORD = 9;

    public static final int CMSG_REQUEST_PTY = 10;

    public static final int CMSG_WINDOW_SIZE = 11;

    public static final int CMSG_EXEC_SHELL = 12;

    public static final int CMSG_EXEC_CMD = 13;

    public static final int SMSG_SUCCESS = 14;

    public static final int SMSG_FAILURE = 15;

    public static final int CMSG_STDIN_DATA = 16;

    public static final int SMSG_STDOUT_DATA = 17;

    public static final int SMSG_STDERR_DATA = 18;

    public static final int CMSG_EOF = 19;

    public static final int SMSG_EXITSTATUS = 20;

    public static final int MSG_CHANNEL_OPEN_CONFIRMATION = 21;

    public static final int MSG_CHANNEL_OPEN_FAILURE = 22;

    public static final int MSG_CHANNEL_DATA = 23;

    public static final int MSG_CHANNEL_CLOSE = 24;

    public static final int MSG_CHANNEL_CLOSE_CONFIRMATION = 25;

    public static final int MSG_CHANNEL_INPUT_EOF = 24;

    public static final int MSG_CHANNEL_OUTPUT_CLOSED = 25;

    public static final int SMSG_X11_OPEN = 27;

    public static final int CMSG_PORT_FORWARD_REQUEST = 28;

    public static final int MSG_PORT_OPEN = 29;

    public static final int CMSG_AGENT_REQUEST_FORWARDING = 30;

    public static final int SMSG_AGENT_OPEN = 31;

    public static final int MSG_IGNORE = 32;

    public static final int CMSG_EXIT_CONFIRMATION = 33;

    public static final int CMSG_X11_REQUEST_FORWARDING = 34;

    public static final int CMSG_AUTH_RHOSTS_RSA = 35;

    public static final int MSG_DEBUG = 36;

    public static final int CMSG_REQUEST_COMPRESSION = 37;

    public static final int CMSG_MAX_PACKET_SIZE = 38;

    public static final int CMSG_AUTH_TIS = 39;

    public static final int SMSG_AUTH_TIS_CHALLENGE = 40;

    public static final int CMSG_AUTH_TIS_RESPONSE = 41;

    public static final int CMSG_AUTH_SDI = 16;

    public static final int CMSG_ACM_OK = 64;

    public static final int CMSG_ACM_ACCESS_DENIED = 65;

    public static final int CMSG_ACM_NEXT_CODE_REQUIRED = 66;

    public static final int CMSG_ACM_NEXT_CODE = 67;

    public static final int CMSG_ACM_NEW_PIN_REQUIRED = 68;

    public static final int CMSG_ACM_NEW_PIN_ACCEPTED = 69;

    public static final int CMSG_ACM_NEW_PIN_REJECTED = 70;

    public static final int CMSG_ACM_NEW_PIN = 71;

    public static final int IDX_CIPHER_CLASS = 0;

    public static final int IDX_CIPHER_NAME = 1;

    public static final String[][] cipherClasses = { { "SSHNoEncrypt", "none" }, { "SSHIDEA", "idea-cbc" }, { "SSHDES", "des-cbc" }, { "SSHDES3", "3des-cbc" }, { null, "tss" }, { null, "arcfour" }, { "SSHBlowfish", "blowfish-cbc" }, { null, "reserved" } };

    public static final int CIPHER_NONE = 0;

    public static final int CIPHER_IDEA = 1;

    public static final int CIPHER_DES = 2;

    public static final int CIPHER_3DES = 3;

    public static final int CIPHER_TSS = 4;

    public static final int CIPHER_RC4 = 5;

    public static final int CIPHER_BLOWFISH = 6;

    public static final int CIPHER_RESERVED = 7;

    public static final int CIPHER_NOTSUPPORTED = 8;

    public static final int CIPHER_DEFAULT = CIPHER_BLOWFISH;

    public static final String[] authTypeDesc = { "_N/A_", "rhosts", "publickey", "password", "rhostsrsa", "tis", "kerberos", "kerbtgt", "securid", "cryptocard", "keyboard-interactive" };

    public static final int AUTH_RHOSTS = 1;

    public static final int AUTH_PUBLICKEY = 2;

    public static final int AUTH_PASSWORD = 3;

    public static final int AUTH_RHOSTS_RSA = 4;

    public static final int AUTH_TIS = 5;

    public static final int AUTH_KERBEROS = 6;

    public static final int PASS_KERBEROS_TGT = 7;

    public static final int AUTH_SDI = 8;

    public static final int AUTH_CRYPTOCARD = 9;

    public static final int AUTH_KBDINTERACT = 10;

    public static final int AUTH_NOTSUPPORTED = authTypeDesc.length;

    public static final int AUTH_DEFAULT = AUTH_PASSWORD;

    static final String[] proxyTypes = { "none", "http", "socks4", "socks5", "socks5-local-dns" };

    static final int[] defaultProxyPorts = { 0, 8080, 1080, 1080, 1080 };

    public static final int PROXY_NONE = 0;

    public static final int PROXY_HTTP = 1;

    public static final int PROXY_SOCKS4 = 2;

    public static final int PROXY_SOCKS5_DNS = 3;

    public static final int PROXY_SOCKS5_IP = 4;

    public static final int PROXY_NOTSUPPORTED = proxyTypes.length;

    public static final int TTY_OP_END = 0;

    public static final int TTY_OP_ISPEED = 192;

    public static final int TTY_OP_OSPEED = 193;

    public static final int MAIN_CHAN_NUM = -1;

    public static final int CONNECT_CHAN_NUM = -2;

    public static final int LISTEN_CHAN_NUM = -3;

    public static final int UNKNOWN_CHAN_NUM = -4;

    public static final String KNOWN_HOSTS_FILE = "known_hosts";

    public static final int SRV_HOSTKEY_KNOWN = 0;

    public static final int SRV_HOSTKEY_NEW = 1;

    public static final int SRV_HOSTKEY_CHANGED = 2;

    public static SecureRandomAndPad secureRandom;

    public static RandomSeed randomSeed;

    protected byte[] sessionKey;

    protected byte[] sessionId;

    protected SSHCipher sndCipher;

    protected SSHCipher rcvCipher;

    protected SSHCompressor sndComp;

    protected SSHCompressor rcvComp;

    protected int cipherType;

    protected byte[] srvCookie;

    protected RSAPublicKey srvServerKey;

    protected RSAPublicKey srvHostKey;

    protected int protocolFlags;

    protected int supportedCiphers;

    protected int supportedAuthTypes;

    protected boolean isAnSSHClient = true;

    public String getVersionId(boolean client) {
        String idStr = "SSH-" + SSH_VER_MAJOR + "." + SSH_VER_MINOR + "-";
        idStr += (client ? VER_MINDTERM : VER_MINDTUNL);
        return idStr;
    }

    public static String[] getProxyTypes() {
        return new String[] { "none", "http", "socks4", "socks5" };
    }

    public static int getProxyType(String typeName) throws IllegalArgumentException {
        int i;
        if ("socks5-proxy-dns".equals(typeName)) {
            typeName = "socks5";
        }
        for (i = 0; i < proxyTypes.length; i++) {
            if (proxyTypes[i].equalsIgnoreCase(typeName)) break;
        }
        if (i == PROXY_NOTSUPPORTED) throw new IllegalArgumentException("Proxytype " + typeName + " not supported");
        return i;
    }

    public static String getCipherName(int cipherType) {
        return cipherClasses[cipherType][IDX_CIPHER_NAME];
    }

    public static int getCipherType(String cipherName) {
        int i;
        if ("blowfish".equals(cipherName)) {
            cipherName = "blowfish-cbc";
        } else if ("rc4".equals(cipherName)) {
            cipherName = "arcfour";
        } else if ("des".equals(cipherName)) {
            cipherName = "des-cbc";
        } else if ("3des".equals(cipherName)) {
            cipherName = "3des-cbc";
        } else if ("idea".equals(cipherName)) {
            cipherName = "idea-cbc";
        }
        for (i = 0; i < cipherClasses.length; i++) {
            String ciN = cipherClasses[i][IDX_CIPHER_NAME];
            if (ciN.equalsIgnoreCase(cipherName)) {
                if (cipherClasses[i][0] == null) i = cipherClasses.length;
                break;
            }
        }
        return i;
    }

    public static String getAuthName(int authType) {
        return authTypeDesc[authType];
    }

    public static String getAltAuthName(int authType) {
        if (authType == AUTH_TIS || authType == AUTH_SDI || authType == AUTH_CRYPTOCARD) {
            return "keyboard-interactive";
        }
        return getAuthName(authType);
    }

    public static int getAuthType(String authName) throws IllegalArgumentException {
        int i;
        if ("sdi-token".equals(authName) || "kbd-interact".equals(authName) || "secureid".equals(authName)) {
            authName = "securid";
        } else if ("rsa".equals(authName)) {
            authName = "publickey";
        } else if ("passwd".equals(authName)) {
            authName = "password";
        }
        for (i = 1; i < SSH.authTypeDesc.length; i++) {
            if (authTypeDesc[i].equalsIgnoreCase(authName)) break;
        }
        if (i == AUTH_NOTSUPPORTED) throw new IllegalArgumentException("Authtype " + authName + " not supported");
        return i;
    }

    static int cntListSize(String authList) {
        int cnt = 1;
        int i = 0, n;
        while (i < authList.length() && (n = authList.indexOf(',', i)) != -1) {
            i = n + 1;
            cnt++;
        }
        return cnt;
    }

    public static int[] getAuthTypes(String authList) throws IllegalArgumentException {
        int len = cntListSize(authList);
        int[] authTypes = new int[len];
        int r, l = 0;
        String type;
        for (int i = 0; i < len; i++) {
            r = authList.indexOf(',', l);
            if (r == -1) r = authList.length();
            type = authList.substring(l, r).trim();
            authTypes[i] = getAuthType(type);
            l = r + 1;
        }
        return authTypes;
    }

    protected boolean isCipherSupported(int cipherType) {
        int cipherMask = (0x01 << cipherType);
        if ((cipherMask & supportedCiphers) != 0) return true;
        return false;
    }

    protected boolean isAuthTypeSupported(int authType) {
        int authTypeMask = (0x01 << authType);
        if ((authTypeMask & supportedAuthTypes) != 0) return true;
        return false;
    }

    protected boolean isProtocolFlagSet(int protFlag) {
        int protFlagMask = (0x01 << protFlag);
        if ((protFlagMask & protocolFlags) != 0) return true;
        return false;
    }

    public static boolean haveSecureRandom() {
        return (secureRandom != null);
    }

    public static synchronized RandomSeed randomSeed() {
        if (randomSeed == null) {
            if (com.mindbright.util.Util.isNetscapeJava()) {
                try {
                    netscape.security.PrivilegeManager.enablePrivilege("UniversalFileAccess");
                } catch (netscape.security.ForbiddenTargetException e) {
                }
            }
            randomSeed = new RandomSeed("/dev/random", "/dev/urandom");
        }
        return randomSeed;
    }

    public static void initSeedGenerator() {
        RandomSeed seed = randomSeed();
        if (secureRandom == null) {
            byte[] s = seed.getBytesBlocking(20, false);
            secureRandom = new SecureRandomAndPad(new SecureRandom(s));
        } else {
            int bytes = seed.getAvailableBits() / 8;
            if (bytes > 0) {
                secureRandom.setSeed(seed.getBytesBlocking(bytes > 20 ? 20 : bytes));
            }
        }
        secureRandom.setPadSeed(seed.getBytes(20));
    }

    public static synchronized SecureRandomAndPad secureRandom() {
        initSeedGenerator();
        return secureRandom;
    }

    public static void log(String msg) {
        if (DEBUG) System.out.println(msg);
    }

    public static void logExtra(String msg) {
        if (DEBUGMORE) System.out.println(msg);
    }

    public static void logDebug(String msg) {
        if (DEBUG) System.out.println(msg);
    }

    public static void logIgnore(SSHPduInputStream pdu) {
        if (DEBUG) System.out.println("MSG_IGNORE received...(len = " + pdu.length + ")");
    }

    void generateSessionId() throws IOException {
        byte[] message;
        byte[] srvKey = srvServerKey.getModulus().toByteArray();
        byte[] hstKey = srvHostKey.getModulus().toByteArray();
        int len = srvKey.length + hstKey.length + srvCookie.length;
        if (srvKey[0] == 0) len -= 1;
        if (hstKey[0] == 0) len -= 1;
        message = new byte[len];
        if (hstKey[0] == 0) {
            System.arraycopy(hstKey, 1, message, 0, hstKey.length - 1);
            len = hstKey.length - 1;
        } else {
            System.arraycopy(hstKey, 0, message, 0, hstKey.length);
            len = hstKey.length;
        }
        if (srvKey[0] == 0) {
            System.arraycopy(srvKey, 1, message, len, srvKey.length - 1);
            len += srvKey.length - 1;
        } else {
            System.arraycopy(srvKey, 0, message, len, srvKey.length);
            len += srvKey.length;
        }
        System.arraycopy(srvCookie, 0, message, len, srvCookie.length);
        try {
            MessageDigest md5;
            md5 = MessageDigest.getInstance("MD5");
            md5.update(message);
            sessionId = md5.digest();
        } catch (Exception e) {
            throw new IOException("MD5 not implemented, can't generate session-id");
        }
    }

    protected void initClientCipher() throws IOException {
        initCipher(false);
    }

    protected void initServerCipher() throws IOException {
        initCipher(true);
    }

    protected void initCipher(boolean server) throws IOException {
        sndCipher = SSHCipher.getInstance(cipherClasses[cipherType][0]);
        rcvCipher = SSHCipher.getInstance(cipherClasses[cipherType][0]);
        if (sndCipher == null) {
            throw new IOException("SSHCipher " + cipherClasses[cipherType][1] + " not found, can't use it");
        }
        sndCipher.setKey(sessionKey);
        rcvCipher.setKey(sessionKey);
    }

    public static String generateKeyFiles(RSAPrivateCrtKey key, String fileName, String passwd, String comment) throws IOException {
        SSHRSAKeyFile.createKeyFile(key, passwd, fileName, comment);
        SSHRSAPublicKeyString pks = new SSHRSAPublicKeyString("", comment, key.getPublicExponent(), key.getModulus());
        pks.toFile(fileName + ".pub");
        return pks.toString();
    }
}
