package ncclient;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import ncclient.gui.ChatWindow;
import ncclient.gui.AddWindow;
import ncclient.gui.LoginWindow;
import ncclient.gui.MainWindow;
import ncclient.gui.SettingsWindow;

/**
 *
 * @author Carl Berglund
 */
public class State {

    private Core core;

    private MainWindow mw;

    private ArrayList<ChatWindow> ch;

    private LoginWindow lw;

    private SettingsWindow sw;

    private AddWindow cw;

    private ArrayList<Connection> cl;

    private Connection serverConnection;

    private String id;

    private String nick;

    private String ip;

    private String status;

    /** Creates a new instance of State */
    public State() {
        ch = new ArrayList();
        cl = new ArrayList();
        try {
            InetAddress ia = InetAddress.getLocalHost();
            ip = ia.toString().split("/")[1];
        } catch (Exception e) {
            System.out.println("State1: " + e);
        }
    }

    public void addConnection(Socket s, Contact c, BufferedReader reader, BufferedWriter writer) {
        Connection connection = new Connection(s, c, reader, writer);
        cl.add(connection);
    }

    public Connection getConnection(Contact c) {
        for (int i = 0; i < cl.size(); i++) {
            if (cl.get(i).getContact().equals(c)) {
                return cl.get(i);
            }
        }
        return null;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNick() {
        return nick;
    }

    public String getIP() {
        return ip;
    }

    public String getID() {
        return id;
    }

    public Core getCore() {
        return core;
    }

    public void setCore(Core core) {
        this.core = core;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public void setStatus(String status) {
        this.status = status;
        mw.setStatus(status);
    }

    public String getStatus() {
        return status;
    }

    public MainWindow getMainWindow() {
        return mw;
    }

    public void setMainWindow(MainWindow mw) {
        this.mw = mw;
    }

    public LoginWindow getLoginWindow() {
        return lw;
    }

    public SettingsWindow getSettingsWindow() {
        return sw;
    }

    public void setSettingsWindow(SettingsWindow sw) {
        this.sw = sw;
    }

    public void addChatWindow(ChatWindow ch) {
        this.ch.add(ch);
    }

    public void setLoginWindow(LoginWindow lw) {
        this.lw = lw;
    }

    public Connection getServerConnection() {
        return serverConnection;
    }

    public Contact getContactById(String id) {
        for (int i = 0; i < cl.size(); i++) {
            if (cl.get(i).getContact().getId().equals(id)) {
                return cl.get(i).getContact();
            }
        }
        return null;
    }

    public void setServerConnection(Socket s, BufferedReader reader, BufferedWriter writer) {
        Connection tmp = new Connection(s, null, reader, writer);
        this.serverConnection = tmp;
    }

    public ChatWindow chatWindowExists(Contact c) {
        for (int i = 0; i < ch.size(); i++) {
            if (ch.get(i).getContact().equals(c)) {
                return ch.get(i);
            }
        }
        return null;
    }

    public void addMsg(Contact contact, String msg) {
        ChatWindow cw;
        if ((cw = chatWindowExists(contact)) == null) {
            cw = new ChatWindow(this, contact);
            addChatWindow(cw);
            cw.addText(contact, msg);
            cw.setVisible(true);
            cw.setTitle(contact.getNick());
            cw.setState(Frame.ICONIFIED);
        } else {
            cw.addText(contact, msg);
            cw.setVisible(true);
        }
    }

    public void userHasExit(Contact contact) {
        System.out.println("userHasExit");
        for (int i = 0; i < cl.size(); i++) {
            if (cl.get(i).getContact().equals(contact)) {
                System.out.println("cl: " + i);
                cl.remove(i);
                break;
            }
        }
        ChatWindow cw;
        if ((cw = chatWindowExists(contact)) != null) {
            if (cw.isVisible()) {
                cw.addText(contact, "--The user has left--");
                cw.setWritable(false);
            } else {
                cw.dispose();
            }
        }
        for (int i = 0; i < ch.size(); i++) {
            if (ch.get(i).getContact().equals(contact)) {
                ch.remove(i);
                break;
            }
            System.out.println("ch: " + i);
        }
        mw.removeContact(contact);
    }

    public void exit() {
        try {
            for (int i = 0; i < cl.size(); i++) {
                cl.get(i).getWriter().write("BYE\n");
                cl.get(i).getWriter().flush();
            }
            serverConnection.getWriter().write("SETSTATUS OFFLINE\n");
            serverConnection.getWriter().flush();
        } catch (Exception e) {
            System.out.println("State2: " + e);
        }
    }
}
