package com.tribling.gwt.TomcatWarBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * build the WAR for Apache Tomcat servlet deployment
 * 
 * This code works great for building your gwt projects and deploying them with
 * Tomcat manager check http://gwt-examples.googlecode.com for updates.
 * 
 * http://localhost:8081/manager/html - upload your file and wala, your done In
 * my opinion using the Tomcat manager is so fast and efficient to deploy and
 * application, I'll never change to another platform.
 * 
 * Built for linux and windows compiling, haven't had a chance to test this on a
 * mac, but should work the same as the linux version
 * 
 * @author branflake2267 - Brandon Donnelson
 * 
 *         Watch Console for output. Vars will be outputed there.
 * 
 * 
 * 
 *         Wants In Future TODO - deploy to tomcat automatically TODO - fix
 *         gwt-files classpath in project-compile after import of svn project?
 *         TODO - setup linux, windows compile files fix TODO - auto scan tomcat
 *         configuration for problems - mysql,mssql, jdbc connect -
 *         public/private object permission threw rpc - jars exist?
 * 
 *         Documentation http://tomcat.apache.org/tomcat-6.0-doc/index.html
 */
public class TomcatWarBuilder {

    private String gwtHome;

    private String path_GWT_HOME_Manual = null;

    private String projectCompileFile;

    private String projectCompileFileContents;

    private String classPackage;

    private String projectName;

    private String[] projectDirs;

    private String projectDir;

    private String projectGWTxmlFile;

    private String projectGWTxmlFileLocation;

    private String projectGWTxmlFileContents;

    private String servletPackageClass;

    private String servletPackageClassPath;

    private String servletClassNameIMPL;

    private String classFileContents;

    private String[] classLibs;

    private String webXML;

    private boolean setData = false;

    private WarBuilderData data = null;

    private String tempBuildFolderFullPath;

    private boolean wait = false;

    /**
   * constructor - make the instance
   */
    public void TomCatWarBuilder() {
    }

    /**
   * set the war builder data first, to control the build
   * 
   * @param data
   */
    public void setTomcatCompileData(WarBuilderData data) {
        setData = true;
        this.data = data;
        this.path_GWT_HOME_Manual = data.path_GWT_HOME_Manual;
    }

    /**
   * start the build process
   * 
   * NOTE: don't forget to set the war builder data first
   */
    public void start() {
        if (setData == false) {
            System.out.println("set the WarCompileData first");
            System.exit(1);
        }
        setupProjectVars();
        compileProject();
        buildWar();
        System.out.println("");
        System.out.println("All Done!");
    }

    /**
   * find the compile file and get the vars starting with it
   */
    private void setupProjectVars() {
        checkProjectListForCompileFile();
        readProjectCompileFile();
        getProjectVars();
    }

    /**
   * get project Vars
   * 
   * @throws Exception
   */
    private void getProjectVars() {
        setTempBuildFolder();
        getGwtHome();
        getProjectPackageClassName();
        getProjectDirectoryFromClassName();
        getProjectName();
        getProjectsXMLFile();
        readProjectXMLFileContents();
        getServletPackageClass();
        getServletPackageClassPath();
        getServerClassNameIMPL();
        getClassPathContents();
        getClassLibs();
        System.out.println("Done getting vars.");
    }

    /**
   * build WAR
   * 
   * summary - copy tmp files from production to zip - create web.xml file for
   * guiding the servlet - make an index page for application
   * 
   */
    private void buildWar() {
        deleteProductionFolder(0);
        createDirectoriesForBuild();
        createWebXMLFile();
        createIndexPage();
        copyFilesToProductionBuildFolder();
        zipProject();
        renameWar();
        if (data.deleteTempFolder == true) {
            deleteProductionFolder(1);
        }
        System.out.println("Done Building WAR.");
    }

