package net.sf.j2s.ajax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

/**
 * This class is a Java implementation of browser's XMLHttpRequest object.
 * This class can be considered as a bridge of Java's AJAX programming and
 * JavaScript/Browser's AJAX programming.
 * 
 * @author zhou renjian
 *
 * 2006-2-11
 */
public class HttpRequest {

    /**
	 * Static methods for translating Base64 encoded strings to byte arrays
	 * and vice-versa.
	 *
	 * @author  Josh Bloch
	 * @version 1.4, 01/23/03
	 * @see     Preferences
	 * @since   1.4
	 */
    static class Base64 {

        /**
	     * Translates the specified byte array into a Base64 string as per
	     * Preferences.put(byte[]).
	     */
        static String byteArrayToBase64(byte[] a) {
            int aLen = a.length;
            int numFullGroups = aLen / 3;
            int numBytesInPartialGroup = aLen - 3 * numFullGroups;
            int resultLen = 4 * ((aLen + 2) / 3);
            StringBuffer result = new StringBuffer(resultLen);
            char[] intToAlpha = intToBase64;
            int inCursor = 0;
            for (int i = 0; i < numFullGroups; i++) {
                int byte0 = a[inCursor++] & 0xff;
                int byte1 = a[inCursor++] & 0xff;
                int byte2 = a[inCursor++] & 0xff;
                result.append(intToAlpha[byte0 >> 2]);
                result.append(intToAlpha[(byte0 << 4) & 0x3f | (byte1 >> 4)]);
                result.append(intToAlpha[(byte1 << 2) & 0x3f | (byte2 >> 6)]);
                result.append(intToAlpha[byte2 & 0x3f]);
            }
            if (numBytesInPartialGroup != 0) {
                int byte0 = a[inCursor++] & 0xff;
                result.append(intToAlpha[byte0 >> 2]);
                if (numBytesInPartialGroup == 1) {
                    result.append(intToAlpha[(byte0 << 4) & 0x3f]);
                    result.append("==");
                } else {
                    int byte1 = a[inCursor++] & 0xff;
                    result.append(intToAlpha[(byte0 << 4) & 0x3f | (byte1 >> 4)]);
                    result.append(intToAlpha[(byte1 << 2) & 0x3f]);
                    result.append('=');
                }
            }
            return result.toString();
        }

        /**
	     * This array is a lookup table that translates 6-bit positive integer
	     * index values into their "Base64 Alphabet" equivalents as specified 
	     * in Table 1 of RFC 2045.
	     */
        private static final char intToBase64[] = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/' };
    }

    /**
	 * This class is used to monitoring data-receiving process.
	 * 
	 * Attention: Only be visible in Java.
	 */
    public static interface IXHRReceiving {

        /**
		 * Monitoring the received data along with the given output stream.
		 * 
		 * @param baos an output stream
		 * @param b buffer
		 * @param off offset
		 * @param len length
		 * @return whether the data is dealt into the given output stream or not
		 */
        public boolean receiving(ByteArrayOutputStream baos, byte b[], int off, int len);
    }

    protected int status;

    protected String statusText;

    protected int readyState;

    protected String responseText;

    protected Document responseXML;

    protected IXHRCallback onreadystatechange;

    protected IXHRReceiving receiving;

    protected boolean asynchronous;

    private HttpURLConnection connection;

    protected String url;

    protected String method;

    protected String user;

    protected String password;

    protected Map<String, String> headers = new HashMap<String, String>();

    protected String content;

    protected boolean toAbort = false;

    protected boolean isDisconnected = false;

    private OutputStream activeOS;

    private InputStream activeIS;

    protected boolean isCometConnection = false;

    /**
	 * Return read state of XMLHttpRequest.
	 * @return int ready state
	 */
    public int getReadyState() {
        return readyState;
    }

    /**
	 * Return response raw text of XMLHttpRequest 
	 * @return String response text. May be null if the request is not sent
	 * or an error happens. 
	 */
    public String getResponseText() {
        return responseText;
    }

