package xplanetconfigurator.downloader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import xplanetconfigurator.gui.FeedbackReceiver;
import xplanetconfigurator.gui.LoggingBoard;
import xplanetconfigurator.util.FileUtil;
import xplanetconfigurator.util.XPlanetRessourceFinder;

/**
 *
 * @author tom
 */
public class DownloadTimer extends Thread {

    private static int instanceNumber;

    private Logger logger;

    String configFile;

    private String configFileContent = "";

    private Map downloadFiles;

    private String currentDownloadTypeId;

    private FeedbackReceiver feedbackReceiver;

    private boolean run = true;

    private boolean runOnceAndLeave;

    private int seconds;

    private DownloaderProxy proxy;

    private List currentDownloadTypeIDs;

    public DownloadTimer(String configFile) {
        instanceNumber++;
        this.configFile = configFile;
        this.logger = Logger.getLogger(this.getClass().getName());
        this.logger.finest("Creating Downloader with config file: " + configFile);
        this.proxy = new DownloaderProxy();
        this.downloadFiles = new HashMap();
        this.readConfigurationFile(configFile);
    }

    /**
     * Read the configuration from the config file
     *
     * @param file the absolute path
     */
    private void readConfigurationFile(String file) {
        File f = new File(file);
        if (!f.exists()) {
            this.logger.fine("Config file does not exist: " + file);
            return;
        }
        if (!f.canRead()) {
            this.logger.warning("Not allowed to read configuration file: " + file);
            return;
        }
        FileUtil util = new FileUtil();
        try {
            this.configFileContent = util.getFileAsString(f);
        } catch (Exception ex) {
            this.logger.log(Level.SEVERE, null, ex);
            return;
        }
        String regExpr = "(?m)^(\\w+)(\\.)(\\w+)(\\.)";
        Pattern p = Pattern.compile(regExpr);
        Matcher m = p.matcher(configFileContent);
        List tmpTypeId = new ArrayList();
        while (m.find()) {
            String keyValue = m.group();
            this.logger.finest("Found URL to download in key-value-pair: " + keyValue);
            String type = m.group(1);
            String id = m.group(3);
            String typeId = type + "." + id;
            if (tmpTypeId.contains(typeId)) {
                continue;
            } else {
                this.logger.finer("Added type-id-pair (DownloadFile) to temp list: " + typeId);
                tmpTypeId.add(typeId);
            }
        }
        this.downloadFiles = new HashMap();
        Iterator it = tmpTypeId.iterator();
        while (it.hasNext()) {
            DownloadFile downloadFile = new DownloadFile();
            String typeId = (String) it.next();
            String[] splittees = typeId.split("\\.");
            String type = splittees[0];
            String id = splittees[1];
            downloadFile.setType(type);
            downloadFile.setId(id);
            this.downloadFiles.put(typeId, downloadFile);
            regExpr = "(?i)(" + type + "\\." + id + "\\.localstatus=)(.+)";
            p = Pattern.compile(regExpr);
            m = p.matcher(configFileContent);
            if (m.find()) {
                String value = m.group(2);
                this.logger.finer("Found status for " + typeId + ": " + value);
                downloadFile.setLocalStatus(value);
            } else {
                this.logger.finer("Found no status for " + typeId);
            }
            regExpr = "(?i)(" + type + "\\." + id + "\\.downloadstatus=)(.+)";
            p = Pattern.compile(regExpr);
            m = p.matcher(configFileContent);
            if (m.find()) {
                String value = m.group(2);
                this.logger.finer("Found status for " + typeId + ": " + value);
                downloadFile.setDownloadStatus(value);
            } else {
                this.logger.finer("Found no status for " + typeId);
            }
            regExpr = "(?i)(" + type + "\\." + id + "\\.wait=)(.+)";
            p = Pattern.compile(regExpr);
            m = p.matcher(configFileContent);
            if (m.find()) {
                String value = m.group(2);
                this.logger.finer("Found wait time for " + typeId + ": " + value);
                try {
                    downloadFile.setWait(Integer.parseInt(value));
                } catch (Exception e) {
                    this.logger.warning("Failed to parse wait (download intervall) from: " + value + ". The Downloader will use a default value.");
                    LoggingBoard.logDownloaderMessage("Failed to parse download intervall from: " + value + ". Taking a default value.");
                }
            } else {
                this.logger.finer("Found wait time for " + typeId);
            }
            regExpr = "(?i)(" + type + "\\." + id + "\\.lastmodified=)(.+)";
            p = Pattern.compile(regExpr);
            m = p.matcher(configFileContent);
            if (m.find()) {
                String value = m.group(2);
                if (value == null) {
                    value = "";
                }
                this.logger.finer("Found lastmodified for " + typeId + ": " + value);
                try {
                    downloadFile.setLastModified(Long.parseLong(value));
                } catch (Exception e) {
                    this.logger.warning("Failed to parse wait (last modified) from: " + value + ". Setting value to '0'...");
                    downloadFile.setLastModified(0);
                }
            } else {
                downloadFile.setLastModified(0);
                this.logger.finer("Found no lastmodified for " + typeId);
            }
            regExpr = "(?i)(" + type + "\\." + id + "\\.dir=)(.+)";
            p = Pattern.compile(regExpr);
            m = p.matcher(configFileContent);
            if (m.find()) {
                String value = m.group(2);
                this.logger.finer("Found directory for " + typeId + ": " + value);
                downloadFile.setLocalDirectoryRelativ(value);
            } else {
                this.logger.fine("Found no directory for marker " + typeId + ". Removing from Downloader...");
                this.logger.finer("Removing DownloaderFile from list: " + typeId + "...");
                this.downloadFiles.remove(typeId);
                continue;
            }
            regExpr = "(?i)(" + type + "\\." + id + "\\.url\\.selected=)(.+)";
            p = Pattern.compile(regExpr);
            m = p.matcher(configFileContent);
            if (m.find()) {
                String value = m.group(2);
                this.logger.finer("Found selected URL for " + typeId + ": " + value);
                downloadFile.setUrl(value);
            } else {
                this.logger.finer("Found no selected URL for " + typeId + ". The downloader will not download anything for this type.id: " + typeId);
            }
            regExpr = "(?i)(" + type + "\\." + id + "\\.url\\.listitem=)(.+)";
            p = Pattern.compile(regExpr);
            m = p.matcher(configFileContent);
            while (m.find()) {
                String value = m.group(2);
                this.logger.finer("Found url for " + typeId + ": " + value);
                downloadFile.addURL(value);
            }
        }
        regExpr = "(?i)(proxy.set=)(.+)";
        p = Pattern.compile(regExpr);
        m = p.matcher(configFileContent);
        if (m.find()) {
            String value = m.group(2);
            this.logger.finer("Found proxy.set: " + value);
            this.proxy.setProxySet(value);
        }
        regExpr = "(?i)(http.proxyHost=)(.+)";
        p = Pattern.compile(regExpr);
        m = p.matcher(configFileContent);
        if (m.find()) {
            String value = m.group(2);
            this.logger.finer("Found http.proxyHost: " + value);
            this.proxy.setProxyHost(value);
        }
        regExpr = "(?i)(http.proxyPort=)(.+)";
        p = Pattern.compile(regExpr);
        m = p.matcher(configFileContent);
        if (m.find()) {
            String value = m.group(2);
            this.logger.finer("Found http.proxyPort: " + value);
            this.proxy.setProxyPort(value);
        }
        regExpr = "(?i)(http.proxyUser=)(.+)";
        p = Pattern.compile(regExpr);
        m = p.matcher(configFileContent);
        if (m.find()) {
            String value = m.group(2);
            this.logger.finer("Found http.proxyUser: " + value);
            this.proxy.setProxyUser(value);
        }
        regExpr = "(?i)(http.proxyPass=)(.+)";
        p = Pattern.compile(regExpr);
        m = p.matcher(configFileContent);
        if (m.find()) {
            String value = m.group(2);
            this.logger.finer("Found http.proxyPass for: " + value);
            this.proxy.setProxyPass(value);
        }
        this.checkIfDownloadFilesExists();
        this.updateConfigurationContent();
    }

