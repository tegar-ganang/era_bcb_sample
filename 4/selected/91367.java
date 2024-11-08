package com.peterhi.player.actions;

import java.awt.event.ActionEvent;
import com.peterhi.player.ChannelDialog;

public class ShowChannelDialogAction extends BaseAction {

    private static final ShowChannelDialogAction instance = new ShowChannelDialogAction();

    public static ShowChannelDialogAction getInstance() {
        return instance;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ChannelDialog.getChannelDialog().setVisible(true);
    }
}
