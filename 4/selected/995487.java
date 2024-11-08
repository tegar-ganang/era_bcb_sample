package org.regilo.core.installer.jobs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.regilo.core.RegiloPlugin;
import org.regilo.core.installer.InstallConfiguration;
import org.regilo.core.installer.RegiloInstallerPlugin;
import org.regilo.core.installer.tar.TarEntry;
import org.regilo.core.installer.tar.TarException;
import org.regilo.core.installer.tar.TarFile;
import org.regilo.core.model.WebSiteConnector;
import org.regilo.core.utils.FileSystemUtils;
import org.regilo.core.utils.HeadlessBrowser;
import org.regilo.database.model.MysqlSiteConnector;
import org.regilo.ftp.client.FtpClient;
import org.regilo.ftp.client.exceptions.FtpException;
import org.regilo.ftp.model.FtpSiteConnector;

public class InstallJob implements IRunnableWithProgress {

    private InstallConfiguration installConfiguration;

    private FtpSiteConnector ftpSiteConnector;

    public InstallJob(String name) {
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        ftpSiteConnector = (FtpSiteConnector) installConfiguration.getSite().getConnector(FtpSiteConnector.TYPE);
        File downloadDirectory = FileSystemUtils.getDir(RegiloInstallerPlugin.DOWNLOAD_DIR);
        SubMonitor subMonitor = SubMonitor.convert(monitor, "install", 220);
        try {
            downloadDrupal(subMonitor.newChild(40, SubMonitor.SUPPRESS_NONE), downloadDirectory, installConfiguration);
            downloadCustomModules(subMonitor.newChild(20, SubMonitor.SUPPRESS_NONE), downloadDirectory, installConfiguration);
            downloadTheme(subMonitor.newChild(20, SubMonitor.SUPPRESS_NONE), downloadDirectory, installConfiguration);
            downloadLanguage(subMonitor.newChild(20, SubMonitor.SUPPRESS_NONE), installConfiguration);
            downloadInstallationProfile(subMonitor.newChild(20, SubMonitor.SUPPRESS_NONE), downloadDirectory, installConfiguration);
            File tempDirectory = new File(FileSystemUtils.getDir(RegiloPlugin.TEMP_DIR), UUID.randomUUID().toString());
            tempDirectory.mkdir();
            untar(subMonitor.newChild(20, SubMonitor.SUPPRESS_NONE), tempDirectory, installConfiguration);
            File drupalDir = createPackage(subMonitor.newChild(20, SubMonitor.SUPPRESS_NONE), tempDirectory, installConfiguration);
            upload(subMonitor.newChild(40, SubMonitor.SUPPRESS_NONE), drupalDir, installConfiguration);
            install(subMonitor.newChild(20, SubMonitor.SUPPRESS_NONE), installConfiguration);
        } finally {
            if (subMonitor != null) {
                subMonitor.done();
            }
        }
        monitor.done();
    }

    private void downloadDrupal(IProgressMonitor monitor, File downloadDirectory, InstallConfiguration installConfiguration) throws InvocationTargetException {
        String taskName = "download drupal core and modules";
        SubMonitor subMonitor = SubMonitor.convert(monitor, taskName, installConfiguration.getModules().size() + 1);
        try {
            String ftp = installConfiguration.getDrupalFtp();
            String remotePath = installConfiguration.getDrupalRemotePath();
            File coreDir = new File(downloadDirectory, "core");
            coreDir.mkdir();
            downloadComponent(ftp, remotePath, coreDir, installConfiguration.getCore(), taskName, subMonitor.newChild(1, SubMonitor.SUPPRESS_NONE));
            File modulesDir = new File(downloadDirectory, "modules");
            modulesDir.mkdir();
            for (String component : installConfiguration.getModules()) {
                downloadComponent(ftp, remotePath, modulesDir, component, taskName, subMonitor.newChild(1, SubMonitor.SUPPRESS_NONE));
            }
        } finally {
            if (subMonitor != null) {
                subMonitor.done();
            }
        }
    }

