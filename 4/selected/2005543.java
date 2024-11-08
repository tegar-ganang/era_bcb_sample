package sgep;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.oreilly.servlet.MultipartRequest;

public class SunTCP extends HttpServlet {

    class JobInfo {

        String project = "";

        String email = "";

        String input = "";

        final String output;

        final String error;

        String cpu = "";

        String arch = "";

        String queue = "";

        String strPE = "";

        String strOthers = "";

        final String projectDirectory;

        String jobName = "";

        String appName = "";

        String binary = "";

        String xApp = "";

        ArrayList envVars = new ArrayList();

        String parallel = "";

        private BufferedReader projectInfo;

        private BufferedReader appInfo;

        public JobInfo(HttpServletRequest request, String home, String uid, MultipartRequest multi) throws FileNotFoundException, IOException {
            project = multi.getParameter("project");
            email = multi.getParameter("email");
            input = multi.getParameter("input");
            output = multi.getParameter("output");
            error = multi.getParameter("error");
            cpu = multi.getParameter("cpu");
            arch = multi.getParameter("arch");
            queue = multi.getParameter("queue");
            strPE = multi.getParameter("pe");
            strOthers = multi.getParameter("others");
            projectDirectory = home + project;
            projectInfo = new BufferedReader(new FileReader(new File(projectDirectory, SUNTCP_PROJECT)));
            jobName = projectInfo.readLine().replace(' ', '_').replace('\'', '_');
            appName = projectInfo.readLine();
            appInfo = new BufferedReader(new FileReader(new File(APP_HOME_DIR + appName, SUNTCP_APP)));
            appInfo.readLine();
            binary = APP_HOME_DIR + appName + "/" + appInfo.readLine();
            parallel = appInfo.readLine();
            xApp = appInfo.readLine();
            String line;
            envVars = new ArrayList();
            appInfo.readLine();
            appInfo.readLine();
            while ((line = appInfo.readLine()) != null) {
                envVars.add(line);
            }
            appInfo.close();
            while ((line = projectInfo.readLine()) != null) {
                envVars.add(line);
            }
            projectInfo.close();
        }
    }

    private String APP_HOME_DIR = "/export/home/dingyl/apps/";

    private String SGE_ROOT = "/opt/sge/";

    private String SGE_CELL = "default";

    private String COMMD_PORT = "667";

    private String[] SGE_EXPORT = { "SGE_ROOT=/opt/sge", "SGE_CELL=default", "COMMD_PORT=667", "LD_LIBRARY_PATH=/opt/sge", "SGP_ROOT=/gridware/Tools/SGP", "PATH=/bin:/usr/bin:/usr/openwin/bin" };

    private String SGE_ARCH;

    private String SGE_MPRUN = "mpi/MPRUN";

    private String SUNTCP_QSUB_SCRIPT = ".suntcp-qsub";

    private String SUNTCP_SU_SCRIPT = ".suntcp-su";

    private String SUNTCP_PROJECT_DIR = "/suntcp/";

    private String SUNTCP_LIST = ".suntcp-list";

    private String SUNTCP_PROJECT = ".suntcp-project";

    private String SUNTCP_APP = ".suntcp-app";

    private String SUNTCP_APP_FORM = ".suntcp-form";

    private String SUNTCP_DESKTOP_ATTR = "xdesktop";

    private String X_NETLET = "Xvnc";

    private String X_SERVER;

    private String VNCSERVER = "vncserver";

    private String SGP_ROOT = "/gridware/Tools/SGP";

    private String GETWORKSPACE = "/gridware/Tools/SGP/bin/gethomedir";

    private String ADMINRUN = "adminrun";

    private String xDisplayNum = null;

    private RequestDispatcher reqDisp;

    private String projectName;

    private static String getUser(HttpServletRequest request) {
        String uid = "dingyl";
        return uid;
    }

    private static String getDomain(HttpServletRequest request) {
        String domain_name = null;
        return domain_name;
    }

    public void init(ServletConfig config) throws ServletException {
        String s;
        super.init(config);
        s = getInitParameter("app_home");
        if (s != null) {
            APP_HOME_DIR = s;
        }
        s = getInitParameter("sgp_root");
        if (s != null) {
            SGP_ROOT = s;
            SGE_EXPORT[4] = "SGP_ROOT=" + s;
        }
        s = getInitParameter("sge_root");
        if (s != null) {
            SGE_ROOT = s;
            SGE_EXPORT[0] = "SGE_ROOT=" + s;
        }
        s = getInitParameter("sge_cell");
        if (s != null) {
            SGE_CELL = s;
            SGE_EXPORT[1] = "SGE_CELL=" + s;
        }
        s = getInitParameter("commd_port");
        if (s != null) {
            COMMD_PORT = s;
            SGE_EXPORT[2] = "COMMD_PORT=" + s;
        }
        try {
            Process sgeArch = Runtime.getRuntime().exec(SGE_ROOT + "util/arch", SGE_EXPORT);
            BufferedReader archStdout = new BufferedReader(new InputStreamReader(sgeArch.getInputStream()));
            SGE_ARCH = archStdout.readLine();
            archStdout.close();
        } catch (IOException ioe) {
            SGE_ARCH = "solaris";
        }
        s = getInitParameter("x_netlet");
        if (s != null) {
            X_NETLET = s;
        }
        s = getInitParameter("x_server");
        if (s != null) {
            X_SERVER = s;
        } else {
            try {
                X_SERVER = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException uhe) {
                X_SERVER = "localhost";
            }
        }
        s = getInitParameter("vncroot");
        if (s != null) {
            VNCSERVER = s + "/vncserver";
        }
        SGE_EXPORT[3] = "LD_LIBRARY_PATH=" + SGE_ROOT + "lib/" + SGE_ARCH;
        GETWORKSPACE = SGP_ROOT + "bin/gethomedir " + SGP_ROOT + " ";
        ADMINRUN = SGE_ROOT + "utilbin/" + SGE_ARCH + "/adminrun ";
    }

    private String getWorkspace(String uid, String mapped_uid) throws IOException, InterruptedException {
        String workdir;
        Process test_user = Runtime.getRuntime().exec(GETWORKSPACE + uid);
        test_user.waitFor();
        if (test_user.exitValue() == 0) {
            BufferedReader test_home = new BufferedReader(new InputStreamReader(test_user.getInputStream()));
            workdir = test_home.readLine() + SUNTCP_PROJECT_DIR;
        } else {
            Process test_user2 = Runtime.getRuntime().exec(GETWORKSPACE + mapped_uid);
            test_user2.waitFor();
            BufferedReader test_home2 = new BufferedReader(new InputStreamReader(test_user2.getInputStream()));
            workdir = test_home2.readLine() + uid + SUNTCP_PROJECT_DIR;
        }
        return workdir;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        try {
            String uid = getUser(request);
            String domain_name = getDomain(request);
            String submit_uid = uid;
            String home = "/home/" + uid + "/";
            String action = request.getParameter("action");
            if (action.equals("projectList")) {
                projectList(uid, home, response);
            } else if (action.equals("projectInfo")) {
                projectInfo(uid, home, request, response);
            } else if (action.equals("editProjectForm")) {
                editProjectForm(uid, home, request, response);
            } else if (action.equals("deleteProject")) {
                deleteProject(uid, home, request, response);
            } else if (action.equals("deleteFile")) {
                deleteFile(uid, home, request, response);
            } else if (action.equals("jobList")) {
                jobList(uid, submit_uid, home, response);
                jobList(uid, response);
            } else if (action.equals("jobInfo")) {
                jobInfo(uid, home, request, response);
            } else if (action.equals("newJobForm")) {
                newJobForm(uid, home, request, response);
            } else if (action.equals("newJobForm2")) {
                newJobForm2(uid, home, request, response);
            } else if (action.equals("newJobCustomForm")) {
                newJobCustomForm(uid, home, request, response);
            } else if (action.equals("newJobCustomForm2")) {
                newJobCustomForm2(uid, home, request, response);
            } else if (action.equals("jobAccounting")) {
                jobAccounting(uid, response);
            } else if (action.equals("killJob")) {
                killJob(uid, home, request, response);
            } else if (action.equals("applicationList")) {
                applicationList(uid, response);
            } else if (action.equals("applicationInfo")) {
                applicationInfo(request, response);
            } else if (action.equals("viewFile")) {
                viewFile(uid, home, request, response);
            } else if (action.equals("downloadFile")) {
                downloadFile(uid, home, request, response);
            } else if (action.equals("moveFileProject")) {
                moveFiles(request, response, home);
            } else {
                unknownAction(uid, response);
            }
        } catch (Exception e) {
            PrintWriter out = response.getWriter();
            out.println("<html><body><pre>");
            out.println(e.toString());
            out.println("Session Timeout, please relogin</pre></body></html>");
        }
    }

