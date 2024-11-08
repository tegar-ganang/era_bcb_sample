package org.sharefast.core;

import org.sharefast.textsearch.SFIndexer;
import org.sharefast.util.DBAccess;
import org.sharefast.util.TextUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.util.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** 
 * ServerConsoleServlet is a class for controlling all this system.
 * 
 * @author Kazuo Hiekata <hiekata@nakl.t.u-tokyo.ac.jp>
 */
public class ServerConsoleServlet extends HttpServlet {

    /**
	 * 
	 */
    private static final long serialVersionUID = 5236980104838386476L;

    public static final String USERLOGSERVLET = "UserLogServlet";

    public static final String SEARCHSERVLET = "TextSearchServlet";

    public static final String SERVERCONSOLESERVLET = "ServerConsoleServlet";

    public static final String GROUPCONSOLESERVLET = "GroupConsoleServlet";

    public static final String FILEUPLOADSERVLET = "FileUploadServlet";

    public static final String DISCUSSIONSERVLET = "DiscussionServlet";

    public static final String METAEDITSERVLET = "MetaEditServlet";

    public static final String FILEACCESSSERVLET = "FileAccessServlet";

    public static final String MENUSERVLET = "MenuServlet";

    public static final String REVISIONSERVLET = "RevisionServlet";

    private String username = null;

    private String password = null;

    private String message = null;

    private static String OperatingSystemName = null;

    private static File ServletLocalDirectory = null;

    private static File RepositoryLocalDirectory = null;

    private static File ConfigFile = null;

    private static Document ConfigFileXML = null;

    private static Hashtable OrganizationConfigFiles = null;

    private static Hashtable OrganizationUserDatabases = null;

    public static final int LOG_NONE = 0;

    public static final int LOG_CRIT = 1;

    public static final int LOG_ERROR = 2;

    public static final int LOG_WARN = 3;

    public static final int LOG_INFO = 4;

    public static final int LOG_DEBUG = 5;

    public static final int LOG_VDEBUG = 6;

    private static int m_currentLogLevel = ServerConsoleServlet.LOG_INFO;

    private static Properties m_messageProperty = null;

    public static final String TABLE_SESSION = "session";

    public static final String TABLE_THREAD = "thread";

    public static final String TABLE_MESSAGE = "message";

    public static final String TABLE_THREAD_CHANGE_LOG = "thread_status_change_log";

    public static final String OPEN_GIF = "../open.gif";

    public static final String CLOSE_GIF = "../close.gif";

    public static final int STATUS_OPEN = 0;

    public static final int STATUS_CLOSE = 1;

    public static final int STATUS_NULL = 2;

    public static final String[] STATUS_LABEL = { "open", "close", "---" };

    public static final String[] STATUS_IMAGE = { OPEN_GIF, CLOSE_GIF, "" };

