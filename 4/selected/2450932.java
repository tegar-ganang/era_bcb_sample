package com.peterhi.player.action;

import java.awt.event.*;
import javax.swing.*;
import com.peterhi.net.messages.LeaveChannelMessage;
import com.peterhi.client.SocketClient;
import com.peterhi.player.Application;

public class LeaveChannelAction extends BaseAction {

    private static final Action instance = new LeaveChannelAction();

    public static Action getInstance() {
        return instance;
    }

    public LeaveChannelAction() {
        super();
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke('H', KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
        putValue(MNEMONIC_KEY, KeyEvent.VK_E);
    }

    public void actionPerformed(ActionEvent e) {
        LeaveChannelMessage message = new LeaveChannelMessage();
        message.channel = Application.getChannel();
        message.id = Application.getId();
        try {
            SocketClient.getInstance().send(message);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
