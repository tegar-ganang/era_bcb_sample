package openjirc.plugin;

import java.awt.*;
import java.util.*;
import java.awt.event.*;
import openjirc.*;

/**
 *
 * English: This is the Basic Graphical User Interface class which is the default interface for the <BR>
 * OJIRC application<BR>
 * <BR>&nbsp;<BR>
 * Eesti Keeles : See on algse graafilise kasutaja liidese klass , mis on vaikimisi OJIRC programmi <BR>
 * kasutajaliides. <BR>
 *
 */
public class BasicGUI extends java.awt.Frame implements IUI {

    public static final int CHAT_HISTORY = 300;

    public static final String CONSOLE_CHANNEL_NAME = "@Console";

    UIListener listener = null;

    boolean disconnected = true;

    ChatChannel currentChannel = null;

    Vector channels = new Vector();

    Vector knownChannels = new Vector();

    private int mode = MEMBERMODE;

    private static final int CHANNELMODE = 1;

    private static final int MEMBERMODE = 2;

    Image dbler = null;

    public void invalidate() {
        super.invalidate();
        dbler = null;
    }

    private class ChatChannel {

        public boolean isprivate = false;

        public boolean isConsole = false;

        public String channelName = "";

        public String chatHistory = "";

        public String topic = "<no topic>";

        public String members[] = new String[0];

        public String toString() {
            return channelName;
        }
    }

    Label titleLabel = new Label();

    Label membersLabel = new Label();

    TextField textin = new TextField();

    TextArea textout = new TextArea("", 1, 1, TextArea.SCROLLBARS_VERTICAL_ONLY);

    java.awt.List memberslist = new java.awt.List();

    Panel bg = new Panel();

    Choice viewChoice = new Choice();

    Button configButton = new Button();

    Choice channelChoice = new Choice();

    Color mainbg = new Color(100, 100, 125);

    Color textbg = new Color(0, 0, 0);

    Color textfg = new Color(255, 255, 250);

    Font font_normal = new Font("SansSerif", 0, 12);

    Font font_title = new Font("SansSerif", 1, 15);

    Font font_sub_title = new Font("SansSerif", 2, 12);

    Button connect_btn = new Button("connect");

    Button leaveChannelBtn = new Button("leave channel");

    /**
   * English: Constructs a new BasiGUI<BR>
   * Eesti keeles: Konstrueerib uue BasicGUI tyypi objekti.
   */
    public BasicGUI() {
        super();
    }

    /**
   * English : initalizes the BasicGUI and it's components<BR>
   *
   * Eesti Keeles : initsialiseerib BasicGUI graafilised vahendid
   */
    public void init() {
        this.setSize(new Dimension(600, 400));
        this.setResizable(false);
        this.setBackground(mainbg);
        this.setLayout(new BorderLayout());
        this.setTitle("OJIRC - BasicGUI beta");
        bg.setBackground(mainbg);
        this.add(bg, BorderLayout.CENTER);
        bg.setLayout(null);
        titleLabel.setBounds(new Rectangle(5, 5, 280, 30));
        titleLabel.setText("OJIRC - BasicGUI beta");
        titleLabel.setFont(font_title);
        titleLabel.setForeground(Color.white);
        titleLabel.setBackground(mainbg);
        bg.add(titleLabel, null);
        textin.setBounds(new Rectangle(5, 350, 500, 25));
        textin.setForeground(textfg);
        textin.setBackground(textbg);
        textin.setFont(font_normal);
        FontMetrics k = textin.getFontMetrics(textin.getFont());
        textin.setSize(textin.getSize().width, k.getHeight() + 6);
        bg.add(textin, null);
        connect_btn.setBounds(510, 350, 80, 20);
        connect_btn.setFont(font_normal);
        connect_btn.setForeground(Color.black);
        connect_btn.setBackground(mainbg);
        bg.add(connect_btn, null);
        textout.setBounds(new Rectangle(5, 35, 450, 305));
        textout.setEditable(false);
        textout.setBackground(textbg);
        textout.setForeground(textbg);
        textout.setFont(font_normal);
        bg.add(textout, null);
        configButton.setBounds(320, 5, 120, 20);
        configButton.setLabel("Configuration");
        configButton.setFont(font_normal);
        configButton.setForeground(Color.black);
        configButton.setBackground(mainbg);
        bg.add(configButton, null);
        channelChoice.setBounds(new Rectangle(460, 5, 130, 30));
        channelChoice.setFont(font_normal);
        channelChoice.setForeground(Color.black);
        bg.add(channelChoice, null);
        memberslist.setBounds(new Rectangle(460, 60, 130, 275));
        memberslist.setForeground(textfg);
        memberslist.setBackground(textbg);
        memberslist.setFont(font_normal);
        bg.add(memberslist, null);
        leaveChannelBtn.setBounds(460, 35, 130, 20);
        leaveChannelBtn.setFont(font_normal);
        leaveChannelBtn.setForeground(Color.black);
        leaveChannelBtn.setBackground(mainbg);
        bg.add(leaveChannelBtn, null);
        addListeners();
        this.show();
        repaint();
        textout.setCaretPosition(32000);
        createConsoleChannel();
    }

