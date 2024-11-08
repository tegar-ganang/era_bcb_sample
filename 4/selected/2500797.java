package com.appspot.fabiojmor.thin.httpclient;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Map;

/**
 * @author Fabio Jose de Moraes - fabiojose@gmail.com
 */
public class ThinHttpClient implements Serializable {

    private static final long serialVersionUID = -311763168697324769L;

    private static final String CR_LF = "\r\n";

    private URL destiny;

    public ThinHttpClient() {
    }

    public ThinHttpClient(String url) throws MalformedURLException {
        destiny = new URL(url);
    }

    public ThinHttpClient(URL url) {
        setURL(url);
    }

    private void putCommonsHeaders(Request request) {
        request.addHeader("User-Agent", "(Realy) ThinHttpClient/0.1 Developer/FabioMoraes Source/Open Java/" + System.getProperty("java.version") + " OS/" + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        request.addHeader("Cache-Control", "no-cache");
    }

    private String createBoundary() {
        return "49B70F5AD5E7A90".toLowerCase();
    }

    private void buildHeader(Request request, URLConnection conn) {
        for (String header : request.getHeaders()) conn.setRequestProperty(header, request.getHeader(header));
    }

    private String getMimeType(String resource) {
        if (resource.toLowerCase().endsWith("txt")) return "text/plain"; else if (resource.toLowerCase().endsWith("html")) return "text/html";
        return "application/octet-stream";
    }

    public Response executePost(Request request) throws IOException {
        Response response = new Response();
        URLConnection conn = destiny.openConnection();
        conn.setDoOutput(true);
        DataOutputStream out = null;
        BufferedReader reader = null;
        try {
            putCommonsHeaders(request);
            if (0 < request.getBody().getParameters().size()) {
                request.addHeader("Content-Type", "application/x-www-form-urlencoded");
            }
            String boundary = createBoundary();
            if (0 < request.getBody().getBinaryParameters().size()) {
                request.addHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
            }
            buildHeader(request, conn);
            out = new DataOutputStream(conn.getOutputStream());
            if (request.getHeader("Content-Type").startsWith("multipart/form-data")) {
                for (RequestParameter binParam : request.getBody().getBinaryParameters()) {
                    DataInputStream input = null;
                    String filename = null, filetype = null;
                    if (binParam.getValue() instanceof java.io.File) {
                        input = new DataInputStream(new FileInputStream((File) binParam.getValue()));
                        filename = ((File) binParam.getValue()).getName();
                        filetype = getMimeType(filename);
                    } else if (binParam.getValue() instanceof java.io.InputStream) {
                        input = new DataInputStream((java.io.InputStream) binParam.getValue());
                        filename = binParam.getName();
                        filetype = getMimeType(filename);
                    } else throw new InvalidParameterException("A implementacao atual somente suporta parametro binario do tipo File.");
                    out.writeBytes("--" + boundary + CR_LF);
                    out.writeBytes("Content-Disposition: form-data; name=\"" + URLEncoder.encode(binParam.getName(), "iso-8859-1") + "\"; filename=\"" + URLEncoder.encode(filename, "iso-8859-1") + "\"" + CR_LF);
                    out.writeBytes("Content-Type: " + filetype + CR_LF);
                    out.writeBytes("Content-Transfer-Encoding: binary" + CR_LF + CR_LF);
                    try {
                        byte[] buffer = new byte[2033];
                        int readed;
                        while (0 < (readed = input.read(buffer))) {
                            out.write(buffer, 0, readed);
                        }
                        out.writeBytes(CR_LF);
                    } finally {
                        input.close();
                    }
                }
                for (RequestParameter param : request.getBody().getParameters()) {
                    out.writeBytes("--" + boundary + CR_LF);
                    out.writeBytes("Content-Disposition: form-data; name=\"" + URLEncoder.encode(param.getName(), "iso-8859-1") + "\"" + CR_LF + CR_LF);
                    out.writeBytes(param.getValue().toString() + CR_LF);
                }
                if (0 < request.getBody().getParameters().size() || 0 < request.getBody().getBinaryParameters().size()) out.writeBytes("--" + boundary + "--" + CR_LF);
            }
            out.writeBytes(CR_LF);
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            String plainResponse = "";
            while (null != (line = reader.readLine())) plainResponse += line;
            response.setContent(plainResponse);
            response.setContentType(conn.getContentType());
            Map<String, List<String>> headers = conn.getHeaderFields();
            Response.treat(((List<String>) headers.get(null)).get(0), response);
        } catch (IOException e) {
            Response.treat(conn.getHeaderField(null), response);
            if (response.getStatus() == 200) throw e;
        } finally {
            if (null != out) out.close();
            if (null != reader) reader.close();
        }
        return response;
    }

    public Response executeGet(Request request) throws IOException {
        throw new UnsupportedOperationException("O00o. (+ +) .o00O - oops! metodo nao implementado ainda!");
    }

    public void setURL(URL url) {
        destiny = url;
    }

    public URL getURL() {
        return destiny;
    }
}
