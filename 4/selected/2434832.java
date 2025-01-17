package org.apache.catalina.ssi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Globals;

/**
 * Filter to process SSI requests within a webpage. Mapped to a content types
 * from within web.xml.
 * 
 * @author David Becker
 * @version $Revision: 531303 $, $Date: 2007-04-23 02:24:01 +0200 (Mon, 23 Apr 2007) $
 * @see org.apache.catalina.ssi.SSIServlet
 */
public class SSIFilter implements Filter {

    protected FilterConfig config = null;

    /** Debug level for this servlet. */
    protected int debug = 0;

    /** Expiration time in seconds for the doc. */
    protected Long expires = null;

    /** virtual path can be webapp-relative */
    protected boolean isVirtualWebappRelative = false;

    /** regex pattern to match when evaluating content types */
    protected Pattern contentTypeRegEx = null;

    /** default pattern for ssi filter content type matching */
    protected Pattern shtmlRegEx = Pattern.compile("text/x-server-parsed-html(;.*)?");

    /**
     * Initialize this servlet.
     * 
     * @exception ServletException
     *                if an error occurs
     */
    public void init(FilterConfig config) throws ServletException {
        this.config = config;
        if (config.getInitParameter("debug") != null) {
            debug = Integer.parseInt(config.getInitParameter("debug"));
        }
        if (config.getInitParameter("contentType") != null) {
            contentTypeRegEx = Pattern.compile(config.getInitParameter("contentType"));
        } else {
            contentTypeRegEx = shtmlRegEx;
        }
        isVirtualWebappRelative = Boolean.parseBoolean(config.getInitParameter("isVirtualWebappRelative"));
        if (config.getInitParameter("expires") != null) expires = Long.valueOf(config.getInitParameter("expires"));
        if (debug > 0) config.getServletContext().log("SSIFilter.init() SSI invoker started with 'debug'=" + debug);
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        req.setAttribute(Globals.SSI_FLAG_ATTR, "true");
        ByteArrayServletOutputStream basos = new ByteArrayServletOutputStream();
        ResponseIncludeWrapper responseIncludeWrapper = new ResponseIncludeWrapper(config.getServletContext(), req, res, basos);
        chain.doFilter(req, responseIncludeWrapper);
        responseIncludeWrapper.flushOutputStreamOrWriter();
        byte[] bytes = basos.toByteArray();
        String contentType = responseIncludeWrapper.getContentType();
        if (contentTypeRegEx.matcher(contentType).matches()) {
            String encoding = res.getCharacterEncoding();
            SSIExternalResolver ssiExternalResolver = new SSIServletExternalResolver(config.getServletContext(), req, res, isVirtualWebappRelative, debug, encoding);
            SSIProcessor ssiProcessor = new SSIProcessor(ssiExternalResolver, debug);
            Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), encoding);
            ByteArrayOutputStream ssiout = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(ssiout, encoding));
            long lastModified = ssiProcessor.process(reader, responseIncludeWrapper.getLastModified(), writer);
            writer.flush();
            bytes = ssiout.toByteArray();
            if (expires != null) {
                res.setDateHeader("expires", (new java.util.Date()).getTime() + expires.longValue() * 1000);
            }
            if (lastModified > 0) {
                res.setDateHeader("last-modified", lastModified);
            }
            res.setContentLength(bytes.length);
            Matcher shtmlMatcher = shtmlRegEx.matcher(responseIncludeWrapper.getContentType());
            if (shtmlMatcher.matches()) {
                String enc = shtmlMatcher.group(1);
                res.setContentType("text/html" + ((enc != null) ? enc : ""));
            }
        }
        OutputStream out = null;
        try {
            out = res.getOutputStream();
        } catch (IllegalStateException e) {
        }
        if (out == null) {
            res.getWriter().write(new String(bytes));
        } else {
            out.write(bytes);
        }
    }

    public void destroy() {
    }
}
