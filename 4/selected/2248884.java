package org.riverock.portlet.login;

import java.io.IOException;
import java.io.Writer;
import java.util.ResourceBundle;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.riverock.common.tools.ServletTools;
import org.riverock.interfaces.sso.a3.AuthSession;
import org.riverock.portlet.tools.ContentTypeTools;
import org.riverock.portlet.tools.RequestTools;
import org.riverock.webmill.container.ContainerConstants;
import org.riverock.webmill.container.tools.PortletService;

/**
 * Author: mill
 * Date: Dec 2, 2002
 * Time: 4:10:28 PM
 * 
 * $Id: LoginPlainPortlet.java,v 1.11 2006/06/28 14:49:16 serg_main Exp $
 */
public final class LoginPlainPortlet implements Portlet {

    private static final Logger log = Logger.getLogger(LoginPlainPortlet.class);

    public LoginPlainPortlet() {
    }

    private PortletConfig portletConfig = null;

    public void init(PortletConfig portletConfig) {
        this.portletConfig = portletConfig;
    }

    public void destroy() {
    }

    public void processAction(final ActionRequest actionRequest, final ActionResponse actionResponse) throws IOException {
        LoginUtils.check(actionRequest, actionResponse);
    }

    public void render(final RenderRequest renderRequest, final RenderResponse renderResponse) throws PortletException, IOException {
        Writer out = null;
        try {
            ContentTypeTools.setContentType(renderResponse, ContentTypeTools.CONTENT_TYPE_UTF8);
            out = renderResponse.getWriter();
            ResourceBundle bundle = portletConfig.getResourceBundle(renderRequest.getLocale());
            if (log.isDebugEnabled()) {
                log.debug("Process input auth data");
            }
            AuthSession auth_ = (AuthSession) renderRequest.getUserPrincipal();
            if (auth_ != null && auth_.checkAccess(renderRequest.getServerName())) {
                if (log.isDebugEnabled()) {
                    log.debug("user " + auth_.getUserLogin() + " is  valid for " + renderRequest.getServerName() + " site");
                }
                out.write("User already logged in.");
                return;
            }
            PortletURL portletUrl = renderResponse.createActionURL();
            portletUrl.setParameter(ContainerConstants.NAME_TYPE_CONTEXT_PARAM, LoginUtils.CTX_TYPE_LOGIN_PLAIN);
            out.write("<form method=\"POST\" action=\"" + portletUrl + "\" >\n");
            String srcURL;
            if (renderRequest.getParameter(LoginUtils.NAME_TOURL_PARAM) != null) {
                srcURL = RequestTools.getString(renderRequest, LoginUtils.NAME_TOURL_PARAM);
            } else {
                srcURL = PortletService.url(ContainerConstants.CTX_TYPE_INDEX, renderRequest, renderResponse);
            }
            srcURL = StringUtils.replace(srcURL, "%3D", "=");
            srcURL = StringUtils.replace(srcURL, "%26", "&");
            out.write(ServletTools.getHiddenItem(LoginUtils.NAME_TOURL_PARAM, srcURL));
            if (log.isDebugEnabled()) {
                log.debug("reqeust parameter  mill.tourl: " + renderRequest.getParameter(LoginUtils.NAME_TOURL_PARAM));
                log.debug("toURL: " + srcURL);
                log.debug("encoded toURL - " + srcURL);
                log.debug("Header string - " + bundle.getString("auth.check.header"));
            }
            out.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"2\" align=\"center\">\n" + "<tr><th>" + bundle.getString("auth.check.header") + "</th></tr>\n" + "<tr><td>" + "<table border=\"0\">" + "<tr><td align=\"right\">" + "&nbsp;" + bundle.getString("auth.check.login") + "</td><td>&nbsp;&nbsp;" + "</td><td>" + "<input type=\"text\" name=\"" + LoginUtils.NAME_USERNAME_PARAM + "\" tabindex=\"1\">" + "</td>" + "<td align=\"left\" valing=\"top\" cellspan=\"2\">" + "<input type=\"submit\" name=\"button\" value=\"" + bundle.getString("auth.check.register") + "\" tabindex=\"3\">" + "</td></tr>\n" + "<tr><td align=\"right\">" + "&nbsp;" + bundle.getString("auth.check.password") + "</td><td>" + "</td><td aling=\"left\">" + "<input type=\"password\" name=\"" + LoginUtils.NAME_PASSWORD_PARAM + "\" value = \"\" tabindex=\"2\">" + "</td></tr>\n" + "</table>" + "</td>" + "</tr>\n" + "</table>\n" + "</form>\n");
        } catch (Throwable e) {
            String es = "Error in render()";
            log.error(es, e);
            throw new PortletException(es, e);
        } finally {
            if (out != null) {
                out.flush();
                out.close();
                out = null;
            }
        }
    }
}