    /**
   * English: Attaches the listener (the main program) to the interface<BR>
   * Eesti keeles: Seob kasutajaliidese IUIga.
   */
    public void setListener(UIListener l) {
        listener = l;
    }

    public void showOJIRCMessage(String message, int id) {
        addTextToChat("OJIRC [" + getTime() + "]");
    }

    public void disconnected() {
        disconnected = true;
        addTextToChat("! DISCONNECTED ! ==== ! DISCONNECTED ! ==[" + getTime() + "] == ! DISCONNECTED ! ==== ! DISCONNECTED !");
        System.exit(0);
    }

    public void connecteced() {
        addTextToChat("[OJIRC] Connection to server is done :) ");
    }

    public void loggedIn() {
        addTextToChat("[OJIRC] Logged in");
    }

    public String getTime() {
        return Calendar.getInstance().getTime().toString();
    }

    public String getShortTime() {
        Calendar obj = Calendar.getInstance();
        int h = obj.get(Calendar.HOUR_OF_DAY);
        int m = obj.get(Calendar.MINUTE);
        return ((h < 10) ? "0" : "") + h + ":" + ((m < 10) ? "0" : "") + m;
    }

    /**
   * English: Processes an incoming IRC message according to interface<BR>
   * Eesti keeles: Tegeleb sissetuleva IRC teatega.
   * @param message the incoming IRC message object / sissetulev IRC teate objekt
   */
    public void executeCommand(IRCMessage message) {
        int reply = 0;
        try {
            reply = Integer.parseInt(message.getCommand());
            processReply(reply, message);
            return;
        } catch (NumberFormatException e) {
        }
        String cmd = message.getCommand();
        if (cmd.equals(OJConstants.ID_CLIENT_PRIVMSG)) {
            if (message.getParameter(0).equals(listener.getParameter("nickname"))) {
                ChatChannel x = findChannel(message.getFromNick());
                if (x == null) {
                    x = createChannel(message.getFromNick(), true, true);
                    addTextToChannel("Private chat with " + message.getFromNick(), x.channelName);
                    addTextToChannel(constructPrivateMessage(message.getFromNick(), message.getJoinedParameter(1)), x.channelName);
                } else {
                    addTextToChannel(constructPrivateMessage(message.getFromNick(), message.getJoinedParameter(1)), message.getFromNick());
                }
            } else {
                ChatChannel x = findChannel(message.getParameter(0));
                if (x != null) {
                    addTextToChannel(constructPrivateMessage(message.getFromNick(), message.getJoinedParameter(1)), x.channelName);
                }
            }
        } else if (cmd.equals(OJConstants.ID_CLIENT_TOPIC)) {
            setTopicOnChannel(message.getParameter(0), message.getParameter(1));
        } else if (cmd.equals(OJConstants.ID_CLIENT_NOTICE)) {
            addTextToChannel(message.getJoinedParameter(1), currentChannel.channelName);
        }
    }

    private void setTopicOnChannel(String ch, String top) {
        ChatChannel p = findChannel(ch);
        if (p != null) {
            p.topic = top;
            if (currentChannel.equals(p)) {
                titleLabel.setText(top);
                repaint();
            }
        }
    }

    /**
   * English: Generates the form used for private messages<BR>
   * Eesti keeles: Formaadib personaalse teate.
   * @param from nickname of sender / saatja nimi
   * @param text private message text / teate sisu
   */
    public String constructPrivateMessage(String from, String text) {
        return "[" + getShortTime() + "] <" + from + "> " + text;
    }

