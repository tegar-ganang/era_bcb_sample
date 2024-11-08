import java.util.ArrayList;

public class Group {

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

    private boolean autoConnect = false;

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public void addServer(String server) {
        this.server.add(server);
    }

    public void addServerPass(String serverPass) {
        this.serverPass.add(serverPass);
    }

    public void addPort(int port) {
        this.port.add(port);
    }

    public void addChannel(String channel) {
        this.channel.add(channel);
    }

    public void setNick(int index, String nick) {
        this.nick[index] = nick;
    }

    public void setNickPass(int index, String nickPass) {
        this.nickPass[index] = nickPass;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRealname(String realname) {
        this.realname = realname;
    }

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getCharset() {
        return charset;
    }

    public ArrayList<String> getServers() {
        return server;
    }

    public ArrayList<String> getServerPasses() {
        return serverPass;
    }

    public ArrayList<Integer> getPorts() {
        return port;
    }

    public ArrayList<String> getChannels() {
        return channel;
    }

    public String[] getNicks() {
        return nick;
    }

    public String getNick(int index) {
        return nick[index];
    }

    public String[] getNickPasses() {
        return nickPass;
    }

    public String getNickPass(int index) {
        return nickPass[index];
    }

    public String getUsername() {
        return username;
    }

    public String getRealname() {
        return realname;
    }

    public boolean getAutoConnect() {
        return autoConnect;
    }
}
