package com.peterhi.client.nio.handlers;

import com.peterhi.StatusCode;
import com.peterhi.beans.ChannelBean;
import com.peterhi.client.Application;
import com.peterhi.client.impl.managers.StoreManager;
import com.peterhi.client.nio.DatagramHandler;
import com.peterhi.client.nio.DatagramSession;
import com.peterhi.client.nio.NetworkManager;
import com.peterhi.client.nio.events.ChannelEvent;
import com.peterhi.net.msg.EChnlRsp;

public class EnterChannelFeedbackHandler implements DatagramHandler {

    public void handle(NetworkManager man, DatagramSession session, byte[] data) {
        try {
            EChnlRsp f = new EChnlRsp();
            f.deserialize(data, 22);
            if (f.getStatusCode() == StatusCode.OK) getStore().setChannel(f.getChannelBean());
            ChannelBean[] array = new ChannelBean[] { f.getChannelBean() };
            ChannelEvent e = new ChannelEvent(this, array, f.getStatusCode());
            man.fireOnEnteredChannel(e);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private StoreManager getStore() {
        return Application.getApplication().getManager(StoreManager.class);
    }
}
