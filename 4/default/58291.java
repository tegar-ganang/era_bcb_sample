import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URL;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Observable;
import java.util.StringTokenizer;
import ar.com.jkohen.awt.*;
import ar.com.jkohen.irc.*;
import ar.com.jkohen.util.ConfigurationProperties;
import ar.com.jkohen.util.Resources;

class ChannelWindow extends ChatWindow implements ActionListener, MouseListener, KeyListener, ItemListener {

    protected TextField topic;

    protected TextFieldHistory entry;

    protected NickList nick_list;

    protected ImageButton close;

    protected ImageButton kill;

    protected PopupMenu popup_menu;

    protected Menu child_menu;

    protected Menu ircop_menu;

    private Choice color;

    private boolean have_halfops;

    private EIRC eirc;

    private Channel channel;

    private Resources res;

    private String item_under_menu;

    private boolean is_op;

    private boolean is_hop;

    private boolean is_adm;

    private boolean is_own;

    private String complete_partial;

    private String complete_current = "";

    public ChannelWindow(EIRC eirc, Channel channel) {
        super(channel.getTag());
        this.eirc = eirc;
        this.channel = channel;
        text_canvas.setMode(eirc.scrollSpeed());
        GridBagLayout gb = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        setLayout(gb);
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridy = 0;
        topic = new TextFieldHistory(eirc.getFrame());
        gbc.weightx = 1.0;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(topic, gbc);
        add(topic);
        gbc.gridy++;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gb.setConstraints(text_canvas, gbc);
        add(text_canvas);
        gbc.weightx = 0.0;
        nick_list = new NickList();
        gbc.weightx = 0;
        gbc.gridwidth = 2;
        gb.setConstraints(nick_list, gbc);
        add(nick_list);
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.gridx = 0;
        gbc.gridy++;
        color = new Choice();
        StringTokenizer tk = new StringTokenizer(res.getString("conf.write_col_list"), ",");
        while (tk.hasMoreTokens()) color.add(tk.nextToken());
        gbc.anchor = GridBagConstraints.WEST;
        gb.setConstraints(color, gbc);
        add(color);
        gbc.weightx = 0;
        gbc.gridx++;
        kill = new ImageButton(Resources.getLabel("chan.text.kill"), Resources.getImage("chan.icon.kill"));
        kill.setEnabled(true);
        gb.setConstraints(kill, gbc);
        add(kill);
        gbc.gridx++;
        close = new ImageButton(Resources.getLabel("chan.text.close"), Resources.getImage("chan.icon.close"));
        gbc.anchor = GridBagConstraints.EAST;
        close.setEnabled(true);
        gb.setConstraints(close, gbc);
        add(close);
        gbc.gridx = 0;
        gbc.gridy++;
        this.entry = new TextFieldHistory(eirc.getFrame());
        entry.setFocusTraversable(false);
        setFocusTraversalKeysEnabled(entry, false);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gb.setConstraints(entry, gbc);
        add(entry);
        popup_menu = new PopupMenu();
        child_menu = new Menu();
        ircop_menu = new Menu();
        add(popup_menu);
        channel.addObserver(this);
        topic.addActionListener(this);
        popup_menu.addActionListener(this);
        nick_list.addActionListener(this);
        nick_list.addMouseListener(this);
        entry.addActionListener(this);
        entry.addKeyListener(this);
        color.addItemListener(this);
        close.addActionListener(this);
        kill.addActionListener(this);
    }

    public void requestFocus() {
        entry.requestFocus();
    }

    public Channel getChannel() {
        return channel;
    }

    protected String getNick() {
        return eirc.getNick();
    }

    public void refreshUsers() {
        nick_list.loadList(channel.elements());
    }

    public void clearUsers() {
        channel.clean();
    }

    protected void visitURL(URL url) {
        eirc.visitURL(url);
    }

    protected void joinChannel(String name) {
        eirc.joinChannel(name);
    }

