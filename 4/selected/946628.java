package net.sourceforge.sandirc.events;

import java.awt.EventQueue;
import jerklib.events.IRCEvent;
import jerklib.events.WhoisEvent;
import net.sourceforge.sandirc.gui.IRCWindowContainer;
import net.sourceforge.sandirc.gui.SandIRCFrame;
import net.sourceforge.sandirc.gui.ServerPanel;
import net.sourceforge.sandirc.gui.ServerTreeNode;

/**
 * Created: Feb 26, 2008 11:41:07 PM
 *
 * @author <a href="mailto:robby.oconnor@gmail.com">Robert O'Connor</a>
 */
public class WhoisEventRunnable implements IRCEventRunnable {

    public void run(final IRCEvent e) {
        WhoisEvent we = (WhoisEvent) e;
        StringBuilder channels = new StringBuilder();
        if (we.getChannelNames() != null) {
            for (String chan : we.getChannelNames()) {
                channels.append(chan + " ");
            }
        }
        final StringBuilder buff = new StringBuilder();
        buff.append("** Whois \n");
        buff.append("** Nick:" + we.getNick() + "[" + we.getUser() + "@" + we.getHost() + "]\n");
        buff.append("** Real:" + we.getRealName() + "\n");
        buff.append("** Server:" + we.whoisServer() + "\n");
        buff.append("** Signon Time:" + we.signOnTime() + "\n");
        buff.append(we.isIdle() ? "** Idle Time:" + we.secondsIdle() + "\n" : "");
        buff.append(channels.length() > 0 ? "** Channels:" + channels : "");
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                ServerPanel panel = SandIRCFrame.getInstance().getServersPanel();
                ServerTreeNode node = panel.getOrCreateServerNode(e.getSession());
                IRCWindowContainer sessionContainer = node.getContainer();
                sessionContainer.getSelectedWindow().insertDefault(buff.toString());
            }
        });
    }
}