    private void moveFiles(HttpServletRequest request, HttpServletResponse response, String home) {
        String strDestionation = request.getParameter("ddlProject");
        String strOrginal = request.getParameter("strDestionation");
        PrintWriter out = null;
        try {
            out = response.getWriter();
            Runtime.getRuntime().exec("mv " + home + strOrginal + " " + home + strDestionation);
        } catch (IOException ie) {
            htmlHeader(out, "Status", "");
            out.println("Operation Failed<p>Error Message: <br>" + ie.getMessage());
            htmlFooter(out);
            return;
        }
        htmlHeader(out, "Status", "");
        out.println("Operation Successful");
        out.println("<center><form><input type=button value=Continue onClick=\"opener.location.reload(); window.close()\"></form></center>");
        htmlFooter(out);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        try {
            String uid = getUser(request);
            String domain_name = getDomain(request);
            String submit_uid = uid;
            String home = "/home/" + uid + "/";
            if (request.getParameter("action") != null) {
                moveFiles(request, response, home);
                return;
            }
            String name = "T" + System.currentTimeMillis();
            String tmpdir = "/tmp/" + name;
            Runtime.getRuntime().exec("mkdir -m 777 -p " + tmpdir).waitFor();
            PrintWriter sume;
            MultipartRequest multi = new MultipartRequest(request, tmpdir, 100 * 1024 * 1024);
            String action = multi.getParameter("action");
            String project = multi.getParameter("project").trim();
            String app = multi.getParameter("app");
            String export = multi.getParameter("export");
            String email = multi.getParameter("email");
            String input = multi.getParameter("input");
            String output = multi.getParameter("output");
            String error = multi.getParameter("error");
            String cpu = multi.getParameter("cpu");
            String arch = multi.getParameter("arch");
            String queue = multi.getParameter("queue");
            String strMPI = multi.getParameter("pe");
            String strOthers = multi.getParameter("others");
            Enumeration objFiles = multi.getFileNames();
            String strFileName = null;
            if (objFiles.hasMoreElements()) {
                strFileName = multi.getFilesystemName((String) objFiles.nextElement());
                Runtime.getRuntime().exec("/usr/bin/chmod 644 " + tmpdir + "/" + strFileName).waitFor();
                if (multi.getParameter("tar") != null) {
                    sume = new PrintWriter(new BufferedWriter(new FileWriter(new File(tmpdir, SUNTCP_SU_SCRIPT))));
                    sume.println("cd " + tmpdir);
                    sume.println("gunzip -d " + strFileName);
                    sume.println("tar -xvf " + strFileName.substring(0, strFileName.length() - 3));
                    sume.println("rm -f " + strFileName.substring(0, strFileName.length() - 3));
                    sume.close();
                    Runtime.getRuntime().exec("/usr/bin/chmod 755 " + tmpdir + "/" + SUNTCP_SU_SCRIPT).waitFor();
                    Runtime.getRuntime().exec("su - " + submit_uid + " -c " + tmpdir + "/" + SUNTCP_SU_SCRIPT).waitFor();
                    File objFile = new File(tmpdir + "/" + strFileName.substring(0, strFileName.length() - 3));
                    if (objFile.exists()) {
                        objFile.delete();
                    }
                }
            }
            if (action.equals("createNewProject")) {
                createNewProject(uid, submit_uid, home, domain_name, tmpdir, strFileName, app, export, request, response, out, email, input, output, error, cpu, arch, queue, strMPI, strOthers);
            } else if (action.equals("updateProject")) {
                String id = multi.getParameter("id");
                updateProject(uid, submit_uid, home, id, tmpdir, strFileName, project, app, export, request, response);
            } else if (action.equals("submitNewJob")) {
                submitNewJob(uid, submit_uid, home, domain_name, request, response, tmpdir, multi, strFileName);
            } else {
                unknownAction(uid, response);
            }
        } catch (Exception e) {
            out.println("<html><body><pre>" + e.getMessage());
            e.printStackTrace(out);
            out.println("</pre></body></html>");
        }
    }

    private void projectList(String uid, String home, HttpServletResponse response) throws Exception {
        StringTokenizer st;
        String line, project;
        String directory = home;
        boolean blnHaveFiles = false;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        try {
            synchronized (Class.forName("GEP.SunTCP")) {
                BufferedReader list = new BufferedReader(new FileReader(directory + SUNTCP_LIST));
                out.println("<table width=100%>");
                while ((line = list.readLine()) != null) {
                    blnHaveFiles = true;
                    st = new StringTokenizer(line, "\t");
                    project = st.nextToken();
                    out.println("<tr><td><li><a href=\"SunTCP?action=projectInfo&project=" + project + "\" target=SunTCPProject>");
                    out.println(st.nextToken() + "</a></li></td>");
                    out.println("<td><a href=\"/Edit Project\" onClick=\"window.open(\'SunTCP?action=editProjectForm&project=" + project + "\', \'SunTCPProject\'); return false\">");
                    out.println("edit</a></td>");
                    out.println("<td><a href=URL onClick=\"window.open(\'SunTCP?action=deleteProject&project=" + project + "\', \'SunTCPProject\'); return false\">");
                    out.println("delete</a></td></tr>");
                }
                if (blnHaveFiles == false) {
                    out.println("You have no projects available");
                }
                out.println("</table>");
                list.close();
            }
        } catch (FileNotFoundException e) {
            out.println("You have no project.");
        }
        out.println("</body></html>");
    }

    private void projectInfo(String uid, String home, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String project = request.getParameter("project");
        String line, appfolder, text = "";
        String projectname;
        String path = home + project;
        boolean found = false;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        htmlHeader(out, "Project Information", "");
        try {
            BufferedReader info = new BufferedReader(new FileReader(new File(path, SUNTCP_PROJECT)));
            projectname = info.readLine();
            out.println("<table>");
            out.println("<tr><td><a target=SunTCPFiles> Project name:</td><td>" + projectname + "</a></td></tr>");
            appfolder = info.readLine();
            StringTokenizer st;
            BufferedReader list = new BufferedReader(new FileReader(APP_HOME_DIR + SUNTCP_LIST));
            while ((line = list.readLine()) != null && !found) {
                st = new StringTokenizer(line, "\t");
                if ((st.nextToken()).equals(appfolder)) {
                    text = st.nextToken();
                    found = true;
                }
            }
            out.println("<tr><td>Application:</td><td>" + text + "</td></tr>");
            out.println("<tr><td valign=top>Exports:</td>");
            if ((line = info.readLine()) != null) {
                out.println("<td>" + line + "</td>");
            }
            out.println("</tr>");
            while ((line = info.readLine()) != null) {
                out.println("<tr><td>&nbsp;</td><td>" + line + "</td></tr>");
            }
            File directory = new File(path);
            String[] files = directory.list();
            out.println("<tr><td valign=top>Files:</td>");
            out.println("</tr>");
            if (files.length <= 3) {
                out.println("<tr><td><b>NO FILES IN THIS PROJECT</b></td></tr><table>");
                out.println("<center><form><input type=button value=Continue onClick=window.close()></form></center>");
                htmlFooter(out);
                return;
            }
            out.println("</table>");
            String listfile = home + SUNTCP_LIST;
            list = new BufferedReader(new FileReader(listfile));
            Vector objProject = new Vector();
            String strTemp;
            while ((line = list.readLine()) != null) {
                st = new StringTokenizer(line, "\t");
                strTemp = st.nextToken();
                objProject.addElement(strTemp);
            }
            boolean blnDisplayed = false;
            for (int i = 0; i < files.length; i++) {
                if (files[i].indexOf(".suntcp") == -1) {
                    File temp = new File(path, files[i]);
                    if (temp.length() != 0) {
                        if (blnDisplayed == false) {
                            out.println("<table><tr><th colspan=4>File Name</th>");
                            out.println("<th colspan=8>Action</th>");
                            out.println("<th>File Transfer</th></tr>");
                            blnDisplayed = true;
                        }
                        out.println("<td>&nbsp;</td><td><a href=\"SunTCP?action=downloadFile&project=" + project + "&file=" + files[i] + "\" target=SunTCPFile2>" + files[i] + "</a></td>");
                        out.println("<td>&nbsp;</td><td>(" + temp.length() + " bytes) </td>");
                        out.println("<td>&nbsp;</td><td><a href=URL onClick=\"window.open(\'SunTCP?action=viewFile&view=view&project=" + project + "&file=" + files[i] + "\', \'SunTCPFile2\'); return false\">");
                        out.println("view</a></td>");
                        out.println("<td>&nbsp;</td><td><a href=URL onClick=\"window.open(\'SunTCP?action=viewFile&view=head&project=" + project + "&file=" + files[i] + "\', \'SunTCPFile2\'); return false\">");
                        out.println("head</a></td>");
                        out.println("<td>&nbsp;</td><td><a href=URL onClick=\"window.open(\'SunTCP?action=viewFile&view=tail&project=" + project + "&file=" + files[i] + "\', \'SunTCPFile2\'); return false\">");
                        out.println("tail</a></td>");
                        if (!files[i].equals(".suntcp-project")) {
                            out.println("<td>&nbsp;</td><td><a href=URL onClick=\"window.open(\'SunTCP?action=deleteFile&project=" + project + "&file=" + files[i] + "\', \'SunTCPFile2\'); return false\">");
                            out.println("delete</a></td>");
                        }
                        Vector objAdd = new Vector();
                        for (int intCount = 0; intCount < objProject.size(); intCount++) {
                            if (projectname.trim().equals(objProject.elementAt(intCount).toString().trim()) == false) {
                                objAdd.addElement(objProject.elementAt(intCount));
                            }
                        }
                        if (objAdd.size() == 0) {
                            out.println("<td>No other projects to transfer files to</td>");
                        } else {
                            out.println("<td><form method=post target=_blank action=SunTCP enctype=text/html><input type=hidden name=action value=moveFileProject><select name=ddlProject>");
                            for (int intCount = 0; intCount < objAdd.size(); intCount++) {
                                out.println("<option>" + objAdd.elementAt(intCount));
                            }
                            out.println("</select>");
                            out.println("<input type=hidden name=strDestionation value=\"" + projectname + "/" + files[i] + "\">");
                            out.println("<input type=submit value=Transfer></form></td></tr>");
                        }
                        out.println("</tr>");
                    }
                }
            }
            if (!blnDisplayed) {
                out.println("<tr><td><b>NO FILES IN THIS PROJECT</b></td></tr></table>");
                out.println("<center><form><input type=button value=Continue onClick=window.close()></form></center>");
                htmlFooter(out);
                return;
            }
            out.println("</table>");
        } catch (FileNotFoundException e) {
            out.println("Error accessing this project.");
        }
        out.println("<center><form><input type=button value=Continue onClick=window.close()></form></center>");
        htmlFooter(out);
    }

