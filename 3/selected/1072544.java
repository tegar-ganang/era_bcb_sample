package net.sf.dsorapart;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.naming.Context;
import javax.naming.NamingException;
import org.apache.directory.server.core.authn.AbstractAuthenticator;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.Authenticator;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.authz.AciAuthorizationInterceptor;
import org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor;
import org.apache.directory.server.core.collective.CollectiveAttributeInterceptor;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerStringValue;
import org.apache.directory.server.core.event.EventInterceptor;
import org.apache.directory.server.core.exception.ExceptionInterceptor;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.core.operational.OperationalAttributeInterceptor;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.server.core.referral.ReferralInterceptor;
import org.apache.directory.server.core.schema.SchemaInterceptor;
import org.apache.directory.server.core.subtree.SubentryInterceptor;
import org.apache.directory.server.core.trigger.TriggerInterceptor;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.constants.LdapSecurityConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationException;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.ArrayUtils;
import org.apache.directory.shared.ldap.util.Base64;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.directory.shared.ldap.util.UnixCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link Authenticator} that authenticates clear text passwords
 * contained within the <code>userPassword</code> attribute in DIT. If the
 * password is stored with a one-way encryption applied (e.g. SHA), the password
 * is hashed the same way before comparison.
 * 
 * We use a cache to speedup authentication, where the DN/password are stored.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OracleSimpleAuthenticator extends AbstractAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(OracleSimpleAuthenticator.class);

    /** A speedup for logger in debug mode */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /**
     * Define the interceptors we should *not* go through when we will have to request the backend
     * about a userPassword.
     */
    private static final Collection<String> USERLOOKUP_BYPASS;

    static {
        Set<String> c = new HashSet<String>();
        c.add(NormalizationInterceptor.class.getName());
        c.add(AuthenticationInterceptor.class.getName());
        c.add(ReferralInterceptor.class.getName());
        c.add(AciAuthorizationInterceptor.class.getName());
        c.add(DefaultAuthorizationInterceptor.class.getName());
        c.add(ExceptionInterceptor.class.getName());
        c.add(OperationalAttributeInterceptor.class.getName());
        c.add(SchemaInterceptor.class.getName());
        c.add(SubentryInterceptor.class.getName());
        c.add(CollectiveAttributeInterceptor.class.getName());
        c.add(EventInterceptor.class.getName());
        c.add(TriggerInterceptor.class.getName());
        USERLOOKUP_BYPASS = Collections.unmodifiableCollection(c);
    }

    /**
     * Creates a new instance.
     * @see AbstractAuthenticator
     */
    public OracleSimpleAuthenticator() {
        super(AuthenticationLevel.SIMPLE.toString());
    }

    /**
     * A private class to store all informations about the existing
     * password found in the cache or get from the backend.
     * 
     * This is necessary as we have to compute :
     * - the used algorithm
     * - the salt if any
     * - the password itself.
     * 
     * If we have a on-way encrypted password, it is stored using this 
     * format :
     * {<algorithm>}<encrypted password>
     * where the encrypted password format can be :
     * - MD5/SHA : base64([<salt (8 bytes)>]<password>)
     * - crypt : <salt (2 btytes)><password> 
     * 
     * Algorithm are currently MD5, SMD5, SHA, SSHA, CRYPT and empty
     */
    private class EncryptionMethod {

        private byte[] salt;

        private LdapSecurityConstants algorithm;

        private EncryptionMethod(LdapSecurityConstants algorithm, byte[] salt) {
            this.algorithm = algorithm;
            this.salt = salt;
        }
    }

    /**
     * Get the password either from cache or from backend.
     * @param principalDN The DN from which we want the password
     * @return A byte array which can be empty if the password was not found
     * @throws NamingException If we have a problem during the lookup operation
     */
    private LdapPrincipal getStoredPassword(Registries registries, LdapDN principalDN) throws NamingException {
        LdapPrincipal principal = null;
        byte[] storedPassword;
        if (principal == null) {
            storedPassword = lookupUserPassword(registries, principalDN);
            if (storedPassword == null) {
                storedPassword = ArrayUtils.EMPTY_BYTE_ARRAY;
            }
            principal = new LdapPrincipal(principalDN, AuthenticationLevel.SIMPLE, storedPassword);
        }
        return principal;
    }

    /**
     * Get the user credentials from the environment. It is stored into the
     * ServcerContext.
     *
     * @param ctx the naming context to get the credentials from
     * @return the credentials
     * @throws LdapAuthenticationException if the there are probelms with security
     * credentials provided
     */
    private byte[] getCredentials(ServerContext ctx) throws LdapAuthenticationException {
        Object creds = ctx.getEnvironment().get(Context.SECURITY_CREDENTIALS);
        byte[] credentials;
        if (creds == null) {
            credentials = ArrayUtils.EMPTY_BYTE_ARRAY;
        } else if (creds instanceof String) {
            credentials = StringTools.getBytesUtf8((String) creds);
        } else if (creds instanceof byte[]) {
            credentials = (byte[]) creds;
        } else {
            LOG.info("Incorrect credentials stored in {}", Context.SECURITY_CREDENTIALS);
            throw new LdapAuthenticationException();
        }
        return credentials;
    }

    /**
     * Looks up <tt>userPassword</tt> attribute of the entry whose name is the
     * value of {@link Context#SECURITY_PRINCIPAL} environment variable, and
     * authenticates a user with the plain-text password.
     * 
     * We have at least 6 algorithms to encrypt the password :
     * - SHA
     * - SSHA (salted SHA)
     * - MD5
     * - SMD5 (slated MD5)
     * - crypt (unix crypt)
     * - plain text, ie no encryption.
     * 
     *  If we get an encrypted password, it is prefixed by the used algorithm, between
     *  brackets : {SSHA}password ...
     *  
     *  If the password is using SSHA, SMD5 or crypt, some 'salt' is added to the password :
     *  - length(password) - 20, starting at 21th position for SSHA
     *  - length(password) - 16, starting at 16th position for SMD5
     *  - length(password) - 2, starting at 3rd position for crypt
     *  
     *  For (S)SHA and (S)MD5, we have to transform the password from Base64 encoded text
     *  to a byte[] before comparing the password with the stored one.
     *  For crypt, we only have to remove the salt.
     *  
     *  At the end, we use the digest() method for (S)SHA and (S)MD5, the crypt() method for
     *  the CRYPT algorithm and a straight comparison for PLAIN TEXT passwords.
     *  
     *  The stored password is always using the unsalted form, and is stored as a bytes array.
     */
    public LdapPrincipal authenticate(LdapDN principalDn, ServerContext ctx) throws NamingException {
        if (IS_DEBUG) {
            LOG.debug("Authenticating {}", principalDn);
        }
        byte[] credentials = getCredentials(ctx);
        LdapPrincipal principal = getStoredPassword(getDirectoryService().getRegistries(), principalDn);
        byte[] storedPassword = principal.getUserPassword();
        if (Arrays.equals(credentials, storedPassword)) {
            if (IS_DEBUG) {
                LOG.debug("{} Authenticated", principalDn);
            }
            return principal;
        }
        LdapSecurityConstants algorithm = findAlgorithm(storedPassword);
        if (algorithm != null) {
            EncryptionMethod encryptionMethod = new EncryptionMethod(algorithm, null);
            byte[] encryptedStored = splitCredentials(storedPassword, encryptionMethod);
            byte[] userPassword = encryptPassword(credentials, encryptionMethod);
            if (Arrays.equals(userPassword, encryptedStored)) {
                if (IS_DEBUG) {
                    LOG.debug("{} Authenticated", principalDn);
                }
                return principal;
            } else {
                String message = "Password not correct for user '" + principalDn.getUpName() + "'";
                LOG.info(message);
                throw new LdapAuthenticationException(message);
            }
        } else {
            String message = "Password not correct for user '" + principalDn.getUpName() + "'";
            LOG.info(message);
            throw new LdapAuthenticationException(message);
        }
    }

    private static void split(byte[] all, int offset, byte[] left, byte[] right) {
        System.arraycopy(all, offset, left, 0, left.length);
        System.arraycopy(all, offset + left.length, right, 0, right.length);
    }

    /**
     * Decopose the stored password in an algorithm, an eventual salt
     * and the password itself.
     * 
     * If the algorithm is SHA, SSHA, MD5 or SMD5, the part following the algorithm
     * is base64 encoded
     * 
     * @param encryptionMethod The structure to feed
     * @return The password
     * @param credentials the credentials to split
     */
    private byte[] splitCredentials(byte[] credentials, EncryptionMethod encryptionMethod) {
        int pos = encryptionMethod.algorithm.getName().length() + 2;
        switch(encryptionMethod.algorithm) {
            case HASH_METHOD_MD5:
            case HASH_METHOD_SHA:
                try {
                    return Base64.decode(new String(credentials, pos, credentials.length - pos, "UTF-8").toCharArray());
                } catch (UnsupportedEncodingException uee) {
                    return credentials;
                }
            case HASH_METHOD_SMD5:
            case HASH_METHOD_SSHA:
                try {
                    byte[] passwordAndSalt = Base64.decode(new String(credentials, pos, credentials.length - pos, "UTF-8").toCharArray());
                    encryptionMethod.salt = new byte[8];
                    byte[] password = new byte[passwordAndSalt.length - encryptionMethod.salt.length];
                    split(passwordAndSalt, 0, password, encryptionMethod.salt);
                    return password;
                } catch (UnsupportedEncodingException uee) {
                    return credentials;
                }
            case HASH_METHOD_CRYPT:
                encryptionMethod.salt = new byte[2];
                byte[] password = new byte[credentials.length - encryptionMethod.salt.length - pos];
                split(credentials, pos, encryptionMethod.salt, password);
                return password;
            default:
                return credentials;
        }
    }

    /**
     * Get the algorithm from the stored password. 
     * It can be found on the beginning of the stored password, between 
     * curly brackets.
     * @param credentials the credentials of the user
     * @return the name of the algorithm to use
     * TODO use an enum for the algorithm
     */
    private LdapSecurityConstants findAlgorithm(byte[] credentials) {
        if ((credentials == null) || (credentials.length == 0)) {
            return null;
        }
        if (credentials[0] == '{') {
            int pos = 1;
            while (pos < credentials.length) {
                if (credentials[pos] == '}') {
                    break;
                }
                pos++;
            }
            if (pos < credentials.length) {
                if (pos == 1) {
                    return null;
                }
                String algorithm = new String(credentials, 1, pos - 1).toLowerCase();
                return LdapSecurityConstants.getAlgorithm(algorithm);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Compute the hashed password given an algorithm, the credentials and 
     * an optional salt.
     *
     * @param algorithm the algorithm to use
     * @param password the credentials
     * @param salt the optional salt
     * @return the digested credentials
     */
    private static byte[] digest(LdapSecurityConstants algorithm, byte[] password, byte[] salt) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm.getName());
        } catch (NoSuchAlgorithmException e1) {
            return null;
        }
        if (salt != null) {
            digest.update(password);
            digest.update(salt);
            return digest.digest();
        } else {
            return digest.digest(password);
        }
    }

    private byte[] encryptPassword(byte[] credentials, EncryptionMethod encryptionMethod) {
        byte[] salt = encryptionMethod.salt;
        switch(encryptionMethod.algorithm) {
            case HASH_METHOD_SHA:
            case HASH_METHOD_SSHA:
                return digest(LdapSecurityConstants.HASH_METHOD_SHA, credentials, salt);
            case HASH_METHOD_MD5:
            case HASH_METHOD_SMD5:
                return digest(LdapSecurityConstants.HASH_METHOD_MD5, credentials, salt);
            case HASH_METHOD_CRYPT:
                if (salt == null) {
                    salt = new byte[2];
                    SecureRandom sr = new SecureRandom();
                    int i1 = sr.nextInt(64);
                    int i2 = sr.nextInt(64);
                    salt[0] = (byte) (i1 < 12 ? (i1 + '.') : i1 < 38 ? (i1 + 'A' - 12) : (i1 + 'a' - 38));
                    salt[1] = (byte) (i2 < 12 ? (i2 + '.') : i2 < 38 ? (i2 + 'A' - 12) : (i2 + 'a' - 38));
                }
                String saltWithCrypted = UnixCrypt.crypt(StringTools.utf8ToString(credentials), StringTools.utf8ToString(salt));
                String crypted = saltWithCrypted.substring(2);
                return StringTools.getBytesUtf8(crypted);
            default:
                return credentials;
        }
    }

    /**
     * Local function which request the password from the backend
     * @param principalDn the principal to lookup
     * @return the credentials from the backend
     * @throws NamingException if there are problems accessing backend
     */
    private byte[] lookupUserPassword(Registries registries, LdapDN principalDn) throws NamingException {
        Invocation invocation = InvocationStack.getInstance().peek();
        PartitionNexusProxy proxy = invocation.getProxy();
        ServerEntry userEntry;
        try {
            LookupOperationContext lookupContex = new LookupOperationContext(registries, new String[] { SchemaConstants.USER_PASSWORD_AT });
            lookupContex.setDn(principalDn);
            userEntry = proxy.lookup(lookupContex, USERLOOKUP_BYPASS);
            if (userEntry == null) {
                throw new LdapAuthenticationException("Failed to lookup user for authentication: " + principalDn);
            }
        } catch (Exception cause) {
            LOG.error("Authentication error : " + cause.getMessage());
            LdapAuthenticationException e = new LdapAuthenticationException();
            e.setRootCause(e);
            throw e;
        }
        Value userPassword;
        EntryAttribute userPasswordAttr = userEntry.get(SchemaConstants.USER_PASSWORD_AT);
        if (userPasswordAttr == null) {
            return StringTools.EMPTY_BYTES;
        } else {
            userPassword = userPasswordAttr.get();
            if (userPassword instanceof ServerStringValue) {
                return StringTools.getBytesUtf8((String) userPassword.get());
            } else {
                return (byte[]) userPassword.get();
            }
        }
    }

    /**
     * Get the algorithm of a password, which is stored in the form "{XYZ}...".
     * The method returns null, if the argument is not in this form. It returns
     * XYZ, if XYZ is an algorithm known to the MessageDigest class of
     * java.security.
     * 
     * @param password a byte[]
     * @return included message digest alorithm, if any
     * @throws IllegalArgumentException if the algorithm cannot be identified
     */
    protected String getAlgorithmForHashedPassword(byte[] password) throws IllegalArgumentException {
        String result = null;
        String sPassword = StringTools.utf8ToString(password);
        int rightParen = sPassword.indexOf('}');
        if ((sPassword.length() > 2) && (sPassword.charAt(0) == '{') && (rightParen > -1)) {
            String algorithm = sPassword.substring(1, rightParen);
            if (LdapSecurityConstants.HASH_METHOD_CRYPT.getName().equalsIgnoreCase(algorithm)) {
                return algorithm;
            }
            try {
                MessageDigest.getInstance(algorithm);
                result = algorithm;
            } catch (NoSuchAlgorithmException e) {
                LOG.warn("Unknown message digest algorithm in password: " + algorithm, e);
            }
        }
        return result;
    }

    /**
     * Creates a digested password. For a given hash algorithm and a password
     * value, the algorithm is applied to the password, and the result is Base64
     * encoded. The method returns a String which looks like "{XYZ}bbbbbbb",
     * whereas XYZ is the name of the algorithm, and bbbbbbb is the Base64
     * encoded value of XYZ applied to the password.
     * 
     * @param algorithm
     *            an algorithm which is supported by
     *            java.security.MessageDigest, e.g. SHA
     * @param password
     *            password value, a byte[]
     * 
     * @return a digested password, which looks like
     *         {SHA}LhkDrSoM6qr0fW6hzlfOJQW61tc=
     * 
     * @throws IllegalArgumentException
     *             if password is neither a String nor a byte[], or algorithm is
     *             not known to java.security.MessageDigest class
     */
    protected String createDigestedPassword(String algorithm, byte[] password) throws IllegalArgumentException {
        try {
            if (LdapSecurityConstants.HASH_METHOD_CRYPT.getName().equalsIgnoreCase(algorithm)) {
                String saltWithCrypted = UnixCrypt.crypt(StringTools.utf8ToString(password), "");
                String crypted = saltWithCrypted.substring(2);
                return '{' + algorithm + '}' + Arrays.toString(StringTools.getBytesUtf8(crypted));
            } else {
                MessageDigest digest = MessageDigest.getInstance(algorithm);
                byte[] fingerPrint = digest.digest(password);
                char[] encoded = Base64.encode(fingerPrint);
                return '{' + algorithm + '}' + new String(encoded);
            }
        } catch (NoSuchAlgorithmException nsae) {
            LOG.error("Cannot create a digested password for algorithm '{}'", algorithm);
            throw new IllegalArgumentException(nsae.getMessage());
        }
    }

    /**
     * Remove the principal form the cache. This is used when the user changes
     * his password.
     */
    public void invalidateCache(LdapDN bindDn) {
    }
}
