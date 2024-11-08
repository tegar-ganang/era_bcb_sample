package com.peterhi.server.handler;

import java.io.IOException;
import org.xsocket.stream.INonBlockingConnection;
import com.peterhi.persist.Persister;
import com.peterhi.net.message.EraseMessage;
import com.peterhi.server.SocketHandler;
import com.peterhi.server.Main;
import com.peterhi.server.ClientHandle;
import com.peterhi.server.SocketServer;
import com.peterhi.PeterHi;

public class EraseMessageHandler implements SocketHandler<EraseMessage> {

    public void handle(INonBlockingConnection conn, EraseMessage message) throws IOException {
        Main.broadcast(message, PeterHi.SOCKET, false);
        SocketServer ss = SocketServer.getInstance();
        ClientHandle cs = ss.get(conn);
        RemoveAllShapes rs = new RemoveAllShapes(cs.getChannel());
        Persister.getInstance().execute(rs);
    }
}
