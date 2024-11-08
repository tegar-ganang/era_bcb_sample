package com.peterhi.client.nio.handlers;

import com.peterhi.beans.PeerBean;
import com.peterhi.client.App;
import com.peterhi.client.managers.PeerManager;
import com.peterhi.client.managers.StoreManager;
import com.peterhi.client.nio.DatagramHandler;
import com.peterhi.client.nio.DatagramSession;
import com.peterhi.client.nio.NetworkManager;
import com.peterhi.client.voice.Speaker;
import com.peterhi.net.Protocol;
import com.peterhi.net.msg.Voice;
import com.peterhi.net.packet.Packet;

public class VoiceHandler implements DatagramHandler {

    public void handle(NetworkManager man, DatagramSession session, Packet packet) throws Exception {
        if (getStore().getChannel() == null) return;
        Voice v = new Voice();
        v.deserialize(packet.getBuf(), Protocol.UDP_CURSOR);
        PeerBean bean = getPeers().get(v.id);
        if (bean == null) return;
        Speaker speaker = (Speaker) bean.getData();
        if (speaker == null) return;
        byte[] data = new byte[320];
        System.arraycopy(v.data, 0, data, 0, v.data.length);
        speaker.write(data, 62);
    }

    private StoreManager getStore() {
        return App.getApp().getManager(StoreManager.class);
    }

    private PeerManager getPeers() {
        return App.getApp().getManager(PeerManager.class);
    }
}