    protected void bell() {
        eirc.playSound(res.EVENT_MSG);
    }

    protected void talkTo(String nick) {
        String text = entry.getText();
        nick += talkto;
        int pos = entry.getCaretPosition();
        text = text.substring(0, pos).concat(nick).concat(text.substring(pos));
        entry.setText(text);
        entry.setCaretPosition(pos + nick.length());
        entry.requestFocus();
    }

    protected void copyText(String s) {
        eirc.cutPaste(s);
    }

    private void resetCompleteNick() {
        this.complete_current = "";
    }

    private void completeNick() {
        char[] text = entry.getText().toCharArray();
        int pos = entry.getCaretPosition();
        int begin = pos - 1;
        int end = pos;
        Collator collator = RFC1459.getCollator();
        String[] list = nick_list.getNicks();
        for (int i = 0; i < list.length; i++) {
            for (int j = list.length - 1; j > i; j--) {
                if (collator.compare(list[i], list[j]) > 0) {
                    String t = list[i];
                    list[i] = list[j];
                    list[j] = t;
                }
            }
        }
        while (begin >= 0 && RFC1459.isDeclaredChar(text[begin])) begin--;
        while (end < text.length && RFC1459.isDeclaredChar(text[end])) end++;
        begin++;
        if (complete_current.length() == 0) {
            complete_partial = String.valueOf(text, begin, end - begin);
        }
        int partial_length = complete_partial.length();
        int first_match = -1;
        boolean found = false;
        for (int i = 0; i < list.length; i++) {
            if (list[i].length() < partial_length) {
                continue;
            }
            String partial_nick = list[i].substring(0, partial_length);
            if (collator.equals(partial_nick, complete_partial)) {
                if (first_match == -1) {
                    first_match = i;
                }
                int order = collator.compare(list[i], complete_current);
                if (order > 0) {
                    complete_current = list[i];
                    found = true;
                    break;
                }
            }
        }
        if (first_match != -1 && !found) {
            complete_current = list[first_match];
            found = true;
        }
        if (found) {
            String new_text = String.valueOf(text, 0, begin).concat(complete_current).concat(String.valueOf(text, end, text.length - end));
            entry.setText(new_text);
            entry.setCaretPosition(begin + complete_current.length());
        }
    }

    private void update_channel(Object hints) {
        String action = hints.toString();
        boolean load_list = true;
        if (action == null || action.equals("topic")) {
            topic.setText(channel.getTopic());
            load_list = false;
        }
        if (action == null || action.equals("mode")) {
            User user = channel.get(eirc.getNick());
            if (user != null) {
                this.is_op = user.isOp();
                this.is_hop = user.isHalfOp();
                this.is_adm = user.isAdmin();
                this.is_own = user.isOwner();
            }
            topic.setEditable(is_own || is_adm || is_op || is_hop || channel.isTopicSettable() || eirc.canOverride());
            buildPopupMenu(true);
        }
        if (action == null || load_list) nick_list.loadList(channel.elements());
    }

    private void update_properties(ConfigurationProperties props, Object arg) {
        super.update(props, arg);
        if (arg == null || arg.equals("nick_item_renderer")) {
            nick_list.setItemRenderer(props.getInt("nick_item_renderer"));
            nick_list.loadList(channel.elements());
        }
        if (arg == null || arg.equals("nick_item_sort_method")) nick_list.setSortMethod(props.getInt("nick_item_sort_method"));
        if (arg == null || arg.equals("have_halfops")) this.have_halfops = props.getBoolean("have_halfops");
        if (arg == null || arg.equals("write_color")) color.select(props.getInt("write_color"));
    }

    public void update(Observable o, Object arg) {
        if (o instanceof Channel) update_channel(arg); else update_properties((ConfigurationProperties) o, arg);
    }

    public void setEnabled(boolean b) {
        text_canvas.setEnabled(b);
        topic.setEnabled(b);
        entry.setEnabled(b);
        close.setEnabled(b);
        kill.setEnabled(b);
        buildPopupMenu(false);
    }

