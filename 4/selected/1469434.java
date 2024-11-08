package ncclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.Socket;

/**
 *
 * @author Carl Berglund
 */
public class Connection {

    private Socket s;

    private BufferedReader reader;

    private BufferedWriter writer;

    private Contact c;

    /** Creates a new instance of SocketList */
    public Connection(Socket s, Contact c, BufferedReader reader, BufferedWriter writer) {
        this.s = s;
        this.c = c;
        this.reader = reader;
        this.writer = writer;
    }

    public Socket getSocket() {
        return s;
    }

    public Contact getContact() {
        return c;
    }

    public BufferedReader getReader() {
        return reader;
    }

    public BufferedWriter getWriter() {
        return writer;
    }
}
