package com.scottandjoe.texasholdem.networking;

import com.scottandjoe.texasholdem.resources.Utilities;
import javax.swing.ImageIcon;
import java.net.Socket;

/**
 * A class representing a client to a MessageServer.
 *
 * Contains all connection info to the actual client.
 *
 * @author Scott DellaTorre
 * @author Joe Stein
 */
public class Client implements EMSExceptionHandler {

    private static int currID = 0;

    private ClientHandler ch;

    private int id;

    private ImageIcon image;

    private boolean isDying = false;

    private boolean isHost;

    private boolean isObserving = true;

    private boolean isReady;

    private String name;

    private MessageServer server;

    private final Socket socket;

    private EncryptedMessageWriter writer;

    Client(boolean gameStarted, boolean isHost, boolean listenToClient, String message, boolean requireSpectator, MessageServer server, Socket socket) {
        id = currID++;
        EncryptedMessageReader reader = null;
        try {
            reader = new EncryptedMessageReader(new MessageReader(socket.getInputStream()), "RSA", 128, server.getSettings().getKeyPair().getPrivate());
            writer = new EncryptedMessageWriter(this, new MessageWriter(socket.getOutputStream()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.isHost = isHost;
        this.server = server;
        this.socket = socket;
        if (listenToClient) {
            NetworkUtilities.sendMessage(writer, new Message(Message.Type.CONNECTED, "connected", server.getSettings().getKeyPair().getPublic()), false);
            ch = new ClientHandler(this, gameStarted, message, reader, requireSpectator, server, writer);
        } else {
            NetworkUtilities.sendMessage(writer, new Message(Message.Type.CONNECTION_DENIED, message), false);
        }
    }

    ClientHandler getClientHandler() {
        return ch;
    }

    int getID() {
        return id;
    }

    ImageIcon getImage() {
        return image;
    }

    MessageServer getMessageServer() {
        return server;
    }

    String getName() {
        if (name == null) {
            return String.valueOf(id);
        } else {
            return name;
        }
    }

    Socket getSocket() {
        return socket;
    }

    EncryptedMessageWriter getWriter() {
        return writer;
    }

    public void handleException(EMSException emse) {
        if (emse instanceof EMSCorruptedException) {
            if (!isDying) {
                Utilities.log(Utilities.LOG_ERROR, emse);
                server.removeClient(this);
            }
        } else if (emse instanceof InvalidMessageException) {
            Utilities.log(Utilities.LOG_ERROR, emse);
            server.kickClient(this, false, "You have been kicked from the lobby because you sent an invalid message to the server.");
        }
    }

    boolean isHost() {
        return isHost;
    }

    boolean isObserving() {
        return isObserving;
    }

    boolean isReady() {
        return isReady;
    }

    void kill() {
        setDying(true);
        ch.kill();
    }

    void sendMessage(Message message, boolean encrypted) {
        NetworkUtilities.sendMessage(writer, message, encrypted);
    }

    void sendMessageAndWait(Message message, boolean encrypted) throws InterruptedException {
        NetworkUtilities.sendMessageAndWait(writer, message, encrypted);
    }

    void setDying(boolean dying) {
        isDying = dying;
    }

    void setID(int id) {
        this.id = id;
    }

    void setImage(ImageIcon img) {
        image = img;
    }

    void setName(String name) {
        this.name = name;
    }

    void setObserving(boolean observing) {
        isObserving = observing;
        server.updatePlayerCount();
    }

    void setReady(boolean ready) {
        isReady = ready;
    }

    @Override
    public String toString() {
        return "Client {ID: " + id + ", Name: " + name + ", Host: " + isHost + ", Observing: " + isObserving + "}";
    }
}
