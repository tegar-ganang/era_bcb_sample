package net.sourceforge.sandirc.events;

import java.awt.EventQueue;
import jerklib.events.IRCEvent;
import jerklib.events.MessageEvent;
import net.sourceforge.sandirc.gui.IRCWindow;
import net.sourceforge.sandirc.gui.IRCWindowContainer;
import net.sourceforge.sandirc.gui.SandIRCFrame;
import net.sourceforge.sandirc.gui.ServerPanel;
import net.sourceforge.sandirc.gui.ServerTreeNode;

/**
 *
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 */
public class ChannelMsgEventRunnable implements IRCEventRunnable {

    public void run(final IRCEvent e) {
        final MessageEvent chanEvent = (MessageEvent) e;
        ServerPanel panel = SandIRCFrame.getInstance().getServersPanel();
        ServerTreeNode node = panel.getOrCreateServerNode(e.getSession());
        final IRCWindowContainer sessionContainer = node.getContainer();
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                if (chanEvent.getType().equals(IRCEvent.Type.PRIVATE_MESSAGE)) {
                    IRCWindow window = sessionContainer.getPrivateMessageWindow(chanEvent.getNick(), e.getSession());
                    window.insertMsg(chanEvent.getNick(), chanEvent.getMessage());
                } else {
                    IRCWindow window = sessionContainer.findWindowByChannel(chanEvent.getChannel());
                    window.insertMsg(chanEvent.getNick(), chanEvent.getMessage());
                }
            }
        });
    }
}
