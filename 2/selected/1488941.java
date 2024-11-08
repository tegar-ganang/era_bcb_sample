package neembuuuploader.accounts;

import neembuuuploader.accountgui.AccountsManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import neembuuuploader.HostsPanel;
import neembuuuploader.NeembuuUploader;
import neembuuuploader.TranslationProvider;
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
 * @author vigneshwaran
 */
public class HotFileAccount extends AbstractAccount {

    public static Cookie hfcookie = null;

    public HotFileAccount() {
        KEY_USERNAME = "hfusername";
        KEY_PASSWORD = "hfpassword";
        HOSTNAME = "HotFile.com";
    }

    @Override
    public void login() {
        loginsuccessful = false;
        try {
            HttpParams params = new BasicHttpParams();
            params.setParameter("http.useragent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2) Gecko/20100115 Firefox/3.6");
            DefaultHttpClient httpclient = new DefaultHttpClient(params);
            NULogger.getLogger().info("Trying to log in to HotFile");
            HttpPost httppost = new HttpPost("http://www.hotfile.com/login.php");
            httppost.setHeader("Referer", "http://www.hotfile.com/");
            httppost.setHeader("Cache-Control", "max-age=0");
            httppost.setHeader("Origin", "http://www.hotfile.com/");
            httppost.setHeader("Accept", "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("returnto", "%2F"));
            formparams.add(new BasicNameValuePair("user", getUsername()));
            formparams.add(new BasicNameValuePair("pass", getPassword()));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            httppost.setEntity(entity);
            HttpResponse httpresponse = httpclient.execute(httppost);
            if (httpresponse.getFirstHeader("Set-Cookie") == null) {
                NULogger.getLogger().info("HotFile Login not successful");
                loginsuccessful = false;
                username = "";
                password = "";
                JOptionPane.showMessageDialog(NeembuuUploader.getInstance(), "<html><b>" + HOSTNAME + "</b> " + TranslationProvider.get("neembuuuploader.accounts.loginerror") + "</html>", HOSTNAME, JOptionPane.WARNING_MESSAGE);
                AccountsManager.getInstance().setVisible(true);
            } else {
                Iterator<Cookie> it = httpclient.getCookieStore().getCookies().iterator();
                while (it.hasNext()) {
                    hfcookie = it.next();
                    if (hfcookie.getName().equals("auth")) {
                        NULogger.getLogger().log(Level.INFO, "hotfile login successful auth:{0}", hfcookie.getValue());
                        loginsuccessful = true;
                        HostsPanel.getInstance().hotFileCheckBox.setEnabled(true);
                        username = getUsername();
                        password = getPassword();
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            NULogger.getLogger().log(Level.SEVERE, "{0}: Error in Hotfile Login", getClass().getName());
        }
    }

    public static Cookie getHfcookie() {
        return hfcookie;
    }

    public void disableLogin() {
        loginsuccessful = false;
        HostsPanel.getInstance().hotFileCheckBox.setEnabled(false);
        HostsPanel.getInstance().hotFileCheckBox.setSelected(false);
        NeembuuUploader.getInstance().updateSelectedHostsLabel();
        NULogger.getLogger().log(Level.INFO, "{0} account disabled", getHOSTNAME());
    }
}
