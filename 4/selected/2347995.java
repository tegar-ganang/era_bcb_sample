package net.sourceforge.sandirc.events;

import jerklib.events.IRCEvent;
import jerklib.events.NickChangeEvent;
import net.sourceforge.sandirc.gui.IRCWindow;
import net.sourceforge.sandirc.gui.IRCWindowContainer;
import net.sourceforge.sandirc.gui.SandIRCFrame;
import net.sourceforge.sandirc.gui.ServerPanel;
import net.sourceforge.sandirc.gui.ServerTreeNode;
import net.sourceforge.sandirc.utils.WindowUtilites;
import java.awt.EventQueue;
import java.util.List;

/**
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 */
public class NickChangedEventRunnable implements IRCEventRunnable {

    public void run(final IRCEvent e) {
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                NickChangeEvent nce = (NickChangeEvent) e;
                ServerPanel panel = SandIRCFrame.getInstance().getServersPanel();
                ServerTreeNode node = panel.getOrCreateServerNode(e.getSession());
                IRCWindowContainer sessionContainer = node.getContainer();
                List<IRCWindow> windows = sessionContainer.getIRCWindows();
                windows = WindowUtilites.getWindowsForNick(nce.getNewNick(), e.getSession(), windows);
                for (final IRCWindow win : windows) {
                    if (nce.getNewNick().equals(e.getSession().getNick())) {
                        win.insertDefault("** You are now known as " + nce.getNewNick());
                    } else {
                        win.insertDefault("** " + nce.getOldNick() + " is now known as " + nce.getNewNick());
                    }
                    if (win.getDocument().getChannel() != null) win.updateUsersList();
                }
            }
        });
    }
}
