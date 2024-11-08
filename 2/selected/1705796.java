package org.opennms.netmgt.xmlrpcd;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.xmlrpc.util.HttpUtil;
import org.apache.xmlrpc.DefaultXmlRpcTransport;

/**
 * TimeoutSecureXmlRpcTransport adds a read timeout to the url connection.
 */
public class TimeoutSecureXmlRpcTransport extends DefaultXmlRpcTransport {

    protected int timeout = 0;

    /**
     * Create a new TimeoutSecureXmlRpcTransport with the specified URL and 
     * basic authorization string.
     *
     * @deprecated Use setBasicAuthentication instead of passing an encoded authentication String.
     *
     * @param url the url to POST XML-RPC requests to.
     * @param auth the Base64 encoded HTTP Basic authentication value.
     */
    public TimeoutSecureXmlRpcTransport(URL url, String auth, int timeout) {
        super(url, auth);
        this.timeout = timeout;
    }

    /**
     * Create a new DefaultXmlRpcTransport with the specified URL.
     *
     * @param url the url to POST XML-RPC requests to.
     */
    public TimeoutSecureXmlRpcTransport(URL url, int timeout) {
        super(url, null);
        this.timeout = timeout;
    }

    /**
     * Sends the actual XMLRPC request. 
     *
     * Taken from org.apache.xmlrpc.DefaultXmlRpcTransport with a socket
     * timeout added.
     */
    public InputStream sendXmlRpc(byte[] request) throws IOException {
        con = url.openConnection();
        System.err.println("Setting read timeout to 60000");
        con.setReadTimeout(timeout);
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setAllowUserInteraction(false);
        con.setRequestProperty("Content-Length", Integer.toString(request.length));
        con.setRequestProperty("Content-Type", "text/xml");
        if (auth != null) {
            con.setRequestProperty("Authorization", "Basic " + auth);
        }
        OutputStream out = con.getOutputStream();
        out.write(request);
        out.flush();
        out.close();
        return con.getInputStream();
    }
}
