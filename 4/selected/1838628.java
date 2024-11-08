package com.peterhi.server.handlers;

import java.io.IOException;
import org.xsocket.stream.INonBlockingConnection;
import com.peterhi.persist.Persister;
import com.peterhi.net.messages.ChangeStateMessage;
import com.peterhi.server.SocketHandler;
import com.peterhi.server.Main;
import com.peterhi.server.ClientHandle;
import com.peterhi.server.SocketServer;

public class ChangeStateMessageHandler implements SocketHandler<ChangeStateMessage> {

    public void handle(INonBlockingConnection conn, ChangeStateMessage message) throws IOException {
        Main.broadcast(message, false);
        SocketServer ss = SocketServer.getInstance();
        ClientHandle cs = ss.get(conn);
        ChangeState op = new ChangeState(cs.getChannel(), cs.getEmail(), message.state);
        Persister.getInstance().execute(op);
    }
}
