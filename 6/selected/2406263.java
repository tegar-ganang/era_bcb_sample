package edu.unibi.agbi.biodwh.download.ftp;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import edu.unibi.agbi.biodwh.download.Download;
import edu.unibi.agbi.biodwh.project.logic.queue.DownloadQueue;
import edu.unibi.agbi.biodwh.project.object.ParserObject;

/**
 * @author Benjamin Kormeier
 * @version 1.00 30.03.2007
 */
public class CommonFTPDownload extends FTPDownloadAdapter {

    private final String WILDCARD_WORD = new String("*");

    private final String WILDCARD_DIGIT = new String("?");

    private final String WILDCARD_WORD_PATTERN = new String("\\\\w{1,}");

    private final String WILDCARD_DIGIT_PATTERN = new String("\\\\d{1,}");

    private String username = new String("anonymous");

    private String password = new String("");

    private String projectName = null;

    private ParserObject parser = null;

    private URL source = null;

    private File target = null;

    private String downloadFiles[] = null;

    public CommonFTPDownload() {
    }

    private void downloadDirectory() throws SocketException, IOException {
        FTPClient client = new FTPClient();
        client.connect(source.getHost());
        client.login(username, password);
        FTPFile[] files = client.listFiles(source.getPath());
        for (FTPFile file : files) {
            if (!file.isDirectory()) {
                long file_size = file.getSize() / 1024;
                Calendar cal = file.getTimestamp();
                URL source_file = new File(source + file.getName()).toURI().toURL();
                DownloadQueue.add(new Download(projectName, parser.getParserID(), source_file, file_size, cal, target + file.getName()));
            }
        }
    }

    private void downloadFiles() throws SocketException, IOException {
        HashSet<String> files_set = new HashSet<String>();
        boolean hasWildcarts = false;
        FTPClient client = new FTPClient();
        for (String file : downloadFiles) {
            files_set.add(file);
            if (file.contains(WILDCARD_WORD) || file.contains(WILDCARD_DIGIT)) hasWildcarts = true;
        }
        client.connect(source.getHost());
        client.login(username, password);
        FTPFile[] files = client.listFiles(source.getPath());
        if (!hasWildcarts) {
            for (FTPFile file : files) {
                String filename = file.getName();
                if (files_set.contains(filename)) {
                    long file_size = file.getSize() / 1024;
                    Calendar cal = file.getTimestamp();
                    URL source_file = new File(source + file.getName()).toURI().toURL();
                    DownloadQueue.add(new Download(projectName, parser.getParserID(), source_file, file_size, cal, target + file.getName()));
                }
            }
        } else {
            for (FTPFile file : files) {
                String filename = file.getName();
                boolean match = false;
                for (String db_filename : downloadFiles) {
                    db_filename = db_filename.replaceAll("\\" + WILDCARD_WORD, WILDCARD_WORD_PATTERN);
                    db_filename = db_filename.replaceAll("\\" + WILDCARD_DIGIT, WILDCARD_DIGIT_PATTERN);
                    Pattern p = Pattern.compile(db_filename);
                    Matcher m = p.matcher(filename);
                    match = m.matches();
                }
                if (match) {
                    long file_size = file.getSize() / 1024;
                    Calendar cal = file.getTimestamp();
                    URL source_file = new File(source + file.getName()).toURI().toURL();
                    DownloadQueue.add(new Download(projectName, parser.getParserID(), source_file, file_size, cal, target + file.getName()));
                }
            }
        }
    }

    @Override
    public void startDownload(String projectName, ParserObject parser) throws Exception {
        this.projectName = projectName;
        this.parser = parser;
        source = parser.getMonitorObject().getSourceURL();
        target = parser.getSourceDirectory();
        downloadFiles = parser.getMonitorObject().getDowloadFiles();
        if (downloadFiles == null) downloadDirectory(); else downloadFiles();
    }
}
