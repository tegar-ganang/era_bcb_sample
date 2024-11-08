package net.pesahov.remote.socket.rmi.http.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.pesahov.common.utils.Exceptions;
import net.pesahov.remote.socket.RemoteSocket;
import net.pesahov.remote.socket.rmi.http.server.data.RemoteServerSockets;
import net.pesahov.remote.socket.rmi.http.server.data.RemoteSockets;

/**
 * @author Pesahov Dmitry
 * @since 2.0
 */
public class HttpServerUnderlyingServiceServlet extends HttpServlet {

    /**
     * Use serialVersionUID from JDK 1.0.2 for interoperability
     */
    static final long serialVersionUID = -3715228668239966592L;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null) throw new IllegalStateException("Unauthorized user request!");
        log("SERVICE >> Session id: " + session.getId() + " Creation time: " + session.getCreationTime());
        ObjectInputStream in = new ObjectInputStream(request.getInputStream());
        String method = in.readUTF();
        Serializable[] arguments;
        try {
            arguments = (Serializable[]) in.readUnshared();
        } catch (ClassNotFoundException ex) {
            throw Exceptions.getNested(ServletException.class, ex);
        }
        log("SERVICE >> Method: " + method + " Arguments" + Arrays.deepToString(arguments));
        Object result;
        try {
            if (method.equals("initSocket")) result = RemoteSockets.init(session, (Class<?>[]) arguments[0], (Object[]) arguments[1]); else if (method.equals("initServerSocket")) result = RemoteServerSockets.init(session, (Class<?>[]) arguments[0], (Object[]) arguments[1]); else if (method.equals("accept") || method.equals("readFromInputStream") || method.equals("writeToOutputStream")) throw new IllegalArgumentException("Illegal invocation method: " + method); else {
                result = invoke(session, (Long) arguments[0], method, (Class<?>[]) arguments[1], (Object[]) arguments[2]);
            }
        } catch (Exception ex) {
            result = Exceptions.getNested(IOException.class, ex);
        }
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        try {
            ObjectOutputStream out = new ObjectOutputStream(content);
            out.writeUnshared(result);
            out.flush();
        } catch (Exception ex) {
            content.reset();
            log("Serializaion error: " + ex.getMessage());
        }
        response.setContentType("application/octet-stream");
        response.setContentLength(content.size());
        content.writeTo(response.getOutputStream());
        response.getOutputStream().flush();
    }

    /**
     * Invokes the socket instance method.
     * @param session {@link HttpSession} instance.
     * @param id Socket id.
     * @param methodName Socket method to invoke. 
     * @param arguments Socket method's arguments.
     * @return A result value of invoked method or <code>null</code> if it does not return a value.
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Object invoke(HttpSession session, Long id, String methodName, Class<?>[] argumentTypes, Object[] arguments) throws Exception {
        Object socket = RemoteSockets.get(session, id);
        if (socket == null) socket = RemoteServerSockets.get(session, id);
        if (socket == null) throw new IllegalArgumentException("RemoteSocket or RemoteServerSocket instance id is invalid!");
        if (methodName.equals("close")) {
            if (socket instanceof RemoteSocket) {
                RemoteSockets.remove(session, id);
            } else {
                Object[] accepterAndRemoteSocketList = (Object[]) session.getAttribute("RemoteServerSocketAcceptor" + id);
                if (accepterAndRemoteSocketList != null) {
                    List<Long> socketIds = (List<Long>) accepterAndRemoteSocketList[1];
                    for (Long socketId : socketIds) try {
                        invoke(session, socketId, methodName, argumentTypes, arguments);
                    } catch (Exception ex) {
                    }
                }
                RemoteServerSockets.remove(session, id);
            }
        }
        return socket.getClass().getMethod(methodName, argumentTypes).invoke(socket, arguments);
    }
}
