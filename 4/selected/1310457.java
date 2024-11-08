package net.sourceforge.sandirc.events;

import jerklib.Channel;
import jerklib.events.*;
import net.sourceforge.sandirc.gui.*;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 */
public class QuitEventRunnable implements IRCEventRunnable {

    public void run(IRCEvent e) {
        final QuitEvent qe = (QuitEvent) e;
        ServerPanel panel = SandIRCFrame.getInstance().getServersPanel();
        ServerTreeNode node = panel.getOrCreateServerNode(e.getSession());
        IRCWindowContainer sessionContainer = node.getContainer();
        List<Channel> channels = qe.getChannelList();
        for (Channel channel : channels) {
            final IRCWindow window = sessionContainer.findWindowByChannel(channel);
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    window.updateUsersList();
                    window.insertDefault("*** " + qe.getWho() + " [" + qe.getUserName() + "@" + qe.getHostName() + "] has quit [" + qe.getQuitMessage() + "]");
                }
            });
        }
    }
}
