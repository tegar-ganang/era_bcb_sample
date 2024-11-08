package net.sourceforge.libairc;

import java.io.*;
import java.net.*;

/**
 * Outgoing message queue
 * 
 * @author alx
 * @author p-static
 */
public class Outgoing extends Thread {

    /**
	 * Client
	 */
    protected Client client;

    /**
	 * Socket
	 */
    protected Socket socket;

    /**
	 * Writer to the socket
	 */
    protected BufferedWriter out;

    /**
	 * Reference to the queue
	 */
    protected PrioritizedQueue queue;

    /**
	 * Are we running?
	 */
    protected boolean running;

    /**
	 * Outgoing constructor
	 *
	 * @param client the client
	 * @param socket the socket
	 * @param priorityLevels the number of priority levels
	 */
    public Outgoing(Client client, Socket socket, int priorityLevels) {
        super();
        this.client = client;
        this.socket = socket;
        try {
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            queue = new PrioritizedQueue(priorityLevels);
            start();
        } catch (IOException ioEx) {
            libairc.exception("Creating outgoing thread", ioEx);
        }
    }

    /**
	 * Get a reference to the queue
	 *
	 * @return a reference to the queue
	 */
    public PrioritizedQueue getQueue() {
        return queue;
    }

    /**
	 * Thread.run() runs in a separate thread
	 * Output lines to the server
	 */
    public void run() {
        String line;
        running = true;
        try {
            while (running) {
                line = queue.getNextMessage();
                if (line != null) {
                    libairc.debug("Outgoing", line);
                    out.write(line + "\n");
                    out.flush();
                    sleep(1000);
                } else {
                    sleep(500);
                }
            }
        } catch (InterruptedException iEx) {
            libairc.exception("Outgoing thread: wait()", iEx);
        } catch (IOException ioEx) {
            libairc.exception("Outgoing thread: write()", ioEx);
        }
        try {
            socket.close();
        } catch (IOException ioEx) {
            libairc.exception("Closing outgoing socket", ioEx);
        }
    }
}
