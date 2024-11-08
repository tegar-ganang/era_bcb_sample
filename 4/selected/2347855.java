package openjirc;

import openjirc.plugin.*;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 *
 * OJIRC class , base of the application.<br>
 * Dispatches all messages to other classes and handles major events<BR>
 *
 * OJIRC klass on peamine klass kogu aplikatsioonis.<br>
 * Kogu suhtlemine programmis k�ib l�bi selle klassi, samuti k�sitleb ta peamisi<br>
 * s�ndmusi nagu serveriga �henduse saavutamine , ruumide vaheline liikumine jne.
 *
 */
public class OJIRC {

    Vector params = new Vector();

    Vector channels = new Vector();

    static final String versionString = "0.3";

    static final String config_file = "ojirc.cfg";

    static final String user_config_file = "user.cfg";

    static final String default_server = "irc.debian.org";

    static final int default_server_port = 6667;

    static final String default_nick = "newbie";

    static boolean connected = false;

    Interpreter myInterpreter = new Interpreter();

    Socket connection = null;

    IUI user_interface = null;

    boolean user_has_config = false;

    /**
   * Internal class for holding runtime parameters like server info & user info.<br>
   * Sisemine klass parameetrite hoidmiseks<br>
   */
    class CFGParam {

        public String name;

        public String value;

        public CFGParam(String t_name, String t_value) {
            super();
            name = t_name;
            value = t_value;
        }

        public String toString() {
            return name + "=" + value + "\n";
        }
    }

    /**
   * The constructor.<br>
   * Konstruktor.
   */
    public OJIRC() {
        super();
        boolean ucf = false;
        System.out.println("Checking if user config file exists ...");
        try {
            if ((new File(user_config_file)).exists()) ucf = true;
        } catch (Exception e) {
            System.out.println("Failed to check user config file");
            ucf = false;
        }
        System.out.println("Trying to read settings file " + ((ucf) ? user_config_file : config_file));
        user_has_config = ucf;
        loadData();
        checkMissingData();
        createUI();
        if (user_interface != null) {
            user_interface.init();
        }
    }

    /**
   * Set's the UserInterface to the new user interface.<br>
   *
   * S�testab kasutajaliidese.
   *
   * @param new_interface uus kasutajaliides / the new user interface
   */
    public void setUI(IUI new_interface) {
        user_interface = new_interface;
    }

    /**
	 * The Main Function Which Starts The Aplication.<BR>
	 *
	 * Programmi k�ivitamiseks vajalik main funktsioon.<br>
	 *
	 * @param args[] k�surea argumendid / command line parameters
	 */
    public static void main(String[] args) {
        OJIRC client;
        System.out.println("OJ IRC is starting up ...");
        System.out.println("OJ IRC version " + versionString);
        if (args.length > 0) {
        }
        client = new OJIRC();
    }

    /**
	 * Adds a parameter or replaces it if it already exists.<br>
	 * Lisab parameetri v�i asendab selle kui see juba eksisteerib.<br>
	 *
	 * @param t_nam parameetri v�ti / parameter key
	 * @param t_val parameetri v��rtus / parameter value
	 */
    public void addParam(String t_nam, String t_val) {
        removeParam(t_nam);
        params.addElement(new CFGParam(t_nam, t_val));
    }

    /**
	 * Removes a parameter.<br>
	 * Eemaldab parameetri.
	 *
	 * @param t_nam eemaldatava parameetri v�ti / key of the removable parameter.
	 *
	 */
    public void removeParam(String t_nam) {
        for (int i = 0; i < params.size(); i++) {
            if (params.elementAt(i) instanceof CFGParam && (((CFGParam) params.elementAt(i)).name).equals(t_nam)) {
                params.removeElementAt(i);
                break;
            }
        }
    }

    /**
	 * Returns the value of the parameter.<br>
	 * Tagastab parameetri v��rtuse.<br>
	 *
	 *
	 * @param param_name parameetri v�ti / key of the parameter
	 *
	 * @return parameetri v��rtus / the value of the parameter
	 */
    public String getParamValue(String param_name) {
        for (int i = 0; i < params.size(); i++) {
            if (params.elementAt(i) instanceof CFGParam && (((CFGParam) params.elementAt(i)).name).equals(param_name)) {
                return ((CFGParam) params.elementAt(i)).value;
            }
        }
        return null;
    }

