package danlib;

import java.net.*;
import java.io.*;
import java.util.HashMap;

public class Client {

    public ObjectOutputStream out;

    public ObjectInputStream in;

    public int id = (int) (Math.random() * Integer.MAX_VALUE);

    public HashMap<Integer, Packet> last = new HashMap<Integer, Packet>();

    OutgoingWriter sender = new OutgoingWriter();

    public Client(String IP, int port) {
    }

    public void send(Packet pack) {
        pack.id = id;
        sender.send(pack);
    }

    public void start() {
        id = (int) (Math.random() * Integer.MAX_VALUE);
        connect();
        Thread reader = new Thread(new IncomingReader());
        reader.start();
        Thread writer = new Thread(sender);
        writer.start();
    }

    public void connect() {
        try {
            Socket sock = new Socket("127.0.0.1", 53287);
            out = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream());
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    public class IncomingReader implements Runnable {

        public void run() {
            Object obj = null;
            try {
                while ((obj = in.readObject()) != null) {
                    assert (obj instanceof Packet);
                    Packet temp = (Packet) obj;
                    last.put(temp.id, temp);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                if (ex instanceof StreamCorruptedException) {
                    start();
                }
            }
        }
    }

    public class OutgoingWriter implements Runnable {

        public void run() {
        }

        public void send(Object obj) {
            try {
                synchronized (obj) {
                    out.writeObject(obj);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
