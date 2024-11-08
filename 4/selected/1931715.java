package ao.dd.web.alexo;

import ao.util.serial.Cloner;
import ao.util.serial.Stringer;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CookieManagerImpl implements CookieManager {

    private static final String SET_COOKIE = "Set-Cookie";

    private static final String COOKIE_VALUE_DELIMITER = ";";

    private static final String PATH = "path";

    private static final String EXPIRES = "expires";

    private static final String SET_COOKIE_SEPARATOR = "; ";

    private static final String COOKIE = "Cookie";

    private static final char NAME_VALUE_SEPARATOR = '=';

    private static final char DOT = '.';

    private static final String DATE_FORMAT_STRING_A = "EEE, dd-MMM-yyyy hh:mm:ss z";

    private static final String DATE_FORMAT_STRING_B = "EEE, dd MMM yyyy hh:mm:ss z";

    private static final DateFormat DATE_FORMAT_A = new SimpleDateFormat(DATE_FORMAT_STRING_A);

    private static final DateFormat DATE_FORMAT_B = new SimpleDateFormat(DATE_FORMAT_STRING_B);

    private final Map<String, Map<String, Map<String, String>>> STORE;

    public CookieManagerImpl() {
        STORE = new HashMap<String, Map<String, Map<String, String>>>();
    }

    public Map<String, Map<String, String>> readCookies(URLConnection conn) {
        Map<String, Map<String, String>> domainStore = retrieveOrCreateDomainStore(conn.getURL());
        String headerName;
        for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
            if (headerName.equalsIgnoreCase(SET_COOKIE)) {
                Map<String, String> cookie = new HashMap<String, String>();
                StringTokenizer st = new StringTokenizer(conn.getHeaderField(i), COOKIE_VALUE_DELIMITER);
                if (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    String name = token.substring(0, token.indexOf(NAME_VALUE_SEPARATOR));
                    String value = token.substring(token.indexOf(NAME_VALUE_SEPARATOR) + 1, token.length());
                    domainStore.put(name, cookie);
                    cookie.put(name, value);
                }
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    cookie.put(token.substring(0, token.indexOf(NAME_VALUE_SEPARATOR)).toLowerCase(), token.substring(token.indexOf(NAME_VALUE_SEPARATOR) + 1, token.length()));
                }
            }
        }
        return Cloner.clone(domainStore);
    }

    public Map<String, Map<String, String>> getCookies(URL url) {
        return Cloner.clone(retrieveOrCreateDomainStore(url));
    }

    public String getCookie(URL url, String name, String key) {
        return retrieveOrCreateCookies(url, name).get(key);
    }

    /**
     * Prior to opening a URLConnection, calling this method will set all
     * unexpired cookies that match the path or subpaths for this
     *  underlying URL
     * <p/>
     * The connection MUST NOT have been opened
     * method or an IOException will be thrown.
     *
     * @param conn a java.net.URLConnection - must NOT be open, or
     *  IOException will be thrown
     * //@throws java.io.IOException Thrown if <i>conn</i> has already been opened.
     */
    public void writeCookies(URLConnection conn) {
        writeCookies(conn, null);
    }

    public void writeCookies(URLConnection conn, Map<String, Map<String, String>> cookies) {
        URL url = conn.getURL();
        String domain = getDomainFromUrl(url);
        String path = url.getPath();
        Map<String, Map<String, String>> domainStore = (cookies != null ? Cloner.clone(cookies) : STORE.get(domain));
        if (domainStore == null) return;
        StringBuffer cookieStringBuffer = new StringBuffer();
        Iterator<String> cookieNames = domainStore.keySet().iterator();
        while (cookieNames.hasNext()) {
            String cookieName = cookieNames.next();
            Map<String, String> cookie = domainStore.get(cookieName);
            if (comparePaths(cookie.get(PATH), path) && isNotExpired(cookie.get(EXPIRES))) {
                cookieStringBuffer.append(cookieName);
                cookieStringBuffer.append("=");
                cookieStringBuffer.append(cookie.get(cookieName));
                if (cookieNames.hasNext()) {
                    cookieStringBuffer.append(SET_COOKIE_SEPARATOR);
                }
            }
        }
        try {
            conn.setRequestProperty(COOKIE, cookieStringBuffer.toString());
        } catch (java.lang.IllegalStateException ise) {
            throw new Error("Illegal State! Cookies cannot be set on a " + "URLConnection that is already connected. Only " + "call writeCookies(java.net.URLConnection) " + "AFTER calling java.net.URLConnection.connect().");
        }
    }

    public void setCookies(URL url, Map<String, Map<String, String>> cookies) {
        STORE.put(getDomainFromHost(url.getHost()), Cloner.clone(cookies));
    }

    public void setCookie(URL url, String name, String key, String value) {
        retrieveOrCreateCookies(url, name).put(key, value);
    }

    private Map<String, Map<String, String>> retrieveOrCreateDomainStore(URL url) {
        return retrieveOrCreateDomainStore(getDomainFromHost(url.getHost()));
    }

    private Map<String, Map<String, String>> retrieveOrCreateDomainStore(String domain) {
        Map<String, Map<String, String>> domainStore = STORE.get(domain);
        if (domainStore == null) {
            domainStore = new HashMap<String, Map<String, String>>();
            STORE.put(domain, domainStore);
        }
        return domainStore;
    }

    private Map<String, String> retrieveOrCreateCookies(URL url, String name) {
        String domain = getDomainFromHost(url.getHost());
        Map<String, Map<String, String>> domainCookies = retrieveOrCreateDomainStore(domain);
        Map<String, String> cookies = domainCookies.get(name);
        if (cookies == null) {
            cookies = new HashMap<String, String>();
            domainCookies.put(domain, cookies);
        }
        return cookies;
    }

    private String getDomainFromUrl(URL url) {
        return getDomainFromHost(url.getHost());
    }

    private String getDomainFromHost(String host) {
        if (host.indexOf(DOT) != host.lastIndexOf(DOT)) {
            return host.substring(host.indexOf(DOT) + 1);
        } else {
            return host;
        }
    }

    private boolean isNotExpired(String cookieExpires) {
        if (cookieExpires == null) return true;
        Date expiry;
        try {
            expiry = DATE_FORMAT_A.parse(cookieExpires);
        } catch (java.text.ParseException peA) {
            try {
                expiry = DATE_FORMAT_B.parse(cookieExpires);
            } catch (java.text.ParseException peB) {
                System.out.println("Can't extract date from: " + cookieExpires);
                return false;
            }
        }
        return (new Date().compareTo(expiry)) <= 0;
    }

    private boolean comparePaths(String cookiePath, String targetPath) {
        return (cookiePath == null) || cookiePath.equals("/") || targetPath.regionMatches(0, cookiePath, 0, cookiePath.length());
    }

    /**
     * Returns a string representation of stored cookies organized by domain.
     */
    public String toString() {
        return Stringer.toString(STORE);
    }
}