    private void createNewProject(String uid, String submit_uid, String home, String domain_name, String tmpdir, String filename, String app, String export, HttpServletRequest request, HttpServletResponse response, PrintWriter out, String email, String input, String output, String error, String cpu, String arch, String queue, String strMPI, String strOthers) throws Exception {
        if (app.equalsIgnoreCase("null")) {
            out.println("Please select an valid application<br>Use the back button to return to the previous screen<br>");
            out.println("<input type=\"button\" value=\"Back\" onclick=\"history.back()\">");
            return;
        }
        PrintWriter sume;
        String apps, text, line;
        StringTokenizer st;
        long currentDate;
        try {
            BufferedReader list2 = new BufferedReader(new FileReader(APP_HOME_DIR + SUNTCP_LIST));
            while ((line = list2.readLine()) != null) {
                st = new StringTokenizer(line, "\t");
                apps = st.nextToken();
                if (app.equals(apps)) {
                    text = st.nextToken();
                    text = text.replace(' ', '_').replace('-', '_');
                    projectName = text + new java.text.SimpleDateFormat("ddMMyyyy" + "HHmms").format(new Date(System.currentTimeMillis()));
                    list2.close();
                    String path = home + projectName;
                    File directory = new File(path);
                    sume = new PrintWriter(new BufferedWriter(new FileWriter("/tmp/" + SUNTCP_SU_SCRIPT + uid)));
                    sume.println("mkdir -m 755 -p " + path);
                    sume.println("/usr/bin/chmod 755 " + home);
                    sume.close();
                    Runtime.getRuntime().exec("/usr/bin/chmod 755 " + "/tmp/" + SUNTCP_SU_SCRIPT + uid).waitFor();
                    Runtime.getRuntime().exec("su - " + submit_uid + " -c " + "/tmp/" + SUNTCP_SU_SCRIPT + uid).waitFor();
                    htmlHeader(out, "New Project Status", "");
                    if (filename != null) {
                        sume = new PrintWriter(new BufferedWriter(new FileWriter(new File(directory, SUNTCP_SU_SCRIPT))));
                        sume.println("cd " + tmpdir);
                        sume.println("/usr/bin/cp -r * " + path);
                        sume.close();
                        Runtime.getRuntime().exec("/usr/bin/chmod 755 " + path + "/" + SUNTCP_SU_SCRIPT).waitFor();
                        Runtime.getRuntime().exec("su - " + submit_uid + " -c " + path + "/" + SUNTCP_SU_SCRIPT).waitFor();
                        File objFile = new File(path + "/" + filename.substring(0, filename.length() - 3));
                        objFile.delete();
                    }
                    if (projectName.length() > 0) {
                        synchronized (Class.forName("com.sun.gep.SunTCP")) {
                            String listfile = home + SUNTCP_LIST;
                            PrintWriter list = new PrintWriter(new BufferedWriter(new FileWriter(listfile, true)));
                            list.println(projectName + "\t" + projectName);
                            list.close();
                            PrintWriter ressource = new PrintWriter(new BufferedWriter(new FileWriter(new File(path, SUNTCP_PROJECT))));
                            ressource.println(projectName);
                            ressource.println(app);
                            try {
                                String value;
                                StringTokenizer st_equal, st_newline = new StringTokenizer(export, "\n\r\f");
                                while (st_newline.hasMoreTokens()) {
                                    st_equal = new StringTokenizer(st_newline.nextToken(), "= \t");
                                    ressource.print(st_equal.nextToken() + "=");
                                    value = st_equal.nextToken();
                                    if (value.equals("$HOME")) {
                                        ressource.println(path);
                                    } else {
                                        ressource.println(value);
                                    }
                                }
                                Runtime.getRuntime().exec("/usr/bin/rm -rf " + tmpdir).waitFor();
                                out.println("<p>Project <i>" + projectName + "</i> was created successfully.");
                            } catch (NoSuchElementException e) {
                                out.println("<p>Error parsing export variables. You can add/correct variables by editing the newly created project.");
                            }
                            ressource.close();
                        }
                    } else {
                        Runtime.getRuntime().exec("/usr/bin/rm -rf " + path);
                        out.println("<p>Go back and enter a non-empty project name.");
                        out.println("<center><form><input type=button value=Back onClick=window.back()></form></center>");
                        htmlFooter(out);
                    }
                    submitNewJob2(uid, submit_uid, path, domain_name, request, response, email, input, output, error, cpu, arch, queue, strMPI, strOthers);
                    htmlFooter(out);
                }
            }
        } catch (Exception e) {
        }
    }

    private void editProjectForm(String uid, String home, HttpServletRequest request, HttpServletResponse response) throws Exception {
        StringTokenizer st;
        String line, app, text;
        boolean dismiss = false;
        String id = request.getParameter("project");
        String directory = home + id;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        htmlHeader(out, "Project Form", "");
        try {
            BufferedReader read = new BufferedReader(new FileReader(new File(directory, SUNTCP_PROJECT)));
            String project = read.readLine();
            String application = read.readLine();
            Vector export = new Vector();
            while ((line = read.readLine()) != null) {
                export.addElement(line);
            }
            read.close();
            BufferedReader list = new BufferedReader(new FileReader(APP_HOME_DIR + SUNTCP_LIST));
            out.println("<form method=post action=SunTCP enctype=multipart/form-data>");
            out.println("<input type=hidden name=action value=updateProject>");
            out.println("<input type=hidden name=id value=" + id + ">");
            out.println("<table>");
            out.println("<tr><td>Project name:</td>");
            out.println("<td><input type=text name=project value=\"" + project + "\" size=25></td></tr>");
            out.println("<tr><td>Application:</td>");
            out.println("<td><select name=app>");
            while ((line = list.readLine()) != null) {
                st = new StringTokenizer(line, "\t");
                app = st.nextToken();
                text = st.nextToken();
                if (st.hasMoreTokens()) {
                    dismiss = true;
                    while (st.hasMoreTokens()) {
                        if (st.nextToken(" \t").equals(uid)) {
                            dismiss = false;
                            break;
                        }
                    }
                    if (dismiss) {
                        continue;
                    }
                }
                if (app.equals(application)) {
                    out.println("<option selected value=" + app + ">");
                } else {
                    out.println("<option value=" + app + ">");
                }
                out.println(text);
            }
            out.println("</select></td></tr>");
            out.println("<tr><td>Input file:</td>");
            out.println("<td><input type=file name=input size=25></td></tr>");
            out.println("<tr><td colspan=2><input type=checkbox name=tar value=yes>");
            out.println("check here if this file is a compressed tar archive.</td></tr>");
            out.println("<tr><td valign=top>Environment variables:</td>");
            out.println("<td><textarea name=export rows=5 cols=25>");
            for (int i = 0; i < export.size(); i++) {
                out.println((String) export.get(i));
            }
            out.println("</textarea></td></tr>");
            out.println("</table>");
            out.println("<center><input type=submit value=Submit></center>");
            out.println("</form>");
        } catch (FileNotFoundException e) {
            out.println("There is no application available.");
        }
        htmlFooter(out);
    }

    private void updateProject(String uid, String submit_uid, String home, String id, String tmpdir, String filename, String project, String app, String export, HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter sume;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        String directory = home + id;
        if (filename != null) {
            sume = new PrintWriter(new BufferedWriter(new FileWriter(new File(directory, SUNTCP_SU_SCRIPT))));
            sume.println("cd " + tmpdir);
            sume.println("/usr/bin/cp -R * " + directory);
            sume.close();
            Runtime.getRuntime().exec("/usr/bin/chmod 755 " + directory + "/" + SUNTCP_SU_SCRIPT).waitFor();
            Runtime.getRuntime().exec("su - " + submit_uid + " -c " + directory + "/" + SUNTCP_SU_SCRIPT).waitFor();
            Runtime.getRuntime().exec("rm -rf " + tmpdir).waitFor();
        }
        htmlHeader(out, "Project Status", "");
        if (project.length() > 0) {
            synchronized (Class.forName("com.sun.gep.SunTCP")) {
                Vector list = new Vector();
                String line;
                String path = home;
                BufferedReader read = new BufferedReader(new FileReader(path + SUNTCP_LIST));
                while ((line = read.readLine()) != null) {
                    if (!((new StringTokenizer(line, "\t")).nextToken().equals(id))) {
                        list.addElement(line);
                    }
                }
                read.close();
                PrintWriter write = new PrintWriter(new BufferedWriter(new FileWriter(path + SUNTCP_LIST)));
                for (int i = 0; i < list.size(); i++) {
                    write.println((String) list.get(i));
                }
                write.println(id + "\t" + project);
                write.close();
                PrintWriter ressource = new PrintWriter(new BufferedWriter(new FileWriter(new File(directory, SUNTCP_PROJECT))));
                ressource.println(project);
                ressource.println(app);
                try {
                    String value;
                    StringTokenizer st_equal, st_newline = new StringTokenizer(export, "\n\r\f");
                    while (st_newline.hasMoreTokens()) {
                        st_equal = new StringTokenizer(st_newline.nextToken(), "= \t");
                        ressource.print(st_equal.nextToken() + "=");
                        value = st_equal.nextToken();
                        if (value.equals("$HOME")) {
                            ressource.println(directory);
                        } else {
                            ressource.println(value);
                        }
                    }
                    Runtime.getRuntime().exec("/usr/bin/rm -rf " + tmpdir).waitFor();
                    out.println("<p>Project <i>" + project + "</i> was updated successfully.");
                } catch (NoSuchElementException e) {
                    out.println("<p>Error parsing export variables. You can add/correct variables by editing again the project.");
                }
                out.println("<center><form><input type=button value=Continue onClick=\"opener.location.reload(); window.close()\"></form></center>");
                ressource.close();
            }
        } else {
            out.println("<p>Go back and enter a non-empty project name.");
            out.println("<center><form><input type=button value=Back onClick=window.back()></form></center>");
        }
        htmlFooter(out);
    }

