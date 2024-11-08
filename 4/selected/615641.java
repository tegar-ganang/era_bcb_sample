package net.disy.legato.tools.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.disy.legato.tools.util.DefaultMapPersister;
import net.disy.legato.tools.util.PropertiesMapPersister;
import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.commons.collections15.map.UnmodifiableOrderedMap;
import org.apache.commons.io.IOUtils;

public class ResourceMappingFilter implements Filter {

    private static final String FILTER_PATH_PARAMETER_NAME = "filterPath";

    private static final String MAPPING_PROPERTIES_INIT_PARAMETER_NAME = "mappingProperties";

    private Map<String, String> mapping = Collections.emptyMap();

    /**
	 * Filter path.
	 */
    private String filterPath = "";

    /**
	 * Returns filter path.
	 * 
	 * @return path of the filter.
	 */
    public String getFilterPath() {
        return filterPath;
    }

    public void setFilterPath(final String filterPath) {
        this.filterPath = filterPath;
    }

    @Override
    public void init(final FilterConfig config) throws ServletException {
        final String filterPath = config.getInitParameter(FILTER_PATH_PARAMETER_NAME);
        if (filterPath == null) {
            throw new ServletException("Required init parameter [prefix] is not specified.");
        }
        setFilterPath(filterPath);
        final String configuredMappingPropertiesResourceName = config.getInitParameter(MAPPING_PROPERTIES_INIT_PARAMETER_NAME);
        final String mappingPropertiesResourceName = (configuredMappingPropertiesResourceName != null ? configuredMappingPropertiesResourceName : getDefaultMappingPropertiesResourceName());
        final PropertiesMapPersister propertiesMapPersister = new DefaultMapPersister();
        final OrderedMap<String, String> mapping = new ListOrderedMap<String, String>();
        InputStream is = null;
        try {
            is = getClass().getResourceAsStream(mappingPropertiesResourceName);
            if (is == null) {
                throw new ServletException("Could not load mapping properties from the resource [" + mappingPropertiesResourceName + "] because it does not exist.");
            }
            propertiesMapPersister.load(mapping, is);
        } catch (final IOException ioex) {
            throw new ServletException("Could not load mapping properties from the resource [" + mappingPropertiesResourceName + "].", ioex);
        } finally {
            IOUtils.closeQuietly(is);
        }
        this.mapping = UnmodifiableOrderedMap.decorate(mapping);
    }

    @Override
    public void destroy() {
    }

    protected String getDefaultMappingPropertiesResourceName() {
        return "/" + getClass().getName().replace('.', '/') + ".properties";
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            final HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            final String contextPath = httpServletRequest.getContextPath() + "/";
            final String requestURI = httpServletRequest.getRequestURI();
            if (requestURI.startsWith(contextPath)) {
                final String filterURI = requestURI.substring(contextPath.length());
                final String filterPath = getFilterPath();
                if (filterURI.startsWith(filterPath)) {
                    final String targetURI = filterURI.substring(filterPath.length());
                    for (final Entry<String, String> entry : mapping.entrySet()) {
                        final String key = entry.getKey();
                        final String value = entry.getValue();
                        if (targetURI.startsWith(key) && (targetURI.length() > key.length())) {
                            final String resourceName = value + targetURI.substring(key.length());
                            InputStream is = null;
                            try {
                                is = getClass().getResourceAsStream("/" + resourceName);
                                if (is != null) {
                                    IOUtils.copy(is, httpServletResponse.getOutputStream());
                                    httpServletResponse.flushBuffer();
                                    break;
                                } else {
                                    httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                                    break;
                                }
                            } catch (final IOException ioex) {
                                throw new ServletException("Error serving resource [" + resourceName + "].", ioex);
                            } finally {
                                IOUtils.closeQuietly(is);
                            }
                        }
                    }
                } else {
                    chain.doFilter(request, response);
                }
            } else {
                chain.doFilter(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