    /**
     * Returns the file content of the configution file or at runtime the
     * String hold in memory.
     * @return
     */
    public String getConfiguration() {
        return this.configFileContent;
    }

    /**
     * Used by the GUI to configure the downloader.
     *
     * @param config
     */
    public void setConfiguration(String config) {
        this.configFileContent = config;
        this.saveConfiguration();
    }

    /**
     * Writes the configuration to file
     */
    public void saveConfiguration() {
        FileUtil util = new FileUtil();
        try {
            util.printFile(this.configFile, this.configFileContent);
        } catch (Exception ex) {
            this.logger.log(Level.SEVERE, null, ex);
        }
    }

    public void cleanUp() {
        this.saveConfiguration();
        this.run = false;
    }

    private void checkIfDownloadFilesExists() {
        Iterator it = this.downloadFiles.keySet().iterator();
        while (it.hasNext()) {
            String typeId = (String) it.next();
            checkIfDownloadFileExist(typeId);
        }
    }

    private boolean checkIfDownloadFileExist(String typeId) {
        boolean exists = false;
        String[] splittees = typeId.split("\\.");
        String type = splittees[0];
        String id = splittees[1];
        DownloadFile downloadFile = (DownloadFile) this.downloadFiles.get(typeId);
        String localDir = downloadFile.getLocalDirectoryRelativ();
        String regExpr = "(?i)(" + type + "\\." + id + "\\.url\\.selected=)(.+)";
        Pattern p = Pattern.compile(regExpr);
        Matcher m = p.matcher(this.configFileContent);
        if (m.find()) {
            String url = m.group(2);
            splittees = url.split("/");
            int length = splittees.length;
            String filename = splittees[length - 1];
            XPlanetRessourceFinder rf = new XPlanetRessourceFinder();
            String downloaderRootDir = rf.getRootDirectoryForDownloads();
            String path = downloaderRootDir + File.separator + localDir + File.separator + filename;
            File localFile = new File(path);
            if (localFile.exists()) {
                downloadFile.setLocalStatus("<html><font color='green'>Found</font></html>");
                return true;
            } else {
                this.logger.finer("Missing file on disk: " + path);
                downloadFile.setLocalStatus("<html><font color='red'>Missing</font></html>");
                downloadFile.setLastModified(0);
                return false;
            }
        }
        return exists;
    }

