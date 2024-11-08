package network.client.output;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;
import network.Operation;
import world.GameObject;

/**
 * responsible for writing client information to the server
 * @author Jack
 *
 */
public final class ClientTCPWriterThread implements Runnable {

    /**
	 * after data is written it is removed from the map
	 */
    HashMap<GameObject, WriteData> m = new HashMap<GameObject, WriteData>();

    PriorityQueue<WriteData> q = new PriorityQueue<WriteData>();

    private Semaphore s = new Semaphore(1, true);

    private DataOutputStream dos;

    /**
	 * if true then disconnect message is being transmitted
	 */
    private boolean disconnecting = false;

    /**
	 * if true then disconnect message has been successfully sent
	 */
    private boolean disconnected = false;

    public ClientTCPWriterThread(DataOutputStream dos) {
        this.dos = dos;
    }

    /**
	 * sends the disconnect message to the server
	 */
    public synchronized void disconnect() {
        disconnecting = true;
        notify();
    }

    /**
	 * gets whether or not the disconnect message has been successfull transmitted
	 * @return returns true if the disconnect message has been
	 * sent to the server, false otherwise
	 */
    public boolean isDisconnected() {
        return disconnected;
    }

    public void run() {
        try {
            while (!disconnecting) {
                try {
                    while (q.size() > 0 && !disconnecting) {
                        s.acquire();
                        WriteData wd = q.poll();
                        byte[] buf = wd.getData();
                        m.remove(wd.getObject());
                        s.release();
                        dos.write(buf);
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
            dos.writeByte(Operation.clientDisconnect);
            dos.close();
            disconnected = true;
            System.out.println("disconnect message written");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("terminating tcp write thread");
    }

    /**
	 * adds a byte buffer to the queue to be written to the output stream
	 * @param buff
	 * @param o the game object the data is describing
	 */
    public synchronized void add(byte[] buff, GameObject o) {
        try {
            s.acquire();
            WriteData wd = new WriteData(o, buff);
            if (m.containsKey(o)) {
                WriteData temp = m.get(o);
                q.remove(temp);
                wd.setPriority(temp.getPriority() + wd.getPriority());
            }
            m.put(o, wd);
            q.add(wd);
            s.release();
            notify();
        } catch (InterruptedException e) {
        }
    }
}
