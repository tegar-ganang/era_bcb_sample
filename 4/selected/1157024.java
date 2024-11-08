package info.javadev.jSEO.filter;

import info.javadev.jSEO.urlMapper.IURLMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class RequestWrapper extends HttpServletRequestWrapper {

    private ByteArrayInputStream bais;

    private ByteArrayOutputStream baos;

    private ServletInputStream sis;

    private byte[] buffer;

    private HttpServletRequest req;

    private Map parameterMap;

    private IURLMapper mapper;

    public RequestWrapper(HttpServletRequest req, IURLMapper mapper) throws IOException {
        super(req);
        this.req = req;
        parameterMap = new HashMap();
        this.mapper = mapper;
        initializeParameterMap();
        InputStream is = req.getInputStream();
        baos = new ByteArrayOutputStream();
        byte buf[] = new byte[1024];
        int letti;
        while ((letti = is.read(buf)) > 0) baos.write(buf, 0, letti);
        buffer = baos.toByteArray();
    }

    private void initializeParameterMap() {
        if (req.getParameterMap() != null) parameterMap.putAll(req.getParameterMap());
        String queryStr = getQueryString();
        while (queryStr.indexOf('=') != -1) {
            String key = queryStr.substring(0, queryStr.indexOf('='));
            String value = "";
            if (queryStr.indexOf('&') != -1) {
                value = queryStr.substring(queryStr.indexOf('=') + 1, queryStr.indexOf('&'));
                queryStr = queryStr.substring(queryStr.indexOf('&') + 1);
            } else {
                value = queryStr.substring(queryStr.indexOf('=') + 1);
                queryStr = "";
            }
            parameterMap.put(key, value);
        }
    }

    public ServletInputStream getInputStream() {
        try {
            bais = new ByteArrayInputStream(buffer);
            sis = new ServletInputStreamImpl(bais);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return sis;
    }

    public String getRequestURI() {
        String actualURI = req.getRequestURI();
        String servletPath = req.getServletPath();
        return actualURI.substring(0, actualURI.indexOf(servletPath) + servletPath.length());
    }

    public String getQueryString() {
        if (req.getQueryString() != null) return req.getQueryString();
        String dynamicURL = mapper.generateDynamicURL(req.getRequestURI(), req.getServletPath());
        return dynamicURL.substring(dynamicURL.indexOf('?') + 1);
    }

    public Map getParameterMap() {
        return parameterMap;
    }

    public String[] getParameterValues(String name) {
        if (parameterMap.containsKey(name)) {
            Object o = parameterMap.get(name);
            if (o instanceof String) return (new String[] { (String) o }); else if (o instanceof String[]) {
                return (String[]) o;
            }
        }
        return null;
    }

    public String getParameter(String name) {
        if (parameterMap.containsKey(name)) {
            Object o = parameterMap.get(name);
            if (o instanceof String[]) return ((String[]) o)[0]; else if (o instanceof String) {
                return (String) o;
            }
        }
        return null;
    }
}
