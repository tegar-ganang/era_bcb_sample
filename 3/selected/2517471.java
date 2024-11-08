package org.rapla.plugin.jndi;

import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.apache.avalon.framework.activity.Startable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.Logger;
import org.rapla.components.util.Tools;
import org.rapla.entities.Category;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaMap;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.jndi.internal.JNDIConf;
import org.rapla.storage.AuthenticationStore;
import org.rapla.storage.RaplaSecurityException;

/**
 * This Plugin is based on the jakarta.apache.org/tomcat JNDI Realm
 * and enables the authentication of a rapla user against a JNDI-Directory.
 * The most commen usecase is LDAP-Authentication, but ActiveDirectory
 * may be possible, too.

 * <li>Each user element has a distinguished name that can be formed by
 *     substituting the presented username into a pattern configured by the
 *     <code>userPattern</code> property.</li>
 * </li>
 *
 * <li>The user may be authenticated by binding to the directory with the
 *      username and password presented. This method is used when the
 *      <code>userPassword</code> property is not specified.</li>
 *
 * <li>The user may be authenticated by retrieving the value of an attribute
 *     from the directory and comparing it explicitly with the value presented
 *     by the user. This method is used when the <code>userPassword</code>
 *     property is specified, in which case:
 *     <ul>
 *     <li>The element for this user must contain an attribute named by the
 *         <code>userPassword</code> property.
 *     <li>The value of the user password attribute is either a cleartext
 *         String, or the result of passing a cleartext String through the
 *         <code>digest()</code> method (using the standard digest
 *         support included in <code>RealmBase</code>).
 *     <li>The user is considered to be authenticated if the presented
 *         credentials (after being passed through
 *         <code>digest()</code>) are equal to the retrieved value
 *         for the user password attribute.</li>
 *     </ul></li>
 *
 */
public class JNDIAuthenticationStore extends AbstractLogEnabled implements AuthenticationStore, Startable, JNDIConf {

    /**
     * Digest algorithm used in storing passwords in a non-plaintext format.
     * Valid values are those accepted for the algorithm name by the
     * MessageDigest class, or <code>null</code> if no digesting should
     * be performed.
     */
    protected String digest = null;

    /**
     * The MessageDigest object for digesting user credentials (passwords).
     */
    protected MessageDigest md = null;

    /**
     * The connection username for the server we will contact.
     */
    protected String connectionName = null;

    /**
     * The connection password for the server we will contact.
     */
    protected String connectionPassword = null;

    /**
     * The connection URL for the server we will contact.
     */
    protected String connectionURL = null;

    /**
     * The directory context linking us to our directory server.
     */
    protected DirContext context = null;

    /**
     * The JNDI context factory used to acquire our InitialContext.  By
     * default, assumes use of an LDAP server using the standard JNDI LDAP
     * provider.
     */
    protected String contextFactory = "com.sun.jndi.ldap.LdapCtxFactory";

    /**
     * The attribute name used to retrieve the user password.
     */
    protected String userPassword = null;

    /**
     * The attribute name used to retrieve the user email.
     */
    protected String userMail = null;

    /**
     * The attribute name used to retrieve the complete name of the user.
     */
    protected String userCn = null;

    /**
     * The base element for user searches.
     */
    protected String userBase = "";

    /**
     * The message format used to search for a user, with "{0}" marking
     * the spot where the username goes.
     */
    protected String userSearch = null;

    /**
     * The MessageFormat object associated with the current
     * <code>userSearch</code>.
     */
    protected MessageFormat userSearchFormat = null;

    /**
     * The number of connection attempts.  If greater than zero we use the
     * alternate url.
     */
    protected int connectionAttempt = 0;

    RaplaContext rapla_context;

    public JNDIAuthenticationStore(RaplaContext context, Configuration config, Logger logger) throws RaplaException {
        enableLogging(logger);
        this.rapla_context = context;
        Map map = generateMap(config);
        initWithMap(map);
    }

    public static Map generateMap(Configuration config) {
        String[] attributes = config.getAttributeNames();
        Map map = new TreeMap();
        for (int i = 0; i < attributes.length; i++) {
            map.put(attributes[i], config.getAttribute(attributes[i], null));
        }
        return map;
    }

    public static JNDIAuthenticationStore createJNDIAuthenticationStore(Map config, Logger logger) throws RaplaException {
        return new JNDIAuthenticationStore(config, logger);
    }

