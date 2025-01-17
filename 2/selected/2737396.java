package org.opensocial.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * An internal object which represents a single HTTP request and contains a
 * minimal set of methods for setting request properties, submitting the
 * request, and retrieving the response which can later be parsed into a
 * more meaningful object.
 *
 * @author Jason Cooper
 */
public class OpenSocialHttpRequest {

    private String method;

    private String body;

    private OpenSocialUrl url;

    private HttpURLConnection connection;

    public OpenSocialHttpRequest(String method, OpenSocialUrl url) throws IOException {
        this.method = method;
        this.body = null;
        this.connection = null;
        this.setUrl(url);
    }

    /**
   * Initializes connection if necessary before establishing the connection,
   * including writing POST data to the connection's output stream if
   * applicable.
   * 
   * @return HTTP status code of request, e.g. 200, 401, 500, etc.
   * @throws IOException
   */
    public String execute() throws IOException {
        if (this.connection == null) {
            this.connection = this.getConnection();
        }
        this.connection.setRequestMethod(this.method);
        if (this.body == null) {
            this.connection.connect();
        } else {
            this.connection.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(this.connection.getOutputStream());
            wr.write(this.body);
            wr.flush();
            wr.close();
        }
        return this.getResponseString();
    }

    /**
   * After executing the request, returns the server response as an InputStream
   * object.
   * 
   * @throws IOException
   */
    public InputStream getResponseStream() throws IOException {
        return this.connection.getInputStream();
    }

    /**
   * After executing the request, transforms response output contained in the
   * connection's InputStream object into a string representation which can
   * later be parsed into a more meaningful object, e.g. OpenSocialObject. 
   *
   * @throws IOException if the open connection's input stream is not
   *         retrievable or accessible
   */
    public String getResponseString() throws IOException {
        InputStream responseStream = this.getResponseStream();
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        responseStream.close();
        return sb.toString();
    }

    /**
   * Returns instance variable: method.
   */
    public String getMethod() {
        return this.method;
    }

    /**
   * Returns instance variable: body.
   */
    public String getBody() {
        return this.body;
    }

    /**
   * Returns instance variable: url.
   */
    public OpenSocialUrl getUrl() {
        return this.url;
    }

    /**
   * Sets instance variable method to passed String.
   */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
   * Sets instance variable body to passed String and automatically sets
   * request method to POST.
   */
    public void setBody(String body) {
        this.body = body;
    }

    /**
   * Sets instance variable url to passed OpenSocialUrl object.
   * 
   * @param requestUrl
   * @throws IOException
   */
    private void setUrl(OpenSocialUrl requestUrl) throws IOException {
        this.url = requestUrl;
    }

    /**
   * Opens a new HTTP connection for the URL associated with this object.
   * 
   * @return Opened connection
   * @throws IOException if URL protocol is not http
   */
    private HttpURLConnection getConnection() throws IOException {
        URL url = new URL(this.url.toString());
        if (!url.getProtocol().startsWith("http")) {
            throw new IOException("Unsupported scheme:" + url.getProtocol());
        }
        return ((HttpURLConnection) url.openConnection());
    }
}