    private void deleteProject(String uid, String home, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String project = request.getParameter("project");
        String line;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        htmlHeader(out, "Project Status", "");
        try {
            synchronized (Class.forName("com.sun.gep.SunTCP")) {
                Vector list = new Vector();
                String directory = home;
                Runtime.getRuntime().exec("/usr/bin/rm -rf " + directory + project);
                FilePermission perm = new FilePermission(directory + SUNTCP_LIST, "read,write,execute");
                File listfile = new File(directory + SUNTCP_LIST);
                BufferedReader read = new BufferedReader(new FileReader(listfile));
                while ((line = read.readLine()) != null) {
                    if (!((new StringTokenizer(line, "\t")).nextToken().equals(project))) {
                        list.addElement(line);
                    }
                }
                read.close();
                if (list.size() > 0) {
                    PrintWriter write = new PrintWriter(new BufferedWriter(new FileWriter(listfile)));
                    for (int i = 0; i < list.size(); i++) {
                        write.println((String) list.get(i));
                    }
                    write.close();
                } else {
                    listfile.delete();
                }
                out.println("The project was successfully deleted.");
            }
        } catch (Exception e) {
            out.println("Error accessing this project.");
        }
        out.println("<center><form><input type=button value=Continue onClick=\"opener.location.reload(); window.close()\"></form></center>");
        htmlFooter(out);
    }

    private void jobList(String uid, String submit_uid, String home, HttpServletResponse response) throws Exception {
        String line, id, name, jobstatus;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        try {
            Process qstat = Runtime.getRuntime().exec(SGE_ROOT + "bin/" + SGE_ARCH + "/qstat -u " + submit_uid, SGE_EXPORT);
            BufferedReader jobs = new BufferedReader(new InputStreamReader(qstat.getInputStream()));
            BufferedReader error = new BufferedReader(new InputStreamReader(qstat.getErrorStream()));
            boolean blnHaveJob = false;
            out.println("<table width=100%>");
            if ((jobs.readLine()) != null) {
                jobs.readLine();
                line = jobs.readLine();
                do {
                    id = line.substring(2, 7).trim();
                    if (id.length() > 0) {
                        String partial = line.substring(16, 19);
                        if (!partial.equals("tmp")) {
                            out.println("<tr><td><li><a href=\"SunTCP?action=jobInfo&id=" + id + "\" target=SunTCPJob>");
                            name = line.substring(15, 26).trim();
                            if (name.length() == 10) {
                                name = name.concat("...");
                            }
                            name = name.replace('_', ' ');
                            blnHaveJob = true;
                            out.println(name + "</a></li></td>");
                            jobstatus = line.substring(40, 41);
                            if (jobstatus.equals("r")) {
                                out.println("<td>Status: Running</td>");
                            }
                            if (jobstatus.equals("q")) {
                                out.println("<td>Status: Queued</td>");
                            }
                            out.println("<td align=right><a href=URL onCLick=\"window.open(\'SunTCP?action=killJob&id=" + id + "\', \'SunTCPJob\'); return false\">kill</a></td></tr>");
                        }
                    }
                } while ((line = jobs.readLine()) != null);
            }
            if (!blnHaveJob) {
                out.println("You have no running jobs.");
                out.println("<pre>");
                while ((line = error.readLine()) != null) {
                    out.println(line);
                }
                out.println("</pre>");
            }
            out.println("</table>");
        } catch (Exception e) {
            e.printStackTrace(out);
        }
        out.println("<table width=100%><tr><td align=right>");
        out.println("<a href=URL onCLick=\"window.open(\'SunTCP?action=newJobForm\', \'SunTCPJob\',\'width=600,height=600,status=1,scrollbars=1\'); return false\">");
        out.println("Submit jobs using existing projects</a></br>");
        out.println("</td></tr>");
        out.println("<tr><td align=right>");
        out.println("<a href=URL onCLick=\"window.open(\'SunTCP?action=newJobForm2\', \'SunTCPJob\',\'width=600,height=600,status=1,scrollbars=1\'); return false\">");
        out.println("Submit new jobs</a>");
        out.println("</td></tr></table>");
        out.println("</body></html>");
    }

    private void jobList(String uid, HttpServletResponse response) throws Exception {
        String line, id, name;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<table width=100%><tr><td align=right>");
        out.println("<a href=URL onCLick=\"window.open(\'SunTCP?action=jobAccounting\', \'SunTCPJob\'); return false\">");
        out.println("View usage statistics</a>");
        out.println("</td></tr></table>");
        out.println("</body></html>");
    }

