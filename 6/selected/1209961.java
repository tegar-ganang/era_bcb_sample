package org.yccheok.jstock.chat;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.yccheok.jstock.engine.Subject;
import org.yccheok.jstock.gui.MainFrame;

/**
 *
 * @author yccheok
 */
public class ChatServiceManager {

    public enum State {

        CONNECTING, CONNECTED, ACCOUNT_CREATING, ACCOUNT_CREATED, ROOM_CREATING, ROOM_CREATED, END
    }

    private class ChatService implements Runnable {

        public ChatService(String username, String password) {
            this.username = username;
            this.password = password;
            this.connection = null;
            readWriteLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
            readerLock = readWriteLock.readLock();
            writerLock = readWriteLock.writeLock();
        }

        public void start() {
            this.runnableFlag = true;
            new Thread(this).start();
        }

        private void notifyPacketObserver(Packet packet) {
            if (this.runnableFlag) ChatServiceManager.this.notifyPacketObserver(packet);
        }

        private void notifyStateObserver(State state) {
            if (this.runnableFlag) ChatServiceManager.this.notifyStateObserver(state);
        }

        @Override
        public void run() {
            this.me = Thread.currentThread();
            ChatService.this.notifyStateObserver(State.CONNECTING);
            this.connection = new XMPPConnection(Utils.getXMPPServer());
            while (runnableFlag) {
                try {
                    State state = State.CONNECTING;
                    ChatService.this.notifyStateObserver(state);
                    boolean shouldContinue = true;
                    while (runnableFlag && shouldContinue) {
                        switch(state) {
                            case CONNECTING:
                                state = this.connecting();
                                break;
                            case CONNECTED:
                                state = this.connected();
                                break;
                            case ACCOUNT_CREATING:
                                state = this.account_creating();
                                break;
                            case ACCOUNT_CREATED:
                                state = this.account_created();
                                break;
                            case ROOM_CREATING:
                                state = this.room_creating();
                                break;
                            case ROOM_CREATED:
                                state = this.room_created();
                                break;
                            case END:
                                state = this.end();
                                shouldContinue = false;
                                break;
                            default:
                                throw new java.lang.IllegalArgumentException("Missing case " + state);
                        }
                    }
                } catch (Exception exp) {
                    log.error("Some stupid thing happens here.", exp);
                } finally {
                    if (muc != null) {
                        try {
                            this.writerLock.lock();
                            muc.leave();
                            muc.removeMessageListener(messageListener);
                            muc.removeParticipantListener(participantListener);
                            muc = null;
                        } catch (Exception exp) {
                            log.error("Some stupid thing happens here.", exp);
                        } finally {
                            this.writerLock.unlock();
                        }
                    }
                }
            }
            connection.disconnect();
            connection.removeConnectionListener(connectionListener);
        }

        private State connected() {
            State state = State.CONNECTED;
            ChatService.this.notifyStateObserver(state);
            state = State.ROOM_CREATING;
            return state;
        }

        private ConnectionListener getConnectionListener() {
            return new ConnectionListener() {

                @Override
                public void connectionClosed() {
                    if (doneSignal != null) doneSignal.countDown();
                }

                @Override
                public void connectionClosedOnError(Exception arg0) {
                    if (doneSignal != null) doneSignal.countDown();
                }

                @Override
                public void reconnectingIn(int arg0) {
                    if (doneSignal != null) doneSignal.countDown();
                }

                @Override
                public void reconnectionSuccessful() {
                    if (doneSignal != null) doneSignal.countDown();
                }

                @Override
                public void reconnectionFailed(Exception arg0) {
                    if (doneSignal != null) doneSignal.countDown();
                }
            };
        }

        private PacketListener getPacketListener() {
            return new PacketListener() {

                @Override
                public void processPacket(Packet arg0) {
                    ChatService.this.notifyPacketObserver(arg0);
                }
            };
        }

