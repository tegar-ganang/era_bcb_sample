package de.herberlin.server.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Logger;
import de.herberlin.server.common.ConfigConstants;
import de.herberlin.server.common.Configuration;
import de.herberlin.server.common.HttpData;
import de.herberlin.server.common.TempFileHandler;
import de.herberlin.server.common.event.ApplicationEvent;
import de.herberlin.server.common.event.ErrorEvent;
import de.herberlin.server.common.event.EventDispatcher;
import de.herberlin.wwwutil.ChunkedInputStream;
import de.herberlin.wwwutil.ChunkedOutputStream;
import de.herberlin.wwwutil.ContentLengthInputStream;
import de.herberlin.wwwutil.ProxyResponse;
import de.herberlin.wwwutil.RequestHeader;
import de.herberlin.wwwutil.ResponseHeader;
import de.herberlin.wwwutil.httperror.BadRequest_400;

/**
 *
 * @author Hans Joachim Herbertz
 * @created 29.01.2003
 */
public class ProxyThread implements Runnable {

    private Socket client = null;

    private Logger logger = Logger.getLogger(getClass().getName());

    /**
	 * Constructor for ProxyThread.
	 */
    public ProxyThread(Socket client) {
        super();
        this.client = client;
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    public void run() {
        try {
            final String threadName = Thread.currentThread().getName();
            logger.finest("Proxy Thread started for client: " + client);
            EventDispatcher.add(new ApplicationEvent(ApplicationEvent.CONNECTION_ESTABLISHED));
            boolean keepAlive = true;
            while (keepAlive) {
                HttpData httpData = new HttpData();
                httpData.inetAddress = client.getInetAddress();
                httpData.port = client.getLocalPort();
                BufferedInputStream clientIn = new BufferedInputStream(client.getInputStream());
                RequestHeader requestHeader = new RequestHeader(clientIn);
                httpData.req = requestHeader;
                requestHeader.removeHeader("proxy-connection");
                if (Configuration.getPrefs().getBoolean(ConfigConstants.PROXY_NO_CACHING_HEADERS, true)) {
                    removeCachingHeaders(requestHeader);
                }
                String host = requestHeader.getHost();
                logger.fine(threadName + " Opening connection to: " + requestHeader.getUrl());
                if (host == null) throw new BadRequest_400("Host missing");
                Integer port = requestHeader.getPort();
                if (port == null) port = new Integer(80);
                Socket server = new Socket(host, port.intValue());
                BufferedOutputStream serverOut = new BufferedOutputStream(server.getOutputStream());
                requestHeader.write(serverOut);
                serverOut.flush();
                logger.finest(threadName + " Request Header sent to server.");
                if (requestHeader.getContentLength() != null && requestHeader.getPostData() != null) {
                    logger.fine(threadName + " Request Content-Length=" + requestHeader.getContentLength());
                    serverOut.write(requestHeader.getPostData());
                    serverOut.flush();
                    logger.finest(threadName + " Content written to server.");
                }
                BufferedInputStream serverIn = new BufferedInputStream(server.getInputStream());
                ResponseHeader responseHeader = new ProxyResponse(serverIn);
                if (Configuration.getPrefs().getBoolean(ConfigConstants.PROXY_NO_CACHING_HEADERS, true)) {
                    removeCachingHeaders(responseHeader);
                }
                httpData.resp = responseHeader;
                if (responseHeader.getContentLength() != null) {
                    httpData.fileData.setContentLength(responseHeader.getContentLength().longValue());
                }
                httpData.fileData.setContentType(responseHeader.getHeader("Content-Type"));
                httpData.fileData.setEncoding(responseHeader.getHeader("Content-Encoding"));
                logger.finest(threadName + " Response headers read:" + httpData.resp);
                BufferedOutputStream clientOut = new BufferedOutputStream(client.getOutputStream());
                responseHeader.write(clientOut);
                clientOut.flush();
                logger.finest(threadName + " Response header written to client.");
                if (responseHeader.getContentLength() != null) {
                    ContentLengthInputStream clIn = new ContentLengthInputStream(serverIn, responseHeader.getContentLength().intValue());
                    File cacheFile = TempFileHandler.getTempFile();
                    httpData.fileData.setFile(cacheFile);
                    FileOutputStream fileOut = new FileOutputStream(cacheFile);
                    byte[] bytes = new byte[4800];
                    int read = -1;
                    logger.finest(threadName + " Writing contentLength input to file:" + cacheFile);
                    while ((read = clIn.read(bytes)) != -1) {
                        fileOut.write(bytes, 0, read);
                    }
                    fileOut.close();
                    logger.finest(threadName + " Writing contentLength input to client.");
                    slowOutput(clientOut, cacheFile);
                    clientOut.flush();
                } else if (responseHeader.getHeader("Transfer-encoding") != null && responseHeader.getHeader("Transfer-Encoding").equalsIgnoreCase("chunked")) {
                    ChunkedInputStream chIn = new ChunkedInputStream(serverIn);
                    File cacheFile = TempFileHandler.getTempFile();
                    httpData.fileData.setFile(cacheFile);
                    FileOutputStream fileOut = new FileOutputStream(cacheFile);
                    int read = -1;
                    logger.finest(threadName + " Writing Chunked to file: " + cacheFile);
                    while ((read = chIn.read()) != -1) {
                        fileOut.write(read);
                    }
                    fileOut.close();
                    logger.finest(threadName + " Writing Chunked to client.");
                    ChunkedOutputStream chOut = new ChunkedOutputStream(clientOut);
                    slowOutput(chOut, cacheFile);
                    chIn.close();
                    chOut.flush();
                    chOut.close();
                } else if ((responseHeader.getHeader("Connection") != null && responseHeader.getHeader("Connection").equalsIgnoreCase("close")) || (responseHeader.getProtocol().equalsIgnoreCase("http/1.0"))) {
                    keepAlive = false;
                    File cacheFile = TempFileHandler.getTempFile();
                    httpData.fileData.setFile(cacheFile);
                    FileOutputStream fileOut = new FileOutputStream(cacheFile);
                    int read = -1;
                    logger.finest(threadName + " Writing connectionClose to input to file:" + cacheFile);
                    while ((read = serverIn.read()) != -1) {
                        fileOut.write(read);
                    }
                    fileOut.close();
                    logger.finest(threadName + " Writing connectionClose to client.");
                    slowOutput(clientOut, cacheFile);
                }
                EventDispatcher.add(httpData.asEvent());
                logger.finest(threadName + " Request: " + requestHeader.getUrl() + " done.");
            }
        } catch (Throwable e) {
            logger.info(e + "");
            EventDispatcher.add(new ErrorEvent(e));
        } finally {
            try {
                client.close();
            } catch (IOException e1) {
                logger.info(e1 + "");
            }
            EventDispatcher.add(new ApplicationEvent(ApplicationEvent.CONNECTION_CLOSED));
        }
        logger.fine(Thread.currentThread().getName() + " terminated.");
    }

    private void slowOutput(OutputStream out, File file) throws Exception {
        FileInputStream in = new FileInputStream(file);
        byte[] buffer = new byte[2048];
        int read = -1;
        long aSleep = 10 * (long) Configuration.getPrefs().getInt(ConfigConstants.MODE_PROXY + ConfigConstants.SETTING_DELAY, 0);
        while ((read = in.read(buffer)) > 0) {
            long start = System.currentTimeMillis();
            if (aSleep > 0) Thread.sleep(aSleep);
            out.write(buffer, 0, read);
        }
        in.close();
    }

    private void removeCachingHeaders(ResponseHeader response) {
        response.setHeader("Expires", "Thu, 19 Nov 1981 08:52:00 GMT");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");
        response.removeHeader("ETag");
        response.removeHeader("Last-Modified");
    }

    private void removeCachingHeaders(RequestHeader request) {
        request.removeHeader("If-Modified-Since");
        request.removeHeader("If-None-Match");
        logger.finer("Caching Headers removed: " + request);
    }
}
