package fr.antifirewall.util;

import java.net.*;
import java.io.*;

/**
 * Serveur d'echo permettant de tester les differents service de communication en local
 */
public class ServeurEcho extends Thread {

    public static final int PORT_DEFAUT = 7;

    int port;

    EtatServeur etatServeur = null;

    public ServeurEcho() {
        this(PORT_DEFAUT);
    }

    public ServeurEcho(int port) {
        this.port = port;
        etatServeur = new EtatServeur();
        etatServeur.passeEtat(EtatServeur.NON_DEMARRE);
    }

    public EtatServeur getEtat() {
        return etatServeur;
    }

    public void run() {
        try {
            ServerSocket serveur = new ServerSocket(port);
            etatServeur.passeEtat(EtatServeur.DEMARRE);
            Socket socket = serveur.accept();
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            try {
                byte read;
                while (true) {
                    read = input.readByte();
                    output.writeByte(read);
                }
            } catch (EOFException e) {
            }
            output.flush();
            socket.shutdownInput();
            socket.shutdownOutput();
            socket.close();
            serveur.close();
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }
}
