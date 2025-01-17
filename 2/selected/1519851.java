package com.ecyrd.jspwiki.auth.authorize;

import java.io.IOException;
import java.net.URL;
import java.security.Principal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;

/**
 * Authorizes users by delegating role membership checks to the servlet
 * container. In addition to implementing methods for the
 * <code>Authorizer</code> interface, this class also provides a convenience
 * method {@link #isContainerAuthorized()} that queries the web application
 * descriptor to determine if the container manages authorization.
 * @author Andrew Jaquith
 * @since 2.3
 */
public class WebContainerAuthorizer implements WebAuthorizer {

    private static final String J2EE_SCHEMA_24_NAMESPACE = "http://java.sun.com/xml/ns/j2ee";

    protected static final Logger log = Logger.getLogger(WebContainerAuthorizer.class.getName());

    protected WikiEngine m_engine;

    /**
     * A lazily-initialized array of Roles that the container knows about. These
     * are parsed from JSPWiki's <code>web.xml</code> web application
     * deployment descriptor. If this file cannot be read for any reason, the
     * role list will be empty. This is a hack designed to get around the fact
     * that we have no direct way of querying the web container about which
     * roles it manages.
     */
    protected Role[] m_containerRoles = new Role[0];

    /**
     * Lazily-initialized boolean flag indicating whether the web container
     * protects JSPWiki resources.
     */
    protected boolean m_containerAuthorized = false;

    private Document m_webxml = null;

    /**
     * Constructs a new instance of the WebContainerAuthorizer class.
     */
    public WebContainerAuthorizer() {
        super();
    }

