package net.jetrix.servlets;

import java.io.*;
import java.util.logging.*;
import javax.servlet.*;
import javax.servlet.http.*;
import net.jetrix.*;

/**
 * Action Servlet handling actions on users.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 794 $, $Date: 2009-02-17 14:08:39 -0500 (Tue, 17 Feb 2009) $
 */
public class UserAction extends HttpServlet {

    private Logger logger = Logger.getLogger("net.jetrix");

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        String name = request.getParameter("name");
        String redirect = "/user.jsp?name=" + name;
        Client client = ClientRepository.getInstance().getClient(name);
        if ("kick".equals(action)) {
            logger.info(client.getUser().getName() + " (" + client.getInetAddress() + ") has been kicked by " + request.getRemoteUser() + " (" + request.getRemoteHost() + ")");
        } else if ("ban".equals(action)) {
            Banlist banlist = Banlist.getInstance();
            banlist.ban(client.getInetAddress().getHostAddress());
            logger.info(client.getUser().getName() + " (" + client.getInetAddress() + ") has been banned by " + request.getRemoteUser() + " (" + request.getRemoteHost() + ")");
            Server.getInstance().getConfig().save();
        }
        client.disconnect();
        response.sendRedirect("/channel.jsp?name=" + client.getChannel().getConfig().getName());
    }
}
