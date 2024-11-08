package net.sourceforge.sandirc.events;

import jerklib.events.IRCEvent;
import jerklib.events.KickEvent;
import net.sourceforge.sandirc.gui.ServerPanel;
import net.sourceforge.sandirc.gui.SandIRCFrame;
import net.sourceforge.sandirc.gui.ServerTreeNode;
import net.sourceforge.sandirc.gui.IRCWindowContainer;
import net.sourceforge.sandirc.gui.IRCWindow;
import javax.swing.SwingUtilities;

/**
 * Created: Feb 21, 2008 3:59:22 PM
 *
 * @author <a href="mailto:robby.oconnor@gmail.com">Robert O'Connor</a>
 */
public class KickEventRunnable implements IRCEventRunnable {

    public void run(IRCEvent e) {
        final KickEvent ke = (KickEvent) e;
        ServerPanel panel = SandIRCFrame.getInstance().getServersPanel();
        ServerTreeNode node = panel.getOrCreateServerNode(e.getSession());
        IRCWindowContainer sessionContainer = node.getContainer();
        final IRCWindow win = sessionContainer.findWindowByChannel(ke.getChannel());
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                win.insertDefault("*** " + ke.getWho() + " was kicked by " + ke.byWho() + " (" + ke.getMessage() + ")");
                win.updateUsersList();
            }
        });
    }
}
