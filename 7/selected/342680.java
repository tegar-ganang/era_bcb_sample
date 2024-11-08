package cn.vlabs.clb.ui.rest;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import cn.cnic.esac.clb.util.HttpStatus;

public class RestAPI extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
	 * Constructor of the object.
	 */
    public RestAPI() {
        super();
    }

    public void service(HttpServletRequest p_request, HttpServletResponse p_response) throws IOException, ServletException {
        p_request.setCharacterEncoding("GBK");
        String requestURL = extractUrl(p_request);
        String method = p_request.getMethod();
        if (requestURL == null || requestURL.length() == 0) {
            p_response.sendError(HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }
        String[] params = splitUrl(requestURL);
        if (params == null || params.length == 0) {
            p_response.sendError(HttpStatus.BAD_PARAMETER);
            debug("REST:Parameter is required!\n", requestURL, method);
            return;
        }
        RestAction action = ActionRegistry.getRegistedAction(method, params);
        if (action == null) {
            p_response.sendError(HttpStatus.BAD_PARAMETER);
            error("Action not Found.\n", requestURL, method);
            return;
        }
        try {
            action.execute(p_request, p_response, params);
        } catch (IllegalStateException e) {
            error("IllegalState:", requestURL, method);
        }
    }

    private void debug(String msg, String requestURL, String method) {
        StringBuffer message = new StringBuffer();
        message.append(msg);
        message.append("HTTP Method:" + method + "\n");
        message.append("URL: " + requestURL + "\n");
        log.debug(message);
    }

    private void error(String msg, String requestURL, String method) {
        StringBuffer message = new StringBuffer();
        message.append(msg);
        message.append("HTTP Method:" + method + "\n");
        message.append("URL: " + requestURL + "\n");
        log.error(message);
    }

    private String extractUrl(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String servletPath = request.getServletPath();
        String requestURI = request.getRequestURI().toString();
        String tobeRemove = contextPath + servletPath;
        return requestURI.replaceFirst(tobeRemove, "");
    }

    private String[] splitUrl(String url) {
        String[] parts = url.split("/");
        if (parts.length == 0) return null;
        if (!parts[0].equalsIgnoreCase("")) return parts;
        if (parts.length <= 1) return null;
        String[] retParts = new String[parts.length - 1];
        for (int i = 0; i < retParts.length; i++) {
            retParts[i] = parts[i + 1];
        }
        return retParts;
    }

    private static Logger log = Logger.getLogger(RestAPI.class);
}
