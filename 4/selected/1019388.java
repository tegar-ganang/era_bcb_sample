package com.volantis.mcs.prerenderer.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Request MCS for XDIME page or their resource, save prerendered response in file 
 */
public class FetcherThread extends Thread {

    private Exception result;

    private Queue queue;

    private HttpClient httpClient;

    private String deviceName;

    private File deviceFile;

    FetcherThread(Queue queue, HttpClient client, File deviceFile, String device) {
        this.queue = queue;
        this.httpClient = client;
        this.deviceName = device;
        this.deviceFile = deviceFile;
    }

    /**
     * Request and save response in file
     */
    public void run() {
        Pair p = null;
        try {
            while ((p = queue.pop()) != null) {
                GetMethod get = new GetMethod(p.getRemoteUri());
                try {
                    get.setFollowRedirects(true);
                    get.setRequestHeader("Mariner-Application", "prerenderer");
                    get.setRequestHeader("Mariner-DeviceName", deviceName);
                    int iGetResultCode = httpClient.executeMethod(get);
                    if (iGetResultCode != 200) {
                        throw new IOException("Got response code " + iGetResultCode + " for a request for " + p.getRemoteUri());
                    }
                    InputStream is = get.getResponseBodyAsStream();
                    File localFile = new File(deviceFile, p.getLocalUri());
                    localFile.getParentFile().mkdirs();
                    OutputStream os = new FileOutputStream(localFile);
                    IOUtils.copy(is, os);
                    os.close();
                } finally {
                    get.releaseConnection();
                }
            }
        } catch (Exception ex) {
            result = ex;
        }
    }

    /**
     * @return Returns the result.
     */
    public Exception getResult() {
        return result;
    }
}
