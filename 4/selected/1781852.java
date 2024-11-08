package org.dbe.kb.server;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.Properties;
import org.dbe.kb.mdrman.MDRmanager;
import org.dbe.kb.modelman.ModelManRDB;
import org.dbe.kb.usgman.UsageManager;
import org.dbe.kb.server.proxyimpl.KBproxy;
import org.dbe.kb.server.common.KBinfo;
import net.fada.toolkit.*;
import net.fada.directory.*;
import net.fada.directory.tool.*;
import org.dbe.kb.rdb.DatabaseServer;
import org.dbe.kb.rdb.DBObject;
import org.dbe.kb.qee.QueryEngine;
import java.util.Vector;

public class KBserver extends HttpServlet {

    public KBserver() {
    }

    private static final String HTML_TYPE = "text/html";

    private static final String XML_TYPE = "text/xml";

    private ModelManRDB _modelMan;

    private UsageManager _usgMan;

    private QueryEngine _queryEngine;

    public static boolean _isactive = false;

    private Properties _sProperties;

    public static String _kbdir;

    public void init() throws ServletException {
        System.out.println("The KB server is starting ...");
        _kbdir = getInitParameter("KBdir");
        System.out.println("KB directory:" + _kbdir);
        _sProperties = new Properties();
        try {
            _sProperties.load(new FileInputStream(_kbdir + "/resources/kb.properties"));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ServletException("KB property file could not be read");
        }
        if (_sProperties.getProperty("org.dbe.kb.peer.active").equals("false")) {
            System.out.println("KB: initialization cannot proceed");
            System.out.println("KB: Semantic Service is disabled for the current configuration");
            _isactive = false;
            return;
        }
        _isactive = true;
        initMDR();
        _modelMan = new ModelManRDB();
        _usgMan = new UsageManager();
        _queryEngine = new QueryEngine();
        System.out.println("Starting Local Database server");
        DBObject._process = "KB";
        DatabaseServer.setDatabaseDir(_kbdir + _sProperties.getProperty("org.dbe.kb.db.dir"));
        DatabaseServer.setDatabaseErrorLog(_kbdir + _sProperties.getProperty("org.dbe.kb.db.errorlog"));
        DatabaseServer.setDatabaseName(_sProperties.getProperty("org.dbe.kb.db.defaultName"));
        DatabaseServer.setDatabaseUser(_sProperties.getProperty("org.dbe.kb.db.defaultUser"));
        DatabaseServer.setPort(Integer.parseInt(_sProperties.getProperty("org.dbe.kb.db.port")));
        DatabaseServer.setHost(_sProperties.getProperty("org.dbe.kb.serverIP"));
        try {
            DatabaseServer.start();
        } catch (Exception ex) {
            throw new ServletException(ex.getMessage());
        }
        System.out.println("Trying to register to FADA Network");
        registerToFADAnode();
        System.out.println("KBproxy registered");
    }

    public void registerToFADAnode() throws ServletException {
        String[] entries = new String[1];
        entries[0] = _sProperties.getProperty("org.dbe.kb.serverName");
        String ip = _sProperties.getProperty("org.dbe.kb.serverIP");
        String fip = _sProperties.getProperty("org.dbe.kb.fada.addr");
        long leasetime = Long.parseLong(_sProperties.getProperty("org.dbe.kb.fada.leasetime"));
        KBproxy proxy = new KBproxy("http://" + ip + ":" + _sProperties.getProperty("org.dbe.kb.serverPort") + "/KB/server");
        String codebase = "http://" + ip + ":" + _sProperties.getProperty("org.dbe.kb.serverPort") + "/KB/resources/KBproxiesCB.jar";
        System.out.println("CODEBASE:" + codebase);
        System.out.println("KBpeer[" + ip + "]: Trying to register KBproxy in FADA node[" + fip + "]");
        try {
            FadaHelper helper = new FadaHelper(new FadaLeaseRenewer());
            FadaServiceID id = helper.register(new FadaLookupLocator(fip).getRegistrar(), proxy, null, entries, leasetime, null, codebase, null);
            System.out.println("KBproxy Registered!");
        } catch (Exception ex) {
            throw new ServletException(ex.getMessage());
        }
    }

