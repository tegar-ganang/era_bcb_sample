package org.owasp.oss.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

/**
 * This class is used to build and send the http-response
 */
public class HttpResponse {

    private HttpExchange _exchange;

    private Headers _headers;

    private int _status = 200;

    private List<String> _cookieList = null;

    public static enum MimeType {

        HTML, CSS, TEXT
    }

    public static enum ErrorType {

        FORBIDDEN, SERVICE_UNAVAILABLE
    }

    public static HttpResponse create(HttpExchange exchange) {
        return new HttpResponse(exchange);
    }

    public static void sendErrorPage(ErrorType errorType, HttpExchange exchange) throws IOException {
        String errorPage = null;
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Content-Type", "text/html");
        if (errorType == ErrorType.FORBIDDEN) {
            errorPage = "<head><title>Error</title></head><body><p><h1>Forbidden</h1></p><hr></body></html>";
            exchange.sendResponseHeaders(403, errorPage.length());
        } else if (errorType == ErrorType.SERVICE_UNAVAILABLE) {
            errorPage = "<head><title>Error</title></head><body><p><h1>Service Unavailable</h1></p><hr></body></html>";
            exchange.sendResponseHeaders(503, errorPage.length());
        } else {
            errorPage = "<head><title>Error</title></head><body><p><h1> Internal Server Error</h1></p><hr></body></html>";
            exchange.sendResponseHeaders(500, errorPage.length());
        }
        exchange.getResponseBody().write(errorPage.getBytes());
        exchange.getResponseBody().close();
    }

    public static void sendDebugPage(HttpExchange exchange) throws IOException {
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(200, 0);
        OutputStream responseBody = exchange.getResponseBody();
        Headers requestHeaders = exchange.getRequestHeaders();
        Set<String> keySet = requestHeaders.keySet();
        Iterator<String> iter = keySet.iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            List values = requestHeaders.get(key);
            String s = key + " = " + values.toString() + "\n";
            responseBody.write(s.getBytes());
        }
        Map<String, Object> as = exchange.getHttpContext().getAttributes();
        keySet = as.keySet();
        iter = keySet.iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            String s = key + " = " + as.get(key) + "\n";
            responseBody.write(s.getBytes());
        }
        responseBody.write("HttpPrincipal:\n".getBytes());
        HttpPrincipal principal = exchange.getPrincipal();
        if (principal != null) responseBody.write(principal.toString().getBytes());
        responseBody.write("Body:\n".getBytes());
        InputStream body = exchange.getRequestBody();
        while (body.available() > 0) responseBody.write(body.read());
        responseBody.close();
    }

    private HttpResponse(HttpExchange exchange) {
        _headers = exchange.getResponseHeaders();
        _exchange = exchange;
        _cookieList = new Vector<String>();
    }

    private void setMimeType(MimeType type) {
        if (type == MimeType.HTML) _headers.add("Content-Type", "text/html"); else if (type == MimeType.CSS) _headers.add("Content-Type", "text/css"); else if (type == MimeType.TEXT) _headers.add("Content-Type", "text/plain");
    }

    /**
	 * This method takes a name and value, which will be placed in the header as
	 * part of the cookie
	 * 
	 * @param nameValue
	 *            name and value in form of: "name=value"
	 */
    public void setCookie(String nameValue) {
        _headers.set("Set-cookie", nameValue);
    }

    public void send(InputStream body, MimeType type) throws IOException {
        setMimeType(type);
        long bodyLength = body.available();
        _exchange.sendResponseHeaders(_status, bodyLength);
        OutputStream bodyStream = _exchange.getResponseBody();
        while (body.available() > 0) bodyStream.write(body.read());
        bodyStream.close();
    }
}
