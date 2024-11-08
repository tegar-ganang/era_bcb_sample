package com.google.code.cubeirc.ui.adapters;

import java.awt.MouseInfo;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.wb.swt.SWTResourceManager;
import org.pircbotx.Channel;
import org.pircbotx.User;
import com.google.code.cubeirc.Main;

public class ChannelPopmenuListener implements Listener {

    @Getter
    @Setter
    private Tree tSender;

    @Getter
    @Setter
    private Channel channel;

    @Getter
    @Setter
    private Shell shell;

    public ChannelPopmenuListener(Shell shell, Tree sender, Channel channel) {
        setTSender(sender);
        setChannel(channel);
        setShell(shell);
    }

    @SuppressWarnings("unused")
    @Override
    public void handleEvent(Event event) {
        if (getTSender().getSelection().length != 0) {
            TreeItem it = getTSender().getSelection()[0];
            if (it.getData() instanceof User) {
                final User sUser = (User) it.getData();
                final ChannelPopupListener cpl = new ChannelPopupListener(getShell(), sUser, getChannel());
                Menu menu = new Menu(getTSender());
                MenuItem mi = new MenuItem(menu, SWT.NORMAL);
                mi.setText("//" + sUser.getNick());
                mi.setEnabled(false);
                addMenuItem(menu, SWT.NORMAL, "Send private message", SWTResourceManager.getImage(Main.class, "/com/google/code/cubeirc/resources/img_message.png"), cpl);
                MenuItem sep3 = new MenuItem(menu, SWT.SEPARATOR);
                addMenuItem(menu, SWT.NORMAL, "DCC Send file", SWTResourceManager.getImage(Main.class, "/com/google/code/cubeirc/resources/img_sendfile.png"), cpl);
                addMenuItem(menu, SWT.NORMAL, "DCC Chat", SWTResourceManager.getImage(Main.class, "/com/google/code/cubeirc/resources/img_sendfile.png"), cpl);
                MenuItem sep4 = new MenuItem(menu, SWT.SEPARATOR);
                addMenuItem(menu, SWT.NORMAL, "Op", SWTResourceManager.getImage(Main.class, "/com/google/code/cubeirc/resources/img_op.png"), cpl);
                addMenuItem(menu, SWT.NORMAL, "DeOp", SWTResourceManager.getImage(Main.class, "/com/google/code/cubeirc/resources/img_deop.png"), cpl);
                MenuItem sep1 = new MenuItem(menu, SWT.SEPARATOR);
                addMenuItem(menu, SWT.NORMAL, "Voice", SWTResourceManager.getImage(Main.class, "/com/google/code/cubeirc/resources/img_op.png"), cpl);
                addMenuItem(menu, SWT.NORMAL, "DeVoice", SWTResourceManager.getImage(Main.class, "/com/google/code/cubeirc/resources/img_deop.png"), cpl);
                MenuItem sep2 = new MenuItem(menu, SWT.SEPARATOR);
                addMenuItem(menu, SWT.NORMAL, "Kick", SWTResourceManager.getImage(Main.class, "/com/google/code/cubeirc/resources/img_trash.png"), cpl);
                menu.setLocation(getMousePosition());
                menu.setVisible(true);
            }
        }
    }

    private Point getMousePosition() {
        return new Point(MouseInfo.getPointerInfo().getLocation().x, MouseInfo.getPointerInfo().getLocation().y);
    }

    private void addMenuItem(Menu parent, int style, String text, Image image, SelectionListener sl) {
        MenuItem mi = new MenuItem(parent, style);
        mi.setText(text);
        if (image != null) mi.setImage(image);
        mi.addSelectionListener(sl);
    }
}
