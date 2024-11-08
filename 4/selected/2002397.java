package com.freelancer.wordgame;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author seb
 */
public class ClientConnection extends Thread {

    private ServerSocket privateSocket;

    private Socket client;

    DataInputStream fromClient = null;

    DataOutputStream toClient = null;

    private String letters;

    private String clientName = null;

    private int score = 0;

    private int row;

    public String getClientName() {
        return clientName;
    }

    public int getScore() {
        return score;
    }

    public int addScore(int toAdd) {
        score += toAdd;
        return score;
    }

    public ClientConnection(ServerSocket privateSocket, String letters) throws IOException {
        this.privateSocket = privateSocket;
        this.letters = letters;
        client = privateSocket.accept();
        connected = true;
        fromClient = new DataInputStream(client.getInputStream());
        toClient = new DataOutputStream(client.getOutputStream());
        clientName = fromClient.readUTF();
        toClient.writeUTF(letters);
        row = Server.server.addClient(this);
    }

    HashSet<String> submitted = new HashSet<String>();

    @Override
    public void run() {
        while (false == disconnectClient) {
            try {
                String word = fromClient.readUTF();
                Server.logln("Client " + clientName + " submited the word \"" + word + "\"");
                if (submitted.contains(word)) {
                    toClient.writeUTF("Word rejected: already submitted");
                    continue;
                }
                int wordScore = computeScore(word);
                submitted.add(word);
                score += wordScore;
                String msg = "Word accepted, score: " + score;
                toClient.writeUTF(msg);
                Server.logln("Client " + clientName + ": " + msg);
                Server.server.updateScore(row, score);
            } catch (IOException ex) {
                Server.log(ex);
                disconnectClient = true;
            }
        }
        disconnect();
    }

    boolean connected = false;

    boolean disconnectClient = false;

    public void disconnect() {
        connected = false;
        try {
            fromClient.close();
        } catch (IOException ex) {
            Server.log(ex);
        }
        fromClient = null;
        try {
            toClient.close();
        } catch (IOException ex) {
            Server.log(ex);
        }
        toClient = null;
        disconnectClient = true;
    }

    private int computeScore(String word) {
        return 1;
    }
}
