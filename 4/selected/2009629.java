package it.ilz.hostingjava.servlet;

import it.ilz.hostingjava.backend.DAOFactory;
import it.ilz.hostingjava.backend.UserException;
import it.ilz.hostingjava.model.Ftpuser;
import it.ilz.hostingjava.model.Userdata;
import it.ilz.hostingjava.util.MailHelper;
import it.ilz.hostingjava.util.Util;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.catalina.manager.ManagerServlet;
import org.apache.catalina.manager.Constants;
import javax.mail.internet.InternetAddress;
import org.apache.velocity.exception.ResourceNotFoundException;

/**
 *
 * @author  luigi
 * @version
 */
public class MainServlet extends ManagerServlet {

    public static String USER = "user";

    public static String FTPUSER = "ftpuser";

    public static String USERNAME = "username";

    public static String PASSWORD = "password";

    public static String NEWPASSWORD = "newPassword";

    public static String EMAIL = "email";

    public static String BASE = null;

    public static String CTXBASE = null;

    public static String MAILFROM = "luigi.zanderighi@glz.it";

    private Util util = null;

    /** Destroys the servlet.
     */
    public void destroy() {
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (config.getInitParameter("mailFrom") != null) {
            MAILFROM = config.getInitParameter("mailFrom");
        }
        CTXBASE = config.getInitParameter("ctxBase");
        BASE = config.getInitParameter("webappBase");
        util = Util.getUtil();
    }

    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     *
     *Se la richiesta proviene da una pagina interna il target sar� la pagina stessa
     *Questo per una comodit� di mapping delle jsp
     *Non ho voglia di impegolarmi in framework o menate di mapping per 2 cazzo di pagine
     *P.T.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String target = request.getHeader("Referer");
            PrintWriter writer = null;
            CharArrayWriter caw = null;
            if (target != null && target.startsWith("http://" + request.getServerName()) && !target.endsWith("index2.jsp") && target.indexOf("/hj") == -1) {
                caw = new CharArrayWriter();
                writer = new PrintWriter(caw);
            } else {
                response.setHeader("Refresh", "1; URL=/");
                response.setContentType("text/plain; charset=" + Constants.CHARSET);
                writer = response.getWriter();
            }
            String command = request.getPathInfo();
            if (command == null) command = "/status";
            String password = request.getParameter(PASSWORD);
            String newPassword = request.getParameter(NEWPASSWORD);
            String email = request.getParameter(EMAIL);
            if (!isLoggedIn(request)) {
                String username = request.getParameter(USERNAME);
                Ftpuser user = null;
                if (command.equals("/status")) {
                    super.serverinfo(writer);
                } else if (command.equals("/logon")) {
                    user = logon(writer, username, password);
                } else if (command.equals("/register")) {
                    user = register(writer, username, password, email);
                    if (user == null) {
                        MailHelper.sendConfirmationMail(username, email, false);
                    }
                } else if (command.equals("/recover")) {
                    if (recover(writer, username, email)) target = "/index.jsp";
                } else {
                    writer.println(sm.getString("managerServlet.unknownCommand", command));
                }
                if (user != null) {
                    request.getSession(true).setAttribute(USER, username);
                    request.getSession(true).setAttribute(FTPUSER, user);
                    target = "/amministrazione.jsp";
                }
            } else {
                String username = (String) request.getSession(true).getAttribute(USER);
                String path = util.toContext((String) request.getSession(true).getAttribute(USER));
                if (command.equals("/reload")) {
                    super.reload(writer, path);
                } else if (command.equals("/start")) {
                    super.start(writer, path);
                } else if (command.equals("/update")) {
                    changePwd(writer, username, password, email, newPassword);
                } else if (command.equals("/stop")) {
                    super.stop(writer, path);
                } else if (command.equals("/status")) {
                    status(writer, path);
                } else if (command.equals("/logoff")) {
                    request.getSession().removeAttribute(USER);
                    request.getSession().invalidate();
                    target = "/index.jsp";
                } else if (command.equals("/serverinfo")) {
                    super.serverinfo(writer);
                } else {
                    writer.println(sm.getString("managerServlet.unknownCommand", command));
                }
            }
            if (request.getSession(true).getAttribute(USER) != null) {
                String path = util.toContext((String) request.getSession(true).getAttribute(USER));
                ContextStatus cs = new ContextStatus();
                org.apache.catalina.Context context = findDeployedApp(path);
                if (cs != null) {
                    cs.setSession(context.getManager().findSessions().length);
                    cs.setRealPath(context.getDocBase());
                    cs.setRunning(context.getAvailable());
                    cs.setName(context.getName());
                    request.getSession(true).setAttribute("cs", cs);
                }
            }
            writer.flush();
            writer.close();
            if (caw != null) {
                request.getSession().setAttribute("messages", caw);
                response.sendRedirect(target);
            }
        } catch (ServletException sex) {
            throw sex;
        } catch (IOException ioex) {
            throw ioex;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new ServletException(t);
        }
    }

    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Short description";
    }

    /**
     *Verifica se l'utente ha fatto il logon
     */
    private boolean isLoggedIn(HttpServletRequest request) {
        return (request.getSession(true).getAttribute(USER) != null);
    }

    private org.apache.catalina.Context findDeployedApp(String ctxName) {
        org.apache.catalina.Wrapper wrapper = super.getWrapper();
        org.apache.catalina.Context context = (org.apache.catalina.Context) wrapper.getParent();
        org.apache.catalina.Host host = (org.apache.catalina.Host) context.getParent();
        return (org.apache.catalina.Context) host.findChild(ctxName);
    }

    /**
     * Recovers the user password
     */
    private boolean recover(PrintWriter writer, String username, String email) {
        try {
            Ftpuser u = DAOFactory.getUserDAO().recreatePassword(username, email);
            if (u.getUserdata().getHasDatabase() != null && u.getUserdata().getHasDatabase().booleanValue()) {
                DAOFactory.getDBDAO().updateGrants(username, u.getPasswd());
                String contextTmpFolder = getServletConfig().getServletContext().getRealPath("/WEB-INF/config");
                String contextFile = contextTmpFolder + File.separatorChar + util.toContext(username) + ".xml";
                DAOFactory.getFSDAO().savectx(username, u.getPasswd(), contextFile, BASE, u.getUserdata().getHasDatabase().booleanValue());
            }
            MailHelper.sendTemplateMail(username, "La tua password � stata ricreata", "templates/recoverPassword.html");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            writer.println("FAIL - Error recovering password, check username and email");
            return false;
        }
    }

    private Ftpuser register(PrintWriter writer, String username, String password, String email) throws javax.naming.NamingException, java.sql.SQLException, ResourceNotFoundException, Exception {
        Ftpuser user = null;
        if (findDeployedApp(util.toContext(username)) != null) {
            writer.println("FAIL - User exists");
            return null;
        }
        try {
            new InternetAddress(email).validate();
        } catch (javax.mail.internet.AddressException aex) {
            writer.println("FAIL - " + aex.getMessage());
            return null;
        }
        String userHome = BASE + File.separatorChar + username;
        if (new File(userHome).exists()) {
            writer.println("FAIL - User home exists");
            return null;
        }
        if ((DAOFactory.getUserDAO().usernameExists(username))) {
            writer.println("FAIL - User already registered");
            return null;
        }
        String contextTmpFolder = getServletConfig().getServletContext().getRealPath("/WEB-INF/config");
        String contextFile = contextTmpFolder + File.separatorChar + util.toContext(username) + ".xml";
        if (!new File(contextTmpFolder).exists() && !new File(contextTmpFolder).mkdir()) {
            writer.println("FAIL - Unable to create temporary context information");
            return null;
        }
        try {
            DAOFactory.getFSDAO().savectx(username, password, contextFile, BASE);
            DAOFactory.getUserDAO().createUser(username, email, password);
            DAOFactory.getDBDAO().createNewSchema(username, password);
            DAOFactory.getFSDAO().createHome(BASE, username);
            super.deploy(writer, contextFile, util.toContext(username), null, false);
            user = logon(writer, username, password);
            MailHelper.sendConfirmationMail(username, email, false);
        } catch (Exception e) {
            writer.println("FAIL - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        return user;
    }

    /**
     * Changes user password
     *
     * @param writer writes to jsp
     * @param username username
     * @param password oldpassword
     * @param email newemail
     * @param newPassword newpassword
     */
    private void changePwd(PrintWriter writer, String username, String password, String email, String newPassword) {
        try {
            Ftpuser user = new Ftpuser();
            user.setUserdata(new Userdata());
            user.getUserdata().setEmail(email);
            user.setPasswd(newPassword);
            user = DAOFactory.getUserDAO().updateUser(user, username, password);
            if (user.getUserdata().getHasDatabase().booleanValue()) {
                DAOFactory.getDBDAO().updateGrants(username, newPassword);
                DAOFactory.getFSDAO().savectx(username, newPassword, MainServlet.CTXBASE + File.separatorChar + '-' + username + ".xml", MainServlet.BASE, true);
            }
            if (email != null && !email.equals("")) {
                writer.println("OK - Updated email token");
            }
            if (newPassword != null && !newPassword.equals("")) {
                writer.println("OK - Updated password token");
            }
        } catch (Exception e) {
            writer.println("FAIL - " + e.getMessage());
        }
    }

    private Ftpuser logon(PrintWriter writer, String username, String password) throws javax.naming.NamingException, java.sql.SQLException {
        Ftpuser user = null;
        try {
            user = DAOFactory.getUserDAO().logon(username, password);
            writer.write("OK - Valid credentials");
        } catch (UserException ue) {
            writer.write("FAIL - Invalid credentials");
        }
        return user;
    }

    private void status(PrintWriter writer, String path) {
        org.apache.catalina.Context context = findDeployedApp(path);
        String displayPath = path;
        if (displayPath.equals("")) displayPath = "/";
        if (context != null) {
            if (context.getAvailable()) {
                writer.println(sm.getString("managerServlet.listitem", displayPath, "running", "" + context.getManager().findSessions().length, context.getDocBase()));
            } else {
                writer.println(sm.getString("managerServlet.listitem", displayPath, "stopped", "0", context.getDocBase()));
            }
        }
    }
}
