package net.sourceforge.sandirc.events;

import jerklib.events.IRCEvent;
import jerklib.events.PartEvent;
import net.sourceforge.sandirc.gui.IRCWindow;
import net.sourceforge.sandirc.gui.IRCWindowContainer;
import net.sourceforge.sandirc.gui.SandIRCFrame;
import net.sourceforge.sandirc.gui.ServerPanel;
import net.sourceforge.sandirc.gui.ServerTreeNode;
import javax.swing.SwingUtilities;

/**
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 */
public class PartEventRunnable implements IRCEventRunnable {

    public void run(IRCEvent e) {
        final PartEvent pe = (PartEvent) e;
        if (pe.getWho().equals(e.getSession().getNick())) return;
        ServerPanel panel = SandIRCFrame.getInstance().getServersPanel();
        ServerTreeNode node = panel.getOrCreateServerNode(e.getSession());
        IRCWindowContainer sessionContainer = node.getContainer();
        final IRCWindow win = sessionContainer.findWindowByChannel(pe.getChannel());
        if (win == null) return;
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                win.updateUsersList();
                win.insertDefault("*** " + pe.getWho() + " [" + pe.getUserName() + "@" + pe.getHostName() + "] has left " + pe.getChannelName() + " [" + pe.getPartMessage() + "]");
            }
        });
    }
}