    private String getInitMessage() {
        return " <html> <base target=\"_self\"><style></style></head><body topmargin=\"0\" leftmargin=\"0\">" + "<p>&nbsp;</p><table border=\"0\" cellspacing=\"0\" style=\"border-collapse: collapse\" bordercolor=\"#111111\" width=\"63%\" id=\"AutoNumber1\" bgcolor=\"#D6FCFE\">" + "<tr><td width=\"9%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\" bgcolor=\"#FFFFFF\">&nbsp;</td>" + "<td width=\"165%\" colspan=\"2\" style=\"border-right-style: none; border-right-width: medium; border-top-style: solid; border-top-width: 1\">" + "<img border=\"0\" src=\"resources/dbelogo.gif\" width=\"297\" height=\"73\"></td>" + "<td width=\"13%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: solid; border-top-width: 1; border-bottom-style: none; border-bottom-width: medium\">&nbsp;</td>" + "</tr><tr><td width=\"1%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\" bgcolor=\"#FFFFFF\">&nbsp;</td>" + "<td width=\"14%\" style=\"border-right-style: none; border-right-width: medium\">&nbsp;</td><td width=\"151%\" style=\"border-style: none; border-width: medium\"><b>" + "<font face=\"Tahoma\" size=\"5\">KB peer server is up and running</font></b></td><td width=\"13%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\">&nbsp;</td>" + "</tr><tr><td width=\"1%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\" bgcolor=\"#FFFFFF\">&nbsp;</td>" + "<td width=\"14%\">&nbsp;</td><td width=\"151%\" style=\"border-right-style: none; border-right-width: medium; border-top-style: none; border-top-width: medium\">" + "<p style=\"margin-top: -10\">&nbsp;</p><p style=\"margin-top: -10\">" + "<font face=\"Tahoma\" size=\"4\">" + _sProperties.getProperty("org.dbe.kb.serverName") + ": " + _sProperties.getProperty("org.dbe.kb.serverIP") + "</font></p><p style=\"margin-top: -10\"><u><font face=\"Tahoma\" size=\"4\" color=\"#0000FF\">" + "<b><a href=\"resources/proxydoc\">Browse KB Proxy Documentation</a></b></p>" + "</font></u>" + "<p style=\"margin-top: -1\"><u><font face=\"Tahoma\" size=\"3\" color=\"#0000FF\">" + "<b><a href=\"resources/kb-pi-1.0.jar\">Download KB Proxy Interfaces</a></u> - <u><a href=\"resources/lightMDR.jar\">Download Light-MDR</a></b></font></u></td>" + "<td width=\"13%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\">&nbsp;</td>" + "</tr><tr><td width=\"1%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\" bgcolor=\"#FFFFFF\">&nbsp;</td>" + "<td width=\"14%\">&nbsp;</td><td width=\"151%\" style=\"border-right-style: none; border-right-width: medium\">&nbsp;</td>" + "<td width=\"13%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\">&nbsp;</td>" + "</tr><tr><td width=\"1%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\" bgcolor=\"#FFFFFF\">&nbsp;</td>" + "<td width=\"14%\" style=\"border-bottom-style: solid; border-bottom-width: 1\">&nbsp;</td><td width=\"151%\" style=\"border-right-style: none; border-right-width: medium; border-bottom-style: solid; border-bottom-width: 1\">&nbsp;</td>" + "<td width=\"13%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: solid; border-bottom-width: 1\">&nbsp;</td>" + "</tr></table></body></html>";
    }

