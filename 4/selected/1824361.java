package org.thole.phiirc.client.view.swing.actions;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;
import javax.swing.JList;
import org.thole.phiirc.client.controller.Controller;

/**
 * 
 * @author hendrik
 * @date 26.04.2009
 *
 */
public class UserQueryListener implements MouseListener {

    public UserQueryListener() {
        super();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            final int listIndex = ((JList) e.getSource()).locationToIndex(e.getPoint());
            final String user = ((JList) e.getSource()).getModel().getElementAt(listIndex).toString();
            ActiveChannel.active(user);
            Controller.getInstance().getClient().getChannelList().setChannelData(new Vector<String>(Controller.getInstance().getCWatcher().chanList()));
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }
}
