package com.vayoodoot.research;

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.TransportType;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Sachin Shetty
 * Date: Aug 8, 2007
 * Time: 4:49:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class MinaServerHandler extends IoHandlerAdapter {

    public void exceptionCaught(IoSession session, Throwable t) throws Exception {
        t.printStackTrace();
        session.close();
    }

    public void messageReceived(IoSession session, Object msg) throws Exception {
        String str = msg.toString();
        if (str.trim().equalsIgnoreCase("quit")) {
            session.close();
            return;
        }
        if (session.getAttribute("FIRST_MESSAGE") == null) {
            session.setAttribute("FIRST_MESSAGE", str);
        }
        Date date = new Date();
        session.write(date.toString() + ":" + Thread.currentThread().getId() + str + ":" + session.getAttribute("FIRST_MESSAGE"));
        System.out.println("Message written...");
    }

    public void sessionCreated(IoSession session) throws Exception {
        System.out.println("Session created...");
        if (session.getTransportType() == TransportType.SOCKET) ((SocketSessionConfig) session.getConfig()).setReceiveBufferSize(2048);
        session.setIdleTime(IdleStatus.BOTH_IDLE, 10);
    }
}
