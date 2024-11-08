package de.objectcode.openk.soa.webapps.proxy.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class BaseProxyServlet extends HttpServlet {

    private static final Log LOG = LogFactory.getLog(BaseProxyServlet.class);

    private static final long serialVersionUID = 1L;

    private final String[] REQUEST_HEADER_KEYS = new String[] { "User-Agent", "Accept", "Accept-Language", "Accept-Encoding", "Accept-Charset", "Referer", "Content-Type" };

    TargetInvoker targetInvoker;

    String basePath;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        targetInvoker = new TargetInvoker(getTargetParams());
        basePath = getBasePath();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println(">>>>>>>>  " + targetInvoker + " " + Thread.currentThread());
        try {
            HttpState httpState = getHttpState(req);
            Map<String, String> requestHeaders = new HashMap<String, String>();
            for (String headerKey : REQUEST_HEADER_KEYS) {
                String value = req.getHeader(headerKey);
                if (value != null) {
                    requestHeaders.put(headerKey, value);
                }
            }
            TargetResult result = targetInvoker.performGet(httpState, req.getPathInfo(), req.getQueryString(), requestHeaders);
            for (org.apache.commons.httpclient.Cookie resultCookie : httpState.getCookies()) {
                Cookie cookie = new Cookie(resultCookie.getName(), resultCookie.getValue());
                if (resultCookie.getPath() == null || !resultCookie.getPath().startsWith(basePath)) {
                    cookie.setPath(basePath);
                } else {
                    cookie.setPath(resultCookie.getPath());
                }
                cookie.setSecure(resultCookie.getSecure());
                resp.addCookie(cookie);
            }
            handleResult(result, resp);
        } catch (IOException e) {
            LOG.error("IOException", e);
            throw e;
        } catch (Exception e) {
            LOG.error("Exception", e);
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println(">>>>>>>>  " + targetInvoker + " " + Thread.currentThread());
        try {
            HttpState httpState = getHttpState(req);
            Map<String, String> requestHeaders = new HashMap<String, String>();
            for (String headerKey : REQUEST_HEADER_KEYS) {
                String value = req.getHeader(headerKey);
                if (value != null) {
                    requestHeaders.put(headerKey, value);
                }
            }
            TargetResult result = targetInvoker.performPost(httpState, req.getPathInfo(), requestHeaders, req.getInputStream());
            for (org.apache.commons.httpclient.Cookie resultCookie : httpState.getCookies()) {
                Cookie cookie = new Cookie(resultCookie.getName(), resultCookie.getValue());
                if (resultCookie.getPath() == null || !resultCookie.getPath().startsWith(basePath)) {
                    cookie.setPath(basePath);
                } else {
                    cookie.setPath(resultCookie.getPath());
                }
                cookie.setSecure(resultCookie.getSecure());
                resp.addCookie(cookie);
            }
            handleResult(result, resp);
        } catch (IOException e) {
            LOG.error("IOException", e);
            throw e;
        } catch (Exception e) {
            LOG.error("Exception", e);
            throw new ServletException(e);
        }
    }

    protected void handleResult(TargetResult result, HttpServletResponse response) throws IOException {
        if ((result.getStatus() == HttpServletResponse.SC_MOVED_TEMPORARILY || result.getStatus() == HttpServletResponse.SC_MOVED_PERMANENTLY)) {
            URL url = new URL(result.getHeaders().get("Location"));
            response.setContentType("text/html");
            ServletOutputStream out = response.getOutputStream();
            out.println("<html>");
            out.println("<head>");
            out.println("</head>");
            out.println("<body>");
            out.println("<center>Automatic redirect to: <a href=\"" + url.getPath() + "\">" + url.getPath() + "</a></center>");
            out.println("<center>" + result.getHeaders().get("Location") + "</center>");
            out.println("</body>");
            out.println("</html>");
        } else {
            response.setStatus(result.getStatus());
            for (Map.Entry<String, String> entry : result.getHeaders().entrySet()) {
                response.setHeader(entry.getKey(), entry.getValue());
            }
            InputStream in = result.getContent();
            OutputStream out = response.getOutputStream();
            byte[] buffer = new byte[8192];
            int readed;
            while ((readed = in.read(buffer)) > 0) {
                out.write(buffer, 0, readed);
            }
            in.close();
            out.flush();
            out.close();
        }
    }

    protected HttpState getHttpState(HttpServletRequest request) {
        HttpState state = new HttpState();
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                System.out.println(">>>>  req: " + cookie.getName() + " " + cookie.getValue());
                state.addCookie(targetInvoker.createCookie(cookie.getName(), cookie.getValue()));
            }
        }
        return state;
    }

    protected abstract TargetParams getTargetParams();

    protected abstract String getBasePath();
}
