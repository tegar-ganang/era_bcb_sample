import java.applet.AudioClip;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.lang.reflect.*;
import ar.com.jkohen.awt.BorderedPanel;
import ar.com.jkohen.awt.ImageButton;
import ar.com.jkohen.awt.NickList;
import ar.com.jkohen.irc.Channel;
import ar.com.jkohen.irc.User;
import ar.com.jkohen.util.Resources;
import ar.com.jkohen.util.ConfigurationProperties;
import com.splendid.awtchat.SmileyTextArea;

public class Configurator extends NewDialog implements ActionListener, ItemListener, AdjustmentListener {

    private Resources res;

    private ConfigurationProperties properties;

    private static final int panel_num = 6;

    private BorderedPanel bp[] = new BorderedPanel[panel_num];

    private ImageButton ib[] = new ImageButton[panel_num];

    private ImageButton ban_list, except_list, invit_list;

    private Panel page;

    private CardLayout page_layout;

    private TextField size, bye, password, key, limit, operlogin, operpass;

    private Checkbox moder, invit, secret, nickchange, graphic_bg, text_bg;

    private Choice fonts;

    private java.awt.List chans, schemes;

    private Scrollbar red, green, blue;

    private Label r, g, b;

    private String s[] = new String[panel_num];

    private Vector items;

    private Vector boxes;

