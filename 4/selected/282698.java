package com.peterhi.client.nio.handlers;

import com.peterhi.StatusCode;
import com.peterhi.beans.ChannelBean;
import com.peterhi.beans.PeerBean;
import com.peterhi.beans.TalkState;
import com.peterhi.client.App;
import com.peterhi.client.managers.StoreManager;
import com.peterhi.client.nio.DatagramHandler;
import com.peterhi.client.nio.DatagramSession;
import com.peterhi.client.nio.NetworkManager;
import com.peterhi.client.nio.events.ChannelEvent;
import com.peterhi.client.nio.events.PeerEvent;
import com.peterhi.client.voice.Speaker;
import com.peterhi.net.Protocol;
import com.peterhi.net.msg.EChnlRsp;
import com.peterhi.net.msg.ISesMsg;
import com.peterhi.net.packet.Packet;

public class EChnlRspHandler implements DatagramHandler {

    public void handle(NetworkManager man, DatagramSession server, Packet packet) throws Exception {
        if (getStore().getChannel() != null) return;
        EChnlRsp f = new EChnlRsp();
        f.deserialize(packet.getBuf(), Protocol.RUDP_CURSOR);
        ChannelBean cbean = f.getChannelBean();
        if (f.getStatusCode() == StatusCode.OK) getStore().setChannel(cbean);
        ChannelBean[] array = new ChannelBean[] { cbean };
        ChannelEvent e = new ChannelEvent(this, array, f.getStatusCode());
        man.fireOnEnteredChannel(e);
        PeerBean[] pbeans = f.getPeerBeans();
        man.fireOnFoundPeers(new PeerEvent(this, pbeans));
        if (pbeans == null) return;
        for (PeerBean pbean : pbeans) {
            Speaker speaker = new Speaker();
            pbean.setData(speaker);
            pbean.fireOnTalkingChanged(new com.peterhi.beans.PeerEvent(this));
            if (pbean.getTalkState() == TalkState.On) speaker.start();
            int id = getStore().getID();
            int tid = pbean.getID();
            DatagramSession ses = new DatagramSession(man, false);
            ses.setTsa(server.getRemoteSocketAddress());
            ses.start();
            man.putPeerSession(tid, ses);
            ISesMsg m = new ISesMsg();
            m.setClientHashCode(id);
            m.setTargetClientHashCode(tid);
            man.udpPost(tid, m);
        }
    }

    private StoreManager getStore() {
        return App.getApp().getManager(StoreManager.class);
    }
}
