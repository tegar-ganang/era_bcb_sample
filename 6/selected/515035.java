package uploadHookServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;
import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;

public class HookServ implements Runnable {

    HookCallbacks cbacks;

    FTPClient fClient;

    Thread thread;

    int pollSecs;

    String rHost, user, passwd;

    public HookServ(String host, int pollInterval, String uname, String pass, HookCallbacks callbacks) {
        cbacks = callbacks;
        pollSecs = pollInterval;
        rHost = host;
        user = uname;
        passwd = pass;
        thread = new Thread(this);
        thread.start();
    }

    private void initializeServer() throws IOException, FTPException {
        fClient = new FTPClient();
        fClient.setRemoteHost(rHost);
        fClient.connect();
        fClient.login(user, passwd);
    }

    public void sendFile(File f, String dir) throws IOException, FTPException {
        if (dir.length() >= 1 && dir.lastIndexOf('/') == dir.length() - 1) fClient.put(f.getAbsolutePath(), dir + f.getName()); else fClient.put(f.getAbsolutePath(), dir + '/' + f.getName());
    }

    private static FTPFile findFTPFileByName(LinkedList<FTPFile> a, String n) {
        for (FTPFile f : a) if (f.getName().equals(n)) return f;
        return null;
    }

    private void evaluateDirectory(LinkedList<FTPFile> oldFiles, String dir) throws IOException, FTPException, ParseException {
        FTPFile[] newFiles = fClient.dirDetails(dir);
        for (FTPFile f : newFiles) {
            if (f.isDir()) {
                evaluateDirectory(oldFiles, dir + f.getName() + "/");
            } else {
                FTPFile oldFile = findFTPFileByName(oldFiles, f.getName());
                if (oldFile == null || !oldFile.lastModified().equals(f.lastModified())) {
                    File localFile = new File(System.getProperty("java.io.tmpdir") + File.separatorChar + f.getName());
                    localFile.createNewFile();
                    fClient.get(new FileOutputStream(localFile), dir + f.getName());
                    cbacks.fileReceived(localFile);
                    if (oldFile != null) oldFiles.set(oldFiles.indexOf(oldFile), f); else oldFiles.add(f);
                }
            }
        }
    }

    public void run() {
        LinkedList<FTPFile> fList = new LinkedList<FTPFile>();
        while (!thread.isInterrupted()) {
            try {
                initializeServer();
                evaluateDirectory(fList, "/");
                fClient.quit();
            } catch (Exception e) {
                cbacks.exception(e);
                break;
            }
            try {
                Thread.sleep(1000 * pollSecs);
            } catch (Exception e) {
            }
        }
    }
}
