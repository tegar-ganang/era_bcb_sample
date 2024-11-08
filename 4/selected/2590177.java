package com.peterhi.server;

import com.peterhi.data.Account;
import com.peterhi.data.DB;
import com.peterhi.net.messages.KillPeerMessage;
import com.peterhi.net.messages.Message;
import com.peterhi.persist.Persister;
import com.peterhi.persist.beans.Classroom;
import com.peterhi.persist.beans.Member;
import com.peterhi.persist.beans.Root;
import com.peterhi.State;
import com.peterhi.Log;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.hibernate.Session;
import org.prevayler.Transaction;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.stream.IConnectHandler;
import org.xsocket.stream.IDataHandler;
import org.xsocket.stream.IDisconnectHandler;
import org.xsocket.stream.IMultithreadedServer;
import org.xsocket.stream.INonBlockingConnection;
import org.xsocket.stream.MultithreadedServer;

/**
 *
 * @author YUN TAO
 */
public class SocketServer {

    private static final SocketServer instance = new SocketServer();

    public static SocketServer getInstance() {
        return instance;
    }

    private IMultithreadedServer socketServer;

    private IMultithreadedServer adminServer;

    private Map<Byte, SocketHandler> handlers = new Hashtable<Byte, SocketHandler>();

    private Set<ClientHandle> clients = new HashSet<ClientHandle>();

    private Set<AdminSession> admins = new HashSet<AdminSession>();

    private Set<SocketServerListener> listeners = new HashSet<SocketServerListener>();

    protected SocketServer() {
    }

    public void addHandler(byte type, SocketHandler handler) {
        handlers.put(type, handler);
    }

    public synchronized void start(int port) throws IOException {
        socketServer = new MultithreadedServer(port, new TcpServerHandler());
        new Thread(socketServer, "socket-server").start();
        adminServer = new MultithreadedServer(port + 100, new AdminServerHandler());
        new Thread(adminServer, "admin-server").start();
        Log.info("socket server started @ " + socketServer.getLocalAddress() + ":" + socketServer.getLocalPort());
        Log.info("admin server started @ " + adminServer.getLocalAddress() + ":" + adminServer.getLocalPort());
    }

    public synchronized void close() throws IOException {
        clients.clear();
        admins.clear();
        socketServer.close();
        adminServer.close();
        Log.info("socket and admin server closed");
    }

    public synchronized void addServerListener(SocketServerListener l) {
        if (l == null) {
            throw new NullPointerException();
        }
        listeners.add(l);
    }

    public synchronized void removeServerListener(SocketServerListener l) {
        if (l == null) {
            throw new NullPointerException();
        }
        listeners.remove(l);
    }

    public synchronized boolean add(ClientHandle ses) {
        for (ClientHandle cur : clients) {
            if (cur.getId() == ses.getId()) {
                return false;
            }
            if (cur.connection() != null && cur.connection().equals(ses.connection())) {
                return false;
            }
        }
        return clients.add(ses);
    }

    public synchronized boolean remove(ClientHandle ses) {
        return clients.remove(ses);
    }

    public synchronized ClientHandle remove(INonBlockingConnection conn) {
        for (Iterator<ClientHandle> itor = clients.iterator(); itor.hasNext(); ) {
            ClientHandle cur = itor.next();
            if (cur.connection() != null && cur.connection().equals(conn)) {
                itor.remove();
                return cur;
            }
        }
        return null;
    }

    public synchronized ClientHandle remove(SocketAddress addr) {
        InetSocketAddress inetAddr = (InetSocketAddress) addr;
        for (Iterator<ClientHandle> itor = clients.iterator(); itor.hasNext(); ) {
            ClientHandle cur = itor.next();
            if (cur.connection() != null) {
                if (cur.connection().getRemoteAddress().equals(inetAddr.getAddress()) && cur.connection().getRemotePort() == inetAddr.getPort()) {
                    itor.remove();
                    return cur;
                }
            }
        }
        return null;
    }