    /**
	 * Reads configuration data from the config file "ojirc.cfg".<br>
	 * Loeb konfiguratsiooni parameetrid konfiguratsiooni failist "ojirc.cfg".<br>
	 */
    public void loadData() {
        try {
            String fname = user_has_config ? user_config_file : config_file;
            BufferedReader datareader = new BufferedReader(new FileReader(config_file));
            String line;
            while ((line = datareader.readLine()) != null) {
                if (line.indexOf("=") > 0) {
                    addParam(line.substring(0, line.indexOf("=")), line.substring(line.indexOf("=") + 1, line.length()));
                }
            }
            System.out.println();
            datareader.close();
        } catch (Exception e) {
            System.out.println("Warning! Could not open config file: " + config_file);
            try {
                System.out.println((new File(config_file)).getCanonicalPath());
            } catch (Exception es) {
            }
        }
    }

    /**
   * Saves the configuration data.<br>
   * Salvestab hetke parameetrid konfiguratsiooni faili.<br>
   * @see #loadData
   */
    public void saveData() {
        try {
            BufferedWriter databuf = new BufferedWriter(new FileWriter(user_config_file));
            for (int i = 0; i < params.size(); i++) {
                databuf.write(params.get(i).toString());
            }
            databuf.close();
        } catch (IOException e) {
            System.out.println("An IO error occurred while saving configuration: " + e.getMessage());
        }
    }

    /**
   * Checks if some configuration data is missing.<br>
   * Kontrollib kas kogu vajalik konfiguratsiooni andmestik on olemas.<br>
   */
    public void checkMissingData() {
    }