    /**
	 * doRequest handles requests from client with some parameters.<br/>
	 * Parameters: <br/>
	 * 	command=initsdb: Rebuild Search Database. <br/>
	 *  command=initsp:  Reset Search Processor. <br/>
	 *  command=initrdf: Reload RDF model from RDF files. <br/>
	 *  command=countlog: Reset CountLog Database of all the organizations.<br/>
	 *  command=getsyslog: Get the system log in html format.<br/>
	 * 
	 * @return void
	 */
    protected void doRequest(HttpServletRequest request, HttpServletResponse response) {
        try {
            request.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        HttpSession session = request.getSession(true);
        message = "";
        username = request.getParameter("username");
        if ((username == null) || (!(username instanceof String)) || username.equals("")) {
            username = (String) session.getAttribute("username");
            if ((username == null) || username.equals("") || (!username.equals(ServerConsoleServlet.getConfigByTagName("AdminUsername")))) {
                try {
                    response.sendError(500, "Invalid user.");
                    return;
                } catch (IOException e) {
                }
            }
        } else {
            session.setAttribute("username", username);
        }
        password = request.getParameter("password");
        if ((password == null) || (!(password instanceof String)) || password.equals("")) {
            password = (String) session.getAttribute("password");
            if ((password == null) || password.equals("") || (!password.equals(ServerConsoleServlet.getConfigByTagName("AdminPassword")))) {
                try {
                    response.sendError(500, "Invalid password.");
                    return;
                } catch (IOException e) {
                }
            }
        } else {
            session.setAttribute("password", password);
        }
        String command = request.getParameter("command");
        if (command == null) command = "unknown";
        if (command.equals("getsyslog")) {
            String systemlogFileName = ServerConsoleServlet.getConfigByTagName("SystemLogFile");
            systemlogFileName = ServerConsoleServlet.convertToAbsolutePath(systemlogFileName);
            String strsize = request.getParameter("size");
            int size = 0;
            try {
                size = Integer.parseInt(strsize);
                message = ServerConsoleServlet.tailSystemLog(size);
            } catch (Exception e) {
                size = 10000;
                message = "Specified size= " + strsize + " is invalid. using size=10000";
            }
        } else if (command.equals("getorglog")) {
            String strorg = request.getParameter("organization");
            if (!ServerConsoleServlet.orgExist(strorg)) message = "Organization <i>" + strorg + "</i> does not exist."; else message = ServerConsoleServlet.printOrgLog(strorg);
        } else if (command.equals("showreport")) {
            String strorg = request.getParameter("org");
            if (!ServerConsoleServlet.orgExist(strorg)) message = "Organization <i>" + strorg + "</i> does not exist."; else {
                ServerConsoleServlet.generateLogfiles(strorg);
                File dir = new File(ServerConsoleServlet.convertToAbsolutePath(ServerConsoleServlet.getConfigByTagName("LogPath") + strorg));
                if (dir.exists()) {
                    String[] children = dir.list();
                    for (int i = 0; i < children.length; i++) {
                        logFilter(new File(dir, children[i]), strorg);
                        reformLog(new File(dir, children[i]), strorg);
                    }
                }
                message = generateReport(strorg);
            }
        } else if (command.equals("uploadfile")) {
            String organization = request.getParameter("org");
            if (!ServerConsoleServlet.orgExist(organization)) message = "Organization <i>" + organization + "</i> does not exist."; else {
                String timeFile = ServerConsoleServlet.getConfigByTagName("LogPath") + File.separator + organization + ".time";
                timeFile = ServerConsoleServlet.convertToAbsolutePath(timeFile);
                File outFile = new File(timeFile);
                String filename = request.getParameter("file");
                File inFile = new File(filename);
                try {
                    copyFile(inFile, outFile);
                    message = "File uploaded: " + filename;
                } catch (Exception e) {
                    ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_ERROR);
                    e.printStackTrace();
                    message = "Upload Failure";
                }
            }
        } else if (command.equals("startclass")) {
            String strorg = request.getParameter("org");
            if (!ServerConsoleServlet.orgExist(strorg)) message = "Organization <i>" + strorg + "</i> does not exist."; else {
                String str = "'start_class', '', '', '', ''" + ", '" + Calendar.getInstance().getTime().toString() + "'";
                DBAccess db = null;
                db = new DBAccess();
                db.printLog(str, strorg);
                message = "Class started at " + Calendar.getInstance().getTime().toString();
            }
        } else if (command.equals("createorg")) {
            String new_organization_name = request.getParameter("new_organization_name");
            this.createOrganization(new_organization_name, request);
        } else if (command.equals("listorg")) {
            String[] orgnames = ServerConsoleServlet.getOrganizationNames();
            for (int i = 0; i < orgnames.length; i++) {
                message += orgnames[i] + "<br>";
            }
        } else if (command.equals("importmodel")) {
            String organization_name = request.getParameter("organization_name");
            String[] orgnames = ServerConsoleServlet.getOrganizationNames();
            for (int i = 0; i < orgnames.length; i++) {
                if (orgnames[i].equals(organization_name)) {
                    File modelxml = new File(ServerConsoleServlet.getRDFLocalDirectory(organization_name), "model.xml.import");
                    ResourceModelHolder.importModel(modelxml, organization_name);
                    message = "import rdf model: " + modelxml.getAbsolutePath();
                    break;
                }
                if (i == orgnames.length - 1) {
                    message = "invalid organization name: " + organization_name;
                }
            }
        } else if (command.equals("exportmodel")) {
            String organization_name = request.getParameter("organization_name");
            String[] orgnames = ServerConsoleServlet.getOrganizationNames();
            for (int i = 0; i < orgnames.length; i++) {
                if (orgnames[i].equals(organization_name)) {
                    File modelxml = new File(ServerConsoleServlet.getRDFLocalDirectory(organization_name), "model.xml.export");
                    ResourceModelHolder.exportModel(modelxml, organization_name);
                    message = "export rdf model: " + modelxml.getAbsolutePath();
                    break;
                }
                if (i == orgnames.length - 1) {
                    message = "invalid organization name: " + organization_name;
                }
            }
        } else if (command.equals("replacemodel")) {
            String organization_name = request.getParameter("organization_name");
            String[] orgnames = ServerConsoleServlet.getOrganizationNames();
            for (int i = 0; i < orgnames.length; i++) {
                if (orgnames[i].equals(organization_name)) {
                    File modelxml = new File(ServerConsoleServlet.getRDFLocalDirectory(organization_name), "model.xml.replace");
                    ResourceModelHolder.replaceModel(modelxml, organization_name);
                    message = "replace rdf model: " + modelxml.getAbsolutePath();
                    break;
                }
                if (i == orgnames.length - 1) {
                    message = "invalid organization name: " + organization_name;
                }
            }
        } else if (command.equals("index")) {
            String organization_name = request.getParameter("organization_name");
            String[] orgnames = ServerConsoleServlet.getOrganizationNames();
            for (int i = 0; i < orgnames.length; i++) {
                if (orgnames[i].equals(organization_name)) {
                    String[] allUris = ResourceModelHolder.getAllUri(organization_name, SF.Document);
                    for (int j = 0; j < allUris.length; j++) {
                        int startIndex = allUris[j].indexOf("&filename=");
                        if (startIndex != -1) {
                            String filename = allUris[j].substring(startIndex + "&filename=".length());
                            File targetFile = new File(ServerConsoleServlet.getRepositoryLocalDirectory().getAbsolutePath() + File.separator + organization_name + File.separator + filename);
                            if (targetFile.exists()) {
                                SFIndexer sfi = new SFIndexer(organization_name);
                                if (SFIndexer.getActiveSFIndexerCount() < 10) {
                                    sfi.addFileToSearchIndexAsync(targetFile, allUris[j]);
                                } else {
                                    sfi.addFileToSearchIndexSync(targetFile, allUris[j]);
                                }
                            }
                        }
                    }
                    message = "initializing search engine. organization name: " + organization_name;
                    break;
                }
                if (i == orgnames.length - 1) {
                    message = "invalid organization name: " + organization_name;
                }
            }
        }
        request.setAttribute("message", message);
        String serveradminjsp = "/jsp/serveradmin.jsp";
        RequestDispatcher rDispatcher = request.getRequestDispatcher(serveradminjsp);
        try {
            rDispatcher.forward(request, response);
        } catch (Exception e) {
            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_ERROR);
            e.printStackTrace();
        }
        return;
    }

    /**
	 * doGet handles request from clients.<br/>
	 * Common processing is provided in BaseServlet Class.
	 * 
	 * @return	void
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doRequest(request, response);
    }

    /**
	 * doPost forwards request.
	 * 
	 * @return	void
	 */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    }

    /**
	 * initialization
	 * 
	 * @return	void
	 */
    public void init() throws ServletException {
        Properties prop = System.getProperties();
        OperatingSystemName = prop.getProperty("os.name");
        if ((OperatingSystemName != null) && (OperatingSystemName instanceof String)) {
            if (OperatingSystemName.toLowerCase().indexOf("windows") >= 0) OperatingSystemName = "windows";
        }
        if ((OperatingSystemName != null) && (OperatingSystemName instanceof String)) {
            if (OperatingSystemName.toLowerCase().indexOf("linux") >= 0) OperatingSystemName = "linux";
        }
        try {
            ServletLocalDirectory = new File(getServletContext().getRealPath(""));
            if ((ServletLocalDirectory == null) || (!ServletLocalDirectory.isDirectory())) {
                System.out.println("Invalid ServletLocalDirectory. ServletLocalDirectory = " + ServletLocalDirectory.getPath());
            }
        } catch (Exception e) {
            System.out.println("Error in setting ServletLocalDirectory.");
            e.printStackTrace();
        }
        String configfilename = this.getInitParameter("configfile").toString();
        configfilename = ServerConsoleServlet.convertToAbsolutePath(configfilename);
        ConfigFile = new File(configfilename);
        if ((ConfigFile == null) || (!ConfigFile.isFile())) {
            System.out.println("Invalid ConfigFile. ConfigFile = " + ConfigFile.getPath());
        }
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            ConfigFileXML = builder.parse(ConfigFile);
        } catch (Exception e) {
            System.out.println("Error in parsing ConfigFile. ConfigFile = " + ConfigFile.getPath());
            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_CRIT);
            e.printStackTrace();
        }
        String repositorypathname = ServerConsoleServlet.getConfigByTagName("RepositoryPath");
        repositorypathname = ServerConsoleServlet.convertToAbsolutePath(repositorypathname);
        RepositoryLocalDirectory = new File(repositorypathname);
        if ((RepositoryLocalDirectory == null) || (!RepositoryLocalDirectory.isDirectory())) {
            System.out.println("Invalid RepositoryLocalDirectory. RepositoryLocalDirectory = " + RepositoryLocalDirectory.getPath());
            ServerConsoleServlet.printSystemLog("Invalid RepositoryLocalDirectory. RepositoryLocalDirectory = " + RepositoryLocalDirectory.getPath(), ServerConsoleServlet.LOG_CRIT);
        }
        m_messageProperty = new Properties();
        String messagepropfile = ServerConsoleServlet.getConfigByTagName("MessagePropertyFile");
        messagepropfile = ServerConsoleServlet.convertToAbsolutePath(messagepropfile);
        try {
            FileInputStream fis = new FileInputStream(new File(messagepropfile));
            m_messageProperty.load(fis);
            fis.close();
        } catch (FileNotFoundException e) {
            ServerConsoleServlet.printSystemLog("cannot find messagepropfile= " + messagepropfile, ServerConsoleServlet.LOG_CRIT);
            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_CRIT);
            e.printStackTrace();
        } catch (IOException e) {
            ServerConsoleServlet.printSystemLog("messagepropfile= " + messagepropfile, ServerConsoleServlet.LOG_CRIT);
            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_CRIT);
        } catch (Exception e) {
            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_CRIT);
        }
        ServerConsoleServlet.printSystemLog("------------- Starting ShareFast Server ------------- ", ServerConsoleServlet.LOG_INFO);
        if ("auto".equals(ServerConsoleServlet.getConfigByTagName("DatabaseType"))) {
            String dbtype = DBAccess.probeDatabaseType();
            ServerConsoleServlet.setConfigByTagName("DatabaseType", dbtype);
            ServerConsoleServlet.printSystemLog("Database Auto Configuration Completed. DatabaseType = " + dbtype, ServerConsoleServlet.LOG_INFO);
        }
        ResourceModelHolder.initialize();
        SchemaModelHolder.initialize();
        ServerConsoleServlet.setLogLevel(Integer.parseInt(getConfigByTagName("LogLevel")));
        String systemlogFileName = ServerConsoleServlet.getConfigByTagName("SystemLogFile");
        systemlogFileName = ServerConsoleServlet.convertToAbsolutePath(systemlogFileName);
        File systemlogFile = new File(systemlogFileName);
        if (systemlogFile.exists() && !systemlogFile.isFile()) {
            System.err.println("systemlogFile exists, but is not an ordinary file. systemlogFile= " + systemlogFile.getAbsolutePath());
        }
        if (!systemlogFile.exists()) {
            System.err.println("systemlogFile doesn't exist, create it. systemlogFile= " + systemlogFile.getAbsolutePath());
        }
        OrganizationConfigFiles = new Hashtable();
        OrganizationUserDatabases = new Hashtable();
        try {
            File organizationConfigFile = new File(ServerConsoleServlet.convertToAbsolutePath("WEB-INF/conf/organization_template/conf/organization-config.xml"));
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(organizationConfigFile);
            OrganizationConfigFiles.put("default", doc);
        } catch (Exception e) {
            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_CRIT);
            e.printStackTrace();
        }
        String[] organizationNames = ServerConsoleServlet.getOrganizationNames();
        for (int i = 0; i < organizationNames.length; i++) {
            File organizationConfigFile = new File(ServerConsoleServlet.getConfLocalDirectory(organizationNames[i]) + File.separator + "organization-config.xml");
            if (organizationConfigFile.exists()) {
                try {
                    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document doc = builder.parse(organizationConfigFile);
                    OrganizationConfigFiles.put(organizationNames[i], doc);
                } catch (Exception e) {
                    System.out.println("Error in parsing organizationConfigFile. organizationConfigFile = " + organizationConfigFile.getAbsolutePath());
                    ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_CRIT);
                    e.printStackTrace();
                }
            } else {
                ServerConsoleServlet.printSystemLog("skip organizationConfigFile=" + organizationConfigFile.getAbsolutePath(), ServerConsoleServlet.LOG_DEBUG);
            }
            File organizationUserFile = new File(ServerConsoleServlet.getConfLocalDirectory(organizationNames[i]) + File.separator + "sharefast-users.xml");
            if (organizationUserFile.exists()) {
                try {
                    OrganizationUserDatabases.put(organizationNames[i], new UserDatabase(organizationUserFile));
                } catch (Exception e) {
                    System.out.println("Error in parsing organizationUserFile. organizationUserFile = " + organizationUserFile.getAbsolutePath());
                    ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_CRIT);
                    e.printStackTrace();
                }
            } else {
                ServerConsoleServlet.printSystemLog("skip organizationUserFile=" + organizationUserFile.getAbsolutePath(), ServerConsoleServlet.LOG_DEBUG);
            }
            File backupModelXmlFile = new File(ServerConsoleServlet.getRDFLocalDirectory(organizationNames[i]), "model-" + TextUtil.getNewFilename("xml"));
            ResourceModelHolder.exportModel(backupModelXmlFile, organizationNames[i]);
        }
        ServerConsoleServlet.printSystemLog("ConfigFile= " + ConfigFile.getPath(), ServerConsoleServlet.LOG_INFO);
        ServerConsoleServlet.printSystemLog("System Type = " + ServerConsoleServlet.getOperatingSystemName(), ServerConsoleServlet.LOG_INFO);
        ServerConsoleServlet.printSystemLog("DatabaseType= " + getConfigByTagName("DatabaseType"), ServerConsoleServlet.LOG_INFO);
        ServerConsoleServlet.printSystemLog("DatabaseServer= " + getConfigByTagName("DatabaseServer"), ServerConsoleServlet.LOG_INFO);
        ServerConsoleServlet.printSystemLog("DatabaseUser= " + getConfigByTagName("DatabaseUser"), ServerConsoleServlet.LOG_INFO);
        ServerConsoleServlet.printSystemLog("DocumentRespositoryPath= " + ServerConsoleServlet.getRepositoryLocalDirectory().getAbsolutePath(), ServerConsoleServlet.LOG_INFO);
        ServerConsoleServlet.printSystemLog("ServletLocalDirectory= " + ServletLocalDirectory.getAbsolutePath(), ServerConsoleServlet.LOG_INFO);
        ServerConsoleServlet.printSystemLog("SystemLogFile= " + systemlogFile.getAbsolutePath(), ServerConsoleServlet.LOG_INFO);
        ServerConsoleServlet.printSystemLog("MessagePropFile= " + messagepropfile, ServerConsoleServlet.LOG_INFO);
    }

    /**
     * save current rdf model. <br>
     * 
     * @return void
     */
    public void finalize() {
        String[] organizationNames = ServerConsoleServlet.getOrganizationNames();
        for (int i = 0; i < organizationNames.length; i++) {
            File backupModelXmlFile = new File(ServerConsoleServlet.getRDFLocalDirectory(organizationNames[i]), "model-" + TextUtil.getNewFilename("xml"));
            ResourceModelHolder.exportModel(backupModelXmlFile, organizationNames[i]);
            File ModelXmlFile = new File(ServerConsoleServlet.getRDFLocalDirectory(organizationNames[i]), "model.xml");
            ResourceModelHolder.exportModel(ModelXmlFile, organizationNames[i]);
        }
    }

    /**
	 * convertToAbsolutePath returns AbsolutePath of filename.
	 * 
	 * @param	filename	filename which you want to convert
	 * 						e.g. WEB-INF/conf/utess-system.log
	 * @return	String
	 */
    private synchronized boolean createOrganization(String organizationName, HttpServletRequest req) {
        if ((organizationName == null) || (organizationName.equals(""))) {
            message = "invalid new_organization_name.";
            return false;
        }
        String tmpxml = TextUtil.xmlEscape(organizationName);
        String tmpdb = DBAccess.SQLEscape(organizationName);
        if ((!organizationName.equals(tmpxml)) || (!organizationName.equals(tmpdb)) || (!TextUtil.isValidFilename(organizationName))) {
            message = "invalid new_organization_name.";
            return false;
        }
        if ((organizationName.indexOf('-') > -1) || (organizationName.indexOf(' ') > -1)) {
            message = "invalid new_organization_name.";
            return false;
        }
        String[] orgnames = ServerConsoleServlet.getOrganizationNames();
        for (int i = 0; i < orgnames.length; i++) {
            if (orgnames.equals(organizationName)) {
                message = "already exists.";
                return false;
            }
        }
        message = "create new organization: " + organizationName;
        File newOrganizationDirectory = new File(ServerConsoleServlet.RepositoryLocalDirectory.getAbsolutePath() + File.separator + organizationName);
        if (!newOrganizationDirectory.mkdir()) {
            message = "cannot create directory.";
            return false;
        }
        File cacheDir = new File(newOrganizationDirectory.getAbsolutePath() + File.separator + ServerConsoleServlet.getConfigByTagName("CacheDirName"));
        cacheDir.mkdir();
        File confDir = new File(newOrganizationDirectory.getAbsolutePath() + File.separator + ServerConsoleServlet.getConfigByTagName("ConfDirName"));
        confDir.mkdir();
        File rdfDir = new File(newOrganizationDirectory.getAbsolutePath() + File.separator + ServerConsoleServlet.getConfigByTagName("RDFDirName"));
        rdfDir.mkdir();
        File resourceDir = new File(newOrganizationDirectory.getAbsolutePath() + File.separator + ServerConsoleServlet.getConfigByTagName("ResourceDirName"));
        resourceDir.mkdir();
        File obsoleteDir = new File(resourceDir.getAbsolutePath() + File.separator + "obsolete");
        obsoleteDir.mkdir();
        File schemaDir = new File(newOrganizationDirectory.getAbsolutePath() + File.separator + ServerConsoleServlet.getConfigByTagName("SchemaDirName"));
        schemaDir.mkdir();
        String organization_temp_dir = ServerConsoleServlet.convertToAbsolutePath(ServerConsoleServlet.getConfigByTagName("OrganizationTemplate"));
        File templ = new File(organization_temp_dir);
        File[] confFiles = templ.listFiles();
        for (int i = 0; i < confFiles.length; i++) {
            try {
                FileReader fr = new FileReader(confFiles[i]);
                FileWriter fw = new FileWriter(confDir.getAbsolutePath() + File.separator + confFiles[i].getName());
                int c = -1;
                while ((c = fr.read()) != -1) fw.write(c);
                fw.flush();
                fw.close();
                fr.close();
            } catch (IOException e) {
            }
        }
        SchemaModelHolder.reloadSchemaModel(organizationName);
        ResourceModelHolder.reloadResourceModel(organizationName);
        UserLogServlet.initializeUserLogDB(organizationName);
        MetaEditServlet.createNewProject(organizationName, "standard", MetaEditServlet.convertProjectIdToProjectUri(organizationName, "standard", req), this.username);
        ResourceModelHolder.reloadResourceModel(organizationName);
        message = organizationName + " is created. Restart Tomcat to activate this organization.";
        return true;
    }

    /**
	 * BE CAREFULL when accessing OrganizationUserDatabases in write mode.
	 * @return Returns the repositoryLocalDirectory.
	 */
    public static synchronized UserDatabase getUserDatabase(String organizationname) {
        return (UserDatabase) OrganizationUserDatabases.get(organizationname);
    }

    /**
	 * @return null
	 */
    public static synchronized void putUserDatabase(String organizationname, UserDatabase ud) {
        Object obj = OrganizationUserDatabases.get(organizationname);
        if (obj == null) {
            ServerConsoleServlet.printSystemLog("cannot find organization = " + organizationname, ServerConsoleServlet.LOG_ERROR);
            return;
        }
        OrganizationUserDatabases.remove(organizationname);
        OrganizationUserDatabases.put(organizationname, ud);
        return;
    }

    /**
	 * convertToAbsolutePath returns AbsolutePath of filename.
	 * 
	 * @param	filename	filename which you want to convert
	 * 						e.g. WEB-INF/conf/utess-system.log
	 * @return	String
	 */
    public static String convertToAbsolutePath(String filename) {
        String fullfilename = replaceFileSeparator(filename);
        if (new File(fullfilename).canRead() || new File(fullfilename).canWrite() || new File(fullfilename).exists()) {
            fullfilename = new File(fullfilename).getAbsolutePath();
        } else {
            fullfilename = ServletLocalDirectory.getAbsolutePath() + File.separator + replaceFileSeparator(fullfilename);
            if (new File(fullfilename).canRead() || new File(fullfilename).canWrite() || new File(fullfilename).exists()) {
                fullfilename = new File(fullfilename).getAbsolutePath();
                if (new File(fullfilename).isDirectory()) fullfilename = fullfilename + File.separator;
            }
        }
        return fullfilename;
    }

    /**
	 * getConfigByTagName returns values in configfile specified by tagname.
	 * 
	 * @param	tagname	this must be TagName of configfile
	 * 						e.g. AccessLogFile
	 * @return	String
	 */
    public static String getConfigByTagName(String tagname) {
        NodeList nl = ConfigFileXML.getElementsByTagName(tagname);
        if (nl.getLength() <= 0) {
            ServerConsoleServlet.printSystemLog("Unknown tagname is requested. tagname= " + tagname, ServerConsoleServlet.LOG_WARN);
            return "";
        }
        if (!nl.item(0).hasChildNodes()) {
            ServerConsoleServlet.printSystemLog("Specified Tag doesn't have NodeValue. tagname= " + tagname, ServerConsoleServlet.LOG_WARN);
            return "";
        }
        String ret = ((Node) nl.item(0)).getFirstChild().getNodeValue();
        if (ret == null) {
            ServerConsoleServlet.printSystemLog("ret == null for tagname= " + tagname, ServerConsoleServlet.LOG_WARN);
            return "";
        }
        return ret;
    }

    /**
	 * setConfigByTagName sets values in configfile specified by tagname. <br>
	 * This doesn't changes xml file, just change ConfigXML on RAM.
	 * 
	 * @param	tagname	this must be TagName of configfile
	 * 						e.g. AccessLogFile
	 * @return	String
	 */
    public static void setConfigByTagName(String tagname, String value) {
        NodeList nl = ConfigFileXML.getElementsByTagName(tagname);
        if (nl.getLength() <= 0) {
            ServerConsoleServlet.printSystemLog("Unknown tagname is requested. tagname= " + tagname, ServerConsoleServlet.LOG_WARN);
            return;
        }
        if (!nl.item(0).hasChildNodes()) {
            ServerConsoleServlet.printSystemLog("Specified Tag doesn't have NodeValue. tagname= " + tagname, ServerConsoleServlet.LOG_WARN);
            return;
        }
        ((Node) nl.item(0)).getFirstChild().setNodeValue(value);
        return;
    }

    /**
	 * deleteDir Delete directory <br>
	 * 
	 * @return	boolean
	 */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    /**
	 * getResultRecord Get string result from specific file name and key <br>
	 * 
	 * @return	String
	 */
    public static String getResultRecord(String fileName, String strKey, int inxResult) {
        File targetFile = new File(fileName);
        String strResult = null;
        try {
            FileInputStream fis = new FileInputStream(targetFile);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.split(",")[0].equals(strKey)) strResult = line.split(",")[inxResult];
            }
            br.close();
            isr.close();
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        return strResult;
    }

    /**
	 * orgExist check if the organization exists <br>
	 * 
	 * @return	boolean
	 */
    public static boolean orgExist(String strorg) {
        boolean orgfound = false;
        String[] orgnames = ServerConsoleServlet.getOrganizationNames();
        for (int i = 0; i < orgnames.length; i++) {
            if (orgnames[i].equals(strorg)) orgfound = true;
        }
        return orgfound;
    }

    /**
	 * printOrgLog Print the specific organization log <br>
	 * 
	 * @return	string
	 */
    public static String printOrgLog(String organization) {
        String logFile = ServerConsoleServlet.getConfigByTagName("LogPath") + File.separator + organization + ".log";
        logFile = ServerConsoleServlet.convertToAbsolutePath(logFile);
        File targetFile = new File(logFile);
        String readstr = null;
        try {
            FileInputStream fis = new FileInputStream(targetFile);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                readstr = readstr + line + "<br>\n";
            }
            br.close();
            isr.close();
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        return readstr;
    }

    /**
	 * generateLogfiles generates log file for each user kept in organization log <br>
	 * 
	 * @return	none
	 */
    public static void generateLogfiles(String organization) {
        String logFile = ServerConsoleServlet.getConfigByTagName("LogPath") + File.separator + organization + ".log";
        logFile = ServerConsoleServlet.convertToAbsolutePath(logFile);
        File targetFile = new File(logFile);
        try {
            FileInputStream fis = new FileInputStream(targetFile);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String[] spline = null;
            String line, res, user, time, logtype = null;
            DBAccess db = null;
            db = new DBAccess();
            while ((line = br.readLine()) != null) {
                spline = line.split("', '");
                logtype = spline[0].substring(1);
                user = spline[2];
                time = spline[5].substring(11, 19);
                if (logtype.equals("start_class")) {
                    ServerConsoleServlet.deleteDir(new File(ServerConsoleServlet.convertToAbsolutePath(ServerConsoleServlet.getConfigByTagName("LogPath") + organization)));
                } else if (logtype.equals("readfile")) {
                    res = spline[1].substring(spline[1].lastIndexOf("/") + 1);
                    if (res.substring(res.lastIndexOf(".") + 1).equals("sfw") || res.substring(res.lastIndexOf(".") + 1).equals("lock")) continue;
                    db.printorgLog(res + "," + time, organization, user + ".log");
                }
            }
            br.close();
            isr.close();
            fis.close();
            return;
        } catch (FileNotFoundException e) {
            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_ERROR);
            e.printStackTrace();
            return;
        } catch (IOException e) {
            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_ERROR);
            e.printStackTrace();
            return;
        }
    }

    /**
	 * createReportRow makes graphical report for each row <br>
	 * 
	 * @return	String
	 */
    public static String createReportRow(File targetFile, String username, int pixelPerMin, String organization) {
        String strReport = "";
        int totalMin = 0;
        try {
            FileInputStream fis = new FileInputStream(targetFile);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            String lastTime = null;
            String lastRes = null;
            String lastStep = null;
            int i = 0;
            strReport = strReport + "<tr><td width=\"70\">" + username + "</td>\n";
            strReport = strReport + "<td>";
            while ((line = br.readLine()) != null) {
                if (line.split(",")[2].equals("not_finished") || line.split(",")[2].equals("finished")) break;
                i++;
                totalMin = totalMin + Integer.parseInt(line.split(",")[1]);
                lastRes = line.split(",")[0];
                lastTime = line.split(",")[1];
                lastStep = line.split(",")[2];
                strReport = strReport + "<img src=\"..\\img\\" + i + ".gif\" alt=\"" + lastTime + " minutes\" border=1 width=\"" + Integer.parseInt(lastTime) * pixelPerMin + "\" height=\"35\">";
            }
            strReport = strReport + " " + totalMin + " min. (" + line.split(",")[2] + ")<br>\n";
            if (line.split(",")[2].equals("not_finished")) {
                String timeFile = ServerConsoleServlet.getConfigByTagName("LogPath") + File.separator + organization + ".time";
                timeFile = ServerConsoleServlet.convertToAbsolutePath(timeFile);
                strReport = strReport + "has performed <i>" + lastStep + "</i> for " + lastTime + " min. ";
                strReport = strReport + "(expected time = " + getResultRecord(timeFile, lastRes, 1) + " min.)";
            }
            strReport = strReport + "</td></tr>\n";
            br.close();
            isr.close();
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        return strReport;
    }

    /**
	 * generateReport makes graphical report for this organization <br>
	 * 
	 * @return	String
	 */
    public static String generateReport(String organization) {
        String strReport1, strReport2 = "";
        strReport1 = "<h2>Real time report of " + organization + "</h2><br><br>\n";
        int totalMin = 0;
        String timeFile = ServerConsoleServlet.getConfigByTagName("LogPath") + File.separator + organization + ".time";
        timeFile = ServerConsoleServlet.convertToAbsolutePath(timeFile);
        File targetFile = new File(timeFile);
        try {
            FileInputStream fis = new FileInputStream(targetFile);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            int i = 0;
            int j = 0;
            strReport2 = "<table width=\"1000\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n";
            while ((line = br.readLine()) != null) {
                if (line.split(",")[2].equals("finished")) break;
                i++;
                j++;
                if (i == 13) i = 1;
                totalMin = totalMin + Integer.parseInt(line.split(",")[1]);
                if ((j - 1) % 4 == 0) strReport2 = strReport2 + "<tr>\n";
                strReport2 = strReport2 + "<td width=\"40\"><img src=\"..\\img\\" + i + ".gif\" border=1 width=\"20\" height=\"20\"></td>\n";
                strReport2 = strReport2 + "<td width=\"210\">" + line.split(",")[2] + "</td>\n";
                if (j % 4 == 0) strReport2 = strReport2 + "</tr>\n";
            }
            if (j % 4 != 0) strReport2 = strReport2 + "</tr>\n";
            strReport2 = strReport2 + "</table>\n";
            br.close();
            isr.close();
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        int pixelPerMin = 750 / totalMin;
        strReport1 = strReport1 + "<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n";
        strReport1 = strReport1 + createReportRow(new File(timeFile), "Expected time", pixelPerMin, organization);
        File dir = new File(ServerConsoleServlet.convertToAbsolutePath(ServerConsoleServlet.getConfigByTagName("LogPath") + organization));
        if (dir.exists()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                strReport1 = strReport1 + createReportRow(new File(dir, children[i]), children[i].substring(0, children[i].lastIndexOf(".")), pixelPerMin, organization);
            }
        }
        strReport1 = strReport1 + "</table><br><br>\n";
        return strReport1 + strReport2;
    }

    /**
	 * reformLog changes log file format <br>
	 * 
	 * @return	void
	 */
    public static void reformLog(File userlog, String organization) {
        long diff;
        String pStep = null;
        String pStepname = null;
        String tStep = null;
        String tStepname = null;
        String pTime = null;
        String tTime = null;
        String tempLog = userlog.toString() + "tmp";
        File tempLogFile = new File(tempLog);
        DBAccess db = null;
        db = new DBAccess();
        try {
            FileInputStream fis = new FileInputStream(userlog);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                if (!tempLogFile.exists()) {
                    pStep = line.split(",")[0];
                    pTime = line.split(",")[1];
                    pStepname = line.split(",")[2];
                    line = br.readLine();
                    tStep = line.split(",")[0];
                    tTime = line.split(",")[1];
                    tStepname = line.split(",")[2];
                    GregorianCalendar d1 = new GregorianCalendar(2007, 1, 1, Integer.parseInt(tTime.substring(0, 2)), Integer.parseInt(tTime.substring(3, 5)), Integer.parseInt(tTime.substring(6, 8)));
                    GregorianCalendar d2 = new GregorianCalendar(2007, 1, 1, Integer.parseInt(pTime.substring(0, 2)), Integer.parseInt(pTime.substring(3, 5)), Integer.parseInt(pTime.substring(6, 8)));
                    diff = (d1.getTime().getTime() - d2.getTime().getTime()) / 60000;
                    db.printorgLog(pStep + "," + diff + "," + pStepname, organization, tempLogFile.getName());
                    pStep = tStep;
                    pTime = tTime;
                    pStepname = tStepname;
                    if (pStep.equals("current")) {
                        db.printorgLog("not_finished,not_finished,not_finished", organization, tempLogFile.getName());
                        break;
                    } else if (pStep.equals("finish")) {
                        db.printorgLog("finished,finished,finished", organization, tempLogFile.getName());
                        break;
                    }
                } else {
                    tStep = line.split(",")[0];
                    tTime = line.split(",")[1];
                    tStepname = line.split(",")[2];
                    GregorianCalendar d1 = new GregorianCalendar(2007, 1, 1, Integer.parseInt(tTime.substring(0, 2)), Integer.parseInt(tTime.substring(3, 5)), Integer.parseInt(tTime.substring(6, 8)));
                    GregorianCalendar d2 = new GregorianCalendar(2007, 1, 1, Integer.parseInt(pTime.substring(0, 2)), Integer.parseInt(pTime.substring(3, 5)), Integer.parseInt(pTime.substring(6, 8)));
                    diff = (d1.getTime().getTime() - d2.getTime().getTime()) / 60000;
                    db.printorgLog(pStep + "," + diff + "," + pStepname, organization, tempLogFile.getName());
                    if (tStep.equals("current")) {
                        db.printorgLog("not_finished,not_finished,not_finished", organization, tempLogFile.getName());
                        break;
                    } else if (tStep.equals("finish")) {
                        db.printorgLog("finished,finished,finished", organization, tempLogFile.getName());
                        break;
                    } else {
                        pStep = tStep;
                        pTime = tTime;
                        pStepname = tStepname;
                    }
                }
            }
            br.close();
            isr.close();
            fis.close();
            userlog.delete();
            tempLogFile.renameTo(userlog);
            return;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
	 * copyFile copies src file to dst file <br>
	 * 
	 * @return	none
	 */
    public static void copyFile(File src, File dst) throws IOException {
        deleteDir(dst);
        try {
            FileInputStream fis = new FileInputStream(src);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                try {
                    FileOutputStream fos = new FileOutputStream(dst, true);
                    OutputStreamWriter osr = new OutputStreamWriter(fos, "UTF-8");
                    PrintWriter pw = new PrintWriter(osr);
                    pw.println(line);
                    pw.close();
                    osr.close();
                    fos.close();
                } catch (FileNotFoundException e) {
                    ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_ERROR);
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_ERROR);
                    e.printStackTrace();
                    return;
                }
            }
            br.close();
            isr.close();
            fis.close();
        } catch (FileNotFoundException e) {
            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_ERROR);
            e.printStackTrace();
            return;
        } catch (IOException e) {
            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_ERROR);
            e.printStackTrace();
            return;
        }
    }

    /**
	 * logFilter reforms user log file <br>
	 * 
	 * @return	none
	 */
    public static void logFilter(File userlog, String organization) {
        String timeFile = ServerConsoleServlet.getConfigByTagName("LogPath") + File.separator + organization + ".time";
        timeFile = ServerConsoleServlet.convertToAbsolutePath(timeFile);
        File targetFile = new File(timeFile);
        String tempLog = userlog.toString() + "tmp";
        File tempLogFile = new File(tempLog);
        DBAccess db = null;
        db = new DBAccess();
        try {
            FileInputStream fis = new FileInputStream(targetFile);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            String nextFile = null;
            String nextStep = null;
            String readFile = null;
            boolean perform = false;
            String lastTime = "00:00:00";
            while ((line = br.readLine()) != null) {
                nextFile = line.split(",")[0];
                nextStep = line.split(",")[2];
                try {
                    FileInputStream fis2 = new FileInputStream(userlog);
                    InputStreamReader isr2 = new InputStreamReader(fis2, "UTF-8");
                    BufferedReader br2 = new BufferedReader(isr2);
                    String linea = null;
                    perform = false;
                    while ((linea = br2.readLine()) != null) {
                        readFile = linea.split(",")[0];
                        if (readFile.equals(nextFile)) {
                            if (!tempLogFile.exists()) {
                                db.printorgLog(linea + "," + nextStep, organization, tempLogFile.getName());
                                perform = true;
                                lastTime = linea.split(",")[1];
                                break;
                            } else {
                                GregorianCalendar d1 = new GregorianCalendar(2007, 1, 1, Integer.parseInt(lastTime.substring(0, 2)), Integer.parseInt(lastTime.substring(3, 5)), Integer.parseInt(lastTime.substring(6, 8)));
                                GregorianCalendar d2 = new GregorianCalendar(2007, 1, 1, Integer.parseInt(linea.split(",")[1].substring(0, 2)), Integer.parseInt(linea.split(",")[1].substring(3, 5)), Integer.parseInt(linea.split(",")[1].substring(6, 8)));
                                long diff = (d2.getTime().getTime() - d1.getTime().getTime()) / 60000;
                                if (diff >= 1) {
                                    if (nextStep.equals("finished")) {
                                        db.printorgLog("finish," + linea.split(",")[1] + ",finish", organization, tempLogFile.getName());
                                        perform = true;
                                    } else {
                                        db.printorgLog(linea + "," + nextStep, organization, tempLogFile.getName());
                                        perform = true;
                                        lastTime = linea.split(",")[1];
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    br2.close();
                    isr2.close();
                    fis2.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                if (!perform) break;
            }
            if (!readFile.equals("finished")) {
                if (tempLogFile.exists()) db.printorgLog("current," + Calendar.getInstance().getTime().toString().substring(11, 19) + ",current", organization, tempLogFile.getName());
            }
            userlog.delete();
            tempLogFile.renameTo(userlog);
            br.close();
            isr.close();
            fis.close();
            return;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
	 * This function is used for authentication process.
	 * 
	 * @param	organizationname
	 * @param	username
	 * @param	password
	 * 
	 * @return	0: normal user
	 * 			1: administrator
	 * 			-1: not authenticated			
	 */
    public static int authenticate(String organizationname, String username, String password) {
        if (OrganizationUserDatabases == null) {
            ServerConsoleServlet.printSystemLog("OrganizationUserDatabases is null", ServerConsoleServlet.LOG_ERROR);
            return -1;
        }
        UserDatabase userdatabase = null;
        synchronized (OrganizationUserDatabases) {
            userdatabase = (UserDatabase) OrganizationUserDatabases.get(organizationname);
            if (userdatabase == null) {
                ServerConsoleServlet.printSystemLog("organization not found in organizationUserXML organizationname=" + organizationname, ServerConsoleServlet.LOG_DEBUG);
                userdatabase = (UserDatabase) OrganizationUserDatabases.get("default");
                if (userdatabase == null) {
                    ServerConsoleServlet.printSystemLog("userfile not found.", ServerConsoleServlet.LOG_ERROR);
                    return -1;
                }
            }
        }
        return userdatabase.authenticate(username, password);
    }

    /**
	 * getOrganizationConfigByTagName returns values in configfile specified by tagname and organizationname.
	 * 
	 * @param	tagname	this must be TagName of configfile for some organization
	 * 						e.g. JSPPath
	 * @return	String
	 */
    public static String getOrganizationConfigByTagName(String tagname, String organizationname) {
        if (OrganizationConfigFiles == null) {
            ServerConsoleServlet.printSystemLog("OrganizationConfigFiles is null", ServerConsoleServlet.LOG_ERROR);
            return "";
        }
        Document organizationConfigXML = (Document) OrganizationConfigFiles.get(organizationname);
        if (organizationConfigXML == null) {
            ServerConsoleServlet.printSystemLog("organization not found in OrganizationConfigFiles organizationname=" + organizationname, ServerConsoleServlet.LOG_DEBUG);
            organizationConfigXML = (Document) OrganizationConfigFiles.get("default");
            if (organizationConfigXML == null) {
                ServerConsoleServlet.printSystemLog("default config file not found.", ServerConsoleServlet.LOG_DEBUG);
                return "";
            }
        }
        NodeList nl = organizationConfigXML.getElementsByTagName(tagname);
        if (nl.getLength() <= 0) {
            ServerConsoleServlet.printSystemLog("Unknown tagname is requested. tagname= " + tagname, ServerConsoleServlet.LOG_WARN);
            return "";
        }
        if (!nl.item(0).hasChildNodes()) {
            ServerConsoleServlet.printSystemLog("Specified Tag doesn't have NodeValue. tagname= " + tagname, ServerConsoleServlet.LOG_WARN);
            return "";
        }
        String ret = ((Node) nl.item(0)).getFirstChild().getNodeValue();
        if (ret == null) {
            ServerConsoleServlet.printSystemLog("ret == null for tagname= " + tagname, ServerConsoleServlet.LOG_WARN);
            return "";
        }
        return ret;
    }

    /**
	 * getMessageProperty returns properties defined in property file.
	 * 
	 * @param	propname	Property name specified in Property File.
	 * @return	String
	 */
    public static String getMessageProperty(String propname) {
        try {
            String propstr = m_messageProperty.getProperty(propname);
            if (propstr == null) {
                ServerConsoleServlet.printSystemLog("not found Property propname= " + propname, ServerConsoleServlet.LOG_WARN);
                propstr = "";
            }
            return propstr;
        } catch (Exception e) {
            ServerConsoleServlet.printSystemLog("cannot get Property propname= " + propname, ServerConsoleServlet.LOG_ERROR);
            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_ERROR);
            return "";
        }
    }

    /**
	 * Set LogLevel(m_currentLogLevel) of this log facility. 
	 * LOG_NONE(0): Never report logs
	 * LOG_VDEBUG(6): Report all the meesages
	 * @param loglevel
	 * @return	void
	 */
    private static void setLogLevel(int loglevel) {
        if ((loglevel < 0) || (loglevel > LOG_VDEBUG)) {
            ServerConsoleServlet.printSystemLog("setLogLevel: invalid LogLevel: " + Integer.toString(loglevel), ServerConsoleServlet.LOG_WARN);
        } else {
            ServerConsoleServlet.printSystemLog("setLogLevel: Change LogLevel from: " + Integer.toString(m_currentLogLevel) + " to: " + Integer.toString(loglevel), ServerConsoleServlet.LOG_INFO);
            m_currentLogLevel = loglevel;
        }
    }

    /**
	 * Replace file separators. <br/>
	 * 
	 * @deprecated moved to util.Textutil class.
	 * @param in
	 * @return	String
	 */
    public static String replaceFileSeparator(String in) {
        String out = null;
        String ostype = ServerConsoleServlet.getOperatingSystemName();
        if (ostype != null && ostype.equals("windows")) {
            out = in.replace('/', '\\');
        } else if (ostype != null && ostype.equals("linux")) {
            out = in.replace('\\', '/');
        }
        return out;
    }

    /**
	 * tailSystemLog gets last bytes in LogFile.
	 * This command is similar to "tail" command in UNIX.
	 * 
	 * @param bytes		specify the number of bytes
	 * @return String
	 */
    private static synchronized String tailSystemLog(int bytes) {
        String systemlogFileName = ServerConsoleServlet.getConfigByTagName("SystemLogFile");
        systemlogFileName = ServerConsoleServlet.convertToAbsolutePath(systemlogFileName);
        File systemlogFile = new File(systemlogFileName);
        if (systemlogFile.exists() && !systemlogFile.isFile()) {
            System.err.println("systemlogFile exists, but is not an ordinary file. systemlogFile= " + systemlogFile.getAbsolutePath());
            return "";
        }
        try {
            if (!systemlogFile.exists()) {
                System.err.println("systemlogFile doesn't exist, create it. systemlogFile= " + systemlogFile.getAbsolutePath());
            }
            int filesize = (int) systemlogFile.length();
            int skipsize = filesize - bytes;
            if (skipsize <= 0) {
                bytes = filesize;
                skipsize = 0;
            }
            FileInputStream fis = new FileInputStream(systemlogFile);
            fis.skip(skipsize);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            char[] buf = new char[bytes + 1];
            br.read(buf);
            br.close();
            return (new String(buf)).replaceAll("\n", "<br/>");
        } catch (FileNotFoundException e) {
            System.err.println("tailSystemLog: This Program is NEVER Reached Here!.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("tailSystemLog: ERROR.");
            e.printStackTrace();
        }
        return "";
    }

    /**
	 * TODO: Should move to log4j.
	 * Print messages to LogFile if the priority(=loglevel) <br>
	 * is higher than current loglevel(=m_currentLogLevel)  <br>
	 * <pre>
	 * LOG_NONE = 0: Never Report Log 
	 * LOG_CRIT = 1: Report only critlcal messages
	 * LOG_ERROR = 2: Report only errors = "expected errors/exceptions, etc. "
	 * LOG_WARN = 3: Report warning = "important operation, etc. "
	 * LOG_INFO = 4: Report information = "login info, etc. "
	 * LOG_DEBUG = 5: Report debug information
	 * LOG_VDEBUG = 6: Verbose debug mode
	 * </pre>
	 * 
	 * @param	str
	 * @param	loglevel
	 * @return	void
	 */
    public static synchronized void printSystemLog(String str, int loglevel) {
        if (loglevel > m_currentLogLevel) return;
        String systemlogFile = ServerConsoleServlet.getConfigByTagName("SystemLogFile");
        systemlogFile = ServerConsoleServlet.convertToAbsolutePath(systemlogFile);
        StringBuffer header = new StringBuffer(Calendar.getInstance().getTime().toString());
        StackTraceElement element = new Throwable().getStackTrace()[1];
        header.append(" " + element.getClassName() + "#" + element.getMethodName() + ":" + element.getLineNumber());
        switch(loglevel) {
            case LOG_CRIT:
                header.append(" CRITICAL: ");
                break;
            case LOG_ERROR:
                header.append(" ERROR: ");
                break;
            case LOG_WARN:
                header.append(" WARN: ");
                break;
            case LOG_INFO:
                header.append(" INFO: ");
                break;
            case LOG_DEBUG:
                header.append(" DEBUG: ");
                break;
            case LOG_VDEBUG:
                header.append(" VDEBUG: ");
                break;
            default:
                header.append(" UNKNOWN: ");
        }
        if (systemlogFile.equals("")) {
            System.out.print(header);
            System.out.println(str);
        }
        File targetFile = new File(systemlogFile);
        try {
            FileOutputStream fos = new FileOutputStream(targetFile, true);
            OutputStreamWriter osr = new OutputStreamWriter(fos, "UTF-8");
            PrintWriter pw = new PrintWriter(osr);
            pw.print(header);
            pw.println(str);
            pw.close();
            osr.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
	 * @return Returns the repositoryLocalDirectory.
	 */
    public static File getRepositoryLocalDirectory() {
        return RepositoryLocalDirectory;
    }

    /**
	 * @return Returns the ResourceLocalDirectory.
	 */
    public static File getResourceLocalDirectory(String organization) {
        File ResourceLocalDirectory = new File(RepositoryLocalDirectory.getAbsolutePath() + File.separator + organization + File.separator + getConfigByTagName("ResourceDirName"));
        if (ResourceLocalDirectory.exists() && ResourceLocalDirectory.isDirectory()) {
            ServerConsoleServlet.printSystemLog("returns ResourceLocalDirectory = " + ResourceLocalDirectory.getAbsolutePath(), ServerConsoleServlet.LOG_VDEBUG);
            return ResourceLocalDirectory;
        }
        return null;
    }

    /**
	 * @return Returns the ConfLocalDirectory.
	 */
    public static File getConfLocalDirectory(String organization) {
        File ConfLocalDirectory = new File(RepositoryLocalDirectory.getAbsolutePath() + File.separator + organization + File.separator + getConfigByTagName("ConfDirName"));
        if (ConfLocalDirectory.exists() && ConfLocalDirectory.isDirectory()) {
            ServerConsoleServlet.printSystemLog("returns ConfLocalDirectory = " + ConfLocalDirectory.getAbsolutePath(), ServerConsoleServlet.LOG_VDEBUG);
            return ConfLocalDirectory;
        }
        return null;
    }

    /**
	 * @return Returns the cacheLocalDirectory.
	 */
    public static File getCacheLocalDirectory(String organization) {
        File CacheLocalDirectory = new File(RepositoryLocalDirectory.getAbsolutePath() + File.separator + organization + File.separator + getConfigByTagName("CacheDirName"));
        if (CacheLocalDirectory.exists() && CacheLocalDirectory.isDirectory()) {
            ServerConsoleServlet.printSystemLog("returns CacheLocalDirectory = " + CacheLocalDirectory.getAbsolutePath(), ServerConsoleServlet.LOG_VDEBUG);
            return CacheLocalDirectory;
        }
        return null;
    }

    /**
	 * @return Returns the RDFLocalDirectory.
	 */
    public static File getRDFLocalDirectory(String organization) {
        File RDFLocalDirectory = new File(RepositoryLocalDirectory.getAbsolutePath() + File.separator + organization + File.separator + getConfigByTagName("RDFDirName"));
        if (RDFLocalDirectory.exists() && RDFLocalDirectory.isDirectory()) {
            ServerConsoleServlet.printSystemLog("returns RDFLocalDirectory = " + RDFLocalDirectory.getAbsolutePath(), ServerConsoleServlet.LOG_VDEBUG);
            return RDFLocalDirectory;
        }
        return null;
    }

    /**
	 * @return Returns the SchemaLocalDirectory for each group
	 */
    public static File getSchemaLocalDirectory(String organization) {
        File SchemaLocalDirectory = new File(RepositoryLocalDirectory.getAbsolutePath() + File.separator + organization + File.separator + getConfigByTagName("SchemaDirName"));
        if (SchemaLocalDirectory.exists() && SchemaLocalDirectory.isDirectory()) {
            ServerConsoleServlet.printSystemLog("returns SchemaLocalDirectory = " + SchemaLocalDirectory.getAbsolutePath(), ServerConsoleServlet.LOG_DEBUG);
            return SchemaLocalDirectory;
        }
        return null;
    }

    /**
	 * getServerRootURL returns the root URL of this server. <br>
	 * Both "http://" and "https://" are applicable.<br>
	 * 
	 * @see #getServerRootURI(HttpServletRequest)
	 * @return (e.g. http://www.sharefast.org/ or https://www.sharefast.org/)
	 */
    public static String getServerRootURL(HttpServletRequest request) {
        String rootURL = null;
        try {
            rootURL = request.getRequestURL().toString();
            rootURL = rootURL.substring(0, rootURL.indexOf('/', "https://".length())) + "/";
        } catch (Exception e) {
            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_CRIT);
            e.printStackTrace();
        }
        return rootURL;
    }

    /**
	 * getServerRootURI returns the root URI of this server. <br>
	 * Only "http://" is applicable for this method.
	 * 
	 * @see #getServerRootURL(HttpServletRequest)
	 * @return (e.g. http://www.sharefast.org/)
	 */
    public static String getServerRootURI(HttpServletRequest request) {
        String rootURI = null;
        try {
            rootURI = "http://" + request.getServerName();
            if (request.getServerPort() != 80) rootURI = rootURI + ":" + Integer.toString(request.getServerPort());
            rootURI = rootURI + "/";
        } catch (Exception e) {
            ServerConsoleServlet.printSystemLog(e.toString() + " " + e.getMessage(), ServerConsoleServlet.LOG_CRIT);
            e.printStackTrace();
        }
        return rootURI;
    }

    /**
	 * getOperatingSystemName returns the name of operating system of this machine. <br>
	 * 
	 * @return Returns OperatingSystemName.
	 */
    public static String getOperatingSystemName() {
        return OperatingSystemName;
    }

    /**
	 * getOrganizationNames looks for directories specified in RepositoryPath <br>
	 * and check if the directory has sub-directories for resource, rdf and cache. <br>
	 * Then getOrganizationNames returns array of names of those directories. <br>
	 * If there is no matched entry, getOrganizationNames returns null.<br>
	 * <br>
	 * <PRE>
	 * e.g. 
	 *      repos/sfgroup1/resource
	 *                    /rdf
	 *                    /cache
	 *           /sfgroup2/resource
	 *                    /rdf
	 *                    /cache
	 *           /somedir/somefile
	 * getOrganizationNames returns {"sfgroup1", "sfgroup2"}
	 * </PRE>
	 * 
	 * @return Returns array of organization names or null.
	 */
    public static String[] getOrganizationNames() {
        File[] organizationDirectories = ServerConsoleServlet.getRepositoryLocalDirectory().listFiles();
        Vector retvec = new Vector();
        for (int i = 0; i < organizationDirectories.length; i++) {
            String organization = organizationDirectories[i].getName();
            if (organization.equals("CVS")) continue;
            if (!organizationDirectories[i].isDirectory()) {
                ServerConsoleServlet.printSystemLog("Not a directory. " + organizationDirectories[i].getAbsolutePath(), ServerConsoleServlet.LOG_DEBUG);
                continue;
            }
            File ResourceFileDir = ServerConsoleServlet.getResourceLocalDirectory(organization);
            if ((ResourceFileDir == null) || (!ResourceFileDir.isDirectory())) {
                ServerConsoleServlet.printSystemLog("ResourceFileDir not found in directory " + organization, ServerConsoleServlet.LOG_INFO);
                continue;
            }
            File CacheFileDir = ServerConsoleServlet.getCacheLocalDirectory(organization);
            if ((CacheFileDir == null) || (!CacheFileDir.isDirectory())) {
                ServerConsoleServlet.printSystemLog("CacheFileDir not found in directory " + organization, ServerConsoleServlet.LOG_INFO);
                continue;
            }
            File RDFFileDir = ServerConsoleServlet.getRDFLocalDirectory(organization);
            if ((RDFFileDir == null) || (!RDFFileDir.isDirectory())) {
                ServerConsoleServlet.printSystemLog("RDFFileDir not found in directory " + organization, ServerConsoleServlet.LOG_INFO);
                continue;
            }
            File SchemaFileDir = ServerConsoleServlet.getSchemaLocalDirectory(organization);
            if ((SchemaFileDir == null) || (!SchemaFileDir.isDirectory())) {
                ServerConsoleServlet.printSystemLog("SchemaFileDir not found in directory " + organization, ServerConsoleServlet.LOG_INFO);
                continue;
            }
            retvec.add(organization);
        }
        if (retvec.size() <= 0) {
            return null;
        }
        String[] ret = new String[retvec.size()];
        for (int i = 0; i < ret.length; i++) ret[i] = (String) retvec.get(i);
        return ret;
    }
}
