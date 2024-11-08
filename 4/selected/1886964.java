package net.sf.j2s.ajax;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This class is designed as a Java Servlet container.
 * 
 * @author zhou renjian
 *
 * 2006-10-10
 */
public class SimpleRPCHttpServlet extends HttpServlet {

    private static final long serialVersionUID = -3852449040495666018L;

    protected Set<String> runnables = new HashSet<String>();

    protected long postLimit = 0x1000000;

    protected boolean supportXSS = true;

    protected int xssPartLimit = 128;

    protected long xssLatency = 60 * 1000;

    protected boolean managingPipe = false;

    protected long maxPostLimit() {
        return postLimit;
    }

    /**
	 * Return support cross site script request or not.
	 * 
	 * If cross site script is supported, http log format should be modified
	 * so those requests are not logged.
	 * If Apache httpd server is used, you can modify httpd.conf as following:
	 * <pre>
	 * LogFormat "%h %l %u %t \"%m %U %H\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\"" noquery
	 * ...
	 * CustomLog logs/dummy-host.example.com-access_log noquery
	 * </pre>
	 * Or use SetEnvIf
	 * <pre>
	 * SetEnvIf Request_URI "^/your-url" dontlog
	 * CustomLog logs/dummy-host.example.com-access_log combined env=!dontlog
	 * </pre>
	 * 
	 * @see http://httpd.apache.org/docs/2.0/mod/mod_log_config.html
	 * @see http://httpd.apache.org/docs/1.3/logs.html
	 * 
	 * @return
	 */
    protected boolean supportXSSRequest() {
        return supportXSS;
    }

    protected int maxXSSRequestParts() {
        return xssPartLimit;
    }

    protected long maxXSSRequestLatency() {
        return xssLatency;
    }

    /**
	 * Get SimpleRPCRunnable instance correspond to the given request. 
	 * 
	 * @param request String that is from POST data or GET query  
	 * @return SimpleRPCRunnable instance. If request is bad request or 
	 * specified class name is invalid, null will be returned.
	 */
    protected SimpleRPCRunnable getRunnableByRequest(String request) {
        SimpleSerializable instance = SimpleSerializable.parseInstance(request, new SimpleFilter() {

            public boolean accept(String clazzName) {
                return validateRunnable(clazzName);
            }

            public boolean ignoreDefaultFields() {
                return false;
            }
        });
        if (instance instanceof SimpleRPCRunnable) {
            instance.deserialize(request);
            return (SimpleRPCRunnable) instance;
        }
        return null;
    }

