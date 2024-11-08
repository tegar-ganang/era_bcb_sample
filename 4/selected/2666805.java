package org.net.stbp;

import java.io.EOFException;
import java.io.IOException;
import org.engine.AbstractGameEngine;
import org.engine.GameEngine;
import org.engine.IllegalGameActionException;
import org.engine.Player;
import org.event.GameEvent;
import org.event.Request;
import org.game.thyvin.event.MessageTypes;

public class STBPGameEngineProxy extends AbstractGameEngine implements GameEngine {

    protected ProtocolWriter pmwriter;

    protected IncommingMessageHandler imh;

    protected boolean keepConn = true;

    public STBPGameEngineProxy(ProtocolReader reader, ProtocolWriter writer) {
        pmwriter = writer;
        imh = new IncommingMessageHandler(reader);
        imh.start();
    }

    public Player playerJoin(String playerName) throws IllegalStateException {
        try {
            return imh.playerJoin(playerName);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void request(Request requestEvent) throws IllegalStateException, IllegalGameActionException {
        try {
            pmwriter.writeMessage(requestEvent);
        } catch (IOException e) {
            throw new IllegalGameActionException("Connection issue", e);
        }
    }

    public void setMaxTurns(int turns) throws IllegalStateException {
        try {
            sendMSG(ProtocolMessage.REQ_MAX_TURNS, turns);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setRoom(String roomName) throws IllegalStateException {
        try {
            sendMSG(ProtocolMessage.REQ_ROOM_NAME, roomName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startGame() throws IllegalStateException, IOException, IllegalGameActionException {
        sendMSG(ProtocolMessage.REQ_START_GAME, null);
    }

    public void terminateGame() {
        try {
            sendMSG(ProtocolMessage.REQ_TERMINATE, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void sendMSG(int id, Object value) throws IOException {
        ProtocolMessage pm = new ProtocolMessage();
        pm.pmid = id;
        pm.payload = value;
        pmwriter.writeMessage(pm);
    }

    protected class IncommingMessageHandler extends Thread {

        protected ProtocolReader reader;

        String plName;

        boolean pending = false;

        Player pl;

        Object registerMutex = new Object();

        public IncommingMessageHandler(ProtocolReader reader) {
            this.reader = reader;
        }

        @Override
        public void run() {
            while (keepConn) {
                try {
                    ProtocolMessage pm = reader.readMessage();
                    switch(pm.pmid) {
                        case ProtocolMessage.GAME_EVENT:
                            {
                                GameEvent amsg = ((GameEvent) pm.payload);
                                int type = amsg.getType();
                                if (type == MessageTypes.GAME_SETUP) {
                                    isGameRunning = true;
                                } else if (type == MessageTypes.GAME_FINISH) {
                                    isGameRunning = false;
                                }
                                java.awt.EventQueue.invokeLater(new DelayedMessageTask(amsg));
                                break;
                            }
                        case ProtocolMessage.PLAYER_ID:
                            {
                                Object[] v = (Object[]) pm.payload;
                                setId((String) v[1], (Player) v[0]);
                                break;
                            }
                    }
                } catch (EOFException e) {
                    keepConn = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public Player playerJoin(String name) throws IOException {
            ProtocolMessage pm = new ProtocolMessage();
            pm.pmid = ProtocolMessage.REQ_JOIN_PLAYER;
            pm.payload = name;
            synchronized (registerMutex) {
                synchronized (this) {
                    pmwriter.writeMessage(pm);
                    pending = true;
                    plName = name;
                    long start = System.currentTimeMillis();
                    while (pending && System.currentTimeMillis() < start + 5000) {
                        try {
                            wait(5000);
                        } catch (InterruptedException e) {
                        }
                    }
                    if (!pending) {
                        if (pl == null) {
                            throw new IllegalStateException();
                        }
                        return pl;
                    }
                    System.err.println("Protocol Error: No Id received");
                    return null;
                }
            }
        }

        public synchronized void setId(String name, Player id) {
            if (pending && name.equals(plName)) {
                pending = false;
                pl = id;
                notify();
            }
        }
    }

    protected class DelayedMessageTask implements Runnable {

        protected GameEvent e;

        public DelayedMessageTask(GameEvent e) {
            this.e = e;
        }

        public void run() {
            broadcastGameEvent(e);
        }
    }
}
