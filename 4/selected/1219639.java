package org.ajaxaio.web;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import com.metaparadigm.jsonrpc.JSONRPCBridge;

/**
 * @author qiangli
 * 
 */
public class JsonrpcInterceptor extends HandlerInterceptorAdapter {

    private final Log logger = LogFactory.getLog(getClass());

    private static final String JSONRPC_URI = "/jsonrpc/JSON-RPC";

    private static final String JSONRPC_ENGINE_JS = "/jsonrpc/engine.js";

    private String jsonrpcJavascript = null;

    private JSONRPCBridge jsonrpcBridge = null;

    private Map registeredObjects = null;

    public JsonrpcInterceptor() {
    }

    public String getJsonrpcJavascript() {
        return this.jsonrpcJavascript;
    }

    public void setJsonrpcJavascript(String jsonrpcJavascript) {
        this.jsonrpcJavascript = jsonrpcJavascript;
    }

    public JSONRPCBridge getJsonrpcBridge() {
        return this.jsonrpcBridge;
    }

    public void setJsonrpcBridge(JSONRPCBridge jsonrpcBridge) {
        this.jsonrpcBridge = jsonrpcBridge;
    }

    public Map getRegisteredObjects() {
        return this.registeredObjects;
    }

    public void setRegisteredObjects(Map registeredObjects) {
        this.registeredObjects = registeredObjects;
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(true);
        logger.info("@@@ Session Id:" + session.getId());
        String uri = request.getRequestURI();
        String ctxpath = request.getContextPath();
        ServletContext sc = session.getServletContext();
        String filename = uri.substring(ctxpath.length());
        logger.info("uri: " + uri + " contextPath: " + ctxpath + " filename: " + filename);
        if (session.getAttribute(JSONRPC_URI) == null) {
            session.setAttribute(JSONRPC_URI, JSONRPC_URI);
            if (jsonrpcBridge == null) {
                jsonrpcBridge = new JSONRPCBridge();
            }
            if (registeredObjects != null) {
                Set set = registeredObjects.entrySet();
                for (Iterator i = set.iterator(); i.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) i.next();
                    logger.info("registering: " + entry.getKey() + ":" + entry.getValue());
                    jsonrpcBridge.registerObject(entry.getKey(), entry.getValue());
                }
            }
            session.setAttribute("JSONRPCBridge", jsonrpcBridge);
        }
        if (filename.startsWith(JSONRPC_ENGINE_JS)) {
            RequestDispatcher dispatcher = sc.getRequestDispatcher("/jsp/jsonrpc/engine.jsp");
            dispatcher.forward(request, response);
            return false;
        } else if (filename.equals(JSONRPC_URI)) {
            return true;
        }
        request.setAttribute("registeredObjects", registeredObjects);
        RequestDispatcher dispatcher = sc.getRequestDispatcher("/jsp/jsonrpc/index.jsp");
        dispatcher.forward(request, response);
        return false;
    }

    protected String getResourceAsString(String name) throws Exception {
        InputStream is = null;
        ByteArrayOutputStream os = null;
        try {
            os = new ByteArrayOutputStream();
            is = new BufferedInputStream(this.getClass().getResourceAsStream(name));
            byte[] buf = new byte[4096];
            int nread = -1;
            while ((nread = is.read(buf)) != -1) {
                os.write(buf, 0, nread);
            }
            os.flush();
        } finally {
            os.close();
            is.close();
        }
        return os.toString();
    }
}
