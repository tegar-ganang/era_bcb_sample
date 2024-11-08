package com.scottandjoe.texasholdem.networking;

import com.scottandjoe.texasholdem.misc.KillableThread;
import com.scottandjoe.texasholdem.resources.Utilities;
import java.io.IOException;
import javax.crypto.SecretKey;
import javax.swing.ImageIcon;

/**
 *
 * @author Scott DellaTorre
 * @author Joe Stein
 */
class ClientHandler {

    private Client c;

    private boolean gameStarted;

    private ListenerThread listenerThread;

    private String message;

    private boolean requireSpectator;

    private MessageServer server;

    ClientHandler(Client client, boolean gameStarted, String message, EncryptedMessageReader reader, boolean requireSpectator, MessageServer server, EncryptedMessageWriter writer) {
        this.c = client;
        this.gameStarted = gameStarted;
        this.message = message;
        this.requireSpectator = requireSpectator;
        this.server = server;
        listenerThread = new ListenerThread(reader);
        listenerThread.start();
    }

    void kill() {
        listenerThread.kill();
    }

    void setServer(MessageServer server) {
        this.server = server;
    }

    private class ListenerThread extends KillableThread {

        private Message mes = null;

        private EncryptedMessageReader reader;

        private ListenerThread(EncryptedMessageReader reader) {
            super("Client Handler Listener Thread for Client " + c.getID());
            this.reader = reader;
        }

        public void doRun() {
            try {
                mes = reader.readMessage();
                Utilities.logParcial(Utilities.LOG_OUTPUT, "Message received from client " + mes.getName() + ": ");
                if (message != null) {
                    if (mes.getType() == Message.Type.REQUEST_HEADER) {
                        Utilities.log(Utilities.LOG_OUTPUT, "Requested header.");
                        Object[] content = (Object[]) mes.getContent();
                        c.setObserving((Boolean) content[0]);
                        c.getWriter().reInitialize("AES", Integer.MAX_VALUE, (SecretKey) content[1]);
                        if ((Boolean) content[0] == requireSpectator) {
                            NetworkUtilities.sendMessage(c.getWriter(), new Message(Message.Type.SERVER_INFO, server.getSettings().getServerName(), c.getMessageServer().getSettings().getWaitForPlayers()), false);
                        } else {
                            NetworkUtilities.sendMessage(c.getWriter(), new Message(Message.Type.ASK_TO_SPECTATE), false);
                        }
                        message = null;
                    } else {
                        Utilities.log(Utilities.LOG_ERROR, "Message ignored!!!");
                    }
                } else if (mes.getType() == Message.Type.ASK_TO_SPECTATE) {
                    if ((Boolean) mes.getContent() == true) {
                        Utilities.log(Utilities.LOG_OUTPUT, "Client will spectate.");
                        c.setObserving(true);
                        NetworkUtilities.sendMessage(c.getWriter(), new Message(Message.Type.SERVER_INFO, c.getMessageServer().getSettings().getServerName(), c.getMessageServer().getSettings().getWaitForPlayers()), false);
                        server.sendClientInfo(c.getID());
                    } else {
                        server.removeClient(c);
                    }
                } else if (mes.getType() == Message.Type.DELIVER_INPUT) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Input delivered");
                    if (server instanceof GameServer) {
                        ((GameServer) server).relayInput(c, mes);
                    } else {
                        throw new InvalidMessageException("Game server not expecting client input", c, mes);
                    }
                } else if (mes.getType() == Message.Type.JOINED_LOBBY) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Joined lobby.");
                    if (server.setClientInfo(c.getID(), mes.getName(), (ImageIcon) mes.getContent()) && gameStarted) {
                        NetworkUtilities.sendMessage(c.getWriter(), new Message(Message.Type.STARTING_GAME, server.getSettings().getServerName(), server.getSettings().getHostPort()), false);
                    }
                } else if (mes.getType() == Message.Type.KICK_USER) {
                    if (c.isHost()) {
                        Utilities.log(Utilities.LOG_OUTPUT, "Kicking user " + mes.getName() + ".");
                        if (((String) mes.getContent()).equals("KickAndBan")) {
                            server.banClient(mes.getName());
                            server.kickClient(mes.getName(), true);
                        } else {
                            server.kickClient(mes.getName(), false);
                        }
                    } else {
                        Utilities.log(Utilities.LOG_OUTPUT, "Kick user request from " + c.getName() + " denied.");
                    }
                } else if (mes.getType() == Message.Type.LEFT_GAME || mes.getType() == Message.Type.LEFT_LOBBY) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Left lobby/game.");
                    server.removeClient(c);
                } else if (mes.getType() == Message.Type.READY) {
                    boolean isReady;
                    if (((String) mes.getContent()).equals("true")) {
                        Utilities.log(Utilities.LOG_OUTPUT, "Is ready.");
                        isReady = true;
                    } else {
                        Utilities.log(Utilities.LOG_OUTPUT, "Is not ready.");
                        isReady = false;
                    }
                    server.setReady(c.getID(), isReady);
                } else if (mes.getType() == Message.Type.REQUEST_CLIENT_INFO) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Requested names.");
                    server.sendClientInfo(c.getID());
                } else if (mes.getType() == Message.Type.REQUEST_HEADER) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Requested header.");
                    Object[] content = (Object[]) mes.getContent();
                    c.setObserving((Boolean) content[0]);
                    reader.reInitialize("AES", Integer.MAX_VALUE, (SecretKey) content[1]);
                    c.getWriter().reInitialize("AES", Integer.MAX_VALUE, (SecretKey) content[1]);
                    NetworkUtilities.sendMessage(c.getWriter(), new Message(Message.Type.SERVER_INFO, c.getMessageServer().getSettings().getServerName(), c.getMessageServer().getSettings().getWaitForPlayers()), false);
                } else {
                    Utilities.log(Utilities.LOG_OUTPUT, mes);
                    server.relayMessage(mes, false);
                }
            } catch (EMSException emse) {
                c.handleException(emse);
            } catch (Exception e) {
                e.printStackTrace();
                server.removeClient(c);
            }
        }

        public void postDeath() {
        }

        public void preDeath() {
            try {
                reader.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        public void preRun() {
        }
    }
}
