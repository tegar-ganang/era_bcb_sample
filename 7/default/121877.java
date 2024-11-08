import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;
import java.util.*;
import java.text.Collator;
import java.text.MessageFormat;
import java.text.ParseException;
import ar.com.jkohen.irc.Channel;
import ar.com.jkohen.irc.User;
import ar.com.jkohen.irc.MircMessage;
import ar.com.jkohen.awt.ChatPanel;
import ar.com.jkohen.awt.ImageButton;
import ar.com.jkohen.awt.MircSmileyTextArea;
import ar.com.jkohen.util.Resources;
import ar.com.jkohen.util.ConfigurationProperties;
import com.splendid.awtchat.*;

public class ChanListWindow extends ChatPanel implements ActionListener, ItemListener, HyperlinkReceiver, Observer, CopyText {

    private EIRC eirc;

    private String title;

    private MircSmileyTextArea text_canvas;

    private TextField new_chan, users_nbr;

    private Checkbox byname, bynumber;

    private ImageButton b;

    private Label label;

    private int number = 0;

    private Resources res;

    protected static final String min_str = Resources.getString("channel_list.minima");

    protected static final String users_str = Resources.getString("channel_list.users");

    protected static final String newchan_str = Resources.getString("channel_list.new");

    protected static final String refresh = Resources.getString("channel_list.refresh");

    private ChannelItem chans[];

    private int count = 0;

    private Collator collator;

    private final int SORT_BYNAME = 0;

    private final int SORT_BYNUM = 1;

    private final int SORT_BYTOPIC = 2;

    private int sort_criteria = SORT_BYNAME;

    private final int MAXCHANS = 40000;

    private String list_parameter;

    public ChanListWindow(EIRC eirc, String title) {
        super(title);
        text_canvas = new MircSmileyTextArea(this, this);
        this.eirc = eirc;
        this.title = title;
        this.collator = Collator.getInstance();
        this.chans = new ChannelItem[MAXCHANS];
        text_canvas.setMode(eirc.scrollSpeed());
        text_canvas.setBufferlen(MAXCHANS);
        text_canvas.setBreaks(false);
        int tabs[] = { -1, 160, 190 };
        text_canvas.setTabs(tabs);
        GridBagLayout gb = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        setLayout(gb);
        gbc.insets = new Insets(2, 2, 2, 2);
        label = new Label(newchan_str + " : ");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0;
        gbc.weightx = 0;
        gb.setConstraints(label, gbc);
        add(label);
        new_chan = new TextField(20);
        gbc.gridx = 3;
        gbc.weighty = 0;
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gb.setConstraints(new_chan, gbc);
        add(new_chan);
        Label l = new Label(min_str);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gb.setConstraints(l, gbc);
        add(l);
        gbc.gridx++;
        users_nbr = new TextField("1", 2);
        gb.setConstraints(users_nbr, gbc);
        add(users_nbr);
        gbc.gridx++;
        l = new Label(users_str);
        gb.setConstraints(l, gbc);
        add(l);
        l = new Label(res.getString("channel_list.sort") + " :");
        gbc.gridx++;
        gb.setConstraints(l, gbc);
        add(l);
        CheckboxGroup cbg = new CheckboxGroup();
        byname = new Checkbox(res.getString("channel_list.byname"), cbg, (sort_criteria == SORT_BYNAME));
        gbc.gridx++;
        gb.setConstraints(byname, gbc);
        add(byname);
        bynumber = new Checkbox(res.getString("channel_list.bynumber"), cbg, (sort_criteria == SORT_BYNUM));
        gbc.gridx++;
        gb.setConstraints(bynumber, gbc);
        add(bynumber);
        b = new ImageButton(refresh);
        b.setWaitType();
        b.setEnabled(true);
        b.setActionCommand("list");
        gbc.gridx++;
        gbc.anchor = GridBagConstraints.WEST;
        gb.setConstraints(b, gbc);
        add(b);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.gridwidth = 7;
        gb.setConstraints(text_canvas, gbc);
        add(text_canvas);
        bynumber.addItemListener(this);
        byname.addItemListener(this);
        new_chan.addActionListener(this);
        users_nbr.addActionListener(this);
        b.addActionListener(this);
    }

    public void clear() {
        count = 0;
    }

    public void listChannels() {
        String l[];
        int min = 1;
        try {
            min = Integer.parseInt(users_nbr.getText());
        } catch (NumberFormatException e) {
        }
        if (--min > 0) {
            l = new String[1];
            l[0] = MessageFormat.format(list_parameter, new Object[] { new Integer(min) });
            eirc.sendMessage("LIST", l);
        } else {
            l = new String[0];
            eirc.sendMessage("LIST", l);
        }
    }

    private void display() {
        text_canvas.clear();
        text_canvas.setMode(text_canvas.SAFE);
        sort();
        for (int i = 0; i < count; i++) {
            ChannelItem channel = chans[i];
            String topic = channel.getTopic();
            if (topic != null) {
                if (topic.startsWith("[+")) {
                    MessageFormat mf = new MessageFormat("[+{0}] {1}");
                    try {
                        Object o[] = mf.parse(topic);
                        if (o[1] != null) topic = o[1].toString();
                    } catch (ParseException ex) {
                    }
                }
            }
            text_canvas.append(channel.getTag() + " " + MircMessage.COLOR + "3 " + channel.getUsers() + " " + MircMessage.RESET + topic);
        }
        text_canvas.setMode(eirc.scrollSpeed());
        text_canvas.home();
        b.setEnabled(true);
    }