    /**
   * create the servlet /WEB-INF/web.xml file
   * 
   * Reference http://tomcat.apache.org/tomcat-6.0-doc/config/context.html
   * http://www.caucho.com/resin/doc/filters.xtp
   * 
   * 
   * Name your application ROOT.war to make it the root application for your
   * domain
   * 
   * 
   * <error-page> <error-code>404</error-code> <location>/404.html</location>
   * </error-page>
   */
    private String createWebXMLFileContents() {
        String xml = "";
        xml += "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n";
        xml += "<web-app id=\"/\" xmlns=\"http://java.sun.com/xml/ns/j2ee\" ";
        xml += "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ";
        xml += "xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee ";
        xml += "http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd\" ";
        xml += "version=\"2.4\">\n\n";
        xml += "<display-name>gwt-" + projectName + " Compiled: " + getDate() + "</display-name>\n";
        xml += "<description>Google Web Toolkit Project</description>\n\n";
        if (data.goDirectlyToApplication == true) {
            xml += "" + "<welcome-file-list>\n";
            xml += "\t<welcome-file>" + projectName + ".html</welcome-file>\n";
            xml += "\t<welcome-file>index.jsp</welcome-file>\n";
            xml += "\t<welcome-file>index.html</welcome-file>\n";
            xml += "\t<welcome-file>index.htm</welcome-file>\n";
            xml += "\t<welcome-file>default.jsp</welcome-file>\n";
            xml += "\t<welcome-file>default.htm</welcome-file>\n";
            xml += "\t<welcome-file>default.html</welcome-file>\n";
            xml += "</welcome-file-list>\n";
        }
        if (servletPackageClass != null) {
            xml += "\n<!-- reference project.gwt.xml for servlet class path -->\n";
            xml += "<!-- servlet class -->\n";
            xml += "<servlet>\n";
            xml += "\t<servlet-name>" + projectName + "</servlet-name>\n";
            xml += "\t<servlet-class>" + servletPackageClass + "</servlet-class>\n";
            xml += "</servlet>\n\n";
            xml += "<!-- url/path to servlet class and mapping -->\n";
            xml += "<servlet-mapping>\n";
            xml += "\t<servlet-name>" + projectName + "</servlet-name>\n";
            xml += "\t<url-pattern>" + servletPackageClassPath + "</url-pattern>\n";
            xml += "</servlet-mapping>\n\n";
        }
        xml += createWebXMLFileContents_Security();
        xml += "\n\n\n<!-- custom content included -->\n";
        xml += data.customXmlContent;
        xml += "\n<!-- custom content included -->\n\n";
        xml += "\n\n</web-app>\n\n";
        xml += "<!-- Compiled by the TomcatWarBuilder on http://gwt-examples.googlecode.com -->\n";
        xml += "<!-- " + getDate() + " -->\n";
        return xml;
    }

    /**
   * add security to web.xml file - Need to login to view servlet application
   * 
   * Roles are in /etc/tomcat5.5/tomcat-users.xml - roles are like groups - user
   * needs to be apart of role. You can change the role below to a different
   * role in the list, as long as the users are part of that role that want to
   * login
   * 
   * Recommendation, after setting youself up as a user in tomcat-users.xml use
   * the http://localhost:8081/admin (tomcat admin) to administor the tomcat
   * configuration
   * 
   * @return
   */
    private String createWebXMLFileContents_Security() {
        String xml = "";
        xml += "<!-- Built by TomcatWarBuilder - http://gwt-examples.googlecode.com -->\n\n";
        xml += "\n<!-- Define a Security Constraint on this Application -->\n";
        xml += "<security-constraint>\n";
        xml += "\t<web-resource-collection>";
        xml += "\t<web-resource-name>Entire Application</web-resource-name>\n";
        xml += "\t<url-pattern>/*</url-pattern>\n";
        xml += "\t</web-resource-collection>\n";
        xml += "\t<auth-constraint>\n";
        xml += "\t<role-name>admin</role-name>\n";
        xml += "\t</auth-constraint>\n";
        xml += "</security-constraint>\n\n";
        xml += "<login-config>\n";
        xml += "\t<auth-method>BASIC</auth-method>\n";
        xml += "\t<realm-name>Your Realm Name</realm-name>\n";
        xml += "</login-config>\n\n";
        xml += "<security-role>\n";
        xml += "\t<description>The role that is required to log in to the Manager Application</description>\n";
        xml += "\t<role-name>admin</role-name>\n";
        xml += "</security-role>\n\n";
        if (data.askForLogin == true) {
            return xml;
        } else {
            return "";
        }
    }

