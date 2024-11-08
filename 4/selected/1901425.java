package org.openprojectservices.filters;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.openprojectservices.ext.Base64;

public class OpsAuthenticationFilter implements Filter {

    private static final String CURRENT_TYPE = "current_type";

    private static final String TYPE = "type";

    private static final String REALM = "realm";

    private static final String PROJECT_MANAGER = "projectManager";

    private static final String ADMINISTRATOR_GROUP = "AdministratorGroup";

    private static final String OPS_DOCUMENT_PATH = "opsDocumentPath";

    private static final String GET = "get";

    private static final String OWNER_REF = "ownerRef";

    private static final String WRITER = "writer";

    private static final String READER = "reader";

    private static final String MEMBER = "memberUID";

    private static final String ICS = "ics";

    private static final String TIMESHEET_OU = "timesheetOU";

    private static final String CALENDAR_OU = "calendarOU";

    private static final String PROJECT_OU = "projectOU";

    private static final String BASIC_REALM = "BASIC realm=\"";

    private static final String USERNAME = "USERNAME";

    private static final String PASSWORD = "PASSWORD";

    private static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    private static final String CONFIG_FILE = "configFile";

    private static final String PROVIDER_URL = "providerURL";

    private static final String CONTEXT_FACTORY = "contextFactory";

    private static final String USER_ATTRIBUTE = "userAttribute";

    private static final String AUTHORIZATION = "authorization";

    private static final String BASIC = "BASIC ";

    private static final String USER_OU = "userOU";

    private static final String HTTP = "HTTP";

    private static Properties properties;

    protected static Log log = org.apache.commons.logging.LogFactory.getLog(OpsAuthenticationFilter.class);

    public OpsAuthenticationFilter() {
        super();
    }