    public boolean initialized() {
        return (count >= 0);
    }

    public void add(ChannelItem ch) {
        if (count < MAXCHANS) chans[count++] = ch; else stop();
    }

    public void stop() {
        display();
    }

    public int number() {
        return (count);
    }

    private void sort() {
        sort(0, count - 1);
        switch(sort_criteria) {
            case SORT_BYNAME:
                sortByName();
                break;
            case SORT_BYNUM:
                sortByNum();
        }
    }

    private void sortByName() {
        int i, j;
        String v;
        for (i = 0; i < count; i++) {
            ChannelItem ch = chans[i];
            v = ch.getTag();
            j = i;
            while ((j > 0) && (collator.compare(chans[j - 1].getTag(), v) > 0)) {
                chans[j] = chans[j - 1];
                j--;
            }
            chans[j] = ch;
        }
    }

    private void sortByNum() {
        int i, j, w;
        for (i = count - 1; i >= 0; i--) {
            ChannelItem ch = chans[i];
            w = ch.getUsers();
            j = i;
            while ((j < count - 1) && (chans[j + 1].getUsers() > w)) {
                chans[j] = chans[j + 1];
                j++;
            }
            chans[j] = ch;
        }
    }

    private void sort(int l, int r) {
        int i, j;
        if ((r - l) > 4) {
            i = (r + l) / 2;
            switch(sort_criteria) {
                case SORT_BYNAME:
                    String s1 = chans[i].getTag();
                    String s2 = chans[l].getTag();
                    String s3 = chans[r].getTag();
                    if (collator.compare(s2, s1) < 0) swap(l, i);
                    if (collator.compare(s2, s3) < 0) swap(l, r);
                    if (collator.compare(s1, s3) < 0) swap(i, r);
                    break;
                case SORT_BYNUM:
                    int comp1 = chans[i].getUsers();
                    int comp2 = chans[l].getUsers();
                    int comp3 = chans[r].getUsers();
                    if (comp2 < comp1) swap(l, i);
                    if (comp2 < comp3) swap(l, r);
                    if (comp1 < comp3) swap(i, r);
            }
            j = r - 1;
            swap(i, j);
            i = l;
            switch(sort_criteria) {
                case SORT_BYNAME:
                    String v = chans[j].getTag();
                    for (; ; ) {
                        while (collator.compare(chans[++i].getTag(), v) > 0) ;
                        while (collator.compare(chans[--j].getTag(), v) < 0) ;
                        if (j < i) break;
                        swap(i, j);
                    }
                    break;
                case SORT_BYNUM:
                    int w = chans[j].getUsers();
                    for (; ; ) {
                        while (chans[++i].getUsers() > w) ;
                        while (chans[--j].getUsers() < w) ;
                        if (j < i) break;
                        swap(i, j);
                    }
            }
            swap(i, r - 1);
            sort(l, j);
            sort(i + 1, r);
        }
    }

    private void swap(int i, int j) {
        ChannelItem ch = chans[i];
        chans[i] = chans[j];
        chans[j] = ch;
    }

    public void setBackground(Color c) {
        super.setBackground(c);
        b.setBackground(c);
        Component cp[] = getComponents();
        for (int i = 0; i < cp.length; i++) if (cp[i] instanceof Label || cp[i] instanceof Checkbox) cp[i].setBackground(c);
    }

    public void setTextBackground(Color c) {
        new_chan.setBackground(c);
        users_nbr.setBackground(c);
        text_canvas.setBackground(c);
    }

    public void setSelectedBackground(Color c) {
        text_canvas.setSelectedBackground(c);
    }

    public void setForeground(Color c) {
        super.setForeground(c);
    }

    public void setTextForeground(Color c) {
        new_chan.setForeground(c);
        users_nbr.setForeground(c);
    }

    public void setFont(Font f) {
        new_chan.setFont(f);
        users_nbr.setFont(f);
        text_canvas.setFont(f);
        b.setFont(f);
    }

    protected void visitURL(URL url) {
        eirc.visitURL(url);
    }

    protected void joinChannel(String name) {
        eirc.joinChannel(name);
    }

    public void handleHyperlink(String link) {
        if (Channel.isChannel(link)) {
            joinChannel(link);
        } else {
            try {
                visitURL(new URL(link));
            } catch (MalformedURLException e) {
            }
        }
    }

    public void handleNick(String nick) {
    }

    public void addText(String s) {
        eirc.cutPaste(s);
    }

    public void update(Observable o, Object arg) {
        ConfigurationProperties props = (ConfigurationProperties) o;
        if (arg == null || arg.equals("list_parameter")) this.list_parameter = props.getString("list_parameter");
        if (arg == null || arg.equals("scroll_speed")) text_canvas.setMode(props.getInt("scroll_speed"));
    }

    public void itemStateChanged(ItemEvent ev) {
        Object ob = ev.getSource();
        if (ob == byname || ob == bynumber) {
            if (byname.getState()) sort_criteria = SORT_BYNAME;
            if (bynumber.getState()) sort_criteria = SORT_BYNUM;
            display();
        }
    }

    public void actionPerformed(ActionEvent ev) {
        String ac = ev.getActionCommand();
        Object ob = ev.getSource();
        if (ob == new_chan) {
            String chan = new_chan.getText();
            if (!Channel.isChannel(chan)) chan = "#" + chan;
            if (chan.length() > 1) joinChannel(chan);
        }
        if (ac.equals("list") || ob == users_nbr) {
            b.setEnabled(false);
            listChannels();
        }
    }
}
