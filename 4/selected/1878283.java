package com.peterhi.server.handler;

import java.io.IOException;
import org.xsocket.stream.INonBlockingConnection;
import com.peterhi.persist.Persister;
import com.peterhi.net.message.RemoveElementMessage;
import com.peterhi.server.SocketHandler;
import com.peterhi.server.Main;
import com.peterhi.server.ClientHandle;
import com.peterhi.server.SocketServer;
import com.peterhi.PeterHi;

public class RemoveElementMessageHandler implements SocketHandler<RemoveElementMessage> {

    public void handle(INonBlockingConnection conn, RemoveElementMessage message) throws IOException {
        Main.broadcast(message, PeterHi.SOCKET, false);
        SocketServer ss = SocketServer.getInstance();
        ClientHandle cs = ss.get(conn);
        RemoveShape rs = new RemoveShape(cs.getChannel(), message.name);
        Persister.getInstance().execute(rs);
    }
}