        private State room_creating() {
            State state = State.ROOM_CREATING;
            ChatService.this.notifyStateObserver(state);
            Collection<String> serviceNames = null;
            try {
                serviceNames = MultiUserChat.getServiceNames(connection);
            } catch (XMPPException ex) {
                log.error(null, ex);
            }
            String serviceName = "conference." + Utils.getXMPPServer();
            if (serviceNames != null) {
                serviceName = serviceNames.toArray(new String[0])[0];
            }
            final String roomName = Utils.getRoomName(MainFrame.getInstance().getJStockOptions().getCountry()) + "@" + serviceName;
            if (muc != null) {
                muc.removeMessageListener(messageListener);
                muc.removeParticipantListener(participantListener);
            }
            muc = new MultiUserChat(connection, roomName);
            muc.addMessageListener(messageListener);
            muc.addParticipantListener(participantListener);
            if (muc.isJoined() == false) {
                try {
                    muc.join(username);
                } catch (XMPPException ex) {
                    log.error(null, ex);
                    XMPPError error = ex.getXMPPError();
                    if (error != null) {
                        if (error.getCode() == 404) {
                            try {
                                muc.sendConfigurationForm(null);
                                muc.create(username);
                                state = State.ROOM_CREATED;
                                return state;
                            } catch (XMPPException ex1) {
                                log.error(null, ex1);
                            }
                        }
                    }
                    state = State.CONNECTING;
                    return state;
                }
            }
            state = State.ROOM_CREATED;
            return state;
        }

        private State end() {
            State state = State.END;
            ChatService.this.notifyStateObserver(state);
            return state;
        }

        private State room_created() {
            State state = State.ROOM_CREATED;
            ChatService.this.notifyStateObserver(state);
            doneSignal = new CountDownLatch(1);
            try {
                doneSignal.await();
            } catch (InterruptedException ex) {
                state = State.END;
            }
            state = State.END;
            return state;
        }

        private State account_created() {
            State state = State.ACCOUNT_CREATED;
            ChatService.this.notifyStateObserver(state);
            state = State.CONNECTING;
            return state;
        }

        private State account_creating() {
            State state = State.ACCOUNT_CREATING;
            ChatService.this.notifyStateObserver(state);
            AccountManager accountManager = connection.getAccountManager();
            if (accountManager == null) {
                state = State.CONNECTING;
                return state;
            }
            try {
                if (use_login_retry) {
                    accountManager.createAccount(username + this.login_retry, password);
                } else {
                    accountManager.createAccount(username, password);
                }
                state = State.ACCOUNT_CREATED;
            } catch (XMPPException ex) {
                log.error(null, ex);
                final XMPPError error = ex.getXMPPError();
                if (error != null) {
                    if (error.getCode() == 409) {
                        use_login_retry = true;
                        login_retry++;
                    }
                    state = State.CONNECTING;
                } else {
                    state = State.CONNECTING;
                }
            }
            return state;
        }

        private void sendMessage(String msg) {
            try {
                readerLock.lock();
                if (muc != null) {
                    muc.sendMessage(msg);
                }
            } catch (XMPPException ex) {
                log.error(null, ex);
            } finally {
                readerLock.unlock();
            }
        }

        private State connecting() {
            State state = State.CONNECTING;
            ChatService.this.notifyStateObserver(state);
            if (connection.isAuthenticated() && connection.isConnected()) {
                state = State.CONNECTED;
                return state;
            }
            try {
                connection.connect();
                SASLAuthentication.supportSASLMechanism("PLAIN", 0);
                if (use_login_retry) {
                    connection.login(username + this.login_retry, password);
                } else {
                    connection.login(username, password);
                }
                if (connection.isAuthenticated() && connection.isConnected()) {
                    if (use_login_retry) {
                        use_login_retry = !use_login_retry;
                        username = username + this.login_retry;
                        this.login_retry = -1;
                        MainFrame.getInstance().getJStockOptions().setChatUsername(username);
                    }
                    this.connection.removeConnectionListener(connectionListener);
                    this.connection.addConnectionListener(connectionListener);
                    state = State.CONNECTED;
                }
            } catch (XMPPException ex) {
                log.error(null, ex);
                final XMPPError error = ex.getXMPPError();
                if (error != null) {
                    if (error.getCode() == 504) {
                        state = State.CONNECTING;
                    }
                } else {
                    state = State.ACCOUNT_CREATING;
                }
            }
            return state;
        }

