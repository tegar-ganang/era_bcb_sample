package com.peterhi.player.action;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import com.peterhi.PeterHi;
import com.peterhi.net.messages.LeaveChannelMessage;
import com.peterhi.client.SocketClient;
import com.peterhi.player.*;

public class KickAction extends BaseAction {

    private static final Action instance = new KickAction();

    public static Action getInstance() {
        return instance;
    }

    public void actionPerformed(ActionEvent e) {
        SocketClient sc = SocketClient.getInstance();
        Classroom c = Application.getWindow().getView(Classroom.class);
        int[] array = c.getSelectedRows();
        if (array == null || array.length <= 0) {
            return;
        }
        int index = array[0];
        Classmate cm = c.getModel().get(index);
        if (cm.getState().getRole() == PeterHi.ROLE_STUDENT) {
            LeaveChannelMessage message = new LeaveChannelMessage();
            message.channel = Application.getChannel();
            message.id = cm.getId();
            try {
                sc.send(message);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
