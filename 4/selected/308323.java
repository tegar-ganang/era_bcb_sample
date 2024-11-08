package net.sourceforge.sandirc.events;

import javax.swing.SwingUtilities;
import jerklib.events.IRCEvent;
import jerklib.events.JoinCompleteEvent;
import net.sourceforge.sandirc.gui.*;
import net.sourceforge.sandirc.gui.IRCWindowContainer;
import net.sourceforge.sandirc.gui.SandIRCFrame;
import net.sourceforge.sandirc.gui.ServerPanel;

/**
 *
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 */
public class JoinCompleteEventRunnable implements IRCEventRunnable {

    public void run(IRCEvent e) {
        final JoinCompleteEvent je = (JoinCompleteEvent) e;
        ServerPanel panel = SandIRCFrame.getInstance().getServersPanel();
        ServerTreeNode node = panel.getOrCreateServerNode(je.getSession());
        final IRCWindowContainer sessionContainer = node.getContainer();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                sessionContainer.findWindowByChannel(je.getChannel());
            }
        });
    }
}
