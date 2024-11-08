package com.c2b2.tools.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Properties;
import org.apache.commons.net.ftp.*;

public class FTPBackup {

    public void getDataFiles(String server, String username, String password, String folder, String destinationFolder) {
        try {
            FTPClient ftp = new FTPClient();
            ftp.connect(server);
            ftp.login(username, password);
            System.out.println("Connected to " + server + ".");
            System.out.print(ftp.getReplyString());
            ftp.enterLocalActiveMode();
            ftp.changeWorkingDirectory(folder);
            System.out.println("Changed to " + folder);
            FTPFile[] files = ftp.listFiles();
            System.out.println("Number of files in dir: " + files.length);
            for (int i = 0; i < files.length; i++) {
                getFiles(ftp, files[i], destinationFolder);
            }
            ftp.logout();
            ftp.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void putDataFiles(String server, String username, String password, String folder, String destinationFolder) {
        try {
            FTPClient ftp = new FTPClient();
            ftp.connect(server);
            System.out.println("Connected");
            ftp.login(username, password);
            System.out.println("Logged in to " + server + ".");
            System.out.print(ftp.getReplyString());
            ftp.changeWorkingDirectory(destinationFolder);
            System.out.println("Changed to directory " + destinationFolder);
            File localRoot = new File(folder);
            File[] files = localRoot.listFiles();
            System.out.println("Number of files in dir: " + files.length);
            for (int i = 0; i < files.length; i++) {
                putFiles(ftp, files[i]);
            }
            ftp.logout();
            ftp.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getFiles(FTPClient ftp, FTPFile remote, String local) throws IOException {
        if (remote.isDirectory()) {
            ftp.changeWorkingDirectory(remote.getName());
            System.out.println("Changed to directory " + remote.getName());
            File file = new File(local + File.separator + remote.getName());
            file.mkdir();
            System.out.println("Created local directory " + file.getName());
            FTPFile[] files = ftp.listFiles();
            for (int i = 0; i < files.length; i++) {
                getFiles(ftp, files[i], file.getAbsolutePath());
            }
            ftp.changeToParentDirectory();
            System.out.println("Changed to parent directory");
        } else {
            System.out.print("\t" + remote.getName() + ":");
            File file = new File(local + File.separator + remote.getName());
            FileOutputStream fos = new FileOutputStream(file);
            ftp.retrieveFile(remote.getName(), fos);
            System.out.println(" Done");
            fos.close();
        }
    }

    private void putFiles(FTPClient ftp, File local) throws IOException {
        if (local.isDirectory()) {
            ftp.makeDirectory(local.getName());
            System.out.println("Created Directory" + local.getName());
            ftp.changeWorkingDirectory(local.getName());
            System.out.println("Changed to remote directory " + local.getName());
            File[] files = local.listFiles();
            for (int i = 0; i < files.length; i++) {
                putFiles(ftp, files[i]);
            }
            ftp.changeToParentDirectory();
            System.out.println("Changed to parent directory");
        } else {
            System.out.print("\t" + local.getName() + ":");
            FileInputStream fis = new FileInputStream(local);
            ftp.storeFile(local.getName(), fis);
            System.out.println(" Done");
            fis.close();
        }
    }

    public static void main(String args[]) {
        if (args.length != 1) {
            System.out.println("Usage: java ... FTPBackup <properties file>");
            System.out.println("Properties file requires:");
            System.out.println("ftp.server, ftp.user, ftp.password, ftp.remote.dir, ftp.local.dir");
        }
        Properties props = new Properties();
        try {
            String filename = args[0];
            FileInputStream fis = new FileInputStream(filename);
            props.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String server = props.getProperty("ftp.server");
        String user = props.getProperty("ftp.user");
        String password = props.getProperty("ftp.password");
        String remote = props.getProperty("ftp.remote.dir");
        String local = props.getProperty("ftp.local.dir");
        FTPBackup ftpbackup = new FTPBackup();
        ftpbackup.putDataFiles(server, user, password, local, remote);
    }
}
