package network.client.output;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;
import world.GameObject;

/**
 * responsible for writing client information to the server
 * @author Jack
 *
 */
public final class ClientUDPWriterThread implements Runnable {

    /**
	 * after data is written it is removed from the map
	 */
    protected HashMap<GameObject, WriteData> m = new HashMap<GameObject, WriteData>();

    protected PriorityQueue<WriteData> q = new PriorityQueue<WriteData>();

    /**
	 * semaphore controlling access to the queue containing the write data
	 */
    protected Semaphore dataSem = new Semaphore(1, true);

    MulticastSocket ms;

    /**
	 * the semaphore controlling access to the multicast socket,
	 * the socket cannot be writing things while it changes groups
	 */
    Semaphore socketSem = new Semaphore(1, true);

    boolean disconnecting = false;

    InetAddress group;

    public ClientUDPWriterThread(MulticastSocket ms, InetAddress group) {
        this.ms = ms;
        this.group = group;
    }

    /**
	 * clears the queue and hash map of all data
	 */
    public void clearData() {
        try {
            dataSem.acquire();
            q = new PriorityQueue<WriteData>();
            m = new HashMap<GameObject, WriteData>();
            dataSem.release();
        } catch (InterruptedException e) {
        }
    }

    /**
	 * adds a byte buffer to the queue to be written to the output stream
	 * @param buff
	 * @param o the game object the data is describing
	 */
    public synchronized void add(byte[] buff, GameObject o) {
        try {
            dataSem.acquire();
            WriteData wd = new WriteData(o, buff);
            if (m.containsKey(o)) {
                WriteData temp = m.get(o);
                q.remove(temp);
                wd.setPriority(temp.getPriority() + wd.getPriority());
            }
            m.put(o, wd);
            q.add(wd);
            dataSem.release();
            notify();
        } catch (InterruptedException e) {
        }
    }

    public void run() {
        try {
            while (!disconnecting) {
                try {
                    while (q.size() > 0 && !disconnecting) {
                        dataSem.acquire();
                        WriteData wd = q.poll();
                        byte[] buff = wd.getData();
                        m.remove(wd.getObject());
                        dataSem.release();
                        DatagramPacket packet = new DatagramPacket(buff, buff.length, group, ms.getLocalPort());
                        ms.send(packet);
                    }
                    if (!disconnecting) {
                        synchronized (this) {
                            wait();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("terminating udp write thread");
    }

    /**
	 * sends the disconnect message to the server
	 */
    public synchronized void disconnect() {
        disconnecting = true;
        notify();
    }
}