    private String getDisableMessage() {
        return "<html><body topmargin=\"0\" leftmargin=\"0\"><p>&nbsp;</p>" + "<table border=\"0\" cellspacing=\"0\" style=\"border-collapse: collapse\" bordercolor=\"#111111\" width=\"63%\" id=\"AutoNumber1\" bgcolor=\"#D6FCFE\" height=\"165\">" + "<tr><td width=\"9%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\" bgcolor=\"#FFFFFF\">&nbsp;</td>" + "<td width=\"165%\" colspan=\"2\" style=\"border-right-style: solid; border-right-width: 1px; border-top-style: solid; border-top-width: 1\">" + "<img border=\"0\" src=\"resources/dbelogo.gif\" width=\"297\" height=\"73\"></td></tr><tr><td width=\"1%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\" bgcolor=\"#FFFFFF\">&nbsp;</td>" + "<td width=\"14%\" style=\"border-right-style: none; border-right-width: medium; border-bottom-style:solid; border-bottom-width:1px\">&nbsp;</td>" + "<td width=\"151%\" style=\"border-left-style:none; border-left-width:medium; border-right-style:solid; border-right-width:1px; border-top-style:none; border-top-width:medium; border-bottom-style:solid; border-bottom-width:1px\"><b>" + "<font face=\"Tahoma\" size=\"5\">KB peer server is disabled</font></b><p><font face=\"Tahoma\" size=\"4\">" + _sProperties.getProperty("org.dbe.kb.serverName") + ": " + _sProperties.getProperty("org.dbe.kb.serverIP") + "</font></td></tr></table></body></html>";
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream out = response.getOutputStream();
        if (!_isactive) {
            response.setContentType(HTML_TYPE);
            out.write(getDisableMessage().getBytes());
            return;
        }
        int cmd = 0;
        String value = null;
        response.setContentType(XML_TYPE);
        for (int i = 1; i <= KBinfo.max; i++) if ((value = request.getParameter(String.valueOf(i))) != null) {
            cmd = i;
            break;
        }
        System.out.println("KBserver accepted retrieval command " + cmd + ":" + value);
        try {
            switch(cmd) {
                case KBinfo.KB_Get_SSL_Metamodel:
                    _modelMan.handleSSL().getSSLMetamodel(out);
                    break;
                case KBinfo.KB_Get_All_SSL_Models:
                    _modelMan.handleSSL().getAllSSLModels(out);
                    break;
                case KBinfo.KB_Get_SSL_Model:
                    _modelMan.handleSSL().getSSLModel(value, out);
                    break;
                case KBinfo.KB_Delete_SSL_Model:
                    _modelMan.handleSSL().deleteSSLModel(value, out);
                    break;
                case KBinfo.KB_Get_All_SSL_Data:
                    _modelMan.handleSSL().getAllSSLData(out);
                    break;
                case KBinfo.KB_Get_SSL_Data:
                    _modelMan.handleSSL().getSSLData(value, out);
                    break;
                case KBinfo.KB_Get_SDL_Metamodel:
                    _modelMan.handleSDL().getSDLMetamodel(out);
                    break;
                case KBinfo.KB_Get_All_SDL_Models:
                    _modelMan.handleSDL().getAllSDLModels(out);
                    break;
                case KBinfo.KB_Get_SDL_Model:
                    _modelMan.handleSDL().getSDLModel(value, out);
                    break;
                case KBinfo.KB_Delete_SDL_Model:
                    _modelMan.handleSDL().deleteSDLModel(value, out);
                    break;
                case KBinfo.KB_Search_SDL_Model:
                    _modelMan.handleSDL().searchSDLModels(value, out);
                    break;
                case KBinfo.KB_Get_SCM_Metamodel:
                    _modelMan.handleSCM().getSCMMetamodel(out);
                    break;
                case KBinfo.KB_Get_All_SCM_Models:
                    _modelMan.handleSCM().getAllSCMModels(out);
                    break;
                case KBinfo.KB_Get_SCM_Model:
                    _modelMan.handleSCM().getSCMModel(value, out);
                    break;
                case KBinfo.KB_Delete_SCM_Model:
                    _modelMan.handleSCM().deleteSCMModel(value, out);
                    break;
                case KBinfo.KB_Get_ODM_Metamodel:
                    _modelMan.handleODM().getODMMetamodel(out);
                    break;
                case KBinfo.KB_Get_All_ODM_Models:
                    _modelMan.handleODM().getAllODMModels(out);
                    break;
                case KBinfo.KB_Get_ODM_Model:
                    _modelMan.handleODM().getODMModel(value, out);
                    break;
                case KBinfo.KB_Delete_ODM_Model:
                    _modelMan.handleODM().deleteODMModel(value, out);
                    break;
                case KBinfo.KB_Get_OQL_Metamodel:
                    _modelMan.handleOQL().getOQLMetamodel(out);
                    break;
                default:
                    response.setContentType(HTML_TYPE);
                    out.write(getInitMessage().getBytes());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ServletException("KB: Request Error");
        }
        System.out.println("KBserver completed retrieval command " + cmd + ":" + value);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream out = response.getOutputStream();
        if (!_isactive) {
            response.setContentType(HTML_TYPE);
            out.write(getDisableMessage().getBytes());
            return;
        }
        int cmd = 0;
        InputStream in = request.getInputStream();
        StringWriter sw = new StringWriter();
        int x;
        while ((x = in.read()) != '\n') sw.write(x);
        sw.flush();
        cmd = Integer.parseInt(sw.toString());
        System.out.println("KBserver accepted store command |" + cmd + "|");
        try {
            switch(cmd) {
                case KBinfo.KB_Submit_Query_Model:
                    System.out.println("-- Submit Query --");
                    _modelMan.handleOQL().storeOQLModel(in);
                    try {
                        _queryEngine.evaluateQueryRDB();
                    } catch (Exception ex1) {
                        ex1.printStackTrace();
                    }
                    out = response.getOutputStream();
                    String modelname = _modelMan.handleOQL().getQueryContext();
                    if (modelname.equals(KBinfo.KB_SSL_MODEL_NAME)) {
                        _modelMan.handleSSL().formulateQueryResults(out);
                    } else if (modelname.toUpperCase().equals(KBinfo.KB_SDL_MODEL_NAME)) {
                        _modelMan.handleSDL().formulateQueryResults(out);
                    } else if (modelname.equals(KBinfo.KB_ODM_MODEL_NAME)) {
                    }
                    _modelMan.handleOQL().clearRepository();
                    break;
                case KBinfo.KB_Store_SSL_Model:
                    System.out.println("-- Store SSL model --");
                    _modelMan.handleSSL().storeSSLModel(in);
                    out = response.getOutputStream();
                    out.println("<SSL_MODEL_STORED/>");
                    break;
                case KBinfo.KB_Store_SDL_Model:
                    System.out.println("-- Store SDL model --");
                    _modelMan.handleSDL().storeSDLModel(in);
                    out = response.getOutputStream();
                    out.println("<SDL_MODEL_STORED/>");
                    break;
                case KBinfo.KB_Store_ODM_Model:
                    System.out.println("-- Store ODM model --");
                    _modelMan.handleODM().storeODMModel(in);
                    out = response.getOutputStream();
                    out.println("<ODM_MODEL_STORED/>");
                    break;
                case KBinfo.KB_Store_SCM_Model:
                    System.out.println("-- Store SCM model --");
                    _modelMan.handleSCM().storeSCMModel(in);
                    out = response.getOutputStream();
                    out.println("<SCM_MODEL_STORED/>");
                    break;
                case KBinfo.KB_Store_Usage_Data:
                    System.out.println("-- Store Usage Data --");
                    try {
                        _usgMan.storeUsageDataBatch(in);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        throw new ServletException(ex.getMessage());
                    }
                    break;
                default:
                    in.close();
                    throw new Exception("KB: Command not Found");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ServletException("KB: Constraint Violation Error");
        }
        System.out.println("KBserver completed store command |" + cmd + "|");
    }

    public void initMDR() {
        MDRmanager.setRepositoryDirectoryProperty(_kbdir + _sProperties.getProperty("org.dbe.kb.repository"));
        System.out.println("NAME:" + _sProperties.getProperty("org.dbe.kb.repository"));
        MDRmanager.setLogFileProperty(_kbdir + _sProperties.getProperty("org.dbe.kb.repositoryLog"));
        MDRmanager.initRep();
    }

    public void destroy() {
        MDRmanager.close();
        try {
            DatabaseServer.shutdown();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
