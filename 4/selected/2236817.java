package au.edu.diasb.annotation.danno;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import au.edu.diasb.annotation.danno.model.AnnoteaObject;
import au.edu.diasb.annotation.danno.model.RDFContainer;
import au.edu.diasb.annotation.danno.model.RDFObject;
import au.edu.diasb.chico.config.PropertyTree;
import au.edu.diasb.chico.mvc.AuthenticationContext;
import au.edu.diasb.chico.mvc.DefaultAuthenticationContext;
import au.edu.diasb.chico.mvc.RequestFailureException;
import au.edu.diasb.danno.constants.AnnoteaSchemaConstants;

/**
 * This class combines Danno and Dannotate access policies into one object
 * to simplify configuration.
 * <p>
 * The default access policy implementation checks that the request
 * has read, write and/or admin rights, and (in the case of PUT and
 * delete) that the requester is the 'owner' of the annotation. 
 * <p>
 * 
 * @author scrawley
 */
public class DefaultAccessPolicy implements DannoAccessPolicy, DannotateAccessPolicy, InitializingBean {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(DefaultAccessPolicy.class);

    private String[] adminAuthorities = { "ROLE_ADMIN" };

    private String[] readAuthorities = { "ROLE_USER" };

    private String[] writeAuthorities = { "ROLE_ANNOTATOR" };

    private String[] oaiAuthorities = { "ROLE_OAI" };

    private String[] useAuthorities = { "ROLE_USER" };

    private AuthenticationContext ac;

    private DannoIdentityProvider ip;

    private boolean webAdmin = false;

    private boolean checkOwner = true;

    private boolean userSettableName = false;

    private boolean adminSettableName = false;

    private boolean adminCanModerate = true;

    public DefaultAccessPolicy() {
        super();
    }

    public DefaultAccessPolicy(AuthenticationContext ac) {
        super();
        this.ac = ac;
    }

    @Override
    public void afterPropertiesSet() {
        if (ac == null) {
            ac = new DefaultAuthenticationContext();
        }
        if (ip == null) {
            ip = new DefaultDannoIdentityProvider(ac);
        }
    }

    /**
     * The requestor can change names if this is allowed globally or
     * if admin authority is granted.
     */
    public final boolean canChangeNames(HttpServletRequest request) {
        return userSettableName || adminSettableName && ac.hasAuthority(request, adminAuthorities);
    }

    @Override
    public void checkUse(HttpServletRequest request) throws RequestFailureException {
        ac.checkAuthority(request, useAuthorities);
    }

    @Override
    public void checkCreateOrEditAnnotations(HttpServletRequest request) throws RequestFailureException {
        ac.checkAuthority(request, writeAuthorities);
    }

    @Override
    public void checkCreateAnnotation(HttpServletRequest request, String schemaName) throws RequestFailureException {
        ac.checkAuthority(request, writeAuthorities);
    }

    @Override
    public void checkEditAnnotation(HttpServletRequest request, String schemaName, PropertyTree pt) throws RequestFailureException {
        ac.checkAuthority(request, writeAuthorities);
        checkObjectOwner(request, pt);
        checkObjectGroup(request, pt);
    }

    @Override
    public void checkDelete(HttpServletRequest request, PropertyTree pt) throws RequestFailureException {
        ac.checkAuthority(request, writeAuthorities);
        checkObjectOwner(request, pt);
        checkObjectGroup(request, pt);
    }

    @Override
    public void checkWebAdmin(HttpServletRequest request) throws RequestFailureException {
        if (!webAdmin) {
            throw new RequestFailureException(HttpServletResponse.SC_BAD_REQUEST, "Danno admin requests are disabled");
        }
        ac.checkAuthority(request, adminAuthorities);
    }

    @Override
    public void checkCreate(HttpServletRequest request, RDFObject res) throws RequestFailureException {
        ac.checkAuthority(request, writeAuthorities);
        checkCreateThis(request, res, res.getURI());
    }

    @Override
    public void checkDelete(HttpServletRequest request, AnnoteaObject obj) throws RequestFailureException {
        ac.checkAuthority(request, writeAuthorities);
        checkUpdateOrDeleteThis(request, obj);
    }

    @Override
    public void checkRead(HttpServletRequest request, RDFContainer res) throws RequestFailureException {
        ac.checkAuthority(request, readAuthorities);
        filterReadByGroup(request, res);
    }

    @Override
    public void checkUpdate(HttpServletRequest request, AnnoteaObject obj) throws RequestFailureException {
        ac.checkAuthority(request, writeAuthorities);
        checkUpdateOrDeleteThis(request, obj);
    }

    private void checkUpdateOrDeleteThis(HttpServletRequest request, AnnoteaObject obj) {
        if (!checkOwner) {
            return;
        }
        PropertyTree pt = obj.getPropertyTree(obj.getURI());
        checkObjectOwner(request, pt);
        checkObjectGroup(request, pt);
    }

    private void checkObjectOwner(HttpServletRequest request, PropertyTree pt) {
        if (!checkOwner || pt == null) {
            return;
        }
        String ownerId = pt.getString(AnnoteaSchemaConstants.DANNO_OWNER_ID_PROPERTY);
        if (ownerId == null) {
            return;
        }
        if (adminCanModerate && ac.hasAuthority(request, adminAuthorities)) {
            return;
        }
        String userUri = ip.obtainUserURI(request);
        if (userUri == null) {
            throw new AccessDeniedException("Ooops ... cannot establish your identity");
        } else if (!ownerId.equals(userUri)) {
            throw new AccessDeniedException("You do not own this object");
        }
    }

