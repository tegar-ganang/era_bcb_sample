package org.imlinker.login;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

/**
 * 登录线程.
 */
public class Login extends Thread {

    public Login() {
        windowManager = new WindowManager(this);
    }

    public XMPPConnection getConnection() {
        return connection;
    }

    public void run() {
        boolean loginSuccess = false;
        while (!loginSuccess) {
            final int sleepSeconds = 100;
            while (fullAccount == null) {
                try {
                    Thread.sleep(sleepSeconds);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            windowManager.setStatusByBundle("Login.conntect");
            String host = fullAccount.getHost();
            int port = Integer.parseInt(fullAccount.getPort());
            String domain = fullAccount.getDomain();
            ConnectionConfiguration config = new ConnectionConfiguration(host, port, domain);
            connection = new XMPPConnection(config);
            try {
                connection.connect();
            } catch (XMPPException e) {
                e.printStackTrace();
            }
            if (connection.isConnected()) {
                windowManager.setStatusByBundle("Login.logining");
                String username = fullAccount.getUser();
                String password = fullAccount.getPassword();
                try {
                    connection.login(username, password);
                } catch (XMPPException e) {
                }
                if (connection.isAuthenticated()) {
                    loginSuccess = true;
                    windowManager.setStatusByBundle("Login.initialing");
                } else {
                    windowManager.setStatusByBundle("Login.loginfailed");
                    fullAccount = null;
                }
            } else {
                fullAccount = null;
                windowManager.setStatusByBundle("Login.connectfailed");
            }
        }
        windowManager.closeLoginWindow();
    }

    public void setAccount(final Account account) {
        fullAccount = Accounts.fillAccount(account);
    }

    private Account fullAccount;

    private WindowManager windowManager;

    private XMPPConnection connection;
}
