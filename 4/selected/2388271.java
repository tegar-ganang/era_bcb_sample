package ncclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JOptionPane;

/**
 *
 * @author Carl Berglund
 */
public class Listener implements Runnable {

    private ServerSocket serverSocket;

    private Socket socket;

    private BufferedWriter writer;

    private BufferedReader reader;

    private State state;

    /** Creates a new instance of Listener */
    public Listener(State state) {
        this.state = state;
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(Constants.CLIENT_PORT);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Listener1: " + e, "ERROR", JOptionPane.WARNING_MESSAGE);
        }
        for (; ; ) {
            try {
                socket = serverSocket.accept();
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
                String clientip = socket.getInetAddress().toString().substring(1);
                Contact contact = null;
                String msg = reader.readLine();
                String[] array;
                if (msg.startsWith("CLIENTINFO")) {
                    array = msg.split(" ")[1].split(";");
                    String id = array[0];
                    String ip = array[1];
                    String nick = array[2];
                    String status = array[3];
                    if (clientip.equals(ip)) {
                        contact = new Contact(id, ip, nick, status);
                        System.out.println("connection added for: " + contact.getNick());
                    }
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
                state.addConnection(socket, contact, reader, writer);
                Thread reciever = new Thread(new Reciever(state, state.getConnection(contact)));
                reciever.start();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Listener2: " + e, "ERROR", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
}