    /**
     * Initializes the authorizer for.
     * @param engine the current wiki engine
     * @param props the wiki engine initialization properties
     */
    public void initialize(WikiEngine engine, Properties props) {
        m_engine = engine;
        m_containerAuthorized = false;
        try {
            m_webxml = getWebXml();
            if (m_webxml != null) {
                m_webxml.getRootElement().setNamespace(Namespace.getNamespace(J2EE_SCHEMA_24_NAMESPACE));
                m_containerAuthorized = isConstrained("/Delete.jsp", Role.ALL) && isConstrained("/Login.jsp", Role.ALL);
            }
            if (m_containerAuthorized) {
                m_containerRoles = getRoles(m_webxml);
                log.log(Level.INFO, "JSPWiki is using container-managed authentication.");
            } else {
                log.log(Level.INFO, "JSPWiki is using custom authentication.");
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Initialization failed: ", e);
            throw new InternalWikiException(e.getClass().getName() + ": " + e.getMessage());
        } catch (JDOMException e) {
            log.log(Level.SEVERE, "Malformed XML in web.xml", e);
            throw new InternalWikiException(e.getClass().getName() + ": " + e.getMessage());
        }
        if (m_containerRoles.length > 0) {
            String roles = "";
            for (Role containerRole : m_containerRoles) {
                roles = roles + containerRole + " ";
            }
            log.log(Level.INFO, " JSPWiki determined the web container manages these roles: " + roles);
        }
        log.log(Level.INFO, "Authorizer WebContainerAuthorizer initialized successfully.");
    }

    /**
     * Determines whether a user associated with an HTTP request possesses
     * a particular role. This method simply delegates to 
     * {@link javax.servlet.http.HttpServletRequest#isUserInRole(String)}
     * by converting the Principal's name to a String.
     * @param request the HTTP request
     * @param role the role to check
     * @return <code>true</code> if the user is considered to be in the role,
     *         <code>false</code> otherwise
     */
    public boolean isUserInRole(HttpServletRequest request, Principal role) {
        return request.isUserInRole(role.getName());
    }

    /**
     * Determines whether the Subject associated with a WikiSession is in a
     * particular role. This method takes two parameters: the WikiSession
     * containing the subject and the desired role ( which may be a Role or a
     * Group). If either parameter is <code>null</code>, this method must
     * return <code>false</code>.
     * This method simply examines the WikiSession subject to see if it
     * possesses the desired Principal. We assume that the method
     * {@link com.ecyrd.jspwiki.ui.WikiServletFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
     * previously executed, and that it has set the WikiSession
     * subject correctly by logging in the user with the various login modules,
     * in particular {@link com.ecyrd.jspwiki.auth.login.WebContainerLoginModule}}.
     * This is definitely a hack,
     * but it eliminates the need for WikiSession to keep dangling
     * references to the last WikiContext hanging around, just
     * so we can look up the HttpServletRequest.
     *
     * @param session the current WikiSession
     * @param role the role to check
     * @return <code>true</code> if the user is considered to be in the role,
     *         <code>false</code> otherwise
     * @see com.ecyrd.jspwiki.auth.Authorizer#isUserInRole(com.ecyrd.jspwiki.WikiSession, java.security.Principal)
     */
    public boolean isUserInRole(WikiSession session, Principal role) {
        if (session == null || role == null) {
            return false;
        }
        return session.hasPrincipal(role);
    }

    /**
     * Looks up and returns a Role Principal matching a given String. If the
     * Role does not match one of the container Roles identified during
     * initialization, this method returns <code>null</code>.
     * @param role the name of the Role to retrieve
     * @return a Role Principal, or <code>null</code>
     * @see com.ecyrd.jspwiki.auth.Authorizer#initialize(WikiEngine, Properties)
     */
    public Principal findRole(String role) {
        for (Role containerRole : m_containerRoles) {
            if (containerRole.getName().equals(role)) {
                return containerRole;
            }
        }
        return null;
    }

    /**
     * <p>
     * Protected method that identifies whether a particular webapp URL is
     * constrained to a particular Role. The resource is considered constrained
     * if:
     * </p>
     * <ul>
     * <li>the web application deployment descriptor contains a
     * <code>security-constraint</code> with a child
     * <code>web-resource-collection/url-pattern</code> element matching the
     * URL, <em>and</em>:</li>
     * <li>this constraint also contains an
     * <code>auth-constraint/role-name</code> element equal to the supplied
     * Role's <code>getName()</code> method. If the supplied Role is Role.ALL,
     * it matches all roles</li>
     * </ul>
     * @param url the web resource
     * @param role the role
     * @return <code>true</code> if the resource is constrained to the role,
     *         <code>false</code> otherwise
     * @throws JDOMException if elements cannot be parsed correctly
     */
    public boolean isConstrained(String url, Role role) throws JDOMException {
        Element root = m_webxml.getRootElement();
        XPath xpath;
        String selector;
        selector = "//j:web-app/j:security-constraint[j:web-resource-collection/j:url-pattern=\"" + url + "\"]";
        xpath = XPath.newInstance(selector);
        xpath.addNamespace("j", J2EE_SCHEMA_24_NAMESPACE);
        List<?> constraints = xpath.selectNodes(root);
        selector = "//j:web-app/j:security-constraint[j:auth-constraint/j:role-name=\"" + role.getName() + "\"]";
        xpath = XPath.newInstance(selector);
        xpath.addNamespace("j", J2EE_SCHEMA_24_NAMESPACE);
        List<?> roles = xpath.selectNodes(root);
        if (constraints.size() == 0) {
            return false;
        }
        if (role.equals(Role.ALL)) {
            return true;
        }
        if (roles.size() == 0) {
            return false;
        }
        for (Iterator<?> c = constraints.iterator(); c.hasNext(); ) {
            Element constraint = (Element) c.next();
            for (Iterator<?> r = roles.iterator(); r.hasNext(); ) {
                Element roleConstraint = (Element) r.next();
                if (constraint.equals(roleConstraint)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if the web container is configured to protect
     * certain JSPWiki resources by requiring authentication. Specifically, this
     * method parses JSPWiki's web application descriptor (<code>web.xml</code>)
     * and identifies whether the string representation of
     * {@link com.ecyrd.jspwiki.auth.authorize.Role#AUTHENTICATED} is required
     * to access <code>/Delete.jsp</code> and <code>LoginRedirect.jsp</code>.
     * If the administrator has uncommented the large
     * <code>&lt;security-constraint&gt;</code> section of <code>web.xml</code>,
     * this will be true. This is admittedly an indirect way to go about it, but
     * it should be an accurate test for default installations, and also in 99%
     * of customized installs.
     * @return <code>true</code> if the container protects resources,
     *         <code>false</code> otherwise
     */
    public boolean isContainerAuthorized() {
        return m_containerAuthorized;
    }

    /**
     * Returns an array of role Principals this Authorizer knows about.
     * This method will return an array of Role objects corresponding to
     * the logical roles enumerated in the <code>web.xml</code>.
     * This method actually returns a defensive copy of an internally stored
     * array.
     * @return an array of Principals representing the roles
     */
    public Principal[] getRoles() {
        return m_containerRoles.clone();
    }

    /**
     * Protected method that extracts the roles from JSPWiki's web application
     * deployment descriptor. Each Role is constructed by using the String
     * representation of the Role, for example
     * <code>new Role("Administrator")</code>.
     * @param webxml the web application deployment descriptor
     * @return an array of Role objects
     * @throws JDOMException if elements cannot be parsed correctly
     */
    protected Role[] getRoles(Document webxml) throws JDOMException {
        Set<Role> roles = new HashSet<Role>();
        Element root = webxml.getRootElement();
        String selector = "//j:web-app/j:security-constraint/j:auth-constraint/j:role-name";
        XPath xpath = XPath.newInstance(selector);
        xpath.addNamespace("j", J2EE_SCHEMA_24_NAMESPACE);
        List<?> nodes = xpath.selectNodes(root);
        for (Iterator<?> it = nodes.iterator(); it.hasNext(); ) {
            String role = ((Element) it.next()).getTextTrim();
            roles.add(new Role(role));
        }
        selector = "//j:web-app/j:security-role/j:role-name";
        xpath = XPath.newInstance(selector);
        xpath.addNamespace("j", J2EE_SCHEMA_24_NAMESPACE);
        nodes = xpath.selectNodes(root);
        for (Iterator<?> it = nodes.iterator(); it.hasNext(); ) {
            String role = ((Element) it.next()).getTextTrim();
            roles.add(new Role(role));
        }
        return roles.toArray(new Role[roles.size()]);
    }

    /**
     * Returns an {@link org.jdom.Document} representing JSPWiki's web
     * application deployment descriptor. The document is obtained by calling
     * the servlet context's <code>getResource()</code> method and requesting
     * <code>/WEB-INF/web.xml</code>. For non-servlet applications, this
     * method calls this class'
     * {@link ClassLoader#getResource(java.lang.String)} and requesting
     * <code>WEB-INF/web.xml</code>.
     * @return the descriptor
     * @throws IOException if the deployment descriptor cannot be found or opened
     * @throws JDOMException if the deployment descriptor cannot be parsed correctly
     */
    protected Document getWebXml() throws JDOMException, IOException {
        URL url;
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(false);
        builder.setEntityResolver(new LocalEntityResolver());
        Document doc = null;
        if (m_engine.getServletContext() == null) {
            ClassLoader cl = WebContainerAuthorizer.class.getClassLoader();
            url = cl.getResource("WEB-INF/web.xml");
            if (url != null) log.log(Level.INFO, "Examining " + url.toExternalForm());
        } else {
            url = m_engine.getServletContext().getResource("/WEB-INF/web.xml");
            if (url != null) log.log(Level.INFO, "Examining " + url.toExternalForm());
        }
        if (url == null) throw new IOException("Unable to find web.xml for processing.");
        log.log(Level.INFO, "Processing web.xml at " + url.toExternalForm());
        doc = builder.build(url);
        return doc;
    }

    /**
     * <p>XML entity resolver that redirects resolution requests by JDOM, JAXP and
     * other XML parsers to locally-cached copies of the resources. Local
     * resources are stored in the <code>WEB-INF/dtd</code> directory.</p>
     * <p>For example, Sun Microsystem's DTD for the webapp 2.3 specification is normally
     * kept at <code>http://java.sun.com/dtd/web-app_2_3.dtd</code>. The
     * local copy is stored at <code>WEB-INF/dtd/web-app_2_3.dtd</code>.</p>
     * @author Andrew Jaquith
     */
    public class LocalEntityResolver implements EntityResolver {

        /**
         * Returns an XML input source for a requested external resource by
         * reading the resource instead from local storage. The local resource path
         * is <code>WEB-INF/dtd</code>, plus the file name of the requested
         * resource, minus the non-filename path information.
         * @param publicId the public ID, such as
         *            <code>-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN</code>
         * @param systemId the system ID, such as
         *            <code>http://java.sun.com/dtd/web-app_2_3.dtd</code>
         * @return the InputSource containing the resolved resource
         * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String,
         *      java.lang.String)
         * @throws SAXException if the resource cannot be resolved locally
         * @throws IOException if the resource cannot be opened
         */
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            String file = systemId.substring(systemId.lastIndexOf('/') + 1);
            URL url;
            if (m_engine.getServletContext() == null) {
                ClassLoader cl = WebContainerAuthorizer.class.getClassLoader();
                url = cl.getResource("WEB-INF/dtd/" + file);
            } else {
                url = m_engine.getServletContext().getResource("/WEB-INF/dtd/" + file);
            }
            if (url != null) {
                InputSource is = new InputSource(url.openStream());
                log.log(Level.INFO, "Resolved systemID=" + systemId + " using local file " + url);
                return is;
            }
            log.log(Level.INFO, "Please note: There are no local DTD references in /WEB-INF/dtd/" + file + "; falling back to default behaviour." + " This may mean that the XML parser will attempt to connect to the internet to find the DTD." + " If you are running JSPWiki locally in an unconnected network, you might want to put the DTD files in place to avoid nasty UnknownHostExceptions.");
            return null;
        }
    }
}
