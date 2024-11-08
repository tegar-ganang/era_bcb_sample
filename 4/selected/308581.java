package org.owasp.oss.httpserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

/**
 * This class parses an http request and exposes the values of interest
 */
public class HttpRequest {

    private static Logger log = Logger.getLogger(HttpRequest.class);

    private static enum Method {

        GET, POST, PUT, DELETE
    }

    private Method _method = null;

    private String _path = null;

    private byte[] _bodyBytes = null;

    Map<String, String> _parameters = null;

    HttpPrincipal _principal = null;

    private HttpRequest(Method method, String path, byte[] bodyBytes, Map<String, String> parameters, HttpPrincipal principal) {
        _method = method;
        _path = path;
        _bodyBytes = bodyBytes;
        _parameters = parameters;
        _principal = principal;
    }

    public static HttpRequest create(HttpExchange exchange) throws org.owasp.oss.httpserver.HttpHandlerException, IOException {
        Method method;
        log.info("Creating HttpRequest object, with following properties:");
        if (exchange.getRequestMethod().equals("GET")) method = Method.GET; else if (exchange.getRequestMethod().equals("POST")) method = Method.POST; else if (exchange.getRequestMethod().equals("PUT")) method = Method.PUT; else if (exchange.getRequestMethod().equals("DELETE")) method = Method.DELETE; else throw new org.owasp.oss.httpserver.HttpHandlerException("Could not create HttpRequest, no vaild method");
        log.info("\t Method: " + method);
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        log.info("\t Path: " + path);
        Headers headers = exchange.getRequestHeaders();
        Map<String, String> parameters = new HashMap<String, String>();
        String cookieStr = headers.getFirst("Cookie");
        if (cookieStr != null && cookieStr.length() > 0) HttpRequest.parseCookiesParameters(cookieStr, parameters);
        InputStream body = exchange.getRequestBody();
        String bodyLenStr = headers.getFirst("Content-Length");
        log.info("\t Body length: " + bodyLenStr);
        byte[] bodyBytes = null;
        if (bodyLenStr != null) {
            int bodyLen = Integer.parseInt(bodyLenStr);
            bodyBytes = new byte[bodyLen];
            ByteArrayOutputStream os = new ByteArrayOutputStream(bodyLen);
            int readByteNum = 0;
            while ((readByteNum = body.read(bodyBytes)) > 0) {
                os.write(bodyBytes, 0, readByteNum);
            }
            bodyBytes = os.toByteArray();
            if (bodyBytes != null) HttpRequest.parsePostParameters(new ByteArrayInputStream(bodyBytes), bodyBytes.length, parameters);
        }
        return new HttpRequest(method, path, bodyBytes, parameters, exchange.getPrincipal());
    }

    private static void parseCookiesParameters(String cookieStr, Map<String, String> parameters) throws HttpHandlerException, IOException {
        StringTokenizer st = new StringTokenizer(cookieStr, ";");
        while (st.hasMoreElements()) {
            String current = st.nextToken();
            int index = current.indexOf("=");
            String key = current.substring(0, index).replaceAll(" ", "");
            String value = current.substring(index + 1);
            parameters.put(key, value);
        }
    }

    private static void parsePostParameters(InputStream body, int bodyLength, Map<String, String> parameters) throws HttpHandlerException, IOException {
        try {
            byte[] bodyBytes = new byte[bodyLength];
            body.read(bodyBytes);
            String bodyStr = new String(bodyBytes);
            bodyStr = bodyStr.replace("+", " ");
            StringTokenizer st = new StringTokenizer(bodyStr, "&");
            while (st.hasMoreElements()) {
                String current = st.nextToken();
                int index = current.indexOf("=");
                if (index > 0) {
                    String key = URLDecoder.decode(current.substring(0, index), "UTF-8");
                    String value = URLDecoder.decode(current.substring(index + 1), "UTF-8");
                    log.info("Parameter: " + key + " = " + value);
                    parameters.put(key, value);
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new HttpHandlerException(e);
        }
    }

    public HttpPrincipal getHttpPrincipal() {
        return _principal;
    }

    public String getParameterValue(String key) {
        return _parameters.get(key);
    }

    public byte[] getBodyBytes() {
        return _bodyBytes;
    }

    public String getPath() {
        return _path;
    }

    public boolean isPathEmpty() {
        return (_path.length() <= 1);
    }

    public boolean isGET() {
        return (_method == Method.GET);
    }

    public boolean isPOST() {
        return (_method == Method.POST);
    }

    public boolean isPUT() {
        return (_method == Method.PUT);
    }

    public boolean isDELETE() {
        return (_method == Method.DELETE);
    }
}
