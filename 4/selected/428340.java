package http.downloader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpHost;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.farng.mp3.MP3File;
import org.farng.mp3.TagException;
import org.farng.mp3.id3.ID3v1;
import IO.IOUtility;
import utils.Mp3Utils;

/**
 * Downloads URL using an auto incremented parameter . The range should be specified.
 * <p>
 * A swing UI for this code would make it very user friendly.
 * <p>
 * Also can be used for ebook download, batch image download etc.
 * <p>
 * Use Mp3Downloader which extends me to download music.
 * @author sandeep
 * 
 */
public class URLDownloaderAutomatic {

    static final String PROPERTY_KEY_DOWNLOAD_FILE_NAME = "download.file.name";

    static final String HOST_NAME_PROPERTY_KEY = "host.name";

    static final String PARAM_NAME_PROPERTY_KEY = "param.name";

    String downloadDir;

    protected PropertiesConfiguration configuration = null;

    static final String DEFAULT_PROPERTIES_FILE_PATH = "download.properties";

    protected HttpClient client;

    public URLDownloaderAutomatic(String propertyFilePath) {
        try {
            configuration = new PropertiesConfiguration(propertyFilePath);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        new URLDownloaderAutomatic(DEFAULT_PROPERTIES_FILE_PATH).doIt();
    }

    public void doIt() throws Exception {
        downloadDir = configuration.getString("download.dir");
        String downloadURL = (String) configuration.getString("download.url");
        int paramStart = configuration.getInt("param.start");
        int paramEnd = configuration.getInt("param.end");
        String paramName = configuration.getString(PARAM_NAME_PROPERTY_KEY);
        List<String> urls = new ArrayList<String>();
        String url = null;
        String paddingFormat = (String) configuration.getProperty("padding.format");
        for (int i = paramStart; i <= paramEnd; i++) {
            String indexStr = paddingFormat != null ? String.format(paddingFormat, i) : i + "";
            url = downloadURL.replaceFirst(paramName, indexStr + "");
            urls.add(url);
        }
        download(urls, downloadDir);
    }

    public Map<String, byte[]> download(List<String> urls, String dir) throws Exception {
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpClient client = getClient();
        Map<String, byte[]> downloads = new HashMap<String, byte[]>();
        int i = configuration.getInt("param.start");
        for (Iterator<String> iterator = urls.iterator(); iterator.hasNext(); ) {
            String url = iterator.next();
            GetMethod getMethod = new GetMethod(url);
            getMethod.setRequestHeader("Referer", url);
            System.out.println();
            System.out.println(Thread.currentThread().getName() + " : " + "downloading " + url);
            DownloadingThread downloadingThread = new DownloadingThread();
            try {
                downloadingThread.start();
                client.executeMethod(getMethod);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(Thread.currentThread().getName() + " : " + "failed downloading " + url);
            }
            InputStream response = getMethod.getResponseBodyAsStream();
            String fileNameRaw = configuration.getString(PROPERTY_KEY_DOWNLOAD_FILE_NAME);
            String fileName = fileNameRaw.replaceAll("INDEX", String.format("%02d", (i++)) + "");
            writeToFile(dir, response, fileName);
            downloadingThread.setAlive(false);
        }
        return downloads;
    }

    HttpClient getClient() {
        HttpClient client = new HttpClient();
        HostConfiguration hostConfiguration = new HostConfiguration();
        HttpHost host = new HttpHost(configuration.getString(HOST_NAME_PROPERTY_KEY));
        hostConfiguration.setHost(host);
        client.setHostConfiguration(hostConfiguration);
        return client;
    }

    void writeToFile(String dir, InputStream input, String fileName) throws FileNotFoundException, IOException {
        makeDirs(dir);
        FileOutputStream fo = null;
        try {
            System.out.println(Thread.currentThread().getName() + " : " + "Writing file " + fileName + " to path " + dir);
            File file = new File(dir, fileName);
            fo = new FileOutputStream(file);
            IOUtils.copy(input, fo);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to write " + fileName);
        }
    }

    protected String get(String url) {
        log("Getting :" + url);
        GetMethod getMethod = new GetMethod(url);
        getMethod.setRequestHeader("Referer", url);
        try {
            int executeMethod = client.executeMethod(getMethod);
            InputStream response = getMethod.getResponseBodyAsStream();
            String result = IOUtils.toString(response);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void log(Object msg) {
        System.out.println(msg);
    }

    void makeDirs(String dir) {
        File dirObj = new File(dir);
        if (!dirObj.exists()) {
            dirObj.mkdirs();
        }
    }

    public PropertiesConfiguration getConfiguration() {
        return configuration;
    }
}

class DownloadingThread extends Thread {

    boolean alive = true;

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    @Override
    public void run() {
        while (alive) {
            try {
                System.out.print(".");
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
