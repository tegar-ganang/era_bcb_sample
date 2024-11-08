package net.kano.joscar.snaccmd.auth;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import net.kano.joscar.flapcmd.SnacPacket;
import net.kano.joscar.tlv.Tlv;
import net.kano.joscar.tlv.TlvChain;
import net.kano.joscar.tlv.TlvTools;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A SNAC command used to log into the OSCAR server.
 * <br>
 * <br>
 * For those interested, authorization is done by sending an MD5 hash of the
 * string formed by concatenating the {@linkplain KeyResponse#getKey
 * authorization key}, the user's password, and the string "AOL Instant
 * Messenger (SM)". This way the user's password is never sent over an OSCAR
 * connection.
 * <br>
 * <br>
 * Newer clients use a slightly different algorithm and send an extra empty
 * <code>0x4c</code> TLV to indicate that this algorithm was used. The algorithm
 * is almost identical to the one mentioned above except that instead of using
 * the user's password, the password's MD5 hash is used. An MD5 hash of the
 * string formed by concatenating the authorization key, an MD5 hash of the
 * user's password, and the string "AOL Instant Messenger (SM)" is sent. The
 * reason for adding this extra step is unknown, as it does not appear to
 * increase security.
 * <br>
 * <br>
 * As of version 0.9.3, joscar always uses the second algorithm.
 *
 * @snac.src client
 * @snac.cmd 0x17 0x02
 *
 * @see AuthResponse
 */
public class AuthRequest extends AuthCommand {

    /** A TLV type containing the user's screen name. */
    private static final int TYPE_SN = 0x0001;

    /** A TLV type containing the user's two-letter country code. */
    private static final int TYPE_COUNTRY = 0x000e;

    /** A TLV type containing the user's two-letter language code. */
    private static final int TYPE_LANG = 0x000f;

    /** A TLV type containing the user's password, encrypted. */
    private static final int TYPE_ENCPASS = 0x0025;

    /**
     * A TLV type indicating that the user's password was sent as its MD5 hash.
     */
    private static final int TYPE_HASHEDPASS = 0x4c;

    private static final String AIMSM_STRING = "AOL Instant Messenger (SM)";

    /** The string "AOL Instant Messenger (SM)" encoded as US-ASCII. */
    private static final byte[] AIMSM_BYTES = BinaryTools.getAsciiBytes(AIMSM_STRING);

    private static final Pattern PATTERN_NUMBER = Pattern.compile("^\\d+$");

    /** The user's screenname. */
    private final String sn;

    /** The user's client version information. */
    private final ClientVersionInfo version;

    /** The user's locale. */
    private final Locale locale;

    /** The user's password, encrypted. */
    private final ByteBlock encryptedPass;

    /** Whether or not the password was sent as its MD5 hash. */
    private final boolean hashedPass;

    /**
     * Generates an auth request command from the given incoming SNAC packet.
     *
     * @param packet an authorization request SNAC packet
     */
    protected AuthRequest(SnacPacket packet) {
        super(CMD_AUTH_REQ);
        DefensiveTools.checkNull(packet, "packet");
        TlvChain chain = TlvTools.readChain(packet.getData());
        sn = chain.getString(TYPE_SN);
        encryptedPass = chain.hasTlv(TYPE_ENCPASS) ? chain.getLastTlv(TYPE_ENCPASS).getData() : null;
        version = ClientVersionInfo.readClientVersionInfo(chain);
        String language = chain.getString(TYPE_LANG);
        String country = chain.getString(TYPE_COUNTRY);
        if (language != null && country != null) {
            locale = new Locale(language, country);
        } else {
            locale = null;
        }
        hashedPass = chain.hasTlv(TYPE_HASHEDPASS);
    }

    /**
     * Creates an outgoing authorization request command with the given
     * screenname, password, client version, and authorization key, and with the
     * JVM's current locale.
     * <br>
     * <br>
     * Calling this method is equivalent to calling {@link #AuthRequest(String,
     * String, ClientVersionInfo, Locale, ByteBlock) new AuthRequest(sn, pass,
     * version, Locale.getDefault(), key)}.
     *
     * @param sn the user's screenname
     * @param pass the user's password
     * @param version a client information block
     * @param key an authorization key block provided by the server in a
     *        {@link KeyResponse}
     */
    public AuthRequest(String sn, String pass, ClientVersionInfo version, ByteBlock key) {
        this(sn, pass, version, Locale.getDefault(), key);
    }

    /**
     * Creates an outgoing authorization request command with the given
     * screenname, password, client version, locale, and authorization key.
     *
     * @param sn the user's screenname
     * @param pass the user's password
     * @param version a client information block
     * @param locale the user's locale
     * @param key an authorization key block provided by the server in a
     *        {@link KeyResponse}
     */
    public AuthRequest(String sn, String pass, ClientVersionInfo version, Locale locale, ByteBlock key) {
        this(sn, pass, version, locale, key, isIcqNumber(sn));
    }