    /**
     * Reads the properties of the all DownloadFiles and writes them to file
     */
    private void updateConfigurationContent() {
        this.logger.finer("Start to read out the downloader to write a new configuration file for the uploader...");
        Iterator it = this.downloadFiles.keySet().iterator();
        StringBuffer buf = new StringBuffer();
        while (it.hasNext()) {
            String typeId = (String) it.next();
            DownloadFile downloadFile = (DownloadFile) this.downloadFiles.get(typeId);
            typeId = typeId.toLowerCase();
            long l = downloadFile.getLastModified();
            if (l > 0) {
                String s = downloadFile.getLastUpdate();
                buf.append("\n").append(typeId + ".lastupdate=" + s);
                this.logger.finer("Added time for last update (last download) '" + s + "' found for: " + typeId);
            } else {
                buf.append("\n").append(typeId + ".lastupdate=");
                this.logger.finer("Empty lastupdate because last modified is '0' found for: " + typeId);
            }
            String s = Long.toString(l);
            buf.append("\n").append(typeId + ".lastmodified=" + s);
            this.logger.finer("Added lastmodified '" + s + "' for: " + typeId);
            int i = downloadFile.getWait();
            s = Integer.toString(i);
            buf.append("\n").append(typeId + ".wait=" + s);
            this.logger.finer("Added download intervall (wait) '" + s + "' for: " + typeId);
            boolean b = downloadFile.getUpToDate();
            String status = downloadFile.getDownloadStatus();
            if (b) {
                if (status == null) {
                    buf.append("\n").append(typeId + ".downloadstatus=" + "<html><font color='red'>Not set yet</font></html>");
                    this.logger.finer("Error in setting up to date info '" + b + "' for: " + typeId + ". File is up to date but the status was never set.");
                } else {
                    buf.append("\n").append(typeId + ".downloadstatus=" + status);
                    this.logger.finer("File up to date. Take existing status because it was not empty for: " + typeId);
                }
            } else {
                if (status == null) {
                    buf.append("\n").append(typeId + ".downloadstatus=" + "<html><font color='red'>Has never been updated</font></html>");
                    this.logger.finer("Added up to date info '" + b + "' for: " + typeId);
                } else {
                    buf.append("\n").append(typeId + ".downloadstatus=" + status);
                    this.logger.finer("File not up to date. Take existing status because it was not empty for: " + typeId);
                }
            }
            s = downloadFile.getLocalStatus();
            if (s != null) {
                buf.append("\n").append(typeId + ".localstatus=" + s);
                this.logger.finer("Added status (missing/found) '" + s + "' for: " + typeId);
            } else {
                this.logger.finer("No status (missing/found) found for: " + typeId);
            }
            s = downloadFile.getLocalDirectoryRelativ();
            if (s != null) {
                buf.append("\n").append(typeId + ".dir=" + s);
                this.logger.finer("Added local directory (relativ) '" + s + "' for: " + typeId);
            } else {
                this.logger.finer("No local directory (relativ) found for: " + typeId);
            }
            s = downloadFile.getUrl();
            if (s != null) {
                buf.append("\n").append(typeId + ".url.selected=" + s);
                this.logger.finer("Added selected URL '" + s + "' for: " + typeId);
            } else {
                this.logger.finer("No selected URL found for: " + typeId);
            }
            List urls = downloadFile.getUrls();
            if (urls != null) {
                Iterator itURLs = urls.iterator();
                while (itURLs.hasNext()) {
                    s = (String) itURLs.next();
                    buf.append("\n").append(typeId + ".url.listitem=" + s);
                    this.logger.finer("Added URL '" + s + "' for: " + typeId);
                }
            } else {
                this.logger.finer("No URLs found for: " + typeId);
            }
        }
        String s = this.proxy.getProxySet();
        if (s != null) {
            buf.append("\n").append("proxy.set=" + s);
            this.logger.finer("Added proxy.set " + s);
        } else {
            this.logger.finer("No http.proxyPass found");
        }
        s = this.proxy.getProxyHost();
        if (s != null) {
            buf.append("\n").append("http.proxyHost=" + s);
            this.logger.finer("Added proxy.proxyHost " + s);
        } else {
            this.logger.finer("No http.proxyPass found");
        }
        s = this.proxy.getProxyPort();
        if (s != null) {
            buf.append("\n").append("http.proxyPort=" + s);
            this.logger.finer("Added proxy.proxyPort " + s);
        } else {
            this.logger.finer("No http.proxyPass found");
        }
        s = this.proxy.getProxyUser();
        if (s != null) {
            buf.append("\n").append("http.proxyUser=" + s);
            this.logger.finer("Added proxy.proxyUser " + s);
        } else {
            this.logger.finer("No http.proxyPass found");
        }
        s = this.proxy.getProxyPass();
        if (s != null) {
            buf.append("\n").append("http.proxyPass=" + s);
            this.logger.finer("Added proxy.proxyPass *****");
        } else {
            this.logger.finer("No http.proxyPass found");
        }
        this.logger.finer("Finished to read out the downloader to write a new configuration file for the uploade");
        this.configFileContent = buf.toString();
        this.configFileContent = this.configFileContent.replaceAll("(?m)^\\s*$^\\s*", "");
        this.saveConfiguration();
    }

