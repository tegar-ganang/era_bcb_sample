package net.sf.webwarp.util.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * RessourceFilter used for theme supoprt.
 * 
 * @author mos
 * @author aul
 */
public class ResourceFilter implements Filter {

    private static final Logger log = Logger.getLogger(ResourceFilter.class);

    public static final String THEME_NAME_KEY = "theme-name";

    public static final String PATH_KEY = "path";

    static final String DEFAULT_PATH = "/orca/resources";

    static final String DEFAULT_THEME = "/defaulttheme";

    private static final Map<String, String> mimeTypes = new HashMap<String, String>();

    ;

    private String themeName = DEFAULT_THEME;

    private String path = DEFAULT_PATH;

    static {
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("jpe", "image/jpeg");
        mimeTypes.put("bmp", "image/bmp");
        mimeTypes.put("ico", "image/x-icon");
        mimeTypes.put("pic", "image/pict");
        mimeTypes.put("js", "text/javascript");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("tiff", "image/tiff");
    }

    public ResourceFilter() {
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            log.fatal("not a http request");
            return;
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        int pathStartIdx = 0;
        String resourceName = null;
        pathStartIdx = uri.indexOf(path);
        if (pathStartIdx <= -1) {
            log.fatal("the url pattern must match: " + path + " found uri: " + uri);
            return;
        }
        resourceName = uri.substring(pathStartIdx + path.length());
        int suffixIdx = uri.lastIndexOf('.');
        if (suffixIdx <= -1) {
            log.fatal("no file suffix found for resource: " + uri);
            return;
        }
        String suffix = uri.substring(suffixIdx + 1).toLowerCase();
        String mimeType = (String) mimeTypes.get(suffix);
        if (mimeType == null) {
            log.fatal("no mimeType found for resource: " + uri);
            log.fatal("valid mimeTypes are: " + mimeTypes.keySet());
            return;
        }
        String themeName = getThemeName();
        if (themeName == null) {
            themeName = this.themeName;
        }
        if (!themeName.startsWith("/")) {
            themeName = "/" + themeName;
        }
        InputStream is = null;
        is = ResourceFilter.class.getResourceAsStream(themeName + resourceName);
        if (is != null) {
            IOUtils.copy(is, response.getOutputStream());
            response.setContentType(mimeType);
            response.flushBuffer();
            IOUtils.closeQuietly(response.getOutputStream());
            IOUtils.closeQuietly(is);
        } else {
            log.fatal("error loading resource: " + resourceName);
        }
    }

    protected String getThemeName() {
        return null;
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        if (StringUtils.isNotEmpty(filterConfig.getInitParameter(THEME_NAME_KEY))) {
            themeName = filterConfig.getInitParameter(THEME_NAME_KEY);
        }
        if (StringUtils.isNotEmpty(filterConfig.getInitParameter(PATH_KEY))) {
            path = filterConfig.getInitParameter(PATH_KEY);
        }
    }
}
