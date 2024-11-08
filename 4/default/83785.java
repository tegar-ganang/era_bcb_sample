import java.io.*;
import java.util.*;
import java.sql.*;
import borknet_services.core.*;

public class H implements Modules {

    private Core C;

    private Server ser;

    private DBControl dbc;

    private String description = "";

    private String nick = "";

    private String ident = "";

    private String host = "";

    private String pass = "";

    private String numeric = "";

    private String num = "AAA";

    private String reportchan = "";

    private String channels[] = { "0", "0" };

    private ArrayList<Object> cmds = new ArrayList<Object>();

    private ArrayList<String> cmdn = new ArrayList<String>();

    private boolean[] queon = { false, false };

    private String[] quelast = { "0", "0" };

    private ArrayList<LinkedList<String>> queues = new ArrayList<LinkedList<String>>();

    private Faqs faqs;

    public H() {
    }

    public void start(Core C) {
        this.C = C;
        load_conf();
        numeric = C.get_numeric();
        faqs = new Faqs(C, this);
        dbc = new DBControl(C, this);
        ser = new Server(C, dbc, this);
        C.cmd_create_service(num, nick, ident, host, "+oXwkgsr", description);
        reportchan = C.get_reportchan();
        C.cmd_join(numeric, num, reportchan);
        queon = new boolean[channels.length];
        quelast = new String[channels.length];
        for (int n = 0; n < channels.length; n++) {
            C.cmd_join(numeric, num, channels[n]);
            queon[n] = false;
            quelast[n] = "0";
            queues.add(new LinkedList<String>());
        }
    }

    public void setCmnds(ArrayList<Object> cmds, ArrayList<String> cmdn) {
        this.cmds = cmds;
        this.cmdn = cmdn;
    }

    public ArrayList<Object> getCmds() {
        return cmds;
    }

    public ArrayList<String> getCmdn() {
        return cmdn;
    }

    public void stop() {
        C.cmd_kill_service(numeric + num, "Quit: Help! Bah! I don't need your help! I'm just fine th...oh cra...");
    }

    public void hstop() {
        C.cmd_kill_service(numeric + num, "Quit: Ooh, a cookie!.");
    }

    private void load_conf() {
        try {
            ConfLoader loader = new ConfLoader(C, "core/modules/" + this.getClass().getName().toLowerCase() + "/" + this.getClass().getName().toLowerCase() + ".conf");
            loader.load();
            Properties dataSrc = loader.getVars();
            description = dataSrc.getProperty("description");
            nick = dataSrc.getProperty("nick");
            ident = dataSrc.getProperty("ident");
            host = dataSrc.getProperty("host");
            pass = dataSrc.getProperty("pass");
            num = dataSrc.getProperty("numeric");
            channels = dataSrc.getProperty("channels").toLowerCase().split(",");
        } catch (Exception e) {
            C.printDebug("Error loading configfile.");
            C.debug(e);
            System.exit(0);
        }
    }

    public void parse(String msg) {
        try {
            ser.parse(msg);
        } catch (Exception e) {
            C.debug(e);
        }
    }

    public String get_num() {
        return numeric;
    }

    public String get_corenum() {
        return num;
    }

    public String get_nick() {
        return nick;
    }

    public String get_host() {
        return host;
    }

    public Faqs getFaqs() {
        return faqs;
    }

    public String getReportchan() {
        return reportchan;
    }

    public void clean() {
        faqs.cleanSessions();
    }

    public DBControl getDBC() {
        return dbc;
    }

    public void addQueue(String c) {
        for (int n = 0; n < channels.length; n++) {
            if (channels[n].equals(c.toLowerCase())) {
                queon[n] = true;
                String[] users = dbc.getChannelUsers(c);
                if (!users[0].equals("0")) {
                    for (String u : users) {
                        addUser(u, c);
                    }
                }
                return;
            }
        }
    }

    public void delQueue(String c) {
        for (int n = 0; n < channels.length; n++) {
            if (channels[n].equals(c.toLowerCase())) {
                queon[n] = false;
                return;
            }
        }
    }

    public void addUser(String u, String c) {
        for (int n = 0; n < channels.length; n++) {
            if (channels[n].equals(c.toLowerCase())) {
                queues.get(n).offer(u);
                return;
            }
        }
    }

    public String getUser(String c) {
        for (int n = 0; n < channels.length; n++) {
            if (channels[n].equals(c.toLowerCase())) {
                quelast[n] = queues.get(n).poll();
                return quelast[n];
            }
        }
        return "0";
    }

    public String getPrevUser(String c) {
        for (int n = 0; n < channels.length; n++) {
            if (channels[n].equals(c.toLowerCase())) {
                return quelast[n];
            }
        }
        return "0";
    }

    public boolean onChan(String c) {
        for (int n = 0; n < channels.length; n++) {
            if (channels[n].equals(c.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasQueue(String c) {
        for (int n = 0; n < channels.length; n++) {
            if (channels[n].equals(c.toLowerCase())) {
                return queon[n];
            }
        }
        return false;
    }

    public void reop(String chan) {
        if (onChan(chan)) {
            C.cmd_mode(numeric, numeric + num, chan, "+o");
        }
    }
}
