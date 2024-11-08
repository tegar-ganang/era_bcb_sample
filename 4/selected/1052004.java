package com.peterhi.player.actions;

import static com.peterhi.player.ResourceLocator.*;
import java.awt.event.ActionEvent;
import com.peterhi.player.ChannelDialog;
import com.peterhi.player.Window;
import com.peterhi.client.SocketClient;
import com.peterhi.net.messages.EnterChannelMessage;

public class EnterChannelAction extends BaseAction {

    private static final EnterChannelAction instance = new EnterChannelAction();

    public static EnterChannelAction getInstance() {
        return instance;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ChannelDialog c = ChannelDialog.getChannelDialog();
        String channel = c.getName();
        if (channel == null || channel.length() <= 0) {
            c.setStatusText(getString(c, "NO_NAME"));
            return;
        }
        c.setFrozen(true);
        EnterChannelMessage message = new EnterChannelMessage();
        message.channel = channel;
        Window.getWindow().channel = channel;
        try {
            SocketClient.getInstance().send(message);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
