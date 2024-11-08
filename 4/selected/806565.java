package network.writeController;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * reliably writes queued byte buffers to the server or client via tcp
 * @author Jack
 *
 */
public final class TCPWriteThread extends Thread {

    private LinkedBlockingQueue<byte[]> q = new LinkedBlockingQueue<byte[]>();

    private Socket s;

    public TCPWriteThread(Socket s) {
        this.s = s;
        setDaemon(true);
        start();
    }

    public int getQueueSize() {
        return q.size();
    }

    public void run() {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(s.getOutputStream());
        } catch (IOException e) {
        }
        while (dos != null) {
            while (q.size() > 0) {
                byte[] b = q.poll();
                try {
                    dos.writeShort(Short.MIN_VALUE + b.length);
                    s.getOutputStream().write(b);
                } catch (IOException e) {
                    System.out.println("tcp write thread io exception, cannot write to socket stream");
                    break;
                }
            }
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    System.out.println("tcp write thread interrupted exception caught, breaking loop");
                    break;
                }
            }
        }
        System.out.println("tcp write thread terminated");
    }

    /**
	 * adds the passed byte buffer to the queue to be written to the socket
	 * @param b
	 */
    public synchronized void queueData(byte[] b) {
        q.add(b);
        notify();
    }
}
