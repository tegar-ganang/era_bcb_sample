package jriaffe.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import jriaffe.client.Application;
import jriaffe.client.NotificationCenter;
import org.jdesktop.http.Response;
import org.jdesktop.http.Session;
import org.jdesktop.http.StatusCode;

/**
 *
 * @author preisler
 */
public final class ApplicationInstaller {

    public static final String APPLICATION_INSTALLED = "applicationInstalled";

    private SystemPreferences prefs;

    private String applicationURL;

    private URL url;

    private InstallerObserver delegate = new DefaultDelegate();

    /** Creates a new instance of ApplicationInstaller */
    private ApplicationInstaller() {
    }

    public static ApplicationInstaller init(String URL) throws Exception {
        if (URL == null) {
            throw new Exception("URL is null.  Must pass in a value for URL.");
        }
        ApplicationInstaller installer = new ApplicationInstaller();
        if (!URL.endsWith("/")) {
            installer.applicationURL = URL + "/";
        } else {
            installer.applicationURL = URL;
        }
        try {
            installer.url = new URL(installer.applicationURL);
        } catch (MalformedURLException ex) {
            Logger.getLogger(ApplicationInstaller.class.getName()).log(Level.SEVERE, null, ex);
        }
        installer.prefs = SystemPreferences.init();
        return installer;
    }

    public void install() throws Exception {
        String appDirName = buildAppDirName();
        String applicationDir = prefs.getApplicationDirectory() + System.getProperty("file.separator") + buildDomainDiskDir() + System.getProperty("file.separator") + appDirName;
        String applicationLogDir = prefs.getApplicationDirectory() + System.getProperty("file.separator") + buildDomainDiskDir() + System.getProperty("file.separator") + appDirName + System.getProperty("file.separator") + "logs";
        String domainDirectory = prefs.getApplicationDirectory() + System.getProperty("file.separator") + buildDomainDiskDir();
        File domainDir = new File(domainDirectory);
        domainDir.mkdir();
        String domainStringDiskDir = domainDirectory + System.getProperty("file.separator") + "disk";
        File domainDiskDir = new File(domainStringDiskDir);
        domainDiskDir.mkdir();
        String appStringDiskDir = domainDirectory + System.getProperty("file.separator") + "disk";
        File appDiskDir = new File(appStringDiskDir);
        appDiskDir.mkdir();
        File appDir = new File(applicationDir);
        appDir.mkdir();
        File appLogDir = new File(applicationLogDir);
        appLogDir.mkdir();
        String appFileName = downloadFile(applicationDir, "application.nba");
        File appFile = new File(appFileName);
        Application application = Application.init(appDir, appFile);
        try {
            checkCodeBase(application);
            downloadApplicationFile(applicationDir, application);
            writeLogFileProperties(applicationLogDir, application.getAppName());
            delegate.message("Preparing security file.");
        } finally {
            writeSecurityFile(application, applicationDir, applicationLogDir, appStringDiskDir, domainStringDiskDir);
            delegate.installComplete();
            NotificationCenter.getDefaultNotificationCenter().postNotification(APPLICATION_INSTALLED, application);
        }
    }

