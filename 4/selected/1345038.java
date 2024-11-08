package com.fusteeno.gnutella.net;

import java.io.IOException;
import com.fusteeno.gnutella.io.DataOutputStream;
import com.fusteeno.gnutella.util.Debug;
import com.fusteeno.gnutella.util.SendQueue;

/**
 * @author aGO!
 * 
 * Thread per l'invio dei messaggi del servent
 * 
 * CONTROLLATO
 */
public class WriterThread implements Runnable {

    protected static final int MAX_TIME_BETWEEN_FLUSHS = 500;

    protected static final int MAX_MESSAGES_BETWEEN_FLUSHES = 10;

    private Servent servent;

    private DataOutputStream out;

    private SendQueue sendQueue;

    private boolean alive = false;

    private Thread thread;

    private final Object lock = new Object();

    private long lastFlushTime;

    private int messagesSent;

    public WriterThread(Servent servent) {
        this.servent = servent;
        sendQueue = servent.getSendQueue();
    }

    public void setOutputStream(DataOutputStream out) {
        this.out = out;
    }

    public void start() {
        if (!alive && out != null) {
            alive = true;
            thread = new Thread(this);
            thread.setName("[WriterThread] " + servent.getIp() + ":" + servent.getPort());
            thread.start();
        }
    }

    public void run() {
        lastFlushTime = System.currentTimeMillis();
        long currentTime;
        long waittime;
        int sendcounter = 0;
        Message msg;
        while (alive) {
            msg = null;
            try {
                msg = (Message) sendQueue.pop();
                switch(msg.getPayloadDescriptor()) {
                    case DescriptorHeader.QUERY_HIT:
                        Debug.log("invio msg QUERY_HIT");
                        break;
                }
                msg.write(out);
                sendcounter++;
                messagesSent++;
                sendcounter = 0;
                lastFlushTime = System.currentTimeMillis();
                out.flush();
            } catch (IOException e) {
                servent.stop();
                return;
            } catch (InterruptedException e) {
                Debug.log("Interrotto writer thread");
            }
        }
    }

    public void stop() {
        if (alive) {
            alive = false;
            thread.interrupt();
        }
    }

    public void close() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
            }
        }
    }

    public int getMessagesSent() {
        return messagesSent;
    }

    private boolean timeToFlush() {
        return (System.currentTimeMillis() - lastFlushTime) > MAX_TIME_BETWEEN_FLUSHS;
    }
}
