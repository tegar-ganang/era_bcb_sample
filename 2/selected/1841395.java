package neembuuuploader.accounts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import neembuuuploader.NeembuuUploader;
import neembuuuploader.TranslationProvider;
import neembuuuploader.accountgui.AccountsManager;
import neembuuuploader.interfaces.abstractimpl.AbstractAccount;
import neembuuuploader.utils.NULogger;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

/**
 *
 * @author dinesh
 */
public class OronAccount extends AbstractAccount {

    private static String logincookie;

    private static String xfsscookie;

    public OronAccount() {
        KEY_USERNAME = "orusername";
        KEY_PASSWORD = "orpassword";
        HOSTNAME = "Oron.com";
    }

    public static String getLogincookie() {
        return logincookie;
    }

    public static String getXfsscookie() {
        return xfsscookie;
    }

    public void disableLogin() {
        loginsuccessful = false;
        logincookie = "";
        xfsscookie = "";
        NULogger.getLogger().log(Level.INFO, "{0} account disabled", getHOSTNAME());
    }

    public void login() {
        loginsuccessful = false;
        try {
            HttpParams params = new BasicHttpParams();
            params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
            DefaultHttpClient httpclient = new DefaultHttpClient(params);
            NULogger.getLogger().info("Trying to log in to oron.com");
            HttpPost httppost = new HttpPost("http://oron.com/login");
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("login", getUsername()));
            formparams.add(new BasicNameValuePair("password", getPassword()));
            formparams.add(new BasicNameValuePair("op", "login"));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            httppost.setEntity(entity);
            HttpResponse httpresponse = httpclient.execute(httppost);
            NULogger.getLogger().info("Getting cookies........");
            Iterator<Cookie> it = httpclient.getCookieStore().getCookies().iterator();
            Cookie escookie = null;
            while (it.hasNext()) {
                escookie = it.next();
                if (escookie.getName().equalsIgnoreCase("login")) {
                    logincookie = "login=" + escookie.getValue();
                    NULogger.getLogger().info(logincookie);
                }
                if (escookie.getName().equalsIgnoreCase("xfss")) {
                    xfsscookie = "xfss=" + escookie.getValue();
                    NULogger.getLogger().info(xfsscookie);
                    loginsuccessful = true;
                }
            }
            if (loginsuccessful) {
                NULogger.getLogger().info("Oron Login successful :)");
                username = getUsername();
                password = getPassword();
            } else {
                NULogger.getLogger().info("Oron Login failed :(");
                loginsuccessful = false;
                username = "";
                password = "";
                JOptionPane.showMessageDialog(NeembuuUploader.getInstance(), "<html><b>" + HOSTNAME + "</b> " + TranslationProvider.get("neembuuuploader.accounts.loginerror") + "</html>", HOSTNAME, JOptionPane.WARNING_MESSAGE);
                AccountsManager.getInstance().setVisible(true);
            }
        } catch (Exception e) {
            NULogger.getLogger().log(Level.SEVERE, "{0}: {1}", new Object[] { getClass().getName(), e.toString() });
            System.err.println(e);
        }
    }
}
