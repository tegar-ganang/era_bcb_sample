package annone.server.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import annone.engine.Channel;
import annone.engine.Engine;
import annone.http.HttpContent;
import annone.http.HttpContext;
import annone.http.HttpCookie;
import annone.http.HttpNotModifiedException;
import annone.http.HttpRequest;
import annone.http.HttpResponse;
import annone.http.HttpServer;
import annone.mime.ContentType;
import annone.server.EndPoint;
import annone.util.Checks;
import annone.util.Const;
import annone.util.Safe;

public class WebContext extends HttpContext {

    public static final String X_ANNONE_CHANNEL_ID = "X-Annone-Channel-Id";

    private static Map<String, Object> buildUrlEncoded(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, Const.US_ASCII));
        Map<String, Object> data = null;
        String line;
        StringBuilder b = new StringBuilder();
        do {
            line = r.readLine();
            if (line != null) b.append(line);
            String nameAndValue;
            while (b.length() > 0) {
                int i = b.indexOf("&");
                if (i < 0) {
                    if (line == null) {
                        if (b.length() == 0) break;
                        nameAndValue = b.toString();
                        b.setLength(0);
                    } else break;
                } else {
                    nameAndValue = b.substring(0, i);
                    b.delete(0, i + 1);
                }
                if (data == null) data = new TreeMap<String, Object>();
                splitUrlEncodedNameAndValue(data, nameAndValue);
            }
        } while (line != null);
        return data;
    }

    private static Map<String, Object> buildUrlEncoded(String text) {
        Map<String, Object> data = null;
        for (String nameAndValue : text.split("&")) if (!nameAndValue.isEmpty()) {
            if (data == null) data = new TreeMap<String, Object>();
            splitUrlEncodedNameAndValue(data, nameAndValue);
        }
        return data;
    }

    private static void splitUrlEncodedNameAndValue(Map<String, Object> data, String nameAndValue) {
        int i = nameAndValue.indexOf('=');
        String name;
        String value;
        if (i >= 0) {
            name = decodeUrlEscaped(nameAndValue.substring(0, i));
            value = decodeUrlEscaped(nameAndValue.substring(i + 1));
        } else {
            name = decodeUrlEscaped(nameAndValue);
            value = "";
        }
        Object oldValue = data.get(name);
        if (oldValue == null) data.put(name, value); else if (oldValue instanceof String) data.put(name, new String[] { (String) oldValue, value }); else if (oldValue instanceof String[]) {
            String[] newValue = (String[]) oldValue;
            newValue = Arrays.copyOf(newValue, newValue.length + 1);
            newValue[newValue.length - 1] = value;
            data.put(name, newValue);
        }
    }

    private static String decodeUrlEscaped(String s) {
        if (Safe.isEmpty(s)) return s;
        byte[] b = new byte[s.length()];
        int ib = 0, is = 0;
        while (is < s.length()) {
            char ch = s.charAt(is);
            switch(ch) {
                case '+':
                    b[ib++] = ' ';
                    is++;
                    break;
                case '%':
                    if (is < s.length() - 2) {
                        b[ib++] = (byte) Integer.parseInt(s.substring(is + 1, is + 3), 16);
                        is += 3;
                        break;
                    }
                default:
                    b[ib++] = (byte) ch;
                    is++;
            }
        }
        return new String(b, 0, ib, Const.UTF_8);
    }

    private static Map<String, Object> buildFormData(InputStream in) {
        return null;
    }

    private final Engine engine;

    private Channel channel;

    private Map<String, Object> queryData;

    private Map<String, Object> formData;

    public WebContext(HttpServer server, EndPoint endPoint, HttpRequest request, HttpResponse response, Engine engine) {
        super(server, endPoint, request, response);
        this.engine = engine;
    }

    public Engine getEngine() {
        return engine;
    }

    private Map<String, Object> getQueryDataImpl() {
        if (queryData == null) {
            Map<String, Object> queryData = null;
            String query = getRequest().getUri().getQuery();
            if (!Safe.isEmpty(query)) queryData = buildUrlEncoded(query);
            if (queryData != null) this.queryData = queryData; else this.queryData = Collections.emptyMap();
        }
        return queryData;
    }

    public Map<String, Object> getQueryData() {
        return Collections.unmodifiableMap(getQueryDataImpl());
    }

    public Object getQueryData(String name) {
        Checks.notEmpty("name", name);
        return getQueryDataImpl().get(name);
    }

    private Map<String, Object> getFormDataImpl() {
        if (formData == null) {
            Map<String, Object> formData = null;
            HttpContent content = getRequest().getContent();
            if (content != null) try {
                ContentType contentType = getRequest().getContentType();
                if ("application".equals(contentType.getPrimaryType()) && "x-www-form-urlencoded".equals(contentType.getSecondaryType())) formData = buildUrlEncoded(content.getInputStream()); else if ("multipart".equals(contentType.getPrimaryType()) && "form-data".equals(contentType.getSecondaryType())) formData = buildFormData(content.getInputStream());
            } catch (IOException xp) {
                formData = null;
            }
            if (formData != null) this.formData = formData; else this.formData = Collections.emptyMap();
        }
        return formData;
    }

    public Map<String, Object> getFormData() {
        return Collections.unmodifiableMap(getFormDataImpl());
    }

    public Object getFormData(String name) {
        Checks.notEmpty("name", name);
        return getFormDataImpl().get(name);
    }

    public Object getClientData(String name) {
        Checks.notEmpty("name", name);
        Object data = getQueryDataImpl().get(name);
        if (data == null) data = getFormDataImpl().get(name);
        if (data == null) {
            HttpCookie cookie = getRequest().getCookie(name);
            if (cookie != null) data = cookie.getValue();
        }
        return data;
    }

    @Override
    public WebServer getServer() {
        return (WebServer) super.getServer();
    }

    public synchronized Channel getChannel() {
        if (channel == null) {
            Object channelId = getClientData("channelId");
            Channel channel = null;
            if (channelId != null) channel = getServer().getChannels().get(channelId.toString());
            this.channel = channel;
        }
        return channel;
    }

    public synchronized Channel newChannel() {
        Channel channel = engine.openChannel();
        getResponse().addCookie(new HttpCookie("channelId", channel.getId()));
        getServer().getChannels().put(channel.getId(), channel);
        this.channel = channel;
        return channel;
    }

    public void checkAndSetLastModified(Date lastModified) {
        Date ifModifiedSince = getRequest().getDateHeader("If-Modified-Since");
        if ((lastModified != null) && (ifModifiedSince != null)) if (lastModified.equals(ifModifiedSince)) throw new HttpNotModifiedException("Not modified.");
        getResponse().setDateHeader("Last-Modified", lastModified);
    }

    public Locale getLocale() {
        Locale locale = getRequest().getPreferredLanguage();
        if (locale == null) locale = Locale.ENGLISH;
        return locale;
    }
}