    private JNDIAuthenticationStore(Map config, Logger logger) throws RaplaException {
        enableLogging(logger);
        initWithMap(config);
    }

    private void initWithMap(Map config) throws RaplaException {
        setDigest(getAttribute(config, DIGEST, null));
        setConnectionName(getAttribute(config, CONNECTION_NAME));
        setConnectionPassword(getAttribute(config, CONNECTION_PASSWORD, null));
        setConnectionURL(getAttribute(config, CONNECTION_URL));
        setContextFactory(getAttribute(config, CONTEXT_FACTORY, contextFactory));
        setUserPassword(getAttribute(config, USER_PASSWORD, null));
        setUserMail(getAttribute(config, USER_MAIL, null));
        setUserCn(getAttribute(config, USER_CN, null));
        setUserSearch(getAttribute(config, USER_SEARCH));
        setUserBase(getAttribute(config, USER_BASE));
    }

    private String getAttribute(Map config, String key, String defaultValue) {
        Object object = config.get(key);
        if (object == null) {
            return defaultValue;
        }
        return (String) object;
    }

    private String getAttribute(Map config, String key) throws RaplaException {
        String result = getAttribute(config, key, null);
        if (result == null) {
            throw new RaplaException("Can't find provided configuration entry for key " + key);
        }
        return result;
    }

    public JNDIAuthenticationStore() {
    }

    private void log(String message, Exception ex) {
        getLogger().error(message, ex);
    }

    private void log(String message) {
        getLogger().debug(message);
    }

    public String getName() {
        return ("JNDIAuthenticationStore with contectFactory " + contextFactory);
    }

    /**
     * Set the digest algorithm used for storing credentials.
     *
     * @param digest The new digest algorithm
     */
    public void setDigest(String digest) {
        this.digest = digest;
    }

    public boolean isCreateUserEnabled() {
        return true;
    }

    /** queries the user and initialize the name and the email field. */
    public boolean initUser(org.rapla.entities.User user, String username, String password, Category userGroupCategory) throws RaplaException {
        boolean modified = false;
        JNDIUser intUser = authenticateUser(username, password);
        if (intUser == null) throw new RaplaSecurityException("Can't authenticate user " + username);
        String oldUsername = user.getUsername();
        if (oldUsername == null || !oldUsername.equals(username)) {
            user.setUsername(username);
            modified = true;
        }
        String oldEmail = user.getEmail();
        if (intUser.mail != null && (oldEmail == null || !oldEmail.equals(intUser.mail))) {
            user.setEmail(intUser.mail);
            modified = true;
        }
        String oldName = user.getName();
        if (intUser.cn != null && (oldName == null || !oldName.equals(intUser.cn))) {
            user.setName(intUser.cn);
            modified = true;
        }
        if (rapla_context != null && user.getGroups().length == 0) {
            ClientFacade facade = (ClientFacade) rapla_context.lookup(ClientFacade.ROLE);
            Preferences preferences = facade.getPreferences(null);
            RaplaMap groupList = (RaplaMap) preferences.getEntry(JNDIPlugin.USERGROUP_CONFIG);
            Collection<Category> groups;
            if (groupList == null) {
                groups = new ArrayList<Category>();
            } else {
                groups = groupList.values();
            }
            for (Category group : groups) {
                user.addGroup(group);
            }
            modified = true;
        }
        return modified;
    }

