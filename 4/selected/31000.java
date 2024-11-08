package network.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

/**
 * a thread that automatically accepts clients to the server
 * @author Jack
 *
 */
public final class ServerAcceptorThread implements Runnable {

    private ServerSocket ss;

    /**
	 * keeps track of the total number of clients that the server accepted
	 * connection to in the course of its life
	 */
    private int clientsAccepted = 0;

    /**
	 * a map of all connected sockets, in order to modify it needs permission
	 * from the socket semamphore, used to forward information
	 */
    private HashMap<Socket, ServerWriterThread> sockets = new HashMap<Socket, ServerWriterThread>();

    /**
	 * the semaphore for protecting the hash set of client sockets from
	 * concurrency exceptions
	 */
    private Semaphore socketSem = new Semaphore(1, true);

    private Server server;

    private long id = 0;

    private long idRange;

    /**
	 * creates a new server acceptor thread to connect clients to the server
	 * @param ss
	 * @param server
	 * @param idRange the range of id values to be assigned to each client
	 */
    public ServerAcceptorThread(ServerSocket ss, Server server, long idRange) {
        this.ss = ss;
        this.server = server;
        this.idRange = idRange;
    }

    /**
	 * gets the map containing the sockets of connected clients
	 * and the writer objects for each
	 * @return returns a map of connected client sockets and
	 * write objects
	 */
    public HashMap<Socket, ServerWriterThread> getSockets() {
        return sockets;
    }

    /**
	 * gets the semaphore for modifying the client socket set
	 * @return reuturns the socket semaphore
	 */
    public Semaphore getSocketSemaphore() {
        return socketSem;
    }

    public void run() {
        for (; ; ) {
            try {
                Socket s = ss.accept();
                clientsAccepted++;
                System.out.println("client connection accepted!");
                System.out.println("total clients accepted = " + clientsAccepted);
                System.out.println("assigning ids: " + id + " - " + (id + idRange));
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                dos.writeLong(id);
                id += idRange;
                dos.writeLong(id);
                System.out.println("creating new server writer thread...");
                ServerWriterThread swt = new ServerWriterThread(s);
                try {
                    socketSem.acquire();
                    sockets.put(s, swt);
                    socketSem.release();
                } catch (InterruptedException e) {
                }
                new Thread(swt).start();
                System.out.println("creating new server receiver thread...");
                new Thread(new ServerReceiverThread(s, server)).start();
                System.out.println("ready!");
                System.out.println("--------------------------------------------------");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
