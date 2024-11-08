package net.sourceforge.sandirc.events;

import jerklib.events.IRCEvent;
import jerklib.events.JoinEvent;
import net.sourceforge.sandirc.gui.IRCWindow;
import net.sourceforge.sandirc.gui.IRCWindowContainer;
import net.sourceforge.sandirc.gui.SandIRCFrame;
import net.sourceforge.sandirc.gui.ServerPanel;
import net.sourceforge.sandirc.gui.ServerTreeNode;
import javax.swing.SwingUtilities;

/**
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 */
public class JoinEventRunnable implements IRCEventRunnable {

    public void run(IRCEvent e) {
        final JoinEvent je = (JoinEvent) e;
        ServerPanel panel = SandIRCFrame.getInstance().getServersPanel();
        ServerTreeNode node = panel.getOrCreateServerNode(e.getSession());
        IRCWindowContainer sessionContainer = node.getContainer();
        final IRCWindow window = sessionContainer.findWindowByChannel(je.getChannel());
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                window.updateUsersList();
                window.insertDefault("*** " + je.getNick() + " [" + je.getUserName() + "@" + je.getHostName() + "] has joined " + je.getChannelName());
            }
        });
    }
}
