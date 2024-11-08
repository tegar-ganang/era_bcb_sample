package org.dbe.kb.server;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import org.dbe.kb.server.common.SRinfo;
import org.dbe.kb.mdrman.MDRmanager;
import org.dbe.kb.rdb.DatabaseServer;
import org.dbe.kb.modelman.ModelManRDB;
import net.fada.directory.FadaLookupLocator;
import org.dbe.kb.server.srproxyimpl.SRproxy;
import net.fada.toolkit.FadaHelper;
import net.fada.directory.tool.FadaLeaseRenewer;
import net.fada.directory.tool.FadaServiceID;
import org.dbe.kb.smman.SMmanager;
import org.dbe.kb.rdb.DBObject;
import org.dbe.kb.qee.QueryEngine;

public class SRserver extends HttpServlet {

    private static final String HTML_TYPE = "text/html";

    private static final String XML_TYPE = "text/xml";

    public static boolean _isactive = false;

    private ModelManRDB _modelMan;

    private SMmanager _smMan;

    private QueryEngine _queryEngine;

    private Properties _sProperties;

    public static String _kbdir;

    public void init() throws ServletException {
        System.out.println("The Semantic Registry Service is starting ...");
        _kbdir = getInitParameter("SRdir");
        System.out.println("SR directory:" + _kbdir);
        _sProperties = new Properties();
        try {
            _sProperties.load(new FileInputStream(_kbdir + "/resources/sr.properties"));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ServletException("SR property file could not be read");
        }
        if (_sProperties.getProperty("org.dbe.kb.peer.active").equals("false")) {
            System.out.println("SR: initialization cannot proceed");
            System.out.println("SR: Semantic Service is disabled for the current configuration");
            _isactive = false;
            return;
        }
        _isactive = true;
        initMDR();
        _modelMan = new ModelManRDB();
        _smMan = new SMmanager();
        _queryEngine = new QueryEngine();
        System.out.println("Starting Local Database server");
        DBObject._process = "SR";
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
        System.out.println("SRproxy registered");
    }

    private String getInitMessage() {
        return " <html> <base target=\"_self\"><style></style></head><body topmargin=\"0\" leftmargin=\"0\">" + "<p>&nbsp;</p><table border=\"0\" cellspacing=\"0\" style=\"border-collapse: collapse\" bordercolor=\"#111111\" width=\"63%\" id=\"AutoNumber1\" bgcolor=\"#D6FCFE\">" + "<tr><td width=\"9%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\" bgcolor=\"#FFFFFF\">&nbsp;</td>" + "<td width=\"165%\" colspan=\"2\" style=\"border-right-style: none; border-right-width: medium; border-top-style: solid; border-top-width: 1\">" + "<img border=\"0\" src=\"resources/dbelogo.gif\" width=\"297\" height=\"73\"></td>" + "<td width=\"13%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: solid; border-top-width: 1; border-bottom-style: none; border-bottom-width: medium\">&nbsp;</td>" + "</tr><tr><td width=\"1%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\" bgcolor=\"#FFFFFF\">&nbsp;</td>" + "<td width=\"14%\" style=\"border-right-style: none; border-right-width: medium\">&nbsp;</td><td width=\"151%\" style=\"border-style: none; border-width: medium\"><b>" + "<font face=\"Tahoma\" size=\"5\">Semantic Registry Service is up and running</font></b></td><td width=\"13%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\">&nbsp;</td>" + "</tr><tr><td width=\"1%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\" bgcolor=\"#FFFFFF\">&nbsp;</td>" + "<td width=\"14%\">&nbsp;</td><td width=\"151%\" style=\"border-right-style: none; border-right-width: medium; border-top-style: none; border-top-width: medium\">" + "<p style=\"margin-top: -10\">&nbsp;</p><p style=\"margin-top: -10\">" + "<font face=\"Tahoma\" size=\"4\">" + _sProperties.getProperty("org.dbe.kb.serverName") + ": " + _sProperties.getProperty("org.dbe.kb.serverIP") + "</font></p><p style=\"margin-top: -10\"><u><font face=\"Tahoma\" size=\"4\" color=\"#0000FF\">" + "<b><a href=\"resources/proxydoc\">Browse SR Proxy Doc</a></b></p>" + "</font></u>" + "<p style=\"margin-top: -1\"><u><font face=\"Tahoma\" size=\"3\" color=\"#0000FF\">" + "<b><a href=\"resources/sr-pi-1.0.jar\">Download SR Proxy Interfaces</a></u> - <u><a href=\"resources/lightMDR.jar\"> Download Light-MDR</a></b></font></u></td>" + "<td width=\"13%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\">&nbsp;</td>" + "</tr><tr><td width=\"1%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\" bgcolor=\"#FFFFFF\">&nbsp;</td>" + "<td width=\"14%\">&nbsp;</td><td width=\"151%\" style=\"border-right-style: none; border-right-width: medium\">&nbsp;</td>" + "<td width=\"13%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\">&nbsp;</td>" + "</tr><tr><td width=\"1%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\" bgcolor=\"#FFFFFF\">&nbsp;</td>" + "<td width=\"14%\" style=\"border-bottom-style: solid; border-bottom-width: 1\">&nbsp;</td><td width=\"151%\" style=\"border-right-style: none; border-right-width: medium; border-bottom-style: solid; border-bottom-width: 1\">&nbsp;</td>" + "<td width=\"13%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: solid; border-bottom-width: 1\">&nbsp;</td>" + "</tr></table></body></html>";
    }

