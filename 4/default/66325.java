import java.util.ArrayList;

public class Configuration {

    private String progname = "rirca";

    private String version = "0.0.10";

    private boolean debug = false;

    private Input in;

    private Output out;

    private String[] args;

    private String configfile = "config";

    private int defaultWidth = 1024;

    private int defaultHeight = 768;

    private String[] nick = new String[2];

    private String[] nickPass = new String[2];

    private String username;

    private String realname;

    private String localhost;

    private int width = defaultWidth;

    private int height = defaultHeight;

    private ArrayList<Group> group = new ArrayList<Group>();

    public void getConfig() {
        String line;
        in = new Input(configfile);
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("group[")) {
                group.add(new Group());
                ((Group) group.get(group.size() - 1)).setGroupName(line.substring(line.indexOf("[") + 1, line.indexOf("]")));
            }
        }
        in.close();
        in = new Input(configfile);
        line = "";
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.indexOf("=") != -1) {
                String fPart = line.substring(0, line.indexOf("="));
                String mPart = line.substring(line.indexOf("="), line.indexOf("=") + 1);
                String lPart = line.substring(line.indexOf("=") + 1);
                line = fPart.trim() + mPart.trim() + lPart.trim();
            }
            if (!(line.startsWith("/"))) {
                if (line.startsWith("nick[") && line.charAt(7) != '[') {
                    nick[Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]")))] = line.substring(line.indexOf("=") + 1);
                    if (debug) {
                        System.err.println("nick[" + Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))) + "]: " + nick[Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]")))]);
                    }
                } else if (line.startsWith("nickPass[") && line.charAt(11) != '[') {
                    nickPass[Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]")))] = line.substring(line.indexOf("=") + 1);
                    if (debug) {
                        System.err.println("nickPass[" + Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))) + "]: " + nickPass[Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]")))]);
                    }
                } else if (line.startsWith("username=")) {
                    username = line.substring(line.indexOf("=") + 1);
                    if (debug) {
                        System.err.println("username: " + username);
                    }
                } else if (line.startsWith("realname=")) {
                    realname = line.substring(line.indexOf("=") + 1);
                    if (debug) {
                        System.err.println("realname: " + realname);
                    }
                } else if (line.startsWith("localhost=")) {
                    localhost = line.substring(line.indexOf("=") + 1);
                    if (debug) {
                        System.err.println("localhost: " + localhost);
                    }
                } else if (line.startsWith("size=")) {
                    width = Integer.parseInt(line.substring(line.indexOf("=") + 1, line.indexOf("x")));
                    height = Integer.parseInt(line.substring(line.indexOf("x") + 1));
                    if (debug) {
                        System.err.println("width: " + width);
                        System.err.println("height: " + height);
                    }
                } else if (line.startsWith("server[")) {
                    ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).addServer(line.substring(line.indexOf("=") + 1));
                    if (debug) {
                        System.err.println("server[" + Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))) + "][" + Integer.parseInt(line.substring(line.indexOf("[", line.indexOf("[") + 1) + 1, line.indexOf("]", line.indexOf("]") + 1))) + "]: " + ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).getServers().get(Integer.parseInt(line.substring(line.indexOf("[", line.indexOf("[") + 1) + 1, line.indexOf("]", line.indexOf("]") + 1)))));
                    }
                } else if (line.startsWith("port[")) {
                    ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).addPort(Integer.parseInt(line.substring(line.indexOf("=") + 1)));
                    if (debug) {
                        System.err.println("port[" + Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))) + "][" + Integer.parseInt(line.substring(line.indexOf("[", line.indexOf("[") + 1) + 1, line.indexOf("]", line.indexOf("]") + 1))) + "]: " + ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).getPorts().get(Integer.parseInt(line.substring(line.indexOf("[", line.indexOf("[") + 1) + 1, line.indexOf("]", line.indexOf("]") + 1)))));
                    }
                } else if (line.startsWith("serverPass[")) {
                    ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).addServerPass(line.substring(line.indexOf("=") + 1));
                    if (debug) {
                        System.err.println("serverPass[" + Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))) + "][" + Integer.parseInt(line.substring(line.indexOf("[", line.indexOf("[") + 1) + 1, line.indexOf("]", line.indexOf("]") + 1))) + "]: " + ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).getServerPasses().get(Integer.parseInt(line.substring(line.indexOf("[", line.indexOf("[") + 1) + 1, line.indexOf("]", line.indexOf("]") + 1)))));
                    }
                } else if (line.startsWith("charset[")) {
                    ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).setCharset(line.substring(line.indexOf("=") + 1));
                    if (debug) {
                        System.err.println("charset[" + Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))) + "]: " + ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).getCharset());
                    }
                } else if (line.startsWith("channel[")) {
                    ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).addChannel(line.substring(line.indexOf("=") + 1));
                    if (debug) {
                        System.err.println("channel[" + Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))) + "][" + Integer.parseInt(line.substring(line.indexOf("[", line.indexOf("[") + 1) + 1, line.indexOf("]", line.indexOf("]") + 1))) + "]: " + ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).getChannels().get(Integer.parseInt(line.substring(line.indexOf("[", line.indexOf("[") + 1) + 1, line.indexOf("]", line.indexOf("]") + 1)))));
                    }
                } else if (line.startsWith("nick[") && line.charAt(7) == '[') {
                    ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).setNick(Integer.parseInt(line.substring(line.indexOf("[", line.indexOf("[") + 1) + 1, line.indexOf("]", line.indexOf("]") + 1))), line.substring(line.indexOf("=") + 1));
                    if (debug) {
                        System.err.println("nick[" + Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))) + "][" + Integer.parseInt(line.substring(line.indexOf("[", line.indexOf("[") + 1) + 1, line.indexOf("]", line.indexOf("]") + 1))) + "]: " + ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).getNick(Integer.parseInt(line.substring(line.indexOf("[", line.indexOf("[") + 1) + 1, line.indexOf("]", line.indexOf("]") + 1)))));
                    }
                } else if (line.startsWith("nickPass[") && line.charAt(11) == '[') {
                    ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).setNickPass(Integer.parseInt(line.substring(line.indexOf("[", line.indexOf("[") + 1) + 1, line.indexOf("]", line.indexOf("]") + 1))), line.substring(line.indexOf("=") + 1));
                    if (debug) {
                        System.err.println("nickPass[" + Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))) + "][" + Integer.parseInt(line.substring(line.indexOf("[", line.indexOf("[") + 1) + 1, line.indexOf("]", line.indexOf("]") + 1))) + "]: " + ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).getNickPass(Integer.parseInt(line.substring(line.indexOf("[", line.indexOf("[") + 1) + 1, line.indexOf("]", line.indexOf("]") + 1)))));
                    }
                } else if (line.startsWith("username[")) {
                    ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).setUsername(line.substring(line.indexOf("=") + 1));
                    if (debug) {
                        System.err.println("username[" + Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))) + "]: " + ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).getUsername());
                    }
                } else if (line.startsWith("realname[")) {
                    ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).setRealname(line.substring(line.indexOf("=") + 1));
                    if (debug) {
                        System.err.println("realname[" + Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))) + "]: " + ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).getRealname());
                    }
                } else if (line.startsWith("autoConnect[")) {
                    ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).setAutoConnect(Boolean.parseBoolean(line.substring(line.indexOf("=") + 1)));
                    if (debug) {
                        System.err.println("autoConnect[" + Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))) + "]: " + ((Group) group.get(Integer.parseInt(line.substring(line.indexOf("[") + 1, line.indexOf("]"))))).getAutoConnect());
                    }
                }
            }
        }
        in.close();
    }

    public void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--config")) {
                configfile = args[i + 1];
            } else if (args[i].startsWith("--help")) {
                System.out.println("rirca [--config <configfile>] [--help]");
                System.out.println("      --config <configfile>   Use <configfile> instead of the default config file.");
                System.out.println("      --help                  Show this help and exit.");
                System.exit(0);
            }
        }
    }

    public void clear() {
        nick[0] = "";
        nick[1] = "";
        nickPass[0] = "";
        nickPass[1] = "";
        username = "";
        realname = "";
        localhost = "";
        width = defaultWidth;
        height = defaultHeight;
        group.clear();
    }

    public String getProgname() {
        return progname;
    }

    public String getVersion() {
        return version;
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

    public String getLocalhost() {
        return localhost;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ArrayList getGroups() {
        return group;
    }

    public Group getGroup(int index) {
        return group.get(index);
    }

    public String getGroupName(int groupIndex) {
        return group.get(groupIndex).getGroupName();
    }

    public String getCharset(int groupIndex) {
        return group.get(groupIndex).getCharset();
    }

    public ArrayList<String> getServers(int groupIndex) {
        return group.get(groupIndex).getServers();
    }

    public ArrayList<String> getServerPasses(int groupIndex) {
        return group.get(groupIndex).getServerPasses();
    }

    public ArrayList<Integer> getPorts(int groupIndex) {
        return group.get(groupIndex).getPorts();
    }

    public ArrayList<String> getChannels(int groupIndex) {
        return group.get(groupIndex).getChannels();
    }

    public String[] getNicks(int groupIndex) {
        return group.get(groupIndex).getNicks();
    }

    public String getNick(int groupIndex, int index) {
        return group.get(groupIndex).getNick(index);
    }

    public String[] getNickPasses(int groupIndex) {
        return group.get(groupIndex).getNickPasses();
    }

    public String getNickPass(int groupIndex, int index) {
        return group.get(groupIndex).getNickPass(index);
    }

    public String getUsername(int groupIndex) {
        return group.get(groupIndex).getUsername();
    }

    public String getRealname(int groupIndex) {
        return group.get(groupIndex).getRealname();
    }

    public boolean getAutoConnect(int groupIndex) {
        return group.get(groupIndex).getAutoConnect();
    }
}
