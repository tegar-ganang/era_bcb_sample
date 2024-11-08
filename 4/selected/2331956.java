package com.google.gsa.valve.modules.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URLDecoder;
import org.htmlparser.Parser;
import org.htmlparser.visitors.NodeVisitor;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.log4j.Logger;
import org.htmlparser.util.ParserException;

/**
 * It processes the HTTP response when it's needed to rewrite URLs and send 
 * the content back to the requestor through the Security Framework
 * 
 */
public class HTTPAuthZProcessor {

    private static Logger logger = Logger.getLogger(HTTPAuthZProcessor.class);

    private static final int BUFFER_BLOCK_SIZE = 4096;

    /**
     * Class constructor
     * 
     */
    public HTTPAuthZProcessor() {
    }

    /**
     * Processes the HTTP response to check the content type and parsers then 
     * the document depending on the kind of content.
     * 
     * @param response HTTP response
     * @param method HTTP method
     * @param url document url
     * @param loginUrl login url
     */
    public static void processResponse(HttpServletResponse response, HttpMethodBase method, String url, String loginUrl) {
        logger.debug("Processing Response");
        String contentType = method.getResponseHeader("Content-Type").getValue();
        logger.debug("Content Type is... " + contentType);
        if (contentType != null) {
            if (contentType.startsWith("text/html")) {
                boolean processHTML = AuthorizationUtils.isProcessHTML();
                if (processHTML) {
                    logger.debug("It's an HTML doc that is going to be processed (URLs rewritten)");
                    try {
                        logger.debug("Document is HTML. Processing");
                        processHTML(response, method, url, loginUrl, contentType);
                    } catch (IOException e) {
                        logger.error("I/O Error processing HTML document: " + e.getMessage(), e);
                    } catch (ParserException e) {
                        logger.error("Parsering Error processing HTML document: " + e.getMessage(), e);
                    } catch (Exception e) {
                        logger.error("Error processing HTML document: " + e.getMessage(), e);
                    }
                } else {
                    logger.debug("It's an HTML doc that is NOT going to be processed (return content as is)");
                    try {
                        returnHTML(response, method, url, loginUrl, contentType);
                    } catch (IOException e) {
                        logger.error("I/O Error returning HTML document: " + e.getMessage(), e);
                    } catch (Exception e) {
                        logger.error("Error returning HTML document: " + e.getMessage(), e);
                    }
                }
            } else {
                try {
                    logger.debug("Document is not HTML. Processing");
                    setDocumentName(response, method, contentType);
                    processNonHTML(response, method);
                } catch (IOException e) {
                    logger.error("I/O Error processing NON HTML document: " + e.getMessage(), e);
                } catch (Exception e) {
                    logger.error("Error processing NON HTML document: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Sets the document name putting it into the Content-Disposition header
     * 
     * @param response HTTP response
     * @param method HTTP method
     */
    public static void setDocumentName(HttpServletResponse response, HttpMethodBase method, String contentType) {
        response.setHeader("Content-Type", contentType);
        String[] tabpath = (method.getPath()).split("/");
        String fileName = tabpath[tabpath.length - 1];
        String decodeFileName = null;
        try {
            decodeFileName = URLDecoder.decode(fileName, "UTF-8");
        } catch (Exception e) {
            logger.error("Exception decoding URL: " + e);
            decodeFileName = fileName;
        }
        response.setHeader("Content-Disposition", "inline; filename=" + decodeFileName);
    }

    /**
     * If the document is HTML, this method processes its content in order to 
     * rewrite the URLs it includes
     * 
     * @param response HTTP response
     * @param method HTTP method
     * @param url document url
     * @param loginUrl login url
     * 
     * @throws IOException
     * @throws ParserException
     */
    public static void processHTML(HttpServletResponse response, HttpMethodBase method, String url, String loginUrl) throws IOException, ParserException {
        processHTML(response, method, url, loginUrl, "text/html");
    }

    /**
     * If the document is HTML, this method processes its content in order to 
     * rewrite the URLs it includes
     * 
     * @param response HTTP response
     * @param method HTTP method
     * @param url document url
     * @param loginUrl login url
     * @param contenType content Type
     * 
     * @throws IOException
     * @throws ParserException
     */
    public static void processHTML(HttpServletResponse response, HttpMethodBase method, String url, String loginUrl, String contentType) throws IOException, ParserException {
        logger.debug("Processing an HTML document");
        String stream = null;
        Parser parser = null;
        NodeVisitor visitor = null;
        stream = readFully(new InputStreamReader(method.getResponseBodyAsStream()));
        if (stream != null) {
            logger.debug("Stream content size: " + stream.length());
            parser = Parser.createParser(stream, null);
            visitor = new HTTPVisitor(url, loginUrl);
            parser.visitAllNodesWith(visitor);
            PrintWriter out = response.getWriter();
            if (out != null) {
                out.flush();
                out.print(((HTTPVisitor) visitor).getModifiedHTML());
                out.close();
                logger.debug("Wrote: " + ((HTTPVisitor) visitor).getModifiedHTML().length());
            }
            response.setHeader("Content-Type", contentType);
            stream = null;
            parser = null;
            visitor = null;
        }
    }

    /**
     * Includes the HTML document in the response
     * 
     * @param response HTTP response
     * @param method HTTP method
     * @param url document url
     * @param loginUrl login url
     * 
     * @throws IOException
     */
    public static void returnHTML(HttpServletResponse response, HttpMethodBase method, String url, String loginUrl) throws IOException {
        returnHTML(response, method, url, loginUrl, "text/html");
    }

    /**
     * Includes the HTML document in the response
     * 
     * @param response HTTP response
     * @param method HTTP method
     * @param url document url
     * @param loginUrl login url
     * @param contentType content type
     * 
     * @throws IOException
     */
    public static void returnHTML(HttpServletResponse response, HttpMethodBase method, String url, String loginUrl, String contentType) throws IOException {
        logger.debug("Returning an HTML document");
        PrintWriter out = null;
        try {
            out = response.getWriter();
            if (out != null) {
                out.print(method.getResponseBodyAsString());
                out.close();
                response.setHeader("Content-Type", contentType);
            }
        } catch (Exception e) {
            logger.error("Error when returning HTML content: " + e.getMessage(), e);
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * If the document is non HTML, this method processes its content in order to 
     * rewrite the URLs it includes
     * 
     * @param response
     * @param method
     * @throws IOException
     */
    public static void processNonHTML(HttpServletResponse response, HttpMethodBase method) throws IOException {
        logger.debug("Processing a non HTML document");
        InputStream is = new BufferedInputStream(method.getResponseBodyAsStream());
        OutputStream os = response.getOutputStream();
        byte[] buffer = new byte[BUFFER_BLOCK_SIZE];
        int read = is.read(buffer);
        while (read >= 0) {
            if (read > 0) {
                os.write(buffer, 0, read);
            }
            read = is.read(buffer);
        }
        buffer = null;
        is.close();
        os.close();
    }

    /**
     * Reads the file in blocks for non-HTML documents
     * 
     * @param input the input reader
     * 
     * @return the content block
     * 
     * @throws IOException
     */
    public static String readFully(Reader input) throws IOException {
        String resultStr = null;
        BufferedReader bufferedReader = input instanceof BufferedReader ? (BufferedReader) input : new BufferedReader(input);
        StringBuffer result = new StringBuffer();
        char[] buffer = new char[BUFFER_BLOCK_SIZE];
        int charsRead;
        while ((charsRead = bufferedReader.read(buffer)) != -1) {
            result.append(buffer, 0, charsRead);
        }
        resultStr = result.toString();
        bufferedReader = null;
        result = null;
        buffer = null;
        return resultStr;
    }
}