    /**
	 * Creates the user interface and loads all classes it needs.<br>
	 * <br>
	 * Loob kasutajaliidese ning laadib vajalikud klassid.<br>
	 */
    public void createUI() {
        String classname = getParamValue("user_interface");
        ClassLoader l = this.getClass().getClassLoader();
        try {
            Object newinterface = l.loadClass(classname).newInstance();
            if (newinterface instanceof IUI) {
                ((IUI) newinterface).setListener(generateUIListener());
                setUI((IUI) newinterface);
            } else {
                System.out.println(" FATAL ERROR ! USER INTERFACE NOT INSTANCE OF IUI ");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(" FATAL ERROR ! COULD NOT FIND USER INTERFACE'S CLASS! ");
            System.exit(1);
        }
    }

    /**
	 * doMessageRead handles the IRCMessage read from server.
	 * doMessageRead k�sitleb serverist loetud IRCMessage tyypi objekti
	 *
	 * @param m IRCMessage serverist / IRCMessage from the server
	 */
    public void doMessageRead(IRCMessage m) {
        if (m.getCommand().equals("" + OJConstants.ID_RPL_NAMEREPLY)) {
            for (int i = 0; i < m.getParameterCount(); i++) {
                if ((m.getParameter(i).trim().charAt(0) == '#')) {
                    String memberslist = m.getParameter(i + 1);
                    memberslist = memberslist.substring(0).trim();
                    if (memberslist.length() > 1) {
                        String members[] = split(memberslist);
                        addMembersToChannel(members, m.getParameter(i));
                    }
                    break;
                }
            }
        } else if (m.getCommand().equals(OJConstants.ID_CLIENT_JOIN)) {
            if (m.getFromNick().equals(getParamValue("nickname"))) {
                String channels = m.getParameter(0);
                if (channels.indexOf(',') < 0) {
                    addChannel(channels);
                    try {
                        user_interface.joinedChannel(channels);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    for (int i = 0; i < channels.length(); i++) if (channels.charAt(i) == ',') channels = channels.substring(0, i) + " " + channels.substring(i + 1);
                    String arr[] = split(channels);
                    for (int i = 0; i < arr.length; i++) {
                        addChannel(arr[i]);
                        try {
                            user_interface.joinedChannel(arr[i]);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                addMembersToChannel(new String[] { m.getFromNick() }, m.getParameter(0));
                try {
                } catch (Exception e) {
                }
            }
        } else if (m.getCommand().equals(OJConstants.ID_CLIENT_PART)) {
            String channels = m.getParameter(0);
            if (channels.indexOf(',') < 0) {
                removeChannel(channels);
            } else {
                for (int i = 0; i < channels.length(); i++) if (channels.charAt(i) == ',') channels = channels.substring(0, i) + " " + channels.substring(i + 1);
                String arr[] = split(channels);
                for (int i = 0; i < arr.length; i++) {
                    removeChannel(arr[i]);
                }
            }
        }
        if (user_interface != null) {
            user_interface.executeCommand(m);
        }
    }

    /**
   * Splits an String into an String array using space as an separator.<br>
   * L�igub s�ne (Stringi) tykkideks tyhikute j�rgi ning tagastab selle massiivina.<br>
   *
   * @param splitable l�igutav s�ne / the String to split<br>
   *
   * @return l�igutud s�nede massiivi / the String splitted as an array<br>
   */
    private static String[] split(String splitable) {
        Vector dv = new Vector();
        splitable = splitable.trim();
        String current = "";
        for (int j = 0; j < splitable.length(); j++) {
            if (splitable.charAt(j) == ' ') {
                if (!current.trim().equals("")) {
                    dv.addElement(new String(current));
                }
                current = "";
            } else if (j < splitable.length() - 1) {
                current = current + splitable.charAt(j);
            } else {
                current = current + splitable.charAt(j);
                if (!current.trim().equals("")) {
                    dv.addElement(new String(current));
                }
            }
        }
        String arr[] = new String[dv.size()];
        for (int i = 0; i < dv.size(); i++) {
            arr[i] = (String) dv.elementAt(i);
        }
        return arr;
    }

    /**
   * Connects the application to an irc server specified by previous parameters.<br>
   * Yhendab programmi irc v�rgu serveriga.
   *
   */
    public void doConnect() {
        try {
            Socket connector = new Socket(getParamValue("server"), new Integer(getParamValue("serverport")).intValue());
            connection = connector;
            myInterpreter.init(connector.getInputStream(), connector.getOutputStream());
            myInterpreter.setOJIRC(this);
            System.out.println("Connected !");
            connected = true;
        } catch (Exception e) {
            System.out.println("Socket error: " + e.getMessage());
        }
    }

    /**
   * Disconnects the user from the server.<br>
   * L�petab yhenduse serveriga.<br>
   *
   *
   */
    public void doDisconnect() {
        myInterpreter.sendData(new IRCMessage(OJConstants.ID_CLIENT_QUIT));
        try {
            Thread.currentThread().sleep(2500);
            System.out.println("Closing connection");
            connection.shutdownInput();
            connection.shutdownOutput();
            connection.close();
            System.out.println("Closed connection");
        } catch (Exception e) {
            System.out.println("Socket error: " + e.getMessage());
        }
        connected = false;
    }

    /**
   * Logs in into the server after connecting.<br>
   * Autoriseerib kasutaja serveris.<br>
   */
    public void doLogin() {
        IRCMessage logdata;
        logdata = new IRCMessage("PASS", new String[] { getParamValue("password") });
        myInterpreter.sendData(logdata);
        logdata = new IRCMessage("NICK", new String[] { getParamValue("nickname") });
        myInterpreter.sendData(logdata);
        logdata = new IRCMessage("USER", new String[] { getParamValue("user"), "8", "*", ":Martini" });
        myInterpreter.sendData(logdata);
    }

    /**
   * Constructs an IRCMessage from the line entered by user.<br>
   * Konstrueerib IRCMessage kasutaja sisestatud reast.<br>
   *
   * @param line Sisestatud rida / The line typed.<br>
   *
   * @return koostatud IRCMessage / the IRCMessage that was constructed.
   */
    public IRCMessage doConstructMessage(String line) {
        IRCMessage resp = null;
        resp = OJMessageProcessor.ProcessMessage(line);
        return resp;
    }

    /**
   * Generated the UIListener for the OJIRC instance.<br>
   * Genereerib UIListeneri OJIRC objekti jaoks.<br>
   * @return UIListener, mis kuulab k�sitleb teateid / UIListener for messages handling.
   */
    public UIListener generateUIListener() {
        return new UIListener() {

            public void sendCommand(IRCMessage m) {
                myInterpreter.sendData(m);
            }

            public boolean isValidCommand(IRCMessage m) {
                if (m == null) return false;
                return true;
            }

            public IRCMessage constructMessage(String commandline) {
                return doConstructMessage(commandline);
            }

            public void connect() {
                doConnect();
            }

            public void disconnect() {
                doDisconnect();
            }

            public void setParameter(String key, String val) {
                addParam(key, val);
            }

            public String getParameter(String key) {
                return getParamValue(key);
            }

            public void saveParams() {
                saveData();
            }

            public void login() {
                if (connected) doLogin();
            }

            public OJIRCChannel getChannel(String ch) {
                return getTheChannel(ch);
            }

            public String[] getChannels() {
                return getChannelsList();
            }

            public String[] getChannelMembers(String chname) {
                return getChannelMembersNicknames(chname);
            }

            public int getChannelCount() {
                return getChannelsCount();
            }

            public boolean getConnected() {
                return connected;
            }
        };
    }

    /**
	 * Returns the list of channels as an String array.<br>
	 * Tagastab kanalite nimed s�nede massiivina.<br>
	 *
	 * @return aktiivsed irc kanalid / active irc channels.<br>
	 */
    public String[] getChannelsList() {
        String chans[] = new String[getChannelsCount()];
        for (int i = 0; i < channels.size(); i++) {
            chans[i] = new String(((OJIRCChannel) channels.elementAt(i)).getChannelName());
        }
        return chans;
    }

    /**
	 * Same as getChannelsList / Sama mis getChannelsList.<br>
	 * @see #getChannelsList()
	 */
    public String[] getChannels() {
        return getChannelsList();
    }

    /**
	 * Gets the members of the Channel.<br>
	 * Annab kanali liikmete nimekirja.<br>
	 * @param channame kanali nimi / channel's name
	 * @return Kanali liikmed / Members of The Channel channame
	 */
    public String[] getChannelMembersNicknames(String channame) {
        return getTheChannel(channame).getMembersNickNames();
    }

    /**
	 * Get the count of IRC Channels in the application.<br>
	 *
	 * Tagastab IRC kanalite arvu programmis.<br>
	 *
	 * @return kanalite arv / count of channels.
	 */
    public int getChannelsCount() {
        return channels.size();
    }

    /**
	 * Adds a new channel to the channel's list.<br>
	 * Lisab uue kanali kanalite nimekirja.<br>
	 *
	 * @param channame kanali nimi / name of the channel to add<br>
	 */
    public void addChannel(String channame) {
        channels.add(new OJIRCChannel(channame));
    }

    /**
	 * Removes the channel from the channel's list.<br>
	 * Eemaldab kanali kanalite nimekirjast.<br>
	 *
	 * @param channame eemaldatava nimi / name of the channel to remove <br>
	 *
	 */
    public void removeChannel(String channame) {
        for (int i = 0; i < channels.size(); i++) {
            if (((OJIRCChannel) channels.elementAt(i)).getName().equals(channame)) {
                channels.removeElementAt(i);
                break;
            }
        }
    }

    /**
	 * Find the channel called channame.<br>
	 * Leiab ning tagastab kanali vastava nime j�rgi.
	 *
	 * @param channame kanali nimi / name of the channel to find<br>
	 */
    public OJIRCChannel getTheChannel(String channame) {
        for (int i = 0; i < getChannelsCount(); i++) {
            if (((OJIRCChannel) channels.elementAt(i)).getChannelName().equals(channame)) return (OJIRCChannel) channels.elementAt(i);
        }
        return null;
    }

    /**
	 * Adds members to a channel.<br>
	 * Lisab kanalile liikmeid.<br>
	 *
	 * @param username[] kasutajanimete hyydnimed / nicknames of users to add to the channel<br>
	 * @param channelname kanali nimi / name of the channel <br>
	 *
	 */
    public void addMembersToChannel(String username[], String channelname) {
        OJIRCChannel c = getTheChannel(channelname);
        try {
            user_interface.memberJoinedChannel(username, channelname);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (c == null || username.length < 1) return;
        for (int i = 0; i < username.length; i++) {
            c.addMember(new OJIRCMember(username[i], "", ""));
        }
    }

    /**
	 * Removes a member from a channel.<br>
	 * Eemaldab liikme kanalilt.<br>
	 *
	 * @param username kasutaja hyydnimi / nickname of the user
	 * @param channelname kanali nimi / name of the channel
	 */
    public void removeMemberFromChannel(String username, String channelname) {
        OJIRCChannel c = getTheChannel(channelname);
        if (c == null) return;
        c.removeMember(new OJIRCMember(username, "", ""));
    }

    /**
	 * Removes the user from all channels (for example he has quit irc).<br>
	 * Eemaldab kasutaja k�ikidelt kanalitelt.<BR>
	 *
	 * @param kasutaja hyydnimi / nickname of the user.<br>
	 */
    public void removeMemberFromAll(String username) {
        for (int i = 0; i < channels.size(); i++) {
            ((OJIRCChannel) channels.elementAt(i)).removeMember(new OJIRCMember(username, "", ""));
        }
    }
}
