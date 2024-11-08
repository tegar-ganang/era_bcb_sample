package org.ftpscan;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;

/**
 * Class for scanning FTP directory.
 *
 * @author Nikolai Holub (nikolai.holub at gmail.com)
 */
public class FTPScanner {

    private Logger log = Logger.getLogger(getClass().getPackage().getName());

    private String host;

    private String dir;

    private FTPClient client;

    private XMLDocument xmlDocument;

    /**
     * Constructs FTPScanner.
     *
     * @param ftpHost FTP host
     * @param scanDir directory to scan
     */
    public FTPScanner(String ftpHost, String scanDir) {
        host = ftpHost;
        if (scanDir.endsWith("/")) {
            dir = scanDir;
        } else {
            dir = scanDir + "/";
        }
    }

    /**
     * Scans FTP directory.
     *
     * @throws Throwable if any exception occurs
     */
    public void scan() throws Throwable {
        client = new FTPClient();
        log.info("connecting to " + host + "...");
        client.connect(host);
        log.info(client.getReplyString());
        log.info("logging in...");
        client.login("anonymous", "");
        log.info(client.getReplyString());
        Date date = Calendar.getInstance().getTime();
        xmlDocument = new XMLDocument(host, dir, date);
        scanDirectory(dir);
    }

    /**
     * Returns results of scanning.
     *
     * @return results of scanning
     */
    public XMLDocument getXmlDocument() {
        return xmlDocument;
    }

    /**
     * Recursively scans specified directory.
     *
     * @param directory directory to scan
     * @throws IOException if IO exception occurs
     */
    private void scanDirectory(String directory) throws IOException {
        log.info("LIST " + directory);
        FTPFile[] files = client.listFiles(directory);
        if (files.length == 0) {
            log.warn("no files in " + directory);
        }
        for (FTPFile file : files) {
            String name = file.getName();
            if (!".".equals(name) && !"..".equals(name)) {
                if (file.isDirectory()) {
                    scanDirectory(directory + name + "/");
                } else {
                    xmlDocument.addFile(directory, file);
                }
            }
        }
    }
}
