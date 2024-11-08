package com.volantis.testtools.server;

import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.InputStream;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:phil.weighill-smith@volantis.com">Phil W-S</a>
 * 
 * @deprecated Do not make modifications as the testcase has been defuncted as 
 *  it was not reliable. Use testurl instead. 
 */
public class HTTPResourceServer extends HTTPServer {

    public HTTPResourceServer() {
        this(8088);
    }

    public HTTPResourceServer(int port) {
        this(port, 0);
    }

    public HTTPResourceServer(int port, int maxConnections) {
        super(port, maxConnections);
    }

    protected void handleConnection(Socket server) throws IOException {
        OutputStream out = server.getOutputStream();
        PrintWriter pout = new PrintWriter(out, true);
        BufferedReader in = SocketUtil.getReader(server);
        String failureReason = null;
        int failureCode = 0;
        String httpVersion = "HTTP/1.0";
        String uri = null;
        String command = in.readLine();
        URL url = null;
        if (command != null) {
            StringTokenizer tokenizer = new StringTokenizer(command);
            if (tokenizer.countTokens() != 3) {
                failureCode = 400;
                failureReason = "Illformed Request-Line";
            } else {
                String method = tokenizer.nextToken();
                if (!method.equalsIgnoreCase("get")) {
                    failureCode = 501;
                    failureReason = "Only supports GET method";
                } else {
                    uri = tokenizer.nextToken();
                    httpVersion = tokenizer.nextToken();
                    try {
                        url = getURL(uri);
                    } catch (IOException e) {
                        failureCode = 404;
                        failureReason = "resource not found";
                    }
                }
            }
        } else {
            failureCode = 400;
            failureReason = "Null request";
        }
        if (url != null) {
            InputStream stream = null;
            try {
                URLConnection connection = url.openConnection();
                byte[] chunk = new byte[1024];
                int read = 0;
                pout.println(httpVersion + " 200 ");
                pout.println("Content-Type: " + connection.getContentType());
                pout.println("Content-Length: " + connection.getContentLength());
                pout.println("Content-Encoding: " + connection.getContentEncoding());
                pout.println();
                stream = connection.getInputStream();
                read = stream.read(chunk);
                while (read != -1) {
                    out.write(chunk, 0, read);
                    read = stream.read(chunk);
                }
            } catch (IOException e) {
                failureCode = 500;
                failureReason = "problem reading the resource content";
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        } else {
            failureCode = 404;
            failureReason = "resource not found";
        }
        if (failureCode != 0) {
            pout.println(httpVersion + " " + failureCode + " " + failureReason);
            pout.println();
        }
        doDelay();
        server.close();
    }

    protected URL getURL(String uri) throws IOException {
        URL url = this.getClass().getResource(uri);
        return url;
    }
}