    /**
   * English: Takes specific action according to server replies<BR>
   * Eesti keeles: Tegutseb vastavalt serverilt saadud vastustele.
   * @param reply numeric code of reply received / vastase numbriline kood
   * @param m object of message received / saabunud teate objekt
   */
    public void processReply(int reply, IRCMessage m) {
        switch(reply) {
            case OJConstants.ID_RPL_LIST:
                if (m.getParameter(1).length() > 1) memberslist.add(m.getParameter(1));
                break;
            case OJConstants.ID_RPL_LISTEND:
                addMembersToChannel(memberslist.getItems(), "@Console");
                break;
            case OJConstants.ID_RPL_WELCOME:
                break;
            case OJConstants.ID_RPL_MOTD:
                addTextToChannel(m.getJoinedParameter(1), CONSOLE_CHANNEL_NAME);
                break;
            case OJConstants.ID_RPL_ENDOFMOTD:
                listener.sendCommand(new IRCMessage(OJConstants.ID_CLIENT_LIST));
                break;
            case OJConstants.ID_RPL_TOPIC:
                setTopicOnChannel(m.getParameter(1), m.getParameter(2));
                break;
        }
    }

    /**
   * English: processes lines typed in the input box. Sends the data away.<BR>
   * Eesti keeles: Tegeleb sisendridade t��tlemisega. Saadab info teele ka.
   * @param tt the string received from textin / sissetipitud tekst.
   */
    public void processLine(String tt) {
        textin.setText("");
        if (tt == null || tt.equals("") || tt.trim().equals("")) return;
        if (tt.charAt(0) != '/') {
            if (currentChannel == null) {
                textout.append("[BasicGUI] Commands must start with / !\n");
                return;
            } else {
                System.out.println("Sending channel message");
                tt = "/PRIVMSG " + currentChannel.toString() + " " + tt;
            }
        }
        if (listener != null) {
            IRCMessage p = listener.constructMessage(tt);
            if (p.getCommand().equals(OJMessageProcessor.FAKEMESSAGECOMMAND)) {
                textout.append("[OJIRC] Invalid command");
                return;
            }
            if (p.getCommand().equals(OJConstants.ID_CLIENT_PRIVMSG)) {
                ChatChannel x = findChannel(p.getParameter(0));
                if (x != null) {
                    addTextToChannel(constructPrivateMessage(listener.getParameter("nickname"), p.getJoinedParameter(1).substring(1)), x.channelName);
                } else {
                    addTextToChat(constructPrivateMessage(listener.getParameter("nickname") + "->" + p.getParameter(0), p.getJoinedParameter(1).substring(1)));
                }
            }
            String com = p.getCommand();
            if (com.equals("CONNECT")) {
                listener.connect();
                listener.login();
            } else if (com.equals("DISCONNECT")) {
                listener.sendCommand(new IRCMessage("QUIT"));
                textout.append("Trying to disconnect...");
                try {
                    Thread.currentThread().sleep(1000);
                    listener.disconnect();
                    System.exit(0);
                } catch (Exception e) {
                    System.exit(1);
                } finally {
                    textout.append("Disconnected.");
                }
            } else {
                listener.sendCommand(p);
            }
        }
    }