    private void jobInfo(String uid, String home, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String id = request.getParameter("id");
        String line;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        htmlHeader(out, "Job Information", "");
        out.println("<pre>");
        try {
            Process qstat = Runtime.getRuntime().exec(SGE_ROOT + "bin/" + SGE_ARCH + "/qstat -j " + id, SGE_EXPORT);
            BufferedReader jobs = new BufferedReader(new InputStreamReader(qstat.getInputStream()));
            while ((line = jobs.readLine()) != null) {
                out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace(out);
        }
        out.println("</pre>");
        out.println("<center><form><input type=button value=Continue onClick=window.close()></form></center>");
        htmlFooter(out);
    }

    private void newJobForm(String uid, String home, HttpServletRequest request, HttpServletResponse response) throws Exception {
        StringTokenizer st;
        String line, project, path, application;
        String qname, qtype, used, load, stat, arch, test;
        BufferedReader projectinfo, appinfo;
        Vector all, yes;
        Vector queue = new Vector();
        int i;
        boolean blnHaveProject = false;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        htmlHeader(out, "Submit New Job Via Selecting Available Projects", "");
        try {
            File objFile = new File(home + SUNTCP_LIST);
            if (objFile.exists() == false) {
                out.println("There are no projects configured for this user<BR>");
                out.println("Select \"Continue\" to return to previous screen<BR>");
                out.println("<center><form><input type=button value=Continue onClick=\"opener.location.reload(); window.close()\"></form></center>");
                return;
            }
            String listfile = home + SUNTCP_LIST;
            BufferedReader list = new BufferedReader(new FileReader(listfile));
            all = new Vector();
            yes = new Vector();
            while ((line = list.readLine()) != null) {
                blnHaveProject = true;
                st = new StringTokenizer(line, "\t");
                project = st.nextToken();
                all.addElement(project);
                all.addElement(st.nextToken());
                path = home + project;
                projectinfo = new BufferedReader(new FileReader(new File(path, SUNTCP_PROJECT)));
                projectinfo.readLine();
                application = projectinfo.readLine();
                appinfo = new BufferedReader(new FileReader(new File(APP_HOME_DIR + application, SUNTCP_APP)));
                appinfo.readLine();
                appinfo.readLine();
                appinfo.readLine();
                appinfo.readLine();
                if (appinfo.readLine().equals("no") == false) {
                    yes.addElement(project);
                    yes.addElement(application);
                }
                projectinfo.close();
                appinfo.close();
            }
            if (blnHaveProject == false) {
                out.println("There are no projects configured for this user<BR>");
                out.println("Select \"Continue\" to return to previous screen<BR>");
                out.println("<center><form><input type=button value=Continue onClick=\"opener.location.reload(); window.close()\"></form></center>");
                return;
            }
            int length = yes.size();
            if (length > 0) {
                out.println("<script language=javascript>");
                out.println("function loadCustom() {");
                out.println("  var myselect = document.forms[0].elements[1];");
                out.println("  var project = myselect.options[myselect.selectedIndex].value;");
                out.println("  var p = new Array(" + length + ");");
                for (i = 0; i < length; i++) {
                    out.println("  p[" + i + "] = \"" + (String) yes.get(i) + "\";");
                }
                out.println("  for (var i = 0; i < p.length; i += 2) {");
                out.println("    if (project == p[i]) {");
                out.println("      window.open(\"SunTCP?action=newJobCustomForm&project=\" + p[i] + \"&app=\" + p[i + 1], p[i], \"menubar=no,scrollbars=yes,height=600,width=600,resizable=yes,toolbar=no,location=no,status=no\");");
                out.println("      return false;");
                out.println("    }");
                out.println("  }");
                out.println("}");
                out.println("</script>");
            }
            printJSProject(out);
            out.println("<form method=post action=SunTCP name=\"frmData\" enctype=multipart/form-data>");
            out.println("<input type=hidden name=action value=submitNewJob>");
            out.println("<table>");
            project = request.getParameter("project");
            out.println("<tr><td>* Project name:</td>");
            out.println("<td><select name=project onChange=loadCustom()>");
            if (project == null) {
                out.println("<option value=null>Select project first.");
                for (i = 0; i < all.size(); i += 2) {
                    out.println("<option value=" + (String) all.get(i) + ">" + (String) all.get(i + 1));
                }
            } else {
                for (i = 0; i < all.size(); i += 2) {
                    if (project.equals((String) all.get(i))) {
                        out.println("<option value=" + (String) all.get(i) + " selected>" + (String) all.get(i + 1));
                    } else {
                        out.println("<option value=" + (String) all.get(i) + ">" + (String) all.get(i + 1));
                    }
                }
            }
            out.println("</select></td></tr>");
            out.println("<tr><td>Email notification:</td>");
            String email = request.getParameter("email");
            if (email == null) {
                out.println("<td><input type=text name=email value=" + uid + " size=25></td></tr>");
            } else {
                out.println("<td><input type=text name=email value=" + email + " size=25></td></tr>");
            }
            out.println("<tr><td>Input arguments:</td>");
            String input = request.getParameter("input");
            if (input == null) {
                out.println("<td><input type=text name=input size=25></td></tr>");
            } else {
                out.println("<td><input type=text name=input size=25 value=\"" + input + "\"></td></tr>");
            }
            out.println("<tr><td>* Output file name:</td>");
            String output = request.getParameter("output");
            if (output == null) {
                out.println("<td><input type=text name=output value=output.txt size=25></td></tr>");
            } else {
                out.println("<td><input type=text name=output value=\"" + output + "\" size=25></td></tr>");
            }
            out.println("<tr><td>* Error file name:</td>");
            String error = request.getParameter("error");
            if (error == null) {
                out.println("<td><input type=text name=error value=error.txt size=25></td></tr>");
            } else {
                out.println("<td><input type=text name=error value=\"" + error + "\" size=25></td></tr>");
            }
            out.println("<tr><td>Input file:</td>");
            out.println("<td><input type=file name=input size=25></td></tr>");
            out.println("<tr><td colspan=2><input type=checkbox name=tar value=yes>");
            out.println("check here if this file is a compressed tar archive.</td></tr>");
            this.printAdvance(out, response, request);
        } catch (Exception e) {
        }
        out.println("<center><input type=button value=Clear onCLick=\"window.open(\'SunTCP?action=newJobForm\', \'SunTCPJob\'); return false\"></center>");
        out.println("</form>");
        out.println("<tr><td><i>* - denotes mandatory field</i></tr></td>");
        htmlFooter(out);
    }

    private void newJobForm2(String uid, String home, HttpServletRequest request, HttpServletResponse response) throws Exception {
        StringTokenizer st, st2;
        String line, project, path, application, line2, text, app, appName, formName;
        String qname, qtype, used, load, stat, arch, test;
        boolean dismiss = false;
        BufferedReader projectinfo, appinfo;
        Vector all, yes;
        Vector queue = new Vector();
        int i, c;
        String defaultOut, defaultErr;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        try {
            htmlHeader(out, "Submit New Job Via Selecting Application", "");
            BufferedReader list2 = new BufferedReader(new FileReader(APP_HOME_DIR + SUNTCP_LIST));
            all = new Vector();
            yes = new Vector();
            while ((line = list2.readLine()) != null) {
                st = new StringTokenizer(line, "\t");
                app = st.nextToken();
                text = st.nextToken();
                all.addElement(app);
                all.addElement(text);
                BufferedReader check = new BufferedReader(new FileReader(new File(APP_HOME_DIR + app, SUNTCP_APP)));
                check.readLine();
                check.readLine();
                check.readLine();
                check.readLine();
                formName = check.readLine();
                if (formName.equals("no")) {
                } else {
                    yes.addElement(app);
                }
            }
            list2.close();
            int length = yes.size();
            if (length > 0) {
                out.println("<script language=javascript>");
                out.println("function loadCustom2() {");
                out.println("  var app = document.forms[0].app.options[document.forms[0].app.selectedIndex].value;");
                out.println("  var p = new Array(" + length + ");");
                for (i = 0; i < length; i++) {
                    out.println("  p[" + i + "] = \"" + (String) yes.get(i) + "\";");
                }
                out.println("  for (var i = 0; i < p.length; i++) {");
                out.println("    if (app == p[i]) {");
                out.println("      window.open(\"SunTCP?action=newJobCustomForm2&app=\" + app,app, \" menubar=no,scrollbars=yes,height=600,width=600,resizable=yes,toolbar=no,location=no,status=no\");");
                out.println("      return false;");
                out.println("    }");
                out.println("  }");
                out.println("}");
                out.println("</script>");
            }
            printJSApp(out);
            out.println("<form method=post name=\"frmData\" action=SunTCP enctype=multipart/form-data>");
            out.println("<input type=hidden name=action value=createNewProject>");
            out.println("<input type=hidden name=project value=>");
            out.println("<table>");
            appName = request.getParameter("project");
            out.println("<tr><td>* Application:</td>");
            out.println("<td><select name=app onChange=loadCustom2()>");
            if (appName == null) {
                out.println("<option value=null>Select application first.");
                for (i = 0; i < all.size(); i += 2) {
                    out.println("<option value=" + (String) all.get(i) + ">" + (String) all.get(i + 1));
                }
            } else {
                for (i = 0; i < all.size(); i += 2) {
                    if (appName.equals((String) all.get(i))) {
                        out.println("<option value=" + (String) all.get(i) + " selected>" + (String) all.get(i + 1));
                    } else {
                        out.println("<option value=" + (String) all.get(i) + ">" + (String) all.get(i + 1));
                    }
                }
            }
            out.println("</select></td></tr>");
            out.println("<tr><td>Email notification:</td>");
            String email = request.getParameter("email");
            if (email == null) {
                out.println("<td><input type=text name=email value=" + uid + " size=25></td></tr>");
            } else {
                out.println("<td><input type=text name=email value=" + email + " size=25></td></tr>");
            }
            out.println("<tr><td>Binary input arguments:</td>");
            String input = request.getParameter("input");
            if (input == null) {
                out.println("<td><input type=text name=input size=25></td></tr>");
            } else {
                out.println("<td><input type=text name=input size=25 value=\"" + input + "\"></td></tr>");
            }
            out.println("<tr><td>* Output file name:</td>");
            String output = request.getParameter("output");
            if (output == null) {
                out.println("<td><input type=text name=output value=output.txt size=25></td></tr>");
            } else {
                out.println("<td><input type=text name=output value=\"" + output + "\" size=25></td></tr>");
            }
            out.println("<tr><td>* Error file name:</td>");
            String error = request.getParameter("error");
            if (error == null) {
                out.println("<td><input type=text name=error value=error.txt size=25></td></tr>");
            } else {
                out.println("<td><input type=text name=error value=\"" + error + "\" size=25></td></tr>");
            }
            out.println("<tr><td>Input file:</td>");
            out.println("<td><input type=file name=input size=25></td></tr>");
            out.println("<tr><td colspan=2><input type=checkbox name=tar value=yes>");
            out.println("check here if this file is a compressed tar archive.</td></tr>");
        } catch (Exception e) {
        }
        this.printAdvance(out, response, request);
        out.println("<center><input type=button value=Clear onCLick=\"window.open(\'SunTCP?action=newJobForm2\', \'SunTCPJob\'); return false\"></center>");
        out.println("</form>");
        out.println("<tr><td><i>* - denotes mandatory field</i></tr></td>");
        htmlFooter(out);
    }

    private void jobAccounting(String uid, HttpServletResponse response) throws Exception {
        String line, token;
        StringTokenizer st;
        int nb, aid;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        Process qacct;
        BufferedReader jobs;
        BufferedReader error;
        htmlHeader(out, "Job Accounting");
        try {
            qacct = Runtime.getRuntime().exec(SGE_ROOT + "bin/" + SGE_ARCH + "/qacct -o " + uid, SGE_EXPORT);
            jobs = new BufferedReader(new InputStreamReader(qacct.getInputStream()));
            error = new BufferedReader(new InputStreamReader(qacct.getErrorStream()));
            out.println("<table border=2>");
            if ((jobs.readLine()) != null) {
                line = jobs.readLine();
                out.println("<tr>");
                out.println("<th>Owner</th>");
                out.println("<th>Wall Clock</th>");
                out.println("<th>User Time</th>");
                out.println("<th>System Time</th>");
                out.println("<th>CPU</th>");
                out.println("<th>Memory</th>");
                out.println("<th>IO</th>");
                out.println("<th>IO Wait Time</th>");
                while ((line = jobs.readLine()) != null) {
                    st = new StringTokenizer(line);
                    out.println("<tr>");
                    out.println("<td align=center>" + st.nextToken() + "</td>");
                    out.println("<td align=center>" + st.nextToken() + "</td>");
                    out.println("<td align=center>" + st.nextToken() + "</td>");
                    out.println("<td align=center>" + st.nextToken() + "</td>");
                    out.println("<td align=center>" + st.nextToken() + "</td>");
                    out.println("<td align=center>" + st.nextToken() + "</td>");
                    out.println("<td align=center>" + st.nextToken() + "</td>");
                    out.println("<td align=center>" + st.nextToken() + "</td>");
                    out.println("</tr>");
                }
                out.println("</table>");
            } else {
                out.println("There are no running jobs.");
                out.println("<pre>");
                while ((line = error.readLine()) != null) {
                    out.println(line);
                }
                out.println("</pre>");
            }
        } catch (Exception e) {
            e.printStackTrace(out);
        }
        out.println("<center><form><input type=button value=Continue onClick=window.close()></form></center>");
        htmlFooter(out);
    }

    private void newJobCustomForm(String uid, String home, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String line, formName;
        String project = request.getParameter("project");
        String app = request.getParameter("app");
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        try {
            BufferedReader check = new BufferedReader(new FileReader(new File(APP_HOME_DIR + app, SUNTCP_APP)));
            check.readLine();
            check.readLine();
            check.readLine();
            check.readLine();
            formName = check.readLine();
            if (formName.equals("no")) {
            } else {
                BufferedReader form = null;
                if (formName.equals("yes")) {
                    form = new BufferedReader(new FileReader(new File(APP_HOME_DIR + app, SUNTCP_APP_FORM)));
                } else {
                    form = new BufferedReader(new FileReader(new File(APP_HOME_DIR + app, formName)));
                }
                out.println("<html><body><form>");
                out.println("<input type=hidden name=project value=" + project + ">");
                while ((line = form.readLine()) != null) {
                    out.println(line);
                }
            }
        } catch (Exception e) {
            out.println("There is no customized form found.");
        }
        out.println("</form></body></html>");
    }

    private void newJobCustomForm2(String uid, String home, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        String line, formName;
        String app = request.getParameter("app");
        try {
            BufferedReader check = new BufferedReader(new FileReader(new File(APP_HOME_DIR + app, SUNTCP_APP)));
            check.readLine();
            check.readLine();
            check.readLine();
            check.readLine();
            formName = check.readLine();
            if (formName.equals("no")) {
            } else {
                BufferedReader form = null;
                if (formName.equals("yes")) {
                    form = new BufferedReader(new FileReader(new File(APP_HOME_DIR + app, SUNTCP_APP_FORM)));
                } else {
                    form = new BufferedReader(new FileReader(new File(APP_HOME_DIR + app, formName)));
                }
                out.println("<html><body><form>");
                out.println("<input type=hidden name=app value=" + app + ">");
                while ((line = form.readLine()) != null) {
                    out.println(line);
                }
            }
        } catch (Exception e) {
            out.println("There is no customized form found.");
        }
        out.println("</form></body></html>");
    }

    private boolean isxDisplayNumValid() {
        if (xDisplayNum == null) {
            return false;
        }
        return (!xDisplayNum.equals("::"));
    }

    private void submitNewJob(String uid, String submit_uid, String home, String domain_name, HttpServletRequest request, HttpServletResponse response, String tmpdir, MultipartRequest multi, String strFileName) throws Exception {
        PrintWriter out = response.getWriter();
        if (multi.getParameter("project").equals("null")) {
            out.println("Please select a valid project<br>Use the back button to return to the previous screen<br>");
            out.println("<input type=\"button\" value=\"Back\" onclick=\"history.back()\">");
            return;
        }
        JobInfo ji = new JobInfo(request, home, uid, multi);
        if (!(strFileName == null)) {
            PrintWriter sume = new PrintWriter(new BufferedWriter(new FileWriter(new File(tmpdir, SUNTCP_SU_SCRIPT + "2"))));
            sume.println("cd " + tmpdir);
            sume.println("cp -R * " + ji.projectDirectory);
            sume.close();
            Runtime.getRuntime().exec("/usr/bin/chmod 755 " + tmpdir + "/" + SUNTCP_SU_SCRIPT + "2").waitFor();
            Process zip = Runtime.getRuntime().exec("su - " + submit_uid + " -c " + tmpdir + "/" + SUNTCP_SU_SCRIPT + "2");
        }
        String line;
        response.setContentType("text/html");
        htmlHeader(out, "New Job Status", "");
        PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(new File(ji.projectDirectory, SUNTCP_QSUB_SCRIPT))));
        script.println("#!/bin/ksh");
        script.println("# qsub script automatically generated by suntcp");
        script.println(". $SGE_ROOT/default/common/settings.sh");
        script.println("#$ -N " + ji.jobName);
        script.println("#$ -S /bin/ksh");
        script.println("#$ -o " + ji.output);
        script.println("#$ -e " + ji.error);
        script.println("#$ -A " + ji.appName);
        if (ji.email.length() > 0) {
            script.println("#$ -M " + ji.email);
            script.println("#$ -m besa");
        } else {
            script.println("#$ -m n");
        }
        script.println("#$ -cwd");
        script.println("#$ -v PATH -v SGE_ROOT -v COMMD_PORT -v SGE_CELL -v LD_LIBRARY_PATH");
        if (ji.arch.equals("default") == false) {
            script.println("#$ -l arch=" + ji.arch);
        }
        if (ji.queue.equals("default") == false) {
            script.println("#$ -q " + ji.queue);
        }
        script.println("# ------------------------------------------");
        script.println("# Other Commands");
        script.println("# ------------------------------------------");
        String aryData[] = ji.strOthers.split("\r");
        for (int intCount = 0; intCount < aryData.length; intCount++) {
            script.println("#$ " + aryData[intCount]);
        }
        if (ji.strPE.length() > 0) {
            script.println("#$ -pe " + ji.strPE.trim() + " " + ji.cpu);
        }
        Iterator i = ji.envVars.iterator();
        while (i.hasNext()) {
            script.println(i.next());
        }
        if (ji.xApp.equals("yes")) {
            script.println("export DISPLAY=" + X_SERVER + ":" + xDisplayNum);
        }
        script.println("cat > tmp$$ << EOF");
        script.println("qacct -j $JOB_ID >> $SGE_STDOUT_PATH");
        script.println("EOF");
        script.println("qsub -hold_jid $JOB_ID -o /dev/null -e /dev/null tmp$$ > /dev/null");
        script.println("rm tmp$$");
        if (ji.strPE.equalsIgnoreCase("mpi")) {
            script.println(SGE_ROOT + SGE_MPRUN + " -np $NSLOTS -Mf $TMPDIR/machines " + ji.binary + " " + ji.input);
        } else {
            script.println(ji.binary + " " + ji.input);
        }
        script.close();
        PrintWriter sume = new PrintWriter(new BufferedWriter(new FileWriter(new File(ji.projectDirectory, SUNTCP_SU_SCRIPT))));
        sume.println("#!/bin/ksh");
        sume.println("cd " + ji.projectDirectory);
        for (int j = 0; j < SGE_EXPORT.length; j++) {
            sume.println("export " + SGE_EXPORT[j]);
        }
        if (ji.arch.equals("default") && ji.queue.equals("default")) {
            sume.println(SGE_ROOT + "bin/" + SGE_ARCH + "/qsub " + SUNTCP_QSUB_SCRIPT);
        } else if (ji.arch.equals("default")) {
            sume.println(SGE_ROOT + "bin/" + SGE_ARCH + "/qsub -q " + ji.queue + " " + SUNTCP_QSUB_SCRIPT);
        } else {
            sume.println(SGE_ROOT + "bin/" + SGE_ARCH + "/qsub -l arch=" + ji.arch + " " + SUNTCP_QSUB_SCRIPT);
        }
        sume.close();
        Runtime.getRuntime().exec("/usr/bin/chmod 755 " + ji.projectDirectory + "/" + SUNTCP_SU_SCRIPT).waitFor();
        Runtime.getRuntime().exec("/usr/bin/chmod 644 " + ji.projectDirectory + "/" + SUNTCP_QSUB_SCRIPT).waitFor();
        Process qsub = Runtime.getRuntime().exec("su - " + submit_uid + " -c " + ji.projectDirectory + "/" + SUNTCP_SU_SCRIPT);
        final BufferedReader job = new BufferedReader(new InputStreamReader(qsub.getInputStream()));
        final BufferedReader ejob = new BufferedReader(new InputStreamReader(qsub.getErrorStream()));
        out.println("<p>New Job Status:");
        out.println("<pre>");
        while ((line = ejob.readLine()) != null) {
            out.println(line);
        }
        while ((line = job.readLine()) != null) {
            out.println(line);
        }
        out.println("</pre>");
        out.println("<center><form><input type=button value=Close onClick=\"opener.location.reload(); window.close()\"></form></center>");
        htmlFooter(out);
    }

    private void submitNewJob2(String uid, String submit_uid, String home, String domain_name, HttpServletRequest request, HttpServletResponse response, String email, String input, String output, String error, String cpu, String arch, String queue, String strMPI, String strOthers) throws Exception {
        String lines, line, projectDirectory, jobName, appName, xApp, parallel, binary;
        ArrayList envVars;
        BufferedReader projectInfo, appInfo;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        projectDirectory = home;
        projectInfo = new BufferedReader(new FileReader(new File(projectDirectory, SUNTCP_PROJECT)));
        jobName = projectInfo.readLine().replace(' ', '_').replace('\'', '_');
        appName = projectInfo.readLine();
        appInfo = new BufferedReader(new FileReader(new File(APP_HOME_DIR + appName, SUNTCP_APP)));
        appInfo.readLine();
        binary = APP_HOME_DIR + appName + "/" + appInfo.readLine();
        parallel = appInfo.readLine();
        xApp = appInfo.readLine();
        envVars = new ArrayList();
        appInfo.readLine();
        appInfo.readLine();
        while ((line = appInfo.readLine()) != null) {
            envVars.add(line);
        }
        appInfo.close();
        while ((line = projectInfo.readLine()) != null) {
            envVars.add(line);
        }
        projectInfo.close();
        PrintWriter script = new PrintWriter(new BufferedWriter(new FileWriter(new File(projectDirectory, SUNTCP_QSUB_SCRIPT))));
        script.println("#!/bin/ksh");
        script.println("# qsub script automatically generated by suntcp");
        script.println(". $SGE_ROOT/default/common/settings.sh");
        script.println("#$ -N " + jobName);
        script.println("#$ -S /bin/ksh");
        script.println("#$ -o " + output);
        script.println("#$ -e " + error);
        script.println("#$ -A " + appName);
        if (email.length() > 0) {
            script.println("#$ -M " + email);
            script.println("#$ -m besa");
        } else {
            script.println("#$ -m n");
        }
        script.println("#$ -cwd");
        script.println("#$ -v PATH -v SGE_ROOT -v COMMD_PORT -v SGE_CELL -v LD_LIBRARY_PATH");
        script.println("# ------------------------------------------");
        script.println("# Other Commands");
        script.println("# ------------------------------------------");
        String aryData[] = strOthers.split("\r");
        for (int intCount = 0; intCount < aryData.length; intCount++) {
            if (aryData[intCount].equals("") == false) {
                script.println("#$ " + aryData[intCount]);
            }
        }
        if (strMPI.equalsIgnoreCase("default") == false) {
            script.println("#$ -pe " + strMPI.trim() + " " + cpu);
        }
        Iterator i = envVars.iterator();
        while (i.hasNext()) {
            script.println(i.next());
        }
        if (xApp.equals("yes")) {
            script.println("export DISPLAY=" + X_SERVER + ":" + xDisplayNum);
        }
        if (arch.equals("default") == false) {
            script.println("#$ -l arch=" + arch);
        }
        if (queue.equals("default") == false) {
            script.println("#$ -q " + queue);
        }
        script.println("cat > tmp$$ << EOF");
        script.println("qacct -j $JOB_ID >> $SGE_STDOUT_PATH");
        script.println("EOF");
        script.println("qsub -hold_jid $JOB_ID -o /dev/null -e /dev/null tmp$$ > /dev/null");
        script.println("rm tmp$$");
        if (strMPI.equalsIgnoreCase("mpi")) {
            script.println(SGE_ROOT + SGE_MPRUN + " -np $NSLOTS -Mf $TMPDIR/machines " + binary + " " + input);
        } else {
            script.println(binary + " " + input);
        }
        script.close();
        PrintWriter sume = new PrintWriter(new BufferedWriter(new FileWriter(new File(projectDirectory, SUNTCP_SU_SCRIPT))));
        sume.println("#!/bin/ksh");
        sume.println("cd " + projectDirectory);
        for (int j = 0; j < SGE_EXPORT.length; j++) {
            sume.println("export " + SGE_EXPORT[j]);
        }
        if (arch.equals("default") && queue.equals("default")) {
            sume.println(SGE_ROOT + "bin/" + SGE_ARCH + "/qsub " + SUNTCP_QSUB_SCRIPT);
        } else if (arch.equals("default")) {
            sume.println(SGE_ROOT + "bin/" + SGE_ARCH + "/qsub -q " + queue + " " + SUNTCP_QSUB_SCRIPT);
        } else {
            sume.println(SGE_ROOT + "bin/" + SGE_ARCH + "/qsub -l arch=" + arch + " " + SUNTCP_QSUB_SCRIPT);
        }
        sume.close();
        Runtime.getRuntime().exec("/usr/bin/chmod 755 " + projectDirectory + "/" + SUNTCP_SU_SCRIPT).waitFor();
        Runtime.getRuntime().exec("/usr/bin/chmod 644 " + projectDirectory + "/" + SUNTCP_QSUB_SCRIPT).waitFor();
        Process qsub = Runtime.getRuntime().exec("su - " + submit_uid + " -c " + projectDirectory + "/" + SUNTCP_SU_SCRIPT);
        final BufferedReader objOutput = new BufferedReader(new InputStreamReader(qsub.getInputStream()));
        final BufferedReader objErrorOutput = new BufferedReader(new InputStreamReader(qsub.getErrorStream()));
        out.println("<p>New Job Status:");
        out.println("<pre>");
        while ((lines = objErrorOutput.readLine()) != null) {
            out.println(lines);
        }
        while ((lines = objOutput.readLine()) != null) {
            out.println(lines);
        }
        out.println("</pre>");
        out.println("<center><form><input type=button value=Close onClick=\"opener.location.reload(); window.close()\"></form></center>");
        htmlFooter(out);
    }

    private void killJob(String uid, String home, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String id = request.getParameter("id");
        String line;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        htmlHeader(out, "Job Status", "");
        try {
            Process qdel = Runtime.getRuntime().exec(SGE_ROOT + "bin/" + SGE_ARCH + "/qdel " + id, SGE_EXPORT);
            BufferedReader jobs = new BufferedReader(new InputStreamReader(qdel.getInputStream()));
            BufferedReader error = new BufferedReader(new InputStreamReader(qdel.getErrorStream()));
            out.println("<pre>");
            while ((line = jobs.readLine()) != null) {
                out.println(line);
            }
            while ((line = error.readLine()) != null) {
                out.println(line);
            }
            out.println("</pre>");
        } catch (Exception e) {
            out.println("<pre>");
            e.printStackTrace(out);
            out.println("</pre>");
        }
        out.println("<center><form><input type=button value=Continue onClick=\"opener.location.reload(); window.close()\"></form></center>");
        htmlFooter(out);
    }

    private void applicationList(String uid, HttpServletResponse response) throws Exception {
        StringTokenizer st, st2;
        String line, app, text, helpfile, temp;
        boolean dismiss = false;
        boolean help = false;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        try {
            BufferedReader list = new BufferedReader(new FileReader(APP_HOME_DIR + SUNTCP_LIST));
            out.println("<html><body><table width=100%>");
            while ((line = list.readLine()) != null) {
                st = new StringTokenizer(line, "\t");
                app = st.nextToken();
                text = st.nextToken();
                helpfile = text.trim();
                temp = "";
                st2 = new StringTokenizer(helpfile);
                while (st2.hasMoreTokens()) {
                    temp += st2.nextToken();
                }
                helpfile = temp;
                File f = new File("/opt/SUNWam/public_html/help/" + helpfile + ".html");
                if (f.exists()) {
                    help = true;
                } else {
                    help = false;
                }
                if (st.hasMoreTokens()) {
                    dismiss = true;
                    while (st.hasMoreTokens()) {
                        if (st.nextToken(" \t").equals(uid)) {
                            dismiss = false;
                            break;
                        }
                    }
                    if (dismiss) {
                        continue;
                    }
                }
                out.println("<tr><td><li><a href=\"SunTCP?action=applicationInfo&application=" + app + "\" target=SunTCPApplication >");
                out.println(text + "</a></li></td>");
                if (help) {
                    out.println("<td><a href=\"/" + "help/" + helpfile + ".html" + "\">");
                    out.println("Help</a></td>");
                }
                out.println("</tr>");
            }
            out.println("</table></body></html>");
        } catch (FileNotFoundException e) {
            out.println("<html><body>There are no applications available.</body></html>");
        }
    }

    private void applicationInfo(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String application = request.getParameter("application");
        String line;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        htmlHeader(out, "Application Information", "");
        try {
            BufferedReader info = new BufferedReader(new FileReader(new File(APP_HOME_DIR + application, SUNTCP_APP)));
            out.println("<table>");
            out.println("<tr><td>Application name:</td><td>" + info.readLine() + "</td></tr>");
            out.println("<tr><td>Binary name:</td><td>" + info.readLine() + "</td></tr>");
            out.println("<tr><td>Parallel mode:</td><td>" + info.readLine() + "</td></tr>");
            out.println("<tr><td>Uses X11:</td><td>" + info.readLine() + "</td></tr>");
            out.println("<tr><td>Customized form:</td><td>" + info.readLine() + "</td></tr>");
            out.println("<tr><td>User access list:</td><td>" + info.readLine() + "</td></tr>");
            out.println("<tr><td valign=top>Exports:</td><td>");
            while ((line = info.readLine()) != null) {
                out.println(line + "<br>");
            }
            out.println("</td></tr></table>");
        } catch (FileNotFoundException e) {
            out.println("<pre>");
            e.printStackTrace(out);
            out.println("</pre>");
        }
        out.println("<center><form><input type=button value=Continue onClick=window.close()></form></center>");
        htmlFooter(out);
    }

    private void htmlHeader(PrintWriter page, String title) {
        page.println("<html><body bgcolor=FFFFFF>");
        page.println("<table border=0 cellpadding=2 cellspacing=0 width=100% bgcolor=888888>");
        page.println("<tr><td bgcolor=666699><font color=FFFFFF><b>GEP " + title + "</b></font></td></tr><tr><td bgcolor=DDDDDD>");
    }

    private void htmlHeader(PrintWriter page, String title, String headInfo) {
        page.println("<html><head>" + headInfo + "</head><body bgcolor=FFFFFF>");
        page.println("<table border=0 cellpadding=2 cellspacing=0 width=100% bgcolor=888888>");
        page.println("<tr><td bgcolor=666699><font color=FFFFFF><b>SunTCP " + title + "</b></font></td></tr><tr><td bgcolor=DDDDDD>");
    }

    private void htmlFooter(PrintWriter page) {
        page.println("</td></tr><tr><td bgcolor=000000 align=center>");
        page.println("<font color=FFFFFF>Copyright 2001 Sun Microsystems, Inc. All rights reserved.</font></td></tr>");
        page.println("</table></body></html>");
    }

    private void viewFile(String uid, String home, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String project = request.getParameter("project");
        String view = request.getParameter("view");
        String file = request.getParameter("file");
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        final int NBLINES = 40;
        String line;
        htmlHeader(out, "File View", "");
        out.println("<pre>");
        try {
            String directory = home + project;
            File objFile = new File(directory, file);
            if (objFile.isDirectory()) {
                out.println("File not viewable, " + file + " is a directory");
                out.println("<center><form><input type=button value=Continue onClick=window.close()></form></center>");
                return;
            }
            if (view.equals("view")) {
                BufferedReader zaq = new BufferedReader(new FileReader(new File(directory, file)));
                while ((line = zaq.readLine()) != null) {
                    out.println(line);
                }
                zaq.close();
            } else if (view.equals("head")) {
                BufferedReader head = new BufferedReader(new FileReader(new File(directory, file)));
                for (int i = 0; ((line = head.readLine()) != null) && (i < NBLINES); i++) {
                    out.println(line);
                }
                head.close();
            } else if (view.equals("tail")) {
                Process ptail = Runtime.getRuntime().exec("/usr/bin/tail -" + NBLINES + " " + directory + "/" + file);
                BufferedReader tail = new BufferedReader(new InputStreamReader(ptail.getInputStream()));
                while ((line = tail.readLine()) != null) {
                    out.println(line);
                }
            } else {
                out.println("Unrecognized view option.");
            }
        } catch (Exception e) {
            e.printStackTrace(out);
        }
        out.println("</pre>");
        out.println("<center><form><input type=button value=Continue onClick=\"opener.location.reload(); window.close()\"></form></center>");
        htmlFooter(out);
    }

    private void downloadFile(String uid, String home, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String project = request.getParameter("project");
        String name = request.getParameter("file");
        String directory = home + project;
        File file = new File(directory, name);
        if (file.isDirectory()) {
            PrintWriter out = response.getWriter();
            htmlHeader(out, "Error", "");
            out.println(name + " is a directory and cannot be downloaded");
            out.println("<center><form><input type=button value=Continue onClick=\"opener.location.reload(); window.close()\"></form></center>");
            htmlFooter(out);
            return;
        }
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        response.setContentType("application/binary");
        ServletOutputStream out = response.getOutputStream();
        long offset = 0, length = file.length();
        int BLOCK = 8 * 1024, nb;
        byte[] data = new byte[BLOCK];
        try {
            while (offset < length) {
                nb = in.read(data, 0, BLOCK);
                out.write(data, 0, nb);
                offset += nb;
            }
        } catch (IOException e) {
        } finally {
            in.close();
            out.close();
            data = null;
        }
    }

    private void notYetImplemented(HttpServletResponse response) throws Exception {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        htmlHeader(out, "Status", "");
        out.println("Not yet Implemented.");
        out.println("<center><form><input type=button value=Continue onClick=window.close()></form></center>");
        htmlFooter(out);
    }

    private void unknownAction(String uid, HttpServletResponse response) throws Exception {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("Unkown action for user " + uid);
        out.println("</body></html>");
    }

    private void deleteFile(String uid, String home, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String project = request.getParameter("project");
        String file = request.getParameter("file");
        String line;
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        htmlHeader(out, "File Status", "");
        try {
            synchronized (Class.forName("com.sun.gep.SunTCP")) {
                String directory = home + project;
                Runtime.getRuntime().exec("/usr/bin/rm -rf " + directory + "/" + file).waitFor();
                out.println(file + " is successfully deleted.");
            }
        } catch (Exception e) {
            out.println("Error accessing this file.");
        }
        out.println("<center><form><input type=button value=Continue onClick=\"opener.location.reload(); window.close()\"></form></center>");
        htmlFooter(out);
    }

    public LinkedList removeDulicaptes(LinkedList objData) {
        LinkedList objOutput = new LinkedList();
        String objTemp;
        boolean blnAdd;
        for (int intCount = 0; intCount < objData.size(); intCount++) {
            objTemp = objData.get(intCount).toString().trim();
            if (objTemp.equals("") == false) {
                if (objOutput.contains(objTemp) == false) {
                    objOutput.add(objTemp);
                }
            }
        }
        return objOutput;
    }

    private LinkedList retrieveMPI() {
        LinkedList objData = new LinkedList();
        try {
            Process objProcess = Runtime.getRuntime().exec(SGE_ROOT + "bin/" + SGE_ARCH + "/qconf -spl", SGE_EXPORT);
            BufferedReader objReader = new BufferedReader(new InputStreamReader(objProcess.getInputStream()));
            String strRead;
            while ((strRead = objReader.readLine()) != null) {
                if (strRead.trim().equals("") == false) {
                    objData.add(strRead);
                }
            }
        } catch (Exception ie) {
        }
        return objData;
    }

    private void printMPI(HttpServletResponse response, HttpServletRequest request) {
        PrintWriter out = null;
        try {
            out = response.getWriter();
        } catch (Exception ie) {
        }
        out.println("<tr><td>Number of CPUs:</td>");
        String cpu = request.getParameter("cpu");
        if (cpu == null) {
            out.println("<td><input type=text name=cpu value=1 size=25></td></tr>");
        } else {
            out.println("<td><input type=text name=cpu value=" + cpu + " size=25></td></tr>");
        }
        out.println("<tr>");
        out.println("<td>Select Parallel Enviroment</td>");
        LinkedList objMPI = retrieveMPI();
        if (objMPI.size() == 0) {
            out.println("<td>No Parallel Environment Configured</td>");
        } else {
            out.println("<td><select name=\"pe\">");
            out.println("<option value=\"default\">Select PE Settings");
            for (int intCount = 0; intCount < objMPI.size(); intCount++) {
                out.println("<option>" + objMPI.get(intCount).toString());
            }
            out.println("</select>");
        }
    }

    private String getHomeDir(String username) {
        Process objhomedir;
        BufferedReader objStat, objError;
        return null;
    }

    private void printQueueArch(PrintWriter out) {
        out.println("<tr><td>Please Specify either queuename/Architecture or None.</td></tr>");
        LinkedList objArch = new LinkedList();
        LinkedList objQueue = new LinkedList();
        BufferedReader objStat, objError;
        Process objQstat;
        String strTempLine;
        String strQName, strArch;
        StringTokenizer objProcessST;
        try {
            objQstat = Runtime.getRuntime().exec(SGE_ROOT + "bin/" + SGE_ARCH + "/qstat -f", SGE_EXPORT);
            objStat = new BufferedReader(new InputStreamReader(objQstat.getInputStream()));
            objError = new BufferedReader(new InputStreamReader(objQstat.getErrorStream()));
            if ((objStat.readLine()) != null) {
                out.println("<tr><td>Architecture:</td>");
                boolean blnRead = false;
                while ((strTempLine = objStat.readLine()) != null) {
                    if (strTempLine.indexOf("------------------------------") != -1) {
                        blnRead = true;
                    } else if (blnRead) {
                        objProcessST = new StringTokenizer(strTempLine);
                        if (objProcessST.hasMoreTokens()) {
                            strQName = objProcessST.nextToken();
                            objProcessST.nextToken();
                            objProcessST.nextToken();
                            objProcessST.nextToken();
                            strArch = objProcessST.nextToken();
                            if (objProcessST.hasMoreTokens()) {
                                String strState = objProcessST.nextToken();
                            } else {
                                objQueue.add(strQName);
                                objArch.add(strArch);
                            }
                            blnRead = false;
                        }
                    }
                }
            }
            objArch = removeDulicaptes(objArch);
            if (objArch.size() == 0) {
                out.println("<td><select name=arch><option value=default>No Available Queues</td>");
            } else {
                out.println("<td><select name=arch>");
                out.println("<option value=default>Specify Architecture.");
                for (int intCount = 0; intCount < objArch.size(); intCount++) {
                    out.println("<option value=" + objArch.get(intCount) + ">" + objArch.get(intCount));
                }
                out.println("</select></td></tr>");
            }
            out.println("<tr><td>Queue name:</td>");
            if (objQueue.size() == 0) {
                out.println("<td><select name=queue><option value=default>No Queues Available</select></td>");
            } else {
                out.println("<td><select name=queue>");
                out.println("<option value=default>Specify Queuename.");
                for (int intCount = 0; intCount < objQueue.size(); intCount++) {
                    out.println("<option value=" + (String) objQueue.get(intCount) + ">" + (String) objQueue.get(intCount));
                }
                out.println("</select></td></tr>");
            }
        } catch (Exception ie) {
        }
    }

    private void printOtherQStatCommands(PrintWriter out) {
        out.println("<tr>");
        out.println("<td>Additional Commands to Qsub</td>");
        out.println("<td><textarea name=others rows=5 cols=25></textarea></td>");
        out.println("</tr>");
        out.println("</table>");
        out.println("<center><input type=submit value=Submit onCLick=\"validate()\"></center>");
    }

    private void printEnvirVar(PrintWriter out) {
        out.println("<tr><td valign=top>Environment variables:</td>");
        out.println("<td><textarea name=export rows=5 cols=25></textarea></td></tr>");
    }

    private void printAdvance(PrintWriter out, HttpServletResponse response, HttpServletRequest request) {
        out.println("<tr><td><b><u>ADVANCE SECTION</u></b></tr></td>");
        this.printEnvirVar(out);
        this.printQueueArch(out);
        this.printMPI(response, request);
        this.printOtherQStatCommands(out);
    }

    private void printJSApp(PrintWriter out) {
        out.println("<script language=javascript>");
        out.println("    function validate(){");
        out.println("         //====================================================");
        out.println("         //User must select valid application");
        out.println("         //====================================================");
        out.println("             if (document.frmData.app.value == \"null\"){");
        out.println("               alert(\"Please select a valid application to run\");");
        out.println("       window.event.returnValue=false;");
        out.println("             }");
        out.println("             else if (document.frmData.error.value==\"\"){");
        out.println("               alert(\"Please enter a valid error file name\");");
        out.println("               window.event.returnValue=false;");
        out.println("              }");
        out.println("             else if (document.frmData.output.value==\"\"){");
        out.println("               alert(\"Please enter a valid output file name\");");
        out.println("               window.event.returnValue=false;");
        out.println("              }");
        out.println("    }");
        out.println("</script>");
    }

    private void printJSProject(PrintWriter out) {
        out.println("<script language=javascript>");
        out.println("    function validate(){");
        out.println("         //====================================================");
        out.println("         //User must select valid project");
        out.println("         //====================================================");
        out.println("             if (document.frmData.project.value == \"null\"){");
        out.println("               alert(\"Please select a valid project to run\");");
        out.println("       window.event.returnValue=false;");
        out.println("             }");
        out.println("             else if (document.frmData.error.value==\"\"){");
        out.println("               alert(\"Please enter a valid error file name\");");
        out.println("       window.event.returnValue=false;");
        out.println("              }");
        out.println("             else if (document.frmData.output.value==\"\"){");
        out.println("               alert(\"Please enter a valid output file name\");");
        out.println("       window.event.returnValue=false;");
        out.println("              }");
        out.println("    }");
        out.println("</script>");
    }
}
