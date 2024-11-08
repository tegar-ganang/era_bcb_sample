package org.exist.webdav;

import org.apache.log4j.Logger;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import org.exist.security.Subject;
import org.exist.storage.BrokerPool;
import org.exist.xmldb.XmldbURI;

/**
 * Generic class representing a Milton Resource.
 * 
 * @author Dannes Wessels <dannes@exist-db.org>
 */
public class MiltonResource implements Resource {

    protected static final Logger LOG = Logger.getLogger(MiltonResource.class);

    protected XmldbURI resourceXmldbUri;

    protected BrokerPool brokerPool;

    protected String host;

    protected Subject subject;

    protected static final String AUTHENTICATED = "AUTHENTICATED";

    protected String REALM = "exist";

    protected ExistResource existResource;

    private DatatypeFactory datatypeFactory;

    public MiltonResource() {
        if (datatypeFactory == null) {
            try {
                datatypeFactory = DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException ex) {
                LOG.error(ex);
            }
        }
    }

    protected XmldbURI getXmldbUri() {
        return resourceXmldbUri;
    }

    protected String getHost() {
        return host;
    }

    private Subject getUserAsSubject() {
        return subject;
    }

    /**
     * Convert date to dateTime XML format.
     * s
     * @param date Representation of data
     * @return ISO8601 like formatted representation of date.s
     */
    protected String getXmlDateTime(Long date) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(new Date(date));
        XMLGregorianCalendar xgc = datatypeFactory.newXMLGregorianCalendar(gc);
        return xgc.toXMLFormat();
    }

    /**
     *  Converts an org.exist.dom.LockToken into com.bradmcevoy.http.LockToken.
     *
     * @param existLT Exist-db representation of a webdav token.
     * @return Milton representation of a webdav token.
     */
    protected LockToken convertToken(org.exist.dom.LockToken existLT) {
        LockInfo.LockScope scope = null;
        switch(existLT.getScope()) {
            case org.exist.dom.LockToken.LOCK_SCOPE_SHARED:
                scope = LockInfo.LockScope.SHARED;
                break;
            case org.exist.dom.LockToken.LOCK_SCOPE_EXCLUSIVE:
                scope = LockInfo.LockScope.EXCLUSIVE;
                break;
            default:
                scope = LockInfo.LockScope.NONE;
                break;
        }
        LockInfo.LockType type = null;
        switch(existLT.getType()) {
            case org.exist.dom.LockToken.LOCK_TYPE_WRITE:
                type = LockInfo.LockType.WRITE;
                break;
            default:
                type = LockInfo.LockType.READ;
                break;
        }
        String owner = existLT.getOwner();
        LockInfo.LockDepth depth = null;
        switch(existLT.getDepth()) {
            case org.exist.dom.LockToken.LOCK_DEPTH_INFINIY:
                depth = LockInfo.LockDepth.INFINITY;
                break;
            default:
                depth = LockInfo.LockDepth.ZERO;
                break;
        }
        LockInfo li = new LockInfo(scope, type, owner, depth);
        Long timeout = existLT.getTimeOut();
        if (timeout == org.exist.dom.LockToken.NO_LOCK_TIMEOUT) {
            timeout = null;
        } else if (timeout == org.exist.dom.LockToken.LOCK_TIMEOUT_INFINITE) {
            timeout = Long.MAX_VALUE;
        }
        LockTimeout lt = new LockTimeout(timeout);
        String id = existLT.getOpaqueLockToken();
        return new LockToken(id, li, lt);
    }

    /**
     *  Converts an org.exist.dom.LockToken into com.bradmcevoy.http.LockToken.
     */
    protected org.exist.dom.LockToken convertToken(LockTimeout timeout, LockInfo lockInfo) {
        org.exist.dom.LockToken existToken = new org.exist.dom.LockToken();
        switch(lockInfo.depth) {
            case ZERO:
                existToken.setDepth(org.exist.dom.LockToken.LOCK_DEPTH_0);
                break;
            case INFINITY:
                existToken.setDepth(org.exist.dom.LockToken.LOCK_DEPTH_INFINIY);
                break;
        }
        switch(lockInfo.scope) {
            case EXCLUSIVE:
                existToken.setScope(org.exist.dom.LockToken.LOCK_SCOPE_EXCLUSIVE);
                break;
            case SHARED:
                existToken.setScope(org.exist.dom.LockToken.LOCK_SCOPE_SHARED);
                break;
            case NONE:
                existToken.setScope(org.exist.dom.LockToken.LOCK_SCOPE_NONE);
                break;
        }
        switch(lockInfo.type) {
            case READ:
                existToken.setScope(org.exist.dom.LockToken.LOCK_TYPE_NONE);
                break;
            case WRITE:
                existToken.setScope(org.exist.dom.LockToken.LOCK_TYPE_WRITE);
                break;
        }
        if (timeout == null || timeout.getSeconds() == null) {
            existToken.setTimeOut(existToken.NO_LOCK_TIMEOUT);
        } else if (timeout.getSeconds() == Long.MAX_VALUE) {
            existToken.setTimeOut(org.exist.dom.LockToken.LOCK_TIMEOUT_INFINITE);
        } else {
            Long futureDate = (new Date().getTime()) / 1000 + timeout.getSeconds();
            existToken.setTimeOut(futureDate);
        }
        String user = lockInfo.lockedByUser;
        if (user != null) {
            existToken.setOwner(user);
        }
        return existToken;
    }

    /**
     *  Convert % encoded string back to text
     */
    protected XmldbURI decodePath(XmldbURI uri) {
        XmldbURI retval = null;
        try {
            String path = new URI(uri.toString()).getPath();
            retval = XmldbURI.xmldbUriFor("" + path, false);
        } catch (URISyntaxException ex) {
            LOG.error(ex.getMessage());
        }
        return retval;
    }

    /**
     *  Convert % encoded string back to text
     */
    protected String decodePath(String uri) {
        String path = null;
        try {
            path = new URI(uri).getPath();
        } catch (URISyntaxException ex) {
            LOG.error(ex.getMessage());
        }
        return path;
    }

    @Override
    public String getUniqueId() {
        return null;
    }

    @Override
    public String getName() {
        return decodePath("" + resourceXmldbUri.lastSegment());
    }

    @Override
    public Object authenticate(String username, String password) {
        if (LOG.isDebugEnabled()) LOG.debug("Authenticating user " + username + " for " + resourceXmldbUri);
        if (username == null) {
            return null;
        }
        if (subject != null) {
            if (LOG.isDebugEnabled()) LOG.debug("User was already authenticated.");
            return AUTHENTICATED;
        }
        subject = existResource.authenticate(username, password);
        if (subject == null) {
            if (LOG.isDebugEnabled()) LOG.debug("User could not be authenticated.");
            return null;
        }
        Subject guest = brokerPool.getSecurityManager().getGuestSubject();
        if (guest.equals(subject)) {
            LOG.error("The user " + guest.getName() + " is prohibited from logging in through WebDAV.");
            return null;
        }
        existResource.initMetadata();
        if (LOG.isDebugEnabled()) LOG.debug("User '" + subject.getName() + "' has been authenticated.");
        return AUTHENTICATED;
    }

    @Override
    public boolean authorise(Request request, Method method, Auth auth) {
        LOG.info(method.toString() + " " + resourceXmldbUri + " (write=" + method.isWrite + ")");
        if (auth == null) {
            if (LOG.isDebugEnabled()) LOG.debug("User hasn't been authenticated.");
            return false;
        }
        String userName = auth.getUser();
        Object tag = auth.getTag();
        String authURI = auth.getUri();
        if (tag == null) {
            if (LOG.isDebugEnabled()) LOG.debug("No tag, user " + userName + " not authenticated");
            return false;
        } else if (tag instanceof String) {
            String value = (String) tag;
            if (AUTHENTICATED.equals(value)) {
            } else {
                if (LOG.isDebugEnabled()) LOG.debug("Authentication tag contains wrong value, user " + userName + " is not authenticated");
                return false;
            }
        }
        if (method.isWrite) {
            if (!existResource.writeAllowed) {
                if (LOG.isDebugEnabled()) LOG.debug("User " + userName + " is NOT authorized to write resource, abort.");
                return false;
            }
        } else {
            if (!existResource.readAllowed) {
                if (LOG.isDebugEnabled()) LOG.debug("User " + userName + " is NOT authorized to read resource, abort.");
                return false;
            }
        }
        if (auth.getUri() == null) {
            if (LOG.isTraceEnabled()) LOG.trace("URI is null");
        }
        String action = method.isWrite ? "write" : "read";
        if (LOG.isDebugEnabled()) LOG.debug("User " + userName + " is authorized to " + action + " resource " + resourceXmldbUri.toString());
        return true;
    }

    @Override
    public String getRealm() {
        return REALM;
    }

    @Override
    public Date getModifiedDate() {
        Date modifiedDate = null;
        Long time = existResource.getLastModified();
        if (time != null) {
            modifiedDate = new Date(time);
        }
        return modifiedDate;
    }

    @Override
    public String checkRedirect(Request request) {
        return null;
    }
}
