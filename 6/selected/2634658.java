package crony.services.xmpp;

import crony.ui.main_frames.AccountsManagerFrame;
import javax.swing.JOptionPane;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;

/**
 *
 * @author Abhishek
 */
public class XMPPGateway {

    protected XMPPConnection connection;

    protected String username;

    protected String jid;

    protected void connect(String _username, String _password, ConnectionConfiguration config) {
        config.setCompressionEnabled(true);
        config.setDebuggerEnabled(true);
        connection = new XMPPConnection(config);
        try {
            connection.connect();
        } catch (XMPPException ex) {
            JOptionPane.showMessageDialog(AccountsManagerFrame.getInstance(), "remote-server-timeout(504): " + ex.getMessage(), "Server Not Responding", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            connection.login(_username, _password, "CronyIM");
        } catch (XMPPException ex) {
            if (ex.getMessage().equals("forbidden(403) Username or password not correct.")) {
                JOptionPane.showMessageDialog(AccountsManagerFrame.getInstance(), "Invalid User Name or password", "Error Loggin in", JOptionPane.ERROR_MESSAGE);
                return;
            } else if (ex.getMessage().equalsIgnoreCase("SASL authentication DIGEST-MD5 failed: not-authorized:")) {
                JOptionPane.showMessageDialog(AccountsManagerFrame.getInstance(), "Either Invalid User Name or password or application is facing some problems while logging in", "Error Loggin in", JOptionPane.ERROR_MESSAGE);
                return;
            }
            System.out.println("ERROR LOGGIN IN, ERROR IS : " + ex);
        }
        jid = StringUtils.parseBareAddress(connection.getUser());
        username = StringUtils.parseName(jid);
    }

    public XMPPConnection getConnection() {
        return connection;
    }

    public Roster getRoster() {
        return connection.getRoster();
    }

    public String getUsername() {
        return username;
    }

    public String getJID() {
        return jid;
    }
}