    public synchronized ClientHandle remove(int id) {
        for (Iterator<ClientHandle> itor = clients.iterator(); itor.hasNext(); ) {
            ClientHandle cur = itor.next();
            if (cur.getId() == id) {
                itor.remove();
                return cur;
            }
        }
        return null;
    }

    public synchronized ClientHandle get(INonBlockingConnection conn) {
        for (ClientHandle item : clients) {
            if (item.connection().equals(conn)) {
                return item;
            }
        }
        return null;
    }

    public synchronized ClientHandle get(SocketAddress addr) {
        InetSocketAddress inetAddr = (InetSocketAddress) addr;
        for (ClientHandle item : clients) {
            if (item.connection() != null) {
                if (item.connection().getRemoteAddress().equals(inetAddr.getAddress()) && item.connection().getRemotePort() == inetAddr.getPort()) {
                    return item;
                }
            }
        }
        return null;
    }

    public synchronized ClientHandle get(int id) {
        for (ClientHandle item : clients) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    public synchronized ClientHandle get(String email) {
        for (ClientHandle item : clients) {
            if (item.getEmail().equals(email)) {
                return item;
            }
        }
        return null;
    }

    public synchronized boolean addAdmin(AdminSession ses) {
        for (AdminSession cur : admins) {
            if (cur.connection() != null && cur.connection().equals(ses.connection())) {
                return false;
            }
        }
        return admins.add(ses);
    }

    public synchronized boolean removeAdmin(AdminSession ses) {
        return admins.remove(ses);
    }

    public synchronized AdminSession removeAdmin(INonBlockingConnection conn) {
        for (Iterator<AdminSession> itor = admins.iterator(); itor.hasNext(); ) {
            AdminSession cur = itor.next();
            if (cur.connection() != null && cur.connection().equals(conn)) {
                itor.remove();
                return cur;
            }
        }
        return null;
    }

    public synchronized AdminSession removeAdmin(SocketAddress addr) {
        InetSocketAddress inetAddr = (InetSocketAddress) addr;
        for (Iterator<AdminSession> itor = admins.iterator(); itor.hasNext(); ) {
            AdminSession cur = itor.next();
            if (cur.connection() != null) {
                if (cur.connection().getRemoteAddress().equals(inetAddr.getAddress()) && cur.connection().getRemotePort() == inetAddr.getPort()) {
                    itor.remove();
                    return cur;
                }
            }
        }
        return null;
    }

    public synchronized AdminSession getAdmin(INonBlockingConnection conn) {
        for (Iterator<AdminSession> itor = admins.iterator(); itor.hasNext(); ) {
            AdminSession cur = itor.next();
            if (cur.connection() != null && cur.connection().equals(conn)) {
                return cur;
            }
        }
        return null;
    }

    public synchronized AdminSession getAdmin(SocketAddress addr) {
        InetSocketAddress inetAddr = (InetSocketAddress) addr;
        for (Iterator<AdminSession> itor = admins.iterator(); itor.hasNext(); ) {
            AdminSession cur = itor.next();
            if (cur.connection() != null) {
                if (cur.connection().getRemoteAddress().equals(inetAddr.getAddress()) && cur.connection().getRemotePort() == inetAddr.getPort()) {
                    return cur;
                }
            }
        }
        return null;
    }

    public synchronized void send(INonBlockingConnection conn, Message message) throws IOException {
        byte[] data = message.serialize();
        conn.write(data);
    }

    class TcpServerHandler implements IDataHandler, IConnectHandler, IDisconnectHandler {

        public boolean onData(INonBlockingConnection conn) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            try {
                int length = conn.getNumberOfAvailableBytes();
                byte[] data = conn.readBytesByLength(length);
                SocketHandler handler = handlers.get(data[0]);
                if (handler != null) {
                    Message msg = Message.newInstance(data[0]);
                    if (msg != null) {
                        msg.deserialize(data);
                        synchronized (handler) {
                            handler.handle(conn, msg);
                        }
                    } else {
                        Log.info("can't create message for: " + data[0]);
                    }
                } else {
                    Log.info("can't find handler for: " + data[0]);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return true;
        }

        public boolean onConnect(INonBlockingConnection conn) throws IOException {
            Log.info("client connect @ " + conn.getRemoteAddress() + ":" + conn.getRemotePort());
            conn.setAttachment(new InetSocketAddress(conn.getRemoteAddress(), conn.getRemotePort()));
            ClientHandle client = new ClientHandle(conn);
            add(client);
            fireOnConnected(conn);
            return true;
        }

        public boolean onDisconnect(INonBlockingConnection conn) throws IOException {
            try {
                Log.info("client dc @ " + conn.getAttachment());
                final ClientHandle cs = remove(conn);
                String identity;
                if (cs.getEmail() == null || cs.getEmail().length() <= 0) {
                    identity = "" + conn.getAttachment();
                } else {
                    identity = cs.getEmail() + ", " + conn.getAttachment();
                }
                if (cs != null) {
                    Log.info("removed client " + identity);
                    KillPeerMessage killPeer = new KillPeerMessage();
                    killPeer.id = cs.getId();
                    for (Integer id : cs.getIds()) {
                        ClientHandle ses = get(id);
                        ses.getIds().remove(Integer.valueOf(cs.getId()));
                        send(ses.connection(), killPeer);
                    }
                    cs.getIds().clear();
                    Log.info("kill peer msg sent to the peers of " + identity);
                    if (cs.getEmail() != null && cs.getEmail().length() > 0) {
                        Session s = null;
                        try {
                            s = DB.begin();
                            Account acc = (Account) DB.queryOne(s, Account.class, Account.F_ACC_EMAIL, cs.getEmail());
                            if (acc != null) {
                                acc.setAccOnline(false);
                                s.update(acc);
                            }
                            DB.commit(s);
                            Log.info("mark " + identity + " as offline on database");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            if (s != null) {
                                try {
                                    DB.rollback(s);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        Persister.getInstance().execute(new Offline(cs.getChannel(), cs.getEmail()));
                        Log.info("mark " + identity + " as offline on persistent store");
                    }
                }
                fireOnDisconnected(conn);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return true;
        }
    }

    class AdminServerHandler implements IDataHandler, IConnectHandler, IDisconnectHandler {

        public boolean onData(INonBlockingConnection conn) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            int length = conn.getNumberOfAvailableBytes();
            byte[] data = conn.readBytesByLength(length);
            SocketHandler handler = handlers.get(data[0]);
            if (handler != null) {
                Message msg = Message.newInstance(data[0]);
                if (msg != null) {
                    msg.deserialize(data);
                    synchronized (handler) {
                        handler.handle(conn, msg);
                    }
                } else {
                    Logger.getLogger(getClass().getName()).warning("can't create message for: " + data[0]);
                }
            } else {
                Logger.getLogger(getClass().getName()).warning("can't find handler for: " + data[0]);
            }
            return true;
        }

        public boolean onConnect(INonBlockingConnection conn) throws IOException {
            Log.info("admin connect @ " + conn.getRemoteAddress() + ":" + conn.getRemotePort());
            conn.setAttachment(new InetSocketAddress(conn.getRemoteAddress(), conn.getRemotePort()));
            AdminSession admin = new AdminSession(conn);
            addAdmin(admin);
            fireOnAdminConnected(conn);
            return true;
        }

        public boolean onDisconnect(INonBlockingConnection conn) throws IOException {
            Log.info("admin dc @ " + conn.getAttachment());
            removeAdmin(conn);
            fireOnAdminDisconnected(conn);
            return true;
        }
    }

    private void fireOnConnected(INonBlockingConnection conn) {
        for (SocketServerListener listener : listeners) {
            listener.connected(conn);
        }
    }

    private void fireOnDisconnected(INonBlockingConnection conn) {
        for (SocketServerListener listener : listeners) {
            listener.disconnected(conn);
        }
    }

    private void fireOnAdminConnected(INonBlockingConnection conn) {
        for (SocketServerListener listener : listeners) {
            listener.adminConnected(conn);
        }
    }

    private void fireOnAdminDisconnected(INonBlockingConnection conn) {
        for (SocketServerListener listener : listeners) {
            listener.adminDisconnected(conn);
        }
    }
}
