package net.sourceforge.sandirc.events;

import java.awt.EventQueue;
import net.sourceforge.sandirc.gui.IRCWindow;
import net.sourceforge.sandirc.gui.IRCWindowContainer;
import net.sourceforge.sandirc.gui.SandIRCFrame;
import net.sourceforge.sandirc.gui.ServerPanel;
import net.sourceforge.sandirc.gui.ServerTreeNode;
import jerklib.events.ChannelListEvent;
import jerklib.events.IRCEvent;

public class ChanListEventRunnable implements IRCEventRunnable {

    public void run(final IRCEvent e) {
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                ServerPanel panel = SandIRCFrame.getInstance().getServersPanel();
                ServerTreeNode node = panel.getOrCreateServerNode(e.getSession());
                IRCWindowContainer sessionContainer = node.getContainer();
                IRCWindow noticeWindow = sessionContainer.getNoticeWindow();
                ChannelListEvent cle = (ChannelListEvent) e;
                noticeWindow.insertDefault(cle.getChannelName() + " " + cle.getNumberOfUser() + "[" + cle.getTopic() + "]");
            }
        });
    }
}