    private void downloadCustomModules(IProgressMonitor monitor, File downloadDirectory, InstallConfiguration installConfiguration) throws InvocationTargetException {
        String taskName = "dowload custom modules";
        SubMonitor subMonitor = SubMonitor.convert(monitor, taskName, installConfiguration.getCustomModules().size());
        try {
            String ftp = installConfiguration.getCustomFtp();
            String remotePath = installConfiguration.getCustomRemotePath();
            String username = installConfiguration.getCustomUsername();
            String password = installConfiguration.getCustomPassword();
            File customDir = new File(downloadDirectory, "custom");
            customDir.mkdir();
            for (String component : installConfiguration.getCustomModules()) {
                downloadComponent(ftp, remotePath, customDir, component, username, password, taskName, subMonitor.newChild(1, SubMonitor.SUPPRESS_NONE));
            }
        } finally {
            if (subMonitor != null) {
                subMonitor.done();
            }
        }
    }

    private void downloadTheme(IProgressMonitor monitor, File downloadDirectory, InstallConfiguration installConfiguration) throws InvocationTargetException {
        String taskName = "dowload theme";
        SubMonitor subMonitor = SubMonitor.convert(monitor, taskName, 1);
        try {
            String ftp = installConfiguration.getThemeFtp();
            String remotePath = installConfiguration.getThemeRemotePath();
            String file = installConfiguration.getThemeFile();
            String username = installConfiguration.getThemeUsername();
            String password = installConfiguration.getThemePassword();
            File themeDir = new File(downloadDirectory, "theme");
            themeDir.mkdir();
            downloadComponent(ftp, remotePath, themeDir, file, username, password, taskName, subMonitor.newChild(1, SubMonitor.SUPPRESS_NONE));
        } finally {
            if (subMonitor != null) {
                subMonitor.done();
            }
        }
    }

    private void downloadLanguage(IProgressMonitor monitor, InstallConfiguration installConfiguration) {
    }

    private void downloadInstallationProfile(IProgressMonitor monitor, File downloadDirectory, InstallConfiguration installConfiguration) throws InvocationTargetException {
        String taskName = "dowload installation profile";
        SubMonitor subMonitor = SubMonitor.convert(monitor, taskName, 1);
        try {
            String ftp = installConfiguration.getProfileFtp();
            String remotePath = installConfiguration.getProfileRemotePath();
            String file = installConfiguration.getProfileFile();
            String username = installConfiguration.getProfileUsername();
            String password = installConfiguration.getProfilePassword();
            File profileDir = new File(downloadDirectory, "profile");
            profileDir.mkdir();
            downloadComponent(ftp, remotePath, profileDir, file, username, password, taskName, subMonitor.newChild(1, SubMonitor.SUPPRESS_NONE));
        } finally {
            if (subMonitor != null) {
                subMonitor.done();
            }
        }
    }

    private void downloadComponent(String hostname, String remotePath, File localDirectory, String fileName, String taskname, IProgressMonitor monitor) throws InvocationTargetException {
        downloadComponent(hostname, remotePath, localDirectory, fileName, "anonymous", "", taskname, monitor);
    }

    private void downloadComponent(String hostname, String remotePath, File localDirectory, String fileName, String username, String password, String taskname, IProgressMonitor monitor) throws InvocationTargetException {
        monitor.beginTask(taskname, 100);
        final File localFile = new File(localDirectory, fileName);
        FtpSiteConnector connector = new FtpSiteConnector();
        connector.setUrl(URI.create(hostname));
        connector.setUsername(username);
        connector.setPassword(password);
        FtpClient client = new FtpClient(connector);
        try {
            monitor.subTask(fileName);
            try {
                client.download(remotePath, fileName, localFile, false, monitor);
            } catch (FtpException e) {
                throw new InvocationTargetException(e);
            }
        } finally {
            monitor.done();
        }
    }

    private void untar(IProgressMonitor monitor, File tempDir, InstallConfiguration installConfiguration) throws InvocationTargetException {
        monitor.beginTask("decompress downloaded files", 5);
        monitor.subTask("decompress core");
        untar(tempDir, installConfiguration, "core");
        monitor.worked(1);
        monitor.subTask("decompress modules");
        untar(tempDir, installConfiguration, "modules");
        monitor.worked(1);
        monitor.subTask("decompress profile");
        untar(tempDir, installConfiguration, "profile");
        monitor.worked(1);
        monitor.subTask("decompress custom modules");
        untar(tempDir, installConfiguration, "custom");
        monitor.worked(1);
        monitor.subTask("decompress theme");
        untar(tempDir, installConfiguration, "theme");
        monitor.worked(1);
        monitor.done();
    }

