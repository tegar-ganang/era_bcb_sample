package net.sf.jood.download.ftp;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import net.sf.jood.download.Download;

/**
 * @author Firas Al Khalil
 * 
 */
public class FtpDownload extends Download {

    private URL url = null;

    private FTPClient ftp = null;

    public FtpDownload(String url, File file) {
        super(file);
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        try {
            ftp = new FTPClient();
            ftp.connect(this.url.getHost(), this.url.getPort() == -1 ? this.url.getDefaultPort() : this.url.getPort());
            String username = "anonymous";
            String password = "";
            if (this.url.getUserInfo() != null) {
                username = this.url.getUserInfo().split(":")[0];
                password = this.url.getUserInfo().split(":")[1];
            }
            ftp.login(username, password);
            long startPos = 0;
            if (getFile().exists()) startPos = getFile().length(); else getFile().createNewFile();
            ftp.download(this.url.getPath(), getFile(), startPos, new FTPDTImpl());
            ftp.disconnect(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            speedTimer.cancel();
        }
    }

    @Override
    public void stop() {
        try {
            ftp.abortCurrentDataTransfer(true);
            ftp.disconnect(true);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FTPIllegalReplyException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (FTPException e) {
            e.printStackTrace();
        } finally {
            speedTimer.cancel();
        }
    }

    @Override
    public void run() {
    }

    class FTPDTImpl implements FTPDataTransferListener {

        @Override
        public void aborted() {
            setDownloading(false);
            speedTimer.cancel();
        }

        @Override
        public void completed() {
            speedTimer.cancel();
        }

        @Override
        public void failed() {
            speedTimer.cancel();
        }

        @Override
        public void started() {
            setDownloading(true);
            bytesDownloaded = 0;
        }

        @Override
        public void transferred(int length) {
            bytesDownloaded += (long) length;
        }
    }
}
