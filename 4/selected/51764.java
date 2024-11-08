package com.peterhi.server;

import com.peterhi.StatusCode;
import com.peterhi.data.Account;
import com.peterhi.data.DB;
import com.peterhi.net.Handler;
import com.peterhi.net.Protocol;
import com.peterhi.net.messages.CommandMessage;
import com.peterhi.net.messages.CommandResponse;
import com.peterhi.net.messages.EnterChannelMessage;
import com.peterhi.net.messages.EnterChannelResponse;
import com.peterhi.net.messages.LoginMessage;
import com.peterhi.net.messages.LoginResponse;
import com.peterhi.net.messages.UdpMessage;
import com.peterhi.net.messages.UdpResponse;
import com.peterhi.persist.Persister;
import com.peterhi.persist.beans.Classroom;
import com.peterhi.persist.beans.Member;
import com.peterhi.persist.tx.GetClassroomTx;
import com.peterhi.persist.tx.GetMemberTx;
import com.peterhi.server.commands.ClassroomCommand;
import com.peterhi.server.commands.CommandEngine;
import com.peterhi.server.commands.CommandResult;
import com.peterhi.server.commands.KickCommand;
import com.peterhi.server.commands.ServerHealthCommand;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.Arrays;
import org.hibernate.Session;
import org.xsocket.stream.INonBlockingConnection;

/**
 *
 * @author YUN TAO
 */
public class Main {

    public static void main(String[] args) throws Exception {
        CommandEngine.getInstance().put("server-health", new ServerHealthCommand());
        CommandEngine.getInstance().put("kick", new KickCommand());
        CommandEngine.getInstance().put("class", new ClassroomCommand());
        Server server = new Server();
        server.addTcpHandler(Protocol.LOGIN_MESSAGE, new LoginMessageHandler());
        server.addTcpHandler(Protocol.LOGOUT_MESSAGE, new LogoutMessageHandler());
        server.addTcpHandler(Protocol.COMMAND_MESSAGE, new CommandMessageHandler());
        server.addTcpHandler(Protocol.ENTER_CHANNEL_MESSAGE, new EnterChannelMessageHandler());
        server.addUdpHandler(Protocol.UDP_MESSAGE, new UdpMessageHandler());
        server.start(9080);
    }

    public static class LoginMessageHandler implements Handler<Server, INonBlockingConnection> {

        public void handle(Server server, INonBlockingConnection client, byte[] data) throws IOException {
            LoginMessage message = new LoginMessage(data, Protocol.TCP_BODY_BEGIN);
            LoginResponse response;
            Session s = null;
            try {
                s = DB.begin();
                Account acc = (Account) DB.queryOne(s, Account.class, Account.F_ACC_EMAIL, message.getUserName());
                if (acc != null) {
                    if (Arrays.equals(acc.getAccPass(), message.getPassword())) {
                        if (acc.isAccActivated()) {
                            if (acc.isAccEnabled()) {
                                if (!acc.isAccOnline()) {
                                    acc.setAccOnline(true);
                                    s.update(acc);
                                    DB.commit(s);
                                    ClientSession session = server.getClient(client);
                                    session.setEmail(acc.getAccEmail());
                                    response = new LoginResponse(StatusCode.OK, session.getClientId());
                                } else {
                                    DB.commit(s);
                                    response = new LoginResponse(StatusCode.LOGIN_DUPLICATE, -1);
                                }
                            } else {
                                DB.commit(s);
                                response = new LoginResponse(StatusCode.LOGIN_ACC_DISABLED, -1);
                            }
                        } else {
                            DB.commit(s);
                            response = new LoginResponse(StatusCode.LOGIN_ACC_INACTIVE, -1);
                        }
                    } else {
                        DB.commit(s);
                        response = new LoginResponse(StatusCode.LOGIN_WRONG_PASS, -1);
                    }
                } else {
                    DB.commit(s);
                    response = new LoginResponse(StatusCode.LOGIN_NO_SUCH_ACCOUNT, -1);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                if (s != null) {
                    try {
                        DB.rollback(s);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                response = new LoginResponse(StatusCode.Fail, -1);
            }
            server.send(client, response);
            if (response.getStatusCode() != StatusCode.OK) {
                client.close();
            }
        }
    }

    static class CommandMessageHandler implements Handler<Server, INonBlockingConnection> {

        public void handle(Server server, INonBlockingConnection client, byte[] data) throws IOException {
            CommandMessage message = new CommandMessage(data, Protocol.TCP_BODY_BEGIN);
            CommandResult result = CommandEngine.getInstance().execute(server, message.getCommand());
            CommandResponse response = new CommandResponse(message.getCommandId(), result.getResponse(), result.getStatusCode());
            server.send(client, response);
        }
    }

    static class UdpMessageHandler implements Handler<Server, SocketAddress> {

        public void handle(Server server, SocketAddress client, byte[] data) throws IOException {
            UdpMessage message = new UdpMessage(data, Protocol.RUDP_BODY_BEGIN);
            ClientSession session = server.getClient(message.getClientId());
            if (session != null) {
                session.setUdpAddress(client);
                UdpResponse response = new UdpResponse(StatusCode.OK);
                server.post(session, response);
            }
        }
    }

    static class LogoutMessageHandler implements Handler<Server, INonBlockingConnection> {

        public void handle(Server server, INonBlockingConnection client, byte[] data) throws IOException {
            client.close();
        }
    }

    static class EnterChannelMessageHandler implements Handler<Server, INonBlockingConnection> {

        public void handle(Server server, INonBlockingConnection client, byte[] data) throws IOException {
            ClientSession cs = server.getClient(client);
            System.out.println("cs email: " + cs.getEmail());
            EnterChannelMessage message = new EnterChannelMessage(data, Protocol.TCP_BODY_BEGIN);
            EnterChannelResponse response;
            GetClassroomTx tx = new GetClassroomTx(message.getChannelName());
            try {
                Classroom classroom = (Classroom) Persister.getInstance().execute(tx);
                if (classroom != null) {
                    GetMemberTx tx2 = new GetMemberTx(message.getChannelName(), cs.getEmail());
                    Member member = (Member) Persister.getInstance().execute(tx2);
                    if (member != null) {
                        member.setOnline(true);
                        response = new EnterChannelResponse(member.hashCode(), StatusCode.OK);
                    } else {
                        response = new EnterChannelResponse(-1, StatusCode.EntryDenied);
                    }
                } else {
                    response = new EnterChannelResponse(-1, StatusCode.NotFound);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                response = new EnterChannelResponse(-1, StatusCode.Fail);
            }
            server.send(client, response);
        }
    }
}
