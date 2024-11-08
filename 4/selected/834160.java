package osdep;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import osdep.proxy.ProxyAuthenticationHandler;

/**
 * To download a file from the internet to a local directory
 * @author SHZ Mar 12, 2008
 */
public class Downloader {

    /**
	 * listens to the download events (start, end, downloaded a piece)
	 */
    private IDownloadListener listener;

    /**
	 * if the connection uses a proxy that needs authentication, this is the needed authentication data
	 */
    private ProxyAuthenticationHandler authenticationHandler;

    public Downloader(ProxyAuthenticationHandler authenticationHandler, IDownloadListener listener) {
        this.authenticationHandler = authenticationHandler;
        this.listener = listener;
    }

    /**
	 * The download process (start, download, end)
	 * @param file
	 * @param connection
	 * @throws DownloadException
	 */
    private void fromInternetToFile(File file, URLConnection connection) throws DownloadException {
        try {
            listener.downloadStarted(connection.getURL().toExternalForm());
            long len = connection.getContentLength();
            InputStream in = connection.getInputStream();
            try {
                FileOutputStream out = new FileOutputStream(file);
                try {
                    byte[] buffer = new byte[10240];
                    int totalReadBytes = 0;
                    int readBytes;
                    while ((readBytes = in.read(buffer)) > 0) {
                        out.write(buffer, 0, readBytes);
                        totalReadBytes += readBytes;
                        listener.downloadStep(totalReadBytes, len);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
            listener.downloadFinished(connection.getURL().toExternalForm());
        } catch (FileNotFoundException e) {
            throw new DownloadException("Downloading the file " + connection.getURL() + " to " + file + " got", e);
        } catch (IOException e) {
            throw new DownloadException("Downloading the file " + connection.getURL() + " to " + file + " got", e);
        }
    }

    /**
	 * Downloads an internet file as a local file
	 * @param fileURL the internet url of the file
	 * @param filePath the downloaded file
	 * @throws DownloadException
	 */
    public void downloadFile(String fileURL, final String filePath) throws DownloadException {
        ConnectionHandler handler = new ConnectionHandler(authenticationHandler);
        try {
            handler.doWithConnection(fileURL, new ICallableWithParameter<Void, URLConnection, DownloadException>() {

                public Void call(URLConnection connection) throws DownloadException {
                    fromInternetToFile(new File(filePath), connection);
                    return null;
                }
            });
        } catch (ConnectionException e) {
            throw new DownloadException("Downloading the file " + fileURL + " in the path " + filePath + " got ", e);
        }
    }
}
