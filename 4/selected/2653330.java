package com.peterhi.server.handler;

import java.io.IOException;
import org.xsocket.stream.INonBlockingConnection;
import com.peterhi.persist.Persister;
import com.peterhi.net.message.LineMessage;
import com.peterhi.server.SocketHandler;
import com.peterhi.server.Main;
import com.peterhi.server.ClientHandle;
import com.peterhi.server.SocketServer;
import com.peterhi.PeterHi;

public class LineMessageHandler implements SocketHandler<LineMessage> {

    public void handle(INonBlockingConnection conn, LineMessage message) throws IOException {
        Main.broadcast(message, PeterHi.SOCKET, false);
        SocketServer ss = SocketServer.getInstance();
        ClientHandle cs = ss.get(conn);
        AddShape as = new AddShape(cs.getChannel(), Main.toShape(message));
        Persister.getInstance().execute(as);
    }
}