        public void stop() {
            runnableFlag = false;
            if (me != null) me.interrupt();
            if (doneSignal != null) doneSignal.countDown();
        }

        public boolean changePassword(String newPassword) {
            final XMPPConnection c = this.connection;
            if (c == null) {
                return false;
            }
            try {
                if ((c.isAuthenticated() == false) || (c.isConnected() == false)) {
                    return false;
                }
                final AccountManager accountManager = c.getAccountManager();
                if (accountManager == null) {
                    return false;
                }
                accountManager.changePassword(newPassword);
            } catch (Exception ex) {
                log.error(null, ex);
                return false;
            }
            return true;
        }

        public boolean isLogin() {
            final XMPPConnection c = this.connection;
            if (c == null) {
                return false;
            }
            return c.isAuthenticated() && c.isConnected();
        }

        private final ConnectionListener connectionListener = this.getConnectionListener();

        private final PacketListener messageListener = this.getPacketListener();

        private final PacketListener participantListener = this.getPacketListener();

        private MultiUserChat muc = null;

        private volatile boolean runnableFlag = true;

        private String username = null;

        private final String password;

        private XMPPConnection connection = null;

        private boolean use_login_retry = false;

        private int login_retry = -1;

        private volatile CountDownLatch doneSignal = null;

        private Thread me = null;

        private final java.util.concurrent.locks.ReadWriteLock readWriteLock;

        private final java.util.concurrent.locks.Lock readerLock;

        private final java.util.concurrent.locks.Lock writerLock;
    }

    public synchronized void start() {
        String username = MainFrame.getInstance().getJStockOptions().getChatUsername();
        String password = org.yccheok.jstock.gui.Utils.decrypt(MainFrame.getInstance().getJStockOptions().getChatPassword());
        stop();
        chatService = new ChatService(username, password);
        chatService.start();
    }

    public synchronized void stop() {
        if (chatService == null) {
            return;
        }
        chatService.stop();
    }

    public void sendMessage(String msg) {
        this.chatService.sendMessage(msg);
    }

    private static class SubjectEx<S, A> extends Subject<S, A> {

        @Override
        protected void notify(S subject, A arg) {
            super.notify(subject, arg);
        }
    }

    private SubjectEx<ChatServiceManager, Packet> getPacketSubject() {
        return new SubjectEx<ChatServiceManager, Packet>();
    }

    private SubjectEx<ChatServiceManager, ChatServiceManager.State> getStateSubject() {
        return new SubjectEx<ChatServiceManager, ChatServiceManager.State>();
    }

    private void notifyPacketObserver(Packet packet) {
        this.packetSubject.notify(this, packet);
    }

    private void notifyStateObserver(State state) {
        this.stateSubject.notify(this, state);
    }

    public void attachPacketObserver(org.yccheok.jstock.engine.Observer<ChatServiceManager, Packet> observer) {
        packetSubject.attach(observer);
    }

    public void attachStateObserver(org.yccheok.jstock.engine.Observer<ChatServiceManager, ChatServiceManager.State> observer) {
        stateSubject.attach(observer);
    }

    public void dettachAllPacketObserver() {
        packetSubject.dettachAll();
    }

    public void dettachAllStateObserver() {
        stateSubject.dettachAll();
    }

    public boolean isLogin() {
        final ChatService c = this.chatService;
        if (c == null) {
            return false;
        }
        return c.isLogin();
    }

    public boolean changePassword(String newPassword) {
        final ChatService c = this.chatService;
        if (c == null) {
            return false;
        }
        return c.changePassword(newPassword);
    }

    private final SubjectEx<ChatServiceManager, ChatServiceManager.State> stateSubject = this.getStateSubject();

    private final SubjectEx<ChatServiceManager, Packet> packetSubject = this.getPacketSubject();

    private static final Log log = LogFactory.getLog(ChatServiceManager.class);

    private ChatService chatService = null;
}