    public void setFont(Font f) {
        super.setFont(f);
        topic.setFont(f);
        entry.setFont(f);
        nick_list.setFont(f);
        nick_list.repaint();
        buildPopupMenu(true);
        close.setFont(f);
        kill.setFont(f);
    }

    public void setBackground(Color c) {
        super.setBackground(c);
        nick_list.setBackground(c);
        close.repaint();
        kill.repaint();
    }

    public void setForeground(Color c) {
        super.setForeground(c);
        nick_list.setForeground(c);
    }

    public void setTextBackground(Color c) {
        text_canvas.setBackground(c);
        entry.setBackground(c);
        topic.setBackground(c);
        nick_list.setTextBackground(c);
    }

    public void setTextForeground(Color c) {
        text_canvas.setForeground(c);
        entry.setForeground(c);
        topic.setForeground(c);
        nick_list.setTextForeground(c);
    }

    public void setSelectedBackground(Color c) {
        text_canvas.setSelectedBackground(c);
        nick_list.setSelectedBackground(c);
    }

    public void setSelectedForeground(Color c) {
        nick_list.setSelectedForeground(c);
    }

    private synchronized void buildPopupMenu(boolean b) {
        String tag = channel.getTag();
        PopupMenu pm = popup_menu;
        Menu cm = child_menu;
        Menu im = ircop_menu;
        MenuItem mi;
        pm.removeAll();
        cm.removeAll();
        im.removeAll();
        mi = new MenuItem(res.getString("nicklist.popup.query"));
        mi.setActionCommand("QUERY {0}");
        pm.add(mi);
        pm.addSeparator();
        String n = res.getString("nicklist.popup.customs");
        try {
            int max = Integer.parseInt(n);
            for (int i = 1; i <= max; i++) {
                String name = res.getString("nicklist.popup.custom." + i);
                String cmd = res.getString("nicklist.popup.command." + i);
                if (name != null && cmd != null) {
                    mi = new MenuItem(name);
                    mi.setActionCommand(cmd);
                    pm.add(mi);
                }
            }
            pm.addSeparator();
        } catch (NumberFormatException ex) {
            System.err.println("Wrong custom menu number");
        }
        mi = new MenuItem(res.getString("nicklist.popup.ignore"));
        mi.setActionCommand("IGNORE {0}");
        pm.add(mi);
        mi = new MenuItem(res.getString("nicklist.popup.unignore"));
        mi.setActionCommand("UNIGNORE {0}");
        pm.add(mi);
        pm.addSeparator();
        mi = new MenuItem(res.getString("nicklist.popup.ghost"));
        mi.setActionCommand("GHOST");
        pm.add(mi);
        if (is_op || is_adm || is_own || eirc.canOverride()) {
            pm.addSeparator();
            cm = new Menu(res.getString("nicklist.popup.operators"));
            cm.addActionListener(this);
            mi = new MenuItem(res.getString("nicklist.popup.kick"));
            mi.setActionCommand("KICK");
            cm.add(mi);
            mi = new MenuItem(res.getString("nicklist.popup.kban"));
            mi.setActionCommand("KBAN");
            cm.add(mi);
            cm.addSeparator();
            mi = new MenuItem(res.getString("nicklist.popup.ban"));
            mi.setActionCommand("MODE {1} +b {0}");
            cm.add(mi);
            mi = new MenuItem(res.getString("nicklist.popup.unban"));
            mi.setActionCommand("MODE {1} -b {0}");
            cm.add(mi);
            cm.addSeparator();
            mi = new MenuItem(res.getString("nicklist.popup.admin"));
            mi.setActionCommand("MODE {1} +a {0}");
            cm.add(mi);
            mi = new MenuItem(res.getString("nicklist.popup.except"));
            mi.setActionCommand("MODE {1} +e {0}");
            cm.add(mi);
            mi = new MenuItem(res.getString("nicklist.popup.invite"));
            mi.setActionCommand("MODE {1} +I {0}");
            cm.add(mi);
            cm.addSeparator();
            mi = new MenuItem(res.getString("nicklist.popup.voice"));
            mi.setActionCommand("MODE {1} +v {0}");
            cm.add(mi);
            mi = new MenuItem(res.getString("nicklist.popup.unvoice"));
            mi.setActionCommand("MODE {1} -v {0}");
            cm.add(mi);
            if (have_halfops) {
                cm.addSeparator();
                mi = new MenuItem(res.getString("nicklist.popup.hop"));
                mi.setActionCommand("MODE {1} +h {0}");
                cm.add(mi);
                mi = new MenuItem(res.getString("nicklist.popup.dehop"));
                mi.setActionCommand("MODE {1} -h {0}");
                cm.add(mi);
            }
            cm.addSeparator();
            mi = new MenuItem(res.getString("nicklist.popup.op"));
            mi.setActionCommand("MODE {1} +o {0}");
            cm.add(mi);
            mi = new MenuItem(res.getString("nicklist.popup.deop"));
            mi.setActionCommand("MODE {1} -o {0}");
            cm.add(mi);
            pm.add(cm);
        } else {
            if (is_hop) {
                pm.addSeparator();
                cm = new Menu(res.getString("nicklist.popup.hoperators"));
                cm.addActionListener(this);
                mi = new MenuItem(res.getString("nicklist.popup.kick"));
                mi.setActionCommand("KICK");
                cm.add(mi);
                mi = new MenuItem(res.getString("nicklist.popup.kban"));
                mi.setActionCommand("KBAN");
                cm.add(mi);
                cm.addSeparator();
                mi = new MenuItem(res.getString("nicklist.popup.ban"));
                mi.setActionCommand("MODE {1} +b {0}");
                cm.add(mi);
                mi = new MenuItem(res.getString("nicklist.popup.unban"));
                mi.setActionCommand("MODE {1} -b {0}");
                cm.add(mi);
                cm.addSeparator();
                mi = new MenuItem(res.getString("nicklist.popup.except"));
                mi.setActionCommand("MODE {1} +e {0}");
                cm.add(mi);
                cm.addSeparator();
                mi = new MenuItem(res.getString("nicklist.popup.voice"));
                mi.setActionCommand("MODE {1} +v {0}");
                cm.add(mi);
                mi = new MenuItem(res.getString("nicklist.popup.unvoice"));
                mi.setActionCommand("MODE {1} -v {0}");
                cm.add(mi);
                pm.add(cm);
            }
        }
        if (eirc.isIRCop()) {
            pm.addSeparator();
            im = new Menu(res.getString("nicklist.popup.ircops"));
            im.addActionListener(this);
            pm.add(im);
            mi = new MenuItem(res.getString("nicklist.popup.kill"));
            mi.setActionCommand("KILL");
            im.add(mi);
        }
        for (int i = 0; i < pm.getItemCount(); i++) {
            pm.getItem(i).setFont(getFont());
            pm.getItem(i).setEnabled(b);
        }
        for (int i = 0; i < cm.getItemCount(); i++) {
            cm.getItem(i).setFont(getFont());
            cm.getItem(i).setEnabled(b);
        }
        for (int i = 0; i < im.getItemCount(); i++) {
            im.getItem(i).setFont(getFont());
            im.getItem(i).setEnabled(b);
        }
    }

