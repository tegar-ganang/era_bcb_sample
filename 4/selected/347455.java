package uk.org.primrose.console;

import uk.org.primrose.Constants;
import uk.org.primrose.GeneralException;
import uk.org.primrose.Logger;
import uk.org.primrose.Util;
import uk.org.primrose.pool.PoolException;
import uk.org.primrose.pool.core.ConnectionHolder;
import uk.org.primrose.pool.core.Pool;
import uk.org.primrose.pool.core.PoolConfigImpl;
import uk.org.primrose.pool.core.PoolData;
import uk.org.primrose.pool.core.PoolLoader;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class WebConsole {

    private static int port = -1;

    private static boolean alreadyStarted = false;

    private static boolean bKeepRunning = true;

    private static String username = null;

    private static String password = null;

    private Logger logger = null;

    List<Pool> loadedPools = null;

    public WebConsole(String username, String password, int port, Logger logger) {
        WebConsole.port = port;
        this.logger = logger;
        WebConsole.username = username;
        WebConsole.password = password;
    }

    public WebConsole(int port, Logger logger) {
        WebConsole.port = port;
        this.logger = logger;
    }

    public static void main(String[] args) throws IOException {
        new WebConsole(8080, null).start();
    }

    public void start() throws IOException {
        if ((port > 0) && !alreadyStarted) {
            if (logger == null) {
                logger = new Logger();
            }
            logger.info("Starting WebConsole on port " + port);
            new WebConsoleStarter().start();
            Runtime.getRuntime().addShutdownHook(new WebConsoleStopper());
        }
    }

    public static void shutdown() {
        try {
            bKeepRunning = false;
            alreadyStarted = false;
            Socket s = new Socket("localhost", port);
            s.close();
        } catch (Exception e) {
        }
    }

    public void setUsername(String username) {
        WebConsole.username = username;
    }

    public void setPassword(String password) {
        WebConsole.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    class WebConsoleStarter extends Thread {

        public void run() {
            if (alreadyStarted) {
                return;
            }
            try {
                logger.verbose("About to bind to port " + port);
                ServerSocket ss = new ServerSocket(port);
                alreadyStarted = true;
                logger.verbose("Listening for connections ...");
                while (bKeepRunning) {
                    new SocketHandler(ss.accept()).start();
                }
            } catch (IOException ioe) {
                logger.error("Cannot start WebConsole on port " + port + " : " + ioe);
                logger.printStackTrace(ioe);
            }
        }
    }

    class WebConsoleStopper extends Thread {

        public void run() {
            bKeepRunning = false;
            logger.info("Stopping WebConsole");
            WebConsole.shutdown();
        }
    }

    class SocketHandler extends Thread {

        Socket s = null;

        HttpRequest httpRequest = new HttpRequest(logger);

        OutputStream out = null;

        SocketHandler(Socket s) {
            this.s = s;
        }

        public void run() {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                out = s.getOutputStream();
                String line;
                int iCountBlankLines = 0;
                while ((line = br.readLine()) != null) {
                    if (line.length() == 0) {
                        iCountBlankLines++;
                    }
                    baos.write((line + "\n").getBytes());
                    if (iCountBlankLines == 1) {
                        break;
                    }
                }
                baos.flush();
                byte[] requestData = baos.toByteArray();
                baos.close();
                if (httpRequest.parseRequest(new String(requestData))) {
                    handleRequest();
                }
                br.close();
                out.close();
            } catch (IOException ioe) {
                logger.printStackTrace(ioe);
            } finally {
                try {
                    s.close();
                } catch (IOException ioe) {
                    logger.printStackTrace(ioe);
                }
            }
        }

        private boolean userAuthorised() {
            if ((username == null) || (password == null)) {
                return true;
            }
            HashMap<String, String> headers = httpRequest.getHeaders();
            String value = headers.get("authorization");
            if (value == null) {
                value = headers.get("Authorization");
            }
            if (value != null) {
                String b64pass = value.split(" ")[1];
                sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();
                String userpass = null;
                try {
                    userpass = new String(decoder.decodeBuffer(b64pass));
                } catch (IOException ioe) {
                }
                if (userpass != null) {
                    if (userpass.indexOf(":") != -1) {
                        String[] s = userpass.split(":");
                        if (password.equals(s[1]) && username.equals(s[0])) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private void handleRequest() throws IOException {
            if (!userAuthorised()) {
                writeHeaders("text/html", "", 401, 0);
                return;
            }
            String resource = httpRequest.getResource();
            logger.verbose("Processing resource : '" + resource + "'");
            loadedPools = PoolLoader.getLoadedPools();
            if ((resource == null) || (resource.length() == 0) || resource.equals("/") || resource.equals("/index.html")) {
                writeHeaders("text/html", "", 200, 0);
                writeTop();
                writeData("<br/><h4>Welcome to the Primrose Web Console</h4>");
                for (Pool pool : loadedPools) {
                    writeData("<p>");
                    writeData("<a href=\"/showPool?poolName=" + pool.getPoolName() + "\">Show '" + pool.getPoolName() + "' Pool</a>");
                    writeData("</p>");
                }
                writeBottom();
            } else if (resource.startsWith("/showPoolConfig")) {
                writeHeaders("text/html", "", 200, 0);
                writeTop();
                String poolName = httpRequest.getParameters().get("poolName");
                Pool pool = findPoolByName(poolName);
                if (pool != null) {
                    writeData("<p>");
                    writeData("<a href=\"/showPool?poolName=" + pool.getPoolName() + "\">Show '" + pool.getPoolName() + "' Connections</a>");
                    writeData("</p>");
                    writeData("<br/><form method=\"get\" action=\"alterPoolProperties\"><table>");
                    Method[] publicMethods = PoolConfigImpl.class.getMethods();
                    for (int j = 0; j < publicMethods.length; j++) {
                        String methodName = publicMethods[j].getName();
                        if (methodName.startsWith("getClass")) {
                            continue;
                        }
                        if (methodName.startsWith("get")) {
                            try {
                                Object ret = publicMethods[j].invoke(pool, new Object[] {});
                                if (ret == null) {
                                    ret = "";
                                }
                                writeData("<tr><td>" + methodName.replaceAll("get", "") + "</td><td><input type=\"text\" name=\"" + methodName.replaceAll("get", "") + "\" size=\"60\" value=\"" + ret + "\"/></td></tr>");
                            } catch (Exception e) {
                            }
                        }
                    }
                    writeData("<tr><td colspan=\"2\"><input type=\"hidden\" value=\"" + poolName + "\"/></td></tr>");
                    writeData("<tr><td colspan=\"2\"><input type=\"submit\" value=\"Update Pool Now\"/></td></tr>");
                    writeData("</table></form>");
                    writeData("<p>If you update the pool settings, the existing pool will be restarted in order for the settings to take effect. Any running connections/SQL will NOT be affected - their jobs will finish normally, and then those connections will be retired.</p>");
                } else {
                    writeData("<p>Sorry, cannot find pool under name '" + poolName + "'</p>");
                }
                writeBottom();
            } else if (resource.equals("/webconsole.css")) {
                writeCSS();
            } else if (resource.startsWith("/poolStop")) {
                String poolName = httpRequest.getParameters().get("poolName");
                Pool pool = findPoolByName(poolName);
                try {
                    pool.stop(false);
                } catch (PoolException pe) {
                    logger.printStackTrace(pe);
                }
                writeHeaders("text/html", "/showPool?poolName=" + pool.getPoolName(), 302, 0);
            } else if (resource.startsWith("/poolStart")) {
                String poolName = httpRequest.getParameters().get("poolName");
                Pool pool = findPoolByName(poolName);
                try {
                    pool.start();
                } catch (PoolException pe) {
                    logger.printStackTrace(pe);
                }
                writeHeaders("text/html", "/showPool?poolName=" + pool.getPoolName(), 302, 0);
            } else if (resource.startsWith("/poolRestart")) {
                String poolName = httpRequest.getParameters().get("poolName");
                Pool pool = findPoolByName(poolName);
                try {
                    pool.restart(false);
                } catch (PoolException pe) {
                    logger.printStackTrace(pe);
                }
                writeHeaders("text/html", "/showPool?poolName=" + pool.getPoolName(), 302, 0);
            } else if (resource.startsWith("/showPool")) {
                String poolName = httpRequest.getParameters().get("poolName");
                String refreshRate = httpRequest.getParameters().get("refresh");
                Pool pool = findPoolByName(poolName);
                writeHeaders("text/html", "", 200, 0);
                writeTop(refreshRate);
                writeData("<table><tr><td><table><tr><td>Server date : " + new java.util.Date() + "</td></tr>");
                writeData("<tr><td>Primrose Version : " + Constants.VERSION + "</td></tr>");
                if (pool != null) {
                    int numberOfWaitingThreads = pool.getNumberOfWaitingThreads();
                    if (numberOfWaitingThreads > 0) {
                        writeData("<tr><td><span style='color:red'>Warning ! There are " + numberOfWaitingThreads + " threads waiting for a connection.</span> You may want to increase your base pool size.</td></tr>");
                    }
                    writeData("<tr><td>Total number of connections handed out : " + pool.getTotalConnectionsHandedOut() + "</td></tr>");
                    writeData("<tr><td><a href=\"/showPoolConfig?poolName=" + poolName + "\">Edit runtime config properties</a></td></tr></table></td><td>&nbsp;</td>");
                    writeData("<td><table><tr><td><a href=\"/poolStop?poolName=" + poolName + "\">Stop Pool</a></td></tr>");
                    writeData("<tr><td><a href=\"/poolStart?poolName=" + poolName + "\">Start Pool</a></td></tr>");
                    writeData("<tr><td><a href=\"/poolRestart?poolName=" + poolName + "\">Restart Pool</a></td></tr></table></td></tr></table>");
                    writeData("<p/><h5>Connection Data</h5>");
                    Vector<ConnectionHolder> connections = pool.getPoolConnections();
                    writeData("<table border=\"1\">");
                    writeData("<tr><td><h5>ID</h5></td>" + "<td><h5>Status</h5></td>" + "<td><h5>Opens</h5></td>" + "<td><h5>Closes</h5></td>" + "<td><h5>CallableStatement</h5></td>" + "<td><h5>PreparedStatement</h5></td>" + "<td><h5>Statement</h5></td>" + "<td><h5>Active For</h5></td>" + "<td><h5>Idle For</h5></td>" + "<td><h5>SQL</h5></td></tr>");
                    for (ConnectionHolder ch : connections) {
                        String status = PoolData.getStringStatus(Pool.UNKNOWN_STATUS_CODE);
                        String inusefor = "n/a";
                        String idlefor = "n/a";
                        if (ch.status == Pool.CONNECTION_ACTIVE) {
                            status = "<span style='color:red'>" + PoolData.getStringStatus(ch.status) + "</span>";
                            inusefor = ((System.currentTimeMillis() - ch.connOpenedDate) / 1000) + " secs";
                        } else if (ch.status == Pool.CONNECTION_INACTIVE) {
                            status = "<span style='color:green'>" + PoolData.getStringStatus(ch.status) + "</span>";
                            if (ch.lastUsedTimestamp > 0L) {
                                idlefor = ((System.currentTimeMillis() - ch.lastUsedTimestamp) / 1000) + " secs";
                            }
                        }
                        writeData("<tr><td>" + ch.conn.hashCode() + "</td><td>" + status + "</td>" + "<td>" + ch.numberOfOpens + "</td>" + "<td>" + ch.numberOfCloses + "</td>" + "<td>" + ch.numberOfJDBCCallableStatementsRun + "</td>" + "<td>" + ch.numberOfJDBCPreparedStatementsRun + "</td>" + "<td>" + ch.numberOfJDBCStatementsRun + "</td>" + "<td>" + inusefor + "</td>" + "<td>" + idlefor + "</td>" + "<td>" + ch.sql + "</td></tr>");
                    }
                    writeData("</table>");
                    writeData("<br/><p>" + "ID - The PoolConnection class hashCode()<br/>" + "Status - The Status of the connection<br/>" + "Opens - How many times this connection has been extracted from the pool<br/>" + "Closes - How many times this connection has been returned to the pool<br/>" + "CallableStatement - How many JDBC CallableStatement objects have been created from this connection<br/>" + "PreparedStatement - How many JDBC PreparedStatement objects have been created from this connection<br/>" + "Statement - How many JDBC Statement objects have been created from this connection<br/>" + "Active For - How many seconds this connection has been in use for(if active)<br/>" + "Idle For - How many seconds this connection has been idle for (if inactive)<br/>" + "SQL - The current executing SQL on this connection (if any)</p>");
                } else {
                    writeData("<p>Sorry, cannot find pool under name '" + poolName + "'</p>");
                }
                writeData("<p><form method=\"get\"><input type=\"text\" name=\"refresh\"/><input type=\"hidden\" name=\"poolName\" value=\"" + poolName + "\"/>&nbsp;<input type=\"submit\" value=\"Set Page Refresh Rate (seconds)\"/></form>");
                writeBottom();
            } else if (resource.startsWith("/alterPoolProperties")) {
                Pool pool = findPoolByName(httpRequest.parameters.get("PoolName"));
                Iterator<String> parametersIter = httpRequest.parameters.keySet().iterator();
                while (parametersIter.hasNext()) {
                    String key = parametersIter.next();
                    String value = httpRequest.parameters.get(key);
                    if (!key.equals("PoolName")) {
                        try {
                            logger.info("[WebConsole] alterPoolProperties() Setting " + key + "=" + value);
                            Util.callClassMethod(Pool.class, pool, "set" + key, new Object[] { value });
                        } catch (GeneralException ge) {
                            showErrorPage("Error setting property " + key + " : " + ge);
                            return;
                        }
                    }
                }
                try {
                    pool.restart(false);
                } catch (PoolException pe) {
                    showErrorPage("Error restarting pool : " + pe);
                }
                writeHeaders("text/html", "/showPool?poolName=" + pool.getPoolName(), 302, 0);
            }
        }

        private void showErrorPage(String error) throws IOException {
            writeHeaders("text/html", "", 200, 0);
            writeTop();
            writeData(error);
            writeBottom();
        }

        private Pool findPoolByName(String poolName) {
            for (Pool pool : loadedPools) {
                if (pool.getPoolName().equals(poolName)) {
                    return pool;
                }
            }
            return null;
        }

        private void writeHeaders(String contentType, String szExtraData, int iHttpStatusCode, int iContentLength) throws IOException {
            if (iHttpStatusCode == 500) {
                writeData("HTTP/1.1 500 " + szExtraData + ";");
            } else if (iHttpStatusCode == 401) {
                writeData("HTTP/1.1 401 Authenitcation Required;");
                writeData("WWW-Authenticate: Basic realm=\"Primrose Web Console\"");
            } else if (iHttpStatusCode == 302) {
                writeData("HTTP/1.1 302 Found;");
                writeData("Location: " + szExtraData + "");
            } else {
                writeData("HTTP/1.1 200 OK;");
            }
            writeData("Date: " + new Date() + "");
            writeData("Server: Primrose Web Console");
            writeData("LastModified: " + new Date() + "");
            writeData("Content-Type: " + contentType + "");
            if (iContentLength > 0) {
                writeData("Content-Length: " + iContentLength + "");
            }
            writeData("Connection:close");
            writeData("");
        }

        private void writeData(String data) throws IOException {
            data += "\n";
            out.write(data.getBytes());
            out.flush();
        }

        private void writeTop() throws IOException {
            writeTop(null);
        }

        private void writeTop(String refreshRate) throws IOException {
            writeData("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN'");
            writeData("  'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>");
            writeData("<!--<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
            writeData("	\"http://www.w3.org/TR/html4/strict.dtd\">-->");
            writeData("<html lang=\"en\">");
            writeData("	<head>");
            if (refreshRate != null) {
                writeData("<meta http-equiv=\"refresh\" content=\"" + refreshRate + "\"/>");
            }
            writeData("		<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\"/>");
            writeData("		<style type=\"text/css\"><!-- ");
            writeData("			@import \"/webconsole.css\"; ");
            writeData("		--></style>");
            writeData("		<title>Primrose Web Console</title>");
            writeData("	</head>");
            writeData("	<body>");
            writeData("		<div id=\"pageWrapper\">");
            writeData("			");
            writeData("			<!-- masthead content begin -->");
            writeData("			");
            writeData("<!--banner -->");
            writeData("<div id=\"bannerOuterWrapper\">");
            writeData("  <div id=\"bannerWrapper\">");
            writeData("  ");
            writeData("    ");
            writeData("    <h1><a href=\"http://www.primrose.org.uk\"></a></h1>");
            writeData("  </div>");
            writeData("</div>  ");
            writeData("			<!-- masthead content end -->");
            writeData("			");
            writeData("			<!-- horizontal nav begin -->");
            writeData("			<div class=\"hnav\">");
            writeData("	<hr class=\"hide\"/>");
            writeData("</div>");
            writeData("			<!-- horizontal nav end -->");
            writeData("<!--- middle (main content) column begin -->");
            writeData("			<div id=\"outerColumnContainer\">");
            writeData("				<div id=\"innerColumnContainer\">");
            writeData("					<div id=\"SOWrap\">");
            writeData("						<div id=\"middleColumn\">");
            writeData("							<div class=\"inside\">");
        }

        private void writeBottom() throws IOException {
            writeData("		<hr class=\"hide\"/>");
            writeData("	</div>");
            writeData("</div>");
            writeData("<!--- middle (main content) column end -->");
            writeData("<!--- left column begin -->");
            writeData("						<div id=\"leftColumn\">");
            writeData("							<div class=\"inside\">");
            writeData("<div class=\"vnav\"><p>&nbsp;</p></div>");
            writeData("<div class=\"vnav\">");
            writeData("	<h3>&nbsp;</h3>");
            writeData("	<h3>Site Navigation</h3>");
            writeData("	<p></p>");
            writeData("	<ul");
            writeData("		><li");
            writeData("			><a href=\"/\">Home</a");
            writeData("		></li");
            for (Pool pool : loadedPools) {
                writeData("		><li");
                writeData("			><a href=\"/showPool?poolName=" + pool.getPoolName() + "\">Show '" + pool.getPoolName() + "' Pool</a");
                writeData("		></li");
            }
            writeData("		");
            writeData("	></ul>");
            writeData("</div>");
            writeData("						<hr class=\"hide\"/>");
            writeData("							</div>");
            writeData("						</div>");
            writeData("						<div class=\"clear\"></div>");
            writeData("					</div>");
            writeData("<!--- left column end -->");
            writeData("<!--- right column begin -->");
            writeData("					<div id=\"rightColumn\">");
            writeData("						<div class=\"inside\">");
            writeData("<p>&nbsp;</p>");
            writeData("							<hr class=\"hide\"/>");
            writeData("						</div>");
            writeData("					</div>");
            writeData("					<div class=\"clear\"></div>");
            writeData("				</div>");
            writeData("			</div>");
            writeData("<!--- right column end -->");
            writeData("<!-- footer begin -->");
            writeData("			<!-- horizontal nav begin -->");
            writeData("			<div class=\"hnav\">");
            writeData("	<hr class=\"hide\"/>");
            writeData("</div>");
            writeData("			<!-- horizontal nav end -->");
            writeData("<div id=\"bannerWrapperRepeat\">");
            writeData("  ");
            writeData("    ");
            writeData("    <div id=\"footer\">");
            writeData("	<p style=\"margin:0;\">");
            writeData("		&copy; www.primrose.org.uk<br/>");
            writeData("    	");
            writeData("		 <a href=\"http://www.primrose.org.uk\"><img style=\"border:0;\"");
            writeData("			alt=\"Powered By Primrose\"/></a>	        	");
            writeData("	</p>	");
            writeData("    </div>");
            writeData("</div>  ");
            writeData("<!-- footer end -->");
            writeData("		</div>");
            writeData("	</body>");
            writeData("</html>");
        }

        private void writeCSS() throws IOException {
            writeData("@charset \"iso-8859-1\";");
            writeData("/* begin with generic selectors so that they can be overridden if needed");
            writeData(" * by classes deeper in the stylesheet");
            writeData(" */");
            writeData(".clear");
            writeData("{");
            writeData("	clear: both;");
            writeData("	padding-bottom: 1px;	/* for Gecko-based browsers */");
            writeData("	margin-bottom: -1px;	/* for Gecko-based browsers */");
            writeData("}");
            writeData(".hide");
            writeData("{");
            writeData("	display: none !important;");
            writeData("}");
            writeData(".inside");
            writeData("{");
            writeData("	/* glitch in IE caused by vertical padding in this class, so 0 padding is");
            writeData("	 * set here and those blocks that need the vertical padding must be ");
            writeData("	 * applied to the parent element. the purpose of this class is to provide");
            writeData("	 * horizontal padding without using hacks to get around IE's broken box ");
            writeData("	 * model. so it's okay to apply vertical padding to the parent element, ");
            writeData("	 * just not horizontal padding.");
            writeData("	 */");
            writeData("	padding: 0 1em;");
            writeData("}");
            writeData("/* margin values and font sizes for headings, and margins on paragraphs");
            writeData(" * and lists are not consistent across browser platforms. to achieve a");
            writeData(" * consistent look we need to explicity set these values here. it may");
            writeData(" * seem an odd way to declare the margins like this but you never");
            writeData(" * know what kind of horizontal padding a browser may be using on an");
            writeData(" * element, and I only want to change the vertical padding.");
            writeData(" *");
            writeData(" * pixels are used here, rather than ems, because I want a consistent");
            writeData(" * margin on the different headings. if I use ems, 1em for an h1 element");
            writeData(" * is much larger than 1em on an h6 element. I don't wnat this.");
            writeData(" *");
            writeData(" * salt to taste");
            writeData(" */");
            writeData("ul, ol, dl, p, h1, h2, h3, h4, h5, h6");
            writeData("{");
            writeData("	margin-top: 14px;");
            writeData("	margin-bottom: 14px;");
            writeData("	padding-top: 0;");
            writeData("	padding-bottom: 0;");
            writeData("}");
            writeData("h1");
            writeData("{");
            writeData("	font-size: 220%;");
            writeData("}");
            writeData("h2");
            writeData("{");
            writeData("	font-size: 160%;");
            writeData("	color: #4411A1;");
            writeData("}");
            writeData("h3");
            writeData("{");
            writeData("	font-size: 160%;");
            writeData("	color: #4411A1;");
            writeData("}");
            writeData("h4");
            writeData("{");
            writeData("	font-size: 130%;");
            writeData("	color: #4411A1;");
            writeData("}");
            writeData("h5");
            writeData("{");
            writeData("	margin-top: 0px;");
            writeData("	margin-bottom: 0px;");
            writeData("	font-size: 100%;");
            writeData("	color: #4411A1;");
            writeData("}");
            writeData("h6");
            writeData("{");
            writeData("	font-size: 70%;");
            writeData("	color: #4411A1;");
            writeData("}");
            writeData("/* alter some HTML elements' default style");
            writeData(" */");
            writeData("a, a:link, a:visited, a:active");
            writeData("{");
            writeData("	text-decoration: underline;");
            writeData("	color: #3388CC;");
            writeData("}");
            writeData("a:hover");
            writeData("{");
            writeData("	text-decoration: none;");
            writeData("}");
            writeData("code");
            writeData("{");
            writeData("	font-family: \"Courier New\", Courier, monospace;");
            writeData("}");
            writeData("label");
            writeData("{");
            writeData("	cursor: pointer;");
            writeData("}");
            writeData("table");
            writeData("{");
            writeData("	font-size: 100%;");
            writeData("}");
            writeData("td, th");
            writeData("{");
            writeData("	vertical-align: top;");
            writeData("}");
            writeData("#pageWrapper");
            writeData("{");
            writeData("	border: solid 1px #fff;");
            writeData("	border-width: 0 0px;");
            writeData("	min-width: 0em;	/* IE doens't understand this property. EMs are used");
            writeData("				   so that as the font size increases, the proportional");
            writeData("				   limitations (min-width) increase with it, rather");
            writeData("				   than creating a middle column that can only fit");
            writeData("				   3 or 4 characters in it. */");
            writeData("	width: auto;");
            writeData("}");
            writeData("* html #pageWrapper");
            writeData("{");
            writeData("		word-wrap: break-word;");
            writeData("}");
            writeData("#masthead");
            writeData("{");
            writeData("	border: solid 1px #fff;");
            writeData("	border-width: 1px 0;");
            writeData("	padding: 0.5em;");
            writeData("}");
            writeData("#masthead h1");
            writeData("{");
            writeData("	padding: 0;");
            writeData("	margin: 0;");
            writeData("}");
            writeData("#outerColumnContainer");
            writeData("{");
            writeData("	/* reserves space for the left and right columns. you can use either");
            writeData("	 * padding, margins, or borders, depending on your needs. however you");
            writeData("	 * can use the border method to create a background color for both left");
            writeData("	 * and right columns");
            writeData("	 */");
            writeData("	border-left: solid 15em #fff;");
            writeData("	border-right: solid 0em #fff;");
            writeData("}");
            writeData("#innerColumnContainer");
            writeData("{");
            writeData("	border: solid 1px #fff;");
            writeData("	border-width: 0 0px;");
            writeData("	margin: 0 -1px;		/* compensate for the borders because of");
            writeData("				   100% width declaration */");
            writeData("	width: 100%;");
            writeData("	z-index: 1;");
            writeData("}");
            writeData("#leftColumn, #middleColumn, #rightColumn, * html #SOWrap");
            writeData("{");
            writeData("	overflow: visible;	/* fix for IE italics bug */");
            writeData("	position: relative;	/* fix some rendering issues */");
            writeData("}");
            writeData("#SOWrap");
            writeData("{");
            writeData("	float: left;");
            writeData("	margin: 0 -1px 0 0;");
            writeData("	width: 100%;");
            writeData("	z-index: 3;");
            writeData("}");
            writeData("#middleColumn");
            writeData("{");
            writeData("	float: right;");
            writeData("	margin: 0 0 0 -1px;");
            writeData("	width: 100%;");
            writeData("	z-index: 5;");
            writeData("}");
            writeData("#leftColumn");
            writeData("{");
            writeData("	float: left;");
            writeData("	margin: 0 1px 0 -15em;");
            writeData("	width: 15em;");
            writeData("	z-index: 4;");
            writeData("}");
            writeData("#rightColumn");
            writeData("{");
            writeData("	float: right;");
            writeData("	/*width: 14em;");
            writeData("	margin: 0 -14em 0 1px;*/");
            writeData("	z-index: 2;");
            writeData("}");
            writeData("#footer");
            writeData("{");
            writeData("	border: solid 1px #fff;");
            writeData("	border-width: 1px 0;");
            writeData("	padding: 0.5em;");
            writeData("}");
            writeData("p.fontsize-set");
            writeData("{");
            writeData("	text-align: center;");
            writeData("}");
            writeData("p.fontsize-set img");
            writeData("{");
            writeData("	border-width: 0;");
            writeData("}");
            writeData(".vnav");
            writeData("{");
            writeData("	margin: 1em 0;");
            writeData("}");
            writeData(".vnav ul, .vnav ul li");
            writeData("{");
            writeData("	margin: 0;");
            writeData("	padding: 0;");
            writeData("	list-style-type: none;");
            writeData("	display: block;");
            writeData("}");
            writeData(".vnav ul");
            writeData("{");
            writeData("	border: solid 1px #fff;");
            writeData("	border-bottom-width: 0;");
            writeData("}");
            writeData(".vnav ul li");
            writeData("{");
            writeData("	border-bottom: solid 1px #fff;");
            writeData("}");
            writeData(".vnav ul li, .vnav ul li a");
            writeData("{");
            writeData("	margin: 0;");
            writeData("	display: block;");
            writeData("	padding: 0;");
            writeData("	line-height: normal;");
            writeData("}");
            writeData(".vnav ul li a");
            writeData("{");
            writeData("	display: block;");
            writeData("	padding: 2px 5px 3px 5px;");
            writeData("}");
            writeData(".vnav ul li a, .vnav ul li a:link, .vnav ul li a:visited, .vnav ul li a:active, .vnav ul li a:hover");
            writeData("{");
            writeData("	text-decoration: none;");
            writeData("	cursor: pointer;");
            writeData("}");
            writeData(".vnav h3");
            writeData("{");
            writeData("	margin-bottom: 0;");
            writeData("	padding-bottom: 0;");
            writeData("	font-size: 126%;");
            writeData("}");
            writeData("* html .vnav ul li a/* hide from IE5.0/Win & IE5/Mac */");
            writeData("{");
            writeData("	height: 0.01%;");
            writeData("}");
            writeData("* html .vnav ul");
            writeData("{");
            writeData("	position: relative;	/* IE needs this to fix a rendering problem */");
            writeData("}");
            writeData("/* horizontal navigation elements. create a DIV element with the class hnav");
            writeData(" * and stick one unordered list inside it to generate a horizontal menu.");
            writeData(" */");
            writeData(".hnav");
            writeData("{");
            writeData("	border-bottom: solid 1px #fff;");
            writeData("	text-align: center;");
            writeData("}");
            writeData(".hnav, .hnav ul li a");
            writeData("{");
            writeData("	padding-top: 3px;");
            writeData("	padding-bottom: 4px;");
            writeData("}");
            writeData(".hnav ul, .hnav ul li");
            writeData("{");
            writeData("	display: inline;");
            writeData("	list-style-type: none;");
            writeData("	margin: 0;");
            writeData("	padding: 0;");
            writeData("}");
            writeData(".hnav ul li a");
            writeData("{");
            writeData("	margin: 0 -1px 0 0;");
            writeData("	padding-left: 10px;");
            writeData("	padding-right: 10px;	/* short-hand padding attribute would overwrite");
            writeData("				   top/bottom padding set in a previous rule */");
            writeData("	border-left: solid 1px #000;");
            writeData("	border-right: solid 1px #000;");
            writeData("	white-space: nowrap;");
            writeData("}");
            writeData(".hnav ul li a:link, .hnav ul li a:visited, .hnav ul li a:active, .hnav ul li a:hover");
            writeData("{");
            writeData("	text-decoration: none;");
            writeData("}");
            writeData(".hnav ul li span.divider");
            writeData("{");
            writeData("	display: none;");
            writeData("}");
            writeData("* html .hnav ul li, * html .hnav ul li a");
            writeData("{");
            writeData("	width: 1%; /* IE/Mac needs this */");
            writeData("	display: inline-block;	/* IE/Mac needs this */");
            writeData("		width: auto;");
            writeData("		display: inline;");
            writeData("	/* reset above hack */");
            writeData("}");
            writeData("* html .hnav, * html .hnav ul a");
            writeData("{");
            writeData("	height: 0.01%; /* hasLayout hack to fix render bugs in IE/Win. ");
            writeData("				 IE/Mac will ignore this rule. */");
            writeData("}");
            writeData("* html .HNAV");
            writeData("{");
            writeData("	padding: 0;	/* IE5/Win will resize #hnav to fit the heights of its");
            writeData("			   inline children that have vertical padding. So this");
            writeData("			   incorrect case selector hack will be applied only by");
            writeData("			   IE 5.x/Win */");
            writeData("}");
            writeData("/* everything below this point is related to the page's \"theme\" and could be");
            writeData(" * placed in a separate stylesheet to allow for multiple color/font scemes on");
            writeData(" * the layout. you should probably leave a default theme within this stylesheet");
            writeData(" * just to be on the safe side.	");
            writeData(" */");
            writeData("#pageWrapper, #masthead, #innerColumnContainer, #footer, .vnav ul, .vnav ul li, .hnav, .hnav ul li a");
            writeData("{");
            writeData("	border-color: #565;");
            writeData("}");
            writeData("html, body");
            writeData("{");
            writeData("	/* note that both html and body elements are in the selector.");
            writeData("	 * this is because we have margins applied to the body element");
            writeData("	 * and the HTML's background property will show through if");
            writeData("	 * it is ever set. _DO_NOT_ apply a font-size value to the");
            writeData("	 * html or body elements, set it in #pageWrapper.");
            writeData("	 */");
            writeData("	background-color: #eee;");
            writeData("	color: #000;");
            writeData("    	font-family: Verdana, Arial, Tahoma;");
            writeData("    	font-size: 14px; ");
            writeData("    	margin:0px;");
            writeData("	");
            writeData("}");
            writeData("#pageWrapper");
            writeData("{");
            writeData("	font-size: 80%;	/* set your default font size here. */");
            writeData("}");
            writeData("#masthead");
            writeData("{");
            writeData("	background-color: #4411A1;");
            writeData("	color: #fff;");
            writeData("}");
            writeData("#bannerOuterWrapper {");
            writeData("	height:100px;");
            writeData("	background-color: #fff;");
            writeData("}");
            writeData("#bannerWrapper {");
            writeData("	height:100px;");
            writeData("}");
            writeData("#bannerWrapperRepeat {");
            writeData("	height:100px;");
            writeData("}");
            writeData(".logo_text {");
            writeData("	font-size: 220%;");
            writeData("	font-weight:bold;");
            writeData("	color:#CCCCFF;");
            writeData("}");
            writeData(".logo_body_text {");
            writeData("	font-size: 100%;");
            writeData("	font-weight:bold;");
            writeData("	color:#4411A1;");
            writeData("}");
            writeData(".hnav");
            writeData("{");
            writeData("	background-color: #CCCCFF;");
            writeData("	color: #fff;");
            writeData("	border:0;");
            writeData("}");
            writeData("#outerColumnContainer");
            writeData("{");
            writeData("	border-left-color: #CCCCFF;	/* left column background color */");
            writeData("	border-right-color: #CCCCFF;	/* right column background color */");
            writeData("	background-color: #fff;		/* set the background color for the");
            writeData("					   middle column here */");
            writeData("}");
            writeData(".vnav ul li a:link, .vnav ul li a:visited, .vnav ul li a:active");
            writeData("{");
            writeData("	text-decoration: none;");
            writeData("	background-color: #4411A1;");
            writeData("	color: #FFF;");
            writeData("}");
            writeData("#rightColumn .vnav ul li a:link, #rightColumn .vnav ul li a:visited, #rightColumn .vnav ul li a:active");
            writeData("{");
            writeData("	background-color: #ded;");
            writeData("}");
            writeData(".vnav ul li a:hover, #rightColumn .vnav ul li a:hover");
            writeData("{");
            writeData("	text-decoration: none;");
            writeData("	background-color: #FFFFFF;");
            writeData("	color: #000000;");
            writeData("}");
            writeData(".hnav ul li a:link, .hnav ul li a:visited");
            writeData("{");
            writeData("	background-color: #4411A1;");
            writeData("	color: #fff;");
            writeData("}");
            writeData(".hnav ul li a:hover");
            writeData("{");
            writeData("	background-color: #FFF;");
            writeData("	color: #000;");
            writeData("	text-decoration: none;");
            writeData("}");
            writeData(".login_box");
            writeData("{");
            writeData("    	font-family: Verdana, Arial, Tahoma;");
            writeData("    	font-size: 10px; ");
            writeData("    	border-style: solid;");
            writeData("    	border-width: 3px; ");
            writeData("    	border-color: #4411A1;");
            writeData("	color: #4411A1;");
            writeData("	background-color: #FFF;");
            writeData("	padding-bottom:5px;");
            writeData("	padding-left:3px;");
            writeData("}");
            writeData("input ");
            writeData("{");
            writeData("    	font-family: Verdana, Arial, Tahoma;");
            writeData("    	font-size: 10px; ");
            writeData("    	border-style: solid;");
            writeData("    	border-width: 1px; ");
            writeData("    	border-color: #4411A1;");
            writeData("    	background-color: #FFFFFF;");
            writeData("    	color:#4411A1;");
            writeData("}");
            writeData("select ");
            writeData("{");
            writeData("    	font-family: Verdana, Arial, Tahoma;");
            writeData("    	font-size: 10px; ");
            writeData("    	border-style: solid;");
            writeData("    	border-width: 1px; ");
            writeData("    	border-color: #4411A1;");
            writeData("    	background-color: #FFFFFF;");
            writeData("    	color:#4411A1;");
            writeData("}");
            writeData(".profile_table");
            writeData("{");
            writeData("    	font-family: Verdana, Arial, Tahoma;");
            writeData("    	font-size: 10px; ");
            writeData("    	border-style: solid;");
            writeData("    	border-width: 1px; ");
            writeData("    	border-color: #4411A1;");
            writeData("    	background-color: #FFFFFF;");
            writeData("    	color:#000;");
            writeData("}");
            writeData("#rightColumn .inside");
            writeData("{");
            writeData("	/* if you apply a font size to just #rightColumn, then its width,");
            writeData("	 * which is specified in EMs, will also be affected. you don't want");
            writeData("	 * that. so apply font size changes to the .inside element which exists");
            writeData("	 * inside underneath all three columns");
            writeData("	 */");
            writeData("	font-size: 90%;");
            writeData("}");
            writeData("#rightColumn .inside .vnav");
            writeData("{");
            writeData("	font-size: 110%;");
            writeData("}");
            writeData("#footer");
            writeData("{");
            writeData("	border:0;");
            writeData("	color: #4411A1;");
            writeData("	text-align: center;");
            writeData("}");
            writeData(".input_plain {");
            writeData("	color:#000;");
            writeData("}");
            writeData("/******************************************************************************/");
        }
    }
}