    /**
   * English: A method to add all the listeners needed in the interface. Called from init.<BR>
   * Eesti keeles: Meetod, mis loob k�ik vajalikud kuularid ja seob need objektidega.
   */
    public void addListeners() {
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        textin.addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    processLine(textin.getText());
                }
            }
        });
        connect_btn.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (connect_btn.getLabel().equals("connect")) {
                    listener.connect();
                    listener.login();
                    if (listener.getConnected()) {
                        connect_btn.setLabel("disconnect");
                        disconnected = false;
                    }
                } else {
                    textout.append("\nDisconnecting..");
                    listener.disconnect();
                    if (!listener.getConnected()) {
                        connect_btn.setLabel("connect");
                        disconnected = true;
                        memberslist.removeAll();
                    }
                }
            }
        });
        leaveChannelBtn.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (!currentChannel.isConsole) {
                    listener.sendCommand(new IRCMessage(OJConstants.ID_CLIENT_PART, new String[] { currentChannel.channelName }));
                    leftChannel(currentChannel.channelName);
                }
            }
        });
        memberslist.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == e.SELECTED) {
                    if (currentChannel == null || currentChannel.isConsole) listener.sendCommand(new IRCMessage(OJConstants.ID_CLIENT_JOIN, new String[] { memberslist.getSelectedItem() }));
                }
            }
        });
        channelChoice.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == e.SELECTED) {
                    System.out.println(e.getItem());
                    changeChannel(e.getItem().toString());
                }
            }
        });
        viewChoice.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == e.SELECTED) {
                    System.out.println("v " + e.getItem());
                }
            }
        });
        configButton.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                showConfigPanel();
            }
        });
    }

    /**
   * English: Changes the active channel. Updates screen to show it.<BR>
   * Eesti keeles: Vahetab aktiivset kanalit. Uuendab ekraaani.
   * @param channame the name of the channel to be switched to / kanali nimi, mille peale vahetada
   */
    public void changeChannel(String channame) {
        if (currentChannel == null && findChannel(channame) != null) {
            currentChannel = findChannel(channame);
            channelChoice.select(channame);
            syncActiveChannelMembers();
            return;
        }
        if (currentChannel.channelName.equals(channame)) return;
        ChatChannel c = null;
        if ((c = findChannel(channame)) != null) {
            String hist = textout.getText();
            currentChannel.chatHistory = new String(hist);
            String newhist = new String(c.chatHistory);
            textout.setText(new String(""));
            textout.append(newhist);
            currentChannel = c;
            channelChoice.select(c.channelName);
            titleLabel.setText(c.topic);
            repaint();
            syncActiveChannelMembers();
            return;
        }
    }

    /**
   * English: Determines if the specified channel has been created<BR>
   * Eesti keeles: Kontrollib, kas konkreetne kanal on juba loodud.
   * @param channame the name of the channel searched / otsitava kanali nimi
   */
    public ChatChannel findChannel(String channame) {
        for (int i = 0; i < channels.size(); i++) if (channels.elementAt(i).toString().equals(channame)) return (ChatChannel) channels.elementAt(i);
        return null;
    }

    /**
   * English: Constructs a new chat channel<BR>
   * Eesti keeles: Konstrueerib uue jutukanali.
   * @param channelname the name of the channel to be constructed / loodava kanali nimi
   * @param isprivate is the channel a private channel / kas kanal on erakanal (kahe inimese vaheline)
   * @param autochange if set, the channel will become the active channel / kas aktiveerida kanal automaatselt?
   */
    public ChatChannel createChannel(String channelname, boolean isprivate, boolean autochange) {
        ChatChannel newchannel = new ChatChannel();
        newchannel.channelName = channelname;
        newchannel.isprivate = isprivate;
        channels.addElement(newchannel);
        syncChannelChoice();
        if (autochange) {
            changeChannel(channelname);
        }
        return newchannel;
    }

    /**
   * English: Used to set the topic of a channel<BR>
   * Eesti keeles: Muudab konkreetse kanali teemat.
   * @param channelname the name of the channel / kanali nimi
   * @param topic the topic to be used / uus teema
   */
    public void setChannelTopic(String channelname, String topic) {
        ChatChannel c = findChannel(channelname);
        if (c != null) {
            c.topic = topic;
            if (c == currentChannel) {
                titleLabel.setText(topic);
            }
        }
    }

    /**
   * English: Creates the pseudo-channel to hold all channels<BR>
   * Eesti keeles: Loob konsoolikanali, mille liikmed k�ik �lej��nud kanalid on.
   */
    public void createConsoleChannel() {
        if (findChannel(CONSOLE_CHANNEL_NAME) != null) return;
        ChatChannel z = createChannel(CONSOLE_CHANNEL_NAME, false, true);
        z.topic = "<User Console - Use /LIST to see and /JOIN to join channels>";
        z.isConsole = true;
    }

    /**
   * English: Deletes a channel from the list of channels<BR>
   * Eesti keeles: Kustutab kanali nimekirjast.
   * @param channelname the name of the channel to be deleted / kustutatava kanali nimi
   */
    public void destroyChannel(String channelName) {
        ChatChannel c = findChannel(channelName);
        if (c == null || c.isConsole) return;
        channels.removeElement(c);
        channelChoice.remove(c.channelName);
        if (c == currentChannel) {
            changeChannel(CONSOLE_CHANNEL_NAME);
        }
    }

    /**
   * English: Adds members to an existing channel<BR>
   * Eesti keeles: Lisab kanalile liikmeid.
   * @param members[] a list of names to be added / lisatavate nimekiri
   * @param channame the channel to get the members / mis kanalile lisatakse?
   */
    public void addMembersToChannel(String[] members, String channame) {
        ChatChannel c = findChannel(channame);
        if (c == null) return;
        String reallyAddable[] = new String[members.length];
        String existing[] = c.members;
        if (existing.length >= 0) {
            int reallyAdded = 0;
            for (int i = 0; i < members.length; i++) {
                boolean matched = false;
                for (int j = 0; j < existing.length; j++) if (existing[j].equals(members[i])) {
                    matched = true;
                    break;
                }
                if (!matched) {
                    reallyAddable[reallyAdded] = members[i];
                    reallyAdded++;
                }
            }
            String temp[] = new String[existing.length + reallyAdded];
            System.arraycopy(existing, 0, temp, 0, existing.length);
            System.arraycopy(reallyAddable, 0, temp, existing.length, reallyAdded);
            c.members = temp;
        }
        sortChannelMembers(channame);
        if (c == currentChannel) {
            syncActiveChannelMembers();
        }
    }

    public void removeMemberFromChannel(String member, String channel) {
        ChatChannel c = findChannel(channel);
        if (c == null) return;
        int i = locateInArray(c.members, member);
        if (i < 0) return;
        String[] temp = new String[c.members.length - 1];
        System.arraycopy(c.members, 0, temp, 0, i - 1);
        System.arraycopy(c.members, i + 1, temp, i, c.members.length - i);
    }

    public void showArray(String c[]) {
        for (int i = 0; i < c.length; i++) {
            System.out.print(" " + c[i]);
        }
        System.out.println();
    }

    public int locateInArray(String arr[], String c) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(c)) return i;
        }
        return -1;
    }

    public void sortChannelMembers(String channelName) {
        ChatChannel c = findChannel(channelName);
        String s[] = c.members;
        sortArray(s);
    }

    public void sortArray(String s[]) {
        for (int i = 0; i < s.length - 1; i++) for (int j = i + 1; j < s.length; j++) if (s[i].compareTo(s[j]) > 0) {
            String abi = s[i];
            s[i] = s[j];
            s[j] = abi;
        }
    }

    public void syncChannelChoice() {
        String chans[] = new String[channels.size()];
        for (int i = 0; i < channels.size(); i++) {
            chans[i] = channels.elementAt(i).toString();
        }
        sortArray(chans);
        channelChoice.removeAll();
        for (int i = 0; i < chans.length; i++) channelChoice.add(chans[i]);
        if (currentChannel != null) channelChoice.select(currentChannel.channelName);
    }

    public void syncActiveChannelMembers() {
        if (currentChannel == null) return;
        memberslist.removeAll();
        ChatChannel c = currentChannel;
        for (int i = 0; i < c.members.length; i++) {
            memberslist.add(c.members[i]);
        }
        repaint();
    }

    public void joinedChannel(String chan) {
        createChannel(chan, false, true);
        syncChannelChoice();
    }

    public void leftChannel(String ch) {
        destroyChannel(ch);
        syncChannelChoice();
    }

    public void memberJoinedChannel(String memberName[], String channelName) {
        addMembersToChannel(memberName, channelName);
        syncActiveChannelMembers();
    }

    public void memberLeftChannel(String memberName, String channelName) {
        removeMemberFromChannel(memberName, channelName);
        syncActiveChannelMembers();
    }

    public void addTextToChat(String line) {
        String ups = new String(textout.getText());
        int ln = 0;
        for (int i = 0; i < ups.length(); i++) {
            if (ups.charAt(i) == '\n') ln++;
        }
        int xln = 0;
        if (ln > CHAT_HISTORY) {
            for (int j = (ups.length() - 1); j > -1; j--) {
                if (ups.charAt(j) == '\n') {
                    xln++;
                    if (xln == CHAT_HISTORY) {
                        ups = ups.substring(j + 1);
                        break;
                    }
                }
            }
        }
        ups = ups + line;
        textout.setText(ups);
        textout.setCaretPosition(32000);
    }

    public void addTextToChannel(String text, String channame) {
        ChatChannel c = findChannel(channame);
        if (c == null) return;
        if (c == currentChannel) {
            addTextToChat(text + '\n');
        } else {
            c.chatHistory += text + '\n';
        }
    }

    public void update(Graphics g) {
        if (g == null) return;
        try {
            if (dbler == null) {
                dbler = createImage(getSize().width, getSize().height);
            }
            Graphics og = dbler.getGraphics();
            paint(og);
            g.drawImage(dbler, 0, 0, null);
            og.dispose();
        } catch (Exception e) {
            e.printStackTrace();
            paint(g);
        }
    }

    /**
   * For Compability with jdk1.1.
   *
   * by MoRo
   */
    public int getX() {
        return getLocation().x;
    }

    /**
   * For Compability with jdk1.1.
   *
   * by MoRo
   */
    public int getY() {
        return getLocation().y;
    }

    Frame configPanel = null;

    TextField nickField = null;

    TextField serverField = null;

    TextField portField = null;

    public Frame createConfigPanel() {
        Frame x = new Frame("OJIRC Configuration");
        x.setSize(300, 250);
        x.setLocation(this.getX() + 25, this.getY() + 25);
        x.setResizable(false);
        Panel backPanel = new Panel();
        backPanel.setBackground(mainbg);
        backPanel.setForeground(Color.black);
        backPanel.setLayout(null);
        x.setLayout(new BorderLayout());
        x.add(backPanel, BorderLayout.CENTER);
        Label nickLabel = new Label("Nickname");
        Label serverLabel = new Label("Server host");
        Label portLabel = new Label("Server port");
        Button applyButton = new Button("Apply");
        Button cancelButton = new Button("Cancel");
        nickField = new TextField();
        serverField = new TextField();
        portField = new TextField();
        nickLabel.setBounds(10, 10, 80, 27);
        nickLabel.setBackground(mainbg);
        nickLabel.setForeground(textfg);
        nickLabel.setFont(font_normal);
        nickField.setBounds(10, 40, 150, 27);
        nickField.setBackground(textbg);
        nickField.setForeground(textfg);
        nickField.setFont(font_normal);
        serverLabel.setBounds(10, 70, 80, 27);
        serverLabel.setBackground(mainbg);
        serverLabel.setForeground(textfg);
        serverLabel.setFont(font_normal);
        serverField.setBounds(10, 110, 150, 27);
        serverField.setBackground(textbg);
        serverField.setForeground(textfg);
        serverField.setFont(font_normal);
        portLabel.setBounds(10, 140, 60, 27);
        portLabel.setBackground(mainbg);
        portLabel.setForeground(textfg);
        portLabel.setFont(font_normal);
        portField.setBounds(10, 170, 80, 27);
        portField.setBackground(textbg);
        portField.setForeground(textfg);
        portField.setFont(font_normal);
        applyButton.setBounds(10, 200, 70, 20);
        cancelButton.setBounds(100, 200, 70, 20);
        applyButton.setFont(font_normal);
        cancelButton.setFont(font_normal);
        applyButton.setBackground(mainbg);
        cancelButton.setBackground(mainbg);
        applyButton.setForeground(Color.white);
        cancelButton.setForeground(Color.black);
        backPanel.add(nickLabel);
        backPanel.add(nickField);
        backPanel.add(serverLabel);
        backPanel.add(serverField);
        backPanel.add(portLabel);
        backPanel.add(portField);
        backPanel.add(cancelButton);
        backPanel.add(applyButton);
        x.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                hideConfigPanel();
            }
        });
        cancelButton.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                hideConfigPanel();
            }
        });
        applyButton.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                hideConfigPanel();
                savePreferences();
            }
        });
        return x;
    }

    public void showConfigPanel() {
        if (configPanel == null) configPanel = createConfigPanel();
        getPreferences();
        configPanel.show();
        configPanel.repaint();
        getPreferences();
    }

    public void hideConfigPanel() {
        configPanel.hide();
    }

    public void savePreferences() {
        listener.setParameter("nickname", nickField.getText());
        listener.setParameter("server", serverField.getText());
        listener.setParameter("serverport", portField.getText());
        listener.saveParams();
        if (!disconnected) listener.sendCommand(new IRCMessage(OJConstants.ID_CLIENT_NICK, new String[] { nickField.getText() }));
        System.out.println("Saved");
    }

    public void getPreferences() {
        nickField.setText(listener.getParameter("nickname"));
        serverField.setText(listener.getParameter("server"));
        portField.setText(listener.getParameter("serverport"));
    }
}
