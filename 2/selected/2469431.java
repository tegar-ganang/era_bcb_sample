package org.gudy.azureus2.pluginsimpl.local.utils.resourceuploader;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import org.gudy.azureus2.core3.security.SEPasswordListener;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.util.AddressUtils;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourceuploader.*;

public class ResourceUploaderURLImpl implements ResourceUploader, SEPasswordListener {

    private URL target;

    private InputStream data;

    private String user_name;

    private String password;

    private Map properties = new HashMap();

    protected ResourceUploaderURLImpl(URL _target, InputStream _data, String _user_name, String _password) {
        target = _target;
        data = _data;
        user_name = _user_name;
        password = _password;
    }

    public void setProperty(String name, Object value) throws ResourceDownloaderException {
        properties.put(name, value);
    }

    public Object getProperty(String name) throws ResourceDownloaderException {
        return (properties.get(name));
    }

    public InputStream upload() throws ResourceUploaderException {
        try {
            try {
                URL url = new URL(target.toString().replaceAll(" ", "%20"));
                String protocol = url.getProtocol().toLowerCase();
                if (url.getPort() == -1) {
                    int target_port;
                    if (protocol.equals("http")) {
                        target_port = 80;
                    } else {
                        target_port = 443;
                    }
                    try {
                        String str = target.toString().replaceAll(" ", "%20");
                        int pos = str.indexOf("://");
                        pos = str.indexOf("/", pos + 4);
                        if (pos == -1) {
                            url = new URL(str + ":" + target_port + "/");
                        } else {
                            url = new URL(str.substring(0, pos) + ":" + target_port + str.substring(pos));
                        }
                    } catch (Throwable e) {
                        Debug.printStackTrace(e);
                    }
                }
                url = AddressUtils.adjustURL(url);
                try {
                    if (user_name != null) {
                        SESecurityManager.setPasswordHandler(url, this);
                    }
                    for (int i = 0; i < 2; i++) {
                        try {
                            HttpURLConnection con;
                            if (url.getProtocol().equalsIgnoreCase("https")) {
                                HttpsURLConnection ssl_con = (HttpsURLConnection) url.openConnection();
                                ssl_con.setHostnameVerifier(new HostnameVerifier() {

                                    public boolean verify(String host, SSLSession session) {
                                        return (true);
                                    }
                                });
                                con = ssl_con;
                            } else {
                                con = (HttpURLConnection) url.openConnection();
                            }
                            con.setRequestMethod("POST");
                            con.setRequestProperty("User-Agent", Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION);
                            setRequestProperties(con, false);
                            con.setDoOutput(true);
                            con.setDoInput(true);
                            OutputStream os = con.getOutputStream();
                            byte[] buffer = new byte[65536];
                            while (true) {
                                int len = data.read(buffer);
                                if (len <= 0) {
                                    break;
                                }
                                os.write(buffer, 0, len);
                            }
                            con.connect();
                            int response = con.getResponseCode();
                            if ((response != HttpURLConnection.HTTP_ACCEPTED) && (response != HttpURLConnection.HTTP_OK)) {
                                throw (new ResourceUploaderException("Error on connect for '" + url.toString() + "': " + Integer.toString(response) + " " + con.getResponseMessage()));
                            }
                            InputStream is = con.getInputStream();
                            getRequestProperties(con);
                            return (is);
                        } catch (SSLException e) {
                            if (i == 0) {
                                if (SESecurityManager.installServerCertificates(url) != null) {
                                    continue;
                                }
                            }
                            throw (e);
                        } catch (IOException e) {
                            if (i == 0) {
                                URL retry_url = UrlUtils.getIPV4Fallback(url);
                                if (retry_url != null) {
                                    url = retry_url;
                                    continue;
                                }
                            }
                            throw (e);
                        }
                    }
                    throw (new ResourceUploaderException("Should never get here"));
                } finally {
                    if (user_name != null) {
                        SESecurityManager.setPasswordHandler(url, null);
                    }
                }
            } catch (java.net.MalformedURLException e) {
                throw (new ResourceUploaderException("Exception while parsing URL '" + target + "':" + e.getMessage(), e));
            } catch (java.net.UnknownHostException e) {
                throw (new ResourceUploaderException("Exception while initializing download of '" + target + "': Unknown Host '" + e.getMessage() + "'", e));
            } catch (java.io.IOException e) {
                throw (new ResourceUploaderException("I/O Exception while downloading '" + target + "':" + e.toString(), e));
            }
        } catch (Throwable e) {
            ResourceUploaderException rde;
            if (e instanceof ResourceUploaderException) {
                rde = (ResourceUploaderException) e;
            } else {
                rde = new ResourceUploaderException("Unexpected error", e);
            }
            throw (rde);
        } finally {
            try {
                data.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    protected void setRequestProperties(HttpURLConnection con, boolean use_compression) {
        Iterator it = properties.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String) entry.getKey();
            Object value = entry.getValue();
            if (key.startsWith("URL_") && value instanceof String) {
                key = key.substring(4);
                if (key.equalsIgnoreCase("Accept-Encoding") && !use_compression) {
                } else {
                    con.setRequestProperty(key, (String) value);
                }
            }
        }
    }

    protected void getRequestProperties(HttpURLConnection con) {
        try {
            setProperty(ResourceDownloader.PR_STRING_CONTENT_TYPE, con.getContentType());
            Map headers = con.getHeaderFields();
            Iterator it = headers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String key = (String) entry.getKey();
                Object val = entry.getValue();
                if (key != null) {
                    setProperty("URL_" + key, val);
                }
            }
        } catch (Throwable e) {
            Debug.printStackTrace(e);
        }
    }

    public PasswordAuthentication getAuthentication(String realm, URL tracker) {
        if (user_name == null || password == null) {
            String user_info = tracker.getUserInfo();
            if (user_info == null) {
                return (null);
            }
            String user_bit = user_info;
            String pw_bit = "";
            int pos = user_info.indexOf(':');
            if (pos != -1) {
                user_bit = user_info.substring(0, pos);
                pw_bit = user_info.substring(pos + 1);
            }
            return (new PasswordAuthentication(user_bit, pw_bit.toCharArray()));
        }
        return (new PasswordAuthentication(user_name, password.toCharArray()));
    }

    public void setAuthenticationOutcome(String realm, URL tracker, boolean success) {
    }

    public void clearPasswords() {
    }
}
