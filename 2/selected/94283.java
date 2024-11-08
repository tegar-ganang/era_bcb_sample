package pspdash;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.Map;
import java.util.ResourceBundle;
import org.w3c.dom.*;

public class CachedURLObject extends CachedObject {

    public static final String PASSWORD_MISSING = "Password_Missing";

    public static final String PASSWORD_INCORRECT = "Password_Incorrect";

    public static final String NOT_FOUND = "Not_Found";

    public static final String COULD_NOT_RETRIEVE = "Could_Not_Retrieve";

    public static final String NO_SUCH_HOST = "No_Such_Host";

    public static final String COULD_NOT_CONNECT = "Could_Not_Connect";

    public static final String OWNER_HEADER_FIELD = "Dash-Owner-Name";

    public static final String OWNER_ATTR = "Owner";

    protected URL url;

    protected String credential;

    public CachedURLObject(ObjectCache c, String type, URL u) {
        this(c, type, u, null, null);
    }

    public CachedURLObject(ObjectCache c, String type, URL u, String username, String password) {
        super(c, type);
        this.url = u;
        if (username != null && password != null) credential = TinyWebServer.calcCredential(username, password); else credential = null;
        refresh();
    }

    /** Deserialize a cached URL object from an XML stream. */
    public CachedURLObject(ObjectCache c, int id, Element xml, CachedDataProvider dataProvider) {
        super(c, id, xml, dataProvider);
        Element e = (Element) xml.getElementsByTagName("url").item(0);
        try {
            url = new URL(e.getAttribute("href"));
        } catch (MalformedURLException mue) {
            throw new IllegalArgumentException("Malformed or missing URL");
        }
        credential = e.getAttribute("credential");
        if (!XMLUtils.hasValue(credential)) credential = null;
    }

    /** Serialize information to XML */
    public void getXMLContent(StringBuffer buf) {
        buf.append("  <url href='").append(XMLUtils.escapeAttribute(url.toString()));
        if (credential != null) buf.append("' credential='").append(XMLUtils.escapeAttribute(credential));
        buf.append("'/>\n");
    }

    public boolean refresh() {
        try {
            URLConnection conn = url.openConnection();
            conn.setUseCaches(false);
            if (credential != null) conn.setRequestProperty("Authorization", credential);
            conn.connect();
            int status = ((HttpURLConnection) conn).getResponseCode();
            if (status == 401 || status == 403) errorMessage = (credential == null ? PASSWORD_MISSING : PASSWORD_INCORRECT); else if (status == 404) errorMessage = NOT_FOUND; else if (status != 200) errorMessage = COULD_NOT_RETRIEVE; else {
                InputStream in = conn.getInputStream();
                byte[] httpData = TinyWebServer.slurpContents(in, true);
                synchronized (this) {
                    data = httpData;
                    dataProvider = null;
                }
                errorMessage = null;
                refreshDate = new Date();
                String owner = conn.getHeaderField(OWNER_HEADER_FIELD);
                if (owner != null) setLocalAttr(OWNER_ATTR, owner);
                store();
                return true;
            }
        } catch (UnknownHostException uhe) {
            errorMessage = NO_SUCH_HOST;
        } catch (ConnectException ce) {
            errorMessage = COULD_NOT_CONNECT;
        } catch (IOException ioe) {
            errorMessage = COULD_NOT_RETRIEVE;
        }
        return false;
    }

    public boolean refresh(double maxAge, long maxWait) {
        if (!olderThanAge(maxAge)) return true;
        switch(Ping.ping(url.getHost(), url.getPort(), maxWait)) {
            case Ping.HOST_NOT_FOUND:
                errorMessage = NO_SUCH_HOST;
                return false;
            case Ping.CANNOT_CONNECT:
                errorMessage = COULD_NOT_CONNECT;
                return false;
            case Ping.SUCCESS:
            default:
                return refresh();
        }
    }

    private static Resources RESOURCES = null;

    public static String translateMessage(ResourceBundle resources, String prefix, String errorKey) {
        String resourceKey = prefix + errorKey;
        String result = resources.getString(resourceKey);
        if (result != null) return result;
        if (RESOURCES == null) RESOURCES = Resources.getDashBundle("pspdash.CachedURLObject");
        result = RESOURCES.getString(errorKey);
        return result == null ? errorKey : result;
    }
}