    private void downloadApplicationFile(String applicationDir, Application application) throws Exception {
        downloadFile(applicationDir, application.getDefaultJar());
        if (application.getIcon() != null) {
            downloadFile(applicationDir, application.getIcon());
        }
        if (application.getSplash() != null) {
            downloadFile(applicationDir, application.getSplash());
        }
        if (application.getDefaultJar().endsWith(".nap")) {
            JarFile file = new JarFile(applicationDir + System.getProperty("file.separator") + application.getDefaultJar());
            Enumeration<JarEntry> e = file.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = e.nextElement();
                String fileName = applicationDir + System.getProperty("file.separator") + entry.getName();
                if (entry.isDirectory()) {
                    new File(fileName).mkdir();
                } else {
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        in = file.getInputStream(entry);
                        out = new BufferedOutputStream(new FileOutputStream(fileName));
                        int len;
                        byte[] buffer = new byte[1024];
                        while ((len = in.read(buffer)) >= 0) {
                            out.write(buffer, 0, len);
                        }
                    } finally {
                        if (in != null) {
                            in.close();
                        }
                        if (out != null) {
                            out.close();
                        }
                    }
                }
            }
        }
    }

    private String downloadFile(String applicationDir, String fileName) throws Exception {
        Session webSession = new Session();
        Response response = webSession.get(applicationURL + fileName);
        long contentLength = 0;
        try {
            contentLength = Long.parseLong(response.getHeader("Content-Length").getValue());
        } catch (NumberFormatException ex) {
        }
        delegate.downloadingFile(applicationDir, fileName, contentLength);
        if (response.getStatusCode() != StatusCode.OK) {
            throw new Exception("HTTP error downloading file " + fileName + ".  Error code: " + response.getStatusCode() + " Error Message:" + response.getStatusText());
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            in = response.getBodyAsStream();
            String fullFileName = applicationDir + System.getProperty("file.separator") + fileName;
            out = new FileOutputStream(fullFileName);
            int b = -1;
            int byteCount = 0;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return fullFileName;
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
            if (in != null) {
                in.close();
            }
            delegate.doneDownloadingFile("Done downloading file " + fileName);
        }
    }

    /**
     *  Strip off the http:// from the front of the string, strip off trailing / and 
     *  replace remaining / with _ 
     */
    public String buildAppDirName() {
        String appDirName = url.getPath();
        if (appDirName.endsWith("/")) {
            appDirName = appDirName.substring(0, appDirName.length() - 1);
        }
        return appDirName.replace('/', '_');
    }

    public String buildDomainDiskDir() {
        return url.getHost();
    }

    /**
     * @return the delegate
     */
    public InstallerObserver getDelegate() {
        return delegate;
    }

    /**
     * @param delegate the delegate to set
     */
    public void setDelegate(InstallerObserver delegate) {
        this.delegate = delegate;
    }

    /**
     * Codebase must match URL passed in for application. If the codebase does not match
     * the application will not be downloaded or installed.
     */
    private void checkCodeBase(Application application) throws Exception {
        if (application.getCodeBase() == null) {
            String em = "Application.nba file missing codebase URL.";
            Logger.getLogger(ApplicationInstaller.class.getName()).log(Level.WARNING, em);
            throw new Exception(em);
        }
        String installUrl = url.toString();
        String appUrl = application.getCodeBase().toString();
        if (appUrl.endsWith("/") == false) {
            appUrl = appUrl + "/";
        }
        if (installUrl.endsWith("/") == false) {
            installUrl = installUrl + "/";
        }
        if (!installUrl.equals(appUrl)) {
            String msg = "Application " + application.getAppName() + " not installed.  " + "Codebase in application.nba file does not match requested URL. " + "application.nba URL: " + application.getCodeBase() + " download URL: " + url;
            Logger.getLogger(ApplicationInstaller.class.getName()).log(Level.WARNING, msg);
            throw new Exception(msg);
        }
    }

    private void writeLogFileProperties(String appLogDir, String appName) throws IOException {
        delegate.message("Writing log file properties.");
        Properties props = new Properties();
        props.setProperty("handlers", "java.util.logging.ConsoleHandler, java.util.logging.FileHandler");
        props.setProperty(".level", "ALL");
        props.setProperty("java.util.logging.ConsoleHandler.level", "INFO");
        props.setProperty("java.util.logging.ConsoleHandler.formatter", "java.util.logging.SimpleFormatter");
        props.setProperty("java.util.logging.FileHandler.level", "ALL");
        props.setProperty("java.util.logging.FileHandler.pattern", appLogDir + System.getProperty("file.separator") + appName + ".log");
        props.setProperty("java.util.logging.FileHandler.limit", "50000");
        props.setProperty("java.util.logging.FileHandler.count", "1");
        props.setProperty("java.util.logging.FileHandler.formatter", "java.util.logging.XMLFormatter");
        Writer out = new FileWriter(appLogDir + System.getProperty("file.separator") + "logging.properties");
        props.store(out, "Generated by jriaffe at install time");
    }

    private void writeSecurityFile(Application application, String appDir, String appLogDir, String appDiskDir, String domainDiskDir) throws IOException {
        Writer out = null;
        InputStream template = null;
        try {
            out = new FileWriter(appDir + System.getProperty("file.separator") + "jriaffe.policy");
            template = ApplicationInstaller.class.getResourceAsStream("/jriaffe/core/jriaffe.policy");
            int i = -1;
            while ((i = template.read()) != -1) {
                out.write(i);
            }
            out.write("permission java.io.FilePermission \"" + fixPath(appLogDir) + "/*\", \"read,write\";");
            out.write("\r");
            out.write("permission java.io.FilePermission \"" + fixPath(appDir) + "\", \"read\";");
            out.write("\r");
            out.write("permission java.io.FilePermission \"" + fixPath(appDir) + "/*\", \"read\";");
            out.write("\r");
            out.write("permission java.io.FilePermission \"" + fixPath(domainDiskDir) + "/*\", \"read,write\";");
            out.write("\r");
            out.write("permission java.io.FilePermission \"" + fixPath(appDiskDir) + "/*\", \"read,write\";");
            out.write("\r");
            List<String> hosts = application.getHosts();
            if (hosts != null) {
                for (String host : hosts) {
                    out.write("permission java.net.SocketPermission \"" + host + "\", \"connect,resolve\";");
                    out.write("\r");
                }
            }
            out.write("};");
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
            if (template != null) {
                template.close();
            }
        }
    }

    private String fixPath(String path) {
        path = path.replace("\\", "/");
        return path;
    }
}

class DefaultDelegate implements InstallerObserver {

    public void downloadingFile(String filename, long size) {
        System.out.println("Installer downloading file: " + filename + " with size " + size);
    }

    public void downloadingFile(String path, String filename, long size) {
        System.out.println("Done downloading file: " + filename);
    }

    public void installComplete() {
        System.out.println("Download complete");
    }

    public boolean installWarning(String message) {
        System.out.println("Download Warning " + message);
        return true;
    }

    public void message(String message) {
        System.out.println(message);
    }

    public void doneDownloadingFile(String filename) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
