package com.webdeninteractive.sbie.service;

import com.webdeninteractive.sbie.Service;
import com.webdeninteractive.sbie.Client;
import com.webdeninteractive.sbie.ProtocolHandler;
import com.webdeninteractive.sbie.ServiceEndpoint;
import com.webdeninteractive.sbie.util.DocumentUtils;
import com.webdeninteractive.sbie.exception.TransportException;
import java.io.IOException;
import java.lang.SecurityException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FilenameFilter;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;

/**
 * SBIE Service for simple file transport.  This service takes any
 * files in a watch directory, sends them to the endpoint, then moves
 * the sent file to a "finished" directory.  The contents of the file
 * are sent without any particular interpretation, just copied to the
 * upstream receiver.
 * <P>
 * This service is configured with two parameters:
 * <code>watchDirectory</code> and
 * <code>finishedDirectory</code>.
 *
 * @author gfast
 * @version $Id: FileService.java,v 1.1.1.1 2003/05/20 16:56:49 gdf Exp $ 
 */
public class FileService extends ServiceBase implements Service {

    private Logger logger = Logger.getLogger("com.webdeninteractive.sbie.service.FileService");

    /** Create and initialize a ServiceEndpoint for this Service,
     *  filling in all the request-agnostic parameters.
     *  For any call made by this service, only the method property of the EP
     *  should need to be specified further.
     */
    protected ServiceEndpoint initEndpoint() {
        ServiceEndpoint ep = protocolHandler.createServiceEndpoint(this);
        String hostname = this.getParameter("hostname");
        if (hostname == null) {
            logger.warn("Service has null hostname parameter");
        }
        String hostport = this.getParameter("hostport");
        if (hostport == null) {
            logger.warn("Service has null hostport parameter");
        }
        String soapservice = this.getParameter("SoapService");
        if (soapservice == null) {
            logger.warn("Service has null soapservice parameter");
            soapservice = "null";
        }
        if (!soapservice.startsWith("/")) {
            soapservice = "/" + soapservice;
        }
        ep.setTarget("http://" + hostname + ":" + hostport + soapservice);
        ep.setUser(this.getParameter("username"));
        ep.setPassword(this.getParameter("password"));
        return ep;
    }

    /** Have this service perform its task. */
    public void runService() {
        ServiceEndpoint ep = initEndpoint();
        String watchDirName = this.getParameter("watchDirectory");
        String finishedDirName = this.getParameter("finishedDirectory");
        File watchDir = new File(watchDirName);
        if (!watchDir.isDirectory()) {
            logger.warn("watchDirectory parameter does not specify a (existing) directory");
            return;
        }
        File finishedDir = new File(finishedDirName);
        if (!finishedDir.isDirectory()) {
            logger.warn("finishedDirectory parameter does not specify a (existing) directory");
            return;
        }
        logger.debug("Scanning " + watchDir.getPath());
        File[] files = watchDir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String filename) {
                return filename.endsWith(".xml");
            }
        });
        for (int i = 0; i < files.length; i++) {
            try {
                FileInputStream is = new FileInputStream(files[i]);
                Document doc = DocumentUtils.parseInputStream(is);
                logger.info("Sending " + files[i].getName() + "...");
                sendDoc(watchDir, finishedDir, files[i], doc, ep);
            } catch (IOException e) {
                logger.warn("Can't read watched file " + files[i].getName());
            } catch (Exception e) {
                logger.warn("Can't parse watched file " + files[i].getName());
            }
        }
    }

    protected void sendDoc(File indir, File outdir, File orig, Document doc, ServiceEndpoint ep) {
        ep.setMethod("simpleDocumentTransfer");
        Document response = null;
        try {
            response = protocolHandler.sendMessage(ep, doc);
        } catch (TransportException e) {
            logger.warn("Message was not accepted, will try again later");
            return;
        }
        String serial = String.valueOf(System.currentTimeMillis());
        File origCopy = new File(outdir, orig.getName() + "." + serial);
        File respDrop = new File(outdir, orig.getName() + "." + serial + ".resp");
        FileOutputStream respos = null;
        try {
            respos = new FileOutputStream(respDrop);
            serializeDocument(respos, response);
        } catch (IOException e) {
            logger.warn("Failed to dump response");
            return;
        } finally {
            try {
                respos.close();
            } catch (IOException ignored) {
            }
        }
        FileInputStream in = null;
        FileOutputStream out = null;
        byte[] buffer = new byte[2048];
        try {
            in = new FileInputStream(orig);
            out = new FileOutputStream(origCopy);
            int bytesread = 0;
            while ((bytesread = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesread);
            }
        } catch (IOException e) {
            logger.warn("Failed to copy original");
            return;
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException ignored) {
            }
        }
        orig.delete();
        logger.info("File processed: " + orig.getName());
    }

    public void serializeDocument(OutputStream out, Document dom) throws IOException {
        OutputFormat format = new OutputFormat();
        format.setLineWidth(0);
        format.setIndent(5);
        XMLSerializer serializer = new XMLSerializer(out, format);
        serializer.serialize(dom);
    }
}
