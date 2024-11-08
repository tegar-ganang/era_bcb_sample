package com.aol.services.licensing;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.log4j.Logger;

public class HTTPClient {

    private String urlString = null;

    private URL url = null;

    private String authorization = null;

    private static Logger logger = Logger.getLogger(HTTPClient.class);

    public HTTPClient() {
    }

    public HTTPClient(URL u, String auth) throws Exception {
        if (u == null) throw new Exception("Invalid URL");
        url = u;
        authorization = auth;
    }

    public HTTPClient(String _url, String auth) throws MalformedURLException {
        url = new URL(_url);
        authorization = auth;
    }

    public String get(final String _url) throws Exception {
        url = new URL(_url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(false);
        if (authorization != null) connection.setRequestProperty("Authorization", authorization);
        int responseLen = connection.getContentLength();
        InputStream ins = null;
        try {
            ins = connection.getInputStream();
        } catch (IOException ioe) {
            if (ins != null) ins.close();
            StringBuffer _err = new StringBuffer();
            StackTraceElement _st[] = ioe.getStackTrace();
            _err.append("HTTPClient: ").append(url.toString()).append(" Response code ").append(connection.getResponseCode()).append(" ").append(connection.getResponseMessage()).append("\n");
            for (int _i = 0; _i < 10 && _i < _st.length; _i++) _err.append(_st[_i]).append("\n");
            logger.error(_err.toString());
            return null;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(ins));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) response.append(inputLine);
        in.close();
        return response.toString();
    }

    public String post(final String payload) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (authorization != null) connection.setRequestProperty("Authorization", authorization);
        connection.setDoOutput(true);
        OutputStream outs = null;
        try {
            outs = connection.getOutputStream();
        } catch (IOException ioe) {
            if (outs != null) outs.close();
            throw ioe;
        }
        PrintWriter out = new PrintWriter(outs);
        out.print(payload);
        out.close();
        int responseLen = connection.getContentLength();
        InputStream ins = null;
        try {
            ins = connection.getInputStream();
        } catch (IOException ioe) {
            if (ins != null) ins.close();
            throw ioe;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(ins));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) response.append(inputLine);
        in.close();
        return response.toString();
    }
}