    public void init() throws ServletException {
        String runnableStr = getInitParameter("simple.rpc.runnables");
        if (runnableStr != null) {
            String[] splits = runnableStr.trim().split("\\s*[,;:]\\s*");
            for (int i = 0; i < splits.length; i++) {
                String trim = splits[i].trim();
                if (trim.length() != 0) {
                    runnables.add(trim);
                }
            }
        }
        String postLimitStr = getInitParameter("simple.rpc.post.limit");
        if (postLimitStr != null) {
            try {
                postLimit = Long.parseLong(postLimitStr);
                if (postLimit <= 0) {
                    postLimit = Long.MAX_VALUE;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        String xssSupportStr = getInitParameter("simple.rpc.xss.support");
        if (xssSupportStr != null) {
            supportXSS = "true".equals(xssSupportStr);
        }
        String xssLatencytStr = getInitParameter("simple.rpc.xss.max.latency");
        if (xssLatencytStr != null) {
            try {
                xssLatency = Long.parseLong(xssLatencytStr);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        String xssPartsStr = getInitParameter("simple.rpc.xss.max.parts");
        if (xssPartsStr != null) {
            try {
                xssPartLimit = Integer.parseInt(xssPartsStr);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        String managablePipeStr = getInitParameter("simple.pipe.managable");
        if (managablePipeStr != null) {
            managingPipe = "true".equals(managablePipeStr);
        }
        super.init();
    }

    /**
	 * Validate the given class name.
	 * @param clazzName
	 * @return specified class is valid or not.
	 */
    protected boolean validateRunnable(String clazzName) {
        return runnables.contains(clazzName);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String request = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int read = 0;
        InputStream res = req.getInputStream();
        while (true) {
            try {
                read = res.read(buf);
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                res.close();
                return;
            }
            if (read == -1) {
                break;
            }
            baos.write(buf, 0, read);
            if (baos.size() > maxPostLimit()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Data size reaches the limit of Java2Script Simple RPC!");
                res.close();
                return;
            }
        }
        request = baos.toString();
        res.close();
        SimpleRPCRunnable runnable = getRunnableByRequest(request);
        if (runnable == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setDateHeader("Expires", 0);
        resp.setContentType("text/plain; charset=utf-8");
        PrintWriter writer = resp.getWriter();
        SimpleRPCRunnable clonedRunnable = null;
        try {
            clonedRunnable = (SimpleRPCRunnable) runnable.clone();
        } catch (CloneNotSupportedException e) {
        }
        runnable.ajaxRun();
        final String[] diffs = SimpleRPCUtils.compareDiffs(runnable, clonedRunnable);
        String serialize = runnable.serialize(new SimpleFilter() {

            public boolean accept(String field) {
                for (int i = 0; i < diffs.length; i++) {
                    if (diffs[i].equals(field)) {
                        return true;
                    }
                }
                return false;
            }

            public boolean ignoreDefaultFields() {
                return false;
            }
        });
        writer.write(serialize);
        runnable.ajaxOut();
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String request = req.getQueryString();
        if (request == null || request.length() < 4) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        boolean isScriptReuest = false;
        String requestID = null;
        if (request.charAt(3) == '=') {
            request = req.getParameter("jzz");
            if (request == null || request.trim().length() == 0) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            requestID = req.getParameter("jzn");
            if (requestID != null && requestID.length() != 0) {
                isScriptReuest = true;
                request = prepareScriptRequest(req, resp, requestID, request);
                if (request == null) {
                    return;
                }
            }
        } else {
            request = URLDecoder.decode(request, "UTF-8");
        }
        SimpleRPCRunnable runnable = getRunnableByRequest(request);
        if (runnable == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        SimpleRPCRunnable clonedRunnable = null;
        try {
            clonedRunnable = (SimpleRPCRunnable) runnable.clone();
        } catch (CloneNotSupportedException e) {
        }
        runnable.ajaxRun();
        final String[] diffs = SimpleRPCUtils.compareDiffs(runnable, clonedRunnable);
        String serialize = runnable.serialize(new SimpleFilter() {

            public boolean accept(String field) {
                for (int i = 0; i < diffs.length; i++) {
                    if (diffs[i].equals(field)) {
                        return true;
                    }
                }
                return false;
            }

            public boolean ignoreDefaultFields() {
                return false;
            }
        });
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setDateHeader("Expires", 0);
        if (isScriptReuest) {
            resp.setContentType("text/javascript; charset=utf-8");
            PrintWriter writer = resp.getWriter();
            writer.write("net.sf.j2s.ajax.SimpleRPCRequest.xssNotify(");
            writer.write("\"" + requestID + "\", \"");
            writer.write(serialize.replaceAll("\\\\", "\\\\\\\\").replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n").replaceAll("\"", "\\\\\""));
            writer.write("\");");
            runnable.ajaxOut();
            return;
        }
        resp.setContentType("text/plain; charset=utf-8");
        PrintWriter writer = resp.getWriter();
        writer.write(serialize);
        runnable.ajaxOut();
    }

    private String prepareScriptRequest(HttpServletRequest req, HttpServletResponse resp, String scriptRequestID, String request) throws IOException {
        if (!scriptRequestID.matches("\\d{6,}")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        if (!supportXSSRequest()) {
            resp.setContentType("text/javascript");
            resp.getWriter().write("net.sf.j2s.ajax.SimpleRPCRequest" + ".xssNotify(\"" + scriptRequestID + "\", \"unsupported\");");
            return null;
        }
        String count = req.getParameter("jzp");
        if (count == null || !count.matches("[1-9]\\d{0,2}")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        int partsCount = Integer.parseInt(count);
        if (partsCount == 1) {
            return request;
        }
        String current = req.getParameter("jzc");
        if (current == null || !current.matches("[1-9]\\d{0,2}")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        int curPart = Integer.parseInt(current);
        if (partsCount < 1 || curPart > partsCount) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        if (partsCount > maxXSSRequestParts()) {
            resp.setContentType("text/javascript");
            resp.getWriter().write("net.sf.j2s.ajax.SimpleRPCRequest" + ".xssNotify(\"" + scriptRequestID + "\", \"exceedrequestlimit\");");
            return null;
        }
        String attrName = "jzn" + scriptRequestID;
        String attrTime = "jzt" + scriptRequestID;
        String[] parts = null;
        boolean badRequest = false;
        boolean toContinue = false;
        HttpSession session = req.getSession();
        synchronized (session) {
            cleanSession(session);
            Object attr = session.getAttribute(attrName);
            if (attr == null) {
                parts = new String[partsCount];
                session.setAttribute(attrName, parts);
                session.setAttribute(attrTime, new Long(System.currentTimeMillis()));
            } else {
                parts = (String[]) attr;
                if (partsCount != parts.length) {
                    badRequest = true;
                }
            }
            if (!badRequest) {
                parts[curPart - 1] = request;
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i] == null) {
                        toContinue = true;
                        break;
                    }
                }
                if (!toContinue) {
                    session.removeAttribute(attrName);
                    session.removeAttribute(attrTime);
                }
            }
        }
        if (badRequest) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        if (toContinue) {
            resp.setContentType("text/javascript");
            resp.getWriter().write("net.sf.j2s.ajax.SimpleRPCRequest" + ".xssNotify(\"" + scriptRequestID + "\", \"continue\"" + ((curPart == 1) ? ", \"" + session.getId() + "\");" : ");"));
            return null;
        }
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < parts.length; i++) {
            buf.append(parts[i]);
            parts[i] = null;
        }
        return buf.toString();
    }

    private void cleanSession(HttpSession ses) {
        Enumeration<?> attrNames = ses.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String name = (String) attrNames.nextElement();
            if (name.startsWith("jzt")) {
                Long time = (Long) ses.getAttribute(name);
                if (System.currentTimeMillis() - time.longValue() > maxXSSRequestLatency()) {
                    ses.removeAttribute(name);
                    ses.removeAttribute("jzn" + name.substring(3));
                }
            }
        }
    }
}
