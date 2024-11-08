package org.acegisecurity.providers.ldap.authenticator;

import org.acegisecurity.providers.encoding.ShaPasswordEncoder;
import org.acegisecurity.providers.encoding.PasswordEncoder;
import org.acegisecurity.ldap.LdapDataAccessException;
import org.apache.commons.codec.binary.Base64;
import org.springframework.util.Assert;
import java.security.MessageDigest;

/**
 * A version of {@link ShaPasswordEncoder} which usesBase-64 encoding
 * by default and always prepends the string "{SHA}" to the encoded hash.
 *
 * @author Luke Taylor
 * @version $Id: LdapShaPasswordEncoder.java,v 1.3 2006/04/16 21:12:39 luke_t Exp $
 */
public class LdapShaPasswordEncoder implements PasswordEncoder {

    /** The nuber of bytes in a SHA hash */
    private static final int SHA_LENGTH = 20;

    private static final String SSHA_PREFIX = "{SSHA}";

    private static final String SHA_PREFIX = "{SHA}";

    public LdapShaPasswordEncoder() {
    }

    /**
     * Checks the validity of an unencoded password against an encoded one in the form
     * "{SSHA}sQuQF8vj8Eg2Y1hPdh3bkQhCKQBgjhQI".
     *
     * @param encPass the SSHA or SHA encoded password
     * @param rawPass unencoded password to be verified.
     * @param salt ignored. If the format is SSHA the salt
     *        bytes will be extracted from the encoded password.
     * @return true if they match.
     */
    public boolean isPasswordValid(String encPass, String rawPass, Object salt) {
        if (encPass.startsWith(SSHA_PREFIX)) {
            salt = extractSalt(encPass);
        } else {
            salt = null;
        }
        return encPass.equals(encodePassword(rawPass, salt));
    }

    private byte[] extractSalt(String encPass) {
        String encPassNoLabel = encPass.substring(6);
        byte[] hashAndSalt = Base64.decodeBase64(encPassNoLabel.getBytes());
        int saltLength = hashAndSalt.length - SHA_LENGTH;
        byte[] salt = new byte[saltLength];
        System.arraycopy(hashAndSalt, SHA_LENGTH, salt, 0, saltLength);
        return salt;
    }

    /**
     *
     * @param rawPass the password to be encoded.
     * @param salt the salt. Must be a byte array or null.
     *
     * @return base64 encoded concatenation of password hash and salt,
     *         prefixed with {SHA} or {SSHA} depending on whether salt
     *         bytes were supplied.
     */
    public String encodePassword(String rawPass, Object salt) {
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new LdapDataAccessException("No SHA implementation available!");
        }
        sha.update(rawPass.getBytes());
        if (salt != null) {
            Assert.isInstanceOf(byte[].class, salt, "Salt value must be a byte array");
            sha.update((byte[]) salt);
        }
        byte[] hash = combineHashAndSalt(sha.digest(), (byte[]) salt);
        return (salt == null ? SHA_PREFIX : SSHA_PREFIX) + new String(Base64.encodeBase64(hash));
    }

    private byte[] combineHashAndSalt(byte[] hash, byte[] salt) {
        if (salt == null) {
            return hash;
        }
        byte[] hashAndSalt = new byte[hash.length + salt.length];
        System.arraycopy(hash, 0, hashAndSalt, 0, hash.length);
        System.arraycopy(salt, 0, hashAndSalt, hash.length, salt.length);
        return hashAndSalt;
    }
}
