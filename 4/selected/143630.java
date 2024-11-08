package com.peterhi.player.actions;

import java.awt.event.ActionEvent;
import com.peterhi.player.ChannelDialog;

public class MIEnterChannelAction extends BaseAction {

    private static final MIEnterChannelAction instance = new MIEnterChannelAction();

    public static MIEnterChannelAction getInstance() {
        return instance;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ChannelDialog.getChannelDialog().setVisible(true);
    }
}