    /**
     * Set the connection username for this Realm.
     *
     * @param connectionName The new connection username
     */
    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    /**
     * Set the connection password for this Realm.
     *
     * @param connectionPassword The new connection password
     */
    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword = connectionPassword;
    }

    /**
     * Set the connection URL for this Realm.
     *
     * @param connectionURL The new connection URL
     */
    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    /**
     * Set the JNDI context factory for this Realm.
     *
     * @param contextFactory The new context factory
     */
    public void setContextFactory(String contextFactory) {
        this.contextFactory = contextFactory;
    }

    /**
     * Set the password attribute used to retrieve the user password.
     *
     * @param userPassword The new password attribute
     */
    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    /**
     * Set the mail attribute used to retrieve the user mail-address.
     */
    public void setUserMail(String userMail) {
        this.userMail = userMail;
    }

    /**
     * Set the password attribute used to retrieve the users completeName.
     */
    public void setUserCn(String userCn) {
        this.userCn = userCn;
    }

    /**
     * Set the base element for user searches.
     *
     * @param userBase The new base element
     */
    public void setUserBase(String userBase) {
        this.userBase = userBase;
    }

    /**
     * Set the message format pattern for selecting users in this Realm.
     *
     * @param userSearch The new user search pattern
     */
    public void setUserSearch(String userSearch) {
        this.userSearch = userSearch;
        if (userSearch == null) userSearchFormat = null; else userSearchFormat = new MessageFormat(userSearch);
    }

    public boolean authenticate(String username, String credentials) {
        return authenticateUser(username, credentials) != null;
    }

    /**
     * Return the Principal associated with the specified username and
     * credentials, if there is one; otherwise return <code>null</code>.
     *
     * If there are any errors with the JDBC connection, executing
     * the query or anything we return null (don't authenticate). This
     * event is also logged, and the connection will be closed so that
     * a subsequent request will automatically re-open it.
     *
     * @param username Username of the Principal to look up
     * @param credentials Password or other credentials to use in
     *  authenticating this username
     */
    private JNDIUser authenticateUser(String username, String credentials) {
        DirContext context = null;
        JNDIUser user = null;
        try {
            context = open();
            try {
                user = authenticate(context, username, credentials);
            } catch (CommunicationException e) {
                if (e.getMessage() != null && e.getMessage().indexOf("closed") < 0) throw (e);
                log("jndiRealm.exception", e);
                if (context != null) close(context);
                context = open();
                user = authenticate(context, username, credentials);
            }
            return user;
        } catch (NamingException e) {
            log("jndiRealm.exception", e);
            if (context != null) close(context);
            return null;
        }
    }

    /**
     * Return the Principal associated with the specified username and
     * credentials, if there is one; otherwise return <code>null</code>.
     *
     * @param context The directory context
     * @param username Username of the Principal to look up
     * @param credentials Password or other credentials to use in
     *  authenticating this username
     *
     * @exception NamingException if a directory server error occurs
     */
    protected synchronized JNDIUser authenticate(DirContext context, String username, String credentials) throws NamingException {
        if (username == null || username.equals("") || credentials == null || credentials.equals("")) return (null);
        JNDIUser user = getUser(context, username);
        if (user != null && checkCredentials(context, user, credentials)) return user;
        return null;
    }

    /**
     * Return a User object containing information about the user
     * with the specified username, if found in the directory;
     * otherwise return <code>null</code>.
     *
     * If the <code>userPassword</code> configuration attribute is
     * specified, the value of that attribute is retrieved from the
     * user's directory entry. If the <code>userRoleName</code>
     * configuration attribute is specified, all values of that
     * attribute are retrieved from the directory entry.
     *
     * @param context The directory context
     * @param username Username to be looked up
     *
     * @exception NamingException if a directory server error occurs
     */
    protected JNDIUser getUser(DirContext context, String username) throws NamingException {
        JNDIUser user = null;
        ArrayList list = new ArrayList();
        if (userPassword != null) list.add(userPassword);
        if (userMail != null) list.add(userMail);
        if (userCn != null) list.add(userCn);
        String[] attrIds = new String[list.size()];
        list.toArray(attrIds);
        user = getUserBySearch(context, username, attrIds);
        return user;
    }

    /**
     * Search the directory to return a User object containing
     * information about the user with the specified username, if
     * found in the directory; otherwise return <code>null</code>.
     *
     * @param context The directory context
     * @param username The username
     * @param attrIds String[]containing names of attributes to retrieve.
     *
     * @exception NamingException if a directory server error occurs
     */
    protected JNDIUser getUserBySearch(DirContext context, String username, String[] attrIds) throws NamingException {
        if (userSearchFormat == null) {
            getLogger().error("no userSearchFormat specied");
            return null;
        }
        String filter = userSearchFormat.format(new String[] { username });
        SearchControls constraints = new SearchControls();
        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
        if (attrIds == null) attrIds = new String[0];
        constraints.setReturningAttributes(attrIds);
        if (getLogger().isDebugEnabled()) {
            log("  Searching for " + username);
            log("  base: " + userBase + "  filter: " + filter);
        }
        Attributes attributes = new BasicAttributes(true);
        attributes.put(new BasicAttribute("uid", "admin"));
        NamingEnumeration results = context.search(userBase, filter, constraints);
        if (results == null || !results.hasMore()) {
            if (getLogger().isDebugEnabled()) {
                log("  username not found");
            }
            return (null);
        }
        SearchResult result = (SearchResult) results.next();
        if (results.hasMore()) {
            log("username " + username + " has multiple entries");
            return (null);
        }
        NameParser parser = context.getNameParser("");
        Name contextName = parser.parse(context.getNameInNamespace());
        Name baseName = parser.parse(userBase);
        Name entryName = parser.parse(result.getName());
        Name name = contextName.addAll(baseName);
        name = name.addAll(entryName);
        String dn = name.toString();
        if (getLogger().isDebugEnabled()) log("  entry found for " + username + " with dn " + dn);
        Attributes attrs = result.getAttributes();
        if (attrs == null) return null;
        return createUser(username, dn, attrs);
    }

    private JNDIUser createUser(String username, String dn, Attributes attrs) throws NamingException {
        String password = null;
        if (userPassword != null) password = getAttributeValue(userPassword, attrs);
        String mail = null;
        if (userMail != null) {
            mail = getAttributeValue(userMail, attrs);
        }
        String cn = null;
        if (userCn != null) {
            cn = getAttributeValue(userCn, attrs);
        }
        return new JNDIUser(username, dn, password, mail, cn);
    }

    /**
     * Check whether the given User can be authenticated with the
     * given credentials. If the <code>userPassword</code>
     * configuration attribute is specified, the credentials
     * previously retrieved from the directory are compared explicitly
     * with those presented by the user. Otherwise the presented
     * credentials are checked by binding to the directory as the
     * user.
     *
     * @param context The directory context
     * @param user The User to be authenticated
     * @param credentials The credentials presented by the user
     *
     * @exception NamingException if a directory server error occurs
     */
    protected boolean checkCredentials(DirContext context, JNDIUser user, String credentials) throws NamingException {
        boolean validated = false;
        if (userPassword == null) {
            validated = bindAsUser(context, user, credentials);
        } else {
            validated = compareCredentials(context, user, credentials);
        }
        if (getLogger().isDebugEnabled()) {
            if (validated) {
                log("jndiRealm.authenticateSuccess: " + user.username);
            } else {
                log("jndiRealm.authenticateFailure: " + user.username);
            }
        }
        return (validated);
    }

    /**
     * Check whether the credentials presented by the user match those
     * retrieved from the directory.
     *
     * @param context The directory context
     * @param info The User to be authenticated
     * @param credentials Authentication credentials
     *
     * @exception NamingException if a directory server error occurs
     */
    protected boolean compareCredentials(DirContext context, JNDIUser info, String credentials) throws NamingException {
        if (info == null || credentials == null) return (false);
        String password = info.password;
        if (password == null) return (false);
        if (getLogger().isDebugEnabled()) log("  validating credentials");
        boolean validated = false;
        if (hasMessageDigest()) {
            validated = (digest(credentials).equalsIgnoreCase(password));
        } else {
            validated = (digest(credentials).equals(password));
        }
        return (validated);
    }

    protected boolean hasMessageDigest() {
        return !(md == null);
    }

    /**
     * Digest the password using the specified algorithm and
     * convert the result to a corresponding hexadecimal string.
     * If exception, the plain credentials string is returned.
     *
     * <strong>IMPLEMENTATION NOTE</strong> - This implementation is
     * synchronized because it reuses the MessageDigest instance.
     * This should be faster than cloning the instance on every request.
     *
     * @param credentials Password or other credentials to use in
     *  authenticating this username
     */
    protected String digest(String credentials) {
        if (hasMessageDigest() == false) return (credentials);
        synchronized (this) {
            try {
                md.reset();
                md.update(credentials.getBytes());
                return (Tools.convert(md.digest()));
            } catch (Exception e) {
                log("realmBase.digest", e);
                return (credentials);
            }
        }
    }

    /**
     * Check credentials by binding to the directory as the user
     *
     * @param context The directory context
     * @param user The User to be authenticated
     * @param credentials Authentication credentials
     *
     * @exception NamingException if a directory server error occurs
     */
    protected boolean bindAsUser(DirContext context, JNDIUser user, String credentials) throws NamingException {
        if (credentials == null || user == null) return (false);
        String dn = user.dn;
        if (dn == null) return (false);
        if (getLogger().isDebugEnabled()) {
            log("  validating credentials by binding as the user");
        }
        context.addToEnvironment(Context.SECURITY_PRINCIPAL, dn);
        context.addToEnvironment(Context.SECURITY_CREDENTIALS, credentials);
        boolean validated = false;
        try {
            if (getLogger().isDebugEnabled()) {
                log("  binding as " + dn);
            }
            context.getAttributes("", null);
            validated = true;
        } catch (AuthenticationException e) {
            if (getLogger().isDebugEnabled()) {
                log("  bind attempt failed" + e.getMessage());
            }
        }
        if (connectionName != null) {
            context.addToEnvironment(Context.SECURITY_PRINCIPAL, connectionName);
        } else {
            context.removeFromEnvironment(Context.SECURITY_PRINCIPAL);
        }
        if (connectionPassword != null) {
            context.addToEnvironment(Context.SECURITY_CREDENTIALS, connectionPassword);
        } else {
            context.removeFromEnvironment(Context.SECURITY_CREDENTIALS);
        }
        return (validated);
    }

    /**
     * Return a String representing the value of the specified attribute.
     *
     * @param attrId Attribute name
     * @param attrs Attributes containing the required value
     *
     * @exception NamingException if a directory server error occurs
     */
    private String getAttributeValue(String attrId, Attributes attrs) throws NamingException {
        if (getLogger().isDebugEnabled()) log("  retrieving attribute " + attrId);
        if (attrId == null || attrs == null) return null;
        Attribute attr = attrs.get(attrId);
        if (attr == null) return (null);
        Object value = attr.get();
        if (value == null) return (null);
        String valueString = null;
        if (value instanceof byte[]) valueString = new String((byte[]) value); else valueString = value.toString();
        return valueString;
    }

    /**
     * Close any open connection to the directory server for this Realm.
     *
     * @param context The directory context to be closed
     */
    protected void close(DirContext context) {
        if (context == null) return;
        try {
            if (getLogger().isDebugEnabled()) log("Closing directory context");
            context.close();
        } catch (NamingException e) {
            log("jndiRealm.close", e);
        }
        this.context = null;
    }

    /**
     * Open (if necessary) and return a connection to the configured
     * directory server for this Realm.
     *
     * @exception NamingException if a directory server error occurs
     */
    protected DirContext open() throws NamingException {
        if (context != null) return (context);
        try {
            context = new InitialDirContext(getDirectoryContextEnvironment());
        } finally {
            connectionAttempt = 0;
        }
        return (context);
    }

    /**
     * Create our directory context configuration.
     *
     * @return java.util.Hashtable the configuration for the directory context.
     */
    protected Hashtable getDirectoryContextEnvironment() {
        Hashtable env = new Hashtable();
        if (getLogger().isDebugEnabled() && connectionAttempt == 0) log("Connecting to URL " + connectionURL);
        env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
        if (connectionName != null) env.put(Context.SECURITY_PRINCIPAL, connectionName);
        if (connectionPassword != null) env.put(Context.SECURITY_CREDENTIALS, connectionPassword);
        if (connectionURL != null && connectionAttempt == 0) env.put(Context.PROVIDER_URL, connectionURL);
        return env;
    }

    /**
     * Prepare for active use of the public methods of this Component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents it from being started
     */
    public void start() throws Exception {
        if (digest != null) {
            md = MessageDigest.getInstance(digest);
        }
        open();
    }

    /**
     * Gracefully shut down active use of the public methods of this Component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws Exception {
        close(this.context);
    }

    public static void main(String[] args) {
        JNDIAuthenticationStore aut = new JNDIAuthenticationStore();
        aut.enableLogging(new ConsoleLogger());
        aut.setConnectionName("uid=admin,ou=system");
        aut.setConnectionPassword("secret");
        aut.setConnectionURL("ldap://localhost:10389");
        aut.setUserBase("dc=example,dc=com");
        aut.setUserSearch("(uid={0})");
        try {
            aut.start();
            if (aut.authenticate("admin", "admin")) {
                System.out.println("Authentication succeeded.");
            } else {
                System.out.println("Authentication failed");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * A private class representing a User
     */
    static class JNDIUser {

        String username = null;

        String dn = null;

        String password = null;

        String mail = null;

        String cn = null;

        JNDIUser(String username, String dn, String password, String mail, String cn) {
            this.username = username;
            this.dn = dn;
            this.password = password;
            this.mail = mail;
            this.cn = cn;
        }
    }
}
