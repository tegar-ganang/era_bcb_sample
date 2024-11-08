package org.metastatic.net.ssh2;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

/**
 * This thread waits for channel data to become available, and sends it
 * out over the wire, first-in-first-out.
 *
 * @version $Revision: 180 $
 */
class ClientOutputLoop extends Thread implements SSH2Constants {

    protected LinkedList channel_queue;

    protected Connection conn;

    protected boolean frozen;

    ClientOutputLoop(Connection conn, LinkedList channel_queue) {
        this.channel_queue = channel_queue;
        this.conn = conn;
    }

    public void run() {
        PacketOutputStream pout = conn.getPacketOutputStream();
        OutputStream out = conn.getOutputStream();
        frozen = false;
        Debug.debug("Client output loop started.");
        while (conn.connected()) {
            synchronized (channel_queue) {
                while (channel_queue.size() == 0) {
                    try {
                        Debug.debug2("Waiting on " + channel_queue);
                        channel_queue.wait();
                    } catch (InterruptedException ie) {
                    }
                }
                while (frozen) ;
            }
            synchronized (pout) {
                synchronized (out) {
                    ChannelPacket p = (ChannelPacket) channel_queue.removeFirst();
                    int rem = p.getChannel().getSendWindowSize();
                    Debug.debug("window size=" + rem + " data size=" + p.getDataLength());
                    if (p.getChannel() != Channel.CONTROL_CHANNEL && p.consumesWindow() && rem < p.getDataLength()) {
                        channel_queue.addFirst(p);
                        continue;
                    }
                    try {
                        Debug.debug("Writing packet.");
                        pout.reset();
                        pout.write(p.getPayload());
                        out.write(pout.toBinaryPacket());
                    } catch (IOException ioe) {
                    }
                    if (p.consumesWindow()) {
                        p.getChannel().adjustSendWindowSize(-p.getDataLength());
                    }
                }
            }
        }
        Debug.debug("Client output loop ending.");
    }

    void freeze() {
        frozen = true;
    }

    void unfreeze() {
        frozen = false;
    }
}
