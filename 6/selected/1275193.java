package lv.webkursi.klucis.eim.demo.miscellaneous.ftpclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;

public class FtpFileService {

    private String host;

    private int port;

    private String user;

    private String password;

    private String remotedir;

    private String remotefile;

    public String[] getFile() {
        List<String> records = new ArrayList<String>();
        FTPClient ftp = new FTPClient();
        try {
            int reply;
            FTPClientConfig conf = new FTPClientConfig(FTPClientConfig.SYST_UNIX);
            ftp.configure(conf);
            ftp.connect(host, port);
            System.out.println("Connected to " + host + ".");
            System.out.print(ftp.getReplyString());
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
            }
            ftp.login(user, password);
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused login.");
            }
            InputStream is = ftp.retrieveFileStream(remotedir + "/" + remotefile);
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                if (!line.equals("")) {
                    records.add(line);
                }
            }
            br.close();
            isr.close();
            is.close();
            ftp.logout();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        return records.toArray(new String[0]);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getRemotedir() {
        return remotedir;
    }

    public void setRemotedir(String remotedir) {
        this.remotedir = remotedir;
    }

    public String getRemotefile() {
        return remotefile;
    }

    public void setRemotefile(String remotefile) {
        this.remotefile = remotefile;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