    public void init(final FilterConfig config) throws ServletException {
        if (properties == null) {
            final String file = config.getServletContext().getRealPath(config.getInitParameter(CONFIG_FILE));
            try {
                properties = loadProperties(file);
                log.debug("Propertyfile loaded successfully");
            } catch (final IOException e) {
                log.fatal("Could not load the config file: " + file);
                throw new ServletException("Could not load the config file: " + file, e);
            }
        }
    }

    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        if (!request.getProtocol().startsWith(HTTP)) {
            log.debug("Only http and https supported");
            throw new ServletException("Only http and https supported");
        }
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final String authHeader = httpRequest.getHeader(AUTHORIZATION);
        final String resource = httpRequest.getRequestURL().toString();
        final String method = httpRequest.getMethod();
        Map credentials;
        try {
            credentials = getCredentials(authHeader);
        } catch (final IOException e1) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        if (credentials == null) {
            ((HttpServletResponse) response).setHeader(WWW_AUTHENTICATE, BASIC_REALM + properties.getProperty(REALM) + "\"");
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try {
            if (!authenticate(resource, method, (String) credentials.get(USERNAME), (String) credentials.get(PASSWORD))) {
                ((HttpServletResponse) response).setHeader(WWW_AUTHENTICATE, BASIC_REALM + properties.getProperty(REALM) + "\"");
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            chain.doFilter(request, response);
        } catch (final NamingException e) {
            log.debug("NamingException " + e);
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        } catch (final IOException e) {
            log.debug("IOException " + e);
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }

    public void destroy() {
    }

    /** Loads the properties from the given file
	 * @param file the property file's name
	 * @return A properties object
	 * @throws IOException if the file can't be found or read
	 */
    private static Properties loadProperties(final String file) throws IOException {
        final Properties props = new Properties();
        FileInputStream in;
        try {
            in = new FileInputStream(file);
            props.load(in);
            in.close();
        } catch (final FileNotFoundException e) {
            log.error(e);
            throw e;
        } catch (final IOException e) {
            log.error(e);
            throw e;
        }
        return props;
    }

    /** Gets the credentials from the authenticate http-header
	 * @param header the base64 encoded header
	 * @return a Map containig the username and password
	 * @throws IOException if the header is null or not BASIC
	 * @throws DecoderException
	 * @throws AuthenticationException
	 */
    private static Map getCredentials(final String header) throws IOException {
        final Map credentials = new Hashtable();
        if (header == null || !header.toUpperCase().startsWith(BASIC)) {
            return null;
        }
        final String userpassEncoded = header.substring(6);
        final String userpassDecoded = new String(Base64.decode(userpassEncoded));
        credentials.put(PASSWORD, userpassDecoded.substring(userpassDecoded.indexOf(':') + 1));
        credentials.put(USERNAME, userpassDecoded.substring(0, userpassDecoded.indexOf(':')));
        return credentials;
    }

    /** Authenticates the user and checks if he/she is authorized to do the requested http-method on the requested resource
	 * @param resource the full http address of the resource
	 * @param method the http method
	 * @param username the username
	 * @param password the password
	 * @throws NamingException
	 * @throws OpsAuthenticationException
	 */
    private static boolean authenticate(final String resource, final String method, final String username, final String password) throws NamingException {
        if (username == null || password == null || username.length() < 1 || password.length() < 1) {
            throw new AuthenticationException("No authentication available");
        }
        DirContext initialDirContext = null;
        final Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, properties.getProperty(CONTEXT_FACTORY));
        env.put(Context.PROVIDER_URL, properties.getProperty(PROVIDER_URL));
        initialDirContext = new InitialDirContext(env);
        boolean writer = false;
        boolean reader = false;
        final String dn = properties.getProperty(USER_ATTRIBUTE) + "=" + username + "," + properties.getProperty(USER_OU);
        initialDirContext.addToEnvironment(Context.SECURITY_PRINCIPAL, dn);
        initialDirContext.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
        try {
            final String adminDN = properties.getProperty(ADMINISTRATOR_GROUP);
            final Attribute admins = initialDirContext.getAttributes(adminDN, new String[] { properties.getProperty(MEMBER) }).get(properties.getProperty(MEMBER));
            if (admins != null && admins.contains(username)) {
                log.debug("User is an admin");
                return true;
            }
            String baseDN = properties.getProperty(CALENDAR_OU);
            BasicAttributes searchTerm = new BasicAttributes(properties.getProperty(ICS), resource);
            String[] attributesToRetrieve = { properties.getProperty(READER), properties.getProperty(WRITER), properties.getProperty(OWNER_REF), properties.getProperty(TYPE) };
            NamingEnumeration searchResults = initialDirContext.search(baseDN, searchTerm, attributesToRetrieve);
            if (!searchResults.hasMoreElements()) {
                log.debug("No calendars found; search for the timesheet entry with this url");
                baseDN = properties.getProperty(TIMESHEET_OU);
                searchResults = initialDirContext.search(baseDN, searchTerm, attributesToRetrieve);
            }
            if (!searchResults.hasMoreElements()) {
                log.debug("no Calendars, no timesheets, perhaps OpsDocument?");
                int index = 0;
                for (int i = 0; i < 6; i++) {
                    index = resource.indexOf("/", index) + 1;
                    if (index == 0) {
                        return false;
                    }
                }
                baseDN = properties.getProperty(PROJECT_OU);
                final String baseURL = resource.substring(0, index);
                searchTerm = new BasicAttributes(properties.getProperty(OPS_DOCUMENT_PATH), baseURL);
                attributesToRetrieve = new String[] { properties.getProperty(MEMBER), properties.getProperty(PROJECT_MANAGER) };
                searchResults = initialDirContext.search(baseDN, searchTerm, attributesToRetrieve);
            }
            if (searchResults.hasMoreElements()) {
                final Attributes attributes = ((SearchResult) searchResults.nextElement()).getAttributes();
                String owner = null;
                String type = null;
                if (attributes.get(properties.getProperty(OWNER_REF)) != null) {
                    owner = (String) attributes.get(properties.getProperty(OWNER_REF)).get();
                }
                final Attribute readers = attributes.get(properties.getProperty(READER));
                final Attribute writers = attributes.get(properties.getProperty(WRITER));
                if (attributes.get(properties.getProperty(TYPE)) != null) {
                    type = (String) attributes.get(properties.getProperty(TYPE)).get();
                }
                final Attribute members = attributes.get(properties.getProperty(MEMBER));
                final Attribute projectManager = attributes.get(properties.getProperty(PROJECT_MANAGER));
                if ((owner != null && owner.equals(username)) || (writers != null && writers.contains(username)) || (members != null && members.contains(username)) || (projectManager != null && projectManager.contains(username))) {
                    log.debug("The user is either a owner or writer of the timesheet/calendar " + "or a member of the project of wich he/she requests a OpsDocument resource");
                    if ((type != null && type.equals(properties.get(CURRENT_TYPE))) || type == null) {
                        log.debug("either type is current or this isn'nt a calendar/timesheet");
                        writer = true;
                    } else {
                        log.debug("the user is a reader");
                        reader = true;
                    }
                } else if (readers != null && readers.contains(username)) {
                    log.debug("the user is a reader");
                    reader = true;
                }
            } else {
                return false;
            }
            if (!writer && !reader) {
                return false;
            } else if (!writer && reader && !method.equalsIgnoreCase(GET) && !method.equalsIgnoreCase("PROPFIND")) {
                return false;
            }
        } catch (final AuthenticationException e) {
            log.debug("Could not authenticate user: " + e);
            return false;
        } finally {
            if (initialDirContext != null) {
                initialDirContext.removeFromEnvironment(Context.SECURITY_PRINCIPAL);
                initialDirContext.removeFromEnvironment(Context.SECURITY_CREDENTIALS);
                initialDirContext.close();
            }
        }
        return true;
    }
}
