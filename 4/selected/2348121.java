package com.peterhi.server.handlers;

import java.io.IOException;
import org.xsocket.stream.INonBlockingConnection;
import com.peterhi.persist.Persister;
import com.peterhi.net.messages.EraseMessage;
import com.peterhi.server.SocketHandler;
import com.peterhi.server.Main;
import com.peterhi.server.ClientHandle;
import com.peterhi.server.SocketServer;

public class EraseMessageHandler implements SocketHandler<EraseMessage> {

    public void handle(INonBlockingConnection conn, EraseMessage message) throws IOException {
        Main.broadcast(message, false);
        SocketServer ss = SocketServer.getInstance();
        ClientHandle cs = ss.get(conn);
        RemoveAllShapes rs = new RemoveAllShapes(cs.getChannel());
        Persister.getInstance().execute(rs);
    }
}
