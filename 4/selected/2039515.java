package sies.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import sies.kr.reasoning.ForwardChaining;
import sies.kr.reasoning.WorkingMemory;
import sies.server.messages.CommunicationMessage;
import sies.server.messages.ProtocoleHeader;

/**
 * Describe class <code>ReasoningServer</code> here.
 *
 * @author <a href="mailto:fsipp@users.sourceforge.net">Fabian Sipp</a>
 * @version 1.0
 */
public class ReasoningServer {

    static WorkingMemory wm = new WorkingMemory();

    static ArrayList clients = new ArrayList();

    private int portNb;

    private ArrayList wms = new ArrayList();

    /**
     * true when server needs to be shut down.
     */
    static boolean killyourself = false;

    /**
     * These are the clients which need to be notified.
     */
    static RegisteredAgents agentsToNotify = new RegisteredAgents();

    class TimerThread extends Thread {

        public void run() {
            final String timeCompound = "currenttime";
            while (!killyourself) {
                synchronized (wm) {
                    wm.deleteFactsMatching(timeCompound + "(?x)");
                    wm.addFact(timeCompound + "(" + System.currentTimeMillis() + ")");
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
        }
    }

    ;

    public ReasoningServer(final int port) {
        this.portNb = port;
        this.start();
    }

    public ReasoningServer(final int port, final ArrayList wms) {
        this.portNb = port;
        this.wms = wms;
        this.start();
    }

    public void start() {
        try {
            ServerSocket server = new ServerSocket(portNb);
            for (int i = 0; i < wms.size(); i++) {
                try {
                    wm.loadFromXML((String) wms.get(i));
                } catch (Exception e) {
                    ReasoningServer.writeLog("Couldn't load working memory from file " + wms.get(i));
                }
            }
            TimerThread timer = new TimerThread();
            timer.start();
            ReasoningServer.writeLog(ServerMessagesResources.getString("sies.server.ready"));
            while (!killyourself) {
                try {
                    Socket client = server.accept();
                    if (killyourself) {
                        break;
                    }
                    ClientThread t = new ClientThread(new SocketIOManager(client));
                    t.start();
                    synchronized (clients) {
                        clients.add(t);
                    }
                } catch (IOException e) {
                    ReasoningServer.writeLog("Failed to accept connection on port " + portNb);
                    killyourself = true;
                }
            }
        } catch (IOException e) {
            System.out.println("error on port " + portNb);
        }
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i) != null) {
                ClientThread t = (ClientThread) clients.get(i);
                try {
                    t.closeConnections();
                } catch (IOException e) {
                    ReasoningServer.writeLog("Cannot close all connections.");
                }
            }
        }
    }

    public static void writeLog(final SocketIOManager s, final String message) {
        DateFormat longTimeStamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        String date = longTimeStamp.format(new Date());
        String toPrint = "(" + date + ") ";
        if (s != null) {
            toPrint += "[" + s.getRemoteSocketAddress() + "] ";
        }
        System.out.println(toPrint + message);
    }

    public static void writeLog(final String message) {
        ReasoningServer.writeLog(null, message);
    }

    /**
     * Describe class <code>ClientThread</code> here.
     *
     */
    static class ClientThread extends Thread {

        /**
         * Socket to the current client.
         */
        private SocketIOManager client;

        private WorkingMemory currentWm;

        private HashMap localWms;

        private ArrayList toRemoveOnDisconnection;

        private boolean local = false;

        private final char immediateTreatmentSign = '!';

        private final String messageLogRun = ServerMessagesResources.getString("sies.server.messageLogRun");

        private final String messageLogGlobal = ServerMessagesResources.getString("sies.server.messageLogGlobal");

        private final String messageLogLocal = ServerMessagesResources.getString("sies.server.messageLogLocal");

        private final String messageLogFac = ServerMessagesResources.getString("sies.server.messageLogFac");

        private final String messageLogKill = ServerMessagesResources.getString("sies.server.messageLogKill");

        private final String messageLogRem = ServerMessagesResources.getString("sies.server.messageLogRem");

        private final String messageLogRul = ServerMessagesResources.getString("sies.server.messageLogRul");

        private final String messageLogNot = ServerMessagesResources.getString("sies.server.messageLogNot");

        private final String messageLogEmpty = ServerMessagesResources.getString("sies.server.messageLogEmpty");

        private final String messageLogSave = ServerMessagesResources.getString("sies.server.messageLogSave");

        private final String messageLogLoad = ServerMessagesResources.getString("sies.server.messageLogLoad");

        private final String messageLogRemoveOnDisconnection = ServerMessagesResources.getString("sies.server.messageLogRemoveOnDisconnection");

        private final String messageLogDestroy = ServerMessagesResources.getString("sies.server.messageLogDestroy");

        private final String messageLogExtend = ServerMessagesResources.getString("sies.server.messageLogExtend");

        private final int statusOK = 1;

        private final int statusKO = 0;

        private boolean isLocal() {
            return local;
        }

        public void closeConnections() throws IOException {
            this.getClient().send(new CommunicationMessage(new ProtocoleHeader(ProtocoleHeader.BYE)));
            this.getClient().close();
            synchronized (clients) {
                clients.remove(this.getClient());
            }
        }

        /**
         * Get the client <code>ReasoningServerComManager</code> value.
         * @return the client value.
         */
        private SocketIOManager getClient() {
            return client;
        }

        private void setClient(final SocketIOManager newClient) {
            this.client = newClient;
        }

        private void setLocalWms(final HashMap newLocalWms) {
            this.localWms = newLocalWms;
        }

        private HashMap getLocalWms() {
            return localWms;
        }

        private void setCurrentWm(final WorkingMemory newCurrentWm) {
            this.currentWm = newCurrentWm;
        }

        private WorkingMemory getCurrentWm() {
            return currentWm;
        }

        private void setToRemoveOnDisconnection(final ArrayList newToRemoveOnDisconnection) {
            this.toRemoveOnDisconnection = newToRemoveOnDisconnection;
        }

        private ArrayList getToRemoveOnDisconnection() {
            return toRemoveOnDisconnection;
        }

        private void addToRemoveOnDisconnection(final String pattern) {
            this.getToRemoveOnDisconnection().add(pattern);
        }

        /**
         * Status value when client wants to be disconnected.
         */
        static final int STATUSDISCONNECT = -1;

        /**
         * Creates a new <code>ClientThread</code> instance.
         *
         * @param newClient a <code>ReasoningServerComManager</code> value
         */
        ClientThread(final SocketIOManager newClient) {
            this.setLocalWms(new HashMap());
            this.setCurrentWm(wm);
            this.setToRemoveOnDisconnection(new ArrayList());
            this.setClient(newClient);
        }

        /**
         * Describe <code>processQuery</code> method here.
         *
         * @param message a <code>CommunicationMessage</code> value
         * @return an <code>int</code> value
         */
        private int processMessage(final CommunicationMessage message) {
            String content = message.getContent(), messageLog = "";
            ProtocoleHeader header = message.getHeader();
            int status = statusOK;
            try {
                if (header.isQuit()) {
                    return STATUSDISCONNECT;
                } else if (header.isKill()) {
                    killyourself = true;
                    ReasoningServer.writeLog(this.getClient(), messageLogKill);
                    return STATUSDISCONNECT;
                } else if (header.isRemove()) {
                    messageLog = messageLogRem + " " + content + ".";
                    if (content.length() > 0) {
                        if (this.isLocal()) {
                            this.getCurrentWm().deleteFactsMatching(content);
                        } else {
                            synchronized (wm) {
                                wm.deleteFactsMatching(content);
                            }
                        }
                    }
                } else if (header.isRemoveOnDisconnection()) {
                    messageLog = messageLogRemoveOnDisconnection + " " + content;
                    this.addToRemoveOnDisconnection(content);
                } else if (header.isEmpty()) {
                    messageLog = messageLogEmpty;
                    if (this.isLocal()) {
                        this.getCurrentWm().empty();
                    } else {
                        synchronized (wm) {
                            wm.empty();
                        }
                    }
                } else if (header.isSave()) {
                    messageLog = messageLogSave + " " + content;
                    if (this.isLocal()) {
                        this.getCurrentWm().saveAsXML(content);
                    } else {
                        synchronized (wm) {
                            wm.saveAsXML(content);
                        }
                    }
                } else if (header.isLoad()) {
                    messageLog = messageLogLoad + " " + content;
                    if (this.isLocal()) {
                        this.getCurrentWm().loadFromXML(content);
                    } else {
                        synchronized (wm) {
                            wm.loadFromXML(content);
                        }
                    }
                } else if (header.isRule()) {
                    messageLog = messageLogRul + " " + content + ".";
                    if (content.length() > 0) {
                        if (this.isLocal()) {
                            try {
                                this.getCurrentWm().addRule(content);
                            } catch (Exception e) {
                                this.getCurrentWm().addProductionRule(content);
                            }
                        } else {
                            synchronized (wm) {
                                try {
                                    wm.addRule(content);
                                } catch (Exception e) {
                                    wm.addProductionRule(content);
                                }
                            }
                        }
                    }
                } else if (header.isFact()) {
                    boolean act = false;
                    ArrayList newFacts = new ArrayList();
                    messageLog = messageLogFac + " " + content + ".";
                    if (content.length() > 1) {
                        if (content.charAt(0) == immediateTreatmentSign) {
                            act = true;
                            content = content.substring(1, content.length());
                        }
                        if (this.isLocal()) {
                            this.getCurrentWm().addFact(content);
                            if (act) {
                                newFacts = new ForwardChaining(this.getCurrentWm()).run();
                                message.answer(this.getClient(), newFacts);
                            }
                        } else {
                            synchronized (wm) {
                                wm.addFact(content);
                                if (act) {
                                    synchronized (agentsToNotify) {
                                        agentsToNotify.notifyAgents(content);
                                        newFacts = new ForwardChaining(wm).run();
                                        agentsToNotify.notifyAgents(newFacts);
                                    }
                                }
                            }
                        }
                    }
                } else if (header.isNotify()) {
                    messageLog = messageLogNot + " " + content + ".";
                    synchronized (agentsToNotify) {
                        agentsToNotify.register(this.getClient(), content);
                    }
                } else if (header.isRun()) {
                    ArrayList newFacts = new ArrayList();
                    messageLog = messageLogRun;
                    if (this.isLocal()) {
                        newFacts = new ForwardChaining(this.getCurrentWm()).run();
                        message.answer(this.getClient(), newFacts);
                    } else {
                        synchronized (wm) {
                            newFacts = new ForwardChaining(wm).run();
                            synchronized (agentsToNotify) {
                                agentsToNotify.notifyAgents(newFacts);
                            }
                        }
                    }
                } else if (header.isGlobal()) {
                    messageLog = messageLogGlobal;
                    this.local = false;
                } else if (header.isLocal()) {
                    if (content != null && !"".equals(content.trim())) {
                        if (this.getLocalWms().containsKey(content)) {
                            this.setCurrentWm((WorkingMemory) this.getLocalWms().get(content));
                        } else {
                            this.setCurrentWm(new WorkingMemory());
                            this.getLocalWms().put(content, this.getCurrentWm());
                        }
                        messageLog = messageLogLocal + content;
                        this.local = true;
                    } else {
                        status = statusKO;
                        messageLog = ServerMessagesResources.getString("sies.server.error") + "cannot create local WM" + ".";
                    }
                } else if (header.isDestroy()) {
                    if (this.getLocalWms().containsKey(content)) {
                        if (((WorkingMemory) this.getLocalWms().get(content)) == this.getCurrentWm()) {
                            this.local = false;
                            this.setCurrentWm(wm);
                        }
                        this.getLocalWms().remove(content);
                    }
                    messageLog = messageLogDestroy + content;
                } else if (header.isExtend()) {
                    messageLog = messageLogExtend;
                    this.local = true;
                } else {
                    status = statusKO;
                    messageLog = ServerMessagesResources.getString("sies.server.error") + content + ".";
                }
            } catch (Exception e) {
                status = statusKO;
                e.printStackTrace();
                messageLog = ServerMessagesResources.getString("sies.server.error") + content + ".";
            }
            if (status == statusKO) {
                message.answer(this.getClient(), ProtocoleHeader.KO);
            } else {
                message.answer(this.getClient(), ProtocoleHeader.OK);
            }
            ReasoningServer.writeLog(this.getClient(), messageLog);
            return status;
        }

        /**
         * <code>run</code> waits for clients' messages
         */
        public void run() {
            try {
                String line = "";
                int status = 0;
                ReasoningServer.writeLog(this.getClient(), ServerMessagesResources.getString("sies.server.Connected"));
                while (status != STATUSDISCONNECT) {
                    if (this.getClient().isConnected()) {
                        line = this.getClient().readLine();
                        if (line != null) {
                            status = this.processMessage(new CommunicationMessage(line));
                            continue;
                        }
                    }
                    break;
                }
                this.closeConnections();
            } catch (IOException e) {
                ReasoningServer.writeLog(this.getClient(), ServerMessagesResources.getString("sies.server.IOException"));
            }
            synchronized (agentsToNotify) {
                agentsToNotify.unregister(this.getClient());
                ReasoningServer.writeLog(this.getClient(), ServerMessagesResources.getString("sies.server.Disconnected"));
            }
            ArrayList toRemoveList = this.getToRemoveOnDisconnection();
            synchronized (wm) {
                for (int i = 0; i < toRemoveList.size(); i++) {
                    wm.deleteFactsMatching((String) toRemoveList.get(i));
                }
            }
            ReasoningServer.writeLog(this.getClient(), ServerMessagesResources.getString("sies.server.Disconnected"));
        }
    }
}
