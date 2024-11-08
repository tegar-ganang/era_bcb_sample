package net.sf.mongrel.actions;

import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import net.sf.mongrel.TomcatLauncherPlugin;
import net.sf.mongrel.TomcatProject;

public class RestartContextActionDelegate extends TomcatProjectAbstractActionDelegate {

    public void doActionOn(TomcatProject prj) throws Exception {
        String path = TomcatLauncherPlugin.getDefault().getManagerAppUrl();
        try {
            path += "/reload?path=" + prj.getWebPath();
            URL url = new URL(path);
            Authenticator.setDefault(new Authenticator() {

                protected PasswordAuthentication getPasswordAuthentication() {
                    String user = TomcatLauncherPlugin.getDefault().getManagerAppUser();
                    String password = TomcatLauncherPlugin.getDefault().getManagerAppPassword();
                    return new PasswordAuthentication(user, password.toCharArray());
                }
            });
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.getContent();
            connection.disconnect();
            Authenticator.setDefault(null);
        } catch (Exception e) {
            throw new Exception("The following url was used : \n" + path + "\n\nCheck manager app settings (username and password)\n\n");
        }
    }
}
