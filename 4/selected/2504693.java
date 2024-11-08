package com.totalchange.wtframework.basichttpserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles a request for a resource on this server. The {@link #handleRequest()}
 * method sees what the client is requesting and resolves the request for the
 * particular resource against the root url. So does what a http server does
 * which is handle GET and POST requests.
 * 
 * @author Ralph Jones
 */
class HttpRequest {

    private static final String HTTP_TYPE = "HTTP/1.0";

    private static final String EOL = "\r\n";

    private static final String SERVER_TITLE = "Web Test Fun Basic HTTP Server";

    private static final Logger logger = LoggerFactory.getLogger(HttpRequest.class);

    private BufferedReader in;

    private PrintStream out;

    private URL root;

    private String requestedResource;

    private URL resource;

    /**
     * Constructs a new request handler.
     * 
     * @param in
     *            the request from the client
     * @param out
     *            the response to the client
     * @param root
     *            where to resolve requests against
     */
    HttpRequest(InputStream in, OutputStream out, URL root) {
        this.in = new BufferedReader(new InputStreamReader(in));
        this.out = new PrintStream(out);
        this.root = root;
    }

    /**
     * Gets an input stream to a resource.
     * 
     * @param resourceName
     * @return
     */
    private void findResource(String resourceName) {
        requestedResource = resourceName;
        int spaceChar = requestedResource.indexOf(' ');
        if (spaceChar > -1) {
            requestedResource = requestedResource.substring(0, spaceChar);
        }
        if (requestedResource.startsWith("/")) {
            requestedResource = requestedResource.substring(1);
        }
        try {
            resource = new URL(root, requestedResource);
            URLConnection connection = resource.openConnection();
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                if (httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Not OK HTTP response - " + httpConnection.getResponseCode() + " - " + httpConnection.getResponseMessage());
                }
            }
            resource.openStream();
            logger.debug("Resolved requested resource {} to {}", requestedResource, resource);
        } catch (MalformedURLException urlEx) {
            logger.warn("Requested resource " + requestedResource + " invalid (from " + resourceName + ")", urlEx);
        } catch (IOException ioEx) {
            resource = null;
            logger.warn("Couldn't open a connection to " + requestedResource + " (from " + resourceName + ")", ioEx);
        }
    }

    private void printHeaders() throws IOException {
        if (resource != null) {
            out.print(HTTP_TYPE + " " + HttpURLConnection.HTTP_OK + " OK" + EOL);
        } else {
            out.print(HTTP_TYPE + " " + HttpURLConnection.HTTP_NOT_FOUND + " OK" + EOL);
        }
        out.print("Server: " + SERVER_TITLE + EOL);
        out.print("Date: " + new Date() + EOL);
        if (resource == null) {
            resource = HttpRequest.class.getResource("404.html");
        }
        URLConnection connection = resource.openConnection();
        out.print("Content-length: " + connection.getContentLength() + EOL);
        out.print("Last Modified: " + new Date(connection.getLastModified()) + EOL);
        out.print("Content-type: " + MimeLookup.getInstance().getMimeType(resource.getFile()) + EOL);
    }

    private void sendResource() throws IOException {
        out.print(EOL);
        out.print(EOL);
        InputStream in = resource.openStream();
        byte[] buffer = new byte[4 * 1024];
        int read = -1;
        while ((read = in.read(buffer)) > -1) {
            out.write(buffer, 0, read);
        }
    }

    /**
     * See what the client wants and return it to them. Only GET and HEAD
     * requests at the mo.
     * 
     * @throws IOException
     *             by the client request/response
     */
    void handleRequest() throws IOException {
        String request = in.readLine();
        if (request.startsWith("GET")) {
            findResource(request.substring(4));
            printHeaders();
            sendResource();
        } else if (request.startsWith("HEAD")) {
            findResource(request.substring(5));
            printHeaders();
        } else {
            out.print(HTTP_TYPE + " " + HttpURLConnection.HTTP_BAD_METHOD);
            out.print(" Unsupported method type: " + request);
            out.print(EOL);
        }
        out.flush();
    }
}
