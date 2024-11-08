package com.ask.FSD;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import org.openide.filesystems.FileObject;

/**
 *
 * @author dany
 */
public class UploaderThread extends Thread {

    private FileObject fo1;

    private FTPClient ftp;

    UploaderThread(FileObject StartFile) {
        fo1 = StartFile;
    }

    private String getRelativePath(String Assoluto) {
        String rel = "";
        String root = Settings.getMainProject().getProjectDirectory().getPath();
        return rel;
    }

    private boolean appartieneMainProg(String Assoluto) {
        boolean b = false;
        String root = Settings.getMainProject().getProjectDirectory().getPath();
        int indice = Assoluto.indexOf(root);
        if (indice > -1) {
            b = true;
        }
        return b;
    }

    private String getEstensione(FileObject fo) {
        String este = "";
        int indice = fo.getPath().lastIndexOf(".");
        este = fo.getPath().substring(indice + 1);
        return este;
    }

    private String getCompiledClassFile(FileObject fo) {
        String cla = "";
        String src = Settings.getMainProject().getProjectDirectory().getPath() + File.separator + "src" + File.separator + "java" + File.separator;
        String rel = fo.getPath().substring(src.length());
        cla = Settings.getMainProject().getProjectDirectory().getPath() + File.separator + "build" + File.separator + "web" + File.separator + "WEB-INF" + File.separator + "classes" + File.separator;
        cla = cla + rel;
        cla = cla.substring(0, cla.length() - 4) + "class";
        return cla;
    }

    private String getBuildFile(FileObject fo) {
        String cla = "";
        String src = Settings.getMainProject().getProjectDirectory().getPath() + File.separator + "web" + File.separator;
        String rel = fo.getPath().substring(src.length());
        cla = Settings.getMainProject().getProjectDirectory().getPath() + File.separator + "build" + File.separator + "web" + File.separator;
        cla = cla + rel;
        return cla;
    }

    private boolean existFile(String fileName) {
        boolean b = false;
        try {
            File f = new File(fileName);
            b = f.exists() && f.canRead();
        } catch (java.lang.Exception Ex) {
        }
        return b;
    }

    private String getBuildFileRemote(FileObject fo) {
        String rel = "";
        String src = Settings.getMainProject().getProjectDirectory().getPath() + File.separator + "web" + File.separator;
        rel = "/" + fo.getPath().substring(src.length());
        return rel;
    }

    private String getCompiledClassFileRemote(FileObject fo) {
        String rel = "";
        String src = Settings.getMainProject().getProjectDirectory().getPath() + File.separator + "src" + File.separator + "java" + File.separator;
        rel = fo.getPath().substring(src.length());
        rel = "/WEB-INF/classes/" + rel;
        rel = rel.substring(0, rel.length() - 4) + "class";
        return rel;
    }

    @Override
    public void run() {
        ftp = new FTPClient();
        doUpload(fo1);
    }

    private boolean doUpload(FileObject fo) {
        boolean b = false;
        Settings.out("Selected : " + fo.getPath());
        if (!appartieneMainProg(fo.getPath())) {
            Settings.out("Error - the selected file : " + fo.getPath() + " does not belong to the main project.");
            b = false;
        } else {
            if (fo.isFolder()) {
                if (!fo.getName().equals("CVS")) {
                    Settings.out("- It's a directory, do scan...");
                    FileObject[] childs = fo.getChildren();
                    for (int i = 0; i < childs.length; i++) {
                        doUpload(childs[i]);
                    }
                }
            } else {
                if (getEstensione(fo).equals("java")) {
                    String compilato = getCompiledClassFile(fo);
                    if (existFile(compilato)) {
                        Settings.out("- It's a java source file, the compiled one is : " + compilato);
                        String remoto = getCompiledClassFileRemote(fo);
                        InviaFile(compilato, remoto);
                    } else {
                        Settings.out("- Local file : " + compilato + " does not exist or unable to read. jump to next.");
                    }
                } else {
                    String buildfile = getBuildFile(fo);
                    if (existFile(buildfile)) {
                        String remoto = getBuildFileRemote(fo);
                        InviaFile(buildfile, remoto);
                    } else {
                        Settings.out("- Local file : " + buildfile + " does not exist or unable to read. jump to next.");
                    }
                }
            }
        }
        return b;
    }

    private void checkAndCreateRemotePath(String FileRemoto) {
        String cartella = FileRemoto.substring(0, FileRemoto.lastIndexOf("/"));
        try {
            if (!ftp.changeWorkingDirectory(cartella)) {
                String Attuale = "";
                ftp.changeWorkingDirectory("/");
                StringTokenizer st = new StringTokenizer(cartella, "/");
                while (st.hasMoreElements()) {
                    Attuale = Attuale + "/" + st.nextToken();
                    ftp.makeDirectory(Attuale);
                }
            }
        } catch (java.lang.Exception ex) {
        }
    }

    private boolean CheckConnection() {
        boolean b = false;
        String host = "" + Settings.getHost();
        String user = "" + Settings.getUser();
        String pass = "" + Settings.getPass();
        int port = Settings.getPort();
        if (!ftp.isConnected()) {
            try {
                int reply;
                ftp.connect(host, port);
                ftp.login(user, pass);
                ftp.enterLocalPassiveMode();
                reply = ftp.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftp.disconnect();
                    Settings.out("Error, connection refused from the FTP server." + host, 4);
                    b = false;
                } else {
                    b = true;
                }
            } catch (IOException e) {
                b = false;
                Settings.out("Error : " + e.toString(), 4);
                if (ftp.isConnected()) {
                    try {
                        ftp.disconnect();
                    } catch (IOException ioe) {
                    }
                }
            }
        } else {
            b = true;
        }
        return b;
    }

    private boolean InviaFile(String FileLocale, String FileRemoto) {
        boolean b = false;
        try {
            if (CheckConnection()) {
                checkAndCreateRemotePath(FileRemoto);
                File f = new File(FileLocale);
                FtpListener listener = new FtpListener(f);
                ftp.setCopyStreamListener(listener);
                Settings.out("Uploading file: " + FileLocale, 1);
                if (ftp.storeFile(FileRemoto, new FileInputStream(f))) {
                    b = true;
                    Settings.inLinePrint("100% ");
                    Settings.out("File uploaded: " + f.getName(), 1);
                } else {
                    Settings.out("Error, file NOT sent: " + f.getName(), 4);
                }
            }
        } catch (IOException e) {
            b = false;
            Settings.out("Error : " + e.toString(), 4);
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
        return b;
    }

    class FtpListener implements CopyStreamListener {

        long lastperc = 0;

        long filesize = 0;

        public FtpListener(File f) {
            lastperc = 0;
            filesize = f.length();
        }

        @Override
        public void bytesTransferred(CopyStreamEvent cse) {
        }

        @Override
        public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
            double perc = ((double) totalBytesTransferred / (double) filesize) * 100;
            if ((perc > 10) && (lastperc < perc)) {
                lastperc = (long) perc + 10;
                Settings.inLinePrint((int) perc + "% ");
            }
        }
    }
}