    /**
   * get todays date for adding to the project name in the xml file gives a
   * bearing of when we built this project
   * 
   * @return
   */
    private String getDate() {
        Date date = new Date();
        String now;
        try {
            now = date.toString();
        } catch (Exception e) {
            e.printStackTrace();
            now = " getDate(); error ";
        }
        return now;
    }

    /**
   * create an index page for easy application access, if not directly going to
   * app. This can be used for quickly testing, especially if your using your
   * application as an include on another site.
   * 
   * @return
   */
    private String createIndexPageContents() {
        String linkToApplication = projectName + ".html";
        String linkToApplicationDescription = linkToApplication + " - goes to your gwt application";
        String html = "";
        html += "<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">\n";
        html += "<html>\n";
        html += "<head>";
        html += "<title>GWT Application HomePage - " + projectName + "</title>";
        html += "</head>";
        html += "<body>\n";
        html += "";
        html += "\t<a href=\"";
        html += linkToApplication;
        html += "\">";
        html += linkToApplicationDescription;
        html += "</a> Quick link to your gwt module.\n";
        html += "</body>\n";
        html += "</html>\n";
        return html;
    }

    /**
   * create file ./production/index.jsp
   */
    private void createIndexPage() {
        String sep = getPathsFileSeparator();
        String File = tempBuildFolderFullPath + sep + "index.jsp";
        createFile(File, createIndexPageContents());
    }

    /**
   * setup temp build folder, staging area for files
   */
    private void setTempBuildFolder() {
        String sep = getPathsFileSeparator();
        this.tempBuildFolderFullPath = data.projectDirectory + sep + data.tempBuildFolderName;
    }