    public Configurator(EIRC eirc, ConfigurationProperties properties) {
        super(eirc);
        this.properties = properties;
        this.items = new Vector();
        this.boxes = new Vector();
        setFont(eirc.getFont());
        s[0] = Resources.getLabel("conf.text.dial");
        s[1] = Resources.getLabel("conf.text.display");
        s[2] = Resources.getLabel("conf.text.col");
        s[3] = Resources.getLabel("conf.text.snds");
        s[4] = Resources.getLabel("conf.text.adm");
        s[5] = Resources.getLabel("conf.text.adv");
        GridBagLayout gb;
        GridBagConstraints gbc;
        Checkbox cb;
        CheckboxGroup cbg;
        TextField tf;
        Label l;
        gb = new GridBagLayout();
        gbc = new GridBagConstraints();
        setLayout(gb);
        gbc.insets = new Insets(2, 2, 2, 2);
        int current = 0;
        ib[current] = new ImageButton(s[0]);
        ib[current].setIcon(res.getImage("conf.icon.dial"));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.SOUTH;
        gb.setConstraints(ib[current], gbc);
        add(ib[current]);
        current++;
        ib[current] = new ImageButton(s[1]);
        ib[current].setIcon(res.getImage("conf.icon.display"));
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gb.setConstraints(ib[current], gbc);
        add(ib[current]);
        current++;
        ib[current] = new ImageButton(s[2]);
        ib[current].setIcon(res.getImage("conf.icon.col"));
        gbc.gridy++;
        gb.setConstraints(ib[current], gbc);
        add(ib[current]);
        current++;
        ib[current] = new ImageButton(s[3]);
        ib[current].setIcon(res.getImage("conf.icon.snds"));
        gbc.gridy++;
        gb.setConstraints(ib[current], gbc);
        if (properties.getBoolean("load_sounds")) add(ib[current]);
        current++;
        ib[current] = new ImageButton(s[4]);
        ib[current].setIcon(res.getImage("conf.icon.adm"));
        gbc.gridy++;
        gb.setConstraints(ib[current], gbc);
        add(ib[current]);
        current++;
        ib[current] = new ImageButton(s[5]);
        ib[current].setIcon(res.getImage("conf.icon.adv"));
        gbc.gridy++;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        gb.setConstraints(ib[current], gbc);
        add(ib[current]);
        for (int i = 0; i < panel_num; i++) bp[i] = new BorderedPanel(s[i]);
        page = new Panel();
        page_layout = new CardLayout();
        page.setLayout(page_layout);
        gbc.gridx++;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridheight = panel_num;
        gbc.fill = GridBagConstraints.VERTICAL;
        gb.setConstraints(page, gbc);
        add(page);
        ImageButton button = new ImageButton(res.getString("ok"));
        button.setActionCommand("ok");
        button.addActionListener(this);
        gbc.gridx = 0;
        gbc.gridy = panel_num;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gb.setConstraints(button, gbc);
        add(button);
        for (int i = 0; i < panel_num; i++) page.add(bp[i], s[i]);
        current = 0;
        gb = new GridBagLayout();
        gbc = new GridBagConstraints();
        bp[current].setLayout(gb);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(2, 2, 2, 2);
        gb.setConstraints(bp[current], gbc);
        cb = new Checkbox(res.getString("conf.private"), properties.getBoolean("no_privates"));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gb.setConstraints(cb, gbc);
        bp[current].add(cb);
        items.addElement(new ConfCheckbox("no_privates", cb));
        cb = new Checkbox(res.getString("conf.invite"), properties.getBoolean("see_invite"));
        gbc.gridy++;
        gb.setConstraints(cb, gbc);
        bp[current].add(cb);
        items.addElement(new ConfCheckbox("see_invite", cb));
        cb = new Checkbox(res.getString("conf.focus_privates"), properties.getBoolean("focus_opening_privates"));
        gbc.gridy++;
        gb.setConstraints(cb, gbc);
        bp[current].add(cb);
        items.addElement(new ConfCheckbox("focus_opening_privates", cb));
        l = new Label(res.getString("conf.quit") + " :");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        bye = new TextField(properties.getString("quit_message"), 20);
        gbc.gridx++;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(bye, gbc);
        bp[current].add(bye);
        items.addElement(new ConfTextField("quit_message", bye));
        current++;
        gb = new GridBagLayout();
        gbc = new GridBagConstraints();
        bp[current].setLayout(gb);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(2, 2, 2, 2);
        gb.setConstraints(bp[current], gbc);
        cb = new Checkbox(res.getString("conf.join_part"), properties.getBoolean("see_join"));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gb.setConstraints(cb, gbc);
        bp[current].add(cb);
        items.addElement(new ConfCheckbox("see_join", cb));
        cb = new Checkbox(res.getString("conf.no_color"), properties.getBoolean("filter_mirc_attribs"));
        gbc.gridy++;
        gb.setConstraints(cb, gbc);
        bp[current].add(cb);
        items.addElement(new ConfCheckbox("filter_mirc_attribs", cb));
        l = new Label(res.getString("conf.timestamp") + " :");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        Choice formats = new Choice();
        StringTokenizer tk = new StringTokenizer(res.getString("conf.formats"), ",");
        while (tk.hasMoreTokens()) formats.add(tk.nextToken());
        formats.select(properties.getInt("date_format"));
        gbc.gridx++;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gb.setConstraints(formats, gbc);
        bp[current].add(formats);
        items.addElement(new ConfChoice("date_format", formats, false));
        l = new Label(res.getString("conf.priv") + " :");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        cbg = new CheckboxGroup();
        int renderer = properties.getInt("nick_item_renderer");
        cb = new Checkbox(res.getString("conf.bullet"), cbg, NickList.BULLET_RENDERER == renderer);
        cb.setName(String.valueOf(NickList.BULLET_RENDERER));
        gbc.gridx++;
        gbc.anchor = GridBagConstraints.WEST;
        gb.setConstraints(cb, gbc);
        bp[current].add(cb);
        cb = new Checkbox(res.getString("conf.symbol"), cbg, NickList.SYMBOL_RENDERER == renderer);
        cb.setName(String.valueOf(NickList.SYMBOL_RENDERER));
        gbc.gridx++;
        gb.setConstraints(cb, gbc);
        bp[current].add(cb);
        items.addElement(new ConfCheckboxGroup("nick_item_renderer", cbg));
        l = new Label(res.getString("conf.scroll") + " :");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.EAST;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        cbg = new CheckboxGroup();
        int method = properties.getInt("scroll_speed");
        cb = new Checkbox(res.getString("conf.fast"), cbg, SmileyTextArea.FAST == method);
        cb.setName(String.valueOf(SmileyTextArea.FAST));
        gbc.gridx++;
        gbc.anchor = GridBagConstraints.WEST;
        gb.setConstraints(cb, gbc);
        bp[current].add(cb);
        cb = new Checkbox(res.getString("conf.smooth"), cbg, SmileyTextArea.SMOOTH == method);
        cb.setName(String.valueOf(SmileyTextArea.SMOOTH));
        gbc.gridx++;
        gb.setConstraints(cb, gbc);
        bp[current].add(cb);
        items.addElement(new ConfCheckboxGroup("scroll_speed", cbg));
        l = new Label(res.getString("conf.text_font") + " :");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        String fontNames[];
        try {
            Class GraphicsEnvironment = Class.forName("java.awt.GraphicsEnvironment");
            Method getLocalGraphicsEnvironment = GraphicsEnvironment.getMethod("getLocalGraphicsEnvironment", new Class[] {});
            Object obj = getLocalGraphicsEnvironment.invoke(null, new Object[] {});
            Method getAvailableFontFamilyNames = GraphicsEnvironment.getMethod("getAvailableFontFamilyNames", new Class[] {});
            obj = getAvailableFontFamilyNames.invoke(obj, new Object[] {});
            fontNames = new String[Array.getLength(obj)];
            for (int i = 0; i < Array.getLength(obj); i++) fontNames[i] = (String) Array.get(obj, i);
        } catch (Exception ex) {
            fontNames = Toolkit.getDefaultToolkit().getFontList();
        }
        fonts = new Choice();
        for (int i = 0; i < fontNames.length; i++) fonts.addItem(fontNames[i]);
        Font font = eirc.getFont();
        if (font != null) fonts.select(font.getName());
        gbc.gridx++;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(fonts, gbc);
        bp[current].add(fonts);
        l = new Label(res.getString("conf.text_size") + " :");
        gbc.gridx++;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        size = new TextField(2);
        if (font != null) size.setText(String.valueOf(font.getSize()));
        gbc.gridx++;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(size, gbc);
        bp[current].add(size);
        current++;
        gb = new GridBagLayout();
        gbc = new GridBagConstraints();
        bp[current].setLayout(gb);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.25;
        gbc.weighty = 1;
        gbc.insets = new Insets(2, 2, 2, 2);
        gb.setConstraints(bp[current], gbc);
        cbg = new CheckboxGroup();
        graphic_bg = new Checkbox(res.getString("conf.graphic_bg"), cbg, true);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gb.setConstraints(graphic_bg, gbc);
        bp[current].add(graphic_bg);
        text_bg = new Checkbox(res.getString("conf.text_bg"), cbg, false);
        gbc.gridx++;
        gb.setConstraints(text_bg, gbc);
        bp[current].add(text_bg);
        l = new Label(res.getString("conf.r") + " :");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.25;
        gbc.anchor = GridBagConstraints.EAST;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        l = new Label(res.getString("conf.g") + " :");
        gbc.gridy++;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        l = new Label(res.getString("conf.b") + " :");
        gbc.gridy++;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        l = new Label(res.getString("conf.write_col") + " :");
        gbc.gridy++;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        red = new Scrollbar(Scrollbar.HORIZONTAL, getBackground().getRed(), 24, 0, 279);
        gbc.gridx++;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 0.25;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gb.setConstraints(red, gbc);
        bp[current].add(red);
        green = new Scrollbar(Scrollbar.HORIZONTAL, getBackground().getGreen(), 24, 0, 279);
        gbc.gridy++;
        gb.setConstraints(green, gbc);
        bp[current].add(green);
        blue = new Scrollbar(Scrollbar.HORIZONTAL, getBackground().getBlue(), 24, 0, 279);
        gbc.gridy++;
        gb.setConstraints(blue, gbc);
        bp[current].add(blue);
        Choice ch = new Choice();
        tk = new StringTokenizer(res.getString("conf.write_col_list"), ",");
        while (tk.hasMoreTokens()) ch.add(tk.nextToken());
        ch.select(properties.getInt("write_color"));
        gbc.gridy++;
        gb.setConstraints(ch, gbc);
        bp[current].add(ch);
        items.addElement(new ConfChoice("write_color", ch, false));
        r = new Label(String.valueOf(getBackground().getRed()));
        gbc.gridx += 2;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.15;
        gbc.anchor = GridBagConstraints.EAST;
        gb.setConstraints(r, gbc);
        bp[current].add(r);
        g = new Label(String.valueOf(getBackground().getGreen()));
        gbc.gridy++;
        gb.setConstraints(g, gbc);
        bp[current].add(g);
        b = new Label(String.valueOf(getBackground().getBlue()));
        gbc.gridy++;
        gb.setConstraints(b, gbc);
        bp[current].add(b);
        schemes = new java.awt.List(9);
        gbc.gridx++;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 5;
        gbc.weightx = 0.40;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gb.setConstraints(schemes, gbc);
        bp[current].add(schemes);
        String col[] = eirc.getUserColors();
        for (int i = 0; i < col.length; i++) schemes.add(col[i]);
        current++;
        gb = new GridBagLayout();
        gbc = new GridBagConstraints();
        bp[current].setLayout(gb);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(2, 2, 2, 2);
        gb.setConstraints(bp[current], gbc);
        l = new Label(res.getString("conf.snd") + " :");
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        cbg = new CheckboxGroup();
        int mute = properties.getInt("silent");
        cb = new Checkbox(res.getString("conf.snd.on"), cbg, mute == res.SND_ON);
        cb.setName(String.valueOf(res.SND_ON));
        gbc.gridx++;
        gbc.anchor = GridBagConstraints.WEST;
        gb.setConstraints(cb, gbc);
        bp[current].add(cb);
        cb = new Checkbox(res.getString("conf.snd.offaway"), cbg, mute == res.SND_OFFAWAY);
        cb.setName(String.valueOf(res.SND_OFFAWAY));
        gbc.gridx++;
        gb.setConstraints(cb, gbc);
        bp[current].add(cb);
        cb = new Checkbox(res.getString("conf.snd.off"), cbg, mute == res.SND_OFF);
        cb.setName(String.valueOf(res.SND_OFF));
        gbc.gridx++;
        gbc.gridwidth = 1;
        gb.setConstraints(cb, gbc);
        bp[current].add(cb);
        items.addElement(new ConfCheckboxGroup("silent", cbg));
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        Choice c[] = new Choice[res.EVENTS];
        String silent = res.getString("conf.snd.no");
        for (int i = 0; i < 5; i++) {
            l = new Label(res.getString("conf.snd." + (i + 1)) + " :");
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.EAST;
            gb.setConstraints(l, gbc);
            bp[current].add(l);
            c[i] = new Choice();
            c[i].setName("event_" + i);
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            gb.setConstraints(c[i], gbc);
            bp[current].add(c[i]);
            c[i].addItem(silent);
            gbc.gridy++;
        }
        gbc.gridy = 1;
        for (int i = 5; i < c.length; i++) {
            l = new Label(res.getString("conf.snd." + (i + 1)) + " :");
            gbc.gridx = 2;
            gbc.anchor = GridBagConstraints.EAST;
            gb.setConstraints(l, gbc);
            bp[current].add(l);
            c[i] = new Choice();
            c[i].setName("event_" + i);
            gbc.gridx = 3;
            gbc.anchor = GridBagConstraints.WEST;
            gb.setConstraints(c[i], gbc);
            bp[current].add(c[i]);
            c[i].addItem(silent);
            gbc.gridy++;
        }
        Enumeration e = res.SOUNDS.keys();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            for (int i = 0; i < c.length; i++) c[i].addItem(name);
        }
        for (int i = 0; i < c.length; i++) {
            String value = properties.getString("event_" + (i + 1));
            if (value != null && !value.equals("")) c[i].select(value);
            items.addElement(new ConfChoice("event_" + (i + 1), c[i], true));
        }
        current++;
        gb = new GridBagLayout();
        gbc = new GridBagConstraints();
        bp[current].setLayout(gb);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(2, 2, 2, 2);
        gb.setConstraints(bp[current], gbc);
        l = new Label(res.getString("conf.admchan") + " :");
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        ban_list = new ImageButton(res.getString("conf.bankey"));
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.CENTER;
        gb.setConstraints(ban_list, gbc);
        bp[current].add(ban_list);
        except_list = new ImageButton(res.getString("conf.exceptkey"));
        gbc.gridy++;
        gb.setConstraints(except_list, gbc);
        bp[current].add(except_list);
        invit_list = new ImageButton(res.getString("conf.invitkey"));
        gbc.gridy++;
        gb.setConstraints(invit_list, gbc);
        bp[current].add(invit_list);
        chans = new java.awt.List(6);
        gbc.gridx++;
        gbc.gridy = 0;
        gbc.gridheight = 6;
        gbc.fill = GridBagConstraints.BOTH;
        gb.setConstraints(chans, gbc);
        bp[current].add(chans);
        moder = new Checkbox(res.getString("conf.mode.m"));
        gbc.gridx++;
        gbc.gridwidth = 3;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gb.setConstraints(moder, gbc);
        bp[current].add(moder);
        secret = new Checkbox(res.getString("conf.mode.s"));
        gbc.gridy++;
        gb.setConstraints(secret, gbc);
        bp[current].add(secret);
        invit = new Checkbox(res.getString("conf.mode.i"));
        gbc.gridy++;
        gb.setConstraints(invit, gbc);
        bp[current].add(invit);
        nickchange = new Checkbox(res.getString("conf.mode.N"));
        gbc.gridy++;
        gb.setConstraints(nickchange, gbc);
        bp[current].add(nickchange);
        l = new Label(res.getString("conf.mode.l") + " :");
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        limit = new TextField(2);
        gbc.gridx++;
        gbc.anchor = GridBagConstraints.WEST;
        gb.setConstraints(limit, gbc);
        bp[current].add(limit);
        l = new Label(res.getString("conf.users"));
        gbc.gridx++;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        l = new Label(res.getString("conf.mode.k") + " :");
        gbc.gridx -= 2;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        key = new TextField(8);
        gbc.gridx++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gb.setConstraints(key, gbc);
        bp[current].add(key);
        boxes.addElement(moder);
        boxes.addElement(invit);
        boxes.addElement(secret);
        boxes.addElement(nickchange);
        moder.setName("m");
        invit.setName("i");
        secret.setName("s");
        nickchange.setName("N");
        limit.setName("l");
        key.setName("k");
        current++;
        gb = new GridBagLayout();
        gbc = new GridBagConstraints();
        bp[current].setLayout(gb);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(2, 2, 2, 2);
        gb.setConstraints(bp[current], gbc);
        cb = new Checkbox(res.getString("conf.see_everything"), properties.getBoolean("see_everything_from_server"));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gb.setConstraints(cb, gbc);
        bp[current].add(cb);
        items.addElement(new ConfCheckbox("see_everything_from_server", cb));
        cb = new Checkbox(res.getString("conf.dcc_notify"), properties.getBoolean("on_dcc_notify_peer"));
        gbc.gridy++;
        gb.setConstraints(cb, gbc);
        bp[current].add(cb);
        items.addElement(new ConfCheckbox("on_dcc_notify_peer", cb));
        cb = new Checkbox(res.getString("conf.motd"), properties.getBoolean("request_motd"));
        gbc.gridy++;
        gb.setConstraints(cb, gbc);
        bp[current].add(cb);
        items.addElement(new ConfCheckbox("request_motd", cb));
        l = new Label(res.getString("conf.oper") + " :");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        operlogin = new TextField(8);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(operlogin, gbc);
        bp[current].add(operlogin);
        l = new Label(res.getString("conf.operpass") + " :");
        gbc.gridx++;
        gbc.fill = GridBagConstraints.NONE;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        operpass = new TextField(8);
        operpass.setEchoChar('*');
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(operpass, gbc);
        bp[current].add(operpass);
        l = new Label(res.getString("conf.password") + " :");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gb.setConstraints(l, gbc);
        bp[current].add(l);
        password = new TextField(properties.getString("irc_pass"), 10);
        password.setEchoChar('*');
        gbc.gridx++;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(password, gbc);
        bp[current].add(password);
        items.addElement(new ConfTextField("irc_pass", password));
        fonts.addItemListener(this);
        size.addActionListener(this);
        for (Enumeration en = items.elements(); en.hasMoreElements(); ) ((ConfComponent) en.nextElement()).addListener(this);
        for (Enumeration en = boxes.elements(); en.hasMoreElements(); ) ((Checkbox) en.nextElement()).addItemListener(this);
        limit.addActionListener(this);
        key.addActionListener(this);
        operpass.addActionListener(this);
        operlogin.addActionListener(this);
        chans.addItemListener(this);
        schemes.addItemListener(this);
        graphic_bg.addItemListener(this);
        text_bg.addItemListener(this);
        red.addAdjustmentListener(this);
        green.addAdjustmentListener(this);
        blue.addAdjustmentListener(this);
        for (int i = 0; i < panel_num; i++) {
            ib[i].addActionListener(this);
            ib[i].setActionCommand(s[i]);
        }
        ban_list.setActionCommand("bans");
        ban_list.addActionListener(this);
        except_list.setActionCommand("excepts");
        except_list.addActionListener(this);
        invit_list.setActionCommand("invits");
        invit_list.addActionListener(this);
        setTextBackground(eirc.getTextBackground());
        setTextForeground(eirc.getTextForeground());
        pack();
        setTitle(res.getString("conf.title"));
        setResizable(false);
        setVisible(true);
    }

    private void updateConfiguration() {
        for (Enumeration e = items.elements(); e.hasMoreElements(); ) {
            ConfComponent c = (ConfComponent) e.nextElement();
            String name = c.getName();
            Object o = c.getValue();
            if (o instanceof Boolean) {
                properties.setBoolean(name, ((Boolean) o).booleanValue());
            } else if (o instanceof Integer) {
                properties.setInt(name, ((Integer) o).intValue());
            } else if (o instanceof String) {
                properties.setString(name, (String) o);
            }
        }
    }

    private void updateModes() {
        chans.removeAll();
        String list[] = eirc.getChans();
        for (int i = 0; i < list.length; i++) chans.add(list[i]);
        if (list.length > 0) {
            int i = 0;
            OutputWindow ow = eirc.getCurrentPanel();
            if (ow != null) {
                String tag = ow.getPanelTag();
                while (i < list.length && !tag.equals(list[i])) i++;
                if (i == list.length) i = 0;
            }
            chans.select(i);
            updateModes(chans.getItem(i));
            ib[4].setEnabled(true);
        } else {
            ib[4].setEnabled(false);
        }
    }

    public void updateModes(String s) {
        Channel ch = eirc.getChannel(s);
        if (ch != null) {
            User user = ch.get(eirc.getNick());
            boolean hop = eirc.canOverride();
            boolean op = eirc.canOverride();
            if (user != null) {
                hop |= (user.isHalfOp() || user.isOp());
                op |= user.isOp();
            }
            moder.setEnabled(hop);
            invit.setEnabled(hop);
            secret.setEnabled(op);
            nickchange.setEnabled(op);
            limit.setEnabled(op);
            key.setEnabled(hop);
            moder.setState(ch.isModerated());
            secret.setState(ch.isSecret());
            invit.setState(ch.isInvitOnly());
            nickchange.setState(!ch.canNick());
            key.setText(ch.getKey());
            limit.setText(ch.getLimit() < 0 ? "" : String.valueOf(ch.getLimit()));
        }
    }

    public void setFont(Font f) {
        super.setFont(f);
    }

    public void setBackground(Color c) {
        super.setBackground(c);
    }

    public void setForeground(Color c) {
        super.setForeground(c);
    }

    public void setTextBackground(Color c) {
        size.setBackground(c);
        bye.setBackground(c);
        password.setBackground(c);
        key.setBackground(c);
        limit.setBackground(c);
        operlogin.setBackground(c);
        operpass.setBackground(c);
    }

    public void setTextForeground(Color c) {
        size.setForeground(c);
        bye.setForeground(c);
        password.setForeground(c);
        key.setForeground(c);
        limit.setForeground(c);
        operlogin.setForeground(c);
        operpass.setForeground(c);
    }

    public void actionPerformed(ActionEvent ev) {
        updateConfiguration();
        String action = ev.getActionCommand();
        Object src = ev.getSource();
        for (int i = 0; i < panel_num; i++) if (action.equals(s[i])) page_layout.show(page, action);
        if (src == size) change_font((Component) eirc);
        if (action.equals("bans")) {
            String a[] = { chans.getSelectedItem(), "+b" };
            eirc.sendMessage("MODE", a);
        }
        if (action.equals("excepts")) {
            String a[] = { chans.getSelectedItem(), "+e" };
            eirc.sendMessage("MODE", a);
        }
        if (action.equals("invits")) {
            String a[] = { chans.getSelectedItem(), "+I" };
            eirc.sendMessage("MODE", a);
        }
        if (src == key || src == limit) {
            String s = ((TextField) src).getText();
            char sign = (!s.equals("") ? '+' : '-');
            String a[] = { chans.getSelectedItem(), sign + ((Component) src).getName(), s };
            eirc.sendMessage("MODE", a);
        }
        if (src == operlogin || src == operpass) {
            String l = operlogin.getText();
            String p = operpass.getText();
            if (l.length() > 0 && p.length() > 0) {
                String a[] = { l, p };
                eirc.sendMessage("OPER", a);
            }
        }
        if (action.equals("ok")) {
            change_font((Component) eirc);
            processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }

    public void adjustmentValueChanged(AdjustmentEvent ae) {
        Object src = ae.getSource();
        if (src == red || src == green || src == blue) {
            schemes.deselect(schemes.getSelectedIndex());
            int col_red = red.getValue();
            int col_green = green.getValue();
            int col_blue = blue.getValue();
            r.setText(String.valueOf(col_red));
            g.setText(String.valueOf(col_green));
            b.setText(String.valueOf(col_blue));
            if (graphic_bg.getState()) eirc.setBackground(new Color(col_red, col_green, col_blue)); else eirc.setTextBackground(new Color(col_red, col_green, col_blue));
        }
    }

    public void itemStateChanged(ItemEvent e) {
        Object src = e.getSource();
        if (src == fonts) change_font((Component) eirc);
        if (src == chans) updateModes(chans.getSelectedItem());
        if (boxes.contains(src)) {
            char sign = (((Checkbox) src).getState() ? '+' : '-');
            String a[] = { chans.getSelectedItem(), sign + ((Component) src).getName() };
            eirc.sendMessage("MODE", a);
        }
        if (src instanceof Choice) {
            Choice ch = (Choice) src;
            if (ch.getName().startsWith("event")) {
                String s = ch.getSelectedItem();
                AudioClip au = (AudioClip) res.SOUNDS.get(s);
                if (au != null) au.play();
            }
        }
        if (src == graphic_bg || src == text_bg) {
            Color col = eirc.getBackground();
            if (text_bg.getState()) col = eirc.getTextBackground();
            r.setText(String.valueOf(col.getRed()));
            g.setText(String.valueOf(col.getGreen()));
            b.setText(String.valueOf(col.getBlue()));
            red.setValue(col.getRed());
            green.setValue(col.getGreen());
            blue.setValue(col.getBlue());
        }
        if (src == schemes) {
            Color c = eirc.getUserColor(schemes.getSelectedItem());
            red.setValue(c.getRed());
            green.setValue(c.getGreen());
            blue.setValue(c.getBlue());
            r.setText(String.valueOf(c.getRed()));
            g.setText(String.valueOf(c.getGreen()));
            b.setText(String.valueOf(c.getBlue()));
            if (graphic_bg.getState()) eirc.setBackground(c); else eirc.setTextBackground(c);
        }
    }

    private void change_font(Component target) {
        int font_size = target.getFont().getSize();
        try {
            font_size = Integer.parseInt(size.getText());
        } catch (NumberFormatException e) {
            size.setText(String.valueOf(font_size));
        }
        Font newFont = new Font(fonts.getSelectedItem(), Font.PLAIN, font_size);
        if (newFont.equals(target.getFont())) return;
        target.setFont(newFont);
        target.validate();
        if (target instanceof Frame) ((Frame) target).pack();
    }

    public void setVisible(boolean b) {
        if (b) updateModes();
        super.setVisible(b);
    }
}

