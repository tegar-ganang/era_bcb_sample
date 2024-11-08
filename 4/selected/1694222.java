package net.sf.jfdm;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 *
 * @author Rohan Ranade
 */
public final class DownloadTask {

    private HttpClient httpClient;

    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private static final String PROP_DOWNLOAD_UPDATE = "download_update";

    private String url;

    private String fileName;

    private File downloadDir;

    public DownloadTask(String url, File downloadDir, String fileName) {
        this.url = url;
        this.downloadDir = downloadDir;
        this.fileName = fileName;
        this.httpClient = new HttpClient();
    }

    public boolean doDownload() throws FileNotFoundException, IOException {
        if (downloadDir == null || !downloadDir.exists()) {
            throw new IllegalArgumentException("Cannot have a non-existent download directory");
        }
        File f = new File(downloadDir, fileName);
        FileOutputStream fos = new FileOutputStream(f);
        GetMethod getMethod = new GetMethod(url);
        int respCode = httpClient.executeMethod(getMethod);
        if (respCode != HttpStatus.SC_OK) {
            return false;
        }
        f.createNewFile();
        InputStream is = getMethod.getResponseBodyAsStream();
        long fileSize = getMethod.getResponseContentLength();
        long readSize = 0;
        byte[] buffer = new byte[1024];
        int readLength = -1;
        while ((readLength = is.read(buffer)) != -1) {
            fos.write(buffer, 0, readLength);
            readSize += readLength;
            firePropertyChange(readSize);
        }
        fos.close();
        is.close();
        return true;
    }

    public void addChangeListener(PropertyChangeListener pcl) {
        pcs.addPropertyChangeListener(pcl);
    }

    public void removeChangeListener(PropertyChangeListener pcl) {
        pcs.removePropertyChangeListener(pcl);
    }

    private void firePropertyChange(long readCount) {
        pcs.firePropertyChange(PROP_DOWNLOAD_UPDATE, null, readCount);
    }
}