    public void actionPerformed(ActionEvent ev) {
        Object comp = ev.getSource();
        if (comp instanceof MenuItem) {
            String command = ev.getActionCommand();
            if (command.indexOf("+b") >= 0 || command.indexOf("-b") >= 0) {
                String mask = item_under_menu;
                String addr = NickInfo.getInetAddr(item_under_menu);
                String user = NickInfo.getUser(item_under_menu);
                if (addr == null) {
                    if (user != null) mask = "*!" + user + "@*";
                } else {
                    mask = "*!*@" + addr;
                }
                Object o[] = { mask, channel.getTag() };
                MessageFormat mf = new MessageFormat(command);
                eirc.sendCommand(mf.format(o), this);
            } else {
                Object o[] = { item_under_menu, channel.getTag() };
                MessageFormat mf = new MessageFormat(command);
                if (command.indexOf("KICK") >= 0 || command.indexOf("KBAN") >= 0) eirc.openKick(command, channel.getTag(), item_under_menu, this); else if (command.indexOf("KILL") >= 0) eirc.openKill(command, item_under_menu, this); else if (command.indexOf("GHOST") >= 0) eirc.openGhostWin(command, item_under_menu, this); else eirc.sendCommand(mf.format(o), this);
            }
        } else if (comp.equals(topic)) {
            String p[] = { channel.getTag(), topic.getText() };
            eirc.sendMessage("TOPIC", p);
        } else if (comp.equals(nick_list)) {
            String item = ev.getActionCommand();
            eirc.openPrivate(item);
            eirc.showPanel(item);
        } else if (comp.equals(entry)) {
            String text = entry.getText();
            if (text.length() <= 0) return;
            if (text.charAt(0) == '/') {
                text = text.substring(1);
                if (text.trim().length() > 0) eirc.sendCommand(text, this);
            } else {
                if (text.length() > 450) text = text.substring(0, 449);
                if (color.getSelectedIndex() != 1) text = MircMessage.COLOR + String.valueOf(color.getSelectedIndex()) + " " + text;
                String[] p = { channel.getTag(), text };
                eirc.sendMessage("PRIVMSG", p);
                printMyPrivmsg(text, getChannel().get(getNick()));
            }
            entry.setText("");
        } else if (comp.equals(kill)) {
            Object[] selection = nick_list.getSelectedObjects();
            boolean selected = false;
            String s = "";
            for (int i = 0; i < selection.length; i++) {
                if (selected) s = s.concat(",");
                selected = true;
                s = s.concat(selection[i].toString());
            }
            if (selected) {
                if (comp.equals(kill)) eirc.sendCommand("IGNORE " + s, this);
            } else {
                printError(res.getString("eirc.select_nicks"));
            }
        } else if (comp.equals(close)) {
            String p[] = { channel.getTag() };
            eirc.sendMessage("PART", p);
            eirc.closeChannel(channel.getTag());
        }
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == color) entry.requestFocus();
    }

    public void mouseClicked(MouseEvent ev) {
    }

    public void mouseReleased(MouseEvent ev) {
    }

    public void mousePressed(MouseEvent ev) {
        if (ev.isPopupTrigger() || (ev.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
            String item = nick_list.getNickAt(ev.getPoint());
            if (item != null) {
                item_under_menu = item;
                Point offset = nick_list.getScrollPosition();
                popup_menu.show(nick_list, ev.getX() - offset.x, ev.getY() - offset.y);
            }
        }
    }

    public void mouseEntered(MouseEvent ev) {
    }

    public void mouseExited(MouseEvent ev) {
    }

    public void keyPressed(KeyEvent ev) {
        if (ev.getSource() == entry) {
            switch(ev.getKeyCode()) {
                case (KeyEvent.VK_TAB):
                    ev.consume();
                    completeNick();
                    break;
                default:
                    resetCompleteNick();
                    break;
            }
        }
    }

    public void keyReleased(KeyEvent ev) {
    }

    public void keyTyped(KeyEvent ev) {
    }

    public void setFocusTraversalKeysEnabled(TextField tf, boolean b) {
        try {
            Class FieldClass = tf.getClass();
            Class args[] = { boolean.class };
            Method setfocus = FieldClass.getMethod("setFocusTraversalKeysEnabled", args);
            Object params[] = { new Boolean(b) };
            Object obj = setfocus.invoke(tf, params);
        } catch (Exception ex) {
        }
    }
}