    /**
     * Send a feedback the GUI
     * @param o
     */
    private void sendFeedback() {
        if (this.run) {
            this.checkIfDownloadFilesExists();
            this.updateConfigurationContent();
            if (this.getFeedbackReceiver() != null) {
                this.getFeedbackReceiver().receiveDownloaderFeedback();
            }
        }
    }

    public void stopRuning() {
        this.run = false;
        this.logger.finer("Download Timer was stopped. (Instance '" + instanceNumber + "')");
        this.setFeedbackReceiver(null);
    }

    @Override
    public void run() {
        this.logger.info("Start running...  (Instance '" + instanceNumber + "')");
        if (this.isRunOnceAndLeave()) {
            if (this.currentDownloadTypeId != null) {
                DownloadFile downloadFile = (DownloadFile) this.downloadFiles.get(this.currentDownloadTypeId.toLowerCase());
                if (downloadFile != null) {
                    HttpDownloader loader = new HttpDownloader();
                    loader.downloadFile(downloadFile, true, this.proxy);
                    this.sendFeedback();
                }
            }
            if (this.currentDownloadTypeIDs != null) {
                Iterator it = this.currentDownloadTypeIDs.iterator();
                while (it.hasNext()) {
                    if (this.run) {
                        String typeId = (String) it.next();
                        DownloadFile downloadFile = (DownloadFile) this.downloadFiles.get(typeId.toLowerCase());
                        if (downloadFile != null) {
                            HttpDownloader loader = new HttpDownloader();
                            loader.downloadFile(downloadFile, true, this.proxy);
                            this.sendFeedback();
                        }
                    } else {
                        this.logger.fine("Have still files to download but was stopped from outside. (Instance '" + instanceNumber + "')");
                        break;
                    }
                }
            }
        } else {
            try {
                while (this.run) {
                    if (this.run) {
                        Iterator it = this.downloadFiles.keySet().iterator();
                        while (it.hasNext()) {
                            if (this.run) {
                                String typeId = (String) it.next();
                                DownloadFile downloadFile = (DownloadFile) this.downloadFiles.get(typeId);
                                if (!this.checkIfDownloadFileExist(typeId)) {
                                    this.logger.fine("URL needs update because it was never downloaded yet. id: " + typeId + ". Creating new HttpDownloader...");
                                    HttpDownloader loader = new HttpDownloader();
                                    loader.downloadFile(downloadFile, false, this.proxy);
                                } else {
                                    boolean isUpDoDate = downloadFile.getUpToDate();
                                    if (!isUpDoDate) {
                                        this.logger.finer("URL needs update for id: " + typeId + ". Creating new HttpDownloader...");
                                        HttpDownloader loader = new HttpDownloader();
                                        loader.downloadFile(downloadFile, false, this.proxy);
                                    } else {
                                        downloadFile.setDownloadStatus("<html><font color='green'>Needs no Update</font></html>");
                                        this.logger.finer("Still have to wait to download file for id: " + typeId + ". Set status to 'Needs no Update'.");
                                    }
                                }
                            }
                        }
                    }
                    this.sendFeedback();
                    this.logger.finest("Waiting " + this.getSeconds() + " seconds... (Instance '" + instanceNumber + "')");
                    sleep(getSeconds() * 1000);
                    this.logger.finer("Timer woke up after sleeping " + this.getSeconds() + " seconds... (Instance '" + instanceNumber + "')");
                }
            } catch (InterruptedException ex) {
                logger.log(Level.FINEST, null, ex);
            } finally {
                this.logger.finest("Exiting run()... (Instance '" + instanceNumber + "')");
            }
        }
    }