    /**
   * delete the previous production folder
   */
    private void deleteProductionFolder(int when) {
        String s = "";
        if (when == 0) {
            s = "Previous";
        }
        File dir = new File(tempBuildFolderFullPath);
        System.out.println("Deleting " + s + " production directory contents: " + tempBuildFolderFullPath);
        try {
            deleteDir(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
   * delete folder contents
   * 
   * @param dir
   * @return
   */
    private boolean deleteDir(File dir) {
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
   * create file ./production/WEB-INF/web.xml
   */
    private void createWebXMLFile() {
        String sep = getPathsFileSeparator();
        String File = tempBuildFolderFullPath + sep + "WEB-INF" + sep + "web.xml";
        createFile(File, createWebXMLFileContents());
    }

    /**
   * create Folders for build
   */
    private void createDirectoriesForBuild() {
        System.out.println("");
        System.out.println("Starting Build");
        String sep = getPathsFileSeparator();
        String ProductionFolder = tempBuildFolderFullPath;
        createFolder(ProductionFolder);
        String ProductionWebInf = tempBuildFolderFullPath + sep + "WEB-INF";
        createFolder(ProductionWebInf);
        String ProductionWebInfClasses = tempBuildFolderFullPath + sep + "WEB-INF" + sep + "classes";
        createFolder(ProductionWebInfClasses);
        String ProductionWebInfLib = tempBuildFolderFullPath + sep + "WEB-INF" + sep + "lib";
        createFolder(ProductionWebInfLib);
    }

    /**
   * copy the files in the correct folders
   * 
   * @throws IOException
   */
    private void copyFilesToProductionBuildFolder() {
        copyWWWFiles();
        copyCompiledClasses();
        copyJarFiles();
    }

    /**
   * copy the compiled classes to /production/WEB-INF/classes
   */
    private void copyCompiledClasses() {
        String sep = getPathsFileSeparator();
        String src = data.projectDirectory + sep + "bin";
        String dest = tempBuildFolderFullPath + sep + "WEB-INF" + sep + "classes";
        try {
            copyFiles(src, dest);
        } catch (IOException e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    /**
   * copy files - ./www to ./production/
   * 
   * www is the compiled project directory production is the temp directory for
   * setting up the war archive
   * 
   * Notes Archive/www/*files - have to change the servelt context path if you
   * change this String dest = TempBuildFolder + "/www";
   */
    private void copyWWWFiles() {
        System.out.println("Copying gwt WWW files to the temp directory");
        String sep = getPathsFileSeparator();
        String src = data.projectDirectory + sep + "www" + sep + classPackage;
        String dest = tempBuildFolderFullPath;
        try {
            copyFiles(src, dest);
        } catch (IOException e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

    /**
   * copy class jars to the libs folder in the temp production folder
   * 
   * @throws IOException
   */
    private void copyJarFiles() {
        String sep = getPathsFileSeparator();
        System.out.println("Copying Jars" + classLibs.length + " (Using .classpath for locating jars)");
        String src = null;
        String dest = null;
        for (int i = 0; i < (classLibs.length); i++) {
            src = classLibs[i];
            if (src.contains("/") && data.os == 1) {
                src = src.replaceAll("/", "\\\\");
            }
            String DestFile = getDestinationDirectory(src);
            dest = tempBuildFolderFullPath + sep + "WEB-INF" + sep + "lib" + sep + DestFile;
            try {
                copyFiles(src, dest);
            } catch (IOException e) {
                System.err.println(e);
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
   * get class path file contents search .classpath for libs to add
   */
    private void getClassPathContents() {
        String sep = getPathsFileSeparator();
        String classpathfilelocation = data.projectDirectory + sep + ".classpath";
        File file = new File(classpathfilelocation);
        if (file.isFile() == false) {
            System.out.println("Can't read the .classpath file or it does not exist");
            System.exit(1);
        }
        classFileContents = readFile(file);
        System.out.println("ClassFileContents: " + classFileContents);
    }

    /**
   * get jar files location
   * 
   */
    private void getClassLibs() {
        String re = "lib.*?path=['\"](.*?jar)['\"]";
        Pattern p = Pattern.compile(re);
        Matcher matcher = p.matcher(classFileContents);
        int i = 0;
        while (matcher.find()) {
            i++;
        }
        String[] jars = new String[i];
        matcher.reset();
        i = 0;
        while (matcher.find()) {
            String match = matcher.group(1);
            System.out.println("Class libs: " + i + ". " + match);
            jars[i] = match;
            i++;
        }
        boolean addGwtServlet = true;
        jars = deleteGwtUserJar(jars, addGwtServlet);
        classLibs = jars;
    }

    /**
   * Switch out gwt-user.jar with gwt-servlet.jar
   * 
   * @param jars
   * @param AddGwtServlet
   *          - tell it to replace gwt-servlet.jar and not erase it
   * @return
   */
    private String[] deleteGwtUserJar(String[] jars, boolean AddGwtServlet) {
        String jar = null;
        for (int i = 0; i < jars.length; i++) {
            jar = jars[i].toString();
            boolean has = jars[i].contains("gwt-user.jar");
            if (has == true) {
                if (AddGwtServlet == true) {
                    System.out.println("ClassLibs Changing gwt-user.jar to gwt-servlet.jar");
                    jars[i] = gwtHome + "gwt-servlet.jar";
                } else {
                    jars[i] = null;
                }
            }
        }
        return jars;
    }

    /**
   * get projects client xml settings
   */
    private void getProjectsXMLFile() {
        String sep = getPathsFileSeparator();
        int depth = projectDirs.length - 1;
        String projectName = projectDirs[depth];
        projectGWTxmlFile = projectName + ".gwt.xml";
        projectGWTxmlFileLocation = data.projectDirectory + sep + "src" + projectDir + sep + projectGWTxmlFile;
        System.out.println("ProjectGWTxmlFile: " + projectGWTxmlFile);
        System.out.println("ProjectGWTxmlFileLocation: " + projectGWTxmlFileLocation);
    }

    /**
   * read xml files contents
   */
    private void readProjectXMLFileContents() {
        File file = new File(projectGWTxmlFileLocation);
        projectGWTxmlFileContents = readFile(file);
        if (projectGWTxmlFileContents == null) {
            System.out.println("Could not read project.gwt.xml file. (" + projectGWTxmlFileLocation + ")");
        }
        System.out.println("ProjectGWTxmlFileContents: " + projectGWTxmlFileContents);
        if (projectGWTxmlFileContents.length() == 0) {
            System.out.println("Could not read your GWT XML file. debug: readProjectXMLFileContents");
            System.exit(1);
        }
    }

    /**
   * get servlet class
   * 
   */
    private void getServletPackageClass() {
        Pattern p = Pattern.compile("<servlet.*?class=[\"'](.*?)[\"'].*?>");
        Matcher m = p.matcher(projectGWTxmlFileContents);
        boolean found = m.find();
        if (found == true) {
            servletPackageClass = m.group(1);
        }
        System.out.println("ServletPackageClass: " + servletPackageClass);
        if (servletPackageClass == null) {
            System.out.println("No servlet class, means you don't have any server side rpc. debug: getServletPackageClass()");
        }
    }

    /**
   * get servlet package/class
   * 
   * This is the url-pattern, or rpcServicePath. Or servlet context that
   * redirects to the rpc class
   */
    private void getServletPackageClassPath() {
        Pattern p = Pattern.compile("<servlet.*?path=[\"'](.*?)[\"'].*?>");
        Matcher m = p.matcher(projectGWTxmlFileContents);
        boolean found = m.find();
        if (found == true) {
            servletPackageClassPath = m.group(1);
        }
        System.out.println("getServletPackageClassPath: " + servletPackageClassPath);
        System.out.println("");
    }

    /**
   * get the directory structure from the 'com.example.test.client.application'
   * 
   * name and no . in it
   * 
   * "com.domain.gwt.class.client.class"
   */
    private void getProjectDirectoryFromClassName() {
        String sep = getPathsFileSeparator();
        String[] dirs = null;
        if (classPackage.contains(".")) {
            dirs = classPackage.split("\\.");
        } else {
            dirs[0] = new String(classPackage);
        }
        projectDirs = dirs;
        String dir = "";
        for (int i = 0; i < (dirs.length - 1); i++) {
            dir = dir + sep + dirs[i].toString();
        }
        projectDir = dir;
        System.out.println("ProjectDir: " + dir);
    }

    /**
   * get project name from class name
   */
    private void getProjectName() {
        projectName = projectDirs[projectDirs.length - 1].toString();
        System.out.println("ProjectName: " + projectName);
    }

    /**
   * get path/directory in array
   * 
   * @param file
   * @return
   */
    private String getDestinationDirectory(String file) {
        String sep = "";
        if (data.os == 0) {
            sep = "/";
        } else {
            sep = "\\\\";
        }
        String[] dirs = file.split(sep);
        String dir = dirs[dirs.length - 1];
        return dir;
    }

    /**
   * get the server class name "ServiceImpl"
   * 
   * "com.domain.gwt.class.client.class"
   */
    private void getServerClassNameIMPL() {
        if (servletPackageClass == null) {
            return;
        }
        String[] dirs = null;
        if (servletPackageClass.contains(".")) {
            dirs = servletPackageClass.split("\\.");
        } else {
            dirs[0] = new String(servletPackageClass);
        }
        servletClassNameIMPL = dirs[dirs.length - 1].toString();
        System.out.println("ServletClassNameIMPL: " + servletClassNameIMPL);
    }

    /**
   * get the package name (and class) that the src files are in
   * com.tribling.gwt.test.client.MyProject or
   * com.tribling.gwt.test.client.MyProject;
   * 
   */
    private void getProjectPackageClassName() {
        Pattern p = Pattern.compile(".*\040(.*?)$|.*\040(.*?);$");
        Matcher m = p.matcher(projectCompileFileContents);
        boolean found = m.find();
        if (found == true) {
            classPackage = m.group(1);
        }
        classPackage = classPackage.replace(";", "");
        System.out.println("classPackage: " + classPackage);
        System.out.println("");
        if (classPackage.length() == 0) {
            System.out.println("Cant figure out the class project in the compile file. debug getProjectPackageClassName()");
            System.exit(1);
        }
    }

    /**
   * get the location for the gwt files from eclipse class path contents
   * 
   * TODO - errors when the class path is relative
   */
    private void getGwtHome() {
        String sep = getPathsFileSeparator();
        if (path_GWT_HOME_Manual != null) {
            gwtHome = path_GWT_HOME_Manual;
            return;
        }
        String re = "";
        if (projectCompileFileContents.contains("\\")) {
            re = "([a-zA-Z]:.*?)gwt-user.jar";
        } else {
            re = ":(" + sep + ".*)gwt-user.jar:";
        }
        Pattern p = Pattern.compile(re);
        Matcher m = p.matcher(projectCompileFileContents);
        boolean found = m.find();
        if (found == true) {
            gwtHome = m.group(1);
        } else {
            System.out.println("Can't find gwtHome Directory, in the ProjectCompileFileContents Classpath. debug getGwtHome()");
            System.exit(1);
        }
        System.out.println("gwtHome dir: " + gwtHome);
    }

    /**
   * read the gwt-compile folder to get contents
   */
    private void readProjectCompileFile() {
        String sep = getPathsFileSeparator();
        String directory = data.projectDirectory + sep + projectCompileFile;
        File ProjectCompileFile = new File(directory);
        projectCompileFileContents = readFile(ProjectCompileFile);
        System.out.println("FileContents: " + projectCompileFileContents);
    }

    /**
   * read the project xml file to get contents
   * 
   * note: this isn't used at the moment can't remember why
   */
    private void readProjectGWTxmlFile() {
        String sep = getPathsFileSeparator();
        String Dir = data.projectDirectory + sep + projectGWTxmlFile;
        File ProjectCompileFile = new File(Dir);
        projectGWTxmlFileContents = readFile(ProjectCompileFile);
        System.out.println("FileContents: " + projectGWTxmlFileContents);
    }

    /**
   * get file list in the directory (find project-compile)
   * 
   * looking for project-compile, which builds the project from console
   */
    private void checkProjectListForCompileFile() {
        File file;
        String list[];
        file = new File(data.projectDirectory);
        if (file.isDirectory() == false) {
            System.out.println("Your data.ProjectDirectory is not a directory or does not exist. (debug: " + data.projectDirectory + ")");
            System.out.println("Make sure you have the correct path");
            System.exit(1);
        }
        list = file.list();
        if (list == null) {
            System.out.println("Error reading current directory. debug: checkProjectListForCompile()");
            System.exit(1);
        }
        findProjectCompile(list);
    }

    /**
   * find the project gwt-compile file
   * 
   * @param list
   */
    private void findProjectCompile(String[] list) {
        String file = null;
        boolean found = false;
        for (int i = 0; i <= list.length; i++) {
            file = list[i];
            found = checkForCompile(file);
            if (found == true) {
                projectCompileFile = file;
                return;
            }
        }
    }

    /**
   * get the file that says project-compile
   * 
   * @param file
   * @return
   */
    private boolean checkForCompile(String file) {
        if (file == null) {
            return false;
        }
        boolean found = false;
        Pattern p = Pattern.compile(".*compile");
        Matcher m = p.matcher(file);
        found = m.find();
        return found;
    }

    /**
   * create new directory
   * 
   * @param dir
   * @return
   */
    private boolean createFolder(String dir) {
        boolean status = false;
        String newDir = null;
        if (dir != null) {
            newDir = dir;
            status = new File(newDir).mkdir();
        }
        System.out.println("Created Production Dir: " + newDir);
        return status;
    }

    /**
   * read file contents
   * 
   * @param file
   * @return
   */
    private String readFile(File file) {
        if (file.length() == 0) {
            System.out.println("Could not read file; debug: readFile()");
        }
        String sFile = null;
        try {
            FileReader input = new FileReader(file);
            BufferedReader bufRead = new BufferedReader(input);
            String line;
            int count = 0;
            line = bufRead.readLine();
            sFile = line;
            count++;
            while (line != null) {
                line = bufRead.readLine();
                if (line != null) {
                    sFile = sFile + line;
                }
                count++;
            }
            bufRead.close();
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Usage: java ReadFile filename\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sFile;
    }

    /**
   * copy files
   * 
   * @param srcPath
   * @param dstPath
   * @throws IOException
   */
    private void copyFiles(String srcPath, String dstPath) throws IOException {
        String sep = getPathsFileSeparator();
        File src = new File(srcPath);
        File dest = new File(dstPath);
        if (src.isDirectory() == true && src.getName() != ".svn") {
            dest.mkdirs();
            String list[] = src.list();
            for (int i = 0; i < list.length; i++) {
                String src1 = src.getAbsolutePath() + sep + list[i];
                String dest1 = dest.getAbsolutePath() + sep + list[i];
                if (list[i].toString().matches(".svn") != true) {
                    copyFiles(src1, dest1);
                }
            }
        } else {
            if (src.isFile() == false) {
                System.out.println("ERROR: For Some reason this file does not exist: " + srcPath);
            }
            FileInputStream fin = new FileInputStream(src);
            FileOutputStream fout = new FileOutputStream(dest);
            System.out.println("Copying file src:" + src + " dest:" + dest);
            int c;
            while ((c = fin.read()) >= 0) {
                fout.write(c);
            }
            fin.close();
            fout.close();
        }
    }

    /**
   * create file with these contents
   * 
   * @param createFilePath
   * @param contents
   */
    private void createFile(String createFilePath, String contents) {
        try {
            File file = new File(createFilePath);
            boolean success = file.createNewFile();
            if (success) {
                BufferedWriter out = new BufferedWriter(new FileWriter(createFilePath, true));
                out.write(contents);
                out.close();
            } else {
                (new File(createFilePath)).delete();
                createFile(createFilePath, contents);
            }
        } catch (IOException e) {
            System.out.println("Could not create file: (debug: createFilePath() " + createFilePath + ")");
            System.out.println("Do you have write permission for this location?");
            System.exit(1);
        }
    }

    /**
   * zip up the production folder
   * 
   * WAR archive is a zip archive
   * 
   * @throws IOException
   */
    private void zipProject() {
        System.out.println("Zipping up archive. WAR file generation");
        String zipFile = getWarFilePathAndName();
        String zipUp = tempBuildFolderFullPath;
        System.out.println("WarZipFile: " + zipFile + " ZipUpDir:" + zipUp);
        FileOutputStream file = null;
        try {
            file = new FileOutputStream(zipFile);
        } catch (FileNotFoundException e) {
            System.out.println("Could not make the zip file. debug: zipProject");
            e.printStackTrace();
            System.exit(1);
        }
        ZipOutputStream zos = new ZipOutputStream(file);
        zipTempDir(zipUp, zos);
        try {
            zos.close();
        } catch (IOException e) {
            System.out.println("could not close ZipOutputStream");
            e.printStackTrace();
        }
    }

    /**
   * get war file with entire path
   * 
   * @return
   */
    private String getWarFilePathAndName() {
        String sep = getPathsFileSeparator();
        String warFile = null;
        if (data.tempWarFileLocation == null) {
            warFile = data.projectDirectory + sep + projectName + ".war";
        } else {
            warFile = data.tempWarFileLocation + sep + projectName + ".war";
        }
        return warFile;
    }

    /**
   * get war directory path
   * 
   * @return
   */
    private String getWarDir() {
        String warDir = null;
        if (data.tempWarFileLocation == null) {
            warDir = data.projectDirectory;
        } else {
            warDir = data.tempWarFileLocation;
        }
        return warDir;
    }

    /**
   * archive temp build folder
   * 
   * @param dir2zip
   * @param zos
   */
    private void zipTempDir(String dir2zip, ZipOutputStream zos) {
        try {
            File zipDir = new File(dir2zip);
            String[] dirList = zipDir.list();
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    String filePath = f.getPath();
                    zipTempDir(filePath, zos);
                    continue;
                }
                FileInputStream fis = new FileInputStream(f);
                String filePath = f.getPath();
                filePath = getZipPath(filePath);
                ZipEntry anEntry = new ZipEntry(filePath);
                zos.putNextEntry(anEntry);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                fis.close();
            }
        } catch (Exception e) {
            System.out.println("Could not do the zipDir process. debug zipDir()");
            System.out.println("This would be an internal variable configuration error. I guess you'll have to debug it");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
   * Figure out the build path, so we can set the zip archive to relative path
   * 
   * TODO - veryify name change works
   * 
   * @param path
   * @return
   */
    private String getZipPath(String path) {
        String sep = getPathsFileSeparator();
        String tmp = "";
        if (data.os == 1) {
            tmp = data.tempBuildFolderName + sep + sep;
        } else {
            tmp = data.tempBuildFolderName + sep;
        }
        String re = ".*" + tmp + "(.*)";
        Pattern p = Pattern.compile(re);
        Matcher m = p.matcher(path);
        boolean found = m.find();
        String newPath = "";
        if (found == true) {
            newPath = m.group(1);
        }
        System.out.println("Copying to zip archive (warPath): " + newPath);
        return newPath;
    }

    /**
   * rename war file to desired name
   */
    private void renameWar() {
        String sep = getPathsFileSeparator();
        if (data.renameWarFile == false && data.renameWarFileNameTo != null) {
            return;
        }
        if (data.renameWarFileNameTo.equals("")) {
            System.out.println("renameWarFielNameTo is null, add that field");
        }
        String warFile = getWarFilePathAndName();
        String warDir = getWarDir();
        String newName = warDir + sep + data.renameWarFileNameTo;
        File file = new File(warFile);
        File file2 = new File(newName);
        boolean success = file.renameTo(file2);
        if (!success) {
            System.out.println("failed to rename war.");
        } else {
            System.out.println("Rename War: " + file2);
        }
    }

    /**
   * get file path seperator for operating system
   * 
   * @return
   */
    private String getPathsFileSeparator() {
        String s = "/";
        if (data.os == 0) {
            s = "/";
        } else if (data.os == 1) {
            s = "\\";
        }
        if (s.length() == 0) {
            System.out.println("no os was selected");
            System.out.println("select an os number 0 or 1, this determines what type of file system exists.");
            System.exit(1);
        }
        return s;
    }

    /**
   * compile the eclipse project first
   * 
   * TODO - change permissions on folder if on os 0, so to not have to worry
   * about manual exec permission
   */
    private void compileProject() {
        String sep = getPathsFileSeparator();
        String runPath = data.projectDirectory + sep + projectCompileFile;
        System.out.println("Compiling gwt application - This may take a minute to complete! Be patient.");
        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(runPath);
            proc.waitFor();
        } catch (Throwable t) {
            System.out.println("If you get a permission error on linux, 'chmod 777 project-compile'");
            System.out.println("Was not able to exec the project-compile file to compile project");
            t.printStackTrace();
            System.exit(1);
        }
        System.out.println("Done compiling gwt application");
    }

    /**
   * TODO - in the future use a worker to show the wait during compile
   */
    private void showWait() {
        int i = 0;
        do {
            System.out.print(".");
            if (i == 40) {
                i = 0;
                System.out.println(".");
            }
            i++;
        } while (wait == true);
    }
}
