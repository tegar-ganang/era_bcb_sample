package net.sf.connect5d.osf.donorsvc.pub.fsutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import net.sf.connect5d.osf.donorsvc.pub.util.XMLOverHTTP;
import net.sf.connect5d.osf.donorsvc.pub.util.XMLUtil;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 */
public class RequestForwarder {

    private static Logger log = Logger.getLogger(RequestForwarder.class);

    private String address = null;

    private File requestDir = null;

    private File processedRequestsDir = null;

    Timer timer = null;

    /**
     */
    public RequestForwarder(FileSystem fileSystem) {
        File requests = new File(fileSystem.getDonorDir(), "requests");
        fileSystem.createDirectory(requests);
        log.info("Set the requests directory to: " + requests.getAbsolutePath());
        File processedRequests = new File(requests, "processed");
        fileSystem.createDirectory(processedRequests);
        log.info("Set the process requests directory to: " + processedRequests.getAbsolutePath());
        timer = new Timer("Request Forwarder");
    }

    /**
     * Add a request to the file system
     * @param fileName the filename to save the request using
     * @param request the document to save
     */
    public void addRequest(String fileName, Document request) {
        File outFile = new File(getRequestDir(), fileName);
        if (outFile.exists()) {
            log.warn("An attempt was made to write the request file to a file that already exists");
            throw new FSServiceException(FSServiceException.Code.UNEXPECTED);
        }
        try {
            XMLUtil.transformToStream(request, new FileOutputStream(outFile));
        } catch (FileNotFoundException ex) {
            log.error("Could not write to request file", ex);
        }
        log.debug("wrote request to file " + outFile.getAbsolutePath());
    }

    /**
     */
    public List<String> getUnforwardedRequestList() {
        return Arrays.asList(getRequestDir().list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        }));
    }

    /**
     */
    public Document getUnforwardedRequest(String id) {
        try {
            Document doc = XMLUtil.parseStream(new FileInputStream(new File(getRequestDir(), id)));
            return doc;
        } catch (IOException ex) {
            log.error(ex);
            throw new FSServiceException(ex);
        } catch (SAXException ex) {
            log.error("XML Document not valid xml", ex);
            throw new FSServiceException(ex);
        }
    }

    /**
     */
    public void markForwarded(String id, Document response) {
        File requestFile = new File(getRequestDir(), id);
        boolean success = requestFile.renameTo(new File(getProcessedRequestsDir(), id));
        if (!success) {
            log.error("Could not move request id " + id + " to the processed directory");
        }
        String responseId = id.substring(0, id.length() - 4) + "-RESPONSE.xml";
        File responseFile = new File(getProcessedRequestsDir(), responseId);
        try {
            XMLUtil.transformToStream(response, new FileOutputStream(responseFile));
        } catch (FileNotFoundException ex) {
            log.error("Could not write response id " + responseId + " to ther processed directory", ex);
        }
    }

    /**
     */
    public void trigger(boolean ignoreFileNotFound) throws IOException, MalformedURLException {
        log.debug("Triggering request forwarding");
        if (address == null) {
            log.warn("Attempting to trigger request forwarding without a destination address set");
            return;
        }
        try {
            List<String> requestIds = getUnforwardedRequestList();
            for (String id : requestIds) {
                Document request = getUnforwardedRequest(id);
                Document response = XMLOverHTTP.send(address, request);
                markForwarded(id, response);
            }
        } catch (FileNotFoundException ex) {
            log.warn("Could not contact remote donor service " + address);
            if (!ignoreFileNotFound) {
                throw ex;
            }
        } catch (UnknownHostException ex) {
            log.warn("Could not contact remote donor service " + address);
            if (!ignoreFileNotFound) {
                throw ex;
            }
        }
    }

    /**
     */
    public String getAddress() {
        return address;
    }

    /**
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     */
    public void startTimer(int delay) {
        if (timer != null) {
            timer = new Timer("Request Forwarder");
            timer.schedule(new TimerTask() {

                public void run() {
                    try {
                        trigger(true);
                    } catch (MalformedURLException ex) {
                        log.error("URL for the remote donor service is not valid", ex);
                    } catch (IOException ex) {
                        log.error("Error while triggering request forwards", ex);
                    }
                }
            }, 0, 10000);
        }
    }

    /**
     */
    public File getRequestDir() {
        return requestDir;
    }

    /**
     */
    public void setRequestDir(File requestDir) {
        this.requestDir = requestDir;
    }

    /**
     */
    public void setRequestDir(String requestDir) {
        this.requestDir = new File(requestDir);
    }

    /**
     */
    public File getProcessedRequestsDir() {
        return processedRequestsDir;
    }

    /**
     */
    public void setProcessedRequestsDir(File processedRequestsDir) {
        this.processedRequestsDir = processedRequestsDir;
    }

    /**
     */
    public void setProcessedRequestsDir(String processedRequestsDir) {
        this.processedRequestsDir = new File(processedRequestsDir);
    }
}
