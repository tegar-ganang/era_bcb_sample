package net.sf.gps2nmea;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.bluetooth.RemoteDevice;
import javax.microedition.io.StreamConnection;

/**
 * Write-only bluetooth client.
 * 
 * @author Olivier Cornu <o.cornu@gmail.com>
 */
public class NmeaClient implements Runnable {

    private final String name;

    private final String mac;

    private final Listener listener;

    private final StreamConnection conn;

    private volatile boolean online = true;

    private final InputStream in;

    private final Thread reader = new Thread(this);

    private final OutputStream out;

    private final StringBuffer outbuf = new StringBuffer();

    private final Thread writer = new Thread(this);

    public NmeaClient(Listener clientManager, StreamConnection conn) throws IOException {
        RemoteDevice dev = RemoteDevice.getRemoteDevice(conn);
        this.name = dev.getFriendlyName(false);
        this.mac = macAddress(dev.getBluetoothAddress());
        this.listener = clientManager;
        this.conn = conn;
        this.in = conn.openInputStream();
        this.out = conn.openOutputStream();
        clientManager.newClient(this);
        reader.start();
        writer.start();
    }

    public synchronized void send(String data) {
        if (data == null || data.length() == 0) return;
        outbuf.append(data);
        notify();
    }

    private synchronized String empty() {
        while (online && outbuf.length() == 0) try {
            wait();
        } catch (InterruptedException e) {
        }
        String data = outbuf.toString();
        outbuf.setLength(0);
        return data;
    }

    public void run() {
        try {
            if (Thread.currentThread() == reader) runReader(); else if (Thread.currentThread() == writer) runWriter();
        } catch (IOException e) {
            close();
        } catch (Exception e) {
        }
    }

    private void runReader() throws IOException {
        byte[] inbuf = new byte[128];
        while (online) in.read(inbuf);
    }

    private void runWriter() throws IOException {
        String data;
        while (online) {
            data = empty();
            if (data.length() == 0) continue;
            out.write(data.getBytes());
        }
    }

    public synchronized void close() {
        if (!online) return;
        online = false;
        notifyAll();
        try {
            in.close();
        } catch (IOException e) {
        }
        try {
            out.close();
        } catch (IOException e) {
        }
        try {
            conn.close();
        } catch (IOException e) {
        }
        listener.clientClosed(this);
    }

    public String toString() {
        return mac + "  " + name;
    }

    public static String macAddress(String mac) {
        StringBuffer buf = new StringBuffer(mac.substring(0, 2));
        buf.append(":").append(mac.substring(2, 4));
        buf.append(":").append(mac.substring(4, 6));
        buf.append(":").append(mac.substring(6, 8));
        buf.append(":").append(mac.substring(8, 10));
        buf.append(":").append(mac.substring(10, 12));
        return buf.toString();
    }

    public static interface Listener {

        public void newClient(NmeaClient client);

        public void clientClosed(NmeaClient client);
    }
}
