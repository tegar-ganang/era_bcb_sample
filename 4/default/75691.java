import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.util.Vector;
import org.relayirc.core.*;

public class ChatApplet extends Applet {

    private boolean _isStandalone = false;

    private String _hostName = null;

    private int _port = 6667;

    private String _channel = "#default";

    private Vector _channels = new Vector();

    private ChatPanel _chatPanel = new ChatPanel();

    private LoginPanel _loginPanel = new LoginPanel();

    private CardLayout _cardLayout = new CardLayout();

    public ChatApplet() {
    }

    public void login(String nick, String channel) {
        _cardLayout.next(this);
        if (_isStandalone == false) {
            _hostName = getCodeBase().getHost();
        }
        _chatPanel.connect(_hostName, _port, channel, nick, nick, nick, "Relay-AWT User");
    }

    public Vector getChannels() {
        return _channels;
    }

    public String getParameter(String key, String def) {
        return _isStandalone ? System.getProperty(key, def) : (getParameter(key) != null ? getParameter(key) : def);
    }

    public void init() {
        try {
            _port = Integer.parseInt(this.getParameter("port", "6667"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            _channel = this.getParameter("channel", "#relay");
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int i = -1; i < 50; i++) {
            String chan = null;
            try {
                if (i == -1) {
                    chan = getParameter("channel", null);
                } else {
                    chan = getParameter("channel" + i, null);
                }
                if (i > 0 && chan == null) {
                    break;
                } else if (chan != null) {
                    _channels.addElement(chan);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        _loginPanel = new LoginPanel(this);
        _chatPanel = new ChatPanel(this);
        setLayout(_cardLayout);
        add("LoginPanel", _loginPanel);
        add("ChatPanel", _chatPanel);
        _cardLayout.show(this, "LoginPanel");
    }

    public void start() {
    }

    public void stop() {
    }

    public void destroy() {
    }

    public String getAppletInfo() {
        return "Applet Information";
    }

    public String[][] getParameterInfo() {
        String[][] pinfo = { { "hostname", "String", "Host name of IRC server" }, { "port", "int", "Port number on host" }, { "channel", "String", "Channel " } };
        return pinfo;
    }

    public static void main(String[] args) {
        ChatApplet applet = new ChatApplet();
        applet._isStandalone = true;
        Frame frame;
        frame = new Frame() {

            protected void processWindowEvent(WindowEvent e) {
                super.processWindowEvent(e);
                if (e.getID() == WindowEvent.WINDOW_CLOSING) {
                    System.exit(0);
                }
            }

            public synchronized void setTitle(String title) {
                super.setTitle(title);
                enableEvents(AWTEvent.WINDOW_EVENT_MASK);
            }
        };
        frame.setTitle("Applet Frame");
        frame.add(applet, BorderLayout.CENTER);
        applet.init();
        applet.start();
        frame.setSize(400, 320);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((d.width - frame.getSize().width) / 2, (d.height - frame.getSize().height) / 2);
        frame.setVisible(true);
    }
}