    private String getDisableMessage() {
        return "<html><body topmargin=\"0\" leftmargin=\"0\"><p>&nbsp;</p>" + "<table border=\"0\" cellspacing=\"0\" style=\"border-collapse: collapse\" bordercolor=\"#111111\" width=\"63%\" id=\"AutoNumber1\" bgcolor=\"#D6FCFE\" height=\"165\">" + "<tr><td width=\"9%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\" bgcolor=\"#FFFFFF\">&nbsp;</td>" + "<td width=\"165%\" colspan=\"2\" style=\"border-right-style: solid; border-right-width: 1px; border-top-style: solid; border-top-width: 1\">" + "<img border=\"0\" src=\"resources/dbelogo.gif\" width=\"297\" height=\"73\"></td></tr><tr><td width=\"1%\" style=\"border-left-style: none; border-left-width: medium; border-right-style: solid; border-right-width: 1; border-top-style: none; border-top-width: medium; border-bottom-style: none; border-bottom-width: medium\" bgcolor=\"#FFFFFF\">&nbsp;</td>" + "<td width=\"14%\" style=\"border-right-style: none; border-right-width: medium; border-bottom-style:solid; border-bottom-width:1px\">&nbsp;</td>" + "<td width=\"151%\" style=\"border-left-style:none; border-left-width:medium; border-right-style:solid; border-right-width:1px; border-top-style:none; border-top-width:medium; border-bottom-style:solid; border-bottom-width:1px\"><b>" + "<font face=\"Tahoma\" size=\"5\">Semantic Registry Service is disabled</font></b><p><font face=\"Tahoma\" size=\"4\">" + _sProperties.getProperty("org.dbe.kb.serverName") + ": " + _sProperties.getProperty("org.dbe.kb.serverIP") + "</font></td></tr></table></body></html>";
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
        for (int i = 1; i <= SRinfo.max; i++) if ((value = request.getParameter(String.valueOf(i))) != null) {
            cmd = i;
            break;
        }
        System.out.println("SRserver accepted retrieval command " + cmd + ":" + value);
        try {
            switch(cmd) {
                case SRinfo.Get_Service_Manifest:
                    _smMan.getSM(value, out);
                    break;
                case SRinfo.SR_Get_OQL_Metamodel:
                    _modelMan.handleOQL().getOQLMetamodel(out);
                    break;
                case SRinfo.SR_Get_SSL_Metamodel:
                    _modelMan.handleSSL().getSSLMetamodel(out);
                    break;
                case SRinfo.SR_Get_SDL_Metamodel:
                    _modelMan.handleSDL().getSDLMetamodel(out);
                    break;
                case SRinfo.Delete_Service_Manifest:
                    _smMan.deleteSM(value, out);
                    break;
                default:
                    response.setContentType(HTML_TYPE);
                    out.write(getInitMessage().getBytes());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ServletException("SR: Request Error");
        }
        System.out.println("SR service completed retrieval command " + cmd + ":" + value);
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
        System.out.println("SR service accepted store command |" + cmd + "|");
        try {
            switch(cmd) {
                case SRinfo.Publish_Service_Manifest:
                    System.out.println("-- Publish Service Manifest --");
                    String sslModelID = null;
                    String sdlModelID = null;
                    String sslDataID = null;
                    _smMan.processSM(in);
                    InputStream insm = null;
                    try {
                        insm = _smMan.getBMLmodel();
                        sslModelID = _modelMan.handleSSL().storeSSLModel(insm);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    ;
                    try {
                        insm = _smMan.getSDLmodel();
                        sdlModelID = _modelMan.handleSDL().storeSDLModel(insm);
                    } catch (Exception ex) {
                    }
                    ;
                    try {
                        insm = _smMan.getBMLdata();
                        sslDataID = _modelMan.handleSSL().storeSSLModel(insm);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    ;
                    String smid = _smMan.storeSM(sslModelID, sdlModelID, null, sslDataID);
                    out = response.getOutputStream();
                    out.println("<SMID>" + smid + "</SMID>");
                    break;
                case SRinfo.SR_Submit_Query_Model:
                    System.out.println("-- Submit Query --");
                    _modelMan.handleOQL().storeOQLModel(in);
                    String modelName = _modelMan.handleOQL().getQueryContext();
                    try {
                        _queryEngine.evaluateQueryRDB();
                    } catch (Exception ex1) {
                        ex1.printStackTrace();
                    }
                    out = response.getOutputStream();
                    _smMan.formulateQueryResults(modelName, out);
                    _modelMan.handleOQL().clearRepository();
                    break;
                default:
                    in.close();
                    throw new Exception("SR: Command not Found");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ServletException("SR: Constraint Violation Error");
        }
        System.out.println("SR service completed store command |" + cmd + "|");
    }

    public void registerToFADAnode() throws ServletException {
        String[] entries = new String[1];
        entries[0] = _sProperties.getProperty("org.dbe.kb.serverName");
        String ip = _sProperties.getProperty("org.dbe.kb.serverIP");
        String fip = _sProperties.getProperty("org.dbe.kb.fada.addr");
        long leasetime = Long.parseLong(_sProperties.getProperty("org.dbe.kb.fada.leasetime"));
        SRproxy proxy = new SRproxy("http://" + ip + ":" + _sProperties.getProperty("org.dbe.kb.serverPort") + "/SR/server");
        System.out.println("SRproxy registered as:" + "http://" + ip + ":" + _sProperties.getProperty("org.dbe.kb.serverPort") + "/SR/server");
        String codebase = "http://" + ip + ":" + _sProperties.getProperty("org.dbe.kb.serverPort") + "/SR/resources/SRproxiesCB.jar";
        System.out.println("CODEBASE:" + codebase);
        System.out.println("SRpeer[" + ip + "]: Trying to register SRproxy in FADA node[" + fip + "]");
        try {
            FadaHelper helper = new FadaHelper(new FadaLeaseRenewer());
            FadaServiceID id = helper.register(new FadaLookupLocator(fip).getRegistrar(), proxy, null, entries, leasetime, null, codebase, null);
            System.out.println("SRproxy Registered!");
        } catch (Exception ex) {
            throw new ServletException(ex.getMessage());
        }
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
