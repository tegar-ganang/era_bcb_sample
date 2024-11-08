import java.nio.charset.Charset;
import java.util.ArrayList;
import javax.swing.JTabbedPane;

public class Server implements Runnable {

    private Rirca main;

    private Input in;

    private Output out;

    private InputInterpreter ii;

    private OutputInterpreter oi;

    private JTabbedPane bt;

    private JTabbedPane tt;

    private BottomTab serverTab;

    private TopTab statusTab;

    private ArrayList<Channel> joinedChannel = new ArrayList<Channel>();

    private ArrayList<Query> openQuery = new ArrayList<Query>();

    private String groupName;

    private String charset;

    private ArrayList<String> server = new ArrayList<String>();

    private ArrayList<Integer> port = new ArrayList<Integer>();

    private ArrayList<String> serverPass = new ArrayList<String>();

    private ArrayList<String> channel = new ArrayList<String>();

    private String[] nick = new String[2];

    private String[] nickPass = new String[2];

    private String username;

    private String realname;

    private String currNick;

    private String remoteHost;

    private String umode;

    public Server(Rirca main, JTabbedPane bt, Group group) {
        this.main = main;
        this.bt = bt;
        groupName = group.getGroupName();
        charset = group.getCharset();
        server = group.getServers();
        port = group.getPorts();
        serverPass = group.getServerPasses();
        channel = group.getChannels();
        nick = group.getNicks();
        nickPass = group.getNickPasses();
        username = group.getUsername();
        realname = group.getRealname();
    }

    public Server(Rirca main, JTabbedPane bt, String server, int port, Charset charset, String[] nick, String username, String realname) {
        setServer(main, bt, server, null, port, charset, nick, username, realname);
    }

    public Server(Rirca main, JTabbedPane bt, String server, String serverPass, int port, Charset charset, String[] nick, String username, String realname) {
        setServer(main, bt, server, serverPass, port, charset, nick, username, realname);
    }

    public Server(Rirca main, JTabbedPane bt, String server, int port, Charset charset, String nick, String username, String realname) {
        String[] tmp = new String[2];
        tmp[0] = nick;
        setServer(main, bt, server, null, port, charset, tmp, username, realname);
    }

    public Server(Rirca main, JTabbedPane bt, String server, String serverPass, int port, Charset charset, String nick, String username, String realname) {
        String[] tmp = new String[2];
        tmp[0] = nick;
        setServer(main, bt, server, serverPass, port, charset, tmp, username, realname);
    }

    public void setServer(Rirca main, JTabbedPane bt, String server, String serverPass, int port, Charset charset, String[] nick, String username, String realname) {
    }

    public void run() {
        in = new Input(server.get(0), port.get(0), Charset.forName(charset));
        int i;
        for (i = 1; true; i++) {
            if (in != null && in.getSocket() != null) {
                break;
            } else {
                if (i >= server.size()) {
                    i = 0;
                }
                in = new Input(server.get(i), port.get(i), Charset.forName(charset));
            }
        }
        out = new Output(in.getSocket(), Charset.forName(charset));
        out.write("NICK " + nick[0]);
        currNick = nick[0];
        out.write("USER " + username + " 0 * :" + realname);
        oi = new OutputInterpreter(out);
        serverTab = new BottomTab(main, bt, server.get(i));
        tt = serverTab.getJTabbedPane();
        statusTab = new TopTab(main, this, tt, currNick);
        ii = new InputInterpreter(main, this, out, tt, statusTab);
        String response;
        while ((response = in.readLine()) != null) {
            System.out.println(response);
            ii.parse(joinedChannel, openQuery, response);
        }
    }

    public void joinChannels() {
        for (int i = 0; i < channel.size(); i++) {
            joinChannel(channel.get(i));
        }
    }

    public void joinChannel(String chan) {
        out.write("JOIN " + chan);
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public Output getOut() {
        return out;
    }

    public OutputInterpreter getOutputInterpreter() {
        return oi;
    }

    public String getCurrNick() {
        return currNick;
    }

    public void setCurrNick(String currNick) {
        this.currNick = currNick;
    }

    public void setUmode(String umode) {
        this.umode = umode;
    }

    public void quit() {
        out.write("QUIT :gone");
        out.close();
        in.close();
    }
}