    /**
     * Used by the GUI only.
     *
     * @param currentDownloadTypeId the currentDownloadTypeId to set
     * Example: type.id=markers.volcanos
     */
    public void setCurrentDownloadTypeId(String currentDownloadId) {
        this.currentDownloadTypeId = currentDownloadId;
    }

    /**
     * Used by the GUI only
     *
     * @return the feedbackReceiver
     */
    public FeedbackReceiver getFeedbackReceiver() {
        return feedbackReceiver;
    }

    /**
     * Used by the GUI only
     *
     * @param feedbackReceiver the feedbackReceiver to set
     */
    public void setFeedbackReceiver(FeedbackReceiver feedbackReceiver) {
        this.feedbackReceiver = feedbackReceiver;
    }

    /**
     * Used by the GUI only
     *
     * @return the runOnceAndLeave
     */
    public boolean isRunOnceAndLeave() {
        return runOnceAndLeave;
    }

    /**
     * Used by the GUI only
     *
     * @param runOnceAndLeave the runOnceAndLeave to set
     */
    public void setRunOnceAndLeave(boolean runOnceAndLeave) {
        this.runOnceAndLeave = runOnceAndLeave;
    }

    /**
     * The sleep time
     *
     * @return the seconds
     */
    public int getSeconds() {
        return seconds;
    }

    /**
     * The sleep time
     * 
     * @param seconds the seconds to set
     */
    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    /**
     * @param currentDownloadTypeIDs the currentDownloadTypeIDs to set
     */
    public void setCurrentDownloadTypeIDs(List currentDownloadTypeIDs) {
        this.currentDownloadTypeIDs = currentDownloadTypeIDs;
    }
}
