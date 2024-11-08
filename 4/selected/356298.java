package com.peterhi.server.handler;

import java.io.IOException;
import org.xsocket.stream.INonBlockingConnection;
import com.peterhi.persist.Persister;
import com.peterhi.net.message.ChangeStateMessage;
import com.peterhi.server.SocketHandler;
import com.peterhi.server.Main;
import com.peterhi.server.ClientHandle;
import com.peterhi.server.SocketServer;
import com.peterhi.PeterHi;

public class ChangeStateMessageHandler implements SocketHandler<ChangeStateMessage> {

    static int counter = 0;

    public void handle(INonBlockingConnection conn, ChangeStateMessage message) throws IOException {
        Main.broadcast(message, PeterHi.SOCKET, message.toSender);
        SocketServer ss = SocketServer.getInstance();
        ClientHandle cs = ss.get(conn);
        ChangeState op = new ChangeState(cs.getChannel(), cs.getEmail(), message.state, message.changedBits);
        Persister.getInstance().execute(op);
    }
}