    private void checkObjectGroup(HttpServletRequest request, PropertyTree pt) {
        if (pt == null) {
            return;
        }
        if (adminCanModerate && ac.hasAuthority(request, adminAuthorities)) {
            return;
        }
        String group = pt.getString(AnnoteaSchemaConstants.DANNO_GROUP_PROPERTY);
        List<String> allowedGroups = new ArrayList<String>();
        String root = pt.getString(AnnoteaSchemaConstants.ROOT_PROPERTY);
        if (root == null) {
            allowedGroups = ip.obtainCreateAccessGroups(request);
        } else {
            allowedGroups = ip.obtainReplyAccessGroups(request);
        }
        if (!(group == null || group.isEmpty()) && !allowedGroups.contains(group)) {
            throw new AccessDeniedException("You must be a member of '" + group + "' to perform this action.");
        }
    }

    private void checkCreateThis(HttpServletRequest request, RDFContainer res, String id) throws AccessDeniedException {
        checkObjectGroup(request, res.getPropertyTree(id));
    }

    private void filterReadByGroup(HttpServletRequest request, RDFContainer res) throws AccessDeniedException {
        List<String> groups = ip.obtainReadAccessGroups(request);
        if (res == null) {
            return;
        }
        for (PropertyTree pt : res.toPropertyTrees()) {
            String group = pt.getString(AnnoteaSchemaConstants.DANNO_GROUP_PROPERTY);
            if (!(group == null || group.isEmpty()) && !groups.contains(group)) {
                res.removeResource(pt.getString(AnnoteaSchemaConstants.BODY_PROPERTY));
                res.removeResource(pt.getString("id"));
            }
        }
    }

    /**
     * Set the name of the admin authority; e.g. "ROLE_ADMIN".
     * @param adminAuthorities
     */
    public void setAdminAuthorities(String adminAuthorities) {
        this.adminAuthorities = StringUtils.commaDelimitedListToStringArray(adminAuthorities);
    }

    /**
     * Set the names of the read authorities; e.g. "ROLE_USER".
     * @param readAuthorities
     */
    public void setReadAuthorities(String readAuthorities) {
        this.readAuthorities = StringUtils.commaDelimitedListToStringArray(readAuthorities);
    }

    /**
     * Set the names of the write authorities; e.g. "ROLE_ANNOTATOR".
     * @param writeAuthorities
     */
    public void setWriteAuthorities(String writeAuthorities) {
        this.writeAuthorities = StringUtils.commaDelimitedListToStringArray(writeAuthorities);
    }

    /**
     * Set the names of the OAI authorities; e.g. "ROLE_OAI".
     * @param oaiAuthorities
     */
    public void setOaiAuthorities(String oaiAuthorities) {
        this.oaiAuthorities = StringUtils.commaDelimitedListToStringArray(oaiAuthorities);
    }

    public AuthenticationContext getAuthenticationContext() {
        return ac;
    }

    public void setAuthenticationContext(AuthenticationContext ac) {
        this.ac = ac;
    }

    public DannoIdentityProvider getIdentityProvider() {
        return ip;
    }

    public void setIdentityProvider(DannoIdentityProvider ip) {
        this.ip = ip;
    }

    public boolean isWebAdmin() {
        return webAdmin;
    }

    /**
     * This property enables Danno's web admin requests. 
     * It defaults to {@literal false}.
     * 
     * @param webAdmin
     */
    public void setWebAdmin(boolean webAdmin) {
        this.webAdmin = webAdmin;
    }

    public boolean isCheckOwner() {
        return checkOwner;
    }

    /**
     * This property determines if we should check the ownership of
     * the target annotation before editing or deleting it.  It defaults
     * to {@literal true}.
     * 
     * @param checkOwner 
     */
    public void setCheckOwner(boolean checkOwner) {
        this.checkOwner = checkOwner;
    }

    /**
     * This property determines if we allow the user to set an
     * annotation's creator or ownerId that is different to what 
     * the request credentials say. 
     * It defaults to {@literal false}.
     * 
     * @param userSettableName 
     */
    public void setUserSettableName(boolean userSettableName) {
        this.userSettableName = userSettableName;
    }

    /**
     * This property determines if we allow admins to set an
     * annotation's creator or ownerId. 
     * It defaults to {@literal false}.
     * 
     * @param adminSettableName 
     */
    public void setAdminSettableName(boolean adminSettableName) {
        this.adminSettableName = adminSettableName;
    }

    /**
     * This property determines if we allow admins to modify
     * or delete annotations owned by someone else.
     * 
     * It defaults to {@literal true}.
     * 
     * @param adminCanModerate 
     */
    public void setAdminCanModerate(boolean adminCanModerate) {
        this.adminCanModerate = adminCanModerate;
    }

    public void setUseAuthorities(String useAuthorities) {
        this.useAuthorities = StringUtils.commaDelimitedListToStringArray(useAuthorities);
    }

    @Override
    public String toString() {
        return "DefaultAccessPolicy{webAdmin=" + webAdmin + ",userSettableName=" + userSettableName + ",checkOwner=" + checkOwner + ",useAuthorities=" + StringUtils.arrayToCommaDelimitedString(useAuthorities) + ",oaiAuthorities=" + StringUtils.arrayToCommaDelimitedString(oaiAuthorities) + ",readAuthorities=" + StringUtils.arrayToCommaDelimitedString(readAuthorities) + ",writeAuthorities=" + StringUtils.arrayToCommaDelimitedString(writeAuthorities) + ",adminAuthorities=" + StringUtils.arrayToCommaDelimitedString(adminAuthorities) + "}";
    }
}
