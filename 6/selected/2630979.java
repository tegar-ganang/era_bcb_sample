package jabba.jabber.account;

import jabba.platform.account.Account;
import org.jivesoftware.smack.*;

/**
 * Класс для работы с Jabber-аккаунтом.
 * @author Ildar Shaynurov
 */
public class JabberAccount extends Account<JabberAccountPrefs> implements Runnable {

    private Thread connectionThread = null;

    private abstract class ThreadOperation implements Runnable {

        public void runOper() {
            if (Thread.currentThread() == connectionThread) {
                run();
            } else {
                if (connectionThread != null) {
                    synchronized (connectionThread) {
                        connectionThread.notify();
                    }
                }
            }
        }
    }

    private ThreadOperation threadOperation = null;

    XMPPConnection connection = null;

    /**
   * Creates a new instance of Account
   * @param accountPrefs Параметры аккаунта.
   */
    public JabberAccount(JabberAccountPrefs accountPrefs) {
        super(accountPrefs);
    }

    public void connect() {
        connectionThread = new Thread(this, "Account thread");
        connectionThread.start();
    }

    public void dissconnect() {
        connectionThread = null;
    }

    public Roster getRoster() {
        return connection.getRoster();
    }

    public ChatManager getChatManager() {
        return connection.getChatManager();
    }

    public void run() {
        try {
            setMainState(EMainState.connecting);
            connection = new XMPPConnection(accountPrefs.getServer());
            connection.connect();
            connection.login(accountPrefs.getLogin(), accountPrefs.getPasswd());
            setMainState(EMainState.online);
            while (Thread.currentThread() == connectionThread) {
                try {
                    synchronized (connectionThread) {
                        connectionThread.wait();
                    }
                    if (threadOperation != null) threadOperation.run();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            connection.disconnect();
            connection = null;
            setMainState(EMainState.offline);
        } catch (Exception ex) {
            System.err.println("Exception caughted: " + ex);
            ex.printStackTrace();
            connection = null;
            setMainState(EMainState.offline);
        }
    }
}