abstract class ConfComponent {

    private String name;

    public ConfComponent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract Object getValue();

    public abstract void addListener(Object o);
}

class ConfTextField extends ConfComponent {

    private TextField component;

    public ConfTextField(String name, TextField tf) {
        super(name);
        this.component = tf;
    }

    public Object getValue() {
        return component.getText();
    }

    public void addListener(Object o) {
        component.addActionListener((ActionListener) o);
    }
}

class ConfCheckbox extends ConfComponent {

    private Checkbox component;

    public ConfCheckbox(String name, Checkbox cb) {
        super(name);
        this.component = cb;
    }

    public Object getValue() {
        return new Boolean(component.getState());
    }

    public void addListener(Object o) {
        component.addItemListener((ItemListener) o);
    }
}

class ConfCheckboxGroup extends ConfComponent {

    private CheckboxGroup component;

    public ConfCheckboxGroup(String name, CheckboxGroup cbg) {
        super(name);
        this.component = cbg;
    }

    public Object getValue() {
        Checkbox cb = component.getSelectedCheckbox();
        try {
            return Integer.decode(cb.getName());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public void addListener(Object o) {
    }
}

class ConfChoice extends ConfComponent {

    private Choice component;

    private boolean return_string;

    public ConfChoice(String name, Choice ch, boolean b) {
        super(name);
        this.component = ch;
        this.return_string = b;
    }

    public Object getValue() {
        if (return_string) return (new String(component.getSelectedItem())); else return (new Integer(component.getSelectedIndex()));
    }

    public void addListener(Object o) {
        component.addItemListener((ItemListener) o);
    }
}
