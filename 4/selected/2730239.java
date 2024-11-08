package net.sf.aft.servlet;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.aft.test.HttpMessage;
import net.sf.aft.test.Listener;
import net.sf.aft.test.ServletContainer;
import net.sf.aft.util.Queue;

/**
 * This is the servlet which is registered with Tomcat to receive all
 * the incoming HTTP requests.
 *
 * <p>When a request is received on a given path, the servlet will
 * verify if there's a matching {@link
 * net.sf.aft.test.Listener} waiting for an incoming request
 * on that path. If no <code>listener</code> is registered a
 * <code>404</code> error message is sent back to the client.
 *
 * <p>If there is a <code>listener</code> waiting for an HTTP request
 * in the main Ant thread, the request is placed in a {@link
 * net.sf.aft.util.Queue}, and the thread running the
 * <code>listener</code> is awaken. This will give it the oportunity
 * to execute the set of matchers against the incoming HTTP
 * request. The servler thread blocks at this point, waiting for the
 * <code>listener</code>'s thread to start writing the response back
 * to the client.
 *
 * <p>When the <code>listener</code> finishes the matching, it starts
 * writing the response on a {@link java.io.PipedWriter} object. The
 * servlet thread waits at the other end of the pipe on a {@link
 * java.io.PipedReader}. When the <code>listener</code> closes its end
 * of the pipe, the servlet considers that the response to be sent was
 * created, flushes all the data it has received back to the HTTP
 * client, and returns from the {@link #service(ServletRequest,
 * ServletResponse)} method.
 *
 * @author <a href="mailto:ovidiu@cup.hp.com">Ovidiu Predescu</a>
 * @version $Revision: 1.1.1.1 $ $Date: 2001/12/31 16:23:39 $
 * @since September 27, 2001
 */
public class ListenerProxyServlet extends HttpServlet {

    public void service(ServletRequest servletRequest, ServletResponse servletResponse) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        PrintWriter out;
        try {
            out = response.getWriter();
            Listener dummy = new Listener(request.getServerPort(), request.getRequestURI());
            ServletContainer container = ServletContainer.getServletContainer();
            Hashtable registeredListeners = container.getRegisteredListeners();
            Listener listener = (Listener) registeredListeners.get(dummy);
            if (listener != null) {
                Queue requestsQueue = container.getRequestsQueue();
                registeredListeners.remove(dummy);
                HttpMessage message = new HttpMessage(request);
                if (listener.getDebug() > 0) listener.println("Received message:\n" + message);
                listener.setHttpMessageRequest(message);
                listener.setHttpServletResponse(response);
                PipedWriter writer = new PipedWriter();
                PipedReader reader = new PipedReader(writer);
                listener.setResponseWriter(writer);
                requestsQueue.put(dummy);
                char[] buffer = new char[1024];
                int len;
                while ((len = reader.read(buffer, 0, 1024)) != -1) {
                    out.write(buffer, 0, len);
                }
                reader.close();
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not found " + registeredListeners + ", dummy " + dummy);
            }
        } catch (Exception ex) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain");
                ex.printStackTrace();
            } catch (Exception e) {
            }
        }
    }
}
