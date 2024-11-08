package topchat.client.connection;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextField;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import topchat.client.chat.User;

/**
 *
 * @author Oana Iancu
 */
public class ClientConnection implements ConnectConstants {

    public static XMPPConnection connection;

    public static User user;

    public static void makeConnection(JTextField name, JTextField pass, JTextField server) throws XMPPException {
        user = new User(name.getText(), pass.getText(), "", "");
        ConnectionConfiguration config = new ConnectionConfiguration(server.getText(), PORT);
        connection = new XMPPConnection(config);
        try {
            connection.connect();
            if (connection.isConnected()) {
                SASLAuthentication.supportSASLMechanism("PLAIN", 0);
                connection.login(name.getText(), pass.getText(), "");
                Logger.getLogger(ClientConnection.class.getName()).log(Level.INFO, "I'm connected!");
            }
        } catch (XMPPException ex) {
            Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