    /**
     * Creates an outgoing authorization request command with the given
     * screenname, password, client version, locale, and authorization key.
     *
     * @param sn the user's screenname
     * @param pass the user's password
     * @param version a client information block
     * @param locale the user's locale
     * @param key an authorization key block provided by the server in a
     *        {@link KeyResponse}
     * @param xorPassword whether to XOR the password. Normally this is only
     *        done for ICQ users.
     */
    public AuthRequest(String sn, String pass, ClientVersionInfo version, Locale locale, ByteBlock key, boolean xorPassword) {
        super(CMD_AUTH_REQ);
        this.sn = sn;
        this.version = version;
        this.locale = locale;
        if (xorPassword) {
            this.hashedPass = false;
            this.encryptedPass = ByteBlock.wrap(encryptPassword(pass, key, false));
        } else {
            this.hashedPass = true;
            this.encryptedPass = ByteBlock.wrap(encryptPassword(pass, key, true));
        }
    }

    private static boolean isIcqNumber(String sn) {
        return PATTERN_NUMBER.matcher(sn).matches();
    }

    /**
     * Encrypts the given password with the given key into a format suitable
     * for sending in an auth request packet. Note that the password string must
     * contain only US-ASCII characters.
     *
     * @param pass the user's password
     * @param key a "key" provided by the server
     * @param hashedPass whether the password should be sent as its MD5 hash
     *        like newer clients do using the <code>0x4c</code> TLV
     * @return the user's password, encrypted
     */
    private byte[] encryptPassword(String pass, ByteBlock key, boolean hashedPass) {
        byte[] passBytes = BinaryTools.getAsciiBytes(pass);
        if (hashedPass) {
            MessageDigest digest = createMd5Hasher();
            passBytes = digest.digest(passBytes);
            return getPassHash(digest, key, ByteBlock.wrap(passBytes));
        } else {
            return getPassHash(key, ByteBlock.wrap(passBytes));
        }
    }

    private MessageDigest createMd5Hasher() {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
        return digest;
    }

    /**
     * Returns the MD5 sum of the given key, the given block of password data,
     * and the string "AOL Instant Messenger (SM)".
     *
     * @param key a block of data
     * @param passBytes another block of data
     * @return the MD5 sum of the given key, password data, and {@link
     *         #AIMSM_BYTES}
     */
    private byte[] getPassHash(ByteBlock key, ByteBlock passBytes) {
        return getPassHash(createMd5Hasher(), key, passBytes);
    }

    /**
     * Returns the MD5 sum of the given key, the given block of password data,
     * and the string "AOL Instant Messenger (SM)".
     *
     * @param md5 a message digest object to use
     * @param key a block of data
     * @param passBytes another block of data
     * @return the MD5 sum of the given key, password data, and {@link
     *         #AIMSM_BYTES}
     */
    private byte[] getPassHash(MessageDigest md5, ByteBlock key, ByteBlock passBytes) {
        md5.update(key.toByteArray());
        md5.update(passBytes.toByteArray());
        md5.update(AIMSM_BYTES);
        return md5.digest();
    }

    /**
     * Returns the screen name whose login is being attempted.
     *
     * @return the user's screen name
     */
    public final String getScreenname() {
        return sn;
    }

    /**
     * Returns the user's client information block.
     *
     * @return the user's client version information
     */
    public final ClientVersionInfo getVersionInfo() {
        return version;
    }

    /**
     * Returns the user's locale.
     *
     * @return the user's locale
     */
    public final Locale getLocale() {
        return locale;
    }

    /**
     * The raw encrypted password sent in this authorization request.
     *
     * @return the user's password, encrypted
     */
    public final ByteBlock getEncryptedPass() {
        return encryptedPass;
    }

    /**
     * Returns whether the password was encoded as its MD5 hash.
     *
     * @return whether the password was encoded as its MD5 hash
     */
    public final boolean isPassHashed() {
        return hashedPass;
    }

    public void writeData(OutputStream out) throws IOException {
        if (sn != null) {
            Tlv.getStringInstance(TYPE_SN, sn).write(out);
        }
        if (encryptedPass != null) {
            new Tlv(TYPE_ENCPASS, encryptedPass).write(out);
        }
        if (hashedPass) new Tlv(TYPE_HASHEDPASS).write(out);
        if (version != null) version.write(out);
        if (locale != null) {
            String country = locale.getCountry();
            if (!country.equals("")) {
                Tlv.getStringInstance(TYPE_COUNTRY, country).write(out);
            }
            String language = locale.getLanguage();
            if (!language.equals("")) {
                Tlv.getStringInstance(TYPE_LANG, language).write(out);
            }
        }
        new Tlv(0x004a, ByteBlock.wrap(new byte[] { 0x01 })).write(out);
    }

    public String toString() {
        return "AuthRequest: " + "sn='" + sn + "'" + ", version='" + version + "'" + ", locale=" + locale;
    }
}
