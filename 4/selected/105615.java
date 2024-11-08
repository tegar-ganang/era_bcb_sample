package com.peterhi.server.handler;

import java.io.IOException;
import org.xsocket.stream.INonBlockingConnection;
import com.peterhi.PeterHi;
import com.peterhi.server.SocketHandler;
import com.peterhi.server.SocketServer;
import com.peterhi.server.ClientHandle;
import com.peterhi.net.message.LeaveChannelMessage;
import com.peterhi.net.message.LeaveChannelResponse;
import com.peterhi.net.message.KillPeerMessage;
import com.peterhi.persist.Persister;
import com.peterhi.State;

public class LeaveChannelMessageHandler implements SocketHandler<LeaveChannelMessage> {

    public void handle(INonBlockingConnection client, LeaveChannelMessage message) throws IOException {
        SocketServer ss = SocketServer.getInstance();
        final ClientHandle cs = ss.get(message.id);
        if (cs != null) {
            KillPeerMessage killPeer = new KillPeerMessage();
            killPeer.id = cs.getId();
            for (Integer id : cs.getIds()) {
                ClientHandle ses = ss.get(id);
                ses.getIds().remove(Integer.valueOf(cs.getId()));
                ss.send(ses.connection(), killPeer);
            }
            cs.getIds().clear();
            Persister.getInstance().execute(new MarkAsOffline(cs.getChannel(), cs.getEmail()));
            cs.setChannelId(0);
            cs.setChannel(null);
            State state = cs.getState();
            state.setBaseStatus(PeterHi.BASE_STATUS_OFFLINE);
        }
        LeaveChannelResponse response = new LeaveChannelResponse();
        response.code = PeterHi.OK;
        ss.send(cs.connection(), response);
    }
}
