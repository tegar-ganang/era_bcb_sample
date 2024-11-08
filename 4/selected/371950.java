package com.lightminds.map.tileserver.scheduler;

import com.lightminds.map.tileserver.admin.SessionHandler;
import com.lightminds.map.tileserver.admin.Statistics;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class LogSenderJob implements Job {

    private static String[] usernames, emails;

    public void execute(JobExecutionContext context) throws JobExecutionException {
        SessionHandler.getInstance().flushSessionsAndStats();
        String logfile_dir = ResourceBundle.getBundle("tileserver_conf").getString("statistics_log_dir");
        File[] logFiles;
        File tempDirectory;
        synchronized (Statistics.mutex) {
            File logDirectory = new File(logfile_dir);
            logFiles = logDirectory.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    if (new File(dir, name).isFile() && name.startsWith("XeptoTileServer_Statistics_") && name.substring(name.length() - 4).equalsIgnoreCase(".log")) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            if (logFiles.length == 0) {
                return;
            }
            tempDirectory = new File(logDirectory, "tmp");
            tempDirectory.mkdir();
            for (int i = 0; i < logFiles.length; i++) {
                File temp = new File(tempDirectory, logFiles[i].getName());
                logFiles[i].renameTo(temp);
                logFiles[i] = temp;
            }
        }
        extractUsernamesAndEmails();
        zipAndSendFiles(logFiles);
        tempDirectory.delete();
    }

    private void extractUsernamesAndEmails() {
        XMLConfiguration config = new XMLConfiguration();
        config.setReloadingStrategy(new FileChangedReloadingStrategy());
        config.setFileName("/customers.xml");
        try {
            config.load();
        } catch (ConfigurationException ex) {
            System.out.println("Cannot extract usernames.");
        }
        Collection props = (Collection) config.getProperty("customer.username");
        Iterator iter = props.iterator();
        usernames = new String[props.size()];
        emails = new String[props.size()];
        for (int i = 0; i < usernames.length; i++) {
            usernames[i] = (String) iter.next();
            emails[i] = config.getString("customer(" + i + ").email");
            System.out.println(emails[i]);
        }
    }

    private void zipAndSendFiles(File[] logFiles) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        EmailSender emailSender = new EmailSender();
        try {
            File globalZipFile = new File("XeptoTileServer_Statistics_" + sdf.format(new Date()) + ".zip");
            ZipOutputStream globalZos = new ZipOutputStream(new CheckedOutputStream(new FileOutputStream(globalZipFile), new Adler32()));
            BufferedOutputStream globalOut = new BufferedOutputStream(globalZos);
            globalZos.setComment("Statistics for all users from the XeptoTileServer.");
            File[] zipFiles = new File[usernames.length];
            ZipOutputStream[] zipOutputStreams = new ZipOutputStream[usernames.length];
            BufferedOutputStream[] bufferedZipStreams = new BufferedOutputStream[usernames.length];
            for (File logFile : logFiles) {
                int i;
                for (i = 0; i < usernames.length; i++) {
                    if (logFile.getName().substring(27, 28 + usernames[i].length()).equalsIgnoreCase(usernames[i] + "_")) break;
                }
                BufferedReader in = new BufferedReader(new FileReader(logFile.getPath()));
                ZipEntry entry = new ZipEntry(logFile.getName());
                globalZos.putNextEntry(entry);
                if (i < usernames.length && emails[i] != null) {
                    if (zipFiles[i] == null) {
                        zipFiles[i] = new File("XeptoTileServer_Statistics_" + usernames[i] + "_" + sdf.format(new Date()) + ".zip");
                        zipOutputStreams[i] = new ZipOutputStream(new CheckedOutputStream(new FileOutputStream(zipFiles[i]), new Adler32()));
                        bufferedZipStreams[i] = new BufferedOutputStream(zipOutputStreams[i]);
                        zipOutputStreams[i].setComment("Statistics for username " + usernames[i] + " from the XeptoTileServer.");
                    }
                    zipOutputStreams[i].putNextEntry(entry);
                    int c;
                    while ((c = in.read()) != -1) {
                        globalOut.write(c);
                        bufferedZipStreams[i].write(c);
                    }
                } else {
                    int c;
                    while ((c = in.read()) != -1) globalOut.write(c);
                }
                in.close();
                logFile.delete();
            }
            globalOut.close();
            emailSender.send(java.util.ResourceBundle.getBundle("logsender_conf").getString("e-mail"), globalZipFile);
            globalZipFile.delete();
            for (int i = 0; i < zipFiles.length; i++) {
                if (zipFiles[i] != null) {
                    bufferedZipStreams[i].close();
                    emailSender.send(emails[i], zipFiles[i]);
                    zipFiles[i].delete();
                }
            }
        } catch (IOException ex) {
            System.out.println("Cannot create zip file.");
            ex.printStackTrace();
        }
    }
}
