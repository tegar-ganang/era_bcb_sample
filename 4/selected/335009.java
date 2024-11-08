package net.sourceforge.sandirc.events;

import jerklib.events.IRCEvent;
import jerklib.events.InviteEvent;
import net.sourceforge.sandirc.gui.ServerPanel;
import net.sourceforge.sandirc.gui.SandIRCFrame;
import net.sourceforge.sandirc.gui.ServerTreeNode;
import net.sourceforge.sandirc.gui.IRCWindowContainer;
import net.sourceforge.sandirc.gui.IRCWindow;
import javax.swing.SwingUtilities;

/**
 * Created: Feb 21, 2008 5:16:01 PM
 *
 * @author <a href="mailto:robby.oconnor@gmail.com">Robert O'Connor</a>
 */
public class InviteEventRunnable implements IRCEventRunnable {

    public void run(IRCEvent e) {
        final InviteEvent inviteEvent = (InviteEvent) e;
        ServerPanel panel = SandIRCFrame.getInstance().getServersPanel();
        ServerTreeNode node = panel.getOrCreateServerNode(e.getSession());
        IRCWindowContainer sessionContainer = node.getContainer();
        final IRCWindow win = sessionContainer.getSelectedWindow();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                win.insertDefault("* " + inviteEvent.getNick() + " has invited you to " + inviteEvent.getChannelName());
            }
        });
    }
}
