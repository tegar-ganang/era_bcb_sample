package net.sf.iqser.plugin.web.crawler;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CrawlerServlet extends HttpServlet {

    /** Serial ID */
    private static final long serialVersionUID = 3161118460702868849L;

    /** The action parameter. */
    private static final String ACTION_PARAMETER = "action";

    /** The servlet writer. */
    private PrintWriter writer = null;

    /** The crawler manaager */
    private CrawlerManager manager = null;

    public void init() throws ServletException {
        super.init();
    }

    public void doGet(HttpServletRequest inRequest, HttpServletResponse inResponse) throws ServletException, IOException {
        String requestedAction = (String) inRequest.getParameter(ACTION_PARAMETER);
        inResponse.setContentType("text/html");
        writer = inResponse.getWriter();
        if (requestedAction == null) {
            createErrorPage();
        } else if (requestedAction.equals("run")) {
            String protocol = (String) inRequest.getParameter("protocol");
            String database = (String) inRequest.getParameter("database");
            String username = (String) inRequest.getParameter("username");
            String password = (String) inRequest.getParameter("password");
            if ((protocol == null) || (database == null) || (username == null) || (password == null)) {
                createErrorPage();
            } else {
                createRunPage();
                manager = CrawlerManager.getInstance();
                manager.setDatabaseConnection(protocol, database, username, password);
            }
        } else if (requestedAction.equals("start")) {
            createStartPage();
        } else if (requestedAction.equals("stop")) {
            createStopPage();
        } else if (requestedAction.equals("test")) {
            createTestPage();
        } else {
            createDefaultPage();
        }
    }

    private void createStartPage() {
        createHeader();
        writer.println("<h1>Crawler Servlet</h1>");
        writer.println("<p>Please fill in the data base connection to start the crawler jobs:</p>");
        writer.println("<form id=\"configure\" action=\"crawler\" method=\"get\" name=\"configure\">");
        writer.println("<input type=\"hidden\" name=\"action\" value=\"run\">");
        writer.println("<table width=\"122\" border=\"0\" cellspacing=\"2\" cellpadding=\"0\">");
        writer.println("<tr><td>Database</td><td><input type=\"text\" name=\"database\" size=\"24\"></td></tr>");
        writer.println("<tr><td>Protocol</td><td><input type=\"text\" name=\"protocol\" value=\"jdbc:mysql:\" size=\"24\"></td></tr>");
        writer.println("<tr><td>Username</td><td><input type=\"text\" name=\"username\" size=\"24\"></td></tr>");
        writer.println("<tr><td>Password</td><td><input type=\"password\" name=\"password\" size=\"24\"></td></tr>");
        writer.println("</table>");
        writer.println("<p><input type=\"submit\" name=\"submit\" value=\"Submit\">&nbsp;<input type=\"reset\" value=\"Reset\"></p>");
        writer.println("</form>");
        createFooter();
    }

    private void createRunPage() {
        createHeader();
        writer.println("<h1>Crawler Servlet</h1>");
        if (!manager.areRunning()) {
            manager.startAllCrawler();
            writer.println("<p>Crawler jobs have been startet. Look at the log file for further information.</p>");
        } else {
            writer.println("<p>Crawler are currently allready runnung.</p>");
        }
        createFooter();
    }

    private void createStopPage() {
        createHeader();
        writer.println("<h1>Crawler Servlet</h1>");
        manager.stopAllCrawler();
        writer.println("<p>Crawler jobs have been stopped.</p>");
        createFooter();
    }

    private void createTestPage() {
        createHeader();
        writer.println("<h1>Crawler Servlet</h1>");
        writer.println("<p>The Servlet is running.</p>");
        createFooter();
    }

    private void createDefaultPage() {
        createHeader();
        writer.println("<h1>Crawler Servlet</h1>");
        writer.println("<p>You have following options:</p>");
        writeLinksWithActions();
        createFooter();
    }

    private void createErrorPage() {
        createHeader();
        writer.println("<h1>Crawler Servlet</h1>");
        writer.println("<h2>An error occured:</h2>");
        writer.println("<p>Missing parameters. You have the following options</p>");
        writeLinksWithActions();
        createFooter();
    }

    private void createHeader() {
        writer.println("<html>");
        writer.println("<head>");
        writer.println("<title>iQser Crawler Servlet</title>");
        writer.println("<style type=\"text/css\" media=\"screen\"><!-- body { font-family: Arial, Helvetica, Geneva, SunSans-Regular, sans-serif } --></style>");
        writer.println("</head>");
        writer.println("<body>");
        writer.println("<p />");
    }

    private void createFooter() {
        writer.println("</body>");
        writer.println("</html>");
    }

    private void writeLinksWithActions() {
        writer.println("<h2>Actions:</h2>");
        writer.println("<ul>");
        writer.println("<li>test: <a href=\"http://localhost:8080/crawler?action=test\">http://localhost:8080/crawler?action=test</a></li>");
        writer.println("<li>start: <a href=\"http://localhost:8080/crawler?action=start\">http://localhost:8080/crawler?action=start</a></li>");
        writer.println("<li>stop: <a href=\"http://localhost:8080/cawler?action=stop\">http://localhost:8080/crawler?action=stop</a>\n</li>");
        writer.println("</ul>");
    }
}
