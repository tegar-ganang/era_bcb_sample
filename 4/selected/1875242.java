package tk.bot;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class BotSwingDisplay extends JPanel implements BotUserDisplay, ActionListener {

    TKBot bot;

    BotGUI gui;

    static LinkedList list = new LinkedList();

    JList userList = new JList(list.toArray());

    JScrollPane listScroller = new JScrollPane(userList);

    static JLabel channel;

    JToolBar controls = new JToolBar();

    BotJarReader reader = new BotJarReader("TKBot.jar");

    JButton kick = new JButton(new ImageIcon(reader.getBytes("kick.jpg")));

    JButton ban = new JButton(new ImageIcon(reader.getBytes("ban.jpg")));

    JButton whisper = new JButton(new ImageIcon(reader.getBytes("whisper.jpg")));

    Updater updater;

    public BotSwingDisplay(TKBot bot, BotGUI gui) {
        this.bot = bot;
        this.gui = gui;
        this.setLayout(new BorderLayout());
        if (userList == null) userList = new JList();
        if (channel == null) channel = new JLabel("None");
        this.add(channel, "North");
        userList.setCellRenderer(new BotCellRenderer());
        this.add(listScroller, "Center");
        controls.setLayout(new GridLayout(1, 3));
        kick.addActionListener(this);
        ban.addActionListener(this);
        whisper.addActionListener(this);
        controls.add(kick);
        controls.add(ban);
        controls.add(whisper);
        this.add(controls, "South");
        setPreferredSize(new Dimension(120, getPreferredSize().height));
        updater = new Updater(bot);
        updater.start();
    }

    public void addUser(BotUser user) {
        if (user.getOperator()) {
            list.addFirst(user);
        } else list.add(user);
        if (list.size() <= 15) refresh();
    }

    public void removeUser(BotUser user) {
        BotUser temp = new BotUser(user.getName(), 0, false);
        for (int i = 0; i < BotUser.CLIENTS.length; i++) {
            try {
                temp.currentClient = BotUser.CLIENTS[i];
                list.remove(temp);
            } catch (IllegalArgumentException e) {
            }
        }
        if (list.size() <= 15) refresh();
    }

    public void removeAll() {
        list.clear();
        refresh();
    }

    public void setChannel(String name) {
        channel.setText(name);
    }

    public String getChannel() {
        return channel.getText();
    }

    public void refresh() {
        int[] indices = userList.getSelectedIndices();
        userList.setListData(list.toArray());
        userList.repaint();
        userList.setSelectedIndices(indices);
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        Object source = e.getSource();
        Object[] users = userList.getSelectedValues();
        if (source.equals(kick)) {
            for (int i = 0; i < users.length; i++) {
                bot.addCommand("/kick " + ((BotUser) users[i]).getName());
            }
        }
        if (source.equals(ban)) {
            for (int i = 0; i < users.length; i++) {
                bot.addCommand("/ban " + ((BotUser) users[i]).getName());
            }
        }
        if (source.equals(whisper)) {
            for (int i = 0; i < users.length; i++) {
                (new BotSwingWhisper(((BotUser) users[i]).getName(), bot)).display();
            }
            gui.getEntryArea().clear();
        }
    }

    public void dispose() {
        updater.shutdown();
    }

    public class BotCellRenderer extends DefaultListCellRenderer implements ListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean selected, boolean focus) {
            DefaultListCellRenderer component = (DefaultListCellRenderer) super.getListCellRendererComponent(list, value, index, selected, focus);
            if (!(value instanceof BotUser)) {
                return component;
            } else {
                BotUser user = (BotUser) value;
                ImageIcon icon;
                String file;
                component.setText(user.name);
                switch(user.currentClient) {
                    case BotUser.D2DV:
                    case BotUser.DRTL:
                        file = "drtl.jpg";
                        break;
                    case BotUser.STAR:
                        file = "star.jpg";
                        break;
                    case BotUser.SEXP:
                        file = "sexp.jpg";
                        break;
                    case BotUser.CHAT:
                        file = "chat.jpg";
                        break;
                    case BotUser.W2BN:
                        file = "war2.jpg";
                        break;
                    default:
                        file = "";
                }
                if (user.operator) {
                    file = "opss.jpg";
                }
                if (!file.equals("")) {
                    icon = new ImageIcon(reader.getBytes(file));
                    component.setIcon(icon);
                }
                return component;
            }
        }
    }

    public class Updater extends Thread implements BotThread {

        boolean finished = false;

        public Updater(TKBot bot) {
            super(bot.getThreadGroup(), "User Display Updater");
            setPriority(MIN_PRIORITY);
        }

        public void run() {
            while (!TKBot.getStatus() && !finished) {
                refresh();
                try {
                    if (isVisible() && (list.size() > 15)) refresh();
                    sleep(3000);
                } catch (Exception e) {
                }
            }
        }

        public void shutdown() {
            finished = true;
        }
    }

    public void paint(Graphics g) {
        try {
            super.paint(g);
        } catch (Exception e) {
            System.out.println("Exception in Paint caught!");
            System.out.println(e);
        }
    }
}
