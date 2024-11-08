package org.localstorm.marketwatch.sms;

import org.apache.commons.io.IOUtils;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class NotificationUtil {

    private static final String password = "WherWolF";

    private static final String user = "localstorm@gmail.com";

    private static final String apiId = "3337956";

    private static final String to = "79163420445";

    public static void main(String[] args) throws Exception {
        notify("Sell everything");
    }

    public static void notify(String msg) throws Exception {
        String url = "http://api.clickatell.com/http/sendmsg?";
        url = add(url, "user", user);
        url = add(url, "password", password);
        url = add(url, "api_id", apiId);
        url = add(url, "to", to);
        url = add(url, "text", msg);
        URL u = new URL(url);
        URLConnection c = u.openConnection();
        InputStream is = c.getInputStream();
        IOUtils.copy(is, System.out);
        IOUtils.closeQuietly(is);
    }

    private static String add(String url, String param, String value) throws Exception {
        return url + URLEncoder.encode(param, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8") + "&";
    }
}
