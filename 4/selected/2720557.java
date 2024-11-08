package com.peterhi.server.handlers;

import java.io.IOException;
import org.xsocket.stream.INonBlockingConnection;
import com.peterhi.persist.Persister;
import com.peterhi.net.messages.PolylineMessage;
import com.peterhi.server.SocketHandler;
import com.peterhi.server.Main;
import com.peterhi.server.ClientHandle;
import com.peterhi.server.SocketServer;

public class PolylineMessageHandler implements SocketHandler<PolylineMessage> {

    public void handle(INonBlockingConnection conn, PolylineMessage message) throws IOException {
        Main.broadcast(message, false);
        SocketServer ss = SocketServer.getInstance();
        ClientHandle cs = ss.get(conn);
        AddShape as = new AddShape(cs.getChannel(), Main.toShape(message));
        Persister.getInstance().execute(as);
    }
}
