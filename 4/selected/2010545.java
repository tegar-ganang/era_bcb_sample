package com.migniot.streamy.amfplugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
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
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * A player delayed filter.
 */
public class AMFPlayerFilter implements Filter {

    /**
	 * The logger.
	 */
    private static Logger LOGGER = Logger.getLogger(AMFPlayerFilter.class);

    public static Pattern FILTER = Pattern.compile("^.*/SoundEffectExample[^/]*.swf$");

    /**
	 * The delayed URLs.
	 */
    private Set<String> delayed;

    /**
	 * {@inheritDoc}
	 */
    public void init(FilterConfig config) throws ServletException {
        this.delayed = new HashSet<String>();
    }

    /**
	 * {@inheritDoc}
	 */
    public void destroy() {
    }

    /**
	 * {@inheritDoc}
	 */
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        Matcher matcher = FILTER.matcher(((HttpServletRequest) req).getRequestURI());
        if (matcher.matches()) {
            delay((HttpServletRequest) req, (HttpServletResponse) resp, chain);
        } else {
            chain.doFilter(req, resp);
        }
    }

    /**
	 * Delay player loading.
	 *
	 * @param request
	 *            The request
	 * @param response
	 *            The response
	 * @param chain
	 *            The servlet chain
	 * @throws ServletException
	 *             When chain fails
	 * @throws IOException
	 *             When chain fails
	 */
    private void delay(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String url = request.getRequestURL().toString();
        if (delayed.contains(url)) {
            delayed.remove(url);
            LOGGER.info(MessageFormat.format("Loading delayed resource at url = [{0}]", url));
            chain.doFilter(request, response);
        } else {
            LOGGER.info("Returning resource = [LoaderApplication.swf]");
            InputStream input = null;
            OutputStream output = null;
            try {
                input = getClass().getResourceAsStream("LoaderApplication.swf");
                output = response.getOutputStream();
                delayed.add(url);
                response.setHeader("Cache-Control", "no-cache");
                IOUtils.copy(input, output);
            } finally {
                IOUtils.closeQuietly(output);
                IOUtils.closeQuietly(input);
            }
        }
    }
}
