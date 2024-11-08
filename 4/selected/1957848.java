package net.jetrix.spectator.ui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import net.jetrix.GameState;
import net.jetrix.agent.ChannelInfo;
import net.jetrix.agent.QueryAgent;
import net.jetrix.agent.TSpecAgent;
import net.jetrix.messages.CommandMessage;

/**
 * Menu containing the list of the channels available on the server.
 * The channels are refreshed automatically 
 *
 * @author Emmanuel Bourg
 * @version $Revision: 751 $, $Date: 2008-08-28 04:20:33 -0400 (Thu, 28 Aug 2008) $
 */
public class ChannelMenu extends JMenu {

    private Timer timer = new Timer();

    private List<ChannelInfo> channels;

    private String channelName;

    private TSpecAgent agent;

    public ChannelMenu(TSpecAgent agent) {
        this.agent = agent;
        setText("Channels");
        setEnabled(false);
        timer.schedule(new TimerTask() {

            public void run() {
                try {
                    refreshMenu();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 1000, 10000);
        addMenuListener(new ChannelMenuListener());
    }

    public void stop() {
        timer.cancel();
    }

    private void refreshMenu() throws IOException {
        if (agent.getHostname() != null) {
            QueryAgent qagent = new QueryAgent();
            qagent.connect(agent.getHostname());
            channels = qagent.getChannels();
            qagent.disconnect();
            if (channels != null && !channels.isEmpty()) {
                setEnabled(true);
            }
        }
    }

    private void rebuild() {
        if (channels != null) {
            ButtonGroup group = new ButtonGroup();
            for (ChannelInfo channel : channels) {
                JRadioButtonMenuItem item = new JRadioButtonMenuItem();
                item.setText(channel.getName() + " (" + channel.getPlayernum() + " / " + channel.getPlayermax() + ")");
                item.setActionCommand(channel.getName());
                if (channel.getStatus() != GameState.STOPPED.getValue()) {
                    item.setForeground(Color.GREEN);
                } else if (channel.isEmpty()) {
                    item.setForeground(Color.LIGHT_GRAY);
                }
                if (channel.getName().equals(channelName)) {
                    item.setSelected(true);
                }
                group.add(item);
            }
            final ActionListener actionListener = new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    String chan = e.getActionCommand();
                    channelName = chan;
                    CommandMessage command = new CommandMessage();
                    command.setCommand("join");
                    command.addParameter("#" + chan);
                    try {
                        agent.send(command);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            };
            removeAll();
            Enumeration<AbstractButton> it = group.getElements();
            while (it.hasMoreElements()) {
                AbstractButton button = it.nextElement();
                button.addActionListener(actionListener);
                add(button);
            }
        }
    }

    class ChannelMenuListener implements MenuListener {

        public void menuSelected(MenuEvent e) {
            rebuild();
        }

        public void menuDeselected(MenuEvent e) {
        }

        public void menuCanceled(MenuEvent e) {
        }
    }
}