    /**
	 * Return the parsed XML document of the response of XMLHttpRequest.
	 * @return Document XML document. May be null if the response text is not
	 * a valid XML document.
	 */
    public Document getResponseXML() {
        if (responseXML != null) {
            return responseXML;
        }
        String type = connection.getHeaderField("Content-Type");
        if (type != null && (type.indexOf("/xml") != -1 || type.indexOf("+xml") != -1)) {
            if (responseText != null && responseText.length() != 0) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                dbf.setAttribute("http://xml.org/sax/features/namespaces", Boolean.TRUE);
                try {
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    ByteArrayInputStream biStream = new ByteArrayInputStream(responseText.getBytes());
                    responseXML = db.parse(biStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return responseXML;
        } else {
            return null;
        }
    }

    /**
	 * Return response code.
	 * @return int response code. For more information please read about
	 * HTTP protocol.
	 */
    public int getStatus() {
        return status;
    }

    /**
	 * Return response code related text.
	 * @return int response code. For more information please read about
	 * HTTP protocol.
	 */
    public String getStatusText() {
        return statusText;
    }

    /**
	 * Register XMLHttpRequest callback.
	 * 
	 * @param onreadystatechange IXHRCallback callback
	 */
    public void registerOnReadyStateChange(IXHRCallback onreadystatechange) {
        this.onreadystatechange = onreadystatechange;
    }

    /**
	 * Register XMLHttpRequest receiving monitor.
	 * 
	 * This method is to given inherited class a chance to set a monitor to
	 * monitor the process that the data is receiving from the connection. 
	 * 
	 * Attention: Only be visible inside Java! There is no such JavaScript
	 * methods.
	 *  
	 * @param receiving IXHRReceiving monitor
	 */
    protected IXHRReceiving initializeReceivingMonitor() {
        return null;
    }

    /**
	 * Set request header with given key and value.
	 * @param key String request header keyword. For more information please 
	 * read about HTTP protocol.
	 * @param value String request header value
	 */
    public void setRequestHeader(String key, String value) {
        headers.put(key, value);
    }

    /**
	 * Get all response headers.
	 * @return String the all response header value.
	 */
    public String getAllResponseHeaders() {
        StringBuffer buffer = new StringBuffer();
        int i = 1;
        while (true) {
            String key = connection.getHeaderFieldKey(i);
            if (key != null) {
                String value = connection.getHeaderField(i);
                buffer.append(key);
                buffer.append(": ");
                buffer.append(value);
                buffer.append("\r\n");
            } else {
                break;
            }
            i++;
        }
        buffer.append("\r\n");
        return buffer.toString();
    }

    /**
	 * Get response header with given key.
	 * @param key String header keyword. For more information please 
	 * read about HTTP protocol.
	 * @return String the response header value.
	 */
    public String getResponseHeader(String key) {
        Map<String, List<String>> headerFields = connection.getHeaderFields();
        List<String> list = headerFields.get(key);
        if (list == null) {
            return null;
        }
        if (list.size() == 0) {
            return "";
        }
        String headerValue = null;
        for (Iterator<String> itr = list.iterator(); itr.hasNext(); ) {
            String value = (String) itr.next();
            if (value != null) {
                if (headerValue == null) {
                    headerValue = value;
                } else {
                    headerValue = value + "\r\n" + headerValue;
                }
            }
        }
        return headerValue;
    }

    /**
	 * Open connection for HTTP request with given method and URL 
	 * synchronously.
	 * 
	 * @param method String "POST" or "GET" usually.
	 * @param url String remote URL. Should always be absolute URL.
	 */
    public void open(String method, String url) {
        open(method, url, false, null, null);
    }

    /**
	 * Open connection for HTTP request with given method, URL and mode.
	 * 
	 * @param method String "POST" or "GET" usually.
	 * @param url String remote URL. Should always be absolute URL.
	 * @param async boolean whether send request asynchronously or not. 
	 */
    public void open(String method, String url, boolean async) {
        open(method, url, async, null, null);
    }

    /**
	 * Open connection for HTTP request with given method, URL and mode.
	 * 
	 * @param method String "POST" or "GET" usually.
	 * @param url String remote URL. Should always be absolute URL.
	 * @param async boolean whether send request asynchronously or not. 
	 * @param user String user name
	 */
    public void open(String method, String url, boolean async, String user) {
        open(method, url, async, user, null);
    }

    /**
	 * Open connection for HTTP request with given method, URL and mode.
	 * 
	 * @param method String "POST" or "GET" usually.
	 * @param url String remote URL. Should always be absolute URL.
	 * @param async boolean whether send request asynchronously or not.
	 * @param user String user name
	 * @param password String user password 
	 */
    public void open(String method, String url, boolean async, String user, String password) {
        this.asynchronous = async;
        this.method = method;
        this.url = url;
        this.user = user;
        this.password = password;
        responseText = null;
        responseXML = null;
        readyState = 1;
        status = 200;
        statusText = null;
        toAbort = false;
        if (onreadystatechange != null) {
            onreadystatechange.onOpen();
        }
    }

    /**
	 * Send the HTTP request without extra content.
	 */
    public void send() {
        send(null);
    }

    /**
	 * Send the HTTP request with given content.
	 * @param str String HTTP request content. May be null.
	 */
    public void send(String str) {
        content = str;
        if (asynchronous) {
            (new Thread("Java2Script HTTP Request") {

                public void run() {
                    if (!toAbort) {
                        request();
                    }
                }
            }).start();
        } else {
            request();
        }
    }

    /**
	 * Abort the sending or receiving data process.
	 */
    public void abort() {
        toAbort = true;
        isDisconnected = false;
        checkAbort();
    }

    protected boolean checkAbort() {
        if (!toAbort) return false;
        if (activeOS != null) {
            try {
                activeOS.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            activeOS = null;
        }
        if (activeIS != null) {
            try {
                activeIS.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            activeIS = null;
        }
        if (!isDisconnected && connection != null) {
            connection.disconnect();
            isDisconnected = true;
        }
        return true;
    }

    private void request() {
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            if (isCometConnection) {
                connection.setReadTimeout(0);
            } else {
                connection.setReadTimeout(30000);
            }
            connection.setInstanceFollowRedirects(false);
            connection.setDoInput(true);
            connection.setRequestMethod(method);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.5) Gecko/20091102 Firefox/3.5.5 GTB5");
            if ("post".equalsIgnoreCase(method)) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            }
            if (user != null) {
                String auth = user + ":" + (password != null ? password : "");
                String base64Auth = HttpRequest.Base64.byteArrayToBase64(auth.getBytes());
                connection.setRequestProperty("Authorization", "Basic " + base64Auth);
            }
            for (Iterator<String> iter = headers.keySet().iterator(); iter.hasNext(); ) {
                String key = (String) iter.next();
                connection.setRequestProperty(key, (String) headers.get(key));
            }
            connection.setUseCaches(false);
            if (checkAbort()) return;
            if ("post".equalsIgnoreCase(method)) {
                DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
                activeOS = dos;
                if (content != null) {
                    dos.writeBytes(content);
                }
                if (checkAbort()) return;
                dos.flush();
                dos.close();
                activeOS = null;
            }
            if (checkAbort()) return;
            InputStream is = null;
            try {
                is = connection.getInputStream();
            } catch (IOException e) {
                if (checkAbort()) return;
                readyState = 4;
                if (onreadystatechange != null) {
                    onreadystatechange.onLoaded();
                }
                connection = null;
                readyState = 0;
                return;
            }
            activeIS = is;
            if (readyState < 2) {
                readyState = 2;
                status = connection.getResponseCode();
                statusText = connection.getResponseMessage();
                if (onreadystatechange != null) {
                    onreadystatechange.onSent();
                }
            }
            receiving = initializeReceivingMonitor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
            byte[] buffer = new byte[10240];
            int read;
            while (!toAbort && (read = is.read(buffer)) != -1) {
                if (checkAbort()) return;
                if (readyState != 3) {
                    readyState = 3;
                    if (onreadystatechange != null) {
                        onreadystatechange.onReceiving();
                    }
                }
                boolean received = false;
                if (receiving != null) {
                    received = receiving.receiving(baos, buffer, 0, read);
                }
                if (!received) {
                    baos.write(buffer, 0, read);
                }
            }
            if (checkAbort()) return;
            is.close();
            activeIS = null;
            responseText = null;
            String type = connection.getHeaderField("Content-Type");
            if (type != null) {
                String charset = null;
                String lowerType = type.toLowerCase();
                int idx = lowerType.indexOf("charset=");
                if (idx != -1) {
                    charset = type.substring(idx + 8);
                } else {
                    idx = lowerType.indexOf("/xml");
                    if (idx != -1) {
                        String tmp = baos.toString();
                        Matcher matcher = Pattern.compile("<\\?.*encoding\\s*=\\s*[\'\"]([^'\"]*)[\'\"].*\\?>", Pattern.MULTILINE).matcher(tmp);
                        if (matcher.find()) {
                            charset = matcher.group(1);
                        } else {
                            responseText = tmp;
                        }
                    } else {
                        idx = lowerType.indexOf("html");
                        if (idx != -1) {
                            String tmp = baos.toString();
                            Matcher matcher = Pattern.compile("<meta.*content\\s*=\\s*[\'\"][^'\"]*charset\\s*=\\s*([^'\"]*)\\s*[\'\"].*>", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(tmp);
                            if (matcher.find()) {
                                charset = matcher.group(1);
                            } else {
                                responseText = tmp;
                            }
                        }
                    }
                }
                if (charset != null) {
                    try {
                        responseText = baos.toString(charset);
                    } catch (UnsupportedEncodingException e) {
                    }
                }
            }
            if (responseText == null) {
                try {
                    responseText = baos.toString("iso-8859-1");
                } catch (UnsupportedEncodingException e) {
                    responseText = baos.toString();
                }
            }
            readyState = 4;
            if (onreadystatechange != null) {
                onreadystatechange.onLoaded();
            }
            connection.disconnect();
            readyState = 0;
        } catch (Exception e) {
            if (checkAbort()) return;
            e.printStackTrace();
            readyState = 4;
            if (onreadystatechange != null) {
                onreadystatechange.onLoaded();
            }
            connection = null;
            readyState = 0;
        }
    }

    /**
	 * Enabling Comet mode for HTTP request connection.
	 * Comet connection is used on Java level to provide SimplePipe connection.
	 * @param isCometConnection
	 */
    protected void setCometConnection(boolean isCometConnection) {
        this.isCometConnection = isCometConnection;
    }
}
