package games.midhedava.client.update;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Properties;

/**
 * a very simple http client
 *
 * @author hendrik
 */
public class HttpClient {

    private String urlString = null;

    private HttpURLConnection connection = null;

    private InputStream is = null;

    private ProgressListener progressListener = null;

    private int timeout = 1500;

    private boolean tryVeryHard = false;

    /**
	 * An interface to notify some other parts of the program about
	 * download process.
	 */
    public interface ProgressListener {

        /**
		 * update download status
		 *
		 * @param downloadedBytes bytes downloaded now
		 */
        public void onDownloading(int downloadedBytes);

        /**
		 * completed download of this file 
		 *
		 * @param downloadedBytes completed download
		 */
        public void onDownloadCompleted(int downloadedBytes);
    }

    /**
	 * Creates a HTTP-Client which will connect to the specified URL
	 *
	 * @param url URL to connect to
	 */
    public HttpClient(String url) {
        this.urlString = url;
    }

    /**
	 * Creates a HTTP-Client which will connect to the specified URL
	 *
	 * @param url URL to connect to
	 * @param tryVeryHard true, to do several attempts.
	 */
    public HttpClient(String url, boolean tryVeryHard) {
        this.urlString = url;
        this.tryVeryHard = tryVeryHard;
    }

    /**
	 * Sets a ProgressListener to be informed of download progress
	 *
	 * @param progressListener ProgressListener
	 */
    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
	 * connects to the server and opens a stream. 
	 */
    private void openInputStream() {
        try {
            URL url = new URL(urlString);
            int retryCount = 0;
            int myTimeout = timeout;
            while (is == null) {
                retryCount++;
                try {
                    HttpURLConnection.setFollowRedirects(true);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(timeout);
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        System.err.println("HttpServer returned an error code (" + urlString + "): " + connection.getResponseCode());
                        connection = null;
                    }
                    if (connection != null) {
                        is = connection.getInputStream();
                        if (retryCount > 1) {
                            System.err.println("Retry successful");
                        }
                    }
                } catch (SocketTimeoutException e) {
                    System.err.println("Timeout (" + urlString + "): " + " " + e.toString());
                }
                myTimeout = myTimeout * 2;
                if (!tryVeryHard || (retryCount > 3)) {
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error connecting to http-Server (" + urlString + "): ");
            e.printStackTrace(System.err);
        }
        return;
    }

    /**
	 * Return an InputStream to read the requested file from.
	 * You have to close it using @see close().
	 *
	 * @return InputStream or null on error.
	 */
    public InputStream getInputStream() {
        openInputStream();
        return is;
    }

    /**
	 * Closes the connection and associated streams.
	 */
    public void close() {
        try {
            is.close();
        } catch (IOException e) {
            System.err.println(e);
            e.printStackTrace(System.err);
        }
        connection.disconnect();
    }

    /**
	 * fetches the first line of a file using http and closes the
	 * connection automatically.
	 *
	 * @return the first line
	 */
    public String fetchFirstLine() {
        String line = null;
        try {
            openInputStream();
            if (is == null) {
                return null;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            line = br.readLine();
            br.close();
            connection.disconnect();
        } catch (Exception e) {
            System.err.println("Error connecting to http-Server: ");
            e.printStackTrace(System.err);
        }
        return line;
    }

    /**
	 * fetches a file using http as Properties object and closes the
	 * connection automatically.
	 *
	 * @return the first line
	 */
    public Properties fetchProperties() {
        Properties prop = null;
        openInputStream();
        if (is == null) {
            return prop;
        }
        try {
            prop = new Properties();
            prop.load(is);
        } catch (IOException e) {
            System.err.println(e);
            e.printStackTrace(System.err);
        }
        this.close();
        return prop;
    }

    /**
	 * Fetches a file from the HTTP-Server and stores it on disk
	 *
	 * @param filename name of the file to write
	 * @return true on success, false otherwise
	 */
    public boolean fetchFile(String filename) {
        boolean res = false;
        openInputStream();
        if (is == null) {
            return res;
        }
        try {
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(filename));
            copyStream(is, os);
            connection.disconnect();
            res = true;
        } catch (Exception e) {
            res = true;
            System.err.println(e);
            e.printStackTrace(System.err);
        }
        return res;
    }

    /**
	 * Copies data from an inputStream to and outputStream and closes both
	 * steams after work
	 *
	 * @param inputStream  stream to read from
	 * @param outputStream stream to write to
	 * @throws IOException on an input/output error
	 */
    private void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[10240];
        int length = inputStream.read(buffer);
        int byteCounter = length;
        while (length > -1) {
            outputStream.write(buffer, 0, length);
            length = inputStream.read(buffer);
            if (length > 0) {
                byteCounter = byteCounter + length;
                if (progressListener != null) {
                    progressListener.onDownloading(byteCounter);
                }
            }
        }
        inputStream.close();
        outputStream.close();
        progressListener.onDownloadCompleted(byteCounter);
    }
}