    private void untar(File tempDir, InstallConfiguration installConfiguration, String type) throws InvocationTargetException {
        File downloadDir = new File(FileSystemUtils.getDir(RegiloInstallerPlugin.DOWNLOAD_DIR), type);
        tempDir = new File(tempDir, type);
        tempDir.mkdir();
        try {
            String[] files = downloadDir.list(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    Pattern pattern = Pattern.compile("(?i)^.+\\.gz$");
                    Matcher matcher = pattern.matcher(name);
                    if (matcher.matches()) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            for (int i = 0; i < files.length; i++) {
                File localFile = new File(downloadDir, files[i]);
                TarFile tar = new TarFile(localFile);
                Enumeration entries = tar.entries();
                while (entries.hasMoreElements()) {
                    TarEntry entry = (TarEntry) entries.nextElement();
                    if (entry.getFileType() == TarEntry.DIRECTORY) {
                        File currentDirectory = new File(tempDir, entry.getName());
                        currentDirectory.mkdir();
                    } else if (entry.getFileType() == TarEntry.FILE) {
                        File currentFile = new File(tempDir, entry.getName());
                        currentFile.createNewFile();
                        InputStream in = tar.getInputStream(entry);
                        OutputStream out = new FileOutputStream(currentFile);
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        in.close();
                        out.close();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new InvocationTargetException(e);
        } catch (TarException e) {
            throw new InvocationTargetException(e);
        } catch (IOException e) {
            throw new InvocationTargetException(e);
        }
    }

    private File createPackage(IProgressMonitor monitor, File tempDirectory, InstallConfiguration installConfiguration) throws InvocationTargetException {
        monitor.beginTask("create package", 7);
        File randomDirectory = new File(FileSystemUtils.getDir(RegiloInstallerPlugin.INSTALL_DIR), UUID.randomUUID().toString());
        randomDirectory.mkdir();
        monitor.subTask("creating directories");
        File core = new File(tempDirectory, "core");
        String[] coreFiles = core.list();
        File modules = new File(tempDirectory, "modules");
        String[] modulesFiles = modules.list();
        File profile = new File(tempDirectory, "profile");
        File custom = new File(tempDirectory, "custom");
        String[] customFiles = custom.list();
        File theme = new File(tempDirectory, "theme");
        String[] themeFiles = theme.list();
        File drupalDir = new File(randomDirectory, "drupal");
        File drupalSitesDir = new File(drupalDir, "sites");
        File drupalProfileDir = new File(drupalDir, "profiles");
        File drupalAllDir = new File(drupalSitesDir, "all");
        drupalAllDir.mkdirs();
        File drupalDefaultDir = new File(drupalSitesDir, "default");
        drupalDefaultDir.mkdirs();
        File drupalModulesDir = new File(drupalAllDir, "modules");
        drupalModulesDir.mkdirs();
        File drupalThemesDir = new File(drupalAllDir, "themes");
        drupalThemesDir.mkdirs();
        File drupalFilesDir = new File(drupalDefaultDir, "files");
        drupalFilesDir.mkdirs();
        monitor.worked(1);
        try {
            monitor.subTask("configuring core");
            File coreFile = new File(core, coreFiles[0]);
            FileUtils.copyDirectory(coreFile, drupalDir);
            monitor.worked(1);
            monitor.subTask("configuring modules");
            for (int i = 0; i < modulesFiles.length; i++) {
                File moduleFile = new File(modules, modulesFiles[i]);
                File moduleDir = new File(drupalModulesDir, modulesFiles[i]);
                if (moduleFile.isDirectory()) {
                    FileUtils.copyDirectory(moduleFile, moduleDir);
                }
            }
            monitor.worked(1);
            monitor.subTask("configuring custom module");
            for (int i = 0; i < customFiles.length; i++) {
                File customFile = new File(custom, customFiles[i]);
                File moduleDir = new File(drupalModulesDir, customFiles[i]);
                if (customFile.isDirectory()) {
                    FileUtils.copyDirectory(customFile, moduleDir);
                }
            }
            monitor.worked(1);
            monitor.subTask("configuring theme module");
            File themeFile = new File(theme, themeFiles[0]);
            String themeName = installConfiguration.getThemeName();
            File themeDir = new File(drupalThemesDir, themeName);
            FileUtils.copyDirectory(themeFile, themeDir);
            monitor.worked(1);
            monitor.subTask("configuring settings file");
            File defaultSettings = new File(drupalDefaultDir, "default.settings.php");
            File settings = new File(drupalDefaultDir, "settings.php");
            FileUtils.copyFile(defaultSettings, settings);
            monitor.worked(1);
            monitor.subTask("configuring installation file");
            File profileDir = new File(profile, installConfiguration.getProfileName());
            File unattendedFile = new File(profileDir, "unattended_install.php");
            FileUtils.copyFileToDirectory(unattendedFile, drupalDir);
            profileDir = new File(profileDir, installConfiguration.getProfileName());
            drupalProfileDir = new File(drupalProfileDir, installConfiguration.getProfileName());
            FileUtils.copyDirectory(profileDir, drupalProfileDir);
            createConfigurationFile(drupalDir, installConfiguration);
            monitor.worked(1);
        } catch (IOException e) {
            throw new InvocationTargetException(e);
        } finally {
            monitor.done();
        }
        return drupalDir;
    }

    private void upload(IProgressMonitor monitor, File tempDirectory, InstallConfiguration installConfiguration) throws InvocationTargetException {
        SubMonitor subMonitor = SubMonitor.convert(monitor, "upload", 1000);
        try {
            FtpClient client = new FtpClient(ftpSiteConnector);
            try {
                client.upload(tempDirectory, null, "", false, false, monitor);
            } catch (FtpException e) {
                throw new InvocationTargetException(e);
            }
        } finally {
            if (subMonitor != null) {
                subMonitor.done();
            }
        }
    }

    private void install(IProgressMonitor monitor, InstallConfiguration installConfiguration) throws InvocationTargetException {
        FtpClient client = new FtpClient(ftpSiteConnector);
        client.chmod("sites/default/files", "777");
        client.chmod("sites/default/settings.php", "777");
        HeadlessBrowser browser = new HeadlessBrowser();
        WebSiteConnector webSiteConnector = (WebSiteConnector) installConfiguration.getSite().getConnector(WebSiteConnector.TYPE);
        URI uri = webSiteConnector.getUrl();
        String install = uri.toString() + "/unattended_install.php?profile=" + installConfiguration.getProfileName() + "&locale=en";
        try {
            browser.navigateTo(new URI(install));
        } catch (URISyntaxException e) {
            throw new InvocationTargetException(e);
        }
        client.chmod("sites/default/settings.php", "555");
    }

    public void setInstallConfiguration(InstallConfiguration installConfiguration) {
        this.installConfiguration = installConfiguration;
    }

    private File createConfigurationFile(File drupalDir, InstallConfiguration installConfiguration) throws InvocationTargetException {
        MysqlSiteConnector mysqlSiteConnector = (MysqlSiteConnector) installConfiguration.getSite().getConnector(MysqlSiteConnector.TYPE);
        String dbUrl = mysqlSiteConnector.getUrl().toString();
        StringBuilder builder = new StringBuilder();
        builder.append("<?php\n");
        builder.append("define('DB_URL', '" + dbUrl + "');\n");
        builder.append("define('SITE_NAME', '" + installConfiguration.getSiteName() + "');\n");
        builder.append("define('SITE_MAIL', '" + installConfiguration.getSiteMail() + "');\n");
        builder.append("define('ADMIN_NAME', '" + installConfiguration.getAdminName() + "');\n");
        builder.append("define('ADMIN_MAIL', '" + installConfiguration.getAdminMail() + "');\n");
        builder.append("define('ADMIN_PASS', '" + installConfiguration.getAdminPass() + "');\n");
        builder.append("define('DEFAULT_THEME', '" + installConfiguration.getThemeName() + "');\n");
        File configuration = new File(drupalDir, "configuration.inc");
        try {
            FileUtils.writeStringToFile(configuration, builder.toString());
        } catch (IOException e) {
            throw new InvocationTargetException(e);
        }
        return configuration;
    }
}
