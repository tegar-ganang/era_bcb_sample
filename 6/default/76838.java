import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

public class conn {

    public static void main(String[] args) throws XMPPException {
        XMPPConnection.DEBUG_ENABLED = true;
        XMPPConnection connection = new XMPPConnection("uberklop.org");
        connection.connect();
        connection.login("test", "testtest");
    }
}
