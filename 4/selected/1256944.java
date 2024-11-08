package com.armatiek.infofuze.web.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import net.iharder.Base64;
import net.sf.saxon.Configuration;
import net.sf.saxon.trans.XPathException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.armatiek.infofuze.config.Config;
import com.armatiek.infofuze.config.Definitions;
import com.armatiek.infofuze.config.Definitions.TransformMode;
import com.armatiek.infofuze.error.InfofuzeException;
import com.armatiek.infofuze.resolver.SourceURIResolver;
import com.armatiek.infofuze.source.DataSourceIf;
import com.armatiek.infofuze.source.NullSource;
import com.armatiek.infofuze.transformer.Transformer;

/**
 * 
 * 
 * @author Maarten Kroon
 */
public abstract class AbstractServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected static final Logger logger = LoggerFactory.getLogger(AbstractServlet.class);

    protected Configuration configuration;

    protected boolean isDevelopmentMode;

    protected File xslBase;

    public void init() throws ServletException {
        super.init();
        try {
            isDevelopmentMode = Config.getInstance().isDevelopmentMode();
            configuration = new Configuration();
            configuration.setURIResolver(new SourceURIResolver());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.Attribute());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.AttributeNames());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.AuthType());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.CharacterEncoding());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.ContentLength());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.ContentType());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.ContextPath());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.Cookies());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.DateHeader());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.Header());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.HeaderNames());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.Headers());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.IntHeader());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.IsRequestedSessionIdFromCookie());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.IsRequestedSessionIdFromUrl());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.IsRequestedSessionIdValid());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.IsSecure());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.IsUserInRole());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.Method());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.Parameter());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.ParameterNames());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.ParameterValues());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.PathInfo());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.PathTranslated());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.Protocol());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.QueryString());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.RemoteAddr());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.RemoteHost());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.RemoteUser());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.RemoveAttribute());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.RequestedSessionId());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.RequestURI());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.RequestURL());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.Scheme());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.ServerName());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.ServerPort());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.ServletPath());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.SetAttribute());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.SetCharacterEncoding());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.request.UserAgent());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.AddCookie());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.AddDateHeader());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.AddHeader());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.AddIntHeader());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.BufferSize());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.CharacterEncoding());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.ContainsHeader());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.EncodeRedirectURL());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.EncodeURL());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.FlushBuffer());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.IsCommitted());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.Reset());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.SendError());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.SendRedirect());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.SetBufferSize());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.SetContentLength());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.SetContentType());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.SetDateHeader());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.SetHeader());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.SetIntHeader());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.response.SetStatus());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.session.Attribute());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.session.AttributeNames());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.session.CreationTime());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.session.Id());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.session.Invalidate());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.session.IsNew());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.session.LastAccessedTime());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.session.MaxInactiveInterval());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.session.RemoveAttribute());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.session.SetAttribute());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.session.SetMaxInactiveInterval());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.solr.Commit());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.solr.DeleteById());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.solr.DeleteByQuery());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.solr.Optimize());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.solr.Rollback());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.solr.Update());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.base64.Base64Encode());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.base64.Base64Decode());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.digest.ShaDigest());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.language.IdentifyLanguage());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.image.ScaleImage());
            configuration.registerExtensionFunction(new com.armatiek.infofuze.xslt.functions.ldap.EscapeFilter());
            configuration.setXIncludeAware(true);
            String xslPath = getInitParameter(Definitions.SERVLET_INIT_PARAMNAME_XSLPATH);
            if (StringUtils.isBlank(xslPath)) {
                String msg = "Init parameter \"" + Definitions.SERVLET_INIT_PARAMNAME_XSLPATH + "\" of servlet \"" + getServletName() + "\" not specified.";
                throw new ServletException(msg);
            }
            xslBase = new File(Config.getInstance().getHomeDir().getAbsolutePath() + File.separator + Definitions.DIRNAME_XSLT + File.separator + Definitions.DIRNAME_WEBAPP + File.separator + xslPath);
            if (!xslBase.isDirectory()) {
                throw new ServletException("XSL base directory \"" + xslBase.getAbsolutePath() + "\" not found");
            }
        } catch (XPathException xe) {
            logger.error(xe.getMessage());
            throw new ServletException(xe);
        } catch (ServletException se) {
            logger.error(se.getMessage());
            throw se;
        }
    }

    @SuppressWarnings("unchecked")
    protected void processTransformAction(HttpServletRequest request, HttpServletResponse response, String action) throws Exception {
        File transformationFile = null;
        String tr = request.getParameter(Definitions.REQUEST_PARAMNAME_XSLT);
        if (StringUtils.isNotBlank(tr)) {
            transformationFile = new File(xslBase, tr);
            if (!transformationFile.isFile()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter \"" + Definitions.REQUEST_PARAMNAME_XSLT + "\" " + "with value \"" + tr + "\" refers to non existing file");
                return;
            }
        }
        StreamResult result;
        ByteArrayOutputStream baos = null;
        if (isDevelopmentMode) {
            baos = new ByteArrayOutputStream();
            if (StringUtils.equals(action, "get")) {
                result = new StreamResult(new Base64.OutputStream(baos, Base64.DECODE));
            } else {
                result = new StreamResult(baos);
            }
        } else {
            if (StringUtils.equals(action, "get")) {
                result = new StreamResult(new Base64.OutputStream(response.getOutputStream(), Base64.DECODE));
            } else {
                result = new StreamResult(response.getOutputStream());
            }
        }
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.putAll(request.getParameterMap());
        params.put("{" + Definitions.CONFIGURATION_NAMESPACE + "}configuration", configuration);
        params.put("{" + Definitions.REQUEST_NAMESPACE + "}request", request);
        params.put("{" + Definitions.RESPONSE_NAMESPACE + "}response", response);
        params.put("{" + Definitions.SESSION_NAMESPACE + "}session", request.getSession());
        params.put("{" + Definitions.INFOFUZE_NAMESPACE + "}development-mode", new Boolean(Config.getInstance().isDevelopmentMode()));
        Transformer transformer = new Transformer();
        transformer.setTransformationFile(transformationFile);
        transformer.setParams(params);
        transformer.setTransformMode(TransformMode.NORMAL);
        transformer.setConfiguration(configuration);
        transformer.setErrorListener(new TransformationErrorListener(response));
        transformer.setLogInfo(false);
        String method = transformer.getOutputProperties().getProperty(OutputKeys.METHOD, "xml");
        String contentType;
        if (method.endsWith("html")) {
            contentType = Definitions.MIMETYPE_HTML;
        } else if (method.equals("xml")) {
            contentType = Definitions.MIMETYPE_XML;
        } else {
            contentType = Definitions.MIMETYPE_TEXTPLAIN;
        }
        String encoding = transformer.getOutputProperties().getProperty(OutputKeys.ENCODING, "UTF-8");
        response.setContentType(contentType + ";charset=" + encoding);
        DataSourceIf dataSource = new NullSource();
        transformer.transform((Source) dataSource, result);
        if (isDevelopmentMode) {
            IOUtils.copy(new ByteArrayInputStream(baos.toByteArray()), response.getOutputStream());
        }
    }

    protected void processClearCacheAction(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!isDevelopmentMode) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        Transformer.clearCache();
        response.setContentType("text/plain");
        response.getWriter().write("Templates cache cleared");
    }

    protected void processKeepAliveAction(HttpServletRequest request, HttpServletResponse response) throws Exception {
        int interval = request.getSession().getMaxInactiveInterval() - 30;
        response.setHeader("Refresh", Integer.toString(interval));
        response.getWriter().println("Keep alive");
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            executeRequest(request, response);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            if (!isDevelopmentMode && !response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void executeRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String action = request.getParameter(Definitions.REQUEST_PARAMNAME_ACTION);
        if (StringUtils.isBlank(action) || action.equals("transform") || action.equals("get")) {
            processTransformAction(request, response, action);
        } else if (action.equals("clearcache")) {
            processClearCacheAction(request, response);
        } else if (action.equals("keepalive")) {
            processKeepAliveAction(request, response);
        } else {
            throw new InfofuzeException("Action \"" + action + "\" not supported");
        }
    }
}
